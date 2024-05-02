package com.android.server.wm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.MiuiMultiWindowUtils;
import android.util.Slog;
import android.view.WindowManager;
import java.util.function.Consumer;
/* loaded from: classes.dex */
public class MiuiFreeFormFlashBackHelper {
    public static final int COMPENSATION_FOR_X_POSITION = 1;
    public static final int COMPENSATION_FOR_Y_POSITION = 4;
    public static final float FLASHBACK_FLOAT_WINDOW_MARGIN_TO_SMALL_WINDOW_RATIO = 0.008547009f;
    public static final int FLASHBACK_LAUNCH_FROM_BACKGROUND_OFFSET = -1000;
    private static final int FLASHBACK_ORIENTATION_LANDSCAPE = 0;
    private static final int FLASHBACK_ORIENTATION_PORTRAIT = 1;
    private static final int FLASHBACK_ORIENTATION_UNSPECIFIED = -1;
    public static final String FLASHBACK_WINDOW_ACTION_TITLE_NAME = "com.miui.flashback.action.view";
    public static final String FLASHBACK_WINDOW_TRAFFIC_TITLE_NAME = "com.miui.flashback.traffic.view";
    static final String TAG = "MiuiFreeFormFlashBackHelper";
    private static final int WINDOW_STATE_SMALL_FREEFORM = 1;
    public static boolean sIsFlashBackMode = false;
    WindowState mFlashBackActionWindow;
    private int mFlashBackAppOrientation = -1;
    private FlashBackReceiver mFlashBackReceiver = new FlashBackReceiver();
    WindowState mFlashBackTrafficWindow;
    private MiuiFreeFormGestureController mGestureController;

    public MiuiFreeFormFlashBackHelper(MiuiFreeFormGestureController gestureController) {
        this.mGestureController = gestureController;
        Context context = gestureController.mService.mContext;
        FlashBackReceiver flashBackReceiver = this.mFlashBackReceiver;
        context.registerReceiver(flashBackReceiver, flashBackReceiver.mFilter);
    }

    public void resetFlashBackWindowIfNeeded() {
        if (this.mFlashBackTrafficWindow != null) {
            this.mGestureController.mGestureListener.mGestureAnimator.resetFlashBackLeash(this.mFlashBackTrafficWindow.mToken);
            this.mFlashBackTrafficWindow = null;
        }
        if (this.mFlashBackActionWindow != null) {
            this.mGestureController.mGestureListener.mGestureAnimator.resetFlashBackLeash(this.mFlashBackActionWindow.mToken);
            this.mFlashBackActionWindow = null;
        }
    }

    public void reparentFlashBackWindowIfNeeded() {
        if (this.mFlashBackTrafficWindow != null) {
            this.mGestureController.mGestureListener.mGestureAnimator.reparentFlashBackLeash(this.mFlashBackTrafficWindow.mToken);
            this.mFlashBackTrafficWindow = null;
        }
        if (this.mFlashBackActionWindow != null) {
            this.mGestureController.mGestureListener.mGestureAnimator.reparentFlashBackLeash(this.mFlashBackActionWindow.mToken);
            this.mFlashBackActionWindow = null;
        }
    }

    public int getMiuiFreeFormPositionX(int orientation) {
        Rect rect;
        if (orientation != 1) {
            rect = findCurrentPosition(this.mGestureController.mService.mContext, true);
        } else {
            rect = findCurrentPosition(this.mGestureController.mService.mContext, false);
        }
        return rect.left;
    }

    public int getMiuiFreeFormPositionY(int orientation) {
        Rect rect;
        if (orientation != 1) {
            rect = findCurrentPosition(this.mGestureController.mService.mContext, true);
        } else {
            rect = findCurrentPosition(this.mGestureController.mService.mContext, false);
        }
        return rect.top;
    }

    public void launchFlashBackFromBackGroundAnim(final int orientation, Handler handler) {
        handler.postDelayed(new Runnable() { // from class: com.android.server.wm.MiuiFreeFormFlashBackHelper.1
            @Override // java.lang.Runnable
            public void run() {
                Rect rect;
                MiuiFreeFormActivityStack mffas = MiuiFreeFormFlashBackHelper.this.mGestureController.mGestureListener.synchronizeFreeFormStackInfo();
                if (mffas == null) {
                    return;
                }
                MiuiFreeFormFlashBackHelper.this.mFlashBackAppOrientation = orientation;
                if (mffas != null) {
                    MiuiFreeFormFlashBackHelper.this.mGestureController.mGestureListener.hideScreenSurface(mffas);
                    if (orientation == 1) {
                        rect = MiuiFreeFormFlashBackHelper.findCurrentPosition(MiuiFreeFormFlashBackHelper.this.mGestureController.mService.mContext, false);
                    } else {
                        rect = MiuiFreeFormFlashBackHelper.findCurrentPosition(MiuiFreeFormFlashBackHelper.this.mGestureController.mService.mContext, true);
                    }
                    MiuiFreeFormFlashBackHelper.this.mGestureController.mGestureListener.mFreeFormWindowMotionHelper.applyFlashBackLaunchFromBackGround(mffas, rect);
                    if (mffas.mStackControlInfo != null) {
                        mffas.mStackControlInfo.mSmallWindowBounds = new Rect(rect);
                    }
                    mffas.setIsLaunchFlashBackFromBackGround(false);
                }
            }
        }, 800L);
    }

    public static int getFloatWindowMarginToSmallWindow(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService("window");
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(outMetrics);
        int screenWidth = outMetrics.widthPixels;
        int screenHeight = outMetrics.heightPixels;
        int screenLongSide = Math.max(screenWidth, screenHeight);
        return (int) (screenLongSide * 0.008547009f);
    }

    public static Rect findCurrentPosition(Context context, boolean isLandcapeFreeform) {
        Slog.d(TAG, "findCurrentPosition isLandcapeFreeform=" + isLandcapeFreeform);
        Rect leftTopWindowBounds = new Rect();
        Rect rightTopWindowBounds = new Rect();
        Rect leftBottomWindowBounds = new Rect();
        Rect rightBottomWindowBounds = new Rect();
        MiuiMultiWindowUtils.calculateCornerBound(context, leftTopWindowBounds, rightTopWindowBounds, leftBottomWindowBounds, rightBottomWindowBounds, isLandcapeFreeform);
        if (MiuiMultiWindowUtils.mCurrentSmallWindowCorner == 1) {
            return leftTopWindowBounds;
        }
        return MiuiMultiWindowUtils.mCurrentSmallWindowCorner == 2 ? rightTopWindowBounds : MiuiMultiWindowUtils.mCurrentSmallWindowCorner == 4 ? rightBottomWindowBounds : leftBottomWindowBounds;
    }

    public void initVisibleFlashBackWindow() {
        this.mGestureController.mMiuiFreeFormManagerService.getAllFreeFormStackInfosOnDisplay(-1);
        if (!MiuiMultiWindowUtils.hasSmallFreeform()) {
            return;
        }
        synchronized (this.mGestureController.mMiuiFreeFormManagerService.mActivityTaskManagerService.mGlobalLock) {
            this.mGestureController.mService.getDefaultDisplayContentLocked().forAllWindows(new Consumer() { // from class: com.android.server.wm.MiuiFreeFormFlashBackHelper$$ExternalSyntheticLambda0
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    MiuiFreeFormFlashBackHelper.this.m1642x8caadbd8((WindowState) obj);
                }
            }, true);
        }
    }

    /* renamed from: lambda$initVisibleFlashBackWindow$0$com-android-server-wm-MiuiFreeFormFlashBackHelper */
    public /* synthetic */ void m1642x8caadbd8(WindowState w) {
        WindowStateAnimator animator;
        if (w == null) {
            return;
        }
        String title = w.mAttrs.getTitle().toString();
        if ((FLASHBACK_WINDOW_TRAFFIC_TITLE_NAME.equals(title) || FLASHBACK_WINDOW_ACTION_TITLE_NAME.equals(title)) && (animator = w.mWinAnimator) != null && animator.hasSurface() && animator.getShown()) {
            MiuiFreeFormActivityStack currentControlActivityStack = this.mGestureController.mMiuiFreeFormManagerService.getTopFreeFormActivityStack();
            Slog.d(TAG, " initVisibleFlashBackWindow: currentControlActivityStack= " + currentControlActivityStack);
            if (FLASHBACK_WINDOW_TRAFFIC_TITLE_NAME.equals(title)) {
                this.mFlashBackTrafficWindow = w;
                this.mGestureController.mGestureListener.mGestureAnimator.createFlashBackLeash(this.mFlashBackTrafficWindow.mToken, currentControlActivityStack);
            }
            if (FLASHBACK_WINDOW_ACTION_TITLE_NAME.equals(title)) {
                this.mFlashBackActionWindow = w;
                this.mGestureController.mGestureListener.mGestureAnimator.createFlashBackLeash(this.mFlashBackActionWindow.mToken, currentControlActivityStack);
            }
        }
    }

    public void stopFlashBackService(final Context context, Handler handler) {
        Slog.d(TAG, "stopFlashBackService");
        handler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreeFormFlashBackHelper.2
            @Override // java.lang.Runnable
            public void run() {
                Intent intent = new Intent();
                intent.setPackage("com.miui.freeform");
                intent.setAction("miui.intent.action.FLASHBACK_WINDOW");
                intent.setClassName("com.miui.freeform", "com.miui.flashback.MiuiFlashbackWindowService");
                context.stopService(intent);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class FlashBackReceiver extends BroadcastReceiver {
        private static final String ACTION_FLASHBACK_LAUNCH_BACKGROUND_ANIM = "miui.intent.action_flashback_launch_background_anim";
        private static final String ACTION_KILL_FLASHBACK_LEASH = "miui.intent.action_kill_flashback_leash";
        private static final String ACTION_REPARENT_FLASHBACK_LEASH = "miui.intent.action_reparent_flashback_leash";
        private IntentFilter mFilter;

        public FlashBackReceiver() {
            MiuiFreeFormFlashBackHelper.this = r2;
            IntentFilter intentFilter = new IntentFilter();
            this.mFilter = intentFilter;
            intentFilter.addAction(ACTION_KILL_FLASHBACK_LEASH);
            this.mFilter.addAction(ACTION_REPARENT_FLASHBACK_LEASH);
            this.mFilter.addAction(ACTION_FLASHBACK_LAUNCH_BACKGROUND_ANIM);
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            handleFlashBackReceiver(intent);
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        private void handleFlashBackReceiver(Intent intent) {
            char c;
            String action = intent.getAction();
            switch (action.hashCode()) {
                case -1335999896:
                    if (action.equals(ACTION_REPARENT_FLASHBACK_LEASH)) {
                        c = 1;
                        break;
                    }
                    c = 65535;
                    break;
                case 744116851:
                    if (action.equals(ACTION_FLASHBACK_LAUNCH_BACKGROUND_ANIM)) {
                        c = 2;
                        break;
                    }
                    c = 65535;
                    break;
                case 1058294185:
                    if (action.equals(ACTION_KILL_FLASHBACK_LEASH)) {
                        c = 0;
                        break;
                    }
                    c = 65535;
                    break;
                default:
                    c = 65535;
                    break;
            }
            switch (c) {
                case 0:
                    MiuiFreeFormFlashBackHelper.this.resetFlashBackWindowIfNeeded();
                    return;
                case 1:
                    MiuiFreeFormFlashBackHelper.this.reparentFlashBackWindowIfNeeded();
                    return;
                case 2:
                    Rect rect = new Rect();
                    switch (MiuiFreeFormFlashBackHelper.this.mFlashBackAppOrientation) {
                        case 0:
                            rect = MiuiFreeFormFlashBackHelper.findCurrentPosition(MiuiFreeFormFlashBackHelper.this.mGestureController.mService.mContext, true);
                            break;
                        case 1:
                            rect = MiuiFreeFormFlashBackHelper.findCurrentPosition(MiuiFreeFormFlashBackHelper.this.mGestureController.mService.mContext, false);
                            break;
                    }
                    MiuiFreeFormActivityStack mffas = MiuiFreeFormFlashBackHelper.this.mGestureController.mMiuiFreeFormManagerService.getTopFreeFormActivityStack();
                    if (mffas != null && mffas.mStackControlInfo != null) {
                        mffas.mStackControlInfo.mSmallWindowBounds = new Rect(rect);
                    }
                    if (mffas != null) {
                        mffas.setIsLaunchFlashBackFromBackGround(false);
                    }
                    MiuiFreeFormFlashBackHelper.this.mFlashBackAppOrientation = -1;
                    return;
                default:
                    return;
            }
        }
    }
}
