package io.github.appunnim.businessgate.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import io.github.appunnim.businessgate.GateApplication;
import io.github.appunnim.businessgate.automation.AdapterRegistry;
import io.github.appunnim.businessgate.automation.AutomationController;
import io.github.appunnim.businessgate.automation.QualifiedAdapter;
import io.github.appunnim.businessgate.data.GateRepository;
import io.github.appunnim.businessgate.policy.GuardFacts;
import io.github.appunnim.businessgate.policy.GuardFacts.Guard;
import io.github.appunnim.businessgate.policy.Model.*;
import io.github.appunnim.businessgate.policy.RuleEngine;
import io.github.appunnim.businessgate.support.SessionOverlay;
import java.nio.charset.StandardCharsets;

/** User-started finite sessions; unsupported installations never enter the event filter. */
public final class GateAccessibilityService extends AccessibilityService {
    private static GateAccessibilityService connected;
    private GateRepository repository;
    private SessionOverlay overlay;
    private final AutomationController controller=new AutomationController();
    private final Handler handler=new Handler(Looper.getMainLooper());
    private long generation,deadline,activeAccount=-1,candidateAt;
    private String targetPackage="",reason="NO_CONNECTION",pendingObservation="";
    private Binding candidate;
    private boolean circuitOpen,requested,arming;
    private final Runnable repositoryChanged=()->{
        if(repository==null)return;
        if(!repository.consented()){
            candidate=null;
            boolean active=requested||activeAccount>=0||!targetPackage.isEmpty();
            targetPackage="";filter("io.github.appunnim.businessgate.disabled");
            if(active)stop();
            reason="CONSENT_REQUIRED";return;
        }
        if(activeAccount>=0&&(repository.disarmed()||repository.vetoed(activeAccount)))stop();
        else if(requested&&!arming)inspectIdleProfile();
    };
    private final BroadcastReceiver screenOff=new BroadcastReceiver(){@Override public void onReceive(Context context,Intent intent){stop();}};
    public static boolean connected(){return connected!=null;}
    public static void stopNow(){if(connected!=null)connected.stop();}
    public static String status(){return connected==null?"SCREEN_ACCESS_OFF":connected.reason;}
    public static String packageSelected(){return connected==null?"":connected.targetPackage;}
    public static boolean selectInstallation(String pkg){return connected!=null&&connected.select(pkg);}
    public static Binding connectionCandidate(){
        if(connected==null||connected.candidate==null||SystemClock.elapsedRealtime()-connected.candidateAt>60_000)return null;
        return connected.candidate;
    }
    public static boolean requestApply(){return connected!=null&&connected.request();}
    private GateApplication app(){return (GateApplication)getApplication();}
    @Override protected void onServiceConnected(){
        connected=this;repository=app().repository();overlay=new SessionOverlay(this);repository.emergencyStop();repository.addListener(repositoryChanged);
        filter("io.github.appunnim.businessgate.disabled");
        if(Build.VERSION.SDK_INT>=33)registerReceiver(screenOff,new IntentFilter(Intent.ACTION_SCREEN_OFF),Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(screenOff,new IntentFilter(Intent.ACTION_SCREEN_OFF));
    }
    private void filter(String pkg){AccessibilityServiceInfo info=getServiceInfo();info.packageNames=new String[]{pkg};setServiceInfo(info);}
    private boolean select(String pkg){
        stop();candidate=null;targetPackage="";filter("io.github.appunnim.businessgate.disabled");
        if(!repository.consented()||app().registry().resolve(this,pkg)==null){reason="ENVIRONMENT_UNSUPPORTED";return false;}
        targetPackage=pkg;filter(pkg);reason="OPEN_SUPPORTED_PROFILE";return true;
    }
    private boolean request(){
        if(repository==null||!repository.consented()||!repository.current().binding().bound()||app().registry().resolve(this,targetPackage)==null)return false;
        stop();requested=true;deadline=SystemClock.elapsedRealtime()+25_000;long owner=++generation;reason="WAITING_FOR_PROFILE";
        if(!overlay.show(this::stop)){stop();reason="STOP_CONTROL_UNAVAILABLE";return false;}
        app().sessionAttention(true);handler.postDelayed(()->{if(generation==owner)stop();},25_000);return true;
    }
    @Override public void onAccessibilityEvent(AccessibilityEvent event){
        if(repository==null||!repository.consented()||event.getPackageName()==null)return;
        if(!targetPackage.contentEquals(event.getPackageName())){if(activeAccount!=-1)stop();return;}
        QualifiedAdapter adapter=app().registry().resolve(this,targetPackage);
        if(adapter==null){stop();reason="ENVIRONMENT_CHANGED";filter("io.github.appunnim.businessgate.disabled");return;}
        // No event-origin exemption is assumed. Own-action attribution awaits measured qualification.
        if(event.getEventType()==AccessibilityEvent.TYPE_VIEW_CLICKED||event.getEventType()==AccessibilityEvent.TYPE_VIEW_SCROLLED){
            if(activeAccount!=-1){stop();reason="INTERACTION_STOPPED";}return;
        }
        if(activeAccount!=-1){controller.onEvent(port);return;}
        inspectIdleProfile();
    }
    private void inspectIdleProfile(){
        if(repository==null||!repository.consented()||targetPackage.isEmpty())return;
        QualifiedAdapter adapter=app().registry().resolve(this,targetPackage);if(adapter==null)return;
        AccessibilityNodeInfo root=getRootInActiveWindow();QualifiedAdapter.Reading read=adapter.inspect(root,targetPackage);
        if(read==null||read.screen()!=AutomationController.Screen.PROFILE)return;
        candidate=new Binding(repository.installation(),AdapterRegistry.sha256(targetPackage.getBytes(StandardCharsets.UTF_8)),"user-"+android.os.Process.myUserHandle().hashCode(),read.receiver(),adapter.id());
        candidateAt=SystemClock.elapsedRealtime();
        if(!candidate.equals(repository.current().binding())){if(requested)stop();reason="RECEIVER_REVIEW_REQUIRED";return;}
        if(requested&&!arming){
            if(SystemClock.elapsedRealtime()>=deadline){stop();return;}
            Account account=repository.current().accounts().stream().filter(a->a.phone().equals(read.phone())).findFirst().orElse(null);
            if(account==null){
                reason="CHECKING_PROFILE";
                if(!pendingObservation.equals(read.phone())){pendingObservation=read.phone();repository.observe(evidence(read,adapter,root),read.name());}
                return;
            }
            long owner=generation;arming=true;
            Readiness ready=new Readiness(repository.current().namespace(),repository.current().globalRevision(),repository.epoch(),SystemClock.elapsedRealtime(),candidate);
            repository.arm(ready,()->{
                arming=false;if(owner!=generation||!requested||SystemClock.elapsedRealtime()>=deadline){repository.emergencyStop();return;}
                if(!account.pending()){finishSession();reason="RULE_READY";return;}
                circuitOpen=false;activeAccount=account.id();requested=false;reason="APPLYING";controller.start(activeAccount,SystemClock.elapsedRealtime(),generation);controller.onEvent(port);
            });
        }else if(!requested)repository.observe(evidence(read,adapter,root),read.name());
    }
    private Evidence evidence(QualifiedAdapter.Reading reading,QualifiedAdapter adapter,AccessibilityNodeInfo root){
        return new Evidence(repository.current().namespace(),reading.phone(),reading.kind(),reading.blockState(),SystemClock.elapsedRealtime(),generation,
            root.getWindowId(),reading.receiver(),adapter.id(),reading.screen()==AutomationController.Screen.PROFILE||reading.screen()==AutomationController.Screen.BLOCK_DIALOG||reading.screen()==AutomationController.Screen.UNBLOCK_DIALOG,reading.safe());
    }
    private AutomationController.Frame inspect(){
        if(activeAccount<0||!repository.consented())return null;
        Snapshot s=repository.current();Account account=s.account(activeAccount);if(account==null)return null;
        QualifiedAdapter adapter=app().registry().resolve(this,targetPackage);if(adapter==null)return null;
        AccessibilityNodeInfo root=getRootInActiveWindow();QualifiedAdapter.Reading read=adapter.inspect(root,targetPackage);if(read==null)return null;
        boolean foreground=false,windowSafe=true;Rect target=new Rect();root.getBoundsInScreen(target);
        for(var window:getWindows()){
            if(window.getId()==root.getWindowId())foreground=window.isActive()&&window.isFocused();
            else if(!overlay.ownsWindow(window.getId())){Rect bounds=new Rect();window.getBoundsInScreen(bounds);if(Rect.intersects(target,bounds))windowSafe=false;}
        }
        boolean receiver=s.binding().bound()&&s.binding().receiver().equals(read.receiver())&&s.binding().adapter().equals(adapter.id())
            &&s.binding().installation().equals(repository.installation())&&s.binding().packageDigest().equals(AdapterRegistry.sha256(targetPackage.getBytes(StandardCharsets.UTF_8)));
        GuardFacts facts=new GuardFacts().record(Guard.CONNECTED,connected==this)
            .record(Guard.SIGNATURE,app().registry().resolve(this,targetPackage)!=null).record(Guard.BUILD,adapter!=null)
            .record(Guard.LANGUAGE,adapter.languageMatches(root,targetPackage,read)).record(Guard.FOREGROUND,foreground&&targetPackage.contentEquals(root.getPackageName()==null?"":root.getPackageName()))
            .record(Guard.INTERACTIVE,getSystemService(PowerManager.class).isInteractive()).record(Guard.UNLOCKED,!getSystemService(KeyguardManager.class).isKeyguardLocked())
            .record(Guard.RECEIVER,receiver).record(Guard.PROFILE,account.phone().equals(read.phone())&&account.namespace()==s.namespace())
            .record(Guard.WINDOW,windowSafe&&root.isVisibleToUser()).record(Guard.OVERLAY,overlay.clearOf(adapter.actionBounds(root,targetPackage,read))&&overlay.clearOf(adapter.identityBounds(root,targetPackage,read)))
            .record(Guard.NO_STOP,!repository.disarmed()).record(Guard.NO_ALLOW_VETO,!repository.vetoed(activeAccount))
            .record(Guard.CIRCUIT,!circuitOpen).record(Guard.LEASE,SystemClock.elapsedRealtime()<deadline)
            .record(Guard.STORAGE,s.loaded()&&s.error().isEmpty());
        return new AutomationController.Frame(read.screen(),evidence(read,adapter,root),new RuleEngine.Context(facts.bits(),SystemClock.elapsedRealtime(),System.currentTimeMillis(),generation,s.globalRevision(),account.revision(),deadline),adapter.actionable(root,targetPackage,read));
    }
    private final AutomationController.Port port=new AutomationController.Port(){
        @Override public Snapshot policy(){return repository.current();}
        @Override public AutomationController.Frame inspect(){return GateAccessibilityService.this.inspect();}
        @Override public void journal(AutomationController.Plan plan,Runnable success,Runnable failed){repository.journal(plan,success,failed);}
        @Override public boolean click(AutomationController.Control control,AutomationController.Frame checked){
            AutomationController.Frame fresh=inspect();Snapshot s=repository.current();Account account=s.account(activeAccount);
            if(fresh==null||account==null||fresh.screen()!=checked.screen()||fresh.evidence().windowId()!=checked.evidence().windowId()
                ||!fresh.evidence().phone().equals(checked.evidence().phone())||!fresh.evidence().receiver().equals(checked.evidence().receiver())
                ||!fresh.evidence().adapter().equals(checked.evidence().adapter())||fresh.context().expectedAccount()!=checked.context().expectedAccount()
                ||fresh.context().expectedGlobal()!=checked.context().expectedGlobal()||fresh.context().generation()!=checked.context().generation()
                ||new RuleEngine().evaluate(s,account,fresh.evidence(),fresh.context()).action()==Action.NONE)return false;
            QualifiedAdapter adapter=app().registry().resolve(GateAccessibilityService.this,targetPackage);if(adapter==null)return false;
            AccessibilityNodeInfo root=getRootInActiveWindow();QualifiedAdapter.Reading reading=adapter.inspect(root,targetPackage);
            return reading!=null&&reading.screen()==fresh.screen()&&root.getWindowId()==fresh.evidence().windowId()&&reading.phone().equals(account.phone())
                &&reading.receiver().equals(s.binding().receiver())&&overlay.clearOf(adapter.actionBounds(root,targetPackage,reading))&&overlay.clearOf(adapter.identityBounds(root,targetPackage,reading))
                &&!repository.disarmed()&&!repository.vetoed(activeAccount)&&adapter.click(root,targetPackage,reading,control);
        }
        @Override public void verified(AutomationController.Plan plan,Evidence evidence,java.util.function.Consumer<AutomationController.CommitResult> result){repository.verified(plan,evidence,result);}
        @Override public void completed(){finishSession();reason="VERIFIED";}
        @Override public void stopped(boolean uncertain){if(uncertain)circuitOpen=true;finishSession();repository.emergencyStop();reason=uncertain?"RESULT_UNVERIFIED":"STOPPED";}
    };
    private void finishSession(){if(overlay!=null)overlay.hide();activeAccount=-1;requested=false;arming=false;pendingObservation="";handler.removeCallbacksAndMessages(null);app().sessionAttention(false);}
    private void stop(){if(repository==null)return;generation++;controller.stop(port);finishSession();repository.emergencyStop();}
    @Override public void onInterrupt(){stop();}
    @Override public void onDestroy(){stop();if(repository!=null)repository.removeListener(repositoryChanged);try{unregisterReceiver(screenOff);}catch(IllegalArgumentException ignored){/* Connection was never completed. */}connected=null;super.onDestroy();}
}
