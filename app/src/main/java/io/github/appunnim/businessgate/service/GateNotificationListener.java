package io.github.appunnim.businessgate.service;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.os.SystemClock;
import io.github.appunnim.businessgate.GateApplication;
import java.util.LinkedHashMap;

/** Opaque, bounded hints only. No body access without a qualified notification binding contract. */
public final class GateNotificationListener extends NotificationListenerService {
    private static final int CAP=128;
    private final LinkedHashMap<String,Long> hints=new LinkedHashMap<>();
    @Override public void onNotificationPosted(StatusBarNotification notice){
        GateApplication app=(GateApplication)getApplication();
        if(!app.repository().current().discovery())return;
        // Package and signing checks occur before notification extras can be accessed.
        if(app.registry().resolve(this,notice.getPackageName())==null)return;
        long now=SystemClock.elapsedRealtime();hints.values().removeIf(time->now<time||now-time>15*60_000);
        if((notice.getNotification().flags & android.app.Notification.FLAG_GROUP_SUMMARY) != 0)return;
        // Names and notification text are neither read nor used as phone identity.
        hints.put(notice.getKey(),now);while(hints.size()>CAP)hints.remove(hints.keySet().iterator().next());
    }
    @Override public void onNotificationRemoved(StatusBarNotification notice){hints.remove(notice.getKey());}
    @Override public void onListenerDisconnected(){hints.clear();}
    @Override public void onDestroy(){hints.clear();super.onDestroy();}
}
