package io.github.appunnim.businessgate.policy;

import java.util.HashSet;
import java.util.Set;

/** Navigation-independent limits for future qualified batches and explicit scans. */
public final class SessionBudget {
    private final long started;
    private final boolean scan;
    private final Set<String> resolved=new HashSet<>();
    private int mutations,unchangedPages;
    private boolean stopped;
    public SessionBudget(long elapsedStart,boolean explicitScan){started=elapsedStart;scan=explicitScan;}
    public boolean mayContinue(long now){return !stopped&&now>=started&&now-started<(scan?120_000:25_000)&&mutations<5&&(!scan||resolved.size()<100)&&unchangedPages<3;}
    public boolean resolve(String phone,long now){if(!mayContinue(now)||resolved.size()>=(scan?100:5))return false;return resolved.add(Identity.canonicalPhone(phone));}
    public boolean reserveMutation(long now){if(!mayContinue(now))return false;mutations++;return true;}
    public void pageProgress(boolean changed){unchangedPages=changed?0:unchangedPages+1;}
    public void stop(){stopped=true;}
    public int resolvedCount(){return resolved.size();}
}
