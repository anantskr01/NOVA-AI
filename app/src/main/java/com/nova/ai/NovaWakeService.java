package com.nova.ai;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.WindowManager;
import java.util.ArrayList;
import java.util.Locale;

/** Hands-free NOVA listener. It keeps a microphone foreground service alive and shows the HUD after "Hey NOVA". */
public final class NovaWakeService extends Service implements TextToSpeech.OnInitListener {
    public static final String ACTION_START = "com.nova.ai.START_WAKE";
    public static final String ACTION_STOP = "com.nova.ai.STOP_WAKE";
    private static final String CHANNEL = "nova_voice";
    private static final int NOTIFICATION_ID = 9001;

    private final Handler main = new Handler(Looper.getMainLooper());
    private SpeechRecognizer recognizer;
    private TextToSpeech tts;
    private NovaAgent agent;
    private WindowManager windowManager;
    private NovaOverlayView overlay;
    private boolean running;
    private boolean ttsReady;

    @Override public void onCreate() {
        super.onCreate();
        agent = new NovaAgent(this);
        tts = new TextToSpeech(this, this);
        createChannel();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) { stopSelf(); return START_NOT_STICKY; }
        if (Build.VERSION.SDK_INT >= 29) startForeground(NOTIFICATION_ID, notification(), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
        else startForeground(NOTIFICATION_ID, notification());
        if (!hasMicPermission()) { stopSelf(); return START_NOT_STICKY; }
        running = true;
        startRecognizer();
        return START_STICKY;
    }

    private void startRecognizer() {
        main.post(() -> {
            if (!running || !SpeechRecognizer.isRecognitionAvailable(this)) return;
            destroyRecognizer();
            recognizer = SpeechRecognizer.createSpeechRecognizer(this);
            recognizer.setRecognitionListener(new RecognitionListener() {
                public void onReadyForSpeech(android.os.Bundle b) { }
                public void onBeginningOfSpeech() { }
                public void onRmsChanged(float rms) { }
                public void onBufferReceived(byte[] b) { }
                public void onEndOfSpeech() { }
                public void onPartialResults(android.os.Bundle b) { }
                public void onEvent(int type, android.os.Bundle b) { }
                public void onError(int error) { scheduleRestart(350); }
                public void onResults(android.os.Bundle b) {
                    String heard = "";
                    if (b != null) {
                        ArrayList<String> values = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                        if (values != null && !values.isEmpty()) heard = values.get(0).trim();
                    }
                    handleHeard(heard);
                    scheduleRestart(250);
                }
            });
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
            recognizer.startListening(intent);
        });
    }

    private void handleHeard(String heard) {
        if (heard == null) return;
        String text = heard.trim();
        String lower = text.toLowerCase(Locale.ROOT);
        int wake = lower.indexOf("hey nova");
        if (wake < 0) return;
        String command = text.substring(wake + 8).trim();
        showHud("NOVA ONLINE", command.isEmpty() ? "Listening..." : command);
        if (command.isEmpty()) {
            speak("I'm listening.");
            return;
        }
        agent.handle(command, result -> main.post(() -> {
            String response = result.optString("text", "");
            if (response.isEmpty()) response = result.optString("error", "I couldn't complete that request.");
            showHud("NOVA", response);
            speak(response);
            main.postDelayed(this::hideHud, 3200);
        }));
    }

    private void showHud(String state, String detail) {
        if (!Settings.canDrawOverlays(this)) return;
        main.post(() -> {
            if (windowManager == null) windowManager = (WindowManager)getSystemService(WINDOW_SERVICE);
            if (overlay == null) {
                overlay = new NovaOverlayView(this);
                int type = Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
                WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.MATCH_PARENT, 330, type,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                        PixelFormat.TRANSLUCENT);
                p.gravity = Gravity.CENTER;
                try { windowManager.addView(overlay, p); } catch (Exception ignored) { overlay = null; return; }
            }
            overlay.setVisibility(android.view.View.VISIBLE);
            overlay.setState(state, detail);
        });
    }

    private void hideHud() { main.post(() -> { if (overlay != null) overlay.setVisibility(android.view.View.GONE); }); }

    private void speak(String text) {
        if (ttsReady && tts != null && text != null && !text.trim().isEmpty())
            tts.speak(text.trim(), TextToSpeech.QUEUE_FLUSH, null, "nova");
    }

    private void scheduleRestart(long delay) { if (running) main.postDelayed(this::startRecognizer, delay); }
    private boolean hasMicPermission() { return checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED; }

    private Notification notification() {
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, CHANNEL) : new Notification.Builder(this);
        return b.setContentTitle("NOVA is listening").setContentText("Say Hey NOVA to wake the HUD").setSmallIcon(android.R.drawable.ic_btn_speak_now).setOngoing(true).build();
    }
    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel c = new NotificationChannel(CHANNEL, "NOVA Voice", NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(c);
        }
    }
    private void destroyRecognizer() { if (recognizer != null) { try { recognizer.cancel(); recognizer.destroy(); } catch (Exception ignored) { } recognizer = null; } }

    @Override public void onInit(int status) { ttsReady = status == TextToSpeech.SUCCESS; if (ttsReady) tts.setLanguage(Locale.getDefault()); }
    @Override public void onDestroy() {
        running = false; destroyRecognizer(); hideHud();
        if (tts != null) { try { tts.stop(); tts.shutdown(); } catch (Exception ignored) { } tts = null; }
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
