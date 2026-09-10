package io.github.appunnim.businessgate.policy;

import java.util.List;

public final class Model {
    private Model() {}
    public enum Kind { UNKNOWN, BUSINESS_CONFIRMED, REGULAR_PROFILE_OBSERVED, AMBIGUOUS, NON_DIRECT }
    public enum Choice { DEFAULT, ALLOW, DENY_MANUAL }
    public enum BlockState { UNKNOWN, BLOCKED, UNBLOCKED }
    public enum JobState { NONE, PENDING, WAITING, ACTION_INTENT, VERIFYING, REINSPECT, DONE, CANCELED, FAILED }
    public enum Action { NONE, BLOCK, UNBLOCK }
    public enum Review { NONE, NEW_SENDER, POSSIBLE_COMMERCIAL, TYPE_CHANGED }
    public record Account(long id, long namespace, String phone, String name, Kind kind, Choice choice,
        BlockState blockState, boolean gateOwned, long revision, boolean everBusiness, Review review,
        int hintBits, long dismissedUntil, long checkedAt, long lastSeen, JobState jobState,
        Action jobAction, String nonce, long grantCreatedAt, int attempts, long jobUpdatedAt, String jobReason) {
        public Account(long id,long namespace,String phone,String name,Kind kind,Choice choice,BlockState blockState,
            boolean gateOwned,long revision,boolean everBusiness,Review review,int hintBits,long dismissedUntil,long checkedAt,long lastSeen,
            JobState jobState,Action jobAction,String nonce,long grantCreatedAt){
            this(id,namespace,phone,name,kind,choice,blockState,gateOwned,revision,everBusiness,review,hintBits,dismissedUntil,checkedAt,lastSeen,
                jobState,jobAction,nonce,grantCreatedAt,0,0,"");
        }
        public boolean businessRow() { return everBusiness || choice == Choice.DENY_MANUAL || (choice == Choice.ALLOW && kind == Kind.UNKNOWN); }
        public boolean pending() { return jobState != JobState.NONE && jobState != JobState.DONE && jobState != JobState.CANCELED && jobState != JobState.FAILED; }
    }
    public record Binding(String installation, String packageDigest, String profile, String receiver, String adapter) {
        public static Binding empty() { return new Binding("", "", "", "", ""); }
        public boolean bound() { return !installation.isEmpty() && !packageDigest.isEmpty() && !profile.isEmpty() && !receiver.isEmpty() && !adapter.isEmpty(); }
    }
    public record Readiness(long namespace, long globalRevision, long epoch, long observedElapsed, Binding binding) {}
    public record Snapshot(long namespace, long globalRevision, boolean loaded, boolean enabled,
        boolean paused, boolean consent, boolean salesHints, boolean discovery, boolean digest,
        String setup, List<Account> accounts, String error, Binding binding, boolean circuitOpen) {
        public Snapshot(long namespace,long globalRevision,boolean loaded,boolean enabled,boolean paused,boolean consent,
            boolean salesHints,boolean discovery,boolean digest,String setup,List<Account> accounts,String error,Binding binding){
            this(namespace,globalRevision,loaded,enabled,paused,consent,salesHints,discovery,digest,setup,accounts,error,binding,false);
        }
        public Snapshot(long namespace, long globalRevision, boolean loaded, boolean enabled, boolean paused, boolean consent,
            boolean salesHints, boolean discovery, boolean digest, String setup, List<Account> accounts, String error) {
            this(namespace, globalRevision, loaded, enabled, paused, consent, salesHints, discovery, digest, setup, accounts, error, Binding.empty());
        }
        public Snapshot { accounts = java.util.Collections.unmodifiableList(new java.util.ArrayList<>(accounts)); }
        public static Snapshot empty() { return new Snapshot(1, 0, false, false, true, false, false, false, false, "WELCOME", java.util.Collections.emptyList(), ""); }
        public Account account(long id) { return accounts.stream().filter(a -> a.id() == id).findFirst().orElse(null); }
        public int pending() { return (int) accounts.stream().filter(Account::pending).count(); }
    }
    public record Evidence(long namespace, String phone, Kind kind, BlockState blockState,
        long observedElapsed, long generation, int windowId, String receiver, String adapter,
        boolean completeProfile, boolean sideEffectFree) {}
}
