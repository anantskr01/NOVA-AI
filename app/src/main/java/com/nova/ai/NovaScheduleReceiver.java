package com.nova.ai;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import org.json.JSONObject;

/** Receives OS scheduler events and hands them to NOVA without executing untrusted payloads. */
public final class NovaScheduleReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent){String task=intent==null?"":intent.getStringExtra("taskId"); if(task==null||task.trim().isEmpty())return; try{NovaRuntime.get(context).orchestrator().enqueue(new JSONObject().put("type","scheduled").put("taskId",task));}catch(Exception ignored){}}
}
