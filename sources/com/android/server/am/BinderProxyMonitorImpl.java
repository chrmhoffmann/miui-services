package com.android.server.am;

import android.app.IApplicationThread;
import android.os.Binder;
import android.os.BinderStub;
import android.os.FileUtils;
import android.util.Slog;
import com.android.internal.os.TransferPipe;
import com.android.internal.util.FastPrintWriter;
import com.miui.base.MiuiStubRegistry;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.function.Consumer;
import libcore.io.IoUtils;
/* loaded from: classes.dex */
public class BinderProxyMonitorImpl implements BinderProxyMonitor {
    private static final String BINDER_ALLOC_PREFIX = "binder_alloc_u";
    private static final String DUMP_DIR = "/data/miuilog/stability/resleak/binderproxy";
    private static final String TAG = "BinderProxyMonitor";
    private volatile int mTrackedUid = -1;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<BinderProxyMonitorImpl> {

        /* compiled from: BinderProxyMonitorImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final BinderProxyMonitorImpl INSTANCE = new BinderProxyMonitorImpl();
        }

        public BinderProxyMonitorImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public BinderProxyMonitorImpl provideNewInstance() {
            return new BinderProxyMonitorImpl();
        }
    }

    public void trackBinderAllocations(final ActivityManagerService ams, final int targetUid) {
        if (!BinderStub.get().isEnabled()) {
            return;
        }
        Slog.i(TAG, "Enable binder tracker on uid " + targetUid);
        ams.mHandler.post(new Runnable() { // from class: com.android.server.am.BinderProxyMonitorImpl$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                BinderProxyMonitorImpl.this.m339x481b8eb3(targetUid, ams);
            }
        });
    }

    /* renamed from: lambda$trackBinderAllocations$1$com-android-server-am-BinderProxyMonitorImpl */
    public /* synthetic */ void m339x481b8eb3(final int targetUid, ActivityManagerService ams) {
        if (targetUid == this.mTrackedUid) {
            return;
        }
        final int prevTrackedUid = this.mTrackedUid;
        this.mTrackedUid = targetUid;
        synchronized (ams.mProcLock) {
            ams.mProcessList.forEachLruProcessesLOSP(true, new Consumer() { // from class: com.android.server.am.BinderProxyMonitorImpl$$ExternalSyntheticLambda0
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    BinderProxyMonitorImpl.lambda$trackBinderAllocations$0(prevTrackedUid, targetUid, (ProcessRecord) obj);
                }
            });
        }
    }

    public static /* synthetic */ void lambda$trackBinderAllocations$0(int prevTrackedUid, int targetUid, ProcessRecord proc) {
        try {
            IApplicationThread thread = proc.getThread();
            if (thread == null) {
                return;
            }
            if (prevTrackedUid != -1 && proc.uid == prevTrackedUid) {
                Slog.i(TAG, "Disable binder tracker on process " + proc.getPid());
                proc.getThread().trackBinderAllocations(false);
            } else if (targetUid != -1) {
                try {
                    if (proc.uid == targetUid) {
                        Slog.i(TAG, "Enable binder tracker on process " + proc.getPid());
                        proc.getThread().trackBinderAllocations(true);
                    }
                } catch (Exception e) {
                    Slog.w(TAG, "Failed to enable binder allocation tracking in " + proc + ".  Exception: " + e);
                }
            }
        } catch (Exception e2) {
            Slog.w(TAG, "Failed to enable binder allocation tracking in " + proc + ".  Exception: " + e2);
        }
    }

    public void trackProcBinderAllocations(final ActivityManagerService ams, final ProcessRecord targetProc) {
        int currentTrackedUid;
        if (!BinderStub.get().isEnabled() || (currentTrackedUid = this.mTrackedUid) < 0 || targetProc.uid != currentTrackedUid) {
            return;
        }
        Slog.i(TAG, "Enable binder tracker on new process " + targetProc.getPid());
        ams.mHandler.post(new Runnable() { // from class: com.android.server.am.BinderProxyMonitorImpl$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() {
                BinderProxyMonitorImpl.this.m341x6da54a67(ams, targetProc);
            }
        });
    }

    /* renamed from: lambda$trackProcBinderAllocations$3$com-android-server-am-BinderProxyMonitorImpl */
    public /* synthetic */ void m341x6da54a67(ActivityManagerService ams, final ProcessRecord targetProc) {
        synchronized (ams.mProcLock) {
            ams.mProcessList.forEachLruProcessesLOSP(true, new Consumer() { // from class: com.android.server.am.BinderProxyMonitorImpl$$ExternalSyntheticLambda1
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    BinderProxyMonitorImpl.this.m340xb42dbcc8(targetProc, (ProcessRecord) obj);
                }
            });
        }
    }

    /* renamed from: lambda$trackProcBinderAllocations$2$com-android-server-am-BinderProxyMonitorImpl */
    public /* synthetic */ void m340xb42dbcc8(ProcessRecord targetProc, ProcessRecord proc) {
        if (proc != targetProc) {
            return;
        }
        try {
            if (this.mTrackedUid != -1 && proc.uid == this.mTrackedUid) {
                proc.getThread().trackBinderAllocations(true);
            }
        } catch (Exception e) {
            Slog.w(TAG, "Failed to enable binder allocation tracking in " + proc + ".  Exception: " + e);
        }
    }

    public boolean handleDumpBinderProxies(ActivityManagerService ams, FileDescriptor fd, PrintWriter pw, String[] args, int opti) {
        if (!BinderStub.get().isEnabled()) {
            return false;
        }
        boolean dumpDetailToFile = false;
        boolean dumpDetail = false;
        for (int i = opti; i < args.length; i++) {
            if ("--dump-details-to-file".equals(args[opti])) {
                dumpDetailToFile = true;
            } else if ("--dump-details".equals(args[opti])) {
                dumpDetail = true;
            } else if ("--track-uid".equals(args[opti])) {
                if (opti + 1 >= args.length) {
                    throw new IllegalArgumentException("--trackUid should be followed by a uid");
                }
                int uid = Integer.parseInt(args[opti + 1]);
                trackBinderAllocations(ams, uid);
                return true;
            }
        }
        if (!dumpDetail && !dumpDetailToFile) {
            return false;
        }
        dumpBinderProxies(ams, fd, pw, dumpDetailToFile);
        return true;
    }

    private void dumpBinderProxies(ActivityManagerService ams, FileDescriptor fd, PrintWriter pw, boolean persistToFile) {
        int uid = this.mTrackedUid;
        if (uid < 0 || !BinderStub.get().isEnabled()) {
            pw.println("Binder allocation tracker not enabled yet");
            return;
        }
        FileOutputStream fos = null;
        if (persistToFile && ensureDumpDir(DUMP_DIR)) {
            File persistFile = new File(DUMP_DIR, BINDER_ALLOC_PREFIX + uid);
            pw.println("Dumping binder proxies to " + persistFile);
            try {
                fos = new FileOutputStream(persistFile);
                fd = fos.getFD();
                pw = new FastPrintWriter(fos);
            } catch (Exception e) {
                pw.println("Failed to open " + persistFile + ": " + e.getMessage());
            }
        }
        long iden = Binder.clearCallingIdentity();
        try {
            try {
                ams.dumpBinderProxies(pw, 0);
                pw.write("\n\n");
                pw.flush();
                doDumpRemoteBinders(ams, uid, fd, pw);
                Binder.restoreCallingIdentity(iden);
                if (fos == null) {
                    return;
                }
            } catch (Exception e2) {
                pw.println("Dump binder allocations failed" + e2);
                Binder.restoreCallingIdentity(iden);
                if (fos == null) {
                    return;
                }
            }
            IoUtils.closeQuietly(fos);
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(iden);
            if (fos != null) {
                IoUtils.closeQuietly(fos);
            }
            throw th;
        }
    }

    public static boolean ensureDumpDir(String path) {
        File dir = new File(path);
        if (!dir.exists() && !dir.mkdirs()) {
            Slog.w(TAG, "Failed to mkdir " + path);
            return false;
        }
        int errno = FileUtils.setPermissions(dir, 493, -1, -1);
        if (errno != 0) {
            Slog.w(TAG, "Failed to update dir perms: path = " + path + ", errno=" + errno);
            return false;
        }
        return true;
    }

    private void doDumpRemoteBinders(ActivityManagerService ams, final int targetUid, final FileDescriptor fd, final PrintWriter pw) {
        if (fd == null) {
            return;
        }
        synchronized (ams.mProcLock) {
            ams.mProcessList.forEachLruProcessesLOSP(true, new Consumer() { // from class: com.android.server.am.BinderProxyMonitorImpl$$ExternalSyntheticLambda3
                @Override // java.util.function.Consumer
                public final void accept(Object obj) {
                    BinderProxyMonitorImpl.lambda$doDumpRemoteBinders$4(targetUid, fd, pw, (ProcessRecord) obj);
                }
            });
        }
    }

    public static /* synthetic */ void lambda$doDumpRemoteBinders$4(int targetUid, FileDescriptor fd, PrintWriter pw, ProcessRecord proc) {
        try {
            IApplicationThread thread = proc.getThread();
            if (thread == null || proc.uid != targetUid) {
                return;
            }
            TransferPipe tp = new TransferPipe();
            proc.getThread().dumpBinderAllocations(tp.getWriteFd());
            tp.go(fd, 10000L);
            tp.kill();
        } catch (Exception e) {
            if (pw != null) {
                pw.println("Failure while dumping binder traces from " + proc + ".  Exception: " + e);
                pw.flush();
            }
        }
    }
}
