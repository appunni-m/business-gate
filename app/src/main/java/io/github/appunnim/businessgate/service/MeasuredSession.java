package io.github.appunnim.businessgate.service;

import android.content.Intent;
import android.net.Uri;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import io.github.appunnim.businessgate.automation.AutomationController;
import io.github.appunnim.businessgate.connected.BusinessProfileRoute;
import io.github.appunnim.businessgate.connected.ReceivingAccountRoute;
import io.github.appunnim.businessgate.data.GateRepository;
import io.github.appunnim.businessgate.policy.Model.*;
import io.github.appunnim.businessgate.policy.NameVisibilityPolicy;
import io.github.appunnim.businessgate.policy.RuleEngine;
import java.util.List;

/** One user-started receiver check and exact-number operation; no unattended activity launch. */
final class MeasuredSession {
    interface Host {
        String selectedPackage(); String adapter(); boolean active();
        AccessibilityNodeInfo root(); List<AccessibilityWindowInfo> windows();
        boolean back(); boolean clearFor(AccessibilityNodeInfo node); void open(Intent intent); void later(Runnable task,long delay);
        void candidate(String receiver,long observedAt); void finished(String status);
    }
    private final Host host;
    private final GateRepository repository;
    private final String phone,adapter;
    private final long generation;
    private final boolean connectOnly,activate,checkOnly;
    private final Binding binding;
    private final long namespace,initialRevision,initialEpoch;
    private ReceivingAccountRoute receiverRoute;
    private BusinessProfileRoute businessRoute;
    private long contactOpened,profileOpened,armedEpoch;
    private boolean done,headerClicked,observing,journalPending,mutationPossible;
    private int scrolls;
    private BusinessProfileRoute.Step preparedStep;
    private Account command;
    private long commandRevision;
    private AutomationController.Plan lastIntent;
    private Readiness sessionScope;
    private io.github.appunnim.businessgate.connected.ActionEcho expectedHeader;

    MeasuredSession(Host host,GateRepository repository,String phone,long generation,boolean connectOnly,boolean activate,boolean checkOnly){
        this.host=host;this.repository=repository;this.phone=phone;this.generation=generation;this.connectOnly=connectOnly;this.activate=activate;this.checkOnly=checkOnly;
        var s=repository.current();binding=s.binding();namespace=s.namespace();initialRevision=s.globalRevision();initialEpoch=repository.epoch();adapter=host.adapter();
        sessionScope=new Readiness(namespace,initialRevision,initialEpoch,SystemClock.elapsedRealtime(),binding);
    }
    private boolean active(){return !done&&host.active()&&adapter.equals(host.adapter())&&repository.consented()
        &&repository.current().namespace()==namespace&&repository.current().binding().equals(binding)&&repository.current().error().isEmpty();}
    void start(){
        if(!active()){fail("SESSION_CHANGED");return;}
        receiverRoute=new ReceivingAccountRoute(new ReceivingAccountRoute.Host(){
            public String selectedPackage(){return host.selectedPackage();} public boolean active(){return MeasuredSession.this.active();}
            public boolean clearFor(AccessibilityNodeInfo node){return host.clearFor(node);}
            public AccessibilityNodeInfo root(){return host.root();} public List<AccessibilityWindowInfo> windows(){return host.windows();}
            public boolean back(){return host.back();} public void later(Runnable r,long ms){host.later(r,ms);}
            public void failed(String reason){receiverRoute=null;fail("RECEIVER_"+reason);}
            public void verified(String receiver,long observedAt){
                receiverRoute=null;
                if(!active())return;
                host.candidate(receiver,observedAt);
                if(connectOnly){finish("RECEIVER_REVIEW_REQUIRED");return;}
                if(!binding.receiver().equals(receiver)){fail("RECEIVER_CHANGED");return;}
                if(repository.current().globalRevision()!=initialRevision||repository.epoch()!=initialEpoch){fail("POLICY_CHANGED");return;}
                Readiness ready=new Readiness(namespace,initialRevision,initialEpoch,observedAt,binding);
                if(checkOnly){repository.compatibilityChecked(ready,()->finish("COMPATIBILITY_CHECK_PASSED"));return;}
                repository.arm(ready,activate,()->{
                    if(!active()){fail("SESSION_CHANGED");return;}
                    armedEpoch=repository.epoch();commandRevision=repository.current().globalRevision();
                    if(phone.isEmpty()){finish("RULE_READY");return;}
                    Intent intent=new Intent(Intent.ACTION_SENDTO,Uri.fromParts("smsto",phone,null)).setPackage(host.selectedPackage()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    contactOpened=SystemClock.elapsedRealtime();host.open(intent);host.later(MeasuredSession.this::inspectContact,500);
                });
            }
        });receiverRoute.start();
    }
    void event(AccessibilityEvent event){
        if(done)return;
        // The profile-opening click may arrive after its navigation has completed. Retain its
        // exact node across the handoff, so it cannot be mistaken for an unrequested action.
        if(event.getEventType()==AccessibilityEvent.TYPE_VIEW_CLICKED&&expectedHeader!=null){
            AccessibilityNodeInfo source=event.getSource();
            try{if(expectedHeader.matches(String.valueOf(event.getPackageName()),event.getAction(),event.getWindowId(),String.valueOf(event.getClassName()),event.getEventTime(),source==null?null:source.getViewIdResourceName(),source!=null)){expectedHeader=null;return;}}
            finally{release(source);}
        }
        if(receiverRoute!=null){receiverRoute.event(event);return;}
        if(businessRoute!=null){businessRoute.event(event);return;}
        if(event.getEventType()==AccessibilityEvent.TYPE_VIEW_CLICKED)fail("CONTACT_INTERACTION_CHANGED");
    }
    private boolean scope(){return active()&&!repository.disarmed()&&repository.epoch()==armedEpoch&&repository.current().globalRevision()==commandRevision
        &&!repository.current().circuitOpen()&&repository.nameVisibilityPolicy()!=null;}
    private void inspectContact(){
        if(done||observing)return;
        if(!scope()){fail("POLICY_CHANGED");return;}
        long now=SystemClock.elapsedRealtime();
        if(now-contactOpened>10_000){fail("CONTACT_OPEN_TIMEOUT");return;}
        AccessibilityNodeInfo root=host.root(),header=null,list=null,entry=null,label=null;
        try{
            if(root==null){host.later(this::inspectContact,200);return;}
            if(!host.selectedPackage().contentEquals(root.getPackageName())){fail("FOREGROUND_CHANGED");return;}
            boolean focused=false;for(var window:host.windows()){if(window.getId()==root.getWindowId())focused=window.isActive()&&window.isFocused();window.recycle();}
            if(!focused||(headerClicked&&now-profileOpened<800)){host.later(this::inspectContact,200);return;}
            BusinessProfileRoute.Profile profile=BusinessProfileRoute.readProfile(root,host.selectedPackage(),phone);
            if(profile!=null){
                list=root.getChild(0);
                if(BusinessProfileRoute.entryIndex(list.getChildCount())>=0){entry=list.getChild(BusinessProfileRoute.entryIndex(list.getChildCount()));label=entry==null?null:entry.getChild(0);}
                if(!field(entry,"block_contact_btn","android.widget.LinearLayout",1)||!entry.isVisibleToUser()){
                    if(scrolls>=2||!list.isScrollable()||!list.isVisibleToUser()){
                        if(now-profileOpened<2500){host.later(this::inspectContact,200);return;}
                        fail("BUSINESS_STATE_UNAVAILABLE");return;
                    }
                    if(!list.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)){
                        if(now-profileOpened<2500){host.later(this::inspectContact,200);return;}
                        fail("PROFILE_SCROLL_REJECTED");return;
                    }
                    scrolls++;
                    host.later(this::inspectContact,400);return;
                }
                if(!field(label,"block_contact_text","android.widget.TextView",0)){fail("BUSINESS_STATE_UNAVAILABLE");return;}
                BlockState state="Block business".contentEquals(label.getText())?BlockState.UNBLOCKED:"Unblock business".contentEquals(label.getText())?BlockState.BLOCKED:BlockState.UNKNOWN;
                if(state==BlockState.UNKNOWN){fail("BUSINESS_STATE_UNAVAILABLE");return;}
                var fresh=BusinessProfileRoute.readProfile(root,host.selectedPackage(),phone);
                if(fresh==null||!fresh.name().equals(profile.name())){fail("BUSINESS_IDENTITY_CHANGED");return;}
                observing=true;
                repository.observe(evidence(fresh,state),fresh.name(),()->{
                    observing=false;if(!scope()){fail("POLICY_CHANGED");return;}
                    command=repository.current().accounts().stream().filter(a->a.phone().equals(phone)).findFirst().orElse(null);
                    if(command==null){fail("OBSERVATION_NOT_SAVED");return;}
                    if(!command.pending()){finish("NO_PENDING_ACTION");return;}
                    startBusiness();
                });return;
            }
            if(headerClicked){if(now-profileOpened>3500){fail("UNSUPPORTED_BUSINESS_PROFILE");return;}host.later(this::inspectContact,200);return;}
            if(root.getChildCount()!=5||root.getViewIdResourceName()!=null||!"android.widget.FrameLayout".contentEquals(root.getClassName())){host.later(this::inspectContact,200);return;}
            header=root.getChild(2);
            if(!field(header,"conversation_contact","android.widget.LinearLayout",1)||!header.isVisibleToUser()||!header.isClickable()){host.later(this::inspectContact,200);return;}
            if(!host.clearFor(header)){host.later(this::inspectContact,200);return;}
            expectedHeader=new io.github.appunnim.businessgate.connected.ActionEcho(host.selectedPackage(),header.getWindowId(),String.valueOf(header.getClassName()),header.getViewIdResourceName(),SystemClock.uptimeMillis());headerClicked=true;profileOpened=now;
            if(!header.performAction(AccessibilityNodeInfo.ACTION_CLICK)){fail("PROFILE_OPEN_REJECTED");return;}
            host.later(this::inspectContact,350);
        }catch(RuntimeException error){fail("CONTACT_ROUTE_FAILED");}
        finally{release(label);release(entry);release(list);release(header);release(root);}
    }
    private void startBusiness(){
        var eligibility=io.github.appunnim.businessgate.policy.RetryPolicy.eligibility(command.attempts(),command.jobUpdatedAt(),System.currentTimeMillis());
        if(eligibility!=io.github.appunnim.businessgate.policy.RetryPolicy.Eligibility.READY){fail(eligibility==io.github.appunnim.businessgate.policy.RetryPolicy.Eligibility.WAIT?"RETRY_WAIT":"RETRY_EXHAUSTED");return;}
        BusinessProfileRoute.Action action=command.jobAction()==Action.BLOCK?BusinessProfileRoute.Action.BLOCK:BusinessProfileRoute.Action.UNBLOCK;
        businessRoute=new BusinessProfileRoute(new BusinessProfileRoute.Host(){
            public String selectedPackage(){return host.selectedPackage();} public boolean active(){return scope();}
            public boolean clearFor(AccessibilityNodeInfo node){return host.clearFor(node);}
            public AccessibilityNodeInfo root(){return host.root();} public List<AccessibilityWindowInfo> windows(){return host.windows();}
            public void later(Runnable r,long ms){host.later(r,ms);}
            public boolean authorized(BusinessProfileRoute.Profile profile,BusinessProfileRoute.Action requested){return authorize(profile,requested);}
            public boolean prepared(BusinessProfileRoute.Profile profile,BusinessProfileRoute.Action requested,BusinessProfileRoute.Step step){
                if(!authorize(profile,requested))return false;
                if(step==BusinessProfileRoute.Step.REASON)return true;
                if(preparedStep==step){if(step==BusinessProfileRoute.Step.CONFIRM||requested==BusinessProfileRoute.Action.UNBLOCK)mutationPossible=true;return true;}
                if(journalPending)return false;
                journalPending=true;lastIntent=plan(profile,step,requested==BusinessProfileRoute.Action.UNBLOCK||step==BusinessProfileRoute.Step.CONFIRM);
                repository.journal(lastIntent,()->{journalPending=false;if(!done)preparedStep=step;},()->{journalPending=false;fail("PERSISTENCE_FAILED");});return false;
            }
            public void failed(BusinessProfileRoute.Failure failure){businessRoute=null;mutationPossible|=failure.mutationPossible();fail("BUSINESS_"+failure.reason()+"_PHASE_"+failure.phase());}
            public void completed(BusinessProfileRoute.Result result){
                businessRoute=null;
                if(!authorize(result.profile(),result.action())){fail("POLICY_CHANGED");return;}
                var p=plan(result.profile(),result.action()==BusinessProfileRoute.Action.BLOCK?BusinessProfileRoute.Step.CONFIRM:BusinessProfileRoute.Step.ENTRY,result.mutated());
                repository.verified(p,evidence(result.profile(),result.action()==BusinessProfileRoute.Action.BLOCK?BlockState.BLOCKED:BlockState.UNBLOCKED),outcome->{
                    if(outcome==AutomationController.CommitResult.COMMITTED)finish("VERIFIED");else fail("RESULT_NOT_COMMITTED");
                });
            }
        },phone,action);businessRoute.start();
    }
    private boolean authorize(BusinessProfileRoute.Profile profile,BusinessProfileRoute.Action action){
        if(!scope()||command==null||!phone.equals(profile.phone()))return false;
        Account current=repository.current().account(command.id());
        if(current==null||current.revision()!=command.revision()||!current.pending()||current.jobAction()!=command.jobAction()||!current.nonce().equals(command.nonce())||repository.vetoed(current.id()))return false;
        var policy=repository.nameVisibilityPolicy();if(policy==null)return false;
        var decision=NameVisibilityPolicy.evaluate(policy,new NameVisibilityPolicy.Observation(policy.scope(),policy.revision(),true,phone,profile.name(),Kind.BUSINESS_CONFIRMED));
        if(action==BusinessProfileRoute.Action.BLOCK)return command.jobAction()==Action.BLOCK&&repository.current().enabled()&&!repository.current().paused()&&decision.visibility()==NameVisibilityPolicy.Visibility.HIDE;
        long now=System.currentTimeMillis();
        return command.jobAction()==Action.UNBLOCK&&decision.visibility()==NameVisibilityPolicy.Visibility.SHOW&&!command.nonce().isEmpty()&&now>=command.grantCreatedAt()&&now-command.grantCreatedAt()<RuleEngine.GRANT_TTL_MS;
    }
    private AutomationController.Plan plan(BusinessProfileRoute.Profile profile,BusinessProfileRoute.Step step,boolean attempted){
        var control=command.jobAction()==Action.BLOCK?(step==BusinessProfileRoute.Step.CONFIRM?AutomationController.Control.CONFIRM_BLOCK:AutomationController.Control.BLOCK_ENTRY):AutomationController.Control.UNBLOCK_ENTRY;
        return new AutomationController.Plan(command.id(),commandRevision,command.revision(),generation,command.jobAction(),control,phone,binding.receiver(),profile.windowId(),namespace,adapter,command.nonce(),attempted);
    }
    private Evidence evidence(BusinessProfileRoute.Profile p,BlockState state){return new Evidence(namespace,phone,Kind.BUSINESS_CONFIRMED,state,p.observedAt(),generation,p.windowId(),binding.receiver(),adapter,true,true);}
    private boolean field(AccessibilityNodeInfo n,String id,String type,int children){return n!=null&&n.refresh()&&host.selectedPackage().contentEquals(n.getPackageName())&&(host.selectedPackage()+":id/"+id).equals(n.getViewIdResourceName())&&type.contentEquals(n.getClassName())&&n.getChildCount()==children&&n.isEnabled();}
    void stop(){fail("USER_STOP");}
    private void fail(String reason){
        if(done)return;done=true;clear();repository.emergencyStop();
        repository.interrupted(lastIntent,reason.equals("USER_STOP")?AutomationController.StopReason.USER_STOP:AutomationController.StopReason.GUARD_FAILED,mutationPossible,sessionScope);
        host.finished(mutationPossible?"RESULT_UNVERIFIED":reason);
    }
    private void finish(String reason){if(done)return;done=true;clear();host.finished(reason);}
    private void clear(){if(receiverRoute!=null){var route=receiverRoute;receiverRoute=null;route.stop();}if(businessRoute!=null){var route=businessRoute;businessRoute=null;route.stop();}expectedHeader=null;}
    @SuppressWarnings("deprecation") private static AccessibilityNodeInfo copy(AccessibilityNodeInfo n){return AccessibilityNodeInfo.obtain(n);}
    @SuppressWarnings("deprecation") private static void release(AccessibilityNodeInfo n){if(n!=null)n.recycle();}
}
