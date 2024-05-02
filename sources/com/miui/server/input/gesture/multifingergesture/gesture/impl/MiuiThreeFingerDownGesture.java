package com.miui.server.input.gesture.multifingergesture.gesture.impl;

import android.content.Context;
import android.os.Handler;
import android.view.MotionEvent;
import com.android.server.wm.MiuiFreeformPinManagerService;
import com.miui.server.input.gesture.multifingergesture.MiuiMultiFingerGestureManager;
import com.miui.server.input.gesture.multifingergesture.gesture.BaseMiuiMultiFingerGesture;
import miui.os.Build;
/* loaded from: classes.dex */
public class MiuiThreeFingerDownGesture extends BaseMiuiMultiFingerGesture {
    private float mThreshold;

    public MiuiThreeFingerDownGesture(Context context, Handler handler, MiuiMultiFingerGestureManager manager) {
        super(context, handler, manager);
        updateConfig();
    }

    @Override // com.miui.server.input.gesture.multifingergesture.gesture.BaseMiuiMultiFingerGesture
    public void onTouchEvent(MotionEvent event) {
        handleEvent(event);
    }

    @Override // com.miui.server.input.gesture.multifingergesture.gesture.BaseMiuiMultiFingerGesture
    public String getGestureKey() {
        return "three_gesture_down";
    }

    private void handleEvent(MotionEvent event) {
        if (event.getAction() != 2) {
            return;
        }
        float distanceX = MiuiFreeformPinManagerService.EDGE_AREA;
        float distanceY = MiuiFreeformPinManagerService.EDGE_AREA;
        for (int i = 0; i < getFunctionNeedFingerNum(); i++) {
            distanceY += event.getY(i) - this.mInitY[i];
            distanceX += Math.abs(event.getX(i) - this.mInitX[i]);
        }
        if (distanceY < (-this.mThreshold) || (Build.IS_TABLET && distanceX > distanceY)) {
            checkFail();
        } else if (distanceY >= this.mThreshold) {
            checkSuccess();
        }
    }

    @Override // com.miui.server.input.gesture.multifingergesture.gesture.BaseMiuiMultiFingerGesture
    public void onConfigChange() {
        super.onConfigChange();
        updateConfig();
    }

    private void updateConfig() {
        this.mThreshold = getFunctionNeedFingerNum() * this.mContext.getResources().getDisplayMetrics().density * 50.0f * (Build.IS_TABLET ? 2 : 1);
    }
}
