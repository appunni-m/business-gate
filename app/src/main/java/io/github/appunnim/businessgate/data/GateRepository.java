package io.github.appunnim.businessgate.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import io.github.appunnim.businessgate.policy.Identity;
import io.github.appunnim.businessgate.policy.Model.*;
import io.github.appunnim.businessgate.automation.AutomationController.Plan;
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/** One serial writer. UI decisions are published only after successful transactions. */
public final class GateRepository {
    public static final String CONSENT_VERSION = "screen-v1";
    private final GateDbHelper helper;
    private final Context context;
    private final ExecutorService writer = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final AtomicReference<Snapshot> snapshot = new AtomicReference<>(Snapshot.empty());
    private final Set<Runnable> listeners = new CopyOnWriteArraySet<>();
    private final java.util.concurrent.ConcurrentHashMap<Long,Long> vetoes = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.atomic.AtomicLong commandSequence = new java.util.concurrent.atomic.AtomicLong();
    private volatile boolean disarmed = true;
    private volatile String failure = "";
    public GateRepository(Context context) { this.context = context; helper = new GateDbHelper(context); initialize(); }
    public Snapshot current() { return snapshot.get(); }
    public boolean disarmed() { return disarmed || !failure.isEmpty(); }
    public boolean vetoed(long id) { return vetoes.containsKey(id); }
    public void reload(Runnable success) { transaction(db -> {}, success); }
    public void addListener(Runnable listener) { listeners.add(listener); }
    public void removeListener(Runnable listener) { listeners.remove(listener); }
    private void notifyChanged() { main.post(() -> listeners.forEach(Runnable::run)); }
    private void initialize() {
        writer.execute(() -> {
            try {
                File marker = new File(context.getNoBackupFilesDir(), "installation");
                if (!marker.exists()) Files.write(marker.toPath(), UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
                String installation = new String(Files.readAllBytes(marker.toPath()), StandardCharsets.UTF_8);
                SQLiteDatabase db = helper.getWritableDatabase();
                db.beginTransaction();
                try {
                    db.execSQL("INSERT OR IGNORE INTO namespace(id,installation) VALUES(1,?)", new Object[]{installation});
                    try (Cursor c = db.rawQuery("SELECT installation FROM namespace WHERE id=1", null)) {
                        if (!c.moveToFirst()) throw new IllegalStateException("NAMESPACE_MISSING");
                        if (!installation.equals(c.getString(0))) {
                            db.execSQL("UPDATE namespace SET installation=?,enabled=0,paused=1,consent_version='',setup='WELCOME',global_revision=global_revision+1", new Object[]{installation});
                            db.execSQL("UPDATE action_job SET state='CANCELED',nonce=NULL");
                            event(db, null, "RECOVERY", "INSTALLATION_CHANGED");
                        }
                    }
                    db.execSQL("UPDATE action_job SET state='REINSPECT',reason='PROCESS_RESTART' WHERE state IN ('ACTION_INTENT','VERIFYING')");
                    prune(db);
                    db.setTransactionSuccessful();
                } finally { db.endTransaction(); }
                publish(db);
            } catch (Exception error) { fail(); }
        });
    }
    private interface Write { void run(SQLiteDatabase db); }
    private void transaction(Write operation, Runnable success) {
        writer.execute(() -> {
            try {
                SQLiteDatabase db = helper.getWritableDatabase();
                db.beginTransaction();
                try { operation.run(db); db.setTransactionSuccessful(); }
                finally { db.endTransaction(); }
                publish(db);
                if (success != null) main.post(success);
            } catch (Exception error) { fail(); }
        });
    }
    private void fail() { disarmed = true; failure = "Could not save. No new blocks will run."; Snapshot s = current();
        snapshot.set(new Snapshot(s.namespace(),s.globalRevision(),s.loaded(),s.enabled(),true,s.consent(),s.salesHints(),s.discovery(),s.digest(),s.setup(),s.accounts(),failure)); notifyChanged(); }
    private void publish(SQLiteDatabase db) {
        try (Cursor n = db.rawQuery("SELECT * FROM namespace WHERE id=1", null)) {
            if (!n.moveToFirst()) throw new IllegalStateException("NAMESPACE_MISSING");
            Snapshot next = new Snapshot(1, number(n,"global_revision"),true,number(n,"enabled")==1,number(n,"paused")==1,
                CONSENT_VERSION.equals(string(n,"consent_version")), number(n,"sales_hints")==1,number(n,"discovery")==1,number(n,"digest")==1,
                string(n,"setup"),readAccounts(db,"",new String[0]),failure);
            snapshot.set(next);
        }
        notifyChanged();
    }
    private List<Account> readAccounts(SQLiteDatabase db, String extra, String[] args) {
        List<Account> rows = new ArrayList<>();
        try (Cursor c = db.rawQuery("SELECT a.*, j.state AS job_state,j.action AS job_action,j.nonce,j.grant_created_at FROM account a LEFT JOIN action_job j ON a.id=j.account_id WHERE a.namespace_id=1 " + extra + " ORDER BY a.search_key,a.phone,a.id", args)) {
            while (c.moveToNext()) rows.add(new Account(number(c,"id"),1,string(c,"phone"),string(c,"name"),Kind.valueOf(string(c,"kind")),Choice.valueOf(string(c,"choice")),
                BlockState.valueOf(string(c,"observed_state")),number(c,"gate_owned")==1,number(c,"revision"),number(c,"ever_business")==1,
                Review.valueOf(string(c,"review")),(int)number(c,"hint_bits"),number(c,"dismissed_until"),number(c,"checked_at"),number(c,"last_seen"),
                c.isNull(c.getColumnIndexOrThrow("job_state")) ? JobState.NONE : JobState.valueOf(string(c,"job_state")),
                c.isNull(c.getColumnIndexOrThrow("job_action")) ? Action.NONE : Action.valueOf(string(c,"job_action")),string(c,"nonce"),number(c,"grant_created_at")));
        }
        return rows;
    }
    public void search(String input, Consumer<List<Account>> result) {
        String q = Identity.query(input);
        writer.execute(() -> {
            try {
                String normalized = Identity.likeLiteral(Identity.searchKey(q));
                String digits = q.replaceAll("[ +()\\-]", "");
                boolean isDigits = !digits.isEmpty() && digits.matches("[0-9]+");
                String clause = q.isEmpty() ? "" : "AND (a.search_key LIKE ? ESCAPE '\\'" + (isDigits ? " OR a.phone LIKE ?" : "") + ")";
                String[] args = q.isEmpty() ? new String[0] : isDigits ? new String[]{"%"+normalized+"%","%"+digits+"%"} : new String[]{"%"+normalized+"%"};
                List<Account> rows = readAccounts(helper.getReadableDatabase(),clause,args);
                main.post(() -> result.accept(rows));
            } catch (Exception error) { fail(); }
        });
    }
    public void enableNumber(String rawPhone, String label, Runnable success) {
        String phone = Identity.canonicalPhone(rawPhone); String name = Identity.label(label);
        // Adding an existing number must revoke authority before the write is queued.
        long sequence=commandSequence.incrementAndGet();
        current().accounts().stream().filter(a -> a.phone().equals(phone)).forEach(a -> vetoes.put(a.id(),sequence));
        transaction(db -> {
            long now = System.currentTimeMillis();
            db.execSQL("INSERT OR IGNORE INTO account(namespace_id,phone,name,search_key,first_seen,last_seen) VALUES(1,?,?,?,?,?)",new Object[]{phone,name,Identity.searchKey(name),now,now});
            try(Cursor c=db.rawQuery("SELECT id FROM account WHERE namespace_id=1 AND phone=?",new String[]{phone})) {
                if(!c.moveToFirst()) throw new IllegalStateException("ACCOUNT_MISSING");
                choose(db,c.getLong(0),Choice.ALLOW);
            }
        }, () -> { current().accounts().stream().filter(a -> a.phone().equals(phone)).forEach(a -> vetoes.remove(a.id(),sequence)); if(success!=null)success.run(); });
    }
    public void choose(long id, Choice choice, Runnable success) {
        long sequence=commandSequence.incrementAndGet();
        if(choice==Choice.ALLOW) vetoes.put(id,sequence);
        transaction(db -> choose(db,id,choice), () -> { vetoes.remove(id,sequence); if(success!=null)success.run(); });
    }
    private void choose(SQLiteDatabase db,long id,Choice choice) {
        long now=System.currentTimeMillis();
        db.execSQL("UPDATE account SET choice=?,revision=revision+1,review='NONE',hint_bits=0,dismissed_until=0 WHERE id=? AND namespace_id=1",new Object[]{choice.name(),id});
        db.delete("action_job","account_id=?",new String[]{Long.toString(id)});
        try(Cursor c=db.rawQuery("SELECT revision,kind,observed_state FROM account WHERE id=? AND namespace_id=1",new String[]{Long.toString(id)})) {
            if(!c.moveToFirst()) throw new IllegalArgumentException("ACCOUNT_MISSING");
            boolean allow=choice==Choice.ALLOW;
            if(allow || choice==Choice.DENY_MANUAL || c.getString(1).equals(Kind.BUSINESS_CONFIRMED.name())) {
                db.execSQL("INSERT INTO action_job(account_id,action,state,global_revision,account_revision,nonce,grant_created_at,created_at,updated_at) SELECT ?,?,'PENDING',global_revision,?,?,?,?,? FROM namespace WHERE id=1",
                    new Object[]{id,allow?"UNBLOCK":"BLOCK",c.getLong(0),allow?UUID.randomUUID().toString():null,allow?now:0,now,now});
            }
        }
        event(db,id,"POLICY","USER_CHOICE");
    }
    public void dismiss(long id) {
        transaction(db -> { db.execSQL("UPDATE account SET dismissed_until=? WHERE id=?", new Object[]{System.currentTimeMillis()+30L*86400000,id}); event(db,id,"POLICY","HINT_DISMISSED"); },null);
    }
    public void updateSetup(String step, boolean consent, Runnable success) {
        Set<String> steps=new java.util.HashSet<>(java.util.Arrays.asList("WELCOME","ACCESS","CAPABILITY","DISCOVERY","REVIEW","READY"));
        if(!steps.contains(step)) throw new IllegalArgumentException("INVALID_STEP");
        transaction(db -> db.execSQL("UPDATE namespace SET setup=?,consent_version=?,consent_at=?,global_revision=global_revision+1 WHERE id=1",new Object[]{step,consent?CONSENT_VERSION:"",System.currentTimeMillis()}),success);
    }
    public void setting(String field, boolean value) {
        if(!new java.util.HashSet<>(java.util.Arrays.asList("sales_hints","discovery","digest")).contains(field)) throw new IllegalArgumentException("INVALID_SETTING");
        transaction(db -> { db.execSQL("UPDATE namespace SET "+field+"=? WHERE id=1",new Object[]{value?1:0});
            if(field.equals("sales_hints")&&!value) db.execSQL("UPDATE account SET hint_bits=0,review='NEW_SENDER' WHERE review='POSSIBLE_COMMERCIAL' AND choice='DEFAULT'");
        },null);
    }
    public void pause() {
        disarmed=true;
        transaction(db -> { db.execSQL("UPDATE namespace SET paused=1,global_revision=global_revision+1 WHERE id=1");
            db.execSQL("UPDATE action_job SET state='CANCELED',nonce=NULL WHERE action='UNBLOCK'");
            db.execSQL("UPDATE action_job SET state='REINSPECT' WHERE state IN ('ACTION_INTENT','VERIFYING')");
            event(db,null,"STOPPED","USER_STOP"); },null);
    }
    public void arm(boolean qualified, Runnable success) {
        if(!qualified || !current().consent() || !failure.isEmpty()) return;
        transaction(db -> { db.execSQL("UPDATE namespace SET enabled=1,paused=0,setup='READY',global_revision=global_revision+1 WHERE id=1");
            db.execSQL("UPDATE action_job SET global_revision=(SELECT global_revision FROM namespace WHERE id=1) WHERE action='BLOCK'");
        }, () -> { disarmed=false; if(success!=null)success.run(); });
    }
    public void emergencyStop() { disarmed=true; }
    public void reset(Runnable success) {
        disarmed=true; vetoes.clear();
        transaction(db -> { db.delete("action_job",null,null); db.delete("action_event",null,null); db.delete("account",null,null); db.delete("attention_daily",null,null); db.delete("app_meta",null,null);
            db.execSQL("UPDATE namespace SET enabled=0,paused=1,consent_version='',consent_at=0,setup='WELCOME',sales_hints=0,discovery=0,digest=0,receiver_binding='',qualification_id='',global_revision=global_revision+1");
        }, success);
    }
    public void observe(Evidence e,String name,boolean qualified) {
        if(!qualified || !current().consent() || e.namespace()!=current().namespace() || !e.completeProfile()) return;
        String phone=Identity.canonicalPhone(e.phone());
        transaction(db -> {
            long now=System.currentTimeMillis();
            db.execSQL("INSERT OR IGNORE INTO account(namespace_id,phone,name,search_key,first_seen,last_seen) SELECT 1,?,?,?,?,? WHERE (SELECT count(*) FROM account)<50000",new Object[]{phone,Identity.label(name),Identity.searchKey(name),now,now});
            db.execSQL("UPDATE account SET kind=?,observed_state=?,ever_business=MAX(ever_business,?),checked_at=?,last_seen=? WHERE namespace_id=1 AND phone=?",new Object[]{e.kind().name(),e.blockState().name(),e.kind()==Kind.BUSINESS_CONFIRMED?1:0,now,now,phone});
            db.execSQL("INSERT OR IGNORE INTO action_job(account_id,action,state,global_revision,account_revision,created_at,updated_at) SELECT a.id,'BLOCK','PENDING',n.global_revision,a.revision,?,? FROM account a JOIN namespace n ON n.id=a.namespace_id WHERE a.phone=? AND n.id=1 AND a.choice<>'ALLOW' AND (a.kind='BUSINESS_CONFIRMED' OR a.choice='DENY_MANUAL') AND a.observed_state<>'BLOCKED'",new Object[]{now,now,phone});
            db.execSQL("UPDATE action_job SET state='DONE',nonce=NULL WHERE account_id IN (SELECT id FROM account WHERE namespace_id=1 AND phone=?) AND ((action='BLOCK' AND ?='BLOCKED') OR (action='UNBLOCK' AND ?='UNBLOCKED'))",new Object[]{phone,e.blockState().name(),e.blockState().name()});
        },null);
    }
    public void journal(Plan p,Runnable success,Runnable failed) {
        writer.execute(() -> {
            try {
                SQLiteDatabase db=helper.getWritableDatabase(); db.beginTransaction();
                try {
                    ContentValues values=new ContentValues(); values.put("state","ACTION_INTENT"); values.put("generation",p.generation()); values.put("updated_at",System.currentTimeMillis());
                    int changed=db.update("action_job",values,"account_id=? AND account_revision=? AND global_revision=? AND state NOT IN ('CANCELED','DONE')",new String[]{""+p.accountId(),""+p.accountRevision(),""+p.globalRevision()});
                    if(changed!=1) throw new IllegalStateException("STALE_JOB");
                    event(db,p.accountId(),"INTENT","ACTION_INTENT"); db.setTransactionSuccessful();
                } finally { db.endTransaction(); }
                publish(db); main.post(success);
            } catch(Exception error) { disarmed=true; main.post(failed); }
        });
    }
    public void verified(Plan p,Evidence e) {
        transaction(db -> {
            try(Cursor c=db.rawQuery("SELECT a.revision,j.generation,n.global_revision FROM account a JOIN action_job j ON j.account_id=a.id JOIN namespace n ON n.id=a.namespace_id WHERE a.id=?",new String[]{""+p.accountId()})) {
                if(!c.moveToFirst() || c.getLong(0)!=p.accountRevision() || c.getLong(1)!=p.generation() || c.getLong(2)!=p.globalRevision())return;
            }
            db.execSQL("UPDATE account SET observed_state=?,gate_owned=?,checked_at=? WHERE id=? AND phone=?",new Object[]{e.blockState().name(),e.blockState()==BlockState.BLOCKED?1:0,System.currentTimeMillis(),p.accountId(),e.phone()});
            db.execSQL("UPDATE action_job SET state='DONE',nonce=NULL WHERE account_id=?",new Object[]{p.accountId()});
            event(db,p.accountId(),"VERIFIED","VERIFIED");
        },null);
    }
    public void reserveReminder(Runnable notify) {
        writer.execute(() -> {
            try {
                SQLiteDatabase db=helper.getWritableDatabase();
                long now=System.currentTimeMillis();
                try(Cursor c=db.rawQuery("SELECT value FROM app_meta WHERE key='last_digest_ms'",null)) {
                    if(c.moveToFirst()){long last=Long.parseLong(c.getString(0));if(now<last||now-last<30L*86400000)return;}
                }
                try(Cursor c=db.rawQuery("SELECT count(*) FROM account WHERE namespace_id=1 AND choice='DEFAULT' AND review='POSSIBLE_COMMERCIAL' AND dismissed_until<=?",new String[]{""+now})) {
                    if(!c.moveToFirst()||c.getLong(0)==0)return;
                }
                db.execSQL("INSERT OR REPLACE INTO app_meta(key,value) VALUES('last_digest_ms',?)",new Object[]{""+now});
                main.post(notify);
            }catch(Exception error){fail();}
        });
    }
    public void effortSummary(Consumer<String> callback) {
        writer.execute(() -> {
            try(Cursor c=helper.getReadableDatabase().rawQuery("SELECT COALESCE(SUM(management_ms),0),COALESCE(SUM(occupancy_ms),0),COALESCE(SUM(union_ms),0) FROM attention_daily",null)) {
                c.moveToFirst();String result="Observed effort, retained days\nManagement: "+c.getLong(0)/1000+" seconds\nVisible sessions: "+c.getLong(1)/1000+" seconds\nCombined, without overlap: "+c.getLong(2)/1000+" seconds\n\nLocal lower-bound timings. Unobserved reading and outside-app repair time require a separate pilot. The monthly attention target is not yet measured.";
                main.post(()->callback.accept(result));
            }catch(Exception error){fail();}
        });
    }
    public void attention(long[] durations) {
        String day=java.time.LocalDate.now(java.time.ZoneOffset.UTC).toString();
        transaction(db -> db.execSQL("INSERT INTO attention_daily(day_utc,management_ms,occupancy_ms,union_ms) VALUES(?,?,?,?) ON CONFLICT(day_utc) DO UPDATE SET management_ms=management_ms+excluded.management_ms,occupancy_ms=occupancy_ms+excluded.occupancy_ms,union_ms=union_ms+excluded.union_ms",new Object[]{day,durations[0],durations[1],durations[2]}),null);
    }
    private void prune(SQLiteDatabase db) {
        long now=System.currentTimeMillis();
        db.execSQL("DELETE FROM action_event WHERE at_ms<? OR id NOT IN (SELECT id FROM action_event ORDER BY at_ms DESC,id DESC LIMIT 5000)",new Object[]{now-30L*86400000});
        db.execSQL("DELETE FROM account WHERE choice='DEFAULT' AND ever_business=0 AND review='NONE' AND id NOT IN (SELECT account_id FROM action_job) AND ((kind='REGULAR_PROFILE_OBSERVED' AND last_seen<?) OR (kind='UNKNOWN' AND last_seen<?))",new Object[]{now-90L*86400000,now-30L*86400000});
        db.execSQL("DELETE FROM attention_daily WHERE day_utc<?",new Object[]{java.time.LocalDate.now(java.time.ZoneOffset.UTC).minusDays(35).toString()});
        db.execSQL("UPDATE action_job SET state='CANCELED',nonce=NULL WHERE action='UNBLOCK' AND (grant_created_at>? OR grant_created_at<=?)",new Object[]{now,now-7L*86400000});
    }
    private static void event(SQLiteDatabase db,Long id,String type,String reason) {
        db.execSQL("INSERT INTO action_event(account_id,type,reason,at_ms) VALUES(?,?,?,?)",new Object[]{id,type,reason,System.currentTimeMillis()});
    }
    private static String string(Cursor c,String key) { String value=c.getString(c.getColumnIndexOrThrow(key));return value==null?"":value; }
    private static long number(Cursor c,String key) { return c.getLong(c.getColumnIndexOrThrow(key)); }
}
