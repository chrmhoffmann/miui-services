package com.android.server.pm;

import android.app.ActivityManagerInternal;
import android.app.ActivityThread;
import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageDeleteObserver2;
import android.content.pm.PackageHideManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManagerInternal;
import android.content.pm.PackageParser;
import android.content.pm.ResolveInfo;
import android.content.pm.SigningDetails;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.miui.AppOpsUtils;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;
import android.util.Slog;
import com.android.commands.pm.PmInjector;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.ArrayUtils;
import com.android.server.LocalServices;
import com.android.server.SystemConfig;
import com.android.server.am.ProcessUtils;
import com.android.server.inputmethod.InputMethodManagerServiceImpl;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.dex.DexoptOptions;
import com.android.server.pm.dex.PackageDexUsage;
import com.android.server.pm.parsing.PackageInfoUtils;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.pm.parsing.pkg.AndroidPackageUtils;
import com.android.server.pm.parsing.pkg.ParsedPackage;
import com.android.server.pm.permission.LegacyPermission;
import com.android.server.pm.permission.LegacyPermissionSettings;
import com.android.server.pm.permission.PermissionManagerService;
import com.android.server.wm.ActivityTaskSupervisorImpl;
import com.miui.base.MiuiStubRegistry;
import com.miui.enterprise.ApplicationHelper;
import com.miui.enterprise.settings.EnterpriseSettings;
import com.miui.enterprise.signature.EnterpriseVerifier;
import com.miui.hybrid.hook.HookClient;
import com.miui.server.AccessController;
import com.miui.server.GreenGuardManagerService;
import com.miui.server.input.util.MiuiCustomizeShortCutUtils;
import com.miui.server.xspace.SecSpaceManagerService;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import libcore.io.IoUtils;
import libcore.util.HexEncoding;
import miui.content.pm.IPackageDeleteConfirmObserver;
import miui.content.pm.PreloadedAppPolicy;
import miui.mqsas.sdk.BootEventManager;
import miui.os.Build;
import miui.util.DeviceLevel;
import miui.util.FeatureParser;
import miui.util.ReflectionUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
/* loaded from: classes.dex */
public class PackageManagerServiceImpl extends PackageManagerServiceStub {
    private static final String APP_LIST_FILE = "/system/etc/install_app_filter.xml";
    private static final int CORE_THREAD_NUM = 2;
    static final boolean DEBUG_CHANGE = true;
    static final int DELETE_FAILED_FORBIDED_BY_MIUI = -1000;
    public static final String DEXOPT_BG_TAG = "bg-filter:";
    private static final String GLOBAL_SYNC_KEY_ENABLE = "1";
    private static final String GOOGLE_INSTALLER_PACKAGE = "com.google.android.packageinstaller";
    private static final String GOOGLE_MARKET_PACKAGE = "com.android.vending";
    private static final String GOOGLE_WEB_SEARCH_PACKAGE = "com.google.android.googlequicksearchbox";
    public static final int INSTALL_FULL_APP = 16384;
    static final int INSTALL_IGNORE_PACKAGE = -1000;
    public static final int INSTALL_REASON_USER = 4;
    private static final long KEEP_ALIVE_DURATION = 60;
    private static final String MANAGED_PROVISION = "com.android.managedprovisioning";
    private static final int MAX_THREAD_NUM = 2;
    private static final String MIUI_BROWSER_PACKAGE = "com.android.browser";
    private static final HashSet<String> MIUI_SYSTEM_PACKAGES;
    static final int MIUI_VERIFICATION_TIMEOUT = -100;
    private static final String PACKAGE_INSTALLER_NAME = "com.google.android.packageinstaller";
    private static final String PACKAGE_MIME_TYPE = "application/vnd.android.package-archive";
    private static final String PACKAGE_WEBVIEW = "com.google.android.webview";
    private static final String SUPPORT_OLDMAN_MODE = "support_oldman_mode";
    static final String TAG = "PKMSImpl";
    private static final String TAG_ADD_APPS = "add_apps";
    private static final String TAG_APP = "app";
    private static final String TAG_IGNORE_APPS = "ignore_apps";
    static final ArrayList<String> sAllowPackage;
    static ArrayList<String> sCrossXSpaceIntent;
    private static volatile Handler sDexMetaDataHandler;
    private static HandlerThread sDexMetaDataThread;
    private static boolean sEnableBgDexopt;
    private static volatile Handler sFirstUseHandler;
    private static Object sFirstUseLock;
    private static HandlerThread sFirstUseThread;
    private static final ReentrantReadWriteLock sGlobalRWLock;
    private static String sGlobalSyncKey;
    private static final ArrayList<String> sGlobalWhitePackageList;
    public static final Set<String> sInstallerSet;
    static final ArrayList<String> sNoVerifyAllowPackage;
    private static final List<String> sNonProfileCompiledPkgs;
    private static volatile Handler sOtaSpeedProfileHandler;
    private static Object sOtaSpeedProfileLock;
    private static HandlerThread sOtaSpeedProfileThread;
    static ArrayList<String> sShellCheckPermissions;
    private static Set<String> sTop10ThirdPartyPks;
    static ArrayList<String> sXSpaceAddUserAuthorityBlackList;
    static ArrayList<String> sXSpaceDataSchemeWhiteList;
    private static final ArrayList<String> sXSpaceFriendlyActionList;
    private Context mContext;
    private String mCurrentPackageInstaller;
    private int mDexMetaDataWaitTime;
    private DexoptServiceThread mDexoptServiceThread;
    private boolean mEnableDexMetaData;
    private PackageManagerServiceInjector mInjector;
    private AndroidPackage mMiuiInstallerPackage;
    private PackageSetting mMiuiInstallerPackageSetting;
    private int mOtaDexOptStatus;
    private PackageManagerService.IPackageManagerImpl mPM;
    private PackageDexOptimizer mPdo;
    private Settings mPkgSettings;
    private PackageManagerService mPms;
    private ThreadPoolExecutor sPoolExecutor;
    private static Boolean sIsPlatformSignature = null;
    private static Object sDexMetaDataLock = new Object();
    private static final String ANDROID_INSTALLER_PACKAGE = "com.android.packageinstaller";
    private static final String MIUI_INSTALLER_PACKAGE = "com.miui.packageinstaller";
    static final String[] EP_INSTALLER_PKG_WHITELIST = {ANDROID_INSTALLER_PACKAGE, MIUI_INSTALLER_PACKAGE};
    private static final String MIUI_MARKET_PACKAGE = "com.xiaomi.market";
    private static final String[] MIUI_CORE_APPS = {"com.lbe.security.miui", ActivityTaskSupervisorImpl.MIUI_APP_LOCK_PACKAGE_NAME, "com.android.updater", MIUI_MARKET_PACKAGE, "com.xiaomi.finddevice", InputMethodManagerServiceImpl.MIUI_HOME};
    private static final Set<String> sSilentlyUninstallPackages = new HashSet();
    private final Set<String> mIgnoreApks = new HashSet();
    private final Set<String> mIgnorePackages = new HashSet();
    private String mRsaFeature = null;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<PackageManagerServiceImpl> {

        /* compiled from: PackageManagerServiceImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final PackageManagerServiceImpl INSTANCE = new PackageManagerServiceImpl();
        }

        public PackageManagerServiceImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public PackageManagerServiceImpl provideNewInstance() {
            throw new RuntimeException("Impl class com.android.server.pm.PackageManagerServiceImpl is marked as singleton");
        }
    }

    public PackageManagerServiceImpl() {
        Set<String> set = sSilentlyUninstallPackages;
        set.add(SystemProperties.get("ro.miui.product.home", InputMethodManagerServiceImpl.MIUI_HOME));
        set.add(MIUI_MARKET_PACKAGE);
        set.add("com.xiaomi.mipicks");
        set.add("com.xiaomi.discover");
        set.add("com.xiaomi.gamecenter");
        set.add("com.xiaomi.gamecenter.pad");
        set.add("com.miui.global.packageinstaller");
        set.add("com.google.android.packageinstaller");
        set.add(GreenGuardManagerService.GREEN_KID_AGENT_PKG_NAME);
        set.add("com.miui.cleaner");
        set.add("com.miui.cloudbackup");
        if (Build.IS_INTERNATIONAL_BUILD) {
            set.add("de.telekom.tsc");
            set.add("com.sfr.android.sfrjeux");
            set.add("com.altice.android.myapps");
        }
        this.mOtaDexOptStatus = 0;
    }

    static {
        ArraySet arraySet = new ArraySet();
        sInstallerSet = arraySet;
        arraySet.add(MIUI_INSTALLER_PACKAGE);
        arraySet.add("com.google.android.packageinstaller");
        arraySet.add(ANDROID_INSTALLER_PACKAGE);
        ArrayList<String> arrayList = new ArrayList<>();
        sShellCheckPermissions = arrayList;
        arrayList.add("android.permission.SEND_SMS");
        sShellCheckPermissions.add("android.permission.CALL_PHONE");
        sShellCheckPermissions.add("android.permission.READ_CONTACTS");
        sShellCheckPermissions.add("android.permission.WRITE_CONTACTS");
        sShellCheckPermissions.add("android.permission.CLEAR_APP_USER_DATA");
        sShellCheckPermissions.add("android.permission.WRITE_SECURE_SETTINGS");
        sShellCheckPermissions.add("android.permission.WRITE_SETTINGS");
        sShellCheckPermissions.add("android.permission.MANAGE_DEVICE_ADMINS");
        sShellCheckPermissions.add("android.permission.UPDATE_APP_OPS_STATS");
        sShellCheckPermissions.add("android.permission.INJECT_EVENTS");
        sShellCheckPermissions.add("android.permission.INSTALL_GRANT_RUNTIME_PERMISSIONS");
        sShellCheckPermissions.add("android.permission.GRANT_RUNTIME_PERMISSIONS");
        sShellCheckPermissions.add("android.permission.REVOKE_RUNTIME_PERMISSIONS");
        sShellCheckPermissions.add("android.permission.SET_PREFERRED_APPLICATIONS");
        if ((Build.IS_DEBUGGABLE && (SystemProperties.getInt("ro.secureboot.devicelock", 0) == 0 || "unlocked".equals(SystemProperties.get("ro.secureboot.lockstate")))) || Build.IS_TABLET) {
            sShellCheckPermissions.clear();
        }
        sAllowPackage = new ArrayList<>();
        ArrayList<String> arrayList2 = new ArrayList<>();
        sNoVerifyAllowPackage = arrayList2;
        arrayList2.add("android");
        arrayList2.add("com.android.provision");
        arrayList2.add("com.miui.securitycore");
        arrayList2.add(GOOGLE_MARKET_PACKAGE);
        arrayList2.add(MIUI_MARKET_PACKAGE);
        arrayList2.add("com.xiaomi.gamecenter");
        arrayList2.add(ActivityTaskSupervisorImpl.MIUI_APP_LOCK_PACKAGE_NAME);
        arrayList2.add("com.android.updater");
        arrayList2.add("com.amazon.venezia");
        if (Build.IS_INTERNATIONAL_BUILD) {
            arrayList2.add("com.miui.cotaservice");
            arrayList2.add("com.xiaomi.discover");
            arrayList2.add("com.xiaomi.mipicks");
        }
        HashSet<String> hashSet = new HashSet<>(8);
        MIUI_SYSTEM_PACKAGES = hashSet;
        hashSet.add(InputMethodManagerServiceImpl.MIUI_HOME);
        hashSet.add("com.android.contacts");
        hashSet.add("com.android.mms");
        hashSet.add(MIUI_BROWSER_PACKAGE);
        hashSet.add("com.mi.globalbrowser");
        hashSet.add(AccessController.PACKAGE_CAMERA);
        hashSet.add(AccessController.PACKAGE_GALLERY);
        hashSet.add("com.miui.player");
        hashSet.add("com.android.email");
        hashSet.add("com.miui.voiceassist");
        sGlobalWhitePackageList = new ArrayList<>();
        sGlobalSyncKey = "1";
        sGlobalRWLock = new ReentrantReadWriteLock();
        ArrayList<String> arrayList3 = new ArrayList<>();
        sXSpaceFriendlyActionList = arrayList3;
        arrayList3.add("com.sina.weibo.action.sdkidentity");
        arrayList3.add("com.sina.weibo.remotessoservice");
        ArrayList<String> arrayList4 = new ArrayList<>();
        sCrossXSpaceIntent = arrayList4;
        arrayList4.add("android.intent.action.VIEW");
        sCrossXSpaceIntent.add("android.intent.action.SEND");
        sCrossXSpaceIntent.add("android.intent.action.DIAL");
        sCrossXSpaceIntent.add("android.intent.action.PICK");
        sCrossXSpaceIntent.add("android.intent.action.INSERT");
        sCrossXSpaceIntent.add("android.intent.action.GET_CONTENT");
        sCrossXSpaceIntent.add("android.media.action.IMAGE_CAPTURE");
        sCrossXSpaceIntent.add("android.settings.MANAGE_APPLICATIONS_SETTINGS");
        sCrossXSpaceIntent.add("android.settings.APPLICATION_DETAILS_SETTINGS");
        sCrossXSpaceIntent.add("android.provider.Telephony.ACTION_CHANGE_DEFAULT");
        ArrayList<String> arrayList5 = new ArrayList<>();
        sXSpaceDataSchemeWhiteList = arrayList5;
        arrayList5.add("content");
        sXSpaceDataSchemeWhiteList.add("http");
        sXSpaceDataSchemeWhiteList.add("https");
        sXSpaceDataSchemeWhiteList.add("file");
        sXSpaceDataSchemeWhiteList.add("ftp");
        sXSpaceDataSchemeWhiteList.add("ed2k");
        sXSpaceDataSchemeWhiteList.add("geo");
        ArrayList<String> arrayList6 = new ArrayList<>();
        sXSpaceAddUserAuthorityBlackList = arrayList6;
        arrayList6.add("com.android.contacts");
        ArrayList arrayList7 = new ArrayList();
        sNonProfileCompiledPkgs = arrayList7;
        arrayList7.add("com.tencent.mm");
        arrayList7.add("com.tencent.mobileqq");
        arrayList7.add("com.taobao.taobao");
        arrayList7.add("com.eg.android.AlipayGphone");
        arrayList7.add("com.ss.android.article.news");
        sEnableBgDexopt = SystemProperties.getBoolean("persist.bg.dexopt.enable", false);
        sFirstUseLock = new Object();
        sOtaSpeedProfileLock = new Object();
    }

    public static PackageManagerServiceImpl get() {
        return (PackageManagerServiceImpl) PackageManagerServiceStub.get();
    }

    void init(PackageManagerService pms, Settings pkgSettings, Context context) {
        this.mPms = pms;
        this.mInjector = pms.mInjector;
        this.mPdo = pms.mInjector.getPackageDexOptimizer();
        this.mContext = context;
        this.mPkgSettings = pkgSettings;
        initIgnoreApps();
        if (SystemProperties.getBoolean("pm.dexopt.async.enabled", true)) {
            DexoptServiceThread dexoptServiceThread = new DexoptServiceThread(this.mPms, this.mPdo);
            this.mDexoptServiceThread = dexoptServiceThread;
            dexoptServiceThread.start();
        }
    }

    void initIPackageManagerImpl(PackageManagerService.IPackageManagerImpl pm) {
        this.mPM = pm;
    }

    void addMiuiSharedUids() {
        this.mPkgSettings.addSharedUserLPw("android.uid.theme", 6101, 1, 8);
        this.mPkgSettings.addSharedUserLPw("android.uid.backup", 6100, 1, 8);
        this.mPkgSettings.addSharedUserLPw("android.uid.updater", 6102, 1, 8);
        this.mPkgSettings.addSharedUserLPw("android.uid.finddevice", 6110, 1, 8);
    }

    public void beforeSystemReady() {
        PreinstallApp.exemptPermissionRestrictions();
        SecSpaceManagerService.init(this.mContext);
        hideOrDisplayApp();
        if (Build.IS_INTERNATIONAL_BUILD) {
            registerDataObserver(this.mContext);
        }
        preConfigDexMetaData(this.mContext);
    }

    private void hideOrDisplayApp() {
        PackageHideManager phm;
        List<String> ignoreApkPkgNameList;
        if (Build.IS_INTERNATIONAL_BUILD || !Build.IS_CM_CUSTOMIZATION || (ignoreApkPkgNameList = (phm = PackageHideManager.getInstance(this.mPms.isFirstBoot())).getIgnoreApkPkgNameList()) == null) {
            return;
        }
        boolean appHide = phm.isAppHide();
        Slog.i(TAG, "appHide: " + appHide);
        synchronized (this.mPms.mLock) {
            for (String pkgName : ignoreApkPkgNameList) {
                PackageSetting packageSetting = (PackageSetting) this.mPkgSettings.mPackages.get(pkgName);
                if (packageSetting != null) {
                    packageSetting.setInstalled(!appHide, 0);
                }
            }
        }
    }

    void performPreinstallApp() {
        PreinstallApp.copyPreinstallApps(this.mPms, this.mPkgSettings);
        updateDefaultPkgInstallerLocked();
        checkGTSSpecAppOptMode();
    }

    boolean isPreinstallApp(String packageName) {
        return PreinstallApp.isPreinstallApp(packageName);
    }

    boolean isVerificationEnabled(int installerUid) {
        if (Settings.Global.getInt(this.mContext.getContentResolver(), "device_provisioned", 0) != 1) {
            return false;
        }
        return isCTS() || (Build.IS_INTERNATIONAL_BUILD && installerUid != 1000);
    }

    private void initIgnoreApps() {
        if (!FeatureParser.getBoolean("support_fm", true)) {
            this.mIgnoreApks.add("/system/app/FM.apk");
            this.mIgnoreApks.add("/system/app/FM");
        }
        readIgnoreApks();
        this.mIgnorePackages.add("com.sogou.inputmethod.mi");
        boolean isCmccCooperationDevice = this.mContext.getResources().getBoolean(285540401);
        if (Build.IS_INTERNATIONAL_BUILD || (!Build.IS_CM_CUSTOMIZATION && (!Build.IS_CT_CUSTOMIZATION || !isCmccCooperationDevice))) {
            this.mIgnorePackages.add("com.miui.dmregservice");
        }
        try {
            addIgnoreApks("ignoredAppsForPackages", this.mIgnorePackages);
            addIgnoreApks("ignoredAppsForApkPath", this.mIgnoreApks);
        } catch (NumberFormatException e) {
            Slog.e(TAG, e.toString());
        }
        String productPath = Environment.getProductDirectory().getPath();
        String MiuiHomeTPath = productPath + "/priv-app/MiuiHomeT";
        String MiLauncherGlobalPath = productPath + "/priv-app/MiLauncherGlobal";
        if (new File(MiuiHomeTPath).exists() && new File(MiLauncherGlobalPath).exists()) {
            if ("POCO".equals(Build.BRAND)) {
                this.mIgnoreApks.add(MiuiHomeTPath);
                this.mIgnoreApks.add("/system/priv-app/MIUIGlobalMinusScreenWidget");
            } else {
                this.mIgnoreApks.add(MiLauncherGlobalPath);
                this.mIgnoreApks.add("/system/priv-app/MIUIGlobalMinusScreen");
            }
        }
        if (!Build.IS_INTERNATIONAL_BUILD) {
            if (new File("/product/priv-app/GmsCore").exists()) {
                this.mIgnorePackages.add("android.ext.shared");
                this.mIgnorePackages.add("com.android.printservice.recommendation");
                return;
            }
            this.mIgnorePackages.add("com.google.android.gsf");
        }
    }

    private static void addIgnoreApks(String tag, Set<String> set) {
        String[] whiteList = FeatureParser.getStringArray(tag);
        if (whiteList != null && whiteList.length > 0) {
            for (String str : whiteList) {
                String[] item = TextUtils.split(str, ",");
                if (DeviceLevel.TOTAL_RAM <= Integer.parseInt(item[0])) {
                    set.add(item[1]);
                }
            }
        }
    }

    private void readIgnoreApks() {
        String custVariant = Build.getCustVariant();
        if (TextUtils.isEmpty(custVariant)) {
            return;
        }
        InputStream inputStream = null;
        try {
            try {
                inputStream = new FileInputStream(new File(APP_LIST_FILE));
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                XmlPullParser parser = factory.newPullParser();
                parser.setInput(inputStream, "UTF-8");
                String appPath = null;
                boolean is_add_apps = false;
                for (int type = parser.getEventType(); 1 != type; type = parser.next()) {
                    switch (type) {
                        case 2:
                            String tagName = parser.getName();
                            if (parser.getAttributeCount() > 0) {
                                appPath = parser.getAttributeValue(0);
                            }
                            if (TAG_ADD_APPS.equals(tagName)) {
                                is_add_apps = true;
                                break;
                            } else if (TAG_IGNORE_APPS.equals(tagName)) {
                                is_add_apps = false;
                                break;
                            } else if ("app".equals(tagName)) {
                                String[] ss = parser.nextText().split(" ");
                                boolean is_current_cust = false;
                                int i = 0;
                                while (true) {
                                    if (i < ss.length) {
                                        if (!ss[i].equals(custVariant)) {
                                            i++;
                                        } else {
                                            is_current_cust = true;
                                        }
                                    }
                                }
                                if ((is_add_apps && !is_current_cust) || (!is_add_apps && is_current_cust)) {
                                    this.mIgnoreApks.add(appPath);
                                    break;
                                } else if (is_add_apps && is_current_cust && this.mIgnoreApks.contains(appPath)) {
                                    this.mIgnoreApks.remove(appPath);
                                    break;
                                }
                            }
                            break;
                        case 3:
                            String end_tag_name = parser.getName();
                            if (TAG_ADD_APPS.equals(end_tag_name)) {
                                is_add_apps = false;
                                break;
                            } else if (TAG_IGNORE_APPS.equals(end_tag_name)) {
                                is_add_apps = true;
                                break;
                            }
                            break;
                    }
                }
            } catch (IOException | XmlPullParserException e) {
                e.printStackTrace();
            }
        } finally {
            IoUtils.closeQuietly(inputStream);
        }
    }

    boolean shouldIgnoreApp(String packageName, String codePath, String reason) {
        boolean ignored = this.mIgnorePackages.contains(packageName) || this.mIgnoreApks.contains(codePath);
        if (ignored) {
            Slog.i(TAG, "Skip scanning package: " + packageName + ", path=" + codePath + ", reason: " + reason);
        }
        return ignored;
    }

    void clearNoHistoryFlagIfNeed(List<ResolveInfo> resolveInfos, Intent intent) {
        if (!Build.IS_INTERNATIONAL_BUILD || resolveInfos == null) {
            return;
        }
        for (ResolveInfo resolveInfo : resolveInfos) {
            Bundle bundle = resolveInfo.activityInfo.metaData;
            if (bundle != null) {
                try {
                    if (bundle.getBoolean("mi_use_custom_resolver") && resolveInfo.activityInfo.enabled) {
                        Log.i(TAG, "Removing FLAG_ACTIVITY_NO_HISTORY flag for Intent {" + intent.toShortString(true, true, true, false) + "}");
                        intent.removeFlags(1073741824);
                    }
                } catch (Exception e) {
                    Log.w(TAG, e.getMessage());
                }
            }
        }
    }

    static boolean isTrustedEnterpriseInstaller(Context context, int callingUid, String installerPkg) {
        return !EnterpriseSettings.ENTERPRISE_ACTIVATED || !ApplicationHelper.isTrustedAppStoresEnabled(context, UserHandle.getUserId(callingUid)) || ArrayUtils.contains(EP_INSTALLER_PKG_WHITELIST, installerPkg) || ApplicationHelper.getTrustedAppStores(context, UserHandle.getUserId(callingUid)).contains(installerPkg);
    }

    boolean checkEnterpriseRestriction(ParsedPackage pkg) {
        if (pkg.getRequestedPermissions().contains("com.miui.enterprise.permission.ACCESS_ENTERPRISE_API") && !EnterpriseVerifier.verify(this.mContext, pkg.getBaseApkPath(), pkg.getPackageName())) {
            Slog.d("Enterprise", "Verify enterprise signature of package " + pkg.getPackageName() + " failed");
            return true;
        } else if (!EnterpriseSettings.ENTERPRISE_ACTIVATED || !ApplicationHelper.checkEnterprisePackageRestriction(this.mContext, pkg.getPackageName())) {
            return false;
        } else {
            Slog.d("Enterprise", "Installation of package " + pkg.getPackageName() + " is restricted");
            return true;
        }
    }

    PackageInfo hookPkgInfo(PackageInfo origPkgInfo, String packageName, long flags) {
        return HookClient.hookPkgInfo(origPkgInfo, packageName, flags);
    }

    boolean canBeDisabled(String packageName, int newState) {
        if (newState == 0 || newState == 1) {
            return true;
        }
        try {
            if (Settings.Global.getInt(this.mContext.getContentResolver(), "maintenance_mode_user_id") == 110) {
                return true;
            }
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        return true ^ ArrayUtils.contains(MIUI_CORE_APPS, packageName);
    }

    boolean isCallerAllowedToSilentlyUninstall(int callingUid) {
        String[] packagesForUid;
        AndroidPackage pkgSetting;
        synchronized (this.mPms.mLock) {
            for (String s : this.mPms.snapshotComputer().getPackagesForUid(callingUid)) {
                if (sSilentlyUninstallPackages.contains(s) && (pkgSetting = (AndroidPackage) this.mPms.mPackages.get(s)) != null && UserHandle.getAppId(callingUid) == pkgSetting.getUid()) {
                    Slog.i(TAG, "Allowed silently uninstall from callinguid:" + callingUid);
                    return true;
                }
            }
            return false;
        }
    }

    boolean isMiuiStubPackage(String packageName) {
        PackageInfo packageInfo;
        Bundle meta;
        PackageSetting pkgSetting = null;
        synchronized (this.mPms.mLock) {
            AndroidPackage pkg = (AndroidPackage) this.mPms.mPackages.get(packageName);
            if (pkg != null) {
                pkgSetting = this.mPms.mSettings.getPackageLPr(packageName);
            }
        }
        return (pkgSetting == null || !pkgSetting.getPkgState().isUpdatedSystemApp() || (packageInfo = this.mPms.snapshotComputer().getPackageInfo(packageName, 2097280L, 0)) == null || packageInfo.applicationInfo == null || (meta = packageInfo.applicationInfo.metaData) == null || !meta.getBoolean("com.miui.stub.install", false)) ? false : true;
    }

    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:69:0x0194 -> B:70:0x0195). Please submit an issue!!! */
    boolean protectAppFromDeleting(final String packageName, final IPackageDeleteObserver2 observer, final int callingUid, final int userId, int deleteFlags) {
        Throwable th;
        int appId;
        boolean isReject;
        synchronized (this.mPms.mLock) {
            try {
                AndroidPackage pkg = (AndroidPackage) this.mPms.mPackages.get(packageName);
                try {
                    PackageSetting ps = this.mPms.mSettings.getPackageLPr(packageName);
                    if (pkg != null && ps != null && ps.getInstalled(userId)) {
                        int callingPid = Binder.getCallingPid();
                        String msg = "Uninstall pkg: " + packageName + " u" + userId + " flags:" + deleteFlags + " from u" + callingUid + "/" + callingPid + " of " + ProcessUtils.getPackageNameByPid(callingPid);
                        PackageManagerService.reportSettingsProblem(3, msg);
                    }
                    if (isMiuiStubPackage(packageName) && !MANAGED_PROVISION.equals(this.mPms.snapshotComputer().getNameForUid(callingUid))) {
                        boolean deleteSystem = (deleteFlags & 4) != 0;
                        int removeUser = (deleteFlags & 2) != 0 ? -1 : userId;
                        boolean fullRemove = removeUser == -1 || removeUser == 0;
                        if (!deleteSystem || fullRemove) {
                            final boolean z = deleteSystem;
                            this.mPms.mHandler.post(new Runnable() { // from class: com.android.server.pm.PackageManagerServiceImpl$$ExternalSyntheticLambda3
                                @Override // java.lang.Runnable
                                public final void run() {
                                    PackageManagerServiceImpl.lambda$protectAppFromDeleting$0(packageName, callingUid, userId, z, observer);
                                }
                            });
                            return true;
                        }
                    }
                    if (EnterpriseSettings.ENTERPRISE_ACTIVATED && ApplicationHelper.protectedFromDelete(this.mContext, packageName, UserHandle.getUserId(callingUid))) {
                        Slog.d(TAG, "Can't uninstall pkg : " + packageName + " callingUid : " + callingUid + ", reason Enterprise");
                        this.mPms.mHandler.post(new Runnable() { // from class: com.android.server.pm.PackageManagerServiceImpl$$ExternalSyntheticLambda4
                            @Override // java.lang.Runnable
                            public final void run() {
                                observer.onPackageDeleted(packageName, -1000, (String) null);
                            }
                        });
                        return true;
                    }
                    if (pkg != null && !pkg.isSystem() && PreloadedAppPolicy.isProtectedDataApp(this.mContext, packageName, 0) && (appId = UserHandle.getAppId(callingUid)) != 0 && appId != 1000) {
                        Iterator it = PreloadedAppPolicy.getAllowDeleteSourceApps().iterator();
                        while (true) {
                            if (!it.hasNext()) {
                                isReject = true;
                                break;
                            }
                            String allowPkg = (String) it.next();
                            if (appId == this.mPms.snapshotComputer().getPackageUid(allowPkg, 0L, 0)) {
                                isReject = false;
                                break;
                            }
                        }
                        if (isReject) {
                            try {
                                Slog.d(TAG, "MIUILOG- can't uninstall pkg : " + packageName + " callingUid : " + callingUid);
                                if (observer != null && (observer instanceof IPackageDeleteObserver2)) {
                                    observer.onPackageDeleted(packageName, -1000, (String) null);
                                    return true;
                                }
                                return true;
                            } catch (RemoteException e) {
                                return true;
                            }
                        }
                    }
                    return false;
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
            }
        }
    }

    public static /* synthetic */ void lambda$protectAppFromDeleting$0(String packageName, int callingUid, int userId, boolean deleteSystem, IPackageDeleteObserver2 observer) {
        Slog.d(TAG, "Can't uninstall pkg: " + packageName + " from uid: " + callingUid + " with user: " + userId + " deleteSystem: " + deleteSystem + " reason: shell preinstall");
        try {
            observer.onPackageDeleted(packageName, -1000, (String) null);
        } catch (RemoteException e) {
        }
    }

    /* loaded from: classes.dex */
    static class PackageDeleteConfirmObserver extends IPackageDeleteConfirmObserver.Stub {
        boolean delete;
        boolean finished;

        PackageDeleteConfirmObserver() {
        }

        public void onConfirm(boolean delete) {
            synchronized (this) {
                this.finished = true;
                this.delete = delete;
                notifyAll();
            }
        }
    }

    public int preCheckUidPermission(String permName, int uid) {
        if (UserHandle.getAppId(uid) != 2000 || !sShellCheckPermissions.contains(permName) || SystemProperties.getBoolean("persist.security.adbinput", false)) {
            return 0;
        }
        if ("android.permission.CALL_PHONE".equals(permName) && 110 == ((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class)).getCurrentUserId()) {
            Slog.d(TAG, "preCheckUidPermission: permission granted call phone");
            return 0;
        }
        Slog.d(TAG, "preCheckUidPermission: permission\u3000denied, perm=" + permName);
        return -1;
    }

    protected static void initAllowPackageList(Context context) {
        ArrayList<String> arrayList = sAllowPackage;
        arrayList.clear();
        try {
            String[] stringArray = context.getResources().getStringArray(285409284);
            arrayList.addAll(Arrays.asList(stringArray));
            Slog.i(TAG, "add " + stringArray.length + " common packages into sAllowPackage list");
            for (String pkg : stringArray) {
                Slog.i(TAG, pkg);
            }
            if (!Build.IS_INTERNATIONAL_BUILD) {
                return;
            }
            String[] stringArray2 = context.getResources().getStringArray(285409285);
            sAllowPackage.addAll(Arrays.asList(stringArray2));
            Slog.i(TAG, "add " + stringArray2.length + " international package into sAllowPackage list");
            for (String pkg2 : stringArray2) {
                Slog.i(TAG, pkg2);
            }
            String[] stringArray3 = context.getResources().getStringArray(285409286);
            sAllowPackage.addAll(Arrays.asList(stringArray3));
            Slog.i(TAG, "add " + stringArray3.length + " operator packages into sAllowPackage list");
            for (String pkg3 : stringArray3) {
                Slog.i(TAG, pkg3);
            }
        } catch (Resources.NotFoundException e) {
            e.printStackTrace();
        }
    }

    private static void fatalIf(boolean condition, int err, String msg, String logPrefix) throws PackageManagerException {
        if (condition) {
            Slog.e(TAG, logPrefix + ", msg=" + msg);
            throw new PackageManagerException(err, msg);
        }
    }

    public static synchronized boolean isReleaseRom() {
        boolean contains;
        synchronized (PackageManagerServiceImpl.class) {
            contains = Build.TAGS.contains("release-key");
        }
        return contains;
    }

    void assertValidApkAndInstaller(String packageName, SigningDetails signingDetails, int callingUid, String callingPackage, boolean isManuallyAccepted, int sessionId) throws PackageManagerException {
        if (AppOpsUtils.isXOptMode()) {
            return;
        }
        String log = "MIUILOG- assertCallerAndPackage: uid=" + callingUid + ", installerPkg=" + callingPackage;
        int callingAppId = UserHandle.getAppId(callingUid);
        switch (callingAppId) {
            case 0:
                return;
            case 2000:
                verifyInstallFromShell(this.mContext, sessionId, log);
                return;
            default:
                if (sNoVerifyAllowPackage.contains(callingPackage) || !verifyPackageForRelease(this.mContext, packageName, signingDetails, callingUid, callingPackage, log)) {
                    return;
                }
                notifyGlobalPackageInstaller(this.mContext, callingPackage);
                ArrayList<String> arrayList = sAllowPackage;
                if (arrayList.isEmpty()) {
                    initAllowPackageList(this.mContext);
                }
                if (isManuallyAccepted || arrayList.contains(callingPackage)) {
                    return;
                }
                fatalIf(true, -115, "Permission denied", log);
                return;
        }
    }

    private static boolean verifyPackageForRelease(Context context, String packageName, SigningDetails signingDetails, int callingUid, String callingPackage, String log) throws PackageManagerException {
        fatalIf(!isTrustedEnterpriseInstaller(context, callingUid, callingPackage), -22, "FAILED_VERIFICATION_FAILURE ENTERPRISE", log);
        if (isReleaseRom()) {
            fatalIf(PACKAGE_WEBVIEW.equals(packageName) && sInstallerSet.contains(callingPackage), -22, "FAILED_VERIFICATION_FAILURE MIUI WEBVIEW", log);
            if (!PreloadedAppPolicy.isProtectedDataApp(packageName) || context.getPackageManager().isPackageAvailable(packageName)) {
                return true;
            }
            String signSha256 = PreloadedAppPolicy.getProtectedDataAppSign(packageName);
            if (TextUtils.isEmpty(signSha256) || signingDetails.hasSha256Certificate(HexEncoding.decode(signSha256.replace(":", "").toLowerCase(), false))) {
                return true;
            }
            fatalIf(true, -22, "FAILED_VERIFICATION_FAILURE SIGNATURE FAIL", log);
        }
        return true;
    }

    private static void verifyInstallFromShell(Context context, int sessionId, String log) throws PackageManagerException {
        int result = -1;
        try {
            if (isSecondUserlocked(context)) {
                result = 2;
            } else {
                result = PmInjector.installVerify(sessionId);
            }
        } catch (Throwable e) {
            Log.e(TAG, "Error", e);
        }
        fatalIf(result != 2, -111, PmInjector.statusToString(result), log);
    }

    private static void notifyGlobalPackageInstaller(final Context context, final String callingPackage) {
        if (Build.IS_INTERNATIONAL_BUILD) {
            if (GOOGLE_MARKET_PACKAGE.equals(callingPackage) || "com.google.android.packageinstaller".equals(callingPackage)) {
                BackgroundThread.getHandler().post(new Runnable() { // from class: com.android.server.pm.PackageManagerServiceImpl$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        PackageManagerServiceImpl.lambda$notifyGlobalPackageInstaller$2(callingPackage, context);
                    }
                });
            }
        }
    }

    public static /* synthetic */ void lambda$notifyGlobalPackageInstaller$2(String callingPackage, Context context) {
        Intent intent = new Intent("com.miui.global.packageinstaller.action.verifypackage");
        intent.putExtra("installing", callingPackage);
        context.sendBroadcast(intent, "com.miui.securitycenter.permission.GLOBAL_PACKAGEINSTALLER");
    }

    private static boolean isSecondUserlocked(Context context) {
        boolean iscts = isCTS();
        int userid = PmInjector.getDefaultUserId();
        UserManager userManager = (UserManager) context.getSystemService("user");
        if (iscts && userid != 0 && !userManager.isUserUnlocked(userid)) {
            return true;
        }
        return false;
    }

    static boolean isMiuiSystemApp(String packageName) {
        return MIUI_SYSTEM_PACKAGES.contains(packageName);
    }

    ResolveInfo getSystemResolveInfo(List<ResolveInfo> riList) {
        ResolveInfo ret = null;
        int match = riList.get(0).match;
        for (ResolveInfo ri : riList) {
            if (ri.priority < riList.get(0).priority) {
                break;
            }
            if (ri.match > match) {
                match = ri.match;
                ret = null;
            } else if (ri.match >= match && ret == null) {
            }
            if (ri.system && isMiuiSystemApp(ri.activityInfo.packageName)) {
                ret = ri;
            }
        }
        return ret;
    }

    ResolveInfo getMarketResolveInfo(List<ResolveInfo> riList) {
        for (ResolveInfo ri : riList) {
            if (MIUI_MARKET_PACKAGE.equals(ri.activityInfo.packageName) && ri.system) {
                return ri;
            }
        }
        return null;
    }

    ResolveInfo getGlobalMarketResolveInfo(List<ResolveInfo> riList) {
        for (ResolveInfo ri : riList) {
            if (GOOGLE_MARKET_PACKAGE.equals(ri.activityInfo.packageName) && ri.system) {
                return ri;
            }
        }
        return null;
    }

    ResolveInfo getGoogleWebSearchResolveInfo(List<ResolveInfo> riList) {
        if (riList == null || riList.size() == 0) {
            return null;
        }
        for (ResolveInfo ri : riList) {
            if (ri != null && ri.activityInfo != null && GOOGLE_WEB_SEARCH_PACKAGE.equals(ri.activityInfo.packageName)) {
                return ri;
            }
        }
        return null;
    }

    ResolveInfo hookChooseBestActivity(Intent intent, String resolvedType, long flags, List<ResolveInfo> query, int userId, ResolveInfo defaultValue) {
        ResolveInfo hookedRi = hookChooseBestActivity(intent, resolvedType, flags, query, userId);
        return hookedRi == this.mPms.mResolveInfo ? defaultValue : hookedRi;
    }

    private ResolveInfo hookChooseBestActivity(Intent intent, String resolvedType, long flags, List<ResolveInfo> query, int userId) {
        ResolveInfo ri;
        String host;
        ResolveInfo ri2;
        String realPkgName;
        if (intent != null && !Build.IS_INTERNATIONAL_BUILD) {
            if ("mimarket".equals(intent.getScheme()) || ("market".equals(intent.getScheme()) && "android.intent.action.VIEW".equals(intent.getAction()))) {
                Uri uri = intent.getData();
                if (uri != null && (host = uri.getHost()) != null && ((host.equals("details") || host.equals("search")) && (ri2 = getMarketResolveInfo(query)) != null)) {
                    return ri2;
                }
            } else if (PACKAGE_MIME_TYPE.equals(intent.getType()) && "android.intent.action.VIEW".equals(intent.getAction())) {
                if (isCTS()) {
                    String realPkgName2 = this.mCurrentPackageInstaller;
                    synchronized (this.mPms.mLock) {
                        if (this.mPkgSettings.getRenamedPackageLPr(this.mCurrentPackageInstaller) != null) {
                            realPkgName2 = this.mPkgSettings.getRenamedPackageLPr(this.mCurrentPackageInstaller);
                        }
                    }
                    realPkgName = realPkgName2;
                } else {
                    realPkgName = MIUI_INSTALLER_PACKAGE;
                }
                intent.setPackage(realPkgName);
                return ((PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class)).resolveIntent(intent, resolvedType, flags, 0L, userId, false, Binder.getCallingUid());
            }
        } else if (intent != null && Build.IS_INTERNATIONAL_BUILD && "android.intent.action.WEB_SEARCH".equals(intent.getAction()) && isRsa4() && (ri = getGoogleWebSearchResolveInfo(query)) != null) {
            return ri;
        }
        return this.mPms.mResolveInfo;
    }

    private boolean isRsa4() {
        if (TextUtils.isEmpty(this.mRsaFeature)) {
            this.mRsaFeature = SystemProperties.get("ro.com.miui.rsa.feature", "");
        }
        return !TextUtils.isEmpty(this.mRsaFeature);
    }

    /* JADX WARN: Code restructure failed: missing block: B:17:0x004d, code lost:
        if (com.android.server.pm.PackageManagerServiceImpl.sGlobalWhitePackageList.contains(null) == false) goto L18;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private static boolean isOpenByGooglePlayStore(android.content.Intent r6) {
        /*
            java.lang.String r0 = r6.getScheme()
            java.lang.String r1 = "market"
            boolean r0 = r1.equals(r0)
            r1 = 0
            if (r0 == 0) goto L88
            java.lang.String r0 = r6.getAction()
            java.lang.String r2 = "android.intent.action.VIEW"
            boolean r0 = r2.equals(r0)
            if (r0 != 0) goto L1a
            goto L88
        L1a:
            android.net.Uri r0 = r6.getData()
            if (r0 == 0) goto L87
            android.net.Uri r0 = r6.getData()
            java.lang.String r0 = r0.getHost()
            if (r0 != 0) goto L2b
            goto L87
        L2b:
            r0 = 0
            boolean r2 = android.text.TextUtils.isEmpty(r0)
            if (r2 == 0) goto L33
            return r1
        L33:
            r1 = 1
            java.util.concurrent.locks.ReentrantReadWriteLock r2 = com.android.server.pm.PackageManagerServiceImpl.sGlobalRWLock     // Catch: java.lang.Throwable -> L59 java.lang.Exception -> L5b
            java.util.concurrent.locks.ReentrantReadWriteLock$ReadLock r3 = r2.readLock()     // Catch: java.lang.Throwable -> L59 java.lang.Exception -> L5b
            r3.lock()     // Catch: java.lang.Throwable -> L59 java.lang.Exception -> L5b
            java.lang.String r3 = "1"
            java.lang.String r4 = com.android.server.pm.PackageManagerServiceImpl.sGlobalSyncKey     // Catch: java.lang.Throwable -> L59 java.lang.Exception -> L5b
            boolean r3 = r3.equals(r4)     // Catch: java.lang.Throwable -> L59 java.lang.Exception -> L5b
            if (r3 == 0) goto L4f
            java.util.ArrayList<java.lang.String> r3 = com.android.server.pm.PackageManagerServiceImpl.sGlobalWhitePackageList     // Catch: java.lang.Throwable -> L59 java.lang.Exception -> L5b
            boolean r3 = r3.contains(r0)     // Catch: java.lang.Throwable -> L59 java.lang.Exception -> L5b
            if (r3 != 0) goto L50
        L4f:
            r1 = 0
        L50:
        L51:
            java.util.concurrent.locks.ReentrantReadWriteLock$ReadLock r2 = r2.readLock()
            r2.unlock()
            goto L7c
        L59:
            r2 = move-exception
            goto L7d
        L5b:
            r2 = move-exception
            java.lang.String r3 = "PKMSImpl"
            java.lang.StringBuilder r4 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L59
            r4.<init>()     // Catch: java.lang.Throwable -> L59
            java.lang.String r5 = "error:"
            java.lang.StringBuilder r4 = r4.append(r5)     // Catch: java.lang.Throwable -> L59
            java.lang.String r5 = r2.toString()     // Catch: java.lang.Throwable -> L59
            java.lang.StringBuilder r4 = r4.append(r5)     // Catch: java.lang.Throwable -> L59
            java.lang.String r4 = r4.toString()     // Catch: java.lang.Throwable -> L59
            android.util.Slog.e(r3, r4)     // Catch: java.lang.Throwable -> L59
            java.util.concurrent.locks.ReentrantReadWriteLock r2 = com.android.server.pm.PackageManagerServiceImpl.sGlobalRWLock
            goto L51
        L7c:
            return r1
        L7d:
            java.util.concurrent.locks.ReentrantReadWriteLock r3 = com.android.server.pm.PackageManagerServiceImpl.sGlobalRWLock
            java.util.concurrent.locks.ReentrantReadWriteLock$ReadLock r3 = r3.readLock()
            r3.unlock()
            throw r2
        L87:
            return r1
        L88:
            return r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.pm.PackageManagerServiceImpl.isOpenByGooglePlayStore(android.content.Intent):boolean");
    }

    private static void decodeFromServerFormat(String cloudDataString) {
        if (TextUtils.isEmpty(cloudDataString)) {
            sGlobalWhitePackageList.clear();
            return;
        }
        try {
            String[] packages = cloudDataString.split("#");
            sGlobalWhitePackageList.clear();
            for (String app : packages) {
                sGlobalWhitePackageList.add(app);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String encodeToServerFormat() {
        String defaultCloudDataString = "";
        ArrayList<String> arrayList = sGlobalWhitePackageList;
        if (arrayList == null || arrayList.isEmpty()) {
            return defaultCloudDataString;
        }
        int i = 0;
        while (true) {
            ArrayList<String> arrayList2 = sGlobalWhitePackageList;
            if (i < arrayList2.size() - 1) {
                defaultCloudDataString = defaultCloudDataString + arrayList2.get(i) + "#";
                i++;
            } else {
                return defaultCloudDataString + arrayList2.get(arrayList2.size() - 1);
            }
        }
    }

    private static void registerDataObserver(final Context context) {
        context.getContentResolver().registerContentObserver(MiuiSettings.SettingsCloudData.getCloudDataNotifyUri(), true, new ContentObserver(BackgroundThread.getHandler()) { // from class: com.android.server.pm.PackageManagerServiceImpl.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                PackageManagerServiceImpl.updatePackageWhiteList(context);
            }
        });
    }

    public static void updatePackageWhiteList(Context context) {
        ReentrantReadWriteLock.WriteLock writeLock;
        try {
            try {
                ReentrantReadWriteLock reentrantReadWriteLock = sGlobalRWLock;
                reentrantReadWriteLock.writeLock().lock();
                String data = MiuiSettings.SettingsCloudData.getCloudDataString(context.getContentResolver(), "IntentInterceptHelper", "global_intenttogp_packagelist", encodeToServerFormat());
                decodeFromServerFormat(data);
                sGlobalSyncKey = MiuiSettings.SettingsCloudData.getCloudDataString(context.getContentResolver(), "IntentInterceptHelper", "global_intenttogp_switch", "1");
                writeLock = reentrantReadWriteLock.writeLock();
            } catch (Exception e) {
                Log.e(TAG, "error:" + e.toString());
                writeLock = sGlobalRWLock.writeLock();
            }
            writeLock.unlock();
        } catch (Throwable th) {
            sGlobalRWLock.writeLock().unlock();
            throw th;
        }
    }

    public boolean updateDefaultPkgInstallerLocked() {
        if (!Build.IS_INTERNATIONAL_BUILD) {
            Log.i(TAG, "updateDefaultPkgInstallerLocked");
            PackageSetting miuiInstaller = (PackageSetting) this.mPkgSettings.mPackages.get(MIUI_INSTALLER_PACKAGE);
            if (miuiInstaller != null) {
                Log.i(TAG, "found miui installer");
            }
            PackageSetting googleInstaller = (PackageSetting) this.mPkgSettings.mPackages.get("com.google.android.packageinstaller");
            if (googleInstaller != null) {
                Log.i(TAG, "found google installer");
            }
            PackageSetting androidInstaller = (PackageSetting) this.mPkgSettings.mPackages.get(ANDROID_INSTALLER_PACKAGE);
            if (androidInstaller != null) {
                Log.i(TAG, "found android installer");
            }
            boolean isUseGooglePackageInstaller = isCTS() || (miuiInstaller == null && this.mMiuiInstallerPackageSetting == null);
            if (isUseGooglePackageInstaller) {
                if (miuiInstaller != null) {
                    miuiInstaller.setInstalled(false, 0);
                }
                this.mMiuiInstallerPackageSetting = (PackageSetting) this.mPkgSettings.mPackages.get(MIUI_INSTALLER_PACKAGE);
                this.mMiuiInstallerPackage = (AndroidPackage) this.mPms.mPackages.get(MIUI_INSTALLER_PACKAGE);
                this.mPkgSettings.mPackages.remove(MIUI_INSTALLER_PACKAGE);
                if (this.mPkgSettings.isDisabledSystemPackageLPr(MIUI_INSTALLER_PACKAGE)) {
                    this.mPkgSettings.removeDisabledSystemPackageLPw(MIUI_INSTALLER_PACKAGE);
                }
                this.mPms.mPackages.remove(MIUI_INSTALLER_PACKAGE);
            } else {
                PackageSetting packageSetting = this.mMiuiInstallerPackageSetting;
                if (packageSetting != null) {
                    packageSetting.setInstalled(true, 0);
                    this.mPkgSettings.mPackages.put(MIUI_INSTALLER_PACKAGE, this.mMiuiInstallerPackageSetting);
                    if ((this.mMiuiInstallerPackageSetting.getFlags() & 128) != 0) {
                        this.mPkgSettings.disableSystemPackageLPw(MIUI_INSTALLER_PACKAGE, true);
                    }
                }
                if (this.mMiuiInstallerPackage != null) {
                    this.mPms.mPackages.put(MIUI_INSTALLER_PACKAGE, this.mMiuiInstallerPackage);
                }
                this.mCurrentPackageInstaller = MIUI_INSTALLER_PACKAGE;
            }
            if (isUseGooglePackageInstaller) {
                if (googleInstaller != null) {
                    googleInstaller.setInstalled(true, 0);
                    this.mCurrentPackageInstaller = "com.google.android.packageinstaller";
                }
                if (androidInstaller != null) {
                    androidInstaller.setInstalled(true, 0);
                    this.mCurrentPackageInstaller = ANDROID_INSTALLER_PACKAGE;
                }
            } else {
                if (googleInstaller != null) {
                    googleInstaller.setInstalled(false, 0);
                }
                if (androidInstaller != null) {
                    androidInstaller.setInstalled(false, 0);
                }
            }
            Log.i(TAG, "set default package install as" + this.mCurrentPackageInstaller);
            return true;
        }
        return false;
    }

    boolean isAllowedToGetInstalledApps(int callingUid, String where) {
        if (Build.IS_INTERNATIONAL_BUILD) {
            return true;
        }
        int callingAppId = UserHandle.getAppId(callingUid);
        if (callingAppId < 10000) {
            return true;
        }
        String callingPackage = ProcessUtils.getPackageNameByPid(Binder.getCallingPid());
        if (TextUtils.isEmpty(callingPackage)) {
            return true;
        }
        AppOpsManager appOpsManager = (AppOpsManager) ActivityThread.currentApplication().getSystemService(AppOpsManager.class);
        if (appOpsManager.noteOpNoThrow(10022, callingUid, callingPackage, (String) null, (String) null) == 0) {
            return true;
        }
        Slog.e(TAG, "MIUILOG- Permission Denied " + where + ". pkg : " + callingPackage + " uid : " + callingUid);
        return false;
    }

    void filterOutThirdPartyApps(List<ApplicationInfo> infos, int callingUid) {
        if (infos == null || infos.size() == 0) {
            return;
        }
        int uid = UserHandle.getAppId(callingUid);
        for (int i = infos.size() - 1; i >= 0; i--) {
            ApplicationInfo info = infos.get(i);
            if (info.uid != uid && !info.isSystemApp()) {
                infos.remove(i);
            }
        }
    }

    void filterOutThirdPartyPkgs(List<PackageInfo> infos, int callingUid) {
        if (infos == null || infos.size() == 0) {
            return;
        }
        int uid = UserHandle.getAppId(callingUid);
        for (int i = infos.size() - 1; i >= 0; i--) {
            ApplicationInfo info = infos.get(i).applicationInfo;
            if (info.uid != uid && !info.isSystemApp()) {
                infos.remove(i);
            }
        }
    }

    private static boolean isNonProfileFilterNeeded(PackageManagerService pms, String packageName) {
        return sNonProfileCompiledPkgs.contains(packageName) || getTop10ThirdPartyPkgs(pms).contains(packageName);
    }

    private static List<UsageStats> getRecentlyWeekUsageStats(Context context) {
        UsageStatsManager usm = (UsageStatsManager) context.getSystemService("usagestats");
        if (usm != null) {
            long tillTime = System.currentTimeMillis();
            Map<String, UsageStats> statsMap = usm.queryAndAggregateUsageStats(tillTime - 604800000, tillTime);
            if (statsMap != null && !statsMap.isEmpty()) {
                List<UsageStats> entryList = new ArrayList<>();
                for (Map.Entry<String, UsageStats> entry : statsMap.entrySet()) {
                    entryList.add(entry.getValue());
                }
                Collections.sort(entryList, new Comparator<UsageStats>() { // from class: com.android.server.pm.PackageManagerServiceImpl.2
                    public int compare(UsageStats left, UsageStats right) {
                        return Long.signum(right.getTotalTimeInForeground() - left.getTotalTimeInForeground());
                    }
                });
                return entryList;
            }
        }
        return Collections.emptyList();
    }

    private static Set<String> getTop10ThirdPartyPkgs(PackageManagerService pms) {
        if (sTop10ThirdPartyPks == null) {
            sTop10ThirdPartyPks = new ArraySet();
            List<UsageStats> usageStats = getRecentlyWeekUsageStats(pms.mContext);
            for (UsageStats usage : usageStats) {
                String packageName = usage.getPackageName();
                synchronized (pms.mLock) {
                    AndroidPackage pkg = (AndroidPackage) pms.mPackages.get(packageName);
                    if (pkg != null && !pkg.isSystem()) {
                        sTop10ThirdPartyPks.add(packageName);
                    }
                }
            }
        }
        return sTop10ThirdPartyPks;
    }

    public boolean needClearDefaultBrowserSettings(String currentDefaultPkg) {
        return isCTS() || Build.IS_INTERNATIONAL_BUILD;
    }

    public String getMiuiDefaultBrowserPkg() {
        if (!Build.IS_INTERNATIONAL_BUILD && !isCTS()) {
            return MIUI_BROWSER_PACKAGE;
        }
        return null;
    }

    public void switchPackageInstaller() {
        String ctsInstallerPackageName;
        try {
            if (isCTS()) {
                PackageSetting googleInstaller = (PackageSetting) this.mPkgSettings.mPackages.get("com.google.android.packageinstaller");
                PackageSetting androidInstaller = (PackageSetting) this.mPkgSettings.mPackages.get(ANDROID_INSTALLER_PACKAGE);
                if (androidInstaller != null) {
                    ctsInstallerPackageName = ANDROID_INSTALLER_PACKAGE;
                } else if (googleInstaller == null) {
                    ctsInstallerPackageName = null;
                } else {
                    ctsInstallerPackageName = "com.google.android.packageinstaller";
                }
                if (ctsInstallerPackageName != null) {
                    this.mPM.installExistingPackageAsUser(ctsInstallerPackageName, 0, 16384, 4, (List) null);
                }
            }
            synchronized (this.mPms.mLock) {
                if (updateDefaultPkgInstallerLocked()) {
                    this.mPms.mRequiredInstallerPackage = this.mCurrentPackageInstaller;
                    this.mPms.mRequiredUninstallerPackage = this.mCurrentPackageInstaller;
                    AndroidPackage pkg = (AndroidPackage) this.mPms.mPackages.get(this.mPms.mRequiredInstallerPackage);
                    PermissionManagerService permService = ServiceManager.getService("permissionmgr");
                    ReflectionUtils.callMethod(permService, "updatePackageInstallerPermissions", Void.class, new Object[]{this.mCurrentPackageInstaller, pkg});
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

    public void checkGTSSpecAppOptMode() {
        String[] pkgs;
        if (Build.IS_INTERNATIONAL_BUILD) {
            pkgs = new String[]{"com.miui.screenrecorder"};
        } else {
            pkgs = new String[]{"com.miui.cleanmaster", "com.xiaomi.drivemode", "com.xiaomi.aiasst.service", "com.miui.thirdappassistant", "com.miui.screenrecorder"};
        }
        synchronized (this.mPms.mLock) {
            Settings mPkgSettings = this.mPms.mSettings;
            boolean isCtsBuild = isCTS();
            for (String pkg : pkgs) {
                PackageSetting uninstallPkg = (PackageSetting) mPkgSettings.mPackages.get(pkg);
                if ("com.miui.cleanmaster".equals(pkg) && uninstallPkg == null) {
                    checkAndClearResiduePermissions(mPkgSettings.mPermissions, pkg, "com.miui.cleanmaster.permission.Clean_Master");
                }
                if (isCtsBuild && uninstallPkg != null && !uninstallPkg.isSystem()) {
                    uninstallPkg.setInstalled(false, 0);
                    mPkgSettings.mPackages.remove(pkg);
                    if (mPkgSettings.isDisabledSystemPackageLPr(pkg)) {
                        mPkgSettings.removeDisabledSystemPackageLPw(pkg);
                    }
                    this.mPms.mPackages.remove(pkg);
                    if ("com.miui.cleanmaster".equals(pkg) || "com.miui.thirdappassistant".equals(pkg)) {
                        clearPermissions(pkg);
                    }
                }
            }
        }
    }

    private static void clearPermissions(String packageName) {
        if (Build.VERSION.SDK_INT < 30) {
            return;
        }
        PermissionManagerService permService = ServiceManager.getService("permissionmgr");
        try {
            ReflectionUtils.callMethod(permService, "updatePackageInstallerPermissions", Void.class, new Object[]{packageName, null});
            Slog.i(TAG, "clear residue permission finish");
        } catch (Exception e) {
            Slog.e(TAG, "clear residue permission error" + e.getLocalizedMessage());
        }
    }

    private static void checkAndClearResiduePermissions(LegacyPermissionSettings settings, String packageName, String perName) {
        if (Build.VERSION.SDK_INT < 30 || settings == null || TextUtils.isEmpty(packageName) || TextUtils.isEmpty(perName)) {
            return;
        }
        List<LegacyPermission> permissions = settings.getPermissions();
        if (permissions.size() > 0) {
            Slog.i(TAG, "find residue permission");
            clearPermissions(packageName);
        }
    }

    public static boolean isCTS() {
        return AppOpsUtils.isXOptMode();
    }

    private static Handler getFirstUseHandler() {
        if (sFirstUseHandler == null) {
            synchronized (sFirstUseLock) {
                if (sFirstUseHandler == null) {
                    HandlerThread handlerThread = new HandlerThread("first_use_thread");
                    sFirstUseThread = handlerThread;
                    handlerThread.start();
                    sFirstUseHandler = new Handler(sFirstUseThread.getLooper());
                }
            }
        }
        return sFirstUseHandler;
    }

    public boolean isFirstUseCompileNeeded(String packageName) {
        boolean z = false;
        if (packageName.contains("xiaomi") || packageName.contains("miui") || packageName.contains("GSMA")) {
            return false;
        }
        synchronized (this.mPms.mLock) {
            AndroidPackage pkg = (AndroidPackage) this.mPms.mPackages.get(packageName);
            if (pkg != null && !pkg.isSystem()) {
                z = true;
            }
        }
        return z;
    }

    public void processFirstUseActivity(final String packageName) {
        if (!isFirstUseCompileNeeded(packageName)) {
            return;
        }
        getFirstUseHandler().post(new Runnable() { // from class: com.android.server.pm.PackageManagerServiceImpl$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                PackageManagerServiceImpl.this.m1314x75716aa1(packageName);
            }
        });
    }

    /* renamed from: handleFirstUseActivity */
    public void m1314x75716aa1(String packageName) {
    }

    /* renamed from: handleAppLoadSecondaryDex */
    public void m1313x6fc1bf62(PackageDexOptimizer pdo, ApplicationInfo loaderAppinfo, PackageDexUsage.DexUseInfo dexUseInfo, String dexPath) {
        handleAppLoadSecondaryDexReason(pdo, loaderAppinfo, dexUseInfo, dexPath, 9);
    }

    public void handleAppLoadSecondaryDexReason(PackageDexOptimizer pdo, ApplicationInfo loaderAppinfo, PackageDexUsage.DexUseInfo dexUseInfo, String dexPath, int reason) {
        if (loaderAppinfo.packageName != null) {
            DexoptOptions options = new DexoptOptions(loaderAppinfo.packageName, reason, "speed-profile", (String) null, 12);
            performDexOptSecondary(loaderAppinfo, dexPath, dexUseInfo, options);
            int result = getDexoptSecondaryResult();
            Slog.d(TAG, "Run dexopt on: " + dexPath + ", because top-app{" + loaderAppinfo.packageName + "} loading it. Dexopt result: " + dexoptResultToString(result));
            return;
        }
        Slog.d(TAG, "Skip dexopt on: " + dexPath + ", because " + loaderAppinfo.packageName + " is not current resumed.");
    }

    static String dexoptResultToString(int result) {
        switch (result) {
            case -1:
                return "DEX_OPT_FAILED";
            case 0:
                return "DEX_OPT_SKIPPED";
            case 1:
                return "DEX_OPT_PERFORMED";
            default:
                return "DEX_OPT_FAILED";
        }
    }

    public void processAppLoadSecondaryDexReason(final PackageDexOptimizer pdo, final ApplicationInfo loaderAppinfo, final PackageDexUsage.DexUseInfo dexUseInfo, final String dexPath, final int reason) {
        if (dexUseInfo == null) {
            Slog.d(TAG, "Oops, can't get dexUseInfo with " + dexPath + ", skip dexopt on it");
            return;
        }
        Runnable task = new Runnable() { // from class: com.android.server.pm.PackageManagerServiceImpl.3
            @Override // java.lang.Runnable
            public void run() {
                PackageManagerServiceImpl.this.handleAppLoadSecondaryDexReason(pdo, loaderAppinfo, dexUseInfo, dexPath, reason);
            }
        };
        getFirstUseHandler().post(task);
    }

    public void processAppLoadSecondaryDex(final PackageDexOptimizer pdo, final ApplicationInfo loaderAppinfo, final PackageDexUsage.DexUseInfo dexUseInfo, final String dexPath) {
        if (dexUseInfo == null) {
            Slog.d(TAG, "Oops, can't get dexUseInfo with " + dexPath + ", skip dexopt on it");
        } else {
            getFirstUseHandler().post(new Runnable() { // from class: com.android.server.pm.PackageManagerServiceImpl$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    PackageManagerServiceImpl.this.m1313x6fc1bf62(pdo, loaderAppinfo, dexUseInfo, dexPath);
                }
            });
        }
    }

    public static boolean checkSignatures(PackageParser.SigningDetails p1, PackageParser.SigningDetails p2) {
        return PackageManagerServiceUtils.compareSignatures(p1.signatures, p2.signatures) == 0;
    }

    void markPmsScanDetail(PackageManagerService pms) {
        int type;
        int thirdAppCount = 0;
        int systemAppCount = 0;
        int persistAppCount = 0;
        synchronized (pms.mLock) {
            for (AndroidPackage pkg : pms.mPackages.values()) {
                if (pkg.isSystem()) {
                    systemAppCount++;
                    if (pms.mSettings.isDisabledSystemPackageLPr(pkg.getPackageName())) {
                        thirdAppCount++;
                    }
                } else {
                    thirdAppCount++;
                }
                if (pkg.isPersistent() && (!pms.getSafeMode() || pkg.isSystem())) {
                    persistAppCount++;
                }
            }
        }
        BootEventManager.getInstance().setSystemAppCount(systemAppCount);
        BootEventManager.getInstance().setThirdAppCount(thirdAppCount);
        BootEventManager.getInstance().setPersistAppCount(persistAppCount);
        if (pms.isFirstBoot()) {
            type = 2;
        } else {
            type = pms.isDeviceUpgrading() ? 3 : 1;
        }
        BootEventManager.getInstance().setBootType(type);
    }

    void markCoreAppDexopt(long startTime, long endTime) {
        BootEventManager.getInstance().setCoreAppDexopt(endTime - startTime);
    }

    void markPackageOptimized(AndroidPackage pkg) {
        BootEventManager manager = BootEventManager.getInstance();
        if (pkg.isSystem() && !AndroidPackageUtils.generateAppInfoWithoutState(pkg).isUpdatedSystemApp()) {
            manager.setDexoptSystemAppCount(manager.getDexoptSystemAppCount() + 1);
        } else {
            manager.setDexoptThirdAppCount(manager.getDexoptThirdAppCount() + 1);
        }
    }

    void setCallingPackage(PackageInstallerSession session, String callingPackageName) {
        if (!TextUtils.isEmpty(callingPackageName)) {
            session.setCallingPackage(callingPackageName);
            return;
        }
        String realPkg = ProcessUtils.getPackageNameByPid(Binder.getCallingPid());
        session.setCallingPackage(TextUtils.isEmpty(realPkg) ? session.getInstallerPackageName() : realPkg);
    }

    public void performDexOptAsyncTask(DexoptOptions options) {
        DexoptServiceThread dexoptServiceThread = this.mDexoptServiceThread;
        if (dexoptServiceThread != null) {
            dexoptServiceThread.performDexOptAsyncTask(options);
        }
    }

    public int getDexOptResult() {
        DexoptServiceThread dexoptServiceThread = this.mDexoptServiceThread;
        if (dexoptServiceThread != null) {
            return dexoptServiceThread.getDexOptResult();
        }
        return 0;
    }

    public void performDexOptSecondary(ApplicationInfo info, String path, PackageDexUsage.DexUseInfo dexUseInfo, DexoptOptions options) {
        DexoptServiceThread dexoptServiceThread = this.mDexoptServiceThread;
        if (dexoptServiceThread != null) {
            dexoptServiceThread.performDexOptSecondary(info, path, dexUseInfo, options);
        }
    }

    public int getDexoptSecondaryResult() {
        DexoptServiceThread dexoptServiceThread = this.mDexoptServiceThread;
        if (dexoptServiceThread != null) {
            return dexoptServiceThread.getDexoptSecondaryResult();
        }
        return 0;
    }

    private void disableSystemApp(int userId, String pkg) {
        PackageManagerServiceUtils.logCriticalInfo(4, "Disable " + pkg + " for user " + userId);
        try {
            this.mPM.setApplicationEnabledSetting(pkg, 3, 0, userId, "COTA");
            synchronized (this.mPms.mLock) {
                PackageSetting pkgSetting = this.mPms.mSettings.getPackageLPr(pkg);
                if (pkgSetting != null) {
                    updatedefaultState(pkgSetting, "cota-disabled", userId);
                    this.mPms.scheduleWritePackageRestrictions(userId);
                }
            }
        } catch (Exception e) {
            PackageManagerServiceUtils.logCriticalInfo(6, "Failed to disable " + pkg + ", msg=" + e.getMessage());
        }
    }

    private void enableSystemApp(int userId, String pkg) {
        PackageManagerServiceUtils.logCriticalInfo(4, "Enable " + pkg + " for user " + userId);
        try {
            this.mPM.setApplicationEnabledSetting(pkg, 1, 0, userId, "COTA");
            synchronized (this.mPms.mLock) {
                PackageSetting pkgSetting = this.mPms.mSettings.getPackageLPr(pkg);
                if (pkgSetting != null) {
                    updatedefaultState(pkgSetting, null, userId);
                    this.mPms.scheduleWritePackageRestrictions(userId);
                }
            }
        } catch (Exception e) {
            PackageManagerServiceUtils.logCriticalInfo(6, "Failed to enable " + pkg + ", msg=" + e.getMessage());
        }
    }

    private void updateSystemAppState(int userId, boolean isCTS, String pkg) {
        synchronized (this.mPms.mLock) {
            PackageSetting pkgSetting = this.mPms.mSettings.getPackageLPr(pkg);
            if (pkgSetting != null && pkgSetting.isSystem()) {
                boolean alreadyDisabled = true;
                boolean updatedSystemApp = (pkgSetting.getFlags() & 128) != 0 && pkgSetting.getInstalled(userId);
                boolean untouchedYet = getdatedefaultState(pkgSetting, userId) == null;
                int state = pkgSetting.getEnabled(userId);
                if (state != 2 && state != 3) {
                    alreadyDisabled = false;
                }
                boolean wasDisabledByUs = "COTA".equals(pkgSetting.readUserState(userId).getLastDisableAppCaller());
                if (updatedSystemApp) {
                    return;
                }
                if (isCTS) {
                    if (!alreadyDisabled || !wasDisabledByUs) {
                        return;
                    }
                    enableSystemApp(userId, pkg);
                } else if (!alreadyDisabled && untouchedYet) {
                    disableSystemApp(userId, pkg);
                }
            }
        }
    }

    private String getdatedefaultState(PackageSetting ps, int userId) {
        return ps.readUserState(userId).getDefaultState();
    }

    private void updatedefaultState(PackageSetting ps, String value, int userId) {
        ps.modifyUserState(userId).setDefaultState(value);
    }

    void updateSystemAppDefaultStateForUser(int userId) {
        boolean isCTS = isCTS();
        ArrayMap<String, Boolean> defaultPkgState = SystemConfig.getInstance().getPackageDefaultState();
        if (defaultPkgState.isEmpty()) {
            return;
        }
        int size = defaultPkgState.size();
        for (int i = 0; i < size; i++) {
            String pkg = defaultPkgState.keyAt(i);
            boolean disableByDefault = !defaultPkgState.getOrDefault(pkg, false).booleanValue();
            if (disableByDefault) {
                updateSystemAppState(userId, isCTS, pkg);
            }
        }
    }

    public void doUpdateSystemAppDefaultUserStateForAllUsers() {
        UserManagerInternal mUserManager = (UserManagerInternal) LocalServices.getService(UserManagerInternal.class);
        int[] allUsers = mUserManager.getUserIds();
        for (int userId : allUsers) {
            updateSystemAppDefaultStateForUser(userId);
        }
    }

    void updateSystemAppDefaultStateForAllUsers() {
        ArrayMap<String, Boolean> defaultPkgState = SystemConfig.getInstance().getPackageDefaultState();
        if (defaultPkgState.isEmpty()) {
            return;
        }
        doUpdateSystemAppDefaultUserStateForAllUsers();
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("miui_optimization"), false, new ContentObserver(this.mPms.mHandler) { // from class: com.android.server.pm.PackageManagerServiceImpl.4
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                PackageManagerServiceImpl.this.doUpdateSystemAppDefaultUserStateForAllUsers();
            }
        }, -1);
    }

    public static Handler getOtaSpeedProfileHandler() {
        if (sOtaSpeedProfileHandler == null) {
            synchronized (sOtaSpeedProfileLock) {
                if (sOtaSpeedProfileHandler == null) {
                    HandlerThread handlerThread = new HandlerThread("ota_speed_profile_thread");
                    sOtaSpeedProfileThread = handlerThread;
                    handlerThread.start();
                    sOtaSpeedProfileHandler = new Handler(sOtaSpeedProfileThread.getLooper());
                }
            }
        }
        return sOtaSpeedProfileHandler;
    }

    private ThreadPoolExecutor getThreadPoolExecutor() {
        ThreadPoolExecutor sCreatePoolExecutor = new ThreadPoolExecutor(2, 2, KEEP_ALIVE_DURATION, TimeUnit.SECONDS, new LinkedBlockingQueue());
        return sCreatePoolExecutor;
    }

    private int performDexOptTraced(DexoptOptions options) {
        try {
            Method declaredMethod = this.mPms.getClass().getDeclaredMethod("performDexOptTraced", DexoptOptions.class);
            declaredMethod.setAccessible(true);
            return ((Integer) declaredMethod.invoke(this.mPms, options)).intValue();
        } catch (Exception e) {
            Slog.w(TAG, "Exception: " + e);
            return -1;
        }
    }

    public int performPrimaryDexOptStaus(final AndroidPackage pkg, final CountDownLatch countDownLatch, int pkgCompilationReason, int dexoptFlags) {
        DexoptOptions options = new DexoptOptions(pkg.getPackageName(), pkgCompilationReason, dexoptFlags);
        if (isNonProfileFilterNeeded(this.mPms, pkg.getPackageName())) {
            String adjustedFilter = PackageManagerServiceCompilerMapping.getCompilerFilterForReason(9);
            final DexoptOptions dexoptions = new DexoptOptions(pkg.getPackageName(), 9, adjustedFilter, (String) null, dexoptFlags);
            if (this.sPoolExecutor == null) {
                this.sPoolExecutor = getThreadPoolExecutor();
            }
            this.sPoolExecutor.execute(new Runnable() { // from class: com.android.server.pm.PackageManagerServiceImpl.5
                @Override // java.lang.Runnable
                public void run() {
                    PackageManagerServiceImpl.this.performDexOptAsyncTask(dexoptions);
                    Slog.w(PackageManagerServiceImpl.TAG, "The speed profile is executed after ota upgrade = " + pkg.getPackageName() + " status = " + PackageManagerServiceImpl.this.mOtaDexOptStatus);
                    countDownLatch.countDown();
                }
            });
            this.mOtaDexOptStatus = 1;
        } else {
            this.mOtaDexOptStatus = performDexOptTraced(options);
            countDownLatch.countDown();
            Slog.i(TAG, "The verify is executed after ota upgrade = " + pkg.getPackageName() + " status = " + this.mOtaDexOptStatus);
        }
        return this.mOtaDexOptStatus;
    }

    void removePackageFromSharedUser(PackageSetting ps) {
        SharedUserSetting sharedUserSetting = this.mPms.mSettings.getSharedUserSettingLPr(ps);
        if (sharedUserSetting != null) {
            sharedUserSetting.removePackage(ps);
        }
    }

    public int getPackageUid(String packageName, int userId) {
        PackageManagerService packageManagerService = this.mPms;
        if (packageManagerService != null) {
            return packageManagerService.snapshotComputer().getPackageUid(packageName, 8192L, userId);
        }
        return super.getPackageUid(packageName, userId);
    }

    public PackageManagerService getService() {
        return this.mPms;
    }

    List<ApplicationInfo> getPersistentAppsForOtherUser(PackageManagerService pkms, boolean safeMode, int flags, int userId) {
        synchronized (pkms.mLock) {
            ArrayList<ApplicationInfo> finalList = new ArrayList<>();
            for (AndroidPackage p : pkms.mPackages.values()) {
                if (p.isPersistent() && (!safeMode || p.isSystem())) {
                    PackageSetting ps = (PackageSetting) pkms.mSettings.mPackages.get(p.getPackageName());
                    if (ps == null) {
                        Slog.w(TAG, "ps is null!");
                        return finalList;
                    }
                    ApplicationInfo ai = PackageInfoUtils.generateApplicationInfo(p, flags, ps.readUserState(userId), userId, ps);
                    switch (userId) {
                        case 110:
                            addPersistentPackages(ai, finalList);
                            continue;
                        default:
                            continue;
                    }
                }
            }
            return finalList;
        }
    }

    private static void addPersistentPackages(ApplicationInfo ai, ArrayList<ApplicationInfo> finalList) {
        if (ai == null) {
            Slog.w(TAG, "ai is null!");
        } else if (ai.processName == null) {
            Slog.w(TAG, "processName is null!");
        } else {
            String str = ai.processName;
            char c = 65535;
            switch (str.hashCode()) {
                case -1977039313:
                    if (str.equals("com.goodix.fingerprint")) {
                        c = 1;
                        break;
                    }
                    break;
                case -695600961:
                    if (str.equals("com.android.nfc")) {
                        c = 0;
                        break;
                    }
                    break;
            }
            switch (c) {
                case 0:
                case 1:
                    finalList.add(ai);
                    return;
                default:
                    return;
            }
        }
    }

    public boolean exemptApplink(Intent intent, List<ResolveInfo> candidates, List<ResolveInfo> result) {
        if (intent.isWebIntent() && intent.getData() != null && "digitalkeypairing.org".equals(intent.getData().getHost())) {
            int size = candidates.size();
            int i = 0;
            while (true) {
                if (i < size) {
                    ResolveInfo resolveInfo = candidates.get(i);
                    if (resolveInfo == null || resolveInfo.activityInfo == null || !"com.miui.tsmclient".equals(resolveInfo.activityInfo.packageName)) {
                        i++;
                    } else {
                        result.add(resolveInfo);
                        break;
                    }
                } else {
                    break;
                }
            }
            int i2 = result.size();
            if (i2 > 0) {
                return true;
            }
            return false;
        }
        return false;
    }

    private void preConfigDexMetaData(final Context context) {
        this.mDexMetaDataWaitTime = SystemProperties.getInt("pm.dexopt.dm.waittime", 0);
        context.getContentResolver().registerContentObserver(MiuiSettings.SettingsCloudData.getCloudDataNotifyUri(), true, new ContentObserver(BackgroundThread.getHandler()) { // from class: com.android.server.pm.PackageManagerServiceImpl.6
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                PackageManagerServiceImpl.this.updateDexMetaDataState(context);
            }
        });
    }

    public void updateDexMetaDataState(Context context) {
        String enable = MiuiSettings.SettingsCloudData.getCloudDataString(context.getContentResolver(), "DexMetaData", MiuiCustomizeShortCutUtils.ATTRIBUTE_ENABLE, "false");
        if ("true".equals(enable)) {
            Slog.d(TAG, "prepare DexMetadata enable: " + enable);
            SystemProperties.set("pm.dexopt.dm.enable", "true");
            this.mEnableDexMetaData = true;
            return;
        }
        SystemProperties.set("pm.dexopt.dm.enable", "false");
        this.mEnableDexMetaData = false;
    }

    public int performDexOptInternal(DexoptOptions options) {
        DexOptHelper dexoptHelper = new DexOptHelper(this.mPms);
        try {
            Method declaredMethod = dexoptHelper.getClass().getDeclaredMethod("performDexOptInternal", DexoptOptions.class);
            declaredMethod.setAccessible(true);
            return ((Integer) declaredMethod.invoke(dexoptHelper, options)).intValue();
        } catch (Exception e) {
            Slog.w(TAG, "Exception: " + e);
            return 0;
        }
    }

    public int prepareDexMetadata(AndroidPackage pkg) {
        boolean success;
        String apkPath = pkg.getBaseApkPath();
        String codePath = pkg.getPath();
        pkg.getPackageName();
        try {
            AssetManager assetManager = new AssetManager();
            assetManager.addAssetPath(apkPath);
            File target = new File(codePath, "base.dm");
            ProfileTranscoder profileTranscoder = new ProfileTranscoder(target, apkPath, codePath, assetManager);
            success = profileTranscoder.read().transcodeIfNeeded().write();
        } catch (Exception e) {
            Slog.d(TAG, "prepareDexMetadata exception" + e);
            success = false;
        }
        if (success) {
            Slog.d(TAG, "prepareDexMetadata success");
            return 1;
        }
        return -117;
    }

    private static Handler getDexMetaDataHandler() {
        if (sDexMetaDataHandler == null) {
            synchronized (sDexMetaDataLock) {
                if (sDexMetaDataHandler == null) {
                    HandlerThread handlerThread = new HandlerThread("dex_metadata_async_thread");
                    sDexMetaDataThread = handlerThread;
                    handlerThread.start();
                    sDexMetaDataHandler = new Handler(sDexMetaDataThread.getLooper());
                }
            }
        }
        return sDexMetaDataHandler;
    }

    public void asyncDexMetadataDexopt(final AndroidPackage pkg, final int[] userId) {
        final boolean enableDebug = this.mDexMetaDataWaitTime > 0;
        if (this.mEnableDexMetaData || enableDebug) {
            Runnable task = new Runnable() { // from class: com.android.server.pm.PackageManagerServiceImpl.7
                @Override // java.lang.Runnable
                public void run() {
                    long asyncDexMetadataBeginTime = System.currentTimeMillis();
                    int result = PackageManagerServiceImpl.this.prepareDexMetadata(pkg);
                    long prepareDexMetadataTime = System.currentTimeMillis() - asyncDexMetadataBeginTime;
                    if (enableDebug) {
                        Slog.d(PackageManagerServiceImpl.TAG, "waiting for Dexmetadata, push base.dm into " + pkg.getPath());
                        try {
                            Thread.sleep(PackageManagerServiceImpl.this.mDexMetaDataWaitTime);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Slog.d(PackageManagerServiceImpl.TAG, "waiting for Dexmetadata end");
                    }
                    if (result == 1 || enableDebug) {
                        PackageManagerServiceImpl.this.mPms.mArtManagerService.prepareAppProfiles(pkg, userId, true);
                        PackageManagerService unused = PackageManagerServiceImpl.this.mPms;
                        DexoptOptions dexoptOptions = new DexoptOptions(pkg.getPackageName(), 3, 5);
                        int result2 = PackageManagerServiceImpl.this.performDexOptInternal(dexoptOptions);
                        long totalTime = System.currentTimeMillis() - asyncDexMetadataBeginTime;
                        Slog.d(PackageManagerServiceImpl.TAG, "asyncDexMetadataDexopt result: " + result2 + " prepare DexMetadata time: " + prepareDexMetadataTime + " total time: " + totalTime);
                    }
                }
            };
            getDexMetaDataHandler().post(task);
            return;
        }
        Slog.d(TAG, "asyncDexMetadataDexopt not enable");
    }
}
