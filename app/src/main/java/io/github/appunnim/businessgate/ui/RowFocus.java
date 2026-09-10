package io.github.appunnim.businessgate.ui;

import android.view.View;
import android.view.ViewGroup;
import io.github.appunnim.businessgate.R;

/** Keyboard presentation only. Screen readers retain their own accessibility focus. */
final class RowFocus {
    record Key(String data,long namespace,String phone,String role){}
    private record Request(Key key){}
    private final ViewGroup root;
    private final View fallback;
    private final android.widget.ListView list;
    private Request input;
    private boolean restoring,binding;
    private String data="";
    private long namespace=-1;
    private int inputAttempts;

    RowFocus(ViewGroup root,View fallback,android.widget.ListView list){
        this.root=root;this.fallback=fallback;this.list=list;
        root.getViewTreeObserver().addOnGlobalFocusChangeListener((old,now)->{
            if(key(now)!=null&&!root.isInLayout())alignSelection(now);
            if(!restoring&&!binding&&!root.isInLayout()&&now!=null&&input!=null&&!input.key().equals(key(now)))input=null;
        });
        root.getViewTreeObserver().addOnPreDrawListener(()->{restore();return true;});
    }
    void context(String identity,long receiver){
        if(!data.equals(identity)||namespace!=receiver){cancel();data=identity;namespace=receiver;}
    }
    void cancel(){input=null;inputAttempts=0;}
    void capture(){
        View current=root.findFocus();Key key=key(current);
        if(key!=null&&current.isAttachedToWindow()){input=new Request(key);inputAttempts=0;}
    }
    void binding(boolean value){binding=value;}
    boolean tab(boolean backward){
        View current=root.findFocus();Key identity=key(current);
        if(!root.hasWindowFocus()||identity==null||!identity.data().equals(data)||identity.namespace()!=namespace)return false;
        View row=current;while(row.getParent() instanceof View parent&&parent!=list)row=parent;
        if(row.getParent()!=list)return false;
        // ListView maps Tab to vertical row navigation. Visit the native controls within this
        // row first; leave row boundaries and scrolling to ListView. This changes input focus only.
        java.util.ArrayList<View> controls=row.getFocusables(View.FOCUS_FORWARD);
        int index=controls.indexOf(current),next=index+(backward?-1:1);
        if(index<0||next<0||next>=controls.size())return false;
        cancel();return controls.get(next).requestFocus(backward?View.FOCUS_BACKWARD:View.FOCUS_FORWARD);
    }
    void mark(View view,String phone,String role){
        view.setTag(R.id.row_focus,new Key(data,namespace,phone,role));
    }
    private static Key key(View view){return view!=null&&view.getTag(R.id.row_focus) instanceof Key k?k:null;}
    private boolean current(Request request){return request.key().data().equals(data)&&request.key().namespace()==namespace;}
    private View target(Request request){
        if(!current(request))return fallback;
        View exact=find(root,v->request.key().equals(key(v)));
        if(exact!=null)return exact;
        Key details=new Key(data,namespace,request.key().phone(),"details");
        View row=find(root,v->details.equals(key(v)));
        return row==null?fallback:row;
    }
    private void alignSelection(View control){
        Key identity=key(control);if(identity==null||!identity.data().equals(data)||identity.namespace()!=namespace)return;
        View row=control;
        while(row.getParent() instanceof View parent&&parent!=list)row=parent;
        if(row.getParent()!=list)return;
        int position=list.getPositionForView(row);
        if(position>=0&&position!=list.getSelectedItemPosition())list.setSelectionFromTop(position,row.getTop());
    }
    private void restore(){
        if(!root.hasWindowFocus()){cancel();return;}
        restoring=true;
        try{
            if(input!=null){
                View target=target(input);alignSelection(target);input=key(target)==null?null:new Request(key(target));
                if(target!=root.findFocus()&&(inputAttempts++>=2||!target.requestFocus())){input=null;fallback.requestFocus();}
            }
        }finally{restoring=false;}
    }
    private static View find(View view,java.util.function.Predicate<View> match){
        if(view.isShown()&&match.test(view))return view;
        if(view instanceof ViewGroup group)for(int i=0;i<group.getChildCount();i++){View found=find(group.getChildAt(i),match);if(found!=null)return found;}
        return null;
    }
}
