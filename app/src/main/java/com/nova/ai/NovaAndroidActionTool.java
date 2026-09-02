package com.nova.ai;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;

/** Unified local Android action tool. */
public final class NovaAndroidActionTool implements NovaTool {
    private final NovaActionEngine actions;
    public NovaAndroidActionTool(Context context){actions=new NovaActionEngine(context);}
    @Override public String id(){return "android.action";}
    @Override public String description(){return "Interact with Android: home, back, recents, notifications, quick settings, settings, app/package launch, URLs, scroll/swipe, click visible text, click resource id, long press visible text and type text.";}
    @Override public JSONObject schema(){try{return new JSONObject().put("type","object").put("required",new JSONArray().put("action")).put("properties",new JSONObject().put("action",new JSONObject().put("type","string")).put("value",new JSONObject().put("type","string")));}catch(Exception e){return new JSONObject();}}
    @Override public JSONObject execute(JSONObject input)throws Exception{String action=input==null?"":input.optString("action","").trim().toLowerCase();String value=input==null?"":input.optString("value","");if(!isSupported(action))throw new IllegalArgumentException("unsupported_android_action");if(value.length()>4096)throw new IllegalArgumentException("value_too_long");if("open_url".equals(action)&&!(value.startsWith("https://")||value.startsWith("http://")))throw new IllegalArgumentException("http_url_required");boolean ok=actions.execute(action,value);return new JSONObject().put("action",action).put("success",ok);}
    private static boolean isSupported(String a){return "home".equals(a)||"back".equals(a)||"recents".equals(a)||"notifications".equals(a)||"quick_settings".equals(a)||"scroll_up".equals(a)||"scroll_down".equals(a)||"swipe_up".equals(a)||"swipe_down".equals(a)||"swipe_left".equals(a)||"swipe_right".equals(a)||"click_text".equals(a)||"click_resource_id".equals(a)||"long_press_text".equals(a)||"type_text".equals(a)||"open_url".equals(a)||"open_package".equals(a)||"settings".equals(a);}
}
