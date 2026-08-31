package com.nova.ai;

import org.json.JSONObject;

/** A capability exposed to the NOVA agent. Tools must validate input and remain permission-bound. */
public interface NovaTool {
    String id();
    String description();
    JSONObject schema();
    JSONObject execute(JSONObject input) throws Exception;
}
