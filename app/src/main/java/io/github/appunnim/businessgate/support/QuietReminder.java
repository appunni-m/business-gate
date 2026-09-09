package io.github.appunnim.businessgate.support;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import io.github.appunnim.businessgate.R;
import io.github.appunnim.businessgate.data.GateRepository;
import io.github.appunnim.businessgate.ui.MainActivity;

/** Useful, permission-gated and capped. A tap never grants mutation authority. */
public final class QuietReminder {
    private final Context context;
    private final GateRepository repository;
    public QuietReminder(Context context,GateRepository repository){this.context=context;this.repository=repository;}
    public void onNewReviewEvidence(){
        if(!repository.current().digest())return;
        if(Build.VERSION.SDK_INT>=33&&context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;
        NotificationManager manager=context.getSystemService(NotificationManager.class);
        if(!manager.areNotificationsEnabled())return;
        repository.reserveReminder(()->{
            NotificationChannel channel=new NotificationChannel("attention","Optional review",NotificationManager.IMPORTANCE_LOW);
            channel.setSound(null,null);channel.enableVibration(false);channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);manager.createNotificationChannel(channel);
            PendingIntent open=PendingIntent.getActivity(context,0,new Intent(context,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
            manager.notify(1,new Notification.Builder(context,"attention").setSmallIcon(R.drawable.ic_gate).setContentTitle(context.getString(R.string.reminder_title))
                .setContentText(context.getString(R.string.reminder_detail)).setContentIntent(open).setAutoCancel(true).setOnlyAlertOnce(true).setVisibility(Notification.VISIBILITY_PRIVATE).build());
        });
    }
    public void clear(){context.getSystemService(NotificationManager.class).cancel(1);}
}
