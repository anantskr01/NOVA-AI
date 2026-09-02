package com.nova.ai;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/** Cooperative cancellation registry. Running provider calls may finish, but cancelled tasks never continue. */
public final class NovaTaskController {
    private static final NovaTaskController INSTANCE = new NovaTaskController();
    private final ConcurrentHashMap<String, AtomicBoolean> cancelled = new ConcurrentHashMap<>();
    private NovaTaskController() { }
    public static NovaTaskController get() { return INSTANCE; }
    public void start(String taskId) { if(taskId!=null&&!taskId.isEmpty()) cancelled.put(taskId,new AtomicBoolean(false)); }
    public void cancel(String taskId) { AtomicBoolean flag=cancelled.get(taskId); if(flag!=null)flag.set(true); }
    public boolean isCancelled(String taskId) { AtomicBoolean flag=cancelled.get(taskId); return flag!=null&&flag.get(); }
    public void finish(String taskId) { if(taskId!=null)cancelled.remove(taskId); }
}
