package com.android.server.display.statistics;

import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.util.Slog;
import com.android.server.MiuiBatteryStatsService;
/* loaded from: classes.dex */
public class OneTrackUploaderHelper {
    public static final String ADVANCED_EVENT_NAME = "advanced_brightness";
    private static final String BRIGHTNESS_EVENT_APP_ID = "31000000084";
    public static final String BRIGHTNESS_EVENT_NAME = "brightness";
    private static final int FLAG_NON_ANONYMOUS = 2;
    private static final int FLAG_NOT_LIMITED_BY_USER_EXPERIENCE_PLAN = 1;
    private static final String KEY_AFFECT_FACTOR_FLAG = "affect_factor_flag";
    private static final String KEY_ALL_STATS_ENTRIES = "all_stats_entries";
    private static final String KEY_AMBIENT_LUX = "ambient_lux";
    private static final String KEY_AMBIENT_LUX_SPAN = "ambient_lux_span";
    private static final String KEY_AUTO_BRIGHTNESS_ANIMATION_DURATION = "auto_brightness_animation_duration";
    private static final String KEY_BRIGHTNESS_CHANGED_STATUS = "brightness_changed_state";
    private static final String KEY_BRIGHTNESS_USAGE_MAP = "brightness_usage_map";
    private static final String KEY_CURRENT_ANIMATE_VALUE = "current_animate_value";
    private static final String KEY_CURRENT_USER_ID = "current_user_id";
    private static final String KEY_DEFAULT_SPLINE_ERROR = "default_spline_error";
    private static final String KEY_DISPLAY_GRAY_SCALE = "display_gray_scale";
    private static final String KEY_EVENT_TYPE = "type";
    private static final String KEY_EXTRA = "extra";
    private static final String KEY_INTERRUPT_BRIGHTNESS_ANIMATION_TIMES = "interrupt_brightness_animation_times";
    private static final String KEY_IS_DEFAULT_CONFIG = "is_default_config";
    private static final String KEY_LONG_TERM_MODEL_SPLINE_ERROR = "long_term_model_spline_error";
    private static final String KEY_LOW_POWER_MODE_FLAG = "low_power_mode_flag";
    private static final String KEY_ORIENTATION = "orientation";
    private static final String KEY_PREVIOUS_BRIGHTNESS = "previous_brightness";
    private static final String KEY_PREVIOUS_BRIGHTNESS_SPAN = "previous_brightness_span";
    private static final String KEY_SCREEN_BRIGHTNESS = "screen_brightness";
    private static final String KEY_SCREEN_BRIGHTNESS_SPAN = "screen_brightness_span";
    private static final String KEY_SPLINE = "spline";
    private static final String KEY_TARGET_ANIMATE_VALUE = "target_animate_value";
    private static final String KEY_TIME_STAMP = "time_stamp";
    private static final String KEY_TOP_PACKAGE = "top_package";
    private static final String KEY_USER_BRIGHTNESS = "user_brightness";
    private static final String KEY_USER_DATA_POINT = "user_data_point";
    private static final String KEY_USER_RESET_BRIGHTNESS_MODE_TIMES = "user_reset_brightness_mode_times";
    private static final String ONE_TRACK_ACTION = "onetrack.action.TRACK_EVENT";
    private static final String DEVICE_REGION = SystemProperties.get("ro.miui.region", "CN");
    public static final boolean IS_INTERNATIONAL_BUILD = SystemProperties.get("ro.product.mod_device", "").contains("_global");

    public static void reportToOneTrack(Context context, Parcelable parcelable, String eventName) {
        if (DEVICE_REGION.equals("IN")) {
            return;
        }
        Intent intent = new Intent("onetrack.action.TRACK_EVENT");
        intent.setPackage(MiuiBatteryStatsService.TrackBatteryUsbInfo.ANALYTICS_PACKAGE).putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_PACKAGE, context.getPackageName());
        char c = 65535;
        switch (eventName.hashCode()) {
            case -725610354:
                if (eventName.equals(ADVANCED_EVENT_NAME)) {
                    c = 1;
                    break;
                }
                break;
            case 648162385:
                if (eventName.equals("brightness")) {
                    c = 0;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
                updateBrightnessEventIntent(intent, (BrightnessEvent) parcelable);
                break;
            case 1:
                updateAdvancedEventIntent(intent, (AdvancedEvent) parcelable);
                break;
        }
        if (!IS_INTERNATIONAL_BUILD) {
            intent.setFlags(3);
        }
        try {
            context.startServiceAsUser(intent, UserHandle.CURRENT);
        } catch (IllegalStateException e) {
            Slog.w("MiuiBrightnessChangeTracker", "Failed to upload brightness event!");
        }
    }

    public static void updateBrightnessEventIntent(Intent intent, BrightnessEvent event) {
        intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_APP_ID, BRIGHTNESS_EVENT_APP_ID).putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_EVENT_NAME, "brightness").putExtra("type", event.getEventType()).putExtra(KEY_AMBIENT_LUX, event.getAmbientLux()).putExtra(KEY_SCREEN_BRIGHTNESS, event.getScreenBrightness()).putExtra(KEY_ORIENTATION, event.getOrientation()).putExtra(KEY_SPLINE, event.getSpline()).putExtra(KEY_TOP_PACKAGE, event.getForegroundPackage()).putExtra(KEY_TIME_STAMP, event.getTimeStamp()).putExtra(KEY_EXTRA, event.getExtra()).putExtra(KEY_PREVIOUS_BRIGHTNESS, event.getPreviousBrightness()).putExtra(KEY_IS_DEFAULT_CONFIG, event.isDefaultConfig()).putExtra(KEY_USER_DATA_POINT, event.getUserDataPoint()).putExtra(KEY_LOW_POWER_MODE_FLAG, event.getLowPowerModeFlag()).putExtra(KEY_CURRENT_USER_ID, event.getUserId()).putExtra(KEY_ALL_STATS_ENTRIES, event.getSwitchStats().toString()).putExtra(KEY_AFFECT_FACTOR_FLAG, event.getAffectFactorFlag()).putExtra(KEY_SCREEN_BRIGHTNESS_SPAN, event.getCurBrightnessSpanIndex()).putExtra(KEY_PREVIOUS_BRIGHTNESS_SPAN, event.getPreBrightnessSpanIndex()).putExtra(KEY_AMBIENT_LUX_SPAN, event.getLuxSpanIndex()).putExtra(KEY_BRIGHTNESS_USAGE_MAP, event.getBrightnessUsageMap().toString()).putExtra(KEY_DISPLAY_GRAY_SCALE, event.getDisplayGrayScale());
    }

    public static void updateAdvancedEventIntent(Intent intent, AdvancedEvent event) {
        intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_APP_ID, BRIGHTNESS_EVENT_APP_ID).putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_EVENT_NAME, ADVANCED_EVENT_NAME).putExtra("type", event.getEventType()).putExtra(KEY_INTERRUPT_BRIGHTNESS_ANIMATION_TIMES, event.getInterruptBrightnessAnimationTimes()).putExtra(KEY_USER_RESET_BRIGHTNESS_MODE_TIMES, event.getUserResetBrightnessModeTimes()).putExtra(KEY_AUTO_BRIGHTNESS_ANIMATION_DURATION, event.getAutoBrightnessAnimationDuration()).putExtra(KEY_CURRENT_ANIMATE_VALUE, event.getCurrentAnimateValue()).putExtra(KEY_TARGET_ANIMATE_VALUE, event.getTargetAnimateValue()).putExtra(KEY_USER_BRIGHTNESS, event.getUserBrightness()).putExtra(KEY_LONG_TERM_MODEL_SPLINE_ERROR, event.getLongTermModelSplineError()).putExtra(KEY_DEFAULT_SPLINE_ERROR, event.getDefaultSplineError()).putExtra(KEY_BRIGHTNESS_CHANGED_STATUS, event.getBrightnessChangedState()).putExtra(KEY_EXTRA, event.getExtra());
    }
}
