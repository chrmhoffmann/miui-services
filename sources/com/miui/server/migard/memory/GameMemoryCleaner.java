package com.miui.server.migard.memory;

import android.content.Context;
import android.os.Handler;
import android.os.Process;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.Trace;
import com.android.server.ServiceThread;
import com.android.server.am.GameProcessCompactor;
import com.android.server.am.GameProcessKiller;
import com.android.server.am.IGameProcessAction;
import com.android.server.am.ProcessManagerService;
import com.miui.server.migard.IMiGardFeature;
import com.miui.server.migard.PackageStatusManager;
import com.miui.server.migard.ScreenStatusManager;
import com.miui.server.migard.UidStateManager;
import com.miui.server.migard.utils.FileUtils;
import com.miui.server.migard.utils.LogUtils;
import com.miui.server.migard.utils.PackageUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import miui.os.Build;
/* loaded from: classes.dex */
public class GameMemoryCleaner implements IMiGardFeature, PackageStatusManager.IForegroundChangedCallback, ScreenStatusManager.IScreenChangedCallback, UidStateManager.IUidStateChangedCallback {
    private static final boolean DEBUG = true;
    private static final String DEFAULT_MEMCG_PATH_R = "/sys/fs/cgroup/memory";
    private static final String DEFAULT_MEMCG_PATH_S = "/dev/memcg";
    private static final String GAME_MEMCG_PATH_R = "/sys/fs/cgroup/memory/game";
    private static final String GAME_MEMCG_PATH_S = "/dev/memcg/game";
    private static final String GAME_MEM_SAVE_PATH = "/data/system/migard/game_mem/";
    private static final String MEMCG_CURR_MEMUSAGE_NODE = "memory.memsw.usage_in_bytes";
    private static final String MEMCG_MAX_MEMUSAGE_NODE = "memory.memsw.max_usage_in_bytes";
    private static final String MEMCG_PROCS_NODE = "cgroup.procs";
    private static final String UID_PREFIX = "uid_";
    private Context mContext;
    private boolean mFeatureUsed;
    private Map<String, GameMemInfo> mGameMemInfos;
    private Handler mHandler;
    private ServiceThread mServiceThread;
    private static final String TAG = GameMemoryCleaner.class.getSimpleName();
    private static final boolean IS_MIUI_LITE = Build.IS_MIUI_LITE_VERSION;
    private static final boolean IS_MEMORY_CLEAN_ENABLED = SystemProperties.getBoolean("persist.sys.migard.mem_clean_enabled", false);
    private static final int GAME_MEMORY_PREDICT_POLICY = SystemProperties.getInt("persist.sys.migard.game_predict_policy", 2);
    private String mVpnPackage = null;
    private String mCurrentPackage = null;
    private String mCurrentGame = null;
    private int mCurrentAppUid = 0;
    private String mMemCgroupPath = null;
    private String mGameMemCgroupPath = null;
    private boolean hasConfiged = false;
    private Runnable mDelayedCleanRunnable = null;
    private ProcessManagerService mProcessManager = (ProcessManagerService) ServiceManager.getService("ProcessManager");

    /* loaded from: classes.dex */
    public class DelayedCleanRunnable implements Runnable {
        private DelayedCleanRunnable() {
            GameMemoryCleaner.this = r1;
        }

        @Override // java.lang.Runnable
        public void run() {
            LogUtils.i(GameMemoryCleaner.TAG, "reclaim memory for game(periodic).");
            GameMemoryCleaner gameMemoryCleaner = GameMemoryCleaner.this;
            gameMemoryCleaner.reclaimMemoryForGameIfNeed(gameMemoryCleaner.mCurrentPackage);
            if (GameMemoryCleanerConfig.getInstance().getCleanPeriod() > 0) {
                GameMemoryCleaner.this.periodicClean();
            }
        }
    }

    public GameMemoryCleaner(Context context) {
        this.mContext = null;
        boolean z = false;
        this.mFeatureUsed = false;
        this.mContext = context;
        String str = TAG;
        ServiceThread serviceThread = new ServiceThread(str, 0, false);
        this.mServiceThread = serviceThread;
        serviceThread.start();
        this.mHandler = new Handler(this.mServiceThread.getLooper());
        boolean isCgroupSupportd = initMemCgroupInfo();
        if (!isCgroupSupportd) {
            LogUtils.i(str, "mem cgroup is not supported");
        }
        if (isCgroupSupportd && (IS_MIUI_LITE || IS_MEMORY_CLEAN_ENABLED)) {
            z = true;
        }
        this.mFeatureUsed = z;
        if (z) {
            PackageStatusManager.getInstance().registerCallback(this);
            ScreenStatusManager.getInstance().registerCallback(this);
            UidStateManager.getInstance().registerCallback(this);
        }
        this.mGameMemInfos = new HashMap(10);
        GameMemoryCleanerConfig.getInstance().configFromFile();
        List<String> gameList = GameMemoryCleanerConfig.getInstance().getGameList();
        for (String g : gameList) {
            if (!this.mGameMemInfos.containsKey(g)) {
                this.mGameMemInfos.put(g, generateGameMemInfo(g));
            }
        }
    }

    private boolean initMemCgroupInfo() {
        if (new File("/sys/fs/cgroup/memory/uid_1000").exists() || new File("/dev/memcg/uid_1000").exists()) {
            return false;
        }
        if (new File(DEFAULT_MEMCG_PATH_R).exists() && new File(GAME_MEMCG_PATH_R).exists()) {
            this.mMemCgroupPath = DEFAULT_MEMCG_PATH_R;
            this.mGameMemCgroupPath = GAME_MEMCG_PATH_R;
            return true;
        } else if (!new File(DEFAULT_MEMCG_PATH_S).exists() || !new File(GAME_MEMCG_PATH_S).exists()) {
            return false;
        } else {
            this.mMemCgroupPath = DEFAULT_MEMCG_PATH_S;
            this.mGameMemCgroupPath = GAME_MEMCG_PATH_S;
            return true;
        }
    }

    private GameMemInfo generateGameMemInfo(String pkg) {
        GameMemInfo info = null;
        FileInputStream in = null;
        String path = GAME_MEM_SAVE_PATH + pkg;
        File f = new File(path);
        try {
            try {
            } catch (Throwable th) {
                if (in != null) {
                    try {
                        in.close();
                    } catch (Exception e) {
                        LogUtils.e(TAG, "close file failed, ", e);
                    }
                }
                throw th;
            }
        } catch (Exception e2) {
            LogUtils.e(TAG, "close file failed, ", e2);
        }
        if (!f.exists()) {
            return new GameMemInfo(pkg);
        }
        try {
            in = new FileInputStream(f);
            info = GameMemInfo.unmarshall(FileUtils.readFileByBytes(in));
            in.close();
        } catch (Exception e3) {
            info = null;
            LogUtils.e(TAG, "read game info failed, ", e3);
            if (in != null) {
                in.close();
            }
        }
        if (info == null) {
            GameMemInfo info2 = new GameMemInfo(pkg);
            return info2;
        }
        return info;
    }

    private void saveGameMemInfo() {
        for (GameMemInfo info : this.mGameMemInfos.values()) {
            saveGameMemInfo(info);
        }
        LogUtils.d(TAG, "save game memory info succeed");
    }

    private void saveGameMemInfo(GameMemInfo info) {
        if (info != null && info.hasUpdated) {
            FileUtils.writeFileByBytes(GAME_MEM_SAVE_PATH + info.pkgName, GameMemInfo.marshall(info), false);
            LogUtils.d(TAG, "save game memory info succeed, pkg: " + info.pkgName);
        }
    }

    private long predictGameMemory(String game) {
        long pred = -1;
        Trace.traceBegin(524288L, "predictGameMemory");
        int policy = GAME_MEMORY_PREDICT_POLICY;
        if (policy < 0) {
            policy = 0;
        }
        int i = 2;
        if (policy <= 2) {
            i = policy;
        }
        int policy2 = i;
        if (this.mGameMemInfos.keySet().contains(game)) {
            pred = this.mGameMemInfos.get(game).getPredSize(policy2);
        }
        Trace.traceEnd(524288L);
        return pred;
    }

    public void reclaimMemoryForGameIfNeed(String game) {
        long pred = (predictGameMemory(game) * GameMemoryCleanerConfig.getInstance().getReclaimMemoryPercent()) / 100;
        if (pred <= 0) {
            return;
        }
        long free = Process.getFreeMemory();
        long used = currentGameMemSize();
        if (used == -1) {
            LogUtils.e(TAG, "read current memory usage failed, abort reclaim.");
            return;
        }
        String str = TAG;
        LogUtils.d(str, "device free mem: " + free + ", pred game mem: " + pred + ", game used mem: " + used);
        long need = (pred - used) - free;
        if (need <= 0) {
            LogUtils.i(str, "enough mem for game, dont need kill background apps");
        } else {
            this.mProcessManager.reclaimBackgroundForGame(need);
        }
    }

    private long currentGameMemSize() {
        String uidPath = this.mGameMemCgroupPath + "/" + UID_PREFIX + this.mCurrentAppUid + "/";
        String content = FileUtils.readFromSys(uidPath + MEMCG_CURR_MEMUSAGE_NODE);
        if (content.isEmpty()) {
            return -1L;
        }
        try {
            long memSize = Long.parseLong(content);
            return memSize;
        } catch (NumberFormatException e) {
            LogUtils.e(TAG, "read game size failed, err:" + e);
            return -1L;
        }
    }

    private boolean checkAndApplyConfig(boolean onForeground) {
        if (onForeground) {
            this.hasConfiged = false;
        }
        boolean z = this.hasConfiged;
        if (z) {
            return z;
        }
        List<IGameProcessAction.IGameProcessActionConfig> configs = GameMemoryCleanerConfig.getInstance().getActionConfigs();
        if (configs == null || configs.size() == 0) {
            this.hasConfiged = false;
            return false;
        }
        for (int i = 0; i < configs.size(); i++) {
            IGameProcessAction.IGameProcessActionConfig cfg = configs.get(i);
            if (cfg instanceof GameProcessKiller.GameProcessKillerConfig) {
                GameProcessKiller.GameProcessKillerConfig kcfg = (GameProcessKiller.GameProcessKillerConfig) cfg;
                kcfg.addWhiteList(GameMemoryCleanerConfig.getInstance().getUserProtectList(), true);
                kcfg.addWhiteList(GameMemoryCleanerConfig.getInstance().getKillerCommonWhilteList(), true);
                kcfg.addWhiteList(GameMemoryCleanerConfig.getInstance().getPowerWhiteList(), true);
                if (this.mVpnPackage != null) {
                    List<String> vpn = new ArrayList<>();
                    vpn.add(this.mVpnPackage);
                    kcfg.addWhiteList(vpn, true);
                }
                this.mProcessManager.addGameProcessKiller(kcfg);
            } else if (cfg instanceof GameProcessCompactor.GameProcessCompactorConfig) {
                GameProcessCompactor.GameProcessCompactorConfig ccfg = (GameProcessCompactor.GameProcessCompactorConfig) cfg;
                ccfg.addWhiteList(GameMemoryCleanerConfig.getInstance().getCompactorCommonWhiteList(), true);
                this.mProcessManager.addGameProcessCompactor(ccfg);
            }
        }
        this.hasConfiged = true;
        return true;
    }

    public void reclaimBackgroundMemory() {
        this.mHandler.post(new Runnable() { // from class: com.miui.server.migard.memory.GameMemoryCleaner$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                GameMemoryCleaner.this.m2302xf71bbd46();
            }
        });
    }

    /* renamed from: lambda$reclaimBackgroundMemory$0$com-miui-server-migard-memory-GameMemoryCleaner */
    public /* synthetic */ void m2302xf71bbd46() {
        String str = this.mCurrentPackage;
        if (str == null || !this.mGameMemInfos.containsKey(str) || !checkAndApplyConfig(false)) {
            return;
        }
        LogUtils.i(TAG, "reclaim memory for game(scene).");
        reclaimMemoryForGameIfNeed(this.mCurrentPackage);
    }

    private void onGameForeground() {
        this.mProcessManager.notifyGameForeground(this.mCurrentPackage);
        if (checkAndApplyConfig(true) && GameMemoryCleanerConfig.getInstance().hasForegroundClean()) {
            delayClean(false);
        }
    }

    public void onVpnConnected(String user, boolean connected) {
        if (connected) {
            this.mVpnPackage = user;
        } else {
            this.mVpnPackage = null;
        }
    }

    private void delayClean(boolean periodic) {
        long j;
        disableDelayedClean();
        DelayedCleanRunnable delayedCleanRunnable = new DelayedCleanRunnable();
        this.mDelayedCleanRunnable = delayedCleanRunnable;
        Handler handler = this.mHandler;
        if (periodic) {
            j = GameMemoryCleanerConfig.getInstance().getCleanPeriod() * 1000;
        } else {
            j = GameMemoryCleanerConfig.getInstance().getCleanFirstDelay() * 1000;
        }
        handler.postDelayed(delayedCleanRunnable, j);
    }

    public void periodicClean() {
        if (this.mFeatureUsed && this.mGameMemInfos.containsKey(this.mCurrentPackage)) {
            LogUtils.d(TAG, this.mCurrentPackage + ", plan periodic clean...");
            delayClean(true);
        }
    }

    private void disableDelayedClean() {
        Runnable runnable = this.mDelayedCleanRunnable;
        if (runnable != null && this.mHandler.hasCallbacks(runnable)) {
            this.mHandler.removeCallbacks(this.mDelayedCleanRunnable);
            this.mDelayedCleanRunnable = null;
        }
    }

    public void onProcessKilled(String pkg, String reason) {
    }

    /* renamed from: onGameProcessStart */
    public void m2298x27044b80(int uid, int pid, String pkg) {
        if (this.mGameMemInfos.containsKey(pkg) && checkGameCgroup(uid)) {
            LogUtils.d(TAG, "monitor game pid: " + pid + " uid: " + uid);
            String uidPath = this.mGameMemCgroupPath + "/" + UID_PREFIX + uid;
            FileUtils.writeToSys(uidPath + "/" + MEMCG_PROCS_NODE, Integer.toString(pid));
        }
    }

    private boolean checkGameCgroup(int uid) {
        String uidPath = this.mGameMemCgroupPath + "/" + UID_PREFIX + uid;
        File gameCgDir = new File(uidPath);
        if (gameCgDir.exists()) {
            return true;
        }
        if (gameCgDir.mkdir()) {
            gameCgDir.setReadable(true, false);
            gameCgDir.setWritable(true, false);
            LogUtils.d(TAG, "create game cgroup succeed");
            return true;
        }
        LogUtils.d(TAG, "create game cgroup failed");
        return false;
    }

    /* renamed from: recordGameMemInfo */
    public void m2300xe7942ce3(int uid) {
        long memSize;
        String game = PackageUtils.getPackageNameByUid(this.mContext, uid);
        if (!this.mGameMemInfos.containsKey(game)) {
            return;
        }
        String uidPath = this.mGameMemCgroupPath + "/" + UID_PREFIX + uid;
        String content = FileUtils.readFromSys(uidPath + "/" + MEMCG_MAX_MEMUSAGE_NODE);
        if (!content.isEmpty()) {
            try {
                memSize = Long.parseLong(content);
            } catch (NumberFormatException e) {
                LogUtils.e(TAG, "read game size failed, err:" + e);
                memSize = -1;
            }
            LogUtils.d(TAG, "game mem size: " + memSize);
            if (memSize > 0) {
                this.mGameMemInfos.get(game).addHistoryItem(memSize);
            }
        }
    }

    public void addGameList(final List<String> gameList) {
        this.mHandler.post(new Runnable() { // from class: com.miui.server.migard.memory.GameMemoryCleaner$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                GameMemoryCleaner.this.m2296xbf2c3720(gameList);
            }
        });
    }

    /* renamed from: lambda$addGameList$1$com-miui-server-migard-memory-GameMemoryCleaner */
    public /* synthetic */ void m2296xbf2c3720(List gameList) {
        GameMemoryCleanerConfig.getInstance().addGameList(gameList, true);
        Iterator<String> iter = this.mGameMemInfos.keySet().iterator();
        while (iter.hasNext()) {
            String pkg = iter.next();
            if (!gameList.contains(pkg)) {
                saveGameMemInfo(this.mGameMemInfos.get(pkg));
                iter.remove();
            }
        }
        Iterator it = gameList.iterator();
        while (it.hasNext()) {
            String g = (String) it.next();
            if (!this.mGameMemInfos.containsKey(g)) {
                this.mGameMemInfos.put(g, generateGameMemInfo(g));
            }
        }
    }

    public void runOnThread(Runnable r) {
        this.mHandler.post(r);
    }

    @Override // com.miui.server.migard.PackageStatusManager.IForegroundChangedCallback
    public void onForegroundChanged(int pid, final int uid, final String name) {
        if (name == null || uid <= 0) {
            return;
        }
        this.mHandler.post(new Runnable() { // from class: com.miui.server.migard.memory.GameMemoryCleaner$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() {
                GameMemoryCleaner.this.m2297x25ecac42(name, uid);
            }
        });
    }

    /* renamed from: lambda$onForegroundChanged$2$com-miui-server-migard-memory-GameMemoryCleaner */
    public /* synthetic */ void m2297x25ecac42(String name, int uid) {
        String str = this.mCurrentPackage;
        if (str == null || !str.equals(name)) {
            if (this.mGameMemInfos.containsKey(this.mCurrentPackage)) {
                disableDelayedClean();
                this.mCurrentGame = null;
            }
            this.mCurrentPackage = name;
            this.mCurrentAppUid = uid;
            if (this.mGameMemInfos.containsKey(name)) {
                this.mCurrentGame = this.mCurrentPackage;
                GameMemoryCleanerConfig.getInstance().applyGameConfig(this.mCurrentGame);
                onGameForeground();
                return;
            }
            GameMemoryCleanerConfig.getInstance().applyGameConfig(this.mCurrentGame);
        }
    }

    @Override // com.miui.server.migard.PackageStatusManager.IForegroundChangedCallback, com.miui.server.migard.ScreenStatusManager.IScreenChangedCallback, com.miui.server.migard.UidStateManager.IUidStateChangedCallback
    public String getCallbackName() {
        return GameMemoryCleaner.class.getSimpleName();
    }

    @Override // com.miui.server.migard.ScreenStatusManager.IScreenChangedCallback
    public void onUserPresent() {
        this.mHandler.post(new Runnable() { // from class: com.miui.server.migard.memory.GameMemoryCleaner$$ExternalSyntheticLambda6
            @Override // java.lang.Runnable
            public final void run() {
                GameMemoryCleaner.this.m2301xefbbe602();
            }
        });
    }

    /* renamed from: lambda$onUserPresent$3$com-miui-server-migard-memory-GameMemoryCleaner */
    public /* synthetic */ void m2301xefbbe602() {
        String str = this.mCurrentPackage;
        if (str != null && this.mGameMemInfos.containsKey(str)) {
            onGameForeground();
        }
    }

    @Override // com.miui.server.migard.ScreenStatusManager.IScreenChangedCallback
    public void onScreenOff() {
        this.mHandler.post(new Runnable() { // from class: com.miui.server.migard.memory.GameMemoryCleaner$$ExternalSyntheticLambda5
            @Override // java.lang.Runnable
            public final void run() {
                GameMemoryCleaner.this.m2299x9710136();
            }
        });
    }

    /* renamed from: lambda$onScreenOff$4$com-miui-server-migard-memory-GameMemoryCleaner */
    public /* synthetic */ void m2299x9710136() {
        saveGameMemInfo();
        String str = this.mCurrentPackage;
        if (str != null && this.mGameMemInfos.containsKey(str)) {
            disableDelayedClean();
        }
    }

    @Override // com.miui.server.migard.UidStateManager.IUidStateChangedCallback
    public void onUidGone(final int uid) {
        this.mHandler.post(new Runnable() { // from class: com.miui.server.migard.memory.GameMemoryCleaner$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                GameMemoryCleaner.this.m2300xe7942ce3(uid);
            }
        });
    }

    public void onProcessStart(final int uid, final int pid, final String pkg, String caller) {
        this.mHandler.post(new Runnable() { // from class: com.miui.server.migard.memory.GameMemoryCleaner$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                GameMemoryCleaner.this.m2298x27044b80(uid, pid, pkg);
            }
        });
    }

    @Override // com.miui.server.migard.IMiGardFeature
    public void onShellHelp(PrintWriter pw) {
        pw.println("GameMemoryCleaner commands:");
        pw.println();
    }

    @Override // com.miui.server.migard.IMiGardFeature
    public boolean onShellCommand(String cmd, String value, PrintWriter pw) {
        return false;
    }

    @Override // com.miui.server.migard.IMiGardFeature
    public void dump(PrintWriter pw) {
        pw.println("game memory cleaner info: ");
        pw.println("is miui lite: " + IS_MIUI_LITE);
        pw.println("is enabled from sys prop: " + IS_MEMORY_CLEAN_ENABLED);
        pw.println("game memory predict policy: " + GAME_MEMORY_PREDICT_POLICY);
        pw.println("game memory records: ");
        for (GameMemInfo info : this.mGameMemInfos.values()) {
            pw.println(info);
        }
        GameMemoryCleanerConfig.getInstance().dump(pw);
    }
}
