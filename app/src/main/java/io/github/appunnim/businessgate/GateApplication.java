package io.github.appunnim.businessgate;

import android.app.Application;
import io.github.appunnim.businessgate.data.GateRepository;
import io.github.appunnim.businessgate.automation.AdapterRegistry;
import io.github.appunnim.businessgate.policy.AttentionLedger;

public final class GateApplication extends Application {
    private GateRepository repository;
    private AdapterRegistry registry;
    private final AttentionLedger attention=new AttentionLedger();
    @Override public void onCreate(){super.onCreate();registry=new AdapterRegistry(this);repository=new GateRepository(this);
        var reminder=new io.github.appunnim.businessgate.support.QuietReminder(this,repository);
        repository.addListener(()->{if(!repository.optionEnabled("digest")||!repository.optionEnabled("sales_hints"))reminder.clear();else reminder.onNewReviewEvidence();});}
    public void managementAttention(boolean active){attention.management(android.os.SystemClock.elapsedRealtime(),System.currentTimeMillis(),active);flushAttention();}
    public void sessionAttention(boolean active){attention.session(android.os.SystemClock.elapsedRealtime(),System.currentTimeMillis(),active);flushAttention();}
    private void flushAttention(){attention.drain(android.os.SystemClock.elapsedRealtime(),System.currentTimeMillis()).forEach(repository::attention);}
    public GateRepository repository(){return repository;}
    public AdapterRegistry registry(){return registry;}
}
