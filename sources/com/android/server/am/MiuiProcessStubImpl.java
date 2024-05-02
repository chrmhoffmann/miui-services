package com.android.server.am;

import com.miui.base.MiuiStubRegistry;
/* loaded from: classes.dex */
public class MiuiProcessStubImpl implements MiuiProcessStub {

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MiuiProcessStubImpl> {

        /* compiled from: MiuiProcessStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MiuiProcessStubImpl INSTANCE = new MiuiProcessStubImpl();
        }

        public MiuiProcessStubImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public MiuiProcessStubImpl provideNewInstance() {
            return new MiuiProcessStubImpl();
        }
    }

    public int getSchedModeAnimatorRt() {
        return 1;
    }

    public long getLaunchRtSchedDurationMs() {
        return 500L;
    }

    public int getSchedModeNormal() {
        return 0;
    }

    public long getScrollRtSchedDurationMs() {
        return 5000L;
    }

    public long getTouchRtSchedDurationMs() {
        return 2000L;
    }

    public int getSchedModeTouchRt() {
        return 2;
    }
}
