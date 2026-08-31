package com.nova.ai;

import android.graphics.Bitmap;
import org.json.JSONObject;

/** Vision boundary for camera, screen-context and MediaPipe adapters. */
public interface NovaVisionEngine {
    void analyze(Bitmap frame, Callback callback);
    interface Callback { void onResult(JSONObject result); void onError(Exception error); }
}
