package com.android.server;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.HwBinder;
import android.os.HwParcel;
import android.os.IHwBinder;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Settings;
import android.server.am.SplitScreenReporter;
import android.text.TextUtils;
import android.util.Slog;
import com.android.server.pm.CloudControlPreinstallService;
import com.miui.server.SecurityManagerService;
import java.io.BufferedReader;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.List;
import miui.telephony.TelephonyManager;
import miui.util.IMiCharge;
import org.json.JSONException;
import org.json.JSONObject;
/* loaded from: classes.dex */
public class MiuiBatteryAuthentic {
    private static final String BATTERY_AUTHENTIC = "battery_authentic_certificate";
    private static final boolean DEBUG = false;
    private static final String DEFAULT_BATTERT_SN = "111111111111111111111111";
    private static final String DEFAULT_IMEI = "1234567890";
    private static final int DELAY_TIME = 60000;
    private static volatile MiuiBatteryAuthentic INSTANCE = null;
    private static final String PROVISION_COMPLETE_BROADCAST = "android.provision.action.PROVISION_COMPLETE";
    private static final String PUBLIC_KEY = "3059301306072a8648ce3d020106082a8648ce3d030107034200045846fce7eaab1053c62f76cd7c61ae09a8411a5c106cad7a95c11c26dd25e507e963e2ae8f2c9672db92fe9834584dc41996454c8c929fc26e9d512e4096f450";
    private static final String TAG = "MiuiBatteryAuthentic";
    public String mCloudSign;
    public final ContentResolver mContentResolver;
    public Context mContext;
    private IMiCharge mMiCharge = IMiCharge.getInstance();
    public String mImei = null;
    public String mBatterySn = null;
    public boolean mIsVerified = false;
    private BatteryAuthenticHandler mHandler = new BatteryAuthenticHandler(MiuiFgThread.get().getLooper());
    public IMTService mIMTService = new IMTService();

    public static MiuiBatteryAuthentic getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (MiuiBatteryAuthentic.class) {
                if (INSTANCE == null) {
                    INSTANCE = new MiuiBatteryAuthentic(context);
                }
            }
        }
        return INSTANCE;
    }

    public MiuiBatteryAuthentic(Context context) {
        this.mContext = context;
        this.mContentResolver = context.getContentResolver();
        initBatteryAuthenticCertificate();
        BroadcastReceiver receiver = new BroadcastReceiver() { // from class: com.android.server.MiuiBatteryAuthentic.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                if (("android.net.conn.CONNECTIVITY_CHANGE".equals(intent.getAction()) || MiuiBatteryAuthentic.PROVISION_COMPLETE_BROADCAST.equals(intent.getAction())) && !MiuiBatteryAuthentic.this.mIsVerified && MiuiBatteryAuthentic.this.mHandler.isNetworkConnected(context2) && MiuiBatteryAuthentic.this.mHandler.isDeviceProvisioned()) {
                    MiuiBatteryAuthentic.this.mHandler.sendMessageDelayed(0, 0L);
                }
            }
        };
        IntentFilter filter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
        filter.addAction(PROVISION_COMPLETE_BROADCAST);
        context.registerReceiver(receiver, filter);
    }

    private void initBatteryAuthenticCertificate() {
        this.mCloudSign = Settings.System.getString(this.mContentResolver, BATTERY_AUTHENTIC);
        this.mBatterySn = this.mHandler.getBatterySn();
        if (!TextUtils.isEmpty(this.mCloudSign)) {
            this.mHandler.sendMessageDelayed(1, 0L);
            return;
        }
        this.mIsVerified = false;
        this.mHandler.sendMessageDelayed(0, 0L);
    }

    /* loaded from: classes.dex */
    public class BatteryAuthenticHandler extends Handler {
        static final int CONFIRM = 3;
        private static final String CONTENT_TYPE = "Content-Type";
        private static final int DEFAULT_CONNECT_TIME_OUT = 2000;
        private static final int DEFAULT_READ_TIME_OUT = 2000;
        private static final int DELAY_SET_TIME = 5000;
        static final int INIT = 1;
        static final int MSG_BATTERY_AUTHENTIC = 0;
        static final int MSG_SIGN_RESULT = 3;
        static final int MSG_VERITY_SIGN = 1;
        static final int MSG_VERITY_SIGN_CONFIRM = 2;
        private static final String TYPE = "application/json; charset=UTF-8";
        static final int VERIFY = 2;
        private String mAndroidId = null;
        private String mProjectName = null;
        private String mChallenge = null;
        private String mConFirmChallenge = null;
        private String mBatteryAuthentic = null;
        private int mGetFromServerCount = 0;
        private int mConfirmResultCount = 0;
        private int mTryGetBatteryCount = 0;
        private boolean mIsConfirm = false;
        private boolean mIsGetServerInfo = false;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public BatteryAuthenticHandler(Looper looper) {
            super(looper);
            MiuiBatteryAuthentic.this = this$0;
        }

        public void sendMessageDelayed(int what, long delayMillis) {
            removeMessages(what);
            Message m = Message.obtain(this, what);
            sendMessageDelayed(m, delayMillis);
        }

        public void sendMessageDelayed(int what, boolean arg, long delayMillis) {
            removeMessages(what);
            Message m = Message.obtain(this, what);
            m.arg1 = arg ? 1 : 0;
            sendMessageDelayed(m, delayMillis);
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            int i;
            boolean z = false;
            switch (msg.what) {
                case 0:
                    if (isNetworkConnected(MiuiBatteryAuthentic.this.mContext) && isDeviceProvisioned()) {
                        if (MiuiBatteryAuthentic.DEFAULT_BATTERT_SN.equals(MiuiBatteryAuthentic.this.mBatterySn) && this.mTryGetBatteryCount < 10) {
                            sendMessageDelayed(0, 5000L);
                            MiuiBatteryAuthentic.this.mBatterySn = getBatterySn();
                            this.mTryGetBatteryCount++;
                            return;
                        } else if (!verifyFromServer() && this.mGetFromServerCount < 4) {
                            sendMessageDelayed(0, 0L);
                            this.mGetFromServerCount++;
                            return;
                        } else {
                            MiuiBatteryAuthentic.this.mIsVerified = true;
                            if (this.mIsGetServerInfo) {
                                sendMessageDelayed(3, false, 0L);
                                return;
                            }
                            return;
                        }
                    }
                    return;
                case 1:
                    if (TextUtils.isEmpty(MiuiBatteryAuthentic.this.mImei)) {
                        MiuiBatteryAuthentic.this.mImei = getImei();
                    }
                    if (TextUtils.isEmpty(MiuiBatteryAuthentic.this.mBatterySn)) {
                        MiuiBatteryAuthentic.this.mBatterySn = getBatterySn();
                    }
                    if (this.mAndroidId == null) {
                        this.mAndroidId = getAndroidId();
                    }
                    if (verifyECDSA()) {
                        MiuiBatteryAuthentic.this.mIsVerified = true;
                        sendMessageDelayed(3, true, 0L);
                        return;
                    } else if (!MiuiBatteryAuthentic.this.mIsVerified) {
                        MiuiBatteryAuthentic.this.mCloudSign = null;
                        sendMessageDelayed(0, 0L);
                        return;
                    } else {
                        return;
                    }
                case 2:
                    if (isNetworkConnected(MiuiBatteryAuthentic.this.mContext) && isDeviceProvisioned()) {
                        batteryVerifyConfirm();
                    }
                    if (!this.mIsConfirm && (i = this.mConfirmResultCount) < 60) {
                        this.mConfirmResultCount = i + 1;
                        sendMessageDelayed(2, SecurityManagerService.LOCK_TIME_OUT);
                        return;
                    }
                    if (verifyECDSA()) {
                        Settings.System.putString(MiuiBatteryAuthentic.this.mContentResolver, MiuiBatteryAuthentic.BATTERY_AUTHENTIC, MiuiBatteryAuthentic.this.mCloudSign);
                    }
                    this.mConfirmResultCount = 0;
                    return;
                case 3:
                    if (msg.arg1 == 1) {
                        z = true;
                    }
                    boolean success = z;
                    boolean callResult = success ? MiuiBatteryAuthentic.this.mMiCharge.setMiChargePath("server_result", SplitScreenReporter.ACTION_ENTER_SPLIT) : MiuiBatteryAuthentic.this.mMiCharge.setMiChargePath("server_result", "0");
                    if (!callResult) {
                        sendMessageDelayed(3, success, 5000L);
                        return;
                    }
                    return;
                default:
                    Slog.d(MiuiBatteryAuthentic.TAG, "No Message");
                    return;
            }
        }

        private boolean verifyECDSA() {
            try {
                String str = MiuiBatteryAuthentic.this.mBatterySn + ":" + MiuiBatteryAuthentic.this.mImei;
                if (TextUtils.isEmpty(MiuiBatteryAuthentic.this.mCloudSign)) {
                    return false;
                }
                String signString = MiuiBatteryAuthentic.this.mCloudSign;
                byte[] sign = hexStringToByte(signString);
                X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(hexStringToByte(MiuiBatteryAuthentic.PUBLIC_KEY));
                KeyFactory keyFactory = KeyFactory.getInstance("EC");
                PublicKey publicKey = keyFactory.generatePublic(x509EncodedKeySpec);
                Signature signature = Signature.getInstance("SHA256withECDSA");
                signature.initVerify(publicKey);
                signature.update(str.getBytes());
                boolean result = signature.verify(sign);
                return result;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        private boolean verifyFromServer() {
            batteryVerifyInit();
            if (!TextUtils.isEmpty(this.mChallenge)) {
                batteryVerify();
                if (!TextUtils.isEmpty(MiuiBatteryAuthentic.this.mCloudSign)) {
                    sendMessageDelayed(2, 0L);
                    sendMessageDelayed(1, 0L);
                    return true;
                }
                this.mChallenge = null;
                Slog.e(MiuiBatteryAuthentic.TAG, "verify error failed, get sign failed");
                return false;
            }
            Slog.e(MiuiBatteryAuthentic.TAG, "verify error failed, get challenge failed");
            return false;
        }

        private String getAndroidId() {
            String androidID = SystemProperties.get("ro.serialno", "unknown");
            if (!TextUtils.isEmpty(androidID)) {
                return androidID;
            }
            return "";
        }

        private String getProjectName() {
            String device = SystemProperties.get("ro.product.device", "unknown");
            if (!TextUtils.isEmpty(device)) {
                return device;
            }
            return "";
        }

        public String getBatterySn() {
            StringBuilder stringBuilder = new StringBuilder("");
            String str = MiuiBatteryAuthentic.this.mMiCharge.getMiChargePath("server_sn");
            try {
                String[] splitString = str.split(" ");
                for (String s : splitString) {
                    int number = Integer.valueOf(s.substring(2, s.length()), 16).intValue();
                    char c = (char) number;
                    stringBuilder.append(c);
                }
            } catch (Exception e) {
                Slog.e(MiuiBatteryAuthentic.TAG, "getBatterySn failed " + e);
            }
            String result = stringBuilder.toString();
            if (result.length() == 0) {
                return MiuiBatteryAuthentic.DEFAULT_BATTERT_SN;
            }
            if (result.length() >= 24) {
                return result.substring(0, 24);
            }
            return result;
        }

        public String bytesToHexString(byte[] src) {
            StringBuilder stringBuilder = new StringBuilder("");
            if (src == null || src.length <= 0) {
                return null;
            }
            for (byte b : src) {
                int v = b & 255;
                String hv = Integer.toHexString(v);
                if (hv.length() < 2) {
                    stringBuilder.append(0);
                }
                stringBuilder.append(hv);
            }
            return stringBuilder.toString();
        }

        public byte[] hexStringToByte(String hex) {
            int len = hex.length() / 2;
            byte[] result = new byte[len];
            char[] achar = hex.toCharArray();
            for (int i = 0; i < len; i++) {
                int pos = i * 2;
                result[i] = (byte) ((toByte(achar[pos]) << 4) | toByte(achar[pos + 1]));
            }
            return result;
        }

        private byte toByte(char c) {
            byte b = (byte) "0123456789abcdef".indexOf(c);
            return b;
        }

        private String getImei() {
            List<String> imeis = TelephonyManager.getDefault().getImeiList();
            if (imeis != null && !imeis.isEmpty()) {
                for (int i = 0; i < imeis.size(); i++) {
                    if (!TextUtils.isEmpty(imeis.get(i))) {
                        return imeis.get(i);
                    }
                }
                return MiuiBatteryAuthentic.DEFAULT_IMEI;
            }
            return MiuiBatteryAuthentic.DEFAULT_IMEI;
        }

        public boolean isDeviceProvisioned() {
            return Settings.Global.getInt(MiuiBatteryAuthentic.this.mContentResolver, "device_provisioned", 0) != 0;
        }

        public boolean isNetworkConnected(Context context) {
            try {
                ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService("connectivity");
                NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                if (networkInfo == null) {
                    return false;
                }
                boolean isAvailable = networkInfo.isAvailable();
                return isAvailable;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        private String SHA256(String strText) {
            if (TextUtils.isEmpty(strText)) {
                return "";
            }
            try {
                MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
                messageDigest.update(strText.getBytes());
                byte[] byteBuffer = messageDigest.digest();
                String strResult = bytesToHexString(byteBuffer);
                return strResult;
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
                return "";
            } catch (Exception e2) {
                e2.printStackTrace();
                return "";
            }
        }

        public JSONObject collectData(int pushMethod) {
            JSONObject jsonObject = new JSONObject();
            String BEREncoding = null;
            if (pushMethod == 1) {
                if (TextUtils.isEmpty(MiuiBatteryAuthentic.this.mImei) || MiuiBatteryAuthentic.DEFAULT_IMEI.equals(MiuiBatteryAuthentic.this.mImei)) {
                    MiuiBatteryAuthentic.this.mImei = getImei();
                }
                if (TextUtils.isEmpty(MiuiBatteryAuthentic.this.mBatterySn) || MiuiBatteryAuthentic.DEFAULT_BATTERT_SN.equals(MiuiBatteryAuthentic.this.mBatterySn)) {
                    MiuiBatteryAuthentic.this.mBatterySn = getBatterySn();
                }
            }
            if (this.mAndroidId == null) {
                this.mAndroidId = getAndroidId();
            }
            if (this.mProjectName == null) {
                this.mProjectName = getProjectName();
            }
            if (this.mBatteryAuthentic == null) {
                this.mBatteryAuthentic = MiuiBatteryAuthentic.this.mMiCharge.getMiChargePath("authentic");
            }
            try {
                if (pushMethod == 1) {
                    String data = bytesToHexString(MiuiBatteryAuthentic.this.mBatterySn.getBytes());
                    String sign = MiuiBatteryAuthentic.this.mIMTService.eccSign(1, SHA256(MiuiBatteryAuthentic.this.mBatterySn));
                    if (sign != null && sign.length() > 64) {
                        BEREncoding = "30440220" + sign.substring(0, 64) + "0220" + sign.substring(64);
                    }
                    String fid = MiuiBatteryAuthentic.this.mIMTService.getFid();
                    jsonObject.put("fid", fid);
                    jsonObject.put(CloudControlPreinstallService.ConnectEntity.SIGN, BEREncoding);
                    jsonObject.put("content", data);
                } else if (pushMethod == 2) {
                    jsonObject.put("rk", this.mChallenge);
                } else if (pushMethod == 3) {
                    jsonObject.put("rk", this.mConFirmChallenge);
                    jsonObject.put("hostAuthentication", this.mBatteryAuthentic);
                }
                jsonObject.put("imei", MiuiBatteryAuthentic.this.mImei);
                jsonObject.put("sn", MiuiBatteryAuthentic.this.mBatterySn);
                jsonObject.put("androidId", this.mAndroidId);
                jsonObject.put("project", this.mProjectName);
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (Exception e2) {
                e2.printStackTrace();
            }
            return jsonObject;
        }

        /* JADX WARN: Code restructure failed: missing block: B:19:0x00b4, code lost:
            if (r2 != null) goto L32;
         */
        /* JADX WARN: Code restructure failed: missing block: B:31:0x0107, code lost:
            if (0 == 0) goto L33;
         */
        /*
            Code decompiled incorrectly, please refer to instructions dump.
            To view partially-correct add '--show-bad-code' argument
        */
        public void batteryVerify() {
            /*
                Method dump skipped, instructions count: 282
                To view this dump add '--comments-level debug' option
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.server.MiuiBatteryAuthentic.BatteryAuthenticHandler.batteryVerify():void");
        }

        /* JADX WARN: Code restructure failed: missing block: B:16:0x00a0, code lost:
            if (r2 != null) goto L29;
         */
        /* JADX WARN: Code restructure failed: missing block: B:28:0x00f3, code lost:
            if (0 == 0) goto L30;
         */
        /*
            Code decompiled incorrectly, please refer to instructions dump.
            To view partially-correct add '--show-bad-code' argument
        */
        public void batteryVerifyInit() {
            /*
                Method dump skipped, instructions count: 262
                To view this dump add '--comments-level debug' option
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.server.MiuiBatteryAuthentic.BatteryAuthenticHandler.batteryVerifyInit():void");
        }

        /* JADX WARN: Code restructure failed: missing block: B:16:0x00a1, code lost:
            if (r2 != null) goto L29;
         */
        /* JADX WARN: Code restructure failed: missing block: B:28:0x00f4, code lost:
            if (0 == 0) goto L30;
         */
        /*
            Code decompiled incorrectly, please refer to instructions dump.
            To view partially-correct add '--show-bad-code' argument
        */
        public void batteryVerifyConfirm() {
            /*
                Method dump skipped, instructions count: 263
                To view this dump add '--comments-level debug' option
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.server.MiuiBatteryAuthentic.BatteryAuthenticHandler.batteryVerifyConfirm():void");
        }

        private void closeBufferedReader(BufferedReader br) {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /* loaded from: classes.dex */
    public class IMTService {
        private final String SERVICE_NAME = "vendor.xiaomi.hardware.mtdservice@1.0::IMTService";
        private final String INTERFACE_DESCRIPTOR = "vendor.xiaomi.hardware.mtdservice@1.0::IMTService";
        private final String DEFAULT = "default";
        private final int GET_FID = 1;
        private final int GET_ECC_SIGN = 2;

        IMTService() {
            MiuiBatteryAuthentic.this = this$0;
        }

        public String getFid() {
            HwParcel hidl_reply = new HwParcel();
            String val = null;
            try {
                try {
                    IHwBinder hwService = HwBinder.getService("vendor.xiaomi.hardware.mtdservice@1.0::IMTService", "default");
                    if (hwService != null) {
                        HwParcel hidl_request = new HwParcel();
                        hidl_request.writeInterfaceToken("vendor.xiaomi.hardware.mtdservice@1.0::IMTService");
                        hwService.transact(1, hidl_request, hidl_reply, 0);
                        hidl_reply.verifySuccess();
                        hidl_request.releaseTemporaryStorage();
                        val = hidl_reply.readString();
                    }
                } catch (Exception e) {
                    Slog.e(MiuiBatteryAuthentic.TAG, "IMTService getFid transact failed. " + e);
                }
                return val;
            } finally {
                hidl_reply.release();
            }
        }

        public String eccSign(int keyType, String text) {
            HwParcel hidl_reply = new HwParcel();
            String val = null;
            try {
                try {
                    IHwBinder hwService = HwBinder.getService("vendor.xiaomi.hardware.mtdservice@1.0::IMTService", "default");
                    if (hwService != null) {
                        HwParcel hidl_request = new HwParcel();
                        hidl_request.writeInterfaceToken("vendor.xiaomi.hardware.mtdservice@1.0::IMTService");
                        hidl_request.writeInt32(keyType);
                        hidl_request.writeString(text);
                        hwService.transact(2, hidl_request, hidl_reply, 0);
                        hidl_reply.verifySuccess();
                        hidl_request.releaseTemporaryStorage();
                        val = hidl_reply.readString();
                    }
                } catch (Exception e) {
                    Slog.e(MiuiBatteryAuthentic.TAG, "IMTService eccSign transact failed. " + e);
                }
                return val;
            } finally {
                hidl_reply.release();
            }
        }
    }
}
