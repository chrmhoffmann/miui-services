package com.android.server.lights.interpolater;

import android.view.animation.Interpolator;
/* loaded from: classes.dex */
public class SineEaseInOutInterpolater implements Interpolator {
    @Override // android.animation.TimeInterpolator
    public float getInterpolation(float input) {
        return ((float) (Math.cos(input * 3.141592653589793d) - 1.0d)) * (-0.5f);
    }
}
