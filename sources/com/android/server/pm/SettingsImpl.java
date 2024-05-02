package com.android.server.pm;

import android.app.ActivityManager;
import android.app.IActivityManager;
import android.app.IApplicationThread;
import android.content.IIntentReceiver;
import android.content.Intent;
import android.miui.AppOpsUtils;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import com.android.server.pm.pkg.component.ParsedMainComponent;
import com.miui.base.MiuiStubRegistry;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import miui.os.Build;
import miui.securityspace.XSpaceConstant;
import miui.securityspace.XSpaceUserHandle;
/* loaded from: classes.dex */
public class SettingsImpl extends SettingsStub {
    private static final String ANDROID_INSTALLER = "com.android.packageinstaller";
    private static final String GOOGLE_INSTALLER = "com.google.android.packageinstaller";
    private static final String MIUI_ACTION_PACKAGE_FIRST_LAUNCH = "miui.intent.action.PACKAGE_FIRST_LAUNCH";
    private static final String MIUI_INSTALLER = "com.miui.packageinstaller";
    private static final String MIUI_PERMISSION = "miui.permission.USE_INTERNAL_GENERAL_API";
    private static final String TAG = SettingsImpl.class.getSimpleName();

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<SettingsImpl> {

        /* compiled from: SettingsImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final SettingsImpl INSTANCE = new SettingsImpl();
        }

        public SettingsImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public SettingsImpl provideNewInstance() {
            return new SettingsImpl();
        }
    }

    private static boolean findComponent(List<? extends ParsedMainComponent> components, String targetComponent) {
        if (components == null) {
            return false;
        }
        for (ParsedMainComponent component : components) {
            if (TextUtils.equals(component.getClassName(), targetComponent)) {
                return true;
            }
        }
        return false;
    }

    public boolean checkXSpaceApp(PackageSetting ps, int userHandle) {
        if (XSpaceUserHandle.isXSpaceUserId(userHandle)) {
            if (XSpaceConstant.REQUIRED_APPS.contains(ps.getPkg().getPackageName())) {
                ps.setInstalled(true, userHandle);
            } else {
                ps.setInstalled(false, userHandle);
            }
            if (XSpaceConstant.SPECIAL_APPS.containsKey(ps.getPkg().getPackageName())) {
                ArrayList<String> requiredComponent = (ArrayList) XSpaceConstant.SPECIAL_APPS.get(ps.getPkg().getPackageName());
                ArrayList<ParsedMainComponent> components = new ArrayList<>();
                components.addAll(ps.getPkg().getActivities());
                components.addAll(ps.getPkg().getServices());
                components.addAll(ps.getPkg().getReceivers());
                components.addAll(ps.getPkg().getProviders());
                Iterator<ParsedMainComponent> it = components.iterator();
                while (it.hasNext()) {
                    ParsedMainComponent component = it.next();
                    if (!requiredComponent.contains(component.getClassName())) {
                        ps.addDisabledComponent(component.getClassName(), userHandle);
                    }
                }
            }
            return true;
        }
        if (!Build.IS_INTERNATIONAL_BUILD && !Build.IS_TABLET) {
            if (MIUI_INSTALLER.equals(ps.getPkg().getPackageName())) {
                ps.setInstalled(!AppOpsUtils.isXOptMode(), userHandle);
                return true;
            } else if (GOOGLE_INSTALLER.equals(ps.getPkg().getPackageName())) {
                ps.setInstalled(AppOpsUtils.isXOptMode(), userHandle);
                return true;
            } else if (ANDROID_INSTALLER.equals(ps.getName())) {
                ps.setInstalled(AppOpsUtils.isXOptMode(), userHandle);
                return true;
            }
        }
        return false;
    }

    public void noftifyFirstLaunch(PackageManagerService pms, final PackageSetting pkgSetting, final int userId) {
        if (pkgSetting == null || pkgSetting.isSystem()) {
            return;
        }
        pms.mHandler.post(new Runnable() { // from class: com.android.server.pm.SettingsImpl$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                SettingsImpl.lambda$noftifyFirstLaunch$0(pkgSetting, userId);
            }
        });
    }

    public static /* synthetic */ void lambda$noftifyFirstLaunch$0(PackageSetting pkgSetting, int userId) {
        try {
            Log.i(TAG, "notify first launch");
            Intent intent = new Intent(MIUI_ACTION_PACKAGE_FIRST_LAUNCH);
            intent.putExtra("package", pkgSetting.getName());
            if (!TextUtils.isEmpty(pkgSetting.getInstallSource().installerPackageName)) {
                intent.putExtra("installer", pkgSetting.getInstallSource().installerPackageName);
            }
            intent.putExtra("userId", userId);
            intent.addFlags(16777216);
            IActivityManager am = ActivityManager.getService();
            String[] requiredPermissions = {MIUI_PERMISSION};
            am.broadcastIntentWithFeature((IApplicationThread) null, "FirstLaunch", intent, (String) null, (IIntentReceiver) null, 0, (String) null, (Bundle) null, requiredPermissions, (String[]) null, (String[]) null, -1, (Bundle) null, false, false, 0);
        } catch (Throwable t) {
            Log.e(TAG, "notify first launch exception", t);
        }
    }
}
