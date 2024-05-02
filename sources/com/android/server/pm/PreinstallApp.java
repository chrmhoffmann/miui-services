package com.android.server.pm;

import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.content.Context;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManagerInternal;
import android.content.pm.PermissionInfo;
import android.content.pm.parsing.ApkLite;
import android.content.pm.parsing.ApkLiteParseUtils;
import android.content.pm.parsing.PackageLite;
import android.content.pm.parsing.result.ParseResult;
import android.content.pm.parsing.result.ParseTypeImpl;
import android.os.Build;
import android.os.FileUtils;
import android.os.RemoteException;
import android.os.SELinux;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Slog;
import com.android.internal.util.JournaledFile;
import com.android.server.LocalServices;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.permission.PermissionManagerService;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import libcore.io.IoUtils;
import miui.os.CustVerifier;
import miui.util.CustomizeUtil;
import miui.util.FeatureParser;
import miui.util.PreinstallAppUtils;
/* loaded from: classes.dex */
public class PreinstallApp {
    private static final String ALLOW_INSTALL_THIRD_APP_LIST = "allow_install_third_app.list";
    private static final boolean DEBUG = true;
    private static final String DISALLOW_INSTALL_THIRD_APP_LIST = "disallow_install_third_app.list";
    private static final String ENCRYPTING_STATE = "trigger_restart_min_framework";
    private static final String GLOBAL_BROWSER_APK = "MIBrowserGlobal.apk";
    private static final String GLOBAL_BROWSER_REMOVEABLE_APK = "MIBrowserGlobal_removable.apk";
    private static final String INSTALL_DIR = "/data/app/";
    private static final int MIUI_APP_PATH_L_LENGTH = 7;
    private static final String OLD_PREINSTALL_HISTORY_FILE = "/data/system/preinstall_history";
    private static final String OLD_PREINSTALL_PACKAGE_PAI_TRACKING_FILE = "/cust/etc/pre_install.appsflyer";
    private static final String PREINSTALL_HISTORY_FILE = "/data/app/preinstall_history";
    private static final String PREINSTALL_PACKAGE_LIST = "/data/system/preinstall.list";
    private static final String PREINSTALL_PACKAGE_MIUI_TRACKING_DIR = "/data/miui/pai/";
    private static final String PREINSTALL_PACKAGE_MIUI_TRACKING_DIR_CONTEXT = "u:object_r:miui_pai_file:s0";
    private static final String PREINSTALL_PACKAGE_PAI_LIST = "/data/system/preinstallPAI.list";
    private static final String PREINSTALL_PACKAGE_PAI_TRACKING_FILE = "/data/miui/pai/pre_install.appsflyer";
    private static final String PREINSTALL_PACKAGE_PATH = "/data/app/preinstall_package_path";
    private static final String PRODUCT_CARRIER_DIR = "/product/opcust";
    private static final String RANDOM_DIR_PREFIX = "~~";
    private static final String TYPE_TRACKING_APPSFLYER = "appsflyer";
    private static final String TYPE_TRACKING_MIUI = "xiaomi";
    public static Map<String, String> sAdvanceApps;
    private static final String TAG = PreinstallApp.class.getSimpleName();
    private static final File OLD_PREINSTALL_APP_DIR = new File("/data/miui/apps");
    private static final File NONCUSTOMIZED_APP_DIR = CustomizeUtil.getMiuiNoCustomizedAppDir();
    private static final File VENDOR_NONCUSTOMIZED_APP_DIR = CustomizeUtil.getMiuiVendorNoCustomizedAppDir();
    private static final File PRODUCT_NONCUSTOMIZED_APP_DIR = CustomizeUtil.getMiuiProductNoCustomizedAppDir();
    private static final File CUSTOMIZED_APP_DIR = CustomizeUtil.getMiuiCustomizedAppDir();
    private static final File RECOMMENDED_APP_DIR = new File("/data/miui/app/recommended");
    private static final File OTA_SKIP_BUSINESS_APP_LIST_FILE = new File("/system/etc/ota_skip_apps");
    private static final File S_SYSTEM_PREINSTALL_APP_DIR = new File("/system/data-app");
    private static final File S_DATA_PREINSTALL_APP_DIR = new File("/data/miui/app/recommended");
    private static final File T_SYSTEM_PREINSTALL_APP_DIR = new File("/product/data-app");
    private static final Set<String> sIgnorePreinstallApks = new HashSet();
    private static final Map<String, Item> sPreinstallApps = new HashMap();
    private static final Set<String> NOT_OTA_PACKAGE_NAMES = new HashSet();
    private static Map<String, Integer> mPackageVersionMap = new HashMap();
    private static List<String> mPackagePAIList = new ArrayList();
    private static List<String> mTraditionalTrackContentList = new ArrayList();
    private static List<String> mNewTrackContentList = new ArrayList();
    public static ArrayList<String> sCloudControlUninstall = new ArrayList<>();
    private static final Set<String> sAllowOnlyInstallThirdApps = new HashSet();
    private static final Set<String> sDisallowInstallThirdApps = new HashSet();
    private static final Set<Item> sNewOrUpdatePreinstallApps = new HashSet();

    /* loaded from: classes.dex */
    public static class Item {
        static final int TYPE_CLUSTER = 2;
        static final int TYPE_MONOLITHIC = 1;
        File apkFile;
        File app;
        String packageName;
        ApkLite pkg;
        PackageLite pkgLite;
        int type;

        Item(String packageName, File file, PackageLite pkgLite, ApkLite pkg) {
            this.packageName = packageName;
            this.app = file;
            this.apkFile = PreinstallApp.getApkFile(file);
            this.pkgLite = pkgLite;
            this.pkg = pkg;
            this.type = file.isDirectory() ? 2 : 1;
        }

        static boolean betterThan(Item newItem, Item oldItem) {
            if (Build.VERSION.SDK_INT < 21) {
                if (newItem.pkgLite.getVersionCode() > oldItem.pkgLite.getVersionCode()) {
                    return true;
                }
            } else {
                int i = newItem.type;
                int i2 = oldItem.type;
                if (i > i2) {
                    return true;
                }
                return i == i2 && newItem.pkgLite.getVersionCode() > oldItem.pkgLite.getVersionCode();
            }
            return false;
        }

        public String toString() {
            return "" + this.packageName + "[" + this.apkFile.getPath() + "," + this.pkgLite.getVersionCode() + "]";
        }
    }

    static {
        if (!miui.os.Build.IS_MI2A && !miui.os.Build.IS_MITHREE) {
            ignorePreinstallApks("ota-miui-MiTagApp.apk");
        }
        if (!FeatureParser.getBoolean("support_ir", false)) {
            ignorePreinstallApks("partner-XMRemoteController.apk");
        }
        if ("POCO".equals(Build.BRAND)) {
            ignorePreinstallApks("MICOMMUNITY_OVERSEA.apk");
            ignorePreinstallApks("MISTORE_OVERSEA.apk");
        } else {
            ignorePreinstallApks("POCOCOMMUNITY_OVERSEA.apk");
            ignorePreinstallApks("POCOSTORE_OVERSEA.apk");
        }
        if (!"sweet".equals(Build.DEVICE) && "global".equalsIgnoreCase(SystemProperties.get("ro.miui.build.region"))) {
            ignorePreinstallApks(GLOBAL_BROWSER_APK);
            ignorePreinstallApks(GLOBAL_BROWSER_REMOVEABLE_APK);
        }
        sAdvanceApps = new HashMap();
    }

    static void ignorePreinstallApks(String fileName) {
        File[] preinstallDirs = {NONCUSTOMIZED_APP_DIR, VENDOR_NONCUSTOMIZED_APP_DIR, PRODUCT_NONCUSTOMIZED_APP_DIR, CUSTOMIZED_APP_DIR, RECOMMENDED_APP_DIR};
        for (File dir : preinstallDirs) {
            File apkFile = new File(dir.getPath() + "/" + fileName);
            if (apkFile.exists()) {
                sIgnorePreinstallApks.add(apkFile.getPath());
            }
            File apkFile2 = new File(dir.getPath() + "/" + fileName.replace(".apk", "") + "/" + fileName);
            if (apkFile2.exists()) {
                sIgnorePreinstallApks.add(apkFile2.getPath());
            }
        }
    }

    private static IPackageManager getPackageManager() {
        return IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
    }

    /* JADX WARN: Multi-variable type inference failed */
    private static void readHistory(String filePath, Map<String, Long> preinstallHistoryMap) {
        Throwable th;
        File installHistoryFile;
        String[] paths;
        BufferedReader bufferReader = null;
        FileReader fileReader = null;
        try {
            try {
                installHistoryFile = new File(filePath);
            } catch (IOException e) {
            } catch (Throwable th2) {
                th = th2;
                IoUtils.closeQuietly(bufferReader);
                IoUtils.closeQuietly(fileReader);
                throw th;
            }
        } catch (IOException e2) {
        } catch (Throwable th3) {
            th = th3;
        }
        if (!installHistoryFile.exists()) {
            IoUtils.closeQuietly((AutoCloseable) null);
            IoUtils.closeQuietly((AutoCloseable) null);
            return;
        }
        fileReader = new FileReader(installHistoryFile);
        bufferReader = new BufferedReader(fileReader);
        while (true) {
            String line = bufferReader.readLine();
            if (line == null) {
                break;
            }
            String[] ss = line.split(":");
            if (ss != null && ss.length == 2) {
                try {
                    long mtime = Long.valueOf(ss[1]).longValue();
                    String str = ss[0];
                    File file = S_SYSTEM_PREINSTALL_APP_DIR;
                    if (str.startsWith(file.getPath())) {
                        preinstallHistoryMap.put(ss[0].replace(file.getPath(), T_SYSTEM_PREINSTALL_APP_DIR.getPath()), Long.valueOf(mtime));
                    } else {
                        String str2 = ss[0];
                        File file2 = S_DATA_PREINSTALL_APP_DIR;
                        if (str2.startsWith(file2.getPath())) {
                            preinstallHistoryMap.put(ss[0].replace(file2.getPath(), T_SYSTEM_PREINSTALL_APP_DIR.getPath()), Long.valueOf(mtime));
                        }
                    }
                    String str3 = ss[0];
                    File file3 = OLD_PREINSTALL_APP_DIR;
                    if (str3.startsWith(file3.getPath())) {
                        preinstallHistoryMap.put(ss[0].replace(file3.getPath(), CUSTOMIZED_APP_DIR.getPath()), Long.valueOf(mtime));
                        preinstallHistoryMap.put(ss[0].replace(file3.getPath(), NONCUSTOMIZED_APP_DIR.getPath()), Long.valueOf(mtime));
                        preinstallHistoryMap.put(ss[0].replace(file3.getPath(), VENDOR_NONCUSTOMIZED_APP_DIR.getPath()), Long.valueOf(mtime));
                        preinstallHistoryMap.put(ss[0].replace(file3.getPath(), PRODUCT_NONCUSTOMIZED_APP_DIR.getPath()), Long.valueOf(mtime));
                    } else {
                        if (Build.VERSION.SDK_INT >= 21 && (paths = ss[0].split("/")) != null && paths.length != 7) {
                            String fileName = paths[paths.length - 1];
                            String apkName = fileName.replace(".apk", "");
                            String possibleNewPath = ss[0].replace(fileName, apkName + "/" + fileName);
                            File oldAppFile = new File(ss[0]);
                            File newAppFile = new File(possibleNewPath);
                            if (newAppFile.exists() && isSamePackage(oldAppFile, newAppFile)) {
                                ss[0] = possibleNewPath;
                            }
                        }
                        preinstallHistoryMap.put(ss[0], Long.valueOf(mtime));
                    }
                } catch (NumberFormatException e3) {
                }
            }
        }
        IoUtils.closeQuietly(bufferReader);
        IoUtils.closeQuietly(fileReader);
    }

    private static void readHistory(Map<String, Long> preinstallHistoryMap) {
        readHistory(OLD_PREINSTALL_HISTORY_FILE, preinstallHistoryMap);
        readHistory(PREINSTALL_HISTORY_FILE, preinstallHistoryMap);
    }

    private static void writeHistory(String filePath, Map<String, Long> preinstallHistoryMap) {
        FileWriter fileWriter = null;
        BufferedWriter bufferWriter = null;
        try {
            File installHistoryFile = new File(filePath);
            if (!installHistoryFile.exists()) {
                installHistoryFile.createNewFile();
            }
            fileWriter = new FileWriter(installHistoryFile, false);
            bufferWriter = new BufferedWriter(fileWriter);
            for (Map.Entry<String, Long> r : preinstallHistoryMap.entrySet()) {
                if (new File(r.getKey()).exists()) {
                    bufferWriter.write(r.getKey() + ":" + r.getValue());
                    bufferWriter.write("\n");
                }
            }
        } catch (IOException e) {
        } catch (Throwable th) {
            IoUtils.closeQuietly(bufferWriter);
            IoUtils.closeQuietly(fileWriter);
            throw th;
        }
        IoUtils.closeQuietly(bufferWriter);
        IoUtils.closeQuietly(fileWriter);
    }

    private static void writeHistory(Map<String, Long> preinstallHistoryMap) {
        File old = new File(OLD_PREINSTALL_HISTORY_FILE);
        if (old.exists()) {
            old.delete();
        }
        writeHistory(PREINSTALL_HISTORY_FILE, preinstallHistoryMap);
    }

    private static void recordToHistory(Map<String, Long> history, File apkFile) {
        history.put(apkFile.getPath(), Long.valueOf(apkFile.lastModified()));
    }

    private static void recordHistory(Map<String, Long> history, Item item) {
        recordToHistory(history, item.apkFile);
    }

    private static boolean existHistory() {
        return new File(PREINSTALL_HISTORY_FILE).exists() || new File(OLD_PREINSTALL_HISTORY_FILE).exists();
    }

    private static boolean deleteContentsRecursive(File dir) {
        File[] files = dir.listFiles();
        boolean success = true;
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    success &= deleteContentsRecursive(file);
                }
                if (!file.delete()) {
                    Slog.w(TAG, "Failed to delete " + file);
                    success = false;
                }
            }
        }
        return success;
    }

    private static boolean underData(PackageSetting ps) {
        return ps.getPathString().startsWith(INSTALL_DIR);
    }

    public static File getApkFile(File app) {
        if (app.isDirectory()) {
            return getBaseApkFile(app);
        }
        return app;
    }

    private static boolean copyPreinstallApp(Item srcApp, PackageSetting ps) {
        boolean z = false;
        if (!srcApp.apkFile.exists()) {
            return false;
        }
        File dstCodePath = null;
        if (ps != null && underData(ps)) {
            dstCodePath = ps.getPath();
            cleanUpResource(dstCodePath);
        }
        if (dstCodePath == null) {
            dstCodePath = new File(INSTALL_DIR, srcApp.apkFile.getName().replace(".apk", ""));
            if (dstCodePath.exists()) {
                deleteContentsRecursive(dstCodePath);
            }
        }
        if (!dstCodePath.exists()) {
            dstCodePath.mkdirs();
        }
        FileUtils.setPermissions(dstCodePath, 509, -1, -1);
        File codePathParent = dstCodePath.getParentFile();
        if (codePathParent.getName().startsWith(RANDOM_DIR_PREFIX)) {
            FileUtils.setPermissions(codePathParent.getAbsolutePath(), 509, -1, -1);
        }
        File dstApkFile = new File(dstCodePath, "base.apk");
        try {
            FileUtils.copy(srcApp.apkFile, dstApkFile);
            if (FileUtils.setPermissions(dstApkFile, 420, -1, -1) == 0) {
                z = true;
            }
            boolean success = z;
            return success;
        } catch (IOException e) {
            Slog.d(TAG, "Copy failed from: " + srcApp.apkFile.getPath() + " to " + dstApkFile.getPath() + ":" + e.toString());
            return false;
        }
    }

    static void cleanUpResource(File dstCodePath) {
        if (dstCodePath != null && dstCodePath.isDirectory()) {
            dstCodePath.listFiles(new FileFilter() { // from class: com.android.server.pm.PreinstallApp$$ExternalSyntheticLambda0
                @Override // java.io.FileFilter
                public final boolean accept(File file) {
                    return PreinstallApp.lambda$cleanUpResource$0(file);
                }
            });
        }
    }

    public static /* synthetic */ boolean lambda$cleanUpResource$0(File f) {
        if (f.getName().endsWith(".apk")) {
            Slog.d(TAG, "list and delete " + f.getName());
            f.delete();
            return false;
        } else if (f.isDirectory()) {
            deleteContentsRecursive(f);
            f.delete();
            return false;
        } else {
            Slog.d(TAG, "list unknown file: " + f.getName());
            return false;
        }
    }

    private static final boolean isApkFile(File apkFile) {
        return apkFile != null && apkFile.getPath().endsWith(".apk");
    }

    private static void readLineToSet(File file, Set<String> set) {
        if (file.exists()) {
            BufferedReader buffer = null;
            try {
                try {
                    try {
                        buffer = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
                        if (!file.getName().equals("vanward_applist")) {
                            while (true) {
                                String line = buffer.readLine();
                                if (line == null) {
                                    break;
                                }
                                set.add(line.trim());
                            }
                        } else {
                            IActivityManager am = ActivityManagerNative.getDefault();
                            Locale curLocale = am.getConfiguration().locale;
                            while (true) {
                                String line2 = buffer.readLine();
                                if (line2 == null) {
                                    break;
                                }
                                String[] ss = line2.trim().split("\\s+");
                                if (ss.length == 2 && isValidIme(ss[1], curLocale)) {
                                    set.add(ss[0]);
                                }
                            }
                        }
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                } catch (FileNotFoundException e2) {
                    e2.printStackTrace();
                } catch (IOException e3) {
                    e3.printStackTrace();
                }
            } finally {
                IoUtils.closeQuietly((AutoCloseable) null);
            }
        }
    }

    private static File getBaseApkFile(File dir) {
        return new File(dir, dir.getName() + ".apk");
    }

    private static void addPreinstallAppToList(List<File> preinstallAppList, File appDir, Set<String> filterSet) {
        File apk;
        File[] apps = appDir.listFiles();
        if (apps != null) {
            for (File app : apps) {
                if (app.isDirectory()) {
                    apk = getBaseApkFile(app);
                    if (!apk.exists()) {
                    }
                    if (!sIgnorePreinstallApks.contains(apk.getPath()) && (filterSet == null || filterSet.contains(apk.getName()))) {
                        preinstallAppList.add(app);
                    }
                } else {
                    apk = app;
                    if (!isApkFile(apk)) {
                    }
                    if (!sIgnorePreinstallApks.contains(apk.getPath())) {
                        preinstallAppList.add(app);
                    }
                }
            }
        }
    }

    private static ArrayList<File> getPreinstallApplist(boolean onlyCust) {
        ArrayList<File> preinstallAppList = new ArrayList<>();
        Set<String> customizedAppSet = new HashSet<>();
        Set<String> recommendedAppSet = new HashSet<>();
        Set<String> productCarrierAppSet = new HashSet<>();
        File custVariantDir = CustomizeUtil.getMiuiCustVariantDir();
        if (custVariantDir != null) {
            readLineToSet(new File(custVariantDir, "customized_applist"), customizedAppSet);
            readLineToSet(new File(custVariantDir, "ota_customized_applist"), customizedAppSet);
            readLineToSet(new File(custVariantDir, "recommended_applist"), recommendedAppSet);
            readLineToSet(new File(custVariantDir, "ota_recommended_applist"), recommendedAppSet);
        }
        File carrierRegionAppFile = CustomizeUtil.getProductCarrierRegionAppFile();
        if (carrierRegionAppFile.exists()) {
            readLineToSet(carrierRegionAppFile, productCarrierAppSet);
        }
        addPreinstallAppToList(preinstallAppList, CUSTOMIZED_APP_DIR, customizedAppSet);
        addPreinstallAppToList(preinstallAppList, RECOMMENDED_APP_DIR, recommendedAppSet);
        if (!onlyCust) {
            addPreinstallAppToList(preinstallAppList, NONCUSTOMIZED_APP_DIR, null);
            addPreinstallAppToList(preinstallAppList, VENDOR_NONCUSTOMIZED_APP_DIR, null);
            addPreinstallAppToList(preinstallAppList, PRODUCT_NONCUSTOMIZED_APP_DIR, null);
        }
        addPreinstallAppToList(preinstallAppList, CustomizeUtil.getProductCarrierAppDir(), productCarrierAppSet);
        browserRSACustomize(preinstallAppList);
        return preinstallAppList;
    }

    private static void browserRSACustomize(List<File> preinstallAppList) {
        String rsa = SystemProperties.get("ro.com.miui.rsa", "");
        if (!"sweet".equals(Build.DEVICE) && "tier2".equals(rsa) && "global".equalsIgnoreCase(SystemProperties.get("ro.miui.build.region"))) {
            Map<String, Long> history = new HashMap<>();
            readHistory(history);
            String str = TAG;
            Slog.i(str, "globalBrowser RSA Customize");
            File apkFile = new File(NONCUSTOMIZED_APP_DIR.getPath() + "/" + GLOBAL_BROWSER_APK.replace(".apk", "") + "/" + GLOBAL_BROWSER_APK);
            StringBuilder sb = new StringBuilder();
            File file = PRODUCT_NONCUSTOMIZED_APP_DIR;
            File apkFileRemovable = new File(sb.append(file.getPath()).append("/").append(GLOBAL_BROWSER_REMOVEABLE_APK.replace(".apk", "")).append("/").append(GLOBAL_BROWSER_REMOVEABLE_APK).toString());
            File apkFileProduct = new File(file.getPath() + "/" + GLOBAL_BROWSER_APK.replace(".apk", "") + "/" + GLOBAL_BROWSER_APK);
            if (apkFile.exists() && !recorded(history, apkFile)) {
                Slog.i(str, "globalBrowser PreInstall install apkFile: " + apkFile + " hasInstallHistory : " + recorded(history, apkFile));
                preinstallAppList.add(apkFile);
            } else if (apkFileRemovable.exists() && !recorded(history, apkFileRemovable)) {
                Slog.i(str, "globalBrowser PreInstall install apkFile apkFileRemovable: " + apkFileRemovable + " hasInstallHistory apkFileRemovable : " + recorded(history, apkFileRemovable));
                preinstallAppList.add(apkFileRemovable);
            } else if (apkFileProduct.exists() && !recorded(history, apkFileProduct)) {
                Slog.i(str, "globalBrowser PreInstall install apkFile apkFileProduct: " + apkFileProduct + " hasInstallHistory apkFileProduct: " + recorded(history, apkFileProduct));
                preinstallAppList.add(apkFileProduct);
            }
        }
    }

    public static List<File> getCustomizePreinstallAppList() {
        return getPreinstallApplist(true);
    }

    private static List<File> getAllPreinstallApplist() {
        return getPreinstallApplist(false);
    }

    private static boolean isSystemApp(PackageSetting ps) {
        return (ps.getFlags() & 1) != 0;
    }

    private static boolean isUpdatedSystemApp(PackageSetting ps) {
        return ps.getPathString() != null && ps.getPathString().startsWith(INSTALL_DIR);
    }

    private static boolean isSystemAndNotUpdatedSystemApp(PackageSetting ps) {
        return ps != null && isSystemApp(ps) && !isUpdatedSystemApp(ps);
    }

    private static boolean systemAppDeletedOrDisabled(PackageManagerService pms, String pkgName) {
        return !pms.mPackages.containsKey(pkgName);
    }

    private static boolean dealed(Map<String, Long> history, File apkFile) {
        return history.containsKey(apkFile.getPath()) && apkFile.lastModified() == history.get(apkFile.getPath()).longValue() && !apkFile.getPath().contains("/system/data-app") && !apkFile.getPath().contains("/vendor/data-app") && !apkFile.getPath().contains("/product/data-app");
    }

    private static boolean dealed(Map<String, Long> history, Item item) {
        return dealed(history, item.apkFile);
    }

    private static boolean recorded(Map<String, Long> history, File apkFile) {
        return history.containsKey(apkFile.getPath());
    }

    private static boolean recorded(Map<String, Long> history, Item item) {
        return recorded(history, item.apkFile);
    }

    private static boolean signCheck(File apkFile) {
        if (Build.IS_DEBUGGABLE) {
            return false;
        }
        boolean custAppSupportSign = PreinstallAppUtils.supportSignVerifyInCust() && apkFile.getPath().contains("/cust/app");
        String str = TAG;
        Slog.i(str, "Sign support is " + custAppSupportSign);
        if (custAppSupportSign && CustVerifier.getInstance() == null) {
            Slog.i(str, "CustVerifier init error !" + apkFile.getPath() + " will not be installed.");
            return true;
        } else if (!custAppSupportSign || CustVerifier.getInstance().verifyApkSignatue(apkFile.getPath(), 0)) {
            return false;
        } else {
            Slog.i(str, apkFile.getPath() + " verify failed!");
            return true;
        }
    }

    private static void parseAndDeleteDuplicatePreinstallApps() {
        List<File> preinstallAppFiles = getAllPreinstallApplist();
        long currentTime = System.currentTimeMillis();
        for (File pa : preinstallAppFiles) {
            PackageLite pl = parsePackageLite(pa);
            if (pl == null || pl.getPackageName() == null) {
                Slog.e(TAG, "Parse " + pa.getPath() + " failed, skip");
            } else {
                String packageName = pl.getPackageName();
                Item newItem = new Item(packageName, pa, pl, null);
                Map<String, Item> map = sPreinstallApps;
                if (!map.containsKey(packageName)) {
                    map.put(packageName, newItem);
                } else {
                    Item oldItem = map.get(packageName);
                    if (Item.betterThan(newItem, oldItem)) {
                        map.put(packageName, newItem);
                    } else {
                        newItem = oldItem;
                        oldItem = newItem;
                    }
                    Slog.w(TAG, newItem.toString() + " is better than " + oldItem.toString() + ", ignore " + oldItem.toString() + " !!!");
                }
            }
        }
        Slog.i(TAG, "Parse preinstall apps, consume " + (System.currentTimeMillis() - currentTime) + "ms");
    }

    private static void copyPreinstallAppsForFirstBoot(PackageManagerService pms, Settings settings) {
        long currentTime = System.currentTimeMillis();
        Map<String, Long> history = new HashMap<>();
        if (existHistory()) {
            Slog.w(TAG, "Exist preinstall history, skip copying preinstall apps for first boot!");
            return;
        }
        Slog.i(TAG, "Copy preinstall apps start for first boot");
        Map<String, Integer> pkgMap = new HashMap<>();
        Map<String, String> pkgPathMap = new HashMap<>();
        for (Item item : sPreinstallApps.values()) {
            if (signCheck(item.apkFile)) {
                Slog.i(TAG, "Skip copying when the sign is false for first boot.");
            } else {
                PackageLite pl = item.pkgLite;
                PackageSetting ps = null;
                if (pl != null) {
                    ps = settings.getPackageLPr(pl.getPackageName());
                    if (isSystemAndNotUpdatedSystemApp(ps) && pl.getVersionCode() >= ps.getVersionCode()) {
                        Slog.w(TAG, "Copying new system updated preinstall app " + item.toString() + ", disable system package firstly.");
                        settings.disableSystemPackageLPw(pl.getPackageName(), true);
                        removePackageLI(pms, pl.getPackageName(), true);
                    }
                }
                if (copyPreinstallApp(item, ps)) {
                    Slog.i(TAG, "Copy " + item.toString() + " for first boot");
                    if (pl != null && !TextUtils.isEmpty(pl.getPackageName())) {
                        pkgMap.put(pl.getPackageName(), Integer.valueOf(pl.getVersionCode()));
                        pkgPathMap.put(pl.getPackageName(), item.apkFile.getPath());
                    }
                    recordHistory(history, item);
                    recordAdvanceAppsHistory(item);
                } else {
                    Slog.e(TAG, "Copy " + item.toString() + " failed for first boot");
                }
            }
        }
        writeHistory(history);
        writePreinstallPackage(pkgMap);
        writePreInstallPackagePath(pkgPathMap);
        Slog.i(TAG, "Copy preinstall apps end for first boot, consume " + (System.currentTimeMillis() - currentTime) + "ms");
    }

    private static void removePackageLI(PackageManagerService pms, String packageName, boolean chatty) {
        RemovePackageHelper removePackageHelper = new RemovePackageHelper(pms);
        removePackageHelper.removePackageLI(packageName, chatty);
    }

    private static boolean skipYouPinIfHadPreinstall(Map<String, Long> history, Item item) {
        boolean isNewYoupinPreinstall = "/system/data-app/Youpin/Youpin.apk".equals(item.apkFile.getPath()) || "/vendor/data-app/Youpin/Youpin.apk".equals(item.apkFile.getPath());
        boolean hadPreInstallYouPin = history.containsKey("/system/data-app/com.xiaomi.youpin/com.xiaomi.youpin.apk") || history.containsKey("/cust/app/customized/recommended-3rd-com.xiaomi.youpin/recommended-3rd-com.xiaomi.youpin.apk");
        return isNewYoupinPreinstall && hadPreInstallYouPin;
    }

    private static boolean skipInstallByCota(Item item) {
        String apkFilePath;
        if (item == null || item.apkFile == null || (apkFilePath = item.apkFile.getPath()) == null || apkFilePath.contains("/system/data-app") || apkFilePath.contains("/vendor/data-app") || apkFilePath.contains("/product/data-app")) {
            return false;
        }
        Set<String> set = sAllowOnlyInstallThirdApps;
        if (set.size() <= 0 || set.contains(item.packageName)) {
            Set<String> set2 = sDisallowInstallThirdApps;
            if (set2.size() <= 0 || !set2.contains(item.packageName)) {
                return false;
            }
        }
        return true;
    }

    private static void copyPreinstallAppsForBoot(PackageManagerService pms, Settings settings) {
        Iterator<Item> it;
        boolean z;
        String str;
        String str2;
        String str3;
        long currentTime = System.currentTimeMillis();
        Map<String, Long> history = new HashMap<>();
        DeletePackageHelper deletePackageHelper = new DeletePackageHelper(pms);
        readLineToSet(OTA_SKIP_BUSINESS_APP_LIST_FILE, NOT_OTA_PACKAGE_NAMES);
        readHistory(history);
        readAllowOnlyInstallThirdAppForCarrier();
        Slog.i(TAG, "copy preinstall apps start");
        Iterator<Item> it2 = sPreinstallApps.values().iterator();
        while (it2.hasNext()) {
            Item item = it2.next();
            if (skipYouPinIfHadPreinstall(history, item)) {
                Slog.i(TAG, "skip YouPin because pre-installed");
            } else if (skipOTA(item)) {
                Slog.i(TAG, "ota skip copy business preinstall data-app, :" + item.packageName);
            } else if (skipInstallByCota(item)) {
                Slog.i(TAG, "cota skip install cust preinstall data-app, :" + item.toString());
            } else if (!dealed(history, item)) {
                if (signCheck(item.apkFile)) {
                    Slog.i(TAG, "Skip copying when the sign is false.");
                } else {
                    boolean recorded = recorded(history, item);
                    PackageSetting ps = settings.getPackageLPr(item.packageName);
                    String str4 = TAG;
                    Slog.i(str4, "Copy " + item.toString());
                    if (ps == null) {
                        if (!recorded) {
                            String cotaCarrier = SystemProperties.get("persist.sys.cota.carrier", "");
                            if (!TextUtils.isEmpty(cotaCarrier) && !"XM".equals(cotaCarrier) && "com.xiaomi.glgm".equals(item.packageName)) {
                                Slog.i(str4, "skip funmax because cota-reject: " + cotaCarrier);
                                recordHistory(history, item);
                            } else {
                                if (!copyPreinstallApp(item, null)) {
                                    Slog.e(str4, "Copy " + item.toString() + " failed");
                                } else {
                                    sNewOrUpdatePreinstallApps.add(item);
                                    recordHistory(history, item);
                                }
                                it = it2;
                            }
                        } else {
                            Slog.w(str4, "User had uninstalled " + item.packageName + ", skip coping" + item.toString());
                            recordHistory(history, item);
                            it = it2;
                        }
                        it2 = it;
                    } else if (item.pkgLite.getVersionCode() <= ps.getVersionCode()) {
                        Slog.w(str4, item.toString() + " is not newer than " + ps.getPathString() + "[" + ps.getVersionCode() + "], skip coping");
                        recordHistory(history, item);
                    } else {
                        ApkLite pkg = parsePackage(item.apkFile);
                        if (pkg == null || pkg.getPackageName() == null) {
                            Slog.e(str4, "Parse " + item.toString() + " failed, skip");
                            it2 = it2;
                        } else {
                            boolean sameSignatures = PackageManagerServiceUtils.compareSignatures(pkg.getSigningDetails().getSignatures(), ps.getSignatures().mSigningDetails.getSignatures()) == 0;
                            if (!sameSignatures && isSystemApp(ps)) {
                                Slog.e(str4, item.toString() + " mismatch signature with system app " + ps.getPathString() + ", skip coping");
                                recordHistory(history, item);
                            } else {
                                if (sameSignatures) {
                                    z = true;
                                    str = " failed";
                                    str2 = "Copy ";
                                    it = it2;
                                    str3 = str4;
                                } else {
                                    Slog.e(str4, item.toString() + " mismatch signature with " + ps.getPathString() + ", delete it's resources and data before coping");
                                    str = " failed";
                                    str2 = "Copy ";
                                    it = it2;
                                    str3 = str4;
                                    z = true;
                                    if (deletePackageHelper.deletePackageX(pkg.getPackageName(), ps.getVersionCode(), 0, 2, true) != 1) {
                                        Slog.e(str3, "Delete mismatch signature app " + item.packageName + " failed, skip coping " + item.toString());
                                        it2 = it;
                                    }
                                }
                                if (!systemAppDeletedOrDisabled(pms, item.packageName) && isSystemAndNotUpdatedSystemApp(ps)) {
                                    Slog.w(str3, "Copying new system updated preinstall app " + item.toString() + ", disable system package first.");
                                    settings.disableSystemPackageLPw(item.packageName, z);
                                    removePackageLI(pms, item.packageName, z);
                                }
                                if (copyPreinstallApp(item, ps)) {
                                    sNewOrUpdatePreinstallApps.add(item);
                                    recordHistory(history, item);
                                } else {
                                    Slog.e(str3, str2 + item.toString() + str);
                                }
                                it2 = it;
                            }
                        }
                    }
                }
            }
        }
        sAllowOnlyInstallThirdApps.clear();
        sDisallowInstallThirdApps.clear();
        writeHistory(history);
        Slog.i(TAG, "copy preinstall apps end, consume " + (System.currentTimeMillis() - currentTime) + "ms");
    }

    public static void copyPreinstallApps(PackageManagerService pms, Settings settings) {
        String cryptState = SystemProperties.get("vold.decrypt");
        if (ENCRYPTING_STATE.equals(cryptState)) {
            Slog.w(TAG, "Detected encryption in progress - can't copy preinstall apps now!");
            return;
        }
        readPackageVersionMap();
        readPackagePAIList();
        copyTraditionalTrackFileToNewLocationIfNeed();
        if (pms.isFirstBoot() || pms.isDeviceUpgrading()) {
            parseAndDeleteDuplicatePreinstallApps();
        }
        if (pms.isFirstBoot()) {
            copyPreinstallAppsForFirstBoot(pms, settings);
        } else if (pms.isDeviceUpgrading()) {
            copyPreinstallAppsForBoot(pms, settings);
        } else {
            Slog.i(TAG, "Nothing need copy for normal boot.");
        }
        CloudControlPreinstallService.startCloudControlService();
    }

    private static void readAllowOnlyInstallThirdAppForCarrier() {
        String cotaCarrier = SystemProperties.get("persist.sys.cota.carrier", "");
        if (!TextUtils.isEmpty(cotaCarrier) && !"XM".equals(cotaCarrier)) {
            try {
                String region = SystemProperties.get("ro.miui.region", "cn").toLowerCase();
                File customizedAppSetFile = new File(new File("/product/opcust/" + cotaCarrier, region), ALLOW_INSTALL_THIRD_APP_LIST);
                if (!customizedAppSetFile.exists()) {
                    customizedAppSetFile = new File(new File(PRODUCT_CARRIER_DIR, cotaCarrier), ALLOW_INSTALL_THIRD_APP_LIST);
                }
                if (customizedAppSetFile.exists()) {
                    readLineToSet(customizedAppSetFile, sAllowOnlyInstallThirdApps);
                    return;
                }
                File customizedAppSetFile2 = new File(new File("/product/opcust/" + cotaCarrier, region), DISALLOW_INSTALL_THIRD_APP_LIST);
                if (!customizedAppSetFile2.exists()) {
                    customizedAppSetFile2 = new File(new File(PRODUCT_CARRIER_DIR, cotaCarrier), DISALLOW_INSTALL_THIRD_APP_LIST);
                }
                if (customizedAppSetFile2.exists()) {
                    readLineToSet(customizedAppSetFile2, sDisallowInstallThirdApps);
                }
            } catch (Exception e) {
                Slog.e(TAG, "Error occurs when to read allow install third app list.");
            }
        }
    }

    private static boolean neednotInstall(File apk) {
        PackageLite pl;
        if (apk == null || (pl = parsePackageLite(apk)) == null) {
            return false;
        }
        if (!sCloudControlUninstall.contains(pl.getPackageName())) {
            Set<String> set = sAllowOnlyInstallThirdApps;
            if (set.size() <= 0 || set.contains(pl.getPackageName())) {
                Set<String> set2 = sDisallowInstallThirdApps;
                if ((set2.size() <= 0 || !set2.contains(pl.getPackageName())) && !isUninstallByMccOrMnc(pl.getPackageName())) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isUninstallByMccOrMnc(String packageName) {
        if (!"com.yandex.searchapp".equals(packageName)) {
            return false;
        }
        String sku = SystemProperties.get("ro.miui.build.region", "");
        if (!TextUtils.equals(sku, "eea") && !TextUtils.equals(sku, "global")) {
            return false;
        }
        boolean isUninstalled = !"ru_operator".equals(SystemProperties.get("persist.sys.carrier.subnetwork", ""));
        return isUninstalled;
    }

    public static void installCustApps(Context context) {
        List<File> needToInstall;
        List<File> custAppList;
        List<File> custAppList2 = getCustomizePreinstallAppList();
        if (custAppList2.isEmpty()) {
            Slog.w(TAG, " No cust app need to install");
            return;
        }
        long currentTime = System.currentTimeMillis();
        Slog.i(TAG, "Install cust apps start");
        Map<String, Long> history = new HashMap<>();
        readHistory(history);
        Map<String, Integer> pkgMap = new HashMap<>();
        Map<String, String> pkgPathMap = new HashMap<>();
        List<File> needToInstall2 = new ArrayList<>();
        readAllowOnlyInstallThirdAppForCarrier();
        for (File app : custAppList2) {
            File apkFile = getApkFile(app);
            if (!dealed(history, apkFile) && !neednotInstall(apkFile)) {
                needToInstall2.add(apkFile);
            }
        }
        sCloudControlUninstall.clear();
        sAllowOnlyInstallThirdApps.clear();
        sDisallowInstallThirdApps.clear();
        Map<File, Integer> installedResult = InstallerUtil.installAppList(context, needToInstall2);
        for (Map.Entry<File, Integer> entry : installedResult.entrySet()) {
            File apkFile2 = entry.getKey();
            int result = entry.getValue().intValue();
            if (result == 0) {
                custAppList = custAppList2;
                needToInstall = needToInstall2;
                Slog.i(TAG, "Install cust app [" + apkFile2.getPath() + "] mtime[" + apkFile2.lastModified() + "]");
                PackageLite pl = parsePackageLite(apkFile2);
                if (pl != null && !TextUtils.isEmpty(pl.getPackageName())) {
                    pkgMap.put(pl.getPackageName(), Integer.valueOf(pl.getVersionCode()));
                    pkgPathMap.put(pl.getPackageName(), apkFile2.getPath());
                }
                recordToHistory(history, apkFile2);
            } else {
                custAppList = custAppList2;
                needToInstall = needToInstall2;
                Slog.w(TAG, "Install cust app [" + apkFile2.getPath() + "] fail, result[" + result + "]");
            }
            custAppList2 = custAppList;
            needToInstall2 = needToInstall;
        }
        writeHistory(history);
        writePreinstallPackage(pkgMap);
        writePreInstallPackagePath(pkgPathMap);
        Slog.i(TAG, "Install cust apps end, consume " + (System.currentTimeMillis() - currentTime) + "ms");
    }

    public static void installVanwardCustApps(Context context) {
        Set<String> vanwardCustAppSet;
        List<File> custAppList;
        List<File> needToInstall;
        List<File> custAppList2 = getCustomizePreinstallAppList();
        Set<String> vanwardCustAppSet2 = new HashSet<>();
        readLineToSet(new File(CustomizeUtil.getMiuiAppDir(), "vanward_applist"), vanwardCustAppSet2);
        if (!custAppList2.isEmpty() && !vanwardCustAppSet2.isEmpty()) {
            long currentTime = System.currentTimeMillis();
            Slog.i(TAG, "Install vanward cust apps start");
            Map<String, Long> history = new HashMap<>();
            readHistory(history);
            Map<String, Integer> pkgMap = new HashMap<>();
            Map<String, String> pkgPathMap = new HashMap<>();
            List<File> needToInstall2 = new ArrayList<>();
            for (File app : custAppList2) {
                File apkFile = getApkFile(app);
                if (vanwardCustAppSet2.contains(apkFile.getName()) && !dealed(history, apkFile)) {
                    needToInstall2.add(apkFile);
                }
            }
            Map<File, Integer> installedResult = InstallerUtil.installAppList(context, needToInstall2);
            for (Map.Entry<File, Integer> entry : installedResult.entrySet()) {
                File apkFile2 = entry.getKey();
                int result = entry.getValue().intValue();
                if (result == 0) {
                    custAppList = custAppList2;
                    vanwardCustAppSet = vanwardCustAppSet2;
                    needToInstall = needToInstall2;
                    Slog.i(TAG, "install vanward cust app [" + apkFile2.getPath() + "] mtime[" + apkFile2.lastModified() + "]");
                    PackageLite pl = parsePackageLite(apkFile2);
                    if (pl != null && !TextUtils.isEmpty(pl.getPackageName())) {
                        pkgPathMap.put(pl.getPackageName(), apkFile2.getPath());
                        pkgMap.put(pl.getPackageName(), Integer.valueOf(pl.getVersionCode()));
                    }
                    recordToHistory(history, apkFile2);
                } else {
                    custAppList = custAppList2;
                    vanwardCustAppSet = vanwardCustAppSet2;
                    needToInstall = needToInstall2;
                    Slog.w(TAG, "Install vanward cust app [" + apkFile2.getPath() + "] fail, result[" + result + "]");
                }
                needToInstall2 = needToInstall;
                custAppList2 = custAppList;
                vanwardCustAppSet2 = vanwardCustAppSet;
            }
            writeHistory(history);
            writePreinstallPackage(pkgMap);
            writePreInstallPackagePath(pkgPathMap);
            Slog.i(TAG, "Install vanward cust apps end, consume " + (System.currentTimeMillis() - currentTime) + "ms");
            return;
        }
        Slog.w(TAG, "No vanward cust app need to install");
    }

    private static boolean isValidIme(String locale, Locale curLocale) {
        String[] locales = locale.split(",");
        for (int i = 0; i < locales.length; i++) {
            if (locales[i].startsWith(curLocale.toString()) || locales[i].equals("*") || locales[i].startsWith(curLocale.getLanguage() + "_*")) {
                return true;
            }
        }
        return false;
    }

    private static void writePreInstallPackagePath(Map<String, String> maps) {
        Slog.i(TAG, "Write preinstalled package name into: /data/app/preinstall_package_path");
        if (maps == null || maps.isEmpty()) {
            return;
        }
        BufferedWriter bufferWriter = null;
        try {
            try {
                bufferWriter = new BufferedWriter(new FileWriter(PREINSTALL_PACKAGE_PATH, true));
                for (Map.Entry<String, String> r : maps.entrySet()) {
                    String packageName = r.getKey();
                    String path = r.getValue();
                    if (!TextUtils.isEmpty(packageName) && !TextUtils.isEmpty(path)) {
                        bufferWriter.write(packageName + ":" + path);
                        bufferWriter.write("\n");
                    }
                }
            } catch (Exception e) {
                Slog.e(TAG, "Error occurs when to write preinstalled package path.");
                if (bufferWriter == null) {
                    return;
                }
            }
            IoUtils.closeQuietly(bufferWriter);
        } catch (Throwable th) {
            if (bufferWriter != null) {
                IoUtils.closeQuietly(bufferWriter);
            }
            throw th;
        }
    }

    private static void writePreinstallPackage(Map<String, Integer> maps) {
        Slog.i(TAG, "Write preinstalled package name into /data/system/preinstall.list");
        if (maps == null || maps.isEmpty()) {
            return;
        }
        BufferedWriter bufferWriter = null;
        try {
            try {
                bufferWriter = new BufferedWriter(new FileWriter(PREINSTALL_PACKAGE_LIST, true));
                synchronized (mPackageVersionMap) {
                    for (Map.Entry<String, Integer> r : maps.entrySet()) {
                        mPackageVersionMap.put(r.getKey(), r.getValue());
                        bufferWriter.write(r.getKey() + ":" + r.getValue());
                        bufferWriter.write("\n");
                    }
                }
            } catch (IOException e) {
                Slog.e(TAG, "Error occurs when to write preinstalled package name.");
            }
        } finally {
            IoUtils.closeQuietly(bufferWriter);
        }
    }

    public static void writePreinstallPAIPackage(String pkg) {
        Slog.i(TAG, "Write PAI package name into /data/system/preinstallPAI.list");
        if (TextUtils.isEmpty(pkg)) {
            return;
        }
        BufferedWriter bufferWriter = null;
        try {
            try {
                bufferWriter = new BufferedWriter(new FileWriter(PREINSTALL_PACKAGE_PAI_LIST, true));
                synchronized (mPackagePAIList) {
                    mPackagePAIList.add(pkg);
                    bufferWriter.write(pkg);
                    bufferWriter.write("\n");
                }
            } catch (IOException e) {
                Slog.e(TAG, "Error occurs when to write PAI package name.");
            }
        } finally {
            IoUtils.closeQuietly(bufferWriter);
        }
    }

    public static ArrayList<String> getPeinstalledChannelList() {
        ArrayList<String> preinstalledChannelList = new ArrayList<>();
        File custVariantDir = CustomizeUtil.getMiuiCustVariantDir();
        if (custVariantDir != null) {
            String custVariantPath = custVariantDir.getPath();
            File file = CUSTOMIZED_APP_DIR;
            addPreinstallChannelToList(preinstalledChannelList, file, custVariantPath + "/customized_channellist");
            addPreinstallChannelToList(preinstalledChannelList, file, custVariantPath + "/ota_customized_channellist");
            File file2 = RECOMMENDED_APP_DIR;
            addPreinstallChannelToList(preinstalledChannelList, file2, custVariantPath + "/recommended_channellist");
            addPreinstallChannelToList(preinstalledChannelList, file2, custVariantPath + "/ota_recommended_channellist");
        }
        return preinstalledChannelList;
    }

    private static void addPreinstallChannelToList(List<String> preinstallChannelList, File channelDir, String channelListFile) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(channelListFile));
            while (true) {
                String channelName = reader.readLine();
                if (channelName != null) {
                    preinstallChannelList.add(channelDir.getPath() + "/" + channelName);
                } else {
                    reader.close();
                    return;
                }
            }
        } catch (IOException e) {
            Slog.e(TAG, "Error occurs while read preinstalled channels " + e);
        }
    }

    private static boolean isSamePackage(File appFileA, File appFileB) {
        PackageLite plA = parsePackageLite(appFileA);
        PackageLite plB = parsePackageLite(appFileB);
        if (plA == null || plA.getPackageName() == null) {
            Slog.e(TAG, "Parse " + appFileA.getPath() + " failed, return false");
            return false;
        } else if (plB == null || plB.getPackageName() == null) {
            Slog.e(TAG, "Parse " + appFileB.getPath() + " failed, return false");
            return false;
        } else if (plA.getPackageName().equals(plB.getPackageName())) {
            return true;
        } else {
            Slog.e(TAG, "isSamePackage return false.");
            return false;
        }
    }

    private static void readPackageVersionMap() {
        BufferedReader reader = null;
        try {
            try {
                reader = new BufferedReader(new FileReader(PREINSTALL_PACKAGE_LIST));
                while (true) {
                    String line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    String[] ss = line.split(":");
                    if (ss != null && (ss.length == 1 || ss.length == 2)) {
                        int version = 0;
                        try {
                            if (ss.length == 2) {
                                version = Integer.valueOf(ss[1]).intValue();
                            }
                            mPackageVersionMap.put(ss[0], Integer.valueOf(version));
                        } catch (NumberFormatException e) {
                        }
                    }
                }
            } catch (IOException e2) {
                Slog.e(TAG, "Error occurs while read preinstalled packages " + e2);
                if (reader == null) {
                    return;
                }
            }
            IoUtils.closeQuietly(reader);
        } catch (Throwable th) {
            if (reader != null) {
                IoUtils.closeQuietly(reader);
            }
            throw th;
        }
    }

    private static void readPackagePAIList() {
        BufferedReader reader = null;
        try {
            try {
                reader = new BufferedReader(new FileReader(PREINSTALL_PACKAGE_PAI_LIST));
                while (true) {
                    String line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    mPackagePAIList.add(line);
                }
            } catch (IOException e) {
                Slog.e(TAG, "Error occurs while read preinstalled PAI packages " + e);
                if (reader == null) {
                    return;
                }
            }
            IoUtils.closeQuietly(reader);
        } catch (Throwable th) {
            if (reader != null) {
                IoUtils.closeQuietly(reader);
            }
            throw th;
        }
    }

    private static boolean skipOTA(Item item) {
        try {
            String deviceName = (String) Class.forName("android.os.SystemProperties").getMethod("get", String.class, String.class).invoke(null, "ro.product.name", "");
            if ("sagit".equals(deviceName) && "com.xiaomi.youpin".equals(item.packageName)) {
                Slog.i(TAG, "skipOTA, deviceName is " + deviceName);
                return false;
            }
        } catch (Exception e) {
            Slog.e(TAG, "Get exception", e);
        }
        return NOT_OTA_PACKAGE_NAMES.contains(item.packageName);
    }

    public static boolean isPreinstalledPackage(String pkg) {
        boolean containsKey;
        synchronized (mPackageVersionMap) {
            containsKey = mPackageVersionMap.containsKey(pkg);
        }
        return containsKey;
    }

    public static boolean isPreinstalledPAIPackage(String pkg) {
        boolean contains;
        synchronized (mPackagePAIList) {
            contains = mPackagePAIList.contains(pkg);
        }
        return contains;
    }

    public static void removeFromPreinstallList(String pkg) {
        synchronized (mPackageVersionMap) {
            if (!mPackageVersionMap.containsKey(pkg)) {
                return;
            }
            mPackageVersionMap.remove(pkg);
            File tempFile = new File("/data/system/preinstall.list.tmp");
            JournaledFile journal = new JournaledFile(new File(PREINSTALL_PACKAGE_LIST), tempFile);
            File writeTarget = journal.chooseForWrite();
            BufferedOutputStream str = null;
            try {
                try {
                    FileOutputStream fstr = new FileOutputStream(writeTarget);
                    str = new BufferedOutputStream(fstr);
                    FileUtils.setPermissions(fstr.getFD(), 416, 1000, 1032);
                    StringBuilder sb = new StringBuilder();
                    synchronized (mPackageVersionMap) {
                        for (Map.Entry<String, Integer> map : mPackageVersionMap.entrySet()) {
                            sb.setLength(0);
                            sb.append(map.getKey());
                            sb.append(":");
                            sb.append(map.getValue());
                            sb.append("\n");
                            str.write(sb.toString().getBytes());
                        }
                    }
                    str.flush();
                    FileUtils.sync(fstr);
                    journal.commit();
                } catch (Exception e) {
                    Slog.wtf(TAG, "Failed to write preinstall.list + ", e);
                    journal.rollback();
                }
                IoUtils.closeQuietly(str);
                Slog.d(TAG, "Delete package:" + pkg + " from preinstall.list");
            } catch (Throwable th) {
                IoUtils.closeQuietly((AutoCloseable) null);
                throw th;
            }
        }
    }

    public static void removeFromPreinstallPAIList(String pkg) {
        synchronized (mPackagePAIList) {
            if (!mPackagePAIList.contains(pkg)) {
                return;
            }
            mPackagePAIList.remove(pkg);
            File tempFile = new File("/data/system/preinstallPAI.list.tmp");
            JournaledFile journal = new JournaledFile(new File(PREINSTALL_PACKAGE_PAI_LIST), tempFile);
            File writeTarget = journal.chooseForWrite();
            FileOutputStream fstr = null;
            BufferedOutputStream str = null;
            try {
                try {
                    fstr = new FileOutputStream(writeTarget);
                    str = new BufferedOutputStream(fstr);
                    FileUtils.setPermissions(fstr.getFD(), 416, 1000, 1032);
                    StringBuilder sb = new StringBuilder();
                    synchronized (mPackagePAIList) {
                        for (int i = 0; i < mPackagePAIList.size(); i++) {
                            sb.setLength(0);
                            sb.append(mPackagePAIList.get(i));
                            sb.append("\n");
                            str.write(sb.toString().getBytes());
                        }
                    }
                    str.flush();
                    FileUtils.sync(fstr);
                    journal.commit();
                } catch (Exception e) {
                    Slog.wtf(TAG, "Failed to delete preinstallPAI.list + ", e);
                    journal.rollback();
                }
                IoUtils.closeQuietly(fstr);
                IoUtils.closeQuietly(str);
                Slog.d(TAG, "Delete package:" + pkg + " from preinstallPAI.list");
            } catch (Throwable th) {
                IoUtils.closeQuietly((AutoCloseable) null);
                IoUtils.closeQuietly((AutoCloseable) null);
                throw th;
            }
        }
    }

    public static void copyPreinstallPAITrackingFile(String type, String fileName, String content) {
        String filePath;
        if (TextUtils.isEmpty(type) || TextUtils.isEmpty(content)) {
            return;
        }
        boolean isAppsflyer = false;
        if (TextUtils.equals(TYPE_TRACKING_APPSFLYER, type)) {
            filePath = PREINSTALL_PACKAGE_PAI_TRACKING_FILE;
            isAppsflyer = true;
        } else if (TextUtils.equals(TYPE_TRACKING_MIUI, type)) {
            filePath = PREINSTALL_PACKAGE_MIUI_TRACKING_DIR + fileName;
        } else {
            Slog.e(TAG, "Used invalid pai tracking type =" + type + "! you can use type:appsflyer or xiaomi");
            return;
        }
        if (isAppsflyer && mNewTrackContentList.contains(content)) {
            Slog.i(TAG, "Content duplication dose not need to be written again! content is :" + content);
            return;
        }
        String str = TAG;
        Slog.i(str, "use " + type + " tracking method!");
        BufferedWriter bw = null;
        try {
            try {
                File file = new File(filePath);
                if (!file.exists()) {
                    Slog.d(str, "create tracking file：" + file.getAbsolutePath());
                    if (!file.getParentFile().exists()) {
                        file.getParentFile().mkdir();
                        FileUtils.setPermissions(file.getParentFile(), 509, -1, -1);
                    }
                    file.createNewFile();
                }
                FileUtils.setPermissions(file, 436, -1, -1);
                bw = new BufferedWriter(new FileWriter(file, isAppsflyer));
                if (isAppsflyer) {
                    mNewTrackContentList.add(content);
                }
                bw.write(content);
                bw.write("\n");
                bw.flush();
                Slog.i(str, "Copy PAI tracking content Success!");
                IoUtils.closeQuietly(bw);
                restoreconPreinstallDir();
            } catch (IOException e) {
                if (TextUtils.equals(TYPE_TRACKING_APPSFLYER, type)) {
                    Slog.e(TAG, "Error occurs when to copy PAI tracking content(" + content + ") into " + filePath + ":" + e);
                } else if (TextUtils.equals(TYPE_TRACKING_MIUI, type)) {
                    Slog.e(TAG, "Error occurs when to create " + fileName + " PAI tracking file into " + filePath + ":" + e);
                }
                IoUtils.closeQuietly(bw);
            }
        } catch (Throwable th) {
            IoUtils.closeQuietly(bw);
            throw th;
        }
    }

    private static void copyTraditionalTrackFileToNewLocationIfNeed() {
        readNewPAITrackFileIfNeed();
        String appsflyerPath = SystemProperties.get("ro.appsflyer.preinstall.path", "");
        if (TextUtils.isEmpty(appsflyerPath)) {
            Slog.e(TAG, "no system property ro.appsflyer.preinstall.path");
            return;
        }
        File paiTrackingPath = new File(PREINSTALL_PACKAGE_PAI_TRACKING_FILE);
        if (TextUtils.equals(appsflyerPath, PREINSTALL_PACKAGE_PAI_TRACKING_FILE) && !paiTrackingPath.exists()) {
            try {
                Slog.d(TAG, "create new appsflyer tracking file：" + paiTrackingPath.getAbsolutePath());
                if (!paiTrackingPath.getParentFile().exists()) {
                    paiTrackingPath.getParentFile().mkdir();
                    FileUtils.setPermissions(paiTrackingPath.getParentFile(), 509, -1, -1);
                }
                paiTrackingPath.createNewFile();
                FileUtils.setPermissions(paiTrackingPath, 436, -1, -1);
            } catch (IOException e) {
                Slog.e(TAG, "Error occurs when to create new appsflyer tracking" + e);
            }
            readTraditionalPAITrackFile();
            writeNewPAITrackFile();
        }
        restoreconPreinstallDir();
    }

    private static void readTraditionalPAITrackFile() {
        Slog.i(TAG, "read traditional track file content from /cust/etc/pre_install.appsflyer");
        File file = new File(OLD_PREINSTALL_PACKAGE_PAI_TRACKING_FILE);
        if (file.exists()) {
            mTraditionalTrackContentList.clear();
            BufferedReader reader = null;
            try {
                try {
                    reader = new BufferedReader(new FileReader(file));
                    while (true) {
                        String line = reader.readLine();
                        if (line == null) {
                            break;
                        }
                        mTraditionalTrackContentList.add(line);
                    }
                } catch (IOException e) {
                    Slog.e(TAG, "Error occurs while read traditional track file content" + e);
                }
            } finally {
                IoUtils.closeQuietly(reader);
            }
        }
    }

    private static void writeNewPAITrackFile() {
        String str = TAG;
        Slog.i(str, "Write old track file content to  /data/miui/pai/pre_install.appsflyer");
        List<String> list = mTraditionalTrackContentList;
        if (list == null || list.isEmpty()) {
            Slog.e(str, "no content write to new appsflyer file");
            return;
        }
        BufferedWriter bufferWriter = null;
        try {
            try {
                File newFile = new File(PREINSTALL_PACKAGE_PAI_TRACKING_FILE);
                if (!newFile.getParentFile().exists()) {
                    newFile.getParentFile().mkdirs();
                    FileUtils.setPermissions(newFile.getParentFile(), 509, -1, -1);
                }
                bufferWriter = new BufferedWriter(new FileWriter(newFile, true));
                FileUtils.setPermissions(newFile, 436, -1, -1);
                synchronized (mTraditionalTrackContentList) {
                    for (int i = 0; i < mTraditionalTrackContentList.size(); i++) {
                        String content = mTraditionalTrackContentList.get(i);
                        if (!mNewTrackContentList.contains(content)) {
                            mNewTrackContentList.add(content);
                            bufferWriter.write(content);
                            bufferWriter.write("\n");
                        }
                    }
                }
            } catch (IOException e) {
                Slog.e(TAG, "Error occurs when to write track file from /cust/etc/pre_install.appsflyer to /data/miui/pai/pre_install.appsflyer");
            }
        } finally {
            IoUtils.closeQuietly(bufferWriter);
        }
    }

    private static void readNewPAITrackFileIfNeed() {
        Slog.i(TAG, "read new track file content from /data/miui/pai/pre_install.appsflyer");
        File file = new File(PREINSTALL_PACKAGE_PAI_TRACKING_FILE);
        if (file.exists()) {
            BufferedReader reader = null;
            try {
                try {
                    reader = new BufferedReader(new FileReader(file));
                    while (true) {
                        String line = reader.readLine();
                        if (line == null) {
                            break;
                        } else if (!mNewTrackContentList.contains(line)) {
                            mNewTrackContentList.add(line);
                        }
                    }
                } catch (IOException e) {
                    Slog.e(TAG, "Error occurs while read new track file content on pkms start" + e);
                }
            } finally {
                IoUtils.closeQuietly(reader);
            }
        }
    }

    private static void restoreconPreinstallDir() {
        File file = new File(PREINSTALL_PACKAGE_MIUI_TRACKING_DIR);
        String fileContext = SELinux.getFileContext(PREINSTALL_PACKAGE_MIUI_TRACKING_DIR);
        if (file.exists() && !TextUtils.equals(fileContext, PREINSTALL_PACKAGE_MIUI_TRACKING_DIR_CONTEXT)) {
            SELinux.restoreconRecursive(file);
        }
    }

    public static int getPreinstalledAppVersion(String pkg) {
        int intValue;
        synchronized (mPackageVersionMap) {
            Integer version = mPackageVersionMap.get(pkg);
            intValue = version != null ? version.intValue() : 0;
        }
        return intValue;
    }

    public static boolean isPreinstallApp(String packageName) {
        return sPreinstallApps.containsKey(packageName);
    }

    static ApkLite parsePackage(File apkFile) {
        ParseTypeImpl input = ParseTypeImpl.forDefaultParsing();
        ParseResult<ApkLite> result = ApkLiteParseUtils.parseApkLite(input.reset(), apkFile, 32);
        if (result.isError()) {
            Slog.e(TAG, "Failed to parseApkLite: " + apkFile + " error: " + result.getErrorMessage(), result.getException());
            return null;
        }
        return (ApkLite) result.getResult();
    }

    public static PackageLite parsePackageLite(File apkFile) {
        ParseTypeImpl input = ParseTypeImpl.forDefaultParsing();
        ParseResult<PackageLite> result = ApkLiteParseUtils.parsePackageLite(input.reset(), apkFile, 0);
        if (result.isError()) {
            Slog.e(TAG, "Failed to parsePackageLite: " + apkFile + " error: " + result.getErrorMessage(), result.getException());
            return null;
        }
        return (PackageLite) result.getResult();
    }

    public static void recordAdvanceAppsHistory(Item item) {
        String path = item.apkFile.getPath();
        if (path == null) {
            return;
        }
        if (path.contains(AdvancePreinstallService.ADVANCE_TAG) || path.contains(AdvancePreinstallService.ADVERT_TAG)) {
            sAdvanceApps.put(item.packageName, item.apkFile.getPath());
        }
    }

    public static void exemptPermissionRestrictions() {
        PackageManagerInternal packageManagerInt = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        PermissionManagerService permService = ServiceManager.getService("permissionmgr");
        for (Item item : sNewOrUpdatePreinstallApps) {
            AndroidPackage pkg = packageManagerInt.getPackage(item.packageName);
            if (pkg != null) {
                Slog.i(TAG, "exemptPermissionRestrictions package: " + pkg.getPackageName());
                exemptPackagePermissionRestrictions(pkg, permService);
            }
        }
    }

    private static void exemptPackagePermissionRestrictions(AndroidPackage pkg, PermissionManagerService permService) {
        for (String permissionName : pkg.getRequestedPermissions()) {
            if (isPermissionRestricted(pkg.getPackageName(), permissionName, permService)) {
                permService.addAllowlistedRestrictedPermission(pkg.getPackageName(), permissionName, 4, 0);
            }
        }
    }

    private static boolean isPermissionRestricted(String pkgName, String permissionName, PermissionManagerService permService) {
        PermissionInfo permissionInfo = permService.getPermissionInfo(permissionName, pkgName, 0);
        return permissionInfo != null && permissionInfo.isRestricted();
    }
}
