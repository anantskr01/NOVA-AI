package com.nova.ai;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;

/** Persistent task definitions used by the OS alarm receiver and background executor. */
public final class NovaScheduledTaskStore {
    private static final String PREFS="nova_scheduled_tasks_v1"; private static final String DATA="tasks";
    private final SharedPreferences prefs;
    public NovaScheduledTaskStore(Context context){prefs=context.getApplicationContext().getSharedPreferences(PREFS,Context.MODE_PRIVATE);}
    public synchronized void put(String id,String prompt,boolean execute){put(id,prompt,execute,0);}
    public synchronized void put(String id,String prompt,boolean execute,long repeatEveryMs){try{JSONObject all=new JSONObject(prefs.getString(DATA,"{}"));all.put(id,new JSONObject().put("id",id).put("prompt",prompt==null?"":prompt).put("execute",execute).put("repeatEveryMs",repeatEveryMs));prefs.edit().putString(DATA,all.toString()).apply();}catch(Exception ignored){}}
    public synchronized JSONObject get(String id){try{return new JSONObject(prefs.getString(DATA,"{}")).optJSONObject(id);}catch(Exception e){return null;}}
    public synchronized void remove(String id){try{JSONObject all=new JSONObject(prefs.getString(DATA,"{}"));all.remove(id);prefs.edit().putString(DATA,all.toString()).apply();}catch(Exception ignored){}}
    public synchronized JSONArray list(){try{JSONObject all=new JSONObject(prefs.getString(DATA,"{}"));JSONArray out=new JSONArray();java.util.Iterator<String> keys=all.keys();while(keys.hasNext())out.put(all.optJSONObject(keys.next()));return out;}catch(Exception e){return new JSONArray();}}
}
