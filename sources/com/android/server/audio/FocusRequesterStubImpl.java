package com.android.server.audio;

import android.os.Handler;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.util.Log;
import com.miui.base.MiuiStubRegistry;
import com.miui.server.greeze.GreezeManagerService;
import miui.greeze.IGreezeManager;
/* loaded from: classes.dex */
public class FocusRequesterStubImpl implements FocusRequesterStub {
    private static final String TAG = "FocusRequesterStubImpl";
    private IGreezeManager mIGreezeManager = null;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<FocusRequesterStubImpl> {

        /* compiled from: FocusRequesterStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final FocusRequesterStubImpl INSTANCE = new FocusRequesterStubImpl();
        }

        public FocusRequesterStubImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public FocusRequesterStubImpl provideNewInstance() {
            return new FocusRequesterStubImpl();
        }
    }

    public IGreezeManager getGreeze() {
        if (this.mIGreezeManager == null) {
            this.mIGreezeManager = IGreezeManager.Stub.asInterface(ServiceManager.getService(GreezeManagerService.SERVICE_NAME));
        }
        return this.mIGreezeManager;
    }

    public void notifyFocusChange(final int uid, Handler h) {
        if (getGreeze() != null && UserHandle.isApp(uid) && h != null) {
            Log.d(TAG, "notifyFocusChange uid=" + uid);
            h.post(new Runnable() { // from class: com.android.server.audio.FocusRequesterStubImpl.1
                @Override // java.lang.Runnable
                public void run() {
                    int[] uids = {uid};
                    try {
                        if (FocusRequesterStubImpl.this.getGreeze().isUidFrozen(uid)) {
                            FocusRequesterStubImpl.this.getGreeze().thawUids(uids, 1000, "audioFocus");
                        }
                    } catch (Exception e) {
                        Log.e(FocusRequesterStubImpl.TAG, "notifyFocusChange err:", e);
                    }
                }
            });
        }
    }
}
