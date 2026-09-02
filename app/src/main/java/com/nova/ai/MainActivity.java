package com.nova.ai;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** NOVA control surface: AI provider, agent tasks, wake HUD and device connectivity. */
public final class MainActivity extends Activity {
    private static final int MIC_REQUEST=4101;
    private NovaAgent agent; private NovaVoiceController voice; private TextView output; private Button wake; private Button cancel;

    @Override public void onCreate(Bundle state){super.onCreate(state);agent=new NovaAgent(this);buildUi();}

    private void buildUi(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(32,44,32,28);root.setBackgroundColor(Color.rgb(5,10,16));
        TextView title=text("N O V A",30,Color.rgb(141,235,255));title.setGravity(Gravity.CENTER);root.addView(title,new LinearLayout.LayoutParams(-1,58));
        TextView sub=text("PERSONAL ANDROID AGENT",12,Color.LTGRAY);sub.setGravity(Gravity.CENTER);root.addView(sub,new LinearLayout.LayoutParams(-1,30));

        EditText input=new EditText(this);input.setHint("Ask NOVA anything...");input.setHintTextColor(Color.GRAY);input.setTextColor(Color.WHITE);input.setSingleLine(false);input.setPadding(24,18,24,18);GradientDrawable box=new GradientDrawable();box.setColor(Color.rgb(12,22,31));box.setCornerRadius(28);box.setStroke(1,Color.rgb(45,90,105));input.setBackground(box);root.addView(input,new LinearLayout.LayoutParams(-1,120));
        LinearLayout buttons=new LinearLayout(this);buttons.setOrientation(LinearLayout.HORIZONTAL);buttons.setPadding(0,18,0,10);Button send=button("SEND");Button mic=button("MIC");buttons.addView(send,new LinearLayout.LayoutParams(0,58,1));buttons.addView(mic,new LinearLayout.LayoutParams(0,58,1));root.addView(buttons);

        LinearLayout taskButtons=new LinearLayout(this);taskButtons.setOrientation(LinearLayout.HORIZONTAL);cancel=button("CANCEL TASK");Button provider=button("AI PROVIDER");taskButtons.addView(cancel,new LinearLayout.LayoutParams(0,54,1));taskButtons.addView(provider,new LinearLayout.LayoutParams(0,54,1));root.addView(taskButtons);
        wake=button("ENABLE HEY NOVA");root.addView(wake,new LinearLayout.LayoutParams(-1,58));
        Button overlayPermission=button("ALLOW HUD OVER OTHER APPS");root.addView(overlayPermission,new LinearLayout.LayoutParams(-1,58));
        Button accessibility=button("OPEN ACCESSIBILITY SETTINGS");root.addView(accessibility,new LinearLayout.LayoutParams(-1,58));
        Button device=button("CONNECT PC / OTHER NOVA NODE");root.addView(device,new LinearLayout.LayoutParams(-1,58));

        output=text(statusText(),15,Color.WHITE);output.setPadding(8,22,8,8);ScrollView scroll=new ScrollView(this);scroll.addView(output);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));

        voice=new NovaVoiceController(this,agent,new NovaVoiceController.Listener(){public void onState(String s){runOnUiThread(()->mic.setText(s.equals("LISTENING")?"STOP":"MIC"));}public void onText(String t){runOnUiThread(()->output.setText("Heard:\n"+t));}public void onError(String e){runOnUiThread(()->output.setText("Voice: "+e));}});
        send.setOnClickListener(v->{String t=input.getText().toString().trim();if(t.isEmpty())return;runRequest(t);});
        mic.setOnClickListener(v->{if(voice.isListening()){voice.stopListening();return;}if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},MIC_REQUEST);return;}voice.startListening();});
        cancel.setOnClickListener(v->{agent.cancelCurrentTask();output.setText("Cancellation requested for the active task.");});
        provider.setOnClickListener(v->showProviderDialog());
        overlayPermission.setOnClickListener(v->{if(Build.VERSION.SDK_INT>=23&&!Settings.canDrawOverlays(this))startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName())));else output.setText("HUD permission is already enabled.");});
        accessibility.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        wake.setOnClickListener(v->toggleWake());
        device.setOnClickListener(v->showGatewayDialog());
        setContentView(root);
    }

    private void runRequest(String t){output.setText("Thinking...\n"+t);agent.handle(t,result->runOnUiThread(()->{String response=result.optString("text",result.optString("error","NOVA couldn't complete that request."));output.setText(response+"\n\nStatus: "+result.optString("status",result.optString("mode","unknown")));}));}

    private void showProviderDialog(){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(28,10,28,0);
        EditText endpoint=field("Endpoint",agent.providerEndpoint());EditText model=field("Model",agent.providerModel());EditText key=field("API key","");key.setInputType(129);
        box.addView(endpoint);box.addView(model);box.addView(key);
        new AlertDialog.Builder(this).setTitle("NOVA AI Provider").setMessage("Use an OpenAI-compatible chat-completions endpoint. The key is encrypted in Android Keystore and never shown in logs.").setView(box)
                .setPositiveButton("SAVE & CONNECT",(d,w)->{boolean ok=agent.saveProvider(endpoint.getText().toString(),model.getText().toString(),key.getText().toString());output.setText(ok?"AI provider connected. NOVA can now process arbitrary natural-language tasks.":"Provider configuration failed.");})
                .setNegativeButton("CANCEL",null).setNeutralButton("CLEAR KEY",(d,w)->{agent.clearProvider();output.setText("AI provider disconnected. Local commands remain available.");}).show();
    }

    private void showGatewayDialog(){
        EditText url=field("WebSocket URL","ws://192.168.1.10:8765");
        new AlertDialog.Builder(this).setTitle("Connect NOVA node").setMessage("Connect a trusted NOVA PC/Android companion gateway. The remote node must implement NOVA's command protocol.").setView(url)
                .setPositiveButton("CONNECT",(d,w)->{NovaRuntime.get(this).connectGateway(url.getText().toString().trim());output.setText("Connecting to NOVA node...\n"+url.getText());})
                .setNegativeButton("DISCONNECT",(d,w)->{NovaRuntime.get(this).disconnectGateway();output.setText("NOVA node disconnected.");}).show();
    }

    private EditText field(String hint,String value){EditText e=new EditText(this);e.setHint(hint);e.setText(value);e.setTextColor(Color.WHITE);e.setHintTextColor(Color.GRAY);e.setSingleLine(true);e.setPadding(8,10,8,10);return e;}
    private String statusText(){return "NOVA ready.\n\nAI provider: "+(agent.providerConfigured()?"CONNECTED":"NOT CONFIGURED")+"\n• Natural-language agent loop\n• Web research tools\n• Screen/UI observation + OCR\n• Generic Android app interaction\n• Scheduled/background tasks\n• Cross-device gateway\n• Cooperative task cancellation\n\nConnect the AI provider to unlock the full agent brain.";}
    private void toggleWake(){if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},MIC_REQUEST);return;}if(Build.VERSION.SDK_INT>=23&&!Settings.canDrawOverlays(this)){startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName())));output.setText("Allow NOVA to display over other apps, then press ENABLE HEY NOVA again.");return;}Intent service=new Intent(this,NovaWakeService.class);if(Build.VERSION.SDK_INT>=26)startForegroundService(service);else startService(service);wake.setText("HEY NOVA: ONLINE");output.setText("NOVA is listening in the background. Say: Hey NOVA");}
    private Button button(String label){Button b=new Button(this);b.setText(label);b.setTextColor(Color.rgb(141,235,255));return b;}
    private TextView text(String value,float size,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);return t;}
    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){super.onRequestPermissionsResult(requestCode,permissions,grantResults);if(requestCode==MIC_REQUEST&&grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED)toggleWake();}
    @Override protected void onDestroy(){if(voice!=null)voice.destroy();super.onDestroy();}
}
