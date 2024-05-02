package com.miui.server.greeze;

import android.app.ActivityManager;
import java.util.Arrays;
import miui.process.RunningProcessInfo;
/* loaded from: classes.dex */
public class RunningProcess {
    int adj;
    boolean hasForegroundActivities;
    boolean hasForegroundServices;
    int pid;
    String[] pkgList;
    int procState;
    String processName;
    int uid;

    public RunningProcess(RunningProcessInfo info) {
        this.pid = info.mPid;
        this.uid = info.mUid;
        this.processName = info.mProcessName;
        this.pkgList = info.mPkgList != null ? info.mPkgList : new String[0];
        this.adj = info.mAdj;
        this.procState = info.mProcState;
        this.hasForegroundActivities = info.mHasForegroundActivities;
        this.hasForegroundServices = info.mHasForegroundServices;
    }

    public RunningProcess(ActivityManager.RunningAppProcessInfo info) {
        this.pid = info.pid;
        this.uid = info.uid;
        this.processName = info.processName;
        this.pkgList = info.pkgList;
    }

    public String toString() {
        String str = "";
        StringBuilder append = new StringBuilder().append(this.uid).append(" ").append(this.pid).append(" ").append(this.processName).append(" ").append(Arrays.toString(this.pkgList)).append(" ").append(this.adj).append(" ").append(this.procState).append(this.hasForegroundActivities ? " FA" : str);
        if (this.hasForegroundServices) {
            str = " FS";
        }
        return append.append(str).toString();
    }
}
