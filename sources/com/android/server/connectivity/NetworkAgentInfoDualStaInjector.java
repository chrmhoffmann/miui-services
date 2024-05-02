package com.android.server.connectivity;

import android.content.Context;
import android.net.NetworkCapabilities;
/* loaded from: classes.dex */
public class NetworkAgentInfoDualStaInjector {
    private final Context mContext;
    private NetworkCapabilities mNetworkCapabilities;

    public NetworkAgentInfoDualStaInjector(Context ctx, NetworkCapabilities networkCapabilities) {
        this.mContext = ctx;
        this.mNetworkCapabilities = networkCapabilities;
    }

    private boolean isValid() {
        return this.mNetworkCapabilities.hasTransport(1) || this.mNetworkCapabilities.hasTransport(20);
    }

    private boolean isMasterWifi() {
        return this.mNetworkCapabilities.hasTransport(1);
    }

    private boolean slaveIsConnected() {
        return false;
    }

    public boolean isSlaveWifi() {
        return this.mNetworkCapabilities.hasTransport(20);
    }

    public boolean couldBeFullScore() {
        if (!isValid()) {
            return true;
        }
        return (!isMasterWifi() || !slaveIsConnected()) && !isSlaveWifi();
    }

    public void setNetworkCapabilities(NetworkCapabilities nc) {
        this.mNetworkCapabilities = nc;
    }
}
