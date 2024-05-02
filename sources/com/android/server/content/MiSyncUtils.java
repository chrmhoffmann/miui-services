package com.android.server.content;

import android.accounts.Account;
import android.os.Build;
import android.util.Log;
import com.android.server.content.MiSyncConstants;
import com.android.server.content.SyncManager;
import java.util.HashSet;
import java.util.Iterator;
/* loaded from: classes.dex */
public class MiSyncUtils {
    private static final int HIGH_PARALLEL_SYNC_NUM = Integer.MAX_VALUE;
    private static final HashSet<String> LOW_PARALLEL_SYNC_DEVICES;
    private static final int LOW_PARALLEL_SYNC_NUM = 1;
    private static final String TAG = "MiSyncUtils";
    private static final int XIAOMI_MAX_PARALLEL_SYNC_NUM;

    static {
        HashSet<String> hashSet = new HashSet<>();
        LOW_PARALLEL_SYNC_DEVICES = hashSet;
        hashSet.add("onc");
        hashSet.add("pine");
        hashSet.add("ugg");
        hashSet.add("cactus");
        hashSet.add("cereus");
        hashSet.add("santoni");
        hashSet.add("riva");
        hashSet.add("rosy");
        hashSet.add("rolex");
        String device = Build.DEVICE.toLowerCase();
        if (hashSet.contains(device)) {
            XIAOMI_MAX_PARALLEL_SYNC_NUM = 1;
        } else {
            XIAOMI_MAX_PARALLEL_SYNC_NUM = HIGH_PARALLEL_SYNC_NUM;
        }
        Log.i(TAG, "Max parallel sync number is " + XIAOMI_MAX_PARALLEL_SYNC_NUM);
    }

    public static boolean isSyncRoomForbiddenH(SyncOperation op, SyncManager syncManager) {
        if (op == null || syncManager == null) {
            if (Log.isLoggable(TAG, 3)) {
                Log.d(TAG, "injector: isSyncRoomAvailable: null parameter, false");
            }
            return false;
        } else if (!checkSyncOperationAccount(op)) {
            if (Log.isLoggable(TAG, 3)) {
                Log.d(TAG, "injector: isSyncRoomAvailable: not xiaomi account, false");
            }
            return false;
        } else if (checkSyncOperationPass(op)) {
            if (Log.isLoggable(TAG, 3)) {
                Log.d(TAG, "injector: isSyncRoomAvailable: sync operation pass, false");
            }
            return false;
        } else {
            String device = Build.DEVICE.toLowerCase();
            if ("dipper".equals(device)) {
                return false;
            }
            int count = 0;
            Iterator it = syncManager.mActiveSyncContexts.iterator();
            while (it.hasNext()) {
                SyncManager.ActiveSyncContext activeSyncContext = (SyncManager.ActiveSyncContext) it.next();
                if (checkSyncOperationAccount(activeSyncContext.mSyncOperation)) {
                    count++;
                }
            }
            return count >= XIAOMI_MAX_PARALLEL_SYNC_NUM;
        }
    }

    public static boolean checkSyncOperationAccount(SyncOperation syncOperation) {
        if (syncOperation == null || syncOperation.target == null || syncOperation.target.account == null) {
            if (Log.isLoggable(TAG, 3)) {
                Log.d(TAG, "injector: checkSyncOperationAccount: false");
                return false;
            }
            return false;
        }
        Account account = syncOperation.target.account;
        if (Log.isLoggable(TAG, 3)) {
            Log.d(TAG, "injector: checkSyncOperationAccount: " + account.type);
        }
        return MiSyncConstants.Config.XIAOMI_ACCOUNT_TYPE.equals(account.type);
    }

    public static boolean checkSyncOperationPass(SyncOperation syncOperation) {
        if (syncOperation == null) {
            if (Log.isLoggable(TAG, 3)) {
                Log.d(TAG, "injector: checkSyncOperationPass: null parameter, fail");
            }
            return false;
        } else if (syncOperation.isInitialization() || syncOperation.isManual() || syncOperation.isIgnoreSettings()) {
            if (Log.isLoggable(TAG, 3)) {
                Log.d(TAG, "injector: checkSyncOperationPass: init or ignore settings, pass");
            }
            return true;
        } else if (syncOperation.reason == -6) {
            if (Log.isLoggable(TAG, 3)) {
                Log.d(TAG, "injector: checkSyncOperationPass: sync for auto, pass");
            }
            return true;
        } else {
            if (Log.isLoggable(TAG, 3)) {
                Log.d(TAG, "injector: checkSyncOperationPass: fail");
            }
            return false;
        }
    }

    public static boolean checkAccount(Account account) {
        if (account == null) {
            if (Log.isLoggable(TAG, 3)) {
                Log.d(TAG, "injector: checkAccount: false");
                return false;
            }
            return false;
        }
        if (Log.isLoggable(TAG, 3)) {
            Log.d(TAG, "injector: checkAccount: " + account.type);
        }
        return MiSyncConstants.Config.XIAOMI_ACCOUNT_TYPE.equals(account.type);
    }
}
