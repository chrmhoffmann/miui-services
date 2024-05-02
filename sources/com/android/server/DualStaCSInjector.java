package com.android.server;

import android.content.Context;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkInfoFeatureImpl;
import android.os.Binder;
import android.os.SystemProperties;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.server.am.SplitScreenReporter;
import java.util.ArrayList;
import java.util.Iterator;
/* loaded from: classes.dex */
public class DualStaCSInjector {
    private static final String TAG = "DualStaCSInjector";
    private static final NetworkInfoFeatureImpl networkInfoFeature = new NetworkInfoFeatureImpl();
    private boolean IS_CTS_MODE = false;
    private final CloudUtils mCloudUtils;
    private final ConnectivityService mConnectivityService;
    private final Context mContext;

    public DualStaCSInjector(Context context, ConnectivityService cs) {
        this.mContext = context;
        this.mConnectivityService = cs;
        this.mCloudUtils = new CloudUtils(context, TAG);
        registerMiuiOptimizationObserver();
    }

    public NetworkInfo getActiveNetworkInfo(NetworkInfo ni, int uid) {
        if (uid == 1000 || this.IS_CTS_MODE || this.mCloudUtils.isUidInSlaveOnlyBlackList(uid)) {
            return ni;
        }
        if (ni != null && ni.getType() == 30) {
            return typeConvert(ni, 1);
        }
        return ni;
    }

    public NetworkInfo getNetworkInfoForType(NetworkInfo ni, int networkType, int uid) {
        NetworkInfo slaveInfo;
        if (!shouldConvert(uid)) {
            return ni;
        }
        if (networkType == 1 && (slaveInfo = this.mConnectivityService.getNetworkInfo(30)) != null) {
            return typeConvert(slaveInfo, 1);
        }
        return ni;
    }

    public ArrayList<NetworkInfo> getAllNetworkInfo(ArrayList<NetworkInfo> networkInfos, int uid) {
        if (!shouldConvert(uid)) {
            return networkInfos;
        }
        long ident = Binder.clearCallingIdentity();
        try {
            NetworkInfo active = this.mConnectivityService.getActiveNetworkInfo();
            if (active != null && active.getType() == 30) {
                Iterator<NetworkInfo> it = networkInfos.iterator();
                while (it.hasNext()) {
                    NetworkInfo ni = it.next();
                    if (ni.getType() == 30) {
                        ni.setType(1);
                        networkInfoFeature.setTypeName(ni, ConnectivityManager.getNetworkTypeName(1));
                    }
                }
            }
            return networkInfos;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public int translateNetworkType(int networkType, int uid) {
        if (!shouldConvert(uid)) {
            return networkType;
        }
        if (networkType == 1) {
            return 30;
        }
        return networkType;
    }

    public NetworkCapabilities getNetworkCapabilities(NetworkCapabilities nc, int uid) {
        if (nc == null) {
            return nc;
        }
        if (shouldConvert(uid) && nc.hasTransport(20)) {
            nc.addTransportType(1);
        }
        return nc;
    }

    private NetworkInfo typeConvert(NetworkInfo oldInfo, int targetType) {
        NetworkInfo newInfo = new NetworkInfo(oldInfo);
        newInfo.setType(targetType);
        networkInfoFeature.setTypeName(newInfo, ConnectivityManager.getNetworkTypeName(targetType));
        return newInfo;
    }

    private boolean shouldConvert(int uid) {
        long id = Binder.clearCallingIdentity();
        NetworkInfo active = this.mConnectivityService.getActiveNetworkInfo();
        Binder.restoreCallingIdentity(id);
        if (active != null && active.getType() == 30 && !isUidInBlackList(uid) && !this.IS_CTS_MODE) {
            return true;
        }
        return false;
    }

    private boolean isUidInBlackList(int uid) {
        if (uid == 1000 || this.mCloudUtils.isUidInSlaveOnlyBlackList(uid)) {
            return true;
        }
        return false;
    }

    private void registerMiuiOptimizationObserver() {
        ContentObserver observer = new ContentObserver(null) { // from class: com.android.server.DualStaCSInjector.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                DualStaCSInjector.this.IS_CTS_MODE = !SystemProperties.getBoolean("persist.sys.miui_optimization", !SplitScreenReporter.ACTION_ENTER_SPLIT.equals(SystemProperties.get("ro.miui.cts")));
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(MiuiSettings.Secure.MIUI_OPTIMIZATION), false, observer, -2);
        observer.onChange(false);
    }
}
