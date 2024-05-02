package com.miui.server;

import android.app.backup.IBackupManager;
import android.app.usage.StorageStats;
import android.app.usage.StorageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageStatsObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageStats;
import android.os.Binder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.util.Slog;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class BackupManagerServiceProxy {
    public static final String TAG = "BackupManagerServiceProxy";

    BackupManagerServiceProxy() {
    }

    public static void fullBackup(ParcelFileDescriptor outFileDescriptor, String[] pkgs, boolean includeApk) throws RemoteException {
        IBackupManager bm = ServiceManager.getService("backup");
        bm.adbBackup(0, outFileDescriptor, includeApk, true, false, false, false, false, false, false, pkgs);
    }

    public static void fullRestore(ParcelFileDescriptor fd) throws RemoteException {
        IBackupManager bm = ServiceManager.getService("backup");
        bm.adbRestore(0, fd);
    }

    public static void fullCancel() throws RemoteException {
        IBackupManager bm = ServiceManager.getService("backup");
        bm.cancelMiuiBackupsForUser(0);
    }

    public static void getPackageSizeInfo(Context context, PackageManager pm, String pkg, int userId, IPackageStatsObserver observer) {
        StorageStatsManager ssm = (StorageStatsManager) context.getSystemService("storagestats");
        long oldId = Binder.clearCallingIdentity();
        try {
            try {
                PackageInfo pInfo = pm.getPackageInfoAsUser(pkg, 0, userId);
                ApplicationInfo appInfo = pInfo.applicationInfo;
                StorageStats stats = ssm.queryStatsForPackage(appInfo.storageUuid, appInfo.packageName, UserHandle.getUserHandleForUid(appInfo.uid));
                PackageStats legacy = new PackageStats(pkg, appInfo.uid);
                legacy.codeSize = stats.getAppBytes();
                legacy.dataSize = stats.getDataBytes();
                legacy.cacheSize = stats.getCacheBytes();
                try {
                    observer.onGetStatsCompleted(legacy, true);
                } catch (RemoteException e) {
                }
            } finally {
                Binder.restoreCallingIdentity(oldId);
            }
        } catch (Exception e2) {
            Slog.e(TAG, "getPackageSizeInfo error", e2);
            try {
                observer.onGetStatsCompleted(new PackageStats(pkg, userId), false);
            } catch (RemoteException e3) {
            }
        }
    }

    public static boolean isPackageStateProtected(PackageManager pm, String packageName, int userId) {
        boolean isProtected = false;
        try {
            isProtected = pm.isPackageStateProtected(packageName, userId);
        } catch (UnsupportedOperationException e) {
            e.printStackTrace();
        }
        Slog.d(TAG, "isPackageStateProtected, packageName:" + packageName + " userId:" + userId + " isProtected:" + isProtected);
        return isProtected;
    }
}
