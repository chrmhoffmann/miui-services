package com.android.server.am;

import com.android.server.LocalServices;
import com.miui.base.MiuiStubRegistry;
import com.miui.server.rtboost.SchedBoostManagerInternal;
/* loaded from: classes.dex */
public class SchedBoostManagerInternalStubImpl implements SchedBoostManagerInternalStub {

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<SchedBoostManagerInternalStubImpl> {

        /* compiled from: SchedBoostManagerInternalStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final SchedBoostManagerInternalStubImpl INSTANCE = new SchedBoostManagerInternalStubImpl();
        }

        public SchedBoostManagerInternalStubImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public SchedBoostManagerInternalStubImpl provideNewInstance() {
            return new SchedBoostManagerInternalStubImpl();
        }
    }

    public void setSchedMode(String procName, int pid, int rtid, int schedMode, long timeout) {
        SchedBoostManagerInternal si = (SchedBoostManagerInternal) LocalServices.getService(SchedBoostManagerInternal.class);
        if (si != null) {
            si.beginSchedThreads(new int[]{pid, rtid}, timeout, procName, schedMode);
        }
    }

    public void setHomeRenderThreadTid(int tid) {
        SchedBoostManagerInternal si = (SchedBoostManagerInternal) LocalServices.getService(SchedBoostManagerInternal.class);
        if (si != null) {
            si.setHomeRenderThreadTid(tid);
        }
    }
}
