package com.nova.ai;

import android.content.Context;
import android.os.Build;
import android.os.PowerManager;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** Cross-cutting NOVA tools: research, memory search, notifications, device state and verification. */
public final class NovaFeatureTools {
    private NovaFeatureTools() { }
    public static NovaTool memorySearch(Context context) { return new NovaTool(){
        public String id(){return "memory.search";} public String description(){return "Search NOVA's persistent long-term memories by keywords.";}
        public JSONObject schema(){try{return new JSONObject().put("type","object").put("required",new JSONArray().put("query")).put("properties",new JSONObject().put("query",new JSONObject().put("type","string")).put("max_results",new JSONObject().put("type","integer")));}catch(Exception e){return new JSONObject();}}
        public JSONObject execute(JSONObject input){String q=input==null?"":input.optString("query","").trim();int max=Math.max(1,Math.min(10,input==null?5:input.optInt("max_results",5)));return new JSONObject().put("query",q).put("results",NovaRuntime.get(context).memoryManager().search(q,max));}
    };}
    public static NovaTool notifications(Context context){return new NovaTool(){public String id(){return "notification.recent";}public String description(){return "Return recent notification metadata captured by NOVA.";}public JSONObject schema(){return new JSONObject();}public JSONObject execute(JSONObject input){return new JSONObject().put("notifications",NovaNotificationStore.recent(context));}};}
    public static NovaTool devices(Context context){return new NovaTool(){public String id(){return "device.list";}public String description(){return "List NOVA nodes currently known to this device.";}public JSONObject schema(){return new JSONObject();}public JSONObject execute(JSONObject input){return new JSONObject().put("nodes",NovaRuntime.get(context).devices().list());}};}
    public static NovaTool research(Context context){return new NovaTool(){
        public String id(){return "web.research";} public String description(){return "Run a bounded multi-source web research pass and return source material for synthesis.";}
        public JSONObject schema(){try{return new JSONObject().put("type","object").put("required",new JSONArray().put("query")).put("properties",new JSONObject().put("query",new JSONObject().put("type","string")).put("max_sources",new JSONObject().put("type","integer")));}catch(Exception e){return new JSONObject();}}
        public JSONObject execute(JSONObject input)throws Exception{String q=input==null?"":input.optString("query","").trim();if(q.isEmpty())throw new IllegalArgumentException("query_required");int max=Math.max(2,Math.min(5,input==null?4:input.optInt("max_sources",4)));JSONArray candidates=search(q);JSONArray sources=new JSONArray();for(int i=0;i<candidates.length()&&sources.length()<max;i++){JSONObject c=candidates.optJSONObject(i);if(c==null)continue;String u=c.optString("url","");if(u.isEmpty())continue;try{JSONObject p=fetch(u);sources.put(new JSONObject().put("title",c.optString("title","")).put("url",u).put("snippet",c.optString("snippet","")).put("status",p.optInt("status",0)).put("text",NovaAgentPolicy.bounded(p.optString("text",""),5000)));}catch(Exception ignored){}}return new JSONObject().put("query",q).put("sources",sources).put("instruction","Synthesize the sources, distinguish facts from uncertainty, and mention source URLs in the final answer.");}
        private JSONArray search(String q)throws Exception{HttpURLConnection c=(HttpURLConnection)new URL("https://api.duckduckgo.com/?q="+URLEncoder.encode(q,"UTF-8")+"&format=json&no_html=1&no_redirect=1").openConnection();c.setConnectTimeout(8000);c.setReadTimeout(12000);c.setRequestProperty("User-Agent","NOVA-AI/1.0");int code=c.getResponseCode();String body=read(code>=200&&code<300?c.getInputStream():c.getErrorStream(),16000);c.disconnect();if(code<200||code>=300)throw new IllegalStateException("search_http_"+code);JSONObject d=new JSONObject(body);JSONArray r=new JSONArray();if(!d.optString("AbstractURL","").isEmpty())r.put(new JSONObject().put("title",d.optString("Heading","")).put("url",d.optString("AbstractURL","")).put("snippet",d.optString("AbstractText","")));add(d.optJSONArray("RelatedTopics"),r,8);return r;}
        private JSONObject fetch(String url)throws Exception{URI u=URI.create(url);if(u.getScheme()==null||!(u.getScheme().equalsIgnoreCase("http")||u.getScheme().equalsIgnoreCase("https"))||u.getHost()==null)throw new IllegalArgumentException("valid_http_url_required");HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(8000);c.setReadTimeout(15000);c.setRequestProperty("User-Agent","NOVA-AI/1.0");int code=c.getResponseCode();String body=read(code>=200&&code<300?c.getInputStream():c.getErrorStream(),12000);c.disconnect();if(code<200||code>=300)throw new IllegalStateException("fetch_http_"+code);return new JSONObject().put("status",code).put("text",body.replaceAll("(?is)<script.*?>.*?</script>"," ").replaceAll("(?is)<style.*?>.*?</style>"," ").replaceAll("<[^>]+>"," ").replace("&nbsp;"," ").replace("&amp;","&").replaceAll("\\s+"," ").trim());}
        private void add(JSONArray a,JSONArray out,int max){if(a==null)return;for(int i=0;i<a.length()&&out.length()<max;i++){JSONObject x=a.optJSONObject(i);if(x==null)continue;if(x.has("Topics")){add(x.optJSONArray("Topics"),out,max);continue;}String u=x.optString("FirstURL","");if(!u.isEmpty())out.put(new JSONObject().put("title",x.optString("Text","")).put("url",u).put("snippet",x.optString("Text","")));}}
        private String read(InputStream s,int max)throws Exception{if(s==null)return"";StringBuilder b=new StringBuilder();try(BufferedReader r=new BufferedReader(new InputStreamReader(s,StandardCharsets.UTF_8))){String l;while((l=r.readLine())!=null&&b.length()<max)b.append(l).append('\n');}return b.substring(0,Math.min(max,b.length()));}
    };}
    public static NovaTool systemStatus(Context context){return new NovaTool(){public String id(){return "system.status";}public String description(){return "Return NOVA runtime, Android, provider, accessibility, node and battery-interaction status.";}public JSONObject schema(){return new JSONObject();}public JSONObject execute(JSONObject input){PowerManager pm=(PowerManager)context.getSystemService(Context.POWER_SERVICE);return new JSONObject().put("android",Build.VERSION.SDK_INT).put("model",Build.MODEL).put("manufacturer",Build.MANUFACTURER).put("accessibility",NovaAccessibilityService.getInstance()!=null).put("provider",new NovaProviderConfig(context).isConfigured()).put("nodes",NovaRuntime.get(context).devices().list().length()).put("interactive",pm==null||pm.isInteractive());}};}
}
