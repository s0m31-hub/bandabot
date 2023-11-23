package org.nwolfhub.bandabot.telegram.requests;

public class PendingRequest {
    public Thread thread;
    public Object request;
    public Class<?> bindsTo;

    public Thread getThread() {
        return thread;
    }

    public PendingRequest setThread(Thread thread) {
        this.thread = thread;
        return this;
    }

    public Object getRequest() {
        return request;
    }

    public PendingRequest setRequest(Object request) {
        this.request = request;
        return this;
    }

    public Class<?> getBindsTo() {
        return bindsTo;
    }

    public PendingRequest setBindsTo(Class<?> bindsTo) {
        this.bindsTo = bindsTo;
        return this;
    }
}
