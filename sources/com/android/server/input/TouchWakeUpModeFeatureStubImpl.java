package com.android.server.input;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.util.Slog;
import android.view.SurfaceControl;
import com.android.server.input.config.InputCommonConfig;
import com.miui.base.MiuiStubRegistry;
import miui.os.DeviceFeature;
import miui.util.ITouchFeature;
/* loaded from: classes.dex */
public class TouchWakeUpModeFeatureStubImpl implements TouchWakeUpModeFeatureStub {
    private static final int WAKEUP_OFF = 4;
    private static final int WAKEUP_ON = 5;
    private Context mContext;
    private Handler mHandler;
    private long mPtr;
    private ITouchFeature mTouchFeature;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<TouchWakeUpModeFeatureStubImpl> {

        /* compiled from: TouchWakeUpModeFeatureStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final TouchWakeUpModeFeatureStubImpl INSTANCE = new TouchWakeUpModeFeatureStubImpl();
        }

        public TouchWakeUpModeFeatureStubImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public TouchWakeUpModeFeatureStubImpl provideNewInstance() {
            return new TouchWakeUpModeFeatureStubImpl();
        }
    }

    public void initTouchWakeupModeFeature(Context context, Handler handler, long ptr) {
        this.mContext = context;
        this.mHandler = handler;
        this.mPtr = ptr;
        this.mTouchFeature = ITouchFeature.getInstance();
    }

    public void registerTouchWakeupModeSettingObserver() {
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("gesture_wakeup"), false, new ContentObserver(this.mHandler) { // from class: com.android.server.input.TouchWakeUpModeFeatureStubImpl.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                TouchWakeUpModeFeatureStubImpl.this.updateTouchWakeupModeFromSettings();
            }
        }, -1);
    }

    public void registerSubScreenTouchWakeupModeSettingObserver() {
        if (!DeviceFeature.IS_SUBSCREEN_DEVICE) {
            return;
        }
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("subscreen_gesture_wakeup"), false, new ContentObserver(this.mHandler) { // from class: com.android.server.input.TouchWakeUpModeFeatureStubImpl.2
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                TouchWakeUpModeFeatureStubImpl.this.updateSubScreenTouchWakeupModeFromSettings();
            }
        }, -1);
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r0v2, types: [int, boolean] */
    public void updateTouchWakeupModeFromSettings() {
        ?? booleanForUser = MiuiSettings.System.getBooleanForUser(this.mContext.getContentResolver(), "gesture_wakeup", false, -2);
        if (this.mTouchFeature.hasDoubleTapWakeUpSupport()) {
            Slog.d("TouchWakeUpModeFeature", "gesture_wakeup modeOn = " + ((boolean) booleanForUser));
            this.mTouchFeature.setTouchMode(0, 14, booleanForUser == true ? 1 : 0);
            if (SurfaceControl.getPhysicalDisplayIds().length == 2) {
                this.mTouchFeature.setTouchMode(1, 14, (int) booleanForUser);
                return;
            }
            return;
        }
        switchTouchWakeupMode(booleanForUser);
    }

    public void updateSubScreenTouchWakeupModeFromSettings() {
        if (this.mTouchFeature.hasDoubleTapWakeUpSupport() && DeviceFeature.IS_SUBSCREEN_DEVICE) {
            boolean touchWakeupMode = MiuiSettings.System.getBooleanForUser(this.mContext.getContentResolver(), "subscreen_gesture_wakeup", false, -2);
            Slog.d("TouchWakeUpModeFeature", "subscreen_gesture_wakeup modeOn = " + touchWakeupMode);
            if (SurfaceControl.getPhysicalDisplayIds().length == 2) {
                this.mTouchFeature.setTouchMode(1, 14, touchWakeupMode ? 1 : 0);
            }
        }
    }

    public void switchTouchWakeupMode(boolean modeOn) {
        Slog.d("TouchWakeUpModeFeature", "wakeup modeOn = " + modeOn);
        InputCommonConfig inputCommonConfig = InputCommonConfig.getInstance();
        inputCommonConfig.setWakeUpMode(modeOn ? 5 : 4);
        inputCommonConfig.flushToNative();
    }
}
