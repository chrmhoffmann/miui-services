package com.android.server.am;

import android.app.ActivityThread;
import android.app.IWallpaperManager;
import android.app.WallpaperInfo;
import android.content.Context;
import android.os.Binder;
import android.os.Bundle;
import android.os.Debug;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.speech.tts.TtsEngines;
import android.telecom.TelecomManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;
import android.view.accessibility.AccessibilityManager;
import com.android.internal.os.ProcessCpuTracker;
import com.android.server.LocalServices;
import com.android.server.ScoutHelper;
import com.android.server.wm.ActivityTaskManagerInternal;
import com.android.server.wm.WindowProcessController;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import miui.util.ReflectionUtils;
/* loaded from: classes.dex */
public class ProcessUtils {
    public static final int FREEFORM_WORKSPACE_STACK_ID = 2;
    public static final int FULLSCREEN_WORKSPACE_STACK_ID = 1;
    private static final int LOW_MEMORY_RATE = 20;
    private static final String TAG = "ProcessUtils";
    private static TtsEngines sTtsEngines;
    public static final Pair<Integer, Integer> PRIORITY_VISIBLE = new Pair<>(100, 2);
    public static final Pair<Integer, Integer> PRIORITY_PERCEPTIBLE = new Pair<>(Integer.valueOf((int) ScoutHelper.BINDER_FULL_KILL_SCORE_ADJ_MIN), 4);
    public static final Pair<Integer, Integer> PRIORITY_HEAVY = new Pair<>(400, 13);
    public static final Pair<Integer, Integer> PRIORITY_UNKNOW = new Pair<>(1001, 20);
    private static ActivityManagerService sAmInstance = null;

    public static boolean isPhoneWorking() {
        TelecomManager tm;
        boolean isWorking = true;
        boolean mWifiOnly = SystemProperties.getBoolean("ro.radio.noril", false);
        if (mWifiOnly) {
            return false;
        }
        long token = Binder.clearCallingIdentity();
        try {
            if (ActivityThread.currentApplication() != null && (tm = (TelecomManager) ActivityThread.currentApplication().getSystemService("telecom")) != null) {
                isWorking = tm.isInCall();
            }
            return isWorking;
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public static String getDefaultInputMethod(Context context) {
        int endIndex;
        String inputMethodId = Settings.Secure.getString(context.getContentResolver(), "default_input_method");
        if (TextUtils.isEmpty(inputMethodId) || (endIndex = inputMethodId.indexOf(47)) <= 0) {
            return null;
        }
        String inputMethodPkg = inputMethodId.substring(0, endIndex);
        return inputMethodPkg;
    }

    public static String getActiveWallpaperPackage(Context context) {
        WallpaperInfo wInfo = null;
        IWallpaperManager wpm = IWallpaperManager.Stub.asInterface(ServiceManager.getService("wallpaper"));
        try {
            wInfo = wpm.getWallpaperInfo(-2);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        if (wInfo == null) {
            return null;
        }
        String wallpaperPkg = wInfo.getPackageName();
        return wallpaperPkg;
    }

    public static String getActiveTtsEngine(Context context) {
        AccessibilityManager accessibilityManager = (AccessibilityManager) context.getSystemService("accessibility");
        if (accessibilityManager == null || !accessibilityManager.isEnabled() || !accessibilityManager.isTouchExplorationEnabled()) {
            return null;
        }
        if (sTtsEngines == null) {
            sTtsEngines = new TtsEngines(context);
        }
        String ttsEngine = sTtsEngines.getDefaultEngine();
        return ttsEngine;
    }

    public static boolean isLowMemory() {
        long freeMemory = Process.getFreeMemory();
        long totalMemory = Process.getTotalMemory();
        return 20 * freeMemory < totalMemory;
    }

    public static boolean isHomeProcess(ProcessRecord pr) {
        return pr != null && pr.getWindowProcessController().isHomeProcess();
    }

    public static List<ProcessRecord> getProcessListByAdj(ActivityManagerService ams, int minOomAdj, List<String> whiteList) {
        List<ProcessRecord> procs = new ArrayList<>();
        synchronized (ams) {
            int NP = ams.getProcessNamesLOSP().size();
            for (int ip = 0; ip < NP; ip++) {
                SparseArray<ProcessRecord> apps = (SparseArray) ams.getProcessNamesLOSP().getMap().valueAt(ip);
                int NA = apps.size();
                for (int ia = 0; ia < NA; ia++) {
                    ProcessRecord app = apps.valueAt(ia);
                    if (!app.isPersistent() && (whiteList == null || !whiteList.contains(app.processName))) {
                        if (app.isRemoved()) {
                            procs.add(app);
                        } else if (app.mState.getSetAdj() >= minOomAdj) {
                            procs.add(app);
                        }
                    }
                }
            }
        }
        return procs;
    }

    public static int getProcTotalPss(int pid) {
        Debug.MemoryInfo info = new Debug.MemoryInfo();
        Debug.getMemoryInfo(pid, info);
        return info.getTotalPss();
    }

    public static int getTotalPss(int[] pids) {
        if (pids != null && pids.length > 0) {
            int totalPss = 0;
            for (int i = 0; i < pids.length && pids[i] != 0; i++) {
                totalPss += getProcTotalPss(pids[i]);
            }
            return totalPss;
        }
        return 0;
    }

    public static long getPackageLastPss(String packageName, int userId) {
        ActivityManagerService ams = getActivityManagerService();
        long totalPss = 0;
        synchronized (ams) {
            List<ProcessRecord> lruProcesses = ams.mProcessList.getLruProcessesLOSP();
            for (int i = lruProcesses.size() - 1; i >= 0; i--) {
                ProcessRecord proc = lruProcesses.get(i);
                if (proc != null && proc.userId == userId && proc.getThread() != null && !proc.isKilledByAm() && proc.getPkgList().containsKey(packageName)) {
                    synchronized (ams.mAppProfiler.mProfilerLock) {
                        totalPss += proc.mProfile.getLastPss();
                    }
                }
            }
        }
        return totalPss;
    }

    static synchronized ActivityManagerService getActivityManagerService() {
        ActivityManagerService activityManagerService;
        synchronized (ProcessUtils.class) {
            if (sAmInstance == null) {
                sAmInstance = ServiceManager.getService("activity");
            }
            activityManagerService = sAmInstance;
        }
        return activityManagerService;
    }

    public static ProcessRecord getProcessRecordByPid(int pid) {
        ProcessRecord processRecord;
        ActivityManagerService amService = getActivityManagerService();
        synchronized (amService.mPidsSelfLocked) {
            processRecord = amService.mPidsSelfLocked.get(pid);
        }
        return processRecord;
    }

    public static String getPackageNameByPid(int pid) {
        ActivityManagerService amService = getActivityManagerService();
        synchronized (amService.mPidsSelfLocked) {
            ProcessRecord processRecord = amService.mPidsSelfLocked.get(pid);
            if (processRecord != null) {
                return processRecord.info.packageName;
            }
            return null;
        }
    }

    public static String getProcessNameByPid(int pid) {
        String valueOf;
        ActivityManagerService amService = getActivityManagerService();
        synchronized (amService.mPidsSelfLocked) {
            ProcessRecord app = amService.mPidsSelfLocked.get(pid);
            valueOf = app == null ? String.valueOf(pid) : app.processName;
        }
        return valueOf;
    }

    public static int getCurAdjByPid(int pid) {
        ActivityManagerService amService = getActivityManagerService();
        synchronized (amService) {
            synchronized (amService.mPidsSelfLocked) {
                ProcessRecord processRecord = amService.mPidsSelfLocked.get(pid);
                if (processRecord != null) {
                    return processRecord.mState.getCurAdj();
                }
                return Integer.MAX_VALUE;
            }
        }
    }

    public static int getProcStateByPid(int pid) {
        ActivityManagerService amService = getActivityManagerService();
        synchronized (amService) {
            synchronized (amService.mPidsSelfLocked) {
                ProcessRecord processRecord = amService.mPidsSelfLocked.get(pid);
                if (processRecord != null) {
                    return processRecord.mState.getCurProcState();
                }
                return -1;
            }
        }
    }

    public static boolean hasForegroundActivities(int pid) {
        ActivityManagerService amService = getActivityManagerService();
        synchronized (amService) {
            synchronized (amService.mPidsSelfLocked) {
                ProcessRecord processRecord = amService.mPidsSelfLocked.get(pid);
                if (processRecord != null) {
                    return processRecord.mState.hasForegroundActivities();
                }
                return false;
            }
        }
    }

    public static int getCurSchedGroupByPid(int pid) {
        ActivityManagerService amService = getActivityManagerService();
        synchronized (amService) {
            synchronized (amService.mPidsSelfLocked) {
                ProcessRecord proc = amService.mPidsSelfLocked.get(pid);
                if (proc != null) {
                    return proc.mState.getCurrentSchedulingGroup();
                }
                return -1;
            }
        }
    }

    public static int getRenderThreadTidByPid(int pid) {
        Field renderThreadTid = ReflectionUtils.tryFindField(ProcessRecord.class, "renderThreadTid");
        if (renderThreadTid == null) {
            return 0;
        }
        ActivityManagerService amService = getActivityManagerService();
        synchronized (amService.mPidsSelfLocked) {
            ProcessRecord proc = amService.mPidsSelfLocked.get(pid);
            if (proc == null) {
                return 0;
            }
            try {
                int tid = renderThreadTid.getInt(proc);
                return tid;
            } catch (Exception e) {
                return 0;
            }
        }
    }

    public static String getTopAppPackageName() {
        ActivityTaskManagerInternal aTm = (ActivityTaskManagerInternal) LocalServices.getService(ActivityTaskManagerInternal.class);
        WindowProcessController wpc = aTm != null ? aTm.getTopApp() : null;
        if (wpc != null) {
            return getPackageNameByPid(wpc.getPid());
        }
        return null;
    }

    public static int getCurrentUserId() {
        ActivityManagerService amService = getActivityManagerService();
        return amService.getCurrentUserId();
    }

    public static int getMemoryTrimLevel() {
        int memoryTrimLevel;
        ActivityManagerService ams = getActivityManagerService();
        if (UserHandle.isIsolated(Binder.getCallingUid())) {
            throw new SecurityException("Isolated process not allowed to call getMemoryTrimLevel");
        }
        synchronized (ams) {
            memoryTrimLevel = ams.getMemoryTrimLevel();
        }
        return memoryTrimLevel;
    }

    public static List<Bundle> getRunningProcessInfos() {
        List<Bundle> result = new ArrayList<>();
        List<Integer> pids = new ArrayList<>();
        ActivityManagerService service = getActivityManagerService();
        synchronized (service) {
            for (int i = service.mProcessList.getLruSizeLOSP() - 1; i >= 0; i--) {
                ProcessRecord proc = (ProcessRecord) service.mProcessList.getLruProcessesLOSP().get(i);
                int curAdj = proc.mState.getSetAdjWithServices();
                if (proc.getThread() != null && curAdj < 500) {
                    Bundle bundle = new Bundle();
                    bundle.putInt("pid", proc.getPid());
                    bundle.putInt("adj", curAdj);
                    bundle.putLong("lastPss", proc.mProfile.getLastPss());
                    bundle.putLong("lastPssTime", proc.mProfile.getLastPssTime());
                    bundle.putString("processName", proc.processName);
                    bundle.putInt("packageUid", proc.info.uid);
                    bundle.putString("packageName", proc.info.packageName);
                    result.add(bundle);
                }
                pids.add(Integer.valueOf(proc.getPid()));
            }
        }
        synchronized (service.getProcessCpuTrackerLocked()) {
            int N = service.getProcessCpuTrackerLocked().countStats();
            for (int i2 = 0; i2 < N; i2++) {
                ProcessCpuTracker.Stats st = service.getProcessCpuTrackerLocked().getStats(i2);
                if (st.vsize > 0 && !pids.contains(Integer.valueOf(st.pid))) {
                    Bundle bundle2 = new Bundle();
                    bundle2.putInt("pid", st.pid);
                    bundle2.putInt("adj", -1000);
                    bundle2.putLong("lastPss", -1L);
                    bundle2.putLong("lastPssTime", 0L);
                    bundle2.putString("processName", st.name);
                    result.add(bundle2);
                }
            }
        }
        return result;
    }

    public static void killUnusedApp(int uid, int pid) {
        ActivityManagerService service = getActivityManagerService();
        synchronized (service) {
            int i = service.mProcessList.getLruSizeLOSP() - 1;
            while (true) {
                if (i < 0) {
                    break;
                }
                ProcessRecord app = (ProcessRecord) service.mProcessList.getLruProcessesLOSP().get(i);
                if (app != null && app.uid == uid && app.getPid() == pid && app.getThread() != null && !app.mErrorState.isCrashing() && !app.mErrorState.isNotResponding()) {
                    int tempAdj = app.mState.getSetAdj();
                    Log.i(TAG, "check  package : " + app.info.packageName + "  uid : " + uid + " pid : " + pid + " tempAdj : " + tempAdj);
                    if (tempAdj > 200 && !app.isKilledByAm()) {
                        Log.i(TAG, "kill app : " + app.info.packageName + "  uid : " + uid + " pid : " + pid);
                        app.killLocked("User unused app kill it !!", 13, true);
                    }
                }
                i--;
            }
        }
    }

    public static final boolean isDiedProcess(long uid, long pid) {
        String[] procStatusLabels = {"Uid:", "Tgid:"};
        long[] procStatusValues = {-1, -1};
        Process.readProcLines("/proc/" + pid + "/status", procStatusLabels, procStatusValues);
        if (procStatusValues[0] == uid && procStatusValues[0] == pid) {
            return false;
        }
        return true;
    }
}
