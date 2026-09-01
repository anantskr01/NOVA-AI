package com.nova.ai;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import java.util.ArrayList;
import java.util.Locale;

/** Lifecycle-safe voice bridge: speech input -> NOVA -> spoken response. */
public final class NovaVoiceController implements TextToSpeech.OnInitListener {
    public interface Listener { void onState(String state); void onText(String text); void onError(String error); }
    private final Context context; private final NovaAgent agent; private final Listener listener; private final Handler main = new Handler(Looper.getMainLooper());
    private SpeechRecognizer recognizer; private TextToSpeech tts; private boolean ttsReady; private boolean listening; private boolean destroyed;
    public NovaVoiceController(Context context, NovaAgent agent, Listener listener) { this.context=context.getApplicationContext(); this.agent=agent; this.listener=listener; tts=new TextToSpeech(this.context,this); }
    public boolean isListening(){return listening;}
    public boolean hasAudioPermission(){return context.checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED;}
    public void startListening(){
        if(destroyed)return; if(!SpeechRecognizer.isRecognitionAvailable(context)){error("speech_unavailable");return;} if(!hasAudioPermission()){error("microphone_permission_required");return;}
        stopListening(); recognizer=SpeechRecognizer.createSpeechRecognizer(context); recognizer.setRecognitionListener(new RecognitionListener(){
            public void onReadyForSpeech(Bundle b){state("LISTENING");} public void onBeginningOfSpeech(){state("LISTENING");} public void onRmsChanged(float r){} public void onBufferReceived(byte[] b){}
            public void onEndOfSpeech(){listening=false;state("PROCESSING");} public void onPartialResults(Bundle b){} public void onEvent(int a,Bundle b){}
            public void onError(int e){listening=false; state("IDLE"); error("speech_error_"+e);}
            public void onResults(Bundle b){listening=false; String text=""; if(b!=null){ArrayList<String> values=b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION); if(values!=null&&!values.isEmpty())text=values.get(0).trim();} if(text.isEmpty()){state("IDLE");return;} if(listener!=null)listener.onText(text); agent.handle(text,result->{String response=result.optString("text",result.optString("message","")); if(response.isEmpty())response=result.optString("error","NOVA could not complete that request."); speak(response);});}
        });
        Intent intent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH); intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM); intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,Locale.getDefault()); intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,false); intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,1);
        listening=true; recognizer.startListening(intent);
    }
    public void stopListening(){if(recognizer!=null){try{recognizer.cancel();recognizer.destroy();}catch(Exception ignored){}recognizer=null;}listening=false;state("IDLE");}
    public void speak(String text){if(destroyed||!ttsReady||text==null||text.trim().isEmpty())return; state("SPEAKING"); tts.speak(text.trim(),TextToSpeech.QUEUE_FLUSH,null,"nova-response");}
    public void stopSpeaking(){if(tts!=null){try{tts.stop();}catch(Exception ignored){}}state("IDLE");}
    @Override public void onInit(int status){ttsReady=status==TextToSpeech.SUCCESS;if(ttsReady)tts.setLanguage(Locale.getDefault());else error("tts_unavailable");}
    public void destroy(){destroyed=true;stopListening();if(tts!=null){try{tts.stop();tts.shutdown();}catch(Exception ignored){}tts=null;}}
    private void state(String s){main.post(()->{if(listener!=null)listener.onState(s);});} private void error(String e){main.post(()->{if(listener!=null)listener.onError(e);});}
}
