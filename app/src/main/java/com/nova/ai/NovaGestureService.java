package com.nova.ai;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.util.Range;

import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker;
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult;

import java.util.Arrays;
import java.util.List;

/** Camera -> MediaPipe -> index fingertip -> residual motion -> Android Accessibility. */
public final class NovaGestureService extends Service {
    private static final String TAG = "NOVA-Gesture";
    private static final String CHANNEL = "nova_gesture";
    private static final int NOTIFICATION_ID = 2001;

    // Tuned for the latest NOVA continuous-control design.
    private static final float POSITION_SMOOTHING = 0.70f;
    private static final float RESIDUAL_THRESHOLD = 0.0025f;
    private static final float MOVEMENT_SCALE = 1.20f;
    private static final float MAX_STEP = 0.040f;
    private static final long OUTPUT_INTERVAL_MS = 33L;
    private static final long FRAME_INTERVAL_MS = 33L;

    private CameraManager cameraManager;
    private CameraDevice camera;
    private CameraCaptureSession session;
    private ImageReader reader;
    private HandlerThread cameraThread;
    private Handler cameraHandler;
    private HandLandmarker landmarker;
    private String cameraId;
    private boolean processing;

    private float smoothX = -1f, smoothY = -1f;
    private float pendingX, pendingY;
    private long lastOutput;
    private long lastFrame;

    @Override public void onCreate() {
        super.onCreate();
        cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        createNotificationChannel();
        setupLandmarker();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            stopSelf(); return START_NOT_STICKY;
        }
        startForegroundCompat();
        startCamera();
        return START_STICKY;
    }

    private void setupLandmarker() {
        try {
            BaseOptions base = BaseOptions.builder().setModelAssetPath("hand_landmarker.task").build();
            HandLandmarker.HandLandmarkerOptions options = HandLandmarker.HandLandmarkerOptions.builder()
                    .setBaseOptions(base).setRunningMode(RunningMode.VIDEO).setNumHands(1)
                    .setMinHandDetectionConfidence(0.30f).setMinHandPresenceConfidence(0.30f)
                    .setMinTrackingConfidence(0.30f).build();
            landmarker = HandLandmarker.createFromOptions(this, options);
        } catch (Exception e) { Log.e(TAG, "MediaPipe initialization failed", e); }
    }

    private void startCamera() {
        if (camera != null || cameraManager == null) return;
        try {
            cameraId = findFrontCamera();
            if (cameraId == null) return;
            cameraThread = new HandlerThread("NOVA-Gesture-Camera");
            cameraThread.start();
            cameraHandler = new Handler(cameraThread.getLooper());
            reader = ImageReader.newInstance(320, 240, android.graphics.ImageFormat.JPEG, 2);
            reader.setOnImageAvailableListener(r -> {
                Image image = r.acquireLatestImage();
                if (image == null) return;
                long now = SystemClock.uptimeMillis();
                if (processing || now - lastFrame < FRAME_INTERVAL_MS) { image.close(); return; }
                lastFrame = now;
                processing = true;
                try { process(image); } catch (Exception e) { Log.w(TAG, "Frame processing failed", e); processing = false; }
            }, cameraHandler);
            cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override public void onOpened(CameraDevice c) { camera = c; createSession(); }
                @Override public void onDisconnected(CameraDevice c) { c.close(); if (camera == c) camera = null; }
                @Override public void onError(CameraDevice c, int error) { c.close(); if (camera == c) camera = null; }
            }, cameraHandler);
        } catch (Exception e) { Log.e(TAG, "Camera start failed", e); }
    }

    private String findFrontCamera() throws CameraAccessException {
        for (String id : cameraManager.getCameraIdList()) {
            Integer facing = cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING);
            if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) return id;
        }
        String[] ids = cameraManager.getCameraIdList();
        return ids.length == 0 ? null : ids[0];
    }

    private void createSession() {
        if (camera == null || reader == null) return;
        try {
            CaptureRequest.Builder request = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            request.addTarget(reader.getSurface());
            try {
                Range<Integer>[] ranges = cameraManager.getCameraCharacteristics(cameraId)
                        .get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
                if (ranges != null) for (Range<Integer> range : ranges) {
                    if (range.getLower() <= 30 && range.getUpper() >= 30) {
                        request.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, range); break;
                    }
                }
            } catch (Exception ignored) { }
            camera.createCaptureSession(Arrays.asList(reader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override public void onConfigured(CameraCaptureSession s) {
                    session = s;
                    try { s.setRepeatingRequest(request.build(), null, cameraHandler); }
                    catch (CameraAccessException e) { Log.e(TAG, "Camera stream failed", e); }
                }
                @Override public void onConfigureFailed(CameraCaptureSession s) { Log.e(TAG, "Camera session failed"); }
            }, cameraHandler);
        } catch (Exception e) { Log.e(TAG, "Session creation failed", e); }
    }

    private void process(Image image) {
        try {
            byte[] bytes = new byte[image.getPlanes()[0].getBuffer().remaining()];
            image.getPlanes()[0].getBuffer().get(bytes);
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            image.close();
            if (bitmap == null || landmarker == null) { processing = false; return; }
            MPImage mp = new BitmapImageBuilder(bitmap).build();
            HandLandmarkerResult result = landmarker.detectForVideo(mp, SystemClock.uptimeMillis());
            List<List<NormalizedLandmark>> hands = result.landmarks();
            if (!hands.isEmpty() && hands.get(0).size() > 8) {
                NormalizedLandmark tip = hands.get(0).get(8);
                update(tip.x(), tip.y());
            } else {
                smoothX = smoothY = -1f;
                pendingX = pendingY = 0f;
            }
            bitmap.recycle();
        } catch (Exception e) {
            try { image.close(); } catch (Exception ignored) { }
            Log.w(TAG, "MediaPipe frame error", e);
        } finally { processing = false; }
    }

    private void update(float x, float y) {
        if (smoothX < 0f) { smoothX = x; smoothY = y; return; }
        float nextX = smoothX + POSITION_SMOOTHING * (x - smoothX);
        float nextY = smoothY + POSITION_SMOOTHING * (y - smoothY);
        float dx = nextX - smoothX;
        float dy = nextY - smoothY;
        smoothX = nextX; smoothY = nextY;
        if (Math.abs(dx) < RESIDUAL_THRESHOLD) dx = 0f;
        if (Math.abs(dy) < RESIDUAL_THRESHOLD) dy = 0f;
        pendingX += dx * MOVEMENT_SCALE;
        pendingY += dy * MOVEMENT_SCALE;
        long now = SystemClock.uptimeMillis();
        if (now - lastOutput < OUTPUT_INTERVAL_MS) return;
        if (Math.abs(pendingX) < RESIDUAL_THRESHOLD && Math.abs(pendingY) < RESIDUAL_THRESHOLD) return;
        float sx = clamp(pendingX, -MAX_STEP, MAX_STEP);
        float sy = clamp(pendingY, -MAX_STEP, MAX_STEP);
        pendingX -= sx; pendingY -= sy;
        NovaAccessibilityService service = NovaAccessibilityService.getInstance();
        if (service != null) {
            service.swipe(sx * 9000f, sy * 9000f);
            lastOutput = now;
        }
    }

    private float clamp(float value, float min, float max) { return Math.max(min, Math.min(max, value)); }

    private void startForegroundCompat() {
        Notification n = new Notification.Builder(this, CHANNEL).setContentTitle("NOVA")
                .setContentText("Gesture control is active").setSmallIcon(android.R.drawable.ic_menu_view)
                .setOngoing(true).build();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            startForeground(NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA);
        else startForeground(NOTIFICATION_ID, n);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel c = new NotificationChannel(CHANNEL, "NOVA Gesture Control", NotificationManager.IMPORTANCE_LOW);
            NotificationManager m = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (m != null) m.createNotificationChannel(c);
        }
    }

    @Override public void onDestroy() {
        try { if (session != null) session.close(); } catch (Exception ignored) { }
        try { if (camera != null) camera.close(); } catch (Exception ignored) { }
        try { if (reader != null) reader.close(); } catch (Exception ignored) { }
        if (cameraThread != null) cameraThread.quitSafely();
        if (landmarker != null) landmarker.close();
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
