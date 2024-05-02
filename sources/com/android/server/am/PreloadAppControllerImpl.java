package com.android.server.am;

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.AppGlobals;
import android.app.IApplicationThread;
import android.app.ProfilerInfo;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.util.Slog;
import android.util.SparseArray;
import android.view.WindowManager;
import com.android.server.ServiceThread;
import com.android.server.wm.PreloadLifecycle;
import com.android.server.wm.PreloadStateManagerImpl;
import com.android.server.wm.WindowProcessController;
import com.miui.base.MiuiStubRegistry;
import com.miui.base.MiuiStubUtil;
import com.miui.server.AccessController;
import com.miui.server.SplashScreenServiceDelegate;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import miui.process.IPreloadCallback;
import miui.process.LifecycleConfig;
/* loaded from: classes.dex */
public class PreloadAppControllerImpl implements PreloadAppControllerStub {
    public static final int APP_NOT_PRELOAD = -483;
    private static final int FLAG_DISPLAY_ID_SHIFT = 8;
    private static final int MAX_PRELOAD_COUNT = 10;
    public static final int MIN_TOTAL_MEMORY = 6;
    public static final int PACKAGE_NAME_EMPTY = -495;
    public static final int PERMISSION_DENIED = -488;
    public static final String PREFIX = "preloadApp";
    public static final int START_ALREADY_LOAD_APP = -497;
    public static final int START_CONFIG_NULL = -485;
    public static final int START_ERROR = -500;
    public static final int START_FAIL_SPEED_TEST = -484;
    public static final int START_FORBIDDEN_LOW_MEMORY = -494;
    public static final int START_FORBIDDEN_SYSTEM_APP = -498;
    public static final int START_FREQUENTLY_KILLED = -487;
    public static final int START_GREEZE_IS_DISABLE = -489;
    public static final int START_LAUNCH_INTENT_NOT_FOUND = -493;
    public static final int START_NO_INTERCEPT = 500;
    public static final int START_PACKAGE_NAME_IN_BLACK_LIST = -491;
    public static final int START_PACKAGE_NAME_NOT_FOUND = -499;
    public static final int START_PERSISTENT_APP = -496;
    public static final int START_PRELOAD_IS_DISABLE = -486;
    public static final int START_SWITCH_PRELOAD_STATE_ERROR = -490;
    public static final int START_TOP_APP_IN_BLACK_LIST = -492;
    private static final String TAG = "PreloadAppControllerImpl";
    private static volatile int mPreloadLifecycleState;
    private static PreloadAppControllerImpl sInstance;
    private ActivityManagerService mActivityManagerService;
    private PreloadHandler mHandler;
    private ProcessManagerService mProcessManagerService;
    private volatile boolean mSpeedTestState;
    public static final boolean DEBUG = SystemProperties.getBoolean("persist.sys.preload.debug", true);
    private static boolean ENABLE = SystemProperties.getBoolean("persist.sys.preload.enable", false);
    public static final int[] sDefaultSchedAffinity = {0, 1, 2, 3, 4, 5, 6, 7};
    public static final int[] sPreloadSchedAffinity = {0, 1, 2, 3, 4, 5};
    private static Set<String> sWindowBlackList = new HashSet();
    private static Set<String> sBlackList = new HashSet();
    private static Set<String> sTopAppBlackList = new HashSet();
    private static Set<String> sRelaunchBlackList = new HashSet();
    private final Object mPreloadingAppLock = new Object();
    private List<PreloadLifecycle> mAllPreloadAppInfos = new LinkedList();

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<PreloadAppControllerImpl> {

        /* compiled from: PreloadAppControllerImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final PreloadAppControllerImpl INSTANCE = new PreloadAppControllerImpl();
        }

        public PreloadAppControllerImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public PreloadAppControllerImpl provideNewInstance() {
            return new PreloadAppControllerImpl();
        }
    }

    static {
        sBlackList.add("com.xiaomi.youpin");
        sBlackList.add("com.ss.android.ugc.live");
        sWindowBlackList.add(SplashScreenServiceDelegate.SPLASHSCREEN_PACKAGE);
        sTopAppBlackList.add(AccessController.PACKAGE_CAMERA);
        sRelaunchBlackList.add("com.ss.android.ugc");
    }

    public void init(ActivityManagerService ams, ProcessManagerService processManagerService, ServiceThread thread) {
        if (this.mHandler != null) {
            return;
        }
        sInstance = getInstance();
        this.mActivityManagerService = ams;
        this.mProcessManagerService = processManagerService;
        this.mHandler = new PreloadHandler(thread.getLooper());
        initPreloadEnableState();
    }

    private void initPreloadEnableState() {
        if ((Process.getTotalMemory() >> 30) < 6) {
            ENABLE = false;
            Slog.e(TAG, "preloadApp low total memory, disable");
            return;
        }
        ENABLE &= PreloadStateManagerImpl.checkEnablePreload();
    }

    public static PreloadAppControllerImpl getInstance() {
        return (PreloadAppControllerImpl) MiuiStubUtil.getImpl(PreloadAppControllerStub.class);
    }

    public static boolean isEnable() {
        return ENABLE;
    }

    public void preloadAppEnqueue(String packageName, boolean ignoreMemory, LifecycleConfig config) {
        PreloadLifecycle lifecycle = createPreloadLifeCycle(ignoreMemory, packageName, config);
        if (lifecycle != null) {
            Message msg = Message.obtain(this.mHandler, 1);
            msg.obj = lifecycle;
            this.mHandler.sendMessage(msg);
        }
    }

    public int startPreloadApp(PreloadLifecycle lifecycle) {
        String packageName = lifecycle.getPackageName();
        boolean ignoreMemory = lifecycle.isIgnoreMemory();
        LifecycleConfig config = lifecycle.getConfig();
        if (!ENABLE) {
            if (DEBUG) {
                Slog.w(TAG, "preloadApp is disable!!!");
                return START_PRELOAD_IS_DISABLE;
            }
            return START_PRELOAD_IS_DISABLE;
        } else if (this.mSpeedTestState && lifecycle.getPreloadType() != 1) {
            if (DEBUG) {
                Slog.w(TAG, "preloadApp fail! mSpeedTestState=" + this.mSpeedTestState);
                return START_FAIL_SPEED_TEST;
            }
            return START_FAIL_SPEED_TEST;
        } else if (!this.mProcessManagerService.checkPermission()) {
            String msg = " Permission Denial: from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid();
            if (DEBUG) {
                Slog.w(TAG, PREFIX + msg);
                return PERMISSION_DENIED;
            }
            return PERMISSION_DENIED;
        } else if (packageName == null) {
            return PACKAGE_NAME_EMPTY;
        } else {
            if (inBlackList(packageName)) {
                return START_PACKAGE_NAME_IN_BLACK_LIST;
            }
            if (config == null) {
                return START_CONFIG_NULL;
            }
            if (this.mProcessManagerService.frequentlyKilledForPreload(packageName) && !config.forceStart()) {
                if (DEBUG) {
                    Slog.w(TAG, "preloadApp skip " + packageName + ", because of errors or killed by user before");
                    return START_FREQUENTLY_KILLED;
                }
                return START_FREQUENTLY_KILLED;
            } else if (!ignoreMemory && ProcessUtils.isLowMemory()) {
                if (DEBUG) {
                    Slog.w(TAG, "low memory! skip preloadApp!");
                    return START_FORBIDDEN_LOW_MEMORY;
                }
                return START_FORBIDDEN_LOW_MEMORY;
            } else {
                WindowProcessController wpc = this.mActivityManagerService.mAtmInternal.getTopApp();
                ProcessRecord r = wpc != null ? (ProcessRecord) wpc.mOwner : null;
                if (r != null && inTopAppBlackList(r.processName)) {
                    if (DEBUG) {
                        Slog.w(TAG, "preloadApp skip " + packageName + "because topApp is " + r.processName);
                        return START_TOP_APP_IN_BLACK_LIST;
                    }
                    return START_TOP_APP_IN_BLACK_LIST;
                }
                try {
                    ApplicationInfo info = AppGlobals.getPackageManager().getApplicationInfo(packageName, 1024L, 0);
                    if (info == null) {
                        return START_PACKAGE_NAME_NOT_FOUND;
                    }
                    if ((1 & info.flags) > 0) {
                        return START_FORBIDDEN_SYSTEM_APP;
                    }
                    ProcessRecord app = (ProcessRecord) this.mActivityManagerService.mProcessList.getProcessNamesLOSP().get(packageName, info.uid);
                    if (app != null) {
                        if (app.isPersistent()) {
                            if (DEBUG) {
                                Slog.w(TAG, "preloadApp: " + packageName + " is persistent, skip!");
                                return START_PERSISTENT_APP;
                            }
                            return START_PERSISTENT_APP;
                        } else if (DEBUG) {
                            Slog.w(TAG, "preloadApp: " + packageName + " already exits, skip it");
                            return START_ALREADY_LOAD_APP;
                        } else {
                            return START_ALREADY_LOAD_APP;
                        }
                    }
                    lifecycle.setUid(info.uid);
                    return realStartPreloadApp(packageName, lifecycle);
                } catch (RemoteException e) {
                    if (DEBUG) {
                        Slog.w(TAG, "preloadApp error in getApplicationInfo!", e);
                    }
                    return START_PACKAGE_NAME_NOT_FOUND;
                }
            }
        }
    }

    private PreloadLifecycle createPreloadLifeCycle(boolean ignoreMemory, String packageName, LifecycleConfig config) {
        PreloadLifecycle lifecycle = new PreloadLifecycle(ignoreMemory, packageName, config);
        if (DEBUG) {
            Slog.w(TAG, "received preloadApp request, " + lifecycle);
        }
        synchronized (this.mPreloadingAppLock) {
            int size = this.mAllPreloadAppInfos.size();
            if (size >= 10) {
                Slog.w(TAG, size + " applications have been preloaded, no more preload");
                return null;
            }
            int uid = getUidFromPackageName(packageName);
            if (uid < 0) {
                this.mAllPreloadAppInfos.add(lifecycle);
                return lifecycle;
            }
            ProcessRecord app = (ProcessRecord) this.mActivityManagerService.mProcessList.getProcessNamesLOSP().get(packageName, uid);
            if (app == null) {
                return lifecycle;
            }
            Slog.w(TAG, "preloadApp: " + packageName + " already exits, skip it");
            return null;
        }
    }

    public Set<String> getPreloadingApps() {
        Set<String> packages = new HashSet<>();
        synchronized (this.mPreloadingAppLock) {
            for (PreloadLifecycle lifecycle : this.mAllPreloadAppInfos) {
                if (lifecycle.isAlreadyPreloaded()) {
                    packages.add(lifecycle.getPackageName());
                }
            }
        }
        return packages;
    }

    public static String getPackageName(int uid) {
        PreloadAppControllerImpl preloadAppControllerImpl = sInstance;
        if (preloadAppControllerImpl != null) {
            synchronized (preloadAppControllerImpl.mPreloadingAppLock) {
                for (PreloadLifecycle lifecycle : sInstance.mAllPreloadAppInfos) {
                    if (lifecycle.getUid() == uid) {
                        return lifecycle.getPackageName();
                    }
                }
                return null;
            }
        }
        return null;
    }

    public static int getUidFromPackageName(String packageName) {
        PreloadAppControllerImpl preloadAppControllerImpl = sInstance;
        if (preloadAppControllerImpl != null && packageName != null) {
            synchronized (preloadAppControllerImpl.mPreloadingAppLock) {
                for (PreloadLifecycle lifecycle : sInstance.mAllPreloadAppInfos) {
                    if (packageName.equals(lifecycle.getPackageName())) {
                        return lifecycle.getUid();
                    }
                }
                return -1;
            }
        }
        return -1;
    }

    private int getPreloadType(String packageName) {
        synchronized (this.mPreloadingAppLock) {
            for (PreloadLifecycle lifecycle : this.mAllPreloadAppInfos) {
                if (packageName.equals(lifecycle.getPackageName())) {
                    return lifecycle.getPreloadType();
                }
            }
            return -1;
        }
    }

    public void onPreloadAppStarted(int uid, String packageName, int pid) {
        removePreloadAppInfo(uid);
        Message msg = Message.obtain(this.mHandler, 5);
        msg.obj = packageName;
        msg.arg1 = pid;
        msg.arg2 = getPreloadType(packageName);
        this.mHandler.sendMessage(msg);
    }

    public void onPreloadAppKilled(int uid) {
        removePreloadAppInfo(uid);
    }

    public void onProcessKilled(String reason, String processName) {
        if (reason.contains(PreloadStateManagerImpl.TIMEOUT_KILL_PRELOADAPP_REASON)) {
            PreloadStateManagerImpl.enableAudio(getUidFromPackageName(processName));
        }
    }

    public void setSpeedTestState(boolean speedTestState) {
        this.mSpeedTestState = speedTestState;
    }

    private PreloadLifecycle removePreloadAppInfo(int uid) {
        PreloadLifecycle removeItem;
        if (DEBUG) {
            Slog.e(TAG, "preloadApp removePreloadAppInfo uid:" + uid);
        }
        synchronized (this.mPreloadingAppLock) {
            Iterator<PreloadLifecycle> iter = this.mAllPreloadAppInfos.iterator();
            removeItem = null;
            while (iter.hasNext()) {
                PreloadLifecycle p = iter.next();
                if (p.getUid() == uid) {
                    iter.remove();
                    removeItem = p;
                }
            }
        }
        return removeItem;
    }

    public PreloadLifecycle removePreloadAppInfo(String packageName) {
        PreloadLifecycle removeItem;
        if (packageName == null) {
            return null;
        }
        if (DEBUG) {
            Slog.e(TAG, "preloadApp removePreloadAppInfo packageName:" + packageName);
        }
        synchronized (this.mPreloadingAppLock) {
            Iterator<PreloadLifecycle> iter = this.mAllPreloadAppInfos.iterator();
            removeItem = null;
            while (iter.hasNext()) {
                PreloadLifecycle p = iter.next();
                if (packageName.equals(p.getPackageName())) {
                    iter.remove();
                    removeItem = p;
                }
            }
        }
        return removeItem;
    }

    private int realStartPreloadApp(String packageName, PreloadLifecycle lifecycle) {
        Intent intent = this.mActivityManagerService.mContext.getPackageManager().getLaunchIntentForPackage(packageName);
        if (intent != null) {
            if (DEBUG) {
                Slog.w(TAG, "preloadAppstart app: " + packageName);
            }
            intent.setPackage(null);
            ActivityOptions options = ActivityOptions.makeBasic();
            int displayId = PreloadStateManagerImpl.getOrCreatePreloadDisplayId(packageName);
            lifecycle.setDisplayId(displayId);
            options.setLaunchDisplayId(displayId);
            this.mHandler.postDelayed(new Runnable() { // from class: com.android.server.am.PreloadAppControllerImpl.1
                @Override // java.lang.Runnable
                public void run() {
                    PreloadAppControllerImpl.mPreloadLifecycleState = 3;
                    Slog.e(PreloadAppControllerImpl.TAG, "preloadApp PreloadLifecycle.PRE_STOP");
                }
            }, ActivityManagerServiceImpl.BOOST_DURATION);
            Slog.e(TAG, "preloadApp PreloadLifecycle.PRELOAD_SOON");
            mPreloadLifecycleState = 2;
            return this.mActivityManagerService.startActivity((IApplicationThread) null, (String) null, intent, (String) null, (IBinder) null, (String) null, 0, 0, (ProfilerInfo) null, options.toBundle());
        }
        return START_LAUNCH_INTENT_NOT_FOUND;
    }

    public boolean interceptWindowInjector(WindowManager.LayoutParams attrs) {
        if (inWindowBlackList(attrs.packageName)) {
            if (DEBUG) {
                Slog.w(TAG, "preloadApp inWindowBlackList");
            }
            if (mPreloadLifecycleState == 2) {
                Slog.w(TAG, "preloadApp window should add to PreloadDisplay");
                return true;
            }
            return false;
        }
        return false;
    }

    public static boolean inBlackList(String packageName) {
        return sBlackList.contains(packageName);
    }

    public static boolean inWindowBlackList(String packageName) {
        return sWindowBlackList.contains(packageName);
    }

    public static boolean inTopAppBlackList(String packageName) {
        return sTopAppBlackList.contains(packageName);
    }

    public static boolean inRelaunchBlackList(String packageName) {
        if (packageName == null) {
            return false;
        }
        for (String appName : sRelaunchBlackList) {
            if (packageName.contains(appName)) {
                return true;
            }
        }
        return false;
    }

    public static int[] querySchedAffinityFromUid(int uid) {
        PreloadAppControllerImpl preloadAppControllerImpl = sInstance;
        if (preloadAppControllerImpl != null) {
            synchronized (preloadAppControllerImpl.mPreloadingAppLock) {
                for (PreloadLifecycle lifecycle : sInstance.mAllPreloadAppInfos) {
                    if (lifecycle.getUid() == uid) {
                        return lifecycle.getPreloadSchedAffinity();
                    }
                }
            }
        }
        return sPreloadSchedAffinity;
    }

    public static long queryKillTimeoutFromUid(int uid) {
        PreloadAppControllerImpl preloadAppControllerImpl = sInstance;
        if (preloadAppControllerImpl != null) {
            synchronized (preloadAppControllerImpl.mPreloadingAppLock) {
                for (PreloadLifecycle lifecycle : sInstance.mAllPreloadAppInfos) {
                    if (lifecycle.getUid() == uid) {
                        return lifecycle.getKillTimeout();
                    }
                }
                return 300000L;
            }
        }
        return 300000L;
    }

    public static long queryFreezeTimeoutFromUid(int uid) {
        PreloadAppControllerImpl preloadAppControllerImpl = sInstance;
        if (preloadAppControllerImpl != null) {
            synchronized (preloadAppControllerImpl.mPreloadingAppLock) {
                for (PreloadLifecycle lifecycle : sInstance.mAllPreloadAppInfos) {
                    if (lifecycle.getUid() == uid) {
                        return lifecycle.getFreezeTimeout();
                    }
                }
                return 21000L;
            }
        }
        return 21000L;
    }

    public static long queryStopTimeoutFromUid(int uid) {
        PreloadAppControllerImpl preloadAppControllerImpl = sInstance;
        if (preloadAppControllerImpl != null) {
            synchronized (preloadAppControllerImpl.mPreloadingAppLock) {
                for (PreloadLifecycle lifecycle : sInstance.mAllPreloadAppInfos) {
                    if (lifecycle.getUid() == uid) {
                        return lifecycle.getStopTimeout();
                    }
                }
                return ActivityManagerServiceImpl.KEEP_FOREGROUND_DURATION;
            }
        }
        return ActivityManagerServiceImpl.KEEP_FOREGROUND_DURATION;
    }

    /* loaded from: classes.dex */
    public class PreloadHandler extends Handler {
        static final int MSG_ENQUEUE_PRELOAD_APP_REQUEST = 1;
        static final int MSG_EXECUTE_PRELOAD_APP = 2;
        static final int MSG_PRELOAD_APP_START = 5;
        static final int MSG_REGIST_PRELOAD_CALLBACK = 3;
        static final int MSG_UNREGIST_PRELOAD_CALLBACK = 4;
        private LinkedList<PreloadLifecycle> mPendingWork = new LinkedList<>();
        private SparseArray<IPreloadCallback> mCallBacks = new SparseArray<>(4);

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public PreloadHandler(Looper looper) {
            super(looper);
            PreloadAppControllerImpl.this = this$0;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    PreloadLifecycle preloadLifecycle = (PreloadLifecycle) msg.obj;
                    if (!this.mPendingWork.contains(preloadLifecycle)) {
                        this.mPendingWork.add(preloadLifecycle);
                        if (!hasMessages(2)) {
                            sendEmptyMessage(2);
                            return;
                        }
                        return;
                    }
                    return;
                case 2:
                    if (this.mPendingWork.size() == 0) {
                        return;
                    }
                    PreloadLifecycle lifecycle = this.mPendingWork.remove();
                    long timeout = 0;
                    if (lifecycle != null) {
                        int res = PreloadAppControllerImpl.this.startPreloadApp(lifecycle);
                        if (ActivityManager.isStartResultSuccessful(res)) {
                            timeout = lifecycle.getPreloadNextTimeout();
                            lifecycle.setAlreadyPreloaded(true);
                            if (PreloadAppControllerImpl.DEBUG) {
                                Slog.w(PreloadAppControllerImpl.TAG, "preloadApp SUCCESS preload:" + lifecycle.getPackageName() + " res=" + res);
                            }
                        } else {
                            Slog.w(PreloadAppControllerImpl.TAG, "preloadApp failed preload:" + lifecycle.getPackageName() + " res=" + res);
                            PreloadAppControllerImpl.this.removePreloadAppInfo(lifecycle.getPackageName());
                        }
                        int displayId = lifecycle.getDisplayId();
                        int type = lifecycle.getPreloadType();
                        int i = type | (displayId << 8);
                        int size = this.mCallBacks.size();
                        for (int i2 = 0; i2 < size; i2++) {
                            try {
                                this.mCallBacks.valueAt(i2).onPreloadComplete(lifecycle.getPreloadType(), res);
                            } catch (RemoteException e) {
                                if (PreloadAppControllerImpl.DEBUG) {
                                    Slog.e(PreloadAppControllerImpl.TAG, "complete callback fail, type " + this.mCallBacks.keyAt(i2));
                                }
                            }
                        }
                    }
                    sendEmptyMessageDelayed(2, timeout);
                    return;
                case 3:
                    this.mCallBacks.put(msg.arg1, (IPreloadCallback) msg.obj);
                    return;
                case 4:
                    this.mCallBacks.delete(msg.arg1);
                    return;
                case 5:
                    int size2 = this.mCallBacks.size();
                    String packageName = (String) msg.obj;
                    int pid = msg.arg1;
                    int type2 = msg.arg2;
                    for (int i3 = 0; i3 < size2; i3++) {
                        if (type2 == this.mCallBacks.keyAt(i3)) {
                            try {
                                this.mCallBacks.valueAt(i3).onPreloadAppStarted(packageName, pid);
                            } catch (RemoteException e2) {
                                if (PreloadAppControllerImpl.DEBUG) {
                                    Slog.e(PreloadAppControllerImpl.TAG, "onPreloadAppStarted callback fail, type " + this.mCallBacks.keyAt(i3));
                                }
                            }
                        }
                    }
                    return;
                default:
                    return;
            }
        }
    }

    public void unRegistPreloadCallback(int type) {
        Message msg = Message.obtain(this.mHandler, 4);
        msg.arg1 = type;
        this.mHandler.sendMessage(msg);
    }

    public void registerPreloadCallback(IPreloadCallback callback, int type) {
        try {
            callback.asBinder().linkToDeath(new CallbackDeathRecipient(type), 0);
            Slog.e(TAG, "registPreloadCallback , type=" + type + "pid=" + Binder.getCallingPid());
            Message msg = Message.obtain(this.mHandler, 3);
            msg.obj = callback;
            msg.arg1 = type;
            this.mHandler.sendMessage(msg);
        } catch (RemoteException e) {
            Slog.e(TAG, "registPreloadCallback fail due to linkToDeath, type " + type);
        }
    }

    public int killPreloadApp(String packageName) {
        if (!this.mProcessManagerService.checkPermission()) {
            return PERMISSION_DENIED;
        }
        if (packageName == null) {
            return PACKAGE_NAME_EMPTY;
        }
        synchronized (this.mPreloadingAppLock) {
            int uid = getUidFromPackageName(packageName);
            if (uid < 0) {
                return APP_NOT_PRELOAD;
            }
            Slog.e(TAG, Binder.getCallingPid() + " advance kill preloadApp " + packageName);
            PreloadStateManagerImpl.killPreloadApp(packageName, uid);
            return 500;
        }
    }

    /* loaded from: classes.dex */
    public class CallbackDeathRecipient implements IBinder.DeathRecipient {
        int type;

        CallbackDeathRecipient(int type) {
            PreloadAppControllerImpl.this = this$0;
            this.type = type;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            PreloadAppControllerImpl.this.unRegistPreloadCallback(this.type);
        }
    }
}
