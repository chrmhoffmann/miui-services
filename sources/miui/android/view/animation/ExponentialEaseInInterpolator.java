package miui.android.view.animation;

import android.view.animation.Interpolator;
import com.android.server.wm.MiuiFreeformPinManagerService;
/* loaded from: classes.dex */
public class ExponentialEaseInInterpolator implements Interpolator {
    @Override // android.animation.TimeInterpolator
    public float getInterpolation(float t) {
        return t == MiuiFreeformPinManagerService.EDGE_AREA ? MiuiFreeformPinManagerService.EDGE_AREA : (float) Math.pow(2.0d, (t - 1.0f) * 10.0f);
    }
}
