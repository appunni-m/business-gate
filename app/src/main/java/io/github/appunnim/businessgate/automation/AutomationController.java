package io.github.appunnim.businessgate.automation;

import io.github.appunnim.businessgate.policy.Model.*;
import io.github.appunnim.businessgate.policy.RuleEngine;

/** Event-driven, bounded action session. No live node or saved click is a job. */
public final class AutomationController {
    public enum Phase { IDLE, PROFILE, JOURNALING, DIALOG, VERIFYING, COMPLETE, STOPPED }
    public enum Screen { PROFILE, BLOCK_DIALOG, UNBLOCK_DIALOG, UNKNOWN }
    public enum Control { BLOCK_ENTRY, UNBLOCK_ENTRY, CONFIRM_BLOCK, CONFIRM_UNBLOCK }
    public record Frame(Screen screen, Evidence evidence, RuleEngine.Context context, boolean uniqueControl) {}
    public record Plan(long accountId, long globalRevision, long accountRevision, long generation,
        Action action, Control control, String phone, String receiver, int windowId) {}
    public interface Port {
        Snapshot policy();
        Frame inspect();
        void journal(Plan plan, Runnable committed, Runnable failed);
        boolean click(Control control, Frame freshlyChecked);
        void verified(Plan plan, Evidence evidence);
        void stopped(boolean uncertain);
    }
    private final RuleEngine engine = new RuleEngine();
    private Phase phase = Phase.IDLE;
    private long accountId, generation, deadline, globalRevision, accountRevision;
    private Action action;
    private String phone, receiver;
    private boolean pendingJournal, sent;
    public Phase phase() { return phase; }
    public void start(long id, long elapsedNow, long sessionGeneration) {
        if (phase != Phase.IDLE && phase != Phase.COMPLETE && phase != Phase.STOPPED) throw new IllegalStateException("SESSION_BUSY");
        accountId = id; generation = sessionGeneration; deadline = elapsedNow + 25_000;
        phase = Phase.PROFILE; action = Action.NONE; phone = receiver = null; pendingJournal = sent = false;
    }
    public void stop(Port port) { phase = Phase.STOPPED; generation++; pendingJournal = false; port.stopped(sent); }
    public void onEvent(Port port) {
        if (phase == Phase.IDLE || phase == Phase.COMPLETE || phase == Phase.STOPPED || pendingJournal) return;
        Frame f = port.inspect();
        Snapshot s = port.policy(); Account a = s.account(accountId);
        if (f == null || a == null || f.evidence() == null || f.context().elapsedNow() >= deadline
            || f.context().generation() != generation) { stop(port); return; }
        if (phone != null && (!phone.equals(f.evidence().phone()) || !receiver.equals(f.evidence().receiver())
            || globalRevision != s.globalRevision() || accountRevision != a.revision())) { stop(port); return; }
        RuleEngine.Decision decision = engine.evaluate(s, a, f.evidence(), f.context());
        if (phase == Phase.VERIFYING) {
            BlockState expected = action == Action.BLOCK ? BlockState.BLOCKED : BlockState.UNBLOCKED;
            if (f.screen() == Screen.PROFILE && f.evidence().blockState() == expected
                && decision.reason() == RuleEngine.Reason.ALREADY_SATISFIED) {
                phase = Phase.COMPLETE;
                port.verified(plan(a, s, f, action == Action.BLOCK ? Control.CONFIRM_BLOCK : Control.CONFIRM_UNBLOCK), f.evidence());
            } else if (decision.action() == Action.NONE && decision.reason() != RuleEngine.Reason.ALREADY_SATISFIED) stop(port);
            return;
        }
        if (decision.action() == Action.NONE || !f.uniqueControl()) { stop(port); return; }
        Control control;
        if (phase == Phase.PROFILE && f.screen() == Screen.PROFILE) {
            action = decision.action(); phone = a.phone(); receiver = f.evidence().receiver();
            globalRevision = s.globalRevision(); accountRevision = a.revision();
            control = action == Action.BLOCK ? Control.BLOCK_ENTRY : Control.UNBLOCK_ENTRY;
        } else if (phase == Phase.DIALOG && f.screen() == (action == Action.BLOCK ? Screen.BLOCK_DIALOG : Screen.UNBLOCK_DIALOG)
            && decision.action() == action) {
            control = action == Action.BLOCK ? Control.CONFIRM_BLOCK : Control.CONFIRM_UNBLOCK;
        } else { stop(port); return; }
        Plan p = plan(a, s, f, control);
        Phase previous = phase;
        pendingJournal = true; phase = Phase.JOURNALING;
        port.journal(p, () -> {
            if (!pendingJournal || generation != p.generation() || phase != Phase.JOURNALING) return;
            pendingJournal = false;
            Frame fresh = port.inspect(); Snapshot current = port.policy(); Account latest = current.account(accountId);
            if (fresh == null || fresh.evidence() == null || latest == null || fresh.context().elapsedNow() >= deadline
                || !fresh.uniqueControl() || fresh.screen() != f.screen() || fresh.evidence().windowId() != p.windowId()
                || !p.receiver().equals(fresh.evidence().receiver()) || !p.phone().equals(fresh.evidence().phone())
                || fresh.context().generation() != p.generation() || current.globalRevision() != p.globalRevision()
                || latest.revision() != p.accountRevision() || engine.evaluate(current, latest, fresh.evidence(), fresh.context()).action() != p.action()) {
                stop(port); return;
            }
            // Phase changes before dispatch: reentrant framework callbacks cannot repeat a click.
            phase = previous == Phase.PROFILE ? Phase.DIALOG : Phase.VERIFYING;
            sent = true;
            if (!port.click(control, fresh)) stop(port);
        }, () -> stop(port));
    }
    private Plan plan(Account a, Snapshot s, Frame f, Control control) {
        return new Plan(a.id(), s.globalRevision(), a.revision(), generation, action, control,
            a.phone(), f.evidence().receiver(), f.evidence().windowId());
    }
}
