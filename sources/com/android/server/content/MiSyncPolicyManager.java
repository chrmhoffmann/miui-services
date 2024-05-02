package com.android.server.content;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;
import android.util.Slog;
import com.android.server.content.SyncManager;
import java.util.Iterator;
import java.util.List;
/* loaded from: classes.dex */
public class MiSyncPolicyManager extends MiSyncPolicyManagerBase {
    private static final Uri SYNC_ON_WIFI_ONLY_URI = Settings.Secure.getUriFor("sync_on_wifi_only");
    private static final String TAG = "SyncManager";

    public static void registerSyncSettingsObserver(Context context, final SyncManager syncManager) {
        ContentObserver syncSettingsObserver = new ContentObserver(syncManager.mSyncHandler) { // from class: com.android.server.content.MiSyncPolicyManager.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                MiSyncPolicyManager.handleMasterWifiOnlyChanged(syncManager);
            }
        };
        context.getContentResolver().registerContentObserver(SYNC_ON_WIFI_ONLY_URI, false, syncSettingsObserver);
    }

    public static long getSyncDelayedH(SyncOperation op, SyncManager syncManager) {
        if (isSyncRoomForbiddenH(op, syncManager)) {
            if (Log.isLoggable(TAG, 3)) {
                Log.d(TAG, "injector: sync is forbidden for no room!");
                return 30000L;
            }
            return 30000L;
        }
        return 0L;
    }

    private static boolean isSyncRoomForbiddenH(SyncOperation op, SyncManager syncManager) {
        return MiSyncUtils.isSyncRoomForbiddenH(op, syncManager);
    }

    public static void wrapSyncJobInfo(Context context, SyncOperation op, SyncStorageEngine syncStorageEngine, JobInfo.Builder builder, long minDelay) {
        SyncJobInfoProcessor.buildSyncJobInfo(context, op, syncStorageEngine, builder, minDelay);
    }

    public static void handleMasterWifiOnlyChanged(final SyncManager syncManager) {
        syncManager.mSyncHandler.post(new Runnable() { // from class: com.android.server.content.MiSyncPolicyManager.2
            @Override // java.lang.Runnable
            public void run() {
                MiSyncPolicyManager.rescheduleAllSyncsH(syncManager);
            }
        });
    }

    public static void handleSyncPauseChanged(Context context, SyncManager syncManager, long pauseTimeMills) {
        handleSyncPauseChanged(syncManager);
    }

    public static void handleSyncPauseChanged(final SyncManager syncManager) {
        syncManager.mSyncHandler.post(new Runnable() { // from class: com.android.server.content.MiSyncPolicyManager.3
            @Override // java.lang.Runnable
            public void run() {
                MiSyncPolicyManager.rescheduleXiaomiSyncsH(syncManager);
            }
        });
    }

    public static void handleSyncStrategyChanged(Context context, SyncManager syncManager) {
        handleSyncStrategyChanged(syncManager);
    }

    public static void handleSyncStrategyChanged(final SyncManager syncManager) {
        syncManager.mSyncHandler.post(new Runnable() { // from class: com.android.server.content.MiSyncPolicyManager.4
            @Override // java.lang.Runnable
            public void run() {
                MiSyncPolicyManager.rescheduleXiaomiSyncsH(syncManager);
            }
        });
    }

    public static void rescheduleAllSyncsH(SyncManager syncManager) {
        Iterator it = syncManager.mActiveSyncContexts.iterator();
        while (it.hasNext()) {
            SyncManager.ActiveSyncContext asc = (SyncManager.ActiveSyncContext) it.next();
            syncManager.mSyncHandler.deferActiveSyncH(asc, "reschedule-all");
        }
        JobScheduler jobScheduler = syncManager.getJobScheduler();
        List<SyncOperation> ops = syncManager.getAllPendingSyncs();
        int count = 0;
        for (SyncOperation op : ops) {
            count++;
            jobScheduler.cancel(op.jobId);
            SyncManagerAdapter.postScheduleSyncMessage(syncManager, op, 0L);
        }
        if (Log.isLoggable(TAG, 2)) {
            Slog.v(TAG, "Rescheduled " + count + " syncs");
        }
    }

    public static void rescheduleXiaomiSyncsH(SyncManager syncManager) {
        Iterator it = syncManager.mActiveSyncContexts.iterator();
        while (it.hasNext()) {
            SyncManager.ActiveSyncContext asc = (SyncManager.ActiveSyncContext) it.next();
            if (MiSyncUtils.checkSyncOperationAccount(asc.mSyncOperation)) {
                syncManager.mSyncHandler.deferActiveSyncH(asc, "reschedule-xiaomi");
            }
        }
        JobScheduler jobScheduler = syncManager.getJobScheduler();
        List<SyncOperation> ops = syncManager.getAllPendingSyncs();
        int count = 0;
        for (SyncOperation op : ops) {
            if (MiSyncUtils.checkSyncOperationAccount(op)) {
                count++;
                jobScheduler.cancel(op.jobId);
                SyncManagerAdapter.postScheduleSyncMessage(syncManager, op, 0L);
            }
        }
        if (Log.isLoggable(TAG, 2)) {
            Slog.v(TAG, "Rescheduled " + count + " syncs");
        }
    }
}
