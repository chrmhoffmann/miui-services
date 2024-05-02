package com.android.server.lights.interpolater;

import android.view.animation.Interpolator;
/* loaded from: classes.dex */
public class ExpoEaseOutInterpolater implements Interpolator {
    @Override // android.animation.TimeInterpolator
    public float getInterpolation(float input) {
        if (input == 1.0f) {
            return 1.0f;
        }
        return (float) ((-Math.pow(2.0d, (-10.0f) * input)) + 1.0d);
    }
}
