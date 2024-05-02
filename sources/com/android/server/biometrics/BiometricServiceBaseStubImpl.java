package com.android.server.biometrics;

import android.content.Context;
import android.database.ContentObserver;
import android.hardware.fingerprint.FingerprintFidoOut;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Handler;
import android.os.HwBinder;
import android.os.HwParcel;
import android.os.IBinder;
import android.os.IHwBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.util.Slog;
import com.android.server.biometrics.sensors.BaseClientMonitor;
import com.miui.base.MiuiStubRegistry;
import com.miui.server.input.PadManager;
import java.util.NoSuchElementException;
import miui.util.ITouchFeature;
/* loaded from: classes.dex */
public class BiometricServiceBaseStubImpl implements BiometricServiceBaseStub {
    protected static final String CIT_PACKAGE = "com.miui.cit";
    private static final int CMD_APP_AUTHEN = 1;
    private static final int CMD_APP_CANCEL_AUTHEN = 2;
    private static final int CMD_APP_CANCEL_ENROLL = 8;
    private static final int CMD_APP_ENROLL = 7;
    private static final int CMD_FW_LOCK_CANCEL = 5;
    private static final int CMD_FW_TOP_APP_CANCEL = 6;
    private static final int CMD_NOTIFY_FOD_LOWBRIGHTNESS_ALLOW_STATE = 7;
    private static final int CMD_NOTIFY_LOCK_OUT_TO_FOD_ENGINE = 5;
    private static final int CMD_NOTIFY_MONITOR_STATE_TO_FOD_ENGINE = 4;
    private static final int CMD_NOTIFY_TO_SURFACEFLINGER = 31111;
    private static final int CMD_VENDOR_AUTHENTICATED = 3;
    private static final int CMD_VENDOR_ENROLL_RES = 9;
    private static final int CMD_VENDOR_ERROR = 4;
    private static final int CMD_VENDOR_REMOVED = 11;
    private static final int CODE_EXT_CMD = 1;
    private static final int CODE_PROCESS_CMD = 1;
    protected static final boolean DEBUG = true;
    private static final int DEFAULT_DISPLAY = 0;
    private static final String DEFAULT_PACKNAME = "";
    private static final int DEFAULT_PARAM = 0;
    private static final String EXT_DESCRIPTOR = "vendor.xiaomi.hardware.fingerprintextension@1.0::IXiaomiFingerprint";
    private static final String FOD_SERVICE_NAME = "android.app.fod.ICallback";
    protected static final String HEART_RATE_PACKAGE = "com.mi.health";
    private static final String INTERFACE_DESCRIPTOR = "android.app.fod.ICallback";
    private static final boolean IS_FOLD;
    protected static final String KEYGUARD_PACKAGE = "com.android.systemui";
    private static final String NAME_EXT_DAEMON = "vendor.xiaomi.hardware.fingerprintextension@1.0::IXiaomiFingerprint";
    private static final String PRODUCT_NAME;
    protected static final String SETTING_PACKAGE = "com.android.settings";
    private static final int SETTING_VALUE_OFF = 0;
    private static final int SETTING_VALUE_ON = 1;
    private static final int SF_AUTH_START = 3;
    private static final int SF_AUTH_STOP = 4;
    private static final int SF_ENROLL_START = 1;
    private static final int SF_ENROLL_STOP = 2;
    private static final int SF_FINGERPRINT_NONE = 0;
    private static final int SF_HEART_RATE_START = 5;
    private static final int SF_HEART_RATE_STOP = 6;
    protected static final String TAG = "[FingerprintService]BiometricServiceBaseStubImpl";
    private static final int TOUCH_AUTHEN = 1;
    private static final int TOUCH_CANCEL = 0;
    private static final int TOUCH_ENROLL = 2;
    private static final int TOUCH_FW_LOCK_CANCEL = 3;
    private static final int TOUCH_MODE = 10;
    private static final int TOUCH_REMOVED = 100;
    private IHwBinder mExtDaemon;
    private IBinder mFodService;
    protected static int mRootUserId = 0;
    private static int mTouchStatus = -1;
    private static int mSfValue = -1;
    private static int mSfPackageType = -1;
    private static String RO_BOOT_HWC = SystemProperties.get("ro.boot.hwc", "");
    private static boolean mIsFodEngineEnabled = SystemProperties.get("ro.hardware.fp.fod.touch.ctl.version", "").equals("2.0");
    private final Object mFodLock = new Object();
    private int mSettingShowTapsState = 0;
    private int mSettingPointerLocationState = 0;
    protected int mSideFpUnlockType = -1;
    protected boolean IS_FOD = SystemProperties.getBoolean("ro.hardware.fp.fod", false);
    protected boolean IS_POWERFP = SystemProperties.getBoolean("ro.hardware.fp.sideCap", false);
    protected boolean IS_FOD_LHBM = SystemProperties.getBoolean("ro.vendor.localhbm.enable", false);
    protected int mLockoutMode = 0;
    private IHwBinder.DeathRecipient mDeathRecipient = new IHwBinder.DeathRecipient() { // from class: com.android.server.biometrics.BiometricServiceBaseStubImpl.1
        public void serviceDied(long cookie) {
            Slog.e(BiometricServiceBaseStubImpl.TAG, "mExtDaemon Died");
            if (BiometricServiceBaseStubImpl.this.mExtDaemon == null) {
                return;
            }
            try {
                BiometricServiceBaseStubImpl.this.mExtDaemon.unlinkToDeath(BiometricServiceBaseStubImpl.this.mDeathRecipient);
                BiometricServiceBaseStubImpl.this.mExtDaemon = null;
                BiometricServiceBaseStubImpl biometricServiceBaseStubImpl = BiometricServiceBaseStubImpl.this;
                biometricServiceBaseStubImpl.mExtDaemon = biometricServiceBaseStubImpl.getExtDaemon();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    };

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<BiometricServiceBaseStubImpl> {

        /* compiled from: BiometricServiceBaseStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final BiometricServiceBaseStubImpl INSTANCE = new BiometricServiceBaseStubImpl();
        }

        public BiometricServiceBaseStubImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public BiometricServiceBaseStubImpl provideNewInstance() {
            return new BiometricServiceBaseStubImpl();
        }
    }

    static {
        boolean z = false;
        if (SystemProperties.getInt("persist.sys.muiltdisplay_type", 0) == 2) {
            z = true;
        }
        IS_FOLD = z;
        PRODUCT_NAME = SystemProperties.get("ro.product.device", "unknow");
    }

    public IHwBinder getExtDaemon() throws RemoteException {
        if (this.mExtDaemon == null) {
            IHwBinder service = HwBinder.getService("vendor.xiaomi.hardware.fingerprintextension@1.0::IXiaomiFingerprint", "default");
            this.mExtDaemon = service;
            service.linkToDeath(this.mDeathRecipient, 0L);
        }
        return this.mExtDaemon;
    }

    public boolean isFingerprintClient(BaseClientMonitor client) {
        return client != null && client.statsModality() == 1;
    }

    public int fodCallBack(Context context, int cmd, int param, BaseClientMonitor client) {
        return fodCallBack(context, cmd, param, "", client);
    }

    /* JADX WARN: Can't wrap try/catch for region: R(15:21|(3:23|(1:(1:26)(4:(1:28)(1:29)|30|(1:32)(1:33)|34))|(2:36|(4:45|(2:47|(2:49|52))(1:50)|51|52)(3:42|43|44))(1:53))(10:55|(3:60|72|(1:(3:(2:82|83)(1:84)|85|(1:87))(13:88|89|(2:91|(2:103|(4:105|(1:107)|108|(1:110)))(4:97|(1:99)|100|(1:102)))|111|112|128|113|(1:115)(2:116|117)|118|119|123|124|125))(3:77|78|79))|63|64|(1:66)(1:67)|68|(1:70)(1:71)|72|(1:74)|(0)(0))|54|89|(0)|111|112|128|113|(0)(0)|118|119|123|124|125) */
    /* JADX WARN: Code restructure failed: missing block: B:120:0x01fe, code lost:
        r0 = move-exception;
     */
    /* JADX WARN: Code restructure failed: missing block: B:121:0x01ff, code lost:
        android.util.Slog.e(com.android.server.biometrics.BiometricServiceBaseStubImpl.TAG, "fodCallBack failed, " + r0);
     */
    /* JADX WARN: Code restructure failed: missing block: B:122:0x0217, code lost:
        r0.recycle();
     */
    /* JADX WARN: Code restructure failed: missing block: B:62:0x00c8, code lost:
        r2 = setTouchMode(0, 10, r0);
     */
    /* JADX WARN: Removed duplicated region for block: B:115:0x01d2 A[Catch: all -> 0x01fc, RemoteException -> 0x01fe, TryCatch #0 {RemoteException -> 0x01fe, blocks: (B:113:0x01cc, B:115:0x01d2, B:116:0x01da), top: B:128:0x01cc, outer: #1 }] */
    /* JADX WARN: Removed duplicated region for block: B:116:0x01da A[Catch: all -> 0x01fc, RemoteException -> 0x01fe, TRY_LEAVE, TryCatch #0 {RemoteException -> 0x01fe, blocks: (B:113:0x01cc, B:115:0x01d2, B:116:0x01da), top: B:128:0x01cc, outer: #1 }] */
    /* JADX WARN: Removed duplicated region for block: B:81:0x0124  */
    /* JADX WARN: Removed duplicated region for block: B:88:0x0144  */
    /* JADX WARN: Removed duplicated region for block: B:91:0x014a A[Catch: all -> 0x027c, TryCatch #2 {, blocks: (B:5:0x000c, B:7:0x0014, B:9:0x0018, B:13:0x0020, B:17:0x0036, B:21:0x0040, B:23:0x0049, B:26:0x0053, B:28:0x005d, B:33:0x0068, B:34:0x006a, B:36:0x006e, B:42:0x0078, B:45:0x0081, B:52:0x0092, B:55:0x00b1, B:57:0x00bb, B:62:0x00c8, B:67:0x00d5, B:68:0x00d7, B:70:0x00db, B:72:0x00ed, B:74:0x00f5, B:77:0x00fc, B:83:0x0129, B:87:0x0134, B:89:0x0146, B:91:0x014a, B:97:0x0157, B:99:0x0175, B:100:0x0188, B:102:0x018c, B:103:0x0197, B:105:0x019d, B:107:0x01a1, B:108:0x01b4, B:110:0x01b8, B:111:0x01c2, B:118:0x01f5, B:119:0x01f8, B:122:0x0217, B:123:0x021b, B:113:0x01cc, B:115:0x01d2, B:116:0x01da, B:121:0x01ff), top: B:129:0x000c }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public synchronized int fodCallBack(android.content.Context r18, int r19, int r20, java.lang.String r21, com.android.server.biometrics.sensors.BaseClientMonitor r22) {
        /*
            Method dump skipped, instructions count: 639
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.biometrics.BiometricServiceBaseStubImpl.fodCallBack(android.content.Context, int, int, java.lang.String, com.android.server.biometrics.sensors.BaseClientMonitor):int");
    }

    public void setCurrentLockoutMode(int lockoutMode) {
        this.mLockoutMode = lockoutMode;
        if (mIsFodEngineEnabled) {
            startExtCmd(5, lockoutMode);
        }
    }

    public int isIndia() {
        return ("INDIA".equalsIgnoreCase(RO_BOOT_HWC) || "IN".equalsIgnoreCase(RO_BOOT_HWC) || PadManager.getInstance().isPad() || (IS_FOLD && !"cetus".equals(PRODUCT_NAME))) ? 1 : 0;
    }

    public int getFingerprintUnlockType(Context context) {
        if (!this.IS_POWERFP) {
            return -1;
        }
        int i = this.mSideFpUnlockType;
        if (i != -1) {
            return i;
        }
        int i2 = 0;
        if (Settings.Secure.getIntForUser(context.getContentResolver(), MiuiSettings.Secure.FINGERPRINT_UNLOCK_TYPE, isIndia(), 0) == 1) {
            i2 = 1;
        }
        int unlockType = i2;
        return unlockType;
    }

    public boolean getIsFod() {
        return this.IS_FOD;
    }

    public boolean getIsPowerfp() {
        return this.IS_POWERFP;
    }

    public void registerSideFpUnlockTypeChangedObserver(Handler mHandler, final Context context) {
        ContentObserver observer = new ContentObserver(mHandler) { // from class: com.android.server.biometrics.BiometricServiceBaseStubImpl.2
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                BiometricServiceBaseStubImpl.this.mSideFpUnlockType = Settings.Secure.getIntForUser(context.getContentResolver(), MiuiSettings.Secure.FINGERPRINT_UNLOCK_TYPE, BiometricServiceBaseStubImpl.this.isIndia(), 0);
            }
        };
        context.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(MiuiSettings.Secure.FINGERPRINT_UNLOCK_TYPE), false, observer);
    }

    public int startExtCmd(int cmd, int param) {
        int result = -1;
        if (this.mExtDaemon == null) {
            try {
                this.mExtDaemon = getExtDaemon();
            } catch (Exception e) {
                Slog.e(TAG, "getExtDaemon failed");
            }
        }
        if (this.mExtDaemon != null) {
            HwParcel hidl_reply = new HwParcel();
            try {
                try {
                    HwParcel hidl_request = new HwParcel();
                    hidl_request.writeInterfaceToken("vendor.xiaomi.hardware.fingerprintextension@1.0::IXiaomiFingerprint");
                    hidl_request.writeInt32(cmd);
                    hidl_request.writeInt32(param);
                    this.mExtDaemon.transact(1, hidl_request, hidl_reply, 0);
                    hidl_reply.verifySuccess();
                    hidl_request.releaseTemporaryStorage();
                    result = hidl_reply.readInt32();
                } catch (RemoteException | NoSuchElementException e2) {
                    Slog.e(TAG, "extCmd failed, reset mExtDaemon. ", e2);
                    this.mExtDaemon = null;
                }
            } finally {
                hidl_reply.release();
            }
        }
        Slog.i(TAG, "startExtCmd cmd: " + cmd + " param: " + param + " result:" + result);
        return result;
    }

    public boolean setTouchMode(int touchId, int mode, int value) {
        try {
            ITouchFeature touchFeature = ITouchFeature.getInstance();
            if (touchFeature != null) {
                return touchFeature.setTouchMode(touchId, mode, value);
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public int getStatus4Touch(int cmd, int param) {
        switch (cmd) {
            case 1:
                return 1;
            case 2:
            case 6:
            case 8:
                return 0;
            case 3:
                if (param == 0) {
                    return 1;
                }
                return 0;
            case 4:
                if (this.mLockoutMode == 0) {
                    return 0;
                }
                return 3;
            case 5:
                return 3;
            case 7:
                return 2;
            case 9:
                if (param == 0) {
                    return 0;
                }
                return 2;
            case 11:
                return 100;
            case 400001:
                return 1;
            case 400004:
            case 400006:
                return 0;
            default:
                return -1;
        }
    }

    private boolean isFodMonitorState(int sf_status) {
        switch (sf_status) {
            case 1:
            case 3:
            case 5:
                return true;
            case 2:
            case 4:
            default:
                return false;
        }
    }

    private int getStatus4SurfaceFlinger(int cmd, int param, BaseClientMonitor client) {
        int res = -1;
        switch (cmd) {
            case 1:
                return 3;
            case 2:
            case 6:
                return 4;
            case 3:
                if (param == 0) {
                    return 3;
                }
                return 4;
            case 4:
                if (client == null) {
                    return -1;
                }
                if (client.getStatsAction() == 1) {
                    res = 2;
                }
                if (client.getStatsAction() == 2) {
                    return 4;
                }
                return res;
            case 5:
                return 0;
            case 7:
                return 1;
            case 8:
                return 2;
            case 9:
                if (param == 0) {
                    return 2;
                }
                return 1;
            case 400001:
                return 5;
            case 400004:
            case 400006:
                return 6;
            default:
                return -1;
        }
    }

    private int notifySurfaceFlinger(Context context, int msg, String packName, int value, int cmd) {
        int resBack = -1;
        int packageType = 0;
        if (!this.IS_FOD) {
            return -1;
        }
        if (isKeyguard(packName) && cmd == 1) {
            packageType = 1;
        }
        if (mSfValue == value && mSfPackageType == packageType) {
            Slog.i(TAG, "duplicate notifySurfaceFlinger msg: " + msg + ", value: " + value + ", packageType: " + packageType);
            return -1;
        }
        IBinder flinger = ServiceManager.getService("SurfaceFlinger");
        if (flinger != null) {
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            data.writeInterfaceToken("android.ui.ISurfaceComposer");
            data.writeInt(value);
            data.writeInt(packageType);
            try {
                try {
                    flinger.transact(msg, data, reply, 0);
                    reply.readException();
                    resBack = reply.readInt();
                } catch (RemoteException ex) {
                    Slog.e(TAG, "Failed to notifySurfaceFlinger", ex);
                }
            } finally {
                data.recycle();
                reply.recycle();
            }
        }
        Slog.i(TAG, "notifySurfaceFlinger msg: " + msg + ", value: " + value + ", packageType: " + packageType);
        mSfValue = value;
        mSfPackageType = packageType;
        return resBack;
    }

    public IBinder getFodServ() throws RemoteException {
        IBinder iBinder;
        synchronized (this.mFodLock) {
            if (this.mFodService == null) {
                IBinder service = ServiceManager.getService("android.app.fod.ICallback");
                this.mFodService = service;
                if (service != null) {
                    service.linkToDeath(new IBinder.DeathRecipient() { // from class: com.android.server.biometrics.BiometricServiceBaseStubImpl$$ExternalSyntheticLambda0
                        @Override // android.os.IBinder.DeathRecipient
                        public final void binderDied() {
                            BiometricServiceBaseStubImpl.this.m507x5401c58e();
                        }
                    }, 0);
                }
            }
            iBinder = this.mFodService;
        }
        return iBinder;
    }

    /* renamed from: lambda$getFodServ$0$com-android-server-biometrics-BiometricServiceBaseStubImpl */
    public /* synthetic */ void m507x5401c58e() {
        synchronized (this.mFodLock) {
            Slog.e(TAG, "fodCallBack Service Died.");
            this.mFodService = null;
        }
    }

    public String getCmdStr(int cmd) {
        switch (cmd) {
            case 1:
                return "CMD_APP_AUTHEN";
            case 2:
                return "CMD_APP_CANCEL_AUTHEN";
            case 3:
                return "CMD_VENDOR_AUTHENTICATED";
            case 4:
                return "CMD_VENDOR_ERROR";
            case 5:
                return "CMD_FW_LOCK_CANCEL";
            case 6:
                return "CMD_FW_TOP_APP_CANCEL";
            case 7:
                return "CMD_APP_ENROLL";
            case 8:
                return "CMD_APP_CANCEL_ENROLL";
            case 9:
                return "CMD_VENDOR_ENROLL_RES";
            case 10:
            default:
                return "unknown";
            case 11:
                return "CMD_VENDOR_REMOVED";
        }
    }

    /* loaded from: classes.dex */
    public static abstract class AuthenticationFidoCallback extends FingerprintManager.AuthenticationCallback {
        public void onAuthenticationFidoSucceeded(FingerprintManager.AuthenticationResult result, FingerprintFidoOut fidoOut) {
        }
    }

    protected boolean isKeyguard(String clientPackage) {
        return "com.android.systemui".equals(clientPackage);
    }
}
