package com.android.server.am;

import android.content.Context;
import android.os.ServiceManager;
/* loaded from: classes.dex */
public class MiuiWarnings {
    private Context mContext;
    private ActivityManagerService mService;

    /* loaded from: classes.dex */
    public interface WarningCallback {
        void onCallback(boolean z);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class NoPreloadHolder {
        private static final MiuiWarnings INSTANCE = new MiuiWarnings();

        private NoPreloadHolder() {
        }
    }

    private MiuiWarnings() {
    }

    public static MiuiWarnings getInstance() {
        return NoPreloadHolder.INSTANCE;
    }

    public void init(Context context) {
        this.mContext = context;
    }

    public boolean showWarningDialog(String packageLabel, WarningCallback callback) {
        checkService();
        if (this.mService.mAtmInternal.canShowErrorDialogs()) {
            new MiuiWarningDialog(packageLabel, this.mContext, callback).show();
            return true;
        }
        return false;
    }

    private void checkService() {
        if (this.mService == null) {
            this.mService = ServiceManager.getService("activity");
        }
    }
}
