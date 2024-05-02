package com.android.server.am;

import android.content.res.Resources;
import android.os.Message;
import com.miui.base.MiuiStubRegistry;
/* loaded from: classes.dex */
public class AppNotRespondingDialogImpl extends AppNotRespondingDialogStub {

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<AppNotRespondingDialogImpl> {

        /* compiled from: AppNotRespondingDialogImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final AppNotRespondingDialogImpl INSTANCE = new AppNotRespondingDialogImpl();
        }

        public AppNotRespondingDialogImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public AppNotRespondingDialogImpl provideNewInstance() {
            return new AppNotRespondingDialogImpl();
        }
    }

    void onInit(AppNotRespondingDialog dialog, boolean hasErrorReceiver, Message forceCloseMsg, Message waitMsg, Message waitAndReportMsg) {
        Resources res = dialog.getContext().getResources();
        dialog.setButton(-1, res.getText(286195739), forceCloseMsg);
        dialog.setButton(-2, res.getText(286195770), waitMsg);
        if (hasErrorReceiver) {
            dialog.setButton(-3, res.getText(286195752), waitAndReportMsg);
        }
    }

    boolean onCreate(AppNotRespondingDialog dialog) {
        dialog.getButton(-1).setId(16908755);
        dialog.getButton(-2).setId(16908759);
        dialog.getButton(-3).setId(16908757);
        return true;
    }
}
