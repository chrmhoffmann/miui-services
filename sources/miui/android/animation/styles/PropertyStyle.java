package miui.android.animation.styles;

import android.animation.TimeInterpolator;
import miui.android.animation.IAnimTarget;
import miui.android.animation.internal.AnimData;
import miui.android.animation.internal.AnimValueUtils;
import miui.android.animation.physics.AccelerateOperator;
import miui.android.animation.physics.EquilibriumChecker;
import miui.android.animation.physics.FrictionOperator;
import miui.android.animation.physics.PhysicsOperator;
import miui.android.animation.physics.SpringOperator;
import miui.android.animation.property.FloatProperty;
import miui.android.animation.utils.CommonUtils;
import miui.android.animation.utils.EaseManager;
import miui.android.animation.utils.LogUtils;
/* loaded from: classes.dex */
public class PropertyStyle {
    private static final long LONGEST_DURATION = 10000;
    static final SpringOperator sSpring = new SpringOperator();
    static final AccelerateOperator sAccelerate = new AccelerateOperator();
    static final FrictionOperator sFriction = new FrictionOperator();
    static final ThreadLocal<EquilibriumChecker> mCheckerLocal = new ThreadLocal<>();

    public static void doAnimationFrame(IAnimTarget target, AnimData data, long totalT, long deltaT, long averageDelta) {
        long totalTime = totalT - data.startTime;
        if (EaseManager.isPhysicsStyle(data.ease.style)) {
            updatePhysicsAnim(target, data, totalTime, deltaT, averageDelta);
        } else {
            updateInterpolatorAnim(data, totalTime);
        }
    }

    private static void updateInterpolatorAnim(AnimData data, long totalTime) {
        EaseManager.InterpolateEaseStyle ease = (EaseManager.InterpolateEaseStyle) data.ease;
        TimeInterpolator interpolator = EaseManager.getInterpolator(ease);
        if (totalTime < ease.duration) {
            data.progress = interpolator.getInterpolation(((float) totalTime) / ((float) ease.duration));
            data.value = data.progress;
            return;
        }
        data.setOp((byte) 3);
        data.progress = 1.0d;
        data.value = data.progress;
    }

    private static void updatePhysicsAnim(IAnimTarget target, AnimData data, long totalTime, long deltaT, long averageDelta) {
        int frameCount;
        if (deltaT > averageDelta) {
            frameCount = Math.round(((float) deltaT) / ((float) averageDelta));
        } else {
            frameCount = 1;
        }
        double delta = averageDelta / 1000.0d;
        EquilibriumChecker checker = (EquilibriumChecker) CommonUtils.getLocal(mCheckerLocal, EquilibriumChecker.class);
        checker.init(target, data.property, data.targetValue);
        int i = 0;
        while (i < frameCount) {
            doPhysicsCalculation(data, delta);
            int i2 = i;
            if (!isAnimRunning(checker, data.property, data.ease.style, data.value, data.velocity, totalTime)) {
                data.setOp((byte) 3);
                setFinishValue(data);
                return;
            }
            i = i2 + 1;
        }
    }

    private static void setFinishValue(AnimData data) {
        if (!isUsingSpringPhy(data)) {
            return;
        }
        data.value = data.targetValue;
    }

    private static void doPhysicsCalculation(AnimData data, double delta) {
        double startVelocity = data.velocity;
        PhysicsOperator op = getPhyOperator(data.ease.style);
        if (op == null || ((op instanceof SpringOperator) && AnimValueUtils.isInvalid(data.targetValue))) {
            data.value = data.targetValue;
            data.velocity = 0.0d;
            return;
        }
        double velocity = op.updateVelocity(startVelocity, data.ease.parameters[0], data.ease.parameters[1], delta, data.targetValue, data.value);
        data.value += velocity * delta;
        data.velocity = velocity;
    }

    static boolean isAnimRunning(EquilibriumChecker checker, FloatProperty property, int easeStyle, double value, double velocity, long totalTime) {
        boolean isRunning = !checker.isAtEquilibrium(easeStyle, value, velocity);
        if (isRunning && totalTime > 10000) {
            isRunning = false;
            if (LogUtils.isLogEnabled()) {
                LogUtils.debug("animation for " + property.getName() + " stopped for running time too long, totalTime = " + totalTime, new Object[0]);
            }
        }
        return isRunning;
    }

    public static PhysicsOperator getPhyOperator(int style) {
        switch (style) {
            case EaseManager.EaseStyleDef.FRICTION /* -4 */:
                return sFriction;
            case EaseManager.EaseStyleDef.ACCELERATE /* -3 */:
                return sAccelerate;
            case -2:
                return sSpring;
            default:
                return null;
        }
    }

    private static boolean isUsingSpringPhy(AnimData data) {
        return data.ease.style == -2;
    }
}
