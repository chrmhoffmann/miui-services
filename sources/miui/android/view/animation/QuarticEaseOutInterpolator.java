package miui.android.view.animation;

import android.view.animation.Interpolator;
/* loaded from: classes.dex */
public class QuarticEaseOutInterpolator implements Interpolator {
    @Override // android.animation.TimeInterpolator
    public float getInterpolation(float t) {
        float t2 = t - 1.0f;
        return -((((t2 * t2) * t2) * t2) - 1.0f);
    }
}
