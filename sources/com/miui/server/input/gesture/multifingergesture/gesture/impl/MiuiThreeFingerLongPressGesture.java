package com.miui.server.input.gesture.multifingergesture.gesture.impl;

import android.content.Context;
import android.os.Handler;
import android.view.MotionEvent;
import com.android.server.wm.MiuiFreeformPinManagerService;
import com.miui.server.input.gesture.multifingergesture.MiuiMultiFingerGestureManager;
import com.miui.server.input.gesture.multifingergesture.gesture.BaseMiuiMultiFingerGesture;
import miui.os.Build;
/* loaded from: classes.dex */
public class MiuiThreeFingerLongPressGesture extends BaseMiuiMultiFingerGesture {
    private static final int THREE_LONG_PRESS_TIME_OUT = 600;
    private float mMoveThreshold;
    private final Runnable mThreeLongPressRunnable = new Runnable() { // from class: com.miui.server.input.gesture.multifingergesture.gesture.impl.MiuiThreeFingerLongPressGesture$$ExternalSyntheticLambda0
        @Override // java.lang.Runnable
        public final void run() {
            MiuiThreeFingerLongPressGesture.this.checkSuccess();
        }
    };

    public MiuiThreeFingerLongPressGesture(Context context, Handler handler, MiuiMultiFingerGestureManager manager) {
        super(context, handler, manager);
        updateConfig();
    }

    @Override // com.miui.server.input.gesture.multifingergesture.gesture.BaseMiuiMultiFingerGesture
    public void onTouchEvent(MotionEvent event) {
        handleEvent(event);
    }

    @Override // com.miui.server.input.gesture.multifingergesture.gesture.BaseMiuiMultiFingerGesture
    public String getGestureKey() {
        return "three_gesture_long_press";
    }

    private void handleEvent(MotionEvent event) {
        if (event.getAction() == 2) {
            float distanceX = MiuiFreeformPinManagerService.EDGE_AREA;
            float distanceY = MiuiFreeformPinManagerService.EDGE_AREA;
            for (int i = 0; i < getFunctionNeedFingerNum(); i++) {
                distanceY += Math.abs(event.getY(i) - this.mInitY[i]);
                distanceX += Math.abs(event.getX(i) - this.mInitX[i]);
            }
            float f = this.mMoveThreshold;
            if (distanceX >= f || distanceY >= f) {
                checkFail();
            }
        } else if (event.getActionMasked() == 5) {
            this.mHandler.postDelayed(this.mThreeLongPressRunnable, 600L);
        }
    }

    @Override // com.miui.server.input.gesture.multifingergesture.gesture.BaseMiuiMultiFingerGesture
    protected void onFail() {
        this.mHandler.removeCallbacks(this.mThreeLongPressRunnable);
    }

    private void updateConfig() {
        this.mMoveThreshold = this.mContext.getResources().getDisplayMetrics().density * 50.0f * (Build.IS_TABLET ? 2 : 1) * 0.6f;
    }

    @Override // com.miui.server.input.gesture.multifingergesture.gesture.BaseMiuiMultiFingerGesture
    public void onConfigChange() {
        super.onConfigChange();
        updateConfig();
    }

    @Override // com.miui.server.input.gesture.multifingergesture.gesture.BaseMiuiMultiFingerGesture
    public boolean preCondition() {
        return !this.mMiuiMultiFingerGestureManager.isKeyguardActive();
    }
}
