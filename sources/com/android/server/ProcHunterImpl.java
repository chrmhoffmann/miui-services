package com.android.server;

import android.os.SystemProperties;
import android.util.Log;
import com.miui.base.MiuiStubRegistry;
/* loaded from: classes.dex */
public class ProcHunterImpl extends ProcHunterStub {
    private static final String SYSPROP_ENABLE_PROCHUNTER = "persist.sys.debug.enable_prochunter";
    private static final String TAG = "ProcHunter";

    private static native void nNotifyScreenState(boolean z);

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<ProcHunterImpl> {

        /* compiled from: ProcHunterImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final ProcHunterImpl INSTANCE = new ProcHunterImpl();
        }

        public ProcHunterImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public ProcHunterImpl provideNewInstance() {
            return new ProcHunterImpl();
        }
    }

    static {
        try {
            if (SystemProperties.getBoolean(SYSPROP_ENABLE_PROCHUNTER, false)) {
                Log.i(TAG, "Load libprochunter");
                System.loadLibrary("procdaemon");
            }
        } catch (UnsatisfiedLinkError e) {
            Log.w(TAG, "can't loadLibrary libprochunter", e);
        }
    }

    public void start() {
        Log.i(TAG, "Here we start the ProcHunter...");
    }

    public void notifyScreenState(boolean state) {
        nNotifyScreenState(state);
    }
}
