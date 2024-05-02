package com.android.server.policy;

import miui.util.ObjectReference;
import miui.util.ReflectionUtils;
/* loaded from: classes.dex */
public class PhoneWindowManagerFeatureImpl {
    public Object getLock(PhoneWindowManager phoneWindowManager) {
        ObjectReference<Object> ref = ReflectionUtils.tryGetObjectField(phoneWindowManager, "mLock", Object.class);
        if (ref == null) {
            return null;
        }
        return ref.get();
    }

    public Runnable getScreenshotChordLongPress(PhoneWindowManager manager) {
        ObjectReference<Runnable> ref = ReflectionUtils.tryGetObjectField(manager, "mScreenshotRunnable", Runnable.class);
        if (ref == null) {
            return null;
        }
        return (Runnable) ref.get();
    }

    public Runnable getPowerLongPress(PhoneWindowManager manager) {
        ObjectReference<Runnable> ref = ReflectionUtils.tryGetObjectField(manager, "mEndCallLongPress", Runnable.class);
        if (ref == null) {
            return null;
        }
        return (Runnable) ref.get();
    }

    public void setPowerLongPress(PhoneWindowManager manager, Runnable value) {
        ReflectionUtils.trySetObjectField(manager, "mEndCallLongPress", value);
    }

    public void callInterceptPowerKeyUp(PhoneWindowManager manager, boolean canceled) {
    }

    public boolean isScreenOnFully(PhoneWindowManager manager) {
        if (manager.mDefaultDisplayPolicy == null) {
            return false;
        }
        return manager.mDefaultDisplayPolicy.isScreenOnFully();
    }
}
