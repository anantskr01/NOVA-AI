package com.nova.ai;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;

/** Local action boundary. Remote agents must use the same permission-controlled executor. */
public final class NovaActionEngine {
    private final Context context;
    public NovaActionEngine(Context context) { this.context = context.getApplicationContext(); }
    public boolean execute(String type, String value) {
        NovaAccessibilityService s = NovaAccessibilityService.getInstance();
        try {
            switch (type == null ? "" : type.trim().toLowerCase()) {
                case "home": return s != null && s.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME);
                case "back": return s != null && s.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
                case "recents": return s != null && s.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS);
                case "notifications": return s != null && s.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS);
                case "quick_settings": return s != null && s.performGlobalAction(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS);
                case "open_url":
                    if (value == null || value.trim().isEmpty()) return false;
                    Intent web = new Intent(Intent.ACTION_VIEW, Uri.parse(value.trim())); web.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(web); return true;
                case "settings":
                    Intent settings = new Intent(Settings.ACTION_SETTINGS); settings.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(settings); return true;
                case "open_package":
                    if (value == null) return false;
                    Intent app = context.getPackageManager().getLaunchIntentForPackage(value.trim()); if (app == null) return false;
                    app.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(app); return true;
                case "click_text": return s != null && s.clickText(value);
                case "type_text": return s != null && s.typeText(value);
                case "scroll_up": return s != null && s.swipe(0, -350);
                case "scroll_down": return s != null && s.swipe(0, 350);
                case "swipe_left": return s != null && s.swipe(-350, 0);
                case "swipe_right": return s != null && s.swipe(350, 0);
                default: return false;
            }
        } catch (Exception ignored) { return false; }
    }
}
