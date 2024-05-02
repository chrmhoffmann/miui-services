package com.android.server.wm;

import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.ArraySet;
import android.util.MiuiMultiWindowAdapter;
import android.util.MiuiMultiWindowUtils;
import android.util.Slog;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceControl;
import android.view.VelocityTracker;
import android.view.WindowManagerPolicyConstants;
import com.android.server.LocalServices;
import com.android.server.inputmethod.InputMethodManagerInternal;
import com.android.server.wm.MiuiFreeFormGestureAnimator;
import com.google.android.collect.Sets;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
/* loaded from: classes.dex */
public class MiuiFreeFormGesturePointerEventListener implements WindowManagerPolicyConstants.PointerEventListener {
    private static final int EVENT_SEND_TO_HANDWRITING = 134217728;
    private static final int MSG_CHECK_MULTI_WINDOW_SWITCH = 2;
    private static final int MSG_PROCESS_EVENT = 3;
    public static final String TAG = "MiuiFreeFormGesturePointerEventListener";
    private MiuiFreeFormActivityStack mCurrentControlActivityStack;
    int mDisplayRotation;
    private long mFirstEnterHotAreaTime;
    MiuiFreeFormGestureAnimator mGestureAnimator;
    MiuiFreeFormGestureController mGestureController;
    Handler mHandler;
    private boolean mInMultiTouch;
    InputManagerCallback mInputManagerCallback;
    private InputMethodManagerInternal mInputMethodManagerInternal;
    private boolean mIsEnterHotArea;
    boolean mIsPortrait;
    private MotionEvent mLastMotionEvent;
    private int mLastOrientation;
    int mLongSide;
    private Handler mMainHandler;
    int mNotchBar;
    int mScreenHeight;
    int mScreenWidth;
    WindowManagerService mService;
    private GestureDetector mTouchGestureDetector;
    private VelocityTracker mVelocityTracker;
    int shortSide;
    final Object mLock = new Object();
    private Region mTouchExcludeRegion = new Region();
    Rect mFreeFormAccessibleArea = new Rect();
    private boolean mHasTriggerActioned = false;
    private boolean mHasShowedToast = false;
    private volatile boolean mHasThawInputDispatch = false;
    private boolean mHasDownMotionEvent = false;
    private boolean mInExcludeRegion = false;
    final Object mLaunchSmallFreeformLock = new Object();
    final Object mLaunchFreeformFromControlCenterLock = new Object();
    private int mWaitThreshold = MiuiMultiWindowUtils.LONG_WAIT;
    Runnable runnable = new Runnable() { // from class: com.android.server.wm.MiuiFreeFormGesturePointerEventListener.1
        @Override // java.lang.Runnable
        public void run() {
            for (MiuiFreeFormActivityStack stack : MiuiFreeFormGesturePointerEventListener.this.mGestureController.mMiuiFreeFormManagerService.mFreeFormActivityStacks.values()) {
                if (stack.isControlled()) {
                    stack.endControl();
                }
            }
        }
    };
    MiuiFreeFormSmallWindowMotionHelper mSmallFreeFormWindowMotionHelper = new MiuiFreeFormSmallWindowMotionHelper(this);
    MiuiFreeFormWindowMotionHelper mFreeFormWindowMotionHelper = new MiuiFreeFormWindowMotionHelper(this);
    MiuiFreeFormTaskPositioner mTaskPositioner = new MiuiFreeFormTaskPositioner(this);
    MiuiFreeFormGestureDetector mGestureDetector = new MiuiFreeFormGestureDetector(this);

    /* loaded from: classes.dex */
    public static final class MsgObject {
        MiuiFreeFormActivityStack mffas;
        MotionEvent motionEvent;

        private MsgObject() {
        }
    }

    public MiuiFreeFormGesturePointerEventListener(WindowManagerService wms, MiuiFreeFormGestureController controller) {
        this.mService = wms;
        this.mGestureController = controller;
        this.mHandler = new H(this.mGestureController.mHandler.getLooper());
        this.mTouchGestureDetector = new GestureDetector(this.mService.mContext, this.mSmallFreeFormWindowMotionHelper.mGestureListener, this.mHandler);
        this.mGestureAnimator = new MiuiFreeFormGestureAnimator(this.mService, this);
        this.mInputManagerCallback = new InputManagerCallback(this.mService);
        updateScreenParams(this.mGestureController.mDisplayContent, this.mService.mContext.getResources().getConfiguration());
    }

    public void onPointerEvent(MotionEvent event) {
        MotionEvent clonedEvent = MotionEvent.obtain(event);
        if (this.mMainHandler == null) {
            this.mMainHandler = new Handler(Looper.myLooper());
        }
        if (event.isTouchEvent()) {
            if (event.getActionMasked() == 0) {
                this.mMainHandler.removeCallbacks(this.runnable);
            } else if (event.getActionMasked() == 1 || event.getActionMasked() == 3) {
                this.mMainHandler.postDelayed(this.runnable, 1200L);
            }
        }
        this.mHandler.obtainMessage(3, clonedEvent).sendToTarget();
    }

    public boolean isInExcludeRegion(float x, float y) {
        Task homeTask = this.mGestureController.mDisplayContent.getDefaultTaskDisplayArea().getRootHomeTask();
        synchronized (this.mTouchExcludeRegion) {
            if (MiuiMultiWindowUtils.isInFullScreenNavHotArea(this.mService.mContext, x, y, homeTask != null && homeTask.isVisible())) {
                return true;
            }
            List<Rect> curPinList = this.mGestureController.mMiuiFreeFormManagerService.getCurrPinFloatingWindowPos(false);
            for (Rect pinedRect : curPinList) {
                if (pinedRect.contains((int) x, (int) y)) {
                    return true;
                }
            }
            return this.mTouchExcludeRegion.contains((int) x, (int) y);
        }
    }

    public void setMiuiFreeFormTouchExcludeRegion(Region region) {
        synchronized (this.mTouchExcludeRegion) {
            this.mTouchExcludeRegion.set(region);
            if (MiuiFreeFormGestureController.DEBUG) {
                Slog.w(TAG, "setMiuiFreeFormTouchExcludeRegion mTouchExcludeRegion = " + this.mTouchExcludeRegion);
            }
        }
    }

    public void startFreeformToSmallFreeFormWindow(int action, MiuiFreeFormActivityStack stack) {
        MiuiFreeFormActivityStack topMiniPinedFreeform;
        Slog.d(TAG, "startFreeformToSmallFreeFormWindow");
        this.mGestureController.mMiuiFreeFormSwitchAppHelper.startSwitchAppFourStepIfNeeded();
        if (MiuiMultiWindowUtils.multiFreeFormSupported(this.mService.mContext) && (topMiniPinedFreeform = findTopMiniPinedFreeform(stack)) != null) {
            topMiniPinedFreeform.setStackFreeFormMode(0);
            topMiniPinedFreeform.removeAnimationControlLeash();
            topMiniPinedFreeform.mWindowRoundCorner = MiuiMultiWindowUtils.getFreeformRoundCorner(this.mService.mContext);
            topMiniPinedFreeform.mWindowScaleX = 1.0f;
            topMiniPinedFreeform.mWindowScaleY = 1.0f;
            this.mGestureController.mMiuiFreeFormManagerService.dispatchFreeFormStackModeChanged(14, topMiniPinedFreeform);
        }
        stack.setStackFreeFormMode(1);
        this.mGestureController.notifyStartFreeformToSmallFreeFormWindow(action, stack);
        WindowState topFullWindow = this.mFreeFormWindowMotionHelper.getTopWindow(1);
        synchronized (this.mService.mGlobalLock) {
            DisplayContent dc = this.mGestureController.mDisplayContent;
            if (topFullWindow != null && topFullWindow.mActivityRecord != null) {
                dc.setFocusedApp(topFullWindow.mActivityRecord);
            }
            stack.setIsFlashBackMode(false);
            this.mService.mAtmService.setFocusedTask(stack.mTask.mTaskId);
            this.mService.updateFocusedWindowLocked(0, true);
            SurfaceControl.Transaction inputTransaction = new SurfaceControl.Transaction();
            dc.getInputMonitor().updateInputWindowsImmediately(inputTransaction);
            inputTransaction.apply();
        }
    }

    private MiuiFreeFormActivityStack findTopMiniPinedFreeform(final MiuiFreeFormActivityStack stack) {
        final MiuiFreeFormManagerService service = this.mGestureController.mMiuiFreeFormManagerService;
        TaskDisplayArea defaultTaskDisplayArea = service.mActivityTaskManagerService.mRootWindowContainer.getDefaultTaskDisplayArea();
        MiuiFreeFormActivityStack miniPinedFreeformStack = null;
        if (defaultTaskDisplayArea != null) {
            synchronized (this.mService.mGlobalLock) {
                Task miniPinedFreeform = defaultTaskDisplayArea.getTask(new Predicate() { // from class: com.android.server.wm.MiuiFreeFormGesturePointerEventListener$$ExternalSyntheticLambda0
                    @Override // java.util.function.Predicate
                    public final boolean test(Object obj) {
                        return MiuiFreeFormGesturePointerEventListener.lambda$findTopMiniPinedFreeform$0(MiuiFreeFormManagerService.this, stack, (Task) obj);
                    }
                }, true);
                if (miniPinedFreeform == null) {
                    Slog.d(TAG, "find Top miniPined Freeform Task == null");
                    return null;
                }
                miniPinedFreeformStack = (MiuiFreeFormActivityStack) service.getMiuiFreeFormActivityStack(miniPinedFreeform.getRootTaskId());
            }
        }
        return miniPinedFreeformStack;
    }

    public static /* synthetic */ boolean lambda$findTopMiniPinedFreeform$0(MiuiFreeFormManagerService service, MiuiFreeFormActivityStack stack, Task t) {
        MiuiFreeFormActivityStack mffas;
        if (t.inFreeformSmallWinMode() && (mffas = (MiuiFreeFormActivityStack) service.getMiuiFreeFormActivityStack(t.getRootTaskId())) != null && mffas.isInMiniFreeFormMode() && stack != mffas && mffas.inPinMode()) {
            return true;
        }
        return false;
    }

    public void turnFreeFormToSmallWindow(MiuiFreeFormActivityStack stack) {
        Slog.d(TAG, "turnFreeFormToSmallWindow");
        if (stack.mMiuiFreeFromWindowMode == 0 && stack.mTaskAnimationAdapter != null && stack.mTaskAnimationAdapter.mCapturedLeash != null && stack.mTaskAnimationAdapter.mCapturedLeash.isValid()) {
            return;
        }
        MiuiFreeFormGestureAnimator.AnimalLock animalLock = this.mFreeFormWindowMotionHelper.mStackLocks.get(stack);
        if (animalLock != null && animalLock.mCurrentAnimation != -1) {
            Slog.d(TAG, "animalLock.mCurrentAnimation:" + animalLock.mCurrentAnimation + " animation is in progress turnFreeFormToSmallWindow()");
        } else if (stack.mStackControlInfo.mCurrentAnimation != -1) {
            Slog.d(TAG, "stack.mStackControlInfo.mCurrentAnimation: " + stack.mStackControlInfo.mCurrentAnimation + " animation is in progress turnFreeFormToSmallWindow()");
        } else if (stack.mMiuiFreeFromWindowMode == 0 && this.mService.mTaskSnapshotController != null) {
            stack.mStackBeenHandled = false;
            reflectHandleSnapshotTaskByFreeform(stack.mTask);
            stack.mStackControlInfo.mLastFreeFormWindowStartBounds = new Rect(stack.mTask.getBounds());
            stack.setStackFreeFormMode(1);
            this.mFreeFormWindowMotionHelper.turnFreeFormToSmallWindow(stack);
        }
    }

    public void startFullScreenFromFreeFormAnimation(MiuiFreeFormActivityStack stack) {
        MiuiFreeFormGestureAnimator.AnimalLock animalLock = this.mFreeFormWindowMotionHelper.mStackLocks.get(stack);
        if (animalLock == null || animalLock.mCurrentAnimation == -1) {
            if (stack.mStackControlInfo.mCurrentAnimation != -1) {
                Slog.d(TAG, "startFullScreenFromFreeFormAnimation stack.mStackControlInfo.mCurrentAnimation: " + stack.mStackControlInfo.mCurrentAnimation);
                return;
            }
            if (!stack.inPinMode()) {
                this.mGestureController.mTrackManager.trackSmallWindowQuitEvent("其他", stack.mStackRatio, stack.getStackPackageName(), stack.getApplicationName(), -1L);
            }
            synchronized (this.mService.mGlobalLock) {
                if (stack.getFreeFormConrolSurface() == null) {
                    this.mGestureAnimator.createLeash(stack);
                }
            }
            if (!this.mIsPortrait) {
                this.mGestureController.mWindowController.setStartBounds(new Rect(stack.mTask.getBounds().left, stack.mTask.getBounds().top, (int) (stack.mTask.getBounds().left + (stack.mTask.getBounds().width() * stack.mFreeFormScale)), (int) (stack.mTask.getBounds().top + (stack.mTask.getBounds().height() * stack.mFreeFormScale))));
                this.mGestureController.mWindowController.startContentAnimation(1, stack.getStackPackageName(), 5, stack);
                return;
            } else if (stack.mIsLandcapeFreeform) {
                MiuiFreeFormWindowController.DropWindowType = 0;
                this.mGestureController.mWindowController.setStartBounds(new Rect(stack.mTask.getBounds().left, stack.mTask.getBounds().top, (int) (stack.mTask.getBounds().left + (stack.mTask.getBounds().width() * stack.mFreeFormScale)), (int) (stack.mTask.getBounds().top + (stack.mTask.getBounds().height() * stack.mFreeFormScale))));
                this.mGestureController.mWindowController.startContentAnimation(1, stack.getStackPackageName(), 5, stack);
                return;
            } else {
                this.mFreeFormWindowMotionHelper.startGestureAnimation(15, stack);
                return;
            }
        }
        Slog.d(TAG, "startFullScreenFromFreeFormAnimation abnormal currentAnimation: " + animalLock.mCurrentAnimation);
    }

    public void startFullScreenFromSmallAnimation(MiuiFreeFormActivityStack mffas) {
        this.mSmallFreeFormWindowMotionHelper.startShowFullScreenWindow(mffas);
    }

    public void upwardMovementSmallWindow(int position, MiuiFreeFormActivityStack stack) {
        Rect finalWindowDragBounds = MiuiMultiWindowUtils.findNearestCorner(this.mService.mContext, (float) MiuiFreeformPinManagerService.EDGE_AREA, (float) MiuiFreeformPinManagerService.EDGE_AREA, position, stack.mIsLandcapeFreeform);
        Rect startWindowDragBounds = new Rect(stack.mStackControlInfo.mSmallWindowBounds);
        this.mTaskPositioner.smallFreeFormTranslateScaleAnimal(startWindowDragBounds, finalWindowDragBounds, 9, true, stack);
    }

    public void inputMethodVisibleChanged(int inputMethodHeight, MiuiFreeFormActivityStack stack) {
        if (stack != null && stack.isInMiniFreeFormMode() && inputMethodHeight != 0 && stack.mStackControlInfo.mSmallWindowBounds.top > this.mScreenHeight / 2) {
            if ((stack.mStackControlInfo.mSmallWindowBounds.left + stack.mStackControlInfo.mSmallWindowBounds.right) / 2 > this.mScreenWidth / 2) {
                upwardMovementSmallWindow(2, stack);
            } else {
                upwardMovementSmallWindow(1, stack);
            }
        }
    }

    public void notifyRecentAnimationFinished() {
        synchronized (this.mLaunchSmallFreeformLock) {
            this.mLaunchSmallFreeformLock.notifyAll();
        }
    }

    public void launchSmallFreeFormWindow(final MiuiFreeFormActivityStack stack, final boolean launchFromControlCenter) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreeFormGesturePointerEventListener.2
            @Override // java.lang.Runnable
            public void run() {
                long timeoutAtTimeMs = System.currentTimeMillis() + 500;
                synchronized (MiuiFreeFormGesturePointerEventListener.this.mLaunchSmallFreeformLock) {
                    while (MiuiFreeFormGesturePointerEventListener.this.mService.getRecentsAnimationController() != null) {
                        try {
                            long waitMillis = timeoutAtTimeMs - System.currentTimeMillis();
                            if (waitMillis <= 0) {
                                break;
                            }
                            MiuiFreeFormGesturePointerEventListener.this.mLaunchSmallFreeformLock.wait(waitMillis);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
                long timeoutAtTimeMs2 = System.currentTimeMillis() + 500;
                if (launchFromControlCenter && !stack.mIsLandcapeFreeform) {
                    synchronized (MiuiFreeFormGesturePointerEventListener.this.mLaunchFreeformFromControlCenterLock) {
                        try {
                            WindowState topFullScreenWindow = MiuiFreeFormGesturePointerEventListener.this.mFreeFormWindowMotionHelper.getTopWindow(1);
                            while (topFullScreenWindow != null) {
                                if (topFullScreenWindow.isDrawFinishedLw()) {
                                    break;
                                }
                                long waitMillis2 = timeoutAtTimeMs2 - System.currentTimeMillis();
                                if (waitMillis2 <= 0) {
                                    break;
                                }
                                MiuiFreeFormGesturePointerEventListener.this.mLaunchFreeformFromControlCenterLock.wait(waitMillis2);
                            }
                        } catch (InterruptedException e2) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
                stack.mTask.mLaunchMiniFreeformFromFull = false;
                Rect corner = MiuiMultiWindowUtils.findNearestCorner(MiuiFreeFormGesturePointerEventListener.this.mService.mContext, (float) MiuiFreeformPinManagerService.EDGE_AREA, (float) MiuiFreeformPinManagerService.EDGE_AREA, stack.mCornerPosition, stack.mIsLandcapeFreeform);
                synchronized (MiuiFreeFormGesturePointerEventListener.this.mService.mGlobalLock) {
                    stack.setStackFreeFormMode(1);
                    if (!corner.isEmpty()) {
                        stack.mStackControlInfo.mSmallWindowBounds = new Rect(corner);
                        stack.mStackControlInfo.mNowPosX = stack.mStackControlInfo.mSmallWindowBounds.left;
                        stack.mStackControlInfo.mNowPosY = stack.mStackControlInfo.mSmallWindowBounds.top;
                    }
                    SurfaceControl.Transaction inputTransaction = new SurfaceControl.Transaction();
                    MiuiFreeFormGesturePointerEventListener.this.mGestureController.mDisplayContent.getInputMonitor().updateInputWindowsImmediately(inputTransaction);
                    inputTransaction.apply();
                }
                if (launchFromControlCenter) {
                    MiuiFreeFormGesturePointerEventListener.this.mGestureController.mTrackManager.trackSmallWindowEnterWayEvent("控制中心", stack.mStackRatio, stack.getStackPackageName(), stack.getApplicationName(), MiuiFreeFormGesturePointerEventListener.this.mGestureController.mMiuiFreeFormManagerService.getCurrentMiuiFreeFormNum());
                }
                MiuiFreeFormActivityStack topActivityStack = MiuiFreeFormGesturePointerEventListener.this.synchronizeFreeFormStackInfo();
                Slog.d(MiuiFreeFormGesturePointerEventListener.TAG, "launchSmallFreeFormWindow stack=" + stack);
                if (MiuiFreeFormGesturePointerEventListener.this.mGestureController.isSwitchingApp() && topActivityStack == null) {
                    Slog.d(MiuiFreeFormGesturePointerEventListener.TAG, "exchange failed for not find freeform window");
                    try {
                        MiuiFreeFormGesturePointerEventListener.this.mGestureController.mMiuiFreeFormSwitchAppHelper.startSwitchAppFourStep();
                        return;
                    } catch (Exception e3) {
                        return;
                    }
                }
                synchronized (MiuiFreeFormGesturePointerEventListener.this.mService.mGlobalLock) {
                    if (stack.getFreeFormConrolSurface() == null) {
                        MiuiFreeFormGesturePointerEventListener.this.mGestureAnimator.createLeash(stack);
                    }
                }
                if (stack.isLaunchFlashBackFromBackGround()) {
                    MiuiFreeFormGesturePointerEventListener.this.startSmallFreeformWithoutAnimation(stack);
                } else if (stack.mIsLandcapeFreeform) {
                    MiuiFreeFormGesturePointerEventListener.this.startSmallFreeformWithoutAnimation(stack);
                } else {
                    MiuiFreeFormActivityStack homeStack = MiuiFreeFormGesturePointerEventListener.this.mGestureController.mMiuiFreeFormManagerService.getHomeActivityStack();
                    if (homeStack != null) {
                        MiuiFreeFormGesturePointerEventListener.this.mFreeFormWindowMotionHelper.startGestureAnimation(14, homeStack);
                    }
                    MiuiFreeFormGesturePointerEventListener.this.mFreeFormWindowMotionHelper.startGestureAnimation(12, stack);
                }
            }
        });
    }

    public void startSmallFreeformWithoutAnimation(MiuiFreeFormActivityStack stack) {
        float endY;
        this.mGestureAnimator.hideStack(stack);
        this.mGestureAnimator.applyTransaction();
        String pkgName = stack.getStackPackageName();
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.LAUNCHER");
        intent.setPackage(pkgName);
        ResolveInfo rInfo = this.mService.mContext.getPackageManager().resolveActivity(intent, 0);
        if (rInfo != null && MiuiMultiWindowAdapter.LIST_ABOUT_IGNORE_REQUEST_ORIENTATION_IN_FREEFORM.contains(pkgName)) {
            stack.mIsLandcapeFreeform = MiuiMultiWindowUtils.isOrientationLandscape(rInfo.activityInfo.screenOrientation) || MiuiMultiWindowAdapter.FORCE_LANDSCAPE_APPLICATION.contains(pkgName);
        }
        int displayRotation = this.mGestureController.mDisplayContent.getRotation();
        int statusBarHeight = Math.max(MiuiFreeFormGestureDetector.getStatusBarHeight(this.mGestureController.mInsetsStateController), MiuiFreeFormGestureDetector.getDisplayCutoutHeight(this.mGestureController.mDisplayContent.mDisplayFrames));
        Rect lastBounds = MiuiMultiWindowUtils.getFreeformRect(this.mService.mContext, true, displayRotation == 0, stack.isInMiniFreeFormMode(), stack.mIsLandcapeFreeform, (Rect) null, stack.getStackPackageName(), true, statusBarHeight);
        stack.mStackControlInfo.mLastFreeFormWindowStartBounds = new Rect(lastBounds);
        stack.mFreeFormScale = MiuiMultiWindowUtils.getOriFreeformScale(this.mService.mContext, stack.mIsLandcapeFreeform);
        try {
            this.mService.mActivityManager.resizeTask(stack.mStackID, lastBounds, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        stack.mStackControlInfo.mScaleWindowHeight = lastBounds.height() * stack.mFreeFormScale;
        stack.mStackControlInfo.mScaleWindowWidth = lastBounds.width() * stack.mFreeFormScale;
        Rect corner = MiuiMultiWindowUtils.findNearestCorner(this.mService.mContext, (float) MiuiFreeformPinManagerService.EDGE_AREA, (float) MiuiFreeformPinManagerService.EDGE_AREA, stack.mCornerPosition, stack.mIsLandcapeFreeform);
        stack.mStackControlInfo.mSmallWindowBounds = new Rect(corner);
        float endX = corner.left;
        float endY2 = corner.top;
        float[] widthAndHeight = this.mFreeFormWindowMotionHelper.getSmallwindowWidthHeight(stack);
        stack.mStackControlInfo.mSmallWindowTargetWScale = widthAndHeight[0] / stack.mStackControlInfo.mScaleWindowWidth;
        stack.mStackControlInfo.mSmallWindowTargetHScale = widthAndHeight[1] / stack.mStackControlInfo.mScaleWindowHeight;
        if (!stack.isLaunchFlashBackFromBackGround()) {
            endY = endY2;
        } else {
            endY = endY2 - 1000.0f;
        }
        this.mGestureAnimator.setMatrixInTransaction(stack, stack.mStackControlInfo.mSmallWindowTargetWScale, MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA, stack.mStackControlInfo.mSmallWindowTargetHScale);
        this.mGestureAnimator.setPositionInTransaction(stack, endX, endY);
        this.mGestureAnimator.showStack(stack);
        this.mGestureAnimator.applyTransaction();
        hideScreenSurface(stack);
        stack.mIsLaunchingSmallFreeForm = false;
        startFreeformToSmallFreeFormWindow(1, stack);
    }

    public void setRequestedOrientation(int requestedOrientation, Task task, boolean noAnimation) {
        MiuiFreeFormActivityStack stack = (MiuiFreeFormActivityStack) this.mGestureController.mMiuiFreeFormManagerService.getMiuiFreeFormActivityStack(task.getRootTaskId());
        if (stack != null && stack.isInFreeFormMode()) {
            this.mFreeFormWindowMotionHelper.setRequestedOrientation(requestedOrientation, stack, noAnimation);
        }
    }

    public void startShowSmallFreeformToFreeFormWindow(int action, MiuiFreeFormActivityStack stack) {
        Slog.d(TAG, "startShowSmallFreeformToFreeFormWindow");
        stack.setIsFlashBackMode(false);
        stack.setStackFreeFormMode(0);
        this.mGestureController.notifyStartSmallFreeFormToFreeformWindow(action, stack);
        SurfaceControl.Transaction inputTransaction = new SurfaceControl.Transaction();
        synchronized (this.mService.mGlobalLock) {
            this.mGestureController.mDisplayContent.getInputMonitor().updateInputWindowsImmediately(inputTransaction);
        }
        inputTransaction.apply();
        this.mGestureAnimator.removeAnimationControlLeash(stack);
        this.mGestureController.mMiuiFreeFormFlashBackHelper.resetFlashBackWindowIfNeeded();
        this.mGestureController.mMiuiFreeFormFlashBackHelper.stopFlashBackService(this.mService.mContext, this.mHandler);
        synchronized (this.mService.mGlobalLock) {
            this.mService.mAtmService.setFocusedTask(stack.mTask.mTaskId);
            this.mService.updateFocusedWindowLocked(0, true);
        }
    }

    public void startShowFullScreenWindow(int action, MiuiFreeFormActivityStack stack) {
        Slog.d(TAG, "startShowFullScreenWindow");
        synchronized (this.mService.mGlobalLock) {
            Task currentFullRootTask = this.mService.mAtmService.mRootWindowContainer.getDefaultTaskDisplayArea().getTopRootTaskInWindowingMode(1);
            if (currentFullRootTask != null) {
                ArraySet<Task> tasks = Sets.newArraySet(new Task[]{currentFullRootTask});
                this.mService.mTaskSnapshotController.snapshotTasks(tasks);
                this.mService.mTaskSnapshotController.addSkipClosingAppSnapshotTasks(tasks);
            }
            SurfaceControl.Transaction inputTransaction = new SurfaceControl.Transaction();
            this.mGestureController.mDisplayContent.getInputMonitor().updateInputWindowsImmediately(inputTransaction);
            inputTransaction.apply();
        }
        this.mGestureController.mMiuiFreeFormFlashBackHelper.resetFlashBackWindowIfNeeded();
        this.mGestureAnimator.removeAnimationControlLeash(stack);
        this.mGestureController.mWindowController.startRemoveOverLayViewAnimation(1500);
        this.mGestureController.notifyFullScreenWidnowModeStart(action, stack);
    }

    public void resetStateBeforEnterSplit(int stackID) {
        try {
            MiuiFreeFormActivityStack stack = (MiuiFreeFormActivityStack) this.mGestureController.mMiuiFreeFormManagerService.getMiuiFreeFormActivityStack(stackID);
            if (stack == null) {
                return;
            }
            this.mGestureAnimator.removeAnimationControlLeash(stack);
            this.mFreeFormWindowMotionHelper.cancelAllSpringAnimal();
            stack.setStackFreeFormMode(-1);
            this.mGestureController.notifyFullScreenWidnowModeStart(3, stack, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startExitApplication(MiuiFreeFormActivityStack mffas) {
        Slog.d(TAG, "startExitApplication");
        mffas.mStartExitStack = true;
        this.mGestureAnimator.hideStack(mffas);
        this.mGestureAnimator.applyTransaction();
        mffas.mHadHideStack = true;
        SurfaceControl.Transaction inputTransaction = new SurfaceControl.Transaction();
        synchronized (this.mService.mGlobalLock) {
            this.mGestureController.mDisplayContent.getInputMonitor().updateInputWindowsImmediately(inputTransaction);
        }
        inputTransaction.apply();
        this.mGestureController.notifyExitFreeFormApplicationStart(mffas);
        this.mGestureAnimator.removeAnimationControlLeash(mffas);
    }

    public void startExitSmallFreeformApplication(MiuiFreeFormActivityStack mffas) {
        Slog.d(TAG, "startExitSmallFreeformApplication");
        mffas.mStartExitStack = true;
        this.mGestureAnimator.hideStack(mffas);
        this.mGestureAnimator.applyTransaction();
        mffas.mHadHideStack = true;
        SurfaceControl.Transaction inputTransaction = new SurfaceControl.Transaction();
        synchronized (this.mService.mGlobalLock) {
            this.mGestureController.mDisplayContent.getInputMonitor().updateInputWindowsImmediately(inputTransaction);
        }
        inputTransaction.apply();
        this.mGestureController.mMiuiFreeFormFlashBackHelper.resetFlashBackWindowIfNeeded();
        this.mGestureController.mMiuiFreeFormFlashBackHelper.stopFlashBackService(this.mService.mContext, this.mHandler);
        this.mGestureAnimator.removeAnimationControlLeash(mffas);
        this.mGestureController.notifyExitSmallFreeFormApplicationStart(mffas);
    }

    public void onActionDown(MsgObject msgObject) {
        int i;
        int currentWindowMode = msgObject.mffas != null ? msgObject.mffas.mMiuiFreeFromWindowMode : -1;
        if (MiuiFreeFormGestureController.DEBUG) {
            Slog.d(TAG, "onActionDown currentWindowMode " + currentWindowMode);
        }
        VelocityTracker velocityTracker = this.mVelocityTracker;
        if (velocityTracker == null) {
            this.mVelocityTracker = VelocityTracker.obtain();
        } else {
            velocityTracker.clear();
        }
        if (msgObject.motionEvent != null) {
            this.mVelocityTracker.addMovement(msgObject.motionEvent);
        }
        if (MiuiMultiWindowUtils.MULTI_WINDOW_SWITCH_ENABLED) {
            ActivityRecord topActivity = msgObject.mffas.mTask.getTopNonFinishingActivity();
            if (this.mGestureController.mMiuiMultiWindowSwitchManager.checkIfSplitAvailable(topActivity != null ? topActivity.token : null, false)) {
                i = MiuiMultiWindowUtils.LONG_WAIT;
            } else {
                i = MiuiMultiWindowUtils.LONG_WAIT_TOAST;
            }
            this.mWaitThreshold = i;
        }
        switch (currentWindowMode) {
            case 0:
                this.mFreeFormWindowMotionHelper.notifyDownLocked(msgObject.motionEvent, msgObject.mffas);
                return;
            case 1:
                this.mSmallFreeFormWindowMotionHelper.notifyDownLocked(msgObject.motionEvent, msgObject.mffas);
                return;
            default:
                return;
        }
    }

    public void onActionMove(MsgObject msgObject) {
        int currentWindowMode = msgObject.mffas != null ? msgObject.mffas.mMiuiFreeFromWindowMode : -1;
        if (this.mVelocityTracker != null && msgObject.motionEvent != null) {
            this.mVelocityTracker.addMovement(msgObject.motionEvent);
        }
        if (currentWindowMode == 1) {
            this.mSmallFreeFormWindowMotionHelper.notifyMoveLocked(msgObject.motionEvent, msgObject.mffas);
        } else if (currentWindowMode == 0) {
            if (MiuiMultiWindowUtils.MULTIWIN_SWITCH_ENABLED && this.mFreeFormWindowMotionHelper.mCurrentTouchedMode == 0) {
                if ((!Build.DEVICE.equals("cetus") || MiuiMultiWindowUtils.getScreenType(this.mService.mContext) != 0) && isEnterHotArea(msgObject.motionEvent)) {
                    if (!this.mIsEnterHotArea) {
                        this.mFirstEnterHotAreaTime = System.currentTimeMillis();
                        this.mIsEnterHotArea = true;
                    }
                    this.mVelocityTracker.computeCurrentVelocity(1000);
                    float xVelocity = this.mVelocityTracker.getXVelocity();
                    float yVelocity = this.mVelocityTracker.getYVelocity();
                    if (!this.mHasTriggerActioned) {
                        this.mHandler.removeMessages(2);
                        Handler handler = this.mHandler;
                        handler.sendMessageDelayed(handler.obtainMessage(2, msgObject), 500L);
                        ActivityRecord topActivity = msgObject.mffas.mTask.getTopNonFinishingActivity();
                        if (System.currentTimeMillis() - this.mFirstEnterHotAreaTime > this.mWaitThreshold && MiuiMultiWindowUtils.mergeXY(xVelocity, yVelocity) < MiuiMultiWindowUtils.SLOWER_SPEED) {
                            if (!this.mHasShowedToast && topActivity != null) {
                                this.mHandler.removeMessages(2);
                                trggerMultiWindowSwitch(msgObject);
                                return;
                            }
                            this.mHasShowedToast = true;
                            this.mFreeFormWindowMotionHelper.notifyMoveLocked(msgObject.motionEvent, msgObject.mffas);
                            return;
                        }
                        this.mFreeFormWindowMotionHelper.notifyMoveLocked(msgObject.motionEvent, msgObject.mffas);
                        return;
                    }
                    return;
                }
                if (this.mIsEnterHotArea) {
                    this.mFirstEnterHotAreaTime = System.currentTimeMillis();
                    this.mHandler.removeMessages(2);
                    this.mIsEnterHotArea = false;
                }
                if (!this.mHasTriggerActioned) {
                    this.mHasShowedToast = false;
                    this.mFreeFormWindowMotionHelper.notifyMoveLocked(msgObject.motionEvent, msgObject.mffas);
                    return;
                }
                return;
            }
            this.mFreeFormWindowMotionHelper.notifyMoveLocked(msgObject.motionEvent, msgObject.mffas);
        }
    }

    public void trggerMultiWindowSwitch(MsgObject msgObject) {
        final ActivityRecord topActivity = msgObject.mffas.mTask.getTopNonFinishingActivity();
        if (this.mHasShowedToast || topActivity == null || this.mHasTriggerActioned) {
            return;
        }
        if (!this.mGestureController.mMiuiMultiWindowSwitchManager.checkIfSplitAvailable(topActivity.token)) {
            this.mHasShowedToast = true;
            return;
        }
        Slog.d(TAG, "trggerMultiWindowSwitch");
        this.mHasTriggerActioned = true;
        this.mHasShowedToast = true;
        try {
            this.mGestureController.mWindowController.hideHotSpotView();
        } catch (Exception e) {
            Slog.d(TAG, "hideHotSpotView fail", e);
        }
        try {
            final MotionEvent up = MotionEvent.obtain(msgObject.motionEvent);
            up.setAction(1);
            synchronized (msgObject.mffas.mTask.mWmService.mGlobalLock) {
                msgObject.mffas.mTask.forAllWindows(new Consumer() { // from class: com.android.server.wm.MiuiFreeFormGesturePointerEventListener$$ExternalSyntheticLambda1
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        ((WindowState) obj).dispatchEnterDragArea(up);
                    }
                }, true);
            }
            up.recycle();
        } catch (Exception e2) {
            e2.printStackTrace();
        }
        final MotionEvent motionEvent = MotionEvent.obtain(msgObject.motionEvent);
        this.mFreeFormWindowMotionHelper.mShowedHotSpotView = false;
        this.mService.mAtmService.mUiHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreeFormGesturePointerEventListener.3
            @Override // java.lang.Runnable
            public void run() {
                MiuiFreeFormGesturePointerEventListener.this.mGestureController.mMiuiMultiWindowSwitchManager.addHotArea(topActivity.token, MiuiFreeFormGesturePointerEventListener.this.prepareToHandleMultiWindowSwitch(motionEvent));
            }
        });
    }

    public void onActionUp(MsgObject msgObject) {
        int currentWindowMode = msgObject.mffas != null ? msgObject.mffas.mMiuiFreeFromWindowMode : -1;
        this.mHandler.removeMessages(2);
        if (MiuiFreeFormGestureController.DEBUG) {
            Slog.d(TAG, "onActionUp currentWindowMode " + currentWindowMode);
        }
        VelocityTracker velocityTracker = this.mVelocityTracker;
        if (velocityTracker != null) {
            velocityTracker.recycle();
            this.mVelocityTracker = null;
        }
        if (currentWindowMode == 1) {
            this.mSmallFreeFormWindowMotionHelper.notifyUpLocked(msgObject.motionEvent, msgObject.mffas);
        } else if (currentWindowMode == 0) {
            if (this.mHasTriggerActioned && this.mFreeFormWindowMotionHelper.mStackLocks.get(msgObject.mffas) != null) {
                this.mFreeFormWindowMotionHelper.mStackLocks.get(msgObject.mffas).cancel();
                this.mFreeFormWindowMotionHelper.mStackLocks.remove(msgObject.mffas);
                this.mGestureAnimator.removeAnimationControlLeash(msgObject.mffas);
                return;
            }
            this.mFreeFormWindowMotionHelper.notifyUpLocked(msgObject.motionEvent, msgObject.mffas);
        }
    }

    public void showScreenSurface(MiuiFreeFormActivityStack stack) {
        this.mFreeFormWindowMotionHelper.showScreenSurface(stack);
    }

    public void hideScreenSurface(MiuiFreeFormActivityStack stack) {
        this.mFreeFormWindowMotionHelper.hideScreenSurface(stack);
    }

    public MiuiFreeFormActivityStack synchronizeFreeFormStackInfo() {
        MiuiFreeFormActivityStack topActivityStack = this.mGestureController.mMiuiFreeFormManagerService.getTopFreeFormActivityStack();
        if (topActivityStack != null && MiuiFreeFormGestureController.DEBUG) {
            Slog.d(TAG, "DEBUG_CONTROL Current Control AS = : " + topActivityStack.getStackPackageName());
        }
        this.mGestureController.mMiuiFreeFormFlashBackHelper.initVisibleFlashBackWindow();
        return topActivityStack;
    }

    public MiuiFreeFormActivityStack synchronizeControlInfoForeMoveEvent(MotionEvent event) {
        MiuiFreeFormActivityStack currentControlActivityStack = this.mFreeFormWindowMotionHelper.findControlFreeFormActivityStack(event.getX(), event.getY());
        if (currentControlActivityStack != null) {
            if (MiuiFreeFormGestureController.DEBUG) {
                Slog.d(TAG, "DEBUG_CONTROL Current Control AS = : " + currentControlActivityStack.getStackPackageName());
            }
        } else if (MiuiFreeFormGestureController.DEBUG) {
            Slog.d(TAG, "DEBUG_CONTROL Current Control AS = NULL ");
        }
        this.mGestureController.mMiuiFreeFormFlashBackHelper.initVisibleFlashBackWindow();
        return currentControlActivityStack;
    }

    public void hideInputMethodWindowIfNeeded() {
        DisplayContent displayContent = this.mGestureController.mDisplayContent;
        if (displayContent.mInputMethodWindow != null && displayContent.mInputMethodWindow.mWinAnimator != null && displayContent.mInputMethodWindow.mWinAnimator.getShown()) {
            if (this.mInputMethodManagerInternal == null) {
                this.mInputMethodManagerInternal = (InputMethodManagerInternal) LocalServices.getService(InputMethodManagerInternal.class);
            }
            if (this.mInputMethodManagerInternal != null) {
                Slog.d(TAG, "hideInputMethodWindowIfNeeded");
                this.mInputMethodManagerInternal.hideCurrentInputMethod(3);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class H extends Handler {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        private H(Looper looper) {
            super(looper);
            MiuiFreeFormGesturePointerEventListener.this = r1;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            try {
                try {
                    try {
                        switch (msg.what) {
                            case 2:
                                MiuiFreeFormGesturePointerEventListener.this.trggerMultiWindowSwitch((MsgObject) msg.obj);
                                break;
                            case 3:
                                MotionEvent event = (MotionEvent) msg.obj;
                                processEvent(event);
                                break;
                        }
                        if (!(msg.obj instanceof MotionEvent) || msg.obj == null) {
                            return;
                        }
                        ((MotionEvent) msg.obj).recycle();
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (!(msg.obj instanceof MotionEvent) || msg.obj == null) {
                            return;
                        }
                        ((MotionEvent) msg.obj).recycle();
                    }
                } catch (Throwable th) {
                    if ((msg.obj instanceof MotionEvent) && msg.obj != null) {
                        try {
                            ((MotionEvent) msg.obj).recycle();
                        } catch (Exception e2) {
                        }
                    }
                    throw th;
                }
            } catch (Exception e3) {
            }
        }

        private void processEvent(MotionEvent event) {
            if (MiuiFreeFormGesturePointerEventListener.this.mGestureController.mMiuiFreeFormManagerService.getCurrentMiuiFreeFormNum() >= 1 && event.isTouchEvent()) {
                if (MiuiMultiWindowUtils.isUserAMonkey()) {
                    Slog.d(MiuiFreeFormGesturePointerEventListener.TAG, "MTBF Or Monkey is Running");
                } else if ((event.getFlags() & 134217728) == 134217728) {
                    if (event.getActionMasked() == 0) {
                        Slog.d(MiuiFreeFormGesturePointerEventListener.TAG, "Because handwriting, drop this event");
                    }
                } else if (event.getActionMasked() == 5) {
                    Slog.w(MiuiFreeFormGesturePointerEventListener.TAG, "will ignore multi-touch event");
                    MiuiFreeFormGesturePointerEventListener.this.mLastMotionEvent = MotionEvent.obtain(event);
                    MiuiFreeFormGesturePointerEventListener.this.mInMultiTouch = true;
                } else {
                    if (event.getActionMasked() == 0) {
                        if (!MiuiFreeFormGesturePointerEventListener.this.mGestureController.mWindowController.inTipViewBounds(event)) {
                            MiuiFreeFormGesturePointerEventListener.this.mGestureController.mWindowController.removeOpenCloseTipWindow();
                        }
                        MiuiFreeFormGesturePointerEventListener.this.mHasTriggerActioned = false;
                        MiuiFreeFormGesturePointerEventListener.this.mHasShowedToast = false;
                        MiuiFreeFormGesturePointerEventListener.this.mInMultiTouch = false;
                        MiuiFreeFormGesturePointerEventListener.this.mLastMotionEvent = null;
                        MiuiFreeFormGesturePointerEventListener miuiFreeFormGesturePointerEventListener = MiuiFreeFormGesturePointerEventListener.this;
                        miuiFreeFormGesturePointerEventListener.mCurrentControlActivityStack = miuiFreeFormGesturePointerEventListener.synchronizeControlInfoForeMoveEvent(event);
                        if (MiuiFreeFormGesturePointerEventListener.this.mCurrentControlActivityStack != null && !MiuiFreeFormGesturePointerEventListener.this.mCurrentControlActivityStack.checkReadyForFreeFormControl()) {
                            return;
                        }
                    }
                    if (MiuiFreeFormGesturePointerEventListener.this.mCurrentControlActivityStack == null) {
                        if (MiuiFreeFormGestureController.DEBUG) {
                            Slog.d(MiuiFreeFormGesturePointerEventListener.TAG, "Do not have freeform stacks");
                            return;
                        }
                        return;
                    }
                    int action = event.getActionMasked();
                    if (MiuiFreeFormGesturePointerEventListener.this.mCurrentControlActivityStack.mMiuiFreeFromWindowMode == 1) {
                        if (action == 0) {
                            MiuiFreeFormGesturePointerEventListener miuiFreeFormGesturePointerEventListener2 = MiuiFreeFormGesturePointerEventListener.this;
                            miuiFreeFormGesturePointerEventListener2.mInExcludeRegion = miuiFreeFormGesturePointerEventListener2.isInExcludeRegion(event.getX(), event.getY());
                        }
                        if (action != 2 && !MiuiFreeFormGesturePointerEventListener.this.mInExcludeRegion) {
                            boolean result = MiuiFreeFormGesturePointerEventListener.this.mTouchGestureDetector.onTouchEvent(event);
                            Slog.d(MiuiFreeFormGesturePointerEventListener.TAG, "touchGestureDetector result:" + result);
                        }
                    }
                    MsgObject msgObject = new MsgObject();
                    msgObject.mffas = MiuiFreeFormGesturePointerEventListener.this.mCurrentControlActivityStack;
                    msgObject.motionEvent = event;
                    switch (action) {
                        case 0:
                            MiuiFreeFormGesturePointerEventListener.this.onActionDown(msgObject);
                            MiuiFreeFormGesturePointerEventListener.this.mHasDownMotionEvent = true;
                            return;
                        case 1:
                        case 3:
                            if (MiuiFreeFormGesturePointerEventListener.this.mInMultiTouch && MiuiFreeFormGesturePointerEventListener.this.mLastMotionEvent != null) {
                                msgObject.motionEvent = MiuiFreeFormGesturePointerEventListener.this.mLastMotionEvent;
                                msgObject.motionEvent.setAction(1);
                            }
                            if (MiuiFreeFormGesturePointerEventListener.this.mHasDownMotionEvent) {
                                MiuiFreeFormGesturePointerEventListener.this.mHasDownMotionEvent = false;
                                MiuiFreeFormGesturePointerEventListener.this.onActionUp(msgObject);
                            }
                            if (MiuiFreeFormGesturePointerEventListener.this.mInMultiTouch && MiuiFreeFormGesturePointerEventListener.this.mLastMotionEvent != null) {
                                MiuiFreeFormGesturePointerEventListener.this.mLastMotionEvent.recycle();
                                MiuiFreeFormGesturePointerEventListener.this.mInMultiTouch = false;
                                MiuiFreeFormGesturePointerEventListener.this.mLastMotionEvent = null;
                                return;
                            }
                            return;
                        case 2:
                            if (!MiuiFreeFormGesturePointerEventListener.this.mInMultiTouch && MiuiFreeFormGesturePointerEventListener.this.mHasDownMotionEvent) {
                                MiuiFreeFormGesturePointerEventListener.this.onActionMove(msgObject);
                                return;
                            }
                            return;
                        default:
                            return;
                    }
                }
            }
        }
    }

    public Bundle prepareToHandleMultiWindowSwitch(MotionEvent motionEvent) {
        Bitmap dragBarBitmap;
        Bundle bundle = new Bundle();
        bundle.putParcelable("dragTouchPoint", new Point((int) motionEvent.getX(), (int) motionEvent.getY()));
        int[] touchOffsets = new int[2];
        int touchX = (int) motionEvent.getX();
        int touchY = (int) motionEvent.getY();
        getTouchOffsets(touchOffsets, touchX, touchY);
        int touchOffsetX = touchOffsets[0];
        int touchOffsetY = touchOffsets[1];
        bundle.putParcelable("dragTouchOffsets", new Point(touchOffsetX, touchOffsetY));
        if (MiuiMultiWindowUtils.isPadScreen(this.mService.mContext)) {
            dragBarBitmap = MiuiMultiWindowUtils.drawableToBitmap(this.mService.mContext.getResources().getDrawable(285737082, null));
        } else {
            dragBarBitmap = MiuiMultiWindowUtils.drawableToBitmap(this.mService.mContext.getResources().getDrawable(285737081, null));
        }
        bundle.putParcelable("dragBarBitmap", dragBarBitmap);
        return bundle;
    }

    private void getTouchOffsets(int[] outTouchOffsets, float touchX, float touchY) {
        outTouchOffsets[0] = 0;
        outTouchOffsets[1] = 0;
    }

    private boolean isUpDownSplit() {
        Bundle bundle = this.mGestureController.mMiuiMultiWindowSwitchManager.getSplitRootTasksPos(this.mGestureController.mDisplayContent.getDisplayId());
        if (bundle == null) {
            Slog.i(TAG, "MiuiSplitStacksPos bundle is null");
            return true;
        }
        int primaryScreenType = bundle.getInt(MiuiMultiWindowManager.MIUI_SPLIT_SCREEN_PRIMARY_POSITION);
        return primaryScreenType == 0;
    }

    private boolean isEnterHotArea(MotionEvent event) {
        int displayWidth = this.mGestureController.mDisplayContent.mDisplayInfo.logicalWidth;
        int displayHeight = this.mGestureController.mDisplayContent.mDisplayInfo.logicalHeight;
        if (isUpDownSplit()) {
            RectF topHotArea = new RectF();
            RectF bottomHotArea = new RectF();
            if (displayWidth > displayHeight) {
                topHotArea.set(MiuiMultiWindowUtils.FREEFORM_HOTSPOT_TRIGGER_LANDCAPE_RADIUS, MiuiFreeformPinManagerService.EDGE_AREA, displayWidth - MiuiMultiWindowUtils.FREEFORM_HOTSPOT_TRIGGER_LANDCAPE_RADIUS, displayHeight / 10);
                bottomHotArea.set(MiuiMultiWindowUtils.FREEFORM_HOTSPOT_TRIGGER_LANDCAPE_RADIUS, displayHeight - ((displayHeight / 10) * 2), displayWidth - MiuiMultiWindowUtils.FREEFORM_HOTSPOT_TRIGGER_LANDCAPE_RADIUS, displayHeight);
            } else {
                topHotArea.set(MiuiMultiWindowUtils.FREEFORM_HOTSPOT_TRIGGER_PORTRAIT_RADIUS, MiuiFreeformPinManagerService.EDGE_AREA, displayWidth - MiuiMultiWindowUtils.FREEFORM_HOTSPOT_TRIGGER_PORTRAIT_RADIUS, displayHeight / 10);
                bottomHotArea.set(MiuiMultiWindowUtils.FREEFORM_HOTSPOT_TRIGGER_PORTRAIT_RADIUS, displayHeight - ((displayHeight / 10) * 2), displayWidth - MiuiMultiWindowUtils.FREEFORM_HOTSPOT_TRIGGER_PORTRAIT_RADIUS, displayHeight);
            }
            return topHotArea.contains(event.getRawX(), event.getRawY()) || bottomHotArea.contains(event.getRawX(), event.getRawY());
        }
        RectF leftHotArea = new RectF();
        RectF rightHotArea = new RectF();
        if (MiuiMultiWindowUtils.getScreenType(this.mService.mContext) == 2) {
            if (displayWidth > displayHeight) {
                leftHotArea.set(MiuiFreeformPinManagerService.EDGE_AREA, MiuiMultiWindowUtils.FREEFORM_HOTSPOT_TRIGGER_LANDCAPE_RADIUS, displayWidth / 10, displayHeight - MiuiMultiWindowUtils.FREEFORM_HOTSPOT_TRIGGER_LANDCAPE_RADIUS);
                rightHotArea.set(displayWidth - (displayWidth / 10), MiuiMultiWindowUtils.FREEFORM_HOTSPOT_TRIGGER_LANDCAPE_RADIUS, displayWidth, displayHeight - MiuiMultiWindowUtils.FREEFORM_HOTSPOT_TRIGGER_LANDCAPE_RADIUS);
            } else {
                leftHotArea.set(MiuiFreeformPinManagerService.EDGE_AREA, MiuiMultiWindowUtils.FREEFORM_HOTSPOT_TRIGGER_PORTRAIT_RADIUS, displayWidth / 10, displayHeight - MiuiMultiWindowUtils.FREEFORM_HOTSPOT_TRIGGER_PORTRAIT_RADIUS);
                rightHotArea.set(displayWidth - (displayWidth / 10), MiuiMultiWindowUtils.FREEFORM_HOTSPOT_TRIGGER_PORTRAIT_RADIUS, displayWidth, displayHeight - MiuiMultiWindowUtils.FREEFORM_HOTSPOT_TRIGGER_PORTRAIT_RADIUS);
            }
        } else if (MiuiMultiWindowUtils.getScreenType(this.mService.mContext) == 3) {
            if (displayWidth > displayHeight) {
                leftHotArea.set(MiuiFreeformPinManagerService.EDGE_AREA, 80.0f, displayWidth / 10, displayHeight - MiuiMultiWindowUtils.FREEFORM_HOTSPOT_TRIGGER_LANDCAPE_RADIUS);
                rightHotArea.set(displayWidth - (displayWidth / 10), 80.0f, displayWidth, displayHeight - MiuiMultiWindowUtils.FREEFORM_HOTSPOT_TRIGGER_LANDCAPE_RADIUS);
            } else {
                leftHotArea.set(MiuiFreeformPinManagerService.EDGE_AREA, 80.0f, displayWidth / 10, displayHeight - MiuiMultiWindowUtils.FREEFORM_HOTSPOT_TRIGGER_PORTRAIT_RADIUS);
                rightHotArea.set(displayWidth - (displayWidth / 10), 80.0f, displayWidth, displayHeight - MiuiMultiWindowUtils.FREEFORM_HOTSPOT_TRIGGER_PORTRAIT_RADIUS);
            }
        } else if (displayWidth > displayHeight) {
            leftHotArea.set(MiuiFreeformPinManagerService.EDGE_AREA, MiuiMultiWindowUtils.FREEFORM_HOTSPOT_TRIGGER_LANDCAPE_RADIUS, displayWidth / 10, displayHeight - MiuiMultiWindowUtils.FREEFORM_HOTSPOT_TRIGGER_LANDCAPE_RADIUS);
            rightHotArea.set(displayWidth - (displayWidth / 10), MiuiMultiWindowUtils.FREEFORM_HOTSPOT_TRIGGER_LANDCAPE_RADIUS, displayWidth, displayHeight - MiuiMultiWindowUtils.FREEFORM_HOTSPOT_TRIGGER_LANDCAPE_RADIUS);
        } else {
            leftHotArea.set(MiuiFreeformPinManagerService.EDGE_AREA, MiuiMultiWindowUtils.FREEFORM_HOTSPOT_TRIGGER_PORTRAIT_RADIUS, displayWidth / 10, displayHeight - MiuiMultiWindowUtils.FREEFORM_HOTSPOT_TRIGGER_PORTRAIT_RADIUS);
            rightHotArea.set(displayWidth - (displayWidth / 10), MiuiMultiWindowUtils.FREEFORM_HOTSPOT_TRIGGER_PORTRAIT_RADIUS, displayWidth, displayHeight - MiuiMultiWindowUtils.FREEFORM_HOTSPOT_TRIGGER_PORTRAIT_RADIUS);
        }
        return leftHotArea.contains(event.getRawX(), event.getRawY()) || rightHotArea.contains(event.getRawX(), event.getRawY());
    }

    public void updateScreenParams(DisplayContent displayContent, Configuration configuration) {
        this.mLongSide = Math.max(displayContent.mBaseDisplayHeight, displayContent.mBaseDisplayWidth);
        this.shortSide = Math.min(displayContent.mBaseDisplayHeight, displayContent.mBaseDisplayWidth);
        if (this.mGestureController.mWindowController != null) {
            this.mGestureController.mWindowController.updateScreenParams(displayContent, configuration);
        }
        int ori = configuration.orientation;
        this.mIsPortrait = ori == 1;
        this.mFreeFormAccessibleArea.set(MiuiMultiWindowUtils.getFreeFormAccessibleArea(this.mService.mContext, false));
        if (this.mIsPortrait) {
            this.mScreenHeight = this.mLongSide;
            this.mScreenWidth = this.shortSide;
        } else {
            this.mScreenHeight = this.shortSide;
            this.mScreenWidth = this.mLongSide;
        }
        this.mNotchBar = MiuiFreeFormGestureDetector.getDisplayCutoutHeight(displayContent.mDisplayFrames);
        this.mDisplayRotation = this.mGestureController.mDisplayContent.getRotation();
        Slog.d(TAG, "updateScreenParams ori:" + ori + " mNotchBar:" + this.mNotchBar + " mDisplayRotation:" + this.mDisplayRotation);
        List<MiuiFreeFormActivityStack> stackList = this.mGestureController.mMiuiFreeFormManagerService.getAllMiuiFreeFormActivityStack();
        for (MiuiFreeFormActivityStack stack : stackList) {
            if (stack != null) {
                boolean isLandscapeFreeform = stack.mTask.getBounds().width() > stack.mTask.getBounds().height();
                stack.mWidthHeightScale = getWidthHeightScale(isLandscapeFreeform, this.mIsPortrait, stack.getStackPackageName());
                int windowWidth = (int) (stack.mTask.getBounds().width() * stack.mFreeFormScale);
                int windowHeight = (int) (stack.mTask.getBounds().height() * stack.mFreeFormScale);
                stack.mMaxMinWidthSize = getMaxMinWidthSize(isLandscapeFreeform, this.mIsPortrait, stack.getStackPackageName());
                float[] widthAndHeight = this.mFreeFormWindowMotionHelper.getSmallwindowWidthHeight(stack);
                stack.mStackControlInfo.mSmallWindowTargetWScale = widthAndHeight[0] / windowWidth;
                stack.mStackControlInfo.mSmallWindowTargetHScale = widthAndHeight[1] / windowHeight;
                if (stack.isInMiniFreeFormMode()) {
                    stack.mStackControlInfo.mSmallWindowTargetWScale = widthAndHeight[0] / windowWidth;
                    stack.mStackControlInfo.mSmallWindowTargetHScale = widthAndHeight[1] / windowHeight;
                    stack.mStackControlInfo.mLastFreeFormWindowStartBounds = new Rect(stack.mTask.getBounds());
                    this.mFreeFormWindowMotionHelper.startSmallWindowTranslateAnimal(this.mLastOrientation, this.mDisplayRotation, stack);
                }
            }
        }
        try {
            if (!stackList.isEmpty()) {
                this.mGestureController.mWindowController.removeOverlayView();
                this.mGestureController.mWindowController.addOverlayView();
                MiuiFreeFormActivityStack topAS = synchronizeFreeFormStackInfo();
                if (this.mIsPortrait && topAS != null && topAS.mIsLandcapeFreeform) {
                    MiuiFreeFormWindowController.DropWindowType = 0;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.mLastOrientation = ori;
    }

    private float getWidthHeightScale(boolean isLandcapeFreeform, boolean isPortrait, String packageName) {
        return MiuiMultiWindowUtils.getAspectRatio(isPortrait, isLandcapeFreeform, this.mService.mContext, packageName);
    }

    public int[] getMaxMinWidthSize(boolean isLandcapeFreeform, boolean isPortrait, String packageName) {
        float widthAfterScale = MiuiMultiWindowUtils.getPossibleBounds(this.mService.mContext, isPortrait, isLandcapeFreeform, packageName).width() * MiuiMultiWindowUtils.getOriFreeformScale(this.mService.mContext, isLandcapeFreeform);
        int[] maxMinWidthSize = {(int) (MiuiMultiWindowUtils.getScalingMaxValue(this.mService.mContext, isPortrait, isLandcapeFreeform) * widthAfterScale), (int) (MiuiMultiWindowUtils.getScalingMinValue(isPortrait, isLandcapeFreeform) * widthAfterScale)};
        return maxMinWidthSize;
    }

    public void reflectHandleSnapshotTaskByFreeform(Task task) {
        try {
            Class clazz = this.mService.mTaskSnapshotController.getClass();
            Method method = clazz.getDeclaredMethod("handleSnapshotTaskByFreeform", Task.class);
            method.setAccessible(true);
            method.invoke(this.mService.mTaskSnapshotController, task);
        } catch (Exception e) {
            Slog.d(TAG, "getDeclaredMethod:handleSnapshotTaskByFreeform" + e.toString());
        }
    }

    public void finishDrawingWindow(WindowState windowstate) {
        synchronized (this.mLaunchFreeformFromControlCenterLock) {
            WindowState topFullScreenWindow = this.mFreeFormWindowMotionHelper.getTopWindow(1);
            if (topFullScreenWindow != null && topFullScreenWindow == windowstate) {
                this.mLaunchFreeformFromControlCenterLock.notifyAll();
                Slog.d(TAG, "finishDrawingWindow: topFullScreenWindow= " + topFullScreenWindow);
            }
        }
    }
}
