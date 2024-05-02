package com.android.server.wm;

import android.view.animation.Interpolator;
import miui.os.Build;
/* loaded from: classes.dex */
public class MiuiRotationAnimationUtils {
    private static final int DEVICE_HIGH_END = 3;
    private static final int DEVICE_LOW_END = 1;
    private static final int DEVICE_MIDDLE_END = 2;
    private static final int HIGH_ROTATION_ANIMATION_DURATION = 500;
    private static final int LOW_ROTATION_ANIMATION_DURATION = 300;
    private static final int MIDDLE_ROTATION_ANIMATION_DURATION = 500;
    private static final String TAG = "MiuiRotationAnimationUtil";

    public static int getRotationDuration() {
        if (Build.IS_MIUI_LITE_VERSION) {
            return 300;
        }
        int devicelevel = Build.getDeviceLevelForAnimation();
        switch (devicelevel) {
            case 1:
                return 300;
            case 2:
                return 500;
            case 3:
                return 500;
            default:
                return 500;
        }
    }

    /* loaded from: classes.dex */
    public static class SineEaseInInterpolater implements Interpolator {
        @Override // android.animation.TimeInterpolator
        public float getInterpolation(float t) {
            return (-((float) Math.cos(t * 1.5707963267948966d))) + 1.0f;
        }
    }

    /* loaded from: classes.dex */
    public static class SineEaseOutInterpolater implements Interpolator {
        @Override // android.animation.TimeInterpolator
        public float getInterpolation(float t) {
            return (float) Math.sin(t * 1.5707963267948966d);
        }
    }

    /* loaded from: classes.dex */
    public static class EaseQuartOutInterpolator implements Interpolator {
        @Override // android.animation.TimeInterpolator
        public float getInterpolation(float input) {
            return 1.0f - ((((input - 1.0f) * (input - 1.0f)) * (input - 1.0f)) * (input - 1.0f));
        }
    }

    /* loaded from: classes.dex */
    public static class EaseSineInOutInterpolator implements Interpolator {
        @Override // android.animation.TimeInterpolator
        public float getInterpolation(float input) {
            return (((float) Math.cos((input * 3.141592653589793d) / 1.0d)) - 1.0f) * (-0.5f);
        }
    }
}
