package com.android.server.am;

import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.Trace;
import android.util.Slog;
import com.android.server.am.IGameProcessAction;
import com.android.server.wm.ScreenRotationAnimationImpl;
import java.util.ArrayList;
import java.util.List;
import miui.process.ForegroundInfo;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
/* loaded from: classes.dex */
public class GameProcessCompactor implements IGameProcessAction {
    private static final boolean DEBUG = true;
    private static final String TAG = GameProcessCompactor.class.getSimpleName();
    private GameProcessCompactorConfig mConfig;
    private GameMemoryReclaimer mReclaimer;

    public GameProcessCompactor(GameMemoryReclaimer reclaimer, GameProcessCompactorConfig cfg) {
        this.mReclaimer = reclaimer;
        this.mConfig = cfg;
    }

    @Override // com.android.server.am.IGameProcessAction
    public boolean shouldSkip(ProcessRecord proc) {
        ForegroundInfo foregroundInfo;
        if (!this.mConfig.mSkipActive || !this.mReclaimer.mProcessPolicy.getActiveUidList(1).contains(Integer.valueOf(proc.uid))) {
            try {
                foregroundInfo = this.mReclaimer.mProcessManager.getForegroundInfo();
            } catch (RemoteException e) {
                foregroundInfo = null;
            }
            if (foregroundInfo == null || proc.uid == foregroundInfo.mForegroundUid || proc.uid == foregroundInfo.mMultiWindowForegroundUid || this.mConfig.mWhiteList.contains(proc.info.packageName) || this.mConfig.mWhiteList.contains(proc.processName) || proc.mState.getSetAdj() <= this.mConfig.mMinAdj || proc.mState.getSetAdj() > this.mConfig.mMaxAdj) {
                return true;
            }
            return this.mConfig.mSkipForeground && proc.getWindowProcessController().isInterestingToUser();
        }
        return true;
    }

    @Override // com.android.server.am.IGameProcessAction
    public long doAction(long need) {
        int num = 0;
        Trace.traceBegin(524288L, "doCompact:" + this.mConfig.mPrio);
        long time = SystemClock.uptimeMillis();
        long reclaim = 0;
        List<ProcessCompactInfo> pidList = this.mReclaimer.filterProcessInfos(this);
        if (pidList != null && pidList.size() > 0) {
            for (ProcessCompactInfo compInfo : pidList) {
                long income = compInfo.compact(this);
                if (income >= 0) {
                    num++;
                    reclaim += income;
                    if (reclaim >= need) {
                        break;
                    } else if (this.mConfig.mCompactMaxNum > 0 && num >= this.mConfig.mCompactMaxNum) {
                        break;
                    }
                }
            }
        }
        Slog.i(TAG, "compact " + num + " processes, and reclaim mem: " + reclaim + ", during " + (SystemClock.uptimeMillis() - time) + "ms");
        Trace.traceEnd(524288L);
        return reclaim;
    }

    long getIntervalThreshold() {
        return this.mConfig.mCompactIntervalThreshold;
    }

    long getIncomePctThreshold() {
        return this.mConfig.mCompactIncomePctThreshold;
    }

    int getPrio() {
        return this.mConfig.mPrio;
    }

    /* loaded from: classes.dex */
    public static class ProcessCompactInfo {
        private static final String COMPACT_ACTION_FULL = "all";
        private boolean mDied = false;
        private long mLastCompactTime;
        private int mLastIncomePct;
        private int mPid;

        public ProcessCompactInfo(int pid) {
            this.mPid = pid;
        }

        private boolean shouldCompact(GameProcessCompactor compactor) {
            return this.mDied || SystemClock.uptimeMillis() - this.mLastCompactTime > compactor.getIntervalThreshold() * 1000 || ((long) this.mLastIncomePct) > compactor.getIncomePctThreshold();
        }

        long compact(GameProcessCompactor compactor) {
            if (!shouldCompact(compactor)) {
                return -1L;
            }
            long[] rssBefore = Process.getRss(this.mPid);
            if (rssBefore[0] <= 0) {
                return -1L;
            }
            SystemPressureControllerStub.getInstance().performCompaction(COMPACT_ACTION_FULL, this.mPid);
            long[] rssAfter = Process.getRss(this.mPid);
            long income = rssBefore[0] - rssAfter[0];
            this.mLastIncomePct = (int) ((100 * income) / rssBefore[0]);
            this.mLastCompactTime = SystemClock.uptimeMillis();
            Slog.d(GameProcessCompactor.TAG, "compact " + this.mPid + ", reclaim mem: " + income + ", income pct:" + this.mLastIncomePct);
            return income;
        }

        public void notifyDied() {
            this.mDied = true;
        }
    }

    /* loaded from: classes.dex */
    public static class GameProcessCompactorConfig implements IGameProcessAction.IGameProcessActionConfig {
        private static final String COMPACTOR_INCOME_PCT = "compactor-income-pct";
        private static final String COMPACTOR_INTERVAL = "compactor-interval";
        private static final String COMPACT_MAX_NUM = "compact-max-num";
        private static final String CONFIG_PRIO = "prio";
        private static final int DEFAULT_MAX_NUM = -1;
        private static final int DEFAULT_PRIO = 0;
        private static final String MAX_ADJ = "max-adj";
        private static final String MIN_ADJ = "min-adj";
        private static final String SKIP_ACTIVE = "skip-active";
        private static final String SKIP_FOREGROUND = "skip-foreground";
        private static final String WHITE_LIST = "white-list";
        int mPrio = 0;
        int mMinAdj = ScreenRotationAnimationImpl.BLACK_SURFACE_INVALID_POSITION;
        int mMaxAdj = 1001;
        List<String> mWhiteList = new ArrayList();
        boolean mSkipForeground = true;
        boolean mSkipActive = true;
        int mCompactIntervalThreshold = 0;
        int mCompactIncomePctThreshold = 0;
        int mCompactMaxNum = -1;

        @Override // com.android.server.am.IGameProcessAction.IGameProcessActionConfig
        public void initFromJSON(JSONObject obj) throws JSONException {
            JSONArray tmpArray;
            try {
                if (obj.has(CONFIG_PRIO)) {
                    this.mPrio = obj.optInt(CONFIG_PRIO, 0);
                }
                if (obj.has(MIN_ADJ)) {
                    this.mMinAdj = obj.optInt(MIN_ADJ, ScreenRotationAnimationImpl.BLACK_SURFACE_INVALID_POSITION);
                }
                if (obj.has(MAX_ADJ)) {
                    this.mMaxAdj = obj.optInt(MAX_ADJ, 1001);
                }
                if (obj.has(COMPACTOR_INTERVAL)) {
                    this.mCompactIntervalThreshold = obj.optInt(COMPACTOR_INTERVAL, 0);
                }
                if (obj.has(COMPACTOR_INCOME_PCT)) {
                    this.mCompactIncomePctThreshold = obj.optInt(COMPACTOR_INCOME_PCT, 0);
                }
                if (obj.has(SKIP_ACTIVE)) {
                    this.mSkipActive = obj.optBoolean(SKIP_ACTIVE, true);
                }
                if (obj.has(SKIP_FOREGROUND)) {
                    this.mSkipForeground = obj.optBoolean(SKIP_FOREGROUND, true);
                }
                if (obj.has(COMPACT_MAX_NUM)) {
                    this.mCompactMaxNum = obj.optInt(COMPACT_MAX_NUM, -1);
                }
                if (obj.has(WHITE_LIST) && (tmpArray = obj.optJSONArray(WHITE_LIST)) != null) {
                    for (int i = 0; i < tmpArray.length(); i++) {
                        if (!tmpArray.isNull(i)) {
                            this.mWhiteList.add(tmpArray.getString(i));
                        }
                    }
                }
            } catch (JSONException e) {
                throw e;
            }
        }

        @Override // com.android.server.am.IGameProcessAction.IGameProcessActionConfig
        public int getPrio() {
            return this.mPrio;
        }

        public void addWhiteList(List<String> wl, boolean append) {
            if (append) {
                this.mWhiteList.removeAll(wl);
            } else {
                this.mWhiteList.clear();
            }
            this.mWhiteList.addAll(wl);
        }

        public String toString() {
            return "prio=" + this.mPrio + ", minAdj=" + this.mMinAdj + ", maxAdj=" + this.mMaxAdj + ", compactInterval=" + this.mCompactIntervalThreshold + ", compactIncomePct=" + this.mCompactIncomePctThreshold + ", skipForeground=" + this.mSkipForeground + ", skipActive=" + this.mSkipActive + ", whiteList=" + this.mWhiteList;
        }
    }
}
