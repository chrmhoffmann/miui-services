package com.miui.server;

import android.content.ComponentName;
import android.content.Intent;
import com.android.server.wm.ActivityTaskSupervisorImpl;
/* loaded from: classes.dex */
public class ConfirmStartHelper {
    private static final String CONFIRM_ACCESS_CONTROL_ACTIVITY_NAME = "com.miui.applicationlock.ConfirmAccessControl";
    private static final String CONFIRM_START_ACTIVITY_ACTION = "android.app.action.CHECK_ALLOW_START_ACTIVITY";
    private static final String CONFIRM_START_ACTIVITY_NAME = "com.miui.wakepath.ui.ConfirmStartActivity";
    private static final String PACKAGE_SECURITYCENTER = "com.miui.securitycenter";

    public static boolean isAllowStartCurrentActivity(int callerAppUid, Intent intent) {
        if (callerAppUid != 1000 && intent != null) {
            if (CONFIRM_START_ACTIVITY_ACTION.equals(intent.getAction()) || (ActivityTaskSupervisorImpl.MIUI_APP_LOCK_ACTION.equals(intent.getAction()) && intent.getParcelableExtra("android.intent.extra.INTENT") != null)) {
                return false;
            }
            if (intent.getComponent() != null) {
                ComponentName curComponent = intent.getComponent();
                if ("com.miui.securitycenter".equals(curComponent.getPackageName()) && CONFIRM_START_ACTIVITY_NAME.equals(curComponent.getClassName())) {
                    return false;
                }
                return !"com.miui.securitycenter".equals(curComponent.getPackageName()) || !"com.miui.applicationlock.ConfirmAccessControl".equals(curComponent.getClassName()) || intent.getParcelableExtra("android.intent.extra.INTENT") == null;
            }
        }
        return true;
    }

    public static boolean isAllowStartCurrentActivity(Intent intent) {
        if (intent != null) {
            if (CONFIRM_START_ACTIVITY_ACTION.equals(intent.getAction())) {
                return false;
            }
            if (intent.getComponent() != null) {
                ComponentName curComponent = intent.getComponent();
                return !"com.miui.securitycenter".equals(curComponent.getPackageName()) || !CONFIRM_START_ACTIVITY_NAME.equals(curComponent.getClassName());
            }
            return true;
        }
        return true;
    }
}
