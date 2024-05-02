package miui.android.view.animation;

import android.view.animation.Interpolator;
/* loaded from: classes.dex */
public class SineEaseOutInterpolator implements Interpolator {
    @Override // android.animation.TimeInterpolator
    public float getInterpolation(float t) {
        return (float) Math.sin(t * 1.5707963267948966d);
    }
}
