package io.github.appunnim.businessgate.measure;

import android.app.Notification;
import android.app.Person;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import org.json.JSONArray;
import org.json.JSONObject;

/** Explicit, shell-protected metadata measurement. Never reads or exports message bodies. */
public final class NotificationMeasurementService extends NotificationListenerService {
    private static NotificationMeasurementService connected;
    @Override public void onListenerConnected() { connected = this; }
    @Override public void onListenerDisconnected() { if (connected == this) connected = null; }
    @Override public void onDestroy() { if (connected == this) connected = null; super.onDestroy(); }
    static NotificationMeasurementService connected() { return connected; }

    JSONObject capture(String target, String sender, String receiver) throws Exception {
        StatusBarNotification[] active = getActiveNotifications();
        if (active == null || active.length > 128) throw new IllegalStateException("NOTIFICATION_SNAPSHOT_UNAVAILABLE");
        JSONArray records = new JSONArray();
        for (StatusBarNotification notice : active) {
            if (!target.equals(notice.getPackageName()) || !android.os.Process.myUserHandle().equals(notice.getUser())) continue;
            Notification n = notice.getNotification();
            JSONObject row = new JSONObject().put("keySha256", Neutral.digest(notice.getKey()))
                .put("postTime", notice.getPostTime()).put("groupSummary", (n.flags & Notification.FLAG_GROUP_SUMMARY) != 0)
                .put("categoryMessage", Notification.CATEGORY_MESSAGE.equals(n.category))
                .put("shortcut", identity(n.getShortcutId(), sender, receiver))
                .put("contentIntentPresent", n.contentIntent != null);
            if (n.contentIntent != null) row.put("contentIntentCreatorMatches", target.equals(n.contentIntent.getCreatorPackage()))
                .put("contentIntentUserMatches", notice.getUser().equals(n.contentIntent.getCreatorUserHandle()))
                .put("contentIntentActivity", android.os.Build.VERSION.SDK_INT >= 31 && n.contentIntent.isActivity()).put("contentIntentImmutable", android.os.Build.VERSION.SDK_INT >= 31 && n.contentIntent.isImmutable());
            Notification.Style recovered = Notification.Builder.recoverBuilder(this, n).getStyle();
            Notification.MessagingStyle style = recovered instanceof Notification.MessagingStyle messaging ? messaging : null;
            row.put("messagingStyle", style != null);
            if (style != null) {
                row.put("groupConversation", style.isGroupConversation()).put("user", person(style.getUser(), sender, receiver));
                var messages = style.getMessages();
                if (messages.size() > 64) throw new IllegalStateException("NOTIFICATION_MESSAGE_LIMIT");
                JSONArray people = new JSONArray();
                for (var message : messages) people.put(person(message.getSenderPerson(), sender, receiver));
                row.put("messageSenders", people);
            }
            if (android.os.Build.VERSION.SDK_INT >= 31) {
                Ranking ranking = new Ranking();
                if (getCurrentRanking().getRanking(notice.getKey(), ranking)) {
                    var shortcut = ranking.getConversationShortcutInfo();
                    JSONObject summary = new JSONObject().put("present", shortcut != null);
                    if (shortcut != null) {
                        summary.put("id", identity(shortcut.getId(), sender, receiver));
                        summary.put("intentPresent", shortcut.getIntent() != null);
                        if (shortcut.getIntent() != null) {
                            var intent = shortcut.getIntent();
                            summary.put("data", identity(intent.getDataString(), sender, receiver));
                            JSONArray extraFields = new JSONArray();
                            android.os.Bundle extras = intent.getExtras();
                            if (extras != null && extras.size() <= 32) for (String key : extras.keySet()) {
                                @SuppressWarnings("deprecation") Object value = extras.get(key);
                                JSONObject field = new JSONObject().put("field", Neutral.safeIdentifier(key) ? key : "restricted-field");
                                if (value instanceof String text) field.put("string", identity(text, sender, receiver));
                                else field.put("type", value == null ? "null" : Neutral.safeIdentifier(value.getClass().getName()) ? value.getClass().getName() : "restricted-type");
                                extraFields.put(field);
                            }
                            summary.put("extras", extraFields);
                        }
                    }
                    row.put("rankingShortcut", summary);
                }
            }
            row.put("subTextIdentity", identity(string(n.extras.getCharSequence(Notification.EXTRA_SUB_TEXT)), sender, receiver));
            records.put(row);
        }
        return new JSONObject().put("notifications", records).put("messageBodiesRead", false).put("rawIdentitiesExported", false);
    }
    private static String string(CharSequence value) { return value == null ? null : value.toString(); }
    private static JSONObject person(Person p, String sender, String receiver) throws Exception {
        JSONObject result = new JSONObject().put("present", p != null);
        if (p == null) return result;
        return result.put("uri", identity(p.getUri(), sender, receiver)).put("key", identity(p.getKey(), sender, receiver))
            .put("nameIdentity", identity(string(p.getName()), sender, receiver)).put("bot", p.isBot());
    }
    private static JSONObject identity(String value, String sender, String receiver) throws Exception {
        JSONObject result = new JSONObject().put("present", value != null);
        if (value == null) return result;
        if (value.length() > 512) return result.put("tooLong", true);
        String phone = value.startsWith("tel:") ? value.substring(4) : value;
        return result.put("length", value.length()).put("sha256", Neutral.digest(value))
            .put("telUri", value.startsWith("tel:")).put("internationalPhone", Neutral.internationalPhoneSyntax(phone))
            .put("expectedSender", Neutral.phoneMatches(phone, sender)).put("expectedReceiver", Neutral.phoneMatches(phone, receiver))
            .put("containsSenderDigits", sender != null && value.contains(sender.substring(1)))
            .put("containsReceiverDigits", receiver != null && value.contains(receiver.substring(1)));
    }
}
