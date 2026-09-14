package io.github.appunnim.businessgate;

import android.app.Application;
import io.github.appunnim.businessgate.data.GateRepository;
import io.github.appunnim.businessgate.automation.AdapterRegistry;
import io.github.appunnim.businessgate.policy.AttentionLedger;

public final class GateApplication extends Application {
    private GateRepository repository;
    private io.github.appunnim.businessgate.data.SenderFilterStore senderFilter;
    private AdapterRegistry registry;
    private final AttentionLedger attention=new AttentionLedger();
    private final android.os.Handler handler=new android.os.Handler(android.os.Looper.getMainLooper());
    private long observedMetricsEpoch;
    private final Runnable checkpoint=new Runnable(){@Override public void run(){flushAttention();scheduleCheckpoint();}};
    @Override public void onCreate(){super.onCreate();registry=new AdapterRegistry(this);repository=new GateRepository(this);senderFilter=new io.github.appunnim.businessgate.data.SenderFilterStore(this,repository);
        var reminder=new io.github.appunnim.businessgate.support.QuietReminder(this,repository);
        repository.addListener(()->{
            synchronizeMetricsEpoch();
            if(!repository.optionEnabled("digest")||!repository.optionEnabled("sales_hints"))reminder.clear();else reminder.onNewReviewEvidence();
        });}
    private void synchronizeMetricsEpoch(){if(observedMetricsEpoch!=repository.metricsEpoch()){observedMetricsEpoch=repository.metricsEpoch();attention.reset(android.os.SystemClock.elapsedRealtime(),System.currentTimeMillis());}}
    public void managementAttention(boolean active){synchronizeMetricsEpoch();attention.management(android.os.SystemClock.elapsedRealtime(),System.currentTimeMillis(),active);flushAttention();scheduleCheckpoint();}
    public void sessionAttention(boolean active){synchronizeMetricsEpoch();attention.session(android.os.SystemClock.elapsedRealtime(),System.currentTimeMillis(),active);flushAttention();scheduleCheckpoint();}
    private void flushAttention(){synchronizeMetricsEpoch();attention.drain(android.os.SystemClock.elapsedRealtime(),System.currentTimeMillis()).forEach(repository::attention);}
    private void scheduleCheckpoint(){handler.removeCallbacks(checkpoint);if(attention.active())handler.postDelayed(checkpoint,5_000);}
    public io.github.appunnim.businessgate.data.SenderFilterStore senderFilter(){return senderFilter;}
    public GateRepository repository(){return repository;}
    public AdapterRegistry registry(){return registry;}
}
