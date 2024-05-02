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
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import com.android.server.MiuiNetworkManagementService;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import miui.util.FeatureParser;
/* loaded from: classes.dex */
public class MiuiNetworkPolicyTrafficLimit {
    private static final boolean DEBUG = true;
    private static final boolean IS_QCOM = "qcom".equals(FeatureParser.getString("vendor"));
    private static final int MOBILE_TC_MODE_CLOSED = 0;
    private static final int MOBILE_TC_MODE_DROP = 1;
    public static final int RULE_RESULT_ERROR_EXIST = 3;
    public static final int RULE_RESULT_ERROR_IFACE = 6;
    public static final int RULE_RESULT_ERROR_OFF = 2;
    public static final int RULE_RESULT_ERROR_ON = 4;
    public static final int RULE_RESULT_ERROR_PARAMS = 5;
    public static final int RULE_RESULT_ERROR_PROCESSING_ENABLE_LIMIT = 7;
    public static final int RULE_RESULT_ERROR_PROCESSING_SET_LIMIT = 8;
    public static final int RULE_RESULT_INIT = 0;
    public static final int RULE_RESULT_SUCCESS = 1;
    private static final String TAG = "MiuiNetworkPolicyTrafficLimit";
    private static final int TRAFFIC_LIMIT_INIT = -1;
    private static final int TRAFFIC_LIMIT_OFF = 0;
    private static final int TRAFFIC_LIMIT_ON = 1;
    private static final int TRAFFIC_LIMIT_RULE_OFF = 2;
    private static final int TRAFFIC_LIMIT_RULE_ON = 3;
    private ConnectivityManager mCm;
    private final Context mContext;
    private String mDefaultInputMethod;
    private final Handler mHandler;
    private String mIface;
    private int mLastMobileTcMode;
    private MiuiNetworkManagementService mNetMgrService;
    private boolean mProcessEnableLimit;
    private boolean mProcessSetLimit;
    private int mTrafficLimitMode;
    private Set<String> mWhiteListAppsPN;
    private Set<Integer> mWhiteListAppsUid;
    private boolean mIsMobileNwOn = false;
    private boolean mMobileLimitEnabled = false;
    private boolean mMobileTcDropEnabled = false;
    final BroadcastReceiver mReceiver = new BroadcastReceiver() { // from class: com.android.server.net.MiuiNetworkPolicyTrafficLimit.9
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.net.conn.CONNECTIVITY_CHANGE".equals(action)) {
                int networkType = intent.getIntExtra("networkType", 0);
                if (networkType == 0) {
                    MiuiNetworkPolicyTrafficLimit.this.log("BroadcastReceiver TYPE_MOBILE");
                    MiuiNetworkPolicyTrafficLimit.this.isAllowedToEnableMobileTcMode();
                }
            }
        }
    };

    public MiuiNetworkPolicyTrafficLimit(Context context, Handler handler) {
        this.mContext = context;
        this.mHandler = handler;
        initParams();
    }

    public void systemReady(MiuiNetworkManagementService networkMgr) {
        this.mNetMgrService = networkMgr;
        this.mCm = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        initReceiver();
        this.mDefaultInputMethod = getDefaultInputMethod();
        registerDefaultInputMethodObserver();
        registerWhiteListAppsChangedObserver();
        registerMobileTrafficControllerChangedObserver();
        this.mNetMgrService.enableLimitter(false);
        updateMobileTcWhiteList();
        log("systemReady");
        isAllowedToEnableMobileTcMode();
    }

    private void initParams() {
        this.mIface = "";
        this.mTrafficLimitMode = -1;
        this.mProcessEnableLimit = false;
        this.mProcessSetLimit = false;
        this.mWhiteListAppsUid = new HashSet();
        this.mLastMobileTcMode = 0;
    }

    private void registerWhiteListAppsChangedObserver() {
        final ContentObserver observer = new ContentObserver(this.mHandler) { // from class: com.android.server.net.MiuiNetworkPolicyTrafficLimit.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                MiuiNetworkPolicyTrafficLimit.this.updateMobileTcWhiteList();
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("mobile_tc_white_list_pkg_name"), false, observer, -2);
        this.mHandler.post(new Runnable() { // from class: com.android.server.net.MiuiNetworkPolicyTrafficLimit.2
            @Override // java.lang.Runnable
            public void run() {
                observer.onChange(false);
            }
        });
    }

    public String getDefaultInputMethod() {
        String inputMethodId = Settings.Secure.getString(this.mContext.getContentResolver(), "default_input_method");
        if (TextUtils.isEmpty(inputMethodId)) {
            return "";
        }
        String[] inputMethodIds = inputMethodId.split("/");
        if (inputMethodIds.length <= 0) {
            return "";
        }
        String defaultInputMethod = inputMethodIds[0];
        return defaultInputMethod;
    }

    private void registerDefaultInputMethodObserver() {
        ContentObserver dimObserver = new ContentObserver(this.mHandler) { // from class: com.android.server.net.MiuiNetworkPolicyTrafficLimit.3
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                MiuiNetworkPolicyTrafficLimit miuiNetworkPolicyTrafficLimit = MiuiNetworkPolicyTrafficLimit.this;
                miuiNetworkPolicyTrafficLimit.mDefaultInputMethod = miuiNetworkPolicyTrafficLimit.getDefaultInputMethod();
                MiuiNetworkPolicyTrafficLimit.this.updateMobileTcWhiteList();
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("default_input_method"), false, dimObserver, -2);
    }

    public synchronized void updateMobileTcWhiteList() {
        UserManager um = (UserManager) this.mContext.getSystemService("user");
        PackageManager pm = this.mContext.getPackageManager();
        List<UserInfo> users = um.getUsers();
        this.mWhiteListAppsPN = fetchMobileTcEnabledList();
        if (!TextUtils.isEmpty(this.mDefaultInputMethod) && !this.mWhiteListAppsPN.contains(this.mDefaultInputMethod)) {
            log("updateMobileTcWhiteList add pkgNames=" + this.mDefaultInputMethod);
            this.mWhiteListAppsPN.add(this.mDefaultInputMethod);
        }
        this.mWhiteListAppsUid.clear();
        if (!this.mWhiteListAppsPN.isEmpty()) {
            for (UserInfo user : users) {
                List<PackageInfo> apps = PackageManagerCompat.getInstalledPackagesAsUser(pm, 0, user.id);
                for (PackageInfo app : apps) {
                    if (app.packageName != null && app.applicationInfo != null && this.mWhiteListAppsPN.contains(app.packageName)) {
                        int uid = UserHandle.getUid(user.id, app.applicationInfo.uid);
                        this.mWhiteListAppsUid.add(Integer.valueOf(uid));
                    }
                }
            }
        }
    }

    public Set<String> fetchMobileTcEnabledList() {
        String pkgNames = Settings.Global.getString(this.mContext.getContentResolver(), "mobile_tc_white_list_pkg_name");
        log("fetchMobileTcEnabledList  pkgNames=" + pkgNames);
        if (!TextUtils.isEmpty(pkgNames)) {
            Set<String> whiteList = new HashSet<>(Arrays.asList(pkgNames.split(",")));
            return whiteList;
        }
        return new HashSet();
    }

    public synchronized void updateAppPN(String packageName, int uid, boolean installed) {
        log("updateAppPN packageName=" + packageName + ",uid=" + uid + ",installed=" + installed);
        Set<String> set = this.mWhiteListAppsPN;
        if (set != null && set.contains(packageName)) {
            if (installed) {
                this.mWhiteListAppsUid.add(Integer.valueOf(uid));
            } else {
                this.mWhiteListAppsUid.remove(Integer.valueOf(uid));
            }
        }
    }

    private boolean isValidStateForWhiteListApp(int uid, int state) {
        return state >= 0 && (state <= 4 || (state < 19 && this.mWhiteListAppsUid.contains(Integer.valueOf(uid))));
    }

    private static boolean isUidValidForMobileTrafficLimit(int uid) {
        return UserHandle.isApp(uid);
    }

    public void updateRulesForUidStateChange(int uid, int oldUidState, int newUidState) {
        if (isUidValidForMobileTrafficLimit(uid) && isValidStateForWhiteListApp(uid, oldUidState) != isValidStateForWhiteListApp(uid, newUidState)) {
            updateWhiteListUidForMobileTraffic(uid, isValidStateForWhiteListApp(uid, newUidState));
        }
    }

    private boolean isLimitterEnabled() {
        return isLimitterEnabled(this.mLastMobileTcMode);
    }

    private boolean isLimitterEnabled(int mode) {
        return mode == 1;
    }

    public void enableMobileTcMode(int mode) {
        boolean isNeedUpdate = false;
        boolean oldLimitterEnabled = isLimitterEnabled();
        boolean newLimitterEnabled = isLimitterEnabled(mode);
        log("enableMobileTcMode oldLimitterEnabled=" + oldLimitterEnabled + ",newLimitterEnabled=" + newLimitterEnabled);
        if (oldLimitterEnabled && !newLimitterEnabled) {
            enableMobileTrafficLimit(false);
            this.mLastMobileTcMode = mode;
        } else if (!oldLimitterEnabled && newLimitterEnabled && this.mIsMobileNwOn) {
            enableMobileTrafficLimit(true);
            this.mLastMobileTcMode = mode;
            isNeedUpdate = true;
        }
        if (isNeedUpdate) {
            Handler handler = this.mHandler;
            handler.sendMessage(handler.obtainMessage(12));
        }
    }

    private void registerMobileTrafficControllerChangedObserver() {
        final ContentObserver observer = new ContentObserver(this.mHandler) { // from class: com.android.server.net.MiuiNetworkPolicyTrafficLimit.4
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                int tcMode = MiuiNetworkPolicyTrafficLimit.this.getUserMobileTcMode();
                MiuiNetworkPolicyTrafficLimit.this.log("tcMode = " + tcMode + " mLastMobileTcMode =" + MiuiNetworkPolicyTrafficLimit.this.mLastMobileTcMode + " mIsMobileNwOn=" + MiuiNetworkPolicyTrafficLimit.this.mIsMobileNwOn);
                if (tcMode != MiuiNetworkPolicyTrafficLimit.this.mLastMobileTcMode) {
                    MiuiNetworkPolicyTrafficLimit.this.enableMobileTcMode(tcMode);
                }
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("mobile_tc_user_enable"), false, observer, -2);
        this.mHandler.post(new Runnable() { // from class: com.android.server.net.MiuiNetworkPolicyTrafficLimit.5
            @Override // java.lang.Runnable
            public void run() {
                observer.onChange(false);
            }
        });
    }

    public int getUserMobileTcMode() {
        int enable = Settings.Global.getInt(this.mContext.getContentResolver(), "mobile_tc_user_enable", 1);
        return enable;
    }

    private synchronized void enableMobileTrafficLimit(final boolean enabled) {
        log("enableMobileTrafficLimit enabled=" + enabled + ",mIface=" + this.mIface);
        if (this.mNetMgrService != null && !TextUtils.isEmpty(this.mIface)) {
            this.mProcessEnableLimit = true;
            this.mHandler.post(new Runnable() { // from class: com.android.server.net.MiuiNetworkPolicyTrafficLimit.6
                @Override // java.lang.Runnable
                public void run() {
                    boolean rst = MiuiNetworkPolicyTrafficLimit.this.mNetMgrService.enableMobileTrafficLimit(enabled, MiuiNetworkPolicyTrafficLimit.this.mIface);
                    if (rst) {
                        MiuiNetworkPolicyTrafficLimit.this.updateTrafficLimitStatus(enabled ? 1 : 0);
                        if (MiuiNetworkPolicyTrafficLimit.this.mMobileTcDropEnabled && MiuiNetworkPolicyTrafficLimit.this.mNetMgrService.setMobileTrafficLimit(true, 0L)) {
                            MiuiNetworkPolicyTrafficLimit.this.updateTrafficLimitStatus(3);
                        }
                    }
                    MiuiNetworkPolicyTrafficLimit.this.mProcessEnableLimit = false;
                    MiuiNetworkPolicyTrafficLimit.this.log("enableMobileTrafficLimit rst=" + rst);
                }
            });
            return;
        }
        log("enableMobileTrafficLimit return by invalid value!!!");
    }

    private synchronized void setMobileTrafficLimit(final boolean enabled, final long rate) {
        log("setMobileTrafficLimit " + enabled + ",rate=" + rate);
        if (this.mNetMgrService == null) {
            log("setMobileTrafficLimit return by invalid value!!!");
            return;
        }
        this.mProcessSetLimit = true;
        this.mHandler.post(new Runnable() { // from class: com.android.server.net.MiuiNetworkPolicyTrafficLimit.7
            @Override // java.lang.Runnable
            public void run() {
                boolean rst = MiuiNetworkPolicyTrafficLimit.this.mNetMgrService.setMobileTrafficLimit(enabled, rate);
                if (rst) {
                    MiuiNetworkPolicyTrafficLimit.this.updateTrafficLimitStatus(enabled ? 3 : 2);
                }
                MiuiNetworkPolicyTrafficLimit.this.mProcessSetLimit = false;
                MiuiNetworkPolicyTrafficLimit.this.log("setMobileTrafficLimit rst=" + rst);
            }
        });
    }

    private synchronized void updateWhiteListUidForMobileTraffic(final int uid, final boolean add) {
        Log.i(TAG, "updateWhiteListUidForMobileTraffic uid=" + uid + ",add=" + add);
        if (this.mNetMgrService == null) {
            log("updateWhiteListUidForMobileTraffic return by invalid value!!!");
        } else {
            this.mHandler.post(new Runnable() { // from class: com.android.server.net.MiuiNetworkPolicyTrafficLimit.8
                @Override // java.lang.Runnable
                public void run() {
                    boolean rst = MiuiNetworkPolicyTrafficLimit.this.mNetMgrService.whiteListUidForMobileTraffic(uid, add);
                    MiuiNetworkPolicyTrafficLimit.this.log("updateWhiteListUidForMobileTraffic rst=" + rst);
                }
            });
        }
    }

    public void updateTrafficLimitStatus(int mode) {
        this.mTrafficLimitMode = mode;
        log("updateTrafficLimitStatus mode=" + mode);
    }

    private int isAllowToSetTrafficRule(boolean enabled, String iface, long rate) {
        int rst = 0;
        log("isAllowToSetTrafficRule enabled=" + enabled + ",mode=" + this.mTrafficLimitMode);
        if (this.mProcessEnableLimit) {
            return 7;
        }
        if (this.mProcessSetLimit) {
            return 8;
        }
        if (enabled) {
            switch (this.mTrafficLimitMode) {
                case 0:
                    rst = 2;
                    break;
                case 1:
                case 2:
                    rst = 1;
                    break;
                case 3:
                    rst = 3;
                    break;
            }
        } else {
            switch (this.mTrafficLimitMode) {
                case 0:
                    rst = 2;
                    break;
                case 1:
                    rst = 4;
                    break;
                case 2:
                    rst = 3;
                    break;
                case 3:
                    rst = 1;
                    break;
            }
        }
        if (rst == 1) {
            if (!isValidMobileIface(iface)) {
                return 6;
            }
            if (!isValidBandWidth(rate)) {
                return 5;
            }
            return rst;
        }
        return rst;
    }

    private void initReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        this.mContext.registerReceiver(this.mReceiver, filter);
    }

    private String getMobileLinkIface() {
        if (this.mCm == null) {
            this.mCm = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        }
        LinkProperties prop = this.mCm.getLinkProperties(0);
        if (prop == null || TextUtils.isEmpty(prop.getInterfaceName())) {
            return "";
        }
        return prop.getInterfaceName();
    }

    private boolean isValidBandWidth(long rate) {
        return rate >= 0;
    }

    private boolean isValidMobileIface(String iface) {
        if (!TextUtils.isEmpty(iface)) {
            if (IS_QCOM && iface.indexOf("rmnet") == -1) {
                return false;
            }
            return true;
        }
        return false;
    }

    public synchronized int setMobileTrafficLimitForGame(boolean enabled, long rate) {
        this.mMobileTcDropEnabled = enabled && rate == 0;
        int rst = isAllowToSetTrafficRule(enabled, this.mIface, rate);
        if (rst != 1) {
            log("setMobileTrafficLimitForGame rst=" + rst + ",mMobileTcDropEnabled=" + this.mMobileTcDropEnabled);
            return rst;
        }
        setMobileTrafficLimit(enabled, rate);
        return 1;
    }

    public String getMobileIface() {
        return this.mIface;
    }

    public boolean getMobileLimitStatus() {
        return this.mMobileLimitEnabled;
    }

    public void updateMobileLimit(boolean enabled) {
        log("updateMobileLimit enabled=" + enabled);
        if (this.mMobileLimitEnabled != enabled) {
            setMobileTrafficLimit(enabled, 0L);
            this.mMobileLimitEnabled = enabled;
        }
    }

    public synchronized void isAllowedToEnableMobileTcMode() {
        String iface = getMobileLinkIface();
        log("isAllowedToEnableMobileTcMode iface=" + iface + ",mIface=" + this.mIface);
        boolean wasMobileNwOn = this.mIsMobileNwOn;
        int i = 0;
        boolean z = !TextUtils.isEmpty(iface);
        this.mIsMobileNwOn = z;
        if (z && !iface.equals(this.mIface)) {
            this.mIface = iface;
        }
        if (this.mIsMobileNwOn != wasMobileNwOn) {
            int tcMode = getUserMobileTcMode();
            if (isLimitterEnabled(tcMode)) {
                if (this.mIsMobileNwOn) {
                    i = tcMode;
                }
                enableMobileTcMode(i);
            }
        }
    }

    public void log(String s) {
        Log.d(TAG, s);
    }
}
