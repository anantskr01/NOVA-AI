package com.nova.ai;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;

/** Screen-capture permission/controller. Actual capture starts only after Android grants projection consent. */
public final class NovaScreenCaptureController {
    public interface Callback { void onPermissionRequired(Intent intent); void onReady(MediaProjection projection); void onError(Exception error); }
    public static final int REQUEST_CODE = 4101;
    private final Context context;
    private final MediaProjectionManager manager;

    public NovaScreenCaptureController(Context context) {
        this.context = context.getApplicationContext();
        manager = (MediaProjectionManager) this.context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
    }

    public void requestPermission(Activity activity, Callback callback) {
        if (manager == null) { callback.onError(new IllegalStateException("MediaProjection unavailable")); return; }
        callback.onPermissionRequired(manager.createScreenCaptureIntent());
    }

    public void onPermissionResult(int resultCode, Intent data, Callback callback) {
        if (resultCode != Activity.RESULT_OK || data == null) { callback.onError(new SecurityException("Screen capture permission denied")); return; }
        try {
            MediaProjection projection;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) projection = manager.getMediaProjection(resultCode, data);
            else projection = manager.getMediaProjection(resultCode, data);
            if (projection == null) throw new IllegalStateException("MediaProjection unavailable");
            callback.onReady(projection);
        } catch (Exception e) { callback.onError(e); }
    }
}
