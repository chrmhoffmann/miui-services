package com.miui.server.sptm;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings;
import android.server.am.SplitScreenReporter;
import android.text.TextUtils;
import android.util.Slog;
import com.android.server.am.PreloadAppControllerImpl;
import com.android.server.am.SpeedTestModeServiceStub;
import com.android.server.inputmethod.InputMethodManagerServiceImpl;
import com.android.server.wm.ActivityTaskSupervisorImpl;
import com.miui.base.MiuiStubRegistry;
import com.miui.base.MiuiStubUtil;
import com.miui.server.SecurityManagerService;
import com.miui.server.sptm.PreLoadStrategy;
import com.miui.server.sptm.SpeedTestModeController;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
/* loaded from: classes.dex */
public class SpeedTestModeServiceImpl implements SpeedTestModeServiceStub {
    public static final int ENABLE_SPTM_MIN_MEMORY = 6000;
    public static final int EVENT_TYPE_LOCK_SCREEN = 5;
    public static final int EVENT_TYPE_ONE_KEY_CLEAN = 4;
    public static final int EVENT_TYPE_PAUSE = 3;
    public static final int EVENT_TYPE_PRELOAD_STARTED = 6;
    public static final int EVENT_TYPE_RESUME = 2;
    public static final int EVENT_TYPE_START_PROC = 1;
    public static final List<String> GAME_APPS;
    private static final List<String> PERMISSION_DIALOG_PACKAGE_NAMES;
    public static final List<String> PRELOAD_APPS;
    public static final List<String> PRELOAD_GAME_APPS;
    public static final List<String> SPEED_TEST_APP_LIST;
    private static final String SPTM_ANIMATION_CLOUD_ENABLE = "perf_sptm_animation_enable";
    private static final String SPTM_APP_LIST = "perf_sptm_app_list";
    private static final String SPTM_CLOUD_ENABLE = "perf_sptm_enable";
    private static final String SPTM_PRELOAD_CLOUD = "perf_sptm_preload";
    public static final String TAG = "SPTM";
    private Handler mH;
    private HandlerThread mHandlerThread;
    private HomeAnimationStrategy mHomeAnimationStrategy;
    private PreLoadStrategy mPreLoadStrategy;
    private SpeedTestModeState mSpeedTestModeState;
    public static final long START_PROC_DELAYED_TIME = SystemProperties.getLong("persist.sys.miui_sptm.start_proc_delayed", 1500);
    public static final long FAST_SWITCH_HOME_DURATION = SystemProperties.getInt("persist.sys.miui_sptm.fast_home", 5000);
    public static final long HOT_START_DELAYED_TIME = SystemProperties.getLong("persist.sys.miui_sptm.hot_start_delayed", 15000);
    public static final long COLD_START_DELAYED_TIME = SystemProperties.getLong("persist.sys.miui_sptm.cold_start_delayed", 15000);
    public static final long GAME_HOT_START_DELAYED_TIME = SystemProperties.getLong("persist.sys.miui_sptm.game_hot_start_delayed", 30000);
    public static final long GAME_COLD_START_DELAYED_TIME = SystemProperties.getLong("persist.sys.miui_sptm.game_cold_start_delayed", (long) SecurityManagerService.LOCK_TIME_OUT);
    public static final int PRELOAD_THRESHOLD = SystemProperties.getInt("persist.sys.miui_sptm.pl_threshold", 5);
    public static final int SPTM_LOW_MEMORY_DEVICE_THRESHOLD = SystemProperties.getInt("persist.sys.miui_stpm.low_mem_device", 12) * 1000;
    public static final int SPTM_LOW_MEMORY_DEVICE_PRELOAD_CORE = SystemProperties.getInt("persist.sys.miui_stpm.low_mem_device_pl_cores", 5);
    public static final long TOTAL_MEMORY = Process.getTotalMemory() >> 20;
    private static final boolean IGNORE_CLOUD_ENABLE = SystemProperties.getBoolean("persist.sys.miui_sptm.ignore_cloud_enable", false);
    private boolean mIsEnable = SystemProperties.getBoolean("persist.sys.miui_sptm.enable", false);
    private int mPreloadType = SystemProperties.getInt("persist.sys.miui_sptm.enable_pl_type", 0);
    private boolean mIsEnableSPTMAnimation = SystemProperties.getBoolean("persist.sys.miui_sptm.enable", true);
    private String mHomePackageName = InputMethodManagerServiceImpl.MIUI_HOME;
    private boolean mIsSpeedTestEnabled = true;
    private boolean mIsSpeedTestMode = false;
    public LinkedList<Strategy> mSpeedTestModeStrategies = new LinkedList<>();
    private LinkedList<AppUsageRecord> mAppStartRecords = new LinkedList<>();
    private HashMap<String, Long> mAppLastResumedTimes = new HashMap<>();
    private LinkedHashMap<String, Long> mAppStartProcTimes = new LinkedHashMap<String, Long>() { // from class: com.miui.server.sptm.SpeedTestModeServiceImpl.1
        @Override // java.util.LinkedHashMap
        protected boolean removeEldestEntry(Map.Entry<String, Long> eldest) {
            return size() > 100;
        }
    };
    private Context mContext = null;

    /* loaded from: classes.dex */
    public interface Strategy {
        void onAppStarted(PreLoadStrategy.AppStartRecord appStartRecord);

        void onNewEvent(int i);

        void onSpeedTestModeChanged(boolean z);
    }

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<SpeedTestModeServiceImpl> {

        /* compiled from: SpeedTestModeServiceImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final SpeedTestModeServiceImpl INSTANCE = new SpeedTestModeServiceImpl();
        }

        public SpeedTestModeServiceImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public SpeedTestModeServiceImpl provideNewInstance() {
            return new SpeedTestModeServiceImpl();
        }
    }

    static {
        LinkedList linkedList = new LinkedList();
        SPEED_TEST_APP_LIST = linkedList;
        linkedList.add("com.tencent.mm");
        linkedList.add("com.tencent.mobileqq");
        linkedList.add("com.sina.weibo");
        linkedList.add("com.eg.android.AlipayGphone");
        linkedList.add("com.taobao.taobao");
        linkedList.add("com.jingdong.app.mall");
        linkedList.add("com.ss.android.lark");
        linkedList.add("com.xingin.xhs");
        linkedList.add("com.tencent.qqmusic");
        linkedList.add("com.MobileTicket");
        linkedList.add("com.qiyi.video");
        linkedList.add("com.netease.cloudmusic");
        linkedList.add("com.zhihu.android");
        linkedList.add("com.autonavi.minimap");
        linkedList.add("com.ss.android.ugc.aweme");
        linkedList.add("tv.danmaku.bili");
        linkedList.add("com.hicorenational.antifraud");
        linkedList.add("com.miHoYo.ys.mi");
        LinkedList linkedList2 = new LinkedList();
        PRELOAD_GAME_APPS = linkedList2;
        linkedList2.add("com.tencent.tmgp.sgame");
        linkedList2.add("com.tencent.tmgp.pubgmhd");
        LinkedList linkedList3 = new LinkedList();
        GAME_APPS = linkedList3;
        linkedList3.addAll(linkedList2);
        linkedList3.add("com.miHoYo.ys.mi");
        LinkedList linkedList4 = new LinkedList();
        PRELOAD_APPS = linkedList4;
        linkedList4.add("com.netease.cloudmusic");
        linkedList4.addAll(linkedList2);
        LinkedList linkedList5 = new LinkedList();
        PERMISSION_DIALOG_PACKAGE_NAMES = linkedList5;
        linkedList5.add(ActivityTaskSupervisorImpl.MIUI_APP_LOCK_PACKAGE_NAME);
        linkedList5.add("com.lbe.security.miui");
    }

    /* loaded from: classes.dex */
    public class Handler extends android.os.Handler {
        public static final int MSG_DISABLE_SPTM = 3;
        public static final int MSG_ENABLE_SPTM = 2;
        public static final int MSG_EVENT = 1;
        public static final int MSG_EXIT_MODE = 4;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public Handler(Looper looper) {
            super(looper);
            SpeedTestModeServiceImpl.this = r1;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (!SpeedTestModeServiceImpl.this.mIsSpeedTestEnabled) {
                return;
            }
            if (msg.what == 1) {
                Bundle bundle = msg.getData();
                if (bundle != null) {
                    int event = bundle.getInt("event");
                    String packageName = bundle.getString("packageName");
                    long time = bundle.getLong(SplitScreenReporter.STR_DEAL_TIME);
                    SpeedTestModeServiceImpl.this.handleEvent(event, packageName, time);
                }
            } else if (msg.what == 2) {
                SpeedTestModeServiceImpl.this.handleUpdateSpeedTestMode(true);
            } else if (msg.what == 3) {
                SpeedTestModeServiceImpl.this.handleUpdateSpeedTestMode(false);
            } else if (msg.what == 4) {
                SpeedTestModeServiceImpl.this.mSpeedTestModeState.addAppSwitchOps(1);
            }
        }
    }

    public static SpeedTestModeServiceImpl getInstance() {
        return (SpeedTestModeServiceImpl) MiuiStubUtil.getImpl(SpeedTestModeServiceStub.class);
    }

    public void init(Context context) {
        if (!this.mIsEnable || TOTAL_MEMORY <= 6000) {
            return;
        }
        this.mContext = context;
        SpeedTestModeController speedTestModeController = new SpeedTestModeController(new SpeedTestModeController.OnSpeedTestModeChangeListener() { // from class: com.miui.server.sptm.SpeedTestModeServiceImpl$$ExternalSyntheticLambda0
            @Override // com.miui.server.sptm.SpeedTestModeController.OnSpeedTestModeChangeListener
            public final void onSpeedTestModeChange(boolean z) {
                SpeedTestModeServiceImpl.this.setEnableSpeedTestMode(z);
            }
        });
        this.mSpeedTestModeState = new SpeedTestModeState(speedTestModeController);
        HandlerThread handlerThread = new HandlerThread("SPTModeServiceTh", -2);
        this.mHandlerThread = handlerThread;
        handlerThread.start();
        this.mH = new Handler(this.mHandlerThread.getLooper());
        registerCloudObserver(this.mContext);
        updateCloudControlParas();
        if (this.mSpeedTestModeStrategies.size() == 0) {
            HomeAnimationStrategy homeAnimationStrategy = new HomeAnimationStrategy(context);
            this.mHomeAnimationStrategy = homeAnimationStrategy;
            this.mSpeedTestModeStrategies.add(homeAnimationStrategy);
            this.mSpeedTestModeStrategies.add(new MemoryOptimizeStrategy());
            this.mSpeedTestModeStrategies.add(new GreezeStrategy());
            PreLoadStrategy preLoadStrategy = new PreLoadStrategy();
            this.mPreLoadStrategy = preLoadStrategy;
            this.mSpeedTestModeStrategies.add(preLoadStrategy);
        }
    }

    public void updateHomeProcess(String homePackageName) {
        if (!TextUtils.isEmpty(homePackageName)) {
            this.mHomePackageName = homePackageName;
        }
    }

    public float getWindowStateAnimationScaleOverride() {
        HomeAnimationStrategy homeAnimationStrategy = this.mHomeAnimationStrategy;
        if (homeAnimationStrategy != null) {
            return homeAnimationStrategy.getWindowAnimatorDurationOverride();
        }
        return 1.0f;
    }

    public void setSPTModeEnabled(boolean isEnable) {
        this.mIsSpeedTestEnabled = isEnable;
    }

    public void reportAppUsageEvents(int usageEventCode, String packageName) {
        int event;
        if (this.mH == null || TextUtils.isEmpty(packageName) || PERMISSION_DIALOG_PACKAGE_NAMES.contains(packageName)) {
            return;
        }
        switch (usageEventCode) {
            case 1:
                event = 2;
                break;
            case 2:
                event = 3;
                break;
            case 17:
                event = 5;
                break;
            default:
                return;
        }
        PreloadAppControllerImpl preloadController = PreloadAppControllerImpl.getInstance();
        if (preloadController == null || !preloadController.getPreloadingApps().contains(packageName)) {
            Message msg = this.mH.obtainMessage(1);
            msg.setData(createMsgData(event, packageName, SystemClock.uptimeMillis()));
            this.mH.sendMessage(msg);
        } else if (DEBUG) {
            Slog.e(TAG, String.format("skip %s code: %s because preloading", packageName, Integer.valueOf(event)));
        }
    }

    public void reportOneKeyCleanEvent() {
        Handler handler = this.mH;
        if (handler == null) {
            return;
        }
        Message msg = handler.obtainMessage(1);
        msg.setData(createMsgData(4, "", SystemClock.uptimeMillis()));
        this.mH.sendMessage(msg);
    }

    public void reportStartProcEvent(String packageName, String procName) {
        if (this.mH != null && !TextUtils.isEmpty(packageName) && !TextUtils.isEmpty(procName) && packageName.equals(procName)) {
            Message msg = this.mH.obtainMessage(1);
            msg.setData(createMsgData(1, packageName, SystemClock.uptimeMillis()));
            this.mH.sendMessage(msg);
        }
    }

    public void reportPreloadAppStart(String packageName) {
        Handler handler = this.mH;
        if (handler == null) {
            return;
        }
        Message msg = handler.obtainMessage(1);
        msg.setData(createMsgData(6, packageName, SystemClock.uptimeMillis()));
        this.mH.sendMessage(msg);
    }

    public static boolean isLowMemDeviceForSpeedTestMode() {
        return TOTAL_MEMORY <= ((long) SPTM_LOW_MEMORY_DEVICE_THRESHOLD);
    }

    public boolean isSpeedTestMode() {
        return this.mIsSpeedTestMode;
    }

    private void registerCloudObserver(Context context) {
        ContentObserver observer = new ContentObserver(this.mH) { // from class: com.miui.server.sptm.SpeedTestModeServiceImpl.2
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                if (uri.equals(Settings.System.getUriFor("perf_shielder_SPTM"))) {
                    SpeedTestModeServiceImpl.this.updateCloudControlParas();
                }
            }
        };
        context.getContentResolver().registerContentObserver(Settings.System.getUriFor("perf_shielder_SPTM"), false, observer, -2);
    }

    public void updateCloudControlParas() {
        if (!IGNORE_CLOUD_ENABLE) {
            String sptmEnable = Settings.System.getStringForUser(this.mContext.getContentResolver(), SPTM_CLOUD_ENABLE, -2);
            if (sptmEnable != null) {
                this.mIsEnable = Boolean.parseBoolean(sptmEnable);
                Slog.d(TAG, "SPTM enable cloud control received : " + this.mIsEnable);
            }
            String preloadType = Settings.System.getStringForUser(this.mContext.getContentResolver(), SPTM_PRELOAD_CLOUD, -2);
            if (preloadType != null) {
                this.mPreloadType = Integer.parseInt(preloadType);
                Slog.d(TAG, "SPTM preload cloud control received : " + this.mPreloadType);
            }
            String animationEnable = Settings.System.getStringForUser(this.mContext.getContentResolver(), SPTM_ANIMATION_CLOUD_ENABLE, -2);
            if (animationEnable != null) {
                this.mIsEnableSPTMAnimation = Boolean.parseBoolean(animationEnable);
                Slog.d(TAG, "SPTM animation cloud control received : " + this.mIsEnableSPTMAnimation);
            }
        }
        String appString = Settings.System.getStringForUser(this.mContext.getContentResolver(), SPTM_APP_LIST, -2);
        if (!TextUtils.isEmpty(appString)) {
            String[] appArray = appString.split(",");
            for (String appPackageName : appArray) {
                if (!TextUtils.isEmpty(appPackageName)) {
                    if (DEBUG) {
                        Slog.d(TAG, "SPTM add app package name : " + appPackageName);
                    }
                    List<String> list = SPEED_TEST_APP_LIST;
                    if (!list.contains(appPackageName)) {
                        list.add(appPackageName);
                    }
                }
            }
        }
    }

    private void reportAppSwitchEventWaitTimeout(long timeout) {
        Handler handler = this.mH;
        if (handler == null) {
            return;
        }
        Message msg = handler.obtainMessage(4);
        this.mH.removeMessages(4);
        this.mH.sendMessageDelayed(msg, timeout);
    }

    public void handleEvent(int event, String packageName, long time) {
        PreLoadStrategy preLoadStrategy;
        if (event == 2) {
            this.mAppLastResumedTimes.put(packageName, Long.valueOf(time));
            reportAppSwitchEventWaitTimeout(GAME_APPS.contains(packageName) ? GAME_COLD_START_DELAYED_TIME : COLD_START_DELAYED_TIME);
        } else if (event == 1) {
            this.mAppStartProcTimes.put(packageName, Long.valueOf(time));
        } else if (event == 3) {
            Long resumedTime = this.mAppLastResumedTimes.remove(packageName);
            if (resumedTime != null) {
                AppUsageRecord r = new AppUsageRecord();
                r.startTime = resumedTime.longValue();
                r.endTime = time;
                r.packageName = packageName;
                Long startProcTime = this.mAppStartProcTimes.get(r.packageName);
                if (startProcTime != null) {
                    if (r.startTime - startProcTime.longValue() < START_PROC_DELAYED_TIME) {
                        r.isColdStart = true;
                    }
                    this.mAppStartProcTimes.remove(r.packageName);
                }
                handleAppSwitchingEvent(r);
            } else if (DEBUG) {
                Slog.e(TAG, String.format("pkg %s has not resumed %s", packageName, Long.valueOf(time)));
            }
        } else if (event == 5 || event == 4) {
            this.mSpeedTestModeState.addAppSwitchOps(1);
        } else if (event == 6 && (preLoadStrategy = this.mPreLoadStrategy) != null) {
            preLoadStrategy.onPreloadAppStarted(packageName);
        }
        Iterator<Strategy> it = this.mSpeedTestModeStrategies.iterator();
        while (it.hasNext()) {
            Strategy st = it.next();
            st.onNewEvent(event);
        }
    }

    private void handleAppSwitchingEvent(AppUsageRecord appRecord) {
        LinkedList<Strategy> linkedList;
        if (this.mHomePackageName.equals(appRecord.packageName)) {
            PreLoadStrategy.AppStartRecord outRes = new PreLoadStrategy.AppStartRecord();
            int switchOps = getAppSwitchingOperation(this.mAppStartRecords, appRecord, outRes);
            this.mSpeedTestModeState.addAppSwitchOps(switchOps);
            LinkedList<AppUsageRecord> linkedList2 = this.mAppStartRecords;
            if (linkedList2 != null && linkedList2.size() > 0) {
                this.mSpeedTestModeState.addAppSwitchOps(this.mAppStartRecords.get(0).packageName);
            }
            if (DEBUG) {
                Slog.d(TAG, "handleAppSwitchingEvent: ops" + switchOps);
                Iterator<AppUsageRecord> it = this.mAppStartRecords.iterator();
                while (it.hasNext()) {
                    AppUsageRecord r = it.next();
                    Slog.e(TAG, "handleAppSwitchingEvent Apps: " + r.toString());
                }
                Slog.e(TAG, "handleAppSwitchingEvent Home: " + appRecord.toString());
            }
            this.mAppStartRecords.clear();
            if (outRes.packageName != null && (linkedList = this.mSpeedTestModeStrategies) != null) {
                Iterator<Strategy> it2 = linkedList.iterator();
                while (it2.hasNext()) {
                    Strategy s = it2.next();
                    s.onAppStarted(outRes);
                }
            }
        } else if (this.mAppStartRecords.size() == 0 || this.mAppStartRecords.get(0).packageName == appRecord.packageName) {
            this.mAppStartRecords.add(appRecord);
        }
        if (this.mAppStartRecords.size() > 30) {
            this.mAppStartRecords.clear();
        }
    }

    private int getAppSwitchingOperation(List<AppUsageRecord> appUsageRecord, AppUsageRecord homeRecord, PreLoadStrategy.AppStartRecord outRes) {
        if (homeRecord == null || homeRecord.getDuration() >= FAST_SWITCH_HOME_DURATION) {
            return 1;
        }
        if (appUsageRecord == null || appUsageRecord.size() == 0) {
            return 2;
        }
        if (!SPEED_TEST_APP_LIST.contains(appUsageRecord.get(0).packageName) && !GAME_APPS.contains(appUsageRecord.get(0).packageName)) {
            return 1;
        }
        String coldStartPackageName = null;
        long totalDuration = 0;
        for (AppUsageRecord aur : appUsageRecord) {
            totalDuration += aur.getDuration();
            if (coldStartPackageName == null && aur.isColdStart) {
                coldStartPackageName = aur.packageName;
            }
            outRes.packageName = aur.packageName;
            outRes.isColdStart = false;
        }
        if (coldStartPackageName == null) {
            if (!GAME_APPS.contains(outRes.packageName)) {
                if (totalDuration > HOT_START_DELAYED_TIME) {
                    return 1;
                }
                return 2;
            } else if (totalDuration > GAME_HOT_START_DELAYED_TIME) {
                return 1;
            } else {
                return 2;
            }
        }
        outRes.packageName = coldStartPackageName;
        outRes.isColdStart = true;
        long totalDuration2 = 0;
        long lastResumedActivityDuration = 0;
        for (AppUsageRecord ur : appUsageRecord) {
            if (coldStartPackageName.equals(ur.packageName)) {
                totalDuration2 += ur.getDuration();
                lastResumedActivityDuration = !ur.isColdStart ? ur.getDuration() : 0L;
            }
        }
        if (!GAME_APPS.contains(coldStartPackageName)) {
            if (totalDuration2 <= COLD_START_DELAYED_TIME && lastResumedActivityDuration <= HOT_START_DELAYED_TIME) {
                return 3;
            }
        } else if (totalDuration2 <= GAME_COLD_START_DELAYED_TIME) {
            return 3;
        }
        return 1;
    }

    public void handleUpdateSpeedTestMode(boolean isEnabled) {
        this.mIsSpeedTestMode = isEnabled;
        LinkedList<Strategy> linkedList = this.mSpeedTestModeStrategies;
        if (linkedList != null) {
            Iterator<Strategy> it = linkedList.iterator();
            while (it.hasNext()) {
                Strategy s = it.next();
                s.onSpeedTestModeChanged(isEnabled);
            }
        }
    }

    public void setEnableSpeedTestMode(boolean isEnable) {
        this.mH.sendEmptyMessage(isEnable ? 2 : 3);
    }

    private static Bundle createMsgData(int event, String packageName, long time) {
        Bundle b = new Bundle();
        b.putInt("event", event);
        b.putString("packageName", packageName);
        b.putLong(SplitScreenReporter.STR_DEAL_TIME, time);
        return b;
    }

    /* loaded from: classes.dex */
    public static class AppUsageRecord {
        long endTime;
        boolean isColdStart;
        String packageName;
        long startTime;

        private AppUsageRecord() {
        }

        public long getDuration() {
            return this.endTime - this.startTime;
        }

        public String toString() {
            return "AppUsageRecord{packageName='" + this.packageName + "', startTime=" + this.startTime + ", endTime=" + this.endTime + ", usedTime=" + getDuration() + ", isColdStart=" + this.isColdStart + '}';
        }
    }

    public static int getAmsMaxCachedProcesses() {
        if (TOTAL_MEMORY <= 6000) {
            return -1;
        }
        return 60;
    }

    public int getPreloadCloudType() {
        return this.mPreloadType;
    }

    public boolean getSPTMCloudEnable() {
        return this.mIsEnable;
    }

    public boolean getAnimationCloudEnable() {
        return this.mIsEnableSPTMAnimation;
    }
}
