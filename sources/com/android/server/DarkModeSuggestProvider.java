package com.android.server;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.MiuiSettings;
import android.text.TextUtils;
import android.util.Slog;
import java.util.HashSet;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
/* loaded from: classes.dex */
public class DarkModeSuggestProvider {
    private static final String DARK_MODE_DATA_DISABLE_REGION = "dark_mode_data_disable_region";
    private static final String DARK_MODE_SUGGEST_MODULE_NAME = "dark_mode_suggest";
    private static final String TAG = DarkModeSuggestProvider.class.getSimpleName();
    private static volatile DarkModeSuggestProvider sDarkModeSuggestProvider;
    private ContentObserver mDarkModeSuggestObserver;

    public static DarkModeSuggestProvider getInstance() {
        if (sDarkModeSuggestProvider == null) {
            synchronized (DarkModeSuggestProvider.class) {
                if (sDarkModeSuggestProvider == null) {
                    sDarkModeSuggestProvider = new DarkModeSuggestProvider();
                }
            }
        }
        return sDarkModeSuggestProvider;
    }

    public void updateCloudDataForDarkModeSuggest(Context context) {
        try {
            MiuiSettings.SettingsCloudData.CloudData data = MiuiSettings.SettingsCloudData.getCloudDataSingle(context.getContentResolver(), DARK_MODE_SUGGEST_MODULE_NAME, DARK_MODE_SUGGEST_MODULE_NAME, (String) null, false);
            if (data != null && data.json() != null) {
                String str = TAG;
                Slog.d(str, "suggestData: " + data.json());
                JSONObject suggestArray = data.json().getJSONArray(DARK_MODE_SUGGEST_MODULE_NAME).getJSONObject(0);
                DarkModeTimeModeHelper.setDarkModeSuggestEnable(context, suggestArray.getBoolean("showDarkModeSuggest"));
                Slog.d(str, "sHasGetSuggestFromCloud: " + DarkModeTimeModeHelper.isDarkModeSuggestEnable(context));
            }
        } catch (Exception e) {
            Slog.e(TAG, "exception when updateCloudDataForDarkModeSuggest", e);
        }
    }

    public Set<String> updateCloudDataForDisableRegion(Context context) {
        Set<String> set = new HashSet<>();
        try {
            String data = MiuiSettings.SettingsCloudData.getCloudDataString(context.getContentResolver(), DARK_MODE_DATA_DISABLE_REGION, DARK_MODE_DATA_DISABLE_REGION, (String) null);
            if (!TextUtils.isEmpty(data)) {
                JSONArray apps = new JSONArray(data);
                for (int i = 0; i < apps.length(); i++) {
                    set.add(apps.getString(i));
                }
            }
            Slog.i(TAG, "uploadCloudDataForDisableRegion: CloudList: " + set);
        } catch (JSONException e) {
            Slog.e(TAG, "exception when updateDisableRegionList: ", e);
        }
        return set;
    }

    public void registerDataObserver(Context context) {
        Slog.i(TAG, "registerDataObserver");
        this.mDarkModeSuggestObserver = new DarkModeSuggestObserver(MiuiBgThread.getHandler(), context);
        context.getContentResolver().registerContentObserver(MiuiSettings.SettingsCloudData.getCloudDataNotifyUri(), true, this.mDarkModeSuggestObserver);
    }

    public void unRegisterDataObserver(Context context) {
        Slog.i(TAG, "unRegisterDataObserver");
        if (this.mDarkModeSuggestObserver == null) {
            return;
        }
        context.getContentResolver().unregisterContentObserver(this.mDarkModeSuggestObserver);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class DarkModeSuggestObserver extends ContentObserver {
        private final String TAG = DarkModeSuggestProvider.class.getSimpleName();
        private Context mContext;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public DarkModeSuggestObserver(Handler handler, Context context) {
            super(handler);
            DarkModeSuggestProvider.this = r1;
            this.mContext = context;
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            Slog.i(this.TAG, "onChange");
            DarkModeSuggestProvider.this.updateCloudDataForDarkModeSuggest(this.mContext);
            DarkModeTimeModeHelper.updateDarkModeSuntime(this.mContext);
        }
    }
}
