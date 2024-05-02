package miui.android.view.animation;

import android.view.animation.Interpolator;
import com.android.server.wm.MiuiFreeformPinManagerService;
/* loaded from: classes.dex */
public class ElasticEaseInOutInterpolator implements Interpolator {
    private final float mAmplitude;
    private final float mPeriod;

    public ElasticEaseInOutInterpolator() {
        this(MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA);
    }

    public ElasticEaseInOutInterpolator(float amplitude, float period) {
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
        float t2 = t / 0.5f;
        if (t2 == 2.0f) {
            return 1.0f;
        }
        if (p == MiuiFreeformPinManagerService.EDGE_AREA) {
            p = 0.45000002f;
        }
        if (a == MiuiFreeformPinManagerService.EDGE_AREA || a < 1.0f) {
            a = 1.0f;
            s = p / 4.0f;
        } else {
            s = (float) ((p / 6.283185307179586d) * Math.asin(1.0f / a));
        }
        if (t2 < 1.0f) {
            float t3 = t2 - 1.0f;
            return ((float) (a * Math.pow(2.0d, 10.0f * t3) * Math.sin(((t3 - s) * 6.283185307179586d) / p))) * (-0.5f);
        }
        float t4 = t2 - 1.0f;
        return (float) ((a * Math.pow(2.0d, (-10.0f) * t4) * Math.sin(((t4 - s) * 6.283185307179586d) / p) * 0.5d) + 1.0d);
    }
}
