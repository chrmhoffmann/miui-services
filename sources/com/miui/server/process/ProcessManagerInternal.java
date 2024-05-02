package com.miui.server.process;

import android.content.ComponentName;
import android.content.pm.ApplicationInfo;
import com.android.server.am.HostingRecord;
import com.android.server.am.ProcessRecord;
import com.android.server.wm.ForegroundInfoManager;
import java.util.List;
import miui.process.IMiuiApplicationThread;
import miui.process.RunningProcessInfo;
/* loaded from: classes.dex */
public abstract class ProcessManagerInternal {
    public abstract boolean checkAppFgServices(int i);

    public abstract void forceStopPackage(String str, int i, String str2);

    public abstract List<RunningProcessInfo> getAllRunningProcessInfo();

    public abstract IMiuiApplicationThread getMiuiApplicationThread(int i);

    public abstract ApplicationInfo getMultiWindowForegroundAppInfoLocked();

    public abstract boolean isAllowRestartProcessLock(String str, int i, int i2, String str2, String str3, HostingRecord hostingRecord);

    public abstract boolean isForegroundApp(String str, int i);

    public abstract boolean isPackageFastBootEnable(String str, int i, boolean z);

    public abstract void notifyActivityChanged(ComponentName componentName);

    public abstract void notifyForegroundInfoChanged(ForegroundInfoManager.FgActivityChangedInfo fgActivityChangedInfo);

    public abstract void notifyForegroundWindowChanged(ForegroundInfoManager.FgWindowChangedInfo fgWindowChangedInfo);

    public abstract void notifyProcessDied(ProcessRecord processRecord);

    public abstract void notifyProcessKill(ProcessRecord processRecord, String str);

    public abstract void notifyProcessStarted(ProcessRecord processRecord);

    public abstract boolean restartDiedAppOrNot(ProcessRecord processRecord, boolean z, boolean z2, boolean z3);

    public abstract void setSpeedTestState(boolean z);

    public abstract void updateProcessForegroundLocked(int i);
}
