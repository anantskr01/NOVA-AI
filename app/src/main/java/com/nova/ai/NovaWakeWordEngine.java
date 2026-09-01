package com.nova.ai;

import android.content.Context;
import java.util.Locale;

/** Lightweight wake-word gate. Audio recognition is delegated to the existing voice layer. */
public final class NovaWakeWordEngine {
    public interface Listener { void onWakeWord(); }
    private static final String WAKE_PHRASE = "hey nova";
    private final Listener listener;
    private boolean enabled = true;

    public NovaWakeWordEngine(Context context, Listener listener) { this.listener = listener; }

    public synchronized void setEnabled(boolean enabled) { this.enabled = enabled; }
    public synchronized boolean isEnabled() { return enabled; }
    public String wakePhrase() { return WAKE_PHRASE; }

    /** Feed finalized speech text here. Returns true when the wake phrase was detected. */
    public synchronized boolean acceptTranscript(String transcript) {
        if (!enabled || transcript == null) return false;
        String normalized = transcript.toLowerCase(Locale.US).replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
        boolean detected = normalized.equals(WAKE_PHRASE) || normalized.startsWith(WAKE_PHRASE + " ") || normalized.contains(" " + WAKE_PHRASE + " ");
        if (detected && listener != null) listener.onWakeWord();
        return detected;
    }
}
