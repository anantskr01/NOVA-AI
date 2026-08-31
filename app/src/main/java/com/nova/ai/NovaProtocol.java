package com.nova.ai;

import org.json.JSONObject;

import java.util.UUID;

/** Stable wire protocol shared by NOVA central services and device nodes. */
public final class NovaProtocol {
    private NovaProtocol() { }

    public static final String VERSION = "1";
    public static final String HELLO = "node.hello";
    public static final String READY = "node.ready";
    public static final String HEARTBEAT = "node.heartbeat";
    public static final String COMMAND = "node.command";
    public static final String RESULT = "node.result";
    public static final String EVENT = "node.event";
    public static final String ERROR = "node.error";

    public static String id() { return UUID.randomUUID().toString(); }

    public static JSONObject hello(String nodeId, String platform, String version) {
        JSONObject o = new JSONObject();
        try {
            o.put("v", VERSION).put("type", HELLO).put("id", id())
                    .put("nodeId", nodeId).put("platform", platform).put("version", version)
                    .put("capabilities", new org.json.JSONArray()
                            .put("accessibility").put("screen_context").put("camera").put("gesture"));
        } catch (Exception ignored) { }
        return o;
    }

    public static JSONObject result(String requestId, boolean ok, String message) {
        JSONObject o = new JSONObject();
        try { o.put("v", VERSION).put("type", RESULT).put("id", id()).put("requestId", requestId).put("ok", ok).put("message", message == null ? "" : message); }
        catch (Exception ignored) { }
        return o;
    }
}
