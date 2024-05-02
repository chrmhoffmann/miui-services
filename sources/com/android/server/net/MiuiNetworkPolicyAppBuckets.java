package com.android.server.net;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerCompat;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.os.Build;
import android.os.Handler;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import miui.telephony.TelephonyManagerEx;
/* loaded from: classes.dex */
public class MiuiNetworkPolicyAppBuckets {
    private static final String ACTION_CLOUD_TELE_FEATURE_INFO_CHANGED = "com.android.phone.intent.action.CLOUD_TELE_FEATURE_INFO_CHANGED";
    private static final String ACTION_SPEED_WHITE_LIST = "com.android.phone.intent.action.SPEED_WHITE_LIST";
    private static final String ACTION_THROTTLE_WHITE_APP_SCENE = "com.android.phone.intent.action.THROTTLE_WHITE_APP_SCENE";
    private static final String CLOUD_CATEGORY_THERMAL_THORTTLE = "TelephonyThermalThrottle";
    private static final String CLOUD_KEY_THROTTLE_WHITE_APP = "Params2";
    private static final String CONNECTION_EX = "enableConnectionExtension";
    private static final int CON_DISABLED = 0;
    private static final int CON_ENABLED = 1;
    private static final boolean DEBUG = true;
    private static final String LATENCY_ACTION_CHANGE_LEVEL = "com.android.phone.intent.action.CHANGE_LEVEL";
    private static final String NOTIFACATION_RECEIVER_PACKAGE = "com.android.phone";
    private static final String OPTIMIZATION_ENABLED = "optimizationEnabled";
    private static final String TAG = "MiuiNetworkPolicyAppBuckets";
    private static final String THROTTLE_WHITE_APP_EXTRA = "throttleWhiteAppExtra";
    private static final String TPUT_TEST_APP_OPTIMIZATION = "com.android.phone.intent.action.TPUT_OPTIMIZATION";
    private static final String WHITE_LIST_PACKAGE_NAME = "whiteListPackageName";
    private static final String WHITE_LIST_STATE_TOP = "whiteListStateTop";
    private Set<String> mAppsPN;
    private ConnectivityManager mCm;
    private final Context mContext;
    private final Handler mHandler;
    private Set<String> mMobileTcEnabledList;
    private Set<String> mSpeedWhiteList;
    private static final String[] LOCAL_HONGBAO_APP_LIST = {"com.tencent.mm"};
    private static final String[] LOCAL_TPUT_TOOL_APP_LIST = {"org.zwanoo.android.speedtest", "org.zwanoo.android.speedtest.china", "cn.nokia.speedtest5g", "org.zwanoo.android.speedtest.gworld", "cn.speedtest.lite", "cn.lezhi.speedtest"};
    private static String[] LOCAL_THROTTLE_WHITE_APP_LIST = new String[0];
    private boolean mIsMobileNwOn = false;
    private boolean mLastMobileNw = false;
    private boolean mIsHongbaoAppOn = false;
    private boolean mLastHongbaoApp = false;
    private boolean mIsTputTestAppOn = false;
    private boolean mLastTputTestApp = false;
    private boolean mIsSpeedWhiteListOn = false;
    private boolean mLastSpeedWhiteList = false;
    private boolean mIsMobileTcEnabledListOn = false;
    private boolean mLastMobileTcEnabledListOn = false;
    private ConcurrentHashMap<String, Integer> mUidMap = new ConcurrentHashMap<>();
    private boolean mIsWhiteAppOn = false;
    private boolean mLastWhiteApp = false;
    private String mLastThrottleWhiteAppList = null;
    private String mWhiteListPkgName = null;
    private String mMobileTcEnabledPkgName = null;
    ContentObserver mSpeedWhiteListObserver = new ContentObserver(null) { // from class: com.android.server.net.MiuiNetworkPolicyAppBuckets.1
        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            MiuiNetworkPolicyAppBuckets.this.updateAppList();
        }
    };
    final BroadcastReceiver mReceiver = new BroadcastReceiver() { // from class: com.android.server.net.MiuiNetworkPolicyAppBuckets.2
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (TextUtils.isEmpty(action)) {
                MiuiNetworkPolicyAppBuckets.this.log("BroadcastReceiver action is null!");
            } else if ("android.net.conn.CONNECTIVITY_CHANGE".equals(action)) {
                int networkType = intent.getIntExtra("networkType", 0);
                if (networkType == 0) {
                    String iface = MiuiNetworkPolicyAppBuckets.this.getMobileLinkIface();
                    MiuiNetworkPolicyAppBuckets.this.log("BroadcastReceiver iface=" + iface);
                    MiuiNetworkPolicyAppBuckets.this.mIsMobileNwOn = !TextUtils.isEmpty(iface);
                    if (MiuiNetworkPolicyAppBuckets.this.mLastMobileNw != MiuiNetworkPolicyAppBuckets.this.mIsMobileNwOn) {
                        MiuiNetworkPolicyAppBuckets.this.updateHongbaoModeStatus();
                        MiuiNetworkPolicyAppBuckets.this.updateTputTestAppStatus();
                        MiuiNetworkPolicyAppBuckets.this.updateSpeedWhiteListStatus();
                        MiuiNetworkPolicyAppBuckets.this.updateMobileTcEnabledListStatus();
                        MiuiNetworkPolicyAppBuckets miuiNetworkPolicyAppBuckets = MiuiNetworkPolicyAppBuckets.this;
                        miuiNetworkPolicyAppBuckets.mLastMobileNw = miuiNetworkPolicyAppBuckets.mIsMobileNwOn;
                    }
                }
            } else if (action.equals(MiuiNetworkPolicyAppBuckets.ACTION_CLOUD_TELE_FEATURE_INFO_CHANGED)) {
                MiuiNetworkPolicyAppBuckets.this.update5gPowerWhiteApplist(MiuiNetworkPolicyAppBuckets.CLOUD_CATEGORY_THERMAL_THORTTLE, MiuiNetworkPolicyAppBuckets.CLOUD_KEY_THROTTLE_WHITE_APP);
            }
        }
    };
    ContentObserver mMobileTcEnabledListObserver = new ContentObserver(null) { // from class: com.android.server.net.MiuiNetworkPolicyAppBuckets.3
        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            MiuiNetworkPolicyAppBuckets.this.updateAppList();
        }
    };
    private Set<Integer> mAppUid = new HashSet();

    public MiuiNetworkPolicyAppBuckets(Context context, Handler handler) {
        this.mContext = context;
        this.mHandler = handler;
    }

    public void systemReady() {
        updateAppList();
        initReceiver();
        if (isCommonSceneRecognitionAllowed()) {
            registerSpeedWhiteList();
        }
        if (MiuiNetworkPolicyManagerService.isMobileTcFeatureAllowed()) {
            registerMobileTcEnabledList();
        }
    }

    private void registerSpeedWhiteList() {
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("fiveg_speed_white_list_pkg_name"), false, this.mSpeedWhiteListObserver);
    }

    private void initReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        filter.addAction(ACTION_CLOUD_TELE_FEATURE_INFO_CHANGED);
        this.mContext.registerReceiver(this.mReceiver, filter);
    }

    public String getMobileLinkIface() {
        if (this.mCm == null) {
            this.mCm = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        }
        LinkProperties prop = this.mCm.getLinkProperties(0);
        if (prop == null || TextUtils.isEmpty(prop.getInterfaceName())) {
            return "";
        }
        return prop.getInterfaceName();
    }

    private static boolean isUidValidForQos(int uid) {
        return UserHandle.isApp(uid);
    }

    private Set<String> getAllAppsPN() {
        Set<String> appList = new HashSet<>();
        if (isHongbaoModeAllowed()) {
            int i = 0;
            while (true) {
                String[] strArr = LOCAL_HONGBAO_APP_LIST;
                if (i >= strArr.length) {
                    break;
                }
                appList.add(strArr[i]);
                i++;
            }
        }
        if (isCommonSceneRecognitionAllowed()) {
            int i2 = 0;
            while (true) {
                String[] strArr2 = LOCAL_TPUT_TOOL_APP_LIST;
                if (i2 >= strArr2.length) {
                    break;
                }
                appList.add(strArr2[i2]);
                i2++;
            }
            Set<String> fetchSpeedAppWhiteList = fetchSpeedAppWhiteList();
            this.mSpeedWhiteList = fetchSpeedAppWhiteList;
            if (fetchSpeedAppWhiteList != null && fetchSpeedAppWhiteList.size() > 0) {
                appList.addAll(this.mSpeedWhiteList);
            }
        }
        if (MiuiNetworkPolicyManagerService.isMobileTcFeatureAllowed()) {
            Set<String> fetchMobileTcEnabledList = fetchMobileTcEnabledList();
            this.mMobileTcEnabledList = fetchMobileTcEnabledList;
            if (fetchMobileTcEnabledList != null && fetchMobileTcEnabledList.size() > 0) {
                appList.addAll(this.mMobileTcEnabledList);
            }
        }
        int i3 = 0;
        while (true) {
            String[] strArr3 = LOCAL_THROTTLE_WHITE_APP_LIST;
            if (i3 < strArr3.length) {
                appList.add(strArr3[i3]);
                i3++;
            } else {
                return appList;
            }
        }
    }

    public Set<String> fetchSpeedAppWhiteList() {
        String pkgNames = Settings.Global.getString(this.mContext.getContentResolver(), "fiveg_speed_white_list_pkg_name");
        log("fetchSpeedAppWhiteList  pkgNames=" + pkgNames);
        if (!TextUtils.isEmpty(pkgNames)) {
            Set<String> whiteList = new HashSet<>(Arrays.asList(pkgNames.split(",")));
            return whiteList;
        }
        return new HashSet();
    }

    public void updateAppList() {
        UserManager um = (UserManager) this.mContext.getSystemService("user");
        PackageManager pm = this.mContext.getPackageManager();
        List<UserInfo> users = um.getUsers();
        this.mAppsPN = getAllAppsPN();
        uidRemoveAll();
        if (!this.mAppsPN.isEmpty()) {
            for (UserInfo user : users) {
                List<PackageInfo> apps = PackageManagerCompat.getInstalledPackagesAsUser(pm, 0, user.id);
                for (PackageInfo app : apps) {
                    if (app.packageName != null && app.applicationInfo != null && this.mAppsPN.contains(app.packageName)) {
                        int uid = UserHandle.getUid(user.id, app.applicationInfo.uid);
                        addUidToMap(app.packageName, uid);
                    }
                }
            }
            updateUidFromWholeAppMap();
        }
    }

    private void addUidToMap(String packageName, int uid) {
        if (!this.mUidMap.containsKey(packageName)) {
            this.mUidMap.put(packageName, Integer.valueOf(uid));
        }
    }

    private int getUidFromMap(String packageName) {
        if (this.mUidMap.get(packageName) == null) {
            return -1;
        }
        return this.mUidMap.get(packageName).intValue();
    }

    private void removeUidFromMap(String packageName) {
        this.mUidMap.remove(packageName);
    }

    private void uidRemoveAll() {
        this.mUidMap.clear();
    }

    public void updateAppPN(String packageName, int uid, boolean installed) {
        log("updateAppPN packageName=" + packageName + ",uid=" + uid + ",installed=" + installed);
        Set<String> set = this.mAppsPN;
        if (set != null && set.contains(packageName)) {
            if (installed) {
                addUidToMap(packageName, uid);
            } else {
                removeUidFromMap(packageName);
            }
            updateUidFromWholeAppMap();
        }
    }

    public void updateAppBucketsForUidStateChange(int uid, int oldUidState, int newUidState) {
        if (isUidValidForQos(uid) && isAppBucketsEnabledForUid(uid, oldUidState) != isAppBucketsEnabledForUid(uid, newUidState)) {
            appBucketsForUidStateChanged(uid, newUidState);
        }
    }

    private boolean isAppBucketsEnabledForUid(int uid, int state) {
        return state == 2 && this.mAppUid.contains(Integer.valueOf(uid));
    }

    private void appBucketsForUidStateChanged(int uid, int state) {
        log("appBucketsForUidStateChanged uid=" + uid + ",state=" + state);
        if (isAppBucketsEnabledForUid(uid, state)) {
            processHongbaoAppIfNeed(uid, true);
            processTputTestAppIfNeed(uid, true);
            processSpeedWhiteListIfNeed(uid, true);
            processMobileTcEnabledListIfNeed(uid, true);
            processThrottleWhiteAppIfNeed(uid, true);
            return;
        }
        processHongbaoAppIfNeed(uid, false);
        processTputTestAppIfNeed(uid, false);
        processSpeedWhiteListIfNeed(uid, false);
        processMobileTcEnabledListIfNeed(uid, false);
        processThrottleWhiteAppIfNeed(uid, false);
    }

    public synchronized void updateHongbaoModeStatus() {
        boolean isOldStatusOn = true;
        boolean isNewStatusOn = this.mIsMobileNwOn && this.mIsHongbaoAppOn;
        if (!this.mLastMobileNw || !this.mLastHongbaoApp) {
            isOldStatusOn = false;
        }
        log("updateHongbaoModeStatus isNewStatusOn=" + isNewStatusOn + ",isOldStatusOn=" + isOldStatusOn);
        if (isNewStatusOn != isOldStatusOn) {
            enableHongbaoMode(isNewStatusOn);
        }
    }

    private boolean hasUidFromHongbaoMap(int uid) {
        boolean rst = false;
        int i = 0;
        while (true) {
            String[] strArr = LOCAL_HONGBAO_APP_LIST;
            if (i < strArr.length) {
                if (getUidFromMap(strArr[i]) == uid) {
                    rst = true;
                }
                i++;
            } else {
                return rst;
            }
        }
    }

    private boolean hasUidFromTputTestMap(int uid) {
        int i = 0;
        while (true) {
            String[] strArr = LOCAL_TPUT_TOOL_APP_LIST;
            if (i >= strArr.length) {
                return false;
            }
            if (getUidFromMap(strArr[i]) != uid) {
                i++;
            } else {
                return true;
            }
        }
    }

    private boolean hasUidFromSpeedWhiteListMap(int uid) {
        Set<String> set = this.mSpeedWhiteList;
        if (set == null || set.size() == 0) {
            return false;
        }
        for (String packageName : this.mSpeedWhiteList) {
            if (getUidFromMap(packageName) == uid) {
                this.mWhiteListPkgName = packageName;
                return true;
            }
        }
        return false;
    }

    private void updateUidFromWholeAppMap() {
        this.mAppUid.clear();
        if (this.mAppsPN.isEmpty()) {
            return;
        }
        for (String pn : this.mAppsPN) {
            int uid = getUidFromMap(pn);
            if (uid != -1) {
                this.mAppUid.add(Integer.valueOf(uid));
            }
        }
    }

    private void processHongbaoAppIfNeed(int uid, boolean enabled) {
        if (hasUidFromHongbaoMap(uid)) {
            log("processHongbaoAppIfNeed Hongbao" + enabled);
            this.mIsHongbaoAppOn = enabled;
            updateHongbaoModeStatus();
            this.mLastHongbaoApp = this.mIsHongbaoAppOn;
        }
    }

    private void enableHongbaoMode(boolean enable) {
        log("enableHongbaoMode enable" + enable);
        Intent intent = new Intent();
        intent.setAction(LATENCY_ACTION_CHANGE_LEVEL);
        intent.setPackage(NOTIFACATION_RECEIVER_PACKAGE);
        intent.putExtra(CONNECTION_EX, enable ? 1 : 0);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
    }

    private void processTputTestAppIfNeed(int uid, boolean enabled) {
        if (hasUidFromTputTestMap(uid)) {
            log("processTputTestAppIfNeed TputTest=" + enabled);
            this.mIsTputTestAppOn = enabled;
            updateTputTestAppStatus();
            this.mLastTputTestApp = this.mIsTputTestAppOn;
        }
    }

    public synchronized void updateTputTestAppStatus() {
        boolean isNewStatusOn = this.mIsTputTestAppOn;
        boolean isOldStatusOn = this.mLastTputTestApp;
        log("updateTputTestAppStatus isNewStatusOn=" + isNewStatusOn + ",isOldStatusOn=" + isOldStatusOn);
        if (isNewStatusOn != isOldStatusOn) {
            tputTestAppNotification(isNewStatusOn);
        }
    }

    private void tputTestAppNotification(boolean enable) {
        log("tputTestAppNotification enable=" + enable);
        Intent intent = new Intent();
        intent.setAction(TPUT_TEST_APP_OPTIMIZATION);
        intent.setPackage(NOTIFACATION_RECEIVER_PACKAGE);
        intent.putExtra(OPTIMIZATION_ENABLED, enable);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
    }

    private void processSpeedWhiteListIfNeed(int uid, boolean enabled) {
        if (hasUidFromSpeedWhiteListMap(uid)) {
            log("processSpeedWhiteListIfNeed enabled=" + enabled);
            this.mIsSpeedWhiteListOn = enabled;
            updateSpeedWhiteListStatus();
            this.mLastSpeedWhiteList = this.mIsSpeedWhiteListOn;
        }
    }

    public synchronized void updateSpeedWhiteListStatus() {
        boolean isOldStatusOn = true;
        boolean isNewStatusOn = this.mIsMobileNwOn && this.mIsSpeedWhiteListOn;
        if (!this.mLastMobileNw || !this.mLastSpeedWhiteList) {
            isOldStatusOn = false;
        }
        log("updateSpeedWhiteListStatus isNewStatusOn=" + isNewStatusOn + ",isOldStatusOn=" + isOldStatusOn);
        if (isNewStatusOn != isOldStatusOn) {
            speedWhiteListAppNotification(isNewStatusOn);
        }
    }

    private void speedWhiteListAppNotification(boolean enable) {
        log("speedWhiteListAppNotification enable=" + enable + "; mWhiteListPkgName=" + this.mWhiteListPkgName);
        Intent intent = new Intent();
        intent.setAction(ACTION_SPEED_WHITE_LIST);
        intent.setPackage(NOTIFACATION_RECEIVER_PACKAGE);
        intent.putExtra(WHITE_LIST_STATE_TOP, enable);
        intent.putExtra(WHITE_LIST_PACKAGE_NAME, this.mWhiteListPkgName);
        this.mContext.sendStickyBroadcastAsUser(intent, UserHandle.CURRENT);
    }

    private static boolean isCommonSceneRecognitionAllowed() {
        return Build.VERSION.SDK_INT >= 29 && !miui.os.Build.IS_INTERNATIONAL_BUILD && !"crux".equals(miui.os.Build.DEVICE) && !"andromeda".equals(miui.os.Build.DEVICE);
    }

    private static boolean isHongbaoModeAllowed() {
        return false;
    }

    public synchronized void updateMobileTcEnabledListStatus() {
        boolean isOldStatusOn = true;
        boolean isNewStatusOn = this.mIsMobileNwOn && this.mIsMobileTcEnabledListOn;
        if (!this.mLastMobileNw || !this.mLastMobileTcEnabledListOn) {
            isOldStatusOn = false;
        }
        log("updateMobileTcEnabledListStatus isNewStatusOn=" + isNewStatusOn + ",isOldStatusOn=" + isOldStatusOn);
        if (isNewStatusOn != isOldStatusOn) {
            mobileTcEnabledStatusChanged(isNewStatusOn);
        }
    }

    private void mobileTcEnabledStatusChanged(boolean enable) {
        log("mobileTcEnabledStatusChanged enable" + enable + ",mMobileTcEnabledPkgName=" + this.mMobileTcEnabledPkgName);
        Handler handler = this.mHandler;
        handler.sendMessage(handler.obtainMessage(13, enable ? 1 : 0, 0));
    }

    private void processMobileTcEnabledListIfNeed(int uid, boolean enabled) {
        if (hasUidFromMobileTcEnabledListMap(uid)) {
            log("processMobileTcEnabledListIfNeed enabled=" + enabled);
            this.mIsMobileTcEnabledListOn = enabled;
            updateMobileTcEnabledListStatus();
            this.mLastMobileTcEnabledListOn = this.mIsMobileTcEnabledListOn;
        }
    }

    private boolean hasUidFromMobileTcEnabledListMap(int uid) {
        Set<String> set = this.mMobileTcEnabledList;
        if (set == null || set.size() == 0) {
            return false;
        }
        for (String packageName : this.mMobileTcEnabledList) {
            if (getUidFromMap(packageName) == uid) {
                this.mMobileTcEnabledPkgName = packageName;
                return true;
            }
        }
        return false;
    }

    public Set<String> fetchMobileTcEnabledList() {
        String pkgNames = Settings.Global.getString(this.mContext.getContentResolver(), "mobile_tc_enabled_list_pkg_name");
        log("fetchMobileTcEnabledList  pkgNames=" + pkgNames);
        if (!TextUtils.isEmpty(pkgNames)) {
            Set<String> whiteList = new HashSet<>(Arrays.asList(pkgNames.split(",")));
            return whiteList;
        }
        return new HashSet();
    }

    private void registerMobileTcEnabledList() {
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("mobile_tc_enabled_list_pkg_name"), false, this.mMobileTcEnabledListObserver);
    }

    private void processThrottleWhiteAppIfNeed(int uid, boolean enabled) {
        if (hasUidFromThrottleWhiteAppMap(uid)) {
            log("processThrottleWhiteAppIfNeed enabled = " + enabled);
            this.mIsWhiteAppOn = enabled;
            update5gPowerWhiteAppStatus(ACTION_THROTTLE_WHITE_APP_SCENE, NOTIFACATION_RECEIVER_PACKAGE, THROTTLE_WHITE_APP_EXTRA);
            this.mLastWhiteApp = this.mIsWhiteAppOn;
        }
    }

    private boolean hasUidFromThrottleWhiteAppMap(int uid) {
        int i = 0;
        while (true) {
            String[] strArr = LOCAL_THROTTLE_WHITE_APP_LIST;
            if (i >= strArr.length) {
                return false;
            }
            if (getUidFromMap(strArr[i]) != uid) {
                i++;
            } else {
                return true;
            }
        }
    }

    public void update5gPowerWhiteApplist(String Category, String Key) {
        log("update5gPowerWhiteApplist Category = " + Category + ", Key = " + Key);
        String cloudAppList = getAppListFromCloud(Category, Key);
        if (cloudAppList != null) {
            String str = this.mLastThrottleWhiteAppList;
            if (str == null || !str.equals(cloudAppList)) {
                log("update5gPowerWhiteApplist newWhitelist = " + cloudAppList);
                if (Category.equals(CLOUD_CATEGORY_THERMAL_THORTTLE)) {
                    LOCAL_THROTTLE_WHITE_APP_LIST = cloudAppList.split(",");
                    this.mLastThrottleWhiteAppList = cloudAppList;
                }
                updateAppList();
            }
        }
    }

    private synchronized void update5gPowerWhiteAppStatus(String action, String pkg, String extra) {
        boolean isNewStatusOn = this.mIsWhiteAppOn;
        boolean isOldStatusOn = this.mLastWhiteApp;
        log("update5gPowerWhiteAppStatus isNewStatusOn=" + isNewStatusOn + ",isOldStatusOn=" + isOldStatusOn);
        if (isNewStatusOn != isOldStatusOn) {
            whiteAppNotification(action, pkg, extra, isNewStatusOn);
        }
    }

    private String getAppListFromCloud(String Category, String Key) {
        Intent featureInfoIntent = TelephonyManagerEx.getDefault().getFeatureInfoIntentByCloud(Category);
        if (featureInfoIntent != null) {
            return featureInfoIntent.getStringExtra(Key);
        }
        return null;
    }

    private void whiteAppNotification(String action, String pkg, String extra, boolean enable) {
        log("whiteAppNotification action = " + action + ", enable = " + enable);
        Intent intent = new Intent();
        intent.setAction(action);
        intent.setPackage(pkg);
        intent.putExtra(extra, enable);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
    }

    public void log(String s) {
        Log.d(TAG, s);
    }
}
