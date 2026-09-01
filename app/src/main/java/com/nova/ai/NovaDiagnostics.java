package com.nova.ai;

import android.content.Context;
import android.os.Build;
import org.json.JSONObject;

/** Lightweight runtime diagnostics that never expose credentials or secrets. */
public final class NovaDiagnostics {
    private NovaDiagnostics() {}

    public static JSONObject snapshot(Context context) {
        JSONObject out = new JSONObject();
        try {
            out.put("ok", true).put("sdk", Build.VERSION.SDK_INT)
                    .put("release", Build.VERSION.RELEASE == null ? "" : Build.VERSION.RELEASE)
                    .put("package", context == null ? "" : context.getPackageName())
                    .put("timestamp", System.currentTimeMillis());
        } catch (Exception ignored) { }
        return out;
    }
}
