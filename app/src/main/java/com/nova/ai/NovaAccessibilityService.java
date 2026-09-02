package com.nova.ai;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Bitmap;
import android.graphics.Path;
import android.graphics.Rect;
import android.hardware.HardwareBuffer;
import android.os.Build;
import android.os.Bundle;
import android.view.Display;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Android execution/observation boundary. */
public final class NovaAccessibilityService extends AccessibilityService {
    private static NovaAccessibilityService instance; private static final long SWIPE_MS=45L;
    @Override public void onServiceConnected(){super.onServiceConnected();instance=this;} @Override public void onAccessibilityEvent(AccessibilityEvent event){} @Override public void onInterrupt(){} @Override public void onDestroy(){if(instance==this)instance=null;super.onDestroy();}
    public static NovaAccessibilityService getInstance(){return instance;}
    public String screenText(){AccessibilityNodeInfo root=getRootInActiveWindow();if(root==null)return"";StringBuilder out=new StringBuilder();collectText(root,out,0);return out.toString().trim();}
    public String uiSnapshot(){AccessibilityNodeInfo root=getRootInActiveWindow();if(root==null)return"No accessibility window.";StringBuilder out=new StringBuilder();collectUi(root,out,0);return out.length()>9000?out.substring(0,9000):out.toString();}
    public Bitmap captureScreen(long timeoutMs){if(Build.VERSION.SDK_INT<30)return null;CountDownLatch latch=new CountDownLatch(1);AtomicReference<Bitmap> result=new AtomicReference<>();try{takeScreenshot(Display.DEFAULT_DISPLAY,getMainExecutor(),new TakeScreenshotCallback(){public void onSuccess(ScreenshotResult screenshot){HardwareBuffer buffer=null;try{buffer=screenshot.getHardwareBuffer();Bitmap raw=Bitmap.wrapHardwareBuffer(buffer,screenshot.getColorSpace());if(raw!=null)result.set(raw.copy(Bitmap.Config.ARGB_8888,false));}catch(Exception ignored){}finally{if(buffer!=null)buffer.close();latch.countDown();}}public void onFailure(int errorCode){latch.countDown();}});latch.await(Math.max(250,Math.min(timeoutMs,3000)),TimeUnit.MILLISECONDS);}catch(Exception ignored){}return result.get();}
    private void collectText(AccessibilityNodeInfo n,StringBuilder out,int depth){if(n==null||depth>20||out.length()>7000)return;CharSequence t=n.getText(),d=n.getContentDescription();if(t!=null&&t.length()>0)out.append(t).append('\n');else if(d!=null&&d.length()>0)out.append(d).append('\n');for(int i=0;i<n.getChildCount();i++)collectText(n.getChild(i),out,depth+1);}
    private void collectUi(AccessibilityNodeInfo n,StringBuilder out,int depth){if(n==null||depth>20||out.length()>9000)return;CharSequence t=n.getText(),d=n.getContentDescription();String id="";if(Build.VERSION.SDK_INT>=18)id=n.getViewIdResourceName();if((t!=null&&t.length()>0)||(d!=null&&d.length()>0)||!id.isEmpty()||n.isClickable()||n.isEditable()){Rect r=new Rect();n.getBoundsInScreen(r);out.append("• ").append(t!=null&&t.length()>0?t:d!=null&&d.length()>0?d:"[unlabeled]").append(" | id=").append(id).append(" | clickable=").append(n.isClickable()).append(" | editable=").append(n.isEditable()).append(" | enabled=").append(n.isEnabled()).append(" | bounds=").append(r.toShortString()).append('\n');}for(int i=0;i<n.getChildCount();i++)collectUi(n.getChild(i),out,depth+1);}
    public boolean clickText(String text){AccessibilityNodeInfo root=getRootInActiveWindow();if(root==null||text==null||text.trim().isEmpty())return false;AccessibilityNodeInfo target=find(root,text.trim().toLowerCase(Locale.ROOT),0);return target!=null&&click(target);}
    public boolean clickResourceId(String resourceId){AccessibilityNodeInfo root=getRootInActiveWindow();if(root==null||resourceId==null||resourceId.trim().isEmpty())return false;AccessibilityNodeInfo target=findId(root,resourceId.trim(),0);return target!=null&&click(target);}
    public boolean longPressText(String text){AccessibilityNodeInfo root=getRootInActiveWindow();if(root==null||text==null||text.trim().isEmpty())return false;AccessibilityNodeInfo target=find(root,text.trim().toLowerCase(Locale.ROOT),0);if(target==null)return false;Rect r=new Rect();target.getBoundsInScreen(r);return longPress((r.left+r.right)/2f,(r.top+r.bottom)/2f);}
    public boolean clickAt(float x,float y){Path p=new Path();p.moveTo(x,y);return dispatchGesture(new GestureDescription.Builder().addStroke(new GestureDescription.StrokeDescription(p,0,40)).build(),null,null);}
    public boolean typeText(String text){if(text==null)return false;AccessibilityNodeInfo root=getRootInActiveWindow();if(root==null)return false;AccessibilityNodeInfo target=findEditable(root,0);if(target==null)return false;Bundle args=new Bundle();args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,text);return target.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,args);}
    public boolean typeIntoText(String label,String text){if(text==null)return false;AccessibilityNodeInfo root=getRootInActiveWindow();if(root==null)return false;AccessibilityNodeInfo target=label==null||label.trim().isEmpty()?findEditable(root,0):find(root,label.trim().toLowerCase(Locale.ROOT),0);if(target==null||!target.isEditable())target=findEditable(root,0);if(target==null)return false;Bundle args=new Bundle();args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,text);return target.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,args);}
    private AccessibilityNodeInfo find(AccessibilityNodeInfo n,String q,int depth){if(n==null||depth>20)return null;String t=n.getText()==null?"":n.getText().toString().trim().toLowerCase(Locale.ROOT);String d=n.getContentDescription()==null?"":n.getContentDescription().toString().trim().toLowerCase(Locale.ROOT);if(n.isVisibleToUser()&&n.isEnabled()&&(t.equals(q)||d.equals(q)||t.contains(q)||d.contains(q)))return n;for(int i=0;i<n.getChildCount();i++){AccessibilityNodeInfo x=find(n.getChild(i),q,depth+1);if(x!=null)return x;}return null;}
    private AccessibilityNodeInfo findId(AccessibilityNodeInfo n,String q,int depth){if(n==null||depth>20)return null;String id=Build.VERSION.SDK_INT>=18?n.getViewIdResourceName():null;if(n.isVisibleToUser()&&n.isEnabled()&&id!=null&&(id.equals(q)||id.endsWith(":"+q)||id.endsWith("/"+q)))return n;for(int i=0;i<n.getChildCount();i++){AccessibilityNodeInfo x=findId(n.getChild(i),q,depth+1);if(x!=null)return x;}return null;}
    private AccessibilityNodeInfo findEditable(AccessibilityNodeInfo n,int depth){if(n==null||depth>20)return null;if(n.isVisibleToUser()&&n.isEnabled()&&n.isEditable())return n;for(int i=0;i<n.getChildCount();i++){AccessibilityNodeInfo x=findEditable(n.getChild(i),depth+1);if(x!=null)return x;}return null;}
    private boolean click(AccessibilityNodeInfo n){if(n.isClickable()&&n.performAction(AccessibilityNodeInfo.ACTION_CLICK))return true;AccessibilityNodeInfo p=n.getParent();for(int i=0;p!=null&&i<6;i++,p=p.getParent())if(p.isClickable()&&p.performAction(AccessibilityNodeInfo.ACTION_CLICK))return true;return false;}
    private boolean longPress(float x,float y){Path path=new Path();path.moveTo(x,y);return dispatchGesture(new GestureDescription.Builder().addStroke(new GestureDescription.StrokeDescription(path,0,650)).build(),null,null);}
    public boolean swipe(float dx,float dy){int w=getResources().getDisplayMetrics().widthPixels,h=getResources().getDisplayMetrics().heightPixels;float cx=w/2f,cy=h/2f;Path path=new Path();path.moveTo(cx,cy);path.lineTo(Math.max(5,Math.min(w-5,cx+dx)),Math.max(5,Math.min(h-5,cy+dy)));return dispatchGesture(new GestureDescription.Builder().addStroke(new GestureDescription.StrokeDescription(path,0,SWIPE_MS)).build(),null,null);}
}
