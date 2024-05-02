package miui.android.view.animation;

import android.view.animation.Interpolator;
import com.android.server.wm.MiuiFreeformPinManagerService;
/* loaded from: classes.dex */
public class ExponentialEaseInOutInterpolator implements Interpolator {
    @Override // android.animation.TimeInterpolator
    public float getInterpolation(float t) {
        if (t == MiuiFreeformPinManagerService.EDGE_AREA) {
            return MiuiFreeformPinManagerService.EDGE_AREA;
        }
        if (t == 1.0f) {
            return 1.0f;
        }
        float t2 = t * 2.0f;
        if (t2 < 1.0f) {
            return ((float) Math.pow(2.0d, (t2 - 1.0f) * 10.0f)) * 0.5f;
        }
        return ((float) ((-Math.pow(2.0d, (-10.0f) * (t2 - 1.0f))) + 2.0d)) * 0.5f;
    }
}
