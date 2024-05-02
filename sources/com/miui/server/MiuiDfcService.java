package com.miui.server;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Slog;
import miui.dfc.IDfc;
import miui.dfc.IDfcService;
import miui.dfc.IDupCompressCallback;
import miui.dfc.IDuplicateFileScanCallback;
import miui.dfc.IGetDuplicateFilesCallback;
/* loaded from: classes.dex */
public class MiuiDfcService extends IDfcService.Stub {
    public static final String ACTION_TEST_USB_STATE = "android.hardware.usb.action.USB_STATE";
    public static final String BUILD_TYPE;
    public static final boolean DFC_DEBUG = SystemProperties.getBoolean("persist.sys.miui_dfc_debug", false);
    private static final String DFC_NATIVE_SERVICE = "DfcNativeService";
    public static final String IS_FRONT_SCENE = "sys.dfcservice.is_front_scene";
    public static final boolean IS_USERDEBUG;
    public static final String SERVICE_NAME = "miui.dfc.service";
    public static final String START_DFC_PROP = "sys.dfcservice.ctrl";
    public static final String STOP_COMPRESS_PROP = "sys.dfcservice.stop_compress";
    public static final String STOP_SCAN_PROP = "sys.dfcservice.stop_scan";
    private static final String TAG = "MiuiDfcService";
    private static volatile MiuiDfcService sInstance;
    private Context mContext;
    private volatile IDfc mService;
    private boolean mUsbConnect = false;
    IBinder.DeathRecipient mDeathHandler = new IBinder.DeathRecipient() { // from class: com.miui.server.MiuiDfcService.1
        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            Slog.e(MiuiDfcService.TAG, "DfcNativeService binderDied!");
            MiuiDfcService.this.mService = null;
        }
    };

    static {
        String str = SystemProperties.get("ro.build.type", "");
        BUILD_TYPE = str;
        IS_USERDEBUG = "userdebug".equals(str);
    }

    public MiuiDfcService forDfcInitialization(Context context) {
        this.mContext = context;
        if (isDfcDebug()) {
            Slog.d(TAG, "use MiuiDfcService(Context context) mContext" + this.mContext);
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_TEST_USB_STATE);
        this.mContext.registerReceiver(new UsbStatusReceiver(), filter);
        return getInstance();
    }

    public static MiuiDfcService getInstance() {
        if (sInstance == null) {
            synchronized (MiuiDfcService.class) {
                if (sInstance == null) {
                    sInstance = new MiuiDfcService();
                }
            }
        }
        return sInstance;
    }

    protected IDfc getDfcNativeService() {
        int isScene = SystemProperties.getInt(IS_FRONT_SCENE, 0);
        if (this.mUsbConnect && isScene == 0) {
            Slog.d(TAG, "Usb connect, no need to get DfcNativeService, isScene:" + isScene);
            SystemProperties.set(START_DFC_PROP, "false");
            return null;
        }
        if (this.mService == null) {
            synchronized (MiuiDfcService.class) {
                if (this.mService == null) {
                    if (!SystemProperties.getBoolean(START_DFC_PROP, false)) {
                        SystemProperties.set(START_DFC_PROP, "true");
                        try {
                            Thread.sleep(500L);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    this.mService = IDfc.Stub.asInterface(ServiceManager.getService(DFC_NATIVE_SERVICE));
                    if (this.mService != null) {
                        try {
                            this.mService.asBinder().linkToDeath(this.mDeathHandler, 0);
                        } catch (Exception e2) {
                            e2.printStackTrace();
                        }
                    } else {
                        Slog.e(TAG, "failed to get DfcNativeService.");
                    }
                }
            }
        }
        return this.mService;
    }

    private MiuiDfcService() {
        Slog.d(TAG, "Create MiuiDfcService");
    }

    public int getDFCVersion() {
        if (!checkIsSystemUid()) {
            Slog.d(TAG, "calling permission denied");
            return -1;
        } else if (isDfcDebug()) {
            Slog.d(TAG, "getDFCVersion!");
            return 1;
        } else {
            return 1;
        }
    }

    public boolean isSupportDFC() {
        if (!checkIsSystemUid()) {
            Slog.d(TAG, "calling permission denied");
            return false;
        }
        int isScene = SystemProperties.getInt(IS_FRONT_SCENE, 0);
        if (isDfcDebug()) {
            Slog.d(TAG, "isSupportDFC! isScene:" + isScene);
        }
        return isScene == 1 || !this.mUsbConnect;
    }

    public void scanDuplicateFiles(String data, IDuplicateFileScanCallback callback, int flags) {
        if (!checkIsSystemUid()) {
            Slog.d(TAG, "calling permission denied");
            return;
        }
        Slog.d(TAG, "scanDuplicateFiles!");
        try {
            IDfc service = getDfcNativeService();
            if (service != null) {
                SystemProperties.set(STOP_SCAN_PROP, "false");
                service.Dfc_scanDuplicateFiles(data);
                if (isDfcDebug()) {
                    Slog.d(TAG, "Dfc_scanDuplicateFiles done");
                }
            } else if (isDfcDebug()) {
                Slog.d(TAG, "scanDuplicateFiles getDfcNativeService is null");
            }
        } catch (Exception e) {
            Slog.e(TAG, "scanDuplicateFiles error: " + e.toString());
        }
        if (callback != null) {
            try {
                callback.onDumplicateFileScanfinished();
            } catch (Exception e2) {
                Slog.e(TAG, "onDumplicateFileScanfinished error: " + e2.toString());
            }
        }
    }

    public void getDuplicateFiles(String data, IGetDuplicateFilesCallback callback) {
        if (!checkIsSystemUid()) {
            Slog.d(TAG, "calling permission denied");
            return;
        }
        if (isDfcDebug()) {
            Slog.d(TAG, "getDuplicateFiles! data: " + data);
        }
        String result = "";
        try {
            IDfc service = getDfcNativeService();
            if (service != null) {
                result = service.Dfc_getDuplicateFiles(data);
                if (isDfcDebug()) {
                    Slog.d(TAG, "Dfc_getDuplicateFiles, result: " + result);
                }
            } else if (isDfcDebug()) {
                Slog.d(TAG, "getDuplicateFiles getDfcNativeService is null");
            }
        } catch (Exception e) {
            Slog.e(TAG, "Dfc_getDuplicateFiles error:  " + e.toString());
        }
        if (callback != null) {
            try {
                callback.onFinish(result);
            } catch (Exception e2) {
                Slog.e(TAG, "IGetDuplicateFilesCallback callback error: " + e2.toString());
            }
        }
    }

    public void compressDuplicateFiles(String data, IDupCompressCallback callback) {
        if (!checkIsSystemUid()) {
            Slog.d(TAG, "calling permission denied");
            return;
        }
        Slog.d(TAG, "compressDuplicateFiles!");
        try {
            IDfc service = getDfcNativeService();
            if (service != null) {
                SystemProperties.set(STOP_COMPRESS_PROP, "false");
                String result = service.Dfc_compressDuplicateFiles(data);
                if (isDfcDebug()) {
                    Slog.d(TAG, "Dfc_compressDuplicateFiles, result: " + result);
                }
                String[] countSizeArray = result.split(",");
                if (callback != null && countSizeArray != null && countSizeArray.length == 2) {
                    callback.onFinish(Long.parseLong(countSizeArray[0].trim()), Long.parseLong(countSizeArray[1].trim()));
                    return;
                }
            } else if (isDfcDebug()) {
                Slog.d(TAG, "compressDuplicateFiles getDfcNativeService is null");
            }
        } catch (Exception e) {
            Slog.e(TAG, "Dfc_compressDuplicateFiles error:  " + e.toString());
        }
        if (callback != null) {
            try {
                callback.onFinish(0L, 0L);
            } catch (Exception e2) {
                Slog.e(TAG, "onFinish error:  " + e2.toString());
            }
        }
    }

    public void setDFCExceptRules(String data) {
        if (!checkIsSystemUid()) {
            Slog.d(TAG, "calling permission denied");
            return;
        }
        if (isDfcDebug()) {
            Slog.d(TAG, "setDFCExceptRules!, data:" + data);
        }
        try {
            IDfc service = getDfcNativeService();
            if (service != null) {
                service.Dfc_setDFCExceptRules(data);
            } else if (isDfcDebug()) {
                Slog.d(TAG, "setDFCExceptRules getDfcNativeService is null");
            }
        } catch (Exception e) {
            Slog.e(TAG, "Dfc_setDFCExceptRules error: " + e.toString());
        }
    }

    public long restoreDuplicateFiles(String data) {
        if (!checkIsSystemUid()) {
            Slog.d(TAG, "calling permission denied");
            return 0L;
        }
        if (isDfcDebug()) {
            Slog.d(TAG, "restoreDuplicateFiles!, data:" + data);
        }
        long result = 0;
        try {
            IDfc service = getDfcNativeService();
            if (service != null) {
                result = service.Dfc_restoreDuplicateFiles(data);
                if (isDfcDebug()) {
                    Slog.d(TAG, "Dfc_restoreDuplicateFiles, result:" + result);
                }
            } else if (isDfcDebug()) {
                Slog.d(TAG, "restoreDuplicateFiles getDfcNativeService is null");
            }
        } catch (Exception e) {
            Slog.e(TAG, "Dfc_restoreDuplicateFiles error:  " + e.toString());
        }
        return result;
    }

    /* loaded from: classes.dex */
    public class UsbStatusReceiver extends BroadcastReceiver {
        public UsbStatusReceiver() {
            MiuiDfcService.this = this$0;
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            int isScene = SystemProperties.getInt(MiuiDfcService.IS_FRONT_SCENE, 0);
            if (intent == null || MiuiDfcService.DFC_DEBUG || isScene == 1) {
                Slog.w(MiuiDfcService.TAG, "cleanmaster isScene: " + isScene);
                return;
            }
            String action = intent.getAction();
            if (MiuiDfcService.ACTION_TEST_USB_STATE.equals(action)) {
                boolean connected = intent.getExtras().getBoolean("connected");
                if (connected) {
                    SystemProperties.set(MiuiDfcService.START_DFC_PROP, "false");
                    MiuiDfcService.this.mUsbConnect = true;
                    Slog.d(MiuiDfcService.TAG, "Usb connect, shutdown DfcNativeService");
                    return;
                }
                MiuiDfcService.this.mUsbConnect = false;
                Slog.d(MiuiDfcService.TAG, "Usb disconnect");
            }
        }
    }

    private boolean checkIsSystemUid() {
        return 1000 == Binder.getCallingUid();
    }

    public boolean isDfcDebug() {
        return IS_USERDEBUG || DFC_DEBUG;
    }
}
