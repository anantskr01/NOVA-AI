package com.nova.ai;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;

/** Provider -> response parser -> authorized tools. A model never receives direct device access. */
public final class NovaModelLoop {
    public interface Callback { void onComplete(JSONObject result); }

    private final NovaContextStore contextStore;
    private final NovaToolRegistry registry;
    private final NovaToolExecutor executor;
    private NovaAiProvider provider;

    public NovaModelLoop(Context context) {
        Context app = context.getApplicationContext();
        contextStore = new NovaContextStore(app);
        registry = new NovaToolRegistry();
        registry.register(NovaBuiltInTools.echo());
        registry.register(NovaBuiltInTools.contextAppend(app));
        executor = new NovaToolExecutor(app, registry);
    }

    public void setProvider(NovaAiProvider provider) { this.provider = provider; }

    public void run(String input, Callback callback) {
        if (provider == null) {
            fail(callback, "ai_provider_not_configured");
            return;
        }
        String text = input == null ? "" : input.trim();
        NovaAiRequest request = new NovaAiRequest(
                "You are NOVA. Use only tools in the supplied catalog. Never invent permissions or capabilities.",
                text, contextStore.recent(), registry.describe());
        provider.complete(request.systemContext, request.userInput, request.toJson(), new NovaAiProvider.Callback() {
            @Override public void onSuccess(JSONObject response) {
                NovaAiResponse parsed = NovaAiResponse.parse(response);
                JSONObject result = parsed.toJson();
                JSONArray execution = new JSONArray();
                for (int i = 0; i < parsed.toolCalls.length(); i++) {
                    JSONObject raw = parsed.toolCalls.optJSONObject(i);
                    if (raw == null) continue;
                    NovaToolCall call = NovaToolCall.fromJson(raw);
                    execution.put(executor.execute(call.tool, call.input));
                }
                try { result.put("executions", execution); } catch (Exception ignored) { }
                contextStore.add("user", text);
                if (callback != null) callback.onComplete(result);
            }

            @Override public void onError(Exception error) {
                JSONObject result = new JSONObject();
                try {
                    result.put("ok", false);
                    result.put("error", "ai_provider_error");
                    result.put("message", error == null || error.getMessage() == null ? "provider request failed" : error.getMessage());
                } catch (Exception ignored) { }
                if (callback != null) callback.onComplete(result);
            }
        });
    }

    private static void fail(Callback callback, String error) {
        if (callback == null) return;
        JSONObject result = new JSONObject();
        try { result.put("ok", false); result.put("error", error); } catch (Exception ignored) { }
        callback.onComplete(result);
    }
}
