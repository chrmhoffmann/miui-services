package com.android.server.multiwin.view;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Slog;
import android.view.DragEvent;
import android.view.ViewGroup;
/* loaded from: classes.dex */
public class MiuiMultiWinNotchPendingDropView extends MiuiMultiWinHotAreaView {
    private static final boolean DBG = false;
    private static final int OUT_OF_ACCEPT_BOUND_COUNT_THRESHOLD = 2;
    private static final String TAG = "MiuiMultiWinNotchPendingDropView";
    private Rect mAcceptBound;
    private MiuiMultiWinHotAreaView mExecutorView;
    private int mOutOfAcceptBoundCount = 0;
    private Rect mPendingDropBound;

    public MiuiMultiWinNotchPendingDropView(Context context) {
        super(context);
    }

    public MiuiMultiWinNotchPendingDropView(Context context, Rect acceptBound, Rect pendingDropBound) {
        super(context);
        this.mAcceptBound = acceptBound;
        this.mPendingDropBound = pendingDropBound;
    }

    public MiuiMultiWinNotchPendingDropView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MiuiMultiWinNotchPendingDropView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override // com.android.server.multiwin.view.MiuiMultiWinHotAreaView
    public void handleDragEntered(DragEvent dragEvent, int dragSurfaceAnimType) {
        Slog.d(TAG, "handleDragEntered");
        Rect rect = this.mPendingDropBound;
        if (rect != null) {
            layout(rect.left, this.mPendingDropBound.top, this.mPendingDropBound.right, this.mPendingDropBound.bottom);
            updateLpSize(this.mPendingDropBound.width(), this.mPendingDropBound.height());
        }
        MiuiMultiWinHotAreaView miuiMultiWinHotAreaView = this.mExecutorView;
        if (miuiMultiWinHotAreaView != null) {
            miuiMultiWinHotAreaView.handleDragEntered(dragEvent, dragSurfaceAnimType);
        }
    }

    @Override // com.android.server.multiwin.view.MiuiMultiWinHotAreaView
    public void handleDragExited(DragEvent dragEvent) {
        Slog.d(TAG, "handleDragExited");
        Rect rect = this.mAcceptBound;
        if (rect != null) {
            layout(rect.left, this.mAcceptBound.top, this.mAcceptBound.right, this.mAcceptBound.bottom);
            updateLpSize(this.mAcceptBound.width(), this.mAcceptBound.height());
        }
    }

    @Override // com.android.server.multiwin.view.MiuiMultiWinHotAreaView
    public void handleDragLocation(DragEvent dragEvent) {
        Slog.d(TAG, "handleDragLocation");
        if (this.mAcceptBound != null && this.mPendingDropBound != null) {
            if (isInAcceptBound(dragEvent.getX() + getLeft(), dragEvent.getY() + getTop())) {
                this.mOutOfAcceptBoundCount = 0;
            } else {
                this.mOutOfAcceptBoundCount++;
            }
            if (this.mOutOfAcceptBoundCount >= 2) {
                layout(this.mAcceptBound.left, this.mAcceptBound.top, this.mAcceptBound.right, this.mAcceptBound.bottom);
                updateLpSize(this.mAcceptBound.width(), this.mAcceptBound.height());
                this.mOutOfAcceptBoundCount = 0;
            }
        }
        Slog.w(TAG, "handleDragLocation failed, cause mAcceptBound mPendingDropBound is null!");
    }

    @Override // com.android.server.multiwin.view.MiuiMultiWinHotAreaView
    public void handleDrop(DragEvent dragEvent, int dragSurfaceAnimType, boolean isUpDownSplitDrop) {
        Slog.d(TAG, "handleDrop");
        MiuiMultiWinHotAreaView miuiMultiWinHotAreaView = this.mExecutorView;
        if (miuiMultiWinHotAreaView == null) {
            super.handleDrop(dragEvent, dragSurfaceAnimType, isUpDownSplitDrop);
        } else if (miuiMultiWinHotAreaView instanceof MiuiMultiWinSwapAcceptView) {
            this.mIsDropOnThisView = true;
            this.mDragAnimationListener.onDrop(this, dragEvent, getSplitMode(), getDropBound(), 9, false);
        } else if (!(miuiMultiWinHotAreaView instanceof MiuiMultiWinPushAcceptView)) {
            miuiMultiWinHotAreaView.handleDrop(dragEvent, 6, isUpDownSplitDrop);
        } else {
            this.mIsDropOnThisView = true;
            ((MiuiMultiWinPushAcceptView) this.mExecutorView).getPushPendingDropView().handleDrop(dragEvent, dragSurfaceAnimType, isUpDownSplitDrop);
        }
    }

    private boolean isInAcceptBound(float x, float y) {
        Rect rect = this.mAcceptBound;
        if (rect != null) {
            return x >= ((float) rect.left) && x <= ((float) this.mAcceptBound.right) && y >= ((float) this.mAcceptBound.top) && y <= ((float) this.mAcceptBound.bottom);
        }
        Slog.w(TAG, "isInAcceptBound return false, cause mAcceptBound is null");
        return false;
    }

    public void setExecutorView(MiuiMultiWinHotAreaView executorView) {
        this.mExecutorView = executorView;
    }

    private void updateLpSize(int width, int height) {
        ViewGroup.LayoutParams lp = getLayoutParams();
        lp.width = width;
        lp.height = height;
        setLayoutParams(lp);
    }
}
