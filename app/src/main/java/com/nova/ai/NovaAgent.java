package com.nova.ai;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;

/** Agent facade backed by the shared application runtime and optionally a real model loop. */
public final class NovaAgent {
    public interface Callback { void onComplete(JSONObject result); }

    private final NovaRuntime runtime;
    private final NovaContextStore contextStore;
    private final NovaModelLoop modelLoop;
    private NovaAiProvider provider;

    public NovaAgent(Context context) {
        Context app = context.getApplicationContext();
        runtime = NovaRuntime.get(app);
        contextStore = new NovaContextStore(app);
        modelLoop = new NovaModelLoop(app);
    }

    public void setProvider(NovaAiProvider provider) {
        this.provider = provider;
        modelLoop.setProvider(provider);
    }

    public NovaToolRegistry tools() { return runtime.tools(); }
    public NovaContextStore context() { return contextStore; }

    public void handle(String request, Callback callback) {
        String text = request == null ? "" : request.trim();
        if (text.isEmpty()) {
            JSONObject result = new JSONObject();
            try { result.put("ok", false); result.put("error", "empty_request"); } catch (Exception ignored) { }
            if (callback != null) callback.onComplete(result);
            return;
        }
        if (provider != null) {
            modelLoop.run(text, result -> {
                contextStore.add("assistant", result.optString("text", ""));
                if (callback != null) callback.onComplete(result);
            });
            return;
        }
        callbackIfPresent(callback, executeLocal(text));
    }

    /** Compatibility plan view used by diagnostics and UI when no provider is configured. */
    public JSONObject plan(String request) {
        JSONObject plan = new JSONObject();
        try {
            String text = request == null ? "" : request.trim();
            plan.put("id", NovaProtocol.id());
            plan.put("request", text);
            plan.put("status", text.isEmpty() ? "invalid" : "planned");
            JSONArray steps = new JSONArray();
            if (!text.isEmpty()) steps.put(new JSONObject()
                    .put("id", NovaProtocol.id())
                    .put("tool", "nova.echo")
                    .put("input", new JSONObject().put("text", text)));
            plan.put("steps", steps);
        } catch (Exception ignored) { }
        return plan;
    }

    private JSONObject executeLocal(String text) {
        JSONObject result = new JSONObject();
        try {
            JSONObject plan = plan(text);
            result.put("ok", "planned".equals(plan.optString("status")));
            result.put("plan", plan);
            NovaToolExecutor executor = runtime.executor();
            JSONObject input = new JSONObject().put("text", text);
            result.put("execution", executor.execute("nova.echo", input));
            contextStore.add("user", text);
        } catch (Exception e) {
            try { result.put("ok", false); result.put("error", e.getClass().getSimpleName()); } catch (Exception ignored) { }
        }
        return result;
    }

    private static void callbackIfPresent(Callback callback, JSONObject result) {
        if (callback != null) callback.onComplete(result);
    }
}
