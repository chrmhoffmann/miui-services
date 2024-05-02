package miui.android.animation.physics;

import com.android.server.wm.MiuiFreeformPinManagerService;
import miui.android.animation.physics.DynamicAnimation;
import miui.android.animation.property.FloatProperty;
import miui.android.animation.property.FloatValueHolder;
/* loaded from: classes.dex */
public final class FlingAnimation extends DynamicAnimation<FlingAnimation> {
    private final DragForce mFlingForce;

    public FlingAnimation(FloatValueHolder floatValueHolder) {
        super(floatValueHolder);
        DragForce dragForce = new DragForce();
        this.mFlingForce = dragForce;
        dragForce.setValueThreshold(getValueThreshold());
    }

    public <K> FlingAnimation(K object, FloatProperty<K> property) {
        super(object, property);
        DragForce dragForce = new DragForce();
        this.mFlingForce = dragForce;
        dragForce.setValueThreshold(getValueThreshold());
    }

    public FlingAnimation setFriction(float friction) {
        if (friction <= MiuiFreeformPinManagerService.EDGE_AREA) {
            throw new IllegalArgumentException("Friction must be positive");
        }
        this.mFlingForce.setFrictionScalar(friction);
        return this;
    }

    public float getFriction() {
        return this.mFlingForce.getFrictionScalar();
    }

    @Override // miui.android.animation.physics.DynamicAnimation
    public FlingAnimation setMinValue(float minValue) {
        super.setMinValue(minValue);
        return this;
    }

    @Override // miui.android.animation.physics.DynamicAnimation
    public FlingAnimation setMaxValue(float maxValue) {
        super.setMaxValue(maxValue);
        return this;
    }

    @Override // miui.android.animation.physics.DynamicAnimation
    public FlingAnimation setStartVelocity(float startVelocity) {
        super.setStartVelocity(startVelocity);
        return this;
    }

    @Override // miui.android.animation.physics.DynamicAnimation
    boolean updateValueAndVelocity(long deltaT) {
        DynamicAnimation.MassState state = this.mFlingForce.updateValueAndVelocity(this.mValue, this.mVelocity, deltaT);
        this.mValue = state.mValue;
        this.mVelocity = state.mVelocity;
        if (this.mValue < this.mMinValue) {
            this.mValue = this.mMinValue;
            return true;
        } else if (this.mValue <= this.mMaxValue) {
            return isAtEquilibrium(this.mValue, this.mVelocity);
        } else {
            this.mValue = this.mMaxValue;
            return true;
        }
    }

    @Override // miui.android.animation.physics.DynamicAnimation
    float getAcceleration(float value, float velocity) {
        return this.mFlingForce.getAcceleration(value, velocity);
    }

    @Override // miui.android.animation.physics.DynamicAnimation
    boolean isAtEquilibrium(float value, float velocity) {
        return value >= this.mMaxValue || value <= this.mMinValue || this.mFlingForce.isAtEquilibrium(value, velocity);
    }

    @Override // miui.android.animation.physics.DynamicAnimation
    void setValueThreshold(float threshold) {
        this.mFlingForce.setValueThreshold(threshold);
    }

    /* loaded from: classes.dex */
    public static final class DragForce implements Force {
        private static final float DEFAULT_FRICTION = -4.2f;
        private static final float VELOCITY_THRESHOLD_MULTIPLIER = 62.5f;
        private float mFriction;
        private final DynamicAnimation.MassState mMassState;
        private float mVelocityThreshold;

        private DragForce() {
            this.mFriction = DEFAULT_FRICTION;
            this.mMassState = new DynamicAnimation.MassState();
        }

        void setFrictionScalar(float frictionScalar) {
            this.mFriction = DEFAULT_FRICTION * frictionScalar;
        }

        float getFrictionScalar() {
            return this.mFriction / DEFAULT_FRICTION;
        }

        DynamicAnimation.MassState updateValueAndVelocity(float value, float velocity, long deltaT) {
            this.mMassState.mVelocity = (float) (velocity * Math.exp((((float) deltaT) / 1000.0f) * this.mFriction));
            DynamicAnimation.MassState massState = this.mMassState;
            float f = this.mFriction;
            massState.mValue = (float) ((value - (velocity / f)) + ((velocity / f) * Math.exp((f * ((float) deltaT)) / 1000.0f)));
            if (isAtEquilibrium(this.mMassState.mValue, this.mMassState.mVelocity)) {
                this.mMassState.mVelocity = MiuiFreeformPinManagerService.EDGE_AREA;
            }
            return this.mMassState;
        }

        @Override // miui.android.animation.physics.Force
        public float getAcceleration(float position, float velocity) {
            return this.mFriction * velocity;
        }

        @Override // miui.android.animation.physics.Force
        public boolean isAtEquilibrium(float value, float velocity) {
            return Math.abs(velocity) < this.mVelocityThreshold;
        }

        void setValueThreshold(float threshold) {
            this.mVelocityThreshold = VELOCITY_THRESHOLD_MULTIPLIER * threshold;
        }
    }
}
