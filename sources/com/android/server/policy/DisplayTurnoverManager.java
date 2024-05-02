package com.android.server.policy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.util.Pair;
import com.android.server.wm.MiuiFreeformPinManagerService;
/* loaded from: classes.dex */
public class DisplayTurnoverManager implements SensorEventListener {
    private static final int ACCURATE_THRESHOLD = 2;
    public static final int CODE_TURN_OFF_SUB_DISPLAY = 16777211;
    public static final int CODE_TURN_ON_SUB_DISPLAY = 16777212;
    private static final int CRESCENDO_RINGER_MILLI_TIME_DELAY_ANTISPAM_STRANGER = 4500;
    private static final int CRESCENDO_RINGER_MILLI_TIME_DELAY_MARK_RINGING = 1500;
    private static final int CRESCENDO_RINGER_SECONDS_TIME = 4;
    private static final int CRESCENDO_RINGER_SECONDS_TIME_WITH_HEADSET = 10;
    private static final int DECRESCENDO_DELAY_TIME = 400;
    private static final float DECRESCENDO_INTERVAL_VOLUME = 0.08f;
    private static final float DECRESCENDO_MIN_VOLUME = 0.52f;
    private static final float HORIZONTAL_Z_ACCELERATION = 9.0f;
    private static final String KEY_SUB_SCREEN_FACE_UP = "sub_screen_face_up";
    private static final float NS2S = 1.0E-9f;
    private static final int SENSOR_SCREEN_DOWN = 33171037;
    private static final int SUB_SCREEN_OFF = 1;
    private static final int SUB_SCREEN_ON = 2;
    private static final String TAG = "DisplayTurnoverManager";
    private static final float TILT_Z_ACCELERATION = 8.9f;
    private static final int TRIGGER_THRESHOLD = 1;
    private Pair<Float, Long> mAccValues;
    private Sensor mAccelerometerSensor;
    private Context mContext;
    private Sensor mGravitySensor;
    private float[] mGravityValues;
    private Sensor mGyroscopeSensor;
    private float mGyroscopeTimestamp;
    private float[] mGyroscopeValues;
    private boolean mIsLastStateOn;
    private boolean mIsTurnOverMuteEnabled;
    private boolean mIsUserSetupComplete;
    private Sensor mLinearAccSensor;
    private Pair<Float, Long> mLinearAccValues;
    private float[] mRadians;
    private Sensor mScreendownSensor;
    private float[] mSensorDownValues;
    private SensorManager mSensorManager;
    private SettingsObserver mSettingsObserver;
    private boolean mTurnoverTriggered;
    private boolean mListeningSensor = false;
    private Handler mHandler = new Handler();
    private IBinder mIPowerManager = ServiceManager.getService("power");

    public DisplayTurnoverManager(Context context) {
        this.mContext = context;
        this.mSensorManager = (SensorManager) context.getSystemService("sensor");
        this.mContext.registerReceiver(new BroadcastReceiver() { // from class: com.android.server.policy.DisplayTurnoverManager.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                DisplayTurnoverManager.this.updateSubSwitchMode();
            }
        }, new IntentFilter("android.intent.action.USER_SWITCHED"), null, this.mHandler);
    }

    public void systemReady() {
        this.mSettingsObserver = new SettingsObserver(this.mHandler);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("subscreen_switch"), false, this.mSettingsObserver, -1);
        updateSubSwitchMode();
    }

    public void switchSubDisplayPowerState(boolean isOnSubScreen, String reason) {
        int code = isOnSubScreen ? CODE_TURN_ON_SUB_DISPLAY : CODE_TURN_OFF_SUB_DISPLAY;
        turnOnOrOFFSubDisplay(code, reason);
    }

    @Override // android.hardware.SensorEventListener
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case 1:
                this.mAccValues = new Pair<>(Float.valueOf(event.values[2]), Long.valueOf(System.currentTimeMillis()));
                break;
            case 4:
                this.mGyroscopeValues = event.values;
                break;
            case 9:
                this.mGravityValues = event.values;
                break;
            case 10:
                this.mLinearAccValues = new Pair<>(Float.valueOf(event.values[2]), Long.valueOf(System.currentTimeMillis()));
                break;
            case SENSOR_SCREEN_DOWN /* 33171037 */:
                this.mSensorDownValues = event.values;
                break;
        }
        if (this.mGravityValues != null) {
            readyToTurnoverOrHandonByGravity();
            return;
        }
        Log.w(TAG, "Do not support Gravity sensor!!!");
        if (this.mSensorDownValues == null) {
            Log.w(TAG, "Do not support Screen Down sensor!!!");
        } else {
            readyToTurnoverOrByScreenDownSensor();
        }
    }

    private void readyToTurnoverOrByScreenDownSensor() {
        float z = this.mSensorDownValues[0];
        Settings.System.putIntForUser(this.mContext.getContentResolver(), KEY_SUB_SCREEN_FACE_UP, (int) z, -2);
        if (this.mIsTurnOverMuteEnabled) {
            if (z == 2.0d && !this.mIsLastStateOn) {
                turnOverMute(true);
            } else if (z == 1.0d && this.mIsLastStateOn) {
                turnOverMute(false);
            }
        }
    }

    private void readyToTurnoverOrHandonByGravity() {
        float z = this.mGravityValues[2];
        if (this.mIsTurnOverMuteEnabled) {
            if (z < -6.0d && !this.mIsLastStateOn) {
                turnOverMute(true);
            } else if (z > 4.0d && this.mIsLastStateOn) {
                turnOverMute(false);
            }
        }
    }

    @Override // android.hardware.SensorEventListener
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void startSensor() {
        if (this.mIsTurnOverMuteEnabled) {
            this.mScreendownSensor = this.mSensorManager.getDefaultSensor(SENSOR_SCREEN_DOWN, true);
            Sensor defaultSensor = this.mSensorManager.getDefaultSensor(9);
            this.mGravitySensor = defaultSensor;
            if (this.mScreendownSensor != null) {
                Log.d(TAG, "mScreendownSensor");
                this.mSensorManager.registerListener(this, this.mScreendownSensor, 0);
            } else if (defaultSensor != null) {
                this.mSensorManager.registerListener(this, defaultSensor, 3);
            } else {
                this.mGyroscopeTimestamp = MiuiFreeformPinManagerService.EDGE_AREA;
                this.mRadians = new float[3];
                this.mAccelerometerSensor = this.mSensorManager.getDefaultSensor(1);
                this.mLinearAccSensor = this.mSensorManager.getDefaultSensor(10);
                this.mGyroscopeSensor = this.mSensorManager.getDefaultSensor(4);
                this.mSensorManager.registerListener(this, this.mAccelerometerSensor, 0);
                this.mSensorManager.registerListener(this, this.mLinearAccSensor, 0);
                this.mSensorManager.registerListener(this, this.mGyroscopeSensor, 3);
            }
            this.mListeningSensor = true;
        }
    }

    private void stopSensor() {
        this.mSensorManager.unregisterListener(this);
    }

    private void turnOverMute(boolean isOnSubScreen) {
        try {
            this.mIsLastStateOn = isOnSubScreen;
            Log.d(TAG, "Set SubScreen On: " + isOnSubScreen);
            int code = isOnSubScreen ? CODE_TURN_ON_SUB_DISPLAY : CODE_TURN_OFF_SUB_DISPLAY;
            turnOnOrOFFSubDisplay(code, "TURN_OVER");
        } catch (Exception e) {
            Log.d(TAG, "close sub display manager service connect fail!");
        }
    }

    private void turnOnOrOFFSubDisplay(int code, String reason) {
        if (isUserSetupComplete() && this.mIPowerManager != null) {
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            data.writeInterfaceToken("android.os.IPowerManager");
            data.writeLong(SystemClock.uptimeMillis());
            data.writeString(reason);
            try {
                try {
                    this.mIPowerManager.transact(code, data, reply, 1);
                } catch (RemoteException ex) {
                    Log.e(TAG, "Failed to wake up secondary display", ex);
                }
            } finally {
                data.recycle();
                reply.recycle();
            }
        }
    }

    private boolean isUserSetupComplete() {
        boolean z = true;
        if (this.mIsUserSetupComplete) {
            return true;
        }
        if (Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "user_setup_complete", 0, -2) == 0) {
            z = false;
        }
        this.mIsUserSetupComplete = z;
        return z;
    }

    public void updateSubSwitchMode() {
        boolean z = true;
        if (Settings.System.getIntForUser(this.mContext.getContentResolver(), "subscreen_switch", 0, -2) != 1) {
            z = false;
        }
        this.mIsTurnOverMuteEnabled = z;
        if (z) {
            startSensor();
            return;
        }
        stopSensor();
        turnOverMute(false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class SettingsObserver extends ContentObserver {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public SettingsObserver(Handler handler) {
            super(handler);
            DisplayTurnoverManager.this = r1;
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            DisplayTurnoverManager.this.updateSubSwitchMode();
        }
    }
}
