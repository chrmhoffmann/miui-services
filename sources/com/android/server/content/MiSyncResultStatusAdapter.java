package com.android.server.content;

import android.content.SyncResult;
import android.content.SyncStatusInfo;
import android.text.TextUtils;
import android.util.Log;
/* loaded from: classes.dex */
public class MiSyncResultStatusAdapter {
    private static final String TAG = "SyncManager";

    public static void updateResultStatus(SyncStatusInfo syncStatusInfo, String lastSyncMessage, SyncResult syncResult) {
        if (TextUtils.equals(lastSyncMessage, "canceled")) {
            if (Log.isLoggable(TAG, 3)) {
                Log.d(TAG, "updateResultStatus: lastSyncMessage is canceled, do nothing");
                return;
            }
            return;
        }
        syncStatusInfo.miSyncStatusInfo.lastResultMessage = lastSyncMessage;
        if (syncResult == null || syncResult.miSyncResult == null || TextUtils.isEmpty(syncResult.miSyncResult.resultMessage)) {
            if (Log.isLoggable(TAG, 3)) {
                Log.d(TAG, "updateResultStatus: sync result message is null");
                return;
            }
            return;
        }
        syncStatusInfo.miSyncStatusInfo.lastResultMessage = syncResult.miSyncResult.resultMessage;
    }
}
