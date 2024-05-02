package com.android.server.display;

import com.miui.base.MiuiStubRegistry;
/* loaded from: classes.dex */
public class ScreenEffectServiceStubImpl implements ScreenEffectServiceStub {

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<ScreenEffectServiceStubImpl> {

        /* compiled from: ScreenEffectServiceStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final ScreenEffectServiceStubImpl INSTANCE = new ScreenEffectServiceStubImpl();
        }

        public ScreenEffectServiceStubImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public ScreenEffectServiceStubImpl provideNewInstance() {
            return new ScreenEffectServiceStubImpl();
        }
    }

    public void updateScreenEffect(int state) {
        if (ScreenEffectService.sScreenEffectManager != null) {
            ScreenEffectService.sScreenEffectManager.updateScreenEffect(state);
        }
    }

    public void updateDozeBrightness(long physicalDisplayId, int brightness) {
        if (ScreenEffectService.sScreenEffectManager != null) {
            ScreenEffectService.sScreenEffectManager.updateDozeBrightness(physicalDisplayId, brightness);
        }
    }

    public void updateBCBCState(int state) {
        if (ScreenEffectService.sScreenEffectManager != null) {
            ScreenEffectService.sScreenEffectManager.updateBCBCState(state);
        }
    }
}
