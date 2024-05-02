package com.miui.server.sptm;

import android.os.RemoteException;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Slog;
import com.android.server.LocalServices;
import com.miui.server.process.ProcessManagerInternal;
import com.miui.server.sptm.SpeedTestModeServiceImpl;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import miui.process.LifecycleConfig;
import miui.process.ProcessManagerNative;
/* loaded from: classes.dex */
public class PreLoadStrategy implements SpeedTestModeServiceImpl.Strategy {
    private static final String PUBG_PACKAGE_NAME = "com.tencent.tmgp.pubgmhd";
    private static final String SGAME_PACKAGE_NAME = "com.tencent.tmgp.sgame";
    private static final String STOP_TIME_PACKAGES_PREFIX = "persist.sys.miui_sptm.";
    private boolean mIsLowMemoryDevice;
    private ProcessManagerInternal mPMS;
    private SpeedTestModeServiceImpl mSpeedTestModeService = SpeedTestModeServiceImpl.getInstance();
    private final String TAG = SpeedTestModeServiceImpl.TAG;
    private final boolean DEBUG = SpeedTestModeServiceImpl.DEBUG;
    private PreloadConfigs mPreloadConfigs = new PreloadConfigs();
    private HashMap<String, AppStartRecord> mAppStartedRecords = new HashMap<>();
    private int mNormalAppCount = SpeedTestModeServiceImpl.PRELOAD_APPS.size() - SpeedTestModeServiceImpl.GAME_APPS.size();
    private int mLastPreloadStartedAppCount = 0;
    private int mNextPreloadTargetIndex = 0;
    private boolean mIsInSpeedTestMode = false;

    public PreLoadStrategy() {
        boolean z = false;
        this.mIsLowMemoryDevice = SpeedTestModeServiceImpl.TOTAL_MEMORY < 8192 ? true : z;
    }

    public void onPreloadAppStarted(String packageName) {
    }

    @Override // com.miui.server.sptm.SpeedTestModeServiceImpl.Strategy
    public void onNewEvent(int eventType) {
        ProcessManagerInternal processManagerInternal;
        if (eventType == 5 && (processManagerInternal = this.mPMS) != null) {
            processManagerInternal.setSpeedTestState(false);
        }
    }

    @Override // com.miui.server.sptm.SpeedTestModeServiceImpl.Strategy
    public void onAppStarted(AppStartRecord r) {
        if (SpeedTestModeServiceImpl.SPEED_TEST_APP_LIST.contains(r.packageName) && this.mSpeedTestModeService.getPreloadCloudType() != 0 && !this.mAppStartedRecords.containsKey(r.packageName)) {
            this.mAppStartedRecords.put(r.packageName, r);
            preloadNextApps();
        }
    }

    @Override // com.miui.server.sptm.SpeedTestModeServiceImpl.Strategy
    public void onSpeedTestModeChanged(boolean isEnable) {
        this.mIsInSpeedTestMode = isEnable;
        if (this.mPMS == null) {
            this.mPMS = (ProcessManagerInternal) LocalServices.getService(ProcessManagerInternal.class);
        }
        if (this.mSpeedTestModeService.getPreloadCloudType() == 0) {
            return;
        }
        if (isEnable) {
            preloadNextApps();
            ProcessManagerInternal processManagerInternal = this.mPMS;
            if (processManagerInternal != null) {
                processManagerInternal.setSpeedTestState(isEnable);
                return;
            }
            return;
        }
        if (this.mPMS != null && this.mAppStartedRecords.size() < 14) {
            this.mPMS.setSpeedTestState(isEnable);
        }
        reset();
    }

    private void preloadNextApps() {
        if (!this.mIsInSpeedTestMode) {
            return;
        }
        int appStartedCount = this.mAppStartedRecords.size();
        int preloadThreshold = SpeedTestModeServiceImpl.PRELOAD_THRESHOLD;
        int preloadType = this.mSpeedTestModeService.getPreloadCloudType();
        if (!this.mIsLowMemoryDevice && preloadType > 2 && this.mNextPreloadTargetIndex <= this.mNormalAppCount) {
            preloadThreshold = 2;
        }
        int nextPreloadAppStartedThreshold = this.mLastPreloadStartedAppCount;
        if (nextPreloadAppStartedThreshold != 0) {
            nextPreloadAppStartedThreshold += preloadThreshold;
        }
        if (this.DEBUG) {
            Slog.d(this.TAG, String.format("preloadNextApps: cur_apps: %s, threshold: %s", Integer.valueOf(appStartedCount), Integer.valueOf(nextPreloadAppStartedThreshold)));
        }
        if (appStartedCount < nextPreloadAppStartedThreshold) {
            return;
        }
        this.mLastPreloadStartedAppCount = appStartedCount;
        if (preloadType > 2 && !this.mIsLowMemoryDevice) {
            if (this.mNextPreloadTargetIndex < SpeedTestModeServiceImpl.PRELOAD_APPS.size()) {
                List<String> list = SpeedTestModeServiceImpl.PRELOAD_APPS;
                int i = this.mNextPreloadTargetIndex;
                this.mNextPreloadTargetIndex = i + 1;
                preloadPackage(list.get(i));
            }
        } else if (preloadType > 0 && this.mNextPreloadTargetIndex < SpeedTestModeServiceImpl.PRELOAD_GAME_APPS.size()) {
            List<String> list2 = SpeedTestModeServiceImpl.PRELOAD_GAME_APPS;
            int i2 = this.mNextPreloadTargetIndex;
            this.mNextPreloadTargetIndex = i2 + 1;
            preloadPackage(list2.get(i2));
        }
    }

    private void preloadPackage(String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return;
        }
        try {
            LifecycleConfig lifecycleConfig = this.mPreloadConfigs.find(packageName);
            if (lifecycleConfig != null) {
                int res = ProcessManagerNative.getDefault().startPreloadApp(packageName, true, false, lifecycleConfig);
                if (this.DEBUG) {
                    Slog.d(this.TAG, String.format("preloadNextApps: preload: %s, res=%s", packageName, Integer.valueOf(res)));
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void reset() {
        this.mAppStartedRecords.clear();
        this.mLastPreloadStartedAppCount = 0;
        this.mNextPreloadTargetIndex = 0;
    }

    /* loaded from: classes.dex */
    public static class AppStartRecord {
        String packageName = null;
        boolean isColdStart = false;

        public String toString() {
            return "AppStartRecord{packageName='" + this.packageName + "', isColdStart=" + this.isColdStart + '}';
        }
    }

    /* loaded from: classes.dex */
    public class PreloadConfigs {
        private HashMap<String, LifecycleConfig> mConfigs = new HashMap<>();

        public PreloadConfigs() {
            PreLoadStrategy.this = r7;
            resetPreloadConfigs();
            if (r7.DEBUG) {
                for (Map.Entry<String, LifecycleConfig> entry : this.mConfigs.entrySet()) {
                    Slog.d(r7.TAG, "preload configs:" + entry.getKey() + " " + entry.getValue().getStopTimeout());
                }
            }
        }

        private void resetPreloadConfigs() {
            this.mConfigs.clear();
            int preloadType = PreLoadStrategy.this.mSpeedTestModeService.getPreloadCloudType();
            if (preloadType == 1) {
                setPreloadAppConfig(PreLoadStrategy.SGAME_PACKAGE_NAME, 16000);
            } else if (preloadType == 2) {
                setPreloadAppConfig(PreLoadStrategy.SGAME_PACKAGE_NAME, 15000);
                setPreloadAppConfig(PreLoadStrategy.PUBG_PACKAGE_NAME, 15000);
            } else if (preloadType == 3) {
                if (!PreLoadStrategy.this.mIsLowMemoryDevice) {
                    for (int i = 0; i < PreLoadStrategy.this.mNormalAppCount; i++) {
                        SpeedTestModeServiceImpl unused = PreLoadStrategy.this.mSpeedTestModeService;
                        setPreloadAppConfig(SpeedTestModeServiceImpl.PRELOAD_APPS.get(i), 3000);
                    }
                }
                setPreloadAppConfig(PreLoadStrategy.SGAME_PACKAGE_NAME, 16000);
                setPreloadAppConfig(PreLoadStrategy.PUBG_PACKAGE_NAME, 16000);
            }
            loadProperty();
        }

        private void setPreloadAppConfig(String preloadPkg, int timeOut) {
            LifecycleConfig config = LifecycleConfig.create(1);
            config.setStopTimeout(timeOut);
            SpeedTestModeServiceImpl unused = PreLoadStrategy.this.mSpeedTestModeService;
            config.setSchedAffinity(0, SpeedTestModeServiceImpl.SPTM_LOW_MEMORY_DEVICE_PRELOAD_CORE);
            this.mConfigs.put(preloadPkg, config);
        }

        private void loadProperty() {
            for (Map.Entry<String, LifecycleConfig> entry : this.mConfigs.entrySet()) {
                long stopTimeout = SystemProperties.getLong(PreLoadStrategy.STOP_TIME_PACKAGES_PREFIX + entry.getKey(), -1L);
                if (stopTimeout > 0) {
                    entry.getValue().setStopTimeout(stopTimeout);
                }
            }
        }

        public LifecycleConfig find(String packageName) {
            resetPreloadConfigs();
            LifecycleConfig lifecycleConfig = this.mConfigs.get(packageName);
            return lifecycleConfig;
        }
    }
}
