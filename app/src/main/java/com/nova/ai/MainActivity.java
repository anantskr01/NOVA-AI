package com.nova.ai;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONException;

/** Initial NOVA control surface: conversation entry plus runtime status. */
public final class MainActivity extends Activity {
    private TextView output;
    private NovaAgent agent;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        agent = new NovaAgent(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 48, 32, 32);

        TextView title = new TextView(this);
        title.setText("NOVA AI"); title.setTextSize(30); title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));

        EditText input = new EditText(this);
        input.setHint("Ask NOVA...");
        root.addView(input, new LinearLayout.LayoutParams(-1, -2));

        Button send = new Button(this);
        send.setText("SEND");
        root.addView(send, new LinearLayout.LayoutParams(-1, -2));

        output = new TextView(this);
        output.setText("NOVA ready.\nAI provider: not configured\nGateway: standby");
        output.setTextSize(16);
        ScrollView scroll = new ScrollView(this); scroll.addView(output);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        send.setOnClickListener(v -> {
            String text = input.getText().toString().trim();
            if (text.isEmpty()) return;
            output.setText("Thinking...\n");
            agent.handle(text, result -> runOnUiThread(() -> {
                try { output.setText(result.toString(2)); }
                catch (JSONException e) { output.setText(result.toString()); }
            }));
        });
        setContentView(root);
    }
}
