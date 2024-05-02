package com.android.server;

import android.media.MiuiXlog;
import android.os.Process;
import android.os.SystemProperties;
import android.util.Log;
import android.util.Slog;
/* loaded from: classes.dex */
public class MQSThread extends Thread {
    private static final String TAG = "MQSThread.vibrator";
    private final String PackageName;
    private MiuiXlog mMiuiXlog = new MiuiXlog();

    public MQSThread(String PKG) {
        this.PackageName = PKG;
    }

    @Override // java.lang.Thread, java.lang.Runnable
    public void run() {
        Process.setThreadPriority(0);
        try {
            reportDynamicDailyUse();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "error for MQSThread");
        }
    }

    public void reportDynamicDailyUse() {
        if (!isAllowedRegion()) {
            return;
        }
        Slog.i(TAG, "reportDynamicDailyUse start.");
        String result = String.format("{\"name\":\"dynamic_dailyuse\",\"audio_event\":{\"dynamic_package_name\":\"%s\"},\"dgt\":\"null\",\"audio_ext\":\"null\" }", this.PackageName);
        Slog.d(TAG, "reportDynamicDailyUse:" + result);
        try {
            this.mMiuiXlog.miuiXlogSend(result);
        } catch (Throwable th) {
            Slog.d(TAG, "can not use miuiXlogSend!!!");
        }
    }

    private boolean isAllowedRegion() {
        String region = SystemProperties.get("ro.miui.region", "");
        Slog.i(TAG, "the region is :" + region);
        return region.equals("CN");
    }
}
