package miui.android.view.animation;

import android.view.animation.Interpolator;
/* loaded from: classes.dex */
public class BounceEaseInInterpolator implements Interpolator {
    @Override // android.animation.TimeInterpolator
    public float getInterpolation(float t) {
        return 1.0f - new BounceEaseOutInterpolator().getInterpolation(1.0f - t);
    }
}
