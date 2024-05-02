package com.android.server.am;

import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.RemoteException;
import android.os.Trace;
import android.os.UserHandle;
import android.os.spc.PressureStateSettings;
import android.util.Slog;
import com.android.server.am.AppStateManager;
import com.android.server.wm.WindowProcessUtils;
import com.miui.server.greeze.AurogonImmobulusMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import miui.process.ProcessConfig;
/* loaded from: classes.dex */
public abstract class ProcessCleanerBase {
    private static final String DESK_CLOCK_PROCESS_NAME = "com.android.deskclock";
    private static final String HOME_PROCESS_NAME = "com.miui.home";
    private static final String RADIO_PROCESS_NAME = "com.miui.fmservice:remote";
    private static final String RADIO_TURN_OFF_INTENT = "miui.intent.action.TURN_OFF";
    public static final int SMART_POWER_PROTECT_APP_FLAGS = 472;
    private static final String TAG = "ProcessCleanerBase";
    protected ActivityManagerService mAMS;
    protected SmartPowerService mSmartPowerService;

    public ProcessCleanerBase(ActivityManagerService ams) {
        this.mAMS = ams;
    }

    public void systemReady(Context context, ProcessManagerService pms) {
        this.mSmartPowerService = SmartPowerService.getInstance();
    }

    /* JADX WARN: Removed duplicated region for block: B:35:0x0067  */
    /* JADX WARN: Removed duplicated region for block: B:37:0x006f  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public void killOnce(com.android.server.am.ProcessRecord r15, int r16, java.lang.String r17, boolean r18, com.android.server.am.ProcessManagerService r19, java.lang.String r20, android.os.Handler r21, android.content.Context r22) {
        /*
            r14 = this;
            r7 = r14
            r8 = r15
            r9 = r16
            r10 = r19
            if (r8 != 0) goto L9
            return
        L9:
            r1 = 100
            com.android.server.am.ActivityManagerService r2 = r7.mAMS     // Catch: java.lang.Exception -> L92
            monitor-enter(r2)     // Catch: java.lang.Exception -> L92
            boolean r0 = r15.isPersistent()     // Catch: java.lang.Throwable -> L87
            if (r0 != 0) goto L3c
            com.android.server.am.ProcessStateRecord r0 = r8.mState     // Catch: java.lang.Throwable -> L87
            int r0 = r0.getSetAdj()     // Catch: java.lang.Throwable -> L87
            if (r0 < 0) goto L3c
            int r0 = r8.userId     // Catch: java.lang.Throwable -> L87
            boolean r0 = r14.isInWhiteListLock(r15, r0, r9, r10)     // Catch: java.lang.Throwable -> L87
            if (r0 == 0) goto L25
            goto L3c
        L25:
            boolean r0 = r14.isForceStopEnable(r15, r9, r10)     // Catch: java.lang.Throwable -> L87
            if (r0 == 0) goto L31
            if (r18 == 0) goto L31
            r0 = 104(0x68, float:1.46E-43)
            r11 = r0
            goto L63
        L31:
            r0 = 3
            if (r9 != r0) goto L38
            r0 = 102(0x66, float:1.43E-43)
            r11 = r0
            goto L63
        L38:
            r0 = 103(0x67, float:1.44E-43)
            r11 = r0
            goto L63
        L3c:
            com.android.server.am.ProcessStateRecord r0 = r8.mState     // Catch: java.lang.Throwable -> L87
            boolean r0 = r0.hasOverlayUi()     // Catch: java.lang.Throwable -> L87
            if (r0 != 0) goto L62
            com.android.server.am.ProcessStateRecord r0 = r8.mState     // Catch: java.lang.Throwable -> L87
            boolean r0 = r0.hasTopUi()     // Catch: java.lang.Throwable -> L87
            if (r0 != 0) goto L62
            com.android.server.am.ProcessStateRecord r0 = r8.mState     // Catch: java.lang.Throwable -> L87
            boolean r0 = r0.hasShownUi()     // Catch: java.lang.Throwable -> L87
            if (r0 != 0) goto L62
            android.content.pm.ApplicationInfo r0 = r8.info     // Catch: java.lang.Throwable -> L87
            java.lang.String r0 = r0.packageName     // Catch: java.lang.Throwable -> L87
            boolean r0 = r14.isTrimMemoryEnable(r0, r10)     // Catch: java.lang.Throwable -> L87
            if (r0 == 0) goto L62
            r0 = 101(0x65, float:1.42E-43)
            r11 = r0
            goto L63
        L62:
            r11 = r1
        L63:
            r0 = 101(0x65, float:1.42E-43)
            if (r11 <= r0) goto L6f
            r12 = r17
            r13 = r20
            r14.printKillLog(r15, r11, r12, r13)     // Catch: java.lang.Throwable -> L84
            goto L73
        L6f:
            r12 = r17
            r13 = r20
        L73:
            monitor-exit(r2)     // Catch: java.lang.Throwable -> L84
            r1 = r14
            r2 = r15
            r3 = r17
            r4 = r11
            r5 = r21
            r6 = r22
            r1.killOnce(r2, r3, r4, r5, r6)     // Catch: java.lang.Exception -> L81
            goto Lb0
        L81:
            r0 = move-exception
            r1 = r11
            goto L97
        L84:
            r0 = move-exception
            r1 = r11
            goto L8c
        L87:
            r0 = move-exception
            r12 = r17
            r13 = r20
        L8c:
            monitor-exit(r2)     // Catch: java.lang.Throwable -> L90
            throw r0     // Catch: java.lang.Exception -> L8e
        L8e:
            r0 = move-exception
            goto L97
        L90:
            r0 = move-exception
            goto L8c
        L92:
            r0 = move-exception
            r12 = r17
            r13 = r20
        L97:
            java.lang.String r2 = "ProcessCleanerBase"
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            r3.<init>()
            java.lang.String r4 = "killOnce:reason "
            java.lang.StringBuilder r3 = r3.append(r4)
            java.lang.StringBuilder r3 = r3.append(r0)
            java.lang.String r3 = r3.toString()
            android.util.Slog.d(r2, r3)
            r11 = r1
        Lb0:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.am.ProcessCleanerBase.killOnce(com.android.server.am.ProcessRecord, int, java.lang.String, boolean, com.android.server.am.ProcessManagerService, java.lang.String, android.os.Handler, android.content.Context):void");
    }

    public void killOnce(ProcessRecord app, String reason, int killLevel, Handler handler, Context context) {
        try {
            if (checkProcessDied(app) || checkKillFMApp(app, handler, context)) {
                return;
            }
            if (killLevel == 101) {
                trimMemory(app);
            } else if (killLevel == 102) {
                killBackgroundApplication(app, reason);
            } else if (killLevel == 103) {
                synchronized (this.mAMS) {
                    killApplicationLock(app, reason);
                }
            } else if (killLevel == 104) {
                forceStopPackage(app.info.packageName, app.userId, reason);
            }
        } catch (Exception e) {
            Slog.d(TAG, "killOnce:reason " + e);
        }
    }

    public boolean checkProcessDied(ProcessRecord proc) {
        boolean z;
        synchronized (this.mAMS) {
            if (proc != null) {
                try {
                    if (proc.getThread() != null && !proc.isKilled()) {
                        z = false;
                    }
                } finally {
                }
            }
            z = true;
        }
        return z;
    }

    private void printKillLog(ProcessRecord app, int killLevel, String reason, String killTag) {
        String str;
        StringBuilder sb;
        StringBuilder append = new StringBuilder().append(killLevelToString(killLevel));
        if (killLevel == 104) {
            sb = new StringBuilder().append(" pkgName=");
            str = app.info.packageName;
        } else {
            sb = new StringBuilder().append(" procName=");
            str = app.processName;
        }
        String levelString = append.append(sb.append(str).toString()).toString();
        String info = String.format("AS:%d%d", Integer.valueOf(app.mState.getCurAdj()), Integer.valueOf(app.mState.getCurProcState()));
        Slog.i(killTag, reason + ": " + levelString + " info=" + info);
    }

    boolean checkKillFMApp(final ProcessRecord proc, Handler handler, final Context context) {
        if (handler != null && proc.processName.equals(RADIO_PROCESS_NAME)) {
            final Intent intent = new Intent(RADIO_TURN_OFF_INTENT);
            intent.addFlags(268435456);
            handler.post(new Runnable() { // from class: com.android.server.am.ProcessCleanerBase.1
                @Override // java.lang.Runnable
                public void run() {
                    context.sendBroadcastAsUser(intent, new UserHandle(proc.userId));
                }
            });
            return true;
        }
        return false;
    }

    void removeAllTasks(int userId, ProcessManagerService pms) {
        long token = Binder.clearCallingIdentity();
        try {
            WindowProcessUtils.removeAllTasks(pms, userId, this.mAMS.mActivityTaskManager);
            if (userId == 0) {
                WindowProcessUtils.removeAllTasks(pms, 999, this.mAMS.mActivityTaskManager);
            }
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    void removeTasksIfNeeded(List<Integer> taskIdList, Set<Integer> whiteTaskSet, List<String> whiteList, List<Integer> whiteListTaskId, ProcessPolicy procPolicy) {
        long token = Binder.clearCallingIdentity();
        try {
            WindowProcessUtils.removeTasks(taskIdList, whiteTaskSet, procPolicy, this.mAMS.mActivityTaskManager, whiteList, whiteListTaskId);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    void removeTaskIfNeeded(int taskId) {
        long token = Binder.clearCallingIdentity();
        try {
            WindowProcessUtils.removeTask(taskId, this.mAMS.mActivityTaskManager);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public void removeTasksInPackages(List<String> packages, int userId, ProcessPolicy procPolicy) {
        long token = Binder.clearCallingIdentity();
        try {
            WindowProcessUtils.removeTasksInPackages(packages, userId, procPolicy, this.mAMS.mActivityTaskManager);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    public boolean isCurrentProcessInBackup(AppStateManager.AppState.RunningProcess runningProc) {
        return isCurrentProcessInBackup(runningProc.getPackageName(), runningProc.getProcessName());
    }

    public boolean isCurrentProcessInBackup(String packageName, String processName) {
        if (this.mSmartPowerService.isProcessWhiteList(64, packageName, processName)) {
            return true;
        }
        return false;
    }

    public boolean isTrimMemoryEnable(String packageName, ProcessManagerService pms) {
        return pms.isTrimMemoryEnable(packageName);
    }

    public boolean isInWhiteListLock(ProcessRecord app, int userId, int policy, ProcessManagerService pms) {
        return pms.isInWhiteList(app, userId, policy);
    }

    boolean isForceStopEnable(ProcessRecord app, int policy, ProcessManagerService pms) {
        return pms.isForceStopEnable(app, policy);
    }

    public boolean isAudioOrGPSApp(int uid) {
        return SystemPressureController.getInstance().isAudioOrGPSApp(uid);
    }

    boolean isAudioOrGPSProc(int uid, int pid) {
        return SystemPressureController.getInstance().isAudioOrGPSProc(uid, pid);
    }

    void trimMemory(ProcessRecord app) {
        if (app.getWindowProcessController().isInterestingToUser()) {
            return;
        }
        if (app.info.packageName.equals("android")) {
            scheduleTrimMemory(app, 60);
        } else {
            scheduleTrimMemory(app, 80);
        }
    }

    void scheduleTrimMemory(ProcessRecord app, int level) {
        synchronized (this.mAMS) {
            if (!checkProcessDied(app)) {
                try {
                    app.getThread().scheduleTrimMemory(level);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void forceStopPackage(String packageName, int userId, String reason) {
        Trace.traceBegin(64L, "forceStopPackage pck:" + packageName + " r:" + reason);
        this.mAMS.forceStopPackage(packageName, userId, reason);
        Trace.traceEnd(64L);
    }

    public void killApplicationLock(ProcessRecord app, String reason) {
        Trace.traceBegin(64L, "killLocked pid:" + app.getPid() + " r:" + reason);
        app.killLocked(reason, 13, true);
        Trace.traceEnd(64L);
    }

    void killBackgroundApplication(ProcessRecord app, String reason) {
        Trace.traceBegin(64L, "killBackgroundProcesses pid:" + app.getPid() + " r:" + reason);
        this.mAMS.killBackgroundProcesses(app.info.packageName, app.userId, reason);
        Trace.traceEnd(64L);
    }

    public void removeTasksByPolicy(ProcessConfig config, ActivityManagerService mAMS, ProcessPolicy mProcessPolicy, ProcessManagerService mPMS) {
        int policy = config.getPolicy();
        switch (policy) {
            case 1:
            case 4:
            case 14:
            case 15:
            case 16:
            case 20:
                removeTasksForKillAll(config, mProcessPolicy);
                return;
            case 2:
                int userId = UserHandle.getCallingUserId();
                removeAllTasks(userId, mPMS);
                return;
            case 5:
                removeTasksIfNeeded(config, mProcessPolicy, null, null);
                return;
            case 7:
                removeTaskIfNeeded(config.getTaskId());
                return;
            default:
                return;
        }
    }

    void removeTasksForKillAll(ProcessConfig config, ProcessPolicy mProcessPolicy) {
        int policy = config.getPolicy();
        Map<Integer, String> fgTaskPackageMap = WindowProcessUtils.getPerceptibleRecentAppList(this.mAMS.mActivityTaskManager);
        List<String> whiteList = config.getWhiteList();
        if (whiteList == null) {
            whiteList = new ArrayList();
        }
        if (policy == 14 || policy == 16 || policy == 15) {
            whiteList.add(DESK_CLOCK_PROCESS_NAME);
        } else if (policy == 1) {
            whiteList.add("com.miui.home");
        }
        removeTasksIfNeeded(config, mProcessPolicy, whiteList, fgTaskPackageMap);
    }

    void removeTasksIfNeeded(ProcessConfig config, ProcessPolicy mProcessPolicy, List<String> policyWhiteList, Map<Integer, String> fgTaskPackageMap) {
        List<String> whiteList = policyWhiteList == null ? config.getWhiteList() : policyWhiteList;
        List<Integer> whiteListTaskId = config.getWhiteListTaskId();
        if (config.isRemoveTaskNeeded() && config.getRemovingTaskIdList() != null) {
            Set<Integer> fgTaskIdSet = fgTaskPackageMap != null ? fgTaskPackageMap.keySet() : null;
            if (config.getPolicy() == 1) {
                List<Integer> taskList = WindowProcessUtils.getAllTaskIdList(this.mAMS.mActivityTaskManager);
                removeTasksIfNeeded(taskList, fgTaskIdSet, whiteList, whiteListTaskId, mProcessPolicy);
            } else {
                removeTasksIfNeeded(config.getRemovingTaskIdList(), fgTaskIdSet, whiteList, whiteListTaskId, mProcessPolicy);
            }
        }
        if (whiteList != null) {
            if (fgTaskPackageMap != null) {
                whiteList.addAll(fgTaskPackageMap.values());
            }
            if (PressureStateSettings.DEBUG_ALL) {
                printWhiteList(config, whiteList);
            }
            mProcessPolicy.addWhiteList(8, whiteList, false);
        }
    }

    void printWhiteList(ProcessConfig config, List<String> whiteList) {
        String info = "reason=" + getKillReason(config.getPolicy()) + " whiteList=";
        for (int i = 0; i < whiteList.size(); i++) {
            info = info + whiteList.get(i) + " ";
        }
        Slog.d(TAG, info);
    }

    public String getKillReason(int policy) {
        switch (policy) {
            case 1:
                return ProcessPolicy.REASON_ONE_KEY_CLEAN;
            case 2:
                return ProcessPolicy.REASON_FORCE_CLEAN;
            case 3:
                return ProcessPolicy.REASON_LOCK_SCREEN_CLEAN;
            case 4:
                return ProcessPolicy.REASON_GAME_CLEAN;
            case 5:
                return ProcessPolicy.REASON_OPTIMIZATION_CLEAN;
            case 6:
                return ProcessPolicy.REASON_GARBAGE_CLEAN;
            case 7:
                return ProcessPolicy.REASON_SWIPE_UP_CLEAN;
            case 8:
            case 9:
            case 15:
            case 17:
            case 18:
            case 21:
            default:
                return ProcessPolicy.REASON_UNKNOWN;
            case 10:
                return ProcessPolicy.REASON_USER_DEFINED;
            case 11:
                return ProcessPolicy.REASON_AUTO_POWER_KILL;
            case 12:
                return ProcessPolicy.REASON_AUTO_THERMAL_KILL;
            case 13:
                return ProcessPolicy.REASON_AUTO_IDLE_KILL;
            case 14:
                return ProcessPolicy.REASON_AUTO_SLEEP_CLEAN;
            case 16:
                return ProcessPolicy.REASON_AUTO_SYSTEM_ABNORMAL_CLEAN;
            case 19:
                return ProcessPolicy.REASON_AUTO_THERMAL_KILL_ALL_LEVEL_1;
            case 20:
                return ProcessPolicy.REASON_AUTO_THERMAL_KILL_ALL_LEVEL_2;
            case 22:
                return ProcessPolicy.REASON_SCREEN_OFF_CPU_CHECK_KILL;
        }
    }

    String killLevelToString(int level) {
        switch (level) {
            case 100:
                return "none";
            case 101:
                return "trim-memory";
            case 102:
                return "kill-background";
            case AurogonImmobulusMode.MSG_REPEAT_FREEZE_APP /* 103 */:
                return "kill";
            case AurogonImmobulusMode.MSG_REMOVE_ALL_MESSAGE /* 104 */:
                return "force-stop";
            default:
                return "";
        }
    }
}
