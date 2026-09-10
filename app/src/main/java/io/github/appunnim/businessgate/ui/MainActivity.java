package io.github.appunnim.businessgate.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.Switch;
import android.widget.TextView;
import io.github.appunnim.businessgate.BuildConfig;
import io.github.appunnim.businessgate.GateApplication;
import io.github.appunnim.businessgate.R;
import io.github.appunnim.businessgate.data.GateRepository;
import io.github.appunnim.businessgate.policy.Identity;
import io.github.appunnim.businessgate.policy.Model.*;
import io.github.appunnim.businessgate.policy.SalesHintEngine;
import io.github.appunnim.businessgate.service.GateAccessibilityService;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** The sole management screen. All mutations are emitted as repository commands. */
@SuppressLint("SetTextI18n") // English-first composed state labels; counts use Android plurals.
public final class MainActivity extends Activity {
    private GateRepository repository;
    private GateApplication app;
    private EditText search;
    private TextView status;
    private Button pause,clear,batch;
    private ListView list;
    private final Rows adapter=new Rows();
    private final Handler main=new Handler(Looper.getMainLooper());
    private final Runnable changed=this::refresh;
    private int queryGeneration;
    private long expanded=-1,renderNamespace=-1;
    private boolean reviewExpanded,peopleExpanded,started;
    private List<Account> matches=java.util.Collections.emptyList();
    private final List<Row> rows=new ArrayList<>();
    private final List<Runnable> settingsBindings=new ArrayList<>();
    private int savedPosition,savedTop,preSearchPosition,preSearchTop;
    private record Row(long id,String type,String title,Account account){}
    @Override public void onCreate(Bundle state){
        super.onCreate(state);app=(GateApplication)getApplication();repository=app.repository();
        if(state!=null){expanded=state.getLong("expanded",-1);reviewExpanded=state.getBoolean("review");peopleExpanded=state.getBoolean("people");savedPosition=state.getInt("position");savedTop=state.getInt("top");}
        FrameLayout root=new FrameLayout(this);root.setBackgroundColor(getColor(R.color.background));
        LinearLayout column=Ui.column(this);int width=Math.min(getResources().getDisplayMetrics().widthPixels,Ui.dp(this,600));
        FrameLayout.LayoutParams columnParams=new FrameLayout.LayoutParams(width,ViewGroup.LayoutParams.MATCH_PARENT,Gravity.CENTER_HORIZONTAL);root.addView(column,columnParams);
        root.setOnApplyWindowInsetsListener((v,insets)->{
            if(Build.VERSION.SDK_INT>=30){
                android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout()|WindowInsets.Type.ime());
                v.setPadding(bars.left,bars.top,bars.right,bars.bottom);
            }else{
                v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
            }
            return insets;
        });
        LinearLayout toolbar=Ui.row(this);Ui.pad(toolbar,16,4);toolbar.setMinimumHeight(Ui.dp(this,56));
        TextView title=Ui.text(this,"Business Gate",22,R.color.ink,true);title.setAccessibilityHeading(true);toolbar.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        pause=Ui.button(this,"Resume",false,this::toggleRule);toolbar.addView(pause);
        Button more=Ui.button(this,"⋮",false,()->{});more.setTextSize(24);more.setContentDescription("More options");more.setOnClickListener(this::overflow);toolbar.addView(more,new LinearLayout.LayoutParams(Ui.dp(this,48),-2));
        boolean largeText=getResources().getConfiguration().fontScale>=1.5f;
        if(largeText){
            toolbar.removeAllViews();toolbar.setOrientation(LinearLayout.VERTICAL);toolbar.setGravity(Gravity.START);
            toolbar.addView(title,new LinearLayout.LayoutParams(-1,-2));
            LinearLayout actions=Ui.row(this);actions.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);actions.addView(pause);actions.addView(more,new LinearLayout.LayoutParams(Ui.dp(this,48),-2));
            toolbar.addView(actions,new LinearLayout.LayoutParams(-1,-2));
        }
        column.addView(toolbar);
        status=Ui.text(this,"Loading your choices…",13,R.color.muted,false);Ui.pad(status,16,4);status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);column.addView(status);
        LinearLayout searchRow=Ui.row(this);searchRow.setBackground(Ui.shape(this,R.color.surface,true));
        LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2);sp.setMargins(Ui.dp(this,16),Ui.dp(this,8),Ui.dp(this,16),Ui.dp(this,12));column.addView(searchRow,sp);
        search=new EditText(this);search.setId(R.id.account_search);search.setSingleLine(true);search.setTextSize(15);search.setHint(largeText?"Search":"Search name or number");search.setContentDescription("Search local accounts by name or number");
        search.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);search.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        search.setTextColor(getColor(R.color.ink));search.setHintTextColor(getColor(R.color.muted));search.setBackground(null);Ui.pad(search,14,10);search.setMinHeight(Ui.dp(this,48));
        searchRow.addView(search,new LinearLayout.LayoutParams(0,-2,1));clear=Ui.button(this,"×",false,()->search.setText(""));clear.setContentDescription("Clear search");clear.setVisibility(View.GONE);searchRow.addView(clear,new LinearLayout.LayoutParams(Ui.dp(this,48),-2));
        list=new ListView(this);list.setId(R.id.account_list);list.setDivider(null);list.setClipToPadding(false);list.setPadding(0,0,0,Ui.dp(this,20));list.setAdapter(adapter);list.setItemsCanFocus(true);column.addView(list,new LinearLayout.LayoutParams(-1,0,1));
        batch=Ui.button(this,"Apply pending",true,this::applyPending);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,-2);bp.setMargins(Ui.dp(this,16),Ui.dp(this,8),Ui.dp(this,16),Ui.dp(this,12));column.addView(batch,bp);batch.setVisibility(View.GONE);
        setContentView(root);root.requestApplyInsets();
        if(Build.VERSION.SDK_INT>=33)getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,this::handleBack);
        search.addTextChangedListener(new TextWatcher(){
            @Override public void beforeTextChanged(CharSequence s,int start,int count,int after){if(s.length()==0&&after>0){preSearchPosition=list.getFirstVisiblePosition();preSearchTop=list.getChildCount()>0?list.getChildAt(0).getTop():0;}}
            @Override public void onTextChanged(CharSequence s,int start,int before,int count){
                queryGeneration++;
                clear.setVisibility(s.length()==0?View.GONE:View.VISIBLE);main.removeCallbacks(searchTask);main.postDelayed(searchTask,150);
                if(s.length()==0)list.post(()->list.setSelectionFromTop(preSearchPosition,preSearchTop));
            }
            @Override public void afterTextChanged(Editable value){}
        });
        if(state!=null)search.setText(state.getString("query",""));
    }
    private final Runnable searchTask=this::refresh;
    // API 29-32 fallback only; API 33+ registers the native dispatcher in onCreate.
    @SuppressLint("GestureBackNavigation")
    @Override public void onBackPressed(){handleBack();}
    private void handleBack(){
        if(Build.VERSION.SDK_INT>=30&&search.getRootWindowInsets()!=null&&search.getRootWindowInsets().isVisible(WindowInsets.Type.ime())){
            getSystemService(InputMethodManager.class).hideSoftInputFromWindow(search.getWindowToken(),0);return;
        }
        if(expanded!=-1){expanded=-1;render();return;}
        finish();
    }
    @Override protected void onStart(){super.onStart();started=true;repository.addListener(changed);refresh();}
    @Override protected void onResume(){super.onResume();app.managementAttention(true);refresh();}
    @Override protected void onPause(){app.managementAttention(false);super.onPause();}
    @Override protected void onStop(){started=false;queryGeneration++;repository.removeListener(changed);main.removeCallbacks(searchTask);super.onStop();}
    @Override protected void onSaveInstanceState(Bundle out){
        out.putString("query",search.getText().toString());out.putLong("expanded",expanded);out.putBoolean("review",reviewExpanded);out.putBoolean("people",peopleExpanded);
        out.putInt("position",list.getFirstVisiblePosition());out.putInt("top",list.getChildCount()>0?list.getChildAt(0).getTop():0);super.onSaveInstanceState(out);
    }
    private void refresh(){
        if(!started)return;for(Runnable binding:settingsBindings)binding.run();Snapshot s=repository.current();
        if(renderNamespace!=s.namespace()){renderNamespace=s.namespace();matches=java.util.Collections.emptyList();expanded=-1;render();}
        String text;
        if(!s.error().isEmpty())text=s.error();
        else if(!s.loaded())text="Loading your choices…";
        else if(!s.consent())text="Choose the businesses you want to hear from.";
        else if(!app.registry().available())text="Paused · compatibility check needed";
        else if(!GateAccessibilityService.connected())text="Blocking paused · screen access is off";
        else if(s.circuitOpen())text="Blocking paused · compatibility check required";
        else if(repository.disarmed()||s.paused()||!s.enabled())text="Rule off · no new blocks will run";
        else text=s.pending()>0?"Rule on · "+getResources().getQuantityString(R.plurals.waiting_actions,s.pending(),s.pending()):"Rule on · no action needed";
        if(!status.getText().toString().equals(text))status.setText(text);
        pause.setText(!repository.disarmed()&&s.enabled()&&!s.paused()?"Pause":"Resume");
        batch.setVisibility(s.pending()>0?View.VISIBLE:View.GONE);batch.setText(getResources().getQuantityString(R.plurals.pending_actions,s.pending(),s.pending()));
        int generation=++queryGeneration;
        repository.search(search.getText().toString(),result->{if(!started||generation!=queryGeneration)return;matches=result;render();});
    }
    private void render(){
        long anchor=list.getFirstVisiblePosition()<rows.size()?rows.get(list.getFirstVisiblePosition()).id():Long.MIN_VALUE;
        int top=list.getChildCount()>0?list.getChildAt(0).getTop():0;
        rows.clear();Snapshot s=repository.current();boolean searching=!search.getText().toString().trim().isEmpty();
        if(!searching){
            if(!s.consent())rows.add(new Row(-1,"setup","",null));
            else if(!app.registry().available()||!GateAccessibilityService.connected())rows.add(new Row(-2,"compatibility","",null));
        }
        List<Account> enabled=matches.stream().filter(Account::businessRow).filter(a->a.choice()==Choice.ALLOW).collect(java.util.stream.Collectors.toList());
        List<Account> denied=matches.stream().filter(Account::businessRow).filter(a->a.choice()!=Choice.ALLOW).collect(java.util.stream.Collectors.toList());
        group(-10,"ENABLED BY YOU",enabled);group(-11,"NOT ENABLED",denied);
        List<Account> review=matches.stream().filter(a->!a.businessRow()&&a.choice()==Choice.DEFAULT&&a.review()!=Review.NONE).collect(java.util.stream.Collectors.toList());
        if(!review.isEmpty()){
            rows.add(new Row(-12,"reviewHeader",getResources().getQuantityString(R.plurals.review_count,review.size(),review.size()),null));
            if(reviewExpanded||searching)for(Account a:review)rows.add(new Row(a.id(),"review","",a));
        }
        List<Account> people=matches.stream().filter(a->!a.businessRow()&&(a.choice()==Choice.ALLOW||a.review()==Review.NONE)).collect(java.util.stream.Collectors.toList());
        if(!people.isEmpty()){
            rows.add(new Row(-13,"peopleHeader",getResources().getQuantityString(R.plurals.people_count,people.size(),people.size()),null));
            if(peopleExpanded||searching)for(Account a:people)rows.add(new Row(a.id(),"person","",a));
        }
        if(matches.isEmpty())rows.add(new Row(-14,"empty",searching?"No matching accounts":"No businesses found yet",null));
        rows.add(new Row(-15,"footer","Local by design. Personal and uncertain accounts are not automatically blocked.",null));
        adapter.notifyDataSetChanged();
        for(int i=0;i<rows.size();i++)if(rows.get(i).id()==anchor){list.setSelectionFromTop(i,top);break;}
        if(savedPosition>0){list.setSelectionFromTop(savedPosition,savedTop);savedPosition=0;}
    }
    private void group(long id,String title,List<Account> accounts){if(!accounts.isEmpty()){rows.add(new Row(id,"section",title+" · "+accounts.size(),null));for(Account a:accounts)rows.add(new Row(a.id(),"account","",a));}}
    private void overflow(View anchor){
        PopupMenu menu=new PopupMenu(this,anchor);
        String[] items={"Enable a number","Settings & privacy","Compatibility & help","Clear local data","Select connected installation","Review receiving account","Manage unconnected choices"};
        for(int i=0;i<items.length;i++)menu.getMenu().add(0,i,i,items[i]);
        menu.setOnMenuItemClickListener(item->{switch(item.getItemId()){case 0->addNumber();case 1->settings();case 2->compatibility();case 3->clearLocal();case 4->selectInstallation();case 5->reviewReceiver();case 6->{GateAccessibilityService.stopNow();repository.localChoices(()->announce("Managing unconnected choices. Nothing is applied to a receiving account."));}default->{}}return true;});menu.show();
    }
    private void addNumber(){
        LinearLayout fields=Ui.column(this);Ui.pad(fields,24,4);
        TextView explanation=Ui.text(this,"This permission belongs to one exact number. A new number needs its own permission.",14,R.color.muted,false);fields.addView(explanation);
        EditText phone=new EditText(this);phone.setHint("Full number, for example +1 202 555 0101");phone.setContentDescription("Full phone number with country code");phone.setInputType(InputType.TYPE_CLASS_PHONE);phone.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);phone.setMinHeight(Ui.dp(this,56));fields.addView(phone);
        EditText name=new EditText(this);name.setHint("Name (optional)");name.setContentDescription("Optional local name");name.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_WORDS);name.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);name.setMinHeight(Ui.dp(this,56));fields.addView(name);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Enable a number").setView(Ui.scroll(this,fields)).setNegativeButton("Cancel",null).setPositiveButton("Enable",null).create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            try{Identity.canonicalPhone(phone.getText().toString());repository.enableNumber(phone.getText().toString(),name.getText().toString(),()->announce("Number enabled. Waiting for a supported account check."));dialog.dismiss();}
            catch(IllegalArgumentException invalid){phone.setError("Enter the full number with country code");phone.requestFocus();}
        }));dialog.show();
    }
    private void accessDisclosure(){new AlertDialog.Builder(this).setTitle("Screen access, explained").setMessage(R.string.access_disclosure)
        .setNegativeButton("Not now",null).setPositiveButton("Agree and open settings",(d,w)->repository.updateSetup("CAPABILITY",true,()->openSettings(Settings.ACTION_ACCESSIBILITY_SETTINGS))).show();}
    private void notificationDisclosure(){new AlertDialog.Builder(this).setTitle("Notice new senders sooner").setMessage(R.string.notification_disclosure)
        .setNegativeButton("Skip",null).setPositiveButton("Agree and open settings",(d,w)->{repository.setting("discovery",true);openSettings(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);}).show();}
    private void salesDisclosure(){new AlertDialog.Builder(this).setTitle("Optional sales hints").setMessage(R.string.sales_disclosure)
        .setNegativeButton("Keep off",null).setPositiveButton("Enable hints",(d,w)->repository.setting("sales_hints",true)).show();}
    private void toggleRule(){
        Snapshot s=repository.current();
        if(!repository.disarmed()&&s.enabled()&&!s.paused()){GateAccessibilityService.stopNow();repository.pause();return;}
        if(!s.consent()){accessDisclosure();return;}
        if(!app.registry().available()){compatibility();return;}
        if(!GateAccessibilityService.connected()){accessDisclosure();return;}
        if(!s.binding().bound()){reviewReceiver();return;}
        new AlertDialog.Builder(this).setTitle("Turn on your business rule?").setMessage(R.string.activation_disclosure)
            .setNegativeButton("Review choices",null).setPositiveButton("Turn rule on",(d,w)->startRequestedSession(false,true)).show();
        // Device/receiver readiness must be established by the qualification workflow.
    }
    private void applyPending(){
        if(!app.registry().available()){compatibility();return;}
        new AlertDialog.Builder(this).setTitle("Apply your choices").setMessage("Actions need a freshly verified receiving account and supported visible profile. Block actions require your business rule to be on; explicit unblock requests can be checked while it is paused. This version checks one visible profile per session, for up to 25 seconds. Other pending choices stay saved.")
            .setNegativeButton("Close",null).setPositiveButton("Start visible session",(d,w)->startRequestedSession()).show();
    }
    private void retryCheck(Account account){
        if(!app.registry().available()){compatibility();return;}
        new AlertDialog.Builder(this).setTitle("Check this number again?").setMessage(account.phone()+"\n\nKeep your saved choice and start a new visible check. A fresh account check is required before any action. An expired unblock request needs Unblock now.")
            .setNegativeButton("Cancel",null).setPositiveButton("Start visible check",(d,w)->{
                GateAccessibilityService.stopNow();repository.retryCheck(account,result->{
                    switch(result){
                        case QUEUED -> startRequestedSession(false,false,account.id());
                        case NO_AUTHORITY -> announce("No current action is authorized. Use Unblock now for a new unblock request.");
                        case STALE -> announce("This choice changed. Review the current number before trying again.");
                        case SAVE_FAILED -> announce("Could not save the retry. Actions remain stopped.");
                    }
                });
            }).show();
    }
    private void selectInstallation(){
        if(!app.registry().available()){compatibility();return;}
        if(!GateAccessibilityService.connected()){accessDisclosure();return;}
        Intent base=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        Intent picker=new Intent(Intent.ACTION_PICK_ACTIVITY).putExtra(Intent.EXTRA_INTENT,base).putExtra(Intent.EXTRA_TITLE,"Select connected installation");
        try{startActivityForResult(picker,71);}catch(android.content.ActivityNotFoundException error){announce("The system app selector is unavailable.");}
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request!=71||result!=RESULT_OK||data==null||data.getComponent()==null)return;
        if(!GateAccessibilityService.selectInstallation(data.getComponent().getPackageName())){announce("This installation or device is not qualified.");return;}
        Intent launcher=getPackageManager().getLaunchIntentForPackage(data.getComponent().getPackageName());
        if(launcher==null||launcher.getComponent()==null){announce("The selected installation has no available launcher.");return;}
        Intent open=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(launcher.getComponent());
        new AlertDialog.Builder(this).setTitle("Verify the receiving account").setMessage("Open a supported account profile, then return here and choose Review receiving account. Only a measured full receiving-account identity can connect your choices.")
            .setNegativeButton("Later",null).setPositiveButton("Open selected app",(d,w)->{try{startActivity(open);}catch(android.content.ActivityNotFoundException error){announce("The selected installation is no longer available.");}}).show();
    }
    private void reviewReceiver(){
        if(!app.registry().available()){compatibility();return;}
        Binding candidate=GateAccessibilityService.connectionCandidate();
        if(candidate==null){announce("Select the installation and open a supported profile to verify its receiving account.");return;}
        new AlertDialog.Builder(this).setTitle("Connect this receiving account?").setMessage("Receiving account: "+candidate.receiver()+"\n\nChoices for other receiving accounts stay separate. Unconnected choices are not transferred.\n\n"+getString(R.string.access_disclosure))
            .setNegativeButton("Cancel",null).setPositiveButton("Agree and connect",(d,w)->{
                if(!candidate.equals(GateAccessibilityService.connectionCandidate())){announce("The account check expired. Open the supported profile again.");return;}
                repository.bindReceiver(candidate,()->repository.updateSetup("REVIEW",true,()->announce("Receiving account connected. Review its exact-number choices before starting.")));
            }).show();
    }
    private void startRequestedSession(){
        startRequestedSession(false);
    }
    private void startRequestedSession(boolean compatibilityCheck){
        startRequestedSession(compatibilityCheck,false);
    }
    private void startRequestedSession(boolean compatibilityCheck,boolean activateRule){
        startRequestedSession(compatibilityCheck,activateRule,-1);
    }
    private void startRequestedSession(boolean compatibilityCheck,boolean activateRule,long accountId){
        if(!repository.current().binding().bound()){reviewReceiver();return;}
        if(!(compatibilityCheck?GateAccessibilityService.requestCompatibilityCheck():activateRule?GateAccessibilityService.requestActivation():GateAccessibilityService.requestApply(accountId))){compatibility();return;}
        Intent open=getPackageManager().getLaunchIntentForPackage(GateAccessibilityService.packageSelected());
        if(open==null){GateAccessibilityService.stopNow();announce("Select the connected installation again.");return;}
        try{startActivity(open);}catch(android.content.ActivityNotFoundException error){GateAccessibilityService.stopNow();announce("The selected installation is unavailable.");}
    }
    private void compatibility(){
        LinearLayout box=Ui.column(this);Ui.pad(box,24,8);
        box.addView(Ui.text(this,"Your choices are safe here.",18,R.color.ink,true));Ui.gap(box,8);
        box.addView(Ui.text(this,app.registry().summary(),14,R.color.muted,false));Ui.gap(box,12);
        box.addView(Ui.text(this,"This build needs measured screen controls, exact receiver binding and physical block/unblock verification before connecting. The first message may arrive. Existing blocks stay as they are.",14,R.color.muted,false));Ui.gap(box,12);
        box.addView(Ui.text(this,"Screen access: "+(GateAccessibilityService.connected()?"connected":"off")+"\nAndroid API: "+Build.VERSION.SDK_INT+"\nApp version: "+BuildConfig.VERSION_NAME,13,R.color.muted,false));
        if(app.registry().available()&&repository.current().binding().bound())box.addView(Ui.button(this,"Check visible profile",false,()->startRequestedSession(true)));
        box.addView(Ui.button(this,"About visible scans",false,()->new AlertDialog.Builder(this).setTitle("Visible scan disclosure").setMessage(R.string.scan_disclosure).setPositiveButton("Understood",null).show()));
        new AlertDialog.Builder(this).setTitle("Compatibility & help").setView(Ui.scroll(this,box)).setNegativeButton("Close",null).setPositiveButton("View diagnostics",(d,w)->diagnostics()).show();
    }
    private void diagnostics(){
        Snapshot s=repository.current();String diagnostic="Business Gate "+BuildConfig.VERSION_NAME+"\nAndroid API: "+Build.VERSION.SDK_INT+"\nQualified integration: "+app.registry().available()+"\nScreen access connected: "+GateAccessibilityService.connected()+"\nScreen consent: "+s.consent()+"\nRule enabled: "+s.enabled()+"\nPaused: "+s.paused()+"\nPending choices: "+s.pending()+"\nReason: "+(app.registry().available()?GateAccessibilityService.status():"QUALIFICATION_REQUIRED");
        new AlertDialog.Builder(this).setTitle("Local diagnostics").setMessage(diagnostic).setNegativeButton("Close",null).setPositiveButton("Copy diagnostics",(d,w)->{
            getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText("Business Gate diagnostics",diagnostic));announce("Diagnostics copied. No names or numbers included.");}).show();
    }
    private void settings(){
        Snapshot s=repository.current();settingsBindings.clear();LinearLayout box=Ui.column(this);Ui.pad(box,24,0);
        box.addView(Ui.text(this,"LOCAL BY DESIGN",12,R.color.accent,true));Ui.gap(box,8);
        box.addView(Ui.text(this,"Exact numbers, optional names and your choices stay in this phone’s private storage. No account, ads, subscription, analytics or network permission. Chat contents and notification messages are not saved. Backup and device transfer are excluded.",14,R.color.muted,false));Ui.gap(box,12);
        box.addView(settingSwitch("Notification-assisted discovery",()->repository.current().discovery(),value->{if(value)notificationDisclosure();else repository.setting("discovery",false);}));
        box.addView(settingSwitch("Optional sales hints",()->repository.current().salesHints(),value->{if(value)salesDisclosure();else repository.setting("sales_hints",false);}));
        box.addView(Ui.text(this,"Hints are off by default. They can be wrong and never authorize a block. Notification text analysis stays unavailable until a notification binding is qualified.",12,R.color.muted,false));Ui.gap(box,8);
        box.addView(settingSwitch("Quiet review reminder",()->repository.current().digest(),value->{repository.setting("digest",value);if(value&&Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},70);}));
        box.addView(Ui.text(this,"At most one useful, silent reminder per 30 days. No daily summaries or success notifications.",12,R.color.muted,false));Ui.gap(box,8);
        box.addView(Ui.button(this,"Screen access settings",false,()->openSettings(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        box.addView(Ui.button(this,"Notification access settings",false,()->openSettings(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        box.addView(Ui.button(this,"View local effort",false,()->repository.effortSummary(text->new AlertDialog.Builder(this).setTitle("Observed effort").setMessage(text).setPositiveButton("Done",null).show())));
        box.addView(Ui.button(this,"Withdraw screen consent",false,()->{GateAccessibilityService.stopNow();repository.emergencyStop();repository.updateSetup("WELCOME",false,()->announce("Consent withdrawn. Actions stopped."));}));
        android.widget.ScrollView scroll=new android.widget.ScrollView(this);scroll.addView(box);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Settings & privacy").setView(scroll).setPositiveButton("Done",null).create();dialog.setOnDismissListener(d->settingsBindings.clear());dialog.show();
    }
    private View settingSwitch(String label,java.util.function.BooleanSupplier current,java.util.function.Consumer<Boolean> change){
        Switch control=new Switch(this);control.setText(label);control.setTextSize(14);control.setMinHeight(Ui.dp(this,56));control.setChecked(current.getAsBoolean());control.setSwitchPadding(Ui.dp(this,16));
        android.widget.CompoundButton.OnCheckedChangeListener[] listener=new android.widget.CompoundButton.OnCheckedChangeListener[1];
        Runnable sync=()->{control.setOnCheckedChangeListener(null);control.setChecked(current.getAsBoolean());control.setOnCheckedChangeListener(listener[0]);};
        listener[0]=(button,value)->{sync.run();change.accept(value);};
        control.setOnCheckedChangeListener(listener[0]);settingsBindings.add(sync);return control;
    }
    private void clearLocal(){AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Delete local choices and history?").setMessage("This stops future automation and removes local choices, observations, consent and effort totals. It does not unblock anyone in the connected app.")
        .setNegativeButton("Cancel",null).setPositiveButton("Delete local data",(d,w)->{GateAccessibilityService.stopNow();repository.reset(()->{search.setText("");expanded=-1;announce("Local data deleted. Existing blocks stay.");});}).show();dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getColor(R.color.danger));}
    private void openSettings(String action){try{startActivity(new Intent(action));}catch(android.content.ActivityNotFoundException error){announce("This settings screen is unavailable on this device.");}}
    private void announce(String message){status.setText(message);}
    private void choose(Account a,Choice choice){repository.choose(a.id(),choice,()->announce(choice==Choice.ALLOW?"Kept. This number will not be auto-blocked.":"Choice saved. Waiting for a supported account check."));}
    private void unblockNow(Account a){
        repository.choose(a.id(),Choice.ALLOW,()->{
            if(app.registry().available()&&GateAccessibilityService.connected()&&repository.current().binding().bound())startRequestedSession(false,false,a.id());
            else announce("Unblock request saved. A supported visible account check is still required.");
        });
    }
    private void manualBlock(Account a){
        new AlertDialog.Builder(this).setTitle("Block this number?").setMessage("This account has not been identified as a business.\n\n"+a.phone()+"\n\nBlock only this number because you chose it. Future messages and calls may be stopped. Another number will need its own check.")
            .setNegativeButton("Cancel",null).setPositiveButton("Block number",(d,w)->choose(a,Choice.DENY_MANUAL)).show();
    }
    private String subtitle(Account a){
        if(a.jobState()==JobState.FAILED)return "Action failed · check needed";
        if(a.pending()){
            if(a.jobState()==JobState.REINSPECT||a.jobState()==JobState.VERIFYING||a.jobState()==JobState.ACTION_INTENT)return "Action result not verified · check needed";
            return a.jobAction()==Action.UNBLOCK?"Enabled · unblocking pending":a.choice()==Choice.DENY_MANUAL?"Your block is pending":"Block pending";
        }
        if(a.choice()==Choice.ALLOW&&a.blockState()==BlockState.BLOCKED)return "Enabled here; blocked in connected app";
        if(a.blockState()==BlockState.BLOCKED)return "Blocked · last checked "+relative(a.checkedAt());
        if(a.choice()==Choice.ALLOW)return a.blockState()==BlockState.UNBLOCKED?"Enabled · unblocked":"Enabled · not checked yet";
        if(a.kind()==Kind.REGULAR_PROFILE_OBSERVED)return "Not subject to automatic blocking";
        return "Block state not verified";
    }
    private String relative(long at){if(at<=0)return "unknown";long age=System.currentTimeMillis()-at;if(age<0)return "time changed";if(age<60000)return "just now";if(age<86400000)return "today";return DateFormat.getDateInstance(DateFormat.SHORT).format(new Date(at));}
    private final class Rows extends BaseAdapter{
        @Override public int getCount(){return rows.size();}@Override public Object getItem(int p){return rows.get(p);}@Override public long getItemId(int p){return rows.get(p).id();}
        @Override public boolean hasStableIds(){return true;}@Override public boolean isEnabled(int position){return false;}
        @Override public View getView(int position,View recycled,ViewGroup parent){
            Row r=rows.get(position);LinearLayout box=recycled instanceof LinearLayout?(LinearLayout)recycled:Ui.column(MainActivity.this);
            box.removeAllViews();box.setBackgroundColor(getColor(R.color.background));box.setPadding(0,0,0,0);box.setOnClickListener(null);box.setClickable(false);box.setContentDescription(null);box.setAccessibilityHeading(false);
            switch(r.type()){
                case "section"->{TextView t=Ui.text(MainActivity.this,r.title(),12,R.color.muted,true);Ui.pad(t,16,12);t.setAccessibilityHeading(true);box.addView(t);}
                case "setup"->setupCard(box);
                case "compatibility"->compatibilityCard(box);
                case "account","person","review"->accountRow(box,r);
                case "reviewHeader","peopleHeader"->{Button b=Ui.button(MainActivity.this,r.title()+"  "+((r.type().equals("reviewHeader")?reviewExpanded:peopleExpanded)?"−":"+"),false,()->{if(r.type().equals("reviewHeader"))reviewExpanded=!reviewExpanded;else peopleExpanded=!peopleExpanded;render();});b.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);b.setAccessibilityHeading(true);box.addView(b);}
                case "empty"->{Ui.pad(box,24,28);box.setGravity(Gravity.CENTER_HORIZONTAL);box.addView(Ui.text(MainActivity.this,r.title(),18,R.color.ink,true));Ui.gap(box,8);box.addView(Ui.text(MainActivity.this,search.getText().length()>0?"Try a business name or full phone number.":"Businesses appear after a supported account check. Add the numbers you already want to keep.",14,R.color.muted,false));Ui.gap(box,12);box.addView(Ui.button(MainActivity.this,"Enable a number",false,MainActivity.this::addNumber));}
                case "footer"->{TextView t=Ui.text(MainActivity.this,r.title(),12,R.color.muted,false);Ui.pad(t,16,20);box.addView(t);}
                default->throw new IllegalStateException("UNKNOWN_ROW");
            }
            return box;
        }
    }
    private LinearLayout card(LinearLayout parent,int background){
        LinearLayout c=Ui.column(this);Ui.pad(c,20,20);c.setBackground(Ui.shape(this,background,true));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(Ui.dp(this,16),Ui.dp(this,4),Ui.dp(this,16),Ui.dp(this,16));parent.addView(c,p);return c;
    }
    private void setupCard(LinearLayout parent){
        LinearLayout c=card(parent,R.color.surface);
        ImageView icon=new ImageView(this);icon.setImageResource(R.drawable.ic_gate);icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);c.addView(icon,new LinearLayout.LayoutParams(Ui.dp(this,44),Ui.dp(this,44)));Ui.gap(c,20);
        c.addView(Ui.text(this,"Choose the businesses.\nKeep the people.",24,R.color.ink,true));Ui.gap(c,12);
        c.addView(Ui.text(this,"Enable the numbers you want before the rule starts blocking other confirmed businesses.",14,R.color.muted,false));Ui.gap(c,16);
        setupStep(c,"1","Understand the access","Nothing runs without your consent.");setupStep(c,"2","Choose your exceptions","Each full number gets its own choice.");setupStep(c,"3","Turn on the rule","Personal and uncertain senders stay reachable.");Ui.gap(c,16);
        c.addView(Ui.button(this,"Set up screen access",true,this::accessDisclosure));Ui.gap(c,8);c.addView(Ui.button(this,"Choose numbers first",false,this::addNumber));Ui.gap(c,8);
        c.addView(Ui.text(this,"No account. No subscription. Local by design.\nConnected-app actions require a qualified integration.",12,R.color.muted,false));
    }
    private void setupStep(LinearLayout c,String number,String title,String detail){
        LinearLayout row=Ui.row(this);Ui.pad(row,0,8);TextView badge=Ui.text(this,number,14,R.color.accent,true);badge.setGravity(Gravity.CENTER);badge.setBackground(Ui.shape(this,R.color.accent_surface,false));row.addView(badge,new LinearLayout.LayoutParams(Ui.dp(this,32),Ui.dp(this,32)));
        LinearLayout body=Ui.column(this);Ui.pad(body,12,0);body.addView(Ui.text(this,title,14,R.color.ink,true));body.addView(Ui.text(this,detail,12,R.color.muted,false));row.addView(body,new LinearLayout.LayoutParams(0,-2,1));c.addView(row);
    }
    private void compatibilityCard(LinearLayout parent){
        LinearLayout c=card(parent,R.color.amber_surface);c.addView(Ui.text(this,"Your choices are saved.\nActions are paused.",19,R.color.amber,true));Ui.gap(c,8);
        c.addView(Ui.text(this,!app.registry().available()?"A supported integration is needed before Business Gate can apply your choices. Existing blocks stay as they are.":"Screen access is off. Your exact-number choices are still editable.",14,R.color.amber,false));Ui.gap(c,12);
        c.addView(Ui.button(this,"Check compatibility",false,this::compatibility));
    }
    private void accountRow(LinearLayout box,Row row){
        Account a=row.account();box.setBackgroundColor(getColor(R.color.surface));Ui.pad(box,16,12);box.setGravity(Gravity.START);
        LinearLayout header=Ui.row(this);header.setMinimumHeight(Ui.dp(this,64));
        String label=a.name().isEmpty()?a.phone():a.name();String initials=a.name().isEmpty()?"#":a.name().codePoints().limit(1).collect(StringBuilder::new,StringBuilder::appendCodePoint,StringBuilder::append).toString().toUpperCase(Locale.ROOT);
        TextView avatar=Ui.text(this,initials,14,R.color.muted,true);avatar.setGravity(Gravity.CENTER);avatar.setBackground(Ui.shape(this,R.color.background,false));avatar.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);header.addView(avatar,new LinearLayout.LayoutParams(Ui.dp(this,36),Ui.dp(this,36)));
        LinearLayout info=Ui.column(this);Ui.pad(info,12,0);info.setMinimumHeight(Ui.dp(this,64));info.setGravity(Gravity.CENTER_VERTICAL);
        info.addView(Ui.text(this,label,16,R.color.ink,true));TextView number=Ui.text(this,a.phone(),13,R.color.muted,false);number.setTextDirection(View.TEXT_DIRECTION_LTR);info.addView(number);
        String sub=row.type().equals("review")?(a.review()==Review.POSSIBLE_COMMERCIAL?"Possible business · not blocked":"New sender · not blocked"):subtitle(a);
        if(a.review()==Review.TYPE_CHANGED)sub="Account type changed · "+sub;
        info.addView(Ui.text(this,sub,12,row.type().equals("review")?R.color.amber:R.color.muted,false));
        info.setFocusable(true);info.setContentDescription(label+", "+a.phone()+", "+sub+". "+(expanded==a.id()?"Collapse details":"Show details"));info.setOnClickListener(v->{expanded=expanded==a.id()?-1:a.id();render();});header.addView(info,new LinearLayout.LayoutParams(0,-2,1));
        if(row.type().equals("account")){
            Switch sw=new Switch(this);sw.setOnCheckedChangeListener(null);sw.setChecked(a.choice()==Choice.ALLOW);sw.setMinHeight(Ui.dp(this,48));sw.setMinWidth(Ui.dp(this,48));sw.setShowText(false);
            sw.setContentDescription("Enable "+label+", "+a.phone()+". "+sub);sw.setOnCheckedChangeListener((button,on)->choose(a,on?Choice.ALLOW:Choice.DEFAULT));header.addView(sw,new LinearLayout.LayoutParams(Ui.dp(this,52),Ui.dp(this,48)));
        }
        box.addView(header);
        if(row.type().equals("review")){
            Ui.gap(box,8);box.addView(Ui.text(this,a.kind()==Kind.REGULAR_PROFILE_OBSERVED?"No business badge seen":"Account type not checked",13,R.color.muted,false));
            if(a.hintBits()!=0)box.addView(Ui.text(this,SalesHintEngine.reasons(a.hintBits()),13,R.color.amber,false));
            LinearLayout actions=Ui.row(this);actions.addView(Ui.button(this,"Keep",false,()->choose(a,Choice.ALLOW)),new LinearLayout.LayoutParams(0,-2,1));actions.addView(Ui.button(this,"Block",false,()->manualBlock(a)),new LinearLayout.LayoutParams(0,-2,1));box.addView(actions);
            box.addView(Ui.button(this,"Not now",false,()->{repository.dismiss(a.id());reviewExpanded=false;announce("Left alone. Review is optional.");}));
        }
        if(expanded==a.id()){
            Ui.gap(box,12);String checked=a.checkedAt()>0?DateFormat.getDateTimeInstance().format(new Date(a.checkedAt())):"Not checked";
            box.addView(Ui.text(this,"Only "+a.phone()+" follows this choice. A new number needs its own permission.\n\nLast account check: "+checked+"\n"+(a.kind()==Kind.BUSINESS_CONFIRMED?"Business account observed.":"No current business authority.")+"\nYour choice: "+(a.choice()==Choice.ALLOW?"keep enabled":a.choice()==Choice.DENY_MANUAL?"manually block this number":"apply the business rule")+".\n\nThe switch is your preference; a pending action is not proof of a completed block. Exact-number choices remain until you change them, even if a number changes owner.",13,R.color.muted,false));
            if(a.choice()==Choice.ALLOW&&a.blockState()!=BlockState.UNBLOCKED)box.addView(Ui.button(this,"Unblock now",false,()->unblockNow(a)));
            if(row.type().equals("person"))box.addView(Ui.button(this,a.choice()==Choice.ALLOW?"Kept · never auto-blocked":"Keep this number",false,()->choose(a,Choice.ALLOW)));
            if(a.pending()||a.jobState()==JobState.FAILED)box.addView(Ui.button(this,"Retry check",false,()->retryCheck(a)));
        }
        View divider=new View(this);divider.setBackgroundColor(getColor(R.color.line));LinearLayout.LayoutParams line=new LinearLayout.LayoutParams(-1,Ui.dp(this,1));line.topMargin=Ui.dp(this,12);box.addView(divider,line);
    }
}
