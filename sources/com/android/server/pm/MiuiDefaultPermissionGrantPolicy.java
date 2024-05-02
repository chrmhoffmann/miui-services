package com.android.server.pm;

import android.app.AppGlobals;
import android.content.pm.PackageInfo;
import android.content.pm.PermissionInfo;
import android.miui.AppOpsUtils;
import android.os.Build;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import com.android.server.LocalServices;
import com.android.server.MiuiBatteryStatsService;
import com.android.server.inputmethod.InputMethodManagerServiceImpl;
import com.android.server.pm.permission.DefaultPermissionGrantPolicyStub;
import com.android.server.pm.permission.PermissionManagerService;
import com.android.server.pm.permission.PermissionManagerServiceInternal;
import com.miui.base.MiuiStubRegistry;
import com.miui.server.AccessController;
import com.miui.server.GreenGuardManagerService;
import com.miui.server.SplashScreenServiceDelegate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
/* loaded from: classes.dex */
public class MiuiDefaultPermissionGrantPolicy extends DefaultPermissionGrantPolicyStub {
    private static final int DEFAULT_PACKAGE_INFO_QUERY_FLAGS = 536916096;
    private static final String GRANT_RUNTIME_VERSION = "persist.sys.grant_version";
    private static final String REQUIRED_PERMISSIONS = "required_permissions";
    private static final String RUNTIME_PERMSSION_PROPTERY = "persist.sys.runtime_perm";
    private static final int STATE_DEF = -1;
    private static final int STATE_GRANT = 0;
    private static final int STATE_REVOKE = 1;
    private static final String TAG = "DefaultPermGrantPolicyI";
    private static final Set<String> RUNTIME_PERMISSIONS = new ArraySet();
    private static final String INCALL_UI = "com.android.incallui";
    public static final String[] MIUI_SYSTEM_APPS = {"com.android.bips", "com.android.fileexplorer", "com.android.calendar", "com.android.browser", AccessController.PACKAGE_CAMERA, "com.android.mms", "com.xiaomi.xmsf", "com.android.quicksearchbox", InputMethodManagerServiceImpl.MIUI_HOME, "com.miui.securityadd", "com.miui.guardprovider", "com.android.providers.downloads", "com.android.providers.downloads.ui", "com.miui.cloudservice", INCALL_UI, "com.xiaomi.account", "com.android.contacts", "com.miui.cloudbackup", "com.xiaomi.payment", AccessController.PACKAGE_GALLERY, MiuiBatteryStatsService.TrackBatteryUsbInfo.ANALYTICS_PACKAGE, SplashScreenServiceDelegate.SPLASHSCREEN_PACKAGE, "com.xiaomi.metoknlp", "com.android.htmlviewer", "com.xiaomi.simactivate.service", "com.miui.extraphoto", "com.miui.packageinstaller", "com.miui.hybrid", "com.miui.voiceassist", "com.miui.mishare.connectivity", "com.miui.dmregservice", "com.miui.video", "com.miui.player", "com.xiaomi.market", "com.xiaomi.gamecenter.sdk.service", "com.mipay.wallet", "com.miui.tsmclient", "com.miui.yellowpage", "com.xiaomi.mircs", "com.milink.service", "com.miui.smsextra", "com.miui.contentextension", "com.miui.personalassistant", GreenGuardManagerService.GREEN_KID_AGENT_PKG_NAME, "com.mobiletools.systemhelper", "com.miui.fm", "com.xiaomi.miplay_client", "com.miui.accessibility"};
    private static final String[] MIUI_GLOBAL_APPS = {"co.sitic.pp", "com.miui.backup", "com.xiaomi.finddevice"};
    private static final ArrayMap<String, ArrayList<String>> sMiuiAppDefaultGrantedPermissions = new ArrayMap<>();

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MiuiDefaultPermissionGrantPolicy> {

        /* compiled from: MiuiDefaultPermissionGrantPolicy$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MiuiDefaultPermissionGrantPolicy INSTANCE = new MiuiDefaultPermissionGrantPolicy();
        }

        public MiuiDefaultPermissionGrantPolicy provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public MiuiDefaultPermissionGrantPolicy provideNewInstance() {
            return new MiuiDefaultPermissionGrantPolicy();
        }
    }

    private static boolean isDangerousPermission(String permission) {
        ensureDangerousSetInit();
        return RUNTIME_PERMISSIONS.contains(permission);
    }

    private static synchronized void ensureDangerousSetInit() {
        synchronized (MiuiDefaultPermissionGrantPolicy.class) {
            if (RUNTIME_PERMISSIONS.size() > 0) {
                return;
            }
            PermissionManagerServiceInternal pm = (PermissionManagerServiceInternal) LocalServices.getService(PermissionManagerServiceInternal.class);
            ArrayList<PermissionInfo> dangerous = pm.getAllPermissionsWithProtection(1);
            Iterator<PermissionInfo> it = dangerous.iterator();
            while (it.hasNext()) {
                PermissionInfo permissionInfo = it.next();
                RUNTIME_PERMISSIONS.add(permissionInfo.name);
            }
        }
    }

    private static void grantPermissionsForCTS(int userId) {
        String miuiCustomizedRegion = SystemProperties.get("ro.miui.customized.region", "");
        if ("lm_cr".equals(miuiCustomizedRegion) || "mx_telcel".equals(miuiCustomizedRegion)) {
            PermissionManagerService permissionService = AppGlobals.getPermissionManager();
            permissionService.updatePermissionFlags("co.sitic.pp", "android.permission.READ_PHONE_STATE", 2, 2, true, userId);
            permissionService.updatePermissionFlags("co.sitic.pp", "android.permission.RECEIVE_SMS", 2, 2, true, userId);
            permissionService.updatePermissionFlags("co.sitic.pp", "android.permission.CALL_PHONE", 2, 2, true, userId);
            Log.i(TAG, "grant permissions for Sysdll because of CTS");
        }
    }

    public void grantDefaultPermissions(int userId) {
        String[] strArr;
        if (AppOpsUtils.isXOptMode() || (userId == 0 && TextUtils.equals(Build.VERSION.INCREMENTAL, SystemProperties.get(GRANT_RUNTIME_VERSION)))) {
            Log.i(TAG, "Don't need grant default permission to apps");
            if (AppOpsUtils.isXOptMode()) {
                grantPermissionsForCTS(userId);
                return;
            }
            return;
        }
        long startTime = System.currentTimeMillis();
        PackageManagerService service = PackageManagerServiceStub.get().getService();
        if (miui.os.Build.IS_INTERNATIONAL_BUILD) {
            for (String miuiGlobalApp : MIUI_GLOBAL_APPS) {
                grantRuntimePermissionsLPw(service, miuiGlobalApp, false, userId);
            }
            Log.i(TAG, "grant permissions for miui global apps");
        } else {
            realGrantDefaultPermissions(service, userId);
        }
        SystemProperties.set(GRANT_RUNTIME_VERSION, Build.VERSION.INCREMENTAL);
        Log.i(TAG, "grantDefaultPermissions cost " + (System.currentTimeMillis() - startTime) + " ms");
    }

    private static void realGrantDefaultPermissions(PackageManagerService service, int userId) {
        String[] strArr;
        for (String miuiSystemApp : MIUI_SYSTEM_APPS) {
            grantRuntimePermissionsLPw(service, miuiSystemApp, true, userId);
        }
    }

    private static boolean doesPackageSupportRuntimePermissions(PackageInfo pkg) {
        return pkg.applicationInfo.targetSdkVersion > 22;
    }

    /* JADX WARN: Code restructure failed: missing block: B:73:0x01c1, code lost:
        if ("android.permission.POST_NOTIFICATIONS".equals(r7) == false) goto L77;
     */
    /* JADX WARN: Removed duplicated region for block: B:81:0x01ee  */
    /* JADX WARN: Removed duplicated region for block: B:82:0x0203  */
    /* JADX WARN: Removed duplicated region for block: B:85:0x020d  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private static void grantRuntimePermissionsLPw(com.android.server.pm.PackageManagerService r24, java.lang.String r25, boolean r26, int r27) {
        /*
            Method dump skipped, instructions count: 589
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.pm.MiuiDefaultPermissionGrantPolicy.grantRuntimePermissionsLPw(com.android.server.pm.PackageManagerService, java.lang.String, boolean, int):void");
    }

    private static boolean isUserChanged(int flags) {
        return (flags & 3) != 0;
    }

    private static boolean isOTAUpdated(int flags) {
        return ((flags & 2) == 0 || (flags & 32) == 0) ? false : true;
    }

    public static void setCoreRuntimePermissionEnabled(boolean grant, int flags, int userId) {
        if (userId != 0) {
            return;
        }
        PackageManagerService service = PackageManagerServiceStub.get().getService();
        if (!grant) {
            SystemProperties.set(RUNTIME_PERMSSION_PROPTERY, String.valueOf(1));
            return;
        }
        realGrantDefaultPermissions(service, userId);
        SystemProperties.set(RUNTIME_PERMSSION_PROPTERY, String.valueOf(0));
    }

    public static void grantRuntimePermission(String packageName, int userId) {
        PackageManagerService service = PackageManagerServiceStub.get().getService();
        if (INCALL_UI.equals(packageName)) {
            grantIncallUiPermission(service, userId);
        }
    }

    private static void grantIncallUiPermission(PackageManagerService service, int userId) {
        ArrayList<String> perms = new ArrayList<>();
        perms.add("android.permission.RECORD_AUDIO");
        perms.add("android.permission.WRITE_EXTERNAL_STORAGE");
        perms.add("android.permission.READ_EXTERNAL_STORAGE");
        perms.add("android.permission.READ_CONTACTS");
        perms.add("android.permission.BLUETOOTH_CONNECT");
        perms.add("android.permission.POST_NOTIFICATIONS");
        perms.add("android.permission.READ_MEDIA_AUDIO");
        PermissionManagerService permissionService = AppGlobals.getPermissionManager();
        Iterator<String> it = perms.iterator();
        while (it.hasNext()) {
            String p = it.next();
            int result = service.checkPermission(p, INCALL_UI, userId);
            if (result == -1) {
                permissionService.grantRuntimePermission(INCALL_UI, p, userId);
            }
        }
    }

    public static void revokeAllPermssions() {
        if (!AppOpsUtils.isXOptMode()) {
            return;
        }
        SystemProperties.set(GRANT_RUNTIME_VERSION, "");
        try {
            PermissionManagerService permissionManagerService = AppGlobals.getPermissionManager();
            for (String pkg : sMiuiAppDefaultGrantedPermissions.keySet()) {
                ArrayList<String> permissions = sMiuiAppDefaultGrantedPermissions.get(pkg);
                if (!"com.google.android.packageinstaller".equals(pkg) && permissions != null) {
                    Iterator<String> it = permissions.iterator();
                    while (it.hasNext()) {
                        String p = it.next();
                        if (!"com.google.android.gms".equals(pkg) || (!"android.permission.RECORD_AUDIO".equals(p) && !"android.permission.ACCESS_FINE_LOCATION".equals(p))) {
                            try {
                                permissionManagerService.revokeRuntimePermission(pkg, p, 0, "revokeMiuiOpt");
                            } catch (Exception e) {
                                Log.d(TAG, "revokeAllPermssions error:" + e.toString());
                            }
                        }
                    }
                }
            }
            ArrayList<String> permissionList = new ArrayList<>();
            permissionList.add("android.permission.READ_EXTERNAL_STORAGE");
            permissionList.add("android.permission.WRITE_EXTERNAL_STORAGE");
            Iterator<String> it2 = permissionList.iterator();
            while (it2.hasNext()) {
                String permItem = it2.next();
                try {
                    permissionManagerService.revokeRuntimePermission("com.miui.packageinstaller", permItem, 0, "revokeMiuiOpt");
                } catch (Exception e2) {
                    Log.d(TAG, "revokeRuntimePermissionInternal error:" + e2.toString());
                }
            }
        } catch (Exception e3) {
            e3.printStackTrace();
        }
    }

    public static void grantMiuiPackageInstallerPermssions() {
        List<String> permissionList = new ArrayList<>();
        permissionList.add("android.permission.READ_EXTERNAL_STORAGE");
        permissionList.add("android.permission.WRITE_EXTERNAL_STORAGE");
        permissionList.add("android.permission.READ_PHONE_STATE");
        for (String permItem : permissionList) {
            try {
                PermissionManagerService permissionService = AppGlobals.getPermissionManager();
                permissionService.grantRuntimePermission("com.miui.packageinstaller", permItem, 0);
            } catch (Exception e) {
                Log.d(TAG, "grantMiuiPackageInstallerPermssions error:" + e.toString());
            }
        }
    }

    public boolean isSpecialUidNeedDefaultGrant(PackageInfo info) {
        PackageInfo sharedMetaInfo;
        if (UserHandle.getAppId(info.applicationInfo.uid) <= 2000 || UserHandle.getAppId(info.applicationInfo.uid) >= 10000) {
            return true;
        }
        PackageManagerService service = PackageManagerServiceStub.get().getService();
        int userId = UserHandle.getUserId(info.applicationInfo.uid);
        PackageInfo metaInfo = service.snapshotComputer().getPackageInfo(info.packageName, 128L, userId);
        if (metaInfo == null) {
            return true;
        }
        if (TextUtils.isEmpty(metaInfo.sharedUserId)) {
            return isAdaptedRequiredPermissions(metaInfo);
        }
        String[] sharedPackages = service.snapshotComputer().getPackagesForUid(info.applicationInfo.uid);
        if (sharedPackages == null) {
            return true;
        }
        for (String sharedPackage : sharedPackages) {
            if (TextUtils.equals(info.packageName, sharedPackage)) {
                sharedMetaInfo = metaInfo;
            } else {
                sharedMetaInfo = service.snapshotComputer().getPackageInfo(sharedPackage, 128L, userId);
            }
            if (!isAdaptedRequiredPermissions(sharedMetaInfo)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAdaptedRequiredPermissions(PackageInfo pi) {
        if (pi == null || pi.applicationInfo == null || pi.applicationInfo.metaData == null) {
            return false;
        }
        String permission = pi.applicationInfo.metaData.getString(REQUIRED_PERMISSIONS);
        return !TextUtils.isEmpty(permission);
    }
}
