package com.nova.ai;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;

/** Lightweight local agent loop. Planning is deterministic and permission-aware; model providers can be attached later. */
public final class NovaAgent {
    public interface Callback { void onComplete(JSONObject result); }

    private final Context context;
    private final NovaRuntime runtime;

    public NovaAgent(Context context) {
        this.context = context.getApplicationContext();
        this.runtime = NovaRuntime.get(this.context);
    }

    public void handle(String request, Callback callback) {
        JSONObject plan = plan(request);
        JSONObject result = execute(plan);
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
            if (text.isEmpty()) {
                plan.put("status", "invalid");
                return plan;
            }
            String lower = text.toLowerCase();
            if (lower.startsWith("open ")) {
                JSONObject step = new JSONObject();
                step.put("tool", "android.open_app");
                step.put("target", text.substring(5).trim());
                steps.put(step);
            } else if (lower.contains("remember")) {
                JSONObject step = new JSONObject();
                step.put("tool", "memory.store");
                step.put("value", text);
                steps.put(step);
            } else {
                JSONObject step = new JSONObject();
                step.put("tool", "assistant.respond");
                step.put("value", text);
                steps.put(step);
            }
            plan.put("status", "planned");
        } catch (Exception ignored) { }
        return plan;
    }

    private JSONObject execute(JSONObject plan) {
        JSONObject result = new JSONObject();
        try {
            result.put("planId", plan.optString("id"));
            result.put("status", plan.optString("status", "unknown"));
            result.put("steps", plan.optJSONArray("steps"));
            result.put("executed", false);
            // Execution is deliberately delegated to NovaActionEngine/approved tools.
            // This keeps the planner from gaining unrestricted device access.
        } catch (Exception ignored) { }
        return result;
    }
}
