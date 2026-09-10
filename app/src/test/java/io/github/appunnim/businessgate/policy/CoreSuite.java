package io.github.appunnim.businessgate.policy;

import io.github.appunnim.businessgate.automation.AutomationController;
import static io.github.appunnim.businessgate.policy.Model.*;
import java.util.List;

/** Runnable without Android, JUnit, network, or a target installation. */
public final class CoreSuite {
    private static int assertions;
    private static final RuleEngine engine=new RuleEngine();
    private static final long NOW=1_000_000;
    private static Account account(Kind kind,Choice choice,BlockState block,String nonce){
        return new Account(7,1,"+12025550101","Harbor Clinic",kind,choice,block,false,3,kind==Kind.BUSINESS_CONFIRMED,Review.NONE,0,0,0,0,
            JobState.PENDING,choice==Choice.ALLOW?Action.UNBLOCK:Action.BLOCK,nonce,NOW-1000);
    }
    private static Snapshot snapshot(Account a,boolean enabled,boolean paused){return new Snapshot(1,4,true,enabled,paused,true,false,false,false,"READY",List.of(a),"",new Binding("synthetic-installation","synthetic-digest","synthetic-profile","receiver-a","synthetic-v1"));}
    private static Evidence evidence(Kind kind,BlockState state){return new Evidence(1,"+12025550101",kind,state,100,8,2,"receiver-a","synthetic-v1",true,true);}
    private static RuleEngine.Context context(int guards){return new RuleEngine.Context(guards,100,NOW,8,4,3,1000);}
    private static void check(boolean condition,String label){assertions++;if(!condition)throw new AssertionError(label);}
    private static void equals(Object actual,Object expected,String label){check(java.util.Objects.equals(actual,expected),label+" expected "+expected+", got "+actual);}
    public static void main(String[] args){identity();rules();hints();attention();budgets();epochs();controllerRegressions();controller();retries();System.out.println("PASS "+assertions+" assertions: identity, policy guards, hint exclusions, interval union, mutation races and verification");}
    private static void identity(){
        equals(Identity.canonicalPhone("+1 (202) 555-0101"),"+12025550101","canonical identity");
        for(String bad:new String[]{"2025550101","+01234567","+123","+1234567890123456","+1 202 555 0101 ext 2","+١٢٠٢٥٥٥٠١٠١","+12025550101\u202e","+12025550101,+12025550102","++12025550101","+1202\t5550101","+1202\u200b5550101"}){
            boolean rejected=false;try{Identity.canonicalPhone(bad);}catch(IllegalArgumentException expected){rejected=true;}check(rejected,"reject ambiguous phone");
        }
        equals(Identity.searchKey("MÁYA Café"),"maya cafe","accent search");
        equals(Identity.label("Block\u202e\nAlice"),"BlockAlice","safe display controls");
        equals(Identity.likeLiteral("10%_\\"),"10\\%\\_\\\\","bound literal wildcard");
        equals(Identity.query("a".repeat(200)).length(),128,"bounded query");
    }
    private static void rules(){
        Account business=account(Kind.BUSINESS_CONFIRMED,Choice.DEFAULT,BlockState.UNBLOCKED,"");
        Account manual=account(Kind.REGULAR_PROFILE_OBSERVED,Choice.DENY_MANUAL,BlockState.UNBLOCKED,"");
        Account allowed=account(Kind.BUSINESS_CONFIRMED,Choice.ALLOW,BlockState.BLOCKED,"grant");
        for(int mask=0;mask<=RuleEngine.ALL_GUARDS;mask++){
            equals(engine.evaluate(snapshot(business,true,false),business,evidence(Kind.BUSINESS_CONFIRMED,BlockState.UNBLOCKED),context(mask)).action(),mask==RuleEngine.ALL_GUARDS?Action.BLOCK:Action.NONE,"business guard conjunction");
            equals(engine.evaluate(snapshot(manual,true,false),manual,evidence(Kind.REGULAR_PROFILE_OBSERVED,BlockState.UNBLOCKED),context(mask)).action(),mask==RuleEngine.ALL_GUARDS?Action.BLOCK:Action.NONE,"manual guard conjunction");
            equals(engine.evaluate(snapshot(allowed,false,true),allowed,evidence(Kind.BUSINESS_CONFIRMED,BlockState.BLOCKED),context(mask)).action(),mask==RuleEngine.ALL_GUARDS?Action.UNBLOCK:Action.NONE,"explicit unblock while rule paused");
        }
        for(Kind kind:Kind.values())for(Choice choice:Choice.values())for(BlockState state:BlockState.values()){
            Account a=account(kind,choice,state,"");
            Action result=engine.evaluate(snapshot(a,true,false),a,evidence(kind,state),context(RuleEngine.ALL_GUARDS)).action();
            boolean block=(kind==Kind.BUSINESS_CONFIRMED||choice==Choice.DENY_MANUAL)&&kind!=Kind.NON_DIRECT&&kind!=Kind.AMBIGUOUS&&choice!=Choice.ALLOW&&state==BlockState.UNBLOCKED;
            equals(result,block?Action.BLOCK:Action.NONE,"classification/choice/state cross product");
        }
        RuleEngine.Context safe=context(RuleEngine.ALL_GUARDS);
        equals(engine.evaluate(snapshot(business,true,true),business,evidence(Kind.BUSINESS_CONFIRMED,BlockState.UNBLOCKED),safe).action(),Action.NONE,"pause prevents business block");
        Evidence wrong=new Evidence(1,"+12025550102",Kind.BUSINESS_CONFIRMED,BlockState.UNBLOCKED,100,8,2,"receiver-a","synthetic-v1",true,true);
        equals(engine.evaluate(snapshot(business,true,false),business,wrong,safe).reason(),RuleEngine.Reason.IDENTITY_CHANGED,"same brand new number");
        Evidence stale=new Evidence(1,business.phone(),Kind.BUSINESS_CONFIRMED,BlockState.UNBLOCKED,100,9,2,"receiver-a","synthetic-v1",true,true);
        equals(engine.evaluate(snapshot(business,true,false),business,stale,safe).reason(),RuleEngine.Reason.STALE_EVIDENCE,"generation invalidated");
        RuleEngine.Context expired=new RuleEngine.Context(RuleEngine.ALL_GUARDS,100,NOW+RuleEngine.GRANT_TTL_MS,8,4,3,1000);
        equals(engine.evaluate(snapshot(allowed,true,false),allowed,evidence(Kind.BUSINESS_CONFIRMED,BlockState.BLOCKED),expired).reason(),RuleEngine.Reason.NO_UNBLOCK_AUTHORITY,"grant expires independently of ALLOW");
        RuleEngine.Context rollback=new RuleEngine.Context(RuleEngine.ALL_GUARDS,100,NOW-2000,8,4,3,1000);
        equals(engine.evaluate(snapshot(allowed,true,false),allowed,evidence(Kind.BUSINESS_CONFIRMED,BlockState.BLOCKED),rollback).action(),Action.NONE,"clock rollback cannot extend grant");
        RuleEngine.Context revised=new RuleEngine.Context(RuleEngine.ALL_GUARDS,100,NOW,8,4,2,1000);
        equals(engine.evaluate(snapshot(business,true,false),business,evidence(Kind.BUSINESS_CONFIRMED,BlockState.UNBLOCKED),revised).reason(),RuleEngine.Reason.POLICY_CHANGED,"policy revision race");
        equals(engine.evaluate(snapshot(business,true,false),business,evidence(Kind.BUSINESS_CONFIRMED,BlockState.BLOCKED),safe).reason(),RuleEngine.Reason.ALREADY_SATISFIED,"live idempotence");
    }
    private static void hints(){
        SalesHintEngine h=new SalesHintEngine();
        for(String positive:new String[]{"Special offer: our catalogue is online. Buy now.","Sales executive here. Book a site visit. Limited time."})check(h.score(positive,true,false,false,false).promoted(),"golden sales hint");
        for(String negative:new String[]{"Hi, this is Maya. I got your number from Arun.","Your verification code is 123456. Do not share it.","Your parcel arrives today. Call the driver on arrival.","Special offer, special offer, special offer.","Can you lend me Rs 500? See https://example.test","Special offer: our catalogue. Buy now. മലയാളം","Special offer our catalogue buy now verification code 123456","Special offercatalogue buy now"})check(!h.score(negative,true,false,false,false).promoted(),"hint exclusion");
        String sales="Special offer. Our catalogue. Buy now.";
        check(!h.score(sales,false,false,false,false).promoted(),"hints opt in");
        check(!h.score(sales,true,true,false,false).promoted(),"kept suppression");
        check(!h.score(sales,true,false,true,false).promoted(),"fresh regular suppression");
        check(!h.score(sales,true,false,false,true).promoted(),"forward suppression");
        check(!h.score("x ".repeat(2048)+sales,true,false,false,false).promoted(),"bounded notification text");
        equals(h.score(sales,true,false,false,false).bits(),14,"reason bits only");
    }
    private static void attention(){
        AttentionClock a=new AttentionClock();a.transition(0,true,false);a.transition(10,true,true);a.transition(30,false,true);a.transition(40,false,false);
        long[] totals=a.drain(50);equals(totals[0],30L,"management interval");equals(totals[1],30L,"occupancy interval");equals(totals[2],40L,"overlap union");
        equals(a.drain(60)[2],0L,"drain no duplicate time");
        AttentionLedger ledger=new AttentionLedger();long midnight=java.time.Instant.parse("2026-09-10T00:00:00Z").toEpochMilli();
        ledger.management(0,midnight-10,true);ledger.session(5,midnight-5,true);ledger.management(15,midnight+5,false);ledger.session(20,midnight+10,false);
        java.util.Map<String,long[]> days=ledger.drain(20,midnight+10);
        equals(days.get("2026-09-09")[2],10L,"cross-midnight first day");equals(days.get("2026-09-10")[2],10L,"cross-midnight second day");
        equals(days.values().stream().mapToLong(t->t[0]).sum(),15L,"independent management time");equals(days.values().stream().mapToLong(t->t[1]).sum(),15L,"independent session time");
        check(ledger.drain(30,midnight+20).isEmpty(),"daily drain no double count");
    }
    private static void budgets(){
        SessionBudget batch=new SessionBudget(100,false);
        for(int i=0;i<5;i++)check(batch.reserveMutation(101),"five-mutation budget reservation");
        check(!batch.reserveMutation(102),"sixth mutation refused");
        SessionBudget scan=new SessionBudget(100,true);
        check(scan.resolve("+12025550101",101),"new exact scan identity");
        check(!scan.resolve("+12025550101",102),"scan identity deduplication");
        scan.pageProgress(false);scan.pageProgress(false);scan.pageProgress(false);
        check(!scan.mayContinue(103),"three unchanged pages stop scan");
        check(!new SessionBudget(100,true).mayContinue(120100),"120 second explicit scan cap");
        check(!new SessionBudget(100,false).mayContinue(25100),"25 second mutation batch cap");
        SessionBudget stopped=new SessionBudget(100,true);stopped.stop();check(!stopped.mayContinue(101),"Stop revokes scan");
    }
    private static void controller(){
        Fake p=new Fake();AutomationController c=new AutomationController();p.controller=c;c.start(7,100,8);c.onEvent(p);equals(p.clicks,1,"one entry click");
        p.screen=AutomationController.Screen.BLOCK_DIALOG;c.onEvent(p);equals(p.clicks,2,"one confirm click");
        p.screen=AutomationController.Screen.PROFILE;p.block=BlockState.BLOCKED;c.onEvent(p);equals(p.successes,1,"success needs live postcondition");c.onEvent(p);equals(p.clicks,2,"completed job never replays");
        Fake race=new Fake();AutomationController rc=new AutomationController();race.controller=rc;race.defer=true;rc.start(7,100,8);rc.onEvent(race);race.account=account(Kind.BUSINESS_CONFIRMED,Choice.ALLOW,BlockState.UNBLOCKED,"grant");race.continueJournal.run();equals(race.clicks,0,"allow veto during durable journal");
        Fake stop=new Fake();AutomationController sc=new AutomationController();stop.controller=sc;stop.defer=true;sc.start(7,100,8);sc.onEvent(stop);sc.stop(stop);stop.continueJournal.run();equals(stop.clicks,0,"Stop before callback");
        Fake identity=new Fake();AutomationController ic=new AutomationController();identity.controller=ic;ic.start(7,100,8);ic.onEvent(identity);identity.phone="+12025550102";identity.screen=AutomationController.Screen.BLOCK_DIALOG;ic.onEvent(identity);equals(identity.clicks,1,"identity changed before confirmation");equals(identity.successes,0,"wrong identity never success");
        Fake timeout=new Fake();AutomationController tc=new AutomationController();timeout.controller=tc;tc.start(7,100,8);timeout.now=26000;tc.onEvent(timeout);equals(timeout.clicks,0,"25 second session cap");
        Fake uncertain=new Fake();AutomationController uc=new AutomationController();uncertain.controller=uc;uc.start(7,100,8);uc.onEvent(uncertain);uncertain.screen=AutomationController.Screen.BLOCK_DIALOG;uc.onEvent(uncertain);uc.stop(uncertain);equals(uncertain.successes,0,"dispatch is not verification");check(uncertain.uncertain,"after-dispatch interruption marked uncertain");
        Fake ambiguous=new Fake();AutomationController ac=new AutomationController();ambiguous.controller=ac;ambiguous.unique=false;ac.start(7,100,8);ac.onEvent(ambiguous);equals(ambiguous.clicks,0,"ambiguous control abort");
        Fake moved=new Fake();AutomationController mc=new AutomationController();moved.controller=mc;moved.defer=true;mc.start(7,100,8);mc.onEvent(moved);moved.window=5;moved.continueJournal.run();equals(moved.clicks,0,"fresh window recheck after journal");
    }
    private static void epochs(){
        for(GuardFacts.Guard missing:GuardFacts.Guard.values()){
            GuardFacts facts=new GuardFacts();for(GuardFacts.Guard guard:GuardFacts.Guard.values())facts.record(guard,guard!=missing);
            check(facts.bits()!=RuleEngine.ALL_GUARDS,"each named missing observation disarms");
        }
        AuthorityEpoch lease=new AuthorityEpoch();long old=lease.current();lease.revoke();
        check(!lease.arm(old),"late arm cannot undo revocation");check(!lease.armed(),"revoked stays disarmed");
        check(lease.arm(lease.current()),"fresh authority can arm");lease.revoke();check(!lease.armed(),"Stop immediate");
    }
    private static void controllerRegressions(){
        Fake late=new Fake();AutomationController c=new AutomationController();late.controller=c;late.defer=true;
        c.start(7,100,8);c.onEvent(late);Runnable oldFailure=late.failJournal;c.stop(late);
        c.start(7,100,8);c.onEvent(late);oldFailure.run();
        equals(c.phase(),AutomationController.Phase.JOURNALING,"old journal failure cannot stop new operation");
        Fake transition=new Fake();AutomationController t=new AutomationController();transition.controller=t;
        t.start(7,100,8);t.onEvent(transition);t.onEvent(transition);
        equals(t.phase(),AutomationController.Phase.DIALOG,"intermediate profile event waits without repeating entry");
        equals(transition.clicks,1,"intermediate event does not repeat click");
        Fake satisfied=new Fake();satisfied.block=BlockState.BLOCKED;AutomationController i=new AutomationController();satisfied.controller=i;
        i.start(7,100,8);i.onEvent(satisfied);equals(satisfied.successes,1,"already blocked completes by observation");
        equals(satisfied.clicks,0,"idempotent completion never clicks");
        Fake commit=new Fake();commit.deferVerification=true;AutomationController v=new AutomationController();commit.controller=v;
        v.start(7,100,8);v.onEvent(commit);commit.screen=AutomationController.Screen.BLOCK_DIALOG;v.onEvent(commit);
        commit.screen=AutomationController.Screen.PROFILE;commit.block=BlockState.BLOCKED;v.onEvent(commit);
        equals(v.phase(),AutomationController.Phase.COMMITTING,"verification waits for durable acknowledgement");equals(commit.successes,0,"no premature success");
        commit.verification.accept(AutomationController.CommitResult.FAILED);equals(v.phase(),AutomationController.Phase.STOPPED,"failed commit stops");
        equals(commit.successes,0,"failed durable verification never succeeds");
        Fake receiver=new Fake();AutomationController r=new AutomationController();receiver.controller=r;receiver.receiver="other-receiver";
        r.start(7,100,8);r.onEvent(receiver);equals(receiver.clicks,0,"receiver must match durable namespace");
    }
    private static Account attempted(Account a,int count,long updated){
        return new Account(a.id(),a.namespace(),a.phone(),a.name(),a.kind(),a.choice(),a.blockState(),a.gateOwned(),a.revision(),a.everBusiness(),a.review(),a.hintBits(),a.dismissedUntil(),a.checkedAt(),a.lastSeen(),JobState.REINSPECT,a.jobAction(),a.nonce(),a.grantCreatedAt(),count,updated,"RESULT_UNVERIFIED");
    }
    private static void retries(){
        AttentionLedger attention=new AttentionLedger();long wall=java.time.Instant.parse("2026-09-10T12:00:00Z").toEpochMilli();
        attention.management(0,wall,true);attention.session(1000,wall+1000,true);attention.reset(2000,wall+2000);
        check(attention.active(),"reset continues measuring current visible work");
        long[] totals=attention.drain(5000,wall+5000).get("2026-09-10");
        check(totals[0]==3000&&totals[1]==3000&&totals[2]==3000,"reset discards only pre-reset effort and preserves interval union");
        check(attention.drain(5000,wall+5000).isEmpty(),"checkpoint drain never duplicates committed intervals");
        equals(RetryPolicy.eligibility(0,0,NOW),RetryPolicy.Eligibility.READY,"initial attempt available");
        equals(RetryPolicy.eligibility(1,NOW,NOW+29_999),RetryPolicy.Eligibility.WAIT,"first retry waits thirty seconds");
        equals(RetryPolicy.eligibility(1,NOW,NOW+30_000),RetryPolicy.Eligibility.READY,"first later opportunity");
        equals(RetryPolicy.eligibility(2,NOW,NOW+299_999),RetryPolicy.Eligibility.WAIT,"second retry waits five minutes");
        equals(RetryPolicy.eligibility(2,NOW,NOW+300_000),RetryPolicy.Eligibility.READY,"second later opportunity");
        equals(RetryPolicy.eligibility(3,NOW,NOW+900_000),RetryPolicy.Eligibility.EXHAUSTED,"three attempts require explicit recovery");
        equals(RetryPolicy.eligibility(1,NOW,NOW-1),RetryPolicy.Eligibility.WAIT,"wall rollback cannot accelerate a retry");
        Fake waiting=new Fake();waiting.account=attempted(waiting.account,1,NOW);AutomationController w=new AutomationController();w.start(7,100,8);w.onEvent(waiting);
        equals(waiting.clicks,0,"backoff blocks entry");equals(waiting.stopReason,AutomationController.StopReason.RETRY_WAIT,"backoff reason remains truthful");
        Fake observed=new Fake();observed.account=attempted(observed.account,3,NOW);observed.block=BlockState.BLOCKED;AutomationController o=new AutomationController();o.start(7,100,8);o.onEvent(observed);
        equals(observed.clicks,0,"exhausted crash recovery observes before retry");equals(observed.successes,1,"already satisfied observation needs no repeated mutation");
        o.stop(observed);check(!observed.uncertain,"stopping a completed operation does not invent uncertainty");
        Fake stalled=new Fake();stalled.defer=true;AutomationController s=new AutomationController();s.start(7,100,8);s.onEvent(stalled);s.onTime(8100,stalled);stalled.continueJournal.run();
        equals(stalled.stopReason,AutomationController.StopReason.ATTEMPT_TIMEOUT,"stalled journal times out without an event");equals(stalled.clicks,0,"late journal cannot act after timeout");
        Fake transition=new Fake();AutomationController t=new AutomationController();t.start(7,100,8);t.onEvent(transition);t.onTime(2600,transition);
        equals(transition.stopReason,AutomationController.StopReason.TRANSITION_TIMEOUT,"transition has its own bounded deadline");equals(transition.clicks,1,"transition timeout does not repeat entry");
        Account a=account(Kind.BUSINESS_CONFIRMED,Choice.DEFAULT,BlockState.UNBLOCKED,"");Snapshot normal=snapshot(a,true,false);
        Snapshot circuit=new Snapshot(normal.namespace(),normal.globalRevision(),true,true,false,true,false,false,false,"READY",normal.accounts(),"",normal.binding(),true);
        equals(engine.evaluate(circuit,a,evidence(Kind.BUSINESS_CONFIRMED,BlockState.UNBLOCKED),context(RuleEngine.ALL_GUARDS)).action(),Action.NONE,"persisted circuit blocks optimistic guard input");
    }
    private static final class Fake implements AutomationController.Port{
        Account account=account(Kind.BUSINESS_CONFIRMED,Choice.DEFAULT,BlockState.UNBLOCKED,"");
        AutomationController controller;AutomationController.Screen screen=AutomationController.Screen.PROFILE;
        String phone=account.phone(),receiver="receiver-a";BlockState block=BlockState.UNBLOCKED;int window=2,clicks,successes;long now=100;
        boolean defer,deferVerification,unique=true,uncertain;java.util.function.Consumer<AutomationController.CommitResult> verification;Runnable continueJournal,failJournal;
        public Snapshot policy(){return snapshot(account,true,false);}
        public AutomationController.Frame inspect(){return new AutomationController.Frame(screen,new Evidence(1,phone,Kind.BUSINESS_CONFIRMED,block,now,8,window,receiver,"synthetic-v1",true,true),new RuleEngine.Context(RuleEngine.ALL_GUARDS,now,NOW,8,4,3,25100),unique);}
        public void journal(AutomationController.Plan plan,Runnable committed,Runnable failed){if(defer){continueJournal=committed;failJournal=failed;}else committed.run();}
        public boolean click(AutomationController.Control control,AutomationController.Frame frame){clicks++;return true;}
        public void verified(AutomationController.Plan plan,Evidence evidence,java.util.function.Consumer<AutomationController.CommitResult> result){if(deferVerification)verification=result;else result.accept(AutomationController.CommitResult.COMMITTED);}
        public void completed(){successes++;}
        public void stopped(AutomationController.Plan plan,AutomationController.StopReason reason,boolean unverified){uncertain=unverified;stopReason=reason;}
        AutomationController.StopReason stopReason;
    }
}
