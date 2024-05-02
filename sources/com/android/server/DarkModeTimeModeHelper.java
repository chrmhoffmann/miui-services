package com.android.server;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.UiModeManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.SystemProperties;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.util.Slog;
import com.android.server.DarkModeSunTimeHelper;
import com.miui.server.input.util.MiuiCustomizeShortCutUtils;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
/* loaded from: classes.dex */
public class DarkModeTimeModeHelper {
    private static final String TAG = "DarkModeTimeModeHelper";

    public static void startDarkModeAutoTime(Context ctx, Intent intent) {
        if (intent == null) {
            return;
        }
        boolean enable = intent.getBooleanExtra(MiuiCustomizeShortCutUtils.ATTRIBUTE_ENABLE, true);
        boolean onlyRegisterAlarm = intent.getBooleanExtra("onlyRegisterAlarm", false);
        Slog.i(TAG, " enable = " + enable + " onlyRegisterAlarm = " + onlyRegisterAlarm);
        startDarkModeAutoTime(ctx, enable, onlyRegisterAlarm);
    }

    public static void startDarkModeAutoTime(Context ctx, boolean enable, boolean onlyRegisterAlarm) {
        cancelOnOffTime(ctx);
        if (!enable) {
            Slog.i(TAG, "startDarkModeAutoTime enable = false");
        } else if (!isDarkModeTimeEnable(ctx)) {
            Slog.i(TAG, "isDarkModeTimeEnable = false");
        } else {
            Settings.System.putIntForUser(ctx.getContentResolver(), "dark_mode_enable_by_setting", 0, 0);
            if (isInternationalVersion() && isSuntimeType(ctx)) {
                setDarkModeForInternationalTwilight(ctx);
                return;
            }
            int startTime = isSuntimeType(ctx) ? getSunSetTime(ctx) : getDarkModeStartTime(ctx);
            int endTime = isSuntimeType(ctx) ? getSunRiseTime(ctx) : getDarkModeEndTime(ctx);
            Slog.i(TAG, "type = " + getDarkModeTimeType(ctx) + " startDarkModeAutoTime startTime = " + getTimeInString(startTime) + " endTime = " + getTimeInString(endTime));
            if (!onlyRegisterAlarm) {
                setDarkModeEnable(ctx, isInDarkModeTimeSchedule(startTime, endTime), false);
            }
            if (startTime != endTime) {
                setDarkModeTimeStartEndAlarm(ctx, getAlarmInMills(startTime), getAlarmInMills(endTime));
            }
        }
    }

    public static long getAlarmInMills(int time) {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(11);
        int minute = calendar.get(12);
        int currentTime = (hour * 60) + minute;
        if (currentTime >= time) {
            calendar.add(6, 1);
        }
        calendar.set(11, time / 60);
        calendar.set(12, time % 60);
        calendar.set(13, 0);
        calendar.set(14, 0);
        return calendar.getTimeInMillis();
    }

    public static void cancelOnOffTime(Context context) {
        AlarmManager alarm = (AlarmManager) context.getSystemService("alarm");
        PendingIntent darkModeOn = getPendingIntent(context, "miui.action.intent.DARK_MODE_TIME_ON");
        alarm.cancel(darkModeOn);
        PendingIntent darkModeOff = getPendingIntent(context, "miui.action.intent.DARK_MODE_TIME_OFF");
        alarm.cancel(darkModeOff);
    }

    public static void setDarkModeTimeStartEndAlarm(Context context, long startTime, long endTime) {
        Slog.i(TAG, "setDarkModeTimeStartEndAlarm startTime: " + startTime + " endTime: " + endTime);
        AlarmManager alarm = (AlarmManager) context.getSystemService("alarm");
        PendingIntent darkModeOn = getPendingIntent(context, "miui.action.intent.DARK_MODE_TIME_ON");
        PendingIntent darkModeOff = getPendingIntent(context, "miui.action.intent.DARK_MODE_TIME_OFF");
        alarm.setExact(1, startTime, darkModeOn);
        alarm.setExact(1, endTime, darkModeOff);
    }

    private static PendingIntent getPendingIntent(Context context, String action) {
        Intent intent = new Intent();
        intent.setAction(action);
        intent.addFlags(16777216);
        return PendingIntent.getBroadcast(context, 0, intent, 201326592);
    }

    public static void sendDarkModeSunTimeBroadcast(Context ctx) {
        Slog.i(TAG, "sendDarkModeSunTimeCast");
        Intent intent = new Intent("miui.action.intent.DARK_MODE_TIME_MODE");
        intent.putExtra(MiuiCustomizeShortCutUtils.ATTRIBUTE_ENABLE, true);
        intent.putExtra("onlyRegisterAlarm", false);
        ctx.sendBroadcast(intent);
    }

    public static void setDarkModeEnable(Context ctx, boolean enable, boolean isTiming) {
        Slog.i(TAG, "setDarkModeEnable: enable = " + enable);
        Settings.Global.putInt(ctx.getContentResolver(), "uimode_timing", isTiming ? 1 : 0);
        UiModeManager manager = (UiModeManager) ctx.getSystemService(UiModeManager.class);
        manager.setNightMode(enable ? 2 : 1);
    }

    public static void setDarkModeForInternationalTwilight(Context ctx) {
        Slog.i(TAG, "international twilight");
        UiModeManager manager = (UiModeManager) ctx.getSystemService(UiModeManager.class);
        manager.setNightMode(0);
        Settings.Global.putInt(ctx.getContentResolver(), "uimode_timing", 0);
    }

    public static boolean isInDarkModeTimeSchedule(int startTime, int endTime) {
        int now = getNowTime();
        if (startTime > endTime) {
            if (now >= endTime && now < startTime) {
                return false;
            }
            return true;
        } else if (startTime < endTime && now >= startTime && now < endTime) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isInNight(Context context) {
        return getNowTime() >= getSunSetTime(context) || getNowTime() <= getSunRiseTime(context);
    }

    public static int getNowTime() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(11);
        int minute = calendar.get(12);
        return (hour * 60) + minute;
    }

    public static long getNowTimeInMills() {
        return System.currentTimeMillis();
    }

    public static String getTimeInString(int time) {
        return (time / 60) + ": " + (time % 60);
    }

    public static boolean isDarkModeOpen(Context context) {
        if (isDarkModeEnable(context)) {
            Slog.i(TAG, "darkModeEnable = true");
            return true;
        } else if (isDarkModeTimeEnable(context)) {
            Slog.i(TAG, "darkModeTimeEnable = true");
            return true;
        } else {
            return false;
        }
    }

    public static void updateDarkModeSuntime(Context context) {
        DarkModeSunTimeHelper.SunTime suntime = DarkModeSunTimeHelper.getSunRiseSunSetTime(context);
        setSunRiseTime(context, suntime.getSunrise());
        setSunSetTime(context, suntime.getSunset());
    }

    public static void setDarkModeSuggestCount(Context context, int count) {
        Settings.System.putInt(context.getContentResolver(), "dark_mode_suggest_notification_count", count);
    }

    public static int getDarkModeSuggestCount(Context context) {
        return Settings.System.getInt(context.getContentResolver(), "dark_mode_suggest_notification_count", 0);
    }

    public static boolean isOnHome(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService("activity");
        List<ActivityManager.RunningTaskInfo> runningTaskInfos = activityManager.getRunningTasks(1);
        return getHomeApplicationList(context).contains(runningTaskInfos.get(0).topActivity.getPackageName());
    }

    private static List<String> getHomeApplicationList(Context context) {
        List<String> names = new ArrayList<>();
        PackageManager packageManager = context.getPackageManager();
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.HOME");
        List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(intent, 65536);
        for (ResolveInfo resolveInfo : resolveInfos) {
            names.add(resolveInfo.activityInfo.packageName);
        }
        return names;
    }

    public static boolean isDarkModeEnable(Context context) {
        return Settings.System.getIntForUser(context.getContentResolver(), DarkModeTimeModeManager.DARK_MODE_ENABLE, 0, 0) == 1;
    }

    public static boolean isDarkModeTimeEnable(Context context) {
        return MiuiSettings.System.getBoolean(context.getContentResolver(), "dark_mode_time_enable", false);
    }

    public static void setDarkModeAutoTimeEnable(Context ctx, boolean enable) {
        MiuiSettings.System.putBoolean(ctx.getContentResolver(), "dark_mode_auto_time_enable", enable);
    }

    public static void setSunRiseSunSetMode(Context ctx, boolean enable) {
        MiuiSettings.System.putBoolean(ctx.getContentResolver(), "dark_mode_sun_time_mode_enable", enable);
    }

    public static void setDarkModeTimeEnable(Context ctx, boolean enable) {
        MiuiSettings.System.putBoolean(ctx.getContentResolver(), "dark_mode_time_enable", enable);
    }

    public static void setLastSuggestTime(Context ctx, long lastTime) {
        Settings.System.putLong(ctx.getContentResolver(), "dark_mode_last_time_of_suggest", lastTime);
    }

    public static void setDarkModeSuggestEnable(Context ctx, boolean enable) {
        MiuiSettings.System.putBoolean(ctx.getContentResolver(), "dark_mode_has_get_suggest_from_cloud", enable);
    }

    public static int getDarkModeStartTime(Context ctx) {
        return Settings.System.getInt(ctx.getContentResolver(), "dark_mode_time_start", 1140);
    }

    public static int getDarkModeEndTime(Context ctx) {
        return Settings.System.getInt(ctx.getContentResolver(), "dark_mode_time_end", 420);
    }

    public static int getSunRiseTime(Context ctx) {
        return Settings.System.getInt(ctx.getContentResolver(), "dark_mode_sunrise_time", 360);
    }

    public static void setSunRiseTime(Context ctx, int sunrise) {
        Slog.i(TAG, "setSunRiseTime = " + getTimeInString(sunrise));
        Settings.System.putInt(ctx.getContentResolver(), "dark_mode_sunrise_time", sunrise);
    }

    public static int getSunSetTime(Context ctx) {
        return Settings.System.getInt(ctx.getContentResolver(), "dark_mode_sunset_time", 1080);
    }

    public static void setSunSetTime(Context ctx, int sunset) {
        Slog.i(TAG, "setSunSetTime = " + getTimeInString(sunset));
        Settings.System.putInt(ctx.getContentResolver(), "dark_mode_sunset_time", sunset);
    }

    public static int getDarkModeTimeType(Context ctx) {
        return Settings.System.getInt(ctx.getContentResolver(), "dark_mode_time_type", 2);
    }

    public static long getLastSuggestTime(Context ctx) {
        return Settings.System.getLong(ctx.getContentResolver(), "dark_mode_last_time_of_suggest", -10080L);
    }

    public static boolean isDarkModeSuggestEnable(Context ctx) {
        return MiuiSettings.System.getBoolean(ctx.getContentResolver(), "dark_mode_has_get_suggest_from_cloud", false);
    }

    public static void setDarkModeTimeType(Context context, int type) {
        Settings.System.putInt(context.getContentResolver(), "dark_mode_time_type", type);
    }

    public static boolean isInternationalVersion() {
        return !SystemProperties.get("ro.miui.region", "CN").contains("CN");
    }

    public static boolean isSuntimeType(Context context) {
        return 2 == getDarkModeTimeType(context);
    }

    public static boolean isSuntimeIllegal(int sunrise, int sunset) {
        return sunrise > 720 || sunset > 1440 || sunrise < 0 || sunset < 0;
    }

    public static boolean isDarkWallpaperModeEnable(Context ctx) {
        return MiuiSettings.System.getBoolean(ctx.getContentResolver(), "darken_wallpaper_under_dark_mode", true);
    }

    public static boolean isDarkModeContrastEnable(Context ctx) {
        return MiuiSettings.System.getBoolean(ctx.getContentResolver(), "dark_mode_contrast_enable", false);
    }
}
