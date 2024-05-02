package com.miui.server.enterprise;

import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.PendingIntent;
import android.app.admin.IDevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageDataObserver;
import android.content.pm.IPackageDeleteObserver;
import android.content.pm.PackageManager;
import android.content.pm.ParceledListSlice;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Slog;
import com.android.server.am.ActivityManagerService;
import com.android.server.am.ProcessManagerService;
import com.android.server.pm.PackageManagerService;
import com.miui.enterprise.ApplicationHelper;
import com.miui.enterprise.IApplicationManager;
import com.miui.enterprise.sdk.IEpDeletePackageObserver;
import com.miui.enterprise.sdk.IEpInstallPackageObserver;
import com.miui.enterprise.settings.EnterpriseSettings;
import com.miui.server.smartpower.SmartPowerPolicyManager;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import miui.process.ProcessManager;
import miui.security.AppRunningControlManager;
import miui.util.NotificationFilterHelper;
/* loaded from: classes.dex */
public class ApplicationManagerService extends IApplicationManager.Stub {
    private static final String ACTION_APP_RUNNING_BLOCK = "com.miui.securitycore.APP_RUNNING_BLOCK";
    private static final String PACKAGE_SECURITY_CORE = "com.miui.securitycore";
    private static final String TAG = "Enterprise-App";
    private AppOpsManager mAppOpsManager;
    private Context mContext;
    private Intent mDisAllowRunningHandleIntent;
    private PackageManagerService.IPackageManagerImpl mPMS = ServiceManager.getService("package");
    private ActivityManagerService mAMS = ServiceManager.getService("activity");
    private ProcessManagerService mProcessManagerService = (ProcessManagerService) ServiceManager.getService("ProcessManager");
    private IDevicePolicyManager mDevicePolicyManager = IDevicePolicyManager.Stub.asInterface(ServiceManager.getService("device_policy"));

    public ApplicationManagerService(Context context) {
        this.mContext = context;
        this.mAppOpsManager = (AppOpsManager) this.mContext.getSystemService("appops");
        Intent intent = new Intent(ACTION_APP_RUNNING_BLOCK);
        this.mDisAllowRunningHandleIntent = intent;
        intent.setPackage(PACKAGE_SECURITY_CORE);
        this.mDisAllowRunningHandleIntent.setFlags(276824064);
    }

    public void bootComplete() {
        Slog.d(TAG, "ApplicationManagerService init");
        restoreAppRunningControl(0);
    }

    private void restoreAppRunningControl(int userId) {
        List<String> blackList = getDisallowedRunningAppList(userId);
        if (blackList == null || blackList.size() == 0) {
            AppRunningControlManager.getInstance().setBlackListEnable(false);
            return;
        }
        AppRunningControlManager.getInstance().setBlackListEnable(true);
        AppRunningControlManager.getInstance().setDisallowRunningList(blackList, this.mDisAllowRunningHandleIntent);
    }

    public void installPackage(String path, int flags, IEpInstallPackageObserver observer, int userId) {
        ServiceUtils.checkPermission(this.mContext);
        Slog.d(TAG, "install package " + path);
        ApplicationManagerServiceProxy.installPackageAsUser(this.mContext, this.mPMS, path, observer, flags, "Enterprise", userId);
    }

    public void deletePackage(String packageName, int flags, final IEpDeletePackageObserver observer, int userId) {
        ServiceUtils.checkPermission(this.mContext);
        Slog.d(TAG, "delete package " + packageName);
        IPackageDeleteObserver deleteObserver = new IPackageDeleteObserver.Stub() { // from class: com.miui.server.enterprise.ApplicationManagerService.1
            public void packageDeleted(String packageName2, int returnCode) throws RemoteException {
                observer.onPackageDeleted(packageName2, returnCode);
            }
        };
        this.mContext.getPackageManager().deletePackageAsUser(packageName, deleteObserver, flags, userId);
    }

    public void installPackageWithPendingIntent(String path, final PendingIntent pendingIntent, final int userId) {
        ServiceUtils.checkPermission(this.mContext);
        ApplicationManagerServiceProxy.installPackageAsUser(this.mContext, this.mPMS, path, new IEpInstallPackageObserver.Stub() { // from class: com.miui.server.enterprise.ApplicationManagerService.2
            public void onPackageInstalled(final String basePackageName, int returnCode, String msg, Bundle extras) {
                if (returnCode != 1) {
                    Slog.e(ApplicationManagerService.TAG, "Failed to install package: " + basePackageName + ", returnCode: " + returnCode + ", msg: " + msg);
                } else {
                    new Thread(new Runnable() { // from class: com.miui.server.enterprise.ApplicationManagerService.2.1
                        @Override // java.lang.Runnable
                        public void run() {
                            for (int i = 0; i < 5; i++) {
                                try {
                                    ApplicationManagerService.this.mPMS.checkPackageStartable(basePackageName, userId);
                                } catch (SecurityException e) {
                                    Slog.d(ApplicationManagerService.TAG, "Package " + basePackageName + " is still frozen");
                                    try {
                                        Thread.sleep(1000L);
                                    } catch (InterruptedException e2) {
                                    }
                                }
                            }
                            try {
                                pendingIntent.send();
                                Slog.d(ApplicationManagerService.TAG, "Send pending intent: " + pendingIntent);
                            } catch (PendingIntent.CanceledException e3) {
                                Slog.e(ApplicationManagerService.TAG, "Failed to send pending intent", e3);
                            }
                        }
                    }).start();
                }
            }
        }, 2, "Enterprise", userId);
    }

    public void setApplicationSettings(String packageName, int flags, int userId) {
        String str;
        ServiceUtils.checkPermission(this.mContext);
        if (TextUtils.isEmpty(packageName) || flags < 0) {
            Slog.e(TAG, "Invalidate param packageName:" + packageName + ", flags:" + flags);
            return;
        }
        EnterpriseSettings.putInt(this.mContext, ApplicationHelper.buildPackageSettingKey(packageName), flags, userId);
        int i = 3;
        boolean z = true;
        if ((flags & 8) != 0) {
            Slog.d(TAG, "allowed " + packageName + " auto start");
            Bundle extras = new Bundle();
            extras.putLong("extra_permission", 16384L);
            extras.putInt("extra_action", 3);
            extras.putStringArray("extra_package", new String[]{packageName});
            this.mContext.getContentResolver().call(Uri.parse("content://com.lbe.security.miui.permmgr"), "6", (String) null, extras);
            str = null;
        } else {
            Bundle extras2 = new Bundle();
            extras2.putLong("extra_permission", 16384L);
            extras2.putInt("extra_action", 0);
            extras2.putStringArray("extra_package", new String[]{packageName});
            str = null;
            this.mContext.getContentResolver().call(Uri.parse("content://com.lbe.security.miui.permmgr"), "6", (String) null, extras2);
        }
        Intent intent = new Intent("android.intent.action.PACKAGE_ADDED", Uri.fromParts("package", packageName, str));
        intent.putExtra("android.intent.extra.user_handle", userId);
        intent.setPackage("com.lbe.security.miui");
        intent.addFlags(268435456);
        this.mContext.sendBroadcastAsUser(intent, new UserHandle(userId));
        ApplicationInfo info = this.mPMS.getApplicationInfo(packageName, 0L, userId);
        boolean shouldGrantPermission = (flags & 16) != 0;
        if (info != null) {
            if (shouldGrantPermission) {
                i = 0;
            }
            int opsMode = i;
            this.mAppOpsManager.setMode(43, info.uid, packageName, opsMode);
            this.mAppOpsManager.setMode(10022, info.uid, packageName, opsMode);
        }
        Bundle bundle = new Bundle();
        bundle.putInt("userId", userId);
        bundle.putString("pkgName", packageName);
        bundle.putString("bgControl", (flags & 1) != 0 ? "noRestrict" : "miuiAuto");
        try {
            this.mContext.getContentResolver().call(Uri.withAppendedPath(Uri.parse("content://com.miui.powerkeeper.configure"), SmartPowerPolicyManager.PowerSavingStrategyControl.DB_TABLE), "userTableupdate", (String) null, bundle);
        } catch (IllegalArgumentException e) {
            Slog.e(TAG, "Failed to process powerkeeper config for pkg " + packageName, e);
        }
        if ((flags & 1) == 0) {
            z = false;
        }
        ProcessManager.updateApplicationLockedState(packageName, userId, z);
        List<String> whiteList = this.mProcessManagerService.getProcessPolicy().getWhiteList(4096);
        boolean modified = false;
        if ((flags & 1) != 0) {
            if (!whiteList.contains(packageName)) {
                whiteList.add(packageName);
                modified = true;
            }
        } else if (whiteList.contains(packageName)) {
            whiteList.remove(packageName);
            modified = true;
        }
        if (modified) {
            this.mProcessManagerService.getProcessPolicy().addWhiteList(4096, whiteList, false);
        }
    }

    public int getApplicationSettings(String packageName, int userId) {
        ServiceUtils.checkPermission(this.mContext);
        return EnterpriseSettings.getInt(this.mContext, ApplicationHelper.buildPackageSettingKey(packageName), 0, userId);
    }

    public boolean setDeviceAdmin(ComponentName component, int userId) {
        ServiceUtils.checkPermission(this.mContext);
        try {
            this.mDevicePolicyManager.setActiveAdmin(component, true, userId);
            Slog.d(TAG, "Add device admin[" + component + "]");
            return true;
        } catch (RemoteException e) {
            Slog.d(TAG, "Add device admin[" + component + "] failed", e);
            return false;
        }
    }

    public boolean removeDeviceAdmin(ComponentName component, int userId) {
        ServiceUtils.checkPermission(this.mContext);
        try {
            this.mDevicePolicyManager.removeActiveAdmin(component, userId);
            Slog.d(TAG, "Remove device admin[" + component + "]");
            return true;
        } catch (Exception e) {
            Slog.d(TAG, "Remove device admin[" + component + "] failed", e);
            return false;
        }
    }

    public void setApplicationBlackList(List<String> packages, int userId) {
        ServiceUtils.checkPermission(this.mContext);
        EnterpriseSettings.putString(this.mContext, "ep_app_black_list", EnterpriseSettings.generateListSettings(packages), userId);
    }

    public List<String> getApplicationBlackList(int userId) {
        ServiceUtils.checkPermission(this.mContext);
        String savedStr = EnterpriseSettings.getString(this.mContext, "ep_app_black_list", userId);
        return EnterpriseSettings.parseListSettings(savedStr);
    }

    public void setApplicationWhiteList(List<String> packages, int userId) {
        ServiceUtils.checkPermission(this.mContext);
        EnterpriseSettings.putString(this.mContext, "ep_app_white_list", EnterpriseSettings.generateListSettings(packages), userId);
    }

    public List<String> getApplicationWhiteList(int userId) {
        ServiceUtils.checkPermission(this.mContext);
        String savedStr = EnterpriseSettings.getString(this.mContext, "ep_app_white_list", userId);
        return EnterpriseSettings.parseListSettings(savedStr);
    }

    public void setApplicationRestriction(int mode, int userId) {
        ServiceUtils.checkPermission(this.mContext);
        EnterpriseSettings.putInt(this.mContext, "ep_app_restriction_mode", mode, userId);
    }

    public int getApplicationRestriction(int userId) {
        ServiceUtils.checkPermission(this.mContext);
        return EnterpriseSettings.getInt(this.mContext, "ep_app_restriction_mode", 0, userId);
    }

    public void setDisallowedRunningAppList(List<String> packages, int userId) {
        ServiceUtils.checkPermission(this.mContext);
        StringBuilder sb = new StringBuilder();
        if (packages == null) {
            packages = new ArrayList();
        }
        forceCloseTask(packages, userId);
        for (String pkg : packages) {
            sb.append(pkg).append(";");
            this.mAMS.forceStopPackage(pkg, userId);
        }
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        EnterpriseSettings.putString(this.mContext, "ep_app_disallow_running_list", sb.toString(), userId);
        if (packages.isEmpty()) {
            AppRunningControlManager.getInstance().setBlackListEnable(false);
            return;
        }
        AppRunningControlManager.getInstance().setDisallowRunningList(packages, this.mDisAllowRunningHandleIntent);
        AppRunningControlManager.getInstance().setBlackListEnable(true);
    }

    public List<String> getDisallowedRunningAppList(int userId) {
        ServiceUtils.checkPermission(this.mContext);
        return EnterpriseSettings.parseListSettings(EnterpriseSettings.getString(this.mContext, "ep_app_disallow_running_list", userId));
    }

    private void forceCloseTask(List<String> packages, int userId) {
        PackageManager pm = this.mContext.getPackageManager();
        ParceledListSlice<ActivityManager.RecentTaskInfo> slice = this.mAMS.getRecentTasks(1001, 0, userId);
        List<ActivityManager.RecentTaskInfo> tasks = null;
        if (slice != null) {
            tasks = slice.getList();
        }
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        for (ActivityManager.RecentTaskInfo info : tasks) {
            ResolveInfo ri = getResolveInfoFromTask(pm, info);
            if (ri != null && ri.activityInfo.packageName != null) {
                String packageName = ri.activityInfo.packageName;
                if (packages.contains(packageName)) {
                    this.mAMS.removeTask(info.persistentId);
                }
            }
        }
    }

    private ResolveInfo getResolveInfoFromTask(PackageManager packageManager, ActivityManager.RecentTaskInfo recentInfo) {
        Intent intent = new Intent(recentInfo.baseIntent);
        if (recentInfo.origActivity != null) {
            intent.setComponent(recentInfo.origActivity);
        }
        intent.setFlags((intent.getFlags() & (-2097153)) | 268435456);
        return packageManager.resolveActivity(intent, 0);
    }

    public void killProcess(String packageName, int userId) {
        ServiceUtils.checkPermission(this.mContext);
        this.mAMS.forceStopPackage(packageName, userId);
    }

    public void enableAccessibilityService(ComponentName componentName, boolean enabled) {
        ServiceUtils.checkPermission(this.mContext);
        Set<ComponentName> services = getAccessibilityServiceFromPackage(componentName.getPackageName());
        Set<ComponentName> enabledServices = readEnabeledAccessibilityService();
        if (enabledServices == null) {
            enabledServices = new HashSet();
        }
        if (enabled) {
            enabledServices.addAll(services);
        } else {
            enabledServices.removeAll(services);
        }
        StringBuilder enabledServicesBuilder = new StringBuilder();
        for (ComponentName enabledService : enabledServices) {
            enabledServicesBuilder.append(enabledService.flattenToString());
            enabledServicesBuilder.append(':');
        }
        int enabledServicesBuilderLength = enabledServicesBuilder.length();
        if (enabledServicesBuilderLength > 0) {
            enabledServicesBuilder.deleteCharAt(enabledServicesBuilderLength - 1);
        }
        Settings.Secure.putString(this.mContext.getContentResolver(), "enabled_accessibility_services", enabledServicesBuilder.toString());
        Settings.Secure.putInt(this.mContext.getContentResolver(), "accessibility_enabled", !enabledServices.isEmpty() ? 1 : 0);
    }

    private Set<ComponentName> getAccessibilityServiceFromPackage(String pkgName) {
        Set<ComponentName> services = new HashSet<>();
        Intent accessibilityIntent = new Intent("android.accessibilityservice.AccessibilityService");
        accessibilityIntent.setPackage(pkgName);
        List<ResolveInfo> resolveInfos = this.mContext.getPackageManager().queryIntentServices(accessibilityIntent, 4);
        for (ResolveInfo resolveInfo : resolveInfos) {
            if (resolveInfo.serviceInfo != null) {
                services.add(new ComponentName(resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name));
            }
        }
        return services;
    }

    private Set<ComponentName> readEnabeledAccessibilityService() {
        String enabledServicesSetting = Settings.Secure.getString(this.mContext.getContentResolver(), "enabled_accessibility_services");
        if (enabledServicesSetting == null) {
            return null;
        }
        Set<ComponentName> enabledServices = new HashSet<>();
        TextUtils.SimpleStringSplitter stringSplitter = new TextUtils.SimpleStringSplitter(':');
        stringSplitter.setString(enabledServicesSetting);
        while (stringSplitter.hasNext()) {
            String enabledServiceName = stringSplitter.next();
            ComponentName enabledComponent = ComponentName.unflattenFromString(enabledServiceName);
            if (enabledComponent != null) {
                enabledServices.add(enabledComponent);
            }
        }
        return enabledServices;
    }

    public void clearApplicationUserData(String packageName, int userId) {
        ServiceUtils.checkPermission(this.mContext);
        this.mPMS.clearApplicationUserData(packageName, (IPackageDataObserver) null, userId);
    }

    public void clearApplicationCache(String packageName, int userId) {
        ServiceUtils.checkPermission(this.mContext);
        this.mPMS.deleteApplicationCacheFilesAsUser(packageName, userId, (IPackageDataObserver) null);
    }

    public void addTrustedAppStore(List<String> packages, int userId) {
        ServiceUtils.checkPermission(this.mContext);
        EnterpriseSettings.putString(this.mContext, "ep_trusted_app_stores", EnterpriseSettings.generateListSettings(packages), userId);
    }

    public List<String> getTrustedAppStore(int userId) {
        ServiceUtils.checkPermission(this.mContext);
        return EnterpriseSettings.parseListSettings(EnterpriseSettings.getString(this.mContext, "ep_trusted_app_stores", userId));
    }

    public void enableTrustedAppStore(boolean enabled, int userId) {
        ServiceUtils.checkPermission(this.mContext);
        EnterpriseSettings.putInt(this.mContext, "ep_trusted_app_store_enabled", enabled ? 1 : 0, userId);
    }

    public boolean isTrustedAppStoreEnabled(int userId) {
        ServiceUtils.checkPermission(this.mContext);
        return EnterpriseSettings.getInt(this.mContext, "ep_trusted_app_store_enabled", 0, userId) == 1;
    }

    public void setApplicationEnabled(String packageName, boolean enable, int userId) {
        ServiceUtils.checkPermission(this.mContext);
        this.mPMS.setApplicationEnabledSetting(packageName, enable ? 0 : 2, 0, userId, "Enterprise");
    }

    public void enableNotifications(String pkg, boolean enabled) {
        ServiceUtils.checkPermission(this.mContext);
        NotificationFilterHelper.enableNotifications(this.mContext, pkg, enabled);
    }

    public void setNotificaitonFilter(String pkg, String channelId, String type, boolean allow) {
        ServiceUtils.checkPermission(this.mContext);
        if (TextUtils.isEmpty(channelId)) {
            if ("float".equals(type)) {
                NotificationFilterHelper.enableStatusIcon(this.mContext, pkg, allow);
            } else {
                NotificationFilterHelper.setAllow(this.mContext, pkg, type, allow);
            }
        } else if ("float".equals(type)) {
            NotificationFilterHelper.enableStatusIcon(this.mContext, pkg, channelId, allow);
        } else {
            NotificationFilterHelper.setAllow(this.mContext, pkg, channelId, type, allow);
        }
    }

    public void setXSpaceBlack(List<String> packages) {
        ServiceUtils.checkPermission(this.mContext);
        EnterpriseSettings.putString(this.mContext, "ep_app_black_xsapce", EnterpriseSettings.generateListSettings(packages));
    }

    public List<String> getXSpaceBlack() {
        ServiceUtils.checkPermission(this.mContext);
        String savedStr = EnterpriseSettings.getString(this.mContext, "ep_app_black_xsapce");
        return EnterpriseSettings.parseListSettings(savedStr);
    }

    public void grantRuntimePermission(String packageName, String permission, int userId) {
        ServiceUtils.checkPermission(this.mContext);
        PackageManager pm = this.mContext.getPackageManager();
        boolean grantPermission = pm.addWhitelistedRestrictedPermission(packageName, permission, 1);
        Slog.d(TAG, "add restricted permission white list:" + permission + ",success?=" + grantPermission);
        this.mContext.getPackageManager().grantRuntimePermission(packageName, permission, UserHandle.OWNER);
    }
}
