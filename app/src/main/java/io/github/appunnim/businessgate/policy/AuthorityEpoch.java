package io.github.appunnim.businessgate.policy;

/** Revocation and arming share one linearization point; a late callback cannot undo Stop. */
public final class AuthorityEpoch {
    private long generation;
    private boolean armed;
    public synchronized long current() { return generation; }
    public synchronized boolean armed() { return armed; }
    public synchronized boolean matches(long token) { return token == generation; }
    public synchronized void revoke() { generation++; armed = false; }
    public synchronized boolean arm(long token) {
        if (token != generation) return false;
        armed = true; return true;
    }
}
