package com.nova.ai;

import android.content.Context;
import org.json.JSONObject;

/** Safe built-in tools that do not bypass Android permission boundaries. */
public final class NovaBuiltInTools {
    private NovaBuiltInTools() { }

    public static NovaTool echo() {
        return new NovaTool() {
            public String id() { return "nova.echo"; }
            public String description() { return "Return supplied text for diagnostics and agent tests."; }
            public JSONObject schema() {
                JSONObject s = new JSONObject();
                try { s.put("type", "object"); s.put("required", new org.json.JSONArray().put("text")); } catch (Exception ignored) { }
                return s;
            }
            public JSONObject execute(JSONObject input) {
                JSONObject out = new JSONObject();
                try { out.put("text", input == null ? "" : input.optString("text", "")); } catch (Exception ignored) { }
                return out;
            }
        };
    }

    public static NovaTool contextAppend(Context context) {
        final NovaContextStore store = new NovaContextStore(context);
        return new NovaTool() {
            public String id() { return "memory.append_context"; }
            public String description() { return "Append a bounded conversation turn to local agent context."; }
            public JSONObject schema() {
                JSONObject s = new JSONObject();
                try { s.put("type", "object"); s.put("required", new org.json.JSONArray().put("role").put("text")); } catch (Exception ignored) { }
                return s;
            }
            public JSONObject execute(JSONObject input) {
                store.add(input == null ? "unknown" : input.optString("role", "unknown"), input == null ? "" : input.optString("text", ""));
                JSONObject out = new JSONObject();
                try { out.put("stored", true); } catch (Exception ignored) { }
                return out;
            }
        };
    }
}
