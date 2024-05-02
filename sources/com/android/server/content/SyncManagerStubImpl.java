package com.android.server.content;

import android.accounts.Account;
import android.app.job.JobInfo;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;
import com.android.server.LocalServices;
import com.android.server.am.AutoStartManagerService;
import com.android.server.content.MiSyncConstants;
import com.miui.base.MiuiStubRegistry;
import com.miui.server.process.ProcessManagerInternal;
/* loaded from: classes.dex */
public class SyncManagerStubImpl extends SyncManagerAccountChangePolicy implements SyncManagerStub {
    public static final long SYNC_DELAY_ON_DISALLOW_METERED = 3600000;
    private static final String TAG = "SyncManager";
    public static final Uri uri = Settings.Secure.getUriFor("sync_on_wifi_only");
    ProcessManagerInternal mPmi = null;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<SyncManagerStubImpl> {

        /* compiled from: SyncManagerStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final SyncManagerStubImpl INSTANCE = new SyncManagerStubImpl();
        }

        public SyncManagerStubImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public SyncManagerStubImpl provideNewInstance() {
            return new SyncManagerStubImpl();
        }
    }

    public boolean canBindService(Context context, Intent service, int userId) {
        return AutoStartManagerService.getInstance().isAllowStartService(context, service, userId);
    }

    public static boolean isDisallowMeteredBySettings(Context ctx) {
        return Settings.Secure.getInt(ctx.getContentResolver(), "sync_on_wifi_only", 0) == 1;
    }

    public void registerSyncSettingsObserver(Context context, SyncManager syncManager) {
        MiSyncPolicyManager.registerSyncSettingsObserver(context, syncManager);
    }

    public long getSyncDelayedH(SyncOperation op, SyncManager syncManager) {
        return MiSyncPolicyManager.getSyncDelayedH(op, syncManager);
    }

    public void wrapSyncJobInfo(Context context, SyncOperation op, SyncStorageEngine syncStorageEngine, JobInfo.Builder builder, long minDelay) {
        MiSyncPolicyManager.wrapSyncJobInfo(context, op, syncStorageEngine, builder, minDelay);
    }

    public boolean getMasterSyncAutomatically(Account account, int userId, SyncStorageEngine syncStorageEngine) {
        if (account != null && MiSyncConstants.Config.XIAOMI_ACCOUNT_TYPE.equals(account.type)) {
            return true;
        }
        return syncStorageEngine.getMasterSyncAutomatically(userId);
    }

    public boolean isRestrictSync(String pkgName, int uid, Object[] reserved) {
        if (this.mPmi == null) {
            ProcessManagerInternal processManagerInternal = (ProcessManagerInternal) LocalServices.getService(ProcessManagerInternal.class);
            this.mPmi = processManagerInternal;
            if (processManagerInternal == null) {
                Log.d(TAG, "isRestrictSync false for service unready uid=" + uid);
                return false;
            }
        }
        this.mPmi.isForegroundApp(pkgName, uid);
        return false;
    }

    public static void handleMasterWifiOnlyChanged(SyncManager syncManager) {
        MiSyncPolicyManager.handleMasterWifiOnlyChanged(syncManager);
    }

    public static void handleSyncPauseChanged(Context context, SyncManager syncManager, long pauseTimeMills) {
        MiSyncPolicyManager.handleSyncPauseChanged(context, syncManager, pauseTimeMills);
    }

    public static void handleSyncPauseChanged(SyncManager syncManager) {
        MiSyncPolicyManager.handleSyncPauseChanged(syncManager);
    }

    public static void handleSyncStrategyChanged(Context context, SyncManager syncManager) {
        MiSyncPolicyManager.handleSyncStrategyChanged(context, syncManager);
    }

    public static void handleSyncStrategyChanged(SyncManager syncManager) {
        MiSyncPolicyManager.handleSyncStrategyChanged(syncManager);
    }
}
