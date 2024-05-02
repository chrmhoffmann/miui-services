package miui.android.view.animation;

import android.view.animation.Interpolator;
/* loaded from: classes.dex */
public class CubicEaseInInterpolator implements Interpolator {
    @Override // android.animation.TimeInterpolator
    public float getInterpolation(float t) {
        return t * t * t;
    }
}
