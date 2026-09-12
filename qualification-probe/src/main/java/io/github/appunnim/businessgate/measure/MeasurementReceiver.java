package io.github.appunnim.businessgate.measure;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Shell-protected bounded capture without pausing the observed Activity. */
public final class MeasurementReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        PendingResult pending = goAsync();
        MeasurementActivity.begin(context, intent, () -> {}, pending::finish);
    }
}
