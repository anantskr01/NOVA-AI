package com.nova.ai;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.LinkedHashMap;
import java.util.Map;

/** In-memory registry for trusted remote NOVA nodes. Secrets are never stored here. */
public final class NovaDeviceRegistry {
    public static final class Device {
        public final String id; public String name; public String type; public String state; public long lastSeen;
        Device(String id,String name,String type,String state,long lastSeen){this.id=id;this.name=name;this.type=type;this.state=state;this.lastSeen=lastSeen;}
        JSONObject toJson(){try{return new JSONObject().put("id",id).put("name",name).put("type",type).put("state",state).put("lastSeen",lastSeen);}catch(Exception e){return new JSONObject();}}
    }
    private final Map<String,Device> devices=new LinkedHashMap<>();
    public synchronized void upsert(String id,String name,String type,String state){if(id==null||id.trim().isEmpty())return;Device d=devices.get(id);if(d==null){d=new Device(id,name,type,state,System.currentTimeMillis());devices.put(id,d);}else{if(state!=null)d.state=state;if(name!=null&&!name.isEmpty())d.name=name;if(type!=null&&!type.isEmpty())d.type=type;d.lastSeen=System.currentTimeMillis();}}
    public synchronized void markState(String id,String state){Device d=devices.get(id);if(d!=null){if(state!=null)d.state=state;d.lastSeen=System.currentTimeMillis();}}
    public synchronized Device get(String id){return devices.get(id);}
    public synchronized boolean remove(String id){return devices.remove(id)!=null;}
    public synchronized JSONArray list(){JSONArray a=new JSONArray();for(Device d:devices.values())a.put(d.toJson());return a;}
    public synchronized void clear(){devices.clear();}
}
