package io.github.appunnim.businessgate.support;

import android.accessibilityservice.AccessibilityService;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Button;

/** A visible Stop control that moves out of the next action's bounds before dispatch. */
public final class SessionOverlay {
    private final AccessibilityService service;
    private Button view;
    public SessionOverlay(AccessibilityService service){this.service=service;}
    public boolean show(Runnable stop){
        hide();
        try{
            view=new Button(service);view.setText(io.github.appunnim.businessgate.R.string.session_stop);view.setContentDescription("Stop applying choices immediately");
            view.setMinHeight((int)(48*service.getResources().getDisplayMetrics().density));view.setOnClickListener(v->stop.run());
            WindowManager.LayoutParams p=new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT);
            p.gravity=Gravity.TOP|Gravity.CENTER_HORIZONTAL;
            service.getSystemService(WindowManager.class).addView(view,p);return true;
        }catch(RuntimeException error){view=null;return false;}
    }
    public boolean visible(){return view!=null&&view.isAttachedToWindow();}
    @SuppressWarnings("deprecation") public boolean ownsWindow(int id){
        if(!visible())return false;var node=view.createAccessibilityNodeInfo();try{return node.getWindowId()==id;}finally{node.recycle();}
    }
    /** Return false during relocation; the route must re-inspect on its next bounded tick. */
    public boolean prepare(android.graphics.Rect action){
        if(!visible()||action.isEmpty())return false;
        if(clearOf(action))return true;
        var params=(WindowManager.LayoutParams)view.getLayoutParams();
        params.gravity=params.gravity==(Gravity.TOP|Gravity.CENTER_HORIZONTAL)?Gravity.BOTTOM|Gravity.END:Gravity.TOP|Gravity.CENTER_HORIZONTAL;
        try{service.getSystemService(WindowManager.class).updateViewLayout(view,params);}catch(RuntimeException unavailable){return false;}
        return false;
    }
    public boolean clearOf(android.graphics.Rect target){
        if(!visible()||!view.isShown()||target.isEmpty())return false;
        int[] position=new int[2];view.getLocationOnScreen(position);
        android.graphics.Rect bounds=new android.graphics.Rect(position[0],position[1],position[0]+view.getWidth(),position[1]+view.getHeight());
        return !bounds.isEmpty()&&!android.graphics.Rect.intersects(bounds,target);
    }
    public void hide(){if(view!=null){try{service.getSystemService(WindowManager.class).removeView(view);}catch(RuntimeException ignored){/* Already removed by system. */}view=null;}}
}
