package com.android.server.am;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.media.AudioPlaybackConfiguration;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.ShellCommand;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.MemInfoReader;
import com.android.server.LocalServices;
import com.android.server.MiuiBatteryStatsService;
import com.android.server.ScoutHelper;
import com.android.server.SystemService;
import com.android.server.audio.AudioService;
import com.android.server.inputmethod.InputMethodManagerServiceImpl;
import com.android.server.location.LocationManagerService;
import com.android.server.pm.PackageManagerService;
import com.android.server.wm.ActivityTaskSupervisorImpl;
import com.android.server.wm.WindowManagerInternal;
import com.miui.server.AccessController;
import com.miui.server.SecurityManagerService;
import com.miui.server.input.edgesuppression.EdgeSuppressionFactory;
import com.miui.server.input.util.MiuiCustomizeShortCutUtils;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import miui.process.ProcessConfig;
import miui.process.ProcessManager;
/* loaded from: classes.dex */
public class PeriodicCleanerService extends SystemService {
    private static final String CLOUD_PERIODIC_ENABLE = "cloud_periodic_enable";
    private static boolean DEBUG = false;
    private static final int DEVICE_MEM_TYPE_COUNT = 3;
    private static final int HISTORY_SIZE;
    private static final int KILL_LEVEL_FORCE_STOP = 104;
    private static final int KILL_LEVEL_UNKOWN = 100;
    private static final int MEM_NO_PRESSURE = -1;
    private static final int MEM_PRESSURE_COUNT = 3;
    private static final int MEM_PRESSURE_CRITICAL = 2;
    private static final int MEM_PRESSURE_LOW = 0;
    private static final int MEM_PRESSURE_MIN = 1;
    private static final int MININUM_AGING_THRESHOLD = 3;
    private static final int MSG_REPORT_EVENT = 1;
    private static final int MSG_REPORT_PRESSURE = 2;
    private static final int MSG_SCREEN_OFF = 3;
    private static final String PERIODIC_DEBUG_PROP = "persist.sys.periodic.debug";
    private static final String PERIODIC_ENABLE_PROP = "persist.sys.periodic.enable";
    private static final String PERIODIC_MEM_THRES_PROP = "persist.sys.periodic.mem_threshold";
    private static final int SCREEN_STATE_OFF = 2;
    private static final int SCREEN_STATE_ON = 1;
    private static final int SCREEN_STATE_UNKOWN = 3;
    private static final Integer SYSTEM_UID_OBJ;
    private static final String TAG = "PeriodicCleaner";
    private static final String TIME_FORMAT_PATTERN = "HH:mm:ss.SSS";
    private Context mContext;
    private MyHandler mHandler;
    private static final int[] sDefaultActiveLength = {4, 6, 8};
    private static final int[][] sDefaultCacheLevel = {new int[]{450000, 370000, 290000}, new int[]{550000, 470000, 390000}, new int[]{650000, 570000, 490000}};
    private static List<String> sSystemRelatedPkgs = new ArrayList();
    private static List<String> sCleanWhiteList = new ArrayList();
    private static List<String> sHomeOrRecents = new ArrayList();
    private volatile boolean mReady = false;
    private volatile boolean mEnable = SystemProperties.getBoolean(PERIODIC_ENABLE_PROP, false);
    private int mAppMemThreshold = SystemProperties.getInt(PERIODIC_MEM_THRES_PROP, 600);
    private final Object mLock = new Object();
    private HandlerThread mThread = new HandlerThread(TAG);
    private BinderService mBinderService = null;
    private PeriodicCleanerInternal mLocalService = null;
    private ProcessManagerService mPMS = null;
    private ActivityManagerService mAMS = null;
    private WindowManagerInternal mWMS = null;
    private AudioService mAudioService = null;
    private LocationManagerService mLMS = null;
    private PackageManagerService.IPackageManagerImpl mPKMS = null;
    private Class<?> mAudioServiceClz = null;
    private Class<?> mWindowManagerInternalClz = null;
    private Class<?> mLocationManagerServiceClz = null;
    private Method mGetAllAudioFocusMethod = null;
    private Method mGetVisibleWindowOwnerMethod = null;
    private Method mGetLocationActiveUidsByProviderMethod = null;
    private Class<?> mAndroidOsDebugClz = null;
    private Field mDebugClz_Field_MEMINFO_CACHED = null;
    private Field mDebugClz_Field_MEMINFO_SWAPCACHED = null;
    private Field mDebugClz_Field_MEMINFO_BUFFERS = null;
    private Field mDebugClz_Field_MEMINFO_SHMEM = null;
    private Field mDebugClz_Field_MEMINFO_UNEVICTABLE = null;
    private int mDebugClz_MEMINFO_CACHED = 0;
    private int mDebugClz_MEMINFO_SWAPCACHED = 0;
    private int mDebugClz_MEMINFO_BUFFERS = 0;
    private int mDebugClz_MEMINFO_SHMEM = 0;
    private int mDebugClz_MEMINFO_UNEVICTABLE = 0;
    final ArrayList<PackageUseInfo> mLruPackages = new ArrayList<>();
    final CleanInfo[] mCleanHistory = new CleanInfo[HISTORY_SIZE];
    private List<Integer> mLastVisibleUids = new ArrayList();
    private String mLastNonSystemFgPkg = null;
    private int mLastNonSystemFgUid = 0;
    private int mLruActiveLength = 0;
    private int[] mPressureAgingThreshold = new int[3];
    private int[] mPressureCacheThreshold = new int[3];
    private long mLastCleanByPressure = 0;
    private int mCleanHistoryIndex = 0;
    private volatile int mScreenState = 3;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() { // from class: com.android.server.am.PeriodicCleanerService.2
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.SCREEN_ON".equals(action)) {
                PeriodicCleanerService.this.mScreenState = 1;
                PeriodicCleanerService.this.mHandler.removeMessages(2);
            } else if ("android.intent.action.SCREEN_OFF".equals(action)) {
                PeriodicCleanerService.this.mScreenState = 2;
                PeriodicCleanerService.this.mHandler.sendEmptyMessageDelayed(3, SecurityManagerService.LOCK_TIME_OUT);
            }
        }
    };

    /* loaded from: classes.dex */
    public static abstract class PeriodicCleanerInternal {
        public abstract void reportEvent(String str, String str2, int i, int i2, boolean z);

        public abstract void reportMemPressure(int i);
    }

    static {
        boolean z = SystemProperties.getBoolean(PERIODIC_DEBUG_PROP, false);
        DEBUG = z;
        HISTORY_SIZE = z ? 500 : 100;
        SYSTEM_UID_OBJ = new Integer(1000);
        sHomeOrRecents.add(InputMethodManagerServiceImpl.MIUI_HOME);
        sHomeOrRecents.add("com.android.systemui");
        sHomeOrRecents.add("com.mi.android.globallauncher");
        sSystemRelatedPkgs.add("android");
        sSystemRelatedPkgs.add("com.lbe.security.miui");
        sSystemRelatedPkgs.add("com.miui.securityadd");
        sSystemRelatedPkgs.add("com.xiaomi.xmsf");
        sSystemRelatedPkgs.add(MiuiBatteryStatsService.TrackBatteryUsbInfo.ANALYTICS_PACKAGE);
        sSystemRelatedPkgs.add("com.android.permissioncontroller");
        sSystemRelatedPkgs.add(ActivityTaskSupervisorImpl.MIUI_APP_LOCK_PACKAGE_NAME);
        sSystemRelatedPkgs.add("com.miui.powerkeeper");
        sCleanWhiteList.add("com.tencent.mm");
        sCleanWhiteList.add("com.tencent.mobileqq");
        sCleanWhiteList.add("com.whatsapp");
        sCleanWhiteList.add(AccessController.PACKAGE_CAMERA);
    }

    public void onStart() {
        if (DEBUG) {
            Slog.i(TAG, "Starting PeriodicCleaner");
        }
        this.mBinderService = new BinderService();
        LocalService localService = new LocalService();
        this.mLocalService = localService;
        publishLocalService(PeriodicCleanerInternal.class, localService);
        publishBinderService("periodic", this.mBinderService);
    }

    public void onBootPhase(int phase) {
        if (phase == 550) {
            onActivityManagerReady();
        } else if (phase == 1000) {
            onBootComplete();
        }
    }

    private void onActivityManagerReady() {
        this.mPMS = (ProcessManagerService) ServiceManager.getService("ProcessManager");
        this.mAMS = ServiceManager.getService("activity");
        this.mWMS = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);
        this.mLMS = ServiceManager.getService("location");
        this.mPKMS = ServiceManager.getService("package");
        AudioService service = ServiceManager.getService("audio");
        this.mAudioService = service;
        if (this.mPMS == null || this.mAMS == null || this.mWMS == null || this.mLMS == null || this.mPKMS == null || service == null) {
            this.mEnable = false;
            Slog.w(TAG, "disable periodic for dependencies service not available");
        }
    }

    private void onBootComplete() {
        if (this.mEnable) {
            registerCloudObserver(this.mContext);
            if (Settings.System.getStringForUser(this.mContext.getContentResolver(), CLOUD_PERIODIC_ENABLE, -2) != null) {
                this.mEnable = Boolean.parseBoolean(Settings.System.getStringForUser(this.mContext.getContentResolver(), CLOUD_PERIODIC_ENABLE, -2));
                Slog.w(TAG, "set enable state from database: " + this.mEnable);
            }
            this.mReady = this.mEnable;
        }
    }

    public PeriodicCleanerService(Context context) {
        super(context);
        this.mContext = null;
        this.mHandler = null;
        this.mContext = context;
        this.mThread.start();
        this.mHandler = new MyHandler(this.mThread.getLooper());
        init();
    }

    private void registerCloudObserver(final Context context) {
        ContentObserver observer = new ContentObserver(this.mHandler) { // from class: com.android.server.am.PeriodicCleanerService.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                if (uri != null && uri.equals(Settings.System.getUriFor(PeriodicCleanerService.CLOUD_PERIODIC_ENABLE))) {
                    PeriodicCleanerService.this.mEnable = Boolean.parseBoolean(Settings.System.getStringForUser(context.getContentResolver(), PeriodicCleanerService.CLOUD_PERIODIC_ENABLE, -2));
                    PeriodicCleanerService periodicCleanerService = PeriodicCleanerService.this;
                    periodicCleanerService.mReady = periodicCleanerService.mEnable;
                    Slog.w(PeriodicCleanerService.TAG, "cloud control set received: " + PeriodicCleanerService.this.mEnable);
                }
            }
        };
        context.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_PERIODIC_ENABLE), false, observer, -2);
    }

    private void init() {
        if (this.mEnable) {
            if (!dependencyCheck()) {
                this.mEnable = false;
                return;
            }
            int totalMemGB = (int) ((Process.getTotalMemory() / 1073741824) + 1);
            if (totalMemGB != 2 && totalMemGB != 3 && totalMemGB != 4) {
                this.mEnable = false;
                return;
            }
            int index = totalMemGB - 2;
            if (!loadLruLengthConfig(index)) {
                loadDefLruLengthConfig(index);
            }
            if (!loadCacheLevelConfig(index)) {
                loadDefCacheLevelConfig(index);
            }
        }
    }

    private boolean dependencyCheck() {
        try {
            Class<?> cls = Class.forName("com.android.server.audio.AudioService");
            this.mAudioServiceClz = cls;
            this.mGetAllAudioFocusMethod = cls.getDeclaredMethod("getAllAudioFocus", new Class[0]);
            Class<?> cls2 = Class.forName("com.android.server.wm.WindowManagerInternal");
            this.mWindowManagerInternalClz = cls2;
            this.mGetVisibleWindowOwnerMethod = cls2.getDeclaredMethod("getVisibleWindowOwner", new Class[0]);
            Class<?> cls3 = Class.forName("com.android.server.location.LocationManagerService");
            this.mLocationManagerServiceClz = cls3;
            this.mGetLocationActiveUidsByProviderMethod = cls3.getDeclaredMethod("getLocationActiveUidsByProvider", String.class);
            Class<?> cls4 = Class.forName("android.os.Debug");
            this.mAndroidOsDebugClz = cls4;
            this.mDebugClz_Field_MEMINFO_CACHED = cls4.getField("MEMINFO_CACHED");
            this.mDebugClz_Field_MEMINFO_SWAPCACHED = this.mAndroidOsDebugClz.getField("MEMINFO_SWAPCACHED");
            this.mDebugClz_Field_MEMINFO_BUFFERS = this.mAndroidOsDebugClz.getField("MEMINFO_BUFFERS");
            this.mDebugClz_Field_MEMINFO_SHMEM = this.mAndroidOsDebugClz.getField("MEMINFO_SHMEM");
            this.mDebugClz_Field_MEMINFO_UNEVICTABLE = this.mAndroidOsDebugClz.getField("MEMINFO_UNEVICTABLE");
            this.mDebugClz_MEMINFO_CACHED = this.mDebugClz_Field_MEMINFO_CACHED.getInt(null);
            this.mDebugClz_MEMINFO_SWAPCACHED = this.mDebugClz_Field_MEMINFO_SWAPCACHED.getInt(null);
            this.mDebugClz_MEMINFO_BUFFERS = this.mDebugClz_Field_MEMINFO_BUFFERS.getInt(null);
            this.mDebugClz_MEMINFO_SHMEM = this.mDebugClz_Field_MEMINFO_SHMEM.getInt(null);
            this.mDebugClz_MEMINFO_UNEVICTABLE = this.mDebugClz_Field_MEMINFO_UNEVICTABLE.getInt(null);
            return true;
        } catch (NoSuchFieldException nfe) {
            Slog.e(TAG, "dependent field missing: " + nfe);
            return false;
        } catch (NoSuchMethodException nme) {
            Slog.e(TAG, "dependent interface missing: " + nme);
            return false;
        } catch (Exception e) {
            Slog.e(TAG, "dependency check failed: " + e);
            return false;
        }
    }

    private int finalLruActiveLength(int length) {
        if (length <= 3) {
            return 3;
        }
        return length;
    }

    private boolean loadLruLengthConfig(int memIndex) {
        String value = SystemProperties.get("persist.sys.periodic.lru_active_length");
        String[] memArray = value.split(":");
        if (memArray.length != 3) {
            return false;
        }
        try {
            int len = Integer.parseInt(memArray[memIndex]);
            if (len <= 0) {
                return false;
            }
            this.mLruActiveLength = len;
            this.mPressureAgingThreshold[0] = finalLruActiveLength(len);
            this.mPressureAgingThreshold[1] = finalLruActiveLength((this.mLruActiveLength * 3) / 4);
            this.mPressureAgingThreshold[2] = finalLruActiveLength(this.mLruActiveLength / 2);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean loadCacheLevelConfig(int memIndex) {
        String value = SystemProperties.get("persist.sys.periodic.cache_level_config");
        String[] memArray = value.split(":");
        if (memArray.length != 3 || memArray[memIndex].length() == 0) {
            return false;
        }
        String[] pressureArray = memArray[memIndex].split(",");
        if (pressureArray.length != 3) {
            return false;
        }
        int index = 0;
        for (String str : pressureArray) {
            try {
                int cache = Integer.parseInt(str);
                if (cache <= 0) {
                    return false;
                }
                this.mPressureCacheThreshold[index] = cache;
                index++;
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }

    private void loadDefLruLengthConfig(int memIndex) {
        int i = sDefaultActiveLength[memIndex];
        this.mLruActiveLength = i;
        this.mPressureAgingThreshold[0] = finalLruActiveLength(i);
        this.mPressureAgingThreshold[1] = finalLruActiveLength((this.mLruActiveLength * 3) / 4);
        this.mPressureAgingThreshold[2] = finalLruActiveLength(this.mLruActiveLength / 2);
    }

    private void loadDefCacheLevelConfig(int memIndex) {
        int[] iArr = this.mPressureCacheThreshold;
        int[] iArr2 = sDefaultCacheLevel[memIndex];
        iArr[0] = iArr2[0];
        iArr[1] = iArr2[1];
        iArr[2] = iArr2[2];
    }

    private void addCleanHistory(CleanInfo cInfo) {
        if (cInfo.isValid()) {
            synchronized (this.mCleanHistory) {
                CleanInfo[] cleanInfoArr = this.mCleanHistory;
                int i = this.mCleanHistoryIndex;
                cleanInfoArr[i] = cInfo;
                this.mCleanHistoryIndex = (i + 1) % HISTORY_SIZE;
            }
        }
    }

    public void reportEvent(MyEvent event) {
        String packageName = event.mPackage;
        if (!this.mReady || packageName == null || event.mUid == 1000 || isSystemPackage(packageName) || event.mEventType != 1) {
            return;
        }
        if (!event.mFullScreen) {
            Slog.d(TAG, packageName + "/" + event.mClass + " isn't fullscreen, skip.");
        } else if (!packageName.equals(this.mLastNonSystemFgPkg) || event.mUid != this.mLastNonSystemFgUid) {
            this.mLastNonSystemFgPkg = packageName;
            this.mLastNonSystemFgUid = event.mUid;
            if (updateLruPackageLocked(event.mUid, packageName)) {
                cleanPackageByPeriodic();
            } else {
                checkPressureAndClean();
            }
        }
    }

    public void reportMemPressure(int pressureState) {
        if (this.mReady && pressureState == 3) {
            long now = SystemClock.uptimeMillis();
            if (now - this.mLastCleanByPressure > 10000) {
                if (DEBUG) {
                    Slog.d(TAG, "try to clean for ADJ_MEM_FACTOR_CRITICAL.");
                }
                checkPressureAndClean();
            }
        }
    }

    public void handleScreenOff() {
        if (this.mScreenState != 2) {
            Slog.d(TAG, "screen on when deap clean, skip");
            return;
        }
        long startTime = SystemClock.uptimeMillis();
        List<ProcessRecord> victimList = new ArrayList<>();
        List<String> killed = new ArrayList<>();
        List<Integer> topUids = new ArrayList<>();
        synchronized (this.mLock) {
            int N = this.mLruPackages.size();
            int i = N - 1;
            for (int j = 0; i >= 0 && j < 2; j++) {
                topUids.add(Integer.valueOf(this.mLruPackages.get(i).mUid));
                i--;
            }
        }
        this.mLastVisibleUids.remove(SYSTEM_UID_OBJ);
        List<Integer> audioActiveUids = getAudioActiveUids();
        List<Integer> locationActiveUids = getLocationActiveUids("gps");
        if (DEBUG) {
            Slog.d(TAG, "TopPkg=" + topUids + ", AudioActive=" + audioActiveUids + ", LocationActive=" + locationActiveUids + ", LastVisible=" + this.mLastVisibleUids);
        }
        synchronized (this.mAMS) {
            int N2 = this.mAMS.mProcessList.getLruSizeLOSP();
            int i2 = N2 - 1;
            while (i2 >= 0) {
                ProcessRecord app = (ProcessRecord) this.mAMS.mProcessList.getLruProcessesLOSP().get(i2);
                int i3 = i2;
                if (app.mProfile.getLastPss() / 1024 > this.mAppMemThreshold && app.getPid() != ActivityManagerService.MY_PID && !audioActiveUids.contains(Integer.valueOf(app.uid)) && !locationActiveUids.contains(Integer.valueOf(app.uid)) && !topUids.contains(Integer.valueOf(app.uid))) {
                    victimList.add(app);
                }
                i2 = i3 - 1;
            }
            int i4 = 0;
            while (true) {
                if (i4 >= victimList.size()) {
                    break;
                } else if (this.mScreenState != 2) {
                    Slog.w(TAG, "screen on when deap clean, abort");
                    break;
                } else {
                    ProcessRecord app2 = victimList.get(i4);
                    Slog.w(TAG, "Kill process " + app2.processName + ", pid " + app2.getPid() + ", pss " + app2.mProfile.getLastPss() + " for abnormal mem usage.");
                    app2.killLocked("deap clean " + app2.mProfile.getLastPss(), 5, true);
                    killed.add(app2.processName);
                    i4++;
                }
            }
        }
        if (killed.size() > 0) {
            CleanInfo cInfo = new CleanInfo(System.currentTimeMillis(), -1, 2, "deap");
            cInfo.addCleanList(0, killed);
            addCleanHistory(cInfo);
        }
        checkTime(startTime, "finish deap clean");
    }

    private boolean updateLruPackageLocked(int uid, String packageName) {
        int newFgIndex = -1;
        int userId = UserHandle.getUserId(uid);
        PackageUseInfo info = null;
        synchronized (this.mLock) {
            int N = this.mLruPackages.size();
            int i = N - 1;
            while (true) {
                if (i < 0) {
                    break;
                }
                info = this.mLruPackages.get(i);
                if (info.mUserId != userId || !info.mPackageName.equals(packageName)) {
                    i--;
                } else {
                    this.mLruPackages.remove(info);
                    this.mLruPackages.add(info);
                    newFgIndex = i;
                    break;
                }
            }
            if (newFgIndex == -1) {
                PackageUseInfo newInfo = obtainPackageUseInfo(uid, packageName);
                this.mLruPackages.add(newInfo);
                return true;
            }
            if (info != null && info.mUid != uid) {
                Slog.d(TAG, packageName + " re-installed, NewAppId=" + UserHandle.getAppId(uid) + ", OldAppId=" + UserHandle.getAppId(info.mUid));
                info.updateUid(uid);
            }
            return N - newFgIndex > this.mLruActiveLength;
        }
    }

    private PackageUseInfo obtainPackageUseInfo(int uid, String packageName) {
        return new PackageUseInfo(uid, packageName);
    }

    public void cleanPackageByPeriodic() {
        doClean(this.mLruActiveLength, 100, -1, "cycle");
    }

    public void cleanPackageByPressure(int pressure) {
        doClean(this.mPressureAgingThreshold[pressure], 100, pressure, "pressure");
    }

    private void checkPressureAndClean() {
        int pressure = getMemPressureLevel();
        if (pressure != -1) {
            cleanPackageByPressure(pressure);
            this.mLastCleanByPressure = SystemClock.uptimeMillis();
        }
    }

    private int getMemPressureLevel() {
        int level;
        MemInfoReader minfo = new MemInfoReader();
        minfo.readMemInfo();
        long[] rawInfo = minfo.getRawInfo();
        long j = rawInfo[this.mDebugClz_MEMINFO_CACHED];
        int i = this.mDebugClz_MEMINFO_SWAPCACHED;
        long otherFile = j + rawInfo[i] + rawInfo[this.mDebugClz_MEMINFO_BUFFERS];
        long needRemovedFile = rawInfo[this.mDebugClz_MEMINFO_SHMEM] + rawInfo[this.mDebugClz_MEMINFO_UNEVICTABLE] + rawInfo[i];
        if (otherFile > needRemovedFile) {
            otherFile -= needRemovedFile;
        }
        int[] iArr = this.mPressureCacheThreshold;
        if (otherFile >= iArr[0]) {
            return -1;
        }
        if (otherFile >= iArr[1]) {
            level = 0;
        } else if (otherFile >= iArr[2]) {
            level = 1;
        } else {
            level = 2;
        }
        if (DEBUG) {
            Slog.i(TAG, "Other File: " + otherFile + "KB. Mem Pressure Level: " + level);
        }
        return level;
    }

    private void doClean(int thresHold, int killLevel, int pressure, String reason) {
        String longReason = "PeriodicCleaner(" + thresHold + "|" + reason + ")";
        long startTime = SystemClock.uptimeMillis();
        SparseArray<ArrayList<String>> agingPkgs = findAgingPackageLocked(thresHold);
        List<Integer> visibleUids = getVisibleWindowOwner();
        List<Integer> audioActiveUids = getAudioActiveUids();
        List<Integer> locationActiveUids = getLocationActiveUids("gps");
        checkTime(startTime, "finish get active and visible uids");
        if (DEBUG) {
            Slog.d(TAG, "AgingPacakges: " + agingPkgs + ", LocationActiveUids: " + locationActiveUids + ", VisibleUids: " + visibleUids + ", AudioActiveUids: " + audioActiveUids);
        }
        CleanInfo cInfo = new CleanInfo(System.currentTimeMillis(), pressure, thresHold, reason);
        int i = 0;
        while (i < agingPkgs.size()) {
            int userId = agingPkgs.keyAt(i);
            int i2 = i;
            CleanInfo cInfo2 = cInfo;
            ArrayList<String> userTargets = filterOutKillablePackages(userId, pressure, agingPkgs.valueAt(i), visibleUids, audioActiveUids, locationActiveUids);
            if (userTargets.size() > 0) {
                killTargets(userId, userTargets, killLevel, longReason);
                cInfo2.addCleanList(userId, userTargets);
            }
            i = i2 + 1;
            cInfo = cInfo2;
        }
        addCleanHistory(cInfo);
    }

    private SparseArray<ArrayList<String>> findAgingPackageLocked(int threshold) {
        SparseArray<ArrayList<String>> agingPkgs = new SparseArray<>();
        synchronized (this.mLock) {
            long startTime = SystemClock.uptimeMillis();
            int size = this.mLruPackages.size() - threshold;
            if (size > 0) {
                for (int i = 0; i < size; i++) {
                    PackageUseInfo info = this.mLruPackages.get(i);
                    ArrayList<String> userTargets = agingPkgs.get(info.mUserId);
                    if (userTargets == null) {
                        userTargets = new ArrayList<>();
                        agingPkgs.put(info.mUserId, userTargets);
                    }
                    userTargets.add(info.mPackageName);
                }
            }
            checkTime(startTime, "finish findAgingPackageLocked");
        }
        return agingPkgs;
    }

    private List<Integer> getAudioActiveUids() {
        List<AudioPlaybackConfiguration> activePlayers = this.mAudioService.getActivePlaybackConfigurations();
        List<Integer> activeUids = new ArrayList<>();
        for (AudioPlaybackConfiguration conf : activePlayers) {
            int state = conf.getPlayerState();
            if (state == 2 || state == 3) {
                activeUids.add(Integer.valueOf(conf.getClientUid()));
            }
        }
        List<Integer> focusUids = getAllAudioFocus();
        for (Integer uid : focusUids) {
            if (!activeUids.contains(uid)) {
                activeUids.add(uid);
            }
        }
        if (DEBUG && activeUids.size() > 0) {
            int[] uids = new int[activeUids.size()];
            for (int i = 0; i < activeUids.size(); i++) {
                uids[i] = activeUids.get(i).intValue();
            }
        }
        return activeUids;
    }

    private List<Integer> getAllAudioFocus() {
        try {
            return (List) this.mGetAllAudioFocusMethod.invoke(this.mAudioService, new Object[0]);
        } catch (Exception e) {
            Slog.e(TAG, "getAllAudioFocus: " + e);
            return new ArrayList();
        }
    }

    private List<Integer> getVisibleWindowOwner() {
        try {
            List<Integer> list = (List) this.mGetVisibleWindowOwnerMethod.invoke(this.mWMS, new Object[0]);
            this.mLastVisibleUids = list;
            return list;
        } catch (Exception e) {
            Slog.e(TAG, "getVisibleWindowOwner: " + e);
            return new ArrayList();
        }
    }

    private List<Integer> getLocationActiveUids(String provider) {
        try {
            return (List) this.mGetLocationActiveUidsByProviderMethod.invoke(this.mLMS, provider);
        } catch (Exception e) {
            Slog.e(TAG, "getLocationActiveUids: " + e);
            return new ArrayList();
        }
    }

    private ArrayList<String> filterOutKillablePackages(int userId, int pressure, ArrayList<String> agingPkgs, List<Integer> visibleUids, List<Integer> audioActiveUids, List<Integer> locationActiveUids) {
        int i;
        int N;
        long startTime = SystemClock.uptimeMillis();
        HashMap<String, Boolean> dynWhitelist = this.mPMS.getProcessPolicy().updateDynamicWhiteList(this.mContext, userId);
        checkTime(startTime, "finish updateDynamicWhiteList");
        ArrayList<String> killList = new ArrayList<>();
        synchronized (this.mAMS) {
            int N2 = this.mAMS.mProcessList.getLruSizeLOSP();
            int i2 = N2 - 1;
            while (i2 >= 0) {
                ProcessRecord app = (ProcessRecord) this.mAMS.mProcessList.getLruProcessesLOSP().get(i2);
                if (app.isKilledByAm() || app.getThread() == null || app.userId != userId) {
                    N = N2;
                    i = i2;
                } else if (!agingPkgs.contains(app.info.packageName)) {
                    N = N2;
                    i = i2;
                } else {
                    N = N2;
                    i = i2;
                    if (canCleanPackage(app, pressure, dynWhitelist, visibleUids, audioActiveUids, locationActiveUids)) {
                        if (!killList.contains(app.info.packageName)) {
                            killList.add(app.info.packageName);
                        }
                    } else {
                        agingPkgs.remove(app.info.packageName);
                        killList.remove(app.info.packageName);
                    }
                }
                i2 = i - 1;
                N2 = N;
            }
            checkTime(startTime, "finish filterOutKillablePackages");
        }
        return killList;
    }

    private boolean canCleanPackage(ProcessRecord app, int pressure, HashMap<String, Boolean> dynWhitelist, List<Integer> visibleUids, List<Integer> activeAudioUids, List<Integer> locationActiveUids) {
        String packageName = app.info.packageName;
        if (isSystemPackage(packageName) || isWhiteListPackage(packageName) || dynWhitelist.containsKey(packageName) || !Process.isApplicationUid(app.uid)) {
            if (DEBUG) {
                Slog.d(TAG, packageName + " is in exclude list.");
            }
            return false;
        } else if (packageName == null || !packageName.contains("com.google.android")) {
            return !isActive(app, visibleUids, activeAudioUids, locationActiveUids) && !isImportant(app, pressure);
        } else {
            if (DEBUG) {
                Slog.d(TAG, packageName + " skip for google.");
            }
            return false;
        }
    }

    private boolean isActive(ProcessRecord app, List<Integer> visibleUids, List<Integer> activeAudioUids, List<Integer> locationActiveUids) {
        if (app.mState.getCurAdj() < 0 || visibleUids.contains(Integer.valueOf(app.uid))) {
            if (DEBUG) {
                Slog.d(TAG, app.info.packageName + " is persistent or owning visible window.");
            }
            return true;
        } else if (locationActiveUids.contains(Integer.valueOf(app.uid)) || activeAudioUids.contains(Integer.valueOf(app.uid))) {
            if (DEBUG) {
                Slog.d(TAG, app.info.packageName + " is audio or gps active.");
            }
            return true;
        } else {
            return false;
        }
    }

    private boolean isImportant(ProcessRecord app, int pressure) {
        if (pressure <= 0 && app.mServices.hasForegroundServices()) {
            if (DEBUG) {
                Slog.d(TAG, app.info.packageName + " has ForegroundService.");
            }
            return true;
        } else if (pressure <= 1 && ProcessManager.isLockedApplication(app.info.packageName, app.userId)) {
            if (DEBUG) {
                Slog.d(TAG, app.info.packageName + " isLocked.");
            }
            return true;
        } else if ((app.mState.getAdjSource() instanceof ProcessRecord) && ((ProcessRecord) app.mState.getAdjSource()).uid == this.mLastNonSystemFgUid) {
            if (DEBUG) {
                Slog.d(TAG, "Last top pkg " + this.mLastNonSystemFgPkg + " depend " + app);
            }
            return true;
        } else {
            return false;
        }
    }

    private void killTargets(int userId, ArrayList<String> targets, int killLevel, String reason) {
        if (targets.size() <= 0) {
            return;
        }
        try {
            long startTime = SystemClock.uptimeMillis();
            ArrayMap<Integer, List<String>> killList = new ArrayMap<>();
            killList.put(Integer.valueOf(killLevel), targets);
            ProcessConfig config = new ProcessConfig(10, userId, killList, reason);
            this.mPMS.kill(config);
            if (DEBUG) {
                Slog.d(TAG, "User" + userId + ": Clean " + targets.toString() + " for " + reason);
            }
            checkTime(startTime, "finish clean victim packages");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void checkTime(long startTime, String where) {
        long now = SystemClock.uptimeMillis();
        if (now - startTime > (DEBUG ? 50 : ScoutHelper.BINDER_FULL_KILL_SCORE_ADJ_MIN)) {
            Slog.w(TAG, "Slow operation: " + (now - startTime) + "ms so far, now at " + where);
        }
    }

    public void dumpFgLru(PrintWriter pw) {
        pw.println("\n---- Foreground-LRU ----");
        synchronized (this.mLock) {
            pw.println("Settings:");
            pw.print("  Enable=" + this.mEnable);
            pw.print(" Ready=" + this.mReady);
            pw.print(" Debug=" + DEBUG);
            pw.println(" LruLengthLimit=" + this.mLruActiveLength);
            pw.print("  AvailableLow=" + this.mPressureCacheThreshold[0] + "Kb");
            pw.print(" AvailableMin=" + this.mPressureCacheThreshold[1] + "Kb");
            pw.println(" AvailableCritical=" + this.mPressureCacheThreshold[2] + "Kb");
            pw.println("Package Usage LRU:");
            for (int i = this.mLruPackages.size() - 1; i >= 0; i--) {
                pw.println("  " + this.mLruPackages.get(i).toString());
            }
        }
        pw.println("---- End of Foreground-LRU ----");
    }

    public void dumpCleanHistory(PrintWriter pw) {
        pw.println("\n---- CleanHistory ----");
        synchronized (this.mCleanHistory) {
            int index = this.mCleanHistoryIndex - 1;
            SimpleDateFormat formater = new SimpleDateFormat("HH:mm:ss.SSS");
            int i = 0;
            while (true) {
                int i2 = HISTORY_SIZE;
                if (i >= i2) {
                    break;
                }
                int index2 = (index + i2) % i2;
                if (this.mCleanHistory[index2] == null) {
                    break;
                }
                pw.print("#" + i);
                pw.print(" " + formater.format(new Date(this.mCleanHistory[index2].mCleanTime)));
                pw.print(" " + pressureToString(this.mCleanHistory[index2].mPressure));
                pw.print(" " + this.mCleanHistory[index2].mAgingThresHold);
                pw.print(" " + this.mCleanHistory[index2].mReason);
                pw.println(" " + this.mCleanHistory[index2].mCleanList);
                index = index2 - 1;
                i++;
            }
        }
        pw.println("---- End of CleanHistory ----");
    }

    private String pressureToString(int pressure) {
        switch (pressure) {
            case -1:
                return EdgeSuppressionFactory.TYPE_NORMAL;
            case 0:
                return "low";
            case 1:
                return "min";
            case 2:
                return "critical";
            default:
                return "unkown";
        }
    }

    private static boolean isHomeOrRecents(String packageName) {
        return sHomeOrRecents.contains(packageName);
    }

    private static boolean isSystemPackage(String packageName) {
        return sSystemRelatedPkgs.contains(packageName) || isHomeOrRecents(packageName);
    }

    private static boolean isWhiteListPackage(String packageName) {
        return sCleanWhiteList.contains(packageName);
    }

    /* loaded from: classes.dex */
    private final class BinderService extends Binder {
        private BinderService() {
            PeriodicCleanerService.this = r1;
        }

        @Override // android.os.Binder
        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (!DumpUtils.checkDumpPermission(PeriodicCleanerService.this.mContext, PeriodicCleanerService.TAG, pw)) {
                return;
            }
            PeriodicCleanerService.this.dumpFgLru(pw);
            PeriodicCleanerService.this.dumpCleanHistory(pw);
        }

        public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) throws RemoteException {
            PeriodicCleanerService periodicCleanerService = PeriodicCleanerService.this;
            new PeriodicShellCmd(periodicCleanerService).exec(this, in, out, err, args, callback, resultReceiver);
        }
    }

    /* loaded from: classes.dex */
    private final class LocalService extends PeriodicCleanerInternal {
        private LocalService() {
            PeriodicCleanerService.this = r1;
        }

        @Override // com.android.server.am.PeriodicCleanerService.PeriodicCleanerInternal
        public void reportEvent(String packageName, String className, int uid, int eventType, boolean fullscreen) {
            MyEvent event = new MyEvent(packageName, className, uid, eventType, fullscreen);
            PeriodicCleanerService.this.mHandler.obtainMessage(1, event).sendToTarget();
        }

        @Override // com.android.server.am.PeriodicCleanerService.PeriodicCleanerInternal
        public void reportMemPressure(int pressureState) {
            PeriodicCleanerService.this.mHandler.obtainMessage(2, pressureState, 0).sendToTarget();
        }
    }

    /* loaded from: classes.dex */
    private class PeriodicShellCmd extends ShellCommand {
        PeriodicCleanerService mService;

        public PeriodicShellCmd(PeriodicCleanerService service) {
            PeriodicCleanerService.this = r1;
            this.mService = service;
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        public int onCommand(String cmd) {
            if (cmd == null) {
                return handleDefaultCommands(cmd);
            }
            PrintWriter pw = getOutPrintWriter();
            char c = 65535;
            try {
                switch (cmd.hashCode()) {
                    case -1298848381:
                        if (cmd.equals(MiuiCustomizeShortCutUtils.ATTRIBUTE_ENABLE)) {
                            c = 2;
                            break;
                        }
                        break;
                    case 3095028:
                        if (cmd.equals("dump")) {
                            c = 0;
                            break;
                        }
                        break;
                    case 94746185:
                        if (cmd.equals("clean")) {
                            c = 1;
                            break;
                        }
                        break;
                    case 95458899:
                        if (cmd.equals("debug")) {
                            c = 3;
                            break;
                        }
                        break;
                }
                switch (c) {
                    case 0:
                        this.mService.dumpFgLru(pw);
                        this.mService.dumpCleanHistory(pw);
                        break;
                    case 1:
                        runClean(pw);
                        break;
                    case 2:
                        boolean enable = Boolean.parseBoolean(getNextArgRequired());
                        this.mService.mEnable = enable;
                        pw.println("periodic cleaner enabled: " + enable);
                        break;
                    case 3:
                        boolean debug = Boolean.parseBoolean(getNextArgRequired());
                        PeriodicCleanerService.DEBUG = debug;
                        pw.println("periodic cleaner debug enabled: " + debug);
                        break;
                    default:
                        return handleDefaultCommands(cmd);
                }
            } catch (Exception e) {
                pw.println("Error occurred. Check logcat for details. " + e.getMessage());
                Slog.e(PeriodicCleanerService.TAG, "Error running shell command", e);
            }
            return 0;
        }

        private void runClean(PrintWriter pw) {
            int pressure;
            String opt = getNextOption();
            if (opt == null) {
                pw.println("trigger clean package by periodic");
                PeriodicCleanerService.this.cleanPackageByPeriodic();
                return;
            }
            char c = 65535;
            switch (opt.hashCode()) {
                case 1509:
                    if (opt.equals("-r")) {
                        c = 0;
                        break;
                    }
                    break;
            }
            switch (c) {
                case 0:
                    String reason = getNextArgRequired();
                    if ("periodic".equals(reason)) {
                        PeriodicCleanerService.this.cleanPackageByPeriodic();
                        return;
                    }
                    if ("pressure-low".equals(reason)) {
                        pressure = 0;
                    } else if ("pressure-min".equals(reason)) {
                        pressure = 1;
                    } else if ("pressure-critical".equals(reason)) {
                        pressure = 2;
                    } else {
                        pw.println("error: invalid reason: " + reason);
                        return;
                    }
                    pw.println("trigger clean package by " + reason);
                    PeriodicCleanerService.this.cleanPackageByPressure(pressure);
                    return;
                default:
                    pw.println("error: invalid option: " + opt);
                    onHelp();
                    return;
            }
        }

        public void onHelp() {
            PrintWriter pw = getOutPrintWriter();
            pw.println("Periodic Cleaner commands:");
            pw.println("  help");
            pw.println("    Print this help text.");
            pw.println("");
            pw.println("  dump");
            pw.println("    Print fg-lru and clean history.");
            pw.println("");
            pw.println("  clean [-r REASON]");
            pw.println("    Trigger clean action.");
            pw.println("      -r: select clean reason");
            pw.println("          REASON is one of:");
            pw.println("            periodic");
            pw.println("            pressure-low");
            pw.println("            pressure-min");
            pw.println("            pressure-critical");
            pw.println("          default reason is periodic if no REASON");
            pw.println("");
            pw.println("  enable [true|false]");
            pw.println("    Enable/Disable peridic cleaner.");
            pw.println("");
            pw.println("  debug [true|false]");
            pw.println("    Enable/Disable debug config.");
        }
    }

    /* loaded from: classes.dex */
    public class PackageUseInfo {
        private String mPackageName;
        private int mUid;
        private int mUserId;

        public PackageUseInfo(int uid, String packageName) {
            PeriodicCleanerService.this = r1;
            this.mPackageName = packageName;
            this.mUserId = UserHandle.getUserId(uid);
            this.mUid = uid;
        }

        public String toString() {
            return "u" + this.mUserId + "/" + UserHandle.getAppId(this.mUid) + ":" + this.mPackageName;
        }

        public void updateUid(int uid) {
            this.mUid = uid;
            this.mUserId = UserHandle.getUserId(uid);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof PackageUseInfo) {
                PackageUseInfo another = (PackageUseInfo) obj;
                if (this.mUid == another.mUid && this.mPackageName.equals(another.mPackageName)) {
                    return true;
                }
                return false;
            }
            return false;
        }
    }

    /* loaded from: classes.dex */
    public class MyHandler extends Handler {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public MyHandler(Looper looper) {
            super(looper);
            PeriodicCleanerService.this = r1;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    if (msg.obj instanceof MyEvent) {
                        PeriodicCleanerService.this.reportEvent((MyEvent) msg.obj);
                        return;
                    }
                    return;
                case 2:
                    PeriodicCleanerService.this.reportMemPressure(msg.arg1);
                    return;
                case 3:
                    PeriodicCleanerService.this.handleScreenOff();
                    return;
                default:
                    return;
            }
        }
    }

    /* loaded from: classes.dex */
    public class CleanInfo {
        private int mAgingThresHold;
        private long mCleanTime;
        private int mPressure;
        private String mReason;
        private boolean mValid = false;
        private SparseArray<List<String>> mCleanList = new SparseArray<>();

        CleanInfo(long currentTime, int pressure, int agingThresHold, String reason) {
            PeriodicCleanerService.this = r1;
            this.mCleanTime = currentTime;
            this.mPressure = pressure;
            this.mAgingThresHold = agingThresHold;
            this.mReason = reason;
        }

        public void addCleanList(int userId, List<String> list) {
            if (this.mCleanList.get(userId) != null) {
                Slog.e(PeriodicCleanerService.TAG, "Already exists old mapping for user " + userId);
            }
            this.mCleanList.put(userId, list);
            this.mValid = true;
        }

        public boolean isValid() {
            return this.mValid;
        }

        public String toString() {
            return "[" + this.mCleanTime + ": " + this.mCleanList + "]";
        }
    }

    /* loaded from: classes.dex */
    public class MyEvent {
        public String mClass;
        public int mEventType;
        public boolean mFullScreen;
        public String mPackage;
        public int mUid;

        public MyEvent(String packageName, String className, int uid, int event, boolean fullscreen) {
            PeriodicCleanerService.this = r1;
            this.mPackage = packageName;
            this.mClass = className;
            this.mEventType = event;
            this.mFullScreen = fullscreen;
            this.mUid = uid;
        }
    }

    public static boolean isHomeOrRecentsToKeepAlive(String packageName) {
        return sHomeOrRecents.contains(packageName);
    }
}
