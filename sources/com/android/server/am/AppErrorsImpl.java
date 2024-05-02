package com.android.server.am;

import android.content.Context;
import android.provider.MiuiSettings;
import com.android.server.am.AppErrorDialog;
import com.miui.base.MiuiStubRegistry;
/* loaded from: classes.dex */
public class AppErrorsImpl extends AppErrorsStub {

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<AppErrorsImpl> {

        /* compiled from: AppErrorsImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final AppErrorsImpl INSTANCE = new AppErrorsImpl();
        }

        public AppErrorsImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public AppErrorsImpl provideNewInstance() {
            return new AppErrorsImpl();
        }
    }

    public void handleShowAppErrorUI(Context ctx, ProcessErrorStateRecord errState, AppErrorDialog.Data data) {
        if (MiuiSettings.Secure.isForceCloseDialogEnabled(ctx)) {
            errState.getDialogController().showCrashDialogs(data);
        } else if (data.result != null) {
            data.result.set(AppErrorDialog.CANT_SHOW);
        }
    }
}
