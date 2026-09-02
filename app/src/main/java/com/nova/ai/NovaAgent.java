package com.nova.ai;

import android.content.Context;
import org.json.JSONObject;

/** Agent facade backed by the shared application runtime and a configurable real model provider. */
public final class NovaAgent {
    public interface Callback { void onComplete(JSONObject result); }
    private final Context context;
    private final NovaRuntime runtime;
    private final NovaContextStore contextStore;
    private final NovaModelLoop modelLoop;
    private final NovaProviderConfig providerConfig;
    private NovaAiProvider provider;
    private volatile String currentTaskId = "";

    public NovaAgent(Context context) {
        this.context = context.getApplicationContext();
        runtime = NovaRuntime.get(this.context);
        contextStore = new NovaContextStore(this.context);
        modelLoop = new NovaModelLoop(this.context);
        providerConfig = new NovaProviderConfig(this.context);
        configureFromSavedProvider();
    }

    public void configureFromSavedProvider() {
        if (providerConfig.isConfigured()) setProvider(new NovaOpenAiCompatibleProvider(providerConfig.endpoint(), providerConfig.apiKey(), providerConfig.model()));
        else setProvider(null);
    }

    public boolean saveProvider(String endpoint, String model, String apiKey) {
        boolean saved = providerConfig.save(endpoint, model, apiKey);
        if (saved) configureFromSavedProvider();
        return saved;
    }

    public void clearProvider() { providerConfig.clear(); setProvider(null); }
    public boolean providerConfigured() { return providerConfig.isConfigured(); }
    public String providerEndpoint() { return providerConfig.endpoint(); }
    public String providerModel() { return providerConfig.model(); }
    public void setProvider(NovaAiProvider provider) { this.provider = provider; modelLoop.setProvider(provider); }
    public NovaToolRegistry tools() { return runtime.tools(); }
    public NovaContextStore context() { return contextStore; }
    public String currentTaskId() { return currentTaskId; }
    public void cancelCurrentTask() { if (!currentTaskId.isEmpty()) modelLoop.cancel(currentTaskId); }

    public void handle(String request, Callback callback) {
        String text = normalize(request);
        if (text.isEmpty()) { finish(callback, false, "Tell me what you want me to do."); return; }
        if (provider != null) {
            currentTaskId = modelLoop.run(text, result -> { currentTaskId = ""; if (callback != null) callback.onComplete(result); });
            return;
        }
        if (callback != null) callback.onComplete(executeLocal(text));
    }

    /** Compatibility entry point for the legacy conversation engine. */
    public JSONObject plan(String request) {
        final JSONObject[] holder = new JSONObject[1];
        handle(request, result -> holder[0] = result);
        return holder[0] == null ? errorResult("Unable to produce a plan.") : holder[0];
    }

    private JSONObject executeLocal(String text) {
        JSONObject result = new JSONObject();
        try {
            String lower = text.toLowerCase();
            if (lower.equals("hi") || lower.equals("hello") || lower.equals("hey")) return result.put("ok",true).put("mode","local").put("text","Hello. NOVA is online and ready.");
            if (lower.startsWith("remember ")) { String value=text.substring(9).trim(); JSONObject out=runtime.executor().execute("memory.remember",new JSONObject().put("key","user_fact_"+System.currentTimeMillis()).put("value",value)); boolean ok=out.optBoolean("ok",false); return result.put("ok",ok).put("mode","local").put("text",ok?"I'll remember that.":"I couldn't save that memory.").put("execution",out); }
            if (lower.equals("home")||lower.equals("go home"))return androidAction(result,"home","","Going home.");
            if (lower.equals("back")||lower.equals("go back"))return androidAction(result,"back","","Going back.");
            if (lower.contains("open settings")||lower.equals("settings"))return androidAction(result,"settings","","Opening settings.");
            if (lower.contains("notifications"))return androidAction(result,"notifications","","Opening notifications.");
            if (lower.contains("quick settings"))return androidAction(result,"quick_settings","","Opening quick settings.");
            if (lower.contains("scroll down"))return androidAction(result,"scroll_down","","Scrolling down.");
            if (lower.contains("scroll up"))return androidAction(result,"scroll_up","","Scrolling up.");
            if (lower.startsWith("open https://")||lower.startsWith("open http://"))return androidAction(result,"open_url",text.substring(5).trim(),"Opening the link.");
            String app=requestedApp(text); if(app!=null)return openApp(result,app);
            if(lower.equals("help")||lower.startsWith("what can you do"))return result.put("ok",true).put("mode","local").put("text","With the AI provider connected, I can reason over natural language, research the web, observe the screen, control apps, schedule tasks and coordinate trusted devices.");
            return result.put("ok",true).put("mode","local").put("text","I heard you: "+text+". Connect an AI provider for general natural-language execution.");
        }catch(Exception e){
            try { return result.put("ok",false).put("error",e.getClass().getSimpleName()).put("text","NOVA couldn't complete that request."); }
            catch(Exception ignored){ return result; }
        }
    }

    private JSONObject openApp(JSONObject result,String packageName)throws Exception{JSONObject execution=runtime.executor().execute("android.open_app",new JSONObject().put("package",packageName));JSONObject output=execution.optJSONObject("output");boolean ok=execution.optBoolean("ok",false)&&output!=null&&output.optBoolean("opened",false);return result.put("ok",ok).put("mode","local").put("text",ok?"Opening the app.":"I couldn't open that app. Make sure it is installed and NOVA has permission.").put("execution",execution);}
    private JSONObject androidAction(JSONObject result,String action,String value,String successText)throws Exception{JSONObject execution=runtime.executor().execute("android.action",new JSONObject().put("action",action).put("value",value));JSONObject output=execution.optJSONObject("output");boolean ok=execution.optBoolean("ok",false)&&output!=null&&output.optBoolean("success",false);return result.put("ok",ok).put("mode","local").put("text",ok?successText:"I couldn't perform that action. Check NOVA's Accessibility permission.").put("execution",execution);}
    private static String requestedApp(String text){String lower=text.toLowerCase().trim();if(!(lower.startsWith("open ")||lower.startsWith("launch ")||lower.startsWith("start ")))return null;String name=lower.replaceFirst("^(open|launch|start)\\s+","").trim();if(name.equals("youtube")||name.equals("you tube"))return"com.google.android.youtube";if(name.equals("chrome")||name.equals("google chrome"))return"com.android.chrome";if(name.equals("gmail"))return"com.google.android.gm";if(name.equals("maps")||name.equals("google maps"))return"com.google.android.apps.maps";if(name.equals("spotify"))return"com.spotify.music";return null;}
    private static String normalize(String request){String text=request==null?"":request.trim();String lower=text.toLowerCase();if(lower.startsWith("hey nova"))return text.substring(8).trim();if(lower.startsWith("nova"))return text.substring(4).trim();return text;}
    private static void finish(Callback callback,boolean ok,String text){if(callback==null)return;JSONObject result=new JSONObject();try{result.put("ok",ok).put("text",text);}catch(Exception ignored){}callback.onComplete(result);}
    private static JSONObject errorResult(String text){JSONObject result=new JSONObject();try{result.put("ok",false).put("error","plan_failed").put("text",text);}catch(Exception ignored){}return result;}
}
