package com.miui.server.rescue;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Slog;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
/* loaded from: classes.dex */
public class AutoUsbDebuggingManager implements Runnable {
    static final int BUFFER_SIZE = 4096;
    private static final String TAG = "BrokenScreenRescueService";
    private final Handler mHandler;
    OutputStream mOutputStream;
    LocalSocket mSocket;
    private Thread mThread;
    private boolean mAdbEnabled = false;
    private final String ADBD_SOCKET = "adbd";
    private final String ADB_DIRECTORY = "misc/adb";
    private final String ADB_KEYS_FILE = "adb_keys";

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class UsbDebuggingHandler extends Handler {
        private static final int MESSAGE_ADB_ALLOW = 3;
        private static final int MESSAGE_ADB_DISABLED = 2;
        private static final int MESSAGE_ADB_ENABLED = 1;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public UsbDebuggingHandler(Looper looper) {
            super(looper);
            AutoUsbDebuggingManager.this = this$0;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    if (!AutoUsbDebuggingManager.this.mAdbEnabled) {
                        AutoUsbDebuggingManager.this.mAdbEnabled = true;
                        AutoUsbDebuggingManager.this.mThread = new Thread(AutoUsbDebuggingManager.this);
                        AutoUsbDebuggingManager.this.mThread.start();
                        return;
                    }
                    return;
                case 2:
                    if (AutoUsbDebuggingManager.this.mAdbEnabled) {
                        AutoUsbDebuggingManager.this.mAdbEnabled = false;
                        AutoUsbDebuggingManager.this.closeSocket();
                        try {
                            AutoUsbDebuggingManager.this.mThread.join();
                        } catch (Exception e) {
                            Slog.e("BrokenScreenRescueService", "UsbDebuggingHandler MESSAGE_ADB_DISABLED join failed");
                        }
                        AutoUsbDebuggingManager.this.mThread = null;
                        AutoUsbDebuggingManager.this.mOutputStream = null;
                        AutoUsbDebuggingManager.this.mSocket = null;
                        return;
                    }
                    return;
                case 3:
                    String key = (String) msg.obj;
                    if (msg.arg1 == 1) {
                        AutoUsbDebuggingManager.this.writeKey(key);
                    }
                    AutoUsbDebuggingManager.this.sendResponse("OK");
                    return;
                default:
                    Slog.e("BrokenScreenRescueService", "UsbDebuggingHandler msg " + msg.what);
                    return;
            }
        }
    }

    public AutoUsbDebuggingManager(Looper looper) {
        this.mHandler = new UsbDebuggingHandler(looper);
    }

    public void setAdbEnabled(boolean enabled) {
        this.mHandler.sendEmptyMessage(enabled ? 1 : 2);
    }

    synchronized void closeSocket() {
        OutputStream outputStream = this.mOutputStream;
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException ex) {
                Slog.e("BrokenScreenRescueService", "Failed closing output stream: " + ex);
            }
        }
        LocalSocket localSocket = this.mSocket;
        if (localSocket != null) {
            try {
                localSocket.close();
            } catch (IOException ex2) {
                Slog.e("BrokenScreenRescueService", "Failed closing socket: " + ex2);
            }
        }
    }

    synchronized void sendResponse(String msg) {
        OutputStream outputStream = this.mOutputStream;
        if (outputStream != null) {
            try {
                outputStream.write(msg.getBytes());
            } catch (IOException ex) {
                Slog.e("BrokenScreenRescueService", "Failed to write response:", ex);
            }
        }
    }

    synchronized void sendResponse(int msgType, int msgId, byte[] byteMsg) {
        OutputStream outputStream = this.mOutputStream;
        if (outputStream != null && byteMsg != null) {
            try {
                outputStream.write(String.format("%04x", Integer.valueOf(msgId)).getBytes());
                this.mOutputStream.write(String.format("%04x", Integer.valueOf(msgType)).getBytes());
                this.mOutputStream.write(String.format("%08x", Integer.valueOf(byteMsg.length)).getBytes());
                this.mOutputStream.write(byteMsg);
                this.mOutputStream.flush();
            } catch (IOException ex) {
                Slog.e("BrokenScreenRescueService", "Failed to write response:", ex);
            }
        }
    }

    public void writeKey(String key) {
        File dataDir = Environment.getDataDirectory();
        File adbDir = new File(dataDir, "misc/adb");
        if (!adbDir.exists()) {
            Slog.e("BrokenScreenRescueService", "ADB data directory does not exist");
            return;
        }
        try {
            File keyFile = new File(adbDir, "adb_keys");
            if (!keyFile.exists()) {
                keyFile.createNewFile();
                FileUtils.setPermissions(keyFile.toString(), 416, -1, -1);
            }
            FileOutputStream fo = new FileOutputStream(keyFile, true);
            fo.write(key.getBytes());
            fo.write(10);
            fo.close();
        } catch (IOException ex) {
            Slog.e("BrokenScreenRescueService", "Error writing key:" + ex);
        }
    }

    void listenToSocket() throws IOException {
        try {
            try {
                byte[] buffer = new byte[4096];
                LocalSocketAddress address = new LocalSocketAddress("adbd", LocalSocketAddress.Namespace.RESERVED);
                LocalSocket localSocket = new LocalSocket();
                this.mSocket = localSocket;
                localSocket.connect(address);
                Slog.i("BrokenScreenRescueService", "connected to adbd");
                this.mOutputStream = this.mSocket.getOutputStream();
                InputStream inputStream = this.mSocket.getInputStream();
                while (true) {
                    int count = inputStream.read(buffer);
                    if (count < 0) {
                        Slog.e("BrokenScreenRescueService", "got " + count + " reading");
                        break;
                    } else if (buffer[0] != 80 || buffer[1] != 75) {
                        break;
                    } else {
                        String key = new String(Arrays.copyOfRange(buffer, 2, count));
                        Message msg = this.mHandler.obtainMessage(3);
                        msg.obj = key;
                        this.mHandler.sendMessage(msg);
                    }
                }
                Slog.e("BrokenScreenRescueService", "Wrong message: " + new String(Arrays.copyOfRange(buffer, 0, 2)));
            } catch (IOException ex) {
                Slog.e("BrokenScreenRescueService", "Communication error: ", ex);
                throw ex;
            }
        } finally {
            closeSocket();
        }
    }

    @Override // java.lang.Runnable
    public void run() {
        try {
            listenToSocket();
        } catch (Exception e) {
            SystemClock.sleep(1000L);
        }
    }
}
