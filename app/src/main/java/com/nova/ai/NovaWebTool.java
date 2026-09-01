package com.nova.ai;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import org.json.JSONObject;

/** Opens a validated web URL using Android's normal user-facing browser flow. */
public final class NovaWebTool implements NovaTool {
    private final Context context;
    public NovaWebTool(Context context) { this.context = context.getApplicationContext(); }
    @Override public String id() { return "web.open_url"; }
    @Override public String description() { return "Open an HTTP or HTTPS URL in the user's browser."; }
    @Override public JSONObject schema() {
        try { return new JSONObject().put("type", "object").put("required", new org.json.JSONArray().put("url"))
                .put("properties", new JSONObject().put("url", new JSONObject().put("type", "string"))); }
        catch (Exception e) { return new JSONObject(); }
    }
    @Override public JSONObject execute(JSONObject input) throws Exception {
        String url = input == null ? "" : input.optString("url", "").trim();
        Uri uri = Uri.parse(url);
        String scheme = uri.getScheme();
        if (!("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme)) || uri.getHost() == null) throw new IllegalArgumentException("valid_http_url_required");
        Intent intent = new Intent(Intent.ACTION_VIEW, uri); intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
        return new JSONObject().put("opened", true).put("url", url);
    }
}
