package miui.android.view.animation;

import android.view.animation.Interpolator;
/* loaded from: classes.dex */
public class CirclularEaseInInterpolator implements Interpolator {
    @Override // android.animation.TimeInterpolator
    public float getInterpolation(float t) {
        return -((float) (Math.sqrt(1.0f - (t * t)) - 1.0d));
    }
}
