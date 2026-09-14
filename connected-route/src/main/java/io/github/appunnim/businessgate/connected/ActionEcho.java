package io.github.appunnim.businessgate.connected;

/** A short-lived receipt for an action just dispatched by this session. No text or message data. */
public record ActionEcho(String owner,int windowId,String className,String resource,long sentAt) {
    /** A deferred event may lose the action field. Only the retained exact node can match it. */
    public boolean matchesDeferred(String eventOwner,int eventAction,int eventWindow,String eventClass,long eventTime,String sourceResource,boolean sameNode){
        return eventAction==0&&sameNode&&resource!=null&&eventTime>=sentAt&&eventTime-sentAt<=250
            &&matches(eventOwner,16,eventWindow,eventClass,eventTime,sourceResource,true);
    }
    public boolean matches(String eventOwner,int eventAction,int eventWindow,String eventClass,long eventTime,String sourceResource,boolean sourcePresent){
        return owner.equals(eventOwner)&&eventAction==16&&windowId==eventWindow&&className.equals(eventClass)
            &&eventTime>=sentAt&&eventTime-sentAt<=1500
            &&(!sourcePresent||java.util.Objects.equals(resource,sourceResource));
    }
}
