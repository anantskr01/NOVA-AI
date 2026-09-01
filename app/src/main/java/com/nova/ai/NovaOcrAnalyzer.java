package com.nova.ai;

import android.graphics.Bitmap;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

/** On-device OCR. Text stays local unless the caller explicitly forwards it to an AI provider. */
public final class NovaOcrAnalyzer {
    public interface Callback { void onText(String text); void onError(Exception error); }
    private NovaOcrAnalyzer() {}

    public static void recognize(Bitmap bitmap, Callback callback) {
        if (bitmap == null) { callback.onError(new IllegalArgumentException("bitmap is null")); return; }
        try {
            InputImage image = InputImage.fromBitmap(bitmap, 0);
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).process(image)
                    .addOnSuccessListener(result -> callback.onText(flatten(result)))
                    .addOnFailureListener(error -> callback.onError(error instanceof Exception ? (Exception) error : new RuntimeException(error)));
        } catch (Exception e) { callback.onError(e); }
    }

    private static String flatten(Text result) {
        StringBuilder out = new StringBuilder();
        for (Text.TextBlock block : result.getTextBlocks()) {
            if (out.length() > 0) out.append('\n');
            out.append(block.getText());
        }
        return out.toString().trim();
    }
}
