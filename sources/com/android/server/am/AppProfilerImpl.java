package com.android.server.am;

import android.os.Debug;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import com.android.internal.os.ProcessCpuTracker;
import com.android.server.ScoutSystemMonitor;
import com.android.server.content.SyncManagerStubImpl;
import com.miui.base.MiuiStubRegistry;
import com.miui.daemon.performance.PerfShielderManager;
import com.miui.server.sentinel.MiuiSentinelMemoryManager;
import com.miui.server.stability.ScoutDisplayMemoryManager;
import java.util.ArrayList;
/* loaded from: classes.dex */
public class AppProfilerImpl implements AppProfilerStub {
    private static boolean CTS_testTrimMemActivityBg_workaround = SystemProperties.getBoolean("persist.sys.cts.testTrimMemActivityBg.wk.enable", false);
    private static final String TAG = "AppProfilerImpl";
    private long mLastMemUsageReportTime;
    private ActivityManagerService mService;
    private final long REPORT_OOM_MEMINFO_INTERVAL_MILLIS = SyncManagerStubImpl.SYNC_DELAY_ON_DISALLOW_METERED;
    private long mLastReportOomMemTime = 0;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<AppProfilerImpl> {

        /* compiled from: AppProfilerImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final AppProfilerImpl INSTANCE = new AppProfilerImpl();
        }

        public AppProfilerImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public AppProfilerImpl provideNewInstance() {
            return new AppProfilerImpl();
        }
    }

    public void init(ActivityManagerService ams) {
        this.mService = ams;
        MiuiSentinelMemoryManager.getInstance().init(ams, ams.mContext);
        ScoutDisplayMemoryManager.getInstance().init(ams, ams.mContext);
    }

    public void reportScoutLowMemory(ScoutMeminfo scoutMeminfo) {
        ScoutDisplayMemoryManager.getInstance().checkScoutLowMemory(scoutMeminfo);
    }

    public void reportScoutLowMemory(int adj) {
        if (this.mService == null || !ScoutDisplayMemoryManager.getInstance().isEnableScoutMemory()) {
            return;
        }
        long now = SystemClock.uptimeMillis();
        if (adj >= 0 && adj <= 800 && !this.mService.mProcessList.haveBackgroundProcessLOSP() && now > this.mLastMemUsageReportTime + 180000) {
            this.mLastMemUsageReportTime = now;
            Slog.e("MIUIScout Memory", "Scout report Low memory,currently killed process adj= " + adj);
            ScoutSystemMonitor.getInstance().setWorkMessage(0);
        }
    }

    public void reportPssRecord(String processName, String packageName, long pss) {
        try {
            PerfShielderManager.getService().reportPssRecord(processName, packageName, pss, "1.0", 1);
        } catch (RemoteException e) {
        }
    }

    public void reportOomMemRecordIfNeeded(ActivityManagerService ams, ProcessCpuTracker processCpuTracker) {
        long now = SystemClock.uptimeMillis();
        if (now - this.mLastReportOomMemTime < SyncManagerStubImpl.SYNC_DELAY_ON_DISALLOW_METERED) {
            return;
        }
        this.mLastReportOomMemTime = now;
        SparseIntArray procs = new SparseIntArray();
        synchronized (ams) {
            ArrayList<ProcessRecord> lruProcs = ams.mProcessList.getLruProcessesLOSP();
            for (int i = lruProcs.size() - 1; i >= 0; i--) {
                ProcessRecord r = lruProcs.get(i);
                procs.put(r.mPid, r.mState.getSetAdjWithServices());
            }
        }
        SparseArray<String> nativeProcs = new SparseArray<>();
        ams.updateCpuStatsNow();
        synchronized (processCpuTracker) {
            int N = processCpuTracker.countStats();
            for (int i2 = 0; i2 < N; i2++) {
                ProcessCpuTracker.Stats st = processCpuTracker.getStats(i2);
                if (st.vsize > 0 && procs.indexOfKey(st.pid) < 0) {
                    nativeProcs.put(st.pid, st.name);
                }
            }
        }
        long[] oomPss = new long[ActivityManagerService.DUMP_MEM_OOM_LABEL.length];
        for (int i3 = 0; i3 < procs.size(); i3++) {
            int pid = procs.keyAt(i3);
            int oomAdj = procs.get(pid);
            long myTotalPss = Debug.getPss(pid, null, null);
            for (int oomIndex = 0; oomIndex < oomPss.length; oomIndex++) {
                if (oomIndex == oomPss.length - 1 || (oomAdj >= ActivityManagerService.DUMP_MEM_OOM_ADJ[oomIndex] && oomAdj < ActivityManagerService.DUMP_MEM_OOM_ADJ[oomIndex + 1])) {
                    oomPss[oomIndex] = oomPss[oomIndex] + myTotalPss;
                    break;
                }
            }
        }
        for (int i4 = 0; i4 < nativeProcs.size(); i4++) {
            int pid2 = nativeProcs.keyAt(i4);
            String name = nativeProcs.get(pid2);
            long myTotalPss2 = Debug.getPss(pid2, null, null);
            if (myTotalPss2 != 0) {
                oomPss[0] = oomPss[0] + myTotalPss2;
                reportPssRecord(name, "native", myTotalPss2);
            }
        }
        for (int i5 = 0; i5 < ActivityManagerService.DUMP_MEM_OOM_LABEL.length; i5++) {
            if (oomPss[i5] != 0) {
                reportPssRecord("OomMeminfo." + ActivityManagerService.DUMP_MEM_OOM_LABEL[i5], "OomMeminfo", oomPss[i5]);
            }
        }
    }

    public void reportPackagePss(ActivityManagerService ams, String packageName) {
        long pkgPss = 0;
        synchronized (ams) {
            ArrayList<ProcessRecord> lruProcs = ams.mProcessList.getLruProcessesLOSP();
            for (int i = lruProcs.size() - 1; i >= 0; i--) {
                ProcessRecord r = lruProcs.get(i);
                if (r.info.packageName.equals(packageName)) {
                    if (r.mProfile.getLastPss() == 0) {
                        return;
                    }
                    pkgPss += r.mProfile.getLastPss();
                }
            }
            reportPssRecord("Package." + packageName, packageName, pkgPss);
        }
    }

    public boolean ctsTestTrimMemActivityBgWk(String processName) {
        if (CTS_testTrimMemActivityBg_workaround && processName.contains("com.android.app1:android.app.stubs.TrimMemService:trimmem_")) {
            Slog.i(TAG, "CTS special case.");
            return true;
        }
        return false;
    }
}
