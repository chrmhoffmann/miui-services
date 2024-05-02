package com.android.server.multiwin.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Slog;
import android.view.DragEvent;
/* loaded from: classes.dex */
public class MiuiMultiWinHotAreaPendingDropView extends MiuiMultiWinHotAreaView {
    private static final String TAG = "MiuiMultiWinHotAreaPendingDropView";
    private float mLastDragX;
    private float mLastDragY;
    private Rect mPushAcceptBound;

    public MiuiMultiWinHotAreaPendingDropView(Context context, int splitMode, Rect pushAcceptBound) {
        super(context);
        this.mSplitMode = splitMode;
        this.mPushAcceptBound = pushAcceptBound;
    }

    public MiuiMultiWinHotAreaPendingDropView(Context context) {
        super(context);
    }

    public MiuiMultiWinHotAreaPendingDropView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MiuiMultiWinHotAreaPendingDropView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private boolean isDragIntoSwapAcceptBound(float dragX, float dragY) {
        return !isInSwapAcceptBound(this.mLastDragX, this.mLastDragY) && isInSwapAcceptBound(dragX, dragY);
    }

    private boolean isDragOutOfSwapAcceptBound(float dragX, float dragY) {
        return isInSwapAcceptBound(this.mLastDragX, this.mLastDragY) && !isInSwapAcceptBound(dragX, dragY);
    }

    private boolean isInSwapAcceptBound(float x, float y) {
        Rect rect = this.mPushAcceptBound;
        if (rect != null) {
            return x >= ((float) rect.left) && x <= ((float) this.mPushAcceptBound.right) && y >= ((float) this.mPushAcceptBound.top) && y <= ((float) this.mPushAcceptBound.bottom);
        }
        Slog.e("PushPendingDropView", "isInSwapAcceptBound return false, cause mSwapAcceptBound is null");
        return false;
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
        super.handleDragLocation(dragEvent);
        int[] loc = new int[2];
        getLocationOnScreen(loc);
        float dragX = dragEvent.getX() + loc[0];
        float dragY = dragEvent.getY() + loc[1];
        if (isDragOutOfSwapAcceptBound(dragX, dragY)) {
            Slog.d(TAG, this + " handleDragLocation removeSelf");
            removeSelf();
        } else if (isDragIntoSwapAcceptBound(dragX, dragY)) {
            Slog.d(TAG, this + " handleDragLocation addSelf");
            addSelf();
        }
        this.mLastDragX = dragX;
        this.mLastDragY = dragY;
    }

    @Override // com.android.server.multiwin.view.MiuiMultiWinHotAreaView
    public void handleDrop(DragEvent paramDragEvent, int dragSurfaceAnimType, boolean isUpDownSplitDrop) {
        super.handleDrop(paramDragEvent, dragSurfaceAnimType, isUpDownSplitDrop);
    }

    @Override // com.android.server.multiwin.view.MiuiMultiWinClipImageView, android.view.View
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }
}
