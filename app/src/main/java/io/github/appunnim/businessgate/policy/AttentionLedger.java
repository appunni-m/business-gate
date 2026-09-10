package io.github.appunnim.businessgate.policy;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;

/** Monotonic duration, UTC day buckets, and independent management/session ownership. */
public final class AttentionLedger {
    private long last=-1,lastWall;
    private boolean managing,applying;
    private final Map<String,long[]> days=new LinkedHashMap<>();
    public synchronized void management(long elapsed,long wall,boolean active){advance(elapsed,wall);managing=active;}
    public synchronized void session(long elapsed,long wall,boolean active){advance(elapsed,wall);applying=active;}
    public synchronized boolean active(){return managing||applying;}
    /** Clear pre-reset duration while continuing to observe any currently visible interval. */
    public synchronized void reset(long elapsed,long wall){days.clear();last=elapsed;lastWall=wall;}
    private void advance(long elapsed,long wall){
        if(last>=0&&elapsed>=last&&(managing||applying)){
            long duration=elapsed-last;
            // A clock correction never adds duration. Retain at most the observed 35-day window.
            long end=lastWall+duration;
            if(Math.abs(end-wall)>5_000)end=wall;
            long start=Math.max(end-duration,end-35L*86_400_000);
            while(start<end){
                LocalDate day=Instant.ofEpochMilli(start).atZone(ZoneOffset.UTC).toLocalDate();
                long next=Math.min(end,day.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli());
                long amount=next-start;long[] totals=days.computeIfAbsent(day.toString(),key->new long[3]);
                if(managing)totals[0]+=amount;if(applying)totals[1]+=amount;totals[2]+=amount;start=next;
            }
        }
        last=elapsed;lastWall=wall;
    }
    public synchronized Map<String,long[]> drain(long elapsed,long wall){
        advance(elapsed,wall);Map<String,long[]> result=new LinkedHashMap<>();days.forEach((day,totals)->result.put(day,totals.clone()));days.clear();return result;
    }
}
