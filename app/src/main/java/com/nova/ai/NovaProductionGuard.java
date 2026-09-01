package com.nova.ai;

import android.content.Context;
import org.json.JSONObject;

/** Production gate for predictable local execution and diagnostics. */
public final class NovaProductionGuard {
    private final Context context;

    public NovaProductionGuard(Context context) {
        if (context == null) throw new IllegalArgumentException("context");
        this.context = context.getApplicationContext();
    }

    public JSONObject check() {
        JSONObject out = NovaDiagnostics.snapshot(context);
        try {
            out.put("policy", new JSONObject()
                    .put("maxSteps", NovaAgentPolicy.MAX_STEPS)
                    .put("maxRetries", NovaAgentPolicy.MAX_RETRIES)
                    .put("maxTaskMillis", NovaAgentPolicy.MAX_TASK_MILLIS)
                    .put("maxContextItems", NovaAgentPolicy.MAX_CONTEXT_ITEMS));
            out.put("toolsRegistered", NovaRuntime.get(context).tools().describe().length());
            out.put("status", "ready");
        } catch (Exception e) {
            try { out.put("ok", false).put("status", "degraded").put("error", "diagnostics_failed"); } catch (Exception ignored) { }
        }
        return out;
    }
}
