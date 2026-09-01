package com.nova.ai;

import org.json.JSONObject;

/** Offline-provider contract. A bundled/local model can implement this without changing the agent. */
public interface NovaLocalAiProvider extends NovaAiProvider {
    boolean isReady();
    String modelId();
    @Override void complete(String systemContext, String userInput, JSONObject toolCatalog, Callback callback);
}
