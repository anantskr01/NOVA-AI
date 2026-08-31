package com.nova.ai;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

/** Small local cache; the future NOVA core can synchronize durable memory across nodes. */
public final class NovaMemory {
    private final SharedPreferences prefs;
    public NovaMemory(Context context) { prefs = context.getApplicationContext().getSharedPreferences("nova_memory", Context.MODE_PRIVATE); }
    public synchronized void remember(String role, String text) {
        if (text == null || text.trim().isEmpty()) return;
        JSONArray a = recent();
        JSONObject o = new JSONObject();
        try { o.put("role", role == null ? "user" : role); o.put("content", text.trim()); a.put(o); } catch (Exception ignored) { return; }
        while (a.length() > 20) a.remove(0);
        prefs.edit().putString("history", a.toString()).apply();
    }
    public synchronized JSONArray recent() { try { return new JSONArray(prefs.getString("history", "[]")); } catch (Exception e) { return new JSONArray(); } }
    public synchronized void clear() { prefs.edit().remove("history").apply(); }
}
