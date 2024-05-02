package com.miui.server.sf;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Slog;
/* loaded from: classes.dex */
public class SFCloudService {
    private static final int MSG_REGISTER_CLOUD_OBSERVER = 1;
    private static final String SF_PARAMETER = "perf_sf_parameter";
    public static final String TAG = "SFCloudService";
    private static SFCloudService sInstance = new SFCloudService();
    private Context mContext = null;
    private Handler mH;
    private HandlerThread mHandlerThread;

    private SFCloudService() {
    }

    public static SFCloudService getInstance() {
        return sInstance;
    }

    public void initSFCloudStrategy(Context context) {
        this.mContext = context;
        HandlerThread handlerThread = new HandlerThread("PSFCloudServiceTh");
        this.mHandlerThread = handlerThread;
        handlerThread.start();
        Handler handler = new Handler(this.mHandlerThread.getLooper()) { // from class: com.miui.server.sf.SFCloudService.1
            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case 1:
                        SFCloudService sFCloudService = SFCloudService.this;
                        sFCloudService.registerCloudObserver(sFCloudService.mContext);
                        SFCloudService.this.updateCloudControlParas();
                        return;
                    default:
                        return;
                }
            }
        };
        this.mH = handler;
        Message msg = handler.obtainMessage(1);
        this.mH.sendMessage(msg);
        Slog.d(TAG, "init SFCloudService");
    }

    public void registerCloudObserver(Context context) {
        ContentObserver observer = new ContentObserver(this.mH) { // from class: com.miui.server.sf.SFCloudService.2
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                if (uri.equals(Settings.System.getUriFor("perf_shielder_SF"))) {
                    SFCloudService.this.updateCloudControlParas();
                }
            }
        };
        context.getContentResolver().registerContentObserver(Settings.System.getUriFor("perf_shielder_SF"), false, observer, -2);
    }

    public void updateCloudControlParas() {
        String perfSFParameter = Settings.System.getStringForUser(this.mContext.getContentResolver(), SF_PARAMETER, -2);
        Slog.d(TAG, "SF get the ScrollFrictionFactor from cloudControler: " + perfSFParameter);
        SystemProperties.set("persist.sys.view.ScrollFrictionFactor", perfSFParameter);
    }
}
