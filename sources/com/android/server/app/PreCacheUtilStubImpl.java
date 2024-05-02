package com.android.server.app;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.SystemProperties;
import android.provider.MiuiSettings;
import android.util.Slog;
import com.miui.base.MiuiStubRegistry;
/* loaded from: classes.dex */
public class PreCacheUtilStubImpl implements PreCacheUtilStub {
    private static final String CLOUD_ALL_DATA_CHANGE_URI = "content://com.android.settings.cloud.CloudSettings/cloud_all_data/notify";
    private static final String CLOUD_PRE_CACHE_APPS = "pre_cache_apps";
    private static final int MAX_APP_LIST_PROP_NUM = 3;
    private static final int MAX_PROP_LENGTH = 90;
    private Context mContext;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<PreCacheUtilStubImpl> {

        /* compiled from: PreCacheUtilStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final PreCacheUtilStubImpl INSTANCE = new PreCacheUtilStubImpl();
        }

        public PreCacheUtilStubImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public PreCacheUtilStubImpl provideNewInstance() {
            return new PreCacheUtilStubImpl();
        }
    }

    protected PreCacheUtilStubImpl() {
    }

    public void registerPreCacheObserver(Context context, Handler handler) {
        this.mContext = context;
        context.getContentResolver().registerContentObserver(Uri.parse(CLOUD_ALL_DATA_CHANGE_URI), false, new PreCacheContentObserver(handler), -2);
        Slog.d("PreCacheUtilStubImpl", "registerContentObserver");
    }

    /* loaded from: classes.dex */
    private class PreCacheContentObserver extends ContentObserver {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public PreCacheContentObserver(Handler handler) {
            super(handler);
            PreCacheUtilStubImpl.this = r1;
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            if (uri != null && uri.equals(Uri.parse(PreCacheUtilStubImpl.CLOUD_ALL_DATA_CHANGE_URI))) {
                PreCacheUtilStubImpl.this.updatePreCacheSetting();
            }
        }
    }

    public void updatePreCacheSetting() {
        String appStr = MiuiSettings.SettingsCloudData.getCloudDataString(this.mContext.getContentResolver(), "precache", CLOUD_PRE_CACHE_APPS, (String) null);
        if (appStr != null) {
            for (int i = 1; i <= 3; i++) {
                if (appStr.length() > MAX_PROP_LENGTH) {
                    int endIndex = appStr.substring(0, MAX_PROP_LENGTH).lastIndexOf(44);
                    String curStr = appStr.substring(0, endIndex);
                    String keyStr = "persist.sys.precache.appstrs" + String.valueOf(i);
                    try {
                        SystemProperties.set(keyStr, curStr);
                        Slog.d("PreCacheUtilStubImpl setProp:", keyStr + ": " + curStr);
                        appStr = appStr.substring(endIndex + 1);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    String keyStr2 = "persist.sys.precache.appstrs" + String.valueOf(i);
                    try {
                        SystemProperties.set(keyStr2, appStr);
                        Slog.d("PreCacheUtilStubImpl setProp:", keyStr2 + ": " + appStr);
                        return;
                    } catch (Exception e2) {
                        e2.printStackTrace();
                        return;
                    }
                }
            }
            return;
        }
        Slog.d("PreCacheUtilStubImpl CloudStr ", "NULL");
    }
}
