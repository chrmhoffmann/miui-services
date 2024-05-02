package com.android.server.pm;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.IPackageDeleteObserver;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Slog;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.SystemServiceManager;
import com.android.server.pm.PreInstallServiceTrack;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import miui.os.Build;
import miui.os.MiuiInit;
import miui.telephony.TelephonyManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
/* loaded from: classes.dex */
public class CloudControlPreinstallService extends SystemService {
    private static final String BEGIN_UNINSTALL = "begin_uninstall";
    public static final String CLAUSE_AGREED = "clause_agreed";
    public static final String CLAUSE_AGREED_ACTION = "com.miui.clause_agreed";
    private static final String CONF_ID = "confId";
    private static final boolean DEBUG = true;
    private static final int DEFAULT_BIND_DELAY = 500;
    private static final int DEFAULT_CONNECT_TIME_OUT = 2000;
    private static final int DEFAULT_READ_TIME_OUT = 2000;
    private static final String IMEI_MD5 = "imeiMd5";
    private static final String JSON_EXCEPTION = "json_exception";
    private static final String NETWORK_TYPE = "networkType";
    private static final String OFFLINE_COUNT = "offlineCount";
    private static final String PACKAGE_NAME = "packageName";
    private static final String PREINSTALL_CONFIG = "/cust/etc/cust_apps_config";
    private static final String REGION = "region";
    private static final String REMOVE_FROM_LIST_BEGIN = "remove_from_list_begin";
    private static final String REQUEST_CONNECT_EXCEPTION = "request_connect_exception";
    private static final String SALESE_CHANNEL = "saleschannels";
    private static final String SERVER_ADDRESS = "https://control.preload.xiaomi.com/offline_app_list/get?";
    private static final String SERVER_ADDRESS_GLOBAL = "https://global.control.preload.xiaomi.com/offline_app_list/get?";
    private static final String SIM_DETECTION_ACTION = "com.miui.action.SIM_DETECTION";
    private static final String SKU = "sku";
    private static final String TAG = "CloudControlPreinstall";
    private static final String TRACK_EVENT_NAME = "EVENT_NAME";
    private static final String UNINSTALL_FAILED = "uninstall_failed";
    private static final String UNINSTALL_SUCCESS = "uninstall_success";
    private boolean isUninstallPreinstallApps;
    private JSONObject mConfigObj;
    private String mImeiMe5;
    private String mNetworkType;
    private boolean mReceivedSIM;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() { // from class: com.android.server.pm.CloudControlPreinstallService.1
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            Slog.i(CloudControlPreinstallService.TAG, "onReceive:" + intent);
            if (intent == null) {
                return;
            }
            if (CloudControlPreinstallService.SIM_DETECTION_ACTION.equals(intent.getAction()) && !CloudControlPreinstallService.this.mReceivedSIM) {
                CloudControlPreinstallService.this.mReceivedSIM = true;
                CloudControlPreinstallService.this.uninstallPreinstallAppsDelay();
            }
            if ("android.net.conn.CONNECTIVITY_CHANGE".equals(intent.getAction()) && !CloudControlPreinstallService.this.isUninstallPreinstallApps) {
                CloudControlPreinstallService.this.uninstallPreinstallAppsDelay();
            }
        }
    };
    private PreInstallServiceTrack mTrack;

    public void trackEvent(String event, UninstallApp app) {
        PreInstallServiceTrack.Action action = new PreInstallServiceTrack.Action();
        action.addParam("packageName", app.packageName);
        action.addParam(CONF_ID, app.confId);
        action.addParam(OFFLINE_COUNT, app.offlineCount);
        action.addParam(SALESE_CHANNEL, getChannel());
        action.addParam("isPreinstalled", String.valueOf(MiuiInit.isPreinstalledPackage(app.packageName)));
        action.addParam("miuiChannelPath", String.valueOf(MiuiInit.getMiuiChannelPath(app.packageName)));
        action.addParam(IMEI_MD5, this.mImeiMe5);
        action.addParam(NETWORK_TYPE, this.mNetworkType);
        boolean isCn = !Build.IS_INTERNATIONAL_BUILD;
        String region = isCn ? "CN" : Build.getCustVariant().toUpperCase();
        String sku = getSku();
        action.addParam("region", region);
        action.addParam("sku", sku);
        action.addParam("EVENT_NAME", event);
        track(action);
    }

    public void trackEvent(String event) {
        PreInstallServiceTrack.Action action = new PreInstallServiceTrack.Action();
        action.addParam(SALESE_CHANNEL, getChannel());
        boolean isCn = !Build.IS_INTERNATIONAL_BUILD;
        String region = isCn ? "CN" : Build.getCustVariant().toUpperCase();
        String sku = getSku();
        action.addParam("region", region);
        action.addParam("sku", sku);
        action.addParam(IMEI_MD5, this.mImeiMe5);
        action.addParam(NETWORK_TYPE, this.mNetworkType);
        action.addParam("EVENT_NAME", event);
        track(action);
    }

    private void initOneTrack() {
        PreInstallServiceTrack preInstallServiceTrack = new PreInstallServiceTrack();
        this.mTrack = preInstallServiceTrack;
        preInstallServiceTrack.bindTrackService(getContext());
    }

    private void track(PreInstallServiceTrack.Action action) {
        if (this.mTrack != null && action != null && action.getContent() != null) {
            this.mTrack.trackEvent(action.getContent().toString(), 0);
            Slog.i(TAG, action.getContent().toString());
        }
    }

    /* loaded from: classes.dex */
    public static class UninstallApp {
        int confId;
        String custVariant;
        int offlineCount;
        String packageName;

        public UninstallApp(String packageName, String custVariant, int confId, int offlineCount) {
            this.packageName = packageName;
            this.custVariant = custVariant;
            this.confId = confId;
            this.offlineCount = offlineCount;
        }

        public boolean equals(Object obj) {
            if (obj instanceof UninstallApp) {
                UninstallApp app = (UninstallApp) obj;
                return TextUtils.equals(this.packageName, app.packageName) && TextUtils.equals(this.custVariant, app.custVariant);
            }
            return false;
        }
    }

    /* loaded from: classes.dex */
    public static class ConnectEntity {
        public static final String CHANNEL = "channel";
        public static final String DEVICE = "device";
        public static final String IS_CN = "isCn";
        public static final String LANG = "lang";
        public static final String MIUI_VERSION = "miuiVersion";
        public static final String NONCE = "nonceStr";
        public static final String REGION = "region";
        public static final String SIGN = "sign";
        public static final String SKU = "sku";
        private boolean isCn;
        private String mChannel;
        private String mDevice;
        private String mImeiMd5;
        private String mLang;
        private String mNonceStr;
        private String mRegion;
        private String mSign;
        private String mSku;
        private String miuiVersion;

        public ConnectEntity(String imeiMd5, String device, String miuiVersion, String channel, String lang, String nonceStr, String sign, boolean isCn, String region, String sku) {
            this.mImeiMd5 = imeiMd5;
            this.mDevice = device;
            this.miuiVersion = miuiVersion;
            this.mChannel = channel;
            this.mLang = lang;
            this.mNonceStr = nonceStr;
            this.mSign = sign;
            this.isCn = isCn;
            this.mRegion = region;
            this.mSku = sku;
        }

        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("imeiMd5=" + this.mImeiMd5 + "&");
            builder.append("device=" + this.mDevice + "&");
            builder.append("miuiVersion=" + this.miuiVersion + "&");
            builder.append("channel=" + this.mChannel + "&");
            builder.append("lang=" + this.mLang + "&");
            builder.append("nonceStr=" + this.mNonceStr + "&");
            builder.append("sign=" + this.mSign + "&");
            builder.append("isCn=" + this.isCn + "&");
            builder.append("region=" + this.mRegion + "&");
            builder.append("sku=" + this.mSku);
            return builder.toString();
        }
    }

    private boolean isNetworkConnected(Context context) {
        boolean isAvailable = false;
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService("connectivity");
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            if (networkInfo != null) {
                this.mNetworkType = networkInfo.getTypeName();
                isAvailable = networkInfo.isAvailable();
            } else {
                this.mNetworkType = "";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isAvailable;
    }

    public CloudControlPreinstallService(Context context) {
        super(context);
    }

    public static void startCloudControlService() {
        SystemServiceManager systemServiceManager = (SystemServiceManager) LocalServices.getService(SystemServiceManager.class);
        systemServiceManager.startService(CloudControlPreinstallService.class);
        systemServiceManager.startService(AdvancePreinstallService.class);
    }

    public void onStart() {
        Slog.i(TAG, "onStart");
    }

    public void onBootPhase(int phase) {
        if (phase == 500) {
            Slog.d(TAG, "onBootPhase:" + phase);
            if (isProvisioned(getContext())) {
                return;
            }
            Slog.d(TAG, "onBootPhase:register broadcast receiver");
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(SIM_DETECTION_ACTION);
            intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
            getContext().registerReceiver(this.mReceiver, intentFilter);
        }
    }

    public void uninstallPreinstallAppsDelay() {
        initOneTrack();
        new Handler().postDelayed(new Runnable() { // from class: com.android.server.pm.CloudControlPreinstallService.2
            @Override // java.lang.Runnable
            public void run() {
                CloudControlPreinstallService.this.uninstallPreinstallApps();
            }
        }, 500L);
    }

    public void uninstallPreinstallApps() {
        Slog.d(TAG, "uninstallPreinstallApps");
        if (isProvisioned(getContext())) {
            return;
        }
        Slog.d(TAG, "uninstallPreinstallApps:isProvisioned true");
        if (!isNetworkConnected(getContext())) {
            return;
        }
        this.isUninstallPreinstallApps = true;
        Slog.d(TAG, "uninstallPreinstallApps:isNetworkConnected true");
        AsyncTask.execute(new Runnable() { // from class: com.android.server.pm.CloudControlPreinstallService.3
            @Override // java.lang.Runnable
            public void run() {
                CloudControlPreinstallService.this.trackEvent("cloud_uninstall_start");
                List<UninstallApp> apps = CloudControlPreinstallService.this.getUninstallApps();
                if (Build.IS_INTERNATIONAL_BUILD) {
                    CloudControlPreinstallService.this.recordUnInstallApps(apps);
                } else {
                    CloudControlPreinstallService.this.uninstallAppsUpdateList(apps);
                }
            }
        });
    }

    public void recordUnInstallApps(List<UninstallApp> apps) {
        if (apps != null && !apps.isEmpty()) {
            for (UninstallApp app : apps) {
                if (app != null) {
                    trackEvent(REMOVE_FROM_LIST_BEGIN, app);
                    trackEvent(BEGIN_UNINSTALL, app);
                    PreinstallApp.sCloudControlUninstall.add(app.packageName);
                    try {
                        String trackingApk = findTrackingApk(app.packageName);
                        if (!TextUtils.isEmpty(trackingApk)) {
                            UninstallApp tracking = new UninstallApp(trackingApk, null, -1, -1);
                            trackEvent(REMOVE_FROM_LIST_BEGIN, tracking);
                            trackEvent(BEGIN_UNINSTALL, tracking);
                            PreinstallApp.sCloudControlUninstall.add(trackingApk);
                            trackEvent(UNINSTALL_SUCCESS, tracking);
                        }
                    } catch (JSONException e) {
                        Slog.e(TAG, e.getMessage());
                    }
                    trackEvent(UNINSTALL_SUCCESS, app);
                }
            }
        }
    }

    private String findTrackingApk(String packageName) throws JSONException {
        String config;
        if (this.mConfigObj == null && (config = getFileContent(PREINSTALL_CONFIG)) != null) {
            this.mConfigObj = new JSONObject(config);
        }
        JSONObject jSONObject = this.mConfigObj;
        if (jSONObject != null) {
            JSONArray array = jSONObject.getJSONArray("data");
            if (array.length() <= 0) {
                return null;
            }
            int count = array.length();
            for (int i = 0; i < count; i++) {
                JSONObject obj = array.getJSONObject(i);
                if (obj != null && TextUtils.equals(obj.getString("packageName"), packageName)) {
                    return obj.getString("trackApkPackageName");
                }
            }
        }
        return null;
    }

    public String getFileContent(String filePath) {
        File file = new File(filePath);
        try {
            FileInputStream in = new FileInputStream(file);
            long length = file.length();
            byte[] content = new byte[(int) length];
            in.read(content);
            in.close();
            String str = new String(content, StandardCharsets.UTF_8);
            in.close();
            return str;
        } catch (Exception e) {
            Slog.e(TAG, e.getMessage());
            return null;
        }
    }

    private ConnectEntity getConnectEntity() {
        ConnectEntity entity = null;
        try {
            trackEvent("get_device_info");
            String imei = (String) Collections.min(TelephonyManager.getDefault().getImeiList());
            this.mImeiMe5 = !TextUtils.isEmpty(imei) ? CloudSignUtil.md5(imei) : "";
            String device = Build.DEVICE;
            String miuiVersion = Build.VERSION.INCREMENTAL;
            String channel = getChannel();
            String lang = Locale.getDefault().getLanguage();
            String nonceStr = CloudSignUtil.getNonceStr();
            boolean isCn = !miui.os.Build.IS_INTERNATIONAL_BUILD;
            String region = isCn ? "CN" : miui.os.Build.getCustVariant().toUpperCase();
            String sku = getSku();
            String sign = CloudSignUtil.getSign(this.mImeiMe5, device, miuiVersion, channel, region, isCn, lang, nonceStr, sku);
            entity = TextUtils.isEmpty(sign) ? null : new ConnectEntity(this.mImeiMe5, device, miuiVersion, channel, lang, nonceStr, sign, isCn, region, sku);
        } catch (NoSuchElementException ex) {
            ex.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (entity == null) {
            trackEvent("get_device_info_failed");
        }
        return entity;
    }

    private String getSku() {
        if (!miui.os.Build.IS_INTERNATIONAL_BUILD) {
            return "";
        }
        String buildRegion = SystemProperties.get("ro.miui.build.region", "");
        if (!TextUtils.isEmpty(buildRegion)) {
            return TextUtils.equals(buildRegion.toUpperCase(), "GLOBAL") ? "MI" : buildRegion.toUpperCase();
        }
        if (miui.os.Build.IS_STABLE_VERSION) {
            String buildVersion = Build.VERSION.INCREMENTAL;
            if (!TextUtils.isEmpty(buildVersion) && buildVersion.length() > 4) {
                return buildVersion.substring(buildVersion.length() - 4, buildVersion.length() - 2).toUpperCase();
            }
        }
        return "MI";
    }

    private String getChannel() {
        if (!miui.os.Build.IS_INTERNATIONAL_BUILD) {
            return miui.os.Build.getCustVariant();
        }
        return SystemProperties.get("ro.miui.customized.region", "Public Version");
    }

    private String getAddress() {
        if (!miui.os.Build.IS_INTERNATIONAL_BUILD) {
            return SERVER_ADDRESS;
        }
        return SERVER_ADDRESS_GLOBAL;
    }

    /* JADX WARN: Code restructure failed: missing block: B:111:0x023a, code lost:
        if (r7 == null) goto L113;
     */
    /* JADX WARN: Code restructure failed: missing block: B:84:0x01f0, code lost:
        if (r7 != null) goto L112;
     */
    /* JADX WARN: Not initialized variable reg: 20, insn: 0x019f: MOVE  (r8 I:??[OBJECT, ARRAY]) = (r20 I:??[OBJECT, ARRAY] A[D('br' java.io.BufferedReader)]), block:B:63:0x019f */
    /* JADX WARN: Not initialized variable reg: 20, insn: 0x01a4: MOVE  (r8 I:??[OBJECT, ARRAY]) = (r20 I:??[OBJECT, ARRAY] A[D('br' java.io.BufferedReader)]), block:B:65:0x01a4 */
    /* JADX WARN: Not initialized variable reg: 20, insn: 0x01a9: MOVE  (r8 I:??[OBJECT, ARRAY]) = (r20 I:??[OBJECT, ARRAY] A[D('br' java.io.BufferedReader)]), block:B:67:0x01a9 */
    /* JADX WARN: Not initialized variable reg: 20, insn: 0x01ae: MOVE  (r8 I:??[OBJECT, ARRAY]) = (r20 I:??[OBJECT, ARRAY] A[D('br' java.io.BufferedReader)]), block:B:69:0x01ae */
    /* JADX WARN: Not initialized variable reg: 20, insn: 0x01b3: MOVE  (r8 I:??[OBJECT, ARRAY]) = (r20 I:??[OBJECT, ARRAY] A[D('br' java.io.BufferedReader)]), block:B:71:0x01b3 */
    /* JADX WARN: Removed duplicated region for block: B:100:0x021e  */
    /* JADX WARN: Removed duplicated region for block: B:107:0x022e  */
    /* JADX WARN: Removed duplicated region for block: B:116:0x0246  */
    /* JADX WARN: Removed duplicated region for block: B:95:0x020e  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public java.util.List<com.android.server.pm.CloudControlPreinstallService.UninstallApp> getUninstallApps() {
        /*
            Method dump skipped, instructions count: 589
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.pm.CloudControlPreinstallService.getUninstallApps():java.util.List");
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

    public void uninstallAppsUpdateList(List<UninstallApp> uninstallApps) {
        if (uninstallApps == null || uninstallApps.isEmpty()) {
            trackEvent("uninstall_list_empty");
            return;
        }
        String currentCustVariant = SystemProperties.get("ro.miui.cust_variant");
        PackageManager pm = getContext().getPackageManager();
        if (pm == null) {
            return;
        }
        for (UninstallApp app : uninstallApps) {
            if (TextUtils.equals(currentCustVariant, app.custVariant)) {
                trackEvent(REMOVE_FROM_LIST_BEGIN, app);
                PreinstallApp.removeFromPreinstallList(app.packageName);
                try {
                    pm.setApplicationEnabledSetting(app.packageName, 2, 0);
                } catch (Exception e) {
                    Slog.i(TAG, "disable Package " + app.packageName + " failed:" + e.toString());
                }
            }
        }
        for (UninstallApp app2 : uninstallApps) {
            if (TextUtils.equals(currentCustVariant, app2.custVariant)) {
                if (!exists(pm, app2.packageName)) {
                    Slog.i(TAG, "Package " + app2.packageName + " not exist");
                    trackEvent("package_not_exist", app2);
                } else {
                    try {
                        trackEvent(BEGIN_UNINSTALL, app2);
                        pm.deletePackage(app2.packageName, new PackageDeleteObserver(app2), 2);
                    } catch (Exception e2) {
                        Slog.e(TAG, "uninstall Package " + app2.packageName + " failed:" + e2.toString());
                        trackEvent(UNINSTALL_FAILED, app2);
                    }
                }
            }
        }
    }

    public static boolean exists(PackageManager pm, String packageName) {
        try {
            return pm.getApplicationInfo(packageName, 128) != null;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /* loaded from: classes.dex */
    public class PackageDeleteObserver extends IPackageDeleteObserver.Stub {
        UninstallApp mApp;

        public PackageDeleteObserver(UninstallApp app) {
            CloudControlPreinstallService.this = this$0;
            this.mApp = app;
        }

        public void packageDeleted(String packageName, int returnCode) {
            if (returnCode >= 0) {
                CloudControlPreinstallService.this.trackEvent(CloudControlPreinstallService.UNINSTALL_SUCCESS, this.mApp);
                Slog.i(CloudControlPreinstallService.TAG, "Package " + packageName + " was uninstalled.");
                return;
            }
            CloudControlPreinstallService.this.trackEvent(CloudControlPreinstallService.UNINSTALL_FAILED, this.mApp);
            Slog.e(CloudControlPreinstallService.TAG, "Package uninstall failed " + packageName + ", returnCode " + returnCode);
        }
    }

    public static boolean isProvisioned(Context context) {
        ContentResolver resolver = context.getContentResolver();
        return Settings.Secure.getInt(resolver, "user_setup_complete", 0) != 0;
    }
}
