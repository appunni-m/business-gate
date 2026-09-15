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
import io.github.appunnim.businessgate.policy.PendingChoices.Pending;
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
    private TextView status,title;
    private Button pause,clear,batch;
    private ListView list;
    private RowFocus focus;
    private final List<AlertDialog> dialogs=new ArrayList<>();
    private AlertDialog numberDialog;
    private EditText numberPhone,numberName;
    private String dialogData="";
    private GateRepository.ChoiceScope numberScope;
    private record DialogScope(GateRepository.ChoiceScope choice,String data,long revision,long authority){}
    private final Rows adapter=new Rows();
    private final Handler main=new Handler(Looper.getMainLooper());
    private final Runnable changed=this::refresh;
    private int queryGeneration;
    private long expanded=-1,renderNamespace=-1;
    private boolean reviewExpanded,peopleExpanded,started,resumed,compact;
    private List<Account> matches=java.util.Collections.emptyList();
    private final List<Row> rows=new ArrayList<>();
    private final List<Runnable> settingsBindings=new ArrayList<>();
    private String renderDataIdentity="";
    private Bundle restoration;
    private Bundle numberDraft;
    private boolean restoring,matchesReady;
    private record Anchor(long id,String phone,int position,int top){}
    private static final Anchor START=new Anchor(Long.MIN_VALUE,"",0,0);
    private Anchor pendingAnchor,preSearchAnchor=START;
    private record Row(long id,String type,String title,Account account){}
    private record Presentation(Row row,boolean expanded,Pending pending,String data,long namespace,String observedAge){}
    private record AccountHeader(String data,long namespace,String phone,String type,LinearLayout header,LinearLayout info,TextView avatar,TextView name,TextView number,TextView subtitle,Switch toggle){
        boolean matches(String identity,long receiver,Row row){return data.equals(identity)&&namespace==receiver&&phone.equals(row.account().phone())&&type.equals(row.type());}
    }
    @Override public void onCreate(Bundle state){
        super.onCreate(state);app=(GateApplication)getApplication();repository=app.repository();
        boolean horizontal=getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE;
        compact=horizontal||getResources().getConfiguration().screenHeightDp<600;
        restoration=state==null?null:new Bundle(state);
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
        title=Ui.text(this,"Business Gate",22,R.color.ink,true);title.setAccessibilityHeading(true);if(!compact)toolbar.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        pause=Ui.button(this,"Resume",false,this::toggleRule);toolbar.addView(pause);
        Button more=Ui.button(this,"⋮",false,()->{});more.setTextSize(24);more.setContentDescription("More options");more.setOnClickListener(this::overflow);toolbar.addView(more,new LinearLayout.LayoutParams(Ui.dp(this,48),-2));
        boolean largeText=getResources().getConfiguration().fontScale>=1.5f;
        if(largeText&&!compact){
            toolbar.removeAllViews();toolbar.setOrientation(LinearLayout.VERTICAL);toolbar.setGravity(Gravity.START);
            toolbar.addView(title,new LinearLayout.LayoutParams(-1,-2));
            LinearLayout actions=Ui.row(this);actions.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);actions.addView(pause);actions.addView(more,new LinearLayout.LayoutParams(Ui.dp(this,48),-2));
            toolbar.addView(actions,new LinearLayout.LayoutParams(-1,-2));
        }
        if(compact&&!horizontal)toolbar.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);
        column.addView(toolbar);
        status=Ui.text(this,"Loading your choices…",13,R.color.muted,false);Ui.pad(status,16,4);if(!compact)column.addView(status);
        LinearLayout searchRow=Ui.row(this);searchRow.setBackground(Ui.shape(this,R.color.surface,true));
        if(horizontal){LinearLayout.LayoutParams compact=new LinearLayout.LayoutParams(0,-2,1);compact.setMarginEnd(Ui.dp(this,8));toolbar.addView(searchRow,0,compact);}
        else{LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2);sp.setMargins(Ui.dp(this,16),Ui.dp(this,8),Ui.dp(this,16),Ui.dp(this,12));column.addView(searchRow,sp);}
        search=new EditText(this);search.setId(R.id.account_search);search.setSingleLine(true);search.setSaveEnabled(false);search.setFilters(new android.text.InputFilter[]{(source,start,end,dest,dstart,dend)->{
            int remaining=128-Character.codePointCount(dest,0,dstart)-Character.codePointCount(dest,dend,dest.length());
            if(remaining<=0)return "";
            return Character.codePointCount(source,start,end)<=remaining?null:source.subSequence(start,Character.offsetByCodePoints(source,start,remaining));
        }});search.setTextSize(15);search.setHint(largeText?"Search":"Search name or number");search.setContentDescription("Search local accounts by name or number");
        search.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);search.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        search.setTextColor(getColor(R.color.ink));search.setHintTextColor(getColor(R.color.muted));search.setBackground(null);Ui.pad(search,14,10);search.setMinHeight(Ui.dp(this,48));
        searchRow.addView(search,new LinearLayout.LayoutParams(0,-2,1));clear=Ui.button(this,"×",false,()->search.setText(""));clear.setContentDescription("Clear search");clear.setVisibility(View.GONE);searchRow.addView(clear,new LinearLayout.LayoutParams(Ui.dp(this,48),-2));
        list=new ListView(this);list.setId(R.id.account_list);list.setSaveEnabled(false);list.setDivider(null);list.setClipToPadding(false);list.setPadding(0,0,0,Ui.dp(this,20));list.setAdapter(adapter);list.setItemsCanFocus(true);column.addView(list,new LinearLayout.LayoutParams(-1,0,1));
        batch=Ui.button(this,"Apply pending",true,this::applyPending);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,-2);bp.setMargins(Ui.dp(this,16),Ui.dp(this,8),Ui.dp(this,16),Ui.dp(this,12));column.addView(batch,bp);batch.setVisibility(View.GONE);
        root.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{
            int available=r-l-v.getPaddingLeft()-v.getPaddingRight();int bounded=Math.min(available,Ui.dp(this,600));
            if(bounded>0&&column.getLayoutParams().width!=bounded){ViewGroup.LayoutParams params=column.getLayoutParams();params.width=bounded;column.setLayoutParams(params);}
        });
        setContentView(root);root.requestApplyInsets();focus=new RowFocus(root,search,list);
        if(Build.VERSION.SDK_INT>=33)getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,this::handleBack);
        search.addTextChangedListener(new TextWatcher(){
            @Override public void beforeTextChanged(CharSequence s,int start,int count,int after){if(!restoring&&s.length()==0&&after>0)preSearchAnchor=captureAnchor();}
            @Override public void onTextChanged(CharSequence s,int start,int before,int count){
                queryGeneration++;matchesReady=false;
                if(!restoring)pendingAnchor=s.length()==0?preSearchAnchor:START;
                clear.setVisibility(s.length()==0?View.GONE:View.VISIBLE);main.removeCallbacks(searchTask);main.postDelayed(searchTask,150);
            }
            @Override public void afterTextChanged(Editable value){}
        });
    }
    private final Runnable searchTask=this::refresh;
    @Override public boolean dispatchKeyEvent(android.view.KeyEvent event){
        if(focus!=null&&event.getAction()==android.view.KeyEvent.ACTION_DOWN&&event.getKeyCode()==android.view.KeyEvent.KEYCODE_TAB
                &&(event.hasNoModifiers()||event.hasModifiers(android.view.KeyEvent.META_SHIFT_ON))&&focus.tab(event.isShiftPressed()))return true;
        return super.dispatchKeyEvent(event);
    }
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
    @Override protected void onStart(){super.onStart();started=true;repository.addListener(changed);app.senderFilter().addListener(changed);refresh();}
    @Override protected void onResume(){super.onResume();resumed=true;app.managementAttention(true);refresh();
        main.postDelayed(this::automaticCleanup,700);
        if(numberDraft!=null){Bundle draft=numberDraft;numberDraft=null;if(draft.getLong("namespace")==repository.current().namespace()&&draft.getString("dataIdentity","").equals(repository.dataIdentity()))addNumber(draft.getString("numberPhone",""),draft.getString("numberName",""));}
    }
    @Override public void onWindowFocusChanged(boolean focused){super.onWindowFocusChanged(focused);if(focused&&app!=null)main.postDelayed(this::automaticCleanup,250);}
    @Override protected void onPause(){resumed=false;app.managementAttention(false);super.onPause();}
    @Override public void onUserInteraction(){if(focus!=null)focus.cancel();super.onUserInteraction();}
    @Override protected void onStop(){numberDraft=captureNumberDraft();started=false;queryGeneration++;focus.cancel();dismissDialogs();repository.removeListener(changed);app.senderFilter().removeListener(changed);main.removeCallbacks(searchTask);super.onStop();}
    @Override protected void onSaveInstanceState(Bundle out){
        out.putString("query",search.getText().toString());out.putLong("expanded",expanded);out.putBoolean("review",reviewExpanded);out.putBoolean("people",peopleExpanded);
        out.putLong("namespace",renderNamespace);out.putString("dataIdentity",renderDataIdentity);
        Account detail=repository.current().account(expanded);out.putString("expandedPhone",detail==null?"":detail.phone());
        writeAnchor(out,"list",pendingAnchor==null?captureAnchor():pendingAnchor);writeAnchor(out,"preSearch",preSearchAnchor);
        Bundle draft=captureNumberDraft();if(draft==null)draft=numberDraft;
        if(draft!=null&&draft.getLong("namespace")==renderNamespace&&draft.getString("dataIdentity","").equals(renderDataIdentity)){
            out.putBoolean("numberForm",true);out.putString("numberPhone",draft.getString("numberPhone",""));out.putString("numberName",draft.getString("numberName",""));
        }
        super.onSaveInstanceState(out);
    }
    private static void writeAnchor(Bundle out,String key,Anchor anchor){
        out.putLong(key+"Id",anchor.id());out.putString(key+"Phone",anchor.phone());out.putInt(key+"Position",anchor.position());out.putInt(key+"Top",anchor.top());
    }
    private static Anchor readAnchor(Bundle state,String key){return new Anchor(state.getLong(key+"Id",Long.MIN_VALUE),state.getString(key+"Phone",""),Math.max(0,state.getInt(key+"Position")),state.getInt(key+"Top"));}
    private Anchor captureAnchor(){
        int position=list.getFirstVisiblePosition();Row row=position>=0&&position<rows.size()?rows.get(position):null;
        return new Anchor(row==null?Long.MIN_VALUE:row.id(),row==null||row.account()==null?"":row.account().phone(),Math.max(0,position),list.getChildCount()>0?list.getChildAt(0).getTop():0);
    }
    private boolean sameAnchor(Row row,Anchor anchor){return row.id()==anchor.id()&&(row.account()==null?anchor.phone().isEmpty():row.account().phone().equals(anchor.phone()));}
    private void restorePresentation(Snapshot current){
        if(restoration==null)return;Bundle saved=restoration;restoration=null;
        if(saved.getLong("namespace",-1)!=current.namespace()||!repository.dataIdentity().equals(saved.getString("dataIdentity","")))return;
        renderNamespace=current.namespace();renderDataIdentity=repository.dataIdentity();reviewExpanded=saved.getBoolean("review");peopleExpanded=saved.getBoolean("people");
        Account detail=current.account(saved.getLong("expanded",-1));expanded=detail!=null&&detail.phone().equals(saved.getString("expandedPhone",""))?detail.id():-1;
        pendingAnchor=readAnchor(saved,"list");preSearchAnchor=readAnchor(saved,"preSearch");
        restoring=true;search.setText(saved.getString("query",""));restoring=false;
        main.removeCallbacks(searchTask);
        if(saved.getBoolean("numberForm"))main.post(()->{if(started&&repository.current().namespace()==saved.getLong("namespace")&&repository.dataIdentity().equals(saved.getString("dataIdentity")))addNumber(saved.getString("numberPhone",""),saved.getString("numberName",""));});
    }
    private void refresh(){
        if(!started)return;for(Runnable binding:settingsBindings)binding.run();Snapshot s=repository.current();
        if(s.loaded())restorePresentation(s);
        if(renderNamespace!=s.namespace()||!renderDataIdentity.equals(repository.dataIdentity())){
            dismissDialogs();
            renderNamespace=s.namespace();renderDataIdentity=repository.dataIdentity();matches=java.util.Collections.emptyList();matchesReady=false;
            expanded=-1;reviewExpanded=false;peopleExpanded=false;preSearchAnchor=START;pendingAnchor=START;
            restoring=true;search.setText("");restoring=false;render();
        }
        String text;
        if(!s.error().isEmpty())text=s.error();
        else if(!s.loaded())text="Loading your choices…";
        else if(!repository.pendingChoices().isEmpty())text="Choices waiting to save · actions paused";
        else if(!app.registry().available())text="Blocking is unavailable in this build. You can save choices here.";
        else if(!s.consent())text="Choose the businesses you want to hear from.";
        else if(!GateAccessibilityService.connected())text="Blocking paused · screen access is off";
        else if(s.circuitOpen())text="Blocking paused · compatibility check required";
        else if(!s.binding().bound())text="Verify the receiving account before applying choices.";
        else if(repository.disarmed()||s.paused()||!s.enabled())text="Rule off · no new blocks will run";
        else if(app.registry().measuredRoute(this,GateAccessibilityService.packageSelected())!=null)text="Rule saved · start a visible session to apply new choices";
        else text=s.pending()>0?"Rule on · "+getResources().getQuantityString(R.plurals.waiting_actions,s.pending(),s.pending()):"Rule on. Checks run in supported visible screens.";
        if(app.senderFilter().enabled())text="Name filtering on · "+app.senderFilter().current().items().stream().filter(io.github.appunnim.businessgate.data.SenderFilterStore.Item::needsReview).count()+" conversations awaiting cleanup or review";
        if(!status.getText().toString().equals(text))status.setText(text);
        pause.setText(!repository.disarmed()&&s.enabled()&&!s.paused()?"Pause":"Resume");
        int applicable=applicablePending(s);batch.setVisibility(applicable>0?View.VISIBLE:View.GONE);batch.setText(getResources().getQuantityString(R.plurals.pending_actions,applicable,applicable));
        int generation=++queryGeneration;
        repository.search(search.getText().toString(),result->{if(!started||generation!=queryGeneration)return;matches=result;matchesReady=true;render();});
    }
    private int applicablePending(Snapshot state){
        if(!app.registry().available()||!GateAccessibilityService.connected()||!state.binding().bound()||!repository.consented()||state.circuitOpen()||!state.error().isEmpty()||!repository.pendingChoices().isEmpty())return 0;
        long now=System.currentTimeMillis();int count=0;
        for(Account account:state.accounts()){
            if(!account.pending()||account.jobState()==JobState.FAILED||account.kind()==Kind.NON_DIRECT||account.kind()==Kind.AMBIGUOUS)continue;
            if(io.github.appunnim.businessgate.policy.RetryPolicy.eligibility(account.attempts(),account.jobUpdatedAt(),now)!=io.github.appunnim.businessgate.policy.RetryPolicy.Eligibility.READY)continue;
            if(account.jobAction()==Action.BLOCK&&state.enabled()&&!state.paused()&&account.choice()!=Choice.ALLOW&&(account.choice()==Choice.DENY_MANUAL||account.kind()==Kind.BUSINESS_CONFIRMED))count++;
            else if(account.jobAction()==Action.UNBLOCK&&(account.choice()==Choice.ALLOW||repository.businessNameEnabled(account))&&!account.nonce().isEmpty()&&now>=account.grantCreatedAt()&&now-account.grantCreatedAt()<io.github.appunnim.businessgate.policy.RuleEngine.GRANT_TTL_MS)count++;
        }
        return count;
    }
    private void render(){
        focus.context(repository.dataIdentity(),repository.current().namespace());focus.capture();
        Anchor anchor=pendingAnchor!=null&&matchesReady?pendingAnchor:captureAnchor();
        rows.clear();Snapshot s=repository.current();boolean searching=!search.getText().toString().trim().isEmpty();
        if(compact)rows.add(new Row(-18,"summary","",null));
        if(!s.error().isEmpty()||!repository.pendingChoices().isEmpty())rows.add(new Row(-16,"storage","",null));
        if(!s.loaded()){
            rows.add(new Row(-17,"waiting",s.error().isEmpty()?"Loading your choices…":"Check storage to load your saved choices.",null));
            adapter.notifyDataSetChanged();return;
        }
        if(!searching){
            if(!s.consent())rows.add(new Row(-1,"setup","",null));
            else if(!app.registry().available()||!GateAccessibilityService.connected())rows.add(new Row(-2,"compatibility","",null));
        }
        List<Account> enabled=matches.stream().filter(Account::businessRow).filter(a->a.choice()==Choice.ALLOW||repository.businessNameEnabled(a)).collect(java.util.stream.Collectors.toList());
        List<Account> denied=matches.stream().filter(Account::businessRow).filter(a->a.choice()!=Choice.ALLOW&&!repository.businessNameEnabled(a)).collect(java.util.stream.Collectors.toList());
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
        if(matches.isEmpty())rows.add(new Row(-14,"empty",searching?"No matching account on this phone":"No businesses found yet",null));
        rows.add(new Row(-15,"footer","Only accounts found on this phone are listed. Personal and uncertain accounts are not automatically blocked.",null));
        adapter.notifyDataSetChanged();
        if(matchesReady){
            int position=Math.min(anchor.position(),Math.max(0,rows.size()-1));
            for(int i=0;i<rows.size();i++)if(sameAnchor(rows.get(i),anchor)){position=i;break;}
            if(pendingAnchor!=null||position!=list.getFirstVisiblePosition()||list.getChildCount()==0||list.getChildAt(0).getTop()!=anchor.top())list.setSelectionFromTop(Math.max(0,position),anchor.top());
            pendingAnchor=null;
        }
    }
    private void group(long id,String title,List<Account> accounts){if(!accounts.isEmpty()){rows.add(new Row(id,"section",title+" · "+accounts.size(),null));for(Account a:accounts)rows.add(new Row(a.id(),"account","",a));}}
    private void overflow(View anchor){
        PopupMenu menu=new PopupMenu(this,anchor);
        String[] items={"Enable a number","Settings & privacy","Compatibility & help","Clear local data"};
        for(int i=0;i<items.length;i++)menu.getMenu().add(0,i,i,items[i]);
        if(repository.current().binding().bound()&&app.registry().available())menu.getMenu().add(0,4,4,"Inspect business number");
        if(repository.current().binding().bound()&&repository.current().discovery())menu.getMenu().add(0,5,5,"Review incoming conversations");
        menu.getMenu().add(0,6,6,"Sender name filter");
        menu.setOnMenuItemClickListener(item->{switch(item.getItemId()){case 0->addNumber();case 1->settings();case 2->compatibility();case 3->clearLocal();case 4->inspectBusiness();case 5->reviewIncoming();case 6->senderFilter();default->{}}return true;});menu.show();
    }
    private DialogScope dialogScope(){return new DialogScope(repository.choiceScope(),repository.dataIdentity(),repository.current().globalRevision(),repository.epoch());}
    private boolean currentDialog(DialogScope scope){
        return resumed&&started&&!isFinishing()&&!isDestroyed()&&scope.choice().equals(repository.choiceScope())&&scope.data().equals(repository.dataIdentity())&&scope.revision()==repository.current().globalRevision()&&scope.authority()==repository.epoch();
    }
    private AlertDialog.Builder dialog(String title){
        DialogScope scope=dialogScope();focus.cancel();
        TextView heading=Ui.text(this,title,20,R.color.ink,true);Ui.pad(heading,24,12);heading.setAccessibilityHeading(true);
        boolean flowing=getResources().getConfiguration().fontScale>=1.5f||getResources().getConfiguration().screenHeightDp<400;
        AlertDialog[] active={null};
        return new AlertDialog.Builder(this){
            @Override public AlertDialog.Builder setView(View view){
                if(!flowing)return super.setView(view);
                if(view instanceof android.widget.ScrollView scroll&&scroll.getChildCount()==1){View child=scroll.getChildAt(0);scroll.removeView(child);view=child;}
                if(heading.getParent() instanceof ViewGroup parent)parent.removeView(heading);
                LinearLayout content=Ui.column(MainActivity.this);content.addView(heading);content.addView(view);
                super.setTitle(null);super.setCustomTitle(null);return super.setView(Ui.scroll(MainActivity.this,content));
            }
            @Override public AlertDialog.Builder setMessage(CharSequence message){
                if(!flowing)return super.setMessage(message);
                TextView body=Ui.text(MainActivity.this,message,16,R.color.ink,false);Ui.pad(body,24,8);return setView(body);
            }
            @Override public AlertDialog.Builder setMessage(int resource){return setMessage(getText(resource));}
            @Override public AlertDialog.Builder setItems(CharSequence[] items,android.content.DialogInterface.OnClickListener listener){
                if(!flowing)return super.setItems(items,listener);
                LinearLayout choices=Ui.column(MainActivity.this);Ui.pad(choices,24,8);
                for(int i=0;i<items.length;i++){int selected=i;choices.addView(Ui.button(MainActivity.this,items[i].toString(),false,()->{if(active[0]!=null&&active[0].isShowing()&&currentDialog(scope)){active[0].dismiss();listener.onClick(active[0],selected);}}));}
                return setView(choices);
            }
            @Override public AlertDialog.Builder setPositiveButton(CharSequence text,android.content.DialogInterface.OnClickListener listener){
                return super.setPositiveButton(text,listener==null?null:(d,w)->{if(active[0]!=null&&active[0].isShowing()&&currentDialog(scope))listener.onClick(d,w);else announce("This review expired. Open it again to review the current state.");});
            }
            @Override public AlertDialog create(){
                dialogs.removeIf(d->!d.isShowing());AlertDialog created=super.create();active[0]=created;created.getWindow().setTitle(title);dialogs.add(created);created.setOnShowListener(shown->decorateDialog(created));return created;
            }
            @Override public AlertDialog show(){AlertDialog created=create();created.show();return created;}
        }.setTitle(title).setCustomTitle(heading);
    }
    private void decorateDialog(AlertDialog dialog){
        android.graphics.Rect available=new android.graphics.Rect();
        getWindow().getDecorView().getWindowVisibleDisplayFrame(available);
        // The display width includes a side navigation bar on compact landscape windows.
        // Use the activity's visible frame on every supported API to avoid counting its insets twice.
        int width=available.width()>0?available.width():getResources().getDisplayMetrics().widthPixels;
        dialog.getWindow().setLayout(Math.min(width,Ui.dp(this,600)),ViewGroup.LayoutParams.WRAP_CONTENT);
        Button first=null;
        for(int which:new int[]{AlertDialog.BUTTON_POSITIVE,AlertDialog.BUTTON_NEGATIVE,AlertDialog.BUTTON_NEUTRAL}){
            Button button=dialog.getButton(which);if(button==null||button.getVisibility()!=View.VISIBLE)continue;
            button.setAllCaps(false);button.setMinHeight(Ui.dp(this,48));if(first==null)first=button;
        }
        if(first==null||!(first.getParent() instanceof LinearLayout panel))return;
        Runnable fit=()->{
            if(panel.getWidth()==0||panel.getOrientation()==LinearLayout.VERTICAL)return;
            int required=panel.getPaddingLeft()+panel.getPaddingRight();
            for(int i=0;i<panel.getChildCount();i++)if(panel.getChildAt(i) instanceof Button button&&button.getVisibility()==View.VISIBLE)required+=Math.max(button.getMinimumWidth(),Math.round(button.getPaint().measureText(button.getText().toString()))+button.getCompoundPaddingLeft()+button.getCompoundPaddingRight());
            if(required<=panel.getWidth())return;
            panel.setOrientation(LinearLayout.VERTICAL);
            for(int i=0;i<panel.getChildCount();i++){
                View child=panel.getChildAt(i);
                if(child instanceof Button)child.setLayoutParams(new LinearLayout.LayoutParams(-1,-2));else child.setVisibility(View.GONE);
            }
        };
        panel.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->fit.run());fit.run();
    }
    private void dismissDialogs(){for(AlertDialog dialog:new ArrayList<>(dialogs))dialog.dismiss();dialogs.clear();numberDialog=null;numberPhone=null;numberName=null;}
    private Bundle captureNumberDraft(){
        if(numberDialog==null||!numberDialog.isShowing()||!repository.choiceScope().equals(numberScope)||!repository.dataIdentity().equals(dialogData))return null;
        Bundle draft=new Bundle();draft.putLong("namespace",repository.current().namespace());draft.putString("dataIdentity",dialogData);draft.putString("numberPhone",numberPhone.getText().toString());draft.putString("numberName",numberName.getText().toString());return draft;
    }
    private void addNumber(){addNumber("","");}
    private void addNumber(String initialPhone,String initialName){
        GateRepository.ChoiceScope scope=repository.choiceScope();
        DialogScope review=dialogScope();
        LinearLayout fields=Ui.column(this);Ui.pad(fields,24,4);
        TextView explanation=Ui.text(this,"This permission belongs to one exact number. A new number needs its own permission.",14,R.color.muted,false);fields.addView(explanation);
        fields.addView(Ui.text(this,"Full number with country code",14,R.color.ink,false));
        EditText phone=new EditText(this);phone.setHint("+1 202 555 0101");phone.setContentDescription("Full phone number with country code");phone.setInputType(InputType.TYPE_CLASS_PHONE);phone.setTextDirection(View.TEXT_DIRECTION_LTR);phone.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);phone.setMinHeight(Ui.dp(this,56));fields.addView(phone);
        fields.addView(Ui.text(this,"Name (optional)",14,R.color.ink,false));
        EditText name=new EditText(this);name.setHint("Name");name.setContentDescription("Optional local name");name.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_WORDS);name.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);name.setMinHeight(Ui.dp(this,56));fields.addView(name);
        phone.setText(initialPhone);name.setText(initialName);
        AlertDialog dialog=dialog("Enable a number").setView(Ui.scroll(this,fields)).setNegativeButton("Cancel",null).setPositiveButton("Enable",null).create();
        numberDialog=dialog;numberPhone=phone;numberName=name;numberScope=scope;dialogData=repository.dataIdentity();
        dialog.setOnShowListener(d->{decorateDialog(dialog);dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            if(!dialog.isShowing())return;
            if(!currentDialog(review)){phone.setError("This review expired. Reopen the form for the current account.");return;}
            try{Identity.canonicalPhone(phone.getText().toString());if(repository.enableNumber(scope,phone.getText().toString(),name.getText().toString(),()->announce("Number enabled. Waiting for a supported account check.")))dialog.dismiss();else phone.setError("Storage or the receiving account changed, or too many changes are pending. Keep this number and reopen the form.");}
            catch(IllegalArgumentException invalid){phone.setError("Enter the full number with country code");phone.requestFocus();}
        });});dialog.show();
    }
    private void accessDisclosure(){dialog("Screen access, explained").setMessage(R.string.access_disclosure)
        .setNegativeButton("Not now",null).setPositiveButton("Agree and open settings",(d,w)->repository.updateSetup("CAPABILITY",true,()->openSettings(Settings.ACTION_ACCESSIBILITY_SETTINGS))).show();}
    private void notificationDisclosure(){dialog("Notice new senders sooner").setMessage(R.string.notification_disclosure)
        .setNegativeButton("Skip",null).setPositiveButton("Agree and open settings",(d,w)->{repository.setting("discovery",true);openSettings(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);}).show();}
    private void salesDisclosure(){dialog("Optional sales hints").setMessage(R.string.sales_disclosure)
        .setNegativeButton("Keep off",null).setPositiveButton("Enable hints",(d,w)->repository.setting("sales_hints",true)).show();}
    private void toggleRule(){
        Snapshot s=repository.current();
        if(!s.error().isEmpty()||!repository.pendingChoices().isEmpty()){storageHelp();return;}
        if(!repository.disarmed()&&s.enabled()&&!s.paused()){GateAccessibilityService.stopNow();repository.pause();return;}
        if(!s.consent()){accessDisclosure();return;}
        if(!app.registry().available()){compatibility();return;}
        if(!GateAccessibilityService.connected()){accessDisclosure();return;}
        if(!s.binding().bound()){reviewReceiver();return;}
        dialog("Turn on your business rule?").setMessage(R.string.activation_disclosure)
            .setNegativeButton("Review choices",null).setPositiveButton("Turn rule on",(d,w)->startRequestedSession(false,true)).show();
        // Device/receiver readiness must be established by the qualification workflow.
    }
    private void applyPending(){
        if(!repository.current().error().isEmpty()||!repository.pendingChoices().isEmpty()){storageHelp();return;}
        if(!app.registry().available()){compatibility();return;}
        dialog("Apply your choices").setMessage("Actions need a freshly verified receiving account and supported visible profile. Block actions require your business rule to be on; explicit unblock requests can be checked while it is paused. This version checks one visible profile per session, for up to 25 seconds. Other pending choices stay saved.")
            .setNegativeButton("Close",null).setPositiveButton("Start visible session",(d,w)->startRequestedSession()).show();
    }
    private void retryCheck(Account account){
        if(!app.registry().available()){compatibility();return;}
        dialog("Check this number again?").setMessage(account.phone()+"\n\nKeep your saved choice and start a new visible check. A fresh account check is required before any action. An expired unblock request needs Unblock now.")
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
        dialog("Verify the receiving account").setMessage("Business Gate will open Settings and verify the full receiving-account number. Return here and choose Review receiving account to connect it. A visible Stop button ends the check.")
            .setNegativeButton("Later",null).setPositiveButton("Open selected app",(d,w)->{try{GateAccessibilityService.requestConnection();startActivity(open);}catch(android.content.ActivityNotFoundException error){GateAccessibilityService.stopNow();announce("The selected installation is no longer available.");}}).show();
    }
    private void reviewReceiver(){
        if(!app.registry().available()){compatibility();return;}
        Binding candidate=GateAccessibilityService.connectionCandidate();
        if(candidate==null){announce("Select the installation and open a supported profile to verify its receiving account.");return;}
        String selected=GateAccessibilityService.packageSelected();
        dialog("Connect this receiving account?").setMessage("Receiving account: "+candidate.receiver()+"\n\nChoices for other receiving accounts stay separate. Unconnected choices are not transferred.\n\n"+getString(R.string.access_disclosure))
            .setNegativeButton("Cancel",null).setPositiveButton("Agree and connect",(d,w)->{
                if(!candidate.equals(GateAccessibilityService.connectionCandidate())){announce("The account check expired. Open the supported profile again.");return;}
                repository.bindReceiver(candidate,()->repository.updateSetup("REVIEW",true,()->{GateAccessibilityService.selectInstallation(selected);announce("Receiving account connected. Review its business choices before starting.");}));
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
        if(!resumed){GateAccessibilityService.stopNow();return;}
        if(!repository.current().binding().bound()){reviewReceiver();return;}
        if(!(compatibilityCheck?GateAccessibilityService.requestCompatibilityCheck():activateRule?GateAccessibilityService.requestActivation():GateAccessibilityService.requestApply(accountId))){compatibility();return;}
        Intent open=getPackageManager().getLaunchIntentForPackage(GateAccessibilityService.packageSelected());
        if(open==null){GateAccessibilityService.stopNow();announce("Select the connected installation again.");return;}
        try{startActivity(open);}catch(android.content.ActivityNotFoundException error){GateAccessibilityService.stopNow();announce("The selected installation is unavailable.");}
    }
    private void inspectBusiness(){
        EditText phone=new EditText(this);phone.setHint("+ country code and number");phone.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        dialog("Inspect and apply business rule").setMessage("Enter the exact business number. Business Gate will verify the receiving account, open this profile, and block it if its verified business name is not enabled. Personal or uncertain profiles stop the session. The chat may be marked as read.")
            .setView(phone).setNegativeButton("Cancel",null).setPositiveButton("Inspect and apply",(d,w)->{
                if(!resumed||!GateAccessibilityService.requestInspect(phone.getText().toString())){announce("A connected account and full phone number are required.");return;}
                Intent open=getPackageManager().getLaunchIntentForPackage(GateAccessibilityService.packageSelected());
                if(open==null){GateAccessibilityService.stopNow();return;}try{startActivity(open);}catch(RuntimeException error){GateAccessibilityService.stopNow();}
            }).show();
    }
    private void reviewIncoming(){
        var candidates=io.github.appunnim.businessgate.service.GateNotificationListener.pending();
        if(candidates.isEmpty()){dialog("Incoming conversations").setMessage("No supported incoming conversations are available. Notification access and discovery must be enabled. Groups and unsupported notifications are excluded.").setPositiveButton("Close",null).show();return;}
        String[] labels=new String[candidates.size()];
        for(int i=0;i<labels.length;i++)labels[i]=io.github.appunnim.businessgate.service.GateNotificationListener.displayName(candidates.get(i))+" · "+android.text.format.DateUtils.getRelativeTimeSpanString(candidates.get(i).postTime(),System.currentTimeMillis(),android.text.format.DateUtils.MINUTE_IN_MILLIS);
        dialog("Review incoming conversations").setItems(labels,(d,which)->{
            var selected=candidates.get(which);
            boolean resumeRule=repository.current().paused()||!repository.current().enabled();
            dialog("Inspect this incoming conversation?").setMessage((resumeRule?"Resume the business rule for this receiving account and inspect this conversation. ":"")+"Business Gate will open its notification, verify the receiving account and business profile, then apply your current choices. Personal, group or uncertain profiles stop the session. Opening the chat may mark it as read and clear its notification. Stop ends the session immediately.")
                .setNegativeButton("Cancel",null).setPositiveButton(resumeRule?"Resume and inspect":"Inspect and apply",(confirm,button)->{
                    if(!resumed||!GateAccessibilityService.requestIncoming(selected.id(),resumeRule))announce("This notification changed or the connected installation is unavailable. Review incoming conversations again.");
                }).show();
        }).setNegativeButton("Close",null).show();
    }
    private void compatibility(){
        LinearLayout box=Ui.column(this);Ui.pad(box,24,8);
        box.addView(Ui.text(this,"Your choices are safe here.",18,R.color.ink,true));Ui.gap(box,8);
        box.addView(Ui.text(this,app.registry().summary(),14,R.color.muted,false));Ui.gap(box,12);
        box.addView(Ui.text(this,"Connected actions currently support the measured emulator only. Each visible session rechecks the receiving account and the exact business number. The first message may arrive, and opening a chat may mark messages as read. Existing chats remain.",14,R.color.muted,false));Ui.gap(box,12);
        box.addView(Ui.text(this,"Screen access: "+(GateAccessibilityService.connected()?"connected":"off")+"\nAndroid API: "+Build.VERSION.SDK_INT+"\nApp version: "+BuildConfig.VERSION_NAME,13,R.color.muted,false));
        if(app.registry().available()){
            box.addView(Ui.button(this,"Select connected installation",false,()->{dismissDialogs();selectInstallation();}));
            box.addView(Ui.button(this,"Review receiving account",false,()->{dismissDialogs();reviewReceiver();}));
        }
        box.addView(Ui.button(this,"Manage unconnected choices",false,()->{dismissDialogs();GateAccessibilityService.stopNow();repository.localChoices(()->announce("Managing unconnected choices. Nothing is applied to a receiving account."));}));
        if(app.registry().available()&&repository.current().binding().bound()){
            box.addView(Ui.button(this,"Check receiving account",false,()->startRequestedSession(true)));
            box.addView(Ui.button(this,"Inspect business number",true,()->{dismissDialogs();inspectBusiness();}));
        }
        box.addView(Ui.button(this,"About visible scans",false,()->dialog("Visible scan disclosure").setMessage(R.string.scan_disclosure).setPositiveButton("Understood",null).show()));
        dialog("Compatibility & help").setView(Ui.scroll(this,box)).setNegativeButton("Close",null).setPositiveButton("View diagnostics",(d,w)->diagnostics()).show();
    }
    private void diagnostics(){
        Snapshot s=repository.current();String diagnostic="Business Gate "+BuildConfig.VERSION_NAME+"\nAndroid API: "+Build.VERSION.SDK_INT+"\nQualified integration: "+app.registry().available()+"\nScreen access connected: "+GateAccessibilityService.connected()+"\nScreen consent: "+s.consent()+"\nRule enabled: "+s.enabled()+"\nPaused: "+s.paused()+"\nPending choices: "+s.pending()+"\nReason: "+(app.registry().available()?GateAccessibilityService.status():"QUALIFICATION_REQUIRED");
        dialog("Local diagnostics").setMessage(diagnostic).setNegativeButton("Close",null).setPositiveButton("Copy diagnostics",(d,w)->{
            getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText("Business Gate diagnostics",diagnostic));announce("Diagnostics copied. No names or numbers included.");}).show();
    }
    private void settings(){
        Snapshot s=repository.current();settingsBindings.clear();LinearLayout box=Ui.column(this);Ui.pad(box,24,0);
        box.addView(Ui.text(this,"LOCAL BY DESIGN",12,R.color.accent,true));Ui.gap(box,8);
        box.addView(Ui.text(this,"Exact numbers, optional names and your choices stay in this phone’s private storage. No account, ads, subscription, analytics or network permission. Chat contents and notification messages are not saved. Backup and device transfer are excluded.",14,R.color.muted,false));Ui.gap(box,12);
        box.addView(Ui.button(this,"Sender name filter",true,()->{dismissDialogs();senderFilter();}));
        box.addView(Ui.button(this,"Business name choices",false,()->{dismissDialogs();businessNames();}));
        box.addView(settingSwitch("Notification-assisted discovery",()->repository.current().discovery(),value->{if(value)notificationDisclosure();else repository.setting("discovery",false);}));
        if(repository.current().discovery()&&repository.current().binding().bound())box.addView(Ui.button(this,"Review incoming conversations",true,()->{dismissDialogs();reviewIncoming();}));
        box.addView(settingSwitch("Optional sales hints",()->repository.current().salesHints(),value->{if(value)salesDisclosure();else repository.setting("sales_hints",false);}));
        box.addView(Ui.text(this,"Hints are off by default. They can be wrong and never authorize a block. Notification text analysis stays unavailable until a notification binding is qualified.",12,R.color.muted,false));Ui.gap(box,8);
        box.addView(settingSwitch("Quiet review reminder",()->repository.current().digest(),value->{repository.setting("digest",value);if(value&&Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},70);}));
        box.addView(Ui.text(this,"At most one useful, silent reminder per 30 days. No daily summaries or success notifications.",12,R.color.muted,false));Ui.gap(box,8);
        box.addView(Ui.button(this,"Screen access settings",false,()->openSettings(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        box.addView(Ui.button(this,"Notification access settings",false,()->openSettings(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        box.addView(Ui.button(this,"View local effort",false,()->{DialogScope scope=dialogScope();repository.effortSummary(text->{if(box.isShown()&&currentDialog(scope))dialog("Observed effort").setMessage(text).setPositiveButton("Done",null).show();});}));
        box.addView(Ui.button(this,"Withdraw screen consent",false,()->{GateAccessibilityService.stopNow();repository.emergencyStop();repository.updateSetup("WELCOME",false,()->announce("Consent withdrawn. Actions stopped."));}));
        android.widget.ScrollView scroll=Ui.scroll(this,box);
        AlertDialog dialog=dialog("Settings & privacy").setView(scroll).setPositiveButton("Done",null).create();dialog.setOnDismissListener(d->settingsBindings.clear());dialog.show();
    }
    private void automaticCleanup(){
        if(!resumed||!started||!hasWindowFocus()||isFinishing()||dialogs.stream().anyMatch(AlertDialog::isShowing)||search.hasFocus()&&search.length()>0||GateAccessibilityService.cleanupActive())return;
        if(app.senderFilter().enabled()&&app.senderFilter().current().cleanup())GateAccessibilityService.requestCleanup(this);
    }
    private void senderFilter(){
        var store=app.senderFilter();var state=store.current();
        LinearLayout box=Ui.column(this);Ui.pad(box,24,8);
        box.addView(Ui.text(this,"Displayed sender rules",20,R.color.ink,true));Ui.gap(box,8);
        box.addView(Ui.text(this,"Exact names only. "+store.catalogue().size()+" public profile names and "+store.suppliedNames().size()+" supplied sender-name rules are bundled; "+new java.util.HashSet<>(state.learned().values()).size()+" business names were learned locally. This is a starter list, not every business. Unknown names stay visible until a profile check confirms a business.",14,R.color.muted,false));
        box.addView(Ui.text(this,state.error().isEmpty()?(store.enabled()?"Filtering enabled":"Filtering off or waiting for a supported account and notification access"):state.error(),14,R.color.ink,true));
        box.addView(Ui.button(this,store.enabled()?"Turn name filtering off":"Enable name filtering",true,()->{
            if(store.enabled()){GateAccessibilityService.stopNow();store.configure(false,false,ok->{dismissDialogs();senderFilter();});return;}
            dialog("Enable displayed sender filtering?").setMessage("A direct notification matching a bundled, supplied or learned name will be dismissed automatically unless whitelisted. A personal sender using the same name can also match. Unknown names and groups stay visible. A shared summary can retain a business preview until its conversation is opened; removing it could also remove personal notifications. Number-specific permissions are preserved; an unresolved exception can prevent filtering.\n\nOpening Business Gate will start a visible cleanup of up to five hidden conversations, for at most three minutes, with Stop. Each actual business profile and receiving account is verified before Block. Opening a chat may mark it as read. If a saved notification route expires, open that conversation and inspect its exact number. No message bodies are stored.")
                .setNegativeButton("Cancel",null).setPositiveButton("Agree and enable",(d,w)->store.configure(true,true,ok->{if(!ok)announce("Select and verify a receiving account first.");else{repository.setting("discovery",true);dismissDialogs();announce("Name filtering enabled. Allow notification access if it is off.");}})).show();
        }));
        box.addView(Ui.button(this,"Review names and whitelist",false,()->{dismissDialogs();senderNames();}));
        box.addView(Ui.button(this,"Clear pending businesses now",true,()->{
            store.configure(state.enabled(),true,ok->{if(!resumed||!ok)return;dismissDialogs();if(!GateAccessibilityService.requestCleanup(this))announce("No current hidden routes are ready. Inspect an exact business number for an expired route, or review incoming conversations for an unknown sender.");});
        }));
        box.addView(Ui.button(this,"Review unknown incoming senders",false,()->{dismissDialogs();reviewIncoming();}));
        box.addView(Ui.button(this,"Inspect business number",false,()->{dismissDialogs();inspectBusiness();}));
        if(!state.items().isEmpty()){
            Ui.gap(box,12);box.addView(Ui.text(this,"Hidden conversation history",16,R.color.ink,true));
            for(var item:state.items().subList(0,Math.min(30,state.items().size()))){
                String result=item.status().equals("HIDDEN_PENDING_PROFILE")?(io.github.appunnim.businessgate.service.GateNotificationListener.cleanupCandidates().stream().anyMatch(c->io.github.appunnim.businessgate.automation.AdapterRegistry.sha256(c.key().getBytes(java.nio.charset.StandardCharsets.UTF_8)).equals(item.id()))?"Hidden · ready for profile cleanup":"Hidden · route expired; inspect the exact business number"):item.status().startsWith("RESULT_UNVERIFIED")?"Block result not verified · inspect again":item.status().startsWith("VERIFIED")?"Profile verified · choices applied":item.status().startsWith("NO_PENDING_ACTION")?"Profile verified · already follows your choices":"Review needed · "+item.status().replace('_',' ').toLowerCase(java.util.Locale.ROOT);
                box.addView(Ui.text(this,item.name()+" · "+result+" · "+item.count()+" observations",13,R.color.muted,false));
            }
        }
        dialog("Sender name filter").setView(Ui.scroll(this,box)).setPositiveButton("Done",null).show();
    }
    private void senderNames(){
        var store=app.senderFilter();var policy=repository.nameVisibilityPolicy();java.util.TreeSet<String> all=new java.util.TreeSet<>(store.names());if(policy!=null)all.addAll(policy.enabledNames());
        String[] names=all.toArray(new String[0]);String[] labels=new String[names.length];
        for(int i=0;i<names.length;i++)labels[i]=names[i]+" · "+(policy!=null&&policy.enabledNames().contains(names[i])?"Whitelisted":store.catalogue().containsKey(names[i])?"Public profile catalogue":store.suppliedNames().contains(names[i])?"Supplied sender rule":"Learned from a business profile");
        dialog("Names and whitelist").setItems(labels,(d,index)->{
            String name=names[index];var source=store.catalogue().get(name);boolean enabled=policy!=null&&policy.enabledNames().contains(name);
            dialog(name).setMessage((source==null?(store.suppliedNames().contains(name)?"Supplied matching rule. The name has not been independently verified as a business or official sender.":"Name saved locally. This does not verify the sender’s identity."):"Public profile name observed "+source.checkedOn()+". Not a measured notification identity.\n\nProfile source: "+source.source()+"\nContact source: "+source.contact())+"\n\nWhitelisting preserves matching notifications immediately. Previously verified matching businesses may need a visible Unblock session; number choices take precedence.")
                .setNegativeButton("Close",null).setPositiveButton(enabled?"Remove whitelist":"Whitelist",(confirm,which)->confirmBusinessName(name,!enabled)).show();
        }).setNegativeButton("Close",null).setPositiveButton("Add whitelist name",(d,w)->enableBusinessName()).show();
    }
    private void businessNames(){
        java.util.List<GateRepository.BusinessNameChoice> choices=repository.businessNameChoices();
        DialogScope scope=dialogScope();AlertDialog.Builder review=dialog("Business name choices");
        if(choices.isEmpty())review.setMessage("No business names are enabled. Add an exact business name to save permission for this receiving account. Apply choices in a supported visible session. The first message may appear; existing chats remain.");
        else{
            String[] labels=choices.stream().map(choice->choice.name()+" · "+(choice.enabled()?"Enabled":"Not enabled")).toArray(String[]::new);
            review.setItems(labels,(d,which)->{
                if(!currentDialog(scope)){announce("These choices changed. Open the list again.");return;}
                GateRepository.BusinessNameChoice choice=choices.get(which);confirmBusinessName(choice.name(),!choice.enabled());
            });
        }
        review.setNegativeButton("Close",null).setPositiveButton("Enable a business name",(d,w)->enableBusinessName()).show();
    }
    private void enableBusinessName(){
        GateRepository.BusinessNameScope scope=repository.businessNameScope();
        LinearLayout fields=Ui.column(this);Ui.pad(fields,24,8);
        fields.addView(Ui.text(this,"Use the exact business name. Matching notifications stay visible and confirmed businesses with this spelling are permitted in the selected receiving account. Number-specific choices take precedence. Saving does not activate filtering.",14,R.color.muted,false));
        EditText name=new EditText(this);name.setSingleLine(true);name.setHint("Business name");name.setContentDescription("Exact business name to enable");name.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);name.setMinHeight(Ui.dp(this,56));fields.addView(name);
        AlertDialog form=dialog("Enable a business name").setView(Ui.scroll(this,fields)).setNegativeButton("Cancel",null).setPositiveButton("Enable",null).create();
        form.setOnShowListener(d->{decorateDialog(form);form.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            if(!resumed||!form.isShowing())return;
            String exact;try{exact=io.github.appunnim.businessgate.policy.NameVisibilityPolicy.nameKey(name.getText().toString());}
            catch(IllegalArgumentException invalid){name.setError("Enter a name of 1–120 characters without hidden formatting or line breaks.");return;}
            form.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);form.getButton(AlertDialog.BUTTON_POSITIVE).setText("Saving…");
            repository.setBusinessNameEnabled(scope,exact,true,result->{
                if(!form.isShowing()||!resumed)return;
                if(result==GateRepository.SaveResult.SAVED){form.dismiss();announce("Business name whitelisted. Apply pending to finish unblocking verified businesses.");}
                else{form.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);form.getButton(AlertDialog.BUTTON_POSITIVE).setText("Enable");name.setError(result==GateRepository.SaveResult.FAILED?"Not saved. Keep this name and check storage before retrying.":"Choices changed or a save is pending. Reopen this form and try again.");}
            });
        });});form.show();
    }
    private void confirmBusinessName(String name,boolean enabled){
        GateRepository.BusinessNameScope scope=repository.businessNameScope();
        dialog(enabled?"Enable this business name?":"Remove this name's permission?")
            .setMessage(name+"\n\n"+(enabled?"Permit confirmed businesses with this exact name in this receiving account.":"This name will no longer grant permission. Number-specific choices remain unchanged.")+"\n\nApply choices in a supported visible session. The first message may appear; existing chats remain.")
            .setNegativeButton("Cancel",null).setPositiveButton(enabled?"Enable":"Remove permission",(d,w)->repository.setBusinessNameEnabled(scope,name,enabled,result->{
                if(!resumed)return;
                announce(result==GateRepository.SaveResult.SAVED?"Business name choice saved. Start a visible session to apply it.":"Not saved. Review the current choices and retry.");
            })).show();
    }
    private View settingSwitch(String label,java.util.function.BooleanSupplier current,java.util.function.Consumer<Boolean> change){
        GateRepository.ChoiceScope scope=repository.choiceScope();String data=repository.dataIdentity();
        Switch control=new Switch(this);control.setText(label);control.setTextSize(14);control.setMinHeight(Ui.dp(this,56));control.setChecked(current.getAsBoolean());control.setSwitchPadding(Ui.dp(this,16));
        android.widget.CompoundButton.OnCheckedChangeListener[] listener=new android.widget.CompoundButton.OnCheckedChangeListener[1];
        Runnable sync=()->{control.setOnCheckedChangeListener(null);control.setChecked(current.getAsBoolean());control.setOnCheckedChangeListener(listener[0]);};
        listener[0]=(button,value)->{sync.run();if(started&&resumed&&button.isShown()&&button.hasWindowFocus()&&scope.equals(repository.choiceScope())&&data.equals(repository.dataIdentity()))change.accept(value);};
        control.setOnCheckedChangeListener(listener[0]);settingsBindings.add(sync);return control;
    }
    private void clearLocal(){AlertDialog dialog=dialog("Delete local choices and history?").setMessage("This stops future automation and removes local choices, observations, consent and effort totals. It does not unblock anyone in the connected app.")
        .setNegativeButton("Cancel",null).setPositiveButton("Delete local data",(d,w)->{GateAccessibilityService.stopNow();repository.reset(()->{search.setText("");expanded=-1;announce("Local data deleted. Existing blocks stay.");});}).show();dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getColor(R.color.danger));}
    private void openSettings(String action){if(!resumed)return;try{startActivity(new Intent(action));}catch(android.content.ActivityNotFoundException error){announce("This settings screen is unavailable on this device.");}}
    @SuppressWarnings("deprecation") // One explicit completed-action event across API 29–36; background status is not a live region.
    private void announce(String message){
        if(!started||!resumed||!hasWindowFocus())return;
        status.setText(message);status.announceForAccessibility(message);
    }
    private void choose(Account a,Choice choice){if(!repository.choose(a,choice,()->announce(choice==Choice.ALLOW?"Kept. This number will not be auto-blocked.":"Choice saved. Waiting for a supported account check.")))choiceRejected();}
    private void choiceRejected(){refresh();announce("The account changed or too many choices are pending. Review the current number and choose again.");}
    private void unblockNow(Account a){
        dialog("Unblock this number?").setMessage(a.phone()+"\n\nSave an explicit request for this number only. A supported visible check is required, and you can stop it. This does not authorize later automatic unblocking.")
            .setNegativeButton("Cancel",null).setPositiveButton("Unblock now",(d,w)->requestUnblock(a)).show();
    }
    private void requestUnblock(Account a){
        if(!repository.choose(a,Choice.ALLOW,()->{
            if(!started||isFinishing()||isDestroyed())return;
            if(app.registry().available()&&GateAccessibilityService.connected()&&repository.current().binding().bound())startRequestedSession(false,false,a.id());
            else announce("Unblock request saved. A supported visible account check is still required.");
        }))choiceRejected();
    }
    private void manualBlock(Account a){
        dialog("Block this number?").setMessage("This account has not been identified as a business.\n\n"+a.phone()+"\n\nBlock only this number because you chose it. Future messages and calls may be stopped. Another number will need its own check.")
            .setNegativeButton("Cancel",null).setPositiveButton("Block number",(d,w)->choose(a,Choice.DENY_MANUAL)).show();
    }
    private String subtitle(Account a){
        Pending pending=repository.pendingChoice(a.phone());if(pending!=null)return pending.failed()?"Change not saved · actions paused":"Saving choice · actions paused";
        if(a.jobState()==JobState.FAILED)return "Action failed · check needed";
        if(a.pending()){
            if(a.jobState()==JobState.REINSPECT||a.jobState()==JobState.VERIFYING||a.jobState()==JobState.ACTION_INTENT)return "Action result not verified · check needed";
            return a.jobAction()==Action.UNBLOCK?"Enabled · unblocking pending":a.choice()==Choice.DENY_MANUAL?"Your block is pending":"Block pending";
        }
        if((a.choice()==Choice.ALLOW||repository.businessNameEnabled(a))&&a.blockState()==BlockState.BLOCKED)return "Enabled here; blocked in connected app";
        if(a.blockState()==BlockState.BLOCKED)return "Blocked · last checked "+relative(a.checkedAt());
        if(a.choice()==Choice.ALLOW||repository.businessNameEnabled(a))return a.blockState()==BlockState.UNBLOCKED?"Enabled · unblocked":"Enabled · not checked yet";
        if(a.kind()==Kind.REGULAR_PROFILE_OBSERVED)return "Not subject to automatic blocking";
        return "Block state not verified";
    }
    private String relative(long at){if(at<=0)return "unknown";long age=System.currentTimeMillis()-at;if(age<0)return "time changed";if(age<60000)return "just now";if(age<86400000)return "today";return DateFormat.getDateInstance(DateFormat.SHORT).format(new Date(at));}
    private final class Rows extends BaseAdapter{
        @Override public int getCount(){return rows.size();}@Override public Object getItem(int p){return rows.get(p);}@Override public long getItemId(int p){return rows.get(p).id();}
        @Override public boolean hasStableIds(){return true;}
        // Native list positions remain selectable for exact viewport restoration; only child controls have actions.
        @Override public View getView(int position,View recycled,ViewGroup parent){
            Row r=rows.get(position);LinearLayout box=recycled instanceof LinearLayout?(LinearLayout)recycled:Ui.column(MainActivity.this);
            Presentation presentation=r.account()==null?null:new Presentation(r,expanded==r.id(),repository.pendingChoice(r.account().phone()),renderDataIdentity,renderNamespace,relative(r.account().checkedAt()));
            if(presentation!=null&&presentation.equals(box.getTag()))return box;
            focus.binding(true);
            try{
            box.setTag(presentation);
            AccountHeader previous=box.getTag(R.id.account_header) instanceof AccountHeader h?h:null;
            if(r.account()!=null&&previous!=null&&previous.matches(renderDataIdentity,renderNamespace,r)){
                while(box.getChildCount()>1)box.removeViewAt(1);
            }else{box.removeAllViews();box.setTag(R.id.account_header,null);}
            box.setBackgroundColor(getColor(R.color.background));box.setPadding(0,0,0,0);box.setGravity(Gravity.START);box.setOnClickListener(null);box.setClickable(false);box.setContentDescription(null);box.setAccessibilityHeading(false);box.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            switch(r.type()){
                case "summary"->{
                    if(title.getParent() instanceof ViewGroup parentTitle)parentTitle.removeView(title);
                    if(status.getParent() instanceof ViewGroup parentStatus)parentStatus.removeView(status);
                    Ui.pad(title,16,8);box.addView(title);box.addView(status);
                }
                case "section"->{TextView t=Ui.text(MainActivity.this,r.title(),12,R.color.muted,true);Ui.pad(t,16,12);t.setAccessibilityHeading(true);box.addView(t);}
                case "waiting"->{TextView t=Ui.text(MainActivity.this,r.title(),14,R.color.muted,false);Ui.pad(t,16,20);box.addView(t);}
                case "storage"->storageCard(box);
                case "setup"->setupCard(box);
                case "compatibility"->compatibilityCard(box);
                case "account","person","review"->accountRow(box,r);
                case "reviewHeader","peopleHeader"->{Button b=Ui.button(MainActivity.this,r.title()+"  "+((r.type().equals("reviewHeader")?reviewExpanded:peopleExpanded)?"−":"+"),false,()->{if(r.type().equals("reviewHeader"))reviewExpanded=!reviewExpanded;else peopleExpanded=!peopleExpanded;render();});b.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);b.setAccessibilityHeading(true);box.addView(b);}
                case "empty"->{Ui.pad(box,24,28);box.setGravity(Gravity.CENTER_HORIZONTAL);box.addView(Ui.text(MainActivity.this,r.title(),18,R.color.ink,true));Ui.gap(box,8);box.addView(Ui.text(MainActivity.this,search.getText().length()>0?"Try a business name or full phone number.":"Businesses appear after a supported account check. Add the numbers you already want to keep.",14,R.color.muted,false));Ui.gap(box,12);box.addView(Ui.button(MainActivity.this,"Enable a number",false,MainActivity.this::addNumber));}
                case "footer"->{TextView t=Ui.text(MainActivity.this,r.title(),12,R.color.muted,false);Ui.pad(t,16,20);box.addView(t);}
                default->throw new IllegalStateException("UNKNOWN_ROW");
            }
            return box;
            }finally{focus.binding(false);}
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
    private void storageCard(LinearLayout parent){
        LinearLayout c=card(parent,R.color.amber_surface);c.addView(Ui.text(this,"Actions are paused",19,R.color.amber,true));Ui.gap(c,8);
        Snapshot state=repository.current();String detail=!state.loaded()?"Storage could not confirm the current local data. Check storage before reviewing choices. Actions remain paused.":state.error().isEmpty()?"Finish saving your choices before starting a session. Unsaved changes can be lost if the app stops.":"The last saved choices remain on this phone. Check storage before trying an unsaved change again. Unsaved changes can be lost if the app stops.";
        c.addView(Ui.text(this,detail,14,R.color.amber,false));Ui.gap(c,12);
        c.addView(Ui.button(this,"Check storage",false,this::storageHelp));
        if(!repository.pendingChoices().isEmpty())c.addView(Ui.button(this,"Review unsaved changes",false,this::reviewUnsaved));
    }
    private void storageHelp(){
        dialog("Check local storage?").setMessage("Free some device space if it is full, then check again. This checks the database without deleting your saved choices. Actions stay paused. Unsaved changes need a separate Retry save; they are not replayed automatically and can be lost if the app stops.")
            .setNegativeButton("Close",null).setPositiveButton("Check storage",(d,w)->{
                GateAccessibilityService.stopNow();repository.retryStorage(result->{
                    if(!started||isFinishing()||isDestroyed())return;
                    switch(result){
                        case RECOVERED->announce("Storage is available. Actions remain paused; review your choices before starting a session.");
                        case UNSAVED_CHOICES->{announce("Storage is available. Review and retry your unsaved choices.");reviewUnsaved();}
                        case FAILED->announce("Storage is still unavailable. This check did not delete data. Free space and try again.");
                        case STALE->announce("The receiving account or local data changed. Review the current page.");
                    }
                });
            }).show();
    }
    private void reviewUnsaved(){
        List<Pending> pending=repository.pendingChoices();if(pending.isEmpty()){announce("No unsaved choices remain.");return;}
        String[] labels=pending.stream().map(p->p.phone()+" · "+choiceLabel(p.choice())+(p.failed()?" · not saved":" · saving")).toArray(String[]::new);
        dialog("Unsaved choices for this account").setItems(labels,(d,which)->retrySave(pending.get(which))).setNegativeButton("Close",null).show();
    }
    private String choiceLabel(Choice choice){return switch(choice){case ALLOW->"Enable";case DENY_MANUAL->"Block this number";case DEFAULT->"Use business rule";};}
    private void retrySave(Pending pending){
        if(!pending.failed()){announce("This choice is still being saved.");return;}
        String authority=pending.choice()==Choice.ALLOW?"Saving Enable creates a new explicit request to unblock this exact number when a supported check is available.":pending.choice()==Choice.DENY_MANUAL?"Saving Block authorizes only this exact number. Future messages and calls may be stopped.":"Saving this choice allows the business rule to apply only when the account is confirmed as a business.";
        dialog("Retry saving this choice?").setMessage(pending.phone()+"\n\n"+choiceLabel(pending.choice())+"\n\n"+authority+" Actions remain paused.")
            .setNegativeButton("Cancel",null).setPositiveButton("Retry save",(d,w)->repository.retrySave(pending,result->{
                if(!started||isFinishing()||isDestroyed())return;
                switch(result){
                    case SAVED->announce("Choice saved. Actions remain paused.");
                    case FAILED->announce("Could not save. Check storage before trying again.");
                    case STALE->announce("This choice changed. Review the current number and choose again.");
                    case BUSY->announce("A choice is already being saved, or storage is not ready.");
                }
            })).show();
    }
    private void compatibilityCard(LinearLayout parent){
        LinearLayout c=card(parent,R.color.amber_surface);c.addView(Ui.text(this,"Saved choices stay on this phone.\nActions are paused.",19,R.color.amber,true));Ui.gap(c,8);
        c.addView(Ui.text(this,!app.registry().available()?"A supported integration is needed before Business Gate can apply your choices. Existing blocks stay as they are.":"Screen access is off. Your exact-number choices are still editable.",14,R.color.amber,false));Ui.gap(c,12);
        c.addView(Ui.button(this,"Check compatibility",false,this::compatibility));
    }
    private void accountRow(LinearLayout box,Row row){
        Account a=row.account();box.setBackgroundColor(getColor(R.color.surface));Ui.pad(box,16,12);box.setGravity(Gravity.START);
        AccountHeader existing=box.getTag(R.id.account_header) instanceof AccountHeader h?h:null;
        String label=a.name().isEmpty()?a.phone():a.name();String initials=a.name().isEmpty()?"#":a.name().codePoints().limit(1).collect(StringBuilder::new,StringBuilder::appendCodePoint,StringBuilder::append).toString().toUpperCase(Locale.ROOT);
        String sub=repository.pendingChoice(a.phone())!=null?subtitle(a):row.type().equals("review")?(a.review()==Review.POSSIBLE_COMMERCIAL?"Possible business · not blocked":"New sender · not blocked"):subtitle(a);
        if(a.review()==Review.TYPE_CHANGED)sub="Account type changed · "+sub;
        if(existing==null){
            LinearLayout header=Ui.row(this);header.setMinimumHeight(Ui.dp(this,64));
            TextView avatar=Ui.text(this,"",14,R.color.muted,true);avatar.setGravity(Gravity.CENTER);avatar.setBackground(Ui.shape(this,R.color.background,false));avatar.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);header.addView(avatar,new LinearLayout.LayoutParams(Ui.dp(this,36),Ui.dp(this,36)));
            LinearLayout info=Ui.column(this);Ui.pad(info,12,0);info.setMinimumHeight(Ui.dp(this,64));info.setGravity(Gravity.CENTER_VERTICAL);
            TextView name=Ui.text(this,"",16,R.color.ink,true),number=Ui.text(this,"",13,R.color.muted,false),detail=Ui.text(this,"",12,row.type().equals("review")?R.color.amber:R.color.muted,false);
            number.setTextDirection(View.TEXT_DIRECTION_LTR);info.addView(name);info.addView(number);info.addView(detail);header.addView(info,new LinearLayout.LayoutParams(0,-2,1));
            info.setFocusable(true);info.setScreenReaderFocusable(true);info.setBackground(Ui.controlBackground(this,android.R.color.transparent));
            for(int i=0;i<info.getChildCount();i++)info.getChildAt(i).setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            Switch toggle=null;
            if(row.type().equals("account")){toggle=new Switch(this);toggle.setMinHeight(Ui.dp(this,48));toggle.setMinWidth(Ui.dp(this,48));toggle.setShowText(false);toggle.setBackground(Ui.controlBackground(this,android.R.color.transparent));header.addView(toggle,new LinearLayout.LayoutParams(Ui.dp(this,52),Ui.dp(this,48)));}
            existing=new AccountHeader(renderDataIdentity,renderNamespace,a.phone(),row.type(),header,info,avatar,name,number,detail,toggle);box.setTag(R.id.account_header,existing);box.addView(header);
        }
        existing.avatar().setText(initials);existing.name().setText(label);existing.number().setText(a.phone());existing.subtitle().setText(sub);
        LinearLayout info=existing.info();info.setContentDescription(label+", "+a.phone()+", "+sub+". "+(expanded==a.id()?"Collapse details":"Show details"));info.setOnClickListener(v->{expanded=expanded==a.id()?-1:a.id();render();});focus.mark(info,a.phone(),"details");
        if(existing.toggle()!=null){
            Switch sw=existing.toggle();sw.setOnCheckedChangeListener(null);Pending pending=repository.pendingChoice(a.phone());boolean byName=!a.businessName().isEmpty()&&a.kind()==Kind.BUSINESS_CONFIRMED&&a.choice()==Choice.DEFAULT&&pending==null;
            sw.setChecked(byName?repository.businessNameEnabled(a):(pending==null||pending.failed()?a.choice():pending.choice())==Choice.ALLOW);
            sw.setEnabled(!repository.savingBusinessName());
            sw.setContentDescription((byName?"Enable business name ":"Enable ")+label+", "+a.phone()+". "+sub);
            sw.setOnCheckedChangeListener((button,on)->{
                if(byName){
                    boolean saved=repository.businessNameEnabled(a);if(on==saved)return;button.setChecked(saved);
                    Account latest=repository.current().account(a.id());
                    if(latest==null||latest.namespace()!=a.namespace()||latest.revision()!=a.revision()||!latest.businessName().equals(a.businessName())){refresh();return;}
                    confirmBusinessName(a.businessName(),on);
                }else choose(a,on?Choice.ALLOW:Choice.DEFAULT);
            });focus.mark(sw,a.phone(),"switch");
        }
        if(row.type().equals("review")){
            Ui.gap(box,8);box.addView(Ui.text(this,a.kind()==Kind.REGULAR_PROFILE_OBSERVED?"No business badge seen":"Account type not checked",13,R.color.muted,false));
            if(a.hintBits()!=0)box.addView(Ui.text(this,SalesHintEngine.reasons(a.hintBits()),13,R.color.amber,false));
            LinearLayout actions=Ui.row(this);actions.addView(rowButton(a,"Keep","keep",()->choose(a,Choice.ALLOW)),new LinearLayout.LayoutParams(0,-2,1));actions.addView(rowButton(a,"Block","block",()->manualBlock(a)),new LinearLayout.LayoutParams(0,-2,1));box.addView(actions);
            box.addView(Ui.button(this,"Not now",false,()->{repository.dismiss(a.id());reviewExpanded=false;announce("Left alone. Review is optional.");}));
        }
        if(expanded==a.id()){
            Ui.gap(box,12);String checked=a.checkedAt()>0?DateFormat.getDateTimeInstance().format(new Date(a.checkedAt())):"Not checked";
            box.addView(Ui.text(this,(a.businessName().isEmpty()?"Only "+a.phone()+" follows this choice. A new number needs its own permission.":"Verified business name: "+a.businessName()+". Name permission applies to confirmed businesses with exactly this name. Exact-number exceptions take precedence.")+"\n\nLast account check: "+checked+"\n"+(a.kind()==Kind.BUSINESS_CONFIRMED?"Business account observed.":"No current business authority.")+"\nYour choice: "+(a.choice()==Choice.ALLOW?"keep enabled":a.choice()==Choice.DENY_MANUAL?"manually block this number":"apply the business rule")+".\n\nThe switch is your preference; a pending action is not proof of a completed block. Exact-number choices remain until you change them, even if a number changes owner.",13,R.color.muted,false));
            Pending unsaved=repository.pendingChoice(a.phone());
            if(unsaved!=null&&unsaved.failed()){
                box.addView(Ui.text(this,"Requested: "+choiceLabel(unsaved.choice())+"\nSaved choice: "+choiceLabel(a.choice())+"\nThis unsaved change must be entered again if the app closes before it is saved.",13,R.color.amber,false));
                box.addView(rowButton(a,"Retry save","retry-save",()->retrySave(unsaved)));
            }
            if(!a.businessName().isEmpty()&&a.choice()==Choice.DEFAULT)box.addView(rowButton(a,"Always enable this number","enable-number",()->choose(a,Choice.ALLOW)));
            if(a.choice()==Choice.ALLOW&&a.blockState()!=BlockState.UNBLOCKED)box.addView(rowButton(a,"Unblock now","unblock",()->unblockNow(a)));
            if(row.type().equals("person"))box.addView(rowButton(a,a.choice()==Choice.ALLOW?"Kept · never auto-blocked":"Keep this number","keep",()->choose(a,Choice.ALLOW)));
            if(a.pending()||a.jobState()==JobState.FAILED)box.addView(rowButton(a,"Retry check","retry-check",()->retryCheck(a)));
        }
        View divider=new View(this);divider.setBackgroundColor(getColor(R.color.line));LinearLayout.LayoutParams line=new LinearLayout.LayoutParams(-1,Ui.dp(this,1));line.topMargin=Ui.dp(this,12);box.addView(divider,line);
    }
    private Button rowButton(Account account,String label,String role,Runnable action){Button button=Ui.button(this,label,false,action);focus.mark(button,account.phone(),role);return button;}
}
