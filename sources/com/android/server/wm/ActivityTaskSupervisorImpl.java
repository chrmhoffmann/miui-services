package com.android.server.wm;

import android.app.ActivityOptions;
import android.app.ActivityThread;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.IApplicationThread;
import android.app.ProfilerInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.text.TextUtils;
import android.util.Slog;
import com.android.server.am.ActivityManagerService;
import com.android.server.am.PendingIntentRecord;
import com.android.server.am.PendingIntentRecordImpl;
import com.android.server.wm.ActivityTaskSupervisorStub;
import com.miui.base.MiuiStubRegistry;
import java.util.ArrayList;
/* loaded from: classes.dex */
public class ActivityTaskSupervisorImpl extends ActivityTaskSupervisorStub {
    public static final String EXTRA_PACKAGE_NAME = "android.intent.extra.PACKAGE_NAME";
    private static final String INCALL_PACKAGE_NAME = "com.android.incallui";
    private static final String INCALL_UI_NAME = "com.android.incallui.InCallActivity";
    private static final int MAX_SWITCH_INTERVAL = 1000;
    public static final String MIUI_APP_LOCK_ACTION = "miui.intent.action.CHECK_ACCESS_CONTROL";
    public static final String MIUI_APP_LOCK_ACTIVITY_NAME = "com.miui.applicationlock.ConfirmAccessControl";
    public static final String MIUI_APP_LOCK_PACKAGE_NAME = "com.miui.securitycenter";
    public static final int MIUI_APP_LOCK_REQUEST_CODE = -1001;
    private static final String TAG = "ActivityTaskSupervisor";
    private static long mLastIncallUiLaunchTime = -1;
    private static int sActivityRequestId;
    static final ArrayList<String> sSupportsMultiTaskInDockList;
    private ActivityTaskManagerService mAtmService;
    private Context mContext;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<ActivityTaskSupervisorImpl> {

        /* compiled from: ActivityTaskSupervisorImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final ActivityTaskSupervisorImpl INSTANCE = new ActivityTaskSupervisorImpl();
        }

        public ActivityTaskSupervisorImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public ActivityTaskSupervisorImpl provideNewInstance() {
            return new ActivityTaskSupervisorImpl();
        }
    }

    void init(ActivityTaskManagerService atms, Context context) {
        this.mAtmService = atms;
        this.mContext = context;
    }

    void startActivityFromRecentsFlag(Intent intent) {
        ComponentName component;
        String targetPkg = intent.getPackage();
        if (targetPkg == null && (component = intent.getComponent()) != null) {
            targetPkg = component.getPackageName();
        }
        if (targetPkg == null) {
            PackageManager pm = this.mContext.getPackageManager();
            ResolveInfo resolveInfo = pm.resolveActivity(intent, 0);
            if (resolveInfo != null) {
                targetPkg = resolveInfo.resolvePackageName;
            }
        }
        if (!TextUtils.isEmpty(targetPkg)) {
            PendingIntentRecordImpl.exemptTemporarily(targetPkg, true);
        }
    }

    boolean isAppLockActivity(Intent intent, ActivityInfo aInfo) {
        return intent != null && aInfo != null && MIUI_APP_LOCK_PACKAGE_NAME.equals(intent.getPackage()) && MIUI_APP_LOCK_ACTION.equals(intent.getAction()) && "com.miui.applicationlock.ConfirmAccessControl".equals(aInfo.name);
    }

    static {
        ArrayList<String> arrayList = new ArrayList<>();
        sSupportsMultiTaskInDockList = arrayList;
        arrayList.add("com.miui.hybrid");
    }

    static void updateInfoBeforeRealStartActivity(Task stack, IApplicationThread caller, int callingUid, String callingPackage, Intent intent, ActivityInfo aInfo, IBinder resultTo, int requestCode, int userId) {
        MiuiMultiTaskManagerStub.get().updateMultiTaskInfoIfNeed(stack, aInfo, intent);
    }

    static boolean isAllowedAppSwitch(Task stack, String callingPackageName, ActivityInfo aInfo, long lastTime) {
        return isAllowedAppSwitch(stack, callingPackageName, aInfo);
    }

    static boolean isAllowedAppSwitch(Task stack, String callingPackageName, ActivityInfo aInfo) {
        if (stack == null) {
            return false;
        }
        ActivityRecord topr = stack.topRunningNonDelayedActivityLocked((ActivityRecord) null);
        if (topr != null && topr.info != null && INCALL_UI_NAME.equals(topr.info.name) && !INCALL_PACKAGE_NAME.equals(callingPackageName) && aInfo != null && !INCALL_UI_NAME.equals(aInfo.name) && mLastIncallUiLaunchTime + 1000 > System.currentTimeMillis()) {
            Slog.w("ActivityManager", "app switch:" + aInfo.name + " stopped for " + INCALL_UI_NAME + "in 1000 ms.Try later.");
            return false;
        } else if (aInfo != null && INCALL_UI_NAME.equals(aInfo.name)) {
            mLastIncallUiLaunchTime = System.currentTimeMillis();
            return true;
        } else {
            return true;
        }
    }

    public int noteOperationLocked(int appOp, int callingUid, String callingPackage, Handler handler, ActivityTaskSupervisorStub.OpCheckData checker) {
        if (checker == null) {
            return 0;
        }
        ActivityManagerService service = ServiceManager.getService("activity");
        AppOpsManager appOpsManager = (AppOpsManager) ActivityThread.currentApplication().getSystemService(AppOpsManager.class);
        int mode = appOpsManager.checkOpNoThrow(appOp, callingUid, callingPackage);
        if (mode != 5) {
            appOpsManager.noteOpNoThrow(appOp, callingUid, callingPackage, (String) null, (String) null);
            return mode;
        }
        int userId = checker.userId;
        int requestCode = getNextRequestIdLocked();
        PendingIntentRecord intentSender = service.mPendingIntentController.getIntentSender(2, callingPackage, (String) null, callingUid, userId, (IBinder) null, (String) null, requestCode, new Intent[]{checker.orginalintent}, new String[]{checker.orginalintent.resolveType(service.mActivityTaskManager.mContext.getContentResolver())}, 1342177280, (Bundle) null);
        Intent intent = new Intent("com.miui.intent.action.REQUEST_PERMISSIONS");
        intent.setPackage("com.lbe.security.miui");
        intent.putExtra("android.intent.extra.PACKAGE_NAME", callingPackage);
        intent.putExtra("android.intent.extra.UID", callingUid);
        intent.putExtra("android.intent.extra.INTENT", new IntentSender(intentSender));
        if (checker.resultRecord != null) {
            intent.putExtra("EXTRA_RESULT_NEEDED", true);
        }
        intent.putExtra("op", appOp);
        ActivityInfo activityInfo = checker.stackSupervisor.resolveActivity(intent, checker.resolvedType, checker.startFlags, (ProfilerInfo) null, userId, callingUid);
        if (activityInfo == null) {
            return mode;
        }
        checker.newAInfo = activityInfo;
        checker.newIntent = intent;
        checker.newRInfo = resolveIntent(intent, checker.resolvedType, userId);
        Slog.i(TAG, "MIUILOG - Launching Request permission [Activity] uid : " + callingUid + "  pkg : " + callingPackage + " op : " + appOp);
        return 0;
    }

    public static boolean supportsMultiTaskInDock(String packageName) {
        return sSupportsMultiTaskInDockList.contains(packageName);
    }

    private static ResolveInfo resolveIntent(Intent intent, String resolvedType, int userId) {
        try {
            return AppGlobals.getPackageManager().resolveIntent(intent, resolvedType, 66560L, userId);
        } catch (RemoteException e) {
            return null;
        }
    }

    private static int getNextRequestIdLocked() {
        if (sActivityRequestId >= Integer.MAX_VALUE) {
            sActivityRequestId = 0;
        }
        int i = sActivityRequestId + 1;
        sActivityRequestId = i;
        return i;
    }

    public static boolean notPauseAtFreeformMode(Task focusStack, Task curStack) {
        return false;
    }

    public static boolean supportsFreeform() {
        return false;
    }

    public static Task exitfreeformIfNeeded(Task task, int taskId, int windowMode, ActivityTaskSupervisor supervisor) {
        if (task == null || task.getWindowingMode() != 5 || windowMode == 5) {
            return task;
        }
        ActivityOptions op = ActivityOptions.makeBasic();
        op.setLaunchWindowingMode(1);
        Task tTask = supervisor.mRootWindowContainer.anyTaskForId(taskId, 2, op, true);
        return tTask;
    }

    public static void updateApplicationConfiguration(ActivityTaskSupervisor stackSupervisor, Configuration globalConfiguration, String packageName) {
        Task topStack;
        ActivityRecord topActivity;
        synchronized (stackSupervisor.mService.mGlobalLock) {
            topStack = stackSupervisor.mRootWindowContainer.getTopDisplayFocusedRootTask();
        }
        if (topStack != null) {
            synchronized (stackSupervisor.mService.mGlobalLock) {
                topActivity = topStack.topRunningActivityLocked();
            }
            if (topActivity != null && topActivity.getWindowingMode() == 5 && packageName.equals(topActivity.packageName)) {
                Rect rect = topActivity.getConfiguration().windowConfiguration.getBounds();
                globalConfiguration.orientation = rect.height() > rect.width() ? 1 : 2;
                globalConfiguration.windowConfiguration.setWindowingMode(5);
                globalConfiguration.windowConfiguration.setBounds(topActivity.getConfiguration().windowConfiguration.getBounds());
            }
        }
    }
}
