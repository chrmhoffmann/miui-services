package com.miui.server.input.stylus.checker;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Slog;
import android.view.Display;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import com.miui.server.input.stylus.BaseStylusGestureChecker;
import com.miui.server.input.util.ShortCutActionsUtils;
/* loaded from: classes.dex */
public class StylusGestureLowerLeft extends BaseStylusGestureChecker {
    private static final String TAG = "StylusGestureLowerLeft";
    private final int AREA_X;
    private final int AREA_Y;
    private float mInitMotionX;
    private float mInitMotionY;
    private int mTableGestureState;
    private double mTableGestureThreshold_X;
    private double mTableGestureThreshold_Y;
    private final float mTouchSlop;

    public StylusGestureLowerLeft(Context context) {
        super(context);
        int i = (int) (context.getResources().getDisplayMetrics().density * 10.0f);
        this.AREA_Y = i;
        this.AREA_X = i;
        this.mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    @Override // com.miui.server.input.stylus.BaseStylusGestureChecker
    public void onPointerEvent(MotionEvent event) {
        checkLowerLeftGesture(event);
    }

    private void checkLowerLeftGesture(MotionEvent motionEvent) {
        if (motionEvent.getAction() == 0) {
            changeTableGestureState(0);
        } else if (this.mTableGestureState == 0) {
            if (checkIsStartPosition(motionEvent)) {
                changeTableGestureState(1);
            } else {
                changeTableGestureState(2);
            }
        }
        checkIsStylusGesture(motionEvent);
    }

    private boolean checkIsStartPosition(MotionEvent motionEvent) {
        Display display = this.mWindowManager.getDefaultDisplay();
        DisplayMetrics dm = this.mContext.getResources().getDisplayMetrics();
        display.getRealMetrics(dm);
        int height = dm.heightPixels;
        double d = dm.density * 30.0f;
        this.mTableGestureThreshold_X = d;
        this.mTableGestureThreshold_Y = height - d;
        this.mInitMotionX = motionEvent.getX();
        float y = motionEvent.getY();
        this.mInitMotionY = y;
        if (this.mInitMotionX <= this.AREA_X && y >= height - this.AREA_Y) {
            Slog.w(TAG, " InitMotionX : " + this.mInitMotionX + " , InitMotionY : " + this.mInitMotionY + " successful ");
            return true;
        }
        return false;
    }

    private void checkIsStylusGesture(MotionEvent motionEvent) {
        if (this.mTableGestureState == 1 && motionEvent.getAction() == 2) {
            float x = motionEvent.getX();
            float y = motionEvent.getY();
            if (dist(x, y, this.mInitMotionX, this.mInitMotionY) >= this.mTouchSlop * 2.0f) {
                Slog.w(TAG, "PilferPointers cancel event because of three stylus gesture detecting");
                this.mMiuiGestureMonitor.pilferPointers();
            }
            if (x >= this.mTableGestureThreshold_X && y <= this.mTableGestureThreshold_Y) {
                changeTableGestureState(3);
                takeScreenshot();
            }
        }
    }

    private void changeTableGestureState(int newState) {
        this.mTableGestureState = newState;
    }

    private void takeScreenshot() {
        ShortCutActionsUtils.getInstance(this.mContext).triggerFunction("screenshot_without_anim", "stylus_screen_lower_left", null, false);
    }

    private float dist(float x1, float y1, float x2, float y2) {
        float x = x2 - x1;
        float y = y2 - y1;
        return (float) Math.hypot(x, y);
    }
}
