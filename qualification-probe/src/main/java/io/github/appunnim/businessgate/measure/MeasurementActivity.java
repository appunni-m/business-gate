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
        long owner = ++generation;
        TextView text = new TextView(this); text.setText(R.string.app_name); setContentView(text);
        String request = getIntent().getStringExtra("request");
        if (state != null || request == null || !request.matches("[a-f0-9]{32}")) { finish(); return; }
        new CaptureTask(getApplicationContext(), getIntent(), request, owner, this::finish).start();
    }
    private static final class CaptureTask {
        private final Context context;
        private final String request, mode, target, surface, rawPath;
        private final long owner, deadline = SystemClock.elapsedRealtime() + 15_000;
        private final Handler handler = new Handler(Looper.getMainLooper());
        private Runnable returnToScreen;
        private JSONObject before, report;
        private List<Integer> path;
        private MeasurementService service;
        private boolean done;
        CaptureTask(Context context, Intent input, String request, long owner, Runnable returnToScreen) {
            this.context = context; this.request = request; this.owner = owner; this.returnToScreen = returnToScreen;
            mode = input.getStringExtra("mode"); target = input.getStringExtra("target");
            surface = input.getStringExtra("surface"); rawPath = input.getStringExtra("path");
        }
        void start() {
            try {
                if (target == null || !target.matches("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+")) throw new IllegalArgumentException("INVALID_INSTALLATION");
                before = environment(context, target);
                report = new JSONObject().put("schemaVersion", 1).put("mode", mode).put("environment", before)
                    .put("physicalQualification", false).put("mutationPerformed", false)
                    .put("probeApkSha256", Neutral.digest(java.nio.file.Files.readAllBytes(new File(context.getApplicationInfo().sourceDir).toPath())))
                    .put("capturedAtUtc", java.time.Instant.now().toString());
                if ("environment".equals(mode)) { complete(); return; }
                if ("self-test".equals(mode)) {
                    if (!target.equals(context.getPackageName())) throw new IllegalArgumentException("SYNTHETIC_TARGET_REQUIRED");
                    path = List.of();
                    context.startActivity(new Intent(context, FixtureActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                } else if ("node".equals(mode)) {
                    if (surface == null || !List.of("receiver", "profile", "block-confirmation", "unblock-confirmation", "synthetic").contains(surface))
                        throw new IllegalArgumentException("SURFACE_ATTESTATION_REQUIRED");
                    path = ExactPath.parse(rawPath == null ? "" : rawPath);
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
                JSONObject measurement = service.capture(path);
                report.put("measurement", measurement).put("source", "production-declaration read-only developer service");
                if ("self-test".equals(mode)) {
                    if (!measurement.getBoolean("activeFocusedWindow") || measurement.getInt("acquiredNodes") != 1)
                        throw new IllegalStateException("ROOT_CAPTURE_FAILED");
                    report.put("assertions", 3);
                }
                complete();
            } catch (RuntimeException failure) {
                if (("WINDOW_NOT_READY".equals(failure.getMessage()) || "FOREGROUND_CHANGED".equals(failure.getMessage()))
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
            done = true; handler.removeCallbacksAndMessages(null);
            if (owner == generation && service != null) service.end();
            closeActivity();
            if (owner == generation && "self-test".equals(mode) && FixtureActivity.active() != null) FixtureActivity.active().finish();
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
