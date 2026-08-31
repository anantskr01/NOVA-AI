package com.nova.ai;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import org.json.JSONObject;

/** Opens an explicitly named installed Android package. */
public final class NovaAppLauncherTool implements NovaTool {
    private final Context context;
    public NovaAppLauncherTool(Context context) { this.context = context.getApplicationContext(); }
    @Override public String id() { return "android.open_app"; }
    @Override public String description() { return "Open an installed Android app by package name."; }
    @Override public JSONObject schema() {
        JSONObject s = new JSONObject();
        try { s.put("type", "object"); s.put("required", new org.json.JSONArray().put("package")); } catch (Exception ignored) { }
        return s;
    }
    @Override public JSONObject execute(JSONObject input) throws Exception {
        String pkg = input == null ? "" : input.optString("package", "").trim();
        if (pkg.isEmpty()) throw new IllegalArgumentException("package_required");
        PackageManager pm = context.getPackageManager();
        Intent launch = pm.getLaunchIntentForPackage(pkg);
        if (launch == null) throw new IllegalArgumentException("app_not_found");
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(launch);
        return new JSONObject().put("opened", true).put("package", pkg);
    }
}
