package io.github.appunnim.businessgate.automation;

import io.github.appunnim.businessgate.policy.Model.*;
import io.github.appunnim.businessgate.policy.RuleEngine;

/** One finite operation. Every callback belongs to one operation and one durable command. */
public final class AutomationController {
    public enum Phase { IDLE, PROFILE, JOURNALING, DIALOG, VERIFYING, COMMITTING, COMPLETE, STOPPED }
    public enum Screen { PROFILE, BLOCK_DIALOG, UNBLOCK_DIALOG, UNKNOWN }
    public enum Control { BLOCK_ENTRY, UNBLOCK_ENTRY, CONFIRM_BLOCK, CONFIRM_UNBLOCK }
    public enum CommitResult { COMMITTED, STALE, FAILED }
    public record Frame(Screen screen, Evidence evidence, RuleEngine.Context context, boolean uniqueControl) {}
    public record Plan(long accountId, long globalRevision, long accountRevision, long generation,
        Action action, Control control, String phone, String receiver, int windowId,
        long namespace, String adapter, String nonce, boolean confirmationAttempted) {}
    public interface Port {
        Snapshot policy();
        Frame inspect();
        void journal(Plan plan, Runnable committed, Runnable failed);
        boolean click(Control control, Frame freshlyChecked);
        void verified(Plan plan, Evidence evidence, java.util.function.Consumer<CommitResult> result);
        void completed();
        void stopped(boolean uncertain);
    }
    private final RuleEngine engine = new RuleEngine();
    private Phase phase = Phase.IDLE;
    private long accountId, generation, deadline, globalRevision, accountRevision, namespace, operation;
    private Action action;
    private String phone, receiver, adapter, nonce;
    private boolean confirmationAttempted;
    public Phase phase() { return phase; }
    public void start(long id, long elapsedNow, long sessionGeneration) {
        if (phase != Phase.IDLE && phase != Phase.COMPLETE && phase != Phase.STOPPED) throw new IllegalStateException("SESSION_BUSY");
        if (elapsedNow < 0 || elapsedNow > Long.MAX_VALUE - 25_000) throw new IllegalArgumentException("INVALID_CLOCK");
        accountId = id; generation = sessionGeneration; deadline = elapsedNow + 25_000; operation++;
        phase = Phase.PROFILE; action = Action.NONE; phone = receiver = adapter = nonce = null; confirmationAttempted = false;
    }
    public void stop(Port port) {
        if (phase == Phase.STOPPED) return;
        phase = Phase.STOPPED; operation++; port.stopped(confirmationAttempted);
    }
    public void onEvent(Port port) {
        if (phase == Phase.IDLE || phase == Phase.COMPLETE || phase == Phase.STOPPED || phase == Phase.JOURNALING || phase == Phase.COMMITTING) return;
        Frame f = port.inspect(); Snapshot s = port.policy(); Account a = s.account(accountId);
        if (!valid(f, s, a)) { stop(port); return; }
        RuleEngine.Decision decision = engine.evaluate(s, a, f.evidence(), f.context());
        if (phase == Phase.PROFILE && f.screen() == Screen.PROFILE) {
            if (!a.pending() || a.jobAction() == Action.NONE) { stop(port); return; }
            action = a.jobAction(); phone = a.phone(); receiver = f.evidence().receiver(); adapter = f.evidence().adapter();
            namespace = s.namespace(); globalRevision = s.globalRevision(); accountRevision = a.revision(); nonce = a.nonce();
            if (decision.reason() == RuleEngine.Reason.ALREADY_SATISFIED && expected(f.evidence())) { finish(port, a, s, f); return; }
        }
        if (phase == Phase.VERIFYING) {
            if (f.screen() == Screen.PROFILE && expected(f.evidence()) && decision.reason() == RuleEngine.Reason.ALREADY_SATISFIED) finish(port, a, s, f);
            else if (decision.action() != action) stop(port);
            return;
        }
        if (decision.action() != action || !f.uniqueControl()) { stop(port); return; }
        // A content-change event may precede the measured dialog transition. It never repeats entry.
        if (phase == Phase.DIALOG && f.screen() == Screen.PROFILE) return;
        Control control;
        if (phase == Phase.PROFILE && f.screen() == Screen.PROFILE) control = action == Action.BLOCK ? Control.BLOCK_ENTRY : Control.UNBLOCK_ENTRY;
        else if (phase == Phase.DIALOG && f.screen() == (action == Action.BLOCK ? Screen.BLOCK_DIALOG : Screen.UNBLOCK_DIALOG))
            control = action == Action.BLOCK ? Control.CONFIRM_BLOCK : Control.CONFIRM_UNBLOCK;
        else { stop(port); return; }
        Plan p = plan(a, s, f, control); Phase previous = phase; long owner = operation;
        phase = Phase.JOURNALING;
        port.journal(p, () -> {
            if (operation != owner || phase != Phase.JOURNALING) return;
            Frame fresh = port.inspect(); Snapshot current = port.policy(); Account latest = current.account(accountId);
            if (!valid(fresh, current, latest) || !fresh.uniqueControl() || fresh.screen() != f.screen()
                || fresh.evidence().windowId() != p.windowId()
                || engine.evaluate(current, latest, fresh.evidence(), fresh.context()).action() != p.action()) { stop(port); return; }
            phase = previous == Phase.PROFILE ? Phase.DIALOG : Phase.VERIFYING;
            // A failed confirmation return can still leave an uncertain external outcome.
            if (previous == Phase.DIALOG) confirmationAttempted = true;
            if (!port.click(control, fresh)) stop(port);
        }, () -> { if (operation == owner && phase == Phase.JOURNALING) stop(port); });
    }
    private boolean valid(Frame f, Snapshot s, Account a) {
        if (f == null || a == null || f.evidence() == null || f.context() == null || f.context().elapsedNow() >= deadline
            || f.context().generation() != generation) return false;
        return phone == null || (phone.equals(f.evidence().phone()) && receiver.equals(f.evidence().receiver())
            && adapter.equals(f.evidence().adapter()) && namespace == s.namespace() && namespace == f.evidence().namespace()
            && globalRevision == s.globalRevision() && accountRevision == a.revision() && java.util.Objects.equals(nonce, a.nonce()));
    }
    private boolean expected(Evidence e) { return e.blockState() == (action == Action.BLOCK ? BlockState.BLOCKED : BlockState.UNBLOCKED); }
    private void finish(Port port, Account a, Snapshot s, Frame f) {
        phase = Phase.COMMITTING; long owner = operation;
        port.verified(plan(a, s, f, action == Action.BLOCK ? Control.CONFIRM_BLOCK : Control.CONFIRM_UNBLOCK), f.evidence(), result -> {
            if (operation != owner || phase != Phase.COMMITTING) return;
            if (result == CommitResult.COMMITTED) { phase = Phase.COMPLETE; port.completed(); }
            else stop(port);
        });
    }
    private Plan plan(Account a, Snapshot s, Frame f, Control control) {
        return new Plan(a.id(), s.globalRevision(), a.revision(), generation, action, control,
            a.phone(), f.evidence().receiver(), f.evidence().windowId(), s.namespace(), f.evidence().adapter(), a.nonce(), confirmationAttempted);
    }
}
