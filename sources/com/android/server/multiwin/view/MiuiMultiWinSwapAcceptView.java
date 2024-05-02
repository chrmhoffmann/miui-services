package com.android.server.multiwin.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Slog;
import android.view.DragEvent;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import com.android.server.multiwin.MiuiMultiWinUtils;
import com.android.server.multiwin.animation.MiuiMultiWinSplitBarController;
import com.android.server.wm.MiuiFreeformPinManagerService;
/* loaded from: classes.dex */
public class MiuiMultiWinSwapAcceptView extends MiuiMultiWinHotAreaView {
    private static final String TAG = "MiuiMultiWinSwapAcceptView";
    private int[] dropLoc;
    private boolean isAppSupportedFreeform;
    private boolean isDragPrimary;
    private boolean isHideNavBar;
    private boolean isNeedToBringFront;
    private Context mContext;
    private Rect mDropTargetBound;
    private RectF mFreeFormBound;
    private MiuiMultiWinSwapAcceptView mOtherSplitSwapAcceptView;
    private MiuiMultiWinHotAreaView mSwapTarget;
    private Rect mSwapTargetBound;
    private long splitToFreeformStartDelayDuration;
    private Transformation transformation;

    public boolean isNeedToBringFront() {
        return this.isNeedToBringFront;
    }

    public void setNeedToBringFront(boolean needToBringFront) {
        this.isNeedToBringFront = needToBringFront;
    }

    public MiuiMultiWinSwapAcceptView(Context context, int splitMode) {
        super(context);
        this.isAppSupportedFreeform = true;
        this.mContext = context;
        this.mSwapTargetBound = new Rect();
        this.mSplitMode = splitMode;
        this.mIsToDrawBorder = false;
    }

    public MiuiMultiWinSwapAcceptView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.isAppSupportedFreeform = true;
        this.mContext = context;
        this.mSwapTargetBound = new Rect();
    }

    public MiuiMultiWinSwapAcceptView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.isAppSupportedFreeform = true;
        this.mContext = context;
        this.mSwapTargetBound = new Rect();
        this.mDropTargetBound = new Rect();
    }

    private float getSplitSwapSize() {
        Slog.w(TAG, "getSwapSize this:" + this + " mOtherSplitSwapAcceptView:" + this.mOtherSplitSwapAcceptView);
        MiuiMultiWinSwapAcceptView miuiMultiWinSwapAcceptView = this.mOtherSplitSwapAcceptView;
        if (miuiMultiWinSwapAcceptView == null) {
            Slog.w(TAG, "getSwapSize failed, cause mOtherSplitSwapAcceptView is null!");
            return MiuiFreeformPinManagerService.EDGE_AREA;
        }
        Rect otherDropTargetBound = miuiMultiWinSwapAcceptView.getDropTargetBound();
        if (otherDropTargetBound == null) {
            Slog.w(TAG, "getSwapSize failed, cause or otherDropTargetBound is null!");
            return MiuiFreeformPinManagerService.EDGE_AREA;
        }
        int splitDividerBarWidth = this.mContext.getResources().getDimensionPixelSize(285671523);
        int swapSizeNoBar = isLandScape() ? otherDropTargetBound.width() : otherDropTargetBound.height();
        return swapSizeNoBar + splitDividerBarWidth;
    }

    private void getSwapTargetBoundOnLayout() {
        this.mSwapTarget.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() { // from class: com.android.server.multiwin.view.MiuiMultiWinSwapAcceptView.1
            @Override // android.view.ViewTreeObserver.OnGlobalLayoutListener
            public void onGlobalLayout() {
                MiuiMultiWinSwapAcceptView.this.mSwapTargetBound.set(MiuiMultiWinSwapAcceptView.this.mSwapTarget.getLeft(), MiuiMultiWinSwapAcceptView.this.mSwapTarget.getTop(), MiuiMultiWinSwapAcceptView.this.mSwapTarget.getRight(), MiuiMultiWinSwapAcceptView.this.mSwapTarget.getBottom());
                MiuiMultiWinSwapAcceptView.this.mSwapTarget.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
    }

    private void resizeSwapTargetWithoutNavBar() {
        Slog.w(TAG, "resizeSwapTargetWithoutNavBar");
        Rect srcBound = this.mSwapTargetBound;
        Rect dstBound = MiuiMultiWinUtils.getBoundWithoutNavBar(this.mNavBarPos, this.mNavBarBound, srcBound);
        ViewGroup.LayoutParams p = this.mSwapTarget.getLayoutParams();
        if (!(p instanceof LinearLayout.LayoutParams)) {
            Slog.w(TAG, "resizeSwapTargetWithoutNavBar failed, cause p is not LinearLayout.LayoutParams.");
            return;
        }
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) p;
        Bitmap swapTargetScreenShot = this.mSwapTarget.getSnapShotBitmap();
        if (swapTargetScreenShot == null) {
            Slog.w(TAG, "resizeSwapTargetWithoutNavBar failed, cause swapTargetScreenShot is null!");
            return;
        }
        float srcScale = this.mSwapTarget.getWidth() > 0 ? swapTargetScreenShot.getWidth() / this.mSwapTarget.getWidth() : 1.0f;
        Bitmap swapTargetScreenShotWithoutNavBar = MiuiMultiWinUtils.getScreenShotBmpWithoutNavBar(swapTargetScreenShot, this.mNavBarPos, this.mNavBarBound, srcScale);
        params.leftMargin += dstBound.left - srcBound.left;
        params.rightMargin += srcBound.right - dstBound.right;
        params.bottomMargin += srcBound.bottom - dstBound.bottom;
        this.mSwapTarget.setLayoutParams(params);
        this.mSwapTarget.setSnapShotBitmap(swapTargetScreenShotWithoutNavBar);
        this.mSwapTargetBound.set(dstBound);
    }

    public Rect getDropTargetBound() {
        return this.mDropTargetBound;
    }

    public void setDropTargetBound(Rect mDropTargetBound) {
        this.mDropTargetBound = mDropTargetBound;
    }

    @Override // com.android.server.multiwin.view.MiuiMultiWinHotAreaView
    public void handleDragEntered(DragEvent dragEvent, int dragSurfaceAnimType) {
        int dragSurfaceAnimType2;
        Slog.w(TAG, "handleDragEntered view=" + this + " desc=" + ((Object) getContentDescription()) + " mSplitMode=" + this.mSplitMode + " mSwapTarget=" + this.mSwapTarget);
        if (this.isAppSupportedFreeform && this.mSplitMode == 5 && isLandScape() && !this.transformation.needScale()) {
            this.transformation.setNeedScale();
            getLayoutParams().height = -1;
            Slog.e("enterfreeform", "enterfreeform");
            measureAndLayoutHotArea();
            requestLayout();
        }
        if (this.mSplitMode == 5) {
            dragSurfaceAnimType2 = 7;
            this.splitToFreeformStartDelayDuration = 200L;
        } else {
            dragSurfaceAnimType2 = this.transformation.needScale() ? 10 : 8;
            this.splitToFreeformStartDelayDuration = 0L;
        }
        super.handleDragEntered(dragEvent, dragSurfaceAnimType2);
        MiuiMultiWinHotAreaView miuiMultiWinHotAreaView = this.mSwapTarget;
        if (miuiMultiWinHotAreaView != null) {
            if (!this.isHideNavBar && !miuiMultiWinHotAreaView.isHasBeenResizedWithoutNavBar() && MiuiMultiWinUtils.isNeedToResizeWithoutNavBar(this.mSwapTarget.getOriginalInSwapMode(), this.mNavBarPos)) {
                resizeSwapTargetWithoutNavBar();
                this.mSwapTarget.setHasBeenResizedWithoutNavBar(true);
            }
            this.mSwapTarget.playSwapAnimation(consideredNavBar(), this.mSwapTarget.mSplitMode, this.mNavBarBound.height(), this.mSplitMode, getSplitSwapSize(), this.splitToFreeformStartDelayDuration, new AnimatorListenerAdapter() { // from class: com.android.server.multiwin.view.MiuiMultiWinSwapAcceptView.2
                @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
                public void onAnimationEnd(Animator animation) {
                    if (MiuiMultiWinSwapAcceptView.this.mSplitBarController != null && MiuiMultiWinSwapAcceptView.this.mSplitMode != 5) {
                        boolean isUpdateToSwapMargin = false;
                        int swapMode = MiuiMultiWinSwapAcceptView.this.mSwapTarget == null ? 0 : MiuiMultiWinSwapAcceptView.this.mSwapTarget.getOriginalInSwapMode();
                        if (swapMode == MiuiMultiWinSwapAcceptView.this.mSplitMode) {
                            isUpdateToSwapMargin = true;
                        }
                        MiuiMultiWinSwapAcceptView.this.mSplitBarController.updateMargin(isUpdateToSwapMargin);
                    }
                }

                @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
                public void onAnimationStart(Animator param1Animator) {
                    MiuiMultiWinSplitBarController miuiMultiWinSplitBarController = MiuiMultiWinSwapAcceptView.this.mSplitBarController;
                }
            }, this.transformation.needScale());
            if (this.mSwapTarget.getSnapShotAlpha() == 1.0f) {
                this.mSwapTarget.playFullScreenShotDismissAnimation();
                this.mSwapTarget.playIconAlphaAnimation(1.0f, MiuiMultiWinUtils.DEFAULT_ANIMATION_DURATION);
            }
        }
        if (!this.isAppSupportedFreeform && this.isNeedToBringFront) {
            bringToFront();
        }
    }

    @Override // com.android.server.multiwin.view.MiuiMultiWinHotAreaView
    public void handleDragLocation(DragEvent dragEvent) {
        super.handleDragLocation(dragEvent);
        if (!this.isAppSupportedFreeform) {
            int[] loc = new int[2];
            getLocationOnScreen(loc);
            float dragX = dragEvent.getX() + loc[0];
            float dragY = dragEvent.getY() + loc[1];
            RectF intersectRegion = new RectF(getLeft(), getTop(), getRight(), getBottom());
            boolean isIntersect = intersectRegion.intersect(this.mFreeFormBound);
            if (isIntersect) {
                if (intersectRegion.contains(dragX, dragY)) {
                    if (this.mMiuiMultiWinHotAreaConfigListener != null) {
                        this.mMiuiMultiWinHotAreaConfigListener.onNotSupportAppEnterFreeformRegion(true);
                    }
                } else if (this.mMiuiMultiWinHotAreaConfigListener != null) {
                    this.mMiuiMultiWinHotAreaConfigListener.onNotSupportAppEnterFreeformRegion(false);
                }
            }
        }
    }

    @Override // com.android.server.multiwin.view.MiuiMultiWinHotAreaView
    public void handleDragExited(DragEvent dragEvent) {
        super.handleDragExited(dragEvent);
    }

    @Override // com.android.server.multiwin.view.MiuiMultiWinHotAreaView
    public void handleDrop(DragEvent dragEvent, int dragSurfaceAnimType, boolean isUpDownSplitDrop) {
        if (this.mSplitMode != 5) {
            dragSurfaceAnimType = 9;
        }
        if (this.mSwapTarget != null) {
            if (this.mSplitMode == 5) {
                this.mSwapTarget.dropSwapAnimation(this.mSplitMode);
            } else {
                if (this.transformation.needScale()) {
                    this.mSwapTarget.startDropSplitAnimation(consideredNavBar(), this.mNavBarBound.height(), this.mSplitMode, getSplitSwapSize());
                }
                if (!isLandScape() && this.mSwapTarget.getOriginalInSwapMode() == this.mSplitMode) {
                    this.mSwapTarget.playFullScreenShotDismissAnimation();
                    this.mSwapTarget.playIconAlphaAnimation(1.0f, MiuiMultiWinUtils.DEFAULT_ANIMATION_DURATION);
                } else {
                    this.mSwapTarget.playFullScreenShotShowAnimation();
                    this.mSwapTarget.playIconAlphaAnimation(MiuiFreeformPinManagerService.EDGE_AREA, MiuiMultiWinUtils.DEFAULT_ANIMATION_DURATION);
                }
            }
        }
        super.handleDrop(dragEvent, dragSurfaceAnimType, !isLandScape() && this.mSwapTarget.getOriginalInSwapMode() == this.mSplitMode);
    }

    private boolean consideredNavBar() {
        return !this.isHideNavBar && this.mNavBarPos == 4 && !isLandScape() && this.mSwapTarget.mSplitMode == this.mSplitMode;
    }

    public void initSplitSwapAnimation(MiuiMultiWinHotAreaView swapTarget, Transformation transformation) {
        Slog.w(TAG, "initSplitSwapAnimation");
        if (this.mSplitMode == 1 || this.mSplitMode == 2 || this.mSplitMode == 3 || this.mSplitMode == 4) {
            swapTarget.setInSwapMode(swapTarget.getSplitMode());
        }
        this.transformation = transformation;
        this.mSwapTarget = swapTarget;
        getSwapTargetBoundOnLayout();
    }

    public boolean isDragPrimary() {
        return this.isDragPrimary;
    }

    public void setDragPrimary(boolean dragPrimary) {
        this.isDragPrimary = dragPrimary;
    }

    public boolean isHideNavBar() {
        return this.isHideNavBar;
    }

    public void setIsHideNavBar(boolean hideNavBar) {
        this.isHideNavBar = hideNavBar;
    }

    public int[] getDropLoc() {
        return this.dropLoc;
    }

    public void setDropLoc(int[] dropLoc) {
        this.dropLoc = dropLoc;
    }

    public RectF getFreeFormBound() {
        return this.mFreeFormBound;
    }

    public void setFreeFormBound(RectF mFreeFormBound) {
        this.mFreeFormBound = mFreeFormBound;
    }

    public void setOtherSplitSwapAcceptView(MiuiMultiWinSwapAcceptView otherSplitSwapAcceptView) {
        Slog.d(TAG, "setOtherSplitSwapAcceptView this:" + this + " otherSplitSwapAcceptView:" + otherSplitSwapAcceptView);
        this.mOtherSplitSwapAcceptView = otherSplitSwapAcceptView;
    }

    public boolean isAppSupportedFreeform() {
        return this.isAppSupportedFreeform;
    }

    public void setAppSupportedFreeform(boolean appSupportedFreeform) {
        this.isAppSupportedFreeform = appSupportedFreeform;
    }

    @Override // com.android.server.multiwin.view.MiuiMultiWinHotAreaView
    public int[] getDragEndLocation() {
        int[] iArr = this.dropLoc;
        return iArr != null ? iArr : super.getDragEndLocation();
    }

    @Override // com.android.server.multiwin.view.MiuiMultiWinHotAreaView
    public int getDragEndWidth() {
        Rect rect = this.mDropTargetBound;
        return rect != null ? rect.width() : super.getDragEndWidth();
    }

    @Override // com.android.server.multiwin.view.MiuiMultiWinHotAreaView
    public int getDragEndHeight() {
        Rect rect = this.mDropTargetBound;
        return rect != null ? rect.height() : super.getDragEndHeight();
    }

    @Override // com.android.server.multiwin.view.MiuiMultiWinHotAreaView
    public Rect getDropBound() {
        if (this.isHideNavBar || this.mNavBarPos != 4 || isLandScape() || !(this.mSplitMode == 3 || this.mSplitMode == 4)) {
            return this.mDropTargetBound;
        }
        return this.mSplitMode == 3 ? this.mDropTargetBound : new Rect(this.mDropTargetBound.left, this.mDropTargetBound.top, this.mDropTargetBound.right, this.mDropTargetBound.bottom - this.mNavBarBound.height());
    }

    /* loaded from: classes.dex */
    public static class Transformation {
        private boolean needScale = false;

        void setNeedScale() {
            this.needScale = true;
        }

        boolean needScale() {
            return this.needScale;
        }
    }
}
