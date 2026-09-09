package io.github.appunnim.businessgate.policy;

/** Tracks interval union so overlapping management and session time count once. */
public final class AttentionClock {
    private long last = -1, management, occupancy, union;
    private boolean managing, applying;
    public synchronized void transition(long now, boolean managementActive, boolean sessionActive) {
        if (last >= 0 && now >= last) {
            long dt = now - last;
            if (managing) management += dt;
            if (applying) occupancy += dt;
            if (managing || applying) union += dt;
        }
        last = now; managing = managementActive; applying = sessionActive;
    }
    public synchronized long[] drain(long now) {
        transition(now, managing, applying);
        long[] result = {management, occupancy, union}; management = occupancy = union = 0; return result;
    }
}
