package com.android.server.wm;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Rect;
import android.os.SystemProperties;
import android.server.am.SplitScreenReporter;
import android.util.MiuiMultiWindowAdapter;
import android.util.MiuiMultiWindowUtils;
import android.util.Slog;
import java.lang.reflect.Method;
import java.util.List;
import miui.app.MiuiFreeFormManager;
/* loaded from: classes.dex */
class ActivityStarterInjector {
    public static final int FLAG_ASSOCIATED_SETTINGS_AV = 134217728;
    private static final String TAG = "ActivityStarter";

    ActivityStarterInjector() {
    }

    public static void checkFreeformSupport(ActivityTaskManagerService service, ActivityOptions options) {
        if (!service.mSupportsFreeformWindowManagement && options != null && options.getLaunchWindowingMode() == 5) {
            boolean isMiuiBuild = SystemProperties.getBoolean("persist.sys.miui_optimization", !SplitScreenReporter.ACTION_ENTER_SPLIT.equals(SystemProperties.get("ro.miui.cts")));
            if (isMiuiBuild) {
                service.mSupportsFreeformWindowManagement = isMiuiBuild;
            } else {
                service.mMiuiFreeFormManagerService.showOpenMiuiOptimizationToast();
            }
        }
    }

    private static ActivityOptions modifyLaunchActivityOptionIfNeed(RootWindowContainer root, String callingPackgae, ActivityOptions options, WindowProcessController callerApp, int userId, Intent intent, boolean pcRun, ActivityRecord sourceRecord) {
        String startPackageName;
        ActivityOptions options2;
        MiuiFreeFormManager.MiuiFreeFormStackInfo stackToAvoidInfo;
        String startPackageName2 = null;
        MiuiFreeFormActivityStackStub sourceFfas = null;
        ActivityRecord sourceFreeFormActivity = sourceRecord;
        if (intent == null) {
            startPackageName = null;
        } else {
            if (intent.getComponent() != null) {
                startPackageName2 = intent.getComponent().getPackageName();
            }
            if (startPackageName2 != null) {
                startPackageName = startPackageName2;
            } else {
                String startPackageName3 = intent.getPackage();
                startPackageName = startPackageName3;
            }
        }
        if (sourceFreeFormActivity == null && callerApp != null) {
            List<ActivityRecord> callerActivities = callerApp.getActivities();
            List<ActivityRecord> inactiveActivities = callerApp.getInactiveActivities();
            sourceFreeFormActivity = getLastFreeFormActivityRecord(callerActivities) != null ? getLastFreeFormActivityRecord(callerActivities) : getLastFreeFormActivityRecord(inactiveActivities);
        }
        if (sourceFreeFormActivity != null) {
            sourceFfas = sourceFreeFormActivity.mWmService.mAtmService.mMiuiFreeFormManagerService.getMiuiFreeFormActivityStack(sourceFreeFormActivity.getRootTaskId());
        }
        if (sourceFfas != null && startPackageName != null && startPackageName.equals(sourceFfas.getStackPackageName())) {
            if (options != null) {
                options2 = options;
            } else {
                options2 = ActivityOptions.makeBasic();
            }
            options2.setLaunchWindowingMode(5);
            Rect rect = sourceFreeFormActivity.getBounds();
            float scale = sourceFfas.getFreeFormScale();
            Rect targetVisualBounds = MiuiMultiWindowUtils.getVisualBounds(rect, scale);
            Context context = sourceFreeFormActivity.mWmService.mContext;
            if (MiuiMultiWindowUtils.multiFreeFormSupported(context) && (stackToAvoidInfo = MiuiFreeFormManager.getFreeFormStackToAvoid(context.getDisplay().getDisplayId(), startPackageName)) != null) {
                MiuiMultiWindowUtils.avoidAsPossible(targetVisualBounds, MiuiMultiWindowUtils.getVisualBounds(stackToAvoidInfo.bounds, stackToAvoidInfo.freeFormScale), MiuiMultiWindowUtils.getFreeFormAccessibleArea(context, false));
                rect.offsetTo(targetVisualBounds.left, targetVisualBounds.top);
            }
            options2.setLaunchBounds(rect);
            try {
                Method method = MiuiMultiWindowUtils.isMethodExist(options2, "getActivityOptionsInjector", new Object[0]);
                if (method != null && scale != -1.0f) {
                    MiuiMultiWindowUtils.invoke(method.invoke(options2, new Object[0]), "setFreeformScale", new Object[]{Float.valueOf(scale)});
                }
            } catch (Exception e) {
            }
            Slog.d(TAG, "ActivityStarterInjector::modifyLaunchActivityOptionIfNeed::rect = " + rect + " scale = " + scale);
        } else {
            options2 = options;
        }
        if (pcRun) {
            return modifyLaunchActivityOptionForPcWin(options2, intent);
        }
        return options2;
    }

    public static ActivityOptions modifyLaunchActivityOptionForPcWin(ActivityOptions options, Intent intent) {
        if (options == null) {
            options = ActivityOptions.makeBasic();
        }
        if (intent.getComponent() == null) {
            return options;
        }
        String packName = intent.getComponent().getPackageName();
        Slog.d(TAG, "modifyLaunchActivityOptionForPcWin pcRun " + packName + " " + options.getLaunchWindowingMode() + " " + options.getLaunchBounds());
        return options;
    }

    public static ActivityOptions modifyLaunchActivityOptionIfNeed(ActivityTaskManagerService service, RootWindowContainer root, String callingPackgae, ActivityOptions options, WindowProcessController callerApp, Intent intent, int userId, ActivityInfo aInfo, ActivityRecord sourceRecord) {
        if (isStartedInMiuiSetttingVirtualDispaly(root, intent, aInfo) && options != null && options.getLaunchDisplayId() == -1) {
            options.setLaunchDisplayId(0);
        }
        if (0 == 0 && intent != null && intent.getComponent() != null && (MiuiMultiWindowAdapter.START_FROM_FREEFORM_BLACK_LIST_ACTIVITY.contains(intent.getComponent().flattenToShortString()) || MiuiMultiWindowAdapter.getFreeformBlackList().contains(intent.getComponent().getPackageName()))) {
            return options;
        }
        return modifyLaunchActivityOptionIfNeed(root, callingPackgae, options, callerApp, userId, intent, false, sourceRecord);
    }

    public static boolean getLastFrame(String name) {
        if (name.contains("com.tencent.mobileqq/com.tencent.av.ui.VideoInviteActivity") || name.contains("com.tencent.mm/.plugin.voip.ui.VideoActivity") || name.contains("com.tencent.mobileqq/com.tencent.av.ui.AVActivity") || name.contains("com.tencent.mobileqq/com.tencent.av.ui.AVLoadingDialogActivity") || name.contains("com.android.incallui/.InCallActivity") || name.contains("com.google.android.dialer/com.android.incallui.InCallActivity") || name.contains("voipcalling.VoipActivityV2")) {
            return true;
        }
        return false;
    }

    public static void startActivityUncheckedBefore(ActivityRecord r, boolean isFromHome) {
    }

    private static boolean isStartedInMiuiSetttingVirtualDispaly(RootWindowContainer root, Intent intent, ActivityInfo aInfo) {
        ActivityRecord result;
        if (aInfo == null || (result = root.findActivity(intent, aInfo, false)) == null || result.intent == null || (result.intent.getMiuiFlags() & FLAG_ASSOCIATED_SETTINGS_AV) == 0) {
            return false;
        }
        return true;
    }

    private static ActivityRecord getLastFreeFormActivityRecord(List<ActivityRecord> activities) {
        if (activities != null && !activities.isEmpty() && activities.get(activities.size() - 1) != null && activities.get(activities.size() - 1).getWindowingMode() == 5) {
            return activities.get(activities.size() - 1);
        }
        return null;
    }
}
