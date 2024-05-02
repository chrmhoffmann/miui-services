package com.miui.server.greeze;

import android.app.ActivityManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.Trace;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import android.util.Singleton;
import android.util.Slog;
import android.util.SparseArray;
import com.android.server.am.ActivityManagerService;
import com.android.server.inputmethod.InputMethodManagerServiceImpl;
import com.miui.server.SplashScreenServiceDelegate;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import miui.greeze.IGreezeCallback;
import miui.process.ForegroundInfo;
import miui.process.ProcessManager;
/* loaded from: classes.dex */
public class PerfService extends IGreezeCallback.Stub {
    public static final int BINDER_STATE_IN_BUSY = 1;
    public static final int BINDER_STATE_IN_IDLE = 0;
    public static final int BINDER_STATE_IN_TRANSACTION = 4;
    public static final int BINDER_STATE_PROC_IN_BUSY = 3;
    public static final int BINDER_STATE_THREAD_IN_BUSY = 2;
    private static final String SERVICE_NAME = "greezer";
    private static String TAG = "GreezeManager:PerfService";
    private static final String[] WHITELIST_PKG = {InputMethodManagerServiceImpl.MIUI_HOME, SplashScreenServiceDelegate.SPLASHSCREEN_PACKAGE, "com.xiaomi.xmsf", "com.miui.hybrid", "com.android.providers.media.module", "com.google.android.providers.media.module"};
    private static final Singleton<PerfService> singleton = new Singleton<PerfService>() { // from class: com.miui.server.greeze.PerfService.1
        public PerfService create() {
            return new PerfService();
        }
    };
    private ActivityManagerService mAms;
    private Method mGetCastPid;
    private GreezeManagerService mService;

    private PerfService() {
        this.mService = GreezeManagerService.getService();
        this.mAms = ActivityManager.getService();
    }

    public static PerfService getInstance() {
        return (PerfService) singleton.get();
    }

    /* loaded from: classes.dex */
    class GzLaunchBoostRunnable implements Runnable {
        Bundle bundle;

        public GzLaunchBoostRunnable(Bundle bundle) {
            PerfService.this = this$0;
            this.bundle = bundle;
        }

        @Override // java.lang.Runnable
        public void run() {
            int i;
            int castPid;
            long startTime;
            SparseArray<List<RunningProcess>> uidMap;
            int launchinguid;
            int fzCount;
            Set<Integer> dynamicWhiteList;
            String skipReason;
            int i2;
            int castPid2;
            int launchinguid2 = this.bundle.getInt("uid");
            String launchingActivity = this.bundle.getString("launchingActivity");
            int fromUid = this.bundle.getInt("fromUid");
            long startTime2 = SystemClock.uptimeMillis();
            Trace.traceBegin(64L, "GzBoost");
            if (GreezeManagerService.DEBUG) {
                Slog.d(PerfService.TAG, "GzBoost " + launchingActivity + ", start");
            }
            int castPid3 = -1;
            try {
                if (PerfService.this.mGetCastPid != null) {
                    castPid3 = ((Integer) PerfService.this.mGetCastPid.invoke(PerfService.this.mAms, new Object[0])).intValue();
                }
            } catch (IllegalAccessException e) {
                Slog.w(PerfService.TAG, "Failed to get cast pid", e);
            } catch (InvocationTargetException e2) {
                Slog.w(PerfService.TAG, "Failed to get cast pid", e2);
            }
            Set<Integer> dynamicWhiteList2 = new ArraySet<>();
            dynamicWhiteList2.addAll(GreezeServiceUtils.getAudioUid());
            dynamicWhiteList2.addAll(GreezeServiceUtils.getIMEUid());
            try {
                ForegroundInfo foregroundInfo = ProcessManager.getForegroundInfo();
                dynamicWhiteList2.add(Integer.valueOf(foregroundInfo.mForegroundUid));
                dynamicWhiteList2.add(Integer.valueOf(foregroundInfo.mMultiWindowForegroundUid));
            } catch (Exception e3) {
                Slog.w(PerfService.TAG, "Failed to get foreground info from ProcessManager", e3);
            }
            int fzCount2 = 0;
            SparseArray<List<RunningProcess>> uidMap2 = GreezeServiceUtils.getUidMap();
            int i3 = 0;
            while (i3 < uidMap2.size()) {
                int uid = uidMap2.keyAt(i3);
                if (!UserHandle.isApp(uid) || uid == launchinguid2) {
                    fzCount = fzCount2;
                    dynamicWhiteList = dynamicWhiteList2;
                    launchinguid = launchinguid2;
                    startTime = startTime2;
                    castPid = castPid3;
                    uidMap = uidMap2;
                    i = i3;
                } else if (uid == fromUid) {
                    fzCount = fzCount2;
                    dynamicWhiteList = dynamicWhiteList2;
                    launchinguid = launchinguid2;
                    startTime = startTime2;
                    castPid = castPid3;
                    uidMap = uidMap2;
                    i = i3;
                } else {
                    dynamicWhiteList = dynamicWhiteList2;
                    if (dynamicWhiteList2.contains(Integer.valueOf(uid))) {
                        if (GreezeManagerService.DEBUG_SKIPUID) {
                            Slog.d(PerfService.TAG, "GzBoost skip uid " + uid + " for dynamic white list");
                        }
                        fzCount = fzCount2;
                        launchinguid = launchinguid2;
                        startTime = startTime2;
                        castPid = castPid3;
                        uidMap = uidMap2;
                        i = i3;
                    } else {
                        List<RunningProcess> procs = uidMap2.valueAt(i3);
                        boolean skipUid = false;
                        String skipReason2 = "";
                        Iterator<RunningProcess> it = procs.iterator();
                        while (true) {
                            launchinguid = launchinguid2;
                            if (!it.hasNext()) {
                                fzCount = fzCount2;
                                startTime = startTime2;
                                castPid = castPid3;
                                uidMap = uidMap2;
                                i = i3;
                                skipReason = skipReason2;
                                break;
                            }
                            uidMap = uidMap2;
                            RunningProcess proc = it.next();
                            fzCount = fzCount2;
                            if (proc.hasForegroundActivities) {
                                skipUid = true;
                                startTime = startTime2;
                                String skipReason3 = proc.pid + " " + proc.processName + " has foreground activity";
                                castPid = castPid3;
                                i = i3;
                                skipReason = skipReason3;
                                break;
                            }
                            startTime = startTime2;
                            if (proc.hasForegroundServices) {
                                skipUid = true;
                                String skipReason4 = proc.pid + " " + proc.processName + " has foreground service";
                                castPid = castPid3;
                                i = i3;
                                skipReason = skipReason4;
                                break;
                            } else if (proc.pid == castPid3) {
                                skipUid = true;
                                String skipReason5 = proc.pid + " " + proc.processName + " has cast activity";
                                castPid = castPid3;
                                i = i3;
                                skipReason = skipReason5;
                                break;
                            } else {
                                if (proc.pkgList == null) {
                                    castPid2 = castPid3;
                                    i2 = i3;
                                } else {
                                    String[] strArr = proc.pkgList;
                                    int length = strArr.length;
                                    int i4 = 0;
                                    while (true) {
                                        if (i4 >= length) {
                                            castPid2 = castPid3;
                                            i2 = i3;
                                            break;
                                        }
                                        int i5 = length;
                                        String pkg = strArr[i4];
                                        String[] strArr2 = strArr;
                                        String[] strArr3 = PerfService.WHITELIST_PKG;
                                        castPid2 = castPid3;
                                        int castPid4 = strArr3.length;
                                        i2 = i3;
                                        int i6 = 0;
                                        while (true) {
                                            if (i6 >= castPid4) {
                                                break;
                                            }
                                            int i7 = castPid4;
                                            String whitePkg = strArr3[i6];
                                            if (!TextUtils.equals(pkg, whitePkg)) {
                                                i6++;
                                                castPid4 = i7;
                                            } else {
                                                skipUid = true;
                                                String skipReason6 = proc.pid + " " + proc.processName + " in whitelist";
                                                skipReason2 = skipReason6;
                                                break;
                                            }
                                        }
                                        if (skipUid) {
                                            break;
                                        }
                                        i4++;
                                        length = i5;
                                        strArr = strArr2;
                                        castPid3 = castPid2;
                                        i3 = i2;
                                    }
                                }
                                fzCount2 = fzCount;
                                launchinguid2 = launchinguid;
                                uidMap2 = uidMap;
                                startTime2 = startTime;
                                castPid3 = castPid2;
                                i3 = i2;
                            }
                        }
                        if (skipUid) {
                            if (GreezeManagerService.DEBUG_SKIPUID) {
                                Slog.d(PerfService.TAG, "GzBoost skip uid " + uid + ", because " + skipReason);
                            }
                        } else {
                            for (RunningProcess proc2 : procs) {
                                String msg = "GzBoost " + launchingActivity + " from " + fromUid + ", freezing " + proc2.uid + " " + proc2.pid + " " + proc2.processName + " timeout=" + GreezeManagerService.LAUNCH_FZ_TIMEOUT + "ms";
                                if (GreezeManagerService.DEBUG) {
                                    Slog.d(PerfService.TAG, msg);
                                }
                                PerfService.this.mService.freezeProcess(proc2, GreezeManagerService.LAUNCH_FZ_TIMEOUT, GreezeServiceUtils.GREEZER_MODULE_PERFORMANCE, msg);
                                fzCount++;
                            }
                            PerfService.this.mService.queryBinderState(uid);
                            PerfService.this.mService.monitorNet(uid);
                        }
                    }
                }
                fzCount2 = fzCount;
                i3 = i + 1;
                dynamicWhiteList2 = dynamicWhiteList;
                launchinguid2 = launchinguid;
                uidMap2 = uidMap;
                startTime2 = startTime;
                castPid3 = castPid;
            }
            Trace.traceEnd(64L);
            long duration = SystemClock.uptimeMillis() - startTime2;
            Slog.d(PerfService.TAG, "GzBoost " + launchingActivity + " from " + fromUid + ", froze " + fzCount2 + " processes, took " + duration + "ms");
        }
    }

    public void startLaunchBoost(Bundle bundle) {
        if (!GreezeManagerService.isEnable()) {
            return;
        }
        int launchinguid = bundle.getInt("uid");
        String launchingActivity = bundle.getString("launchingActivity");
        int fromUid = bundle.getInt("fromUid");
        String fromPkg = bundle.getString("fromPkg");
        this.mService.thawUids(new int[]{launchinguid}, GreezeServiceUtils.GREEZER_MODULE_PERFORMANCE, "Launching " + launchingActivity + " from " + fromUid + " " + fromPkg);
        boolean isFromHome = TextUtils.equals(InputMethodManagerServiceImpl.MIUI_HOME, fromPkg);
        if (!isFromHome) {
            if (GreezeManagerService.DEBUG_LAUNCH_FROM_HOME) {
                Slog.d(TAG, "Launching " + launchingActivity + " from " + fromUid + " " + fromPkg);
                return;
            }
            return;
        }
        this.mService.mHandler.post(new GzLaunchBoostRunnable(bundle));
    }

    public void reportSignal(int uid, int pid, long now) throws RemoteException {
        this.mService.thawUids(new int[]{uid}, GreezeServiceUtils.GREEZER_MODULE_PERFORMANCE, "Receive frozen signal: uid=" + uid + " pid=" + pid);
    }

    public void reportNet(int uid, long now) throws RemoteException {
        this.mService.thawUids(new int[]{uid}, GreezeServiceUtils.GREEZER_MODULE_PERFORMANCE, "Receive frozen pkg net: uid=" + uid);
    }

    public void reportBinderTrans(int dstUid, int dstPid, int callerUid, int callerPid, int callerTid, boolean isOneway, long now, long buffer) throws RemoteException {
        this.mService.thawUids(new int[]{dstUid}, GreezeServiceUtils.GREEZER_MODULE_PERFORMANCE, "Receive frozen binder trans: dstUid=" + dstUid + " dstPid=" + dstPid + " callerUid=" + callerUid + " callerPid=" + callerPid + " callerTid=" + callerTid + " oneway=" + isOneway);
    }

    public void serviceReady(boolean ready) throws RemoteException {
    }

    public void reportBinderState(int uid, int pid, int tid, int binderState, long now) throws RemoteException {
        switch (binderState) {
            case 0:
            default:
                return;
            case 1:
                this.mService.thawUids(new int[]{uid}, GreezeServiceUtils.GREEZER_MODULE_PERFORMANCE, "Receive binder state: uid=" + uid + " pid=" + pid + " tid=" + tid);
                return;
            case 2:
            case 3:
            case 4:
                this.mService.thawPids(new int[]{pid}, GreezeServiceUtils.GREEZER_MODULE_PERFORMANCE, "Receive binder state: uid=" + uid + " pid=" + pid + " tid=" + tid);
                return;
        }
    }

    public void thawedByOther(int uid, int pid, int module) {
        if (GreezeManagerService.DEBUG) {
            Log.e(TAG, "thawed uid:" + uid + " by:" + module);
        }
    }
}
