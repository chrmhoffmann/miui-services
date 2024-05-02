package com.miui.server.input.gesture.multifingergesture.gesture.impl;

import android.content.Context;
import android.os.Handler;
import android.view.MotionEvent;
import com.android.server.wm.MiuiFreeformPinManagerService;
import com.miui.server.input.gesture.multifingergesture.MiuiMultiFingerGestureManager;
import com.miui.server.input.gesture.multifingergesture.MiuiMultiFingerGestureRect;
import com.miui.server.input.gesture.multifingergesture.gesture.BaseMiuiMultiFingerGesture;
import java.util.Arrays;
import java.util.List;
import miui.os.Build;
/* loaded from: classes.dex */
public class MiuiThreeFingerHorizontalGesture extends BaseMiuiMultiFingerGesture {
    protected float mThreshold;
    private final List<MiuiMultiFingerGestureRect> mValidRangeList;

    public MiuiThreeFingerHorizontalGesture(Context context, Handler handler, MiuiMultiFingerGestureManager manager) {
        super(context, handler, manager);
        MiuiMultiFingerGestureRect rect = new MiuiMultiFingerGestureRect();
        this.mValidRangeList = Arrays.asList(this.mDefaultRage, rect);
        updateConfig();
    }

    @Override // com.miui.server.input.gesture.multifingergesture.gesture.BaseMiuiMultiFingerGesture
    public void onTouchEvent(MotionEvent event) {
        handleEvent(event);
    }

    @Override // com.miui.server.input.gesture.multifingergesture.gesture.BaseMiuiMultiFingerGesture
    public String getGestureKey() {
        return "three_gesture_horizontal";
    }

    protected void handleEvent(MotionEvent event) {
        if (event.getAction() != 2) {
            return;
        }
        float distanceX = MiuiFreeformPinManagerService.EDGE_AREA;
        float distanceY = MiuiFreeformPinManagerService.EDGE_AREA;
        for (int i = 0; i < getFunctionNeedFingerNum(); i++) {
            distanceX += Math.abs(event.getX(i) - this.mInitX[i]);
            distanceY += Math.abs(event.getY(i) - this.mInitY[i]);
        }
        float f = this.mThreshold;
        if (distanceX >= f && distanceY <= f) {
            checkSuccess();
        }
    }

    @Override // com.miui.server.input.gesture.multifingergesture.gesture.BaseMiuiMultiFingerGesture
    protected List<MiuiMultiFingerGestureRect> getValidRange() {
        return this.mValidRangeList;
    }

    @Override // com.miui.server.input.gesture.multifingergesture.gesture.BaseMiuiMultiFingerGesture
    public void onConfigChange() {
        super.onConfigChange();
        updateConfig();
    }

    private void updateConfig() {
        this.mThreshold = getFunctionNeedFingerNum() * this.mContext.getResources().getDisplayMetrics().density * 50.0f * (Build.IS_TABLET ? 2 : 1);
        MiuiMultiFingerGestureRect rect = this.mValidRangeList.get(1);
        rect.setHeight(this.mDefaultRage.getWidth());
        rect.setWidth(this.mDefaultRage.getHeight());
    }

    /* loaded from: classes.dex */
    public static class MiuiThreeFingerHorizontalLTRGesture extends MiuiThreeFingerHorizontalGesture {
        public MiuiThreeFingerHorizontalLTRGesture(Context context, Handler handler, MiuiMultiFingerGestureManager manager) {
            super(context, handler, manager);
        }

        @Override // com.miui.server.input.gesture.multifingergesture.gesture.impl.MiuiThreeFingerHorizontalGesture, com.miui.server.input.gesture.multifingergesture.gesture.BaseMiuiMultiFingerGesture
        public String getGestureKey() {
            return "three_gesture_horizontal_ltr";
        }

        @Override // com.miui.server.input.gesture.multifingergesture.gesture.impl.MiuiThreeFingerHorizontalGesture
        protected void handleEvent(MotionEvent event) {
            if (event.getAction() != 2) {
                return;
            }
            float distanceX = MiuiFreeformPinManagerService.EDGE_AREA;
            float distanceY = MiuiFreeformPinManagerService.EDGE_AREA;
            float direction = MiuiFreeformPinManagerService.EDGE_AREA;
            for (int i = 0; i < getFunctionNeedFingerNum(); i++) {
                distanceX += Math.abs(event.getX(i) - this.mInitX[i]);
                distanceY += Math.abs(event.getY(i) - this.mInitY[i]);
                direction += (event.getX(i) - this.mInitX[i]) + (event.getY(i) - this.mInitY[i]);
            }
            if (distanceX >= this.mThreshold && distanceY <= this.mThreshold && direction > MiuiFreeformPinManagerService.EDGE_AREA) {
                checkSuccess();
            }
        }
    }

    /* loaded from: classes.dex */
    public static class MiuiThreeFingerHorizontalRTLGesture extends MiuiThreeFingerHorizontalGesture {
        public MiuiThreeFingerHorizontalRTLGesture(Context context, Handler handler, MiuiMultiFingerGestureManager manager) {
            super(context, handler, manager);
        }

        @Override // com.miui.server.input.gesture.multifingergesture.gesture.impl.MiuiThreeFingerHorizontalGesture, com.miui.server.input.gesture.multifingergesture.gesture.BaseMiuiMultiFingerGesture
        public String getGestureKey() {
            return "three_gesture_horizontal_rtl";
        }

        @Override // com.miui.server.input.gesture.multifingergesture.gesture.impl.MiuiThreeFingerHorizontalGesture
        protected void handleEvent(MotionEvent event) {
            if (event.getAction() != 2) {
                return;
            }
            float distanceX = MiuiFreeformPinManagerService.EDGE_AREA;
            float distanceY = MiuiFreeformPinManagerService.EDGE_AREA;
            float direction = MiuiFreeformPinManagerService.EDGE_AREA;
            for (int i = 0; i < getFunctionNeedFingerNum(); i++) {
                distanceX += Math.abs(event.getX(i) - this.mInitX[i]);
                distanceY += Math.abs(event.getY(i) - this.mInitY[i]);
                direction += (event.getX(i) - this.mInitX[i]) + (event.getY(i) - this.mInitY[i]);
            }
            if (distanceX >= this.mThreshold && distanceY <= this.mThreshold && direction < MiuiFreeformPinManagerService.EDGE_AREA) {
                checkSuccess();
            }
        }
    }
}
