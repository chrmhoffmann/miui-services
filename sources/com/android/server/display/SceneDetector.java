package com.android.server.display;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Slog;
import com.android.server.wm.MiuiFreeformPinManagerService;
import java.io.PrintWriter;
/* loaded from: classes.dex */
public class SceneDetector {
    private static final float HIGH_CONFIDENCE_STATUS = 3.0f;
    private static final long LONG_DARKENING_DELAY_TIME = 2000;
    private static final float LOW_CONFIDENCE_STATUS = 5.0f;
    private static final float MEDIUM_CONFIDENCE_STATUS = 4.0f;
    private static final int POINT_LIGHT_SOURCE_DETECTOR = 33171039;
    private static final int POINT_LIGHT_SOURCE_SENSOR_VERSION = 2;
    private static final long SHORT_DARKENING_DELAY_TIME = 1000;
    private static final String TAG = "SceneDetector";
    private AutomaticBrightnessControllerImpl mBrightnessControllerImpl;
    private Context mContext;
    private Handler mDetectorHandler;
    private Sensor mPointLightSourceDetector;
    private boolean mPointLightSourceDetectorEnable;
    private SensorEventListener mPointLightSourceDetectorListener = new SensorEventListener() { // from class: com.android.server.display.SceneDetector.1
        @Override // android.hardware.SensorEventListener
        public void onSensorChanged(SensorEvent event) {
            SceneDetector.this.updateDetectorStatus(event);
        }

        @Override // android.hardware.SensorEventListener
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
    private float mPointLightSourceStatus;
    private float mPrePointLightSourceStatus;
    private SensorManager mSensorManager;

    public SceneDetector(Looper looper, SensorManager sensorManager, AutomaticBrightnessControllerImpl BrightnessControllerImpl, Context context) {
        this.mSensorManager = sensorManager;
        this.mPointLightSourceDetector = sensorManager.getDefaultSensor(POINT_LIGHT_SOURCE_DETECTOR);
        this.mDetectorHandler = new Handler(looper);
        this.mBrightnessControllerImpl = BrightnessControllerImpl;
        this.mContext = context;
    }

    public void setSensorEnable(boolean enable) {
        if (enable && !this.mPointLightSourceDetectorEnable) {
            Slog.i(TAG, "setSensorEnable: register point light source detector.");
            this.mPointLightSourceDetectorEnable = true;
            this.mSensorManager.registerListener(this.mPointLightSourceDetectorListener, this.mPointLightSourceDetector, 3, this.mDetectorHandler);
        } else if (!enable && this.mPointLightSourceDetectorEnable) {
            Slog.i(TAG, "setSensorEnable: unregister point light source detector.");
            this.mPointLightSourceDetectorEnable = false;
            this.mPointLightSourceStatus = MiuiFreeformPinManagerService.EDGE_AREA;
            this.mPrePointLightSourceStatus = MiuiFreeformPinManagerService.EDGE_AREA;
            this.mSensorManager.unregisterListener(this.mPointLightSourceDetectorListener, this.mPointLightSourceDetector);
            AutomaticBrightnessControllerImpl automaticBrightnessControllerImpl = this.mBrightnessControllerImpl;
            if (automaticBrightnessControllerImpl != null) {
                automaticBrightnessControllerImpl.setSuppressDarkeningEnable(false);
                this.mBrightnessControllerImpl.notifyAdjustDarkeningDebounce(-1L);
            }
        }
    }

    public void updateDetectorStatus(SensorEvent event) {
        int version = event.sensor.getVersion();
        switch (version) {
            case 2:
                float f = event.values[0];
                this.mPointLightSourceStatus = f;
                if (f != this.mPrePointLightSourceStatus) {
                    this.mPrePointLightSourceStatus = f;
                    handleDarkeningDelayTime();
                    Slog.i(TAG, "updateDetectorStatus: point light source status changed, mPointLightSourceStatus: " + this.mPointLightSourceStatus);
                    return;
                }
                return;
            default:
                return;
        }
    }

    long getDarkeningDelayTime() {
        float f = this.mPointLightSourceStatus;
        if (f == HIGH_CONFIDENCE_STATUS) {
            return Long.MAX_VALUE;
        }
        if (f == MEDIUM_CONFIDENCE_STATUS) {
            return LONG_DARKENING_DELAY_TIME;
        }
        if (f == LOW_CONFIDENCE_STATUS) {
            return SHORT_DARKENING_DELAY_TIME;
        }
        return 0L;
    }

    private void handleDarkeningDelayTime() {
        if (this.mBrightnessControllerImpl != null) {
            long time = getDarkeningDelayTime();
            if (time != Long.MAX_VALUE) {
                this.mBrightnessControllerImpl.setSuppressDarkeningEnable(false);
                this.mBrightnessControllerImpl.notifyAdjustDarkeningDebounce(time);
                return;
            }
            this.mBrightnessControllerImpl.setSuppressDarkeningEnable(true);
            this.mBrightnessControllerImpl.notifyAdjustDarkeningDebounce(0L);
        }
    }

    public void dump(PrintWriter pw) {
        pw.println();
        pw.println("Scene Detector State:");
        pw.println("  mPrePointLightSourceStatus: " + this.mPrePointLightSourceStatus);
        pw.println("  mPointLightSourceStatus: " + this.mPointLightSourceStatus);
        if (this.mBrightnessControllerImpl != null) {
            pw.println("  mSuppressDarkeningEnable: " + this.mBrightnessControllerImpl.getSuppressDarkeningEnable());
            pw.println("  mMainDarkeningDelayTime: " + this.mBrightnessControllerImpl.getMainDarkeningDelayTime());
            pw.println("  mAssistDarkeningDelayTime: " + this.mBrightnessControllerImpl.getAssistDarkeningDelayTime());
        }
    }
}
