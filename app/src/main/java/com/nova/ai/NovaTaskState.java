package com.nova.ai;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashSet;
import java.util.Set;

/** Bounded state for one NOVA task: progress, executed tools and recovery metadata. */
public final class NovaTaskState {
    private final String taskId;
    private final String goal;
    private final long startedAt;
    private final Set<String> completedCalls = new HashSet<>();
    private int step;
    private String status = "running";
    private String error = "";

    public NovaTaskState(String taskId, String goal) {
        this.taskId = taskId == null ? "" : taskId;
        this.goal = goal == null ? "" : goal;
        this.startedAt = System.currentTimeMillis();
    }
    public String taskId() { return taskId; }
    public String goal() { return goal; }
    public int step() { return step; }
    public void advance() { step++; }
    public boolean markCall(String id) { return id != null && !id.isEmpty() && completedCalls.add(id); }
    public void complete() { status = "complete"; }
    public void fail(String message) { status = "failed"; error = message == null ? "" : message; }
    public JSONObject toJson() {
        JSONObject out = new JSONObject();
        try {
            out.put("id", taskId).put("goal", goal).put("step", step).put("status", status)
                    .put("startedAt", startedAt).put("completedCalls", new JSONArray(completedCalls));
            if (!error.isEmpty()) out.put("error", error);
        } catch (Exception ignored) { }
        return out;
    }
}
