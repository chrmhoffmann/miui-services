package com.android.server;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Slog;
/* loaded from: classes.dex */
public class DarkModeTimeModeReceiver extends BroadcastReceiver {
    private static final String TAG = "DarkModeTimeModeReceiver";
    private DarkModeTimeModeManager mDarkModeTimeModeManager;

    public DarkModeTimeModeReceiver(DarkModeTimeModeManager manager) {
        this.mDarkModeTimeModeManager = manager;
    }

    @Override // android.content.BroadcastReceiver
    public void onReceive(final Context context, Intent intent) {
        int startTime;
        int endTime;
        final String action = intent.getAction();
        Slog.i(TAG, " onReceive: action = " + action);
        if (DarkModeTimeModeHelper.isSuntimeType(context)) {
            startTime = DarkModeTimeModeHelper.getSunSetTime(context);
        } else {
            startTime = DarkModeTimeModeHelper.getDarkModeStartTime(context);
        }
        if (DarkModeTimeModeHelper.isSuntimeType(context)) {
            endTime = DarkModeTimeModeHelper.getSunRiseTime(context);
        } else {
            endTime = DarkModeTimeModeHelper.getDarkModeEndTime(context);
        }
        if (DarkModeTimeModeHelper.isDarkModeTimeEnable(context)) {
            Slog.i(TAG, "type = " + DarkModeTimeModeHelper.getDarkModeTimeType(context) + " startDarkModeAutoTime startTime = " + DarkModeTimeModeHelper.getTimeInString(startTime) + " endTime = " + DarkModeTimeModeHelper.getTimeInString(endTime));
            if (DarkModeTimeModeHelper.isSuntimeType(context) && DarkModeTimeModeHelper.isSuntimeIllegal(endTime, startTime)) {
                Slog.i(TAG, "suntime is illegal,update suntime");
                MiuiBgThread.getHandler().post(new Runnable() { // from class: com.android.server.DarkModeTimeModeReceiver.1
                    @Override // java.lang.Runnable
                    public void run() {
                        DarkModeTimeModeHelper.updateDarkModeSuntime(context);
                        DarkModeTimeModeHelper.sendDarkModeSunTimeBroadcast(context);
                    }
                });
            }
        }
        char c = 65535;
        switch (action.hashCode()) {
            case -2035794839:
                if (action.equals("miui.action.intent.DARK_MODE_SUGGEST_ENABLE")) {
                    c = 4;
                    break;
                }
                break;
            case -1822376118:
                if (action.equals("miui.action.intent.DARK_MODE_TIME_MODE")) {
                    c = 0;
                    break;
                }
                break;
            case -1400777402:
                if (action.equals("miui.action.intent.DARK_MODE_TIME_ON")) {
                    c = 1;
                    break;
                }
                break;
            case -474426680:
                if (action.equals("miui.action.intent.DARK_MODE_TIME_OFF")) {
                    c = 2;
                    break;
                }
                break;
            case -415578687:
                if (action.equals("miui.action.intent.DARK_MODE_SUGGEST_MESSAGE")) {
                    c = 5;
                    break;
                }
                break;
            case 502473491:
                if (action.equals("android.intent.action.TIMEZONE_CHANGED")) {
                    c = 7;
                    break;
                }
                break;
            case 505380757:
                if (action.equals("android.intent.action.TIME_SET")) {
                    c = 6;
                    break;
                }
                break;
            case 823795052:
                if (action.equals("android.intent.action.USER_PRESENT")) {
                    c = 3;
                    break;
                }
                break;
            case 1041332296:
                if (action.equals("android.intent.action.DATE_CHANGED")) {
                    c = '\b';
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
                DarkModeTimeModeHelper.startDarkModeAutoTime(context, intent);
                return;
            case 1:
                if (DarkModeTimeModeHelper.isInDarkModeTimeSchedule(startTime, endTime) && DarkModeTimeModeHelper.isDarkModeTimeEnable(context)) {
                    DarkModeTimeModeHelper.setDarkModeEnable(context, true, true);
                }
                DarkModeTimeModeHelper.startDarkModeAutoTime(context, true, true);
                return;
            case 2:
                if (!DarkModeTimeModeHelper.isInDarkModeTimeSchedule(startTime, endTime) && DarkModeTimeModeHelper.isDarkModeTimeEnable(context)) {
                    DarkModeTimeModeHelper.setDarkModeEnable(context, false, true);
                }
                DarkModeTimeModeHelper.startDarkModeAutoTime(context, true, true);
                return;
            case 3:
                if (this.mDarkModeTimeModeManager.canShowDarkModeSuggestNotifocation(context)) {
                    this.mDarkModeTimeModeManager.showDarkModeSuggestNotification(context);
                    return;
                }
                return;
            case 4:
                this.mDarkModeTimeModeManager.showDarkModeSuggestToast(context);
                return;
            case 5:
                this.mDarkModeTimeModeManager.enterSettingsFromNotification(context);
                return;
            case 6:
            case 7:
            case '\b':
                if (DarkModeTimeModeHelper.isDarkModeTimeEnable(context)) {
                    MiuiBgThread.getHandler().post(new Runnable() { // from class: com.android.server.DarkModeTimeModeReceiver.2
                        @Override // java.lang.Runnable
                        public void run() {
                            if (DarkModeTimeModeHelper.isSuntimeType(context) && action == "android.intent.action.TIMEZONE_CHANGED") {
                                DarkModeTimeModeHelper.updateDarkModeSuntime(context);
                            }
                            DarkModeTimeModeHelper.startDarkModeAutoTime(context, true, false);
                        }
                    });
                    return;
                }
                return;
            default:
                return;
        }
    }
}
