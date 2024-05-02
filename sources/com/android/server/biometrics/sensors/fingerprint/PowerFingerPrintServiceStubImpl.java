package com.android.server.biometrics.sensors.fingerprint;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.HwBinder;
import android.os.HwParcel;
import android.os.IHwBinder;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Slog;
import com.android.server.biometrics.sensors.BaseClientMonitor;
import com.android.server.biometrics.sensors.fingerprint.PowerFingerPrintServiceStub;
import com.miui.base.MiuiStubRegistry;
import java.util.NoSuchElementException;
import miui.util.FeatureParser;
/* loaded from: classes.dex */
public class PowerFingerPrintServiceStubImpl implements PowerFingerPrintServiceStub {
    private static final int CODE_EXT_CMD = 1;
    private static final String EXT_DESCRIPTOR = "vendor.xiaomi.hardware.fingerprintextension@1.0::IXiaomiFingerprint";
    private static final int FINGERPRINT_CMD_LOCKOUT_MODE = 12;
    private static final String NAME_EXT_DAEMON = "vendor.xiaomi.hardware.fingerprintextension@1.0::IXiaomiFingerprint";
    protected static final int POWERFP_DISABLE_NAVIGATION = 0;
    protected static final int POWERFP_ENABLE_LOCK_KEY = 1;
    protected static final int POWERFP_ENABLE_NAVIGATION = 2;
    private static final String TAG = "PowerFingerPrintServiceStubImpl";
    private static String mDoubleTapSideFp;
    private PowerFingerPrintServiceStub.ChangeListener listener;
    private IHwBinder mExtDaemon;
    private long interceptPowerKeyTimeByFinger = -1;
    private boolean interceptPowerKeyAuthOrEnroll = false;
    private boolean IS_POWERFP = SystemProperties.getBoolean("ro.hardware.fp.sideCap", false);
    private boolean mIsPowerKeyDown = false;
    private boolean dealOnChange = false;
    private boolean IS_SUPPORT_FINGERPRINT_TAP = FeatureParser.getBoolean("is_support_fingerprint_tap", false);
    private final String DOUBLE_TAP_SIDE_FP = "fingerprint_double_tap";

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<PowerFingerPrintServiceStubImpl> {

        /* compiled from: PowerFingerPrintServiceStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final PowerFingerPrintServiceStubImpl INSTANCE = new PowerFingerPrintServiceStubImpl();
        }

        public PowerFingerPrintServiceStubImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public PowerFingerPrintServiceStubImpl provideNewInstance() {
            return new PowerFingerPrintServiceStubImpl();
        }
    }

    public boolean getIsPowerfp() {
        return this.IS_POWERFP;
    }

    public void setInterceptPowerKeyAuthOrEnroll(boolean start) {
        this.interceptPowerKeyAuthOrEnroll = start;
    }

    public boolean getInterceptPowerKeyAuthOrEnroll() {
        return this.interceptPowerKeyAuthOrEnroll;
    }

    public void setInterceptPowerKeyTimeByFinger(long time) {
        this.interceptPowerKeyTimeByFinger = time;
    }

    public long getInterceptPowerKeyTimeByFinger() {
        return this.interceptPowerKeyTimeByFinger;
    }

    public void setChangeListener(PowerFingerPrintServiceStub.ChangeListener listener) {
        Slog.d(TAG, "setChangeListener");
        this.listener = listener;
    }

    public void setDealOnChange(boolean value) {
        Slog.d(TAG, "setDealOnChange:" + (value ? "true" : "false"));
        this.dealOnChange = value;
    }

    public boolean getDealOnChange() {
        return this.dealOnChange;
    }

    public void setIsPowerKeyDown(boolean isPowerKeyDown) {
        Slog.d(TAG, "setIsPowerKeyDown:" + (isPowerKeyDown ? "true" : "false"));
        this.mIsPowerKeyDown = isPowerKeyDown;
        PowerFingerPrintServiceStub.ChangeListener changeListener = this.listener;
        if (changeListener != null) {
            changeListener.onChange(this.dealOnChange);
        }
    }

    public boolean getIsPowerKeyDown() {
        return this.mIsPowerKeyDown;
    }

    public boolean getIsSupportFpTap() {
        return this.IS_SUPPORT_FINGERPRINT_TAP;
    }

    public void registerDoubleTapSideFpOptionObserver(final Context mContext, Handler mHandler, final BaseClientMonitor mClient) {
        String doubleTapSideFpOption = getDoubleTapSideFpOption(mContext);
        mDoubleTapSideFp = doubleTapSideFpOption;
        if (TextUtils.isEmpty(doubleTapSideFpOption) || "none".equalsIgnoreCase(mDoubleTapSideFp)) {
            NotifyLockOutState(mContext, mClient, 0, 0);
        } else {
            NotifyLockOutState(mContext, mClient, 0, 2);
        }
        ContentObserver observer = new ContentObserver(mHandler) { // from class: com.android.server.biometrics.sensors.fingerprint.PowerFingerPrintServiceStubImpl.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                PowerFingerPrintServiceStubImpl.mDoubleTapSideFp = PowerFingerPrintServiceStubImpl.this.getDoubleTapSideFpOption(mContext);
                if (!"none".equals(PowerFingerPrintServiceStubImpl.mDoubleTapSideFp)) {
                    PowerFingerPrintServiceStubImpl.this.NotifyLockOutState(mContext, mClient, 0, 2);
                } else {
                    PowerFingerPrintServiceStubImpl.this.NotifyLockOutState(mContext, mClient, 0, 0);
                }
            }
        };
        mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("fingerprint_double_tap"), false, observer, -1);
    }

    public String getDoubleTapSideFpOption(Context mContext) {
        if (!this.IS_POWERFP || !this.IS_SUPPORT_FINGERPRINT_TAP) {
            return null;
        }
        String dobuleTap = Settings.System.getStringForUser(mContext.getContentResolver(), "fingerprint_double_tap", -2);
        return dobuleTap;
    }

    public void NotifyLockOutState(Context mContext, BaseClientMonitor mClient, int lockoutMode, int param) {
        if (!getIsPowerfp()) {
            return;
        }
        if (mClient != null && mClient.statsModality() == 1 && !mClient.isAlreadyDone() && lockoutMode == 0 && mClient.getStatsAction() != 0) {
            Slog.w(TAG, "mCurrentClient.statsAction() " + mClient.statsModality() + "mCurrentClient.isAlreadyDone() " + mClient.isAlreadyDone() + "mCurrentClient.statsAction() " + mClient.getStatsAction() + "lockoutMode " + lockoutMode);
        } else if (param == 2 && !this.IS_SUPPORT_FINGERPRINT_TAP) {
            startExtCmd(12, 0);
        } else if (param == 2 && (TextUtils.isEmpty(getDoubleTapSideFpOption(mContext)) || "none".equalsIgnoreCase(getDoubleTapSideFpOption(mContext)))) {
            startExtCmd(12, 0);
        } else {
            startExtCmd(12, param);
        }
    }

    private int startExtCmd(int cmd, int param) {
        int result = -1;
        HwParcel hidl_reply = new HwParcel();
        try {
            try {
                if (this.mExtDaemon == null) {
                    this.mExtDaemon = HwBinder.getService("vendor.xiaomi.hardware.fingerprintextension@1.0::IXiaomiFingerprint", "default");
                }
                if (this.mExtDaemon == null) {
                    Slog.e(TAG, "startExtCmd: mExtDaemon service not found");
                } else {
                    HwParcel hidl_request = new HwParcel();
                    hidl_request.writeInterfaceToken("vendor.xiaomi.hardware.fingerprintextension@1.0::IXiaomiFingerprint");
                    hidl_request.writeInt32(cmd);
                    hidl_request.writeInt32(param);
                    this.mExtDaemon.transact(1, hidl_request, hidl_reply, 0);
                    hidl_reply.verifySuccess();
                    hidl_request.releaseTemporaryStorage();
                    result = hidl_reply.readInt32();
                }
            } catch (RemoteException | NoSuchElementException e) {
                Slog.e(TAG, "extCmd failed, reset mExtDaemon. ", e);
                this.mExtDaemon = null;
            }
            hidl_reply.release();
            Slog.i(TAG, "startExtCmd cmd: " + cmd + " param: " + param + " result:" + result);
            return result;
        } catch (Throwable th) {
            hidl_reply.release();
            throw th;
        }
    }
}
