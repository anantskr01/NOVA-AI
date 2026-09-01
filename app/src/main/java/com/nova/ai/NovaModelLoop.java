package com.nova.ai;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;

/** Provider -> response parser -> bounded multi-step tool execution. */
public final class NovaModelLoop {
    public interface Callback { void onComplete(JSONObject result); }
    private static final int MAX_STEPS = 6;

    private final NovaContextStore contextStore;
    private final NovaRuntime runtime;
    private NovaAiProvider provider;

    public NovaModelLoop(Context context) {
        Context app = context.getApplicationContext();
        contextStore = new NovaContextStore(app);
        runtime = NovaRuntime.get(app);
    }

    public void setProvider(NovaAiProvider provider) { this.provider = provider; }

    public void run(String input, Callback callback) {
        if (provider == null) { fail(callback, "ai_provider_not_configured"); return; }
        final String text = input == null ? "" : input.trim();
        requestNext(text, contextStore.recent(), new JSONArray(), 0, callback);
    }

    private void requestNext(String userText, JSONArray context, JSONArray executions, int step, Callback callback) {
        if (step >= MAX_STEPS) {
            finish(userText, executions, "max_steps_reached", callback);
            return;
        }
        NovaAiRequest request = new NovaAiRequest(
                "You are NOVA. Use only tools in the supplied catalog. Never invent permissions or capabilities. Ask for confirmation before sensitive actions. After a tool result, continue the task only if another step is needed. Stop when the task is complete.",
                userText, context, runtime.tools().describe());
        provider.complete(request.systemContext, request.userInput, request.toJson(), new NovaAiProvider.Callback() {
            @Override public void onSuccess(JSONObject response) {
                NovaAiResponse parsed = NovaAiResponse.parse(response);
                try {
                    for (int i = 0; i < parsed.toolCalls.length(); i++) {
                        JSONObject raw = parsed.toolCalls.optJSONObject(i);
                        if (raw == null) continue;
                        NovaToolCall call = NovaToolCall.fromJson(raw);
                        executions.put(runtime.executor().execute(call.tool, call.input));
                    }
                    if (parsed.toolCalls.length() == 0) {
                        finish(userText, executions, "complete", callback, parsed.text);
                        return;
                    }
                    JSONArray nextContext = new JSONArray();
                    for (int i = 0; i < context.length(); i++) nextContext.put(context.opt(i));
                    nextContext.put(new JSONObject().put("role", "user").put("content", userText));
                    nextContext.put(new JSONObject().put("role", "tool").put("content", executions.opt(executions.length() - 1)));
                    requestNext(userText, nextContext, executions, step + 1, callback);
                } catch (Exception e) {
                    finish(userText, executions, "execution_error", callback);
                }
            }
            @Override public void onError(Exception error) {
                JSONObject result = new JSONObject();
                try { result.put("ok", false); result.put("error", "ai_provider_error"); result.put("message", error == null ? "provider request failed" : String.valueOf(error.getMessage())); }
                catch (Exception ignored) { }
                if (callback != null) callback.onComplete(result);
            }
        });
    }

    private void finish(String userText, JSONArray executions, String status, Callback callback) { finish(userText, executions, status, callback, ""); }

    private void finish(String userText, JSONArray executions, String status, Callback callback, String text) {
        JSONObject result = new JSONObject();
        try {
            result.put("ok", true);
            result.put("status", status);
            result.put("text", text == null ? "" : text);
            result.put("executions", executions);
            contextStore.add("user", userText);
            if (text != null && !text.trim().isEmpty()) contextStore.add("assistant", text);
        } catch (Exception ignored) { }
        if (callback != null) callback.onComplete(result);
    }

    private static void fail(Callback callback, String error) {
        if (callback == null) return;
        JSONObject result = new JSONObject();
        try { result.put("ok", false); result.put("error", error); } catch (Exception ignored) { }
        callback.onComplete(result);
    }
}
