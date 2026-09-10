package io.github.appunnim.businessgate.policy;

import io.github.appunnim.businessgate.policy.Model.Account;
import java.util.ArrayList;
import java.util.List;

/** Immutable search projection of one committed account snapshot, in repository order. */
public final class AccountSearch {
    private record Entry(Account account,String nameKey) {}
    private final List<Entry> entries;
    public AccountSearch(List<Account> accounts){
        List<Entry> rows=new ArrayList<>(accounts.size());
        for(Account account:accounts)rows.add(new Entry(account,account.searchKey()));
        entries=List.copyOf(rows);
    }
    public List<Account> find(String input){
        String query=Identity.query(input),key=Identity.searchKey(query),digits=query.replaceAll("[ +()\\-]", "");
        boolean number=!digits.isEmpty()&&digits.matches("[0-9]+");List<Account> result=new ArrayList<>();
        for(Entry entry:entries)if(query.isEmpty()||entry.nameKey().contains(key)||(number&&entry.account().phone().contains(digits)))result.add(entry.account());
        return List.copyOf(result);
    }
}
