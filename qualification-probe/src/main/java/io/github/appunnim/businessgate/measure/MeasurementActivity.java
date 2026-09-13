package io.github.appunnim.businessgate.measure;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Shell-permission-protected developer entry; results stay in this tool's private data. */
public final class MeasurementActivity extends Activity {
    private static long generation;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        TextView text = new TextView(this); text.setText(R.string.app_name); setContentView(text);
        if (state != null) { finish(); return; }
        begin(this, getIntent(), this::finish, () -> {});
    }
    static void begin(Context context, Intent input, Runnable returnToScreen, Runnable completed) {
        String request = input.getStringExtra("request");
        if (request == null || !request.matches("[a-f0-9]{32}")) { returnToScreen.run(); completed.run(); return; }
        new CaptureTask(context.getApplicationContext(), input, request, ++generation, returnToScreen, completed).start();
    }
    private static final class CaptureTask {
        private final Context context;
        private final Intent input;
        private final String request, mode, target, surface, rawPath;
        private final long owner, deadline = SystemClock.elapsedRealtime() + 8_000;
        private final Handler handler = new Handler(Looper.getMainLooper());
        private Runnable returnToScreen;
        private final Runnable completed;
        private JSONObject before, report;
        private List<Integer> path;
        private MeasurementService service;
        private boolean done;
        CaptureTask(Context context, Intent input, String request, long owner, Runnable returnToScreen, Runnable completed) {
            this.context = context; this.request = request; this.owner = owner; this.returnToScreen = returnToScreen;
            this.input = input;
            this.completed = completed;
            mode = input.getStringExtra("mode"); target = input.getStringExtra("target");
            surface = input.getStringExtra("surface"); rawPath = input.getStringExtra("path");
        }
        void start() {
            try {
                if (target == null || !target.matches("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+")) throw new IllegalArgumentException("INVALID_INSTALLATION");
                String expectedPhone = input.getStringExtra("expectedPhone"), expectedText = input.getStringExtra("expectedTextSha256");
                if (expectedPhone != null && ((!"send-draft".equals(mode) && !"chat-node".equals(mode) && (!"node".equals(mode) || !List.of("profile", "receiver").contains(surface == null ? "" : surface)))
                    || !expectedPhone.matches("\\+[1-9][0-9]{6,14}"))) throw new IllegalArgumentException("INVALID_PHONE_EXPECTATION");
                if (expectedText != null && ((!"send-draft".equals(mode) && (!"node".equals(mode) || !"composer".equals(surface))) || !expectedText.matches("[a-f0-9]{64}")))
                    throw new IllegalArgumentException("INVALID_DRAFT_EXPECTATION");
                if ("send-draft".equals(mode) && (expectedPhone == null || expectedText == null)) throw new IllegalArgumentException("DRAFT_AUTHORITY_REQUIRED");
                if ("chat-node".equals(mode) && expectedPhone == null) throw new IllegalArgumentException("RECIPIENT_AUTHORITY_REQUIRED");
                before = environment(context, target);
                report = new JSONObject().put("schemaVersion", 1).put("mode", mode).put("environment", before)
                    .put("physicalQualification", false).put("mutationPerformed", "send-draft".equals(mode))
                    .put("probeApkSha256", Neutral.digest(java.nio.file.Files.readAllBytes(new File(context.getApplicationInfo().sourceDir).toPath())))
                    .put("capturedAtUtc", java.time.Instant.now().toString());
                if ("environment".equals(mode)) { complete(); return; }
                if ("focus-tab".equals(mode) || "focus-overflow".equals(mode) || "focus-profile".equals(mode) || "open-profile".equals(mode) || "open-contact".equals(mode) || "send-draft".equals(mode) || "chat-node".equals(mode)) {
                    if (!Long.toString(before.getLong("versionCode")).equals(input.getStringExtra("expectedVersion"))
                        || !before.getJSONArray("signingSha256").getString(0).equals(input.getStringExtra("expectedSigner"))
                        || before.getInt("api") != input.getIntExtra("expectedApi", -1))
                        throw new IllegalArgumentException("INSTALLATION_CHANGED");
                    path = "focus-tab".equals(mode) && rawPath == null ? List.of(7, 3) : ExactPath.parse(rawPath == null ? "" : rawPath);
                    report.put("surfaceAttestation", "chat-node".equals(mode) ? "authorized-chat" : "navigation");
                } else if ("root".equals(mode)) {
                    path = List.of();
                    report.put("screenClassification", "unclassified");
                } else if ("self-test".equals(mode)) {
                    if (!target.equals(context.getPackageName())) throw new IllegalArgumentException("SYNTHETIC_TARGET_REQUIRED");
                    path = List.of();
                    context.startActivity(new Intent(context, FixtureActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                } else if ("node".equals(mode) || "structure".equals(mode) || "navigation-label".equals(mode)) {
                    if (surface == null || !List.of("navigation", "receiver", "profile", "block-confirmation", "unblock-confirmation", "composer", "synthetic").contains(surface))
                        throw new IllegalArgumentException("SURFACE_ATTESTATION_REQUIRED");
                    path = ExactPath.parse(rawPath == null ? "" : rawPath);
                    if ("navigation-label".equals(mode) && !"navigation".equals(surface)) throw new IllegalArgumentException("NAVIGATION_SURFACE_REQUIRED");
                    report.put("surfaceAttestation", surface);
                } else throw new IllegalArgumentException("UNKNOWN_MODE");
                awaitService();
            } catch (Exception failure) { fail(failure); }
        }
        private boolean current() {
            if (done) return false;
            if (owner != generation) { fail(new IllegalStateException("CAPTURE_REPLACED")); return false; }
            if (SystemClock.elapsedRealtime() >= deadline) { fail(new IllegalStateException("CAPTURE_TIMEOUT")); return false; }
            return true;
        }
        private void awaitService() {
            if (!current()) return;
            service = MeasurementService.connected();
            if (service == null) { handler.postDelayed(this::awaitService, 100); return; }
            try {
                service.begin(target);
                // Return to the user's existing screen; the tool never launches the selected app.
                closeActivity();
                handler.postDelayed(this::capture, 200);
            } catch (RuntimeException failure) { fail(failure); }
        }
        private void capture() {
            if (!current()) return;
            try {
                JSONObject measurement = service.capture(path, "node".equals(mode) || "navigation-label".equals(mode) || "chat-node".equals(mode), mode.startsWith("focus-") || "open-profile".equals(mode) || "open-contact".equals(mode) || "send-draft".equals(mode) || "chat-node".equals(mode) ? mode : "none", "navigation-label".equals(mode), input.getStringExtra("expectedPhone"), input.getStringExtra("expectedTextSha256"));
                report.put("measurement", measurement).put("source", "production-declaration developer measurement service");
                if ("self-test".equals(mode)) {
                    if (!measurement.getBoolean("activeFocusedWindow") || measurement.getInt("acquiredNodes") != 1
                        || measurement.getJSONObject("selectedText").getBoolean("inspected"))
                        throw new IllegalStateException("ROOT_CAPTURE_FAILED");
                    report.put("assertions", 4);
                }
                complete();
            } catch (RuntimeException failure) {
                if (("NO_ACCESSIBLE_ROOT".equals(failure.getMessage()) || "SELECTED_APP_NOT_FOREGROUND".equals(failure.getMessage()) || "FOREGROUND_CHANGED".equals(failure.getMessage()))
                    && SystemClock.elapsedRealtime() + 200 < deadline) handler.postDelayed(this::capture, 200);
                else fail(failure);
            } catch (Exception failure) { fail(failure); }
        }
        private void complete() throws Exception {
            if (!before.toString().equals(environment(context, target).toString())) throw new IllegalStateException("INSTALLATION_CHANGED");
            report.put("result", "observed");
            write(report);
            cleanup();
        }
        private void fail(Exception failure) {
            String reason = failure.getMessage();
            if (reason == null || !reason.matches("[A-Z_]{1,64}")) reason = "MEASUREMENT_FAILED";
            try { write(new JSONObject().put("schemaVersion", 1).put("result", "failed").put("reason", reason)); }
            catch (Exception unavailable) { /* Host timeout truthfully reports a missing result. */ }
            cleanup();
        }
        private void write(JSONObject value) throws Exception {
            byte[] bytes = value.toString().getBytes(StandardCharsets.UTF_8);
            if (bytes.length > 24_000) throw new IllegalStateException("REPORT_TOO_LARGE");
            File destination = new File(context.getFilesDir(), request + ".json"), temporary = new File(context.getFilesDir(), request + ".tmp");
            if (destination.exists() || temporary.exists()) throw new IllegalStateException("REQUEST_ALREADY_USED");
            try (FileOutputStream output = new FileOutputStream(temporary)) { output.write(bytes); output.getFD().sync(); }
            if (!temporary.renameTo(destination)) throw new IllegalStateException("REPORT_COMMIT_FAILED");
        }
        private void closeActivity() { if (returnToScreen != null) { returnToScreen.run(); returnToScreen = null; } }
        private void cleanup() {
            if (done) return;
            done = true; handler.removeCallbacksAndMessages(null);
            try {
                if (owner == generation && service != null) service.end();
                closeActivity();
                if (owner == generation && "self-test".equals(mode) && FixtureActivity.active() != null) FixtureActivity.active().finish();
            } finally { completed.run(); }
        }
    }
    private static JSONObject environment(Context context, String target) throws Exception {
        var info = context.getPackageManager().getPackageInfo(target, PackageManager.GET_SIGNING_CERTIFICATES);
        if (info.signingInfo == null || info.signingInfo.hasMultipleSigners() || info.signingInfo.getApkContentsSigners().length != 1)
            throw new IllegalStateException("SIGNING_IDENTITY_UNSUPPORTED");
        JSONArray signers = new JSONArray(), history = new JSONArray();
        for (var certificate : info.signingInfo.getApkContentsSigners()) signers.put(Neutral.digest(certificate.toByteArray()));
        var lineage = info.signingInfo.getSigningCertificateHistory();
        if (lineage == null) throw new IllegalStateException("SIGNING_HISTORY_UNAVAILABLE");
        for (var certificate : lineage) history.put(Neutral.digest(certificate.toByteArray()));
        var config = context.getResources().getConfiguration();
        return new JSONObject().put("packageSha256", Neutral.digest(target)).put("versionCode", info.getLongVersionCode())
            .put("signingSha256", signers).put("signingHistorySha256", history).put("api", Build.VERSION.SDK_INT)
            .put("manufacturer", Build.MANUFACTURER).put("model", Build.MODEL).put("deviceLocale", config.getLocales().get(0).toLanguageTag())
            .put("fontScalePercent", Math.round(config.fontScale * 100)).put("densityDpi", config.densityDpi).put("orientation", config.orientation)
            .put("emulatorHint", Build.HARDWARE.startsWith("ranchu") || Build.FINGERPRINT.startsWith("generic"));
    }
}
