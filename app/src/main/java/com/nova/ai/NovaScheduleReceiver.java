package com.nova.ai;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/** Receives OS scheduler events and hands the saved prompt to the bounded background executor. */
public final class NovaScheduleReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        String task=intent==null?"":intent.getStringExtra("taskId"); if(task==null||task.trim().isEmpty())return;
        Intent work=new Intent(context,NovaBackgroundService.class).putExtra(NovaBackgroundService.EXTRA_TASK_ID,task);
        try { if(Build.VERSION.SDK_INT>=26)context.startForegroundService(work);else context.startService(work); }
        catch(Exception ignored) { }
    }
}
