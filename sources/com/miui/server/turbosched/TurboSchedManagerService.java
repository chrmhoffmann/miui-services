package com.miui.server.turbosched;

import android.app.ActivityManager;
import android.app.IActivityManager;
import android.app.IProcessObserver;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.NativeTurboSchedManager;
import android.os.Parcel;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.TurboSchedMonitor;
import android.provider.Settings;
import android.server.am.SplitScreenReporter;
import android.util.LocalLog;
import android.util.Slog;
import com.android.internal.util.DumpUtils;
import com.android.server.am.ProcessUtils;
import com.android.server.inputmethod.InputMethodManagerServiceImpl;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import miui.turbosched.ITurboSchedManager;
/* loaded from: classes.dex */
public class TurboSchedManagerService extends ITurboSchedManager.Stub {
    private static final String BOARD_TEMP_FILE = "/sys/class/thermal/thermal_message/board_sensor_temp";
    public static final String BOOST_ENABLE_PATH = "/sys/module/metis/parameters/mi_fboost_enable";
    public static final String BOOST_SCHED_PATH = "/sys/module/metis/parameters/boost_task";
    public static final String BOOST_THERMAL_PATH = "/sys/class/thermal/thermal_message/boost";
    private static final String CLOUD_TURBO_SCHED_ALLOW_LIST = "cloud_turbo_sched_allow_list";
    private static final String CLOUD_TURBO_SCHED_ENABLE_ALL = "cloud_turbo_sched_enable";
    private static final String CLOUD_TURBO_SCHED_ENABLE_CORE_APP_OPTIMIZER = "cloud_turbo_sched_enable_core_app_optimizer";
    private static final String CLOUD_TURBO_SCHED_ENABLE_V2 = "cloud_turbo_sched_enable_v2";
    private static final String CLOUD_TURBO_SCHED_LINK_APP_LIST = "cloud_turbo_sched_link_app_list";
    private static final String CLOUD_TURBO_SCHED_POLICY_LIST = "cloud_turbo_sched_policy_list";
    private static final String CLOUD_TURBO_SCHED_THERMAL_BREAK_ENABLE = "cloud_turbo_sched_thermal_break_enable";
    private static final String COMMAND_DRY_RUN = "dry_run";
    private static final String ClOUD_TURBO_SCHED_THERMAL_BREAK_THRESHOLD = "cloud_turbo_sched_thermal_break_threshold";
    public static final boolean DEBUG;
    private static final int GET_BUFFER_TX_COUNT_TRANSACTION = 1011;
    protected static final int MAX_HISTORY_ITEMS;
    private static final int METIS_BOOST_DURATION = 40;
    private static final String METIS_BOOST_DURATION_PATH = "sys/module/metis/parameters/mi_boost_duration";
    private static final String METIS_DEV_PATH = "/dev/metis";
    private static final int METIS_IOCTL_BOOST_CMD = 6;
    private static final String NO_THERMAL_BREAK_STRING = " cpu4:2419200 cpu7:2841600";
    public static final String ORI_TURBO_SCHED_PATH = "/sys/module/migt/parameters/mi_viptask";
    private static final String POLICY_NAME_BOOST_WITH_FREQUENCY = "bwf";
    private static final String POLICY_NAME_LINK = "link";
    private static final String POLICY_NAME_LITTLE_CORE = "lc";
    private static final String POLICY_NAME_PRIORITY = "priority";
    private static final String PROCESS_ARTICLE_NEWS = "com.ss.android.article.news";
    private static final String PROCESS_AWEME = "com.ss.android.ugc.aweme";
    private static final String PROCESS_BILIBILI = "tv.danmaku.bili";
    private static final String PROCESS_GIFMAKER = "com.smile.gifmaker";
    private static final String PROCESS_TAOBAO = "com.taobao.taobao";
    private static final String PROCESS_WECHAT = "com.tencent.mm";
    private static final String PROCESS_WEIBO = "com.sina.weibo";
    public static final String RECORD_RT_PATH = "/sys/module/metis/parameters/rt_binder_client";
    public static final String SERVICE_NAME = "turbosched";
    public static final String TAG = "TurboSchedManagerService";
    private static Map<Integer, String> TEMPREATURE_THROSHOLD_BOOST_STRING_MAP = null;
    private static final String TURBO_SCHED_BOOST_WITH_FREQUENCY_PATH = "/sys/module/metis/parameters/add_mi_viptask_enqueue_boost";
    private static final String TURBO_SCHED_CANCEL_BOOST_WITH_FREQUENCY_PATH = "/sys/module/metis/parameters/del_mi_viptask_enqueue_boost";
    private static final String TURBO_SCHED_CANCEL_LITTLE_CORE_PATH = "/sys/module/metis/parameters/del_mi_viptask_sched_lit_core";
    private static final String TURBO_SCHED_CANCEL_PRIORITY_PATH = "/sys/module/metis/parameters/del_mi_viptask_sched_priority";
    private static final String TURBO_SCHED_LITTLE_CORE_PATH = "/sys/module/metis/parameters/add_mi_viptask_sched_lit_core";
    public static final String TURBO_SCHED_PATH = "/sys/module/metis/parameters/mi_viptask";
    private static final String TURBO_SCHED_PRIORITY_PATH = "/sys/module/metis/parameters/add_mi_viptask_sched_priority";
    private static final String TURBO_SCHED_THERMAL_BREAK_PATH = "/sys/module/metis/parameters/is_break_enable";
    private static final String VERSION = "v2.0.18";
    public static final String VIP_LINK_PATH = "/sys/module/metis/parameters/vip_link_enable";
    private static Object mTurboLock;
    final ContentObserver mCloudAllowListObserver;
    final ContentObserver mCloudCoreAppOptimizerEnableObserver;
    final ContentObserver mCloudLinkWhiteListObserver;
    final ContentObserver mCloudPolicyListObserver;
    final ContentObserver mCloudSwichObserver;
    final ContentObserver mCloudThermalBreakEnableObserver;
    final ContentObserver mCloudThermalBreakThresholdObserver;
    private Context mContext;
    public HandlerThread mHandlerThread;
    private long mLastVsyncId;
    private PackageManager mPm;
    private boolean mTurboNodeExist;
    private TurboSchedHandler mTurboSchedHandler;
    private final String BufferTxCountPath = "/sys/module/metis/parameters/buffer_tx_count";
    private boolean mThermalBreakEnabled = SystemProperties.getBoolean("persist.sys.turbosched.thermal_break.enable", false);
    private LocalLog mHistoryLog = new LocalLog(MAX_HISTORY_ITEMS);
    private boolean mTurboEnabled = SystemProperties.getBoolean("persist.sys.turbosched.enable", false);
    private boolean mTurboEnabledV2 = SystemProperties.getBoolean("persist.sys.turbosched.enable_v2", false);
    private boolean mCloudControlEnabled = false;
    private IBinder mSurfaceFlinger = null;
    private List<String> mCallerAllowList = new ArrayList(Arrays.asList(InputMethodManagerServiceImpl.MIUI_HOME, "com.miui.personalassistant"));
    private boolean mIsOnScroll = false;
    private String mProcessName = "";
    private long mFgCorePid = -1;
    private long mFgCoreUid = -1;
    private List<String> mPolicyList = new ArrayList(Arrays.asList(POLICY_NAME_LINK));
    private boolean mRegistedForegroundReceiver = false;
    private boolean mReceiveCloudData = SystemProperties.getBoolean("persist.sys.turbosched.receive.cloud.data", true);
    private List<String> m8550Devices = new ArrayList(Arrays.asList("nuwa", "fuxi", "socrates"));
    private boolean mLinkEnable = SystemProperties.getBoolean("persist.sys.turbosched.link.enable", true);
    private List<String> mCoreAppsPackageNameList = new ArrayList(Arrays.asList(PROCESS_GIFMAKER, PROCESS_WEIBO, PROCESS_ARTICLE_NEWS, PROCESS_TAOBAO, PROCESS_WECHAT, PROCESS_AWEME, PROCESS_BILIBILI));
    private List<String> mAppsLinkVipList = new ArrayList(Arrays.asList(PROCESS_WEIBO, "com.kuaishou.nebula", PROCESS_AWEME, "com.tencent.mobileqq", "com.autonavi.minimap", "com.tencent.news", "com.alibaba.android.rimet", "cn.xuexi.android", "com.zhihu.android", "com.kugou.android", "com.hunantv.imgo.activity", "com.tencent.qqmusic", PROCESS_BILIBILI));
    private boolean mScreenOn = true;
    private Map<Integer, Long> mBWFTidStartTimeMap = new HashMap();
    private Map<Integer, Long> mPriorityTidStartTimeMap = new HashMap();
    private Map<Integer, Long> mLCTidStartTimeMap = new HashMap();
    private final IProcessObserver.Stub mProcessObserver = new IProcessObserver.Stub() { // from class: com.miui.server.turbosched.TurboSchedManagerService.8
        public void onForegroundActivitiesChanged(int pid, int uid, boolean foregroundActivities) {
            String curProcessName = TurboSchedManagerService.this.getProcessNameByPid(pid);
            boolean is_enable = TurboSchedMonitor.getInstance().isCoreAppOptimizerEnabled();
            if (!is_enable) {
                return;
            }
            if (foregroundActivities && TurboSchedManagerService.this.mScreenOn) {
                TurboSchedManagerService.this.mProcessName = curProcessName;
                if (TurboSchedManagerService.this.mProcessName != null && TurboSchedMonitor.getInstance().isCoreApp(TurboSchedManagerService.this.mProcessName)) {
                    TurboSchedManagerService.this.mFgCorePid = pid;
                    TurboSchedManagerService.this.mFgCoreUid = uid;
                }
            }
            if (!TurboSchedManagerService.this.mLinkEnable || curProcessName == null || !TurboSchedManagerService.this.mAppsLinkVipList.contains(curProcessName)) {
                return;
            }
            if (foregroundActivities && TurboSchedManagerService.this.mScreenOn) {
                boolean isSuccese = TurboSchedManagerService.this.writeTurboSchedNode("LINK", TurboSchedManagerService.VIP_LINK_PATH, SplitScreenReporter.ACTION_ENTER_SPLIT);
                if (TurboSchedMonitor.getInstance().isDebugMode()) {
                    Slog.i(TurboSchedManagerService.TAG, "LinkVip, isSuccese:" + isSuccese + " is_link_on " + SplitScreenReporter.ACTION_ENTER_SPLIT);
                    return;
                }
                return;
            }
            boolean isSuccese2 = TurboSchedManagerService.this.writeTurboSchedNode("LINK", TurboSchedManagerService.VIP_LINK_PATH, "0");
            if (TurboSchedMonitor.getInstance().isDebugMode()) {
                Slog.i(TurboSchedManagerService.TAG, "LinkVip, isSuccese:" + isSuccese2 + " is_link_on 0");
            }
        }

        public void onForegroundServicesChanged(int pid, int uid, int serviceTypes) {
        }

        public void onProcessDied(int pid, int uid) {
        }
    };
    private final IActivityManager mActivityManagerService = ActivityManager.getService();
    private List<Long> mBoostDuration = TurboSchedMonitor.getInstance().getBoostDuration();

    static {
        boolean z = Build.IS_DEBUGGABLE;
        DEBUG = z;
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        TEMPREATURE_THROSHOLD_BOOST_STRING_MAP = linkedHashMap;
        linkedHashMap.put(50, " cpu4:2112000 cpu7:2054400");
        TEMPREATURE_THROSHOLD_BOOST_STRING_MAP.put(45, " cpu4:2227200 cpu7:2169600");
        TEMPREATURE_THROSHOLD_BOOST_STRING_MAP.put(Integer.valueOf((int) METIS_BOOST_DURATION), " cpu4:2342400 cpu7:2284800");
        TEMPREATURE_THROSHOLD_BOOST_STRING_MAP.put(35, " cpu4:2419200 cpu7:2400000");
        MAX_HISTORY_ITEMS = z ? 512 : 256;
        mTurboLock = new Object();
    }

    public TurboSchedManagerService(Context context) {
        this.mTurboNodeExist = false;
        this.mHandlerThread = null;
        this.mTurboSchedHandler = null;
        this.mPm = null;
        this.mCloudSwichObserver = new ContentObserver(this.mTurboSchedHandler) { // from class: com.miui.server.turbosched.TurboSchedManagerService.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                if (uri != null && uri.equals(Settings.System.getUriFor(TurboSchedManagerService.CLOUD_TURBO_SCHED_ENABLE_ALL))) {
                    TurboSchedManagerService.this.updateEnableProp();
                }
            }
        };
        this.mCloudCoreAppOptimizerEnableObserver = new ContentObserver(this.mTurboSchedHandler) { // from class: com.miui.server.turbosched.TurboSchedManagerService.2
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                if (uri != null && uri.equals(Settings.System.getUriFor(TurboSchedManagerService.CLOUD_TURBO_SCHED_ENABLE_CORE_APP_OPTIMIZER))) {
                    TurboSchedManagerService.this.updateCoreAppOptimizerEnableProp();
                }
            }
        };
        this.mCloudAllowListObserver = new ContentObserver(this.mTurboSchedHandler) { // from class: com.miui.server.turbosched.TurboSchedManagerService.3
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                if (uri != null && uri.equals(Settings.System.getUriFor(TurboSchedManagerService.CLOUD_TURBO_SCHED_ALLOW_LIST))) {
                    TurboSchedManagerService.this.updateCloudAllowListProp();
                }
            }
        };
        this.mCloudPolicyListObserver = new ContentObserver(this.mTurboSchedHandler) { // from class: com.miui.server.turbosched.TurboSchedManagerService.4
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                if (uri != null && uri.equals(Settings.System.getUriFor(TurboSchedManagerService.CLOUD_TURBO_SCHED_POLICY_LIST))) {
                    TurboSchedManagerService.this.updateCloudPolicyListProp();
                }
            }
        };
        this.mCloudThermalBreakEnableObserver = new ContentObserver(this.mTurboSchedHandler) { // from class: com.miui.server.turbosched.TurboSchedManagerService.5
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                if (uri != null && uri.equals(Settings.System.getUriFor(TurboSchedManagerService.CLOUD_TURBO_SCHED_THERMAL_BREAK_ENABLE))) {
                    TurboSchedManagerService.this.updateCloudThermalBreakEnableProp();
                }
            }
        };
        this.mCloudThermalBreakThresholdObserver = new ContentObserver(this.mTurboSchedHandler) { // from class: com.miui.server.turbosched.TurboSchedManagerService.6
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                if (uri != null && uri.equals(Settings.System.getUriFor(TurboSchedManagerService.ClOUD_TURBO_SCHED_THERMAL_BREAK_THRESHOLD))) {
                    TurboSchedManagerService.this.updateCloudThermalBreakThresholdProp();
                }
            }
        };
        this.mCloudLinkWhiteListObserver = new ContentObserver(this.mTurboSchedHandler) { // from class: com.miui.server.turbosched.TurboSchedManagerService.7
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                if (uri != null && uri.equals(Settings.System.getUriFor(TurboSchedManagerService.CLOUD_TURBO_SCHED_LINK_APP_LIST))) {
                    TurboSchedManagerService.this.updateCloudLinkWhiteListProp();
                }
            }
        };
        this.mContext = context;
        HandlerThread handlerThread = new HandlerThread(SERVICE_NAME);
        this.mHandlerThread = handlerThread;
        handlerThread.start();
        this.mTurboSchedHandler = new TurboSchedHandler(this.mHandlerThread.getLooper());
        this.mPm = this.mContext.getPackageManager();
        this.mTurboNodeExist = checkFileAccess(TURBO_SCHED_PATH);
        registeForegroundReceiver();
        if (this.mReceiveCloudData) {
            registerObserver();
            updateCloudControlProp();
        } else {
            updateLocalProp();
        }
        new TurboSchedBroadcastReceiver();
        String str = String.valueOf((int) METIS_BOOST_DURATION);
        writeTurboSchedNode("B-Duration", METIS_BOOST_DURATION_PATH, str);
    }

    private boolean isTurboEnabled() {
        if (this.mTurboEnabled) {
            return true;
        }
        return this.mTurboEnabledV2;
    }

    private boolean setTurboSchedActionInternal(int[] tids, long time, String path) {
        boolean isSuccese;
        if (isTurboEnabled() && tids.length <= 3) {
            sendTraceBeginMsg("V");
            if (TurboSchedMonitor.getInstance().isDebugMode()) {
                Trace.traceBegin(64L, "setTurboSchedActionInternal");
            }
            synchronized (mTurboLock) {
                StringBuilder sb = new StringBuilder();
                sb.append(tids[0]);
                for (int i = 1; i < tids.length && tids[i] != 0; i++) {
                    sb.append(":");
                    sb.append(tids[i]);
                }
                sb.append("-");
                sb.append(time);
                String str = sb.toString();
                isSuccese = writeTurboSchedNode("V", path, str);
                if (!isSuccese && TurboSchedMonitor.getInstance().isDebugMode()) {
                    Slog.d(TAG, "setTurboSchedActionInternal, not success, check file:" + path);
                }
            }
            if (TurboSchedMonitor.getInstance().isDebugMode()) {
                Trace.traceEnd(64L);
            }
            sendTraceEndMsg();
            return isSuccese;
        }
        return false;
    }

    public boolean writeTurboSchedNode(String logTag, String path, String value) {
        File file = new File(path);
        if (!file.exists()) {
            return false;
        }
        try {
            PrintWriter writer = new PrintWriter(path);
            writer.write(value);
            this.mHistoryLog.log("[ " + logTag + " ] write message : " + value);
            writer.close();
            return true;
        } catch (IOException e) {
            Slog.w(TAG, "Failed to write path : " + path + "  value : " + value, e);
            return false;
        }
    }

    /* loaded from: classes.dex */
    public final class TurboSchedHandler extends Handler {
        static final int MSG_TRACE_BEGIN = 0;
        static final int MSG_TRACE_END = 1;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        private TurboSchedHandler(Looper looper) {
            super(looper);
            TurboSchedManagerService.this = r1;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            int what = msg.what;
            switch (what) {
                case 0:
                    TraceInfo info = (TraceInfo) msg.obj;
                    traceBegin(info.name, info.desc, info.startTime);
                    int tid = msg.arg1;
                    Trace.traceBegin(64L, "P-Tid:" + tid);
                    return;
                case 1:
                    Trace.traceEnd(64L);
                    traceEnd();
                    return;
                default:
                    return;
            }
        }

        private void traceBegin(String name, String desc, long stratTime) {
            long delay = System.currentTimeMillis() - stratTime;
            if (desc != null && !desc.isEmpty()) {
                desc = "(" + desc + ")";
            }
            String tag = name + desc + " delay=" + delay;
            Trace.traceBegin(64L, tag);
        }

        private void traceEnd() {
            Trace.traceEnd(64L);
        }
    }

    private void sendTraceBeginMsg(String name) {
        sendTraceBeginMsg(name, "");
    }

    private void sendTraceBeginMsg(String name, String desc) {
        if (this.mTurboSchedHandler == null) {
            Slog.d(TAG, "handler is null while sending trace begin message");
            return;
        }
        TraceInfo info = new TraceInfo();
        info.name = name;
        info.desc = desc;
        info.startTime = System.currentTimeMillis();
        Message msg = this.mTurboSchedHandler.obtainMessage(0, info);
        int tid = Process.myTid();
        msg.arg1 = tid;
        this.mTurboSchedHandler.sendMessage(msg);
    }

    private void sendTraceEndMsg() {
        TurboSchedHandler turboSchedHandler = this.mTurboSchedHandler;
        if (turboSchedHandler == null) {
            Slog.d(TAG, "handler is null while sending trace end message");
            return;
        }
        Message msg = turboSchedHandler.obtainMessage(1);
        this.mTurboSchedHandler.sendMessage(msg);
    }

    private void registerObserver() {
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_TURBO_SCHED_ENABLE_ALL), false, this.mCloudSwichObserver, -2);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_TURBO_SCHED_ENABLE_V2), false, this.mCloudSwichObserver, -2);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_TURBO_SCHED_ENABLE_CORE_APP_OPTIMIZER), false, this.mCloudCoreAppOptimizerEnableObserver, -2);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_TURBO_SCHED_ALLOW_LIST), false, this.mCloudAllowListObserver, -2);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_TURBO_SCHED_POLICY_LIST), false, this.mCloudPolicyListObserver, -2);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_TURBO_SCHED_THERMAL_BREAK_ENABLE), false, this.mCloudThermalBreakEnableObserver, -2);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_TURBO_SCHED_LINK_APP_LIST), false, this.mCloudLinkWhiteListObserver, -2);
    }

    private void updateCloudControlProp() {
        updateEnableProp();
        updateCoreAppOptimizerEnableProp();
        updateCloudAllowListProp();
        updateCloudPolicyListProp();
        updateCloudThermalBreakEnableProp();
        updateCloudThermalBreakThresholdProp();
        updateCloudLinkWhiteListProp();
    }

    private void updateLocalProp() {
        enableFrameBoostInKernel(TurboSchedMonitor.getInstance().isCoreAppOptimizerEnabled());
        setThermalBreakEnable(this.mThermalBreakEnabled);
    }

    public void updateEnableProp() {
        String enableStr = Settings.System.getStringForUser(this.mContext.getContentResolver(), CLOUD_TURBO_SCHED_ENABLE_ALL, -2);
        if (enableStr == null || enableStr.isEmpty()) {
            enableStr = Settings.System.getStringForUser(this.mContext.getContentResolver(), CLOUD_TURBO_SCHED_ENABLE_V2, -2);
        } else {
            TurboSchedMonitor.getInstance().enableCoreAppOptimizer(Boolean.parseBoolean(enableStr) ? 1 : 0);
        }
        if (enableStr != null && !enableStr.isEmpty()) {
            this.mCloudControlEnabled = Boolean.parseBoolean(enableStr);
            Slog.d(TAG, "cloud control set received :" + this.mCloudControlEnabled);
            enableTurboSched(this.mCloudControlEnabled);
            if (!this.mRegistedForegroundReceiver) {
                registeForegroundReceiver();
            }
        }
        enableFrameBoostInKernel(TurboSchedMonitor.getInstance().isCoreAppOptimizerEnabled());
    }

    public void updateCoreAppOptimizerEnableProp() {
        String enableStr = Settings.System.getStringForUser(this.mContext.getContentResolver(), CLOUD_TURBO_SCHED_ENABLE_CORE_APP_OPTIMIZER, -2);
        if (enableStr != null && !enableStr.isEmpty()) {
            boolean coreAppOptimizerEnabled = Boolean.parseBoolean(enableStr);
            Slog.d(TAG, "cloud core app optimizer set received :" + coreAppOptimizerEnabled);
            TurboSchedMonitor.getInstance().enableCoreAppOptimizer(coreAppOptimizerEnabled ? 1 : 0);
            if (!this.mRegistedForegroundReceiver) {
                registeForegroundReceiver();
            }
            enableFrameBoostInKernel(coreAppOptimizerEnabled);
        }
    }

    private void registeForegroundReceiver() {
        if (TurboSchedMonitor.getInstance().isCoreAppOptimizerEnabled()) {
            try {
                Slog.i(TAG, "registerProcessObserver!");
                this.mActivityManagerService.registerProcessObserver(this.mProcessObserver);
                this.mRegistedForegroundReceiver = true;
            } catch (RemoteException e) {
                Slog.e(TAG, "registerProcessObserver failed");
            }
        }
    }

    public void updateCloudAllowListProp() {
        String str = Settings.System.getStringForUser(this.mContext.getContentResolver(), CLOUD_TURBO_SCHED_ALLOW_LIST, -2);
        if (str != null && !str.isEmpty()) {
            String[] pkgName = str.split(",");
            this.mCallerAllowList.clear();
            if (pkgName.length > 0) {
                for (String name : pkgName) {
                    this.mCallerAllowList.add(name);
                }
            }
        }
    }

    public void updateCloudPolicyListProp() {
        String str = Settings.System.getStringForUser(this.mContext.getContentResolver(), CLOUD_TURBO_SCHED_POLICY_LIST, -2);
        if (str != null && !str.isEmpty()) {
            String[] pkgName = str.split(",");
            this.mPolicyList.clear();
            if (pkgName.length > 0) {
                for (String name : pkgName) {
                    this.mPolicyList.add(name);
                }
            }
        }
    }

    public void updateCloudThermalBreakEnableProp() {
        String enableStr = Settings.System.getStringForUser(this.mContext.getContentResolver(), CLOUD_TURBO_SCHED_THERMAL_BREAK_ENABLE, -2);
        if (enableStr != null && !enableStr.isEmpty()) {
            setThermalBreakEnable(Boolean.parseBoolean(enableStr));
        }
    }

    private void setThermalBreakEnable(boolean enable) {
        Slog.d(TAG, "cloud thermal break set received :" + enable);
        this.mThermalBreakEnabled = enable;
        SystemProperties.set("persist.sys.turbosched.thermal_break.enable", Boolean.toString(enable));
        String enableStr = this.mThermalBreakEnabled ? SplitScreenReporter.ACTION_ENTER_SPLIT : "0";
        boolean isSuccess = writeTurboSchedNode("TB-Enabled", TURBO_SCHED_THERMAL_BREAK_PATH, enableStr);
        if (!isSuccess) {
            Slog.e(TAG, "write turbo sched thermal break failed");
        }
    }

    public void updateCloudThermalBreakThresholdProp() {
        String thresholdStr = Settings.System.getStringForUser(this.mContext.getContentResolver(), ClOUD_TURBO_SCHED_THERMAL_BREAK_THRESHOLD, -2);
        setThermalBreakThresholdMap(thresholdStr);
    }

    public void updateCloudLinkWhiteListProp() {
        String str = Settings.System.getStringForUser(this.mContext.getContentResolver(), CLOUD_TURBO_SCHED_LINK_APP_LIST, -2);
        if (str != null && !str.isEmpty()) {
            String[] pkgName = str.split(",");
            this.mAppsLinkVipList.clear();
            if (pkgName.length > 0) {
                for (String name : pkgName) {
                    this.mAppsLinkVipList.add(name);
                }
            }
        }
    }

    private void setThermalBreakThresholdMap(String thresholdStr) {
        if (thresholdStr != null && !thresholdStr.isEmpty()) {
            Slog.d(TAG, "thermal break threshold set received :" + thresholdStr);
            String[] thresholdList = thresholdStr.split(",");
            for (String t : thresholdList) {
                String[] thresholdValues = t.split("-");
                if (thresholdValues.length == 2) {
                    int temp = Integer.parseInt(thresholdValues[0]);
                    String threshold = " " + thresholdValues[1].trim();
                    TEMPREATURE_THROSHOLD_BOOST_STRING_MAP.put(Integer.valueOf(temp), threshold.replace("_", " "));
                }
            }
        }
    }

    public boolean setTurboSchedAction(int[] tid, long time) {
        int uid = Binder.getCallingUid();
        if (!checkCallerPermmsion(uid)) {
            return false;
        }
        if (this.mTurboNodeExist) {
            boolean ret = setTurboSchedActionInternal(tid, time, TURBO_SCHED_PATH);
            return ret;
        }
        boolean ret2 = setTurboSchedActionInternal(tid, time, ORI_TURBO_SCHED_PATH);
        return ret2;
    }

    public void setTurboSchedActionWithoutBlock(int[] tids, long time) {
        setTurboSchedAction(tids, time);
    }

    public void setTurboSchedActionWithBoostFrequency(int[] tids, long time) {
        if (!isTurboEnabled() || !this.mPolicyList.contains(POLICY_NAME_BOOST_WITH_FREQUENCY)) {
            this.mHistoryLog.log("TS [BWF] : failed, R: not enable, tids: " + Arrays.toString(tids) + ",time: " + time);
        } else if (tids.length > 1) {
            this.mHistoryLog.log("TS [BWF] : failed, R: tids length must be 1, tids: " + Arrays.toString(tids) + ",time: " + time);
        } else {
            boolean success = setTurboSchedAction(tids, time);
            sendTraceBeginMsg(POLICY_NAME_BOOST_WITH_FREQUENCY);
            if (success) {
                boolean success2 = writeTidsToPath(TURBO_SCHED_BOOST_WITH_FREQUENCY_PATH, tids);
                if (success2) {
                    setPolicyTimeoutChecker("-BWF", tids, time, TURBO_SCHED_CANCEL_BOOST_WITH_FREQUENCY_PATH, this.mBWFTidStartTimeMap);
                    this.mHistoryLog.log("TS [BWF] : success, tids: " + Arrays.toString(tids) + ",time: " + time);
                } else {
                    this.mHistoryLog.log("TS [BWF] : failed, R: write failed 1, tids: " + Arrays.toString(tids) + ",time: " + time);
                }
            } else {
                this.mHistoryLog.log("TS [BWF] : failed, R: write failed 2, tids: " + Arrays.toString(tids) + ",time: " + time);
            }
            sendTraceEndMsg();
        }
    }

    public void setTurboSchedActionWithPriority(int[] tids, long time) {
        if (!isTurboEnabled() || !this.mPolicyList.contains(POLICY_NAME_PRIORITY)) {
            this.mHistoryLog.log("TS [PRIORITY] : failed, R: not enable, tids: " + Arrays.toString(tids) + ",time: " + time);
            return;
        }
        sendTraceBeginMsg(POLICY_NAME_PRIORITY);
        boolean success = writeTidsToPath(TURBO_SCHED_PRIORITY_PATH, tids);
        if (success) {
            setPolicyTimeoutChecker("-PRIORITY", tids, time, TURBO_SCHED_CANCEL_PRIORITY_PATH, this.mPriorityTidStartTimeMap);
            this.mHistoryLog.log("TS [PRIORITY] : success, tids: " + Arrays.toString(tids) + ",time: " + time);
        } else {
            this.mHistoryLog.log("TS [PRIORITY] : failed, R: write failed, tids: " + Arrays.toString(tids) + ",time: " + time);
        }
        sendTraceEndMsg();
    }

    public void setTurboSchedActionToLittleCore(int[] tids, long time) {
        if (!isTurboEnabled() || !this.mPolicyList.contains(POLICY_NAME_LITTLE_CORE)) {
            this.mHistoryLog.log("TS [LC] : failed, R: not enable, tids: " + Arrays.toString(tids) + ",time: " + time);
            return;
        }
        sendTraceBeginMsg(POLICY_NAME_LITTLE_CORE);
        boolean success = writeTidsToPath(TURBO_SCHED_LITTLE_CORE_PATH, tids);
        if (success) {
            setPolicyTimeoutChecker("-LC", tids, time, TURBO_SCHED_CANCEL_LITTLE_CORE_PATH, this.mPriorityTidStartTimeMap);
            this.mHistoryLog.log("TS [LC] : success, tids: " + Arrays.toString(tids) + ",time: " + time);
        } else {
            this.mHistoryLog.log("TS [LC] : failed, R: write failed, tids: " + Arrays.toString(tids) + ",time: " + time);
        }
        sendTraceEndMsg();
    }

    private boolean isVideoApp() {
        if (this.mProcessName.equals(PROCESS_AWEME) || this.mProcessName.equals(PROCESS_GIFMAKER)) {
            return true;
        }
        return false;
    }

    private boolean isFgDrawingFrame(int[] tids) {
        for (int i = 0; i < tids.length; i++) {
            if (tids[i] == this.mFgCorePid) {
                return true;
            }
        }
        return false;
    }

    public void notifyOnScroll(boolean isOnScroll) {
        this.mIsOnScroll = isOnScroll;
    }

    public void setTurboSchedActionWithId(int[] tids, long time, long id, int mode) {
        if (isTurboEnabled() && isFgDrawingFrame(tids) && this.mIsOnScroll && TurboSchedMonitor.getInstance().isCoreApp(this.mProcessName) && id != this.mLastVsyncId) {
            int bufferTx = getBufferTxCount();
            if (bufferTx >= 1) {
                if (TurboSchedMonitor.getInstance().isDebugMode()) {
                    Slog.d(TAG, "bufferTx larger than 2, dont't need boost, FluencyOptimizer");
                    return;
                }
                return;
            }
            if (TurboSchedMonitor.getInstance().isDebugMode()) {
                Slog.d(TAG, "setTurboSchedActionWithId, id:" + id + ", bufferTx:" + bufferTx + ", FluencyOptimizer");
            }
            this.mLastVsyncId = id;
            breakThermlimit(1, time);
            metisFrameBoost(this.mBoostDuration.get(0).intValue());
        }
    }

    public int checkBoostPermission() {
        int uid = Binder.getCallingUid();
        int pid = Binder.getCallingPid();
        String str = getProcessNameByPid(pid);
        int ret = -1;
        if (this.mFgCoreUid != -1) {
            if (str != null && TurboSchedMonitor.getInstance().isCoreApp(str) && uid == this.mFgCoreUid) {
                ret = 1;
            } else {
                ret = 0;
            }
        }
        if (TurboSchedMonitor.getInstance().isDebugMode()) {
            Slog.d(TAG, "checkBoostPermission, uid:" + uid + ", pid:" + pid + ", str:" + str + ", mFgCoreUid:" + this.mFgCoreUid + ", ret:" + ret);
        }
        return ret;
    }

    public boolean checkPackagePermission(String packageName) {
        if (packageName != null && TurboSchedMonitor.getInstance().isCoreApp(packageName)) {
            return true;
        }
        return false;
    }

    public boolean isCoreApp() {
        int pid = Binder.getCallingPid();
        String processName = getProcessNameByPid(pid);
        return TurboSchedMonitor.getInstance().isCoreApp(processName);
    }

    public void triggerBoostAction(int boostMs) {
        metisFrameBoost(boostMs);
    }

    public void triggerBoostTask(int tid, int boostSec) {
        if (TurboSchedMonitor.getInstance().isCoreAppOptimizerEnabled() && checkFileAccess(METIS_DEV_PATH)) {
            synchronized (mTurboLock) {
                int handle = NativeTurboSchedManager.nativeOpenDevice(METIS_DEV_PATH);
                if (handle >= 0) {
                    NativeTurboSchedManager.nativeTaskBoost(handle, tid, boostSec);
                    NativeTurboSchedManager.nativeCloseDevice(handle);
                }
            }
        }
    }

    public void breakThermlimit(int boost, long time) {
        String str;
        if (!TurboSchedMonitor.getInstance().isCoreAppOptimizerEnabled() || !this.mThermalBreakEnabled) {
            return;
        }
        sendTraceBeginMsg("breakThermlimit", String.valueOf(boost));
        if (TurboSchedMonitor.getInstance().isDebugMode()) {
            Slog.d(TAG, "breakThermlimit, begin");
        }
        if (boost == 0) {
            str = "boost:0";
        } else {
            String strThermalBreak = getThermalBreakString();
            str = "boost:1" + strThermalBreak + " time:" + time;
        }
        if (TurboSchedMonitor.getInstance().isDebugMode()) {
            Slog.d(TAG, "breakThermlimit, str:" + str);
        }
        boolean isSuccese = writeTurboSchedNode("BT-Limit", BOOST_THERMAL_PATH, str);
        sendTraceEndMsg();
        if (TurboSchedMonitor.getInstance().isDebugMode()) {
            Slog.d(TAG, "breakThermlimit, isSuccese:" + isSuccese + ", FluencyOptimizer");
        }
    }

    private int getBufferTxCount() {
        Parcel data = null;
        Parcel reply = null;
        try {
            try {
                if (this.mSurfaceFlinger == null) {
                    this.mSurfaceFlinger = ServiceManager.getService("SurfaceFlinger");
                }
            } catch (RemoteException e) {
                Slog.e(TAG, "Failed to get Buffer-Tx count");
                if (data != null) {
                    data.recycle();
                }
                if (reply == null) {
                    return -1;
                }
            }
            if (this.mSurfaceFlinger == null) {
                if (0 != 0) {
                    data.recycle();
                }
                if (0 == 0) {
                    return -1;
                }
                reply.recycle();
                return -1;
            }
            reply = Parcel.obtain();
            data = Parcel.obtain();
            data.writeInterfaceToken("android.ui.ISurfaceComposer");
            this.mSurfaceFlinger.transact(GET_BUFFER_TX_COUNT_TRANSACTION, data, reply, 0);
            int readInt = reply.readInt();
            if (data != null) {
                data.recycle();
            }
            if (reply != null) {
                reply.recycle();
            }
            return readInt;
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

    private boolean checkCallerPermmsion(int uid) {
        String str = getPackageNameFromUid(uid);
        if (uid != 0) {
            if (str != null && this.mCallerAllowList.contains(str)) {
                return true;
            }
            return false;
        }
        return true;
    }

    private String getPackageNameFromUid(int uid) {
        String packName = null;
        PackageManager packageManager = this.mPm;
        if (packageManager != null) {
            packName = packageManager.getNameForUid(uid);
        }
        if (packName == null) {
            Slog.d(TAG, "get caller pkgname failed uid = " + uid);
        }
        return packName;
    }

    public String getProcessNameByPid(int pid) {
        String processName = ProcessUtils.getProcessNameByPid(pid);
        return processName;
    }

    private boolean checkFileAccess(String path) {
        File file = new File(path);
        return file.exists();
    }

    public boolean isFileAccess() {
        File file = new File(TURBO_SCHED_PATH);
        return file.exists();
    }

    private void metisFrameBoost(int boostMs) {
        if (TurboSchedMonitor.getInstance().isCoreAppOptimizerEnabled() && checkFileAccess(METIS_DEV_PATH)) {
            synchronized (mTurboLock) {
                int handle = NativeTurboSchedManager.nativeOpenDevice(METIS_DEV_PATH);
                if (handle >= 0) {
                    if (TurboSchedMonitor.getInstance().isDebugMode()) {
                        Trace.traceBegin(64L, "metisFrameBoost");
                    }
                    NativeTurboSchedManager.nativeIoctlDevice(handle, 6);
                    NativeTurboSchedManager.nativeCloseDevice(handle);
                    if (TurboSchedMonitor.getInstance().isDebugMode()) {
                        Trace.traceEnd(64L);
                    }
                }
            }
        }
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (!DumpUtils.checkDumpPermission(this.mContext, TAG, pw)) {
            return;
        }
        boolean handled = parseDumpCommand(fd, pw, args);
        if (handled) {
            return;
        }
        pw.println("--------------------current config-----------------------");
        pw.println("TurboSchedManagerService: v2.0.18");
        boolean z = true;
        pw.println(" disable cloud control : " + (!this.mReceiveCloudData));
        pw.println(" mTurboEnabled : " + this.mTurboEnabled + ", mTurboEnabledV2: " + this.mTurboEnabledV2 + ", mCloudControlEnabled : " + this.mCloudControlEnabled);
        pw.println(" CoreAppOptimizerEnabled: " + TurboSchedMonitor.getInstance().isCoreAppOptimizerEnabled() + ", DebugMode: " + TurboSchedMonitor.getInstance().isDebugMode());
        pw.println(" mThermalBreakEnabled: " + this.mThermalBreakEnabled + ", mLinkEnable: " + this.mLinkEnable);
        pw.println(" mPolicyList : " + Arrays.toString(this.mPolicyList.toArray()));
        pw.println(" Caller allow list : " + Arrays.toString(this.mCallerAllowList.toArray()));
        pw.println("--------------------environment config-----------------------");
        pw.println(" cores sched: " + SystemProperties.get("persist.sys.miui_animator_sched.big_prime_cores", "not set"));
        pw.println(" [PRendering] enable:" + SystemProperties.get("ro.vendor.perf.scroll_opt", "not set") + ", h-a value: " + SystemProperties.get("ro.vendor.perf.scroll_opt.heavy_app", "not set"));
        pw.println("--------------------kernel config-----------------------");
        if (this.mTurboNodeExist) {
            pw.println(" isFileAccess = " + checkFileAccess(TURBO_SCHED_PATH));
        } else {
            pw.println(" isFileAccess = " + checkFileAccess(ORI_TURBO_SCHED_PATH));
        }
        pw.println(" metis dev access = " + checkFileAccess(METIS_DEV_PATH));
        boolean isBWFPolicyAccess = checkFileAccess(TURBO_SCHED_BOOST_WITH_FREQUENCY_PATH) && checkFileAccess(TURBO_SCHED_CANCEL_BOOST_WITH_FREQUENCY_PATH);
        pw.println(" bwf policy access = " + isBWFPolicyAccess);
        boolean isPriorityPolicyAccess = checkFileAccess(TURBO_SCHED_PRIORITY_PATH) && checkFileAccess(TURBO_SCHED_CANCEL_PRIORITY_PATH);
        pw.println(" priority policy access = " + isPriorityPolicyAccess);
        if (!checkFileAccess(TURBO_SCHED_LITTLE_CORE_PATH) || !checkFileAccess(TURBO_SCHED_CANCEL_LITTLE_CORE_PATH)) {
            z = false;
        }
        boolean isLittleCorePolicyAccess = z;
        pw.println(" little core policy access = " + isLittleCorePolicyAccess);
        pw.println(" thermal break enable access = " + checkFileAccess(TURBO_SCHED_THERMAL_BREAK_PATH));
        pw.println(" thermal break access = " + checkFileAccess(BOOST_THERMAL_PATH));
        pw.println(" link access =" + checkFileAccess(VIP_LINK_PATH));
    }

    private boolean parseDumpCommand(FileDescriptor fd, PrintWriter pw, String[] args) {
        boolean result;
        boolean z = false;
        boolean z2 = false;
        boolean z3 = false;
        boolean enableBoolean = false;
        if (args.length >= 3 && "setAppFrameDelay".equals(args[0])) {
            int appType = Integer.parseInt(args[1]);
            float frameDelayTH = Float.parseFloat(args[2]);
            TurboSchedMonitor.getInstance().setCoreAppFrameDealyThreshold(appType, frameDelayTH);
            printCommandResult(pw, "  AppType: " + args[1] + " frame delay TH: " + args[2]);
            return true;
        }
        int appType2 = args.length;
        if (appType2 >= 3 && "setBoostDuration".equals(args[0])) {
            int boostType = Integer.parseInt(args[1]);
            TurboSchedMonitor.getInstance().setBoostDuration(boostType, Long.parseLong(args[2]));
            printCommandResult(pw, "  BoostType: " + args[1] + " Duration: " + args[2]);
            return true;
        }
        int boostType2 = args.length;
        if (boostType2 >= 2 && "setDebugMode".equals(args[0])) {
            int debugMode = Integer.parseInt(args[1]);
            TurboSchedMonitor.getInstance().setDebugMode(debugMode);
            printCommandResult(pw, " setDebugMode: " + args[1]);
            return true;
        }
        int debugMode2 = args.length;
        if (debugMode2 >= 2 && "disableCloudControl".equals(args[0])) {
            int disableCloudControl = Integer.parseInt(args[1]);
            if (disableCloudControl <= 0) {
                z2 = true;
            }
            this.mReceiveCloudData = z2;
            SystemProperties.set("persist.sys.turbosched.receive.cloud.data", Boolean.toString(z2));
            printCommandResult(pw, " disableCloudControl: " + args[1]);
            return true;
        }
        int disableCloudControl2 = args.length;
        if (disableCloudControl2 >= 2 && "enableCoreAppOptimizer".equals(args[0])) {
            int enable = Integer.parseInt(args[1]);
            TurboSchedMonitor.getInstance().enableCoreAppOptimizer(enable);
            if (enable > 0) {
                z3 = true;
            }
            boolean enableBoolean2 = z3;
            enableFrameBoostInKernel(enableBoolean2);
            if (!this.mRegistedForegroundReceiver) {
                registeForegroundReceiver();
            }
            if (enableBoolean2) {
                if (this.m8550Devices.contains(miui.os.Build.DEVICE)) {
                    SystemProperties.set("persist.sys.miui_animator_sched.big_prime_cores", "3-7");
                } else {
                    SystemProperties.set("persist.sys.miui_animator_sched.big_prime_cores", "4-7");
                }
            } else if (this.m8550Devices.contains(miui.os.Build.DEVICE)) {
                SystemProperties.set("persist.sys.miui_animator_sched.big_prime_cores", "3-6");
            } else {
                SystemProperties.set("persist.sys.miui_animator_sched.big_prime_cores", "4-6");
            }
            printCommandResult(pw, " enableCoreAppOptimizer: " + args[1]);
            return true;
        }
        int enable2 = args.length;
        if (enable2 == 2 && "setVipTask".equals(args[0])) {
            String str = args[1];
            if (this.mTurboNodeExist) {
                result = writeTurboSchedNode("dump-setVipTask", TURBO_SCHED_PATH, str);
            } else {
                result = writeTurboSchedNode("dump-setVipTask", ORI_TURBO_SCHED_PATH, str);
            }
            printCommandResult(pw, "setVipTask result: " + result);
            return true;
        } else if (args.length >= 4 && "policy".equals(args[0])) {
            String policy = args[1];
            long time = Long.parseLong(args[2]);
            int[] tids = new int[args.length - 3];
            for (int i = 3; i < args.length; i++) {
                tids[i - 3] = Integer.parseInt(args[i]);
            }
            if (policy.equals(POLICY_NAME_BOOST_WITH_FREQUENCY)) {
                setTurboSchedActionWithBoostFrequency(tids, time);
            } else if (policy.equals(POLICY_NAME_PRIORITY)) {
                setTurboSchedActionWithPriority(tids, time);
            } else if (policy.equals("vip")) {
                setTurboSchedAction(tids, time);
            } else if (policy.equals(POLICY_NAME_LITTLE_CORE)) {
                setTurboSchedActionToLittleCore(tids, time);
            }
            return true;
        } else if (args.length == 2 && "setPolicy".equals(args[0])) {
            String str2 = args[1];
            String[] policies = str2.split(",");
            this.mPolicyList.clear();
            for (String policy2 : policies) {
                this.mPolicyList.add(policy2);
            }
            printCommandResult(pw, "mPolicyList: " + this.mPolicyList);
            return true;
        } else if (args.length == 2 && "setCallerAllowList".equals(args[0])) {
            String str3 = args[1];
            String[] policies2 = str3.split(",");
            this.mCallerAllowList.clear();
            for (String policy3 : policies2) {
                this.mCallerAllowList.add(policy3);
            }
            printCommandResult(pw, "mCallerAllowList: " + this.mCallerAllowList);
            return true;
        } else if (args.length >= 3 && "thermalBreak".equals(args[0])) {
            int boost = Integer.parseInt(args[1]);
            breakThermlimit(boost, Integer.parseInt(args[2]));
            return true;
        } else {
            int boost2 = args.length;
            if (boost2 == 2 && "setThermalBreakEnable".equals(args[0])) {
                int enable3 = Integer.parseInt(args[1]);
                if (enable3 > 0) {
                    enableBoolean = true;
                }
                setThermalBreakEnable(enableBoolean);
                printCommandResult(pw, "setThermalBreakEnable: " + enable3);
                return true;
            }
            int enable4 = args.length;
            if (enable4 == 2 && "setThermalBreakThreshold".equals(args[0])) {
                if (!this.mThermalBreakEnabled) {
                    printCommandResult(pw, "thermal break is not enabled");
                    return true;
                }
                String thresholdStr = args[1];
                if (!COMMAND_DRY_RUN.equals(thresholdStr)) {
                    setThermalBreakThresholdMap(thresholdStr);
                }
                printCommandResult(pw, TEMPREATURE_THROSHOLD_BOOST_STRING_MAP.toString());
                return true;
            } else if (args.length == 1 && "CoreApp".equals(args[0])) {
                pw.println(" appFrameDelay: ");
                int index = 0;
                for (Float f : TurboSchedMonitor.getInstance().getCoreAppFrameDealyThreshold()) {
                    float ths = f.floatValue();
                    if (index == 0) {
                        pw.println("  0 AWEME: " + ths);
                    } else if (index == 1) {
                        pw.println("  1 GIFMAKER: " + ths);
                    } else if (index == 2) {
                        pw.println("  2 WEIBO: " + ths);
                    } else if (index == 3) {
                        pw.println("  3 ARTICLE_NEWS: " + ths);
                    } else if (index == 4) {
                        pw.println("  4 TAOBAO: " + ths);
                    } else if (index == 5) {
                        pw.println("  5 WECHAT: " + ths);
                    }
                    index++;
                }
                pw.println(" Duration: ");
                List<Long> boostDuration = TurboSchedMonitor.getInstance().getBoostDuration();
                pw.println("  0 Boost Duration: " + boostDuration.get(0));
                pw.println("  1 Thermal Break Duration: " + boostDuration.get(1));
                pw.println("Boost allow list : ");
                for (String name : TurboSchedMonitor.getInstance().getCoreAppList()) {
                    pw.println("CoreAppName : " + name);
                }
                return true;
            } else {
                int index2 = args.length;
                if (index2 >= 1 && "history".equals(args[0])) {
                    if (args.length > 1 && ("-c".equals(args[1]) || "--clear".equals(args[1]))) {
                        this.mHistoryLog = new LocalLog(MAX_HISTORY_ITEMS);
                        pw.println("History log cleared");
                        return true;
                    }
                    this.mHistoryLog.dump(fd, pw, args);
                    return true;
                } else if (args.length != 2 || !"enableLink".equals(args[0])) {
                    return false;
                } else {
                    int enable5 = Integer.parseInt(args[1]);
                    if (enable5 > 0) {
                        z = true;
                    }
                    this.mLinkEnable = z;
                    printCommandResult(pw, "enableLink: " + enable5);
                    return true;
                }
            }
        }
    }

    private void printCommandResult(PrintWriter pw, String result) {
        pw.println("--------------------command result-----------------------");
        pw.println(result);
    }

    private void recordRtPid(int id) {
        if (!TurboSchedMonitor.getInstance().isCoreAppOptimizerEnabled()) {
            return;
        }
        Slog.i(TAG, "recordrtpid,pid " + id + ", MITEST");
        synchronized (mTurboLock) {
            StringBuilder sb = new StringBuilder();
            sb.append(id);
            String str = sb.toString();
            boolean isSuccese = writeTurboSchedNode("RR-PID", RECORD_RT_PATH, str);
            if (TurboSchedMonitor.getInstance().isDebugMode()) {
                Slog.i(TAG, "recordrtpid, isSuccese:" + isSuccese + "pid " + id + ", MITEST");
            }
        }
    }

    private void enableFrameBoostInKernel(boolean enable) {
        boolean isSuccese = enable ? writeTurboSchedNode("TB-Enabled", BOOST_ENABLE_PATH, SplitScreenReporter.ACTION_ENTER_SPLIT) : writeTurboSchedNode("TB-Enabled", BOOST_ENABLE_PATH, "0");
        if (TurboSchedMonitor.getInstance().isDebugMode()) {
            Slog.i(TAG, "enableFrameBoostInKernel, isSuccese:" + isSuccese);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class TurboSchedBroadcastReceiver extends BroadcastReceiver {
        public TurboSchedBroadcastReceiver() {
            TurboSchedManagerService.this = r3;
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.intent.action.SCREEN_ON");
            filter.addAction("android.intent.action.SCREEN_OFF");
            r3.mContext.registerReceiver(this, filter);
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("android.intent.action.SCREEN_ON")) {
                TurboSchedManagerService.this.mScreenOn = true;
            } else if (action.equals("android.intent.action.SCREEN_OFF")) {
                TurboSchedManagerService.this.mScreenOn = false;
            }
        }
    }

    private void enableTurboSched(boolean enable) {
        this.mTurboEnabled = enable;
        SystemProperties.set("persist.sys.turbosched.enable", Boolean.toString(enable));
    }

    public boolean writeTidsToPath(String path, int[] tids) {
        StringBuilder sb = new StringBuilder();
        for (int i : tids) {
            sb.append(i);
            sb.append(":");
        }
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return writeTurboSchedNode("M-Tids", path, sb.toString());
    }

    private void setPolicyTimeoutChecker(final String logTag, final int[] tids, final long time, final String cancelPath, final Map<Integer, Long> startTimeMap) {
        Timer timer = new Timer();
        for (int tid : tids) {
            startTimeMap.put(Integer.valueOf(tid), Long.valueOf(System.currentTimeMillis()));
        }
        timer.schedule(new TimerTask() { // from class: com.miui.server.turbosched.TurboSchedManagerService.9
            @Override // java.util.TimerTask, java.lang.Runnable
            public void run() {
                int[] iArr;
                for (int tid2 : tids) {
                    if (((Long) startTimeMap.get(Integer.valueOf(tid2))).longValue() + time <= System.currentTimeMillis()) {
                        boolean ret = TurboSchedManagerService.this.writeTidsToPath(cancelPath, tids);
                        TurboSchedManagerService.this.mHistoryLog.log("TS [" + logTag + "] : success: " + ret + ", tids: " + Arrays.toString(tids));
                    }
                }
            }
        }, time);
    }

    private String getThermalBreakString() {
        int temperatue = getBoardSensorTemperature();
        for (Map.Entry<Integer, String> entry : TEMPREATURE_THROSHOLD_BOOST_STRING_MAP.entrySet()) {
            if (temperatue >= entry.getKey().intValue()) {
                return entry.getValue();
            }
        }
        return NO_THERMAL_BREAK_STRING;
    }

    private int getBoardSensorTemperature() {
        StringBuilder sb;
        int sensorTemp = 0;
        FileInputStream fis = null;
        try {
            try {
                fis = new FileInputStream(BOARD_TEMP_FILE);
                byte[] buffer = new byte[10];
                int read = fis.read(buffer);
                if (read > 0) {
                    String str = new String(buffer, 0, read);
                    sensorTemp = Integer.parseInt(str.trim()) / 1000;
                }
            } catch (Exception e) {
                Slog.e(TAG, "getBoardSensorTemperature failed : " + e.getMessage());
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e2) {
                        e = e2;
                        sb = new StringBuilder();
                        Slog.e(TAG, sb.append("getBoardSensorTemperature failed : ").append(e.getMessage()).toString());
                        return sensorTemp;
                    }
                }
            }
            try {
                fis.close();
            } catch (IOException e3) {
                e = e3;
                sb = new StringBuilder();
                Slog.e(TAG, sb.append("getBoardSensorTemperature failed : ").append(e.getMessage()).toString());
                return sensorTemp;
            }
            return sensorTemp;
        } catch (Throwable th) {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e4) {
                    Slog.e(TAG, "getBoardSensorTemperature failed : " + e4.getMessage());
                }
            }
            throw th;
        }
    }

    /* loaded from: classes.dex */
    public static class TraceInfo {
        String desc;
        String name;
        long startTime;

        TraceInfo() {
        }
    }
}
