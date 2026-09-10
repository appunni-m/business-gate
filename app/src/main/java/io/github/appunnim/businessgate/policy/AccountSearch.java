package io.github.appunnim.businessgate.policy;

import io.github.appunnim.businessgate.policy.Model.Account;
import java.util.ArrayList;
import java.util.List;

/** Immutable search projection of one committed account snapshot, in repository order. */
public final class AccountSearch {
    private final List<Account> all;
    private final Account[] accounts;
    private final String[] keys;
    public AccountSearch(List<Account> source){
        all=List.copyOf(source);accounts=source.toArray(new Account[0]);keys=new String[accounts.length];
        for(int i=0;i<accounts.length;i++)keys[i]=accounts[i].searchKey();
    }
    public List<Account> find(String input){
        String query=Identity.query(input);if(query.isEmpty())return all;
        String key=Identity.searchKey(query),digits=query.replaceAll("[ +()\\-]", "");
        boolean number=!digits.isEmpty()&&digits.matches("[0-9]+");List<Account> result=new ArrayList<>();
        if(number){
            for(int i=0;i<accounts.length;i++)if(keys[i].indexOf(key)>=0||accounts[i].phone().indexOf(digits)>=0)result.add(accounts[i]);
        }else{
            for(int i=0;i<accounts.length;i++)if(keys[i].indexOf(key)>=0)result.add(accounts[i]);
        }
        return List.copyOf(result);
    }
}
