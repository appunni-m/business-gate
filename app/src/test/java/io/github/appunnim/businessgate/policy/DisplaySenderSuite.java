package io.github.appunnim.businessgate.policy;
import java.util.*;
import io.github.appunnim.businessgate.policy.Model.*;
import io.github.appunnim.businessgate.policy.DisplaySenderPolicy.Result;
public final class DisplaySenderSuite {
    private static int assertions;
    private static void expect(String name,Set<String> whitelist,List<Account> accounts,Result expected){assertions++;if(DisplaySenderPolicy.evaluate(name,Set.of("Harbor Clinic","Éclair"),whitelist,accounts)!=expected)throw new AssertionError("display sender policy "+assertions);}
    private static Account account(Kind kind,Choice choice,String businessName){return new Account(1,1,"+12025550100","Harbor Clinic",kind,choice,BlockState.UNKNOWN,false,1,false,Review.NONE,0,0,0,0,JobState.NONE,Action.NONE,"",0,0,0,"","",businessName);}
    public static void main(String[] args){
        expect("Harbor Clinic",Set.of(),List.of(),Result.HIDE);
        expect(" Harbor Clinic\u00a0",Set.of(),List.of(),Result.HIDE);
        expect("E\u0301clair",Set.of(),List.of(),Result.HIDE);
        for(String name:new String[]{"harbor Clinic","Harbor  Clinic","Harbor Clinic Support","Eclair","Ｈarbor Clinic","Personal sender"})expect(name,Set.of(),List.of(),Result.UNKNOWN_NAME);
        for(String name:new String[]{null,"","a".repeat(121),"Harbor\u200b Clinic","Harbor\nClinic","\ud800"})expect(name,Set.of(),List.of(),Result.INVALID_NAME);
        expect("Harbor Clinic",Set.of("Harbor Clinic"),List.of(),Result.WHITELISTED);
        expect("Harbor Clinic",Set.of(),List.of(account(Kind.REGULAR_PROFILE_OBSERVED,Choice.DEFAULT,"")),Result.PERSONAL);
        expect("Harbor Clinic",Set.of(),List.of(account(Kind.BUSINESS_CONFIRMED,Choice.ALLOW,"Harbor Clinic")),Result.NUMBER_EXCEPTION);
        expect("Harbor Clinic",Set.of(),List.of(account(Kind.UNKNOWN,Choice.ALLOW,"")),Result.NUMBER_EXCEPTION);
        expect("Harbor Clinic",Set.of(),List.of(account(Kind.BUSINESS_CONFIRMED,Choice.ALLOW,"Other business")),Result.HIDE);
        expect("Harbor Clinic",Set.of("Harbor Clinic"),List.of(account(Kind.BUSINESS_CONFIRMED,Choice.DENY_MANUAL,"Harbor Clinic")),Result.WHITELISTED);
        System.out.println("PASS "+assertions+" displayed-sender rule assertions; name rules grant no native authority");
    }
}
