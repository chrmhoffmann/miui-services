package com.miui.server.sentinel;

import android.os.Debug;
import android.os.SystemProperties;
import android.util.Slog;
import java.text.SimpleDateFormat;
import java.util.Date;
/* loaded from: classes.dex */
public class MiuiSentinelUtils {
    private static boolean MTBF_MIUI_TEST = false;
    private static final String PROPERTIES_MTBF_MIUI_TEST = "ro.miui.mtbftest";
    private static final String PROPERTIES_MTBF_TEST = "persist.mtbf.test";
    private static final String PROPERTIES_OMNI_TEST = "persist.omni.test";
    private static final String SYSPROP_ENABLE_TRACK_MALLOC = "persist.track.malloc.enable";
    private static final String TAG = "MiuiSentinelUtils";
    private static final String PROPERTIES_MTBF_COREDUMP = "persist.reboot.coredump";
    private static boolean REBOOT_COREDUMP = SystemProperties.getBoolean(PROPERTIES_MTBF_COREDUMP, false);

    static {
        boolean z = false;
        if (SystemProperties.getInt(PROPERTIES_MTBF_MIUI_TEST, 0) == 1) {
            z = true;
        }
        MTBF_MIUI_TEST = z;
    }

    private static boolean isOmniTest() {
        return SystemProperties.getInt(PROPERTIES_OMNI_TEST, 0) == 1;
    }

    private static boolean isMtbfTest() {
        return SystemProperties.getBoolean(PROPERTIES_MTBF_TEST, false) || REBOOT_COREDUMP || MTBF_MIUI_TEST;
    }

    public static boolean isLaboratoryTest() {
        return isMtbfTest() || isOmniTest();
    }

    public static boolean isEnaleTrack() {
        if (SystemProperties.get(SYSPROP_ENABLE_TRACK_MALLOC, "") != "") {
            return true;
        }
        return false;
    }

    public static long getTotalRss() {
        Debug.MemoryInfo mi = new Debug.MemoryInfo();
        Slog.d(TAG, "total RSS :" + mi.getTotalRss());
        return mi.getTotalRss();
    }

    public static String getFormatDateTime(long timeMillis) {
        Date date = new Date(timeMillis);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        return dateFormat.format(date);
    }
}
