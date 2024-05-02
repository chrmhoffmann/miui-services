package com.android.server.am;

import android.os.SystemProperties;
import android.util.Slog;
import com.android.server.LocalServices;
import com.android.server.MiuiNetworkManagementService;
import com.miui.base.MiuiStubRegistry;
import com.miui.server.process.ProcessManagerInternal;
/* loaded from: classes.dex */
public class ProcessListStubImpl implements ProcessListStub {
    private static final String TAG = "ProcessListStubImpl";
    ProcessManagerInternal mPmi = null;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<ProcessListStubImpl> {

        /* compiled from: ProcessListStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final ProcessListStubImpl INSTANCE = new ProcessListStubImpl();
        }

        public ProcessListStubImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public ProcessListStubImpl provideNewInstance() {
            return new ProcessListStubImpl();
        }
    }

    public void updateOomMinFreeIfNeeded(long mTotalMemMb, int[] mOomMinFree) {
        String prop;
        if (mTotalMemMb < 4096) {
            prop = SystemProperties.get("persist.sys.minfree_def", (String) null);
        } else if (mTotalMemMb < 6144) {
            prop = SystemProperties.get("persist.sys.minfree_6g", (String) null);
        } else {
            prop = SystemProperties.get("persist.sys.minfree_8g", (String) null);
        }
        if (prop == null) {
            return;
        }
        String[] minFrees = prop.split(",");
        if (minFrees.length != 6) {
            return;
        }
        int[] newValues = new int[minFrees.length];
        int index = 0;
        for (String str : minFrees) {
            try {
                int minfree = Integer.parseInt(str);
                if (minfree <= 0) {
                    return;
                }
                newValues[index] = minfree;
                index++;
            } catch (Exception e) {
                return;
            }
        }
        System.arraycopy(newValues, 0, mOomMinFree, 0, 6);
        Slog.w(TAG, "Override mOomMinFree with: " + prop);
    }

    public void notifyProcessStarted(ProcessRecord app, int pid) {
        MiuiProcessPolicyManager.getInstance().promoteImportantProcAdj(app);
        if (this.mPmi == null) {
            ProcessManagerInternal processManagerInternal = (ProcessManagerInternal) LocalServices.getService(ProcessManagerInternal.class);
            this.mPmi = processManagerInternal;
            if (processManagerInternal == null) {
                return;
            }
        }
        this.mPmi.notifyProcessStarted(app);
        MiuiNetworkManagementService.getInstance().setPidForPackage(app.info.packageName, pid, app.uid);
    }

    public void notifyProcessKill(ProcessRecord app, String reason) {
        if (this.mPmi == null) {
            ProcessManagerInternal processManagerInternal = (ProcessManagerInternal) LocalServices.getService(ProcessManagerInternal.class);
            this.mPmi = processManagerInternal;
            if (processManagerInternal == null) {
                return;
            }
        }
        this.mPmi.notifyProcessKill(app, reason);
    }

    public void notifyProcessDied(ProcessRecord app) {
        if (this.mPmi == null) {
            ProcessManagerInternal processManagerInternal = (ProcessManagerInternal) LocalServices.getService(ProcessManagerInternal.class);
            this.mPmi = processManagerInternal;
            if (processManagerInternal == null) {
                return;
            }
        }
        this.mPmi.notifyProcessDied(app);
    }

    public boolean isNeedTraceProcess(ProcessRecord app) {
        return MiuiProcessPolicyManager.getInstance().isNeedTraceProcess(app);
    }

    public boolean isAllowRestartProcessLock(String processName, int flag, int uid, String pkgName, String callerPackage, HostingRecord hostingRecord) {
        if (this.mPmi == null) {
            ProcessManagerInternal processManagerInternal = (ProcessManagerInternal) LocalServices.getService(ProcessManagerInternal.class);
            this.mPmi = processManagerInternal;
            if (processManagerInternal == null) {
                return true;
            }
        }
        return this.mPmi.isAllowRestartProcessLock(processName, flag, uid, pkgName, callerPackage, hostingRecord);
    }

    public boolean restartDiedAppOrNot(ProcessRecord app, boolean isHomeApp, boolean allowRestart, boolean fromBinderDied) {
        if (this.mPmi == null) {
            ProcessManagerInternal processManagerInternal = (ProcessManagerInternal) LocalServices.getService(ProcessManagerInternal.class);
            this.mPmi = processManagerInternal;
            if (processManagerInternal == null) {
                return allowRestart;
            }
        }
        return this.mPmi.restartDiedAppOrNot(app, isHomeApp, allowRestart, fromBinderDied);
    }
}
