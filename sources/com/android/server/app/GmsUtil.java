package com.android.server.app;

import android.os.Build;
import android.util.Slog;
import com.android.server.app.GameManagerServiceStubImpl;
import java.math.BigDecimal;
import java.util.HashSet;
/* loaded from: classes.dex */
public class GmsUtil {
    private static final String TAG = "GameManagerServiceStub";

    public static boolean isContainDevice(HashSet<String> devices) {
        if (devices == null) {
            return false;
        }
        boolean containDevice = devices.contains(Build.DEVICE);
        return containDevice;
    }

    public static String calcuRatio(int targetWidth, int currentWidth) {
        BigDecimal bigDecimal = new BigDecimal((targetWidth * 1.0f) / currentWidth);
        float ratio = bigDecimal.setScale(2, 0).floatValue();
        if (ratio >= 1.0f) {
            Slog.d(TAG, "ratio >= 1  targetWidth = " + targetWidth + "  currentWidth = " + currentWidth);
            return "disable";
        }
        if (ratio > 0.6f) {
            int raTemp = (int) (ratio * 100.0f);
            int lastDigital = raTemp % 10;
            int firstDigital = raTemp / 10;
            if (lastDigital != 0) {
                lastDigital = 5;
            }
            ratio = (((firstDigital * 10) + lastDigital) * 1.0f) / 100.0f;
        }
        return Math.max(ratio, 0.6f) + "";
    }

    public static String getTargetRatioForPad(GameManagerServiceStubImpl.AppItem appItem, boolean mPowerSaving, GameManagerServiceStubImpl.DownscaleCloudData mDownscaleCloudData) {
        int mode = appItem.mode;
        if (mode == 0) {
            return "disable";
        }
        int appVersion = appItem.appVersion;
        boolean saveBatteryEnable = true;
        if (appVersion > 1) {
            return "disable";
        }
        if ((mode & 2) == 0) {
            saveBatteryEnable = false;
        }
        boolean containDevice = isContainDevice(mDownscaleCloudData.devices);
        if (!mPowerSaving || !saveBatteryEnable || !containDevice) {
            return "disable";
        }
        return mDownscaleCloudData.scenes.padSaveBattery;
    }
}
