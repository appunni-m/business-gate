package io.github.appunnim.businessgate.connected;

/** A short-lived receipt for an action just dispatched by this session. No text or message data. */
public record ActionEcho(String owner,int windowId,String className,String resource,long sentAt) {
    public boolean matches(String eventOwner,int eventAction,int eventWindow,String eventClass,long eventTime,String sourceResource,boolean sourcePresent){
        return owner.equals(eventOwner)&&eventAction==16&&windowId==eventWindow&&className.equals(eventClass)
            &&eventTime>=sentAt&&eventTime-sentAt<=1500
            &&(!sourcePresent||java.util.Objects.equals(resource,sourceResource));
    }
}
