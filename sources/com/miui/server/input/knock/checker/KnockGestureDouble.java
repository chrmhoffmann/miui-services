package com.miui.server.input.knock.checker;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Slog;
import android.view.Display;
import android.view.MotionEvent;
import android.view.WindowManager;
import com.android.server.wm.MiuiFreeformPinManagerService;
import com.miui.server.input.knock.KnockGestureChecker;
import com.miui.server.input.util.ShortCutActionsUtils;
import miui.android.animation.internal.AnimTask;
/* loaded from: classes.dex */
public class KnockGestureDouble extends KnockGestureChecker {
    private static final int DOUBLE_KNOCK_MAX_MOVE_DISTANCE = 1;
    private static final int DOUBLE_KNOCK_TIME = 500;
    private static final int DOUBLE_KNOCK_VALID_DISTANCE = 2;
    public static final String TAG = "KnockGestureDouble";
    private int mDoubleKnockValidDistance;
    private float mLastKnockDownX;
    private float mLastKnockDownY;
    private long mLastKnockTime;
    private long mLastTriggerTime;
    private int mMaxMoveDistance;

    public KnockGestureDouble(Context context) {
        super(context);
        initScreenSize();
    }

    @Override // com.miui.server.input.knock.KnockGestureChecker
    public boolean continueCheck() {
        if (!checkEmpty(this.mFunction)) {
            return super.continueCheck();
        }
        return false;
    }

    @Override // com.miui.server.input.knock.KnockGestureChecker
    public void onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        float nowX = event.getX();
        float nowY = event.getY();
        long nowTime = System.currentTimeMillis();
        if (action == 0) {
            Slog.i(TAG, "knock down, time:" + nowTime + " nowX:" + nowX + " nowY:" + nowY + " mDoubleKnockValidDistance:" + this.mDoubleKnockValidDistance);
            if (nowTime - this.mLastKnockTime < 500 && Math.abs(nowX - this.mLastKnockDownX) < this.mDoubleKnockValidDistance && Math.abs(nowY - this.mLastKnockDownY) < this.mDoubleKnockValidDistance) {
                this.mLastKnockTime = 0L;
                this.mLastKnockDownX = MiuiFreeformPinManagerService.EDGE_AREA;
                this.mLastKnockDownY = MiuiFreeformPinManagerService.EDGE_AREA;
                triggerDoubleKnockFeature();
                return;
            }
            this.mLastKnockDownX = nowX;
            this.mLastKnockDownY = nowY;
            this.mLastKnockTime = nowTime;
        } else if (action == 2) {
            if (Math.abs(nowX - this.mLastKnockDownX) > this.mMaxMoveDistance || Math.abs(nowY - this.mLastKnockDownY) > this.mMaxMoveDistance) {
                this.mLastKnockTime = 0L;
                this.mLastKnockDownX = MiuiFreeformPinManagerService.EDGE_AREA;
                this.mLastKnockDownY = MiuiFreeformPinManagerService.EDGE_AREA;
                Slog.i(TAG, "knock move more than mMaxMoveDistance:" + this.mMaxMoveDistance);
            }
        }
    }

    private void triggerDoubleKnockFeature() {
        this.mKnockPathListener.hideView();
        setCheckSuccess();
        long nowTime = System.currentTimeMillis();
        if (nowTime - this.mLastTriggerTime < 500) {
            return;
        }
        this.mLastTriggerTime = nowTime;
        ShortCutActionsUtils.getInstance(this.mContext).triggerFunction(this.mFunction, "double_knock", null, true);
    }

    @Override // com.miui.server.input.knock.KnockGestureChecker
    public void onScreenSizeChanged() {
        initScreenSize();
    }

    private void initScreenSize() {
        WindowManager mWindowManager = (WindowManager) this.mContext.getSystemService("window");
        Display defaultDisplay = mWindowManager.getDefaultDisplay();
        DisplayMetrics dm = new DisplayMetrics();
        defaultDisplay.getRealMetrics(dm);
        float oneCentimeterPix = dm.densityDpi / 2.54f;
        this.mDoubleKnockValidDistance = Math.max((int) (2.0f * oneCentimeterPix), (int) AnimTask.MAX_PAGE_SIZE);
        this.mMaxMoveDistance = Math.max((int) (1.0f * oneCentimeterPix), 100);
    }
}
