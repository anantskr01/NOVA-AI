package com.nova.ai;

import org.json.JSONArray;
import org.json.JSONObject;

/** Lightweight deterministic validation for tool calls before execution. */
public final class NovaToolValidator {
    private static final int MAX_KEYS = 32;
    private static final int MAX_STRING = 4096;
    private NovaToolValidator() {}

    public static void validate(NovaTool tool, JSONObject input) {
        if (tool == null) throw new IllegalArgumentException("tool_required");
        JSONObject value = input == null ? new JSONObject() : input;
        JSONObject schema = tool.schema();
        JSONArray required = schema == null ? null : schema.optJSONArray("required");
        if (required != null) for (int i = 0; i < required.length(); i++) {
            String key = required.optString(i, "");
            if (key.isEmpty() || !value.has(key) || value.isNull(key)) throw new IllegalArgumentException("missing_required:" + key);
        }
        if (value.length() > MAX_KEYS) throw new IllegalArgumentException("too_many_arguments");
        java.util.Iterator<String> keys = value.keys();
        while (keys.hasNext()) {
            Object v = value.opt(keys.next());
            if (v instanceof String && ((String) v).length() > MAX_STRING) throw new IllegalArgumentException("argument_too_long");
        }
    }
}
