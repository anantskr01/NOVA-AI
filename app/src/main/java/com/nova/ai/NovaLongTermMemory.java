package com.nova.ai;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;

/** Small persistent key/value memory layer. Sensitive data should not be stored here without explicit consent. */
public final class NovaLongTermMemory {
    private static final String PREFS = "nova_long_term_memory";
    private final SharedPreferences prefs;

    public NovaLongTermMemory(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public synchronized boolean put(String key, String value) {
        if (key == null || key.trim().isEmpty() || value == null) return false;
        prefs.edit().putString(key.trim(), value.trim()).apply();
        return true;
    }

    public synchronized String get(String key) {
        return key == null ? null : prefs.getString(key.trim(), null);
    }

    public synchronized boolean remove(String key) {
        if (key == null || key.trim().isEmpty()) return false;
        prefs.edit().remove(key.trim()).apply();
        return true;
    }

    public synchronized void clear() { prefs.edit().clear().apply(); }

    public synchronized JSONArray export() {
        JSONArray result = new JSONArray();
        try {
            for (String key : prefs.getAll().keySet()) {
                Object value = prefs.getAll().get(key);
                result.put(new JSONObject().put("key", key).put("value", String.valueOf(value)));
            }
        } catch (Exception ignored) { }
        return result;
    }
}
