package com.android.server.input;

import android.view.MotionEvent;
import com.android.server.LocalServices;
import com.android.server.input.shoulderkey.ShoulderKeyManagerInternal;
/* loaded from: classes.dex */
public class MiInputManager {
    private long mPtr;
    private ShoulderKeyManagerInternal mShoulderKeyManagerInternel;

    private static native String nativeDump();

    private static native boolean nativeHideCursor(long j);

    private static native long nativeInit(MiInputManager miInputManager);

    private static native void nativeInjectMotionEvent(long j, MotionEvent motionEvent, int i);

    private static native boolean nativeSetCursorPosition(long j, float f, float f2);

    private static native void nativeSetInputConfig(long j, int i, long j2);

    static {
        System.loadLibrary("miinputflinger");
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class MiInputManagerInstance {
        private static final MiInputManager sInstance = new MiInputManager();

        private MiInputManagerInstance() {
        }
    }

    private MiInputManager() {
        this.mShoulderKeyManagerInternel = null;
        this.mPtr = -1L;
        this.mPtr = nativeInit(this);
    }

    public static MiInputManager getInstance() {
        return MiInputManagerInstance.sInstance;
    }

    public void setInputConfig(int configType, long configPtr) {
        nativeSetInputConfig(this.mPtr, configType, configPtr);
    }

    public void injectMotionEvent(MotionEvent event, int mode) {
        nativeInjectMotionEvent(this.mPtr, event, mode);
    }

    private void notifyTouchMotionEvent(MotionEvent event) {
        if (this.mShoulderKeyManagerInternel == null) {
            this.mShoulderKeyManagerInternel = (ShoulderKeyManagerInternal) LocalServices.getService(ShoulderKeyManagerInternal.class);
        }
        ShoulderKeyManagerInternal shoulderKeyManagerInternal = this.mShoulderKeyManagerInternel;
        if (shoulderKeyManagerInternal != null) {
            shoulderKeyManagerInternal.notifyTouchMotionEvent(event);
        }
    }

    public String dump() {
        return nativeDump();
    }

    public boolean setCursorPosition(float x, float y) {
        return nativeSetCursorPosition(this.mPtr, x, y);
    }

    public boolean hideCursor() {
        return nativeHideCursor(this.mPtr);
    }
}
