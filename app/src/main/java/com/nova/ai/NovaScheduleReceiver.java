package com.nova.ai;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Receives OS scheduler events without executing arbitrary payloads. */
public final class NovaScheduleReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        String task = intent == null ? "" : intent.getStringExtra("taskId");
        if (task == null || task.trim().isEmpty()) return;
        try { NovaRuntime.get(context); } catch (Exception ignored) { }
    }
}
