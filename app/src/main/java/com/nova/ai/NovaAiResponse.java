package com.nova.ai;

import org.json.JSONArray;
import org.json.JSONObject;

/** Normalizes common OpenAI-compatible chat responses into text and structured tool calls. */
public final class NovaAiResponse {
    public final String text;
    public final JSONArray toolCalls;

    private NovaAiResponse(String text, JSONArray toolCalls) {
        this.text = text == null ? "" : text;
        this.toolCalls = toolCalls == null ? new JSONArray() : toolCalls;
    }

    public static NovaAiResponse parse(JSONObject response) {
        String text = "";
        JSONArray calls = new JSONArray();
        try {
            JSONArray choices = response == null ? null : response.optJSONArray("choices");
            JSONObject choice = choices == null ? null : choices.optJSONObject(0);
            JSONObject message = choice == null ? null : choice.optJSONObject("message");
            if (message != null) {
                Object content = message.opt("content");
                if (content instanceof String) text = (String) content;
                JSONArray rawCalls = message.optJSONArray("tool_calls");
                if (rawCalls != null) {
                    for (int i = 0; i < rawCalls.length(); i++) {
                        JSONObject raw = rawCalls.optJSONObject(i);
                        if (raw == null) continue;
                        JSONObject function = raw.optJSONObject("function");
                        if (function == null) continue;
                        JSONObject normalized = new JSONObject();
                        normalized.put("id", raw.optString("id", NovaProtocol.id()));
                        normalized.put("tool", function.optString("name", ""));
                        String arguments = function.optString("arguments", "{}");
                        try { normalized.put("input", new JSONObject(arguments)); }
                        catch (Exception ignored) { normalized.put("input", new JSONObject()); }
                        if (!normalized.optString("tool").isEmpty()) calls.put(normalized);
                    }
                }
            }
        } catch (Exception ignored) { }
        return new NovaAiResponse(text, calls);
    }

    public JSONObject toJson() {
        JSONObject result = new JSONObject();
        try { result.put("text", text); result.put("toolCalls", toolCalls); } catch (Exception ignored) { }
        return result;
    }
}
