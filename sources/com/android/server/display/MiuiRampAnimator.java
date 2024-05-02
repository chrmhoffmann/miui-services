package com.android.server.display;

import android.animation.ValueAnimator;
import android.util.IntProperty;
import android.view.Choreographer;
import com.android.server.wm.MiuiFreeformPinManagerService;
/* loaded from: classes.dex */
public final class MiuiRampAnimator<T> {
    private float mAnimatedValue;
    private boolean mAnimating;
    private int mCurrentValue;
    private long mLastFrameTimeNanos;
    private Listener mListener;
    private final T mObject;
    private final IntProperty<T> mProperty;
    private int mRate;
    private int mTargetValue;
    private boolean mFirstTime = true;
    private final Runnable mAnimationCallback = new Runnable() { // from class: com.android.server.display.MiuiRampAnimator.1
        /* JADX WARN: Multi-variable type inference failed */
        @Override // java.lang.Runnable
        public void run() {
            long frameTimeNanos = MiuiRampAnimator.this.mChoreographer.getFrameTimeNanos();
            float timeDelta = ((float) (frameTimeNanos - MiuiRampAnimator.this.mLastFrameTimeNanos)) * 1.0E-9f;
            MiuiRampAnimator.this.mLastFrameTimeNanos = frameTimeNanos;
            float scale = ValueAnimator.getDurationScale();
            if (scale == MiuiFreeformPinManagerService.EDGE_AREA) {
                MiuiRampAnimator miuiRampAnimator = MiuiRampAnimator.this;
                miuiRampAnimator.mAnimatedValue = miuiRampAnimator.mTargetValue;
            } else {
                float amount = (MiuiRampAnimator.this.mRate * timeDelta) / scale;
                if (MiuiRampAnimator.this.mTargetValue > MiuiRampAnimator.this.mCurrentValue) {
                    MiuiRampAnimator miuiRampAnimator2 = MiuiRampAnimator.this;
                    miuiRampAnimator2.mAnimatedValue = Math.min(miuiRampAnimator2.mAnimatedValue + amount, MiuiRampAnimator.this.mTargetValue);
                } else {
                    MiuiRampAnimator miuiRampAnimator3 = MiuiRampAnimator.this;
                    miuiRampAnimator3.mAnimatedValue = Math.max(miuiRampAnimator3.mAnimatedValue - amount, MiuiRampAnimator.this.mTargetValue);
                }
            }
            int oldCurrentValue = MiuiRampAnimator.this.mCurrentValue;
            MiuiRampAnimator miuiRampAnimator4 = MiuiRampAnimator.this;
            miuiRampAnimator4.mCurrentValue = Math.round(miuiRampAnimator4.mAnimatedValue);
            if (oldCurrentValue != MiuiRampAnimator.this.mCurrentValue) {
                MiuiRampAnimator.this.mProperty.setValue(MiuiRampAnimator.this.mObject, MiuiRampAnimator.this.mCurrentValue);
            }
            if (MiuiRampAnimator.this.mTargetValue != MiuiRampAnimator.this.mCurrentValue) {
                MiuiRampAnimator.this.postAnimationCallback();
                return;
            }
            MiuiRampAnimator.this.mAnimating = false;
            if (MiuiRampAnimator.this.mListener != null) {
                MiuiRampAnimator.this.mListener.onAnimationEnd();
            }
        }
    };
    private final Choreographer mChoreographer = Choreographer.getInstance();

    /* loaded from: classes.dex */
    public interface Listener {
        void onAnimationEnd();
    }

    public MiuiRampAnimator(T object, IntProperty<T> property) {
        this.mObject = object;
        this.mProperty = property;
    }

    public boolean animateTo(int target, int rate) {
        int i;
        int i2;
        boolean z = this.mFirstTime;
        boolean changed = false;
        if (z || rate <= 0) {
            if (!z && target == this.mCurrentValue) {
                return false;
            }
            this.mFirstTime = false;
            this.mRate = 0;
            this.mTargetValue = target;
            this.mCurrentValue = target;
            this.mProperty.setValue(this.mObject, target);
            if (this.mAnimating) {
                this.mAnimating = false;
                cancelAnimationCallback();
            }
            Listener listener = this.mListener;
            if (listener != null) {
                listener.onAnimationEnd();
            }
            return true;
        }
        boolean z2 = this.mAnimating;
        if (!z2 || rate > this.mRate || ((target <= (i2 = this.mCurrentValue) && i2 <= this.mTargetValue) || (this.mTargetValue <= i2 && i2 <= target))) {
            this.mRate = rate;
        }
        if (this.mTargetValue != target) {
            changed = true;
        }
        this.mTargetValue = target;
        if (!z2 && target != (i = this.mCurrentValue)) {
            this.mAnimating = true;
            this.mAnimatedValue = i;
            this.mLastFrameTimeNanos = System.nanoTime();
            postAnimationCallback();
        }
        return changed;
    }

    public boolean isAnimating() {
        return this.mAnimating;
    }

    public void setListener(Listener listener) {
        this.mListener = listener;
    }

    public void postAnimationCallback() {
        this.mChoreographer.postCallback(1, this.mAnimationCallback, null);
    }

    private void cancelAnimationCallback() {
        this.mChoreographer.removeCallbacks(1, this.mAnimationCallback, null);
    }
}
