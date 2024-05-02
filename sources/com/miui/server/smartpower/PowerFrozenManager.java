package com.miui.server.smartpower;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.SystemClock;
import android.util.EventLog;
import android.util.Slog;
import android.util.SparseArray;
import com.miui.server.greeze.FreezeUtils;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
/* loaded from: classes.dex */
public class PowerFrozenManager {
    private static final int HISTORY_SIZE = 4096;
    private static final int MSG_ASYNC_THAW_PID = 1;
    public static final String TAG = "PowerFrozen";
    private H mHandler;
    private HandlerThread mHandlerTh;
    private static final boolean DEBUG = SmartPowerSettings.DEBUG_ALL;
    public static final PowerFrozenManager sInstance = new PowerFrozenManager();
    private static boolean sEnable = SmartPowerSettings.PROP_FROZEN_ENABLE;
    private final SparseArray<Integer> mAllFrozenPids = new SparseArray<>();
    private final SparseArray<FrozenInfo> mFrozenList = new SparseArray<>();
    private FrozenInfo[] mFrozenHistory = new FrozenInfo[4096];
    private int mHistoryIndexNext = 0;
    private List<IFrozenReportCallback> mFrozenCallbacks = new ArrayList();

    /* loaded from: classes.dex */
    public interface IFrozenReportCallback {
        void reportBinderState(int i, int i2, int i3, int i4, long j);

        void reportBinderTrans(int i, int i2, int i3, int i4, int i5, boolean z, long j, long j2);

        void reportNet(int i, long j);

        void reportSignal(int i, int i2, long j);

        void serviceReady(boolean z);

        void thawedByOther(int i, int i2);
    }

    private PowerFrozenManager() {
        HandlerThread handlerThread = new HandlerThread("PowerFrozenTh", -2);
        this.mHandlerTh = handlerThread;
        handlerThread.start();
        this.mHandler = new H(this.mHandlerTh.getLooper());
        Process.setThreadGroupAndCpuset(this.mHandlerTh.getThreadId(), 1);
    }

    public static PowerFrozenManager getInstance() {
        return sInstance;
    }

    public void syncCloudControlSettings(boolean frozenEnable) {
        if (!frozenEnable && sEnable) {
            thawAll("disable frozen from cloud control");
        }
    }

    public boolean frozenProcess(int uid, int pid, String processName, String reason) {
        return frozenProcess(uid, pid, processName, reason, 0L);
    }

    public boolean frozenProcess(int uid, int pid, String processName, String reason, long timeOut) {
        boolean done;
        if (isEnable() && pid > 0 && Process.myPid() != pid && Process.getUidForPid(pid) == uid) {
            if (SmartPowerSettings.PROP_FROZEN_CGROUPV1_ENABLE) {
                done = FreezeUtils.freezePid(pid);
            } else {
                done = FreezeUtils.freezePid(pid, uid);
            }
            addFrozenPid(uid, pid, true);
            synchronized (this.mFrozenList) {
                FrozenInfo info = this.mFrozenList.get(pid);
                if (info == null) {
                    info = new FrozenInfo(uid, pid, processName);
                    this.mFrozenList.put(pid, info);
                }
                info.addFrozenInfo(System.currentTimeMillis(), reason);
                if (DEBUG) {
                    Slog.d(TAG, "Frozen " + info + " reason:" + reason);
                }
                EventLog.writeEvent((int) SmartPowerSettings.EVENT_TAGS, "frozen " + info);
                if (timeOut != 0) {
                    Message msg = this.mHandler.obtainMessage(1, Integer.valueOf(info.mPid));
                    msg.arg1 = info.mUid;
                    msg.arg2 = info.mPid;
                    this.mHandler.sendMessageDelayed(msg, timeOut);
                }
            }
            return done;
        }
        return false;
    }

    public boolean thawProcess(int uid, int pid, String reason) {
        boolean done;
        if (!isEnable()) {
            return false;
        }
        if (SmartPowerSettings.PROP_FROZEN_CGROUPV1_ENABLE) {
            done = FreezeUtils.thawPid(pid);
        } else {
            done = FreezeUtils.thawPid(pid, uid);
        }
        removeFrozenPid(uid, pid, true);
        synchronized (this.mFrozenList) {
            FrozenInfo info = this.mFrozenList.get(pid);
            if (info == null) {
                info = new FrozenInfo(uid, pid, "unknown");
            }
            info.mThawTime = System.currentTimeMillis();
            info.mThawUptime = SystemClock.uptimeMillis();
            info.mThawReason = reason;
            this.mFrozenList.remove(pid);
            addHistoryInfo(info);
            if (DEBUG) {
                Slog.d(TAG, "Thaw " + info + " " + info.getFrozenDuration() + "ms reason:" + reason);
            }
            EventLog.writeEvent((int) SmartPowerSettings.EVENT_TAGS, "thaw " + info);
        }
        return done;
    }

    public void thawUid(int uid, String reason) {
        synchronized (this.mAllFrozenPids) {
            ArrayList<Integer> frozenPids = getFrozenPids(uid);
            for (int i = frozenPids.size() - 1; i >= 0; i--) {
                thawProcess(uid, frozenPids.get(i).intValue(), reason);
            }
        }
    }

    public ArrayList<Integer> getFrozenPids(int uid) {
        ArrayList<Integer> frozenPids = new ArrayList<>();
        synchronized (this.mAllFrozenPids) {
            for (int i = this.mAllFrozenPids.size() - 1; i >= 0; i--) {
                if (this.mAllFrozenPids.valueAt(i).intValue() == uid) {
                    frozenPids.add(Integer.valueOf(this.mAllFrozenPids.keyAt(i)));
                }
            }
        }
        return frozenPids;
    }

    public void thawAll(String reason) {
        synchronized (this.mAllFrozenPids) {
            ArrayList<Integer> frozenUids = new ArrayList<>();
            ArrayList<Integer> frozenPids = new ArrayList<>();
            for (int i = this.mAllFrozenPids.size() - 1; i >= 0; i--) {
                frozenUids.add(this.mAllFrozenPids.valueAt(i));
                frozenPids.add(Integer.valueOf(this.mAllFrozenPids.keyAt(i)));
            }
            int i2 = frozenPids.size();
            for (int i3 = i2 - 1; i3 >= 0; i3--) {
                thawProcess(frozenUids.get(i3).intValue(), frozenPids.get(i3).intValue(), reason);
            }
        }
    }

    public boolean isAllFrozenForUid(int uid) {
        synchronized (this.mAllFrozenPids) {
            for (int i = 0; i < this.mAllFrozenPids.size(); i++) {
                if (this.mAllFrozenPids.valueAt(i).intValue() == uid) {
                    return true;
                }
            }
            return false;
        }
    }

    public boolean isAllFrozenPid(int pid) {
        boolean z;
        synchronized (this.mAllFrozenPids) {
            z = this.mAllFrozenPids.get(pid) != null;
        }
        return z;
    }

    public boolean isFrozenForUid(int uid) {
        synchronized (this.mFrozenList) {
            for (int i = 0; i < this.mFrozenList.size(); i++) {
                FrozenInfo info = this.mFrozenList.valueAt(i);
                if (info != null && info.mUid == uid) {
                    return true;
                }
            }
            return false;
        }
    }

    public boolean isFrozenPid(int pid) {
        boolean z;
        synchronized (this.mFrozenList) {
            z = this.mFrozenList.get(pid) != null;
        }
        return z;
    }

    public void addFrozenPid(int uid, int pid) {
        addFrozenPid(uid, pid, false);
    }

    private void addFrozenPid(int uid, int pid, boolean isSmartPower) {
        synchronized (this.mAllFrozenPids) {
            if (DEBUG) {
                Slog.d(TAG, "add p:" + pid + "/u:" + uid + " isMy:" + isSmartPower);
            }
            if (this.mHandler.hasMessages(1, Integer.valueOf(pid))) {
                this.mHandler.removeMessages(1, Integer.valueOf(pid));
            }
            this.mAllFrozenPids.put(pid, Integer.valueOf(uid));
        }
    }

    public void removeFrozenPid(int uid, int pid) {
        removeFrozenPid(uid, pid, false);
    }

    private void removeFrozenPid(int uid, int pid, boolean isSmartPower) {
        synchronized (this.mAllFrozenPids) {
            if (DEBUG) {
                Slog.d(TAG, "remove p:" + pid + "/u:" + uid + " isMy:" + isSmartPower);
            }
            this.mAllFrozenPids.remove(pid);
            if (this.mHandler.hasMessages(1, Integer.valueOf(pid))) {
                this.mHandler.removeMessages(1, Integer.valueOf(pid));
            }
        }
    }

    public void registerFrozenCallback(IFrozenReportCallback callback) {
        this.mFrozenCallbacks.add(callback);
    }

    public void unRegisterFrozenCallback(IFrozenReportCallback callback) {
        this.mFrozenCallbacks.remove(callback);
    }

    public void reportSignal(int uid, int pid, long now) {
        if (!isEnable()) {
            return;
        }
        for (IFrozenReportCallback callback : this.mFrozenCallbacks) {
            callback.reportSignal(uid, pid, now);
        }
    }

    public void reportNet(int uid, long now) {
        if (!isEnable()) {
            return;
        }
        for (IFrozenReportCallback callback : this.mFrozenCallbacks) {
            callback.reportNet(uid, now);
        }
    }

    public void reportBinderTrans(int dstUid, int dstPid, int callerUid, int callerPid, int callerTid, boolean isOneway, long now, long buffer) {
        if (!isEnable()) {
            return;
        }
        for (IFrozenReportCallback callback : this.mFrozenCallbacks) {
            callback.reportBinderTrans(dstUid, dstPid, callerUid, callerPid, callerTid, isOneway, now, buffer);
        }
    }

    public void serviceReady(boolean ready) {
        sEnable = ready && SmartPowerSettings.PROP_FROZEN_ENABLE;
        Slog.d(TAG, "serviceReady millet:" + ready + " frozen:" + sEnable);
        for (IFrozenReportCallback callback : this.mFrozenCallbacks) {
            callback.serviceReady(sEnable);
        }
    }

    public void reportBinderState(int uid, int pid, int tid, int binderState, long now) {
        if (!isEnable()) {
            return;
        }
        for (IFrozenReportCallback callback : this.mFrozenCallbacks) {
            callback.reportBinderState(uid, pid, tid, binderState, now);
        }
    }

    public void thawedByOther(int uid, int pid) {
        if (!isEnable()) {
            return;
        }
        for (IFrozenReportCallback callback : this.mFrozenCallbacks) {
            callback.thawedByOther(uid, pid);
        }
    }

    public static boolean isEnable() {
        return sEnable;
    }

    private static int ringAdvance(int origin, int increment, int size) {
        int index = (origin + increment) % size;
        return index < 0 ? index + size : index;
    }

    private void addHistoryInfo(FrozenInfo info) {
        FrozenInfo[] frozenInfoArr = this.mFrozenHistory;
        int i = this.mHistoryIndexNext;
        frozenInfoArr[i] = info;
        this.mHistoryIndexNext = ringAdvance(i, 1, 4096);
    }

    private List<FrozenInfo> getHistoryInfos(long sinceUptime) {
        List<FrozenInfo> ret = new ArrayList<>();
        int index = ringAdvance(this.mHistoryIndexNext, -1, 4096);
        for (int i = 0; i < 4096; i++) {
            FrozenInfo frozenInfo = this.mFrozenHistory[index];
            if (frozenInfo == null || frozenInfo.mThawTime < sinceUptime) {
                break;
            }
            ret.add(this.mFrozenHistory[index]);
            index = ringAdvance(index, -1, 4096);
        }
        return ret;
    }

    public void dump(PrintWriter pw, String[] args, int opti) {
        try {
            if (opti < args.length) {
                String parm = args[opti];
                int opti2 = opti + 1;
                if (!parm.contains("history") && !parm.contains("-h")) {
                    if (!parm.contains("current") && !parm.contains("-c")) {
                        if (parm.contains("uid") || parm.contains("-u")) {
                            dumpUid(pw, args, opti2);
                        }
                        return;
                    }
                    dumpFrozen(pw, args, opti2, 0);
                    return;
                }
                dumpHistory(pw, args, opti2, 0);
                return;
            }
            dumpSettings(pw, args, opti);
            dumpFrozen(pw, args, opti, 0);
            dumpHistory(pw, args, opti, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void dumpSettings(PrintWriter pw, String[] args, int opti) {
        pw.println("Settings:");
        pw.println("  enable=" + sEnable + " def(" + SmartPowerSettings.PROP_FROZEN_ENABLE + ")");
    }

    private void dumpUid(PrintWriter pw, String[] args, int opti) {
        if (opti < args.length) {
            String uidStr = args[opti];
            int dpUid = Integer.parseInt(uidStr);
            dumpFrozen(pw, args, opti, dpUid);
            dumpHistory(pw, args, opti, dpUid);
        }
    }

    private void dumpFrozen(PrintWriter pw, String[] args, int opti, int dpUid) {
        pw.println("frozen all: ");
        synchronized (this.mAllFrozenPids) {
            int n = this.mAllFrozenPids.size();
            for (int i = 0; i < n; i++) {
                int pid = this.mAllFrozenPids.keyAt(i);
                int uid = this.mAllFrozenPids.valueAt(i).intValue();
                pw.print(" p:" + pid + "/u:" + uid);
            }
        }
        pw.println("");
        SimpleDateFormat formater = new SimpleDateFormat(SmartPowerSettings.TIME_FORMAT_PATTERN);
        pw.println("smart power frozen:");
        synchronized (this.mFrozenList) {
            int n2 = this.mFrozenList.size();
            for (int i2 = 0; i2 < n2; i2++) {
                FrozenInfo info = this.mFrozenList.valueAt(i2);
                if (dpUid == 0 || info.mUid == dpUid) {
                    pw.print("  ");
                    pw.print("#" + (i2 + 1));
                    pw.println(" " + info);
                    for (int index = 0; index < info.mFrozenTimes.size(); index++) {
                        pw.print("    ");
                        pw.print("fz: ");
                        pw.print(formater.format(new Date(((Long) info.mFrozenTimes.get(index)).longValue())));
                        pw.print(" " + ((String) info.mFrozenReasons.get(index)));
                        pw.println("");
                    }
                }
            }
        }
        pw.println("");
    }

    private void dumpHistory(PrintWriter pw, String[] args, int opti, int dpUid) {
        pw.println("smart power frozen in history:");
        List<FrozenInfo> infos = getHistoryInfos(SystemClock.uptimeMillis() - SmartPowerSettings.MAX_HISTORY_REPORT_DURATION);
        int index = 1;
        SimpleDateFormat formater = new SimpleDateFormat(SmartPowerSettings.TIME_FORMAT_PATTERN);
        for (FrozenInfo info : infos) {
            if (dpUid == 0 || info.mUid == dpUid) {
                pw.print("  ");
                int index2 = index + 1;
                pw.print("#" + index);
                pw.print(" " + formater.format(new Date(info.mThawTime)));
                pw.print(" " + info);
                pw.println(" " + info.getFrozenDuration() + "ms");
                for (int i = 0; i < info.mFrozenTimes.size(); i++) {
                    pw.print("    ");
                    pw.print("fz: ");
                    pw.print(formater.format(new Date(((Long) info.mFrozenTimes.get(i)).longValue())));
                    pw.print(" " + ((String) info.mFrozenReasons.get(i)));
                    pw.println("");
                }
                pw.print("    ");
                pw.print("th: ");
                pw.print(formater.format(new Date(info.mThawTime)));
                pw.println(" " + info.mThawReason);
                index = index2;
            }
        }
    }

    /* loaded from: classes.dex */
    public static class FrozenInfo {
        private List<String> mFrozenReasons;
        private List<Long> mFrozenTimes;
        private int mPid;
        private String mProcessName;
        private String mThawReason;
        private long mThawTime;
        private long mThawUptime;
        private int mUid;

        private FrozenInfo(int uid, int pid, String processName) {
            this.mFrozenTimes = new ArrayList(16);
            this.mFrozenReasons = new ArrayList(16);
            this.mUid = uid;
            this.mPid = pid;
            this.mProcessName = processName;
        }

        public void addFrozenInfo(long curTime, String reason) {
            this.mFrozenTimes.add(Long.valueOf(curTime));
            this.mFrozenReasons.add(reason);
        }

        private long getStartTime() {
            if (this.mFrozenTimes.size() == 0) {
                return 0L;
            }
            return this.mFrozenTimes.get(0).longValue();
        }

        private long getEndTime() {
            return this.mThawTime;
        }

        public long getFrozenDuration() {
            if (getStartTime() < getEndTime()) {
                return getEndTime() - getStartTime();
            }
            return 0L;
        }

        public String toString() {
            return "(" + this.mPid + ")" + this.mProcessName + "/" + this.mUid;
        }
    }

    /* loaded from: classes.dex */
    public class H extends Handler {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public H(Looper looper) {
            super(looper);
            PowerFrozenManager.this = r1;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    try {
                        if (PowerFrozenManager.this.isFrozenPid(msg.arg2)) {
                            int uid = msg.arg1;
                            int pid = msg.arg2;
                            PowerFrozenManager.this.thawProcess(uid, pid, "Timeout pid " + pid);
                            return;
                        }
                        return;
                    } catch (Exception e) {
                        e.printStackTrace();
                        return;
                    }
                default:
                    return;
            }
        }
    }
}
