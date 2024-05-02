package com.android.server.wm;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.MiuiMultiWindowAdapter;
import android.util.MiuiMultiWindowUtils;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import android.view.SurfaceControl;
import com.android.server.wm.LocalAnimationAdapter;
import com.android.server.wm.MiuiFreeformTrackManager;
import com.android.server.wm.SurfaceAnimator;
import java.io.PrintWriter;
import java.util.function.Consumer;
import miui.app.MiuiFreeFormManager;
/* loaded from: classes.dex */
public class MiuiFreeFormActivityStack extends MiuiFreeFormActivityStackStub {
    private final String TAG;
    boolean isChangingFromFreeformToFullscreen;
    CVWControlInfo mCVWControlInfo;
    private Controller mController;
    int mCornerPosition;
    int mDisplayId;
    float mEnterVelocityX;
    float mEnterVelocityY;
    Bitmap mExitIconBitmap;
    int mExitIconHeight;
    int mExitIconWidth;
    float mExitPivotX;
    float mExitPivotY;
    float mExitVelocityX;
    float mExitVelocityY;
    float mFreeFormScale;
    float mFreeformCornerRadius;
    boolean mHadHideStack;
    boolean mHasHadStackAdded;
    boolean mHasSetShadow;
    private volatile boolean mInPinMode;
    boolean mIsEnterClick;
    boolean mIsFlashBackMode;
    boolean mIsLandcapeFreeform;
    boolean mIsLaunchFlashBackFromBackGround;
    boolean mIsLaunchingSmallFreeForm;
    boolean mIsPinFloatingWindowPosInit;
    boolean mIsRelaunchingActivityInTask;
    boolean mIsRunningPinAnim;
    boolean mIsRunningUnPinAnim;
    int mLastFloatIconlayerTaskId;
    ActivityRecord mLastIconLayerWindowToken;
    int mLastTokenLayer;
    int[] mMaxMinWidthSize;
    boolean mMemoryExceptionHadBeenNegatived;
    int mMiuiFreeFromWindowMode;
    boolean mNeedAnimation;
    Rect mPinFloatingWindowPos;
    long mPinedStartTime;
    boolean mStackBeenHandled;
    StackControlInfo mStackControlInfo;
    int mStackID;
    String mStackRatio;
    boolean mStartExitStack;
    boolean mSwitchingApp;
    Task mTask;
    MiuiFreeFormAnimationAdapter mTaskAnimationAdapter;
    int mTopActivityOrientation;
    int mUserId;
    float mWidthHeightScale;
    float mWindowRoundCorner;
    float mWindowScaleX;
    float mWindowScaleY;
    boolean shouldDelayDispatchFreeFormStackModeChanged;
    boolean topWindowHasDrawn;

    /* loaded from: classes.dex */
    public enum Controller {
        NONE,
        FREEFORM,
        CVW
    }

    public boolean inPinMode() {
        return this.mInPinMode;
    }

    public void setInPinMode(boolean inPinMode) {
        this.mInPinMode = inPinMode;
    }

    public int getPinMode() {
        return !this.mInPinMode ? 0 : 2;
    }

    boolean isChangingFromFreeformToFullscreen() {
        return this.isChangingFromFreeformToFullscreen;
    }

    boolean isRunningUnPinAnim() {
        return this.mIsRunningUnPinAnim;
    }

    boolean isRunningPinAnim() {
        return this.mIsRunningPinAnim;
    }

    public void setCornerPosition(int cornerPosition) {
        this.mCornerPosition = cornerPosition;
    }

    public boolean isLaunchingSmallFreeForm() {
        return this.mIsLaunchingSmallFreeForm;
    }

    public boolean isLandcapeFreeform() {
        return this.mIsLandcapeFreeform;
    }

    public void setIsFlashBackMode(boolean flashBackMode) {
        this.mIsFlashBackMode = flashBackMode;
    }

    public boolean isFlashBackMode() {
        return this.mIsFlashBackMode;
    }

    public void setIsLaunchFlashBackFromBackGround(boolean isLaunchFlashBackFromBackGround) {
        this.mIsLaunchFlashBackFromBackGround = isLaunchFlashBackFromBackGround;
    }

    public boolean isLaunchFlashBackFromBackGround() {
        return this.mIsLaunchFlashBackFromBackGround;
    }

    public MiuiFreeFormActivityStack(Task task) {
        this(task, -1);
    }

    public MiuiFreeFormActivityStack(Task task, int miuiFreeFromWindowMode) {
        boolean z;
        boolean z2;
        this.TAG = "MiuiFreeFormActivityStack";
        this.mTaskAnimationAdapter = null;
        this.mMiuiFreeFromWindowMode = -1;
        this.mHasHadStackAdded = false;
        this.mHadHideStack = false;
        this.mStartExitStack = false;
        this.mNeedAnimation = true;
        this.mStackBeenHandled = false;
        this.mCornerPosition = 1;
        this.mSwitchingApp = false;
        this.mIsRelaunchingActivityInTask = false;
        this.shouldDelayDispatchFreeFormStackModeChanged = false;
        this.mIsFlashBackMode = false;
        this.mIsLaunchFlashBackFromBackGround = false;
        this.mTopActivityOrientation = -1;
        this.mMemoryExceptionHadBeenNegatived = false;
        this.mMaxMinWidthSize = new int[2];
        this.mController = Controller.NONE;
        this.mPinFloatingWindowPos = new Rect();
        this.mLastFloatIconlayerTaskId = -1;
        this.mLastTokenLayer = -1;
        this.mLastIconLayerWindowToken = null;
        this.mTask = task;
        this.mUserId = task.mUserId;
        this.mStackID = task.inMultiWindowMode() ? task.mTaskId : task.getRootTaskId();
        this.mDisplayId = task.getDisplayId();
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.LAUNCHER");
        intent.setPackage(getStackPackageName());
        ActivityInfo info = intent.resolveActivityInfo(task.mAtmService.mContext.getPackageManager(), 65536);
        if (info == null) {
            if (!MiuiMultiWindowUtils.isOrientationLandscape(task.getOrientation()) && !MiuiMultiWindowAdapter.FORCE_LANDSCAPE_APPLICATION.contains(getStackPackageName())) {
                z2 = false;
            } else {
                z2 = true;
            }
            this.mIsLandcapeFreeform = z2;
        } else {
            if (!MiuiMultiWindowUtils.isOrientationLandscape(info.screenOrientation) && !MiuiMultiWindowAdapter.FORCE_LANDSCAPE_APPLICATION.contains(getStackPackageName())) {
                z = false;
            } else {
                z = true;
            }
            this.mIsLandcapeFreeform = z;
        }
        this.mFreeFormScale = MiuiMultiWindowUtils.getOriFreeformScale(task.mAtmService.mContext, this.mIsLandcapeFreeform);
        this.mIsLaunchingSmallFreeForm = false;
        this.mStackRatio = this.mIsLandcapeFreeform ? MiuiFreeformTrackManager.SmallWindowTrackConstants.STACK_RATIO2 : MiuiFreeformTrackManager.SmallWindowTrackConstants.STACK_RATIO1;
        this.mStackControlInfo = new StackControlInfo();
        if (!task.isActivityTypeHome()) {
            setStackFreeFormMode(0);
        }
        this.shouldDelayDispatchFreeFormStackModeChanged = false;
        if (miuiFreeFromWindowMode != -1) {
            this.mMiuiFreeFromWindowMode = miuiFreeFromWindowMode;
            if (miuiFreeFromWindowMode == 1) {
                this.mIsLaunchingSmallFreeForm = true;
                this.mFreeFormScale = 1.0f;
            }
        }
    }

    public MiuiFreeFormActivityStack(MiuiFreeFormActivityStack other) {
        this.TAG = "MiuiFreeFormActivityStack";
        this.mTaskAnimationAdapter = null;
        this.mMiuiFreeFromWindowMode = -1;
        this.mHasHadStackAdded = false;
        this.mHadHideStack = false;
        this.mStartExitStack = false;
        this.mNeedAnimation = true;
        this.mStackBeenHandled = false;
        this.mCornerPosition = 1;
        this.mSwitchingApp = false;
        this.mIsRelaunchingActivityInTask = false;
        this.shouldDelayDispatchFreeFormStackModeChanged = false;
        this.mIsFlashBackMode = false;
        this.mIsLaunchFlashBackFromBackGround = false;
        this.mTopActivityOrientation = -1;
        this.mMemoryExceptionHadBeenNegatived = false;
        this.mMaxMinWidthSize = new int[2];
        this.mController = Controller.NONE;
        this.mPinFloatingWindowPos = new Rect();
        this.mLastFloatIconlayerTaskId = -1;
        this.mLastTokenLayer = -1;
        this.mLastIconLayerWindowToken = null;
        this.mTask = other.mTask;
        this.mUserId = other.mTask.mUserId;
        this.mStackID = other.mStackID;
        this.mDisplayId = other.mDisplayId;
        this.mIsLaunchingSmallFreeForm = other.mIsLaunchingSmallFreeForm;
        this.mFreeFormScale = other.mFreeFormScale;
        this.mIsLandcapeFreeform = other.mIsLandcapeFreeform;
        this.mStackControlInfo = other.mStackControlInfo;
        this.mTaskAnimationAdapter = other.mTaskAnimationAdapter;
        this.mMiuiFreeFromWindowMode = other.mMiuiFreeFromWindowMode;
        this.mHadHideStack = other.mHadHideStack;
        this.mStartExitStack = other.mStartExitStack;
        this.mNeedAnimation = other.mNeedAnimation;
        this.mCornerPosition = other.mCornerPosition;
        this.mCVWControlInfo = other.mCVWControlInfo;
        this.mStackRatio = other.mStackRatio;
        this.mHasHadStackAdded = other.mHasHadStackAdded;
        this.mStackBeenHandled = other.mStackBeenHandled;
        this.mSwitchingApp = other.mSwitchingApp;
        this.mIsRelaunchingActivityInTask = other.mIsRelaunchingActivityInTask;
        this.shouldDelayDispatchFreeFormStackModeChanged = other.shouldDelayDispatchFreeFormStackModeChanged;
        this.mWidthHeightScale = other.mWidthHeightScale;
        this.mMaxMinWidthSize = other.mMaxMinWidthSize;
    }

    public void onMiuiFreeFormStasckAdded() {
        updateCornerRadius();
    }

    public void avoidNewlyAddedStackIfNeeded(Rect newVisualBounds) {
        Rect bounds = this.mTask.getBounds();
        Rect visualBounds = MiuiMultiWindowUtils.getVisualBounds(bounds, this.mFreeFormScale);
        MiuiMultiWindowUtils.avoidAsPossible(visualBounds, newVisualBounds, MiuiMultiWindowUtils.getFreeFormAccessibleArea(this.mTask.mAtmService.mContext));
        if (visualBounds.left != bounds.left || visualBounds.top != bounds.top) {
            this.mStackControlInfo.mWindowBounds = new Rect(bounds);
            this.mStackControlInfo.mWindowBounds.offsetTo(visualBounds.left, visualBounds.top);
            LocalAnimationAdapter localAnimationAdapter = new LocalAnimationAdapter(new TranslateSpec(new Point(bounds.left, bounds.top), new Point(this.mStackControlInfo.mWindowBounds.left, this.mStackControlInfo.mWindowBounds.top), 200L), this.mTask.getSurfaceAnimationRunner());
            Task task = this.mTask;
            task.startAnimation(task.getPendingTransaction(), localAnimationAdapter, false, 1073741824, new SurfaceAnimator.OnAnimationFinishedCallback() { // from class: com.android.server.wm.MiuiFreeFormActivityStack$$ExternalSyntheticLambda1
                public final void onAnimationFinished(int i, AnimationAdapter animationAdapter) {
                    MiuiFreeFormActivityStack.this.m1628x8f6ec8b9(i, animationAdapter);
                }
            });
            this.mTask.getPendingTransaction().apply();
        }
    }

    /* renamed from: lambda$avoidNewlyAddedStackIfNeeded$0$com-android-server-wm-MiuiFreeFormActivityStack */
    public /* synthetic */ void m1628x8f6ec8b9(int type, AnimationAdapter anim) {
        try {
            this.mTask.mAtmService.resizeTask(this.mStackID, this.mStackControlInfo.mWindowBounds, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void avoidNewlyAddedStackIfNeeded(MiuiFreeFormActivityStack upperStack) {
        avoidNewlyAddedStackIfNeeded(MiuiMultiWindowUtils.getVisualBounds(upperStack.mTask.getBounds(), upperStack.mFreeFormScale));
    }

    public void onMiuiFreeFormStasckremove() {
        this.mTask.mNeedsZBoost = false;
        if (this.mTask.getSurfaceControl() != null && this.mTask.getSurfaceControl().isValid()) {
            this.mTask.mNeedsZBoost = false;
            MiuiFreeFormGestureController miuiFreeFormGestureController = (MiuiFreeFormGestureController) this.mTask.mWmService.mMiuiFreeFormGestureController;
            miuiFreeFormGestureController.mMiuiFreeFormShadowHelper.resetShadowSettings(this, false);
            SurfaceControl.Transaction t = new SurfaceControl.Transaction();
            t.setCornerRadius(this.mTask.getSurfaceControl(), MiuiFreeformPinManagerService.EDGE_AREA);
            t.apply();
        }
    }

    public void onConfigurationChanged() {
    }

    public String getStackPackageName() {
        Task task = this.mTask;
        if (task == null) {
            return null;
        }
        if (task.origActivity != null) {
            return this.mTask.origActivity.getPackageName();
        }
        if (this.mTask.realActivity != null) {
            return this.mTask.realActivity.getPackageName();
        }
        if (this.mTask.getTopActivity(false, true) == null) {
            return null;
        }
        return this.mTask.getTopActivity(false, true).packageName;
    }

    public String getApplicationName() {
        ApplicationInfo applicationInfo;
        PackageManager packageManager = null;
        try {
            packageManager = this.mTask.mAtmService.mContext.getPackageManager();
            applicationInfo = packageManager.getApplicationInfo(getStackPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            applicationInfo = null;
        }
        if (applicationInfo == null) {
            return "";
        }
        String applicationName = (String) packageManager.getApplicationLabel(applicationInfo);
        return applicationName;
    }

    public void setStackFreeFormMode(int mode) {
        this.mMiuiFreeFromWindowMode = mode;
        updateCornerRadius();
        MiuiFreeFormManagerService.logd("MiuiFreeFormActivityStack", "setStackFreeFormMode = " + this.mMiuiFreeFromWindowMode + " mTask = " + getStackPackageName());
    }

    public boolean isInMiniFreeFormMode() {
        return this.mMiuiFreeFromWindowMode == 1;
    }

    public boolean isInFreeFormMode() {
        return this.mMiuiFreeFromWindowMode == 0;
    }

    void relaunchActivityLocked(boolean preserveWindow) {
        this.mIsRelaunchingActivityInTask = true;
    }

    public float getFreeFormScale() {
        return this.mFreeFormScale;
    }

    public SurfaceControl getFreeFormConrolSurface() {
        MiuiFreeFormAnimationAdapter miuiFreeFormAnimationAdapter = this.mTaskAnimationAdapter;
        if (miuiFreeFormAnimationAdapter != null) {
            synchronized (miuiFreeFormAnimationAdapter.mLock) {
                MiuiFreeFormAnimationAdapter miuiFreeFormAnimationAdapter2 = this.mTaskAnimationAdapter;
                if (miuiFreeFormAnimationAdapter2 != null && miuiFreeFormAnimationAdapter2.mCapturedLeash != null && this.mTaskAnimationAdapter.mCapturedLeash.isValid()) {
                    SurfaceControl capturedLeash = this.mTaskAnimationAdapter.mCapturedLeash;
                    return capturedLeash;
                }
                return null;
            }
        }
        return null;
    }

    public void createLeashIfNeeded(float cornerRadius) {
        synchronized (this.mTask.mWmService.mGlobalLock) {
            if (this.mTaskAnimationAdapter != null) {
                MiuiFreeFormManagerService.logd("MiuiFreeFormActivityStack", "createLeashIfNeeded NOT CreateD mTask = " + getStackPackageName());
                return;
            }
            this.mTaskAnimationAdapter = new MiuiFreeFormAnimationAdapter();
            Task task = this.mTask;
            task.startAnimation(task.getPendingTransaction(), this.mTaskAnimationAdapter, false, 1073741824, (SurfaceAnimator.OnAnimationFinishedCallback) null);
            if (this.mTaskAnimationAdapter.mCapturedLeash != null && this.mTaskAnimationAdapter.mCapturedLeash.isValid()) {
                Point rc = new Point();
                this.mTask.getRelativePosition(rc);
                this.mTask.getPendingTransaction().setPosition(this.mTaskAnimationAdapter.mCapturedLeash, rc.x, rc.y);
                Task task2 = this.mTask;
                task2.updateSurfaceSize(task2.getPendingTransaction());
                this.mTask.getPendingTransaction().apply();
                MiuiFreeFormManagerService.logd("MiuiFreeFormActivityStack", "createLeashIfNeeded CreateD mTask = " + getStackPackageName());
            }
        }
    }

    public void removeAnimationControlLeash() {
        MiuiFreeFormManagerService.logd("MiuiFreeFormActivityStack", "removeAnimationControlLeash mMiuiFreeFromWindowMode= " + this.mMiuiFreeFromWindowMode + "mTaskAnimationAdapter = " + this.mTaskAnimationAdapter + " mTask = " + getStackPackageName());
        synchronized (this.mTask.mWmService.mGlobalLock) {
            MiuiFreeFormAnimationAdapter miuiFreeFormAnimationAdapter = this.mTaskAnimationAdapter;
            if (miuiFreeFormAnimationAdapter != null) {
                miuiFreeFormAnimationAdapter.onFreeFormAnimationFinished();
                this.mTask.forAllWindows(new Consumer() { // from class: com.android.server.wm.MiuiFreeFormActivityStack$$ExternalSyntheticLambda0
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        ((WindowState) obj).onExitAnimationDone();
                    }
                }, true);
                Task task = this.mTask;
                task.updateSurfaceSize(task.getPendingTransaction());
                this.mTask.commitPendingTransaction();
                this.mTaskAnimationAdapter = null;
            }
            if (!this.mTask.isActivityTypeHome() && this.mMiuiFreeFromWindowMode != -1) {
                updateCornerRadius();
            }
        }
    }

    public void updateCornerRadius() {
        int statusBarHeight;
        synchronized (this.mTask.mWmService.mGlobalLock) {
            if (!this.mTask.mLaunchMiniFreeformFromFull && this.mTask.getSurfaceControl() != null && this.mTask.getSurfaceControl().isValid()) {
                SurfaceControl.Transaction t = new SurfaceControl.Transaction();
                if (isInMiniFreeFormMode()) {
                    if (this.mStackControlInfo.mSmallWindowTargetHScale == MiuiFreeformPinManagerService.EDGE_AREA || this.mStackControlInfo.mSmallWindowTargetWScale == MiuiFreeformPinManagerService.EDGE_AREA) {
                        RectF smallFreeformRect = MiuiMultiWindowUtils.getSmallFreeformRect(this.mTask.mAtmService.mContext, this.mIsLandcapeFreeform);
                        DisplayContent displayContent = this.mTask.getDisplayContent();
                        if (displayContent != null && displayContent.getInsetsStateController() != null) {
                            int statusBarHeight2 = Math.max(MiuiFreeFormGestureDetector.getStatusBarHeight(displayContent.getInsetsStateController()), MiuiFreeFormGestureDetector.getDisplayCutoutHeight(displayContent.mDisplayFrames));
                            statusBarHeight = statusBarHeight2;
                        } else {
                            statusBarHeight = 0;
                        }
                        Rect lastBounds = MiuiMultiWindowUtils.getFreeformRect(this.mTask.mAtmService.mContext, false, false, true, this.mIsLandcapeFreeform, (Rect) null, getStackPackageName(), true, statusBarHeight);
                        float freeFormScale = MiuiMultiWindowUtils.getOriFreeformScale(this.mTask.mAtmService.mContext, this.mIsLandcapeFreeform);
                        this.mStackControlInfo.mSmallWindowTargetWScale = (smallFreeformRect.width() / lastBounds.width()) * freeFormScale;
                        this.mStackControlInfo.mSmallWindowTargetHScale = (smallFreeformRect.height() / lastBounds.height()) * freeFormScale;
                    }
                    float maxScale = Math.max(this.mStackControlInfo.mSmallWindowTargetHScale, this.mStackControlInfo.mSmallWindowTargetWScale);
                    this.mFreeformCornerRadius = MiuiMultiWindowUtils.getSmallFreeformRoundCorner(this.mTask.mAtmService.mContext) / maxScale;
                } else if (isInFreeFormMode()) {
                    this.mFreeformCornerRadius = MiuiMultiWindowUtils.getFreeformRoundCorner(this.mTask.mAtmService.mContext);
                }
                t.setCornerRadius(this.mTask.getSurfaceControl(), this.mFreeformCornerRadius);
                t.apply();
            }
        }
    }

    public String toString() {
        StringBuilder info = new StringBuilder();
        info.append("MiuiFreeFormActivityStack{");
        info.append(" mStackID=" + this.mStackID);
        info.append(" mUserId=" + this.mUserId);
        info.append(" mDisplayId=" + this.mDisplayId);
        info.append(" mPackageName=" + getStackPackageName());
        info.append(" mFreeFormScale=" + this.mFreeFormScale);
        info.append(" mIsLaunchingSmallFreeForm=" + this.mIsLaunchingSmallFreeForm);
        info.append(" mTaskAnimationAdapter=" + this.mTaskAnimationAdapter);
        info.append(" mMiuiFreeFromWindowMode=" + this.mMiuiFreeFromWindowMode);
        info.append(" mConfiguration=" + this.mTask.getConfiguration());
        info.append(" mHadHideStack=" + this.mHadHideStack);
        info.append(" mStartExitStack=" + this.mStartExitStack);
        info.append(" mStackRatio=" + this.mStackRatio);
        info.append(" mNeedAnimation=" + this.mNeedAnimation);
        info.append(" mSwitchingApp=" + this.mSwitchingApp);
        info.append(" mIsLandcapeFreeform=" + this.mIsLandcapeFreeform);
        info.append(" mIsRelaunchingActivityInTask=" + this.mIsRelaunchingActivityInTask);
        info.append(" mStackBeenHandled=" + this.mStackBeenHandled);
        if (isInMiniFreeFormMode()) {
            info.append(" mSmallWindowBounds=" + this.mStackControlInfo.mSmallWindowBounds);
        }
        info.append(" mCVWControlInfo=" + this.mCVWControlInfo);
        info.append(" mInPinMode=" + this.mInPinMode);
        info.append(" mIsRunningPinAnim=" + this.mIsRunningPinAnim);
        info.append(" mIsRunningUnPinAnim=" + this.mIsRunningUnPinAnim);
        info.append(" mWindowScaleX=" + this.mWindowScaleX);
        info.append(" mWindowScaleY=" + this.mWindowScaleY);
        info.append(" mPinFloatingWindowFinalRoundCorner=" + this.mWindowRoundCorner);
        info.append(" mPinFloatingWindowPos=" + this.mPinFloatingWindowPos);
        info.append(" mFreeformCornerRadius=" + this.mFreeformCornerRadius);
        info.append('}');
        return info.toString();
    }

    public MiuiFreeFormManager.MiuiFreeFormStackInfo getMiuiFreeFormStackInfo() {
        MiuiFreeFormManager.MiuiFreeFormStackInfo freeFormStackInfo = new MiuiFreeFormManager.MiuiFreeFormStackInfo();
        freeFormStackInfo.stackId = this.mStackID;
        freeFormStackInfo.bounds = this.mTask.getBounds();
        freeFormStackInfo.windowState = this.mMiuiFreeFromWindowMode;
        freeFormStackInfo.packageName = getStackPackageName();
        freeFormStackInfo.displayId = this.mDisplayId;
        freeFormStackInfo.userId = this.mTask.mUserId;
        freeFormStackInfo.configuration = this.mTask.getConfiguration();
        if (this.mMiuiFreeFromWindowMode == -1) {
            freeFormStackInfo.smallWindowBounds = new Rect();
            freeFormStackInfo.freeFormScale = 1.0f;
        } else {
            freeFormStackInfo.smallWindowBounds = this.mStackControlInfo.mSmallWindowBounds;
            freeFormStackInfo.freeFormScale = this.mFreeFormScale;
        }
        freeFormStackInfo.inPinMode = this.mInPinMode;
        freeFormStackInfo.windowScaleX = this.mWindowScaleX;
        freeFormStackInfo.windowScaleY = this.mWindowScaleY;
        freeFormStackInfo.windowRoundCorner = this.mWindowRoundCorner;
        freeFormStackInfo.pinFloatingWindowPos = this.mPinFloatingWindowPos;
        freeFormStackInfo.freeformCornerRadius = this.mFreeformCornerRadius;
        freeFormStackInfo.runningPinAnim = this.mIsRunningPinAnim;
        return freeFormStackInfo;
    }

    void setController(Controller controller) {
        Slog.d("MiuiFreeFormActivityStack", "set controller to " + controller + " freeformStack:" + this);
        this.mController = controller;
    }

    public boolean isControlled() {
        return this.mController != Controller.NONE;
    }

    public boolean isControlledByFreeForm() {
        return this.mController == Controller.FREEFORM;
    }

    public boolean isControlledByCvw() {
        return this.mController == Controller.CVW;
    }

    public boolean checkReadyForFreeFormControl() {
        return checkReadyForControl(Controller.FREEFORM);
    }

    public boolean checkReadyForCvwControl() {
        return checkReadyForControl(Controller.CVW);
    }

    private synchronized boolean checkReadyForControl(Controller controller) {
        if ((controller == Controller.CVW && isControlledByFreeForm()) || (controller == Controller.FREEFORM && isControlledByCvw())) {
            return false;
        }
        setController(controller);
        return true;
    }

    public synchronized void endControl() {
        if (isControlled()) {
            setController(Controller.NONE);
        }
    }

    /* loaded from: classes.dex */
    public static final class StackControlInfo {
        public static final int GESTURE_ACTION_DOWN = 0;
        public static final int GESTURE_ACTION_UNDEFINED = -1;
        public static final int GESTURE_ACTION_UP = 1;
        int mCurrentAction;
        float mNowAlpha;
        float mNowClipHeight;
        float mNowClipWidth;
        float mNowPosX;
        float mNowPosY;
        float mNowRoundCorner;
        float mNowScale;
        float mNowShadowAlpha;
        float mOriPosX;
        float mOriPosY;
        float mScaleWindowHeight;
        float mScaleWindowWidth;
        float mSmallWindowTargetHScale;
        float mSmallWindowTargetWScale;
        float mTargetAlpha;
        float mTargetClipHeight;
        float mTargetClipWidth;
        float mTargetHeightScale;
        float mTargetPosX;
        float mTargetPosY;
        float mTargetRoundCorner;
        float mTargetScale;
        float mTargetShadowAlpha;
        float mTargetWidthScale;
        Rect mLastSmallWindowBounds = new Rect();
        Rect mLastFreeFormWindowStartBounds = new Rect();
        Rect mLastWindowDragBounds = new Rect();
        float mNowWidthScale = 1.0f;
        float mNowHeightScale = 1.0f;
        Rect mSmallWindowBounds = new Rect();
        Rect mWindowBounds = new Rect();
        int mCurrentAnimation = -1;
        int mCurrentGestureAction = -1;

        StackControlInfo() {
        }
    }

    /* loaded from: classes.dex */
    public static final class CVWControlInfo {
        float freeFormScale;
        Configuration fullConfiguration;
        Rect taskBounds = new Rect();
        Rect miniFreeformBounds = new Rect();
        boolean needRemoveCvwCoverLayer = false;
        boolean isSwitchedToFreeform = false;

        public String toString() {
            StringBuilder info = new StringBuilder();
            info.append("CVWControlInfo{");
            info.append(" taskBounds=" + this.taskBounds);
            info.append(" miniFreeformBounds=" + this.miniFreeformBounds);
            info.append(" freeFormScale=" + this.freeFormScale);
            info.append(" needRemoveCvwCoverLayer=" + this.needRemoveCvwCoverLayer);
            info.append(" fullConfiguration=" + this.fullConfiguration);
            info.append(" isSwitchedToFreeform=" + this.isSwitchedToFreeform);
            info.append('}');
            return info.toString();
        }
    }

    public boolean isLaunchedByCVW() {
        return this.mCVWControlInfo != null;
    }

    /* loaded from: classes.dex */
    public class TranslateSpec implements LocalAnimationAdapter.AnimationSpec {
        private long mDuration;
        private Point mFrom = new Point();
        private Point mTo = new Point();

        TranslateSpec(Point from, Point to, long duration) {
            MiuiFreeFormActivityStack.this = this$0;
            this.mFrom.x = from.x;
            this.mFrom.y = from.y;
            this.mTo.x = to.x;
            this.mTo.y = to.y;
            this.mDuration = duration;
        }

        public long getDuration() {
            return this.mDuration;
        }

        public void apply(SurfaceControl.Transaction t, SurfaceControl leash, long currentPlayTime) {
            float fraction = getFraction((float) currentPlayTime);
            float x = ((this.mTo.x - this.mFrom.x) * fraction) + this.mFrom.x;
            float y = ((this.mTo.y - this.mFrom.y) * fraction) + this.mFrom.y;
            t.setPosition(leash, x, y);
        }

        public void dump(PrintWriter pw, String prefix) {
        }

        public void dumpDebugInner(ProtoOutputStream proto) {
        }
    }
}
