package com.nova.ai;

import org.json.JSONArray;
import org.json.JSONObject;

/** Deterministic bounded task orchestration primitives used by higher-level agent flows. */
public final class NovaTaskOrchestrator {
    public interface StepExecutor { JSONObject execute(String tool, JSONObject input); }

    public JSONObject execute(JSONArray steps, StepExecutor executor) {
        JSONObject result = new JSONObject();
        JSONArray results = new JSONArray();
        long started = System.currentTimeMillis();
        int completed = 0;
        try {
            if (steps == null || executor == null) return result.put("ok", false).put("error", "invalid_orchestrator_input");
            for (int i = 0; i < steps.length() && completed < NovaAgentPolicy.MAX_STEPS; i++) {
                if (NovaAgentPolicy.taskExpired(started)) return result.put("ok", false).put("error", "task_timeout").put("results", results);
                JSONObject step = steps.optJSONObject(i);
                if (step == null) continue;
                String tool = step.optString("tool", "").trim();
                if (tool.isEmpty()) {
                    results.put(new JSONObject().put("ok", false).put("error", "missing_tool").put("step", i));
                    continue;
                }
                JSONObject output = executor.execute(tool, step.optJSONObject("input"));
                if (output == null) output = new JSONObject().put("ok", false).put("error", "null_tool_result");
                String serialized = NovaAgentPolicy.bounded(output.toString(), NovaAgentPolicy.MAX_TOOL_RESULT_CHARS);
                results.put(new JSONObject().put("step", i).put("tool", tool).put("result", serialized));
                completed++;
                if (!output.optBoolean("ok", false) && step.optBoolean("stopOnError", true)) {
                    return result.put("ok", false).put("error", "step_failed").put("results", results).put("completed", completed);
                }
            }
            boolean limited = steps.length() > NovaAgentPolicy.MAX_STEPS;
            return result.put("ok", !limited).put("status", limited ? "step_limit_reached" : "complete")
                    .put("results", results).put("completed", completed);
        } catch (Exception e) {
            try { return result.put("ok", false).put("error", "orchestration_error").put("message", String.valueOf(e.getMessage())).put("results", results); }
            catch (Exception ignored) { return result; }
        }
    }
}
