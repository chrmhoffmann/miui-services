package com.android.server.lights;

import android.icu.text.SimpleDateFormat;
import android.text.TextUtils;
import java.util.Date;
/* loaded from: classes.dex */
public class LightState {
    public int brightnessMode;
    public String callingPackage;
    public int colorARGB;
    public int flashMode;
    public long mAddedTime;
    public int mId;
    public int offMS;
    public int onMS;
    public int styleType;

    public LightState(int colorARGB, int flashMode, int onMS, int offMS, int brightnessMode) {
        this.colorARGB = colorARGB;
        this.flashMode = flashMode;
        this.onMS = onMS;
        this.offMS = offMS;
        this.brightnessMode = brightnessMode;
    }

    public LightState(int mId, int colorARGB, int flashMode, int onMS, int offMS, int brightnessMode) {
        this.mAddedTime = System.currentTimeMillis();
        this.mId = mId;
        this.colorARGB = colorARGB;
        this.flashMode = flashMode;
        this.onMS = onMS;
        this.offMS = offMS;
        this.brightnessMode = brightnessMode;
    }

    public LightState(String callingPackage, int styleType) {
        this.mAddedTime = System.currentTimeMillis();
        this.styleType = styleType;
        this.callingPackage = callingPackage;
    }

    public String toString() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String date = formatter.format(new Date(this.mAddedTime));
        StringBuilder builder = new StringBuilder();
        builder.append(", mAddedTime=").append(date);
        if (!TextUtils.isEmpty(this.callingPackage)) {
            builder.append(", callingPackage=").append(this.callingPackage);
        }
        if (this.mId != 0) {
            builder.append(", mId=").append(this.mId);
        }
        if (this.styleType != 0) {
            builder.append(", mLastLightStyle=").append(this.styleType);
        }
        if (this.colorARGB != 0) {
            builder.append(", colorARGB=").append(this.colorARGB);
        }
        if (this.onMS != 0) {
            builder.append(", onMS=").append(this.onMS);
        }
        if (this.offMS != 0) {
            builder.append(", offMS=").append(this.offMS);
        }
        if (this.flashMode != 0) {
            builder.append(", flashMode=").append(this.flashMode);
        }
        if (this.brightnessMode != 0) {
            builder.append(", brightnessMode=").append(this.brightnessMode);
        }
        return builder.toString();
    }
}
