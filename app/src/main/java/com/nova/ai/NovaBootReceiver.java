package com.nova.ai;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

/** Restores user-enabled NOVA background services after reboot without silently enabling the microphone. */
public final class NovaBootReceiver extends BroadcastReceiver {
    private static final String PREFS="nova_background";
    private static final String WAKE="wake_enabled";
    @Override public void onReceive(Context context, Intent intent){
        if(intent==null||!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()))return;
        SharedPreferences p=context.getApplicationContext().getSharedPreferences(PREFS,Context.MODE_PRIVATE);
        if(!p.getBoolean(WAKE,false))return;
        Intent service=new Intent(context,NovaWakeService.class);
        if(Build.VERSION.SDK_INT>=26)context.startForegroundService(service);else context.startService(service);
    }
    public static void setWakeEnabled(Context context,boolean enabled){context.getApplicationContext().getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putBoolean(WAKE,enabled).apply();}
    public static boolean isWakeEnabled(Context context){return context.getApplicationContext().getSharedPreferences(PREFS,Context.MODE_PRIVATE).getBoolean(WAKE,false);}
}
