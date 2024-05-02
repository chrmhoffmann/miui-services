package com.android.server.location;

import android.content.Context;
import android.database.ContentObserver;
import android.location.GnssStatus;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.util.Log;
import android.util.SparseArray;
import com.android.server.am.ActivityManagerServiceImpl;
import com.android.server.location.gnss.GnssEventTrackingStub;
import com.android.server.wm.MiuiFreeformPinManagerService;
import com.miui.base.MiuiStubRegistry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
/* loaded from: classes.dex */
public class GnssSmartSatelliteSwitchImpl implements GnssSmartSatelliteSwitchStub {
    private static final String BLOCK_GLO_QZSS_NAV_GAL = "3,0,4,0,6,0,7,0";
    private static final String BLOCK_NONE = "";
    private static final String CLOUD_KEY_SWITCH_Enable = "smartSatelliteSwitch";
    private static final String CLOUD_MODULE_GNSS_SMART_SATELLITE_SWITCH_CONFIG = "mtkGnssConfig";
    private static final long EFFETCTIVE_NAVI_INTERVAL = 300000;
    private static final String GNSS_CONFIG_SUPPORT_BLOCK_LIST_PROP = "persist.sys.gps.support_block_list_prop";
    private static final int NAVI_STATUS_BLOCK_SATELLITE = 3;
    private static final int NAVI_STATUS_NORMAL = 2;
    private static final int NAVI_STATUS_START = 1;
    private static final int NAVI_STATUS_STOP = 0;
    private static final int SATTELLITE_POWERSAVE_LEVEL_0 = 0;
    private static final int SATTELLITE_POWERSAVE_LEVEL_1 = 1;
    private static final String TAG = "GnssSmartSatelliteSwitchImpl";
    private static final long UPDATE_INTERVAL = 300000;
    private static String[] controlledAppList = {"com.baidu.BaiduMap", "com.autonavi.minimap", "com.tencent.map"};
    private static final boolean isEnabled;
    private Context mContext;
    private Handler mHandler;
    private HandlerThread mHandlerThread;
    private Timer mTimer;
    private TimerTask mTimerTask;
    private volatile ConcurrentHashMap<Integer, Float> GPS_COLLECTOR = new ConcurrentHashMap<>();
    private volatile ConcurrentHashMap<Integer, Float> BDS_COLLECTOR = new ConcurrentHashMap<>();
    private AtomicBoolean isInit = new AtomicBoolean(false);
    private boolean startControllerListener = false;
    private boolean isInControlledList = false;
    private SparseArray<Long> naviTime = new SparseArray<>();
    private long effectiveTime = 0;
    private long totalNaviTime = 0;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<GnssSmartSatelliteSwitchImpl> {

        /* compiled from: GnssSmartSatelliteSwitchImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final GnssSmartSatelliteSwitchImpl INSTANCE = new GnssSmartSatelliteSwitchImpl();
        }

        public GnssSmartSatelliteSwitchImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public GnssSmartSatelliteSwitchImpl provideNewInstance() {
            return new GnssSmartSatelliteSwitchImpl();
        }
    }

    static {
        boolean z = false;
        if (isCnVersion() && SystemProperties.getBoolean(GNSS_CONFIG_SUPPORT_BLOCK_LIST_PROP, false)) {
            z = true;
        }
        isEnabled = z;
    }

    public void addCloudControllListener(Context context) {
        if (this.startControllerListener || context == null) {
            return;
        }
        this.mContext = context;
        context.getContentResolver().registerContentObserver(MiuiSettings.SettingsCloudData.getCloudDataNotifyUri(), true, new ContentObserver(null) { // from class: com.android.server.location.GnssSmartSatelliteSwitchImpl.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                boolean settingsNow = SystemProperties.getBoolean(GnssSmartSatelliteSwitchImpl.GNSS_CONFIG_SUPPORT_BLOCK_LIST_PROP, false);
                String newSettings = MiuiSettings.SettingsCloudData.getCloudDataString(GnssSmartSatelliteSwitchImpl.this.mContext.getContentResolver(), GnssSmartSatelliteSwitchImpl.CLOUD_MODULE_GNSS_SMART_SATELLITE_SWITCH_CONFIG, GnssSmartSatelliteSwitchImpl.CLOUD_KEY_SWITCH_Enable, (String) null);
                if (newSettings == null) {
                    return;
                }
                Log.i(GnssSmartSatelliteSwitchImpl.TAG, "receiver new config, new value: " + newSettings + ", settingsNow: " + settingsNow);
                boolean newSettingsBool = Boolean.parseBoolean(newSettings);
                if (newSettingsBool != settingsNow) {
                    SystemProperties.set(GnssSmartSatelliteSwitchImpl.GNSS_CONFIG_SUPPORT_BLOCK_LIST_PROP, String.valueOf(newSettingsBool));
                }
            }
        });
        Log.i(TAG, "register cloud controller listener");
        this.startControllerListener = true;
    }

    public boolean supportConstellationBlockListFeature() {
        return isEnabled;
    }

    private static boolean isCnVersion() {
        return "CN".equalsIgnoreCase(SystemProperties.get("ro.miui.build.region"));
    }

    public void smartSatelliteSwitchMonitor(GnssStatus gnssStatus) {
        if (this.mContext == null || !this.isInControlledList) {
            return;
        }
        handleGnssStatus(gnssStatus);
    }

    public void isControlled(String workSource) {
        int i = 0;
        while (true) {
            String[] strArr = controlledAppList;
            if (i >= strArr.length) {
                break;
            }
            if (workSource.contains(strArr[i])) {
                this.isInControlledList = true;
            }
            i++;
        }
        if (this.isInControlledList) {
            initOnce();
        }
    }

    public boolean getAvgOfTopFourCN0(Map<Integer, Float> collector) {
        int size = collector.size();
        if (size == 0 || size < 4) {
            return false;
        }
        float sum = MiuiFreeformPinManagerService.EDGE_AREA;
        List<Float> vals = new ArrayList<>();
        for (Integer key : collector.keySet()) {
            vals.add(collector.get(key));
        }
        Collections.sort(vals, new Comparator<Float>() { // from class: com.android.server.location.GnssSmartSatelliteSwitchImpl.2
            public int compare(Float o1, Float o2) {
                return o2.compareTo(o1);
            }
        });
        for (int i = 0; i < 4; i++) {
            sum += vals.get(i).floatValue();
        }
        Log.d(TAG, "getAvgOfTopFourCN0: " + (sum / 4.0f));
        return sum / 4.0f > 40.0f;
    }

    public synchronized void updateBlockList(String mBlockList) {
        String blockListNow;
        String[] blockLists = mBlockList.split(",");
        if (blockLists.length % 2 != 0) {
            mBlockList = "";
        }
        try {
            blockListNow = Settings.Global.getString(this.mContext.getContentResolver(), "gnss_satellite_blocklist");
            if (blockListNow == null) {
                blockListNow = "";
            }
        } catch (Exception e) {
            Log.d(TAG, "update blocklist fail, cause :" + e.getCause());
        }
        if (blockListNow.equals(mBlockList)) {
            return;
        }
        Settings.Global.putString(this.mContext.getContentResolver(), "gnss_satellite_blocklist", mBlockList);
        if (BLOCK_GLO_QZSS_NAV_GAL.equals(mBlockList)) {
            this.naviTime.put(3, Long.valueOf(SystemClock.elapsedRealtime()));
        } else {
            this.naviTime.put(2, Long.valueOf(SystemClock.elapsedRealtime()));
        }
        Log.d(TAG, "update blockList:" + mBlockList);
        updateEffectiveTime(this.naviTime);
    }

    private void handleGnssStatus(GnssStatus status) {
        if (!this.isInit.get()) {
            return;
        }
        for (int i = 0; i < status.getSatelliteCount(); i++) {
            int constellationType = status.getConstellationType(i);
            switch (constellationType) {
                case 1:
                    this.GPS_COLLECTOR.put(Integer.valueOf(status.getSvid(i)), Float.valueOf(status.getCn0DbHz(i)));
                    break;
                case 5:
                    this.BDS_COLLECTOR.put(Integer.valueOf(status.getSvid(i)), Float.valueOf(status.getCn0DbHz(i)));
                    break;
            }
        }
    }

    private void initOnce() {
        if (this.isInit.get()) {
            return;
        }
        initCollector();
        this.naviTime.put(1, Long.valueOf(SystemClock.elapsedRealtime()));
        HandlerThread handlerThread = new HandlerThread("mGnssSmartSatelliteHandler");
        this.mHandlerThread = handlerThread;
        handlerThread.start();
        this.mHandler = new BlockListHandler(this.mHandlerThread.getLooper());
        this.mTimer = new Timer();
        TimerTask timerTask = new TimerTask() { // from class: com.android.server.location.GnssSmartSatelliteSwitchImpl.3
            @Override // java.util.TimerTask, java.lang.Runnable
            public void run() {
                Message message = GnssSmartSatelliteSwitchImpl.this.mHandler.obtainMessage();
                message.what = 0;
                GnssSmartSatelliteSwitchImpl gnssSmartSatelliteSwitchImpl = GnssSmartSatelliteSwitchImpl.this;
                if (gnssSmartSatelliteSwitchImpl.getAvgOfTopFourCN0(gnssSmartSatelliteSwitchImpl.GPS_COLLECTOR)) {
                    GnssSmartSatelliteSwitchImpl gnssSmartSatelliteSwitchImpl2 = GnssSmartSatelliteSwitchImpl.this;
                    if (gnssSmartSatelliteSwitchImpl2.getAvgOfTopFourCN0(gnssSmartSatelliteSwitchImpl2.BDS_COLLECTOR)) {
                        message.what = 1;
                        GnssSmartSatelliteSwitchImpl.this.mHandler.sendMessage(message);
                        return;
                    }
                }
                message.what = 0;
                GnssSmartSatelliteSwitchImpl.this.mHandler.sendMessage(message);
            }
        };
        this.mTimerTask = timerTask;
        this.mTimer.schedule(timerTask, ActivityManagerServiceImpl.KEEP_FOREGROUND_DURATION, 300000L);
        Log.d(TAG, "init");
        this.isInit.set(true);
    }

    private void initCollector() {
        long currentTimemills = SystemClock.elapsedRealtime();
        if (this.naviTime == null) {
            this.naviTime = new SparseArray<>();
        }
        this.naviTime.put(0, Long.valueOf(currentTimemills));
        this.naviTime.put(1, Long.valueOf(currentTimemills));
        this.naviTime.put(2, Long.valueOf(currentTimemills));
        this.naviTime.put(3, Long.valueOf(currentTimemills));
        this.effectiveTime = 0L;
    }

    public synchronized void resetBlockList() {
        if (this.isInit.get()) {
            try {
                this.naviTime.put(0, Long.valueOf(SystemClock.elapsedRealtime()));
                sendToGnssEnent();
                TimerTask timerTask = this.mTimerTask;
                if (timerTask != null) {
                    timerTask.cancel();
                    this.mTimerTask = null;
                }
                Timer timer = this.mTimer;
                if (timer != null) {
                    timer.cancel();
                    this.mTimer.purge();
                    this.mTimer = null;
                }
                HandlerThread handlerThread = this.mHandlerThread;
                if (handlerThread != null) {
                    handlerThread.getLooper().quitSafely();
                }
                Settings.Global.putString(this.mContext.getContentResolver(), "gnss_satellite_blocklist", "");
                Log.d(TAG, "reset Blocklist");
                this.isInControlledList = false;
                this.isInit.set(false);
            } catch (Exception e) {
                Log.d(TAG, Log.getStackTraceString(e));
            }
        }
    }

    private boolean isEffective(long var1, long var2) {
        if (var1 - var2 > 300000) {
            return true;
        }
        return false;
    }

    private synchronized void updateEffectiveTime(SparseArray<Long> naviTimeCollector) {
        long beginTime = naviTimeCollector.valueAt(1).longValue();
        long stopTime = naviTimeCollector.valueAt(0).longValue();
        long blockTime = naviTimeCollector.valueAt(3).longValue();
        long normalTime = naviTimeCollector.valueAt(2).longValue();
        this.totalNaviTime = isEffective(stopTime, beginTime) ? stopTime - beginTime : 0L;
        if (normalTime >= blockTime) {
            this.effectiveTime += normalTime - blockTime;
        } else if (blockTime - normalTime < ActivityManagerServiceImpl.KEEP_FOREGROUND_DURATION) {
        } else {
            this.effectiveTime += stopTime - blockTime;
        }
    }

    private long getEffectiveTime() {
        return this.effectiveTime;
    }

    private void sendToGnssEnent() {
        try {
            updateEffectiveTime(this.naviTime);
            if (this.totalNaviTime > 0) {
                GnssEventTrackingStub.getInstance().recordSatelliteBlockListChanged(this.totalNaviTime, getEffectiveTime(), (String) null);
            }
            this.naviTime.clear();
            this.effectiveTime = 0L;
        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
        }
    }

    /* loaded from: classes.dex */
    public class BlockListHandler extends Handler {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public BlockListHandler(Looper looper) {
            super(looper);
            GnssSmartSatelliteSwitchImpl.this = r1;
        }

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public BlockListHandler(Looper looper, Handler.Callback callback) {
            super(looper, callback);
            GnssSmartSatelliteSwitchImpl.this = r1;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg == null) {
                return;
            }
            switch (msg.what) {
                case 0:
                    GnssSmartSatelliteSwitchImpl.this.updateBlockList("");
                    break;
                case 1:
                    GnssSmartSatelliteSwitchImpl.this.updateBlockList(GnssSmartSatelliteSwitchImpl.BLOCK_GLO_QZSS_NAV_GAL);
                    break;
            }
            GnssSmartSatelliteSwitchImpl.this.BDS_COLLECTOR.clear();
            GnssSmartSatelliteSwitchImpl.this.GPS_COLLECTOR.clear();
        }
    }
}
