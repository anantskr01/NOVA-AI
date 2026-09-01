package com.nova.ai;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Structured local memory facade: bounded conversation context plus searchable long-term facts. */
public final class NovaMemoryManager {
    private static final int MAX_FACTS = 100;
    private final NovaLongTermMemory store;
    private final NovaContextStore context;

    public NovaMemoryManager(Context context) {
        Context app = context.getApplicationContext();
        store = new NovaLongTermMemory(app);
        this.context = new NovaContextStore(app);
    }

    public void addConversation(String role, String text) { context.add(role, text); }
    public JSONArray recentConversation() { return context.recent(); }
    public void clearConversation() { context.clear(); }

    public synchronized boolean remember(String key, String value) {
        if (!valid(key, value) || store.export().length() >= MAX_FACTS && store.get(key) == null) return false;
        return store.put(normalize(key), value.trim());
    }

    public synchronized boolean forget(String key) { return store.remove(normalize(key)); }
    public synchronized String recall(String key) { return store.get(normalize(key)); }
    public synchronized JSONArray allFacts() { return store.export(); }

    /** Simple deterministic relevance search that avoids sending unrelated memories to a model. */
    public synchronized JSONArray search(String query, int limit) {
        JSONArray result = new JSONArray();
        if (query == null || query.trim().isEmpty()) return result;
        String[] terms = query.toLowerCase(Locale.ROOT).split("\\s+");
        List<Scored> scored = new ArrayList<>();
        JSONArray facts = store.export();
        for (int i = 0; i < facts.length(); i++) {
            JSONObject fact = facts.optJSONObject(i);
            if (fact == null) continue;
            String text = (fact.optString("key", "") + " " + fact.optString("value", "")).toLowerCase(Locale.ROOT);
            int score = 0;
            for (String term : terms) if (term.length() > 1 && text.contains(term)) score++;
            if (score > 0) scored.add(new Scored(fact, score));
        }
        scored.sort((a, b) -> Integer.compare(b.score, a.score));
        int max = Math.max(1, Math.min(limit <= 0 ? 5 : limit, scored.size()));
        for (int i = 0; i < max; i++) result.put(scored.get(i).fact);
        return result;
    }

    private static boolean valid(String key, String value) { return key != null && !key.trim().isEmpty() && value != null && !value.trim().isEmpty(); }
    private static String normalize(String key) { return key == null ? "" : key.trim().toLowerCase(Locale.ROOT); }
    private static final class Scored { final JSONObject fact; final int score; Scored(JSONObject f, int s) { fact = f; score = s; } }
}
