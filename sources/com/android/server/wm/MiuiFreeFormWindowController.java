package com.android.server.wm;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.util.Slog;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
/* loaded from: classes.dex */
public class MiuiFreeFormWindowController {
    public static int DropWindowType = -1;
    private static final int HOVER_VELOCITY_THRESHOLD = 1000;
    public static final int LANDCAPE_DROP_DOWN = 1;
    private static final int MIUI_FLAG_IS_MIUI_FREEFORM_OVERLAY = Integer.MIN_VALUE;
    public static final int OPEN_CLOSE_TIP = 1;
    public static final int PORTRAIT_LANDCAPE_DROP__DOWN = 0;
    static final String TAG = "MiuiFreeFormWindowController";
    public static final int UNDEFINED_DROP__DOWN = -1;
    private MiuiFreeformBottomBarHotSpotView mBottomBarHotSpotView;
    private WindowManager.LayoutParams mBottomBarLayoutParams;
    private Context mContext;
    MiuiFreeFormGestureController mGestureController;
    private MiuiFreeFormHotSpotView mHotSpotView;
    private LayoutInflater mInflater;
    private WindowManager.LayoutParams mLayoutParams;
    private WindowManager.LayoutParams mOverlayLayoutParams;
    private MiuiFreeFormOverlayView mOverlayView;
    int mScreenLongSide;
    int mScreenShortSide;
    private View mSmallFreeFormExitRegion;
    private WindowManager.LayoutParams mSmallFreeFormExitRegionLayoutParams;
    private WindowManager.LayoutParams mSwitchOverlayLayoutParams;
    private MiuiFreeFormOverlayView mSwitchOverlayView;
    private volatile View mTipView;
    private WindowManager.LayoutParams mTiplayLayoutParams;
    private WindowManager mWindowManager;
    private final Object mLock = new Object();
    private Rect mTipBounds = new Rect();
    public boolean mShouldRemoveOverlayView = false;
    private boolean mInOverlayOpenAnimation = false;
    private Runnable runnable = new Runnable() { // from class: com.android.server.wm.MiuiFreeFormWindowController.1
        @Override // java.lang.Runnable
        public void run() {
            if (MiuiFreeFormWindowController.this.mOverlayView != null) {
                Slog.d(MiuiFreeFormWindowController.TAG, "startRemoveOverLayViewAnimation");
                MiuiFreeFormWindowController.this.mOverlayView.startRemoveOverLayViewAnimation();
            }
            MiuiFreeFormWindowController.this.mInOverlayOpenAnimation = false;
        }
    };
    private Runnable removeSwitchOverLayAnimationRunnable = new Runnable() { // from class: com.android.server.wm.MiuiFreeFormWindowController.2
        @Override // java.lang.Runnable
        public void run() {
            if (MiuiFreeFormWindowController.this.mSwitchOverlayView != null) {
                Slog.d(MiuiFreeFormWindowController.TAG, "startRemoveSwitchOverLayViewAnimation");
                MiuiFreeFormWindowController.this.mSwitchOverlayView.startRemoveSwitchOverLayViewAnimation();
            }
            MiuiFreeFormWindowController.this.mInOverlayOpenAnimation = false;
        }
    };
    private Runnable resetSwitchAppFourStepRunnable = new Runnable() { // from class: com.android.server.wm.MiuiFreeFormWindowController.3
        @Override // java.lang.Runnable
        public void run() {
            Slog.d(MiuiFreeFormWindowController.TAG, "resetSwitchAppFourStepRunnable");
            MiuiFreeFormWindowController.this.mGestureController.mMiuiFreeFormSwitchAppHelper.startSwitchAppFourStep();
        }
    };
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    public MiuiFreeFormWindowController(Context context, MiuiFreeFormGestureController controller) {
        this.mContext = context;
        this.mGestureController = controller;
        this.mWindowManager = (WindowManager) context.getSystemService("window");
        this.mInflater = (LayoutInflater) context.getSystemService("layout_inflater");
        this.mScreenLongSide = Math.max(controller.mDisplayContent.mBaseDisplayHeight, controller.mDisplayContent.mBaseDisplayWidth);
        this.mScreenShortSide = Math.min(controller.mDisplayContent.mBaseDisplayHeight, controller.mDisplayContent.mBaseDisplayWidth);
    }

    public WindowManager.LayoutParams createLayoutParams() {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(-1, -1, 2021, 1304, 1);
        lp.privateFlags |= 16;
        lp.privateFlags |= 64;
        lp.privateFlags |= 536870912;
        lp.setFitInsetsTypes(0);
        lp.layoutInDisplayCutoutMode = 1;
        lp.gravity = 51;
        lp.y = 0;
        lp.x = 0;
        lp.setTitle("Freeform-HotSpotView");
        return lp;
    }

    public WindowManager.LayoutParams createBottomBarHotSpotLayoutParams() {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(-1, -1, 2021, 1304, 1);
        lp.privateFlags |= 16;
        lp.privateFlags |= 64;
        lp.privateFlags |= 536870912;
        lp.setFitInsetsTypes(0);
        lp.layoutInDisplayCutoutMode = 1;
        lp.gravity = 51;
        lp.y = 0;
        lp.x = 0;
        lp.setTitle("Freeform-BottomBar-HotSpotView");
        return lp;
    }

    public WindowManager.LayoutParams createOverlayLayoutParams() {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(-1, -1, 2021, 1336, 1);
        lp.privateFlags |= 16;
        lp.privateFlags |= 64;
        lp.privateFlags |= 536870912;
        lp.setFitInsetsTypes(0);
        lp.layoutInDisplayCutoutMode = 1;
        lp.extraFlags = 536870912 | lp.extraFlags;
        lp.miuiFlags |= MIUI_FLAG_IS_MIUI_FREEFORM_OVERLAY;
        lp.windowAnimations = -1;
        lp.rotationAnimation = -1;
        lp.gravity = 51;
        lp.y = 0;
        lp.x = 0;
        lp.setTitle("Freeform-OverLayView");
        return lp;
    }

    public WindowManager.LayoutParams createSwitchOverlayLayoutParams() {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(-1, -1, 2021, 1336, 1);
        lp.privateFlags |= 16;
        lp.extraFlags |= 536870912;
        lp.miuiFlags |= MIUI_FLAG_IS_MIUI_FREEFORM_OVERLAY;
        lp.privateFlags |= 64;
        lp.privateFlags |= 536870912;
        lp.setFitInsetsTypes(0);
        lp.layoutInDisplayCutoutMode = 1;
        lp.gravity = 51;
        lp.y = 0;
        lp.x = 0;
        lp.setTitle("Freeform-Switch-OverLayView");
        return lp;
    }

    public WindowManager.LayoutParams createTipLayoutParams(Point position, Point size) {
        int width = size.x;
        int height = size.y;
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(width, height, 2008, 1288, 1);
        lp.gravity = 51;
        lp.y = position.y;
        lp.x = position.x;
        lp.windowAnimations = 286261255;
        lp.setTitle("Freeform-TipView");
        return lp;
    }

    public WindowManager.LayoutParams createExitLayoutParams(Point position, Point size) {
        int width = size.x;
        int height = size.y;
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(width, height, 2008, 1288, 1);
        lp.gravity = 51;
        lp.y = position.y;
        lp.x = position.x;
        lp.setTitle("Small-Freeform-Exit-View");
        return lp;
    }

    public void startRemoveOverLayViewAnimation(int delayed) {
        MiuiFreeFormOverlayView miuiFreeFormOverlayView = this.mOverlayView;
        if (miuiFreeFormOverlayView != null) {
            miuiFreeFormOverlayView.mRemoveAnimationToDo = true;
        }
        this.mainHandler.removeCallbacks(this.runnable);
        this.mainHandler.postDelayed(this.runnable, delayed);
    }

    public void startRemoveOverLayViewIfNeeded() {
        if (this.mainHandler.hasCallbacks(this.runnable)) {
            startRemoveOverLayViewAnimation(60);
        }
    }

    public void startRemoveSwitchOverLayViewAnimation(int delay) {
        this.mainHandler.removeCallbacks(this.removeSwitchOverLayAnimationRunnable);
        this.mainHandler.postDelayed(this.removeSwitchOverLayAnimationRunnable, delay);
    }

    public void startSwitchAppFourStepDelay(int delay) {
        this.mainHandler.removeCallbacks(this.resetSwitchAppFourStepRunnable);
        this.mainHandler.postDelayed(this.resetSwitchAppFourStepRunnable, delay);
    }

    public void startBorderAlphaHideAnimation() {
        Slog.d(TAG, "startBorderAlphaHideAnimation");
        this.mainHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreeFormWindowController.4
            @Override // java.lang.Runnable
            public void run() {
                if (MiuiFreeFormWindowController.this.mOverlayView != null) {
                    MiuiFreeFormWindowController.this.mOverlayView.startBorderAlphaHideAnimation();
                }
            }
        });
    }

    public void addSwitchOverlayView() {
        Slog.d(TAG, "addSwitchOverlayView");
        this.mainHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreeFormWindowController.5
            @Override // java.lang.Runnable
            public void run() {
                try {
                    if (MiuiFreeFormWindowController.this.mSwitchOverlayView != null) {
                        try {
                            MiuiFreeFormWindowController.this.mWindowManager.removeView(MiuiFreeFormWindowController.this.mSwitchOverlayView);
                            MiuiFreeFormWindowController.this.mSwitchOverlayLayoutParams = null;
                            MiuiFreeFormWindowController.this.mSwitchOverlayView = null;
                        } catch (Exception e) {
                            MiuiFreeFormWindowController.this.mSwitchOverlayLayoutParams = null;
                            MiuiFreeFormWindowController.this.mSwitchOverlayView = null;
                        }
                    }
                    MiuiFreeFormWindowController miuiFreeFormWindowController = MiuiFreeFormWindowController.this;
                    miuiFreeFormWindowController.mSwitchOverlayView = (MiuiFreeFormOverlayView) miuiFreeFormWindowController.mInflater.inflate(285999121, (ViewGroup) null);
                    MiuiFreeFormWindowController.this.mSwitchOverlayView.setTag("SwitchOverlayView");
                    MiuiFreeFormWindowController miuiFreeFormWindowController2 = MiuiFreeFormWindowController.this;
                    miuiFreeFormWindowController2.mSwitchOverlayLayoutParams = miuiFreeFormWindowController2.createSwitchOverlayLayoutParams();
                    MiuiFreeFormWindowController.this.mWindowManager.addView(MiuiFreeFormWindowController.this.mSwitchOverlayView, MiuiFreeFormWindowController.this.mSwitchOverlayLayoutParams);
                    MiuiFreeFormWindowController.this.mSwitchOverlayView.setController(MiuiFreeFormWindowController.this);
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
        });
    }

    public void removeSwitchOverlayView() {
        Slog.d(TAG, "removeSwitchOverlayView");
        this.mainHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreeFormWindowController.6
            @Override // java.lang.Runnable
            public void run() {
                try {
                    if (MiuiFreeFormWindowController.this.mSwitchOverlayView != null) {
                        MiuiFreeFormWindowController.this.mWindowManager.removeView(MiuiFreeFormWindowController.this.mSwitchOverlayView);
                        MiuiFreeFormWindowController.this.mSwitchOverlayLayoutParams = null;
                        MiuiFreeFormWindowController.this.mSwitchOverlayView = null;
                    }
                } catch (Exception e) {
                    MiuiFreeFormWindowController.this.mSwitchOverlayLayoutParams = null;
                    MiuiFreeFormWindowController.this.mSwitchOverlayView = null;
                }
            }
        });
    }

    public void addOverlayView() {
        Slog.d(TAG, "addOverlayView");
        this.mainHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreeFormWindowController.7
            @Override // java.lang.Runnable
            public void run() {
                try {
                    if (MiuiFreeFormWindowController.this.mOverlayView == null) {
                        MiuiFreeFormWindowController miuiFreeFormWindowController = MiuiFreeFormWindowController.this;
                        miuiFreeFormWindowController.mOverlayView = (MiuiFreeFormOverlayView) miuiFreeFormWindowController.mInflater.inflate(285999121, (ViewGroup) null);
                        MiuiFreeFormWindowController.this.mOverlayView.setTag("OverlayView");
                        MiuiFreeFormWindowController miuiFreeFormWindowController2 = MiuiFreeFormWindowController.this;
                        miuiFreeFormWindowController2.mOverlayLayoutParams = miuiFreeFormWindowController2.createOverlayLayoutParams();
                        MiuiFreeFormWindowController.this.mWindowManager.addView(MiuiFreeFormWindowController.this.mOverlayView, MiuiFreeFormWindowController.this.mOverlayLayoutParams);
                        MiuiFreeFormWindowController.this.mOverlayView.setController(MiuiFreeFormWindowController.this);
                        MiuiFreeFormWindowController.this.hideOverlayView();
                    }
                } catch (Exception e) {
                }
            }
        });
    }

    public void updateScreenParams(DisplayContent displayContent, Configuration configuration) {
        this.mScreenLongSide = Math.max(displayContent.mBaseDisplayHeight, displayContent.mBaseDisplayWidth);
        this.mScreenShortSide = Math.min(displayContent.mBaseDisplayHeight, displayContent.mBaseDisplayWidth);
    }

    public void updateOvleryView(int orientation) {
        Slog.d(TAG, "updateOvleryView mOverlayLayoutParams:" + this.mOverlayLayoutParams);
        WindowManager.LayoutParams layoutParams = this.mOverlayLayoutParams;
        if (layoutParams != null && this.mOverlayView != null) {
            if (orientation == 1) {
                layoutParams.width = this.mScreenShortSide;
                this.mOverlayLayoutParams.height = this.mScreenLongSide;
            } else {
                layoutParams.width = this.mScreenLongSide;
                this.mOverlayLayoutParams.height = this.mScreenShortSide;
            }
            this.mWindowManager.updateViewLayout(this.mOverlayView, this.mOverlayLayoutParams);
        }
    }

    public void removeOverlayView() {
        Slog.d(TAG, "removeOverlayView");
        this.mainHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreeFormWindowController.8
            @Override // java.lang.Runnable
            public void run() {
                try {
                    if (MiuiFreeFormWindowController.this.mOverlayView != null && MiuiFreeFormWindowController.this.mOverlayView.getVisibility() != 0) {
                        MiuiFreeFormWindowController.this.mWindowManager.removeView(MiuiFreeFormWindowController.this.mOverlayView);
                        MiuiFreeFormWindowController.this.mOverlayLayoutParams = null;
                        MiuiFreeFormWindowController.this.mOverlayView = null;
                    }
                } catch (Exception e) {
                    MiuiFreeFormWindowController.this.mOverlayLayoutParams = null;
                    MiuiFreeFormWindowController.this.mOverlayView = null;
                }
            }
        });
    }

    public void removeOverlayViewChecked() {
        MiuiFreeFormOverlayView miuiFreeFormOverlayView = this.mOverlayView;
        if (miuiFreeFormOverlayView == null) {
            return;
        }
        if (miuiFreeFormOverlayView.removeAnimationToDo()) {
            this.mShouldRemoveOverlayView = true;
        } else {
            removeOverlayView();
        }
    }

    public void startBorderAnimation(final boolean appear) {
        Slog.d(TAG, "startBorderAnimation appear: " + appear);
        this.mainHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreeFormWindowController.9
            @Override // java.lang.Runnable
            public void run() {
                if (MiuiFreeFormWindowController.this.mOverlayView != null) {
                    MiuiFreeFormWindowController.this.mOverlayView.startBorderAnimation(appear);
                }
            }
        });
    }

    public void setStartBounds(final Rect contentBounds) {
        Slog.d(TAG, "setContentBounds=" + contentBounds);
        if (contentBounds == null) {
            return;
        }
        this.mainHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreeFormWindowController.10
            @Override // java.lang.Runnable
            public void run() {
                if (MiuiFreeFormWindowController.this.mOverlayView != null) {
                    MiuiFreeFormWindowController.this.mOverlayView.setStartBounds(contentBounds);
                }
            }
        });
    }

    public void setSwitchOverlayViewBounds(final Rect contentBounds) {
        Slog.d(TAG, "setContentBounds=" + contentBounds);
        if (contentBounds == null) {
            return;
        }
        this.mainHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreeFormWindowController.11
            @Override // java.lang.Runnable
            public void run() {
                if (MiuiFreeFormWindowController.this.mSwitchOverlayView != null) {
                    MiuiFreeFormWindowController.this.mSwitchOverlayView.setStartBounds(contentBounds);
                }
            }
        });
    }

    public void startSwitchOverlayContentAnimation(final int animationType, final String packageName, final MiuiFreeFormActivityStack mffas) {
        Slog.d(TAG, "startContentAnimation");
        this.mainHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreeFormWindowController.12
            @Override // java.lang.Runnable
            public void run() {
                if (MiuiFreeFormWindowController.this.mSwitchOverlayView != null) {
                    MiuiFreeFormWindowController.this.mInOverlayOpenAnimation = true;
                    MiuiFreeFormWindowController.this.mSwitchOverlayView.startContentAnimation(animationType, packageName, -1, mffas);
                }
            }
        });
    }

    public void startContentAnimation(final int animationType, final String packageName, final int action, final MiuiFreeFormActivityStack mffas) {
        Slog.d(TAG, "startContentAnimation");
        this.mainHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreeFormWindowController.13
            @Override // java.lang.Runnable
            public void run() {
                if (MiuiFreeFormWindowController.this.mOverlayView != null) {
                    MiuiFreeFormWindowController.this.mInOverlayOpenAnimation = true;
                    MiuiFreeFormWindowController.this.mOverlayView.startContentAnimation(animationType, packageName, action, mffas);
                }
            }
        });
    }

    public boolean inOverlayOpenAnimation() {
        MiuiFreeFormOverlayView miuiFreeFormOverlayView;
        MiuiFreeFormOverlayView miuiFreeFormOverlayView2;
        return this.mInOverlayOpenAnimation && (((miuiFreeFormOverlayView = this.mOverlayView) != null && miuiFreeFormOverlayView.getVisibility() == 0) || ((miuiFreeFormOverlayView2 = this.mSwitchOverlayView) != null && miuiFreeFormOverlayView2.getVisibility() == 0));
    }

    public void setDisableScreenRotation(boolean disableScreenRotation) {
        this.mGestureController.setDisableScreenRotation(disableScreenRotation);
    }

    public boolean isScreenRotationDisabled() {
        return this.mGestureController.isScreenRotationDisabled();
    }

    public void startShowFullScreenWindow(int action, MiuiFreeFormActivityStack stack) {
        this.mGestureController.startShowFullScreenWindow(action, stack);
    }

    public void showOverlayView() {
        Slog.d(TAG, "showOverlayView");
        this.mainHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreeFormWindowController.14
            @Override // java.lang.Runnable
            public void run() {
                if (MiuiFreeFormWindowController.this.mOverlayView != null) {
                    MiuiFreeFormWindowController.this.mOverlayView.show();
                }
            }
        });
    }

    public void showSwitchOverlayView() {
        Slog.d(TAG, "showSwitchOverlayView");
        this.mainHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreeFormWindowController.15
            @Override // java.lang.Runnable
            public void run() {
                if (MiuiFreeFormWindowController.this.mSwitchOverlayView != null) {
                    MiuiFreeFormWindowController.this.mSwitchOverlayView.show();
                }
            }
        });
    }

    public void hideOverlayView() {
        Slog.d(TAG, "hideOverlayView");
        this.mainHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreeFormWindowController.16
            @Override // java.lang.Runnable
            public void run() {
                if (MiuiFreeFormWindowController.this.mOverlayView != null) {
                    MiuiFreeFormWindowController.this.mOverlayView.hide();
                }
            }
        });
    }

    public void hideSwitchOverlayView() {
        Slog.d(TAG, "hideSwitchOverlayView");
        this.mainHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreeFormWindowController.17
            @Override // java.lang.Runnable
            public void run() {
                if (MiuiFreeFormWindowController.this.mSwitchOverlayView != null) {
                    MiuiFreeFormWindowController.this.mSwitchOverlayView.hide();
                }
            }
        });
    }

    public void addHotSpotView() {
        Slog.d(TAG, "addHotSpotView");
        this.mainHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreeFormWindowController.18
            @Override // java.lang.Runnable
            public void run() {
                if (MiuiFreeFormWindowController.this.mHotSpotView == null) {
                    MiuiFreeFormWindowController miuiFreeFormWindowController = MiuiFreeFormWindowController.this;
                    miuiFreeFormWindowController.mLayoutParams = miuiFreeFormWindowController.createLayoutParams();
                    MiuiFreeFormWindowController.this.mHotSpotView = new MiuiFreeFormHotSpotView(MiuiFreeFormWindowController.this.mContext);
                    MiuiFreeFormWindowController.this.mWindowManager.addView(MiuiFreeFormWindowController.this.mHotSpotView, MiuiFreeFormWindowController.this.mLayoutParams);
                    MiuiFreeFormWindowController.this.mHotSpotView.hide();
                }
            }
        });
    }

    public void removeHotSpotView() {
        Slog.d(TAG, "removeHotSpotView");
        this.mainHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreeFormWindowController.19
            @Override // java.lang.Runnable
            public void run() {
                try {
                    if (MiuiFreeFormWindowController.this.mHotSpotView != null) {
                        MiuiFreeFormWindowController.this.mWindowManager.removeView(MiuiFreeFormWindowController.this.mHotSpotView);
                        MiuiFreeFormWindowController.this.mHotSpotView = null;
                        MiuiFreeFormWindowController.this.mLayoutParams = null;
                    }
                } catch (Exception e) {
                    MiuiFreeFormWindowController.this.mHotSpotView = null;
                    MiuiFreeFormWindowController.this.mLayoutParams = null;
                }
            }
        });
    }

    public void addBottomBarHotSpotView() {
        Slog.d(TAG, "addBottomHotSpotView");
        this.mainHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreeFormWindowController.20
            @Override // java.lang.Runnable
            public void run() {
                if (MiuiFreeFormWindowController.this.mBottomBarHotSpotView == null) {
                    MiuiFreeFormWindowController miuiFreeFormWindowController = MiuiFreeFormWindowController.this;
                    miuiFreeFormWindowController.mBottomBarLayoutParams = miuiFreeFormWindowController.createBottomBarHotSpotLayoutParams();
                    MiuiFreeFormWindowController miuiFreeFormWindowController2 = MiuiFreeFormWindowController.this;
                    miuiFreeFormWindowController2.mBottomBarHotSpotView = (MiuiFreeformBottomBarHotSpotView) miuiFreeFormWindowController2.mInflater.inflate(285999118, (ViewGroup) null);
                    MiuiFreeFormWindowController.this.mWindowManager.addView(MiuiFreeFormWindowController.this.mBottomBarHotSpotView, MiuiFreeFormWindowController.this.mBottomBarLayoutParams);
                }
            }
        });
    }

    public void removeBottomBarHotSpotView() {
        Slog.d(TAG, "removeBottomHotSpotView");
        this.mainHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreeFormWindowController.21
            @Override // java.lang.Runnable
            public void run() {
                try {
                    if (MiuiFreeFormWindowController.this.mBottomBarHotSpotView != null) {
                        MiuiFreeFormWindowController.this.mWindowManager.removeView(MiuiFreeFormWindowController.this.mBottomBarHotSpotView);
                        MiuiFreeFormWindowController.this.mBottomBarHotSpotView = null;
                        MiuiFreeFormWindowController.this.mBottomBarLayoutParams = null;
                    }
                } catch (Exception e) {
                    MiuiFreeFormWindowController.this.mBottomBarHotSpotView = null;
                    MiuiFreeFormWindowController.this.mBottomBarLayoutParams = null;
                }
            }
        });
    }

    public void updateState(final int hotSpotNum, final int targetState, final float x, final float y) {
        Slog.d(TAG, "updateState hotSpotNum: " + hotSpotNum + " targetState: " + targetState + " x: " + x + " y: " + y);
        this.mainHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreeFormWindowController.22
            @Override // java.lang.Runnable
            public void run() {
                if (MiuiFreeFormWindowController.this.mBottomBarHotSpotView != null) {
                    MiuiFreeFormWindowController.this.mBottomBarHotSpotView.updateState(hotSpotNum, targetState, x, y);
                }
            }
        });
    }

    public void showBottomBarHotSpotView() {
        Slog.d(TAG, "showBottomBarHotSpotView");
        this.mainHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreeFormWindowController.23
            @Override // java.lang.Runnable
            public void run() {
                if (MiuiFreeFormWindowController.this.mBottomBarHotSpotView != null) {
                    MiuiFreeFormWindowController.this.mBottomBarHotSpotView.show();
                }
            }
        });
    }

    public void hideBottomBarHotSpotView() {
        Slog.d(TAG, "hideBottomBarHotSpotView");
        this.mainHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreeFormWindowController.24
            @Override // java.lang.Runnable
            public void run() {
                if (MiuiFreeFormWindowController.this.mBottomBarHotSpotView != null) {
                    MiuiFreeFormWindowController.this.mBottomBarHotSpotView.hide();
                }
            }
        });
    }

    public void showHotSpotView() {
        Slog.d(TAG, "showHotSpotView");
        this.mainHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreeFormWindowController.25
            @Override // java.lang.Runnable
            public void run() {
                if (MiuiFreeFormWindowController.this.mHotSpotView != null) {
                    MiuiFreeFormWindowController.this.mHotSpotView.show();
                }
            }
        });
    }

    public void hideHotSpotView() {
        Slog.d(TAG, "hideHotSpotView");
        this.mainHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreeFormWindowController.26
            @Override // java.lang.Runnable
            public void run() {
                if (MiuiFreeFormWindowController.this.mHotSpotView != null) {
                    MiuiFreeFormWindowController.this.mHotSpotView.hide();
                }
            }
        });
    }

    public void enterSmallWindow() {
        Slog.d(TAG, "enterSmallWindow");
        this.mainHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreeFormWindowController.27
            @Override // java.lang.Runnable
            public void run() {
                if (MiuiFreeFormWindowController.this.mHotSpotView != null) {
                    MiuiFreeFormWindowController.this.mHotSpotView.enterSmallWindow();
                }
            }
        });
    }

    public void outSmallWindow() {
        Slog.d(TAG, "outSmallWindow");
        this.mainHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreeFormWindowController.28
            @Override // java.lang.Runnable
            public void run() {
                if (MiuiFreeFormWindowController.this.mHotSpotView != null) {
                    MiuiFreeFormWindowController.this.mHotSpotView.outSmallWindow();
                }
            }
        });
    }

    public void inHotSpotArea(final int hotSpotNum, final float x, final float y) {
        Slog.d(TAG, "inHotSpotArea");
        this.mainHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreeFormWindowController.29
            @Override // java.lang.Runnable
            public void run() {
                if (MiuiFreeFormWindowController.this.mHotSpotView != null) {
                    MiuiFreeFormWindowController.this.mHotSpotView.inHotSpotArea(hotSpotNum, x, y);
                }
            }
        });
    }

    public void removeOpenCloseTipWindow() {
        this.mainHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreeFormWindowController.30
            @Override // java.lang.Runnable
            public void run() {
                if (MiuiFreeFormWindowController.this.mTipView != null) {
                    synchronized (MiuiFreeFormWindowController.this.mLock) {
                        if (MiuiFreeFormWindowController.this.mTipView != null) {
                            try {
                                MiuiFreeFormWindowController.this.mWindowManager.removeView(MiuiFreeFormWindowController.this.mTipView);
                                MiuiFreeFormWindowController.this.mTipView = null;
                                MiuiFreeFormWindowController.this.mTiplayLayoutParams = null;
                                MiuiFreeFormWindowController.this.mTipBounds.setEmpty();
                                Slog.d(MiuiFreeFormWindowController.TAG, "removeOpenCloseTipWindow");
                            } catch (Exception e) {
                                Slog.d(MiuiFreeFormWindowController.TAG, "removeOpenCloseTipWindow failed: " + e);
                                MiuiFreeFormWindowController.this.mTipView = null;
                                MiuiFreeFormWindowController.this.mTiplayLayoutParams = null;
                                MiuiFreeFormWindowController.this.mTipBounds.setEmpty();
                            }
                        }
                    }
                }
            }
        });
    }

    public boolean inTipViewBounds(MotionEvent motionEvent) {
        synchronized (this.mLock) {
            if (this.mTipView != null && motionEvent != null) {
                try {
                    int x = (int) motionEvent.getX();
                    int y = (int) motionEvent.getY();
                    if (this.mTipBounds.contains(x, y)) {
                        return true;
                    }
                } catch (Exception e) {
                }
            }
            return false;
        }
    }
}
