package com.android.server.am;

import android.app.ActivityManager;
import android.app.ActivityThread;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Process;
import android.os.SystemProperties;
import android.util.EventLog;
import android.util.Pair;
import android.util.Slog;
import com.android.server.AppOpsServiceState;
import com.android.server.wm.WindowProcessUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import miui.content.pm.PreloadedAppPolicy;
import miui.io.IoUtils;
/* loaded from: classes.dex */
public class AutoStartManagerService {
    private static final String TAG = "AutoStartManagerService";
    private static HashMap<String, String> startServiceWhiteList;
    private static final boolean ENABLE_SIGSTOP_KILL = SystemProperties.getBoolean("persist.proc.enable_sigstop", true);
    private static AutoStartManagerService sInstance = new AutoStartManagerService();

    static {
        HashMap<String, String> hashMap = new HashMap<>();
        startServiceWhiteList = hashMap;
        hashMap.put("com.miui.huanji", "com.miui.huanji.parsebak.RestoreXSpaceService");
    }

    public static AutoStartManagerService getInstance() {
        return sInstance;
    }

    public boolean isAllowStartService(Context context, Intent service, int userId) {
        try {
            String packageName = service.getComponent().getPackageName();
            IPackageManager packageManager = AppGlobals.getPackageManager();
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, 0L, userId);
            if (applicationInfo == null) {
                return true;
            }
            int uid = applicationInfo.uid;
            return isAllowStartService(context, service, userId, uid);
        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
    }

    public boolean isAllowStartService(Context context, Intent service, int userId, int uid) {
        Exception e;
        AppOpsManager aom;
        try {
            aom = (AppOpsManager) context.getSystemService("appops");
        } catch (Exception e2) {
            e = e2;
        }
        if (aom == null) {
            return true;
        }
        ActivityManagerService ams = ActivityManager.getService();
        ResolveInfo rInfo = AppGlobals.getPackageManager().resolveService(service, (String) null, 1024L, userId);
        ServiceInfo sInfo = rInfo != null ? rInfo.serviceInfo : null;
        if (sInfo != null && (rInfo.serviceInfo.applicationInfo.flags & 1) == 0 && !PreloadedAppPolicy.isProtectedDataApp(context, rInfo.serviceInfo.applicationInfo.packageName, 0)) {
            String packageName = service.getComponent().getPackageName();
            boolean isRunning = WindowProcessUtils.isPackageRunning(ams.mActivityTaskManager, packageName, sInfo.processName, uid);
            if (isRunning) {
                return true;
            }
            int mode = aom.noteOpNoThrow(10008, uid, packageName, (String) null, "AutoStartManagerService#isAllowStartService");
            if (mode == 0) {
                return true;
            }
            try {
            } catch (Exception e3) {
                e = e3;
                e.printStackTrace();
                return true;
            }
            try {
                Slog.i(TAG, "MIUILOG- Reject service :" + service + " userId : " + userId + " uid : " + uid);
                return false;
            } catch (Exception e4) {
                e = e4;
                e.printStackTrace();
                return true;
            }
        }
        return true;
    }

    public static boolean canRestartServiceLocked(String packageName, int uid, String message) {
        return canRestartServiceLocked(packageName, uid, message, null, false);
    }

    public static boolean canRestartServiceLocked(String packageName, int uid, String message, ComponentName component, boolean isNote) {
        int mode;
        if (AppOpsServiceState.isCtsIgnore(packageName)) {
            return true;
        }
        if (component != null && startServiceWhiteList.containsKey(packageName) && startServiceWhiteList.get(packageName).equals(component.getClassName())) {
            return true;
        }
        AppOpsManager appOpsManager = (AppOpsManager) ActivityThread.currentApplication().getSystemService(AppOpsManager.class);
        if (isNote) {
            mode = appOpsManager.noteOpNoThrow(10008, uid, packageName, (String) null, message);
        } else {
            mode = appOpsManager.checkOpNoThrow(10008, uid, packageName);
        }
        if (mode == 0) {
            return true;
        }
        if (isNote && component != null) {
            Slog.i(TAG, "MIUILOG- Reject RestartService service :" + component + " uid : " + uid);
            return false;
        }
        return false;
    }

    public void signalStopProcessesLocked(ArrayList<Pair<ProcessRecord, Boolean>> procs, boolean allowRestart, String packageName, int uid) {
        if (!ENABLE_SIGSTOP_KILL) {
            return;
        }
        ActivityManagerService ams = ActivityManager.getService();
        if (allowRestart && canRestartServiceLocked(packageName, uid, "AutoStartManagerService#signalStopProcessesLocked")) {
            return;
        }
        final ArrayList<KillProcessInfo> tmpProcs = new ArrayList<>();
        Iterator<Pair<ProcessRecord, Boolean>> it = procs.iterator();
        while (it.hasNext()) {
            Pair<ProcessRecord, Boolean> proc = it.next();
            tmpProcs.add(new KillProcessInfo((ProcessRecord) proc.first));
        }
        sendSignalToProcessLocked(tmpProcs, 19, false);
        ams.mHandler.postDelayed(new Runnable() { // from class: com.android.server.am.AutoStartManagerService.1
            @Override // java.lang.Runnable
            public void run() {
                AutoStartManagerService.this.sendSignalToProcessLocked(tmpProcs, 18, true);
            }
        }, 500L);
    }

    void sendSignalToProcessLocked(List<KillProcessInfo> procs, int signal, boolean needKillAgain) {
        HashMap<Integer, Pair<Integer, String>> allUncoverdPids = searchAllUncoverdProcsPid(procs);
        for (KillProcessInfo proc : procs) {
            Slog.d(TAG, "prepare force stop p:" + proc.pid + " s: " + signal);
            Process.sendSignal(proc.pid, signal);
            allUncoverdPids.remove(Integer.valueOf(proc.pid));
        }
        for (Integer pid : allUncoverdPids.keySet()) {
            Slog.d(TAG, "prepare force stop native p:" + pid + " s: " + signal);
            Process.sendSignal(pid.intValue(), signal);
        }
        if (needKillAgain) {
            for (KillProcessInfo p : procs) {
                int pid2 = p.pid;
                int uid = p.uid;
                if (pid2 > 0 && !ProcessUtils.isDiedProcess(uid, pid2)) {
                    EventLog.writeEvent(30023, Integer.valueOf(uid), Integer.valueOf(pid2), p.processName, 1001, "Kill Again");
                    Process.killProcessQuiet(pid2);
                    Process.killProcessGroup(uid, pid2);
                }
            }
        }
    }

    private HashMap<Integer, Pair<Integer, String>> searchAllUncoverdProcsPid(List<KillProcessInfo> procs) {
        HashMap<Integer, Pair<Integer, String>> resPids = new HashMap<>();
        for (KillProcessInfo p : procs) {
            Pair<Integer, String> procInfo = new Pair<>(Integer.valueOf(p.uid), p.processName);
            List<Integer> natiiveProcs = searchNativeProc(p.uid, p.pid);
            for (Integer nPid : natiiveProcs) {
                resPids.put(nPid, procInfo);
            }
        }
        return resPids;
    }

    public static List<Integer> searchNativeProc(int uid, int pid) {
        BufferedReader reader = null;
        List<Integer> nativeProcs = new ArrayList<>();
        try {
            try {
                String fileName = ProcessManagerService.getCgroupFilePath(uid, pid);
                reader = new BufferedReader(new FileReader(new File(fileName)));
                while (true) {
                    String line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    int proc = Integer.parseInt(line);
                    if (proc > 0 && pid != proc) {
                        nativeProcs.add(Integer.valueOf(proc));
                    }
                }
            } catch (IOException e) {
            } catch (Exception e2) {
                e2.printStackTrace();
            }
            return nativeProcs;
        } finally {
            IoUtils.closeQuietly(reader);
        }
    }

    /* loaded from: classes.dex */
    public static final class KillProcessInfo {
        int pid;
        ProcessRecord proc;
        String processName;
        int uid;

        KillProcessInfo(ProcessRecord proc) {
            this.uid = proc.uid;
            this.pid = proc.mPid;
            this.processName = proc.processName;
        }
    }
}
