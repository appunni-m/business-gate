package io.github.appunnim.businessgate.service;

import android.app.*;
import android.content.Intent;
import android.os.SystemClock;
import io.github.appunnim.businessgate.GateApplication;
import io.github.appunnim.businessgate.automation.AdapterRegistry;
import io.github.appunnim.businessgate.data.GateRepository;
import io.github.appunnim.businessgate.policy.Model.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

/** Real Android notification lifecycle, fictional identities, account-free emulator only. */
public final class IncomingNotificationTests {
    private final Instrumentation test;
    private final GateRepository repository;
    private final BiConsumer<Boolean,String> check;
    private final android.content.Context context;
    private final NotificationManager manager;
    private final String pkg,channel="incoming-fixture";
    private int next=12000;
    public IncomingNotificationTests(Instrumentation test,GateRepository repository,BiConsumer<Boolean,String> check){
        this.test=test;this.repository=repository;this.check=check;context=test.getTargetContext();pkg=context.getPackageName();manager=context.getSystemService(NotificationManager.class);
    }
    private void until(BooleanSupplier ready)throws Exception{
        long end=SystemClock.elapsedRealtime()+10000;
        while(!ready.getAsBoolean()){if(SystemClock.elapsedRealtime()>end)throw new AssertionError("Notification fixture did not settle");Thread.sleep(25);}
    }
    private void main(Runnable action){
        Throwable[] failure={null};test.runOnMainSync(()->{try{action.run();}catch(Throwable error){failure[0]=error;}});
        if(failure[0]!=null)throw new AssertionError("Main-thread fixture action failed: "+failure[0],failure[0]);
    }
    private void commit(java.util.function.Consumer<Runnable> action)throws Exception{
        CountDownLatch latch=new CountDownLatch(1);main(()->action.accept(latch::countDown));if(!latch.await(10,TimeUnit.SECONDS))throw new AssertionError("Notification fixture transaction timed out");
    }
    private void shell(String command)throws Exception{
        try(var descriptor=test.getUiAutomation(android.app.UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES).executeShellCommand(command);
            var stream=new android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor)){stream.readAllBytes();}
    }
    private PendingIntent token(int id,boolean mutable){
        var intent=new Intent(context,io.github.appunnim.businessgate.ui.MainActivity.class).setAction("fixture-"+id);
        return PendingIntent.getActivity(context,id,intent,PendingIntent.FLAG_UPDATE_CURRENT|(mutable?PendingIntent.FLAG_MUTABLE:PendingIntent.FLAG_IMMUTABLE));
    }
    private Notification notice(int id,boolean group,boolean summary,boolean mutable){
        var style=new Notification.MessagingStyle(new Person.Builder().setName("Receiver fixture").build())
            .setGroupConversation(group).addMessage("Fixture body must never be inspected",System.currentTimeMillis(),new Person.Builder().setName("Fixture sender").build());
        return new Notification.Builder(context,channel).setSmallIcon(io.github.appunnim.businessgate.R.drawable.ic_gate)
            .setContentTitle("Fixture sender").setStyle(style).setContentIntent(token(id,mutable)).setGroup("fixture-group").setGroupSummary(summary).build();
    }
    private GateNotificationListener.Candidate candidate(int id){
        return GateNotificationListener.pending().stream().filter(c->c.route().equals(token(id,false))).findFirst().orElse(null);
    }
    private GateNotificationListener.Claim postAndClaim()throws Exception{
        int id=++next;manager.notify(id,notice(id,false,false,false));
        until(()->{boolean[] found={false};main(()->found[0]=candidate(id)!=null);return found[0];});
        GateNotificationListener.Claim[] result={null};main(()->result[0]=GateNotificationListener.claim(candidate(id).id()));
        check.accept(result[0]!=null,"actual posted direct notification can be explicitly claimed");return result[0];
    }
    private void arm()throws Exception{
        var s=repository.current();commit(done->repository.arm(new Readiness(s.namespace(),s.globalRevision(),repository.epoch(),SystemClock.elapsedRealtime(),s.binding()),true,done));
    }
    private void observed(String phone,Kind kind,BlockState state)throws Exception{
        arm();var s=repository.current();commit(done->repository.observe(new Evidence(s.namespace(),phone,kind,state,SystemClock.elapsedRealtime(),1,1,s.binding().receiver(),s.binding().adapter(),true,true),"Fixture business",done));
    }
    private String dismiss(GateNotificationListener.Claim claim,String phone){
        String[] result={null};main(()->{
            // The native identity stage is stubbed here; live-incoming exercises the actual route.
            claim.opened=true;GateNotificationListener.bindIdentity(claim,phone,"Fixture business",repository.current().binding().receiver());
            result[0]=GateNotificationListener.dismiss(claim,phone,"Fixture business",repository.current().globalRevision(),repository.epoch());
        });return result[0];
    }
    private void nameEnabled(boolean enabled)throws Exception{
        commit(done->repository.setBusinessNameEnabled(repository.businessNameScope(),"Fixture business",enabled,result->{check.accept(result==GateRepository.SaveResult.SAVED,"fixture business-name permission saved");done.run();}));arm();
    }
    private void visibleStop(Activity activity)throws Exception{
        var automation=test.getUiAutomation(android.app.UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES);
        var resolver=context.getContentResolver();String key=android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES;
        String original=android.provider.Settings.Secure.getString(resolver,key),enabled=android.provider.Settings.Secure.getString(resolver,android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
        String component=new android.content.ComponentName(context,GateAccessibilityService.class).flattenToString();
        automation.adoptShellPermissionIdentity(android.Manifest.permission.WRITE_SECURE_SETTINGS);
        try{
            java.util.List<String> services=new java.util.ArrayList<>();if(original!=null)for(String service:original.split(":"))if(!service.isEmpty()&&!service.equals(component))services.add(service);
            services.add(component);android.provider.Settings.Secure.putString(resolver,key,String.join(":",services));android.provider.Settings.Secure.putString(resolver,android.provider.Settings.Secure.ACCESSIBILITY_ENABLED,"1");
            until(GateAccessibilityService::connected);main(()->check.accept(GateAccessibilityService.selectInstallation(pkg),"select owned fixture for visible Stop test"));
            var first=postAndClaim();String id=first.candidate.id();GateNotificationListener.Claim[] active={null};
            var listenerField=GateNotificationListener.class.getDeclaredField("connected");listenerField.setAccessible(true);Object listener=listenerField.get(null);
            var claimField=GateNotificationListener.class.getDeclaredField("claim");claimField.setAccessible(true);
            main(()->{
                check.accept(GateAccessibilityService.requestIncoming(id,true),"visible incoming session starts from the foreground activity");
                try{
                    active[0]=(GateNotificationListener.Claim)claimField.get(listener);
                    var serviceField=GateAccessibilityService.class.getDeclaredField("connected");serviceField.setAccessible(true);Object service=serviceField.get(null);
                    var overlayField=GateAccessibilityService.class.getDeclaredField("overlay");overlayField.setAccessible(true);Object overlay=overlayField.get(service);
                    var viewField=overlay.getClass().getDeclaredField("view");viewField.setAccessible(true);((android.widget.Button)viewField.get(overlay)).performClick();
                }catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
            });
            check.accept(active[0]!=null&&!GateNotificationListener.valid(active[0])&&repository.disarmed(),"actual Stop button revokes the incoming claim and native authority");
            Thread.sleep(900);check.accept(GateAccessibilityService.status().equals("USER_STOP"),"delayed incoming navigation does not resume after Stop");
        }finally{
            android.provider.Settings.Secure.putString(resolver,key,original);android.provider.Settings.Secure.putString(resolver,android.provider.Settings.Secure.ACCESSIBILITY_ENABLED,enabled);automation.dropShellPermissionIdentity();
        }
    }
    public void run()throws Exception{
        var previous=Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread,error)->{
            String detail=android.util.Log.getStackTraceString(error);if(detail.length()>6000)detail=detail.substring(0,6000);
            var report=new android.os.Bundle();report.putString("stream","FAIL uncaught notification fixture error on "+thread.getName()+": "+detail+"\n");
            try{test.sendStatus(0,report);}finally{if(previous!=null)previous.uncaughtException(thread,error);}
        });
        try{runFixture();}finally{Thread.setDefaultUncaughtExceptionHandler(previous);}
    }
    private void runFixture()throws Exception{
        check.accept(android.os.Build.VERSION.SDK_INT>=36,"notification runtime fixture uses API 36 or newer");
        routeReceipts();
        GateApplication app=(GateApplication)context.getApplicationContext();
        check.accept(app.registry().measuredRoute(context,pkg)==null,"owned fixture has no production connected-app qualification");
        var measuredField=AdapterRegistry.class.getDeclaredField("measured");measuredField.setAccessible(true);Object measured=measuredField.get(app.registry());
        var rowsField=measured.getClass().getDeclaredField("rows");rowsField.setAccessible(true);var rows=(org.json.JSONArray)rowsField.get(measured);
        var original=new org.json.JSONObject(rows.getJSONObject(0).toString());
        String component=new android.content.ComponentName(context,GateNotificationListener.class).flattenToString();
        var connectedField=GateNotificationListener.class.getDeclaredField("connected");connectedField.setAccessible(true);
        try{
            // In-memory compatibility seam for this fixture APK. No bundled evidence changes;
            // no native business route is invoked, and all signing identities are read from Android.
            var fixture=new org.json.JSONObject(original.toString());var info=context.getPackageManager().getPackageInfo(pkg,android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES);
            String packageDigest=AdapterRegistry.sha256(pkg.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            fixture.put("packageSha256",packageDigest).put("versionCode",info.getLongVersionCode());
            fixture.put("signingSha256",new org.json.JSONArray().put(AdapterRegistry.sha256(info.signingInfo.getApkContentsSigners()[0].toByteArray())));
            var history=new org.json.JSONArray();for(var signer:info.signingInfo.getSigningCertificateHistory())history.put(AdapterRegistry.sha256(signer.toByteArray()));fixture.put("signingHistorySha256",history);
            var config=context.getResources().getConfiguration();var environment=fixture.getJSONObject("environment");
            environment.put("manufacturer",android.os.Build.MANUFACTURER).put("model",android.os.Build.MODEL).put("deviceLocale",config.getLocales().get(0).toLanguageTag())
                .put("densityDpi",config.densityDpi).put("fontScalePercent",Math.round(config.fontScale*100)).put("orientation",config.orientation);
            main(()->{try{rows.put(0,fixture);}catch(org.json.JSONException error){throw new IllegalStateException(error);}});
            commit(repository::reset);commit(done->repository.bindReceiver(new Binding(repository.installation(),packageDigest,"user-"+android.os.Process.myUserHandle().hashCode(),"+12025550199","business-profile-v1"),done));
            commit(done->repository.updateSetup("REVIEW",true,done));main(()->repository.setting("discovery",true));until(()->repository.optionEnabled("discovery"));
            shell("pm grant "+pkg+" android.permission.POST_NOTIFICATIONS");shell("cmd notification allow_listener "+component);
            until(()->{try{return connectedField.get(null)!=null;}catch(IllegalAccessException unavailable){return false;}});
            manager.createNotificationChannel(new NotificationChannel(channel,"Incoming fixture",NotificationManager.IMPORTANCE_LOW));
            Activity activity=test.startActivitySync(new Intent(context,io.github.appunnim.businessgate.ui.MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));until(activity::hasWindowFocus);
            for(int variant=0;variant<3;variant++){
                int id=++next;manager.notify(id,notice(id,variant==0,variant==1,variant==2));Thread.sleep(150);
                boolean[] absent={false};main(()->absent[0]=GateNotificationListener.pending().isEmpty());check.accept(absent[0],"group, summary or mutable token excluded: "+variant);manager.cancel(id);
            }
            visibleStop(activity);
            var replace=postAndClaim();manager.notify(next,notice(next,false,false,false));until(()->replace.invalid);
            check.accept(!GateNotificationListener.valid(replace),"same-key update revokes the notification claim");
            var stop=postAndClaim();main(()->GateNotificationListener.release(stop));check.accept(!GateNotificationListener.valid(stop),"Stop releases notification authority");
            var personal=postAndClaim();observed("+12025550101",Kind.REGULAR_PROFILE_OBSERVED,BlockState.BLOCKED);
            check.accept(dismiss(personal,"+12025550101").equals("NOTIFICATION_KEPT"),"personal profile cannot authorize notification cancellation");
            var unknown=postAndClaim();check.accept(dismiss(unknown,"+12025550102").equals("NOTIFICATION_KEPT"),"unknown sender cannot authorize cancellation");
            observed("+12025550103",Kind.BUSINESS_CONFIRMED,BlockState.BLOCKED);
            var allowed=postAndClaim();var account=repository.current().accounts().stream().filter(a->a.phone().equals("+12025550103")).findFirst().orElseThrow();
            commit(done->repository.choose(account.id(),Choice.ALLOW,done));arm();
            check.accept(dismiss(allowed,account.phone()).equals("NOTIFICATION_KEPT"),"exact-number allow keeps a confirmed-business notification");
            commit(done->repository.choose(account.id(),Choice.DEFAULT,done));arm();
            nameEnabled(true);var named=postAndClaim();
            check.accept(dismiss(named,account.phone()).equals("NOTIFICATION_KEPT"),"enabled business name keeps its notification");
            commit(done->repository.choose(account.id(),Choice.DENY_MANUAL,done));arm();var denied=postAndClaim();
            check.accept(dismiss(denied,account.phone()).equals("NOTIFICATION_DISMISSAL_REQUESTED"),"exact-number deny takes precedence over the enabled name");
            until(()->denied.removedByGate);
            commit(done->repository.choose(account.id(),Choice.DEFAULT,done));nameEnabled(false);
            var noIdentity=postAndClaim();String[] unverified={null};main(()->unverified[0]=GateNotificationListener.dismiss(noIdentity,account.phone(),"Fixture business",repository.current().globalRevision(),repository.epoch()));
            check.accept(unverified[0].equals("NOTIFICATION_IDENTITY_UNVERIFIED"),"saved business state alone cannot bind an incoming notification");
            var expired=postAndClaim();long oldEpoch=repository.epoch();main(repository::emergencyStop);
            expired.opened=true;GateNotificationListener.bindIdentity(expired,account.phone(),"Fixture business",repository.current().binding().receiver());
            String[] stale={null};main(()->stale[0]=GateNotificationListener.dismiss(expired,account.phone(),"Fixture business",repository.current().globalRevision(),oldEpoch));
            check.accept(stale[0].equals("POLICY_CHANGED"),"revoked epoch prevents notification cancellation");
            arm();var target=postAndClaim();int targetId=next;var other=postAndClaim();int otherId=next;
            check.accept(!GateNotificationListener.valid(target),"new session revokes the preceding notification claim");
            main(()->GateNotificationListener.release(other));GateNotificationListener.Claim[] exact={null};main(()->exact[0]=GateNotificationListener.claim(candidate(targetId).id()));
            exact[0].opened=true;
            check.accept(dismiss(exact[0],account.phone()).equals("NOTIFICATION_DISMISSAL_REQUESTED"),"hidden business requests cancellation of one exact notification");
            until(()->exact[0].removedByGate);
            check.accept(GateNotificationListener.dismissalResult(exact[0]).equals("NOTIFICATION_DISMISSED"),"Android callback and active snapshot confirm cancellation");
            check.accept(java.util.Arrays.stream(manager.getActiveNotifications()).noneMatch(n->n.getId()==targetId),"selected framework notification removed");
            check.accept(java.util.Arrays.stream(manager.getActiveNotifications()).anyMatch(n->n.getId()==otherId),"unrelated framework notification retained");
            var revoked=postAndClaim();main(()->repository.setting("discovery",false));until(()->!repository.optionEnabled("discovery"));
            check.accept(!GateNotificationListener.valid(revoked),"discovery revocation immediately prevents action");
            main(()->check.accept(GateNotificationListener.pending().isEmpty(),"discovery revocation drops in-memory candidates"));main(activity::finish);
        }finally{
            manager.cancelAll();manager.deleteNotificationChannel(channel);shell("cmd notification disallow_listener "+component);
            main(()->{try{rows.put(0,original);}catch(org.json.JSONException error){throw new IllegalStateException(error);}});
            check.accept(app.registry().measuredRoute(context,pkg)==null,"production compatibility restored after the fixture");
        }
    }
    private void routeReceipts()throws Exception{
        java.util.List<String> failures=new java.util.ArrayList<>();
        var route=new io.github.appunnim.businessgate.connected.BusinessProfileRoute(new io.github.appunnim.businessgate.connected.BusinessProfileRoute.Host(){
            public String selectedPackage(){return pkg;} public boolean active(){return true;}
            public boolean authorized(io.github.appunnim.businessgate.connected.BusinessProfileRoute.Profile p,io.github.appunnim.businessgate.connected.BusinessProfileRoute.Action a){throw new AssertionError("No native action in event fixture");}
            public boolean prepared(io.github.appunnim.businessgate.connected.BusinessProfileRoute.Profile p,io.github.appunnim.businessgate.connected.BusinessProfileRoute.Action a,io.github.appunnim.businessgate.connected.BusinessProfileRoute.Step s){throw new AssertionError("No native action in event fixture");}
            public android.view.accessibility.AccessibilityNodeInfo root(){throw new AssertionError("No screen access in event fixture");}
            public java.util.List<android.view.accessibility.AccessibilityWindowInfo> windows(){throw new AssertionError("No screen access in event fixture");}
            public void later(Runnable r,long delay){throw new AssertionError("No dispatch in event fixture");}
            public void completed(io.github.appunnim.businessgate.connected.BusinessProfileRoute.Result result){throw new AssertionError("No native result in event fixture");}
            public void failed(io.github.appunnim.businessgate.connected.BusinessProfileRoute.Failure failure){failures.add(failure.reason());}
        },"+12025550199",io.github.appunnim.businessgate.connected.BusinessProfileRoute.Action.BLOCK);
        var field=route.getClass().getDeclaredField("clicks");field.setAccessible(true);var queue=(java.util.ArrayDeque<Object>)field.get(route);
        var constructor=Class.forName(route.getClass().getName()+"$Click").getDeclaredConstructors()[0];constructor.setAccessible(true);
        Activity activity=test.startActivitySync(new Intent(context,io.github.appunnim.businessgate.ui.MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));until(activity::hasWindowFocus);
        var first=new android.widget.Button(activity);var second=new android.widget.RadioButton(activity);
        main(()->{var layout=new android.widget.LinearLayout(activity);layout.setOrientation(1);first.setId(android.R.id.button1);first.setText("First fixture");first.setOnClickListener(v->{});second.setId(android.R.id.button2);second.setText("Second fixture");second.setOnClickListener(v->{});layout.addView(first);layout.addView(second);activity.setContentView(layout);});
        until(()->first.isShown()&&first.isLaidOut()&&second.isShown()&&second.isLaidOut());
        var automation=test.getUiAutomation(android.app.UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES);
        var originalInfo=automation.getServiceInfo();int originalFlags=originalInfo.flags;originalInfo.flags|=android.accessibilityservice.AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;automation.setServiceInfo(originalInfo);
        until(()->{
            var root=automation.getRootInActiveWindow();if(root==null)return false;
            var nodes=root.findAccessibilityNodeInfosByViewId("android:id/button1");
            try{return nodes.size()==1&&pkg.contentEquals(nodes.get(0).getPackageName());}
            finally{for(var node:nodes)node.recycle();root.recycle();}
        });
        java.util.List<android.view.accessibility.AccessibilityNodeInfo> sources=new java.util.ArrayList<>();
        android.view.accessibility.AccessibilityEvent one=null,two=null;
        try{
            one=fixtureClick(automation,"android:id/button1","android.widget.Button",sources);
            two=fixtureClick(automation,"android:id/button2","android.widget.RadioButton",sources);
            int index=0;
            for(var event:java.util.List.of(one,two)){
                var source=sources.get(index++);
                queue.add(constructor.newInstance(new io.github.appunnim.businessgate.connected.ActionEcho(pkg,event.getWindowId(),String.valueOf(event.getClassName()),source.getViewIdResourceName(),event.getEventTime()-1),source));
            }
            sources.clear();
            route.event(two);check.accept(failures.isEmpty()&&queue.size()==1,"later native click consumes its own receipt when an earlier event is delayed");
            route.event(one);check.accept(failures.isEmpty()&&queue.isEmpty(),"delayed earlier click consumes only its remaining exact receipt");
            route.event(one);check.accept(failures.size()==1&&failures.get(0).equals("INTERACTION_CHANGED_NO_RECEIPT"),"consumed native receipt cannot authorize a duplicate click");
        }finally{if(one!=null)one.recycle();if(two!=null)two.recycle();route.stop();for(var source:sources)source.recycle();originalInfo.flags=originalFlags;automation.setServiceInfo(originalInfo);main(activity::finish);}
    }
    private android.view.accessibility.AccessibilityEvent fixtureClick(android.app.UiAutomation automation,String resource,String type,java.util.List<android.view.accessibility.AccessibilityNodeInfo> retained)throws Exception{
        return automation.executeAndWaitForEvent(()->{
            var root=automation.getRootInActiveWindow();if(root==null)throw new AssertionError("Owned fixture root missing");
            var nodes=root.findAccessibilityNodeInfosByViewId(resource);
            try{
                if(nodes.size()!=1||!pkg.contentEquals(nodes.get(0).getPackageName()))throw new AssertionError("Owned fixture control missing");
                retained.add(android.view.accessibility.AccessibilityNodeInfo.obtain(nodes.get(0)));
                if(!nodes.get(0).performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK))throw new AssertionError("Owned fixture action rejected");
            }finally{for(var node:nodes)node.recycle();root.recycle();}
        },e->e.getEventType()==android.view.accessibility.AccessibilityEvent.TYPE_VIEW_CLICKED&&pkg.contentEquals(e.getPackageName())&&type.contentEquals(e.getClassName()),3000);
    }
}
