package com.android.server.location.gnss.map;

import android.os.Bundle;
import java.util.ArrayList;
/* loaded from: classes.dex */
public class AmapExtraCommand {
    public static final String APP_ACTIVE_KEY = "app_active";
    public static final String APP_CTRL_KEY = "app_control";
    public static final String APP_CTRL_LOG_KEY = "app_control_log";
    public static final String APP_FG_KEY = "app_forground";
    public static final String APP_LAST_RPT_TIME_KEY = "app_last_report_second";
    public static final String APP_PERM_KEY = "app_permission";
    public static final String APP_PWR_MODE_KEY = "app_power_mode";
    public static final String GNSS_LAST_RPT_TIME_KEY = "gnss_last_report_second";
    public static final String GNSS_REAL_KEY = "gnss_real";
    public static final String GNSS_STATUS_KEY = "gnss_status";
    public static final String GPS_TIMEOUT_CMD = "send_gps_timeout";
    public static final String LISTENER_HASHCODE_KEY = "listenerHashcode";
    public static final String ORIGIN_KEY = "origin";
    public static final String OWNER = "amap";
    public static final String SAT_ALL_CNT_KEY = "satellite_all_count";
    public static final String SAT_SNR_OVER0_CNT_KEY = "satellite_snr_over0_count";
    public static final String SAT_SNR_OVER20_CNT_KEY = "satellite_snr_over20_count";
    public static final int SV_STATUS_INTERVAL_MILLIS = 5000;
    public static final String VERSION_KEY = "version";
    public static final String VERSION_NAME = "v2";
    private static final ArrayList<String> sSupportedCommands;

    static {
        ArrayList<String> arrayList = new ArrayList<>();
        sSupportedCommands = arrayList;
        arrayList.add(GPS_TIMEOUT_CMD);
    }

    private AmapExtraCommand() {
    }

    public static boolean isSupported(String command, Bundle extras) {
        if (!OWNER.equalsIgnoreCase(extras.getString(ORIGIN_KEY))) {
            return false;
        }
        return sSupportedCommands.contains(command);
    }
}
