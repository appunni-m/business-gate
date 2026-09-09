package io.github.appunnim.businessgate.support;

import android.accessibilityservice.AccessibilityService;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Button;

/** A non-obscuring location must be demonstrated in each adapter's physical evidence. */
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
    public void hide(){if(view!=null){try{service.getSystemService(WindowManager.class).removeView(view);}catch(RuntimeException ignored){/* Already removed by system. */}view=null;}}
}
