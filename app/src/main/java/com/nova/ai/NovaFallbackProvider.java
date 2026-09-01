package com.nova.ai;

import org.json.JSONObject;

/** Tries providers in order and advances to the next provider after a provider error. */
public final class NovaFallbackProvider implements NovaAiProvider {
    private final NovaAiProvider[] providers;

    public NovaFallbackProvider(NovaAiProvider... providers) { this.providers = providers == null ? new NovaAiProvider[0] : providers.clone(); }

    @Override public void complete(String systemContext, String userInput, JSONObject toolCatalog, Callback callback) {
        attempt(0, systemContext, userInput, toolCatalog, callback);
    }

    private void attempt(int index, String systemContext, String userInput, JSONObject catalog, Callback callback) {
        if (index >= providers.length) {
            if (callback != null) callback.onError(new IllegalStateException("No AI provider succeeded"));
            return;
        }
        NovaAiProvider provider = providers[index];
        if (provider == null) { attempt(index + 1, systemContext, userInput, catalog, callback); return; }
        provider.complete(systemContext, userInput, catalog, new Callback() {
            @Override public void onSuccess(JSONObject response) { if (callback != null) callback.onSuccess(response); }
            @Override public void onError(Exception error) { attempt(index + 1, systemContext, userInput, catalog, callback); }
        });
    }
}
