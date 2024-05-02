package com.android.server.statusbar;

import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Slog;
import com.android.internal.statusbar.IStatusBar;
import com.miui.base.MiuiStubRegistry;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
/* loaded from: classes.dex */
public class StatusBarManagerServiceStubImpl extends StatusBarManagerServiceStub {
    private static final boolean SPEW = false;
    private static final String TAG = "StatusBarManagerServiceStubImpl";
    private WeakReference<IStatusBar> mService;
    ArrayList<StatusRecord> mStatusRecords = new ArrayList<>();
    final Object mLock = new Object();

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<StatusBarManagerServiceStubImpl> {

        /* compiled from: StatusBarManagerServiceStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final StatusBarManagerServiceStubImpl INSTANCE = new StatusBarManagerServiceStubImpl();
        }

        public StatusBarManagerServiceStubImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public StatusBarManagerServiceStubImpl provideNewInstance() {
            return new StatusBarManagerServiceStubImpl();
        }
    }

    public void setStatus(int what, IBinder token, String action, Bundle ext, IStatusBar statusBar) {
        WeakReference<IStatusBar> weakReference;
        synchronized (this.mLock) {
            manageStatusListLocked(what, token, action);
            if (statusBar == null && (weakReference = this.mService) != null) {
                statusBar = weakReference.get();
            }
            if (statusBar != null) {
                try {
                    statusBar.setStatus(what, action, ext);
                } catch (RemoteException ex) {
                    Slog.e("StatusBarManagerServiceStubImpl RE", ex.toString());
                }
                WeakReference<IStatusBar> weakReference2 = this.mService;
                if (weakReference2 == null || weakReference2.get() != statusBar) {
                    this.mService = new WeakReference<>(statusBar);
                }
            }
        }
    }

    /* loaded from: classes.dex */
    public class StatusRecord implements IBinder.DeathRecipient {
        String action;
        IBinder token;

        StatusRecord() {
            StatusBarManagerServiceStubImpl.this = this$0;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            Slog.i(StatusBarManagerServiceStubImpl.TAG, "binder died for action=" + this.action);
            StatusBarManagerServiceStubImpl statusBarManagerServiceStubImpl = StatusBarManagerServiceStubImpl.this;
            statusBarManagerServiceStubImpl.setStatus(0, this.token, this.action, null, statusBarManagerServiceStubImpl.mService == null ? null : (IStatusBar) StatusBarManagerServiceStubImpl.this.mService.get());
            this.token.unlinkToDeath(this, 0);
        }
    }

    void manageStatusListLocked(int what, IBinder token, String action) {
        int N = this.mStatusRecords.size();
        StatusRecord tok = null;
        int i = 0;
        while (true) {
            if (i >= N) {
                break;
            }
            StatusRecord t = this.mStatusRecords.get(i);
            if (t.token != token) {
                i++;
            } else {
                tok = t;
                break;
            }
        }
        if (what == 0 || !token.isBinderAlive()) {
            if (tok != null) {
                this.mStatusRecords.remove(i);
                tok.token.unlinkToDeath(tok, 0);
                return;
            }
            return;
        }
        if (tok == null) {
            tok = new StatusRecord();
            try {
                token.linkToDeath(tok, 0);
                this.mStatusRecords.add(tok);
            } catch (RemoteException e) {
                return;
            }
        }
        tok.token = token;
        tok.action = action;
    }
}
