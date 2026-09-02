package com.nova.ai;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

/** Executes scheduled NOVA prompts in a bounded foreground service. */
public final class NovaBackgroundService extends Service {
    public static final String EXTRA_TASK_ID="taskId"; private static final int NOTIFICATION_ID=7301; private static final String CHANNEL="nova_background";
    @Override public void onCreate(){super.onCreate();createChannel();}
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        String taskId=intent==null?"":intent.getStringExtra(EXTRA_TASK_ID);startForeground(NOTIFICATION_ID,notification("NOVA is running a scheduled task"));
        if(taskId==null||taskId.trim().isEmpty()){stopSelf(startId);return START_NOT_STICKY;}
        final NovaScheduledTaskStore store=new NovaScheduledTaskStore(this);final org.json.JSONObject task=store.get(taskId);
        if(task==null){stopSelf(startId);return START_NOT_STICKY;}
        String prompt=task.optString("prompt","");long repeat=task.optLong("repeatEveryMs",0);boolean execute=task.optBoolean("execute",true);
        if(prompt.isEmpty()){store.remove(taskId);stopSelf(startId);return START_NOT_STICKY;}
        if(!execute){notifyResult("Reminder: "+prompt);if(repeat==0)store.remove(taskId);stopSelf(startId);return START_NOT_STICKY;}
        NovaAgent agent=new NovaAgent(this);agent.handle(prompt,result->{if(repeat==0)store.remove(taskId);String text=result.optString("text",result.optString("error","Scheduled task finished."));notifyResult(text);stopSelf(startId);});
        return START_NOT_STICKY;
    }
    private void notifyResult(String text){NotificationManager manager=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);if(manager!=null)manager.notify(NOTIFICATION_ID,notification("NOVA: "+bounded(text,180)));}
    @Override public IBinder onBind(Intent intent){return null;}
    private Notification notification(String text){if(Build.VERSION.SDK_INT>=26){NotificationChannel channel=new NotificationChannel(CHANNEL,"NOVA background tasks",NotificationManager.IMPORTANCE_LOW);NotificationManager manager=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);if(manager!=null)manager.createNotificationChannel(channel);}Notification.Builder builder=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL):new Notification.Builder(this);return builder.setSmallIcon(android.R.drawable.ic_popup_sync).setContentTitle("NOVA").setContentText(text).setOngoing(false).build();}
    private static String bounded(String value,int max){if(value==null)return"";return value.length()>max?value.substring(0,max):value;}
}
