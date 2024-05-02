package miui.android.view.animation;

import android.view.animation.Interpolator;
import com.android.server.wm.MiuiFreeformPinManagerService;
/* loaded from: classes.dex */
public class BackEaseInInterpolator implements Interpolator {
    private final float mOvershot;

    public BackEaseInInterpolator() {
        this(MiuiFreeformPinManagerService.EDGE_AREA);
    }

    public BackEaseInInterpolator(float overshot) {
        this.mOvershot = overshot;
    }

    @Override // android.animation.TimeInterpolator
    public float getInterpolation(float t) {
        float s = this.mOvershot;
        if (s == MiuiFreeformPinManagerService.EDGE_AREA) {
            s = 1.70158f;
        }
        return t * t * (((1.0f + s) * t) - s);
    }
}
