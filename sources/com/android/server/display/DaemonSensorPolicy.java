package com.android.server.display;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.util.Slog;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import miui.util.FeatureParser;
/* loaded from: classes.dex */
public class DaemonSensorPolicy {
    private static final int ASSIST_SENSOR_TYPE = 33171055;
    private static final int MSG_UPDATE_DEVICE_IDLE = 1;
    private static final String TAG = DaemonSensorPolicy.class.getSimpleName();
    private static final boolean USE_DAEMON_SENSOR_POLICY = FeatureParser.getBoolean("use_daemon_sensor_policy", true);
    private AutomaticBrightnessControllerImpl mAutomaticBrightnessControllerImpl;
    private final Context mContext;
    private boolean mDaemonLightSensorsEnabled;
    private Map<Integer, Float> mDaemonSensorValues;
    private List<Sensor> mDaemonSensors;
    private int mDisplayPolicy;
    private DaemonSensorHandle mHandler;
    private boolean mIsDeviceIdleMode;
    private boolean mIsDeviceIdleReceiverRegistered;
    private final Sensor mMainLightSensor;
    private final PowerManager mPowerManager;
    private final SensorManager mSensorManager;
    private final SensorEventListener mDaemonSensorListener = new SensorEventListener() { // from class: com.android.server.display.DaemonSensorPolicy.1
        @Override // android.hardware.SensorEventListener
        public void onSensorChanged(SensorEvent event) {
            DaemonSensorPolicy.this.mDaemonSensorValues.put(Integer.valueOf(event.sensor.getType()), Float.valueOf(event.values[0]));
        }

        @Override // android.hardware.SensorEventListener
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
    private BroadcastReceiver mDeviceIdleReceiver = new BroadcastReceiver() { // from class: com.android.server.display.DaemonSensorPolicy.2
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            Slog.d(DaemonSensorPolicy.TAG, "Device idle changed, mDeviceIdle: " + DaemonSensorPolicy.this.mIsDeviceIdleMode);
            DaemonSensorPolicy.this.mHandler.sendEmptyMessage(1);
        }
    };

    public DaemonSensorPolicy(Context context, SensorManager sensorManager, Looper looper, AutomaticBrightnessControllerImpl impl, Sensor lightSensor) {
        this.mContext = context;
        this.mHandler = new DaemonSensorHandle(looper);
        this.mSensorManager = sensorManager;
        this.mPowerManager = (PowerManager) context.getSystemService("power");
        this.mAutomaticBrightnessControllerImpl = impl;
        this.mMainLightSensor = lightSensor;
        addDaemonSensor();
        this.mHandler.post(new Runnable() { // from class: com.android.server.display.DaemonSensorPolicy$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                DaemonSensorPolicy.this.m548lambda$new$0$comandroidserverdisplayDaemonSensorPolicy();
            }
        });
    }

    /* renamed from: registerReceiver */
    public void m548lambda$new$0$comandroidserverdisplayDaemonSensorPolicy() {
        if (USE_DAEMON_SENSOR_POLICY && !this.mIsDeviceIdleReceiverRegistered) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.os.action.DEVICE_IDLE_MODE_CHANGED");
            this.mContext.registerReceiver(this.mDeviceIdleReceiver, intentFilter);
            this.mIsDeviceIdleReceiverRegistered = true;
        }
    }

    private void unregisterReceiver() {
        if (this.mIsDeviceIdleReceiverRegistered) {
            this.mContext.unregisterReceiver(this.mDeviceIdleReceiver);
            this.mIsDeviceIdleReceiverRegistered = false;
        }
    }

    private void addDaemonSensor() {
        this.mDaemonSensorValues = new HashMap();
        ArrayList arrayList = new ArrayList();
        this.mDaemonSensors = arrayList;
        Sensor sensor = this.mMainLightSensor;
        if (sensor != null) {
            arrayList.add(sensor);
        }
        Sensor sensor2 = this.mSensorManager.getDefaultSensor(ASSIST_SENSOR_TYPE);
        if (sensor2 != null) {
            this.mDaemonSensors.add(sensor2);
        }
    }

    public void setDaemonLightSensorsEnabled(boolean enable) {
        if (enable) {
            if (!this.mDaemonLightSensorsEnabled) {
                this.mDaemonLightSensorsEnabled = true;
                for (Sensor sensor : this.mDaemonSensors) {
                    this.mSensorManager.registerListener(this.mDaemonSensorListener, sensor, 0, this.mHandler);
                }
                Slog.i(TAG, "register daemon light sensor.");
            }
        } else if (this.mDaemonLightSensorsEnabled) {
            this.mDaemonLightSensorsEnabled = false;
            this.mSensorManager.unregisterListener(this.mDaemonSensorListener);
            this.mDaemonSensorValues.clear();
            Slog.i(TAG, "unregister daemon light sensor.");
        }
    }

    public float getMainLightSensorLux() {
        Sensor sensor = this.mMainLightSensor;
        if (sensor != null) {
            return getDaemonSensorValue(sensor.getType());
        }
        return Float.NaN;
    }

    public float getDaemonSensorValue(int type) {
        if (!this.mDaemonSensorValues.containsKey(Integer.valueOf(type))) {
            Slog.w(TAG, "the sensor of the type " + type + " is not in daemon sensor list!");
            return Float.NaN;
        }
        return this.mDaemonSensorValues.get(Integer.valueOf(type)).floatValue();
    }

    public void notifyRegisterDaemonLightSensor(int state, int displayPolicy) {
        boolean isActive = state == 3;
        this.mDisplayPolicy = displayPolicy;
        if (isActive) {
            setDaemonLightSensorsEnabled(true);
        }
    }

    public void stop() {
        setDaemonLightSensorsEnabled(false);
        unregisterReceiver();
    }

    public void dump(PrintWriter pw) {
        if (USE_DAEMON_SENSOR_POLICY) {
            pw.println();
            pw.println("Daemon sensor policy state:");
            pw.println("  mDaemonLightSensorsEnabled=" + this.mDaemonLightSensorsEnabled);
            pw.println("  mIsDeviceIdleMode=" + this.mIsDeviceIdleMode);
            pw.println("  mDaemonSensors: size=" + this.mDaemonSensors.size());
            for (Sensor sensor : this.mDaemonSensors) {
                pw.println("      sensor name=" + sensor.getName() + ", type=" + sensor.getType());
            }
            for (Map.Entry<Integer, Float> entry : this.mDaemonSensorValues.entrySet()) {
                int type = entry.getKey().intValue();
                float lux = entry.getValue().floatValue();
                pw.println("      sensor [type: " + type + ", lux=" + lux + "];");
            }
        }
    }

    /* loaded from: classes.dex */
    public class DaemonSensorHandle extends Handler {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public DaemonSensorHandle(Looper looper) {
            super(looper);
            DaemonSensorPolicy.this = this$0;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    DaemonSensorPolicy daemonSensorPolicy = DaemonSensorPolicy.this;
                    daemonSensorPolicy.mIsDeviceIdleMode = daemonSensorPolicy.mPowerManager.isDeviceIdleMode();
                    return;
                default:
                    return;
            }
        }
    }
}
