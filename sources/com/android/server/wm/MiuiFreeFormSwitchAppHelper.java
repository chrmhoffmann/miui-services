package com.android.server.wm;

import android.app.ActivityTaskManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.MiuiMultiWindowAdapter;
import android.util.MiuiMultiWindowUtils;
import android.util.Slog;
import android.util.TypedValue;
import android.view.WindowManager;
import java.util.List;
import java.util.function.Predicate;
/* loaded from: classes.dex */
public class MiuiFreeFormSwitchAppHelper {
    private static final String TAG = "MiuiFreeFormSwitchAppHelper";
    private Task mCurrentFreeformRootTask;
    private Task mCurrentFullRootTask;
    private MiuiFreeFormGestureController mGestureController;
    InputManagerCallback mInputManagerCallback;
    private volatile boolean mIsMaskDisplayed;
    private boolean mMarkAccelerRotation;
    private MiuiFreeFormExchangeOverlay mSwitchOverlay;
    private boolean mSwitchingApp = false;
    private boolean mEnableSwitchApp = true;
    private boolean mCanSwitchApp = false;
    private Object mSwitchOverlayLock = new Object();
    private boolean mHasThawInputDispatch = false;
    private boolean mContainsRotation = false;
    private final float FREEFORM_EXCHANGE_VERTICAL_HEIGHT = TypedValue.applyDimension(1, 437.45f, Resources.getSystem().getDisplayMetrics());
    private final float FREEFORM_EXCHANGE_VERTICAL_WIDTH = TypedValue.applyDimension(1, 141.09f, Resources.getSystem().getDisplayMetrics());
    private final float FREEFORM_EXCHANGE_HORIZONTAL_HEIGHT = TypedValue.applyDimension(1, 289.45f, Resources.getSystem().getDisplayMetrics());
    private final float FREEFORM_EXCHANGE_HORIZONTAL_WIDTH = TypedValue.applyDimension(1, 455.27f, Resources.getSystem().getDisplayMetrics());
    private final float FREEFORM_EXCHANGE_HORIZONTAL_GAP_PRECENT = 0.2857143f;
    private Runnable mThawInputRunnable = new Runnable() { // from class: com.android.server.wm.MiuiFreeFormSwitchAppHelper.1
        @Override // java.lang.Runnable
        public void run() {
            if (!MiuiFreeFormSwitchAppHelper.this.mHasThawInputDispatch && MiuiFreeFormSwitchAppHelper.this.mInputManagerCallback != null) {
                MiuiFreeFormSwitchAppHelper.this.mInputManagerCallback.thawInputDispatchingLw();
                MiuiFreeFormSwitchAppHelper.this.mInputManagerCallback.setEventDispatchingLw(true);
                MiuiFreeFormSwitchAppHelper.this.mHasThawInputDispatch = true;
            }
        }
    };
    private final Runnable mShowSwitchOverlay = new Runnable() { // from class: com.android.server.wm.MiuiFreeFormSwitchAppHelper.2
        @Override // java.lang.Runnable
        public void run() {
            synchronized (MiuiFreeFormSwitchAppHelper.this.mSwitchOverlayLock) {
                if (MiuiFreeFormSwitchAppHelper.this.mSwitchOverlay != null) {
                    MiuiFreeFormSwitchAppHelper.this.mSwitchOverlay.destroyOverlay(false);
                }
                MiuiFreeFormSwitchAppHelper.this.mSwitchOverlay = new MiuiFreeFormExchangeOverlay(MiuiFreeFormSwitchAppHelper.this.mGestureController);
                MiuiFreeFormSwitchAppHelper.this.mSwitchOverlay.show(true);
                MiuiFreeFormSwitchAppHelper.this.mGestureController.mWindowController.addSwitchOverlayView();
                MiuiFreeFormSwitchAppHelper.this.mIsMaskDisplayed = true;
            }
        }
    };

    public MiuiFreeFormSwitchAppHelper(MiuiFreeFormGestureController gestureController) {
        this.mGestureController = gestureController;
        this.mInputManagerCallback = new InputManagerCallback(gestureController.mService);
    }

    public void init(Context context) {
        String packageName;
        String packageName2;
        boolean z = true;
        boolean z2 = !MiuiMultiWindowUtils.isPadScreen(context);
        this.mEnableSwitchApp = z2;
        if (z2) {
            synchronized (this.mSwitchOverlayLock) {
                MiuiFreeFormExchangeOverlay miuiFreeFormExchangeOverlay = this.mSwitchOverlay;
                if (miuiFreeFormExchangeOverlay != null) {
                    miuiFreeFormExchangeOverlay.destroy(false);
                }
                this.mIsMaskDisplayed = false;
            }
            if (this.mGestureController.mService.mAtmService.isInSplitScreenWindowingMode()) {
                Slog.d(TAG, " isSplitScreenModeActivated ");
                this.mCanSwitchApp = false;
                return;
            }
            synchronized (this.mGestureController.mGestureListener.mService.mGlobalLock) {
                this.mCurrentFullRootTask = this.mGestureController.mService.mAtmService.mRootWindowContainer.getDefaultTaskDisplayArea().getTopRootTaskInWindowingMode(1);
            }
            MiuiFreeFormActivityStack topActivityStack = this.mGestureController.mMiuiFreeFormManagerService.getTopFreeFormActivityStack();
            this.mCurrentFreeformRootTask = topActivityStack == null ? null : topActivityStack.mTask;
            Task task = this.mCurrentFullRootTask;
            if (task != null) {
                if (task.isActivityTypeHome()) {
                    Slog.d(TAG, "fullScreen app is home");
                    this.mCanSwitchApp = false;
                    return;
                } else if (this.mCurrentFullRootTask.intent != null && this.mCurrentFullRootTask.intent.getComponent() != null && MiuiMultiWindowAdapter.START_FROM_FREEFORM_BLACK_LIST_ACTIVITY.contains(this.mCurrentFullRootTask.intent.getComponent().flattenToShortString())) {
                    Slog.d(TAG, "fullScreen app is START_FROM_FREEFORM_BLACK_LIST_ACTIVITY");
                    this.mCanSwitchApp = false;
                    return;
                } else if (this.mGestureController.mMiuiFreeFormManagerService != null && (packageName2 = this.mGestureController.mMiuiFreeFormManagerService.getStackPackageName(this.mCurrentFullRootTask)) != null && packageName2.contains(MiuiFreeFormGestureDetector.COM_TENCENT_TMGP_PUBGMHD_NAME)) {
                    Slog.d(TAG, "fullScreen app is com.epicgames.ue4.SplashActivity");
                    this.mCanSwitchApp = false;
                    return;
                } else {
                    Object isPairRootTask = MiuiMultiWindowUtils.invoke(ActivityTaskManager.getService(), "isPairRootTask", new Object[]{Integer.valueOf(this.mCurrentFullRootTask.mTaskId)});
                    if (isPairRootTask != null && ((Boolean) isPairRootTask).booleanValue()) {
                        Slog.d(TAG, "fullScreen app isPairRootTask");
                        this.mCanSwitchApp = false;
                        return;
                    } else if (this.mCurrentFreeformRootTask == null) {
                        this.mCanSwitchApp = false;
                        return;
                    } else if (this.mGestureController.mMiuiFreeFormManagerService != null && (packageName = this.mGestureController.mMiuiFreeFormManagerService.getStackPackageName(this.mCurrentFullRootTask)) != null && packageName.contains(MiuiFreeFormGestureDetector.COM_TENCENT_TMGP_PUBGMHD_NAME)) {
                        Slog.d(TAG, "freeform app is com.epicgames.ue4.SplashActivity");
                        this.mCanSwitchApp = false;
                        return;
                    } else {
                        List<String> blackList = MiuiMultiWindowAdapter.getFreeformBlackList();
                        boolean isResizeable = this.mCurrentFullRootTask.isResizeable();
                        if ((blackList != null && blackList.contains(getStackPackageName(this.mCurrentFullRootTask))) || !MiuiMultiWindowUtils.pkgHasIcon(this.mGestureController.mService.mContext, getStackPackageName(this.mCurrentFullRootTask)) || (!isResizeable && !MiuiMultiWindowAdapter.FREEFORM_RESIZEABLE_WHITE_LIST.contains(getStackPackageName(this.mCurrentFullRootTask)))) {
                            Slog.d(TAG, "fullScreen app is in black list or unResizeable,packageName:" + getStackPackageName(this.mCurrentFullRootTask));
                            this.mCanSwitchApp = false;
                            return;
                        } else if (this.mCurrentFullRootTask.getActivity(new Predicate() { // from class: com.android.server.wm.MiuiFreeFormSwitchAppHelper$$ExternalSyntheticLambda0
                            @Override // java.util.function.Predicate
                            public final boolean test(Object obj) {
                                boolean isAnimating;
                                isAnimating = ((ActivityRecord) obj).isAnimating(3);
                                return isAnimating;
                            }
                        }) != null) {
                            Slog.d(TAG, "fullScreen app is in animating, packageName:" + getStackPackageName(this.mCurrentFullRootTask));
                            this.mCanSwitchApp = false;
                            return;
                        } else {
                            this.mCanSwitchApp = true;
                            if (this.mCurrentFreeformRootTask != null) {
                                boolean isLandscapeFullScreen = !MiuiMultiWindowUtils.isVerical(context);
                                boolean isLandscapeFreeForm = topActivityStack.isLandcapeFreeform();
                                if (!isLandscapeFreeForm && !isLandscapeFullScreen) {
                                    z = false;
                                }
                                this.mContainsRotation = z;
                                return;
                            }
                            return;
                        }
                    }
                }
            }
            this.mCanSwitchApp = false;
        }
    }

    public boolean isSwitchingApp(Task task) {
        if (task == this.mCurrentFreeformRootTask || task == this.mCurrentFullRootTask) {
            return isSwitchingApp();
        }
        return false;
    }

    public boolean isSwitchingApp() {
        return this.mSwitchingApp;
    }

    public boolean canSwitchApp() {
        return this.mCanSwitchApp;
    }

    public boolean startSwitchAppOneStep() {
        if (this.mCurrentFreeformRootTask != null) {
            MiuiFreeFormActivityStack ffas = (MiuiFreeFormActivityStack) this.mGestureController.mMiuiFreeFormManagerService.getMiuiFreeFormActivityStack(this.mCurrentFreeformRootTask.getRootTaskId());
            Slog.d(TAG, "startSwitchAppOneStep: mCurrentFreeformRootTask= " + this.mCurrentFreeformRootTask + " ffas= " + ffas);
            if (ffas == null) {
                return false;
            }
            this.mGestureController.mWindowController.setSwitchOverlayViewBounds(this.mGestureController.getSmallFreeFormWindowBounds(this.mCurrentFreeformRootTask.getRootTaskId()));
            this.mGestureController.mWindowController.startSwitchOverlayContentAnimation(4, getStackPackageName(this.mCurrentFreeformRootTask), ffas);
            InputManagerCallback inputManagerCallback = this.mInputManagerCallback;
            if (inputManagerCallback != null) {
                inputManagerCallback.freezeInputDispatchingLw();
                this.mHasThawInputDispatch = false;
            }
            this.mGestureController.mHandler.removeCallbacks(this.mThawInputRunnable);
            this.mGestureController.mHandler.postDelayed(this.mThawInputRunnable, 2000L);
            return true;
        }
        this.mGestureController.mWindowController.removeSwitchOverlayView();
        synchronized (this.mSwitchOverlayLock) {
            MiuiFreeFormExchangeOverlay miuiFreeFormExchangeOverlay = this.mSwitchOverlay;
            if (miuiFreeFormExchangeOverlay != null) {
                miuiFreeFormExchangeOverlay.destroy(false);
            }
        }
        this.mCurrentFullRootTask = null;
        return false;
    }

    public void startSwitchAppTwoStep() {
        boolean z;
        Slog.d(TAG, "startSwitchAppTwoStep");
        synchronized (this.mSwitchOverlayLock) {
            MiuiFreeFormExchangeOverlay miuiFreeFormExchangeOverlay = this.mSwitchOverlay;
            z = false;
            if (miuiFreeFormExchangeOverlay != null) {
                miuiFreeFormExchangeOverlay.destroy(false);
            }
        }
        this.mGestureController.mGestureListener.hideInputMethodWindowIfNeeded();
        synchronized (this.mGestureController.mGestureListener.mService.mGlobalLock) {
            Slog.d(TAG, " startSwitchAppTwoStep: mCurrentFreeformRootTask= " + this.mCurrentFreeformRootTask + " mCurrentFullRootTask= " + this.mCurrentFullRootTask);
            Task fullRootTaskTmp = this.mCurrentFullRootTask;
            if (this.mCurrentFreeformRootTask != null && fullRootTaskTmp != null && !fullRootTaskTmp.isActivityTypeHome()) {
                if (Settings.System.getIntForUser(this.mGestureController.mService.mContext.getContentResolver(), "accelerometer_rotation", 0, -2) == 1) {
                    z = true;
                }
                this.mMarkAccelerRotation = z;
                if (z) {
                    this.mGestureController.mService.freezeRotation(-1);
                }
                this.mSwitchingApp = true;
                this.mGestureController.mGestureListener.mFreeFormWindowMotionHelper.cancelAllSpringAnimal();
                MiuiFreeFormActivityStack ffas = (MiuiFreeFormActivityStack) this.mGestureController.mMiuiFreeFormManagerService.getMiuiFreeFormActivityStack(this.mCurrentFreeformRootTask.getRootTaskId());
                if (ffas != null) {
                    ffas.mSwitchingApp = this.mSwitchingApp;
                }
                this.mGestureController.mGestureListener.mGestureAnimator.removeAnimationControlLeash(ffas);
                Rect rect = new Rect();
                WindowManager wm = (WindowManager) this.mGestureController.mService.mContext.getSystemService("window");
                wm.getDefaultDisplay().getRectSize(rect);
                ActivityTaskManager.RootTaskInfo stackInfo = this.mGestureController.mService.mAtmService.mRootWindowContainer.getRootTaskInfo(fullRootTaskTmp.getRootTaskId());
                if (stackInfo != null) {
                    this.mGestureController.mService.mAtmService.launchSmallFreeFormWindow(stackInfo, rect, 2);
                    startSwitchAppThreeStep();
                } else {
                    Slog.d(TAG, " startSwitchAppTwoStep: stackInfo = null");
                    this.mGestureController.mWindowController.removeSwitchOverlayView();
                    this.mCurrentFullRootTask = null;
                    this.mCurrentFreeformRootTask = null;
                }
            } else {
                this.mGestureController.mWindowController.removeSwitchOverlayView();
                this.mCurrentFullRootTask = null;
                this.mCurrentFreeformRootTask = null;
            }
        }
    }

    public void startSwitchAppThreeStep() {
        Slog.d(TAG, "startSwitchAppThreeStep mSwitchingApp=" + this.mSwitchingApp + " mCurrentFreeformRootTask=" + this.mCurrentFreeformRootTask);
        if (this.mSwitchingApp) {
            if (this.mCurrentFreeformRootTask != null) {
                try {
                    Rect rect = new Rect();
                    WindowManager wm = (WindowManager) this.mGestureController.mService.mContext.getSystemService("window");
                    wm.getDefaultDisplay().getRectSize(rect);
                    MiuiFreeFormActivityStack ffas = (MiuiFreeFormActivityStack) this.mGestureController.mMiuiFreeFormManagerService.getMiuiFreeFormActivityStack(this.mCurrentFreeformRootTask.getRootTaskId());
                    if (ffas != null) {
                        ffas.mFreeFormScale = 1.0f;
                    }
                    this.mGestureController.mService.mActivityManager.resizeTask(this.mCurrentFreeformRootTask.getRootTaskId(), (Rect) null, 0);
                    this.mGestureController.mService.mAtmService.setTaskWindowingMode(this.mCurrentFreeformRootTask.getRootTaskId(), 0, true);
                    this.mCurrentFreeformRootTask.sendTaskAppeared();
                    this.mGestureController.mMiuiFreeFormFlashBackHelper.stopFlashBackService(this.mGestureController.mService.mContext, this.mGestureController.mHandler);
                    return;
                } catch (Exception e) {
                    return;
                }
            }
            this.mGestureController.mWindowController.removeSwitchOverlayView();
            this.mCurrentFullRootTask = null;
        }
    }

    public void startSwitchAppFourStep() {
        Slog.d(TAG, "startSwitchAppFourStep mSwitchingApp=" + this.mSwitchingApp);
        if (this.mSwitchingApp) {
            try {
                this.mSwitchingApp = false;
                InputManagerCallback inputManagerCallback = this.mInputManagerCallback;
                if (inputManagerCallback != null) {
                    inputManagerCallback.thawInputDispatchingLw();
                    this.mInputManagerCallback.setEventDispatchingLw(true);
                    this.mHasThawInputDispatch = true;
                }
                this.mGestureController.mWindowController.startRemoveSwitchOverLayViewAnimation(0);
                this.mCurrentFullRootTask = null;
                this.mCurrentFreeformRootTask = null;
                if (this.mMarkAccelerRotation) {
                    this.mMarkAccelerRotation = false;
                    this.mGestureController.mService.thawRotation();
                }
            } catch (Exception e) {
                Slog.d(TAG, "startSwitchAppFourStep failed: " + e);
                InputManagerCallback inputManagerCallback2 = this.mInputManagerCallback;
                if (inputManagerCallback2 != null) {
                    inputManagerCallback2.thawInputDispatchingLw();
                    this.mInputManagerCallback.setEventDispatchingLw(true);
                    this.mHasThawInputDispatch = true;
                }
            }
        }
    }

    public void startSwitchAppFourStepIfNeeded() {
        if (!this.mContainsRotation) {
            startSwitchAppFourStep();
        } else {
            this.mGestureController.mWindowController.startSwitchAppFourStepDelay(1500);
        }
    }

    public void handleMoveGesture(float x, float y, float xVelocity, float yVelocity) {
        if (isEnterExchangeHotArea(this.mGestureController.mService.mContext, x, y) && this.mCanSwitchApp) {
            if (Math.abs(xVelocity) < 1000.0f && Math.abs(yVelocity) < 1000.0f) {
                synchronized (this.mSwitchOverlayLock) {
                    if (!this.mGestureController.mHandler.hasCallbacks(this.mShowSwitchOverlay) && !this.mIsMaskDisplayed) {
                        this.mGestureController.mHandler.postDelayed(this.mShowSwitchOverlay, 100L);
                    }
                }
                return;
            }
            return;
        }
        synchronized (this.mSwitchOverlayLock) {
            this.mGestureController.mHandler.removeCallbacks(this.mShowSwitchOverlay);
            if (this.mSwitchOverlay != null && this.mIsMaskDisplayed) {
                this.mSwitchOverlay.destroy(true);
            }
            this.mIsMaskDisplayed = false;
        }
    }

    public boolean handleUpGesture(float xVelocity, float yVelocity) {
        synchronized (this.mSwitchOverlayLock) {
            this.mGestureController.mHandler.removeCallbacks(this.mShowSwitchOverlay);
            if (this.mShowSwitchOverlay != null && this.mIsMaskDisplayed) {
                Slog.d(TAG, "handleUpGesture : xVelocity= " + xVelocity + " yVelocity= " + yVelocity);
                if (Math.abs(xVelocity) < 1000.0f && Math.abs(yVelocity) < 1000.0f) {
                    this.mIsMaskDisplayed = false;
                    startSwitchAppOneStep();
                    return true;
                }
                this.mIsMaskDisplayed = false;
                this.mSwitchOverlay.destroy(false);
            }
            return false;
        }
    }

    private boolean isEnterExchangeHotArea(Context context, float x, float y) {
        WindowManager wm = (WindowManager) context.getSystemService("window");
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(outMetrics);
        int screenWidth = outMetrics.widthPixels;
        int screenHeight = outMetrics.heightPixels;
        boolean isVertical = true;
        if (screenWidth > screenHeight) {
            isVertical = false;
        }
        Rect vExchangeHotRect = new Rect();
        Rect hExchangeHotRect = new Rect();
        if (isVertical) {
            float f = this.FREEFORM_EXCHANGE_VERTICAL_WIDTH;
            float f2 = this.FREEFORM_EXCHANGE_VERTICAL_HEIGHT;
            vExchangeHotRect.set(((int) (screenWidth - f)) / 2, ((int) (screenHeight - f2)) / 2, (int) ((screenWidth / 2) + (f / 2.0f)), (int) ((screenHeight / 2) + (f2 / 2.0f)));
            float f3 = this.FREEFORM_EXCHANGE_HORIZONTAL_HEIGHT;
            hExchangeHotRect.set((int) ((screenWidth * 0.2857143f) + 0.5f), ((int) (screenHeight - f3)) / 2, screenWidth - ((int) ((screenWidth * 0.2857143f) + 0.5f)), (int) ((screenHeight / 2) + (f3 / 2.0f)));
            if (vExchangeHotRect.contains((int) x, (int) y) || hExchangeHotRect.contains((int) x, (int) y)) {
                return true;
            }
            return false;
        }
        float f4 = this.FREEFORM_EXCHANGE_HORIZONTAL_WIDTH;
        hExchangeHotRect.set(((int) (screenWidth - f4)) / 2, 0, (int) ((screenWidth / 2) + (f4 / 2.0f)), screenHeight);
        return hExchangeHotRect.contains((int) x, (int) y);
    }

    private String getStackPackageName(Task task) {
        if (task == null) {
            return null;
        }
        if (task.origActivity != null) {
            return task.origActivity.getPackageName();
        }
        if (task.realActivity != null) {
            return task.realActivity.getPackageName();
        }
        if (task.getTopActivity(false, true) == null) {
            return null;
        }
        return task.getTopActivity(false, true).packageName;
    }
}
