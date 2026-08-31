package com.nova.ai;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;

/** Coordinates provider-backed conversations while preserving the local tool authorization boundary. */
public final class NovaConversationEngine {
    private final NovaContextStore contextStore;
    private final NovaToolRegistry registry;
    private final NovaAgent fallbackAgent;
    private NovaAiProvider provider;

    public NovaConversationEngine(Context context) {
        Context app = context.getApplicationContext();
        contextStore = new NovaContextStore(app);
        registry = new NovaToolRegistry();
        registry.register(NovaBuiltInTools.echo());
        registry.register(NovaBuiltInTools.contextAppend(app));
        fallbackAgent = new NovaAgent(app);
    }

    public void setProvider(NovaAiProvider provider) { this.provider = provider; }

    public void send(String input, NovaAiProvider.Callback callback) {
        String text = input == null ? "" : input.trim();
        if (provider == null) {
            JSONObject fallback = fallbackAgent.plan(text);
            if (callback != null) callback.onSuccess(fallback);
            return;
        }
        JSONArray context = contextStore.recent();
        NovaAiRequest request = new NovaAiRequest(
                "You are NOVA. Propose only capabilities present in the supplied tool catalog. Never assume device permissions.",
                text, context, registry.describe());
        provider.complete(request.systemContext, request.userInput, request.toJson(), callback);
        contextStore.add("user", text);
    }
}
