package com.android.server.wm;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.provider.MiuiSettings;
import android.text.TextUtils;
import com.android.server.display.TemperatureController;
import com.android.server.wm.MiuiCvwGestureController;
import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.json.JSONArray;
import org.json.JSONObject;
/* loaded from: classes.dex */
public class MiuiCvwAspectRatioConfig {
    private static final String KEY_APP_RATIO_CONFIG = "appRatioConfig";
    private static final String MODULE_APP_CONFIG = "cvw_AppConfig";
    public static final int RATIO_1 = 1;
    public static final int RATIO_2 = 2;
    public static final int RATIO_3 = 3;
    private static final String TAG = "MiuiCvwAspectRatioConfig";
    private static final String WRAP_BRACKETS = "[%s]";
    private static final ConcurrentHashMap<String, AppConfig> mAppConfigs = new ConcurrentHashMap<>();
    private final Context mContext;

    public MiuiCvwAspectRatioConfig(final Context context, Looper looper) {
        this.mContext = context;
        final Handler threadHandler = new Handler(looper);
        final ContentObserver cloudDataObserver = new ContentObserver(threadHandler) { // from class: com.android.server.wm.MiuiCvwAspectRatioConfig.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                MiuiCvwAspectRatioConfig.this.updateCloudData();
            }
        };
        new Handler(Looper.getMainLooper()).post(new Runnable() { // from class: com.android.server.wm.MiuiCvwAspectRatioConfig$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                MiuiCvwAspectRatioConfig.this.m1512lambda$new$0$comandroidserverwmMiuiCvwAspectRatioConfig(context, cloudDataObserver, threadHandler);
            }
        });
    }

    /* renamed from: lambda$new$0$com-android-server-wm-MiuiCvwAspectRatioConfig */
    public /* synthetic */ void m1512lambda$new$0$comandroidserverwmMiuiCvwAspectRatioConfig(Context context, ContentObserver cloudDataObserver, Handler threadHandler) {
        context.getContentResolver().registerContentObserver(MiuiSettings.SettingsCloudData.getCloudDataNotifyUri(), false, cloudDataObserver, -1);
        threadHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiCvwAspectRatioConfig$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                MiuiCvwAspectRatioConfig.this.updateCloudData();
            }
        });
    }

    public void updateCloudData() {
        mAppConfigs.clear();
        String data = MiuiSettings.SettingsCloudData.getCloudDataString(this.mContext.getContentResolver(), MODULE_APP_CONFIG, KEY_APP_RATIO_CONFIG, "");
        if (!TextUtils.isEmpty(data)) {
            try {
                JSONArray jsonArray = new JSONArray(data);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.optJSONObject(i);
                    if (jsonObject != null) {
                        AppConfig appConfig = new AppConfig(jsonObject);
                        mAppConfigs.put(appConfig.packageName, appConfig);
                    }
                }
            } catch (Exception e) {
                MiuiCvwGestureController.Slog.d(TAG, "updateCloudData err :", e);
            }
        }
    }

    public static boolean existAppInConfig(String packageName) {
        AppConfig appConfig = mAppConfigs.get(packageName);
        return appConfig != null;
    }

    public static boolean supportGivenRatioInConfig(String packageName, int ratio) {
        AppConfig appConfig = mAppConfigs.get(packageName);
        if (appConfig != null) {
            return appConfig.supportRatio.contains(String.format(WRAP_BRACKETS, Integer.valueOf(ratio)));
        }
        return false;
    }

    public static boolean notSupportGivenRatioInConfig(String packageName, int ratio) {
        AppConfig appConfig = mAppConfigs.get(packageName);
        if (appConfig != null) {
            return appConfig.notSupportRatio.contains(String.format(WRAP_BRACKETS, Integer.valueOf(ratio)));
        }
        return false;
    }

    /* loaded from: classes.dex */
    public static class AppConfig {
        final String notSupportRatio;
        final String packageName;
        final String supportRatio;

        private AppConfig(JSONObject jsonObject) {
            this.supportRatio = jsonObject.optString("supportRatio");
            this.notSupportRatio = jsonObject.optString("notSupportRatio");
            this.packageName = jsonObject.optString(TemperatureController.STRATEGY_NAME);
        }

        public String toString() {
            return "AppConfig{supportRatio='" + this.supportRatio + "'notSupportRatio='" + this.notSupportRatio + "', packageName='" + this.packageName + "'}";
        }
    }

    public void dump(PrintWriter pw, String prefix) {
        String innerPrefix = prefix + "  ";
        pw.println("MiuiCvwAspectRatioConfig:");
        pw.print(innerPrefix);
        pw.println("mAppConfigs: ");
        for (Map.Entry<String, AppConfig> entry : mAppConfigs.entrySet()) {
            pw.print(innerPrefix);
            pw.println(entry.getValue());
        }
    }
}
