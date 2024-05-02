package com.android.server;

import android.content.Context;
import android.content.Intent;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.util.Slog;
import com.android.server.MiuiBatteryStatsService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
/* loaded from: classes.dex */
public class DarkModeOneTrackHelper {
    private static final String APP_ID = "31000000485";
    private static final String EVENT_NAME = "EVENT_NAME";
    public static final String EVENT_NAME_AUTO_SWITCH = "auto_switch";
    public static final String EVENT_NAME_SETTING = "setting";
    public static final String EVENT_NAME_STATUS = "status";
    public static final String EVENT_NAME_SUGGEST = "darkModeSuggest";
    private static final String ONETRACK_PACKAGE_NAME = "com.miui.analytics";
    private static final String ONE_TRACK_ACTION = "onetrack.action.TRACK_EVENT";
    private static final String PARAM_KEY_APP_LIST = "app_list";
    private static final String PARAM_KEY_APP_NAME = "app_name";
    private static final String PARAM_KEY_APP_PKG = "app_package_name";
    private static final String PARAM_KEY_BEGIN_TIME = "begin_time";
    private static final String PARAM_KEY_CONTRAST = "font_bgcolor_status";
    private static final String PARAM_KEY_DARK_MODE_STATUS = "dark_status";
    private static final String PARAM_KEY_END_TIME = "end_time";
    private static final String PARAM_KEY_SETTING_CHANNEL = "setting_channel";
    private static final String PARAM_KEY_STATUS_AFTER_CLICK = "after_click_status";
    private static final String PARAM_KEY_SUGGEST = "dark_mode_mode_suggest";
    private static final String PARAM_KEY_SUGGEST_CLICK = "dark_mode_suggest_enter_settings";
    private static final String PARAM_KEY_SUGGEST_ENABLE = "dark_mode_suggest_enable";
    private static final String PARAM_KEY_SUGGEST_OPEN_IN_SETTING = "open_dark_mode_from_settings";
    private static final String PARAM_KEY_SWITCH_WAY = "switch_way";
    private static final String PARAM_KEY_TIME_MODE_PATTERN = "dark_mode_timing_pattern";
    private static final String PARAM_KEY_TIME_MODE_STATUS = "dark_mode_timing_status";
    private static final String PARAM_KEY_WALL_PAPER = "wallpaper_status";
    public static final String PARAM_VALUE_APP_NAME = "app_name";
    public static final String PARAM_VALUE_APP_PKG = "app_package_name";
    public static final String PARAM_VALUE_APP_SWITCH_STATUS = "app_switch_status";
    public static final String PARAM_VALUE_CHANNEL_CENTER = "控制中心";
    public static final String PARAM_VALUE_CHANNEL_NOTIFY = "弹窗";
    public static final String PARAM_VALUE_CHANNEL_SETTING = "设置";
    public static final String PARAM_VALUE_CLOSE = "关";
    public static final String PARAM_VALUE_CUSTOM = "自定义时间";
    public static final String PARAM_VALUE_OPEN = "开";
    public static final String PARAM_VALUE_SWITCH_CLOSE = "从深到浅";
    public static final String PARAM_VALUE_SWITCH_OPEN = "从浅到深";
    public static final String PARAM_VALUE_TWILIGHT = "日出日落模式";
    private static final String TAG = "DarkModeOneTrackHelper";
    private static final String TIP = "tip";
    public static final String TIP_APP_SETTING = "577.3.3.1.23094";
    public static final String TIP_APP_STATUS = "577.1.2.1.23090";
    public static final String TIP_AUTO_SWITCH = "577.5.0.1.23096";
    public static final String TIP_CONTRAST_SETTING = "577.3.2.1.23093";
    public static final String TIP_DARK_MODE_SETTING = "577.4.0.1.23106";
    public static final String TIP_DARK_MODE_STATUS = "577.1.1.1.23089";
    public static final String TIP_SUGGEST = "";
    public static final String TIP_TIME_MODE_SETTING = "577.2.0.1.23091";
    public static final String TIP_WALL_PAPER_SETTING = "577.3.1.1.23092";
    private static final String DEVICE_REGION = SystemProperties.get("ro.miui.region", "CN");
    private static Set<String> sRegions = new HashSet();

    public static void uploadToOneTrack(final Context context, DarkModeEvent event) {
        if (!sRegions.isEmpty()) {
            Set<String> set = sRegions;
            String str = DEVICE_REGION;
            if (set.contains(str)) {
                Slog.i(TAG, "do not upload data in " + str);
                return;
            }
        }
        if (context == null || event == null || event.getEventName() == null || event.getTip() == null) {
            return;
        }
        final DarkModeEvent newEvent = event.clone();
        MiuiBgThread.getHandler().post(new Runnable() { // from class: com.android.server.DarkModeOneTrackHelper.1
            @Override // java.lang.Runnable
            public void run() {
                Intent intent = new Intent("onetrack.action.TRACK_EVENT");
                intent.setPackage("com.miui.analytics");
                intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_APP_ID, DarkModeOneTrackHelper.APP_ID);
                intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_PACKAGE, context.getPackageName());
                DarkModeOneTrackHelper.updateDarkModeIntent(intent, newEvent);
                try {
                    context.startServiceAsUser(intent, UserHandle.CURRENT);
                    Slog.d(DarkModeOneTrackHelper.TAG, "uploadDataToOneTrack Success");
                } catch (IllegalStateException e) {
                    Slog.e(DarkModeOneTrackHelper.TAG, "Filed to upload DarkModeOneTrackInfo ", e);
                }
            }
        });
    }

    public static void setDataDisableRegion(Set<String> regions) {
        sRegions.clear();
        sRegions.addAll(regions);
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public static void updateDarkModeIntent(Intent intent, DarkModeEvent event) {
        char c;
        String tip = event.getTip();
        switch (tip.hashCode()) {
            case -1558836096:
                if (tip.equals(TIP_DARK_MODE_STATUS)) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case -1548332969:
                if (tip.equals(TIP_TIME_MODE_SETTING)) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case -857771494:
                if (tip.equals(TIP_WALL_PAPER_SETTING)) {
                    c = 3;
                    break;
                }
                c = 65535;
                break;
            case -847267704:
                if (tip.equals(TIP_DARK_MODE_SETTING)) {
                    c = 6;
                    break;
                }
                c = 65535;
                break;
            case -177713122:
                if (tip.equals(TIP_APP_SETTING)) {
                    c = 5;
                    break;
                }
                c = 65535;
                break;
            case 0:
                if (tip.equals("")) {
                    c = '\b';
                    break;
                }
                c = 65535;
                break;
            case 928676759:
                if (tip.equals(TIP_APP_STATUS)) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case 1629741340:
                if (tip.equals(TIP_CONTRAST_SETTING)) {
                    c = 4;
                    break;
                }
                c = 65535;
                break;
            case 1650747551:
                if (tip.equals(TIP_AUTO_SWITCH)) {
                    c = 7;
                    break;
                }
                c = 65535;
                break;
            default:
                c = 65535;
                break;
        }
        switch (c) {
            case 0:
                updateDarkModeStatusIntent(intent, (DarkModeStauesEvent) event);
                return;
            case 1:
                updateAppStatusIntent(intent, (DarkModeStauesEvent) event);
                return;
            case 2:
                updateTimeModeSettingIntent(intent, (DarkModeStauesEvent) event);
                return;
            case 3:
                updateWallPaperSettingIntent(intent, (DarkModeStauesEvent) event);
                return;
            case 4:
                updateContrastSettingIntent(intent, (DarkModeStauesEvent) event);
                return;
            case 5:
                updateAppSettingIntent(intent, (DarkModeStauesEvent) event);
                return;
            case 6:
                updateDarkModeSettingIntent(intent, (DarkModeStauesEvent) event);
                return;
            case 7:
                updateAutoSwitchIntent(intent, (DarkModeStauesEvent) event);
                return;
            case '\b':
                updateSuggestEventIntent(intent, (DarkModeStauesEvent) event);
                return;
            default:
                return;
        }
    }

    private static void updateDarkModeStatusIntent(Intent intent, DarkModeStauesEvent event) {
        intent.putExtra("EVENT_NAME", EVENT_NAME_STATUS).putExtra(TIP, event.getTip()).putExtra(PARAM_KEY_DARK_MODE_STATUS, event.getDarkModeStatus()).putExtra(PARAM_KEY_TIME_MODE_STATUS, event.getTimeModeStatus()).putExtra(PARAM_KEY_TIME_MODE_PATTERN, event.getTimeModePattern()).putExtra(PARAM_KEY_BEGIN_TIME, event.getBeginTime()).putExtra(PARAM_KEY_END_TIME, event.getEndTime()).putExtra(PARAM_KEY_WALL_PAPER, event.getWallPaperStatus()).putExtra(PARAM_KEY_CONTRAST, event.getContrastStatus());
    }

    private static void updateAppStatusIntent(Intent intent, DarkModeStauesEvent event) {
        intent.putExtra("EVENT_NAME", EVENT_NAME_STATUS).putExtra(TIP, event.getTip()).putStringArrayListExtra(PARAM_KEY_APP_LIST, (ArrayList) event.getAppList());
    }

    private static void updateTimeModeSettingIntent(Intent intent, DarkModeStauesEvent event) {
        intent.putExtra("EVENT_NAME", EVENT_NAME_SETTING).putExtra(TIP, event.getTip()).putExtra(PARAM_KEY_TIME_MODE_STATUS, event.getTimeModeStatus()).putExtra(PARAM_KEY_TIME_MODE_PATTERN, event.getTimeModePattern()).putExtra(PARAM_KEY_BEGIN_TIME, event.getBeginTime()).putExtra(PARAM_KEY_END_TIME, event.getEndTime());
        if (PARAM_VALUE_TWILIGHT.equals(event.getTimeModePattern())) {
            intent.putExtra(PARAM_KEY_SETTING_CHANNEL, event.getSettingChannel());
        }
    }

    private static void updateWallPaperSettingIntent(Intent intent, DarkModeStauesEvent event) {
        intent.putExtra("EVENT_NAME", EVENT_NAME_SETTING).putExtra(TIP, event.getTip()).putExtra(PARAM_KEY_STATUS_AFTER_CLICK, event.getWallPaperStatus());
    }

    private static void updateContrastSettingIntent(Intent intent, DarkModeStauesEvent event) {
        intent.putExtra("EVENT_NAME", EVENT_NAME_SETTING).putExtra(TIP, event.getTip()).putExtra(PARAM_KEY_STATUS_AFTER_CLICK, event.getContrastStatus());
    }

    private static void updateAppSettingIntent(Intent intent, DarkModeStauesEvent event) {
        intent.putExtra("EVENT_NAME", EVENT_NAME_SETTING).putExtra(TIP, event.getTip()).putExtra("app_package_name", event.getAppPkg()).putExtra("app_name", event.getAppName()).putExtra(PARAM_KEY_STATUS_AFTER_CLICK, event.getAppEnable());
    }

    private static void updateDarkModeSettingIntent(Intent intent, DarkModeStauesEvent event) {
        intent.putExtra("EVENT_NAME", EVENT_NAME_SETTING).putExtra(TIP, event.getTip()).putExtra(PARAM_KEY_DARK_MODE_STATUS, event.getDarkModeStatus()).putExtra(PARAM_KEY_SETTING_CHANNEL, event.getSettingChannel());
    }

    private static void updateAutoSwitchIntent(Intent intent, DarkModeStauesEvent event) {
        intent.putExtra("EVENT_NAME", EVENT_NAME_AUTO_SWITCH).putExtra(TIP, event.getTip()).putExtra(PARAM_KEY_SWITCH_WAY, event.getAutoSwitch());
    }

    private static void updateSuggestEventIntent(Intent intent, DarkModeStauesEvent event) {
        intent.putExtra("EVENT_NAME", EVENT_NAME_SUGGEST);
        if (event.getSuggest() != 0) {
            intent.putExtra(PARAM_KEY_SUGGEST, event.getSuggest());
        }
        if (event.getSuggestEnable() != 0) {
            intent.putExtra(PARAM_KEY_SUGGEST_ENABLE, event.getSuggestEnable());
        }
        if (event.getSuggestClick() != 0) {
            intent.putExtra(PARAM_KEY_SUGGEST_CLICK, event.getSuggestClick());
        }
        if (event.getSuggestOpenInSetting() != 0) {
            intent.putExtra(PARAM_KEY_SUGGEST_OPEN_IN_SETTING, event.getSuggestOpenInSetting());
        }
    }
}
