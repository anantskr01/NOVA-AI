package com.nova.ai;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import java.net.URI;
import java.util.UUID;

/** Lightweight cross-device node bridge inspired by Jarvis OS's daemon/gateway model. */
public final class NovaDeviceGateway {
    public interface Listener { void onEvent(JSONObject event); void onState(String state); }
    private static final String TAG = "NOVA-Gateway";
    private final Handler main = new Handler(Looper.getMainLooper());
    private final String nodeId = "android-" + UUID.randomUUID().toString();
    private final Listener listener;
    private WebSocketClient socket;

    public NovaDeviceGateway(Listener listener) { this.listener = listener; }
    public String nodeId() { return nodeId; }

    public synchronized void connect(String wsUrl) {
        disconnect();
        try {
            URI uri = new URI(wsUrl);
            socket = new WebSocketClient(uri) {
                @Override public void onOpen(ServerHandshake handshake) {
                    state("CONNECTED");
                    JSONObject hello = new JSONObject();
                    try { hello.put("type", "node.hello").put("nodeId", nodeId).put("platform", "android").put("version", "0.1.0"); send(hello.toString()); }
                    catch (Exception e) { Log.e(TAG, "hello", e); }
                }
                @Override public void onMessage(String message) {
                    try { JSONObject event = new JSONObject(message); main.post(() -> listener.onEvent(event)); }
                    catch (Exception e) { Log.w(TAG, "Ignoring malformed gateway event", e); }
                }
                @Override public void onClose(int code, String reason, boolean remote) { state("DISCONNECTED"); }
                @Override public void onError(Exception ex) { Log.w(TAG, "Gateway error", ex); state("ERROR"); }
            };
            socket.connect();
        } catch (Exception e) { Log.e(TAG, "Gateway connect failed", e); state("ERROR"); }
    }

    public synchronized boolean send(JSONObject event) {
        if (socket == null || !socket.isOpen()) return false;
        socket.send(event.toString()); return true;
    }

    public synchronized void disconnect() { if (socket != null) { try { socket.close(); } catch (Exception ignored) {} socket = null; } }
    public synchronized boolean isConnected() { return socket != null && socket.isOpen(); }
    private void state(String s) { main.post(() -> listener.onState(s)); }
}
