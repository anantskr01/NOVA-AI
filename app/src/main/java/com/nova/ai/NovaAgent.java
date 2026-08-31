package com.nova.ai;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;

/** Local agent loop: context -> deterministic plan -> registered tool execution. */
public final class NovaAgent {
    public interface Callback { void onComplete(JSONObject result); }

    private final NovaContextStore contextStore;
    private final NovaToolRegistry registry;
    private final NovaToolExecutor executor;

    public NovaAgent(Context context) {
        Context app = context.getApplicationContext();
        contextStore = new NovaContextStore(app);
        registry = new NovaToolRegistry();
        registry.register(NovaBuiltInTools.echo());
        registry.register(NovaBuiltInTools.contextAppend(app));
        executor = new NovaToolExecutor(registry);
    }

    public void handle(String request, Callback callback) {
        JSONObject result = execute(plan(request));
        if (callback != null) callback.onComplete(result);
    }

    public JSONObject plan(String request) {
        JSONObject plan = new JSONObject();
        JSONArray steps = new JSONArray();
        String text = request == null ? "" : request.trim();
        try {
            plan.put("id", NovaProtocol.id());
            plan.put("request", text);
            plan.put("steps", steps);
            if (text.isEmpty()) { plan.put("status", "invalid"); return plan; }

            // Until an LLM provider is attached, keep planning deterministic and safe.
            JSONObject step = new JSONObject();
            step.put("id", NovaProtocol.id());
            step.put("tool", "nova.echo");
            step.put("input", new JSONObject().put("text", text));
            steps.put(step);
            plan.put("status", "planned");
        } catch (Exception ignored) { }
        return plan;
    }

    private JSONObject execute(JSONObject plan) {
        JSONObject result = new JSONObject();
        JSONArray outputs = new JSONArray();
        try {
            String status = plan.optString("status", "unknown");
            result.put("planId", plan.optString("id"));
            result.put("status", status);
            result.put("executed", false);
            if (!"planned".equals(status)) return result;

            JSONArray steps = plan.optJSONArray("steps");
            if (steps != null) {
                for (int i = 0; i < steps.length(); i++) {
                    JSONObject step = steps.optJSONObject(i);
                    if (step == null) continue;
                    NovaToolCall call = NovaToolCall.fromJson(step);
                    outputs.put(executor.execute(call.tool, call.input));
                }
            }
            contextStore.add("user", plan.optString("request"));
            result.put("outputs", outputs);
            result.put("executed", true);
        } catch (Exception e) {
            try { result.put("error", e.getClass().getSimpleName()); } catch (Exception ignored) { }
        }
        return result;
    }
}
