package com.android.server.multiwin.animation;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.PropertyValuesHolder;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.util.MiuiMultiWinClientUtils;
import android.util.MiuiMultiWindowUtils;
import android.util.Slog;
import android.view.InputChannel;
import android.view.View;
import android.view.animation.LinearInterpolator;
import com.android.server.multiwin.animation.MiuiMultiWinDragAnimationAdapter;
import com.android.server.multiwin.animation.interpolator.SharpCurveInterpolator;
import com.android.server.wm.MiuiFreeformPinManagerService;
import com.android.server.wm.PhysicBasedInterpolator;
import java.lang.ref.WeakReference;
/* loaded from: classes.dex */
public class MiuiMultiWinDragAnimationAdapter {
    private static final boolean DBG = false;
    public static final long DEFAULT_STARTDELAY_ANIMATION_DURATION = 0;
    public static final float FREE_FORM_ENTER_SPLIT_LANDSCAPE_HEIGHT_SCALE = 0.7f;
    public static final float FREE_FORM_ENTER_SPLIT_LANDSCAPE_WIDTH_SCALE = 0.72f;
    public static final float FREE_FORM_ENTER_SPLIT_VERTICAL_HEIGHT_SCALE = 0.42f;
    public static final float FREE_FORM_ENTER_SPLIT_VERTICAL_WIDTH_SCALE = 0.73f;
    public static final float FREE_FORM_ENTER_SPLIT_VERTICAL_WIDTH_SCALE2 = 0.86f;
    public static final float SPLIT_ENTER_SPLIT_HEIGHT_SCALE = 0.93f;
    public static final float SPLIT_ENTER_SPLIT_WIDTH_SCALE = 0.9f;
    public static final long SPLIT_TO_FREEFORM_ANIMATION_DURATION = 200;
    public static final int START_DRAGGING_FROM_FREE_FORM_AREA = 1;
    private static final String TAG = "MiuiMultiWinDragAnimationAdapter";
    private Drawable mBackground;
    private Bitmap mBlurRadiatedIconBitmap;
    private Drawable mBorder;
    private Drawable mBottomBar;
    private int mBottomBarBottomMargin;
    private View mCaptionView;
    private Rect mDefaultFreeFormDropBound;
    private Rect mDisplayBounds;
    private Bitmap mDragBarBmp;
    private float mDragBarTopMargin;
    private Point mDraggingClipSize;
    private int mFreeFormDragBarTopMargin;
    private Rect mFreeFromDragBarRect;
    private View mHostView;
    private Drawable mIconDrawable;
    private boolean mIsLandScape;
    private MiuiDragShadowBuilder mMiuiDragShadowBuilder;
    private boolean mRootTaskIsSplit;
    private Bitmap mScreenShot;
    private Rect mSplitDragBarRect;
    private int mSplitDragBarTopMargin;
    private int mSplitMode;

    /* loaded from: classes.dex */
    public interface ScaleUpAnimationListener {
        void onAnimationDone();
    }

    /* loaded from: classes.dex */
    public static class MiuiDragShadowBuilder extends View.DragShadowBuilder {
        public static final int DRAGGING_ANIM_TYPE = 1;
        public static final int DROP_FREE_FORM_ANIM_TYPE = 2;
        public static final int DROP_FULL_SCREEN_ANIM_TYPE = 4;
        public static final int DROP_SPLIT_SCREEN_ANIM_TYPE = 3;
        private static final String PROPERTY_CLIPRECT = "cliprect";
        private static final String PROPERTY_ICONDRAWMIDX = "iconDrawMidX";
        private static final String PROPERTY_ICONDRAWMIDY = "iconDrawMidY";
        private ValueAnimator mAnchorAnimator;
        private WeakReference mBackground;
        private float mBackgroundAlpha;
        private ValueAnimator mBackgroundAlphaAnimator;
        private int mBackgroundHeight;
        private int mBackgroundWidth;
        private WeakReference mBlurRadiateIconBitmap;
        private float mBlurRadiatedIconAlpha;
        private ValueAnimator mBlurRadiatedIconAlphaAnimator;
        private WeakReference mBorder;
        private WeakReference mBottomBar;
        private float mBottomBarAlpha;
        private ValueAnimator mBottomBarAlphaAnimator;
        private int mBottomBarBottomMargin;
        private ValueAnimator mClipAnimator;
        private int mClipHeight;
        private RectF mClipRect;
        private int mClipWidth;
        private Rect mDefaultFreeFormDropBounds;
        private WeakReference mDragBar;
        private ValueAnimator mDragBarAnimator;
        private Rect mDragBarDstBound;
        private ValueAnimator mDragBarRectAnimator;
        private Rect mDragBarSrcBound;
        private float mDragBarTopMargin;
        private ValueAnimator mDragBarTopMarginAnimator;
        private int mDragSurfaceHeight;
        private int mDragSurfaceWidth;
        private int mFreeFormDragBarTopMargin;
        private int mFreeFormDraggingClipHeight;
        private int mFreeFormDraggingClipWidth;
        private float mFreeFormRoundCornerRadius;
        private Rect mFreeFromDragBarRect;
        private float mFreeformFactor;
        private WeakReference mHostView;
        private WeakReference mIcon;
        private float mIconAlpha;
        private ValueAnimator mIconAlphaAnimator;
        private float mIconDrawMidX;
        private float mIconDrawMidY;
        private float mInitialClipHeight;
        private float mInitialClipWidth;
        private boolean mIsLandScape;
        private boolean mIsSplitCaptionViewDragged;
        private float mRoundCornerR;
        private ValueAnimator mRoundCornerRAnimator;
        private WeakReference mScreenShot;
        private Rect mScreenShotDstBound;
        private Rect mScreenShotSrcBound;
        private ValueAnimator mSnapShotAlphaAnimator;
        private final float mSplitBarWidth;
        private Rect mSplitDragBarRect;
        private int mSplitDragBarTopMargin;
        private float mSplitRoundCornerRadius;
        private int mTouchPointX;
        private int mTouchPointY;
        private float mSnapShortAlpha = 1.0f;
        Paint blurRadiatedIconPaint = new Paint();
        private float mDragBarAlpha = 1.0f;
        private Paint mDragBarPaint = new Paint();
        private Path mClipPath = new Path();
        private boolean mIsIconKeepUpWidthClipRect = true;
        private float[] mIconDrawPos = new float[2];

        /* loaded from: classes.dex */
        public static class RectFTypeEvaluator implements TypeEvaluator<RectF> {
            RectFTypeEvaluator() {
            }

            public RectF evaluate(float fraction, RectF startValue, RectF endValue) {
                return new RectF(((endValue.left - startValue.left) * fraction) + startValue.left, ((endValue.top - startValue.top) * fraction) + startValue.top, ((endValue.right - startValue.right) * fraction) + startValue.right, ((endValue.bottom - startValue.bottom) * fraction) + startValue.bottom);
            }
        }

        /* loaded from: classes.dex */
        public static class RectTypeEvaluator implements TypeEvaluator<Rect> {
            RectTypeEvaluator() {
            }

            public Rect evaluate(float fraction, Rect startValue, Rect endValue) {
                return new Rect((int) (((endValue.left - startValue.left) * fraction) + startValue.left + 0.5d), (int) (((endValue.top - startValue.top) * fraction) + startValue.top + 0.5d), (int) (((endValue.right - startValue.right) * fraction) + startValue.right + 0.5d), (int) (((endValue.bottom - startValue.bottom) * fraction) + startValue.bottom + 0.5d));
            }
        }

        public MiuiDragShadowBuilder setmDefaultFreeFormDropBounds(Rect mDefaultFreeFormDropBounds) {
            this.mDefaultFreeFormDropBounds = mDefaultFreeFormDropBounds;
            return this;
        }

        MiuiDragShadowBuilder(View hostView, Point dragSurfaceSize, Point clipSize, Point touchOffset, boolean isSplitCaptionViewDragged) {
            this.mFreeformFactor = 0.6f;
            this.mHostView = new WeakReference(hostView);
            this.mDragSurfaceWidth = dragSurfaceSize.x;
            this.mDragSurfaceHeight = dragSurfaceSize.y;
            this.mClipWidth = clipSize.x;
            this.mClipHeight = clipSize.y;
            this.mInitialClipWidth = clipSize.x;
            this.mInitialClipHeight = clipSize.y;
            if (!isSplitCaptionViewDragged) {
                this.mTouchPointX = this.mDragSurfaceWidth / 2;
            } else {
                this.mTouchPointX = touchOffset.x + ((int) ((this.mDragSurfaceWidth - this.mInitialClipWidth) / 2.0f));
            }
            this.mTouchPointY = touchOffset.y;
            if (this.mTouchPointX < 0) {
                Slog.w(MiuiMultiWinDragAnimationAdapter.TAG, "MiuiDragShadowBuilder init: mTouchPointX is negative! mDragSurfaceWidth = " + this.mDragSurfaceWidth + ", mInitialClipWidth = " + this.mInitialClipWidth + ",  touchOffset.x = " + touchOffset.x);
                this.mTouchPointX = 0;
            }
            if (this.mTouchPointY < 0) {
                Slog.w(MiuiMultiWinDragAnimationAdapter.TAG, "MiuiDragShadowBuilder init: mTouchPointY is negative! mDragSurfaceHeight = " + this.mDragSurfaceHeight + ", mInitialClipHeight = " + this.mInitialClipHeight + ",  touchOffset.y = " + touchOffset.y);
                this.mTouchPointY = 0;
            }
            float surfaceCenterX = this.mDragSurfaceWidth / 2.0f;
            int i = this.mClipWidth;
            this.mClipRect = new RectF(surfaceCenterX - (i / 2.0f), MiuiFreeformPinManagerService.EDGE_AREA, (i / 2.0f) + surfaceCenterX, this.mClipHeight);
            Context context = hostView.getContext();
            if (MiuiMultiWindowUtils.isPadScreen(context)) {
                this.mFreeformFactor = 0.5f;
            }
            this.mSplitBarWidth = context.getResources().getDimensionPixelSize(285671527);
            this.mFreeFormRoundCornerRadius = MiuiMultiWindowUtils.getFreeformRoundCorner(context);
            float dimensionPixelSize = context.getResources().getDimensionPixelSize(285671519);
            this.mSplitRoundCornerRadius = dimensionPixelSize;
            this.mRoundCornerR = !isSplitCaptionViewDragged ? this.mFreeFormRoundCornerRadius : dimensionPixelSize;
            this.mIsSplitCaptionViewDragged = isSplitCaptionViewDragged;
            this.mBackgroundAlpha = MiuiFreeformPinManagerService.EDGE_AREA;
        }

        public MiuiDragShadowBuilder animate() {
            return this;
        }

        public MiuiDragShadowBuilder animateBgAlpha(float alpha) {
            this.mBackgroundAlpha = alpha;
            if (alpha < MiuiFreeformPinManagerService.EDGE_AREA) {
                this.mBackgroundAlpha = MiuiFreeformPinManagerService.EDGE_AREA;
            }
            if (this.mBackgroundAlpha > 1.0f) {
                this.mBackgroundAlpha = 1.0f;
            }
            return this;
        }

        public MiuiDragShadowBuilder animateSnapShortAlpha(float alpha) {
            this.mSnapShortAlpha = alpha;
            if (alpha < MiuiFreeformPinManagerService.EDGE_AREA) {
                this.mSnapShortAlpha = MiuiFreeformPinManagerService.EDGE_AREA;
            }
            if (this.mSnapShortAlpha > 1.0f) {
                this.mSnapShortAlpha = 1.0f;
            }
            return this;
        }

        public MiuiDragShadowBuilder animateBlurRadiatedIconAlpha(float alpha) {
            this.mBlurRadiatedIconAlpha = alpha;
            if (alpha < MiuiFreeformPinManagerService.EDGE_AREA) {
                this.mBlurRadiatedIconAlpha = MiuiFreeformPinManagerService.EDGE_AREA;
            }
            if (this.mBlurRadiatedIconAlpha > 1.0f) {
                this.mBlurRadiatedIconAlpha = 1.0f;
            }
            return this;
        }

        public MiuiDragShadowBuilder animateDragBarAlpha(float dragBarAlpha) {
            this.mDragBarAlpha = dragBarAlpha;
            if (dragBarAlpha < MiuiFreeformPinManagerService.EDGE_AREA) {
                this.mDragBarAlpha = MiuiFreeformPinManagerService.EDGE_AREA;
            }
            if (this.mDragBarAlpha > 1.0f) {
                this.mDragBarAlpha = 1.0f;
            }
            return this;
        }

        public MiuiDragShadowBuilder animateIconAlpha(float iconAlpha) {
            this.mIconAlpha = iconAlpha;
            if (iconAlpha < MiuiFreeformPinManagerService.EDGE_AREA) {
                this.mIconAlpha = MiuiFreeformPinManagerService.EDGE_AREA;
            }
            if (this.mIconAlpha > 1.0f) {
                this.mIconAlpha = 1.0f;
            }
            return this;
        }

        public MiuiDragShadowBuilder animateBottomBarAlpha(float bottomBarAlpha) {
            this.mBottomBarAlpha = bottomBarAlpha;
            if (bottomBarAlpha < MiuiFreeformPinManagerService.EDGE_AREA) {
                this.mBottomBarAlpha = MiuiFreeformPinManagerService.EDGE_AREA;
            }
            if (this.mBottomBarAlpha > 1.0f) {
                this.mBottomBarAlpha = 1.0f;
            }
            return this;
        }

        public void getIconDrawPositionByClipRect(float[] outPos, Drawable icon) {
            if (icon == null) {
                return;
            }
            Rect rect = icon.getBounds();
            int w = rect.right - rect.left;
            int h = rect.bottom - rect.top;
            float tx = ((this.mClipRect.left + this.mClipRect.right) / 2.0f) - (w / 2.0f);
            float ty = ((this.mClipRect.top + this.mClipRect.bottom) / 2.0f) - (h / 2.0f);
            outPos[0] = tx;
            outPos[1] = ty;
            this.mIconDrawMidX = (this.mClipRect.left + this.mClipRect.right) / 2.0f;
            this.mIconDrawMidY = (this.mClipRect.top + this.mClipRect.bottom) / 2.0f;
        }

        public void getIconDrawPositionBySelf(float[] outPos, Drawable icon) {
            if (icon == null) {
                return;
            }
            Rect rect = icon.getBounds();
            int w = rect.right - rect.left;
            int h = rect.bottom - rect.top;
            float tx = this.mIconDrawMidX - (w / 2.0f);
            float ty = this.mIconDrawMidY - (h / 2.0f);
            outPos[0] = tx;
            outPos[1] = ty;
        }

        private void onDrawBackground(Canvas canvas, Drawable background) {
            if (this.mIsSplitCaptionViewDragged && this.mScreenShot == null) {
                this.mBackgroundAlpha = 1.0f;
            }
            float f = this.mBackgroundAlpha;
            if (f > MiuiFreeformPinManagerService.EDGE_AREA && background != null) {
                background.setAlpha((int) (f * 255.0f));
                background.draw(canvas);
            }
        }

        private void onDrawBorder(Canvas canvas, Drawable border) {
            if (border instanceof GradientDrawable) {
                ((GradientDrawable) border).setCornerRadius(this.mRoundCornerR);
                border.setBounds(new Rect((int) this.mClipRect.left, (int) this.mClipRect.top, (int) this.mClipRect.right, (int) this.mClipRect.bottom));
                border.draw(canvas);
            }
        }

        private void onDrawDragBar(Canvas canvas, Bitmap dragBarBmp) {
            if (dragBarBmp == null) {
                Slog.d(MiuiMultiWinDragAnimationAdapter.TAG, "debug onDrawDragBar: dragBarBmp is null!");
                return;
            }
            float f = this.mDragBarAlpha;
            if (f > MiuiFreeformPinManagerService.EDGE_AREA) {
                if (f > 1.0f) {
                    this.mDragBarAlpha = 1.0f;
                }
                canvas.save();
                float clipMidX = (this.mClipRect.left + this.mClipRect.right) / 2.0f;
                float dx = clipMidX - (this.mDragBarDstBound.width() / 2.0f);
                float dy = this.mClipRect.top + this.mDragBarTopMargin;
                canvas.translate(dx, dy);
                this.mDragBarPaint.reset();
                this.mDragBarPaint.setAlpha((int) (this.mDragBarAlpha * 255.0f));
                canvas.drawBitmap(dragBarBmp, this.mDragBarSrcBound, this.mDragBarDstBound, this.mDragBarPaint);
                canvas.restore();
            }
        }

        private void onDrawBottomBar(Canvas canvas, Drawable bottomBar) {
            if (bottomBar == null) {
                Slog.d(MiuiMultiWinDragAnimationAdapter.TAG, "debug onDrawBottomBar: bottomBar is null!");
                return;
            }
            float f = this.mBottomBarAlpha;
            if (f > MiuiFreeformPinManagerService.EDGE_AREA) {
                if (f > 1.0f) {
                    this.mBottomBarAlpha = 1.0f;
                }
                Rect rect = bottomBar.getBounds();
                int w = rect.right - rect.left;
                int h = rect.bottom - rect.top;
                float tx = ((this.mClipRect.left + this.mClipRect.right) / 2.0f) - (w / 2.0f);
                float ty = (this.mClipRect.bottom - this.mBottomBarBottomMargin) - h;
                canvas.save();
                canvas.translate(tx, ty);
                bottomBar.setAlpha((int) (this.mBottomBarAlpha * 255.0f));
                bottomBar.draw(canvas);
                canvas.restore();
            }
        }

        private void onDrawIcon(Canvas canvas, Drawable icon) {
            if (icon == null) {
                Slog.d(MiuiMultiWinDragAnimationAdapter.TAG, "debug onDrawIcon: icon is null!");
                return;
            }
            float f = this.mIconAlpha;
            if (f > MiuiFreeformPinManagerService.EDGE_AREA) {
                if (f > 1.0f) {
                    this.mIconAlpha = 1.0f;
                }
                if (this.mIsIconKeepUpWidthClipRect) {
                    getIconDrawPositionByClipRect(this.mIconDrawPos, icon);
                } else {
                    getIconDrawPositionBySelf(this.mIconDrawPos, icon);
                }
                canvas.save();
                float[] fArr = this.mIconDrawPos;
                canvas.translate(fArr[0], fArr[1]);
                icon.setAlpha((int) (this.mIconAlpha * 255.0f));
                icon.draw(canvas);
                canvas.restore();
            }
        }

        private void onDrawScreenShot(Canvas canvas, Bitmap screenShot) {
            if (screenShot != null && this.mSnapShortAlpha > MiuiFreeformPinManagerService.EDGE_AREA) {
                this.mScreenShotDstBound.left = (int) this.mClipRect.left;
                this.mScreenShotDstBound.top = (int) this.mClipRect.top;
                this.mScreenShotDstBound.right = (int) this.mClipRect.right;
                this.mScreenShotDstBound.bottom = (int) this.mClipRect.bottom;
                canvas.drawBitmap(screenShot, this.mScreenShotSrcBound, this.mScreenShotDstBound, (Paint) null);
            }
        }

        @Override // android.view.View.DragShadowBuilder
        public void onDrawShadow(Canvas canvas) {
            onDrawShadowCanvas(canvas);
        }

        private void onDrawShadowCanvas(Canvas canvas) {
            this.mClipPath.reset();
            Path path = this.mClipPath;
            RectF rectF = this.mClipRect;
            float f = this.mRoundCornerR;
            path.addRoundRect(rectF, f, f, Path.Direction.CW);
            canvas.save();
            canvas.clipPath(this.mClipPath);
            WeakReference weakReference = this.mScreenShot;
            Drawable drawable = null;
            onDrawScreenShot(canvas, weakReference == null ? null : (Bitmap) weakReference.get());
            WeakReference weakReference2 = this.mBackground;
            onDrawBackground(canvas, weakReference2 == null ? null : (Drawable) weakReference2.get());
            WeakReference weakReference3 = this.mBlurRadiateIconBitmap;
            onDrawRadiateIcon(canvas, weakReference3 == null ? null : (Bitmap) weakReference3.get());
            WeakReference weakReference4 = this.mBorder;
            onDrawBorder(canvas, weakReference4 == null ? null : (Drawable) weakReference4.get());
            canvas.restore();
            WeakReference weakReference5 = this.mDragBar;
            onDrawDragBar(canvas, weakReference5 == null ? null : (Bitmap) weakReference5.get());
            WeakReference weakReference6 = this.mIcon;
            onDrawIcon(canvas, weakReference6 == null ? null : (Drawable) weakReference6.get());
            WeakReference weakReference7 = this.mBottomBar;
            if (weakReference7 != null) {
                drawable = (Drawable) weakReference7.get();
            }
            onDrawBottomBar(canvas, drawable);
        }

        private void onDrawRadiateIcon(Canvas canvas, Bitmap blurRadiateIconBitmap) {
            if (blurRadiateIconBitmap != null) {
                float f = this.mBlurRadiatedIconAlpha;
                if (f > MiuiFreeformPinManagerService.EDGE_AREA) {
                    if (f > 1.0f) {
                        this.mBlurRadiatedIconAlpha = 1.0f;
                    }
                    this.blurRadiatedIconPaint.setAlpha((int) (this.mBlurRadiatedIconAlpha * 255.0f));
                    canvas.save();
                    float clipMidX = (this.mClipRect.left + this.mClipRect.right) / 2.0f;
                    float clipMidY = (this.mClipRect.top + this.mClipRect.bottom) / 2.0f;
                    canvas.translate(clipMidX, clipMidY);
                    if (this.mIcon.get() != null) {
                        Drawable iconDrawable = (Drawable) this.mIcon.get();
                        float width = iconDrawable.getBounds().width();
                        float scale = (4.0f * width) / blurRadiateIconBitmap.getWidth();
                        canvas.scale(scale, scale);
                    } else {
                        canvas.scale(6.0f, 6.0f);
                    }
                    canvas.translate((-blurRadiateIconBitmap.getWidth()) / 2, (-blurRadiateIconBitmap.getHeight()) / 2);
                    if (this.mBlurRadiatedIconAlpha == 1.0f) {
                        canvas.drawBitmap(blurRadiateIconBitmap, MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA, (Paint) null);
                    } else {
                        canvas.drawBitmap(blurRadiateIconBitmap, MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA, this.blurRadiatedIconPaint);
                    }
                    canvas.restore();
                }
            }
        }

        @Override // android.view.View.DragShadowBuilder
        public void onProvideShadowMetrics(Point outShadowSize, Point outShadowTouchPoint) {
            Drawable background = (Drawable) this.mBackground.get();
            if (background != null) {
                background.setBounds(0, 0, this.mBackgroundWidth, this.mBackgroundHeight);
            }
            outShadowSize.set(this.mDragSurfaceWidth, this.mDragSurfaceHeight);
            outShadowTouchPoint.set(this.mTouchPointX, this.mTouchPointY);
        }

        public MiuiDragShadowBuilder setBackground(Drawable background, Point backgroundSize) {
            this.mBackground = new WeakReference(background);
            this.mBackgroundWidth = backgroundSize.x;
            this.mBackgroundHeight = backgroundSize.y;
            return this;
        }

        public MiuiDragShadowBuilder setBorder(Drawable background) {
            this.mBorder = new WeakReference(background);
            return this;
        }

        public MiuiDragShadowBuilder setDragBar(Bitmap dragBarBmp, float dragBarTopMargin) {
            if (dragBarBmp != null) {
                this.mDragBar = new WeakReference(dragBarBmp);
                this.mDragBarSrcBound = new Rect(0, 0, dragBarBmp.getWidth(), dragBarBmp.getHeight());
                this.mDragBarDstBound = new Rect(0, 0, dragBarBmp.getWidth(), dragBarBmp.getHeight());
                this.mDragBarTopMargin = dragBarTopMargin;
            }
            return this;
        }

        public MiuiDragShadowBuilder setBottomBar(Drawable bottomBarBmp, int bottomBarBottomMargin) {
            this.mBottomBar = new WeakReference(bottomBarBmp);
            this.mBottomBarBottomMargin = bottomBarBottomMargin;
            return this;
        }

        public MiuiDragShadowBuilder setDraggingClipSize(Point draggingClipSize) {
            this.mFreeFormDraggingClipWidth = draggingClipSize.x;
            this.mFreeFormDraggingClipHeight = draggingClipSize.y;
            return this;
        }

        public MiuiDragShadowBuilder setIsLandScape(boolean isLandScape) {
            this.mIsLandScape = isLandScape;
            return this;
        }

        public MiuiDragShadowBuilder adjustInitialRect() {
            int i = this.mTouchPointX;
            int i2 = this.mClipWidth;
            this.mClipRect = new RectF(i - (i2 / 2.0f), MiuiFreeformPinManagerService.EDGE_AREA, (i2 / 2.0f) + i, this.mClipHeight);
            Slog.i(MiuiMultiWinDragAnimationAdapter.TAG, " adjustInitialRect mClipRect " + this.mClipRect + " mClipWidth " + this.mClipWidth + " mClipHeight " + this.mClipHeight);
            return this;
        }

        public MiuiDragShadowBuilder setIcon(Drawable icon) {
            this.mIcon = new WeakReference(icon);
            this.mIconDrawMidX = (this.mClipRect.left + this.mClipRect.right) / 2.0f;
            this.mIconDrawMidY = (this.mClipRect.top + this.mClipRect.bottom) / 2.0f;
            return this;
        }

        public MiuiDragShadowBuilder setBlurRadiateIconBitmap(Bitmap blurRadiateIconBitmap) {
            this.mBlurRadiateIconBitmap = new WeakReference(blurRadiateIconBitmap);
            return this;
        }

        public MiuiDragShadowBuilder setScreenShot(Bitmap screenShot) {
            if (screenShot != null) {
                this.mScreenShot = new WeakReference(screenShot);
                this.mScreenShotSrcBound = new Rect(0, 0, screenShot.getWidth(), screenShot.getHeight());
                this.mScreenShotDstBound = new Rect();
            }
            return this;
        }

        public MiuiDragShadowBuilder setSplitDragBarTopMargin(int mSplitDragBarTopMargin) {
            this.mSplitDragBarTopMargin = mSplitDragBarTopMargin;
            return this;
        }

        public MiuiDragShadowBuilder setFreeFormDragBarTopMargin(int mFreeFormDragBarTopMargin) {
            this.mFreeFormDragBarTopMargin = mFreeFormDragBarTopMargin;
            return this;
        }

        public MiuiDragShadowBuilder setSplitDragBarRect(Rect mSplitDragBarRect) {
            this.mSplitDragBarRect = mSplitDragBarRect;
            return this;
        }

        public MiuiDragShadowBuilder setFreeFromDragBarRect(Rect mFreeFromDragBarRect) {
            this.mFreeFromDragBarRect = mFreeFromDragBarRect;
            return this;
        }

        public void startAnchorAnimation(long duration) {
            ValueAnimator valueAnimator = this.mAnchorAnimator;
            if (valueAnimator != null && valueAnimator.isStarted()) {
                this.mAnchorAnimator.cancel();
            }
            Slog.d(MiuiMultiWinDragAnimationAdapter.TAG, "startAnchorAnimation: , duration = " + duration);
            ValueAnimator ofPropertyValuesHolder = ValueAnimator.ofPropertyValuesHolder(PropertyValuesHolder.ofInt("mAnchor", 0, 1));
            this.mAnchorAnimator = ofPropertyValuesHolder;
            ofPropertyValuesHolder.setDuration(duration);
            this.mAnchorAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.server.multiwin.animation.MiuiMultiWinDragAnimationAdapter$MiuiDragShadowBuilder$$ExternalSyntheticLambda4
                @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                public final void onAnimationUpdate(ValueAnimator valueAnimator2) {
                    MiuiMultiWinDragAnimationAdapter.MiuiDragShadowBuilder.this.m1034x86c06e46(valueAnimator2);
                }
            });
            this.mAnchorAnimator.start();
        }

        /* renamed from: lambda$startAnchorAnimation$0$com-android-server-multiwin-animation-MiuiMultiWinDragAnimationAdapter$MiuiDragShadowBuilder */
        public /* synthetic */ void m1034x86c06e46(ValueAnimator animation) {
            View view = (View) this.mHostView.get();
            if (view != null) {
                MiuiDragShadowBuilder builder = animateBgAlpha(this.mBackgroundAlpha);
                view.updateDragShadow(builder);
            }
        }

        public void startBackgroundAlphaAnimation(float toAlpha, long duration, long startDelayDuration) {
            ValueAnimator valueAnimator = this.mBackgroundAlphaAnimator;
            if (valueAnimator != null && valueAnimator.isStarted()) {
                this.mBackgroundAlphaAnimator.cancel();
            }
            if (this.mBackgroundAlpha == toAlpha) {
                return;
            }
            Slog.d(MiuiMultiWinDragAnimationAdapter.TAG, "startBackgroundAlphaAnimation: fromBackgroundAlpha = " + this.mBackgroundAlpha + ", toBackgroundAlpha = " + toAlpha);
            ValueAnimator ofPropertyValuesHolder = ValueAnimator.ofPropertyValuesHolder(PropertyValuesHolder.ofFloat("mBackgroundAlpha", this.mBackgroundAlpha, toAlpha));
            this.mBackgroundAlphaAnimator = ofPropertyValuesHolder;
            ofPropertyValuesHolder.setDuration(duration);
            this.mBackgroundAlphaAnimator.setStartDelay(startDelayDuration);
            this.mBackgroundAlphaAnimator.setInterpolator(new PhysicBasedInterpolator(0.9f, 0.857f));
            this.mBackgroundAlphaAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.server.multiwin.animation.MiuiMultiWinDragAnimationAdapter$MiuiDragShadowBuilder$$ExternalSyntheticLambda0
                @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                public final void onAnimationUpdate(ValueAnimator valueAnimator2) {
                    MiuiMultiWinDragAnimationAdapter.MiuiDragShadowBuilder.this.m1035xc3775156(valueAnimator2);
                }
            });
            this.mBackgroundAlphaAnimator.start();
        }

        /* renamed from: lambda$startBackgroundAlphaAnimation$1$com-android-server-multiwin-animation-MiuiMultiWinDragAnimationAdapter$MiuiDragShadowBuilder */
        public /* synthetic */ void m1035xc3775156(ValueAnimator animation) {
            Object alphaValue = animation.getAnimatedValue("mBackgroundAlpha");
            if (alphaValue instanceof Float) {
                this.mBackgroundAlpha = ((Float) alphaValue).floatValue();
            }
            View view = (View) this.mHostView.get();
            if (view != null) {
                MiuiDragShadowBuilder builder = animateBgAlpha(this.mBackgroundAlpha);
                ValueAnimator valueAnimator = this.mAnchorAnimator;
                if (valueAnimator != null && valueAnimator.isRunning()) {
                    return;
                }
                view.updateDragShadow(builder);
            }
        }

        public void startSnapShotAlphaAnimation(float toAlpha, long duration, long startDelayDuration) {
            ValueAnimator valueAnimator = this.mSnapShotAlphaAnimator;
            if (valueAnimator != null && valueAnimator.isStarted()) {
                this.mSnapShotAlphaAnimator.cancel();
            }
            if (this.mSnapShortAlpha == toAlpha) {
                return;
            }
            Slog.d(MiuiMultiWinDragAnimationAdapter.TAG, "startSnapShotAlphaAnimation: fromAlpha = " + this.mSnapShortAlpha + ", toAlpha = " + toAlpha);
            ValueAnimator ofPropertyValuesHolder = ValueAnimator.ofPropertyValuesHolder(PropertyValuesHolder.ofFloat("mSnapShortAlpha", this.mSnapShortAlpha, toAlpha));
            this.mSnapShotAlphaAnimator = ofPropertyValuesHolder;
            ofPropertyValuesHolder.setDuration(duration);
            this.mSnapShotAlphaAnimator.setStartDelay(startDelayDuration);
            this.mSnapShotAlphaAnimator.setInterpolator(new PhysicBasedInterpolator(0.9f, 0.857f));
            this.mSnapShotAlphaAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.server.multiwin.animation.MiuiMultiWinDragAnimationAdapter$MiuiDragShadowBuilder$$ExternalSyntheticLambda3
                @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                public final void onAnimationUpdate(ValueAnimator valueAnimator2) {
                    MiuiMultiWinDragAnimationAdapter.MiuiDragShadowBuilder.this.m1044x774972bf(valueAnimator2);
                }
            });
            this.mSnapShotAlphaAnimator.start();
        }

        /* renamed from: lambda$startSnapShotAlphaAnimation$2$com-android-server-multiwin-animation-MiuiMultiWinDragAnimationAdapter$MiuiDragShadowBuilder */
        public /* synthetic */ void m1044x774972bf(ValueAnimator animation) {
            Object alphaValue = animation.getAnimatedValue("mSnapShortAlpha");
            if (alphaValue instanceof Float) {
                this.mSnapShortAlpha = ((Float) alphaValue).floatValue();
            }
            View view = (View) this.mHostView.get();
            if (view != null) {
                MiuiDragShadowBuilder builder = animateSnapShortAlpha(this.mSnapShortAlpha);
                ValueAnimator valueAnimator = this.mAnchorAnimator;
                if (valueAnimator != null && valueAnimator.isRunning()) {
                    return;
                }
                view.updateDragShadow(builder);
            }
        }

        public void startBlurRadiatedIconAlphaAnimation(float toAlpha, long duration, long startDelayDuration) {
            ValueAnimator valueAnimator = this.mBlurRadiatedIconAlphaAnimator;
            if (valueAnimator != null && valueAnimator.isStarted()) {
                this.mBlurRadiatedIconAlphaAnimator.cancel();
            }
            if (this.mBlurRadiatedIconAlpha == toAlpha) {
                return;
            }
            Slog.d(MiuiMultiWinDragAnimationAdapter.TAG, "startBlurRadiatedIconAlphaAnimation: fromAlpha = " + this.mBlurRadiatedIconAlpha + ", toAlpha = " + toAlpha);
            ValueAnimator ofPropertyValuesHolder = ValueAnimator.ofPropertyValuesHolder(PropertyValuesHolder.ofFloat("blurRadiatedIconAlpha", this.mBlurRadiatedIconAlpha, toAlpha));
            this.mBlurRadiatedIconAlphaAnimator = ofPropertyValuesHolder;
            ofPropertyValuesHolder.setDuration(duration);
            this.mBlurRadiatedIconAlphaAnimator.setStartDelay(startDelayDuration);
            this.mBlurRadiatedIconAlphaAnimator.setInterpolator(new PhysicBasedInterpolator(0.9f, 0.857f));
            this.mBlurRadiatedIconAlphaAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.server.multiwin.animation.MiuiMultiWinDragAnimationAdapter$MiuiDragShadowBuilder$$ExternalSyntheticLambda1
                @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                public final void onAnimationUpdate(ValueAnimator valueAnimator2) {
                    MiuiMultiWinDragAnimationAdapter.MiuiDragShadowBuilder.this.m1036x8aa6bfdc(valueAnimator2);
                }
            });
            this.mBlurRadiatedIconAlphaAnimator.start();
        }

        /* renamed from: lambda$startBlurRadiatedIconAlphaAnimation$3$com-android-server-multiwin-animation-MiuiMultiWinDragAnimationAdapter$MiuiDragShadowBuilder */
        public /* synthetic */ void m1036x8aa6bfdc(ValueAnimator animation) {
            Object alphaValue = animation.getAnimatedValue("blurRadiatedIconAlpha");
            if (alphaValue instanceof Float) {
                this.mBlurRadiatedIconAlpha = ((Float) alphaValue).floatValue();
            }
            View view = (View) this.mHostView.get();
            if (view != null) {
                MiuiDragShadowBuilder builder = animateBlurRadiatedIconAlpha(this.mBlurRadiatedIconAlpha);
                ValueAnimator valueAnimator = this.mAnchorAnimator;
                if (valueAnimator != null && valueAnimator.isRunning()) {
                    return;
                }
                view.updateDragShadow(builder);
            }
        }

        public void startIconAlphaAnimation(float toAlpha, long duration, long startDelayDuration) {
            ValueAnimator valueAnimator = this.mIconAlphaAnimator;
            if (valueAnimator != null && valueAnimator.isStarted()) {
                this.mIconAlphaAnimator.cancel();
            }
            if (this.mIconAlpha == toAlpha) {
                return;
            }
            Slog.d(MiuiMultiWinDragAnimationAdapter.TAG, "startIconhaAnimation: fromiconAlpha = " + this.mIconAlpha + ", toiconAlpha = " + toAlpha);
            ValueAnimator ofPropertyValuesHolder = ValueAnimator.ofPropertyValuesHolder(PropertyValuesHolder.ofFloat("iconAlpha", this.mIconAlpha, toAlpha));
            this.mIconAlphaAnimator = ofPropertyValuesHolder;
            ofPropertyValuesHolder.setDuration(duration);
            this.mIconAlphaAnimator.setStartDelay(startDelayDuration);
            this.mIconAlphaAnimator.setInterpolator(new PhysicBasedInterpolator(0.9f, 0.857f));
            this.mIconAlphaAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.server.multiwin.animation.MiuiMultiWinDragAnimationAdapter$MiuiDragShadowBuilder$$ExternalSyntheticLambda2
                @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                public final void onAnimationUpdate(ValueAnimator valueAnimator2) {
                    MiuiMultiWinDragAnimationAdapter.MiuiDragShadowBuilder.this.m1042x8a7ad308(valueAnimator2);
                }
            });
            this.mIconAlphaAnimator.start();
        }

        /* renamed from: lambda$startIconAlphaAnimation$4$com-android-server-multiwin-animation-MiuiMultiWinDragAnimationAdapter$MiuiDragShadowBuilder */
        public /* synthetic */ void m1042x8a7ad308(ValueAnimator animation) {
            Object alphaValue = animation.getAnimatedValue("iconAlpha");
            float iconAlpha = MiuiFreeformPinManagerService.EDGE_AREA;
            if (alphaValue instanceof Float) {
                iconAlpha = ((Float) alphaValue).floatValue();
            }
            View view = (View) this.mHostView.get();
            if (view != null) {
                MiuiDragShadowBuilder builder = animateIconAlpha(iconAlpha);
                ValueAnimator valueAnimator = this.mAnchorAnimator;
                if (valueAnimator != null && valueAnimator.isRunning()) {
                    return;
                }
                view.updateDragShadow(builder);
            }
        }

        public void startBottomBarAlphaAnimation(float toAlpha, long duration, long startDelayDuration) {
            ValueAnimator valueAnimator = this.mBottomBarAlphaAnimator;
            if (valueAnimator != null && valueAnimator.isStarted()) {
                this.mBottomBarAlphaAnimator.cancel();
            }
            if (this.mBottomBarAlpha == toAlpha) {
                return;
            }
            Slog.d(MiuiMultiWinDragAnimationAdapter.TAG, "startIconhaAnimation: fromBottomBarAlpha = " + this.mBottomBarAlpha + ", toBottomBarAlpha = " + toAlpha);
            ValueAnimator ofPropertyValuesHolder = ValueAnimator.ofPropertyValuesHolder(PropertyValuesHolder.ofFloat("bottomBarAlpha", this.mBottomBarAlpha, toAlpha));
            this.mBottomBarAlphaAnimator = ofPropertyValuesHolder;
            ofPropertyValuesHolder.setDuration(duration);
            this.mBottomBarAlphaAnimator.setStartDelay(startDelayDuration);
            this.mBottomBarAlphaAnimator.setInterpolator(new PhysicBasedInterpolator(0.9f, 0.857f));
            this.mBottomBarAlphaAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.server.multiwin.animation.MiuiMultiWinDragAnimationAdapter$MiuiDragShadowBuilder$$ExternalSyntheticLambda10
                @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                public final void onAnimationUpdate(ValueAnimator valueAnimator2) {
                    MiuiMultiWinDragAnimationAdapter.MiuiDragShadowBuilder.this.m1037xa5ce3802(valueAnimator2);
                }
            });
            this.mBottomBarAlphaAnimator.start();
        }

        /* renamed from: lambda$startBottomBarAlphaAnimation$5$com-android-server-multiwin-animation-MiuiMultiWinDragAnimationAdapter$MiuiDragShadowBuilder */
        public /* synthetic */ void m1037xa5ce3802(ValueAnimator animation) {
            Object alphaValue = animation.getAnimatedValue("bottomBarAlpha");
            float bottomBarAlpha = MiuiFreeformPinManagerService.EDGE_AREA;
            if (alphaValue instanceof Float) {
                bottomBarAlpha = ((Float) alphaValue).floatValue();
            }
            View view = (View) this.mHostView.get();
            if (view != null) {
                MiuiDragShadowBuilder builder = animateBottomBarAlpha(bottomBarAlpha);
                ValueAnimator valueAnimator = this.mAnchorAnimator;
                if (valueAnimator != null && valueAnimator.isRunning()) {
                    return;
                }
                view.updateDragShadow(builder);
            }
        }

        private void startClipAnimation(RectF toClipRect, long duration, long startDelayDuration) {
            ValueAnimator valueAnimator = this.mClipAnimator;
            if (valueAnimator != null && valueAnimator.isStarted()) {
                this.mClipAnimator.cancel();
            }
            if (this.mClipRect.equals(toClipRect)) {
                return;
            }
            Slog.d(MiuiMultiWinDragAnimationAdapter.TAG, "startAnimation: fromcliprect = " + this.mClipRect + ", tocliprect = " + toClipRect);
            ValueAnimator ofPropertyValuesHolder = ValueAnimator.ofPropertyValuesHolder(PropertyValuesHolder.ofObject(PROPERTY_CLIPRECT, new RectFTypeEvaluator(), this.mClipRect, toClipRect));
            this.mClipAnimator = ofPropertyValuesHolder;
            ofPropertyValuesHolder.setStartDelay(startDelayDuration);
            this.mClipAnimator.setDuration(duration);
            this.mClipAnimator.setInterpolator(new PhysicBasedInterpolator(0.9f, 0.857f));
            this.mClipAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.server.multiwin.animation.MiuiMultiWinDragAnimationAdapter$MiuiDragShadowBuilder$$ExternalSyntheticLambda9
                @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                public final void onAnimationUpdate(ValueAnimator valueAnimator2) {
                    MiuiMultiWinDragAnimationAdapter.MiuiDragShadowBuilder.this.m1038x65331adb(valueAnimator2);
                }
            });
            this.mClipAnimator.start();
        }

        /* renamed from: lambda$startClipAnimation$6$com-android-server-multiwin-animation-MiuiMultiWinDragAnimationAdapter$MiuiDragShadowBuilder */
        public /* synthetic */ void m1038x65331adb(ValueAnimator animation) {
            Object clipRectObj = animation.getAnimatedValue(PROPERTY_CLIPRECT);
            if (clipRectObj instanceof RectF) {
                this.mClipRect = (RectF) clipRectObj;
            }
            View view = (View) this.mHostView.get();
            if (view != null) {
                ValueAnimator valueAnimator = this.mAnchorAnimator;
                if (valueAnimator != null && valueAnimator.isRunning()) {
                    return;
                }
                view.updateDragShadow(animate());
            }
        }

        public void startDragBarAlphaAnimation(float toAlpha, long duration, long startDelayDuration) {
            ValueAnimator valueAnimator = this.mDragBarAnimator;
            if (valueAnimator != null && valueAnimator.isStarted()) {
                this.mDragBarAnimator.cancel();
            }
            if (this.mDragBarAlpha == toAlpha) {
                return;
            }
            Slog.d(MiuiMultiWinDragAnimationAdapter.TAG, "startDragBarAlphaAnimation: fromdragBarAlpha = " + this.mDragBarAlpha + ", todragBarAlpha = " + toAlpha);
            ValueAnimator ofPropertyValuesHolder = ValueAnimator.ofPropertyValuesHolder(PropertyValuesHolder.ofFloat("dragBarAlpha", this.mDragBarAlpha, toAlpha));
            this.mDragBarAnimator = ofPropertyValuesHolder;
            ofPropertyValuesHolder.setDuration(duration);
            this.mDragBarAnimator.setStartDelay(startDelayDuration);
            this.mDragBarAnimator.setInterpolator(new SharpCurveInterpolator());
            this.mDragBarAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.server.multiwin.animation.MiuiMultiWinDragAnimationAdapter$MiuiDragShadowBuilder$$ExternalSyntheticLambda5
                @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                public final void onAnimationUpdate(ValueAnimator valueAnimator2) {
                    MiuiMultiWinDragAnimationAdapter.MiuiDragShadowBuilder.this.m1039x2ab47989(valueAnimator2);
                }
            });
            this.mDragBarAnimator.start();
        }

        /* renamed from: lambda$startDragBarAlphaAnimation$7$com-android-server-multiwin-animation-MiuiMultiWinDragAnimationAdapter$MiuiDragShadowBuilder */
        public /* synthetic */ void m1039x2ab47989(ValueAnimator animation) {
            Object dragBarAlphaObj = animation.getAnimatedValue("dragBarAlpha");
            if (dragBarAlphaObj instanceof Float) {
                this.mDragBarAlpha = ((Float) dragBarAlphaObj).floatValue();
            }
            View view = (View) this.mHostView.get();
            if (view != null) {
                MiuiDragShadowBuilder builder = animateDragBarAlpha(this.mDragBarAlpha);
                ValueAnimator valueAnimator = this.mAnchorAnimator;
                if (valueAnimator != null && valueAnimator.isRunning()) {
                    return;
                }
                view.updateDragShadow(builder);
            }
        }

        public void startDragBarTopMarginAnimation(final float toDragBarTopMargin, long duration, long startDelayDuration) {
            ValueAnimator valueAnimator = this.mDragBarTopMarginAnimator;
            if (valueAnimator != null && valueAnimator.isStarted()) {
                this.mDragBarTopMarginAnimator.cancel();
            }
            if (toDragBarTopMargin == this.mDragBarTopMargin) {
                return;
            }
            Slog.d(MiuiMultiWinDragAnimationAdapter.TAG, "startDragBarTopMarginAnimation: fromDragBarTopMargin = " + this.mDragBarTopMargin + ", toDragBarTopMargin = " + toDragBarTopMargin);
            ValueAnimator ofPropertyValuesHolder = ValueAnimator.ofPropertyValuesHolder(PropertyValuesHolder.ofFloat("dragBarTopMargin", this.mDragBarTopMargin, toDragBarTopMargin));
            this.mDragBarTopMarginAnimator = ofPropertyValuesHolder;
            ofPropertyValuesHolder.setDuration(duration);
            this.mDragBarTopMarginAnimator.setStartDelay(startDelayDuration);
            this.mDragBarTopMarginAnimator.setInterpolator(new PhysicBasedInterpolator(0.9f, 0.857f));
            this.mDragBarTopMarginAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.server.multiwin.animation.MiuiMultiWinDragAnimationAdapter$MiuiDragShadowBuilder$$ExternalSyntheticLambda7
                @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                public final void onAnimationUpdate(ValueAnimator valueAnimator2) {
                    MiuiMultiWinDragAnimationAdapter.MiuiDragShadowBuilder.this.m1041xce80672d(valueAnimator2);
                }
            });
            this.mDragBarTopMarginAnimator.addListener(new AnimatorListenerAdapter() { // from class: com.android.server.multiwin.animation.MiuiMultiWinDragAnimationAdapter.MiuiDragShadowBuilder.1
                @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
                public void onAnimationCancel(Animator animation) {
                    MiuiDragShadowBuilder.this.mDragBarTopMargin = toDragBarTopMargin;
                    View view = (View) MiuiDragShadowBuilder.this.mHostView.get();
                    if (view != null) {
                        if (MiuiDragShadowBuilder.this.mAnchorAnimator != null && MiuiDragShadowBuilder.this.mAnchorAnimator.isRunning()) {
                            return;
                        }
                        view.updateDragShadow(MiuiDragShadowBuilder.this);
                    }
                }
            });
            this.mDragBarTopMarginAnimator.start();
        }

        /* renamed from: lambda$startDragBarTopMarginAnimation$8$com-android-server-multiwin-animation-MiuiMultiWinDragAnimationAdapter$MiuiDragShadowBuilder */
        public /* synthetic */ void m1041xce80672d(ValueAnimator animation) {
            Object dragBarTopMargin = animation.getAnimatedValue("dragBarTopMargin");
            if (dragBarTopMargin instanceof Float) {
                this.mDragBarTopMargin = ((Float) dragBarTopMargin).floatValue();
            }
            View view = (View) this.mHostView.get();
            if (view != null) {
                ValueAnimator valueAnimator = this.mAnchorAnimator;
                if (valueAnimator != null && valueAnimator.isRunning()) {
                    return;
                }
                view.updateDragShadow(this);
            }
        }

        public void startDragBarRectAnimation(final Rect toDragBarRect, long duration, long startDelayDuration) {
            ValueAnimator valueAnimator = this.mDragBarRectAnimator;
            if (valueAnimator != null && valueAnimator.isStarted()) {
                this.mDragBarRectAnimator.cancel();
            }
            if (toDragBarRect.equals(this.mDragBarDstBound)) {
                return;
            }
            Slog.d(MiuiMultiWinDragAnimationAdapter.TAG, "startDragBarRectAnimation: fromDragBarRect = " + this.mDragBarDstBound + ", toDragBarRect = " + toDragBarRect);
            ValueAnimator ofPropertyValuesHolder = ValueAnimator.ofPropertyValuesHolder(PropertyValuesHolder.ofObject("dragBarRect", new RectTypeEvaluator(), this.mDragBarDstBound, toDragBarRect));
            this.mDragBarRectAnimator = ofPropertyValuesHolder;
            ofPropertyValuesHolder.setDuration(duration);
            this.mDragBarRectAnimator.setStartDelay(startDelayDuration);
            this.mDragBarRectAnimator.setInterpolator(new PhysicBasedInterpolator(0.9f, 0.857f));
            this.mDragBarRectAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.server.multiwin.animation.MiuiMultiWinDragAnimationAdapter$MiuiDragShadowBuilder$$ExternalSyntheticLambda6
                @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                public final void onAnimationUpdate(ValueAnimator valueAnimator2) {
                    MiuiMultiWinDragAnimationAdapter.MiuiDragShadowBuilder.this.m1040xd3b4d4c1(valueAnimator2);
                }
            });
            this.mDragBarRectAnimator.addListener(new AnimatorListenerAdapter() { // from class: com.android.server.multiwin.animation.MiuiMultiWinDragAnimationAdapter.MiuiDragShadowBuilder.2
                @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
                public void onAnimationCancel(Animator animation) {
                    MiuiDragShadowBuilder.this.mDragBarDstBound = toDragBarRect;
                    View view = (View) MiuiDragShadowBuilder.this.mHostView.get();
                    if (view != null) {
                        if (MiuiDragShadowBuilder.this.mAnchorAnimator != null && MiuiDragShadowBuilder.this.mAnchorAnimator.isRunning()) {
                            return;
                        }
                        view.updateDragShadow(MiuiDragShadowBuilder.this);
                    }
                }
            });
            this.mDragBarRectAnimator.start();
        }

        /* renamed from: lambda$startDragBarRectAnimation$9$com-android-server-multiwin-animation-MiuiMultiWinDragAnimationAdapter$MiuiDragShadowBuilder */
        public /* synthetic */ void m1040xd3b4d4c1(ValueAnimator animation) {
            Object dragBarRect = animation.getAnimatedValue("dragBarRect");
            if (dragBarRect instanceof Rect) {
                this.mDragBarDstBound = (Rect) dragBarRect;
            }
            View view = (View) this.mHostView.get();
            if (view != null) {
                ValueAnimator valueAnimator = this.mAnchorAnimator;
                if (valueAnimator != null && valueAnimator.isRunning()) {
                    return;
                }
                view.updateDragShadow(this);
            }
        }

        public void startRoundCornerRAnimation(float roundCornerR, long duration, long startDelayDuration) {
            ValueAnimator valueAnimator = this.mRoundCornerRAnimator;
            if (valueAnimator != null && valueAnimator.isStarted()) {
                this.mRoundCornerRAnimator.cancel();
            }
            if (this.mRoundCornerR == roundCornerR) {
                return;
            }
            Slog.d(MiuiMultiWinDragAnimationAdapter.TAG, "startRoundCornerRAnimation: fromroundCornerR = " + this.mRoundCornerR + ", toroundCornerR = " + roundCornerR);
            ValueAnimator ofPropertyValuesHolder = ValueAnimator.ofPropertyValuesHolder(PropertyValuesHolder.ofFloat("roundCornerR", this.mRoundCornerR, roundCornerR));
            this.mRoundCornerRAnimator = ofPropertyValuesHolder;
            ofPropertyValuesHolder.setDuration(duration);
            this.mRoundCornerRAnimator.setStartDelay(startDelayDuration);
            this.mRoundCornerRAnimator.setInterpolator(new LinearInterpolator());
            this.mRoundCornerRAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.server.multiwin.animation.MiuiMultiWinDragAnimationAdapter$MiuiDragShadowBuilder$$ExternalSyntheticLambda8
                @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                public final void onAnimationUpdate(ValueAnimator valueAnimator2) {
                    MiuiMultiWinDragAnimationAdapter.MiuiDragShadowBuilder.this.m1043x29c33f49(valueAnimator2);
                }
            });
            this.mRoundCornerRAnimator.start();
        }

        /* renamed from: lambda$startRoundCornerRAnimation$10$com-android-server-multiwin-animation-MiuiMultiWinDragAnimationAdapter$MiuiDragShadowBuilder */
        public /* synthetic */ void m1043x29c33f49(ValueAnimator animation) {
            Object roundCornerRObj = animation.getAnimatedValue("roundCornerR");
            if (roundCornerRObj instanceof Float) {
                this.mRoundCornerR = ((Float) roundCornerRObj).floatValue();
            }
            View view = (View) this.mHostView.get();
            if (view != null) {
                MiuiDragShadowBuilder builder = animateDragBarAlpha(this.mRoundCornerR);
                ValueAnimator valueAnimator = this.mAnchorAnimator;
                if (valueAnimator != null && valueAnimator.isRunning()) {
                    return;
                }
                view.updateDragShadow(builder);
            }
        }

        public float getTopMargin(int splitMode) {
            if (this.mIsLandScape) {
                return MiuiMultiWinClientUtils.DEFAULT_SPLIT_DRAG_BAR_TOP_MARGIN;
            }
            if (splitMode == 3) {
                return Resources.getSystem().getDimensionPixelSize(17105574);
            }
            return MiuiMultiWinClientUtils.VERTICAL_SPLIT_DRAG_BAR_TOP_MARGIN + MiuiMultiWinClientUtils.BOTTOM_SPLIT_DRAG_BAR_TOP_MARGIN;
        }

        public void startDropDisappearAnimation(Rect dropTargetBound, int splitMode) {
            float centerXOfClipRect = (this.mClipRect.left + this.mClipRect.right) / 2.0f;
            float toTop = this.mClipRect.top;
            float toLeft = centerXOfClipRect - (dropTargetBound.width() / 2.0f);
            float toRight = (dropTargetBound.width() / 2.0f) + centerXOfClipRect;
            float toBottom = toTop + dropTargetBound.height();
            float toTopMargin = getTopMargin(splitMode);
            RectF targetBound = new RectF(toLeft, toTop, toRight, toBottom);
            startAnchorAnimation(350L);
            startClipAnimation(targetBound, 350L, 0L);
            startRoundCornerRAnimation(this.mSplitRoundCornerRadius, 350L, 0L);
            startSnapShotAlphaAnimation(1.0f, 350L, 0L);
            startBackgroundAlphaAnimation(1.0f, 350L, 0L);
            startIconAlphaAnimation(1.0f, 350L, 0L);
            startBlurRadiatedIconAlphaAnimation(1.0f, 350L, 0L);
            startDragBarTopMarginAnimation(toTopMargin, 350L, 0L);
            startDragBarRectAnimation(this.mSplitDragBarRect, 350L, 0L);
            startDragBarAlphaAnimation(1.0f, 350L, 0L);
            startBottomBarAlphaAnimation(MiuiFreeformPinManagerService.EDGE_AREA, 350L, 0L);
        }

        public void startFreeformDropFreeformAnimation() {
            float toWidth = this.mFreeFormDraggingClipWidth;
            float toHeight = this.mFreeFormDraggingClipHeight;
            float centerxOfClipRect = (this.mClipRect.left + this.mClipRect.right) / 2.0f;
            RectF toClipRectF = new RectF(centerxOfClipRect - (toWidth / 2.0f), this.mClipRect.top, (toWidth / 2.0f) + centerxOfClipRect, this.mClipRect.top + toHeight);
            startClipAnimation(toClipRectF, 350L, 0L);
            startAnchorAnimation(350L);
            startRoundCornerRAnimation(this.mFreeFormRoundCornerRadius, 350L, 0L);
            startSnapShotAlphaAnimation(1.0f, 350L, 0L);
            startBackgroundAlphaAnimation(MiuiFreeformPinManagerService.EDGE_AREA, 350L, 0L);
            startIconAlphaAnimation(MiuiFreeformPinManagerService.EDGE_AREA, 350L, 0L);
            startBlurRadiatedIconAlphaAnimation(MiuiFreeformPinManagerService.EDGE_AREA, 350L, 0L);
            startDragBarTopMarginAnimation(this.mFreeFormDragBarTopMargin, 350L, 0L);
            startDragBarRectAnimation(this.mFreeFromDragBarRect, 350L, 0L);
            startDragBarAlphaAnimation(MiuiFreeformPinManagerService.EDGE_AREA, 350L, 0L);
            startBottomBarAlphaAnimation(MiuiFreeformPinManagerService.EDGE_AREA, 350L, 0L);
        }

        public void startDropSplitAnimation() {
            float toWidth = this.mFreeFormDraggingClipWidth;
            float toHeight = this.mFreeFormDraggingClipHeight;
            float centerxOfClipRect = (this.mClipRect.left + this.mClipRect.right) / 2.0f;
            RectF toClipRectF = new RectF(centerxOfClipRect - (toWidth / 2.0f), this.mClipRect.top, (toWidth / 2.0f) + centerxOfClipRect, this.mClipRect.top + toHeight);
            startAnchorAnimation(350L);
            startClipAnimation(toClipRectF, 350L, 0L);
            startRoundCornerRAnimation(this.mFreeFormRoundCornerRadius, 350L, 0L);
            startSnapShotAlphaAnimation(1.0f, 350L, 0L);
            startBackgroundAlphaAnimation(1.0f, 350L, 0L);
            startIconAlphaAnimation(1.0f, 350L, 0L);
            startBlurRadiatedIconAlphaAnimation(1.0f, 350L, 0L);
            startDragBarTopMarginAnimation(this.mFreeFormDragBarTopMargin, 350L, 0L);
            startDragBarRectAnimation(this.mFreeFromDragBarRect, 350L, 0L);
            startDragBarAlphaAnimation(1.0f, 350L, 0L);
            startBottomBarAlphaAnimation(1.0f, 350L, 0L);
        }

        public void startEnterSplitAlphaAnimation(boolean replaceSplit, int splitMode) {
            float toHeight;
            float toWidth;
            float centerxOfClipRect = (this.mClipRect.left + this.mClipRect.right) / 2.0f;
            float toTop = this.mClipRect.top;
            float toTopMargin = getTopMargin(splitMode);
            if (this.mIsLandScape) {
                float widthScale = replaceSplit ? 0.72f : this.mFreeformFactor;
                float heightScale = replaceSplit ? 0.7f : this.mFreeformFactor;
                float toHeight2 = this.mDragSurfaceHeight * heightScale;
                toWidth = ((this.mDragSurfaceWidth - this.mSplitBarWidth) / 2.0f) * widthScale;
                toHeight = toHeight2;
            } else {
                float widthScale2 = replaceSplit ? 0.86f : 0.73f;
                toWidth = this.mDragSurfaceWidth * widthScale2;
                toHeight = ((this.mDragSurfaceHeight - this.mSplitBarWidth) / 2.0f) * 0.42f;
            }
            RectF toClipRectF = new RectF(centerxOfClipRect - (toWidth / 2.0f), toTop, (toWidth / 2.0f) + centerxOfClipRect, toTop + toHeight);
            startAnchorAnimation(350L);
            startClipAnimation(toClipRectF, 350L, 0L);
            startRoundCornerRAnimation(this.mSplitRoundCornerRadius, 350L, 0L);
            startSnapShotAlphaAnimation(1.0f, 350L, 0L);
            startBackgroundAlphaAnimation(1.0f, 350L, 0L);
            startIconAlphaAnimation(1.0f, 350L, 0L);
            startBlurRadiatedIconAlphaAnimation(1.0f, 350L, 0L);
            startDragBarTopMarginAnimation(toTopMargin, 350L, 0L);
            startDragBarRectAnimation(this.mSplitDragBarRect, 350L, 0L);
            startDragBarAlphaAnimation(1.0f, 350L, 0L);
            startBottomBarAlphaAnimation(MiuiFreeformPinManagerService.EDGE_AREA, 350L, 0L);
        }

        public void startRecoverAnimation() {
            float toHeight;
            float toWidth;
            float f = this.mFreeformFactor;
            float toWidth2 = this.mFreeFormDraggingClipWidth * f;
            float toHeight2 = this.mFreeFormDraggingClipHeight * f;
            if (toWidth2 < this.mDefaultFreeFormDropBounds.width() * this.mFreeformFactor || toHeight2 < this.mDefaultFreeFormDropBounds.height() * this.mFreeformFactor) {
                toWidth = this.mDefaultFreeFormDropBounds.width() * this.mFreeformFactor;
                toHeight = this.mDefaultFreeFormDropBounds.height() * this.mFreeformFactor;
            } else {
                toWidth = toWidth2;
                toHeight = toHeight2;
            }
            float centerxOfClipRect = (this.mClipRect.left + this.mClipRect.right) / 2.0f;
            RectF toClipRectF = new RectF(centerxOfClipRect - (toWidth / 2.0f), this.mClipRect.top, (toWidth / 2.0f) + centerxOfClipRect, this.mClipRect.top + toHeight);
            startAnchorAnimation(350L);
            startClipAnimation(toClipRectF, 350L, 0L);
            startRoundCornerRAnimation(this.mFreeFormRoundCornerRadius, 350L, 0L);
            startSnapShotAlphaAnimation(MiuiFreeformPinManagerService.EDGE_AREA, 350L, 0L);
            startBackgroundAlphaAnimation(1.0f, 350L, 0L);
            startIconAlphaAnimation(1.0f, 350L, 0L);
            startBlurRadiatedIconAlphaAnimation(1.0f, 350L, 0L);
            startDragBarTopMarginAnimation(this.mFreeFormDragBarTopMargin, 350L, 0L);
            startDragBarRectAnimation(this.mFreeFromDragBarRect, 350L, 0L);
            startDragBarAlphaAnimation(1.0f, 350L, 0L);
            startBottomBarAlphaAnimation(1.0f, 350L, 0L);
        }

        public void startSplitDropSplit(Rect dropTargetBound, int splitMode, boolean isUpDownSplitDrop) {
            float toWidth = dropTargetBound.width();
            float toHeight = dropTargetBound.height();
            float centerxOfClipRect = (this.mClipRect.left + this.mClipRect.right) / 2.0f;
            float toTopMargin = getTopMargin(splitMode);
            RectF toClipRectF = new RectF(centerxOfClipRect - (toWidth / 2.0f), this.mClipRect.top, (toWidth / 2.0f) + centerxOfClipRect, this.mClipRect.top + toHeight);
            startAnchorAnimation(350L);
            startClipAnimation(toClipRectF, 350L, 0L);
            startRoundCornerRAnimation(this.mSplitRoundCornerRadius, 350L, 0L);
            startDragBarTopMarginAnimation(toTopMargin, 350L, 0L);
            startDragBarRectAnimation(this.mSplitDragBarRect, 350L, 0L);
            startDragBarAlphaAnimation(1.0f, 350L, 0L);
            startBottomBarAlphaAnimation(MiuiFreeformPinManagerService.EDGE_AREA, 350L, 0L);
            startSnapShotAlphaAnimation(isUpDownSplitDrop ? 0.0f : 1.0f, 350L, 0L);
            startBackgroundAlphaAnimation(isUpDownSplitDrop ? 1.0f : 0.0f, 350L, 0L);
            startIconAlphaAnimation(isUpDownSplitDrop ? 1.0f : 0.0f, 350L, 0L);
            startBlurRadiatedIconAlphaAnimation(isUpDownSplitDrop ? 1.0f : 0.0f, 350L, 0L);
        }

        public void startSplitEnterFreeFormAnimation() {
            float toHeight;
            float toWidth;
            float f = this.mFreeformFactor;
            float toWidth2 = this.mFreeFormDraggingClipWidth * f;
            float toHeight2 = this.mFreeFormDraggingClipHeight * f;
            if (toWidth2 < this.mDefaultFreeFormDropBounds.width() * this.mFreeformFactor || toHeight2 < this.mDefaultFreeFormDropBounds.height() * this.mFreeformFactor) {
                toWidth = this.mDefaultFreeFormDropBounds.width() * this.mFreeformFactor;
                toHeight = this.mDefaultFreeFormDropBounds.height() * this.mFreeformFactor;
            } else {
                toWidth = toWidth2;
                toHeight = toHeight2;
            }
            float centerxOfClipRect = (this.mClipRect.left + this.mClipRect.right) / 2.0f;
            RectF toClipRectF = new RectF(centerxOfClipRect - (toWidth / 2.0f), this.mClipRect.top, (toWidth / 2.0f) + centerxOfClipRect, this.mClipRect.top + toHeight);
            startAnchorAnimation(550L);
            startClipAnimation(toClipRectF, 350L, 200L);
            startRoundCornerRAnimation(this.mFreeFormRoundCornerRadius, 350L, 200L);
            startSnapShotAlphaAnimation(MiuiFreeformPinManagerService.EDGE_AREA, 350L, 200L);
            startBackgroundAlphaAnimation(1.0f, 350L, 200L);
            startIconAlphaAnimation(1.0f, 350L, 200L);
            startBlurRadiatedIconAlphaAnimation(1.0f, 350L, 200L);
            startDragBarTopMarginAnimation(this.mFreeFormDragBarTopMargin, 350L, 200L);
            startDragBarRectAnimation(this.mFreeFromDragBarRect, 350L, 200L);
            startDragBarAlphaAnimation(1.0f, 350L, 200L);
            startBottomBarAlphaAnimation(1.0f, 350L, 200L);
        }

        public void startSplitExitFreeFormAnimation(boolean scaled, int splitMode) {
            float f;
            float f2;
            if (scaled) {
                f = this.mInitialClipWidth * this.mFreeformFactor;
            } else {
                f = this.mInitialClipWidth * 0.9f;
            }
            float toWidth = f;
            if (scaled) {
                f2 = this.mInitialClipHeight * this.mFreeformFactor;
            } else {
                f2 = this.mInitialClipHeight * 0.93f;
            }
            float toHeight = f2;
            float toTopMargin = getTopMargin(splitMode);
            this.mDragBarAlpha = 1.0f;
            float centerxOfClipRect = (this.mClipRect.left + this.mClipRect.right) / 2.0f;
            RectF toClipRectF = new RectF(centerxOfClipRect - (toWidth / 2.0f), this.mClipRect.top, (toWidth / 2.0f) + centerxOfClipRect, this.mClipRect.top + toHeight);
            startAnchorAnimation(350L);
            startClipAnimation(toClipRectF, 350L, 0L);
            startRoundCornerRAnimation(this.mSplitRoundCornerRadius, 350L, 0L);
            startSnapShotAlphaAnimation(MiuiFreeformPinManagerService.EDGE_AREA, 350L, 0L);
            startBackgroundAlphaAnimation(1.0f, 350L, 0L);
            startIconAlphaAnimation(1.0f, 350L, 0L);
            startBlurRadiatedIconAlphaAnimation(1.0f, 350L, 0L);
            startDragBarTopMarginAnimation(toTopMargin, 350L, 0L);
            startDragBarRectAnimation(this.mSplitDragBarRect, 350L, 0L);
            startDragBarAlphaAnimation(1.0f, 350L, 0L);
            startBottomBarAlphaAnimation(MiuiFreeformPinManagerService.EDGE_AREA, 350L, 0L);
        }
    }

    public MiuiMultiWinDragAnimationAdapter(View hostView) {
        this.mHostView = hostView;
    }

    public MiuiMultiWinDragAnimationAdapter setCaptionView(View captionView) {
        this.mCaptionView = captionView;
        return this;
    }

    public MiuiMultiWinDragAnimationAdapter setDragBackground(Drawable background) {
        this.mBackground = background;
        return this;
    }

    public MiuiMultiWinDragAnimationAdapter setDragBorder(Drawable border) {
        this.mBorder = border;
        return this;
    }

    public void setDragBarBmp(Bitmap dragBarBmp, float dragBarTopMargin) {
        this.mDragBarBmp = dragBarBmp;
        this.mDragBarTopMargin = dragBarTopMargin;
    }

    public MiuiMultiWinDragAnimationAdapter setDraggingClipSize(int clipWidth, int clipHeight) {
        this.mDraggingClipSize = new Point(clipWidth, clipHeight);
        return this;
    }

    public MiuiMultiWinDragAnimationAdapter setIsLandScape(boolean isLandScape) {
        this.mIsLandScape = isLandScape;
        return this;
    }

    public MiuiMultiWinDragAnimationAdapter setIconDrawable(Drawable iconDrawable) {
        this.mIconDrawable = iconDrawable;
        return this;
    }

    public MiuiMultiWinDragAnimationAdapter setBlurRadiatedIconBitmap(Bitmap blurRadiatedIconBitmap) {
        this.mBlurRadiatedIconBitmap = blurRadiatedIconBitmap;
        return this;
    }

    public MiuiMultiWinDragAnimationAdapter setScreenShot(Bitmap screenShot) {
        this.mScreenShot = screenShot;
        MiuiDragShadowBuilder miuiDragShadowBuilder = this.mMiuiDragShadowBuilder;
        if (miuiDragShadowBuilder != null) {
            miuiDragShadowBuilder.setScreenShot(screenShot);
        }
        return this;
    }

    public MiuiMultiWinDragAnimationAdapter setBottomBar(Drawable bottomBarDrawable, int bottomBarBottomMargin) {
        this.mBottomBar = bottomBarDrawable;
        this.mBottomBarBottomMargin = bottomBarBottomMargin;
        return this;
    }

    public MiuiMultiWinDragAnimationAdapter setSplitDragBarTopMargin(int mSplitDragBarTopMargin) {
        this.mSplitDragBarTopMargin = mSplitDragBarTopMargin;
        return this;
    }

    public MiuiMultiWinDragAnimationAdapter setFreeFormDragBarTopMargin(int mFreeFormDragBarTopMargin) {
        this.mFreeFormDragBarTopMargin = mFreeFormDragBarTopMargin;
        return this;
    }

    public int getSplitDragBarTopMargin() {
        return this.mSplitDragBarTopMargin;
    }

    public int getFreeFormDragBarTopMargin() {
        return this.mFreeFormDragBarTopMargin;
    }

    public MiuiMultiWinDragAnimationAdapter setSplitDragBarRect(Rect mSplitDragBarRect) {
        this.mSplitDragBarRect = mSplitDragBarRect;
        return this;
    }

    public MiuiMultiWinDragAnimationAdapter setFreeFromDragBarRect(Rect mFreeFromDragBarRect) {
        this.mFreeFromDragBarRect = mFreeFromDragBarRect;
        return this;
    }

    public MiuiMultiWinDragAnimationAdapter setDefaultFreeFormDropBound(Rect mDefaultFreeFormDropBound) {
        this.mDefaultFreeFormDropBound = mDefaultFreeFormDropBound;
        return this;
    }

    public MiuiMultiWinDragAnimationAdapter setSplitMode(int splitMode) {
        this.mSplitMode = splitMode;
        return this;
    }

    public MiuiMultiWinDragAnimationAdapter setRootTaskIsSplit(boolean rootTaskIsSplit) {
        this.mRootTaskIsSplit = rootTaskIsSplit;
        return this;
    }

    public boolean startDragAndDrop(Bundle info, boolean isSplitCaptionViewDragged, InputChannel inputChannel) {
        if (this.mHostView == null) {
            Log.w(TAG, "startDragAndDrop failed, cause mHostView is null!");
            return false;
        } else if (info == null) {
            Slog.w(TAG, "startDragAndDrop failed, cause info is null!");
            return false;
        } else if (!info.containsKey("dragTouchOffsets")) {
            Slog.w(TAG, "startDragAndDrop failed, cause info has no DRAG_TOUCH_OFFSETS_KEY!");
            return false;
        } else {
            Point touchOffsets = (Point) info.getParcelable("dragTouchOffsets");
            if (!info.containsKey("dragSurfaceSize")) {
                Slog.w(TAG, "startDragAndDrop failed, cause info has no DRAG_SURFACE_SIZE_KEY!");
                return false;
            }
            Point dragSurfaceSize = (Point) info.getParcelable("dragSurfaceSize");
            if (!info.containsKey("initialClipSize")) {
                Slog.w(TAG, "startDragAndDrop failed, cause info has no INITIAL_CLIP_SIZE_KEY!");
                return false;
            }
            Point initialClipSize = (Point) info.getParcelable("initialClipSize");
            int surfaceWidth = dragSurfaceSize.x;
            int surfaceHeight = dragSurfaceSize.y;
            MiuiDragShadowBuilder miuiDragShadowBuilder = new MiuiDragShadowBuilder(this.mHostView, new Point(surfaceWidth, surfaceHeight), initialClipSize, new Point(touchOffsets.x, touchOffsets.y), isSplitCaptionViewDragged).setIcon(this.mIconDrawable).setScreenShot(this.mScreenShot).setBlurRadiateIconBitmap(this.mBlurRadiatedIconBitmap).setBottomBar(this.mBottomBar, this.mBottomBarBottomMargin).setDragBar(this.mDragBarBmp, this.mDragBarTopMargin).setFreeFormDragBarTopMargin(this.mFreeFormDragBarTopMargin).setSplitDragBarTopMargin(this.mSplitDragBarTopMargin).setFreeFromDragBarRect(this.mFreeFromDragBarRect).setSplitDragBarRect(this.mSplitDragBarRect).setmDefaultFreeFormDropBounds(this.mDefaultFreeFormDropBound);
            this.mMiuiDragShadowBuilder = miuiDragShadowBuilder;
            miuiDragShadowBuilder.setIsLandScape(this.mIsLandScape);
            this.mMiuiDragShadowBuilder.setBackground(this.mBackground, new Point(surfaceWidth, surfaceHeight));
            this.mMiuiDragShadowBuilder.setBorder(this.mBorder);
            this.mMiuiDragShadowBuilder.setDraggingClipSize(this.mDraggingClipSize);
            if (!isSplitCaptionViewDragged) {
                this.mMiuiDragShadowBuilder.adjustInitialRect();
            }
            Intent intent = new Intent();
            intent.putExtra("miuiFreeFormPreDragInputChannel", (Parcelable) inputChannel);
            Log.w("windowmanager", "inputChannel=" + inputChannel);
            if (!info.containsKey("freeformDropBound")) {
                Slog.w(TAG, "startDragAndDrop failed, cause info has no FREE_FORM_BOUND!");
                return false;
            }
            intent.putExtra("miuiFreeFormDropBound", (Rect) info.getParcelable("freeformDropBound"));
            ClipData clipData = ClipData.newIntent(TAG, intent);
            if (info.containsKey("dragTouchPoint")) {
                return this.mHostView.startDragAndDrop(clipData, this.mMiuiDragShadowBuilder, (Point) info.getParcelable("dragTouchPoint"), 12800);
            }
            Slog.w(TAG, "startDragAndDrop failed, cause info has no DRAG_TOUCH_POINT_KEY!");
            return false;
        }
    }

    public void startDropDisappearAnimation(Rect dropTargetRect) {
        Slog.w(TAG, "startDropDisappearAnimation");
        MiuiDragShadowBuilder miuiDragShadowBuilder = this.mMiuiDragShadowBuilder;
        if (miuiDragShadowBuilder != null) {
            miuiDragShadowBuilder.startDropDisappearAnimation(dropTargetRect, this.mSplitMode);
        }
    }

    public void startFreeformDropFreeformAnimation() {
        Slog.w(TAG, "startFreeformDropFreeformAnimation");
        MiuiDragShadowBuilder miuiDragShadowBuilder = this.mMiuiDragShadowBuilder;
        if (miuiDragShadowBuilder != null) {
            miuiDragShadowBuilder.startFreeformDropFreeformAnimation();
        }
    }

    public void startDropSplitScreenAnimation() {
        Slog.w(TAG, "startDropSplitScreenAnimation");
        MiuiDragShadowBuilder miuiDragShadowBuilder = this.mMiuiDragShadowBuilder;
        if (miuiDragShadowBuilder != null) {
            miuiDragShadowBuilder.startDropSplitAnimation();
        }
    }

    public void startEnterSplitAlphaAnimation() {
        Slog.w(TAG, "startEnterSplitAlphaAnimation");
        MiuiDragShadowBuilder miuiDragShadowBuilder = this.mMiuiDragShadowBuilder;
        if (miuiDragShadowBuilder != null) {
            miuiDragShadowBuilder.startEnterSplitAlphaAnimation(this.mRootTaskIsSplit, this.mSplitMode);
        }
    }

    public void startRecoverAnimation() {
        Slog.w(TAG, "startRecoverAnimation");
        MiuiDragShadowBuilder miuiDragShadowBuilder = this.mMiuiDragShadowBuilder;
        if (miuiDragShadowBuilder != null) {
            miuiDragShadowBuilder.startRecoverAnimation();
        }
    }

    public void startSplitDropSplit(Rect dropTargetRect, boolean isUpDownSplitDrop) {
        Slog.w(TAG, "startSplitDropSplit");
        MiuiDragShadowBuilder miuiDragShadowBuilder = this.mMiuiDragShadowBuilder;
        if (miuiDragShadowBuilder != null) {
            miuiDragShadowBuilder.startSplitDropSplit(dropTargetRect, this.mSplitMode, isUpDownSplitDrop);
        }
    }

    public void startSplitEnterFreeFormAnimation() {
        Slog.w(TAG, "startSplitEnterFreeFormAnimation");
        MiuiDragShadowBuilder miuiDragShadowBuilder = this.mMiuiDragShadowBuilder;
        if (miuiDragShadowBuilder != null) {
            miuiDragShadowBuilder.startSplitEnterFreeFormAnimation();
        }
    }

    public void startSplitExitFreeFormAnimation(boolean scaled) {
        Slog.w(TAG, "startSplitExitFreeFormAnimation");
        MiuiDragShadowBuilder miuiDragShadowBuilder = this.mMiuiDragShadowBuilder;
        if (miuiDragShadowBuilder != null) {
            miuiDragShadowBuilder.startSplitExitFreeFormAnimation(scaled, this.mSplitMode);
        }
    }
}
