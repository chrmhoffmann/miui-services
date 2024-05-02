package com.android.server.am;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.spc.PressureStateSettings;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Slog;
import android.util.SparseArray;
import com.android.server.LocalServices;
import com.android.server.am.AppStateManager;
import com.android.server.am.SystemPressureController;
import com.android.server.input.pocketmode.MiuiPocketModeSensorWrapper;
import com.miui.server.smartpower.SmartPowerSettings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
/* loaded from: classes.dex */
public class ProcessMemoryCleaner extends ProcessCleanerBase {
    private static final int AUDIO_PROC_PAUSE_PROTECT_TIME = 300000;
    private static final String CAMERA_PACKAGE_NAME = "com.android.camera";
    private static final double COMM_USED_PSS_LIMIT_HIGH_FACTOR = 1.33d;
    private static final double COMM_USED_PSS_LIMIT_LITE_FACTOR = 0.67d;
    private static final double COMM_USED_PSS_LIMIT_USUAL_FACTOR = 1.0d;
    private static final int DEF_MIN_KILL_PROC_ADJ = 200;
    private static final String HOME_PACKAGE_NAME = "com.miui.home";
    private static final String KILLING_PROC_REASON = "MiuiMemoryService";
    private static final int KILL_PROC_COUNT_LIMIT = 10;
    private static final double MEM_EXCEPTION_TH_HIGH_FACTOR = 1.25d;
    private static final double MEM_EXCEPTION_TH_LITE_FACTOR = 0.625d;
    private static final double MEM_EXCEPTION_TH_MID_FACTOR = 0.75d;
    private static final double MEM_EXCEPTION_TH_USUAL_FACTOR = 1.0d;
    private static final int MSG_APP_SWITCH_BG_EXCEPTION = 1;
    private static final int MSG_PAD_SMALL_WINDOW_CLEAN = 3;
    private static final int MSG_PROCESS_BG_COMPACT = 4;
    private static final int MSG_REGISTER_CLOUD_OBSERVER = 2;
    private static final long PAD_SMALL_WINDOW_CLEAN_TIME = 5000;
    private static final String PERF_PROC_THRESHOLD_CLOUD = "perf_proc_threshold";
    private static final int PROCESS_PRIORITY_FACTOR = 1000;
    private static final int PROC_BG_COMPACT_DELAY_TIME = 4000;
    private static final String PROC_COMM_PSS_LIMIT_CLOUD = "perf_proc_common_pss_limit";
    private static final String PROC_MEM_EXCEPTION_THRESHOLD_CLOUD = "perf_proc_mem_exception_threshold";
    private static final String PROC_PROTECT_LIST_CLOUD = "perf_proc_protect_list";
    private static final String PROC_SWITCH_BG_DELAY_TIME_CLOUD = "perf_proc_switch_Bg_time";
    private static final String REASON_PAD_SMALL_WINDOW_CLEAN = "PadSmallWindowClean";
    public static final String TAG = "ProcessMemoryCleaner";
    private static List<String> mProtectProcessList;
    private ActivityManagerService mAMS;
    private Context mContext;
    private H mHandler;
    private MiuiMemoryServiceInternal mMiuiMemoryService;
    private ProcessManagerService mPMS;
    private List<String> mPackageWhiteList;
    private SystemPressureController.ProcessCompactStateListener mProcessCompactStateListener;
    private ProcessPolicy mProcessPolicy;
    private ProcMemCleanerStatistics mStatistics;
    public static final boolean DEBUG = PressureStateSettings.DEBUG_ALL;
    private static final long RAM_SIZE_1GB = 1073741824;
    private static final long TOTAL_MEMEORY_GB = Process.getTotalMemory() / RAM_SIZE_1GB;
    private int mAppSwitchBgExceptDelayTime = 30000;
    private long mCommonUsedPssLimitKB = PressureStateSettings.PROC_COMMON_USED_PSS_LIMIT_KB;
    private HandlerThread mHandlerTh = new HandlerThread("ProcessMemoryCleanerTh", -2);
    private long mMemExceptionThresholdKB = PressureStateSettings.PROC_MEM_EXCEPTION_PSS_LIMIT_KB;
    private String mForegroundPkg = "";
    private int mForegroundUid = -1;
    private long mLastPadSmallWindowUpdateTime = 0;
    private boolean mIsInit = false;

    static {
        ArrayList arrayList = new ArrayList();
        mProtectProcessList = arrayList;
        arrayList.add("com.tencent.mm");
        mProtectProcessList.add("com.tencent.mm:push");
        mProtectProcessList.add("com.android.externalstorage");
    }

    public ProcessMemoryCleaner(ActivityManagerService ams) {
        super(ams);
        this.mAMS = ams;
    }

    @Override // com.android.server.am.ProcessCleanerBase
    public void systemReady(Context context, ProcessManagerService pms) {
        String[] pckWhiteList;
        super.systemReady(context, pms);
        if (DEBUG) {
            Slog.d(TAG, "ProcessesCleaner init");
        }
        this.mContext = context;
        this.mPMS = pms;
        this.mHandlerTh.start();
        this.mHandler = new H(this.mHandlerTh.getLooper());
        Process.setThreadGroupAndCpuset(this.mHandlerTh.getThreadId(), 1);
        this.mProcessPolicy = this.mPMS.getProcessPolicy();
        this.mMiuiMemoryService = (MiuiMemoryServiceInternal) LocalServices.getService(MiuiMemoryServiceInternal.class);
        this.mStatistics = ProcMemCleanerStatistics.getInstance();
        this.mPackageWhiteList = this.mProcessPolicy.getWhiteList(53);
        if (!TextUtils.isEmpty(PressureStateSettings.WHITE_LIST_PKG) && (pckWhiteList = PressureStateSettings.WHITE_LIST_PKG.split(";")) != null && pckWhiteList.length > 0) {
            this.mPackageWhiteList.addAll(new ArrayList(Arrays.asList(pckWhiteList)));
        }
        computeMemExceptionThreshold(PressureStateSettings.PROC_MEM_EXCEPTION_PSS_LIMIT_KB);
        computeCommonUsedAppPssThreshold(this.mCommonUsedPssLimitKB);
        Message msg = this.mHandler.obtainMessage(2);
        this.mHandler.sendMessage(msg);
        this.mIsInit = true;
    }

    public boolean isInit() {
        return this.mIsInit;
    }

    public static int getProcPriority(AppStateManager.AppState.RunningProcess runningProc) {
        return (runningProc.getAdj() * 1000) + runningProc.getPriorityScore();
    }

    public void setProcessCompactStateListener(SystemPressureController.ProcessCompactStateListener listener) {
        this.mProcessCompactStateListener = listener;
    }

    public boolean scanProcessAndCleanUpMemory(long targetReleaseMem) {
        if (this.mContext == null) {
            return false;
        }
        if (DEBUG) {
            Slog.d(TAG, "Start clean up memory.....");
        }
        List<AppStateManager.AppState.RunningProcess> runningProcList = new ArrayList<>();
        ArrayList<AppStateManager.AppState> appStateList = this.mSmartPowerService.getAllAppState();
        Iterator<AppStateManager.AppState> it = appStateList.iterator();
        while (it.hasNext()) {
            AppStateManager.AppState appState = it.next();
            if (!appState.isVsible()) {
                Iterator<AppStateManager.AppState.RunningProcess> it2 = appState.getRunningProcessList().iterator();
                while (it2.hasNext()) {
                    AppStateManager.AppState.RunningProcess runningProc = it2.next();
                    if (runningProc.getAdj() > 0 && !isIsolatedByAdj(runningProc) && !runningProc.isProcessPerceptible() && !runningProc.hasForegrundService() && !isSystemHighPrioProc(runningProc) && !isLastMusicPlayProcess(runningProc.getPid()) && !containInWhiteList(runningProc) && !isAutoStartProcess(appState, runningProc) && !this.mSmartPowerService.isProcessWhiteList(ProcessCleanerBase.SMART_POWER_PROTECT_APP_FLAGS, runningProc.getPackageName(), runningProc.getProcessName())) {
                        runningProcList.add(runningProc);
                    }
                }
            }
        }
        return cleanUpMemory(runningProcList, targetReleaseMem);
    }

    private boolean isAutoStartProcess(AppStateManager.AppState appState, AppStateManager.AppState.RunningProcess runningProc) {
        return appState.isAutoStartApp() && runningProc.getAdj() <= 800;
    }

    private boolean isIsolatedByAdj(AppStateManager.AppState.RunningProcess runningProc) {
        return isIsolatedProcess(runningProc);
    }

    private boolean isIsolatedProcess(AppStateManager.AppState.RunningProcess runningProc) {
        return Process.isIsolated(runningProc.getUid()) || runningProc.getProcessName().startsWith(new StringBuilder().append(runningProc.getPackageName()).append(":sandboxed_").toString());
    }

    private boolean isSystemHighPrioProc(AppStateManager.AppState.RunningProcess runningProc) {
        if (runningProc.getAdj() <= 200 && isSystemApp(runningProc.getUid(), runningProc.getPackageName())) {
            return true;
        }
        return false;
    }

    private boolean isLastMusicPlayProcess(int pid) {
        long lastMusicPlayTime = this.mSmartPowerService.getLastMusicPlayTimeStamp(pid);
        if (SystemClock.uptimeMillis() - lastMusicPlayTime <= 300000) {
            return true;
        }
        return false;
    }

    private boolean cleanUpMemory(List<AppStateManager.AppState.RunningProcess> runningProcList, long targetReleaseMem) {
        Collections.sort(runningProcList, new Comparator<AppStateManager.AppState.RunningProcess>() { // from class: com.android.server.am.ProcessMemoryCleaner.1
            public int compare(AppStateManager.AppState.RunningProcess o1, AppStateManager.AppState.RunningProcess o2) {
                boolean isHav1 = o1.hasActivity();
                boolean isHav2 = o2.hasActivity();
                if ((isHav1 && isHav2) || (!isHav1 && !isHav2)) {
                    return ProcessMemoryCleaner.getProcPriority(o2) - ProcessMemoryCleaner.getProcPriority(o1);
                }
                if (isHav1) {
                    return 1;
                }
                return -1;
            }
        });
        long nowTime = System.currentTimeMillis();
        this.mStatistics.checkLastKillTime(nowTime);
        if (DEBUG) {
            debugAppGroupToString(runningProcList);
        }
        long releasePssByProcClean = 0;
        int killProcCount = 0;
        for (AppStateManager.AppState.RunningProcess runningProc : runningProcList) {
            if (!this.mStatistics.isLastKillProcess(runningProc.getProcessName(), runningProc.getUid(), nowTime) && (runningProc.getAdj() < 900 || isIsolatedProcess(runningProc))) {
                if (runningProc.hasActivity()) {
                    return true;
                }
                long pss = killProcess(runningProc, 0, ProcMemCleanerStatistics.REASON_CLEAN_UP_MEM);
                if (targetReleaseMem <= 0) {
                    continue;
                } else if (PressureStateSettings.ONLY_KILL_ONE_PKG && runningProc.hasActivity()) {
                    if (DEBUG) {
                        Slog.d(TAG, "----skip kill: " + processToString(runningProc));
                    }
                    return true;
                } else {
                    releasePssByProcClean += pss;
                    if (releasePssByProcClean >= targetReleaseMem || (killProcCount = killProcCount + 1) >= 10) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void compactProcess(AppStateManager.AppState.RunningProcess processInfo) {
        SystemPressureController.ProcessCompactStateListener processCompactStateListener = this.mProcessCompactStateListener;
        if (processCompactStateListener != null) {
            processCompactStateListener.onCompactProcess(processInfo.getProcessRecord());
        }
    }

    private boolean containInWhiteList(AppStateManager.AppState.RunningProcess proc) {
        boolean isProcessImportant;
        boolean isInWhite = this.mPackageWhiteList.contains(proc.getPackageName()) || this.mProcessPolicy.isInProcessStaticWhiteList(proc.getProcessName()) || proc.getUid() == UserHandle.getAppId(1002) || checkCtsProcess(proc.getProcessName()) || checkProtectProcess(proc.getProcessName());
        if (isInWhite) {
            return true;
        }
        synchronized (this.mAMS) {
            isProcessImportant = this.mProcessPolicy.isProcessImportant(proc.getProcessRecord());
        }
        return isProcessImportant;
    }

    private boolean checkProtectProcess(String processName) {
        return mProtectProcessList.contains(processName);
    }

    private boolean checkCtsProcess(String processName) {
        return processName.startsWith("com.android.cts.") || processName.startsWith("android.app.cts.") || processName.startsWith("com.android.server.cts.") || processName.startsWith("com.android.RemoteDPC") || processName.startsWith("android.camera.cts");
    }

    private List<AppStateManager.AppState.RunningProcess> getProcessGroup(String packageName, int uid) {
        ArrayList<AppStateManager.AppState.RunningProcess> procs = new ArrayList<>();
        synchronized (this.mAMS) {
            ProcessList procList = this.mAMS.mProcessList;
            int NP = procList.getProcessNamesLOSP().getMap().size();
            for (int ip = 0; ip < NP; ip++) {
                SparseArray<ProcessRecord> apps = (SparseArray) procList.getProcessNamesLOSP().getMap().valueAt(ip);
                int NA = apps.size();
                for (int ia = 0; ia < NA; ia++) {
                    ProcessRecord app = apps.valueAt(ia);
                    boolean isDep = app.getPkgDeps() != null && app.getPkgDeps().contains(packageName);
                    if (UserHandle.getAppId(app.uid) == UserHandle.getAppId(uid) && (app.getPkgList().containsKey(packageName) || isDep)) {
                        procs.add(this.mSmartPowerService.getRunningProcess(app.uid, app.processName));
                    }
                }
            }
        }
        return procs;
    }

    private boolean checkRunningProcess(AppStateManager.AppState.RunningProcess runningProc, int minAdj) {
        return runningProc != null && !checkProcessDied(runningProc.getProcessRecord()) && runningProc.getAdj() > minAdj;
    }

    public void registerCloudObserver(Context context) {
        ContentObserver observer = new ContentObserver(this.mHandler) { // from class: com.android.server.am.ProcessMemoryCleaner.2
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                if (uri.equals(Settings.System.getUriFor(ProcessMemoryCleaner.PERF_PROC_THRESHOLD_CLOUD))) {
                    ProcessMemoryCleaner.this.updateCloudControlData();
                }
            }
        };
        context.getContentResolver().registerContentObserver(Settings.System.getUriFor(PERF_PROC_THRESHOLD_CLOUD), false, observer, -2);
    }

    public void updateCloudControlData() {
        try {
            updateAppSwitchBgDelayTime();
            updateCommonUsedPssLimitKB();
            updateMemExceptionThresholdKB();
            updateProtectProcessList();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateAppSwitchBgDelayTime() {
        String SwitchBgTime = getCloudControlData(PROC_SWITCH_BG_DELAY_TIME_CLOUD);
        if (!TextUtils.isEmpty(SwitchBgTime)) {
            this.mAppSwitchBgExceptDelayTime = Integer.parseInt(SwitchBgTime);
            if (DEBUG) {
                Slog.d(TAG, "SwitchBgTime cloud control received: " + this.mAppSwitchBgExceptDelayTime);
            }
        }
    }

    private void updateCommonUsedPssLimitKB() {
        String pssLimitKB = getCloudControlData(PROC_COMM_PSS_LIMIT_CLOUD);
        if (!TextUtils.isEmpty(pssLimitKB)) {
            computeCommonUsedAppPssThreshold(Long.parseLong(pssLimitKB));
            if (DEBUG) {
                Slog.d(TAG, "pssLimitKB cloud control received: " + this.mCommonUsedPssLimitKB);
            }
        }
    }

    private void updateMemExceptionThresholdKB() {
        String memExceptionThresholdKB = getCloudControlData(PROC_MEM_EXCEPTION_THRESHOLD_CLOUD);
        if (!TextUtils.isEmpty(memExceptionThresholdKB)) {
            computeMemExceptionThreshold(Long.parseLong(memExceptionThresholdKB));
            if (DEBUG) {
                Slog.d(TAG, "memExceptionThresholdKB cloud control received: " + this.mMemExceptionThresholdKB);
            }
        }
    }

    private void updateProtectProcessList() {
        String processString = getCloudControlData(PROC_PROTECT_LIST_CLOUD);
        if (!TextUtils.isEmpty(processString)) {
            String[] processArray = processString.split(",");
            for (String processName : processArray) {
                if (!mProtectProcessList.contains(processName)) {
                    mProtectProcessList.add(processName);
                    if (DEBUG) {
                        Slog.d(TAG, "ProtectProcessList cloud control received: " + processName);
                    }
                }
            }
        }
    }

    private String getCloudControlData(String key) {
        return Settings.System.getStringForUser(this.mContext.getContentResolver(), key, -2);
    }

    public long killPackage(AppStateManager.AppState.RunningProcess runningProc, String reason) {
        return killPackage(runningProc, 200, reason);
    }

    public long killPackage(AppStateManager.AppState.RunningProcess proc, int minAdj, String reason) {
        if (proc.getProcessRecord() == null) {
            return 0L;
        }
        long appCurPss = 0;
        ArrayList<AppStateManager.AppState.RunningProcess> runningAppList = this.mSmartPowerService.getLruProcesses(proc.getUid(), proc.getPackageName());
        AppStateManager.AppState.RunningProcess mainProc = null;
        Iterator<AppStateManager.AppState.RunningProcess> it = runningAppList.iterator();
        while (it.hasNext()) {
            AppStateManager.AppState.RunningProcess runningProc = it.next();
            appCurPss += runningProc.getPss();
            if (!checkRunningProcess(runningProc, minAdj)) {
                return 0L;
            }
            if (runningProc.getPackageName().equals(runningProc.getProcessName())) {
                mainProc = runningProc;
            }
        }
        if (mainProc == null || !checkRunningProcess(mainProc, minAdj)) {
            return 0L;
        }
        forceStopPackage(mainProc.getPackageName(), mainProc.getUserId(), getKillReason(mainProc));
        this.mStatistics.reportEvent(2, mainProc, appCurPss, reason);
        return appCurPss;
    }

    private String getKillReason(AppStateManager.AppState.RunningProcess proc) {
        return "MiuiMemoryService(" + proc.getAdjType() + ")";
    }

    public long killProcess(AppStateManager.AppState.RunningProcess runningProc, String reason) {
        return killProcess(runningProc, 200, reason);
    }

    public long killProcess(AppStateManager.AppState.RunningProcess runningProc, int minAdj, String reason) {
        if (isCurrentProcessInBackup(runningProc)) {
            return 0L;
        }
        synchronized (this.mAMS) {
            ProcessRecord proc = runningProc.getProcessRecord();
            if (isInWhiteListLock(proc, runningProc.getUserId(), 21, this.mPMS) || !checkRunningProcess(runningProc, minAdj) || isRunningComponent(proc)) {
                return 0L;
            }
            killApplicationLock(proc, getKillReason(runningProc));
            this.mStatistics.reportEvent(1, runningProc, runningProc.getPss(), reason);
            return runningProc.getPss();
        }
    }

    private boolean isRunningComponent(ProcessRecord proc) {
        if (proc.mServices.numberOfExecutingServices() > 0 || proc.mReceivers.numberOfCurReceivers() > 0) {
            return true;
        }
        return false;
    }

    public void KillProcessForPadSmallWindowMode(String pkgName) {
        long nowTime = SystemClock.uptimeMillis();
        if (this.mHandler.hasMessages(3) || nowTime - this.mLastPadSmallWindowUpdateTime < PAD_SMALL_WINDOW_CLEAN_TIME) {
            return;
        }
        this.mLastPadSmallWindowUpdateTime = nowTime;
        Message msg = this.mHandler.obtainMessage(3);
        msg.obj = pkgName;
        this.mHandler.sendMessage(msg);
    }

    public void killProcessByMinAdj(int minAdj, String reason, List<String> whiteList) {
        if (minAdj <= 300) {
            minAdj = MiuiPocketModeSensorWrapper.STATE_STABLE_DELAY;
        }
        ArrayList<AppStateManager.AppState.RunningProcess> runningAppList = this.mSmartPowerService.getLruProcesses();
        List<AppStateManager.AppState.RunningProcess> canForcePkg = new ArrayList<>();
        Iterator<AppStateManager.AppState.RunningProcess> it = runningAppList.iterator();
        while (it.hasNext()) {
            AppStateManager.AppState.RunningProcess runningApp = it.next();
            if (!whiteList.contains(runningApp.getPackageName()) && checkRunningProcess(runningApp, minAdj)) {
                if (runningApp.getProcessName().equals(runningApp.getPackageName())) {
                    canForcePkg.add(runningApp);
                } else {
                    synchronized (this.mAMS) {
                        killApplicationLock(runningApp.getProcessRecord(), reason);
                    }
                }
            }
        }
        for (AppStateManager.AppState.RunningProcess runningProc : canForcePkg) {
            if (SystemPressureController.getInstance().isForceStopEnable(runningProc.getProcessRecord(), 0)) {
                forceStopPackage(runningProc.getPackageName(), runningProc.getUserId(), reason);
            } else {
                synchronized (this.mAMS) {
                    killApplicationLock(runningProc.getProcessRecord(), reason);
                }
            }
        }
    }

    public ProcMemCleanerStatistics getProcMemStat() {
        return this.mStatistics;
    }

    private void debugAppGroupToString(List<AppStateManager.AppState.RunningProcess> runningProcList) {
        for (AppStateManager.AppState.RunningProcess runningProc : runningProcList) {
            String deg = processToString(runningProc);
            Slog.d(TAG, "ProcessInfo: " + deg);
        }
    }

    private String processToString(AppStateManager.AppState.RunningProcess runningProc) {
        StringBuilder sb = new StringBuilder(128);
        sb.append("pck=");
        sb.append(runningProc.getPackageName());
        sb.append(", prcName=");
        sb.append(runningProc.getProcessName());
        sb.append(", priority=");
        sb.append(getProcPriority(runningProc));
        sb.append(", hasAct=");
        sb.append(runningProc.hasActivity());
        sb.append(", pss=");
        sb.append(runningProc.getPss());
        return sb.toString();
    }

    private boolean isLaunchCameraForThirdApp(SystemPressureController.ControllerActivityInfo info) {
        if (info.fromPkg != null && info.launchPkg.equals("com.android.camera") && !isSystemApp(info.formUid, info.fromPkg)) {
            return true;
        }
        return false;
    }

    private boolean isSystemApp(int uid, String pkgName) {
        ArrayList<AppStateManager.AppState.RunningProcess> runningList = this.mSmartPowerService.getLruProcesses(uid, pkgName);
        if (runningList.size() > 0) {
            return AppStateManager.isSystemApp(runningList.get(0).getProcessRecord());
        }
        return false;
    }

    public void foregroundActivityChanged(SystemPressureController.ControllerActivityInfo info) {
        if (this.mHandler == null) {
            return;
        }
        sendAppSwitchBgExceptionMsg(info);
        sendGlobalCompactMsg(info);
        this.mForegroundPkg = info.launchPkg;
        this.mForegroundUid = info.launchUid;
    }

    public void compactBackgroundProcess(int uid, String processName) {
        AppStateManager.AppState.RunningProcess proc = this.mSmartPowerService.getRunningProcess(uid, processName);
        compactBackgroundProcess(proc, false);
    }

    public void compactBackgroundProcess(AppStateManager.AppState.RunningProcess proc, boolean isDelayed) {
        if (proc == null || !proc.hasActivity()) {
            return;
        }
        if (!isDelayed) {
            Message msg = this.mHandler.obtainMessage(4, proc);
            msg.obj = proc;
            this.mHandler.sendMessage(msg);
        } else if (!this.mHandler.hasMessages(4, proc)) {
            Message msg2 = this.mHandler.obtainMessage(4, proc);
            msg2.obj = proc;
            this.mHandler.sendMessageDelayed(msg2, 4000L);
        }
    }

    public void checkBackgroundProcCompact(AppStateManager.AppState.RunningProcess proc) {
        if (proc != null && proc.hasActivity() && proc.getCurrentState() >= 4 && checkRunningProcess(proc, 100)) {
            Slog.d(TAG, "Compact memory: " + proc + " pss:" + proc.getPss() + " swap:" + proc.getSwapPss());
            compactProcess(proc);
        }
    }

    private void sendAppSwitchBgExceptionMsg(SystemPressureController.ControllerActivityInfo info) {
        if (this.mHandler.hasEqualMessages(1, info.launchPkg)) {
            this.mHandler.removeMessages(1, info.launchPkg);
        }
        if (!this.mPackageWhiteList.contains(this.mForegroundPkg) && !isLaunchCameraForThirdApp(info) && !SystemPressureController.getInstance().isGameApp(this.mForegroundPkg)) {
            Message msg = this.mHandler.obtainMessage(1, this.mForegroundPkg);
            msg.obj = this.mForegroundPkg;
            msg.arg1 = this.mForegroundUid;
            this.mHandler.sendMessageDelayed(msg, this.mAppSwitchBgExceptDelayTime);
        }
    }

    private void sendGlobalCompactMsg(SystemPressureController.ControllerActivityInfo info) {
        MiuiMemoryServiceInternal miuiMemoryServiceInternal;
        if (info.launchPkg.equals("com.miui.home") && (miuiMemoryServiceInternal = this.mMiuiMemoryService) != null) {
            miuiMemoryServiceInternal.runGlobalCompaction(1);
        }
    }

    private void computeMemExceptionThreshold(long threshold) {
        long j = TOTAL_MEMEORY_GB;
        if (j < 5) {
            this.mMemExceptionThresholdKB = (long) (threshold * MEM_EXCEPTION_TH_LITE_FACTOR);
        } else if (j <= 6) {
            this.mMemExceptionThresholdKB = (long) (threshold * MEM_EXCEPTION_TH_MID_FACTOR);
        } else if (j <= 8) {
            this.mMemExceptionThresholdKB = (long) (threshold * 1.0d);
        } else {
            this.mMemExceptionThresholdKB = (long) (threshold * MEM_EXCEPTION_TH_HIGH_FACTOR);
        }
    }

    private void computeCommonUsedAppPssThreshold(long threshold) {
        long j = TOTAL_MEMEORY_GB;
        if (j <= 6) {
            this.mCommonUsedPssLimitKB = (long) (threshold * COMM_USED_PSS_LIMIT_LITE_FACTOR);
        } else if (j <= 8) {
            this.mCommonUsedPssLimitKB = (long) (threshold * 1.0d);
        } else {
            this.mCommonUsedPssLimitKB = (long) (threshold * COMM_USED_PSS_LIMIT_HIGH_FACTOR);
        }
    }

    public int checkBackgroundAppException(String packageName, int uid) {
        int killType = 0;
        if (SystemPressureController.getInstance().isGameApp(packageName)) {
            return 0;
        }
        long mainPss = 0;
        long subPss = 0;
        long subPssWithActivity = 0;
        AppStateManager.AppState.RunningProcess mainProc = null;
        List<AppStateManager.AppState.RunningProcess> procGroup = getProcessGroup(packageName, uid);
        for (AppStateManager.AppState.RunningProcess proc : procGroup) {
            if (proc.getPackageName().equals(proc.getProcessName())) {
                mainProc = proc;
                mainPss = proc.getPss();
            } else if (proc.hasActivity()) {
                subPssWithActivity += proc.getPss();
            } else {
                subPss += proc.getPss();
            }
        }
        if (mainProc == null) {
            return 0;
        }
        if (ProcMemCleanerStatistics.isCommonUsedApp(mainProc.getPackageName())) {
            if (mainPss >= this.mCommonUsedPssLimitKB) {
                killPackage(mainProc, ProcMemCleanerStatistics.REASON_KILL_BG_PROC);
                killType = 2;
                if (DEBUG) {
                    Slog.d(TAG, "common used app takes up too much memory: " + mainProc.getPackageName());
                }
            }
            return killType;
        }
        long j = mainPss + subPss + subPssWithActivity;
        long subPssWithActivity2 = this.mMemExceptionThresholdKB;
        if (j > subPssWithActivity2) {
            killPackage(this.mSmartPowerService.getRunningProcess(mainProc.getUid(), mainProc.getProcessName()), ProcMemCleanerStatistics.REASON_KILL_BG_PROC);
            killType = 2;
            if (DEBUG) {
                Slog.d(TAG, "app takes up too much memory: " + mainProc.getPackageName() + ". pss:" + mainPss + " sub:" + subPss);
            }
        } else if (subPss >= SmartPowerSettings.PROC_MEM_LVL1_PSS_LIMIT_KB) {
            for (AppStateManager.AppState.RunningProcess proc2 : procGroup) {
                if (!proc2.getPackageName().equals(proc2.getProcessName()) && !proc2.hasActivity()) {
                    killProcess(this.mSmartPowerService.getRunningProcess(proc2.getUid(), proc2.getProcessName()), ProcMemCleanerStatistics.REASON_KILL_BG_PROC);
                }
            }
            killType = 1;
            if (DEBUG) {
                Slog.d(TAG, "subprocess takes up too much memory: " + mainProc.getPackageName() + ". sub:" + subPss);
            }
        }
        return killType;
    }

    /* loaded from: classes.dex */
    public class H extends Handler {
        private List<String> mPadSmallWindowWhiteList = new ArrayList();

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public H(Looper looper) {
            super(looper);
            ProcessMemoryCleaner.this = r1;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    try {
                        ProcessMemoryCleaner.this.checkBackgroundAppException((String) msg.obj, msg.arg1);
                        return;
                    } catch (Exception e) {
                        Slog.d(ProcessMemoryCleaner.TAG, "checkBackgroundAppException failed: " + e.toString());
                        return;
                    }
                case 2:
                    ProcessMemoryCleaner processMemoryCleaner = ProcessMemoryCleaner.this;
                    processMemoryCleaner.registerCloudObserver(processMemoryCleaner.mContext);
                    ProcessMemoryCleaner.this.updateCloudControlData();
                    return;
                case 3:
                    this.mPadSmallWindowWhiteList.add((String) msg.obj);
                    ProcessMemoryCleaner.this.killProcessByMinAdj(799, ProcessMemoryCleaner.REASON_PAD_SMALL_WINDOW_CLEAN, this.mPadSmallWindowWhiteList);
                    this.mPadSmallWindowWhiteList.clear();
                    return;
                case 4:
                    try {
                        ProcessMemoryCleaner.this.checkBackgroundProcCompact((AppStateManager.AppState.RunningProcess) msg.obj);
                        return;
                    } catch (Exception e2) {
                        Slog.d(ProcessMemoryCleaner.TAG, "checkBackgroundProcCompact failed: " + e2.toString());
                        return;
                    }
                default:
                    return;
            }
        }
    }
}
