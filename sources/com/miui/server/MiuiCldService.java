package com.miui.server;

import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.HwBinder;
import android.os.HwParcel;
import android.os.IHwBinder;
import android.util.Slog;
import com.android.server.DarkModeOneTrackHelper;
import java.util.Date;
import miui.hardware.ICldCallback;
import miui.hardware.ICldManager;
/* loaded from: classes.dex */
public class MiuiCldService extends ICldManager.Stub {
    private static final int CLD_OPER_ABORTED = 3;
    private static final int CLD_OPER_DONE = 2;
    private static final int CLD_OPER_FATAL_ERR = 4;
    private static final int CLD_OPER_NOT_SUPPORTED = 0;
    private static final int CLD_OPER_PROCESSING = 1;
    private static final int FRAG_ANALYSIS = 0;
    private static final int FRAG_LEVEL_LOW = 1;
    private static final int FRAG_LEVEL_MEDIUM = 2;
    private static final int FRAG_LEVEL_SERVERE = 3;
    private static final int FRAG_LEVEL_UNKNOWN = 4;
    private static final int GET_CLD_OPERATION_STATUS = 4;
    private static final int GET_FRAGMENT_LEVEL = 2;
    private static final long HALF_A_DAY_MS = 43200000;
    private static final String HAL_DEFAULT = "default";
    private static final String HAL_INTERFACE_DESCRIPTOR = "vendor.xiaomi.hardware.cld@1.0::ICld";
    private static final String HAL_SERVICE_NAME = "vendor.xiaomi.hardware.cld@1.0::ICld";
    private static final int IS_CLD_SUPPORTED = 1;
    private static final String MIUI_CLD_PROCESSED_DONE = "miui.intent.action.CLD_PROCESSED_DONE";
    public static final String SERVICE_NAME = "miui.cld.service";
    private static final int SET_CALLBACK = 5;
    private static final String TAG = MiuiCldService.class.getSimpleName();
    private static final int TRIGGER_CLD = 3;
    private final Context mContext;
    private HALCallback mHALCallback;
    private Date sLastCld = new Date(0);

    private String getCallerProcessName() {
        int uid = Binder.getCallingUid();
        String callingApp = this.mContext.getPackageManager().getNameForUid(uid);
        Slog.d(TAG, "callingApp: " + callingApp);
        if (callingApp != null) {
            return callingApp;
        }
        return "";
    }

    public MiuiCldService(Context context) {
        this.mContext = context;
        initCallback();
    }

    public void reportCldProcessedBroadcast(int level) {
        Intent intent = new Intent(MIUI_CLD_PROCESSED_DONE);
        intent.putExtra(DarkModeOneTrackHelper.EVENT_NAME_STATUS, Integer.toString(level));
        Slog.d(TAG, "Send CLD broadcast, status = " + level);
        this.mContext.sendBroadcast(intent);
    }

    private void initCallback() {
        this.mHALCallback = new HALCallback(this);
        HwParcel hidl_reply = new HwParcel();
        try {
            try {
                IHwBinder hwService = HwBinder.getService("vendor.xiaomi.hardware.cld@1.0::ICld", HAL_DEFAULT);
                if (hwService != null) {
                    HwParcel hidl_request = new HwParcel();
                    hidl_request.writeInterfaceToken("vendor.xiaomi.hardware.cld@1.0::ICld");
                    hidl_request.writeStrongBinder(this.mHALCallback.asBinder());
                    hwService.transact(5, hidl_request, hidl_reply, 0);
                    hidl_reply.verifySuccess();
                    hidl_request.releaseTemporaryStorage();
                    return;
                }
            } catch (Exception e) {
                Slog.e(TAG, "Transaction failed: " + e);
            }
            Slog.e(TAG, "initCallback failed.");
        } finally {
            hidl_reply.release();
        }
    }

    public boolean isCldSupported() {
        HwParcel hidl_reply = new HwParcel();
        try {
            try {
                IHwBinder hwService = HwBinder.getService("vendor.xiaomi.hardware.cld@1.0::ICld", HAL_DEFAULT);
                if (hwService != null) {
                    HwParcel hidl_request = new HwParcel();
                    hidl_request.writeInterfaceToken("vendor.xiaomi.hardware.cld@1.0::ICld");
                    hwService.transact(1, hidl_request, hidl_reply, 0);
                    hidl_reply.verifySuccess();
                    hidl_request.releaseTemporaryStorage();
                    int val = hidl_reply.readInt32();
                    if (val == 0) {
                        Slog.e(TAG, "CLD not supported on current device!");
                        return false;
                    }
                    Slog.i(TAG, "CLD supported.");
                    return true;
                }
            } catch (Exception e) {
                Slog.e(TAG, "Transaction failed: " + e);
            }
            Slog.e(TAG, "Failed calling isCldSupported.");
            return false;
        } finally {
            hidl_reply.release();
        }
    }

    public int getFragmentLevel() {
        HwParcel hidl_reply = new HwParcel();
        try {
            try {
                IHwBinder hwService = HwBinder.getService("vendor.xiaomi.hardware.cld@1.0::ICld", HAL_DEFAULT);
                if (hwService != null) {
                    HwParcel hidl_request = new HwParcel();
                    hidl_request.writeInterfaceToken("vendor.xiaomi.hardware.cld@1.0::ICld");
                    hwService.transact(2, hidl_request, hidl_reply, 0);
                    hidl_reply.verifySuccess();
                    hidl_request.releaseTemporaryStorage();
                    int val = hidl_reply.readInt32();
                    if (val >= 0 && val <= 4) {
                        return val;
                    }
                    Slog.e(TAG, "Got invalid fragment level: " + val);
                    return 4;
                }
            } catch (Exception e) {
                Slog.e(TAG, "Transaction failed: " + e);
            }
            Slog.e(TAG, "Failed calling getFragmentLevel.");
            return 4;
        } finally {
            hidl_reply.release();
        }
    }

    public void triggerCld(int val) {
        Date now = new Date();
        if (now.getTime() - this.sLastCld.getTime() < HALF_A_DAY_MS) {
            Slog.w(TAG, "Less than half a day before last defragmentation!");
        }
        this.sLastCld = now;
        if (!getCallerProcessName().equals("com.miui.cleanmaster") && !getCallerProcessName().equals("com.miui.cleaner")) {
            Slog.d(TAG, "callingApp is not com.miui.cleanmaster or com.miui.cleaner");
            return;
        }
        HwParcel hidl_reply = new HwParcel();
        try {
            try {
                IHwBinder hwService = HwBinder.getService("vendor.xiaomi.hardware.cld@1.0::ICld", HAL_DEFAULT);
                if (hwService != null) {
                    HwParcel hidl_request = new HwParcel();
                    hidl_request.writeInterfaceToken("vendor.xiaomi.hardware.cld@1.0::ICld");
                    hidl_request.writeInt32(val);
                    hwService.transact(3, hidl_request, hidl_reply, 0);
                    hidl_reply.verifySuccess();
                    hidl_request.releaseTemporaryStorage();
                    return;
                }
            } catch (Exception e) {
                Slog.e(TAG, "Transaction failed: " + e);
            }
            Slog.e(TAG, "Failed calling triggerCld.");
        } finally {
            hidl_reply.release();
        }
    }

    public int getCldOperationStatus() {
        HwParcel hidl_reply = new HwParcel();
        try {
            try {
                IHwBinder hwService = HwBinder.getService("vendor.xiaomi.hardware.cld@1.0::ICld", HAL_DEFAULT);
                if (hwService != null) {
                    HwParcel hidl_request = new HwParcel();
                    hidl_request.writeInterfaceToken("vendor.xiaomi.hardware.cld@1.0::ICld");
                    hwService.transact(4, hidl_request, hidl_reply, 0);
                    hidl_reply.verifySuccess();
                    hidl_request.releaseTemporaryStorage();
                    int val = hidl_reply.readInt32();
                    if (val >= 0 && val <= 4) {
                        return val;
                    }
                    Slog.e(TAG, "Got invalid operation status: " + val);
                    return 0;
                }
            } catch (Exception e) {
                Slog.e(TAG, "Transaction failed: " + e);
            }
            Slog.e(TAG, "Failed calling getCldOperationStatus.");
            return 0;
        } finally {
            hidl_reply.release();
        }
    }

    /* loaded from: classes.dex */
    public static class HALCallback extends ICldCallback.Stub {
        public MiuiCldService cldService;

        HALCallback(MiuiCldService cldService) {
            this.cldService = cldService;
        }

        public void notifyStatusChange(int level) {
            this.cldService.reportCldProcessedBroadcast(level);
        }
    }
}
