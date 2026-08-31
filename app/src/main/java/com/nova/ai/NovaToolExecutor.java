package com.nova.ai;

import android.content.Context;
import org.json.JSONObject;

/** Executes only registered capabilities after an explicit authorization check. */
public final class NovaToolExecutor {
    private final NovaToolRegistry registry;
    private final NovaAuthorization authorization;

    public NovaToolExecutor(Context context, NovaToolRegistry registry) {
        if (context == null) throw new IllegalArgumentException("context");
        if (registry == null) throw new IllegalArgumentException("registry");
        this.registry = registry;
        this.authorization = new NovaAuthorization(context);
    }

    public JSONObject execute(String toolId, JSONObject input) {
        JSONObject result = new JSONObject();
        try {
            NovaTool tool = registry.get(toolId);
            if (tool == null) {
                result.put("ok", false);
                result.put("error", "tool_not_found");
                return result;
            }
            if (!authorization.isAllowed(toolId)) {
                result.put("ok", false);
                result.put("error", "capability_not_authorized");
                result.put("tool", toolId);
                return result;
            }
            JSONObject output = tool.execute(input == null ? new JSONObject() : input);
            result.put("ok", true);
            result.put("tool", toolId);
            result.put("output", output == null ? JSONObject.NULL : output);
        } catch (Exception e) {
            try {
                result.put("ok", false);
                result.put("tool", toolId);
                result.put("error", e.getClass().getSimpleName());
                result.put("message", e.getMessage() == null ? "tool execution failed" : e.getMessage());
            } catch (Exception ignored) { }
        }
        return result;
    }
}
