package com.android.server.am;

import android.os.BatteryStats;
import android.os.Build;
import android.text.format.DateFormat;
import android.util.LocalLog;
import android.util.Log;
import android.util.SparseArray;
import com.android.internal.os.BatteryStatsImpl;
import com.android.server.ScoutHelper;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
/* loaded from: classes.dex */
public class CpuTimeCollection {
    private static final String FORMATE_RULE = "yyyy-MM-dd-HH-mm-ss";
    private static final String TAG = "CpuTimeCollection";
    private static final int WHICH = 0;
    private static final LocalLog mCpuCollectionLog;
    private static long mRecordTime;
    private static ConcurrentHashMap<Integer, ModuleCpuTime> mCpuTimeModules = new ConcurrentHashMap<>();
    private static boolean DEBUG = Build.IS_DEBUGGABLE;

    static {
        mCpuCollectionLog = new LocalLog(DEBUG ? 500 : ScoutHelper.BINDER_FULL_KILL_SCORE_ADJ_MIN);
    }

    public static void updateUidCpuTime(BatteryStatsImpl batteryStatsImpl, boolean screenOn, boolean firstRecord) {
        SparseArray<? extends BatteryStats.Uid> uidStatsArray;
        SparseArray<? extends BatteryStats.Uid> uidStats;
        boolean z;
        boolean firstRecord2;
        SparseArray<? extends BatteryStats.Uid> uidStatsArray2;
        boolean firstRecord3;
        SparseArray<? extends BatteryStats.Uid> uidStats2;
        if (batteryStatsImpl == null || (uidStatsArray = batteryStatsImpl.getUidStats()) == null || (uidStats = uidStatsArray.clone()) == null) {
            return;
        }
        if (DEBUG) {
            z = firstRecord;
            Log.d(TAG, "firstRecord == " + z);
        } else {
            z = firstRecord;
        }
        if (mRecordTime > batteryStatsImpl.getStartClockTime()) {
            firstRecord2 = z;
        } else {
            resetCpuTimeModule();
            firstRecord2 = true;
        }
        ConcurrentHashMap<Integer, ModuleCpuTime> subCpuTimeModules = new ConcurrentHashMap<>();
        int NU = uidStats.size();
        int iu = 0;
        while (iu < NU) {
            int uid = uidStats.keyAt(iu);
            BatteryStats.Uid uidStat = (BatteryStats.Uid) uidStats.valueAt(iu);
            if (uidStat == null) {
                uidStatsArray2 = uidStatsArray;
                uidStats2 = uidStats;
                firstRecord3 = firstRecord2;
            } else {
                long userCpuTimeUs = uidStat.getUserCpuTimeUs(0);
                long systemCpuTimeUs = uidStat.getSystemCpuTimeUs(0);
                ModuleCpuTime currentItem = mCpuTimeModules.get(Integer.valueOf(uid));
                if (currentItem != null) {
                    uidStatsArray2 = uidStatsArray;
                    uidStats2 = uidStats;
                    firstRecord3 = firstRecord2;
                    ModuleCpuTime subCpuTime = new ModuleCpuTime(uid, userCpuTimeUs - currentItem.userCpuTimeUs, systemCpuTimeUs - currentItem.systemCpuTimeUs);
                    subCpuTimeModules.put(Integer.valueOf(uid), subCpuTime);
                    currentItem.userCpuTimeUs = userCpuTimeUs;
                    currentItem.systemCpuTimeUs = systemCpuTimeUs;
                } else {
                    uidStatsArray2 = uidStatsArray;
                    uidStats2 = uidStats;
                    firstRecord3 = firstRecord2;
                    ModuleCpuTime newCpuTime = new ModuleCpuTime(uid, userCpuTimeUs, systemCpuTimeUs);
                    subCpuTimeModules.put(Integer.valueOf(uid), newCpuTime);
                    mCpuTimeModules.put(Integer.valueOf(uid), newCpuTime);
                }
            }
            iu++;
            uidStats = uidStats2;
            firstRecord2 = firstRecord3;
            uidStatsArray = uidStatsArray2;
        }
        recordUidCpuTime(screenOn, firstRecord2, subCpuTimeModules);
    }

    public static void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        StringBuilder sb = new StringBuilder(2048);
        mCpuCollectionLog.dump(fd, writer, args);
        if (mCpuTimeModules.size() == 0) {
            return;
        }
        sb.append("\nDump Uid Cpu Time:\n");
        writeString(sb, mapValueCompareSort(mCpuTimeModules));
        writer.println(sb.toString());
    }

    private CpuTimeCollection() {
    }

    private static void writeString(StringBuilder stringBuilder, List<Map.Entry<Integer, ModuleCpuTime>> CpuTimeModules) {
        if (CpuTimeModules == null || CpuTimeModules.size() == 0) {
            return;
        }
        List<Map.Entry<Integer, ModuleCpuTime>> subCpuTime = CpuTimeModules.size() >= 20 ? CpuTimeModules.subList(0, 20) : CpuTimeModules;
        for (Map.Entry<Integer, ModuleCpuTime> entry : subCpuTime) {
            ModuleCpuTime moduleStats = entry.getValue();
            if (moduleStats != null && (moduleStats.userCpuTimeUs > 0 || moduleStats.systemCpuTimeUs > 0)) {
                stringBuilder.append(moduleStats.toString());
                stringBuilder.append("\n");
            }
        }
    }

    private static List<Map.Entry<Integer, ModuleCpuTime>> mapValueCompareSort(ConcurrentHashMap<Integer, ModuleCpuTime> hashMap) {
        List<Map.Entry<Integer, ModuleCpuTime>> CpuTimeModules = new ArrayList<>(hashMap.entrySet());
        Collections.sort(CpuTimeModules, new Comparator<Map.Entry<Integer, ModuleCpuTime>>() { // from class: com.android.server.am.CpuTimeCollection.1
            public int compare(Map.Entry<Integer, ModuleCpuTime> cpuTimeEntry, Map.Entry<Integer, ModuleCpuTime> cpuTimeEntryNext) {
                Long userTime = Long.valueOf(cpuTimeEntry.getValue().userCpuTimeUs + cpuTimeEntry.getValue().systemCpuTimeUs);
                Long systemTime = Long.valueOf(cpuTimeEntryNext.getValue().userCpuTimeUs + cpuTimeEntryNext.getValue().systemCpuTimeUs);
                return -userTime.compareTo(systemTime);
            }
        });
        return CpuTimeModules;
    }

    private static void recordUidCpuTime(boolean screenOn, boolean firstRecord, ConcurrentHashMap<Integer, ModuleCpuTime> subCpuTimeModules) {
        if (subCpuTimeModules == null) {
            return;
        }
        StringBuilder strb = new StringBuilder(2048);
        long curTime = System.currentTimeMillis();
        if (DEBUG) {
            Log.d(TAG, "Screen state " + screenOn + ", firstRecord = " + firstRecord);
        }
        int status = getStatus(screenOn, firstRecord);
        switch (status) {
            case 0:
                strb.append("[screen_on (" + DateFormat.format(FORMATE_RULE, mRecordTime).toString() + "--" + DateFormat.format(FORMATE_RULE, curTime).toString() + ")]");
                break;
            case 1:
                strb.append("[first record screen off (" + DateFormat.format(FORMATE_RULE, curTime).toString() + ")]");
                break;
            case 2:
                strb.append("[screen_off (" + DateFormat.format(FORMATE_RULE, mRecordTime).toString() + "--" + DateFormat.format(FORMATE_RULE, curTime).toString() + ")]");
                break;
            case 3:
                strb.append("[first record screen on (" + DateFormat.format(FORMATE_RULE, curTime).toString() + ")]");
                break;
        }
        mRecordTime = curTime;
        strb.append("\ndump subtract cpu time:\n");
        if (subCpuTimeModules.size() != 0) {
            writeString(strb, mapValueCompareSort(subCpuTimeModules));
        }
        mCpuCollectionLog.log(strb.toString());
    }

    private static int getStatus(boolean screenOn, boolean firstRecord) {
        return (screenOn ? 2 : 0) | (firstRecord ? 1 : 0);
    }

    /* loaded from: classes.dex */
    public static class ModuleCpuTime {
        int moduleUid;
        long systemCpuTimeUs;
        long userCpuTimeUs;

        public ModuleCpuTime(int uid, long userCpuTimeUs, long systemCpuTimeUs) {
            this.moduleUid = uid;
            long j = 0;
            this.userCpuTimeUs = userCpuTimeUs > 0 ? userCpuTimeUs : 0L;
            this.systemCpuTimeUs = systemCpuTimeUs > 0 ? systemCpuTimeUs : j;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(128);
            if (this.userCpuTimeUs > 0 || this.systemCpuTimeUs > 0) {
                sb.append(this.moduleUid);
                sb.append(":  Total cpu time: u=");
                BatteryStats.formatTimeMs(sb, this.userCpuTimeUs / 1000);
                sb.append("s=");
                BatteryStats.formatTimeMs(sb, this.systemCpuTimeUs / 1000);
            }
            return sb.toString();
        }
    }

    public static void resetCpuTimeModule() {
        mCpuTimeModules.clear();
    }
}
