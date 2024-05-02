package com.android.server.am;

import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.util.Pair;
import com.miui.base.MiuiStubRegistry;
import java.util.ArrayList;
/* loaded from: classes.dex */
public class AutoStartManagerServiceStubImpl implements AutoStartManagerServiceStub {

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<AutoStartManagerServiceStubImpl> {

        /* compiled from: AutoStartManagerServiceStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final AutoStartManagerServiceStubImpl INSTANCE = new AutoStartManagerServiceStubImpl();
        }

        public AutoStartManagerServiceStubImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public AutoStartManagerServiceStubImpl provideNewInstance() {
            return new AutoStartManagerServiceStubImpl();
        }
    }

    public void signalStopProcessesLocked(ArrayList<Pair<ProcessRecord, Boolean>> procs, boolean allowRestart, String packageName, int uid) {
        AutoStartManagerService.getInstance().signalStopProcessesLocked(procs, allowRestart, packageName, uid);
    }

    public boolean isAllowStartService(Context context, Intent service, int userId) {
        try {
            String packageName = service.getComponent().getPackageName();
            IPackageManager packageManager = AppGlobals.getPackageManager();
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, 0L, userId);
            if (applicationInfo == null) {
                return true;
            }
            int uid = applicationInfo.uid;
            return isAllowStartServiceByUid(context, service, uid);
        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
    }

    private static boolean isAllowStartServiceByUid(Context context, Intent service, int uid) {
        AppOpsManager aom = (AppOpsManager) context.getSystemService("appops");
        if (aom == null) {
            return true;
        }
        int mode = aom.checkOpNoThrow(10008, uid, service.getComponent().getPackageName());
        if (mode == 0) {
            return true;
        }
        return false;
    }
}
