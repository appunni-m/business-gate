package io.github.appunnim.businessgate.automation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Callback-scoped exact paths only; never discovers or walks an arbitrary subtree. */
public final class BoundedNodes<N> implements AutoCloseable {
    public record Ancestor(String resourceSuffix,String className,int childCount) {}
    public record Path(List<Integer> children,String resourceSuffix,String className,String expectedText,List<Ancestor> ancestors) {
        public Path { children=List.copyOf(children);ancestors=List.copyOf(ancestors); }
    }
    public interface Access<N> {
        String packageName(N node);
        String resource(N node);
        String className(N node);
        CharSequence text(N node);
        boolean visible(N node);
        int childCount(N node);
        N child(N node,int index);
        void release(N node);
    }
    public static final class Budget {
        public static final int LIMIT=250;
        private int acquired;
        private boolean exhausted;
        private boolean take(){if(exhausted||acquired>=LIMIT){exhausted=true;return false;}acquired++;return true;}
        public int acquired(){return acquired;}
        public boolean exhausted(){return exhausted;}
    }
    private final Access<N> access;
    private final Budget budget;
    private final String pkg;
    private final Map<List<Integer>,N> cache=new HashMap<>();
    private final Map<N,Boolean> owned=new IdentityHashMap<>();
    private boolean closed;
    public BoundedNodes(N root,String pkg,Access<N> access,Budget budget){
        this.access=access;this.budget=budget;this.pkg=pkg;
        if(root!=null){owned.put(root,true);if(budget.take())cache.put(List.of(),root);}
    }
    public N resolve(Path path){
        if(closed||budget.exhausted()||path.children().size()>12||path.ancestors().size()!=path.children().size())return null;
        N node=cache.get(List.of());List<Integer> prefix=new ArrayList<>();
        for(int depth=0;depth<path.children().size();depth++){
            Ancestor ancestor=path.ancestors().get(depth);int index=path.children().get(depth);
            if(!matches(node,ancestor.resourceSuffix(),ancestor.className())||ancestor.childCount()<1||ancestor.childCount()>64
                ||index<0||index>=ancestor.childCount()||access.childCount(node)!=ancestor.childCount())return null;
            prefix.add(index);List<Integer> key=List.copyOf(prefix);
            if(!cache.containsKey(key)){
                if(!budget.take())return null;
                N child=access.child(node,index);cache.put(key,child);if(child!=null)owned.put(child,true);
            }
            node=cache.get(key);
        }
        if(!matches(node,path.resourceSuffix(),path.className()))return null;
        if(!path.expectedText().isEmpty()){
            CharSequence label=access.text(node);
            if(label==null||label.length()>320||Character.codePointCount(label,0,label.length())>160||!path.expectedText().contentEquals(label))return null;
        }
        return node;
    }
    private boolean matches(N node,String suffix,String type){
        return node!=null&&pkg.equals(access.packageName(node))&&access.visible(node)
            &&(pkg+":id/"+suffix).equals(access.resource(node))&&type.equals(access.className(node));
    }
    @Override public void close(){
        if(closed)return;closed=true;
        for(N node:owned.keySet())access.release(node);
        owned.clear();cache.clear();
    }
}
