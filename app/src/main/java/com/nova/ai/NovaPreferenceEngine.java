package com.nova.ai;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;

/** Learns explicit, non-sensitive workflow preferences with bounded local storage. */
public final class NovaPreferenceEngine {
    private static final String PREFS = "nova_preferences_v1";
    private static final String DATA = "data";
    private final SharedPreferences prefs;
    public NovaPreferenceEngine(Context context) { prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE); }
    public synchronized boolean learn(String key, String value) {
        if (key == null || key.trim().isEmpty() || value == null || value.trim().isEmpty()) return false;
        try {
            JSONObject data = new JSONObject(prefs.getString(DATA, "{}"));
            data.put(key.trim(), value.trim());
            while (data.length() > 100) data.remove(data.keys().next());
            return prefs.edit().putString(DATA, data.toString()).commit();
        } catch (Exception e) { return false; }
    }
    public synchronized String get(String key) { try { return new JSONObject(prefs.getString(DATA, "{}")).optString(key, ""); } catch (Exception e) { return ""; } }
    public synchronized JSONObject snapshot() { try { return new JSONObject(prefs.getString(DATA, "{}")); } catch (Exception e) { return new JSONObject(); } }
    public synchronized boolean forget(String key) { try { JSONObject data=new JSONObject(prefs.getString(DATA,"{}")); if (!data.has(key)) return false; data.remove(key); return prefs.edit().putString(DATA,data.toString()).commit(); } catch(Exception e){return false;} }
}
