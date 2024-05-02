package com.android.server.am;

import android.content.Context;
import android.os.Handler;
import android.os.SystemClock;
import android.os.Trace;
import android.util.Slog;
import android.util.SparseArray;
import com.android.server.ServiceThread;
import com.android.server.am.GameMemoryReclaimer;
import com.android.server.am.GameProcessCompactor;
import com.android.server.am.GameProcessKiller;
import com.android.server.wm.ForegroundInfoManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
/* loaded from: classes.dex */
public class GameMemoryReclaimer {
    private static final boolean DEBUG = true;
    private static final String TAG = "GameMemoryReclaimer";
    public ActivityManagerService mActivityManagerService;
    private final Context mContext;
    public ForegroundInfoManager mForegroundManager;
    private final Handler mHandler;
    public ProcessManagerService mProcessManager;
    public ProcessPolicy mProcessPolicy;
    private final ServiceThread mServiceThread;
    private Map<IGameProcessAction, List<GameProcessCompactor.ProcessCompactInfo>> mAllProcessInfos = new HashMap();
    private Map<IGameProcessAction, List<GameProcessKiller.PackageMemInfo>> mAllPackageInfos = new HashMap();
    private Map<Integer, GameProcessCompactor.ProcessCompactInfo> mCompactInfos = new HashMap();
    private String mCurrentGame = null;
    private List<IGameProcessAction> mProcessActions = new ArrayList();

    public GameMemoryReclaimer(ProcessManagerService processManager, Context context, ActivityManagerService ams) {
        this.mContext = context;
        this.mProcessManager = processManager;
        this.mActivityManagerService = ams;
        this.mProcessPolicy = processManager.getProcessPolicy();
        ServiceThread serviceThread = new ServiceThread(TAG, 0, false);
        this.mServiceThread = serviceThread;
        serviceThread.start();
        this.mHandler = new Handler(serviceThread.getLooper());
    }

    public void notifyGameForeground(String game) {
        synchronized (this.mProcessActions) {
            this.mCurrentGame = game;
            this.mProcessActions.clear();
            this.mAllProcessInfos.clear();
            this.mAllPackageInfos.clear();
            Slog.i(TAG, "reclaim memory for " + this.mCurrentGame);
        }
    }

    public void notifyGameBackground() {
        synchronized (this.mProcessActions) {
            this.mCurrentGame = null;
            this.mProcessActions.clear();
            this.mAllProcessInfos.clear();
            this.mAllPackageInfos.clear();
        }
    }

    public void addGameProcessKiller(GameProcessKiller.GameProcessKillerConfig cfg) {
        if (cfg != null) {
            GameProcessKiller killer = new GameProcessKiller(this, cfg);
            synchronized (this.mProcessActions) {
                this.mProcessActions.add(killer);
                this.mAllPackageInfos.put(killer, new ArrayList());
            }
        }
    }

    public void addGameProcessCompactor(GameProcessCompactor.GameProcessCompactorConfig cfg) {
        if (cfg != null) {
            GameProcessCompactor compactor = new GameProcessCompactor(this, cfg);
            synchronized (this.mProcessActions) {
                this.mProcessActions.add(compactor);
                this.mAllProcessInfos.put(compactor, new ArrayList());
            }
        }
    }

    private void filterAllProcessInfos() {
        long time = SystemClock.uptimeMillis();
        for (List<GameProcessCompactor.ProcessCompactInfo> v : this.mAllProcessInfos.values()) {
            v.clear();
        }
        getMatchedProcessList(new Comparable<ProcessRecord>() { // from class: com.android.server.am.GameMemoryReclaimer.1
            public int compareTo(ProcessRecord app) {
                int i = 1;
                for (IGameProcessAction action : GameMemoryReclaimer.this.mProcessActions) {
                    if ((action instanceof GameProcessCompactor) && !action.shouldSkip(app) && GameMemoryReclaimer.this.mAllProcessInfos.containsKey(action)) {
                        synchronized (GameMemoryReclaimer.this.mActivityManagerService) {
                            if (!GameMemoryReclaimer.this.mCompactInfos.containsKey(Integer.valueOf(app.getPid()))) {
                                GameMemoryReclaimer.this.mCompactInfos.put(Integer.valueOf(app.getPid()), new GameProcessCompactor.ProcessCompactInfo(app.getPid()));
                            }
                            GameProcessCompactor.ProcessCompactInfo info = (GameProcessCompactor.ProcessCompactInfo) GameMemoryReclaimer.this.mCompactInfos.get(Integer.valueOf(app.getPid()));
                            ((List) GameMemoryReclaimer.this.mAllProcessInfos.get(action)).add(info);
                        }
                        i = 0;
                    }
                }
                return i;
            }
        }, null);
        Slog.i(TAG, "spent " + (SystemClock.uptimeMillis() - time) + "ms to filter all processes(" + this.mCompactInfos.size() + ")");
    }

    private void filterAllPackageInfos() {
        Iterator<IGameProcessAction> it;
        long time = SystemClock.uptimeMillis();
        for (List<GameProcessKiller.PackageMemInfo> v : this.mAllPackageInfos.values()) {
            v.clear();
        }
        synchronized (this.mActivityManagerService) {
            ActiveUids uids = this.mActivityManagerService.mProcessList.mActiveUids;
            for (int i = 0; i < uids.size(); i++) {
                int uid = uids.keyAt(i);
                UidRecord uidRecord = uids.valueAt(i);
                int uidState = uidRecord.getCurProcState();
                GameProcessKiller.PackageMemInfo meminfo = null;
                Iterator<IGameProcessAction> it2 = this.mProcessActions.iterator();
                while (it2.hasNext()) {
                    IGameProcessAction action = it2.next();
                    if (action instanceof GameProcessKiller) {
                        final GameProcessKiller killer = (GameProcessKiller) action;
                        if (uidState >= killer.getMinProcState() && !killer.shouldSkip(uid)) {
                            final UidPss uidPss = new UidPss();
                            uidRecord.forEachProcess(new Consumer() { // from class: com.android.server.am.GameMemoryReclaimer$$ExternalSyntheticLambda1
                                @Override // java.util.function.Consumer
                                public final void accept(Object obj) {
                                    GameMemoryReclaimer.lambda$filterAllPackageInfos$0(GameMemoryReclaimer.UidPss.this, killer, (ProcessRecord) obj);
                                }
                            });
                            if (!uidPss.skip) {
                                if (meminfo == null) {
                                    it = it2;
                                    meminfo = new GameProcessKiller.PackageMemInfo(uid, uidPss.pss, uidState);
                                } else {
                                    it = it2;
                                }
                                if (this.mAllPackageInfos.containsKey(action)) {
                                    this.mAllPackageInfos.get(action).add(meminfo);
                                }
                                it2 = it;
                            }
                        }
                    }
                }
            }
        }
        Slog.i(TAG, "spent " + (SystemClock.uptimeMillis() - time) + "ms to filter all packages...");
    }

    public static /* synthetic */ void lambda$filterAllPackageInfos$0(UidPss uidPss, GameProcessKiller killer, ProcessRecord app) {
        uidPss.skip |= killer.shouldSkip(app);
        if (!uidPss.skip) {
            uidPss.pss += app.mProfile.getLastPss();
        }
    }

    public void reclaimBackground(final long need) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.am.GameMemoryReclaimer$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                GameMemoryReclaimer.this.m367xe2b0e465(need);
            }
        });
    }

    /* renamed from: lambda$reclaimBackground$1$com-android-server-am-GameMemoryReclaimer */
    public /* synthetic */ void m367xe2b0e465(long need) {
        Trace.traceBegin(524288L, "reclaimBackground: " + need);
        long mem = need;
        synchronized (this.mProcessActions) {
            if (this.mAllProcessInfos.size() > 0) {
                Trace.traceBegin(524288L, "filterAllProcessInfos");
                filterAllProcessInfos();
                Trace.traceEnd(524288L);
            }
            if (this.mAllPackageInfos.size() > 0) {
                Trace.traceBegin(524288L, "filterAllPackageInfos");
                filterAllPackageInfos();
                Trace.traceEnd(524288L);
            }
            for (int i = 0; i < this.mProcessActions.size(); i++) {
                long reclaim = this.mProcessActions.get(i).doAction(mem);
                if (reclaim >= mem) {
                    break;
                }
                mem -= reclaim;
            }
        }
        Trace.traceEnd(524288L);
    }

    public List<GameProcessKiller.PackageMemInfo> filterPackageInfos(IGameProcessAction killer) {
        if (!this.mAllPackageInfos.containsKey(killer)) {
            return new ArrayList();
        }
        List<GameProcessKiller.PackageMemInfo> packageList = this.mAllPackageInfos.get(killer);
        Collections.sort(packageList, new Comparator<GameProcessKiller.PackageMemInfo>() { // from class: com.android.server.am.GameMemoryReclaimer.2
            public int compare(GameProcessKiller.PackageMemInfo o1, GameProcessKiller.PackageMemInfo o2) {
                if (o2.mState == o1.mState) {
                    return (int) (o2.mMemSize - o1.mMemSize);
                }
                return o2.mState - o1.mState;
            }
        });
        return packageList;
    }

    public List<GameProcessCompactor.ProcessCompactInfo> filterProcessInfos(IGameProcessAction compactor) {
        if (this.mAllProcessInfos.containsKey(compactor)) {
            return this.mAllProcessInfos.get(compactor);
        }
        return new ArrayList();
    }

    public String getPackageNameByUid(int uid) {
        String[] pkgs = this.mContext.getPackageManager().getPackagesForUid(uid);
        if (pkgs != null && pkgs.length > 0) {
            String packageName = pkgs[0];
            return packageName;
        }
        String packageName2 = Integer.toString(uid);
        return packageName2;
    }

    private List<ProcessRecord> getMatchedProcessList(Comparable<ProcessRecord> condition, List<String> whitelist) {
        ArrayList<ProcessRecord> procs = new ArrayList<>();
        synchronized (this.mActivityManagerService) {
            int NP = this.mActivityManagerService.mProcessList.getProcessNamesLOSP().getMap().size();
            for (int ip = 0; ip < NP; ip++) {
                SparseArray<ProcessRecord> apps = (SparseArray) this.mActivityManagerService.mProcessList.getProcessNamesLOSP().getMap().valueAt(ip);
                int NA = apps.size();
                for (int ia = 0; ia < NA; ia++) {
                    ProcessRecord app = apps.valueAt(ia);
                    if (!app.isPersistent() && ((whitelist == null || !whitelist.contains(app.processName)) && (app.isRemoved() || condition.compareTo(app) == 0))) {
                        procs.add(app);
                    }
                }
            }
        }
        return procs;
    }

    public void notifyProcessDied(final int pid) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.am.GameMemoryReclaimer$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                GameMemoryReclaimer.this.m366x50e9f699(pid);
            }
        });
    }

    /* renamed from: lambda$notifyProcessDied$2$com-android-server-am-GameMemoryReclaimer */
    public /* synthetic */ void m366x50e9f699(int pid) {
        if (this.mCompactInfos.containsKey(Integer.valueOf(pid))) {
            this.mCompactInfos.get(Integer.valueOf(pid)).notifyDied();
        }
    }

    /* loaded from: classes.dex */
    public class UidPss {
        long pss;
        boolean skip;

        private UidPss() {
            GameMemoryReclaimer.this = r3;
            this.pss = 0L;
            this.skip = false;
        }
    }
}
