package com.android.server.pm;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.IPackageDeleteObserver;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.parsing.PackageLite;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Slog;
import com.android.id.IdentifierManager;
import com.android.server.SystemService;
import com.android.server.pm.PreInstallServiceTrack;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import miui.os.Build;
import miui.os.MiuiInit;
import miui.telephony.TelephonyManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
/* loaded from: classes.dex */
public class AdvancePreinstallService extends SystemService {
    public static final String ADVANCE_TAG = "miuiAdvancePreload";
    public static final String ADVERT_TAG = "miuiAdvertisement";
    private static final String AD_ONLINE_SERVICE_NAME = "com.xiaomi.preload.services.AdvancePreInstallService";
    private static final String AD_ONLINE_SERVICE_PACKAGE_NAME = "com.xiaomi.preload";
    private static final String ANDROID_VERSION = "androidVersion";
    private static final String APP_TYPE = "appType";
    private static final String CHANNEL = "channel";
    private static final String CONF_ID = "confId";
    private static final boolean DEBUG = true;
    private static final int DEFAULT_BIND_DELAY = 500;
    private static final int DEFAULT_CONNECT_TIME_OUT = 2000;
    private static final int DEFAULT_READ_TIME_OUT = 2000;
    private static final String DEVICE = "device";
    private static final String IMEI_ID = "imId";
    private static final String IMEI_MD5 = "imeiMd5";
    private static final String IS_CN = "isCn";
    private static final String KEY_IS_PREINSTALLED = "isPreinstalled";
    private static final String KEY_MIUI_CHANNELPATH = "miuiChannelPath";
    private static final String LANG = "lang";
    private static final String MIUI_VERSION = "miuiVersion";
    private static final String MODEL = "model";
    private static final String NETWORK_TYPE = "networkType";
    private static final String NONCE = "nonceStr";
    private static final String OFFLINE_COUNT = "offlineCount";
    private static final String PACKAGE_NAME = "packageName";
    private static final String PREINSTALL_CONFIG = "/cust/etc/cust_apps_config";
    private static final String REGION = "region";
    private static final String REQUEST_TYPE = "request_type";
    private static final String SALESE_CHANNEL = "saleschannels";
    private static final String SERVER_ADDRESS = "https://control.preload.xiaomi.com/preload_app_info/get?";
    private static final String SERVER_ADDRESS_GLOBAL = "https://global.control.preload.xiaomi.com/preload_app_info/get?";
    private static final String SIGN = "sign";
    private static final String SIM_DETECTION_ACTION = "com.miui.action.SIM_DETECTION";
    private static final String SKU = "sku";
    private static final String TAG = "AdvancePreinstallService";
    private static final String TRACK_EVENT_NAME = "EVENT_NAME";
    private Map<String, String> mAdvanceApps;
    private JSONObject mConfigObj;
    private boolean mHasReceived;
    private String mImeiMe5;
    private boolean mIsNetworkConnected;
    private boolean mIsWifiConnected;
    private String mNetworkTypeName;
    private PackageManager mPackageManager;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() { // from class: com.android.server.pm.AdvancePreinstallService.1
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            Slog.i(AdvancePreinstallService.TAG, "onReceive:" + intent);
            if (intent != null && !AdvancePreinstallService.this.mHasReceived) {
                AdvancePreinstallService.this.mHasReceived = true;
                if (AdvancePreinstallService.SIM_DETECTION_ACTION.equals(intent.getAction())) {
                    AdvancePreinstallService.this.handleAdvancePreinstallAppsDelay();
                }
            }
        }
    };
    private PreInstallServiceTrack mTrack;

    public AdvancePreinstallService(Context context) {
        super(context);
    }

    public void onStart() {
        Slog.i(TAG, "onStart");
    }

    public void onBootPhase(int phase) {
        if (phase == 500) {
            IntentFilter intentFilter = new IntentFilter(SIM_DETECTION_ACTION);
            getContext().registerReceiver(this.mReceiver, intentFilter);
        }
    }

    private Map<String, String> getAdvanceApps() {
        Map<String, String> map = this.mAdvanceApps;
        if (map != null) {
            return map;
        }
        if (!Build.IS_INTERNATIONAL_BUILD) {
            Map<String, String> map2 = PreinstallApp.sAdvanceApps;
            this.mAdvanceApps = map2;
            return map2;
        }
        List<File> custApps = PreinstallApp.getCustomizePreinstallAppList();
        this.mAdvanceApps = new HashMap();
        for (File file : custApps) {
            if (file != null && file.exists() && (file.getPath().contains(ADVANCE_TAG) || file.getPath().contains(ADVERT_TAG))) {
                PackageLite pl = PreinstallApp.parsePackageLite(file);
                if (pl != null && !TextUtils.isEmpty(pl.getPackageName())) {
                    this.mAdvanceApps.put(pl.getPackageName(), file.getPath());
                }
            }
        }
        return this.mAdvanceApps;
    }

    public void uninstallAdvancePreinstallApps() {
        Map<String, String> pkgMap = getAdvanceApps();
        Slog.d(TAG, "advance app size:" + pkgMap.size());
        for (Map.Entry entry : pkgMap.entrySet()) {
            if (entry != null) {
                String path = entry.getValue();
                String appType = isAdvanceApps(path) ? ADVANCE_TAG : ADVERT_TAG;
                if (Build.IS_INTERNATIONAL_BUILD) {
                    String packageName = entry.getKey();
                    recordUnInstallApps(packageName, -1, -1, appType);
                } else {
                    doUninstallApps(entry.getKey(), -1, -1, appType);
                }
            }
        }
    }

    private boolean isAdvanceApps(String apkPath) {
        return apkPath != null && apkPath.contains(ADVANCE_TAG);
    }

    private boolean isAdvertApps(String apkPath) {
        return apkPath != null && apkPath.contains(ADVERT_TAG);
    }

    private String getIMEIMD5() {
        List<String> imeiList = TelephonyManager.getDefault().getImeiList();
        String imei = "";
        if (imeiList != null && imeiList.size() > 0) {
            imei = (String) Collections.min(imeiList);
        }
        return !TextUtils.isEmpty(imei) ? CloudSignUtil.md5(imei) : "";
    }

    public void handleAdvancePreinstallAppsDelay() {
        initOneTrack();
        new Handler().postDelayed(new Runnable() { // from class: com.android.server.pm.AdvancePreinstallService.2
            @Override // java.lang.Runnable
            public void run() {
                AdvancePreinstallService.this.handleAdvancePreinstallApps();
            }
        }, 500L);
    }

    public void handleAdvancePreinstallApps() {
        if (isProvisioned(getContext())) {
            return;
        }
        this.mPackageManager = getContext().getPackageManager();
        this.mImeiMe5 = getIMEIMD5();
        AsyncTask.execute(new Runnable() { // from class: com.android.server.pm.AdvancePreinstallService.3
            @Override // java.lang.Runnable
            public void run() {
                AdvancePreinstallService.this.trackEvent("advance_uninstall_start");
                AdvancePreinstallService advancePreinstallService = AdvancePreinstallService.this;
                if (!advancePreinstallService.isNetworkConnected(advancePreinstallService.getContext())) {
                    AdvancePreinstallService.this.uninstallAdvancePreinstallApps();
                } else {
                    AdvancePreinstallService.this.getPreloadAppInfo();
                }
            }
        });
    }

    public boolean isNetworkConnected(Context context) {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService("connectivity");
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            if (networkInfo == null) {
                return false;
            }
            int type = networkInfo.getType();
            if (type == 1) {
                this.mIsWifiConnected = true;
            }
            this.mIsNetworkConnected = networkInfo.isAvailable();
            this.mNetworkTypeName = networkInfo.getTypeName();
            return this.mIsNetworkConnected;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private int getNetworkType(Context context) {
        if (!this.mIsNetworkConnected) {
            return 0;
        }
        if (this.mIsWifiConnected) {
            return -1;
        }
        android.telephony.TelephonyManager tm = (android.telephony.TelephonyManager) context.getSystemService("phone");
        if (tm == null) {
            return 0;
        }
        return tm.getNetworkType();
    }

    public boolean exists(PackageManager pm, String packageName) {
        if (pm == null || TextUtils.isEmpty(packageName)) {
            return false;
        }
        try {
            return pm.getApplicationInfo(packageName, 128) != null;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void doUninstallApps(String packageName, int confId, int offlineCount, String appType) {
        if (!TextUtils.isEmpty(packageName) && this.mPackageManager != null) {
            trackEvent("remove_from_list_begin", packageName, confId, offlineCount, appType);
            PreinstallApp.removeFromPreinstallList(packageName);
            if (!exists(this.mPackageManager, packageName)) {
                trackEvent("package_not_exist", packageName, confId, offlineCount, appType);
                return;
            }
            try {
                this.mPackageManager.setApplicationEnabledSetting(packageName, 2, 0);
            } catch (Exception e) {
                Slog.i(TAG, "disable Package " + packageName + " failed:" + e.toString());
                trackEvent("disable_package_failed", packageName, confId, offlineCount, appType);
            }
            try {
                trackEvent("begin_uninstall", packageName, confId, offlineCount, appType);
                this.mPackageManager.deletePackage(packageName, new PackageDeleteObserver(packageName, confId, offlineCount, appType), 2);
            } catch (Exception e2) {
                Slog.e(TAG, "uninstall Package " + packageName + " failed:" + e2.toString());
                trackEvent("uninstall_failed", packageName, confId, offlineCount, appType);
            }
        }
    }

    private String findTrackingApk(String packageName) {
        String config;
        if (this.mConfigObj == null && (config = getFileContent(PREINSTALL_CONFIG)) != null) {
            try {
                this.mConfigObj = new JSONObject(config);
            } catch (JSONException e) {
                Slog.e(TAG, e.getMessage());
            }
        }
        JSONObject jSONObject = this.mConfigObj;
        if (jSONObject != null) {
            try {
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
            } catch (JSONException e2) {
                Slog.e(TAG, e2.getMessage());
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

    private void recordUnInstallApps(String packageName, int confId, int offlineCount, String appType) {
        trackEvent("remove_from_list_begin", packageName, confId, offlineCount, appType);
        trackEvent("begin_uninstall", packageName, confId, offlineCount, appType);
        PreinstallApp.sCloudControlUninstall.add(packageName);
        trackEvent("uninstall_success", packageName, confId, offlineCount, appType);
    }

    private void handleOfflineApps(JSONArray array, String tag) {
        if (array == null || array.length() == 0) {
            return;
        }
        for (int i = 0; i < array.length(); i++) {
            try {
                JSONObject appInfo = array.getJSONObject(i);
                if (appInfo != null) {
                    String pkgName = appInfo.getString("packageName");
                    int confId = appInfo.getInt(CONF_ID);
                    int offlineCount = appInfo.getInt(OFFLINE_COUNT);
                    if (Build.IS_INTERNATIONAL_BUILD) {
                        recordUnInstallApps(pkgName, confId, offlineCount, tag);
                        String trackingPkg = findTrackingApk(pkgName);
                        if (!TextUtils.isEmpty(trackingPkg)) {
                            recordUnInstallApps(trackingPkg, confId, offlineCount, "");
                        }
                    } else {
                        doUninstallApps(pkgName, confId, offlineCount, tag);
                    }
                }
            } catch (JSONException e) {
                Slog.e(TAG, e.getMessage());
            }
        }
    }

    private void handleAdOnlineInfo() {
        if (isAdOnlineServiceAvailable(getContext())) {
            Intent intent = new Intent();
            intent.setClassName(AD_ONLINE_SERVICE_PACKAGE_NAME, AD_ONLINE_SERVICE_NAME);
            getContext().startForegroundService(intent);
        }
    }

    public void trackAdvancePreloadSuccess(String packageName) {
        PreInstallServiceTrack.Action action = new PreInstallServiceTrack.Action();
        action.addParam(REQUEST_TYPE, 1);
        action.addParam(IMEI_MD5, this.mImeiMe5);
        action.addParam("packageName", packageName);
        action.addParam(NETWORK_TYPE, this.mNetworkTypeName);
        action.addParam(APP_TYPE, ADVANCE_TAG);
        action.addParam(SALESE_CHANNEL, getChannel());
        action.addParam(IMEI_ID, IdentifierManager.getOAID(getContext()));
        boolean isCn = !Build.IS_INTERNATIONAL_BUILD;
        String region = isCn ? "CN" : Build.getCustVariant().toUpperCase();
        String sku = getSku();
        action.addParam("region", region);
        action.addParam("sku", sku);
        action.addParam("EVENT_NAME", "install_success");
        track(action);
    }

    private void handleAdvancePreloadTrack(JSONArray array) {
        Map<String, String> pkgMap = getAdvanceApps();
        for (Map.Entry entry : pkgMap.entrySet()) {
            if (entry != null) {
                String path = entry.getValue();
                if (isAdvanceApps(path)) {
                    String existPkgName = entry.getKey();
                    if ((Build.IS_INTERNATIONAL_BUILD && !PreinstallApp.sCloudControlUninstall.contains(existPkgName)) || (!isPackageToRemove(existPkgName, array) && exists(this.mPackageManager, existPkgName))) {
                        trackAdvancePreloadSuccess(existPkgName);
                    }
                }
            }
        }
    }

    private void handleAllInstallSuccess() {
        Map<String, String> pkgMap = getAdvanceApps();
        for (Map.Entry entry : pkgMap.entrySet()) {
            if (entry != null) {
                String path = entry.getValue();
                if (isAdvanceApps(path)) {
                    String existPkgName = entry.getKey();
                    if (Build.IS_INTERNATIONAL_BUILD || exists(this.mPackageManager, existPkgName)) {
                        trackAdvancePreloadSuccess(existPkgName);
                    }
                }
            }
        }
    }

    private boolean isPackageToRemove(String packageName, JSONArray array) {
        if (array == null || array.length() == 0) {
            return false;
        }
        int deleteCount = array.length();
        for (int i = 0; i < deleteCount; i++) {
            try {
                JSONObject appInfo = array.getJSONObject(i);
                if (appInfo != null) {
                    String pkgName = appInfo.getString("packageName");
                    if (TextUtils.equals(pkgName, packageName)) {
                        return true;
                    }
                }
            } catch (JSONException e) {
                Slog.e(TAG, e.getMessage());
            }
        }
        return false;
    }

    private boolean isAdOnlineServiceAvailable(Context context) {
        Intent intent = new Intent();
        intent.setClassName(AD_ONLINE_SERVICE_PACKAGE_NAME, AD_ONLINE_SERVICE_NAME);
        PackageManager pm = context.getPackageManager();
        if (pm == null) {
            return false;
        }
        List<ResolveInfo> list = pm.queryIntentServices(intent, 0);
        return list.size() > 0;
    }

    public void trackEvent(String event, String packageName, int confId, int offlineCount, String tag) {
        PreInstallServiceTrack.Action action = new PreInstallServiceTrack.Action();
        action.addParam("packageName", packageName);
        action.addParam(CONF_ID, confId);
        action.addParam(OFFLINE_COUNT, offlineCount);
        action.addParam(APP_TYPE, tag);
        action.addParam(REQUEST_TYPE, 1);
        action.addParam(SALESE_CHANNEL, getChannel());
        action.addParam(KEY_IS_PREINSTALLED, String.valueOf(MiuiInit.isPreinstalledPackage(packageName)));
        action.addParam(KEY_MIUI_CHANNELPATH, String.valueOf(MiuiInit.getMiuiChannelPath(packageName)));
        action.addParam(IMEI_MD5, this.mImeiMe5);
        action.addParam(IMEI_ID, IdentifierManager.getOAID(getContext()));
        action.addParam(NETWORK_TYPE, this.mNetworkTypeName);
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
        action.addParam(REQUEST_TYPE, 1);
        action.addParam(IMEI_MD5, this.mImeiMe5);
        action.addParam(NETWORK_TYPE, this.mNetworkTypeName);
        action.addParam(SALESE_CHANNEL, getChannel());
        action.addParam(IMEI_ID, IdentifierManager.getOAID(getContext()));
        boolean isCn = !Build.IS_INTERNATIONAL_BUILD;
        String region = isCn ? "CN" : Build.getCustVariant().toUpperCase();
        String sku = getSku();
        action.addParam("region", region);
        action.addParam("sku", sku);
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
        }
    }

    private String getSku() {
        if (!Build.IS_INTERNATIONAL_BUILD) {
            return "";
        }
        String buildRegion = SystemProperties.get("ro.miui.build.region", "");
        if (!TextUtils.isEmpty(buildRegion)) {
            return TextUtils.equals(buildRegion.toUpperCase(), "GLOBAL") ? "MI" : buildRegion.toUpperCase();
        }
        if (Build.IS_STABLE_VERSION) {
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

    public void getPreloadAppInfo() {
        ConnectEntity entity = getConnectEntity();
        if (entity == null) {
            return;
        }
        String url = getAddress() + entity.toString();
        HttpURLConnection conn = null;
        BufferedReader br = null;
        try {
            try {
                try {
                    try {
                        try {
                            trackEvent("request_connect_start");
                            HttpURLConnection conn2 = (HttpURLConnection) new URL(url).openConnection();
                            conn2.setConnectTimeout(2000);
                            conn2.setReadTimeout(2000);
                            conn2.setRequestMethod("GET");
                            conn2.connect();
                            int responeCode = conn2.getResponseCode();
                            if (responeCode == 200) {
                                trackEvent("request_connect_success");
                                br = new BufferedReader(new InputStreamReader(conn2.getInputStream()), 1024);
                                StringBuilder sb = new StringBuilder();
                                while (true) {
                                    String line = br.readLine();
                                    if (line == null) {
                                        break;
                                    }
                                    sb.append(line);
                                }
                                Slog.i(TAG, "result:" + sb.toString());
                                JSONObject result = new JSONObject(sb.toString());
                                int code = result.getInt("code");
                                String message = result.getString("message");
                                if (code == 0 && "Success".equals(message)) {
                                    trackEvent("request_list_success");
                                    JSONObject data = result.getJSONObject("data");
                                    if (data == null) {
                                        trackEvent("uninstall_list_empty");
                                        handleAllInstallSuccess();
                                        if (conn2 != null) {
                                            conn2.disconnect();
                                        }
                                        closeBufferedReader(br);
                                        return;
                                    }
                                    JSONArray previewOfflineApps = data.getJSONArray("previewOfflineApps");
                                    JSONArray adOfflineApps = data.getJSONArray("adOfflineApps");
                                    if ((previewOfflineApps != null && previewOfflineApps.length() != 0) || (adOfflineApps != null && adOfflineApps.length() != 0)) {
                                        handleOfflineApps(previewOfflineApps, ADVANCE_TAG);
                                        handleOfflineApps(adOfflineApps, ADVERT_TAG);
                                        handleAdvancePreloadTrack(previewOfflineApps);
                                        handleAdOnlineInfo();
                                    }
                                    trackEvent("uninstall_list_empty");
                                    handleAllInstallSuccess();
                                    if (conn2 != null) {
                                        conn2.disconnect();
                                    }
                                    closeBufferedReader(br);
                                    return;
                                }
                                trackEvent("request_list_failed");
                                uninstallAdvancePreinstallApps();
                                Slog.e(TAG, "advance_request result is failed");
                            } else {
                                trackEvent("request_connect_failed");
                                uninstallAdvancePreinstallApps();
                                Slog.i(TAG, "server can not connected");
                            }
                            if (conn2 != null) {
                                conn2.disconnect();
                            }
                            if (br == null) {
                                return;
                            }
                        } catch (JSONException e) {
                            trackEvent("json_exception");
                            uninstallAdvancePreinstallApps();
                            e.printStackTrace();
                            if (0 != 0) {
                                conn.disconnect();
                            }
                            if (0 == 0) {
                                return;
                            }
                        }
                    } catch (Exception e2) {
                        trackEvent("request_connect_exception");
                        uninstallAdvancePreinstallApps();
                        e2.printStackTrace();
                        if (0 != 0) {
                            conn.disconnect();
                        }
                        if (0 == 0) {
                            return;
                        }
                    }
                } catch (IOException e3) {
                    trackEvent("request_connect_exception");
                    uninstallAdvancePreinstallApps();
                    e3.printStackTrace();
                    if (0 != 0) {
                        conn.disconnect();
                    }
                    if (0 == 0) {
                        return;
                    }
                }
            } catch (ProtocolException e4) {
                trackEvent("request_connect_exception");
                uninstallAdvancePreinstallApps();
                e4.printStackTrace();
                if (0 != 0) {
                    conn.disconnect();
                }
                if (0 == 0) {
                    return;
                }
            }
            closeBufferedReader(br);
        } catch (Throwable th) {
            if (0 != 0) {
                conn.disconnect();
            }
            if (0 != 0) {
                closeBufferedReader(null);
            }
            throw th;
        }
    }

    private ConnectEntity getConnectEntity() {
        try {
            trackEvent("get_device_info");
            String device = miui.os.Build.DEVICE;
            String miuiVersion = Build.VERSION.INCREMENTAL;
            String androidVersion = Build.VERSION.RELEASE;
            String model = miui.os.Build.MODEL;
            String channel = getChannel();
            String lang = Locale.getDefault().getLanguage();
            String nonceStr = CloudSignUtil.getNonceStr();
            boolean isCn = !miui.os.Build.IS_INTERNATIONAL_BUILD;
            String region = isCn ? "CN" : miui.os.Build.getCustVariant().toUpperCase();
            String sku = getSku();
            int networkType = getNetworkType(getContext());
            String imeiId = IdentifierManager.getOAID(getContext());
            String sign = CloudSignUtil.getSign(getParamsMap(this.mImeiMe5, imeiId, model, device, miuiVersion, androidVersion, channel, region, networkType, isCn, lang, sku), nonceStr);
            if (TextUtils.isEmpty(sign)) {
                return null;
            }
            return new ConnectEntity(this.mImeiMe5, imeiId, device, miuiVersion, channel, lang, nonceStr, sign, isCn, region, androidVersion, model, networkType, sku);
        } catch (NoSuchElementException ex) {
            ex.printStackTrace();
            trackEvent("get_device_info_failed");
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            trackEvent("get_device_info_failed");
            return null;
        }
    }

    private TreeMap<String, Object> getParamsMap(String imeiMd5, String imeiId, String model, String device, String miuiVersion, String androidVersion, String channel, String region, int networkType, boolean isCn, String lang, String sku) {
        TreeMap<String, Object> params = new TreeMap<>();
        params.put(IMEI_MD5, imeiMd5);
        params.put(IMEI_ID, imeiId);
        params.put(MODEL, model);
        params.put("device", device);
        params.put("miuiVersion", miuiVersion);
        params.put(ANDROID_VERSION, androidVersion);
        params.put("channel", channel);
        params.put("region", region);
        params.put("isCn", Boolean.valueOf(isCn));
        params.put("lang", lang);
        params.put(NETWORK_TYPE, Integer.valueOf(networkType));
        if (!TextUtils.isEmpty(sku)) {
            params.put("sku", sku);
        }
        return params;
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

    public static boolean isProvisioned(Context context) {
        ContentResolver resolver = context.getContentResolver();
        return Settings.Secure.getInt(resolver, "device_provisioned", 0) != 0;
    }

    /* loaded from: classes.dex */
    public static class ConnectEntity {
        private boolean isCn;
        private String mAndroidVersion;
        private String mChannel;
        private String mDevice;
        private String mImeiId;
        private String mImeiMd5;
        private String mLang;
        private String mModel;
        private int mNetworkType;
        private String mNonceStr;
        private String mRegion;
        private String mSign;
        private String mSku;
        private String miuiVersion;

        ConnectEntity(String imeiMd5, String imeiId, String device, String miuiVersion, String channel, String lang, String nonceStr, String sign, boolean isCn, String region, String androidVersion, String model, int networkType, String sku) {
            this.mImeiMd5 = imeiMd5;
            this.mImeiId = imeiId;
            this.mDevice = device;
            this.miuiVersion = miuiVersion;
            this.mChannel = channel;
            this.mLang = lang;
            this.mNonceStr = nonceStr;
            this.mSign = sign;
            this.isCn = isCn;
            this.mRegion = region;
            this.mAndroidVersion = androidVersion;
            this.mModel = model;
            this.mNetworkType = networkType;
            this.mSku = sku;
        }

        public String toString() {
            return "imeiMd5=" + this.mImeiMd5 + "&" + AdvancePreinstallService.IMEI_ID + "=" + this.mImeiId + "&device=" + this.mDevice + "&miuiVersion=" + this.miuiVersion + "&channel=" + this.mChannel + "&lang=" + this.mLang + "&nonceStr=" + this.mNonceStr + "&sign=" + this.mSign + "&isCn=" + this.isCn + "&region=" + this.mRegion + "&" + AdvancePreinstallService.NETWORK_TYPE + "=" + this.mNetworkType + "&sku=" + this.mSku + "&" + AdvancePreinstallService.ANDROID_VERSION + "=" + this.mAndroidVersion + "&" + AdvancePreinstallService.MODEL + "=" + this.mModel;
        }
    }

    /* loaded from: classes.dex */
    public class PackageDeleteObserver extends IPackageDeleteObserver.Stub {
        private String mAppType;
        private int mConfId;
        private int mOfflineCount;
        private String mPackageName;

        public PackageDeleteObserver(String packageName, int confId, int offlineCount, String appType) {
            AdvancePreinstallService.this = this$0;
            this.mPackageName = packageName;
            this.mConfId = confId;
            this.mOfflineCount = offlineCount;
            this.mAppType = appType;
        }

        public void packageDeleted(String packageName, int returnCode) {
            if (returnCode >= 0) {
                AdvancePreinstallService.this.trackEvent("uninstall_success", this.mPackageName, this.mConfId, this.mOfflineCount, this.mAppType);
                Slog.i(AdvancePreinstallService.TAG, "Package " + packageName + " was uninstalled.");
                return;
            }
            AdvancePreinstallService.this.trackEvent("uninstall_failed", this.mPackageName, this.mConfId, this.mOfflineCount, this.mAppType);
            AdvancePreinstallService advancePreinstallService = AdvancePreinstallService.this;
            if (advancePreinstallService.exists(advancePreinstallService.mPackageManager, this.mPackageName)) {
                AdvancePreinstallService.this.trackAdvancePreloadSuccess(this.mPackageName);
            }
            Slog.e(AdvancePreinstallService.TAG, "Package uninstall failed " + packageName + ", returnCode " + returnCode);
        }
    }
}
