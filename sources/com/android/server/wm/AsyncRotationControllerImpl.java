package com.android.server.wm;

import com.miui.base.MiuiStubRegistry;
/* loaded from: classes.dex */
public class AsyncRotationControllerImpl implements AsyncRotationControllerStub {

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<AsyncRotationControllerImpl> {

        /* compiled from: AsyncRotationControllerImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final AsyncRotationControllerImpl INSTANCE = new AsyncRotationControllerImpl();
        }

        public AsyncRotationControllerImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public AsyncRotationControllerImpl provideNewInstance() {
            return new AsyncRotationControllerImpl();
        }
    }

    public boolean excludeFromFadeRotationAnimation(WindowState windowState, boolean isFade) {
        if (windowState == null || windowState.mAttrs == null || !isFade || ((windowState.getWindowType() != 2017 || !windowState.getWindowTag().equals("control_center")) && (windowState.mAttrs.extraFlags & 268435456) == 0)) {
            return false;
        }
        return true;
    }
}
