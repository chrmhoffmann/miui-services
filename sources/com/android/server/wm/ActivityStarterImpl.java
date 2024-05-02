package com.android.server.wm;

import android.app.ActivityOptions;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.IApplicationThread;
import android.app.ProfilerInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.miui.AppOpsUtils;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.function.QuintPredicate;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.internal.util.function.pooled.PooledPredicate;
import com.android.server.am.ActivityManagerServiceImpl;
import com.android.server.am.PendingIntentRecordImpl;
import com.android.server.am.SmartPowerServiceStub;
import com.miui.base.MiuiStubRegistry;
import com.miui.server.AppRunningControlService;
import com.miui.server.SplashScreenServiceDelegate;
import com.miui.server.greeze.GreezeManagerService;
import com.miui.whetstone.server.IWhetstoneActivityManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
/* loaded from: classes.dex */
public class ActivityStarterImpl extends ActivityStarterStub {
    private static final ArrayMap<String, String> ACTION_TO_RUNTIME_PERMISSION;
    private static final int ACTIVITY_RESTRICTION_APPOP = 2;
    private static final int ACTIVITY_RESTRICTION_NONE = 0;
    private static final int ACTIVITY_RESTRICTION_PERMISSION = 1;
    private static final String CARLINK_NOT_SUPPORT_IN_MUTIL_DISPLAY_ACTIVITYS_ACTION = "not_support_in_mutil_display_activitys_action";
    private static final String TAG = "ActivityStarterImpl";
    private ActivityTaskManagerService mAtmService;
    private Context mContext;
    private List<String> mDefaultHomePkgNames;
    private Handler mHandler;
    private HandlerThread mHandlerThread;
    private int mLastStartActivityUid;
    private SplashScreenServiceDelegate mSplashScreenServiceDelegate;
    private boolean mSystemReady;
    private static final Set<String> CARLINK_NOT_SUPPORT_IN_MUTIL_DISPLAY_ACTIVITYS = new HashSet(Arrays.asList("com.autonavi.minimap", "com.baidu.BaiduMap", "com.sinyee.babybus.story"));
    private static final Set<String> CARLINK_VIRTUAL_DISPLAY_SET = new HashSet(Arrays.asList("com.miui.carlink", "com.xiaomi.ucar.minimap", "com.miui.car.launcher"));

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<ActivityStarterImpl> {

        /* compiled from: ActivityStarterImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final ActivityStarterImpl INSTANCE = new ActivityStarterImpl();
        }

        public ActivityStarterImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public ActivityStarterImpl provideNewInstance() {
            return new ActivityStarterImpl();
        }
    }

    ActivityStarterImpl() {
    }

    static {
        ArrayMap<String, String> arrayMap = new ArrayMap<>();
        ACTION_TO_RUNTIME_PERMISSION = arrayMap;
        arrayMap.put("android.media.action.IMAGE_CAPTURE", "android.permission.CAMERA");
        arrayMap.put("android.media.action.VIDEO_CAPTURE", "android.permission.CAMERA");
        arrayMap.put("android.intent.action.CALL", "android.permission.CALL_PHONE");
    }

    public void init(ActivityTaskManagerService service, Context context) {
        this.mContext = context;
        this.mAtmService = service;
    }

    public void onSystemReady() {
        this.mSystemReady = true;
        this.mSplashScreenServiceDelegate = new SplashScreenServiceDelegate(this.mContext);
    }

    public static ActivityStarterImpl getInstance() {
        return (ActivityStarterImpl) ActivityStarterStub.get();
    }

    public boolean checkRunningCompatibility(IApplicationThread caller, ActivityInfo info, Intent intent, int userId, String callingPackage) {
        return ActivityManagerServiceImpl.getInstance().checkRunningCompatibility(caller, info, intent, userId, callingPackage);
    }

    private static void checkAndNotify(int uid) {
        try {
            GreezeManagerService gz = GreezeManagerService.getService();
            if (gz != null && gz.isUidFrozen(uid)) {
                IWhetstoneActivityManager ws = IWhetstoneActivityManager.Stub.asInterface(ServiceManager.getService("whetstone.activity"));
                Bundle b = new Bundle();
                b.putInt("uid", uid);
                if (ws != null) {
                    ws.getPowerKeeperPolicy().notifyEvent(11, b);
                }
            }
        } catch (Exception e) {
            Slog.w(TAG, "checkAndNotify error uid = " + uid);
        }
    }

    public boolean carlinkJudge(Context context, Task targetRootTask, ActivityRecord record, RootWindowContainer rwc) {
        ActivityRecord activityRecord = findMapActivityInHistory(record, rwc);
        if (activityRecord == null || activityRecord.getRootTask() == null || targetRootTask == null) {
            return false;
        }
        DisplayContent preDisplayContent = activityRecord.getRootTask().mDisplayContent;
        int targetDisplayid = targetRootTask.getDisplayId();
        if (!isCarWithDisplay(preDisplayContent) || targetDisplayid != 0) {
            return false;
        }
        Slog.d(TAG, "activity is already started in carlink display");
        Intent srcIntent = record.intent;
        sendBroadCastToUcar(srcIntent, context);
        return true;
    }

    private ActivityRecord findMapActivityInHistory(ActivityRecord r, RootWindowContainer rwc) {
        if (r == null || r.intent == null || r.intent.getComponent() == null || r.info == null || !CARLINK_NOT_SUPPORT_IN_MUTIL_DISPLAY_ACTIVITYS.contains(r.intent.getComponent().getPackageName())) {
            return null;
        }
        ActivityRecord result = findActivityInSameApplication(r.intent, r.info, false, rwc);
        Slog.d(TAG, "findMapActivityInHistory result=" + result + " r=" + r.intent + "  info=" + r.info);
        return result;
    }

    private void sendBroadCastToUcar(Intent srcIntent, final Context context) {
        if (this.mHandlerThread == null) {
            HandlerThread handlerThread = new HandlerThread("carlink-workthread");
            this.mHandlerThread = handlerThread;
            handlerThread.start();
            this.mHandler = new Handler(this.mHandlerThread.getLooper());
        }
        if (context != null) {
            final Intent intent = new Intent();
            intent.setAction(CARLINK_NOT_SUPPORT_IN_MUTIL_DISPLAY_ACTIVITYS_ACTION);
            intent.setPackage("com.miui.carlink");
            Bundle bundle = new Bundle();
            bundle.putParcelable("src_intent", srcIntent);
            intent.putExtras(bundle);
            this.mHandler.post(new Runnable() { // from class: com.android.server.wm.ActivityStarterImpl$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    context.sendBroadcast(intent, "miui.car.permission.MI_CARLINK_STATUS");
                }
            });
        }
    }

    public boolean isCarWithDisplay(DisplayContent dc) {
        if (dc != null && dc.getDisplay() != null) {
            String displayName = dc.getDisplay().getName();
            return CARLINK_VIRTUAL_DISPLAY_SET.contains(displayName);
        }
        return false;
    }

    private ActivityRecord findActivityInSameApplication(Intent intent, ActivityInfo info, boolean compareIntentFilters, RootWindowContainer rwc) {
        if (info.applicationInfo == null || rwc == null) {
            return null;
        }
        ComponentName cls = intent.getComponent();
        if (info.targetActivity != null) {
            cls = new ComponentName(info.packageName, info.targetActivity);
        }
        int userId = UserHandle.getUserId(info.applicationInfo.uid);
        PooledPredicate p = PooledLambda.obtainPredicate(new QuintPredicate() { // from class: com.android.server.wm.ActivityStarterImpl$$ExternalSyntheticLambda1
            public final boolean test(Object obj, Object obj2, Object obj3, Object obj4, Object obj5) {
                boolean matchesPackageName;
                matchesPackageName = ActivityStarterImpl.matchesPackageName((ActivityRecord) obj, ((Integer) obj2).intValue(), ((Boolean) obj3).booleanValue(), (Intent) obj4, (ComponentName) obj5);
                return matchesPackageName;
            }
        }, PooledLambda.__(ActivityRecord.class), Integer.valueOf(userId), Boolean.valueOf(compareIntentFilters), intent, cls);
        if (p == null) {
            return null;
        }
        ActivityRecord r = rwc.getActivity(p);
        p.recycle();
        return r;
    }

    public static boolean matchesPackageName(ActivityRecord r, int userId, boolean compareIntentFilters, Intent intent, ComponentName cls) {
        if (r == null || !r.canBeTopRunning() || r.mUserId != userId) {
            return false;
        }
        if (compareIntentFilters) {
            if (r.intent != null && r.intent.filterEquals(intent)) {
                return true;
            }
        } else if (r.mActivityComponent == null || cls == null) {
            return false;
        } else {
            Slog.d(TAG, "matchesApplication r=" + r.mActivityComponent + " cls=" + cls);
            if (TextUtils.equals(r.mActivityComponent.getPackageName(), cls.getPackageName())) {
                return true;
            }
        }
        return false;
    }

    public boolean isAllowedStartActivity(int callingUid, int callingPid, String callingPackage) {
        ActivityRecord r;
        if (UserHandle.getAppId(callingUid) < 10000 || PendingIntentRecordImpl.containsPendingIntent(callingPackage) || callingUid == this.mLastStartActivityUid || this.mAtmService.hasUserVisibleWindow(callingUid)) {
            this.mLastStartActivityUid = callingUid;
            return true;
        }
        AppOpsManager appops = this.mAtmService.getAppOpsManager();
        DisplayContent display = this.mAtmService.mRootWindowContainer.getDefaultDisplay();
        Task stack = display.getFocusedRootTask();
        if (stack == null || (r = stack.topRunningActivityLocked()) == null) {
            return true;
        }
        if (callingUid == r.info.applicationInfo.uid) {
            this.mLastStartActivityUid = callingUid;
            return true;
        } else if (appops.checkOpNoThrow(10021, callingUid, callingPackage) == 0) {
            return true;
        } else {
            ArraySet<WindowProcessController> apps = this.mAtmService.mProcessMap.getProcesses(callingUid);
            if (apps != null && apps.size() != 0) {
                Iterator<WindowProcessController> it = apps.iterator();
                while (it.hasNext()) {
                    WindowProcessController app = it.next();
                    if (app != null && app.mUid == callingUid && app.hasForegroundActivities()) {
                        this.mLastStartActivityUid = callingUid;
                        return true;
                    }
                }
            }
            Slog.d(TAG, "MIUILOG- Permission Denied Activity :  pkg : " + callingPackage + " uid : " + callingUid + " tuid : " + r.info.applicationInfo.uid);
            return false;
        }
    }

    public boolean isAllowedStartActivity(Intent intent, int callingUid, int callingPid, String callingPackage, int realCallingUid, int realCallingPid, ActivityInfo aInfo) {
        int checkingUid;
        String callingPackage2;
        String str;
        checkAndNotify(aInfo.applicationInfo.uid);
        if (callingUid != realCallingUid && UserHandle.getAppId(realCallingUid) > 10000) {
            WindowProcessController realApp = this.mAtmService.getProcessController(realCallingPid, realCallingUid);
            if (realApp == null) {
                Slog.d(TAG, "MIUILOG- Permission Denied Activity : " + intent + " realPid : " + realCallingPid + " realUid : " + realCallingUid + " pid : " + callingPid + " uid : " + callingUid);
                return false;
            }
            checkingUid = realCallingUid;
            callingPackage2 = realApp.mInfo.packageName;
        } else {
            checkingUid = callingUid;
            callingPackage2 = callingPackage;
        }
        if (UserHandle.getAppId(checkingUid) < 10000 || PendingIntentRecordImpl.containsPendingIntent(callingPackage2) || PendingIntentRecordImpl.containsPendingIntent(aInfo.applicationInfo.packageName) || checkingUid == this.mLastStartActivityUid || this.mAtmService.hasUserVisibleWindow(checkingUid) || ("android.service.dreams.DreamActivity".equals(aInfo.name) && AppOpsUtils.isXOptMode())) {
            this.mLastStartActivityUid = aInfo.applicationInfo.uid;
            return true;
        }
        AppOpsManager appops = this.mAtmService.getAppOpsManager();
        DisplayContent display = this.mAtmService.mRootWindowContainer.getDefaultDisplay();
        Task stack = display.getFocusedRootTask();
        if (stack == null) {
            return true;
        }
        if (!this.mAtmService.mWindowManager.isKeyguardLocked()) {
            str = " pkg : ";
        } else {
            str = " pkg : ";
            if (appops.noteOpNoThrow(10020, checkingUid, callingPackage2, (String) null, "ActivityTaskManagerServiceInjector#isAllowedStartActivity") != 0) {
                Slog.d(TAG, "MIUILOG- Permission Denied Activity KeyguardLocked: " + intent + str + callingPackage2 + " uid : " + checkingUid);
                return false;
            }
        }
        ActivityRecord r = stack.topRunningActivityLocked();
        if (r == null) {
            return true;
        }
        if (checkingUid == r.info.applicationInfo.uid) {
            this.mLastStartActivityUid = aInfo.applicationInfo.uid;
            return true;
        } else if (appops.checkOpNoThrow(10021, checkingUid, callingPackage2) == 0) {
            return true;
        } else {
            ArraySet<WindowProcessController> apps = this.mAtmService.mProcessMap.getProcesses(checkingUid);
            if (apps != null && apps.size() != 0) {
                Iterator<WindowProcessController> it = apps.iterator();
                while (it.hasNext()) {
                    WindowProcessController app = it.next();
                    if (app != null && app.mUid == checkingUid && app.hasForegroundActivities()) {
                        this.mLastStartActivityUid = aInfo.applicationInfo.uid;
                        return true;
                    }
                }
            }
            appops.noteOpNoThrow(10021, checkingUid, callingPackage2, (String) null, "ActivityTaskManagerServiceInjector#isAllowedStartActivity");
            Slog.d(TAG, "MIUILOG- Permission Denied Activity : " + intent + str + callingPackage2 + " uid : " + checkingUid + " tuid : " + r.info.applicationInfo.uid);
            return false;
        }
    }

    public IBinder finishActivity(IBinder token, int resultCode, Intent resultData) {
        return token;
    }

    public void triggerLaunchMode(String processName, int uid) {
        GreezeManagerService.getService().triggerLaunchMode(processName, uid);
    }

    public void finishLaunchMode(String processName, int uid) {
        GreezeManagerService.getService().finishLaunchMode(processName, uid);
    }

    void activityIdle(ActivityInfo aInfo) {
        if (!this.mSystemReady) {
            return;
        }
        if (aInfo == null) {
            Slog.w(TAG, "aInfo is null!");
        } else {
            this.mSplashScreenServiceDelegate.activityIdle(aInfo);
        }
    }

    void destroyActivity(ActivityInfo aInfo) {
        if (!this.mSystemReady) {
            return;
        }
        if (aInfo == null) {
            Slog.w(TAG, "aInfo is null!");
        } else {
            this.mSplashScreenServiceDelegate.destroyActivity(aInfo);
        }
    }

    Intent requestSplashScreen(Intent intent, ActivityInfo aInfo) {
        if (!this.mSystemReady) {
            return intent;
        }
        if (intent == null || aInfo == null) {
            Slog.w(TAG, "Intent or aInfo is null!");
            return intent;
        }
        return this.mSplashScreenServiceDelegate.requestSplashScreen(intent, aInfo);
    }

    Intent requestSplashScreen(Intent intent, ActivityInfo aInfo, SafeActivityOptions options, IApplicationThread caller) {
        ActivityOptions activityOptions;
        ActivityOptions checkedOptions;
        if (!this.mSystemReady) {
            return intent;
        }
        if (intent == null || aInfo == null) {
            Slog.w(TAG, "Intent or aInfo is null!");
            return intent;
        }
        synchronized (this.mAtmService.mGlobalLock) {
            WindowProcessController callerApp = this.mAtmService.getProcessController(caller);
            if (options != null) {
                activityOptions = options.getOptions(intent, aInfo, callerApp, this.mAtmService.mTaskSupervisor);
            } else {
                activityOptions = null;
            }
            checkedOptions = activityOptions;
        }
        if (checkedOptions != null && (checkedOptions.getLaunchWindowingMode() == 3 || checkedOptions.getLaunchWindowingMode() == 4 || checkedOptions.getLaunchWindowingMode() == 5)) {
            Slog.w(TAG, "The Activity is in freeForm|split windowing mode !");
            return intent;
        }
        return this.mSplashScreenServiceDelegate.requestSplashScreen(intent, aInfo);
    }

    ActivityInfo resolveSplashIntent(ActivityInfo aInfo, Intent intent, ProfilerInfo profilerInfo, int userId) {
        if (intent == null) {
            return aInfo;
        }
        ComponentName component = intent.getComponent();
        if (component == null) {
            return aInfo;
        }
        if (SplashScreenServiceDelegate.SPLASHSCREEN_PACKAGE.equals(component.getPackageName()) && SplashScreenServiceDelegate.SPLASHSCREEN_ACTIVITY.equals(component.getClassName())) {
            return this.mAtmService.mTaskSupervisor.resolveActivity(intent, (String) null, 0, profilerInfo, userId, Binder.getCallingUid());
        }
        return aInfo;
    }

    ActivityInfo resolveCheckIntent(ActivityInfo aInfo, Intent intent, ProfilerInfo profilerInfo, int userId) {
        if (intent != null && intent.getComponent() == null) {
            boolean transform = false;
            if (ActivityTaskSupervisorImpl.MIUI_APP_LOCK_ACTION.equals(intent.getAction()) || "android.app.action.CHECK_ACCESS_CONTROL_PAD".equals(intent.getAction()) || "android.app.action.CHECK_ALLOW_START_ACTIVITY".equals(intent.getAction()) || "android.app.action.CHECK_ALLOW_START_ACTIVITY_PAD".equals(intent.getAction()) || "com.miui.gamebooster.action.ACCESS_WINDOWCALLACTIVITY".equals(intent.getAction()) || AppRunningControlService.isBlockActivity(intent)) {
                if (userId == 999) {
                    userId = 0;
                }
                transform = true;
            }
            if (transform) {
                return this.mAtmService.mTaskSupervisor.resolveActivity(intent, (String) null, 0, profilerInfo, userId, Binder.getCallingUid());
            }
            return aInfo;
        }
        return aInfo;
    }

    public void updateLastStartActivityUid(String foregroundPackageName, int lastUid) {
        if (foregroundPackageName == null) {
            return;
        }
        if (this.mDefaultHomePkgNames == null) {
            ArrayList<ResolveInfo> homeActivities = new ArrayList<>();
            IPackageManager pm = AppGlobals.getPackageManager();
            try {
                pm.getHomeActivities(homeActivities);
                if (homeActivities.size() > 0) {
                    Iterator<ResolveInfo> it = homeActivities.iterator();
                    while (it.hasNext()) {
                        ResolveInfo info = it.next();
                        if (this.mDefaultHomePkgNames == null) {
                            this.mDefaultHomePkgNames = new ArrayList();
                        }
                        this.mDefaultHomePkgNames.add(info.activityInfo.packageName);
                    }
                }
            } catch (Exception e) {
            }
        }
        List<String> list = this.mDefaultHomePkgNames;
        if (list != null && list.contains(foregroundPackageName)) {
            this.mLastStartActivityUid = lastUid;
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:20:0x0056, code lost:
        if (miui.security.SecurityManager.isDenyAccessGallery(r27.mContext, r37, r36, r30) == false) goto L22;
     */
    /* JADX WARN: Code restructure failed: missing block: B:27:0x007b, code lost:
        if (miui.security.SecurityManager.isDenyAccessGallery(r27.mContext, r29.packageName, r29.applicationInfo.uid, r30) != false) goto L28;
     */
    /* JADX WARN: Code restructure failed: missing block: B:28:0x007d, code lost:
        android.util.Slog.i(com.android.server.wm.ActivityStarterImpl.TAG, "startAsCaller to pick pictures, not skip!");
     */
    /* JADX WARN: Removed duplicated region for block: B:62:0x0103  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    android.content.Intent checkStartActivityPermission(android.app.IApplicationThread r28, android.content.pm.ActivityInfo r29, android.content.Intent r30, java.lang.String r31, boolean r32, int r33, boolean r34, int r35, int r36, java.lang.String r37, android.os.Bundle r38, java.lang.String r39, android.os.IBinder r40) {
        /*
            Method dump skipped, instructions count: 349
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wm.ActivityStarterImpl.checkStartActivityPermission(android.app.IApplicationThread, android.content.pm.ActivityInfo, android.content.Intent, java.lang.String, boolean, int, boolean, int, int, java.lang.String, android.os.Bundle, java.lang.String, android.os.IBinder):android.content.Intent");
    }

    private boolean checkStartActivityLocked(WindowProcessController callerApp, int callingUid, String callingPackage, ActivityInfo aInfo, Intent intent, String resolvedType, boolean ignoreTargetSecurity, Bundle bOptions) {
        int callingUid2;
        int callingPid;
        Exception e;
        Exception e2;
        long startTime = SystemClock.elapsedRealtime();
        if (callerApp != null) {
            callingPid = callerApp.getPid();
            callingUid2 = callerApp.mUid;
        } else if (callingUid >= 0) {
            callingUid2 = callingUid;
            callingPid = -1;
        } else {
            callingPid = Binder.getCallingPid();
            callingUid2 = Binder.getCallingUid();
        }
        long origId = Binder.clearCallingIdentity();
        try {
            try {
                int startAnyPerm = ActivityTaskManagerService.checkPermission("android.permission.START_ANY_ACTIVITY", callingPid, callingUid2);
                if (startAnyPerm == 0) {
                    Binder.restoreCallingIdentity(origId);
                    return true;
                }
                int componentRestriction = getComponentRestrictionForCallingPackage(aInfo, callingPackage, callingPid, callingUid2, ignoreTargetSecurity);
                try {
                    int actionRestriction = getActionRestrictionForCallingPackage(intent.getAction(), callingPackage, callingPid, callingUid2);
                    if (componentRestriction != 1 && actionRestriction != 1) {
                        if (actionRestriction == 2) {
                            Binder.restoreCallingIdentity(origId);
                            return false;
                        } else if (componentRestriction == 2) {
                            Binder.restoreCallingIdentity(origId);
                            return false;
                        } else {
                            ActivityOptions options = ActivityOptions.fromBundle(bOptions);
                            if (options != null) {
                                try {
                                    if (options.getLaunchTaskId() != -1) {
                                        int startInTaskPerm = ActivityTaskManagerService.checkPermission("android.permission.START_TASKS_FROM_RECENTS", callingPid, callingUid2);
                                        if (startInTaskPerm != 0) {
                                            Binder.restoreCallingIdentity(origId);
                                            return false;
                                        }
                                    }
                                } catch (Exception e3) {
                                    e2 = e3;
                                    Slog.w(TAG, "checkStartActivityLocked: An exception occured. ", e2);
                                    Binder.restoreCallingIdentity(origId);
                                    return false;
                                } catch (Throwable th) {
                                    e = th;
                                    Binder.restoreCallingIdentity(origId);
                                    throw e;
                                }
                            }
                            try {
                                if (!this.mAtmService.mIntentFirewall.checkStartActivity(intent, callingUid2, callingPid, resolvedType, aInfo.applicationInfo)) {
                                    Binder.restoreCallingIdentity(origId);
                                    return false;
                                }
                                if (this.mAtmService.mController != null) {
                                    try {
                                        Intent watchIntent = intent.cloneFilter();
                                        if (!this.mAtmService.mController.activityStarting(watchIntent, aInfo.applicationInfo.packageName)) {
                                            Binder.restoreCallingIdentity(origId);
                                            return false;
                                        }
                                    } catch (RemoteException e4) {
                                    } catch (NullPointerException e5) {
                                        Slog.e(TAG, "IActivityController activityStarting catch NullPointerException", e5);
                                    }
                                }
                                Binder.restoreCallingIdentity(origId);
                                checkTime(startTime, "checkStartActivityLocked");
                                return true;
                            } catch (Exception e6) {
                                e2 = e6;
                                Slog.w(TAG, "checkStartActivityLocked: An exception occured. ", e2);
                                Binder.restoreCallingIdentity(origId);
                                return false;
                            }
                        }
                    }
                    Binder.restoreCallingIdentity(origId);
                    return false;
                } catch (Exception e7) {
                    e2 = e7;
                    Slog.w(TAG, "checkStartActivityLocked: An exception occured. ", e2);
                    Binder.restoreCallingIdentity(origId);
                    return false;
                } catch (Throwable th2) {
                    e = th2;
                    Binder.restoreCallingIdentity(origId);
                    throw e;
                }
            } catch (Throwable th3) {
                e = th3;
            }
        } catch (Exception e8) {
            e2 = e8;
        } catch (Throwable th4) {
            e = th4;
        }
    }

    private static void checkTime(long startTime, String where) {
        long now = SystemClock.elapsedRealtime();
        if (now - startTime > 1000) {
            Slog.w(TAG, "MIUILOG-checkTime:Slow operation: " + (now - startTime) + "ms so far, now at " + where);
        }
    }

    private int getComponentRestrictionForCallingPackage(ActivityInfo activityInfo, String callingPackage, int callingPid, int callingUid, boolean ignoreTargetSecurity) {
        int opCode;
        if (ignoreTargetSecurity || ActivityTaskManagerService.checkComponentPermission(activityInfo.permission, callingPid, callingUid, activityInfo.applicationInfo.uid, activityInfo.exported) != -1) {
            return (activityInfo.permission == null || (opCode = AppOpsManager.permissionToOpCode(activityInfo.permission)) == -1 || this.mAtmService.getAppOpsManager().checkOp(opCode, callingUid, callingPackage) == 0 || ignoreTargetSecurity) ? 0 : 2;
        }
        return 1;
    }

    private int getActionRestrictionForCallingPackage(String action, String callingPackage, int callingPid, int callingUid) {
        String permission;
        if (action == null || (permission = ACTION_TO_RUNTIME_PERMISSION.get(action)) == null) {
            return 0;
        }
        try {
            PackageInfo packageInfo = this.mContext.getPackageManager().getPackageInfo(callingPackage, 4096);
            if (!ArrayUtils.contains(packageInfo.requestedPermissions, permission)) {
                return 0;
            }
            if (ActivityTaskManagerService.checkPermission(permission, callingPid, callingUid) == -1) {
                return 1;
            }
            int opCode = AppOpsManager.permissionToOpCode(permission);
            if (opCode == -1 || this.mAtmService.getAppOpsManager().checkOp(opCode, callingUid, callingPackage) == 0) {
                return 0;
            }
            return 2;
        } catch (PackageManager.NameNotFoundException e) {
            return 0;
        }
    }

    private boolean packageIsRunningLocked(String packageName, String processName, int uid) {
        if (packageName == null || packageName.isEmpty() || processName == null || processName.isEmpty() || uid == 0) {
            return false;
        }
        if (this.mAtmService.getProcessController(processName, uid) != null) {
            return true;
        }
        SparseArray<WindowProcessController> pidMap = this.mAtmService.mProcessMap.getPidMap();
        for (int i = pidMap.size() - 1; i >= 0; i--) {
            int pid = pidMap.keyAt(i);
            WindowProcessController app = pidMap.get(pid);
            if (app != null && app.mUid == uid && app.getThread() != null && !app.isCrashing() && !app.isNotResponding() && app.mPkgList.contains(packageName)) {
                return true;
            }
        }
        return false;
    }

    public void startActivityUncheckedBefore(ActivityRecord r) {
        SmartPowerServiceStub.getInstance().onActivityStartUnchecked(r.info.name, r.getUid(), r.getPid(), r.packageName, r.launchedFromUid, r.launchedFromPid, r.launchedFromPackage, r.isColdStart);
    }

    public boolean isCarWithDisplay(RootWindowContainer rootWindowContainer, ActivityOptions options) {
        if (rootWindowContainer == null) {
            return false;
        }
        int displayId = 0;
        if (options != null) {
            displayId = options.getLaunchDisplayId();
        } else {
            Task focusedRootTask = rootWindowContainer.getTopDisplayFocusedRootTask();
            if (focusedRootTask != null) {
                displayId = focusedRootTask.getDisplayId();
            }
        }
        DisplayContent displayContent = rootWindowContainer.getDisplayContent(displayId);
        return isCarWithDisplay(displayContent);
    }

    public void logStartActivityError(int err, Intent intent) {
        try {
            switch (err) {
                case -97:
                    Slog.e(TAG, "Error: Activity not started, voice control not allowed for: " + intent);
                    break;
                case -96:
                case -95:
                case -94:
                default:
                    Slog.e(TAG, "Error: Activity not started, unknown error code " + err);
                    break;
                case -93:
                    Slog.e(TAG, "Error: Activity not started, you requested to both forward and receive its result");
                    break;
                case -92:
                    Slog.e(TAG, "Error: Activity class " + intent.getComponent().toShortString() + " does not exist.");
                    break;
                case -91:
                    Slog.e(TAG, "Error: Activity not started, unable to resolve " + intent.toString());
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
