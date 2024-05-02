package com.android.server.wm;

import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Handler;
import android.view.MotionEvent;
import com.android.server.UiThread;
import com.android.server.wm.MiuiCvwGestureController;
import com.android.server.wm.MiuiCvwPolicy;
import com.android.server.wm.MiuiCvwSnapTargetPool;
import java.io.PrintWriter;
import java.util.Objects;
/* loaded from: classes.dex */
public class MiuiCvwGestureHandlerImpl implements MiuiCvwGestureController.CvwGestureHandler {
    private static final int MOVING_RECT_INSET = -10;
    private static final int RANGE_FREEFORM = 2;
    private static final int RANGE_FULLSCREEN = 0;
    private static final int RANGE_HOVER = 1;
    protected static final int STIFFNESS_DOWN_SCALE = 2;
    protected static final int STIFFNESS_MINI_SWITCH = 0;
    protected static final int STIFFNESS_RATIO_SHIFT = 1;
    private static final String TAG = MiuiCvwGestureHandlerImpl.class.getSimpleName();
    private static final int TOUCH_RESIZE_LEFT = 0;
    private static final int TOUCH_RESIZE_RIGHT = 1;
    protected static final int WIN_BALL = 3;
    protected static final int WIN_FREEFORM = 1;
    protected static final int WIN_FULLSCREEN = 0;
    protected static final int WIN_MINI = 2;
    protected static final int WIN_UNDEFINED = -1;
    MiuiCvwGestureController mController;
    MiuiCvwPolicy mCvwPolicy;
    private int mDownTouchedMode;
    private int mLaunchWinMode;
    MiuiCvwLayerSupervisor mLayerSupervisor;
    private float mOffsetX;
    private float mOffsetY;
    private boolean mRatioChanged;
    private float mScreenAspectRatio;
    protected int mScreenHeight;
    protected int mScreenWidth;
    MiuiCvwPolicy.TaskWrapperInfo mTaskWrapperInfo;
    private int mFinishingAnimType = -1;
    private final Rect mLastMovingBounds = new Rect();
    private final Rect mOriginalBounds = new Rect();
    private float mSynamicStiffness = 1000.0f;
    private final RatioRecord mRatioRecord = new RatioRecord();

    public MiuiCvwGestureHandlerImpl(MiuiCvwGestureController controller) {
        this.mController = controller;
        this.mLayerSupervisor = new MiuiCvwLayerSupervisor(controller, this);
        this.mCvwPolicy = this.mController.mCvwPolicy;
    }

    @Override // com.android.server.wm.MiuiCvwGestureController.CvwGestureHandler
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        if (this.mController.mStack == null) {
            return false;
        }
        float rawX = event.getRawX();
        float rawY = event.getRawY();
        switch (action) {
            case 0:
                notifyDownLocked(this.mController.mStack, rawX, rawY);
                return true;
            case 1:
            case 3:
                notifyUpLocked(this.mController.mStack);
                return true;
            case 2:
                notifyMoveLocked(this.mController.mStack, rawX, rawY);
                return true;
            default:
                return true;
        }
    }

    @Override // com.android.server.wm.MiuiCvwGestureController.CvwGestureHandler
    public boolean isGestureAnimating() {
        return this.mLayerSupervisor.isAnimating() && !this.mLayerSupervisor.isTaskLeashRemoved();
    }

    @Override // com.android.server.wm.MiuiCvwGestureController.CvwGestureHandler
    public boolean isResizeFinished() {
        return this.mFinishingAnimType != -1;
    }

    private void notifyDownLocked(MiuiFreeFormActivityStack stack, float x, float y) {
        int i;
        MiuiCvwPolicy.TaskWrapperInfo taskWrapperInfo = this.mCvwPolicy.getTaskWrapperInfo(stack.mTask, this.mController.mInFreeformRegion);
        this.mTaskWrapperInfo = taskWrapperInfo;
        if (taskWrapperInfo == null) {
            return;
        }
        this.mRatioRecord.reset();
        if (this.mCvwPolicy.getTmpFreeformTaskWrapper() != null) {
            this.mCvwPolicy.getTmpFreeformTaskWrapper().reset();
        }
        this.mFinishingAnimType = -1;
        this.mSynamicStiffness = 1000.0f;
        if ((this.mController.getGestureFlag() & 4) != 0) {
            i = 0;
        } else {
            i = 1;
        }
        this.mDownTouchedMode = i;
        this.mOriginalBounds.set(this.mTaskWrapperInfo.visualBounds);
        this.mOffsetX = (this.mDownTouchedMode == 0 ? this.mOriginalBounds.left : this.mOriginalBounds.right) - x;
        this.mOffsetY = this.mOriginalBounds.bottom - y;
        this.mRatioChanged = false;
        if (stack.isInFreeFormMode()) {
            this.mLaunchWinMode = 1;
        } else if (stack.isInMiniFreeFormMode()) {
            this.mLaunchWinMode = 2;
        } else {
            this.mLaunchWinMode = 0;
        }
        updateRangeParams();
        initializeLayerSupervisor(stack);
        updateRecorderParams(stack);
        if (this.mLaunchWinMode != 0) {
            if (!this.mLayerSupervisor.checkAnimatingStack()) {
                return;
            }
            stiffnessCalibration(true);
            updateRatioRecord(MiuiFreeformPinManagerService.EDGE_AREA);
            winResizeToRatio(stack, this.mRatioRecord.dragRangeArae);
        }
        if (this.mTaskWrapperInfo.supportCvw) {
            this.mController.notifyFreeFormApplicationResizeStart();
        }
        MiuiCvwGestureController.Slog.d(TAG, "notifyDownLocked:" + this.mTaskWrapperInfo + " ," + this.mTaskWrapperInfo.getCurrentSnapTarget() + ", " + stack);
    }

    private void notifyMoveLocked(MiuiFreeFormActivityStack stack, float x, float y) {
        if (!this.mLayerSupervisor.checkAnimatingStack()) {
            return;
        }
        RatioRecord ratioRecord = this.mRatioRecord;
        ratioRecord.movingRatioPair = getRatioPairByRange(ratioRecord.dragRangeArae, this.mOffsetX + x, this.mOffsetY + y);
        if (this.mTaskWrapperInfo.isFixedRatio || !this.mRatioChanged) {
            updateRatioRecord(this.mRatioRecord.touchedRatio);
        } else {
            updateRatioRecord(MiuiFreeformPinManagerService.EDGE_AREA);
        }
        tryShowOverlays(stiffnessCalibration(false));
        winResizeToRatio(stack, this.mRatioRecord.dragRangeArae);
        notifyModeChange(this.mRatioRecord.current.mode, stack);
        MiuiCvwGestureController.Slog.d(TAG, "notifyMoveLocked movingRatio:" + this.mRatioRecord.movingRatioPair[0] + " " + this.mRatioRecord.movingRatioPair[1] + " winRange:" + this.mRatioRecord.dragRangeArae);
    }

    private void tryShowOverlays(int stiffMode) {
        if (stiffMode == 1) {
            this.mRatioChanged = true;
            if (this.mRatioRecord.guess.mode != 2) {
                this.mLayerSupervisor.showCoverLayer();
                if (this.mRatioRecord.current.mode == 1) {
                    this.mLayerSupervisor.notifyFreeformEntering();
                }
            }
        }
    }

    private void notifyModeChange(int currentMode, MiuiFreeFormActivityStack stack) {
        if (this.mRatioRecord.lastWinMode != currentMode) {
            switch (currentMode) {
                case 0:
                    this.mRatioChanged = true;
                    this.mLayerSupervisor.notifyFullscreenEntering();
                    if (this.mRatioRecord.lastWinMode == 1 && this.mLaunchWinMode != 0) {
                        this.mLayerSupervisor.showDim(stack);
                        break;
                    }
                    break;
                case 1:
                    this.mLayerSupervisor.notifyFreeformEntering();
                    if (this.mRatioRecord.lastWinMode == 0 && this.mLaunchWinMode != 0) {
                        this.mLayerSupervisor.hideDim(stack);
                        break;
                    }
                    break;
                case 2:
                    if (this.mLaunchWinMode != 2 || this.mRatioRecord.lastWinMode != -1) {
                        this.mLayerSupervisor.notifyMiniEntering();
                        this.mRatioChanged = true;
                        break;
                    }
                    break;
            }
            if (this.mRatioRecord.lastWinMode == -1) {
                Handler handler = UiThread.getHandler();
                final MiuiCvwGestureController miuiCvwGestureController = this.mController;
                Objects.requireNonNull(miuiCvwGestureController);
                handler.post(new Runnable() { // from class: com.android.server.wm.MiuiCvwGestureHandlerImpl$$ExternalSyntheticLambda4
                    @Override // java.lang.Runnable
                    public final void run() {
                        MiuiCvwGestureController.this.startGesture();
                    }
                });
            }
            RatioRecord ratioRecord = this.mRatioRecord;
            ratioRecord.preWinMode = ratioRecord.lastWinMode;
            this.mRatioRecord.lastWinMode = currentMode;
        }
    }

    private boolean exceedsThresholdRadio(float presentRadio, float referenceRadio, float threshold) {
        return !equalRawRatio(referenceRadio, this.mRatioRecord.guess.aspectRatio) || presentRadio > (threshold + 1.0f) * referenceRadio || presentRadio < (1.0f - threshold) * referenceRadio;
    }

    private boolean equalRawRatio(float preRatio, float curRatio) {
        return ((double) Math.abs(curRatio - preRatio)) < 0.01d;
    }

    private void notifyUpLocked(MiuiFreeFormActivityStack stack) {
        int freeformTop;
        int scrollToBottom;
        int miniFreeformWidth;
        int miniFreeformBottom;
        int freeformRight;
        int miniFreeformTop;
        int miniFreeformTop2;
        int miniFreeformRight;
        int i;
        int i2;
        int i3;
        int i4;
        float freeformRawRatio;
        int miniFreeformWidth2;
        int miniFreeformHeight;
        flingToRightSnapTarget(stack);
        if (this.mRatioRecord.guess.mode == -1) {
            MiuiCvwGestureController.Slog.d(TAG, "guess mode is invalid!");
            this.mFinishingAnimType = 6;
            this.mLayerSupervisor.resetAllState();
        } else if (this.mController.gestureCanceled()) {
        } else {
            if (!this.mTaskWrapperInfo.supportCvw) {
                boundByRatio(this.mRatioRecord.rangeAraes[0], this.mLastMovingBounds, this.mRatioRecord.current, false);
                backToFullscreen(stack);
                return;
            }
            Rect winRange = this.mRatioRecord.guess.mode == 0 ? this.mRatioRecord.rangeAraes[0] : this.mRatioRecord.rangeAraes[2];
            if (!winRange.equals(this.mRatioRecord.preRangeArae)) {
                this.mRatioRecord.preRangeArae = winRange;
                float rangeRatio = (this.mRatioRecord.preRangeArae.width() * 1.0f) / this.mRatioRecord.preRangeArae.height();
                this.mCvwPolicy.updateRangeRatio(rangeRatio);
            }
            updateRatioRecord(this.mTaskWrapperInfo.isFixedRatio ? this.mRatioRecord.touchedRatio : MiuiFreeformPinManagerService.EDGE_AREA);
            boundByRatio(winRange, this.mLastMovingBounds, this.mRatioRecord.guess, false);
            fixPosition(winRange);
            boolean isLeftSuspend = ((float) this.mLastMovingBounds.centerX()) <= (((float) this.mScreenWidth) * 1.0f) / 2.0f;
            float freeformRawRatio2 = 1.0f;
            int scrollToLeft = this.mLastMovingBounds.left;
            int scrollToTop = this.mLastMovingBounds.top;
            int scrollToRight = this.mLastMovingBounds.right;
            int scrollToBottom2 = this.mLastMovingBounds.bottom;
            this.mFinishingAnimType = 6;
            int freeformLeft = this.mLastMovingBounds.left;
            int freeformTop2 = this.mLastMovingBounds.top;
            int freeformRight2 = this.mLastMovingBounds.left + ((int) (this.mLastMovingBounds.width() + 0.5f));
            int freeformBottom = this.mLastMovingBounds.top + ((int) (this.mLastMovingBounds.height() + 0.5f));
            int miniFreeformWidth3 = (int) this.mRatioRecord.miniWindowWidth;
            int miniFreeformHeight2 = (int) this.mRatioRecord.miniWindowHight;
            int miniFreeformLeft = isLeftSuspend ? winRange.left : winRange.right - miniFreeformWidth3;
            int freeformTop3 = winRange.top;
            int miniFreeformRight2 = miniFreeformLeft + miniFreeformWidth3;
            int miniFreeformBottom2 = freeformTop3 + miniFreeformHeight2;
            Rect freeformWindowBounds = new Rect();
            Rect miniFreeformWindowBounds = new Rect();
            if (this.mLaunchWinMode == 1 && this.mRatioRecord.guess.mode == 0) {
                this.mFinishingAnimType = 12;
                this.mLayerSupervisor.showCoverLayer();
                miniFreeformTop = freeformTop3;
                miniFreeformTop2 = freeformTop2;
                miniFreeformRight = miniFreeformRight2;
                miniFreeformBottom = miniFreeformBottom2;
                miniFreeformWidth = 1140457472;
                freeformRight = freeformRight2;
                freeformTop = scrollToBottom2;
                scrollToBottom = 1065353216;
            } else if (this.mLaunchWinMode == 2 && this.mRatioRecord.guess.mode == 0) {
                this.mFinishingAnimType = 17;
                this.mLayerSupervisor.showCoverLayer();
                miniFreeformTop = freeformTop3;
                miniFreeformTop2 = freeformTop2;
                miniFreeformRight = miniFreeformRight2;
                miniFreeformBottom = miniFreeformBottom2;
                miniFreeformWidth = 1140457472;
                freeformRight = freeformRight2;
                freeformTop = scrollToBottom2;
                scrollToBottom = 1065353216;
            } else if (this.mLaunchWinMode == 0 && 1 == this.mRatioRecord.guess.mode) {
                this.mFinishingAnimType = this.mDownTouchedMode == 0 ? 3 : 4;
                freeformRawRatio2 = this.mRatioRecord.guess.aspectRatio;
                int miniFreeformWidth4 = (int) this.mCvwPolicy.getSnapTargetByRawRatio(freeformRawRatio2).getMiniWidth();
                int miniFreeformHeight3 = (int) this.mCvwPolicy.getSnapTargetByRawRatio(freeformRawRatio2).getMiniHeight();
                miniFreeformLeft = ((float) this.mLastMovingBounds.centerX()) <= (((float) this.mScreenWidth) * 1.0f) / 2.0f ? winRange.left : winRange.right - miniFreeformWidth4;
                int miniFreeformBottom3 = freeformTop3 + miniFreeformHeight3;
                miniFreeformTop = freeformTop3;
                miniFreeformTop2 = freeformTop2;
                miniFreeformRight = miniFreeformLeft + miniFreeformWidth4;
                miniFreeformBottom = miniFreeformBottom3;
                miniFreeformWidth = 1133903872;
                freeformRight = freeformRight2;
                freeformBottom = freeformBottom;
                freeformTop = scrollToBottom2;
                scrollToBottom = 1065353216;
            } else if (this.mLaunchWinMode != 0 || 2 != this.mRatioRecord.guess.mode) {
                if (this.mLaunchWinMode == 1 && 2 == this.mRatioRecord.guess.mode) {
                    if (this.mDownTouchedMode == 0) {
                        i3 = 7;
                    } else {
                        i3 = 8;
                    }
                    this.mFinishingAnimType = i3;
                    freeformRawRatio2 = this.mRatioRecord.touchedRatio;
                    freeformLeft = this.mOriginalBounds.left;
                    int freeformTop4 = this.mOriginalBounds.top;
                    freeformRight = this.mOriginalBounds.right;
                    freeformBottom = this.mOriginalBounds.bottom;
                    scrollToLeft = miniFreeformLeft;
                    scrollToTop = freeformTop3;
                    scrollToRight = miniFreeformRight2;
                    freeformTop = miniFreeformBottom2;
                    miniFreeformRight = miniFreeformRight2;
                    miniFreeformBottom = miniFreeformBottom2;
                    miniFreeformWidth = 1137180672;
                    scrollToBottom = 1061158912;
                    miniFreeformTop = freeformTop3;
                    miniFreeformTop2 = freeformTop4;
                } else if (this.mLaunchWinMode == 2 && 1 == this.mRatioRecord.guess.mode) {
                    if (this.mDownTouchedMode == 0) {
                        i2 = 15;
                    } else {
                        i2 = 16;
                    }
                    this.mFinishingAnimType = i2;
                    freeformRawRatio2 = this.mRatioRecord.guess.aspectRatio;
                    int miniFreeformWidth5 = (int) this.mCvwPolicy.getSnapTargetByRawRatio(freeformRawRatio2).getMiniWidth();
                    int miniFreeformHeight4 = (int) this.mCvwPolicy.getSnapTargetByRawRatio(freeformRawRatio2).getMiniHeight();
                    miniFreeformLeft = isLeftSuspend ? winRange.left : winRange.right - miniFreeformWidth5;
                    int miniFreeformBottom4 = freeformTop3 + miniFreeformHeight4;
                    miniFreeformTop = freeformTop3;
                    miniFreeformTop2 = freeformTop2;
                    miniFreeformRight = miniFreeformLeft + miniFreeformWidth5;
                    miniFreeformBottom = miniFreeformBottom4;
                    miniFreeformWidth = 1133903872;
                    freeformRight = freeformRight2;
                    freeformBottom = freeformBottom;
                    freeformTop = scrollToBottom2;
                    scrollToBottom = 1065353216;
                } else if (this.mLaunchWinMode == 2 && 2 == this.mRatioRecord.guess.mode) {
                    this.mFinishingAnimType = 18;
                    freeformRawRatio2 = this.mRatioRecord.touchedRatio;
                    freeformLeft = this.mTaskWrapperInfo.actualBounds.left;
                    int freeformTop5 = this.mTaskWrapperInfo.actualBounds.top;
                    int freeformRight3 = ((int) ((this.mTaskWrapperInfo.actualBounds.width() * this.mTaskWrapperInfo.scale) + 0.5f)) + freeformLeft;
                    freeformBottom = freeformTop5 + ((int) ((this.mTaskWrapperInfo.actualBounds.height() * this.mTaskWrapperInfo.scale) + 0.5f));
                    int miniFreeformWidth6 = (int) this.mCvwPolicy.getSnapTargetByRawRatio(freeformRawRatio2).getMiniWidth();
                    int miniFreeformHeight5 = (int) this.mCvwPolicy.getSnapTargetByRawRatio(freeformRawRatio2).getMiniHeight();
                    miniFreeformLeft = this.mLastMovingBounds.left;
                    int miniFreeformTop3 = this.mLastMovingBounds.top;
                    int miniFreeformRight3 = miniFreeformLeft + miniFreeformWidth6;
                    int miniFreeformBottom5 = miniFreeformTop3 + miniFreeformHeight5;
                    miniFreeformTop = miniFreeformTop3;
                    freeformTop = scrollToBottom2;
                    freeformRight = freeformRight3;
                    miniFreeformTop2 = freeformTop5;
                    miniFreeformWidth = 1133903872;
                    scrollToBottom = 1065353216;
                    miniFreeformRight = miniFreeformRight3;
                    miniFreeformBottom = miniFreeformBottom5;
                } else if (this.mLaunchWinMode == 1 && 1 == this.mRatioRecord.guess.mode) {
                    if (this.mDownTouchedMode == 0) {
                        i = 0;
                    } else {
                        i = 1;
                    }
                    this.mFinishingAnimType = i;
                    freeformRawRatio2 = this.mRatioRecord.guess.aspectRatio;
                    int miniFreeformWidth7 = (int) this.mCvwPolicy.getSnapTargetByRawRatio(freeformRawRatio2).getMiniWidth();
                    int miniFreeformHeight6 = (int) this.mCvwPolicy.getSnapTargetByRawRatio(freeformRawRatio2).getMiniHeight();
                    miniFreeformLeft = isLeftSuspend ? winRange.left : winRange.right - miniFreeformWidth7;
                    int miniFreeformBottom6 = freeformTop3 + miniFreeformHeight6;
                    miniFreeformTop = freeformTop3;
                    miniFreeformTop2 = freeformTop2;
                    miniFreeformRight = miniFreeformLeft + miniFreeformWidth7;
                    miniFreeformBottom = miniFreeformBottom6;
                    miniFreeformWidth = 1133903872;
                    freeformRight = freeformRight2;
                    freeformBottom = freeformBottom;
                    freeformTop = scrollToBottom2;
                    scrollToBottom = 1065353216;
                } else if (this.mLaunchWinMode == 0 && this.mRatioRecord.guess.mode == 0) {
                    this.mFinishingAnimType = 5;
                    miniFreeformTop = freeformTop3;
                    miniFreeformTop2 = freeformTop2;
                    miniFreeformRight = miniFreeformRight2;
                    miniFreeformBottom = miniFreeformBottom2;
                    miniFreeformWidth = 1140457472;
                    freeformRight = freeformRight2;
                    freeformBottom = freeformBottom;
                    freeformTop = scrollToBottom2;
                    scrollToBottom = 1065353216;
                } else {
                    miniFreeformTop = freeformTop3;
                    miniFreeformTop2 = freeformTop2;
                    miniFreeformRight = miniFreeformRight2;
                    miniFreeformBottom = miniFreeformBottom2;
                    miniFreeformWidth = 1148846080;
                    freeformRight = freeformRight2;
                    freeformBottom = freeformBottom;
                    freeformTop = scrollToBottom2;
                    scrollToBottom = 1065353216;
                }
            } else {
                if (this.mDownTouchedMode == 0) {
                    i4 = 9;
                } else {
                    i4 = 10;
                }
                this.mFinishingAnimType = i4;
                if (this.mTaskWrapperInfo.isFixedRatio) {
                    miniFreeformHeight = (int) this.mTaskWrapperInfo.currentSnapTarget.getMiniHeight();
                    freeformRawRatio = this.mTaskWrapperInfo.currentSnapTarget.getRawRatio();
                    miniFreeformWidth2 = (int) this.mTaskWrapperInfo.currentSnapTarget.getMiniWidth();
                } else if (this.mTaskWrapperInfo.mSupportTargets.size() == 1) {
                    miniFreeformHeight = (int) this.mCvwPolicy.mSnapTargetPool.mLargeGeneralSnapTarget.getMiniHeight();
                    freeformRawRatio = this.mCvwPolicy.mSnapTargetPool.mLargeGeneralSnapTarget.getRawRatio();
                    miniFreeformWidth2 = (int) this.mCvwPolicy.mSnapTargetPool.mLargeGeneralSnapTarget.getMiniWidth();
                } else {
                    miniFreeformHeight = (int) this.mCvwPolicy.mSnapTargetPool.mXlargeGeneralSnapTarget.getMiniHeight();
                    freeformRawRatio = this.mCvwPolicy.mSnapTargetPool.mXlargeGeneralSnapTarget.getRawRatio();
                    miniFreeformWidth2 = (int) this.mCvwPolicy.mSnapTargetPool.mXlargeGeneralSnapTarget.getMiniWidth();
                }
                float scale = 1.0f;
                float maxFreeformLength = Math.max(this.mRatioRecord.freeformWindowHight, this.mRatioRecord.freeformWindowWidth);
                float freeformRawRatio3 = freeformRawRatio;
                int minRangeLength = ((Math.min(this.mScreenHeight, this.mScreenWidth) - this.mCvwPolicy.getNavBarHeight()) - 75) - winRange.top;
                if (maxFreeformLength > minRangeLength) {
                    scale = minRangeLength / maxFreeformLength;
                }
                miniFreeformLeft = isLeftSuspend ? winRange.left : winRange.right - miniFreeformWidth2;
                int miniFreeformRight4 = miniFreeformLeft + miniFreeformWidth2;
                int miniFreeformBottom7 = freeformTop3 + miniFreeformHeight;
                freeformLeft = isLeftSuspend ? miniFreeformLeft : (int) ((winRange.right - (this.mRatioRecord.freeformWindowWidth * scale)) + 0.5f);
                miniFreeformTop = freeformTop3;
                scrollToLeft = miniFreeformLeft;
                scrollToTop = freeformTop3;
                scrollToRight = miniFreeformRight4;
                freeformBottom = ((int) ((this.mRatioRecord.freeformWindowHight * scale) + 0.5f)) + miniFreeformTop;
                freeformRight = ((int) ((this.mRatioRecord.freeformWindowWidth * scale) + 0.5f)) + freeformLeft;
                freeformTop = miniFreeformBottom7;
                freeformRawRatio2 = freeformRawRatio3;
                miniFreeformRight = miniFreeformRight4;
                miniFreeformBottom = miniFreeformBottom7;
                scrollToBottom = 1061158912;
                miniFreeformWidth = 1137180672;
                miniFreeformTop2 = miniFreeformTop;
            }
            freeformWindowBounds.set(freeformLeft, miniFreeformTop2, freeformRight, freeformBottom);
            miniFreeformWindowBounds.set(miniFreeformLeft, miniFreeformTop, miniFreeformRight, miniFreeformBottom);
            MiuiCvwPolicy miuiCvwPolicy = this.mCvwPolicy;
            int miniFreeformTop4 = this.mFinishingAnimType;
            miuiCvwPolicy.updateResultTaskWrapperInfo(freeformRawRatio2, freeformWindowBounds, miniFreeformWindowBounds, miniFreeformTop4);
            this.mCvwPolicy.updateTrackEventData(stack, freeformRawRatio2, this.mFinishingAnimType);
            this.mLayerSupervisor.spring(miniFreeformWidth, scrollToBottom).to(2, scrollToLeft, 3, scrollToTop, 7, scrollToRight, 8, freeformTop).start(this.mFinishingAnimType, true);
            MiuiCvwGestureController.Slog.d(TAG, "flingToRightSnapTarget mLastMovingBounds:" + this.mLastMovingBounds);
            if ((this.mRatioRecord.guess.mode == 1 || this.mRatioRecord.guess.mode == 2) && this.mLaunchWinMode != 0) {
                tryRemoveOverlays(0L);
            }
            if (this.mLaunchWinMode == 0) {
                this.mLayerSupervisor.removeBackgroundLayer(stack);
            }
        }
    }

    private void updateRecorderParams(MiuiFreeFormActivityStack stack) {
        Rect range;
        Rect rect;
        float freeformWindowHight;
        float freeformWindowWidth;
        float miniWindowHight;
        float miniWindowWidth;
        float rawRatio;
        if (this.mLaunchWinMode == 0) {
            range = this.mRatioRecord.rangeAraes[1];
        } else {
            range = this.mRatioRecord.rangeAraes[2];
        }
        float rangeRatio = (range.width() * 1.0f) / range.height();
        float startX = this.mDownTouchedMode == 0 ? this.mOriginalBounds.right : this.mOriginalBounds.left;
        float movingX = this.mDownTouchedMode == 0 ? this.mOriginalBounds.left : this.mOriginalBounds.right;
        float startY = this.mOriginalBounds.top;
        float movingY = this.mOriginalBounds.bottom;
        if (this.mTaskWrapperInfo.currentSnapTarget != null) {
            this.mRatioRecord.touchedRatio = this.mTaskWrapperInfo.currentSnapTarget.getRawRatio();
            this.mRatioRecord.miniWindowHight = this.mTaskWrapperInfo.currentSnapTarget.getMiniHeight();
            this.mRatioRecord.miniWindowWidth = this.mTaskWrapperInfo.currentSnapTarget.getMiniWidth();
            RatioRecord ratioRecord = this.mRatioRecord;
            ratioRecord.freeformWindowWidth = this.mCvwPolicy.getMaxWidthByRatio(ratioRecord.touchedRatio);
            RatioRecord ratioRecord2 = this.mRatioRecord;
            ratioRecord2.freeformWindowHight = this.mCvwPolicy.getMaxHeightByRatio(ratioRecord2.touchedRatio);
            float maxRatioX = this.mCvwPolicy.getMaxWidth() / range.width();
            float maxRatioY = this.mCvwPolicy.getMaxHeight() / range.height();
            this.mRatioRecord.minRatio[1] = this.mCvwPolicy.getMinHeightByRawRatio(this.mRatioRecord.touchedRatio) / range.height();
            this.mRatioRecord.maxRatio[0] = maxRatioX;
            this.mRatioRecord.maxRatio[1] = maxRatioY;
        } else {
            if (this.mTaskWrapperInfo.mSupportTargets.size() == 1) {
                MiuiCvwSnapTargetPool.SnapTarget target = this.mTaskWrapperInfo.mSupportTargets.get(0);
                miniWindowWidth = target.getMiniWidth();
                miniWindowHight = target.getMiniHeight();
                freeformWindowWidth = target.getMaxWidth();
                freeformWindowHight = target.getMaxHeight();
                rawRatio = target.getRawRatio();
            } else {
                miniWindowWidth = this.mCvwPolicy.mSnapTargetPool.mXlargeGeneralSnapTarget.getMiniWidth();
                miniWindowHight = this.mCvwPolicy.mSnapTargetPool.mXlargeGeneralSnapTarget.getMiniHeight();
                freeformWindowWidth = this.mCvwPolicy.mSnapTargetPool.mXlargeGeneralSnapTarget.getMaxWidth();
                freeformWindowHight = this.mCvwPolicy.mSnapTargetPool.mXlargeGeneralSnapTarget.getMaxHeight();
                rawRatio = this.mCvwPolicy.mSnapTargetPool.mXlargeGeneralSnapTarget.getRawRatio();
            }
            this.mRatioRecord.miniWindowWidth = miniWindowWidth;
            this.mRatioRecord.miniWindowHight = miniWindowHight;
            this.mRatioRecord.freeformWindowWidth = freeformWindowWidth;
            this.mRatioRecord.freeformWindowHight = freeformWindowHight;
            this.mRatioRecord.minRatio[1] = this.mCvwPolicy.getMinHeightByRawRatio(rawRatio) / range.height();
            this.mRatioRecord.maxRatio = getRatioPairByRange(range, range.right, range.bottom);
            this.mRatioRecord.touchedRatio = this.mScreenAspectRatio / this.mCvwPolicy.getRangeRatio();
        }
        RatioRecord ratioRecord3 = this.mRatioRecord;
        ratioRecord3.miniHRatio = (ratioRecord3.miniWindowHight / range.height()) * 1.0f;
        RatioRecord ratioRecord4 = this.mRatioRecord;
        ratioRecord4.miniRawRatio = (ratioRecord4.miniWindowWidth / this.mRatioRecord.miniWindowHight) / rangeRatio;
        this.mRatioRecord.stack = stack;
        this.mRatioRecord.preGuessWinMode = this.mLaunchWinMode;
        RatioRecord ratioRecord5 = this.mRatioRecord;
        ratioRecord5.preGuessRawRatio = ratioRecord5.touchedRatio;
        RatioRecord ratioRecord6 = this.mRatioRecord;
        if (this.mLaunchWinMode == 0) {
            rect = ratioRecord6.rangeAraes[0];
        } else {
            rect = ratioRecord6.rangeAraes[2];
        }
        ratioRecord6.startRatioPair = getRatioPairByRange(rect, startX, startY);
        RatioRecord ratioRecord7 = this.mRatioRecord;
        ratioRecord7.movingRatioPair = getRatioPairByRange(ratioRecord7.dragRangeArae, movingX, movingY);
    }

    private void updateRangeParams() {
        this.mScreenAspectRatio = this.mScreenWidth / this.mScreenHeight;
        int navBarHeight = this.mCvwPolicy.getNavBarHeight();
        int statusBarHeight = this.mCvwPolicy.getStatusBarHeight();
        int freeformRangeRight = this.mScreenWidth - 24;
        int freeformRangeBottom = (this.mScreenHeight - navBarHeight) - 75;
        int hoverRangeLeft = 24 + 30;
        int hoverRangeTop = statusBarHeight + 30;
        int hoverRangeRight = freeformRangeRight - 30;
        int hoverRangeBottom = (freeformRangeBottom - 30) + 75;
        int fullscreenRangeRight = this.mScreenWidth;
        int fullscreenRangeBottom = this.mScreenHeight;
        this.mRatioRecord.rangeAraes[2].set(24, statusBarHeight, freeformRangeRight, freeformRangeBottom);
        this.mRatioRecord.rangeAraes[0].set(0, 0, fullscreenRangeRight, fullscreenRangeBottom);
        this.mRatioRecord.rangeAraes[1].set(hoverRangeLeft, hoverRangeTop, hoverRangeRight, hoverRangeBottom);
        MiuiCvwPolicy.TaskWrapperInfo taskWrapperInfo = this.mTaskWrapperInfo;
        if (taskWrapperInfo != null) {
            if (taskWrapperInfo.supportCvw) {
                RatioRecord ratioRecord = this.mRatioRecord;
                ratioRecord.dragRangeArae = this.mLaunchWinMode == 0 ? ratioRecord.rangeAraes[1] : ratioRecord.rangeAraes[2];
            } else {
                RatioRecord ratioRecord2 = this.mRatioRecord;
                ratioRecord2.dragRangeArae = this.mLaunchWinMode == 0 ? ratioRecord2.rangeAraes[0] : ratioRecord2.rangeAraes[2];
            }
        }
        RatioRecord ratioRecord3 = this.mRatioRecord;
        ratioRecord3.preRangeArae = this.mLaunchWinMode == 0 ? ratioRecord3.rangeAraes[0] : ratioRecord3.rangeAraes[2];
        int rangeWidth = this.mRatioRecord.preRangeArae.width();
        int rangeHeight = this.mRatioRecord.preRangeArae.height();
        float rangeRatio = (rangeWidth * 1.0f) / rangeHeight;
        if (rangeRatio == MiuiFreeformPinManagerService.EDGE_AREA) {
            MiuiCvwGestureController.Slog.e(TAG, "borderRatio is invaild!");
        }
        this.mCvwPolicy.updateRangeRatio(rangeRatio);
    }

    private void initializeLayerSupervisor(MiuiFreeFormActivityStack stack) {
        this.mLayerSupervisor.checkOverlaysStatus();
        this.mLayerSupervisor.createTaskLeash(this.mTaskWrapperInfo);
        this.mLayerSupervisor.createBackgroundLayer(stack.mTask);
        this.mLayerSupervisor.useAt(stack);
        this.mLayerSupervisor.prepareSetTo(2, this.mOriginalBounds.left, 3, this.mOriginalBounds.top, 7, this.mOriginalBounds.right, 8, this.mOriginalBounds.bottom);
    }

    /* JADX WARN: Removed duplicated region for block: B:54:0x02df  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private void updateRatioRecord(float r29) {
        /*
            Method dump skipped, instructions count: 1077
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wm.MiuiCvwGestureHandlerImpl.updateRatioRecord(float):void");
    }

    private float[] getRatioPairByRange(Rect range, float x, float y) {
        float xPer;
        float left = range.left;
        float right = range.right;
        float top = range.top;
        float bottom = range.bottom;
        if (this.mDownTouchedMode == 1) {
            xPer = perFromVal(x, right, left);
        } else {
            xPer = perFromVal(x, left, right);
        }
        float yPer = perFromVal(y, bottom, top);
        return new float[]{Math.min(1.0f, Math.max((float) MiuiFreeformPinManagerService.EDGE_AREA, xPer)), Math.min(1.0f, Math.max((float) MiuiFreeformPinManagerService.EDGE_AREA, yPer))};
    }

    private float perFromVal(float val, float form, float to) {
        return (val - to) / (form - to);
    }

    private float valFromPer(float val, float form, float to) {
        return ((form - to) * val) + form;
    }

    public void updateScreenParams(DisplayContent displayContent, Configuration configuration) {
        updateScreenParams(displayContent, configuration, false);
    }

    private void updateScreenParams(DisplayContent displayContent, Configuration configuration, boolean ignoreSync) {
        int longSide = Math.max(displayContent.mBaseDisplayHeight, displayContent.mBaseDisplayWidth);
        int shortSide = Math.min(displayContent.mBaseDisplayHeight, displayContent.mBaseDisplayWidth);
        int ori = configuration.orientation;
        if (ori == 1) {
            this.mScreenHeight = longSide;
            this.mScreenWidth = shortSide;
            return;
        }
        this.mScreenHeight = shortSide;
        this.mScreenWidth = longSide;
    }

    private float afterFriction(float val, float range) {
        float t = val >= MiuiFreeformPinManagerService.EDGE_AREA ? 1.0f : -1.0f;
        float per = Math.min(Math.abs(val) / range, 1.0f);
        return (((((per * per) * per) / 3.0f) - (per * per)) + per) * t * range;
    }

    private void boundByRatio(Rect range, Rect result, WindowRatio info, boolean scale) {
        int i;
        int insetY;
        int insetX;
        if (info == null || range == null || result == null) {
            return;
        }
        if (this.mDownTouchedMode == 1) {
            result.left = range.left + ((int) (((range.right - range.left) * info.xRatio) + 0.5f));
            result.top = range.top + ((int) (((range.bottom - range.top) * info.yRatio) + 0.5f));
            result.right = (int) (result.left + ((range.right - range.left) * info.widRatio) + 0.5f);
            result.bottom = (int) (result.top + ((range.bottom - range.top) * info.heiRatio) + 0.5f);
        } else {
            result.right = range.right - ((int) (((range.right - range.left) * info.xRatio) + 0.5f));
            result.top = range.top + ((int) (((range.bottom - range.top) * info.yRatio) + 0.5f));
            result.left = result.right - ((int) (((range.right - range.left) * info.widRatio) + 0.5f));
            result.bottom = (int) (result.top + ((range.bottom - range.top) * info.heiRatio) + 0.5f);
        }
        if (scale && (i = this.mLaunchWinMode) != 0) {
            if (i != 1) {
                int insetY2 = (int) ((((this.mRatioRecord.freeformWindowHight * (-10.0f)) * 1.0f) / this.mRatioRecord.freeformWindowWidth) + 0.5f);
                insetX = (int) ((((float) MOVING_RECT_INSET) * 1.0f) / 2.0f);
                insetY = (int) ((insetY2 * 1.0f) / 2.0f);
            } else {
                insetX = MOVING_RECT_INSET;
                insetY = (int) ((((this.mRatioRecord.freeformWindowHight * (-10.0f)) * 1.0f) / this.mRatioRecord.freeformWindowWidth) + 0.5f);
            }
            result.inset(insetX, insetY);
        }
        if (!this.mTaskWrapperInfo.supportCvw) {
            int dX = (int) (((this.mScreenWidth - result.width()) / 2.0f) + 0.5f);
            int dY = (int) (((this.mScreenHeight - result.height()) / 2.0f) + 0.5f);
            if (this.mDownTouchedMode == 1) {
                result.offset(dX, dY);
            } else {
                result.offset(-dX, dY);
            }
        }
    }

    private void winResizeToRatio(MiuiFreeFormActivityStack stack, Rect winRange) {
        if (this.mController.gestureCanceled()) {
            return;
        }
        boundByRatio(winRange, this.mLastMovingBounds, this.mRatioRecord.current, true);
        if (!this.mTaskWrapperInfo.supportCvw) {
            this.mLayerSupervisor.scrollTo(this.mLastMovingBounds);
        } else {
            boolean isMini = this.mRatioRecord.current.mode == 2;
            int animType = isMini ? 7 : 6;
            float stiffness = this.mSynamicStiffness;
            this.mLayerSupervisor.spring(stiffness, 1.0f).to(2, this.mLastMovingBounds.left, 3, this.mLastMovingBounds.top, 7, this.mLastMovingBounds.right, 8, this.mLastMovingBounds.bottom).start(animType, false);
            MiuiCvwGestureController.Slog.d(TAG, "winResizeToRatio mLastMovingBounds:" + this.mLastMovingBounds + ",stiffness:" + stiffness);
            tryPreExecute();
        }
        this.mLayerSupervisor.updateBackgroundLayerStatus(stack, this.mLastMovingBounds, this.mOriginalBounds);
    }

    private void tryPreExecute() {
        int i;
        if (this.mRatioRecord.preGuessWinMode != this.mRatioRecord.guess.mode) {
            if (this.mLaunchWinMode == 0) {
                return;
            }
            if (this.mRatioRecord.preGuessWinMode == 1 && this.mRatioRecord.guess.mode == 0) {
                preStartFullscreen(this.mRatioRecord.stack);
            } else if (this.mRatioRecord.preGuessWinMode == 0 && 1 == this.mRatioRecord.guess.mode) {
                preStartFreeform(this.mRatioRecord.stack);
            } else if (this.mRatioRecord.preGuessWinMode == 1 && 2 == this.mRatioRecord.guess.mode && ((i = this.mLaunchWinMode) == 2 || i == 1)) {
                preResizeTask(this.mRatioRecord.stack);
            } else if (this.mRatioRecord.preGuessWinMode == 2) {
                int i2 = this.mRatioRecord.guess.mode;
            }
            RatioRecord ratioRecord = this.mRatioRecord;
            ratioRecord.preGuessWinMode = ratioRecord.guess.mode;
        } else if (this.mLaunchWinMode == 0) {
        } else {
            if (this.mRatioRecord.guess.mode == 2 && this.mLaunchWinMode == 2) {
                if (!equalRawRatio(this.mRatioRecord.preGuessRawRatio, this.mRatioRecord.guess.aspectRatio)) {
                    RatioRecord ratioRecord2 = this.mRatioRecord;
                    ratioRecord2.preGuessRawRatio = ratioRecord2.guess.aspectRatio;
                }
            } else if ((this.mRatioRecord.guess.mode == 1 || 2 == this.mRatioRecord.guess.mode) && !equalRawRatio(this.mRatioRecord.preGuessRawRatio, this.mRatioRecord.guess.aspectRatio)) {
                preResizeTask(this.mRatioRecord.stack);
                RatioRecord ratioRecord3 = this.mRatioRecord;
                ratioRecord3.preGuessRawRatio = ratioRecord3.guess.aspectRatio;
            }
        }
    }

    private void preStartFullscreen(MiuiFreeFormActivityStack stack) {
    }

    private void preStartFreeform(MiuiFreeFormActivityStack stack) {
        Rect winRange;
        if (stack != null) {
            if (this.mRatioRecord.guess.mode == 0) {
                winRange = this.mRatioRecord.rangeAraes[0];
            } else {
                winRange = this.mRatioRecord.rangeAraes[2];
            }
            Rect movingBounds = new Rect();
            boundByRatio(winRange, movingBounds, this.mRatioRecord.guess, false);
            fixPosition(winRange);
            float freeformRawRatio = this.mRatioRecord.guess.aspectRatio;
            int freeformLeft = movingBounds.left;
            int freeformTop = movingBounds.top;
            int freeformRight = movingBounds.left + ((int) (movingBounds.width() + 0.5f));
            int freeformBottom = movingBounds.top + ((int) (movingBounds.height() + 0.5f));
            Rect freeformWindowBounds = new Rect(freeformLeft, freeformTop, freeformRight, freeformBottom);
            this.mCvwPolicy.updateTmpFreeformTaskWrapper(freeformRawRatio, freeformWindowBounds);
            this.mLayerSupervisor.preStartFreeform(stack);
        }
    }

    private void preResizeTask(MiuiFreeFormActivityStack stack) {
        Rect winRange;
        int freeformBottom;
        int freeformRight;
        int freeformRight2;
        int freeformTop;
        if (stack != null) {
            if (this.mRatioRecord.guess.mode == 0) {
                winRange = this.mRatioRecord.rangeAraes[0];
            } else {
                winRange = this.mRatioRecord.rangeAraes[2];
            }
            Rect movingBounds = new Rect();
            boundByRatio(winRange, movingBounds, this.mRatioRecord.guess, false);
            fixPosition(winRange);
            float freeformRawRatio = this.mRatioRecord.guess.aspectRatio;
            int freeformLeft = movingBounds.left;
            int freeformTop2 = movingBounds.top;
            int freeformRight3 = movingBounds.left + ((int) (movingBounds.width() + 0.5f));
            int freeformBottom2 = movingBounds.top + ((int) (movingBounds.height() + 0.5f));
            Rect freeformWindowBounds = new Rect(freeformLeft, freeformTop2, freeformRight3, freeformBottom2);
            if (2 == this.mRatioRecord.guess.mode) {
                if (2 == this.mLaunchWinMode) {
                    freeformRawRatio = this.mRatioRecord.touchedRatio;
                    int freeformLeft2 = this.mTaskWrapperInfo.actualBounds.left;
                    int freeformTop3 = this.mTaskWrapperInfo.actualBounds.top;
                    int freeformRight4 = ((int) (this.mTaskWrapperInfo.actualBounds.width() * this.mTaskWrapperInfo.scale)) + freeformLeft2;
                    freeformBottom = ((int) (this.mTaskWrapperInfo.actualBounds.height() * this.mTaskWrapperInfo.scale)) + freeformTop3;
                    freeformRight = freeformRight4;
                    freeformRight2 = freeformTop3;
                    freeformTop = freeformLeft2;
                } else {
                    freeformRawRatio = this.mRatioRecord.touchedRatio;
                    int freeformLeft3 = this.mOriginalBounds.left;
                    int freeformTop4 = this.mOriginalBounds.top;
                    int freeformRight5 = this.mOriginalBounds.right;
                    freeformBottom = this.mOriginalBounds.bottom;
                    freeformRight = freeformRight5;
                    freeformRight2 = freeformTop4;
                    freeformTop = freeformLeft3;
                }
                freeformWindowBounds.set(freeformTop, freeformRight2, freeformRight, freeformBottom);
            }
            this.mCvwPolicy.updateTmpFreeformTaskWrapper(freeformRawRatio, freeformWindowBounds);
            this.mLayerSupervisor.preResizeTask(stack);
        }
    }

    public void updateTaskWrapperVisualBounds(Rect visualRect) {
        MiuiCvwPolicy.TaskWrapperInfo taskWrapperInfo = this.mTaskWrapperInfo;
        if (taskWrapperInfo == null) {
            return;
        }
        taskWrapperInfo.visualBounds.set(visualRect);
    }

    public void updateTaskWrapperVisualBounds(Rect visualRect, float visualScale) {
        MiuiCvwPolicy.TaskWrapperInfo taskWrapperInfo = this.mTaskWrapperInfo;
        if (taskWrapperInfo == null) {
            return;
        }
        taskWrapperInfo.visualBounds.set(visualRect);
        this.mTaskWrapperInfo.visualScale = visualScale;
    }

    public int getLaunchWinMode() {
        return this.mLaunchWinMode;
    }

    public boolean isGestureWinFreeform() {
        return this.mRatioRecord.guess.mode == 1;
    }

    public boolean isGestureWinFullscreen() {
        return this.mRatioRecord.guess.mode == 0;
    }

    public boolean isGestureWinMini() {
        return this.mRatioRecord.guess.mode == 2;
    }

    boolean requestedShowCoverLayer() {
        return this.mRatioChanged;
    }

    private void flingToRightSnapTarget(MiuiFreeFormActivityStack stack) {
    }

    private void fixPosition(Rect range) {
        if (this.mRatioRecord.guess.mode == 1) {
            float realGuessWidth = this.mCvwPolicy.getMaxWidthByRatio(this.mRatioRecord.guess.aspectRatio);
            float realGuessHeight = this.mCvwPolicy.getMaxHeightByRatio(this.mRatioRecord.guess.aspectRatio);
            float realRatio = realGuessWidth / realGuessHeight;
            float rangeWidth = range.width();
            float rangeHeight = range.height();
            if (realGuessWidth > rangeWidth) {
                realGuessWidth = rangeWidth;
                realGuessHeight = realGuessWidth / realRatio;
            } else if (realGuessHeight > rangeHeight) {
                realGuessHeight = rangeHeight;
                realGuessWidth = realGuessHeight * realRatio;
            }
            float maxScaleH = (this.mLastMovingBounds.height() * 1.0f) / realGuessHeight;
            float maxScaleW = (this.mLastMovingBounds.width() * 1.0f) / realGuessWidth;
            if (maxScaleW > 1.0f) {
                if (this.mDownTouchedMode == 0) {
                    Rect rect = this.mLastMovingBounds;
                    rect.left = rect.right - ((int) (0.5f + realGuessWidth));
                } else {
                    Rect rect2 = this.mLastMovingBounds;
                    rect2.right = rect2.left + ((int) (0.5f + realGuessWidth));
                }
            }
            if (maxScaleH > 1.0f) {
                Rect rect3 = this.mLastMovingBounds;
                rect3.bottom = rect3.top + ((int) realGuessHeight);
            }
        } else if (this.mRatioRecord.guess.mode == 0) {
            this.mLastMovingBounds.set(this.mRatioRecord.rangeAraes[0]);
        }
    }

    private void backToFullscreen(MiuiFreeFormActivityStack stack) {
        this.mFinishingAnimType = 5;
        int scrollToRight = this.mScreenWidth;
        int scrollToBottom = this.mScreenHeight;
        this.mController.cancelCVWGesture();
        this.mLayerSupervisor.spring(157.913f, 0.65f);
        this.mLayerSupervisor.prepareSetTo(2, this.mLastMovingBounds.left, 3, this.mLastMovingBounds.top, 7, this.mLastMovingBounds.right, 8, this.mLastMovingBounds.bottom);
        this.mLayerSupervisor.to(2, 0, 3, 0, 7, scrollToRight, 8, scrollToBottom);
        this.mLayerSupervisor.start(this.mFinishingAnimType, true);
    }

    public void tryRemoveOverlays(long delay) {
        if (isResizeFinished() && this.mController.isWindowAllDrawn()) {
            this.mLayerSupervisor.removeCoverLayer(delay);
            this.mLayerSupervisor.updateRefVisualBounds();
        }
    }

    public boolean launchFreeformFromFullscreen() {
        int i = this.mFinishingAnimType;
        return i == 3 || i == 4;
    }

    public boolean launchFullscreenFromFreeform() {
        int i = this.mFinishingAnimType;
        return i == 12 || i == 17;
    }

    public boolean isActivelyRemove() {
        int i = this.mFinishingAnimType;
        return i == 9 || i == 10;
    }

    public void onTaskVanished(Task task) {
        if (this.mController.gestureCanceled()) {
            return;
        }
        MiuiCvwGestureController.Slog.d(TAG, "onTaskVanished: " + task);
        this.mController.cancelCVWGesture();
        UiThread.getHandler().post(new Runnable() { // from class: com.android.server.wm.MiuiCvwGestureHandlerImpl$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                MiuiCvwGestureHandlerImpl.this.m1555x1b571721();
            }
        });
    }

    /* renamed from: lambda$onTaskVanished$0$com-android-server-wm-MiuiCvwGestureHandlerImpl */
    public /* synthetic */ void m1555x1b571721() {
        this.mLayerSupervisor.resetAllState();
    }

    private boolean isFixRatioFreeform() {
        return this.mLaunchWinMode != 0 && this.mTaskWrapperInfo.isFixedRatio;
    }

    private int stiffnessCalibration(boolean down) {
        int i = this.mLaunchWinMode;
        boolean isFullscreen = i == 0;
        boolean isFreeform = i == 1;
        boolean guessFreeform = this.mRatioRecord.current.mode == 1;
        boolean isCovering = this.mLayerSupervisor.isCovering();
        boolean exceedsThreshold = exceedsThresholdRadio(this.mRatioRecord.tmpRawRatio, this.mRatioRecord.touchedRatio, 0.15f);
        if ((this.mRatioRecord.lastWinMode == 2 && this.mRatioRecord.current.mode != 2) || (this.mRatioRecord.lastWinMode != -1 && this.mRatioRecord.lastWinMode != 2 && this.mRatioRecord.current.mode == 2)) {
            this.mSynamicStiffness = 700.0f;
            this.mController.mHandler.postDelayed(new Runnable() { // from class: com.android.server.wm.MiuiCvwGestureHandlerImpl$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    MiuiCvwGestureHandlerImpl.this.m1556xf67f82cf();
                }
            }, 150L);
            return 0;
        } else if (down && this.mLaunchWinMode != 0) {
            this.mSynamicStiffness = 400.0f;
            this.mController.mHandler.postDelayed(new Runnable() { // from class: com.android.server.wm.MiuiCvwGestureHandlerImpl$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    MiuiCvwGestureHandlerImpl.this.m1557xf6091cd0();
                }
            }, 200L);
            return 2;
        } else if ((!isFullscreen && ((!isFreeform && !guessFreeform) || !exceedsThreshold)) || isCovering || !this.mTaskWrapperInfo.supportCvw || this.mRatioChanged || isFixRatioFreeform()) {
            return -1;
        } else {
            this.mSynamicStiffness = 1000.0f;
            this.mController.mHandler.postDelayed(new Runnable() { // from class: com.android.server.wm.MiuiCvwGestureHandlerImpl$$ExternalSyntheticLambda3
                @Override // java.lang.Runnable
                public final void run() {
                    MiuiCvwGestureHandlerImpl.this.m1558xf592b6d1();
                }
            }, 100L);
            return 1;
        }
    }

    /* renamed from: lambda$stiffnessCalibration$1$com-android-server-wm-MiuiCvwGestureHandlerImpl */
    public /* synthetic */ void m1556xf67f82cf() {
        this.mSynamicStiffness = 10000.0f;
    }

    /* renamed from: lambda$stiffnessCalibration$2$com-android-server-wm-MiuiCvwGestureHandlerImpl */
    public /* synthetic */ void m1557xf6091cd0() {
        this.mSynamicStiffness = 10000.0f;
    }

    /* renamed from: lambda$stiffnessCalibration$3$com-android-server-wm-MiuiCvwGestureHandlerImpl */
    public /* synthetic */ void m1558xf592b6d1() {
        this.mSynamicStiffness = 10000.0f;
    }

    /* loaded from: classes.dex */
    public class WindowRatio {
        float aspectRatio;
        float heiRatio;
        int mode;
        float widRatio;
        float xRatio;
        float yRatio;

        public WindowRatio(float xRatio, float yRatio, float widRatio, float heiRatio, float aspectRatio, int mode) {
            MiuiCvwGestureHandlerImpl.this = this$0;
            this.xRatio = xRatio;
            this.yRatio = yRatio;
            this.widRatio = widRatio;
            this.heiRatio = heiRatio;
            this.aspectRatio = aspectRatio;
            this.mode = mode;
        }

        public WindowRatio() {
            MiuiCvwGestureHandlerImpl.this = this$0;
            this.xRatio = MiuiFreeformPinManagerService.EDGE_AREA;
            this.yRatio = MiuiFreeformPinManagerService.EDGE_AREA;
            this.widRatio = 1.0f;
            this.heiRatio = 1.0f;
            this.aspectRatio = 1.0f;
            this.mode = -1;
        }

        public void reset() {
            this.xRatio = MiuiFreeformPinManagerService.EDGE_AREA;
            this.yRatio = MiuiFreeformPinManagerService.EDGE_AREA;
            this.widRatio = 1.0f;
            this.heiRatio = 1.0f;
            this.aspectRatio = 1.0f;
            this.mode = -1;
        }

        public void copyFrom(WindowRatio from) {
            if (from == null) {
                return;
            }
            this.heiRatio = from.heiRatio;
            this.widRatio = from.widRatio;
            this.xRatio = from.xRatio;
            this.yRatio = from.yRatio;
            this.aspectRatio = from.aspectRatio;
            this.mode = from.mode;
        }

        public String toString() {
            return "WindowRatio xRatio:" + this.xRatio + " yRatio:" + this.yRatio + " widRatio:" + this.widRatio + " heiRatio:" + this.heiRatio + " aspectRatio:" + this.aspectRatio + " mode:" + this.mode;
        }
    }

    /* loaded from: classes.dex */
    public class RatioRecord {
        WindowRatio current;
        Rect dragRangeArae;
        float freeformWindowHight;
        float freeformWindowWidth;
        WindowRatio guess;
        float miniHRatio;
        float miniRawRatio;
        float miniWindowHight;
        float miniWindowWidth;
        float preGuessRawRatio;
        Rect preRangeArae;
        MiuiFreeFormActivityStack stack;
        float tmpRawRatio;
        float touchedRatio;
        float[] startRatioPair = new float[2];
        float[] movingRatioPair = new float[2];
        Rect[] rangeAraes = {new Rect(), new Rect(), new Rect()};
        float[] minRatio = new float[2];
        float[] maxRatio = new float[2];
        int preWinMode = -1;
        int lastWinMode = -1;
        int preGuessWinMode = -1;

        RatioRecord() {
            MiuiCvwGestureHandlerImpl.this = this$0;
            this.current = new WindowRatio();
            this.guess = new WindowRatio();
        }

        public void reset() {
            this.current.reset();
            this.guess.reset();
            float[] fArr = this.startRatioPair;
            fArr[0] = 0.0f;
            fArr[1] = 0.0f;
            this.touchedRatio = MiuiFreeformPinManagerService.EDGE_AREA;
            this.miniRawRatio = MiuiFreeformPinManagerService.EDGE_AREA;
            this.miniWindowWidth = MiuiFreeformPinManagerService.EDGE_AREA;
            this.miniWindowHight = MiuiFreeformPinManagerService.EDGE_AREA;
            this.miniHRatio = MiuiFreeformPinManagerService.EDGE_AREA;
            float[] fArr2 = this.minRatio;
            fArr2[0] = 0.0f;
            float[] fArr3 = this.maxRatio;
            fArr3[0] = 0.0f;
            fArr2[1] = 0.0f;
            fArr3[1] = 0.0f;
            this.stack = null;
            this.preRangeArae = null;
            this.preWinMode = -1;
            this.lastWinMode = -1;
            this.preGuessWinMode = -1;
            this.preGuessRawRatio = MiuiFreeformPinManagerService.EDGE_AREA;
            this.tmpRawRatio = MiuiFreeformPinManagerService.EDGE_AREA;
        }

        public String toString() {
            return "RatioRecord current:" + this.current.toString() + " \n  guess:" + this.guess.toString();
        }
    }

    @Override // com.android.server.wm.MiuiCvwGestureController.CvwGestureHandler
    public void dump(PrintWriter pw, String prefix) {
        String innerPrefix = prefix + "  ";
        pw.println("MiuiCvwGestureHandlerImpl:");
        pw.print(innerPrefix);
        pw.println("mScreenWidth:" + this.mScreenWidth);
        pw.print(innerPrefix);
        pw.println("mScreenHeight:" + this.mScreenHeight);
        pw.print(innerPrefix);
        pw.println("mDownTouchedMode:" + this.mDownTouchedMode);
        pw.print(innerPrefix);
        pw.println("mLaunchWinMode:" + this.mLaunchWinMode);
        pw.print(innerPrefix);
        pw.println("mRatioRecord:" + this.mRatioRecord);
        if (this.mRatioRecord.startRatioPair != null) {
            pw.println(innerPrefix + " mRatioRecord.startRatioPair:");
            for (int i = 0; i < this.mRatioRecord.startRatioPair.length; i++) {
                pw.print(innerPrefix);
                pw.println(" #" + i + ":" + this.mRatioRecord.startRatioPair[i]);
            }
        }
        if (this.mRatioRecord.rangeAraes != null) {
            pw.println(innerPrefix + " mRatioRecord.rangeAraes:");
            for (int i2 = 0; i2 < this.mRatioRecord.rangeAraes.length; i2++) {
                pw.print(innerPrefix);
                pw.println(" #" + i2 + ":" + this.mRatioRecord.rangeAraes[i2]);
            }
        }
        MiuiCvwPolicy.TaskWrapperInfo taskWrapperInfo = this.mTaskWrapperInfo;
        if (taskWrapperInfo != null) {
            taskWrapperInfo.dump(pw, prefix);
        }
        this.mLayerSupervisor.dump(pw, prefix);
    }
}
