package com.android.server.input.pocketmode;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Message;
import android.util.Slog;
/* loaded from: classes.dex */
public class MiuiPocketModeSensorWrapper {
    public static final int DEFAULT_SENSOR_STATE = -1;
    public static final int EVENT_FAR = 1;
    public static final int EVENT_TOO_CLOSE = 0;
    private static final float PROXIMITY_THRESHOLD = 4.0f;
    public static final int STATE_STABLE_DELAY = 300;
    private static final String TAG = "MiuiPocketModeSensorWrapper";
    private Sensor mDefaultSensor;
    private final SensorEventListener mDefaultSensorListener = new SensorEventListener() { // from class: com.android.server.input.pocketmode.MiuiPocketModeSensorWrapper.1
        @Override // android.hardware.SensorEventListener
        public void onSensorChanged(SensorEvent sensorEvent) {
            float distance = sensorEvent.values[0];
            boolean isTooClose = ((double) distance) >= 0.0d && distance < MiuiPocketModeSensorWrapper.PROXIMITY_THRESHOLD && distance < MiuiPocketModeSensorWrapper.this.getSensor().getMaximumRange();
            Slog.d(MiuiPocketModeSensorWrapper.TAG, "proximity distance: " + distance);
            if (isTooClose) {
                if (MiuiPocketModeSensorWrapper.this.mSensorState != 1) {
                    MiuiPocketModeSensorWrapper.this.mSensorState = 1;
                    MiuiPocketModeSensorWrapper.this.mHandler.removeMessages(1);
                    MiuiPocketModeSensorWrapper.this.mHandler.sendEmptyMessageDelayed(0, MiuiPocketModeSensorWrapper.this.getStateStableDelay());
                }
            } else if (MiuiPocketModeSensorWrapper.this.mSensorState != 0) {
                MiuiPocketModeSensorWrapper.this.mSensorState = 0;
                MiuiPocketModeSensorWrapper.this.mHandler.removeMessages(0);
                MiuiPocketModeSensorWrapper.this.mHandler.sendEmptyMessageDelayed(1, MiuiPocketModeSensorWrapper.this.getStateStableDelay());
            }
        }

        @Override // android.hardware.SensorEventListener
        public void onAccuracyChanged(Sensor sensor, int i) {
        }
    };
    protected Handler mHandler;
    private ProximitySensorChangeListener mProximitySensorChangeListener;
    private SensorEventListener mSensorEventListener;
    protected SensorManager mSensorManager;
    protected int mSensorState;

    /* loaded from: classes.dex */
    public interface ProximitySensorChangeListener {
        void onSensorChanged(boolean z);
    }

    public MiuiPocketModeSensorWrapper(Context context, SensorEventListener sensorEventListener) {
        init(context, sensorEventListener);
    }

    public MiuiPocketModeSensorWrapper(Context context) {
        init(context, null);
    }

    private void init(Context context, SensorEventListener sensorEventListener) {
        this.mSensorState = -1;
        this.mSensorManager = (SensorManager) context.getSystemService("sensor");
        this.mHandler = new Handler() { // from class: com.android.server.input.pocketmode.MiuiPocketModeSensorWrapper.2
            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 0:
                        MiuiPocketModeSensorWrapper.this.notifyListener(true);
                        return;
                    case 1:
                        MiuiPocketModeSensorWrapper.this.notifyListener(false);
                        return;
                    default:
                        return;
                }
            }
        };
        if (sensorEventListener == null) {
            this.mSensorEventListener = this.mDefaultSensorListener;
        } else {
            this.mSensorEventListener = sensorEventListener;
        }
    }

    public void notifyListener(boolean tooClose) {
        synchronized (this) {
            ProximitySensorChangeListener proximitySensorChangeListener = this.mProximitySensorChangeListener;
            if (proximitySensorChangeListener != null) {
                proximitySensorChangeListener.onSensorChanged(tooClose);
            }
        }
    }

    public void registerListener(ProximitySensorChangeListener listener) {
        synchronized (this) {
            if (listener != null) {
                this.mSensorState = -1;
                this.mProximitySensorChangeListener = listener;
                this.mSensorManager.registerListener(this.mSensorEventListener, getSensor(), 0);
            }
        }
    }

    public void unregisterListener() {
        synchronized (this) {
            this.mSensorManager.unregisterListener(this.mSensorEventListener, getSensor());
            this.mProximitySensorChangeListener = null;
        }
    }

    protected Sensor getSensor() {
        if (this.mDefaultSensor == null) {
            this.mDefaultSensor = this.mSensorManager.getDefaultSensor(8);
        }
        return this.mDefaultSensor;
    }

    public int getStateStableDelay() {
        return STATE_STABLE_DELAY;
    }
}
