package com.nova.ai;

import android.os.Handler;
import android.os.Looper;
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

/** OpenAI-compatible HTTP provider. Credentials are supplied at runtime and are never hard-coded. */
public final class NovaOpenAiCompatibleProvider implements NovaAiProvider {
    private final String endpoint;
    private final String apiKey;
    private final String model;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    public NovaOpenAiCompatibleProvider(String baseUrl, String apiKey, String model) {
        String base = baseUrl == null ? "" : baseUrl.trim();
        if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        endpoint = base.endsWith("/chat/completions") ? base : base + "/chat/completions";
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model == null ? "" : model.trim();
    }

    @Override public void complete(String systemContext, String userInput, JSONObject toolCatalog, Callback callback) {
        if (endpoint.isEmpty() || apiKey.isEmpty() || model.isEmpty()) {
            postError(callback, new IllegalStateException("AI provider is not configured")); return;
        }
        executor.execute(() -> {
            HttpURLConnection connection = null;
            try {
                JSONObject body = new JSONObject();
                body.put("model", model);
                body.put("messages", buildMessages(systemContext, userInput, toolCatalog));
                body.put("temperature", 0.2);
                body.put("tool_choice", "auto");
                JSONArray tools = toOpenAiTools(toolCatalog == null ? new JSONArray() : toolCatalog.optJSONArray("tools"));
                if (tools.length() > 0) body.put("tools", tools);

                connection = (HttpURLConnection) new URL(endpoint).openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(60000);
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Authorization", "Bearer " + apiKey);
                byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
                try (OutputStream out = connection.getOutputStream()) { out.write(payload); }
                int code = connection.getResponseCode();
                InputStream stream = code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream();
                String response = read(stream);
                if (code < 200 || code >= 300) throw new IllegalStateException("AI HTTP " + code + ": " + response);
                JSONObject parsed = new JSONObject(response);
                main.post(() -> callback.onSuccess(parsed));
            } catch (Exception error) { postError(callback, error); }
            finally { if (connection != null) connection.disconnect(); }
        });
    }

    private static JSONArray buildMessages(String system, String input, JSONObject catalog) throws Exception {
        JSONArray messages = new JSONArray();
        messages.put(new JSONObject().put("role", "system").put("content", system == null ? "" : system));
        JSONArray context = catalog == null ? null : catalog.optJSONArray("context");
        if (context != null) for (int i = 0; i < context.length(); i++) {
            JSONObject item = context.optJSONObject(i);
            if (item != null) messages.put(new JSONObject().put("role", item.optString("role", "user")).put("content", item.optString("content", "")));
        }
        messages.put(new JSONObject().put("role", "user").put("content", input == null ? "" : input));
        return messages;
    }

    private static JSONArray toOpenAiTools(JSONArray catalog) throws Exception {
        JSONArray result = new JSONArray();
        if (catalog == null) return result;
        for (int i = 0; i < catalog.length(); i++) {
            JSONObject item = catalog.optJSONObject(i);
            if (item == null) continue;
            JSONObject fn = new JSONObject().put("name", item.optString("id", ""))
                    .put("description", item.optString("description", ""))
                    .put("parameters", item.optJSONObject("schema"));
            if (!fn.optString("name").isEmpty()) result.put(new JSONObject().put("type", "function").put("function", fn));
        }
        return result;
    }

    private static String read(InputStream stream) throws Exception {
        if (stream == null) return "";
        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line; while ((line = reader.readLine()) != null) result.append(line);
        }
        return result.toString();
    }

    private void postError(Callback callback, Exception error) { if (callback != null) main.post(() -> callback.onError(error)); }
}
