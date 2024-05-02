package com.android.server.wm;

import com.android.server.wm.MiuiFreeFormDynamicAnimation;
import com.miui.internal.dynamicanimation.animation.FloatPropertyCompat;
import com.miui.internal.dynamicanimation.animation.FloatValueHolder;
/* loaded from: classes.dex */
public final class MiuiFreeFormSpringAnimation extends MiuiFreeFormDynamicAnimation<MiuiFreeFormSpringAnimation> {
    private static final float UNSET = Float.MAX_VALUE;
    private boolean mEndRequested;
    private boolean mNeedExecuteCallbackWhenAnimationEnd;
    private float mPendingPosition;
    private MiuiFreeFormSpringForce mSpring;

    public void setNeedExecuteCallbackWhenAnimationEnd(boolean mNeedExecuteCallbackWhenAnimationEnd) {
        this.mNeedExecuteCallbackWhenAnimationEnd = mNeedExecuteCallbackWhenAnimationEnd;
    }

    public MiuiFreeFormSpringAnimation(FloatValueHolder floatValueHolder) {
        super(floatValueHolder);
        this.mSpring = null;
        this.mPendingPosition = UNSET;
        this.mEndRequested = false;
    }

    public <MiuiFreeFormActivityStack> MiuiFreeFormSpringAnimation(MiuiFreeFormActivityStack object, FloatPropertyCompat<MiuiFreeFormActivityStack> property) {
        super(object, property);
        this.mSpring = null;
        this.mPendingPosition = UNSET;
        this.mEndRequested = false;
    }

    public <MiuiFreeFormActivityStack> MiuiFreeFormSpringAnimation(MiuiFreeFormActivityStack object, FloatPropertyCompat<MiuiFreeFormActivityStack> property, float finalPosition) {
        super(object, property);
        this.mSpring = null;
        this.mPendingPosition = UNSET;
        this.mEndRequested = false;
        this.mSpring = new MiuiFreeFormSpringForce(finalPosition);
    }

    public MiuiFreeFormSpringForce getSpring() {
        return this.mSpring;
    }

    public MiuiFreeFormSpringAnimation setSpring(MiuiFreeFormSpringForce force) {
        this.mSpring = force;
        return this;
    }

    @Override // com.android.server.wm.MiuiFreeFormDynamicAnimation
    public void start(boolean manully) {
        sanityCheck();
        this.mSpring.setValueThreshold(getValueThreshold());
        super.start(manully);
    }

    public void animateToFinalPosition(float finalPosition) {
        if (isRunning()) {
            this.mPendingPosition = finalPosition;
            return;
        }
        if (this.mSpring == null) {
            this.mSpring = new MiuiFreeFormSpringForce(finalPosition);
        }
        this.mSpring.setFinalPosition(finalPosition);
        start();
    }

    public void skipToEnd() {
        if (!canSkipToEnd()) {
            throw new UnsupportedOperationException("Spring animations can only come to an end when there is damping");
        }
        if (this.mRunning) {
            this.mEndRequested = true;
        }
    }

    public boolean canSkipToEnd() {
        return this.mSpring.mDampingRatio > 0.0d;
    }

    private void sanityCheck() {
        MiuiFreeFormSpringForce miuiFreeFormSpringForce = this.mSpring;
        if (miuiFreeFormSpringForce == null) {
            throw new UnsupportedOperationException("Incomplete SpringAnimation: Either final position or a spring force needs to be set.");
        }
        double finalPosition = miuiFreeFormSpringForce.getFinalPosition();
        if (finalPosition <= this.mMaxValue) {
            float f = this.mMinValue;
        }
    }

    @Override // com.android.server.wm.MiuiFreeFormDynamicAnimation
    boolean updateValueAndVelocity(long deltaT) {
        if (this.mEndRequested) {
            float f = this.mPendingPosition;
            if (f != UNSET) {
                this.mSpring.setFinalPosition(f);
                this.mPendingPosition = UNSET;
            }
            this.mValue = this.mSpring.getFinalPosition();
            this.mVelocity = MiuiFreeformPinManagerService.EDGE_AREA;
            this.mEndRequested = false;
            return true;
        }
        if (this.mPendingPosition == UNSET) {
            MiuiFreeFormDynamicAnimation.MassState massState = this.mSpring.updateValues(this.mValue, this.mVelocity, deltaT);
            this.mValue = massState.mValue;
            this.mVelocity = massState.mVelocity;
        } else {
            this.mSpring.getFinalPosition();
            MiuiFreeFormDynamicAnimation.MassState massState2 = this.mSpring.updateValues(this.mValue, this.mVelocity, deltaT / 2);
            this.mSpring.setFinalPosition(this.mPendingPosition);
            this.mPendingPosition = UNSET;
            MiuiFreeFormDynamicAnimation.MassState massState3 = this.mSpring.updateValues(massState2.mValue, massState2.mVelocity, deltaT / 2);
            this.mValue = massState3.mValue;
            this.mVelocity = massState3.mVelocity;
        }
        this.mValue = Math.max(this.mValue, this.mMinValue);
        this.mValue = Math.min(this.mValue, this.mMaxValue);
        if (isAtEquilibrium(this.mValue, this.mVelocity)) {
            this.mValue = this.mSpring.getFinalPosition();
            this.mVelocity = MiuiFreeformPinManagerService.EDGE_AREA;
            return true;
        }
        return false;
    }

    @Override // com.android.server.wm.MiuiFreeFormDynamicAnimation
    float getAcceleration(float value, float velocity) {
        return this.mSpring.getAcceleration(value, velocity);
    }

    @Override // com.android.server.wm.MiuiFreeFormDynamicAnimation
    boolean isAtEquilibrium(float value, float velocity) {
        return this.mSpring.isAtEquilibrium(value, velocity);
    }

    @Override // com.android.server.wm.MiuiFreeFormDynamicAnimation
    void setValueThreshold(float threshold) {
    }

    @Override // com.android.server.wm.MiuiFreeFormDynamicAnimation
    public boolean needExecuteCallbackWhenAnimationEnd() {
        return this.mNeedExecuteCallbackWhenAnimationEnd;
    }
}
