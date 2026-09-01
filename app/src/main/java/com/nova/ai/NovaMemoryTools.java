package com.nova.ai;

import android.content.Context;
import org.json.JSONObject;

/** Explicit tools for model-controlled persistent memory. */
public final class NovaMemoryTools {
    private NovaMemoryTools() {}

    public static NovaTool remember(Context context) {
        NovaLongTermMemory memory = new NovaLongTermMemory(context);
        return new NovaTool() {
            public String id() { return "memory.remember"; }
            public String description() { return "Store a non-sensitive user-approved fact for future NOVA conversations."; }
            public JSONObject schema() { return buildSchema("key", "string", "value", "string"); }
            public JSONObject execute(JSONObject input) {
                JSONObject out = new JSONObject();
                try {
                    String key = input == null ? "" : input.optString("key", "");
                    String value = input == null ? "" : input.optString("value", "");
                    out.put("ok", memory.put(key, value));
                } catch (Exception e) { try { out.put("ok", false); } catch (Exception ignored) {} }
                return out;
            }
        };
    }

    public static NovaTool recall(Context context) {
        NovaLongTermMemory memory = new NovaLongTermMemory(context);
        return new NovaTool() {
            public String id() { return "memory.recall"; }
            public String description() { return "Recall a previously stored NOVA memory by key."; }
            public JSONObject schema() { return buildSchema("key", "string"); }
            public JSONObject execute(JSONObject input) {
                JSONObject out = new JSONObject();
                try { out.put("value", memory.get(input == null ? "" : input.optString("key", ""))); }
                catch (Exception ignored) {}
                return out;
            }
        };
    }

    private static JSONObject buildSchema(String... pairs) {
        JSONObject out = new JSONObject();
        try {
            JSONObject properties = new JSONObject();
            for (int i = 0; i + 1 < pairs.length; i += 2) properties.put(pairs[i], new JSONObject().put("type", pairs[i + 1]));
            out.put("type", "object").put("properties", properties);
        } catch (Exception ignored) {}
        return out;
    }
}
