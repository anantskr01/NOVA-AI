package com.nova.ai;

import android.content.Context;
import org.json.JSONObject;

/** Single application-level coordinator for local intelligence, tools and remote device transport. */
public final class NovaRuntime {
    private static volatile NovaRuntime instance;
    private final Context context;
    private final NovaMemory memory;
    private final NovaLongTermMemory longTermMemory;
    private final NovaMemoryManager memoryManager;
    private final NovaActionEngine actions;
    private final NovaToolRegistry tools;
    private final NovaToolExecutor executor;
    private final NovaDeviceRegistry devices;
    private final NovaTaskOrchestrator orchestrator;
    private final NovaProductionGuard productionGuard;
    private NovaDeviceGateway gateway;
    private NovaDeviceCommandHandler commandHandler;

    private NovaRuntime(Context context) {
        this.context = context.getApplicationContext();
        this.memory = new NovaMemory(this.context);
        this.longTermMemory = new NovaLongTermMemory(this.context);
        this.memoryManager = new NovaMemoryManager(this.context);
        this.actions = new NovaActionEngine(this.context);
        this.tools = new NovaToolRegistry();
        this.devices = new NovaDeviceRegistry();
        this.orchestrator = new NovaTaskOrchestrator();
        registerBuiltIns();
        this.executor = new NovaToolExecutor(this.context, tools);
        this.productionGuard = new NovaProductionGuard(this.context);
    }

    private void registerBuiltIns() {
        tools.register(NovaBuiltInTools.echo());
        tools.register(NovaBuiltInTools.contextAppend(context));
        tools.register(new NovaDeviceInfoTool());
        tools.register(new NovaAppLauncherTool(context));
        tools.register(NovaMemoryToolsV2.remember(context));
        tools.register(NovaMemoryToolsV2.recall(context));
        tools.register(NovaMemoryToolsV2.forget(context));
        tools.register(new NovaAndroidActionTool(context));
        tools.register(new NovaWebTool(context));
    }

    public static NovaRuntime get(Context context) {
        if (context == null) throw new IllegalArgumentException("context");
        if (instance == null) synchronized (NovaRuntime.class) { if (instance == null) instance = new NovaRuntime(context); }
        return instance;
    }
    public NovaMemory memory() { return memory; }
    public NovaLongTermMemory longTermMemory() { return longTermMemory; }
    public NovaMemoryManager memoryManager() { return memoryManager; }
    public NovaActionEngine actions() { return actions; }
    public NovaToolRegistry tools() { return tools; }
    public NovaToolExecutor executor() { return executor; }
    public NovaDeviceRegistry devices() { return devices; }
    public NovaTaskOrchestrator orchestrator() { return orchestrator; }
    public NovaProductionGuard productionGuard() { return productionGuard; }

    public synchronized void attachGateway(final NovaDeviceGateway.Listener externalListener) {
        if (gateway != null) return;
        gateway = new NovaDeviceGateway(new NovaDeviceGateway.Listener() {
            @Override public void onEvent(JSONObject event) {
                if (event != null) {
                    String node = event.optString("nodeId", event.optString("deviceId", ""));
                    if (!node.isEmpty()) devices.upsert(node, event.optString("name", node), event.optString("platform", "unknown"), event.optString("state", "CONNECTED"));
                    if (NovaProtocol.COMMAND.equals(event.optString("type"))) {
                        NovaDeviceCommandHandler handler = commandHandler;
                        if (handler == null) handler = new NovaDeviceCommandHandler(actions, NovaAccessibilityService.getInstance());
                        commandHandler = handler;
                        handler.handle(event, result -> sendDeviceEvent(result));
                    }
                }
                if (externalListener != null) externalListener.onEvent(event);
            }
            @Override public void onState(String state) { if (externalListener != null) externalListener.onState(state); }
        });
    }
    public synchronized void setCommandHandler(NovaDeviceCommandHandler handler) { commandHandler = handler; }
    public synchronized boolean sendDeviceEvent(JSONObject event) { return gateway != null && gateway.send(event); }
    public synchronized void connectGateway(String wsUrl) { if (gateway == null) attachGateway(null); gateway.connect(wsUrl); }
    public synchronized void disconnectGateway() { if (gateway != null) gateway.disconnect(); }
}
