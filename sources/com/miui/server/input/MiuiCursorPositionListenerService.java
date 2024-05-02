package com.miui.server.input;

import android.os.ServiceManager;
import android.util.Slog;
import android.view.MotionEvent;
import android.view.WindowManagerPolicyConstants;
import com.android.server.wm.WindowManagerService;
import com.miui.server.input.MiuiCursorPositionListenerService;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
/* loaded from: classes.dex */
public class MiuiCursorPositionListenerService implements WindowManagerPolicyConstants.PointerEventListener {
    private static final String TAG = MiuiCursorPositionListenerService.class.getSimpleName();
    private static volatile MiuiCursorPositionListenerService sInstance;
    private final WindowManagerService mWindowManagerService = ServiceManager.getService("window");
    private final List<OnCursorPositionChangedListener> mOnCursorPositionChangedListeners = new ArrayList();
    private final List<OnCursorPositionChangedListener> mTempOnCursorPositionChangedListeners = new ArrayList();

    /* loaded from: classes.dex */
    public interface OnCursorPositionChangedListener {
        void onCursorPositionChanged(int i, float f, float f2);
    }

    private MiuiCursorPositionListenerService() {
    }

    public static MiuiCursorPositionListenerService getInstance() {
        if (sInstance == null) {
            synchronized (MiuiCursorPositionListenerService.class) {
                if (sInstance == null) {
                    sInstance = new MiuiCursorPositionListenerService();
                }
            }
        }
        return sInstance;
    }

    public void registerOnCursorPositionChangedListener(OnCursorPositionChangedListener listener) {
        if (listener == null) {
            throw new RuntimeException("The listener can not be null!");
        }
        synchronized (this.mOnCursorPositionChangedListeners) {
            if (this.mOnCursorPositionChangedListeners.contains(listener)) {
                throw new RuntimeException("The listener already registered");
            }
            this.mOnCursorPositionChangedListeners.add(listener);
            Slog.d(TAG, "a cursor position listener added, current size = " + this.mOnCursorPositionChangedListeners);
            populatePointerListenerLocked();
        }
    }

    public void unregisterOnCursorPositionChangedListener(OnCursorPositionChangedListener listener) {
        if (listener == null) {
            throw new RuntimeException("The listener can not be null!");
        }
        synchronized (this.mOnCursorPositionChangedListeners) {
            if (!this.mOnCursorPositionChangedListeners.contains(listener)) {
                throw new RuntimeException("The listener not registered");
            }
            this.mOnCursorPositionChangedListeners.remove(listener);
            Slog.d(TAG, "a cursor position listener removed, current size = " + this.mOnCursorPositionChangedListeners);
            unPopulatePointerListenerLocked();
        }
    }

    private void populatePointerListenerLocked() {
        if (this.mOnCursorPositionChangedListeners.size() != 1) {
            return;
        }
        Slog.d(TAG, "register pointer event listener to wms");
        this.mWindowManagerService.registerPointerEventListener(this, 0);
    }

    private void unPopulatePointerListenerLocked() {
        if (!this.mOnCursorPositionChangedListeners.isEmpty()) {
            return;
        }
        Slog.d(TAG, "unregister pointer event listener to wms");
        this.mWindowManagerService.unregisterPointerEventListener(this, 0);
    }

    private void deliverCursorPositionChangedEvent(final int deviceId, final float x, final float y) {
        this.mTempOnCursorPositionChangedListeners.clear();
        synchronized (this.mOnCursorPositionChangedListeners) {
            this.mTempOnCursorPositionChangedListeners.addAll(this.mOnCursorPositionChangedListeners);
        }
        this.mTempOnCursorPositionChangedListeners.forEach(new Consumer() { // from class: com.miui.server.input.MiuiCursorPositionListenerService$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ((MiuiCursorPositionListenerService.OnCursorPositionChangedListener) obj).onCursorPositionChanged(deviceId, x, y);
            }
        });
    }

    public void onPointerEvent(MotionEvent motionEvent) {
        if (motionEvent.getToolType(0) != 3) {
            return;
        }
        int action = motionEvent.getActionMasked();
        if (action == 2 || action == 7) {
            deliverCursorPositionChangedEvent(motionEvent.getDeviceId(), motionEvent.getX(), motionEvent.getY());
        }
    }
}
