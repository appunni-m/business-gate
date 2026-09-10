package io.github.appunnim.businessgate.automation;

import io.github.appunnim.businessgate.policy.Model.*;
import io.github.appunnim.businessgate.policy.RuleEngine;
import static io.github.appunnim.businessgate.automation.AutomationController.*;

/** Evaluates the exact fresh frame whose acquired control will receive the action. */
public final class FinalDispatch {
    private FinalDispatch() {}
    public static boolean allowed(Control control,Frame checked,Frame fresh,Snapshot latest,long accountId){
        if(control==null||checked==null||fresh==null||checked.evidence()==null||fresh.evidence()==null||checked.context()==null||fresh.context()==null
            ||!checked.uniqueControl()||!fresh.uniqueControl()||!latest.error().isEmpty())return false;
        Action intended=control==Control.BLOCK_ENTRY||control==Control.CONFIRM_BLOCK?Action.BLOCK:Action.UNBLOCK;
        Screen screen=switch(control){case BLOCK_ENTRY,UNBLOCK_ENTRY->Screen.PROFILE;case CONFIRM_BLOCK->Screen.BLOCK_DIALOG;case CONFIRM_UNBLOCK->Screen.UNBLOCK_DIALOG;};
        if(fresh.screen()!=screen||fresh.screen()!=checked.screen()||fresh.evidence().windowId()!=checked.evidence().windowId()
            ||!fresh.evidence().phone().equals(checked.evidence().phone())||!fresh.evidence().receiver().equals(checked.evidence().receiver())
            ||!fresh.evidence().adapter().equals(checked.evidence().adapter())||fresh.evidence().namespace()!=checked.evidence().namespace()
            ||fresh.context().expectedAccount()!=checked.context().expectedAccount()||fresh.context().expectedGlobal()!=checked.context().expectedGlobal()
            ||fresh.context().generation()!=checked.context().generation())return false;
        Account account=latest.account(accountId);
        return account!=null&&account.pending()&&account.jobAction()==intended
            &&new RuleEngine().evaluate(latest,account,fresh.evidence(),fresh.context()).action()==intended;
    }
}
