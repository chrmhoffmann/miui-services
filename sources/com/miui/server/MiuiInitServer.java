package com.miui.server;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.MiuiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import com.android.server.SystemService;
import com.android.server.pm.CloudControlPreinstallService;
import com.android.server.pm.PreinstallApp;
import com.android.server.wm.WindowManagerService;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import miui.content.res.GlobalConfiguration;
import miui.os.Build;
import miui.os.IMiuiInit;
import miui.os.IMiuiInitObserver;
import miui.util.CustomizeUtil;
import miui.util.FeatureParser;
import miui.util.MiuiFeatureUtils;
/* loaded from: classes.dex */
public class MiuiInitServer extends IMiuiInit.Stub {
    private static final String CARRIER_REGION_PROP_FILE_NAME = "region_specified.prop";
    private static final String CUST_PROPERTIES_FILE_NAME = "cust.prop";
    private static final String PREINSTALL_APP_HISTORY_FILE = "/data/app/preinstall_history";
    private static final String PREINSTALL_PACKAGE_LIST = "/data/system/preinstall.list";
    private static final String TAG = "MiuiInitServer";
    MiuiCompatModePackages mCompatModePackages;
    private final Context mContext;
    private boolean mDoing;
    boolean mNeedAspectSettings;
    private HashMap<String, String> mPreinstallHistoryMap;
    private ArrayList<String> mPreinstalledChannels;

    /* loaded from: classes.dex */
    public static final class Lifecycle extends SystemService {
        private final MiuiInitServer mService;

        public Lifecycle(Context context) {
            super(context);
            this.mService = new MiuiInitServer(context);
        }

        public void onStart() {
            publishBinderService("MiuiInit", this.mService);
        }
    }

    public MiuiInitServer(Context context) {
        this.mContext = context;
        EnableStateManager.updateApplicationEnableState(context);
        deletePackagesByRegion();
        MiuiFeatureUtils.setMiuisdkProperties();
        WindowManagerService windowManager = ServiceManager.getService("window");
        boolean z = false;
        boolean hasNavigationBar = windowManager.hasNavigationBar(0);
        this.mNeedAspectSettings = hasNavigationBar;
        if (hasNavigationBar && !Build.IS_TABLET) {
            z = true;
        }
        this.mNeedAspectSettings = z;
        if (z || CustomizeUtil.HAS_NOTCH) {
            this.mCompatModePackages = new MiuiCompatModePackages(context);
        }
    }

    /* loaded from: classes.dex */
    private class InitCustEnvironmentTask extends Thread {
        private String mCustVarinat;
        private IMiuiInitObserver mObs;

        InitCustEnvironmentTask(String custVariant, IMiuiInitObserver obs) {
            MiuiInitServer.this = r1;
            this.mCustVarinat = custVariant;
            this.mObs = obs;
        }

        @Override // java.lang.Thread, java.lang.Runnable
        public void run() {
            boolean ret = initCustEnvironment(this.mCustVarinat);
            IMiuiInitObserver iMiuiInitObserver = this.mObs;
            if (iMiuiInitObserver != null) {
                try {
                    iMiuiInitObserver.initDone(ret);
                } catch (RemoteException e) {
                }
            }
            MiuiInitServer.this.mDoing = false;
            try {
                Configuration curConfig = GlobalConfiguration.get();
                MiuiConfiguration extraConfig = curConfig.getExtraConfig();
                MiuiConfiguration extraConfig2 = extraConfig instanceof MiuiConfiguration ? extraConfig : null;
                if (extraConfig2 != null) {
                    extraConfig2.updateTheme(0L);
                }
                GlobalConfiguration.update(curConfig);
            } catch (RemoteException e2) {
                e2.printStackTrace();
            }
            EnableStateManager.updateApplicationEnableState(MiuiInitServer.this.mContext);
            MiuiInitServer.this.deletePackagesByRegion();
            MiuiInitServer.this.mContext.sendBroadcast(new Intent("miui.intent.action.MIUI_INIT_COMPLETED"), "miui.os.permisson.INIT_MIUI_ENVIRONMENT");
            Intent intent = new Intent("miui.intent.action.MIUI_REGION_CHANGED");
            intent.addFlags(16777216);
            intent.putExtra(CloudControlPreinstallService.ConnectEntity.REGION, Build.getRegion());
            MiuiInitServer.this.mContext.sendBroadcast(intent);
        }

        private boolean isTimeZoneAuto() {
            try {
                return Settings.Global.getInt(MiuiInitServer.this.mContext.getContentResolver(), "auto_time_zone") > 0;
            } catch (Settings.SettingNotFoundException e) {
                Log.i(MiuiInitServer.TAG, "AUTO_TIME_ZONE can't found : " + e);
                return false;
            }
        }

        private boolean isDeviceNotInProvision() {
            return Settings.Secure.getInt(MiuiInitServer.this.mContext.getContentResolver(), "device_provisioned", 0) != 0;
        }

        private boolean initCustEnvironment(String custVariant) {
            CustomizeUtil.setMiuiCustVariatDir(custVariant);
            File custVariantDir = CustomizeUtil.getMiuiCustVariantDir(true);
            File carrierPropDir = CustomizeUtil.geCarrierRegionPropDir();
            if (custVariantDir == null) {
                return false;
            }
            importCustProperties(new File(custVariantDir, MiuiInitServer.CUST_PROPERTIES_FILE_NAME), isTimeZoneAuto());
            importCustProperties(new File(carrierPropDir, MiuiInitServer.CARRIER_REGION_PROP_FILE_NAME), false);
            saveCustVariantToFile(custVariant);
            String countryCode = Settings.Global.getString(MiuiInitServer.this.mContext.getContentResolver(), "wifi_country_code");
            if (TextUtils.isEmpty(countryCode)) {
                String countryCode2 = Build.getRegion();
                if (!TextUtils.isEmpty(countryCode2)) {
                    WifiManager wifiManager = (WifiManager) MiuiInitServer.this.mContext.getSystemService("wifi");
                }
            }
            installVanwardCustApps();
            return custVariantDir.exists();
        }

        private void installVanwardCustApps() {
            PreinstallApp.installVanwardCustApps(MiuiInitServer.this.mContext);
        }

        private void importCustProperties(File custProp, boolean isTimezoneAuto) {
            String[] ss;
            if (custProp.exists()) {
                try {
                    BufferedReader bufferReader = new BufferedReader(new FileReader(custProp));
                    while (true) {
                        String line = bufferReader.readLine();
                        if (line == null) {
                            break;
                        }
                        String line2 = line.trim();
                        if (!line2.startsWith("#") && (ss = line2.split("=")) != null && ss.length == 2) {
                            if ("persist.sys.timezone".equals(ss[0]) && isTimezoneAuto && isDeviceNotInProvision()) {
                                Log.i(MiuiInitServer.TAG, "persist.sys.timezone will not be changed when AUTO_TIME_ZONE is open!");
                            } else {
                                SystemProperties.set(ss[0], ss[1]);
                            }
                        }
                    }
                    if (Build.IS_GLOBAL_BUILD) {
                        String zoneid = SystemProperties.get("persist.sys.timezone", "");
                        Log.i(MiuiInitServer.TAG, "init MiuiSettings RESIDENT_TIMEZONE: " + zoneid);
                        Settings.System.putString(MiuiInitServer.this.mContext.getContentResolver(), "resident_timezone", zoneid);
                    }
                    pokeSystemProperties();
                    bufferReader.close();
                } catch (IOException e) {
                }
            }
        }

        private void pokeSystemProperties() {
            try {
                String[] services = ServiceManager.listServices();
                for (String service : services) {
                    IBinder obj = ServiceManager.checkService(service);
                    if (obj != null) {
                        Parcel data = Parcel.obtain();
                        try {
                            obj.transact(1599295570, data, null, 0);
                        } catch (RemoteException e) {
                        } catch (Exception e2) {
                            Log.i(MiuiInitServer.TAG, "Someone wrote a bad service '" + service + "' that doesn't like to be poked: " + e2);
                        }
                        data.recycle();
                    }
                }
            } catch (Exception e3) {
            }
        }

        private void saveCustVariantToFile(String custVariant) {
            File custVariantFile = CustomizeUtil.getMiuiCustVariantFile();
            try {
                if (!custVariantFile.exists()) {
                    custVariantFile.getParentFile().mkdirs();
                    custVariantFile.createNewFile();
                }
                FileWriter fileWriter = new FileWriter(custVariantFile, false);
                BufferedWriter bufferWriter = new BufferedWriter(fileWriter);
                bufferWriter.write(custVariant);
                bufferWriter.close();
                fileWriter.close();
                custVariantFile.setReadable(true, false);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean initCustEnvironment(String custVariant, IMiuiInitObserver obs) throws RemoteException {
        this.mContext.enforceCallingOrSelfPermission("miui.os.permisson.INIT_MIUI_ENVIRONMENT", null);
        Slog.i(TAG, "check status, cust variant[" + custVariant + "]");
        synchronized (this) {
            if (this.mDoing) {
                Slog.w(TAG, "skip, initializing cust environment");
                return false;
            } else if (TextUtils.isEmpty(custVariant)) {
                Slog.w(TAG, "skip, cust variant[" + custVariant + "] is empty");
                return false;
            } else {
                this.mDoing = true;
                Slog.i(TAG, "initializing cust environment");
                new InitCustEnvironmentTask(custVariant, obs).start();
                return true;
            }
        }
    }

    public void installPreinstallApp() {
        this.mContext.enforceCallingOrSelfPermission("miui.os.permisson.INIT_MIUI_ENVIRONMENT", null);
        PreinstallApp.installCustApps(this.mContext);
    }

    public String[] getCustVariants() throws RemoteException {
        ArrayList<String> regionList = new ArrayList<>();
        File cust = CustomizeUtil.getMiuiCustPropDir();
        String[] cs = Locale.getISOCountries();
        File[] resgions = cust.listFiles();
        if (resgions != null) {
            for (File region : resgions) {
                if (region.isDirectory()) {
                    String r = region.getName();
                    for (String c : cs) {
                        if (c.equalsIgnoreCase(r)) {
                            regionList.add(r);
                        }
                    }
                }
            }
        }
        return (String[]) regionList.toArray(new String[0]);
    }

    public void doFactoryReset(boolean keepUserApps) throws RemoteException {
        if (Build.IS_GLOBAL_BUILD) {
            CustomizeUtil.setMiuiCustVariatDir("");
            File file = CustomizeUtil.getMiuiCustVariantFile();
            if (file.exists()) {
                file.delete();
            }
        }
        if (!keepUserApps) {
            File file2 = new File(PREINSTALL_APP_HISTORY_FILE);
            if (file2.exists()) {
                file2.delete();
            }
        }
    }

    public boolean isPreinstalledPackage(String pkg) {
        if (TextUtils.isEmpty(pkg)) {
            return false;
        }
        return PreinstallApp.isPreinstalledPackage(pkg);
    }

    public boolean isPreinstalledPAIPackage(String pkg) {
        if (TextUtils.isEmpty(pkg)) {
            return false;
        }
        return PreinstallApp.isPreinstalledPAIPackage(pkg);
    }

    public String getMiuiChannelPath(String pkg) {
        if (!TextUtils.isEmpty(pkg) && isPreinstalledPackage(pkg)) {
            if (this.mPreinstalledChannels == null) {
                this.mPreinstalledChannels = PreinstallApp.getPeinstalledChannelList();
            }
            Iterator<String> it = this.mPreinstalledChannels.iterator();
            while (it.hasNext()) {
                String channel = it.next();
                if (channel.contains(pkg) && new File(channel).exists()) {
                    return channel;
                }
            }
            return "";
        }
        return "";
    }

    public void removeFromPreinstallList(String pkg) {
        this.mContext.enforceCallingOrSelfPermission("miui.os.permisson.INIT_MIUI_ENVIRONMENT", null);
        if (TextUtils.isEmpty(pkg)) {
            return;
        }
        PreinstallApp.removeFromPreinstallList(pkg);
    }

    public void removeFromPreinstallPAIList(String pkg) {
        this.mContext.enforceCallingOrSelfPermission("miui.os.permisson.INIT_MIUI_ENVIRONMENT", null);
        if (TextUtils.isEmpty(pkg)) {
            return;
        }
        PreinstallApp.removeFromPreinstallPAIList(pkg);
    }

    public void writePreinstallPAIPackage(String pkg) throws RemoteException {
        this.mContext.enforceCallingOrSelfPermission("miui.os.permisson.INIT_MIUI_ENVIRONMENT", null);
        if (TextUtils.isEmpty(pkg)) {
            return;
        }
        PreinstallApp.writePreinstallPAIPackage(pkg);
    }

    public void copyPreinstallPAITrackingFile(String type, String fileName, String content) throws RemoteException {
        this.mContext.enforceCallingOrSelfPermission("miui.os.permisson.INIT_MIUI_ENVIRONMENT", null);
        if (TextUtils.isEmpty(type) || TextUtils.isEmpty(content)) {
            return;
        }
        PreinstallApp.copyPreinstallPAITrackingFile(type, fileName, content);
    }

    public int getPreinstalledAppVersion(String pkg) {
        if (TextUtils.isEmpty(pkg)) {
            return -1;
        }
        return PreinstallApp.getPreinstalledAppVersion(pkg);
    }

    public String getMiuiPreinstallAppPath(String pkg) {
        if (TextUtils.isEmpty(pkg)) {
            return "";
        }
        if (this.mPreinstallHistoryMap == null) {
            this.mPreinstallHistoryMap = new HashMap<>();
            try {
                String[] pkgNameList = FeatureParser.getStringArray("system_data_packagename_list");
                String[] appPathList = FeatureParser.getStringArray("system_data_path_list");
                if (pkgNameList != null && appPathList != null && pkgNameList.length == appPathList.length) {
                    for (int i = 0; i < pkgNameList.length; i++) {
                        this.mPreinstallHistoryMap.put(pkgNameList[i], appPathList[i]);
                    }
                }
            } catch (Exception e) {
                Log.i(TAG, "Error occurs while get miui preinstall app path + " + e);
            }
        }
        return this.mPreinstallHistoryMap.get(pkg) == null ? "" : this.mPreinstallHistoryMap.get(pkg);
    }

    public void setRestrictAspect(String pkg, boolean restrict) {
        if (!this.mNeedAspectSettings) {
            return;
        }
        checkPermission("setRestrictAspect");
        this.mCompatModePackages.setRestrictAspect(pkg, restrict);
    }

    public boolean isRestrictAspect(String packageName) {
        if (!this.mNeedAspectSettings) {
            return Build.VERSION.SDK_INT >= 26;
        }
        return this.mCompatModePackages.isRestrictAspect(packageName);
    }

    public float getAspectRatio(String pkg) {
        if (!this.mNeedAspectSettings) {
            return 3.0f;
        }
        return this.mCompatModePackages.getAspectRatio(pkg);
    }

    public int getDefaultAspectType(String pkg) {
        if (!this.mNeedAspectSettings) {
            return 0;
        }
        return this.mCompatModePackages.getDefaultAspectType(pkg);
    }

    public int getNotchConfig(String pkg) {
        if (!CustomizeUtil.HAS_NOTCH) {
            return 0;
        }
        long identity = Binder.clearCallingIdentity();
        try {
            return this.mCompatModePackages.getNotchConfig(pkg);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void setNotchSpecialMode(String pkg, boolean special) {
        if (!CustomizeUtil.HAS_NOTCH) {
            return;
        }
        checkPermission("setNotchSpecialMode");
        this.mCompatModePackages.setNotchSpecialMode(pkg, special);
    }

    public void setCutoutMode(String pkg, int mode) {
        if (!CustomizeUtil.HAS_NOTCH) {
            return;
        }
        checkPermission("setCutoutMode");
        this.mCompatModePackages.setCutoutMode(pkg, mode);
    }

    public int getCutoutMode(String pkg) {
        if (!CustomizeUtil.HAS_NOTCH) {
            return 0;
        }
        long identity = Binder.clearCallingIdentity();
        try {
            return this.mCompatModePackages.getCutoutMode(pkg);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    private void checkPermission(String reason) {
        int permission = this.mContext.checkCallingPermission("android.permission.SET_SCREEN_COMPATIBILITY");
        if (permission == 0) {
            return;
        }
        throw new SecurityException("Permission Denial: " + reason + " pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
    }

    public void deletePackagesByRegion() {
        if ("IT".equals(SystemProperties.get("ro.miui.region", "")) && "eea".equals(SystemProperties.get("ro.miui.build.region", ""))) {
            try {
                PackageManager pm = this.mContext.getPackageManager();
                pm.deletePackage("com.miui.fm", null, 0);
                pm.deletePackage("com.miui.fmservice", null, 0);
            } catch (Exception e) {
            }
        }
    }
}
