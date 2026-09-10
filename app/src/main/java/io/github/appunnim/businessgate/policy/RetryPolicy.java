package io.github.appunnim.businessgate.policy;

/** Event-driven opportunities only. A retry never creates authority or schedules a wake-up. */
public final class RetryPolicy {
    public static final int MAX_ATTEMPTS=3;
    public enum Eligibility { READY, WAIT, EXHAUSTED }
    private RetryPolicy() {}
    public static Eligibility eligibility(int attempts,long lastAttemptWall,long nowWall){
        if(attempts<0||lastAttemptWall<0||nowWall<0)return Eligibility.WAIT;
        if(attempts>=MAX_ATTEMPTS)return Eligibility.EXHAUSTED;
        if(attempts==0)return Eligibility.READY;
        long backoff=attempts==1?30_000:300_000;
        return nowWall>=lastAttemptWall&&nowWall-lastAttemptWall>=backoff?Eligibility.READY:Eligibility.WAIT;
    }
}
