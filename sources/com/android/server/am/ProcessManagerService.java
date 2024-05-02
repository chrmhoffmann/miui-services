package com.android.server.am;

import android.app.ActivityManagerNative;
import android.app.AppOpsManager;
import android.app.INotificationManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.ParceledListSlice;
import android.content.pm.Signature;
import android.hardware.display.DisplayManagerInternal;
import android.os.Binder;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.os.ShellCommand;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.view.accessibility.AccessibilityManager;
import com.android.internal.os.BatteryStatsManagerStub;
import com.android.server.LocalServices;
import com.android.server.ServiceThread;
import com.android.server.am.GameProcessCompactor;
import com.android.server.am.GameProcessKiller;
import com.android.server.am.ProcessPolicy;
import com.android.server.am.ProcessStarter;
import com.android.server.wm.ForegroundInfoManager;
import com.android.server.wm.RealTimeModeControllerStub;
import com.android.server.wm.WindowProcessController;
import com.android.server.wm.WindowProcessUtils;
import com.miui.enterprise.settings.EnterpriseSettings;
import com.miui.server.migard.MiGardInternal;
import com.miui.server.process.ProcessManagerInternal;
import com.miui.server.rtboost.SchedBoostManagerInternal;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import miui.app.backup.BackupManager;
import miui.os.Build;
import miui.process.ActiveUidInfo;
import miui.process.ForegroundInfo;
import miui.process.IActivityChangeListener;
import miui.process.IForegroundInfoListener;
import miui.process.IForegroundWindowListener;
import miui.process.IMiuiApplicationThread;
import miui.process.IPreloadCallback;
import miui.process.LifecycleConfig;
import miui.process.PreloadProcessData;
import miui.process.ProcessCloudData;
import miui.process.ProcessConfig;
import miui.process.ProcessManager;
import miui.process.ProcessManagerNative;
import miui.process.RunningProcessInfo;
/* loaded from: classes.dex */
public class ProcessManagerService extends ProcessManagerNative {
    private static final String BINDER_MONITOR_FD_PATH = "/proc/mi_log/binder_delay";
    static final String CAMERA_TEST = "com.phonetest.application:CameraMemoryWatcher";
    private static final String[] CGROUP_PATH_PREFIXES;
    private static final String CGROUP_PID_PREFIX = "/pid_";
    private static final String CGROUP_PROCS = "/cgroup.procs";
    public static final boolean CONFIG_LOW_RAM;
    public static final boolean CONFIG_PER_APP_MEMCG;
    private static final boolean DEBUG = true;
    static final int MAX_PROCESS_CONFIG_HISTORY = 30;
    private static final String MEM_CGROUP_PATH = "/dev/memcg/apps/uid_";
    static final String PACKAGE_NAME_CAMERA = "com.android.camera";
    static final String PACKAGE_NAME_GALLERY = "com.miui.gallery";
    static final int RESTORE_AI_PROCESSES_INFO_MSG = 1;
    static final int SKIP_PRELOAD_COUNT_LIMIT = 2;
    private static final String TAG = "ProcessManager";
    static final int USER_OWNER = 0;
    static final int USER_XSPACE = 999;
    static String mCgroupPath;
    private AccessibilityManager mAccessibilityManager;
    private AppOpsManager mAppOpsManager;
    private FileWriter mBinderDelayWriter;
    private CameraBooster mCameraBooster;
    private Context mContext;
    private GameMemoryReclaimer mGameMemoryReclaimer;
    final MainHandler mHandler;
    private boolean mIsCameraForeground;
    private PreloadAppControllerImpl mPreloadAppController;
    private ProcessPolicy mProcessPolicy;
    private ProcessStarter mProcessStarter;
    final ServiceThread mServiceThread;
    private Set<Signature> mSystemSignatures;
    private String prev_app_name;
    private int prev_uid;
    public static final boolean CONFIG_CAM_PROTECT_PREV = SystemProperties.getBoolean("persist.sys.miui.camera.protect_prev", false);
    public static final boolean CONFIG_CAM_PROTECT_PREV_EXT = SystemProperties.getBoolean("persist.sys.miui.camera.protect_prev_ext", false);
    public static final String DEVICE = Build.DEVICE.toLowerCase();
    final ProcessConfig[] mProcessConfigHistory = new ProcessConfig[30];
    int mHistoryNext = -1;
    private boolean cameraBoost = false;
    private INotificationManager mNotificationManager = NotificationManager.getService();
    private ActivityManagerService mActivityManagerService = ActivityManagerNative.getDefault();
    private ArrayList<ProcessRecord> mLruProcesses = this.mActivityManagerService.mProcessList.getLruProcessesLOSP();
    private ProcessKiller mProcessKiller = new ProcessKiller(this.mActivityManagerService);
    private MiuiApplicationThreadManager mMiuiApplicationThreadManager = new MiuiApplicationThreadManager(this.mActivityManagerService);
    private ForegroundInfoManager mForegroundInfoManager = new ForegroundInfoManager(this);
    private SparseArray<Map<String, ProcessStarter.ProcessPriorityInfo>> mAdjBoostProcessMap = new SparseArray<>();
    private SparseArray<Map<String, ProcessStarter.ProcessPriorityInfo>> mAdjBoostProcessMapBysimply = new SparseArray<>();
    private SparseArray<Map<String, Runnable>> mAdjDeboostRunnableMap = new SparseArray<>();
    private DisplayManagerInternal mDisplayManagerInternal = (DisplayManagerInternal) LocalServices.getService(DisplayManagerInternal.class);

    static {
        boolean z = SystemProperties.getBoolean("ro.config.low_ram", false);
        CONFIG_LOW_RAM = z;
        CONFIG_PER_APP_MEMCG = SystemProperties.getBoolean("ro.config.per_app_memcg", z);
        String[] strArr = {"/acct/uid_", "/sys/fs/cgroup/uid_", "/dev/cg2_bpf/uid_"};
        CGROUP_PATH_PREFIXES = strArr;
        mCgroupPath = strArr[0];
    }

    public ProcessManagerService(Context context) {
        this.mContext = context;
        this.mAppOpsManager = (AppOpsManager) context.getSystemService("appops");
        this.mAccessibilityManager = (AccessibilityManager) this.mContext.getSystemService("accessibility");
        ServiceThread serviceThread = new ServiceThread("ProcessManager", 0, false);
        this.mServiceThread = serviceThread;
        serviceThread.start();
        MainHandler mainHandler = new MainHandler(serviceThread.getLooper());
        this.mHandler = mainHandler;
        PreloadAppControllerImpl preloadAppControllerImpl = PreloadAppControllerImpl.getInstance();
        this.mPreloadAppController = preloadAppControllerImpl;
        preloadAppControllerImpl.init(this.mActivityManagerService, this, serviceThread);
        this.mProcessPolicy = new ProcessPolicy(this, this.mActivityManagerService, this.mAccessibilityManager, serviceThread);
        this.mProcessStarter = new ProcessStarter(this, this.mActivityManagerService, mainHandler);
        BatteryStatsManagerStub.getInstance(this.mActivityManagerService.mBatteryStatsService.getActiveStatistics(), 0).setActiveCallback(this.mProcessPolicy);
        systemReady();
        LocalServices.addService(ProcessManagerInternal.class, new LocalService());
        this.mCameraBooster = new CameraBooster(this, this.mActivityManagerService, serviceThread, this.mContext);
        this.mGameMemoryReclaimer = new GameMemoryReclaimer(this, this.mContext, this.mActivityManagerService);
        probeCgroupVersion();
    }

    protected void systemReady() {
        this.mProcessPolicy.systemReady(this.mContext);
        this.mProcessStarter.systemReady();
        try {
            this.mBinderDelayWriter = new FileWriter(BINDER_MONITOR_FD_PATH);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* loaded from: classes.dex */
    public class MainHandler extends Handler {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public MainHandler(Looper looper) {
            super(looper, null, true);
            ProcessManagerService.this = this$0;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    synchronized (ProcessManagerService.this.mActivityManagerService) {
                        ProcessManagerService.this.mProcessStarter.restoreLastProcessesInfoLocked(msg.arg1);
                    }
                    return;
                default:
                    return;
            }
        }
    }

    public void shutdown() {
    }

    public ProcessPolicy getProcessPolicy() {
        return this.mProcessPolicy;
    }

    public ProcessKiller getProcessKiller() {
        return this.mProcessKiller;
    }

    public boolean kill(ProcessConfig config) throws RemoteException {
        if (!checkPermission()) {
            String msg = "Permission Denial: ProcessManager.kill() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid();
            Slog.w("ProcessManager", msg);
            return false;
        }
        addConfigToHistory(config);
        this.mProcessPolicy.resetWhiteList(this.mContext, UserHandle.getCallingUserId());
        boolean success = SystemPressureController.getInstance().kill(config);
        ProcessRecordImpl.reportAppPss();
        return success;
    }

    public boolean killAllBackgroundExceptLocked(ProcessConfig config) {
        if (config.isPriorityInvalid()) {
            String msg = "priority:" + config.getPriority() + " is invalid";
            Slog.w("ProcessManager", msg);
            return false;
        }
        int maxProcState = config.getPriority();
        String killReason = TextUtils.isEmpty(config.getReason()) ? getKillReason(config.getPolicy()) : config.getReason();
        synchronized (this.mActivityManagerService) {
            ArrayList<ProcessRecord> procs = new ArrayList<>();
            ProcessList processList = this.mActivityManagerService.mProcessList;
            int NP = processList.getProcessNamesLOSP().getMap().size();
            for (int ip = 0; ip < NP; ip++) {
                SparseArray<ProcessRecord> apps = (SparseArray) processList.getProcessNamesLOSP().getMap().valueAt(ip);
                int NA = apps.size();
                for (int ia = 0; ia < NA; ia++) {
                    ProcessRecord app = apps.valueAt(ia);
                    if (app.isRemoved() || (((maxProcState < 0 || app.mState.getSetProcState() > maxProcState) && app.mState.hasShownUi()) || this.mDisplayManagerInternal.isInResolutionSwitchBlackList(app.processName))) {
                        procs.add(app);
                    }
                }
            }
            int N = procs.size();
            for (int i = 0; i < N; i++) {
                ProcessRecord app2 = procs.get(i);
                if (!this.mDisplayManagerInternal.isInResolutionSwitchProtectList(app2.processName)) {
                    this.mProcessKiller.forceStopPackage(app2, killReason, false);
                }
            }
        }
        return true;
    }

    public boolean isInWhiteList(WindowProcessController wpc, int userId, int policy) {
        synchronized (this.mActivityManagerService.mPidsSelfLocked) {
            if (wpc != null) {
                ProcessRecord pr = this.mActivityManagerService.mPidsSelfLocked.get(wpc.getPid());
                if (pr.getWindowProcessController() == wpc) {
                    return isInWhiteList(pr, userId, policy);
                }
            }
            return false;
        }
    }

    public boolean isInWhiteList(ProcessRecord app, int userId, int policy) {
        if (app.uid != UserHandle.getAppId(1002) && !app.info.packageName.contains("com.android.cts")) {
            switch (policy) {
                case 1:
                case 3:
                case 4:
                case 5:
                case 6:
                case 14:
                case 16:
                case 22:
                    int flags = 13;
                    if (EnterpriseSettings.ENTERPRISE_ACTIVATED) {
                        flags = 13 | 4096;
                    }
                    return isPackageInList(app.info.packageName, flags) || this.mProcessPolicy.isInProcessStaticWhiteList(app.processName) || this.mProcessPolicy.isLockedApplication(app.info.packageName, userId) || this.mProcessPolicy.isProcessImportant(app) || this.mProcessPolicy.isFastBootEnable(app.info.packageName, app.info.uid, true);
                case 2:
                case 19:
                case 20:
                    int flags2 = 5;
                    if (EnterpriseSettings.ENTERPRISE_ACTIVATED) {
                        flags2 = 5 | 4096;
                    }
                    return isPackageInList(app.info.packageName, flags2) || this.mProcessPolicy.isInProcessStaticWhiteList(app.processName) || this.mProcessPolicy.isProcessImportant(app);
                case 7:
                    int flags3 = 13;
                    if (EnterpriseSettings.ENTERPRISE_ACTIVATED) {
                        flags3 = 13 | 4096;
                    }
                    return isPackageInList(app.info.packageName, flags3) || this.mProcessPolicy.isInProcessStaticWhiteList(app.processName) || this.mProcessPolicy.isProcessImportant(app) || this.mProcessPolicy.isFastBootEnable(app.info.packageName, app.info.uid, true);
                case 8:
                case 9:
                case 10:
                case 11:
                case 12:
                case 13:
                case 15:
                case 17:
                default:
                    return false;
                case 18:
                    return this.mProcessPolicy.isInDisplaySizeWhiteList(app.processName);
                case 21:
                    return isPackageInList(app.info.packageName, 1) || this.mProcessPolicy.isInProcessStaticWhiteList(app.processName);
            }
        }
        return true;
    }

    protected String getKillReason(ProcessConfig config) {
        int policy = config.getPolicy();
        if (policy == 10 && !TextUtils.isEmpty(config.getReason())) {
            return config.getReason();
        }
        return getKillReason(policy);
    }

    private String getKillReason(int policy) {
        switch (policy) {
            case 1:
                return ProcessPolicy.REASON_ONE_KEY_CLEAN;
            case 2:
                return ProcessPolicy.REASON_FORCE_CLEAN;
            case 3:
                return ProcessPolicy.REASON_LOCK_SCREEN_CLEAN;
            case 4:
                return ProcessPolicy.REASON_GAME_CLEAN;
            case 5:
                return ProcessPolicy.REASON_OPTIMIZATION_CLEAN;
            case 6:
                return ProcessPolicy.REASON_GARBAGE_CLEAN;
            case 7:
                return ProcessPolicy.REASON_SWIPE_UP_CLEAN;
            case 8:
            case 9:
            default:
                return ProcessPolicy.REASON_UNKNOWN;
            case 10:
                return ProcessPolicy.REASON_USER_DEFINED;
            case 11:
                return ProcessPolicy.REASON_AUTO_POWER_KILL;
            case 12:
                return ProcessPolicy.REASON_AUTO_THERMAL_KILL;
            case 13:
                return ProcessPolicy.REASON_AUTO_IDLE_KILL;
            case 14:
                return ProcessPolicy.REASON_AUTO_SLEEP_CLEAN;
            case 15:
                return ProcessPolicy.REASON_AUTO_LOCK_OFF_CLEAN;
            case 16:
                return ProcessPolicy.REASON_AUTO_SYSTEM_ABNORMAL_CLEAN;
            case 17:
                return ProcessPolicy.REASON_AUTO_LOCK_OFF_CLEAN_BY_PRIORITY;
            case 18:
                return ProcessPolicy.REASON_DISPLAY_SIZE_CHANGED;
        }
    }

    public boolean checkPermission() {
        int callingPid = Binder.getCallingPid();
        int callingUid = Binder.getCallingUid();
        if (callingUid < 10000) {
            return true;
        }
        ProcessRecord app = getProcessRecordByPid(callingPid);
        if (isSystemApp(app) || hasSystemSignature(app)) {
            return true;
        }
        return false;
    }

    public void updateApplicationLockedState(String packageName, int userId, boolean isLocked) throws RemoteException {
        if (!checkPermission()) {
            String msg = "Permission Denial: ProcessManager.updateApplicationLockedState() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid();
            Slog.w("ProcessManager", msg);
            throw new SecurityException(msg);
        }
        this.mProcessPolicy.updateApplicationLockedState(this.mContext, userId, packageName, isLocked);
    }

    public List<String> getLockedApplication(int userId) {
        return this.mProcessPolicy.getLockedApplication(userId);
    }

    public boolean isLockedApplication(String packageName, int userId) throws RemoteException {
        return this.mProcessPolicy.isLockedApplication(packageName, userId);
    }

    public boolean skipCurrentProcessInBackup(ProcessRecord app, String packageName, int userId) {
        BackupManager backupManager = BackupManager.getBackupManager(this.mContext);
        if (backupManager.getState() != 0) {
            String curRunningPkg = backupManager.getCurrentRunningPackage();
            if ((!TextUtils.isEmpty(packageName) && packageName.equals(curRunningPkg)) || (app != null && app.getThread() != null && app.getPkgList().containsKey(curRunningPkg) && app.userId == userId)) {
                Log.i("ProcessManager", "skip kill:" + (app != null ? app.processName : packageName) + " for Backup app");
                return true;
            }
            return false;
        }
        return false;
    }

    public boolean isForceStopEnable(ProcessRecord app, int policy) {
        if (policy == 13) {
            return true;
        }
        return !Build.IS_INTERNATIONAL_BUILD && !isSystemApp(app) && !isAllowAutoStart(app.info.packageName, app.info.uid) && !isPackageInList(app.info.packageName, 42);
    }

    public boolean isTrimMemoryEnable(String packageName) {
        return !isPackageInList(packageName, 16);
    }

    public boolean isAllowAutoStart(String packageName, int uid) {
        int mode = this.mAppOpsManager.checkOpNoThrow(10008, uid, packageName);
        return mode == 0;
    }

    private boolean isPackageInList(String packageName, int flags) {
        if (packageName == null) {
            return false;
        }
        List<String> whiteList = this.mProcessPolicy.getWhiteList(flags);
        for (String item : whiteList) {
            if (packageName.equals(item)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSystemApp(int pid) {
        ProcessRecord processRecord = getProcessRecordByPid(pid);
        if (processRecord != null) {
            return isSystemApp(processRecord);
        }
        return false;
    }

    private boolean isSystemApp(ProcessRecord app) {
        return (app == null || app.info == null || (app.info.flags & 129) == 0) ? false : true;
    }

    private boolean hasSystemSignature(ProcessRecord app) {
        return false;
    }

    private boolean isUidSystem(int uid) {
        return uid % 100000 < 10000;
    }

    private String getPackageNameByPid(int pid) {
        ProcessRecord processRecord = getProcessRecordByPid(pid);
        if (processRecord != null) {
            return processRecord.info.packageName;
        }
        return null;
    }

    public ProcessRecord getProcessRecordByPid(int pid) {
        ProcessRecord processRecord;
        synchronized (this.mActivityManagerService.mPidsSelfLocked) {
            processRecord = this.mActivityManagerService.mPidsSelfLocked.get(pid);
        }
        return processRecord;
    }

    public ProcessRecord getProcessRecord(String processName, int userId) {
        synchronized (this.mActivityManagerService) {
            for (int i = this.mLruProcesses.size() - 1; i >= 0; i--) {
                ProcessRecord app = this.mLruProcesses.get(i);
                if (app.getThread() != null && app.processName.equals(processName) && app.userId == userId) {
                    return app;
                }
            }
            return null;
        }
    }

    public ProcessRecord getProcessRecordBysimply(String processName) {
        synchronized (this.mActivityManagerService) {
            for (int i = this.mLruProcesses.size() - 1; i >= 0; i--) {
                ProcessRecord app = this.mLruProcesses.get(i);
                if (app.getThread() != null && app.processName.equals(processName)) {
                    return app;
                }
            }
            return null;
        }
    }

    public List<ProcessRecord> getProcessRecordList(String packageName, int userId) {
        List<ProcessRecord> appList = new ArrayList<>();
        synchronized (this.mActivityManagerService) {
            for (int i = this.mLruProcesses.size() - 1; i >= 0; i--) {
                ProcessRecord app = this.mLruProcesses.get(i);
                if (app.getThread() != null && app.getPkgList().containsKey(packageName) && app.userId == userId) {
                    appList.add(app);
                }
            }
        }
        return appList;
    }

    public List<ProcessRecord> getProcessRecordListByPackageAndUid(String packageName, int uid) {
        List<ProcessRecord> appList = new ArrayList<>();
        synchronized (this.mActivityManagerService) {
            for (int i = this.mLruProcesses.size() - 1; i >= 0; i--) {
                ProcessRecord app = this.mLruProcesses.get(i);
                if (app.getThread() != null && app.getPkgList().containsKey(packageName) && app.info.uid == uid) {
                    appList.add(app);
                }
            }
        }
        return appList;
    }

    public List<ProcessRecord> getProcessRecordByUid(int uid) {
        List<ProcessRecord> appList = new ArrayList<>();
        synchronized (this.mActivityManagerService) {
            for (int i = this.mLruProcesses.size() - 1; i >= 0; i--) {
                ProcessRecord app = this.mLruProcesses.get(i);
                if (app.getThread() != null && app.info.uid == uid) {
                    appList.add(app);
                }
            }
        }
        return appList;
    }

    public boolean isAppHasForegroundServices(ProcessRecord processRecord) {
        boolean hasForegroundServices;
        synchronized (this.mActivityManagerService) {
            hasForegroundServices = processRecord.mServices.hasForegroundServices();
        }
        return hasForegroundServices;
    }

    private void increaseRecordCount(String processName, Map<String, Integer> recordMap) {
        Integer expCount = recordMap.get(processName);
        if (expCount == null) {
            expCount = 0;
        }
        recordMap.put(processName, Integer.valueOf(expCount.intValue() + 1));
    }

    private void reduceRecordCountDelay(final String processName, final Map<String, Integer> recordMap, long delay) {
        this.mHandler.postDelayed(new Runnable() { // from class: com.android.server.am.ProcessManagerService.1
            @Override // java.lang.Runnable
            public void run() {
                Integer count = (Integer) recordMap.get(processName);
                if (count == null || count.intValue() <= 0) {
                    return;
                }
                Integer count2 = Integer.valueOf(count.intValue() - 1);
                if (count2.intValue() <= 0) {
                    recordMap.remove(processName);
                } else {
                    recordMap.put(processName, count2);
                }
            }
        }, delay);
    }

    public void updateConfig(ProcessConfig config) throws RemoteException {
        if (!checkPermission()) {
            String msg = "Permission Denial: ProcessManager.updateConfig() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid();
            Slog.w("ProcessManager", msg);
            throw new SecurityException(msg);
        }
    }

    public int startProcesses(List<PreloadProcessData> dataList, int startProcessCount, boolean ignoreMemory, int userId, int flag) throws RemoteException {
        if (!checkPermission()) {
            String msg = "Permission Denial: ProcessManager.startMutiProcesses() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid();
            Slog.w("ProcessManager", msg);
            throw new SecurityException(msg);
        } else if (dataList == null || dataList.size() == 0) {
            throw new IllegalArgumentException("packageNames cannot be null!");
        } else {
            if (dataList.size() < startProcessCount) {
                throw new IllegalArgumentException("illegal start number!");
            }
            if (!ignoreMemory && ProcessUtils.isLowMemory()) {
                Slog.w("ProcessManager", "low memory! skip start process!");
                return 0;
            } else if (startProcessCount <= 0) {
                Slog.w("ProcessManager", "startProcessCount <= 0, skip start process!");
                return 0;
            } else if (!this.mProcessStarter.isAllowPreloadProcess(dataList, flag)) {
                return 0;
            } else {
                return this.mProcessStarter.startProcesses(dataList, startProcessCount, ignoreMemory, userId, flag);
            }
        }
    }

    public void notifyGameForeground(String game) {
        this.mGameMemoryReclaimer.notifyGameForeground(game);
    }

    public void notifyGameBackground() {
        this.mGameMemoryReclaimer.notifyGameBackground();
    }

    public void reclaimBackgroundForGame(long need) {
        this.mGameMemoryReclaimer.reclaimBackground(need);
    }

    public void addGameProcessKiller(GameProcessKiller.GameProcessKillerConfig cfg) {
        this.mGameMemoryReclaimer.addGameProcessKiller(cfg);
    }

    public void addGameProcessCompactor(GameProcessCompactor.GameProcessCompactorConfig cfg) {
        this.mGameMemoryReclaimer.addGameProcessCompactor(cfg);
    }

    public void foregroundInfoChanged(String foregroundPackageName, ComponentName component) {
        this.mProcessStarter.foregroundActivityChanged(foregroundPackageName);
        if (this.mIsCameraForeground != TextUtils.equals(foregroundPackageName, "com.android.camera")) {
            this.mIsCameraForeground = !this.mIsCameraForeground;
            Slog.i("ProcessManager", "update oom level for camera app change to " + (this.mIsCameraForeground ? "Forgeground" : "Background"));
            this.mCameraBooster.notifyCameraForegroundState(this.mIsCameraForeground, "com.android.camera", component);
        }
    }

    public void notifyCameraPostProcessState() {
        this.mCameraBooster.notifyCameraPostProcessState();
    }

    public void notifyCameraForegroundState(String cameraId, boolean isForeground, String caller) {
        SmartPowerService.getInstance().notifyCameraForegroundState(cameraId, isForeground, caller, Binder.getCallingUid(), Binder.getCallingPid());
        if (!TextUtils.equals("com.android.camera", caller)) {
            this.mCameraBooster.notifyCameraForegroundState(isForeground, caller, null);
        }
    }

    public void boostCameraByThreshold(long memThreshold) {
        this.mCameraBooster.boostCameraByThreshold(memThreshold);
    }

    public void updateCameraBoosterCloudData(double version, String jsonStr) {
        this.mCameraBooster.updateCameraBoosterCloudData(version, jsonStr);
    }

    public void reclaimMemoryForCamera(int modeValue) {
        String str = DEVICE;
        if (str.equals("star") || str.equals("mars") || str.equals("haydn") || str.equals("mona")) {
            this.mCameraBooster.boostCameraByThreshold(3565158L);
        } else {
            this.mCameraBooster.reclaimMemoryForCamera(modeValue);
        }
    }

    public boolean interceptAppRestartIfNeeded(String processName, String type) {
        if (this.mIsCameraForeground) {
            return this.mCameraBooster.interceptAppRestartIfNeeded(processName, type);
        }
        return false;
    }

    public boolean protectCurrentProcess(boolean isProtected, int timeout) throws RemoteException {
        final ProcessRecord app = getProcessRecordByPid(Binder.getCallingPid());
        if (app == null || !this.mProcessPolicy.isInAppProtectList(app.info.packageName)) {
            String msg = "Permission Denial: ProcessManager.protectCurrentProcess() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid();
            Slog.w("ProcessManager", msg);
            throw new SecurityException(msg);
        }
        boolean success = this.mProcessPolicy.protectCurrentProcess(app, isProtected);
        if (isProtected && timeout > 0) {
            this.mHandler.postDelayed(new Runnable() { // from class: com.android.server.am.ProcessManagerService.2
                @Override // java.lang.Runnable
                public void run() {
                    ProcessManagerService.this.mProcessPolicy.protectCurrentProcess(app, false);
                }
            }, timeout);
        }
        return success;
    }

    public void adjBoost(String processName, int targetAdj, long timeout, int userId) throws RemoteException {
        if (!checkPermission()) {
            String msg = "Permission Denial: ProcessManager.adjBoost() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid();
            Slog.w("ProcessManager", msg);
            throw new SecurityException(msg);
        }
        if (targetAdj < 0) {
            targetAdj = 0;
        }
        if ((timeout > 300000 || timeout <= 0) && !SystemProperties.getBoolean("ro.sys.proc.skip_adj_deboost", false) && !TextUtils.equals(processName, "com.android.camera")) {
            timeout = 300000;
        }
        doAdjBoost(processName, targetAdj, timeout, userId, true);
    }

    private void doAdjBoost(String processName, int targetAdj, long timeout, int userId, boolean forceUpdate) {
        Map<String, Runnable> runnableMap;
        ProcessRecord app = getProcessRecord(processName, userId);
        if (app == null || app.isPersistent()) {
            return;
        }
        if (timeout == 0 && SystemProperties.getBoolean("ro.sys.proc.skip_adj_deboost", false)) {
            synchronized (this.mActivityManagerService) {
                app.mState.setMaxAdj(targetAdj);
                ProcessPolicy.setAppMaxProcState(app, 13);
                this.mActivityManagerService.updateOomAdjLocked("updateOomAdj_activityChange");
            }
            Log.i("ProcessManager", "adj boost for:" + processName + ", timeout:" + timeout);
            return;
        }
        Map<String, ProcessStarter.ProcessPriorityInfo> dataMap = this.mAdjBoostProcessMap.get(userId);
        if (dataMap == null) {
            dataMap = new ConcurrentHashMap();
            this.mAdjBoostProcessMap.put(userId, dataMap);
        }
        ProcessStarter.ProcessPriorityInfo appInfo = dataMap.get(processName);
        if (appInfo == null) {
            appInfo = new ProcessStarter.ProcessPriorityInfo();
            dataMap.put(processName, appInfo);
        } else {
            Log.i("ProcessManager", "process:" + processName + " is already boosted!");
            if (TextUtils.equals(processName, "com.android.camera") && (runnableMap = this.mAdjDeboostRunnableMap.get(userId)) != null) {
                Runnable runnable = runnableMap.get(processName);
                if (runnable != null) {
                    this.mHandler.removeCallbacks(runnable);
                    runnableMap.remove(processName);
                }
                if (timeout > 0) {
                    doAdjDeboost(processName, timeout, userId);
                }
            }
            if (!forceUpdate) {
                return;
            }
        }
        synchronized (this.mActivityManagerService) {
            appInfo.app = app;
            appInfo.maxAdj = app.mState.getMaxAdj();
            appInfo.maxProcState = ProcessPolicy.getAppMaxProcState(app);
            app.mState.setMaxAdj(targetAdj);
            ProcessPolicy.setAppMaxProcState(app, 13);
            this.mActivityManagerService.updateOomAdjLocked("updateOomAdj_activityChange");
        }
        if (timeout > 0) {
            doAdjDeboost(processName, timeout, userId);
        }
        Log.i("ProcessManager", "adj boost for:" + processName + ", timeout:" + timeout);
    }

    private void doAdjBoostBysimply(String processName, int targetAdj, long timeout, int userId) {
        ProcessRecord app = getProcessRecord(processName, userId);
        if (app == null || app.isPersistent()) {
            return;
        }
        Map<String, ProcessStarter.ProcessPriorityInfo> dataMap = this.mAdjBoostProcessMapBysimply.get(userId);
        if (dataMap == null) {
            dataMap = new ConcurrentHashMap();
            this.mAdjBoostProcessMapBysimply.put(userId, dataMap);
        }
        ProcessStarter.ProcessPriorityInfo appInfo = dataMap.get(processName);
        if (appInfo == null) {
            appInfo = new ProcessStarter.ProcessPriorityInfo();
            dataMap.put(processName, appInfo);
        } else if (!CAMERA_TEST.equals(processName)) {
            Log.i("ProcessManager", "process:" + processName + " is already boosted!");
            return;
        }
        synchronized (this.mActivityManagerService) {
            appInfo.app = app;
            appInfo.maxAdj = app.mState.getMaxAdj();
            appInfo.maxProcState = ProcessPolicy.getAppMaxProcState(app);
            app.mState.setMaxAdj(targetAdj);
            ProcessPolicy.setAppMaxProcState(app, 13);
            this.mActivityManagerService.updateOomAdjLocked("updateOomAdj_activityChange");
        }
        if (timeout > 0) {
            doAdjDeboost(processName, timeout, userId);
        }
        Log.i("ProcessManager", "adj boost for:" + processName + ", timeout:" + timeout);
    }

    private void doAdjDeboost(final String processName, long timeout, final int userId) {
        Map<String, Runnable> runnableMap = this.mAdjDeboostRunnableMap.get(userId);
        if (runnableMap == null) {
            runnableMap = new ConcurrentHashMap();
            this.mAdjDeboostRunnableMap.put(userId, runnableMap);
        }
        Runnable runnable = new Runnable() { // from class: com.android.server.am.ProcessManagerService.3
            @Override // java.lang.Runnable
            public void run() {
                Map<String, ProcessStarter.ProcessPriorityInfo> data = (Map) ProcessManagerService.this.mAdjBoostProcessMap.get(userId);
                if (data == null) {
                    Log.i("ProcessManager", "data get failed");
                    return;
                }
                Log.i("ProcessManager", "adj deboost for: " + processName + " begin to execute.");
                ProcessStarter.ProcessPriorityInfo priorityInfo = data.get(processName);
                if (priorityInfo != null && priorityInfo.app != null && priorityInfo.app.info != null) {
                    synchronized (ProcessManagerService.this.mActivityManagerService) {
                        if (ProcessManagerService.this.mProcessPolicy.isLockedApplication(priorityInfo.app.info.packageName, userId)) {
                            priorityInfo.app.mState.setMaxAdj(ProcessManager.LOCKED_MAX_ADJ);
                            ProcessPolicy.setAppMaxProcState(priorityInfo.app, ProcessManager.LOCKED_MAX_PROCESS_STATE);
                        } else {
                            priorityInfo.app.mState.setMaxAdj(priorityInfo.maxAdj);
                            ProcessPolicy.setAppMaxProcState(priorityInfo.app, priorityInfo.maxProcState);
                        }
                        ProcessManagerService.this.mActivityManagerService.updateOomAdjLocked("updateOomAdj_activityChange");
                    }
                    data.remove(processName);
                }
                Map<String, Runnable> rMap = (Map) ProcessManagerService.this.mAdjDeboostRunnableMap.get(userId);
                if (rMap == null) {
                    Log.e("ProcessManager", "runnableMap get failed");
                } else {
                    rMap.remove(processName);
                }
            }
        };
        runnableMap.put(processName, runnable);
        this.mHandler.postDelayed(runnable, timeout);
    }

    private void doAdjDeboostBysimply(final String processName, long timeout, final int userId) {
        Runnable runnable = new Runnable() { // from class: com.android.server.am.ProcessManagerService.4
            @Override // java.lang.Runnable
            public void run() {
                Map<String, ProcessStarter.ProcessPriorityInfo> data = (Map) ProcessManagerService.this.mAdjBoostProcessMapBysimply.get(userId);
                if (data == null) {
                    Log.i("ProcessManager", "data get failed");
                    return;
                }
                Log.i("ProcessManager", "adj deboost for: " + processName + " begin to execute.");
                ProcessStarter.ProcessPriorityInfo priorityInfo = data.get(processName);
                if (priorityInfo != null && priorityInfo.app != null && priorityInfo.app.info != null) {
                    synchronized (ProcessManagerService.this.mActivityManagerService) {
                        if (ProcessManagerService.this.mProcessPolicy.isLockedApplication(priorityInfo.app.info.packageName, userId)) {
                            priorityInfo.app.mState.setMaxAdj(ProcessManager.LOCKED_MAX_ADJ);
                            ProcessPolicy.setAppMaxProcState(priorityInfo.app, ProcessManager.LOCKED_MAX_PROCESS_STATE);
                        } else {
                            priorityInfo.app.mState.setMaxAdj(priorityInfo.maxAdj);
                            ProcessPolicy.setAppMaxProcState(priorityInfo.app, priorityInfo.maxProcState);
                        }
                        ProcessManagerService.this.mActivityManagerService.updateOomAdjLocked("updateOomAdj_activityChange");
                    }
                    data.remove(processName);
                }
            }
        };
        this.mHandler.postDelayed(runnable, timeout);
    }

    public void checkUpdateOomAdjForCamera(String foregroundPkgName, ForegroundInfo lastInfo) {
        String lastForegroundPkgName = lastInfo != null ? lastInfo.mForegroundPackageName : null;
        if (!TextUtils.equals(lastForegroundPkgName, foregroundPkgName) && "com.android.camera".equals(foregroundPkgName)) {
            if ("com.miui.gallery".equals(lastForegroundPkgName) && this.cameraBoost) {
                this.cameraBoost = false;
                doAdjDeboost("com.android.camera", 1000L, this.prev_uid);
            }
            WindowProcessController prev = this.mActivityManagerService.mAtmInternal.getPreviousProcess();
            if (prev == null) {
                Log.i("ProcessManager", "prev controller get failed");
                return;
            }
            ProcessRecord r = getProcessRecordByPid(prev.getPid());
            if (r == null || r.info.processName == null) {
                Log.i("ProcessManager", "prev process get failed");
            } else if ("com.android.camera".equals(r.info.processName)) {
                Log.i("ProcessManager", "prev app " + r.info.processName + " not boost");
            } else {
                Log.i("ProcessManager", "boost prev app: " + r.info.processName);
                this.prev_app_name = r.info.processName;
                this.prev_uid = r.userId;
                doAdjBoost(r.info.processName, 100, -1L, r.userId, false);
            }
        } else if (!TextUtils.equals(lastForegroundPkgName, foregroundPkgName) && "com.android.camera".equals(lastForegroundPkgName)) {
            if (this.prev_app_name == null) {
                Log.i("ProcessManager", "prev app name get failed");
                return;
            }
            Log.i("ProcessManager", "adj deboost for:" + this.prev_app_name + " , " + this.prev_uid + ", timeout: 5s");
            if ("com.miui.gallery".equals(foregroundPkgName)) {
                doAdjBoost("com.android.camera", 100, -1L, this.prev_uid, false);
                this.cameraBoost = true;
            }
            doAdjDeboost(this.prev_app_name, 5000L, this.prev_uid);
        }
    }

    public void checkUpdateOomAdjForCameraBysimply(String foregroundPkgName, ForegroundInfo lastInfo) {
        String lastForegroundPkgName = lastInfo != null ? lastInfo.mForegroundPackageName : null;
        if (!TextUtils.equals(lastForegroundPkgName, foregroundPkgName) && "com.android.camera".equals(foregroundPkgName)) {
            ProcessRecord app = getProcessRecordBysimply(CAMERA_TEST);
            if (app == null) {
                WindowProcessController prev = this.mActivityManagerService.mAtmInternal.getPreviousProcess();
                if (prev == null) {
                    Log.i("ProcessManager", "prev controller get failed");
                    return;
                }
                ProcessRecord r = getProcessRecordByPid(prev.getPid());
                if (r == null || r.info.processName == null) {
                    Log.i("ProcessManager", "prev process get failed");
                    return;
                } else if ("com.android.camera".equals(r.info.processName)) {
                    Log.i("ProcessManager", "prev app " + r.info.processName + " not boost");
                    return;
                } else {
                    Log.i("ProcessManager", "boost prev app: " + r.info.processName);
                    this.prev_app_name = r.info.processName;
                    this.prev_uid = r.userId;
                    doAdjBoostBysimply(r.info.processName, 100, -1L, r.userId);
                    return;
                }
            }
            Log.i("ProcessManager", "boost prev process: com.phonetest.application:CameraMemoryWatcher");
            this.prev_app_name = CAMERA_TEST;
            this.prev_uid = app.userId;
            doAdjBoostBysimply(CAMERA_TEST, 100, -1L, app.userId);
        } else if (!TextUtils.equals(lastForegroundPkgName, foregroundPkgName) && "com.android.camera".equals(lastForegroundPkgName)) {
            String str = this.prev_app_name;
            if (str == null || CAMERA_TEST.equals(str)) {
                Log.i("ProcessManager", "prev app name get failed");
                return;
            }
            Log.i("ProcessManager", "adj deboost for:" + this.prev_app_name + " , " + this.prev_uid + ", timeout: 5s");
            doAdjDeboostBysimply(this.prev_app_name, 5000L, this.prev_uid);
        }
    }

    public void updateCloudData(ProcessCloudData cloudData) throws RemoteException {
        if (!checkPermission()) {
            String msg = "Permission Denial: ProcessManager.updateCloudWhiteList() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid();
            Slog.w("ProcessManager", msg);
            throw new SecurityException(msg);
        } else if (cloudData == null) {
            throw new IllegalArgumentException("cloudData cannot be null!");
        } else {
            this.mProcessPolicy.updateCloudData(cloudData);
        }
    }

    public void registerForegroundInfoListener(IForegroundInfoListener listener) throws RemoteException {
        if (!checkPermission()) {
            String msg = "Permission Denial: ProcessManager.registerForegroundInfoListener() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid();
            Slog.w("ProcessManager", msg);
            throw new SecurityException(msg);
        }
        Log.i("ProcessManager", "registerForegroundInfoListener, caller=" + Binder.getCallingPid() + ", listener=" + listener);
        this.mForegroundInfoManager.registerForegroundInfoListener(listener);
    }

    public void unregisterForegroundInfoListener(IForegroundInfoListener listener) throws RemoteException {
        if (!checkPermission()) {
            String msg = "Permission Denial: ProcessManager.unregisterForegroundInfoListener() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid();
            Slog.w("ProcessManager", msg);
            throw new SecurityException(msg);
        }
        this.mForegroundInfoManager.unregisterForegroundInfoListener(listener);
    }

    public void registerForegroundWindowListener(IForegroundWindowListener listener) throws RemoteException {
        if (!checkPermission()) {
            String msg = "Permission Denial: ProcessManager.registerForegroundWindowListener() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid();
            Slog.w("ProcessManager", msg);
            throw new SecurityException(msg);
        }
        Log.i("ProcessManager", "registerForegroundWindowListener, caller=" + Binder.getCallingPid() + ", listener=" + listener);
        this.mForegroundInfoManager.registerForegroundWindowListener(listener);
    }

    public void unregisterForegroundWindowListener(IForegroundWindowListener listener) throws RemoteException {
        if (!checkPermission()) {
            String msg = "Permission Denial: ProcessManager.unregisterForegroundWindowListener() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid();
            Slog.w("ProcessManager", msg);
            throw new SecurityException(msg);
        }
        this.mForegroundInfoManager.unregisterForegroundWindowListener(listener);
    }

    public void registerActivityChangeListener(List<String> targetPackages, List<String> targetActivities, IActivityChangeListener listener) throws RemoteException {
        if (!checkPermission()) {
            String msg = "Permission Denial: ProcessManager.registerActivityChangeListener() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid();
            Slog.w("ProcessManager", msg);
            throw new SecurityException(msg);
        }
        this.mForegroundInfoManager.registerActivityChangeListener(targetPackages, targetActivities, listener);
    }

    public void unregisterActivityChangeListener(IActivityChangeListener listener) throws RemoteException {
        if (!checkPermission()) {
            String msg = "Permission Denial: ProcessManager.unregisterActivityChangeListener() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid();
            Slog.w("ProcessManager", msg);
            throw new SecurityException(msg);
        }
        this.mForegroundInfoManager.unregisterActivityChangeListener(listener);
    }

    public ForegroundInfo getForegroundInfo() throws RemoteException {
        if (!checkPermission()) {
            String msg = "Permission Denial: ProcessManager.unregisterForegroundInfoListener() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid();
            Slog.w("ProcessManager", msg);
            throw new SecurityException(msg);
        }
        return this.mForegroundInfoManager.getForegroundInfo();
    }

    public void addMiuiApplicationThread(IMiuiApplicationThread applicationThread, int pid) throws RemoteException {
        if (Binder.getCallingPid() != pid) {
            String msg = "Permission Denial: ProcessManager.addMiuiApplicationThread() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid();
            Slog.w("ProcessManager", msg);
            throw new SecurityException(msg);
        }
        this.mMiuiApplicationThreadManager.addMiuiApplicationThread(applicationThread, pid);
    }

    public IMiuiApplicationThread getForegroundApplicationThread() throws RemoteException {
        if (!checkPermission()) {
            String msg = "Permission Denial: ProcessManager.getForegroundApplicationThread() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid();
            Slog.w("ProcessManager", msg);
            throw new SecurityException(msg);
        }
        int pid = WindowProcessUtils.getTopRunningPidLocked();
        return this.mMiuiApplicationThreadManager.getMiuiApplicationThread(pid);
    }

    public void notifyForegroundInfoChanged(final ForegroundInfoManager.FgActivityChangedInfo fgInfo) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.am.ProcessManagerService.5
            @Override // java.lang.Runnable
            public void run() {
                if (ProcessManagerService.CONFIG_CAM_PROTECT_PREV) {
                    ProcessManagerService.this.checkUpdateOomAdjForCamera(fgInfo.getPackageName(), ProcessManagerService.this.mForegroundInfoManager.getForegroundInfo());
                }
                if (ProcessManagerService.CONFIG_CAM_PROTECT_PREV_EXT) {
                    ProcessManagerService.this.checkUpdateOomAdjForCameraBysimply(fgInfo.getPackageName(), ProcessManagerService.this.mForegroundInfoManager.getForegroundInfo());
                }
                ProcessManagerService.this.mForegroundInfoManager.notifyForegroundInfoChanged(fgInfo);
            }
        });
    }

    public void notifyForegroundWindowChanged(final ForegroundInfoManager.FgWindowChangedInfo fgInfo) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.am.ProcessManagerService.6
            @Override // java.lang.Runnable
            public void run() {
                ProcessManagerService.this.mForegroundInfoManager.notifyForegroundWindowChanged(fgInfo);
            }
        });
    }

    public void notifyActivityChanged(final ComponentName curActivityComponent) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.am.ProcessManagerService.7
            @Override // java.lang.Runnable
            public void run() {
                OomAdjusterImpl.getInstance().foregroundActivityChanged(curActivityComponent.getPackageName());
                ProcessManagerService.this.mForegroundInfoManager.notifyActivityChanged(curActivityComponent);
            }
        });
    }

    public void notifyProcessStarted(ProcessRecord app) {
        MiGardInternal migard = (MiGardInternal) LocalServices.getService(MiGardInternal.class);
        migard.onProcessStart(app.uid, app.getPid(), app.info.packageName, app.callerPackage);
    }

    public void notifyProcessKill(ProcessRecord app, String reason) {
        String processName = app.processName;
        if (TextUtils.isEmpty(reason) || TextUtils.isEmpty(processName)) {
            return;
        }
        this.mPreloadAppController.onProcessKilled(reason, processName);
        this.mProcessStarter.recordKillProcessIfNeeded(processName, reason);
        MiGardInternal migard = (MiGardInternal) LocalServices.getService(MiGardInternal.class);
        migard.onProcessKilled(app.uid, app.getPid(), app.info.packageName, reason);
    }

    public void enableBinderMonitor(final int pid, final int enable) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.am.ProcessManagerService.8
            @Override // java.lang.Runnable
            public void run() {
                try {
                    ProcessManagerService.this.mBinderDelayWriter.write(pid + " " + enable);
                    ProcessManagerService.this.mBinderDelayWriter.flush();
                } catch (Exception e) {
                    Slog.e("ProcessManager", "error write exception log");
                }
            }
        });
    }

    public void notifyProcessDied(ProcessRecord app) {
        if (app == null || app.info == null || app.isolated) {
            return;
        }
        String packageName = app.info.packageName;
        int uid = app.uid;
        String processName = app.processName;
        OomAdjusterImpl.getInstance().notifyProcessDied(app);
        this.mProcessStarter.restartCameraIfNeeded(packageName, processName, uid);
        this.mProcessStarter.recordDiedProcessIfNeeded(packageName, processName, uid);
        this.mGameMemoryReclaimer.notifyProcessDied(app.getPid());
        enableBinderMonitor(app.mPid, 0);
    }

    public List<RunningProcessInfo> getRunningProcessInfo(int pid, int uid, String packageName, String processName, int userId) throws RemoteException {
        int userId2;
        if (!checkPermission()) {
            String msg = "Permission Denial: ProcessManager.getRunningProcessInfo() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid();
            Slog.w("ProcessManager", msg);
            throw new SecurityException(msg);
        }
        if (userId > 0) {
            userId2 = userId;
        } else {
            userId2 = UserHandle.getCallingUserId();
        }
        List<RunningProcessInfo> processInfoList = new ArrayList<>();
        synchronized (this.mActivityManagerService) {
            if (pid > 0) {
                ProcessRecord app = getProcessRecordByPid(pid);
                fillRunningProcessInfoList(processInfoList, app);
                return processInfoList;
            } else if (!TextUtils.isEmpty(processName)) {
                ProcessRecord app2 = getProcessRecord(processName, userId2);
                fillRunningProcessInfoList(processInfoList, app2);
                return processInfoList;
            } else if (!TextUtils.isEmpty(packageName) && uid > 0) {
                List<ProcessRecord> appList = getProcessRecordListByPackageAndUid(packageName, uid);
                for (ProcessRecord app3 : appList) {
                    fillRunningProcessInfoList(processInfoList, app3);
                }
                return processInfoList;
            } else {
                if (!TextUtils.isEmpty(packageName)) {
                    List<ProcessRecord> appList2 = getProcessRecordList(packageName, userId2);
                    for (ProcessRecord app4 : appList2) {
                        fillRunningProcessInfoList(processInfoList, app4);
                    }
                }
                if (uid > 0) {
                    List<ProcessRecord> appList3 = getProcessRecordByUid(uid);
                    for (ProcessRecord app5 : appList3) {
                        fillRunningProcessInfoList(processInfoList, app5);
                    }
                }
                return processInfoList;
            }
        }
    }

    private void fillRunningProcessInfoList(List<RunningProcessInfo> infoList, ProcessRecord app) {
        RunningProcessInfo info = generateRunningProcessInfo(app);
        if (info != null && !infoList.contains(info)) {
            infoList.add(info);
        }
    }

    public RunningProcessInfo generateRunningProcessInfo(ProcessRecord app) {
        RunningProcessInfo info = null;
        if (app != null && app.getThread() != null && !app.mErrorState.isCrashing() && !app.mErrorState.isNotResponding()) {
            info = new RunningProcessInfo();
            info.mProcessName = app.processName;
            info.mPid = app.getPid();
            info.mUid = app.uid;
            info.mAdj = app.mState.getCurAdj();
            info.mProcState = app.mState.getCurProcState();
            info.mHasForegroundActivities = app.mState.hasForegroundActivities();
            info.mHasForegroundServices = app.mServices.hasForegroundServices();
            info.mPkgList = app.getPackageList();
            info.mLocationForeground = info.mHasForegroundServices || (app.mServices.getForegroundServiceTypes() & 8) != 0;
        }
        return info;
    }

    private List<StatusBarNotification> getAppNotificationWithFlag(String packageName, int uid, int flags) {
        List<StatusBarNotification> notifications;
        List<StatusBarNotification> notificationList = new ArrayList<>();
        try {
            ParceledListSlice<StatusBarNotification> notificaionList = this.mNotificationManager.getAppActiveNotifications(packageName, UserHandle.getUserId(uid));
            notifications = notificaionList.getList();
        } catch (RemoteException e) {
        }
        if (notifications != null && !notifications.isEmpty()) {
            for (StatusBarNotification statusBarNotification : notifications) {
                if (statusBarNotification != null && statusBarNotification.getNotification() != null) {
                    Notification notification = statusBarNotification.getNotification();
                    if ((notification.flags & flags) != 0) {
                        notificationList.add(statusBarNotification);
                    }
                }
            }
            return notificationList;
        }
        return notificationList;
    }

    public List<ActiveUidInfo> getActiveUidInfo(int flag) throws RemoteException {
        List<ActiveUidInfo> activeUidInfoList;
        if (!checkPermission()) {
            String msg = "Permission Denial: ProcessManager.getActiveUidInfo() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid();
            Slog.w("ProcessManager", msg);
            throw new SecurityException(msg);
        }
        List<ProcessPolicy.ActiveUidRecord> activeUidRecords = this.mProcessPolicy.getActiveUidRecordList(flag);
        SparseArray<ActiveUidInfo> activeUidInfos = new SparseArray<>();
        synchronized (this.mActivityManagerService) {
            for (ProcessPolicy.ActiveUidRecord r : activeUidRecords) {
                List<ProcessRecord> records = getProcessRecordByUid(r.uid);
                for (ProcessRecord app : records) {
                    ActiveUidInfo activeUidInfo = activeUidInfos.get(r.uid);
                    if (activeUidInfo != null) {
                        if (app.mState.getCurAdj() < activeUidInfo.curAdj) {
                            activeUidInfo.curAdj = app.mState.getCurAdj();
                        }
                        if (app.mState.getCurProcState() < activeUidInfo.curProcState) {
                            activeUidInfo.curProcState = app.mState.getCurProcState();
                        }
                    } else {
                        ActiveUidInfo activeUidInfo2 = generateActiveUidInfoLocked(app, r);
                        if (activeUidInfo2 != null) {
                            activeUidInfos.put(r.uid, activeUidInfo2);
                        }
                    }
                }
            }
            activeUidInfoList = new ArrayList<>();
            for (int i = 0; i < activeUidInfos.size(); i++) {
                activeUidInfoList.add(activeUidInfos.valueAt(i));
            }
        }
        return activeUidInfoList;
    }

    private ActiveUidInfo generateActiveUidInfoLocked(ProcessRecord app, ProcessPolicy.ActiveUidRecord activeUidRecord) {
        if (app == null || app.getThread() == null || app.mErrorState.isCrashing() || app.mErrorState.isNotResponding()) {
            return null;
        }
        ActiveUidInfo info = new ActiveUidInfo();
        info.packageName = app.info.packageName;
        info.uid = activeUidRecord.uid;
        info.flag = activeUidRecord.flag;
        info.curAdj = app.mState.getCurAdj();
        info.curProcState = app.mState.getCurProcState();
        info.foregroundServices = app.mServices.hasForegroundServices();
        info.lastBackgroundTime = app.getUidRecord().getLastBackgroundTime();
        info.numProcs = app.getUidRecord().getNumOfProcs();
        info.pkgList = app.getPackageList();
        return info;
    }

    public void registerPreloadCallback(IPreloadCallback callback, int type) {
        this.mPreloadAppController.registerPreloadCallback(callback, type);
    }

    public int startPreloadApp(String packageName, boolean ignoreMemory, boolean sync, LifecycleConfig config) {
        this.mPreloadAppController.preloadAppEnqueue(packageName, ignoreMemory, config);
        return 500;
    }

    public int killPreloadApp(String packageName) {
        return this.mPreloadAppController.killPreloadApp(packageName);
    }

    public boolean frequentlyKilledForPreload(String packageName) {
        return this.mProcessStarter.frequentlyKilledForPreload(packageName);
    }

    /* JADX WARN: Multi-variable type inference failed */
    public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) throws RemoteException {
        new ProcessMangaerShellCommand().exec(this, in, out, err, args, callback, resultReceiver);
    }

    /* loaded from: classes.dex */
    class ProcessMangaerShellCommand extends ShellCommand {
        ProcessMangaerShellCommand() {
            ProcessManagerService.this = this$0;
        }

        public int onCommand(String cmd) {
            boolean z;
            if (cmd == null) {
                return handleDefaultCommands(cmd);
            }
            PrintWriter pw = getOutPrintWriter();
            try {
                switch (cmd.hashCode()) {
                    case 3580:
                        if (cmd.equals("pl")) {
                            z = false;
                            break;
                        }
                    default:
                        z = true;
                        break;
                }
            } catch (Exception e) {
                pw.println(e);
            }
            switch (z) {
                case false:
                    ProcessManagerService.this.startPreloadApp(getNextArgRequired(), Boolean.parseBoolean(getNextArg()), true, LifecycleConfig.create(Integer.parseInt(getNextArg())));
                    return -1;
                default:
                    return handleDefaultCommands(cmd);
            }
        }

        public void onHelp() {
            PrintWriter pw = getOutPrintWriter();
            pw.println("Process manager commands:");
            pw.println("  pl PACKAGENAME");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class LocalService extends ProcessManagerInternal {
        private LocalService() {
            ProcessManagerService.this = r1;
        }

        @Override // com.miui.server.process.ProcessManagerInternal
        public void notifyForegroundInfoChanged(ForegroundInfoManager.FgActivityChangedInfo info) {
            ProcessManagerService.this.notifyForegroundInfoChanged(info);
        }

        @Override // com.miui.server.process.ProcessManagerInternal
        public boolean isForegroundApp(String pkgName, int uid) {
            return ProcessManagerService.this.mForegroundInfoManager.isForegroundApp(pkgName, uid);
        }

        @Override // com.miui.server.process.ProcessManagerInternal
        public void notifyForegroundWindowChanged(ForegroundInfoManager.FgWindowChangedInfo info) {
            ProcessManagerService.this.notifyForegroundWindowChanged(info);
        }

        @Override // com.miui.server.process.ProcessManagerInternal
        public void notifyActivityChanged(ComponentName curComponentActivity) {
            ProcessManagerService.this.notifyActivityChanged(curComponentActivity);
        }

        @Override // com.miui.server.process.ProcessManagerInternal
        public void notifyProcessStarted(ProcessRecord app) {
            ProcessManagerService.this.notifyProcessStarted(app);
        }

        @Override // com.miui.server.process.ProcessManagerInternal
        public void notifyProcessKill(ProcessRecord app, String reason) {
            ProcessManagerService.this.notifyProcessKill(app, reason);
        }

        @Override // com.miui.server.process.ProcessManagerInternal
        public void notifyProcessDied(ProcessRecord app) {
            ProcessManagerService.this.notifyProcessDied(app);
        }

        @Override // com.miui.server.process.ProcessManagerInternal
        public void updateProcessForegroundLocked(int pid) {
            ProcessManagerService.this.mProcessPolicy.updateProcessForegroundLocked(ProcessManagerService.this.getProcessRecordByPid(pid));
        }

        @Override // com.miui.server.process.ProcessManagerInternal
        public void forceStopPackage(String packageName, int userId, String reason) {
            ProcessManagerService.this.mActivityManagerService.forceStopPackage(packageName, userId);
        }

        @Override // com.miui.server.process.ProcessManagerInternal
        public ApplicationInfo getMultiWindowForegroundAppInfoLocked() {
            return WindowProcessUtils.getMultiWindowForegroundAppInfoLocked(ProcessManagerService.this.mActivityManagerService.mActivityTaskManager);
        }

        @Override // com.miui.server.process.ProcessManagerInternal
        public IMiuiApplicationThread getMiuiApplicationThread(int pid) {
            return ProcessManagerService.this.mMiuiApplicationThreadManager.getMiuiApplicationThread(pid);
        }

        @Override // com.miui.server.process.ProcessManagerInternal
        public boolean isPackageFastBootEnable(String packageName, int uid, boolean checkPass) {
            return ProcessManagerService.this.mProcessPolicy.isFastBootEnable(packageName, uid, checkPass);
        }

        @Override // com.miui.server.process.ProcessManagerInternal
        public List<RunningProcessInfo> getAllRunningProcessInfo() {
            List<RunningProcessInfo> processInfoList = new ArrayList<>();
            synchronized (ProcessManagerService.this.mActivityManagerService.mPidsSelfLocked) {
                for (int i = ProcessManagerService.this.mActivityManagerService.mPidsSelfLocked.size() - 1; i >= 0; i--) {
                    ProcessRecord proc = ProcessManagerService.this.mActivityManagerService.mPidsSelfLocked.valueAt(i);
                    if (proc != null) {
                        processInfoList.add(ProcessManagerService.this.generateRunningProcessInfo(proc));
                    }
                }
            }
            return processInfoList;
        }

        @Override // com.miui.server.process.ProcessManagerInternal
        public void setSpeedTestState(boolean state) {
            ProcessManagerService.this.mPreloadAppController.setSpeedTestState(state);
        }

        @Override // com.miui.server.process.ProcessManagerInternal
        public boolean checkAppFgServices(int pid) {
            ProcessRecord app = ProcessManagerService.this.getProcessRecordByPid(pid);
            if (app != null) {
                return ProcessManagerService.this.isAppHasForegroundServices(app);
            }
            return false;
        }

        @Override // com.miui.server.process.ProcessManagerInternal
        public boolean isAllowRestartProcessLock(String processName, int flag, int uid, String pkgName, String callerPackage, HostingRecord hostingRecord) {
            return ProcessManagerService.this.mProcessStarter.isAllowRestartProcessLock(processName, flag, uid, pkgName, callerPackage, hostingRecord);
        }

        @Override // com.miui.server.process.ProcessManagerInternal
        public boolean restartDiedAppOrNot(ProcessRecord app, boolean isHomeApp, boolean allowRestart, boolean fromBinderDied) {
            if (fromBinderDied) {
                return ProcessManagerService.this.mProcessStarter.restartDiedAppOrNot(app, isHomeApp, allowRestart);
            }
            return allowRestart;
        }
    }

    private final int ringAdvance(int x, int increment, int ringSize) {
        int x2 = x + increment;
        if (x2 < 0) {
            return ringSize - 1;
        }
        if (x2 < ringSize) {
            return x2;
        }
        return 0;
    }

    private void addConfigToHistory(ProcessConfig config) {
        config.setKillingClockTime(System.currentTimeMillis());
        int ringAdvance = ringAdvance(this.mHistoryNext, 1, 30);
        this.mHistoryNext = ringAdvance;
        this.mProcessConfigHistory[ringAdvance] = config;
    }

    public void enableHomeSchedBoost(boolean enable) throws RemoteException {
        if (!checkPermission()) {
            Slog.e("ProcessManager", "Permission Denial: can't enable Home Sched Boost");
        }
        SchedBoostManagerInternal si = (SchedBoostManagerInternal) LocalServices.getService(SchedBoostManagerInternal.class);
        if (si != null) {
            si.enableSchedBoost(enable);
        }
    }

    public void beginSchedThreads(int[] tids, long duration, int pid, int mode) throws RemoteException {
        if (!checkPermission() && mode != 3) {
            String msg = "Permission Denial: ProcessManager.beginSchedThreads() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid();
            Slog.d("ProcessManager", msg);
            throw new SecurityException(msg);
        }
        SchedBoostManagerInternal si = (SchedBoostManagerInternal) LocalServices.getService(SchedBoostManagerInternal.class);
        if (si == null || tids == null || tids.length < 1) {
            return;
        }
        if (pid < 0) {
            pid = Binder.getCallingPid();
        }
        String pkgName = getPackageNameByPid(pid);
        boolean isEnabled = RealTimeModeControllerStub.get().checkCallerPermission(pkgName);
        if (!isEnabled) {
            Slog.d("ProcessManager", "beginSchedThreads is not Enabled");
        } else {
            si.beginSchedThreads(tids, duration, pkgName, mode);
        }
    }

    public void reportGameScene(String packageName, int gameScene, int appState) throws RemoteException {
        if (!checkPermission()) {
            String msg = "Permission Denial: ProcessManager.reportGameScene() from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid();
            Slog.w("ProcessManager", msg);
            throw new SecurityException(msg);
        }
        Log.d("ProcessManager", "pms : packageName = " + packageName + " gameScene =" + gameScene + " appState = " + appState);
        OomAdjusterImpl.getInstance().updateGameSceneRecordMap(packageName, gameScene, appState);
    }

    public void notifyBluetoothEvent(boolean isConnect, int bleType, int uid, int pid, String pkg, int data) throws RemoteException {
        SmartPowerService.getInstance().onBluetoothEvent(isConnect, bleType, uid, pid, pkg, data);
    }

    public void reportTrackStatus(int uid, int pid, int sessionId, boolean isMuted) throws RemoteException {
        SmartPowerService.getInstance().reportTrackStatus(uid, pid, sessionId, isMuted);
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        ProcessConfig config;
        if (!checkPermission()) {
            pw.println("Permission Denial: can't dump ProcessManager from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
            return;
        }
        pw.println("Process Config:");
        int lastIndex = this.mHistoryNext;
        int ringIndex = this.mHistoryNext;
        int i = 0;
        while (ringIndex != -1 && (config = this.mProcessConfigHistory[ringIndex]) != null) {
            pw.print("  #");
            pw.print(i);
            pw.print(": ");
            pw.println(config.toString());
            ringIndex = ringAdvance(ringIndex, -1, 30);
            i++;
            if (ringIndex == lastIndex) {
                break;
            }
        }
        this.mForegroundInfoManager.dump(pw, "    ");
        this.mProcessPolicy.dump(pw, "    ");
    }

    private void probeCgroupVersion() {
        try {
            if (CONFIG_PER_APP_MEMCG && new File("/dev/memcg/apps/uid_1000").exists()) {
                mCgroupPath = MEM_CGROUP_PATH;
                return;
            }
            for (int i = CGROUP_PATH_PREFIXES.length - 1; i >= 0; i--) {
                StringBuilder sb = new StringBuilder();
                String[] strArr = CGROUP_PATH_PREFIXES;
                if (new File(sb.append(strArr[i]).append(1000).toString()).exists()) {
                    mCgroupPath = strArr[i];
                    return;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getCgroupFilePath(int uid, int pid) {
        return mCgroupPath + uid + CGROUP_PID_PREFIX + pid + CGROUP_PROCS;
    }
}
