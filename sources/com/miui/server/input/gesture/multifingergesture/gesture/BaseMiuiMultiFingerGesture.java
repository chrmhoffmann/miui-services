package com.miui.server.input.gesture.multifingergesture.gesture;

import android.content.Context;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Slog;
import android.view.MotionEvent;
import com.miui.server.input.gesture.multifingergesture.MiuiMultiFingerGestureManager;
import com.miui.server.input.gesture.multifingergesture.MiuiMultiFingerGestureRect;
import com.miui.server.input.gesture.multifingergesture.MiuiMultiFingerGestureStatus;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import miui.util.FeatureParser;
/* loaded from: classes.dex */
public abstract class BaseMiuiMultiFingerGesture {
    private static final int DEFAULT_FINGER_NUM = 3;
    private static final int DEFAULT_FIRST_AND_LAST_FINGER_DOWN_TIME_DIFF = 280;
    private static final int DEFAULT_MAX_HEIGHT_DIFF = 200;
    private static final int FIXED_WIDTH = FeatureParser.getInteger("support_fix_width_device", 0);
    protected Context mContext;
    protected MiuiMultiFingerGestureRect mDefaultRage;
    private List<MiuiMultiFingerGestureRect> mDefaultRageList;
    protected String mFunction;
    protected Handler mHandler;
    protected final MiuiMultiFingerGestureManager mMiuiMultiFingerGestureManager;
    private MiuiMultiFingerGestureStatus mStatus;
    protected final String TAG = getClass().getSimpleName();
    protected float[] mInitX = new float[getFunctionNeedFingerNum()];
    protected float[] mInitY = new float[getFunctionNeedFingerNum()];

    public abstract String getGestureKey();

    public abstract void onTouchEvent(MotionEvent motionEvent);

    public BaseMiuiMultiFingerGesture(Context context, Handler handler, MiuiMultiFingerGestureManager manager) {
        this.mContext = context;
        this.mHandler = handler;
        this.mMiuiMultiFingerGestureManager = manager;
        initDefaultRage();
    }

    private void initDefaultRage() {
        MiuiMultiFingerGestureRect miuiMultiFingerGestureRect = new MiuiMultiFingerGestureRect();
        this.mDefaultRage = miuiMultiFingerGestureRect;
        this.mDefaultRageList = Collections.singletonList(miuiMultiFingerGestureRect);
        updateRage();
    }

    private void updateRage() {
        DisplayMetrics displayMetrics = this.mContext.getResources().getDisplayMetrics();
        int height = displayMetrics.heightPixels;
        int width = displayMetrics.widthPixels;
        MiuiMultiFingerGestureRect miuiMultiFingerGestureRect = this.mDefaultRage;
        int i = FIXED_WIDTH;
        miuiMultiFingerGestureRect.setWidth(i > 0 ? (int) (i * displayMetrics.density) : Math.min(width, height));
        this.mDefaultRage.setHeight((int) (displayMetrics.density * 200.0f));
    }

    protected boolean checkGestureIsOk(MotionEvent event) {
        if (event.getEventTime() - event.getDownTime() > getFirstAndLastFingerDownTimeDiff()) {
            return false;
        }
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE;
        float maxY = Float.MIN_VALUE;
        for (int i = 0; i < getFunctionNeedFingerNum(); i++) {
            float x = event.getX(i);
            float y = event.getY(i);
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }
        return checkIsInValidRange(minX, minY, maxX, maxY);
    }

    private boolean checkIsInValidRange(final float minX, final float minY, final float maxX, final float maxY) {
        List<MiuiMultiFingerGestureRect> validRange = getValidRange();
        if (validRange == null) {
            return false;
        }
        return validRange.stream().anyMatch(new Predicate() { // from class: com.miui.server.input.gesture.multifingergesture.gesture.BaseMiuiMultiFingerGesture$$ExternalSyntheticLambda0
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return BaseMiuiMultiFingerGesture.lambda$checkIsInValidRange$0(maxX, minX, maxY, minY, (MiuiMultiFingerGestureRect) obj);
            }
        });
    }

    public static /* synthetic */ boolean lambda$checkIsInValidRange$0(float maxX, float minX, float maxY, float minY, MiuiMultiFingerGestureRect rect) {
        return maxX - minX <= ((float) rect.getWidth()) && maxY - minY <= ((float) rect.getHeight());
    }

    protected List<MiuiMultiFingerGestureRect> getValidRange() {
        return this.mDefaultRageList;
    }

    public final MiuiMultiFingerGestureStatus getStatus() {
        return this.mStatus;
    }

    public final void changeStatus(MiuiMultiFingerGestureStatus newStatus) {
        this.mStatus = newStatus;
        if (newStatus == MiuiMultiFingerGestureStatus.FAIL) {
            onFail();
        } else if (newStatus == MiuiMultiFingerGestureStatus.SUCCESS) {
            this.mMiuiMultiFingerGestureManager.checkSuccess(this);
        }
    }

    public final String getGestureFunction() {
        return this.mFunction;
    }

    public final void setGestureFunction(String function) {
        this.mFunction = function;
    }

    public int getFunctionNeedFingerNum() {
        return 3;
    }

    private boolean hasEventFromShoulderKey(MotionEvent event) {
        for (int i = 0; i < getFunctionNeedFingerNum(); i++) {
            if (event.getAxisValue(32, i) >= 16.0f) {
                Slog.i(this.TAG, "Gesture " + getGestureKey() + " init fail, because pointer " + i + " from shoulder key.");
                return true;
            }
        }
        return false;
    }

    public void initGesture(MotionEvent event) {
        if (hasEventFromShoulderKey(event) || !checkGestureIsOk(event)) {
            changeStatus(MiuiMultiFingerGestureStatus.FAIL);
            return;
        }
        for (int i = 0; i < getFunctionNeedFingerNum(); i++) {
            this.mInitX[i] = event.getX(i);
            this.mInitY[i] = event.getY(i);
        }
        changeStatus(MiuiMultiFingerGestureStatus.DETECTING);
    }

    public final void checkSuccess() {
        changeStatus(MiuiMultiFingerGestureStatus.SUCCESS);
    }

    public final void checkFail() {
        changeStatus(MiuiMultiFingerGestureStatus.FAIL);
    }

    protected void onFail() {
    }

    protected int getFirstAndLastFingerDownTimeDiff() {
        return DEFAULT_FIRST_AND_LAST_FINGER_DOWN_TIME_DIFF;
    }

    public void onConfigChange() {
        updateRage();
    }

    public boolean preCondition() {
        return true;
    }
}
