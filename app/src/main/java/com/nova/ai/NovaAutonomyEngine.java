package com.nova.ai;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashSet;
import java.util.Set;

/** Goal-oriented planner that decomposes requests into bounded, registered tool steps. */
public final class NovaAutonomyEngine {
    private final NovaToolRegistry tools;

    /**
     * The autonomy engine depends on the tool registry, not on NovaRuntime itself.
     * This prevents the initialization cycle:
     * NovaRuntime -> NovaAutonomyEngine -> NovaRuntime.get(...).
     */
    public NovaAutonomyEngine(NovaToolRegistry tools) {
        if (tools == null) throw new IllegalArgumentException("tools");
        this.tools = tools;
    }

    public JSONObject inspectGoal(String goal) {
        JSONObject result = new JSONObject();
        try {
            String text = goal == null ? "" : goal.trim();
            result.put("ok", !text.isEmpty());
            result.put("goal", text);
            JSONArray steps = new JSONArray();
            if (!text.isEmpty()) {
                steps.put(new JSONObject()
                        .put("action", "reason")
                        .put("description", "Analyze the goal and select registered capabilities."));
            }
            result.put("steps", steps).put("maxSteps", 6);
        } catch (Exception ignored) {}
        return result;
    }

    public boolean canUseTool(String toolId) {
        return toolId != null && tools.get(toolId) != null;
    }

    public Set<String> availableTools() {
        return new HashSet<>(tools.ids());
    }
}
