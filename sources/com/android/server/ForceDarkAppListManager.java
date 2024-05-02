package com.android.server;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.ServiceManager;
import android.provider.Settings;
import android.text.TextUtils;
import com.android.server.ForceDarkAppListProvider;
import com.miui.darkmode.DarkModeAppData;
import com.miui.darkmode.DarkModeAppDetailInfo;
import com.miui.server.SecurityManagerService;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import miui.os.Build;
/* loaded from: classes.dex */
public class ForceDarkAppListManager {
    private Context mContext;
    private ForceDarkAppListProvider mForceDarkAppListProvider;
    private HashMap<String, DarkModeAppSettingsInfo> mForceDarkAppSettings;
    private ForceDarkUiModeModeManager mForceDarkUiModeModeManager;
    private PackageManager mPackageManager;
    private SecurityManagerService mSecurityManagerService;
    private SoftReference<ArrayList<PackageInfo>> mAppListCacheRef = new SoftReference<>(new ArrayList());
    private DarkModeAppData mDarkModeAppData = new DarkModeAppData();

    public ForceDarkAppListManager(Context context, ForceDarkUiModeModeManager forceDarkUiModeModeManager) {
        ForceDarkAppListProvider forceDarkAppListProvider = ForceDarkAppListProvider.getInstance();
        this.mForceDarkAppListProvider = forceDarkAppListProvider;
        this.mForceDarkAppSettings = forceDarkAppListProvider.getForceDarkAppSettings();
        this.mForceDarkAppListProvider.setAppListChangeListener(new ForceDarkAppListProvider.ForceDarkAppListChangeListener() { // from class: com.android.server.ForceDarkAppListManager.1
            @Override // com.android.server.ForceDarkAppListProvider.ForceDarkAppListChangeListener
            public void onChange() {
                ForceDarkAppListManager forceDarkAppListManager = ForceDarkAppListManager.this;
                forceDarkAppListManager.mForceDarkAppSettings = forceDarkAppListManager.mForceDarkAppListProvider.getForceDarkAppSettings();
                ForceDarkAppListManager.this.mForceDarkUiModeModeManager.notifyAppListChanged();
                ForceDarkAppListManager.this.clearCache();
            }
        });
        this.mPackageManager = context.getPackageManager();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.PACKAGE_REMOVED");
        filter.addAction("android.intent.action.PACKAGE_REPLACED");
        filter.addAction("android.intent.action.PACKAGE_ADDED");
        filter.addDataScheme("package");
        context.registerReceiver(new AppChangedReceiver(), filter);
        context.registerReceiver(new LocaleChangedReceiver(), new IntentFilter("android.intent.action.LOCALE_CHANGED"));
        this.mContext = context;
        this.mForceDarkUiModeModeManager = forceDarkUiModeModeManager;
    }

    public void onBootPhase(int phase, Context context) {
        this.mSecurityManagerService = (SecurityManagerService) ServiceManager.getService("security");
        this.mForceDarkAppListProvider.onBootPhase(phase, context);
    }

    public void setAppDarkModeEnable(String packageName, boolean enable, int userId) {
        if (TextUtils.isEmpty(packageName)) {
            return;
        }
        Settings.System.putString(this.mContext.getContentResolver(), "last_app_dark_mode_pkg", packageName + ":" + enable);
        this.mSecurityManagerService.setAppDarkModeForUser(packageName, enable, userId);
        DarkModeAppData darkModeAppData = this.mDarkModeAppData;
        if (darkModeAppData != null) {
            List<DarkModeAppDetailInfo> darkModeAppDetailInfoList = darkModeAppData.getDarkModeAppDetailInfoList();
            for (int i = 0; i < darkModeAppDetailInfoList.size(); i++) {
                if (packageName.equals(darkModeAppDetailInfoList.get(i).getPkgName())) {
                    darkModeAppDetailInfoList.get(i).setEnabled(enable);
                    this.mDarkModeAppData.setCreateTime(System.currentTimeMillis());
                    return;
                }
            }
        }
    }

    public boolean getAppDarkModeEnable(String packageName, int userId) {
        if (TextUtils.isEmpty(packageName)) {
            return false;
        }
        DarkModeAppSettingsInfo darkModeAppSettingsInfo = this.mForceDarkAppSettings.get(packageName);
        if (darkModeAppSettingsInfo != null && !darkModeAppSettingsInfo.isShowInSettings()) {
            int overrideEnableValue = darkModeAppSettingsInfo.getOverrideEnableValue();
            if (overrideEnableValue == 1) {
                return true;
            }
            if (overrideEnableValue == 2) {
                return false;
            }
        }
        return this.mSecurityManagerService.getAppDarkModeForUser(packageName, userId);
    }

    public boolean getAppForceDarkOrigin(String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return true;
        }
        DarkModeAppSettingsInfo darkModeAppSettingsInfo = this.mForceDarkAppSettings.get(packageName);
        if (darkModeAppSettingsInfo != null) {
            return darkModeAppSettingsInfo.isForceDarkOrigin();
        }
        return this.mDarkModeAppData.getInfoWithPackageName(packageName) == null;
    }

    public boolean getShouldInterceptRelaunch(String packageName, int userId) {
        DarkModeAppSettingsInfo settingsInfo;
        if (!TextUtils.isEmpty(packageName) && (settingsInfo = this.mForceDarkAppSettings.get(packageName)) != null) {
            if (settingsInfo.getInterceptRelaunch() == 1) {
                return true;
            }
            return settingsInfo.getInterceptRelaunch() != 2 && settingsInfo.isShowInSettings() && this.mSecurityManagerService.getAppDarkModeForUser(packageName, userId);
        }
        return false;
    }

    public DarkModeAppData getDarkModeAppList(long appLastUpdateTime, int userId) {
        if (appLastUpdateTime > 0 && appLastUpdateTime == this.mDarkModeAppData.getCreateTime()) {
            return null;
        }
        List<DarkModeAppDetailInfo> darkModeAppDetailInfos = new ArrayList<>();
        List<PackageInfo> installedPkgList = getInstalledPkgList();
        for (PackageInfo packageInfo : installedPkgList) {
            DarkModeAppSettingsInfo darkModeAppSettingsInfo = this.mForceDarkAppSettings.get(packageInfo.applicationInfo.packageName);
            if (darkModeAppSettingsInfo != null) {
                if (darkModeAppSettingsInfo.isShowInSettings()) {
                    DarkModeAppDetailInfo darkModeAppDetailInfo = getAppInfo(darkModeAppSettingsInfo, packageInfo.applicationInfo, userId);
                    darkModeAppDetailInfos.add(darkModeAppDetailInfo);
                }
            } else if (Build.IS_INTERNATIONAL_BUILD && shouldShowInSettings(packageInfo.applicationInfo)) {
                DarkModeAppDetailInfo darkModeAppDetailInfo2 = getAppInfo(new DarkModeAppSettingsInfo(), packageInfo.applicationInfo, userId);
                darkModeAppDetailInfos.add(darkModeAppDetailInfo2);
            }
        }
        this.mDarkModeAppData.setCreateTime(System.currentTimeMillis());
        this.mDarkModeAppData.setDarkModeAppDetailInfoList(darkModeAppDetailInfos);
        return this.mDarkModeAppData;
    }

    private DarkModeAppDetailInfo getAppInfo(DarkModeAppSettingsInfo darkModeAppSettingsInfo, ApplicationInfo applicationInfo, int userId) {
        String label = applicationInfo.loadLabel(this.mPackageManager).toString();
        DarkModeAppDetailInfo appInfo = new DarkModeAppDetailInfo();
        appInfo.setLabel(label);
        appInfo.setPkgName(applicationInfo.packageName);
        appInfo.setEnabled(this.mSecurityManagerService.getAppDarkModeForUser(applicationInfo.packageName, userId));
        appInfo.setAdaptStatus(darkModeAppSettingsInfo.getAdaptStat());
        return appInfo;
    }

    public void clearCache() {
        this.mDarkModeAppData.setCreateTime(0L);
        this.mAppListCacheRef.clear();
    }

    private List<PackageInfo> getInstalledPkgList() {
        ArrayList<PackageInfo> appList = this.mAppListCacheRef.get();
        if (appList != null && !appList.isEmpty()) {
            return appList;
        }
        ArrayList<PackageInfo> appList2 = new ArrayList<>();
        appList2.addAll(this.mPackageManager.getInstalledPackages(64));
        this.mAppListCacheRef = new SoftReference<>(appList2);
        return appList2;
    }

    public HashMap<String, Integer> getAllForceDarkMapForSplashScreen() {
        HashMap<String, Integer> result = new HashMap<>();
        for (Map.Entry<String, DarkModeAppSettingsInfo> settingsInfo : this.mForceDarkAppSettings.entrySet()) {
            int forceDarkSplashScreenValue = settingsInfo.getValue().getForceDarkSplashScreen();
            if (forceDarkSplashScreenValue != 1 && forceDarkSplashScreenValue != 0) {
                result.put(settingsInfo.getKey(), Integer.valueOf(forceDarkSplashScreenValue));
            }
        }
        return result;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class AppChangedReceiver extends BroadcastReceiver {
        private AppChangedReceiver() {
            ForceDarkAppListManager.this = r1;
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) {
                return;
            }
            if (intent.getAction().equals("android.intent.action.PACKAGE_REMOVED") || intent.getAction().equals("android.intent.action.PACKAGE_REPLACED")) {
                Uri data = intent.getData();
                if (data == null) {
                    return;
                }
            }
            ForceDarkAppListManager.this.clearCache();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class LocaleChangedReceiver extends BroadcastReceiver {
        private LocaleChangedReceiver() {
            ForceDarkAppListManager.this = r1;
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            ForceDarkAppListManager.this.clearCache();
        }
    }

    private boolean shouldShowInSettings(ApplicationInfo info) {
        return info != null && !info.isSystemApp() && info.uid > 10000 && !info.packageName.startsWith("com.miui.") && !info.packageName.startsWith("com.xiaomi.") && !info.packageName.startsWith("com.mi.") && !info.packageName.startsWith("com.google.");
    }
}
