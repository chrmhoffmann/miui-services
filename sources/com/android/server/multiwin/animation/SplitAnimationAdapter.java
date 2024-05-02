package com.android.server.multiwin.animation;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.View;
import android.view.animation.ScaleAnimation;
import com.android.server.multiwin.animation.interpolator.FastOutSlowInInterpolator;
/* loaded from: classes.dex */
public class SplitAnimationAdapter {
    private static final int MATRIX_VALUES_NUM = 9;
    private static final float SPLIT_DEFAULT_SCALE_FACTOR = 1.0f;
    private static final long SPLIT_SCALE_DURATION = 200;
    private static final float SPLIT_SCALE_FACTOR = 0.95f;
    private static final String TAG = "SplitScaleAnimation";
    private ValueAnimator mAnimator;
    private ColorDrawable mColorDrawable;
    private ColorDrawable mHideShadowColor;
    private ScaleAnimation mScaleAnimation;
    private View mScaleTarget;
    private ColorDrawable mShowShadowColor;
    private int mSplitMode;

    public SplitAnimationAdapter(View scaleTarget, int splitMode, Context context) {
        this.mScaleTarget = scaleTarget;
        this.mSplitMode = splitMode;
    }

    public SplitAnimationAdapter(View scaleTarget, ColorDrawable drawable, int paramInt, Context context) {
        this.mScaleTarget = scaleTarget;
        this.mColorDrawable = drawable;
        this.mSplitMode = paramInt;
        this.mShowShadowColor = new ColorDrawable(context.getResources().getColor(285605955));
        this.mHideShadowColor = new ColorDrawable(context.getResources().getColor(285605954));
    }

    private Point getScalePivots() {
        float pivotX = this.mScaleTarget.getPivotX();
        float pivotY = this.mScaleTarget.getPivotY();
        return new Point((int) pivotX, (int) pivotY);
    }

    private void playScaleAnimation(float toScale) {
        float fromScaleX;
        float fromScaleX2;
        if (this.mScaleTarget == null) {
            return;
        }
        ScaleAnimation scaleAnimation = this.mScaleAnimation;
        if (scaleAnimation != null && scaleAnimation.hasStarted()) {
            this.mScaleAnimation.cancel();
        }
        Matrix animationMatrix = this.mScaleTarget.getAnimationMatrix();
        if (animationMatrix == null) {
            fromScaleX2 = 1.0f;
            fromScaleX = 1.0f;
        } else {
            float[] values = new float[9];
            animationMatrix.getValues(values);
            float fromScaleX3 = values[0];
            float f = values[4];
            fromScaleX2 = fromScaleX3;
            fromScaleX = f;
        }
        Point pivots = getScalePivots();
        float pivotX = pivots.x;
        float pivotY = pivots.y;
        Log.d(TAG, "start split scale animation mSplitMode = " + this.mSplitMode + ", mScaleTarget.getWidth() = " + this.mScaleTarget.getWidth() + ", mScaleTarget.getHeight() = " + this.mScaleTarget.getHeight());
        ScaleAnimation scaleAnimation2 = new ScaleAnimation(fromScaleX2, toScale, fromScaleX, toScale, 0, pivotX, 0, pivotY);
        this.mScaleAnimation = scaleAnimation2;
        scaleAnimation2.setDuration(200L);
        this.mScaleAnimation.setInterpolator(new FastOutSlowInInterpolator());
        this.mScaleAnimation.setFillEnabled(true);
        this.mScaleAnimation.setFillBefore(true);
        this.mScaleAnimation.setFillAfter(true);
        this.mScaleTarget.startAnimation(this.mScaleAnimation);
    }

    public void playScaleDownAnimation() {
        playScaleAnimation(0.96f);
    }

    public void playScaleUpAnimation() {
        playScaleAnimation(1.0f);
    }

    public void playColorShowAnimation() {
        playColorAnimation(this.mShowShadowColor);
    }

    public void playColorHideAnimation() {
        playColorAnimation(this.mHideShadowColor);
    }

    private void playColorAnimation(ColorDrawable drawable) {
        if (this.mColorDrawable == null) {
            return;
        }
        ValueAnimator valueAnimator = this.mAnimator;
        if (valueAnimator != null && valueAnimator.isRunning()) {
            this.mAnimator.cancel();
        }
        if (this.mColorDrawable.getColor() != drawable.getColor()) {
            ValueAnimator ofObject = ValueAnimator.ofObject(new ArgbEvaluator(), Integer.valueOf(this.mColorDrawable.getColor()), Integer.valueOf(drawable.getColor()));
            this.mAnimator = ofObject;
            ofObject.setDuration(200L);
            this.mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.server.multiwin.animation.SplitAnimationAdapter.1
                @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                public void onAnimationUpdate(ValueAnimator valueAnimator2) {
                    Object value = valueAnimator2.getAnimatedValue();
                    if (value != null) {
                        SplitAnimationAdapter.this.mColorDrawable.setColor(((Integer) value).intValue());
                    }
                }
            });
            this.mAnimator.start();
        }
    }
}
