package miui.android.view.animation;

import android.view.animation.Interpolator;
import com.android.server.wm.MiuiFreeformPinManagerService;
/* loaded from: classes.dex */
public class ElasticEaseInInterpolator implements Interpolator {
    private final float mAmplitude;
    private final float mPeriod;

    public ElasticEaseInInterpolator() {
        this(MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA);
    }

    public ElasticEaseInInterpolator(float amplitude, float period) {
        this.mAmplitude = amplitude;
        this.mPeriod = period;
    }

    @Override // android.animation.TimeInterpolator
    public float getInterpolation(float t) {
        float s;
        float p = this.mPeriod;
        float a = this.mAmplitude;
        if (t == MiuiFreeformPinManagerService.EDGE_AREA) {
            return MiuiFreeformPinManagerService.EDGE_AREA;
        }
        if (t == 1.0f) {
            return 1.0f;
        }
        if (p == MiuiFreeformPinManagerService.EDGE_AREA) {
            p = 0.3f;
        }
        if (a != MiuiFreeformPinManagerService.EDGE_AREA && a >= 1.0f) {
            s = (float) ((p / 6.283185307179586d) * Math.asin(1.0f / a));
        } else {
            a = 1.0f;
            s = p / 4.0f;
        }
        float t2 = t - 1.0f;
        return -((float) (a * Math.pow(2.0d, 10.0f * t2) * Math.sin(((t2 - s) * 6.283185307179586d) / p)));
    }
}
