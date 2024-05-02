package com.android.server.am;

import android.app.AppGlobals;
import android.content.pm.ApplicationInfo;
import android.os.Binder;
import android.os.Handler;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.spc.PressureStateSettings;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import com.android.server.am.AppStateManager;
import com.miui.server.smartpower.SmartPowerPolicyManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import miui.os.Build;
import miui.process.PreloadProcessData;
import miui.process.ProcessManager;
/* loaded from: classes.dex */
public class ProcessStarter {
    private static final int APP_PROTECTION_TIMEOUT = 1800000;
    private static final String CAMERA_PACKAGE_NAME = "com.android.camera";
    private static final int DISABLE_PROC_PROTECT_THRESHOLD = 5;
    private static final int DISABLE_RESTART_PROC_THRESHOLD = 3;
    private static final int MAX_PROTECT_APP = 5;
    private static final long PROCESS_RESTART_TIMEOUT = 2000;
    private static final List<String> PROTECT_PROCESS_LIST;
    public static final long REDUCE_DIED_PROC_COUNT_DELAY = 120000;
    private static final int SKIP_PRELOAD_COUNT_LIMIT = 2;
    static final long SKIP_PRELOAD_KILLED_TIME_OUT = 300000;
    private static final int SKIP_RESTART_COUNT_LIMIT = 3;
    private static final String TAG = "ProcessStarter";
    private static ArrayList<String> sNoCheckCallerPackages;
    private final ActivityManagerService mAms;
    private long mCameraStartTime;
    private Handler mHandler;
    private ProcessManagerService mPms;
    private SmartPowerService mSmartPowerService;
    private static List<String> mProcRestartWhiteList = new ArrayList();
    public static final boolean CONFIG_CAM_LOWMEMDEV_RESTART = SystemProperties.getBoolean("persist.sys.cam_lowmem_restart", false);
    private static List<String> mProcRestartCallerWhiteList = new ArrayList();
    private boolean mSystemReady = false;
    public Map<String, Integer> mKilledProcessRecordMap = new ConcurrentHashMap();
    private Map<String, Integer> mProcessDiedRecordMap = new HashMap();
    private SparseArray<List<ProcessPriorityInfo>> mLastProcessesInfo = new SparseArray<>();
    private String mForegroundPackageName = "";

    /* loaded from: classes.dex */
    public static class ProcessPriorityInfo {
        ProcessRecord app = null;
        int maxAdj = ProcessManager.DEFAULT_MAX_ADJ;
        int maxProcState = 20;
    }

    static {
        ArrayList arrayList = new ArrayList();
        PROTECT_PROCESS_LIST = arrayList;
        arrayList.add("com.tencent.mm");
        arrayList.add("com.tencent.mm:push");
        mProcRestartWhiteList.add("com.tencent.mm:push");
        ArrayList<String> arrayList2 = new ArrayList<>();
        sNoCheckCallerPackages = arrayList2;
        arrayList2.add("Webview");
        sNoCheckCallerPackages.add("SdkSandbox");
    }

    public ProcessStarter(ProcessManagerService pms, ActivityManagerService ams, Handler handler) {
        this.mPms = pms;
        this.mAms = ams;
        this.mHandler = handler;
    }

    public void systemReady() {
        List<String> whiteList = this.mPms.getProcessPolicy().getWhiteList(1);
        mProcRestartWhiteList.addAll(whiteList);
        mProcRestartCallerWhiteList.addAll(whiteList);
        this.mSmartPowerService = SmartPowerService.getInstance();
        this.mSystemReady = true;
    }

    public ProcessRecord startProcessLocked(String packageName, String processName, int userId, String hostingType) {
        String processName2;
        int callingPid = Binder.getCallingPid();
        if (!TextUtils.isEmpty(processName)) {
            processName2 = processName;
        } else {
            processName2 = packageName;
        }
        try {
            ApplicationInfo info = AppGlobals.getPackageManager().getApplicationInfo(packageName, 1024L, userId);
            if (info == null) {
                return null;
            }
            ProcessRecord app = (ProcessRecord) this.mAms.mProcessList.getProcessNamesLOSP().get(processName2, info.uid);
            if (app != null) {
                if (app.isPersistent()) {
                    Slog.w(TAG, "process: " + processName2 + " is persistent, skip!");
                    return null;
                }
                Slog.i(TAG, "process: " + processName2 + " already exits, just protect");
                return app;
            }
            ProcessRecord newApp = this.mAms.startProcessLocked(processName2, info, false, 0, new HostingRecord(hostingType, processName2), 0, false, false, ActivityManagerServiceImpl.getInstance().getPackageNameByPid(callingPid));
            if (newApp != null) {
                this.mAms.updateLruProcessLocked(newApp, false, (ProcessRecord) null);
                return newApp;
            }
            Slog.w(TAG, "startProcess :" + processName2 + " failed!");
            return null;
        } catch (RemoteException e) {
            Slog.w(TAG, "error in getApplicationInfo!", e);
            return null;
        }
    }

    public int startProcesses(List<PreloadProcessData> dataList, int startProcessCount, boolean ignoreMemory, int userId, int flag) {
        synchronized (this.mAms) {
            int protectCount = 0;
            try {
                try {
                    restoreLastProcessesInfoLocked(flag);
                    this.mPms.mHandler.removeMessages(flag);
                    Iterator<PreloadProcessData> it = dataList.iterator();
                    while (true) {
                        if (!it.hasNext()) {
                            break;
                        }
                        PreloadProcessData preloadProcessData = it.next();
                        if (0 >= startProcessCount) {
                            break;
                        } else if (preloadProcessData != null && !TextUtils.isEmpty(preloadProcessData.getPackageName())) {
                            String process = preloadProcessData.getPackageName();
                            ProcessRecord app = startProcessLocked(process, process, userId, makeHostingTypeFromFlag(flag));
                            if (app != null) {
                                saveProcessInfoLocked(app, flag);
                                addProtectionLocked(app, flag);
                                protectCount++;
                                if (protectCount >= 5) {
                                    Slog.w(TAG, "preload and protect processes max limit is: 5, while now count is: " + startProcessCount);
                                    break;
                                }
                                this.mHandler.sendEmptyMessageDelayed(flag, SmartPowerPolicyManager.UPDATE_USAGESTATS_DURATION);
                            } else {
                                continue;
                            }
                        }
                    }
                    return 0;
                } catch (Throwable th) {
                    th = th;
                    throw th;
                }
            } catch (Throwable th2) {
                th = th2;
                throw th;
            }
        }
    }

    public boolean isAllowRestartProcessLock(String processName, int flag, int uid, String pkgName, String callerPackage, HostingRecord hostingRecord) {
        if (PressureStateSettings.PROCESS_RESTART_INTERCEPT_ENABLE && this.mSystemReady && hostingRecord != null) {
            if (!hostingRecord.getType().contains("activity")) {
                if (isProcRestartInterceptScenario() && !SmartPowerPolicyManager.sCtsModeEnable) {
                    if (this.mPms.isAllowAutoStart(pkgName, uid) || mProcRestartWhiteList.contains(processName) || mProcRestartCallerWhiteList.contains(callerPackage) || this.mPms.getProcessPolicy().isInProcessStaticWhiteList(processName) || this.mSmartPowerService.isProcessWhiteList(ProcessCleanerBase.SMART_POWER_PROTECT_APP_FLAGS, pkgName, processName) || isProcessPerceptible(callerPackage)) {
                        return true;
                    }
                    if ("com.android.camera".equals(this.mForegroundPackageName)) {
                        long nowTime = SystemClock.uptimeMillis();
                        if (nowTime - this.mCameraStartTime < 5000 && !isSystemApp(flag, uid)) {
                            Slog.d(TAG, "camera start scenario!");
                            return false;
                        }
                    }
                    Integer procDiedCount = this.mProcessDiedRecordMap.get(processName);
                    if (procDiedCount == null || procDiedCount.intValue() <= 3) {
                        return true;
                    }
                    Slog.d(TAG, "proc frequent died! proc = " + processName + " callerPkg = " + callerPackage);
                    return false;
                }
            }
        }
        return true;
    }

    public void recordKillProcessIfNeeded(String processName, String reason) {
        for (String lowMemReason : ProcessPolicy.sLowMemKillProcReasons) {
            if (reason.contains(lowMemReason)) {
                increaseRecordCount(processName, this.mKilledProcessRecordMap);
                reduceRecordCountDelay(processName, this.mKilledProcessRecordMap, SKIP_PRELOAD_KILLED_TIME_OUT);
            }
        }
    }

    public void recordDiedProcessIfNeeded(final String pkgName, final String processName, final int uid) {
        if (processName != null) {
            Integer count = this.mProcessDiedRecordMap.get(processName);
            if (count == null) {
                count = 0;
            }
            this.mProcessDiedRecordMap.put(processName, Integer.valueOf(count.intValue() + 1));
            reduceRecordCountDelay(processName, this.mProcessDiedRecordMap, REDUCE_DIED_PROC_COUNT_DELAY);
        }
        if (PROTECT_PROCESS_LIST.contains(processName)) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.am.ProcessStarter.1
                @Override // java.lang.Runnable
                public void run() {
                    if (!ProcessStarter.this.mPms.isAllowAutoStart(pkgName, uid)) {
                        Slog.d(ProcessStarter.TAG, "process " + processName + " No restart permission!");
                        return;
                    }
                    Integer value = (Integer) ProcessStarter.this.mProcessDiedRecordMap.get(processName);
                    if (value != null && value.intValue() >= 5) {
                        Slog.d(ProcessStarter.TAG, "process " + processName + " frequent died!");
                        return;
                    }
                    synchronized (ProcessStarter.this.mAms) {
                        ProcessStarter.this.startProcessLocked(pkgName, processName, UserHandle.getUserId(uid), ProcessStarter.makeHostingTypeFromFlag(2));
                    }
                }
            });
        }
    }

    public void restartCameraIfNeeded(final String packageName, final String processName, final int uid) {
        if (!TextUtils.equals(processName, "com.android.camera")) {
            return;
        }
        boolean isLowMemory = ProcessUtils.isLowMemory();
        boolean isLowMemDevice = Build.TOTAL_RAM <= 4;
        if (isLowMemory || isLowMemDevice) {
            if (CONFIG_CAM_LOWMEMDEV_RESTART) {
                Slog.d(TAG, "lowMem device restart " + processName + " as required!");
            } else {
                Slog.d(TAG, "skip restart camera due to lowMemory or lowMemDevice");
                return;
            }
        }
        int userId = UserHandle.getUserId(uid);
        if (!this.mAms.mUserController.isUserRunning(userId, 4)) {
            Slog.i(TAG, "user " + userId + " is still locked. Cannot restart process " + processName);
        } else {
            this.mHandler.postDelayed(new Runnable() { // from class: com.android.server.am.ProcessStarter.2
                @Override // java.lang.Runnable
                public void run() {
                    if (ProcessStarter.this.mPms.isAllowAutoStart(packageName, uid)) {
                        Integer killCount = ProcessStarter.this.mKilledProcessRecordMap.get(processName);
                        if (killCount != null && killCount.intValue() >= 3) {
                            Log.w(ProcessStarter.TAG, "skip restart " + processName + " due to too much kill!");
                            return;
                        }
                        synchronized (ProcessStarter.this.mAms) {
                            ProcessStarter.this.startProcessLocked(packageName, processName, UserHandle.getUserId(uid), ProcessStarter.makeHostingTypeFromFlag(2));
                        }
                    }
                }
            }, PROCESS_RESTART_TIMEOUT);
        }
    }

    private boolean isSystemApp(int flag, int uid) {
        return (flag & 129) != 0 || 1000 == uid;
    }

    private boolean isProcRestartInterceptScenario() {
        return SystemPressureController.getInstance().isGameApp(this.mForegroundPackageName) || "com.android.camera".equals(this.mForegroundPackageName);
    }

    private void reduceRecordCountDelay(final String processName, final Map<String, Integer> recordMap, long delay) {
        this.mHandler.postDelayed(new Runnable() { // from class: com.android.server.am.ProcessStarter.3
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

    private void increaseRecordCount(String processName, Map<String, Integer> recordMap) {
        Integer expCount = recordMap.get(processName);
        if (expCount == null) {
            expCount = 0;
        }
        recordMap.put(processName, Integer.valueOf(expCount.intValue() + 1));
    }

    public boolean isAllowPreloadProcess(List<PreloadProcessData> dataList, int flag) {
        if ((flag & 1) != 0) {
            Iterator dataIterator = dataList.iterator();
            while (dataIterator.hasNext()) {
                PreloadProcessData data = dataIterator.next();
                if (data != null && !TextUtils.isEmpty(data.getPackageName()) && frequentlyKilledForPreload(data.getPackageName())) {
                    dataIterator.remove();
                    Slog.w(TAG, "skip start " + data.getPackageName() + ", because of errors or killed by user before");
                }
            }
            return dataList.size() > 0;
        }
        return true;
    }

    public boolean frequentlyKilledForPreload(String packageName) {
        for (String killedProcess : this.mKilledProcessRecordMap.keySet()) {
            Integer count = this.mKilledProcessRecordMap.get(killedProcess);
            if (count != null && count.intValue() > 0 && count.intValue() >= 2 && killedProcess.startsWith(packageName)) {
                return true;
            }
        }
        return false;
    }

    public void foregroundActivityChanged(String packageName) {
        if (packageName.equals(this.mForegroundPackageName)) {
            return;
        }
        this.mForegroundPackageName = packageName;
        if (packageName.equals("com.android.camera")) {
            this.mCameraStartTime = SystemClock.uptimeMillis();
        }
    }

    public boolean restartDiedAppOrNot(ProcessRecord app, boolean isHomeApp, boolean allowRestart) {
        if (SystemPressureController.getInstance().isCtsModeEnable()) {
            return true;
        }
        if (isHomeApp || !this.mSystemReady) {
            return allowRestart;
        }
        if (app.mState.getCurProcState() > 10 || Process.isIsolated(app.uid)) {
            return false;
        }
        return this.mPms.isAllowAutoStart(app.info.packageName, app.info.uid) || isProcessPerceptible(app.uid, app.info.packageName);
    }

    public boolean isProcessPerceptible(int uid, String pkgName) {
        ArrayList<AppStateManager.AppState.RunningProcess> runningProcesses = this.mSmartPowerService.getLruProcesses(uid, pkgName);
        Iterator<AppStateManager.AppState.RunningProcess> it = runningProcesses.iterator();
        while (it.hasNext()) {
            AppStateManager.AppState.RunningProcess runningProc = it.next();
            if (runningProc.isProcessPerceptible()) {
                return true;
            }
        }
        return false;
    }

    public boolean isProcessPerceptible(String callerPackage) {
        ArrayList<AppStateManager.AppState> appList = this.mSmartPowerService.getAllAppState();
        if (appList == null || appList.isEmpty()) {
            return true;
        }
        Iterator<AppStateManager.AppState> it = appList.iterator();
        while (it.hasNext()) {
            AppStateManager.AppState appState = it.next();
            if (!sNoCheckCallerPackages.contains(callerPackage)) {
                if (appState.getPackageName().equals(callerPackage)) {
                    ArrayList<AppStateManager.AppState.RunningProcess> runningProcList = appState.getRunningProcessList();
                    Iterator<AppStateManager.AppState.RunningProcess> it2 = runningProcList.iterator();
                    while (it2.hasNext()) {
                        AppStateManager.AppState.RunningProcess runningProc = it2.next();
                        if (runningProc.isProcessPerceptible()) {
                            return true;
                        }
                    }
                    return false;
                }
            } else {
                return false;
            }
        }
        return false;
    }

    void saveProcessInfoLocked(ProcessRecord app, int flag) {
        ProcessPriorityInfo lastProcess = new ProcessPriorityInfo();
        lastProcess.app = app;
        lastProcess.maxAdj = app.mState.getMaxAdj();
        lastProcess.maxProcState = ProcessPolicy.getAppMaxProcState(app);
        List<ProcessPriorityInfo> lastProcessList = this.mLastProcessesInfo.get(flag);
        if (lastProcessList == null) {
            lastProcessList = new ArrayList();
            this.mLastProcessesInfo.put(flag, lastProcessList);
        }
        lastProcessList.add(lastProcess);
    }

    void addProtectionLocked(ProcessRecord app, int flag) {
        switch (flag) {
            case 1:
                app.mState.setMaxAdj(ProcessManager.AI_MAX_ADJ);
                ProcessPolicy.setAppMaxProcState(app, 13);
                return;
            default:
                return;
        }
    }

    public void restoreLastProcessesInfoLocked(int flag) {
        List<ProcessPriorityInfo> lastProcessInfoList = this.mLastProcessesInfo.get(flag);
        if (lastProcessInfoList != null && !lastProcessInfoList.isEmpty()) {
            for (int i = 0; i < lastProcessInfoList.size(); i++) {
                ProcessPriorityInfo process = lastProcessInfoList.get(i);
                if (this.mPms.getProcessPolicy().isLockedApplication(process.app.processName, process.app.userId)) {
                    Slog.i(TAG, "user: " + process.app.userId + ", packageName: " + process.app.processName + " was Locked.");
                    process.app.mState.setMaxAdj(ProcessManager.LOCKED_MAX_ADJ);
                    ProcessPolicy.setAppMaxProcState(process.app, ProcessManager.LOCKED_MAX_PROCESS_STATE);
                } else {
                    process.app.mState.setMaxAdj(process.maxAdj);
                    ProcessPolicy.setAppMaxProcState(process.app, process.maxProcState);
                }
            }
            lastProcessInfoList.clear();
        }
    }

    public static String makeHostingTypeFromFlag(int flag) {
        switch (flag) {
            case 1:
                return "AI";
            case 2:
                return "FastRestart";
            default:
                return "unknown";
        }
    }
}
