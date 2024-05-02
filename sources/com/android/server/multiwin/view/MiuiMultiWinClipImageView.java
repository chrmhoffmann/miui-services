package com.android.server.multiwin.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.MiuiMultiWinClientUtils;
import android.util.MiuiMultiWindowUtils;
import android.util.Slog;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import com.android.server.multiwin.MiuiMultiWinUtils;
import com.android.server.multiwin.animation.interpolator.FastOutSlowInInterpolator;
import com.android.server.wm.MiuiFreeformPinManagerService;
import com.android.server.wm.PhysicBasedInterpolator;
/* loaded from: classes.dex */
public class MiuiMultiWinClipImageView extends View {
    private static final float FINAL_PUSH_SCALE_FACTOR_1 = 0.5f;
    private static final float FULL_SCREEN_SHOT_SCALE_FACTOR_2 = 0.95f;
    private static final float MARGIN_SCALE = 0.0145f;
    private static final float PRE_PUSH_SCALE_FACTOR_1 = 0.75f;
    private static final float PRE_PUSH_SCALE_FACTOR_2 = 1.0f;
    private static final String PROPERTY_SCALE_X = "scaleX";
    private static final String PROPERTY_SCALE_Y = "scaleY";
    private static final String PROPERTY_TRANSLATION = "translation";
    private static final String TAG = "MiuiMultiWinClipImageView";
    Paint blurRadiatedIconPaint;
    private int currentPushAcceptSplitMode;
    private float mBackgroundAlpha;
    private Drawable mBgDrawable;
    private ValueAnimator mBgDrawableAlphaAnimation;
    private Bitmap mBlurRadiatedIconBitmap;
    protected Paint mBorderPaint;
    private ValueAnimator mFinalPushAnimator;
    private boolean mHasBeenResizedWithoutNavBar;
    private boolean mHasRemovedSelf;
    private Drawable mIcon;
    private ValueAnimator mIconAlphaAnimator;
    private int mInSwapMode;
    private boolean mIsLandScape;
    protected boolean mIsToDrawBorder;
    private int mLastSwapAcceptSplitMode;
    private int mOriginalInSwapMode;
    private ValueAnimator mPrePushAnimator;
    private Path mRoundCornerClipPath;
    private RectF mRoundCornerClipRect;
    protected float mRoundCornerRadius;
    private Bitmap mSnapShot;
    private float mSnapShotAlpha;
    private ValueAnimator mSnapShotAlphaAnimation;
    private ValueAnimator mSwapAnimator;
    private float scaleRatioX;
    private float scaleRatioY;

    public MiuiMultiWinClipImageView(Context context) {
        this(context, null, 0);
    }

    public MiuiMultiWinClipImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MiuiMultiWinClipImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mSnapShotAlpha = 1.0f;
        this.blurRadiatedIconPaint = new Paint();
        this.scaleRatioX = 1.0f;
        this.scaleRatioY = 1.0f;
        this.currentPushAcceptSplitMode = 0;
        this.mIsToDrawBorder = true;
        this.mRoundCornerClipRect = new RectF();
        this.mRoundCornerClipPath = new Path();
        this.mIsLandScape = false;
        this.mHasRemovedSelf = true;
        this.mHasBeenResizedWithoutNavBar = false;
        this.mLastSwapAcceptSplitMode = 0;
        init(context);
        setForceDarkAllowed(false);
    }

    private void clipForRoundCorner(Canvas canvas) {
        this.mRoundCornerClipRect.set(MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA, getWidth(), getHeight());
        if (isLandScape()) {
            this.scaleRatioX = getScaleY() / getScaleX();
            this.scaleRatioY = 1.0f;
        } else {
            this.scaleRatioX = 1.0f;
            this.scaleRatioY = getScaleX() / getScaleY();
        }
        this.mRoundCornerClipPath.reset();
        Path path = this.mRoundCornerClipPath;
        RectF rectF = this.mRoundCornerClipRect;
        float f = this.mRoundCornerRadius;
        path.addRoundRect(rectF, this.scaleRatioX * f, f * this.scaleRatioY, Path.Direction.CW);
        canvas.clipPath(this.mRoundCornerClipPath);
    }

    private Animator createSwapAnimation(boolean isConsiderNavBar, int originSwapMode, int navBarHeight, int currentSwapAcceptSplitMode, float splitSwapSize, long startDelayDuration, AnimatorListenerAdapter swapAnimationListenerAdapter, boolean needScale) {
        ValueAnimator valueAnimator = this.mSwapAnimator;
        if (valueAnimator != null && valueAnimator.isStarted()) {
            this.mSwapAnimator.cancel();
        }
        float fromTranslation = isLandScape() ? getTranslationX() : getTranslationY();
        float toTranslation = getSwapToTranslation(isConsiderNavBar, navBarHeight, currentSwapAcceptSplitMode, splitSwapSize, needScale);
        float fromScaleX = getScaleX();
        float toScaleX = getSwapToScale(currentSwapAcceptSplitMode, true, true, needScale);
        float fromScaleY = getScaleY();
        float toScaleY = getSwapToScale(isConsiderNavBar, originSwapMode, navBarHeight, currentSwapAcceptSplitMode, false, true, needScale);
        Slog.d(TAG, "playSwapAnimation: mInSwapMode = " + this.mInSwapMode + ", currentSwapAcceptSplitMode = " + currentSwapAcceptSplitMode + ", fromTranslation = " + fromTranslation + ", toTranslation = " + toTranslation + ", fromScaleX = " + fromScaleX + ", toScaleX = " + toScaleX + ", fromScaleY = " + fromScaleY + ", toScaleY = " + toScaleY);
        ValueAnimator ofPropertyValuesHolder = ValueAnimator.ofPropertyValuesHolder(PropertyValuesHolder.ofFloat(PROPERTY_TRANSLATION, fromTranslation, toTranslation), PropertyValuesHolder.ofFloat(PROPERTY_SCALE_X, fromScaleX, toScaleX), PropertyValuesHolder.ofFloat(PROPERTY_SCALE_Y, fromScaleY, toScaleY));
        this.mSwapAnimator = ofPropertyValuesHolder;
        ofPropertyValuesHolder.setDuration(350L);
        this.mSwapAnimator.setStartDelay(startDelayDuration);
        this.mSwapAnimator.setInterpolator(new PhysicBasedInterpolator(0.9f, 0.8f));
        this.mSwapAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.server.multiwin.view.MiuiMultiWinClipImageView$$ExternalSyntheticLambda8
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public final void onAnimationUpdate(ValueAnimator valueAnimator2) {
                MiuiMultiWinClipImageView.this.m1046x9b5583bf(valueAnimator2);
            }
        });
        this.mSwapAnimator.addListener(swapAnimationListenerAdapter);
        return this.mSwapAnimator;
    }

    /* renamed from: lambda$createSwapAnimation$0$com-android-server-multiwin-view-MiuiMultiWinClipImageView */
    public /* synthetic */ void m1046x9b5583bf(ValueAnimator animation) {
        updateSwapTranslation(animation.getAnimatedValue(PROPERTY_TRANSLATION));
        updateSwapScale(animation.getAnimatedValue(PROPERTY_SCALE_X), true);
        updateSwapScale(animation.getAnimatedValue(PROPERTY_SCALE_Y), false);
        invalidate();
    }

    public void dropSwapAnimation(int currentSwapAcceptSplitMode) {
        ValueAnimator valueAnimator = this.mSwapAnimator;
        if (valueAnimator != null && valueAnimator.isStarted()) {
            this.mSwapAnimator.cancel();
        }
        float fromTranslation = isLandScape() ? getTranslationX() : getTranslationY();
        float toTranslation = getSwapToMiddleTranslation();
        float fromScaleX = getScaleX();
        float toScaleX = getSwapToScale(currentSwapAcceptSplitMode, true, false, false);
        float fromScaleY = getScaleY();
        float toScaleY = getSwapToScale(currentSwapAcceptSplitMode, false, false, false);
        Slog.d(TAG, "dropSwapAnimation currentSwapAcceptSplitMode=" + currentSwapAcceptSplitMode + ", fromTranslation = " + fromTranslation + ", toTranslation = " + toTranslation + " fromscaleX=" + fromScaleX + " toscaleX=" + toScaleX + " fromscaleY=" + fromScaleY + " toscaleY=" + toScaleY);
        ValueAnimator ofPropertyValuesHolder = ValueAnimator.ofPropertyValuesHolder(PropertyValuesHolder.ofFloat(PROPERTY_TRANSLATION, fromTranslation, toTranslation), PropertyValuesHolder.ofFloat(PROPERTY_SCALE_X, fromScaleX, toScaleX), PropertyValuesHolder.ofFloat(PROPERTY_SCALE_Y, fromScaleY, toScaleY));
        this.mSwapAnimator = ofPropertyValuesHolder;
        ofPropertyValuesHolder.setDuration(350L);
        this.mSwapAnimator.setInterpolator(new FastOutSlowInInterpolator());
        this.mSwapAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.server.multiwin.view.MiuiMultiWinClipImageView$$ExternalSyntheticLambda7
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public final void onAnimationUpdate(ValueAnimator valueAnimator2) {
                MiuiMultiWinClipImageView.this.m1047xc71adc91(valueAnimator2);
            }
        });
        this.mSwapAnimator.start();
    }

    /* renamed from: lambda$dropSwapAnimation$1$com-android-server-multiwin-view-MiuiMultiWinClipImageView */
    public /* synthetic */ void m1047xc71adc91(ValueAnimator animation) {
        updateSwapTranslation(animation.getAnimatedValue(PROPERTY_TRANSLATION));
        updateSwapScale(animation.getAnimatedValue(PROPERTY_SCALE_X), true);
        updateSwapScale(animation.getAnimatedValue(PROPERTY_SCALE_Y), false);
        invalidate();
    }

    public void startDropSplitAnimation(boolean isConsiderNavBar, int navBarHeight, int currentSwapAcceptSplitMode, float splitSwapSize) {
        ValueAnimator valueAnimator = this.mSwapAnimator;
        if (valueAnimator != null && valueAnimator.isStarted()) {
            this.mSwapAnimator.cancel();
        }
        float fromTranslation = isLandScape() ? getTranslationX() : getTranslationY();
        float toTranslation = getDropSplitTranslation(isConsiderNavBar, navBarHeight, currentSwapAcceptSplitMode, splitSwapSize);
        float fromScaleX = getScaleX();
        float fromScaleY = getScaleY();
        Slog.d(TAG, "dropSplitAnimation: mInSwapMode = " + this.mInSwapMode + ", currentSwapAcceptSplitMode = " + currentSwapAcceptSplitMode + ", fromTranslation = " + fromTranslation + ", toTranslation = " + toTranslation + ", fromScaleX = " + fromScaleX + ", toScaleX = 1.0, fromScaleY = " + fromScaleY + ", toScaleY = 1.0");
        ValueAnimator ofPropertyValuesHolder = ValueAnimator.ofPropertyValuesHolder(PropertyValuesHolder.ofFloat(PROPERTY_TRANSLATION, fromTranslation, toTranslation), PropertyValuesHolder.ofFloat(PROPERTY_SCALE_X, fromScaleX, 1.0f), PropertyValuesHolder.ofFloat(PROPERTY_SCALE_Y, fromScaleY, 1.0f));
        this.mSwapAnimator = ofPropertyValuesHolder;
        ofPropertyValuesHolder.setDuration(350L);
        this.mSwapAnimator.setInterpolator(new PhysicBasedInterpolator(0.9f, 0.8f));
        this.mSwapAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.server.multiwin.view.MiuiMultiWinClipImageView$$ExternalSyntheticLambda2
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public final void onAnimationUpdate(ValueAnimator valueAnimator2) {
                MiuiMultiWinClipImageView.this.m1054x3aa5faf7(valueAnimator2);
            }
        });
        this.mSwapAnimator.start();
    }

    /* renamed from: lambda$startDropSplitAnimation$2$com-android-server-multiwin-view-MiuiMultiWinClipImageView */
    public /* synthetic */ void m1054x3aa5faf7(ValueAnimator animation) {
        updateSwapTranslation(animation.getAnimatedValue(PROPERTY_TRANSLATION));
        updateSwapScale(animation.getAnimatedValue(PROPERTY_SCALE_X), true);
        updateSwapScale(animation.getAnimatedValue(PROPERTY_SCALE_Y), false);
        invalidate();
    }

    private float getDropSplitTranslation(boolean isConsiderNavBar, int navBarHeight, int currentSwapAcceptSplitMode, float splitSwapSize) {
        boolean currentSwapIsLeftOrTop;
        if (currentSwapAcceptSplitMode == 1 || currentSwapAcceptSplitMode == 3) {
            currentSwapIsLeftOrTop = true;
        } else {
            currentSwapIsLeftOrTop = false;
        }
        if (isOriginalInLeftOrTopSwapMode() && currentSwapIsLeftOrTop) {
            if (isConsiderNavBar) {
                float toTranslation = splitSwapSize - (navBarHeight / 2);
                return toTranslation;
            }
            return splitSwapSize;
        } else if (isOriginalInLeftOrTopSwapMode() && !currentSwapIsLeftOrTop) {
            return MiuiFreeformPinManagerService.EDGE_AREA;
        } else {
            if (!isOriginalInLeftOrTopSwapMode() && currentSwapIsLeftOrTop) {
                return MiuiFreeformPinManagerService.EDGE_AREA;
            }
            if (isConsiderNavBar) {
                float toTranslation2 = (-splitSwapSize) + (navBarHeight / 2);
                return toTranslation2;
            }
            float toTranslation3 = -splitSwapSize;
            return toTranslation3;
        }
    }

    private ViewGroup getParentViewGroup() {
        ViewParent viewParent = getParent();
        if (!(viewParent instanceof ViewGroup)) {
            return null;
        }
        ViewGroup viewGroup = (ViewGroup) viewParent;
        return viewGroup;
    }

    private float getPushScaleFactorWithSplitBar(float scaleFactorWithSplitBar) {
        int totalLength = isLandScape() ? getWidth() : getHeight();
        if (totalLength <= MiuiFreeformPinManagerService.EDGE_AREA) {
            Slog.w(TAG, "getPushScaleFactorWithSplitBar failed, cause totalLength is less than 0!");
            return 1.0f;
        }
        float splitDividerbarWidth = getContext().getResources().getDimensionPixelSize(285671523);
        return ((totalLength * scaleFactorWithSplitBar) - (splitDividerbarWidth / 2.0f)) / totalLength;
    }

    private float getPushScaleFactor(float scale, boolean isGetWidth, boolean hasBar, int marginCount) {
        int totalLength = isGetWidth ? getWidth() : getHeight();
        int marginSize = Math.round(totalLength * MARGIN_SCALE);
        float f = MiuiFreeformPinManagerService.EDGE_AREA;
        if (totalLength <= MiuiFreeformPinManagerService.EDGE_AREA) {
            Slog.w(TAG, "getPushScaleFactor failed, cause totalLength is less than 0!");
            return 1.0f;
        }
        if (hasBar) {
            f = getContext().getResources().getDimensionPixelSize(285671523);
        }
        float splitDividerbarWidth = f;
        int margin = marginCount * marginSize;
        return (((totalLength * scale) - (splitDividerbarWidth / 2.0f)) - margin) / totalLength;
    }

    private int getSwapToMiddleTranslation() {
        int middleSelf;
        int middleParent;
        ViewGroup viewGroup = getParentViewGroup();
        if (viewGroup == null) {
            return 0;
        }
        if (isLandScape()) {
            middleParent = (int) ((viewGroup.getLeft() + viewGroup.getRight()) / 2.0f);
            middleSelf = (int) ((getLeft() + getRight()) / 2.0f);
            Slog.w(TAG, "getSwapToMiddleTranslation viewGroup left=" + viewGroup.getLeft() + " Right=" + viewGroup.getRight() + "self left=" + getLeft() + " Right=" + getRight() + " des=" + ((Object) getContentDescription()));
        } else {
            int middleParent2 = viewGroup.getTop();
            middleParent = (int) ((middleParent2 + viewGroup.getBottom()) / 2.0f);
            middleSelf = (int) ((getTop() + getBottom()) / 2.0f);
            Slog.w(TAG, "getSwapToMiddleTranslation viewGroup top=" + viewGroup.getTop() + " Bottom=" + viewGroup.getBottom() + "self top=" + getTop() + " Bottom=" + getBottom() + " des=" + ((Object) getContentDescription()));
        }
        return middleParent - middleSelf;
    }

    private int getHadEnteredFreeformTranslation(boolean isInLeftHotArea) {
        ViewGroup viewGroup = getParentViewGroup();
        if (viewGroup == null) {
            return 0;
        }
        int middleParent = isInLeftHotArea ? (int) (((viewGroup.getLeft() + (viewGroup.getWidth() * 0.24f)) + viewGroup.getRight()) / 2.0f) : (int) (((viewGroup.getLeft() + viewGroup.getRight()) - (viewGroup.getWidth() * 0.24f)) / 2.0f);
        int middleSelf = (int) ((getLeft() + getRight()) / 2.0f);
        Slog.w(TAG, "getSwapToMiddleTranslation viewGroup left=" + viewGroup.getLeft() + " Right=" + viewGroup.getRight() + "self left=" + getLeft() + " Right=" + getRight() + " des=" + ((Object) getContentDescription()));
        return middleParent - middleSelf;
    }

    private float getSwapToScale(int currentSwapAcceptSplitMode, boolean isGetWidth, boolean hasMargin, boolean needScale) {
        return getSwapToScale(false, 0, 0, currentSwapAcceptSplitMode, isGetWidth, hasMargin, needScale);
    }

    private float getSwapToScale(boolean isConsiderNavBar, int originSwapMode, int navBarHeight, int currentSwapAcceptSplitMode, boolean isGetWidth, boolean hasMargin, boolean needScale) {
        ViewGroup viewGroup = getParentViewGroup();
        if (viewGroup == null) {
            return isGetWidth ? getScaleX() : getScaleY();
        } else if (currentSwapAcceptSplitMode == 5) {
            int parentSize = isGetWidth ? viewGroup.getWidth() : viewGroup.getHeight();
            int selfSize = isGetWidth ? getWidth() : getHeight();
            float marginSize = MiuiFreeformPinManagerService.EDGE_AREA;
            float swapToSize = selfSize;
            if (hasMargin) {
                marginSize = parentSize * MARGIN_SCALE;
            }
            if ((isLandScape() && isGetWidth) || (!isLandScape() && !isGetWidth)) {
                swapToSize = parentSize;
            }
            Slog.d(TAG, "getSwapToScale marginSize " + marginSize + " isGetWidth " + isGetWidth);
            float swapToScale = (swapToSize - (2.0f * marginSize)) / selfSize;
            return swapToScale;
        } else if (needScale) {
            if (isGetWidth) {
                float swapToScale2 = (viewGroup.getWidth() * 0.76f) / getWidth();
                return swapToScale2;
            }
            float f = 0.971f;
            if (isConsiderNavBar) {
                if (originSwapMode == 3) {
                    float swapToScale3 = (getHeight() - navBarHeight) / getHeight();
                    return swapToScale3;
                } else if (originSwapMode == 4) {
                    float swapToScale4 = (getHeight() + navBarHeight) / getHeight();
                    return swapToScale4;
                } else {
                    if (!hasMargin) {
                        f = 1.0f;
                    }
                    float swapToScale5 = f;
                    return swapToScale5;
                }
            }
            if (!hasMargin) {
                f = 1.0f;
            }
            float swapToScale6 = f;
            return swapToScale6;
        } else if (isConsiderNavBar) {
            if (originSwapMode == 3) {
                float swapToScale7 = (getHeight() - navBarHeight) / getHeight();
                return swapToScale7;
            } else if (originSwapMode == 4) {
                float swapToScale8 = (getHeight() + navBarHeight) / getHeight();
                return swapToScale8;
            } else {
                return 1.0f;
            }
        } else {
            return 1.0f;
        }
    }

    private float getSwapToTranslation(boolean isConsiderNavBar, int navBarHeight, int currentSwapAcceptSplitMode, float splitSwapSize, boolean needScale) {
        int currentSwapAcceptSplitMode2;
        float toTranslation = MiuiFreeformPinManagerService.EDGE_AREA;
        int i = 3;
        int i2 = 1;
        boolean currentSwapIsLeftOrTop = currentSwapAcceptSplitMode == 1 || currentSwapAcceptSplitMode == 3;
        if (currentSwapAcceptSplitMode == 5) {
            this.mInSwapMode = 5;
            return getSwapToMiddleTranslation();
        }
        int i3 = this.mInSwapMode;
        if (i3 == 5 || currentSwapAcceptSplitMode == i3) {
            if (isOriginalInLeftOrTopSwapMode() && currentSwapIsLeftOrTop) {
                if (needScale) {
                    toTranslation = getHadEnteredFreeformTranslation(currentSwapIsLeftOrTop);
                } else {
                    toTranslation = isConsiderNavBar ? splitSwapSize - (navBarHeight / 2.0f) : splitSwapSize;
                }
            } else if (isOriginalInLeftOrTopSwapMode() && !currentSwapIsLeftOrTop) {
                if (needScale) {
                    toTranslation = getHadEnteredFreeformTranslation(currentSwapIsLeftOrTop);
                } else {
                    toTranslation = MiuiFreeformPinManagerService.EDGE_AREA;
                }
            } else if (!isOriginalInLeftOrTopSwapMode() && currentSwapIsLeftOrTop) {
                if (needScale) {
                    toTranslation = getHadEnteredFreeformTranslation(currentSwapIsLeftOrTop);
                } else {
                    toTranslation = MiuiFreeformPinManagerService.EDGE_AREA;
                }
            } else if (needScale) {
                toTranslation = getHadEnteredFreeformTranslation(currentSwapIsLeftOrTop);
            } else {
                toTranslation = isConsiderNavBar ? (-splitSwapSize) + (navBarHeight / 2.0f) : -splitSwapSize;
            }
        }
        if (isLandScape()) {
            if (currentSwapIsLeftOrTop) {
                i2 = 2;
            }
            currentSwapAcceptSplitMode2 = i2;
        } else {
            if (currentSwapIsLeftOrTop) {
                i = 4;
            }
            currentSwapAcceptSplitMode2 = i;
        }
        this.mInSwapMode = currentSwapAcceptSplitMode2;
        return toTranslation;
    }

    private void init(Context context) {
        this.mRoundCornerRadius = context.getResources().getDimensionPixelSize(285671519);
        int borderWidth = context.getResources().getDimensionPixelSize(285671515);
        if (borderWidth > 0) {
            Paint paint = new Paint();
            this.mBorderPaint = paint;
            paint.setColor(context.getResources().getColor(285605964));
            this.mBorderPaint.setStyle(Paint.Style.STROKE);
            this.mBorderPaint.setStrokeWidth(borderWidth * 2.0f);
            this.mBorderPaint.setStrokeCap(Paint.Cap.ROUND);
        }
    }

    private boolean isOriginalInLeftOrTopSwapMode() {
        int i = this.mOriginalInSwapMode;
        return i == 1 || i == 3;
    }

    private void updatePushScaleX(Object scaleObjX) {
        if (!(scaleObjX instanceof Float)) {
            return;
        }
        float scaleX = ((Float) scaleObjX).floatValue();
        setScaleX(scaleX);
    }

    private void updatePushScaleY(Object scaleObjY) {
        if (!(scaleObjY instanceof Float)) {
            return;
        }
        float scaleY = ((Float) scaleObjY).floatValue();
        setScaleY(scaleY);
    }

    private void updatePushTranslation(Object translationObj) {
        if (!(translationObj instanceof Float)) {
            return;
        }
        float translation = ((Float) translationObj).floatValue();
        if (isLandScape()) {
            setTranslationX(translation);
        } else {
            setTranslationY(translation);
        }
    }

    private void updatePushTranslation(Object translationObj, boolean isLandScape) {
        if (!(translationObj instanceof Float)) {
            return;
        }
        float translation = ((Float) translationObj).floatValue();
        if (isLandScape) {
            setTranslationX(translation);
        } else {
            setTranslationY(translation);
        }
    }

    private void updateSwapScale(Object scaleObj) {
        if (!(scaleObj instanceof Float)) {
            return;
        }
        float scale = ((Float) scaleObj).floatValue();
        if (isLandScape()) {
            setScaleX(scale);
        } else {
            setScaleY(scale);
        }
    }

    private void updateSwapScale(Object scaleObj, boolean isScaleWidth) {
        if (!(scaleObj instanceof Float)) {
            return;
        }
        float scale = ((Float) scaleObj).floatValue();
        if (isScaleWidth) {
            setScaleX(scale);
        } else {
            setScaleY(scale);
        }
    }

    private void updateSwapTranslation(Object translationObj) {
        if (!(translationObj instanceof Float)) {
            return;
        }
        float translation = ((Float) translationObj).floatValue();
        if (isLandScape()) {
            setTranslationX(translation);
        } else {
            setTranslationY(translation);
        }
    }

    public void addSelf() {
        if (!this.mHasRemovedSelf) {
            return;
        }
        ViewParent viewParent = getParent();
        if (viewParent instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) viewParent;
            setVisibility(0);
            viewGroup.addView(this);
            this.mHasRemovedSelf = false;
            Slog.d(TAG, "add self = " + this + ", parent = " + viewGroup);
        }
    }

    public void disableBorder() {
        this.mIsToDrawBorder = false;
    }

    protected void drawOutlineBorder(Canvas canvas) {
        Paint paint = this.mBorderPaint;
        if (paint != null && this.mIsToDrawBorder) {
            canvas.drawPath(this.mRoundCornerClipPath, paint);
        }
    }

    public float getFinalPushTranslation(float finalScaleFactor) {
        if (isLandScape()) {
            return (getWidth() * (1.0f - finalScaleFactor)) / 2.0f;
        }
        return (getHeight() * (1.0f - finalScaleFactor)) / 2.0f;
    }

    public float getFullScreenShotScaleFactor2() {
        return 0.95f;
    }

    public float getPrePushScaleFactor1() {
        return 0.75f;
    }

    public int getOriginalInSwapMode() {
        return this.mOriginalInSwapMode;
    }

    public float getPrePushWidth() {
        return getPrePushWidthByScaleFactor(getPushScaleFactorWithSplitBar(0.5f), false);
    }

    public float getPrePushWidthByScaleFactor(float scaleFactor, boolean hasMargin) {
        int totalLength = isLandScape() ? getWidth() : getHeight();
        int marginSize = hasMargin ? Math.round(totalLength * MARGIN_SCALE) : 0;
        if (isLandScape()) {
            float PrePushWidth = ((getWidth() * (1.0f - scaleFactor)) / 2.0f) - marginSize;
            return PrePushWidth;
        }
        float PrePushWidth2 = ((getHeight() * (1.0f - scaleFactor)) / 2.0f) - marginSize;
        return PrePushWidth2;
    }

    public boolean isHasBeenResizedWithoutNavBar() {
        return this.mHasBeenResizedWithoutNavBar;
    }

    protected boolean isLandScape() {
        return this.mIsLandScape;
    }

    @Override // android.view.View
    public void onDraw(Canvas canvas) {
        canvas.save();
        clipForRoundCorner(canvas);
        drawSnapShot(canvas);
        drawBg(canvas);
        if (!(this instanceof MiuiMultiWinPushPendingDropView)) {
            drawOutlineBorder(canvas);
        }
        Drawable drawable = this.mIcon;
        if (drawable != null && drawable.getAlpha() > 0) {
            drawBlurRadiatedIcon(canvas);
            drawIcon(canvas);
            if (!isFullScreen()) {
                drawDragBar(canvas);
            }
        }
        canvas.restore();
    }

    private void drawDragBar(Canvas canvas) {
        Drawable bar;
        canvas.save();
        if (MiuiMultiWindowUtils.isPadScreen(getContext())) {
            bar = getResources().getDrawable(285737424);
        } else {
            bar = getResources().getDrawable(285737423);
        }
        float scaleX = 1.0f / getScaleX();
        float scaleY = 1.0f / getScaleY();
        bar.setBounds(0, 0, bar.getIntrinsicWidth(), bar.getIntrinsicHeight());
        float topMargin = getDragBarTopMargin();
        canvas.translate((getWidth() - (bar.getBounds().width() * scaleX)) / 2.0f, topMargin * scaleY);
        canvas.scale(scaleX, scaleY);
        bar.draw(canvas);
        canvas.restore();
    }

    private float getDragBarTopMargin() {
        if (MiuiMultiWindowUtils.isPadScreen(getContext()) || MiuiMultiWindowUtils.isFoldInnerScreen(getContext())) {
            return MiuiMultiWinClientUtils.DEFAULT_SPLIT_DRAG_BAR_TOP_MARGIN;
        }
        if (isOnPrimary()) {
            return getResources().getDimensionPixelSize(17105574);
        }
        return MiuiMultiWinClientUtils.BOTTOM_SPLIT_DRAG_BAR_TOP_MARGIN + MiuiMultiWinClientUtils.VERTICAL_SPLIT_DRAG_BAR_TOP_MARGIN;
    }

    private void drawIcon(Canvas canvas) {
        Drawable drawable = this.mIcon;
        if (drawable != null && drawable.getAlpha() > 0) {
            canvas.save();
            float scaleX = 1.0f / getScaleX();
            float scaleY = 1.0f / getScaleY();
            canvas.translate((getWidth() - (this.mIcon.getBounds().width() * scaleX)) / 2.0f, (getHeight() - (this.mIcon.getBounds().height() * scaleY)) / 2.0f);
            canvas.scale(scaleX, scaleY);
            this.mIcon.draw(canvas);
            canvas.restore();
        }
    }

    private void drawBlurRadiatedIcon(Canvas canvas) {
        Drawable drawable = this.mIcon;
        if (drawable != null && drawable.getAlpha() > 0 && this.mBlurRadiatedIconBitmap != null) {
            canvas.save();
            float scaleX = 1.0f / getScaleX();
            float scaleY = 1.0f / getScaleY();
            canvas.translate(getWidth() / 2, getHeight() / 2);
            canvas.scale(scaleX, scaleY);
            Drawable drawable2 = this.mIcon;
            if (drawable2 != null) {
                float width = drawable2.getBounds().width();
                float scale = (4.0f * width) / this.mBlurRadiatedIconBitmap.getWidth();
                canvas.scale(scale, scale);
            } else {
                canvas.scale(6.0f, 6.0f);
            }
            canvas.translate((-this.mBlurRadiatedIconBitmap.getWidth()) / 2, (-this.mBlurRadiatedIconBitmap.getWidth()) / 2);
            this.blurRadiatedIconPaint.setAlpha(this.mIcon.getAlpha());
            if (this.mIcon.getAlpha() == 255) {
                canvas.drawBitmap(this.mBlurRadiatedIconBitmap, MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA, (Paint) null);
            } else {
                canvas.drawBitmap(this.mBlurRadiatedIconBitmap, MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA, this.blurRadiatedIconPaint);
            }
            canvas.restore();
        }
    }

    private void drawSnapShot(Canvas canvas) {
        Bitmap bitmap = this.mSnapShot;
        if (bitmap != null && this.mSnapShotAlpha > MiuiFreeformPinManagerService.EDGE_AREA) {
            canvas.drawBitmap(bitmap, MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA, (Paint) null);
        }
    }

    private void drawBg(Canvas canvas) {
        Drawable drawable = this.mBgDrawable;
        if (drawable != null) {
            float f = this.mBackgroundAlpha;
            if (f > MiuiFreeformPinManagerService.EDGE_AREA) {
                drawable.setAlpha((int) (f * 255.0f));
                this.mBgDrawable.draw(canvas);
            }
        }
    }

    public void playFinalPushAnimation(int currentPushAcceptSplitMode) {
        Slog.d(TAG, "play final push animation now: currentPushAcceptSplitMode = " + currentPushAcceptSplitMode);
        float finalScaleFactor = getPushScaleFactorWithSplitBar(0.5f);
        float toScaleX = isLandScape() ? finalScaleFactor : 1.0f;
        float toScaleY = isLandScape() ? 1.0f : finalScaleFactor;
        float finalPushWidth = getFinalPushTranslation(finalScaleFactor);
        if (currentPushAcceptSplitMode == 1 || currentPushAcceptSplitMode == 3) {
            playFinalPushAnimationInternal(finalPushWidth, toScaleX, toScaleY, 350L);
        } else if (currentPushAcceptSplitMode == 2 || currentPushAcceptSplitMode == 4) {
            playFinalPushAnimationInternal(-finalPushWidth, toScaleX, toScaleY, 350L);
        } else if (currentPushAcceptSplitMode == 5) {
            playFinalPushAnimationInternal(MiuiFreeformPinManagerService.EDGE_AREA, 1.0f, 1.0f, 350L);
        }
    }

    public void playFinalPushAnimationInternal(float toTranslation, float toScaleX, float toScaleY, long duration) {
        ValueAnimator valueAnimator = this.mFinalPushAnimator;
        if (valueAnimator != null && valueAnimator.isStarted()) {
            this.mFinalPushAnimator.cancel();
        }
        float fromTranslation = isLandScape() ? getTranslationX() : getTranslationY();
        if (fromTranslation == toTranslation && getScaleX() == toScaleX && getScaleY() == toScaleY) {
            return;
        }
        Slog.d(TAG, "playFinalPushAnimationInternal: , fromTranslation = " + fromTranslation + ", toTranslation = " + toTranslation);
        ValueAnimator ofPropertyValuesHolder = ValueAnimator.ofPropertyValuesHolder(PropertyValuesHolder.ofFloat(PROPERTY_TRANSLATION, fromTranslation, toTranslation), PropertyValuesHolder.ofFloat(PROPERTY_SCALE_X, getScaleX(), toScaleX), PropertyValuesHolder.ofFloat(PROPERTY_SCALE_Y, getScaleY(), toScaleY));
        this.mFinalPushAnimator = ofPropertyValuesHolder;
        ofPropertyValuesHolder.setDuration(duration);
        this.mFinalPushAnimator.setInterpolator(new FastOutSlowInInterpolator());
        this.mFinalPushAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.server.multiwin.view.MiuiMultiWinClipImageView$$ExternalSyntheticLambda0
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public final void onAnimationUpdate(ValueAnimator valueAnimator2) {
                MiuiMultiWinClipImageView.this.m1049xb76883f2(valueAnimator2);
            }
        });
        this.mFinalPushAnimator.start();
    }

    /* renamed from: lambda$playFinalPushAnimationInternal$3$com-android-server-multiwin-view-MiuiMultiWinClipImageView */
    public /* synthetic */ void m1049xb76883f2(ValueAnimator animation) {
        updatePushTranslation(animation.getAnimatedValue(PROPERTY_TRANSLATION));
        updatePushScaleX(animation.getAnimatedValue(PROPERTY_SCALE_X));
        updatePushScaleY(animation.getAnimatedValue(PROPERTY_SCALE_Y));
        invalidate();
    }

    private void playBgDrawableAlphaAnimation(float toAlpha, int duration) {
        ValueAnimator valueAnimator = this.mBgDrawableAlphaAnimation;
        if (valueAnimator != null && valueAnimator.isStarted()) {
            this.mBgDrawableAlphaAnimation.cancel();
        }
        if (this.mBackgroundAlpha == toAlpha) {
            return;
        }
        Slog.d(TAG, "playBgDrawableAlphaAnimation: , fromBackgroundAlpha = " + this.mBackgroundAlpha + ", toBackgroundAlpha = " + toAlpha);
        ValueAnimator ofPropertyValuesHolder = ValueAnimator.ofPropertyValuesHolder(PropertyValuesHolder.ofFloat("mBackgroundAlpha", this.mBackgroundAlpha, toAlpha));
        this.mBgDrawableAlphaAnimation = ofPropertyValuesHolder;
        ofPropertyValuesHolder.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.server.multiwin.view.MiuiMultiWinClipImageView$$ExternalSyntheticLambda5
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public final void onAnimationUpdate(ValueAnimator valueAnimator2) {
                MiuiMultiWinClipImageView.this.m1048xeb5fb1f9(valueAnimator2);
            }
        });
        this.mBgDrawableAlphaAnimation.setDuration(duration);
        this.mBgDrawableAlphaAnimation.setInterpolator(new FastOutSlowInInterpolator());
        this.mBgDrawableAlphaAnimation.start();
    }

    /* renamed from: lambda$playBgDrawableAlphaAnimation$4$com-android-server-multiwin-view-MiuiMultiWinClipImageView */
    public /* synthetic */ void m1048xeb5fb1f9(ValueAnimator param1ValueAnimator) {
        Object bgAlpha = param1ValueAnimator.getAnimatedValue("mBackgroundAlpha");
        if (bgAlpha instanceof Float) {
            setBgDrawableAlpha(((Float) bgAlpha).floatValue());
        }
        invalidate();
    }

    private void playSnapShotAlphaAnimation(float toAlpha, int duration) {
        ValueAnimator valueAnimator = this.mSnapShotAlphaAnimation;
        if (valueAnimator != null && valueAnimator.isStarted()) {
            this.mSnapShotAlphaAnimation.cancel();
        }
        if (this.mSnapShotAlpha == toAlpha) {
            return;
        }
        Slog.d(TAG, "playSnapShotAlphaAnimation: , fromsnapShotAlpha = " + this.mSnapShotAlpha + ", tosnapShotAlpha = " + toAlpha);
        ValueAnimator ofPropertyValuesHolder = ValueAnimator.ofPropertyValuesHolder(PropertyValuesHolder.ofFloat("snapShotalpha", this.mSnapShotAlpha, toAlpha));
        this.mSnapShotAlphaAnimation = ofPropertyValuesHolder;
        ofPropertyValuesHolder.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.server.multiwin.view.MiuiMultiWinClipImageView$$ExternalSyntheticLambda4
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public final void onAnimationUpdate(ValueAnimator valueAnimator2) {
                MiuiMultiWinClipImageView.this.m1053x9f92897(valueAnimator2);
            }
        });
        this.mSnapShotAlphaAnimation.setDuration(duration);
        this.mSnapShotAlphaAnimation.setInterpolator(new FastOutSlowInInterpolator());
        this.mSnapShotAlphaAnimation.start();
    }

    /* renamed from: lambda$playSnapShotAlphaAnimation$5$com-android-server-multiwin-view-MiuiMultiWinClipImageView */
    public /* synthetic */ void m1053x9f92897(ValueAnimator param1ValueAnimator) {
        Object snapShotAlpha = param1ValueAnimator.getAnimatedValue("snapShotalpha");
        if (snapShotAlpha instanceof Float) {
            setSnapShotAlpha(((Float) snapShotAlpha).floatValue());
        }
        invalidate();
    }

    public void playFullScreenShotDismissAnimation() {
        playBgDrawableAlphaAnimation(1.0f, MiuiMultiWinUtils.DEFAULT_ANIMATION_DURATION);
        playSnapShotAlphaAnimation(MiuiFreeformPinManagerService.EDGE_AREA, MiuiMultiWinUtils.DEFAULT_ANIMATION_DURATION);
    }

    public void playFullScreenShotShowAnimation() {
        playBgDrawableAlphaAnimation(MiuiFreeformPinManagerService.EDGE_AREA, MiuiMultiWinUtils.DEFAULT_ANIMATION_DURATION);
        playSnapShotAlphaAnimation(1.0f, MiuiMultiWinUtils.DEFAULT_ANIMATION_DURATION);
    }

    public void playIconAlphaAnimation(float toAlpha, int duration) {
        if (this.mIcon != null) {
            ValueAnimator valueAnimator = this.mIconAlphaAnimator;
            if (valueAnimator != null && valueAnimator.isStarted()) {
                this.mIconAlphaAnimator.cancel();
            }
            if (this.mIcon.getAlpha() == ((int) (toAlpha * 255.0f))) {
                return;
            }
            Slog.d(TAG, "playIconShowAnimation: , fromiconAlpha = " + this.mIcon.getAlpha() + ", toiconAlpha = " + ((int) (toAlpha * 255.0f)));
            ValueAnimator ofPropertyValuesHolder = ValueAnimator.ofPropertyValuesHolder(PropertyValuesHolder.ofFloat("iconAlpha", this.mIcon.getAlpha() / 255.0f, toAlpha));
            this.mIconAlphaAnimator = ofPropertyValuesHolder;
            ofPropertyValuesHolder.setDuration(duration);
            this.mIconAlphaAnimator.setInterpolator(new PhysicBasedInterpolator(0.9f, 0.857f));
            this.mIconAlphaAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.server.multiwin.view.MiuiMultiWinClipImageView$$ExternalSyntheticLambda6
                @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                public final void onAnimationUpdate(ValueAnimator valueAnimator2) {
                    MiuiMultiWinClipImageView.this.m1050xa897a9c1(valueAnimator2);
                }
            });
            this.mIconAlphaAnimator.start();
        }
    }

    /* renamed from: lambda$playIconAlphaAnimation$6$com-android-server-multiwin-view-MiuiMultiWinClipImageView */
    public /* synthetic */ void m1050xa897a9c1(ValueAnimator animation) {
        if (this.mIcon != null) {
            Object alphaObj = animation.getAnimatedValue("iconAlpha");
            float alpha = 1.0f;
            if (alphaObj instanceof Float) {
                alpha = ((Float) alphaObj).floatValue();
            }
            this.mIcon.setAlpha((int) (255.0f * alpha));
            invalidate();
        }
    }

    public void playPrePushAnimation(int currentPushAcceptSplitMode) {
        float preScaleX;
        float preScaleWithBar;
        float preScaleY;
        float prePushWidth;
        Slog.d(TAG, "playPrePushAnimation now: currentPushAcceptSplitMode = " + currentPushAcceptSplitMode);
        this.currentPushAcceptSplitMode = currentPushAcceptSplitMode;
        float prePushScaleFactor = currentPushAcceptSplitMode == 5 ? 1.0f : 0.75f;
        boolean hasBar = currentPushAcceptSplitMode != 5;
        int marginCount = currentPushAcceptSplitMode == 5 ? 2 : 1;
        if (isLandScape()) {
            float preScaleX2 = getPushScaleFactor(prePushScaleFactor, true, hasBar, marginCount);
            preScaleY = getPushScaleFactor(1.0f, false, false, 2);
            preScaleWithBar = preScaleX2;
            preScaleX = preScaleX2;
        } else {
            float preScaleX3 = getPushScaleFactor(1.0f, true, false, 2);
            float preScaleY2 = getPushScaleFactor(prePushScaleFactor, false, hasBar, marginCount);
            preScaleY = preScaleY2;
            preScaleWithBar = preScaleY2;
            preScaleX = preScaleX3;
        }
        float prePushWidth2 = getPrePushWidthByScaleFactor(preScaleWithBar, true);
        Slog.d(TAG, "playPrePushAnimation now: prePushScaleFactor = " + prePushScaleFactor + " prePushWidth=" + prePushWidth2);
        if (currentPushAcceptSplitMode == 1 || currentPushAcceptSplitMode == 3) {
            prePushWidth = prePushWidth2;
            playPrePushAnimationInternal(prePushWidth2, 350L, preScaleX, preScaleY);
        } else {
            prePushWidth = prePushWidth2;
        }
        if (currentPushAcceptSplitMode == 2 || currentPushAcceptSplitMode == 4) {
            playPrePushAnimationInternal(-prePushWidth, 350L, preScaleX, preScaleY);
        }
        if (currentPushAcceptSplitMode == 5) {
            playPrePushAnimationInternal(MiuiFreeformPinManagerService.EDGE_AREA, 350L, preScaleX, preScaleY);
        }
    }

    public void playPrePushAnimationInternal(float toTranslation, long duration, float toScaleX, float toScaleY) {
        ValueAnimator valueAnimator = this.mPrePushAnimator;
        if (valueAnimator != null && valueAnimator.isStarted()) {
            this.mPrePushAnimator.cancel();
        }
        float fromTranslation = isLandScape() ? getTranslationX() : getTranslationY();
        if (fromTranslation == toTranslation && getScaleX() == toScaleX && getScaleY() == toScaleY) {
            return;
        }
        Slog.d(TAG, "playPushAnimation: , fromTranslation = " + fromTranslation + ", toTranslation = " + toTranslation);
        ValueAnimator ofPropertyValuesHolder = ValueAnimator.ofPropertyValuesHolder(PropertyValuesHolder.ofFloat(PROPERTY_TRANSLATION, fromTranslation, toTranslation), PropertyValuesHolder.ofFloat(PROPERTY_SCALE_X, getScaleX(), toScaleX), PropertyValuesHolder.ofFloat(PROPERTY_SCALE_Y, getScaleY(), toScaleY));
        this.mPrePushAnimator = ofPropertyValuesHolder;
        ofPropertyValuesHolder.setDuration(duration);
        this.mPrePushAnimator.setInterpolator(new FastOutSlowInInterpolator());
        this.mPrePushAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.server.multiwin.view.MiuiMultiWinClipImageView$$ExternalSyntheticLambda3
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public final void onAnimationUpdate(ValueAnimator valueAnimator2) {
                MiuiMultiWinClipImageView.this.m1051xa02b111b(valueAnimator2);
            }
        });
        this.mPrePushAnimator.start();
    }

    /* renamed from: lambda$playPrePushAnimationInternal$7$com-android-server-multiwin-view-MiuiMultiWinClipImageView */
    public /* synthetic */ void m1051xa02b111b(ValueAnimator animation) {
        updatePushTranslation(animation.getAnimatedValue(PROPERTY_TRANSLATION));
        updatePushScaleX(animation.getAnimatedValue(PROPERTY_SCALE_X));
        updatePushScaleY(animation.getAnimatedValue(PROPERTY_SCALE_Y));
        invalidate();
    }

    public void playRoundCornerDismissAnimation() {
        ValueAnimator valueAnimator = ValueAnimator.ofPropertyValuesHolder(PropertyValuesHolder.ofFloat("roundCornerRadius", this.mRoundCornerRadius, MiuiFreeformPinManagerService.EDGE_AREA));
        valueAnimator.setDuration(350L);
        valueAnimator.setInterpolator(new FastOutSlowInInterpolator());
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.server.multiwin.view.MiuiMultiWinClipImageView$$ExternalSyntheticLambda1
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public final void onAnimationUpdate(ValueAnimator valueAnimator2) {
                MiuiMultiWinClipImageView.this.m1052xa5e48763(valueAnimator2);
            }
        });
        valueAnimator.start();
    }

    /* renamed from: lambda$playRoundCornerDismissAnimation$8$com-android-server-multiwin-view-MiuiMultiWinClipImageView */
    public /* synthetic */ void m1052xa5e48763(ValueAnimator param1ValueAnimator) {
        Object radiusObj = param1ValueAnimator.getAnimatedValue("roundCornerRadius");
        if (radiusObj instanceof Float) {
            this.mRoundCornerRadius = ((Float) radiusObj).floatValue();
            invalidate();
        }
    }

    public void playSwapAnimation(boolean isConsiderNavBar, int originSwapMode, int navBarHeight, int currentSwapAcceptSplitMode, float splitSwapSize, long startDelayDuration, AnimatorListenerAdapter swapAnimationListenerAdapter, boolean needScale) {
        if (this.mLastSwapAcceptSplitMode == currentSwapAcceptSplitMode) {
            return;
        }
        Animator swapAnimator = createSwapAnimation(isConsiderNavBar, originSwapMode, navBarHeight, currentSwapAcceptSplitMode, splitSwapSize, startDelayDuration, swapAnimationListenerAdapter, needScale);
        if (swapAnimator != null) {
            swapAnimator.start();
        }
        this.mLastSwapAcceptSplitMode = currentSwapAcceptSplitMode;
    }

    public void removeSelf() {
        if (this.mHasRemovedSelf) {
            return;
        }
        ViewParent viewParent = getParent();
        if (viewParent instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) viewParent;
            viewGroup.removeView(this);
            this.mHasRemovedSelf = true;
            Slog.d(TAG, "remove self = " + this + ", parent = " + viewGroup);
        }
    }

    public void setHasBeenResizedWithoutNavBar(boolean hasBeenResizedWithNavBar) {
        this.mHasBeenResizedWithoutNavBar = hasBeenResizedWithNavBar;
    }

    public void setHasRemovedSelf(boolean hasRemovedSelf) {
        this.mHasRemovedSelf = hasRemovedSelf;
    }

    public void setIcon(Drawable icon) {
        this.mIcon = icon;
        if (icon != null) {
            icon.setAlpha(0);
        }
    }

    public void setInSwapMode(int inSwapMode) {
        this.mInSwapMode = inSwapMode;
        this.mOriginalInSwapMode = inSwapMode;
    }

    public void setIsLandScape(boolean isLandScape) {
        this.mIsLandScape = isLandScape;
    }

    public boolean isLandOrientation() {
        return getContext().getResources().getConfiguration().orientation == 2;
    }

    public void setSnapShotBitmap(Bitmap bitmap) {
        this.mSnapShot = bitmap;
    }

    public void setBgDrawable(Drawable backgroundDrawable) {
        this.mBgDrawable = backgroundDrawable;
    }

    public Drawable getBgDrawable() {
        return this.mBgDrawable;
    }

    public void setBgDrawableAlpha(float alpha) {
        this.mBackgroundAlpha = alpha;
        if (alpha < MiuiFreeformPinManagerService.EDGE_AREA) {
            this.mBackgroundAlpha = MiuiFreeformPinManagerService.EDGE_AREA;
        }
        if (this.mBackgroundAlpha > 1.0f) {
            this.mBackgroundAlpha = 1.0f;
        }
    }

    @Override // android.view.View
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        Drawable drawable = this.mBgDrawable;
        if (drawable != null && w > 0 && h > 0) {
            drawable.setBounds(0, 0, w, h);
        }
    }

    public Bitmap getSnapShotBitmap() {
        return this.mSnapShot;
    }

    public float getSnapShotAlpha() {
        return this.mSnapShotAlpha;
    }

    public void setSnapShotAlpha(float snapShotAlpha) {
        this.mSnapShotAlpha = snapShotAlpha;
        if (snapShotAlpha < MiuiFreeformPinManagerService.EDGE_AREA) {
            this.mSnapShotAlpha = MiuiFreeformPinManagerService.EDGE_AREA;
        }
        if (this.mSnapShotAlpha > 1.0f) {
            this.mSnapShotAlpha = 1.0f;
        }
    }

    public void setBlurRadiatedIconBitmap(Bitmap bitmap) {
        this.mBlurRadiatedIconBitmap = bitmap;
    }

    private boolean isOnPrimary() {
        int i;
        return this.currentPushAcceptSplitMode == 0 && ((i = this.mInSwapMode) == 1 || i == 3);
    }

    private boolean isFullScreen() {
        return this.mInSwapMode == 5 || this.currentPushAcceptSplitMode == 5;
    }
}
