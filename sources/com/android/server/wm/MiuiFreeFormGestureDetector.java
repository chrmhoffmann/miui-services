package com.android.server.wm;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.Region;
import android.os.Vibrator;
import android.util.MiuiMultiWindowUtils;
import android.util.Slog;
import android.view.Display;
import android.view.DisplayCutout;
import android.view.InsetsSource;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import java.util.ArrayList;
import java.util.List;
import miui.util.HapticFeedbackUtil;
/* loaded from: classes.dex */
public class MiuiFreeFormGestureDetector {
    public static final String COM_TENCENT_TMGP_PUBGMHD_NAME = "com.epicgames.ue4.SplashActivity";
    public static final String CONFIRM_ACCESSCONTROL_NAME = "com.miui.applicationlock.ConfirmAccessControl";
    private static final float DRAGLOG = -16.8f;
    public static final String FAMILYSMILE_ACTIVITY = "jp.netstar.familysmile.appwatch.AppWatchBlockActivity";
    public static final String FAMILYSMILE_PACKAGE = "jp.netstar.familysmile";
    public static final String FOCUS_MODE_STATUS = "settings_focus_mode_status";
    private static final float FRICTION = 4.0f;
    public static final String INPUT_METHOD_WINDOW_TITLE_NAME = "InputMethod";
    public static final String MM_LOGIN_NAME = "com.tencent.mm.plugin.account.ui.WelcomeActivity";
    public static final String PARENTALCONTROLS_ACTIVITY = "jp.co.daj.consumer.ifilter.blocker.BlockActivity";
    public static final String PARENTALCONTROLS_PACKAGE = "jp.softbank.mb.parentalcontrols";
    public static final String SYSTEMUI_PACKAGE_NAME = "com.android.systemui";
    public static final String SYSTEM_APPPERMISSION_DIALOGACTIVITY = "com.miui.permcenter.permissions.SystemAppPermissionDialogActivity";
    public static final String TAG = "MiuiFreeFormGestureDetector";
    public static final String TRANSPARENT_THEME_PACKAGE_NAME = "com.android.thememanager";
    private static final int VIBRATE_LIGHT_TIME = 50;
    private HapticFeedbackUtil mHapticFeedbackUtil;
    private MiuiFreeFormGesturePointerEventListener mListener;
    private static boolean DEBUG = MiuiFreeFormGestureController.DEBUG;
    private static List<String> mReportEventList = new ArrayList();

    public MiuiFreeFormGestureDetector(MiuiFreeFormGesturePointerEventListener listener) {
        this.mListener = listener;
        this.mHapticFeedbackUtil = new HapticFeedbackUtil(this.mListener.mService.mContext, false);
    }

    public static float getPredictMoveDistance(float velocity) {
        return (-velocity) / DRAGLOG;
    }

    public boolean passedSlop(float x, float y, float startDragX, float startDragY) {
        int dragSlop = ViewConfiguration.get(this.mListener.mService.mContext).getScaledTouchSlop();
        return Math.abs(x - startDragX) > ((float) dragSlop) || Math.abs(y - startDragY) > ((float) dragSlop);
    }

    public static int getScreenRoundCornerRadiusTop(Context context) {
        int radius;
        Display display = context.getDisplayNoVerify();
        if (display != null && display.getRoundedCorner(0) != null && (radius = display.getRoundedCorner(0).getRadius()) > 0) {
            return radius;
        }
        int resourceId = context.getResources().getIdentifier("rounded_corner_radius_top", "dimen", "android");
        if (resourceId > 0) {
            return context.getResources().getDimensionPixelSize(resourceId);
        }
        return -1;
    }

    public static int getScreenRoundCornerRadiusBottom(Context context) {
        int radius;
        Display display = context.getDisplayNoVerify();
        if (display != null && display.getRoundedCorner(3) != null && (radius = display.getRoundedCorner(3).getRadius()) > 0) {
            return radius;
        }
        int resourceId = context.getResources().getIdentifier("rounded_corner_radius_bottom", "dimen", "android");
        if (resourceId > 0) {
            return context.getResources().getDimensionPixelSize(resourceId);
        }
        return -1;
    }

    public static boolean interceptTouchEvent(List<WindowState> windows, MotionEvent motionEvent) {
        try {
            for (WindowState w : windows) {
                Region region = new Region();
                w.getTouchableRegion(region);
                int downX = (int) motionEvent.getX();
                int downY = (int) motionEvent.getY();
                if (region.contains(downX, downY)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    public void hapticFeedback(int flag, boolean always) {
        if (MiuiMultiWindowUtils.isSupportLatestVibrate()) {
            this.mHapticFeedbackUtil.performExtHapticFeedback(((Integer) MiuiMultiWindowUtils.PREV_HAPTIC_FEEDBACK_ID_TO_NEW_ID.get(Integer.valueOf(flag))).intValue());
        } else if (HapticFeedbackUtil.isSupportLinearMotorVibrate(flag)) {
            this.mHapticFeedbackUtil.performHapticFeedback(flag, always);
        } else {
            vibrateLight(this.mListener.mService.mContext);
        }
    }

    private void vibrateLight(Context context) {
        Vibrator vibrator = (Vibrator) context.getSystemService("vibrator");
        if (vibrator != null) {
            vibrator.vibrate(50L);
        }
    }

    public static int getStatusBarHeight(InsetsStateController insetsStateController) {
        return getStatusBarHeight(insetsStateController, true);
    }

    public static int getStatusBarHeight(InsetsStateController insetsStateController, boolean ignoreVisibility) {
        Rect frame;
        if (insetsStateController == null) {
            return 0;
        }
        int statusBarHeight = 0;
        WindowContainerInsetsSourceProvider sourceProvider = insetsStateController.getSourceProvider(0);
        if (sourceProvider != null) {
            InsetsSource insetsSource = sourceProvider.getSource();
            if ((ignoreVisibility || insetsSource.isVisible()) && (frame = insetsSource.getFrame()) != null && !frame.isEmpty()) {
                statusBarHeight = frame.height();
            }
        }
        if (DEBUG) {
            Slog.d(TAG, "getStatusBarHeight statusBarHeight=" + statusBarHeight);
        }
        return statusBarHeight;
    }

    public static int getNavBarHeight(InsetsStateController insetsStateController) {
        return getNavBarHeight(insetsStateController, true);
    }

    public static int getNavBarHeight(InsetsStateController insetsStateController, boolean ignoreVisibility) {
        Rect frame;
        if (insetsStateController == null) {
            return 0;
        }
        int navBarHeight = 0;
        WindowContainerInsetsSourceProvider sourceProvider = insetsStateController.getSourceProvider(1);
        if (sourceProvider != null) {
            InsetsSource insetsSource = sourceProvider.getSource();
            if ((ignoreVisibility || insetsSource.isVisible()) && (frame = insetsSource.getFrame()) != null && !frame.isEmpty()) {
                navBarHeight = Math.min(frame.height(), frame.width());
            }
        }
        if (DEBUG) {
            Slog.d(TAG, "getNavBarHeight navBarHeight=" + navBarHeight);
        }
        return navBarHeight;
    }

    public static int getDisplayCutoutHeight(DisplayFrames displayFrames) {
        if (displayFrames == null) {
            return 0;
        }
        int displayCutoutHeight = 0;
        DisplayCutout cutout = displayFrames.mInsetsState.getDisplayCutout();
        if (displayFrames.mRotation == 0) {
            displayCutoutHeight = cutout.getSafeInsetTop();
        } else if (displayFrames.mRotation == 2) {
            displayCutoutHeight = cutout.getSafeInsetBottom();
        } else if (displayFrames.mRotation == 1) {
            displayCutoutHeight = cutout.getSafeInsetLeft();
        } else if (displayFrames.mRotation == 3) {
            displayCutoutHeight = cutout.getSafeInsetRight();
        }
        if (DEBUG) {
            Slog.d(TAG, "getDisplayCutoutHeight displayCutoutHeight=" + displayCutoutHeight);
        }
        return displayCutoutHeight;
    }
}
