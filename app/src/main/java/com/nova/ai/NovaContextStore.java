package com.nova.ai;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;

/** Small bounded local context store for recent agent turns. Long-term memory remains a separate concern. */
public final class NovaContextStore {
    private static final String PREFS = "nova_agent_context";
    private static final String TURNS = "turns";
    private static final int MAX_TURNS = 24;

    private final SharedPreferences prefs;

    public NovaContextStore(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public synchronized void add(String role, String text) {
        JSONArray turns = read();
        JSONObject item = new JSONObject();
        try {
            item.put("role", role == null ? "unknown" : role);
            item.put("text", text == null ? "" : text);
            item.put("time", System.currentTimeMillis());
        } catch (Exception ignored) { }
        turns.put(item);
        while (turns.length() > MAX_TURNS) turns.remove(0);
        prefs.edit().putString(TURNS, turns.toString()).apply();
    }

    public synchronized JSONArray recent() { return read(); }

    public synchronized void clear() { prefs.edit().remove(TURNS).apply(); }

    private JSONArray read() {
        try { return new JSONArray(prefs.getString(TURNS, "[]")); }
        catch (Exception ignored) { return new JSONArray(); }
    }
}
