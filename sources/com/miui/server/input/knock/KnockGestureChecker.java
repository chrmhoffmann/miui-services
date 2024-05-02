package com.miui.server.input.knock;

import android.content.Context;
import android.text.TextUtils;
import android.view.MotionEvent;
import com.miui.server.input.knock.view.KnockPathListener;
/* loaded from: classes.dex */
public abstract class KnockGestureChecker {
    protected static final int STATE_CHECKING_ALL_CHECKER = 1;
    protected static final int STATE_CHECKING_ONLY_CHECKER = 2;
    protected static final int STATE_FAIL = 4;
    protected static final int STATE_SUCCESS = 3;
    protected int mCheckState = 1;
    protected Context mContext;
    protected String mFunction;
    protected KnockPathListener mKnockPathListener;

    public abstract void onTouchEvent(MotionEvent motionEvent);

    public KnockGestureChecker(Context context) {
        this.mContext = context;
    }

    public boolean continueCheck() {
        return this.mCheckState < 3;
    }

    public boolean checkOnlyOneGesture() {
        int i = this.mCheckState;
        return i == 3 || i == 2;
    }

    public void setCheckSuccess() {
        this.mCheckState = 3;
    }

    public void setCheckFail() {
        this.mCheckState = 4;
    }

    public void resetState() {
        this.mCheckState = 1;
    }

    public void setFunction(String function) {
        this.mFunction = function;
    }

    public void onScreenSizeChanged() {
    }

    public void setKnockPathListener(KnockPathListener knockPathListener) {
        this.mKnockPathListener = knockPathListener;
    }

    public boolean checkEmpty(String feature) {
        if (TextUtils.isEmpty(feature) || feature.equals("none")) {
            return true;
        }
        return false;
    }
}
