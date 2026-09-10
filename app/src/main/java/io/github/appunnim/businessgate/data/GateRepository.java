package io.github.appunnim.businessgate.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import io.github.appunnim.businessgate.policy.AuthorityEpoch;
import io.github.appunnim.businessgate.policy.AccountSearch;
import io.github.appunnim.businessgate.policy.PendingChoices;
import io.github.appunnim.businessgate.policy.PendingChoices.Pending;
import io.github.appunnim.businessgate.policy.Identity;
import io.github.appunnim.businessgate.policy.Model.*;
import io.github.appunnim.businessgate.policy.RuleEngine;
import io.github.appunnim.businessgate.policy.RetryPolicy;
import io.github.appunnim.businessgate.automation.AutomationController.CommitResult;
import io.github.appunnim.businessgate.automation.AutomationController.Plan;
import io.github.appunnim.businessgate.automation.AutomationController.StopReason;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/** One serial writer; commands capture namespace and cancellation ownership before queuing. */
public final class GateRepository {
    public static final String CONSENT_VERSION = "screen-v1";
    private static final int ACCOUNT_LIMIT = 50_000;
    private final GateDbHelper helper;
    private final Context context;
    private final ExecutorService writer = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private record Published(Snapshot snapshot,AccountSearch search,String identity) {}
    private final AtomicReference<Published> published = new AtomicReference<>(new Published(Snapshot.empty(),new AccountSearch(List.of()),""));
    private final Set<Runnable> listeners = new CopyOnWriteArraySet<>();
    private final PendingChoices pendingChoices=new PendingChoices();
    private final java.util.concurrent.ConcurrentHashMap<String,Long> optionVetoes = new java.util.concurrent.ConcurrentHashMap<>();
    private volatile boolean consentVeto;
    private volatile long consentCommand;
    private final AtomicLong commandSequence = new AtomicLong(), dataEpoch = new AtomicLong(), metricsEpoch=new AtomicLong();
    private final AuthorityEpoch authority = new AuthorityEpoch();
    private volatile String failure = "", installation = "";
    private record Stamp(long namespace, long data) {}
    private static final class Stale extends RuntimeException {}
    private static final class Capacity extends RuntimeException {}
    public enum RetryResult { QUEUED, NO_AUTHORITY, STALE, SAVE_FAILED }
    public enum StorageResult { RECOVERED, UNSAVED_CHOICES, FAILED, STALE }
    public enum SaveResult { SAVED, FAILED, STALE, BUSY }
    public record ChoiceScope(long namespace,long epoch) {}
    public ChoiceScope choiceScope(){return new ChoiceScope(current().namespace(),dataEpoch.get());}
    public GateRepository(Context context) { this.context=context.getApplicationContext();helper = new GateDbHelper(context); initialize(context); }
    public Snapshot current() { return published.get().snapshot(); }
    public boolean disarmed() { return !authority.armed() || !failure.isEmpty() || pendingChoices.any(current().namespace()); }
    public boolean consented(){return current().consent()&&!consentVeto;}
    public boolean optionEnabled(String field){
        if(!consented()||optionVetoes.containsKey(field))return false;
        return switch(field){case "digest"->current().digest();case "discovery"->current().discovery();case "sales_hints"->current().salesHints();default->false;};
    }
    public long epoch() { return authority.current(); }
    public long metricsEpoch(){return metricsEpoch.get();}
    public String dataIdentity(){return published.get().identity();}
    public String installation() { return installation; }
    public boolean vetoed(long id) { Account account=current().account(id);return account!=null&&pendingChoices.get(account.namespace(),account.phone())!=null; }
    public List<Pending> pendingChoices(){return pendingChoices.list(current().namespace());}
    public Pending pendingChoice(String phone){return pendingChoices.get(current().namespace(),phone);}
    public void reload(Runnable success) { transaction(stamp(), db -> {}, success); }
    public void addListener(Runnable listener) { listeners.add(listener); }
    public void removeListener(Runnable listener) { listeners.remove(listener); }
    private void notifyChanged() { main.post(() -> listeners.forEach(Runnable::run)); }
    private Stamp stamp() { return new Stamp(current().namespace(), dataEpoch.get()); }
    private void require(SQLiteDatabase db, Stamp stamp) {
        if (stamp.data() != dataEpoch.get() || activeId(db) != stamp.namespace()) throw new Stale();
    }
    private static long activeId(SQLiteDatabase db) {
        try (Cursor c = db.rawQuery("SELECT id FROM namespace WHERE active=1", null)) {
            if (!c.moveToFirst()) throw new IllegalStateException("NAMESPACE_MISSING");
            return c.getLong(0);
        }
    }
    private void initialize(Context context) {
        writer.execute(() -> {
            try {
                SQLiteDatabase db=reconcileStorage(context,false,null);publish(db);
            } catch (Exception error) { fail(); }
        });
    }
    private SQLiteDatabase reconcileStorage(Context context,boolean retry,Stamp owner)throws java.io.IOException {
        File marker = new File(context.getNoBackupFilesDir(), "installation");
        if (!marker.exists()) Files.write(marker.toPath(), UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
        installation = new String(Files.readAllBytes(marker.toPath()), StandardCharsets.UTF_8);
        SQLiteDatabase db = helper.getWritableDatabase(); db.beginTransaction();
        try {
            boolean sameInstallation;
            try (Cursor c = db.rawQuery("SELECT installation FROM namespace WHERE active=1", null)) {
                sameInstallation = c.moveToFirst() && installation.equals(c.getString(0));
            }
            if (!sameInstallation) {
                clearPublished();
                db.execSQL("UPDATE namespace SET active=0,enabled=0,paused=1,global_revision=global_revision+1");
                db.execSQL("UPDATE action_job SET state='CANCELED',nonce=NULL");
                db.execSQL("INSERT INTO namespace(installation,active) VALUES(?,1)", new Object[]{installation});
                event(db, null, "RECOVERY", "INSTALLATION_CHANGED");
                db.delete("app_meta","key='ui_data_identity'",null);
            }
            db.execSQL("INSERT OR IGNORE INTO app_meta(key,value) VALUES('ui_data_identity',?)",new Object[]{UUID.randomUUID().toString()});
            db.execSQL("UPDATE namespace SET paused=1"+(retry?",global_revision=global_revision+1":""));
            db.execSQL("UPDATE action_job SET state='REINSPECT',reason=? WHERE state IN ('ACTION_INTENT','VERIFYING')",new Object[]{retry?"STORAGE_RETRY":"PROCESS_RESTART"});
            if(retry){
                try(Cursor integrity=db.rawQuery("PRAGMA quick_check",null)){if(!integrity.moveToFirst()||!"ok".equals(integrity.getString(0)))throw new IllegalStateException("STORAGE_INTEGRITY_FAILED");}
                try(Cursor foreign=db.rawQuery("PRAGMA foreign_key_check",null)){if(foreign.moveToFirst())throw new IllegalStateException("STORAGE_REFERENCES_FAILED");}
            }
            prune(db);
            if(owner!=null&&owner.data()!=dataEpoch.get())throw new Stale();
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
        return db;
    }
    /** Explicit storage-only recovery never retries a user choice, grants authority or clears a circuit. */
    public void retryStorage(Consumer<StorageResult> result){
        emergencyStop();Stamp stamp=stamp();
        writer.execute(()->{
            StorageResult outcome;
            try{
                if(stamp.data()!=dataEpoch.get()||current().namespace()!=stamp.namespace())throw new Stale();
                SQLiteDatabase db=reconcileStorage(context,true,stamp);
                if(stamp.data()!=dataEpoch.get())throw new Stale();
                failure="";publish(db);outcome=pendingChoices.any(current().namespace())?StorageResult.UNSAVED_CHOICES:StorageResult.RECOVERED;
            }catch(Stale stale){outcome=StorageResult.STALE;}catch(Exception error){fail();outcome=StorageResult.FAILED;}
            StorageResult finished=outcome;main.post(()->result.accept(stamp.data()!=dataEpoch.get()?StorageResult.STALE:!current().error().isEmpty()?StorageResult.FAILED:finished));
        });
    }
    private interface Write { void run(SQLiteDatabase db); }
    private void transaction(Stamp stamp, Write operation, Runnable success) {
        writer.execute(() -> {
            try {
                SQLiteDatabase db = helper.getWritableDatabase(); db.beginTransaction();
                try { require(db, stamp); operation.run(db); prune(db); db.setTransactionSuccessful(); }
                finally { db.endTransaction(); }
                publish(db);
                if (success != null) main.post(() -> { if (stamp.data() == dataEpoch.get() && current().namespace() == stamp.namespace()) success.run(); });
            } catch (Stale ignored) { /* Superseded command has no authority over its replacement. */ }
            catch (Capacity full) { fail("Local account storage is full. Existing choices are safe; no new blocks will run."); }
            catch (Exception error) { fail(); }
        });
    }
    private void fail() { fail("Could not save. No new blocks will run."); }
    private void fail(String message) {
        authority.revoke(); failure = message;
        published.updateAndGet(previous->{Snapshot s=previous.snapshot();return new Published(new Snapshot(s.namespace(),s.globalRevision(),s.loaded(),s.enabled(),true,s.consent(),s.salesHints(),s.discovery(),s.digest(),s.setup(),s.accounts(),failure,s.binding(),s.circuitOpen()),previous.search(),previous.identity());});
        notifyChanged();
    }
    /** A data-boundary command hides the old projection until storage establishes its current identity. */
    private void clearPublished(){
        published.set(new Published(new Snapshot(-1,0,false,false,true,false,false,false,false,"WELCOME",List.of(),""),new AccountSearch(List.of()),""));
        notifyChanged();
    }
    private void publish(SQLiteDatabase db) {
        long id = activeId(db);String dataIdentity;
        try(Cursor identity=db.rawQuery("SELECT value FROM app_meta WHERE key='ui_data_identity'",null)){
            if(!identity.moveToFirst()||identity.getString(0).isEmpty())throw new IllegalStateException("DATA_IDENTITY_MISSING");
            dataIdentity=identity.getString(0);
        }
        try (Cursor n = db.rawQuery("SELECT * FROM namespace WHERE id=?", new String[]{""+id})) {
            if (!n.moveToFirst()) throw new IllegalStateException("NAMESPACE_MISSING");
            Binding binding = new Binding(string(n,"installation"),string(n,"package_digest"),string(n,"profile_key"),string(n,"receiver_binding"),string(n,"qualification_id"));
            boolean circuit=false;
            try(Cursor breaker=db.rawQuery("SELECT value FROM app_meta WHERE key=?",new String[]{"circuit:"+id})){
                circuit=breaker.moveToFirst()&&!binding.adapter().isEmpty()&&binding.adapter().equals(breaker.getString(0));
            }
            List<Account> accounts=readAccounts(db,id,"",new String[0]);
            AccountSearch index=new AccountSearch(accounts);
            Snapshot state=new Snapshot(id,number(n,"global_revision"),true,number(n,"enabled")==1,number(n,"paused")==1,
                CONSENT_VERSION.equals(string(n,"consent_version")),number(n,"sales_hints")==1,number(n,"discovery")==1,number(n,"digest")==1,
                string(n,"setup"),accounts,failure,binding,circuit);
            published.set(new Published(state,index,dataIdentity));
        }
        notifyChanged();
    }
    private List<Account> readAccounts(SQLiteDatabase db, long namespace, String extra, String[] args) {
        List<Account> rows = new ArrayList<>(); String[] bound = new String[args.length+1]; bound[0]=""+namespace; System.arraycopy(args,0,bound,1,args.length);
        // Fixed projection avoids per-row column-name resolution and unused cursor payload at the account ceiling.
        String projection="a.id,a.phone,a.name,a.kind,a.choice,a.observed_state,a.gate_owned,a.revision,a.ever_business,a.review,a.hint_bits,a.dismissed_until,a.checked_at,a.last_seen,j.state,j.action,j.nonce,j.grant_created_at,j.attempts,j.updated_at,j.reason,a.search_key";
        try (Cursor c = db.rawQuery("SELECT "+projection+" FROM account a LEFT JOIN action_job j ON a.id=j.account_id WHERE a.namespace_id=? " + extra + " ORDER BY a.search_key,a.phone,a.id",bound)) {
            while(c.moveToNext()) rows.add(new Account(c.getLong(0),namespace,string(c,1),string(c,2),Kind.valueOf(string(c,3)),Choice.valueOf(string(c,4)),
                BlockState.valueOf(string(c,5)),c.getInt(6)==1,c.getLong(7),c.getInt(8)==1,Review.valueOf(string(c,9)),
                c.getInt(10),c.getLong(11),c.getLong(12),c.getLong(13),
                c.isNull(14)?JobState.NONE:JobState.valueOf(c.getString(14)),
                c.isNull(15)?Action.NONE:Action.valueOf(c.getString(15)),string(c,16),c.getLong(17),c.getInt(18),c.getLong(19),string(c,20),string(c,21)));
        }
        return rows;
    }
    public void search(String input, Consumer<List<Account>> result) {
        String query=Identity.query(input);Stamp stamp=stamp();
        writer.execute(()->{
            // Search the last committed projection. Writes replace it before notifying the UI.
            // This avoids repeated SQLite scans and remains available during storage failure.
            if(stamp.data()!=dataEpoch.get()||current().namespace()!=stamp.namespace())return;
            List<Account> rows=published.get().search().find(query);
            main.post(()->{if(stamp.data()==dataEpoch.get()&&current().namespace()==stamp.namespace())result.accept(rows);});
        });
    }
    public boolean enableNumber(String rawPhone,String label,Runnable success) {return enableNumber(choiceScope(),rawPhone,label,success);}
    public boolean enableNumber(ChoiceScope scope,String rawPhone,String label,Runnable success) {
        String phone=Identity.canonicalPhone(rawPhone),name=Identity.label(label);Account existing=current().accounts().stream().filter(a->a.phone().equals(phone)).findFirst().orElse(null);
        return submitChoice(scope,phone,name,Choice.ALLOW,existing==null?-1:existing.revision(),false,success,null);
    }
    private boolean submitChoice(ChoiceScope scope,String phone,String label,Choice choice,long baseRevision,boolean retry,Runnable success,Consumer<SaveResult> result){
        emergencyStop();
        if(!current().loaded()||scope.namespace()!=current().namespace()||scope.epoch()!=dataEpoch.get()){notifyChanged();if(result!=null)main.post(()->result.accept(SaveResult.STALE));return false;}
        Stamp stamp=new Stamp(scope.namespace(),scope.epoch());Pending command=new Pending(stamp.namespace(),phone,label,choice,baseRevision,commandSequence.incrementAndGet(),false);
        if(!pendingChoices.put(command)){notifyChanged();if(result!=null)main.post(()->result.accept(SaveResult.BUSY));return false;}
        notifyChanged();
        writer.execute(()->{
            SaveResult outcome;long savedRevision=-1;
            try{
                SQLiteDatabase db=helper.getWritableDatabase();db.beginTransaction();
                try{
                    require(db,stamp);if(!pendingChoices.current(command))throw new Stale();
                    long id=-1,revision=-1;
                    try(Cursor c=db.rawQuery("SELECT id,revision FROM account WHERE namespace_id=? AND phone=?",new String[]{""+command.namespace(),phone})){if(c.moveToFirst()){id=c.getLong(0);revision=c.getLong(1);}}
                    if(revision!=baseRevision)throw new Stale();
                    if(id<0){
                        if(choice!=Choice.ALLOW)throw new Stale();ensureCapacity(db);long now=System.currentTimeMillis();
                        db.execSQL("INSERT INTO account(namespace_id,phone,name,search_key,first_seen,last_seen) VALUES(?,?,?,?,?,?)",new Object[]{stamp.namespace(),phone,label,Identity.searchKey(label),now,now});
                        try(Cursor c=db.rawQuery("SELECT id FROM account WHERE namespace_id=? AND phone=?",new String[]{""+stamp.namespace(),phone})){if(!c.moveToFirst())throw new Stale();id=c.getLong(0);revision=0;}
                    }
                    if(retry)db.execSQL("UPDATE namespace SET paused=1,global_revision=global_revision+1 WHERE id=?",new Object[]{stamp.namespace()});
                    choose(db,stamp.namespace(),id,choice);prune(db);require(db,stamp);if(!pendingChoices.current(command))throw new Stale();savedRevision=revision+1;db.setTransactionSuccessful();
                }finally{db.endTransaction();}
                pendingChoices.committed(command);publish(db);outcome=SaveResult.SAVED;
            }catch(Stale stale){pendingChoices.failed(command);notifyChanged();outcome=SaveResult.STALE;}
            catch(Capacity full){pendingChoices.failed(command);fail("Local account storage is full. Existing choices are safe; no new blocks will run.");outcome=SaveResult.FAILED;}
            catch(Exception error){pendingChoices.failed(command);fail();outcome=SaveResult.FAILED;}
            SaveResult finished=outcome;long committedRevision=savedRevision;main.post(()->{
                Account saved=current().accounts().stream().filter(a->a.phone().equals(phone)).findFirst().orElse(null);
                boolean replaced=finished==SaveResult.SAVED&&(saved==null||saved.revision()!=committedRevision||saved.choice()!=choice||pendingChoices.get(stamp.namespace(),phone)!=null);
                SaveResult current=replaced||stamp.data()!=dataEpoch.get()||stamp.namespace()!=current().namespace()?SaveResult.STALE:finished;
                if(result!=null)result.accept(current);if(current==SaveResult.SAVED&&success!=null)success.run();
            });
        });return true;
    }
    public void retrySave(Pending expected,Consumer<SaveResult> result){
        if(expected==null||expected.namespace()!=current().namespace()||!pendingChoices.current(expected)){main.post(()->result.accept(SaveResult.STALE));return;}
        Pending latest=pendingChoices.get(expected.namespace(),expected.phone());if(latest==null||!latest.equals(expected)){main.post(()->result.accept(SaveResult.STALE));return;}if(!latest.failed()){main.post(()->result.accept(SaveResult.BUSY));return;}
        submitChoice(choiceScope(),latest.phone(),latest.label(),latest.choice(),latest.baseRevision(),true,null,result);
    }
    private static void ensureCapacity(SQLiteDatabase db) {
        try(Cursor c=db.rawQuery("SELECT count(*) FROM account",null)){
            c.moveToFirst();if(c.getLong(0)<ACCOUNT_LIMIT)return;
        }
        int removed=db.delete("account","id IN (SELECT id FROM account WHERE choice='DEFAULT' AND ever_business=0 AND review='NONE' AND id NOT IN (SELECT account_id FROM action_job) ORDER BY last_seen,id LIMIT 1)",null);
        if(removed!=1)throw new Capacity();
    }
    public boolean choose(long id,Choice choice,Runnable success) {return choose(current().account(id),choice,success);}
    public boolean choose(Account expected,Choice choice,Runnable success) {
        ChoiceScope scope=choiceScope();Account account=expected==null?null:current().account(expected.id());
        if(account==null||account.namespace()!=scope.namespace()||account.namespace()!=expected.namespace()||!account.phone().equals(expected.phone())||account.revision()!=expected.revision()){
            emergencyStop();notifyChanged();return false;
        }
        return submitChoice(scope,account.phone(),account.name(),choice,account.revision(),false,success,null);
    }
    private void choose(SQLiteDatabase db,long namespace,long id,Choice choice) {
        long now=System.currentTimeMillis();
        db.execSQL("UPDATE account SET choice=?,revision=revision+1,review='NONE',hint_bits=0,dismissed_until=0 WHERE id=? AND namespace_id=?",new Object[]{choice.name(),id,namespace});
        try(Cursor c=db.rawQuery("SELECT revision,kind FROM account WHERE id=? AND namespace_id=?",new String[]{""+id,""+namespace})) {
            if(!c.moveToFirst())throw new Stale(); db.delete("action_job","account_id=?",new String[]{""+id}); boolean allow=choice==Choice.ALLOW;
            if(allow||choice==Choice.DENY_MANUAL||c.getString(1).equals(Kind.BUSINESS_CONFIRMED.name()))
                db.execSQL("INSERT INTO action_job(account_id,action,state,global_revision,account_revision,nonce,grant_created_at,created_at,updated_at) SELECT ?,?,'PENDING',global_revision,?,?,?,?,? FROM namespace WHERE id=?",
                    new Object[]{id,allow?"UNBLOCK":"BLOCK",c.getLong(0),allow?UUID.randomUUID().toString():null,allow?now:0,now,now,namespace});
        }
        event(db,id,"POLICY","USER_CHOICE");
    }
    public void dismiss(long id) {
        Stamp stamp=stamp();transaction(stamp,db->{db.execSQL("UPDATE account SET dismissed_until=? WHERE id=? AND namespace_id=?",new Object[]{System.currentTimeMillis()+30L*86400000,id,stamp.namespace()});event(db,id,"POLICY","HINT_DISMISSED");},null);
    }
    public void updateSetup(String step,boolean consent,Runnable success) {
        if(!set("WELCOME","ACCESS","CAPABILITY","DISCOVERY","REVIEW","READY").contains(step))throw new IllegalArgumentException("INVALID_STEP");
        emergencyStop();long command=commandSequence.incrementAndGet();consentCommand=command;consentVeto=true;
        if(!consent){withdrawConsent(command,success);return;}
        Stamp stamp=stamp();
        transaction(stamp,db->{
            db.execSQL("UPDATE namespace SET setup=?,consent_version=?,consent_at=?,paused=1,global_revision=global_revision+1 WHERE id=?",new Object[]{step,CONSENT_VERSION,System.currentTimeMillis(),stamp.namespace()});
        },()->{if(consentCommand==command){consentVeto=false;notifyChanged();if(success!=null)success.run();}});
    }
    private void withdrawConsent(long command,Runnable success){
        writer.execute(()->{
            try{
                SQLiteDatabase db=helper.getWritableDatabase();db.beginTransaction();
                try{
                    // Screen consent withdrawal is global even if a receiver switch was queued first.
                    db.execSQL("UPDATE namespace SET consent_version='',consent_at=0,setup='WELCOME',paused=1,discovery=0,sales_hints=0,digest=0,global_revision=global_revision+1");
                    db.execSQL("UPDATE action_job SET state='CANCELED',nonce=NULL WHERE action='UNBLOCK'");
                    db.execSQL("UPDATE account SET hint_bits=0,review='NEW_SENDER' WHERE review='POSSIBLE_COMMERCIAL' AND choice='DEFAULT'");
                    db.setTransactionSuccessful();
                }finally{db.endTransaction();}
                publish(db);main.post(()->{if(consentCommand==command){consentVeto=true;notifyChanged();if(success!=null)success.run();}});
            }catch(Exception error){fail();}
        });
    }
    private static void clearHints(SQLiteDatabase db,long namespace) { db.execSQL("UPDATE account SET hint_bits=0,review='NEW_SENDER' WHERE namespace_id=? AND review='POSSIBLE_COMMERCIAL' AND choice='DEFAULT'",new Object[]{namespace}); }
    public void setting(String field,boolean value) {
        if(!set("sales_hints","discovery","digest").contains(field))throw new IllegalArgumentException("INVALID_SETTING");
        long command=commandSequence.incrementAndGet();optionVetoes.put(field,command);notifyChanged();Stamp stamp=stamp();
        transaction(stamp,db->{db.execSQL("UPDATE namespace SET "+field+"=? WHERE id=?",new Object[]{value?1:0,stamp.namespace()});if(field.equals("sales_hints")&&!value)clearHints(db,stamp.namespace());},()->{optionVetoes.remove(field,command);notifyChanged();});
    }
    private static void cancelGrants(SQLiteDatabase db,long namespace) {
        db.execSQL("UPDATE action_job SET state='CANCELED',nonce=NULL WHERE action='UNBLOCK' AND account_id IN (SELECT id FROM account WHERE namespace_id=?)",new Object[]{namespace});
    }
    public void pause() {
        emergencyStop();Stamp stamp=stamp();transaction(stamp,db->{
            db.execSQL("UPDATE namespace SET paused=1,global_revision=global_revision+1 WHERE id=?",new Object[]{stamp.namespace()});cancelGrants(db,stamp.namespace());
            db.execSQL("UPDATE action_job SET state='REINSPECT' WHERE state IN ('ACTION_INTENT','VERIFYING') AND account_id IN (SELECT id FROM account WHERE namespace_id=?)",new Object[]{stamp.namespace()});event(db,null,"STOPPED","USER_STOP");
        },null);
    }
    public boolean ready(Readiness r) {
        Snapshot s=current();return r!=null&&s.loaded()&&consented()&&failure.isEmpty()&&!pendingChoices.any(s.namespace())&&s.binding().bound()&&s.binding().equals(r.binding())
            &&s.namespace()==r.namespace()&&s.globalRevision()==r.globalRevision()&&authority.matches(r.epoch())
            &&SystemClock.elapsedRealtime()>=r.observedElapsed()&&SystemClock.elapsedRealtime()-r.observedElapsed()<=RuleEngine.EVIDENCE_TTL_MS;
    }
    public void arm(Readiness readiness,Runnable success) {
        arm(readiness,true,success);
    }
    public void arm(Readiness readiness,boolean activateRule,Runnable success) {
        if(!ready(readiness)||current().circuitOpen())return;Stamp stamp=stamp();long owner=readiness.epoch();
        transaction(stamp,db->{
            if(!ready(readiness)||current().circuitOpen())throw new Stale();
            db.execSQL("UPDATE namespace SET enabled=CASE WHEN ? THEN 1 ELSE enabled END,paused=CASE WHEN ? THEN 0 ELSE paused END,setup='READY',global_revision=global_revision+1 WHERE id=?",new Object[]{activateRule?1:0,activateRule?1:0,stamp.namespace()});
            long now=System.currentTimeMillis();
            db.execSQL("UPDATE action_job SET global_revision=(SELECT global_revision FROM namespace WHERE id=?) WHERE state NOT IN ('DONE','CANCELED','FAILED') AND (action='BLOCK' OR (nonce IS NOT NULL AND grant_created_at<=? AND grant_created_at>?)) AND account_id IN (SELECT id FROM account WHERE namespace_id=?)",new Object[]{stamp.namespace(),now,now-RuleEngine.GRANT_TTL_MS,stamp.namespace()});
        },()->{if(authority.arm(owner)&&consented()&&current().binding().equals(readiness.binding())){notifyChanged();if(success!=null)success.run();}else emergencyStop();});
    }
    public void emergencyStop() { authority.revoke();notifyChanged(); }
    /** An explicit Retry preserves preference and any existing grant; it never creates an unblock nonce. */
    public void retryCheck(Account expected,Consumer<RetryResult> callback){
        emergencyStop();Stamp stamp=stamp();
        writer.execute(()->{
            RetryResult result;
            try{
                SQLiteDatabase db=helper.getWritableDatabase();db.beginTransaction();
                try{
                    require(db,stamp);Account a=current().account(expected.id());
                    if(a==null||a.namespace()!=expected.namespace()||a.revision()!=expected.revision())throw new Stale();
                    long now=System.currentTimeMillis();
                    boolean authority=a.jobAction()==Action.BLOCK&&a.choice()!=Choice.ALLOW&&(a.kind()==Kind.BUSINESS_CONFIRMED||a.choice()==Choice.DENY_MANUAL)
                        ||a.jobAction()==Action.UNBLOCK&&a.choice()==Choice.ALLOW&&!a.nonce().isEmpty()&&now>=a.grantCreatedAt()&&now-a.grantCreatedAt()<RuleEngine.GRANT_TTL_MS;
                    if(!authority||a.jobState()==JobState.DONE||a.jobState()==JobState.CANCELED||a.jobState()==JobState.NONE)result=RetryResult.NO_AUTHORITY;
                    else{
                        db.execSQL("UPDATE account SET revision=revision+1 WHERE id=? AND namespace_id=?",new Object[]{a.id(),stamp.namespace()});
                        db.execSQL("UPDATE action_job SET state='REINSPECT',attempts=0,reason='USER_RETRY',generation=0,updated_at=?,account_revision=?,global_revision=(SELECT global_revision FROM namespace WHERE id=?) WHERE account_id=?",new Object[]{now,a.revision()+1,stamp.namespace(),a.id()});
                        event(db,a.id(),"POLICY","USER_CHOICE");result=RetryResult.QUEUED;
                    }
                    db.setTransactionSuccessful();
                }finally{db.endTransaction();}
                publish(db);
            }catch(Stale ignored){result=RetryResult.STALE;}catch(Exception error){fail();result=RetryResult.SAVE_FAILED;}
            RetryResult finished=result;main.post(()->callback.accept(stamp.data()==dataEpoch.get()&&stamp.namespace()==current().namespace()?finished:RetryResult.STALE));
        });
    }
    /** Record only the interrupted command; a newer choice or receiving namespace cannot be overwritten. */
    public void interrupted(Plan p,StopReason reason,boolean uncertain,Readiness scope){
        if(scope==null)return;Stamp stamp=stamp();
        transaction(stamp,db->{
            if(scope.namespace()!=stamp.namespace()||!scope.binding().equals(current().binding()))throw new Stale();
            if(reason==StopReason.IDENTITY_CHANGED||reason==StopReason.UNSUPPORTED_SCREEN){
                db.execSQL("INSERT OR REPLACE INTO app_meta(key,value) VALUES(?,?)",new Object[]{"circuit:"+stamp.namespace(),scope.binding().adapter()});
            }
            if(p!=null&&matchesPlan(db,p,false)){
                db.execSQL("UPDATE action_job SET state=CASE WHEN attempts>=? THEN 'FAILED' ELSE 'REINSPECT' END,reason=?,updated_at=? WHERE account_id=? AND generation=? AND state IN ('ACTION_INTENT','VERIFYING')",
                    new Object[]{RetryPolicy.MAX_ATTEMPTS,uncertain?"RESULT_UNVERIFIED":reason.name(),System.currentTimeMillis(),p.accountId(),p.generation()});
            }
            event(db,null,"STOPPED","USER_STOP");
        },null);
    }
    /** A finite, explicit observe-only compatibility check can clear the persisted circuit. */
    public void compatibilityChecked(Readiness readiness,Runnable success){
        if(!ready(readiness))return;Stamp stamp=stamp();
        transaction(stamp,db->{if(!ready(readiness))throw new Stale();db.delete("app_meta","key=?",new String[]{"circuit:"+stamp.namespace()});},success);
    }
    /** Called only after measured installation and visible receiver verification plus user selection. */
    public void bindReceiver(Binding binding,Runnable success) {
        if(!binding.bound()||!installation.equals(binding.installation())||!binding.packageDigest().matches("[a-f0-9]{64}"))throw new IllegalArgumentException("INVALID_BINDING");
        Identity.canonicalPhone(binding.receiver());switchNamespace(binding,success);
    }
    public void localChoices(Runnable success) { switchNamespace(new Binding(installation,"","","",""),success); }
    private void switchNamespace(Binding binding,Runnable success) {
        emergencyStop();long owner=dataEpoch.incrementAndGet();optionVetoes.clear();consentVeto=true;long consentOwner=commandSequence.incrementAndGet();consentCommand=consentOwner;
        writer.execute(()->{
            try{
                if(owner!=dataEpoch.get())return;clearPublished();SQLiteDatabase db=helper.getWritableDatabase();db.beginTransaction();
                try{
                    db.execSQL("UPDATE namespace SET active=0,paused=1,global_revision=global_revision+1");db.execSQL("UPDATE action_job SET state='CANCELED',nonce=NULL WHERE action='UNBLOCK'");
                    long id=-1;
                    try(Cursor c=db.rawQuery("SELECT id FROM namespace WHERE installation=? AND package_digest=? AND profile_key=? AND receiver_binding=?",new String[]{installation,binding.packageDigest(),binding.profile(),binding.receiver()})){if(c.moveToFirst())id=c.getLong(0);}
                    if(id<0){ContentValues row=new ContentValues();row.put("installation",installation);row.put("package_digest",binding.packageDigest());row.put("profile_key",binding.profile());row.put("receiver_binding",binding.receiver());row.put("qualification_id",binding.adapter());id=db.insertOrThrow("namespace",null,row);}
                    db.execSQL("UPDATE namespace SET active=1,enabled=0,paused=1,qualification_id=? WHERE id=?",new Object[]{binding.adapter(),id});db.setTransactionSuccessful();
                }finally{db.endTransaction();}
                publish(db);main.post(()->{if(owner==dataEpoch.get()&&consentCommand==consentOwner){consentVeto=false;notifyChanged();if(success!=null)success.run();}});
            }catch(Exception error){fail();}
        });
    }
    public void reset(Runnable success) {
        emergencyStop();metricsEpoch.incrementAndGet();long owner=dataEpoch.incrementAndGet();pendingChoices.clear();optionVetoes.clear();consentVeto=true;long consentOwner=commandSequence.incrementAndGet();consentCommand=consentOwner;
        writer.execute(()->{
            try{
                if(owner!=dataEpoch.get())return;clearPublished();SQLiteDatabase db=helper.getWritableDatabase();db.beginTransaction();
                try{
                    for(String table:new String[]{"action_job","action_event","account","attention_daily","app_meta","namespace"})db.delete(table,null,null);
                    db.execSQL("INSERT INTO namespace(id,installation,active) VALUES(1,?,1)",new Object[]{installation});
                    db.execSQL("INSERT INTO app_meta(key,value) VALUES('ui_data_identity',?)",new Object[]{UUID.randomUUID().toString()});db.setTransactionSuccessful();
                }finally{db.endTransaction();}
                failure="";publish(db);main.post(()->{if(owner==dataEpoch.get()){if(consentCommand==consentOwner)consentVeto=false;notifyChanged();if(success!=null)success.run();}});
            }catch(Exception error){fail();}
        });
    }
    private boolean currentEvidence(Evidence e,long owner) {
        Snapshot s=current();return e!=null&&authority.matches(owner)&&consented()&&s.binding().bound()&&e.namespace()==s.namespace()&&e.completeProfile()&&e.sideEffectFree()
            &&s.binding().receiver().equals(e.receiver())&&s.binding().adapter().equals(e.adapter())&&SystemClock.elapsedRealtime()>=e.observedElapsed()
            &&SystemClock.elapsedRealtime()-e.observedElapsed()<=RuleEngine.EVIDENCE_TTL_MS;
    }
    public void observe(Evidence e,String name) {
        long owner=epoch();if(!currentEvidence(e,owner))return;String phone=Identity.canonicalPhone(e.phone());Stamp stamp=stamp();long revision=current().globalRevision();
        transaction(stamp,db->{
            if(!currentEvidence(e,owner)||current().globalRevision()!=revision)throw new Stale();
            if(e.kind()!=Kind.BUSINESS_CONFIRMED&&e.kind()!=Kind.REGULAR_PROFILE_OBSERVED)return;
            long now=System.currentTimeMillis();
            if(e.kind()==Kind.BUSINESS_CONFIRMED){try(Cursor c=db.rawQuery("SELECT id FROM account WHERE namespace_id=? AND phone=?",new String[]{""+stamp.namespace(),phone})){if(!c.moveToFirst())ensureCapacity(db);}}
            db.execSQL("INSERT OR IGNORE INTO account(namespace_id,phone,name,search_key,first_seen,last_seen) SELECT ?,?,?,?,?,? WHERE (SELECT count(*) FROM account)<?",new Object[]{stamp.namespace(),phone,Identity.label(name),Identity.searchKey(name),now,now,ACCOUNT_LIMIT});
            db.execSQL("UPDATE account SET review=CASE WHEN kind='REGULAR_PROFILE_OBSERVED' AND ?='BUSINESS_CONFIRMED' THEN 'TYPE_CHANGED' ELSE 'NONE' END,hint_bits=0,kind=?,observed_state=?,ever_business=MAX(ever_business,?),checked_at=?,last_seen=? WHERE namespace_id=? AND phone=?",new Object[]{e.kind().name(),e.kind().name(),e.blockState().name(),e.kind()==Kind.BUSINESS_CONFIRMED?1:0,now,now,stamp.namespace(),phone});
            // Reconcile only settled block jobs. An observation never completes a mutation or an unblock grant.
            db.execSQL("DELETE FROM action_job WHERE action='BLOCK' AND state IN ('DONE','CANCELED') AND account_id IN (SELECT id FROM account WHERE namespace_id=? AND phone=? AND observed_state='UNBLOCKED')",new Object[]{stamp.namespace(),phone});
            db.execSQL("INSERT OR IGNORE INTO action_job(account_id,action,state,global_revision,account_revision,created_at,updated_at) SELECT a.id,'BLOCK','PENDING',n.global_revision,a.revision,?,? FROM account a JOIN namespace n ON n.id=a.namespace_id WHERE a.namespace_id=? AND a.phone=? AND a.choice<>'ALLOW' AND (a.kind='BUSINESS_CONFIRMED' OR a.choice='DENY_MANUAL') AND a.observed_state='UNBLOCKED'",new Object[]{now,now,stamp.namespace(),phone});
        },null);
    }
    private boolean matchesPlan(SQLiteDatabase db,Plan p,boolean verification) {
        try(Cursor c=db.rawQuery("SELECT a.phone,a.revision,j.action,j.state,j.nonce,j.generation,n.global_revision,n.receiver_binding,n.qualification_id,j.global_revision,j.account_revision,j.grant_created_at,n.consent_version FROM account a JOIN action_job j ON a.id=j.account_id JOIN namespace n ON n.id=a.namespace_id WHERE a.id=? AND n.id=? AND n.active=1",new String[]{""+p.accountId(),""+p.namespace()})) {
            if(!c.moveToFirst())return false;
            long now=System.currentTimeMillis();
            if(p.action()==Action.UNBLOCK&&(now<c.getLong(11)||now-c.getLong(11)>=RuleEngine.GRANT_TTL_MS))return false;
            if(c.getLong(9)!=p.globalRevision()||c.getLong(10)!=p.accountRevision()||!CONSENT_VERSION.equals(c.getString(12)))return false;
            return p.phone().equals(c.getString(0))&&p.accountRevision()==c.getLong(1)&&p.action().name().equals(c.getString(2))
                &&!set("DONE","CANCELED","FAILED").contains(c.getString(3))&&java.util.Objects.equals(p.nonce()==null?"":p.nonce(),c.isNull(4)?"":c.getString(4))
                &&(!verification||!p.confirmationAttempted()||p.generation()==c.getLong(5))&&p.globalRevision()==c.getLong(6)&&p.receiver().equals(c.getString(7))&&p.adapter().equals(c.getString(8));
        }
    }
    public void journal(Plan p,Runnable success,Runnable failed) {
        Stamp stamp=stamp();long owner=epoch();writer.execute(()->{
            try{
                SQLiteDatabase db=helper.getWritableDatabase();db.beginTransaction();
                try{
                    require(db,stamp);if(disarmed()||current().circuitOpen()||!authority.matches(owner)||!matchesPlan(db,p,false)||vetoed(p.accountId()))throw new Stale();
                    boolean entry=p.control()==io.github.appunnim.businessgate.automation.AutomationController.Control.BLOCK_ENTRY||p.control()==io.github.appunnim.businessgate.automation.AutomationController.Control.UNBLOCK_ENTRY;
                    try(Cursor job=db.rawQuery("SELECT attempts,updated_at,state,generation FROM action_job WHERE account_id=?",new String[]{""+p.accountId()})){
                        if(!job.moveToFirst())throw new Stale();
                        if(entry){
                            if(RetryPolicy.eligibility(job.getInt(0),job.getLong(1),System.currentTimeMillis())!=RetryPolicy.Eligibility.READY)throw new Stale();
                            db.execSQL("UPDATE action_job SET attempts=attempts+1 WHERE account_id=?",new Object[]{p.accountId()});
                        }else if(!job.getString(2).equals("ACTION_INTENT")||job.getLong(3)!=p.generation())throw new Stale();
                    }
                    ContentValues values=new ContentValues();values.put("state",p.control()==io.github.appunnim.businessgate.automation.AutomationController.Control.CONFIRM_BLOCK||p.control()==io.github.appunnim.businessgate.automation.AutomationController.Control.CONFIRM_UNBLOCK?"VERIFYING":"ACTION_INTENT");values.put("generation",p.generation());values.put("updated_at",System.currentTimeMillis());
                    if(db.update("action_job",values,"account_id=?",new String[]{""+p.accountId()})!=1)throw new Stale();event(db,p.accountId(),"INTENT","ACTION_INTENT");db.setTransactionSuccessful();
                }finally{db.endTransaction();}
                publish(db);main.post(()->{if(authority.matches(owner)&&stamp.data()==dataEpoch.get())success.run();else failed.run();});
            }catch(Stale ignored){main.post(failed);}catch(Exception error){fail();main.post(failed);}
        });
    }
    public void verified(Plan p,Evidence e,Consumer<CommitResult> result) {
        Stamp stamp=stamp();long owner=epoch();writer.execute(()->{
            CommitResult outcome;
            try{
                SQLiteDatabase db=helper.getWritableDatabase();db.beginTransaction();
                try{
                    require(db,stamp);if(!currentEvidence(e,owner)||!matchesPlan(db,p,true)||!p.phone().equals(e.phone())||e.generation()!=p.generation()
                        ||e.blockState()!=(p.action()==Action.BLOCK?BlockState.BLOCKED:BlockState.UNBLOCKED))throw new Stale();
                    db.execSQL("UPDATE account SET observed_state=?,gate_owned=CASE WHEN ?='UNBLOCKED' THEN 0 WHEN ?=1 THEN 1 ELSE gate_owned END,checked_at=? WHERE id=? AND namespace_id=?",new Object[]{e.blockState().name(),e.blockState().name(),p.confirmationAttempted()?1:0,System.currentTimeMillis(),p.accountId(),p.namespace()});
                    db.execSQL("UPDATE action_job SET state='DONE',nonce=NULL WHERE account_id=?",new Object[]{p.accountId()});event(db,p.accountId(),"VERIFIED","VERIFIED");prune(db);db.setTransactionSuccessful();
                }finally{db.endTransaction();}
                publish(db);outcome=CommitResult.COMMITTED;
            }catch(Stale ignored){outcome=CommitResult.STALE;}catch(Exception error){fail();outcome=CommitResult.FAILED;}
            CommitResult committed=outcome;main.post(()->result.accept(committed==CommitResult.COMMITTED&&(!authority.matches(owner)||stamp.data()!=dataEpoch.get()||current().namespace()!=p.namespace()||current().account(p.accountId())==null||current().account(p.accountId()).revision()!=p.accountRevision())?CommitResult.STALE:committed));
        });
    }
    public void reserveReminder(Runnable notify) {
        Stamp stamp=stamp();long command=commandSequence.get();writer.execute(()->{
            try{
                SQLiteDatabase db=helper.getWritableDatabase();require(db,stamp);Snapshot s=current();if(!optionEnabled("digest")||!optionEnabled("sales_hints"))return;
                long now=System.currentTimeMillis();
                try(Cursor c=db.rawQuery("SELECT value FROM app_meta WHERE key='last_digest_ms'",null)){if(c.moveToFirst()){long last=Long.parseLong(c.getString(0));if(now<last||now-last<30L*86400000)return;}}
                try(Cursor c=db.rawQuery("SELECT count(*) FROM account WHERE namespace_id=? AND choice='DEFAULT' AND review='POSSIBLE_COMMERCIAL' AND dismissed_until<=?",new String[]{""+stamp.namespace(),""+now})){if(!c.moveToFirst()||c.getLong(0)==0)return;}
                db.execSQL("INSERT OR REPLACE INTO app_meta(key,value) VALUES('last_digest_ms',?)",new Object[]{""+now});
                main.post(()->{if(stamp.data()==dataEpoch.get()&&current().namespace()==stamp.namespace()&&command==commandSequence.get()&&optionEnabled("digest")&&optionEnabled("sales_hints"))notify.run();});
            }catch(Stale ignored){/* No notification for an old namespace. */}catch(Exception error){fail();}
        });
    }
    public void effortSummary(Consumer<String> callback) {
        writer.execute(()->{
            try(Cursor c=helper.getReadableDatabase().rawQuery("SELECT COALESCE(SUM(management_ms),0),COALESCE(SUM(occupancy_ms),0),COALESCE(SUM(union_ms),0) FROM attention_daily",null)) {
                c.moveToFirst();String result="Observed effort, retained days\nManagement: "+c.getLong(0)/1000+" seconds\nVisible sessions: "+c.getLong(1)/1000+" seconds\nCombined, without overlap: "+c.getLong(2)/1000+" seconds\n\nLocal lower-bound timings. Unobserved reading and outside-app repair time require a separate pilot. The monthly attention target is not yet measured.";main.post(()->callback.accept(result));
            }catch(Exception error){fail();}
        });
    }
    public void attention(String day,long[] durations) {
        if(durations.length!=3||durations[0]<0||durations[1]<0||durations[2]<0||durations[2]>durations[0]+durations[1])throw new IllegalArgumentException("INVALID_INTERVAL");
        java.time.LocalDate.parse(day);long[] copy=durations.clone();long owner=metricsEpoch.get();
        writer.execute(()->{
            try{
                if(owner!=metricsEpoch.get())return;SQLiteDatabase db=helper.getWritableDatabase();db.beginTransaction();
                try{
                    if(owner!=metricsEpoch.get())throw new Stale();
                    db.execSQL("INSERT OR IGNORE INTO attention_daily(day_utc) VALUES(?)",new Object[]{day});
                    db.execSQL("UPDATE attention_daily SET management_ms=management_ms+?,occupancy_ms=occupancy_ms+?,union_ms=union_ms+? WHERE day_utc=?",new Object[]{copy[0],copy[1],copy[2],day});
                    db.execSQL("DELETE FROM attention_daily WHERE day_utc<?",new Object[]{java.time.LocalDate.now(java.time.ZoneOffset.UTC).minusDays(35).toString()});db.setTransactionSuccessful();
                }finally{db.endTransaction();}
                // Metric-only checkpoints do not reload fifty thousand accounts or move UI rows.
            }catch(Stale ignored){/* Reset owns subsequent metrics. */}catch(Exception error){fail();}
        });
    }
    public void attention(long[] durations) { attention(java.time.LocalDate.now(java.time.ZoneOffset.UTC).toString(),durations); }
    private void prune(SQLiteDatabase db) {
        long now=System.currentTimeMillis();
        db.execSQL("DELETE FROM action_event WHERE at_ms<? OR id NOT IN (SELECT id FROM action_event ORDER BY at_ms DESC,id DESC LIMIT 5000)",new Object[]{now-30L*86400000});
        db.execSQL("DELETE FROM account WHERE choice='DEFAULT' AND ever_business=0 AND review='NONE' AND id NOT IN (SELECT account_id FROM action_job) AND ((kind='REGULAR_PROFILE_OBSERVED' AND last_seen<?) OR (kind='UNKNOWN' AND last_seen<?))",new Object[]{now-90L*86400000,now-30L*86400000});
        db.execSQL("DELETE FROM attention_daily WHERE day_utc<?",new Object[]{java.time.LocalDate.now(java.time.ZoneOffset.UTC).minusDays(35).toString()});
        db.execSQL("UPDATE action_job SET state='CANCELED',nonce=NULL WHERE action='UNBLOCK' AND state NOT IN ('DONE','CANCELED','FAILED') AND (grant_created_at>? OR grant_created_at<=?)",new Object[]{now,now-RuleEngine.GRANT_TTL_MS});
    }
    private static Set<String> set(String... values) { return new java.util.HashSet<>(java.util.Arrays.asList(values)); }
    private static void event(SQLiteDatabase db,Long id,String type,String reason) { db.execSQL("INSERT INTO action_event(account_id,type,reason,at_ms) VALUES(?,?,?,?)",new Object[]{id,type,reason,System.currentTimeMillis()}); }
    private static String string(Cursor c,String key) { String value=c.getString(c.getColumnIndexOrThrow(key));return value==null?"":value; }
    private static String string(Cursor c,int column) { String value=c.getString(column);return value==null?"":value; }
    private static long number(Cursor c,String key) { return c.getLong(c.getColumnIndexOrThrow(key)); }
}
