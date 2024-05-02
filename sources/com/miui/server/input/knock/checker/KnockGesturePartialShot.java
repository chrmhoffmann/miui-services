package com.miui.server.input.knock.checker;

import android.content.Context;
import android.os.Bundle;
import android.os.SystemProperties;
import android.util.Slog;
import android.view.MotionEvent;
import com.android.server.wm.MiuiFreeformPinManagerService;
import com.miui.server.input.knock.KnockGestureChecker;
import com.miui.server.input.knock.view.KnockGesturePathView;
import com.miui.server.input.util.ShortCutActionsUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
/* loaded from: classes.dex */
public class KnockGesturePartialShot extends KnockGestureChecker {
    private static final String TAG = "KnockGesturePartialShot";
    private float mBehindNextX;
    private float mBehindNextY;
    private float mBehindX;
    private float mBehindY;
    private float mCrossoverBehindX;
    private float mCrossoverBehindY;
    private float mCrossoverPreX;
    private float mCrossoverPreY;
    private List<Float> mPathX;
    private List<Float> mPathY;
    private float mPreNextX;
    private float mPreNextY;
    private float mPx;
    private float mPy;
    private int mCrossoverPointCount = 0;
    private final float EPS = 1.0E-6f;
    private boolean mIsBegin = false;

    public KnockGesturePartialShot(Context context) {
        super(context);
    }

    @Override // com.miui.server.input.knock.KnockGestureChecker
    public boolean continueCheck() {
        if ("partial_screen_shot".equals(this.mFunction)) {
            return super.continueCheck();
        }
        return false;
    }

    @Override // com.miui.server.input.knock.KnockGestureChecker
    public void onTouchEvent(MotionEvent event) {
        if (event.getAction() == 1) {
            if (checkIsValid()) {
                setCheckSuccess();
                Slog.i(TAG, "check success path size is :  " + size());
                Bundle bundle = new Bundle();
                float[] motionList = new float[size() * 2];
                int j = 0;
                for (int i = 0; i < size(); i++) {
                    float x = this.mPathX.get(i).floatValue();
                    float y = this.mPathY.get(i).floatValue();
                    if (!Float.isNaN(x)) {
                        int j2 = j + 1;
                        motionList[j] = x;
                        j = j2 + 1;
                        motionList[j2] = y;
                    }
                }
                bundle.putFloatArray(ShortCutActionsUtils.PARTIAL_SCREENSHOT_POINTS, motionList);
                this.mKnockPathListener.hideView();
                ShortCutActionsUtils.getInstance(this.mContext).triggerFunction(this.mFunction, "knock_gesture_partial", bundle, false);
                return;
            }
            Slog.i(TAG, "check fail");
            setCheckFail();
        }
    }

    private boolean checkIsValid() {
        if (SystemProperties.getBoolean("sys.miui.screenshot.partial", false)) {
            Slog.d(TAG, "cloud not start partial screenshot because of partial screenshot is running");
            return false;
        }
        KnockGesturePathView.KnockPointerState pointerState = this.mKnockPathListener.getPointerPathData();
        if (pointerState == null || pointerState.mTraceCount < 10 || pointerState.mTraceCount > 1000) {
            return false;
        }
        this.mPathX = new ArrayList(Arrays.asList(pointerState.mTraceX).subList(0, pointerState.mTraceCount));
        this.mPathY = new ArrayList(Arrays.asList(pointerState.mTraceY).subList(0, pointerState.mTraceCount));
        if (getPathRight() - getPathLeft() < 200.0f || getPathBottom() - getPathTop() < 200.0f) {
            return false;
        }
        judgePath();
        if (this.mCrossoverPointCount != 1 && Math.pow(this.mPathX.get(size() - 1).floatValue() - this.mPathX.get(0).floatValue(), 2.0d) + Math.pow(this.mPathY.get(size() - 1).floatValue() - this.mPathY.get(0).floatValue(), 2.0d) > 90000.0d) {
            return false;
        }
        if (this.mCrossoverPointCount >= 2) {
            return true;
        }
        cutPath();
        return getPathRight() - getPathLeft() >= 200.0f && getPathBottom() - getPathTop() >= 200.0f;
    }

    private void judgePath() {
        this.mCrossoverPointCount = 0;
        for (int i = 0; i < size() - 11; i++) {
            this.mPx = this.mPathX.get(i).floatValue();
            this.mPy = this.mPathY.get(i).floatValue();
            this.mPreNextX = this.mPathX.get(i + 1).floatValue();
            this.mPreNextY = this.mPathY.get(i + 1).floatValue();
            for (int j = i + 10; j < size() - 1; j++) {
                this.mBehindX = this.mPathX.get(j).floatValue();
                this.mBehindY = this.mPathY.get(j).floatValue();
                this.mBehindNextX = this.mPathX.get(j + 1).floatValue();
                this.mBehindNextY = this.mPathY.get(j + 1).floatValue();
                if (Math.min(this.mPx, this.mPreNextX) < Math.max(this.mBehindNextX, this.mBehindX) && Math.min(this.mPy, this.mPreNextY) < Math.max(this.mBehindNextY, this.mBehindY) && Math.min(this.mBehindNextX, this.mBehindX) < Math.max(this.mPx, this.mPreNextX) && Math.min(this.mBehindNextY, this.mBehindY) < Math.max(this.mPy, this.mPreNextY)) {
                    float f = this.mPreNextX;
                    float f2 = this.mPx;
                    float f3 = this.mBehindY;
                    float f4 = this.mPy;
                    float f5 = this.mPreNextY;
                    float f6 = this.mBehindX;
                    float h = ((f - f2) * (f3 - f4)) - ((f5 - f4) * (f6 - f2));
                    float f7 = this.mBehindNextY;
                    float f8 = this.mBehindNextX;
                    float m = ((f - f2) * (f7 - f4)) - ((f5 - f4) * (f8 - f2));
                    float n = ((f8 - f6) * (f4 - f3)) - ((f7 - f3) * (f2 - f6));
                    float k = ((f8 - f6) * (f5 - f3)) - ((f7 - f3) * (f - f6));
                    if (h * m <= 1.0E-6f && n * k <= 1.0E-6f) {
                        int i2 = this.mCrossoverPointCount + 1;
                        this.mCrossoverPointCount = i2;
                        if (i2 == 1) {
                            this.mCrossoverPreX = f;
                            this.mCrossoverPreY = f5;
                            this.mCrossoverBehindX = f6;
                            this.mCrossoverBehindY = f3;
                        }
                    }
                }
            }
        }
    }

    private void cutPath() {
        if (this.mCrossoverPointCount == 1 && (distance(this.mPathX.get(size() - 1).floatValue(), this.mPathX.get(0).floatValue(), this.mPathY.get(size() - 1).floatValue(), this.mPathY.get(0).floatValue()) > 200.0d || (distance(this.mPathX.get(size() - 1).floatValue(), this.mCrossoverBehindX, this.mPathY.get(size() - 1).floatValue(), this.mCrossoverBehindY) < 200.0d && distance(this.mPathX.get(0).floatValue(), this.mCrossoverPreX, this.mPathY.get(0).floatValue(), this.mCrossoverPreY) < 200.0d))) {
            int from = 0;
            int to = 0;
            for (int i = 0; i < size(); i++) {
                if (this.mPathX.get(i).floatValue() == this.mCrossoverPreX && this.mPathY.get(i).floatValue() == this.mCrossoverPreY) {
                    from = i;
                }
                if (this.mPathX.get(i).floatValue() == this.mCrossoverBehindX && this.mPathY.get(i).floatValue() == this.mCrossoverBehindY) {
                    to = i;
                }
            }
            if (from > 0 && to > 0 && to - from > 5) {
                this.mPathY = this.mPathY.subList(from, to);
                List<Float> subList = this.mPathX.subList(from, to);
                this.mPathX = subList;
                subList.add(Float.valueOf(this.mCrossoverBehindX));
                this.mPathY.add(Float.valueOf(this.mCrossoverBehindY));
                this.mPathX.add(Float.valueOf(this.mCrossoverPreX));
                this.mPathY.add(Float.valueOf(this.mCrossoverPreY));
            }
        }
        int from2 = this.mCrossoverPointCount;
        if (from2 == 0) {
            float beginX = this.mPathX.get(0).floatValue();
            float beginY = this.mPathY.get(0).floatValue();
            float lastX = this.mPathX.get(size() - 1).floatValue();
            float lastY = this.mPathY.get(size() - 1).floatValue();
            float minDistance = Float.MAX_VALUE;
            int pointMinCutIndex = -1;
            for (int i2 = 1; i2 < size() / 2; i2++) {
                if (Math.pow(lastX - this.mPathX.get(i2).floatValue(), 2.0d) + Math.pow(lastY - this.mPathY.get(i2).floatValue(), 2.0d) < minDistance) {
                    float minDistance2 = (float) (Math.pow(lastX - this.mPathX.get(i2).floatValue(), 2.0d) + Math.pow(lastY - this.mPathY.get(i2).floatValue(), 2.0d));
                    this.mIsBegin = true;
                    minDistance = minDistance2;
                    pointMinCutIndex = i2;
                }
            }
            for (int i3 = size() / 2; i3 < size() - 1; i3++) {
                if (Math.pow(beginX - this.mPathX.get(i3).floatValue(), 2.0d) + Math.pow(beginY - this.mPathY.get(i3).floatValue(), 2.0d) < minDistance) {
                    float minDistance3 = (float) (Math.pow(beginX - this.mPathX.get(i3).floatValue(), 2.0d) + Math.pow(beginY - this.mPathY.get(i3).floatValue(), 2.0d));
                    this.mIsBegin = false;
                    pointMinCutIndex = i3;
                    minDistance = minDistance3;
                }
            }
            if (pointMinCutIndex > 0) {
                if (this.mIsBegin && pointMinCutIndex + 7 < size()) {
                    this.mPathX = this.mPathX.subList(pointMinCutIndex + 2, size());
                    this.mPathY = this.mPathY.subList(pointMinCutIndex + 2, size());
                } else if (!this.mIsBegin && pointMinCutIndex > 8) {
                    this.mPathX = this.mPathX.subList(0, pointMinCutIndex - 3);
                    this.mPathY = this.mPathY.subList(0, pointMinCutIndex - 3);
                }
            }
        }
    }

    private double distance(float x1, float x2, float y1, float y2) {
        return Math.sqrt(Math.pow(x1 - x2, 2.0d) + Math.pow(y1 - y2, 2.0d));
    }

    public int size() {
        return this.mPathY.size();
    }

    private float getPathTop() {
        float min = this.mPathY.size() > 0 ? this.mPathY.get(0).floatValue() : 0.0f;
        for (Float f : this.mPathY) {
            float y = f.floatValue();
            if (y < min) {
                min = y;
            }
        }
        return min < MiuiFreeformPinManagerService.EDGE_AREA ? MiuiFreeformPinManagerService.EDGE_AREA : min;
    }

    private float getPathLeft() {
        float min = this.mPathX.size() > 0 ? this.mPathX.get(0).floatValue() : 0.0f;
        for (Float f : this.mPathX) {
            float x = f.floatValue();
            if (x < min) {
                min = x;
            }
        }
        return min < MiuiFreeformPinManagerService.EDGE_AREA ? MiuiFreeformPinManagerService.EDGE_AREA : min;
    }

    private float getPathBottom() {
        float max = this.mPathY.size() > 0 ? this.mPathY.get(0).floatValue() : 0.0f;
        for (Float f : this.mPathY) {
            float y = f.floatValue();
            if (y > max) {
                max = y;
            }
        }
        return max < MiuiFreeformPinManagerService.EDGE_AREA ? MiuiFreeformPinManagerService.EDGE_AREA : max;
    }

    private float getPathRight() {
        float max = this.mPathX.size() > 0 ? this.mPathX.get(0).floatValue() : 0.0f;
        for (Float x : this.mPathX) {
            if (x.floatValue() > max) {
                max = x.floatValue();
            }
        }
        return max < MiuiFreeformPinManagerService.EDGE_AREA ? MiuiFreeformPinManagerService.EDGE_AREA : max;
    }
}
