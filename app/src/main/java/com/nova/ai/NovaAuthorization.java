package com.nova.ai;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;

/** Explicit capability gate for agent tools. Read-only observation/research primitives are enabled by default. */
public final class NovaAuthorization {
    private static final String PREFS="nova_capabilities";private final SharedPreferences prefs;
    public NovaAuthorization(Context context){prefs=context.getApplicationContext().getSharedPreferences(PREFS,Context.MODE_PRIVATE);}
    public boolean isAllowed(String toolId){if(toolId==null||toolId.trim().isEmpty())return false;return toolId.equals("nova.echo")||toolId.equals("memory.append_context")||toolId.equals("memory.remember")||toolId.equals("memory.recall")||toolId.equals("memory.search")||toolId.equals("preference.manage")||toolId.equals("android.open_app")||toolId.equals("android.action")||toolId.equals("web.search")||toolId.equals("web.fetch")||toolId.equals("web.research")||toolId.equals("screen.observe")||toolId.equals("notification.recent")||toolId.equals("device.list")||toolId.equals("system.time")||toolId.equals("system.status")||toolId.equals("task.schedule")||toolId.equals("task.list")||toolId.equals("task.cancel_scheduled")||toolId.equals("device.send_command")||prefs.getBoolean("allow_"+toolId,false);}
    public void setAllowed(String toolId,boolean allowed){if(toolId==null||toolId.trim().isEmpty())return;prefs.edit().putBoolean("allow_"+toolId,allowed).apply();}
    public JSONObject check(String toolId){JSONObject result=new JSONObject();try{result.put("tool",toolId==null?"":toolId).put("allowed",isAllowed(toolId));}catch(Exception ignored){}return result;}
}
