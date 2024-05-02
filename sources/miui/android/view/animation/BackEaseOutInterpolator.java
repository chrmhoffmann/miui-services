package miui.android.view.animation;

import android.view.animation.Interpolator;
import com.android.server.wm.MiuiFreeformPinManagerService;
/* loaded from: classes.dex */
public class BackEaseOutInterpolator implements Interpolator {
    private final float mOvershot;

    public BackEaseOutInterpolator() {
        this(MiuiFreeformPinManagerService.EDGE_AREA);
    }

    public BackEaseOutInterpolator(float overshot) {
        this.mOvershot = overshot;
    }

    @Override // android.animation.TimeInterpolator
    public float getInterpolation(float t) {
        float s = this.mOvershot;
        if (s == MiuiFreeformPinManagerService.EDGE_AREA) {
            s = 1.70158f;
        }
        float t2 = t - 1.0f;
        return (t2 * t2 * (((s + 1.0f) * t2) + s)) + 1.0f;
    }
}
