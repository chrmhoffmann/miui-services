package com.android.server.am;

import android.app.IApplicationThread;
import android.os.Bundle;
import android.os.RemoteException;
/* loaded from: classes.dex */
public class ProcessKiller {
    private static final String TAG = "ProcessManager";
    private ActivityManagerService mActivityManagerService;

    public ProcessKiller(ActivityManagerService ams) {
        this.mActivityManagerService = ams;
    }

    private boolean isInterestingToUser(ProcessRecord app) {
        return app.getWindowProcessController().isInterestingToUser();
    }

    public void forceStopPackage(ProcessRecord app, String reason, boolean evenForeground) {
        if (!evenForeground && isInterestingToUser(app)) {
            return;
        }
        forceStopPackage(app.info.packageName, app.userId, reason);
    }

    public boolean killApplication(ProcessRecord app, String reason, boolean evenForeground) {
        synchronized (this.mActivityManagerService) {
            if (!evenForeground) {
                if (isInterestingToUser(app)) {
                    return false;
                }
            }
            killLocked(app, reason);
            return true;
        }
    }

    public void killBackgroundApplication(ProcessRecord app, String reason) {
        this.mActivityManagerService.killBackgroundProcesses(app.info.packageName, app.userId, reason);
    }

    public void trimMemory(ProcessRecord app, boolean evenForeground) {
        if (!evenForeground && isInterestingToUser(app)) {
            return;
        }
        if (app.info.packageName.equals("android")) {
            scheduleTrimMemory(app, 60);
        } else {
            scheduleTrimMemory(app, 80);
        }
    }

    private void scheduleTrimMemory(ProcessRecord app, int level) {
        if (app != null && app.getThread() != null) {
            try {
                app.getThread().scheduleTrimMemory(level);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void killLocked(ProcessRecord app, String reason) {
        if (app != null) {
            app.killLocked(reason, 13, true);
        }
    }

    private void forceStopPackage(String packageName, int userId, String reason) {
        this.mActivityManagerService.forceStopPackage(packageName, userId, reason);
    }

    public static boolean scheduleCrashApp(ActivityManagerService mService, ProcessRecord app, String reason) {
        if (app != null) {
            IApplicationThread thread = app.getThread();
            if (thread != null) {
                synchronized (mService) {
                    app.scheduleCrashLocked(reason, 0, (Bundle) null);
                }
                return true;
            }
        }
        return false;
    }
}
