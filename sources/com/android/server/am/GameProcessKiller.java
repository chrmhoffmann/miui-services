package com.android.server.am;

import android.os.RemoteException;
import android.os.SystemClock;
import android.os.Trace;
import android.os.UserHandle;
import android.util.Slog;
import com.android.server.am.IGameProcessAction;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import miui.process.ForegroundInfo;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
/* loaded from: classes.dex */
public class GameProcessKiller implements IGameProcessAction {
    private static final boolean DEBUG = true;
    private static final String KILL_REASON = "game memory reclaim";
    private static final String TAG = GameProcessKiller.class.getSimpleName();
    private GameProcessKillerConfig mConfig;
    private GameMemoryReclaimer mReclaimer;

    public GameProcessKiller(GameMemoryReclaimer reclaimer, GameProcessKillerConfig cfg) {
        this.mReclaimer = reclaimer;
        this.mConfig = cfg;
    }

    private boolean isUidSystem(int uid) {
        return uid % 100000 < 10000;
    }

    public int getMinProcState() {
        return this.mConfig.mMinProcState;
    }

    int getPrio() {
        return this.mConfig.mPrio;
    }

    public boolean shouldSkip(int uid) {
        ForegroundInfo foregroundInfo;
        if (isUidSystem(uid)) {
            return true;
        }
        if (this.mConfig.mSkipActive && this.mReclaimer.mProcessPolicy.getActiveUidList(3).contains(Integer.valueOf(uid))) {
            return true;
        }
        try {
            foregroundInfo = this.mReclaimer.mProcessManager.getForegroundInfo();
        } catch (RemoteException e) {
            foregroundInfo = null;
        }
        return foregroundInfo == null || uid == foregroundInfo.mForegroundUid || uid == foregroundInfo.mMultiWindowForegroundUid;
    }

    @Override // com.android.server.am.IGameProcessAction
    public boolean shouldSkip(ProcessRecord proc) {
        if (!shouldSkip(proc.uid) && !this.mReclaimer.mProcessPolicy.isLockedApplication(proc.info.packageName, UserHandle.getCallingUserId()) && !this.mConfig.mWhiteList.contains(proc.info.packageName) && !this.mConfig.mWhiteList.contains(proc.processName)) {
            return this.mConfig.mSkipForeground && proc.getWindowProcessController().isInterestingToUser();
        }
        return true;
    }

    @Override // com.android.server.am.IGameProcessAction
    public long doAction(long need) {
        long reclaim = 0;
        int num = 0;
        Trace.traceBegin(524288L, "doKill:" + this.mConfig.mPrio);
        long time = SystemClock.uptimeMillis();
        List<PackageMemInfo> packageList = this.mReclaimer.filterPackageInfos(this);
        if (packageList != null && packageList.size() > 0) {
            Iterator<PackageMemInfo> it = packageList.iterator();
            long reclaim2 = 0;
            int num2 = 0;
            while (true) {
                if (!it.hasNext()) {
                    num = num2;
                    reclaim = reclaim2;
                    break;
                }
                PackageMemInfo pkg = it.next();
                String name = this.mReclaimer.getPackageNameByUid(pkg.mUid);
                synchronized (this.mReclaimer.mActivityManagerService) {
                    this.mReclaimer.mActivityManagerService.forceStopPackage(name, UserHandle.getCallingUserId(), KILL_REASON);
                }
                reclaim2 += pkg.mMemSize;
                num2++;
                if (reclaim2 >= need) {
                    num = num2;
                    reclaim = reclaim2;
                    break;
                }
            }
        }
        Slog.i(TAG, "kill " + num + " packages, and reclaim mem: " + reclaim + ", during " + (SystemClock.uptimeMillis() - time) + "ms");
        Trace.traceEnd(524288L);
        return reclaim;
    }

    /* loaded from: classes.dex */
    public static class PackageMemInfo {
        public long mMemSize;
        public int mState;
        public int mUid;

        public PackageMemInfo(int uid, long pss, int state) {
            this.mUid = uid;
            this.mMemSize = pss;
            this.mState = state;
        }

        public String toString() {
            return "uid=" + this.mUid + ", size=" + this.mMemSize + ", state=" + this.mState;
        }
    }

    /* loaded from: classes.dex */
    public static class GameProcessKillerConfig implements IGameProcessAction.IGameProcessActionConfig {
        private static final String CONFIG_PRIO = "prio";
        private static final int DEFAULT_PRIO = 0;
        private static final String MIN_PROC_STATE = "min-proc-state";
        private static final String SKIP_ACTIVE = "skip-active";
        private static final String SKIP_FOREGROUND = "skip-foreground";
        private static final String WHITE_LIST = "white-list";
        int mPrio = 0;
        List<String> mWhiteList = new ArrayList();
        int mMinProcState = 20;
        boolean mSkipForeground = true;
        boolean mSkipActive = true;

        public void addWhiteList(List<String> wl, boolean append) {
            if (append) {
                this.mWhiteList.removeAll(wl);
            } else {
                this.mWhiteList.clear();
            }
            this.mWhiteList.addAll(wl);
        }

        public void removeWhiteList(List<String> wl) {
            if (wl == null) {
                this.mWhiteList.clear();
            } else {
                this.mWhiteList.removeAll(wl);
            }
        }

        @Override // com.android.server.am.IGameProcessAction.IGameProcessActionConfig
        public void initFromJSON(JSONObject obj) throws JSONException {
            JSONArray tmpArray;
            try {
                if (obj.has(CONFIG_PRIO)) {
                    this.mPrio = obj.optInt(CONFIG_PRIO, 0);
                }
                if (obj.has(MIN_PROC_STATE)) {
                    this.mMinProcState = obj.optInt(MIN_PROC_STATE, 20);
                }
                if (obj.has(SKIP_ACTIVE)) {
                    this.mSkipActive = obj.optBoolean(SKIP_ACTIVE, true);
                }
                if (obj.has(SKIP_FOREGROUND)) {
                    this.mSkipForeground = obj.optBoolean(SKIP_FOREGROUND, true);
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

        public String toString() {
            return "prio=" + this.mPrio + ", minProcState=" + this.mMinProcState + ", skipForeground=" + this.mSkipForeground + ", skipActive=" + this.mSkipActive + ", whiteList=" + this.mWhiteList;
        }
    }
}
