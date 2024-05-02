package miui.android.animation.utils;

import android.animation.TimeInterpolator;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import com.android.server.wm.MiuiFreeformPinManagerService;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import miui.android.animation.physics.PhysicsOperator;
import miui.android.animation.styles.PropertyStyle;
import miui.android.view.animation.BounceEaseInInterpolator;
import miui.android.view.animation.BounceEaseInOutInterpolator;
import miui.android.view.animation.BounceEaseOutInterpolator;
import miui.android.view.animation.CubicEaseInInterpolator;
import miui.android.view.animation.CubicEaseInOutInterpolator;
import miui.android.view.animation.CubicEaseOutInterpolator;
import miui.android.view.animation.ExponentialEaseInInterpolator;
import miui.android.view.animation.ExponentialEaseInOutInterpolator;
import miui.android.view.animation.ExponentialEaseOutInterpolator;
import miui.android.view.animation.QuadraticEaseInInterpolator;
import miui.android.view.animation.QuadraticEaseInOutInterpolator;
import miui.android.view.animation.QuadraticEaseOutInterpolator;
import miui.android.view.animation.QuarticEaseInInterpolator;
import miui.android.view.animation.QuarticEaseInOutInterpolator;
import miui.android.view.animation.QuinticEaseInInterpolator;
import miui.android.view.animation.QuinticEaseInOutInterpolator;
import miui.android.view.animation.QuinticEaseOutInterpolator;
import miui.android.view.animation.SineEaseInInterpolator;
import miui.android.view.animation.SineEaseInOutInterpolator;
import miui.android.view.animation.SineEaseOutInterpolator;
/* loaded from: classes.dex */
public class EaseManager {
    public static final long DEFAULT_DURATION = 300;
    static final ConcurrentHashMap<Integer, TimeInterpolator> sInterpolatorCache = new ConcurrentHashMap<>();

    /* loaded from: classes.dex */
    public interface EaseStyleDef {
        public static final int ACCELERATE = -3;
        public static final int ACCELERATE_DECELERATE = 21;
        public static final int ACCELERATE_INTERPOLATOR = 22;
        public static final int BOUNCE = 23;
        public static final int BOUNCE_EASE_IN = 24;
        public static final int BOUNCE_EASE_INOUT = 26;
        public static final int BOUNCE_EASE_OUT = 25;
        public static final int CUBIC_IN = 5;
        public static final int CUBIC_INOUT = 7;
        public static final int CUBIC_OUT = 6;
        public static final int DECELERATE = 20;
        public static final int DURATION = -1;
        public static final int EXPO_IN = 17;
        public static final int EXPO_INOUT = 19;
        public static final int EXPO_OUT = 18;
        public static final int FRICTION = -4;
        public static final int LINEAR = 1;
        public static final int QUAD_IN = 2;
        public static final int QUAD_INOUT = 4;
        public static final int QUAD_OUT = 3;
        public static final int QUART_IN = 8;
        public static final int QUART_INOUT = 10;
        public static final int QUART_OUT = 9;
        public static final int QUINT_IN = 11;
        public static final int QUINT_INOUT = 13;
        public static final int QUINT_OUT = 12;
        public static final int REBOUND = -6;
        public static final int SIN_IN = 14;
        public static final int SIN_INOUT = 16;
        public static final int SIN_OUT = 15;
        public static final int SPRING = 0;
        public static final int SPRING_PHY = -2;
        public static final int STOP = -5;
    }

    static TimeInterpolator createTimeInterpolator(int style, float... factors) {
        switch (style) {
            case -1:
            case 1:
                return new LinearInterpolator();
            case 0:
                return new SpringInterpolator().setDamping(factors[0]).setResponse(factors[1]);
            case 2:
                return new QuadraticEaseInInterpolator();
            case 3:
                return new QuadraticEaseOutInterpolator();
            case 4:
                return new QuadraticEaseInOutInterpolator();
            case 5:
                return new CubicEaseInInterpolator();
            case 6:
                return new CubicEaseOutInterpolator();
            case 7:
                return new CubicEaseInOutInterpolator();
            case 8:
                return new QuarticEaseInInterpolator();
            case 9:
                return new QuadraticEaseOutInterpolator();
            case 10:
                return new QuarticEaseInOutInterpolator();
            case 11:
                return new QuinticEaseInInterpolator();
            case 12:
                return new QuinticEaseOutInterpolator();
            case 13:
                return new QuinticEaseInOutInterpolator();
            case 14:
                return new SineEaseInInterpolator();
            case 15:
                return new SineEaseOutInterpolator();
            case 16:
                return new SineEaseInOutInterpolator();
            case 17:
                return new ExponentialEaseInInterpolator();
            case 18:
                return new ExponentialEaseOutInterpolator();
            case 19:
                return new ExponentialEaseInOutInterpolator();
            case 20:
                return new DecelerateInterpolator();
            case 21:
                return new AccelerateDecelerateInterpolator();
            case 22:
                return new AccelerateInterpolator();
            case 23:
                return new BounceInterpolator();
            case 24:
                return new BounceEaseInInterpolator();
            case 25:
                return new BounceEaseOutInterpolator();
            case 26:
                return new BounceEaseInOutInterpolator();
            default:
                return null;
        }
    }

    /* loaded from: classes.dex */
    public static class EaseStyle {
        public volatile float[] factors;
        public final double[] parameters;
        public boolean stopAtTarget;
        public final int style;

        public EaseStyle(int s, float... fa) {
            double[] dArr = {0.0d, 0.0d};
            this.parameters = dArr;
            this.style = s;
            this.factors = fa;
            setParameters(this, dArr);
        }

        public void setFactors(float... fa) {
            this.factors = fa;
            setParameters(this, this.parameters);
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof EaseStyle)) {
                return false;
            }
            EaseStyle easeStyle = (EaseStyle) o;
            return this.style == easeStyle.style && Arrays.equals(this.factors, easeStyle.factors);
        }

        public int hashCode() {
            int result = Objects.hash(Integer.valueOf(this.style));
            return (result * 31) + Arrays.hashCode(this.factors);
        }

        public String toString() {
            return "EaseStyle{style=" + this.style + ", factors=" + Arrays.toString(this.factors) + ", parameters = " + Arrays.toString(this.parameters) + '}';
        }

        private static void setParameters(EaseStyle ease, double[] params) {
            PhysicsOperator operator = ease == null ? null : PropertyStyle.getPhyOperator(ease.style);
            if (operator != null) {
                operator.getParameters(ease.factors, params);
            } else {
                Arrays.fill(params, 0.0d);
            }
        }
    }

    /* loaded from: classes.dex */
    public static class InterpolateEaseStyle extends EaseStyle {
        public long duration = 300;

        public InterpolateEaseStyle(int s, float... factors) {
            super(s, factors);
        }

        public InterpolateEaseStyle setDuration(long d) {
            this.duration = d;
            return this;
        }

        @Override // miui.android.animation.utils.EaseManager.EaseStyle
        public String toString() {
            return "InterpolateEaseStyle{style=" + this.style + ", duration=" + this.duration + ", factors=" + Arrays.toString(this.factors) + '}';
        }
    }

    public static boolean isPhysicsStyle(int styleDef) {
        return styleDef < -1;
    }

    public static EaseStyle getStyle(int styleDef, float... values) {
        if (styleDef >= -1) {
            float[] factors = values.length > 1 ? Arrays.copyOfRange(values, 1, values.length) : new float[0];
            InterpolateEaseStyle style = getInterpolatorStyle(styleDef, factors);
            if (values.length > 0) {
                style.setDuration((int) values[0]);
            }
            return style;
        }
        return new EaseStyle(styleDef, values);
    }

    public static TimeInterpolator getInterpolator(int styleDef, float... values) {
        InterpolateEaseStyle style = getInterpolatorStyle(styleDef, values);
        return getInterpolator(style);
    }

    private static InterpolateEaseStyle getInterpolatorStyle(int styleDef, float... values) {
        return new InterpolateEaseStyle(styleDef, values);
    }

    public static TimeInterpolator getInterpolator(InterpolateEaseStyle easeStyle) {
        if (easeStyle != null) {
            ConcurrentHashMap<Integer, TimeInterpolator> concurrentHashMap = sInterpolatorCache;
            TimeInterpolator interpolator = concurrentHashMap.get(Integer.valueOf(easeStyle.style));
            if (interpolator == null && (interpolator = createTimeInterpolator(easeStyle.style, easeStyle.factors)) != null) {
                concurrentHashMap.put(Integer.valueOf(easeStyle.style), interpolator);
            }
            return interpolator;
        }
        return null;
    }

    /* loaded from: classes.dex */
    public static class SpringInterpolator implements TimeInterpolator {
        private float c;
        private float c2;
        private float k;
        private float r;
        private float w;
        private float damping = 0.95f;
        private float response = 0.6f;
        private float initial = -1.0f;
        private float c1 = -1.0f;
        private float m = 1.0f;

        public SpringInterpolator() {
            updateParameters();
        }

        @Override // android.animation.TimeInterpolator
        public float getInterpolation(float input) {
            return (float) ((Math.pow(2.718281828459045d, this.r * input) * ((this.c1 * Math.cos(this.w * input)) + (this.c2 * Math.sin(this.w * input)))) + 1.0d);
        }

        public float getDamping() {
            return this.damping;
        }

        public float getResponse() {
            return this.response;
        }

        public SpringInterpolator setDamping(float damping) {
            this.damping = damping;
            updateParameters();
            return this;
        }

        public SpringInterpolator setResponse(float response) {
            this.response = response;
            updateParameters();
            return this;
        }

        private void updateParameters() {
            double pow = Math.pow(6.283185307179586d / this.response, 2.0d);
            float f = this.m;
            float f2 = (float) (pow * f);
            this.k = f2;
            float f3 = (float) (((this.damping * 12.566370614359172d) * f) / this.response);
            this.c = f3;
            float f4 = this.m;
            float sqrt = ((float) Math.sqrt(((f * 4.0f) * f2) - (f3 * f3))) / (f4 * 2.0f);
            this.w = sqrt;
            float f5 = -((this.c / 2.0f) * f4);
            this.r = f5;
            this.c2 = (MiuiFreeformPinManagerService.EDGE_AREA - (f5 * this.initial)) / sqrt;
        }
    }
}
