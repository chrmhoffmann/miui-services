package com.android.server.wm;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.WindowConfiguration;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.HardwareBuffer;
import android.hardware.display.DisplayManagerGlobal;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.InputConstants;
import android.os.Process;
import android.os.SystemProperties;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.server.am.SplitScreenReporter;
import android.util.Log;
import android.util.MiuiMultiWinClientUtils;
import android.util.MiuiMultiWindowAdapter;
import android.util.MiuiMultiWindowUtils;
import android.util.Slog;
import android.view.Display;
import android.view.DisplayCutout;
import android.view.DragEvent;
import android.view.InputApplicationHandle;
import android.view.InputChannel;
import android.view.InputWindowHandle;
import android.view.LayoutInflater;
import android.view.SurfaceControl;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.window.TaskSnapshot;
import com.android.server.multiwin.MiuiMultiWinUtils;
import com.android.server.multiwin.animation.MiuiMultiWinDragAnimationAdapter;
import com.android.server.multiwin.animation.MiuiMultiWinSplitBarController;
import com.android.server.multiwin.animation.interpolator.SharpCurveInterpolator;
import com.android.server.multiwin.listener.DragAnimationListener;
import com.android.server.multiwin.listener.MiuiMultiWinHotAreaConfigListener;
import com.android.server.multiwin.view.MiuiMultiWinClipImageView;
import com.android.server.multiwin.view.MiuiMultiWinHotAreaView;
import com.android.server.multiwin.view.MiuiMultiWinPushAcceptView;
import com.android.server.multiwin.view.MiuiMultiWinSwapAcceptView;
import com.xiaomi.multiwin.MiuiMultiWinReflectStub;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import miui.app.MiuiFreeFormManager;
/* loaded from: classes.dex */
public class MiuiMultiWindowSwitchManager implements DragAnimationListener, MiuiMultiWinHotAreaConfigListener {
    private static final String ALPHA_PROPERTY_NAME = "alpha";
    private static boolean IS_FORCE_FSG_NAVBAR = false;
    private static boolean IS_HIDE_GESTURELINE = false;
    private static final int MAIN_STAGE = 0;
    private static final long SCREEN_SHOT_COVER_REMOVE_ANIM_DELAY = 50;
    private static final long SCREEN_SHOT_COVER_REMOVE_ANIM_DURATION = 100;
    private static final int SIDE_STAGE = 1;
    private static final String TAG = "MiuiMultiWindowSwitchManager";
    private int barSize;
    private boolean isDragAppSupportedFreeform;
    private boolean isFreeformBlackListAppShowToast;
    private IBinder mActivityToken;
    private ActivityTaskManagerService mAtms;
    private Rect mDefaultFreeformDropBound;
    private MiuiMultiWinDragAnimationAdapter mDragAnimationAdapter;
    private Handler mDragEndAnimHanlder;
    private Rect mDropTargetBound;
    private Rect mFreeFormDropBound;
    private ViewGroup mHotArea;
    private SurfaceControl mInputSurface;
    private MiuiMultiWindowInputInterceptor mInterceptor;
    private int mLastDropSplitMode;
    private MiuiMultiWinHotAreaView mLeftSplitScreenRegion;
    private Rect mNavBarBound;
    private MiuiMultiWinHotAreaView mRightSplitScreenRegion;
    private RelativeLayout mRootView;
    private ImageView mScreenShotCover;
    private ObjectAnimator mScreenShotCoverRemoveAnimator;
    private RelativeLayout mSplitBarContainer;
    private MiuiMultiWinSplitBarController mSplitBarController;
    private Context mUiContext;
    private WindowManager mWindowManager;
    private WindowManagerService mWms;
    private static volatile MiuiMultiWindowSwitchManager mInstance = null;
    private static boolean IS_CTS_MODE = false;
    private static float DEFAULT_BLURICON_ALPHA_RATIO = 0.3f;
    private static final Uri mForceFsgNavBarUri = Settings.Global.getUriFor("force_fsg_nav_bar");
    private static final Uri mHideGestureLineUri = Settings.Global.getUriFor("hide_gesture_line");
    private Rect mDisplayBound = new Rect();
    private boolean mHasDragStarted = false;
    private boolean mIsDragBarReset = true;
    private boolean mIsDropFailedCleanUp = false;
    private boolean mIsDropHandled = false;
    private volatile boolean mIsLeftRightSplit = false;
    private volatile int mForceSwitchLeftRightSplit = -1;
    private volatile boolean mIsProcessingDrag = false;
    private boolean mIsScreenSwitchHandled = false;
    private Point mLeftSplitScreenSize = new Point();
    private int mNavBarPos = -1;
    private Point mRightSplitScreenSize = new Point();
    private float mSplitFractionRatio = MiuiFreeformPinManagerService.EDGE_AREA;
    private boolean mTopRootTaskIsSplit = false;
    private float mSnapShotScaleFactor = 1.0f;
    private int mWindowingMode = 0;
    private float freeFormScale = 1.0f;
    private float mLeftRightRatio = 0.5f;

    /* loaded from: classes.dex */
    public final class MiuiMultiWindowInputInterceptor {
        InputApplicationHandle mApplicationHandle = new InputApplicationHandle(new Binder(), MiuiMultiWindowSwitchManager.TAG, InputConstants.DEFAULT_DISPATCHING_TIMEOUT_MILLIS);
        InputChannel mClientChannel;
        IBinder mToken;
        InputWindowHandle mWindowHandle;

        MiuiMultiWindowInputInterceptor(Display display) {
            MiuiMultiWindowSwitchManager.this = this$0;
            this.mClientChannel = this$0.mWms.mInputManager.createInputChannel(MiuiMultiWindowSwitchManager.TAG);
            this.mWindowHandle = new InputWindowHandle(this.mApplicationHandle, display.getDisplayId());
            this.mToken = this.mClientChannel.getToken();
            this.mWindowHandle.name = MiuiMultiWindowSwitchManager.TAG;
            this.mWindowHandle.token = this.mToken;
            this.mWindowHandle.layoutParamsFlags = 0;
            this.mWindowHandle.layoutParamsType = 2016;
            this.mWindowHandle.dispatchingTimeoutMillis = InputConstants.DEFAULT_DISPATCHING_TIMEOUT_MILLIS;
            this.mWindowHandle.ownerPid = Process.myPid();
            this.mWindowHandle.ownerUid = Process.myUid();
            this.mWindowHandle.scaleFactor = 1.0f;
            this.mWindowHandle.inputConfig = 4;
            this.mWindowHandle.frameLeft = 0;
            this.mWindowHandle.frameTop = 0;
            Point displaySize = new Point();
            display.getRealSize(displaySize);
            this.mWindowHandle.frameRight = displaySize.x;
            this.mWindowHandle.frameBottom = displaySize.y;
            this.mWindowHandle.touchableRegion.set(this.mWindowHandle.frameLeft, this.mWindowHandle.frameTop, this.mWindowHandle.frameRight, this.mWindowHandle.frameBottom);
        }

        void tearDown() {
            MiuiMultiWindowSwitchManager.this.mWms.mInputManager.removeInputChannel(this.mToken);
            this.mClientChannel.dispose();
            this.mClientChannel = null;
            this.mWindowHandle = null;
            this.mApplicationHandle = null;
        }
    }

    private MiuiMultiWindowSwitchManager(ActivityTaskManagerService mActivityTaskManagerService) {
        this.mAtms = mActivityTaskManagerService;
        this.mUiContext = mActivityTaskManagerService.mUiContext;
        this.mWms = this.mAtms.mWindowManager;
        registerObservers(this.mUiContext);
        HandlerThread handlerThread = new HandlerThread(TAG, 10);
        handlerThread.start();
        this.mDragEndAnimHanlder = new Handler(handlerThread.getLooper());
    }

    private void addScreenShotCover() {
        removeScreenShotCover();
        Bitmap screenShot = MiuiMultiWinUtils.takeScreenshot(0);
        ImageView imageView = new ImageView(this.mUiContext);
        this.mScreenShotCover = imageView;
        imageView.setOnClickListener(new View.OnClickListener() { // from class: com.android.server.wm.MiuiMultiWindowSwitchManager$$ExternalSyntheticLambda0
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                MiuiMultiWindowSwitchManager.this.m1784x210bf982(view);
            }
        });
        this.mScreenShotCover.setImageBitmap(screenShot);
        WindowManager.LayoutParams layoutParams = createBasicLayoutParams(0);
        layoutParams.flags |= 8;
        layoutParams.setTitle("MultiWindow - ScreenShotCover");
        this.mScreenShotCover.setLayoutParams(layoutParams);
        this.mWindowManager.addView(this.mScreenShotCover, layoutParams);
        Slog.d(TAG, "addScreenShotCover");
        this.mScreenShotCover.post(new Runnable() { // from class: com.android.server.wm.MiuiMultiWindowSwitchManager$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                MiuiMultiWindowSwitchManager.this.m1785x4ee493e1();
            }
        });
    }

    /* renamed from: lambda$addScreenShotCover$0$com-android-server-wm-MiuiMultiWindowSwitchManager */
    public /* synthetic */ void m1784x210bf982(View v) {
        removeScreenShotCover();
    }

    /* renamed from: lambda$addScreenShotCover$1$com-android-server-wm-MiuiMultiWindowSwitchManager */
    public /* synthetic */ void m1785x4ee493e1() {
        handleRemoveHotArea();
        removeScreenShotCoverWithAnimation(SCREEN_SHOT_COVER_REMOVE_ANIM_DELAY);
    }

    private void addSplitBar() {
        int swapMargin;
        int margin;
        float marginFraction = getSplitFractionBySplitRatio();
        int splitBarContainerWidth = this.mUiContext.getResources().getDimensionPixelSize(285671523);
        int height = -1;
        int width = isLandScape() ? splitBarContainerWidth : -1;
        if (!isLandScape()) {
            height = splitBarContainerWidth;
        }
        int swapMarginAdjustmentWithNavBar = getSplitBarSwapMarginAdjustmentWithNavBar();
        RelativeLayout.LayoutParams containerParams = new RelativeLayout.LayoutParams(width, height);
        int hotAreaWidth = this.mDisplayBound.width();
        int hotAreaHeight = this.mDisplayBound.height();
        if (WindowConfiguration.isMiuiFreeFormWindowingMode(this.mWindowingMode) && !this.mTopRootTaskIsSplit) {
            marginFraction = 0.5f;
            swapMarginAdjustmentWithNavBar = 0;
        }
        if (isLandScape()) {
            margin = ((int) (hotAreaWidth * marginFraction)) - (splitBarContainerWidth / 2);
            float rightOrBottomWidth = hotAreaWidth * (1.0f - marginFraction);
            swapMargin = (((int) rightOrBottomWidth) - swapMarginAdjustmentWithNavBar) - (splitBarContainerWidth / 2);
            containerParams.leftMargin = margin;
        } else {
            margin = ((int) (hotAreaHeight * marginFraction)) - (splitBarContainerWidth / 2);
            float rightOrBottomHeight = hotAreaHeight * (1.0f - marginFraction);
            swapMargin = (((int) rightOrBottomHeight) - swapMarginAdjustmentWithNavBar) - (splitBarContainerWidth / 2);
            containerParams.topMargin = margin;
        }
        RelativeLayout relativeLayout = new RelativeLayout(this.mUiContext);
        this.mSplitBarContainer = relativeLayout;
        relativeLayout.setLayoutParams(containerParams);
        this.mSplitBarContainer.setBackgroundColor(-16777216);
        ImageView splitBar = new ImageView(this.mUiContext);
        splitBar.setImageDrawable(this.mUiContext.getDrawable(285737276));
        int splitBarWidth = this.mUiContext.getResources().getDimensionPixelSize(285671527);
        int splitBarHeight = this.mUiContext.getResources().getDimensionPixelSize(285671526);
        int w = isLandScape() ? splitBarWidth : splitBarHeight;
        int h = isLandScape() ? splitBarHeight : splitBarWidth;
        RelativeLayout.LayoutParams splitBarParams = new RelativeLayout.LayoutParams(w, h);
        splitBarParams.addRule(13);
        splitBar.setLayoutParams(splitBarParams);
        this.mSplitBarContainer.addView(splitBar);
        this.mHotArea.addView(this.mSplitBarContainer);
        MiuiMultiWinSplitBarController miuiMultiWinSplitBarController = new MiuiMultiWinSplitBarController(this.mSplitBarContainer, isLandScape());
        this.mSplitBarController = miuiMultiWinSplitBarController;
        miuiMultiWinSplitBarController.setMargins(margin, swapMargin);
        this.mSplitBarController.hideSplitBar();
        this.mHotArea.findViewById(285868151).bringToFront();
        this.mHotArea.findViewById(285868136).bringToFront();
    }

    private void adjustHotAreaBySplitRatio(MiuiMultiWinHotAreaView leftSplitScreenRegion, MiuiMultiWinHotAreaView rightSplitScreenRegion) {
        LinearLayout.LayoutParams leftParams = (LinearLayout.LayoutParams) leftSplitScreenRegion.getLayoutParams();
        LinearLayout.LayoutParams rightParams = (LinearLayout.LayoutParams) rightSplitScreenRegion.getLayoutParams();
        if (isLandScape()) {
            this.barSize = (this.mDisplayBound.width() - this.mLeftSplitScreenSize.x) - this.mRightSplitScreenSize.x;
            this.mSplitFractionRatio = (this.mLeftSplitScreenSize.x + (this.barSize / 2.0f)) / this.mDisplayBound.width();
            this.mLeftRightRatio = this.mLeftSplitScreenSize.x / (this.mDisplayBound.width() - this.barSize);
        } else {
            this.barSize = (this.mDisplayBound.height() - this.mLeftSplitScreenSize.y) - this.mRightSplitScreenSize.y;
            this.mSplitFractionRatio = (this.mLeftSplitScreenSize.y + (this.barSize / 2.0f)) / this.mDisplayBound.height();
            this.mLeftRightRatio = this.mLeftSplitScreenSize.y / (this.mDisplayBound.height() - this.barSize);
        }
        leftParams.weight = 1.0f - this.mSplitFractionRatio;
        rightParams.weight = this.mSplitFractionRatio;
    }

    public void cleanUpInputSurface() {
        if (this.mInputSurface != null) {
            SurfaceControl.Transaction transaction = new SurfaceControl.Transaction();
            transaction.remove(this.mInputSurface).apply();
            transaction.close();
            this.mInputSurface = null;
        }
        MiuiMultiWindowInputInterceptor miuiMultiWindowInputInterceptor = this.mInterceptor;
        if (miuiMultiWindowInputInterceptor != null) {
            miuiMultiWindowInputInterceptor.tearDown();
            this.mInterceptor = null;
        }
    }

    private void configBaseRegions(List<Bitmap> bitmapList) {
        this.mLeftSplitScreenRegion = (MiuiMultiWinHotAreaView) this.mHotArea.findViewById(285868138);
        this.mRightSplitScreenRegion = (MiuiMultiWinHotAreaView) this.mHotArea.findViewById(285868150);
        if (isLandScape()) {
            this.mLeftSplitScreenRegion.setSplitMode(1);
            this.mRightSplitScreenRegion.setSplitMode(2);
        } else {
            this.mLeftSplitScreenRegion.setSplitMode(3);
            this.mRightSplitScreenRegion.setSplitMode(4);
        }
        if (WindowConfiguration.isMiuiFreeFormWindowingMode(this.mWindowingMode)) {
            this.mLeftSplitScreenRegion.setForeground(new ColorDrawable(this.mUiContext.getResources().getColor(285605954)));
            this.mLeftSplitScreenRegion.initSplitScaleAnimation();
            this.mRightSplitScreenRegion.setForeground(new ColorDrawable(this.mUiContext.getResources().getColor(285605954)));
            this.mRightSplitScreenRegion.initSplitScaleAnimation();
            this.mLeftSplitScreenRegion.setSnapShotBitmap(bitmapList.get(0));
            Drawable mLeftBgDrawable = new ColorDrawable(this.mUiContext.getResources().getColor(285605970));
            this.mLeftSplitScreenRegion.setBgDrawable(mLeftBgDrawable);
            if (bitmapList.size() == 2) {
                this.mRightSplitScreenRegion.setSnapShotBitmap(bitmapList.get(1));
                Drawable mRightBgDrawable = new ColorDrawable(this.mUiContext.getResources().getColor(285605970));
                this.mRightSplitScreenRegion.setBgDrawable(mRightBgDrawable);
            }
        } else {
            if (!isDragPrimary()) {
                this.mLeftSplitScreenRegion.setSnapShotBitmap(bitmapList.get(0));
                Drawable mLeftBgDrawable2 = new ColorDrawable(this.mUiContext.getResources().getColor(285605970));
                this.mLeftSplitScreenRegion.setBgDrawable(mLeftBgDrawable2);
            }
            if (bitmapList.size() > 1 && !isDragSecondary()) {
                this.mRightSplitScreenRegion.setSnapShotBitmap(bitmapList.get(1));
                Drawable mRightBgDrawable2 = new ColorDrawable(this.mUiContext.getResources().getColor(285605970));
                this.mRightSplitScreenRegion.setBgDrawable(mRightBgDrawable2);
            }
        }
        this.mLeftSplitScreenRegion.setDragAnimationListener(this);
        this.mRightSplitScreenRegion.setDragAnimationListener(this);
        MiuiMultiWinHotAreaView freeFormScreenRegion = (MiuiMultiWinHotAreaView) this.mHotArea.findViewById(285868136);
        adjustHotAreaBySplitRatio(this.mLeftSplitScreenRegion, this.mRightSplitScreenRegion);
        setMarginForSplitBar(this.mLeftSplitScreenRegion, this.mRightSplitScreenRegion);
        addSplitBar();
        freeFormScreenRegion.setDragAnimationListener(this);
        freeFormScreenRegion.setSplitMode(5);
        freeFormScreenRegion.disableBorder();
    }

    private void configHotArea(int mode, IBinder activityToken, MiuiMultiWinHotAreaView leftSplitScreenRegion, MiuiMultiWinHotAreaView rightSplitScreenRegion, List bitmapList) {
        int i = 2;
        int i2 = 1;
        if (WindowConfiguration.isFreeFormWindowingMode(mode)) {
            if (bitmapList.size() == 1) {
                configHotAreaForPushFull(leftSplitScreenRegion, rightSplitScreenRegion);
            } else if (bitmapList.size() == 2) {
                leftSplitScreenRegion.setNavBarInfo(this.mNavBarBound, this.mNavBarPos);
                leftSplitScreenRegion.setIsLandScape(isLandScape());
                rightSplitScreenRegion.setNavBarInfo(this.mNavBarBound, this.mNavBarPos);
                rightSplitScreenRegion.setIsLandScape(isLandScape());
                MiuiMultiWinHotAreaView freeformRegion = (MiuiMultiWinHotAreaView) this.mHotArea.findViewById(285868136);
                freeformRegion.setPendingDrpoViewFlag(true);
            }
            Slog.d(TAG, activityToken + " is in free form window mode.");
        } else if (WindowConfiguration.isSplitScreenPrimaryWindowingMode(mode)) {
            if (!isLandScape()) {
                i = 4;
            }
            int dragSplitMode = i;
            rightSplitScreenRegion.setDragSplitMode(dragSplitMode);
            rightSplitScreenRegion.setInitialDragSplitMode(dragSplitMode);
            List icons = getTopAppIcons();
            if (icons != null && icons.size() > 1) {
                rightSplitScreenRegion.setIcon(icons.get(1));
                Bitmap iconBitmap = MiuiMultiWinUtils.drawable2Bitmap(icons.get(1));
                Bitmap radiatedBitamp = MiuiMultiWinUtils.getRadiatedBitmap(iconBitmap, 60, 20);
                Bitmap blurRadiatedBitmap = MiuiMultiWinUtils.rsBlurNoScale(mInstance.mUiContext, radiatedBitamp, 25);
                Bitmap blurAlphaRadiatedBitmap = MiuiMultiWinUtils.changeBitmapAlpha(blurRadiatedBitmap, (int) (DEFAULT_BLURICON_ALPHA_RATIO * 255.0f));
                rightSplitScreenRegion.setBlurRadiatedIconBitmap(blurAlphaRadiatedBitmap);
            }
            configHotAreaForSwapSplit(leftSplitScreenRegion, rightSplitScreenRegion);
            Slog.d(TAG, activityToken + " is in primary window mode.");
        } else if (WindowConfiguration.isSplitScreenSecondaryWindowingMode(mode)) {
            if (!isLandScape()) {
                i2 = 3;
            }
            int dragSplitMode2 = i2;
            leftSplitScreenRegion.setDragSplitMode(dragSplitMode2);
            leftSplitScreenRegion.setInitialDragSplitMode(dragSplitMode2);
            List appIcons = getTopAppIcons();
            if (appIcons != null && appIcons.size() > 0) {
                leftSplitScreenRegion.setIcon(appIcons.get(0));
                Bitmap iconBitmap2 = MiuiMultiWinUtils.drawable2Bitmap(appIcons.get(0));
                Bitmap radiatedBitamp2 = MiuiMultiWinUtils.getRadiatedBitmap(iconBitmap2, 60, 20);
                Bitmap blurRadiatedBitmap2 = MiuiMultiWinUtils.rsBlurNoScale(mInstance.mUiContext, radiatedBitamp2, 25);
                Bitmap blurAlphaRadiatedBitmap2 = MiuiMultiWinUtils.changeBitmapAlpha(blurRadiatedBitmap2, (int) (DEFAULT_BLURICON_ALPHA_RATIO * 255.0f));
                leftSplitScreenRegion.setBlurRadiatedIconBitmap(blurAlphaRadiatedBitmap2);
            }
            configHotAreaForSwapSplit(rightSplitScreenRegion, leftSplitScreenRegion);
            Slog.d(TAG, activityToken + " is in secondary window mode.");
        } else {
            Slog.d(TAG, activityToken + " is in undefined window mode.");
        }
    }

    private void configHotAreaForPushFull(MiuiMultiWinHotAreaView leftSplitScreenRegion, MiuiMultiWinHotAreaView rightSplitRegion) {
        int height = -1;
        int width = isLandScape() ? 0 : -1;
        if (!isLandScape()) {
            height = 0;
        }
        leftSplitScreenRegion.setLayoutParams(new LinearLayout.LayoutParams(width, height, 1.0f));
        rightSplitRegion.setLayoutParams(new LinearLayout.LayoutParams(width, height, MiuiFreeformPinManagerService.EDGE_AREA));
        leftSplitScreenRegion.setIsLandScape(isLandScape());
        List icons = getTopAppIcons();
        if (icons != null && icons.size() > 0) {
            leftSplitScreenRegion.setIcon(getTopAppIcons().get(0));
            Bitmap iconBitmap = MiuiMultiWinUtils.drawable2Bitmap(getTopAppIcons().get(0));
            Bitmap radiatedBitamp = MiuiMultiWinUtils.getRadiatedBitmap(iconBitmap, 60, 20);
            Bitmap blurRadiatedBitmap = MiuiMultiWinUtils.rsBlurNoScale(mInstance.mUiContext, radiatedBitamp, 25);
            Bitmap blurAlphaRadiatedBitmap = MiuiMultiWinUtils.changeBitmapAlpha(blurRadiatedBitmap, (int) (DEFAULT_BLURICON_ALPHA_RATIO * 255.0f));
            leftSplitScreenRegion.setBlurRadiatedIconBitmap(blurAlphaRadiatedBitmap);
        }
        LinearLayout pushFullHotArea = createPushFullHotArea(leftSplitScreenRegion);
        pushFullHotArea.setContentDescription("pushFullHotArea");
        ViewGroup leftRightLayout = (ViewGroup) leftSplitScreenRegion.getParent();
        this.mHotArea.addView(pushFullHotArea);
        leftRightLayout.bringToFront();
        pushFullHotArea.bringToFront();
    }

    private void configHotAreaForSwapSplit(MiuiMultiWinHotAreaView dropTarget, MiuiMultiWinHotAreaView swapTarget) {
        int rightOrBottomSwapHeight;
        int leftOrTopSwapHeight;
        swapTarget.setIsLandScape(isLandScape());
        dropTarget.disableBorder();
        boolean isDragPrimary = WindowConfiguration.isSplitScreenPrimaryWindowingMode(this.mWindowingMode);
        this.isDragAppSupportedFreeform = isSupportedFreeform(this.mActivityToken);
        MiuiMultiWinSwapAcceptView.Transformation transformation = new MiuiMultiWinSwapAcceptView.Transformation();
        MiuiMultiWinSwapAcceptView leftOrTopSwapAcceptView = new MiuiMultiWinSwapAcceptView(this.mUiContext, isLandScape() ? 1 : 3);
        leftOrTopSwapAcceptView.setAppSupportedFreeform(this.isDragAppSupportedFreeform);
        leftOrTopSwapAcceptView.setContentDescription("leftOrTopSwapAcceptView");
        leftOrTopSwapAcceptView.setIsHideNavBar(IS_FORCE_FSG_NAVBAR && IS_HIDE_GESTURELINE);
        leftOrTopSwapAcceptView.setDragPrimary(isDragPrimary);
        configLeftOrTopHotAreaForSwapSplit(leftOrTopSwapAcceptView, dropTarget, swapTarget, transformation);
        MiuiMultiWinSwapAcceptView rightOrBottomSwapAcceptView = new MiuiMultiWinSwapAcceptView(this.mUiContext, isLandScape() ? 2 : 4);
        rightOrBottomSwapAcceptView.setAppSupportedFreeform(this.isDragAppSupportedFreeform);
        rightOrBottomSwapAcceptView.setContentDescription("rightOrBottomSwapAcceptView");
        rightOrBottomSwapAcceptView.setIsHideNavBar(IS_FORCE_FSG_NAVBAR && IS_HIDE_GESTURELINE);
        rightOrBottomSwapAcceptView.setDragPrimary(isDragPrimary);
        configRightOrBottomHotAreaForSwapSplit(rightOrBottomSwapAcceptView, dropTarget, swapTarget, transformation);
        leftOrTopSwapAcceptView.setOtherSplitSwapAcceptView(rightOrBottomSwapAcceptView);
        rightOrBottomSwapAcceptView.setOtherSplitSwapAcceptView(leftOrTopSwapAcceptView);
        LinearLayout hotAreaForDragSplit = createHotAreaForDragSplit();
        hotAreaForDragSplit.setContentDescription("hotAreaForDragSplit");
        LinearLayout splitSwapHotArea = createSplitSwapHotArea();
        splitSwapHotArea.setContentDescription("splitSwapHotArea");
        MiuiMultiWinSwapAcceptView freeFormSwapAcceptView = new MiuiMultiWinSwapAcceptView(this.mUiContext, 5);
        freeFormSwapAcceptView.setIsLandScape(isLandScape());
        freeFormSwapAcceptView.setHotAreaLayout(this.mHotArea);
        freeFormSwapAcceptView.setNavBarInfo(this.mNavBarBound, this.mNavBarPos);
        freeFormSwapAcceptView.initSplitSwapAnimation(swapTarget, transformation);
        freeFormSwapAcceptView.setDragAnimationListener(this);
        freeFormSwapAcceptView.setSplitBarController(this.mSplitBarController);
        freeFormSwapAcceptView.setContentDescription("freeFormSwapAcceptView");
        if (!isLandScape()) {
            float baseRatio = ((this.mDisplayBound.height() - this.barSize) / 2.0f) / 456.0f;
            int leftOrTopSwapHeight2 = (int) (128.0f * baseRatio);
            Slog.e("mLeftRightRatio", this.mLeftRightRatio + "");
            Slog.e("mSplitFractionRatio", this.mSplitFractionRatio + "");
            if (this.mLeftRightRatio < 0.4f) {
                if (isDragPrimary) {
                    leftOrTopSwapHeight = leftOrTopSwapHeight2;
                } else {
                    leftOrTopSwapHeight = (int) (83.0f * baseRatio);
                }
                rightOrBottomSwapHeight = (int) ((((this.mDisplayBound.height() - this.barSize) * 2.0f) / 3.0f) + 24.0f);
            } else if (isDragPrimary) {
                rightOrBottomSwapHeight = (int) (((this.mDisplayBound.height() - this.barSize) / 3.0f) - 24.0f);
                leftOrTopSwapHeight = leftOrTopSwapHeight2;
            } else {
                rightOrBottomSwapHeight = (int) (((this.mDisplayBound.height() - this.barSize) * (1.0f - this.mLeftRightRatio)) + 24.0f);
                leftOrTopSwapHeight = leftOrTopSwapHeight2;
            }
            LinearLayout.LayoutParams leftOrTopLayoutParams = new LinearLayout.LayoutParams(-1, leftOrTopSwapHeight);
            LinearLayout.LayoutParams freeFormLayoutParams = new LinearLayout.LayoutParams(-1, 0);
            freeFormLayoutParams.weight = 1.0f;
            LinearLayout.LayoutParams rightOrBottomLayoutParams = new LinearLayout.LayoutParams(-1, rightOrBottomSwapHeight);
            leftOrTopSwapAcceptView.setLayoutParams(leftOrTopLayoutParams);
            freeFormSwapAcceptView.setLayoutParams(freeFormLayoutParams);
            rightOrBottomSwapAcceptView.setLayoutParams(rightOrBottomLayoutParams);
            hotAreaForDragSplit.addView(leftOrTopSwapAcceptView);
            hotAreaForDragSplit.addView(freeFormSwapAcceptView);
            hotAreaForDragSplit.addView(rightOrBottomSwapAcceptView);
            this.mHotArea.addView(hotAreaForDragSplit);
        } else {
            leftOrTopSwapAcceptView.setLayoutParams(new LinearLayout.LayoutParams(0, -1, 1.0f));
            rightOrBottomSwapAcceptView.setLayoutParams(new LinearLayout.LayoutParams(0, -1, 1.0f));
            int freeFormSwapHotAreaWidth = (int) ((this.mDisplayBound.width() * 4.0f) / 5.0f);
            int freeFormSwapHotAreaHeight = (int) ((this.mDisplayBound.height() * 5.0f) / 6.0f);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(freeFormSwapHotAreaWidth, freeFormSwapHotAreaHeight);
            params.addRule(14);
            params.addRule(12);
            freeFormSwapAcceptView.setLayoutParams(params);
            splitSwapHotArea.addView(leftOrTopSwapAcceptView, 0);
            splitSwapHotArea.addView(rightOrBottomSwapAcceptView, 1);
            hotAreaForDragSplit.addView(splitSwapHotArea);
            this.mHotArea.addView(hotAreaForDragSplit);
            this.mHotArea.addView(freeFormSwapAcceptView);
        }
        if (!this.isDragAppSupportedFreeform) {
            configNotSupportedFreeformArea(freeFormSwapAcceptView, leftOrTopSwapAcceptView, rightOrBottomSwapAcceptView, hotAreaForDragSplit);
        }
        measureDropBoundForSwapSplit(freeFormSwapAcceptView, leftOrTopSwapAcceptView, rightOrBottomSwapAcceptView, dropTarget, swapTarget, isDragPrimary);
    }

    private void configNotSupportedFreeformArea(final MiuiMultiWinSwapAcceptView freeFormSwapAcceptView, final MiuiMultiWinSwapAcceptView leftOrTopSwapAcceptView, final MiuiMultiWinSwapAcceptView rightOrBottomSwapAcceptView, final LinearLayout hotAreaForDragSplit) {
        freeFormSwapAcceptView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() { // from class: com.android.server.wm.MiuiMultiWindowSwitchManager.1
            @Override // android.view.ViewTreeObserver.OnGlobalLayoutListener
            public void onGlobalLayout() {
                freeFormSwapAcceptView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                RectF freeformBound = new RectF(freeFormSwapAcceptView.getLeft(), freeFormSwapAcceptView.getTop(), freeFormSwapAcceptView.getRight(), freeFormSwapAcceptView.getBottom());
                leftOrTopSwapAcceptView.setFreeFormBound(freeformBound);
                rightOrBottomSwapAcceptView.setFreeFormBound(freeformBound);
                if (!MiuiMultiWindowSwitchManager.this.isLandScape()) {
                    RelativeLayout.LayoutParams leftOrTopLayoutParams = new RelativeLayout.LayoutParams(leftOrTopSwapAcceptView.getWidth(), (int) (leftOrTopSwapAcceptView.getHeight() + freeformBound.height()));
                    RelativeLayout.LayoutParams rightOrBottomLayoutParams = new RelativeLayout.LayoutParams(rightOrBottomSwapAcceptView.getWidth(), (int) (rightOrBottomSwapAcceptView.getHeight() + freeformBound.height()));
                    rightOrBottomLayoutParams.addRule(12);
                    hotAreaForDragSplit.removeView(leftOrTopSwapAcceptView);
                    hotAreaForDragSplit.removeView(freeFormSwapAcceptView);
                    hotAreaForDragSplit.removeView(rightOrBottomSwapAcceptView);
                    if (leftOrTopSwapAcceptView.getParent() != null) {
                        ((ViewGroup) leftOrTopSwapAcceptView.getParent()).removeView(leftOrTopSwapAcceptView);
                    }
                    if (freeFormSwapAcceptView.getParent() != null) {
                        ((ViewGroup) freeFormSwapAcceptView.getParent()).removeView(freeFormSwapAcceptView);
                    }
                    if (rightOrBottomSwapAcceptView.getParent() != null) {
                        ((ViewGroup) rightOrBottomSwapAcceptView.getParent()).removeView(rightOrBottomSwapAcceptView);
                    }
                    if (MiuiMultiWindowSwitchManager.this.mHotArea != null) {
                        MiuiMultiWindowSwitchManager.this.mHotArea.removeView(hotAreaForDragSplit);
                    }
                    leftOrTopSwapAcceptView.setLayoutParams(leftOrTopLayoutParams);
                    rightOrBottomSwapAcceptView.setLayoutParams(rightOrBottomLayoutParams);
                    leftOrTopSwapAcceptView.setNeedToBringFront(true);
                    rightOrBottomSwapAcceptView.setNeedToBringFront(true);
                    RelativeLayout notSupportFreeformHotAreaContainer = new RelativeLayout(MiuiMultiWindowSwitchManager.this.mUiContext);
                    notSupportFreeformHotAreaContainer.addView(leftOrTopSwapAcceptView);
                    notSupportFreeformHotAreaContainer.addView(rightOrBottomSwapAcceptView);
                    notSupportFreeformHotAreaContainer.setLayoutParams(new ViewGroup.LayoutParams(-1, -1));
                    if (MiuiMultiWindowSwitchManager.this.mHotArea != null) {
                        MiuiMultiWindowSwitchManager.this.mHotArea.addView(notSupportFreeformHotAreaContainer);
                    }
                }
                if (freeFormSwapAcceptView.getParent() != null) {
                    ((ViewGroup) freeFormSwapAcceptView.getParent()).removeView(freeFormSwapAcceptView);
                }
            }
        });
    }

    private void measureDropBoundForSwapSplit(final MiuiMultiWinSwapAcceptView freeFormSwapAcceptView, final MiuiMultiWinSwapAcceptView leftOrTopSwapAcceptView, final MiuiMultiWinSwapAcceptView rightOrBottomSwapAcceptView, final MiuiMultiWinHotAreaView dropTarget, MiuiMultiWinHotAreaView swapTarget, final boolean isDragPrimary) {
        final LinearLayout tempOutContainer = new LinearLayout(this.mUiContext);
        tempOutContainer.setOrientation(1);
        tempOutContainer.setLayoutParams(new ViewGroup.LayoutParams(-1, -1));
        final LinearLayout tempReverseLeftRightContainer = new LinearLayout(this.mUiContext);
        tempReverseLeftRightContainer.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1.0f));
        tempReverseLeftRightContainer.setOrientation(((LinearLayout) dropTarget.getParent()).getOrientation());
        final MiuiMultiWinHotAreaView reversedDropTargetView = new MiuiMultiWinHotAreaView(this.mUiContext);
        MiuiMultiWinHotAreaView reversedswapTargetView = new MiuiMultiWinHotAreaView(this.mUiContext);
        final MiuiMultiWinHotAreaView freeformTargetView = new MiuiMultiWinHotAreaView(this.mUiContext);
        freeformTargetView.setLayoutParams(new RelativeLayout.LayoutParams(-1, -1));
        LinearLayout.LayoutParams reservedDropTargetParams = new LinearLayout.LayoutParams((LinearLayout.LayoutParams) dropTarget.getLayoutParams());
        LinearLayout.LayoutParams reservedSwapTargetParams = new LinearLayout.LayoutParams((LinearLayout.LayoutParams) swapTarget.getLayoutParams());
        if (isLandScape()) {
            if (isDragPrimary) {
                reservedDropTargetParams.leftMargin = ((LinearLayout.LayoutParams) dropTarget.getLayoutParams()).rightMargin;
                reservedDropTargetParams.rightMargin = 0;
                reservedSwapTargetParams.rightMargin = ((LinearLayout.LayoutParams) swapTarget.getLayoutParams()).leftMargin;
                reservedSwapTargetParams.leftMargin = 0;
            } else {
                reservedDropTargetParams.rightMargin = ((LinearLayout.LayoutParams) dropTarget.getLayoutParams()).leftMargin;
                reservedDropTargetParams.leftMargin = 0;
                reservedSwapTargetParams.leftMargin = ((LinearLayout.LayoutParams) swapTarget.getLayoutParams()).rightMargin;
                reservedSwapTargetParams.rightMargin = 0;
            }
        } else if (isDragPrimary) {
            reservedDropTargetParams.topMargin = ((LinearLayout.LayoutParams) dropTarget.getLayoutParams()).bottomMargin;
            reservedDropTargetParams.bottomMargin = 0;
            reservedSwapTargetParams.bottomMargin = ((LinearLayout.LayoutParams) swapTarget.getLayoutParams()).topMargin;
            reservedSwapTargetParams.topMargin = 0;
        } else {
            reservedDropTargetParams.bottomMargin = ((LinearLayout.LayoutParams) dropTarget.getLayoutParams()).topMargin;
            reservedDropTargetParams.topMargin = 0;
            reservedSwapTargetParams.topMargin = ((LinearLayout.LayoutParams) swapTarget.getLayoutParams()).bottomMargin;
            reservedSwapTargetParams.bottomMargin = 0;
        }
        reversedDropTargetView.setLayoutParams(reservedDropTargetParams);
        reversedDropTargetView.setSplitMode(swapTarget.getSplitMode());
        reversedswapTargetView.setLayoutParams(reservedSwapTargetParams);
        reversedswapTargetView.setSplitMode(dropTarget.getSplitMode());
        if (isDragPrimary) {
            tempReverseLeftRightContainer.addView(reversedswapTargetView);
            tempReverseLeftRightContainer.addView(reversedDropTargetView);
        } else {
            tempReverseLeftRightContainer.addView(reversedDropTargetView);
            tempReverseLeftRightContainer.addView(reversedswapTargetView);
        }
        tempOutContainer.addView(tempReverseLeftRightContainer);
        this.mHotArea.addView(tempOutContainer);
        this.mHotArea.addView(freeformTargetView);
        tempReverseLeftRightContainer.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() { // from class: com.android.server.wm.MiuiMultiWindowSwitchManager.2
            @Override // android.view.ViewTreeObserver.OnGlobalLayoutListener
            public void onGlobalLayout() {
                tempReverseLeftRightContainer.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                Rect dropTargetBound = new Rect(dropTarget.getLeft(), dropTarget.getTop(), dropTarget.getRight(), dropTarget.getBottom());
                Rect reservedDropTargetBound = new Rect(reversedDropTargetView.getLeft(), reversedDropTargetView.getTop(), reversedDropTargetView.getRight(), reversedDropTargetView.getBottom());
                Rect freeFormTargetBound = new Rect(freeformTargetView.getLeft(), freeformTargetView.getTop(), freeformTargetView.getRight(), freeformTargetView.getBottom());
                if ((!MiuiMultiWindowSwitchManager.IS_FORCE_FSG_NAVBAR || !MiuiMultiWindowSwitchManager.IS_HIDE_GESTURELINE) && MiuiMultiWindowSwitchManager.this.mNavBarPos == 4 && MiuiMultiWindowSwitchManager.this.isLandScape()) {
                    dropTargetBound.bottom -= MiuiMultiWindowSwitchManager.this.mNavBarBound.height();
                    reservedDropTargetBound.bottom -= MiuiMultiWindowSwitchManager.this.mNavBarBound.height();
                }
                leftOrTopSwapAcceptView.setDropTargetBound(isDragPrimary ? dropTargetBound : reservedDropTargetBound);
                leftOrTopSwapAcceptView.setDropLoc((isDragPrimary ? dropTarget : reversedDropTargetView).getDragEndLocation());
                rightOrBottomSwapAcceptView.setDropTargetBound(isDragPrimary ? reservedDropTargetBound : dropTargetBound);
                rightOrBottomSwapAcceptView.setDropLoc((isDragPrimary ? reversedDropTargetView : dropTarget).getDragEndLocation());
                if (MiuiMultiWindowSwitchManager.this.isDragAppSupportedFreeform) {
                    freeFormSwapAcceptView.setDropTargetBound(freeFormTargetBound);
                    freeFormSwapAcceptView.setDropLoc(freeformTargetView.getDragEndLocation());
                }
                if (MiuiMultiWindowSwitchManager.this.mHotArea != null) {
                    MiuiMultiWindowSwitchManager.this.mHotArea.removeView(freeformTargetView);
                    MiuiMultiWindowSwitchManager.this.mHotArea.removeView(tempOutContainer);
                }
            }
        });
    }

    private void configLeftOrTopHotAreaForSwapSplit(MiuiMultiWinSwapAcceptView leftOrTopSwapAcceptView, MiuiMultiWinHotAreaView dropTarget, MiuiMultiWinHotAreaView swapTarget, MiuiMultiWinSwapAcceptView.Transformation transformation) {
        leftOrTopSwapAcceptView.setIsLandScape(isLandScape());
        leftOrTopSwapAcceptView.setHotAreaLayout(this.mHotArea);
        leftOrTopSwapAcceptView.setNavBarInfo(this.mNavBarBound, this.mNavBarPos);
        leftOrTopSwapAcceptView.initSplitSwapAnimation(swapTarget, transformation);
        leftOrTopSwapAcceptView.setMiuiMultiWinHotAreaConfigListener(this);
        leftOrTopSwapAcceptView.setDragAnimationListener(this);
        leftOrTopSwapAcceptView.setSplitBarController(this.mSplitBarController);
    }

    private void configRightOrBottomHotAreaForSwapSplit(MiuiMultiWinSwapAcceptView rightOrBottomSwapAcceptView, MiuiMultiWinHotAreaView dropTarget, MiuiMultiWinHotAreaView swapTarget, MiuiMultiWinSwapAcceptView.Transformation transformation) {
        rightOrBottomSwapAcceptView.setIsLandScape(isLandScape());
        rightOrBottomSwapAcceptView.setHotAreaLayout(this.mHotArea);
        rightOrBottomSwapAcceptView.setNavBarInfo(this.mNavBarBound, this.mNavBarPos);
        rightOrBottomSwapAcceptView.initSplitSwapAnimation(swapTarget, transformation);
        rightOrBottomSwapAcceptView.setMiuiMultiWinHotAreaConfigListener(this);
        rightOrBottomSwapAcceptView.setDragAnimationListener(this);
        rightOrBottomSwapAcceptView.setSplitBarController(this.mSplitBarController);
    }

    private WindowManager.LayoutParams createBasicLayoutParams(int subtractY) {
        Point point = MiuiMultiWinUtils.getDisplaySize();
        int i = -1;
        int width = point != null ? point.x : -1;
        if (point != null) {
            i = point.y - subtractY;
        }
        int height = i;
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(width, height, 0, subtractY, 2026, 768, -3);
        layoutParams.privateFlags |= 16;
        layoutParams.gravity = 8388659;
        layoutParams.layoutInDisplayCutoutMode = 1;
        return layoutParams;
    }

    private LinearLayout createHotAreaForDragSplit() {
        LinearLayout linearLayout = new LinearLayout(this.mUiContext);
        linearLayout.setLayoutDirection(0);
        linearLayout.setOrientation(1);
        linearLayout.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));
        return linearLayout;
    }

    private LinearLayout createPushFullHotArea(MiuiMultiWinClipImageView pushTarget) {
        int width = isLandScape() ? 0 : -1;
        int height = isLandScape() ? -1 : 0;
        LinearLayout linearLayout = new LinearLayout(this.mUiContext);
        linearLayout.setLayoutDirection(0);
        int i = 1;
        linearLayout.setOrientation(!isLandScape());
        linearLayout.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));
        MiuiMultiWinPushAcceptView leftOrTopPushAcceptView = new MiuiMultiWinPushAcceptView(this.mUiContext);
        float f = 1.0f;
        leftOrTopPushAcceptView.setLayoutParams(new LinearLayout.LayoutParams(width, height, 1.0f));
        if (!isLandScape()) {
            i = 3;
        }
        leftOrTopPushAcceptView.setSplitMode(i);
        leftOrTopPushAcceptView.setPushTarget(pushTarget);
        leftOrTopPushAcceptView.setIsLandScape(isLandScape());
        leftOrTopPushAcceptView.setDragAnimationListener(this);
        leftOrTopPushAcceptView.setSplitBarController(this.mSplitBarController);
        leftOrTopPushAcceptView.setHotAreaLayout(this.mHotArea);
        MiuiMultiWinPushAcceptView rightOrBottomPushAcceptView = new MiuiMultiWinPushAcceptView(this.mUiContext);
        if (!isLandScape()) {
            f = 2.0f;
        }
        rightOrBottomPushAcceptView.setLayoutParams(new LinearLayout.LayoutParams(width, height, f));
        rightOrBottomPushAcceptView.setSplitMode(isLandScape() ? 2 : 4);
        rightOrBottomPushAcceptView.setPushTarget(pushTarget);
        rightOrBottomPushAcceptView.setIsLandScape(isLandScape());
        rightOrBottomPushAcceptView.setDragAnimationListener(this);
        rightOrBottomPushAcceptView.setSplitBarController(this.mSplitBarController);
        rightOrBottomPushAcceptView.setHotAreaLayout(this.mHotArea);
        MiuiMultiWinPushAcceptView freeformPushAcceptView = new MiuiMultiWinPushAcceptView(this.mUiContext);
        freeformPushAcceptView.setLayoutParams(new LinearLayout.LayoutParams(width, height, isLandScape() ? 8.0f : 7.0f));
        freeformPushAcceptView.setSplitMode(5);
        freeformPushAcceptView.setPushTarget(pushTarget);
        freeformPushAcceptView.setIsLandScape(isLandScape());
        freeformPushAcceptView.setDragAnimationListener(this);
        freeformPushAcceptView.setSplitBarController(this.mSplitBarController);
        freeformPushAcceptView.setHotAreaLayout(this.mHotArea);
        linearLayout.addView(leftOrTopPushAcceptView);
        linearLayout.addView(freeformPushAcceptView);
        linearLayout.addView(rightOrBottomPushAcceptView);
        leftOrTopPushAcceptView.setContentDescription("leftOrTopPushAcceptView");
        freeformPushAcceptView.setContentDescription("freeformPushAcceptView");
        rightOrBottomPushAcceptView.setContentDescription("rightOrBottomPushAcceptView");
        return linearLayout;
    }

    private LinearLayout createSplitSwapHotArea() {
        LinearLayout splitSwapHotArea = new LinearLayout(this.mUiContext);
        int height = 0;
        splitSwapHotArea.setLayoutDirection(0);
        splitSwapHotArea.setOrientation(!isLandScape());
        int width = isLandScape() ? -1 : 0;
        if (!isLandScape()) {
            height = -1;
        }
        splitSwapHotArea.setLayoutParams(new LinearLayout.LayoutParams(width, height, 1.0f));
        return splitSwapHotArea;
    }

    private void dropToFreeForm() {
        Task otherSideTask;
        Task dragTask;
        if (this.mWindowingMode == 5) {
            Slog.d(TAG, "The activity mode is freeform, do nothing");
            ActivityRecord activityRecord = ActivityRecord.forTokenLocked(this.mActivityToken);
            Task rootTask = null;
            if (activityRecord != null) {
                rootTask = activityRecord.getRootTask();
            }
            moveFreeFormToMiddle(rootTask);
            return;
        }
        synchronized (this.mAtms.getGlobalLock()) {
            ActivityRecord activityRecord2 = ActivityRecord.forTokenLocked(this.mActivityToken);
            if (activityRecord2 == null) {
                Slog.e(TAG, "ActivityRecord is null for token " + this.mActivityToken);
            } else if (activityRecord2.getTask() == null) {
                Slog.e(TAG, "this Task is null for token " + this.mActivityToken);
            } else {
                Task dragRootTask = activityRecord2.getRootTask();
                if (dragRootTask != null && dragRootTask.mCreatedByOrganizer && dragRootTask.hasChild()) {
                    int freeFormStackCount = 1;
                    if (dragRootTask.getWindowingMode() == 1 && dragRootTask.getTopChild() != null && dragRootTask.getTopChild().asTask() != null && dragRootTask.getTopChild().getWindowingMode() == 6) {
                        boolean child0IsPrimary = MiuiMultiWindowManager.getInstance(this.mAtms).isSplitPrimaryScreen(dragRootTask.getChildAt(0).asTask());
                        boolean isDragPrimary = WindowConfiguration.isSplitScreenPrimaryWindowingMode(this.mWindowingMode);
                        if (child0IsPrimary == isDragPrimary) {
                            dragTask = dragRootTask.getChildAt(0).getChildAt(0).asTask();
                            otherSideTask = dragRootTask.getChildAt(1).getChildAt(0).asTask();
                        } else {
                            dragTask = dragRootTask.getChildAt(1).getChildAt(0).asTask();
                            otherSideTask = dragRootTask.getChildAt(0).getChildAt(0).asTask();
                        }
                        int taskId = dragTask.mTaskId;
                        takeSnapshotForFreeformReplaced();
                        WindowContainer displayArea = dragTask.getDisplayArea();
                        if (displayArea != null) {
                            dragTask.getRequestedOverrideConfiguration().windowConfiguration.setWindowingMode(5);
                            if (dragTask.getParent() != displayArea) {
                                dragTask.reparent(displayArea, true);
                            }
                            dragTask.getRequestedOverrideConfiguration().smallestScreenWidthDp = 0;
                            this.mAtms.setTaskWindowingMode(taskId, 5, true);
                            moveFreeFormToMiddle(dragTask);
                            this.mAtms.mMiuiFreeFormManagerService.avoidedByOthersIfNeeded(dragTask);
                            final Task finalOtherSideRootTask = otherSideTask;
                            Task otherSideRootTask = displayArea.getRootTask(new Predicate() { // from class: com.android.server.wm.MiuiMultiWindowSwitchManager$$ExternalSyntheticLambda6
                                @Override // java.util.function.Predicate
                                public final boolean test(Object obj) {
                                    return MiuiMultiWindowSwitchManager.lambda$dropToFreeForm$2(finalOtherSideRootTask, (Task) obj);
                                }
                            });
                            List<MiuiFreeFormManager.MiuiFreeFormStackInfo> freeFormStackInfoList = MiuiFreeFormManager.getAllFreeFormStackInfosOnDisplay(displayArea.getDisplayId());
                            if (freeFormStackInfoList != null) {
                                freeFormStackCount = freeFormStackInfoList.size();
                            }
                            if (otherSideRootTask != null) {
                                displayArea.positionChildAt(displayArea.getChildCount() - (freeFormStackCount + 1), otherSideRootTask, false);
                            } else {
                                Slog.e(TAG, "dropToFreeForm otherSideTask is null");
                            }
                            if (displayArea.getOrCreateRootHomeTask() != null) {
                                displayArea.positionChildAt(displayArea.getChildCount() - (freeFormStackCount + 2), displayArea.getOrCreateRootHomeTask(), false);
                            } else {
                                Slog.e(TAG, "dropToFreeForm rootHomeTask is null");
                            }
                        }
                    }
                }
                Slog.e(TAG, "dropToFreeForm dragRootTask is " + dragRootTask);
            }
        }
    }

    public static /* synthetic */ boolean lambda$dropToFreeForm$2(Task finalOtherSideRootTask, Task task1) {
        return task1.getRootTaskId() == finalOtherSideRootTask.getRootTaskId();
    }

    private void dropToSplitScreen(int windowModeToChange) {
        synchronized (this.mAtms.getGlobalLock()) {
            if (WindowConfiguration.isSplitScreenWindowingMode(this.mWindowingMode) && this.mWindowingMode != windowModeToChange) {
                swapSplitScreen();
            } else if (WindowConfiguration.isFreeFormWindowingMode(this.mWindowingMode)) {
                replaceOrEnterSplitScreen(windowModeToChange);
            } else {
                Slog.i(TAG, "No need to switch mode " + this.mWindowingMode);
            }
        }
    }

    private Drawable getCurrentTokenIcon() {
        synchronized (this.mAtms.getGlobalLock()) {
            ActivityRecord activityRecord = ActivityRecord.forTokenLocked(this.mActivityToken);
            if (activityRecord == null) {
                Slog.e(TAG, "getTopAppIcons failed: activityRecord is null for token " + this.mActivityToken);
                return null;
            }
            return MiuiMultiWinUtils.getAppIcon(this.mUiContext, activityRecord.packageName, activityRecord.mUserId);
        }
    }

    private boolean getCurrentWindowingMode() {
        synchronized (this.mAtms.getGlobalLock()) {
            ActivityRecord activityRecord = ActivityRecord.forTokenLocked(this.mActivityToken);
            if (activityRecord == null) {
                Slog.e(TAG, "ActivityRecord is null for token " + this.mActivityToken);
                return false;
            }
            int windowingMode = activityRecord.getWindowingMode();
            this.mWindowingMode = windowingMode;
            if (windowingMode == 6) {
                Task task = activityRecord.getTask();
                if (MiuiMultiWindowManager.getInstance(this.mAtms).isSplitPrimaryScreen(task)) {
                    this.mWindowingMode = 3;
                } else {
                    this.mWindowingMode = 4;
                }
            }
            this.mDisplayBound = activityRecord.getDisplayContent().getWindowConfiguration().getBounds();
            Bundle bundle = getSplitRootTasksPos(activityRecord.getDisplayContent().getDisplayId());
            this.mIsLeftRightSplit = false;
            if (bundle != null && bundle.getInt(MiuiMultiWindowManager.MIUI_SPLIT_SCREEN_PRIMARY_POSITION) == 1) {
                this.mIsLeftRightSplit = true;
            }
            return true;
        }
    }

    public boolean isCtsModeEnabled() {
        return IS_CTS_MODE;
    }

    public Bundle getSplitRootTasksPos(int displayId) {
        DisplayContent display = this.mWms.mRoot.getDisplayContent(displayId);
        if (display == null || !MiuiMultiWindowUtils.MULTIWIN_SWITCH_ENABLED) {
            return null;
        }
        Bundle result = new Bundle();
        Configuration configuration = display.getConfiguration();
        if (configuration != null) {
            if (IS_CTS_MODE) {
                result.putInt(MiuiMultiWindowManager.MIUI_SPLIT_SCREEN_PRIMARY_POSITION, 0);
                return result;
            } else if (this.mForceSwitchLeftRightSplit != -1) {
                result.putInt(MiuiMultiWindowManager.MIUI_SPLIT_SCREEN_PRIMARY_POSITION, this.mIsLeftRightSplit ? 1 : 0);
                return result;
            } else if (configuration.smallestScreenWidthDp >= 600) {
                result.putInt(MiuiMultiWindowManager.MIUI_SPLIT_SCREEN_PRIMARY_POSITION, 1);
                return result;
            } else if (configuration.screenHeightDp > configuration.screenWidthDp) {
                result.putInt(MiuiMultiWindowManager.MIUI_SPLIT_SCREEN_PRIMARY_POSITION, 0);
            } else {
                result.putInt(MiuiMultiWindowManager.MIUI_SPLIT_SCREEN_PRIMARY_POSITION, 1);
            }
        }
        return result;
    }

    private static void registerObservers(final Context context) {
        ContentObserver observer = new ContentObserver(null) { // from class: com.android.server.wm.MiuiMultiWindowSwitchManager.3
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                MiuiMultiWindowSwitchManager.IS_CTS_MODE = !SystemProperties.getBoolean("persist.sys.miui_optimization", !SplitScreenReporter.ACTION_ENTER_SPLIT.equals(SystemProperties.get("ro.miui.cts")));
                MiuiMultiWindowUtils.refreshMultiWindowSwitchStatus();
            }
        };
        context.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(MiuiSettings.Secure.MIUI_OPTIMIZATION), false, observer, -2);
        observer.onChange(false);
        boolean z = true;
        IS_FORCE_FSG_NAVBAR = Settings.Global.getInt(context.getContentResolver(), "force_fsg_nav_bar", 0) != 0;
        ContentObserver forceFsgNavBarObserver = new ContentObserver(null) { // from class: com.android.server.wm.MiuiMultiWindowSwitchManager.4
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                if (MiuiMultiWindowSwitchManager.mForceFsgNavBarUri.equals(uri)) {
                    boolean z2 = false;
                    if (Settings.Global.getInt(context.getContentResolver(), "force_fsg_nav_bar", 0) != 0) {
                        z2 = true;
                    }
                    MiuiMultiWindowSwitchManager.IS_FORCE_FSG_NAVBAR = z2;
                }
            }
        };
        context.getContentResolver().registerContentObserver(mForceFsgNavBarUri, false, forceFsgNavBarObserver, -1);
        forceFsgNavBarObserver.onChange(false);
        if (Settings.Global.getInt(context.getContentResolver(), "hide_gesture_line", 0) == 0) {
            z = false;
        }
        IS_HIDE_GESTURELINE = z;
        ContentObserver navBarLineObserver = new ContentObserver(null) { // from class: com.android.server.wm.MiuiMultiWindowSwitchManager.5
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                if (MiuiMultiWindowSwitchManager.mHideGestureLineUri.equals(uri)) {
                    boolean z2 = false;
                    if (Settings.Global.getInt(context.getContentResolver(), "hide_gesture_line", 0) != 0) {
                        z2 = true;
                    }
                    MiuiMultiWindowSwitchManager.IS_HIDE_GESTURELINE = z2;
                }
            }
        };
        context.getContentResolver().registerContentObserver(mHideGestureLineUri, false, navBarLineObserver, -1);
        navBarLineObserver.onChange(false);
    }

    private Bitmap getDragSplitScreenShot(List<Bitmap> bitmapList) {
        Bitmap dragBmp;
        if (bitmapList == null || bitmapList.isEmpty()) {
            Slog.w(TAG, "getDragSplitScreenShot failed, cause splitScreenShots is null!");
            return null;
        } else if (bitmapList.size() <= 1) {
            Slog.w(TAG, "getDragSplitScreenShot failed, cause splitScreenShots size is less than two");
            return null;
        } else {
            if (isDragPrimary()) {
                dragBmp = bitmapList.get(0);
            } else if (isDragSecondary()) {
                dragBmp = bitmapList.get(1);
            } else {
                Slog.w(TAG, "getDragSplitScreenShot failed, cause not drag split!");
                return null;
            }
            if (dragBmp == null) {
                Slog.w(TAG, "getDragSplitScreenShot failed, cause dragBmp is null");
                return null;
            }
            Slog.d(TAG, "getCurrentTokenSnapShot: dragBmp = " + dragBmp + ", width = " + dragBmp.getWidth() + ", height = " + dragBmp.getHeight() + ", mSnapShotScaleFactor = " + this.mSnapShotScaleFactor);
            if ((!IS_FORCE_FSG_NAVBAR || !IS_HIDE_GESTURELINE) && MiuiMultiWinUtils.isNeedToResizeWithoutNavBar(MiuiMultiWinUtils.convertWindowMode2SplitMode(this.mWindowingMode, isLandScape()), this.mNavBarPos)) {
                return MiuiMultiWinUtils.getScreenShotBmpWithoutNavBar(dragBmp, this.mNavBarPos, this.mNavBarBound, this.mSnapShotScaleFactor);
            }
            return dragBmp;
        }
    }

    private void getSplitScreenSizeFromTaskShot(List<Bitmap> bitmapList) {
        if (bitmapList == null || bitmapList.isEmpty()) {
            Slog.w(TAG, "getSplitScreenSize failed, cause splitScreenShots is null!");
        } else if (bitmapList.size() <= 1) {
            Slog.w(TAG, "getSplitScreenSize failed, cause splitScreenShots size is less than two");
        } else {
            Bitmap bitmap = bitmapList.get(0);
            this.mLeftSplitScreenSize.set(bitmap.getWidth(), bitmap.getHeight());
            Bitmap bitmap2 = bitmapList.get(1);
            this.mRightSplitScreenSize.set(bitmap2.getWidth(), bitmap2.getHeight());
        }
    }

    private Rect getFreeFormRect(IBinder freeFormToken) {
        Rect rect = new Rect();
        synchronized (this.mAtms.getGlobalLock()) {
            ActivityRecord activityRecord = ActivityRecord.forTokenLocked(freeFormToken);
            if (activityRecord == null) {
                Slog.w(TAG, "getFreeFormRect failed: activityRecord is null!");
                return rect;
            }
            Task task = activityRecord.getTask();
            if (task == null) {
                Slog.w(TAG, "getFreeFormRect failed: task is null!");
                return rect;
            }
            Task rootTask = task.getRootTask();
            if (rootTask == null) {
                Slog.w(TAG, "getFreeFormRect failed: rootTask is null!");
                return rect;
            }
            rootTask.getBounds(rect);
            Slog.d(TAG, "getFreeFormRect: rect = " + rect);
            return rect;
        }
    }

    private int getFreeFormStackId(IBinder freeFormToken) {
        synchronized (this.mAtms.getGlobalLock()) {
            ActivityRecord activityRecord = ActivityRecord.forTokenLocked(freeFormToken);
            if (activityRecord == null) {
                Slog.w(TAG, "getFreeFormStackId failed: activityRecord is null!");
                return -1;
            }
            return activityRecord.getRootTaskId();
        }
    }

    private Rect getHotAreaFreeFormDropBound(boolean isDragSplit) {
        Rect freeformRect;
        ActivityRecord activityRecord;
        InsetsStateController insetsStateController;
        int right = this.mDisplayBound.width();
        int bottom = this.mDisplayBound.height();
        float widthCenter = (0 + right) / 2.0f;
        float heightCenter = (0 + bottom) / 2.0f;
        if (!isDragSplit) {
            int freeFormStackId = getFreeFormStackId(this.mActivityToken);
            if (freeFormStackId != -1) {
                MiuiFreeFormManager.MiuiFreeFormStackInfo miuiFreeFormStackInfo = MiuiFreeFormManager.getFreeFormStackInfoByStackId(freeFormStackId);
                if (miuiFreeFormStackInfo != null) {
                    freeformRect = miuiFreeFormStackInfo.bounds;
                    this.freeFormScale = miuiFreeFormStackInfo.freeFormScale;
                } else {
                    Slog.w(TAG, "miuiFreeFormStackInfo is null!");
                    freeformRect = getFreeFormRect(this.mActivityToken);
                    this.freeFormScale = MiuiMultiWindowUtils.sScale;
                }
            } else {
                Slog.w(TAG, "getFreeFormStackId failed!");
                freeformRect = getFreeFormRect(this.mActivityToken);
                this.freeFormScale = MiuiMultiWindowUtils.sScale;
            }
        } else {
            synchronized (this.mAtms.getGlobalLock()) {
                activityRecord = ActivityRecord.forTokenLocked(this.mActivityToken);
            }
            boolean isFreeformLandscape = false;
            if (activityRecord != null) {
                isFreeformLandscape = MiuiMultiWindowUtils.isOrientationLandscape(activityRecord.getRequestedOrientation());
            }
            DisplayFrames displayFrames = null;
            DisplayContent displayContent = activityRecord != null ? activityRecord.getDisplayContent() : null;
            if (displayContent == null) {
                insetsStateController = null;
            } else {
                insetsStateController = displayContent.getInsetsStateController();
            }
            InsetsStateController insetsStateController2 = insetsStateController;
            if (displayContent != null) {
                displayFrames = displayContent.mDisplayFrames;
            }
            DisplayFrames displayFrames2 = displayFrames;
            int statusBarHeight = Math.max(MiuiFreeFormGestureDetector.getStatusBarHeight(insetsStateController2), MiuiFreeFormGestureDetector.getDisplayCutoutHeight(displayFrames2));
            freeformRect = MiuiMultiWindowUtils.getFreeformRect(this.mUiContext, false, false, false, isFreeformLandscape, (Rect) null, (String) null, true, statusBarHeight);
            this.freeFormScale = MiuiMultiWindowUtils.sScale;
        }
        int freeFormWidth = freeformRect.width();
        int freeFormHeight = freeformRect.height();
        Rect dropTargetBound = new Rect();
        dropTargetBound.left = (int) (widthCenter - ((freeFormWidth * this.freeFormScale) / 2.0f));
        dropTargetBound.top = (int) (heightCenter - ((freeFormHeight * this.freeFormScale) / 2.0f));
        dropTargetBound.right = dropTargetBound.left + freeFormWidth;
        dropTargetBound.bottom = dropTargetBound.top + freeFormHeight;
        Slog.d(TAG, "getHotAreaFreeFormDropBound: dropTargetBound = " + dropTargetBound + ", mDisplayBound = " + this.mDisplayBound + ", mTopRootTaskIsSplit = " + this.mTopRootTaskIsSplit + ", mWindowingMode = " + this.mWindowingMode + ", sScale = " + this.freeFormScale + ", freeformRect = " + freeformRect);
        return dropTargetBound;
    }

    public static MiuiMultiWindowSwitchManager getInstance(ActivityTaskManagerService mAtms) {
        if (mInstance == null) {
            synchronized (MiuiMultiWindowSwitchManager.class) {
                if (mInstance == null) {
                    mInstance = new MiuiMultiWindowSwitchManager(mAtms);
                }
            }
        }
        return mInstance;
    }

    private int getNavBarBoundOnScreen(Rect outBound) {
        if (IS_FORCE_FSG_NAVBAR && IS_HIDE_GESTURELINE) {
            Slog.w(TAG, "getNavBarBoundOnScreen failed:full screen gesture and gestureLine is hidden!");
            return -1;
        } else if (outBound == null) {
            Slog.w(TAG, "getNavBarBoundOnScreen failed: outBound is null!");
            return -1;
        } else {
            synchronized (this.mAtms.getGlobalLock()) {
                ActivityRecord activityRecord = ActivityRecord.forTokenLocked(this.mActivityToken);
                if (activityRecord == null) {
                    Slog.w(TAG, "getNavBarBoundOnScreen failed: activityRecord is null!");
                    return -1;
                }
                Task task = activityRecord.getTask();
                if (task == null) {
                    Slog.w(TAG, "getNavBarBoundOnScreen failed: task is null!");
                    return -1;
                }
                Task rootTask = task.getRootTask();
                if (rootTask == null) {
                    Slog.w(TAG, "getNavBarBoundOnScreen failed: rootTask is null!");
                    return -1;
                }
                int navPos = MiuiMultiWindowManager.getInstance(this.mAtms).getNavBarBoundOnScreen(rootTask.getDisplayContent(), outBound);
                Slog.d(TAG, "getNavBarBoundOnScreen: navBarBound = " + outBound + ", navPos = " + navPos);
                return navPos;
            }
        }
    }

    public Rect getTopDisplayCutoutRect(int displayId) {
        DisplayContent displayContent = this.mWms.mRoot.getDisplayContent(displayId);
        if (displayContent == null) {
            Slog.w(TAG, "getTopDisplayCutoutRect failed, cause displayContent is null!");
            return new Rect();
        }
        int rotation = displayContent.getDisplayInfo().rotation;
        DisplayCutout cutout = displayContent.calculateDisplayCutoutForRotation(rotation).getDisplayCutout();
        if (cutout == null) {
            Slog.w(TAG, "getTopDisplayCutoutRect failed, cause cutout is null!");
            return new Rect();
        }
        return cutout.getBoundingRectTop();
    }

    private Point getRealSnapShotSize(Bitmap dragSplitScreenShot) {
        Point point = new Point();
        if (this.mSnapShotScaleFactor <= MiuiFreeformPinManagerService.EDGE_AREA) {
            this.mSnapShotScaleFactor = 1.0f;
        }
        point.x = (int) (dragSplitScreenShot.getWidth() / this.mSnapShotScaleFactor);
        point.y = (int) (dragSplitScreenShot.getHeight() / this.mSnapShotScaleFactor);
        return point;
    }

    private int getSplitBarSwapMarginAdjustmentWithNavBar() {
        if (this.mNavBarPos == 4 && !isLandScape()) {
            return this.mNavBarBound.height();
        }
        if (this.mNavBarPos == 2 && isLandScape()) {
            return this.mNavBarBound.width();
        }
        if (this.mNavBarPos == 1 && isLandScape()) {
            return -this.mNavBarBound.width();
        }
        return 0;
    }

    private float getSplitFractionBySplitRatio() {
        return this.mSplitFractionRatio;
    }

    private int getStatusBarHeight() {
        return this.mUiContext.getResources().getDimensionPixelSize(17105574);
    }

    private boolean isValidSplitRootTask(Task[] out, Task rootContainer) {
        if (rootContainer == null || rootContainer.getChildCount() < 2 || out.length != 2) {
            return false;
        }
        Task mainStage = rootContainer.getChildAt(0).asTask();
        Task sideStage = rootContainer.getChildAt(1).asTask();
        if (mainStage == null || sideStage == null) {
            return false;
        }
        out[0] = mainStage;
        out[1] = sideStage;
        if (!mainStage.hasChild() || !sideStage.hasChild()) {
            return false;
        }
        return true;
    }

    private List<Drawable> getTopAppIcons() {
        Task secondaryTask;
        Task primaryTask;
        ArrayList<Drawable> icons = new ArrayList<>(2);
        synchronized (this.mAtms.getGlobalLock()) {
            ActivityRecord activityRecord = ActivityRecord.forTokenLocked(this.mActivityToken);
            if (activityRecord == null) {
                Slog.e(TAG, "getTopAppIcons failed: activityRecord is null for token " + this.mActivityToken);
                return icons;
            }
            Task filteredTopTask = MiuiMultiWindowManager.getInstance(this.mAtms).getFilteredTopStack(activityRecord.getDisplayContent(), Arrays.asList(5, 2));
            if (filteredTopTask != null && filteredTopTask.getTopMostTask() != null) {
                if (filteredTopTask.mCreatedByOrganizer && filteredTopTask.hasChild() && filteredTopTask.getWindowingMode() == 1 && filteredTopTask.getTopChild() != null && filteredTopTask.getTopChild().asTask() != null && filteredTopTask.getTopChild().getWindowingMode() == 6) {
                    Task[] splitStages = new Task[2];
                    if (!isValidSplitRootTask(splitStages, filteredTopTask)) {
                        Slog.e(TAG, "getTopAppIcons(): split stages are invalid");
                        return icons;
                    }
                    if (MiuiMultiWindowManager.getInstance(this.mAtms).isSplitPrimaryScreen(filteredTopTask.getChildAt(0).asTask())) {
                        primaryTask = splitStages[0].getChildAt(0).asTask();
                        secondaryTask = splitStages[1].getChildAt(0).asTask();
                    } else {
                        primaryTask = splitStages[1].getChildAt(0).asTask();
                        secondaryTask = splitStages[0].getChildAt(0).asTask();
                    }
                    if (primaryTask != null && secondaryTask != null) {
                        ActivityRecord primaryTopActivityRecord = primaryTask.getTopActivity(false, true);
                        if (primaryTopActivityRecord == null) {
                            Slog.w(TAG, "getTopAppIcons failed: topActivityRecord is null!");
                            return icons;
                        }
                        Drawable primaryIcon = MiuiMultiWinUtils.getAppIcon(this.mUiContext, primaryTopActivityRecord.packageName, primaryTopActivityRecord.mUserId);
                        icons.add(primaryIcon);
                        ActivityRecord secondaryTopActivityRecord = secondaryTask.getTopActivity(false, true);
                        if (secondaryTopActivityRecord == null) {
                            Slog.w(TAG, "getTopAppIcons failed: topActivityRecord is null!");
                            return icons;
                        }
                        Drawable secondaryIcon = MiuiMultiWinUtils.getAppIcon(this.mUiContext, secondaryTopActivityRecord.packageName, secondaryTopActivityRecord.mUserId);
                        icons.add(secondaryIcon);
                    }
                    Slog.w(TAG, "getTopAppIcons failed: split stage's child is not task");
                    return icons;
                }
                ActivityRecord topActivityRecord = filteredTopTask.getTopMostTask().getTopActivity(false, true);
                if (topActivityRecord == null) {
                    Slog.w(TAG, "getTopAppIcons failed: topActivityRecord is null!");
                    return icons;
                }
                Drawable icon = MiuiMultiWinUtils.getAppIcon(this.mUiContext, topActivityRecord.packageName, topActivityRecord.mUserId);
                if (icon != null) {
                    icons.add(icon);
                }
                return icons;
            }
            Slog.e(TAG, "getTopAppIcons failed: top rootTask or top task is null " + filteredTopTask);
            return icons;
        }
    }

    private List<Bitmap> getTopRootTaskSnapShot() {
        Task secondaryTask;
        Task primaryTask;
        ArrayList<Bitmap> bitmaps = new ArrayList<>(2);
        synchronized (this.mAtms.getGlobalLock()) {
            ActivityRecord activityRecord = ActivityRecord.forTokenLocked(this.mActivityToken);
            if (activityRecord == null) {
                return bitmaps;
            }
            Task filteredTopTask = MiuiMultiWindowManager.getInstance(this.mAtms).getFilteredTopStack(activityRecord.getDisplayContent(), Arrays.asList(5, 2));
            if (filteredTopTask == null) {
                Slog.e(TAG, "Top filteredTopTask is null");
                return bitmaps;
            }
            if (filteredTopTask.mCreatedByOrganizer && filteredTopTask.hasChild() && filteredTopTask.getWindowingMode() == 1 && filteredTopTask.getTopChild() != null && filteredTopTask.getTopChild().asTask() != null && filteredTopTask.getTopChild().getWindowingMode() == 6) {
                this.mTopRootTaskIsSplit = true;
                Task[] splitStages = new Task[2];
                if (!isValidSplitRootTask(splitStages, filteredTopTask)) {
                    Slog.e(TAG, "getTopRootTaskSnapShot(): split tasks container do not have child");
                    Bitmap[] defaultBitmaps = {createDefaultTaskBitmap(splitStages[0]), createDefaultTaskBitmap(splitStages[1])};
                    bitmaps.addAll(Arrays.asList(defaultBitmaps));
                    return bitmaps;
                }
                if (MiuiMultiWindowManager.getInstance(this.mAtms).isSplitPrimaryScreen(filteredTopTask.getChildAt(0).asTask())) {
                    primaryTask = splitStages[0].getChildAt(0).asTask();
                    secondaryTask = splitStages[1].getChildAt(0).asTask();
                } else {
                    primaryTask = splitStages[1].getChildAt(0).asTask();
                    secondaryTask = splitStages[0].getChildAt(0).asTask();
                }
                if (primaryTask != null && secondaryTask != null) {
                    TaskSnapshot primaryTaskSnapshot = null;
                    TaskSnapshot secondaryTaskSnapshot = null;
                    synchronized (this.mAtms.mWindowManager.getGlobalLock()) {
                        if (this.mAtms.mWindowManager.getTaskSnapshotController().getSnapshotMode(primaryTask) == 0) {
                            primaryTaskSnapshot = this.mAtms.mWindowManager.getTaskSnapshotController().snapshotTask(primaryTask);
                        }
                        if (this.mAtms.mWindowManager.getTaskSnapshotController().getSnapshotMode(secondaryTask) == 0) {
                            secondaryTaskSnapshot = this.mAtms.mWindowManager.getTaskSnapshotController().snapshotTask(secondaryTask);
                        }
                    }
                    if (primaryTaskSnapshot != null) {
                        Bitmap PrimaryBitmap = Bitmap.wrapHardwareBuffer(HardwareBuffer.createFromGraphicBuffer(primaryTaskSnapshot.getSnapshot()), primaryTaskSnapshot.getColorSpace());
                        if (PrimaryBitmap != null) {
                            Bitmap softBmp = PrimaryBitmap.copy(Bitmap.Config.ARGB_8888, true);
                            bitmaps.add(softBmp);
                        } else {
                            Bitmap defaultBitmap = createDefaultTaskBitmap(primaryTask.asTask());
                            bitmaps.add(defaultBitmap);
                        }
                    } else {
                        Bitmap defaultBitmap2 = createDefaultTaskBitmap(primaryTask.asTask());
                        bitmaps.add(defaultBitmap2);
                    }
                    if (secondaryTaskSnapshot != null) {
                        Bitmap secondaryBitmap = Bitmap.wrapHardwareBuffer(HardwareBuffer.createFromGraphicBuffer(secondaryTaskSnapshot.getSnapshot()), secondaryTaskSnapshot.getColorSpace());
                        if (secondaryBitmap != null) {
                            Bitmap softBmp2 = secondaryBitmap.copy(Bitmap.Config.ARGB_8888, true);
                            bitmaps.add(softBmp2);
                        } else {
                            Bitmap defaultBitmap3 = createDefaultTaskBitmap(secondaryTask.asTask());
                            bitmaps.add(defaultBitmap3);
                        }
                    } else {
                        Bitmap defaultBitmap4 = createDefaultTaskBitmap(secondaryTask.asTask());
                        bitmaps.add(defaultBitmap4);
                    }
                }
                Slog.e(TAG, "getTopRootTaskSnapShot(): split stages are invalid");
                return bitmaps;
            }
            this.mTopRootTaskIsSplit = false;
            TaskSnapshot taskSnapshot = MiuiMultiWindowManager.getInstance(this.mAtms).getTaskSnapshot(filteredTopTask);
            if (taskSnapshot != null) {
                Bitmap taskBitmap = Bitmap.wrapHardwareBuffer(HardwareBuffer.createFromGraphicBuffer(taskSnapshot.getSnapshot()), taskSnapshot.getColorSpace());
                if (taskBitmap != null) {
                    Bitmap softBmp3 = taskBitmap.copy(Bitmap.Config.ARGB_8888, true);
                    bitmaps.add(softBmp3);
                } else {
                    Bitmap defaultBitmap5 = createDefaultTaskBitmap(filteredTopTask);
                    bitmaps.add(defaultBitmap5);
                }
            } else {
                Bitmap defaultBitmap6 = createDefaultTaskBitmap(filteredTopTask);
                bitmaps.add(defaultBitmap6);
            }
            return bitmaps;
        }
    }

    private Bitmap createWhiteBitmap(int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(-1);
        return bitmap;
    }

    private Bitmap createDefaultTaskBitmap(Task task) {
        Rect bounds = task.getBounds();
        int width = bounds.right - bounds.left;
        int height = bounds.bottom - bounds.top;
        return createWhiteBitmap(width, height);
    }

    private Bitmap getFreeformRootTaskSnapShot() {
        synchronized (this.mAtms.getGlobalLock()) {
            ActivityRecord activityRecord = ActivityRecord.forTokenLocked(this.mActivityToken);
            if (activityRecord == null) {
                return null;
            }
            Task freeformTask = activityRecord.getTask();
            if (freeformTask == null) {
                Slog.e(TAG, "getFreeformRootTaskSnapShot failed: task is null!");
                return null;
            }
            TaskSnapshot snapshot = MiuiMultiWindowManager.getInstance(this.mAtms).getTaskSnapshot(freeformTask);
            if (snapshot != null) {
                try {
                    Bitmap bitmap = Bitmap.wrapHardwareBuffer(HardwareBuffer.createFromGraphicBuffer(snapshot.getSnapshot()), snapshot.getColorSpace());
                    return bitmap.copy(Bitmap.Config.ARGB_8888, true);
                } catch (IllegalArgumentException e) {
                    Slog.e(TAG, "wrapHardwareBuffer get IllegalArgumentException" + e.getMessage());
                }
            }
            return null;
        }
    }

    public Handler getDragEndAnimHanlder() {
        return this.mDragEndAnimHanlder;
    }

    private void handleDragAdapterAnimation(Rect bound, int animType, boolean isUpDownSplitDrop) {
        Slog.d(TAG, "handleDragAdapterAnimation: animType = " + animType + ", dropTargetBound = " + bound);
        if (this.mDragAnimationAdapter == null) {
            Slog.w(TAG, "handleDragAdapterAnimation: mDragAnimationAdapter is null!");
            return;
        }
        if (bound == null && (animType == 6 || animType == 9)) {
            bound = new Rect();
        }
        switch (animType) {
            case 2:
                this.mDragAnimationAdapter.startFreeformDropFreeformAnimation();
                return;
            case 3:
                this.mDragAnimationAdapter.startRecoverAnimation();
                return;
            case 4:
                this.mDragAnimationAdapter.startDropSplitScreenAnimation();
                return;
            case 5:
                this.mDragAnimationAdapter.startEnterSplitAlphaAnimation();
                return;
            case 6:
                this.mDragAnimationAdapter.startDropDisappearAnimation(bound);
                return;
            case 7:
                this.mDragAnimationAdapter.startSplitEnterFreeFormAnimation();
                return;
            case 8:
                this.mDragAnimationAdapter.startSplitExitFreeFormAnimation(false);
                return;
            case 9:
                this.mDragAnimationAdapter.startSplitDropSplit(bound, isUpDownSplitDrop);
                return;
            case 10:
                this.mDragAnimationAdapter.startSplitExitFreeFormAnimation(true);
                return;
            default:
                return;
        }
    }

    private void handleSwitchScreen() {
        if (this.mIsScreenSwitchHandled) {
            Slog.d(TAG, "handleSwitchScreen has been handled, just return!");
            return;
        }
        try {
            Slog.i(TAG, "handleSwitchScreen: mActivityToken = " + this.mActivityToken + ", mLastDropSplitMode = " + this.mLastDropSplitMode + ", isFreeFormDragged = " + WindowConfiguration.isFreeFormWindowingMode(this.mWindowingMode));
            int i = this.mLastDropSplitMode;
            if (i != 1 && i != 3) {
                if (i != 2 && i != 4) {
                    if (i == 5) {
                        dropToFreeForm();
                    } else {
                        handleRemoveHotArea();
                    }
                    this.mIsScreenSwitchHandled = true;
                }
                dropToSplitScreen(4);
                this.mIsScreenSwitchHandled = true;
            }
            dropToSplitScreen(3);
            this.mIsScreenSwitchHandled = true;
        } catch (Exception e) {
            this.mIsScreenSwitchHandled = true;
            throw e;
        }
    }

    private WindowManager.LayoutParams initHotArea() {
        if (this.mRootView != null) {
            Slog.d(TAG, "mRootView not null");
            handleRemoveHotArea();
            this.mIsProcessingDrag = true;
        }
        removeScreenShotCover();
        RelativeLayout relativeLayout = (RelativeLayout) LayoutInflater.from(this.mUiContext).inflate(285999136, (ViewGroup) null);
        this.mRootView = relativeLayout;
        if (relativeLayout == null) {
            Slog.e(TAG, "initHotArea failed, cause mRootView is null!");
            return null;
        }
        relativeLayout.setLayoutDirection(0);
        LinearLayout mDropView = (LinearLayout) this.mRootView.findViewById(285868139);
        if (mDropView == null) {
            Slog.e(TAG, "initHotArea failed, cause mDropView is null!");
            return null;
        }
        mDropView.setLayoutDirection(0);
        ViewGroup viewGroup = (ViewGroup) mDropView.findViewById(285868140);
        this.mHotArea = viewGroup;
        if (viewGroup == null) {
            Slog.e(TAG, "initHotArea failed, cause mHotArea is null!");
            return null;
        }
        viewGroup.setLayoutDirection(0);
        WindowManager.LayoutParams layoutParams = createBasicLayoutParams(0);
        layoutParams.flags |= 8;
        layoutParams.setTitle("MultiWindowHotArea");
        LinearLayout splitScreeRegion = (LinearLayout) this.mHotArea.findViewById(285868151);
        if (splitScreeRegion == null) {
            Slog.e(TAG, "initHotArea failed, cause splitLayout is null!");
            return null;
        }
        splitScreeRegion.setLayoutDirection(0);
        if (isLandScape()) {
            splitScreeRegion.setOrientation(0);
        } else {
            splitScreeRegion.setOrientation(1);
        }
        if (this.mWindowingMode == 5) {
            MiuiMultiWinHotAreaView freeformRegion = (MiuiMultiWinHotAreaView) this.mHotArea.findViewById(285868136);
            if (freeformRegion == null) {
                Slog.e(TAG, "initHotArea failed, cause freeformScreenRegion is null!");
                return null;
            }
            RelativeLayout.LayoutParams freeformRegionParams = (RelativeLayout.LayoutParams) freeformRegion.getLayoutParams();
            if (isLandScape()) {
                freeformRegionParams.width = this.mDisplayBound.right - ((this.mDisplayBound.width() / 10) * 2);
                freeformRegionParams.height = -1;
            } else {
                freeformRegionParams.height = (this.mDisplayBound.bottom - ((this.mDisplayBound.height() / 10) * 3)) - getStatusBarHeight();
                freeformRegionParams.width = -1;
            }
            freeformRegion.setLayoutParams(freeformRegionParams);
        }
        this.mIsDropHandled = false;
        return layoutParams;
    }

    private boolean isDragFreeForm() {
        return WindowConfiguration.isFreeFormWindowingMode(this.mWindowingMode);
    }

    private boolean isDragPrimary() {
        return WindowConfiguration.isSplitScreenPrimaryWindowingMode(this.mWindowingMode);
    }

    private boolean isDragSecondary() {
        return WindowConfiguration.isSplitScreenSecondaryWindowingMode(this.mWindowingMode);
    }

    private boolean isDragSplit() {
        return isDragPrimary() || isDragSecondary();
    }

    private boolean isInLazyMode() {
        return false;
    }

    private boolean isInSubFoldDisplayMode() {
        return false;
    }

    public boolean isLandScape() {
        return this.mIsLeftRightSplit;
    }

    private void moveFreeFormToMiddle(Task freeFormRootTask) {
        synchronized (this.mAtms.getGlobalLock()) {
            Rect rect = this.mFreeFormDropBound;
            if (rect == null) {
                Slog.w(TAG, "moveFreeFormToMiddle failed, cause mFreeFormDropBound is null!");
            } else if (freeFormRootTask == null) {
                Slog.w(TAG, "moveFreeFormToMiddle failed, cause freeFormRootTask is null!");
            } else {
                freeFormRootTask.resize(rect, false, true);
            }
        }
    }

    private boolean preStartDragAndDrop(Bundle info, List<Bitmap> bitMapList) {
        Point initialClipSize;
        Bitmap dragScreenShot;
        this.mDefaultFreeformDropBound = getHotAreaFreeFormDropBound(true);
        Rect hotAreaFreeFormDropBound = getHotAreaFreeFormDropBound(this.mTopRootTaskIsSplit && !WindowConfiguration.isFreeFormWindowingMode(this.mWindowingMode));
        this.mFreeFormDropBound = hotAreaFreeFormDropBound;
        int width = (int) (hotAreaFreeFormDropBound.width() * this.freeFormScale);
        int height = (int) (this.mFreeFormDropBound.height() * this.freeFormScale);
        this.mDropTargetBound = new Rect(0, 0, width, height);
        Point dragFreeformSize = new Point(width, height);
        if (isDragFreeForm()) {
            dragScreenShot = getFreeformRootTaskSnapShot();
            initialClipSize = new Point(dragFreeformSize);
            if (this.mTopRootTaskIsSplit) {
                getSplitScreenSizeFromTaskShot(bitMapList);
            }
        } else if (!isDragSplit()) {
            Slog.e(TAG, "addHotArea failed, cause drag window mode is unknown!");
            return false;
        } else {
            getSplitScreenSizeFromTaskShot(bitMapList);
            dragScreenShot = getDragSplitScreenShot(bitMapList);
            if (dragScreenShot == null) {
                Slog.w(TAG, "addHotArea getCurrentTokenSnapShot is null, return!");
                return false;
            }
            initialClipSize = getRealSnapShotSize(dragScreenShot);
        }
        Point dragSurfaceSize = new Point(this.mDisplayBound.width(), this.mDisplayBound.height());
        if (dragSurfaceSize.x > 0 && dragSurfaceSize.y > 0) {
            info.putParcelable("dragSurfaceSize", dragSurfaceSize);
            info.putParcelable("dragFreeformSize", dragFreeformSize);
            info.putParcelable("initialClipSize", initialClipSize);
            info.putParcelable("screenShot", dragScreenShot);
            return true;
        }
        Slog.e(TAG, "preStartDragAndDrop failed, cause dragSurfaceSize is invalid!");
        return false;
    }

    private void setMarginForSplitBar(MiuiMultiWinHotAreaView leftSplitScreenRegion, MiuiMultiWinHotAreaView rightSplitScreenRegion) {
        ViewGroup.LayoutParams lp = leftSplitScreenRegion.getLayoutParams();
        if (!(lp instanceof LinearLayout.LayoutParams)) {
            Slog.w(TAG, "setMarginForSplitBar failed, cause lp is not LinearLayout.LayoutParams.");
            return;
        }
        LinearLayout.LayoutParams leftLp = (LinearLayout.LayoutParams) lp;
        ViewGroup.LayoutParams rp = rightSplitScreenRegion.getLayoutParams();
        if (!(rp instanceof LinearLayout.LayoutParams)) {
            Slog.w(TAG, "setMarginForSplitBar failed, cause rp is not LinearLayout.LayoutParams.");
            return;
        }
        LinearLayout.LayoutParams rightLp = (LinearLayout.LayoutParams) rp;
        if (isLandScape()) {
            leftLp.rightMargin = this.mUiContext.getResources().getDimensionPixelSize(285671527);
            rightLp.leftMargin = this.mUiContext.getResources().getDimensionPixelSize(285671527);
            return;
        }
        leftLp.bottomMargin = this.mUiContext.getResources().getDimensionPixelSize(285671527);
        rightLp.topMargin = this.mUiContext.getResources().getDimensionPixelSize(285671527);
    }

    private boolean setUpInputSurface() {
        cleanUpInputSurface();
        Display display = DisplayManagerGlobal.getInstance().getRealDisplay(0);
        if (display == null) {
            Slog.w(TAG, "setUpInputSurface: failed, cause display is null!");
            return false;
        }
        DisplayContent displayContent = this.mWms.mRoot.getDisplayContent(display.getDisplayId());
        if (displayContent == null) {
            Slog.w(TAG, "setUpInputSurface: failed, cause dc is null!");
            return false;
        }
        synchronized (this.mAtms.getGlobalLock()) {
            ActivityRecord activityRecord = ActivityRecord.forTokenLocked(this.mActivityToken);
            if (activityRecord == null) {
                Slog.w(TAG, "setUpInputSurface: failed, cause activityRecord or appWindowToken is null!");
                return false;
            }
            WindowState windowState = activityRecord.findMainWindow();
            if (windowState == null) {
                Slog.w(TAG, "setUpInputSurface: failed, cause currentFocus is null!");
                return false;
            }
            windowState.transferTouch();
            this.mInputSurface = this.mWms.makeSurfaceBuilder(displayContent.getSession()).setContainerLayer().setName("MiuiMultiWindowSwitchMngr Input Consumer").build();
            Slog.w(TAG, "setUpInputSurface inputSurface=" + this.mInputSurface);
            MiuiMultiWindowInputInterceptor miuiMultiWindowInputInterceptor = new MiuiMultiWindowInputInterceptor(display);
            this.mInterceptor = miuiMultiWindowInputInterceptor;
            InputWindowHandle inputWindowHandle = miuiMultiWindowInputInterceptor.mWindowHandle;
            SurfaceControl.Transaction transaction = new SurfaceControl.Transaction();
            transaction.show(this.mInputSurface);
            transaction.setInputWindowInfo(this.mInputSurface, inputWindowHandle);
            transaction.setLayer(this.mInputSurface, Integer.MAX_VALUE);
            Point point = new Point();
            display.getRealSize(point);
            transaction.setWindowCrop(this.mInputSurface, new Rect(0, 0, point.x, point.y));
            transaction.syncInputWindows();
            transaction.apply();
            if (windowState.mInputChannel == null || this.mInterceptor.mClientChannel == null) {
                Slog.d(TAG, "setUpInputSurface: failed, cause inputChannel is null!");
                return false;
            }
            this.mWms.mInputManager.transferTouchFocus(windowState.mInputChannel, this.mInterceptor.mClientChannel, false);
            Slog.d(TAG, "setUpInputSurface done");
            return true;
        }
    }

    private void showNotSupportToast(final Context context, final String activityName, final boolean isSupportSplit) {
        this.mAtms.mUiHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiMultiWindowSwitchManager$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                MiuiMultiWindowSwitchManager.lambda$showNotSupportToast$3(isSupportSplit, context, activityName);
            }
        });
    }

    public static /* synthetic */ void lambda$showNotSupportToast$3(boolean isSupportSplit, Context context, String activityName) {
        String tips;
        if (isSupportSplit) {
            tips = context.getResources().getString(286196350, activityName);
        } else {
            tips = context.getResources().getString(286196349, activityName);
        }
        Toast.makeText(context, tips, 0).show();
    }

    private void showNotSupportFreeformToast(final Context context, final String activityName) {
        this.mAtms.mUiHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiMultiWindowSwitchManager$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                MiuiMultiWindowSwitchManager.lambda$showNotSupportFreeformToast$4(context, activityName);
            }
        });
    }

    public static /* synthetic */ void lambda$showNotSupportFreeformToast$4(Context context, String activityName) {
        String tips = context.getResources().getString(286196349, activityName);
        Toast toast = Toast.makeText(context, tips, 0);
        MiuiMultiWindowUtils.invoke(toast, "setTopMostToast", new Object[]{true});
        toast.show();
    }

    public boolean startDragAndDrop(View dragView, Bundle info) {
        Drawable bottomBarImg;
        Rect freeformDragBarRect;
        Rect freeformDragBarRect2;
        Drawable topBarImg;
        int i;
        Rect splitDragBarRect;
        float f;
        Slog.w(TAG, "startDragAndDrop dragView=" + dragView);
        if (info == null) {
            Slog.w(TAG, "startDragAndDrop failed! cause info is null!");
            return false;
        } else if (dragView == null) {
            Slog.w(TAG, "startDragAndDrop failed! cause dragBar is null!");
            return false;
        } else if (!info.containsKey("dragFreeformSize")) {
            Slog.w(TAG, "startDragAndDrop failed, cause info has no DRAG_SURFACE_SIZE_KEY!");
            return false;
        } else {
            Point dragFreeformSize = (Point) info.getParcelable("dragFreeformSize");
            if (dragFreeformSize == null) {
                Slog.w(TAG, "startDragAndDrop failed, cause dragSurfaceSize is null!");
                return false;
            }
            int clipWidth = dragFreeformSize.x;
            int clipHeight = dragFreeformSize.y;
            Drawable background = new ColorDrawable(this.mUiContext.getResources().getColor(285605970));
            Drawable border = this.mUiContext.getResources().getDrawable(285737280);
            if (!info.containsKey("screenShot")) {
                Slog.w(TAG, "startDragAndDrop failed! cause info has no SCREEN_SHOT_KEY!");
                return false;
            }
            Bitmap screenShot = (Bitmap) info.getParcelable("screenShot");
            if (!info.containsKey("initialClipSize")) {
                Log.w(TAG, "startDragAndDrop failed! cause info has no INITIAL_CLIP_SIZE_KEY!");
                return false;
            } else if (!info.containsKey("dragTouchOffsets")) {
                Slog.w(TAG, "startDragAndDrop failed, cause info has no DRAG_TOUCH_OFFSETS_KEY!");
                return false;
            } else {
                Point touchOffsets = (Point) info.getParcelable("dragTouchOffsets");
                if (isDragFreeForm()) {
                    info.putParcelable("dragTouchOffsets", touchOffsets);
                }
                if (!info.containsKey("dragBarBitmap")) {
                    Slog.w(TAG, "startDragAndDrop failed! cause info has no DRAG_BAR_BMP_KEY!");
                    return false;
                }
                float freeFormScale = this.freeFormScale;
                if (MiuiMultiWindowUtils.multiFreeFormSupported(this.mUiContext)) {
                    bottomBarImg = this.mUiContext.getResources().getDrawable(285737086);
                } else {
                    bottomBarImg = this.mUiContext.getResources().getDrawable(285737085);
                }
                int bottomBarBottomMargin = (int) ((((45.0f / freeFormScale) / 2.0f) - (bottomBarImg.getIntrinsicHeight() / 2.0f)) * freeFormScale);
                this.mDragAnimationAdapter = new MiuiMultiWinDragAnimationAdapter(dragView).setScreenShot(screenShot).setCaptionView(dragView).setDragBackground(background).setDragBorder(border).setBottomBar(bottomBarImg, bottomBarBottomMargin).setDraggingClipSize(clipWidth, clipHeight).setDefaultFreeFormDropBound(this.mDefaultFreeformDropBound).setRootTaskIsSplit(this.mTopRootTaskIsSplit);
                bottomBarImg.setBounds(0, 0, bottomBarImg.getIntrinsicWidth(), bottomBarImg.getIntrinsicHeight());
                Bitmap dragBarBitmap = (Bitmap) info.getParcelable("dragBarBitmap");
                if (dragBarBitmap == null) {
                    Slog.w(TAG, "startDragAndDrop failed: draBarBitmap is null");
                    return false;
                }
                float dragBarSplitTopMargin = MiuiMultiWinClientUtils.DEFAULT_SPLIT_DRAG_BAR_TOP_MARGIN;
                if (!this.mIsLeftRightSplit) {
                    if (isDragPrimary()) {
                        f = this.mUiContext.getResources().getDimensionPixelSize(17105574);
                    } else {
                        f = MiuiMultiWinClientUtils.VERTICAL_SPLIT_DRAG_BAR_TOP_MARGIN + MiuiMultiWinClientUtils.BOTTOM_SPLIT_DRAG_BAR_TOP_MARGIN;
                    }
                    dragBarSplitTopMargin = f;
                }
                float dragBarFreeformTopMargin = ((MiuiMultiWindowUtils.getTopCaptionViewHeight(this.mUiContext) / 2.0f) - (dragBarBitmap.getHeight() / 2.0f)) * freeFormScale;
                if (!isDragFreeForm()) {
                    if (MiuiMultiWindowUtils.multiFreeFormSupported(this.mUiContext)) {
                        topBarImg = this.mUiContext.getResources().getDrawable(285737082);
                    } else {
                        topBarImg = this.mUiContext.getResources().getDrawable(285737081);
                    }
                    Rect freeformDragBarRect3 = new Rect(0, 0, topBarImg.getIntrinsicWidth(), topBarImg.getIntrinsicHeight());
                    freeformDragBarRect2 = new Rect(0, 0, dragBarBitmap.getWidth(), dragBarBitmap.getHeight());
                    this.mDragAnimationAdapter.setDragBarBmp(dragBarBitmap, dragBarSplitTopMargin);
                    freeformDragBarRect = freeformDragBarRect3;
                } else {
                    if (MiuiMultiWindowUtils.isPadScreen(this.mUiContext)) {
                        i = 0;
                        splitDragBarRect = new Rect(0, 0, MiuiMultiWinClientUtils.SPLIT_DRAG_BAR_WIDTH_PAD, MiuiMultiWinClientUtils.SPLIT_DRAG_BAR_HEIGHT_PAD);
                    } else {
                        i = 0;
                        splitDragBarRect = new Rect(0, 0, MiuiMultiWinClientUtils.SPLIT_DRAG_BAR_WIDTH_FOLD, MiuiMultiWinClientUtils.SPLIT_DRAG_BAR_HEIGHT_FOLD);
                    }
                    Rect freeformDragBarRect4 = new Rect(i, i, dragBarBitmap.getWidth(), dragBarBitmap.getHeight());
                    this.mDragAnimationAdapter.setDragBarBmp(dragBarBitmap, dragBarFreeformTopMargin);
                    freeformDragBarRect = freeformDragBarRect4;
                    freeformDragBarRect2 = splitDragBarRect;
                }
                this.mDragAnimationAdapter.setSplitDragBarRect(freeformDragBarRect2).setSplitDragBarTopMargin((int) dragBarSplitTopMargin).setFreeFromDragBarRect(freeformDragBarRect).setFreeFormDragBarTopMargin((int) dragBarFreeformTopMargin).setIsLandScape(this.mIsLeftRightSplit);
                Drawable iconDrawable = getCurrentTokenIcon();
                if (iconDrawable == null) {
                    return false;
                }
                this.mDragAnimationAdapter.setIconDrawable(iconDrawable);
                Bitmap iconBitmap = MiuiMultiWinUtils.drawable2Bitmap(iconDrawable);
                Bitmap radiatedBitamp = MiuiMultiWinUtils.getRadiatedBitmap(iconBitmap, 60, 20);
                Bitmap blurRadiatedBitmap = MiuiMultiWinUtils.rsBlurNoScale(mInstance.mUiContext, radiatedBitamp, 25);
                Bitmap blurAlphaRadiatedBitmap = MiuiMultiWinUtils.changeBitmapAlpha(blurRadiatedBitmap, (int) (DEFAULT_BLURICON_ALPHA_RATIO * 255.0f));
                this.mDragAnimationAdapter.setBlurRadiatedIconBitmap(blurAlphaRadiatedBitmap);
                info.putParcelable("freeformDropBound", this.mFreeFormDropBound);
                boolean result = this.mDragAnimationAdapter.startDragAndDrop(info, isDragSplit(), this.mInterceptor.mClientChannel);
                return result;
            }
        }
    }

    private void swapSplitScreen() {
        Slog.d(TAG, "swapSplitScreen");
        MiuiMultiWinReflectStub.getInstance().invoke(this.mAtms.mTaskOrganizerController, "swapSplitTasks", new Object[0]);
    }

    private void replaceOrEnterSplitScreen(int windowModeToChange) {
        ActivityRecord dragActivityRecord = ActivityRecord.forTokenLocked(this.mActivityToken);
        if (dragActivityRecord == null || dragActivityRecord.getTask() == null) {
            Slog.e(TAG, "ActivityRecord or task is null for token " + this.mActivityToken);
            return;
        }
        Task dragRootTask = dragActivityRecord.getRootTask();
        if (dragRootTask == null) {
            Slog.e(TAG, "replaceOrEnterSplitScreen dragRootTask is null");
            return;
        }
        int dragWindowingMode = dragRootTask.getWindowingMode();
        Slog.i(TAG, "dropToSplitScreen windowMode: " + dragWindowingMode + " changeTo " + windowModeToChange + " rootTask: " + dragRootTask);
        Task topRootTask = MiuiMultiWindowManager.getInstance(this.mAtms).getFilteredTopStack(dragActivityRecord.getDisplayContent(), Arrays.asList(5, 2));
        if (topRootTask == null || topRootTask.getActivityType() == 3 || topRootTask.getActivityType() == 2) {
            Slog.w(TAG, "Top rootTask is null or home or recents " + topRootTask);
            return;
        }
        Slog.d(TAG, "switch from freeform to split, top rootTask " + topRootTask);
        if (topRootTask.getWindowingMode() != 1) {
            Slog.w(TAG, "Top rootTask is invalid " + topRootTask);
            return;
        }
        try {
            ((MiuiFreeFormGestureController) MiuiFreeformGestureControllerStub.getInstance()).mGestureListener.resetStateBeforEnterSplit(dragRootTask.getRootTaskId());
            if (topRootTask.mCreatedByOrganizer && topRootTask.hasChild() && topRootTask.getWindowingMode() == 1 && topRootTask.getTopChild() != null && topRootTask.getTopChild().asTask() != null && topRootTask.getTopChild().getWindowingMode() == 6) {
                dragRootTask.moveTaskToBack(dragRootTask);
                this.mAtms.setTaskWindowingMode(dragRootTask.mTaskId, 0, false);
            } else {
                this.mAtms.setTaskWindowingMode(dragRootTask.mTaskId, 0, true);
            }
            this.mAtms.mTaskOrganizerController.dispatchPendingEvents();
            if (windowModeToChange == 3) {
                MiuiMultiWinReflectStub.getInstance().invoke(this.mAtms.mTaskOrganizerController, "enterSplitScreen", new Object[]{Integer.valueOf(dragRootTask.mTaskId), true});
            }
            if (windowModeToChange == 4) {
                MiuiMultiWinReflectStub.getInstance().invoke(this.mAtms.mTaskOrganizerController, "enterSplitScreen", new Object[]{Integer.valueOf(dragRootTask.mTaskId), false});
            }
        } catch (Exception e) {
            Slog.e(TAG, "dropToSplitScreen error", e);
        }
    }

    private void takeSnapshotForFreeformReplaced() {
        synchronized (this.mAtms.getGlobalLock()) {
            ActivityRecord activityRecord = ActivityRecord.forTokenLocked(this.mActivityToken);
            if (activityRecord != null && activityRecord.getDisplayContent() != null) {
                DisplayContent dc = activityRecord.getDisplayContent();
                dc.getDefaultTaskDisplayArea().forAllRootTasks(new Consumer() { // from class: com.android.server.wm.MiuiMultiWindowSwitchManager$$ExternalSyntheticLambda4
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        MiuiMultiWindowSwitchManager.this.m1786x5d217aa9((Task) obj);
                    }
                });
            }
        }
    }

    /* renamed from: lambda$takeSnapshotForFreeformReplaced$5$com-android-server-wm-MiuiMultiWindowSwitchManager */
    public /* synthetic */ void m1786x5d217aa9(Task rootTask) {
        ActivityRecord activityRecord1;
        if (rootTask.inFreeformWindowingMode() && rootTask.isAlwaysOnTop() && (activityRecord1 = rootTask.getTopMostTask().getTopMostActivity()) != null) {
            MiuiMultiWindowManager.getInstance(this.mAtms).takeTaskSnapshot(activityRecord1.token, true);
        }
    }

    public boolean isProcessingDrag() {
        return this.mIsProcessingDrag;
    }

    public void addHotArea(IBinder activityToken, final Bundle info) {
        Slog.d(TAG, "addHotArea token " + activityToken + ", info = " + info);
        if (this.mIsProcessingDrag) {
            Slog.w(TAG, "addHotArea failed, another addHostArea is doing");
            return;
        }
        this.mIsProcessingDrag = true;
        this.mActivityToken = activityToken;
        if (info == null || activityToken == null) {
            Slog.w(TAG, "addHotArea failed, cause info is null");
            this.mIsProcessingDrag = false;
            return;
        }
        this.mIsDropFailedCleanUp = false;
        this.mHasDragStarted = false;
        this.mIsScreenSwitchHandled = false;
        this.isDragAppSupportedFreeform = true;
        this.isFreeformBlackListAppShowToast = false;
        if (isInLazyMode() || isInSubFoldDisplayMode()) {
            this.mIsProcessingDrag = false;
        } else if (!setUpInputSurface()) {
            this.mIsProcessingDrag = false;
            Slog.w(TAG, "addHotArea: set up input surface failed, not to drag!");
        } else if (!getCurrentWindowingMode()) {
            Slog.w(TAG, "addHotArea: get current windowing mode failed, not to drag!");
            cleanUpInputSurface();
            this.mIsProcessingDrag = false;
        } else {
            Rect rect = new Rect();
            this.mNavBarBound = rect;
            this.mNavBarPos = getNavBarBoundOnScreen(rect);
            List<Bitmap> bitmapList = getTopRootTaskSnapShot();
            if (!preStartDragAndDrop(info, bitmapList)) {
                Slog.w(TAG, "addHotArea: preStartDragAndDrop failed, just return!");
                cleanUpInputSurface();
                this.mIsProcessingDrag = false;
                return;
            }
            WindowManager.LayoutParams layoutParams = initHotArea();
            if (layoutParams == null) {
                cleanUpInputSurface();
                this.mIsProcessingDrag = false;
            } else if (bitmapList.size() == 0) {
                Slog.w(TAG, "screenShots empty!");
                cleanUpInputSurface();
                this.mIsProcessingDrag = false;
            } else {
                configBaseRegions(bitmapList);
                this.mRootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() { // from class: com.android.server.wm.MiuiMultiWindowSwitchManager.6
                    @Override // android.view.ViewTreeObserver.OnGlobalLayoutListener
                    public void onGlobalLayout() {
                        MiuiMultiWindowSwitchManager miuiMultiWindowSwitchManager = MiuiMultiWindowSwitchManager.this;
                        if (!miuiMultiWindowSwitchManager.startDragAndDrop(miuiMultiWindowSwitchManager.mRootView, info)) {
                            Slog.w(MiuiMultiWindowSwitchManager.TAG, "startDragAndDrop failed");
                            MiuiMultiWindowSwitchManager.this.mIsProcessingDrag = false;
                        }
                        MiuiMultiWindowSwitchManager.this.cleanUpInputSurface();
                        if (MiuiMultiWindowSwitchManager.this.mRootView != null) {
                            MiuiMultiWindowSwitchManager.this.mRootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        }
                        if (!MiuiMultiWindowSwitchManager.this.mIsProcessingDrag) {
                            MiuiMultiWindowSwitchManager.this.handleRemoveHotArea();
                        }
                    }
                });
                this.mRootView.setOnClickListener(new View.OnClickListener() { // from class: com.android.server.wm.MiuiMultiWindowSwitchManager$$ExternalSyntheticLambda5
                    @Override // android.view.View.OnClickListener
                    public final void onClick(View view) {
                        MiuiMultiWindowSwitchManager.this.m1783xef52d6b3(view);
                    }
                });
                configHotArea(this.mWindowingMode, this.mActivityToken, this.mLeftSplitScreenRegion, this.mRightSplitScreenRegion, bitmapList);
                if (this.mWindowManager == null) {
                    this.mWindowManager = (WindowManager) this.mUiContext.getSystemService("window");
                }
                Slog.d(TAG, "before add hotarea");
                long token = Binder.clearCallingIdentity();
                try {
                    try {
                        this.mWindowManager.addView(this.mRootView, layoutParams);
                        Binder.restoreCallingIdentity(token);
                    } catch (RuntimeException e) {
                        Slog.e(TAG, "addHotArea: addView failed cause exception happened: " + e.toString());
                        cleanUpInputSurface();
                        this.mIsProcessingDrag = false;
                    }
                    Slog.d(TAG, "after add hotarea");
                } finally {
                    Binder.restoreCallingIdentity(token);
                }
            }
        }
    }

    /* renamed from: lambda$addHotArea$6$com-android-server-wm-MiuiMultiWindowSwitchManager */
    public /* synthetic */ void m1783xef52d6b3(View view) {
        handleRemoveHotArea();
    }

    public void handleRemoveHotArea() {
        WindowManager windowManager;
        Slog.d(TAG, "handleRemoveHotArea " + this.mRootView);
        RelativeLayout relativeLayout = this.mRootView;
        if (relativeLayout != null && (windowManager = this.mWindowManager) != null) {
            try {
                windowManager.removeView(relativeLayout);
            } catch (IllegalArgumentException e) {
                Slog.e(TAG, "handleRemoveHotArea " + e.getMessage());
            }
        }
        MiuiMultiWinSplitBarController miuiMultiWinSplitBarController = this.mSplitBarController;
        if (miuiMultiWinSplitBarController != null) {
            miuiMultiWinSplitBarController.releaseSplitBar();
        }
        this.mIsProcessingDrag = false;
        this.mSplitBarContainer = null;
        this.mLeftSplitScreenRegion = null;
        this.mRightSplitScreenRegion = null;
        this.mHotArea = null;
        this.mRootView = null;
        this.mActivityToken = null;
        this.mDragAnimationAdapter = null;
    }

    public boolean isSupportedFreeform(IBinder token) {
        synchronized (this.mAtms.getGlobalLock()) {
            ActivityRecord activityRecord = ActivityRecord.forTokenLocked(token);
            if (activityRecord == null) {
                Slog.e(TAG, "isSupportedFreeform activityRecord is null, token:" + token);
                return false;
            }
            Task task = activityRecord.getTask();
            if (task == null) {
                Slog.e(TAG, "isSupportedFreeform task is null, token:" + token);
                return false;
            }
            return this.mAtms.getTaskResizeableForFreeform(task.mTaskId);
        }
    }

    public boolean checkIfSplitAvailable(IBinder token) {
        return checkIfSplitAvailable(token, true);
    }

    public boolean checkIfSplitAvailable(IBinder token, boolean showToast) {
        synchronized (this.mAtms.getGlobalLock()) {
            ActivityRecord focusedActivityRecord = ActivityRecord.forTokenLocked(token);
            if (focusedActivityRecord == null) {
                Slog.e(TAG, "isSupportedSplit(): focusedActivityRecord is null, token:" + token);
                return false;
            }
            Task topRootTask = MiuiMultiWindowManager.getInstance(this.mAtms).getFilteredTopStack(focusedActivityRecord.getDisplayContent(), Arrays.asList(5, 2));
            if (topRootTask != null) {
                ActivityRecord rootActivityRecord = topRootTask.getTopMostTask().getTopActivity(false, true);
                if (rootActivityRecord == null) {
                    Slog.e(TAG, "isSupportedSplit(): rootActivityRecord is null: " + topRootTask);
                    return false;
                }
                if (!topRootTask.isActivityTypeHome() && !topRootTask.isActivityTypeRecents()) {
                    if (splitNotSupportedFor(focusedActivityRecord)) {
                        if (showToast) {
                            Context context = this.mUiContext;
                            showNotSupportToast(context, getActivityName(context, focusedActivityRecord), true);
                        }
                        return false;
                    } else if (!splitNotSupportedFor(rootActivityRecord) || topRootTask.getTopMostTask().getWindowingMode() == 6) {
                        return true;
                    } else {
                        if (showToast) {
                            Context context2 = this.mUiContext;
                            showNotSupportToast(context2, getActivityName(context2, rootActivityRecord), true);
                        }
                        return false;
                    }
                }
                return false;
            }
            Slog.e(TAG, "isSupportedSplit(): topRootTask is null, focusedActivityRecord: " + focusedActivityRecord);
            return false;
        }
    }

    private boolean splitNotSupportedFor(ActivityRecord activityRecord) {
        return (MiuiMultiWindowAdapter.FREEFORM_RESIZEABLE_WHITE_LIST.contains(activityRecord.packageName) && !ActivityInfo.isResizeableMode(activityRecord.getTask().mResizeMode)) || !activityRecord.getTask().supportsSplitScreenWindowingMode();
    }

    private String getActivityName(Context context, ActivityRecord activityRecord) {
        PackageManager pm = context.getPackageManager();
        if (pm != null) {
            return activityRecord.info.applicationInfo.loadLabel(pm).toString();
        }
        return "";
    }

    private void showNotSupportToastByActivityToken(IBinder token) {
        ActivityRecord activityRecord = ActivityRecord.forTokenLocked(token);
        if (activityRecord == null) {
            Slog.e(TAG, "showNotSupportToastByActivityToken activityRecord is null, token:" + token);
            return;
        }
        String topActivityName = getActivityName(this.mUiContext, activityRecord);
        showNotSupportFreeformToast(this.mUiContext, topActivityName);
    }

    @Override // com.android.server.multiwin.listener.MiuiMultiWinHotAreaConfigListener
    public void onNotSupportAppEnterFreeformRegion(boolean isEnterFreeformRegion) {
        if (isEnterFreeformRegion) {
            if (!this.isFreeformBlackListAppShowToast) {
                this.isFreeformBlackListAppShowToast = true;
                showNotSupportToastByActivityToken(this.mActivityToken);
                return;
            }
            return;
        }
        this.isFreeformBlackListAppShowToast = false;
    }

    @Override // com.android.server.multiwin.listener.DragAnimationListener
    public void onDragStarted() {
        if (this.mIsDragBarReset) {
            Slog.d(TAG, "onDragStarted: reset mIsDragBarReset to false");
            this.mIsDragBarReset = false;
        }
        if (!this.mHasDragStarted) {
            this.mHasDragStarted = true;
            cleanUpInputSurface();
        }
    }

    @Override // com.android.server.multiwin.listener.DragAnimationListener
    public void onDragEntered(View v, DragEvent event, int splitMode, int dragSurfaceAnimType) {
        Slog.d(TAG, "onDragEntered: splitMode = " + splitMode + ", dragSurfaceAnimType = " + dragSurfaceAnimType);
        this.mDragAnimationAdapter.setSplitMode(splitMode);
        handleDragAdapterAnimation(null, dragSurfaceAnimType, false);
    }

    @Override // com.android.server.multiwin.listener.DragAnimationListener
    public void onDragExited(View v) {
    }

    @Override // com.android.server.multiwin.listener.DragAnimationListener
    public void onDragLocation() {
    }

    @Override // com.android.server.multiwin.listener.DragAnimationListener
    public void onDrop(View v, DragEvent event, int splitMode, Rect dropBounds, int dragSurfaceAnimType, boolean isUpDownSplitDrop) {
        Slog.d(TAG, "onDrop: splitMode = " + splitMode + ", dragSurfaceAnimType = " + dragSurfaceAnimType + ",  dropBounds = " + dropBounds);
        this.mLastDropSplitMode = splitMode;
        if (v.getId() == 285868138 || v.getId() == 285868150) {
            Slog.d(TAG, "onDrop: view is left or right screen");
        } else if (v.getId() == 285868136) {
            Slog.i(TAG, "onDrop: view is freeform");
            dropBounds = this.mDropTargetBound;
        } else {
            Slog.i(TAG, "onDrop: view is other splitMode = " + splitMode);
            dropBounds = splitMode == 5 ? this.mDropTargetBound : dropBounds;
        }
        handleDragAdapterAnimation(dropBounds, dragSurfaceAnimType, isUpDownSplitDrop);
        handleSwitchScreen();
        this.mIsDropHandled = true;
    }

    @Override // com.android.server.multiwin.listener.DragAnimationListener
    public void onDragEnded(boolean isSuccessDrop) {
        this.mIsProcessingDrag = false;
        Slog.d(TAG, "onDragEnded isSuccessDrop = " + isSuccessDrop);
        if (isSuccessDrop) {
            addScreenShotCover();
        }
        if (!this.mIsDropHandled && !this.mIsDropFailedCleanUp) {
            Slog.d(TAG, "onDragEnded: do clean up");
            handleRemoveHotArea();
            this.mIsDropFailedCleanUp = true;
        }
    }

    public void removeScreenShotCover() {
        ImageView imageView = this.mScreenShotCover;
        if (imageView != null && this.mWindowManager != null) {
            try {
                imageView.setVisibility(8);
                this.mWindowManager.removeView(this.mScreenShotCover);
                Slog.d(TAG, "removeScreenShotCover " + this.mScreenShotCover);
                this.mScreenShotCover = null;
            } catch (IllegalArgumentException e) {
                Slog.e(TAG, "removeScreenShotCover " + e.getMessage());
            }
        }
    }

    public void removeScreenShotCoverWithAnimation(long delay) {
        ObjectAnimator objectAnimator = this.mScreenShotCoverRemoveAnimator;
        if (objectAnimator != null && objectAnimator.isRunning()) {
            this.mScreenShotCoverRemoveAnimator.cancel();
        }
        ImageView imageView = this.mScreenShotCover;
        if (imageView == null) {
            Slog.w(TAG, "removeScreenShotCoverWithAnimation failed, cause mScreenShotCover is null!");
            return;
        }
        ObjectAnimator ofFloat = ObjectAnimator.ofFloat(imageView, ALPHA_PROPERTY_NAME, imageView.getAlpha(), MiuiFreeformPinManagerService.EDGE_AREA);
        this.mScreenShotCoverRemoveAnimator = ofFloat;
        ofFloat.setDuration(SCREEN_SHOT_COVER_REMOVE_ANIM_DURATION);
        this.mScreenShotCoverRemoveAnimator.setStartDelay(delay);
        this.mScreenShotCoverRemoveAnimator.setInterpolator(new SharpCurveInterpolator());
        this.mScreenShotCoverRemoveAnimator.addListener(new AnimatorListenerAdapter() { // from class: com.android.server.wm.MiuiMultiWindowSwitchManager.7
            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationCancel(Animator param1Animator) {
                MiuiMultiWindowSwitchManager.this.removeScreenShotCover();
            }

            @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator param1Animator) {
                MiuiMultiWindowSwitchManager.this.removeScreenShotCover();
            }
        });
        this.mScreenShotCoverRemoveAnimator.start();
    }

    public int adjustToSplitWindingMode(IBinder iBinder) {
        ActivityRecord activityRecord;
        synchronized (this.mAtms.getGlobalLock()) {
            activityRecord = ActivityRecord.forTokenLocked(iBinder);
        }
        if (activityRecord == null) {
            Slog.e(TAG, "isSupportedSplit activityRecord is null, token:" + iBinder);
            return -1;
        }
        Task rootTask = activityRecord.getTask();
        if (rootTask == null) {
            return -1;
        }
        if (MiuiMultiWindowManager.getInstance(this.mAtms).isSplitPrimaryScreen(rootTask)) {
            return 3;
        }
        return 4;
    }

    public boolean getTouchOffsetInTask(IBinder iBinder, Point point) {
        synchronized (this.mAtms.getGlobalLock()) {
            ActivityRecord activityRecord = ActivityRecord.forTokenLocked(iBinder);
            if (activityRecord == null) {
                Slog.e(TAG, "getTouchOffsetInTask activityRecord is null, token:" + iBinder);
                return false;
            }
            Task touchTask = activityRecord.getTask();
            if (touchTask == null) {
                Slog.e(TAG, "getTouchOffsetInTask touchTask is null, activityRecord:" + activityRecord);
                return false;
            }
            Rect touchTaskBound = touchTask.getBounds();
            point.x -= touchTaskBound.left;
            point.y -= touchTaskBound.top;
            return true;
        }
    }

    public void dump(PrintWriter pw, String[] args, int opti) {
        if (args == null || args.length <= 1) {
            pw.println("dump of multiwindow switch");
            return;
        }
        String next = args[1];
        if ("0".equals(next)) {
            this.mForceSwitchLeftRightSplit = 0;
            this.mIsLeftRightSplit = false;
        } else if (SplitScreenReporter.ACTION_ENTER_SPLIT.equals(next)) {
            this.mForceSwitchLeftRightSplit = 1;
            this.mIsLeftRightSplit = true;
        } else if ("-1".equals(next)) {
            this.mForceSwitchLeftRightSplit = -1;
        }
    }
}
