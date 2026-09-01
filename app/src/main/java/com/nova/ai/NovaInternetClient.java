package com.nova.ai;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Small allowlisted HTTPS client for agent web access. */
public final class NovaInternetClient {
    public interface Callback { void onSuccess(String body, int code); void onError(Exception error); }
    private final ExecutorService executor = Executors.newCachedThreadPool();
    public void get(String address, Callback callback) {
        executor.execute(() -> {
            HttpURLConnection connection = null;
            try {
                URI uri = URI.create(address);
                String scheme = uri.getScheme();
                if (!"https".equalsIgnoreCase(scheme)) throw new IllegalArgumentException("https_required");
                if (uri.getHost() == null || uri.getUserInfo() != null) throw new IllegalArgumentException("invalid_url");
                URL url = uri.toURL(); connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET"); connection.setConnectTimeout(8000); connection.setReadTimeout(10000);
                connection.setInstanceFollowRedirects(false); connection.setRequestProperty("User-Agent", "NOVA-AI/1.0");
                int code = connection.getResponseCode();
                if (code >= 300 && code < 400) throw new IllegalStateException("redirect_not_allowed");
                BufferedReader reader = new BufferedReader(new InputStreamReader(code >= 400 ? connection.getErrorStream() : connection.getInputStream()));
                StringBuilder body = new StringBuilder(); String line; int total = 0;
                while ((line = reader.readLine()) != null && total < 262144) { body.append(line).append('\n'); total = body.length(); }
                reader.close(); if (callback != null) callback.onSuccess(body.toString(), code);
            } catch (Exception e) { if (callback != null) callback.onError(e); }
            finally { if (connection != null) connection.disconnect(); }
        });
    }
    public void shutdown() { executor.shutdownNow(); }
}
