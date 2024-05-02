package com.miui.server.enterprise;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IMessenger;
import android.os.Message;
import android.os.RemoteException;
import android.server.am.SplitScreenReporter;
import android.util.Log;
import com.android.server.pm.PackageManagerService;
import com.miui.enterprise.sdk.IEpInstallPackageObserver;
/* loaded from: classes.dex */
public class ApplicationManagerServiceProxy {
    private static final String TAG = "AMSProxy";

    public static void installPackageAsUser(Context context, PackageManagerService.IPackageManagerImpl pms, String path, final IEpInstallPackageObserver observer, int flag, String installerPkg, int userId) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.miui.securitycore", "com.miui.enterprise.service.EntInstallService"));
        Bundle bundle = new Bundle();
        if (observer != null) {
            bundle.putIBinder("callback", new IMessenger.Stub() { // from class: com.miui.server.enterprise.ApplicationManagerServiceProxy.1
                public void send(Message msg) throws RemoteException {
                    try {
                        Bundle retData = msg.getData();
                        if (retData != null) {
                            String packageName = retData.getString(SplitScreenReporter.STR_PKG);
                            int retCode = retData.getInt("retCode");
                            Log.d(ApplicationManagerServiceProxy.TAG, "onPackageInstalled = " + retCode + " ,pkg= " + packageName);
                            observer.onPackageInstalled(packageName, retCode, (String) null, (Bundle) null);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }.asBinder());
        }
        bundle.putString("apkPath", path);
        bundle.putInt("flag", flag);
        bundle.putString("installerPkg", installerPkg);
        bundle.putInt("userId", userId);
        intent.putExtras(bundle);
        context.startService(intent);
    }
}
