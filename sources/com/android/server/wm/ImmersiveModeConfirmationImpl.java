package com.android.server.wm;

import android.content.Context;
import com.miui.base.MiuiStubRegistry;
import java.io.PrintWriter;
/* loaded from: classes.dex */
public class ImmersiveModeConfirmationImpl implements ImmersiveModeConfirmationStub {

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<ImmersiveModeConfirmationImpl> {

        /* compiled from: ImmersiveModeConfirmationImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final ImmersiveModeConfirmationImpl INSTANCE = new ImmersiveModeConfirmationImpl();
        }

        public ImmersiveModeConfirmationImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public ImmersiveModeConfirmationImpl provideNewInstance() {
            return new ImmersiveModeConfirmationImpl();
        }
    }

    ImmersiveModeConfirmationImpl() {
    }

    public boolean reloadFromSetting(Context cxt) {
        return PolicyControl.reloadFromSetting(cxt);
    }

    public boolean disableImmersiveConfirmation(String pkg) {
        return PolicyControl.disableImmersiveConfirmation(pkg);
    }

    public void dump(String prefix, PrintWriter pw) {
        PolicyControl.dump(prefix, pw);
    }
}
