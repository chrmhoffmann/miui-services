package com.android.server.am;

import android.os.spc.PressureStateSettings;
import android.util.ArrayMap;
import android.util.EventLog;
import android.util.Slog;
import com.android.server.am.AppStateManager;
import com.android.server.input.pocketmode.MiuiPocketModeSensorWrapper;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
/* loaded from: classes.dex */
public class ProcMemCleanerStatistics {
    private static final boolean DEBUG;
    public static final int EVENT_TAGS = 80800;
    private static final int MAX_KILL_REPORT_COUNTS;
    public static final long MAX_RECENTLY_KILL_REPORT_TIME = 10000;
    public static final String REASON_CLEAN_UP_MEM = "clean_up_mem";
    public static final String REASON_KILL_BG_PROC = "kill_bg_proc";
    private static final String TAG = "ProcessMemCleanerStatistics";
    public static final int TYPE_FORCE_STOP = 2;
    public static final int TYPE_KILL_NONE = 0;
    public static final int TYPE_KILL_PROC = 1;
    private static List<String> sCommonUsedPackageList;
    private static ProcMemCleanerStatistics sInstance;
    private final List<KillInfo> mKillInfos = new LinkedList();
    private long mPreCleanUpTime = 0;
    private ArrayMap<String, Long> mLastKillProc = new ArrayMap<>();
    private int mTotalKillsProc = 0;
    private SimpleDateFormat mDateformat = new SimpleDateFormat("HH:mm:ss");

    static {
        boolean z = PressureStateSettings.DEBUG_ALL;
        DEBUG = z;
        MAX_KILL_REPORT_COUNTS = z ? 1000 : MiuiPocketModeSensorWrapper.STATE_STABLE_DELAY;
        ArrayList arrayList = new ArrayList();
        sCommonUsedPackageList = arrayList;
        arrayList.add("com.tencent.mm");
    }

    private ProcMemCleanerStatistics() {
    }

    public static ProcMemCleanerStatistics getInstance() {
        if (sInstance == null) {
            sInstance = new ProcMemCleanerStatistics();
        }
        return sInstance;
    }

    public static boolean isCommonUsedApp(String pckName) {
        return sCommonUsedPackageList.contains(pckName);
    }

    private String genLastKillKey(String procName, int uid) {
        return procName + uid;
    }

    public boolean isLastKillProcess(String procName, int uid, long nowTime) {
        int index = this.mLastKillProc.indexOfKey(genLastKillKey(procName, uid));
        if (index >= 0) {
            long value = this.mLastKillProc.valueAt(index).longValue();
            if (value - nowTime <= 10000) {
                return true;
            }
            this.mLastKillProc.remove(genLastKillKey(procName, uid));
            return false;
        }
        return false;
    }

    public void checkLastKillTime(long nowTime) {
        if (DEBUG) {
            debugAppGroupToString();
        }
        if (nowTime - this.mPreCleanUpTime > 10000) {
            this.mLastKillProc.clear();
        }
        this.mPreCleanUpTime = nowTime;
    }

    public void reportEvent(int eventType, AppStateManager.AppState.RunningProcess app, long pss, String killReason) {
        KillInfo info = new KillInfo();
        info.uid = app.getUid();
        info.name = app.getProcessName();
        info.pid = app.getPid();
        info.killType = eventType;
        info.time = System.currentTimeMillis();
        info.priority = ProcessMemoryCleaner.getProcPriority(app);
        info.pss = pss;
        info.killReason = killReason;
        if (eventType == 1) {
            this.mLastKillProc.put(genLastKillKey(app.getProcessName(), app.getUid()), Long.valueOf(info.time));
        }
        synchronized (this.mKillInfos) {
            this.mKillInfos.add(info);
            if (this.mKillInfos.size() >= MAX_KILL_REPORT_COUNTS) {
                this.mKillInfos.remove(0);
            }
            this.mTotalKillsProc++;
        }
        if (ProcessMemoryCleaner.DEBUG) {
            Slog.d(ProcessMemoryCleaner.TAG, "spckill:" + info.toString());
        }
        EventLog.writeEvent((int) EVENT_TAGS, "spckill:" + info.toString());
    }

    public void dumpKillInfo(PrintWriter pw) {
        pw.println("ProcessMemCleanerStatistics KillInfo");
        synchronized (this.mKillInfos) {
            pw.println(String.format("total=%s last=%s", Integer.valueOf(this.mTotalKillsProc), Integer.valueOf(this.mKillInfos.size())));
            for (KillInfo i : this.mKillInfos) {
                pw.println(i.toString());
            }
        }
    }

    private void debugAppGroupToString() {
        for (Map.Entry<String, Long> entry : this.mLastKillProc.entrySet()) {
            Slog.d(TAG, "key:" + entry.getKey() + " value:" + this.mDateformat.format(entry.getValue()));
        }
    }

    /* loaded from: classes.dex */
    public class KillInfo {
        private String killReason;
        private int killType;
        private String name;
        private int pid;
        private int priority;
        private long pss;
        private long time;
        private int uid;

        private KillInfo() {
            ProcMemCleanerStatistics.this = r1;
        }

        public String toString() {
            return String.format("#%s n:%s(%d) u:%d t:%d pri:%s pss:%d r:%s", getType(), this.name, Integer.valueOf(this.pid), Integer.valueOf(this.uid), Long.valueOf(this.time), Integer.valueOf(this.priority), Long.valueOf(this.pss), this.killReason);
        }

        public String getType() {
            switch (this.killType) {
                case 1:
                    return "KillProc";
                case 2:
                    return "ForceStop";
                default:
                    return ProcessPolicy.REASON_UNKNOWN;
            }
        }
    }
}
