package com.miui.server.migard.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
/* loaded from: classes.dex */
public final class PackageUtils {
    private static final String TAG = PackageUtils.class.getSimpleName();
    private static final String[] ANDROID_IDS_INFO = {"system", "radio", "bluetooth", "graphics", "input", "audio", "camera", "log", "compass", "mount", "wifi", "adb", "install", "media", "dhcp", "sdcard_rw", "vpn", "keystore", "usb", "drm", "mdnsr", "gps", null, "media_rw", "mtp", null, "drmrpc", "nfc", "sdcard_r", "clat", "loop_radio", "mediadrm", "package_info", "sdcard_pics", "sdcard_av", "sdcard_all", "logd", "shared_relro", "dbus", "tlsdate", "mediaextractor", "audioserver", "metrics_collector", "metricsd", "webservd", "debuggerd", "mediacodec", "cameraserver", "firewalld", "trunksd", "nvram", "dns", "dns_tether", "webview_zygote", "vehicle_network", "media_audio", "media_vidio", "media_image", "tombstoned", "media_obb", "ese", "ota_update"};

    private PackageUtils() {
    }

    public static int getUidByPackage(Context context, String packageName) {
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo applicationInfo = pm.getApplicationInfo(packageName, 1);
            int uid = applicationInfo.uid;
            return uid;
        } catch (Exception e) {
            LogUtils.e(TAG, "", e);
            return -1;
        }
    }

    public static int getPidByPackage(Context context, String packageName) {
        try {
            ActivityManager am = (ActivityManager) context.getSystemService("activity");
            for (ActivityManager.RunningAppProcessInfo appProcessInfo : am.getRunningAppProcesses()) {
                if (appProcessInfo.processName.equals(packageName)) {
                    int pid = appProcessInfo.pid;
                    return pid;
                }
            }
            return -1;
        } catch (Exception e) {
            LogUtils.e(TAG, "", e);
            return -1;
        }
    }

    public static String getPackageNameByUid(Context context, int uid) {
        if (uid == 0) {
            return "root";
        }
        if (uid < 10000) {
            int index = uid - 1000;
            if (index < 0) {
                return null;
            }
            String[] strArr = ANDROID_IDS_INFO;
            if (index < strArr.length) {
                return strArr[index];
            }
            return null;
        }
        String[] pkgs = context.getPackageManager().getPackagesForUid(uid);
        if (pkgs != null && pkgs.length > 0) {
            return pkgs[0];
        }
        return null;
    }
}
