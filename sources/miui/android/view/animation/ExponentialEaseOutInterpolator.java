package miui.android.view.animation;

import android.view.animation.Interpolator;
/* loaded from: classes.dex */
public class ExponentialEaseOutInterpolator implements Interpolator {
    @Override // android.animation.TimeInterpolator
    public float getInterpolation(float t) {
        if (t == 1.0f) {
            return 1.0f;
        }
        return (float) ((-Math.pow(2.0d, (-10.0f) * t)) + 1.0d);
    }
}
