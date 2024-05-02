package com.android.server.net;

import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.net.INetworkStatsService;
import android.net.INetworkStatsSession;
import android.net.NetworkStats;
import android.net.NetworkTemplate;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.Log;
import com.android.server.MiuiBatteryStatsService;
import java.util.HashMap;
import java.util.Map;
/* loaded from: classes.dex */
public class NetworkStatsReporter implements AlarmManager.OnAlarmListener {
    private static final long AlARM_INTERVAL = 43200000;
    private static final int FLAG_NON_ANONYMOUS = 2;
    private static final long HOUR_IN_MILLIS = 3600000;
    private static final String MOBILE_RX_BYTES = "mobile_rx_bytes";
    private static final String MOBILE_TOTAL_BYTES = "mobile_total_bytes";
    private static final String MOBILE_TX_BYTES = "mobile_tx_bytes";
    private static final int MSG_TIMER = 0;
    private static final String NETWORK_RX_BYTES = "network_rx_bytes";
    private static final String NETWORK_STATS_EVENT_NAME = "background_network_statistics";
    private static final String NETWORK_TOTAL_BYTES = "network_total_bytes";
    private static final String NETWORK_TX_BYTES = "network_tx_bytes";
    private static final String ONETRACK_APP_ID = "31000000072";
    private static final String ONETRACK_PACKAGE = "com.android.server.net";
    private static final String PACKAGE_NAME = "background_package";
    private static final String WIFI_RX_BYTES = "wifi_rx_bytes";
    private static final String WIFI_TOTAL_BYTES = "wifi_total_bytes";
    private static final String WIFI_TX_BYTES = "wifi_tx_bytes";
    private static NetworkStatsReporter sSelf;
    private AlarmManager mAlarmManager;
    private Context mContext;
    private Handler mHandler;
    private NetworkTemplate mMobileTemplate;
    private INetworkStatsService mStatsService;
    private NetworkTemplate mWifiTemplate;
    private static final String TAG = NetworkStatsReporter.class.getSimpleName();
    private static final String DEVICE_REGION = SystemProperties.get("ro.miui.region", "CN");
    private long mStartAlarmTime = 0;
    HashMap<Integer, BackgroundNetworkStats> mNetworkStatsMap = new HashMap<>();
    private Handler.Callback mHandlerCallback = new Handler.Callback() { // from class: com.android.server.net.NetworkStatsReporter.1
        @Override // android.os.Handler.Callback
        public boolean handleMessage(Message message) {
            switch (message.what) {
                case 0:
                    NetworkStatsReporter.this.updateNetworkStats();
                    NetworkStatsReporter.this.reportNetworkStats();
                    NetworkStatsReporter.this.startAlarm();
                    NetworkStatsReporter.this.clear();
                    return false;
                default:
                    return false;
            }
        }
    };

    private NetworkStatsReporter(Context context) {
        if (context == null) {
            Log.e(TAG, "context is null, NetworkStatsReporter init failed");
            return;
        }
        this.mContext = context;
        HandlerThread thread = new HandlerThread(TAG);
        thread.start();
        this.mHandler = new Handler(thread.getLooper(), this.mHandlerCallback);
        this.mStatsService = INetworkStatsService.Stub.asInterface(ServiceManager.getService("netstats"));
        this.mWifiTemplate = NetworkTemplate.buildTemplateWifiWildcard();
        this.mMobileTemplate = NetworkTemplate.buildTemplateMobileWildcard();
        this.mAlarmManager = (AlarmManager) this.mContext.getSystemService("alarm");
        startAlarm();
    }

    public static NetworkStatsReporter make(Context context) {
        if (sSelf == null) {
            sSelf = new NetworkStatsReporter(context);
        }
        return sSelf;
    }

    public void updateNetworkStats() {
        try {
            INetworkStatsSession networkStatsSession = this.mStatsService.openSession();
            if (networkStatsSession == null) {
                Log.e(TAG, "networkStatsSession is null");
                return;
            }
            getBackgroundNetworkStats(this.mMobileTemplate, networkStatsSession);
            getBackgroundNetworkStats(this.mWifiTemplate, networkStatsSession);
            try {
                networkStatsSession.close();
            } catch (Exception e) {
                Log.e(TAG, "open session exception:" + e);
            }
        } catch (Exception e2) {
            Log.e(TAG, "open session exception:" + e2);
        }
    }

    private void getBackgroundNetworkStats(NetworkTemplate networkTemplate, INetworkStatsSession networkStatsSession) {
        int uid;
        BackgroundNetworkStats backgroundNetworkStats;
        if (networkTemplate == null) {
            Log.e(TAG, "networkTemplate is null");
            return;
        }
        long now = System.currentTimeMillis();
        try {
            NetworkStats networkStats = networkStatsSession.getSummaryForAllUid(networkTemplate, this.mStartAlarmTime, now, false);
            if (networkStats == null || networkStats.size() == 0) {
                return;
            }
            NetworkStats.Entry entry = new NetworkStats.Entry();
            int size = networkStats.size();
            for (int i = 0; i < size; i++) {
                entry = networkStats.getValues(i, entry);
                if (entry != null && entry.set == 0 && (uid = entry.uid) > 1000) {
                    String packageName = getPackageNameByUid(uid);
                    if (this.mNetworkStatsMap.containsKey(Integer.valueOf(uid))) {
                        backgroundNetworkStats = this.mNetworkStatsMap.get(Integer.valueOf(uid));
                    } else {
                        backgroundNetworkStats = new BackgroundNetworkStats(uid, packageName);
                    }
                    backgroundNetworkStats.updateBytes(networkTemplate, entry);
                    this.mNetworkStatsMap.put(Integer.valueOf(uid), backgroundNetworkStats);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "getSummaryForAllUid exception" + e);
        }
    }

    public void reportNetworkStats() {
        for (Map.Entry<Integer, BackgroundNetworkStats> entry : this.mNetworkStatsMap.entrySet()) {
            BackgroundNetworkStats networkStats = entry.getValue();
            if (networkStats != null && !isNumericOrNull(networkStats.mPackageName)) {
                Bundle params = new Bundle();
                params.putString(PACKAGE_NAME, networkStats.mPackageName);
                params.putLong(WIFI_TX_BYTES, networkStats.mWifiTxBytes);
                params.putLong(WIFI_RX_BYTES, networkStats.mWifiRxBytes);
                params.putLong(WIFI_TOTAL_BYTES, networkStats.mWifiTotalBytes);
                params.putLong(MOBILE_TX_BYTES, networkStats.mMobileTxBytes);
                params.putLong(MOBILE_RX_BYTES, networkStats.mMobileRxBytes);
                params.putLong(MOBILE_TOTAL_BYTES, networkStats.mMobileTotalBytes);
                params.putLong(NETWORK_TX_BYTES, networkStats.mTotalTxBytes);
                params.putLong(NETWORK_RX_BYTES, networkStats.mTotalRxBytes);
                params.putLong(NETWORK_TOTAL_BYTES, networkStats.mTotalBytes);
                reportNetworkStatsEvent(this.mContext, NETWORK_STATS_EVENT_NAME, params);
            }
        }
    }

    private void reportNetworkStatsEvent(Context context, String eventName, Bundle params) {
        if (context == null || !DEVICE_REGION.equals("CN")) {
            return;
        }
        try {
            Intent intent = new Intent(MiuiBatteryStatsService.TrackBatteryUsbInfo.ACTION_TRACK_EVENT);
            intent.setPackage(MiuiBatteryStatsService.TrackBatteryUsbInfo.ANALYTICS_PACKAGE);
            intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_APP_ID, ONETRACK_APP_ID);
            intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_PACKAGE, ONETRACK_PACKAGE);
            intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_EVENT_NAME, eventName);
            intent.putExtras(params);
            intent.setFlags(2);
            context.startService(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startAlarm() {
        this.mStartAlarmTime = System.currentTimeMillis();
        this.mAlarmManager.set(2, SystemClock.elapsedRealtime() + AlARM_INTERVAL, TAG, this, this.mHandler);
    }

    @Override // android.app.AlarmManager.OnAlarmListener
    public void onAlarm() {
        Handler handler = this.mHandler;
        if (handler != null) {
            handler.sendMessage(handler.obtainMessage(0));
        }
    }

    public void clear() {
        this.mNetworkStatsMap.clear();
    }

    private String getPackageNameByUid(int uid) {
        String[] pkgs = this.mContext.getPackageManager().getPackagesForUid(uid);
        if (pkgs != null && pkgs.length > 0) {
            String packageName = pkgs[0];
            return packageName;
        }
        String packageName2 = Integer.toString(uid);
        return packageName2;
    }

    public boolean isNumericOrNull(String str) {
        if (str != null) {
            for (int i = 0; i < str.length(); i++) {
                if (!Character.isDigit(str.charAt(i))) {
                    return false;
                }
            }
            return true;
        }
        return true;
    }

    /* loaded from: classes.dex */
    public class BackgroundNetworkStats {
        private long mMobileRxBytes;
        private long mMobileTotalBytes;
        private long mMobileTxBytes;
        private String mPackageName;
        private long mTotalBytes;
        private long mTotalRxBytes;
        private long mTotalTxBytes;
        private int mUid;
        private long mWifiRxBytes;
        private long mWifiTotalBytes;
        private long mWifiTxBytes;

        public BackgroundNetworkStats(int uid, String packageName) {
            NetworkStatsReporter.this = r1;
            this.mUid = uid;
            this.mPackageName = packageName;
        }

        public void updateBytes(NetworkTemplate networkTemplate, NetworkStats.Entry entry) {
            switch (networkTemplate.getMatchRule()) {
                case 1:
                    this.mMobileTxBytes += entry.txBytes;
                    long j = this.mMobileRxBytes + entry.rxBytes;
                    this.mMobileRxBytes = j;
                    this.mMobileTotalBytes = this.mMobileTxBytes + j;
                    break;
                case 4:
                    this.mWifiTxBytes += entry.txBytes;
                    long j2 = this.mWifiRxBytes + entry.rxBytes;
                    this.mWifiRxBytes = j2;
                    this.mWifiTotalBytes = this.mWifiTxBytes + j2;
                    break;
            }
            this.mTotalTxBytes += entry.txBytes;
            long j3 = this.mTotalRxBytes + entry.rxBytes;
            this.mTotalRxBytes = j3;
            this.mTotalBytes = this.mTotalTxBytes + j3;
        }
    }
}
