package com.android.server.am;

import android.app.AppGlobals;
import android.app.ApplicationErrorReport;
import android.app.IApplicationThread;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Debug;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.server.am.SplitScreenReporter;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.os.BackgroundThread;
import com.android.internal.os.ProcessCpuTracker;
import com.android.server.LocalServices;
import com.android.server.ScoutHelper;
import com.miui.base.MiuiStubRegistry;
import com.miui.server.process.ProcessManagerInternal;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import miui.mqsas.scout.ScoutUtils;
import miui.mqsas.sdk.MQSEventManagerDelegate;
import miui.mqsas.sdk.event.AnrEvent;
import miui.mqsas.sdk.event.LowMemEvent;
import miui.os.DeviceFeature;
import org.json.JSONException;
import org.json.JSONObject;
/* loaded from: classes.dex */
public class ProcessRecordImpl implements ProcessRecordStub {
    private static final int MAX_LOW_MEM_TIME = 1200000;
    private static final long MEM_THRESHOLD_IN_WHITE_LIST = 71680;
    public static final String POLICY_CHANGED_PKG = "pkg changed";
    public static final String POLICY_CLEAR_DATA = "clearApplicationUserData";
    public static final String POLICY_DELETE_PACKAGE = "deletePackageX";
    public static final String POLICY_FINISH_USER = "finish user";
    public static final String POLICY_INSTALL_PACKAGE = "installPackageLI";
    public static final String POLICY_START_INSTR = "start instr";
    public static final String POLICY_UNINSTALL_PKG = "pkg removed";
    private static final String TAG = "ProcessRecordInjector";
    private static final List<String> sPolicyWhiteList;
    private static boolean sSystemBootCompleted;
    private static final Object sLock = new Object();
    private static volatile ProcessManagerInternal sProcessManagerInternal = null;
    private static final String DEVICE = SystemProperties.get("ro.product.device", "UNKNOWN");
    private static long sLastLowMemTime = 0;
    private static final SparseArray<Map<String, AppPss>> sAppPssUserMap = new SparseArray<>();
    private static final boolean ENABLE_LENIENT_CACHE = SystemProperties.getBoolean("persist.am.enable_lenient_cache", false);

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<ProcessRecordImpl> {

        /* compiled from: ProcessRecordImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final ProcessRecordImpl INSTANCE = new ProcessRecordImpl();
        }

        public ProcessRecordImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public ProcessRecordImpl provideNewInstance() {
            return new ProcessRecordImpl();
        }
    }

    ProcessRecordImpl() {
    }

    static {
        ArrayList arrayList = new ArrayList();
        sPolicyWhiteList = arrayList;
        arrayList.add(ProcessPolicy.REASON_ONE_KEY_CLEAN);
        arrayList.add(POLICY_UNINSTALL_PKG);
        arrayList.add(POLICY_DELETE_PACKAGE);
        arrayList.add(POLICY_INSTALL_PACKAGE);
        arrayList.add(POLICY_FINISH_USER);
        arrayList.add(POLICY_START_INSTR);
        arrayList.add(POLICY_CLEAR_DATA);
        arrayList.add(POLICY_CHANGED_PKG);
        arrayList.add(ProcessPolicy.REASON_ONE_KEY_CLEAN);
        arrayList.add(ProcessPolicy.REASON_FORCE_CLEAN);
        arrayList.add(ProcessPolicy.REASON_GARBAGE_CLEAN);
        arrayList.add(ProcessPolicy.REASON_GAME_CLEAN);
        arrayList.add(ProcessPolicy.REASON_SWIPE_UP_CLEAN);
        arrayList.add(ProcessPolicy.REASON_USER_DEFINED);
    }

    private static ProcessManagerInternal getProcessManagerInternal() {
        if (sProcessManagerInternal == null) {
            synchronized (ProcessRecordImpl.class) {
                if (sProcessManagerInternal == null) {
                    sProcessManagerInternal = (ProcessManagerInternal) LocalServices.getService(ProcessManagerInternal.class);
                }
            }
        }
        return sProcessManagerInternal;
    }

    public static void reportLowMemIfNeeded(final ActivityManagerService service, final ProcessRecord dyingProc) {
        long now = SystemClock.uptimeMillis();
        if (now > sLastLowMemTime + 1200000) {
            BackgroundThread.getHandler().post(new Runnable() { // from class: com.android.server.am.ProcessRecordImpl.1
                @Override // java.lang.Runnable
                public void run() {
                    ProcessRecordImpl.reportLowMemEvent(service, dyingProc);
                }
            });
            sLastLowMemTime = now;
        }
    }

    public static void reportLowMemEvent(ActivityManagerService service, ProcessRecord dyingProc) {
        Throwable th;
        IApplicationThread thread;
        int oomAdj;
        int pid;
        SparseArray<LowMemEvent.ProcessMemItem> procMemsMap = new SparseArray<>();
        LowMemEvent event = new LowMemEvent();
        synchronized (service) {
            try {
                if (service != null) {
                    try {
                        if (service.mProcessList.getLruSizeLOSP() != 0) {
                            List<ProcessRecord> procs = new ArrayList<>(service.mProcessList.getLruProcessesLOSP());
                            Debug.MemoryInfo mi = null;
                            for (int i = procs.size() - 1; i >= 0; i--) {
                                ProcessRecord r = procs.get(i);
                                synchronized (service) {
                                    try {
                                        thread = r.getThread();
                                        oomAdj = r.mState.getSetAdjWithServices();
                                        pid = r.getPid();
                                    } catch (Throwable th2) {
                                        th = th2;
                                        while (true) {
                                            try {
                                                break;
                                            } catch (Throwable th3) {
                                                th = th3;
                                            }
                                        }
                                        throw th;
                                    }
                                }
                                if (thread != null && dyingProc != r) {
                                    if (mi == null) {
                                        mi = new Debug.MemoryInfo();
                                    }
                                    Debug.getMemoryInfo(pid, mi);
                                    long myTotalPss = mi.getTotalPss();
                                    long myTotalSwapPss = mi.getTotalSwappedOutPss();
                                    LowMemEvent.ProcessMemItem item = new LowMemEvent.ProcessMemItem(myTotalPss, myTotalSwapPss, r.processName);
                                    procMemsMap.put(pid, item);
                                    if (oomAdj >= -1000 && oomAdj < -900) {
                                        event.nativeMemOom.items.add(item);
                                        event.nativeMemOom.totalPss += myTotalPss;
                                        event.nativeMemOom.totalSwapPss += myTotalSwapPss;
                                    } else if (oomAdj < -900 || oomAdj >= -800) {
                                        if (oomAdj >= -800 && oomAdj < -700) {
                                            event.persistentMemOom.items.add(item);
                                            event.persistentMemOom.totalPss += myTotalPss;
                                            event.persistentMemOom.totalSwapPss += myTotalSwapPss;
                                        } else if (oomAdj >= -700 && oomAdj < 0) {
                                            event.persistentServiceMemOom.items.add(item);
                                            event.persistentServiceMemOom.totalPss += myTotalPss;
                                            event.persistentServiceMemOom.totalSwapPss += myTotalSwapPss;
                                        } else if (oomAdj < 0 || oomAdj >= 100) {
                                            if (oomAdj < 100 || oomAdj >= 200) {
                                                if (oomAdj < 200 || oomAdj >= 250) {
                                                    if (oomAdj < 250 || oomAdj >= 300) {
                                                        if (oomAdj < 300 || oomAdj >= 400) {
                                                            if (oomAdj < 400 || oomAdj >= 500) {
                                                                if (oomAdj < 500 || oomAdj >= 600) {
                                                                    if (oomAdj < 600 || oomAdj >= 700) {
                                                                        if (oomAdj < 700 || oomAdj >= 800) {
                                                                            if (oomAdj >= 800 && oomAdj < 900) {
                                                                                event.bServicesMemOom.items.add(item);
                                                                                event.bServicesMemOom.totalPss += myTotalPss;
                                                                                event.bServicesMemOom.totalSwapPss += myTotalSwapPss;
                                                                            } else if (oomAdj >= 900) {
                                                                                event.cachedMemOom.items.add(item);
                                                                                event.cachedMemOom.totalPss += myTotalPss;
                                                                                event.cachedMemOom.totalSwapPss += myTotalSwapPss;
                                                                            }
                                                                        } else {
                                                                            event.previousMemOom.items.add(item);
                                                                            event.previousMemOom.totalPss += myTotalPss;
                                                                            event.previousMemOom.totalSwapPss += myTotalSwapPss;
                                                                        }
                                                                    } else {
                                                                        event.homeMemOom.items.add(item);
                                                                        event.homeMemOom.totalPss += myTotalPss;
                                                                        event.homeMemOom.totalSwapPss += myTotalSwapPss;
                                                                    }
                                                                } else {
                                                                    event.aServicesMemOom.items.add(item);
                                                                    event.aServicesMemOom.totalPss += myTotalPss;
                                                                    event.aServicesMemOom.totalSwapPss += myTotalSwapPss;
                                                                }
                                                            } else {
                                                                event.heavyWeightMemOom.items.add(item);
                                                                event.heavyWeightMemOom.totalPss += myTotalPss;
                                                                event.heavyWeightMemOom.totalSwapPss += myTotalSwapPss;
                                                            }
                                                        } else {
                                                            event.backupMemOom.items.add(item);
                                                            event.backupMemOom.totalPss += myTotalPss;
                                                            event.backupMemOom.totalSwapPss += myTotalSwapPss;
                                                        }
                                                    } else {
                                                        event.perceptibleLowMemOom.items.add(item);
                                                        event.perceptibleLowMemOom.totalPss += myTotalPss;
                                                        event.perceptibleLowMemOom.totalSwapPss += myTotalSwapPss;
                                                    }
                                                } else {
                                                    event.perceptibleMemOom.items.add(item);
                                                    event.perceptibleMemOom.totalPss += myTotalPss;
                                                    event.perceptibleMemOom.totalSwapPss += myTotalSwapPss;
                                                }
                                            } else {
                                                event.visibleMemOom.items.add(item);
                                                event.visibleMemOom.totalPss += myTotalPss;
                                                event.visibleMemOom.totalSwapPss += myTotalSwapPss;
                                            }
                                        } else {
                                            event.foregroundMemOom.items.add(item);
                                            event.foregroundMemOom.totalPss += myTotalPss;
                                            event.foregroundMemOom.totalSwapPss += myTotalSwapPss;
                                        }
                                    } else {
                                        event.systemMemOom.items.add(item);
                                        event.systemMemOom.totalPss += myTotalPss;
                                        event.systemMemOom.totalSwapPss += myTotalSwapPss;
                                    }
                                }
                            }
                            service.updateCpuStatsNow();
                            Debug.MemoryInfo mi2 = null;
                            synchronized (service.getProcessCpuTrackerLocked()) {
                                int N = service.getProcessCpuTrackerLocked().countStats();
                                for (int i2 = 0; i2 < N; i2++) {
                                    ProcessCpuTracker.Stats st = service.getProcessCpuTrackerLocked().getStats(i2);
                                    if (st.vsize > 0 && procMemsMap.indexOfKey(st.pid) < 0) {
                                        if (mi2 == null) {
                                            mi2 = new Debug.MemoryInfo();
                                        }
                                        Debug.getMemoryInfo(st.pid, mi2);
                                        long myTotalPss2 = mi2.getTotalPss();
                                        long myTotalSwapPss2 = mi2.getTotalSwappedOutPss();
                                        event.nativeMemOom.items.add(new LowMemEvent.ProcessMemItem(myTotalPss2, myTotalSwapPss2, st.name));
                                        event.nativeMemOom.totalPss += myTotalPss2;
                                        event.nativeMemOom.totalSwapPss += myTotalSwapPss2;
                                    }
                                }
                            }
                            event.sortAndSub();
                            MQSEventManagerDelegate.getInstance().reportLowMemEvent(event);
                        }
                    } catch (Throwable th4) {
                        th = th4;
                        throw th;
                    }
                }
            } catch (Throwable th5) {
                th = th5;
            }
        }
    }

    public static boolean isSystemBootCompleted() {
        if (!sSystemBootCompleted) {
            sSystemBootCompleted = SplitScreenReporter.ACTION_ENTER_SPLIT.equals(SystemProperties.get("sys.boot_completed"));
        }
        return sSystemBootCompleted;
    }

    public void updateProcessForegroundLocked(ProcessRecord app) {
        getProcessManagerInternal().updateProcessForegroundLocked(app.getPid());
    }

    public static boolean isLowMemoryDevice() {
        return ((int) (Process.getTotalMemory() / 1073741824)) < 3;
    }

    public static boolean isLowestMemoryDevice() {
        return ((int) (Process.getTotalMemory() / 1073741824)) < 2;
    }

    public static boolean shouldKillHighFrequencyApp(ProcessList processList, ProcessRecord app) {
        boolean z = true;
        if (!ENABLE_LENIENT_CACHE || !getProcessManagerInternal().isPackageFastBootEnable(app.info.packageName, app.uid, false)) {
            return app.mProfile.getLastCachedPss() >= processList.getCachedRestoreThresholdKb();
        }
        if (app.mProfile.getLastCachedPss() < processList.getCachedRestoreThresholdKb() * 1.5d) {
            z = false;
        }
        boolean kill = z;
        if (!kill) {
            Slog.d(TAG, "delay Kill high frequency app ：" + app.info.packageName);
        }
        return kill;
    }

    public static void addAppPssIfNeeded(ProcessManagerService pms, ProcessRecord app) {
        PackageInfo pi;
        String pkn = app.info.packageName;
        if ("android".equals(pkn)) {
            return;
        }
        long pss = ProcessUtils.getPackageLastPss(pkn, app.userId);
        synchronized (sLock) {
            Map<String, AppPss> appPssMap = sAppPssUserMap.get(app.userId);
            if (pss >= MEM_THRESHOLD_IN_WHITE_LIST && (appPssMap == null || appPssMap.get(pkn) == null)) {
                try {
                    pi = AppGlobals.getPackageManager().getPackageInfo(pkn, 0L, app.userId);
                } catch (RemoteException e) {
                    e.printStackTrace();
                    pi = null;
                }
                String version = (pi == null || pi.versionName == null) ? "unknown" : pi.versionName;
                AppPss appPss = new AppPss(pkn, pss, version, app.userId);
                if (appPssMap == null) {
                    appPssMap = new HashMap<>();
                    sAppPssUserMap.put(app.userId, appPssMap);
                }
                appPssMap.put(pkn, appPss);
            }
        }
    }

    public static void reportAppPss() {
        final Map<String, AppPss> map = new HashMap<>();
        synchronized (sLock) {
            SparseArray<Map<String, AppPss>> sparseArray = sAppPssUserMap;
            if (sparseArray.size() > 0) {
                int size = sparseArray.size();
                for (int i = 0; i < size; i++) {
                    Map<? extends String, ? extends AppPss> valueAt = sAppPssUserMap.valueAt(i);
                    if (valueAt != null) {
                        map.putAll(valueAt);
                    }
                }
                sAppPssUserMap.clear();
            }
        }
        if (map.size() == 0) {
            return;
        }
        BackgroundThread.getHandler().post(new Runnable() { // from class: com.android.server.am.ProcessRecordImpl.2
            @Override // java.lang.Runnable
            public void run() {
                List<String> jsons = new ArrayList<>();
                Set<String> pkns = map.keySet();
                for (String pkn : pkns) {
                    AppPss appPss = (AppPss) map.get(pkn);
                    if (appPss != null) {
                        JSONObject object = new JSONObject();
                        try {
                            object.put("packageName", appPss.pkn);
                            object.put("totalPss", appPss.pss);
                            object.put("versionName", appPss.version);
                            object.put("model", Build.MODEL);
                            object.put("userId", appPss.user);
                            jsons.add(object.toString());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
                if (jsons.size() > 0) {
                    MQSEventManagerDelegate.getInstance().reportEventsV2("appPss", jsons, "mqs_whiteapp_lowmem_monitor_63691000", false);
                }
            }
        });
    }

    /* loaded from: classes.dex */
    public static final class AppPss {
        static final String MODEL = "model";
        static final String PACKAGE_NAME = "packageName";
        static final String TOTAL_PSS = "totalPss";
        static final String USER_ID = "userId";
        static final String VERSION_NAME = "versionName";
        String pkn;
        String pss;
        String user;
        String version;

        AppPss(String pkn, long pss, String version, int user) {
            this.pkn = pkn;
            this.pss = String.valueOf(pss);
            this.version = version;
            this.user = String.valueOf(user);
        }
    }

    public void onANR(ActivityManagerService ams, ProcessRecord process, String activityShortComponentName, String parentShortCompontnName, String subject, String report, File logFile, ApplicationErrorReport.CrashInfo crashInfo, String headline, ScoutAnrInfo anrInfo) {
        if (anrInfo != null) {
            anrInfo.setBinderTransInfo(anrInfo.getBinderTransInfo());
            reportANR(ams, ProcessPolicy.REASON_ANR, process, process.processName, activityShortComponentName, parentShortCompontnName, subject, report, logFile, crashInfo, headline, anrInfo);
        }
        ANRManager.onANR(ams, process, subject, report, logFile, crashInfo, headline);
    }

    private void reportANR(ActivityManagerService ams, String eventType, ProcessRecord process, String processName, String activityShortComponentName, String parentShortCompontnName, String subject, String report, File logFile, ApplicationErrorReport.CrashInfo crashInfo, String headline, ScoutAnrInfo anrInfo) {
        AnrEvent event = new AnrEvent();
        event.setPid(anrInfo.getpid());
        event.setUid(anrInfo.getuid());
        event.setProcessName(process.mPid == ActivityManagerService.MY_PID ? "system_server" : processName);
        event.setPackageName((!"system".equals(processName) || process.mPid == ActivityManagerService.MY_PID) ? event.getProcessName() : process.info.packageName);
        event.setTimeStamp(anrInfo.getTimeStamp());
        event.setReason(report);
        event.setCpuInfo(subject);
        event.setBgAnr(anrInfo.getBgAnr());
        event.setBlockSystemState(anrInfo.getBlockSystemState());
        event.setBinderTransactionInfo(anrInfo.getBinderTransInfo());
        if (logFile != null && logFile.exists()) {
            event.setLogName(logFile.getAbsolutePath());
            ANRManager.saveLastAnrState(ams, logFile);
        }
        if (activityShortComponentName != null) {
            event.setTargetActivity(activityShortComponentName);
        }
        if (parentShortCompontnName != null) {
            event.setParent(parentShortCompontnName);
        }
        MQSEventManagerDelegate.getInstance().reportAnrEvent(event);
    }

    public boolean skipAppErrorDialog(ProcessRecord app) {
        return (app == null || app.info == null || app.getPid() == Process.myPid() || ((!DeviceFeature.IS_SUBSCREEN_DEVICE || !"com.xiaomi.misubscreenui".equals(app.info.packageName)) && !ScoutUtils.isLibraryTest())) ? false : true;
    }

    public void scoutAppUpdateAnrInfo(String tag, ProcessRecord process, ScoutAnrInfo anrInfo) {
        anrInfo.setPid(process.getPid());
        anrInfo.setuid(process.getStartUid());
        anrInfo.setTimeStamp(System.currentTimeMillis());
    }

    public void scoutAppCheckBinderCallChain(String tag, int Pid, int systemPid, String annotation, ArrayList<Integer> firstPids, ArrayList<Integer> nativePids, ScoutAnrInfo anrInfo) {
        ArrayList<Integer> scoutJavaPids = new ArrayList<>(5);
        ArrayList<Integer> scoutNativePids = new ArrayList<>(5);
        boolean z = true;
        ScoutHelper.ScoutBinderInfo scoutBinderInfo = new ScoutHelper.ScoutBinderInfo(Pid, systemPid, 1, "MIUIScout ANR");
        scoutJavaPids.add(Integer.valueOf(Pid));
        boolean isBlockSystem = ScoutHelper.checkBinderCallPidList(Pid, scoutBinderInfo, scoutJavaPids, scoutNativePids);
        if (!scoutJavaPids.contains(Integer.valueOf(systemPid)) && annotation.contains("Broadcast")) {
            if (!ScoutHelper.checkAsyncBinderCallPidList(Pid, systemPid, scoutBinderInfo, scoutJavaPids, scoutNativePids) && !isBlockSystem) {
                z = false;
            }
            isBlockSystem = z;
        }
        ScoutHelper.printfProcBinderInfo(Pid, "MIUIScout ANR");
        ScoutHelper.printfProcBinderInfo(systemPid, "MIUIScout ANR");
        anrInfo.setBinderTransInfo(scoutBinderInfo.getBinderTransInfo());
        anrInfo.setDThreadState(scoutBinderInfo.getDThreadState());
        if (scoutJavaPids.size() > 0) {
            Iterator<Integer> it = scoutJavaPids.iterator();
            while (it.hasNext()) {
                int javaPid = it.next().intValue();
                if (!firstPids.contains(Integer.valueOf(javaPid))) {
                    firstPids.add(Integer.valueOf(javaPid));
                    Slog.d(tag, "Dump Trace: add java proc " + javaPid);
                }
            }
        }
        if (scoutNativePids.size() > 0) {
            Iterator<Integer> it2 = scoutNativePids.iterator();
            while (it2.hasNext()) {
                int nativePid = it2.next().intValue();
                if (!nativePids.contains(Integer.valueOf(nativePid))) {
                    nativePids.add(Integer.valueOf(nativePid));
                    Slog.d(tag, "Dump Trace: add java proc " + nativePid);
                }
            }
        }
        anrInfo.setBlockSystemState(isBlockSystem);
    }

    public void scoutAppCheckDumpKernelTrace(String tag, ScoutAnrInfo anrInfo) {
        if (ScoutHelper.SYSRQ_ANR_D_THREAD && anrInfo.getDThreadState()) {
            ScoutHelper.doSysRqInterface('w');
            ScoutHelper.doSysRqInterface('l');
            if (ScoutHelper.PANIC_ANR_D_THREAD && ScoutHelper.isDebugpolicyed(tag)) {
                SystemClock.sleep(ActivityManagerServiceImpl.BOOST_DURATION);
                Slog.e(tag, "Trigge Panic Crash when anr Process has D state thread");
                ScoutHelper.doSysRqInterface('c');
            }
        }
    }

    public void dumpPeriodHistoryMessage(ProcessRecord process, long anrTime, int duration, boolean isSystemInputAnr) {
        try {
            if (process.getThread() == null) {
                Slog.w(TAG, "Can't dumpPeriodHistoryMessage because of null IApplicationThread");
            } else if (!isSystemInputAnr) {
                process.getThread().dumpPeriodHistoryMessage(anrTime, duration);
            } else {
                ScoutHelper.dumpUithreadPeriodHistoryMessage(anrTime, duration);
            }
        } catch (RemoteException e) {
            Slog.w(TAG, "Failed to dumpPeriodHistoryMessage after ANR", e);
        }
    }
}
