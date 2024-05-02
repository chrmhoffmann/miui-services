package miui.android.view.animation;

import android.view.animation.Interpolator;
/* loaded from: classes.dex */
public class SineEaseInInterpolator implements Interpolator {
    @Override // android.animation.TimeInterpolator
    public float getInterpolation(float t) {
        return (-((float) Math.cos(t * 1.5707963267948966d))) + 1.0f;
    }
}
