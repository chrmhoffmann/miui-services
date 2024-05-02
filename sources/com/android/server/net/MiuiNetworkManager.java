package com.android.server.net;

import android.net.IMiuiNetworkManager;
/* loaded from: classes.dex */
public class MiuiNetworkManager extends IMiuiNetworkManager.Stub {
    private static MiuiNetworkManager sSelf;

    public static final MiuiNetworkManager get() {
        if (sSelf == null) {
            sSelf = new MiuiNetworkManager();
        }
        return sSelf;
    }

    public boolean setNetworkTrafficPolicy(int mode) {
        return MiuiNetworkPolicyManagerService.get().setNetworkTrafficPolicy(mode);
    }

    public boolean setRpsStatus(boolean enable) {
        return MiuiNetworkPolicyManagerService.get().setRpsStatus(enable);
    }

    public int setMobileTrafficLimit(boolean enabled, long rate) {
        return MiuiNetworkPolicyManagerService.get().setTrafficControllerForMobile(enabled, rate);
    }

    public boolean setMiuiSlmBpfUid(int uid) {
        return MiuiNetworkPolicyManagerService.get().setMiuiSlmBpfUid(uid);
    }

    public long getMiuiSlmVoipUdpAddress(int uid) {
        return MiuiNetworkPolicyManagerService.get().getMiuiSlmVoipUdpAddress(uid);
    }

    public int getMiuiSlmVoipUdpPort(int uid) {
        return MiuiNetworkPolicyManagerService.get().getMiuiSlmVoipUdpPort(uid);
    }
}
