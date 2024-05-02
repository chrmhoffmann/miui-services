package com.android.server.wm;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.app.ActivityClient;
import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.ActivityTaskManager;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.IActivityController;
import android.app.MiuiThemeHelper;
import android.app.NotificationManager;
import android.app.StatusBarManager;
import android.app.TaskSnapshotHelperStub;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ParceledListSlice;
import android.content.res.Configuration;
import android.content.res.MiuiResources;
import android.database.ContentObserver;
import android.graphics.GraphicBuffer;
import android.graphics.Rect;
import android.miui.AppOpsUtils;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.server.am.SplitScreenReporter;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.EventLog;
import android.util.MiuiMultiWindowUtils;
import android.util.Slog;
import android.view.DisplayInfo;
import android.view.SurfaceControl;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import com.android.internal.app.IPerfShielder;
import com.android.internal.os.BackgroundThread;
import com.android.internal.protolog.ProtoLogGroup;
import com.android.server.LocalServices;
import com.android.server.MiuiFgThread;
import com.android.server.am.SmartPowerService;
import com.android.server.pm.PackageManagerService;
import com.android.server.wm.ActivityRecord;
import com.android.server.wm.ForegroundInfoManager;
import com.android.server.wm.MiuiOrientationImpl;
import com.miui.base.MiuiStubRegistry;
import com.miui.hybrid.hook.HookClient;
import com.miui.server.PerfShielderService;
import com.miui.server.process.ProcessManagerInternal;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import miui.mqsas.sdk.MQSEventManagerDelegate;
import miui.mqsas.sdk.event.PackageForegroundEvent;
import miui.os.Build;
import miui.os.DeviceFeature;
import org.json.JSONArray;
/* loaded from: classes.dex */
public class ActivityTaskManagerServiceImpl extends ActivityTaskManagerServiceStub {
    private static final int CONTINUITY_NOTIFICATION_ID = 9990;
    private static final String EXPAND_DOCK = "expand_dock";
    private static final String EXPAND_OTHER = "expand_other";
    private static final String GESTURE_LEFT_TO_RIGHT = "gesture_left_to_right";
    private static final String GESTURE_RIGHT_TO_LEFT = "gesture_right_to_left";
    public static final String KEY_MULTI_FINGER_SLIDE = "multi_finger_slide";
    private static final String MIUI_THEMEMANAGER_PKG = "com.android.thememanager";
    private static final String PACKAGE_NAME_CAMERA = "com.android.camera";
    private static final String PRESS_META_KEY_AND_W = "press_meta_key_and_w";
    private static final String SPLIT_SCREEN_BLACK_LIST = "miui_resize_black_list";
    private static final String SPLIT_SCREEN_BLACK_LIST_FOR_FOLD = "miui_resize_black_list_for_fold";
    private static final String SPLIT_SCREEN_BLACK_LIST_FOR_PAD = "miui_resize_black_list_for_pad";
    private static final String SPLIT_SCREEN_MODULE_NAME = "split_screen_applist";
    private static final String SUBSCREEN_MAIN_ACTIVITY = "com.xiaomi.misubscreenui.SubScreenMainActivity";
    private static final String SUBSCREEN_PKG = "com.xiaomi.misubscreenui";
    private static final String TAG = "ATMSImpl";
    private static final String THREE_GESTURE_DOCK_TASK = "three_gesture_dock_task";
    private static final int WIDE_SCREEN_SIZE = 600;
    private static final HashSet<String> mIgnoreUriCheckPkg;
    private static int mLastMainDisplayTopTaskId;
    private static IPerfShielder sPerfService;
    private static boolean sPerfServiceObtained;
    private static boolean sSystemBootCompleted;
    private AppCompatTask mAppCompatTask;
    public ActivityTaskManagerService mAtmService;
    public Context mContext;
    public MiuiOrientationImpl.FullScreenPackageManager mFullScreenPackageManager;
    private volatile boolean mInAnimationOut;
    private boolean mIsInUpdateMultiWindowState;
    private MiuiSizeCompatInternal mMiuiSizeCompatIn;
    private NotificationManager mNotificationManager;
    public PackageConfigurationController mPackageConfigurationController;
    String mPackageHoldOn;
    private PackageManagerService.IPackageManagerImpl mPackageManager;
    public PackageSettingsManager mPackageSettingsManager;
    ProcessManagerInternal mProcessManagerIn;
    private int mRestartingTaskId;
    private SurfaceControl mScreenshotLayer;
    private StatusBarManager mStatusBarService;
    private String mTargePackageName;
    private ActivityTaskSupervisor mTaskSupervisor;
    private static final String LOG_TAG = MultiWindowState.class.getSimpleName();
    private static final Uri URI_CLOUD_ALL_DATA_NOTIFY = Uri.parse("content://com.android.settings.cloud.CloudSettings/cloud_all_data/notify");
    private static final Interpolator FAST_OUT_SLOW_IN_REVERSE = new PathInterpolator(0.8f, MiuiFreeformPinManagerService.EDGE_AREA, 0.6f, 1.0f);
    private static final List<PackageForegroundEvent> sCachedForegroundPackageList = new ArrayList();
    private static final int PACKAGE_FORE_BUFFER_SIZE = SystemProperties.getInt("sys.proc.fore_pkg_buffer", 15);
    private static String lastForegroundPkg = null;
    private static ApplicationInfo lastMultiWindowAppInfo = null;
    private ArrayMap<WindowContainer, MultiWindowState> mStateMap = new ArrayMap<>();
    private boolean mAppContinuityIsUnfocused = false;
    private HashSet<String> mResizeBlackList = new HashSet<>();
    private HashSet<String> mResizeWhiteList = new HashSet<>();
    private boolean isHasActivityControl = false;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public enum MultiWindowState {
        RESUMED,
        EXIT
    }

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<ActivityTaskManagerServiceImpl> {

        /* compiled from: ActivityTaskManagerServiceImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final ActivityTaskManagerServiceImpl INSTANCE = new ActivityTaskManagerServiceImpl();
        }

        public ActivityTaskManagerServiceImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public ActivityTaskManagerServiceImpl provideNewInstance() {
            return new ActivityTaskManagerServiceImpl();
        }
    }

    static {
        HashSet<String> hashSet = new HashSet<>();
        mIgnoreUriCheckPkg = hashSet;
        hashSet.add(ActivityTaskSupervisorImpl.MIUI_APP_LOCK_PACKAGE_NAME);
        hashSet.add("com.miui.mishare.connectivity");
        hashSet.add("com.miui.securitycore");
    }

    public static ActivityTaskManagerServiceImpl getInstance() {
        return (ActivityTaskManagerServiceImpl) ActivityTaskManagerServiceStub.get();
    }

    void init(ActivityTaskManagerService atms, Context context) {
        this.mAtmService = atms;
        this.mTaskSupervisor = atms.mTaskSupervisor;
        this.mContext = context;
        MiuiOrientationImpl.getInstance().init(this.mContext, this);
        ActivityStarterImpl.getInstance().init(atms, this.mContext);
    }

    void onSystemReady() {
        MiuiSizeCompatInternal miuiSizeCompatInternal = (MiuiSizeCompatInternal) LocalServices.getService(MiuiSizeCompatInternal.class);
        this.mMiuiSizeCompatIn = miuiSizeCompatInternal;
        if (miuiSizeCompatInternal != null) {
            miuiSizeCompatInternal.onSystemReady(this.mAtmService);
        }
        this.mFullScreenPackageManager = MiuiOrientationImpl.getInstance().getPackageManager();
        MiuiOrientationImpl.getInstance().onSystemReady();
        this.mPackageSettingsManager = new PackageSettingsManager(this);
        PackageConfigurationController packageConfigurationController = new PackageConfigurationController(this.mAtmService);
        this.mPackageConfigurationController = packageConfigurationController;
        packageConfigurationController.registerPolicy(new FoldablePackagePolicy(this));
        this.mPackageConfigurationController.startThread();
        this.mPackageManager = ServiceManager.getService("package");
        this.mNotificationManager = (NotificationManager) this.mContext.getSystemService("notification");
        this.mStatusBarService = (StatusBarManager) this.mContext.getSystemService("statusbar");
        this.mProcessManagerIn = (ProcessManagerInternal) LocalServices.getService(ProcessManagerInternal.class);
        ActivityStarterImpl.getInstance().onSystemReady();
        registerSubScreenSwitchObserver(this.mContext);
        this.mAtmService.mH.post(new Runnable() { // from class: com.android.server.wm.ActivityTaskManagerServiceImpl$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                ActivityTaskManagerServiceImpl.this.m1478xf51d6613();
            }
        });
    }

    public MiuiSizeCompatInternal getMiuiSizeCompatIn() {
        return this.mMiuiSizeCompatIn;
    }

    public boolean inMiuiGameSizeCompat(String pkgName) {
        MiuiSizeCompatInternal miuiSizeCompatInternal = this.mMiuiSizeCompatIn;
        return miuiSizeCompatInternal != null && miuiSizeCompatInternal.inMiuiGameSizeCompat(pkgName);
    }

    public MiuiActivityController getMiuiActivityController() {
        return MiuiActivityControllerImpl.INSTANCE;
    }

    public Intent hookStartActivity(Intent intent, String callingPackage) {
        return HookClient.redirectStartActivity(intent, callingPackage);
    }

    public String hookGetCallingPkg(IBinder token, String originCallingPkg) {
        String hostApp = null;
        synchronized (this.mAtmService.mGlobalLock) {
            ActivityRecord r = ActivityRecord.isInRootTaskLocked(token);
            if (r != null) {
                hostApp = r.packageName;
            }
        }
        return HookClient.hookGetCallingPkg(hostApp, originCallingPkg);
    }

    boolean isGetTasksOpAllowed(String caller, int pid, int uid) {
        if (!AppOpsUtils.isXOptMode() && "getRunningAppProcesses".equals(caller)) {
            String packageName = null;
            synchronized (this.mAtmService.mGlobalLock) {
                WindowProcessController wpc = this.mAtmService.mProcessMap.getProcess(pid);
                if (wpc != null && wpc.mInfo != null) {
                    packageName = wpc.mInfo.packageName;
                }
            }
            if (packageName == null) {
                return false;
            }
            AppOpsManager opsManager = this.mAtmService.getAppOpsManager();
            return opsManager.checkOp(10019, uid, packageName) == 0;
        }
        return false;
    }

    public void onConfigurationChanged(int displayId) {
    }

    boolean ignoreSpecifiedSource(String pkg) {
        return mIgnoreUriCheckPkg.contains(pkg);
    }

    public void onFreeFormToFullScreen(final ActivityRecord r) {
        if (r == null || r.app == null) {
            return;
        }
        final ActivityRecord.State state = r.getState();
        final int pid = r.app.getPid();
        MiuiFgThread.getHandler().post(new Runnable() { // from class: com.android.server.wm.ActivityTaskManagerServiceImpl.1
            @Override // java.lang.Runnable
            public void run() {
                ActivityTaskManagerServiceImpl.this.onForegroundActivityChanged(r, state, pid, null);
            }
        });
    }

    public void onForegroundActivityChangedLocked(final ActivityRecord r) {
        if (r.app == null || r.getRootTask() == null) {
            return;
        }
        if (r.getRootTask().getWindowingMode() == 5) {
            Slog.i(TAG, "do not report freeform event");
            return;
        }
        SmartPowerService.getInstance().onForegroundActivityChangedLocked(r.info.name, r.getUid(), r.getPid(), r.packageName, r.launchedFromUid, r.launchedFromPid, r.launchedFromPackage, r.isColdStart);
        if (PreloadStateManagerImpl.getInstance().isPreloadDisplayId(r.getDisplayId())) {
            Slog.i(TAG, "do not report preloadApp event");
        } else if (DeviceFeature.IS_SUBSCREEN_DEVICE && 2 == r.getDisplayId()) {
            Slog.i(TAG, "do not report subscreen event");
        } else {
            final ApplicationInfo multiWindowAppInfo = this.mProcessManagerIn.getMultiWindowForegroundAppInfoLocked();
            final ActivityRecord.State state = r.getState();
            final int pid = r.app.getPid();
            MiuiFgThread.getHandler().post(new Runnable() { // from class: com.android.server.wm.ActivityTaskManagerServiceImpl.2
                @Override // java.lang.Runnable
                public void run() {
                    ActivityTaskManagerServiceImpl.this.onForegroundActivityChanged(r, state, pid, multiWindowAppInfo);
                }
            });
        }
    }

    void onForegroundActivityChanged(ActivityRecord record, ActivityRecord.State state, int pid, ApplicationInfo multiWindowAppInfo) {
        if (record == null || record.app == null || TextUtils.isEmpty(record.packageName)) {
            Slog.w(TAG, "next or next process is null, skip report!");
            return;
        }
        synchronized (record.mAtmService.mGlobalLock) {
            if (!record.isTopRunningActivity() && this.mAtmService.isInSplitScreenWindowingMode()) {
                Slog.w(TAG, "Don't report foreground because " + record.shortComponentName + " is not top running.");
                return;
            }
            if (!TextUtils.equals(record.packageName, lastForegroundPkg) || lastMultiWindowAppInfo != multiWindowAppInfo) {
                this.mProcessManagerIn.notifyForegroundInfoChanged(new ForegroundInfoManager.FgActivityChangedInfo(record, state, pid, multiWindowAppInfo));
                reportPackageForeground(record, pid, lastForegroundPkg);
                lastForegroundPkg = record.packageName;
                lastMultiWindowAppInfo = multiWindowAppInfo;
            }
            this.mProcessManagerIn.notifyActivityChanged(record.mActivityComponent);
        }
    }

    public static void onForegroundWindowChanged(WindowProcessController app, ActivityInfo info, ActivityRecord record, ActivityRecord.State state) {
        if (app != null && info != null) {
            ((ProcessManagerInternal) LocalServices.getService(ProcessManagerInternal.class)).notifyForegroundWindowChanged(new ForegroundInfoManager.FgWindowChangedInfo(record, info.applicationInfo, app.getPid()));
        }
    }

    private static void reportPackageForeground(ActivityRecord record, int pid, String lastPkgName) {
        PackageForegroundEvent event = new PackageForegroundEvent();
        event.setPackageName(record.packageName);
        event.setComponentName(record.shortComponentName);
        event.setIdentity(System.identityHashCode(record));
        event.setPid(pid);
        event.setForegroundTime(SystemClock.uptimeMillis());
        event.setColdStart(record.isColdStart);
        event.setLastPackageName(lastPkgName);
        List<PackageForegroundEvent> list = sCachedForegroundPackageList;
        list.add(event);
        if (list.size() >= PACKAGE_FORE_BUFFER_SIZE && isSystemBootCompleted()) {
            Slog.d(TAG, "Begin to report package foreground events...");
            List<PackageForegroundEvent> events = new ArrayList<>();
            events.addAll(list);
            list.clear();
            reportPackageForegroundEvents(events);
        }
    }

    private static void reportPackageForegroundEvents(List<PackageForegroundEvent> events) {
        final ParceledListSlice<PackageForegroundEvent> reportEvents = new ParceledListSlice<>(events);
        BackgroundThread.getHandler().post(new Runnable() { // from class: com.android.server.wm.ActivityTaskManagerServiceImpl.3
            @Override // java.lang.Runnable
            public void run() {
                MQSEventManagerDelegate.getInstance().reportPackageForegroundEvents(reportEvents);
            }
        });
    }

    private static boolean isSystemBootCompleted() {
        if (!sSystemBootCompleted) {
            sSystemBootCompleted = SplitScreenReporter.ACTION_ENTER_SPLIT.equals(SystemProperties.get("sys.boot_completed"));
        }
        return sSystemBootCompleted;
    }

    public String getPackageHoldOn() {
        return this.mPackageHoldOn;
    }

    public float getAspectRatio(String packageName) {
        MiuiSizeCompatInternal miuiSizeCompatInternal = this.mMiuiSizeCompatIn;
        if (miuiSizeCompatInternal == null) {
            return -1.0f;
        }
        return miuiSizeCompatInternal.getAspectRatioByPackage(packageName);
    }

    public int getAspectGravity(String packageName) {
        MiuiSizeCompatInternal miuiSizeCompatInternal = this.mMiuiSizeCompatIn;
        if (miuiSizeCompatInternal != null) {
            return miuiSizeCompatInternal.getAspectGravityByPackage(packageName);
        }
        return 17;
    }

    public int getPolicy(String packageName) {
        return this.mPackageSettingsManager.mDisplayCompatPackages.getPolicy(packageName);
    }

    public void setPackageHoldOn(ActivityTaskManagerService atms, String packageName) {
        Task stack;
        if (!TextUtils.isEmpty(packageName)) {
            List<ActivityTaskManager.RootTaskInfo> stackList = this.mAtmService.getAllRootTaskInfos();
            for (ActivityTaskManager.RootTaskInfo info : stackList) {
                if (info.topActivity != null && info.topActivity.getPackageName().equals(packageName) && (stack = atms.mRootWindowContainer.getRootTask(info.taskId)) != null && stack.getTopActivity(false, true) != null) {
                    this.mPackageHoldOn = packageName;
                    WindowManagerInternal wm = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);
                    wm.setHoldOn(stack.getTopActivity(false, true).token, true);
                    PowerManager pm = (PowerManager) this.mContext.getSystemService("power");
                    pm.goToSleep(SystemClock.uptimeMillis());
                    Slog.i(TAG, "Going to sleep and hold on, package name in hold on: " + this.mPackageHoldOn);
                    return;
                }
            }
            return;
        }
        this.mPackageHoldOn = null;
    }

    static void reportActivityLaunchTime(Object... list) {
        EventLog.writeEvent(30088, list);
    }

    static void handleExtraConfigurationChangesForSystem(int changes, Configuration newConfig) {
        MiuiResources.isPreloadedCacheEmpty();
        MiuiThemeHelper.handleExtraConfigurationChangesForSystem(changes, newConfig);
    }

    public boolean needSetWindowMode(Task task, boolean toTop, int windowingMode) {
        Task rootTask = task.getRootTask();
        if (rootTask.getWindowingMode() == 5 && windowingMode != 5) {
            if (toTop) {
                setMiuiConfigFlag(rootTask, 1, true);
                if (rootTask != null) {
                    onFreeFormToFullScreen(rootTask.topRunningActivityLocked());
                    return false;
                }
                return false;
            }
            return false;
        } else if (rootTask.getWindowingMode() != 5 && windowingMode == 5 && toTop) {
            setMiuiConfigFlag(rootTask, 2, true);
            return false;
        } else {
            return false;
        }
    }

    private static boolean setMiuiConfigFlag(Task object, int miuiConfigFlag, boolean isSetToStack) {
        try {
            Method method = Task.class.getDeclaredMethod("setMiuiConfigFlag", Integer.TYPE, Boolean.TYPE);
            method.setAccessible(true);
            method.invoke(object, Integer.valueOf(miuiConfigFlag), Boolean.valueOf(isSetToStack));
            return true;
        } catch (Exception e) {
            Slog.d(TAG, "setMiuiConfigFlag:" + e.toString());
            return false;
        }
    }

    public static int handleFreeformModeRequst(IBinder token, int cmd, Context mContext) {
        int result = -1;
        long ident = Binder.clearCallingIdentity();
        try {
            ActivityRecord r = ActivityRecord.forTokenLocked(token);
            int i = 0;
            switch (cmd) {
                case 0:
                    if (r != null) {
                        i = r.getWindowingMode();
                    }
                    result = i;
                    break;
                case 1:
                    Settings.Secure.putString(mContext.getContentResolver(), "gamebox_stick", r.getTask().getBaseIntent().getComponent().flattenToShortString());
                    break;
                case 2:
                    Settings.Secure.putString(mContext.getContentResolver(), "gamebox_stick", "");
                    break;
                case 3:
                    String component = Settings.Secure.getString(mContext.getContentResolver(), "gamebox_stick");
                    if (r != null && r.getTask().getBaseIntent().getComponent().flattenToShortString().equals(component)) {
                        i = 1;
                    }
                    result = i;
                    break;
            }
            return result;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private static IPerfShielder getPerfService() {
        if (!sPerfServiceObtained) {
            sPerfService = IPerfShielder.Stub.asInterface(ServiceManager.getService(PerfShielderService.SERVICE_NAME));
            sPerfServiceObtained = true;
        }
        return sPerfService;
    }

    static void setSchedFgPid(int pid) {
        IPerfShielder perfService = getPerfService();
        if (perfService != null) {
            try {
                perfService.setSchedFgPid(pid);
            } catch (RemoteException e) {
            }
        }
    }

    public void updateResizeBlackList(Context context) {
        String data;
        try {
            if (Build.IS_TABLET) {
                data = MiuiSettings.SettingsCloudData.getCloudDataString(context.getContentResolver(), SPLIT_SCREEN_MODULE_NAME, SPLIT_SCREEN_BLACK_LIST_FOR_PAD, (String) null);
            } else if (SystemProperties.getInt("persist.sys.muiltdisplay_type", 0) == 2) {
                data = MiuiSettings.SettingsCloudData.getCloudDataString(context.getContentResolver(), SPLIT_SCREEN_MODULE_NAME, SPLIT_SCREEN_BLACK_LIST_FOR_FOLD, (String) null);
            } else {
                data = MiuiSettings.SettingsCloudData.getCloudDataString(context.getContentResolver(), SPLIT_SCREEN_MODULE_NAME, SPLIT_SCREEN_BLACK_LIST, (String) null);
            }
            if (!TextUtils.isEmpty(data)) {
                JSONArray apps = new JSONArray(data);
                for (int i = 0; i < apps.length(); i++) {
                    this.mResizeBlackList.add(apps.getString(i));
                }
            }
        } catch (Exception e) {
            Slog.e(TAG, "Get splitscreen blacklist from xml: ", e);
        }
    }

    private void getSplitScreenBlackListFromXml() {
        if (Build.IS_TABLET) {
            this.mResizeBlackList.add("com.android.settings");
            this.mResizeBlackList.addAll(Arrays.asList(this.mContext.getResources().getStringArray(285409376)));
        } else if (SystemProperties.getInt("persist.sys.muiltdisplay_type", 0) == 2) {
            this.mResizeBlackList.add("com.android.settings");
            this.mResizeBlackList.addAll(Arrays.asList(this.mContext.getResources().getStringArray(285409375)));
        } else {
            this.mResizeBlackList.addAll(Arrays.asList(this.mContext.getResources().getStringArray(285409374)));
        }
    }

    private void getSplitScreenWhiteListFromXml() {
        if (SystemProperties.getInt("persist.sys.muiltdisplay_type", 0) == 2) {
            this.mResizeWhiteList.addAll(Arrays.asList(this.mContext.getResources().getStringArray(285409377)));
        }
    }

    /* renamed from: registerObserver */
    public void m1478xf51d6613() {
        getSplitScreenBlackListFromXml();
        getSplitScreenWhiteListFromXml();
        ContentObserver observer = new ContentObserver(null) { // from class: com.android.server.wm.ActivityTaskManagerServiceImpl.4
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                ActivityTaskManagerServiceImpl activityTaskManagerServiceImpl = ActivityTaskManagerServiceImpl.this;
                activityTaskManagerServiceImpl.updateResizeBlackList(activityTaskManagerServiceImpl.mContext);
            }
        };
        this.mContext.getContentResolver().registerContentObserver(URI_CLOUD_ALL_DATA_NOTIFY, false, observer, -1);
        observer.onChange(false);
    }

    public boolean inResizeBlackList(String packageName) {
        return this.mResizeBlackList.contains(packageName);
    }

    public boolean inResizeWhiteList(String packageName) {
        return this.mResizeWhiteList.contains(packageName);
    }

    public void restartSubScreenUiIfNeeded(int userId, String reason) {
        try {
            ApplicationInfo aInfo = AppGlobals.getPackageManager().getApplicationInfo(SUBSCREEN_PKG, 1024L, userId);
            if (aInfo == null) {
                Slog.d(TAG, "aInfo is null when start subscreenui for user " + userId);
            } else {
                restartSubScreenUiIfNeeded(aInfo, reason);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void restartSubScreenUiIfNeeded(final ApplicationInfo info, final String reason) {
        if (info == null || !SUBSCREEN_PKG.equals(info.packageName) || isCTS() || this.mAtmService == null) {
            return;
        }
        BackgroundThread.getHandler().postDelayed(new Runnable() { // from class: com.android.server.wm.ActivityTaskManagerServiceImpl.5
            @Override // java.lang.Runnable
            public void run() {
                try {
                    ActivityTaskManagerServiceImpl.this.startSubScreenUi(info, reason);
                } catch (Exception e) {
                    Slog.e(ActivityTaskManagerServiceImpl.TAG, e.toString());
                }
            }
        }, 1000L);
    }

    public void startSubScreenUi(ApplicationInfo info, String reason) {
        try {
            int userId = UserHandle.getUserId(info.uid);
            if (userId == this.mAtmService.getCurrentUserId() && isSubScreenFeatureOn(this.mAtmService.mContext, userId) && !shouldNotStartSubscreen()) {
                ActivityOptions options = ActivityOptions.makeBasic();
                options.setLaunchWindowingMode(1);
                options.setLaunchDisplayId(2);
                Intent intent = new Intent("android.intent.action.MAIN");
                ComponentName componentName = new ComponentName(SUBSCREEN_PKG, SUBSCREEN_MAIN_ACTIVITY);
                intent.setComponent(componentName);
                intent.setFlags(268435456);
                ActivityInfo aInfo = AppGlobals.getPackageManager().getActivityInfo(componentName, 1024L, userId);
                Slog.d(TAG, "starSubScreenActivity: " + reason + " for user " + userId);
                this.mAtmService.getActivityStartController().obtainStarter(intent, "starSubScreenActivity: " + reason).setCallingUid(0).setUserId(userId).setActivityInfo(aInfo).setActivityOptions(options.toBundle()).execute();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isCTS() {
        return !SystemProperties.getBoolean("persist.sys.miui_optimization", !SplitScreenReporter.ACTION_ENTER_SPLIT.equals(SystemProperties.get("ro.miui.cts")));
    }

    private boolean isSubScreenFeatureOn(Context context, int userId) {
        return DeviceFeature.IS_SUBSCREEN_DEVICE && context != null && MiuiSettings.System.getBooleanForUser(context.getContentResolver(), "subscreen_switch", false, userId);
    }

    private boolean shouldNotStartSubscreen() {
        DisplayContent display = this.mAtmService.mRootWindowContainer.getDisplayContent(2);
        ActivityRecord topRunningActivity = null;
        boolean switchUser = false;
        if (display != null) {
            topRunningActivity = display.topRunningActivity();
            switchUser = (topRunningActivity == null || topRunningActivity.mUserId == this.mAtmService.getCurrentUserId()) ? false : true;
            Slog.d(TAG, "shouldNotStartSubscreen topRunningActivity=" + topRunningActivity + ", switchUser=" + switchUser);
        }
        if (display != null) {
            return display.isSleeping() && topRunningActivity != null && !switchUser;
        }
        return true;
    }

    void registerSubScreenSwitchObserver(Context context) {
        if (!DeviceFeature.IS_SUBSCREEN_DEVICE || context == null) {
            return;
        }
        context.getContentResolver().registerContentObserver(Settings.System.getUriFor("subscreen_switch"), false, new ContentObserver(BackgroundThread.getHandler()) { // from class: com.android.server.wm.ActivityTaskManagerServiceImpl.6
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri, int flags) {
                super.onChange(selfChange, uri, flags);
                ActivityTaskManagerServiceImpl.this.restartSubScreenUiIfNeeded(flags, "subscreen feature switch on");
            }
        }, -1);
    }

    Configuration getGlobalConfigurationForMiui(ActivityTaskManagerService atms, WindowProcessController app) {
        if (atms == null || app == null || !Build.IS_MIUI || !atms.isInSplitScreenWindowingMode() || !"com.android.thememanager".equals(app.mInfo.packageName)) {
            return null;
        }
        return atms.getGlobalConfiguration();
    }

    public ActivityInfo getLastResumedActivityInfo() {
        int uid = UserHandle.getAppId(Binder.getCallingUid());
        int pid = Binder.getCallingPid();
        ActivityInfo activityInfo = null;
        if (uid != 1000 && ActivityTaskManagerService.checkPermission("android.permission.REAL_GET_TASKS", pid, uid) != 0) {
            Slog.d(TAG, "permission denied for, callingPid:" + pid + " , callingUid:" + uid + ", requires: android.Manifest.permission.REAL_GET_TASKS");
            return null;
        }
        synchronized (this.mAtmService.mGlobalLock) {
            ActivityRecord activity = this.mAtmService.mLastResumedActivity;
            if (activity != null) {
                activityInfo = activity.info;
            }
        }
        return activityInfo;
    }

    public boolean shouldExcludeTaskFromRecents(Task task) {
        if (task == null || task.getBaseIntent() == null || task.getBaseIntent().getComponent() == null) {
            return false;
        }
        String packageName = task.getBaseIntent().getComponent().getPackageName();
        return SUBSCREEN_PKG.equals(packageName) && 2 == task.getDisplayId();
    }

    public Task anyExistingTaskForIdLocked(int taskId) {
        Task anyTaskForId;
        synchronized (this.mAtmService.mGlobalLock) {
            anyTaskForId = this.mAtmService.mRootWindowContainer.anyTaskForId(taskId, 0);
        }
        return anyTaskForId;
    }

    public boolean hasMetaData(String packageName, String metaDataKey) {
        try {
            ApplicationInfo applicationInfo = this.mPackageManager.getApplicationInfo(packageName, 128L, UserHandle.myUserId());
            Bundle metaData = applicationInfo.metaData;
            if (metaData != null) {
                return metaData.get(metaDataKey) != null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean getMetaDataBoolean(String packageName, String metaDataKey) {
        try {
            ApplicationInfo applicationInfo = this.mPackageManager.getApplicationInfo(packageName, 128L, UserHandle.myUserId());
            Bundle metaData = applicationInfo.metaData;
            if (metaData != null && metaData.get(metaDataKey) != null) {
                return metaData.getBoolean(metaDataKey);
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public float getMetaDataFloat(String packageName, String metaDataKey) {
        try {
            ApplicationInfo applicationInfo = this.mPackageManager.getApplicationInfo(packageName, 128L, UserHandle.myUserId());
            Bundle metaData = applicationInfo.metaData;
            if (metaData != null && metaData.get(metaDataKey) != null) {
                return metaData.getFloat(metaDataKey);
            }
            return MiuiFreeformPinManagerService.EDGE_AREA;
        } catch (Exception e) {
            e.printStackTrace();
            return MiuiFreeformPinManagerService.EDGE_AREA;
        }
    }

    boolean executeShellCommand(String command, String[] args, PrintWriter pw) {
        MiuiSizeCompatInternal miuiSizeCompatInternal = this.mMiuiSizeCompatIn;
        if (miuiSizeCompatInternal != null && miuiSizeCompatInternal.executeShellCommand(command, args, pw)) {
            return true;
        }
        return this.mPackageConfigurationController.executeShellCommand(command, args, pw);
    }

    public ActivityManager.RunningTaskInfo getSplitTaskInfo(IBinder token) {
        synchronized (this.mAtmService.mGlobalLock) {
            ActivityRecord record = ActivityRecord.forTokenLocked(token);
            if (record != null && record.getTask() != null && record.getTask().mIsSplitMode) {
                return record.getTask().getTaskInfo();
            }
            return null;
        }
    }

    public boolean isFixedAspectRatioPackage(String packageName, int userId) {
        MiuiSizeCompatInternal miuiSizeCompatInternal = this.mMiuiSizeCompatIn;
        return miuiSizeCompatInternal != null && miuiSizeCompatInternal.getAspectRatioByPackage(packageName) > MiuiFreeformPinManagerService.EDGE_AREA;
    }

    public void dump(PrintWriter pw, String[] args) {
        if (args == null || args.length <= 1) {
            pw.println("dump nothing for ext!");
            return;
        }
        String extCmd = args[1];
        if ("packages".equals(extCmd)) {
            this.mPackageSettingsManager.dump(pw, "");
            this.mMiuiSizeCompatIn.dump(pw, "");
        }
    }

    public boolean updateMultiWindowState(WindowContainer wc) {
        MultiWindowState result = MultiWindowState.EXIT;
        boolean z = true;
        if (wc == null || wc.asTask() == null || this.mIsInUpdateMultiWindowState) {
            return result == MultiWindowState.RESUMED;
        }
        this.mIsInUpdateMultiWindowState = true;
        try {
            WindowContainer rootTask = wc.asTask().getRootTask();
            if (!((Task) rootTask).mCreatedByOrganizer && wc.getWindowingMode() != 6) {
                if (result != MultiWindowState.RESUMED) {
                    z = false;
                }
                return z;
            } else if (!((Task) rootTask).mCreatedByOrganizer) {
                if (result != MultiWindowState.RESUMED) {
                    z = false;
                }
                return z;
            } else {
                if (rootTask.hasChild()) {
                    MultiWindowState cacheState = this.mStateMap.get(rootTask);
                    if (cacheState != null) {
                        Slog.i(LOG_TAG, "Cache state:" + cacheState.toString() + " rootTask:" + rootTask + " wc:" + wc);
                        if (cacheState == MultiWindowState.RESUMED) {
                            clearSizeCompatModeIfNeed(rootTask);
                        }
                        if (cacheState != MultiWindowState.RESUMED) {
                            z = false;
                        }
                        return z;
                    }
                    Slog.i(LOG_TAG, "put state: rootTask:" + rootTask + " wc:" + wc);
                    result = this.mStateMap.put(rootTask, MultiWindowState.RESUMED);
                    clearSizeCompatModeIfNeed(rootTask);
                } else {
                    Slog.i(LOG_TAG, "remove state. rootTask:" + rootTask + " wc:" + wc);
                    this.mStateMap.remove(rootTask);
                }
                this.mIsInUpdateMultiWindowState = false;
                return result == MultiWindowState.RESUMED;
            }
        } finally {
            this.mIsInUpdateMultiWindowState = false;
        }
    }

    private void clearSizeCompatModeIfNeed(final Task rootTask) {
        rootTask.forAllTasks(new Consumer() { // from class: com.android.server.wm.ActivityTaskManagerServiceImpl$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ActivityTaskManagerServiceImpl.lambda$clearSizeCompatModeIfNeed$3(rootTask, (Task) obj);
            }
        });
    }

    public static /* synthetic */ boolean lambda$clearSizeCompatModeIfNeed$1(ActivityRecord ar) {
        return ar.hasSizeCompatBounds() || ar.getCompatDisplayInsets() != null;
    }

    public static /* synthetic */ void lambda$clearSizeCompatModeIfNeed$3(Task rootTask, Task task) {
        if (task.forAllActivities(new Predicate() { // from class: com.android.server.wm.ActivityTaskManagerServiceImpl$$ExternalSyntheticLambda3
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return ActivityTaskManagerServiceImpl.lambda$clearSizeCompatModeIfNeed$1((ActivityRecord) obj);
            }
        }, true)) {
            Slog.i(LOG_TAG, "clear task" + rootTask.mTaskId + " size compat mode.");
            task.forAllActivities(new Consumer() { // from class: com.android.server.wm.ActivityTaskManagerServiceImpl$$ExternalSyntheticLambda4
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    ((ActivityRecord) obj).clearSizeCompatMode();
                }
            });
        }
    }

    public void onMetaKeyCombination() {
        MiuiFreeFormActivityStack mffas;
        ActivityTaskManagerService activityTaskManagerService = this.mAtmService;
        if (activityTaskManagerService == null) {
            return;
        }
        synchronized (activityTaskManagerService.mGlobalLock) {
            Task topFocusedStack = this.mAtmService.getTopDisplayFocusedRootTask();
            if (topFocusedStack == null) {
                Slog.d(TAG, "no stack focued, do noting.");
                return;
            }
            int windowingMode = topFocusedStack.getWindowingMode();
            switch (windowingMode) {
                case 1:
                    if (!exitSplitScreenIfNeed(topFocusedStack) && !topFocusedStack.isActivityTypeHomeOrRecents()) {
                        topFocusedStack.moveTaskToBack(topFocusedStack);
                        break;
                    }
                    break;
                case 2:
                default:
                    return;
                case 3:
                    exitSplitScreen(true);
                    break;
                case 4:
                    exitSplitScreen(false);
                    break;
                case 5:
                    if (this.mAtmService.mMiuiFreeFormManagerService != null && (mffas = (MiuiFreeFormActivityStack) this.mAtmService.mMiuiFreeFormManagerService.getMiuiFreeFormActivityStack(topFocusedStack.getRootTaskId())) != null && mffas.isInMiniFreeFormMode()) {
                        return;
                    }
                    ActivityRecord ar = topFocusedStack.getTopActivity(true, true);
                    if (ar != null) {
                        topFocusedStack.setAlwaysOnTop(false);
                        ActivityClient.getInstance().moveActivityTaskToBack(ar.token, false);
                        break;
                    } else {
                        topFocusedStack.setAlwaysOnTop(false);
                        topFocusedStack.moveTaskToBack(topFocusedStack);
                        break;
                    }
                    break;
            }
        }
    }

    private boolean exitSplitScreenIfNeed(WindowContainer wc) {
        boolean isExpandDock = false;
        if (!isVerticalSplit() || !isInSystemSplitScreen(wc)) {
            return false;
        }
        Task rootTask = wc.asTask().getRootTask();
        Task topLeafTask = rootTask.getTopLeafTask();
        if (topLeafTask.getBounds().left == 0) {
            isExpandDock = true;
        }
        exitSplitScreen(isExpandDock);
        return true;
    }

    private void exitSplitScreen(boolean isExpandDock) {
        String str = isExpandDock ? EXPAND_DOCK : EXPAND_OTHER;
        MiuiSettings.System.putString(this.mAtmService.mUiContext.getContentResolver(), PRESS_META_KEY_AND_W, str);
    }

    public boolean shouldExcludeFromRecentsFreeform(Task task) {
        return false;
    }

    public void notifyActivityResumed(ActivityRecord r, WindowManagerService wms) {
        if (r == null) {
            if (ProtoLogGroup.WM_DEBUG_STARTING_WINDOW.isLogToLogcat()) {
                Slog.i(TAG, "NotifyActivityResumed failed, activity = null");
            }
        } else if (r.inMultiWindowMode()) {
            if (ProtoLogGroup.WM_DEBUG_STARTING_WINDOW.isLogToLogcat()) {
                Slog.i(TAG, "In MultiWindowMode, wont do snapshot, return !....");
            }
        } else if (TaskSnapshotControllerInjectorStub.get().canTakeSnapshot(r)) {
            wms.mTaskSnapshotController.notifyAppResumed(r, true);
        } else if (ProtoLogGroup.WM_DEBUG_STARTING_WINDOW.isLogToLogcat()) {
            Slog.i(TAG, "No snapshot since not start by launcher, activity=" + r.mActivityComponent.flattenToShortString());
        }
    }

    public void handleQSOnConfigureChanged(int userId, int change) {
        TaskSnapshotHelperStub.get().destroyQS(userId);
    }

    public boolean isAppSizeCompatRestarting(String pkgName) {
        MiuiSizeCompatInternal miuiSizeCompatInternal = this.mMiuiSizeCompatIn;
        return miuiSizeCompatInternal != null && miuiSizeCompatInternal.isAppSizeCompatRestarting(pkgName);
    }

    public boolean isCameraForeground() {
        return TextUtils.equals(lastForegroundPkg, "com.android.camera");
    }

    public void splitTaskIfNeed(ActivityOptions options, Task startedActivityRootTask) {
        if (startedActivityRootTask == null || options == null || this.mAtmService == null) {
            return;
        }
        Object enterApppair = MiuiMultiWindowUtils.invoke(options, "getEnterAppPair", new Object[0]);
        Object isPrimary = MiuiMultiWindowUtils.invoke(options, "getAppPairPrimary", new Object[0]);
        if (enterApppair != null && ((Boolean) enterApppair).booleanValue()) {
            Task task1 = startedActivityRootTask.getDisplayArea().getTopRootTaskInWindowingMode(1);
            if ((task1 == null || !task1.supportsMultiWindow() || task1.isActivityTypeHomeOrRecents() || !startedActivityRootTask.supportsMultiWindow()) && (task1.getBaseIntent() == null || task1.getBaseIntent().getComponent() == null || !skipTaskForMultiWindow(task1.getBaseIntent().getComponent().getPackageName()))) {
                Slog.w(TAG, "The top app or drag's app is not supports multi window.");
            } else if (task1 == startedActivityRootTask) {
                Slog.w(TAG, "The task has been front.");
            } else {
                Slog.i(TAG, "pair t1:" + task1 + " t2:" + startedActivityRootTask + " isPrimary:" + isPrimary);
                showScreenShotForSplitTask();
                startedActivityRootTask.setHasBeenVisible(true);
                startedActivityRootTask.sendTaskAppeared();
                this.mAtmService.mTaskOrganizerController.dispatchPendingEvents();
                MiuiMultiWindowUtils.invoke(this.mAtmService, "enterSplitScreen", new Object[]{Integer.valueOf(startedActivityRootTask.mTaskId), (Boolean) isPrimary});
            }
        }
    }

    /* renamed from: removeSplitTaskShotIfNeed */
    public boolean m1479xd950ccb0() {
        if (this.mScreenshotLayer != null && !this.mInAnimationOut) {
            this.mAtmService.mUiHandler.post(new Runnable() { // from class: com.android.server.wm.ActivityTaskManagerServiceImpl$$ExternalSyntheticLambda7
                @Override // java.lang.Runnable
                public final void run() {
                    ActivityTaskManagerServiceImpl.this.animationOut();
                }
            });
            return true;
        }
        return false;
    }

    public void animationOut() {
        if (this.mInAnimationOut) {
            return;
        }
        ValueAnimator anim = ValueAnimator.ofFloat(1.0f, MiuiFreeformPinManagerService.EDGE_AREA);
        final SurfaceControl.Transaction t = new SurfaceControl.Transaction();
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.server.wm.ActivityTaskManagerServiceImpl$$ExternalSyntheticLambda5
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                ActivityTaskManagerServiceImpl.this.m1477x471c4400(t, valueAnimator);
            }
        });
        anim.addListener(new Animator.AnimatorListener() { // from class: com.android.server.wm.ActivityTaskManagerServiceImpl.7
            @Override // android.animation.Animator.AnimatorListener
            public void onAnimationStart(Animator animator) {
            }

            @Override // android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator) {
                if (ActivityTaskManagerServiceImpl.this.mScreenshotLayer != null) {
                    t.remove(ActivityTaskManagerServiceImpl.this.mScreenshotLayer).apply();
                    ActivityTaskManagerServiceImpl.this.mScreenshotLayer = null;
                }
                ActivityTaskManagerServiceImpl.this.mInAnimationOut = false;
            }

            @Override // android.animation.Animator.AnimatorListener
            public void onAnimationCancel(Animator animator) {
                if (ActivityTaskManagerServiceImpl.this.mScreenshotLayer != null) {
                    t.remove(ActivityTaskManagerServiceImpl.this.mScreenshotLayer).apply();
                    ActivityTaskManagerServiceImpl.this.mScreenshotLayer = null;
                }
                ActivityTaskManagerServiceImpl.this.mInAnimationOut = false;
            }

            @Override // android.animation.Animator.AnimatorListener
            public void onAnimationRepeat(Animator animator) {
            }
        });
        anim.setDuration(300L);
        anim.setStartDelay(200L);
        anim.setInterpolator(FAST_OUT_SLOW_IN_REVERSE);
        anim.start();
        this.mInAnimationOut = true;
    }

    /* renamed from: lambda$animationOut$4$com-android-server-wm-ActivityTaskManagerServiceImpl */
    public /* synthetic */ void m1477x471c4400(SurfaceControl.Transaction t, ValueAnimator animation) {
        float alpha = ((Float) animation.getAnimatedValue()).floatValue();
        SurfaceControl surfaceControl = this.mScreenshotLayer;
        if (surfaceControl != null) {
            t.setAlpha(surfaceControl, alpha).apply();
        }
    }

    public void showScreenShotForSplitTask() {
        if (this.mScreenshotLayer != null) {
            return;
        }
        DisplayContent dc = this.mAtmService.mRootWindowContainer.getDefaultDisplay();
        DisplayInfo info = dc.getDisplayInfo();
        Rect bounds = new Rect(0, 0, info.logicalWidth, info.logicalHeight);
        IBinder displayToken = SurfaceControl.getInternalDisplayToken();
        SurfaceControl.DisplayCaptureArgs displayCaptureArgs = new SurfaceControl.DisplayCaptureArgs.Builder(displayToken).setSize(info.logicalWidth, info.logicalHeight).setSourceCrop(bounds).build();
        SurfaceControl.ScreenshotHardwareBuffer screenshotHardwareBuffer = SurfaceControl.captureDisplay(displayCaptureArgs);
        if (screenshotHardwareBuffer == null) {
            return;
        }
        GraphicBuffer buffer = GraphicBuffer.createFromHardwareBuffer(screenshotHardwareBuffer.getHardwareBuffer());
        SurfaceControl.Transaction t = new SurfaceControl.Transaction();
        SurfaceControl build = dc.makeOverlay().setName("SplitTaskScreenShot").setOpaque(true).setSecure(false).setCallsite("SplitTaskScreenShot").setBLASTLayer().build();
        this.mScreenshotLayer = build;
        t.setLayer(build, 30001);
        t.setBuffer(this.mScreenshotLayer, buffer);
        t.setColorSpace(this.mScreenshotLayer, screenshotHardwareBuffer.getColorSpace());
        t.show(this.mScreenshotLayer);
        t.apply();
        this.mAtmService.mUiHandler.postDelayed(new Runnable() { // from class: com.android.server.wm.ActivityTaskManagerServiceImpl$$ExternalSyntheticLambda6
            @Override // java.lang.Runnable
            public final void run() {
                ActivityTaskManagerServiceImpl.this.m1479xd950ccb0();
            }
        }, 1000L);
        Slog.i(TAG, "show split task screen shot layer.");
    }

    public int getOrientation(Task task) {
        if (task == null || task.getDisplayContent() == null || task.getDisplayContent().getConfiguration().smallestScreenWidthDp < WIDE_SCREEN_SIZE) {
            return -1;
        }
        Task rootTask = task.getRootTask();
        if (!rootTask.isVisible() || !rootTask.mCreatedByOrganizer || !rootTask.hasChild() || rootTask.getTopChild().getWindowingMode() != 6 || !rootTask.forAllActivities(new Predicate() { // from class: com.android.server.wm.ActivityTaskManagerServiceImpl$$ExternalSyntheticLambda1
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return ActivityTaskManagerServiceImpl.lambda$getOrientation$6((ActivityRecord) obj);
            }
        })) {
            return -1;
        }
        return 1;
    }

    public static /* synthetic */ boolean lambda$getOrientation$6(ActivityRecord a) {
        return "com.android.camera".equals(a.packageName) && a.isVisible();
    }

    public boolean isVerticalSplit() {
        return this.mAtmService.mRootWindowContainer.getDefaultTaskDisplayArea().getConfiguration().smallestScreenWidthDp >= this.mAtmService.mLargeScreenSmallestScreenWidthDp && !WindowManagerServiceImpl.getInstance().isCtsModeEnabled();
    }

    public boolean toggleSplitByGesture(boolean isLTR) {
        WindowState statusBar;
        if (!isVerticalSplit() || this.mAtmService == null) {
            return false;
        }
        if (Build.IS_TABLET && Settings.Global.getInt(this.mAtmService.mUiContext.getContentResolver(), KEY_MULTI_FINGER_SLIDE, 0) == 1 && (statusBar = this.mAtmService.mRootWindowContainer.getDefaultDisplay().getDisplayPolicy().getStatusBar()) != null && !statusBar.isVisible()) {
            Slog.i(TAG, "Ignore because MULTI_FINGER_SLIDE is setted and StatusBar is hidden.");
            return false;
        } else if (this.mAtmService.mWindowManager.mPolicy.isKeyguardLocked() || !this.mAtmService.mWindowManager.mPolicy.okToAnimate(false)) {
            Slog.i(TAG, "Ignore because keyguard is showing or turning screen off.");
            return false;
        } else if (this.mAtmService.getLockTaskModeState() == 2) {
            return false;
        } else {
            Task focusedRootTask = this.mAtmService.getTopDisplayFocusedRootTask();
            if (focusedRootTask == null) {
                Slog.i(TAG, "Ignore because get none focused task.");
                return false;
            } else if (!focusedRootTask.supportsSplitScreenWindowingMode() && !isInSystemSplitScreen(focusedRootTask)) {
                Slog.i(TAG, "Ignore because the task not support SplitScreen mode.");
                this.mAtmService.getTaskChangeNotificationController().notifyActivityDismissingDockedRootTask();
                return false;
            } else if (focusedRootTask.inFreeformWindowingMode()) {
                Slog.i(TAG, "Ignore because the task at FreeForm mode.");
                return false;
            } else if (isInSystemSplitScreen(focusedRootTask)) {
                exitSplitScreen(isLTR);
                return false;
            } else {
                String threeGestureDockTask = MiuiSettings.System.getString(this.mContext.getContentResolver(), THREE_GESTURE_DOCK_TASK, "");
                if (!TextUtils.isEmpty(threeGestureDockTask)) {
                    MiuiSettings.System.putString(this.mContext.getContentResolver(), THREE_GESTURE_DOCK_TASK, "");
                }
                if (isLTR) {
                    boolean triggered = MiuiSettings.System.putString(this.mContext.getContentResolver(), THREE_GESTURE_DOCK_TASK, GESTURE_LEFT_TO_RIGHT);
                    return triggered;
                }
                boolean triggered2 = MiuiSettings.System.putString(this.mContext.getContentResolver(), THREE_GESTURE_DOCK_TASK, GESTURE_RIGHT_TO_LEFT);
                return triggered2;
            }
        }
    }

    public boolean isInSystemSplitScreen(WindowContainer wc) {
        if (wc == null) {
            return false;
        }
        Task task = null;
        if (wc instanceof Task) {
            task = wc.asTask();
        }
        if (wc instanceof WindowState) {
            task = ((WindowState) wc).getTask();
        }
        if (wc instanceof ActivityRecord) {
            task = ((ActivityRecord) wc).getTask();
        }
        if (task == null) {
            return false;
        }
        boolean isRoot = task.isRootTask();
        if (isRoot) {
            return task.mCreatedByOrganizer && task.getChildCount() == 2 && task.getWindowingMode() == 1 && task.getTopChild() != null && task.getTopChild().asTask() != null && task.getTopChild().getWindowingMode() == 6 && task.getTopChild().hasChild();
        }
        Task rootTask = task.getRootTask();
        return rootTask != null && rootTask.mCreatedByOrganizer && rootTask.hasChild() && rootTask.getWindowingMode() == 1 && rootTask.getTopChild() != null && rootTask.getTopChild().asTask() != null && rootTask.getTopChild().getWindowingMode() == 6;
    }

    public void setActivityController(IActivityController control) {
        if (control != null) {
            this.isHasActivityControl = true;
        } else {
            this.isHasActivityControl = false;
        }
    }

    public boolean hasActivityController() {
        return this.isHasActivityControl;
    }

    public boolean shouldSkipApplyAnimation(WindowContainer container, int transit) {
        if (container == null || !container.inFreeformSmallWinMode() || !container.isEmbedded() || transit != 13) {
            return false;
        }
        Slog.d(TAG, "Skip applyAnimation in freeform");
        return true;
    }

    public boolean skipTaskForMultiWindow(String pkgName) {
        return Build.IS_TABLET && "com.android.quicksearchbox".equals(pkgName);
    }
}
