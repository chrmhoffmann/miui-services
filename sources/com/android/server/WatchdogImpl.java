package com.android.server;

import android.os.Debug;
import android.os.FileUtils;
import android.os.Handler;
import android.os.Process;
import android.os.SystemProperties;
import android.util.Slog;
import android.view.SurfaceControl;
import android.view.SurfaceControlStub;
import com.android.internal.os.BackgroundThread;
import com.android.server.Watchdog;
import com.android.server.am.ActivityManagerServiceImpl;
import com.miui.base.MiuiStubRegistry;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import libcore.io.IoUtils;
import miui.mqsas.scout.ScoutUtils;
import miui.mqsas.sdk.MQSEventManagerDelegate;
import miui.mqsas.sdk.event.WatchdogEvent;
/* loaded from: classes.dex */
public class WatchdogImpl extends WatchdogStub {
    private static final long CHECK_LAYER_TIMEOUT = 600000;
    private static final long GB = 1073741824;
    private static final String HEAP_MONITOR_TAG = "HeapUsage Monitor";
    private static final long KB = 1024;
    private static final int MAX_TRACES = 3;
    private static final long MB = 1048576;
    private static final String SYSTEM_SERVER = "system_server";
    private static final String TAG = "Watchdog";
    private static final String WATCHDOG_DIR = "/data/miuilog/stability/scout/watchdog";
    private static final boolean OOM_CRASH_ON_WATCHDOG = SystemProperties.getBoolean("persist.sys.oom_crash_on_watchdog", false);
    private static final int HEAP_MONITOR_THRESHOLD = SystemProperties.getInt("persist.sys.oom_crash_on_watchdog_size", 500);
    private static boolean isHalfOom = false;
    private static final List<String> diableThreadList = Arrays.asList("PackageManager");

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<WatchdogImpl> {

        /* compiled from: WatchdogImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final WatchdogImpl INSTANCE = new WatchdogImpl();
        }

        public WatchdogImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public WatchdogImpl provideNewInstance() {
            return new WatchdogImpl();
        }
    }

    WatchdogImpl() {
        final Handler h = BackgroundThread.getHandler();
        h.postDelayed(new Runnable() { // from class: com.android.server.WatchdogImpl.1
            @Override // java.lang.Runnable
            public void run() {
                if (SurfaceControl.checkSurfaceLayers()) {
                    SurfaceControlStub.getInstance().reportOORException();
                } else {
                    h.postDelayed(this, WatchdogImpl.CHECK_LAYER_TIMEOUT);
                }
            }
        }, CHECK_LAYER_TIMEOUT);
    }

    void onHalfWatchdog(String subject, File trace, List<Watchdog.HandlerChecker> blockedCheckers, String binderTransInfo, String mUuid) {
        if (Debug.isDebuggerConnected()) {
            return;
        }
        if (blockedCheckers != null && blockedCheckers.size() == 1) {
            String name = blockedCheckers.get(0).getName();
            if (diableThreadList.contains(name)) {
                return;
            }
        }
        Slog.w("MIUIScout Watchdog", "Enter HALF_WATCHDOG");
        reportEvent(384, subject, saveWatchdogTrace(true, subject, binderTransInfo, trace), blockedCheckers, binderTransInfo, mUuid);
    }

    void onWatchdog(String subject, File trace, List<Watchdog.HandlerChecker> blockedCheckers, String binderTransInfo, String mUuid) {
        if (Debug.isDebuggerConnected()) {
            return;
        }
        Slog.w("MIUIScout Watchdog", "Enter WATCHDOG");
        int eventType = trace != null ? 2 : 385;
        reportEvent(eventType, subject, saveWatchdogTrace(false, subject, binderTransInfo, trace), blockedCheckers, binderTransInfo, mUuid);
    }

    public void setWatchdogPropTimestamp(long anrtime) {
        try {
            SystemProperties.set("sys.service.watchdog.timestamp", Long.toString(anrtime));
        } catch (IllegalArgumentException e) {
            Slog.e(TAG, "Failed to set watchdog.timestamp property", e);
        }
    }

    public void checkOOMState() {
        if (!OOM_CRASH_ON_WATCHDOG) {
            return;
        }
        try {
            Runtime runtime = Runtime.getRuntime();
            long dalvikMax = runtime.totalMemory();
            long dalvikFree = runtime.freeMemory();
            long allocHeapSpace = dalvikMax - dalvikFree;
            StringBuilder append = new StringBuilder().append("HeapAllocSize : ").append(prettySize(allocHeapSpace)).append("; monitorSize : ");
            int i = HEAP_MONITOR_THRESHOLD;
            Slog.d(HEAP_MONITOR_TAG, append.append(i).append("MB").toString());
            if (allocHeapSpace >= i * MB) {
                runtime.gc();
                long dalvikMax2 = runtime.totalMemory();
                long dalvikFree2 = runtime.freeMemory();
                long allocHeapSpace2 = dalvikMax2 - dalvikFree2;
                if (allocHeapSpace2 < i * MB) {
                    Slog.d(HEAP_MONITOR_TAG, "After performing Gc, HeapAllocSize : " + prettySize(allocHeapSpace2));
                    isHalfOom = false;
                    return;
                } else if (!isHalfOom) {
                    Slog.d(HEAP_MONITOR_TAG, "Half Oom, Heap of System_Server has allocated " + prettySize(allocHeapSpace2));
                    isHalfOom = true;
                    return;
                } else {
                    String msg = "Heap of System_Server has allocated " + prettySize(allocHeapSpace2) + " , So trigger OutOfMemoryError crash";
                    throw new OutOfMemoryError(msg);
                }
            }
            isHalfOom = false;
        } catch (Exception e) {
            Slog.e(HEAP_MONITOR_TAG, "checkOOMState:" + e.toString());
        }
    }

    private static File saveWatchdogTrace(boolean halfWatchdog, String subject, String binderTransInfo, File file) {
        File tracesDir = getWatchdogDir();
        if (tracesDir == null) {
            Slog.w(TAG, "Failed to get watchdog dir");
            return file;
        }
        final String prefix = halfWatchdog ? "pre_watchdog_pid_" : "watchdog_pid_";
        final TreeSet<File> existingTraces = new TreeSet<>();
        tracesDir.listFiles(new FileFilter() { // from class: com.android.server.WatchdogImpl$$ExternalSyntheticLambda0
            @Override // java.io.FileFilter
            public final boolean accept(File file2) {
                return WatchdogImpl.lambda$saveWatchdogTrace$0(prefix, existingTraces, file2);
            }
        });
        if (existingTraces.size() >= 3) {
            for (int i = 0; i < 2; i++) {
                existingTraces.pollLast();
            }
            Iterator<File> it = existingTraces.iterator();
            while (it.hasNext()) {
                File trace = it.next();
                trace.delete();
            }
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
        String fileName = prefix + Process.myPid() + "_" + dateFormat.format(new Date());
        File watchdogFile = new File(tracesDir, fileName);
        FileOutputStream fout = null;
        FileInputStream fin = null;
        try {
            if (file != null) {
                try {
                    if (file.length() > 0) {
                        fin = new FileInputStream(file);
                    }
                } catch (Exception e) {
                    Slog.w(TAG, "Failed to save watchdog trace: " + e.getMessage());
                }
            }
            fout = new FileOutputStream(watchdogFile);
            if (fin == null) {
                fout.write("Subject".getBytes(StandardCharsets.UTF_8));
                fout.write(10);
                fout.write(10);
            }
            if (binderTransInfo != null) {
                fout.write(binderTransInfo.getBytes(StandardCharsets.UTF_8));
                fout.write(10);
                fout.write(10);
            }
            if (fin != null) {
                FileUtils.copyInternalUserspace(fin, fout, null, null, null);
            }
            FileUtils.setPermissions(watchdogFile, 420, -1, -1);
            return watchdogFile;
        } finally {
            IoUtils.closeQuietly(fin);
            IoUtils.closeQuietly(fout);
        }
    }

    public static /* synthetic */ boolean lambda$saveWatchdogTrace$0(String prefix, TreeSet existingTraces, File pathname) {
        if (pathname.getName().startsWith(prefix)) {
            existingTraces.add(pathname);
            return false;
        }
        return false;
    }

    private static File getWatchdogDir() {
        File tracesDir = new File(WATCHDOG_DIR);
        if (!tracesDir.exists() && !tracesDir.mkdirs()) {
            return null;
        }
        FileUtils.setPermissions(tracesDir, 493, -1, -1);
        return tracesDir;
    }

    public static String prettySize(long byte_count) {
        long[] kUnitThresholds = {0, 10240, 10485760, 10737418240L};
        long[] kBytesPerUnit = {1, KB, MB, GB};
        String[] kUnitStrings = {"B", "KB", "MB", "GB"};
        String negative_str = "";
        if (byte_count < 0) {
            negative_str = "-";
            byte_count = -byte_count;
        }
        int i = kUnitThresholds.length;
        do {
            i--;
            if (i <= 0) {
                break;
            }
        } while (byte_count < kUnitThresholds[i]);
        return negative_str + (byte_count / kBytesPerUnit[i]) + kUnitStrings[i];
    }

    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:24:0x00a7 -> B:35:0x00bc). Please submit an issue!!! */
    private static void reportEvent(int type, String subject, File trace, List<Watchdog.HandlerChecker> handlerCheckers, String mBinderInfo, String mUuid) {
        final WatchdogEvent event = new WatchdogEvent();
        event.setType(type);
        event.setPid(Process.myPid());
        event.setProcessName(SYSTEM_SERVER);
        event.setPackageName(SYSTEM_SERVER);
        event.setTimeStamp(System.currentTimeMillis());
        event.setSystem(true);
        event.setSummary(subject);
        event.setDetails(subject);
        event.setBinderTransactionInfo(mBinderInfo);
        event.setUuid(mUuid);
        if (trace != null) {
            event.setLogName(trace.getAbsolutePath());
        }
        if (handlerCheckers != null) {
            StringBuilder details = new StringBuilder();
            for (int i = 0; i < handlerCheckers.size(); i++) {
                StackTraceElement[] st = handlerCheckers.get(i).getThread().getStackTrace();
                for (StackTraceElement element : st) {
                    details.append("    at ").append(element).append("\n");
                }
                details.append("\n\n");
            }
            event.setDetails(details.toString());
        }
        if (!ScoutUtils.isLibraryTest()) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            FutureTask<Boolean> futureTask = new FutureTask<>(new Callable<Boolean>() { // from class: com.android.server.WatchdogImpl.2
                @Override // java.util.concurrent.Callable
                public Boolean call() throws Exception {
                    MQSEventManagerDelegate.getInstance().reportWatchdogEvent(event);
                    return true;
                }
            });
            executor.execute(futureTask);
            try {
                try {
                    try {
                        futureTask.get(ActivityManagerServiceImpl.KEEP_FOREGROUND_DURATION, TimeUnit.MILLISECONDS);
                        executor.shutdown();
                    } catch (Exception exception) {
                        exception.printStackTrace();
                        executor.shutdown();
                    }
                } catch (Exception exception2) {
                    exception2.printStackTrace();
                }
                return;
            } catch (Throwable th) {
                try {
                    executor.shutdown();
                } catch (Exception exception3) {
                    exception3.printStackTrace();
                }
                throw th;
            }
        }
        MQSEventManagerDelegate.getInstance().reportWatchdogEvent(event);
    }
}
