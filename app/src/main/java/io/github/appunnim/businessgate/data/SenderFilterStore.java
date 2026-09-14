package io.github.appunnim.businessgate.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.os.Looper;
import io.github.appunnim.businessgate.policy.Model.*;
import io.github.appunnim.businessgate.policy.NameVisibilityPolicy;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/** Account-scoped, durable rules and receipts. Activity tokens and message bodies are never saved. */
public final class SenderFilterStore {
    public record Item(String id,String name,long updatedAt,String status,String phone,int count) {
        public boolean needsReview(){return !status.startsWith("VERIFIED")&&!status.startsWith("NO_PENDING_ACTION")&&!status.startsWith("KEPT")&&!status.equals("REMOVAL_CHANGED")&&!status.equals("DISMISSAL_FAILED");}
    }
    public record State(String scope,boolean ready,boolean enabled,boolean cleanup,Map<String,String> learned,List<Item> items,String error) {
        static State empty(String scope){return new State(scope,false,false,false,Map.of(),List.of(),"");}
    }
    private final GateRepository repository;
    private final SQLiteOpenHelper helper;
    private final java.util.concurrent.ExecutorService writer=Executors.newSingleThreadExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());
    private final List<Runnable> listeners=new java.util.concurrent.CopyOnWriteArrayList<>();
    private final Map<String,SenderCatalogue.Entry> catalogue;
    private volatile State state=State.empty("");
    private long generation;
    private String dataIdentity="";
    private boolean optionPending;
    public SenderFilterStore(Context context,GateRepository repository){
        this.repository=repository;Map<String,SenderCatalogue.Entry> loaded;
        try{loaded=SenderCatalogue.load(context);}catch(RuntimeException invalid){loaded=Map.of();}
        catalogue=loaded;
        helper=new SQLiteOpenHelper(context,"sender-filter.db",null,1){
            public void onCreate(SQLiteDatabase db){
                db.execSQL("CREATE TABLE scope(id TEXT PRIMARY KEY, enabled INTEGER NOT NULL DEFAULT 0, cleanup INTEGER NOT NULL DEFAULT 0)");
                db.execSQL("CREATE TABLE learned(scope TEXT NOT NULL, phone TEXT NOT NULL, name TEXT NOT NULL, PRIMARY KEY(scope,phone))");
                db.execSQL("CREATE TABLE queue(scope TEXT NOT NULL,id TEXT NOT NULL,name TEXT NOT NULL,updated INTEGER NOT NULL,status TEXT NOT NULL,phone TEXT NOT NULL DEFAULT '',count INTEGER NOT NULL DEFAULT 1,PRIMARY KEY(scope,id))");
            }
            public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion){throw new IllegalStateException("FILTER_SCHEMA");}
        };
        repository.addListener(this::synchronize);synchronize();
    }
    private String scope(){var s=repository.current();return s.loaded()&&s.binding().bound()&&!repository.dataIdentity().isEmpty()?repository.dataIdentity()+":"+s.namespace()+":"+io.github.appunnim.businessgate.automation.AdapterRegistry.sha256(s.binding().toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)):"";}
    public State current(){return state;}
    public boolean enabled(){return state.ready()&&state.scope().equals(scope())&&state.enabled()&&!optionPending&&repository.consented()&&repository.optionEnabled("discovery")&&repository.nameVisibilityPolicy()!=null&&!catalogue.isEmpty();}
    public Map<String,SenderCatalogue.Entry> catalogue(){return catalogue;}
    public Set<String> names(){Set<String> names=new TreeSet<>(catalogue.keySet());if(state.scope().equals(scope()))names.addAll(state.learned().values());return Set.copyOf(names);}
    public void addListener(Runnable listener){listeners.add(listener);}
    public void removeListener(Runnable listener){listeners.remove(listener);}
    private void changed(){for(Runnable listener:listeners)listener.run();}
    private void synchronize(){
        String scope=scope();
        if(!state.scope().equals(scope)||!dataIdentity.equals(repository.dataIdentity())){
            dataIdentity=repository.dataIdentity();generation++;optionPending=false;state=State.empty(scope);changed();
            if(!scope.isEmpty()){String identity=repository.dataIdentity();write(db->{db.delete("learned","scope NOT LIKE ?",new String[]{identity+":%"});db.delete("queue","scope NOT LIKE ?",new String[]{identity+":%"});db.delete("scope","id NOT LIKE ?",new String[]{identity+":%"});},null);}
            else if(repository.current().loaded()) {String identity=repository.dataIdentity();writer.execute(()->{try{var db=helper.getWritableDatabase();db.delete("learned","scope NOT LIKE ?",new String[]{identity+":%"});db.delete("queue","scope NOT LIKE ?",new String[]{identity+":%"});db.delete("scope","id NOT LIKE ?",new String[]{identity+":%"});}catch(RuntimeException ignored){/* Filtering is disabled while unbound. */}});}
            return;
        }
        if(!state.ready())return;
        Map<String,String> additions=new HashMap<>();
        for(Account account:repository.current().accounts())if(account.kind()==Kind.BUSINESS_CONFIRMED&&!account.businessName().isEmpty()&&!account.businessName().equals(state.learned().get(account.phone())))additions.put(account.phone(),NameVisibilityPolicy.nameKey(account.businessName()));
        if(!additions.isEmpty())write(db->{for(var entry:additions.entrySet())db.execSQL("INSERT OR REPLACE INTO learned(scope,phone,name) VALUES(?,?,?)",new Object[]{scope,entry.getKey(),entry.getValue()});},null);
    }
    public void configure(boolean enabled,boolean cleanup,Consumer<Boolean> completed){
        if(optionPending||state.scope().isEmpty()||!state.ready()){if(completed!=null)completed.accept(false);return;}
        optionPending=true;changed();String scope=state.scope();
        write(db->db.execSQL("UPDATE scope SET enabled=?,cleanup=? WHERE id=?",new Object[]{enabled?1:0,cleanup?1:0,scope}),ok->{optionPending=false;changed();if(completed!=null)completed.accept(ok);});
    }
    public void prepare(String id,String name,Consumer<Boolean> completed){
        if(!enabled()){completed.accept(false);return;}String scope=state.scope();
        write(db->{
            long count=android.database.DatabaseUtils.longForQuery(db,"SELECT count(*) FROM queue WHERE scope=?",new String[]{scope});
            long present=android.database.DatabaseUtils.longForQuery(db,"SELECT count(*) FROM queue WHERE scope=? AND id=?",new String[]{scope,id});
            if(count>=500&&present==0)throw new IllegalStateException("FILTER_QUEUE_FULL");
            long observations=present==0?0:android.database.DatabaseUtils.longForQuery(db,"SELECT count FROM queue WHERE scope=? AND id=?",new String[]{scope,id});
            db.execSQL("INSERT OR REPLACE INTO queue(scope,id,name,updated,status,count) VALUES(?,?,?,?,?,?)",new Object[]{scope,id,NameVisibilityPolicy.nameKey(name),System.currentTimeMillis(),"DISMISSAL_PENDING",observations+1});
        },completed);
    }
    public void result(String id,String status,String phone){String scope=state.scope();if(scope.isEmpty())return;
        write(db->db.execSQL("UPDATE queue SET status=?,phone=?,updated=? WHERE scope=? AND id=?",new Object[]{status,phone==null?"":phone,System.currentTimeMillis(),scope,id}),null);
    }
    private interface Change{void apply(SQLiteDatabase db);}
    private void write(Change change,Consumer<Boolean> completed){
        final String scope=state.scope();final long owner=generation;if(scope.isEmpty()){if(completed!=null)completed.accept(false);return;}
        writer.execute(()->{
            State next;boolean ok;
            try{var db=helper.getWritableDatabase();db.beginTransaction();try{db.execSQL("INSERT OR IGNORE INTO scope(id) VALUES(?)",new Object[]{scope});change.apply(db);next=read(db,scope);db.setTransactionSuccessful();}finally{db.endTransaction();}ok=true;}
            catch(RuntimeException failed){next=new State(scope,false,false,false,Map.of(),List.of(),"Name filter storage unavailable");ok=false;}
            final State result=next;final boolean saved=ok;
            main.post(()->{if(owner!=generation||!scope.equals(scope())){if(completed!=null)completed.accept(false);return;}state=result;changed();if(completed!=null)completed.accept(saved);if(saved)synchronize();});
        });
    }
    private State read(SQLiteDatabase db,String scope){
        boolean enabled,cleanup;try(var c=db.rawQuery("SELECT enabled,cleanup FROM scope WHERE id=?",new String[]{scope})){if(!c.moveToFirst())throw new IllegalStateException("FILTER_SCOPE");enabled=c.getInt(0)==1;cleanup=c.getInt(1)==1;}
        Map<String,String> learned=new HashMap<>();try(var c=db.rawQuery("SELECT phone,name FROM learned WHERE scope=?",new String[]{scope})){while(c.moveToNext())learned.put(c.getString(0),c.getString(1));}
        List<Item> items=new ArrayList<>();try(var c=db.rawQuery("SELECT id,name,updated,status,phone,count FROM queue WHERE scope=? ORDER BY updated DESC",new String[]{scope})){while(c.moveToNext())items.add(new Item(c.getString(0),c.getString(1),c.getLong(2),c.getString(3),c.getString(4),c.getInt(5)));}
        return new State(scope,true,enabled,cleanup,Map.copyOf(learned),List.copyOf(items),"");
    }
}
