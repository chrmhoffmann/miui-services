package miui.android.view.animation;

import android.view.animation.Interpolator;
/* loaded from: classes.dex */
public class BounceEaseInOutInterpolator implements Interpolator {
    @Override // android.animation.TimeInterpolator
    public float getInterpolation(float t) {
        return t < 0.5f ? new BounceEaseInInterpolator().getInterpolation(2.0f * t) * 0.5f : (new BounceEaseOutInterpolator().getInterpolation((2.0f * t) - 1.0f) * 0.5f) + 0.5f;
    }
}
