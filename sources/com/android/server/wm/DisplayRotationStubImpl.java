package com.android.server.wm;

import com.miui.base.MiuiStubRegistry;
/* loaded from: classes.dex */
public class DisplayRotationStubImpl implements DisplayRotationStub {

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<DisplayRotationStubImpl> {

        /* compiled from: DisplayRotationStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final DisplayRotationStubImpl INSTANCE = new DisplayRotationStubImpl();
        }

        public DisplayRotationStubImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public DisplayRotationStubImpl provideNewInstance() {
            return new DisplayRotationStubImpl();
        }
    }

    public boolean forceEnableSeamless(WindowState w) {
        if (w != null && w.mAttrs != null && (w.mAttrs.extraFlags & 1073741824) != 0) {
            return true;
        }
        return false;
    }
}
