package com.miui.server;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings;
import android.server.am.SplitScreenReporter;
import android.util.Base64;
import android.util.Log;
import android.util.Slog;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.util.Arrays;
import miui.usb.IMiuiUsbManager;
/* loaded from: classes.dex */
public class MiuiUsbService extends IMiuiUsbManager.Stub {
    public static final String SERVICE_NAME = "miui.usb.service";
    private static final String TAG = "MiuiUsbService";
    private final BroadcastReceiver mBootCompletedReceiver;
    private Context mContext;
    private UsbDebuggingManager mUsbDebuggingManager;

    public MiuiUsbService(Context context) {
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { // from class: com.miui.server.MiuiUsbService.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                if (MiuiUsbService.this.mUsbDebuggingManager != null) {
                    MiuiUsbService.this.mUsbDebuggingManager.setAdbEnabled(MiuiUsbService.containsFunction(SystemProperties.get("persist.sys.usb.config", "adb"), "adb"));
                }
            }
        };
        this.mBootCompletedReceiver = broadcastReceiver;
        this.mContext = context;
        if (SplitScreenReporter.ACTION_ENTER_SPLIT.equals(SystemProperties.get("ro.adb.secure"))) {
            this.mUsbDebuggingManager = new UsbDebuggingManager(context);
        }
        context.registerReceiver(broadcastReceiver, new IntentFilter("android.intent.action.BOOT_COMPLETED"));
    }

    public static boolean containsFunction(String functions, String function) {
        int index = functions.indexOf(function);
        if (index < 0) {
            return false;
        }
        if (index > 0 && functions.charAt(index - 1) != ',') {
            return false;
        }
        int charAfter = function.length() + index;
        if (charAfter < functions.length() && functions.charAt(charAfter) != ',') {
            return false;
        }
        return true;
    }

    public void acceptMdbRestore() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
    }

    public void cancelMdbRestore() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
    }

    public void allowUsbDebugging(boolean alwaysAllow, String publicKey) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        this.mUsbDebuggingManager.allowUsbDebugging(alwaysAllow, publicKey);
    }

    public void denyUsbDebugging() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.MANAGE_USB", null);
        this.mUsbDebuggingManager.denyUsbDebugging();
    }

    public void dump(FileDescriptor fd, PrintWriter pw) {
        UsbDebuggingManager usbDebuggingManager = this.mUsbDebuggingManager;
        if (usbDebuggingManager != null) {
            usbDebuggingManager.dump(fd, pw);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class UsbDebuggingManager extends UsbManagerConnect {
        private final String ADBD_SOCKET = "adbd";
        private final String ADB_DIRECTORY = "misc/adb";
        private final String ADB_KEYS_FILE = "adb_keys";
        private boolean mAdbEnabled = false;
        private ContentResolver mContentResolver;
        private Context mContext;
        private String mFingerprints;
        private final Handler mHandler;
        private final HandlerThread mHandlerThread;
        private Thread mThread;

        public UsbDebuggingManager(Context context) {
            MiuiUsbService.this = this$0;
            HandlerThread handlerThread = new HandlerThread("UsbDebuggingHandler");
            this.mHandlerThread = handlerThread;
            handlerThread.start();
            this.mHandler = new UsbDebuggingHandler(handlerThread.getLooper());
            this.mContext = context;
            ContentResolver contentResolver = context.getContentResolver();
            this.mContentResolver = contentResolver;
            contentResolver.registerContentObserver(Settings.Secure.getUriFor("adb_enabled"), false, new AdbSettingsObserver());
        }

        @Override // com.miui.server.UsbManagerConnect
        void listenToSocket() throws IOException {
            try {
                try {
                    byte[] buffer = new byte[4096];
                    LocalSocketAddress address = new LocalSocketAddress("adbd", LocalSocketAddress.Namespace.RESERVED);
                    this.mSocket = new LocalSocket();
                    this.mSocket.connect(address);
                    Log.i(MiuiUsbService.TAG, "connected to adbd");
                    this.mOutputStream = this.mSocket.getOutputStream();
                    InputStream inputStream = this.mSocket.getInputStream();
                    while (true) {
                        int count = inputStream.read(buffer);
                        if (count < 0) {
                            Slog.e(MiuiUsbService.TAG, "got " + count + " reading");
                            break;
                        } else if (buffer[0] != 80 || buffer[1] != 75) {
                            break;
                        } else {
                            String key = new String(Arrays.copyOfRange(buffer, 2, count));
                            Slog.d(MiuiUsbService.TAG, "Received public key: " + key);
                            Message msg = this.mHandler.obtainMessage(5);
                            msg.obj = key;
                            this.mHandler.sendMessage(msg);
                        }
                    }
                    Slog.e(MiuiUsbService.TAG, "Wrong message: " + new String(Arrays.copyOfRange(buffer, 0, 2)));
                } catch (IOException ex) {
                    Slog.e(MiuiUsbService.TAG, "Communication error: ", ex);
                    throw ex;
                }
            } finally {
                closeSocket();
            }
        }

        @Override // java.lang.Runnable
        public void run() {
            while (this.mAdbEnabled) {
                try {
                    listenToSocket();
                } catch (Exception e) {
                    SystemClock.sleep(1000L);
                }
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes.dex */
        public class AdbSettingsObserver extends ContentObserver {
            /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
            public AdbSettingsObserver() {
                super(null);
                UsbDebuggingManager.this = r1;
            }

            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                boolean z = false;
                if (Settings.Secure.getInt(UsbDebuggingManager.this.mContentResolver, "adb_enabled", 0) > 0) {
                    z = true;
                }
                boolean enable = z;
                UsbDebuggingManager.this.setAdbEnabled(enable);
            }
        }

        /* JADX INFO: Access modifiers changed from: package-private */
        /* loaded from: classes.dex */
        public class UsbDebuggingHandler extends Handler {
            private static final int MESSAGE_ADB_ALLOW = 3;
            private static final int MESSAGE_ADB_CONFIRM = 5;
            private static final int MESSAGE_ADB_DENY = 4;
            private static final int MESSAGE_ADB_DISABLED = 2;
            private static final int MESSAGE_ADB_ENABLED = 1;

            /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
            public UsbDebuggingHandler(Looper looper) {
                super(looper);
                UsbDebuggingManager.this = this$1;
            }

            @Override // android.os.Handler
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        if (!UsbDebuggingManager.this.mAdbEnabled) {
                            UsbDebuggingManager.this.mAdbEnabled = true;
                            UsbDebuggingManager.this.mThread = new Thread(UsbDebuggingManager.this);
                            UsbDebuggingManager.this.mThread.start();
                            return;
                        }
                        return;
                    case 2:
                        if (UsbDebuggingManager.this.mAdbEnabled) {
                            UsbDebuggingManager.this.mAdbEnabled = false;
                            UsbDebuggingManager.this.closeSocket();
                            try {
                                UsbDebuggingManager.this.mThread.join();
                            } catch (Exception e) {
                            }
                            UsbDebuggingManager.this.mThread = null;
                            UsbDebuggingManager.this.mOutputStream = null;
                            UsbDebuggingManager.this.mSocket = null;
                            return;
                        }
                        return;
                    case 3:
                        String key = (String) msg.obj;
                        String fingerprints = UsbDebuggingManager.this.getFingerprints(key);
                        if (!fingerprints.equals(UsbDebuggingManager.this.mFingerprints)) {
                            Slog.e(MiuiUsbService.TAG, "Fingerprints do not match. Got " + fingerprints + ", expected " + UsbDebuggingManager.this.mFingerprints);
                            return;
                        }
                        if (msg.arg1 == 1) {
                            UsbDebuggingManager.this.writeKey(key);
                        }
                        UsbDebuggingManager.this.sendResponse("OK");
                        return;
                    case 4:
                        UsbDebuggingManager.this.sendResponse("NO");
                        return;
                    case 5:
                        String key2 = (String) msg.obj;
                        UsbDebuggingManager usbDebuggingManager = UsbDebuggingManager.this;
                        usbDebuggingManager.mFingerprints = usbDebuggingManager.getFingerprints(key2);
                        UsbDebuggingManager usbDebuggingManager2 = UsbDebuggingManager.this;
                        usbDebuggingManager2.showConfirmationDialog(key2, usbDebuggingManager2.mFingerprints);
                        return;
                    default:
                        return;
                }
            }
        }

        public String getFingerprints(String key) {
            StringBuilder sb = new StringBuilder();
            try {
                MessageDigest digester = MessageDigest.getInstance("MD5");
                byte[] base64_data = key.split("\\s+")[0].getBytes();
                byte[] digest = digester.digest(Base64.decode(base64_data, 0));
                for (int i = 0; i < digest.length; i++) {
                    sb.append("0123456789ABCDEF".charAt((digest[i] >> 4) & 15));
                    sb.append("0123456789ABCDEF".charAt(digest[i] & 15));
                    if (i < digest.length - 1) {
                        sb.append(":");
                    }
                }
                return sb.toString();
            } catch (Exception ex) {
                Slog.e(MiuiUsbService.TAG, "Error getting digester: " + ex);
                return "";
            }
        }

        public void showConfirmationDialog(String key, String fingerprints) {
            Intent dialogIntent = new Intent();
            dialogIntent.setClassName("com.android.systemui", "com.android.systemui.usb.UsbDebuggingActivity");
            dialogIntent.addFlags(268435456);
            dialogIntent.putExtra("key", key);
            dialogIntent.putExtra("fingerprints", fingerprints);
            try {
                this.mContext.startActivity(dialogIntent);
            } catch (ActivityNotFoundException e) {
                Slog.e(MiuiUsbService.TAG, "unable to start UsbDebuggingActivity");
            }
        }

        public void writeKey(String key) {
            File dataDir = Environment.getDataDirectory();
            File adbDir = new File(dataDir, "misc/adb");
            if (!adbDir.exists()) {
                Slog.e(MiuiUsbService.TAG, "ADB data directory does not exist");
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
                Slog.e(MiuiUsbService.TAG, "Error writing key:" + ex);
            }
        }

        public void setAdbEnabled(boolean enabled) {
            this.mHandler.sendEmptyMessage(enabled ? 1 : 2);
        }

        public void allowUsbDebugging(boolean alwaysAllow, String publicKey) {
            Message msg = this.mHandler.obtainMessage(3);
            msg.arg1 = alwaysAllow ? 1 : 0;
            msg.obj = publicKey;
            this.mHandler.sendMessage(msg);
        }

        public void denyUsbDebugging() {
            this.mHandler.sendEmptyMessage(4);
        }

        public void dump(FileDescriptor fd, PrintWriter pw) {
            pw.println("  USB Debugging State:");
            pw.println("    Connected to adbd: " + (this.mOutputStream != null));
            pw.println("    Last key received: " + this.mFingerprints);
            pw.println("    User keys:");
            try {
                pw.println(FileUtils.readTextFile(new File("/data/misc/adb/adb_keys"), 0, null));
            } catch (IOException e) {
                pw.println("IOException: " + e);
            }
            pw.println("    System keys:");
            try {
                pw.println(FileUtils.readTextFile(new File("/adb_keys"), 0, null));
            } catch (IOException e2) {
                pw.println("IOException: " + e2);
            }
        }
    }
}
