package com.nova.ai;

import android.content.Context;
import org.json.JSONObject;

/** Checks for signed-release metadata/config updates; never executes downloaded code. */
public final class NovaUpdateManager {
    private final NovaInternetClient internet = new NovaInternetClient();
    private final Context context;
    public NovaUpdateManager(Context context) { this.context = context.getApplicationContext(); }
    public void check(String manifestUrl, String currentVersion, NovaInternetClient.Callback callback) {
        internet.get(manifestUrl, new NovaInternetClient.Callback() {
            @Override public void onSuccess(String body, int code) {
                try {
                    JSONObject manifest = new JSONObject(body);
                    String version = manifest.optString("version", "");
                    String config = manifest.optString("config", "");
                    boolean newer = !version.isEmpty() && !version.equals(currentVersion);
                    JSONObject result = new JSONObject().put("ok", code >= 200 && code < 300).put("available", newer).put("version", version).put("config", config);
                    if (callback != null) callback.onSuccess(result.toString(), code);
                } catch (Exception e) { if (callback != null) callback.onError(e); }
            }
            @Override public void onError(Exception error) { if (callback != null) callback.onError(error); }
        });
    }
    public void shutdown() { internet.shutdown(); }
}
