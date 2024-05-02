package com.android.server.am;

import android.app.ActivityManagerNative;
import android.app.ApplicationErrorReport;
import android.os.DropBoxManager;
import android.os.FileUtils;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.os.ProcessCpuTracker;
import com.android.internal.util.FastPrintWriter;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
/* loaded from: classes.dex */
public class ANRManager {
    private static final String ANR_TYPE_BROADCAST = "broadcast";
    private static final String ANR_TYPE_INPUT = "input";
    private static final String ANR_TYPE_PROVIDER = "provider";
    private static final String ANR_TYPE_SERVICE = "service";
    static final int BROADCAST_TIMEOUT_HALF_MSG = 1012;
    static final int DEFAULT_DROPBOX_MAX_SIZE = 196608;
    public static final int DEFAULT_MAX_FILES;
    public static final int DEFAULT_QUOTA_KB;
    static final int DROPBOX_MAX_SIZE;
    static final boolean ENABLE_HALF_ANR_STACK;
    static final int INPUT_TIMEOUT_HALF_MSG = 1014;
    static final int PROVIDER_TIMEOUT_HALF_MSG = 1013;
    private static final String REASON_BROADCAST_ANR = "Broadcast of Intent";
    private static final String REASON_INPUT_ANR = "Input dispatching timed out";
    private static final String REASON_SERVICE_ANR = "executing service";
    static final int SERVICE_TIMEOUT_HALF_MSG = 1011;
    private static final String TAG = "ANRManager";
    private static BroadcastDispatcherFeatureImpl broadcastDispatcherFeature;
    private static ActivityManagerService sActivityManagerService;
    private static SimpleDateFormat sAnrFileDateFormat;
    private static volatile WorkHandler sHandler;

    static {
        boolean z = SystemProperties.getBoolean("persist.enable_anr_half", false);
        ENABLE_HALF_ANR_STACK = z;
        DEFAULT_QUOTA_KB = z ? 51200 : 5120;
        DEFAULT_MAX_FILES = z ? 5000 : 1000;
        DROPBOX_MAX_SIZE = z ? 393216 : DEFAULT_DROPBOX_MAX_SIZE;
        sAnrFileDateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS");
        broadcastDispatcherFeature = new BroadcastDispatcherFeatureImpl();
    }

    /* loaded from: classes.dex */
    public static class WorkHandler extends Handler {
        public WorkHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case ANRManager.SERVICE_TIMEOUT_HALF_MSG /* 1011 */:
                    ANRManager.onServiceTimeoutHalf((ProcessRecord) msg.obj);
                    return;
                case ANRManager.BROADCAST_TIMEOUT_HALF_MSG /* 1012 */:
                    ANRManager.onBroadcastTimeoutHalf((BroadcastQueue) msg.obj);
                    return;
                case ANRManager.PROVIDER_TIMEOUT_HALF_MSG /* 1013 */:
                    ANRManager.onProviderTimeoutHalf((ProcessRecord) msg.obj);
                    return;
                case ANRManager.INPUT_TIMEOUT_HALF_MSG /* 1014 */:
                    ANRManager.onInputTimeoutHalf(msg.arg1);
                    return;
                default:
                    return;
            }
        }
    }

    static void checkInit(ActivityManagerService ams) {
        if (sHandler == null) {
            synchronized (ANRManager.class) {
                if (sHandler == null) {
                    sHandler = new WorkHandler(ams.mHandler.getLooper());
                    sActivityManagerService = ams;
                }
            }
        }
    }

    static void scheduleServiceTimeoutHalf(ActivityManagerService ams, long endTime, ProcessRecord app) {
        if (!ENABLE_HALF_ANR_STACK) {
            return;
        }
        checkInit(ams);
        Message msg = sHandler.obtainMessage(SERVICE_TIMEOUT_HALF_MSG);
        msg.obj = app;
        sHandler.sendMessageAtTime(msg, endTime);
    }

    static void cancelScheduleServiceTimeoutHalf(ActivityManagerService ams, ProcessRecord app) {
        if (!ENABLE_HALF_ANR_STACK) {
            return;
        }
        checkInit(ams);
        sHandler.removeMessages(SERVICE_TIMEOUT_HALF_MSG, app);
    }

    static void scheduleBroadcastTimeoutHalf(ActivityManagerService ams, long endTime, BroadcastQueue bq) {
        if (!ENABLE_HALF_ANR_STACK) {
            return;
        }
        checkInit(ams);
        Message msg = sHandler.obtainMessage(BROADCAST_TIMEOUT_HALF_MSG);
        msg.obj = bq;
        sHandler.sendMessageAtTime(msg, endTime);
    }

    static void cancelScheduleBroadcastTimeoutHalf(ActivityManagerService ams, BroadcastQueue bq) {
        if (!ENABLE_HALF_ANR_STACK) {
            return;
        }
        checkInit(ams);
        sHandler.removeMessages(BROADCAST_TIMEOUT_HALF_MSG, bq);
    }

    static void scheduleProviderTimeoutHalf(ActivityManagerService ams, long delay, ProcessRecord app) {
        if (!ENABLE_HALF_ANR_STACK) {
            return;
        }
        checkInit(ams);
        Message msg = sHandler.obtainMessage(PROVIDER_TIMEOUT_HALF_MSG);
        msg.obj = app;
        sHandler.sendMessageDelayed(msg, delay);
    }

    static void cancelScheduleProviderTimeoutHalf(ActivityManagerService ams, ProcessRecord app) {
        if (!ENABLE_HALF_ANR_STACK) {
            return;
        }
        checkInit(ams);
        sHandler.removeMessages(PROVIDER_TIMEOUT_HALF_MSG, app);
    }

    public static void scheduleInputTimeoutHalf(int pid) {
        if (!ENABLE_HALF_ANR_STACK) {
            return;
        }
        ActivityManagerService ams = ActivityManagerNative.getDefault();
        checkInit(ams);
        Message msg = sHandler.obtainMessage(INPUT_TIMEOUT_HALF_MSG);
        msg.arg1 = pid;
        sHandler.sendMessage(msg);
    }

    static void onServiceTimeoutHalf(ProcessRecord app) {
        dumpStackTracesLite(app, ANR_TYPE_SERVICE);
    }

    static void onBroadcastTimeoutHalf(BroadcastQueue bq) {
        ProcessRecord app = null;
        synchronized (sActivityManagerService) {
            BroadcastRecord r = broadcastDispatcherFeature.getOrderedBroadcasts(bq.mDispatcher).get(0);
            Object curReceiver = r.receivers.get(r.nextReceiver - 1);
            if (curReceiver instanceof BroadcastFilter) {
                BroadcastFilter bf = (BroadcastFilter) curReceiver;
                if (bf.receiverList.pid != 0 && bf.receiverList.pid != ActivityManagerService.MY_PID) {
                    synchronized (sActivityManagerService.mPidsSelfLocked) {
                        app = sActivityManagerService.mPidsSelfLocked.get(bf.receiverList.pid);
                    }
                }
            } else {
                app = r.curApp;
            }
            if (app != null && app.getPid() != 0) {
                dumpStackTracesLite(app, ANR_TYPE_BROADCAST);
                return;
            }
            Slog.e(TAG, "app not exist while broadcast timeout half");
        }
    }

    static void onProviderTimeoutHalf(ProcessRecord app) {
        dumpStackTracesLite(app, ANR_TYPE_PROVIDER);
    }

    static void onInputTimeoutHalf(int pid) {
        dumpStackTracesLite(pid, ANR_TYPE_INPUT);
    }

    public static void onANR(ActivityManagerService ams, ProcessRecord process, String subject, String report, File logFile, ApplicationErrorReport.CrashInfo crashInfo, String headline) {
        if (!ENABLE_HALF_ANR_STACK || TextUtils.isEmpty(report)) {
            return;
        }
        String type = "";
        if (report.startsWith(REASON_INPUT_ANR)) {
            type = ANR_TYPE_INPUT;
        } else if (report.startsWith(REASON_SERVICE_ANR)) {
            type = ANR_TYPE_SERVICE;
        } else if (report.startsWith(REASON_BROADCAST_ANR)) {
            type = ANR_TYPE_BROADCAST;
        }
        renameTraceFile(process.info.packageName, type, false);
    }

    static File dumpStackTracesLite(ProcessRecord app, String type) {
        if (app == null || app.getPid() == 0) {
            return null;
        }
        return dumpStackTracesLite(app.getPid(), type);
    }

    static File dumpStackTracesLite(int pid, String type) {
        String packageName = ProcessUtils.getPackageNameByPid(pid);
        ArrayList<Integer> pids = new ArrayList<>(2);
        pids.add(Integer.valueOf(pid));
        if (Process.myPid() != pid) {
            pids.add(Integer.valueOf(Process.myPid()));
        }
        Log.d(TAG, "start dumpStackTracesLite, pids:" + pids);
        ActivityManagerService.dumpStackTraces(pids, (ProcessCpuTracker) null, (SparseArray) null, (ArrayList) null, (StringWriter) null);
        return renameTraceFile(packageName, type, true);
    }

    static File renameTraceFile(String packageName, String type, boolean half) {
        String newTracesPath;
        String formattedDate = sAnrFileDateFormat.format(new Date());
        String tracesPath = SystemProperties.get("dalvik.vm.stack-trace-file", (String) null);
        String tail = "_" + packageName + "_" + type + "@" + formattedDate + (half ? ".half.txt" : ".txt");
        if (tracesPath == null || tracesPath.length() == 0) {
            return null;
        }
        File traceRenameFile = new File(tracesPath);
        int lpos = tracesPath.lastIndexOf(".");
        if (-1 != lpos) {
            newTracesPath = tracesPath.substring(0, lpos) + tail;
        } else {
            newTracesPath = tracesPath + tail;
        }
        File newFile = new File(newTracesPath);
        traceRenameFile.renameTo(newFile);
        return newFile;
    }

    static void addTextToDropBox(final File dataFile, String packageName, String eventType) {
        final String dropboxTag = packageName + "_half_anr_" + eventType;
        final DropBoxManager dbox = (DropBoxManager) sActivityManagerService.mContext.getSystemService("dropbox");
        if (dbox == null || !dbox.isTagEnabled(dropboxTag)) {
            return;
        }
        final StringBuilder sb = new StringBuilder(1024);
        Log.d(TAG, "add stack to dropbox, tag:" + dropboxTag + ", type: " + eventType);
        Thread worker = new Thread("Half anr time dump: " + dropboxTag) { // from class: com.android.server.am.ANRManager.1
            @Override // java.lang.Thread, java.lang.Runnable
            public void run() {
                int maxDataFileSize = ANRManager.DROPBOX_MAX_SIZE - sb.length();
                File file = dataFile;
                if (file != null && maxDataFileSize > 0) {
                    try {
                        sb.append(FileUtils.readTextFile(file, maxDataFileSize, "\n\n[[TRUNCATED]]"));
                    } catch (IOException e) {
                        Slog.e(ANRManager.TAG, "Error reading " + dataFile, e);
                    }
                }
                dbox.addText(dropboxTag, sb.toString());
            }
        };
        worker.start();
    }

    static String getDropBoxTag(ProcessRecord process, String processName, String eventType) {
        if (ProcessPolicy.REASON_ANR.equals(eventType)) {
            return process.info.packageName + "_" + eventType;
        }
        return processClass(process) + "_" + eventType;
    }

    static String getLastAnrState(ActivityManagerService ams) {
        if (ams == null) {
            return "";
        }
        StringWriter sw = new StringWriter();
        FastPrintWriter pw = new FastPrintWriter(sw);
        pw.println("\n");
        ams.dump((FileDescriptor) null, pw, new String[]{"lastanr"});
        pw.println("\n");
        ams.mWindowManager.dump((FileDescriptor) null, pw, new String[]{"lastanr"});
        pw.flush();
        return sw.toString();
    }

    public static void saveLastAnrState(ActivityManagerService ams, File file) {
        if (ams == null || !file.exists()) {
            return;
        }
        try {
            FileOutputStream fout = new FileOutputStream(file, true);
            FastPrintWriter pw = new FastPrintWriter(fout);
            pw.println("\n");
            ams.dump((FileDescriptor) null, pw, new String[]{"lastanr"});
            pw.println("\n");
            ams.mWindowManager.dump((FileDescriptor) null, pw, new String[]{"lastanr"});
            pw.flush();
            fout.close();
        } catch (Exception e) {
            Slog.e(TAG, "saveLastAnrState error ", e);
        }
    }

    private static String processClass(ProcessRecord process) {
        if (process == null || process.getPid() == Process.myPid()) {
            return "system_server";
        }
        if ((process.info.flags & 1) != 0) {
            return "system_app";
        }
        return "data_app";
    }
}
