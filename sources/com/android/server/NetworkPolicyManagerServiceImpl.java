package com.android.server;

import android.content.Context;
import android.net.INetd;
import android.os.RemoteException;
import android.util.Slog;
import com.android.server.net.MiuiNetworkPolicyManagerService;
import com.miui.base.MiuiStubRegistry;
/* loaded from: classes.dex */
public class NetworkPolicyManagerServiceImpl implements NetworkPolicyManagerServiceStub {
    private static final String TAG = "NetworkManagementServic";
    private MiuiNetworkManagementService mMiNMS;
    MiuiNetworkPolicyManagerService mMiuiNetPolicyManager;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<NetworkPolicyManagerServiceImpl> {

        /* compiled from: NetworkPolicyManagerServiceImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final NetworkPolicyManagerServiceImpl INSTANCE = new NetworkPolicyManagerServiceImpl();
        }

        public NetworkPolicyManagerServiceImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public NetworkPolicyManagerServiceImpl provideNewInstance() {
            return new NetworkPolicyManagerServiceImpl();
        }
    }

    public void initMiuiNMS(Context mContext) {
        this.mMiNMS = MiuiNetworkManagementService.Init(mContext);
    }

    public boolean miuiNotifyInterfaceClassActivity(int type, boolean isActive, long tsNanos, int uid, boolean fromRadio) {
        return this.mMiNMS.miuiNotifyInterfaceClassActivity(type, isActive, tsNanos, uid, fromRadio);
    }

    public void setOemNetd(INetd mNetdService) {
        try {
            this.mMiNMS.setOemNetd(mNetdService.getOemNetd());
        } catch (RemoteException e) {
            Slog.e(TAG, "### setOemNetd failed ###");
            e.printStackTrace();
        }
    }

    public void init(Context mContext) {
        this.mMiuiNetPolicyManager = MiuiNetworkPolicyManagerService.make(mContext);
    }

    public void systemReady() {
        this.mMiuiNetPolicyManager.systemReady();
    }
}
