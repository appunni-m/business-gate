package io.github.appunnim.businessgate.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.os.Build;
import android.os.SystemClock;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import io.github.appunnim.businessgate.GateApplication;
import io.github.appunnim.businessgate.automation.AdapterRegistry;
import io.github.appunnim.businessgate.data.GateRepository;
import io.github.appunnim.businessgate.policy.Model.*;
import io.github.appunnim.businessgate.policy.NameVisibilityPolicy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

/** Automatic candidate capture; all navigation and cancellation require a visible session. */
public final class GateNotificationListener extends NotificationListenerService {
    private static final int CAP = 128;
    private static final long TTL = 15 * 60_000;
    private static GateNotificationListener connected;
    public record Candidate(String id, String key, String selectedPackage, long postTime,
                            long seenAt, long namespace, Binding binding, PendingIntent route, boolean postedReceived) {}
    static final class Claim {
        final Candidate candidate;
        final long startedAt = SystemClock.elapsedRealtime();
        boolean invalid, opened, removedByApp, dismissalRequested, removedByGate;
        String verifiedPhone="", verifiedName="";
        long identityAt;
        Claim(Candidate candidate) { this.candidate = candidate; }
    }
    private final LinkedHashMap<String, Candidate> candidates = new LinkedHashMap<>();
    private GateRepository repository;
    private Claim claim;
    private long namespace = -1;
    private final Runnable settingsChanged = () -> {
        if (repository == null) return;
        if (!enabled() || namespace != repository.current().namespace()) clear();
        namespace = repository.current().namespace();
    };
    private GateApplication app() { return (GateApplication) getApplication(); }
    private boolean enabled() {
        return repository != null && repository.current().loaded() && repository.consented()
            && repository.optionEnabled("discovery") && repository.current().binding().bound();
    }
    @Override public void onCreate() {
        super.onCreate(); repository = app().repository(); repository.addListener(settingsChanged); settingsChanged.run();
    }
    @Override public void onListenerConnected() { connected = this; refresh(); }
    @Override public void onNotificationPosted(StatusBarNotification notice) { capture(notice, true); }
    @Override public void onNotificationRemoved(StatusBarNotification notice, RankingMap ranking, int reason) {
        candidates.remove(notice.getKey());
        if (claim != null && claim.candidate.key().equals(notice.getKey())) {
            if (claim.dismissalRequested && reason == REASON_LISTENER_CANCEL) claim.removedByGate = true;
            else if (claim.opened && (reason == REASON_APP_CANCEL || reason == REASON_APP_CANCEL_ALL || reason == REASON_CLICK)) claim.removedByApp = true;
            else claim.invalid = true;
        }
    }
    private boolean installation(StatusBarNotification notice) {
        return notice != null && enabled() && android.os.Process.myUserHandle().equals(notice.getUser())
            && repository.current().binding().packageDigest().equals(AdapterRegistry.sha256(notice.getPackageName().getBytes(StandardCharsets.UTF_8)))
            && app().registry().measuredRoute(this, notice.getPackageName()) != null;
    }
    private boolean eligible(StatusBarNotification notice) {
        if (!installation(notice) || Build.VERSION.SDK_INT < 31) return false;
        Notification n = notice.getNotification();
        if ((n.flags & Notification.FLAG_GROUP_SUMMARY) != 0 || n.extras == null
            || !n.extras.containsKey(Notification.EXTRA_IS_GROUP_CONVERSATION)
            || n.extras.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION, true)
            || !Notification.MessagingStyle.class.getName().equals(n.extras.getString(Notification.EXTRA_TEMPLATE))) return false;
        PendingIntent route = n.contentIntent;
        return route != null && route.isActivity() && route.isImmutable()
            && notice.getPackageName().equals(route.getCreatorPackage()) && notice.getUser().equals(route.getCreatorUserHandle());
    }
    private void capture(StatusBarNotification notice, boolean posted) {
        try {
            if (!enabled()) { clear(); return; }
            prune();
            if (notice == null) return;
            String key = notice.getKey();
            Candidate old = candidates.get(key);
            boolean eligible = eligible(notice);
            if (posted && eligible && old != null && !old.postedReceived() && same(old, notice)) {
                // A current snapshot can precede its first queued callback. Acknowledge that
                // delivery without manufacturing a replacement or changing the selected ID.
                candidates.put(key, new Candidate(old.id(), old.key(), old.selectedPackage(), old.postTime(),
                    old.seenAt(), old.namespace(), old.binding(), old.route(), true));
                return;
            }
            if (posted && claim != null && claim.candidate.key().equals(key)) claim.invalid = true;
            if (!eligible) { candidates.remove(key); return; }
            if (!posted && old != null && same(old, notice)) return;
            candidates.put(key, new Candidate(UUID.randomUUID().toString(), key, notice.getPackageName(), notice.getPostTime(),
                SystemClock.elapsedRealtime(), repository.current().namespace(), repository.current().binding(), notice.getNotification().contentIntent, posted));
            while (candidates.size() > CAP) candidates.remove(candidates.keySet().iterator().next());
        } catch (RuntimeException unavailable) { if (claim != null) claim.invalid = true; }
    }
    private boolean same(Candidate candidate, StatusBarNotification notice) {
        return candidate.key().equals(notice.getKey()) && candidate.postTime() == notice.getPostTime()
            && candidate.selectedPackage().equals(notice.getPackageName()) && candidate.route().equals(notice.getNotification().contentIntent);
    }
    private void prune() {
        long now = SystemClock.elapsedRealtime();
        candidates.values().removeIf(c -> now < c.seenAt() || now - c.seenAt() > TTL
            || c.namespace() != repository.current().namespace() || !c.binding().equals(repository.current().binding()));
    }
    private void refresh() {
        if (!enabled()) { clear(); return; }
        try {
            StatusBarNotification[] active = getActiveNotifications();
            if (active == null || active.length > CAP) { clear(); return; }
            java.util.Set<String> keys = new java.util.HashSet<>();
            for (StatusBarNotification notice : active) { keys.add(notice.getKey()); capture(notice, false); }
            candidates.keySet().removeIf(key -> !keys.contains(key));
        } catch (RuntimeException unavailable) { clear(); }
    }
    public static List<Candidate> pending() {
        if (connected == null) return List.of();
        connected.refresh(); return List.copyOf(new ArrayList<>(connected.candidates.values()));
    }
    static Claim claim(String id) {
        if (connected == null) return null;
        connected.refresh();
        Candidate selected = connected.candidates.values().stream().filter(c -> c.id().equals(id)).findFirst().orElse(null);
        if (selected == null) return null;
        if (connected.claim != null) connected.claim.invalid = true;
        connected.claim = new Claim(selected); return connected.claim;
    }
    static boolean valid(Claim value) {
        if (connected == null || value == null || connected.claim != value || value.invalid || !connected.enabled()) return false;
        Candidate c = value.candidate; long now = SystemClock.elapsedRealtime();
        return now >= value.startedAt && now - value.startedAt < 60_000
            && connected.repository.current().namespace() == c.namespace() && connected.repository.current().binding().equals(c.binding())
            && connected.app().registry().measuredRoute(connected, c.selectedPackage()) != null && connected.current(value);
    }
    private boolean current(Claim value) {
        try {
            var notices = getActiveNotifications(new String[]{value.candidate.key()});
            return notices != null && (notices.length == 1 && eligible(notices[0]) && same(value.candidate, notices[0])
                || notices.length == 0 && value.opened && (value.removedByApp || value.removedByGate));
        } catch (RuntimeException unavailable) { return false; }
    }
    static boolean open(Claim value) {
        if (!valid(value) || Build.VERSION.SDK_INT < 36 || value.opened) return false;
        try {
            StatusBarNotification[] active = connected.getActiveNotifications(new String[]{value.candidate.key()});
            if (active == null || active.length > 1) return false;
            if (active.length == 1 && (!connected.eligible(active[0]) || !connected.same(value.candidate, active[0]))) return false;
            if (active.length == 0 && !(value.opened && value.removedByApp)) return false;
            value.opened = true;
            var options = android.app.ActivityOptions.makeBasic().setPendingIntentBackgroundActivityStartMode(
                android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_IF_VISIBLE);
            value.candidate.route().send(options.toBundle()); return true;
        } catch (PendingIntent.CanceledException | RuntimeException unavailable) { value.invalid = true; return false; }
    }
    /** Invoked only while the session's Stop overlay and native postcondition remain current. */
    static String dismiss(Claim value, String phone, String name, long revision, long epoch) {
        if (!valid(value)) return "INCOMING_CHANGED";
        if (!value.opened || !value.verifiedPhone.equals(phone) || !value.verifiedName.equals(NameVisibilityPolicy.nameKey(name))
            || SystemClock.elapsedRealtime()<value.identityAt || SystemClock.elapsedRealtime()-value.identityAt>45_000) return "NOTIFICATION_IDENTITY_UNVERIFIED";
        GateRepository r = connected.repository; var s = r.current();
        if (r.disarmed() || r.epoch() != epoch || s.globalRevision() != revision || !s.enabled() || s.paused()) return "POLICY_CHANGED";
        Account account = s.accounts().stream().filter(a -> a.phone().equals(phone)).findFirst().orElse(null);
        var policy = r.nameVisibilityPolicy();
        if (account == null || policy == null || account.kind() != Kind.BUSINESS_CONFIRMED || account.blockState() != BlockState.BLOCKED
            || NameVisibilityPolicy.evaluate(policy, new NameVisibilityPolicy.Observation(policy.scope(), policy.revision(), true, phone, name, Kind.BUSINESS_CONFIRMED)).visibility() != NameVisibilityPolicy.Visibility.HIDE) return "NOTIFICATION_KEPT";
        try {
            StatusBarNotification[] active = connected.getActiveNotifications(new String[]{value.candidate.key()});
            if (active == null || active.length > 1) return "NOTIFICATION_UNAVAILABLE";
            if (active.length == 0) return value.removedByApp ? "NOTIFICATION_REMOVED_BY_CONNECTED_APP" : "NOTIFICATION_UNAVAILABLE";
            if (!connected.eligible(active[0]) || !connected.same(value.candidate, active[0])) return "INCOMING_CHANGED";
            value.dismissalRequested = true;
            connected.cancelNotification(value.candidate.key());
            return "NOTIFICATION_DISMISSAL_REQUESTED";
        } catch (RuntimeException unavailable) { return "NOTIFICATION_DISMISSAL_FAILED"; }
    }
    /** Receipts are created only after the measured profile and receiving-account route agree. */
    static boolean bindIdentity(Claim value,String phone,String name,String receiver) {
        if (!valid(value) || !value.opened || !value.candidate.binding().receiver().equals(receiver)) return false;
        try {
            value.verifiedPhone=io.github.appunnim.businessgate.policy.Identity.canonicalPhone(phone);
            value.verifiedName=NameVisibilityPolicy.nameKey(name);value.identityAt=SystemClock.elapsedRealtime();return true;
        }catch(IllegalArgumentException invalid){return false;}
    }
    static String dismissalResult(Claim value) {
        if (!valid(value)) return "INCOMING_CHANGED";
        if (value.removedByGate) return "NOTIFICATION_DISMISSED";
        if (value.removedByApp) return "NOTIFICATION_REMOVED_BY_CONNECTED_APP";
        return "NOTIFICATION_DISMISSAL_REQUESTED";
    }
    static void release(Claim value) {
        if (value == null) return;
        value.invalid = true;
        if (connected != null && connected.claim == value) connected.claim = null;
    }
    private void clear() { candidates.clear(); if (claim != null) claim.invalid = true; }
    @Override public void onListenerDisconnected() { clear(); if (connected == this) connected = null; }
    @Override public void onDestroy() {
        clear(); if (connected == this) connected = null;
        if (repository != null) repository.removeListener(settingsChanged); super.onDestroy();
    }
}
