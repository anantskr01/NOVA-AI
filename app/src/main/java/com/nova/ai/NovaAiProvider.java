package com.nova.ai;

import org.json.JSONObject;

/** Provider-neutral AI contract. Network/model implementations plug in without coupling the Android runtime to one vendor. */
public interface NovaAiProvider {
    void complete(String systemContext, String userInput, JSONObject toolCatalog, Callback callback);

    interface Callback {
        void onSuccess(JSONObject response);
        void onError(Exception error);
    }
}
