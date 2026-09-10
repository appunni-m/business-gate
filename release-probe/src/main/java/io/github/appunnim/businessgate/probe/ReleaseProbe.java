package io.github.appunnim.businessgate.probe;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.UiAutomation;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.ArrayDeque;
import java.util.function.Predicate;

/** External test APK. Reads and operates only Business Gate's own UI on a test emulator. */
public final class ReleaseProbe extends Instrumentation {
    private static final String PACKAGE="io.github.appunnim.businessgate";
    private static final String PHONE="+12025550197", LABEL="Upgrade fixture";
    private Bundle arguments;
    private UiAutomation automation;
    private int assertions;
    @Override public void onCreate(Bundle args){arguments=args==null?new Bundle():args;super.onCreate(args);start();}
    private void check(boolean condition,String label){assertions++;if(!condition)throw new AssertionError(label);}
    @Override public void onStart(){
        Bundle result=new Bundle();
        try{
            check(android.os.Build.FINGERPRINT.contains("generic")||android.os.Build.MODEL.contains("sdk"),"dedicated emulator required");
            automation=getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES);
            waitFor(node->"Business Gate".contentEquals(text(node)),"Business Gate rendered");
            String mode=arguments.getString("mode","verify-upgrade");
            if(mode.equals("prepare-upgrade")){
                setText("Search local accounts by name or number","Business Gate test "+java.util.UUID.randomUUID());
                waitFor(node->"No matching accounts".contentEquals(text(node)),"empty search available");
                click(node->"Enable a number".contentEquals(text(node)),"open exact-number form");
                setText("Full phone number with country code",PHONE);setText("Optional local name",LABEL);
                click(node->("Enable".contentEquals(text(node))||"ENABLE".contentEquals(text(node)))&&node.isClickable(),"save exact-number choice");
            }else if(!mode.equals("verify-upgrade"))throw new IllegalArgumentException("UNKNOWN_MODE");
            waitFor(node->"Search local accounts by name or number".contentEquals(description(node)),"management screen restored");
            setText("Search local accounts by name or number",PHONE);
            waitFor(node->LABEL.contentEquals(text(node)),"saved name survives");
            waitFor(node->PHONE.contentEquals(text(node))&&"android.widget.TextView".contentEquals(node.getClassName()==null?"":node.getClassName()),"saved exact number survives");
            AccessibilityNodeInfo choice=waitFor(node->"android.widget.Switch".contentEquals(node.getClassName()==null?"":node.getClassName())&&description(node).contains(PHONE),"correct number switch");
            check(choice.isChecked(),"ALLOW survives signed upgrade");
            check(find(node->text(node).startsWith("Rule on"))==null,"test install never silently activates rule");
            int previousWindow=choice.getWindowId();
            getContext().startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW)
                .setComponent(new android.content.ComponentName(PACKAGE,PACKAGE+".ui.MainActivity"))
                .setData(android.net.Uri.parse("businessgate://block?number="+PHONE))
                .putExtra("block_number",PHONE).putExtra("screen_consent",true).putExtra("activate_rule",true)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK|android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP));
            waitFor(node->node.getWindowId()!=previousWindow&&"Business Gate".contentEquals(text(node)),"external intent replacement window is ready");
            setText("Search local accounts by name or number",PHONE);
            AccessibilityNodeInfo retained=waitFor(node->"android.widget.Switch".contentEquals(node.getClassName()==null?"":node.getClassName())&&description(node).contains(PHONE),"choice after external intent");
            check(retained.isChecked(),"another UID cannot replace ALLOW through intent extras");
            check(find(node->text(node).startsWith("Rule on"))==null,"external intent cannot activate actions");
            result.putString("stream","PASS "+assertions+" installed-release assertions; mode="+mode+"\n");finish(Activity.RESULT_OK,result);
        }catch(Throwable error){result.putString("stream","FAIL "+error.getClass().getSimpleName()+": "+error.getMessage()+"\n");finish(Activity.RESULT_CANCELED,result);}
    }
    private static String text(AccessibilityNodeInfo node){return node.getText()==null?"":node.getText().toString();}
    private static String description(AccessibilityNodeInfo node){return node.getContentDescription()==null?"":node.getContentDescription().toString();}
    private AccessibilityNodeInfo find(Predicate<AccessibilityNodeInfo> match){
        AccessibilityNodeInfo root=automation.getRootInActiveWindow();
        if(root==null||!PACKAGE.contentEquals(root.getPackageName()==null?"":root.getPackageName()))return null;
        ArrayDeque<AccessibilityNodeInfo> queue=new ArrayDeque<>();queue.add(root);AccessibilityNodeInfo found=null;int visited=0;
        while(!queue.isEmpty()&&visited++<250){
            AccessibilityNodeInfo node=queue.remove();
            if(!PACKAGE.contentEquals(node.getPackageName()==null?"":node.getPackageName()))continue;
            if(node.isVisibleToUser()&&match.test(node)){if(found!=null)return null;found=node;}
            if(node.getChildCount()>64)return null;
            for(int i=0;i<node.getChildCount();i++){AccessibilityNodeInfo child=node.getChild(i);if(child!=null)queue.add(child);}
        }
        return queue.isEmpty()?found:null;
    }
    private AccessibilityNodeInfo waitFor(Predicate<AccessibilityNodeInfo> predicate,String label)throws InterruptedException{
        long until=SystemClock.elapsedRealtime()+10_000;AccessibilityNodeInfo node;
        while((node=find(predicate))==null){if(SystemClock.elapsedRealtime()>=until)throw new AssertionError(label);Thread.sleep(50);}
        assertions++;return node;
    }
    private void click(Predicate<AccessibilityNodeInfo> predicate,String label)throws InterruptedException{
        AccessibilityNodeInfo node=waitFor(predicate,label);
        for(int depth=0;depth<3;depth++){
            if(node==null||!PACKAGE.contentEquals(node.getPackageName()==null?"":node.getPackageName()))break;
            if(node.getActionList().stream().anyMatch(action->action.getId()==AccessibilityNodeInfo.ACTION_CLICK)){
                check(node.performAction(AccessibilityNodeInfo.ACTION_CLICK),label+" dispatch");return;
            }
            node=node.getParent();
            if(node!=null&&"android.widget.ListView".contentEquals(node.getClassName()==null?"":node.getClassName()))break;
        }
        throw new AssertionError(label+" has no qualified own control");
    }
    private void setText(String description,String value)throws InterruptedException{
        AccessibilityNodeInfo node=waitFor(n->description.contentEquals(description(n)),"own input available");
        Bundle args=new Bundle();args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,value);
        check(node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,args),"own input accepted");
        waitFor(n->description.contentEquals(description(n))&&value.contentEquals(text(n)),"own input value applied");
    }
}
