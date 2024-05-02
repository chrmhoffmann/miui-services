package com.android.server.input.config;

import android.os.Parcel;
/* loaded from: classes.dex */
public class InputCommonConfig extends BaseInputConfig {
    public static final int CONFIG_TYPE = 2;
    private static volatile InputCommonConfig instance;
    private boolean mCtsMode;
    private boolean mCustomized;
    private boolean mInjectEventStatus;
    private boolean mMouseNaturalScroll;
    private boolean mOnewayMode;
    private boolean mPadMode;
    private int mProductId;
    private boolean mRecordEventStatus;
    private boolean mShown;
    private int mSynergyMode;
    private int mVendorId;
    private int mWakeUpMode;

    private InputCommonConfig() {
    }

    public static InputCommonConfig getInstance() {
        if (instance == null) {
            synchronized (InputCommonConfig.class) {
                if (instance == null) {
                    instance = new InputCommonConfig();
                }
            }
        }
        return instance;
    }

    @Override // com.android.server.input.config.BaseInputConfig
    protected void writeToParcel(Parcel dest) {
        dest.writeInt(this.mWakeUpMode);
        dest.writeInt(this.mSynergyMode);
        dest.writeBoolean(this.mInjectEventStatus);
        dest.writeBoolean(this.mRecordEventStatus);
        dest.writeBoolean(this.mShown);
        dest.writeBoolean(this.mCustomized);
        dest.writeBoolean(this.mPadMode);
        dest.writeBoolean(this.mMouseNaturalScroll);
        dest.writeInt(this.mProductId);
        dest.writeInt(this.mVendorId);
        dest.writeBoolean(this.mOnewayMode);
        dest.writeBoolean(this.mCtsMode);
    }

    @Override // com.android.server.input.config.BaseInputConfig
    public int getConfigType() {
        return 2;
    }

    public void setWakeUpMode(int wakeUpMode) {
        this.mWakeUpMode = wakeUpMode;
    }

    public void setSynergyMode(int synergyMode) {
        this.mSynergyMode = synergyMode;
    }

    public void setInjectEventStatus(boolean injectEventStatus) {
        this.mInjectEventStatus = injectEventStatus;
    }

    public void setRecordEventStatus(boolean recordEventStatus) {
        this.mRecordEventStatus = recordEventStatus;
    }

    public void setInputMethodStatus(boolean shown, boolean customized) {
        this.mShown = shown;
        this.mCustomized = customized;
    }

    public void setPadMode(boolean padMode) {
        this.mPadMode = padMode;
    }

    public void setMouseNaturalScrollStatus(boolean mouseNaturalScroll) {
        this.mMouseNaturalScroll = mouseNaturalScroll;
    }

    public void setMiuiKeyboardInfo(int vendorId, int productId) {
        this.mVendorId = vendorId;
        this.mProductId = productId;
    }

    public void setOnewayMode(boolean onewayMode) {
        this.mOnewayMode = onewayMode;
    }

    public void setCtsMode(boolean ctsMode) {
        this.mCtsMode = ctsMode;
    }
}
