package com.android.server.multiwin.view;

import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Slog;
import android.view.DragEvent;
import com.android.server.multiwin.MiuiMultiWinUtils;
import com.android.server.multiwin.animation.MiuiMultiWinSplitBarController;
import com.android.server.multiwin.animation.interpolator.SharpCurveInterpolator;
import com.android.server.wm.MiuiFreeformPinManagerService;
import com.android.server.wm.PhysicBasedInterpolator;
/* loaded from: classes.dex */
public class MiuiMultiWinPushPendingDropView extends MiuiMultiWinHotAreaView {
    private static final String TAG = "MiuiMultiWinPushPendingDropView";
    public static int barWidth;
    private Rect mPushAcceptBound;
    private MiuiMultiWinClipImageView mPushTarget;
    private Drawable mShadow;
    private float mShadowAlpha;
    private Rect mShadowClipRect;
    private Path mShadowPath;
    private float mShadowRoundCorner;
    private float mShadowScaleX;
    private float mShadowScaleY;
    private float mShadowShowAlpha;
    private ValueAnimator mShadowShowAnimator;

    public MiuiMultiWinPushPendingDropView(Context context, Rect pushAcceptBound, MiuiMultiWinClipImageView pushTarget, int splitMode, Point pendingDropSize) {
        super(context);
        this.mShadowClipRect = new Rect();
        this.mPushAcceptBound = pushAcceptBound;
        this.mPushTarget = pushTarget;
        this.mSplitMode = splitMode;
        this.mShadow = new ColorDrawable(Color.parseColor("#FFFFFFFF"));
        barWidth = getContext().getResources().getDimensionPixelSize(285671523);
        initShadowClipRect(pendingDropSize);
        this.mIsToDrawBorder = true;
        if (MiuiMultiWinUtils.isInNightMode(context)) {
            this.mShadowShowAlpha = 0.1f;
        } else {
            this.mShadowShowAlpha = 0.55f;
        }
    }

    public MiuiMultiWinPushPendingDropView(Context context) {
        super(context);
        this.mShadowClipRect = new Rect();
    }

    public MiuiMultiWinPushPendingDropView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mShadowClipRect = new Rect();
    }

    public MiuiMultiWinPushPendingDropView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mShadowClipRect = new Rect();
    }

    private void initShadowClipRect(Point pendingDropSize) {
        if (pendingDropSize == null) {
            Slog.w(TAG, "initShadowClipRect failed, cause pendingDropSize or mPushTarget is null!");
            return;
        }
        int shadowLength = Math.round(this.mPushTarget.getPrePushWidth() * 2.0f) - getContext().getResources().getDimensionPixelSize(285671523);
        int width = isLeftRightSplit() ? shadowLength : this.mPushAcceptBound.width();
        int height = isLeftRightSplit() ? this.mPushAcceptBound.height() : shadowLength;
        int pendingDropWidth = pendingDropSize.x;
        int pendingDropHeight = pendingDropSize.y;
        if (this.mSplitMode == 1 || this.mSplitMode == 3) {
            this.mShadowClipRect = new Rect(0, 0, width, height);
        } else if (this.mSplitMode != 2 && this.mSplitMode != 4) {
            Slog.w(TAG, "initShadowClipRect: no shadow to draw!");
            this.mShadowClipRect = new Rect();
        } else {
            this.mShadowClipRect = new Rect(pendingDropWidth - width, pendingDropHeight - height, pendingDropWidth, pendingDropHeight);
        }
        float f = 1.0f;
        this.mShadowScaleX = isLeftRightSplit() ? 1.0f : this.mPushTarget.getFullScreenShotScaleFactor2();
        if (isLeftRightSplit()) {
            f = this.mPushTarget.getFullScreenShotScaleFactor2();
        }
        this.mShadowScaleY = f;
        this.mShadowRoundCorner = this.mRoundCornerRadius;
        this.mShadowPath = new Path();
    }

    private boolean isDragIntoSwapAcceptBound(float dragX, float dragY) {
        return isInSwapAcceptBound(dragX, dragY);
    }

    private boolean isDragOutOfSwapAcceptBound(float dragX, float dragY) {
        return !isInSwapAcceptBound(dragX, dragY);
    }

    private boolean isInSwapAcceptBound(float x, float y) {
        Rect rect = this.mPushAcceptBound;
        if (rect != null) {
            return x >= ((float) rect.left) && x <= ((float) this.mPushAcceptBound.right) && y >= ((float) this.mPushAcceptBound.top) && y <= ((float) this.mPushAcceptBound.bottom);
        }
        Slog.w("PushPendingDropView", "isInSwapAcceptBound return false, cause mSwapAcceptBound is null");
        return false;
    }

    private boolean isLeftRightSplit() {
        return this.mSplitMode == 1 || this.mSplitMode == 2;
    }

    @Override // com.android.server.multiwin.view.MiuiMultiWinClipImageView
    protected void drawOutlineBorder(Canvas canvas) {
        if (this.mBorderPaint != null && this.mIsToDrawBorder) {
            this.mBorderPaint.setAlpha((int) (this.mShadowAlpha * 255.0f));
            canvas.drawPath(this.mShadowPath, this.mBorderPaint);
        }
    }

    @Override // com.android.server.multiwin.view.MiuiMultiWinHotAreaView
    public void handleDragEntered(DragEvent dragEvent, int dragSurfaceAnimType) {
    }

    @Override // com.android.server.multiwin.view.MiuiMultiWinHotAreaView
    public void handleDragExited(DragEvent dragEvent) {
        super.handleDragExited(dragEvent);
    }

    @Override // com.android.server.multiwin.view.MiuiMultiWinHotAreaView
    public void handleDragLocation(DragEvent dragEvent) {
        Slog.w(TAG, "handleDragLocation");
        super.handleDragLocation(dragEvent);
        int[] loc = new int[2];
        getLocationOnScreen(loc);
        float dragX = dragEvent.getX() + loc[0];
        float dragY = dragEvent.getY() + loc[1];
        if (isDragOutOfSwapAcceptBound(dragX, dragY)) {
            MiuiMultiWinSplitBarController miuiMultiWinSplitBarController = this.mSplitBarController;
            removeSelf();
        } else if (isDragIntoSwapAcceptBound(dragX, dragY)) {
            addSelf();
        }
    }

    @Override // com.android.server.multiwin.view.MiuiMultiWinHotAreaView
    public void handleDrop(DragEvent paramDragEvent, int dragSurfaceAnimType, boolean isUpDownSplitDrop) {
        Slog.d(TAG, "handleDrop: mSplitMode = " + this.mSplitMode);
        if (this.mSplitMode == 5) {
            dragSurfaceAnimType = 2;
        }
        super.handleDrop(paramDragEvent, dragSurfaceAnimType, isUpDownSplitDrop);
        MiuiMultiWinClipImageView miuiMultiWinClipImageView = this.mPushTarget;
        if (miuiMultiWinClipImageView != null) {
            miuiMultiWinClipImageView.playFinalPushAnimation(this.mSplitMode);
            if (this.mSplitMode == 5) {
                this.mPushTarget.playFullScreenShotShowAnimation();
                this.mPushTarget.playIconAlphaAnimation(MiuiFreeformPinManagerService.EDGE_AREA, MiuiMultiWinUtils.DEFAULT_ANIMATION_DURATION);
            }
        }
        MiuiMultiWinSplitBarController miuiMultiWinSplitBarController = this.mSplitBarController;
    }

    @Override // com.android.server.multiwin.view.MiuiMultiWinClipImageView, android.view.View
    public void onDraw(Canvas canvas) {
        this.mShadow.setBounds(this.mShadowClipRect);
        this.mShadow.setAlpha((int) (this.mShadowAlpha * 255.0f));
        this.mShadowPath.reset();
        float f = this.mShadowRoundCorner;
        this.mShadowPath.addRoundRect(this.mShadowClipRect.left, this.mShadowClipRect.top, this.mShadowClipRect.right, this.mShadowClipRect.bottom, f, f, Path.Direction.CW);
        super.onDraw(canvas);
        canvas.save();
        canvas.scale(this.mShadowScaleX, this.mShadowScaleY, this.mShadowClipRect.width() / 2.0f, this.mShadowClipRect.height() / 2.0f);
        canvas.clipPath(this.mShadowPath, Region.Op.INTERSECT);
        this.mShadow.draw(canvas);
        drawOutlineBorder(canvas);
        canvas.restore();
    }

    public void playShadowCompleteShowAnimation() {
        Slog.w(TAG, "playShadowCompleteShowAnimation");
        if (MiuiMultiWinUtils.isInNightMode(getContext())) {
            return;
        }
        ValueAnimator valueAnimator = this.mShadowShowAnimator;
        if (valueAnimator != null) {
            valueAnimator.cancel();
        }
        ValueAnimator ofPropertyValuesHolder = ValueAnimator.ofPropertyValuesHolder(PropertyValuesHolder.ofFloat("shadowAlpha", this.mShadowAlpha, 1.0f));
        this.mShadowShowAnimator = ofPropertyValuesHolder;
        ofPropertyValuesHolder.setDuration(350L);
        this.mShadowShowAnimator.setInterpolator(new PhysicBasedInterpolator(0.9f, 0.857f));
        this.mShadowShowAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.server.multiwin.view.MiuiMultiWinPushPendingDropView$$ExternalSyntheticLambda1
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public final void onAnimationUpdate(ValueAnimator valueAnimator2) {
                MiuiMultiWinPushPendingDropView.this.m1056x373a207b(valueAnimator2);
            }
        });
        this.mShadowShowAnimator.start();
    }

    /* renamed from: lambda$playShadowCompleteShowAnimation$0$com-android-server-multiwin-view-MiuiMultiWinPushPendingDropView */
    public /* synthetic */ void m1056x373a207b(ValueAnimator param1ValueAnimator) {
        Object object = param1ValueAnimator.getAnimatedValue("shadowAlpha");
        float shadowAlpha = 1.0f;
        if (object instanceof Float) {
            shadowAlpha = ((Float) object).floatValue();
        }
        this.mShadowAlpha = shadowAlpha;
        invalidate();
    }

    public void playShadowShowAnimation() {
        Slog.w(TAG, "playShadowShowAnimation");
        ValueAnimator valueAnimator = this.mShadowShowAnimator;
        if (valueAnimator != null) {
            valueAnimator.cancel();
        }
        ValueAnimator ofPropertyValuesHolder = ValueAnimator.ofPropertyValuesHolder(PropertyValuesHolder.ofFloat("shadowAlpha", MiuiFreeformPinManagerService.EDGE_AREA, this.mShadowShowAlpha));
        this.mShadowShowAnimator = ofPropertyValuesHolder;
        ofPropertyValuesHolder.setDuration(250L);
        this.mShadowShowAnimator.setStartDelay(150L);
        this.mShadowShowAnimator.setInterpolator(new SharpCurveInterpolator());
        this.mShadowShowAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.server.multiwin.view.MiuiMultiWinPushPendingDropView$$ExternalSyntheticLambda0
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public final void onAnimationUpdate(ValueAnimator valueAnimator2) {
                MiuiMultiWinPushPendingDropView.this.m1057x62c42e41(valueAnimator2);
            }
        });
        this.mShadowShowAnimator.start();
    }

    /* renamed from: lambda$playShadowShowAnimation$1$com-android-server-multiwin-view-MiuiMultiWinPushPendingDropView */
    public /* synthetic */ void m1057x62c42e41(ValueAnimator animation) {
        Object shadowAlphaObj = animation.getAnimatedValue("shadowAlpha");
        float shadowAlpha = 1.0f;
        if (shadowAlphaObj instanceof Float) {
            shadowAlpha = ((Float) shadowAlphaObj).floatValue();
        }
        this.mShadowAlpha = shadowAlpha;
        invalidate();
    }
}
