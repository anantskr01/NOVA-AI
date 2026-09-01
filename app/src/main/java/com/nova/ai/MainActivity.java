package com.nova.ai;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONException;

/** NOVA control surface with text and lifecycle-safe voice interaction. */
public final class MainActivity extends Activity {
    private static final int MIC_REQUEST = 4101;
    private TextView output; private NovaAgent agent; private NovaVoiceController voice;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state); agent = new NovaAgent(this);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(32,48,32,32);
        TextView title = new TextView(this); title.setText("NOVA AI"); title.setTextSize(30); title.setGravity(Gravity.CENTER); root.addView(title,new LinearLayout.LayoutParams(-1,-2));
        EditText input = new EditText(this); input.setHint("Ask NOVA..."); root.addView(input,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout buttons=new LinearLayout(this); buttons.setOrientation(LinearLayout.HORIZONTAL);
        Button send = new Button(this); send.setText("SEND"); buttons.addView(send,new LinearLayout.LayoutParams(0,-2,1));
        Button mic = new Button(this); mic.setText("MIC"); buttons.addView(mic,new LinearLayout.LayoutParams(0,-2,1)); root.addView(buttons);
        output = new TextView(this); output.setText("NOVA ready.\nAI provider: not configured\nGateway: standby"); output.setTextSize(16);
        ScrollView scroll = new ScrollView(this); scroll.addView(output); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        voice = new NovaVoiceController(this, agent, new NovaVoiceController.Listener(){
            public void onState(String s){runOnUiThread(()->mic.setText(s.equals("LISTENING")?"STOP":"MIC"));}
            public void onText(String text){runOnUiThread(()->{input.setText(text);output.setText("Processing voice command...\n"+text);});}
            public void onError(String error){runOnUiThread(()->output.setText("Voice: "+error));}
        });
        send.setOnClickListener(v -> { String text=input.getText().toString().trim(); if(text.isEmpty()) return; output.setText("Thinking...\n"); agent.handle(text,result -> runOnUiThread(() -> { try { output.setText(result.toString(2)); } catch (JSONException e) { output.setText(result.toString()); } })); });
        mic.setOnClickListener(v -> { if(voice.isListening()){voice.stopListening();return;} if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},MIC_REQUEST);return;} voice.startListening(); });
        setContentView(root);
    }
    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){super.onRequestPermissionsResult(requestCode,permissions,grantResults);if(requestCode==MIC_REQUEST&&grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED&&voice!=null)voice.startListening();}
    @Override protected void onDestroy(){if(voice!=null)voice.destroy();super.onDestroy();}
}
