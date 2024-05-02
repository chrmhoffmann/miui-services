package com.miui.server.input.gesture;

import android.os.Looper;
import android.view.InputChannel;
import android.view.InputEvent;
import android.view.InputEventReceiver;
import android.view.MotionEvent;
import com.miui.server.input.MiuiEventRunnable;
import com.miui.server.input.MiuiInputMonitor;
import java.util.ArrayList;
/* loaded from: classes.dex */
public class MiuiGestureEventDispatcher extends InputEventReceiver {
    private final InputChannel mInputChannel;
    private final ArrayList<MiuiGestureListener> mListeners = new ArrayList<>();
    private MiuiGestureListener[] mListenersArray = new MiuiGestureListener[0];
    private final MiuiEventRunnable mEventRunnable = new MiuiEventRunnable();

    public MiuiGestureEventDispatcher(InputChannel inputChannel, Looper looper) {
        super(inputChannel, looper);
        this.mInputChannel = inputChannel;
    }

    public void onInputEvent(InputEvent event) {
        MiuiGestureListener[] listeners;
        if ((event instanceof MotionEvent) && (event.getSource() & 2) != 0) {
            MotionEvent motionEvent = (MotionEvent) event;
            synchronized (this.mListeners) {
                if (this.mListenersArray == null) {
                    MiuiGestureListener[] miuiGestureListenerArr = new MiuiGestureListener[this.mListeners.size()];
                    this.mListenersArray = miuiGestureListenerArr;
                    this.mListeners.toArray(miuiGestureListenerArr);
                }
                listeners = this.mListenersArray;
            }
            for (int i = 0; i < listeners.length; i++) {
                this.mEventRunnable.setData(motionEvent, listeners[i].getClass().getSimpleName());
                MiuiInputMonitor.getInstance().onEvent(this.mEventRunnable);
                listeners[i].onPointerEvent(motionEvent);
                MiuiInputMonitor.getInstance().onFinishEvent(this.mEventRunnable);
            }
        }
        finishInputEvent(event, false);
    }

    public void registerInputEventListener(MiuiGestureListener listener) {
        synchronized (this.mListeners) {
            if (this.mListeners.contains(listener)) {
                throw new IllegalStateException("registerInputEventListener: trying to register" + listener + " twice.");
            }
            this.mListeners.add(listener);
            this.mListenersArray = null;
        }
    }

    public void unregisterInputEventListener(MiuiGestureListener listener) {
        synchronized (this.mListeners) {
            if (!this.mListeners.contains(listener)) {
                throw new IllegalStateException("registerInputEventListener: " + listener + " not registered.");
            }
            this.mListeners.remove(listener);
            this.mListenersArray = null;
        }
    }

    public int getGestureListenerCount() {
        int size;
        synchronized (this.mListeners) {
            size = this.mListeners.size();
        }
        return size;
    }

    public void dispose() {
        super.dispose();
        this.mInputChannel.dispose();
        synchronized (this.mListeners) {
            this.mListeners.clear();
            this.mListenersArray = null;
        }
    }
}
