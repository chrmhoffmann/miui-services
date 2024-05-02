package com.android.server.input.pocketmode;

import android.content.Context;
import android.hardware.Sensor;
/* loaded from: classes.dex */
public class MiuiPocketModeInertialAndLightSensor extends MiuiPocketModeSensorWrapper {
    public static final int SENSOR_TYPE_INERTIAL_AND_LIGHT = 33171095;
    private Sensor mInertialLightSensor;

    public MiuiPocketModeInertialAndLightSensor(Context context) {
        super(context);
    }

    @Override // com.android.server.input.pocketmode.MiuiPocketModeSensorWrapper
    protected Sensor getSensor() {
        if (this.mInertialLightSensor == null) {
            this.mInertialLightSensor = this.mSensorManager.getDefaultSensor(SENSOR_TYPE_INERTIAL_AND_LIGHT);
        }
        return this.mInertialLightSensor;
    }

    @Override // com.android.server.input.pocketmode.MiuiPocketModeSensorWrapper
    public int getStateStableDelay() {
        return 0;
    }
}
