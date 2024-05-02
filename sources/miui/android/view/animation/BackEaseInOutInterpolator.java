package miui.android.view.animation;

import android.view.animation.Interpolator;
import com.android.server.wm.MiuiFreeformPinManagerService;
/* loaded from: classes.dex */
public class BackEaseInOutInterpolator implements Interpolator {
    private final float mOvershot;

    public BackEaseInOutInterpolator() {
        this(MiuiFreeformPinManagerService.EDGE_AREA);
    }

    public BackEaseInOutInterpolator(float overshot) {
        this.mOvershot = overshot;
    }

    @Override // android.animation.TimeInterpolator
    public float getInterpolation(float t) {
        float s = this.mOvershot;
        if (s == MiuiFreeformPinManagerService.EDGE_AREA) {
            s = 1.70158f;
        }
        float t2 = t * 2.0f;
        if (t2 < 1.0f) {
            float s2 = (float) (s * 1.525d);
            return t2 * t2 * (((1.0f + s2) * t2) - s2) * 0.5f;
        }
        float t3 = t2 - 2.0f;
        float s3 = (float) (s * 1.525d);
        return ((t3 * t3 * (((1.0f + s3) * t3) + s3)) + 2.0f) * 0.5f;
    }
}
