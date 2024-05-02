package com.miui.server.input.gesture;

import android.content.Context;
import android.hardware.input.InputManager;
import android.os.HandlerThread;
import android.view.InputMonitor;
/* loaded from: classes.dex */
public class MiuiGestureMonitor {
    private static MiuiGestureMonitor sMiuiGestureMonitor;
    private final int mDisplayId;
    private InputMonitor mGestureInputMonitor;
    private MiuiGestureEventDispatcher mGesturePointerEventDispatcher;
    private final HandlerThread mHandlerThread;

    private MiuiGestureMonitor(Context context) {
        HandlerThread handlerThread = new HandlerThread("miui-gesture", -4);
        this.mHandlerThread = handlerThread;
        this.mDisplayId = context.getDisplayId();
        handlerThread.start();
    }

    public static synchronized MiuiGestureMonitor getInstance(Context context) {
        MiuiGestureMonitor miuiGestureMonitor;
        synchronized (MiuiGestureMonitor.class) {
            if (sMiuiGestureMonitor == null) {
                sMiuiGestureMonitor = new MiuiGestureMonitor(context);
            }
            miuiGestureMonitor = sMiuiGestureMonitor;
        }
        return miuiGestureMonitor;
    }

    private void registerGestureMonitor() {
        this.mGestureInputMonitor = InputManager.getInstance().monitorGestureInput("miui-gesture", this.mDisplayId);
        this.mGesturePointerEventDispatcher = new MiuiGestureEventDispatcher(this.mGestureInputMonitor.getInputChannel(), this.mHandlerThread.getLooper());
    }

    private void unregisterGestureMonitor() {
        InputMonitor inputMonitor = this.mGestureInputMonitor;
        if (inputMonitor != null) {
            inputMonitor.dispose();
        }
        this.mGestureInputMonitor = null;
        this.mGesturePointerEventDispatcher = null;
    }

    public void registerPointerEventListener(MiuiGestureListener listener) {
        MiuiGestureEventDispatcher miuiGestureEventDispatcher = this.mGesturePointerEventDispatcher;
        if (miuiGestureEventDispatcher == null || miuiGestureEventDispatcher.getGestureListenerCount() == 0) {
            registerGestureMonitor();
        }
        this.mGesturePointerEventDispatcher.registerInputEventListener(listener);
    }

    public void unregisterPointerEventListener(MiuiGestureListener listener) {
        MiuiGestureEventDispatcher miuiGestureEventDispatcher = this.mGesturePointerEventDispatcher;
        if (miuiGestureEventDispatcher == null) {
            return;
        }
        miuiGestureEventDispatcher.unregisterInputEventListener(listener);
        if (this.mGesturePointerEventDispatcher.getGestureListenerCount() == 0) {
            unregisterGestureMonitor();
        }
    }

    public void pilferPointers() {
        this.mGestureInputMonitor.pilferPointers();
    }
}
