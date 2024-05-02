package com.android.server.am;

import android.app.IApplicationThread;
import android.content.Context;
import android.os.Bundle;
import android.util.Slog;
import com.android.server.am.MiuiWarnings;
import com.miui.server.stability.ScoutDisplayMemoryManager;
/* loaded from: classes.dex */
public class ScoutMemoryError {
    private static final String TAG = "ScoutMemoryError";
    private static ScoutMemoryError memoryError;
    private Context mContext;
    private ActivityManagerService mService;

    public static ScoutMemoryError getInstance() {
        if (memoryError == null) {
            memoryError = new ScoutMemoryError();
        }
        return memoryError;
    }

    public void init(ActivityManagerService ams, Context context) {
        this.mService = ams;
        this.mContext = context;
    }

    private String getPackageLabelLocked(ProcessRecord app) {
        CharSequence labelChar;
        if (app == null || app.getPkgList().size() != 1 || (labelChar = this.mContext.getPackageManager().getApplicationLabel(app.info)) == null) {
            return null;
        }
        String label = labelChar.toString();
        return label;
    }

    public boolean showAppMemoryErrorDialog(final ProcessRecord app, final String reason) {
        final String packageLabel = getPackageLabelLocked(app);
        if (packageLabel == null) {
            return false;
        }
        this.mService.mUiHandler.postAtFrontOfQueue(new Runnable() { // from class: com.android.server.am.ScoutMemoryError$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                ScoutMemoryError.this.m462xf80bb15a(app, packageLabel, reason);
            }
        });
        return true;
    }

    public boolean showAppDisplayMemoryErrorDialog(final ProcessRecord app, final String reason, final ScoutDisplayMemoryManager.DiaplayMemoryErrorInfo errorInfo) {
        final String packageLabel = getPackageLabelLocked(app);
        if (packageLabel == null) {
            return false;
        }
        this.mService.mUiHandler.postAtFrontOfQueue(new Runnable() { // from class: com.android.server.am.ScoutMemoryError$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                ScoutMemoryError.this.m461x32ff97ed(app, packageLabel, reason, errorInfo);
            }
        });
        return true;
    }

    /* renamed from: showMemoryErrorDialog */
    public void m462xf80bb15a(final ProcessRecord app, String packageLabel, final String reason) {
        boolean showDialogSuccess = MiuiWarnings.getInstance().showWarningDialog(packageLabel, new MiuiWarnings.WarningCallback() { // from class: com.android.server.am.ScoutMemoryError.1
            @Override // com.android.server.am.MiuiWarnings.WarningCallback
            public void onCallback(boolean positive) {
                if (positive) {
                    Slog.d(ScoutMemoryError.TAG, "showMemoryErrorDialog ok");
                    ScoutMemoryError.this.scheduleCrashApp(app, reason);
                    return;
                }
                Slog.d(ScoutMemoryError.TAG, "showMemoryErrorDialog cancel");
            }
        });
        if (!showDialogSuccess) {
            Slog.d(TAG, "occur memory leak, showWarningDialog fail");
        }
    }

    /* renamed from: showDisplayMemoryErrorDialog */
    public void m461x32ff97ed(final ProcessRecord app, String packageLabel, final String reason, final ScoutDisplayMemoryManager.DiaplayMemoryErrorInfo errorInfo) {
        boolean showDialogSuccess = MiuiWarnings.getInstance().showWarningDialog(packageLabel, new MiuiWarnings.WarningCallback() { // from class: com.android.server.am.ScoutMemoryError.2
            @Override // com.android.server.am.MiuiWarnings.WarningCallback
            public void onCallback(boolean positive) {
                if (positive) {
                    Slog.d(ScoutMemoryError.TAG, "showDisplayMemoryErrorDialog ok");
                    ScoutMemoryError.this.scheduleCrashApp(app, reason);
                    errorInfo.setAction(4);
                } else {
                    Slog.d(ScoutMemoryError.TAG, "showDisplayMemoryErrorDialog cancel");
                    errorInfo.setAction(5);
                    ScoutDisplayMemoryManager.getInstance().setDisableState(true);
                }
                ScoutDisplayMemoryManager.getInstance().setShowDialogState(false);
                ScoutDisplayMemoryManager.getInstance().updateLastReportTime();
                ScoutDisplayMemoryManager.getInstance().reportDisplayMemoryLeakEvent(errorInfo);
            }
        });
        if (showDialogSuccess) {
            ScoutDisplayMemoryManager.getInstance().setShowDialogState(true);
        } else {
            Slog.d(TAG, "occur memory leak, showWarningDialog fail");
        }
    }

    public boolean scheduleCrashApp(ProcessRecord app, String reason) {
        if (app != null) {
            IApplicationThread thread = app.getThread();
            if (thread != null) {
                synchronized (this.mService) {
                    app.scheduleCrashLocked(reason, 0, (Bundle) null);
                }
                return true;
            }
        }
        return false;
    }
}
