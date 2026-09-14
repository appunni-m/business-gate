package io.github.appunnim.businessgate.policy;

import java.util.List;
import java.util.Set;
import io.github.appunnim.businessgate.policy.Model.*;

/** Name-only notification rules. Never produces native action authority. */
public final class DisplaySenderPolicy {
    private DisplaySenderPolicy() {}
    public enum Result { HIDE, UNKNOWN_NAME, WHITELISTED, PERSONAL, NUMBER_EXCEPTION, INVALID_NAME }
    public static Result evaluate(String raw, Set<String> names, Set<String> whitelist, List<Account> accounts) {
        String name;
        try { name=NameVisibilityPolicy.nameKey(raw); } catch(IllegalArgumentException invalid) { return Result.INVALID_NAME; }
        if(whitelist.contains(name))return Result.WHITELISTED;
        for(Account account:accounts) {
            // Without an authenticated notification number, an unresolved exception must win.
            if(account.choice()==Choice.ALLOW && (account.businessName().isEmpty() || account.businessName().equals(name)))return Result.NUMBER_EXCEPTION;
            if(account.kind()==Kind.REGULAR_PROFILE_OBSERVED && (name.equals(account.name()) || name.equals(account.phone())))return Result.PERSONAL;
        }
        return names.contains(name)?Result.HIDE:Result.UNKNOWN_NAME;
    }
}
