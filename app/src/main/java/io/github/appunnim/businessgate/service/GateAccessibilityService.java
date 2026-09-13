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
import io.github.appunnim.businessgate.automation.BoundedNodes;
import io.github.appunnim.businessgate.automation.FinalDispatch;
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
    private String targetPackage="",reason="NO_CONNECTION",pendingObservation="",requestedPhone="";
    private Binding candidate;
    private MeasuredSession measuredSession;
    private boolean requested,arming,checkingCompatibility,activatingRule;
    private Readiness sessionScope;
    private BoundedNodes.Budget callbackBudget;
    private void callback(Runnable work){
        boolean owner=callbackBudget==null;if(owner)callbackBudget=new BoundedNodes.Budget();
        try{work.run();}finally{if(owner)callbackBudget=null;}
    }
    private final Runnable repositoryChanged=()->callback(()->{
        if(repository==null)return;
        if(!repository.consented()){
            candidate=null;
            boolean active=requested||activeAccount>=0||!targetPackage.isEmpty();
            targetPackage="";filter("io.github.appunnim.businessgate.disabled");
            if(active)stop();
            reason="CONSENT_REQUIRED";return;
        }
        if(measuredSession!=null)return;
        if(activeAccount>=0&&(repository.disarmed()||repository.vetoed(activeAccount)))stop();
        else if(requested&&!arming)inspectIdleProfile();
    });
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
    public static boolean requestConnection(){return connected!=null&&connected.requestMeasured("",true,false,false);}
    public static boolean requestInspect(String phone){
        try{return connected!=null&&connected.requestMeasured(io.github.appunnim.businessgate.policy.Identity.canonicalPhone(phone),false,true,false);}
        catch(IllegalArgumentException invalid){return false;}
    }
    public static boolean requestApply(long accountId){return connected!=null&&connected.request(false,false,accountId);}
    public static boolean requestActivation(){return connected!=null&&connected.request(false,true,-1);}
    public static boolean requestCompatibilityCheck(){return connected!=null&&connected.request(true,false,-1);}
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
        if(!repository.consented()||!app().registry().supported(this,pkg)){reason="ENVIRONMENT_UNSUPPORTED";return false;}
        targetPackage=pkg;filter(pkg);reason="OPEN_SUPPORTED_PROFILE";return true;
    }
    private boolean request(boolean checkOnly,boolean activateRule,long accountId){
        if(app().registry().measuredRoute(this,targetPackage)!=null){
            Account target=accountId<0?repository.current().accounts().stream().filter(Account::pending).findFirst().orElse(null):repository.current().account(accountId);
            if(accountId>=0&&target==null)return false;
            return requestMeasured(target==null?"":target.phone(),false,activateRule,checkOnly);
        }
        if(repository==null||!repository.consented()||!repository.current().binding().bound()||app().registry().resolve(this,targetPackage)==null)return false;
        Account target=accountId<0?null:repository.current().account(accountId);if(accountId>=0&&target==null)return false;
        if(repository.current().circuitOpen()&&!checkOnly){reason="COMPATIBILITY_CHECK_REQUIRED";return false;}
        stop();requested=true;checkingCompatibility=checkOnly;activatingRule=activateRule;deadline=SystemClock.elapsedRealtime()+25_000;long owner=++generation;reason="WAITING_FOR_PROFILE";
        requestedPhone=target==null?"":target.phone();
        Snapshot scope=repository.current();sessionScope=new Readiness(scope.namespace(),scope.globalRevision(),repository.epoch(),SystemClock.elapsedRealtime(),scope.binding());
        if(!overlay.show(this::stop)){stop();reason="STOP_CONTROL_UNAVAILABLE";return false;}
        app().sessionAttention(true);handler.postDelayed(()->{if(generation==owner)stop();},25_000);
        handler.postDelayed(new Runnable(){@Override public void run(){
            if(owner!=generation||(!requested&&activeAccount<0))return;
            if(activeAccount>=0)controller.onTime(SystemClock.elapsedRealtime(),port);
            if(owner==generation&&(requested||activeAccount>=0))handler.postDelayed(this,250);
        }},250);return true;
    }
    @Override public void onAccessibilityEvent(AccessibilityEvent event){callback(()->handleEvent(event));}
    private void handleEvent(AccessibilityEvent event){
        if(repository==null||!repository.consented()||event.getPackageName()==null)return;
        if(!targetPackage.contentEquals(event.getPackageName())){if(activeAccount!=-1)stop();return;}
        if(measuredSession!=null){
            if(app().registry().measuredRoute(this,targetPackage)==null){stop();reason="ENVIRONMENT_CHANGED";return;}
            measuredSession.event(event);return;
        }
        if(app().registry().measuredRoute(this,targetPackage)!=null){
            AccessibilityNodeInfo root=getRootInActiveWindow();
            try{String receiver=io.github.appunnim.businessgate.connected.ReceivingAccountRoute.readReceiver(root,targetPackage);if(receiver!=null)measuredCandidate(receiver,SystemClock.elapsedRealtime());}
            finally{if(root!=null)root.recycle();}return;
        }
        QualifiedAdapter adapter=app().registry().resolve(this,targetPackage);
        if(adapter==null){stop();reason="ENVIRONMENT_CHANGED";filter("io.github.appunnim.businessgate.disabled");return;}
        // No event-origin exemption is assumed. Own-action attribution awaits measured qualification.
        if(event.getEventType()==AccessibilityEvent.TYPE_VIEW_CLICKED||event.getEventType()==AccessibilityEvent.TYPE_VIEW_SCROLLED){
            if(activeAccount!=-1){stop();reason="INTERACTION_STOPPED";}return;
        }
        if(activeAccount!=-1){controller.onEvent(port);return;}
        inspectIdleProfile();
    }
    private void measuredCandidate(String receiver,long observedAt){
        String adapter=app().registry().measuredRoute(this,targetPackage);if(adapter==null)return;
        candidate=new Binding(repository.installation(),AdapterRegistry.sha256(targetPackage.getBytes(StandardCharsets.UTF_8)),"user-"+android.os.Process.myUserHandle().hashCode(),receiver,adapter);
        candidateAt=observedAt;
    }
    private boolean requestMeasured(String phone,boolean connectOnly,boolean activate,boolean checkOnly){
        if(repository==null||!repository.consented()||app().registry().measuredRoute(this,targetPackage)==null||(!connectOnly&&!repository.current().binding().bound()))return false;
        if(!connectOnly&&!checkOnly&&repository.current().circuitOpen()){reason="COMPATIBILITY_CHECK_REQUIRED";return false;}
        stop();long owner=++generation;deadline=SystemClock.elapsedRealtime()+60_000;requested=true;reason="CHECKING_RECEIVING_ACCOUNT";
        if(!overlay.show(this::stop)){stop();reason="STOP_CONTROL_UNAVAILABLE";return false;}
        app().sessionAttention(true);
        measuredSession=new MeasuredSession(new MeasuredSession.Host(){
            public String selectedPackage(){return targetPackage;} public String adapter(){return app().registry().measuredRoute(GateAccessibilityService.this,targetPackage);}
            public boolean active(){return owner==generation&&requested&&overlay.visible()&&SystemClock.elapsedRealtime()<deadline
                &&getSystemService(PowerManager.class).isInteractive()&&!getSystemService(KeyguardManager.class).isKeyguardLocked();}
            public AccessibilityNodeInfo root(){return getRootInActiveWindow();}
            public java.util.List<android.view.accessibility.AccessibilityWindowInfo> windows(){return getWindows();}
            public boolean back(){return performGlobalAction(GLOBAL_ACTION_BACK);}
            public boolean clearFor(AccessibilityNodeInfo node){
                if(!active()||node==null||!node.refresh()||!targetPackage.contentEquals(node.getPackageName()))return false;
                Rect action=new Rect();node.getBoundsInScreen(action);if(action.isEmpty())return false;
                var windows=getWindows();int layer=Integer.MIN_VALUE;boolean focused=false;
                try{
                    for(var window:windows)if(window.getId()==node.getWindowId()){layer=window.getLayer();focused=window.isActive()&&window.isFocused();}
                    if(!focused)return false;
                    for(var window:windows)if(window.getLayer()>layer&&!overlay.ownsWindow(window.getId())){
                        Rect bounds=new Rect();window.getBoundsInScreen(bounds);if(Rect.intersects(action,bounds))return false;
                    }
                    return overlay.prepare(action);
                }finally{for(var window:windows)window.recycle();}
            }
            public void open(Intent intent){
                if(!active()||intent.resolveActivity(getPackageManager())==null){stop();reason="CONTACT_INTENT_UNAVAILABLE";return;}
                try{startActivity(intent);}catch(RuntimeException error){stop();reason="CONTACT_INTENT_UNAVAILABLE";}
            }
            public void later(Runnable r,long delay){handler.postDelayed(()->{if(owner==generation)r.run();},delay);}
            public void candidate(String receiver,long at){measuredCandidate(receiver,at);}
            public void finished(String status){if(owner!=generation)return;measuredSession=null;finishSession();reason=status;}
        },repository,phone,owner,connectOnly,activate,checkOnly);
        handler.postDelayed(()->{if(owner==generation)stop();},60_000);
        handler.postDelayed(new Runnable(){int stableWindow=-1;public void run(){
            if(owner!=generation||measuredSession==null)return;
            AccessibilityNodeInfo root=getRootInActiveWindow();boolean target=false;int id=-1;
            if(root!=null&&targetPackage.contentEquals(root.getPackageName())){
                id=root.getWindowId();for(var window:getWindows()){if(window.getId()==id)target=window.isActive()&&window.isFocused();window.recycle();}
            }
            if(root!=null)root.recycle();
            if(target&&stableWindow==id)measuredSession.start();
            else if(SystemClock.elapsedRealtime()<deadline-50_000){stableWindow=target?id:-1;handler.postDelayed(this,350);}
            else{stop();reason="OPEN_SELECTED_APP";}
        }},600);
        return true;
    }
    private void inspectIdleProfile(){
        if(repository==null||!repository.consented()||targetPackage.isEmpty())return;
        QualifiedAdapter adapter=app().registry().resolve(this,targetPackage);if(adapter==null)return;
        AccessibilityNodeInfo root=getRootInActiveWindow();
        try(QualifiedAdapter.Inspection inspection=adapter.open(root,targetPackage,callbackBudget)){
            QualifiedAdapter.Reading read=inspection.reading();
            if(read==null||read.screen()!=AutomationController.Screen.PROFILE)return;
            candidate=new Binding(repository.installation(),AdapterRegistry.sha256(targetPackage.getBytes(StandardCharsets.UTF_8)),"user-"+android.os.Process.myUserHandle().hashCode(),read.receiver(),adapter.id());
            candidateAt=SystemClock.elapsedRealtime();
            if(!candidate.equals(repository.current().binding())){if(requested)stop();reason="RECEIVER_REVIEW_REQUIRED";return;}
            if(requested&&!arming){
                if(!requestedPhone.isEmpty()&&!requestedPhone.equals(read.phone())){reason="OPEN_REQUESTED_NUMBER";return;}
                if(SystemClock.elapsedRealtime()>=deadline){stop();return;}
                Account account=repository.current().accounts().stream().filter(a->a.phone().equals(read.phone())).findFirst().orElse(null);
                if(account==null){
                    reason="CHECKING_PROFILE";
                    if(!pendingObservation.equals(read.phone())){pendingObservation=read.phone();repository.observe(evidence(read,adapter,root),read.name());}
                    return;
                }
                long owner=generation;arming=true;
                Readiness ready=new Readiness(repository.current().namespace(),repository.current().globalRevision(),repository.epoch(),SystemClock.elapsedRealtime(),candidate);
                if(checkingCompatibility){
                    AutomationController.Frame frame=inspect(account);
                    int required=RuleEngine.ALL_GUARDS&~(1<<Guard.NO_STOP.ordinal())&~(1<<Guard.CIRCUIT.ordinal());
                    if(frame==null||(frame.context().guards()&required)!=required){arming=false;reason="CHECK_NEEDS_SAFE_PROFILE";return;}
                    repository.compatibilityChecked(ready,()->{if(owner!=generation)return;finishSession();reason="COMPATIBILITY_CHECK_PASSED";});return;
                }
                repository.arm(ready,activatingRule,()->callback(()->{
                    arming=false;if(owner!=generation||!requested||SystemClock.elapsedRealtime()>=deadline){repository.emergencyStop();return;}
                    if(!account.pending()){String completedReason=activatingRule?"RULE_READY":"NO_PENDING_ACTION";finishSession();reason=completedReason;return;}
                    activeAccount=account.id();requested=false;reason="APPLYING";controller.start(activeAccount,SystemClock.elapsedRealtime(),generation);controller.onEvent(port);
                }));
            }else if(!requested)repository.observe(evidence(read,adapter,root),read.name());
        }
    }
    private Evidence evidence(QualifiedAdapter.Reading reading,QualifiedAdapter adapter,AccessibilityNodeInfo root){
        return new Evidence(repository.current().namespace(),reading.phone(),reading.kind(),reading.blockState(),SystemClock.elapsedRealtime(),generation,
            root.getWindowId(),reading.receiver(),adapter.id(),reading.screen()==AutomationController.Screen.PROFILE||reading.screen()==AutomationController.Screen.BLOCK_DIALOG||reading.screen()==AutomationController.Screen.UNBLOCK_DIALOG,reading.safe());
    }
    private AutomationController.Frame inspect(){
        return inspect(repository.current().account(activeAccount));
    }
    private AutomationController.Frame inspect(Account account){
        if(account==null||!repository.consented())return null;
        Snapshot s=repository.current();
        QualifiedAdapter adapter=app().registry().resolve(this,targetPackage);if(adapter==null)return null;
        AccessibilityNodeInfo root=getRootInActiveWindow();
        try(QualifiedAdapter.Inspection inspection=adapter.open(root,targetPackage,callbackBudget)){
            return frame(account,s,adapter,root,inspection);
        }
    }
    private AutomationController.Frame frame(Account account,Snapshot s,QualifiedAdapter adapter,AccessibilityNodeInfo root,QualifiedAdapter.Inspection inspection){
        QualifiedAdapter.Reading read=inspection.reading();if(read==null)return null;
        boolean foreground=false,windowSafe=true;Rect target=new Rect();root.getBoundsInScreen(target);
        for(var window:getWindows()){
            if(window.getId()==root.getWindowId())foreground=window.isActive()&&window.isFocused();
            else if(!overlay.ownsWindow(window.getId())){Rect bounds=new Rect();window.getBoundsInScreen(bounds);if(Rect.intersects(target,bounds))windowSafe=false;}
        }
        boolean receiver=s.binding().bound()&&s.binding().receiver().equals(read.receiver())&&s.binding().adapter().equals(adapter.id())
            &&s.binding().installation().equals(repository.installation())&&s.binding().packageDigest().equals(AdapterRegistry.sha256(targetPackage.getBytes(StandardCharsets.UTF_8)));
        GuardFacts facts=new GuardFacts().record(Guard.CONNECTED,connected==this)
            .record(Guard.SIGNATURE,app().registry().resolve(this,targetPackage)!=null).record(Guard.BUILD,adapter!=null)
            .record(Guard.LANGUAGE,inspection.languageMatches()).record(Guard.FOREGROUND,foreground&&targetPackage.contentEquals(root.getPackageName()==null?"":root.getPackageName()))
            .record(Guard.INTERACTIVE,getSystemService(PowerManager.class).isInteractive()).record(Guard.UNLOCKED,!getSystemService(KeyguardManager.class).isKeyguardLocked())
            .record(Guard.RECEIVER,receiver).record(Guard.PROFILE,account.phone().equals(read.phone())&&account.namespace()==s.namespace())
            .record(Guard.WINDOW,windowSafe&&root.isVisibleToUser()).record(Guard.OVERLAY,overlay.clearOf(inspection.actionBounds())&&overlay.clearOf(inspection.identityBounds()))
            .record(Guard.NO_STOP,!repository.disarmed()).record(Guard.NO_ALLOW_VETO,!repository.vetoed(account.id()))
            .record(Guard.CIRCUIT,!s.circuitOpen()).record(Guard.LEASE,SystemClock.elapsedRealtime()<deadline)
            .record(Guard.STORAGE,s.loaded()&&s.error().isEmpty());
        return new AutomationController.Frame(read.screen(),evidence(read,adapter,root),new RuleEngine.Context(facts.bits(),SystemClock.elapsedRealtime(),System.currentTimeMillis(),generation,s.globalRevision(),account.revision(),deadline),inspection.actionable());
    }
    private final AutomationController.Port port=new AutomationController.Port(){
        @Override public Snapshot policy(){return repository.current();}
        @Override public AutomationController.Frame inspect(){return GateAccessibilityService.this.inspect();}
        @Override public void journal(AutomationController.Plan plan,Runnable success,Runnable failed){repository.journal(plan,()->callback(success),()->callback(failed));}
        @Override public boolean click(AutomationController.Control control,AutomationController.Frame checked){
            Snapshot s=repository.current();Account account=s.account(activeAccount);if(account==null||!repository.consented())return false;
            QualifiedAdapter adapter=app().registry().resolve(GateAccessibilityService.this,targetPackage);if(adapter==null)return false;
            AccessibilityNodeInfo root=getRootInActiveWindow();
            try(QualifiedAdapter.Inspection inspection=adapter.open(root,targetPackage,callbackBudget)){
                AutomationController.Frame fresh=frame(account,s,adapter,root,inspection);
                return !repository.disarmed()&&!repository.vetoed(activeAccount)
                    &&FinalDispatch.allowed(control,checked,fresh,repository.current(),activeAccount)&&inspection.click(control);
            }
        }
        @Override public void verified(AutomationController.Plan plan,Evidence evidence,java.util.function.Consumer<AutomationController.CommitResult> result){repository.verified(plan,evidence,result);}
        @Override public void completed(){finishSession();reason="VERIFIED";}
        @Override public void stopped(AutomationController.Plan intent,AutomationController.StopReason stopReason,boolean uncertain){
            repository.emergencyStop();repository.interrupted(intent,stopReason,uncertain,sessionScope);
            finishSession();reason=uncertain?"RESULT_UNVERIFIED":stopReason.name();
        }
    };
    private void finishSession(){if(overlay!=null)overlay.hide();activeAccount=-1;requested=false;arming=false;checkingCompatibility=false;activatingRule=false;sessionScope=null;pendingObservation="";requestedPhone="";handler.removeCallbacksAndMessages(null);app().sessionAttention(false);}
    private void stop(){if(repository==null)return;generation++;if(measuredSession!=null){var session=measuredSession;measuredSession=null;session.stop();}controller.stop(port);finishSession();repository.emergencyStop();reason="USER_STOP";}
    @Override public void onInterrupt(){stop();}
    @Override public void onDestroy(){stop();if(repository!=null)repository.removeListener(repositoryChanged);try{unregisterReceiver(screenOff);}catch(IllegalArgumentException ignored){/* Connection was never completed. */}connected=null;super.onDestroy();}
}
