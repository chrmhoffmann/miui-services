package com.android.server.wm;

import android.os.SystemProperties;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Slog;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
/* loaded from: classes.dex */
public class AppResolutionTuner {
    private static final float DEFAULT_SCALE = 1.0f;
    private static final String FILTERED_WINDOWS_SEPARATOR = ";";
    private static final String NODE_APP_RT_ACTIVITY_WHITELIST = "activity_whitelist";
    private static final String NODE_APP_RT_CONFIG = "app_rt_config";
    private static final String NODE_APP_RT_FILTERED_WINDOWS = "filtered_windows";
    private static final String NODE_APP_RT_FLOW = "flow";
    private static final String NODE_APP_RT_PACKAGE_NAME = "package_name";
    private static final String NODE_APP_RT_SCALE = "scale";
    public static final String TAG = "APP_RT";
    private static final String VALUE_SCALING_FLOW_WMS = "wms";
    private static volatile AppResolutionTuner sAppResolutionTuner;
    private Map<String, AppRTConfig> mAppRTConfigMap = new HashMap();
    private boolean mAppRTEnable = SystemProperties.getBoolean("persist.sys.resolutiontuner.enable", false);
    public static final boolean DEBUG = SystemProperties.getBoolean("persist.sys.resolutiontuner.debug", false);
    private static final Object sLock = new Object();

    private AppResolutionTuner() {
    }

    public static AppResolutionTuner getInstance() {
        if (sAppResolutionTuner == null) {
            synchronized (sLock) {
                if (sAppResolutionTuner == null) {
                    sAppResolutionTuner = new AppResolutionTuner();
                }
            }
        }
        return sAppResolutionTuner;
    }

    public boolean updateResolutionTunerConfig(String configStr) {
        JSONException e;
        String str = FILTERED_WINDOWS_SEPARATOR;
        if (DEBUG) {
            Slog.d(TAG, "updateResolutionTunerConfig: " + configStr);
        }
        if (configStr == null || configStr.isEmpty()) {
            return false;
        }
        Map<String, AppRTConfig> arrayMap = new ArrayMap<>();
        try {
            JSONObject object = new JSONObject(configStr);
            if (object.has(NODE_APP_RT_CONFIG)) {
                JSONArray configs = object.getJSONArray(NODE_APP_RT_CONFIG);
                int i = 0;
                while (i < configs.length()) {
                    JSONObject config = configs.getJSONObject(i);
                    if (config.has(NODE_APP_RT_PACKAGE_NAME) && config.has(NODE_APP_RT_SCALE)) {
                        if (config.has(NODE_APP_RT_FILTERED_WINDOWS) && config.has(NODE_APP_RT_FLOW)) {
                            String packageName = config.getString(NODE_APP_RT_PACKAGE_NAME);
                            JSONArray configs2 = configs;
                            JSONObject object2 = object;
                            float scale = (float) config.getDouble(NODE_APP_RT_SCALE);
                            String scalingFlow = config.getString(NODE_APP_RT_FLOW);
                            String filteredWindowsStr = config.getString(NODE_APP_RT_FILTERED_WINDOWS);
                            String[] filteredWindowsArray = filteredWindowsStr.split(str);
                            List<String> filteredWindows = Arrays.asList(filteredWindowsArray);
                            String activityWhitelistStr = config.getString(NODE_APP_RT_ACTIVITY_WHITELIST);
                            String[] activityWhitelistArray = activityWhitelistStr.split(str);
                            List<String> activityWhitelist = Arrays.asList(activityWhitelistArray);
                            int i2 = i;
                            String str2 = str;
                            ArrayMap arrayMap2 = arrayMap;
                            try {
                                arrayMap2.put(packageName, new AppRTConfig(scale, filteredWindows, scalingFlow, activityWhitelist));
                                i = i2 + 1;
                                arrayMap = arrayMap2;
                                object = object2;
                                configs = configs2;
                                str = str2;
                            } catch (JSONException e2) {
                                e = e2;
                                e.printStackTrace();
                                return false;
                            }
                        }
                    }
                    Log.e(TAG, "uncompleted config!");
                    return false;
                }
                Map<String, AppRTConfig> appRTConfigMap = arrayMap;
                this.mAppRTConfigMap = appRTConfigMap;
                if (DEBUG) {
                    Slog.d(TAG, "updateResolutionTunerConfig: " + this.mAppRTConfigMap.toString());
                    return true;
                }
                return true;
            }
            return false;
        } catch (JSONException e3) {
            e = e3;
        }
    }

    public void setAppRTEnable(boolean enable) {
        if (DEBUG) {
            Slog.d(TAG, "setAppRTEnable: " + enable);
        }
        this.mAppRTEnable = enable;
    }

    public boolean isAppRTEnable() {
        if (DEBUG) {
            Slog.d(TAG, "isAppRTEnable: " + this.mAppRTEnable);
        }
        return this.mAppRTEnable;
    }

    public boolean isScaledByWMS(String packageName, String windowName) {
        AppRTConfig config;
        return !this.mAppRTConfigMap.isEmpty() && (config = this.mAppRTConfigMap.get(packageName)) != null && VALUE_SCALING_FLOW_WMS.equals(config.getScalingFlow()) && !config.getFilteredWindows().contains(windowName) && config.isActivityWhitelist(windowName);
    }

    public float getScaleValue(String packageName) {
        AppRTConfig config;
        if (!this.mAppRTConfigMap.isEmpty() && (config = this.mAppRTConfigMap.get(packageName)) != null) {
            return config.getScale();
        }
        return 1.0f;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class AppRTConfig {
        private List<String> activityWhitelist;
        private List<String> filteredWindows;
        private float scale;
        private String scalingFlow;

        public AppRTConfig(float scale, List<String> filteredWindows, String scalingFlow, List<String> activityWhitelist) {
            AppResolutionTuner.this = this$0;
            this.scale = 1.0f;
            this.filteredWindows = new ArrayList();
            this.activityWhitelist = new ArrayList();
            this.scalingFlow = AppResolutionTuner.VALUE_SCALING_FLOW_WMS;
            this.scale = scale;
            this.filteredWindows = filteredWindows;
            this.scalingFlow = scalingFlow;
            this.activityWhitelist = activityWhitelist;
        }

        public float getScale() {
            return this.scale;
        }

        public List<String> getFilteredWindows() {
            return this.filteredWindows;
        }

        public boolean isActivityWhitelist(String target) {
            for (String s : this.activityWhitelist) {
                Slog.e(AppResolutionTuner.TAG, "target: " + target + " list: " + s + " isEmpty: " + s.isEmpty());
                if (!s.isEmpty() && target.contains(s)) {
                    return true;
                }
            }
            return false;
        }

        public String getScalingFlow() {
            return this.scalingFlow;
        }

        public String toString() {
            return "AppRTConfig{scale=" + this.scale + ", filteredWindows=" + this.filteredWindows + ", activityWhitelist=" + this.activityWhitelist + ", scalingFlow='" + this.scalingFlow + "'}";
        }
    }
}
