package io.github.appunnim.businessgate.automation;

import android.view.accessibility.AccessibilityNodeInfo;
import io.github.appunnim.businessgate.policy.Identity;
import io.github.appunnim.businessgate.policy.Model.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/** Strict, measured root-to-field paths. Never searches arbitrary text or message subtrees. */
public final class QualifiedAdapter {
    public record Path(List<Integer> children,String resourceSuffix,String className,String expectedText) {}
    public record Reading(AutomationController.Screen screen,String phone,String receiver,Kind kind,
        BlockState blockState,String name,boolean safe,Path action) {}
    private final String id;
    private final List<Recipe> screens=new ArrayList<>();
    private record Recipe(AutomationController.Screen screen,Path signature,Path phone,Path receiver,
        Path business,Path regular,Path blocked,Path unblocked,Path block,Path unblock,List<Path> forbidden){}
    public QualifiedAdapter(JSONObject row) throws org.json.JSONException {
        id=row.getString("id");
        JSONArray recipes=row.getJSONArray("screens");
        if(recipes.length()!=3)throw new IllegalArgumentException("INCOMPLETE_SCREENS");
        java.util.Set<AutomationController.Screen> roles=new java.util.HashSet<>();
        for(int i=0;i<recipes.length();i++) {
            JSONObject s=recipes.getJSONObject(i);
            AutomationController.Screen role=AutomationController.Screen.valueOf(s.getString("role"));
            if(role==AutomationController.Screen.UNKNOWN||!roles.add(role))throw new IllegalArgumentException("DUPLICATE_SCREEN");
            List<Path> forbidden=new ArrayList<>();JSONArray exclusions=s.getJSONArray("forbiddenControls");
            for(int j=0;j<exclusions.length();j++)forbidden.add(path(exclusions.getJSONObject(j)));
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
        String suffix=p.getString("resourceSuffix");if(!suffix.matches("[a-zA-Z0-9_]+"))throw new IllegalArgumentException("INVALID_RESOURCE");
        return new Path(java.util.Collections.unmodifiableList(children),suffix,p.getString("className"),p.optString("expectedText",""));
    }
    public Reading inspect(AccessibilityNodeInfo root,String pkg) {
        Recipe matched=null;
        for(Recipe s:screens)if(matches(root,pkg,s.signature())){if(matched!=null)return null;matched=s;}
        if(matched==null)return null;
        for(Path p:matched.forbidden())if(matches(root,pkg,p))return null;
        String phone=text(root,pkg,matched.phone()),receiver=text(root,pkg,matched.receiver());
        if(phone==null||receiver==null)return null;
        try{phone=Identity.canonicalPhone(phone);receiver=Identity.canonicalPhone(receiver);}catch(IllegalArgumentException badIdentity){return null;}
        boolean business=matches(root,pkg,matched.business()),regular=matches(root,pkg,matched.regular());
        boolean blocked=matches(root,pkg,matched.blocked()),unblocked=matches(root,pkg,matched.unblocked());
        if(business==regular||blocked==unblocked)return null;
        Path action=blocked?matched.unblock():matched.block();
        return new Reading(matched.screen(),phone,receiver,business?Kind.BUSINESS_CONFIRMED:Kind.REGULAR_PROFILE_OBSERVED,
            blocked?BlockState.BLOCKED:BlockState.UNBLOCKED,"",true,action);
    }
    public boolean click(AccessibilityNodeInfo root,String pkg,Reading reading) {
        AccessibilityNodeInfo node=node(root,pkg,reading.action());
        if(node==null)return false;
        return node.isClickable()&&node.isEnabled()&&node.isVisibleToUser()&&node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
    }
    private static boolean matches(AccessibilityNodeInfo root,String pkg,Path path){return node(root,pkg,path)!=null;}
    private static String text(AccessibilityNodeInfo root,String pkg,Path path){AccessibilityNodeInfo n=node(root,pkg,path);return n==null||n.getText()==null?null:n.getText().toString();}
    private static AccessibilityNodeInfo node(AccessibilityNodeInfo root,String pkg,Path path) {
        AccessibilityNodeInfo n=root;
        if(n==null||!pkg.contentEquals(n.getPackageName()==null?"":n.getPackageName()))return null;
        for(int index:path.children()) {
            if(n.getChildCount()>64||index>=n.getChildCount())return null;
            n=n.getChild(index);if(n==null||!pkg.contentEquals(n.getPackageName()==null?"":n.getPackageName()))return null;
        }
        if(!n.isVisibleToUser()||!n.isEnabled()||!(pkg+":id/"+path.resourceSuffix()).equals(n.getViewIdResourceName())
            ||!path.className().contentEquals(n.getClassName()==null?"":n.getClassName()))return null;
        if(!path.expectedText().isEmpty()&&!path.expectedText().contentEquals(n.getText()==null?"":n.getText()))return null;
        return n;
    }
}
