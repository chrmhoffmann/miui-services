package com.android.server.wm;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.util.MiuiMultiWindowAdapter;
import android.util.MiuiMultiWindowUtils;
import android.view.DisplayInfo;
import android.view.WindowInsets;
import com.android.server.wm.MiuiCvwGestureController;
import com.android.server.wm.MiuiCvwSnapTargetPool;
import com.android.server.wm.MiuiFreeformTrackManager;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
/* loaded from: classes.dex */
public class MiuiCvwPolicy {
    private static final int DISTANCE_MINI_TOUCH_THRESHOLD = 8;
    static boolean mIsScreenLandscape;
    MiuiCvwAspectRatioConfig mAspectRatioConfig;
    private final MiuiCvwGestureController mController;
    protected final MiuiCvwSnapTargetPool mSnapTargetPool;
    private static final String TAG = MiuiCvwPolicy.class.getSimpleName();
    static int mCurrScreenHeight = 0;
    static int mCurrScreenWidth = 0;
    static List<String> FORBIDDEN_PACKAGES = new ArrayList();
    static List<String> FORBIDDEN_WINDOWS = new ArrayList();
    private TaskWrapperInfo mFreeformTaskInfo = null;
    private TaskWrapperInfo mFullScreenTaskInfo = null;
    private TaskWrapperInfo mResultTaskInfo = null;
    private TaskWrapperInfo mTmpFreeformWrapperInfo = null;
    private final Rect mHotAreaBounds = new Rect();
    private final Rect mDisplayBounds = new Rect();
    private final DisplayInfo mDisplayInfo = new DisplayInfo();

    static {
        FORBIDDEN_PACKAGES.add("com.xiaomi.mirror");
        FORBIDDEN_PACKAGES.add("com.android.quicksearchbox");
        FORBIDDEN_WINDOWS.add("com.android.settings.connecteddevice.usb.UsbModeChooserActivity");
        FORBIDDEN_WINDOWS.add(MiuiFreeFormGestureDetector.SYSTEM_APPPERMISSION_DIALOGACTIVITY);
        FORBIDDEN_WINDOWS.add(ActivityRecordImpl.PERMISSION_ACTIVITY);
    }

    public MiuiCvwPolicy(MiuiCvwGestureController controller) {
        this.mController = controller;
        this.mSnapTargetPool = new MiuiCvwSnapTargetPool(controller.mDisplayContent);
        this.mAspectRatioConfig = new MiuiCvwAspectRatioConfig(controller.mWmService.mContext, controller.mExecuteThread.getLooper());
    }

    public void onDisplayInfoChanged(DisplayContent displayContent, Configuration config) {
        DisplayInfo displayInfo = displayContent.getDisplayInfo();
        this.mDisplayInfo.copyFrom(displayInfo);
        this.mDisplayBounds.set(displayContent.getWindowConfiguration().getBounds());
        MiuiCvwGestureController.Slog.d(TAG, "onDisplayInfoChanged mDisplayBounds :" + this.mDisplayBounds);
        mCurrScreenWidth = this.mDisplayBounds.width();
        mCurrScreenHeight = this.mDisplayBounds.height();
        boolean z = true;
        if (config.orientation == 1) {
            z = false;
        }
        mIsScreenLandscape = z;
        this.mHotAreaBounds.set(this.mDisplayBounds);
        this.mSnapTargetPool.updateTargets(config);
    }

    public boolean isForbiddenWindow(ActivityRecord topRunning) {
        if (FORBIDDEN_PACKAGES.contains(topRunning.packageName)) {
            MiuiCvwGestureController.Slog.d(TAG, "isForbiddenWindow package :" + topRunning.packageName);
            return true;
        } else if (FORBIDDEN_WINDOWS.contains(topRunning.mActivityComponent.getClassName())) {
            MiuiCvwGestureController.Slog.d(TAG, "isForbiddenWindow window :" + topRunning.mActivityComponent.getClassName());
            return true;
        } else {
            return false;
        }
    }

    public Rect getHotAreaBounds() {
        return this.mHotAreaBounds;
    }

    Rect getDisplayBounds() {
        return this.mDisplayBounds;
    }

    public int getStatusBarHeight() {
        return MiuiMultiWindowUtils.getInsetValueFromServer(this.mController.mWmService.mContext, WindowInsets.Type.statusBars());
    }

    public int getNavBarHeight() {
        return MiuiMultiWindowUtils.getInsetValueFromServer(this.mController.mWmService.mContext, WindowInsets.Type.navigationBars());
    }

    public int getTouchSlop() {
        return 8;
    }

    public TaskWrapperInfo updateFreeformTaskWrapperInfo(MiuiFreeFormActivityStack stack) {
        TaskWrapperInfo create = TaskWrapperInfo.create(stack, this);
        this.mFreeformTaskInfo = create;
        return create;
    }

    public TaskWrapperInfo updateFullScreenTaskWrapperInfo(MiuiFreeFormActivityStack stack) {
        TaskWrapperInfo create = TaskWrapperInfo.create(stack, this);
        this.mFullScreenTaskInfo = create;
        return create;
    }

    public TaskWrapperInfo getTaskWrapperInfo(Task task, boolean isFreeform) {
        if (isFreeform) {
            return this.mFreeformTaskInfo;
        }
        return this.mFullScreenTaskInfo;
    }

    public void updateTmpFreeformTaskWrapper(float ratio, Rect visualBounds) {
        this.mTmpFreeformWrapperInfo = new TaskWrapperInfo();
        MiuiCvwSnapTargetPool.SnapTarget target = this.mSnapTargetPool.getSnapTargetByRawRatio(ratio);
        if (target != null) {
            int visualFreeformHeight = visualBounds.height();
            int actualFreeformWidth = (int) (target.getWidth() + 0.5f);
            int actualFreeformHeight = (int) (target.getHeight() + 0.5f);
            int freeformLeft = visualBounds.left;
            int freeformTop = visualBounds.top;
            int freeformRight = visualBounds.left + actualFreeformWidth;
            int freeformBottom = visualBounds.top + actualFreeformHeight;
            this.mTmpFreeformWrapperInfo.actualBounds.set(freeformLeft, freeformTop, freeformRight, freeformBottom);
            this.mTmpFreeformWrapperInfo.scale = (visualFreeformHeight * 1.0f) / actualFreeformHeight;
            this.mTmpFreeformWrapperInfo.visualBounds.set(visualBounds);
        }
    }

    public TaskWrapperInfo getTmpFreeformTaskWrapper() {
        return this.mTmpFreeformWrapperInfo;
    }

    public void updateResultTaskWrapperInfo(float ratio, Rect visualBounds, Rect miniBounds, int animtionType) {
        this.mResultTaskInfo = new TaskWrapperInfo();
        MiuiCvwSnapTargetPool.SnapTarget target = this.mSnapTargetPool.getSnapTargetByRawRatio(ratio);
        if (target != null) {
            int visualFreeformHeight = visualBounds.height();
            int actualFreeformWidth = (int) (target.getWidth() + 0.5f);
            int actualFreeformHeight = (int) (target.getHeight() + 0.5f);
            int freeformLeft = visualBounds.left;
            int freeformTop = visualBounds.top;
            int freeformRight = visualBounds.left + actualFreeformWidth;
            int freeformBottom = visualBounds.top + actualFreeformHeight;
            this.mResultTaskInfo.actualBounds.set(freeformLeft, freeformTop, freeformRight, freeformBottom);
            this.mResultTaskInfo.scale = (visualFreeformHeight * 1.0f) / actualFreeformHeight;
            this.mResultTaskInfo.miniBounds.set(miniBounds);
            this.mResultTaskInfo.visualBounds.set(visualBounds);
            this.mController.mDefaultGestureHandler.mLayerSupervisor.preExecute(this.mController.mStack, animtionType);
        }
    }

    public TaskWrapperInfo getResultTaskWrapperInfo() {
        return this.mResultTaskInfo;
    }

    public boolean isGameOrFullScreenVideoWindow(MiuiFreeFormActivityStack stack) {
        try {
            boolean isGame = isGameApp(stack.getStackPackageName());
            boolean isLandscapeVideo = isLandscapeVideo(stack.mTask, this.mController.mWmService.mContext);
            return isGame || isLandscapeVideo;
        } catch (Exception e) {
            MiuiCvwGestureController.Slog.e(TAG, "isForceRatioApplication has an error :" + e.toString());
            return false;
        }
    }

    private static boolean isLandscapeVideo(Task task, Context context) {
        ActivityRecord topRunning;
        WindowState windowState;
        if (task == null || (topRunning = task.topRunningActivityLocked()) == null || ((!MiuiMultiWindowUtils.isOrientationLandscape(topRunning.mOrientation) && (context.getResources().getConfiguration().orientation != 2 || topRunning.mOrientation != -1)) || !MiuiMultiWindowAdapter.isInTopVideoList(topRunning.packageName) || (windowState = topRunning.findMainWindow()) == null)) {
            return false;
        }
        int windowFlags = PolicyControl.getWindowFlags(windowState, windowState.getAttrs());
        int systemUiFlags = PolicyControl.getSystemUiVisibility(null, windowState.getAttrs());
        MiuiCvwGestureController.Slog.d(TAG, "isLandscapeVideo : " + (windowFlags & 1024) + ", systemUiFlags :" + systemUiFlags);
        return (((windowFlags & 1024) == 0 && (systemUiFlags & 4) == 0) || (systemUiFlags & 2) == 0) ? false : true;
    }

    /* JADX WARN: Removed duplicated region for block: B:16:0x002a A[RETURN] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private boolean isGameApp(java.lang.String r6) {
        /*
            r5 = this;
            boolean r0 = android.util.MiuiMultiWindowAdapter.isInTopGameList(r6)
            r1 = 1
            if (r0 == 0) goto L8
            return r1
        L8:
            r0 = 0
            com.android.server.wm.MiuiCvwGestureController r2 = r5.mController     // Catch: java.lang.Exception -> L2c
            com.android.server.wm.WindowManagerService r2 = r2.mWmService     // Catch: java.lang.Exception -> L2c
            android.content.Context r2 = r2.mContext     // Catch: java.lang.Exception -> L2c
            android.content.pm.PackageManager r2 = r2.getPackageManager()     // Catch: java.lang.Exception -> L2c
            android.content.pm.ApplicationInfo r2 = r2.getApplicationInfo(r6, r0)     // Catch: java.lang.Exception -> L2c
            if (r2 == 0) goto L2b
            int r3 = r2.category     // Catch: java.lang.Exception -> L2c
            if (r3 == 0) goto L27
            int r3 = r2.flags     // Catch: java.lang.Exception -> L2c
            r4 = 33554432(0x2000000, float:9.403955E-38)
            r3 = r3 & r4
            if (r3 != r4) goto L25
            goto L27
        L25:
            r3 = r0
            goto L28
        L27:
            r3 = r1
        L28:
            if (r3 == 0) goto L2b
            return r1
        L2b:
            goto L2d
        L2c:
            r1 = move-exception
        L2d:
            return r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wm.MiuiCvwPolicy.isGameApp(java.lang.String):boolean");
    }

    public float getGuessRatio(TaskWrapperInfo taskinfo, float ratio) {
        return taskinfo.calculateGuessRawRatio(ratio);
    }

    float getMinRawRatio(TaskWrapperInfo taskinfo) {
        return taskinfo.getMinRawRatio();
    }

    float getMaxRawRatio(TaskWrapperInfo taskinfo) {
        return taskinfo.getMaxRawRatio();
    }

    public float getMinRawRatio() {
        return this.mSnapTargetPool.getMinRawRatio();
    }

    public float getMaxRawRatio() {
        return this.mSnapTargetPool.getMaxRawRatio();
    }

    float getFullScreenRawRatio() {
        return this.mSnapTargetPool.getDisplayRatio() / getRangeRatio();
    }

    float getRealWidthByRatio(float ratio) {
        MiuiCvwSnapTargetPool.SnapTarget target = this.mSnapTargetPool.getSnapTargetByRawRatio(ratio);
        if (target != null) {
            return target.getWidth();
        }
        MiuiCvwGestureController.Slog.d(TAG, "getRealWidthByRatio target is null ! ratio :" + ratio);
        return -1.0f;
    }

    float getRealHeightByRatio(float ratio) {
        MiuiCvwSnapTargetPool.SnapTarget target = this.mSnapTargetPool.getSnapTargetByRawRatio(ratio);
        if (target != null) {
            return target.getHeight();
        }
        MiuiCvwGestureController.Slog.d(TAG, "getRealHeightByRatio target is null ! ratio :" + ratio);
        return -1.0f;
    }

    public float getMaxWidthByRatio(float ratio) {
        MiuiCvwSnapTargetPool.SnapTarget target = this.mSnapTargetPool.getSnapTargetByRawRatio(ratio);
        if (target != null) {
            return target.getMaxWidth();
        }
        MiuiCvwGestureController.Slog.d(TAG, "getRealWidthByRatio target is null ! ratio :" + ratio);
        return -1.0f;
    }

    public float getMaxHeightByRatio(float ratio) {
        MiuiCvwSnapTargetPool.SnapTarget target = this.mSnapTargetPool.getSnapTargetByRawRatio(ratio);
        if (target != null) {
            return target.getMaxHeight();
        }
        MiuiCvwGestureController.Slog.d(TAG, "getRealHeightByRatio target is null ! ratio :" + ratio);
        return -1.0f;
    }

    float getMinWidthByRawRatio(float ratio) {
        MiuiCvwSnapTargetPool.SnapTarget target = this.mSnapTargetPool.getSnapTargetByRawRatio(ratio);
        if (target != null) {
            return target.getMinHeight();
        }
        MiuiCvwGestureController.Slog.d(TAG, "getMinWidthByRawRatio target is null ! ratio :" + ratio);
        return -1.0f;
    }

    public float getMaxHeight() {
        return this.mSnapTargetPool.getMaxHeight();
    }

    public float getMaxWidth() {
        return this.mSnapTargetPool.getMaxWidth();
    }

    public float getMinHeightByRawRatio(float ratio) {
        MiuiCvwSnapTargetPool.SnapTarget target = this.mSnapTargetPool.getSnapTargetByRawRatio(ratio);
        if (target != null) {
            return target.getMinHeight();
        }
        MiuiCvwGestureController.Slog.d(TAG, "getMinHeightByRawRatio target is null ! ratio :" + ratio);
        return -1.0f;
    }

    public MiuiCvwSnapTargetPool.SnapTarget getSnapTargetByRawRatio(float ratio) {
        return this.mSnapTargetPool.getSnapTargetByRawRatio(ratio);
    }

    public MiuiCvwSnapTargetPool.SnapTarget getSnapTargetByRatio(float ratio) {
        float rawRatio = ratio / getRangeRatio();
        return this.mSnapTargetPool.getSnapTargetByRawRatio(rawRatio);
    }

    public void updateRangeRatio(float ratio) {
        this.mSnapTargetPool.updateRangeRatio(ratio);
    }

    public float getRangeRatio() {
        return this.mSnapTargetPool.getRangeRatio();
    }

    public void updateTrackEventData(MiuiFreeFormActivityStack stack, float ratio, int animationType) {
        MiuiCvwGestureController.CvwTrackEvent trackEvent = this.mController.mTrackEvent;
        trackEvent.reset();
        MiuiCvwSnapTargetPool.SnapTarget target = this.mSnapTargetPool.getSnapTargetByRawRatio(ratio);
        if (stack != null) {
            trackEvent.originalStackRatio = stack.mStackRatio;
        }
        if (target != null) {
            float targetRatio = target.getRatio();
            if (targetRatio == this.mSnapTargetPool.getDisplayPortraitAspectRatio() || targetRatio == this.mSnapTargetPool.getDisplayHorizontalAspectRatio()) {
                trackEvent.targetStackRatio = MiuiFreeformTrackManager.SmallWindowTrackConstants.STACK_RATIO2;
            } else if (targetRatio == 1.3333334f) {
                trackEvent.targetStackRatio = MiuiFreeformTrackManager.SmallWindowTrackConstants.STACK_RATIO3;
            } else if (targetRatio == 0.75f) {
                trackEvent.targetStackRatio = MiuiFreeformTrackManager.SmallWindowTrackConstants.STACK_RATIO4;
            } else {
                trackEvent.targetStackRatio = MiuiFreeformTrackManager.SmallWindowTrackConstants.STACK_RATIO1;
            }
        }
        switch (animationType) {
            case 3:
                trackEvent.enterWay = MiuiFreeformTrackManager.SmallWindowTrackConstants.ENTER_WAY_NAME1;
                return;
            case 4:
                trackEvent.enterWay = MiuiFreeformTrackManager.SmallWindowTrackConstants.ENTER_WAY_NAME2;
                return;
            case 5:
            case 6:
            default:
                return;
            case 7:
                trackEvent.enterWay = MiuiFreeformTrackManager.MiniWindowTrackConstants.ENTER_WAY_NAME3;
                return;
            case 8:
                trackEvent.enterWay = MiuiFreeformTrackManager.MiniWindowTrackConstants.ENTER_WAY_NAME4;
                return;
            case 9:
                trackEvent.enterWay = MiuiFreeformTrackManager.MiniWindowTrackConstants.ENTER_WAY_NAME1;
                return;
            case 10:
                trackEvent.enterWay = MiuiFreeformTrackManager.MiniWindowTrackConstants.ENTER_WAY_NAME2;
                return;
        }
    }

    /* loaded from: classes.dex */
    public static class TaskWrapperInfo {
        public Rect actualBounds;
        public MiuiCvwSnapTargetPool.SnapTarget currentSnapTarget;
        public boolean isFixedRatio;
        public ArrayList<MiuiCvwSnapTargetPool.SnapTarget> mSupportTargets;
        public Rect miniBounds;
        public String packageName;
        public float scale;
        public MiuiFreeFormActivityStack stack;
        public boolean supportCvw;
        public Rect visualBounds;
        public float visualScale;

        public static TaskWrapperInfo create(MiuiFreeFormActivityStack stack, MiuiCvwPolicy policy) {
            return new TaskWrapperInfo(stack, policy);
        }

        public TaskWrapperInfo() {
            this.isFixedRatio = false;
            this.visualBounds = new Rect();
            this.actualBounds = new Rect();
            this.miniBounds = new Rect();
            this.mSupportTargets = new ArrayList<>();
        }

        public TaskWrapperInfo(MiuiFreeFormActivityStack stack, MiuiCvwPolicy policy) {
            boolean z = false;
            this.isFixedRatio = false;
            this.visualBounds = new Rect();
            this.actualBounds = new Rect();
            this.miniBounds = new Rect();
            this.mSupportTargets = new ArrayList<>();
            this.supportCvw = policy.mController.isTaskSupportCvw(stack.mTask);
            this.stack = stack;
            this.packageName = stack.getStackPackageName() != null ? stack.getStackPackageName() : "";
            boolean isGameOrVideoWindow = policy.isGameOrFullScreenVideoWindow(stack);
            boolean isLandscapeApp = isLandscapeApp(this.packageName, policy.mController.mWmService.mContext);
            boolean isPortraitApp = isPortraitApp(stack.mTask);
            boolean isNeedFixedRatio = isNeedFixedRatio(isLandscapeApp, isPortraitApp, policy);
            if (isGameOrVideoWindow) {
                this.isFixedRatio = (stack.isInFreeFormMode() || stack.isInMiniFreeFormMode() || isNeedFixedRatio) ? true : z;
            }
            if (stack.isInFreeFormMode()) {
                this.scale = stack.mFreeFormScale;
                this.visualScale = 1.0f;
                this.actualBounds.set(stack.mTask.getBounds());
                this.visualBounds.set(this.actualBounds.left, this.actualBounds.top, this.actualBounds.left + ((int) (this.actualBounds.width() * stack.mFreeFormScale)), this.actualBounds.top + ((int) (this.actualBounds.height() * stack.mFreeFormScale)));
                generateSnapTargetsInFreeform(isGameOrVideoWindow, policy);
                if (this.mSupportTargets.size() == 1) {
                    this.isFixedRatio = true;
                }
            } else if (stack.isInMiniFreeFormMode()) {
                this.actualBounds.set(stack.mTask.getBounds());
                this.visualBounds.set(stack.mStackControlInfo.mSmallWindowBounds);
                this.scale = stack.mFreeFormScale;
                this.visualScale = (stack.mStackControlInfo.mSmallWindowBounds.height() * 1.0f) / (this.scale * this.actualBounds.height());
                generateSnapTargetsInFreeform(isGameOrVideoWindow, policy);
                if (this.mSupportTargets.size() == 1) {
                    this.isFixedRatio = true;
                }
            } else {
                this.visualScale = 1.0f;
                this.visualBounds.set(policy.mDisplayBounds);
                this.actualBounds.set(policy.mDisplayBounds);
                this.scale = (this.visualBounds.width() * 1.0f) / policy.mDisplayBounds.width();
                generateSnapTargetsInOther(isGameOrVideoWindow, policy, isNeedFixedRatio, isLandscapeApp);
            }
        }

        private void generateSnapTargetsInFreeform(boolean isGameOrVideoWindow, MiuiCvwPolicy policy) {
            if (isGameOrVideoWindow) {
                if (this.actualBounds.width() > this.actualBounds.height()) {
                    this.currentSnapTarget = policy.mSnapTargetPool.getSnapTargetLandscapeFullScreen();
                } else {
                    this.currentSnapTarget = policy.mSnapTargetPool.getSnapTargetPortraitFullScreen();
                }
                this.mSupportTargets.add(this.currentSnapTarget);
                return;
            }
            float ratio = (this.stack.mTask.getBounds().width() * 1.0f) / this.stack.mTask.getBounds().height();
            float rawRatio = ratio / policy.mSnapTargetPool.getRangeRatio();
            this.currentSnapTarget = policy.mSnapTargetPool.getSnapTargetByRawRatio(rawRatio);
            ActivityRecord topRunning = this.stack.mTask.topRunningActivity();
            addSnapTargetRatio1(topRunning, policy);
            addSnapTargetRatio2(topRunning, policy);
            addSnapTargetRatio3(topRunning, policy);
            Collections.sort(this.mSupportTargets);
        }

        private void generateSnapTargetsInOther(boolean isGameOrVideoWindow, MiuiCvwPolicy policy, boolean isNeedFixedRatio, boolean isLandscapeApp) {
            if (isGameOrVideoWindow) {
                if (isNeedFixedRatio) {
                    this.currentSnapTarget = policy.mSnapTargetPool.getSnapTargetFullScreen();
                } else if (isLandscapeApp) {
                    this.currentSnapTarget = policy.mSnapTargetPool.mDisplayHorizontalSnapTarget;
                } else {
                    this.currentSnapTarget = policy.mSnapTargetPool.mDisplayVerticalSnapTarget;
                }
                this.mSupportTargets.add(this.currentSnapTarget);
                return;
            }
            ActivityRecord topRunning = this.stack.mTask.topRunningActivity();
            addSnapTargetRatio1(topRunning, policy);
            addSnapTargetRatio2(topRunning, policy);
            addSnapTargetRatio3(topRunning, policy);
            Collections.sort(this.mSupportTargets);
        }

        private void addSnapTargetRatio1(ActivityRecord activityRecord, MiuiCvwPolicy policy) {
            if (MiuiCvwAspectRatioConfig.existAppInConfig(this.packageName)) {
                if (MiuiCvwAspectRatioConfig.supportGivenRatioInConfig(this.packageName, 1)) {
                    this.mSupportTargets.add(policy.mSnapTargetPool.mXlargeGeneralSnapTarget);
                    return;
                } else if (MiuiCvwAspectRatioConfig.notSupportGivenRatioInConfig(this.packageName, 1)) {
                    return;
                }
            }
            if (activityRecord != null && MiuiMultiWindowUtils.isOrientationLandscape(activityRecord.mOrientation)) {
                return;
            }
            this.mSupportTargets.add(policy.mSnapTargetPool.mXlargeGeneralSnapTarget);
        }

        private void addSnapTargetRatio2(ActivityRecord activityRecord, MiuiCvwPolicy policy) {
            if (MiuiCvwAspectRatioConfig.existAppInConfig(this.packageName)) {
                if (MiuiCvwAspectRatioConfig.supportGivenRatioInConfig(this.packageName, 2)) {
                    this.mSupportTargets.add(policy.mSnapTargetPool.mXlargeWideSnapTarget);
                    return;
                } else if (MiuiCvwAspectRatioConfig.notSupportGivenRatioInConfig(this.packageName, 2)) {
                    return;
                }
            }
            if (activityRecord != null && MiuiMultiWindowUtils.isOrientationLandscape(activityRecord.mOrientation)) {
                return;
            }
            this.mSupportTargets.add(policy.mSnapTargetPool.mXlargeWideSnapTarget);
        }

        private void addSnapTargetRatio3(ActivityRecord activityRecord, MiuiCvwPolicy policy) {
            if (MiuiCvwAspectRatioConfig.existAppInConfig(this.packageName)) {
                if (MiuiCvwAspectRatioConfig.supportGivenRatioInConfig(this.packageName, 3)) {
                    this.mSupportTargets.add(policy.mSnapTargetPool.mLargeGeneralSnapTarget);
                    return;
                } else if (MiuiCvwAspectRatioConfig.notSupportGivenRatioInConfig(this.packageName, 3)) {
                    return;
                }
            }
            if (activityRecord != null && MiuiMultiWindowUtils.isOrientationPortrait(activityRecord.mOrientation)) {
                return;
            }
            this.mSupportTargets.add(policy.mSnapTargetPool.mLargeGeneralSnapTarget);
        }

        public static boolean isLandscapeApp(String pkg, Context context) {
            if (pkg == null) {
                return false;
            }
            Intent intent = new Intent("android.intent.action.MAIN");
            intent.addCategory("android.intent.category.LAUNCHER");
            intent.setPackage(pkg);
            boolean z = false;
            ResolveInfo rInfo = context.getPackageManager().resolveActivity(intent, 0);
            if (rInfo == null) {
                return false;
            }
            if (MiuiMultiWindowUtils.isOrientationLandscape(rInfo.activityInfo.screenOrientation) || MiuiMultiWindowAdapter.getForceLandscapeApplicationInSystem().contains(pkg)) {
                z = true;
            }
            boolean isLandscape = z;
            return isLandscape;
        }

        public static boolean isPortraitApp(Task task) {
            ActivityRecord topRunning;
            if (task != null && (topRunning = task.topRunningActivityLocked()) != null) {
                return MiuiMultiWindowUtils.isOrientationPortrait(topRunning.mOrientation);
            }
            return true;
        }

        public static boolean isNeedFixedRatio(boolean isLandscapeApp, boolean isPortraitApp, MiuiCvwPolicy policy) {
            if (isPortraitApp && policy.mSnapTargetPool.isLandscape()) {
                return false;
            }
            if (isLandscapeApp && !policy.mSnapTargetPool.isLandscape()) {
                return false;
            }
            return true;
        }

        public boolean isValid() {
            return (this.scale == MiuiFreeformPinManagerService.EDGE_AREA || this.actualBounds == null || this.miniBounds == null || this.visualBounds == null || this.mSupportTargets == null) ? false : true;
        }

        float calculateGuessRawRatio(float ratio) {
            int size = this.mSupportTargets.size();
            int targetIndex = size - 1;
            int i = 1;
            while (true) {
                if (i >= size) {
                    break;
                }
                MiuiCvwSnapTargetPool.SnapTarget preTarget = this.mSupportTargets.get(i - 1);
                MiuiCvwSnapTargetPool.SnapTarget curTarget = this.mSupportTargets.get(i);
                if (ratio <= (preTarget.getRawRatio() + curTarget.getRawRatio()) / 2.0f) {
                    targetIndex = i - 1;
                    break;
                } else if (ratio > curTarget.getRawRatio()) {
                    i++;
                } else {
                    targetIndex = i;
                    break;
                }
            }
            return this.mSupportTargets.get(targetIndex).getRawRatio();
        }

        MiuiCvwSnapTargetPool.SnapTarget getSnapTargetByRawRatio(float ratio) {
            Iterator<MiuiCvwSnapTargetPool.SnapTarget> it = this.mSupportTargets.iterator();
            while (it.hasNext()) {
                MiuiCvwSnapTargetPool.SnapTarget target = it.next();
                if (Math.abs(target.getRawRatio() - ratio) < 0.01d) {
                    return target;
                }
            }
            return null;
        }

        float getMinRawRatio() {
            return this.mSupportTargets.get(0).getRawRatio();
        }

        float getMaxRawRatio() {
            ArrayList<MiuiCvwSnapTargetPool.SnapTarget> arrayList = this.mSupportTargets;
            return arrayList.get(arrayList.size() - 1).getRawRatio();
        }

        public MiuiCvwSnapTargetPool.SnapTarget getCurrentSnapTarget() {
            return this.currentSnapTarget;
        }

        public void reset() {
            this.scale = MiuiFreeformPinManagerService.EDGE_AREA;
            this.stack = null;
            this.currentSnapTarget = null;
            this.mSupportTargets.clear();
        }

        public void dump(PrintWriter pw, String prefix) {
            String innerPrefix = prefix + "  ";
            pw.println("TaskWrapperInfo:");
            pw.print(innerPrefix);
            pw.println("visualBounds: " + this.visualBounds);
            pw.print(innerPrefix);
            pw.println("actualBounds:" + this.actualBounds);
            pw.print(innerPrefix);
            pw.println("miniBounds:" + this.miniBounds);
            pw.print(innerPrefix);
            pw.println("SupportTargets:");
            for (int i = 0; i < this.mSupportTargets.size(); i++) {
                pw.print(innerPrefix);
                pw.println("Target #" + i + ":" + this.mSupportTargets.get(i));
            }
        }

        public String toString() {
            return "TaskWrapperInfo:/\"  \"visualBounds:" + this.visualBounds + "/\"  \"actualBounds:" + this.actualBounds + "/\"  \"miniBounds:" + this.miniBounds;
        }
    }

    public void dump(PrintWriter pw, String prefix) {
        String innerPrefix = prefix + "  ";
        pw.println("MiuiCvwPolicy:");
        pw.print(innerPrefix);
        pw.println("HotAreaBounds:" + this.mHotAreaBounds.toString());
        pw.print(innerPrefix);
        pw.println("DisplayBounds:" + this.mDisplayBounds.toString());
        this.mSnapTargetPool.dump(pw, prefix);
        if (MiuiCvwGestureController.Slog.isDebugable()) {
            this.mAspectRatioConfig.dump(pw, prefix);
        }
    }
}
