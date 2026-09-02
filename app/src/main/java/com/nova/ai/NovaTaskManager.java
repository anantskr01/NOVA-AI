package com.nova.ai;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Runs a batch of independent NOVA tool tasks concurrently and returns an ordered result set.
 * Authorization and validation remain inside NovaToolExecutor, so parallelism does not bypass safety.
 */
public final class NovaTaskManager {
    private final NovaToolExecutor executor;
    private final ExecutorService pool;

    public NovaTaskManager(NovaToolExecutor executor) {
        if (executor == null) throw new IllegalArgumentException("executor");
        this.executor = executor;
        this.pool = Executors.newCachedThreadPool();
    }

    /** Execute independent tool calls concurrently while preserving the model's call order in results. */
    public JSONArray executeParallel(final JSONArray calls) {
        JSONArray results = new JSONArray();
        if (calls == null || calls.length() == 0) return results;

        final int count = calls.length();
        final JSONObject[] slots = new JSONObject[count];
        final CountDownLatch latch = new CountDownLatch(count);

        for (int i = 0; i < count; i++) {
            final int index = i;
            final JSONObject raw = calls.optJSONObject(i);
            pool.execute(() -> {
                try {
                    NovaToolCall call = NovaToolCall.fromJson(raw);
                    JSONObject execution = executor.execute(call.tool, call.input);
                    slots[index] = wrap(call, execution);
                } catch (Exception e) {
                    slots[index] = error(raw, e);
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        for (int i = 0; i < count; i++) {
            if (slots[i] != null) results.put(slots[i]);
            else results.put(error(calls.optJSONObject(i), new IllegalStateException("task_timeout")));
        }
        return results;
    }

    private static JSONObject wrap(NovaToolCall call, JSONObject execution) {
        JSONObject out = new JSONObject();
        try {
            out.put("id", call.id).put("tool", call.tool).put("execution", execution);
        } catch (Exception ignored) { }
        return out;
    }

    private static JSONObject error(JSONObject raw, Exception e) {
        JSONObject out = new JSONObject();
        try {
            NovaToolCall call = NovaToolCall.fromJson(raw);
            out.put("id", call.id).put("tool", call.tool).put("execution", new JSONObject()
                    .put("ok", false)
                    .put("error", e == null ? "task_failed" : e.getClass().getSimpleName())
                    .put("message", e == null ? "task failed" : String.valueOf(e.getMessage())));
        } catch (Exception ignored) { }
        return out;
    }

    public void shutdown() {
        pool.shutdownNow();
    }
}
