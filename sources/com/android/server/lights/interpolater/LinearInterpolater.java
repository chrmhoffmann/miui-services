package com.android.server.lights.interpolater;

import android.view.animation.Interpolator;
/* loaded from: classes.dex */
public class LinearInterpolater implements Interpolator {
    @Override // android.animation.TimeInterpolator
    public float getInterpolation(float input) {
        return input;
    }
}
