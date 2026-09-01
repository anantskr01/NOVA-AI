package com.nova.ai;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashSet;
import java.util.Set;

/** Goal-oriented planner that decomposes requests into bounded, registered tool steps. */
public final class NovaAutonomyEngine {
    private final NovaRuntime runtime;
    public NovaAutonomyEngine(Context context) { runtime = NovaRuntime.get(context); }
    public JSONObject inspectGoal(String goal) {
        JSONObject result = new JSONObject();
        try {
            String text = goal == null ? "" : goal.trim();
            result.put("ok", !text.isEmpty()); result.put("goal", text);
            JSONArray steps = new JSONArray();
            if (!text.isEmpty()) steps.put(new JSONObject().put("action", "reason").put("description", "Analyze the goal and select registered capabilities."));
            result.put("steps", steps).put("maxSteps", 6);
        } catch (Exception ignored) {}
        return result;
    }
    public boolean canUseTool(String toolId) { return toolId != null && runtime.tools().get(toolId) != null; }
    public Set<String> availableTools() { return new HashSet<>(runtime.tools().ids()); }
}
