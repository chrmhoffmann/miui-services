package com.android.server.job;

import android.content.Context;
import android.content.Intent;
import com.android.server.LocalServices;
import com.android.server.am.AutoStartManagerService;
import com.android.server.job.controllers.JobStatus;
import com.miui.base.MiuiStubRegistry;
import miui.os.Build;
/* loaded from: classes.dex */
public class JobServiceContextImpl extends JobServiceContextStub {

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<JobServiceContextImpl> {

        /* compiled from: JobServiceContextImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final JobServiceContextImpl INSTANCE = new JobServiceContextImpl();
        }

        public JobServiceContextImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public JobServiceContextImpl provideNewInstance() {
            return new JobServiceContextImpl();
        }
    }

    public boolean checkIfCancelJob(JobServiceContext jobContext, Context context, Intent service, int bindFlags, JobStatus job) {
        if (Build.IS_INTERNATIONAL_BUILD || job == null || AutoStartManagerService.getInstance().isAllowStartService(context, service, job.getUserId(), job.getUid())) {
            return false;
        }
        int jobId = job.getJobId();
        int uid = job.getUid();
        JobSchedulerInternal internal = (JobSchedulerInternal) LocalServices.getService(JobSchedulerInternal.class);
        if (internal != null) {
            internal.cancelJob(uid, jobId);
            return true;
        }
        return true;
    }
}
