package io.github.appunnim.businessgate.policy;

import io.github.appunnim.businessgate.policy.Model.Choice;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Bounded, process-local user intent. Never substitutes for a committed database choice. */
public final class PendingChoices {
    public static final int LIMIT=50_000;
    public record Pending(long namespace,String phone,String label,Choice choice,long baseRevision,long sequence,boolean failed) {}
    private record Key(long namespace,String phone) {}
    private final Map<Key,Pending> entries=new HashMap<>();
    private final int limit;
    public PendingChoices(){this(LIMIT);}
    public PendingChoices(int limit){if(limit<1||limit>LIMIT)throw new IllegalArgumentException("INVALID_PENDING_LIMIT");this.limit=limit;}
    public synchronized boolean put(Pending value){
        Key key=new Key(value.namespace(),value.phone());
        if(!entries.containsKey(key)&&entries.size()>=limit)return false;
        Pending old=entries.get(key);if(old!=null&&old.sequence()>=value.sequence())return false;
        entries.put(key,value);return true;
    }
    public synchronized boolean current(Pending value){Pending now=entries.get(new Key(value.namespace(),value.phone()));return now!=null&&now.sequence()==value.sequence();}
    public synchronized Pending get(long namespace,String phone){return entries.get(new Key(namespace,phone));}
    public synchronized boolean any(long namespace){return entries.values().stream().anyMatch(value->value.namespace()==namespace);}
    public synchronized List<Pending> list(long namespace){
        List<Pending> result=new ArrayList<>();for(Pending value:entries.values())if(value.namespace()==namespace)result.add(value);
        result.sort(Comparator.comparingLong(Pending::sequence));return List.copyOf(result);
    }
    public synchronized void committed(Pending value){if(current(value))entries.remove(new Key(value.namespace(),value.phone()));}
    public synchronized void failed(Pending value){if(current(value))entries.put(new Key(value.namespace(),value.phone()),new Pending(value.namespace(),value.phone(),value.label(),value.choice(),value.baseRevision(),value.sequence(),true));}
    public synchronized void clear(){entries.clear();}
}
