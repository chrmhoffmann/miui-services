package com.android.server.wm;

import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.MiuiMultiWindowUtils;
import android.util.Slog;
import android.view.GestureDetector;
import android.view.MotionEvent;
import com.android.server.wm.MiuiCvwAnimator;
import com.android.server.wm.MiuiCvwPolicy;
import com.android.server.wm.MiuiFreeFormGestureAnimator;
import com.android.server.wm.MiuiFreeformTrackManager;
import java.util.function.Predicate;
/* loaded from: classes.dex */
public class MiuiFreeFormSmallWindowMotionHelper {
    public static final String TAG = "MiuiFreeFormSmallWindowMotionHelper";
    float mEndDragX;
    float mEndDragY;
    private MiuiFreeFormGesturePointerEventListener mListener;
    float mStartDragX;
    float mStartDragY;
    private int mCurrentAction = -1;
    private boolean mUpToExit = false;
    private VelocityMonitor mVelocityMonitor = new VelocityMonitor();
    public final GestureDetector.OnGestureListener mGestureListener = new GestureDetector.SimpleOnGestureListener() { // from class: com.android.server.wm.MiuiFreeFormSmallWindowMotionHelper.1
        @Override // android.view.GestureDetector.SimpleOnGestureListener, android.view.GestureDetector.OnDoubleTapListener
        public boolean onSingleTapConfirmed(MotionEvent motionEvent) {
            if (MiuiFreeFormSmallWindowMotionHelper.this.mListener.isInExcludeRegion((int) motionEvent.getX(), (int) motionEvent.getY())) {
                return false;
            }
            if (MiuiFreeFormSmallWindowMotionHelper.this.mListener.mGestureDetector.passedSlop(MiuiFreeFormSmallWindowMotionHelper.this.mEndDragX, MiuiFreeFormSmallWindowMotionHelper.this.mEndDragY, MiuiFreeFormSmallWindowMotionHelper.this.mStartDragX, MiuiFreeFormSmallWindowMotionHelper.this.mStartDragY)) {
                Slog.d(MiuiFreeFormSmallWindowMotionHelper.TAG, "skip onSingleTapConfirmed for passedSlop");
                return false;
            }
            MiuiFreeFormActivityStack mffas = MiuiFreeFormSmallWindowMotionHelper.this.mListener.mGestureController.getTopInMiniWindowActivityStack((int) motionEvent.getX(), (int) motionEvent.getY());
            if (mffas == null || mffas.inPinMode()) {
                return false;
            }
            Rect smallWindowBounds = new Rect(mffas.mStackControlInfo.mSmallWindowBounds);
            try {
                if (smallWindowBounds.contains((int) motionEvent.getX(), (int) motionEvent.getY())) {
                    Slog.d(MiuiFreeFormSmallWindowMotionHelper.TAG, "onSingleTapConfirmed");
                    MiuiFreeFormSmallWindowMotionHelper.this.startShowFreeFormWindow(null);
                    return true;
                }
            } catch (Exception e) {
            }
            return false;
        }

        @Override // android.view.GestureDetector.SimpleOnGestureListener, android.view.GestureDetector.OnDoubleTapListener
        public boolean onDoubleTap(MotionEvent motionEvent) {
            MiuiFreeFormActivityStack mffas;
            if (!MiuiFreeFormSmallWindowMotionHelper.this.mListener.isInExcludeRegion((int) motionEvent.getX(), (int) motionEvent.getY()) && (mffas = MiuiFreeFormSmallWindowMotionHelper.this.mListener.mGestureController.getTopInMiniWindowActivityStack((int) motionEvent.getX(), (int) motionEvent.getY())) != null && !mffas.inPinMode()) {
                Rect smallWindowBounds = new Rect(mffas.mStackControlInfo.mSmallWindowBounds);
                try {
                    if (smallWindowBounds.contains((int) motionEvent.getX(), (int) motionEvent.getY())) {
                        Slog.d(MiuiFreeFormSmallWindowMotionHelper.TAG, "onDoubleTap");
                        MiuiFreeFormSmallWindowMotionHelper.this.startShowFullScreenWindow(mffas);
                        return true;
                    }
                } catch (Exception e) {
                }
                return false;
            }
            return false;
        }
    };

    public MiuiFreeFormSmallWindowMotionHelper(MiuiFreeFormGesturePointerEventListener listener) {
        this.mListener = listener;
    }

    public void notifyDownLocked(MotionEvent motionEvent, MiuiFreeFormActivityStack stack) {
        stack.mStackBeenHandled = false;
        if (stack.mStackControlInfo.mCurrentAnimation != -1) {
            Slog.d(TAG, "notifyDownLocked will be return, because task has animation, type = " + stack.mStackControlInfo.mCurrentAnimation);
            return;
        }
        try {
            this.mStartDragX = motionEvent.getX();
            float y = motionEvent.getY();
            this.mStartDragY = y;
            if (this.mListener.isInExcludeRegion(this.mStartDragX, y)) {
                Slog.d(TAG, "should intercept touch event");
                return;
            }
            this.mCurrentAction = 0;
            stack.mStackControlInfo.mLastSmallWindowBounds = stack.mStackControlInfo.mSmallWindowBounds;
            Slog.d(TAG, "notifyDownLocked smallWindowBounds = " + stack.mStackControlInfo.mSmallWindowBounds);
            if (stack.mStackControlInfo.mSmallWindowBounds.contains((int) this.mStartDragX, (int) this.mStartDragY)) {
                Slog.d(TAG, "notifyDownLocked");
                stack.mStackBeenHandled = true;
                this.mVelocityMonitor.clear();
                this.mVelocityMonitor.update(motionEvent.getRawX(), motionEvent.getRawY());
                this.mListener.mGestureController.mMiuiFreeFormSwitchAppHelper.init(this.mListener.mService.mContext);
                this.mListener.mTaskPositioner.updateSmallWindowDownBounds(stack);
            }
        } catch (Exception e) {
        }
    }

    public boolean notifyMoveLocked(MotionEvent motionEvent, MiuiFreeFormActivityStack stack) {
        if (stack != null) {
            try {
                if (stack.mStackControlInfo != null && stack.mStackControlInfo.mCurrentAnimation == 6) {
                    Slog.d(TAG, "App has already exit small freeform.");
                    return false;
                }
            } catch (Exception e) {
            }
        }
        float x = motionEvent.getX();
        float y = motionEvent.getY();
        if (stack.mStackBeenHandled && this.mListener.mGestureDetector.passedSlop(x, y, this.mStartDragX, this.mStartDragY)) {
            this.mCurrentAction = 2;
            this.mVelocityMonitor.update(motionEvent.getRawX(), motionEvent.getRawY());
            float xVelocity = this.mVelocityMonitor.getVelocity(0);
            float yVelocity = this.mVelocityMonitor.getVelocity(1);
            if (MiuiFreeFormGestureController.DEBUG) {
                Slog.d(TAG, "notifyMoveLocked");
            }
            this.mListener.mGestureController.mMiuiFreeFormSwitchAppHelper.handleMoveGesture(x, y, xVelocity, yVelocity);
            this.mListener.mTaskPositioner.updateSmallWindowMoveBounds((int) x, (int) y, xVelocity, yVelocity, this.mStartDragX, this.mStartDragY, this.mListener.mIsPortrait, stack.mStackControlInfo.mLastSmallWindowBounds, stack);
            return true;
        }
        return false;
    }

    public boolean notifyUpLocked(MotionEvent motionEvent, MiuiFreeFormActivityStack stack) {
        Slog.d(TAG, "notifyUpLocked ");
        if (stack.mStackBeenHandled) {
            float x = motionEvent.getX();
            float y = motionEvent.getY();
            this.mVelocityMonitor.update(motionEvent.getRawX(), motionEvent.getRawY());
            float xVelocity = this.mVelocityMonitor.getVelocity(0);
            float yVelocity = this.mVelocityMonitor.getVelocity(1);
            this.mEndDragX = x;
            this.mEndDragY = y;
            boolean handled = this.mListener.mGestureController.mMiuiFreeFormSwitchAppHelper.handleUpGesture(xVelocity, yVelocity);
            if (handled) {
                return true;
            }
            if (this.mCurrentAction == 2) {
                try {
                    applyShowSmallWindowCornerAnimal((int) (MiuiFreeFormGestureDetector.getPredictMoveDistance(xVelocity) + x), (int) (MiuiFreeFormGestureDetector.getPredictMoveDistance(yVelocity) + y), stack, xVelocity, yVelocity);
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
        }
        return false;
    }

    public void applyShowSmallWindowCornerAnimal(int x, int y, MiuiFreeFormActivityStack stack, float xVelocity, float yVelocity) {
        int i;
        Rect cornerPoint;
        boolean z;
        Rect smallWindowBounds = new Rect(stack.mStackControlInfo.mSmallWindowBounds);
        int smallWindowCenterX = smallWindowBounds.left + (smallWindowBounds.width() / 2);
        int smallWindowCenterY = smallWindowBounds.top + (smallWindowBounds.height() / 2);
        float[] predictXY = MiuiMultiWindowUtils.getPredictXY(x, y, xVelocity, yVelocity, 0.8f);
        if (MiuiMultiWindowUtils.getScreenType(this.mListener.mService.mContext) == 3) {
            int offsetX = Math.round(x - this.mStartDragX);
            int offsetY = Math.round(y - this.mStartDragY);
            Rect cornerPoint2 = new Rect(stack.mStackControlInfo.mLastSmallWindowBounds);
            cornerPoint2.offsetTo(stack.mStackControlInfo.mLastSmallWindowBounds.left + offsetX, stack.mStackControlInfo.mLastSmallWindowBounds.top + offsetY);
            z = false;
            i = 3;
            cornerPoint = cornerPoint2;
        } else if (!this.mListener.mGestureController.mMiuiFreeFormManagerService.isSupportPin()) {
            i = 3;
            cornerPoint = MiuiMultiWindowUtils.findNearestCorner(this.mListener.mService.mContext, smallWindowCenterX, smallWindowCenterY, -1, xVelocity, yVelocity, stack.mIsLandcapeFreeform);
            z = false;
        } else {
            i = 3;
            z = false;
            cornerPoint = MiuiMultiWindowUtils.findNearestCorner(this.mListener.mService.mContext, predictXY[0], predictXY[1], -1, (float) MiuiFreeformPinManagerService.EDGE_AREA, (float) MiuiFreeformPinManagerService.EDGE_AREA, stack.mIsLandcapeFreeform);
        }
        if (MiuiMultiWindowUtils.getScreenType(this.mListener.mService.mContext) == i) {
            if (cornerPoint.left < this.mListener.mFreeFormAccessibleArea.left) {
                cornerPoint.offsetTo(this.mListener.mFreeFormAccessibleArea.left, cornerPoint.top);
            }
            if (cornerPoint.right > this.mListener.mFreeFormAccessibleArea.right) {
                cornerPoint.offsetTo(this.mListener.mFreeFormAccessibleArea.right - smallWindowBounds.width(), cornerPoint.top);
            }
            if (cornerPoint.top < this.mListener.mFreeFormAccessibleArea.top) {
                cornerPoint.offsetTo(cornerPoint.left, this.mListener.mFreeFormAccessibleArea.top);
            }
            if (cornerPoint.bottom > this.mListener.mFreeFormAccessibleArea.bottom) {
                cornerPoint.offsetTo(cornerPoint.left, this.mListener.mFreeFormAccessibleArea.bottom - smallWindowBounds.height());
            }
        } else {
            if (cornerPoint.left < 0) {
                int i2 = cornerPoint.top;
                int i3 = z ? 1 : 0;
                int i4 = z ? 1 : 0;
                int i5 = z ? 1 : 0;
                cornerPoint.offsetTo(i3, i2);
            }
            if (cornerPoint.right > this.mListener.mScreenWidth) {
                cornerPoint.offsetTo(this.mListener.mScreenWidth - smallWindowBounds.width(), cornerPoint.top);
            }
            if (cornerPoint.bottom > this.mListener.mScreenHeight) {
                cornerPoint.offsetTo(cornerPoint.left, this.mListener.mScreenHeight - smallWindowBounds.height());
            }
        }
        RectF smallFreeformRect = MiuiMultiWindowUtils.getSmallFreeformRect(this.mListener.mService.mContext, z, this.mListener.shortSide);
        float smallFreeformHeight = smallFreeformRect.height();
        if (this.mListener.mGestureController.mMiuiFreeFormManagerService.isSupportPin() && predictXY[1] < MiuiFreeformPinManagerService.EDGE_AREA && y < ((int) MiuiMultiWindowUtils.applyDip2Px(236.36f))) {
            float disY = predictXY[1] - y;
            char c = z ? 1 : 0;
            char c2 = z ? 1 : 0;
            char c3 = z ? 1 : 0;
            float disX = predictXY[c] - x;
            if (disY != MiuiFreeformPinManagerService.EDGE_AREA) {
                float k = disX / disY;
                float targetX = x + (((-(smallFreeformHeight + MiuiMultiWindowUtils.applyDip2Px(35.0f))) - y) * k);
                float disX2 = smallFreeformHeight + MiuiMultiWindowUtils.applyDip2Px(35.0f) + 0.5f;
                float k2 = smallFreeformHeight + MiuiMultiWindowUtils.applyDip2Px(35.0f) + 0.5f;
                cornerPoint.set((int) (targetX + 0.5f), (int) (-disX2), (int) (cornerPoint.width() + targetX + 0.5f), ((int) (-k2)) + cornerPoint.height());
            }
        } else if (Math.abs(yVelocity) > Math.abs(xVelocity) && yVelocity < MiuiFreeformPinManagerService.EDGE_AREA && y < MiuiMultiWindowUtils.SLIDE_OUT_POSITION_THRESHOLD && yVelocity < MiuiMultiWindowUtils.SLIDE_OUT_VELOCITY_THRESHOLD && !this.mListener.mGestureController.mMiuiFreeFormManagerService.isSupportPin()) {
            cornerPoint.set(cornerPoint.left, (int) ((-smallFreeformHeight) - MiuiMultiWindowUtils.SLIDE_OUT_FINAL_POSITION_OFFSITE), cornerPoint.right, (int) (((-smallFreeformHeight) - MiuiMultiWindowUtils.SLIDE_OUT_FINAL_POSITION_OFFSITE) + cornerPoint.height()));
            Slog.d(TAG, " Modify to exit cornerPoint:" + cornerPoint);
        }
        Slog.d(TAG, "smallWindowCenterX:" + smallWindowCenterX + " smallWindowCenterY:" + smallWindowCenterY + " smallWindowBounds:" + smallWindowBounds + " cornerPoint:" + cornerPoint);
        this.mListener.mGestureController.mTrackManager.trackMiniWindowMoveEvent(new Point(stack.mStackControlInfo.mLastSmallWindowBounds.left, stack.mStackControlInfo.mLastSmallWindowBounds.top), new Point(cornerPoint.left, cornerPoint.top), stack.getStackPackageName(), stack.getApplicationName());
        this.mListener.mTaskPositioner.updateSmallWindowUpBounds(x, y, xVelocity, yVelocity, this.mStartDragX, this.mStartDragY, this.mListener.mIsPortrait, smallWindowBounds, cornerPoint, stack);
        this.mCurrentAction = -1;
    }

    public void startShowFullScreenWindow(MiuiFreeFormActivityStack stack) {
        if (stack.isLaunchFlashBackFromBackGround()) {
            Slog.d(TAG, "startShowFullScreenWindow: stack.isLaunchFlashBackFromBackGround ");
            return;
        }
        int currentWindowMode = stack.mMiuiFreeFromWindowMode;
        MiuiFreeFormGestureAnimator.AnimalLock animalLock = this.mListener.mFreeFormWindowMotionHelper.mStackLocks.get(stack);
        if ((animalLock == null || animalLock.mCurrentAnimation == -1) && currentWindowMode == 1) {
            if (!stack.inPinMode()) {
                this.mListener.mGestureController.mTrackManager.trackMiniWindowQuitEvent(MiuiFreeformTrackManager.MiniWindowTrackConstants.QUIT_WAY_NAME1, new Point(stack.mStackControlInfo.mSmallWindowBounds.left, stack.mStackControlInfo.mSmallWindowBounds.top), stack.getStackPackageName(), stack.getApplicationName());
            }
            if (this.mListener.mIsPortrait == (!stack.mIsLandcapeFreeform)) {
                Slog.d(TAG, "startShowFullScreenWindow");
                this.mListener.mFreeFormWindowMotionHelper.startGestureAnimation(6, stack);
            } else {
                Rect startBounds = new Rect(stack.mStackControlInfo.mSmallWindowBounds);
                this.mListener.mGestureController.mWindowController.setStartBounds(startBounds);
                this.mListener.mGestureController.mWindowController.startContentAnimation(1, stack.getStackPackageName(), 5, stack);
                MiuiFreeFormWindowController.DropWindowType = 0;
            }
            this.mListener.mGestureController.mMiuiFreeFormFlashBackHelper.resetFlashBackWindowIfNeeded();
        }
    }

    public void startShowFreeFormWindow(final MiuiFreeFormActivityStack as) {
        final MiuiFreeFormManagerService service = this.mListener.mGestureController.mMiuiFreeFormManagerService;
        TaskDisplayArea defaultTaskDisplayArea = service.mActivityTaskManagerService.mRootWindowContainer.getDefaultTaskDisplayArea();
        Task bottomTask = defaultTaskDisplayArea.getTask(new Predicate() { // from class: com.android.server.wm.MiuiFreeFormSmallWindowMotionHelper$$ExternalSyntheticLambda1
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return MiuiFreeFormSmallWindowMotionHelper.lambda$startShowFreeFormWindow$0(MiuiFreeFormManagerService.this, as, (Task) obj);
            }
        }, false);
        if (bottomTask == null) {
            Slog.d(TAG, "startShowFreeFormWindow bottomTask == null");
            return;
        }
        MiuiFreeFormActivityStack stack = (MiuiFreeFormActivityStack) service.getMiuiFreeFormActivityStack(bottomTask.getRootTaskId());
        int currentWindowMode = stack.mMiuiFreeFromWindowMode;
        synchronized (this.mListener.mGestureController.mService.mGlobalLock) {
            this.mListener.mGestureController.mService.mAtmService.resumeAppSwitches();
        }
        MiuiFreeFormGestureAnimator.AnimalLock animalLock = this.mListener.mFreeFormWindowMotionHelper.mStackLocks.get(stack);
        if ((animalLock == null || animalLock.mCurrentAnimation == -1) && currentWindowMode == 1) {
            this.mListener.mGestureController.mTrackManager.trackMiniWindowQuitEvent(MiuiFreeformTrackManager.MiniWindowTrackConstants.QUIT_WAY_NAME2, new Point(stack.mStackControlInfo.mSmallWindowBounds.left, stack.mStackControlInfo.mSmallWindowBounds.top), stack.getStackPackageName(), stack.getApplicationName());
            this.mListener.mGestureController.mTrackManager.trackSmallWindowEnterWayEvent(MiuiFreeformTrackManager.SmallWindowTrackConstants.ENTER_WAY_NAME3, stack.mStackRatio, stack.getStackPackageName(), stack.getApplicationName(), service.mFreeFormActivityStacks.size());
            stack.setStackFreeFormMode(0);
            Slog.d(TAG, "startShowFreeFormWindow");
            this.mListener.mGestureController.mMiuiFreeFormFlashBackHelper.resetFlashBackWindowIfNeeded();
            this.mListener.mFreeFormWindowMotionHelper.startGestureAnimation(5, stack);
        }
    }

    public static /* synthetic */ boolean lambda$startShowFreeFormWindow$0(MiuiFreeFormManagerService service, MiuiFreeFormActivityStack as, Task t) {
        MiuiFreeFormActivityStack mffas;
        if (t.inFreeformSmallWinMode() && (mffas = (MiuiFreeFormActivityStack) service.getMiuiFreeFormActivityStack(t.getRootTaskId())) != null && mffas.isInMiniFreeFormMode() && as != mffas && !mffas.inPinMode()) {
            return true;
        }
        return false;
    }

    public boolean isUpToExit() {
        return this.mUpToExit;
    }

    public void setUpToExit(boolean upToExit) {
        this.mUpToExit = upToExit;
    }

    public void launchSmallFreeformByCVW(MiuiFreeFormActivityStack mffas, MiuiCvwAnimator animator, MiuiCvwGestureHandlerImpl miuiCvwGestureHandler, Rect taskBounds, Rect miniFreeformBounds, float freeFormScale, boolean islaunchSmallFreeformByFreeform) {
        try {
            Slog.d(TAG, "launchSmallFreeformByCVW mffas=" + mffas);
            MiuiCvwPolicy.TaskWrapperInfo info = miuiCvwGestureHandler.mCvwPolicy.getResultTaskWrapperInfo();
            startShowFreeFormWindow(mffas);
            synchronized (this.mListener.mService.mGlobalLock) {
                if (mffas.getFreeFormConrolSurface() == null) {
                    this.mListener.mGestureAnimator.createLeash(mffas);
                }
            }
            Rect scaleRect = new Rect(taskBounds.left, taskBounds.top, (int) (taskBounds.left + (taskBounds.width() * freeFormScale)), (int) (taskBounds.top + (taskBounds.height() * freeFormScale)));
            float xScale = (miniFreeformBounds.width() * 1.0f) / scaleRect.width();
            float yScale = (miniFreeformBounds.height() * 1.0f) / scaleRect.height();
            mffas.mStackControlInfo.mSmallWindowBounds.set(miniFreeformBounds);
            mffas.mStackControlInfo.mNowPosX = miniFreeformBounds.left;
            mffas.mStackControlInfo.mNowPosY = miniFreeformBounds.top;
            mffas.mStackControlInfo.mSmallWindowTargetWScale = xScale;
            mffas.mStackControlInfo.mSmallWindowTargetHScale = yScale;
            mffas.mStackControlInfo.mLastFreeFormWindowStartBounds.set(taskBounds);
            MiuiCvwAnimator.SurfaceParams params = new MiuiCvwAnimator.SurfaceParams.Builder(mffas).withMatrix(xScale, yScale).withPosition(miniFreeformBounds.left, miniFreeformBounds.top).build();
            animator.mergeTransactionToView(params);
            miuiCvwGestureHandler.updateTaskWrapperVisualBounds(info.visualBounds);
            mffas.setStackFreeFormMode(1);
            if (!islaunchSmallFreeformByFreeform) {
                this.mListener.startFreeformToSmallFreeFormWindow(1, mffas);
            } else {
                this.mListener.startFreeformToSmallFreeFormWindow(2, mffas);
            }
            Slog.d(TAG, "launchSmallFreeformByCVW taskBounds=" + taskBounds + " miniFreeformBounds=" + miniFreeformBounds + " freeFormScale=" + freeFormScale + " xScale =" + xScale + " yScale=" + yScale);
        } catch (Exception e) {
            Slog.d(TAG, "launchSmallFreeformByCVW failed");
            e.printStackTrace();
        }
    }

    public void applySmallFreeformCloseAnimation(final MiuiFreeFormActivityStack as) {
        final MiuiFreeFormManagerService service = this.mListener.mGestureController.mMiuiFreeFormManagerService;
        TaskDisplayArea defaultTaskDisplayArea = service.mActivityTaskManagerService.mRootWindowContainer.getDefaultTaskDisplayArea();
        Task bottomTask = defaultTaskDisplayArea.getTask(new Predicate() { // from class: com.android.server.wm.MiuiFreeFormSmallWindowMotionHelper$$ExternalSyntheticLambda0
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return MiuiFreeFormSmallWindowMotionHelper.lambda$applySmallFreeformCloseAnimation$1(MiuiFreeFormManagerService.this, as, (Task) obj);
            }
        }, false);
        if (bottomTask == null) {
            return;
        }
        MiuiFreeFormActivityStack stack = (MiuiFreeFormActivityStack) service.getMiuiFreeFormActivityStack(bottomTask.getRootTaskId());
        this.mListener.mGestureController.mTrackManager.trackMiniWindowQuitEvent("其他", new Point(stack.mStackControlInfo.mSmallWindowBounds.left, stack.mStackControlInfo.mSmallWindowBounds.top), stack.getStackPackageName(), stack.getApplicationName());
        Slog.d(TAG, "applySmallFreeformCloseAnimation");
        float finalPosX = stack.mStackControlInfo.mSmallWindowBounds.centerX();
        float finalPosY = stack.mStackControlInfo.mSmallWindowBounds.centerY();
        float nowAlpha = stack.mStackControlInfo.mNowAlpha;
        float startPosX = stack.mStackControlInfo.mNowPosX;
        float startPosY = stack.mStackControlInfo.mNowPosY;
        MiuiFreeFormSpringAnimation scaleXSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, stack.mStackControlInfo.mNowWidthScale, MiuiFreeformPinManagerService.EDGE_AREA, 130.5f, 0.75f, MiuiFreeformPinManagerService.EDGE_AREA, 5);
        MiuiFreeFormSpringAnimation scaleYSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, stack.mStackControlInfo.mNowHeightScale, MiuiFreeformPinManagerService.EDGE_AREA, 130.5f, 0.75f, MiuiFreeformPinManagerService.EDGE_AREA, 6);
        MiuiFreeFormSpringAnimation alphaSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, nowAlpha, MiuiFreeformPinManagerService.EDGE_AREA, 130.5f, 0.75f, MiuiFreeformPinManagerService.EDGE_AREA, 1);
        MiuiFreeFormSpringAnimation tXSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, startPosX, finalPosX, 130.5f, 0.75f, MiuiFreeformPinManagerService.EDGE_AREA, 2);
        MiuiFreeFormSpringAnimation tYSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, startPosY, finalPosY, 130.5f, 0.75f, MiuiFreeformPinManagerService.EDGE_AREA, 3);
        alphaSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.ALPHA_END_LISTENER));
        scaleXSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.SCALE_X_END_LISTENER));
        scaleYSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.SCALE_Y_END_LISTENER));
        tXSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.TRANSLATE_X_END_LISTENER));
        tYSpringAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.TRANSLATE_Y_END_LISTENER));
        MiuiFreeFormGestureAnimator.AnimalLock animalLock = new MiuiFreeFormGestureAnimator.AnimalLock(stack);
        animalLock.mScaleXAnimation = scaleXSpringAnimation;
        animalLock.mScaleYAnimation = scaleYSpringAnimation;
        animalLock.mAlphaAnimation = alphaSpringAnimation;
        animalLock.mTranslateXAnimation = tXSpringAnimation;
        animalLock.mTranslateYAnimation = tYSpringAnimation;
        this.mListener.mFreeFormWindowMotionHelper.mStackLocks.put(stack, animalLock);
        animalLock.start(24);
    }

    public static /* synthetic */ boolean lambda$applySmallFreeformCloseAnimation$1(MiuiFreeFormManagerService service, MiuiFreeFormActivityStack as, Task t) {
        MiuiFreeFormActivityStack mffas;
        if (t.inFreeformSmallWinMode() && (mffas = (MiuiFreeFormActivityStack) service.getMiuiFreeFormActivityStack(t.getRootTaskId())) != null && mffas.isInMiniFreeFormMode() && as != mffas && !mffas.inPinMode()) {
            return true;
        }
        return false;
    }
}
