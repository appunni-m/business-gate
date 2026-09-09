package io.github.appunnim.businessgate.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.KeyguardManager;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import io.github.appunnim.businessgate.GateApplication;
import io.github.appunnim.businessgate.automation.AutomationController;
import io.github.appunnim.businessgate.automation.QualifiedAdapter;
import io.github.appunnim.businessgate.data.GateRepository;
import io.github.appunnim.businessgate.policy.Model.*;
import io.github.appunnim.businessgate.policy.RuleEngine;
import io.github.appunnim.businessgate.support.SessionOverlay;

/** Production starts observe-only. No broad event subscription or foreground takeover. */
public final class GateAccessibilityService extends AccessibilityService {
    private static GateAccessibilityService connected;
    private GateRepository repository;
    private SessionOverlay overlay;
    private final AutomationController controller=new AutomationController();
    private final Handler handler=new Handler(Looper.getMainLooper());
    private long generation,deadline,activeAccount=-1;
    private String targetPackage="",receiver="";
    private boolean circuitOpen;
    public static boolean connected(){return connected!=null;}
    public static void stopNow(){if(connected!=null)connected.stop();}
    @Override protected void onServiceConnected(){
        connected=this;repository=((GateApplication)getApplication()).repository();overlay=new SessionOverlay(this);
        repository.emergencyStop();
        // Until a physical qualification exists the platform filter targets an inert package.
        AccessibilityServiceInfo info=getServiceInfo();info.packageNames=new String[]{"io.github.appunnim.businessgate.disabled"};setServiceInfo(info);
    }
    @Override public void onAccessibilityEvent(AccessibilityEvent event){
        if(repository==null||!repository.current().consent()||event.getPackageName()==null)return;
        String pkg=event.getPackageName().toString();
        if(!pkg.equals(targetPackage)){if(activeAccount!=-1)stop();return;}
        QualifiedAdapter adapter=((GateApplication)getApplication()).registry().resolve(this,pkg);
        if(adapter==null){stop();return;}
        if(event.getEventType()==AccessibilityEvent.TYPE_VIEW_CLICKED||event.getEventType()==AccessibilityEvent.TYPE_VIEW_SCROLLED){stop();return;}
        if(activeAccount!=-1)controller.onEvent(port);
        else{
            AccessibilityNodeInfo root=getRootInActiveWindow();
            QualifiedAdapter.Reading read=adapter.inspect(root,pkg);
            if(read!=null&&read.receiver().equals(receiver))repository.observe(evidence(read,adapter,root),read.name(),true);
        }
    }
    // Physical qualification must also bind this receiver to the durable namespace.
    // No UI entry arms this method in the unqualified distribution.
    public boolean startQualifiedCurrentProfile(long accountId,String pkg,String verifiedReceiver){
        if(repository==null||repository.disarmed()||circuitOpen||!repository.current().consent()
            ||((GateApplication)getApplication()).registry().resolve(this,pkg)==null)return false;
        Account a=repository.current().account(accountId);if(a==null)return false;
        targetPackage=pkg;receiver=verifiedReceiver;activeAccount=accountId;generation++;deadline=SystemClock.elapsedRealtime()+25000;
        if(!overlay.show(this::stop)){stop();return false;}
        controller.start(accountId,SystemClock.elapsedRealtime(),generation);
        handler.postDelayed(this::stop,25000);controller.onEvent(port);return true;
    }
    private Evidence evidence(QualifiedAdapter.Reading r,QualifiedAdapter adapter,AccessibilityNodeInfo root){
        return new Evidence(repository.current().namespace(),r.phone(),r.kind(),r.blockState(),SystemClock.elapsedRealtime(),generation,
            root.getWindowId(),r.receiver(),adapter.id(),true,r.safe());
    }
    private final AutomationController.Port port=new AutomationController.Port(){
        @Override public Snapshot policy(){return repository.current();}
        @Override public AutomationController.Frame inspect(){
            if(activeAccount<0||repository.disarmed()||circuitOpen||!overlay.visible())return null;
            QualifiedAdapter adapter=((GateApplication)getApplication()).registry().resolve(GateAccessibilityService.this,targetPackage);
            if(adapter==null)return null;
            AccessibilityNodeInfo root=getRootInActiveWindow();if(root==null)return null;
            QualifiedAdapter.Reading r=adapter.inspect(root,targetPackage);if(r==null||!receiver.equals(r.receiver()))return null;
            Account a=repository.current().account(activeAccount);if(a==null)return null;
            boolean safe=getSystemService(PowerManager.class).isInteractive()&&!getSystemService(KeyguardManager.class).isKeyguardLocked()
                &&root.isVisibleToUser()&&!repository.vetoed(activeAccount);
            // Overlap with any system/IME window is rejected; overlay location itself needs measured proof.
            for(var window:getWindows())if(window.getType()!=android.view.accessibility.AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY
                &&window.getId()!=root.getWindowId())safe=false;
            RuleEngine.Context c=new RuleEngine.Context(safe?RuleEngine.ALL_GUARDS:0,SystemClock.elapsedRealtime(),System.currentTimeMillis(),generation,
                repository.current().globalRevision(),a.revision(),deadline);
            return new AutomationController.Frame(r.screen(),evidence(r,adapter,root),c,true);
        }
        @Override public void journal(AutomationController.Plan p,Runnable success,Runnable failed){repository.journal(p,success,failed);}
        @Override public boolean click(AutomationController.Control control,AutomationController.Frame checked){
            QualifiedAdapter adapter=((GateApplication)getApplication()).registry().resolve(GateAccessibilityService.this,targetPackage);
            if(adapter==null||repository.disarmed()||repository.vetoed(activeAccount))return false;
            AccessibilityNodeInfo root=getRootInActiveWindow();QualifiedAdapter.Reading reading=adapter.inspect(root,targetPackage);
            return reading!=null&&reading.phone().equals(checked.evidence().phone())&&reading.receiver().equals(receiver)
                &&root.getWindowId()==checked.evidence().windowId()&&adapter.click(root,targetPackage,reading);
        }
        @Override public void verified(AutomationController.Plan p,Evidence e){repository.verified(p,e);overlay.hide();activeAccount=-1;handler.removeCallbacksAndMessages(null);}
        @Override public void stopped(boolean uncertain){if(uncertain)circuitOpen=true;overlay.hide();activeAccount=-1;repository.emergencyStop();}
    };
    private void stop(){if(repository==null)return;generation++;handler.removeCallbacksAndMessages(null);controller.stop(port);repository.emergencyStop();}
    @Override public void onInterrupt(){stop();}
    @Override public void onDestroy(){stop();connected=null;super.onDestroy();}
}
