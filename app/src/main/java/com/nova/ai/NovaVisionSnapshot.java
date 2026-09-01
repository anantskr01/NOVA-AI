package com.nova.ai;

import android.graphics.Bitmap;
import android.os.SystemClock;
import org.json.JSONObject;

/** Lightweight vision snapshot metadata; pixels are retained only for the caller's operation. */
public final class NovaVisionSnapshot {
    public final int width;
    public final int height;
    public final long capturedAt;
    public final Bitmap bitmap;
    public final String source;

    public NovaVisionSnapshot(Bitmap bitmap, String source) {
        this.bitmap = bitmap;
        this.width = bitmap == null ? 0 : bitmap.getWidth();
        this.height = bitmap == null ? 0 : bitmap.getHeight();
        this.capturedAt = SystemClock.elapsedRealtime();
        this.source = source == null ? "unknown" : source;
    }
    public JSONObject metadata() {
        JSONObject out = new JSONObject();
        try { out.put("width", width).put("height", height).put("capturedAt", capturedAt).put("source", source); }
        catch (Exception ignored) { }
        return out;
    }
}
