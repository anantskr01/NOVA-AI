package com.nova.ai;

/** Voice boundary: Android speech recognition/TTS implementations plug in here. */
public interface NovaVoiceEngine {
    void startListening(Listener listener);
    void stopListening();
    void speak(String text);
    interface Listener { void onText(String text); void onError(Exception error); }
}
