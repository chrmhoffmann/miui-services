package com.android.server.multiwin.view;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Slog;
import android.view.DragEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.RelativeLayout;
import com.android.server.multiwin.MiuiMultiWinUtils;
import com.android.server.wm.MiuiFreeformPinManagerService;
/* loaded from: classes.dex */
public class MiuiMultiWinPushAcceptView extends MiuiMultiWinHotAreaView {
    private static final String TAG = "MiuiMultiWinPushAcceptView";
    private MiuiMultiWinPushPendingDropView mFreeFormPendingDropView;
    private MiuiMultiWinPushPendingDropView mPushPendingDropView;
    private MiuiMultiWinClipImageView mPushTarget;

    public MiuiMultiWinPushAcceptView(Context context) {
        super(context);
        this.mIsToDrawBorder = false;
    }

    public MiuiMultiWinPushAcceptView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MiuiMultiWinPushAcceptView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private void addSplitPendingDropView() {
        int width;
        int height;
        MiuiMultiWinPushPendingDropView miuiMultiWinPushPendingDropView = this.mPushPendingDropView;
        if (miuiMultiWinPushPendingDropView != null && miuiMultiWinPushPendingDropView.getParent() != null) {
            return;
        }
        ViewParent viewParent = getParent();
        if (viewParent instanceof ViewGroup) {
            ViewGroup viewParentGroup = (ViewGroup) viewParent;
            ViewParent dropViewParent = viewParentGroup.getParent();
            ViewGroup dropViewGroup = null;
            if (dropViewParent instanceof ViewGroup) {
                dropViewGroup = (ViewGroup) dropViewParent;
            }
            Rect pushAcceptBound = new Rect(getLeft(), getTop(), getRight(), getBottom());
            if (dropViewGroup != null) {
                pushAcceptBound.left += dropViewGroup.getLeft();
                pushAcceptBound.top += dropViewGroup.getTop();
                pushAcceptBound.right += dropViewGroup.getLeft();
                pushAcceptBound.bottom += dropViewGroup.getTop();
            }
            int splitMargin = (int) (getContext().getResources().getDimensionPixelSize(285671523) / 2.0f);
            if (isLandScape()) {
                width = (int) ((viewParentGroup.getWidth() / 2.0f) - splitMargin);
            } else {
                width = viewParentGroup.getWidth();
            }
            if (isLandScape()) {
                height = viewParentGroup.getHeight();
            } else {
                height = (int) ((viewParentGroup.getHeight() / 2.0f) - splitMargin);
            }
            Point pendingDropSize = new Point(width, height);
            RelativeLayout.LayoutParams layoutParams = getSplitPendingDropViewLayoutParams(width, height);
            MiuiMultiWinPushPendingDropView miuiMultiWinPushPendingDropView2 = new MiuiMultiWinPushPendingDropView(getContext(), pushAcceptBound, this.mPushTarget, this.mSplitMode, pendingDropSize);
            this.mPushPendingDropView = miuiMultiWinPushPendingDropView2;
            miuiMultiWinPushPendingDropView2.setContentDescription("MiuiMultiWinPushPendingDropView");
            this.mPushPendingDropView.setDragAnimationListener(this.mDragAnimationListener);
            this.mPushPendingDropView.setLayoutParams(layoutParams);
            this.mPushPendingDropView.setSplitBarController(this.mSplitBarController);
            if (dropViewGroup != null) {
                dropViewGroup.addView(this.mPushPendingDropView);
                this.mPushPendingDropView.setHasRemovedSelf(false);
                return;
            }
            return;
        }
        Slog.w(TAG, "addSplitPendingDropView failed cause parent view group is null!");
    }

    private void addFreeformPendingDropView() {
        MiuiMultiWinPushPendingDropView miuiMultiWinPushPendingDropView = this.mFreeFormPendingDropView;
        if (miuiMultiWinPushPendingDropView != null && miuiMultiWinPushPendingDropView.getParent() != null) {
            return;
        }
        ViewParent viewParent = getParent();
        if (viewParent instanceof ViewGroup) {
            ViewGroup viewParentGroup = (ViewGroup) viewParent;
            ViewParent dropViewParent = viewParentGroup.getParent();
            ViewGroup dropViewGroup = null;
            if (dropViewParent instanceof ViewGroup) {
                dropViewGroup = (ViewGroup) dropViewParent;
            }
            Rect pushAcceptBound = new Rect(getLeft(), getTop(), getRight(), getBottom());
            if (dropViewGroup != null) {
                pushAcceptBound.left += dropViewGroup.getLeft();
                pushAcceptBound.top += dropViewGroup.getTop();
                pushAcceptBound.right += dropViewGroup.getLeft();
                pushAcceptBound.bottom += dropViewGroup.getTop();
            }
            int width = viewParentGroup.getWidth();
            int height = viewParentGroup.getHeight();
            Point pendingDropSize = new Point(width, height);
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(-1, -1);
            MiuiMultiWinPushPendingDropView miuiMultiWinPushPendingDropView2 = new MiuiMultiWinPushPendingDropView(getContext(), pushAcceptBound, this.mPushTarget, this.mSplitMode, pendingDropSize);
            this.mFreeFormPendingDropView = miuiMultiWinPushPendingDropView2;
            miuiMultiWinPushPendingDropView2.setContentDescription("MiuiMultiWinPushPendingDropView");
            this.mFreeFormPendingDropView.setDragAnimationListener(this.mDragAnimationListener);
            this.mFreeFormPendingDropView.setLayoutParams(layoutParams);
            this.mFreeFormPendingDropView.setSplitBarController(this.mSplitBarController);
            if (dropViewGroup != null) {
                dropViewGroup.addView(this.mFreeFormPendingDropView);
                this.mFreeFormPendingDropView.setHasRemovedSelf(false);
                return;
            }
            return;
        }
        Slog.w(TAG, "addFreeformPendingDropView failed cause parent view group is null!");
    }

    private RelativeLayout.LayoutParams getSplitPendingDropViewLayoutParams(int width, int height) {
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(width, height);
        if (isLandScape()) {
            if (this.mSplitMode == 1) {
                lp.addRule(9);
            } else {
                lp.addRule(11);
            }
        } else if (this.mSplitMode == 3) {
            lp.addRule(10);
        } else {
            lp.addRule(12);
        }
        lp.width = width;
        lp.height = height;
        return lp;
    }

    public MiuiMultiWinPushPendingDropView getPushPendingDropView() {
        return this.mPushPendingDropView;
    }

    @Override // com.android.server.multiwin.view.MiuiMultiWinHotAreaView
    public void handleDragEntered(DragEvent dragEvent, int dragSurfaceAnimType) {
        super.handleDragEntered(dragEvent, dragSurfaceAnimType);
        if (this.mPushTarget != null) {
            Slog.w(TAG, "handleDragEntered view=" + this + " desc=" + ((Object) getContentDescription()) + " mPushTarget=" + this.mPushTarget + " mSplitMode=" + this.mSplitMode);
            this.mPushTarget.playPrePushAnimation(this.mSplitMode);
            if (this.mPushTarget.getSnapShotAlpha() == 1.0f) {
                this.mPushTarget.playFullScreenShotDismissAnimation();
                this.mPushTarget.playIconAlphaAnimation(1.0f, MiuiMultiWinUtils.DEFAULT_ANIMATION_DURATION);
            }
            if (this.mSplitMode != 5) {
                addSplitPendingDropView();
            } else {
                addFreeformPendingDropView();
            }
            measureAndLayoutHotArea();
            if (this.mSplitBarController != null && this.mSplitMode != 5) {
                boolean isUpdateToSwapMargin = false;
                if (this.mSplitMode == 2 || this.mSplitMode == 4) {
                    isUpdateToSwapMargin = true;
                }
                this.mSplitBarController.updateMargin(isUpdateToSwapMargin);
            }
        }
    }

    @Override // com.android.server.multiwin.view.MiuiMultiWinHotAreaView
    public void handleDrop(DragEvent dragEvent, int dragSurfaceAnimType, boolean isUpDownSplitDrop) {
        Slog.w(TAG, "handleDrop view=" + this + " desc=" + ((Object) getContentDescription()) + " mSplitMode=" + this.mSplitMode);
        if (this.mSplitMode == 5) {
            dragSurfaceAnimType = 2;
        }
        super.handleDrop(dragEvent, dragSurfaceAnimType, isUpDownSplitDrop);
        MiuiMultiWinClipImageView miuiMultiWinClipImageView = this.mPushTarget;
        if (miuiMultiWinClipImageView != null) {
            miuiMultiWinClipImageView.playFinalPushAnimation(this.mSplitMode);
            if (this.mSplitMode == 5) {
                this.mPushTarget.playFullScreenShotShowAnimation();
                this.mPushTarget.playIconAlphaAnimation(MiuiFreeformPinManagerService.EDGE_AREA, MiuiMultiWinUtils.DEFAULT_ANIMATION_DURATION);
            }
        }
    }

    public void setPushTarget(MiuiMultiWinClipImageView pushTarget) {
        this.mPushTarget = pushTarget;
    }

    @Override // com.android.server.multiwin.view.MiuiMultiWinHotAreaView
    public int[] getDragEndLocation() {
        ViewParent viewParent = getParent();
        if (viewParent instanceof ViewGroup) {
            ViewGroup viewParentGroup = (ViewGroup) viewParent;
            ViewParent dropViewParent = viewParentGroup.getParent();
            if (dropViewParent instanceof ViewGroup) {
                ViewGroup dropViewGroup = (ViewGroup) dropViewParent;
                int[] loc = new int[2];
                dropViewGroup.getChildAt(dropViewGroup.getChildCount() - 1).getLocationOnScreen(loc);
                return loc;
            }
        }
        return super.getDragEndLocation();
    }

    @Override // com.android.server.multiwin.view.MiuiMultiWinHotAreaView
    public int getDragEndWidth() {
        ViewParent viewParent = getParent();
        if (viewParent instanceof ViewGroup) {
            ViewGroup viewParentGroup = (ViewGroup) viewParent;
            ViewParent dropViewParent = viewParentGroup.getParent();
            if (dropViewParent instanceof ViewGroup) {
                ViewGroup dropViewGroup = (ViewGroup) dropViewParent;
                return dropViewGroup.getChildAt(dropViewGroup.getChildCount() - 1).getWidth();
            }
        }
        return super.getDragEndWidth();
    }

    @Override // com.android.server.multiwin.view.MiuiMultiWinHotAreaView
    public int getDragEndHeight() {
        ViewParent viewParent = getParent();
        if (viewParent instanceof ViewGroup) {
            ViewGroup viewParentGroup = (ViewGroup) viewParent;
            ViewParent dropViewParent = viewParentGroup.getParent();
            if (dropViewParent instanceof ViewGroup) {
                ViewGroup dropViewGroup = (ViewGroup) dropViewParent;
                return dropViewGroup.getChildAt(dropViewGroup.getChildCount() - 1).getHeight();
            }
        }
        return super.getDragEndHeight();
    }

    @Override // com.android.server.multiwin.view.MiuiMultiWinHotAreaView
    public Rect getDropBound() {
        ViewParent viewParent = getParent();
        if (viewParent instanceof ViewGroup) {
            ViewGroup viewParentGroup = (ViewGroup) viewParent;
            ViewParent dropViewParent = viewParentGroup.getParent();
            if (dropViewParent instanceof ViewGroup) {
                ViewGroup dropViewGroup = (ViewGroup) dropViewParent;
                View topChild = dropViewGroup.getChildAt(dropViewGroup.getChildCount() - 1);
                Rect topChildRect = new Rect(topChild.getLeft(), topChild.getTop(), topChild.getRight(), topChild.getBottom());
                return topChildRect;
            }
        }
        return super.getDropBound();
    }
}
