package com.miui.server.smartpower;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.spc.PressureStateSettings;
import android.util.Slog;
import android.util.TimeUtils;
import com.android.internal.os.ProcessCpuTracker;
import com.android.server.am.ActivityManagerService;
import com.android.server.am.AppStateManager;
import com.android.server.am.ProcessCleanerBase;
import com.android.server.am.SystemPressureController;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
/* loaded from: classes.dex */
public class SmartCpuPolicyManager {
    private static final int CPU_EXCEPTION_HANDLE_THRESHOLD = 30000;
    private static final String CPU_EXCEPTION_KILL_REASON = "cpu exception";
    public static final boolean DEBUG = PressureStateSettings.DEBUG_ALL;
    public static final int DEFAULT_BACKGROUND_CPU_CORE_NUM = 4;
    private static final int MONITOR_CPU_MIN_TIME = 10000;
    static final boolean MONITOR_THREAD_CPU_USAGE = true;
    private static final int MSG_CPU_EXCEPTION = 7;
    public static final String TAG = "SmartPower.CpuPolicy";
    private ActivityManagerService mAMS;
    private H mHandler;
    private SmartPowerPolicyManager mSmartPowerPolicyManager;
    private AppStateManager mAppStateManager = null;
    private HandlerThread mHandlerTh = new HandlerThread("CpuPolicyTh", -2);
    private int mBackgroundCpuCoreNum = 4;
    private long mLashHandleCpuException = 0;
    private final AtomicLong mLastUpdateCpuTime = new AtomicLong(0);
    private final ProcessCpuTracker mProcessCpuTracker = new ProcessCpuTracker(true);

    public SmartCpuPolicyManager(Context context, ActivityManagerService ams) {
        this.mAMS = ams;
    }

    public void init(AppStateManager appStateManager, SmartPowerPolicyManager smartPowerPolicyManager) {
        this.mAppStateManager = appStateManager;
        this.mSmartPowerPolicyManager = smartPowerPolicyManager;
        this.mHandlerTh.start();
        this.mHandler = new H(this.mHandlerTh.getLooper());
    }

    public static boolean isEnableCpuLimit() {
        return false;
    }

    public int getBackgroundCpuCoreNum() {
        return this.mBackgroundCpuCoreNum;
    }

    public void updateBackgroundCpuCoreNum() {
        String bgCpu = SystemPressureController.getInstance().getBackgroundCpuPolicy().trim();
        String[] splCpu = bgCpu.split("-");
        if (splCpu.length >= 2) {
            this.mBackgroundCpuCoreNum = (Integer.parseInt(splCpu[1]) - Integer.parseInt(splCpu[0])) + 1;
        }
    }

    public void cpuPressureEvents(int level) {
    }

    public void cpuExceptionEvents(int type) {
        H h;
        if (PressureStateSettings.PROC_CPU_EXCEPTION_ENABLE && SystemClock.uptimeMillis() - this.mLashHandleCpuException > 30000 && (h = this.mHandler) != null && !h.hasMessages(7)) {
            Message msg = this.mHandler.obtainMessage(7);
            msg.arg1 = type;
            this.mHandler.sendMessage(msg);
        }
    }

    public void handleLimitCpuException(int type) {
        SmartCpuPolicyManager smartCpuPolicyManager = this;
        smartCpuPolicyManager.mLashHandleCpuException = SystemClock.uptimeMillis();
        long uptimeSince = updateCpuStatsNow();
        long lastCpuTime = 0;
        if (uptimeSince <= 0) {
            return;
        }
        Slog.d(TAG, "HandleLimitCpuException: type=" + type + " bgcpu=" + smartCpuPolicyManager.mBackgroundCpuCoreNum + " over:" + TimeUtils.formatDuration(uptimeSince));
        ArrayList<AppStateManager.AppState> appStateList = smartCpuPolicyManager.mAppStateManager.getAllAppState();
        Iterator<AppStateManager.AppState> it = appStateList.iterator();
        while (it.hasNext()) {
            AppStateManager.AppState appState = it.next();
            long backgroundUpdateTime = smartCpuPolicyManager.mLashHandleCpuException - appState.getLastTopTime();
            if (appState.isVsible()) {
                smartCpuPolicyManager = this;
            } else if (backgroundUpdateTime >= 30010) {
                Iterator<AppStateManager.AppState.RunningProcess> it2 = appState.getRunningProcessList().iterator();
                while (it2.hasNext()) {
                    AppStateManager.AppState.RunningProcess runningProc = it2.next();
                    long curCpuTime = runningProc.mCurCpuTime.get();
                    long lastCpuTime2 = runningProc.mLastCpuTime.get();
                    runningProc.mLastCpuTime.set(curCpuTime);
                    if (lastCpuTime2 <= 0) {
                        smartCpuPolicyManager = this;
                        lastCpuTime = 0;
                    } else if (!smartCpuPolicyManager.checkProcessRecord(runningProc)) {
                        smartCpuPolicyManager = this;
                        lastCpuTime = 0;
                    } else if (runningProc.isProcessPerceptible()) {
                        smartCpuPolicyManager = this;
                        lastCpuTime = 0;
                    } else {
                        ArrayList<AppStateManager.AppState> appStateList2 = appStateList;
                        Iterator<AppStateManager.AppState> it3 = it;
                        if (smartCpuPolicyManager.mSmartPowerPolicyManager.isProcessWhiteList(ProcessCleanerBase.SMART_POWER_PROTECT_APP_FLAGS, runningProc.getPackageName(), runningProc.getProcessName())) {
                            lastCpuTime = 0;
                            appStateList = appStateList2;
                            it = it3;
                        } else {
                            long j = curCpuTime - lastCpuTime2;
                            long lastCpuTime3 = smartCpuPolicyManager.mBackgroundCpuCoreNum;
                            long cpuTimeUsed = j / lastCpuTime3;
                            if ((((float) cpuTimeUsed) * 100.0f) / ((float) uptimeSince) >= PressureStateSettings.PROC_CPU_EXCEPTION_THRESHOLD) {
                                String reason = "cpu exception over " + TimeUtils.formatDuration(uptimeSince) + " used " + TimeUtils.formatDuration(cpuTimeUsed) + " (" + ((((float) cpuTimeUsed) * 100.0f) / ((float) uptimeSince)) + "%)";
                                SystemPressureController.getInstance().killProcess(runningProc.getProcessRecord(), 0, reason);
                            }
                            smartCpuPolicyManager = this;
                            lastCpuTime = 0;
                            appStateList = appStateList2;
                            it = it3;
                        }
                    }
                }
                smartCpuPolicyManager = this;
            }
        }
    }

    private void forAllCpuStats(Consumer<ProcessCpuTracker.Stats> consumer) {
        synchronized (this.mProcessCpuTracker) {
            int numOfStats = this.mProcessCpuTracker.countStats();
            for (int i = 0; i < numOfStats; i++) {
                consumer.accept(this.mProcessCpuTracker.getStats(i));
            }
        }
    }

    public long updateCpuStatsNow() {
        synchronized (this.mProcessCpuTracker) {
            long nowTime = SystemClock.uptimeMillis();
            if (nowTime - this.mLastUpdateCpuTime.get() < 10000) {
                return 0L;
            }
            Slog.d(TAG, "update process cpu time");
            updateBackgroundCpuCoreNum();
            this.mProcessCpuTracker.update();
            if (!this.mProcessCpuTracker.hasGoodLastStats()) {
                return 0L;
            }
            long uptimeSince = nowTime - this.mLastUpdateCpuTime.get();
            this.mLastUpdateCpuTime.set(nowTime);
            forAllCpuStats(new Consumer() { // from class: com.miui.server.smartpower.SmartCpuPolicyManager$$ExternalSyntheticLambda0
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    SmartCpuPolicyManager.this.m2359xde68e283((ProcessCpuTracker.Stats) obj);
                }
            });
            return uptimeSince;
        }
    }

    /* renamed from: lambda$updateCpuStatsNow$0$com-miui-server-smartpower-SmartCpuPolicyManager */
    public /* synthetic */ void m2359xde68e283(ProcessCpuTracker.Stats st) {
        AppStateManager.AppState.RunningProcess proc;
        if (!st.working || st.uid <= 1000 || st.pid <= 0 || (proc = this.mAppStateManager.getRunningProcess(st.uid, st.pid)) == null || proc.isKilled()) {
            return;
        }
        long curCpuTime = proc.mCurCpuTime.addAndGet(st.rel_utime + st.rel_stime);
        proc.mLastCpuTime.compareAndSet(0L, curCpuTime);
    }

    private boolean checkProcessRecord(AppStateManager.AppState.RunningProcess proc) {
        return proc != null && proc.getAdj() > 200 && proc.getPid() > 0 && UserHandle.isApp(proc.getUid()) && !proc.isKilled();
    }

    /* loaded from: classes.dex */
    public class H extends Handler {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public H(Looper looper) {
            super(looper);
            SmartCpuPolicyManager.this = r1;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 7:
                    try {
                        SmartCpuPolicyManager.this.handleLimitCpuException(msg.arg1);
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
