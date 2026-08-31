package com.nova.ai;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import java.net.URI;
import java.util.UUID;

/** Reliable Android node transport. It reconnects with bounded exponential backoff and never executes remote work itself. */
public final class NovaDeviceGateway {
    public interface Listener {
        void onEvent(JSONObject event);
        void onState(String state);
    }

    private static final String TAG = "NOVA-Gateway";
    private static final long MIN_RECONNECT_MS = 1000L;
    private static final long MAX_RECONNECT_MS = 30000L;

    private final Handler main = new Handler(Looper.getMainLooper());
    private final String nodeId = "android-" + UUID.randomUUID();
    private final Listener listener;
    private final Object lock = new Object();
    private WebSocketClient socket;
    private String url;
    private boolean shouldReconnect;
    private long reconnectDelay = MIN_RECONNECT_MS;

    public NovaDeviceGateway(Listener listener) { this.listener = listener; }
    public String nodeId() { return nodeId; }

    public void connect(String wsUrl) {
        synchronized (lock) {
            url = wsUrl == null ? "" : wsUrl.trim();
            shouldReconnect = !url.isEmpty();
            reconnectDelay = MIN_RECONNECT_MS;
            closeSocketLocked();
        }
        if (!url.isEmpty()) openSocket(); else state("DISCONNECTED");
    }

    private void openSocket() {
        final String target;
        synchronized (lock) { if (!shouldReconnect || url.isEmpty()) return; target = url; }
        try {
            WebSocketClient client = new WebSocketClient(new URI(target)) {
                @Override public void onOpen(ServerHandshake handshake) {
                    synchronized (lock) { reconnectDelay = MIN_RECONNECT_MS; socket = this; }
                    state("CONNECTED");
                    send(NovaProtocol.hello(nodeId, "android", "0.2.0"));
                }
                @Override public void onMessage(String message) {
                    try {
                        JSONObject event = new JSONObject(message);
                        main.post(() -> listener.onEvent(event));
                    } catch (Exception e) { Log.w(TAG, "Ignoring malformed gateway message", e); }
                }
                @Override public void onClose(int code, String reason, boolean remote) {
                    synchronized (lock) { if (socket == this) socket = null; }
                    state("DISCONNECTED");
                    scheduleReconnect();
                }
                @Override public void onError(Exception ex) {
                    Log.w(TAG, "Gateway error", ex);
                    state("ERROR");
                }
            };
            synchronized (lock) { socket = client; }
            client.connect();
        } catch (Exception e) {
            Log.w(TAG, "Gateway connect failed", e);
            state("ERROR");
            scheduleReconnect();
        }
    }

    private void scheduleReconnect() {
        final long delay;
        synchronized (lock) {
            if (!shouldReconnect || url.isEmpty()) return;
            delay = reconnectDelay;
            reconnectDelay = Math.min(MAX_RECONNECT_MS, reconnectDelay * 2L);
        }
        main.postDelayed(this::openSocket, delay);
    }

    public boolean send(JSONObject event) {
        WebSocketClient s;
        synchronized (lock) { s = socket; }
        if (s == null || !s.isOpen() || event == null) return false;
        try { s.send(event.toString()); return true; }
        catch (Exception e) { Log.w(TAG, "Send failed", e); return false; }
    }

    public void disconnect() {
        synchronized (lock) {
            shouldReconnect = false;
            reconnectDelay = MIN_RECONNECT_MS;
            closeSocketLocked();
        }
        state("DISCONNECTED");
    }

    private void closeSocketLocked() {
        if (socket != null) {
            try { socket.close(); } catch (Exception ignored) { }
            socket = null;
        }
    }

    public boolean isConnected() { synchronized (lock) { return socket != null && socket.isOpen(); } }
    private void state(String value) { main.post(() -> listener.onState(value)); }
}
