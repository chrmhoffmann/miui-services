package com.android.server;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.VersionedPackage;
import android.net.Uri;
import android.util.Slog;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.server.am.BroadcastQueueImpl;
import com.android.server.am.MiuiWarnings;
import com.android.server.pm.PackageManagerServiceUtils;
import com.miui.base.MiuiStubRegistry;
/* loaded from: classes.dex */
public class PackageWatchdogImpl extends PackageWatchdogStub {

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<PackageWatchdogImpl> {

        /* compiled from: PackageWatchdogImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final PackageWatchdogImpl INSTANCE = new PackageWatchdogImpl();
        }

        public PackageWatchdogImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public PackageWatchdogImpl provideNewInstance() {
            return new PackageWatchdogImpl();
        }
    }

    private void clearAppCacheAndData(PackageManager pm, String currentCrashAppName) {
        Slog.w("RescuePartyPlus", "Clear app cache and data: " + currentCrashAppName);
        PackageManagerServiceUtils.logCriticalInfo(3, "Clear app cache and data: " + currentCrashAppName);
        pm.deleteApplicationCacheFiles(currentCrashAppName, null);
        pm.clearApplicationUserData(currentCrashAppName, null);
    }

    public boolean doRescuePartyPlusStep(int mitigationCount, VersionedPackage versionedPackage, Context context) {
        if (RescuePartyPlusHelper.checkDisableRescuePartyPlus()) {
            return false;
        }
        RescuePartyPlusHelper.setMitigationTempCount(mitigationCount);
        if (versionedPackage == null || versionedPackage.getPackageName() == null) {
            Slog.e("RescuePartyPlus", "Package Watchdog check versioned package failed: " + versionedPackage);
            return false;
        }
        Slog.w("RescuePartyPlus", "doRescuePartyPlusStep " + versionedPackage.getPackageName() + ": [" + mitigationCount + "]");
        final String currentCrashAppName = versionedPackage.getPackageName();
        if (RescuePartyPlusHelper.checkPackageIsCore(currentCrashAppName)) {
            Slog.e("RescuePartyPlus", "Skip because system crash: " + versionedPackage);
            return false;
        }
        PackageManager pm = context.getPackageManager();
        switch (mitigationCount) {
            case 0:
                return false;
            case 1:
                return false;
            case 2:
                return false;
            case 3:
                if (!RescuePartyPlusHelper.checkPackageIsTOPUI(currentCrashAppName)) {
                    return false;
                }
                clearAppCacheAndData(pm, currentCrashAppName);
                return false;
            case 4:
                if (currentCrashAppName.equals(RescuePartyPlusHelper.getLauncherPackageName(context))) {
                    clearAppCacheAndData(pm, currentCrashAppName);
                } else if (!RescuePartyPlusHelper.checkPackageIsTOPUI(currentCrashAppName)) {
                    Slog.w("RescuePartyPlus", "Clear app cache: " + currentCrashAppName);
                    pm.deleteApplicationCacheFiles(currentCrashAppName, null);
                } else {
                    clearAppCacheAndData(pm, currentCrashAppName);
                    if (!RescuePartyPlusHelper.resetTheme(currentCrashAppName)) {
                        Slog.e("RescuePartyPlus", "Reset theme failed: " + currentCrashAppName);
                    }
                    RescuePartyPlusHelper.setLastResetConfigStatus(true);
                    RescuePartyPlusHelper.setShowResetConfigUIStatus(false);
                    maybeShowRecoveryTip(context);
                }
                return true;
            case 5:
                if (!RescuePartyPlusHelper.checkPackageIsTOPUI(currentCrashAppName)) {
                    Slog.w("RescuePartyPlus", "Try to rollback app and clear cache : " + currentCrashAppName);
                    if (!currentCrashAppName.equals(RescuePartyPlusHelper.getLauncherPackageName(context))) {
                        Slog.w("RescuePartyPlus", "Clear app cache: " + currentCrashAppName);
                        pm.deleteApplicationCacheFiles(currentCrashAppName, null);
                    } else {
                        clearAppCacheAndData(pm, currentCrashAppName);
                    }
                    try {
                        ApplicationInfo applicationInfo = pm.getApplicationInfo(currentCrashAppName, 0);
                        if (applicationInfo != null) {
                            if (applicationInfo.isSystemApp() && applicationInfo.isUpdatedSystemApp()) {
                                Slog.w("RescuePartyPlus", "App install path: " + applicationInfo.sourceDir);
                                PackageManagerServiceUtils.logCriticalInfo(3, "Finished rescue level ROLLBACK_APP for package " + currentCrashAppName);
                                pm.deletePackage(currentCrashAppName, null, 2);
                                Slog.w("RescuePartyPlus", "Uninstall app: " + currentCrashAppName);
                            } else if (applicationInfo.isSystemApp() || !currentCrashAppName.equals(RescuePartyPlusHelper.getLauncherPackageName(context))) {
                                Slog.w("RescuePartyPlus", "no action for app: " + currentCrashAppName);
                            } else {
                                Slog.w("RescuePartyPlus", "Third party Launcher, no action for app: " + currentCrashAppName);
                            }
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                        Slog.e("RescuePartyPlus", "get application info failed!", e);
                    }
                    return true;
                }
                clearAppCacheAndData(pm, currentCrashAppName);
                Slog.w("RescuePartyPlus", "Delete Top UI App cache and data: " + currentCrashAppName);
                return false;
            default:
                clearAppCacheAndData(pm, currentCrashAppName);
                if (!currentCrashAppName.equals(RescuePartyPlusHelper.getLauncherPackageName(context))) {
                    if (RescuePartyPlusHelper.checkPackageIsTOPUI(currentCrashAppName)) {
                        Slog.w("RescuePartyPlus", "Delete Top UI App cache and data: " + currentCrashAppName);
                        return false;
                    }
                    Slog.w("RescuePartyPlus", "Disable App restart, than clear app cache and data: " + currentCrashAppName);
                    RescuePartyPlusHelper.disableAppRestart(currentCrashAppName);
                    if (RescuePartyPlusHelper.enableDebugStatus()) {
                        MiuiWarnings.getInstance().showWarningDialog(currentCrashAppName, new MiuiWarnings.WarningCallback() { // from class: com.android.server.PackageWatchdogImpl.1
                            @Override // com.android.server.am.MiuiWarnings.WarningCallback
                            public void onCallback(boolean positive) {
                                if (positive) {
                                    Slog.w("RescuePartyPlus", "Check app crash " + currentCrashAppName + " - OK");
                                } else {
                                    Slog.w("RescuePartyPlus", "Check app crash " + currentCrashAppName + " - Cancel");
                                }
                            }
                        });
                    }
                }
                return true;
        }
    }

    public void maybeShowRecoveryTip(final Context context) {
        if (!RescuePartyPlusHelper.checkDisableRescuePartyPlus() && !RescuePartyPlusHelper.getConfigResetProcessStatus() && RescuePartyPlusHelper.getLastResetConfigStatus() && !RescuePartyPlusHelper.getShowResetConfigUIStatus()) {
            RescuePartyPlusHelper.setLastResetConfigStatus(false);
            new Thread(new Runnable() { // from class: com.android.server.PackageWatchdogImpl$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    PackageWatchdogImpl.lambda$maybeShowRecoveryTip$0(context);
                }
            }).start();
        }
    }

    public static /* synthetic */ void lambda$maybeShowRecoveryTip$0(Context context) {
        Slog.w("RescuePartyPlus", "ShowTipsUI Start");
        while (true) {
            if (RescuePartyPlusHelper.checkBootCompletedStatus() && !RescuePartyPlusHelper.getShowResetConfigUIStatus()) {
                Slog.w("RescuePartyPlus", "Show RescueParty Plus Tips UI Ready!!!");
                Uri uri = Uri.parse(context.getString(286196460));
                Intent intent = new Intent("android.intent.action.VIEW", uri);
                NotificationManager notificationManager = (NotificationManager) context.getSystemService("notification");
                PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, BroadcastQueueImpl.FLAG_IMMUTABLE);
                Notification notification = new Notification.Builder(context, SystemNotificationChannels.DEVELOPER_IMPORTANT).setSmallIcon(17301624).setCategory("sys").setShowWhen(true).setContentTitle(context.getString(286196459)).setContentText(context.getString(286196458)).setContentIntent(pendingIntent).setOngoing(true).setPriority(5).setDefaults(-1).setVisibility(1).setAutoCancel(true).build();
                notificationManager.notify("RescueParty", "RescueParty".hashCode(), notification);
                RescuePartyPlusHelper.setShowResetConfigUIStatus(true);
                return;
            }
            try {
                Thread.sleep(10000L);
            } catch (InterruptedException e) {
            }
        }
    }

    public void recordMitigationCount(int mitigationCount) {
        if (RescuePartyPlusHelper.checkDisableRescuePartyPlus()) {
            return;
        }
        Slog.w("RescuePartyPlus", "note SystemServer crash: " + mitigationCount);
        RescuePartyPlusHelper.setMitigationTempCount(mitigationCount);
    }
}
