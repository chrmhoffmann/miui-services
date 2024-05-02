package com.android.server;

import android.content.Context;
import android.database.ContentObserver;
import android.provider.MiuiSettings;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
/* loaded from: classes.dex */
public class ForceDarkAppListProvider {
    private static final String FORCE_DARK_APP_SETTINGS_FILE_PATH = "system_ext/etc/forcedarkconfig/ForceDarkAppSettings.json";
    private static final String FORCE_DARK_APP_SETTINGS_MODULE_NAME = "force_dark_app_settings_list";
    private static final String TAG = ForceDarkAppListProvider.class.getSimpleName();
    private static volatile ForceDarkAppListProvider sForceDarkAppListProvider = null;
    private ForceDarkAppListChangeListener mAppListChangeListener;
    private HashMap<String, DarkModeAppSettingsInfo> mLocalForceDarkAppSettings = new HashMap<>();
    private HashMap<String, DarkModeAppSettingsInfo> mCloudForceDarkAppSettings = new HashMap<>();

    /* loaded from: classes.dex */
    public interface ForceDarkAppListChangeListener {
        void onChange();
    }

    private ForceDarkAppListProvider() {
        initLocalForceDarkAppList();
    }

    public static ForceDarkAppListProvider getInstance() {
        if (sForceDarkAppListProvider == null) {
            synchronized (ForceDarkAppListProvider.class) {
                if (sForceDarkAppListProvider == null) {
                    sForceDarkAppListProvider = new ForceDarkAppListProvider();
                }
            }
        }
        return sForceDarkAppListProvider;
    }

    public void setAppListChangeListener(ForceDarkAppListChangeListener listChangeListener) {
        this.mAppListChangeListener = listChangeListener;
    }

    public HashMap<String, DarkModeAppSettingsInfo> getForceDarkAppSettings() {
        final HashMap<String, DarkModeAppSettingsInfo> result = new HashMap<>(this.mLocalForceDarkAppSettings);
        this.mCloudForceDarkAppSettings.forEach(new BiConsumer() { // from class: com.android.server.ForceDarkAppListProvider$$ExternalSyntheticLambda1
            @Override // java.util.function.BiConsumer
            public final void accept(Object obj, Object obj2) {
                result.merge((String) obj, (DarkModeAppSettingsInfo) obj2, new BiFunction() { // from class: com.android.server.ForceDarkAppListProvider$$ExternalSyntheticLambda0
                    @Override // java.util.function.BiFunction
                    public final Object apply(Object obj3, Object obj4) {
                        return ForceDarkAppListProvider.lambda$getForceDarkAppSettings$0((DarkModeAppSettingsInfo) obj3, (DarkModeAppSettingsInfo) obj4);
                    }
                });
            }
        });
        return result;
    }

    public static /* synthetic */ DarkModeAppSettingsInfo lambda$getForceDarkAppSettings$0(DarkModeAppSettingsInfo v1, DarkModeAppSettingsInfo v2) {
        return v2;
    }

    public boolean getForceDarkAppDefaultEnable(String packageName) {
        DarkModeAppSettingsInfo darkModeAppSettingsInfo = getForceDarkAppSettings().get(packageName);
        return darkModeAppSettingsInfo != null && darkModeAppSettingsInfo.isDefaultEnable();
    }

    public void onBootPhase(int phase, Context context) {
        updateCloudForceDarkAppList(context);
        registerDataObserver(context);
    }

    private void initLocalForceDarkAppList() {
        String jsonString = "";
        try {
            FileInputStream f = new FileInputStream(FORCE_DARK_APP_SETTINGS_FILE_PATH);
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
            e2.printStackTrace();
        }
        if (TextUtils.isEmpty(jsonString)) {
            return;
        }
        this.mLocalForceDarkAppSettings = parseAppSettings2Map(jsonString);
    }

    public void updateCloudForceDarkAppList(Context context) {
        try {
            MiuiSettings.SettingsCloudData.CloudData data = MiuiSettings.SettingsCloudData.getCloudDataSingle(context.getContentResolver(), FORCE_DARK_APP_SETTINGS_MODULE_NAME, FORCE_DARK_APP_SETTINGS_MODULE_NAME, (String) null, false);
            if (data != null && data.json() != null) {
                Log.d(TAG, "getCloudForceDarkAppList: CloudData: " + data.toString());
                this.mCloudForceDarkAppSettings.clear();
                JSONArray appConfigArray = data.json().getJSONArray(FORCE_DARK_APP_SETTINGS_MODULE_NAME);
                this.mCloudForceDarkAppSettings = parseAppSettings2Map(appConfigArray);
                ForceDarkAppListChangeListener forceDarkAppListChangeListener = this.mAppListChangeListener;
                if (forceDarkAppListChangeListener != null) {
                    forceDarkAppListChangeListener.onChange();
                }
            }
            Log.d(TAG, "getCloudForceDarkAppList: mCloudForceDarkAppSettings: " + this.mCloudForceDarkAppSettings);
        } catch (Exception e) {
            Log.e(TAG, "exception when getCloudForceDarkAppList: ", e);
        }
    }

    private HashMap<String, DarkModeAppSettingsInfo> parseAppSettings2Map(String jsonData) {
        HashMap<String, DarkModeAppSettingsInfo> result = new HashMap<>();
        if (TextUtils.isEmpty(jsonData)) {
            return result;
        }
        try {
            JSONArray appConfigArray = new JSONArray(jsonData);
            for (int i = 0; i < appConfigArray.length(); i++) {
                DarkModeAppSettingsInfo darkModeAppSettingsInfo = new DarkModeAppSettingsInfo();
                JSONObject appConfig = appConfigArray.getJSONObject(i);
                String packageName = appConfig.getString("packageName");
                darkModeAppSettingsInfo.setPackageName(packageName);
                fillDarkModeAppSettingsInfo(darkModeAppSettingsInfo, appConfig);
                result.put(packageName, darkModeAppSettingsInfo);
            }
        } catch (JSONException e) {
            Log.e(TAG, "exception when parseAppConfig2Map: ", e);
        }
        return result;
    }

    private HashMap<String, DarkModeAppSettingsInfo> parseAppSettings2Map(JSONArray appConfigArray) {
        if (appConfigArray == null) {
            return null;
        }
        HashMap<String, DarkModeAppSettingsInfo> result = new HashMap<>();
        for (int i = 0; i < appConfigArray.length(); i++) {
            try {
                DarkModeAppSettingsInfo darkModeAppSettingsInfo = new DarkModeAppSettingsInfo();
                JSONObject appConfig = appConfigArray.getJSONObject(i);
                String packageName = appConfig.getString("packageName");
                darkModeAppSettingsInfo.setPackageName(packageName);
                fillDarkModeAppSettingsInfo(darkModeAppSettingsInfo, appConfig);
                result.put(packageName, darkModeAppSettingsInfo);
            } catch (JSONException e) {
                Log.e(TAG, "exception when parseAppConfig2Map: ", e);
            }
        }
        return result;
    }

    private void fillDarkModeAppSettingsInfo(DarkModeAppSettingsInfo darkModeAppSettingsInfo, JSONObject appConfig) {
        try {
            if (appConfig.has("showInSettings")) {
                darkModeAppSettingsInfo.setShowInSettings(appConfig.getBoolean("showInSettings"));
            }
            if (appConfig.has("defaultEnable")) {
                darkModeAppSettingsInfo.setDefaultEnable(appConfig.getBoolean("defaultEnable"));
            }
            if (appConfig.has("adaptStat")) {
                darkModeAppSettingsInfo.setAdaptStat(appConfig.getInt("adaptStat"));
            }
            if (appConfig.has("overrideEnableValue")) {
                darkModeAppSettingsInfo.setOverrideEnableValue(appConfig.getInt("overrideEnableValue"));
            }
            if (appConfig.has("forceDarkOrigin")) {
                darkModeAppSettingsInfo.setForceDarkOrigin(appConfig.getBoolean("forceDarkOrigin"));
            }
            if (appConfig.has("forceDarkSplashScreen")) {
                darkModeAppSettingsInfo.setForceDarkSplashScreen(appConfig.getInt("forceDarkSplashScreen"));
            }
            if (appConfig.has("interceptRelaunch")) {
                darkModeAppSettingsInfo.setInterceptRelaunch(appConfig.getInt("interceptRelaunch"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void registerDataObserver(final Context context) {
        context.getContentResolver().registerContentObserver(MiuiSettings.SettingsCloudData.getCloudDataNotifyUri(), true, new ContentObserver(MiuiBgThread.getHandler()) { // from class: com.android.server.ForceDarkAppListProvider.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                Slog.w(ForceDarkAppListProvider.class.getSimpleName(), "forceDarkAppListCouldData onChange--");
                ForceDarkAppListProvider.this.updateCloudForceDarkAppList(context);
            }
        });
    }
}
