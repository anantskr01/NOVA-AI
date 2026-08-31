package com.nova.ai;

import org.json.JSONArray;
import org.json.JSONObject;

/** Provider-neutral request envelope for NOVA's model layer. */
public final class NovaAiRequest {
    public final String systemContext;
    public final String userInput;
    public final JSONArray recentContext;
    public final JSONArray tools;

    public NovaAiRequest(String systemContext, String userInput, JSONArray recentContext, JSONArray tools) {
        this.systemContext = systemContext == null ? "" : systemContext;
        this.userInput = userInput == null ? "" : userInput;
        this.recentContext = recentContext == null ? new JSONArray() : recentContext;
        this.tools = tools == null ? new JSONArray() : tools;
    }

    public JSONObject toJson() {
        JSONObject result = new JSONObject();
        try {
            result.put("system", systemContext);
            result.put("input", userInput);
            result.put("context", recentContext);
            result.put("tools", tools);
        } catch (Exception ignored) { }
        return result;
    }
}
