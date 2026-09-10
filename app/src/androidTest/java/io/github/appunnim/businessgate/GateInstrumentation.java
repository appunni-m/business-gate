package io.github.appunnim.businessgate;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import io.github.appunnim.businessgate.data.GateDbHelper;
import io.github.appunnim.businessgate.data.GateRepository;
import io.github.appunnim.businessgate.policy.Model.*;
import io.github.appunnim.businessgate.policy.PendingChoices.Pending;
import io.github.appunnim.businessgate.automation.AutomationController;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

/** Test APK only. Never included in either application variant. Uses synthetic identities. */
public final class GateInstrumentation extends Instrumentation {
    private Bundle arguments;
    private int assertions;
    private final java.util.ArrayDeque<String> recentChecks=new java.util.ArrayDeque<>();
    private String metrics="";
    private GateRepository repository;
    @Override public void onCreate(Bundle args){arguments=args==null?new Bundle():args;super.onCreate(args);start();}
    private void check(boolean value,String name){assertions++;recentChecks.add(assertions+": "+name);if(recentChecks.size()>8)recentChecks.remove();if(arguments!=null&&arguments.getString("trace","false").equals("true"))metrics+="TRACE "+assertions+": "+name+"\n";if(!value)throw new AssertionError(name);}
    private void until(BooleanSupplier predicate)throws Exception{
        long deadline=android.os.SystemClock.elapsedRealtime()+10000;
        while(!predicate.getAsBoolean()){if(android.os.SystemClock.elapsedRealtime()>deadline)throw new AssertionError("Timed out waiting for committed state");Thread.sleep(20);}
    }
    private void committed(java.util.function.Consumer<Runnable> action)throws Exception{
        CountDownLatch done=new CountDownLatch(1);runOnMainSync(()->action.accept(done::countDown));check(done.await(10,TimeUnit.SECONDS),"transaction callback");
    }
    @Override public void onStart(){
        Bundle result=new Bundle();
        try{
            check(getTargetContext().getPackageName().equals("io.github.appunnim.businessgate.debug"),"synthetic tests require isolated debug data");
            GateApplication app=(GateApplication)getTargetContext().getApplicationContext();until(()->app.repository()!=null);repository=app.repository();until(()->repository.current().loaded());
            String mode=arguments.getString("mode","all");
            if(mode.equals("verify-attention")){
                long saved=effortTotal();check(saved>=4500,"visible interval checkpoint survives actual process death");
                Thread.sleep(5500);check(effortTotal()==saved,"startup does not resume an old attention interval or background checkpoint");
            }else if(mode.equals("prepare-attention")){
                committed(repository::reset);Activity activity=launch();until(()->hasText(activity,"Business Gate"));
                until(()->effortTotal()>=4500);check(effortTotal()>=4500,"visible activity persists effort before it closes");
            }else if(mode.startsWith("verify-recovery")){
                check(repository.current().accounts().stream().anyMatch(a->a.jobState()==JobState.REINSPECT),"interrupted work re-inspected after actual process restart");
                check(repository.disarmed(),"startup is disarmed");
                check(repository.current().account(4).attempts()==3,"process restart preserves consumed attempt budget");
            }else if(mode.startsWith("prepare-recovery")){
                seed();try(GateDbHelper helper=new GateDbHelper(getTargetContext())){helper.getWritableDatabase().execSQL("UPDATE action_job SET state=?,attempts=3 WHERE account_id=4",new Object[]{mode.endsWith("confirm")?"VERIFYING":"ACTION_INTENT"});}
            }else if(mode.startsWith("layout-")){
                seed();layoutRegressions();
            }else if(mode.equals("ui")){
                seed();uiRegressions();
            }else if(mode.equals("focus")){
                seed();focusRegressions();seed();announcementRegressions();
            }else if(mode.equals("dialogs")){
                seed();dialogRegressions();
            }else if(mode.equals("reminder")||mode.equals("reminder-channel")){
                seed();reminderRegressions(mode.endsWith("channel"));
            }else if(mode.equals("service-lifecycle")){
                seed();serviceLifecycleRegressions();
            }else if(mode.equals("interaction")){
                seed();interactionRegressions();
            }else if(mode.equals("talkback")){
                seed();talkBackRegressions();seed();announcementRegressions();
            }else if(mode.equals("privacy-boundary")){
                seed();publicationBoundaryRegressions();
            }else if(mode.equals("performance")){
                performance();
            }else if(mode.equals("setup")){
                committed(cb->repository.reset(cb));launch();
            }else if(mode.equals("seed")){
                seed();launch();
            }else{
                check(!app.registry().available(),"production registry remains unqualified");check(repository.disarmed(),"cold startup disarmed");
                check(getTargetContext().checkSelfPermission(android.Manifest.permission.INTERNET)!=android.content.pm.PackageManager.PERMISSION_GRANTED,"no network permission");
                committed(cb->repository.reset(cb));check(repository.current().accounts().isEmpty(),"reset clears choices");
                committed(cb->repository.enableNumber("+1 (202) 555-0101","MÁYA 10%_",cb));
                committed(cb->repository.enableNumber("+1 (202) 555-0102","MÁYA 10%_",cb));
                check(repository.current().accounts().size()==2,"identical display names never merge");
                Account first=repository.current().accounts().get(0);check(first.choice()==Choice.ALLOW,"pre-enable persists ALLOW");check(first.kind()==Kind.UNKNOWN,"pre-enable never invents business evidence");check(first.pending(),"pending truthfully distinct from observed state");
                check(first.blockState()==BlockState.UNKNOWN,"no false unblocked claim");
                committed(cb->repository.choose(first.id(),Choice.DENY_MANUAL,cb));
                check(repository.current().account(first.id()).kind()==Kind.UNKNOWN,"manual deny leaves classification alone");
                check(repository.current().accounts().stream().filter(a->a.choice()==Choice.ALLOW).count()==1,"exact-number mutation isolation");
                CountDownLatch searched=new CountDownLatch(1);repository.search("10%_",rows->{check(rows.size()==2,"LIKE wildcards literal");searched.countDown();});check(searched.await(10,TimeUnit.SECONDS),"search callback");
                CountDownLatch accent=new CountDownLatch(1);repository.search("maya",rows->{check(rows.size()==2,"accent-normalized search");accent.countDown();});check(accent.await(10,TimeUnit.SECONDS),"accent callback");
                committed(cb->repository.choose(first.id(),Choice.ALLOW,cb));String nonce=repository.current().account(first.id()).nonce();check(!nonce.isEmpty(),"explicit enable grants nonce");
                runOnMainSync(repository::pause);until(()->repository.current().accounts().stream().allMatch(a->a.nonce().isEmpty()));check(repository.current().account(first.id()).choice()==Choice.ALLOW,"pause cancels authority but retains choice");
                foundationRegressions();recoveryRegressions();seed();Activity activity=launch();
                until(()->hasText(activity,"Harbor Clinic"));check(hasText(activity,"Blocking is unavailable in this build. You can save choices here."),"unsupported status visible");scrollTo(activity,4,0);until(()->hasText(activity,"Block pending"));check(hasText(activity,"Block pending"),"pending subtitle visible after scrolling");
                long beforeSearchRow=visibleRow(activity);int beforeSearchTop=visibleTop(activity);
                runOnMainSync(()->{EditText search=find(activity.getWindow().getDecorView(),EditText.class);search.setText("+12025550102");});
                until(()->hasText(activity,"Parcel Desk")&&!hasText(activity,"Harbor Clinic"));check(!hasText(activity,"Harbor Clinic"),"search excludes other exact number");
                runOnMainSync(()->find(activity.getWindow().getDecorView(),EditText.class).setText("No such local account"));until(()->hasText(activity,"No matching account on this phone"));check(hasText(activity,"Enable a number"),"empty search offers explicit number entry");
                runOnMainSync(()->find(activity.getWindow().getDecorView(),EditText.class).setText(""));until(()->visibleRow(activity)==beforeSearchRow);
                check(Math.abs(visibleTop(activity)-beforeSearchTop)<=1,"clearing an empty search restores the pre-search row and offset");
                check(repository.current().accounts().size()==6,"UI search does not mutate repository");
                storageFailureRegressions(activity);choiceScopeRegressions();
            }
            result.putString("stream",metrics+"PASS "+assertions+" Android persistence, permission, recovery and native UI assertions; mode="+mode+"\n");finish(Activity.RESULT_OK,result);
        }catch(Throwable error){result.putString("stream",metrics+"FAIL after "+assertions+" assertions: "+error.getClass().getSimpleName()+": "+error.getMessage()+"; recent="+recentChecks+"\n");finish(Activity.RESULT_CANCELED,result);}
    }
    private void publicationBoundaryRegressions()throws Exception{
        Activity activity=launch();search(activity,"+12025550101");until(()->hasText(activity,"Harbor Clinic")&&!hasText(activity,"Parcel Desk"));
        clickOwn(activity,v->v.getContentDescription()!=null&&v.getContentDescription().toString().contains("+12025550101")&&v.getContentDescription().toString().endsWith("Show details"));
        try(GateDbHelper helper=new GateDbHelper(getTargetContext())){
            SQLiteDatabase db=helper.getWritableDatabase();
            // Owned fault: reset commits, but its subsequent identity read cannot publish the replacement.
            db.execSQL("CREATE TRIGGER remove_reset_identity AFTER INSERT ON app_meta WHEN NEW.key='ui_data_identity' BEGIN DELETE FROM app_meta WHERE key=NEW.key; END");
            try{
                java.util.concurrent.atomic.AtomicBoolean acknowledged=new java.util.concurrent.atomic.AtomicBoolean();
                runOnMainSync(()->repository.reset(()->acknowledged.set(true)));until(()->!repository.current().error().isEmpty());writerBarrier();
                int durableRows;try(var cursor=db.rawQuery("SELECT count(*) FROM account",null)){cursor.moveToFirst();durableRows=cursor.getInt(0);}
                CountDownLatch searched=new CountDownLatch(1);java.util.concurrent.atomic.AtomicInteger found=new java.util.concurrent.atomic.AtomicInteger(-1);
                repository.search("",rows->{found.set(rows.size());searched.countDown();});check(searched.await(10,TimeUnit.SECONDS),"search after committed reset fault completes");
                metrics+="METRIC committed_reset_rows="+durableRows+" snapshot_rows="+repository.current().accounts().size()+" search_rows="+found.get()+"\n";
                check(durableRows==0,"reset deletion committed before the injected publication fault");
                check(repository.current().accounts().isEmpty()&&found.get()==0,"committed reset cannot retain deleted choices in snapshot or search");
                until(()->ownsView(activity,v->v instanceof EditText e&&e.getText().toString().isEmpty()));
                check(!ownsView(activity,v->v.getContentDescription()!=null&&v.getContentDescription().toString().endsWith("Collapse details")),"failed reset publication clears old exact-number presentation");
                until(()->hasRow(activity,-17));check(!hasRow(activity,-14)&&!repository.current().loaded(),"unknown storage shows an unavailable state without claiming an empty saved list");
                check(repository.disarmed()&&!acknowledged.get(),"failed reset publication never resumes or reports complete recovery");
            }finally{db.execSQL("DROP TRIGGER remove_reset_identity");committed(repository::reset);runOnMainSync(activity::finish);}
        }
        resetRollbackRegressions();receiverPublicationRegressions();
    }
    private void resetRollbackRegressions()throws Exception{
        seed();Activity activity=launch();
        try(GateDbHelper helper=new GateDbHelper(getTargetContext())){
            SQLiteDatabase db=helper.getWritableDatabase();db.execSQL("CREATE TRIGGER reject_reset BEFORE DELETE ON account BEGIN SELECT RAISE(ABORT,'OWNED_RESET_ABORT'); END");
            try{
                runOnMainSync(()->repository.reset(()->{}));until(()->!repository.current().error().isEmpty());writerBarrier();
                try(var cursor=db.rawQuery("SELECT count(*),sum(choice='ALLOW') FROM account",null)){cursor.moveToFirst();check(cursor.getInt(0)==6&&cursor.getInt(1)==2,"failed reset rolls back without deleting durable exact-number choices");}
                check(!repository.current().loaded()&&repository.current().accounts().isEmpty()&&repository.disarmed(),"uncertain reset projection remains hidden and disarmed until checked");
            }finally{db.execSQL("DROP TRIGGER reject_reset");}
            check(result(repository::retryStorage)==GateRepository.StorageResult.RECOVERED,"explicit check recovers the rolled-back reset");
            check(repository.current().loaded()&&repository.current().accounts().size()==6&&repository.current().account(1).choice()==Choice.ALLOW&&repository.current().account(2).choice()==Choice.ALLOW&&repository.disarmed(),"storage check restores durable choices without replaying a reset or resuming");
        }finally{runOnMainSync(activity::finish);}
    }
    private void receiverPublicationRegressions()throws Exception{
        seed();long previousNamespace=repository.current().namespace();
        String digest=io.github.appunnim.businessgate.automation.AdapterRegistry.sha256("owned publication namespace fixture".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        Binding receiver=new Binding(repository.installation(),digest,"synthetic-profile","+12025550009","synthetic-publication");
        Activity activity=launch();search(activity,"+12025550101");until(()->hasText(activity,"Harbor Clinic")&&!hasText(activity,"Parcel Desk"));
        try(GateDbHelper helper=new GateDbHelper(getTargetContext())){
            SQLiteDatabase db=helper.getWritableDatabase();db.execSQL("CREATE TRIGGER remove_receiver_identity AFTER UPDATE OF active ON namespace WHEN NEW.active=1 BEGIN DELETE FROM app_meta WHERE key='ui_data_identity'; END");
            long active;
            try{
                runOnMainSync(()->repository.bindReceiver(receiver,()->{}));until(()->!repository.current().error().isEmpty());writerBarrier();
                try(var cursor=db.rawQuery("SELECT id FROM namespace WHERE active=1",null)){cursor.moveToFirst();active=cursor.getLong(0);}
                check(active!=previousNamespace,"receiver transition commits before the injected publication fault");
                check(!repository.current().loaded()&&repository.current().accounts().isEmpty()&&repository.current().binding().equals(Binding.empty()),"failed receiver publication cannot expose the old receiver or choices");
                CountDownLatch searched=new CountDownLatch(1);java.util.concurrent.atomic.AtomicInteger found=new java.util.concurrent.atomic.AtomicInteger(-1);
                repository.search("",rows->{found.set(rows.size());searched.countDown();});check(searched.await(10,TimeUnit.SECONDS)&&found.get()==0,"failed receiver publication cannot search the previous receiver's records");
                until(()->ownsView(activity,v->v instanceof EditText e&&e.getText().toString().isEmpty()));
                check(repository.disarmed(),"receiver publication fault remains disarmed");
            }finally{db.execSQL("DROP TRIGGER remove_receiver_identity");}
            check(result(repository::retryStorage)==GateRepository.StorageResult.RECOVERED,"explicit check loads the committed receiver after publication failure");
            check(repository.current().namespace()==active&&repository.current().binding().equals(receiver)&&repository.current().accounts().isEmpty()&&repository.disarmed(),"recovery loads only the committed receiving namespace");
            committed(repository::localChoices);check(repository.current().namespace()==previousNamespace&&repository.current().accounts().size()==6&&repository.current().account(1).choice()==Choice.ALLOW,"original namespace choices remain durable and require an explicit return");
        }finally{committed(repository::reset);runOnMainSync(activity::finish);}
    }
    private void storageFailureRegressions(Activity activity)throws Exception{
        committed(cb->repository.enableNumber("+12025550196","MÁYA 10%_",cb));
        Account protectedAccount=repository.current().accounts().stream().filter(a->a.phone().equals("+12025550196")).findFirst().orElseThrow();
        long globalBefore=repository.current().globalRevision();String grantBefore=protectedAccount.nonce();long grantTimeBefore=protectedAccount.grantCreatedAt();
        java.util.concurrent.atomic.AtomicInteger notices=new java.util.concurrent.atomic.AtomicInteger();Runnable listener=notices::incrementAndGet;
        repository.addListener(listener);
        try(GateDbHelper helper=new GateDbHelper(getTargetContext())){
            SQLiteDatabase db=helper.getWritableDatabase();db.execSQL("ALTER TABLE account RENAME TO unavailable_accounts");
            try{
                runOnMainSync(()->repository.reload(()->{}));until(()->!repository.current().error().isEmpty());
                writerBarrier();int first=notices.get();Thread.sleep(500);writerBarrier();
                check(notices.get()==first,"unchanged storage error cannot schedule an endless UI refresh loop");
                check(repository.disarmed(),"read failure disarms all mutations");
                CountDownLatch found=new CountDownLatch(1);repository.search("maya 10%_",rows->{check(rows.size()==1&&rows.get(0).id()==protectedAccount.id(),"saved snapshot search preserves accents and literal wildcards during read failure");found.countDown();});
                check(found.await(10,TimeUnit.SECONDS),"failed-storage search still completes");
                java.util.concurrent.atomic.AtomicBoolean acknowledged=new java.util.concurrent.atomic.AtomicBoolean();
                runOnMainSync(()->repository.choose(protectedAccount.id(),Choice.ALLOW,()->acknowledged.set(true)));writerBarrier();
                check(!acknowledged.get()&&repository.vetoed(protectedAccount.id()),"failed choice has no success acknowledgement and retains immediate ALLOW veto");
                runOnMainSync(()->find(activity.getWindow().getDecorView(),EditText.class).setText("+12025550196"));
                until(()->hasText(activity,"MÁYA 10%_"));check(hasText(activity,"Could not save. No new blocks will run."),"saved row and truthful storage failure remain visible");
                check(repository.pendingChoice(protectedAccount.phone()).failed(),"failed existing choice is explicitly reviewable");
                runOnMainSync(()->repository.enableNumber("+12025550195","Unsaved new choice",()->acknowledged.set(true)));writerBarrier();
                check(repository.pendingChoice("+12025550195").failed()&&!acknowledged.get(),"failed new number retained without claiming a database row");
                check(repository.current().accounts().stream().noneMatch(a->a.phone().equals("+12025550195")),"failed new choice never appears as durable account");
                check(result(repository::retryStorage)==GateRepository.StorageResult.FAILED,"storage retry reports unavailable table");
                check(repository.pendingChoices().size()==2&&repository.disarmed(),"failed storage retry preserves both unsaved choices and pause");

            }finally{db.execSQL("ALTER TABLE unavailable_accounts RENAME TO account");}
            try(var cursor=db.rawQuery("SELECT choice FROM account WHERE id=?",new String[]{""+protectedAccount.id()})){
                check(cursor.moveToFirst()&&cursor.getString(0).equals("ALLOW"),"read/write failure does not delete durable ALLOW");
            }
            check(result(repository::retryStorage)==GateRepository.StorageResult.UNSAVED_CHOICES,"storage repair requires separate user choice retry");
            check(repository.current().error().isEmpty()&&repository.current().paused()&&repository.disarmed(),"storage recovery clears error while retaining pause");
            check(repository.current().globalRevision()>globalBefore,"storage recovery invalidates previous global revision");
            Account recovered=repository.current().account(protectedAccount.id());
            check(recovered.nonce().equals(grantBefore)&&recovered.grantCreatedAt()==grantTimeBefore,"storage check does not mint or extend an unblock grant");
            Pending old=repository.pendingChoice(protectedAccount.phone());
            Pending forged=new Pending(old.namespace(),old.phone(),old.label(),Choice.DENY_MANUAL,old.baseRevision(),old.sequence(),old.failed());
            check(this.<GateRepository.SaveResult>result(cb->repository.retrySave(forged,cb))==GateRepository.SaveResult.STALE,"retry cannot replace the stored intended choice with a forged payload");
            check(this.<GateRepository.SaveResult>result(cb->repository.retrySave(old,cb))==GateRepository.SaveResult.SAVED,"explicit retry saves matching existing choice");
            check(!repository.current().account(protectedAccount.id()).nonce().equals(grantBefore),"only explicit Retry save creates replacement ALLOW grant");
            check(this.<GateRepository.SaveResult>result(cb->repository.retrySave(old,cb))==GateRepository.SaveResult.STALE,"completed retry cannot replay a grant");
            Pending fresh=repository.pendingChoice("+12025550195");
            check(this.<GateRepository.SaveResult>result(cb->repository.retrySave(fresh,cb))==GateRepository.SaveResult.SAVED,"explicit retry creates previously absent exact number");
            check(repository.current().accounts().stream().anyMatch(a->a.phone().equals(fresh.phone())&&a.choice()==Choice.ALLOW),"retried new choice committed with exact phone");
            check(repository.pendingChoices().isEmpty()&&repository.disarmed()&&repository.current().paused(),"saving final pending choice does not resume actions");

            Account current=repository.current().account(protectedAccount.id());
            db.execSQL("ALTER TABLE account RENAME TO unavailable_accounts");
            try{runOnMainSync(()->repository.choose(current,Choice.ALLOW,()->{}));writerBarrier();}
            finally{db.execSQL("ALTER TABLE unavailable_accounts RENAME TO account");}
            Pending stale=repository.pendingChoice(current.phone());
            db.execSQL("UPDATE account SET revision=revision+1 WHERE id=?",new Object[]{current.id()});
            check(result(repository::retryStorage)==GateRepository.StorageResult.UNSAVED_CHOICES,"repair retains revision-stale pending choice");
            check(this.<GateRepository.SaveResult>result(cb->repository.retrySave(stale,cb))==GateRepository.SaveResult.STALE,"revision change rejects old retry without rebasing intent");
            Pending replaced=repository.pendingChoice(current.phone());
            check(repository.current().account(current.id()).choice()==Choice.ALLOW,"stale retry preserves durable choice");
            committed(cb->repository.choose(repository.current().account(current.id()),Choice.DENY_MANUAL,cb));
            check(this.<GateRepository.SaveResult>result(cb->repository.retrySave(replaced,cb))==GateRepository.SaveResult.STALE,"new explicit decision cannot be overwritten by old ALLOW retry");
            check(repository.current().account(current.id()).choice()==Choice.DENY_MANUAL&&repository.current().account(current.id()).nonce().isEmpty(),"replacement deny cancels old grant");

        }finally{repository.removeListener(listener);}
        committed(repository::reset);
    }
    private <T> T result(java.util.function.Consumer<java.util.function.Consumer<T>> action)throws Exception{
        var value=new java.util.concurrent.atomic.AtomicReference<T>();CountDownLatch done=new CountDownLatch(1);
        runOnMainSync(()->action.accept(answer->{value.set(answer);done.countDown();}));
        check(done.await(10,TimeUnit.SECONDS),"explicit operation result callback");return value.get();
    }
    private void choiceScopeRegressions()throws Exception{
        committed(repository::reset);committed(cb->repository.enableNumber("+12025550181","Old row",cb));
        Account old=repository.current().accounts().get(0);GateRepository.ChoiceScope scope=repository.choiceScope();
        committed(repository::reset);committed(cb->repository.enableNumber("+12025550182","Replacement row",cb));
        Account replacement=repository.current().accounts().get(0);check(old.id()==replacement.id(),"fixture exercises row id reuse after reset");
        runOnMainSync(()->check(!repository.choose(old,Choice.DENY_MANUAL,()->{}),"old row cannot target reused id"));
        runOnMainSync(()->check(!repository.enableNumber(scope,"+12025550183","Old form",()->{}),"old add form cannot repopulate reset"));
        writerBarrier();check(repository.current().accounts().size()==1&&repository.current().account(replacement.id()).choice()==Choice.ALLOW,"stale UI commands preserve replacement account");
        committed(cb->repository.choose(replacement,Choice.DEFAULT,cb));
        runOnMainSync(()->check(!repository.choose(replacement,Choice.ALLOW,()->{}),"old row revision cannot mint a new grant"));
        check(repository.disarmed(),"rejected stale choice keeps actions stopped");
        Account latest=repository.current().account(replacement.id());
        CountDownLatch queued=new CountDownLatch(1),drain=new CountDownLatch(1);var obsolete=new java.util.concurrent.atomic.AtomicBoolean();
        writer().execute(()->{queued.countDown();try{drain.await(10,TimeUnit.SECONDS);}catch(InterruptedException interrupted){Thread.currentThread().interrupt();}});
        check(queued.await(10,TimeUnit.SECONDS),"writer held before conflicting choices");
        runOnMainSync(()->{repository.choose(latest,Choice.ALLOW,()->obsolete.set(true));repository.choose(latest,Choice.DENY_MANUAL,()->{});});
        check(repository.pendingChoice(latest.phone()).choice()==Choice.DENY_MANUAL,"latest queued exact-number decision is visible");
        drain.countDown();writerBarrier();
        check(!obsolete.get()&&repository.pendingChoices().isEmpty(),"superseded save has no success callback and cannot retain veto over committed replacement");
        check(repository.current().account(latest.id()).choice()==Choice.DENY_MANUAL&&repository.current().account(latest.id()).nonce().isEmpty(),"newest queued choice commits without stale ALLOW authority");

        String digest=io.github.appunnim.businessgate.automation.AdapterRegistry.sha256("owned pending namespace fixture".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        Binding bound=new Binding(repository.installation(),digest,"synthetic-profile","+12025550008","synthetic-a");
        GateRepository.ChoiceScope local=repository.choiceScope();
        try(GateDbHelper helper=new GateDbHelper(getTargetContext())){
            SQLiteDatabase db=helper.getWritableDatabase();db.execSQL("ALTER TABLE account RENAME TO unavailable_accounts");
            try{runOnMainSync(()->repository.enableNumber("+12025550184","Local unsaved",()->{}));writerBarrier();}
            finally{db.execSQL("ALTER TABLE unavailable_accounts RENAME TO account");}
        }
        Pending localPending=repository.pendingChoice("+12025550184");
        check(result(repository::retryStorage)==GateRepository.StorageResult.UNSAVED_CHOICES,"local pending fixture recovered");
        committed(cb->repository.bindReceiver(bound,cb));
        check(repository.pendingChoices().isEmpty(),"new receiver does not inherit unsaved local intent");
        runOnMainSync(()->check(!repository.enableNumber(local,"+12025550185","Old receiver form",()->{}),"receiver change invalidates open add form"));
        check(this.<GateRepository.SaveResult>result(cb->repository.retrySave(localPending,cb))==GateRepository.SaveResult.STALE,"wrong receiver rejects exact-number retry");
        committed(repository::localChoices);check(repository.pendingChoice(localPending.phone()).equals(localPending),"returning to local choices retains unsaved intent for review");

        CountDownLatch held=new CountDownLatch(1),release=new CountDownLatch(1),reset=new CountDownLatch(1);
        var recovery=new java.util.concurrent.atomic.AtomicReference<GateRepository.StorageResult>();
        var save=new java.util.concurrent.atomic.AtomicReference<GateRepository.SaveResult>();
        writer().execute(()->{held.countDown();try{release.await(10,TimeUnit.SECONDS);}catch(InterruptedException interrupted){Thread.currentThread().interrupt();}});
        check(held.await(10,TimeUnit.SECONDS),"writer held before queued retry and reset");
        runOnMainSync(()->{repository.retrySave(localPending,save::set);repository.retryStorage(recovery::set);repository.reset(reset::countDown);});
        release.countDown();check(reset.await(10,TimeUnit.SECONDS),"reset after queued recovery commits");writerBarrier();
        check(save.get()==GateRepository.SaveResult.STALE&&recovery.get()==GateRepository.StorageResult.STALE,"late recovery and save callbacks remain stale after reset");
        check(repository.pendingChoices().isEmpty()&&repository.current().accounts().isEmpty()&&repository.disarmed(),"queued retries cannot restore reset choices or authority");
    }
    private View ownView(View root,java.util.function.Predicate<View> predicate){
        if(predicate.test(root))return root;
        if(root instanceof ViewGroup group)for(int i=0;i<group.getChildCount();i++){View result=ownView(group.getChildAt(i),predicate);if(result!=null)return result;}
        return null;
    }
    private void focusRegressions()throws Exception{
        Activity activity=launch();search(activity,"+12025550101");until(()->hasText(activity,"Harbor Clinic")&&!hasText(activity,"Parcel Desk"));
        setInTouchMode(false);waitForIdleSync();boolean[] accepted={false};
        runOnMainSync(()->{
            View control=ownView(activity.getWindow().getDecorView(),v->v instanceof android.widget.Switch&&v.getContentDescription()!=null&&v.getContentDescription().toString().contains("+12025550101"));
            if(control!=null)accepted[0]=control.requestFocus();
        });
        check(accepted[0],"exact-number switch accepts keyboard focus");
        waitForIdleSync();check(focusedNumber(activity,"+12025550101"),"keyboard focus starts on the selected exact number");
        int[] painted={0};
        runOnMainSync(()->{View control=activity.getCurrentFocus();android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(control.getWidth(),control.getHeight(),android.graphics.Bitmap.Config.ARGB_8888);try{control.draw(new android.graphics.Canvas(bitmap));int color=activity.getColor(R.color.focus);for(int y=0;y<bitmap.getHeight();y++)for(int x=0;x<bitmap.getWidth();x++)if(bitmap.getPixel(x,y)==color)painted[0]++;}finally{bitmap.recycle();}});
        check(painted[0]>48,"rendered native switch retains an opaque focus outline without ripple dilution");
        committed(repository::reload);waitForIdleSync();
        until(()->focusedNumber(activity,"+12025550101"));
        check(focusedNumber(activity,"+12025550101"),"unchanged committed refresh preserves exact-number switch focus");
        long revision=repository.current().account(1).revision();
        check(activity.hasWindowFocus(),"hardware Space is confined to the owned exact-number switch");
        android.widget.ListView list=find(activity.getWindow().getDecorView(),android.widget.ListView.class);
        runOnMainSync(()->list.suppressLayout(true));
        try{
            sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_SPACE);
            until(()->repository.current().account(1).choice()==Choice.DEFAULT);until(()->focusedNumber(activity,"+12025550101"));
            long previousEpoch=repository.epoch();
            sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_ENTER);waitForIdleSync();writerBarrier();
            check(repository.current().account(1).choice()==Choice.DEFAULT&&repository.current().account(1).revision()==revision+1&&repository.epoch()>previousEpoch,"Enter before row rebinding rejects the old revision without another mutation");
        }finally{runOnMainSync(()->list.suppressLayout(false));}
        until(()->ownsView(activity,v->v instanceof android.widget.Switch sw&&sw.isFocused()&&!sw.isChecked()&&String.valueOf(sw.getContentDescription()).endsWith("+12025550101. Block pending")));
        waitForIdleSync();
        check(repository.current().account(1).revision()==revision+1&&repository.current().account(2).choice()==Choice.ALLOW,"focused section move commits exactly one choice for the selected number");
        check(focusedNumber(activity,"+12025550101"),"section move preserves exact-number switch focus");
        sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_ENTER);until(()->repository.current().account(1).choice()==Choice.ALLOW);until(()->focusedNumber(activity,"+12025550101"));
        until(()->ownsView(activity,v->v instanceof android.widget.Switch sw&&sw.isFocused()&&sw.isChecked()&&String.valueOf(sw.getContentDescription()).endsWith("+12025550101. Enabled · unblocking pending")));
        waitForIdleSync();
        check(repository.current().account(1).revision()==revision+2,"hardware Enter commits exactly one change to the same number");
        long now=android.os.SystemClock.uptimeMillis();
        sendKeySync(new android.view.KeyEvent(now,now,android.view.KeyEvent.ACTION_DOWN,android.view.KeyEvent.KEYCODE_TAB,0,android.view.KeyEvent.META_SHIFT_ON));
        sendKeySync(new android.view.KeyEvent(now,android.os.SystemClock.uptimeMillis(),android.view.KeyEvent.ACTION_UP,android.view.KeyEvent.KEYCODE_TAB,0,android.view.KeyEvent.META_SHIFT_ON));waitForIdleSync();
        String backward=focusedControl(activity);
        check(ownsView(activity,v->v.isFocused()&&v.isScreenReaderFocusable()&&v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("Harbor Clinic, +12025550101, ")),"hardware Shift-Tab returns to this number's summary without changing its choice");
        sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_TAB);waitForIdleSync();
        check(focusedNumber(activity,"+12025550101")&&repository.current().account(1).revision()==revision+2,"hardware Tab returns to the same switch without mutating a choice; backward="+backward+"; forward="+focusedControl(activity));
        CountDownLatch held=new CountDownLatch(1),release=new CountDownLatch(1);
        writer().execute(()->{held.countDown();try{release.await(10,TimeUnit.SECONDS);}catch(InterruptedException error){Thread.currentThread().interrupt();}});
        check(held.await(10,TimeUnit.SECONDS),"writer held before the user changes focus");
        runOnMainSync(()->{repository.reload(()->{});find(activity.getWindow().getDecorView(),EditText.class).requestFocus();});
        release.countDown();writerBarrier();
        check(ownsView(activity,v->v instanceof EditText&&v.isFocused()),"a late refresh does not steal focus after the user selects Search");
        search(activity,"+12025550102");until(()->hasText(activity,"Parcel Desk")&&!hasText(activity,"Harbor Clinic"));
        check(!focusedNumber(activity,"+12025550102"),"recycled row cannot inherit a different number's action focus");
        committed(repository::reset);until(()->hasRow(activity,-14));
        committed(cb->repository.enableNumber("+12025550101","Replacement choice",cb));
        check(!focusedNumber(activity,"+12025550101")&&repository.disarmed(),"reset and reused number cannot restore old focus or action authority");
        runOnMainSync(activity::finish);
    }
    private boolean focusedNumber(Activity activity,String phone){
        boolean[] focused={false};runOnMainSync(()->{View v=activity.getCurrentFocus();focused[0]=v instanceof android.widget.Switch&&v.getContentDescription()!=null&&v.getContentDescription().toString().contains(phone);});return focused[0];
    }
    private String focusedControl(Activity activity){
        String[] value={"none"};runOnMainSync(()->{View v=activity.getCurrentFocus();if(v!=null)value[0]=v.getClass().getSimpleName()+": "+v.getContentDescription();});return value[0];
    }
    private void announcementRegressions()throws Exception{
        android.app.UiAutomation automation=getUiAutomation(android.app.UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES);
        java.util.concurrent.atomic.AtomicInteger announcements=new java.util.concurrent.atomic.AtomicInteger();
        automation.setOnAccessibilityEventListener(event->{
            if(getTargetContext().getPackageName().contentEquals(event.getPackageName()==null?"":event.getPackageName())&&event.getEventType()==android.view.accessibility.AccessibilityEvent.TYPE_ANNOUNCEMENT)announcements.incrementAndGet();
        });
        Activity activity=launch();
        try{
            search(activity,"+12025550101");until(()->hasText(activity,"Harbor Clinic")&&!hasText(activity,"Parcel Desk"));until(activity::hasWindowFocus);
            check(!ownsView(activity,v->v.getAccessibilityLiveRegion()!=View.ACCESSIBILITY_LIVE_REGION_NONE),"background status refreshes do not create repeated live-region announcements");
            clickOwn(activity,v->v instanceof android.widget.Switch&&v.getContentDescription()!=null&&v.getContentDescription().toString().contains("+12025550101"));
            until(()->repository.current().account(1).choice()==Choice.DEFAULT);until(()->announcements.get()>0);
            committed(repository::reload);Thread.sleep(500);
            check(announcements.get()==1,"one completed foreground choice emits exactly one owned accessibility announcement across refresh");
            CountDownLatch held=new CountDownLatch(1),release=new CountDownLatch(1);
            writer().execute(()->{held.countDown();try{release.await(10,TimeUnit.SECONDS);}catch(InterruptedException error){Thread.currentThread().interrupt();}});
            check(held.await(10,TimeUnit.SECONDS),"hold the actual user save before leaving the management page");
            try{
                clickOwn(activity,v->v instanceof android.widget.Switch&&v.getContentDescription()!=null&&v.getContentDescription().toString().contains("+12025550101"));
                check(activity.hasWindowFocus(),"leave only the owned focused Activity");sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_HOME);until(()->!activity.hasWindowFocus());
            }finally{release.countDown();}
            writerBarrier();until(()->repository.current().account(1).choice()==Choice.ALLOW);Thread.sleep(500);
            check(announcements.get()==1,"a save completing after Home cannot announce from the background");
            check(repository.disarmed()&&repository.current().account(2).choice()==Choice.ALLOW,"announcement handling leaves authority inactive and other choices unchanged");
        }finally{automation.setOnAccessibilityEventListener(null);runOnMainSync(activity::finish);}
    }
    private void serviceLifecycleRegressions()throws Exception{
        android.app.UiAutomation automation=getUiAutomation(android.app.UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES);
        android.content.ContentResolver resolver=getTargetContext().getContentResolver();
        String key=android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES;
        String original=android.provider.Settings.Secure.getString(resolver,key);
        String enabled=android.provider.Settings.Secure.getString(resolver,android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
        android.content.ComponentName component=new android.content.ComponentName(getTargetContext(),io.github.appunnim.businessgate.service.GateAccessibilityService.class);
        java.util.ArrayList<String> other=new java.util.ArrayList<>();
        if(original!=null&&!original.isEmpty())for(String entry:original.split(":"))if(!component.equals(android.content.ComponentName.unflattenFromString(entry)))other.add(entry);
        String without=String.join(":",other),with=without.isEmpty()?component.flattenToString():without+":"+component.flattenToString();
        automation.adoptShellPermissionIdentity(android.Manifest.permission.WRITE_SECURE_SETTINGS);
        try{
            check(android.provider.Settings.Secure.putString(resolver,key,without),"remove only owned debug screen service before permission test");
            until(()->!io.github.appunnim.businessgate.service.GateAccessibilityService.connected());
            check(android.provider.Settings.Secure.putString(resolver,key,with),"enable owned debug screen service through Android");
            until(io.github.appunnim.businessgate.service.GateAccessibilityService::connected);writerBarrier();
            check(repository.disarmed()&&repository.current().account(1).choice()==Choice.ALLOW,"actual service connection preserves durable choice and stops authority");
            check(!io.github.appunnim.businessgate.service.GateAccessibilityService.requestActivation()&&!io.github.appunnim.businessgate.service.GateAccessibilityService.requestApply(1),"enabled screen access cannot bypass the empty registry");
            long before=repository.epoch();
            check(android.provider.Settings.Secure.putString(resolver,key,without),"revoke actual owned screen access");
            until(()->!io.github.appunnim.businessgate.service.GateAccessibilityService.connected());writerBarrier();
            check(repository.epoch()>before&&repository.disarmed(),"actual permission revocation invalidates the authority epoch");
            check(repository.current().account(1).choice()==Choice.ALLOW&&repository.current().accounts().stream().allMatch(a->a.nonce().isEmpty()),"revocation retains choices and removes one-shot grants");
            check(android.provider.Settings.Secure.putString(resolver,key,with),"reconnect owned service after permission returns");
            until(io.github.appunnim.businessgate.service.GateAccessibilityService::connected);writerBarrier();
            check(repository.disarmed()&&io.github.appunnim.businessgate.service.GateAccessibilityService.packageSelected().isEmpty(),"reconnection does not restore a selected target or session");
            Activity activity=launch();until(activity::hasWindowFocus);
            check(hasText(activity,"Blocking is unavailable in this build. You can save choices here."),"management page reports compatibility truthfully after service reconnect");
            runOnMainSync(activity::finish);
        }finally{
            try{
                android.provider.Settings.Secure.putString(resolver,key,original);
                android.provider.Settings.Secure.putString(resolver,android.provider.Settings.Secure.ACCESSIBILITY_ENABLED,enabled);
                check(java.util.Objects.equals(original,android.provider.Settings.Secure.getString(resolver,key))&&java.util.Objects.equals(enabled,android.provider.Settings.Secure.getString(resolver,android.provider.Settings.Secure.ACCESSIBILITY_ENABLED)),"original emulator accessibility settings restored exactly");
            }finally{automation.dropShellPermissionIdentity();}
        }
    }
    private void reminderRegressions(boolean blockedChannel)throws Exception{
        android.app.NotificationManager manager=getTargetContext().getSystemService(android.app.NotificationManager.class);
        io.github.appunnim.businessgate.support.QuietReminder reminder=new io.github.appunnim.businessgate.support.QuietReminder(getTargetContext(),repository);
        check(manager.getActiveNotifications().length==0&&manager.getNotificationChannel("attention")==null,"notification matrix starts in the isolated debug installation");
        if(blockedChannel){
            manager.createNotificationChannel(new android.app.NotificationChannel("attention","Optional review",android.app.NotificationManager.IMPORTANCE_NONE));
            check(manager.getNotificationChannel("attention").getImportance()==android.app.NotificationManager.IMPORTANCE_NONE,"actual owned channel is blocked");
        }
        runOnMainSync(()->{repository.setting("sales_hints",true);repository.setting("digest",true);});writerBarrier();waitForIdleSync();
        if(android.os.Build.VERSION.SDK_INT>=33){
            check(getTargetContext().checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED,"actual own notification permission starts denied");
            runOnMainSync(reminder::onNewReviewEvidence);writerBarrier();waitForIdleSync();
            check(manager.getActiveNotifications().length==0,"permission denial prevents reminder delivery");
            getUiAutomation().grantRuntimePermission(getTargetContext().getPackageName(),android.Manifest.permission.POST_NOTIFICATIONS);
        }
        until(manager::areNotificationsEnabled);
        runOnMainSync(reminder::onNewReviewEvidence);writerBarrier();waitForIdleSync();
        if(blockedChannel){
            Thread.sleep(250);check(manager.getActiveNotifications().length==0,"blocked channel prevents actual reminder delivery");
            check(repository.current().account(1).choice()==Choice.ALLOW&&repository.disarmed(),"blocked notifications do not alter choices or enable actions");return;
        }
        until(()->manager.getActiveNotifications().length==1);
        android.app.Notification notification=manager.getActiveNotifications()[0].getNotification();
        android.app.NotificationChannel channel=manager.getNotificationChannel("attention");
        check(channel.getImportance()==android.app.NotificationManager.IMPORTANCE_LOW&&channel.getSound()==null&&!channel.shouldVibrate(),"actual reminder channel is low importance, silent and nonvibrating");
        check(notification.visibility==android.app.Notification.VISIBILITY_PRIVATE&&notification.fullScreenIntent==null,"reminder is private and has no foreground takeover");
        check(notification.extras.getCharSequence(android.app.Notification.EXTRA_TITLE).toString().equals(getTargetContext().getString(R.string.reminder_title))&&notification.extras.getCharSequence(android.app.Notification.EXTRA_TEXT).toString().equals(getTargetContext().getString(R.string.reminder_detail)),"actual notification contains only generic owned copy, no sender number or message text");
        if(android.os.Build.VERSION.SDK_INT>=31)check(notification.contentIntent.isImmutable(),"reminder PendingIntent cannot accept caller modifications");
        runOnMainSync(reminder::clear);until(()->manager.getActiveNotifications().length==0);
        for(long previous:new long[]{System.currentTimeMillis(),System.currentTimeMillis()+86400000L,System.currentTimeMillis()-29L*86400000}){
            try(GateDbHelper helper=new GateDbHelper(getTargetContext())){helper.getWritableDatabase().execSQL("UPDATE app_meta SET value=? WHERE key='last_digest_ms'",new Object[]{""+previous});}
            runOnMainSync(reminder::onNewReviewEvidence);writerBarrier();waitForIdleSync();Thread.sleep(100);
            check(manager.getActiveNotifications().length==0,"30-day cap and stored future-clock boundary prevent repeated delivery");
        }
        try(GateDbHelper helper=new GateDbHelper(getTargetContext())){helper.getWritableDatabase().execSQL("UPDATE app_meta SET value=? WHERE key='last_digest_ms'",new Object[]{""+(System.currentTimeMillis()-31L*86400000)});}
        runOnMainSync(reminder::onNewReviewEvidence);until(()->manager.getActiveNotifications().length==1);
        check(manager.getActiveNotifications().length==1,"eligible evidence can produce a reminder after the cap window");
        committed(cb->repository.updateSetup("WELCOME",false,cb));until(()->manager.getActiveNotifications().length==0);
        check(!repository.optionEnabled("digest")&&!repository.optionEnabled("sales_hints"),"consent withdrawal clears reminder options and actual notification");
        check(repository.current().account(1).choice()==Choice.ALLOW&&repository.disarmed(),"reminder lifecycle preserves exact choices and inactive authority");
    }
    private EditText ownInput(android.app.AlertDialog dialog,String label){
        EditText[] result={null};runOnMainSync(()->result[0]=(EditText)ownView(dialog.getWindow().getDecorView(),v->label.equals(v.getContentDescription())));return result[0];
    }
    private boolean imeVisible(Activity activity){
        boolean[] value={false};runOnMainSync(()->{android.view.WindowInsets insets=activity.getWindow().getDecorView().getRootWindowInsets();value[0]=insets!=null&&(android.os.Build.VERSION.SDK_INT>=30?insets.isVisible(android.view.WindowInsets.Type.ime()):insets.getSystemWindowInsetBottom()>activity.getResources().getDisplayMetrics().density*100);});return value[0];
    }
    private boolean dialogImeVisible(android.app.AlertDialog dialog){
        boolean[] visible={false};runOnMainSync(()->{android.view.WindowInsets insets=dialog.getWindow().getDecorView().getRootWindowInsets();visible[0]=insets!=null&&(android.os.Build.VERSION.SDK_INT>=30?insets.isVisible(android.view.WindowInsets.Type.ime()):insets.getSystemWindowInsetBottom()>dialog.getContext().getResources().getDisplayMetrics().density*100);});return visible[0];
    }
    private java.util.concurrent.atomic.AtomicBoolean observeImeAnimation(View decor){
        java.util.concurrent.atomic.AtomicBoolean moving=new java.util.concurrent.atomic.AtomicBoolean();
        if(android.os.Build.VERSION.SDK_INT>=30)runOnMainSync(()->decor.setWindowInsetsAnimationCallback(new android.view.WindowInsetsAnimation.Callback(android.view.WindowInsetsAnimation.Callback.DISPATCH_MODE_CONTINUE_ON_SUBTREE){
            private final java.util.Set<android.view.WindowInsetsAnimation> animations=new java.util.HashSet<>();
            @Override public void onPrepare(android.view.WindowInsetsAnimation animation){if((animation.getTypeMask()&android.view.WindowInsets.Type.ime())!=0){animations.add(animation);moving.set(true);}}
            @Override public android.view.WindowInsets onProgress(android.view.WindowInsets insets,java.util.List<android.view.WindowInsetsAnimation> running){
                for(android.view.WindowInsetsAnimation animation:running)if((animation.getTypeMask()&android.view.WindowInsets.Type.ime())!=0)animations.add(animation);
                moving.set(!animations.isEmpty());return insets;
            }
            @Override public void onEnd(android.view.WindowInsetsAnimation animation){animations.remove(animation);moving.set(!animations.isEmpty());}
        }));
        return moving;
    }
    private void ownedBack(Activity activity)throws Exception{
        check(activity.hasWindowFocus(),"Back is sent only while the owned Activity has input focus");
        sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);waitForIdleSync();Thread.sleep(250);
    }
    private void interactionRegressions()throws Exception{
        Activity activity=launch();java.util.concurrent.atomic.AtomicBoolean activityImeMoving=observeImeAnimation(activity.getWindow().getDecorView());
        search(activity,"+12025550101");until(()->hasText(activity,"Harbor Clinic")&&!hasText(activity,"Parcel Desk"));
        clickOwn(activity,v->v.getContentDescription()!=null&&v.getContentDescription().toString().contains("+12025550101")&&v.getContentDescription().toString().endsWith("Show details"));
        runOnMainSync(()->{EditText field=find(activity.getWindow().getDecorView(),EditText.class);field.requestFocus();activity.getSystemService(android.view.inputmethod.InputMethodManager.class).showSoftInput(field,android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);});
        until(()->imeVisible(activity)&&!activityImeMoving.get());check(imeVisible(activity),"real search IME is visible after its transition");
        ownedBack(activity);until(()->!imeVisible(activity)&&!activityImeMoving.get());
        check(!activity.isFinishing()&&ownsView(activity,v->v.getContentDescription()!=null&&v.getContentDescription().toString().endsWith("Collapse details")),"first Back dismisses IME and preserves expanded details");
        check(ownsView(activity,v->v instanceof EditText text&&text.getText().toString().equals("+12025550101")),"IME dismissal preserves query");
        ownedBack(activity);check(!activity.isFinishing(),"Back must collapse details before finishing");
        try{until(()->ownsView(activity,v->v.getContentDescription()!=null&&v.getContentDescription().toString().endsWith("Show details")));}
        catch(AssertionError failure){throw new AssertionError("Second Back did not expose the collapsed summary: ime="+imeVisible(activity)+", focused="+activity.hasWindowFocus()+", expandedControl="+ownsView(activity,v->v.getContentDescription()!=null&&v.getContentDescription().toString().endsWith("Collapse details"))+", accountMounted="+hasText(activity,"Harbor Clinic"),failure);}
        check(!activity.isFinishing(),"second Back collapses details without leaving the page");
        invokeOwned(activity,"addNumber",null);until(()->ownDialog(activity)!=null);android.app.AlertDialog form=ownDialog(activity);
        java.util.concurrent.atomic.AtomicBoolean formImeMoving=observeImeAnimation(form.getWindow().getDecorView());
        until(()->form.getWindow().getDecorView().hasWindowFocus());check(true,"number dialog input window is ready");
        EditText phone=ownInput(form,"Full phone number with country code");
        runOnMainSync(()->{phone.setText("+12025550195");phone.requestFocus();});
        android.view.inputmethod.InputMethodManager input=activity.getSystemService(android.view.inputmethod.InputMethodManager.class);
        until(()->input.isActive(phone));check(input.isActive(phone),"number field has a real input connection");
        runOnMainSync(()->input.showSoftInput(phone,android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT));
        until(()->(imeVisible(activity)||dialogImeVisible(form))&&!formImeMoving.get()&&!activityImeMoving.get());
        check(form.getWindow().getDecorView().hasWindowFocus(),"owned number form has input focus");sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);
        until(()->!imeVisible(activity)&&!dialogImeVisible(form)&&!formImeMoving.get()&&!activityImeMoving.get());
        check(!dialogImeVisible(form),"first form Back completes IME dismissal before the next input");
        check(form.isShowing(),"first form Back dismisses IME without canceling unsent text");
        check(ownInput(form,"Full phone number with country code").getText().toString().equals("+12025550195"),"number text survives IME dismissal");
        check(form.getWindow().getDecorView().hasWindowFocus(),"owned form retains input focus after IME dismissal");
        sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK);
        try{until(()->ownDialog(activity)==null);}
        catch(AssertionError failure){throw new AssertionError("Second form Back did not cancel: showing="+form.isShowing()+", ime="+dialogImeVisible(form)+", moving="+formImeMoving.get()+", focused="+form.getWindow().getDecorView().hasWindowFocus(),failure);}
        check(repository.current().accounts().stream().noneMatch(a->a.phone().equals("+12025550195")),"second form Back cancels without saving");
        until(activity::hasWindowFocus);ownedBack(activity);until(activity::isFinishing);
        check(repository.current().account(1).choice()==Choice.ALLOW&&repository.disarmed(),"exit retains choices and never activates actions");
    }
    private void talkBackRegressions()throws Exception{
        android.app.UiAutomation automation=getUiAutomation(android.app.UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES);
        android.view.accessibility.AccessibilityManager manager=getTargetContext().getSystemService(android.view.accessibility.AccessibilityManager.class);
        until(()->manager.getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_SPOKEN).stream().anyMatch(s->s.getResolveInfo().serviceInfo.packageName.equals("com.google.android.marvin.talkback")));
        until(manager::isTouchExplorationEnabled);
        check(manager.isTouchExplorationEnabled(),"TalkBack touch exploration is actually enabled");
        Activity activity=launch();search(activity,"+12025550101");until(()->hasText(activity,"Harbor Clinic")&&!hasText(activity,"Parcel Desk"));
        java.util.function.Predicate<View> summary=v->v.isAccessibilityFocused()&&v.isScreenReaderFocusable()&&v.getContentDescription()!=null&&v.getContentDescription().toString().startsWith("Harbor Clinic, +12025550101, ");
        for(int step=0;step<20&&!ownsView(activity,summary);step++){
            until(activity::hasWindowFocus);
            android.view.accessibility.AccessibilityNodeInfo root=automation.getRootInActiveWindow();
            check(root!=null&&getTargetContext().getPackageName().equals(String.valueOf(root.getPackageName())),"Native Tab with TalkBack is confined to the owned debug window");
            long now=android.os.SystemClock.uptimeMillis();
            injectOwnedKey(automation,now,android.view.KeyEvent.ACTION_DOWN,android.view.KeyEvent.KEYCODE_TAB,0);
            injectOwnedKey(automation,now,android.view.KeyEvent.ACTION_UP,android.view.KeyEvent.KEYCODE_TAB,0);
            Thread.sleep(450);
        }
        check(ownsView(activity,summary),"actual TalkBack native Tab traversal reaches the grouped exact-number summary");
        long navigation=android.os.SystemClock.uptimeMillis();
        injectOwnedKey(automation,navigation,android.view.KeyEvent.ACTION_DOWN,android.view.KeyEvent.KEYCODE_TAB,0);
        injectOwnedKey(automation,navigation,android.view.KeyEvent.ACTION_UP,android.view.KeyEvent.KEYCODE_TAB,0);
        until(()->ownsView(activity,v->v.isAccessibilityFocused()&&v instanceof android.widget.Switch&&v.getContentDescription()!=null&&v.getContentDescription().toString().contains("+12025550101")));
        check(repository.current().account(1).choice()==Choice.ALLOW,"actual TalkBack traversal reaches this number's switch without changing its choice");
        navigation=android.os.SystemClock.uptimeMillis();
        injectOwnedKey(automation,navigation,android.view.KeyEvent.ACTION_DOWN,android.view.KeyEvent.KEYCODE_TAB,android.view.KeyEvent.META_SHIFT_ON);
        injectOwnedKey(automation,navigation,android.view.KeyEvent.ACTION_UP,android.view.KeyEvent.KEYCODE_TAB,android.view.KeyEvent.META_SHIFT_ON);
        until(()->ownsView(activity,summary));check(ownsView(activity,summary),"actual TalkBack reverse traversal returns to this number's summary");
        committed(repository::reload);writerBarrier();Thread.sleep(350);
        check(ownsView(activity,summary),"TalkBack focus stays on the same summary after committed refresh");
        Account account=repository.current().account(1);committed(cb->repository.choose(account,Choice.DEFAULT,cb));writerBarrier();Thread.sleep(350);
        check(ownsView(activity,summary),"TalkBack focus stays on the same number when its section and saved choice change");
        check(ownsView(activity,v->v instanceof android.widget.Switch&&v.getContentDescription()!=null&&v.getContentDescription().toString().contains("+12025550101")&&!((android.widget.Switch)v).isChecked()),"TalkBack sees the current native checked state and full number");
        search(activity,"+12025550102");until(()->hasText(activity,"Parcel Desk")&&!hasText(activity,"Harbor Clinic"));Thread.sleep(350);
        check(!ownsView(activity,v->v.isAccessibilityFocused()&&v instanceof android.widget.Switch),"a filtered-out sender cannot transfer TalkBack focus to a different number's switch");
        check(repository.current().account(2).choice()==Choice.ALLOW&&repository.disarmed(),"screen-reader traversal leaves other exact-number choices and action inactivity intact");
        runOnMainSync(activity::finish);
    }
    private void injectOwnedKey(android.app.UiAutomation automation,long down,int action,int code,int meta){
        android.view.KeyEvent event=new android.view.KeyEvent(down,android.os.SystemClock.uptimeMillis(),action,code,0,meta,android.view.KeyCharacterMap.VIRTUAL_KEYBOARD,0,android.view.KeyEvent.FLAG_FROM_SYSTEM,android.view.InputDevice.SOURCE_KEYBOARD);
        if(!automation.injectInputEvent(event,true))throw new AssertionError("Owned keyboard event rejected");
    }
    private android.app.AlertDialog ownDialog(Activity activity){
        android.app.AlertDialog[] found={null};runOnMainSync(()->{
            try{
                java.lang.reflect.Field field=activity.getClass().getDeclaredField("dialogs");field.setAccessible(true);
                for(Object value:(java.util.List<?>)field.get(activity))if(value instanceof android.app.AlertDialog dialog&&dialog.isShowing())found[0]=dialog;
            }catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
        });return found[0];
    }
    private void invokeOwned(Activity activity,String method,Account account){
        runOnMainSync(()->{
            try{
                java.lang.reflect.Method action=account==null?activity.getClass().getDeclaredMethod(method):activity.getClass().getDeclaredMethod(method,Account.class);
                action.setAccessible(true);if(account==null)action.invoke(activity);else action.invoke(activity,account);
            }catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
        });
    }
    private void dialogRegressions()throws Exception{
        Activity first=launch();until(()->hasText(first,"Business Gate"));
        invokeOwned(first,"addNumber",null);android.app.AlertDialog form=ownDialog(first);check(form!=null,"owned number form opens");
        runOnMainSync(()->{
            EditText phone=(EditText)ownView(form.getWindow().getDecorView(),v->"Full phone number with country code".equals(v.getContentDescription()));
            EditText name=(EditText)ownView(form.getWindow().getDecorView(),v->"Optional local name".equals(v.getContentDescription()));
            phone.setText("+12025550181");name.setText("Unsent local form");
        });
        Activity restored=recreate(first);until(()->ownDialog(restored)!=null);android.app.AlertDialog resumedForm=ownDialog(restored);
        check(ownView(resumedForm.getWindow().getDecorView(),v->v instanceof EditText e&&e.getText().toString().equals("+12025550181"))!=null,"rotation restores the unsent exact number");
        check(ownView(resumedForm.getWindow().getDecorView(),v->v instanceof EditText e&&e.getText().toString().equals("Unsent local form"))!=null,"rotation restores the unsent optional name");
        check(repository.current().accounts().stream().noneMatch(a->a.phone().equals("+12025550181")),"restored form never auto-submits a choice");
        runOnMainSync(()->resumedForm.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).performClick());
        until(()->ownDialog(restored)==null);
        check(ownDialog(restored)==null&&repository.disarmed(),"Cancel closes the form without activating actions");
        runOnMainSync(()->resumedForm.getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick());writerBarrier();
        check(repository.current().accounts().stream().noneMatch(a->a.phone().equals("+12025550181")),"a canceled form's late positive callback cannot save a number");
        invokeOwned(restored,"addNumber",null);android.app.AlertDialog staleForm=ownDialog(restored);
        runOnMainSync(()->((EditText)ownView(staleForm.getWindow().getDecorView(),v->"Full phone number with country code".equals(v.getContentDescription()))).setText("+12025550182"));
        committed(repository::reset);until(()->ownDialog(restored)==null);
        runOnMainSync(()->staleForm.getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick());writerBarrier();
        check(repository.current().accounts().isEmpty()&&repository.disarmed(),"a stale positive button cannot recreate choices after reset");
        seed();until(()->repository.current().accounts().size()==6);until(()->hasText(restored,"Harbor Clinic"));
        Account original=repository.current().account(1);long revision=original.revision();String nonce=original.nonce();
        invokeOwned(restored,"unblockNow",original);android.app.AlertDialog unblock=ownDialog(restored);
        check(unblock!=null,"Unblock now requires a separate exact-number review");
        runOnMainSync(()->unblock.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).performClick());
        until(()->ownDialog(restored)==null);
        check(repository.current().account(1).revision()==revision&&repository.current().account(1).nonce().equals(nonce),"canceling unblock creates no new grant or revision");
        runOnMainSync(()->unblock.getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick());writerBarrier();
        check(repository.current().account(1).revision()==revision&&repository.current().account(1).nonce().equals(nonce),"a canceled unblock review cannot execute a late positive callback");
        invokeOwned(restored,"manualBlock",original);android.app.AlertDialog oldBlock=ownDialog(restored);
        committed(cb->repository.choose(original,Choice.ALLOW,cb));long changed=repository.current().account(1).revision();
        runOnMainSync(()->oldBlock.getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick());writerBarrier();
        check(repository.current().account(1).choice()==Choice.ALLOW&&repository.current().account(1).revision()==changed,"an older manual confirmation cannot override a newer exact-number choice");
        invokeOwned(restored,"clearLocal",null);android.app.AlertDialog deletion=ownDialog(restored);
        Activity recreated=recreate(restored);until(()->hasRow(recreated,1));
        check(ownDialog(recreated)==null,"destructive confirmation is not restored after recreation");
        runOnMainSync(()->deletion.getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick());writerBarrier();
        check(repository.current().accounts().size()==6,"a destroyed Activity's confirmation cannot delete local data");
        search(recreated,"😀".repeat(130));
        check(ownsView(recreated,v->v instanceof EditText e&&Character.codePointCount(e.getText(),0,e.length())==128&&e.length()==256),"search limit counts Unicode code points without splitting supplementary characters");
        search(recreated,"");
        for(String method:new String[]{"settings","compatibility","diagnostics","accessDisclosure","notificationDisclosure","salesDisclosure","clearLocal"}){
            invokeOwned(recreated,method,null);android.app.AlertDialog dialog=ownDialog(recreated);check(dialog!=null,"owned supporting dialog opens: "+method);
            runOnMainSync(dialog::cancel);until(()->ownDialog(recreated)==null);check(ownDialog(recreated)==null,"supporting dialog cancels without replay: "+method);
        }
        check(repository.current().accounts().size()==6&&repository.disarmed(),"supporting dialog cancellation preserves choices and inactive state");
        invokeOwned(recreated,"settings",null);android.app.AlertDialog settings=ownDialog(recreated);until(()->settings.getWindow().getDecorView().hasWindowFocus());
        View hints=ownView(settings.getWindow().getDecorView(),v->v instanceof android.widget.Switch sw&&sw.getText().toString().equals("Optional sales hints"));
        View effort=ownView(settings.getWindow().getDecorView(),v->v instanceof android.widget.Button b&&b.getText().toString().equals("View local effort"));
        CountDownLatch held=new CountDownLatch(1),release=new CountDownLatch(1);
        writer().execute(()->{held.countDown();try{release.await(10,TimeUnit.SECONDS);}catch(InterruptedException error){Thread.currentThread().interrupt();}});
        check(held.await(10,TimeUnit.SECONDS),"hold the requested effort result before canceling Settings");
        try{runOnMainSync(()->{effort.performClick();settings.cancel();hints.performClick();});}finally{release.countDown();}
        writerBarrier();Thread.sleep(250);
        check(ownDialog(recreated)==null&&!repository.current().salesHints(),"canceled settings cannot reopen through a delayed result or old switch callback");
        invokeOwned(recreated,"compatibility",null);android.app.AlertDialog compatibility=ownDialog(recreated);until(()->compatibility.getWindow().getDecorView().hasWindowFocus());
        View local=ownView(compatibility.getWindow().getDecorView(),v->v instanceof android.widget.Button b&&b.getText().toString().equals("Manage unconnected choices"));
        GateRepository.ChoiceScope before=repository.choiceScope();
        runOnMainSync(()->{compatibility.cancel();local.performClick();});writerBarrier();
        check(before.equals(repository.choiceScope()),"a canceled connection dialog's old button cannot switch choice namespaces");
        runOnMainSync(recreated::finish);
    }
    private boolean ownsView(Activity activity,java.util.function.Predicate<View> predicate){
        boolean[] found={false};runOnMainSync(()->found[0]=ownView(activity.getWindow().getDecorView(),predicate)!=null);return found[0];
    }
    private void search(Activity activity,String value){runOnMainSync(()->find(activity.getWindow().getDecorView(),EditText.class).setText(value));}
    private void clickOwn(Activity activity,java.util.function.Predicate<View> predicate)throws Exception{
        until(()->ownsView(activity,predicate));runOnMainSync(()->ownView(activity.getWindow().getDecorView(),predicate).performClick());
    }
    private Activity recreate(Activity activity)throws Exception{
        ActivityMonitor monitor=addMonitor(io.github.appunnim.businessgate.ui.MainActivity.class.getName(),null,false);
        try{runOnMainSync(activity::recreate);Activity replacement=monitor.waitForActivityWithTimeout(10000);check(replacement!=null&&replacement!=activity,"replacement Activity created");waitForIdleSync();return replacement;}
        finally{removeMonitor(monitor);}
    }
    private void layoutRegressions()throws Exception{
        committed(cb->repository.enableNumber("+120255512345678","Clinical supplies and regional scheduling ".repeat(3),cb));
        Activity original=launch();int orientation=arguments.getString("orientation","portrait").equals("landscape")?android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE:android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        int expectedOrientation=orientation==android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE?android.content.res.Configuration.ORIENTATION_LANDSCAPE:android.content.res.Configuration.ORIENTATION_PORTRAIT;
        Activity displayed=original;
        if(original.getResources().getConfiguration().orientation!=expectedOrientation){
            ActivityMonitor monitor=addMonitor(io.github.appunnim.businessgate.ui.MainActivity.class.getName(),null,false);
            try{runOnMainSync(()->original.setRequestedOrientation(orientation));displayed=monitor.waitForActivityWithTimeout(10000);check(displayed!=null,"requested orientation creates an Activity");}
            finally{removeMonitor(monitor);}
        }
        Activity activity=displayed;until(()->activity.getResources().getConfiguration().orientation==expectedOrientation);waitForIdleSync();
        check(Math.abs(activity.getResources().getConfiguration().fontScale-Float.parseFloat(arguments.getString("font","1.0")))<0.01,"requested font scale is applied");
        boolean dark=arguments.getString("night","light").equals("dark");
        check((activity.getResources().getConfiguration().uiMode&android.content.res.Configuration.UI_MODE_NIGHT_MASK)==(dark?android.content.res.Configuration.UI_MODE_NIGHT_YES:android.content.res.Configuration.UI_MODE_NIGHT_NO),"requested theme is applied");
        until(()->ownsView(activity,v->v instanceof android.widget.ListView list&&list.getCount()>5));
        metrics+="METRIC width_px="+activity.getWindow().getDecorView().getWidth()+" height_px="+activity.getWindow().getDecorView().getHeight()+" density_dpi="+activity.getResources().getDisplayMetrics().densityDpi+" font_percent="+Math.round(activity.getResources().getConfiguration().fontScale*100)+"\n";
        java.util.List<String> failures=new java.util.ArrayList<>();
        runOnMainSync(()->{
            android.widget.ListView list=find(activity.getWindow().getDecorView(),android.widget.ListView.class);
            if(list.getHeight()<48*activity.getResources().getDisplayMetrics().density)failures.add("list viewport "+list.getHeight()+"px smaller than 48dp; density="+activity.getResources().getDisplayMetrics().density+"; owned window="+activity.getWindow().getDecorView().getWidth()+"x"+activity.getWindow().getDecorView().getHeight());
            inspectLayout(activity.getWindow().getDecorView(),failures);
        });
        captureOwnedView(activity,"layout.png");
        check(failures.isEmpty(),"visible layout: "+String.join(", ",failures));
        boolean compactSummary=hasRow(activity,-18);
        search(activity,"+120255512345678");until(()->ownsView(activity,v->v instanceof android.widget.ListView list&&list.getCount()==(compactSummary?4:3)));
        long target=repository.current().accounts().stream().filter(a->a.phone().equals("+120255512345678")).findFirst().orElseThrow().id();scrollTo(activity,target,0);
        until(()->ownsView(activity,v->v.getContentDescription()!=null&&v.getContentDescription().toString().contains("+120255512345678")));
        clickOwn(activity,v->v.getContentDescription()!=null&&v.getContentDescription().toString().contains("+120255512345678")&&v.getContentDescription().toString().endsWith("Show details"));
        until(()->ownsView(activity,v->v.getContentDescription()!=null&&v.getContentDescription().toString().contains("+120255512345678")&&v.getContentDescription().toString().endsWith("Collapse details")));
        runOnMainSync(()->inspectLayout(activity.getWindow().getDecorView(),failures));
        captureOwnedView(activity,"layout-expanded.png");
        check(failures.isEmpty(),"expanded layout: "+String.join(", ",failures));
        boolean[] reached={false};
        for(int step=0;step<20&&!reached[0];step++){
            runOnMainSync(()->{
                android.widget.ListView list=find(activity.getWindow().getDecorView(),android.widget.ListView.class);
                View action=ownView(activity.getWindow().getDecorView(),v->v instanceof android.widget.Button button&&button.getText().toString().equals("Unblock now"));
                if(action==null)return;
                android.graphics.Rect visible=new android.graphics.Rect();reached[0]=action.getGlobalVisibleRect(visible)&&visible.height()>=Math.min(action.getHeight(),48*activity.getResources().getDisplayMetrics().density)-1;
                if(!reached[0]){int[] actionPosition=new int[2],listPosition=new int[2];action.getLocationOnScreen(actionPosition);list.getLocationOnScreen(listPosition);list.scrollListBy(actionPosition[1]-listPosition[1]);}
            });
            waitForIdleSync();
        }
        captureOwnedView(activity,"layout-action.png");check(reached[0],"expanded action remains reachable through the single list");
        check(repository.current().account(1).choice()==Choice.ALLOW&&repository.disarmed(),"layout changes never replay policy or resume actions");
        if(compactSummary){
            search(activity,"");until(()->hasRow(activity,1));scrollTo(activity,-18,0);
            until(()->hasText(activity,"Business Gate")&&hasText(activity,"Blocking is unavailable in this build. You can save choices here."));
            check(hasText(activity,"Business Gate")&&hasText(activity,"Blocking is unavailable in this build. You can save choices here."),"compact title and status return after filtered row recycling");
        }
        if(activity.getResources().getConfiguration().fontScale>=1.99f)dialogLayouts(activity);
    }
    private void dialogLayouts(Activity activity)throws Exception{
        for(String method:new String[]{"addNumber","settings","compatibility","diagnostics","accessDisclosure","notificationDisclosure","salesDisclosure","clearLocal","manualBlock","unblockNow"}){
            invokeOwned(activity,method,(method.equals("manualBlock")||method.equals("unblockNow"))?repository.current().account(1):null);
            until(()->ownDialog(activity)!=null);android.app.AlertDialog dialog=ownDialog(activity);waitForIdleSync();
            java.util.List<String> errors=new java.util.ArrayList<>();
            runOnMainSync(()->{
                inspectLayout(dialog.getWindow().getDecorView(),errors);
                for(int which:new int[]{android.app.AlertDialog.BUTTON_POSITIVE,android.app.AlertDialog.BUTTON_NEGATIVE}){
                    View button=dialog.getButton(which);if(button==null||button.getVisibility()!=View.VISIBLE)continue;
                    android.graphics.Rect visible=new android.graphics.Rect();
                    if((!button.getGlobalVisibleRect(visible)||visible.height()+1<button.getHeight()||visible.width()+1<button.getWidth())&&!scrollableAncestor(button))errors.add("fixed dialog action clipped");
                    if(button.getHeight()+1<48*activity.getResources().getDisplayMetrics().density)errors.add("dialog action below 48dp");
                }
            });
            java.util.List<View> controls=new java.util.ArrayList<>();
            runOnMainSync(()->collectControls(dialog.getWindow().getDecorView(),controls));
            for(View control:controls){
                runOnMainSync(()->control.requestRectangleOnScreen(new android.graphics.Rect(0,0,control.getWidth(),control.getHeight()),true));waitForIdleSync();
                runOnMainSync(()->{android.graphics.Rect visible=new android.graphics.Rect();if(!control.getGlobalVisibleRect(visible)||visible.height()+1<control.getHeight()||visible.width()+1<control.getWidth())errors.add("full dialog child control unreachable: "+control.getClass().getSimpleName()+" "+visible.width()+"x"+visible.height()+" of "+control.getWidth()+"x"+control.getHeight());});
            }
            captureOwnedRoot(dialog.getWindow().getDecorView(),"dialog-"+method+".png");
            check(errors.isEmpty(),"large-text dialog "+method+": "+String.join(", ",errors));
            runOnMainSync(dialog::cancel);until(()->ownDialog(activity)==null);
        }
        check(repository.disarmed(),"dialog layout checks leave action authority inactive");
    }
    private boolean scrollableAncestor(View view){
        for(android.view.ViewParent parent=view.getParent();parent instanceof View ancestor;parent=ancestor.getParent())if(ancestor instanceof android.widget.ScrollView&&(ancestor.canScrollVertically(-1)||ancestor.canScrollVertically(1)))return true;
        return false;
    }
    private void collectControls(View view,java.util.List<View> result){
        if(!view.isShown())return;
        if(view instanceof android.widget.Button||view instanceof EditText)result.add(view);
        if(view instanceof ViewGroup group)for(int i=0;i<group.getChildCount();i++)collectControls(group.getChildAt(i),result);
    }
    private void captureOwnedView(Activity activity,String name)throws Exception{captureOwnedRoot(activity.getWindow().getDecorView(),name);}
    private void captureOwnedRoot(View root,String name)throws Exception{
        android.graphics.Bitmap[] rendered={null};
        runOnMainSync(()->{rendered[0]=android.graphics.Bitmap.createBitmap(root.getWidth(),root.getHeight(),android.graphics.Bitmap.Config.ARGB_8888);root.draw(new android.graphics.Canvas(rendered[0]));});
        try(var stream=new java.io.FileOutputStream(new java.io.File(getTargetContext().getCacheDir(),name))){if(!rendered[0].compress(android.graphics.Bitmap.CompressFormat.PNG,100,stream))throw new java.io.IOException("OWN_RENDER_FAILED");}
        finally{rendered[0].recycle();}
    }
    private void inspectLayout(View view,java.util.List<String> failures){
        android.graphics.Rect visible=new android.graphics.Rect();if(!view.isShown())return;boolean onScreen=view.getGlobalVisibleRect(visible);
        if(view instanceof TextView text&&text.getLayout()!=null&&!(view instanceof EditText)){
            if(onScreen&&visible.width()+1<view.getWidth())failures.add("horizontal clipping in "+view.getClass().getSimpleName());
            int available=view.getWidth()-text.getCompoundPaddingLeft()-text.getCompoundPaddingRight();
            for(int line=0;line<text.getLayout().getLineCount();line++)if(text.getLayout().getLineMax(line)>available+1||text.getLayout().getEllipsisCount(line)>0){failures.add("text overflow in "+view.getClass().getSimpleName()+" line="+line+" width="+Math.round(text.getLayout().getLineWidth(line))+" visible="+Math.round(text.getLayout().getLineMax(line))+" available="+available);break;}
        }
        if(view instanceof ViewGroup group)for(int i=0;i<group.getChildCount();i++)inspectLayout(group.getChildAt(i),failures);
    }
    private long visibleRow(Activity activity){long[] id={Long.MIN_VALUE};runOnMainSync(()->{android.widget.ListView list=find(activity.getWindow().getDecorView(),android.widget.ListView.class);id[0]=list.getAdapter().getItemId(list.getFirstVisiblePosition());});return id[0];}
    private int visibleTop(Activity activity){int[] top={0};runOnMainSync(()->{android.widget.ListView list=find(activity.getWindow().getDecorView(),android.widget.ListView.class);if(list.getChildCount()>0)top[0]=list.getChildAt(0).getTop();});return top[0];}
    private boolean hasRow(Activity activity,long id){return ownsView(activity,v->{if(!(v instanceof android.widget.ListView list))return false;for(int i=0;i<list.getCount();i++)if(list.getAdapter().getItemId(i)==id)return true;return false;});}
    private void scrollTo(Activity activity,long id,int top){runOnMainSync(()->{android.widget.ListView list=find(activity.getWindow().getDecorView(),android.widget.ListView.class);for(int i=0;i<list.getCount();i++)if(list.getAdapter().getItemId(i)==id){list.setSelectionFromTop(i,top);return;}throw new AssertionError("owned target row exists");});}
    private void uiRegressions()throws Exception{
        insertSyntheticAccounts(0,40);
        try(GateDbHelper helper=new GateDbHelper(getTargetContext())){helper.getWritableDatabase().execSQL("UPDATE account SET kind='BUSINESS_CONFIRMED',ever_business=1 WHERE phone LIKE '+1999%'");}
        committed(repository::reload);
        Activity first=launch();search(first,"+12025550101");until(()->hasText(first,"Harbor Clinic"));
        clickOwn(first,v->v.getContentDescription()!=null&&v.getContentDescription().toString().contains("+12025550101")&&v.getContentDescription().toString().endsWith("Show details"));
        check(ownsView(first,v->v.getContentDescription()!=null&&v.getContentDescription().toString().endsWith("Collapse details")),"exact-number details expanded");
        Activity replacement=recreate(first);until(()->hasText(replacement,"Harbor Clinic"));
        check(ownsView(replacement,v->v instanceof EditText e&&e.getText().toString().equals("+12025550101")),"search survives Activity recreation");
        check(ownsView(replacement,v->v.getContentDescription()!=null&&v.getContentDescription().toString().contains("+12025550101")&&v.getContentDescription().toString().endsWith("Collapse details")),"same exact-number details survive Activity recreation");
        check(repository.current().account(1).choice()==Choice.ALLOW&&repository.disarmed(),"recreation does not replay a choice or resume actions");
        clickOwn(replacement,v->v.getContentDescription()!=null&&v.getContentDescription().toString().endsWith("Collapse details"));
        search(replacement,"");until(()->ownsView(replacement,v->v instanceof android.widget.ListView list&&list.getCount()>40));
        scrollTo(replacement,30,-12);until(()->visibleRow(replacement)==30);int anchorTop=visibleTop(replacement);
        search(replacement,"+12025550101");until(()->hasText(replacement,"Harbor Clinic"));
        Activity filtered=recreate(replacement);until(()->hasText(filtered,"Harbor Clinic"));
        search(filtered,"");until(()->visibleRow(filtered)==30);
        check(Math.abs(visibleTop(filtered)-anchorTop)<=1,"clearing search restores exact pre-search anchor after recreation");
        Activity scrolled=recreate(filtered);until(()->visibleRow(scrolled)==30);
        check(Math.abs(visibleTop(scrolled)-anchorTop)<=1,"scrolled row and offset survive recreation after asynchronous results arrive");
        search(scrolled,"+12025550101");until(()->hasText(scrolled,"Harbor Clinic"));
        clickOwn(scrolled,v->v instanceof android.widget.Switch&&v.getContentDescription()!=null&&v.getContentDescription().toString().contains("+12025550101"));
        until(()->repository.current().account(1).choice()==Choice.DEFAULT);
        check(repository.current().account(2).choice()==Choice.ALLOW,"filtered toggle changes only the displayed exact number");
        check(ownsView(scrolled,v->v instanceof EditText e&&e.getText().toString().equals("+12025550101")),"filtered toggle retains the current query");
        clickOwn(scrolled,v->v.getContentDescription()!=null&&v.getContentDescription().toString().contains("+12025550101")&&v.getContentDescription().toString().endsWith("Show details"));
        String beforeReset=repository.dataIdentity();committed(repository::reset);
        until(()->ownsView(scrolled,v->v instanceof EditText e&&e.getText().toString().isEmpty()));
        check(!beforeReset.equals(repository.dataIdentity()),"reset replaces persistent presentation identity");
        Activity cleared=recreate(scrolled);until(()->hasRow(cleared,-14));scrollTo(cleared,-14,0);until(()->hasText(cleared,"No businesses found yet"));
        check(ownsView(cleared,v->v instanceof EditText e&&e.getText().toString().isEmpty())&&!ownsView(cleared,v->v.getContentDescription()!=null&&v.getContentDescription().toString().endsWith("Collapse details")),"reset cannot restore prior query or expanded number");
        check(repository.current().accounts().isEmpty()&&repository.disarmed(),"restoring UI after reset cannot restore data or authority");
    }
    private void performance()throws Exception{
        committed(repository::reset);insertSyntheticAccounts(0,10_000);committed(repository::reload);
        check(repository.current().accounts().size()==10_000,"ten thousand records loaded");
        long[] samples=new long[20],workerCpu=new long[20],workerWall=new long[20],workerQueue=new long[20];
        java.util.concurrent.ExecutorService executor=writer();
        for(int i=0;i<samples.length;i++){
            CountDownLatch done=new CountDownLatch(1);int index=i;long start=android.os.SystemClock.elapsedRealtimeNanos();
            long[] began=new long[2];CountDownLatch measured=new CountDownLatch(1);
            executor.execute(()->{began[0]=android.os.SystemClock.elapsedRealtimeNanos();began[1]=android.os.Debug.threadCpuTimeNanos();workerQueue[index]=began[0]-start;});
            repository.search("account "+(9000+i),rows->{check(rows.size()==1,"large-database query identity");samples[index]=android.os.SystemClock.elapsedRealtimeNanos()-start;done.countDown();});
            executor.execute(()->{workerWall[index]=android.os.SystemClock.elapsedRealtimeNanos()-began[0];workerCpu[index]=android.os.Debug.threadCpuTimeNanos()-began[1];measured.countDown();});
            check(done.await(10,TimeUnit.SECONDS),"bounded search callback");check(measured.await(10,TimeUnit.SECONDS),"bounded worker timing probe");
        }
        java.util.Arrays.sort(samples);java.util.Arrays.sort(workerCpu);java.util.Arrays.sort(workerWall);java.util.Arrays.sort(workerQueue);long p95=samples[18]/1_000_000;
        java.io.File database=getTargetContext().getDatabasePath("gate.db");long bytes=database.length()+new java.io.File(database.getPath()+"-wal").length();
        metrics="METRIC records=10000 query_p95_ms="+p95+" database_and_wal_bytes="+bytes+"\n";
        metrics+="METRIC writer_cpu_p95_ms="+workerCpu[18]/1_000_000+" writer_wall_p95_ms="+workerWall[18]/1_000_000+" writer_queue_p95_ms="+workerQueue[18]/1_000_000+"\n";
        check(p95<=100,"declared emulator query target");check(bytes<20L*1024*1024,"ten-thousand-record storage target");
        insertSyntheticAccounts(10_000,40_000);long loadStarted=android.os.SystemClock.elapsedRealtime();
        CountDownLatch loaded=new CountDownLatch(1);runOnMainSync(()->repository.reload(loaded::countDown));
        check(loaded.await(10,TimeUnit.SECONDS),"fifty-thousand-record snapshot callback within ten seconds");
        metrics+="METRIC records=50000 snapshot_load_ms="+(android.os.SystemClock.elapsedRealtime()-loadStarted)+"\n";
        check(repository.current().accounts().size()==50_000,"account ceiling fixture");
        committed(cb->repository.enableNumber("+12025550197","Capacity protection",cb));
        check(repository.current().accounts().size()==50_000,"new choice evicts only optional cache at capacity");
        check(repository.current().accounts().stream().anyMatch(a->a.phone().equals("+12025550197")&&a.choice()==Choice.ALLOW),"new explicit choice preserved at capacity");
        try(GateDbHelper helper=new GateDbHelper(getTargetContext())){helper.getWritableDatabase().execSQL("UPDATE account SET choice='ALLOW'");}
        committed(repository::reload);check(repository.current().accounts().stream().allMatch(a->a.choice()==Choice.ALLOW),"all-protected capacity fixture loaded");
        java.util.concurrent.atomic.AtomicBoolean saved=new java.util.concurrent.atomic.AtomicBoolean();
        runOnMainSync(()->repository.enableNumber("+12025550198","No optional record available",()->saved.set(true)));
        until(()->!repository.current().error().isEmpty());writerBarrier();
        check(!saved.get()&&repository.current().error().contains("storage is full"),"capacity rejection reports failure without a false save acknowledgement");
        check(repository.disarmed()&&repository.current().accounts().size()==50_000,"all-protected capacity failure remains bounded and disarmed");
        check(repository.current().accounts().stream().allMatch(a->a.choice()==Choice.ALLOW)&&repository.current().accounts().stream().noneMatch(a->a.phone().equals("+12025550198")),"capacity failure neither prunes protected choices nor invents a saved new choice");
        Account existing=repository.current().accounts().get(0);committed(cb->repository.choose(existing.id(),Choice.ALLOW,cb));
        check(repository.current().account(existing.id()).revision()==existing.revision()+1,"existing choice remains editable at the capacity ceiling");
        check(repository.disarmed(),"a later successful edit cannot silently resume after storage failure");
    }
    private void insertSyntheticAccounts(int offset,int count){
        try(GateDbHelper helper=new GateDbHelper(getTargetContext())){
            SQLiteDatabase db=helper.getWritableDatabase();db.beginTransaction();
            try(var insert=db.compileStatement("INSERT INTO account(namespace_id,phone,name,search_key,first_seen,last_seen) VALUES(1,?,?,?,?,?)")){
                long now=System.currentTimeMillis();
                for(int i=offset;i<offset+count;i++){
                    String name="Account "+i;insert.bindString(1,"+1999"+String.format(java.util.Locale.ROOT,"%07d",i));insert.bindString(2,name);insert.bindString(3,name.toLowerCase(java.util.Locale.ROOT));insert.bindLong(4,now);insert.bindLong(5,now);insert.executeInsert();insert.clearBindings();
                }
                db.setTransactionSuccessful();
            }finally{db.endTransaction();}
        }
    }
    private java.util.concurrent.ExecutorService writer()throws Exception{
        java.lang.reflect.Field field=GateRepository.class.getDeclaredField("writer");field.setAccessible(true);return (java.util.concurrent.ExecutorService)field.get(repository);
    }
    private void writerBarrier()throws Exception{
        CountDownLatch done=new CountDownLatch(1);writer().execute(done::countDown);
        check(done.await(10,TimeUnit.SECONDS),"writer drained");waitForIdleSync();
    }
    private void foundationRegressions()throws Exception{
        committed(repository::reset);
        String digest=io.github.appunnim.businessgate.automation.AdapterRegistry.sha256("owned synthetic installation".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        Binding a=new Binding(repository.installation(),digest,"synthetic-profile","+12025550001","synthetic-a");
        Binding b=new Binding(repository.installation(),digest,"synthetic-profile","+12025550002","synthetic-a");
        committed(cb->repository.bindReceiver(a,cb));committed(cb->repository.updateSetup("REVIEW",true,cb));
        committed(cb->repository.enableNumber("+12025550101","Synthetic namespace choice",cb));long namespaceA=repository.current().namespace();
        committed(cb->repository.bindReceiver(b,cb));check(repository.current().accounts().isEmpty(),"receiving namespaces isolate the same sender");
        committed(cb->repository.enableNumber("+12025550101","Independent choice",cb));long id=repository.current().accounts().get(0).id();
        committed(cb->repository.choose(id,Choice.DENY_MANUAL,cb));
        committed(cb->repository.bindReceiver(a,cb));check(repository.current().namespace()==namespaceA,"existing namespace restored");
        check(repository.current().accounts().get(0).choice()==Choice.ALLOW,"other receiver cannot change ALLOW");
        check(repository.current().accounts().get(0).nonce().isEmpty(),"namespace switch does not revive unblock grant");
        long sender=repository.current().accounts().get(0).id();
        committed(cb->repository.choose(sender,Choice.DEFAULT,cb));
        Evidence observed=new Evidence(namespaceA,"+12025550101",Kind.BUSINESS_CONFIRMED,BlockState.UNBLOCKED,android.os.SystemClock.elapsedRealtime(),5,2,a.receiver(),a.adapter(),true,true);
        runOnMainSync(()->repository.observe(observed,"Synthetic observation"));writerBarrier();
        check(repository.current().account(sender).jobState()==JobState.PENDING,"fresh confirmed profile queues block");
        try(GateDbHelper helper=new GateDbHelper(getTargetContext())){helper.getWritableDatabase().execSQL("UPDATE action_job SET state='DONE' WHERE account_id=?",new Object[]{sender});}
        committed(repository::reload);
        Evidence again=new Evidence(namespaceA,"+12025550101",Kind.BUSINESS_CONFIRMED,BlockState.UNBLOCKED,android.os.SystemClock.elapsedRealtime(),5,2,a.receiver(),a.adapter(),true,true);
        runOnMainSync(()->repository.observe(again,"Synthetic observation"));writerBarrier();
        check(repository.current().account(sender).jobState()==JobState.PENDING,"externally unblocked state requeues settled job");
        Snapshot pending=repository.current();Account pendingAccount=pending.account(sender);
        var stalePlan=new io.github.appunnim.businessgate.automation.AutomationController.Plan(sender,pending.globalRevision(),pendingAccount.revision(),5,Action.BLOCK,
            io.github.appunnim.businessgate.automation.AutomationController.Control.CONFIRM_BLOCK,pendingAccount.phone(),a.receiver(),2,namespaceA,a.adapter(),pendingAccount.nonce(),false);
        committed(cb->repository.choose(sender,Choice.ALLOW,cb));String newerGrant=repository.current().account(sender).nonce();
        CountDownLatch verified=new CountDownLatch(1);
        Evidence blocked=new Evidence(namespaceA,pendingAccount.phone(),Kind.BUSINESS_CONFIRMED,BlockState.BLOCKED,android.os.SystemClock.elapsedRealtime(),5,2,a.receiver(),a.adapter(),true,true);
        repository.verified(stalePlan,blocked,result->{check(result==io.github.appunnim.businessgate.automation.AutomationController.CommitResult.STALE,"stale verification is acknowledged as stale");verified.countDown();});
        check(verified.await(10,TimeUnit.SECONDS),"verification acknowledgement");
        check(newerGrant.equals(repository.current().account(sender).nonce())&&repository.current().account(sender).jobAction()==Action.UNBLOCK,"old completion cannot consume newer ALLOW command");
        java.lang.reflect.Field field=GateRepository.class.getDeclaredField("writer");field.setAccessible(true);
        java.util.concurrent.ExecutorService writer=(java.util.concurrent.ExecutorService)field.get(repository);
        CountDownLatch held=new CountDownLatch(1),release=new CountDownLatch(1);
        writer.execute(()->{held.countDown();try{release.await(10,TimeUnit.SECONDS);}catch(InterruptedException interrupted){Thread.currentThread().interrupt();}});
        check(held.await(10,TimeUnit.SECONDS),"writer held for race");
        Snapshot before=repository.current();Readiness ready=new Readiness(before.namespace(),before.globalRevision(),repository.epoch(),android.os.SystemClock.elapsedRealtime(),before.binding());
        runOnMainSync(()->{repository.arm(ready,()->{});repository.emergencyStop();});release.countDown();writerBarrier();check(repository.disarmed(),"late arm cannot undo Stop");
        CountDownLatch optionHeld=new CountDownLatch(1),optionRelease=new CountDownLatch(1);
        writer.execute(()->{optionHeld.countDown();try{optionRelease.await(10,TimeUnit.SECONDS);}catch(InterruptedException interrupted){Thread.currentThread().interrupt();}});
        check(optionHeld.await(10,TimeUnit.SECONDS),"writer held before option revocation");
        runOnMainSync(()->{repository.setting("sales_hints",true);repository.setting("sales_hints",false);});
        check(!repository.optionEnabled("sales_hints"),"queued disable immediately vetoes optional processing");
        optionRelease.countDown();writerBarrier();check(!repository.optionEnabled("sales_hints"),"old enable acknowledgement cannot undo newer disable");
        CountDownLatch consentHeld=new CountDownLatch(1),consentRelease=new CountDownLatch(1),withdrawn=new CountDownLatch(1);
        java.util.concurrent.atomic.AtomicBoolean switchCallback=new java.util.concurrent.atomic.AtomicBoolean();
        writer.execute(()->{consentHeld.countDown();try{consentRelease.await(10,TimeUnit.SECONDS);}catch(InterruptedException interrupted){Thread.currentThread().interrupt();}});
        check(consentHeld.await(10,TimeUnit.SECONDS),"writer held before receiver switch and withdrawal");
        runOnMainSync(()->{repository.bindReceiver(b,()->switchCallback.set(true));repository.updateSetup("WELCOME",false,withdrawn::countDown);});
        check(!repository.consented(),"consent withdrawal immediately vetoes processing");consentRelease.countDown();
        check(withdrawn.await(10,TimeUnit.SECONDS),"withdrawal commits after queued receiver switch");writerBarrier();
        check(!switchCallback.get(),"old receiver callback cannot reopen consent after withdrawal");
        check(!repository.consented()&&!repository.current().consent(),"withdrawal persists in the newly active receiver");
        committed(cb->repository.bindReceiver(a,cb));check(!repository.consented(),"returning to an old receiver does not revive withdrawn consent");
        CountDownLatch heldReset=new CountDownLatch(1),releaseReset=new CountDownLatch(1),resetDone=new CountDownLatch(1);
        writer.execute(()->{heldReset.countDown();try{releaseReset.await(10,TimeUnit.SECONDS);}catch(InterruptedException interrupted){Thread.currentThread().interrupt();}});
        check(heldReset.await(10,TimeUnit.SECONDS),"writer held before reset");
        runOnMainSync(()->{repository.enableNumber("+12025550109","Superseded",()->{});repository.attention(java.time.LocalDate.now(java.time.ZoneOffset.UTC).toString(),new long[]{400,0,400});repository.reset(resetDone::countDown);});releaseReset.countDown();
        check(resetDone.await(10,TimeUnit.SECONDS),"reset commits");writerBarrier();check(repository.current().accounts().isEmpty(),"queued old choice cannot repopulate reset");
        check(effortTotal()==0,"queued pre-reset metrics cannot repopulate cleared totals");
        android.content.Context isolated=new android.content.ContextWrapper(getTargetContext()){
            @Override public java.io.File getDatabasePath(String name){return super.getDatabasePath("migration-test.db");}
            @Override public SQLiteDatabase openOrCreateDatabase(String name,int mode,SQLiteDatabase.CursorFactory factory){return SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name),factory);}
            @Override public SQLiteDatabase openOrCreateDatabase(String name,int mode,SQLiteDatabase.CursorFactory factory,android.database.DatabaseErrorHandler handler){return SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name).getPath(),factory,handler);}
        };
        java.io.File path=isolated.getDatabasePath("gate.db");SQLiteDatabase.deleteDatabase(path);
        try(SQLiteDatabase db=SQLiteDatabase.openOrCreateDatabase(path,null);var input=getContext().getAssets().open("schema-v1.sql")){
            for(String sql:new String(io.github.appunnim.businessgate.support.Bytes.read(input),java.nio.charset.StandardCharsets.UTF_8).split(";"))if(!sql.trim().isEmpty())db.execSQL(sql);
            db.execSQL("INSERT INTO namespace(id,installation,enabled,paused,receiver_binding,qualification_id) VALUES(1,'synthetic-installation',1,0,'+12025550001','old-binding')");
            db.execSQL("INSERT INTO account(id,namespace_id,phone,choice,first_seen,last_seen) VALUES(1,1,'+12025550101','ALLOW',0,0)");
            db.execSQL("INSERT INTO action_job(account_id,action,state,global_revision,account_revision,nonce,created_at,updated_at) VALUES(1,'UNBLOCK','PENDING',0,0,'expired-grant',0,0)");
        }
        try(GateDbHelper helper=new GateDbHelper(isolated)){
            SQLiteDatabase db=helper.getWritableDatabase();check(db.getVersion()==2,"real helper upgrades actual shipped schema");
            try(var cursor=db.rawQuery("SELECT a.choice,n.active,n.paused,n.receiver_binding,j.state,j.nonce FROM account a JOIN namespace n ON a.namespace_id=n.id JOIN action_job j ON j.account_id=a.id",null)){
                check(cursor.moveToFirst(),"migration preserves account and job");check(cursor.getString(0).equals("ALLOW"),"migration preserves durable choice");
                check(cursor.getInt(1)==1&&cursor.getInt(2)==1&&cursor.getString(3).isEmpty(),"migration invalidates unverified binding");
                check(cursor.getString(4).equals("CANCELED")&&cursor.isNull(5),"migration revokes old unblock authority");
            }
            db.setVersion(3);
        }
        try(GateDbHelper helper=new GateDbHelper(isolated)){
            boolean rejected=false;try{helper.getWritableDatabase();}catch(IllegalStateException unsupported){rejected=true;}
            check(rejected,"unsupported downgrade fails without resetting choices");
        }
        try(SQLiteDatabase db=SQLiteDatabase.openDatabase(path.getPath(),null,SQLiteDatabase.OPEN_READONLY)){
            check(db.getVersion()==3,"rejected downgrade preserves database version");
            try(var cursor=db.rawQuery("SELECT choice FROM account WHERE id=1",null)){check(cursor.moveToFirst()&&cursor.getString(0).equals("ALLOW"),"rejected downgrade preserves choice");}
        }finally{SQLiteDatabase.deleteDatabase(path);}
        byte[] damaged="Synthetic damaged database fixture; preserve for recovery.".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        java.nio.file.Files.write(path.toPath(),damaged);
        try(GateDbHelper helper=new GateDbHelper(isolated)){
            boolean rejected=false;try{helper.getWritableDatabase();}catch(android.database.sqlite.SQLiteException corrupt){rejected=true;}
            check(rejected,"corrupt database fails closed");
            check(path.exists()&&java.util.Arrays.equals(damaged,java.nio.file.Files.readAllBytes(path.toPath())),"corruption handler does not erase the original database");
        }finally{SQLiteDatabase.deleteDatabase(path);}
        migrationQuota(isolated,path);
    }
    private void migrationQuota(android.content.Context isolated,java.io.File path)throws Exception{
        try(SQLiteDatabase db=SQLiteDatabase.openOrCreateDatabase(path,null);var input=getContext().getAssets().open("schema-v1.sql")){
            for(String sql:new String(io.github.appunnim.businessgate.support.Bytes.read(input),java.nio.charset.StandardCharsets.UTF_8).split(";"))if(!sql.trim().isEmpty())db.execSQL(sql);
            db.execSQL("INSERT INTO namespace(id,installation) VALUES(1,'quota-fixture')");
            db.execSQL("INSERT INTO account(id,namespace_id,phone,choice,first_seen,last_seen) VALUES(1,1,'+12025550101','ALLOW',0,0)");
            db.enableWriteAheadLogging();
            boolean full=false;db.beginTransaction();
            try(GateDbHelper helper=new GateDbHelper(isolated)){
                // WAL uses a connection pool. Bind the quota to this transaction's writer.
                long pages=android.database.DatabaseUtils.longForQuery(db,"PRAGMA page_count",null);
                check(db.setMaximumSize(pages*db.getPageSize())==pages*db.getPageSize(),"writer page quota applied");
                helper.onUpgrade(db,1,2);db.setTransactionSuccessful();
            }catch(android.database.sqlite.SQLiteFullException expected){full=true;}
            finally{db.endTransaction();}
            check(full,"real Android migration reaches its SQLite page quota");
            check(db.getVersion()==1,"quota failure preserves the shipped schema version");
            try(var cursor=db.rawQuery("SELECT choice FROM account WHERE id=1",null)){check(cursor.moveToFirst()&&cursor.getString(0).equals("ALLOW"),"quota failure preserves durable ALLOW");}
            check(android.database.DatabaseUtils.stringForQuery(db,"PRAGMA integrity_check",null).equals("ok"),"quota rollback retains database integrity");
            db.beginTransaction();
            try{db.setMaximumSize(10L*1024*1024);db.setTransactionSuccessful();}finally{db.endTransaction();}
        }
        try(GateDbHelper helper=new GateDbHelper(isolated)){
            SQLiteDatabase recovered=helper.getWritableDatabase();check(recovered.getVersion()==2,"migration retries after storage capacity returns");
            try(var cursor=recovered.rawQuery("SELECT a.choice,n.paused FROM account a JOIN namespace n ON n.id=a.namespace_id WHERE a.id=1",null)){check(cursor.moveToFirst()&&cursor.getString(0).equals("ALLOW")&&cursor.getInt(1)==1,"recovered migration keeps choice and inactive startup");}
        }finally{SQLiteDatabase.deleteDatabase(path);}
    }
    private Activity launch(){return startActivitySync(new Intent(getTargetContext(),io.github.appunnim.businessgate.ui.MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}
    private long effortTotal(){
        try(GateDbHelper helper=new GateDbHelper(getTargetContext());var cursor=helper.getReadableDatabase().rawQuery("SELECT COALESCE(sum(union_ms),0) FROM attention_daily",null)){cursor.moveToFirst();return cursor.getLong(0);}
    }
    private Readiness readiness(){Snapshot s=repository.current();return new Readiness(s.namespace(),s.globalRevision(),repository.epoch(),android.os.SystemClock.elapsedRealtime(),s.binding());}
    private AutomationController.Plan plan(long id,long generation,AutomationController.Control control){
        Snapshot s=repository.current();Account a=s.account(id);
        return new AutomationController.Plan(id,s.globalRevision(),a.revision(),generation,a.jobAction(),control,a.phone(),s.binding().receiver(),2,s.namespace(),s.binding().adapter(),a.nonce(),false);
    }
    private void journal(AutomationController.Plan plan,boolean expected)throws Exception{
        CountDownLatch done=new CountDownLatch(1);java.util.concurrent.atomic.AtomicBoolean saved=new java.util.concurrent.atomic.AtomicBoolean();
        runOnMainSync(()->repository.journal(plan,()->{saved.set(true);done.countDown();},done::countDown));
        check(done.await(10,TimeUnit.SECONDS),"journal acknowledgement");check(saved.get()==expected,"journal eligibility matches current durable authority");
    }
    private GateRepository.RetryResult retry(Account expected)throws Exception{
        CountDownLatch done=new CountDownLatch(1);java.util.concurrent.atomic.AtomicReference<GateRepository.RetryResult> result=new java.util.concurrent.atomic.AtomicReference<>();
        runOnMainSync(()->repository.retryCheck(expected,value->{result.set(value);done.countDown();}));check(done.await(10,TimeUnit.SECONDS),"explicit retry acknowledgement");return result.get();
    }
    private void recoveryRegressions()throws Exception{
        committed(repository::reset);
        Binding binding=new Binding(repository.installation(),"b".repeat(64),"synthetic-profile","+12025550001","synthetic-recovery");
        committed(cb->repository.bindReceiver(binding,cb));committed(cb->repository.updateSetup("REVIEW",true,cb));
        committed(cb->repository.enableNumber("+12025550103","Recovery fixture",cb));long id=repository.current().accounts().get(0).id();
        committed(cb->repository.choose(id,Choice.DENY_MANUAL,cb));
        for(int attempt=1;attempt<=3;attempt++){
            if(attempt>1){
                try(GateDbHelper helper=new GateDbHelper(getTargetContext())){helper.getWritableDatabase().execSQL("UPDATE action_job SET updated_at=? WHERE account_id=?",new Object[]{System.currentTimeMillis()-300_001,id});}
                committed(repository::reload);
            }
            committed(cb->repository.arm(readiness(),cb));Readiness scope=readiness();
            journal(plan(id,20+attempt,AutomationController.Control.BLOCK_ENTRY),true);
            check(repository.current().account(id).attempts()==attempt,"entry reserves one durable attempt");
            AutomationController.Plan confirm=plan(id,20+attempt,AutomationController.Control.CONFIRM_BLOCK);journal(confirm,true);
            check(repository.current().account(id).attempts()==attempt,"confirmation does not count a second attempt");
            runOnMainSync(()->{repository.emergencyStop();repository.interrupted(confirm,AutomationController.StopReason.TRANSITION_TIMEOUT,true,scope);});writerBarrier();
            Account interrupted=repository.current().account(id);
            check(interrupted.jobState()==(attempt==3?JobState.FAILED:JobState.REINSPECT),"bounded failure becomes reinspection or terminal failure");
            check(interrupted.jobReason().equals("RESULT_UNVERIFIED"),"uncertain dispatch has a durable truthful reason");
            committed(cb->repository.arm(readiness(),cb));journal(plan(id,50+attempt,AutomationController.Control.BLOCK_ENTRY),false);
            check(repository.current().account(id).attempts()==attempt,"backoff or exhaustion prevents additional dispatch");
        }
        Account failed=repository.current().account(id);
        check(retry(failed)==GateRepository.RetryResult.QUEUED,"explicit recovery reopens a failed job for inspection");
        check(repository.current().account(id).attempts()==0&&repository.current().account(id).jobState()==JobState.REINSPECT,"explicit recovery resets only attempt budget");
        check(repository.current().account(id).choice()==Choice.DENY_MANUAL&&repository.current().account(id).nonce().isEmpty(),"retry does not invent a different choice or unblock grant");
        check(retry(failed)==GateRepository.RetryResult.STALE,"stale retry cannot replace the newer revision");
        committed(cb->repository.choose(id,Choice.ALLOW,cb));Account allowed=repository.current().account(id);String grant=allowed.nonce();long grantedAt=allowed.grantCreatedAt();
        try(GateDbHelper helper=new GateDbHelper(getTargetContext())){helper.getWritableDatabase().execSQL("UPDATE action_job SET state='FAILED',attempts=3 WHERE account_id=?",new Object[]{id});}
        committed(repository::reload);check(retry(repository.current().account(id))==GateRepository.RetryResult.QUEUED,"current explicit unblock authority can be retried");
        check(repository.current().account(id).nonce().equals(grant)&&repository.current().account(id).grantCreatedAt()==grantedAt,"Retry preserves original nonce and expiry");
        runOnMainSync(repository::pause);writerBarrier();committed(cb->repository.choose(id,Choice.ALLOW,cb));
        committed(cb->repository.arm(readiness(),false,cb));
        check(repository.current().paused(),"explicit unblock session does not silently enable business blocking");
        journal(plan(id,90,AutomationController.Control.UNBLOCK_ENTRY),true);
        AutomationController.Plan old=plan(id,90,AutomationController.Control.CONFIRM_UNBLOCK);Readiness scope=readiness();
        committed(cb->repository.choose(id,Choice.ALLOW,cb));String newer=repository.current().account(id).nonce();
        runOnMainSync(()->repository.interrupted(old,AutomationController.StopReason.USER_STOP,true,scope));writerBarrier();
        check(repository.current().account(id).nonce().equals(newer)&&repository.current().account(id).jobState()==JobState.PENDING,"old interruption cannot overwrite a replacement ALLOW command");
        try(GateDbHelper helper=new GateDbHelper(getTargetContext())){helper.getWritableDatabase().execSQL("UPDATE action_job SET state='FAILED',grant_created_at=? WHERE account_id=?",new Object[]{System.currentTimeMillis()-8L*86400000,id});}
        committed(repository::reload);check(retry(repository.current().account(id))==GateRepository.RetryResult.NO_AUTHORITY,"Retry cannot revive an expired unblock grant");
        Readiness broken=readiness();runOnMainSync(()->{repository.emergencyStop();repository.interrupted(null,AutomationController.StopReason.IDENTITY_CHANGED,false,broken);});writerBarrier();
        check(repository.current().circuitOpen(),"identity anomaly persists the compatibility circuit");
        committed(repository::reload);check(repository.current().circuitOpen(),"reload does not silently close circuit");
        check(result(repository::retryStorage)==GateRepository.StorageResult.RECOVERED,"storage check can complete independently of compatibility");
        check(repository.current().circuitOpen()&&repository.disarmed(),"successful storage check cannot clear compatibility circuit or arm");
        runOnMainSync(()->repository.arm(readiness(),()->{}));writerBarrier();check(repository.disarmed(),"Resume cannot clear a circuit implicitly");
        committed(cb->repository.compatibilityChecked(readiness(),cb));check(!repository.current().circuitOpen()&&repository.disarmed(),"explicit compatibility check clears circuit without activating actions");
        committed(cb->{repository.localChoices(cb);repository.attention(java.time.LocalDate.now(java.time.ZoneOffset.UTC).toString(),new long[]{100,0,100});});writerBarrier();
        check(effortTotal()==100,"receiver switch does not discard already observed global effort");
    }
    private void seed()throws Exception{
        committed(cb->repository.reset(cb));
        try(GateDbHelper helper=new GateDbHelper(getTargetContext())){
            SQLiteDatabase db=helper.getWritableDatabase();db.beginTransaction();
            try{
                db.execSQL("UPDATE namespace SET consent_version='screen-v1',setup='REVIEW'");
                String[] names={"Harbor Clinic","Parcel Desk","Bright Mart","Loan Offers","New sender","Maya"};
                for(int i=0;i<names.length;i++){
                    String kind=i<4?"BUSINESS_CONFIRMED":i==5?"REGULAR_PROFILE_OBSERVED":"UNKNOWN";
                    String choice=i<2?"ALLOW":"DEFAULT";String observed=i==2?"BLOCKED":i<2?"UNBLOCKED":"UNKNOWN";
                    db.execSQL("INSERT INTO account(id,namespace_id,phone,name,search_key,kind,choice,observed_state,ever_business,review,hint_bits,checked_at,first_seen,last_seen) VALUES(?,1,?,?,?,?,?,?,?,?,?,?,?,?)",
                        new Object[]{i+1,"+1202555010"+(i+1),names[i],names[i].toLowerCase(java.util.Locale.ROOT),kind,choice,observed,i<4?1:0,i==4?"POSSIBLE_COMMERCIAL":"NONE",i==4?14:0,i==4?0:System.currentTimeMillis(),System.currentTimeMillis(),System.currentTimeMillis()});
                }
                db.execSQL("INSERT INTO action_job(account_id,action,state,global_revision,account_revision,created_at,updated_at) SELECT 4,'BLOCK','PENDING',global_revision,0,0,0 FROM namespace WHERE id=1");
                db.setTransactionSuccessful();
            }finally{db.endTransaction();}
        }
        committed(cb->repository.reload(cb));
    }
    private boolean hasText(Activity activity,String text){boolean[] found={false};runOnMainSync(()->found[0]=hasText(activity.getWindow().getDecorView(),text));return found[0];}
    private boolean hasText(View v,String text){if(v instanceof TextView t&&t.getText().toString().equals(text))return true;if(v instanceof ViewGroup group)for(int i=0;i<group.getChildCount();i++)if(hasText(group.getChildAt(i),text))return true;return false;}
    private <T extends View>T find(View v,Class<T> type){if(type.isInstance(v))return type.cast(v);if(v instanceof ViewGroup group)for(int i=0;i<group.getChildCount();i++){T found=find(group.getChildAt(i),type);if(found!=null)return found;}return null;}
}
