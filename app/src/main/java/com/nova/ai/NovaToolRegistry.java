package com.nova.ai;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Central registry for explicitly enabled NOVA capabilities. */
public final class NovaToolRegistry {
    private final Map<String, NovaTool> tools = new LinkedHashMap<>();
    public synchronized boolean register(NovaTool tool) { if (tool == null || tool.id() == null || tool.id().trim().isEmpty()) return false; if (tools.containsKey(tool.id())) return false; tools.put(tool.id(), tool); return true; }
    public synchronized boolean unregister(String id) { return tools.remove(id) != null; }
    public synchronized NovaTool get(String id) { return tools.get(id); }
    public synchronized Map<String, NovaTool> snapshot() { return Collections.unmodifiableMap(new LinkedHashMap<>(tools)); }
    public synchronized java.util.Set<String> ids() { return Collections.unmodifiableSet(new java.util.LinkedHashSet<>(tools.keySet())); }
    public synchronized JSONArray describe() { JSONArray result = new JSONArray(); for (NovaTool tool : tools.values()) { JSONObject item = new JSONObject(); try { item.put("id", tool.id()); item.put("description", tool.description()); item.put("schema", tool.schema()); } catch (Exception ignored) {} result.put(item); } return result; }
}
