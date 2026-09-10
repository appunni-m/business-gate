package io.github.appunnim.businessgate.policy;

/** Named observations prevent readiness from being replaced by a single optimistic boolean. */
public final class GuardFacts {
    public enum Guard { CONNECTED, SIGNATURE, BUILD, LANGUAGE, FOREGROUND, INTERACTIVE, UNLOCKED,
        RECEIVER, PROFILE, WINDOW, OVERLAY, NO_STOP, NO_ALLOW_VETO, CIRCUIT, LEASE, STORAGE }
    private int bits;
    public GuardFacts record(Guard guard,boolean observed) { if(observed)bits|=1<<guard.ordinal();else bits&=~(1<<guard.ordinal());return this; }
    public int bits(){return bits;}
}
