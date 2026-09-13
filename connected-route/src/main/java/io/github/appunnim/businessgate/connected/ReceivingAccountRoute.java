package io.github.appunnim.businessgate.connected;

import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import java.util.ArrayDeque;
import java.util.List;

/** Bounded navigation to the measured own-profile field; it never inspects message-list descendants. */
public final class ReceivingAccountRoute {
    public interface Host {
        String selectedPackage();
        boolean active();
        default boolean clearFor(AccessibilityNodeInfo node){return true;}
        AccessibilityNodeInfo root();
        List<AccessibilityWindowInfo> windows();
        boolean back();
        void later(Runnable callback, long delayMillis);
        void verified(String receiver, long observedAt);
        void failed(String reason);
    }
    private final Host host;
    private final String owner;
    private final long started = SystemClock.elapsedRealtime();
    private final ArrayDeque<ActionEcho> clicks = new ArrayDeque<>();
    private long lastAction = -1;
    private int steps;
    private String frame = "", dispatchedFrame = "", dispatchedKind = "";
    private boolean done;
    public ReceivingAccountRoute(Host host) { this.host = host; owner = host.selectedPackage(); }
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
    private void clear() { clicks.clear(); }
    private void fail(String reason) { if (!done) { done = true; clear(); host.failed(reason); } }
    private void schedule() { if (!done) host.later(this::tick, 200); }
    @SuppressWarnings("deprecation")
    private void click(AccessibilityNodeInfo node, String kind) {
        if (!host.clearFor(node)) return;
        if (!host.active() || !node.refresh() || !node.isVisibleToUser() || !node.isEnabled() || !node.isClickable())
            throw new IllegalStateException("NAVIGATION_CHANGED");
        clicks.addLast(new ActionEcho(owner,node.getWindowId(),String.valueOf(node.getClassName()),node.getViewIdResourceName(),SystemClock.uptimeMillis()));
        if (!node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) throw new IllegalStateException("NAVIGATION_UNCONFIRMED");
        lastAction = SystemClock.elapsedRealtime(); steps++; dispatchedFrame = frame; dispatchedKind = kind;
    }
    private void back() {
        if (!host.active() || !host.back()) throw new IllegalStateException("NAVIGATION_UNCONFIRMED");
        lastAction = SystemClock.elapsedRealtime(); steps++; dispatchedFrame = frame; dispatchedKind = "BACK";
    }
    private boolean field(AccessibilityNodeInfo node, String suffix, String type, int count) {
        return node != null && node.refresh() && owner.contentEquals(node.getPackageName()) && node.isVisibleToUser()
            && (owner + ":id/" + suffix).equals(node.getViewIdResourceName())
            && type.contentEquals(node.getClassName()) && node.getChildCount() == count;
    }
    private void tick() {
        if (done) return;
        AccessibilityNodeInfo root = null;
        try {
            long now = SystemClock.elapsedRealtime();
            if (!host.active() || !owner.equals(host.selectedPackage()) || now - started >= 20_000 || steps > 10)
                throw new IllegalStateException("RECEIVER_SESSION_INACTIVE");
            // A dispatched navigation never repeats against its previous frame.
            if (lastAction >= 0 && now - lastAction < 600) { schedule(); return; }
            root = host.root();
            if (root == null && lastAction >= 0 && now - lastAction < 2_500) { schedule(); return; }
            if (root == null || !owner.contentEquals(root.getPackageName())) throw new IllegalStateException("RECEIVER_ROOT_CHANGED_AT_STEP_"+steps);
            boolean active = false;
            List<AccessibilityWindowInfo> windows = host.windows();
            try { for (var window : windows) if (window.getId() == root.getWindowId()) active = window.isActive() && window.isFocused(); }
            finally { for (var window : windows) release(window); }
            if (!active && lastAction >= 0 && now-lastAction < 2_500) { schedule(); return; }
            if (!active) throw new IllegalStateException("RECEIVER_FOCUS_CHANGED_AT_STEP_"+steps);
            AccessibilityNodeInfo first = root.getChildCount() == 0 ? null : root.getChild(0);
            try { frame = root.getWindowId() + ":" + root.getChildCount() + ":" + (first == null ? "" : first.getViewIdResourceName() + ":" + first.getClassName()); }
            finally { release(first); }
            if (lastAction >= 0 && frame.equals(dispatchedFrame)) {
                if (now - lastAction >= 2_500) throw new IllegalStateException("NAVIGATION_TRANSITION_TIMEOUT");
                schedule(); return;
            }
            String receiver = readReceiver(root, owner);
            if (receiver != null) { done = true; clear(); host.verified(receiver, SystemClock.elapsedRealtime()); return; }
            if (root.getViewIdResourceName() != null || !"android.widget.FrameLayout".contentEquals(root.getClassName()))
                throw new IllegalStateException("UNSUPPORTED_NAVIGATION");
            if (root.getChildCount() == 1) {
                AccessibilityNodeInfo content = root.getChild(0);
                try {
                    if (field(content, "me_tab_root_layout", "android.widget.ScrollView", 9)) {
                        AccessibilityNodeInfo profile = content.getChild(4);
                        try {
                            if (!field(profile, "menuitem_edit_profile", "android.widget.Button", 0)) throw new IllegalStateException("PROFILE_NAVIGATION_CHANGED");
                            click(profile, "PROFILE");
                        } finally { release(profile); }
                    } else if (content != null && owner.contentEquals(content.getPackageName())
                        && "android.widget.ListView".contentEquals(content.getClassName()) && content.getChildCount() > 0 && content.getChildCount() <= 12) {
                        if (!dispatchedKind.equals("OVERFLOW")) throw new IllegalStateException("MENU_CONTEXT_UNVERIFIED");
                        AccessibilityNodeInfo selected = null;
                        try {
                            for (int i = 0; i < content.getChildCount(); i++) {
                                AccessibilityNodeInfo row = content.getChild(i), title = null;
                                try {
                                    if (row == null || !owner.contentEquals(row.getPackageName()) || !"android.widget.LinearLayout".contentEquals(row.getClassName()) || row.getChildCount() != 1)
                                        throw new IllegalStateException("MENU_STRUCTURE_CHANGED");
                                    title = row.getChild(0);
                                    if (!field(title, "title", "android.widget.TextView", 0)) throw new IllegalStateException("MENU_STRUCTURE_CHANGED");
                                    String label = String.valueOf(title.getText());
                                    if (label.equals("Settings") || label.equals("Settings X")) {
                                        if (selected != null) throw new IllegalStateException("AMBIGUOUS_SETTINGS");
                                        selected = row; row = null;
                                    }
                                } finally { release(title); release(row); }
                            }
                            if (selected == null) throw new IllegalStateException("SETTINGS_NOT_FOUND");
                            click(selected, "SETTINGS");
                        } finally { release(selected); }
                    } else throw new IllegalStateException("UNSUPPORTED_NAVIGATION");
                } finally { release(content); }
            } else if (BusinessProfileRoute.readProfile(root, owner, null) != null) back();
            else if (root.getChildCount() == 5 && chatHeader(root)) back();
            else if (root.getChildCount() == 8 || root.getChildCount() == 6) {
                AccessibilityNodeInfo overflow = root.getChild(root.getChildCount() == 8 ? 4 : 3);
                try {
                    if (!field(overflow, "menuitem_overflow", "android.widget.ImageView", 0)) throw new IllegalStateException("MAIN_NAVIGATION_CHANGED");
                    click(overflow, "OVERFLOW");
                } finally { release(overflow); }
            } else throw new IllegalStateException("UNSUPPORTED_NAVIGATION");
            schedule();
        } catch (RuntimeException failure) { fail(failure.getMessage() == null ? "RECEIVER_ROUTE_FAILED" : failure.getMessage()); }
        finally { release(root); }
    }
    private boolean chatHeader(AccessibilityNodeInfo root) {
        AccessibilityNodeInfo header = root.getChild(2);
        try { return field(header, "conversation_contact", "android.widget.LinearLayout", 1); }
        finally { release(header); }
    }
    /** Fresh exact own-profile path. The value remains local to the host's receiving-account scope. */
    public static String readReceiver(AccessibilityNodeInfo root, String owner) {
        if (root == null || !root.refresh() || !owner.contentEquals(root.getPackageName()) || root.getViewIdResourceName() != null
            || !"android.widget.FrameLayout".contentEquals(root.getClassName()) || root.getChildCount() != 3) return null;
        AccessibilityNodeInfo scroll = root.getChild(2), phoneRow = null, text = null;
        try {
            if (!node(scroll, owner, "profile_info_scroll_view", "android.widget.ScrollView", 7)) return null;
            phoneRow = scroll.getChild(5);
            if (!node(phoneRow, owner, "profile_phone_info", "android.widget.LinearLayout", 2)) return null;
            text = phoneRow.getChild(1);
            if (!node(text, owner, "profile_settings_row_subtext", "android.widget.TextView", 0) || text.getText() == null || text.getText().length() > 80) return null;
            String value = text.getText().toString().replaceAll("[ ()-]", "");
            return value.matches("\\+[1-9][0-9]{6,14}") ? value : null;
        } finally { release(text); release(phoneRow); release(scroll); }
    }
    private static boolean node(AccessibilityNodeInfo n, String owner, String suffix, String type, int count) {
        return n != null && n.refresh() && owner.contentEquals(n.getPackageName()) && n.isVisibleToUser() && n.isEnabled()
            && (owner + ":id/" + suffix).equals(n.getViewIdResourceName()) && type.contentEquals(n.getClassName()) && n.getChildCount() == count;
    }
    @SuppressWarnings("deprecation") private static void release(AccessibilityNodeInfo node) { if (node != null) node.recycle(); }
    @SuppressWarnings("deprecation") private static void release(AccessibilityWindowInfo window) { if (window != null) window.recycle(); }
}
