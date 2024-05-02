package miui.android.view.animation;

import android.view.animation.Interpolator;
/* loaded from: classes.dex */
public class CirclularEaseOutInterpolator implements Interpolator {
    @Override // android.animation.TimeInterpolator
    public float getInterpolation(float t) {
        float t2 = t - 1.0f;
        return (float) Math.sqrt(1.0f - (t2 * t2));
    }
}
