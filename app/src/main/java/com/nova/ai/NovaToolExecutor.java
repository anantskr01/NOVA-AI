package com.nova.ai;

import android.content.Context;
import org.json.JSONObject;

/** Executes only registered capabilities after authorization and deterministic input validation. */
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
            String id = toolId == null ? "" : toolId.trim();
            NovaTool tool = registry.get(id);
            if (tool == null) return result.put("ok", false).put("error", "tool_not_found").put("tool", id);
            if (!authorization.isAllowed(id)) return result.put("ok", false).put("error", "capability_not_authorized").put("tool", id);
            JSONObject args = input == null ? new JSONObject() : input;
            NovaToolValidator.validate(tool, args);
            JSONObject output = tool.execute(args);
            return result.put("ok", true).put("tool", id).put("output", output == null ? JSONObject.NULL : output);
        } catch (Exception e) {
            try { result.put("ok", false).put("tool", toolId == null ? "" : toolId).put("error", e.getClass().getSimpleName()).put("message", e.getMessage() == null ? "tool execution failed" : e.getMessage()); }
            catch (Exception ignored) { }
            return result;
        }
    }
}
