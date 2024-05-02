package com.android.server.app;

import android.app.ActivityManager;
import android.compat.Compatibility;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.Point;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Slog;
import com.android.internal.compat.CompatibilityChangeConfig;
import com.android.server.ServiceThread;
import com.android.server.am.ActivityManagerService;
import com.android.server.compat.PlatformCompat;
import com.android.server.display.TemperatureController;
import com.android.server.location.gnss.map.AmapExtraCommand;
import com.android.server.wm.MiuiFreeformPinManagerService;
import com.android.server.wm.MiuiFreeformTrackManager;
import com.android.server.wm.WindowManagerService;
import com.android.server.wm.WindowManagerServiceStub;
import com.miui.base.MiuiStubRegistry;
import com.miui.server.input.edgesuppression.EdgeSuppressionFactory;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import miui.os.Build;
import miui.util.FeatureParser;
import org.json.JSONArray;
import org.json.JSONObject;
/* loaded from: classes.dex */
public class GameManagerServiceStubImpl extends GameManagerServiceStub {
    private static final String CLOUD_ALL_DATA_CHANGE_URI = "content://com.android.settings.cloud.CloudSettings/cloud_all_data/notify";
    private static final String DEBUG_PROP_KEY = "persist.sys.miui_downscale";
    private static final String DOWNSCALE_APP_SETTINGS_FILE_PATH = "system/etc/DownscaleAppSettings.json";
    private static final String DOWNSCALE_DISABLE = "disable";
    private static final String DOWNSCALE_ENABLE = "enable";
    private static final String KEY_POWER_MODE_OPEN = "POWER_SAVE_MODE_OPEN";
    public static final String MIUI_RESOLUTION = "persist.sys.miui_resolution";
    private static final String MIUI_SCREEN_COMPAT = "miui_screen_compat";
    public static final int MIUI_SCREEN_COMPAT_VERSION = 1;
    private static final int MIUI_SCREEN_COMPAT_WIDTH = 1080;
    private static final String MODULE_NAME_DOWNSCALE = "tdownscale";
    public static final int MSG_CLOUD_DATA_CHANGE = 111114;
    public static final int MSG_DOWNSCALE_APP_DIED = 111111;
    public static final int MSG_INTELLIGENT_POWERSAVE = 111115;
    public static final int MSG_PROCESS_POWERSAVE_ACTION = 111112;
    public static final int MSG_REBOOT_COMPLETED_ACTION = 111113;
    public static final float SCALE_MIN = 0.6f;
    private static final String TAG = "GameManagerServiceStub";
    private Context mContext;
    private DownscaleCloudData mDownscaleCloudData;
    private InnerHandler mHandler;
    private GMSObserver mObserver;
    private volatile boolean mPowerSaving;
    private ActivityManagerService mService;
    WindowManagerService mWindowManager;
    public final ServiceThread mHandlerThread = new ServiceThread(TAG, -2, false);
    private HashMap<String, Boolean> mDownscaleApps = new HashMap<>();
    private String mProEnable = "enable";
    private boolean intelligentPowerSavingEnable = false;
    private int currentWidth = MIUI_SCREEN_COMPAT_WIDTH;
    private HashSet<String> mShellCmdDownscalePackageNames = new HashSet<>();

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<GameManagerServiceStubImpl> {

        /* compiled from: GameManagerServiceStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final GameManagerServiceStubImpl INSTANCE = new GameManagerServiceStubImpl();
        }

        public GameManagerServiceStubImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public GameManagerServiceStubImpl provideNewInstance() {
            return new GameManagerServiceStubImpl();
        }
    }

    void registerDownscaleObserver(Context mContext) {
        this.mContext = mContext;
        if (this.mHandler == null) {
            if (!this.mHandlerThread.isAlive()) {
                this.mHandlerThread.start();
            }
            this.mHandler = new InnerHandler(this.mHandlerThread.getLooper());
        }
        this.mObserver = new GMSObserver(this.mHandler);
        this.mWindowManager = ServiceManager.getService("window");
        this.mPowerSaving = Settings.System.getInt(mContext.getContentResolver(), KEY_POWER_MODE_OPEN, 0) == 1;
        int defValue = !Build.IS_TABLET ? 1 : 0;
        this.intelligentPowerSavingEnable = Settings.System.getInt(mContext.getContentResolver(), MIUI_SCREEN_COMPAT, defValue) == 1;
        String proEnable = Settings.Global.getString(mContext.getContentResolver(), DEBUG_PROP_KEY);
        if (!TextUtils.isEmpty(proEnable) && TextUtils.equals(DOWNSCALE_DISABLE, proEnable)) {
            this.mProEnable = proEnable;
        }
        mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor(DEBUG_PROP_KEY), false, this.mObserver);
        mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(KEY_POWER_MODE_OPEN), true, this.mObserver);
        mContext.registerReceiverForAllUsers(new GMSBroadcastReceiver(), new IntentFilter("android.intent.action.BOOT_COMPLETED"), null, null);
        mContext.getContentResolver().registerContentObserver(Uri.parse(CLOUD_ALL_DATA_CHANGE_URI), false, this.mObserver);
        mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(MIUI_SCREEN_COMPAT), false, this.mObserver);
        if (this.mDownscaleCloudData == null) {
            JSONObject jsonAll = initLocalDownscaleAppList();
            this.mDownscaleCloudData = new DownscaleCloudData(jsonAll);
        }
    }

    public void processCloudData() {
        MiuiSettings.SettingsCloudData.CloudData cloudData = MiuiSettings.SettingsCloudData.getCloudDataSingle(this.mContext.getContentResolver(), MODULE_NAME_DOWNSCALE, MODULE_NAME_DOWNSCALE, (String) null, false);
        if (cloudData == null || cloudData.json() == null) {
            return;
        }
        JSONObject jsonAll = cloudData.json();
        long version = -1;
        if (jsonAll != null) {
            version = jsonAll.optLong(AmapExtraCommand.VERSION_KEY);
        }
        DownscaleCloudData downscaleCloudData = this.mDownscaleCloudData;
        if (downscaleCloudData == null) {
            DownscaleCloudData downscaleCloudData2 = new DownscaleCloudData(jsonAll);
            this.mDownscaleCloudData = downscaleCloudData2;
            toggleDownscale(null);
        } else if (downscaleCloudData.version != version && version != -1) {
            DownscaleCloudData downscaleCloudData3 = new DownscaleCloudData(jsonAll);
            this.mDownscaleCloudData = downscaleCloudData3;
            toggleDownscale(null);
        }
    }

    public void processBootCompletedAction() {
        boolean z = false;
        if (Settings.System.getInt(this.mContext.getContentResolver(), KEY_POWER_MODE_OPEN, 0) == 1) {
            z = true;
        }
        boolean powerModeOpen = z;
        Slog.v(TAG, "ACTION_BOOT_COMPLETED --- powerModeOpen= " + powerModeOpen + " mPowerSaving = " + this.mPowerSaving);
        if (powerModeOpen) {
            this.mPowerSaving = true;
            toggleDownscale(null);
        }
    }

    public void processPowerSaveAction() {
        boolean z = false;
        if (Settings.System.getInt(this.mContext.getContentResolver(), KEY_POWER_MODE_OPEN, 0) == 1) {
            z = true;
        }
        this.mPowerSaving = z;
        if (DOWNSCALE_DISABLE.equals(this.mProEnable)) {
            return;
        }
        toggleDownscale(null);
    }

    private boolean checkValidApp(String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return true;
        }
        DownscaleCloudData downscaleCloudData = this.mDownscaleCloudData;
        if (downscaleCloudData != null && downscaleCloudData.apps != null) {
            AppItem app = this.mDownscaleCloudData.apps.get(packageName);
            return app != null;
        }
        return false;
    }

    public void toggleDownscale(String packageNameParam) {
        if (!checkValidApp(packageNameParam)) {
            return;
        }
        executeDownscale(packageNameParam, true);
    }

    public void downscaleForSwitchResolution() {
        toggleDownscale(null);
    }

    public void downscaleWithPackageNameAndRatio(String packageName, String ratio) {
    }

    public void dynamicRefresh(String ratio, String packageName) {
    }

    public void shellCmd(String ratio, String packageName) {
        Set<Long> disabled;
        final long changeId = getCompatChangeId(ratio);
        Set<Long> enabled = new ArraySet<>();
        if (changeId == 0) {
            disabled = this.DOWNSCALE_CHANGE_IDS;
        } else {
            enabled.add(168419799L);
            enabled.add(Long.valueOf(changeId));
            disabled = (Set) this.DOWNSCALE_CHANGE_IDS.stream().filter(new Predicate() { // from class: com.android.server.app.GameManagerServiceStubImpl$$ExternalSyntheticLambda0
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return GameManagerServiceStubImpl.lambda$shellCmd$0(changeId, (Long) obj);
                }
            }).collect(Collectors.toSet());
        }
        if (TextUtils.equals(ratio, DOWNSCALE_DISABLE)) {
            if (this.mShellCmdDownscalePackageNames.contains(packageName)) {
                this.mShellCmdDownscalePackageNames.remove(packageName);
            }
        } else {
            this.mShellCmdDownscalePackageNames.add(packageName);
        }
        PlatformCompat platformCompat = ServiceManager.getService("platform_compat");
        CompatibilityChangeConfig overrides = new CompatibilityChangeConfig(new Compatibility.ChangeConfig(enabled, disabled));
        platformCompat.setOverrides(overrides, packageName);
        Slog.v(TAG, "shellCmd packageName = " + packageName + " , ratio = " + ratio);
    }

    public static /* synthetic */ boolean lambda$shellCmd$0(long changeId, Long it) {
        return (it.longValue() == 168419799 || it.longValue() == changeId) ? false : true;
    }

    private void executeDownscale(String packageNameParam, boolean ignoreRunningApps) {
        DownscaleCloudData downscaleCloudData = this.mDownscaleCloudData;
        if (downscaleCloudData != null) {
            appsDownscale(packageNameParam, ignoreRunningApps);
            return;
        }
        if (downscaleCloudData == null) {
            JSONObject jsonAll = initLocalDownscaleAppList();
            this.mDownscaleCloudData = new DownscaleCloudData(jsonAll);
        }
        appsDownscale(packageNameParam, ignoreRunningApps);
    }

    private void appsDownscale(String packageNameParam, boolean ignoreRunningApps) {
        AppItem app;
        HashMap<String, AppItem> apps = this.mDownscaleCloudData.apps;
        String currentResolution = SystemProperties.get(MIUI_RESOLUTION);
        if (!TextUtils.isEmpty(currentResolution)) {
            String[] resolutionArr = currentResolution.split(",");
            if (resolutionArr != null && resolutionArr.length > 0) {
                try {
                    this.currentWidth = Integer.parseInt(resolutionArr[0]);
                } catch (Exception e) {
                }
            }
        } else {
            Point point = new Point();
            this.mWindowManager.getBaseDisplaySize(0, point);
            int baseDisplayWidth = point.x;
            Slog.v(TAG, "baseDisplayWidth = " + baseDisplayWidth);
            if (baseDisplayWidth > 0) {
                this.currentWidth = baseDisplayWidth;
            }
        }
        if (TextUtils.isEmpty(packageNameParam)) {
            if (apps != null) {
                for (Map.Entry<String, AppItem> appItemEntry : apps.entrySet()) {
                    downscaleApp(appItemEntry.getKey(), appItemEntry.getValue(), ignoreRunningApps);
                }
            }
        } else if (apps != null && (app = apps.get(packageNameParam)) != null) {
            downscaleApp(app.packageName, app, ignoreRunningApps);
        }
    }

    private int getTargetWidth(AppItem appItem) {
        int mode;
        if (!"enable".equals(this.mProEnable) || !this.mDownscaleCloudData.enable || (mode = appItem.mode) == 0) {
            return -1;
        }
        int appVersion = appItem.appVersion;
        boolean saveBatteryEnable = true;
        if (appVersion > 1) {
            return -1;
        }
        boolean normalEnable = (mode & 1) != 0;
        if ((mode & 2) == 0) {
            saveBatteryEnable = false;
        }
        boolean containDevice = GmsUtil.isContainDevice(this.mDownscaleCloudData.devices);
        if (this.mPowerSaving && saveBatteryEnable && containDevice) {
            return this.mDownscaleCloudData.scenes.saveBattery;
        }
        int[] resolutionArray = FeatureParser.getIntArray("screen_resolution_supported");
        boolean screenCompatSupported = FeatureParser.getBoolean("screen_compat_supported", false);
        if (!Build.IS_TABLET && this.intelligentPowerSavingEnable && normalEnable && this.currentWidth > MIUI_SCREEN_COMPAT_WIDTH && (resolutionArray != null || screenCompatSupported)) {
            return this.mDownscaleCloudData.scenes.normal;
        }
        if (Build.IS_TABLET && this.intelligentPowerSavingEnable && normalEnable) {
            return this.mDownscaleCloudData.scenes.normal;
        }
        return -1;
    }

    private void downscaleApp(String packageName, AppItem appItem, boolean ignoreRunningApps) {
        Set<Long> disabled;
        if (!TextUtils.isEmpty(packageName) && appItem != null) {
            if ("enable".equals(this.mProEnable) && ignoreRunningApps && runningApp(packageName)) {
                Slog.v(TAG, "packageName = " + packageName + "is running do not downscale");
                return;
            }
            String ratioResult = DOWNSCALE_DISABLE;
            if (!Build.IS_TABLET) {
                int targetWidth = getTargetWidth(appItem);
                if (targetWidth != -1) {
                    ratioResult = GmsUtil.calcuRatio(targetWidth, this.currentWidth);
                }
            } else if ("enable".equals(this.mProEnable) && this.mDownscaleCloudData.enable) {
                ratioResult = GmsUtil.getTargetRatioForPad(appItem, this.mPowerSaving, this.mDownscaleCloudData);
            }
            float currentScale = WindowManagerServiceStub.get().getCompatScale(packageName, 0);
            Slog.v(TAG, "downscale currentScale = " + currentScale);
            if (currentScale != MiuiFreeformPinManagerService.EDGE_AREA) {
                float currentScale2 = 1.0f / currentScale;
                if (ratioResult.equals(String.valueOf(currentScale2)) || (TextUtils.equals(ratioResult, DOWNSCALE_DISABLE) && currentScale2 == 1.0f)) {
                    Slog.v(TAG, "downscale " + packageName + " not change !!");
                    return;
                }
            }
            final long changeId = getCompatChangeId(ratioResult);
            Set<Long> enabled = new ArraySet<>();
            if (changeId == 0) {
                disabled = this.DOWNSCALE_CHANGE_IDS;
            } else {
                enabled.add(168419799L);
                enabled.add(Long.valueOf(changeId));
                disabled = (Set) this.DOWNSCALE_CHANGE_IDS.stream().filter(new Predicate() { // from class: com.android.server.app.GameManagerServiceStubImpl$$ExternalSyntheticLambda1
                    @Override // java.util.function.Predicate
                    public final boolean test(Object obj) {
                        return GameManagerServiceStubImpl.lambda$downscaleApp$1(changeId, (Long) obj);
                    }
                }).collect(Collectors.toSet());
            }
            PlatformCompat platformCompat = ServiceManager.getService("platform_compat");
            CompatibilityChangeConfig overrides = new CompatibilityChangeConfig(new Compatibility.ChangeConfig(enabled, disabled));
            platformCompat.setOverrides(overrides, packageName);
            Slog.v(TAG, "downscale enable packageName = " + packageName + " ratio = " + ratioResult);
        }
    }

    public static /* synthetic */ boolean lambda$downscaleApp$1(long changeId, Long it) {
        return (it.longValue() == 168419799 || it.longValue() == changeId) ? false : true;
    }

    private JSONObject initLocalDownscaleAppList() {
        String jsonString = "";
        try {
            FileInputStream f = new FileInputStream(DOWNSCALE_APP_SETTINGS_FILE_PATH);
            try {
                BufferedReader bis = new BufferedReader(new InputStreamReader(f));
                while (true) {
                    try {
                        String line = bis.readLine();
                        if (line == null) {
                            break;
                        }
                        jsonString = jsonString + line;
                    } catch (Throwable th) {
                        try {
                            bis.close();
                        } catch (Throwable th2) {
                            th.addSuppressed(th2);
                        }
                        throw th;
                    }
                }
                bis.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            f.close();
        } catch (Exception e2) {
            Slog.v(TAG, "system/etc/DownscaleAppSettings.json not find");
        }
        try {
            JSONObject appConfigJSON = new JSONObject(jsonString);
            return appConfigJSON;
        } catch (Exception e3) {
            return null;
        }
    }

    public void handleAppDied(String packageName) {
        if (this.mHandler != null) {
            if (TextUtils.equals(packageName, "com.tencent.mm")) {
                Slog.v(TAG, "com.tencent.mm handleAppDied");
                return;
            }
            Message msg = this.mHandler.obtainMessage(MSG_DOWNSCALE_APP_DIED);
            msg.obj = packageName;
            this.mHandler.sendMessage(msg);
        }
    }

    public void setActivityManagerService(ActivityManagerService service) {
        this.mService = service;
        if (this.mHandler == null) {
            if (!this.mHandlerThread.isAlive()) {
                this.mHandlerThread.start();
            }
            this.mHandler = new InnerHandler(this.mHandlerThread.getLooper());
        }
    }

    private boolean runningApp(String packageName) {
        ActivityManagerService activityManagerService;
        if (TextUtils.isEmpty(packageName) || (activityManagerService = this.mService) == null) {
            return false;
        }
        List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = activityManagerService.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo runningAppProcess : runningAppProcesses) {
            if (packageName.equals(runningAppProcess.processName)) {
                return true;
            }
        }
        return false;
    }

    /* loaded from: classes.dex */
    final class InnerHandler extends Handler {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public InnerHandler(Looper looper) {
            super(looper);
            GameManagerServiceStubImpl.this = this$0;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case GameManagerServiceStubImpl.MSG_DOWNSCALE_APP_DIED /* 111111 */:
                    String processName = (String) msg.obj;
                    Slog.v(GameManagerServiceStubImpl.TAG, "app died  processName = " + processName);
                    if (!GameManagerServiceStubImpl.this.mShellCmdDownscalePackageNames.contains(processName)) {
                        GameManagerServiceStubImpl.this.toggleDownscale(processName);
                        return;
                    }
                    return;
                case GameManagerServiceStubImpl.MSG_PROCESS_POWERSAVE_ACTION /* 111112 */:
                    GameManagerServiceStubImpl.this.processPowerSaveAction();
                    return;
                case GameManagerServiceStubImpl.MSG_REBOOT_COMPLETED_ACTION /* 111113 */:
                    GameManagerServiceStubImpl.this.processBootCompletedAction();
                    return;
                case GameManagerServiceStubImpl.MSG_CLOUD_DATA_CHANGE /* 111114 */:
                    GameManagerServiceStubImpl.this.processCloudData();
                    return;
                case GameManagerServiceStubImpl.MSG_INTELLIGENT_POWERSAVE /* 111115 */:
                    GameManagerServiceStubImpl.this.processIntelligentPowerSave();
                    return;
                default:
                    Slog.d(GameManagerServiceStubImpl.TAG, "default case");
                    return;
            }
        }
    }

    public void processIntelligentPowerSave() {
        if (DOWNSCALE_DISABLE.equals(this.mProEnable)) {
            return;
        }
        executeDownscale(null, false);
    }

    /* loaded from: classes.dex */
    public class DownscaleCloudData {
        boolean enable;
        Scenes scenes;
        long version;
        HashSet<String> devices = new HashSet<>();
        HashMap<String, AppItem> apps = new HashMap<>();

        public DownscaleCloudData(JSONObject cloudData) {
            GameManagerServiceStubImpl.this = this$0;
            if (cloudData != null) {
                this.enable = cloudData.optBoolean("enable", false);
                this.version = cloudData.optLong(AmapExtraCommand.VERSION_KEY, -1L);
                JSONArray appsJson = null;
                JSONArray devicesJson = null;
                JSONObject scenesJson = null;
                if (!Build.IS_TABLET) {
                    appsJson = cloudData.optJSONArray("apps");
                    devicesJson = cloudData.optJSONArray("devices");
                    scenesJson = cloudData.optJSONObject("scenes");
                } else {
                    JSONObject padJson = cloudData.optJSONObject(MiuiFreeformTrackManager.CommonTrackConstants.DEVICE_TYPE_PAD);
                    if (padJson == null) {
                        return;
                    }
                    this.enable = padJson.optBoolean("enable", false);
                    if (padJson != null) {
                        appsJson = padJson.optJSONArray("apps");
                        devicesJson = padJson.optJSONArray("devices");
                        scenesJson = padJson.optJSONObject("scenes");
                    }
                }
                parseApps(appsJson);
                parseDevices(devicesJson);
                parseScenes(scenesJson);
            }
        }

        private void parseApps(JSONArray appsJson) {
            if (appsJson != null && appsJson.length() > 0) {
                for (int i = 0; i < appsJson.length(); i++) {
                    AppItem appItem = new AppItem(appsJson.optJSONObject(i));
                    this.apps.put(appItem.packageName, appItem);
                }
            }
        }

        private void parseDevices(JSONArray devicesJson) {
            if (devicesJson != null && devicesJson.length() > 0) {
                for (int i = 0; i < devicesJson.length(); i++) {
                    DeviceItem deviceItem = new DeviceItem(devicesJson.optJSONObject(i));
                    this.devices.add(deviceItem.name);
                }
            }
        }

        private void parseScenes(JSONObject scenesJson) {
            if (scenesJson != null) {
                this.scenes = new Scenes(scenesJson);
            }
        }
    }

    /* loaded from: classes.dex */
    public static class AppItem {
        int appVersion;
        int mode;
        String packageName;

        public AppItem(JSONObject cloudDataApps) {
            if (cloudDataApps != null) {
                this.packageName = cloudDataApps.optString("packageName");
                this.appVersion = cloudDataApps.optInt(AmapExtraCommand.VERSION_KEY, 0);
                this.mode = cloudDataApps.optInt("mode", 0);
            }
        }
    }

    /* loaded from: classes.dex */
    public static class DeviceItem {
        String name;

        public DeviceItem(JSONObject cloudDataDevice) {
            if (cloudDataDevice != null) {
                this.name = cloudDataDevice.optString(TemperatureController.STRATEGY_NAME);
            }
        }
    }

    /* loaded from: classes.dex */
    public static class Scenes {
        int normal;
        String padSaveBattery;
        int saveBattery;

        public Scenes(JSONObject jsonObject) {
            if (jsonObject != null) {
                this.normal = jsonObject.optInt(EdgeSuppressionFactory.TYPE_NORMAL, GameManagerServiceStubImpl.MIUI_SCREEN_COMPAT_WIDTH);
                this.saveBattery = jsonObject.optInt("save_battery", 720);
                this.padSaveBattery = jsonObject.optString("save_battery", "0.85");
            }
        }
    }

    /* loaded from: classes.dex */
    class GMSObserver extends ContentObserver {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public GMSObserver(Handler handler) {
            super(handler);
            GameManagerServiceStubImpl.this = this$0;
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            if (uri == null) {
                return;
            }
            if (uri.equals(Settings.Global.getUriFor(GameManagerServiceStubImpl.DEBUG_PROP_KEY))) {
                Slog.d(GameManagerServiceStubImpl.TAG, "DEBUG_PROP_KEY");
                String proEnable = Settings.Global.getString(GameManagerServiceStubImpl.this.mContext.getContentResolver(), GameManagerServiceStubImpl.DEBUG_PROP_KEY);
                if (TextUtils.equals(proEnable, GameManagerServiceStubImpl.this.mProEnable)) {
                    return;
                }
                GameManagerServiceStubImpl.this.mProEnable = proEnable;
                if (!GameManagerServiceStubImpl.this.mPowerSaving && !GameManagerServiceStubImpl.this.intelligentPowerSavingEnable) {
                    return;
                }
                GameManagerServiceStubImpl.this.toggleDownscale(null);
            } else if (uri.equals(Settings.System.getUriFor(GameManagerServiceStubImpl.KEY_POWER_MODE_OPEN))) {
                Slog.d(GameManagerServiceStubImpl.TAG, "KEY_POWER_MODE_OPEN");
                if (GameManagerServiceStubImpl.this.mHandler != null) {
                    GameManagerServiceStubImpl.this.mHandler.sendEmptyMessage(GameManagerServiceStubImpl.MSG_PROCESS_POWERSAVE_ACTION);
                }
            } else if (uri.equals(Uri.parse(GameManagerServiceStubImpl.CLOUD_ALL_DATA_CHANGE_URI))) {
                Slog.d(GameManagerServiceStubImpl.TAG, "CLOUD_ALL_DATA_CHANGE_URI");
                GameManagerServiceStubImpl.this.mHandler.sendEmptyMessage(GameManagerServiceStubImpl.MSG_CLOUD_DATA_CHANGE);
            } else if (uri.equals(Settings.System.getUriFor(GameManagerServiceStubImpl.MIUI_SCREEN_COMPAT))) {
                Slog.d(GameManagerServiceStubImpl.TAG, "MIUI_SCREEN_COMPAT");
                GameManagerServiceStubImpl gameManagerServiceStubImpl = GameManagerServiceStubImpl.this;
                boolean z = true;
                if (Settings.System.getInt(gameManagerServiceStubImpl.mContext.getContentResolver(), GameManagerServiceStubImpl.MIUI_SCREEN_COMPAT, 1) != 1) {
                    z = false;
                }
                gameManagerServiceStubImpl.intelligentPowerSavingEnable = z;
                GameManagerServiceStubImpl.this.mHandler.sendEmptyMessage(GameManagerServiceStubImpl.MSG_INTELLIGENT_POWERSAVE);
            }
        }
    }

    /* loaded from: classes.dex */
    class GMSBroadcastReceiver extends BroadcastReceiver {
        GMSBroadcastReceiver() {
            GameManagerServiceStubImpl.this = this$0;
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            Slog.d(GameManagerServiceStubImpl.TAG, "ACTION_BOOT_COMPLETED");
            if (intent != null && intent.getAction() == "android.intent.action.BOOT_COMPLETED" && GameManagerServiceStubImpl.this.mHandler != null) {
                GameManagerServiceStubImpl.this.mHandler.sendEmptyMessage(GameManagerServiceStubImpl.MSG_REBOOT_COMPLETED_ACTION);
            }
        }
    }
}
