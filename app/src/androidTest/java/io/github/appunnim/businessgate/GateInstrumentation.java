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
import io.github.appunnim.businessgate.automation.AutomationController;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

/** Test APK only. Never included in either application variant. Uses synthetic identities. */
public final class GateInstrumentation extends Instrumentation {
    private Bundle arguments;
    private int assertions;
    private String metrics="";
    private GateRepository repository;
    @Override public void onCreate(Bundle args){arguments=args==null?new Bundle():args;super.onCreate(args);start();}
    private void check(boolean value,String name){assertions++;if(!value)throw new AssertionError(name);}
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
                until(()->hasText(activity,"Harbor Clinic"));check(hasText(activity,"Paused · compatibility check needed"),"unsupported status visible");runOnMainSync(()->find(activity.getWindow().getDecorView(),android.widget.ListView.class).setSelection(6));until(()->hasText(activity,"Block pending"));check(hasText(activity,"Block pending"),"pending subtitle visible after scrolling");
                runOnMainSync(()->{EditText search=find(activity.getWindow().getDecorView(),EditText.class);search.setText("+12025550102");});
                until(()->hasText(activity,"Parcel Desk")&&!hasText(activity,"Harbor Clinic"));check(!hasText(activity,"Harbor Clinic"),"search excludes other exact number");
                runOnMainSync(()->find(activity.getWindow().getDecorView(),EditText.class).setText("No such local account"));until(()->hasText(activity,"No matching accounts"));check(hasText(activity,"Enable a number"),"empty search offers explicit number entry");
                runOnMainSync(()->find(activity.getWindow().getDecorView(),EditText.class).setText(""));until(()->hasText(activity,"Harbor Clinic"));
                check(repository.current().accounts().size()==6,"UI search does not mutate repository");
            }
            result.putString("stream",metrics+"PASS "+assertions+" Android persistence, permission, recovery and native UI assertions; mode="+mode+"\n");finish(Activity.RESULT_OK,result);
        }catch(Throwable error){result.putString("stream",metrics+"FAIL after "+assertions+" assertions: "+error.getClass().getSimpleName()+": "+error.getMessage()+"\n");finish(Activity.RESULT_CANCELED,result);}
    }
    private void performance()throws Exception{
        committed(repository::reset);insertSyntheticAccounts(0,10_000);committed(repository::reload);
        check(repository.current().accounts().size()==10_000,"ten thousand records loaded");
        long[] samples=new long[20];
        for(int i=0;i<samples.length;i++){
            CountDownLatch done=new CountDownLatch(1);int index=i;long start=android.os.SystemClock.elapsedRealtimeNanos();
            repository.search("account "+(9000+i),rows->{check(rows.size()==1,"large-database query identity");samples[index]=android.os.SystemClock.elapsedRealtimeNanos()-start;done.countDown();});
            check(done.await(10,TimeUnit.SECONDS),"bounded search callback");
        }
        java.util.Arrays.sort(samples);long p95=samples[18]/1_000_000;
        java.io.File database=getTargetContext().getDatabasePath("gate.db");long bytes=database.length()+new java.io.File(database.getPath()+"-wal").length();
        metrics="METRIC records=10000 query_p95_ms="+p95+" database_and_wal_bytes="+bytes+"\n";
        check(p95<=100,"declared emulator query target");check(bytes<20L*1024*1024,"ten-thousand-record storage target");
        insertSyntheticAccounts(10_000,40_000);long loadStarted=android.os.SystemClock.elapsedRealtime();
        CountDownLatch loaded=new CountDownLatch(1);runOnMainSync(()->repository.reload(loaded::countDown));
        check(loaded.await(10,TimeUnit.SECONDS),"fifty-thousand-record snapshot callback within ten seconds");
        metrics+="METRIC records=50000 snapshot_load_ms="+(android.os.SystemClock.elapsedRealtime()-loadStarted)+"\n";
        check(repository.current().accounts().size()==50_000,"account ceiling fixture");
        committed(cb->repository.enableNumber("+12025550197","Capacity protection",cb));
        check(repository.current().accounts().size()==50_000,"new choice evicts only optional cache at capacity");
        check(repository.current().accounts().stream().anyMatch(a->a.phone().equals("+12025550197")&&a.choice()==Choice.ALLOW),"new explicit choice preserved at capacity");
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
    private void writerBarrier()throws Exception{
        java.lang.reflect.Field field=GateRepository.class.getDeclaredField("writer");field.setAccessible(true);
        CountDownLatch done=new CountDownLatch(1);((java.util.concurrent.ExecutorService)field.get(repository)).execute(done::countDown);
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
