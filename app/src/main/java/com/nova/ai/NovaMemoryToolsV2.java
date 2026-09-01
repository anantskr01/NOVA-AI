package com.nova.ai;

import android.content.Context;
import org.json.JSONObject;

/** Searchable memory tools for the agent. */
public final class NovaMemoryToolsV2 {
    private NovaMemoryToolsV2() {}

    public static NovaTool remember(Context c) {
        NovaMemoryManager memory = new NovaMemoryManager(c);
        return new NovaTool() {
            public String id() { return "memory.remember"; }
            public String description() { return "Store a useful, non-sensitive fact explicitly provided by the user."; }
            public JSONObject schema() { return safeSchema("key", "string", "value", "string"); }
            public JSONObject execute(JSONObject input) throws Exception {
                String key = input == null ? "" : input.optString("key", "");
                String value = input == null ? "" : input.optString("value", "");
                return new JSONObject().put("ok", memory.remember(key, value));
            }
        };
    }
    public static NovaTool recall(Context c) {
        NovaMemoryManager memory = new NovaMemoryManager(c);
        return new NovaTool() {
            public String id() { return "memory.recall"; }
            public String description() { return "Find stored memories relevant to a query."; }
            public JSONObject schema() { return safeSchema("query", "string"); }
            public JSONObject execute(JSONObject input) throws Exception {
                String query = input == null ? "" : input.optString("query", "");
                return new JSONObject().put("ok", true).put("matches", memory.search(query, 5));
            }
        };
    }
    public static NovaTool forget(Context c) {
        NovaMemoryManager memory = new NovaMemoryManager(c);
        return new NovaTool() {
            public String id() { return "memory.forget"; }
            public String description() { return "Delete a stored memory by key."; }
            public JSONObject schema() { return safeSchema("key", "string"); }
            public JSONObject execute(JSONObject input) throws Exception {
                return new JSONObject().put("ok", memory.forget(input == null ? "" : input.optString("key", "")));
            }
        };
    }
    private static JSONObject safeSchema(String... pairs) {
        JSONObject props = new JSONObject();
        try {
            for (int i = 0; i + 1 < pairs.length; i += 2) {
                props.put(pairs[i], new JSONObject().put("type", pairs[i + 1]));
            }
            return new JSONObject().put("type", "object").put("properties", props);
        } catch (Exception ignored) {
            return new JSONObject();
        }
    }
}
