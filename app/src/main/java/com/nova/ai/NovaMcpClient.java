package com.nova.ai;

import org.json.JSONArray;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Minimal MCP JSON-RPC client. Only HTTPS endpoints are accepted; no arbitrary code is executed. */
public final class NovaMcpClient {
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private int nextId = 1;
    public interface Callback { void onSuccess(JSONObject result); void onError(Exception error); }
    public void call(String endpoint, String method, JSONObject params, Callback cb) {
        executor.execute(() -> {
            HttpURLConnection c = null;
            try {
                URI uri = URI.create(endpoint);
                if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null) throw new IllegalArgumentException("https_required");
                JSONObject request = new JSONObject().put("jsonrpc","2.0").put("id", nextId++).put("method",method);
                if (params != null) request.put("params", params);
                URL url = uri.toURL(); c=(HttpURLConnection)url.openConnection(); c.setRequestMethod("POST"); c.setConnectTimeout(8000); c.setReadTimeout(15000); c.setDoOutput(true);
                c.setRequestProperty("Content-Type","application/json"); c.setRequestProperty("Accept","application/json");
                byte[] bytes=request.toString().getBytes(StandardCharsets.UTF_8); c.setFixedLengthStreamingMode(bytes.length);
                try(OutputStream out=c.getOutputStream()){out.write(bytes);}
                int code=c.getResponseCode(); if(code<200||code>=300) throw new IllegalStateException("mcp_http_"+code);
                java.io.InputStream in=c.getInputStream(); java.io.ByteArrayOutputStream b=new java.io.ByteArrayOutputStream(); byte[] buf=new byte[4096]; int n,total=0;
                while((n=in.read(buf))!=-1&&total<262144){b.write(buf,0,n);total+=n;} in.close();
                JSONObject response=new JSONObject(new String(b.toByteArray(),StandardCharsets.UTF_8)); if(response.has("error")) throw new IllegalStateException(response.get("error").toString());
                if(cb!=null)cb.onSuccess(response.optJSONObject("result"));
            }catch(Exception e){if(cb!=null)cb.onError(e);}finally{if(c!=null)c.disconnect();}
        });
    }
    public void listTools(String endpoint, Callback cb){call(endpoint,"tools/list",new JSONObject(),cb);}
    public void shutdown(){executor.shutdownNow();}
}
