package com.miui.server.input.stylus;

import android.content.Context;
import android.view.MotionEvent;
import android.view.WindowManager;
import com.miui.server.input.gesture.MiuiGestureMonitor;
/* loaded from: classes.dex */
public abstract class BaseStylusGestureChecker {
    protected static final int STATE_CHECK_FAIL = 2;
    protected static final int STATE_CHECK_SUCCESS = 3;
    protected static final int STATE_DETECTING = 1;
    protected static final int STATE_NO_DETECT = 0;
    protected Context mContext;
    protected MiuiGestureMonitor mMiuiGestureMonitor;
    protected WindowManager mWindowManager;

    public abstract void onPointerEvent(MotionEvent motionEvent);

    public BaseStylusGestureChecker(Context context) {
        this.mContext = context;
        this.mWindowManager = (WindowManager) context.getSystemService(WindowManager.class);
        this.mMiuiGestureMonitor = MiuiGestureMonitor.getInstance(context);
    }
}
