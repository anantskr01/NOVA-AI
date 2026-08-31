package com.nova.ai;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.Rect;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.Locale;

/** Android execution/observation boundary. The agent never bypasses Android permissions. */
public final class NovaAccessibilityService extends AccessibilityService {
    private static NovaAccessibilityService instance;
    private static final long SWIPE_MS = 45L;

    @Override public void onServiceConnected() { super.onServiceConnected(); instance = this; }
    @Override public void onAccessibilityEvent(AccessibilityEvent event) { }
    @Override public void onInterrupt() { }
    @Override public void onDestroy() { if (instance == this) instance = null; super.onDestroy(); }
    public static NovaAccessibilityService getInstance() { return instance; }

    public String screenText() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return "";
        StringBuilder out = new StringBuilder();
        collectText(root, out, 0);
        return out.toString().trim();
    }

    public String uiSnapshot() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return "No accessibility window.";
        StringBuilder out = new StringBuilder();
        collectUi(root, out, 0);
        return out.length() > 6000 ? out.substring(0, 6000) : out.toString();
    }

    private void collectText(AccessibilityNodeInfo n, StringBuilder out, int depth) {
        if (n == null || depth > 20 || out.length() > 5000) return;
        CharSequence t = n.getText();
        CharSequence d = n.getContentDescription();
        if (t != null && t.length() > 0) out.append(t).append('\n');
        else if (d != null && d.length() > 0) out.append(d).append('\n');
        for (int i = 0; i < n.getChildCount(); i++) collectText(n.getChild(i), out, depth + 1);
    }

    private void collectUi(AccessibilityNodeInfo n, StringBuilder out, int depth) {
        if (n == null || depth > 20 || out.length() > 6000) return;
        CharSequence t = n.getText(); CharSequence d = n.getContentDescription();
        if ((t != null && t.length() > 0) || (d != null && d.length() > 0) || n.isClickable()) {
            Rect r = new Rect(); n.getBoundsInScreen(r);
            out.append("• ").append(t != null && t.length() > 0 ? t : d == null ? "[unlabeled]" : d)
                    .append(" | clickable=").append(n.isClickable()).append(" | enabled=").append(n.isEnabled())
                    .append(" | bounds=").append(r.toShortString()).append('\n');
        }
        for (int i = 0; i < n.getChildCount(); i++) collectUi(n.getChild(i), out, depth + 1);
    }

    public boolean clickText(String text) {
        if (text == null || text.trim().isEmpty()) return false;
        AccessibilityNodeInfo root = getRootInActiveWindow(); if (root == null) return false;
        AccessibilityNodeInfo target = find(root, text.trim().toLowerCase(Locale.ROOT), 0);
        return target != null && click(target);
    }

    private AccessibilityNodeInfo find(AccessibilityNodeInfo n, String q, int depth) {
        if (n == null || depth > 20) return null;
        String t = n.getText() == null ? "" : n.getText().toString().trim().toLowerCase(Locale.ROOT);
        String d = n.getContentDescription() == null ? "" : n.getContentDescription().toString().trim().toLowerCase(Locale.ROOT);
        if (n.isVisibleToUser() && n.isEnabled() && (t.equals(q) || d.equals(q) || t.contains(q) || d.contains(q))) return n;
        for (int i = 0; i < n.getChildCount(); i++) { AccessibilityNodeInfo x = find(n.getChild(i), q, depth + 1); if (x != null) return x; }
        return null;
    }

    private boolean click(AccessibilityNodeInfo n) {
        if (n.isClickable() && n.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true;
        AccessibilityNodeInfo p = n.getParent();
        for (int i = 0; p != null && i < 6; i++, p = p.getParent()) if (p.isClickable() && p.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true;
        return false;
    }

    public boolean swipe(float dx, float dy) {
        int w = getResources().getDisplayMetrics().widthPixels, h = getResources().getDisplayMetrics().heightPixels;
        float cx = w / 2f, cy = h / 2f;
        Path path = new Path(); path.moveTo(cx, cy); path.lineTo(Math.max(5, Math.min(w - 5, cx + dx)), Math.max(5, Math.min(h - 5, cy + dy)));
        return dispatchGesture(new GestureDescription.Builder().addStroke(new GestureDescription.StrokeDescription(path, 0, SWIPE_MS)).build(), null, null);
    }
}
