package com.android.server;

import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import com.android.server.net.MiuiNetworkManager;
import com.miui.base.MiuiStubRegistry;
/* loaded from: classes.dex */
public class NetworkManagermentServiceImp implements NetworkManagementServiceStub {

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<NetworkManagermentServiceImp> {

        /* compiled from: NetworkManagermentServiceImp$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final NetworkManagermentServiceImp INSTANCE = new NetworkManagermentServiceImp();
        }

        public NetworkManagermentServiceImp provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public NetworkManagermentServiceImp provideNewInstance() {
            return new NetworkManagermentServiceImp();
        }
    }

    public IBinder getMiuiNetworkManager() {
        return MiuiNetworkManager.get();
    }

    public void setPidForPackage(String packageName, int pid, int uid) {
        MiuiNetworkManagementService.getInstance().setPidForPackage(packageName, pid, uid);
    }

    public void showLogin(Context context, Intent intent, String ssid) {
    }
}
