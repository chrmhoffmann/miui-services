package com.android.server.am;

import org.json.JSONException;
import org.json.JSONObject;
/* loaded from: classes.dex */
public interface IGameProcessAction {

    /* loaded from: classes.dex */
    public interface IGameProcessActionConfig {
        int getPrio();

        void initFromJSON(JSONObject jSONObject) throws JSONException;
    }

    long doAction(long j);

    boolean shouldSkip(ProcessRecord processRecord);
}
