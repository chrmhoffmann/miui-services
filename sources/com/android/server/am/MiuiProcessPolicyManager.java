package com.android.server.am;

import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Slog;
import com.android.server.wm.ScreenRotationAnimationImpl;
import miui.process.ProcessManager;
/* loaded from: classes.dex */
public final class MiuiProcessPolicyManager {
    private static final String TAG = "MiuiProcessPolicyManager";
    private static volatile ProcessManagerService sPmInstance;
    private static final boolean ENABLE = SystemProperties.getBoolean("persist.am.enable_ppm", true);
    private static final boolean ENABLE_PROMOTE_SUBPROCESS = SystemProperties.getBoolean("persist.am.enable_promote_sub", false);
    private static final int DEATH_COUNT_LIMIT = SystemProperties.getInt("persist.am.death_limit", 3);
    private static MiuiProcessPolicyManager sInstance = new MiuiProcessPolicyManager();

    public static MiuiProcessPolicyManager getInstance() {
        return sInstance;
    }

    private ProcessManagerService getProcessManagerService() {
        if (sPmInstance == null) {
            sPmInstance = (ProcessManagerService) ServiceManager.getService("ProcessManager");
        }
        return sPmInstance;
    }

    public boolean isNeedTraceProcess(ProcessRecord app) {
        if (getProcessManagerService() == null) {
            return false;
        }
        return ENABLE ? getProcessManagerService().getProcessPolicy().getWhiteList(128).contains(app.processName) : app.isPersistent();
    }

    public void promoteImportantProcAdj(ProcessRecord app) {
        if (app.mState.getMaxAdj() <= 0) {
            return;
        }
        if (isInWhiteList(app.processName, app.info.packageName)) {
            if (app.mState.getMaxAdj() > 800) {
                app.mState.setMaxAdj((int) ScreenRotationAnimationImpl.COVER_EGE);
            }
            Slog.d(TAG, "promote " + app.processName + " maxAdj to " + ProcessList.makeOomAdjString(app.mState.getMaxAdj(), false) + ", maxProcState to + " + ProcessList.makeProcStateString(ProcessPolicy.getAppMaxProcState(app)));
        }
        if (isSecretlyProtectProcess(app.processName)) {
            if (app.mState.getMaxAdj() > ProcessManager.SEC_PROTECT_MAX_ADJ) {
                app.mState.setMaxAdj(ProcessManager.SEC_PROTECT_MAX_ADJ);
            }
            if (ProcessPolicy.getAppMaxProcState(app) > ProcessManager.SEC_PROTECT_MAX_PROCESS_STATE) {
                ProcessPolicy.setAppMaxProcState(app, ProcessManager.SEC_PROTECT_MAX_PROCESS_STATE);
            }
            Slog.d(TAG, "promote " + app.processName + " maxAdj to " + ProcessList.makeOomAdjString(app.mState.getMaxAdj(), false) + ", maxProcState to + " + ProcessList.makeProcStateString(ProcessPolicy.getAppMaxProcState(app)));
        } else if (isLockedProcess(app.processName, app.userId)) {
            if (app.mState.getMaxAdj() > ProcessManager.LOCKED_MAX_ADJ) {
                app.mState.setMaxAdj(ProcessManager.LOCKED_MAX_ADJ);
            }
            if (ProcessPolicy.getAppMaxProcState(app) > ProcessManager.LOCKED_MAX_PROCESS_STATE) {
                ProcessPolicy.setAppMaxProcState(app, ProcessManager.LOCKED_MAX_PROCESS_STATE);
            }
            Slog.d(TAG, "promote " + app.processName + " maxAdj to " + ProcessList.makeOomAdjString(app.mState.getMaxAdj(), false) + ", maxProcState to + " + ProcessList.makeProcStateString(ProcessPolicy.getAppMaxProcState(app)));
        }
    }

    private boolean isLockedProcess(String processName, int userId) {
        try {
            if (getProcessManagerService() == null || !ENABLE) {
                return false;
            }
            return getProcessManagerService().isLockedApplication(processName, userId);
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean isSecretlyProtectProcess(String processName) {
        return getProcessManagerService() != null && ENABLE && getProcessManagerService().getProcessPolicy().isInSecretlyProtectList(processName);
    }

    private boolean isInWhiteList(String processName, String packageName) {
        if (processName == null || packageName == null) {
            return false;
        }
        ProcessPolicy policy = getProcessManagerService().getProcessPolicy();
        return policy.isInProcessStaticWhiteList(processName) || policy.getWhiteList(1).contains(packageName);
    }
}
