package com.android.server;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.os.BatteryManager;
import android.os.PowerManager;
import android.util.Slog;
import com.miui.base.MiuiStubRegistry;
/* loaded from: classes.dex */
public class MountServiceIdlerImpl implements MountServiceIdlerStub {
    private static final long FINISH_INTERVAL_TIME = 7200000;
    private static final int MINIMUM_BATTERY_LEVEL = 10;
    private static final long MINIMUM_INTERVAL_TIME = 1800000;
    private static final String TAG = "MountServiceIdlerImpl";
    private long sNextTrimDuration = FINISH_INTERVAL_TIME;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MountServiceIdlerImpl> {

        /* compiled from: MountServiceIdlerImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MountServiceIdlerImpl INSTANCE = new MountServiceIdlerImpl();
        }

        public MountServiceIdlerImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public MountServiceIdlerImpl provideNewInstance() {
            return new MountServiceIdlerImpl();
        }
    }

    MountServiceIdlerImpl() {
    }

    public boolean runIdleMaint(Context context, int jobId, ComponentName componentName) {
        BatteryManager bm = (BatteryManager) context.getSystemService("batterymanager");
        int batteryLevel = bm.getIntProperty(4);
        PowerManager pm = (PowerManager) context.getSystemService("power");
        boolean isInteractive = pm.isInteractive();
        if (!isInteractive && batteryLevel >= 10) {
            this.sNextTrimDuration = FINISH_INTERVAL_TIME;
            return false;
        }
        this.sNextTrimDuration >>= 1;
        internalScheduleIdlePass(context, jobId, componentName);
        return true;
    }

    public boolean internalScheduleIdlePass(Context context, int jobId, ComponentName componentName) {
        JobScheduler tm = (JobScheduler) context.getSystemService("jobscheduler");
        if (this.sNextTrimDuration < 1800000) {
            this.sNextTrimDuration = 1800000L;
        }
        Slog.i(TAG, "sNextTrimDuration :  " + this.sNextTrimDuration);
        JobInfo.Builder builder = new JobInfo.Builder(jobId, componentName);
        builder.setMinimumLatency(this.sNextTrimDuration);
        tm.schedule(builder.build());
        return true;
    }
}
