package com.android.server.wm;

import com.miui.base.MiuiStubRegistry;
/* loaded from: classes.dex */
public class WindowStateAnimatorImpl implements WindowStateAnimatorStub {

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<WindowStateAnimatorImpl> {

        /* compiled from: WindowStateAnimatorImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final WindowStateAnimatorImpl INSTANCE = new WindowStateAnimatorImpl();
        }

        public WindowStateAnimatorImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public WindowStateAnimatorImpl provideNewInstance() {
            return new WindowStateAnimatorImpl();
        }
    }

    WindowStateAnimatorImpl() {
    }

    public boolean isAllowedDisableScreenshot(WindowState w) {
        return (w.mAttrs.extraFlags & 8388608) != 0;
    }
}
