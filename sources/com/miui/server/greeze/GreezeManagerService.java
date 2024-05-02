package com.miui.server.greeze;

import android.app.ActivityManager;
import android.app.IAlarmManager;
import android.app.IProcessObserver;
import android.app.IUidObserver;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.hardware.display.DisplayManagerInternal;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.INetworkManagementService;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.Process;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ServiceSpecificException;
import android.os.ShellCallback;
import android.os.ShellCommand;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.server.am.SplitScreenReporter;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.LocalLog;
import android.util.Log;
import android.util.Singleton;
import android.util.Slog;
import android.util.SparseArray;
import android.view.IWindowManager;
import android.view.WindowManagerGlobal;
import com.android.internal.app.ProcessMap;
import com.android.internal.util.DumpUtils;
import com.android.server.LocalServices;
import com.android.server.ScoutHelper;
import com.android.server.ServiceThread;
import com.android.server.am.ActivityManagerService;
import com.android.server.inputmethod.InputMethodManagerServiceImpl;
import com.android.server.power.PowerManagerServiceStub;
import com.miui.server.AccessController;
import com.miui.server.input.util.MiuiCustomizeShortCutUtils;
import com.miui.server.smartpower.PowerFrozenManager;
import java.io.File;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToIntFunction;
import miui.greeze.IGreezeCallback;
import miui.greeze.IGreezeManager;
import miui.greeze.IMonitorToken;
import miui.process.ActiveUidInfo;
import miui.process.ForegroundInfo;
import miui.process.ProcessManager;
/* loaded from: classes.dex */
public class GreezeManagerService extends IGreezeManager.Stub {
    public static final int BINDER_STATE_IN_BUSY = 1;
    public static final int BINDER_STATE_IN_IDLE = 0;
    public static final int BINDER_STATE_IN_TRANSACTION = 4;
    public static final int BINDER_STATE_PROC_IN_BUSY = 3;
    public static final int BINDER_STATE_THREAD_IN_BUSY = 2;
    private static final String CLOUD_AUROGON_ALARM_ALLOW_LIST = "cloud_aurogon_alarm_allow_list";
    private static final String CLOUD_GREEZER_ENABLE = "cloud_greezer_enable";
    public static boolean DEBUG = false;
    private static boolean DEBUG_AIDL = false;
    public static boolean DEBUG_LAUNCH_FROM_HOME = false;
    private static boolean DEBUG_MILLET = false;
    private static final boolean DEBUG_MONKEY;
    public static boolean DEBUG_SKIPUID = false;
    private static final int DUMPSYS_HISTORY_DURATION = 14400000;
    private static final int HISTORY_SIZE;
    private static final Singleton<IGreezeManager> IGreezeManagerSingleton;
    private static final int MAX_HISTORY_ITEMS = 4096;
    private static final long MILLET_DELAY_CEILING = 10000;
    private static final long MILLET_DELAY_THRASHOLD = 50;
    private static final int MILLET_MONITOR_ALL = 7;
    private static final int MILLET_MONITOR_BINDER = 1;
    private static final int MILLET_MONITOR_NET = 4;
    private static final int MILLET_MONITOR_SIGNAL = 2;
    static final String PROPERTY_GZ_CGROUPV1 = "persist.sys.millet.cgroup1";
    static final String PROPERTY_GZ_DEBUG = "persist.sys.gz.debug";
    static final String PROPERTY_GZ_HANDSHAKE = "persist.sys.millet.handshake";
    static final String PROPERTY_GZ_MONKEY = "persist.sys.gz.monkey";
    private static final String PROP_POWERMILLET_ENABLE = "persist.sys.powmillet.enable";
    public static final String SERVICE_NAME = "greezer";
    public static final String TAG = "GreezeManager";
    private static final String TIME_FORMAT_PATTERN = "HH:mm:ss.SSS";
    static HashMap<Integer, IGreezeCallback> callbacks;
    public static boolean mCgroupV1Flag;
    private ConnectivityManager cm;
    private ActivityManager mActivityManager;
    private final ActivityManagerService mActivityManagerService;
    final ContentObserver mCloudAurogonAlarmListObserver;
    private Context mContext;
    DisplayManagerInternal mDisplayManagerInternal;
    private Method mGetCastPid;
    public Handler mHandler;
    public AurogonImmobulusMode mImmobulusMode;
    private final IProcessObserver mProcessObserver;
    private int mRegisteredMonitor;
    private final IUidObserver mUidObserver;
    private Object powerManagerServiceImpl;
    private static final String PROPERTY_GZ_ENABLE = "persist.sys.gz.enable";
    private static boolean sEnable = SystemProperties.getBoolean(PROPERTY_GZ_ENABLE, false);
    static final String PROPERTY_GZ_FZTIMEOUT = "persist.sys.gz.fztimeout";
    private static final long LOOPONCE_DELAY_TIME = 5000;
    public static final long LAUNCH_FZ_TIMEOUT = SystemProperties.getLong(PROPERTY_GZ_FZTIMEOUT, (long) LOOPONCE_DELAY_TIME);
    private static boolean milletEnable = false;
    public boolean mPowerMilletEnable = SystemProperties.getBoolean(PROP_POWERMILLET_ENABLE, false);
    private List<String> ENABLE_LAUNCH_MODE_DEVICE = new ArrayList(Arrays.asList("thor", "dagu", "zizhan", "unicorn", "mayfly", "cupid", "zeus", "nuwa", "fuxi", "socrates"));
    private final SparseArray<FrozenInfo> mFrozenPids = new SparseArray<>();
    private String mAudioZeroPkgs = "com.tencent.lolm, com.netease.dwrg, com.tencent.tmgp.pubgm, com.tencent.tmgp.dwrg, com.tencent.tmgp.pubgmhd, com.tencent.tmgp.sgame, com.netease.dwrg.mi";
    private FrozenInfo[] mFrozenHistory = new FrozenInfo[HISTORY_SIZE];
    private int mHistoryIndexNext = 0;
    public Object mAurogonLock = new Object();
    private boolean mInited = false;
    private final SparseArray<IMonitorToken> mMonitorTokens = new SparseArray<>();
    private INetworkManagementService mNms = null;
    private int mAppFrontUid = -1;
    public String mFGOrderBroadcastAction = "";
    public String mBGOrderBroadcastAction = "";
    public int mFGOrderBroadcastAppUid = -1;
    public int mBGOrderBroadcastAppUid = -1;
    private LocalLog mHistoryLog = new LocalLog(4096);
    public int mTopAppUid = -1;
    private PackageManager mPm = null;
    private List<String> mAurogonAlarmAllowList = new ArrayList(Arrays.asList("com.tencent.mm", "com.tencent.mobileqq"));
    IAlarmManager mAlarmManager = null;
    public IWindowManager mWindowManager = null;
    public boolean mScreenOnOff = true;
    public Map<Integer, List<Integer>> mFgAppUidList = new HashMap();
    public List<String> mBroadcastIntentDenyList = new ArrayList(Arrays.asList("android.intent.action.BATTERY_CHANGED", "android.net.wifi.STATE_CHANGE", "android.intent.action.DROPBOX_ENTRY_ADDED", "android.net.wifi.RSSI_CHANGED", "android.net.wifi.supplicant.STATE_CHANGE", "com.android.server.action.NETWORK_STATS_UPDATED", "android.intent.action.CLOSE_SYSTEM_DIALOGS", "android.intent.action.TIME_TICK", "android.net.conn.CONNECTIVITY_CHANGE", "android.net.wifi.WIFI_STATE_CHANGED"));
    public List<Integer> mExcuteServiceList = new ArrayList();
    private boolean isBarExpand = false;
    private int mSystemUiPid = -1;
    private ServiceThread mThread = GreezeThread.getInstance();

    private static native void nAddConcernedUid(int i);

    private static native void nClearConcernedUid();

    private static native void nDelConcernedUid(int i);

    public static native void nLoopOnce();

    private static native void nQueryBinder(int i);

    static {
        boolean z = SystemProperties.getBoolean(PROPERTY_GZ_MONKEY, false);
        DEBUG_MONKEY = z;
        boolean z2 = SystemProperties.getBoolean(PROPERTY_GZ_DEBUG, false);
        DEBUG = z2;
        DEBUG_LAUNCH_FROM_HOME = z2;
        DEBUG_AIDL = z2;
        DEBUG_MILLET = z2;
        DEBUG_SKIPUID = z2;
        HISTORY_SIZE = z ? 16384 : 4096;
        mCgroupV1Flag = false;
        IGreezeManagerSingleton = new Singleton<IGreezeManager>() { // from class: com.miui.server.greeze.GreezeManagerService.2
            public IGreezeManager create() {
                IBinder b = ServiceManager.getService(GreezeManagerService.SERVICE_NAME);
                IGreezeManager service = IGreezeManager.Stub.asInterface(b);
                return service;
            }
        };
        callbacks = new HashMap<>();
    }

    private void registerCloudObserver(final Context context) {
        ContentObserver observer = new ContentObserver(this.mHandler) { // from class: com.miui.server.greeze.GreezeManagerService.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                if (uri == null) {
                    return;
                }
                if (uri.equals(Settings.System.getUriFor(GreezeManagerService.CLOUD_GREEZER_ENABLE))) {
                    GreezeManagerService.sEnable = Boolean.parseBoolean(Settings.System.getStringForUser(context.getContentResolver(), GreezeManagerService.CLOUD_GREEZER_ENABLE, -2));
                    Slog.w(GreezeManagerService.TAG, "cloud control set received :" + GreezeManagerService.sEnable);
                } else if (uri.equals(Settings.Global.getUriFor("zeropkgs"))) {
                    GreezeManagerService.this.mAudioZeroPkgs = Settings.Global.getStringForUser(context.getContentResolver(), "zeropkgs", -2);
                    Slog.w(GreezeManagerService.TAG, "mAudioZeroPkgs :" + GreezeManagerService.this.mAudioZeroPkgs);
                }
            }
        };
        context.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_GREEZER_ENABLE), false, observer, -2);
        context.getContentResolver().registerContentObserver(Settings.Global.getUriFor("zeropkgs"), false, observer, -2);
        String data = Settings.Global.getStringForUser(context.getContentResolver(), "zeropkgs", -2);
        if (data != null && data.length() > 3) {
            this.mAudioZeroPkgs = data;
        }
    }

    private GreezeManagerService(Context context) {
        this.mImmobulusMode = null;
        this.cm = null;
        IUidObserver.Stub stub = new IUidObserver.Stub() { // from class: com.miui.server.greeze.GreezeManagerService.8
            public void onUidStateChanged(int uid, int procState, long procStateSeq, int capability) {
            }

            public void onUidActive(final int uid) {
                GreezeManagerService.this.mHandler.postAtFrontOfQueue(new Runnable() { // from class: com.miui.server.greeze.GreezeManagerService.8.1
                    @Override // java.lang.Runnable
                    public void run() {
                        GreezeManagerService.this.mImmobulusMode.notifyAppActive(uid);
                    }
                });
            }

            public void onUidGone(int uid, boolean disabled) {
            }

            public void onUidIdle(int uid, boolean disabled) {
            }

            public void onUidCachedChanged(int uid, boolean cached) {
            }

            public void onUidProcAdjChanged(int uid) {
            }
        };
        this.mUidObserver = stub;
        IProcessObserver.Stub stub2 = new IProcessObserver.Stub() { // from class: com.miui.server.greeze.GreezeManagerService.9
            public void onForegroundActivitiesChanged(int pid, final int uid, boolean foregroundActivities) {
                if (!GreezeManagerService.this.mInited) {
                    return;
                }
                if (foregroundActivities) {
                    Slog.d("Aurogon", " uid = " + uid + " switch to FG");
                    if (GreezeManagerService.this.isUidFrozen(uid)) {
                        GreezeManagerService.this.thawUid(uid, 1000, "FG activity");
                    }
                    GreezeManagerService.this.updateFgAppList(uid, pid, true);
                    GreezeManagerService.this.mHandler.post(new Runnable() { // from class: com.miui.server.greeze.GreezeManagerService.9.1
                        @Override // java.lang.Runnable
                        public void run() {
                            GreezeManagerService.this.updateAurogonUidRule(uid, false);
                            GreezeManagerService.this.checkFgAppList(true);
                            if (GreezeManagerService.this.mImmobulusMode.mEnterIMCamera && uid != GreezeManagerService.this.mImmobulusMode.mCameraUid) {
                                GreezeManagerService.this.mImmobulusMode.triggerImmobulusMode(false);
                                GreezeManagerService.this.mImmobulusMode.mEnterIMCamera = false;
                            }
                        }
                    });
                    return;
                }
                GreezeManagerService.this.updateFgAppList(uid, -1, false);
                Slog.d("Aurogon", " uid = " + uid + " switch to BG");
                GreezeManagerService.this.checkFgAppList(false);
                GreezeManagerService.this.mHandler.post(new Runnable() { // from class: com.miui.server.greeze.GreezeManagerService.9.2
                    @Override // java.lang.Runnable
                    public void run() {
                        GreezeManagerService.this.mImmobulusMode.notifyAppSwitchToBg(uid);
                    }
                });
            }

            public void onForegroundServicesChanged(final int pid, final int uid, int serviceTypes) {
                if (GreezeManagerService.this.mImmobulusMode.getAurogonAppInfo(uid) == null) {
                    return;
                }
                GreezeManagerService.this.mHandler.post(new Runnable() { // from class: com.miui.server.greeze.GreezeManagerService.9.3
                    @Override // java.lang.Runnable
                    public void run() {
                        GreezeManagerService.this.mImmobulusMode.notifyFgServicesChanged(pid, uid);
                    }
                });
            }

            public void onProcessDied(int pid, int uid) {
                GreezeManagerService.this.updateFgAppList(uid, pid, false);
            }
        };
        this.mProcessObserver = stub2;
        this.mDisplayManagerInternal = null;
        this.mCloudAurogonAlarmListObserver = new ContentObserver(this.mHandler) { // from class: com.miui.server.greeze.GreezeManagerService.12
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                String str;
                if (uri != null && uri.equals(Settings.System.getUriFor(GreezeManagerService.CLOUD_AUROGON_ALARM_ALLOW_LIST)) && (str = Settings.System.getStringForUser(GreezeManagerService.this.mContext.getContentResolver(), GreezeManagerService.CLOUD_AUROGON_ALARM_ALLOW_LIST, -2)) != null) {
                    String[] pkgName = str.split(",");
                    if (pkgName.length > 0) {
                        GreezeManagerService.this.mAurogonAlarmAllowList.clear();
                        for (String name : pkgName) {
                            GreezeManagerService.this.mAurogonAlarmAllowList.add(name);
                        }
                    }
                }
            }
        };
        this.mContext = context;
        H h = new H(this.mThread.getLooper());
        this.mHandler = h;
        h.sendEmptyMessage(3);
        ActivityManagerService service = ActivityManager.getService();
        this.mActivityManagerService = service;
        this.mActivityManager = (ActivityManager) this.mContext.getSystemService("activity");
        if (sEnable) {
            registerCloudObserver(context);
            if (Settings.System.getStringForUser(context.getContentResolver(), CLOUD_GREEZER_ENABLE, -2) != null) {
                sEnable = Boolean.parseBoolean(Settings.System.getStringForUser(context.getContentResolver(), CLOUD_GREEZER_ENABLE, -2));
            }
        }
        registerObserverForAurogon();
        getWindowManagerService();
        getAlarmManagerService();
        try {
            service.registerProcessObserver(stub2);
            service.registerUidObserver(stub, 14, -1, (String) null);
        } catch (Exception e) {
        }
        this.mDisplayManagerInternal = (DisplayManagerInternal) LocalServices.getService(DisplayManagerInternal.class);
        new AurogonBroadcastReceiver();
        this.mImmobulusMode = new AurogonImmobulusMode(this.mContext, this.mThread, this);
        this.cm = (ConnectivityManager) this.mContext.getSystemService(ConnectivityManager.class);
    }

    private void sendMilletLoop() {
        this.mHandler.sendEmptyMessageDelayed(4, LOOPONCE_DELAY_TIME);
    }

    public static GreezeManagerService getService() {
        return (GreezeManagerService) IGreezeManagerSingleton.get();
    }

    public static void startService(Context context) {
        ServiceManager.addService(SERVICE_NAME, new GreezeManagerService(context));
        if (getService() != null && SystemProperties.getBoolean(PROPERTY_GZ_HANDSHAKE, false)) {
            getService().sendMilletLoop();
            mCgroupV1Flag = SystemProperties.getBoolean(PROPERTY_GZ_CGROUPV1, false);
        }
    }

    public void checkPermission() {
        int uid = Binder.getCallingUid();
        if (!UserHandle.isApp(uid)) {
            return;
        }
        throw new SecurityException("Uid " + uid + " does not have permission to greezer");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class MonitorDeathRecipient implements IBinder.DeathRecipient {
        IMonitorToken mMonitorToken;
        int mType;

        MonitorDeathRecipient(IMonitorToken token, int type) {
            GreezeManagerService.this = this$0;
            this.mMonitorToken = token;
            this.mType = type;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            synchronized (GreezeManagerService.this.mMonitorTokens) {
                GreezeManagerService.this.mMonitorTokens.remove(this.mType);
            }
            GreezeManagerService.this.mHandler.sendEmptyMessage(3);
            int i = this.mType;
            if (i == 1) {
                GreezeManagerService.this.mRegisteredMonitor &= -2;
            } else if (i == 2) {
                GreezeManagerService.this.mRegisteredMonitor &= -3;
            } else if (i == 3) {
                GreezeManagerService.this.mRegisteredMonitor &= -5;
            }
            PowerFrozenManager.getInstance().serviceReady(false);
            if ((GreezeManagerService.this.mRegisteredMonitor & 7) != 7) {
                GreezeManagerService.milletEnable = false;
                for (IGreezeCallback callback : GreezeManagerService.callbacks.values()) {
                    try {
                        callback.serviceReady(GreezeManagerService.milletEnable);
                    } catch (RemoteException e) {
                    }
                }
            }
            Slog.w(GreezeManagerService.TAG, "Monitor (type " + this.mType + ") died, gz stop");
            if (this.mType == 2) {
                GreezeManagerService.this.mHandler.postDelayed(new Runnable() { // from class: com.miui.server.greeze.GreezeManagerService.MonitorDeathRecipient.1
                    @Override // java.lang.Runnable
                    public void run() {
                        GreezeManagerService.this.mHandler.sendEmptyMessage(4);
                    }
                }, 200L);
            }
        }
    }

    public boolean registerMonitor(IMonitorToken token, int type) throws RemoteException {
        checkPermission();
        Slog.i(TAG, "Monitor registered, type " + type + " pid " + getCallingPid());
        this.mHandler.sendEmptyMessage(3);
        synchronized (this.mMonitorTokens) {
            this.mMonitorTokens.put(type, token);
            token.asBinder().linkToDeath(new MonitorDeathRecipient(token, type), 0);
        }
        if (type == 1) {
            this.mRegisteredMonitor |= 1;
        } else if (type == 2) {
            this.mRegisteredMonitor |= 2;
        } else if (type == 3) {
            this.mRegisteredMonitor |= 4;
        }
        if ((this.mRegisteredMonitor & 7) == 7) {
            Slog.i(TAG, "All monitors registered, about to loop once");
        }
        return true;
    }

    private FrozenInfo getFrozenInfo(int uid) {
        FrozenInfo info = null;
        synchronized (this.mFrozenPids) {
            int i = 0;
            while (true) {
                if (i < this.mFrozenPids.size()) {
                    FrozenInfo frozen = this.mFrozenPids.valueAt(i);
                    if (frozen == null || frozen.uid != uid) {
                        i++;
                    } else {
                        info = frozen;
                        break;
                    }
                } else {
                    break;
                }
            }
        }
        return info;
    }

    public void reportSignal(int uid, int pid, long now) {
        checkPermission();
        PowerFrozenManager.getInstance().reportSignal(uid, pid, now);
        long delay = SystemClock.uptimeMillis() - now;
        String msg = "Receive frozen signal: uid=" + uid + " pid=" + pid + " delay=" + delay + "ms";
        if (DEBUG_MILLET) {
            Slog.i(TAG, msg);
        }
        if (delay > MILLET_DELAY_THRASHOLD && delay < 10000) {
            Slog.w(TAG, "Slow Greezer: " + msg);
        }
        synchronized (this.mFrozenPids) {
            FrozenInfo info = getFrozenInfo(uid);
            if (info == null) {
                String isolatedPid = "";
                if (mCgroupV1Flag && UserHandle.isApp(uid)) {
                    FreezeUtils.thawPid(pid);
                    isolatedPid = " isolatedPid = " + pid;
                }
                if (DEBUG) {
                    Slog.w(TAG, "reportSignal null uid = " + uid + isolatedPid);
                }
                return;
            }
            int owner = info.getOwner();
            if (this.mPowerMilletEnable) {
                thawUid(info.uid, 1000, "Signal");
                return;
            }
            for (Map.Entry<Integer, IGreezeCallback> item : callbacks.entrySet()) {
                if (item.getKey().intValue() == owner) {
                    item.getValue().reportSignal(uid, pid, now);
                    return;
                }
                continue;
            }
        }
    }

    public void reportNet(int uid, long now) {
        checkPermission();
        PowerFrozenManager.getInstance().reportNet(uid, now);
        long delay = SystemClock.uptimeMillis() - now;
        String msg = "Receive frozen pkg net: uid=" + uid + " delay=" + delay + "ms";
        if (DEBUG_MILLET) {
            Slog.i(TAG, msg);
        }
        if (delay > MILLET_DELAY_THRASHOLD && delay < 10000) {
            Slog.w(TAG, "Slow Greezer: " + msg);
        }
        synchronized (this.mFrozenPids) {
            FrozenInfo info = getFrozenInfo(uid);
            if (info == null) {
                try {
                    callbacks.get(1).reportNet(uid, now);
                } catch (RemoteException e) {
                }
                if (DEBUG) {
                    Slog.w(TAG, "reportNet null uid = " + uid);
                }
                return;
            }
            int owner = info.getOwner();
            if (this.mPowerMilletEnable) {
                if (info.isFrozenByLaunchMode) {
                    this.mImmobulusMode.addLaunchModeQiutList(uid);
                } else {
                    thawUid(uid, 1000, "PACKET");
                }
            }
            for (Map.Entry<Integer, IGreezeCallback> item : callbacks.entrySet()) {
                if (item.getKey().intValue() == owner) {
                    item.getValue().reportNet(uid, now);
                    return;
                }
                continue;
            }
        }
    }

    public void reportBinderTrans(int dstUid, int dstPid, int callerUid, int callerPid, int callerTid, boolean isOneway, long now, int buffer) throws RemoteException {
        Throwable th;
        long delay;
        String msg;
        FrozenInfo info;
        int owner;
        checkPermission();
        PowerFrozenManager.getInstance().reportBinderTrans(dstUid, dstPid, callerUid, callerPid, callerTid, isOneway, now, buffer);
        long delay2 = SystemClock.uptimeMillis() - now;
        String msg2 = "Receive frozen binder trans: dstUid=" + dstUid + " dstPid=" + dstPid + " callerUid=" + callerUid + " callerPid=" + callerPid + " callerTid=" + callerTid + " delay=" + delay2 + "ms oneway=" + isOneway;
        if (DEBUG_MILLET) {
            Slog.i(TAG, msg2);
        }
        if (delay2 > MILLET_DELAY_THRASHOLD && delay2 < 10000) {
            Slog.w(TAG, "Slow Greezer: " + msg2);
        }
        synchronized (this.mFrozenPids) {
            try {
                FrozenInfo info2 = getFrozenInfo(dstUid);
                if (info2 == null) {
                    try {
                        if (DEBUG) {
                            Slog.w(TAG, "reportBinderTrans null uid = " + dstUid);
                        }
                        return;
                    } catch (Throwable th2) {
                        th = th2;
                    }
                } else {
                    try {
                        int owner2 = info2.getOwner();
                        try {
                            if (callerPid == this.mSystemUiPid && this.isBarExpand) {
                                thawUid(info2.uid, 1000, "UI bar");
                                return;
                            }
                            int i = buffer;
                            if (i == 0 && this.mPowerMilletEnable) {
                                thawUid(info2.uid, 1000, "BF");
                                return;
                            } else if (!isOneway && this.mPowerMilletEnable) {
                                thawUid(info2.uid, 1000, "Sync Binder");
                                return;
                            } else {
                                for (Map.Entry<Integer, IGreezeCallback> item : callbacks.entrySet()) {
                                    try {
                                        if (item.getKey().intValue() == owner2) {
                                            owner = owner2;
                                            info = info2;
                                            msg = msg2;
                                            delay = delay2;
                                            try {
                                                item.getValue().reportBinderTrans(dstUid, dstPid, callerUid, callerPid, callerTid, isOneway, now, i);
                                                return;
                                            } catch (RemoteException e) {
                                            }
                                        } else {
                                            owner = owner2;
                                            info = info2;
                                            msg = msg2;
                                            delay = delay2;
                                        }
                                    } catch (RemoteException e2) {
                                        owner = owner2;
                                        info = info2;
                                        msg = msg2;
                                        delay = delay2;
                                    }
                                    i = buffer;
                                    owner2 = owner;
                                    info2 = info;
                                    msg2 = msg;
                                    delay2 = delay;
                                }
                                return;
                            }
                        } catch (Throwable th3) {
                            th = th3;
                        }
                    } catch (Throwable th4) {
                        th = th4;
                    }
                }
            } catch (Throwable th5) {
                th = th5;
            }
            while (true) {
                try {
                    break;
                } catch (Throwable th6) {
                    th = th6;
                }
            }
            throw th;
        }
    }

    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:45:0x011b -> B:46:0x011c). Please submit an issue!!! */
    public void reportBinderState(int uid, int pid, int tid, int binderState, long now) {
        Throwable th;
        checkPermission();
        PowerFrozenManager.getInstance().reportBinderState(uid, pid, tid, binderState, now);
        long delay = SystemClock.uptimeMillis() - now;
        String msg = "Receive binder state: uid=" + uid + " pid=" + pid + " tid=" + tid + " delay=" + delay + "ms binderState=" + stateToString(binderState);
        if (DEBUG_MILLET) {
            Slog.i(TAG, msg);
        }
        if (delay > MILLET_DELAY_THRASHOLD && delay < 10000) {
            Slog.w(TAG, "Slow Greezer: " + msg);
        }
        synchronized (this.mFrozenPids) {
            try {
                FrozenInfo info = getFrozenInfo(uid);
                try {
                    if (info == null) {
                        if (DEBUG) {
                            Slog.w(TAG, "reportBinderState null uid = " + uid);
                        }
                        return;
                    }
                    info.getOwner();
                    try {
                        boolean state = (info.state & 2) != 0;
                        if (binderState == 1 && this.mPowerMilletEnable) {
                            if (!this.mImmobulusMode.isRunningLaunchMode()) {
                                thawUid(uid, 1000, "Check Binder");
                            } else {
                                this.mImmobulusMode.addLaunchModeQiutList(uid);
                            }
                        }
                        try {
                            if (callbacks.containsKey(1) && state) {
                                callbacks.get(1).reportBinderState(uid, pid, tid, binderState, now);
                            }
                        } catch (RemoteException e) {
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                }
            } catch (Throwable th4) {
                th = th4;
            }
        }
    }

    public void reportLoopOnce() {
        checkPermission();
        PowerFrozenManager.getInstance().serviceReady(true);
        if (DEBUG_MILLET) {
            Slog.i(TAG, "Receive millet loop once msg");
        }
        if ((this.mRegisteredMonitor & 7) == 7) {
            milletEnable = true;
            Slog.i(TAG, "Receive millet loop once, gz begin to work");
            for (IGreezeCallback callback : callbacks.values()) {
                try {
                    callback.serviceReady(milletEnable);
                } catch (RemoteException e) {
                }
            }
            return;
        }
        Slog.i(TAG, "Receive millet loop once, but monitor not ready");
    }

    public boolean registerCallback(IGreezeCallback callback, int module) throws RemoteException {
        checkPermission();
        if (Binder.getCallingUid() != 1000) {
            return false;
        }
        if (callbacks.getOrDefault(Integer.valueOf(module), null) != null) {
            Slog.i(TAG, "Already registed callback for module: " + module);
        }
        if (module == 4 || module == 3) {
            return false;
        }
        callbacks.put(Integer.valueOf(module), callback);
        callback.asBinder().linkToDeath(new CallbackDeathRecipient(module), 0);
        callback.serviceReady(milletEnable);
        return true;
    }

    public long getLastThawedTime(int uid, int module) {
        List<FrozenInfo> historyInfos = getHistoryInfos(System.currentTimeMillis() - 14400000);
        for (FrozenInfo info : historyInfos) {
            if (info.uid == uid) {
                return info.mThawUptime;
            }
        }
        return -1L;
    }

    public boolean isUidFrozen(int uid) {
        synchronized (this.mFrozenPids) {
            int[] frozenUids = getFrozenUids(9999);
            for (int frozenuid : frozenUids) {
                if (frozenuid == uid) {
                    return true;
                }
            }
            return false;
        }
    }

    /* loaded from: classes.dex */
    public class CallbackDeathRecipient implements IBinder.DeathRecipient {
        int module;

        CallbackDeathRecipient(int module) {
            GreezeManagerService.this = this$0;
            this.module = module;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            GreezeManagerService.callbacks.remove(Integer.valueOf(this.module));
            int[] frozenUids = GreezeManagerService.this.getFrozenUids(this.module);
            for (int frozenuid : frozenUids) {
                GreezeManagerService.this.updateAurogonUidRule(frozenuid, false);
            }
            Slog.i(GreezeManagerService.TAG, "module: " + this.module + " has died!");
            GreezeManagerService greezeManagerService = GreezeManagerService.this;
            int i = this.module;
            greezeManagerService.thawAll(i, i, "module died");
        }
    }

    public static String stateToString(int state) {
        switch (state) {
            case 0:
                return "BINDER_IN_IDLE";
            case 1:
                return "BINDER_IN_BUSY";
            case 2:
                return "BINDER_THREAD_IN_BUSY";
            case 3:
                return "BINDER_PROC_IN_BUSY";
            case 4:
                return "BINDER_IN_TRANSACTION";
            default:
                return Integer.toString(state);
        }
    }

    private void setWakeLockState(List<Integer> ls, boolean disable) {
        for (Integer uid : ls) {
            if (!Process.isIsolated(uid.intValue())) {
                try {
                    PowerManagerServiceStub.get().setUidPartialWakeLockDisabledState(uid.intValue(), (String) null, disable);
                } catch (Exception e) {
                    Log.e(TAG, "updateWakelockBlockedUid", e);
                }
            }
        }
    }

    public void monitorNet(int uid) {
        nAddConcernedUid(uid);
    }

    public void clearMonitorNet(int uid) {
        nDelConcernedUid(uid);
    }

    public void clearMonitorNet() {
        nClearConcernedUid();
    }

    public void queryBinderState(int uid) {
        nQueryBinder(uid);
    }

    List<RunningProcess> getProcessByUid(int uid) {
        List<RunningProcess> procs = GreezeServiceUtils.getUidMap().get(uid);
        if (procs != null) {
            return procs;
        }
        return new ArrayList();
    }

    RunningProcess getProcessByPid(int pid) {
        List<RunningProcess> procs = GreezeServiceUtils.getProcessList();
        for (RunningProcess proc : procs) {
            if (pid == proc.pid) {
                return proc;
            }
        }
        return null;
    }

    ProcessMap<List<RunningProcess>> getPkgMap() {
        String[] strArr;
        ProcessMap<List<RunningProcess>> map = new ProcessMap<>();
        List<RunningProcess> procList = GreezeServiceUtils.getProcessList();
        for (RunningProcess proc : procList) {
            int uid = proc.uid;
            if (proc.pkgList != null) {
                for (String packageName : proc.pkgList) {
                    List<RunningProcess> procs = (List) map.get(packageName, uid);
                    if (procs == null) {
                        procs = new ArrayList<>();
                        map.put(packageName, uid, procs);
                    }
                    procs.add(proc);
                }
            }
        }
        return map;
    }

    public boolean isUidActive(int uid) {
        try {
            List<ActiveUidInfo> infos = ProcessManager.getActiveUidInfo(3);
            for (ActiveUidInfo info : infos) {
                if (info.uid == uid) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            Slog.w(TAG, "Failed to get active audio info. Going to freeze uid" + uid + " regardless of whether it using audio", e);
            return true;
        }
    }

    public void freezeThread(int tid) {
        if (mCgroupV1Flag) {
            FreezeUtils.freezeTid(tid);
        }
    }

    public boolean freezeProcess(RunningProcess proc, long timeout, int fromWho, String reason) {
        boolean done;
        int pid = proc.pid;
        if (pid <= 0 || Process.myPid() == pid || Process.getUidForPid(pid) != proc.uid) {
            return false;
        }
        if (mCgroupV1Flag) {
            done = FreezeUtils.freezePid(pid);
        } else {
            done = FreezeUtils.freezePid(pid, proc.uid);
        }
        PowerFrozenManager.getInstance().addFrozenPid(proc.uid, pid);
        synchronized (this.mFrozenPids) {
            FrozenInfo info = this.mFrozenPids.get(pid);
            if (info == null) {
                info = new FrozenInfo(proc);
                this.mFrozenPids.put(pid, info);
            }
            info.addFreezeInfo(System.currentTimeMillis(), fromWho, reason);
            if (!mCgroupV1Flag && this.mAudioZeroPkgs.contains(proc.processName)) {
                info.isAudioZero = true;
                stopAudio(proc.uid, pid, true);
                Slog.d(TAG, "Audio pause pid:" + pid);
            }
            if (this.mHandler.hasMessages(1, info)) {
                this.mHandler.removeMessages(1, info);
            }
            if (timeout != 0) {
                Message msg = this.mHandler.obtainMessage(1, info);
                msg.arg1 = pid;
                msg.arg2 = fromWho;
                this.mHandler.sendMessageDelayed(msg, timeout);
            }
        }
        return done;
    }

    public List<Integer> freezePids(int[] pids, long timeout, int fromWho, String reason) {
        int i;
        List<RunningProcess> procs;
        int[] iArr = pids;
        checkPermission();
        if (DEBUG_AIDL) {
            Slog.d(TAG, "AIDL freezePids(" + Arrays.toString(pids) + ", " + timeout + ", " + fromWho + ", " + reason + ")");
        }
        if (iArr == null) {
            return new ArrayList();
        }
        List<RunningProcess> procs2 = GreezeServiceUtils.getProcessList();
        List<Integer> result = new ArrayList<>();
        int length = iArr.length;
        int i2 = 0;
        while (i2 < length) {
            int pid = iArr[i2];
            RunningProcess target = null;
            for (RunningProcess proc : procs2) {
                if (pid == proc.pid) {
                    target = proc;
                }
            }
            if (target == null) {
                Slog.w(TAG, "Failed to freeze invalid pid " + pid);
                i = i2;
                procs = procs2;
            } else {
                RunningProcess target2 = target;
                procs = procs2;
                i = i2;
                if (!freezeProcess(target, timeout, fromWho, reason)) {
                    if (DEBUG_AIDL) {
                        Slog.d(TAG, "AIDL freezePid(" + pid + ", " + timeout + ", " + fromWho + ", " + reason + ") failed!");
                    }
                } else {
                    result.add(Integer.valueOf(target2.pid));
                }
            }
            i2 = i + 1;
            iArr = pids;
            procs2 = procs;
        }
        if (DEBUG_AIDL && !mCgroupV1Flag) {
            Slog.d(TAG, "AIDL freezePids result: frozen ");
        }
        if (DEBUG_AIDL && mCgroupV1Flag) {
            Slog.d(TAG, "AIDL freezePids result: frozen " + FreezeUtils.getFrozenPids());
        }
        return result;
    }

    /* JADX WARN: Code restructure failed: missing block: B:110:0x03d4, code lost:
        checkAndFreezeIsolated(r13, true);
        r11 = r28;
        r5 = true;
        r6 = r18;
        r14 = r20;
     */
    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:139:? -> B:117:0x03fb). Please submit an issue!!! */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public java.util.List<java.lang.Integer> freezeUids(int[] r23, long r24, int r26, java.lang.String r27, boolean r28) {
        /*
            Method dump skipped, instructions count: 1090
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.miui.server.greeze.GreezeManagerService.freezeUids(int[], long, int, java.lang.String, boolean):java.util.List");
    }

    public boolean freezeAction(int uid, int fromWho, String reason, boolean isNeedCompact) {
        if (!this.mImmobulusMode.isModeReason(reason) && isAppShowOnWindows(uid)) {
            return false;
        }
        StringBuilder log = new StringBuilder();
        boolean done = false;
        synchronized (this.mAurogonLock) {
            if (isUidFrozen(uid)) {
                return true;
            }
            log.append("FZ uid = " + uid);
            List<Integer> pidList = readPidFromCgroup(uid);
            if (pidList.size() == 0) {
                return false;
            }
            log.append(" pid = [ ");
            boolean isCgroupPidError = true;
            synchronized (this.mFrozenPids) {
                for (Integer num : pidList) {
                    int pid = num.intValue();
                    if (readPidStatus(uid, pid)) {
                        isCgroupPidError = false;
                        done = FreezeUtils.freezePid(pid, uid);
                        if (done) {
                            FrozenInfo info = this.mFrozenPids.get(pid);
                            if (info == null) {
                                info = new FrozenInfo(uid, pid);
                                this.mFrozenPids.put(pid, info);
                            }
                            info.addFreezeInfo(System.currentTimeMillis(), fromWho, reason);
                            if (this.mImmobulusMode.mEnterImmobulusMode) {
                                info.isFrozenByImmobulus = true;
                            } else if ((fromWho & 16) != 0) {
                                info.isFrozenByLaunchMode = true;
                            }
                            log.append(pid + " ");
                        } else {
                            Slog.d(AurogonImmobulusMode.TAG, " Freeze uid = " + uid + " pid = " + pid + " error !");
                        }
                    }
                }
            }
            if (isCgroupPidError) {
                Slog.d(AurogonImmobulusMode.TAG, " Freeze uid = " + uid + " error due pid-uid mismatch");
                return true;
            }
            log.append("] reason : " + reason + " caller : " + fromWho);
            if (!this.mImmobulusMode.isModeReason(reason)) {
                this.mHistoryLog.log(log.toString());
            }
            if (AurogonImmobulusMode.IMMOBULUS_ENABLED && this.mImmobulusMode.isNeedRestictNetworkPolicy(uid)) {
                monitorNet(uid);
            }
            queryBinderState(uid);
            return done;
        }
    }

    public boolean isAppShowOnWindows(int uid) {
        IWindowManager iWindowManager = this.mWindowManager;
        if (iWindowManager != null) {
            try {
                if (iWindowManager.checkAppOnWindowsStatus(uid)) {
                    Slog.d(TAG, "Uid " + uid + " was show on screen, skip it");
                    return true;
                }
                return false;
            } catch (RemoteException e) {
                return false;
            }
        }
        return false;
    }

    public List<Integer> readPidFromCgroup(int uid) {
        String path = "/sys/fs/cgroup/uid_" + uid;
        List<Integer> pidList = new ArrayList<>();
        File file = new File(path);
        File[] tempList = file.listFiles();
        if (tempList == null) {
            return pidList;
        }
        for (int i = 0; i < tempList.length; i++) {
            String temp = tempList[i].toString();
            if (temp.contains("pid_")) {
                String[] Str = tempList[i].toString().split("_");
                if (Str.length > 2 && Str != null && Str[2] != null) {
                    pidList.add(Integer.valueOf(Integer.parseInt(Str[2])));
                }
            }
        }
        return pidList;
    }

    public boolean readPidStatus(int uid, int pid) {
        int tempUid = Process.getUidForPid(pid);
        return tempUid == uid;
    }

    public boolean readPidStatus(int pid) {
        String path = "/proc/" + pid + "/status";
        File file = new File(path);
        if (file.exists()) {
            return true;
        }
        return false;
    }

    public void updateFrozenInfoForImmobulus(int uid, int type) {
        synchronized (this.mFrozenPids) {
            for (int i = 0; i < this.mFrozenPids.size(); i++) {
                FrozenInfo frozen = this.mFrozenPids.valueAt(i);
                if ((type & 8) != 0) {
                    frozen.isFrozenByImmobulus = true;
                } else if ((type & 16) != 0) {
                    frozen.isFrozenByLaunchMode = true;
                }
            }
        }
    }

    public void resetStatusForImmobulus(int type) {
        synchronized (this.mFrozenPids) {
            for (int i = 0; i < this.mFrozenPids.size(); i++) {
                FrozenInfo frozen = this.mFrozenPids.valueAt(i);
                if ((type & 8) != 0) {
                    if (frozen.isFrozenByImmobulus) {
                        frozen.isFrozenByImmobulus = false;
                    }
                } else if ((type & 16) != 0 && frozen.isFrozenByLaunchMode) {
                    frozen.isFrozenByLaunchMode = false;
                }
            }
        }
    }

    public void triggerLaunchMode(String processName, final int uid) {
        if (!this.mScreenOnOff) {
            return;
        }
        this.mTopAppUid = uid;
        if (!this.mImmobulusMode.mLaunchModeEnabled || !milletEnable) {
            return;
        }
        if (InputMethodManagerServiceImpl.MIUI_HOME.equals(processName)) {
            if (!this.mInited) {
                this.mInited = true;
            }
            if (this.mImmobulusMode.isRunningLaunchMode()) {
                return;
            }
        }
        if ("ziyi".equals(Build.DEVICE) || !this.mInited) {
            return;
        }
        if (!this.ENABLE_LAUNCH_MODE_DEVICE.contains(Build.DEVICE) && !AccessController.PACKAGE_CAMERA.equals(processName) && this.mImmobulusMode.mEnabledLMCamera) {
            return;
        }
        if (AccessController.PACKAGE_CAMERA.equals(processName) && this.mImmobulusMode.mEnabledLMCamera) {
            this.mImmobulusMode.triggerImmobulusMode(true);
        }
        if (!UserHandle.isApp(uid)) {
            return;
        }
        this.mHandler.postAtFrontOfQueue(new Runnable() { // from class: com.miui.server.greeze.GreezeManagerService.3
            @Override // java.lang.Runnable
            public void run() {
                GreezeManagerService.this.updateFgAppList(uid, -1, true);
            }
        });
        if (isUidFrozen(uid)) {
            Slog.d("Aurogon", "Thaw uid = " + uid + " Activity Start!");
            thawUid(uid, 1000, "Activity Start");
        }
        this.mImmobulusMode.triggerLaunchMode(processName, uid);
    }

    public void finishLaunchMode(String processName, int uid) {
    }

    public void addToDumpHistory(String log) {
        this.mHistoryLog.log(log);
    }

    public void thawThread(int tid) {
        if (mCgroupV1Flag) {
            FreezeUtils.thawTid(tid);
        }
    }

    private void checkAndFreezeIsolated(final int uid, final boolean freeze) {
        if (mCgroupV1Flag) {
            return;
        }
        this.mHandler.post(new Runnable() { // from class: com.miui.server.greeze.GreezeManagerService.4
            @Override // java.lang.Runnable
            public void run() {
                List<Integer> pids = GreezeManagerService.this.mActivityManagerService.mInternal.getIsolatedProcesses(uid);
                if (pids == null) {
                    return;
                }
                List<Integer> rr = new ArrayList<>();
                for (Integer num : pids) {
                    int p = num.intValue();
                    int isouid = Process.getUidForPid(p);
                    if (isouid != -1 && Process.isIsolated(isouid)) {
                        rr.add(Integer.valueOf(p));
                    }
                }
                if (rr.size() == 0) {
                    return;
                }
                int[] rst = rr.stream().mapToInt(new ToIntFunction() { // from class: com.miui.server.greeze.GreezeManagerService$4$$ExternalSyntheticLambda0
                    @Override // java.util.function.ToIntFunction
                    public final int applyAsInt(Object obj) {
                        return Integer.valueOf(((Integer) obj).intValue()).intValue();
                    }
                }).toArray();
                if (freeze) {
                    GreezeManagerService.this.freezePids(rst, 0L, 1000, "iso");
                } else {
                    GreezeManagerService.this.thawPids(rst, 1000, "iso");
                }
                Slog.d(GreezeManagerService.TAG, "iso uid:" + uid + " p:" + rr.toString() + " :" + freeze);
            }
        });
    }

    public void notifyOtherModule(FrozenInfo info, final int fromWho) {
        int i = info.state;
        final int uid = info.uid;
        final int pid = info.pid;
        this.mHandler.post(new Runnable() { // from class: com.miui.server.greeze.GreezeManagerService.5
            @Override // java.lang.Runnable
            public void run() {
                PowerFrozenManager.getInstance().thawedByOther(uid, pid);
                for (int i2 = 1; i2 < 5; i2++) {
                    if (i2 != fromWho && GreezeManagerService.callbacks.get(Integer.valueOf(i2)) != null) {
                        try {
                            GreezeManagerService.callbacks.get(Integer.valueOf(i2)).thawedByOther(uid, pid, fromWho);
                        } catch (Exception e) {
                            if (GreezeManagerService.DEBUG) {
                                Slog.e(GreezeManagerService.TAG, "notify other module fail uid : " + uid + " from :" + fromWho);
                            }
                        }
                    }
                }
            }
        });
    }

    public boolean thawProcess(int pid, int fromWho, String reason) {
        boolean done;
        synchronized (this.mFrozenPids) {
            FrozenInfo info = this.mFrozenPids.get(pid);
            if (info == null) {
                if (DEBUG) {
                    Slog.w(TAG, "Thawing a non-frozen process (pid=" + pid + "), won't add into history, reason " + reason);
                }
                return false;
            }
            if (mCgroupV1Flag) {
                done = FreezeUtils.thawPid(pid);
            } else {
                done = FreezeUtils.thawPid(pid, info.uid);
            }
            if (info.isAudioZero) {
                stopAudio(info.uid, pid, false);
                Slog.d(TAG, "Audio resume pid:" + pid);
            }
            PowerFrozenManager.getInstance().removeFrozenPid(info.uid, pid);
            info.mThawTime = System.currentTimeMillis();
            info.mThawUptime = SystemClock.uptimeMillis();
            info.mThawReason = reason;
            this.mFrozenPids.remove(pid);
            addHistoryInfo(info);
            if (this.mHandler.hasMessages(1, info)) {
                this.mHandler.removeMessages(1, info);
            }
            return done;
        }
    }

    public List<Integer> thawPids(int[] pids, int fromWho, String reason) {
        checkPermission();
        if (DEBUG_AIDL) {
            Slog.d(TAG, "AIDL thawPids(" + Arrays.toString(pids) + ", " + fromWho + ", " + reason + ")");
        }
        List<Integer> result = new ArrayList<>();
        for (int pid : pids) {
            if (!thawProcess(pid, fromWho, reason)) {
                if (DEBUG_AIDL) {
                    Slog.d(TAG, "AIDL thawPid(" + pid + ", " + fromWho + ", " + reason + ") failed");
                }
            } else {
                result.add(Integer.valueOf(pid));
            }
        }
        return result;
    }

    public boolean thawUid(final int uid, int fromWho, String reason) {
        boolean z;
        HashSet<Integer> toThawPids;
        boolean allDone = true;
        synchronized (this.mAurogonLock) {
            synchronized (this.mFrozenPids) {
                HashSet<Integer> toThawPids2 = new HashSet<>();
                List<FrozenInfo> toThaw = new ArrayList<>();
                for (int i = 0; i < this.mFrozenPids.size(); i++) {
                    FrozenInfo frozen = this.mFrozenPids.valueAt(i);
                    if (frozen.uid == uid) {
                        toThaw.add(frozen);
                        toThawPids2.add(Integer.valueOf(frozen.pid));
                    }
                }
                int i2 = toThaw.size();
                if (i2 == 0) {
                    return false;
                }
                if (mCgroupV1Flag) {
                    List<Integer> curPids = getFrozenNewPids();
                    List<Integer> allPids = FreezeUtils.getFrozenPids();
                    allPids.removeAll(curPids);
                    for (Integer item : allPids) {
                        int ppid = Process.getParentPid(item.intValue());
                        if (!toThawPids2.contains(Integer.valueOf(ppid))) {
                            toThawPids = toThawPids2;
                        } else {
                            FreezeUtils.thawPid(item.intValue());
                            toThawPids = toThawPids2;
                            Slog.d(TAG, "isolated pid: " + item + ", ppid: " + ppid);
                        }
                        toThawPids2 = toThawPids;
                    }
                }
                StringBuilder success = new StringBuilder();
                StringBuilder failed = new StringBuilder();
                StringBuilder log = new StringBuilder();
                log.append("THAW uid = " + uid);
                for (FrozenInfo frozen2 : toThaw) {
                    if (!thawProcess(frozen2.pid, fromWho, reason)) {
                        failed.append(frozen2.pid + " ");
                        allDone = false;
                    } else {
                        success.append(frozen2.pid + " ");
                    }
                }
                log.append(" pid = [ " + success.toString() + "] ");
                if (!"".equals(failed.toString())) {
                    log.append("failed = [ " + failed.toString() + "]");
                }
                log.append(" reason : " + reason + " caller : " + fromWho);
                if (!this.mImmobulusMode.isModeReason(reason) || !allDone) {
                    if (allDone) {
                        this.mHandler.post(new Runnable() { // from class: com.miui.server.greeze.GreezeManagerService.6
                            @Override // java.lang.Runnable
                            public void run() {
                                GreezeManagerService.this.updateAurogonUidRule(uid, false);
                            }
                        });
                    }
                    if (fromWho != 1) {
                        notifyOtherModule(toThaw.get(0), fromWho);
                    }
                    this.mHistoryLog.log(log.toString());
                    Slog.d(TAG, log.toString());
                }
                if (fromWho == 1 || fromWho == 1000) {
                    List<Integer> uids = new ArrayList<>();
                    uids.add(Integer.valueOf(uid));
                    z = false;
                    setWakeLockState(uids, false);
                } else {
                    z = false;
                }
                checkAndFreezeIsolated(uid, z);
                sendPendingAlarmForAurogon(uid);
                if (this.mImmobulusMode.mEnterImmobulusMode) {
                    this.mHandler.post(new Runnable() { // from class: com.miui.server.greeze.GreezeManagerService.7
                        @Override // java.lang.Runnable
                        public void run() {
                            GreezeManagerService.this.mImmobulusMode.repeatCheckAppForImmobulusMode(uid);
                        }
                    });
                }
                return allDone;
            }
        }
    }

    public List<Integer> thawUids(int[] uids, int fromWho, String reason) {
        checkPermission();
        if (DEBUG_AIDL) {
            Slog.d(TAG, "AIDL thawUids(" + Arrays.toString(uids) + ", " + fromWho + ", " + reason + ")");
        }
        List<Integer> result = new ArrayList<>();
        for (int uid : uids) {
            if (!thawUid(uid, fromWho, reason)) {
                if (DEBUG_AIDL) {
                    Slog.d(TAG, "AIDL thawUid(" + uid + ", " + fromWho + ", " + reason + ") failed");
                }
            } else {
                result.add(Integer.valueOf(uid));
            }
        }
        return result;
    }

    private List<Integer> getFrozenNewPids() {
        List<Integer> pids = new ArrayList<>();
        synchronized (this.mFrozenPids) {
            for (int i = 0; i < this.mFrozenPids.size(); i++) {
                FrozenInfo frozen = this.mFrozenPids.valueAt(i);
                pids.add(Integer.valueOf(frozen.pid));
            }
        }
        return pids;
    }

    boolean thawAll(String reason) {
        List<Integer> pids;
        if (mCgroupV1Flag) {
            pids = FreezeUtils.getFrozenPids();
        } else {
            pids = getFrozenNewPids();
        }
        for (Integer num : pids) {
            int pid = num.intValue();
            thawProcess(pid, 9999, reason);
        }
        this.mHistoryLog.log("thawAll pids:" + pids.toString() + " r:" + reason);
        this.mHandler.removeMessages(1);
        if (mCgroupV1Flag) {
            List<Integer> tids = FreezeUtils.getFrozonTids();
            for (Integer num2 : tids) {
                int tid = num2.intValue();
                FreezeUtils.thawTid(tid);
            }
            return FreezeUtils.getFrozenPids().size() == 0;
        }
        return true;
    }

    public List<Integer> thawAll(int module, int fromWho, String reason) {
        checkPermission();
        if (DEBUG_AIDL) {
            Slog.d(TAG, "AIDL thawAll(" + module + ", " + fromWho + ", " + reason + ")");
        }
        List<Integer> pids = new ArrayList<>();
        int[] rst = getFrozenPids(9999);
        for (int p : rst) {
            pids.add(Integer.valueOf(p));
        }
        List<Integer> ret = thawPids(rst, fromWho, reason);
        this.mHistoryLog.log("thawAll pids:" + pids.toString() + " r:" + reason + " fromWho:" + fromWho);
        return ret;
    }

    public int[] getFrozenPids(int module) {
        List<Integer> frozens;
        if (DEBUG_AIDL) {
            Slog.d(TAG, "AIDL getFrozenPids(" + module + ")");
        }
        switch (module) {
            case 0:
            case 1:
            case 2:
                List<Integer> pids = new ArrayList<>();
                synchronized (this.mFrozenPids) {
                    for (int i = 0; i < this.mFrozenPids.size(); i++) {
                        FrozenInfo frozen = this.mFrozenPids.valueAt(i);
                        if (frozen.mFromWho.size() != 0 && frozen.getOwner() == module) {
                            pids.add(Integer.valueOf(frozen.pid));
                        }
                    }
                }
                return toArray(pids);
            case 9999:
                if (mCgroupV1Flag) {
                    frozens = FreezeUtils.getFrozenPids();
                } else {
                    frozens = getFrozenNewPids();
                }
                return toArray(frozens);
            default:
                return new int[0];
        }
    }

    public int[] getFrozenUids(int module) {
        switch (module) {
            case 0:
            case 1:
            case 2:
            case 9999:
                HashSet<Integer> uids = new HashSet<>();
                synchronized (this.mFrozenPids) {
                    for (int i = 0; i < this.mFrozenPids.size(); i++) {
                        FrozenInfo frozen = this.mFrozenPids.valueAt(i);
                        uids.add(Integer.valueOf(frozen.uid));
                    }
                }
                int[] rst = new int[uids.size()];
                int index = 0;
                Iterator<Integer> iterator = uids.iterator();
                while (iterator.hasNext()) {
                    rst[index] = iterator.next().intValue();
                    index++;
                }
                return rst;
            default:
                return new int[0];
        }
    }

    public void updateAurogonUidRule(int uid, boolean allow) {
        if (!this.mInited || miui.os.Build.IS_INTERNATIONAL_BUILD) {
            return;
        }
        try {
            ConnectivityManager connectivityManager = this.cm;
            if (connectivityManager != null) {
                connectivityManager.updateAurogonUidRule(uid, allow);
            }
        } catch (Exception e) {
        }
    }

    public void closeSocketForAurogon(int[] uids) {
        try {
            if (getNmsService() != null) {
                Slog.d(TAG, "call socket destroy!");
                this.mNms.closeSocketForAurogon(uids);
            }
        } catch (RemoteException | ServiceSpecificException e) {
            Slog.d(TAG, "failed to close socket for aurogon!");
        }
    }

    private INetworkManagementService getNmsService() {
        if (this.mNms == null) {
            this.mNms = INetworkManagementService.Stub.asInterface(ServiceManager.getService("network_management"));
        }
        return this.mNms;
    }

    public boolean isNeedCachedAlarmForAurogon(int uid) {
        return isNeedCachedAlarmForAurogonInner(uid);
    }

    public boolean isAppRunning(int uid) {
        if (mCgroupV1Flag) {
            return true;
        }
        List<Integer> list = readPidFromCgroup(uid);
        if (list.size() == 0) {
            return false;
        }
        for (Integer num : list) {
            int pid = num.intValue();
            if (readPidStatus(uid, pid)) {
                return true;
            }
        }
        return false;
    }

    public boolean isAppRunningInFg(int uid) {
        synchronized (this.mFgAppUidList) {
            if (this.mFgAppUidList.get(Integer.valueOf(uid)) != null) {
                return true;
            }
            return false;
        }
    }

    private void stopAudio(int uid, int pid, boolean stop) {
        IBinder audioflinger = ServiceManager.getService("media.audio_flinger");
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            try {
                data.writeInt(uid);
                data.writeInt(pid);
                data.writeBoolean(stop);
                audioflinger.transact(88, data, reply, 1);
                if (data != null) {
                    data.recycle();
                }
                if (reply == null) {
                    return;
                }
            } catch (Exception e) {
                e.fillInStackTrace();
                Log.e(TAG, "stopAudio:" + e);
                if (data != null) {
                    data.recycle();
                }
                if (reply == null) {
                    return;
                }
            }
            reply.recycle();
        } catch (Throwable th) {
            if (data != null) {
                data.recycle();
            }
            if (reply != null) {
                reply.recycle();
            }
            throw th;
        }
    }

    public void forceStopPackage(String packageName, int userId, String reason) {
        this.mActivityManagerService.forceStopPackage(packageName, userId);
        this.mHistoryLog.log("ForceStop packageName = " + packageName + "Reason : " + reason);
    }

    public void updateFgAppList(int uid, int pid, boolean addOrDel) {
        synchronized (this.mFgAppUidList) {
            List<Integer> list = this.mFgAppUidList.get(Integer.valueOf(uid));
            if (addOrDel) {
                if (list == null) {
                    list = new ArrayList();
                    this.mFgAppUidList.put(Integer.valueOf(uid), list);
                }
                if (pid != -1 && !list.contains(Integer.valueOf(pid))) {
                    list.add(Integer.valueOf(pid));
                }
                if (this.mAppFrontUid != uid && UserHandle.isApp(uid)) {
                    this.mAppFrontUid = uid;
                }
            } else if (list == null) {
            } else {
                if (pid != -1) {
                    list.remove(Integer.valueOf(pid));
                }
                if (pid == -1 || list.size() == 0) {
                    this.mFgAppUidList.remove(Integer.valueOf(uid));
                }
            }
        }
    }

    public void checkFgAppList(boolean checkDisguiseFg) {
        int i;
        if (this.mActivityManager == null) {
            return;
        }
        synchronized (this.mFgAppUidList) {
            boolean delDisguiseFg = false;
            if (checkDisguiseFg) {
                try {
                    Iterator<Map.Entry<Integer, List<Integer>>> ite = this.mFgAppUidList.entrySet().iterator();
                    while (ite.hasNext()) {
                        int u = ite.next().getKey().intValue();
                        if (this.mActivityManager.getUidImportance(u) > 200) {
                            ite.remove();
                            delDisguiseFg = true;
                            Slog.d("Aurogon", " uid = " + u + " switch to BG~");
                        }
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
            if (!delDisguiseFg && checkDisguiseFg) {
                return;
            }
            if (this.mFgAppUidList.size() == 0 && (i = this.mAppFrontUid) != -1) {
                updateFgAppList(i, -1, true);
                Slog.d("Aurogon", " uid = " + this.mAppFrontUid + " add again");
            }
        }
    }

    public void notifyMovetoFront(final int uid) {
        this.mTopAppUid = uid;
        this.mHandler.post(new Runnable() { // from class: com.miui.server.greeze.GreezeManagerService.10
            @Override // java.lang.Runnable
            public void run() {
                if (GreezeManagerService.this.isUidFrozen(uid)) {
                    GreezeManagerService.this.thawUid(uid, 1000, "Activity Front");
                }
            }
        });
    }

    public void updateOrderBCStatus(String intentAction, int uid, boolean isforeground, boolean allow) {
        if (intentAction == null) {
            return;
        }
        if (allow) {
            if (isforeground) {
                this.mFGOrderBroadcastAction = intentAction;
                this.mFGOrderBroadcastAppUid = uid;
                return;
            }
            this.mBGOrderBroadcastAction = intentAction;
            this.mBGOrderBroadcastAppUid = uid;
        } else if (isforeground) {
            if (this.mFGOrderBroadcastAction.equals(intentAction)) {
                this.mFGOrderBroadcastAction = "";
                this.mFGOrderBroadcastAppUid = -1;
            }
        } else if (this.mBGOrderBroadcastAction.equals(intentAction)) {
            this.mBGOrderBroadcastAction = "";
            this.mBGOrderBroadcastAppUid = -1;
        }
    }

    public boolean checkOrderBCRecivingApp(int uid) {
        if (uid == this.mFGOrderBroadcastAppUid) {
            this.mFGOrderBroadcastAction = "";
            this.mFGOrderBroadcastAppUid = -1;
            return true;
        } else if (uid == this.mBGOrderBroadcastAppUid) {
            this.mBGOrderBroadcastAction = "";
            this.mBGOrderBroadcastAppUid = -1;
            return true;
        } else {
            return false;
        }
    }

    private void dumpFreezeAction(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("Frozen processes:" + FreezeUtils.getFrozenPids().toString());
        pw.println("Greezer History : ");
        this.mHistoryLog.dump(fd, pw, args);
        pw.println("FG App History : ");
        StringBuilder log = new StringBuilder();
        synchronized (this.mFgAppUidList) {
            for (Integer num : this.mFgAppUidList.keySet()) {
                int uid = num.intValue();
                log.append(uid + " ");
            }
        }
        pw.println(log.toString());
        this.mImmobulusMode.dump(fd, pw, args);
    }

    public void getAlarmManagerService() {
        if (this.mAlarmManager == null) {
            this.mAlarmManager = IAlarmManager.Stub.asInterface(ServiceManager.getService("alarm"));
        }
    }

    private void getWindowManagerService() {
        if (this.mWindowManager == null) {
            this.mWindowManager = WindowManagerGlobal.getWindowManagerService();
        }
    }

    public boolean isNeedCachedAlarmForAurogonInner(int uid) {
        String packageName;
        if (isUidFrozen(uid) && (packageName = getPackageNameFromUid(uid)) != null && UserHandle.isApp(uid) && !packageName.contains("xiaomi") && !packageName.contains("miui") && !this.mAurogonAlarmAllowList.contains(packageName)) {
            Slog.d(TAG, "cached alarm!");
            return true;
        }
        return false;
    }

    public void sendPendingAlarmForAurogon(final int uid) {
        this.mHandler.postDelayed(new Runnable() { // from class: com.miui.server.greeze.GreezeManagerService.11
            @Override // java.lang.Runnable
            public void run() {
                try {
                    if (GreezeManagerService.this.mAlarmManager == null) {
                        GreezeManagerService.this.getAlarmManagerService();
                    }
                    if (GreezeManagerService.this.mAlarmManager != null) {
                        GreezeManagerService.this.mAlarmManager.sendPendingAlarmByAurogon(uid);
                    }
                } catch (RemoteException e) {
                }
            }
        }, 500L);
    }

    private String getPackageNameFromUid(int uid) {
        String packName = null;
        if (this.mPm == null) {
            this.mPm = this.mContext.getPackageManager();
        }
        PackageManager packageManager = this.mPm;
        if (packageManager != null) {
            packName = packageManager.getNameForUid(uid);
        }
        if (packName == null) {
            Slog.d(TAG, "get caller pkgname failed uid = " + uid);
        }
        return packName;
    }

    private void registerObserverForAurogon() {
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_AUROGON_ALARM_ALLOW_LIST), false, this.mCloudAurogonAlarmListObserver, -2);
    }

    public void notifyExcuteServices(final int uid) {
        if (!UserHandle.isApp(uid) || uid > 19999) {
            return;
        }
        synchronized (this.mExcuteServiceList) {
            if (!this.mExcuteServiceList.contains(Integer.valueOf(uid))) {
                this.mExcuteServiceList.add(Integer.valueOf(uid));
            }
        }
        if (this.mImmobulusMode.isRunningLaunchMode()) {
            this.mImmobulusMode.addLaunchModeQiutList(uid);
        } else {
            this.mHandler.post(new Runnable() { // from class: com.miui.server.greeze.GreezeManagerService.13
                @Override // java.lang.Runnable
                public void run() {
                    GreezeManagerService.this.thawUid(uid, 1000, "Excute Service");
                }
            });
        }
    }

    public void updatexcuteServiceStatus() {
        SparseArray<List<RunningProcess>> uidMap = GreezeServiceUtils.getUidMap();
        synchronized (this.mExcuteServiceList) {
            List<Integer> newList = new ArrayList<>();
            for (Integer num : this.mExcuteServiceList) {
                int uid = num.intValue();
                List<RunningProcess> procs = uidMap.get(uid);
                if (procs != null) {
                    Iterator<RunningProcess> it = procs.iterator();
                    while (true) {
                        if (it.hasNext()) {
                            RunningProcess proc = it.next();
                            if (proc.adj == 0) {
                                newList.add(Integer.valueOf(proc.uid));
                                this.mImmobulusMode.addLaunchModeQiutList(proc.uid);
                                break;
                            }
                        }
                    }
                }
            }
            this.mExcuteServiceList.clear();
            this.mExcuteServiceList.addAll(newList);
        }
    }

    /* loaded from: classes.dex */
    public class AurogonBroadcastReceiver extends BroadcastReceiver {
        public String actionUI = "com.android.systemui.fsgesture";

        public AurogonBroadcastReceiver() {
            GreezeManagerService.this = this$0;
            IntentFilter intent = new IntentFilter();
            intent.addAction("android.intent.action.SCREEN_ON");
            intent.addAction("android.intent.action.SCREEN_OFF");
            intent.addAction(this.actionUI);
            this$0.mContext.registerReceiver(this, intent);
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.SCREEN_ON".equals(action)) {
                GreezeManagerService.this.mScreenOnOff = true;
                GreezeManagerService.this.mHistoryLog.log("SCREEN ON!");
            } else if ("android.intent.action.SCREEN_OFF".equals(action)) {
                GreezeManagerService.this.mScreenOnOff = false;
                GreezeManagerService.this.mHistoryLog.log("SCREEN OFF!");
                GreezeManagerService.this.mImmobulusMode.finishLaunchMode();
                if (GreezeManagerService.this.mImmobulusMode.mEnterImmobulusMode) {
                    GreezeManagerService.this.mImmobulusMode.triggerImmobulusMode(false);
                }
            } else if (this.actionUI.equals(action)) {
                String type = intent.getStringExtra("typeFrom");
                boolean isEnter = intent.getBooleanExtra("isEnter", false);
                if (type != null && "typefrom_status_bar_expansion".equals(type)) {
                    GreezeManagerService.this.isBarExpand = isEnter;
                    if (isEnter) {
                        GreezeManagerService.this.mHandler.sendEmptyMessage(5);
                        if (GreezeManagerService.this.mImmobulusMode.mEnterIMCamera || GreezeManagerService.this.mImmobulusMode.mEnterImmobulusMode) {
                            GreezeManagerService.this.mImmobulusMode.triggerImmobulusMode(false);
                            GreezeManagerService.this.mImmobulusMode.mEnterIMCamera = false;
                            GreezeManagerService.this.mImmobulusMode.mLastBarExpandIMStatus = true;
                        }
                    } else if (GreezeManagerService.this.mFgAppUidList.get(Integer.valueOf(GreezeManagerService.this.mImmobulusMode.mCameraUid)) != null) {
                        GreezeManagerService.this.mImmobulusMode.triggerImmobulusMode(true);
                        GreezeManagerService.this.mImmobulusMode.mEnterIMCamera = true;
                    } else if (GreezeManagerService.this.mImmobulusMode.mLastBarExpandIMStatus) {
                        GreezeManagerService.this.mImmobulusMode.triggerImmobulusMode(true);
                        GreezeManagerService.this.mImmobulusMode.mLastBarExpandIMStatus = false;
                    }
                }
            }
        }
    }

    public int getSystemUiPid() {
        List<RunningProcess> procList = GreezeServiceUtils.getProcessList();
        for (RunningProcess proc : procList) {
            if (proc != null && "com.android.systemui".equals(proc.processName)) {
                return proc.pid;
            }
        }
        return -1;
    }

    public boolean isRestrictBackgroundAction(String localhost, int callerUid, String callerPkgName, int calleeUid, String calleePkgName) {
        boolean ret = false;
        if (!isUidFrozen(calleeUid)) {
            return true;
        }
        if (DEBUG) {
            Slog.d(TAG, "localhost = " + localhost + " callerUid = " + callerUid + " callerPkgName = " + callerPkgName + " calleeUid = " + calleeUid + " calleePkgName = " + calleePkgName);
        }
        char c = 65535;
        switch (localhost.hashCode()) {
            case -1618876223:
                if (localhost.equals("broadcast")) {
                    c = 0;
                    break;
                }
                break;
            case -987494927:
                if (localhost.equals("provider")) {
                    c = 1;
                    break;
                }
                break;
            case -246623272:
                if (localhost.equals("bindservice")) {
                    c = 2;
                    break;
                }
                break;
            case 185053203:
                if (localhost.equals("startservice")) {
                    c = 3;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
                ret = isNeedAllowRequest(callerUid, callerPkgName, calleeUid);
                break;
            case 1:
                ret = true;
                break;
            case 2:
            case 3:
                ret = isNeedAllowRequest(callerUid, callerPkgName, calleeUid);
                break;
        }
        if (ret) {
            if (this.mImmobulusMode.isRunningLaunchMode()) {
                this.mImmobulusMode.addLaunchModeQiutList(calleeUid);
                return ret;
            }
            thawUid(calleeUid, 1000, localhost);
        }
        return ret;
    }

    private boolean isNeedAllowRequest(int callerUid, String callerPkgName, int calleeUid) {
        return !this.mScreenOnOff ? callerUid == 1027 || callerUid == 1002 : isAppRunningInFg(callerUid) || callerUid == calleeUid || !UserHandle.isApp(callerUid) || "com.xiaomi.xmsf".equals(callerPkgName);
    }

    public boolean checkAurogonIntentDenyList(String action) {
        if (this.mBroadcastIntentDenyList.contains(action)) {
            return true;
        }
        return false;
    }

    static int[] toArray(List<Integer> lst) {
        if (lst == null) {
            return new int[0];
        }
        int[] arr = new int[lst.size()];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = lst.get(i).intValue();
        }
        return arr;
    }

    public void notifyBackup(int uid, boolean start) {
        Handler handler = this.mHandler;
        handler.sendMessage(handler.obtainMessage(6, uid, start ? 1 : 0));
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public static class GreezeThread extends ServiceThread {
        private static GreezeThread sInstance;

        private GreezeThread() {
            super("Greezer", -2, true);
        }

        private static void ensureThreadLocked() {
            if (sInstance == null) {
                GreezeThread greezeThread = new GreezeThread();
                sInstance = greezeThread;
                greezeThread.start();
            }
        }

        public static GreezeThread getInstance() {
            GreezeThread greezeThread;
            synchronized (GreezeThread.class) {
                ensureThreadLocked();
                greezeThread = sInstance;
            }
            return greezeThread;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class H extends Handler {
        static final int MSG_BACKUP_OP = 6;
        static final int MSG_GET_SYSTEM_PID = 5;
        static final int MSG_LAUNCH_BOOST = 2;
        static final int MSG_MILLET_LOOPONCE = 4;
        static final int MSG_THAW_ALL = 3;
        static final int MSG_THAW_PID = 1;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public H(Looper looper) {
            super(looper);
            GreezeManagerService.this = this$0;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    if (msg.arg1 != 0) {
                        int pid = msg.arg1;
                        FrozenInfo frozenInfo = (FrozenInfo) msg.obj;
                        GreezeManagerService.this.thawProcess(pid, msg.arg2, "Timeout pid " + pid);
                        return;
                    }
                    return;
                case 2:
                default:
                    return;
                case 3:
                    GreezeManagerService.this.thawAll("from msg");
                    return;
                case 4:
                    GreezeManagerService.nLoopOnce();
                    return;
                case 5:
                    GreezeManagerService greezeManagerService = GreezeManagerService.this;
                    greezeManagerService.mSystemUiPid = greezeManagerService.getSystemUiPid();
                    return;
                case 6:
                    if (msg.arg2 == 1 && GreezeManagerService.this.isUidFrozen(msg.arg1)) {
                        GreezeManagerService.this.thawUid(msg.arg1, 1000, "backing");
                        return;
                    } else if (msg.arg2 == 0 && !GreezeManagerService.this.isUidFrozen(msg.arg1)) {
                        int[] uids = {msg.arg1};
                        GreezeManagerService.this.freezeUids(uids, 0L, 1000, "backingE", true);
                        return;
                    } else {
                        return;
                    }
            }
        }
    }

    /* loaded from: classes.dex */
    public static class FrozenInfo {
        String mThawReason;
        long mThawTime;
        long mThawUptime;
        int pid;
        String processName;
        int state;
        int uid;
        List<Integer> mFromWho = new ArrayList(16);
        List<Long> mFreezeTimes = new ArrayList(16);
        List<String> mFreezeReasons = new ArrayList(16);
        boolean isFrozenByImmobulus = false;
        boolean isFrozenByLaunchMode = false;
        boolean isAudioZero = false;

        FrozenInfo(RunningProcess processRecord) {
            this.uid = processRecord.uid;
            this.pid = processRecord.pid;
            this.processName = processRecord.processName;
        }

        FrozenInfo(int uid, int pid) {
            this.uid = uid;
            this.pid = pid;
        }

        void addFreezeInfo(long curTime, int fromWho, String reason) {
            this.mFreezeTimes.add(Long.valueOf(curTime));
            this.mFromWho.add(Integer.valueOf(fromWho));
            this.mFreezeReasons.add(reason);
            this.state |= 1 << fromWho;
        }

        long getStartTime() {
            if (this.mFreezeTimes.size() == 0) {
                return 0L;
            }
            return this.mFreezeTimes.get(0).longValue();
        }

        long getEndTime() {
            return this.mThawTime;
        }

        long getFrozenDuration() {
            if (getStartTime() < getEndTime()) {
                return getEndTime() - getStartTime();
            }
            return 0L;
        }

        int getOwner() {
            if (this.mFromWho.size() == 0) {
                return 0;
            }
            return this.mFromWho.get(0).intValue();
        }

        public String toString() {
            return this.uid + " " + this.pid + " " + this.processName;
        }
    }

    private static int ringAdvance(int origin, int increment, int size) {
        int index = (origin + increment) % size;
        return index < 0 ? index + size : index;
    }

    private void addHistoryInfo(FrozenInfo info) {
        FrozenInfo[] frozenInfoArr = this.mFrozenHistory;
        int i = this.mHistoryIndexNext;
        frozenInfoArr[i] = info;
        this.mHistoryIndexNext = ringAdvance(i, 1, HISTORY_SIZE);
    }

    private List<FrozenInfo> getHistoryInfos(long sinceUptime) {
        FrozenInfo frozenInfo;
        List<FrozenInfo> ret = new ArrayList<>();
        int index = ringAdvance(this.mHistoryIndexNext, -1, HISTORY_SIZE);
        int i = 0;
        while (true) {
            int i2 = HISTORY_SIZE;
            if (i >= i2 || (frozenInfo = this.mFrozenHistory[index]) == null || frozenInfo.mThawTime < sinceUptime) {
                break;
            }
            ret.add(this.mFrozenHistory[index]);
            index = ringAdvance(index, -1, i2);
            i++;
        }
        return ret;
    }

    void dumpHistory(String prefix, FileDescriptor fd, PrintWriter pw) {
        pw.println("Frozen processes in history:");
        List<FrozenInfo> infos = getHistoryInfos(SystemClock.uptimeMillis() - 14400000);
        int index = 1;
        SimpleDateFormat formater = new SimpleDateFormat("HH:mm:ss.SSS");
        for (FrozenInfo info : infos) {
            pw.print(prefix + "  ");
            int index2 = index + 1;
            pw.print("#" + index);
            pw.print(" " + formater.format(new Date(info.mThawTime)));
            if (info.uid != 0) {
                pw.print(" " + info.uid);
            }
            pw.print(" " + info.pid);
            if (!TextUtils.isEmpty(info.processName)) {
                pw.print(" " + info.processName);
            }
            pw.println(" " + info.getFrozenDuration() + "ms");
            for (int i = 0; i < info.mFreezeTimes.size(); i++) {
                pw.print(prefix + "    ");
                pw.print("fz: ");
                pw.print(formater.format(new Date(info.mFreezeTimes.get(i).longValue())));
                pw.print(" " + info.mFreezeReasons.get(i));
                pw.println(" from " + info.mFromWho.get(i));
            }
            pw.print(prefix + "    ");
            pw.print("th: ");
            pw.print(formater.format(new Date(info.mThawTime)));
            pw.println(" " + info.mThawReason);
            index = index2;
        }
    }

    void dumpSettings(String prefix, FileDescriptor fd, PrintWriter pw) {
        pw.println(prefix + "Settings:");
        pw.println(prefix + "  enable=" + (milletEnable && this.mPowerMilletEnable) + " (" + PROP_POWERMILLET_ENABLE + ")");
        pw.println(prefix + "  debug=" + DEBUG + " (" + PROPERTY_GZ_DEBUG + ")");
        pw.println(prefix + "  monkey=" + DEBUG_MONKEY + " (" + PROPERTY_GZ_MONKEY + ")");
        pw.println(prefix + "  fz_timeout=" + LAUNCH_FZ_TIMEOUT + " (" + PROPERTY_GZ_FZTIMEOUT + ")");
        pw.println(prefix + "  monitor=" + milletEnable + " (" + this.mRegisteredMonitor + ")");
    }

    void dumpFrozen(String prefix, FileDescriptor fd, PrintWriter pw) {
        List<Integer> tids;
        if (mCgroupV1Flag) {
            List<Integer> tids2 = FreezeUtils.getFrozonTids();
            pw.println(prefix + "Frozen tids: " + tids2);
            tids = FreezeUtils.getFrozenPids();
        } else {
            tids = getFrozenNewPids();
        }
        pw.println(prefix + "Frozen pids: " + tids);
        SimpleDateFormat formater = new SimpleDateFormat("HH:mm:ss.SSS");
        pw.println(prefix + "Frozen processes:");
        synchronized (this.mFrozenPids) {
            int n = this.mFrozenPids.size();
            for (int i = 0; i < n; i++) {
                FrozenInfo info = this.mFrozenPids.valueAt(i);
                pw.print(prefix + "  ");
                pw.print("#" + (i + 1));
                pw.println(" pid=" + info.pid);
                for (int index = 0; index < info.mFreezeTimes.size(); index++) {
                    pw.print(prefix + "    ");
                    pw.print("fz: ");
                    pw.print(formater.format(new Date(info.mFreezeTimes.get(index).longValue())));
                    pw.print(" " + info.mFreezeReasons.get(index));
                    pw.println(" from " + info.mFromWho.get(index));
                }
            }
        }
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (!DumpUtils.checkDumpPermission(this.mContext, TAG, pw)) {
            return;
        }
        dumpSettings("", fd, pw);
        dumpFreezeAction(fd, pw, args);
        if (args.length != 0 && "old".equals(args[0])) {
            dumpHistory("", fd, pw);
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
        new GreezeMangaerShellCommand(this).exec(this, in, out, err, args, callback, resultReceiver);
    }

    /* loaded from: classes.dex */
    static class GreezeMangaerShellCommand extends ShellCommand {
        GreezeManagerService mService;

        GreezeMangaerShellCommand(GreezeManagerService service) {
            this.mService = service;
        }

        private void runDumpHistory() {
            this.mService.dumpHistory("", getOutFileDescriptor(), getOutPrintWriter());
        }

        private void runListProcesses() {
            PrintWriter pw = getOutPrintWriter();
            List<RunningProcess> list = GreezeServiceUtils.getProcessList();
            pw.println("process total " + list.size());
            for (int i = 0; i < list.size(); i++) {
                RunningProcess proc = list.get(i);
                pw.printf("  #%d %s", Integer.valueOf(i + 1), proc.toString());
                pw.println();
            }
        }

        private void runDumpPackages() {
            PrintWriter pw = getOutPrintWriter();
            ProcessMap<List<RunningProcess>> procMap = this.mService.getPkgMap();
            for (String pkgName : procMap.getMap().keySet()) {
                pw.println("pkg " + pkgName);
                SparseArray<List<RunningProcess>> uids = (SparseArray) procMap.getMap().get(pkgName);
                for (int i = 0; i < uids.size(); i++) {
                    uids.keyAt(i);
                    List<RunningProcess> procs = uids.valueAt(i);
                    if (procs != null) {
                        for (RunningProcess proc : procs) {
                            pw.println("  " + proc.toString());
                        }
                    }
                }
            }
        }

        private void runDumpUids() {
            PrintWriter pw = getOutPrintWriter();
            SparseArray<List<RunningProcess>> uidMap = GreezeServiceUtils.getUidMap();
            int N = uidMap.size();
            pw.println("uid total " + N);
            for (int i = 0; i < N; i++) {
                int uid = uidMap.keyAt(i);
                pw.printf("#%d uid %d", Integer.valueOf(i + 1), Integer.valueOf(uid));
                pw.println();
                List<RunningProcess> procs = uidMap.valueAt(i);
                for (RunningProcess proc : procs) {
                    pw.println("  " + proc.toString());
                }
            }
        }

        private void dumpSkipUid() {
            PrintWriter pw = getOutPrintWriter();
            pw.println("audio uid: " + GreezeServiceUtils.getAudioUid());
            pw.println("ime uid: " + GreezeServiceUtils.getIMEUid());
            try {
                ForegroundInfo foregroundInfo = ProcessManager.getForegroundInfo();
                pw.println("foreground uid: " + foregroundInfo.mForegroundUid);
                pw.println("multi window uid: " + foregroundInfo.mMultiWindowForegroundUid);
            } catch (Exception e) {
                Slog.w("ShellCommand", "Failed to get foreground info from ProcessManager", e);
            }
            List<RunningProcess> procs = GreezeServiceUtils.getProcessList();
            Set<Integer> foreActs = new ArraySet<>();
            Set<Integer> foreSvcs = new ArraySet<>();
            for (RunningProcess proc : procs) {
                if (proc.hasForegroundActivities) {
                    foreActs.add(Integer.valueOf(proc.uid));
                }
                if (proc.hasForegroundServices) {
                    foreSvcs.add(Integer.valueOf(proc.uid));
                }
            }
            pw.println("fore act uid: " + foreActs);
            pw.println("fore svc uid: " + foreSvcs);
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        public int onCommand(String cmd) {
            char c;
            this.mService.checkPermission();
            PrintWriter pw = getOutPrintWriter();
            if (cmd == null) {
                return handleDefaultCommands(cmd);
            }
            try {
                switch (cmd.hashCode()) {
                    case -1346846559:
                        if (cmd.equals("unmonitor")) {
                            c = 6;
                            break;
                        }
                        c = 65535;
                        break;
                    case -1298848381:
                        if (cmd.equals(MiuiCustomizeShortCutUtils.ATTRIBUTE_ENABLE)) {
                            c = 14;
                            break;
                        }
                        c = 65535;
                        break;
                    case -452147603:
                        if (cmd.equals("clearmonitor")) {
                            c = 7;
                            break;
                        }
                        c = 65535;
                        break;
                    case -172220347:
                        if (cmd.equals("callback")) {
                            c = 20;
                            break;
                        }
                        c = 65535;
                        break;
                    case -75092327:
                        if (cmd.equals("getUids")) {
                            c = 19;
                            break;
                        }
                        c = 65535;
                        break;
                    case 3463:
                        if (cmd.equals("ls")) {
                            c = '\t';
                            break;
                        }
                        c = 65535;
                        break;
                    case 3587:
                        if (cmd.equals(ScoutHelper.ACTION_PS)) {
                            c = '\f';
                            break;
                        }
                        c = 65535;
                        break;
                    case 111052:
                        if (cmd.equals(SplitScreenReporter.STR_PKG)) {
                            c = '\r';
                            break;
                        }
                        c = 65535;
                        break;
                    case 115792:
                        if (cmd.equals("uid")) {
                            c = 11;
                            break;
                        }
                        c = 65535;
                        break;
                    case 3327652:
                        if (cmd.equals("loop")) {
                            c = 18;
                            break;
                        }
                        c = 65535;
                        break;
                    case 3331227:
                        if (cmd.equals("lsfz")) {
                            c = '\n';
                            break;
                        }
                        c = 65535;
                        break;
                    case 3532159:
                        if (cmd.equals("skip")) {
                            c = 15;
                            break;
                        }
                        c = 65535;
                        break;
                    case 3558826:
                        if (cmd.equals("thaw")) {
                            c = 4;
                            break;
                        }
                        c = 65535;
                        break;
                    case 95458899:
                        if (cmd.equals("debug")) {
                            c = 16;
                            break;
                        }
                        c = 65535;
                        break;
                    case 97944631:
                        if (cmd.equals("fzpid")) {
                            c = 1;
                            break;
                        }
                        c = 65535;
                        break;
                    case 97949436:
                        if (cmd.equals("fzuid")) {
                            c = 0;
                            break;
                        }
                        c = 65535;
                        break;
                    case 107944136:
                        if (cmd.equals("query")) {
                            c = '\b';
                            break;
                        }
                        c = 65535;
                        break;
                    case 110337687:
                        if (cmd.equals("thpid")) {
                            c = 3;
                            break;
                        }
                        c = 65535;
                        break;
                    case 110342492:
                        if (cmd.equals("thuid")) {
                            c = 2;
                            break;
                        }
                        c = 65535;
                        break;
                    case 926934164:
                        if (cmd.equals("history")) {
                            c = 17;
                            break;
                        }
                        c = 65535;
                        break;
                    case 1236319578:
                        if (cmd.equals("monitor")) {
                            c = 5;
                            break;
                        }
                        c = 65535;
                        break;
                    default:
                        c = 65535;
                        break;
                }
                switch (c) {
                    case 0:
                        this.mService.freezeUids(new int[]{Integer.parseInt(getNextArgRequired())}, 0L, Integer.parseInt(getNextArgRequired()), "cmd: fzuid", false);
                        return 0;
                    case 1:
                        this.mService.freezePids(new int[]{Integer.parseInt(getNextArgRequired())}, 0L, Integer.parseInt(getNextArgRequired()), "cmd: fzpid");
                        return 0;
                    case 2:
                        this.mService.thawUids(new int[]{Integer.parseInt(getNextArgRequired())}, Integer.parseInt(getNextArgRequired()), "cmd: thuid");
                        return 0;
                    case 3:
                        this.mService.thawPids(new int[]{Integer.parseInt(getNextArgRequired())}, Integer.parseInt(getNextArgRequired()), "cmd: thpid");
                        return 0;
                    case 4:
                        this.mService.thawAll(9999, 9999, "ShellCommand: thaw all");
                        return 0;
                    case 5:
                        this.mService.monitorNet(Integer.parseInt(getNextArgRequired()));
                        return 0;
                    case 6:
                        this.mService.clearMonitorNet(Integer.parseInt(getNextArgRequired()));
                        return 0;
                    case 7:
                        this.mService.clearMonitorNet();
                        return 0;
                    case '\b':
                        this.mService.queryBinderState(Integer.parseInt(getNextArgRequired()));
                        return 0;
                    case '\t':
                        this.mService.dumpSettings("", getOutFileDescriptor(), getOutPrintWriter());
                        this.mService.dumpFrozen("", getOutFileDescriptor(), getOutPrintWriter());
                        return 0;
                    case '\n':
                        getOutPrintWriter().println(Arrays.toString(this.mService.getFrozenPids(Integer.parseInt(getNextArgRequired()))));
                        return 0;
                    case 11:
                        runDumpUids();
                        return 0;
                    case '\f':
                        runListProcesses();
                        return 0;
                    case '\r':
                        runDumpPackages();
                        return 0;
                    case 14:
                        boolean enable = Boolean.parseBoolean(getNextArgRequired());
                        GreezeManagerService.sEnable = enable;
                        pw.println("launch freeze enabled " + enable);
                        return 0;
                    case 15:
                        dumpSkipUid();
                        return 0;
                    case 16:
                        boolean debug = Boolean.parseBoolean(getNextArgRequired());
                        GreezeManagerService.DEBUG_MILLET = debug;
                        GreezeManagerService.DEBUG_LAUNCH_FROM_HOME = debug;
                        GreezeManagerService.DEBUG_AIDL = debug;
                        GreezeManagerService.DEBUG = debug;
                        FreezeUtils.DEBUG = debug;
                        pw.println("launch debug log enabled " + debug);
                        return 0;
                    case 17:
                        runDumpHistory();
                        return 0;
                    case 18:
                        GreezeManagerService.nLoopOnce();
                        return 0;
                    case 19:
                        int module = Integer.parseInt(getNextArgRequired());
                        int[] rst = this.mService.getFrozenUids(module);
                        pw.println("Frozen uids : " + Arrays.toString(rst));
                        break;
                    case 20:
                        int tmp_module = Integer.parseInt(getNextArgRequired());
                        this.mService.registerCallback(new TmpCallback(tmp_module), tmp_module);
                        break;
                    default:
                        return handleDefaultCommands(cmd);
                }
            } catch (Exception e) {
                pw.println(e);
            }
            return -1;
        }

        public void onHelp() {
            PrintWriter pw = getOutPrintWriter();
            pw.println("Greeze manager (greezer) commands:");
            pw.println();
            pw.println("  ls lsfz");
            pw.println("  history");
            pw.println();
            pw.println("  fzpid PID");
            pw.println("  fzuid UID");
            pw.println();
            pw.println("  thpid PID");
            pw.println("  thuid UID");
            pw.println("  thaw");
            pw.println();
            pw.println("  monitor/unmonitor UID");
            pw.println("  clearmonitor");
            pw.println();
            pw.println("  query UID");
            pw.println("    Query binder state in all processes of UID");
            pw.println();
            pw.println("  uid pkg ps");
            pw.println();
            pw.println("  enable true/false");
        }
    }

    /* loaded from: classes.dex */
    static class TmpCallback extends IGreezeCallback.Stub {
        int module;

        public TmpCallback(int module) {
            this.module = module;
        }

        public void reportSignal(int uid, int pid, long now) {
        }

        public void reportNet(int uid, long now) {
        }

        public void reportBinderTrans(int dstUid, int dstPid, int callerUid, int callerPid, int callerTid, boolean isOneway, long now, long buffer) {
        }

        public void reportBinderState(int uid, int pid, int tid, int binderState, long now) {
        }

        public void serviceReady(boolean ready) {
        }

        public void thawedByOther(int uid, int pid, int module) {
            Log.e(GreezeManagerService.TAG, this.module + ": thawed uid:" + uid + " by:" + module);
        }
    }

    public static boolean isEnable() {
        return sEnable && milletEnable;
    }
}
