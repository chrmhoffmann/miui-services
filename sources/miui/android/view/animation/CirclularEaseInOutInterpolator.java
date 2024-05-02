package miui.android.view.animation;

import android.view.animation.Interpolator;
/* loaded from: classes.dex */
public class CirclularEaseInOutInterpolator implements Interpolator {
    @Override // android.animation.TimeInterpolator
    public float getInterpolation(float t) {
        float t2 = t * 2.0f;
        if (t2 < 1.0f) {
            return ((float) (Math.sqrt(1.0f - (t2 * t2)) - 1.0d)) * (-0.5f);
        }
        float t3 = t2 - 2.0f;
        return ((float) (Math.sqrt(1.0f - (t3 * t3)) + 1.0d)) * 0.5f;
    }
}
