package io.github.appunnim.businessgate.automation;

import android.view.accessibility.AccessibilityNodeInfo;
import io.github.appunnim.businessgate.policy.Identity;
import io.github.appunnim.businessgate.policy.Model.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import io.github.appunnim.businessgate.automation.BoundedNodes.Ancestor;
import io.github.appunnim.businessgate.automation.BoundedNodes.Path;
import io.github.appunnim.businessgate.automation.BoundedNodes.ResourceOrigin;

/** Strict, measured root-to-field paths. Never searches arbitrary text or message subtrees. */
public final class QualifiedAdapter {
    public record Reading(AutomationController.Screen screen,String phone,String receiver,Kind kind,
        BlockState blockState,String name,boolean safe,Path action) {}
    private final String id;
    private final List<Recipe> screens=new ArrayList<>();
    private record Recipe(AutomationController.Screen screen,Path signature,Path phone,Path receiver,
        Path business,Path regular,Path blocked,Path unblocked,Path block,Path unblock,List<Path> forbidden){}
    public QualifiedAdapter(JSONObject row) throws org.json.JSONException {
        id=row.getString("id");if(!id.matches("[a-z0-9][a-z0-9-]{0,63}"))throw new IllegalArgumentException("INVALID_ADAPTER_ID");
        JSONArray recipes=row.getJSONArray("screens");
        if(recipes.length()!=3)throw new IllegalArgumentException("INCOMPLETE_SCREENS");
        java.util.Set<AutomationController.Screen> roles=new java.util.HashSet<>();
        for(int i=0;i<recipes.length();i++) {
            JSONObject s=recipes.getJSONObject(i);
            AutomationController.Screen role=AutomationController.Screen.valueOf(s.getString("role"));
            if(role==AutomationController.Screen.UNKNOWN||!roles.add(role))throw new IllegalArgumentException("DUPLICATE_SCREEN");
            List<Path> forbidden=new ArrayList<>();JSONArray exclusions=s.getJSONArray("forbiddenControls");
            for(int j=0;j<exclusions.length();j++)forbidden.add(path(exclusions.getJSONObject(j)));
            if(forbidden.size()>8)throw new IllegalArgumentException("UNBOUNDED_SIDE_EFFECT_CHECKS");
            if(forbidden.isEmpty())throw new IllegalArgumentException("MISSING_SIDE_EFFECT_CHECKS");
            screens.add(new Recipe(role,path(s.getJSONObject("signature")),path(s.getJSONObject("phone")),path(s.getJSONObject("receiver")),
                path(s.getJSONObject("business")),path(s.getJSONObject("regular")),path(s.getJSONObject("blocked")),path(s.getJSONObject("unblocked")),
                path(s.getJSONObject("blockControl")),path(s.getJSONObject("unblockControl")),java.util.Collections.unmodifiableList(forbidden)));
        }
    }
    public String id(){return id;}
    private static Path path(JSONObject p)throws org.json.JSONException {
        JSONArray indices=p.getJSONArray("children");if(indices.length()>12)throw new IllegalArgumentException("PATH_TOO_DEEP");
        List<Integer> children=new ArrayList<>();for(int i=0;i<indices.length();i++){int index=indices.getInt(i);if(index<0||index>63)throw new IllegalArgumentException("PATH_UNBOUNDED");children.add(index);}
        ResourceOrigin origin=origin(p,false);
        String suffix=p.getString("resourceSuffix");if(!suffix.matches("[a-zA-Z0-9_]{1,160}"))throw new IllegalArgumentException("INVALID_RESOURCE");
        String className=p.getString("className"),expected=p.optString("expectedText","");
        if(!className.matches("[a-zA-Z0-9_.$]{1,160}")||expected.codePointCount(0,expected.length())>160)throw new IllegalArgumentException("INVALID_FIELD");
        JSONArray lineage=p.getJSONArray("ancestors");if(lineage.length()!=children.size())throw new IllegalArgumentException("INCOMPLETE_ANCESTORS");
        List<Ancestor> ancestors=new ArrayList<>();
        for(int i=0;i<lineage.length();i++){
            JSONObject a=lineage.getJSONObject(i);String resource=a.getString("resourceSuffix"),type=a.getString("className");int count=a.getInt("childCount");
            ResourceOrigin ancestorOrigin=origin(a,true);
            boolean resourceValid=ancestorOrigin==ResourceOrigin.NONE?resource.isEmpty():resource.matches("[a-zA-Z0-9_]{1,160}");
            if(!resourceValid||!type.matches("[a-zA-Z0-9_.$]{1,160}")||count<1||count>64||children.get(i)>=count)throw new IllegalArgumentException("INVALID_ANCESTOR");
            ancestors.add(new Ancestor(resource,type,count,ancestorOrigin));
        }
        return new Path(java.util.Collections.unmodifiableList(children),suffix,className,expected,java.util.Collections.unmodifiableList(ancestors),origin);
    }
    private static ResourceOrigin origin(JSONObject value,boolean ancestor)throws org.json.JSONException {
        String scope=value.has("resourceNamespace")?value.getString("resourceNamespace"):"selected";
        return switch(scope){
            case "selected" -> ResourceOrigin.SELECTED;
            case "android" -> ResourceOrigin.ANDROID;
            case "none" -> {if(!ancestor)throw new IllegalArgumentException("FIELD_RESOURCE_REQUIRED");yield ResourceOrigin.NONE;}
            default -> throw new IllegalArgumentException("INVALID_RESOURCE_NAMESPACE");
        };
    }
    private static final BoundedNodes.Access<AccessibilityNodeInfo> ACCESS=new BoundedNodes.Access<>(){
        private String value(CharSequence value){return value==null?"":value.toString();}
        @Override public String packageName(AccessibilityNodeInfo n){return value(n.getPackageName());}
        @Override public String resource(AccessibilityNodeInfo n){return n.getViewIdResourceName();}
        @Override public String className(AccessibilityNodeInfo n){return value(n.getClassName());}
        @Override public CharSequence text(AccessibilityNodeInfo n){return n.getText();}
        @Override public boolean visible(AccessibilityNodeInfo n){return n.isVisibleToUser();}
        @Override public int childCount(AccessibilityNodeInfo n){return n.getChildCount();}
        @Override public AccessibilityNodeInfo child(AccessibilityNodeInfo n,int index){return n.getChild(index);}
        @SuppressWarnings("deprecation")
        @Override public void release(AccessibilityNodeInfo n){n.recycle();}
    };
    /** Takes ownership of root and releases every acquired node before returning to the event loop. */
    public Inspection open(AccessibilityNodeInfo root,String pkg,BoundedNodes.Budget budget){return new Inspection(root,pkg,budget);}
    public final class Inspection implements AutoCloseable {
        private final BoundedNodes<AccessibilityNodeInfo> nodes;
        private final BoundedNodes.Budget budget;
        private Reading reading;
        private Recipe matched;
        private AccessibilityNodeInfo action;
        private boolean closed;
        private Inspection(AccessibilityNodeInfo root,String pkg,BoundedNodes.Budget budget){
            this.budget=budget;nodes=new BoundedNodes<>(root,pkg,ACCESS,budget);
            try{reading=read();}catch(RuntimeException unavailable){close();}
        }
        public Reading reading(){return closed||budget.exhausted()?null:reading;}
        private boolean matches(Path path){return nodes.resolve(path)!=null;}
        private String text(Path path){AccessibilityNodeInfo n=nodes.resolve(path);return n==null||n.getText()==null||n.getText().length()>64?null:n.getText().toString();}
        private Reading read(){
            for(Recipe recipe:screens)if(matches(recipe.signature())){if(matched!=null)return null;matched=recipe;}
            if(matched==null)return null;
            for(Path path:matched.forbidden())if(matches(path))return null;
            String phone=text(matched.phone()),receiver=text(matched.receiver());
            if(phone==null||receiver==null)return null;
            try{phone=Identity.canonicalPhone(phone);receiver=Identity.canonicalPhone(receiver);}catch(IllegalArgumentException badIdentity){return null;}
            boolean business=matches(matched.business()),regular=matches(matched.regular());
            boolean blocked=matches(matched.blocked()),unblocked=matches(matched.unblocked());
            if(business==regular||blocked==unblocked||budget.exhausted())return null;
            Path path=blocked?matched.unblock():matched.block();action=nodes.resolve(path);
            // Simultaneously exposed opposite controls are ambiguous, even if the preferred path matches.
            if(action==null||nodes.resolve(blocked?matched.block():matched.unblock())!=null||budget.exhausted())return null;
            return new Reading(matched.screen(),phone,receiver,business?Kind.BUSINESS_CONFIRMED:Kind.REGULAR_PROFILE_OBSERVED,
                blocked?BlockState.BLOCKED:BlockState.UNBLOCKED,"",true,path);
        }
        public boolean languageMatches(){return reading()!=null&&!reading.action().expectedText().isEmpty();}
        public android.graphics.Rect identityBounds(){
            android.graphics.Rect bounds=new android.graphics.Rect();if(reading()==null)return bounds;
            for(Path path:List.of(matched.phone(),matched.receiver())){
                AccessibilityNodeInfo n=nodes.resolve(path);if(n==null)return new android.graphics.Rect();
                android.graphics.Rect field=new android.graphics.Rect();n.getBoundsInScreen(field);bounds.union(field);
            }
            return bounds;
        }
        public boolean actionable(){return reading()!=null&&action.isVisibleToUser()&&action.isEnabled()&&action.isClickable()&&!action.isCheckable();}
        public android.graphics.Rect actionBounds(){android.graphics.Rect bounds=new android.graphics.Rect();if(reading()!=null)action.getBoundsInScreen(bounds);return bounds;}
        public boolean click(AutomationController.Control control){return actionable()&&supports(reading,control)&&action.performAction(AccessibilityNodeInfo.ACTION_CLICK);}
        @Override public void close(){closed=true;nodes.close();}
    }
    public boolean supports(Reading reading,AutomationController.Control control) {
        return switch(control){
            case BLOCK_ENTRY -> reading.screen()==AutomationController.Screen.PROFILE&&reading.blockState()==BlockState.UNBLOCKED;
            case UNBLOCK_ENTRY -> reading.screen()==AutomationController.Screen.PROFILE&&reading.blockState()==BlockState.BLOCKED;
            case CONFIRM_BLOCK -> reading.screen()==AutomationController.Screen.BLOCK_DIALOG&&reading.blockState()==BlockState.UNBLOCKED;
            case CONFIRM_UNBLOCK -> reading.screen()==AutomationController.Screen.UNBLOCK_DIALOG&&reading.blockState()==BlockState.BLOCKED;
        };
    }
}
