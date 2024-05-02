package miui.android.view.animation;

import android.view.animation.Interpolator;
/* loaded from: classes.dex */
public class CubicEaseInOutInterpolator implements Interpolator {
    @Override // android.animation.TimeInterpolator
    public float getInterpolation(float t) {
        float t2 = t * 2.0f;
        if (t2 < 1.0f) {
            return 0.5f * t2 * t2 * t2;
        }
        float t3 = t2 - 2.0f;
        return ((t3 * t3 * t3) + 2.0f) * 0.5f;
    }
}
