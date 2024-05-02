package com.android.server.wm;

import com.miui.base.MiuiStubRegistry;
import com.miui.whetstone.client.WhetstoneClientManager;
/* loaded from: classes.dex */
public class DisplayPolicyStubImpl implements DisplayPolicyStub {

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<DisplayPolicyStubImpl> {

        /* compiled from: DisplayPolicyStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final DisplayPolicyStubImpl INSTANCE = new DisplayPolicyStubImpl();
        }

        public DisplayPolicyStubImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public DisplayPolicyStubImpl provideNewInstance() {
            return new DisplayPolicyStubImpl();
        }
    }

    public int getExtraNavigationBarAppearance(WindowState winCandidate, WindowState opaque, WindowState imeWindow, WindowState navColorWin) {
        int appearance = 0;
        if (navColorWin != null) {
            if ((navColorWin.getAttrs().extraFlags & 1048576) == 1048576) {
                appearance = 0 | 16;
            }
            if (winCandidate != null) {
                int flags = winCandidate.getAttrs().flags;
                if ((134217728 & flags) == 0 && (Integer.MIN_VALUE & flags) == 0) {
                    appearance &= -17;
                }
            }
            if (navColorWin.isDimming() && navColorWin != imeWindow && navColorWin != opaque) {
                appearance &= -17;
            }
        }
        if (winCandidate != null && (winCandidate.getAttrs().extraFlags & 32768) == 32768) {
            return appearance | 2048;
        }
        return appearance;
    }

    public boolean isMiuiVersion() {
        return true;
    }

    public void notifyOnScroll(boolean start) {
        WhetstoneClientManager.notifyOnScroll(start);
    }
}
