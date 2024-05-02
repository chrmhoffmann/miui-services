package com.android.server.lights.interpolater;

import android.view.animation.Interpolator;
/* loaded from: classes.dex */
public class SineEaseInInterpolater implements Interpolator {
    @Override // android.animation.TimeInterpolator
    public float getInterpolation(float input) {
        return (-((float) Math.cos(input * 1.5707963267948966d))) + 1.0f;
    }
}
