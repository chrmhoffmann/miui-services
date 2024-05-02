package com.android.server.biometrics.sensors.fingerprint;

import android.app.ActivityThread;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.util.Slog;
import com.android.server.MiuiBatteryStatsService;
import com.android.server.MiuiBgThread;
import com.miui.base.MiuiStubRegistry;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import miui.mqsas.sdk.MQSEventManagerDelegate;
import miui.os.Build;
import miui.util.FeatureParser;
import org.json.JSONException;
import org.json.JSONObject;
/* loaded from: classes.dex */
public class FingerprintServiceInjectorStubImpl implements FingerprintServiceInjectorStub {
    private static final int FLAG_NON_ANONYMOUS = 2;
    private static final int HBM = 0;
    private static final int LOW_BRIGHT = 1;
    private static final int RECORD_FAILED = 2;
    private static final int RECORD_PERFORM = 1;
    private static final String TAG = "FingerprintServiceInjectorStubImpl";
    private static Context mContext;
    private static boolean mUploadHasAuthToUnlock;
    private static long sAuthToUnlock;
    private static long sCaptureToAuth;
    private static String sClientName;
    private static int sFingerDownCount;
    private static long sFingerDownTime;
    private static long sFingerDownToCapture;
    private static int sFingerUnlockBright;
    private static boolean sIsScreenOn;
    private static boolean sIsSupportNewFormat;
    private static long sOnAuthenTime;
    private static PowerManager sPowerManager;
    private static int sStartCaptureCount;
    private static long sStartCaptureTime;
    private static long sUnlockDoneTime;
    private static int sRecordFeature = -1;
    private static boolean IS_RECORD_PERFORM = false;
    private static boolean IS_RECORD_SENSORINFO = false;
    private static SimpleDateFormat sSdfTime = new SimpleDateFormat("yyyyMMdd-HH:mm:ss");
    private static StringBuffer sStrBuf = new StringBuffer();
    private static boolean IS_FOD = SystemProperties.getBoolean("ro.hardware.fp.fod", false);
    private static boolean IS_POWERFP = SystemProperties.getBoolean("ro.hardware.fp.sideCap", false);
    private static String FP_VENDOR = SystemProperties.get("persist.vendor.sys.fp.vendor", "");
    private static String RO_BOOT_HWC = SystemProperties.get("ro.boot.hwc", "");
    private static int sUnlockOption = -1;
    private static int sEnrolledCount = 0;
    private static String EVENT_NAME = "FingerprintUnlockInfo";
    private static String APP_ID = "31000000080";
    private static String ONETRACK_ACTION = MiuiBatteryStatsService.TrackBatteryUsbInfo.ACTION_TRACK_EVENT;
    private static String ONETRACK_PACKAGE_NAME = MiuiBatteryStatsService.TrackBatteryUsbInfo.ANALYTICS_PACKAGE;
    private static String KEY_FP_VENDOR = "key_fp_vendor";
    private static String KEY_FP_TYPE = "key_fp_type";
    private static String KEY_CLIENT_NAME = "key_client_name";
    private static String KEY_OP_TIME = "key_op_time";
    private static String KEY_SCREEN_STATUS = "key_screen_status";
    private static String KEY_ACQUIRE_INFO = "key_acquire_info";
    private static String KEY_FP_OPTION = "key_fp_option";
    private static String KEY_FP_ENROLLED_COUNT = "key_fp_enrolled_count";
    private static String KEY_OP_RESULT = "key_op_result";
    private static String KEY_FINGER_DOWN_COUNT = "key_finger_down_count";
    private static String KEY_CAPTURE_COUNT = "key_capture_count";
    private static String KEY_UNLOCK_COUNT = "key_unlock_count";
    private static String KEY_DOWN_TO_CAPTURE = "key_down_to_capture";
    private static String KEY_CAPTURE_TO_AUTH = "key_capture_to_auth";
    private static String KEY_AUTH_TO_UNLOCK = "key_auth_to_unlock";
    private static String KEY_AUTH_COUNT = "key_auth_count";
    private static String KEY_BRIGHT_UNLOCK = "KEY_BRIGHT_UNLOCK";

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<FingerprintServiceInjectorStubImpl> {

        /* compiled from: FingerprintServiceInjectorStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final FingerprintServiceInjectorStubImpl INSTANCE = new FingerprintServiceInjectorStubImpl();
        }

        public FingerprintServiceInjectorStubImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public FingerprintServiceInjectorStubImpl provideNewInstance() {
            return new FingerprintServiceInjectorStubImpl();
        }
    }

    public void reportFingerEvent(String packName, int authen) {
        if (!sIsSupportNewFormat) {
            StringBuffer sb = new StringBuffer();
            sb.append(packName).append(",").append(authen);
            if (IS_RECORD_SENSORINFO && sStrBuf.length() > 1) {
                sb.append(sStrBuf);
            }
            sb.append(",").append(sSdfTime.format(Calendar.getInstance().getTime()));
            Slog.d(TAG, "info:" + sb.toString());
            MQSEventManagerDelegate.getInstance().reportSimpleEvent(16, sb.toString());
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put(KEY_CLIENT_NAME, packName);
                jsonObject.put(KEY_OP_RESULT, authen);
                jsonObject.put(KEY_AUTH_COUNT, 1);
                if (IS_RECORD_SENSORINFO && sStrBuf.length() > 1) {
                    jsonObject.put(KEY_ACQUIRE_INFO, sStrBuf.toString());
                }
                jsonObject.put(KEY_OP_TIME, sSdfTime.format(Calendar.getInstance().getTime()));
                startOneTrackUpload(jsonObject);
            } catch (JSONException e) {
                Slog.e(TAG, "reportFingeprintEvent build jsonObject error ", e);
            }
            initAcquiredInfo();
            return;
        }
        sOnAuthenTime = Calendar.getInstance().getTimeInMillis();
        sClientName = packName;
        if (authen == 0) {
            startFailUpload(packName);
        }
    }

    public void startFailUpload(String pkgName) {
        StringBuffer sb = new StringBuffer();
        sb.append(pkgName).append(",").append(0);
        if (IS_RECORD_SENSORINFO && sStrBuf.length() > 1) {
            sb.append(sStrBuf);
        }
        startCalculate(0);
        sb.append(",").append(sFingerDownCount).append(",").append(sStartCaptureCount);
        sb.append(",").append(sFingerDownToCapture).append(",").append(sCaptureToAuth);
        sb.append(",").append(sFingerUnlockBright);
        sb.append(",").append(sIsScreenOn);
        sb.append(",").append(sSdfTime.format(Calendar.getInstance().getTime()));
        Slog.d(TAG, "fail info:" + sb.toString());
        MQSEventManagerDelegate.getInstance().reportSimpleEvent(16, sb.toString());
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(KEY_CLIENT_NAME, pkgName);
            jsonObject.put(KEY_OP_RESULT, 0);
            if (IS_RECORD_SENSORINFO && sStrBuf.length() > 1) {
                jsonObject.put(KEY_ACQUIRE_INFO, sStrBuf.toString());
            }
            jsonObject.put(KEY_FINGER_DOWN_COUNT, sFingerDownCount);
            jsonObject.put(KEY_CAPTURE_COUNT, sStartCaptureCount);
            jsonObject.put(KEY_DOWN_TO_CAPTURE, sFingerDownToCapture);
            jsonObject.put(KEY_CAPTURE_TO_AUTH, sCaptureToAuth);
            jsonObject.put(KEY_SCREEN_STATUS, sIsScreenOn);
            jsonObject.put(KEY_UNLOCK_COUNT, 0);
            jsonObject.put(KEY_AUTH_COUNT, 1);
            jsonObject.put(KEY_OP_TIME, sSdfTime.format(Calendar.getInstance().getTime()));
            jsonObject.put(KEY_BRIGHT_UNLOCK, sFingerUnlockBright);
            startOneTrackUpload(jsonObject);
        } catch (JSONException e) {
            Slog.e(TAG, "startFailUpload build jsonObject error ", e);
        }
        initAcquiredInfo();
    }

    public void startSuccessUpload() {
        sUnlockDoneTime = Calendar.getInstance().getTimeInMillis();
        StringBuffer sb = new StringBuffer();
        sb.append(sClientName).append(",").append(1);
        if (IS_RECORD_SENSORINFO && sStrBuf.length() > 1) {
            sb.append(sStrBuf);
        }
        startCalculate(1);
        sb.append(",").append(sFingerDownCount).append(",").append(sStartCaptureCount);
        sb.append(",").append(sFingerDownToCapture).append(",").append(sCaptureToAuth);
        if (mUploadHasAuthToUnlock) {
            sb.append(",").append(sAuthToUnlock);
        }
        sb.append(",").append(sFingerUnlockBright);
        sb.append(",").append(sIsScreenOn);
        sb.append(",").append(sSdfTime.format(Calendar.getInstance().getTime()));
        Slog.d(TAG, "success info:" + sb.toString());
        MQSEventManagerDelegate.getInstance().reportSimpleEvent(16, sb.toString());
        try {
            JSONObject jsonObject = new JSONObject();
            if (IS_RECORD_SENSORINFO && sStrBuf.length() > 1) {
                jsonObject.put(KEY_ACQUIRE_INFO, sStrBuf.toString());
            }
            jsonObject.put(KEY_CLIENT_NAME, sClientName);
            jsonObject.put(KEY_FINGER_DOWN_COUNT, sFingerDownCount);
            jsonObject.put(KEY_CAPTURE_COUNT, sStartCaptureCount);
            jsonObject.put(KEY_DOWN_TO_CAPTURE, sFingerDownToCapture);
            jsonObject.put(KEY_CAPTURE_TO_AUTH, sCaptureToAuth);
            if (mUploadHasAuthToUnlock) {
                jsonObject.put(KEY_AUTH_TO_UNLOCK, sAuthToUnlock);
            }
            jsonObject.put(KEY_SCREEN_STATUS, sIsScreenOn);
            jsonObject.put(KEY_OP_RESULT, 1);
            jsonObject.put(KEY_UNLOCK_COUNT, 1);
            jsonObject.put(KEY_AUTH_COUNT, 1);
            jsonObject.put(KEY_OP_TIME, sSdfTime.format(Calendar.getInstance().getTime()));
            jsonObject.put(KEY_BRIGHT_UNLOCK, sFingerUnlockBright);
            startOneTrackUpload(jsonObject);
        } catch (JSONException e) {
            Slog.e(TAG, "startSuccessUpload build jsonObject error ", e);
        }
        initAcquiredInfo();
    }

    public void startCalculate(int Auth) {
        if (Auth == 1) {
            long j = sStartCaptureTime;
            sFingerDownToCapture = j - sFingerDownTime;
            long j2 = sOnAuthenTime;
            sCaptureToAuth = j2 - j;
            sAuthToUnlock = sUnlockDoneTime - j2;
        }
        if (Auth == 0) {
            long j3 = sStartCaptureTime;
            sFingerDownToCapture = j3 - sFingerDownTime;
            sCaptureToAuth = sOnAuthenTime - j3;
        }
    }

    public void initAcquiredInfo() {
        initRecordFeature();
        if (IS_RECORD_SENSORINFO) {
            StringBuffer stringBuffer = sStrBuf;
            stringBuffer.delete(0, stringBuffer.length());
            sStrBuf.append("##");
        }
        mUploadHasAuthToUnlock = false;
        sFingerDownCount = 0;
        sStartCaptureCount = 0;
        sFingerDownTime = 0L;
        sFingerDownToCapture = 0L;
        sStartCaptureTime = 0L;
        sCaptureToAuth = 0L;
        sOnAuthenTime = 0L;
        sAuthToUnlock = 0L;
        sUnlockDoneTime = 0L;
        sClientName = null;
    }

    public void recordAcquiredInfo(int acquiredInfo, int vendorCode) {
        if (IS_RECORD_SENSORINFO && isSensorInfo(acquiredInfo, vendorCode)) {
            sStrBuf.append("(").append(acquiredInfo).append("-");
            sStrBuf.append(vendorCode).append(")");
        }
        if (sIsSupportNewFormat && IS_RECORD_SENSORINFO && sClientName != null && acquiredInfo == 6 && vendorCode == 19) {
            mUploadHasAuthToUnlock = true;
            startSuccessUpload();
        }
    }

    public boolean isSensorInfo(int acquiredInfo, int vendorCode) {
        if (acquiredInfo == 0) {
            return false;
        }
        if (sIsSupportNewFormat && acquiredInfo == 6) {
            if (vendorCode == 22) {
                sFingerDownCount++;
                sIsScreenOn = sPowerManager.isInteractive();
                sFingerDownTime = Calendar.getInstance().getTimeInMillis();
            }
            if (vendorCode == 20 || vendorCode == 50) {
                sStartCaptureCount++;
                sStartCaptureTime = Calendar.getInstance().getTimeInMillis();
                sFingerUnlockBright = vendorCode == 20 ? 0 : 1;
            }
        }
        return acquiredInfo != 6 || vendorCode < 19 || vendorCode > 23;
    }

    public void initRecordFeature() {
        sIsSupportNewFormat = supportNewFormat();
        if (sPowerManager == null) {
            sPowerManager = (PowerManager) ActivityThread.currentApplication().getSystemService("power");
        }
        if (sRecordFeature == -1) {
            boolean z = false;
            int integer = FeatureParser.getInteger("type_mqs_finger_record", 0);
            sRecordFeature = integer;
            IS_RECORD_PERFORM = (integer & 1) == 1;
            if ((integer & 2) == 2) {
                z = true;
            }
            IS_RECORD_SENSORINFO = z;
            Slog.d(TAG, "feature:" + sRecordFeature + " " + IS_RECORD_PERFORM + " " + IS_RECORD_SENSORINFO + " " + sIsSupportNewFormat + " " + IS_FOD);
        }
    }

    public boolean supportNewFormat() {
        return Resources.getSystem().getBoolean(285540446);
    }

    public void recordFpTypeAndEnrolledCount(Context context, int unlockType, int count) {
        mContext = context;
        sUnlockOption = unlockType;
        sEnrolledCount = count;
    }

    public void recordActivityVisible() {
        if (!mUploadHasAuthToUnlock && sClientName != null) {
            startSuccessUpload();
        }
    }

    public void startOneTrackUpload(final JSONObject jsonObject) {
        if (mContext == null || "INDIA".equalsIgnoreCase(RO_BOOT_HWC)) {
            return;
        }
        final Intent intent = new Intent(ONETRACK_ACTION);
        intent.setPackage(ONETRACK_PACKAGE_NAME);
        intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_APP_ID, APP_ID);
        intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_EVENT_NAME, EVENT_NAME);
        intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_PACKAGE, mContext.getPackageName());
        String fp_type = IS_POWERFP ? "powerFP" : "backFP";
        intent.putExtra(KEY_FP_TYPE, IS_FOD ? "fod" : fp_type).putExtra(KEY_FP_VENDOR, FP_VENDOR).putExtra(KEY_FP_OPTION, IS_POWERFP ? String.valueOf(sUnlockOption) : "null").putExtra(KEY_FP_ENROLLED_COUNT, sEnrolledCount);
        MiuiBgThread.getHandler().post(new Runnable() { // from class: com.android.server.biometrics.sensors.fingerprint.FingerprintServiceInjectorStubImpl$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                FingerprintServiceInjectorStubImpl.lambda$startOneTrackUpload$0(jsonObject, intent);
            }
        });
    }

    public static /* synthetic */ void lambda$startOneTrackUpload$0(JSONObject jsonObject, Intent intent) {
        try {
            Iterator iterator = jsonObject.keys();
            while (iterator.hasNext()) {
                String key = iterator.next();
                String value = jsonObject.getString(key);
                intent.putExtra(key, value);
            }
            if (!Build.IS_INTERNATIONAL_BUILD) {
                intent.setFlags(2);
            }
            mContext.startServiceAsUser(intent, UserHandle.CURRENT);
        } catch (IllegalStateException e) {
            Slog.w(TAG, "Failed to upload FingerprintService event.");
        } catch (SecurityException e2) {
            Slog.w(TAG, "Unable to start service.");
        } catch (JSONException e3) {
            Slog.e(TAG, "startOneTrackUpload build jsonObject error ", e3);
        }
    }
}
