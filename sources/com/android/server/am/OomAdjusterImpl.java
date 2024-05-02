package com.android.server.am;

import android.os.Process;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Slog;
import com.android.server.ScoutHelper;
import com.android.server.wm.RealTimeModeControllerImpl;
import com.miui.base.MiuiStubRegistry;
import com.miui.base.MiuiStubUtil;
import com.miui.server.rtboost.SchedBoostService;
import com.miui.server.smartpower.SmartPowerPolicyManager;
import java.util.ArrayList;
import java.util.List;
import miui.process.ProcessManager;
/* loaded from: classes.dex */
public class OomAdjusterImpl extends OomAdjusterStub {
    private static final int GAME_APP_BACKGROUND_STATE = 2;
    private static final int GAME_APP_TOP_TO_BACKGROUND_STATE = 4;
    private static final int GAME_SCENE_DEFAULT_STATE = 0;
    private static final int GAME_SCENE_DOWNLOAD_STATE = 2;
    private static final int GAME_SCENE_PLAYING_STATE = 3;
    private static final int GAME_SCENE_WATCHING_STATE = 4;
    private static final int MAX_GAME_DOWNLOAD_TIME = 1200000;
    private static final int MAX_PREVIOUS_GAME_TIME = 600000;
    private static final int MAX_PREVIOUS_PROTECT_TIME = 1200000;
    private static final int MAX_PREVIOUS_TIME = 300000;
    private static final String PACKAGE_NAME_CAMERA = "com.android.camera";
    private static final String PACKAGE_NAME_GALLERY = "com.miui.gallery";
    private static final int PREVIOUS_APP_CRITICAL_ADJ = 701;
    private static final int PREVIOUS_APP_MAJOR_ADJ = 702;
    private static final int PREVIOUS_PROTECT_CRITICAL_COUNT = 1;
    private static List<String> sCameraImproveOomAdjList;
    private static List<String> skipMoveCgroupList;
    private String mForegroundPkg = "";
    public static boolean LIMIT_BIND_VEISIBLE_ENABLED = SystemProperties.getBoolean("persist.sys.spc.bindvisible.enabled", true);
    private static final boolean UNTRUSTEDAPP_BG_ENABLED = SystemProperties.getBoolean("persist.sys.miui.camera.inhibit_procs.enable", false);
    private static RunningAppRecordMap mRunningGameMap = new RunningAppRecordMap();
    private static String QQ_PLAYER = "com.tencent.qqmusic:QQPlayerService";

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<OomAdjusterImpl> {

        /* compiled from: OomAdjusterImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final OomAdjusterImpl INSTANCE = new OomAdjusterImpl();
        }

        public OomAdjusterImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public OomAdjusterImpl provideNewInstance() {
            return new OomAdjusterImpl();
        }
    }

    static {
        ArrayList arrayList = new ArrayList();
        sCameraImproveOomAdjList = arrayList;
        arrayList.add("com.tencent.mm");
        ArrayList arrayList2 = new ArrayList();
        skipMoveCgroupList = arrayList2;
        arrayList2.add("com.tencent.mm");
        skipMoveCgroupList.add("com.tencent.mobileqq");
    }

    public static OomAdjusterImpl getInstance() {
        return (OomAdjusterImpl) MiuiStubUtil.getInstance(OomAdjusterStub.class);
    }

    private final boolean computePreviousAdj(ProcessRecord app, int procState) {
        if (!SmartPowerPolicyManager.sCtsModeEnable && app.hasActivities() && !Process.isIsolated(app.uid)) {
            ProcessStateRecord appState = app.mState;
            long backgroundTime = SystemClock.uptimeMillis() - appState.getLastTopTime();
            if (backgroundTime > 1200000) {
                return false;
            }
            if (computeOomAdjForGameApp(app, procState)) {
                return true;
            }
            if (backgroundTime <= 300000) {
                modifyProcessRecordAdj(appState, false, PREVIOUS_APP_MAJOR_ADJ, procState, "previous-improveAdj");
                return true;
            }
        }
        return false;
    }

    private final boolean computeWidgetAdj(ProcessRecord app) {
        if (app.processName != null && app.processName.endsWith(":widgetProvider")) {
            ProcessStateRecord prcState = app.mState;
            modifyProcessRecordAdj(prcState, prcState.isCached(), 999, 19, "widget-degenerate");
            return true;
        }
        return false;
    }

    private boolean improveOomAdjForCamera(ProcessRecord app, ProcessRecord topApp, int adj) {
        if (topApp != null && app.processName != null && adj >= 800) {
            if ((TextUtils.equals(topApp.processName, "com.android.camera") || TextUtils.equals(topApp.processName, "com.miui.gallery")) && sCameraImproveOomAdjList.contains(app.processName)) {
                ProcessStateRecord prcState = app.mState;
                modifyProcessRecordAdj(prcState, prcState.isCached(), 400, 13, "camera-improveAdj");
                return true;
            }
            return false;
        }
        return false;
    }

    private boolean computeOomAdjForGameApp(ProcessRecord app, int procState) {
        ProcessStateRecord appState = app.mState;
        long backgroundTime = SystemClock.uptimeMillis() - appState.getLastTopTime();
        if (!SystemPressureController.getInstance().isGameApp(app.info.packageName)) {
            return false;
        }
        if (backgroundTime <= 600000) {
            mRunningGameMap.size();
            int gameIndex = mRunningGameMap.getIndexForKey(app.info.packageName);
            if (mRunningGameMap.size() > 1 && gameIndex >= 1) {
                return false;
            }
            modifyProcessRecordAdj(appState, false, PREVIOUS_APP_CRITICAL_ADJ, procState, "game-improveAdj");
            return true;
        } else if (backgroundTime <= 1200000) {
            int gameScene = mRunningGameMap.getForKey(app.info.packageName).intValue();
            switch (gameScene) {
                case 2:
                    modifyProcessRecordAdj(appState, false, PREVIOUS_APP_MAJOR_ADJ, procState, "game-improveAdj(download)");
                    return true;
                case 3:
                case 4:
                default:
                    return false;
            }
        } else {
            mRunningGameMap.removeForKey(app.info.packageName);
            return false;
        }
    }

    private void improveOomAdjForAudioProcess(ProcessRecord app) {
        if (QQ_PLAYER.equals(app.processName)) {
            ProcessStateRecord prcState = app.mState;
            if (SystemPressureController.getInstance().isAudioOrGPSProc(app.uid, app.getPid()) && !app.mServices.hasForegroundServices()) {
                prcState.setMaxAdj((int) ScoutHelper.BINDER_FULL_KILL_SCORE_ADJ_MIN);
            } else {
                prcState.setMaxAdj(ProcessManager.LOCKED_MAX_ADJ);
            }
        }
    }

    private void modifyProcessRecordAdj(ProcessStateRecord prcState, boolean cached, int CurRawAdj, int procState, String AdjType) {
        prcState.setCached(cached);
        prcState.setCurRawAdj(CurRawAdj);
        prcState.setCurRawProcState(procState);
        prcState.setAdjType(AdjType);
    }

    public boolean computeOomAdjLocked(ProcessRecord app, ProcessRecord topApp, int adj, int procState, boolean cycleReEval) {
        if (ActivityManagerDebugConfig.DEBUG_OOM_ADJ) {
            String isTop = app == topApp ? "true" : "false";
            Slog.i("ActivityManager", "computeOomAdjLocked processName = " + app.processName + " rawAdj=" + adj + " maxAdj=" + app.mState.getMaxAdj() + " procState=" + procState + " maxProcState=" + app.mState.mMaxProcState + " adjType=" + app.mState.getAdjType() + " isTop=" + isTop + " cycleReEval=" + cycleReEval);
        }
        boolean isChangeAdj = false;
        if (adj <= 0) {
            return false;
        }
        if (computeWidgetAdj(app) || improveOomAdjForCamera(app, topApp, adj)) {
            isChangeAdj = true;
        }
        if (isChangeAdj) {
            return true;
        }
        if (!app.isKilled() && !app.isKilledByAm() && app.getThread() != null) {
            switch (procState) {
                case 10:
                case 15:
                case 16:
                case 17:
                case 18:
                    if (adj >= 1001 || app.mServices.hasAboveClient()) {
                        isChangeAdj = computePreviousAdj(app, procState);
                        break;
                    }
                    break;
            }
            improveOomAdjForAudioProcess(app);
        }
        return isChangeAdj;
    }

    public void applyOomAdjLocked(ProcessRecord app) {
        SmartPowerService.getInstance().applyOomAdjLocked(app);
    }

    public int computeBindServiceAdj(ProcessRecord app, int adj, ConnectionRecord connectService) {
        ProcessRecord client = connectService.binding.client;
        ProcessStateRecord cstate = client.mState;
        if (!LIMIT_BIND_VEISIBLE_ENABLED) {
            return Math.max(cstate.getCurRawAdj(), 100);
        }
        int clientAdj = cstate.getCurRawAdj();
        boolean isSystem = (app.info.flags & 129) != 0;
        if ((connectService.flags & 100663296) != 0 || clientAdj < 0) {
            if (isSystem) {
                return Math.max(clientAdj, 100);
            }
            return Math.max(clientAdj, (int) ScoutHelper.BINDER_FULL_KILL_SCORE_ADJ_MIN);
        } else if ((connectService.flags & 1) != 0 && clientAdj < 200) {
            if (isProcessPerceptible(client.uid, client.processName) || SmartPowerPolicyManager.sCtsModeEnable) {
                return 250;
            }
            return adj;
        } else {
            return adj;
        }
    }

    public static boolean isCacheProcessState(int procState) {
        return procState == 16 || procState == 17 || procState == 18;
    }

    public void compactBackgroundProcess(ProcessRecord proc) {
        ProcessStateRecord state = proc.mState;
        if (state.getCurProcState() != state.getSetProcState()) {
            int oldState = state.getSetProcState();
            int newState = state.getCurProcState();
            if (!isCacheProcessState(oldState) && isCacheProcessState(newState)) {
                SystemPressureController.getInstance().compactBackgroundProcess(proc.uid, proc.processName);
            }
        }
    }

    public boolean isSetUntrustedBgGroup(ProcessRecord app) {
        if (UNTRUSTEDAPP_BG_ENABLED && !app.info.isSystemApp() && !skipMoveCgroupList.contains(app.processName)) {
            return true;
        }
        return false;
    }

    public void updateGameSceneRecordMap(String packageName, int gameScene, int appState) {
        switch (appState) {
            case 2:
            case 4:
                mRunningGameMap.put(packageName, Integer.valueOf(gameScene));
                return;
            case 3:
            default:
                return;
        }
    }

    public void setProperThreadPriority(ProcessRecord app, int pid, int renderThreadTid, int prio) {
        boolean boosted = RealTimeModeControllerImpl.get().checkThreadBoost(pid);
        if (!boosted) {
            super.setProperThreadPriority(app, pid, renderThreadTid, prio);
            return;
        }
        RealTimeModeControllerImpl.get().setThreadSavedPriority(new int[]{pid, renderThreadTid}, prio);
        Slog.d(SchedBoostService.TAG, app.processName + " already boosted, skip boost priority");
    }

    public void resetProperThreadPriority(ProcessRecord app, int tid, int prio) {
        boolean boosted = RealTimeModeControllerImpl.get().checkThreadBoost(tid);
        if (tid != 0 && !boosted) {
            super.resetProperThreadPriority(app, tid, prio);
            return;
        }
        RealTimeModeControllerImpl.get().setThreadSavedPriority(new int[]{tid}, prio);
        Slog.d(SchedBoostService.TAG, app.processName + " boosting, skip reset priority");
    }

    public void foregroundActivityChanged(String packageName) {
        if (!this.mForegroundPkg.equals(packageName)) {
            if (SystemPressureController.getInstance().isGameApp(packageName)) {
                mRunningGameMap.put(0, packageName, 0);
            }
            this.mForegroundPkg = packageName;
        }
    }

    public void notifyProcessDied(ProcessRecord app) {
        if (app.info.packageName.equals(app.processName)) {
            mRunningGameMap.removeForKey(app.info.packageName);
        }
    }

    /* loaded from: classes.dex */
    public static class RunningAppRecordMap {
        private final List<String> mList;
        private final ArrayMap<String, Integer> mMap;

        private RunningAppRecordMap() {
            this.mMap = new ArrayMap<>();
            this.mList = new ArrayList();
        }

        public Integer getForKey(String packageName) {
            Integer value = this.mMap.get(packageName);
            return Integer.valueOf(value != null ? value.intValue() : -1);
        }

        public int getIndexForKey(String packageName) {
            return this.mList.indexOf(packageName);
        }

        public void put(String packageName, Integer gameScence) {
            if (!this.mList.contains(packageName)) {
                this.mList.add(packageName);
            }
            this.mMap.put(packageName, gameScence);
        }

        public void put(int index, String packageName, Integer gameScence) {
            if (this.mList.contains(packageName)) {
                this.mList.remove(packageName);
            }
            this.mList.add(index, packageName);
            this.mMap.put(packageName, gameScence);
        }

        public void removeForKey(String packageName) {
            this.mMap.remove(packageName);
            this.mList.remove(packageName);
        }

        public void clear() {
            this.mMap.clear();
            this.mList.clear();
        }

        public List<String> getList() {
            return this.mList;
        }

        public int size() {
            return this.mList.size();
        }
    }

    private boolean isProcessPerceptible(int uid, String processName) {
        return SmartPowerService.getInstance().isProcessPerceptible(uid, processName);
    }
}
