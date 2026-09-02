package com.nova.ai;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.View;

/** Lightweight futuristic NOVA HUD rendered directly in the system overlay. */
public final class NovaOverlayView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private String state = "NOVA ONLINE";
    private String detail = "Listening...";
    private float pulse;

    public NovaOverlayView(Context context) {
        super(context);
        paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    public void setState(String state, String detail) {
        this.state = state == null ? "NOVA" : state;
        this.detail = detail == null ? "" : detail;
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        float cx = w / 2f;
        float cy = h / 2f;
        pulse += 0.08f;
        float r = 105f + (float)Math.sin(pulse) * 7f;

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3f);
        paint.setShadowLayer(22f, 0, 0, 0xAA8DEBFF);
        paint.setColor(0xCC8DEBFF);
        canvas.drawCircle(cx, cy - 25, r, paint);
        paint.setStrokeWidth(1.5f);
        paint.setColor(0x668DEBFF);
        canvas.drawCircle(cx, cy - 25, r + 18, paint);
        canvas.drawCircle(cx, cy - 25, r - 18, paint);
        paint.clearShadowLayer();

        paint.setStyle(Paint.Style.FILL);
        paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(25f);
        paint.setColor(0xFF8DEBFF);
        canvas.drawText(state, cx, cy - 20, paint);
        paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
        paint.setTextSize(15f);
        paint.setColor(0xFFE8F8FF);
        canvas.drawText(detail, cx, cy + 12, paint);

        if (getVisibility() == VISIBLE) postInvalidateDelayed(40);
    }
}
