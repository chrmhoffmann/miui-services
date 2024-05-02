package com.android.server.input.overscroller;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Slog;
import android.util.SparseArray;
import java.util.HashMap;
import miui.hardware.input.IAppScrollerOptimizationConfigChangedListener;
import miui.os.DeviceFeature;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
/* loaded from: classes.dex */
public class ScrollerOptimizationConfigProvider {
    private static final String POWER_PERFORMANCE_MODE = "POWER_PERFORMANCE_MODE_OPEN";
    private static final String POWER_PERFORMANCE_MODE_OFF = "0";
    private static final String POWER_PERFORMANCE_MODE_ON = "1";
    private static final String SCROLLER_OPTIMIZATION_CONFIG_FILE_PATH = "system_ext/etc/overscrolleroptimization/config.json";
    private static final String SCROLLER_OPTIMIZATION_MODULE_NAME = "over_scroller_optimization_config";
    private static String mCloudGeneralScrollerOptimizationConfig;
    private static String mLocalGeneralScrollerOptimizationConfig;
    private int mCurrentUserId;
    private Handler mHandler;
    protected ContentObserver mPowerPerformanceModeChangeObserver;
    private static final String TAG = ScrollerOptimizationConfigProvider.class.getSimpleName();
    private static volatile ScrollerOptimizationConfigProvider INSTANCE = null;
    private static HashMap<String, String> mLocalAppScrollerOptimizationConfig = new HashMap<>();
    private static HashMap<String, String> mCloudAppScrollerOptimizationConfig = new HashMap<>();
    private boolean mPowerPerformanceMode = false;
    private final SparseArray<ScrollerOptimizationConfigChangedListenerRecord> mScrollerStateChangedListeners = new SparseArray<>();
    private final Object mScrollerListenerLock = new Object();

    private ScrollerOptimizationConfigProvider() {
        if (DeviceFeature.SCROLLER_OPTIMIZATION_SUPPORT) {
            HandlerThread handlerThread = new HandlerThread("scrolleroptimizationthread");
            handlerThread.start();
            this.mHandler = handlerThread.getThreadHandler();
        }
    }

    public static ScrollerOptimizationConfigProvider getInstance() {
        if (INSTANCE == null) {
            synchronized (ScrollerOptimizationConfigProvider.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ScrollerOptimizationConfigProvider();
                }
            }
        }
        return INSTANCE;
    }

    public void onUserSwitch(int newUserId) {
        this.mCurrentUserId = newUserId;
    }

    public void systemBooted(Context context) {
        if (!DeviceFeature.SCROLLER_OPTIMIZATION_SUPPORT) {
            return;
        }
        loadLocalScrollerOptimizationConfig();
        updateCloudScrollerOptimizationConfig(context);
        registerDataObserver(context);
        registerPowerPerformanceModeChangeObserver(context);
    }

    private void registerDataObserver(final Context context) {
        context.getContentResolver().registerContentObserver(MiuiSettings.SettingsCloudData.getCloudDataNotifyUri(), true, new ContentObserver(this.mHandler) { // from class: com.android.server.input.overscroller.ScrollerOptimizationConfigProvider.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                ScrollerOptimizationConfigProvider.this.updateCloudScrollerOptimizationConfig(context);
            }
        });
    }

    private void registerPowerPerformanceModeChangeObserver(final Context context) {
        this.mPowerPerformanceModeChangeObserver = new ContentObserver(this.mHandler) { // from class: com.android.server.input.overscroller.ScrollerOptimizationConfigProvider.2
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                ScrollerOptimizationConfigProvider scrollerOptimizationConfigProvider = ScrollerOptimizationConfigProvider.this;
                boolean z = false;
                if (Settings.System.getIntForUser(context.getContentResolver(), ScrollerOptimizationConfigProvider.POWER_PERFORMANCE_MODE, 0, ScrollerOptimizationConfigProvider.this.mCurrentUserId) == 1) {
                    z = true;
                }
                scrollerOptimizationConfigProvider.mPowerPerformanceMode = z;
                Slog.i(ScrollerOptimizationConfigProvider.TAG, "mPowerPerformanceModeChangeObserver mPowerPerformanceMode:" + ScrollerOptimizationConfigProvider.this.mPowerPerformanceMode);
                ScrollerOptimizationConfigProvider.this.callScrollerOptimizationConfigListener(true);
            }
        };
        context.getContentResolver().registerContentObserver(Settings.System.getUriFor(POWER_PERFORMANCE_MODE), false, this.mPowerPerformanceModeChangeObserver, -1);
        this.mPowerPerformanceModeChangeObserver.onChange(true);
    }

    public void callScrollerOptimizationConfigListener(boolean isPowerPerformanceModeChanged) {
        String[] result = new String[2];
        result[0] = this.mPowerPerformanceMode ? "1" : POWER_PERFORMANCE_MODE_OFF;
        for (int i = 0; i < this.mScrollerStateChangedListeners.size(); i++) {
            ScrollerOptimizationConfigChangedListenerRecord scrollerOptimizationConfigChangedListenerRecord = this.mScrollerStateChangedListeners.valueAt(i);
            if (isPowerPerformanceModeChanged) {
                result[1] = null;
            } else {
                result[1] = getAppScrollerOptimizationConfig(scrollerOptimizationConfigChangedListenerRecord.mPackageName);
            }
            try {
                scrollerOptimizationConfigChangedListenerRecord.mListener.onScrollerOptimizationConfig(result);
            } catch (Exception e) {
                Slog.i(TAG, e.toString());
            }
        }
    }

    public void updateCloudScrollerOptimizationConfig(Context context) {
        String str = TAG;
        Slog.i(str, "update cloud scroller optimization config");
        MiuiSettings.SettingsCloudData.CloudData cloudData = MiuiSettings.SettingsCloudData.getCloudDataSingle(context.getContentResolver(), SCROLLER_OPTIMIZATION_MODULE_NAME, SCROLLER_OPTIMIZATION_MODULE_NAME, (String) null, false);
        if (cloudData != null && cloudData.json() != null) {
            try {
                JSONObject cloudJson = cloudData.json();
                Slog.d(str, "cloud config: " + cloudJson.toString());
                mCloudGeneralScrollerOptimizationConfig = ScrollerOptimizationConfigProviderUtils.parseGeneralConfig(cloudJson);
                if (cloudJson.has(ScrollerOptimizationConfigProviderUtils.APP_LIST_NAME)) {
                    mCloudAppScrollerOptimizationConfig.clear();
                    JSONArray appConfigArray = cloudJson.getJSONArray(ScrollerOptimizationConfigProviderUtils.APP_LIST_NAME);
                    for (int i = 0; i < appConfigArray.length(); i++) {
                        JSONObject appConfig = appConfigArray.getJSONObject(i);
                        String packageName = appConfig.getString("packageName");
                        mCloudAppScrollerOptimizationConfig.put(packageName, appConfig.toString());
                    }
                }
                if (!TextUtils.isEmpty(mCloudGeneralScrollerOptimizationConfig)) {
                    callScrollerOptimizationConfigListener(false);
                }
            } catch (JSONException e) {
                Slog.e(TAG, "exception when updateCloudScrollerOptimizationConfig: ", e);
            }
        }
    }

    private void loadLocalScrollerOptimizationConfig() {
        String str = TAG;
        Slog.i(str, "load local scroller optimization config");
        try {
            JSONObject localJson = ScrollerOptimizationConfigProviderUtils.getLocalFileConfig(SCROLLER_OPTIMIZATION_CONFIG_FILE_PATH);
            Slog.d(str, "local config: " + localJson.toString());
            mLocalGeneralScrollerOptimizationConfig = ScrollerOptimizationConfigProviderUtils.parseGeneralConfig(localJson);
            if (localJson.has(ScrollerOptimizationConfigProviderUtils.APP_LIST_NAME)) {
                mLocalAppScrollerOptimizationConfig.clear();
                JSONArray appConfigArray = localJson.getJSONArray(ScrollerOptimizationConfigProviderUtils.APP_LIST_NAME);
                for (int i = 0; i < appConfigArray.length(); i++) {
                    JSONObject appConfig = appConfigArray.getJSONObject(i);
                    String packageName = appConfig.getString("packageName");
                    mLocalAppScrollerOptimizationConfig.put(packageName, appConfig.toString());
                }
            }
        } catch (JSONException e) {
            Slog.e(TAG, "exception when loadLocalScrollerOptimizationConfig: ", e);
        }
    }

    public String[] getAppScrollerOptimizationConfigAndSwitchState(String packageName) {
        if (!DeviceFeature.SCROLLER_OPTIMIZATION_SUPPORT) {
            return null;
        }
        String[] result = new String[2];
        result[0] = this.mPowerPerformanceMode ? "1" : POWER_PERFORMANCE_MODE_OFF;
        result[1] = getAppScrollerOptimizationConfig(packageName);
        return result;
    }

    private String getAppScrollerOptimizationConfig(String packageName) {
        if (!DeviceFeature.SCROLLER_OPTIMIZATION_SUPPORT) {
            return null;
        }
        String config = getCloudScrollerOptimizationConfig(packageName);
        if (TextUtils.isEmpty(config)) {
            return getLocalScrollerOptimizationConfig(packageName);
        }
        return config;
    }

    public void registerAppScrollerOptimizationConfigListener(String packageName, IAppScrollerOptimizationConfigChangedListener listener) {
        if (!DeviceFeature.SCROLLER_OPTIMIZATION_SUPPORT) {
            return;
        }
        synchronized (this.mScrollerListenerLock) {
            int callingPid = Binder.getCallingPid();
            if (this.mScrollerStateChangedListeners.get(callingPid) != null) {
                Slog.e(TAG, new SecurityException("The calling process has already registered an AppScrollerOptimizationConfigChangedListener").toString());
            }
            ScrollerOptimizationConfigChangedListenerRecord record = new ScrollerOptimizationConfigChangedListenerRecord(callingPid, packageName, listener);
            try {
                IBinder binder = listener.asBinder();
                binder.linkToDeath(record, 0);
            } catch (RemoteException ex) {
                Slog.e(TAG, new RuntimeException(ex).toString());
            }
            this.mScrollerStateChangedListeners.put(callingPid, record);
        }
    }

    private String getCloudScrollerOptimizationConfig(String packageName) {
        if (mCloudAppScrollerOptimizationConfig.containsKey(packageName)) {
            return mCloudAppScrollerOptimizationConfig.get(packageName);
        }
        return mCloudGeneralScrollerOptimizationConfig;
    }

    private String getLocalScrollerOptimizationConfig(String packageName) {
        if (mLocalAppScrollerOptimizationConfig.containsKey(packageName)) {
            return mLocalAppScrollerOptimizationConfig.get(packageName);
        }
        return mLocalGeneralScrollerOptimizationConfig;
    }

    public void onScrollerStateChangedListenerDied(int pid) {
        synchronized (this.mScrollerListenerLock) {
            this.mScrollerStateChangedListeners.remove(pid);
        }
    }

    /* loaded from: classes.dex */
    public final class ScrollerOptimizationConfigChangedListenerRecord implements IBinder.DeathRecipient {
        public IAppScrollerOptimizationConfigChangedListener mListener;
        public String mPackageName;
        public int mPid;

        ScrollerOptimizationConfigChangedListenerRecord(int pid, String packageName, IAppScrollerOptimizationConfigChangedListener listener) {
            ScrollerOptimizationConfigProvider.this = r3;
            this.mPid = pid;
            this.mPackageName = packageName;
            this.mListener = listener;
            if (listener == null) {
                Slog.i(ScrollerOptimizationConfigProvider.TAG, "listener is null, packageName:" + packageName);
            }
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            ScrollerOptimizationConfigProvider.this.onScrollerStateChangedListenerDied(this.mPid);
        }
    }
}
