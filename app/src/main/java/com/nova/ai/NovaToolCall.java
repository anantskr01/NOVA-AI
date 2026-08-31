package com.nova.ai;

import org.json.JSONObject;

/** Structured, auditable request for a capability. */
public final class NovaToolCall {
    public final String id;
    public final String tool;
    public final JSONObject input;

    public NovaToolCall(String id, String tool, JSONObject input) {
        this.id = id == null ? "" : id;
        this.tool = tool == null ? "" : tool;
        this.input = input == null ? new JSONObject() : input;
    }

    public JSONObject toJson() {
        JSONObject out = new JSONObject();
        try {
            out.put("id", id);
            out.put("tool", tool);
            out.put("input", input);
        } catch (Exception ignored) { }
        return out;
    }

    public static NovaToolCall fromJson(JSONObject value) {
        if (value == null) return new NovaToolCall("", "", new JSONObject());
        return new NovaToolCall(value.optString("id"), value.optString("tool"), value.optJSONObject("input"));
    }
}
