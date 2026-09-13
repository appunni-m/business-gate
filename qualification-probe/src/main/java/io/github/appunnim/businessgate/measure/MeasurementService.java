package io.github.appunnim.businessgate.measure;

import android.accessibilityservice.AccessibilityService;
import android.app.KeyguardManager;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.List;
import java.util.Set;

/** Explicit bounded captures and opt-in measured navigation using production accessibility capabilities. */
public final class MeasurementService extends AccessibilityService {
    private static volatile java.lang.ref.WeakReference<MeasurementService> connection = new java.lang.ref.WeakReference<>(null);
    static MeasurementService connected() { return connection.get(); }
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Button stop;
    private String selected = "";
    private record RecipientCheck(String owner, String phoneDigest, String titleDigest, long at) {}
    private static RecipientCheck recipientCheck;
    private long expires;
    private static final Set<String> LABELS = Set.of("You", "Settings", "Profile", "Calls", "Chats", "Updates", "Communities", "Select all", "Favourites", "Block", "Unblock", "Cancel", "Report", "Business account", "Block contact", "Unblock contact");
    private static final java.util.Map<String, String> LOCALIZED_NAVIGATION = java.util.Map.of("Ayarlar", "Settings", "Profil", "Profile");

    @Override protected void onServiceConnected() { connection = new java.lang.ref.WeakReference<>(this); end(); }
    @Override public void onAccessibilityEvent(AccessibilityEvent event) { /* No unsolicited inspection or event-body access. */ }
    @Override public void onInterrupt() { end(); }
    @Override public void onDestroy() { end(); if (connected() == this) connection.clear(); super.onDestroy(); }

    void begin(String target) {
        end();
        if (target == null || !target.matches("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+")) throw new IllegalArgumentException("INVALID_INSTALLATION");
        selected = target;
        var info = getServiceInfo(); info.packageNames = new String[]{target}; setServiceInfo(info);
        stop = new Button(this);
        stop.setText(R.string.stop); stop.setTextColor(Color.WHITE); stop.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff2459d3));
        stop.setMinHeight(Math.round(48 * getResources().getDisplayMetrics().density));
        stop.setOnClickListener(view -> { recipientCheck = null; end(); });
        var params = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.END;
        try { getSystemService(WindowManager.class).addView(stop, params); }
        catch (RuntimeException failure) { end(); throw new IllegalStateException("STOP_UNAVAILABLE"); }
        expires = SystemClock.elapsedRealtime() + 15_000;
        handler.postDelayed(this::end, 15_000);
    }
    void end() {
        expires = 0; selected = ""; handler.removeCallbacksAndMessages(null);
        if (stop != null) {
            try { getSystemService(WindowManager.class).removeViewImmediate(stop); }
            catch (IllegalArgumentException detached) { /* Already removed by Android. */ }
            stop = null;
        }
        var info = getServiceInfo();
        if (info != null) { info.packageNames = new String[]{"io.github.appunnim.businessgate.measure.disabled"}; setServiceInfo(info); }
    }
    JSONObject capture(List<Integer> path, boolean inspectText, String focusMode, boolean readNavigationLabel, String expectedPhone, String expectedTextSha256) {
        boolean requestFocus = focusMode.startsWith("focus-");
        boolean openProfile = "open-profile".equals(focusMode);
        boolean openContact = "open-contact".equals(focusMode);
        boolean openNavigation = openProfile || openContact;
        boolean sendDraft = "send-draft".equals(focusMode);
        boolean chatNode = "chat-node".equals(focusMode);
        if (selected.isEmpty() || SystemClock.elapsedRealtime() >= expires || stop == null || !stop.isShown()
            || !getSystemService(PowerManager.class).isInteractive() || getSystemService(KeyguardManager.class).isKeyguardLocked())
            throw new IllegalStateException("CAPTURE_INACTIVE");
        String owner = selected;
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) throw new IllegalStateException("NO_ACCESSIBLE_ROOT");
        if (root.getPackageName() == null || !owner.contentEquals(root.getPackageName())) {
            release(root);
            throw new IllegalStateException("SELECTED_APP_NOT_FOREGROUND");
        }
        return ExactPath.read(root, path, new ExactPath.Access<AccessibilityNodeInfo>() {
            @Override public boolean belongs(AccessibilityNodeInfo node) { return node.getPackageName() != null && owner.contentEquals(node.getPackageName()) && node.isVisibleToUser(); }
            @Override public int children(AccessibilityNodeInfo node) { return node.getChildCount(); }
            @Override public AccessibilityNodeInfo child(AccessibilityNodeInfo node, int index) { return node.getChild(index); }
            @Override public void release(AccessibilityNodeInfo node) { MeasurementService.release(node); }
        }, chain -> {
            boolean focused = false;
            var windows = getWindows();
            try { for (var window : windows) if (window.getId() == root.getWindowId()) focused = window.isActive() && window.isFocused(); }
            finally { for (var window : windows) release(window); }
            if (!focused || !owner.equals(selected) || SystemClock.elapsedRealtime() >= expires) throw new IllegalStateException("FOREGROUND_CHANGED");
            if (chatNode) {
                RecipientCheck check = recipientCheck;
                if (check == null || expectedPhone == null || !owner.equals(check.owner())
                    || !Neutral.digest(expectedPhone).equals(check.phoneDigest()) || SystemClock.elapsedRealtime() - check.at() > 60_000)
                    throw new IllegalStateException("RECIPIENT_RECHECK_REQUIRED");
                if (chain.size() < 3 || path.get(0) != 4 || path.get(1) != 1 || root.getChildCount() != 5
                    || !"android.widget.FrameLayout".contentEquals(root.getClassName()) || root.getViewIdResourceName() != null
                    || !"android.view.ViewGroup".contentEquals(chain.get(1).getClassName())
                    || !(owner + ":id/coordinator").equals(chain.get(1).getViewIdResourceName())
                    || !"android.widget.ListView".contentEquals(chain.get(2).getClassName())
                    || !"android:id/list".equals(chain.get(2).getViewIdResourceName())) throw new IllegalStateException("CHAT_STRUCTURE_CHANGED");
                AccessibilityNodeInfo header = root.getChild(2), title = null;
                try {
                    if (header == null || !owner.contentEquals(header.getPackageName()) || !header.isVisibleToUser()
                        || !(owner + ":id/conversation_contact").equals(header.getViewIdResourceName()) || header.getChildCount() != 1
                        || !"android.widget.LinearLayout".contentEquals(header.getClassName())) throw new IllegalStateException("RECIPIENT_CHANGED");
                    title = header.getChild(0);
                    if (!fieldMatches(title, owner, "conversation_contact_name", "android.widget.TextView", check.titleDigest(), true))
                        throw new IllegalStateException("RECIPIENT_CHANGED");
                } finally { if (title != null) release(title); if (header != null) release(header); }
            }
            if (readNavigationLabel && (chain.size() != 4 || path.size() != 3 || path.get(0) != 0 || path.get(2) != 0
                || root.getChildCount() != 1 || !"android.widget.FrameLayout".contentEquals(root.getClassName())
                || !"android.widget.ListView".contentEquals(chain.get(1).getClassName()) || chain.get(1).getChildCount() > 12
                || !"android.widget.LinearLayout".contentEquals(chain.get(2).getClassName()) || chain.get(2).getChildCount() != 1
                || !"android.widget.TextView".contentEquals(chain.get(3).getClassName()) || chain.get(3).getChildCount() != 0
                || !(owner + ":id/title").equals(chain.get(3).getViewIdResourceName()))) throw new IllegalStateException("NAVIGATION_LABEL_STRUCTURE_CHANGED");
            if (requestFocus || openNavigation || sendDraft) {
                // Only previously measured navigation structures; no account-action authority.
                AccessibilityNodeInfo leaf = chain.get(chain.size() - 1);
                boolean measuredTab = "focus-tab".equals(focusMode) && chain.size() == 3
                    && ((path.equals(List.of(7, 3)) && root.getChildCount() == 8) || (path.equals(List.of(5, 0)) && root.getChildCount() == 6))
                    && "android.widget.FrameLayout".contentEquals(chain.get(0).getClassName())
                    && chain.get(1).getChildCount() == 4 && "android.view.ViewGroup".contentEquals(chain.get(1).getClassName())
                    && leaf.getChildCount() == 0 && "android.widget.FrameLayout".contentEquals(leaf.getClassName())
                    && chain.stream().allMatch(node -> node.getViewIdResourceName() == null);
                boolean measuredOverflow = "focus-overflow".equals(focusMode) && chain.size() == 2
                    && ((path.equals(List.of(3)) && root.getChildCount() == 6) || (path.equals(List.of(4)) && root.getChildCount() == 8))
                    && "android.widget.FrameLayout".contentEquals(root.getClassName()) && root.getViewIdResourceName() == null
                    && leaf.getChildCount() == 0 && "android.widget.ImageView".contentEquals(leaf.getClassName())
                    && (owner + ":id/menuitem_overflow").equals(leaf.getViewIdResourceName());
                boolean measuredProfile = ("focus-profile".equals(focusMode) || openProfile) && chain.size() == 3 && path.equals(List.of(0, 4))
                    && root.getChildCount() == 1 && "android.widget.FrameLayout".contentEquals(root.getClassName()) && root.getViewIdResourceName() == null
                    && "android.widget.ScrollView".contentEquals(chain.get(1).getClassName()) && chain.get(1).getChildCount() == 9
                    && (owner + ":id/me_tab_root_layout").equals(chain.get(1).getViewIdResourceName())
                    && "android.widget.Button".contentEquals(leaf.getClassName()) && leaf.getChildCount() == 0
                    && (owner + ":id/menuitem_edit_profile").equals(leaf.getViewIdResourceName());
                boolean measuredContact = openContact && chain.size() == 2 && path.equals(List.of(2))
                    && root.getChildCount() == 5 && "android.widget.FrameLayout".contentEquals(root.getClassName()) && root.getViewIdResourceName() == null
                    && "android.widget.LinearLayout".contentEquals(leaf.getClassName()) && leaf.getChildCount() == 1
                    && (owner + ":id/conversation_contact").equals(leaf.getViewIdResourceName());
                boolean measuredSend = sendDraft && chain.size() == 3 && path.equals(List.of(4, 5))
                    && root.getChildCount() == 5 && "android.widget.FrameLayout".contentEquals(root.getClassName()) && root.getViewIdResourceName() == null
                    && "android.view.ViewGroup".contentEquals(chain.get(1).getClassName()) && chain.get(1).getChildCount() == 6
                    && (owner + ":id/coordinator").equals(chain.get(1).getViewIdResourceName())
                    && "android.widget.ImageButton".contentEquals(leaf.getClassName()) && leaf.getChildCount() == 0
                    && (owner + ":id/send").equals(leaf.getViewIdResourceName());
                if ((!measuredTab && !measuredOverflow && !measuredProfile && !measuredContact && !measuredSend) || !leaf.isFocusable() || !leaf.isEnabled() || !leaf.isClickable())
                    throw new IllegalStateException("NAVIGATION_STRUCTURE_CHANGED");
                if (requestFocus && !leaf.isFocused() && !leaf.performAction(AccessibilityNodeInfo.ACTION_FOCUS)) throw new IllegalStateException("NAVIGATION_FOCUS_REJECTED");
                if (requestFocus && (!leaf.refresh() || !leaf.isFocused() || !owner.contentEquals(leaf.getPackageName()))) throw new IllegalStateException("NAVIGATION_FOCUS_UNCONFIRMED");
            }
            try {
                JSONArray ancestors = new JSONArray();
                for (int i = 0; i + 1 < chain.size(); i++) ancestors.put(structure(chain.get(i), owner));
                var serviceInfo = getServiceInfo();
                JSONObject declaration = new JSONObject().put("canRetrieveWindowContent", (serviceInfo.getCapabilities() & android.accessibilityservice.AccessibilityServiceInfo.CAPABILITY_CAN_RETRIEVE_WINDOW_CONTENT) != 0)
                    .put("canPerformGestures", (serviceInfo.getCapabilities() & android.accessibilityservice.AccessibilityServiceInfo.CAPABILITY_CAN_PERFORM_GESTURES) != 0);
                if (android.os.Build.VERSION.SDK_INT >= 31) declaration.put("isAccessibilityTool", serviceInfo.isAccessibilityTool());
                JSONObject result = new JSONObject().put("path", new JSONArray(path)).put("ancestors", ancestors).put("declaration", declaration)
                    .put("node", structure(chain.get(chain.size() - 1), owner)).put("windowId", root.getWindowId())
                    .put("activeFocusedWindow", true).put("acquiredNodes", chain.size()).put("inputFocusRequested", requestFocus);
                if (openNavigation) {
                    AccessibilityNodeInfo leaf = chain.get(chain.size() - 1);
                    if (!leaf.refresh() || !leaf.isVisibleToUser() || !leaf.isEnabled() || !leaf.isClickable()
                        || !owner.contentEquals(leaf.getPackageName()) || !(owner + ":id/" + (openProfile ? "menuitem_edit_profile" : "conversation_contact")).equals(leaf.getViewIdResourceName())
                        || !owner.equals(selected) || SystemClock.elapsedRealtime() >= expires || stop == null || !stop.isShown())
                        throw new IllegalStateException("NAVIGATION_STRUCTURE_CHANGED");
                    if (!leaf.performAction(AccessibilityNodeInfo.ACTION_CLICK)) throw new IllegalStateException("PROFILE_NAVIGATION_REJECTED");
                    result.put("navigationAction", focusMode);
                }
                if (sendDraft) {
                    RecipientCheck check = recipientCheck;
                    if (check == null || !owner.equals(check.owner()) || expectedPhone == null || expectedTextSha256 == null
                        || !Neutral.digest(expectedPhone).equals(check.phoneDigest()) || SystemClock.elapsedRealtime() - check.at() > 60_000)
                        throw new IllegalStateException("RECIPIENT_RECHECK_REQUIRED");
                    AccessibilityNodeInfo composer = null, header = null, title = null;
                    try {
                        // Explicit composer/header paths only. Never enter the message list at 4,1.
                        composer = chain.get(1).getChild(3); header = root.getChild(2);
                        if (header == null || !owner.contentEquals(header.getPackageName()) || !header.isVisibleToUser()
                            || !(owner + ":id/conversation_contact").equals(header.getViewIdResourceName()) || header.getChildCount() != 1
                            || !"android.widget.LinearLayout".contentEquals(header.getClassName())) throw new IllegalStateException("RECIPIENT_CHANGED");
                        title = header.getChild(0);
                        if (!fieldMatches(title, owner, "conversation_contact_name", "android.widget.TextView", check.titleDigest(), true))
                            throw new IllegalStateException("RECIPIENT_CHANGED");
                        if (!fieldMatches(composer, owner, "entry", "android.widget.EditText", expectedTextSha256, false))
                            throw new IllegalStateException("DRAFT_CHANGED");
                        AccessibilityNodeInfo send = chain.get(chain.size() - 1);
                        if (!owner.equals(selected) || SystemClock.elapsedRealtime() >= expires || stop == null || !stop.isShown()
                            || !getSystemService(PowerManager.class).isInteractive() || getSystemService(KeyguardManager.class).isKeyguardLocked())
                            throw new IllegalStateException("CAPTURE_INACTIVE");
                        recipientCheck = null; // Consume before dispatch; uncertain outcomes are never replayed.
                        if (!send.performAction(AccessibilityNodeInfo.ACTION_CLICK)) throw new IllegalStateException("SEND_NOT_ACKNOWLEDGED");
                        result.put("messageDispatchAcknowledged", true).put("recipientProfileChecked", true).put("draftMatched", true)
                            .put("acquiredNodes", chain.size() + 3);
                    } finally {
                        if (composer != null) release(composer); if (title != null) release(title); if (header != null) release(header);
                    }
                }
                // Only explicitly selected fields are considered; no arbitrary profile text is exported.
                if (chatNode) result.put("recipientProfileCheckedForRead", true).put("acquiredNodes", chain.size() + 2);
                if (!inspectText) return result.put("selectedText", new JSONObject().put("inspected", false));
                CharSequence text = chain.get(chain.size() - 1).getText();
                JSONObject summary = new JSONObject().put("inspected", true).put("present", text != null);
                if (text != null && text.length() <= 320) {
                    summary.put("codePoints", Character.codePointCount(text, 0, text.length()));
                    String label = text.toString();
                    if (chatNode) summary.put("observedText", Neutral.redactTestText(label));
                    summary.put("directionMarks", label.codePoints().filter(point -> point == 0x200e || point == 0x200f).count())
                        .put("edgeWhitespace", label.length() - label.strip().length());
                    if (expectedPhone != null && !chatNode) {
                        boolean matches = Neutral.phoneMatches(label, expectedPhone);
                        summary.put("matchesExpectedPhone", matches);
                        recipientCheck = null;
                        if (matches && path.equals(List.of(0, 2)) && chain.size() == 3 && root.getChildCount() == 5
                            && "android.widget.FrameLayout".contentEquals(root.getClassName()) && root.getViewIdResourceName() == null
                            && "android.widget.ListView".contentEquals(chain.get(1).getClassName()) && chain.get(1).getChildCount() >= 3 && chain.get(1).getChildCount() <= 64
                            && "android:id/list".equals(chain.get(1).getViewIdResourceName())
                            && (owner + ":id/business_subtitle").equals(chain.get(2).getViewIdResourceName())
                            && "android.widget.TextView".contentEquals(chain.get(2).getClassName()) && chain.get(2).getChildCount() == 0) {
                            AccessibilityNodeInfo title = chain.get(1).getChild(1);
                            try {
                                if (title != null && owner.contentEquals(title.getPackageName()) && title.isVisibleToUser()
                                    && (owner + ":id/business_title").equals(title.getViewIdResourceName()) && title.getChildCount() == 0
                                    && "android.widget.TextView".contentEquals(title.getClassName()) && title.getText() != null
                                    && title.isEnabled() && title.getText().length() > 0 && title.getText().length() <= 320)
                                    recipientCheck = new RecipientCheck(owner, Neutral.digest(expectedPhone), Neutral.navigationTitleDigest(title.getText().toString()), SystemClock.elapsedRealtime());
                            } finally { if (title != null) release(title); }
                            result.put("acquiredNodes", chain.size() + 1).put("recipientCheckPrepared", recipientCheck != null);
                        }
                    }
                    if (expectedTextSha256 != null) summary.put("matchesExpectedDraft", Neutral.digest(label).equals(expectedTextSha256));
                    if (path.equals(List.of(2, 5, 1)) && chain.size() == 4
                        && root.getChildCount() == 3 && "android.widget.FrameLayout".contentEquals(root.getClassName()) && root.getViewIdResourceName() == null
                        && chain.get(1).getChildCount() == 7 && "android.widget.ScrollView".contentEquals(chain.get(1).getClassName())
                        && (owner + ":id/profile_info_scroll_view").equals(chain.get(1).getViewIdResourceName())
                        && chain.get(2).getChildCount() == 2 && "android.widget.LinearLayout".contentEquals(chain.get(2).getClassName())
                        && (owner + ":id/profile_phone_info").equals(chain.get(2).getViewIdResourceName())
                        && chain.get(3).getChildCount() == 0 && "android.widget.TextView".contentEquals(chain.get(3).getClassName())
                        && (owner + ":id/profile_settings_row_subtext").equals(chain.get(3).getViewIdResourceName()))
                        summary.put("internationalPhoneSyntax", Neutral.internationalPhoneSyntax(label));
                    if (readNavigationLabel) {
                        if (!Neutral.safeNavigationLabel(label)) throw new IllegalStateException("NAVIGATION_LABEL_REDACTED");
                        summary.put("navigationLabel", label);
                    }
                    if (LABELS.contains(label)) summary.put("knownControlLabel", label);
                    else if (LOCALIZED_NAVIGATION.containsKey(label)) summary.put("knownControlLabel", label)
                        .put("recognizedControlLabel", LOCALIZED_NAVIGATION.get(label)).put("labelLanguage", "tr");
                    else for (String known : LABELS) if (known.equalsIgnoreCase(label.strip())) {
                        summary.put("recognizedControlLabel", known).put("labelNeedsNormalization", true);
                        break;
                    }
                    for (String known : LABELS) if (label.startsWith(known) && label.length() > known.length()
                        && label.codePointCount(known.length(), label.length()) <= 4
                        && label.substring(known.length()).codePoints().noneMatch(Character::isLetterOrDigit)) {
                        summary.put("recognizedDecoratedLabel", known);
                        break;
                    }
                    java.util.TreeSet<String> scripts = new java.util.TreeSet<>();
                    label.codePoints().filter(Character::isLetter).forEach(point -> scripts.add(Character.UnicodeScript.of(point).name()));
                    summary.put("letterScripts", new JSONArray(scripts));
                } else if (text != null) summary.put("overLimit", true);
                result.put("selectedText", summary);
                CharSequence description = chain.get(chain.size() - 1).getContentDescription();
                JSONObject descriptionSummary = new JSONObject().put("inspected", true).put("present", description != null);
                if (description != null && description.length() <= 320) {
                    descriptionSummary.put("codePoints", Character.codePointCount(description, 0, description.length()));
                    if (chatNode) descriptionSummary.put("observedDescription", Neutral.redactTestText(description.toString()));
                    if (LABELS.contains(description.toString())) descriptionSummary.put("knownControlLabel", description.toString());
                } else if (description != null) descriptionSummary.put("overLimit", true);
                result.put("selectedDescription", descriptionSummary);
                return result;
            } catch (JSONException impossible) { throw new IllegalStateException("REPORT_UNAVAILABLE"); }
        });
    }
    private static boolean fieldMatches(AccessibilityNodeInfo node, String owner, String suffix, String type, String digest, boolean titlePresentation) {
        return node != null && node.getPackageName() != null && owner.contentEquals(node.getPackageName()) && node.isVisibleToUser()
            && node.isEnabled() && node.getChildCount() == 0 && (owner + ":id/" + suffix).equals(node.getViewIdResourceName())
            && type.contentEquals(node.getClassName()) && node.getText() != null && node.getText().length() <= 320
            && (titlePresentation ? Neutral.navigationTitleDigest(node.getText().toString()) : Neutral.digest(node.getText().toString())).equals(digest);
    }
    @SuppressWarnings("deprecation")
    private static void release(AccessibilityNodeInfo node) { node.recycle(); }
    @SuppressWarnings("deprecation")
    private static void release(android.view.accessibility.AccessibilityWindowInfo window) { window.recycle(); }
    private static JSONObject structure(AccessibilityNodeInfo node, String owner) throws JSONException {
        JSONObject out = new JSONObject().put("visible", node.isVisibleToUser()).put("enabled", node.isEnabled())
            .put("clickable", node.isClickable()).put("checkable", node.isCheckable()).put("checked", node.isChecked())
            .put("focusable", node.isFocusable()).put("focused", node.isFocused()).put("selected", node.isSelected()).put("showingHintText", node.isShowingHintText())
            .put("childCount", node.getChildCount());
        String resource = node.getViewIdResourceName();
        if (resource == null) out.put("resourceAbsent", true);
        else {
            String prefix = owner + ":id/";
            if (resource.startsWith(prefix) || resource.startsWith("android:id/")) {
                String suffix = resource.substring(resource.indexOf('/') + 1);
                out.put("resourceNamespace", resource.startsWith(prefix) ? "selected" : "android");
                if (Neutral.safeIdentifier(suffix)) out.put("resourceSuffix", suffix);
                else out.put("resourceSuffixRedacted", true);
            } else out.put("resourceNamespace", "other");
        }
        String type = node.getClassName() == null ? "" : node.getClassName().toString();
        if (type.startsWith("android.") && Neutral.safeIdentifier(type)) out.put("className", type);
        else out.put("classSha256", Neutral.digest(type));
        return out;
    }
}
