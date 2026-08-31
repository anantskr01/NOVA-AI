package com.nova.ai;

import org.json.JSONObject;

/** Executes only registered capabilities and returns structured results. */
public final class NovaToolExecutor {
    private final NovaToolRegistry registry;

    public NovaToolExecutor(NovaToolRegistry registry) {
        if (registry == null) throw new IllegalArgumentException("registry");
        this.registry = registry;
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
