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

/** Hands-free NOVA listener with self-healing speech-recognition sessions. */
public final class NovaWakeService extends Service implements TextToSpeech.OnInitListener {
    public static final String ACTION_START = "com.nova.ai.START_WAKE";
    public static final String ACTION_STOP = "com.nova.ai.STOP_WAKE";
    private static final String CHANNEL = "nova_voice";
    private static final int NOTIFICATION_ID = 9001;

    private static final long NORMAL_RESTART_MS = 350L;
    private static final long RESULT_RESTART_MS = 200L;
    private static final long BUSY_RESTART_MS = 1000L;
    private static final long DUPLICATE_WINDOW_MS = 1200L;

    private final Handler main = new Handler(Looper.getMainLooper());
    private SpeechRecognizer recognizer;
    private TextToSpeech tts;
    private NovaAgent agent;
    private WindowManager windowManager;
    private NovaOverlayView overlay;
    private boolean running;
    private boolean starting;
    private boolean restartScheduled;
    private boolean ttsReady;
    private long lastHeardAt;
    private String lastHeard = "";

    @Override public void onCreate() {
        super.onCreate();
        agent = new NovaAgent(this);
        tts = new TextToSpeech(this, this);
        createChannel();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            running = false;
            main.removeCallbacksAndMessages(null);
            stopSelf();
            return START_NOT_STICKY;
        }

        if (!hasMicPermission()) {
            stopSelf();
            return START_NOT_STICKY;
        }

        try {
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(NOTIFICATION_ID, notification("Starting hands-free listening"),
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
            } else {
                startForeground(NOTIFICATION_ID, notification("Starting hands-free listening"));
            }
        } catch (Exception e) {
            stopSelf();
            return START_NOT_STICKY;
        }

        if (!running) {
            running = true;
            scheduleRestart(250L);
        }
        return START_STICKY;
    }

    private void startRecognizer() {
        if (!running || starting) return;
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            updateNotification("Speech recognition unavailable");
            scheduleRestart(2000L);
            return;
        }

        starting = true;
        restartScheduled = false;
        destroyRecognizer();

        try {
            recognizer = SpeechRecognizer.createSpeechRecognizer(this);
            recognizer.setRecognitionListener(new RecognitionListener() {
                @Override public void onReadyForSpeech(android.os.Bundle b) {
                    starting = false;
                    updateNotification("NOVA • Hands-Free listening");
                }

                @Override public void onBeginningOfSpeech() {
                    updateNotification("NOVA • hearing");
                }

                @Override public void onRmsChanged(float rms) { }
                @Override public void onBufferReceived(byte[] b) { }
                @Override public void onEndOfSpeech() { }

                @Override public void onError(int error) {
                    starting = false;
                    if (!running) return;
                    updateNotification("NOVA • speech reconnecting");
                    scheduleRestart(error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY
                            ? BUSY_RESTART_MS : NORMAL_RESTART_MS);
                }

                @Override public void onResults(android.os.Bundle b) {
                    starting = false;
                    if (running) handleHeard(extractText(b));
                    scheduleRestart(RESULT_RESTART_MS);
                }

                @Override public void onPartialResults(android.os.Bundle b) {
                    String heard = extractText(b);
                    if (heard.isEmpty()) return;
                    handleHeard(heard);
                }

                @Override public void onEvent(int type, android.os.Bundle b) { }
            });

            Intent input = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            input.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            input.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            input.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
            input.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            input.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
            input.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false);

            recognizer.startListening(input);
            starting = false;
            updateNotification("NOVA • Hands-Free listening");
        } catch (Exception e) {
            starting = false;
            destroyRecognizer();
            scheduleRestart(NORMAL_RESTART_MS);
        }
    }

    private String extractText(android.os.Bundle bundle) {
        if (bundle == null) return "";
        ArrayList<String> values = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (values == null || values.isEmpty() || values.get(0) == null) return "";
        return values.get(0).trim();
    }

    private void handleHeard(String heard) {
        if (heard == null) return;
        String text = heard.trim();
        if (text.isEmpty()) return;

        long now = System.currentTimeMillis();
        if (text.equalsIgnoreCase(lastHeard) && now - lastHeardAt < DUPLICATE_WINDOW_MS) return;
        lastHeard = text;
        lastHeardAt = now;

        String lower = text.toLowerCase(Locale.ROOT);
        int wakeIndex = lower.indexOf("hey nova");
        int wakeLength = 8;
        if (wakeIndex < 0) {
            if (lower.equals("nova")) {
                wakeIndex = 0;
                wakeLength = 4;
            } else if (lower.startsWith("nova ")) {
                wakeIndex = 0;
                wakeLength = 4;
            }
        }
        if (wakeIndex < 0) return;

        String command = text.substring(wakeIndex + wakeLength).trim();
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
            main.postDelayed(this::hideHud, 3200L);
        }));
    }

    private void scheduleRestart(long delay) {
        if (!running || restartScheduled) return;
        restartScheduled = true;
        main.postDelayed(() -> {
            restartScheduled = false;
            if (running) startRecognizer();
        }, delay);
    }

    private void showHud(String state, String detail) {
        if (!Settings.canDrawOverlays(this)) return;
        main.post(() -> {
            if (windowManager == null) windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            if (overlay == null) {
                overlay = new NovaOverlayView(this);
                int type = Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
                WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.MATCH_PARENT, 330, type,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                        PixelFormat.TRANSLUCENT);
                p.gravity = Gravity.CENTER;
                try { windowManager.addView(overlay, p); }
                catch (Exception ignored) { overlay = null; return; }
            }
            overlay.setVisibility(android.view.View.VISIBLE);
            overlay.setState(state, detail);
        });
    }

    private void hideHud() {
        main.post(() -> { if (overlay != null) overlay.setVisibility(android.view.View.GONE); });
    }

    private void speak(String text) {
        if (!ttsReady || tts == null || text == null || text.trim().isEmpty()) return;
        tts.speak(text.trim(), TextToSpeech.QUEUE_FLUSH, null, "nova-response");
    }

    private boolean hasMicPermission() {
        return checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private Notification notification(String text) {
        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL)
                : new Notification.Builder(this);
        return b.setContentTitle("NOVA is listening")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setOngoing(true)
                .build();
    }

    private void updateNotification(String text) {
        if (!running) return;
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) manager.notify(NOTIFICATION_ID, notification(text));
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel c = new NotificationChannel(CHANNEL, "NOVA Voice", NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(c);
        }
    }

    private void destroyRecognizer() {
        if (recognizer != null) {
            try { recognizer.cancel(); } catch (Exception ignored) { }
            try { recognizer.destroy(); } catch (Exception ignored) { }
            recognizer = null;
        }
    }

    @Override public void onInit(int status) {
        ttsReady = status == TextToSpeech.SUCCESS;
        if (ttsReady && tts != null) tts.setLanguage(Locale.getDefault());
    }

    @Override public void onDestroy() {
        running = false;
        main.removeCallbacksAndMessages(null);
        destroyRecognizer();
        hideHud();
        if (tts != null) {
            try { tts.stop(); tts.shutdown(); } catch (Exception ignored) { }
            tts = null;
        }
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
