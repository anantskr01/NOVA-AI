package com.nova.ai;

import org.json.JSONArray;
import org.json.JSONObject;

/** Normalizes chat-completions and OpenAI Responses API output into NOVA's internal format. */
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
            if (response != null && response.has("output")) {
                text = response.optString("output_text", "");
                JSONArray output = response.optJSONArray("output");
                if (output != null) {
                    for (int i = 0; i < output.length(); i++) {
                        JSONObject item = output.optJSONObject(i);
                        if (item == null) continue;
                        if ("message".equals(item.optString("type")) && text.isEmpty()) {
                            JSONArray content = item.optJSONArray("content");
                            if (content != null) {
                                for (int j = 0; j < content.length(); j++) {
                                    JSONObject part = content.optJSONObject(j);
                                    if (part != null && "output_text".equals(part.optString("type"))) text += part.optString("text", "");
                                }
                            }
                        }
                        if ("function_call".equals(item.optString("type"))) {
                            JSONObject normalized = new JSONObject();
                            normalized.put("id", item.optString("call_id", item.optString("id", NovaProtocol.id())));
                            normalized.put("tool", item.optString("name", ""));
                            String arguments = item.optString("arguments", "{}");
                            try { normalized.put("input", new JSONObject(arguments)); }
                            catch (Exception ignored) { normalized.put("input", new JSONObject()); }
                            if (!normalized.optString("tool").isEmpty()) calls.put(normalized);
                        }
                    }
                }
                return new NovaAiResponse(text, calls);
            }

            JSONArray choices = response == null ? null : response.optJSONArray("choices");
            JSONObject choice = choices == null ? null : choices.optJSONObject(0);
            JSONObject message = choice == null ? null : choice.optJSONObject("message");
            if (message != null) {
                Object content = message.opt("content");
                if (content instanceof String) text = (String) content;
                JSONArray rawCalls = message.optJSONArray("tool_calls");
                if (rawCalls != null) {
                    for (int i = 0; i < rawCalls.length(); i++) {
                        JSONObject raw = rawCalls.optJSONObject(i); if (raw == null) continue;
                        JSONObject function = raw.optJSONObject("function"); if (function == null) continue;
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
