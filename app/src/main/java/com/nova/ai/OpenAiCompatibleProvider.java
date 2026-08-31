package com.nova.ai;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Generic OpenAI-compatible HTTP provider. Credentials are supplied at runtime and are never stored in source.
 * The endpoint should be the provider's chat-completions endpoint.
 */
public final class OpenAiCompatibleProvider implements NovaAiProvider {
    private final String endpoint;
    private final String apiKey;
    private final String model;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public OpenAiCompatibleProvider(String endpoint, String apiKey, String model) {
        if (endpoint == null || endpoint.trim().isEmpty()) throw new IllegalArgumentException("endpoint");
        if (model == null || model.trim().isEmpty()) throw new IllegalArgumentException("model");
        this.endpoint = endpoint;
        this.apiKey = apiKey == null ? "" : apiKey;
        this.model = model;
    }

    @Override public void complete(String systemContext, String userInput, JSONObject toolCatalog, Callback callback) {
        executor.execute(() -> {
            try {
                JSONObject body = new JSONObject();
                JSONArray messages = new JSONArray();
                messages.put(new JSONObject().put("role", "system").put("content", systemContext == null ? "" : systemContext));
                messages.put(new JSONObject().put("role", "user").put("content", userInput == null ? "" : userInput));
                body.put("model", model);
                body.put("messages", messages);
                body.put("tools", toolCatalog == null ? new JSONArray() : toolCatalog.optJSONArray("tools"));

                HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(45000);
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");
                if (!apiKey.isEmpty()) connection.setRequestProperty("Authorization", "Bearer " + apiKey);

                byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
                try (OutputStream out = connection.getOutputStream()) { out.write(payload); }

                int status = connection.getResponseCode();
                InputStream stream = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream();
                String responseText = read(stream);
                if (status < 200 || status >= 300) throw new IllegalStateException("provider_http_" + status + ": " + responseText);

                JSONObject response = new JSONObject(responseText);
                if (callback != null) callback.onSuccess(response);
                connection.disconnect();
            } catch (Exception e) {
                if (callback != null) callback.onError(e);
            }
        });
    }

    private static String read(InputStream stream) throws Exception {
        if (stream == null) return "";
        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) result.append(line);
        }
        return result.toString();
    }

    public void shutdown() { executor.shutdownNow(); }
}
