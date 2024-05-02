package com.android.server.am;

import android.content.Context;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Binder;
import android.os.Debug;
import android.os.SystemProperties;
import android.util.Slog;
import com.android.internal.util.DumpUtils;
import com.android.server.LocalServices;
import com.android.server.am.SystemPressureController;
import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
/* loaded from: classes.dex */
public class MiuiMemoryService extends Binder implements MiuiMemoryServiceInternal {
    public static final int COMPACT_MODE_CAMERA = 0;
    public static final int COMPACT_MODE_LMKD = 3;
    public static final int COMPACT_MODE_PMC = 4;
    public static final int COMPACT_MODE_SCREENOFF_CHARGING = 2;
    public static final int COMPACT_MODE_SPTM = 1;
    private static final byte LMK_CAMERA_MODE = 18;
    public static final String SERVICE_NAME = "miui.memory.service";
    private static final String TAG = "MiuiMemoryService";
    public static final int VMPRESS_LEVEL_CRITICAL = 2;
    public static final int VMPRESS_LEVEL_LOW = 0;
    public static final int VMPRESS_LEVEL_MEDIUM = 1;
    public static final int VMPRESS_LEVEL_SUPER_CRITICAL = 3;
    private static final int VMPRESS_POLICY_CHECK_IO = 4;
    private static final int VMPRESS_POLICY_GRECLAM = 0;
    private static final int VMPRESS_POLICY_GRECLAM_AND_PM = 3;
    private static final int VMPRESS_POLICY_GSRECLAM = 1;
    private static final int VMPRESS_POLICY_PRECLAM = 2;
    private static final int VMPRESS_POLICY_TO_MI_PM = 17;
    private Context mContext;
    private OutputStream mLmkdOutputStream;
    private LocalSocket mLmkdSocket;
    public static final boolean DEBUG = SystemProperties.getBoolean("debug.sys.mms", false);
    private static boolean sCompactionEnable = SystemProperties.getBoolean("persist.sys.mms.compact_enable", false);
    private static boolean sCompactSingleProcEnable = SystemProperties.getBoolean("persist.sys.mms.single_compact_enable", true);
    private static long sCompactionMinZramFreeKb = SystemProperties.getLong("persist.sys.mms.min_zramfree_kb", 307200);
    private static boolean sWriteEnable = SystemProperties.getBoolean("persist.sys.mms.write_lmkd", false);
    private Object mWriteLock = new Object();
    private MiuiMemReclaimer mReclaimer = new MiuiMemReclaimer();

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class MiuiMemServiceThread extends Thread {
        public static final String HOST_NAME = "mi_reclaim";

        private MiuiMemServiceThread() {
            MiuiMemoryService.this = r1;
        }

        @Override // java.lang.Thread, java.lang.Runnable
        public void run() {
            LocalServerSocket serverSocket = null;
            ExecutorService threadExecutor = Executors.newCachedThreadPool();
            try {
                try {
                    if (MiuiMemoryService.DEBUG) {
                        Slog.d("MiuiMemoryService", "Create local socket: mi_reclaim");
                    }
                    serverSocket = new LocalServerSocket(HOST_NAME);
                    while (true) {
                        if (MiuiMemoryService.DEBUG) {
                            Slog.d("MiuiMemoryService", "Waiting Client connected...");
                        }
                        LocalSocket socket = serverSocket.accept();
                        socket.setReceiveBufferSize(256);
                        socket.setSendBufferSize(256);
                        Slog.d("MiuiMemoryService", "There is a client is accepted: " + socket.toString());
                        threadExecutor.execute(new ConnectionHandler(socket));
                    }
                } catch (Exception e) {
                    Slog.e("MiuiMemoryService", "mi_reclaim connection catch Exception");
                    Slog.w("MiuiMemoryService", "mi_reclaim connection finally shutdown!");
                    if (threadExecutor != null) {
                        threadExecutor.shutdown();
                    }
                    if (serverSocket != null) {
                        try {
                            serverSocket.close();
                        } catch (IOException e2) {
                            e2.printStackTrace();
                        }
                    }
                    if (MiuiMemoryService.DEBUG) {
                        Slog.d("MiuiMemoryService", "mi_reclaim connection ended!");
                    }
                }
            } catch (Throwable th) {
                Slog.w("MiuiMemoryService", "mi_reclaim connection finally shutdown!");
                if (threadExecutor != null) {
                    threadExecutor.shutdown();
                }
                if (serverSocket != null) {
                    try {
                        serverSocket.close();
                    } catch (IOException e3) {
                        e3.printStackTrace();
                    }
                }
                throw th;
            }
        }
    }

    /* loaded from: classes.dex */
    private class ConnectionHandler implements Runnable {
        private LocalSocket mSocket;
        private boolean mIsContinue = true;
        private InputStreamReader mInput = null;

        public ConnectionHandler(LocalSocket clientSocket) {
            MiuiMemoryService.this = r1;
            this.mSocket = clientSocket;
        }

        public void terminate() {
            Slog.d("MiuiMemoryService", "Reclaim trigger terminate!");
            this.mIsContinue = false;
        }

        @Override // java.lang.Runnable
        public void run() {
            Slog.d("MiuiMemoryService", "mi_reclaim new connection: " + this.mSocket.toString());
            BufferedReader bufferedReader = null;
            try {
                try {
                    try {
                        try {
                            this.mInput = new InputStreamReader(this.mSocket.getInputStream());
                            bufferedReader = new BufferedReader(this.mInput);
                            while (this.mIsContinue) {
                                String data = bufferedReader.readLine();
                                String[] result = data.split(":");
                                if (result[0] != null && result[1] != null) {
                                    int vmPressureLevel = Integer.parseInt(result[0].trim());
                                    int vmPressurePolicy = Integer.parseInt(result[1].trim());
                                    if (MiuiMemoryService.DEBUG) {
                                        Slog.d("MiuiMemoryService", "vmPressureLevel = " + vmPressureLevel + " , vmPressurePolicy = " + vmPressurePolicy);
                                    }
                                    MiuiMemoryService.this.handleReclaimTrigger(vmPressureLevel, vmPressurePolicy);
                                }
                                Slog.e("MiuiMemoryService", "Received mi_reclaim data error");
                            }
                            bufferedReader.close();
                        } catch (IOException e) {
                            Slog.e("MiuiMemoryService", "mi_reclaim connection: " + this.mSocket.toString() + " IOException");
                            if (bufferedReader == null) {
                                return;
                            }
                            bufferedReader.close();
                        }
                    } catch (Exception e2) {
                        terminate();
                        Slog.e("MiuiMemoryService", "mi_reclaim connection: " + this.mSocket.toString() + " Exception");
                        if (bufferedReader == null) {
                            return;
                        }
                        bufferedReader.close();
                    }
                } catch (IOException e3) {
                }
            } catch (Throwable th) {
                if (0 != 0) {
                    try {
                        bufferedReader.close();
                    } catch (IOException e4) {
                    }
                }
                throw th;
            }
        }
    }

    public void handleReclaimTrigger(int vmPressureLevel, int vmPressurePolicy) {
        if (vmPressureLevel < 0 || vmPressureLevel > 3 || vmPressurePolicy < 0 || vmPressurePolicy > 17) {
            Slog.w("MiuiMemoryService", "mi_reclaim data is invalid!");
            return;
        }
        switch (vmPressurePolicy) {
            case 0:
            case 1:
                runGlobalCompaction(vmPressureLevel);
                return;
            case 2:
                runProcsCompaction(3);
                return;
            case 3:
                runGlobalCompaction(vmPressureLevel);
                triggerProcessClean();
                return;
            case 4:
            default:
                return;
            case 17:
                triggerProcessClean();
                return;
        }
    }

    private void triggerProcessClean() {
        if (DEBUG) {
            Slog.d("MiuiMemoryService", "Call process cleaner");
        }
        SystemPressureController.getInstance().triggerProcessClean();
    }

    public MiuiMemoryService(Context context) {
        this.mContext = context;
        MiuiMemServiceThread memServer = new MiuiMemServiceThread();
        memServer.start();
        initHelper(context);
        SystemPressureController.getInstance().setProcessCompactStateListener(new SystemPressureController.ProcessCompactStateListener() { // from class: com.android.server.am.MiuiMemoryService.1
            @Override // com.android.server.am.SystemPressureController.ProcessCompactStateListener
            public void onCompactProcess(ProcessRecord proc) {
                MiuiMemoryService.this.runProcCompaction(proc, 4);
            }

            @Override // com.android.server.am.SystemPressureController.ProcessCompactStateListener
            public void onCompactProcesses() {
                MiuiMemoryService.this.runProcsCompaction(4);
            }
        });
        LocalServices.addService(MiuiMemoryServiceInternal.class, this);
    }

    private void initHelper(Context context) {
        if (!sCompactionEnable) {
            Slog.w("MiuiMemoryService", "MiuiMemServiceHelper didn't init...");
            return;
        }
        MiuiMemServiceHelper memHelper = new MiuiMemServiceHelper(context, this);
        memHelper.startWork();
        if (DEBUG) {
            Slog.d("MiuiMemoryService", "MiuiMemServiceHelper init complete");
        }
    }

    private boolean isZramFreeEnough() {
        long zramFreeKb = Debug.getZramFreeKb();
        if (zramFreeKb < sCompactionMinZramFreeKb) {
            if (DEBUG) {
                Slog.d("MiuiMemoryService", "Skipping compaction, zramFree too small. zramFree = " + zramFreeKb + " KB");
                return false;
            }
            return false;
        }
        return true;
    }

    @Override // com.android.server.am.MiuiMemoryServiceInternal
    public void runProcCompaction(ProcessRecord proc, int mode) {
        if (!sCompactSingleProcEnable || !isZramFreeEnough()) {
            return;
        }
        this.mReclaimer.runProcCompaction(proc, mode);
    }

    private boolean openLmkdSocket() {
        if (this.mLmkdSocket != null) {
            return true;
        }
        try {
            LocalSocket localSocket = new LocalSocket(3);
            this.mLmkdSocket = localSocket;
            localSocket.connect(new LocalSocketAddress("lmkd", LocalSocketAddress.Namespace.RESERVED));
            this.mLmkdOutputStream = this.mLmkdSocket.getOutputStream();
            return this.mLmkdSocket != null;
        } catch (IOException e) {
            Slog.e("MiuiMemoryService", "Lmkd socket open failed");
            this.mLmkdSocket = null;
            return false;
        }
    }

    @Override // com.android.server.am.MiuiMemoryServiceInternal
    public void writeLmkd(boolean isForeground) {
        if (!sWriteEnable) {
            return;
        }
        synchronized (this.mWriteLock) {
            if (this.mLmkdSocket == null && !openLmkdSocket()) {
                Slog.w("MiuiMemoryService", "Write lmkd failed for socket error!");
                return;
            }
            ByteBuffer buf = ByteBuffer.allocate(8);
            buf.putInt(18);
            buf.putInt(isForeground ? 1 : 0);
            try {
                this.mLmkdOutputStream.write(buf.array(), 0, buf.position());
                if (DEBUG) {
                    Slog.d("MiuiMemoryService", "Write lmkd success");
                }
            } catch (IOException e) {
                Slog.e("MiuiMemoryService", "Write lmkd failed for IOException");
                try {
                    this.mLmkdSocket.close();
                } catch (IOException e2) {
                    Slog.e("MiuiMemoryService", "Close lmkd socket failed for IOException!");
                }
                this.mLmkdSocket = null;
            }
        }
    }

    @Override // com.android.server.am.MiuiMemoryServiceInternal
    public void runGlobalCompaction(int vmPressureLevel) {
        if (!sCompactionEnable) {
            return;
        }
        this.mReclaimer.runGlobalCompaction(vmPressureLevel);
    }

    @Override // com.android.server.am.MiuiMemoryServiceInternal
    public void runProcsCompaction(int mode) {
        if (!sCompactionEnable || !isZramFreeEnough()) {
            return;
        }
        this.mReclaimer.runProcsCompaction(mode);
    }

    @Override // com.android.server.am.MiuiMemoryServiceInternal
    public void interruptProcsCompaction() {
        this.mReclaimer.interruptProcsCompaction();
    }

    @Override // com.android.server.am.MiuiMemoryServiceInternal
    public void performCompaction(String action, int pid) {
        this.mReclaimer.performCompaction(action, pid);
    }

    @Override // android.os.Binder
    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (!DumpUtils.checkDumpPermission(this.mContext, "MiuiMemoryService", pw)) {
            return;
        }
        if (!sCompactionEnable) {
            pw.println("Compaction isn't enabled!");
        } else {
            this.mReclaimer.dumpCompactionStats(pw);
        }
    }
}
