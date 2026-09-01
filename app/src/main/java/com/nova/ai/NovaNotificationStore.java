package com.nova.ai;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;

/** Local bounded notification metadata store; notification text is not persisted. */
public final class NovaNotificationStore {
    private static final String P="nova_notifications_v1"; private static final String K="items";
    private NovaNotificationStore(){}
    public static synchronized void record(Context c,String packageName,boolean hasIcon){try{SharedPreferences p=c.getApplicationContext().getSharedPreferences(P,Context.MODE_PRIVATE);JSONArray a=new JSONArray(p.getString(K,"[]"));a.put(new JSONObject().put("package",packageName==null?"":packageName).put("time",System.currentTimeMillis()).put("hasIcon",hasIcon));while(a.length()>50)a.remove(0);p.edit().putString(K,a.toString()).apply();}catch(Exception ignored){}}
    public static synchronized JSONArray recent(Context c){try{return new JSONArray(c.getApplicationContext().getSharedPreferences(P,Context.MODE_PRIVATE).getString(K,"[]"));}catch(Exception e){return new JSONArray();}}
    public static synchronized void clear(Context c){c.getApplicationContext().getSharedPreferences(P,Context.MODE_PRIVATE).edit().remove(K).apply();}
}
