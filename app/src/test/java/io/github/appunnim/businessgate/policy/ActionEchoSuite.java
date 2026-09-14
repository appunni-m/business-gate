package io.github.appunnim.businessgate.policy;

import io.github.appunnim.businessgate.connected.ActionEcho;

/** Missing sources are accepted only with the framework's action receipt and exact dispatch context. */
public final class ActionEchoSuite {
    private static int checks;
    private static void check(boolean actual,String reason){checks++;if(!actual)throw new AssertionError(reason);}
    public static void main(String[] args){
        ActionEcho receipt=new ActionEcho("owned.fixture",7,"android.widget.Button","owned.fixture:id/action",1000);
        check(receipt.matches("owned.fixture",16,7,"android.widget.Button",1000,"owned.fixture:id/action",true),"immediate own action");
        check(receipt.matches("owned.fixture",16,7,"android.widget.Button",1050,null,false),"destroyed source retains a bounded framework action receipt");
        check(!receipt.matches("owned.fixture",0,7,"android.widget.Button",1050,null,false),"ordinary user click cannot impersonate a removed own source");
        check(!receipt.matches("owned.fixture",0,7,"android.widget.Button",1050,"owned.fixture:id/action",true),"ordinary user click on same control still stops");
        check(!receipt.matches("other.fixture",16,7,"android.widget.Button",1050,null,false),"different installation");
        check(!receipt.matches("owned.fixture",16,8,"android.widget.Button",1050,null,false),"replacement window");
        check(!receipt.matches("owned.fixture",16,7,"android.widget.TextView",1050,null,false),"different control class");
        check(!receipt.matches("owned.fixture",16,7,"android.widget.Button",999,null,false),"event preceding dispatch");
        check(!receipt.matches("owned.fixture",16,7,"android.widget.Button",2501,null,false),"expired event");
        check(!receipt.matches("owned.fixture",16,7,"android.widget.Button",1050,"owned.fixture:id/other",true),"available source must match");
        check(!receipt.matches("owned.fixture",16,7,"android.widget.Button",1050,null,true),"a present source that lost its measured identity is rejected");
        check(receipt.matchesDeferred("owned.fixture",0,7,"android.widget.Button",1130,"owned.fixture:id/action",true),"measured deferred exact-node echo lacks the action field");
        check(!receipt.matchesDeferred("owned.fixture",0,7,"android.widget.Button",1130,"owned.fixture:id/action",false),"same resource on a different actual node cannot match");
        check(!receipt.matchesDeferred("owned.fixture",0,7,"android.widget.Button",1251,"owned.fixture:id/action",true),"deferred receipt has a separate short deadline");
        check(!receipt.matchesDeferred("owned.fixture",0,7,"android.widget.Button",999,"owned.fixture:id/action",true),"deferred event before dispatch rejected");
        check(!receipt.matchesDeferred("owned.fixture",0,7,"android.widget.Button",1130,null,true),"deferred source identity cannot be missing");
        check(!receipt.matchesDeferred("owned.fixture",0,8,"android.widget.Button",1130,"owned.fixture:id/action",true),"deferred replacement window rejected");
        check(!receipt.matchesDeferred("owned.fixture",0,7,"android.widget.Button",1130,"owned.fixture:id/other",true),"deferred unrelated control rejected");
        check(!receipt.matchesDeferred("owned.fixture",32,7,"android.widget.Button",1130,"owned.fixture:id/action",true),"deferred different action rejected");
        System.out.println("PASS "+checks+" action receipt and user-interruption checks");
    }
}
