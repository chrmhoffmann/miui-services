package com.android.server;

import android.content.Context;
import android.database.ContentObserver;
import android.os.SystemProperties;
import android.provider.MiuiSettings;
import android.util.Slog;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
/* loaded from: classes.dex */
public class ForceDarkAppConfigProvider {
    private static final String CONFIG_VERSION = "configVersion";
    private static final boolean DEBUG = true;
    private static final String FORCE_DARK_APP_AMEND_NAME = "force_dark_app_config_amend";
    private static final String FORCE_DARK_APP_CONFIG_FILE_PATH = "system_ext/etc/forcedarkconfig/";
    private static final String FORCE_DARK_APP_CONFIG_FILE_VERSION_PATH = "system_ext/etc/forcedarkconfig/ForceDarkAppConfigVersion.json";
    private static final String FORCE_DARK_APP_MODULE_NAME = "force_dark_app_config";
    private static final String TAG = ForceDarkAppConfigProvider.class.getSimpleName();
    private static volatile ForceDarkAppConfigProvider sCloudDataHelper = null;
    private boolean debugDisableCloud;
    private ForceDarkAppConfigChangeListener mAppConfigChangeListener;
    private int mCloudConfigVersion = -1;
    private int mLocalConfigVersion = -1;
    private HashMap<String, String> mLocalForceDarkAppConfig = new HashMap<>();
    private HashMap<String, String> mCloudForceDarkAppConfig = new HashMap<>();
    private HashMap<String, String> mAmendForceDarkAppConfig = new HashMap<>();

    /* loaded from: classes.dex */
    public interface ForceDarkAppConfigChangeListener {
        void onChange();
    }

    private ForceDarkAppConfigProvider() {
        this.debugDisableCloud = SystemProperties.getInt("sys.forcedark.disable_cloud", 0) == 1;
    }

    public static ForceDarkAppConfigProvider getInstance() {
        if (sCloudDataHelper == null) {
            synchronized (ForceDarkAppConfigProvider.class) {
                if (sCloudDataHelper == null) {
                    sCloudDataHelper = new ForceDarkAppConfigProvider();
                }
            }
        }
        return sCloudDataHelper;
    }

    public void setAppConfigChangeListener(ForceDarkAppConfigChangeListener listChangeListener) {
        this.mAppConfigChangeListener = listChangeListener;
    }

    public void onBootPhase(int phase, Context context) {
        updateLocalForceDarkAppConfig(context);
        registerDataObserver(context);
        getLocalAppConfigVersion();
    }

    public void updateLocalForceDarkAppConfig(Context context) {
        if (this.debugDisableCloud) {
            this.mCloudForceDarkAppConfig.clear();
            Slog.d(TAG, "getCloudForceDarkConfig empty for disableCloud");
            return;
        }
        try {
            MiuiSettings.SettingsCloudData.CloudData data = MiuiSettings.SettingsCloudData.getCloudDataSingle(context.getContentResolver(), FORCE_DARK_APP_MODULE_NAME, FORCE_DARK_APP_MODULE_NAME, (String) null, false);
            if (data != null && data.json() != null) {
                Slog.d(TAG, "getCloudConfig, force_dark_app_config: " + data.toString());
                JSONObject jsonAll = data.json();
                if (jsonAll.has(FORCE_DARK_APP_MODULE_NAME)) {
                    this.mCloudForceDarkAppConfig.clear();
                    if (jsonAll.has(CONFIG_VERSION)) {
                        this.mCloudConfigVersion = Integer.parseInt(jsonAll.getString(CONFIG_VERSION));
                    }
                    JSONArray appConfigArray = jsonAll.getJSONArray(FORCE_DARK_APP_MODULE_NAME);
                    for (int i = 0; i < appConfigArray.length(); i++) {
                        JSONObject appConfig = appConfigArray.getJSONObject(i);
                        String packageName = appConfig.getString("packageName");
                        this.mCloudForceDarkAppConfig.put(packageName, appConfig.toString());
                    }
                }
            }
            MiuiSettings.SettingsCloudData.CloudData data2 = MiuiSettings.SettingsCloudData.getCloudDataSingle(context.getContentResolver(), FORCE_DARK_APP_AMEND_NAME, FORCE_DARK_APP_AMEND_NAME, (String) null, false);
            if (data2 != null && data2.json() != null) {
                Slog.d(TAG, "getCloudConfig, force_dark_app_config_amend: " + data2.toString());
                JSONObject jsonAll2 = data2.json();
                if (jsonAll2.has(FORCE_DARK_APP_AMEND_NAME)) {
                    this.mAmendForceDarkAppConfig.clear();
                    JSONArray appConfigArray2 = jsonAll2.getJSONArray(FORCE_DARK_APP_AMEND_NAME);
                    for (int i2 = 0; i2 < appConfigArray2.length(); i2++) {
                        JSONObject appConfig2 = appConfigArray2.getJSONObject(i2);
                        String packageName2 = appConfig2.getString("packageName");
                        this.mAmendForceDarkAppConfig.put(packageName2, appConfig2.toString());
                    }
                }
            }
            ForceDarkAppConfigChangeListener forceDarkAppConfigChangeListener = this.mAppConfigChangeListener;
            if (forceDarkAppConfigChangeListener != null) {
                forceDarkAppConfigChangeListener.onChange();
            }
        } catch (Exception e) {
            Slog.e(TAG, "exception when getCloudForceDarkConfig: ", e);
        }
    }

    private void registerDataObserver(final Context context) {
        context.getContentResolver().registerContentObserver(MiuiSettings.SettingsCloudData.getCloudDataNotifyUri(), true, new ContentObserver(MiuiBgThread.getHandler()) { // from class: com.android.server.ForceDarkAppConfigProvider.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                Slog.i(ForceDarkAppConfigProvider.TAG, "forceDarkCouldData onChange--");
                ForceDarkAppConfigProvider.this.updateLocalForceDarkAppConfig(context);
            }
        });
    }

    public String getForceDarkAppConfig(String packageName) {
        String configJson = null;
        if (this.mCloudConfigVersion >= this.mLocalConfigVersion) {
            configJson = getCloudAppConfig(packageName);
        }
        if (configJson == null) {
            configJson = getLocalAppConfig(packageName);
        }
        Slog.d(TAG, "cloudConfigVersion: " + this.mCloudConfigVersion + "   localConfigVersion:" + this.mLocalConfigVersion);
        String amendJson = getAmendAppConfig(packageName);
        if (configJson != null && amendJson != null) {
            try {
                JSONObject jsonObject = new JSONObject(configJson);
                jsonObject.put("amend", amendJson);
                return jsonObject.toString();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return configJson;
    }

    private String getCloudAppConfig(String packageName) {
        if (this.mCloudForceDarkAppConfig.containsKey(packageName)) {
            return this.mCloudForceDarkAppConfig.get(packageName);
        }
        return null;
    }

    private String getAmendAppConfig(String packageName) {
        if (this.mAmendForceDarkAppConfig.containsKey(packageName)) {
            return this.mAmendForceDarkAppConfig.get(packageName);
        }
        return null;
    }

    private String getLocalAppConfig(String packageName) {
        if (this.mLocalForceDarkAppConfig.containsKey(packageName)) {
            return this.mLocalForceDarkAppConfig.get(packageName);
        }
        File localFile = new File(FORCE_DARK_APP_CONFIG_FILE_PATH + packageName + ".json");
        if (localFile.exists()) {
            try {
                FileInputStream inputStream = new FileInputStream(localFile);
                String jsonString = readFully(inputStream);
                this.mLocalForceDarkAppConfig.put(packageName, jsonString);
                inputStream.close();
                return jsonString;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }

    private void getLocalAppConfigVersion() {
        File versionFile = new File(FORCE_DARK_APP_CONFIG_FILE_VERSION_PATH);
        if (versionFile.exists()) {
            try {
                FileInputStream inputStream = new FileInputStream(FORCE_DARK_APP_CONFIG_FILE_VERSION_PATH);
                String jsonString = readFully(inputStream);
                JSONObject configVersionJson = new JSONObject(jsonString);
                if (configVersionJson.has(CONFIG_VERSION)) {
                    this.mLocalConfigVersion = Integer.parseInt(configVersionJson.getString(CONFIG_VERSION));
                }
                inputStream.close();
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private String readFully(InputStream inputStream) throws IOException {
        OutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int read = inputStream.read(buffer);
        while (read >= 0) {
            baos.write(buffer, 0, read);
            read = inputStream.read(buffer);
        }
        return baos.toString();
    }
}
