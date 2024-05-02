package com.android.server.am;

import android.app.ActivityManagerNative;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.util.Slog;
import com.android.server.input.pocketmode.MiuiPocketModeSensorWrapper;
import java.util.ArrayList;
import java.util.List;
/* loaded from: classes.dex */
public class ProcessKillerIdler extends JobService {
    static final long APP_MEM_THRESHOLD = 300;
    static final long BACKUP_APP_MEM_THRESHOLD = 100;
    static final long BACKUP_MEM_THRESHOLD = 1000;
    static final long CHECK_FREE_MEM_TIME = 21600000;
    private static final String TAG = "ProcessKillerIdler";
    static final ArrayList<String> blackList;
    private ActivityManagerService mAm = ActivityManagerNative.getDefault();
    private static ComponentName cm = new ComponentName("android", ProcessKillerIdler.class.getName());
    private static int PROCESS_KILL_JOB_ID = 100;

    static {
        ArrayList<String> arrayList = new ArrayList<>();
        blackList = arrayList;
        arrayList.add("com.tencent.mm");
        arrayList.add("com.tencent.mobileqq");
    }

    @Override // android.app.job.JobService
    public boolean onStartJob(JobParameters params) {
        char c;
        boolean z;
        boolean z2;
        char c2;
        Slog.w(TAG, "ProcessKillerIdler onStartJob");
        synchronized (this.mAm) {
            try {
                List<ProcessRecord> procs = ProcessUtils.getProcessListByAdj(this.mAm, MiuiPocketModeSensorWrapper.STATE_STABLE_DELAY, null);
                int N = procs.size();
                long totalBackupProcsMem = 0;
                ArrayList<ProcessRecord> backupProcs = new ArrayList<>();
                int i = 0;
                while (true) {
                    c = 3;
                    z = true;
                    if (i >= N) {
                        break;
                    }
                    ProcessRecord app = procs.get(i);
                    if (blackList.contains(app.processName) && app.mProfile.getLastPss() / 1024 > 300) {
                        Slog.w(TAG, "killing process " + app.processName + " pid " + app.getPid() + " size " + (app.mProfile.getLastPss() / 1024));
                        app.killLocked("low mem kill", 3, true);
                    } else if (app.mState.getSetAdj() == 400) {
                        totalBackupProcsMem += app.mProfile.getLastPss();
                        backupProcs.add(app);
                    }
                    i++;
                }
                if (totalBackupProcsMem / 1024 >= BACKUP_MEM_THRESHOLD) {
                    int i2 = 0;
                    while (i2 < backupProcs.size()) {
                        ProcessRecord app2 = backupProcs.get(i2);
                        if (app2.mProfile.getLastPss() / 1024 <= BACKUP_APP_MEM_THRESHOLD) {
                            z2 = z;
                            c2 = c;
                        } else {
                            Slog.w(TAG, "killing process " + app2.processName + " pid " + app2.getPid() + " size " + (app2.mProfile.getLastPss() / 1024) + " reason: backup procs' totalMem is too big, need to kill big mem proc");
                            c2 = 3;
                            z2 = true;
                            app2.killLocked("low mem kill", 3, true);
                        }
                        i2++;
                        c = c2;
                        z = z2;
                    }
                }
            } catch (Throwable th) {
                th = th;
                while (true) {
                    try {
                        break;
                    } catch (Throwable th2) {
                        th = th2;
                    }
                }
                throw th;
            }
        }
        jobFinished(params, false);
        schedule(this);
        return false;
    }

    @Override // android.app.job.JobService
    public boolean onStopJob(JobParameters params) {
        Slog.w(TAG, "ProcessKillerIdler onStopJob");
        return false;
    }

    public static void schedule(Context context) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService("jobscheduler");
        JobInfo.Builder builder = new JobInfo.Builder(PROCESS_KILL_JOB_ID, cm);
        builder.setMinimumLatency(CHECK_FREE_MEM_TIME);
        jobScheduler.schedule(builder.build());
    }
}
