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

/** Explicit, read-only captures using the production accessibility capabilities. */
public final class MeasurementService extends AccessibilityService {
    private static volatile java.lang.ref.WeakReference<MeasurementService> connection = new java.lang.ref.WeakReference<>(null);
    static MeasurementService connected() { return connection.get(); }
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Button stop;
    private String selected = "";
    private long expires;
    private static final Set<String> LABELS = Set.of("Block", "Unblock", "Cancel", "Report", "Business account", "Block contact", "Unblock contact");

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
        stop.setOnClickListener(view -> end());
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
    JSONObject capture(List<Integer> path, boolean inspectText) {
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
            try {
                JSONArray ancestors = new JSONArray();
                for (int i = 0; i + 1 < chain.size(); i++) ancestors.put(structure(chain.get(i), owner));
                var serviceInfo = getServiceInfo();
                JSONObject declaration = new JSONObject().put("canRetrieveWindowContent", (serviceInfo.getCapabilities() & android.accessibilityservice.AccessibilityServiceInfo.CAPABILITY_CAN_RETRIEVE_WINDOW_CONTENT) != 0)
                    .put("canPerformGestures", (serviceInfo.getCapabilities() & android.accessibilityservice.AccessibilityServiceInfo.CAPABILITY_CAN_PERFORM_GESTURES) != 0);
                if (android.os.Build.VERSION.SDK_INT >= 31) declaration.put("isAccessibilityTool", serviceInfo.isAccessibilityTool());
                JSONObject result = new JSONObject().put("path", new JSONArray(path)).put("ancestors", ancestors).put("declaration", declaration)
                    .put("node", structure(chain.get(chain.size() - 1), owner)).put("windowId", root.getWindowId())
                    .put("activeFocusedWindow", true).put("acquiredNodes", chain.size());
                // Only the selected leaf is considered, and no arbitrary text is exported.
                if (!inspectText) return result.put("selectedText", new JSONObject().put("inspected", false));
                CharSequence text = chain.get(chain.size() - 1).getText();
                JSONObject summary = new JSONObject().put("inspected", true).put("present", text != null);
                if (text != null && text.length() <= 320) {
                    summary.put("codePoints", Character.codePointCount(text, 0, text.length()));
                    String label = text.toString();
                    if (LABELS.contains(label)) summary.put("knownControlLabel", label);
                } else if (text != null) summary.put("overLimit", true);
                result.put("selectedText", summary);
                return result;
            } catch (JSONException impossible) { throw new IllegalStateException("REPORT_UNAVAILABLE"); }
        });
    }
    @SuppressWarnings("deprecation")
    private static void release(AccessibilityNodeInfo node) { node.recycle(); }
    @SuppressWarnings("deprecation")
    private static void release(android.view.accessibility.AccessibilityWindowInfo window) { window.recycle(); }
    private static JSONObject structure(AccessibilityNodeInfo node, String owner) throws JSONException {
        JSONObject out = new JSONObject().put("visible", node.isVisibleToUser()).put("enabled", node.isEnabled())
            .put("clickable", node.isClickable()).put("checkable", node.isCheckable()).put("checked", node.isChecked())
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
