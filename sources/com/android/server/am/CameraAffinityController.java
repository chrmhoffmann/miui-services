package com.android.server.am;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Slog;
import com.android.server.ServiceThread;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class CameraAffinityController {
    public static String ADD_REBIND_TASK_LIT = null;
    public static String CLUSTER_ROOT = null;
    public static String DEL_REBIND_TASK_LIT = null;
    private static final int RESET_PROCS_AFFINITY = 1;
    private static String RESTRICT_PIDS = null;
    private static final String RESTRICT_PROCS = "ueventd";
    private static final String TAG = "CameraAffinityController";
    public static String UNTRUSTEDAPP_BG_CPUSHARES_PATH;
    public static String UNTRUSTEDAPP_BG_CPUS_PATH;
    public static String sDefaultAffinity;
    public static int sDefaultCpuShares;
    private CameraAffinityHandler mHandler;
    private long mStartTime;
    private static final long RESTRICT_PROCS_TIME = SystemProperties.getInt("persist.sys.miui.camera.restrict_procs.time", 0);
    private static final String UNTRUSTEDAPP_BG_CPUS = SystemProperties.get("persist.sys.miui.camera.untrustedapp.cpus", "");

    static {
        String defaultCpuShares;
        sDefaultAffinity = null;
        sDefaultCpuShares = 0;
        UNTRUSTEDAPP_BG_CPUS_PATH = null;
        UNTRUSTEDAPP_BG_CPUSHARES_PATH = null;
        String root = getRoot("/sys/module/turbo_sched/parameters", "/add_rebind_task_lit");
        CLUSTER_ROOT = root;
        if (root != null) {
            ADD_REBIND_TASK_LIT = CLUSTER_ROOT + "/add_rebind_task_lit";
            DEL_REBIND_TASK_LIT = CLUSTER_ROOT + "/del_rebind_task_lit";
        }
        String pids = getPid(RESTRICT_PROCS);
        if (pids != null) {
            RESTRICT_PIDS = pids.replace(" ", ":");
        }
        String pathFile = getPathFile("/dev/cpuset/camera-background/limit/cpus", true);
        UNTRUSTEDAPP_BG_CPUS_PATH = pathFile;
        if (pathFile != null) {
            sDefaultAffinity = getInfoFromPath(pathFile);
        }
        String pathFile2 = getPathFile("/dev/cpuctl/background/untrustedapp/cpu.shares", true);
        UNTRUSTEDAPP_BG_CPUSHARES_PATH = pathFile2;
        if (pathFile2 != null && (defaultCpuShares = getInfoFromPath(pathFile2)) != null) {
            sDefaultCpuShares = Integer.parseInt(defaultCpuShares);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class CameraAffinityHandler extends Handler {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public CameraAffinityHandler(Looper looper) {
            super(looper, null, true);
            CameraAffinityController.this = this$0;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    CameraAffinityController.this.setSchedAffinity(CameraAffinityController.RESTRICT_PIDS, ((Boolean) msg.obj).booleanValue());
                    return;
                default:
                    return;
            }
        }
    }

    public CameraAffinityController(ServiceThread thread) {
        this.mHandler = new CameraAffinityHandler(thread.getLooper());
    }

    private static String getPid(String processName) {
        if (processName == null) {
            return null;
        }
        Process p = null;
        BufferedReader input = null;
        try {
            p = Runtime.getRuntime().exec("pidof " + processName);
            input = new BufferedReader(new InputStreamReader(p.getInputStream()), 1024);
            String pid = input.readLine();
            input.close();
            try {
                input.close();
            } catch (IOException e) {
            }
            if (p != null) {
                p.destroy();
            }
            return pid;
        } catch (IOException e2) {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e3) {
                }
            }
            if (p != null) {
                p.destroy();
            }
            return null;
        } catch (Throwable th) {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e4) {
                }
            }
            if (p != null) {
                p.destroy();
            }
            throw th;
        }
    }

    private static String getRoot(String path, String node) {
        File file = new File(path);
        if (file.exists() && file.canRead()) {
            File file2 = new File(path + node);
            if (file2.exists() && file2.canWrite()) {
                return path;
            }
        }
        Slog.w(TAG, "can not find root " + path);
        return null;
    }

    private static String getPathFile(String path, boolean write) {
        File file = new File(path);
        if (file.exists() && file.canRead()) {
            if (write && !file.canWrite()) {
                Slog.w(TAG, "File can not write " + path);
                return null;
            }
            return path;
        }
        Slog.w(TAG, "can not find files " + path);
        return null;
    }

    public void notifySetProcsAffinity(boolean isCameraForeground) {
        long j = RESTRICT_PROCS_TIME;
        if (j > 0 && RESTRICT_PIDS != null) {
            Message msg = this.mHandler.obtainMessage(1, false);
            long now = SystemClock.uptimeMillis();
            if (isCameraForeground) {
                setSchedAffinity(RESTRICT_PIDS, true);
                this.mStartTime = SystemClock.uptimeMillis();
                this.mHandler.sendMessageDelayed(msg, j);
            } else if (now - this.mStartTime < j) {
                this.mHandler.removeMessages(1, false);
                setSchedAffinity(RESTRICT_PIDS, false);
            }
        }
        SetUntrustedAppCPUParams(isCameraForeground);
    }

    public boolean setSchedAffinity(String pid, boolean restrict) {
        String file;
        if (restrict) {
            file = ADD_REBIND_TASK_LIT;
        } else {
            file = DEL_REBIND_TASK_LIT;
        }
        return writeToFile(file, pid);
    }

    public static boolean writeToFile(String fileName, String str) {
        StringBuilder sb;
        FileOutputStream fos = null;
        if (TextUtils.isEmpty(fileName)) {
            return false;
        }
        try {
            if (TextUtils.isEmpty(str)) {
                return false;
            }
            try {
                File file = new File(fileName);
                fos = new FileOutputStream(file);
                byte[] buffer = str.getBytes("UTF-8");
                fos.write(buffer);
                try {
                    fos.close();
                    return true;
                } catch (IOException e) {
                    e = e;
                    sb = new StringBuilder();
                    Slog.e(TAG, sb.append("writeToFile, finally fos.close() IOException e:").append(e.getMessage()).toString());
                    return false;
                }
            } catch (IOException e2) {
                Slog.w(TAG, "writeToFile, IOException e:" + e2.getMessage());
                if (fos == null) {
                    return false;
                }
                try {
                    fos.close();
                    return false;
                } catch (IOException e3) {
                    e = e3;
                    sb = new StringBuilder();
                    Slog.e(TAG, sb.append("writeToFile, finally fos.close() IOException e:").append(e.getMessage()).toString());
                    return false;
                }
            }
        } catch (Throwable th) {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e4) {
                    Slog.e(TAG, "writeToFile, finally fos.close() IOException e:" + e4.getMessage());
                }
            }
            throw th;
        }
    }

    private static String getInfoFromPath(String path) {
        StringBuilder sb;
        BufferedReader reader = null;
        try {
            try {
                reader = new BufferedReader(new FileReader(path));
                String info = reader.readLine();
                if (info != null) {
                    String trim = info.trim();
                    try {
                        reader.close();
                    } catch (IOException e) {
                        Slog.e(TAG, "getInfoFromPath, finally reader.close() IOException e:" + e.getMessage());
                    }
                    return trim;
                }
                try {
                    reader.close();
                    return null;
                } catch (IOException e2) {
                    e = e2;
                    sb = new StringBuilder();
                    Slog.e(TAG, sb.append("getInfoFromPath, finally reader.close() IOException e:").append(e.getMessage()).toString());
                    return null;
                }
            } catch (IOException e3) {
                Slog.e(TAG, "getInfoFromPath, IOException e:" + e3.getMessage());
                if (reader == null) {
                    return null;
                }
                try {
                    reader.close();
                    return null;
                } catch (IOException e4) {
                    e = e4;
                    sb = new StringBuilder();
                    Slog.e(TAG, sb.append("getInfoFromPath, finally reader.close() IOException e:").append(e.getMessage()).toString());
                    return null;
                }
            }
        } catch (Throwable th) {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e5) {
                    Slog.e(TAG, "getInfoFromPath, finally reader.close() IOException e:" + e5.getMessage());
                }
            }
            throw th;
        }
    }

    public void SetUntrustedAppCPUParams(boolean isCameraForeground) {
        int cpushares;
        String cpus;
        if (!TextUtils.isEmpty(UNTRUSTEDAPP_BG_CPUS) && sDefaultAffinity != null) {
            if (isCameraForeground) {
                cpus = UNTRUSTEDAPP_BG_CPUS;
                cpushares = sDefaultCpuShares / 2;
            } else {
                cpus = sDefaultAffinity;
                cpushares = sDefaultCpuShares;
            }
            Slog.i(TAG, "setaffinity: " + cpus + " sDefaultAffinity: " + sDefaultAffinity + " setcpushares: " + cpushares + " sDefaultCpuShares: " + sDefaultCpuShares);
            writeToFile(UNTRUSTEDAPP_BG_CPUS_PATH, cpus);
            if (sDefaultCpuShares != 0) {
                writeToFile(UNTRUSTEDAPP_BG_CPUSHARES_PATH, Integer.toString(cpushares));
            }
        }
    }
}
