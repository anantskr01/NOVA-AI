package com.nova.ai;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;

/** Provider -> response parser -> bounded multi-step task execution with cancellation and recovery. */
public final class NovaModelLoop {
    public interface Callback { void onComplete(JSONObject result); }
    private static final int MAX_STEPS = 8;
    private final NovaContextStore contextStore;
    private final NovaRuntime runtime;
    private final NovaTaskOrchestrator orchestrator;
    private NovaAiProvider provider;

    public NovaModelLoop(Context context) {
        Context app = context.getApplicationContext();
        contextStore = new NovaContextStore(app);
        runtime = NovaRuntime.get(app);
        orchestrator = runtime.orchestrator();
    }

    public void setProvider(NovaAiProvider provider) { this.provider = provider; }

    public String run(String input, Callback callback) {
        if (provider == null) { fail(callback, "ai_provider_not_configured"); return ""; }
        final String text = input == null ? "" : input.trim();
        final NovaTaskState state = new NovaTaskState(NovaProtocol.id(), text);
        NovaTaskController.get().start(state.taskId());
        requestNext(text, contextStore.recent(), new JSONArray(), state, callback);
        return state.taskId();
    }

    public void cancel(String taskId) { NovaTaskController.get().cancel(taskId); }

    private void requestNext(String userText, JSONArray context, JSONArray executions, NovaTaskState state, Callback callback) {
        if (NovaTaskController.get().isCancelled(state.taskId())) {
            state.fail("cancelled"); finish(userText, executions, "cancelled", callback, "Task cancelled.", state); return;
        }
        if (state.step() >= MAX_STEPS) {
            state.fail("maximum agent steps reached");
            finish(userText, executions, "max_steps_reached", callback, "I stopped after reaching the task step limit.", state); return;
        }
        NovaAiRequest request = new NovaAiRequest(
                "You are NOVA, a reliable Android agent. Use only tools in the supplied catalog. Never invent permissions or capabilities. Ask for confirmation before sensitive actions. For complex tasks, observe the screen before acting, execute the smallest useful action, then verify the result with screen.observe or a specific result. If a tool fails, diagnose and retry with corrected arguments when safe. You may return multiple independent tool calls in one response; dependent actions must be separate turns. Use web.search then web.fetch for research and synthesize multiple sources. Use device.send_command only for a connected trusted node. Stop when the task is complete.",
                userText, context, runtime.tools().describe());
        provider.complete(request.systemContext, request.userInput, request.toJson(), new NovaAiProvider.Callback() {
            @Override public void onSuccess(JSONObject response) {
                if (NovaTaskController.get().isCancelled(state.taskId())) { state.fail("cancelled"); finish(userText,executions,"cancelled",callback,"Task cancelled.",state); return; }
                NovaAiResponse parsed = NovaAiResponse.parse(response);
                try {
                    if (parsed.toolCalls.length() == 0) {
                        state.complete(); finish(userText, executions, "complete", callback, parsed.text, state); return;
                    }
                    JSONArray nextContext = new JSONArray();
                    for (int i = 0; i < context.length(); i++) nextContext.put(context.opt(i));
                    nextContext.put(new JSONObject().put("role", "user").put("content", userText));

                    JSONArray assistantCalls = new JSONArray();
                    JSONArray runnable = new JSONArray();
                    for (int i = 0; i < parsed.toolCalls.length(); i++) {
                        JSONObject raw = parsed.toolCalls.optJSONObject(i); if(raw==null)continue;
                        NovaToolCall call = NovaToolCall.fromJson(raw); if(!state.markCall(call.id))continue;
                        runnable.put(call.toJson());
                        JSONObject fn=new JSONObject().put("name",call.tool).put("arguments",call.input==null?"{}":call.input.toString());
                        assistantCalls.put(new JSONObject().put("id",call.id).put("type","function").put("function",fn));
                    }
                    if(assistantCalls.length()>0) nextContext.put(new JSONObject().put("role","assistant").put("content",parsed.text==null?"":parsed.text).put("tool_calls",assistantCalls));

                    JSONArray batch;
                    if (runnable.length() > 1) batch = orchestrator.executeParallel(runnable, runtime.executor()::execute);
                    else {
                        batch=new JSONArray();
                        if(runnable.length()==1){JSONObject call=runnable.optJSONObject(0);JSONObject execution=runtime.executor().execute(call.optString("tool"),call.optJSONObject("input"));batch.put(new JSONObject().put("id",call.optString("id")).put("step",0).put("tool",call.optString("tool")).put("result",execution));}
                    }
                    for(int i=0;i<batch.length();i++){
                        JSONObject item=batch.optJSONObject(i);if(item==null)continue;JSONObject execution=item.optJSONObject("result");if(execution==null)execution=new JSONObject().put("ok",false).put("error","missing_execution_result");
                        executions.put(new JSONObject().put("id",item.optString("id")).put("tool",item.optString("tool")).put("execution",execution));
                        nextContext.put(new JSONObject().put("role","tool").put("tool_call_id",item.optString("id")).put("name",item.optString("tool")).put("content",NovaAgentPolicy.bounded(execution.toString(),NovaAgentPolicy.MAX_TOOL_RESULT_CHARS)));
                    }
                    state.advance(); requestNext(userText,nextContext,executions,state,callback);
                } catch(Exception e){state.fail(e.getMessage());finish(userText,executions,"execution_error",callback,"NOVA could not complete the task safely.",state);}
            }
            @Override public void onError(Exception error) {
                state.fail(error==null?"provider request failed":error.getMessage());
                JSONObject result=new JSONObject();
                try{result.put("ok",false).put("error","ai_provider_error").put("message",error==null?"provider request failed":String.valueOf(error.getMessage())).put("task",state.toJson()).put("executions",executions);}catch(Exception ignored){}
                NovaTaskController.get().finish(state.taskId()); if(callback!=null)callback.onComplete(result);
            }
        });
    }

    private void finish(String userText,JSONArray executions,String status,Callback callback,String text,NovaTaskState state){
        JSONObject result=new JSONObject();
        try{result.put("ok","execution_error".equals(status)?false:true).put("status",status).put("text",text==null?"":text).put("executions",executions).put("task",state.toJson());contextStore.add("user",userText);if(text!=null&&!text.trim().isEmpty())contextStore.add("assistant",text);}catch(Exception ignored){}
        NovaTaskController.get().finish(state.taskId()); if(callback!=null)callback.onComplete(result);
    }
    private static void fail(Callback callback,String error){if(callback==null)return;JSONObject result=new JSONObject();try{result.put("ok",false).put("error",error);}catch(Exception ignored){}callback.onComplete(result);}
}
