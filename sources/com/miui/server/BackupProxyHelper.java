package com.miui.server;

import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.ServiceInfo;
import android.os.RemoteException;
import android.text.TextUtils;
/* loaded from: classes.dex */
public class BackupProxyHelper {
    public static boolean isMiPushRequired(IPackageManager pm, String pkgName) {
        String[] permissions;
        ServiceInfo[] services;
        try {
            PackageInfo pkgInfo = pm.getPackageInfo(pkgName, 4100L, 0);
            if (pkgInfo == null || (permissions = pkgInfo.requestedPermissions) == null) {
                return false;
            }
            String needPermission = pkgName + ".permission.MIPUSH_RECEIVE";
            int permissionIndex = 0;
            while (permissionIndex < permissions.length && !TextUtils.equals(needPermission, permissions[permissionIndex])) {
                permissionIndex++;
            }
            if (permissionIndex >= permissions.length || (services = pkgInfo.services) == null) {
                return false;
            }
            int serviceIndex = 0;
            while (serviceIndex < services.length && !TextUtils.equals(services[serviceIndex].name, "com.xiaomi.mipush.sdk.PushMessageHandler")) {
                serviceIndex++;
            }
            return serviceIndex < services.length;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isAppInXSpace(IPackageManager pm, String pkgName) {
        try {
            PackageInfo pkgInfo = pm.getPackageInfo(pkgName, 0L, 999);
            return pkgInfo != null;
        } catch (RemoteException e) {
            return false;
        }
    }
}
