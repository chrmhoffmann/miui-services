package com.miui.server.input.knock.checker;

import android.content.Context;
import android.util.Slog;
import android.view.MotionEvent;
import com.miui.server.input.knock.KnockGestureChecker;
import com.miui.server.input.knock.view.KnockGesturePathView;
import com.miui.server.input.util.ShortCutActionsUtils;
/* loaded from: classes.dex */
public class KnockGestureV extends KnockGestureChecker {
    private static final String TAG = "KnockGestureV";

    public KnockGestureV(Context context) {
        super(context);
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
        if (event.getAction() == 1) {
            if (isVGesture()) {
                setCheckSuccess();
                this.mKnockPathListener.hideView();
                ShortCutActionsUtils.getInstance(this.mContext).triggerFunction(this.mFunction, "knock_gesture_v", null, true);
                return;
            }
            setCheckFail();
        }
    }

    public boolean isVGesture() {
        double vLeft;
        double atanDegLeft;
        float downY;
        int bottomIndex;
        int i;
        KnockGesturePathView.KnockPointerState pointerState = this.mKnockPathListener.getPointerPathData();
        if (pointerState != null && pointerState.mTraceCount > 10) {
            int N = pointerState.mTraceCount;
            float downX = pointerState.mTraceX[0].floatValue();
            float downY2 = pointerState.mTraceY[0].floatValue();
            float upX = pointerState.mTraceX[N - 1].floatValue();
            float upY = pointerState.mTraceY[N - 1].floatValue();
            float bottomX = 0.0f;
            float bottomY = 0.0f;
            int bottomIndex2 = 0;
            for (int i2 = 0; i2 < N; i2++) {
                float y = pointerState.mTraceY[i2].floatValue();
                if (y > bottomY) {
                    float bottomX2 = pointerState.mTraceX[i2].floatValue();
                    bottomY = y;
                    bottomX = bottomX2;
                    bottomIndex2 = i2;
                }
            }
            if (bottomY == downY2 || bottomY == upY || bottomX <= downX || bottomX >= upX || downX >= upX || bottomY - downY2 < 350.0f) {
                return false;
            }
            if (bottomY - upY < 350.0f) {
                return false;
            }
            double vLeft2 = (bottomX - downX) / (bottomY - downY2);
            double atanLeft = Math.atan(vLeft2);
            double atanDegLeft2 = Math.toDegrees(atanLeft);
            double vRight = (upX - bottomX) / (bottomY - upY);
            double atanRight = Math.atan(vRight);
            int N2 = N;
            double atanDegRight = Math.toDegrees(atanRight);
            Slog.i(TAG, "角度 left:" + atanDegLeft2 + " right:" + atanDegRight);
            int i3 = 0;
            while (true) {
                int N3 = N2;
                if (i3 < N3) {
                    if (i3 < bottomIndex2) {
                        N2 = N3;
                        i = i3;
                        atanDegLeft = atanDegLeft2;
                        float f = downY2;
                        vLeft = vLeft2;
                        downY = downY2;
                        bottomIndex = bottomIndex2;
                        if (pointToLine(downX, f, pointerState.mTraceX[bottomIndex2].floatValue(), pointerState.mTraceY[bottomIndex2].floatValue(), pointerState.mTraceX[i3].floatValue(), pointerState.mTraceY[i3].floatValue()) > 200.0d) {
                            return false;
                        }
                    } else {
                        N2 = N3;
                        i = i3;
                        atanDegLeft = atanDegLeft2;
                        vLeft = vLeft2;
                        downY = downY2;
                        bottomIndex = bottomIndex2;
                        if (i > bottomIndex && pointToLine(upX, upY, pointerState.mTraceX[bottomIndex].floatValue(), pointerState.mTraceY[bottomIndex].floatValue(), pointerState.mTraceX[i].floatValue(), pointerState.mTraceY[i].floatValue()) > 200.0d) {
                            return false;
                        }
                    }
                    i3 = i + 1;
                    bottomIndex2 = bottomIndex;
                    downY2 = downY;
                    atanDegLeft2 = atanDegLeft;
                    vLeft2 = vLeft;
                } else if (Math.abs(atanDegLeft2 - atanDegRight) <= 30.0d && Math.abs(downX - upX) > 200.0f) {
                    return true;
                } else {
                    return false;
                }
            }
        }
        return false;
    }

    private double pointToLine(float x1, float y1, float x2, float y2, float x0, float y0) {
        double a = lineSpace(x1, y1, x2, y2);
        double b = lineSpace(x1, y1, x0, y0);
        double c = lineSpace(x2, y2, x0, y0);
        if (c <= 1.0E-6d || b <= 1.0E-6d) {
            return 0.0d;
        }
        if (a <= 1.0E-6d) {
            return b;
        }
        if (c * c >= (a * a) + (b * b)) {
            return b;
        }
        if (b * b >= (a * a) + (c * c)) {
            return c;
        }
        double p = ((a + b) + c) / 2.0d;
        double s = Math.sqrt((p - a) * p * (p - b) * (p - c));
        double space = (2.0d * s) / a;
        return space;
    }

    private double lineSpace(float x1, float y1, float x2, float y2) {
        return Math.sqrt(((x1 - x2) * (x1 - x2)) + ((y1 - y2) * (y1 - y2)));
    }
}
