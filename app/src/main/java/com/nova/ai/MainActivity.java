package com.nova.ai;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONException;

/** NOVA control surface plus the hands-free system wake interface. */
public final class MainActivity extends Activity {
    private static final int MIC_REQUEST = 4101;
    private NovaAgent agent;
    private NovaVoiceController voice;
    private TextView output;
    private Button wake;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        agent = new NovaAgent(this);
        buildUi();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 44, 32, 28);
        root.setBackgroundColor(Color.rgb(5, 10, 16));

        TextView title = text("N O V A", 30, Color.rgb(141,235,255));
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(-1, 58));
        TextView sub = text("PERSONAL ANDROID AGENT", 12, Color.LTGRAY);
        sub.setGravity(Gravity.CENTER);
        root.addView(sub, new LinearLayout.LayoutParams(-1, 30));

        EditText input = new EditText(this);
        input.setHint("Ask NOVA anything...");
        input.setHintTextColor(Color.GRAY);
        input.setTextColor(Color.WHITE);
        input.setSingleLine(false);
        input.setPadding(24, 18, 24, 18);
        GradientDrawable box = new GradientDrawable();
        box.setColor(Color.rgb(12, 22, 31)); box.setCornerRadius(28); box.setStroke(1, Color.rgb(45, 90, 105));
        input.setBackground(box);
        root.addView(input, new LinearLayout.LayoutParams(-1, 120));

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setPadding(0, 18, 0, 10);
        Button send = button("SEND");
        Button mic = button("MIC");
        buttons.addView(send, new LinearLayout.LayoutParams(0, 58, 1));
        buttons.addView(mic, new LinearLayout.LayoutParams(0, 58, 1));
        root.addView(buttons);

        wake = button("ENABLE HEY NOVA");
        root.addView(wake, new LinearLayout.LayoutParams(-1, 58));
        Button overlayPermission = button("ALLOW HUD OVER OTHER APPS");
        root.addView(overlayPermission, new LinearLayout.LayoutParams(-1, 58));
        Button accessibility = button("OPEN ACCESSIBILITY SETTINGS");
        root.addView(accessibility, new LinearLayout.LayoutParams(-1, 58));

        output = text("NOVA ready.\n\n• Local commands are enabled\n• Say Hey NOVA to wake the HUD\n• Enable the HUD permission for the floating interface", 15, Color.WHITE);
        output.setPadding(8, 22, 8, 8);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(output);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        voice = new NovaVoiceController(this, agent, new NovaVoiceController.Listener() {
            public void onState(String s) { runOnUiThread(() -> mic.setText(s.equals("LISTENING") ? "STOP" : "MIC")); }
            public void onText(String text) { runOnUiThread(() -> output.setText("Heard:\n" + text)); }
            public void onError(String error) { runOnUiThread(() -> output.setText("Voice: " + error)); }
        });

        send.setOnClickListener(v -> {
            String text = input.getText().toString().trim();
            if (text.isEmpty()) return;
            output.setText("Thinking...\n" + text);
            agent.handle(text, result -> runOnUiThread(() -> {
                String response = result.optString("text", result.optString("error", "NOVA couldn't complete that request."));
                output.setText(response);
            }));
        });

        mic.setOnClickListener(v -> {
            if (voice.isListening()) { voice.stopListening(); return; }
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, MIC_REQUEST); return;
            }
            voice.startListening();
        });

        overlayPermission.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())));
            } else output.setText("HUD permission is already enabled.");
        });
        accessibility.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        wake.setOnClickListener(v -> toggleWake());
        setContentView(root);
    }

    private void toggleWake() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, MIC_REQUEST); return;
        }
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())));
            output.setText("Allow NOVA to display over other apps, then press ENABLE HEY NOVA again.");
            return;
        }
        Intent service = new Intent(this, NovaWakeService.class);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(service); else startService(service);
        wake.setText("HEY NOVA: ONLINE");
        output.setText("NOVA is listening in the background. Say: Hey NOVA");
    }

    private Button button(String label) { Button b = new Button(this); b.setText(label); b.setTextColor(Color.rgb(141,235,255)); return b; }
    private TextView text(String value, float size, int color) { TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color); return t; }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MIC_REQUEST && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) toggleWake();
    }
    @Override protected void onDestroy() { if (voice != null) voice.destroy(); super.onDestroy(); }
}
