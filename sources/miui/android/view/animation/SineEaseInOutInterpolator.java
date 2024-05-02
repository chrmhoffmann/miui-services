package miui.android.view.animation;

import android.view.animation.Interpolator;
/* loaded from: classes.dex */
public class SineEaseInOutInterpolator implements Interpolator {
    @Override // android.animation.TimeInterpolator
    public float getInterpolation(float t) {
        return ((float) (Math.cos(t * 3.141592653589793d) - 1.0d)) * (-0.5f);
    }
}
