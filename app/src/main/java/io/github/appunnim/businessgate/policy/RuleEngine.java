package io.github.appunnim.businessgate.policy;

import static io.github.appunnim.businessgate.policy.Model.*;

/** Pure policy. All readiness inputs must come from fresh qualified observations. */
public final class RuleEngine {
    public enum Reason { READY, GUARD_FAILED, POLICY_UNLOADED, CONSENT_MISSING, IDENTITY_CHANGED,
        STALE_EVIDENCE, NON_DIRECT, ENABLED, RULE_PAUSED, NOT_CURRENT_BUSINESS,
        ALREADY_SATISFIED, BLOCK_STATE_UNKNOWN, NO_UNBLOCK_AUTHORITY, POLICY_CHANGED }
    public record Decision(Action action, Reason reason) {}
    public static final long GRANT_TTL_MS = 7L * 24 * 60 * 60 * 1000;
    public static final long EVIDENCE_TTL_MS = 1500;
    public static final int GUARD_COUNT = 16;
    public static final int ALL_GUARDS = (1 << GUARD_COUNT) - 1;
    // connected, official signature, exact build, locale, foreground, interactive,
    // unlocked, receiver, profile binding, safe window, overlay safe, no Stop,
    // no immediate allow veto, circuit closed, finite lease, durable storage.
    public record Context(int guards, long elapsedNow, long wallNow, long generation,
        long expectedGlobal, long expectedAccount, long deadline) {}

    public Decision evaluate(Snapshot s, Account a, Evidence e, Context c) {
        if (!s.loaded() || a == null) return none(Reason.POLICY_UNLOADED);
        if (!s.consent()) return none(Reason.CONSENT_MISSING);
        if (s.circuitOpen() || c.guards() != ALL_GUARDS || c.elapsedNow() >= c.deadline()) return none(Reason.GUARD_FAILED);
        if (s.globalRevision() != c.expectedGlobal() || a.revision() != c.expectedAccount()) return none(Reason.POLICY_CHANGED);
        if (e == null || !s.binding().bound() || !s.binding().receiver().equals(e.receiver())
            || !s.binding().adapter().equals(e.adapter()) || a.namespace() != s.namespace() || e.namespace() != s.namespace() || !a.phone().equals(e.phone())
            || e.receiver().isEmpty() || e.adapter().isEmpty() || !e.completeProfile() || !e.sideEffectFree()) return none(Reason.IDENTITY_CHANGED);
        if (e.generation() != c.generation() || c.elapsedNow() < e.observedElapsed()
            || c.elapsedNow() - e.observedElapsed() > EVIDENCE_TTL_MS) return none(Reason.STALE_EVIDENCE);
        if (e.kind() == Kind.NON_DIRECT || e.kind() == Kind.AMBIGUOUS) return none(Reason.NON_DIRECT);
        if (a.choice() == Choice.ALLOW) {
            if (a.nonce() == null || a.nonce().isEmpty() || c.wallNow() < a.grantCreatedAt()
                || c.wallNow() - a.grantCreatedAt() >= GRANT_TTL_MS) return none(Reason.NO_UNBLOCK_AUTHORITY);
            if (e.blockState() == BlockState.UNBLOCKED) return none(Reason.ALREADY_SATISFIED);
            if (e.blockState() == BlockState.UNKNOWN) return none(Reason.BLOCK_STATE_UNKNOWN);
            return new Decision(Action.UNBLOCK, Reason.READY);
        }
        if (!s.enabled() || s.paused()) return none(Reason.RULE_PAUSED);
        if (a.choice() != Choice.DENY_MANUAL && e.kind() != Kind.BUSINESS_CONFIRMED) return none(Reason.NOT_CURRENT_BUSINESS);
        if (e.blockState() == BlockState.BLOCKED) return none(Reason.ALREADY_SATISFIED);
        if (e.blockState() == BlockState.UNKNOWN) return none(Reason.BLOCK_STATE_UNKNOWN);
        return new Decision(Action.BLOCK, Reason.READY);
    }
    private Decision none(Reason reason) { return new Decision(Action.NONE, reason); }
}
