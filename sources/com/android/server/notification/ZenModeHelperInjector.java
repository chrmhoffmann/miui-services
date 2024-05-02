package com.android.server.notification;

import android.app.AppOpsManager;
import android.content.Context;
import android.provider.MiuiSettings;
import android.service.notification.ZenModeConfig;
import android.util.Log;
/* loaded from: classes.dex */
public class ZenModeHelperInjector {
    private ZenModeHelperInjector() {
    }

    static int applyRingerModeToZen(ZenModeHelper helper, Context context, int ringerMode) {
        int zenMode = helper.getZenMode();
        switch (ringerMode) {
            case 0:
                if (zenMode == 3 || zenMode == 2) {
                    return -1;
                }
                return 3;
            case 1:
            case 2:
                if (zenMode == 0) {
                    return -1;
                }
                int i = 1;
                if (zenMode == 1) {
                    return -1;
                }
                if (!MiuiSettings.AntiSpam.isQuietModeEnable(context)) {
                    i = 0;
                }
                int newZen = i;
                return newZen;
            default:
                return -1;
        }
    }

    static int applyRingerModeToZen(ZenModeHelper helper, Context context, int ringerModeOld, int ringerModeNew, int newZen) {
        if (!MiuiSettings.SilenceMode.isSupported) {
            return applyRingerModeToZen(helper, context, ringerModeNew);
        }
        boolean isChange = ringerModeNew != ringerModeOld;
        int zenMode = helper.getZenMode();
        switch (ringerModeNew) {
            case 0:
            case 1:
                if (isChange) {
                    if (zenMode == 0) {
                        return 4;
                    }
                    if (4 == zenMode) {
                        return -1;
                    }
                    return newZen;
                } else if (newZen == 0) {
                    Log.d("ZenModeHelperInjector", "RINGER MODE is not Change");
                    return zenMode;
                } else {
                    return newZen;
                }
            case 2:
                if (isChange && zenMode == 4) {
                    return 0;
                }
                return newZen;
            default:
                return newZen;
        }
    }

    static void applyMiuiRestrictions(ZenModeHelper helper, AppOpsManager mAppOps) {
        boolean allowNotification;
        boolean allowRingtone;
        if (!MiuiSettings.SilenceMode.isSupported) {
            return;
        }
        String[] defaultException = {"com.android.cellbroadcastreceiver"};
        String[] exceptionPackages = {"com.android.systemui", "android", "com.android.cellbroadcastreceiver", "com.android.server.telecom"};
        int mode = helper.getZenMode();
        ZenModeConfig config = helper.getConfig();
        boolean hasException = false;
        switch (mode) {
            case 1:
                allowRingtone = false;
                allowNotification = false;
                hasException = config.allowCalls || config.allowRepeatCallers;
                break;
            default:
                allowNotification = true;
                allowRingtone = true;
                break;
        }
        applyRestriction(allowRingtone, 6, mAppOps, hasException ? exceptionPackages : defaultException);
        applyRestriction(allowNotification, 5, mAppOps, hasException ? exceptionPackages : defaultException);
    }

    /* JADX WARN: Multi-variable type inference failed */
    private static void applyRestriction(boolean allow, int usage, AppOpsManager appOps, String[] exception) {
        appOps.setRestriction(28, usage, !allow ? 1 : 0, exception);
        appOps.setRestriction(3, usage, !allow, exception);
    }

    static int getOutRingerMode(int newZen, int curZen, int ringerModeNew, int out) {
        if (!MiuiSettings.SilenceMode.isSupported) {
            return out;
        }
        return (newZen == -1 ? curZen : newZen) == 1 ? out : ringerModeNew;
    }

    private static int zenSeverity(int zen) {
        switch (zen) {
            case 1:
                return 1;
            case 2:
                return 3;
            case 3:
                return 2;
            default:
                return 0;
        }
    }

    static int miuiComputeZenMode(String reason, ZenModeConfig config) {
        int zen = 0;
        if (config == null) {
            return 0;
        }
        if (config.manualRule != null && !"conditionChanged".equals(reason) && !"setNotificationPolicy".equals(reason) && !"updateAutomaticZenRule".equals(reason) && !"onSystemReady".equals(reason) && !"readXml".equals(reason) && !"init".equals(reason) && !"zmc.onServiceAdded".equals(reason) && !"cleanUpZenRules".equals(reason)) {
            return config.manualRule.zenMode;
        }
        if (config.manualRule != null) {
            zen = config.manualRule.zenMode;
        }
        for (ZenModeConfig.ZenRule automaticRule : config.automaticRules.values()) {
            if (automaticRule.isAutomaticActive() && (zenSeverity(automaticRule.zenMode) > zenSeverity(zen) || (automaticRule.zenMode == 4 && zen == 0))) {
                zen = automaticRule.zenMode;
            }
        }
        return zen;
    }
}
