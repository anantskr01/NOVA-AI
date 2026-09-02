package com.nova.ai;

import android.content.Context;
import org.json.JSONObject;

/** Agent facade backed by the shared application runtime and optionally a real model loop. */
public final class NovaAgent {
    public interface Callback { void onComplete(JSONObject result); }
    private final NovaRuntime runtime;
    private final NovaContextStore contextStore;
    private final NovaModelLoop modelLoop;
    private NovaAiProvider provider;

    public NovaAgent(Context context) {
        Context app = context.getApplicationContext();
        runtime = NovaRuntime.get(app);
        contextStore = new NovaContextStore(app);
        modelLoop = new NovaModelLoop(app);
    }

    public void setProvider(NovaAiProvider provider) { this.provider = provider; modelLoop.setProvider(provider); }
    public NovaToolRegistry tools() { return runtime.tools(); }
    public NovaContextStore context() { return contextStore; }

    public void handle(String request, Callback callback) {
        String text = normalize(request);
        if (text.isEmpty()) { finish(callback, false, "Tell me what you want me to do."); return; }
        if (provider != null) { modelLoop.run(text, result -> { if (callback != null) callback.onComplete(result); }); return; }
        if (callback != null) callback.onComplete(executeLocal(text));
    }

    /** Deterministic local command router keeps core Android commands usable without a cloud model. */
    private JSONObject executeLocal(String text) {
        JSONObject result = new JSONObject();
        try {
            String lower = text.toLowerCase();
            if (lower.equals("hi") || lower.equals("hello") || lower.equals("hey"))
                return result.put("ok", true).put("mode", "local").put("text", "Hello. NOVA is online and ready.");
            if (lower.startsWith("remember ")) {
                String memory = text.substring(9).trim();
                JSONObject out = runtime.executor().execute("memory.remember", new JSONObject().put("text", memory));
                boolean ok = out.optBoolean("ok", false);
                return result.put("ok", ok).put("mode", "local").put("text", ok ? "I'll remember that." : "I couldn't save that memory.").put("execution", out);
            }
            if (lower.equals("home") || lower.equals("go home")) return androidAction(result, "home", "", "Going home.");
            if (lower.equals("back") || lower.equals("go back")) return androidAction(result, "back", "", "Going back.");
            if (lower.contains("open settings") || lower.equals("settings")) return androidAction(result, "settings", "", "Opening settings.");
            if (lower.contains("notifications")) return androidAction(result, "notifications", "", "Opening notifications.");
            if (lower.contains("quick settings")) return androidAction(result, "quick_settings", "", "Opening quick settings.");
            if (lower.contains("scroll down")) return androidAction(result, "scroll_down", "", "Scrolling down.");
            if (lower.contains("scroll up")) return androidAction(result, "scroll_up", "", "Scrolling up.");
            if (lower.startsWith("open https://") || lower.startsWith("open http://")) return androidAction(result, "open_url", text.substring(5).trim(), "Opening the link.");
            String app = requestedApp(text);
            if (app != null) return openApp(result, app);
            if (lower.equals("help") || lower.startsWith("what can you do"))
                return result.put("ok", true).put("mode", "local").put("text", "I can open apps, open links, navigate Android, scroll, and store memories. A model provider adds broader reasoning.");
            return result.put("ok", true).put("mode", "local").put("text", "I heard you: " + text + ". Try 'open YouTube', 'go home', or 'open settings'.");
        } catch (Exception e) {
            return result.put("ok", false).put("error", e.getClass().getSimpleName()).put("text", "NOVA couldn't complete that request.");
        }
    }

    private JSONObject openApp(JSONObject result, String packageName) throws Exception {
        JSONObject execution = runtime.executor().execute("android.open_app", new JSONObject().put("package", packageName));
        JSONObject output = execution.optJSONObject("output");
        boolean ok = execution.optBoolean("ok", false) && output != null && output.optBoolean("opened", false);
        return result.put("ok", ok).put("mode", "local").put("text", ok ? "Opening the app." : "I couldn't open that app. Make sure it is installed and NOVA has permission.").put("execution", execution);
    }

    private JSONObject androidAction(JSONObject result, String action, String value, String successText) throws Exception {
        JSONObject execution = runtime.executor().execute("android.action", new JSONObject().put("action", action).put("value", value));
        JSONObject output = execution.optJSONObject("output");
        boolean ok = execution.optBoolean("ok", false) && output != null && output.optBoolean("success", false);
        return result.put("ok", ok).put("mode", "local").put("text", ok ? successText : "I couldn't perform that action. Check NOVA's Accessibility permission.").put("execution", execution);
    }

    private static String requestedApp(String text) {
        String lower = text.toLowerCase().trim();
        if (!(lower.startsWith("open ") || lower.startsWith("launch ") || lower.startsWith("start "))) return null;
        String name = lower.replaceFirst("^(open|launch|start)\\s+", "").trim();
        if (name.equals("youtube") || name.equals("you tube")) return "com.google.android.youtube";
        if (name.equals("chrome") || name.equals("google chrome")) return "com.android.chrome";
        if (name.equals("gmail")) return "com.google.android.gm";
        if (name.equals("maps") || name.equals("google maps")) return "com.google.android.apps.maps";
        if (name.equals("spotify")) return "com.spotify.music";
        return null;
    }

    private static String normalize(String request) {
        String text = request == null ? "" : request.trim();
        String lower = text.toLowerCase();
        if (lower.startsWith("hey nova")) return text.substring(8).trim();
        if (lower.startsWith("nova")) return text.substring(4).trim();
        return text;
    }

    private static void finish(Callback callback, boolean ok, String text) {
        if (callback == null) return;
        JSONObject result = new JSONObject();
        try { result.put("ok", ok).put("text", text); } catch (Exception ignored) { }
        callback.onComplete(result);
    }
}
