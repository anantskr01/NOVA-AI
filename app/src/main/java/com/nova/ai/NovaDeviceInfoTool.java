package com.nova.ai;

import android.os.Build;
import org.json.JSONObject;

/** Read-only Android device information capability. */
public final class NovaDeviceInfoTool implements NovaTool {
    @Override public String id() { return "android.device_info"; }
    @Override public String description() { return "Read basic Android device and OS information."; }
    @Override public JSONObject schema() {
        JSONObject s = new JSONObject();
        try { s.put("type", "object"); } catch (Exception ignored) { }
        return s;
    }
    @Override public JSONObject execute(JSONObject input) throws Exception {
        return new JSONObject().put("manufacturer", Build.MANUFACTURER)
                .put("model", Build.MODEL)
                .put("android", Build.VERSION.RELEASE)
                .put("sdk", Build.VERSION.SDK_INT);
    }
}
