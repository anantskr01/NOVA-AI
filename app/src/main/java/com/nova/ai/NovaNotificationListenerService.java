package com.nova.ai;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

/** Notification intelligence boundary. Stores only compact metadata unless explicitly enabled later. */
public final class NovaNotificationListenerService extends NotificationListenerService {
    @Override public void onNotificationPosted(StatusBarNotification sbn) { if (sbn == null) return; NovaNotificationStore.record(getApplicationContext(), sbn.getPackageName(), sbn.getNotification().getSmallIcon() != null); }
    @Override public void onNotificationRemoved(StatusBarNotification sbn) { }
}
