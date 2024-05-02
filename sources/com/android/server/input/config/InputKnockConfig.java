package com.android.server.input.config;

import android.os.Parcel;
/* loaded from: classes.dex */
public class InputKnockConfig extends BaseInputConfig {
    public static final int CONFIG_TYPE = 3;
    private static volatile InputKnockConfig mInstance;
    private int mKnockDeviceProperty;
    private int mKnockFeatureState;
    private int mKnockInValidRectBottom;
    private int mKnockInValidRectLeft;
    private int mKnockInValidRectRight;
    private int mKnockInValidRectTop;
    private float mKnockScoreThreshold;
    private int mKnockValidRectBottom;
    private int mKnockValidRectLeft;
    private int mKnockValidRectRight;
    private int mKnockValidRectTop;
    private float mQuickMoveSpeed;
    private int mUseFrame;
    private String mKnockAlgorithmPath = "";
    private int[] mSensorThreshold = {0, 0, 0, 0};

    private InputKnockConfig() {
    }

    public static InputKnockConfig getInstance() {
        if (mInstance == null) {
            synchronized (InputKnockConfig.class) {
                if (mInstance == null) {
                    mInstance = new InputKnockConfig();
                }
            }
        }
        return mInstance;
    }

    @Override // com.android.server.input.config.BaseInputConfig
    public void writeToParcel(Parcel dest) {
        dest.writeInt(this.mKnockFeatureState);
        dest.writeInt(this.mKnockDeviceProperty);
        dest.writeInt(this.mKnockValidRectLeft);
        dest.writeInt(this.mKnockValidRectTop);
        dest.writeInt(this.mKnockValidRectRight);
        dest.writeInt(this.mKnockValidRectBottom);
        dest.writeInt(this.mKnockInValidRectLeft);
        dest.writeInt(this.mKnockInValidRectTop);
        dest.writeInt(this.mKnockInValidRectRight);
        dest.writeInt(this.mKnockInValidRectBottom);
        dest.writeString8(this.mKnockAlgorithmPath);
        dest.writeFloat(this.mKnockScoreThreshold);
        dest.writeInt(this.mUseFrame);
        dest.writeFloat(this.mQuickMoveSpeed);
        dest.writeIntArray(this.mSensorThreshold);
    }

    @Override // com.android.server.input.config.BaseInputConfig
    public int getConfigType() {
        return 3;
    }

    public void setKnockFeatureState(int state) {
        this.mKnockFeatureState = state;
    }

    public void setKnockDeviceProperty(int property) {
        this.mKnockDeviceProperty = property;
    }

    public void setKnockValidRect(int left, int top, int right, int bottom) {
        this.mKnockValidRectLeft = left;
        this.mKnockValidRectTop = top;
        this.mKnockValidRectRight = right;
        this.mKnockValidRectBottom = bottom;
    }

    public void setKnockInValidRect(int left, int top, int right, int bottom) {
        this.mKnockInValidRectLeft = left;
        this.mKnockInValidRectTop = top;
        this.mKnockInValidRectRight = right;
        this.mKnockInValidRectBottom = bottom;
    }

    public void updateAlgorithmPath(String filePath) {
        this.mKnockAlgorithmPath = filePath;
    }

    public void setKnockScoreThreshold(float score) {
        this.mKnockScoreThreshold = score;
    }

    public void setKnockUseFrame(int useFrame) {
        this.mUseFrame = useFrame;
    }

    public void setKnockQuickMoveSpeed(float quickMoveSpeed) {
        this.mQuickMoveSpeed = quickMoveSpeed;
    }

    public void setKnockSensorThreshold(int[] sensorThreshold) {
        this.mSensorThreshold = sensorThreshold;
    }
}
