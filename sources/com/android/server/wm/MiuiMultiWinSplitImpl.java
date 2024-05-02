package com.android.server.wm;

import android.os.IBinder;
import com.miui.base.MiuiStubRegistry;
/* loaded from: classes.dex */
public class MiuiMultiWinSplitImpl implements MiuiMultiWinSplitStub {
    private MiuiMultiWinSplitService mMiuiMultiWinSplitService;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MiuiMultiWinSplitImpl> {

        /* compiled from: MiuiMultiWinSplitImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MiuiMultiWinSplitImpl INSTANCE = new MiuiMultiWinSplitImpl();
        }

        public MiuiMultiWinSplitImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public MiuiMultiWinSplitImpl provideNewInstance() {
            return new MiuiMultiWinSplitImpl();
        }
    }

    public void init(ActivityTaskManagerService service) {
        this.mMiuiMultiWinSplitService = new MiuiMultiWinSplitService(service);
    }

    public IBinder getMiuiMultiWinSplitService() {
        return this.mMiuiMultiWinSplitService;
    }
}
