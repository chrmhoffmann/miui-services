package com.android.server.wm;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.MiuiProcess;
import android.os.Process;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Slog;
import com.android.server.LocalServices;
import com.android.server.am.MiuiProcessStub;
import com.miui.base.MiuiStubRegistry;
import com.miui.base.MiuiStubUtil;
import com.miui.server.rtboost.SchedBoostManagerInternal;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
/* loaded from: classes.dex */
public class RealTimeModeControllerImpl implements RealTimeModeControllerStub {
    private static final String DEFAULT_GESTURE_WHITE_LIST = "com.miui.home,com.android.systemui,com.mi.android.globallauncher";
    private static final int MSG_REGISTER_CLOUD_OBSERVER = 1;
    private static final String RT_ENABLE_CLOUD = "perf_rt_enable";
    private static final String RT_GESTURE_ENABLE_CLOUD = "perf_rt_gesture_enable";
    private static final String RT_GESTURE_WHITE_LIST_CLOUD = "rt_gesture_white_list";
    private static final String RT_PKG_BLACK_LIST_CLOUD = "rt_pkg_black_list";
    private static final String RT_PKG_WHITE_LIST_CLOUD = "rt_pkg_white_list";
    public static final String TAG = "RTMode";
    private SchedBoostManagerInternal mBoostInternal;
    private WindowState mCurrentFocus;
    private Handler mH;
    private HandlerThread mHandlerThread;
    private WindowManagerService mService;
    private static Context mContext = null;
    public static final boolean DEBUG = SystemProperties.getBoolean("persist.sys.debug_rtmode", false);
    public static boolean ENABLE_RT_MODE = SystemProperties.getBoolean("persist.sys.enable_rtmode", true);
    public static boolean IGNORE_CLOUD_ENABLE = SystemProperties.getBoolean("persist.sys.enable_ignorecloud_rtmode", false);
    public static boolean ENABLE_RT_GESTURE = SystemProperties.getBoolean("persist.sys.enable_sched_gesture", true);
    public static final HashSet<String> RT_PKG_WHITE_LIST = new HashSet<>();
    public static final HashSet<String> RT_PKG_BLACK_LIST = new HashSet<>();
    public static final HashSet<String> RT_GESTURE_WHITE_LIST = new HashSet<>();
    private boolean isInit = false;
    private PackageManager mPm = null;
    private final HashMap<Integer, Boolean> mUidToAppBitType = new HashMap<>();

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<RealTimeModeControllerImpl> {

        /* compiled from: RealTimeModeControllerImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final RealTimeModeControllerImpl INSTANCE = new RealTimeModeControllerImpl();
        }

        public RealTimeModeControllerImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public RealTimeModeControllerImpl provideNewInstance() {
            return new RealTimeModeControllerImpl();
        }
    }

    public static RealTimeModeControllerImpl get() {
        return (RealTimeModeControllerImpl) MiuiStubUtil.getInstance(RealTimeModeControllerStub.class);
    }

    public void init(Context context) {
        if (this.isInit) {
            return;
        }
        mContext = context;
        Resources r = context.getResources();
        String[] whiteList = r.getStringArray(285409394);
        RT_PKG_WHITE_LIST.addAll(Arrays.asList(whiteList));
        RT_GESTURE_WHITE_LIST.addAll(Arrays.asList(DEFAULT_GESTURE_WHITE_LIST.split(",")));
        HandlerThread handlerThread = new HandlerThread("RealTimeModeControllerTh");
        this.mHandlerThread = handlerThread;
        handlerThread.start();
        Process.setThreadGroupAndCpuset(this.mHandlerThread.getThreadId(), 2);
        Handler handler = new Handler(this.mHandlerThread.getLooper()) { // from class: com.android.server.wm.RealTimeModeControllerImpl.1
            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case 1:
                        RealTimeModeControllerImpl.this.registerCloudObserver(RealTimeModeControllerImpl.mContext);
                        RealTimeModeControllerImpl.this.updateCloudControlParas();
                        return;
                    default:
                        return;
                }
            }
        };
        this.mH = handler;
        Message msg = handler.obtainMessage(1);
        this.mH.sendMessage(msg);
        this.isInit = true;
        Slog.d(TAG, "init RealTimeModeController");
    }

    public void registerCloudObserver(Context context) {
        ContentObserver observer = new ContentObserver(null) { // from class: com.android.server.wm.RealTimeModeControllerImpl.2
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                if (uri != null && uri.equals(Settings.System.getUriFor("perf_shielder_RTMODE"))) {
                    RealTimeModeControllerImpl.this.updateCloudControlParas();
                }
            }
        };
        context.getContentResolver().registerContentObserver(Settings.System.getUriFor("perf_shielder_RTMODE"), false, observer, -2);
        ContentObserver gestureObserver = new ContentObserver(null) { // from class: com.android.server.wm.RealTimeModeControllerImpl.3
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                if (uri != null && uri.equals(Settings.System.getUriFor("perf_shielder_GESTURE"))) {
                    RealTimeModeControllerImpl.updateGestureCloudControlParas();
                }
            }
        };
        context.getContentResolver().registerContentObserver(Settings.System.getUriFor("perf_shielder_GESTURE"), false, gestureObserver, -2);
    }

    public static void updateGestureCloudControlParas() {
        if (!IGNORE_CLOUD_ENABLE) {
            String rtGestureEnable = Settings.System.getStringForUser(mContext.getContentResolver(), RT_GESTURE_ENABLE_CLOUD, -2);
            if (rtGestureEnable != null) {
                ENABLE_RT_GESTURE = Boolean.parseBoolean(rtGestureEnable);
                if (DEBUG) {
                    Slog.d(TAG, "rtGestureEnable cloud control set received : " + ENABLE_RT_GESTURE);
                }
            }
            String rtGestureList = Settings.System.getStringForUser(mContext.getContentResolver(), RT_GESTURE_WHITE_LIST_CLOUD, -2);
            if (!TextUtils.isEmpty(rtGestureList)) {
                HashSet<String> hashSet = RT_GESTURE_WHITE_LIST;
                hashSet.clear();
                hashSet.addAll(Arrays.asList(rtGestureList.split(",")));
                if (DEBUG) {
                    Slog.d(TAG, "rtGestureList cloud control set received : " + rtGestureList);
                }
            }
        }
    }

    public void updateCloudControlParas() {
        if (!IGNORE_CLOUD_ENABLE) {
            String rtModeEnable = Settings.System.getStringForUser(mContext.getContentResolver(), RT_ENABLE_CLOUD, -2);
            if (rtModeEnable != null) {
                ENABLE_RT_MODE = Boolean.parseBoolean(rtModeEnable);
                if (DEBUG) {
                    Slog.d(TAG, "RTMode enable cloud control set received : " + ENABLE_RT_MODE);
                }
            }
            String rtWhiteList = Settings.System.getStringForUser(mContext.getContentResolver(), RT_PKG_WHITE_LIST_CLOUD, -2);
            if (!TextUtils.isEmpty(rtWhiteList)) {
                HashSet<String> hashSet = RT_PKG_WHITE_LIST;
                hashSet.clear();
                Resources r = mContext.getResources();
                String[] whiteList = r.getStringArray(285409394);
                hashSet.addAll(Arrays.asList(whiteList));
                hashSet.addAll(Arrays.asList(rtWhiteList.split(",")));
                if (DEBUG) {
                    Slog.d(TAG, "RTMode rtWhiteList cloud control set received : " + rtWhiteList);
                }
            }
            String rtBlackList = Settings.System.getStringForUser(mContext.getContentResolver(), RT_PKG_BLACK_LIST_CLOUD, -2);
            if (!TextUtils.isEmpty(rtBlackList)) {
                HashSet<String> hashSet2 = RT_PKG_BLACK_LIST;
                hashSet2.clear();
                hashSet2.addAll(Arrays.asList(rtBlackList.split(",")));
                if (DEBUG) {
                    Slog.d(TAG, "RTMode rtBlackList cloud control set received : " + rtBlackList);
                }
            }
        }
    }

    public boolean couldBoostTopAppProcess(String currentPackage) {
        if (!ENABLE_RT_MODE) {
            if (DEBUG) {
                Slog.d(TAG, "ENABLE_RT_MODE : " + ENABLE_RT_MODE);
            }
            return false;
        } else if (!RT_PKG_WHITE_LIST.contains(currentPackage) || RT_PKG_BLACK_LIST.contains(currentPackage)) {
            if (DEBUG) {
                Slog.d(TAG, currentPackage + "is not in whitelist or in blacklist!");
            }
            return false;
        } else if (isCurrentApp32Bit()) {
            if (DEBUG) {
                Slog.d(TAG, currentPackage + "is a 32bit app, skip boost!");
            }
            return false;
        } else {
            return true;
        }
    }

    public String getAppPackageName() {
        try {
            DisplayContent dc = this.mService.mRoot.getTopFocusedDisplayContent();
            if (dc != null) {
                WindowState windowState = dc.mCurrentFocus;
                this.mCurrentFocus = windowState;
                if (windowState != null) {
                    return windowState.mAttrs.packageName;
                }
            }
            return null;
        } catch (Exception e) {
            Slog.e(TAG, "failed to getAppPackageName", e);
            return null;
        }
    }

    public WindowProcessController getWindowProcessController() {
        try {
            WindowState windowState = this.mCurrentFocus;
            if (windowState == null) {
                return null;
            }
            ActivityRecord mActivityRecord = windowState.mActivityRecord;
            WindowProcessController mHomeProcess = this.mService.mAtmService.mHomeProcess;
            int mPid = this.mCurrentFocus.mSession.mPid;
            if (mActivityRecord == null) {
                return this.mService.mAtmService.mProcessMap.getProcess(mPid);
            }
            if (mHomeProcess != null && mHomeProcess.mName == mActivityRecord.packageName && mHomeProcess.getPid() != mPid) {
                return this.mService.mAtmService.mProcessMap.getProcess(mPid);
            }
            return mActivityRecord.app;
        } catch (Exception e) {
            Slog.e(TAG, "failed to getWindowProcessController", e);
            return null;
        }
    }

    public boolean checkCallerPermission(String pkgName) {
        if (!ENABLE_RT_GESTURE || !ENABLE_RT_MODE) {
            if (DEBUG) {
                Slog.d(TAG, "ENABLE_RT_MODE : " + ENABLE_RT_MODE + "ENABLE_RT_GESTURE : " + ENABLE_RT_GESTURE);
            }
            return false;
        } else if (pkgName != null && RT_GESTURE_WHITE_LIST.contains(pkgName)) {
            return true;
        } else {
            if (DEBUG) {
                Slog.d(TAG, pkgName + "pkgName is null or has no permission");
            }
            return false;
        }
    }

    public void setWindowManager(WindowManagerService mService) {
        this.mService = mService;
    }

    public void onFling(int durationMs) {
    }

    public void onScroll(boolean started) {
        if (!started) {
            return;
        }
        boostTopApp(MiuiProcessStub.getInstance().getSchedModeNormal(), MiuiProcessStub.getInstance().getScrollRtSchedDurationMs());
    }

    public void onDown() {
        boostTopApp(MiuiProcessStub.getInstance().getSchedModeNormal(), MiuiProcessStub.getInstance().getTouchRtSchedDurationMs());
    }

    private void boostTopApp(int mode, long durationMs) {
        String focusedPackage = getAppPackageName();
        if (focusedPackage == null) {
            Slog.e(TAG, "Error: package name null");
            return;
        }
        boolean couldBoost = couldBoostTopAppProcess(focusedPackage);
        if (couldBoost) {
            WindowProcessController wpc = getWindowProcessController();
            if (wpc == null) {
                Slog.e(TAG, "Error: wpc object null");
            } else {
                wpc.setSchedMode(mode, durationMs);
            }
        }
    }

    private boolean isCurrentApp32Bit() {
        int currentUid = this.mCurrentFocus.mOwnerUid;
        if (currentUid < 10000) {
            return false;
        }
        if (!this.mUidToAppBitType.containsKey(Integer.valueOf(currentUid))) {
            boolean is32Bit = MiuiProcess.is32BitApp(mContext, this.mCurrentFocus.mAttrs.packageName);
            this.mUidToAppBitType.put(Integer.valueOf(currentUid), Boolean.valueOf(is32Bit));
        }
        return this.mUidToAppBitType.get(Integer.valueOf(currentUid)).booleanValue();
    }

    private SchedBoostManagerInternal getSchedBoostService() {
        if (this.mBoostInternal == null) {
            this.mBoostInternal = (SchedBoostManagerInternal) LocalServices.getService(SchedBoostManagerInternal.class);
        }
        return this.mBoostInternal;
    }

    public boolean checkThreadBoost(int tid) {
        if (this.isInit && ENABLE_RT_GESTURE && ENABLE_RT_MODE) {
            return getSchedBoostService().checkThreadBoost(tid);
        }
        return false;
    }

    public void setThreadSavedPriority(int[] tid, int prio) {
        if (!this.isInit || !ENABLE_RT_GESTURE || !ENABLE_RT_MODE) {
            return;
        }
        getSchedBoostService().setThreadSavedPriority(tid, prio);
    }

    public static void dump(PrintWriter pw, String[] args) {
        pw.println("persist.sys.debug_rtmode: " + DEBUG);
        pw.println("persist.sys.enable_rtmode: " + ENABLE_RT_MODE);
        pw.println("persist.sys.enable_ignorecloud_rtmode: " + IGNORE_CLOUD_ENABLE);
        pw.println("persist.sys.enable_sched_gesture: " + ENABLE_RT_GESTURE);
        pw.println("RT_PKG_WHITE_LIST: ");
        HashSet<String> hashSet = RT_PKG_WHITE_LIST;
        synchronized (hashSet) {
            Iterator<String> it = hashSet.iterator();
            while (it.hasNext()) {
                String white = it.next();
                pw.println("    " + white);
            }
        }
        pw.println("RT_PKG_BLACK_LIST: ");
        HashSet<String> hashSet2 = RT_PKG_BLACK_LIST;
        synchronized (hashSet2) {
            Iterator<String> it2 = hashSet2.iterator();
            while (it2.hasNext()) {
                String black = it2.next();
                pw.println("    " + black);
            }
        }
        pw.println("RT_GESTURE_WHITE_LIST: ");
        HashSet<String> hashSet3 = RT_GESTURE_WHITE_LIST;
        synchronized (hashSet3) {
            Iterator<String> it3 = hashSet3.iterator();
            while (it3.hasNext()) {
                String gest_white = it3.next();
                pw.println("    " + gest_white);
            }
        }
    }
}
