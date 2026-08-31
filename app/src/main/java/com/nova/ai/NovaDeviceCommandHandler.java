package com.nova.ai;

import android.util.Log;

import org.json.JSONObject;

/** Converts authenticated, bounded gateway commands into the existing local action boundary. */
public final class NovaDeviceCommandHandler {
    public interface Reply { void send(JSONObject result); }
    private static final String TAG = "NOVA-Command";
    private static final int MAX_VALUE_LENGTH = 2048;
    private final NovaActionEngine actions;
    private final NovaAccessibilityService accessibility;

    public NovaDeviceCommandHandler(NovaActionEngine actions, NovaAccessibilityService accessibility) {
        this.actions = actions;
        this.accessibility = accessibility;
    }

    public boolean handle(JSONObject command, Reply reply) {
        if (command == null) return false;
        String requestId = command.optString("id", command.optString("requestId", ""));
        String type = command.optString("action", command.optString("command", ""));
        String value = command.optString("value", "");
        if (type.isEmpty() || value.length() > MAX_VALUE_LENGTH) {
            reply.send(NovaProtocol.result(requestId, false, "Invalid command."));
            return false;
        }

        // Only expose actions that already pass through Android's permission-controlled APIs.
        boolean allowed = type.equals("home") || type.equals("back") || type.equals("recents")
                || type.equals("notifications") || type.equals("quick_settings")
                || type.equals("scroll_up") || type.equals("scroll_down")
                || type.equals("swipe_left") || type.equals("swipe_right")
                || type.equals("click_text") || type.equals("open_url")
                || type.equals("open_package") || type.equals("settings");
        if (!allowed) {
            reply.send(NovaProtocol.result(requestId, false, "Action is not exposed by this device node."));
            return false;
        }

        boolean ok;
        try { ok = actions.execute(type, value); }
        catch (Exception e) { Log.w(TAG, "Command failed", e); ok = false; }
        reply.send(NovaProtocol.result(requestId, ok, ok ? "Action completed." : "Action failed or permission is unavailable."));
        return ok;
    }
}
