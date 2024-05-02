package com.miui.server.input.knock.checker;

import android.app.ActivityThread;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.hardware.display.BrightnessInfo;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.MathUtils;
import android.util.Slog;
import android.view.MotionEvent;
import com.android.server.wm.MiuiFreeformPinManagerService;
import com.miui.server.input.knock.KnockGestureChecker;
import com.miui.server.input.util.ShortCutActionsUtils;
import miui.util.HapticFeedbackUtil;
/* loaded from: classes.dex */
public class KnockGestureHorizontalBrightness extends KnockGestureChecker {
    private static final int BRIGHTNESS_NO_CHANGE = -1;
    private static final int LONG_PRESS_TIME = 500;
    private static final String TAG = "KnockBrightness";
    private int mBrightnessViewLength;
    private final DisplayManager mDisplayManager;
    private DisplayMetrics mDisplayMetrics;
    private float mDownX;
    private float mDownY;
    private HapticFeedbackUtil mHapticFeedbackUtil;
    private float minX;
    private float mNowBrightness = -1.0f;
    private float mDownBrightness = -1.0f;
    private float mBrightnessMin = MiuiFreeformPinManagerService.EDGE_AREA;
    private float mBrightnessMax = 1.0f;
    private BroadcastReceiver mConfigurationReceiver = new BroadcastReceiver() { // from class: com.miui.server.input.knock.checker.KnockGestureHorizontalBrightness.1
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            KnockGestureHorizontalBrightness.this.mDisplayMetrics = context.getResources().getDisplayMetrics();
            KnockGestureHorizontalBrightness knockGestureHorizontalBrightness = KnockGestureHorizontalBrightness.this;
            knockGestureHorizontalBrightness.mBrightnessViewLength = (int) (Math.min(knockGestureHorizontalBrightness.mDisplayMetrics.heightPixels, KnockGestureHorizontalBrightness.this.mDisplayMetrics.widthPixels) * 0.4676d);
        }
    };
    private Runnable mLongPressRunnable = new Runnable() { // from class: com.miui.server.input.knock.checker.KnockGestureHorizontalBrightness.2
        @Override // java.lang.Runnable
        public void run() {
            KnockGestureHorizontalBrightness.this.mCheckState = 2;
            KnockGestureHorizontalBrightness.this.mKnockPathListener.hideView();
            KnockGestureHorizontalBrightness.this.mHapticFeedbackUtil.performHapticFeedback("virtual_key_longpress", false);
        }
    };
    private Handler mHandler = new Handler();

    public KnockGestureHorizontalBrightness(Context context) {
        super(context);
        this.mDisplayManager = (DisplayManager) context.getSystemService(DisplayManager.class);
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        this.mDisplayMetrics = displayMetrics;
        this.mBrightnessViewLength = (int) (Math.min(displayMetrics.heightPixels, this.mDisplayMetrics.widthPixels) * 0.4676d);
        this.mHapticFeedbackUtil = new HapticFeedbackUtil(context, false);
        this.mContext.registerReceiver(this.mConfigurationReceiver, new IntentFilter("android.intent.action.CONFIGURATION_CHANGED"));
    }

    @Override // com.miui.server.input.knock.KnockGestureChecker
    public boolean continueCheck() {
        if ("change_brightness".equals(this.mFunction)) {
            return super.continueCheck();
        }
        return false;
    }

    @Override // com.miui.server.input.knock.KnockGestureChecker
    public void onTouchEvent(MotionEvent event) {
        float nowX = event.getX();
        float nowY = event.getY();
        switch (event.getAction()) {
            case 0:
                this.mDownX = nowX;
                this.mDownY = nowY;
                this.mHandler.removeCallbacks(this.mLongPressRunnable);
                this.mHandler.postDelayed(this.mLongPressRunnable, 500L);
                return;
            case 1:
                if (this.mNowBrightness != -1.0f) {
                    this.mDisplayManager.setBrightness(event.getDisplayId(), this.mNowBrightness);
                    this.mNowBrightness = -1.0f;
                }
                if (this.mCheckState == 3) {
                    ShortCutActionsUtils.ShortcutOneTrack.reportShortCutActionToOneTrack(this.mContext, "knock_long_press_horizontal_slid", this.mFunction);
                }
                this.mHandler.removeCallbacks(this.mLongPressRunnable);
                return;
            case 2:
                float diffDownX = Math.abs(nowX - this.mDownX);
                float diffDownY = Math.abs(nowY - this.mDownY);
                if ((diffDownX > 50.0f || diffDownY > 50.0f) && this.mHandler.hasCallbacks(this.mLongPressRunnable)) {
                    this.mHandler.removeCallbacks(this.mLongPressRunnable);
                    setCheckFail();
                }
                if (this.mCheckState == 2) {
                    if (diffDownY > 100.0f) {
                        Slog.i(TAG, "knock feature brightness check fail");
                        this.mCheckState = 4;
                        return;
                    } else if (diffDownX / diffDownY > 5.0f && diffDownX > 150.0f) {
                        Slog.i(TAG, "knock feature brightness check success");
                        this.mCheckState = 3;
                        try {
                            BrightnessInfo info = this.mContext.getDisplay().getBrightnessInfo();
                            if (info != null) {
                                this.mBrightnessMax = info.brightnessMaximum;
                                this.mBrightnessMin = info.brightnessMinimum;
                                this.mDownBrightness = info.brightness;
                            }
                            updateMinX(nowX, this.mDownBrightness);
                            return;
                        } catch (Exception e) {
                            e.printStackTrace();
                            return;
                        }
                    } else {
                        return;
                    }
                } else if (this.mCheckState == 3) {
                    setBrightness(event);
                    return;
                } else {
                    return;
                }
            default:
                return;
        }
    }

    public void setBrightness(MotionEvent event) {
        float nowX = event.getX();
        int slideValue = (int) ((((nowX - this.minX) * 1.0f) / this.mBrightnessViewLength) * BrightnessUtils.GAMMA_SPACE_MAX);
        if (slideValue < 0) {
            float f = this.mBrightnessMin;
            this.mNowBrightness = f;
            this.mDownBrightness = f;
            updateMinX(nowX, f);
        } else if (slideValue > BrightnessUtils.GAMMA_SPACE_MAX) {
            float f2 = this.mBrightnessMax;
            this.mNowBrightness = f2;
            this.mDownBrightness = f2;
            updateMinX(nowX, f2);
        } else {
            this.mNowBrightness = BrightnessUtils.convertGammaToLinearFloat(slideValue, this.mBrightnessMin, this.mBrightnessMax);
        }
        this.mDisplayManager.setTemporaryBrightness(event.getDisplayId(), this.mNowBrightness);
    }

    private void updateMinX(float x, float brightness) {
        float percent = (BrightnessUtils.convertLinearToGammaFloat(brightness, this.mBrightnessMin, this.mBrightnessMax) * 1.0f) / BrightnessUtils.GAMMA_SPACE_MAX;
        this.minX = x - (this.mBrightnessViewLength * percent);
    }

    /* loaded from: classes.dex */
    public static class BrightnessUtils {
        private static final float A;
        private static final float B;
        private static final float C;
        public static final int GAMMA_SPACE_MAX = Resources.getSystem().getInteger(17694941);
        public static final int GAMMA_SPACE_MIN = 0;
        private static final float R;
        private static final Resources resources;

        private BrightnessUtils() {
        }

        static {
            Resources resources2 = ActivityThread.currentApplication().getResources();
            resources = resources2;
            R = resources2.getFloat(285671454);
            A = resources2.getFloat(285671451);
            B = resources2.getFloat(285671452);
            C = resources2.getFloat(285671453);
        }

        public static final int convertGammaToLinear(int val, int min, int max) {
            float ret;
            float normalizedVal = MathUtils.norm((float) MiuiFreeformPinManagerService.EDGE_AREA, GAMMA_SPACE_MAX, val);
            float f = R;
            if (normalizedVal <= f) {
                ret = MathUtils.sq(normalizedVal / f);
            } else {
                float ret2 = C;
                ret = MathUtils.exp((normalizedVal - ret2) / A) + B;
            }
            return Math.round(MathUtils.lerp(min, max, ret / 12.0f));
        }

        public static final float convertGammaToLinearFloat(int val, float min, float max) {
            float ret;
            float normalizedVal = MathUtils.norm((float) MiuiFreeformPinManagerService.EDGE_AREA, GAMMA_SPACE_MAX, val);
            float f = R;
            if (normalizedVal <= f) {
                ret = MathUtils.sq(normalizedVal / f);
            } else {
                float ret2 = C;
                ret = MathUtils.exp((normalizedVal - ret2) / A) + B;
            }
            float normalizedRet = MathUtils.constrain(ret, (float) MiuiFreeformPinManagerService.EDGE_AREA, 12.0f);
            return MathUtils.lerp(min, max, normalizedRet / 12.0f);
        }

        public static final int convertLinearToGamma(int val, int min, int max) {
            return convertLinearToGammaFloat(val, min, max);
        }

        public static final int convertLinearToGammaFloat(float val, float min, float max) {
            float ret;
            float normalizedVal = MathUtils.norm(min, max, val) * 12.0f;
            if (normalizedVal <= 1.0f) {
                ret = MathUtils.sqrt(normalizedVal) * R;
            } else {
                float ret2 = A;
                ret = (ret2 * MathUtils.log(normalizedVal - B)) + C;
            }
            return Math.round(MathUtils.lerp(0, GAMMA_SPACE_MAX, ret));
        }
    }
}
