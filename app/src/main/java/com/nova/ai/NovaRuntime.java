package com.nova.ai;

import android.content.Context;
import org.json.JSONObject;

/** Single application-level coordinator. Keeps AI, memory, actions and device transport loosely coupled. */
public final class NovaRuntime {
    private static volatile NovaRuntime instance;
    private final Context context;
    private final NovaMemory memory;
    private final NovaActionEngine actions;
    private NovaDeviceGateway gateway;

    private NovaRuntime(Context context) {
        this.context = context.getApplicationContext();
        this.memory = new NovaMemory(this.context);
        this.actions = new NovaActionEngine(this.context);
    }

    public static NovaRuntime get(Context context) {
        if (instance == null) synchronized (NovaRuntime.class) {
            if (instance == null) instance = new NovaRuntime(context);
        }
        return instance;
    }

    public NovaMemory memory() { return memory; }
    public NovaActionEngine actions() { return actions; }

    public synchronized void attachGateway(NovaDeviceGateway.Listener listener) {
        if (gateway == null) gateway = new NovaDeviceGateway(listener);
    }

    public synchronized boolean sendDeviceEvent(JSONObject event) {
        return gateway != null && gateway.send(event);
    }

    public synchronized void connectGateway(String wsUrl) {
        if (gateway == null) attachGateway(new NovaDeviceGateway.Listener() {
            @Override public void onEvent(JSONObject event) { }
            @Override public void onState(String state) { }
        });
        gateway.connect(wsUrl);
    }

    public synchronized void disconnectGateway() {
        if (gateway != null) gateway.disconnect();
    }
}
