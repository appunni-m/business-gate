package io.github.appunnim.businessgate;

import android.app.Application;
import io.github.appunnim.businessgate.data.GateRepository;
import io.github.appunnim.businessgate.automation.AdapterRegistry;
import io.github.appunnim.businessgate.policy.AttentionClock;

public final class GateApplication extends Application {
    private GateRepository repository;
    private AdapterRegistry registry;
    public final AttentionClock attention=new AttentionClock();
    @Override public void onCreate(){super.onCreate();registry=new AdapterRegistry(this);repository=new GateRepository(this);
        var reminder=new io.github.appunnim.businessgate.support.QuietReminder(this,repository);
        repository.addListener(()->{if(!repository.current().digest()||!repository.current().salesHints())reminder.clear();else reminder.onNewReviewEvidence();});}
    public GateRepository repository(){return repository;}
    public AdapterRegistry registry(){return registry;}
}
