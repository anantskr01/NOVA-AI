package com.nova.ai;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/** Persistent, OS-managed one-shot and repeating task scheduler. */
public final class NovaScheduler {
    private static final String ACTION = "com.nova.ai.SCHEDULED_TASK";
    private final Context context; private final AlarmManager alarms;
    public NovaScheduler(Context context){this.context=context.getApplicationContext();alarms=(AlarmManager)this.context.getSystemService(Context.ALARM_SERVICE);}
    public void schedule(String taskId,long triggerAtMillis){Intent i=new Intent(context,NovaScheduleReceiver.class).setAction(ACTION).putExtra("taskId",taskId);PendingIntent p=pending(i,taskId.hashCode());alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,triggerAtMillis,p);}
    public void repeat(String taskId,long firstAtMillis,long intervalMillis){if(intervalMillis<60000)throw new IllegalArgumentException("interval_too_short");Intent i=new Intent(context,NovaScheduleReceiver.class).setAction(ACTION).putExtra("taskId",taskId);PendingIntent p=pending(i,taskId.hashCode());alarms.setRepeating(AlarmManager.RTC_WAKEUP,firstAtMillis,intervalMillis,p);}
    public void cancel(String taskId){Intent i=new Intent(context,NovaScheduleReceiver.class).setAction(ACTION);alarms.cancel(pending(i,taskId.hashCode()));}
    private PendingIntent pending(Intent i,int id){int flags=PendingIntent.FLAG_UPDATE_CURRENT|(Build.VERSION.SDK_INT>=23?PendingIntent.FLAG_IMMUTABLE:0);return PendingIntent.getBroadcast(context,id,i,flags);}
}
