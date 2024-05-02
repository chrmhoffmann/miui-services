package com.android.server.net;

import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.app.IUidObserver;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.wifi.MiuiWifiManager;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.RemoteException;
import android.text.TextUtils;
import com.android.server.MiuiNetworkManagementService;
/* loaded from: classes.dex */
public class MiuiNetworkPolicyServiceSupport {
    private static final boolean DEBUG = false;
    private static final String TAG = "MiuiNetworkPolicySupport";
    private final Context mContext;
    private final Handler mHandler;
    private MiuiWifiManager mMiuiWifiManager;
    private final IUidObserver mUidObserver = new IUidObserver.Stub() { // from class: com.android.server.net.MiuiNetworkPolicyServiceSupport.1
        public void onUidStateChanged(int uid, int procState, long procStateSeq, int capability) throws RemoteException {
            MiuiNetworkPolicyServiceSupport.this.mHandler.sendMessage(MiuiNetworkPolicyServiceSupport.this.mHandler.obtainMessage(1, uid, procState));
        }

        public void onUidGone(int uid, boolean disabled) throws RemoteException {
            MiuiNetworkPolicyServiceSupport.this.mHandler.sendMessage(MiuiNetworkPolicyServiceSupport.this.mHandler.obtainMessage(2, uid, 0));
        }

        public void onUidActive(int uid) throws RemoteException {
        }

        public void onUidIdle(int uid, boolean disabled) throws RemoteException {
        }

        public void onUidProcAdjChanged(int uid) {
        }

        public void onUidCachedChanged(int uid, boolean cached) {
        }
    };
    private final IActivityManager mActivityManager = ActivityManagerNative.getDefault();

    public MiuiNetworkPolicyServiceSupport(Context context, Handler handler) {
        this.mContext = context;
        this.mHandler = handler;
    }

    public void registerUidObserver() {
        try {
            this.mActivityManager.registerUidObserver(this.mUidObserver, 3, -1, (String) null);
        } catch (RemoteException e) {
        }
    }

    public void enablePowerSave(boolean enabled) {
        if (this.mMiuiWifiManager == null) {
            this.mMiuiWifiManager = (MiuiWifiManager) this.mContext.getSystemService("MiuiWifiService");
        }
        this.mMiuiWifiManager.enablePowerSave(enabled);
    }

    public String updateIface(String iface) {
        String newIface;
        WifiManager wm = (WifiManager) this.mContext.getSystemService("wifi");
        ConnectivityManager cm = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        LinkProperties lp = cm.getLinkProperties(wm.getCurrentNetwork());
        if (lp != null && (newIface = lp.getInterfaceName()) != null && !TextUtils.equals(iface, newIface)) {
            MiuiNetworkManagementService.getInstance().updateIface(newIface);
            return newIface;
        }
        return iface;
    }
}
