package io.github.appunnim.businessgate.connected;

import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import java.text.Normalizer;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Set;

/** Measured foreground business-profile route. The host supplies installation, receiver and policy authority. */
public final class BusinessProfileRoute {
    public enum Action { BLOCK, UNBLOCK }
    public enum Step { ENTRY, REASON, CONFIRM }
    public record Profile(String phone, String name, int windowId, long observedAt) {}
    public record Result(Action action, Profile profile, boolean mutated, int readinessRetries, int scrolls, long elapsedMillis) {}
    public record Failure(String reason, int phase, boolean mutationPossible) {}
    public interface Host {
        String selectedPackage();
        boolean active();
        default boolean clearFor(AccessibilityNodeInfo node){return true;}
        boolean authorized(Profile profile, Action action);
        /** False means a durable intent is pending. The route re-inspects before checking again. */
        boolean prepared(Profile profile, Action action, Step step);
        AccessibilityNodeInfo root();
        List<AccessibilityWindowInfo> windows();
        void later(Runnable callback, long delayMillis);
        void completed(Result result);
        void failed(Failure failure);
    }
    private final Host host;
    private final String owner, phone;
    private final Action action;
    private final ArrayDeque<ActionEcho> clicks = new ArrayDeque<>();
    private final long started = SystemClock.elapsedRealtime();
    private long changedAt = started;
    private Profile identity;
    private int phase, confirmationWindow = -1, readinessRetries, scrolls;
    private boolean done, mutationPossible;

    public BusinessProfileRoute(Host host, String phone, Action action) {
        this.host = host; this.phone = canonicalPhone(phone); this.action = java.util.Objects.requireNonNull(action);
        owner = host.selectedPackage();
        if (owner == null || owner.isEmpty()) throw new IllegalArgumentException("INSTALLATION_REQUIRED");
    }
    public void start() { tick(); }
    public void stop() { fail("USER_STOP"); }
    public void event(AccessibilityEvent event) {
        if (done || event.getEventType() != AccessibilityEvent.TYPE_VIEW_CLICKED) return;
        AccessibilityNodeInfo source = event.getSource();
        try {
            ActionEcho expected=clicks.peekFirst();
            if(expected==null||!expected.matches(String.valueOf(event.getPackageName()),event.getAction(),event.getWindowId(),String.valueOf(event.getClassName()),event.getEventTime(),source==null?null:source.getViewIdResourceName(),source!=null))fail("INTERACTION_CHANGED");
            else clicks.removeFirst();
        } finally { release(source); }
    }
    private void fail(String reason) {
        if (done) return;
        done = true; clicks.clear(); host.failed(new Failure(reason, phase, mutationPossible));
    }
    private void schedule() { if (!done) host.later(this::tick, 150); }
    private void finish(Profile profile) {
        done = true; clicks.clear();
        host.completed(new Result(action, profile, mutationPossible, readinessRetries, scrolls, SystemClock.elapsedRealtime() - started));
    }
    private boolean field(AccessibilityNodeInfo node, String suffix, String type, int count) {
        return ownedField(node, owner, suffix, type, count) && node.isVisibleToUser();
    }
    private void authority() {
        if (done || !host.active() || !owner.equals(host.selectedPackage()) || identity == null
            || !host.authorized(identity, action) || SystemClock.elapsedRealtime() - started >= 25_000)
            throw new IllegalStateException("AUTHORITY_CHANGED");
    }
    private boolean prepared(Step step) {
        authority();
        return host.prepared(identity, action, step);
    }
    private void click(AccessibilityNodeInfo node, String suffix) {
        authority();
        if (!node.refresh() || !node.isVisibleToUser() || !node.isEnabled() || !node.isClickable()
            || !owner.contentEquals(node.getPackageName())) throw new IllegalStateException("ACTION_CONTEXT_CHANGED");
        clicks.addLast(new ActionEcho(owner,node.getWindowId(),String.valueOf(node.getClassName()),owner+":id/"+suffix,SystemClock.uptimeMillis()));
        if (!node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) throw new IllegalStateException("ACTION_UNCONFIRMED");
        changedAt = SystemClock.elapsedRealtime();
    }
    private void tick() {
        if (done) return;
        AccessibilityNodeInfo root = null;
        try {
            long now = SystemClock.elapsedRealtime();
            if (!host.active() || !owner.equals(host.selectedPackage()) || now - started >= 25_000)
                throw new IllegalStateException("SESSION_INACTIVE");
            root = host.root();
            if (root == null && (phase == 1 || phase == 3) && now - changedAt < 2_500) {
                readinessRetries++; schedule(); return;
            }
            if (root == null || !owner.contentEquals(root.getPackageName())) throw new IllegalStateException("FOREGROUND_CHANGED");
            boolean active = false;
            List<AccessibilityWindowInfo> windows = host.windows();
            try { for (var window : windows) if (window.getId() == root.getWindowId()) active = window.isActive() && window.isFocused(); }
            finally { for (var window : windows) release(window); }
            if (!active && (phase == 1 || phase == 3) && now - changedAt < 2_500) { readinessRetries++; schedule(); return; }
            if (!active) throw new IllegalStateException("FOREGROUND_CHANGED");
            if (phase == 0) entry(root);
            else if (phase == 1 || phase == 2) confirmation(root, now);
            else verify(root, now);
            schedule();
        } catch (RuntimeException error) {
            String reason = error.getMessage();
            boolean pending = (phase == 1 || phase == 2) && SystemClock.elapsedRealtime() - changedAt < 2_500
                && Set.of("BLOCK_FORM_CHANGED", "REASON_CONTAINER_CHANGED", "BLOCK_CONTROL_CHANGED", "REPORT_CONTROL_CHANGED", "BLOCK_REASON_CHANGED")
                    .contains(reason == null ? "" : reason);
            if (pending) { readinessRetries++; schedule(); }
            else fail(reason == null ? "ROUTE_FAILED" : reason);
        } finally { release(root); }
    }
    private void entry(AccessibilityNodeInfo root) {
        Profile fresh = readProfile(root, owner, phone);
        if (fresh == null || (identity != null && !sameIdentity(identity, fresh))) throw new IllegalStateException("IDENTITY_CHANGED");
        identity = fresh; authority();
        AccessibilityNodeInfo list = root.getChild(0), entry = null, label = null;
        try {
            if (entryIndex(list.getChildCount()) >= 0) { entry = list.getChild(entryIndex(list.getChildCount())); label = entry == null ? null : entry.getChild(0); }
            if (entry == null || !entry.isVisibleToUser()) {
                if (scrolls >= 2 || !list.isScrollable() || !list.isVisibleToUser()) throw new IllegalStateException("ENTRY_UNAVAILABLE");
                authority(); scrolls++;
                if (!list.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)) throw new IllegalStateException("PROFILE_SCROLL_REJECTED");
                changedAt = SystemClock.elapsedRealtime(); return;
            }
            if (entryIndex(list.getChildCount()) < 0 || !field(entry, "block_contact_btn", "android.widget.LinearLayout", 1)
                || !field(label, "block_contact_text", "android.widget.TextView", 0) || entry.isCheckable())
                throw new IllegalStateException("ENTRY_STRUCTURE_CHANGED");
            String desired = action == Action.BLOCK ? "Unblock business" : "Block business";
            if (desired.contentEquals(label.getText())) { finish(fresh); return; }
            String expected = action == Action.BLOCK ? "Block business" : "Unblock business";
            if (!expected.contentEquals(label.getText())) throw new IllegalStateException("BLOCK_STATE_UNKNOWN");
            if (!host.clearFor(entry) || !prepared(Step.ENTRY)) return;
            Profile immediatelyBefore = readProfile(root, owner, phone);
            if (!sameIdentity(identity, immediatelyBefore)) throw new IllegalStateException("IDENTITY_CHANGED");
            identity = immediatelyBefore;
            phase = action == Action.BLOCK ? 1 : 3;
            if (action == Action.UNBLOCK) mutationPossible = true;
            click(entry, "block_contact_btn");
        } finally { release(label); release(entry); release(list); }
    }
    private void confirmation(AccessibilityNodeInfo root, long now) {
        authority();
        if (root.getChildCount() != 3 || root.getViewIdResourceName() != null || !"android.widget.FrameLayout".contentEquals(root.getClassName())) {
            if (phase == 1 && now - changedAt < 2_500 && sameIdentity(identity, readProfile(root, owner, phone))) return;
            throw new IllegalStateException("BLOCK_FORM_CHANGED");
        }
        AccessibilityNodeInfo title = root.getChild(1), form = root.getChild(2), report = null, container = null, reason = null, button = null;
        try {
            if (title == null || !title.isVisibleToUser() || !"android.widget.TextView".contentEquals(title.getClassName())
                || !"Block business".contentEquals(title.getText()) || !field(form, "block_reasons_content", "android.widget.ScrollView", 11))
                throw new IllegalStateException("BLOCK_FORM_CHANGED");
            if (confirmationWindow == -1) confirmationWindow = root.getWindowId();
            if (root.getWindowId() != confirmationWindow) throw new IllegalStateException("BLOCK_FORM_REPLACED");
            report = form.getChild(7); container = form.getChild(6); button = form.getChild(10);
            if (!field(report, "report_biz_checkbox", "android.widget.CheckBox", 0) || !report.isCheckable()) throw new IllegalStateException("REPORT_CONTROL_CHANGED");
            if (report.isChecked()) throw new IllegalStateException("REPORT_SELECTED");
            if (!field(container, "container", phase == 1 ? "android.widget.FrameLayout" : "android.view.ViewGroup", phase == 1 ? 1 : 3))
                throw new IllegalStateException("REASON_CONTAINER_CHANGED");
            if (!field(button, "block_button", "android.widget.Button", 0)) throw new IllegalStateException("BLOCK_CONTROL_CHANGED");
            if (!"Block".equalsIgnoreCase(String.valueOf(button.getText()))) throw new IllegalStateException("BLOCK_LABEL_CHANGED");
            reason = container.getChild(0);
            if (!field(reason, "reason", "android.widget.RadioButton", 0) || !reason.isCheckable() || !"Other".contentEquals(reason.getText()))
                throw new IllegalStateException("BLOCK_REASON_CHANGED");
            if (phase == 1) {
                if (reason.isChecked()) throw new IllegalStateException("BLOCK_FORM_ALREADY_EDITED");
                if (!host.clearFor(reason) || !prepared(Step.REASON)) return;
                phase = 2; click(reason, "reason");
            } else {
                if (!reason.isChecked() || !button.isEnabled()) {
                    if (now - changedAt < 1_500) return;
                    throw new IllegalStateException("BLOCK_REASON_UNCONFIRMED");
                }
                AccessibilityNodeInfo feedback = container.getChild(1);
                try {
                    if (!field(feedback, "text", "android.widget.EditText", 0)
                        || (!feedback.isShowingHintText() && feedback.getText() != null && feedback.getText().length() != 0))
                        throw new IllegalStateException("FEEDBACK_NOT_EMPTY");
                } finally { release(feedback); }
                if (!host.clearFor(button) || !prepared(Step.CONFIRM)) return;
                if (!report.refresh() || report.isChecked()) throw new IllegalStateException("REPORT_SELECTED");
                mutationPossible = true; phase = 3; click(button, "block_button");
            }
        } finally { for (var node : new AccessibilityNodeInfo[]{button, reason, container, report, form, title}) release(node); }
    }
    private void verify(AccessibilityNodeInfo root, long now) {
        authority();
        Profile after = readProfile(root, owner, phone);
        if (sameIdentity(identity, after)) {
            AccessibilityNodeInfo list = root.getChild(0), entry = null, label = null;
            try {
                if (entryIndex(list.getChildCount()) >= 0) {
                    entry = list.getChild(entryIndex(list.getChildCount())); label = entry == null ? null : entry.getChild(0);
                    String expected = action == Action.BLOCK ? "Unblock business" : "Block business";
                    if (field(entry, "block_contact_btn", "android.widget.LinearLayout", 1)
                        && field(label, "block_contact_text", "android.widget.TextView", 0) && expected.contentEquals(label.getText())) {
                        finish(after); return;
                    }
                }
            } finally { release(label); release(entry); release(list); }
        }
        if (now - changedAt >= 2_500) throw new IllegalStateException("RESULT_UNCONFIRMED");
    }
    /** Both fully measured profile variants; the trailing action position is not generalized. */
    public static int entryIndex(int count){return count==24?22:count==23?21:-1;}
    public static Profile readProfile(AccessibilityNodeInfo root, String owner, String expectedPhone) {
        if (root == null || !root.refresh() || !owner.contentEquals(root.getPackageName()) || (root.getChildCount() != 5 && root.getChildCount() != 6)
            || root.getViewIdResourceName() != null || !"android.widget.FrameLayout".contentEquals(root.getClassName())) return null;
        AccessibilityNodeInfo list = root.getChild(0), title = null, phone = null;
        try {
            if (list == null || !list.refresh() || !owner.contentEquals(list.getPackageName()) || !"android:id/list".equals(list.getViewIdResourceName())
                || !"android.widget.ListView".contentEquals(list.getClassName()) || list.getChildCount() < 3 || list.getChildCount() > 64) return null;
            title = list.getChild(1); phone = list.getChild(2);
            if (!ownedField(title, owner, "business_title", "android.widget.TextView", 0)
                || !ownedField(phone, owner, "business_subtitle", "android.widget.TextView", 0) || !title.isEnabled() || !phone.isEnabled() || title.getText() == null || phone.getText() == null) return null;
            String number = canonicalPhone(phone.getText().toString()), name = title.getText().toString();
            if (expectedPhone != null && !number.equals(expectedPhone)) return null;
            if (name.isEmpty() || name.length() > 320) return null;
            return new Profile(number, name, root.getWindowId(), SystemClock.elapsedRealtime());
        } catch (IllegalArgumentException invalid) { return null; }
        finally { release(phone); release(title); release(list); }
    }
    private static boolean sameIdentity(Profile a, Profile b) {
        return a != null && b != null && a.phone().equals(b.phone())
            && Normalizer.normalize(a.name().strip(), Normalizer.Form.NFC).equals(Normalizer.normalize(b.name().strip(), Normalizer.Form.NFC));
    }
    private static String canonicalPhone(String value) {
        if (value == null || value.length() > 80) throw new IllegalArgumentException("INVALID_PHONE");
        String result = value.replaceAll("[ ()-]", "");
        if (!result.matches("\\+[1-9][0-9]{6,14}")) throw new IllegalArgumentException("INVALID_PHONE");
        return result;
    }
    private static boolean ownedField(AccessibilityNodeInfo node, String owner, String suffix, String type, int children) {
        return node != null && node.refresh() && owner.contentEquals(node.getPackageName())
            && (owner + ":id/" + suffix).equals(node.getViewIdResourceName()) && type.contentEquals(node.getClassName()) && node.getChildCount() == children;
    }
    @SuppressWarnings("deprecation") private static void release(AccessibilityNodeInfo node) { if (node != null) node.recycle(); }
    @SuppressWarnings("deprecation") private static void release(AccessibilityWindowInfo window) { if (window != null) window.recycle(); }
}
