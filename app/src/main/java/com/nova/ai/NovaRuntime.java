package com.nova.ai;

import android.content.Context;
import org.json.JSONObject;

/** Single application-level coordinator for local intelligence, tools and remote device transport. */
public final class NovaRuntime {
    private static volatile NovaRuntime instance;
    private final Context context;
    private final NovaMemory memory;
    private final NovaActionEngine actions;
    private final NovaToolRegistry tools;
    private final NovaToolExecutor executor;
    private NovaDeviceGateway gateway;
    private NovaDeviceCommandHandler commandHandler;

    private NovaRuntime(Context context) {
        this.context = context.getApplicationContext();
        this.memory = new NovaMemory(this.context);
        this.actions = new NovaActionEngine(this.context);
        this.tools = new NovaToolRegistry();
        this.tools.register(NovaBuiltInTools.echo());
        this.tools.register(NovaBuiltInTools.contextAppend(this.context));
        this.tools.register(new NovaDeviceInfoTool());
        this.tools.register(new NovaAppLauncherTool(this.context));
        this.executor = new NovaToolExecutor(this.context, tools);
    }

    public static NovaRuntime get(Context context) {
        if (instance == null) synchronized (NovaRuntime.class) {
            if (instance == null) instance = new NovaRuntime(context);
        }
        return instance;
    }

    public NovaMemory memory() { return memory; }
    public NovaActionEngine actions() { return actions; }
    public NovaToolRegistry tools() { return tools; }
    public NovaToolExecutor executor() { return executor; }

    public synchronized void attachGateway(final NovaDeviceGateway.Listener externalListener) {
        if (gateway != null) return;
        gateway = new NovaDeviceGateway(new NovaDeviceGateway.Listener() {
            @Override public void onEvent(JSONObject event) {
                if (event != null && NovaProtocol.COMMAND.equals(event.optString("type"))) {
                    NovaDeviceCommandHandler handler = commandHandler;
                    if (handler == null) handler = new NovaDeviceCommandHandler(actions, NovaAccessibilityService.getInstance());
                    final NovaDeviceCommandHandler active = handler;
                    active.handle(event, result -> sendDeviceEvent(result));
                }
                if (externalListener != null) externalListener.onEvent(event);
            }
            @Override public void onState(String state) {
                if (externalListener != null) externalListener.onState(state);
            }
        });
    }

    public synchronized void setCommandHandler(NovaDeviceCommandHandler handler) { commandHandler = handler; }
    public synchronized boolean sendDeviceEvent(JSONObject event) { return gateway != null && gateway.send(event); }
    public synchronized void connectGateway(String wsUrl) { if (gateway == null) attachGateway(null); gateway.connect(wsUrl); }
    public synchronized void disconnectGateway() { if (gateway != null) gateway.disconnect(); }
}
