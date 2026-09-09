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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

/** Test APK only. Never included in either application variant. Uses synthetic identities. */
public final class GateInstrumentation extends Instrumentation {
    private Bundle arguments;
    private int assertions;
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
            if(mode.equals("verify-recovery")){
                check(repository.current().accounts().stream().anyMatch(a->a.jobState()==JobState.REINSPECT),"interrupted work re-inspected after actual process restart");
                check(repository.disarmed(),"startup is disarmed");
            }else if(mode.equals("prepare-recovery")){
                seed();try(GateDbHelper helper=new GateDbHelper(getTargetContext())){helper.getWritableDatabase().execSQL("UPDATE action_job SET state='ACTION_INTENT' WHERE account_id=4");}
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
                seed();Activity activity=launch();
                until(()->hasText(activity,"Harbor Clinic"));check(hasText(activity,"Paused · compatibility check needed"),"unsupported status visible");runOnMainSync(()->find(activity.getWindow().getDecorView(),android.widget.ListView.class).setSelection(6));until(()->hasText(activity,"Block pending"));check(hasText(activity,"Block pending"),"pending subtitle visible after scrolling");
                runOnMainSync(()->{EditText search=find(activity.getWindow().getDecorView(),EditText.class);search.setText("+12025550102");});
                until(()->hasText(activity,"Parcel Desk"));Thread.sleep(300);check(!hasText(activity,"Harbor Clinic"),"search excludes other exact number");
                runOnMainSync(()->find(activity.getWindow().getDecorView(),EditText.class).setText("No such local account"));until(()->hasText(activity,"No matching accounts"));check(hasText(activity,"Enable a number"),"empty search offers explicit number entry");
                runOnMainSync(()->find(activity.getWindow().getDecorView(),EditText.class).setText(""));until(()->hasText(activity,"Harbor Clinic"));
                check(repository.current().accounts().size()==6,"UI search does not mutate repository");
            }
            result.putString("stream","PASS "+assertions+" Android persistence, permission, recovery and native UI assertions; mode="+mode+"\n");finish(Activity.RESULT_OK,result);
        }catch(Throwable error){result.putString("stream","FAIL "+error.getClass().getSimpleName()+": "+error.getMessage()+"\n");finish(Activity.RESULT_CANCELED,result);}
    }
    private Activity launch(){return startActivitySync(new Intent(getTargetContext(),io.github.appunnim.businessgate.ui.MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}
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
