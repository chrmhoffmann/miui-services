package miui.android.animation.physics;

import miui.android.animation.IAnimTarget;
import miui.android.animation.property.FloatProperty;
/* loaded from: classes.dex */
public class EquilibriumChecker {
    public static final float MIN_VISIBLE_CHANGE_ALPHA = 0.00390625f;
    public static final float MIN_VISIBLE_CHANGE_PIXELS = 1.0f;
    public static final float MIN_VISIBLE_CHANGE_ROTATION_DEGREES = 0.1f;
    public static final float MIN_VISIBLE_CHANGE_SCALE = 0.002f;
    private static final float THRESHOLD_MULTIPLIER = 0.75f;
    public static final float VELOCITY_THRESHOLD_MULTIPLIER = 16.666666f;
    private double mTargetValue = Double.MAX_VALUE;
    private float mValueThreshold;
    private float mVelocityThreshold;

    public void init(IAnimTarget target, FloatProperty property, double targetValue) {
        float minVisibleChange = target.getMinVisibleChange(property) * 0.75f;
        this.mValueThreshold = minVisibleChange;
        this.mVelocityThreshold = minVisibleChange * 16.666666f;
        this.mTargetValue = targetValue;
    }

    public boolean isAtEquilibrium(int easeStyle, double value, double velocity) {
        return (easeStyle != -2 || isAt(value, this.mTargetValue)) && Math.abs(velocity) < ((double) this.mVelocityThreshold);
    }

    private boolean isAt(double value, double target) {
        return Math.abs(this.mTargetValue) == 3.4028234663852886E38d || Math.abs(value - target) < ((double) this.mValueThreshold);
    }
}
