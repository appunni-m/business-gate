package io.github.appunnim.businessgate.policy;

import io.github.appunnim.businessgate.automation.BoundedNodes;
import io.github.appunnim.businessgate.automation.BoundedNodes.Ancestor;
import io.github.appunnim.businessgate.automation.BoundedNodes.Path;
import io.github.appunnim.businessgate.automation.BoundedNodes.ResourceOrigin;
import java.util.List;

/** Owned structural fixtures. These establish parser behavior, never live compatibility. */
public final class StructuralSuite {
    private static int assertions;
    private static void check(boolean condition,String label){assertions++;if(!condition)throw new AssertionError(label);}
    private static final String PKG="io.github.appunnim.businessgate.fixture";
    private static final class Node {
        String pkg=PKG,id,resourceNamespace,type="View",text="";boolean visible=true,throwOnChild;
        int calls,releases;List<Node> children=List.of();
        Node(String id,Node... children){this.id=id;this.children=List.of(children);}
    }
    private static final BoundedNodes.Access<Node> ACCESS=new BoundedNodes.Access<>(){
        public String packageName(Node n){return n.pkg;}
        public String resource(Node n){return n.id==null?null:(n.resourceNamespace==null?n.pkg:n.resourceNamespace)+":id/"+n.id;}
        public String className(Node n){return n.type;}
        public CharSequence text(Node n){return n.text;}
        public boolean visible(Node n){return n.visible;}
        public int childCount(Node n){return n.children.size();}
        public Node child(Node n,int index){n.calls++;if(n.throwOnChild)throw new IllegalStateException("SOURCE_UNAVAILABLE");return n.children.get(index);}
        public void release(Node n){n.releases++;}
    };
    private static Path path(String text){return new Path(List.of(0,0),"action","View",text,List.of(new Ancestor("root","View",2),new Ancestor("profile","View",1)));}
    private static boolean resolve(Node root,Path path){try(var nodes=new BoundedNodes<>(root,PKG,ACCESS,new BoundedNodes.Budget())){return nodes.resolve(path)!=null;}}
    public static void main(String[] args){
        Node action=new Node("action"),profile=new Node("profile",action),opaque=new Node("opaque"),root=new Node("root",profile,opaque);action.text="Block";opaque.throwOnChild=true;
        var budget=new BoundedNodes.Budget();BoundedNodes<Node> nodes=new BoundedNodes<>(root,PKG,ACCESS,budget);
        check(nodes.resolve(path("Block"))==action,"qualified exact control");
        for(int i=0;i<30;i++)check(nodes.resolve(path("Block"))==action,"same scope shares acquired path");
        check(budget.acquired()==3&&root.calls==1&&profile.calls==1,"shared prefixes avoid repeated node acquisition");
        check(opaque.calls==0&&opaque.releases==0,"unrelated subtree is never acquired or traversed");
        nodes.close();nodes.close();check(root.releases==1&&profile.releases==1&&action.releases==1,"scope releases each acquired handle exactly once");
        check(nodes.resolve(path("Block"))==null,"closed scope cannot supply stale nodes");
        for(Node changed:List.of(root,profile,action)){
            changed.visible=false;check(!resolve(root,path("Block")),"hidden ancestor or field rejected");changed.visible=true;
            changed.pkg="io.github.appunnim.businessgate.foreign";check(!resolve(root,path("Block")),"foreign ancestor or field rejected");changed.pkg=PKG;
            changed.type="Replacement";check(!resolve(root,path("Block")),"class substitution rejected");changed.type="View";
            String id=changed.id;changed.id="replacement";check(!resolve(root,path("Block")),"resource substitution rejected");changed.id=id;
        }
        profile.children=List.of(action,new Node("action"));check(!resolve(root,path("Block")),"extra duplicate control changes qualified structure");profile.children=List.of(action);
        action.text="Report and block";check(!resolve(root,path("Block")),"different side-effect label rejected");
        action.text="\uD83D\uDD35".repeat(160);check(resolve(root,path(action.text)),"160 code points accepted without splitting surrogate pairs");
        action.text="x".repeat(161);check(!resolve(root,path(action.text)),"oversized captured label rejected");action.text="Block";
        check(!resolve(root,new Path(List.of(0),"action","View","",List.of())),"missing ancestry rejected");
        check(!resolve(root,new Path(List.of(-1),"action","View","",List.of(new Ancestor("root","View",2)))),"negative child index rejected");
        check(!resolve(root,new Path(java.util.Collections.nCopies(13,0),"action","View","",java.util.Collections.nCopies(13,new Ancestor("root","View",2)))),"unbounded path rejected");
        check(!resolve(null,path("Block")),"missing active root abstains");
        BoundedNodes.Budget shared=new BoundedNodes.Budget();
        for(int i=0;i<249;i++){try(var scope=new BoundedNodes<>(new Node("root"),PKG,ACCESS,shared)){check(!shared.exhausted(),"callback budget remains available below ceiling");}}
        int calls=root.calls,releases=root.releases;
        try(var scope=new BoundedNodes<>(root,PKG,ACCESS,shared)){
            check(scope.resolve(path("Block"))==null,"needed path beyond callback budget abstains");
            check(shared.acquired()==250&&shared.exhausted()&&root.calls==calls,"budget exhaustion performs no excess child request");
        }
        check(root.releases==releases+1,"budget rejection still releases acquired root");
        try(var scope=new BoundedNodes<>(root,PKG,ACCESS,shared)){check(scope.resolve(path("Block"))==null,"exhaustion stays latched across inspection scopes");}
        check(root.releases==releases+2,"root returned after budget exhaustion is released");
        profile.throwOnChild=true;releases=root.releases;
        try(var scope=new BoundedNodes<>(root,PKG,ACCESS,new BoundedNodes.Budget())){scope.resolve(path("Block"));throw new AssertionError("Missing source failure");}
        catch(IllegalStateException expected){check(root.releases==releases+1,"source exception closes previously acquired handles");}
        Node nativeControl=new Node("button1"),content=new Node("content",nativeControl),decor=new Node(null,content);
        nativeControl.text="Block";nativeControl.resourceNamespace="android";content.resourceNamespace="android";
        Path nativePath=new Path(List.of(0,0),"button1","View","Block",List.of(
            new Ancestor("","View",1,ResourceOrigin.NONE),new Ancestor("content","View",1,ResourceOrigin.ANDROID)),ResourceOrigin.ANDROID);
        check(resolve(decor,nativePath),"explicit absent root and framework resources resolve in selected package");
        decor.id="unexpected";check(!resolve(decor,nativePath),"absent ancestor contract rejects a newly supplied ID");decor.id=null;
        content.resourceNamespace=null;check(!resolve(decor,nativePath),"same suffix in selected namespace cannot impersonate framework ancestor");content.resourceNamespace="android";
        nativeControl.resourceNamespace=null;check(!resolve(decor,nativePath),"same suffix in selected namespace cannot impersonate framework control");nativeControl.resourceNamespace="android";
        content.pkg="android";check(!resolve(decor,nativePath),"framework ID does not permit a foreign owning package");content.pkg=PKG;
        content.type="Different";check(!resolve(decor,nativePath),"framework ancestor class remains exact");content.type="View";
        content.children=List.of(nativeControl,new Node("extra"));check(!resolve(decor,nativePath),"framework ancestor count remains exact");content.children=List.of(nativeControl);
        check(!resolve(decor,new Path(List.of(),"","View","",List.of(),ResourceOrigin.NONE)),"an absent resource is not an eligible field or control");
        check(!resolve(decor,new Path(List.of(0),"content","View","",List.of(new Ancestor("wrong","View",1,ResourceOrigin.NONE)),ResourceOrigin.ANDROID)),"absent origin requires an empty expected suffix");
        nativeControl.id=null;check(!resolve(decor,nativePath),"framework field cannot lose its required resource");
        System.out.println("PASS "+assertions+" owned structural path, acquisition-budget and lifetime assertions");
    }
}
