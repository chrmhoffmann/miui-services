package com.android.server.input.pocketmode;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.util.Slog;
import com.android.server.input.MiuiInputThread;
import com.android.server.input.pocketmode.MiuiPocketModeSensorWrapper;
/* loaded from: classes.dex */
public class MiuiPocketModeManager {
    public static final int AQUIRE_ALLOW_TIME_DIFFERENCE = 100;
    private static final String TAG = "MiuiPocketModeManager";
    private ContentResolver mContentResolver;
    private Context mContext;
    private MiuiPocketModeSensorWrapper mMiuiPocketModeSensorWrapper;
    private MiuiPocketModeSettingsObserver mMiuiPocketModeSettingsObserver;
    private boolean mPocketModeEnableSettings = false;

    public MiuiPocketModeManager(Context context) {
        this.mContext = context;
        this.mContentResolver = context.getContentResolver();
        MiuiPocketModeSettingsObserver miuiPocketModeSettingsObserver = new MiuiPocketModeSettingsObserver();
        this.mMiuiPocketModeSettingsObserver = miuiPocketModeSettingsObserver;
        miuiPocketModeSettingsObserver.initObserver();
        if (isSupportInertialAndLightSensor(context)) {
            Slog.d(TAG, "set inertial and light sensor");
            this.mMiuiPocketModeSensorWrapper = new MiuiPocketModeInertialAndLightSensor(context);
            return;
        }
        Slog.d(TAG, "set proximity sensor");
        this.mMiuiPocketModeSensorWrapper = new MiuiPocketModeProximitySensor(context);
    }

    public static boolean isSupportInertialAndLightSensor(Context context) {
        return hasSupportSensor(context, MiuiPocketModeInertialAndLightSensor.SENSOR_TYPE_INERTIAL_AND_LIGHT);
    }

    private static boolean hasSupportSensor(Context context, int type) {
        Sensor sensor = getSensorManager(context).getDefaultSensor(type);
        return (33171095 != type || sensor == null) ? sensor != null : sensor.getVersion() == 2;
    }

    private static SensorManager getSensorManager(Context context) {
        return (SensorManager) context.getSystemService("sensor");
    }

    public void registerListener(MiuiPocketModeSensorWrapper.ProximitySensorChangeListener sensorListener) {
        MiuiPocketModeSensorWrapper miuiPocketModeSensorWrapper = this.mMiuiPocketModeSensorWrapper;
        if (miuiPocketModeSensorWrapper != null && sensorListener != null) {
            miuiPocketModeSensorWrapper.registerListener(sensorListener);
        }
    }

    public void unregisterListener() {
        MiuiPocketModeSensorWrapper miuiPocketModeSensorWrapper = this.mMiuiPocketModeSensorWrapper;
        if (miuiPocketModeSensorWrapper != null) {
            miuiPocketModeSensorWrapper.unregisterListener();
        }
    }

    public int getStateStableDelay() {
        MiuiPocketModeSensorWrapper miuiPocketModeSensorWrapper = this.mMiuiPocketModeSensorWrapper;
        if (miuiPocketModeSensorWrapper != null) {
            return miuiPocketModeSensorWrapper.getStateStableDelay();
        }
        return MiuiPocketModeSensorWrapper.STATE_STABLE_DELAY;
    }

    public boolean getPocketModeEnableSettings() {
        return this.mPocketModeEnableSettings;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class MiuiPocketModeSettingsObserver extends ContentObserver {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public MiuiPocketModeSettingsObserver() {
            super(MiuiInputThread.getHandler());
            MiuiPocketModeManager.this = this$0;
        }

        public void initObserver() {
            MiuiPocketModeManager.this.mContentResolver.registerContentObserver(Settings.Global.getUriFor("enable_screen_on_proximity_sensor"), false, this, -1);
            onChange(false);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            int pocketModeEnableGlobalSettings = Settings.Global.getInt(MiuiPocketModeManager.this.mContext.getContentResolver(), "enable_screen_on_proximity_sensor", -1);
            if (pocketModeEnableGlobalSettings == -1) {
                MiuiPocketModeManager miuiPocketModeManager = MiuiPocketModeManager.this;
                miuiPocketModeManager.mPocketModeEnableSettings = MiuiSettings.System.getBoolean(miuiPocketModeManager.mContext.getContentResolver(), "enable_screen_on_proximity_sensor", MiuiPocketModeManager.this.mContext.getResources().getBoolean(285540431));
                MiuiSettings.Global.putBoolean(MiuiPocketModeManager.this.mContext.getContentResolver(), "enable_screen_on_proximity_sensor", MiuiPocketModeManager.this.mPocketModeEnableSettings);
                return;
            }
            MiuiPocketModeManager.this.mPocketModeEnableSettings = pocketModeEnableGlobalSettings != 0;
        }
    }
}
