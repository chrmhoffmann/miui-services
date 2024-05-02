package com.miui.server;

import android.app.AppOpsManager;
import android.app.IApplicationThread;
import android.app.INotificationManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageHideManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManagerCompat;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ParceledListSlice;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.miui.AppOpsUtils;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.permission.PermissionManager;
import android.provider.Settings;
import android.telephony.CellIdentity;
import android.telephony.CellInfo;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.util.Xml;
import android.view.SurfaceControl;
import com.android.internal.app.ILocationBlurry;
import com.android.internal.app.IWakePathCallback;
import com.android.internal.content.PackageMonitor;
import com.android.internal.util.FastXmlSerializer;
import com.android.server.ForceDarkAppListProvider;
import com.android.server.LocalServices;
import com.android.server.MiuiNetworkManagementService;
import com.android.server.am.AutoStartManagerService;
import com.android.server.am.PendingIntentRecordImpl;
import com.android.server.am.ProcessUtils;
import com.android.server.inputmethod.InputMethodManagerServiceImpl;
import com.android.server.lights.LightsManager;
import com.android.server.lights.LogicalLight;
import com.android.server.location.MiuiBlurLocationManagerStub;
import com.android.server.pm.MiuiDefaultPermissionGrantPolicy;
import com.android.server.pm.PackageManagerServiceCompat;
import com.android.server.pm.PackageManagerServiceImpl;
import com.android.server.pm.PackageManagerServiceStub;
import com.android.server.pm.UserManagerService;
import com.android.server.pm.permission.PermissionManagerService;
import com.android.server.wm.FoldablePackagePolicy;
import com.android.server.wm.MiuiSizeCompatService;
import com.android.server.wm.ScreenRotationAnimationImpl;
import com.android.server.wm.WindowProcessUtils;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import miui.content.pm.PreloadedAppPolicy;
import miui.content.res.IconCustomizer;
import miui.content.res.ThemeNativeUtils;
import miui.content.res.ThemeRuntimeManager;
import miui.os.Build;
import miui.security.ISecurityCallback;
import miui.security.ISecurityManager;
import miui.security.SecurityManager;
import miui.security.SecurityManagerCompat;
import miui.security.SecurityManagerInternal;
import miui.security.StorageRestrictedPathManager;
import miui.security.WakePathChecker;
import miui.security.WakePathComponent;
import miui.securityspace.ConfigUtils;
import miui.securityspace.XSpaceUserHandle;
import miui.util.FeatureParser;
import org.json.JSONArray;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
/* loaded from: classes.dex */
public class SecurityManagerService extends ISecurityManager.Stub {
    private static final String APPLOCK_MASK_NOTIFY = "applock_mask_notify";
    private static final String CLASS_NAME = "classname";
    private static final String CLASS_NAMES = "classnames";
    private static final boolean DEBUG = false;
    private static final String DEF_BROWSER_COUNT = "miui.sec.defBrowser";
    public static final int INSTALL_FULL_APP = 16384;
    public static final int INSTALL_REASON_USER = 4;
    private static final String LEADCORE = "leadcore";
    public static final long LOCK_TIME_OUT = 60000;
    private static final int MSG_SHOW_DIALOG = 1;
    private static final String MTK = "mediatek";
    private static final String NAME = "name";
    private static final String PACKAGE_SECURITYCENTER = "com.miui.securitycenter";
    private static final String PKG_BROWSER;
    private static final String PLATFORM_VAID_PERMISSION = "com.miui.securitycenter.permission.SYSTEM_PERMISSION_DECLARE";
    private static final int REMVOE_AC_PACKAGE = 4;
    private static final int RTC_POWEROFF_WAKEUP_MTK = 8;
    private static final int SYS_APP_CRACKED = 1;
    private static final int SYS_APP_NOT_CRACKED = 0;
    private static final int SYS_APP_UNINIT = -1;
    static final String TAG = "SecurityManagerService";
    private static final String TIME = "time";
    private static final String UPDATE_VERSION = "1.0";
    private static final String VAID_PLATFORM_CACHE_PATH = "/data/system/vaid_persistence_platform";
    private static final String WAKEALARM_PATH_OF_LEADCORE = "/sys/comip/rtc_alarm";
    private static final String WAKEALARM_PATH_OF_QCOM = "/sys/class/rtc/rtc0/wakealarm";
    private static final int WRITE_BOOTTIME_DELAY = 1000;
    private static final int WRITE_BOOT_TIME = 3;
    private static final int WRITE_SETTINGS = 1;
    private static final int WRITE_SETTINGS_DELAY = 1000;
    private static final int WRITE_WAKE_UP_TIME = 2;
    private static AppRunningControlService mAppRunningControlService;
    private AccessController mAccessController;
    private AppOpsManager mAom;
    private IBinder mAppRunningControlBinder;
    private Context mContext;
    private boolean mFingerprintNotify;
    private Handler mHandler;
    private HandlerThread mHandlerThread;
    private INotificationManager mINotificationManager;
    private boolean mIsUpdated;
    private LogicalLight mLedLight;
    private final int mLightOn;
    private PackageMonitor mPackageMonitor;
    private String mPlatformVAID;
    private SecuritySmsHandler mSecuritySmsHandler;
    private SecurityWriteHandler mSecurityWriteHandler;
    private AtomicFile mSettingsFile;
    private SettingsObserver mSettingsObserver;
    private final Object mUserStateLock;
    private AtomicFile mWakeUpFile;
    private ISecurityCallback sGoogleBaseService;
    private final String READ_AND_WIRTE_PERMISSION_MANAGER = "miui.permission.READ_AND_WIRTE_PERMISSION_MANAGER";
    private HashMap<String, Long> mWakeUpTime = new HashMap<>();
    private boolean mDialogFlag = false;
    final SparseArray<UserState> mUserStates = new SparseArray<>(3);
    private ArrayList<String> mIncompatibleAppList = new ArrayList<>();
    private Object mRegistrantLock = new Object();
    private RegistrantList mAppsPreInstallRegistrant = new RegistrantList();
    private int mSysAppCracked = -1;
    private final List<String> mPrivacyVirtualDisplay = new ArrayList();
    private final List<String> mPrivacyDisplayNameList = new ArrayList();
    private UserManagerService mUserManager = UserManagerService.getInstance();
    private PermissionManagerService mPermissionManagerService = ServiceManager.getService("permissionmgr");
    private long mWakeTime = 0;

    private native boolean nativeIsReleased();

    private native void nativeKillPackageProcesses(int i, String str);

    static {
        PKG_BROWSER = Build.IS_INTERNATIONAL_BUILD ? "com.mi.globalbrowser" : "com.android.browser";
        System.loadLibrary("miui_security");
    }

    protected void dump(FileDescriptor fd, PrintWriter fout, String[] args) {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.DUMP") != 0) {
            fout.println("Permission Denial: can't dump SecurityManager from from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
            return;
        }
        WakePathChecker.getInstance().dump(fout);
        MiuiNetworkManagementService.getInstance().dump(fout);
        fout.println("===================================SCREEN SHARE PROTECTION DUMP BEGIN========================================");
        if (this.mPrivacyVirtualDisplay.size() == 0) {
            fout.println("Don't have any privacy virtual display package!");
        } else {
            for (String packageName : this.mPrivacyVirtualDisplay) {
                fout.println("packageName: " + packageName);
            }
        }
        if (this.mPrivacyDisplayNameList.size() == 0) {
            fout.println("Don't have any privacy virtual display name!");
        } else {
            for (String name : this.mPrivacyDisplayNameList) {
                fout.println("virtual display: " + name);
            }
        }
        fout.println("====================================SCREEN SHARE PROTECTION DUMP END========================================");
    }

    /* loaded from: classes.dex */
    public static final class UserState {
        GameBoosterServiceDeath gameBoosterServiceDeath;
        final ArraySet<String> mAccessControlCanceled;
        boolean mAccessControlEnabled;
        final ArrayMap<String, Long> mAccessControlLastCheck;
        boolean mAccessControlLockConvenient;
        int mAccessControlLockMode;
        final HashSet<String> mAccessControlPassPackages;
        boolean mAccessControlSettingInit;
        private int mAppPermissionControlStatus;
        boolean mIsGameMode;
        String mLastResumePackage;
        final HashMap<String, PackageSetting> mPackages;
        int userHandle;

        private UserState() {
            this.mAppPermissionControlStatus = 1;
            this.mAccessControlPassPackages = new HashSet<>();
            this.mPackages = new HashMap<>();
            this.mAccessControlCanceled = new ArraySet<>();
            this.mAccessControlLastCheck = new ArrayMap<>();
            this.mAccessControlLockMode = 0;
        }
    }

    /* loaded from: classes.dex */
    private class GameBoosterServiceDeath implements IBinder.DeathRecipient {
        private IBinder mGameBoosterService;
        private UserState mUserState;

        public GameBoosterServiceDeath(UserState userState, IBinder gameBoosterService) {
            SecurityManagerService.this = r1;
            this.mUserState = userState;
            this.mGameBoosterService = gameBoosterService;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            synchronized (SecurityManagerService.this.mUserStateLock) {
                try {
                    this.mGameBoosterService.unlinkToDeath(this, 0);
                    this.mUserState.mIsGameMode = false;
                    this.mUserState.gameBoosterServiceDeath = null;
                } catch (Exception e) {
                    Log.e(SecurityManagerService.TAG, "GameBoosterServiceDeath", e);
                }
            }
        }
    }

    /* loaded from: classes.dex */
    private static class AppItem {
        boolean mCheckEnable;
        String mPkg;
        String mSignature;

        public AppItem(String pkg, String signature, boolean ce) {
            this.mPkg = pkg;
            this.mSignature = signature;
            this.mCheckEnable = ce;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class MyPackageMonitor extends PackageMonitor {
        MyPackageMonitor() {
            SecurityManagerService.this = this$0;
        }

        public void onPackageAdded(String packageName, int uid) {
            WakePathChecker.getInstance().onPackageAdded(SecurityManagerService.this.mContext);
            checkDefaultBrowser(uid);
        }

        public void onPackageRemoved(String packageName, int uid) {
            checkDefaultBrowser(uid);
        }

        public void onPackagesAvailable(String[] packages) {
        }

        public void onPackagesUnavailable(String[] packages) {
        }

        public void onPackageUpdateStarted(String packageName, int uid) {
            checkDefaultBrowser(uid);
        }

        private void checkDefaultBrowser(int uid) {
            if (Build.IS_INTERNATIONAL_BUILD || AppOpsUtils.isXOptMode() || Build.VERSION.SDK_INT < 24) {
                return;
            }
            SecurityManagerService.this.mSecurityWriteHandler.postDelayed(new Runnable() { // from class: com.miui.server.SecurityManagerService.MyPackageMonitor.1
                @Override // java.lang.Runnable
                public void run() {
                    ContentResolver cr = SecurityManagerService.this.mContext.getContentResolver();
                    try {
                        SecurityManagerService.this.checkIntentFilterVerifications();
                        int defBrowserCount = Settings.Secure.getInt(cr, SecurityManagerService.DEF_BROWSER_COUNT, -1);
                        boolean allow = true;
                        if (defBrowserCount >= 10 && defBrowserCount < 100) {
                            Settings.Secure.putInt(cr, SecurityManagerService.DEF_BROWSER_COUNT, defBrowserCount + 1);
                            allow = false;
                        }
                        if (allow) {
                            SecurityManagerService.this.setDefaultBrowser();
                        }
                    } catch (Exception e) {
                        Log.e(SecurityManagerService.TAG, "checkDefaultBrowser", e);
                    }
                }
            }, 300L);
        }
    }

    /* loaded from: classes.dex */
    public class SecurityWriteHandler extends Handler {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        SecurityWriteHandler(Looper looper) {
            super(looper);
            SecurityManagerService.this = this$0;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    Process.setThreadPriority(0);
                    synchronized (SecurityManagerService.this.mSettingsFile) {
                        removeMessages(1);
                        SecurityManagerService.this.writeSettings();
                    }
                    Process.setThreadPriority(10);
                    return;
                case 2:
                    Process.setThreadPriority(0);
                    removeMessages(2);
                    SecurityManagerService.this.writeWakeUpTime();
                    Process.setThreadPriority(10);
                    return;
                case 3:
                    Process.setThreadPriority(0);
                    removeMessages(3);
                    if (FeatureParser.hasFeature("vendor", 3)) {
                        String vendor2 = FeatureParser.getString("vendor");
                        SecurityManagerCompat.writeBootTime(SecurityManagerService.this.mContext, vendor2, SecurityManagerService.this.mWakeTime);
                        Log.d(SecurityManagerService.TAG, "Wake up time updated " + SecurityManagerService.this.mWakeTime);
                    } else {
                        Log.w(SecurityManagerService.TAG, "There is no corresponding feature!");
                    }
                    Process.setThreadPriority(10);
                    return;
                case 4:
                    synchronized (SecurityManagerService.this.mUserStateLock) {
                        int userId = msg.arg1;
                        String packageName = (String) msg.obj;
                        UserState userState = SecurityManagerService.this.getUserStateLocked(userId);
                        userState.mAccessControlCanceled.remove(packageName);
                    }
                    return;
                default:
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class SettingsObserver extends ContentObserver {
        private final Uri mAccessControlLockConvenientUri;
        private final Uri mAccessControlLockEnabledUri;
        private final Uri mAccessControlLockModedUri;
        private final Uri mAccessMiuiOptimizationUri;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public SettingsObserver(Handler handler, Context context) {
            super(handler);
            SecurityManagerService.this = r7;
            Uri uriFor = Settings.Secure.getUriFor("access_control_lock_enabled");
            this.mAccessControlLockEnabledUri = uriFor;
            Uri uriFor2 = Settings.Secure.getUriFor("access_control_lock_mode");
            this.mAccessControlLockModedUri = uriFor2;
            Uri uriFor3 = Settings.Secure.getUriFor("access_control_lock_convenient");
            this.mAccessControlLockConvenientUri = uriFor3;
            Uri uriFor4 = Settings.Secure.getUriFor("miui_optimization");
            this.mAccessMiuiOptimizationUri = uriFor4;
            ContentResolver resolver = context.getContentResolver();
            resolver.registerContentObserver(uriFor, false, this, -1);
            resolver.registerContentObserver(uriFor2, false, this, -1);
            resolver.registerContentObserver(uriFor3, false, this, -1);
            resolver.registerContentObserver(uriFor4, false, this, -1);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            onChange(selfChange, uri, 0);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri, int userId) {
            if (this.mAccessMiuiOptimizationUri.equals(uri)) {
                SecurityManagerService.this.updateAccessMiuiOptUri();
                return;
            }
            synchronized (SecurityManagerService.this.mUserStateLock) {
                UserState userState = SecurityManagerService.this.getUserStateLocked(userId);
                if (this.mAccessControlLockEnabledUri.equals(uri)) {
                    SecurityManagerService.this.updateAccessControlEnabledLocked(userState);
                } else if (this.mAccessControlLockModedUri.equals(uri)) {
                    SecurityManagerService.this.updateAccessControlLockModeLocked(userState);
                } else if (this.mAccessControlLockConvenientUri.equals(uri)) {
                    SecurityManagerService.this.updateAccessControlLockConvenientLocked(userState);
                }
                SecurityManagerService.this.updateMaskObserverValues();
            }
        }
    }

    public SecurityManagerService(Context context, boolean onlyCore) {
        Object obj = new Object();
        this.mUserStateLock = obj;
        this.mContext = context;
        File systemDir = new File(Environment.getDataDirectory(), "system");
        this.mSettingsFile = new AtomicFile(new File(systemDir, "miui-packages.xml"));
        HandlerThread securityWriteHandlerThread = new HandlerThread("SecurityWriteHandlerThread");
        securityWriteHandlerThread.start();
        Looper looper = securityWriteHandlerThread.getLooper();
        this.mSecurityWriteHandler = new SecurityWriteHandler(looper);
        MyPackageMonitor myPackageMonitor = new MyPackageMonitor();
        this.mPackageMonitor = myPackageMonitor;
        myPackageMonitor.register(this.mContext, this.mSecurityWriteHandler.getLooper(), false);
        readSettings();
        updateXSpaceSettings();
        initForKK();
        this.mWakeUpFile = new AtomicFile(new File(systemDir, "miui-wakeuptime.xml"));
        readWakeUpTime();
        this.mAccessController = new AccessController(context, looper);
        this.mSettingsObserver = new SettingsObserver(this.mSecurityWriteHandler, context);
        synchronized (obj) {
            UserState userState = getUserStateLocked(0);
            initAccessControlSettingsLocked(userState);
        }
        if ((Build.VERSION.SDK_INT == 21 || Build.VERSION.SDK_INT == 22) && ("hennessy".equals(miui.os.Build.DEVICE) || "kenzo".equals(miui.os.Build.DEVICE) || "ido".equals(miui.os.Build.DEVICE) || "aqua".equals(miui.os.Build.DEVICE))) {
            this.mFingerprintNotify = true;
        }
        AppRunningControlService appRunningControlService = new AppRunningControlService(this.mContext);
        mAppRunningControlService = appRunningControlService;
        this.mAppRunningControlBinder = appRunningControlService.asBinder();
        WakePathChecker.getInstance().init(this.mContext);
        StorageRestrictedPathManager.getInstance().init(this.mContext);
        resetDefaultBrowser(this.mContext);
        RestrictAppNetManager.init(this.mContext);
        this.mAccessController.updatePasswordTypeForPattern(UserHandle.myUserId());
        LightsManager lightManager = (LightsManager) LocalServices.getService(LightsManager.class);
        if (lightManager != null) {
            this.mLedLight = lightManager.getLight(4);
        }
        this.mLightOn = this.mContext.getResources().getInteger(285605982);
        LocalServices.addService(SecurityManagerInternal.class, new LocalService());
    }

    public UserState getUserStateLocked(int userHandle) {
        UserState userState = this.mUserStates.get(userHandle);
        if (userState == null) {
            UserState userState2 = new UserState();
            userState2.userHandle = userHandle;
            this.mUserStates.put(userHandle, userState2);
            return userState2;
        }
        return userState;
    }

    private UserState getUserStateOrNullUnLocked(int userHandle) {
        UserState userState;
        int userHandle2 = SecurityManager.getUserHandle(userHandle);
        synchronized (this.mUserStateLock) {
            userState = this.mUserStates.get(userHandle2);
        }
        return userState;
    }

    private void initAccessControlSettingsLocked(UserState userState) {
        updateAccessControlEnabledLocked(userState);
        updateAccessControlLockModeLocked(userState);
        updateAccessControlLockConvenientLocked(userState);
        userState.mAccessControlSettingInit = true;
    }

    public void updateAccessControlEnabledLocked(UserState userState) {
        boolean z = false;
        if (Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "access_control_lock_enabled", 0, userState.userHandle) == 1) {
            z = true;
        }
        userState.mAccessControlEnabled = z;
    }

    public void updateAccessControlLockModeLocked(UserState userState) {
        userState.mAccessControlLockMode = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "access_control_lock_mode", 1, userState.userHandle);
    }

    public void updateAccessControlLockConvenientLocked(UserState userState) {
        boolean z = false;
        if (Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "access_control_lock_convenient", 0, userState.userHandle) == 1) {
            z = true;
        }
        userState.mAccessControlLockConvenient = z;
    }

    public void updateAccessMiuiOptUri() {
        if (Build.VERSION.SDK_INT > 22 && AppOpsUtils.isXOptMode() && Build.VERSION.SDK_INT >= 29) {
            try {
                PackageManager pm = this.mContext.getPackageManager();
                PackageManagerCompat.setDefaultBrowserPackageNameAsUser(pm, "", 0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        PackageManagerServiceImpl.get().switchPackageInstaller();
        MiuiDefaultPermissionGrantPolicy.revokeAllPermssions();
        if (!AppOpsUtils.isXOptMode()) {
            MiuiDefaultPermissionGrantPolicy.grantMiuiPackageInstallerPermssions();
            setDefaultBrowser();
        }
    }

    public static Object callObjectMethod(Object target, String method, Class<?>[] parameterTypes, Object... values) throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Method declaredMethod = target.getClass().getDeclaredMethod(method, parameterTypes);
        declaredMethod.setAccessible(true);
        return declaredMethod.invoke(target, values);
    }

    private boolean getAccessControlEnabledLocked(UserState userState) {
        UserState transferUserState = changeUserState(userState);
        if (!transferUserState.mAccessControlSettingInit) {
            initAccessControlSettingsLocked(transferUserState);
        }
        return transferUserState.mAccessControlEnabled;
    }

    private int getAccessControlLockMode(UserState userState) {
        UserState transferUserState = changeUserState(userState);
        if (!transferUserState.mAccessControlSettingInit) {
            initAccessControlSettingsLocked(transferUserState);
        }
        return transferUserState.mAccessControlLockMode;
    }

    private boolean getAccessControlLockConvenient(UserState userState) {
        UserState transferUserState = changeUserState(userState);
        if (!transferUserState.mAccessControlSettingInit) {
            initAccessControlSettingsLocked(transferUserState);
        }
        return transferUserState.mAccessControlLockConvenient;
    }

    private static int compareSignatures(Signature[] s1, Signature[] s2) {
        if (s1 == null || s2 == null) {
            return -3;
        }
        HashSet<Signature> set1 = new HashSet<>();
        for (Signature sig : s1) {
            set1.add(sig);
        }
        HashSet<Signature> set2 = new HashSet<>();
        for (Signature sig2 : s2) {
            set2.add(sig2);
        }
        return set1.equals(set2) ? 0 : -3;
    }

    private void enforceAppSignature(Signature[] validSignatures, String pkgName, boolean checkEnabled) {
        if (!checkAppSignature(validSignatures, pkgName, checkEnabled)) {
            throw new RuntimeException("System error: connot find system app : " + pkgName);
        }
    }

    private boolean checkAppSignature(Signature[] validSignatures, String pkgName, boolean checkEnabled) {
        PackageInfo packageInfo;
        ApplicationInfo aInfo;
        PackageManager pm = this.mContext.getPackageManager();
        try {
            packageInfo = pm.getPackageInfo(pkgName, 64);
            aInfo = packageInfo.applicationInfo;
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
        if (checkEnabled && !aInfo.enabled) {
            Log.e(TAG, "System error: " + pkgName + "disabled");
            return false;
        }
        Signature[] signatures = packageInfo.signatures;
        if (compareSignatures(validSignatures, signatures) != 0) {
            Log.e(TAG, pkgName + " signature not match!");
            return false;
        }
        return true;
    }

    private boolean isOldmanMode() {
        return miui.os.Build.getUserMode() == 1;
    }

    private boolean checkSysAppCrack() {
        ArrayList<AppItem> appsTobeChecked = new ArrayList<>();
        appsTobeChecked.add(new AppItem(InputMethodManagerServiceImpl.MIUI_HOME, SignatureConstants.PLATFORM, false));
        appsTobeChecked.add(new AppItem(AccessController.PACKAGE_GALLERY, SignatureConstants.PLATFORM, false));
        if (!miui.os.Build.IS_INTERNATIONAL_BUILD && !miui.os.Build.IS_CM_CUSTOMIZATION && !miui.os.Build.IS_CM_CUSTOMIZATION_TEST) {
            appsTobeChecked.add(new AppItem("com.miui.player", SignatureConstants.PLATFORM, false));
            appsTobeChecked.add(new AppItem("com.android.browser", SignatureConstants.PLATFORM, false));
            appsTobeChecked.add(new AppItem("com.miui.video", SignatureConstants.PLATFORM, false));
        }
        Iterator<AppItem> it = appsTobeChecked.iterator();
        while (it.hasNext()) {
            AppItem appItem = it.next();
            Signature[] sigs = {new Signature(appItem.mSignature)};
            if (!checkAppSignature(sigs, appItem.mPkg, appItem.mCheckEnable)) {
                Log.e(TAG, "checkAppSignature failed at " + appItem.mPkg);
                return true;
            }
        }
        return false;
    }

    private void enforcePlatformSignature(Signature[] signatures) {
        Signature platformSig = new Signature(SignatureConstants.PLATFORM);
        for (Signature sig : signatures) {
            if (platformSig.equals(sig)) {
                return;
            }
        }
        throw new RuntimeException("System error: My heart is broken");
    }

    private void checkEnabled(PackageManager pm, String pkg) {
        SecurityManagerCompat.checkAppHidden(pm, pkg, UserHandle.OWNER);
        try {
            int state = pm.getApplicationEnabledSetting(pkg);
            if (state == 0 || state == 1) {
                return;
            }
            pm.setApplicationEnabledSetting(pkg, 0, 0);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private void initForKK() {
        if (Build.VERSION.SDK_INT >= 19) {
            this.mAom = (AppOpsManager) this.mContext.getSystemService("appops");
            HandlerThread handlerThread = new HandlerThread(TAG);
            this.mHandlerThread = handlerThread;
            handlerThread.start();
            Handler handler = new Handler(this.mHandlerThread.getLooper());
            this.mHandler = handler;
            this.mSecuritySmsHandler = new SecuritySmsHandler(this.mContext, handler);
        }
    }

    private void resetDefaultBrowser(final Context context) {
        if (miui.os.Build.IS_INTERNATIONAL_BUILD || AppOpsUtils.isXOptMode() || Build.VERSION.SDK_INT < 24) {
            return;
        }
        this.mHandler.postDelayed(new Runnable() { // from class: com.miui.server.SecurityManagerService.1
            @Override // java.lang.Runnable
            public void run() {
                ContentResolver cr = context.getContentResolver();
                try {
                    SecurityManagerService.this.checkIntentFilterVerifications();
                    if (Build.VERSION.SDK_INT < 29) {
                        int defBrowserCount = Settings.Secure.getInt(cr, SecurityManagerService.DEF_BROWSER_COUNT, -1);
                        if (defBrowserCount != -1) {
                            return;
                        }
                        PackageManager pm = context.getPackageManager();
                        String defaultBrowser = PackageManagerCompat.getDefaultBrowserPackageNameAsUser(pm, 0);
                        if (TextUtils.isEmpty(defaultBrowser) || SecurityManagerService.PKG_BROWSER.equals(defaultBrowser)) {
                            Settings.Secure.putInt(cr, SecurityManagerService.DEF_BROWSER_COUNT, 1);
                            return;
                        }
                        PackageManagerCompat.setDefaultBrowserPackageNameAsUser(pm, "", 0);
                        Settings.Secure.putInt(cr, SecurityManagerService.DEF_BROWSER_COUNT, 10);
                        return;
                    }
                    SecurityManagerService.this.setDefaultBrowser();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, LOCK_TIME_OUT);
    }

    public void setDefaultBrowser() {
        PackageManager pm = this.mContext.getPackageManager();
        try {
            String defaultBrowser = PackageManagerCompat.getDefaultBrowserPackageNameAsUser(pm, 0);
            if (TextUtils.isEmpty(defaultBrowser)) {
                PackageManagerCompat.setDefaultBrowserPackageNameAsUser(pm, PKG_BROWSER, 0);
            }
        } catch (Exception e) {
            Log.e(TAG, "setDefaultBrowser", e);
        }
    }

    public void checkIntentFilterVerifications() {
        List<PackageInfo> applications;
        String str;
        int i;
        boolean z;
        List<PackageInfo> applications2;
        String str2 = "android.intent.action.VIEW";
        PackageManager pm = this.mContext.getPackageManager();
        try {
            List<PackageInfo> applications3 = pm.getInstalledPackages(8192);
            Intent browserIntent = new Intent().setAction(str2).addCategory("android.intent.category.BROWSABLE").setData(Uri.parse("http://"));
            Intent httpIntent = new Intent().setAction(str2).setData(Uri.parse("http://www.xiaomi.com"));
            Intent httpsIntent = new Intent().setAction(str2).setData(Uri.parse("https://www.xiaomi.com"));
            int i2 = 1;
            Set<String> browsers = queryIntentPackages(pm, browserIntent, true, 0);
            Set<String> httpPackages = queryIntentPackages(pm, httpIntent, false, 0);
            Set<String> httpsPackages = queryIntentPackages(pm, httpsIntent, false, 0);
            httpPackages.addAll(httpsPackages);
            ArraySet<String> rejectPks = new ArraySet<>();
            for (PackageInfo info : applications3) {
                if ((info.applicationInfo.flags & i2) == 0) {
                    String pkg = info.applicationInfo.packageName;
                    if (!browsers.contains(pkg) && httpPackages.contains(pkg)) {
                        List<IntentFilter> filters = pm.getAllIntentFilters(pkg);
                        boolean add = false;
                        if (filters == null || filters.size() <= 0) {
                            str = str2;
                            applications = applications3;
                        } else {
                            for (IntentFilter filter : filters) {
                                if (filter.hasAction(str2)) {
                                    String str3 = str2;
                                    if (filter.hasDataScheme("http") || filter.hasDataScheme("https")) {
                                        ArrayList<String> hostList = filter.getHostsList();
                                        if (hostList.size() != 0) {
                                            ArrayList<String> hostList2 = filter.getHostsList();
                                            if (hostList2.contains("*")) {
                                            }
                                        }
                                        int dataPathsCount = filter.countDataPaths();
                                        if (dataPathsCount > 0) {
                                            int i3 = 0;
                                            while (i3 < dataPathsCount) {
                                                int dataPathsCount2 = dataPathsCount;
                                                List<PackageInfo> applications4 = applications3;
                                                if (".*".equals(filter.getDataPath(i3).getPath())) {
                                                    add = true;
                                                }
                                                i3++;
                                                dataPathsCount = dataPathsCount2;
                                                applications3 = applications4;
                                            }
                                            applications2 = applications3;
                                        } else {
                                            applications2 = applications3;
                                            add = true;
                                        }
                                        str2 = str3;
                                        applications3 = applications2;
                                    }
                                    applications2 = applications3;
                                    str2 = str3;
                                    applications3 = applications2;
                                }
                            }
                            str = str2;
                            applications = applications3;
                        }
                        if (!add) {
                            z = false;
                            i = 1;
                        } else {
                            z = false;
                            int status = pm.getIntentVerificationStatusAsUser(pkg, 0);
                            if (status != 0) {
                                i = 1;
                                if (status == 1) {
                                }
                            } else {
                                i = 1;
                            }
                            rejectPks.add(pkg);
                        }
                        i2 = i;
                        str2 = str;
                        applications3 = applications;
                    }
                }
            }
            Iterator<String> it = rejectPks.iterator();
            while (it.hasNext()) {
                pm.updateIntentVerificationStatusAsUser(it.next(), 3, 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Set<String> queryIntentPackages(PackageManager pm, Intent intent, boolean allweb, int userId) {
        List<ResolveInfo> list = pm.queryIntentActivitiesAsUser(intent, 131072, userId);
        int count = list.size();
        Set<String> packages = new ArraySet<>();
        for (int i = 0; i < count; i++) {
            ResolveInfo info = list.get(i);
            if (info.activityInfo != null && (!allweb || info.handleAllWebDataURI)) {
                String packageName = info.activityInfo.packageName;
                if (!packages.contains(packageName)) {
                    packages.add(packageName);
                }
            }
        }
        return packages;
    }

    public void killNativePackageProcesses(int uid, String pkgName) {
        checkPermission();
        if (uid >= 10000) {
            nativeKillPackageProcesses(uid, pkgName);
        }
    }

    public String getPackageNameByPid(int pid) {
        int callingUid = Binder.getCallingUid();
        if (callingUid != 0 && UserHandle.getAppId(callingUid) != 1000) {
            try {
                PackageManager pm = this.mContext.getPackageManager();
                String[] packages = pm.getPackagesForUid(callingUid);
                if (packages != null && packages.length != 0) {
                    ApplicationInfo packageInfo = pm.getApplicationInfo(packages[0], 0);
                    if ((packageInfo.flags & 1) == 0) {
                        return "";
                    }
                }
                return "";
            } catch (Exception e) {
                return "";
            }
        }
        return ProcessUtils.getPackageNameByPid(pid);
    }

    public boolean checkSmsBlocked(Intent intent) {
        return this.mSecuritySmsHandler.checkSmsBlocked(intent);
    }

    public boolean startInterceptSmsBySender(String pkgName, String sender, int count) {
        return this.mSecuritySmsHandler.startInterceptSmsBySender(pkgName, sender, count);
    }

    public boolean stopInterceptSmsBySender() {
        return this.mSecuritySmsHandler.stopInterceptSmsBySender();
    }

    public void addAccessControlPass(String packageName) {
        int callingUserId = UserHandle.getCallingUserId();
        addAccessControlPassForUser(packageName, callingUserId);
    }

    public void addAccessControlPassForUser(String packageName, int userId) {
        checkPermission();
        synchronized (this.mUserStateLock) {
            UserState userState = getUserStateLocked(userId);
            int lockMode = getAccessControlLockMode(userState);
            if (lockMode == 2) {
                userState.mAccessControlLastCheck.put(packageName, Long.valueOf(SystemClock.elapsedRealtime()));
                scheduleForMaskObserver(packageName, userId);
            } else {
                updateMaskObserverValues();
            }
            userState.mAccessControlPassPackages.add(packageName);
        }
    }

    public void removeAccessControlPass(String packageName) {
        checkPermission();
        int callingUserId = UserHandle.getCallingUserId();
        removeAccessControlPassAsUser(packageName, callingUserId);
    }

    public boolean checkAccessControlPass(String packageName, Intent intent) {
        int callingUserId = UserHandle.getCallingUserId();
        return checkAccessControlPassLocked(packageName, intent, callingUserId);
    }

    public boolean checkAccessControlPassAsUser(String packageName, Intent intent, int userId) {
        return checkAccessControlPassLocked(packageName, intent, userId);
    }

    public boolean getApplicationAccessControlEnabledAsUser(String packageName, int userId) {
        return getApplicationAccessControlEnabledLocked(packageName, userId);
    }

    public boolean getApplicationMaskNotificationEnabledAsUser(String packageName, int userId) {
        return getApplicationMaskNotificationEnabledLocked(packageName, userId);
    }

    public boolean checkGameBoosterAntimsgPassAsUser(String packageName, Intent intent, int userId) {
        return !this.mAccessController.filterIntentLocked(false, packageName, intent);
    }

    public void setGameBoosterIBinder(IBinder gameBooster, int userId, boolean isGameMode) {
        checkPermission();
        synchronized (this.mUserStateLock) {
            UserState userState = getUserStateLocked(SecurityManager.getUserHandle(userId));
            try {
                if (userState.gameBoosterServiceDeath == null) {
                    userState.gameBoosterServiceDeath = new GameBoosterServiceDeath(userState, gameBooster);
                    gameBooster.linkToDeath(userState.gameBoosterServiceDeath, 0);
                } else if (gameBooster != userState.gameBoosterServiceDeath.mGameBoosterService) {
                    userState.gameBoosterServiceDeath.mGameBoosterService.unlinkToDeath(userState.gameBoosterServiceDeath, 0);
                    userState.gameBoosterServiceDeath = new GameBoosterServiceDeath(userState, gameBooster);
                    gameBooster.linkToDeath(userState.gameBoosterServiceDeath, 0);
                } else {
                    userState.mIsGameMode = isGameMode;
                }
            } catch (Exception e) {
                Log.e(TAG, "setGameBoosterIBinder", e);
            }
        }
    }

    public boolean getGameMode(int userId) {
        boolean z;
        synchronized (this.mUserStateLock) {
            UserState userState = getUserStateLocked(SecurityManager.getUserHandle(userId));
            z = userState.mIsGameMode;
        }
        return z;
    }

    private boolean checkAccessControlPassLocked(String packageName, Intent intent, int userId) {
        synchronized (this.mUserStateLock) {
            UserState userState = getUserStateLocked(userId);
            PackageSetting ps = getPackageSetting(userState.mPackages, packageName);
            if (!ps.accessControl) {
                return true;
            }
            return checkAccessControlPassLockedCore(userState, packageName, intent);
        }
    }

    private boolean checkAccessControlPassLockedCore(UserState userState, String packageName, Intent intent) {
        int lockMode = getAccessControlLockMode(userState);
        boolean pass = userState.mAccessControlPassPackages.contains(packageName);
        if (pass && lockMode == 2) {
            Long lastTime = userState.mAccessControlLastCheck.get(packageName);
            if (lastTime != null) {
                long realtime = SystemClock.elapsedRealtime();
                if (realtime - lastTime.longValue() > LOCK_TIME_OUT) {
                    pass = false;
                }
            }
            if (pass && (Build.VERSION.SDK_INT < 24 || !"com.android.systemui".equals(ProcessUtils.getPackageNameByPid(Binder.getCallingPid())))) {
                userState.mAccessControlLastCheck.put(packageName, Long.valueOf(SystemClock.elapsedRealtime()));
                scheduleForMaskObserver(packageName, userState.userHandle);
            }
        }
        if (!pass && lockMode == 1 && getAccessControlLockConvenient(userState) && isPackageAccessControlPass(userState)) {
            pass = true;
        }
        if (!pass && this.mAccessController.skipActivity(intent, ProcessUtils.getPackageNameByPid(Binder.getCallingPid()))) {
            pass = true;
        }
        if (!pass && this.mAccessController.filterIntentLocked(true, packageName, intent)) {
            return true;
        }
        return pass;
    }

    public boolean getApplicationAccessControlEnabled(String packageName) {
        int callingUserId = UserHandle.getCallingUserId();
        return getApplicationAccessControlEnabledLocked(packageName, callingUserId);
    }

    private boolean getApplicationAccessControlEnabledLocked(String packageName, int userId) {
        boolean z;
        synchronized (this.mUserStateLock) {
            UserState userState = getUserStateLocked(userId);
            try {
                PackageSetting ps = getPackageSetting(userState.mPackages, packageName);
                z = ps.accessControl;
            } catch (Exception e) {
                return false;
            }
        }
        return z;
    }

    private boolean getApplicationMaskNotificationEnabledLocked(String packageName, int userId) {
        boolean z;
        synchronized (this.mUserStateLock) {
            UserState userState = getUserStateLocked(userId);
            try {
                PackageSetting ps = getPackageSetting(userState.mPackages, packageName);
                z = ps.maskNotification;
            } catch (Exception e) {
                return false;
            }
        }
        return z;
    }

    public void setApplicationAccessControlEnabled(String packageName, boolean enabled) {
        int callingUserId = UserHandle.getCallingUserId();
        setApplicationAccessControlEnabledForUser(packageName, enabled, callingUserId);
    }

    public void setApplicationAccessControlEnabledForUser(String packageName, boolean enabled, int userId) {
        checkPermission();
        synchronized (this.mUserStateLock) {
            UserState userStateLocked = getUserStateLocked(userId);
            PackageSetting ps = getPackageSetting(userStateLocked.mPackages, packageName);
            ps.accessControl = enabled;
            scheduleWriteSettings();
            updateMaskObserverValues();
        }
    }

    public boolean getAppDarkMode(String packageName) {
        int userId = UserHandle.getCallingUserId();
        return getAppDarkModeForUser(packageName, userId);
    }

    public boolean getAppDarkModeForUser(String packageName, int userId) {
        boolean z;
        synchronized (this.mUserStateLock) {
            UserState userState = getUserStateLocked(userId);
            try {
                PackageSetting ps = getPackageSetting(userState.mPackages, packageName);
                z = ps.isDarkModeChecked;
            } catch (Exception e) {
                return false;
            }
        }
        return z;
    }

    public void setAppDarkModeForUser(String packageName, boolean enabled, int userId) {
        synchronized (this.mUserStateLock) {
            UserState userStateLocked = getUserStateLocked(userId);
            PackageSetting ps = getPackageSetting(userStateLocked.mPackages, packageName);
            ps.isDarkModeChecked = enabled;
            scheduleWriteSettings();
        }
    }

    public boolean getAppRemindForRelaunch(String packageName) {
        int userId = UserHandle.getCallingUserId();
        return getAppRemindForRelaunchForUser(packageName, userId);
    }

    public boolean getAppRemindForRelaunchForUser(String packageName, int userId) {
        boolean z;
        synchronized (this.mUserStateLock) {
            UserState userState = getUserStateLocked(userId);
            try {
                PackageSetting ps = getPackageSetting(userState.mPackages, packageName);
                z = ps.isRemindForRelaunch;
            } catch (Exception e) {
                return false;
            }
        }
        return z;
    }

    public void setAppRemindForRelaunchForUser(String packageName, boolean enabled, int userId) {
        synchronized (this.mUserStateLock) {
            UserState userStateLocked = getUserStateLocked(userId);
            PackageSetting ps = getPackageSetting(userStateLocked.mPackages, packageName);
            ps.isRemindForRelaunch = enabled;
            scheduleWriteSettings();
        }
    }

    public boolean getAppRelaunchModeAfterFolded(String packageName) {
        int userId = UserHandle.getCallingUserId();
        return getAppRelaunchModeAfterFoldedForUser(packageName, userId);
    }

    public boolean getAppRelaunchModeAfterFoldedForUser(String packageName, int userId) {
        boolean z;
        synchronized (this.mUserStateLock) {
            UserState userState = getUserStateLocked(userId);
            try {
                PackageSetting ps = getPackageSetting(userState.mPackages, packageName);
                z = ps.isRelaunchWhenFolded;
            } catch (Exception e) {
                return false;
            }
        }
        return z;
    }

    public void setAppRelaunchModeAfterFoldedForUser(String packageName, boolean enabled, int userId) {
        synchronized (this.mUserStateLock) {
            UserState userStateLocked = getUserStateLocked(userId);
            PackageSetting ps = getPackageSetting(userStateLocked.mPackages, packageName);
            ps.isRelaunchWhenFolded = enabled;
            scheduleWriteSettings();
        }
    }

    public boolean isScRelaunchNeedConfirm(String packageName) {
        int userId = UserHandle.getCallingUserId();
        return isScRelaunchNeedConfirmForUser(packageName, userId);
    }

    public boolean isScRelaunchNeedConfirmForUser(String packageName, int userId) {
        boolean z;
        synchronized (this.mUserStateLock) {
            UserState userState = getUserStateLocked(userId);
            try {
                PackageSetting ps = getPackageSetting(userState.mPackages, packageName);
                z = ps.isScRelaunchConfirm;
            } catch (Exception e) {
                return false;
            }
        }
        return z;
    }

    public void setScRelaunchNeedConfirmForUser(String packageName, boolean confirm, int userId) {
        synchronized (this.mUserStateLock) {
            UserState userStateLocked = getUserStateLocked(userId);
            PackageSetting ps = getPackageSetting(userStateLocked.mPackages, packageName);
            ps.isScRelaunchConfirm = confirm;
            scheduleWriteSettings();
        }
    }

    public void setApplicationMaskNotificationEnabledForUser(String packageName, boolean enabled, int userId) {
        checkPermission();
        synchronized (this.mUserStateLock) {
            UserState userStateLocked = getUserStateLocked(userId);
            PackageSetting ps = getPackageSetting(userStateLocked.mPackages, packageName);
            ps.maskNotification = enabled;
            scheduleWriteSettings();
            updateMaskObserverValues();
        }
    }

    public void saveIcon(String fileName, Bitmap icon) {
        saveIconInner(fileName, icon);
    }

    public void removeAccessControlPassAsUser(String packageName, int userId) {
        HashMap<String, Object> topActivity;
        checkPermission();
        String pkgName = null;
        IBinder token = null;
        Integer activityUserId = 0;
        boolean checkAccessControlPass = false;
        if (userId != -1) {
            topActivity = null;
        } else {
            topActivity = WindowProcessUtils.getTopRunningActivityInfo();
        }
        synchronized (this.mUserStateLock) {
            if (userId == -1) {
                int size = this.mUserStates.size();
                for (int i = 0; i < size; i++) {
                    UserState userState = this.mUserStates.valueAt(i);
                    removeAccessControlPassLocked(userState, packageName);
                }
                int currentUserId = getCurrentUserId();
                UserState userState2 = getUserStateLocked(currentUserId);
                boolean enabled = getAccessControlEnabledLocked(userState2);
                if (!enabled) {
                    return;
                }
                if (topActivity != null) {
                    pkgName = (String) topActivity.get("packageName");
                    token = (IBinder) topActivity.get("token");
                    activityUserId = (Integer) topActivity.get("userId");
                    checkAccessControlPass = checkAccessControlPassLocked(pkgName, null, activityUserId.intValue());
                }
            } else {
                UserState userState3 = getUserStateLocked(userId);
                removeAccessControlPassLocked(userState3, packageName);
            }
            if (userId == -1 && topActivity != null) {
                if (!checkAccessControlPass) {
                    try {
                        Intent intent = SecurityManager.getCheckAccessIntent(true, pkgName, (Intent) null, -1, true, activityUserId.intValue(), (Bundle) null);
                        intent.putExtra("miui.KEYGUARD_LOCKED", true);
                        SecurityManagerCompat.startActvityAsUser(this.mContext, (IApplicationThread) null, token, (String) null, intent, activityUserId.intValue());
                    } catch (Exception e) {
                        Log.e(TAG, "removeAccessControlPassAsUser startActvityAsUser error ", e);
                    }
                } else if (this.mFingerprintNotify && "com.miui.securitycenter".equals(pkgName)) {
                    Intent intent2 = new Intent("miui.intent.action.APP_LOCK_CLEAR_STATE");
                    intent2.setPackage("com.miui.securitycenter");
                    this.mContext.sendBroadcast(intent2);
                }
            }
        }
    }

    private void removeAccessControlPassLocked(UserState userState, String packageName) {
        if ("*".equals(packageName)) {
            userState.mAccessControlPassPackages.clear();
            userState.mAccessControlLastCheck.clear();
        } else {
            userState.mAccessControlPassPackages.remove(packageName);
        }
        updateMaskObserverValues();
    }

    public void setAccessControlPassword(String passwordType, String password, int userId) {
        checkPermission();
        this.mAccessController.setAccessControlPassword(passwordType, password, SecurityManager.getUserHandle(userId));
    }

    public boolean checkAccessControlPassword(String passwordType, String password, int userId) {
        checkPermission();
        return this.mAccessController.checkAccessControlPassword(passwordType, password, SecurityManager.getUserHandle(userId));
    }

    public boolean haveAccessControlPassword(int userId) {
        return this.mAccessController.haveAccessControlPassword(SecurityManager.getUserHandle(userId));
    }

    public String getAccessControlPasswordType(int userId) {
        checkPermission();
        return this.mAccessController.getAccessControlPasswordType(SecurityManager.getUserHandle(userId));
    }

    public boolean needFinishAccessControl(IBinder token) throws RemoteException {
        Intent intent;
        ComponentName component;
        checkPermission();
        ArrayList<Intent> taskIntent = WindowProcessUtils.getTaskIntentForToken(token);
        if (taskIntent != null && taskIntent.size() > 1 && (component = (intent = taskIntent.get(1)).getComponent()) != null) {
            return this.mAccessController.filterIntentLocked(true, component.getPackageName(), intent);
        }
        return false;
    }

    public void finishAccessControl(String packageName, int userId) throws RemoteException {
        checkPermission();
        if (packageName == null) {
            return;
        }
        synchronized (this.mUserStateLock) {
            UserState userState = getUserStateLocked(userId);
            userState.mAccessControlCanceled.add(packageName);
            Message msg = this.mSecurityWriteHandler.obtainMessage(4);
            msg.arg1 = userId;
            msg.obj = packageName;
            this.mSecurityWriteHandler.sendMessageDelayed(msg, 500L);
        }
    }

    public int activityResume(Intent intent) {
        ComponentName componentName;
        String packageName;
        if (intent == null || (componentName = intent.getComponent()) == null || (packageName = componentName.getPackageName()) == null) {
            return 0;
        }
        int callingUid = Binder.getCallingUid();
        int userId = UserHandle.getUserId(callingUid);
        synchronized (this.mUserStateLock) {
            UserState userState = getUserStateLocked(userId);
            boolean enabled = getAccessControlEnabledLocked(userState);
            if (!enabled) {
                return 0;
            }
            int packageUid = PackageManagerServiceStub.get().getPackageUid(packageName, userId);
            if (callingUid != packageUid) {
                return 0;
            }
            int lockMode = getAccessControlLockMode(userState);
            String oldResumePackage = userState.mLastResumePackage;
            userState.mLastResumePackage = packageName;
            HashSet<String> passPackages = userState.mAccessControlPassPackages;
            if (lockMode == 2 && oldResumePackage != null && passPackages.contains(oldResumePackage)) {
                userState.mAccessControlLastCheck.put(oldResumePackage, Long.valueOf(SystemClock.elapsedRealtime()));
                scheduleForMaskObserver(packageName, userId);
            }
            PackageSetting ps = getPackageSetting(userState.mPackages, packageName);
            if (ps.accessControl) {
                int result = 1 | 2;
                if (passPackages.contains(packageName)) {
                    if (lockMode == 2) {
                        Long lastTime = userState.mAccessControlLastCheck.get(packageName);
                        if (lastTime != null) {
                            long realtime = SystemClock.elapsedRealtime();
                            if (realtime - lastTime.longValue() < LOCK_TIME_OUT) {
                                return result | 4;
                            }
                        }
                        passPackages.remove(packageName);
                        updateMaskObserverValues();
                    } else {
                        int result2 = result | 4;
                        if (lockMode == 0) {
                            clearPassPackages(userId);
                            passPackages.add(packageName);
                            updateMaskObserverValues();
                        }
                        return result2;
                    }
                }
                if (lockMode == 0) {
                    clearPassPackages(userId);
                }
                if (userState.mAccessControlCanceled.contains(packageName)) {
                    return result | 8;
                }
                if ((lockMode == 1 && getAccessControlLockConvenient(userState) && isPackageAccessControlPass(userState)) || this.mAccessController.skipActivity(intent, packageName) || this.mAccessController.filterIntentLocked(true, packageName, intent)) {
                    result |= 4;
                }
                return result;
            }
            if (lockMode == 0) {
                clearPassPackages(userId);
            }
            return 1;
        }
    }

    public boolean getApplicationChildrenControlEnabled(String packageName) {
        boolean z;
        int callingUserId = UserHandle.getCallingUserId();
        synchronized (this.mUserStateLock) {
            try {
                try {
                    UserState userStateLocked = getUserStateLocked(callingUserId);
                    PackageSetting ps = getPackageSetting(userStateLocked.mPackages, packageName);
                    z = ps.childrenControl;
                } catch (Exception e) {
                    return false;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        return z;
    }

    public void setApplicationChildrenControlEnabled(String packageName, boolean enabled) {
        checkPermission();
        int callingUserId = UserHandle.getCallingUserId();
        synchronized (this.mUserStateLock) {
            UserState userStateLocked = getUserStateLocked(callingUserId);
            PackageSetting ps = getPackageSetting(userStateLocked.mPackages, packageName);
            ps.childrenControl = enabled;
            scheduleWriteSettings();
        }
    }

    public void setCoreRuntimePermissionEnabled(boolean grant, int flags) throws RemoteException {
        if (Binder.getCallingUid() != 1000) {
            throw new SecurityException("setCoreRuntimePermissionEnabled Permission DENIED");
        }
        int userId = UserHandle.getCallingUserId();
        MiuiDefaultPermissionGrantPolicy.setCoreRuntimePermissionEnabled(grant, flags, userId);
    }

    public void grantRuntimePermission(String packageName) {
        int userId = UserHandle.getCallingUserId();
        int callingUid = Binder.getCallingUid();
        int packageUid = PackageManagerServiceStub.get().getPackageUid(packageName, userId);
        if (callingUid != 1000 && packageUid != callingUid) {
            return;
        }
        PackageManagerInternal packageManagerInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        ApplicationInfo appInfo = packageManagerInternal.getApplicationInfo(packageName, 0L, 1000, userId);
        if (appInfo == null) {
            return;
        }
        if (callingUid != 1000 && (appInfo.flags & 1) == 0) {
            throw new SecurityException("grantRuntimePermission Permission DENIED");
        }
        long identity = Binder.clearCallingIdentity();
        try {
            MiuiDefaultPermissionGrantPolicy.grantRuntimePermission(packageName, userId);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    /* loaded from: classes.dex */
    public class PackageSetting {
        boolean isDarkModeChecked;
        String name;
        boolean accessControl = false;
        boolean childrenControl = false;
        boolean maskNotification = false;
        boolean isPrivacyApp = false;
        boolean isGameStorageApp = false;
        boolean isRemindForRelaunch = true;
        boolean isRelaunchWhenFolded = false;
        boolean isScRelaunchConfirm = true;

        PackageSetting(String name) {
            SecurityManagerService.this = this$0;
            this.name = name;
            this.isDarkModeChecked = ForceDarkAppListProvider.getInstance().getForceDarkAppDefaultEnable(name);
        }
    }

    private PackageSetting getPackageSetting(HashMap<String, PackageSetting> packages, String packageName) {
        PackageSetting ps = packages.get(packageName);
        if (ps == null) {
            PackageSetting ps2 = new PackageSetting(packageName);
            packages.put(packageName, ps2);
            return ps2;
        }
        return ps;
    }

    private void scheduleWriteSettings() {
        if (this.mSecurityWriteHandler.hasMessages(1)) {
            return;
        }
        this.mSecurityWriteHandler.sendEmptyMessageDelayed(1, 1000L);
    }

    private void readSettings() {
        if (!this.mSettingsFile.getBaseFile().exists()) {
            return;
        }
        FileInputStream fis = null;
        try {
            try {
                try {
                    fis = this.mSettingsFile.openRead();
                    readPackagesSettings(fis);
                } catch (Throwable th) {
                    if (fis != null) {
                        try {
                            fis.close();
                        } catch (IOException e) {
                        }
                    }
                    throw th;
                }
            } catch (Exception e2) {
                Log.w(TAG, "Error reading package settings", e2);
                if (fis != null) {
                    fis.close();
                } else {
                    return;
                }
            }
            if (fis != null) {
                fis.close();
            }
        } catch (IOException e3) {
        }
    }

    private void readPackagesSettings(FileInputStream fis) throws Exception {
        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(fis, null);
        for (int eventType = parser.getEventType(); eventType != 2 && eventType != 1; eventType = parser.next()) {
        }
        String tagName = parser.getName();
        if ("packages".equals(tagName)) {
            String updateVersion = parser.getAttributeValue(null, "updateVersion");
            if (!TextUtils.isEmpty(updateVersion) && "1.0".equals(updateVersion)) {
                this.mIsUpdated = true;
            }
            int eventType2 = parser.next();
            do {
                if (eventType2 == 2 && parser.getDepth() == 2) {
                    String tagName2 = parser.getName();
                    if ("package".equals(tagName2)) {
                        String name = parser.getAttributeValue(null, "name");
                        if (TextUtils.isEmpty(name)) {
                            Slog.i(TAG, "read current package name is empty, skip");
                            continue;
                        } else {
                            PackageSetting ps = new PackageSetting(name);
                            int userHandle = 0;
                            String userHandleStr = parser.getAttributeValue(null, "u");
                            if (!TextUtils.isEmpty(userHandleStr)) {
                                userHandle = Integer.parseInt(userHandleStr);
                            }
                            ps.accessControl = Boolean.parseBoolean(parser.getAttributeValue(null, "accessControl"));
                            ps.childrenControl = Boolean.parseBoolean(parser.getAttributeValue(null, "childrenControl"));
                            ps.maskNotification = Boolean.parseBoolean(parser.getAttributeValue(null, "maskNotification"));
                            ps.isPrivacyApp = Boolean.parseBoolean(parser.getAttributeValue(null, "isPrivacyApp"));
                            ps.isDarkModeChecked = Boolean.parseBoolean(parser.getAttributeValue(null, "isDarkModeChecked"));
                            ps.isGameStorageApp = Boolean.parseBoolean(parser.getAttributeValue(null, "isGameStorageApp"));
                            ps.isRemindForRelaunch = Boolean.parseBoolean(parser.getAttributeValue(null, "isRemindForRelaunch"));
                            ps.isRelaunchWhenFolded = Boolean.parseBoolean(parser.getAttributeValue(null, "isRelaunchWhenFolded"));
                            ps.isScRelaunchConfirm = Boolean.parseBoolean(parser.getAttributeValue(null, "isScRelaunchConfirm"));
                            synchronized (this.mUserStateLock) {
                                UserState userState = getUserStateLocked(userHandle);
                                userState.mPackages.put(name, ps);
                            }
                        }
                    }
                }
                eventType2 = parser.next();
                continue;
            } while (eventType2 != 1);
        }
    }

    public void writeSettings() {
        FileOutputStream fos = null;
        try {
            ArrayList<UserState> userStates = new ArrayList<>();
            synchronized (this.mUserStateLock) {
                int size = this.mUserStates.size();
                for (int i = 0; i < size; i++) {
                    UserState state = this.mUserStates.valueAt(i);
                    UserState userState = new UserState();
                    userState.userHandle = state.userHandle;
                    userState.mPackages.putAll(new HashMap(state.mPackages));
                    userStates.add(userState);
                }
            }
            fos = this.mSettingsFile.startWrite();
            FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
            fastXmlSerializer.setOutput(fos, "utf-8");
            fastXmlSerializer.startDocument(null, true);
            fastXmlSerializer.setFeature(MiuiSizeCompatService.FAST_XML, true);
            fastXmlSerializer.startTag(null, "packages");
            fastXmlSerializer.attribute(null, "updateVersion", "1.0");
            Iterator<UserState> it = userStates.iterator();
            while (it.hasNext()) {
                UserState userState2 = it.next();
                for (PackageSetting ps : userState2.mPackages.values()) {
                    if (TextUtils.isEmpty(ps.name)) {
                        Slog.i(TAG, "write current package name is empty, skip");
                    } else {
                        fastXmlSerializer.startTag(null, "package");
                        fastXmlSerializer.attribute(null, "name", ps.name);
                        fastXmlSerializer.attribute(null, "accessControl", String.valueOf(ps.accessControl));
                        fastXmlSerializer.attribute(null, "childrenControl", String.valueOf(ps.childrenControl));
                        fastXmlSerializer.attribute(null, "maskNotification", String.valueOf(ps.maskNotification));
                        fastXmlSerializer.attribute(null, "isPrivacyApp", String.valueOf(ps.isPrivacyApp));
                        fastXmlSerializer.attribute(null, "isDarkModeChecked", String.valueOf(ps.isDarkModeChecked));
                        fastXmlSerializer.attribute(null, "isGameStorageApp", String.valueOf(ps.isGameStorageApp));
                        fastXmlSerializer.attribute(null, "isRemindForRelaunch", String.valueOf(ps.isRemindForRelaunch));
                        fastXmlSerializer.attribute(null, "isRelaunchWhenFolded", String.valueOf(ps.isRelaunchWhenFolded));
                        fastXmlSerializer.attribute(null, "isScRelaunchConfirm", String.valueOf(ps.isScRelaunchConfirm));
                        fastXmlSerializer.attribute(null, "u", String.valueOf(userState2.userHandle));
                        fastXmlSerializer.endTag(null, "package");
                    }
                }
            }
            fastXmlSerializer.endTag(null, "packages");
            fastXmlSerializer.endDocument();
            this.mSettingsFile.finishWrite(fos);
        } catch (Exception e1) {
            Log.e(TAG, "Error writing package settings file", e1);
            if (fos != null) {
                this.mSettingsFile.failWrite(fos);
            }
        }
    }

    private void removePackage(String packageName, int uid) {
        synchronized (this.mUserStateLock) {
            UserState userState = getUserStateLocked(UserHandle.getUserId(uid));
            userState.mPackages.remove(packageName);
            scheduleWriteSettings();
            updateMaskObserverValues();
        }
    }

    private void checkPermission() {
        int callingUid = Binder.getCallingUid();
        if (UserHandle.getAppId(callingUid) == 2000) {
            throw new SecurityException("no permission for UID:" + callingUid);
        }
        int permission = this.mContext.checkCallingOrSelfPermission("android.permission.CHANGE_COMPONENT_ENABLED_STATE");
        if (permission == 0) {
            return;
        }
        int managePermission = this.mContext.checkCallingOrSelfPermission("miui.permission.READ_AND_WIRTE_PERMISSION_MANAGER");
        if (managePermission != 0) {
            throw new SecurityException("Permission Denial: attempt to change application state from pid=" + Binder.getCallingPid() + ", uid=" + callingUid);
        }
    }

    private void sucheduleWriteWakeUpTime() {
        if (this.mSecurityWriteHandler.hasMessages(2)) {
            return;
        }
        this.mSecurityWriteHandler.sendEmptyMessage(2);
    }

    private void sucheduleWriteBootTime() {
        if (this.mSecurityWriteHandler.hasMessages(3)) {
            return;
        }
        this.mSecurityWriteHandler.sendEmptyMessage(3);
    }

    public void setWakeUpTime(String componentName, long timeInSeconds) {
        this.mContext.enforceCallingOrSelfPermission("com.miui.permission.MANAGE_BOOT_TIME", TAG);
        putBootTimeToMap(componentName, timeInSeconds);
        sucheduleWriteWakeUpTime();
        setTimeBoot();
    }

    private void minWakeUpTime(long nowtime) {
        long min = 0;
        long rightBorder = 300 + nowtime;
        for (String componentName : this.mWakeUpTime.keySet()) {
            long tmp = getBootTimeFromMap(componentName);
            if (tmp >= nowtime && (tmp < min || min == 0)) {
                min = tmp >= rightBorder ? tmp : rightBorder;
            }
        }
        this.mWakeTime = min;
    }

    private void setTimeBoot() {
        long now_time = System.currentTimeMillis() / 1000;
        synchronized (this.mWakeUpTime) {
            minWakeUpTime(now_time);
        }
        sucheduleWriteBootTime();
    }

    public void writeWakeUpTime() {
        FileOutputStream fos = null;
        try {
            fos = this.mWakeUpFile.startWrite();
            FastXmlSerializer fastXmlSerializer = new FastXmlSerializer();
            fastXmlSerializer.setOutput(fos, "utf-8");
            fastXmlSerializer.startDocument(null, true);
            fastXmlSerializer.setFeature(MiuiSizeCompatService.FAST_XML, true);
            fastXmlSerializer.startTag(null, CLASS_NAMES);
            synchronized (this.mWakeUpTime) {
                for (String componentName : this.mWakeUpTime.keySet()) {
                    if (getBootTimeFromMap(componentName) != 0) {
                        fastXmlSerializer.startTag(null, CLASS_NAME);
                        fastXmlSerializer.attribute(null, "name", componentName);
                        fastXmlSerializer.attribute(null, "time", String.valueOf(getBootTimeFromMap(componentName)));
                        fastXmlSerializer.endTag(null, CLASS_NAME);
                    }
                }
            }
            fastXmlSerializer.endTag(null, CLASS_NAMES);
            fastXmlSerializer.endDocument();
            this.mWakeUpFile.finishWrite(fos);
        } catch (IOException e) {
            if (fos != null) {
                this.mWakeUpFile.failWrite(fos);
            }
        }
    }

    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:13:0x0027 -> B:27:0x003c). Please submit an issue!!! */
    private void readWakeUpTime() {
        synchronized (this.mWakeUpTime) {
            this.mWakeUpTime.clear();
        }
        if (!this.mWakeUpFile.getBaseFile().exists()) {
            return;
        }
        FileInputStream fis = null;
        try {
            try {
                try {
                    fis = this.mWakeUpFile.openRead();
                    readWakeUpTime(fis);
                    if (fis != null) {
                        fis.close();
                    }
                } catch (Exception e) {
                    this.mWakeUpFile.getBaseFile().delete();
                    if (fis != null) {
                        fis.close();
                    }
                }
            } catch (IOException e2) {
            }
        } catch (Throwable th) {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e3) {
                }
            }
            throw th;
        }
    }

    private void readWakeUpTime(FileInputStream fis) throws Exception {
        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(fis, null);
        for (int eventType = parser.getEventType(); eventType != 2 && eventType != 1; eventType = parser.next()) {
        }
        String tagName = parser.getName();
        if (CLASS_NAMES.equals(tagName)) {
            int eventType2 = parser.next();
            do {
                if (eventType2 == 2 && parser.getDepth() == 2) {
                    String tagName2 = parser.getName();
                    if (CLASS_NAME.equals(tagName2)) {
                        String componentName = parser.getAttributeValue(null, "name");
                        long time = new Long(parser.getAttributeValue(null, "time")).longValue();
                        putBootTimeToMap(componentName, time);
                    }
                }
                eventType2 = parser.next();
            } while (eventType2 != 1);
        }
    }

    public long getWakeUpTime(String componentName) {
        this.mContext.enforceCallingOrSelfPermission("com.miui.permission.MANAGE_BOOT_TIME", TAG);
        return getBootTimeFromMap(componentName);
    }

    private void putBootTimeToMap(String componentName, long time) {
        synchronized (this.mWakeUpTime) {
            this.mWakeUpTime.put(componentName, Long.valueOf(time));
        }
    }

    private long getBootTimeFromMap(String componentName) {
        long longValue;
        synchronized (this.mWakeUpTime) {
            longValue = this.mWakeUpTime.containsKey(componentName) ? this.mWakeUpTime.get(componentName).longValue() : 0L;
        }
        return longValue;
    }

    public boolean putSystemDataStringFile(String path, String value) {
        checkPermissionByUid(1000);
        File file = new File(path);
        RandomAccessFile raf = null;
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            try {
                raf = new RandomAccessFile(file, "rw");
                raf.setLength(0L);
                raf.writeUTF(value);
                try {
                    raf.close();
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
                return true;
            } catch (Throwable th) {
                if (raf != null) {
                    try {
                        raf.close();
                    } catch (IOException e3) {
                        e3.printStackTrace();
                    }
                }
                throw th;
            }
        } catch (IOException e4) {
            e4.printStackTrace();
            if (raf != null) {
                try {
                    raf.close();
                    return false;
                } catch (IOException e5) {
                    e5.printStackTrace();
                    return false;
                }
            }
            return false;
        }
    }

    /* JADX WARN: Unsupported multi-entry loop pattern (BACK_EDGE: B:8:0x0025 -> B:20:0x0040). Please submit an issue!!! */
    public String readSystemDataStringFile(String path) {
        checkPermissionByUid(1000);
        File file = new File(path);
        RandomAccessFile raf = null;
        String result = null;
        try {
            try {
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (file.exists()) {
                try {
                    raf = new RandomAccessFile(file, FoldablePackagePolicy.POLICY_VALUE_RESTART_LIST);
                    result = raf.readUTF();
                    raf.close();
                } catch (IOException e2) {
                    e2.printStackTrace();
                    if (raf != null) {
                        raf.close();
                    }
                }
            }
            return result;
        } catch (Throwable th) {
            if (raf != null) {
                try {
                    raf.close();
                } catch (IOException e3) {
                    e3.printStackTrace();
                }
            }
            throw th;
        }
    }

    private void checkPermissionByUid(int uid) {
        int callingUid = Binder.getCallingUid();
        if (UserHandle.getAppId(callingUid) != uid) {
            throw new SecurityException("no permission to read file for UID:" + callingUid);
        }
    }

    private void checkWakePathPermission() {
        this.mContext.enforcePermission("android.permission.UPDATE_APP_OPS_STATS", Binder.getCallingPid(), Binder.getCallingUid(), null);
    }

    public void pushWakePathData(int wakeType, ParceledListSlice wakePathRuleInfos, int userId) {
        checkWakePathPermission();
        WakePathChecker.getInstance().pushWakePathRuleInfos(wakeType, wakePathRuleInfos.getList(), userId);
    }

    public void pushWakePathWhiteList(List<String> wakePathWhiteList, int userId) {
        checkWakePathPermission();
        WakePathChecker.getInstance().pushWakePathWhiteList(wakePathWhiteList, userId);
    }

    public void pushWakePathConfirmDialogWhiteList(int type, List<String> whiteList) {
        checkWakePathPermission();
        WakePathChecker.getInstance().pushWakePathConfirmDialogWhiteList(type, whiteList);
    }

    public void removeWakePathData(int userId) {
        checkWakePathPermission();
        WakePathChecker.getInstance().removeWakePathData(userId);
    }

    public void setTrackWakePathCallListLogEnabled(boolean enabled) {
        checkWakePathPermission();
        WakePathChecker.getInstance().setTrackWakePathCallListLogEnabled(enabled);
    }

    public ParceledListSlice getWakePathCallListLog() {
        checkWakePathPermission();
        return WakePathChecker.getInstance().getWakePathCallListLog();
    }

    public void registerWakePathCallback(IWakePathCallback callback) {
        checkWakePathPermission();
        WakePathChecker.getInstance().registerWakePathCallback(callback);
    }

    public boolean checkAllowStartActivity(String callerPkgName, String calleePkgName, Intent intent, int callerUid, int calleeUid) {
        if (!this.mUserManager.exists(calleeUid)) {
            return true;
        }
        boolean ret = this.mAccessController.filterIntentLocked(true, calleePkgName, intent);
        if (PreloadedAppPolicy.isProtectedDataApp(this.mContext, callerPkgName, 0) || PreloadedAppPolicy.isProtectedDataApp(this.mContext, calleePkgName, 0)) {
            return true;
        }
        if (!ret) {
            return WakePathChecker.getInstance().checkAllowStartActivity(callerPkgName, calleePkgName, callerUid, calleeUid);
        }
        return ret;
    }

    public int getAppPermissionControlOpen(int userId) {
        UserState userState;
        if (this.mUserManager.exists(userId) && (userState = getUserStateOrNullUnLocked(userId)) != null) {
            return userState.mAppPermissionControlStatus;
        }
        return 1;
    }

    public void setAppPermissionControlOpen(int status) {
        this.mContext.enforcePermission("android.permission.UPDATE_APP_OPS_STATS", Binder.getCallingPid(), Binder.getCallingUid(), null);
        int callingUserId = UserHandle.getCallingUserId();
        synchronized (this.mUserStateLock) {
            UserState userState = getUserStateLocked(callingUserId);
            userState.mAppPermissionControlStatus = status;
        }
    }

    public int getCurrentUserId() {
        return ProcessUtils.getCurrentUserId();
    }

    public int getSysAppCracked() {
        return this.mSysAppCracked;
    }

    public void grantInstallPermission(String packageName, String name) {
        checkPermission();
        if (!"android.permission.CAPTURE_AUDIO_OUTPUT".equals(name)) {
            throw new IllegalArgumentException("not support permssion : " + name);
        }
        Log.e(TAG, "Unimplement grantInstallPermission in Android S.");
    }

    private void updateXSpaceSettings() {
        synchronized (this.mUserStateLock) {
            if (ConfigUtils.isSupportXSpace() && !this.mIsUpdated) {
                UserState userState = getUserStateLocked(0);
                UserState userStateXSpace = getUserStateLocked(999);
                Set<Map.Entry<String, PackageSetting>> packagesSet = userState.mPackages.entrySet();
                for (Map.Entry<String, PackageSetting> entrySet : packagesSet) {
                    String name = entrySet.getKey();
                    if (!TextUtils.isEmpty(name) && XSpaceUserHandle.isAppInXSpace(this.mContext, name)) {
                        PackageSetting value = entrySet.getValue();
                        PackageSetting psXSpace = new PackageSetting(name);
                        psXSpace.accessControl = value.accessControl;
                        psXSpace.childrenControl = value.childrenControl;
                        userStateXSpace.mPackages.put(name, psXSpace);
                    }
                }
                scheduleWriteSettings();
                updateMaskObserverValues();
            }
        }
    }

    private boolean isPackageAccessControlPass(UserState userState) {
        if (!ConfigUtils.isSupportXSpace() || !(userState.userHandle == 999 || userState.userHandle == 0)) {
            return userState.mAccessControlPassPackages.size() > 0;
        }
        UserState userStateOwner = getUserStateLocked(0);
        UserState userStateXSpace = getUserStateLocked(999);
        return userStateOwner.mAccessControlPassPackages.size() + userStateXSpace.mAccessControlPassPackages.size() > 0;
    }

    private UserState changeUserState(UserState userState) {
        int useId = userState.userHandle == 999 ? 0 : userState.userHandle;
        return getUserStateLocked(useId);
    }

    private void clearPassPackages(int userId) {
        if (ConfigUtils.isSupportXSpace() && (userId == 0 || 999 == userId)) {
            UserState userStateOwner = getUserStateLocked(0);
            UserState userStateXSpace = getUserStateLocked(999);
            HashSet<String> passPackagesOwner = userStateOwner.mAccessControlPassPackages;
            HashSet<String> passPackagesXSpace = userStateXSpace.mAccessControlPassPackages;
            passPackagesOwner.clear();
            passPackagesXSpace.clear();
        } else {
            HashSet<String> passPackages = getUserStateLocked(userId).mAccessControlPassPackages;
            passPackages.clear();
        }
        updateMaskObserverValues();
    }

    public boolean isRestrictedAppNet(String packageName) {
        return RestrictAppNetManager.isRestrictedAppNet(this.mContext, packageName);
    }

    public boolean writeAppHideConfig(boolean hide) {
        checkPermission();
        return false;
    }

    private boolean saveIconInner(String fileName, Bitmap icon) {
        boolean allowSaveIcon = allowSaveIconCache();
        if (allowSaveIcon) {
            String path = ThemeRuntimeManager.createTempIconFile(this.mContext, fileName, icon);
            return moveIconInner(path);
        }
        return false;
    }

    private boolean moveIconInner(String srcIconPath) {
        boolean ret = false;
        if (!TextUtils.isEmpty(srcIconPath)) {
            String fileName = getFileName(srcIconPath);
            String destPath = IconCustomizer.CUSTOMIZED_ICON_PATH + fileName;
            ret = ThemeNativeUtils.copy(srcIconPath, destPath);
            if (ret) {
                ret = ThemeNativeUtils.updateFilePermissionWithThemeContext(destPath);
            }
            IconCustomizer.ensureMiuiVersionFlagExist(this.mContext);
            ThemeNativeUtils.remove(srcIconPath);
        }
        return ret;
    }

    private static String getFileName(String path) {
        if (path == null || path.length() == 0) {
            return "";
        }
        int index = path.lastIndexOf(File.separatorChar);
        if (index > -1) {
            return path.substring(index + 1);
        }
        return path;
    }

    private boolean allowSaveIconCache() {
        return isSystemApp() && (UserHandle.getAppId(Binder.getCallingUid()) == 6101 || canSaveExternalIconCache());
    }

    private boolean isSystemApp() {
        try {
            int uid = Binder.getCallingUid();
            PackageManager pm = this.mContext.getPackageManager();
            String packageName = pm.getPackagesForUid(uid)[0];
            ApplicationInfo applicationInfo = pm.getApplicationInfo(packageName, 0);
            return (applicationInfo.flags & 1) != 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean canSaveExternalIconCache() {
        long iconModifiedTime = Libcore_Os_getFileLastStatusChangedTime(IconCustomizer.CUSTOMIZED_ICON_PATH + "miui_version");
        return System.currentTimeMillis() - iconModifiedTime > LOCK_TIME_OUT;
    }

    public static long Libcore_Os_getFileLastStatusChangedTime(String path) {
        try {
            Class<?> clazz = Class.forName("libcore.io.Libcore");
            Field osField = clazz.getField("os");
            osField.setAccessible(true);
            Object osObject = osField.get(clazz);
            Method lstatMethod = osObject.getClass().getMethod("lstat", String.class);
            lstatMethod.setAccessible(true);
            Object structStatObject = lstatMethod.invoke(osObject, path);
            Field ctimeField = structStatObject.getClass().getField("st_ctime");
            ctimeField.setAccessible(true);
            Object ctimeObject = ctimeField.get(structStatObject);
            return ((Long) ctimeObject).longValue() * 1000;
        } catch (Exception e) {
            Log.e(TAG, "getFileChangeTime fail :" + e);
            return -1L;
        }
    }

    public boolean addMiuiFirewallSharedUid(int uid) {
        checkPermissionByUid(1000);
        return MiuiNetworkManagementService.getInstance().addMiuiFirewallSharedUid(uid);
    }

    public boolean setMiuiFirewallRule(String packageName, int uid, int rule, int type) {
        checkPermissionByUid(1000);
        return MiuiNetworkManagementService.getInstance().setMiuiFirewallRule(packageName, uid, rule, type);
    }

    public boolean setCurrentNetworkState(int state) {
        checkPermissionByUid(1000);
        return MiuiNetworkManagementService.getInstance().setCurrentNetworkState(state);
    }

    public void setIncompatibleAppList(List<String> list) {
        checkPermission();
        if (list == null) {
            throw new NullPointerException("List is null");
        }
        synchronized (this.mIncompatibleAppList) {
            this.mIncompatibleAppList.clear();
            this.mIncompatibleAppList.addAll(list);
        }
    }

    public List<String> getIncompatibleAppList() {
        ArrayList arrayList;
        synchronized (this.mIncompatibleAppList) {
            arrayList = new ArrayList(this.mIncompatibleAppList);
        }
        return arrayList;
    }

    public ParceledListSlice getWakePathComponents(String packageName) {
        checkWakePathPermission();
        List<WakePathComponent> ret = PackageManagerServiceCompat.getWakePathComponents(packageName);
        if (ret == null) {
            return null;
        }
        return new ParceledListSlice(ret);
    }

    public void offerGoogleBaseCallBack(final ISecurityCallback cb) {
        checkPermission();
        this.sGoogleBaseService = cb;
        try {
            cb.asBinder().linkToDeath(new IBinder.DeathRecipient() { // from class: com.miui.server.SecurityManagerService.2
                @Override // android.os.IBinder.DeathRecipient
                public void binderDied() {
                    cb.asBinder().unlinkToDeath(this, 0);
                    SecurityManagerService.this.sGoogleBaseService = null;
                    Slog.d(SecurityManagerService.TAG, "securitycenter died, reset handle to null");
                }
            }, 0);
        } catch (Exception e) {
            Log.e(TAG, "offerGoogleBaseCallBack", e);
        }
    }

    public void notifyAppsPreInstalled() {
        checkPermission();
        synchronized (this.mRegistrantLock) {
            this.mAppsPreInstallRegistrant.notifyRegistrants();
            for (int i = this.mAppsPreInstallRegistrant.size() - 1; i >= 0; i--) {
                ((Registrant) this.mAppsPreInstallRegistrant.get(i)).clear();
            }
            this.mAppsPreInstallRegistrant.removeCleared();
        }
    }

    public void registerForAppsPreInstalled(Handler h, int what, Object obj) {
        synchronized (this.mRegistrantLock) {
            if (this.mAppsPreInstallRegistrant.size() == 0) {
                Registrant r = new Registrant(h, what, obj);
                this.mAppsPreInstallRegistrant.add(r);
            }
        }
    }

    public ISecurityCallback getGoogleBaseService() {
        return this.sGoogleBaseService;
    }

    public boolean areNotificationsEnabledForPackage(String packageName, int uid) throws RemoteException {
        checkPermission();
        if (this.mINotificationManager == null) {
            this.mINotificationManager = INotificationManager.Stub.asInterface(ServiceManager.getService("notification"));
        }
        long identity = Binder.clearCallingIdentity();
        try {
            return this.mINotificationManager.areNotificationsEnabledForPackage(packageName, uid);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void setNotificationsEnabledForPackage(String packageName, int uid, boolean enabled) throws RemoteException {
        checkPermission();
        if (this.mINotificationManager == null) {
            this.mINotificationManager = INotificationManager.Stub.asInterface(ServiceManager.getService("notification"));
        }
        long identity = Binder.clearCallingIdentity();
        try {
            this.mINotificationManager.setNotificationsEnabledForPackage(packageName, uid, enabled);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public boolean isAppHide() {
        return PackageHideManager.getInstance(false).isAppHide();
    }

    public boolean isFunctionOpen() {
        return PackageHideManager.getInstance(false).isFunctionOpen();
    }

    public boolean setAppHide(boolean hide) {
        return PackageHideManager.getInstance(false).setHideApp(this.mContext, hide);
    }

    public boolean isValidDevice() {
        return PackageHideManager.isValidDevice();
    }

    private void checkWriteSecurePermission() {
        this.mContext.enforceCallingPermission("android.permission.WRITE_SECURE_SETTINGS", "Permission Denial: attempt to change application privacy revoke state from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
    }

    public void setAppPrivacyStatus(String packageName, boolean isOpen) {
        if (TextUtils.isEmpty(packageName)) {
            throw new RuntimeException("packageName can not be null or empty");
        }
        int callingPid = Binder.getCallingPid();
        String callingPackageName = ProcessUtils.getPackageNameByPid(callingPid);
        if (!"com.android.settings".equals(callingPackageName) && !packageName.equals(callingPackageName)) {
            checkWriteSecurePermission();
        }
        long identity = Binder.clearCallingIdentity();
        try {
            Settings.Secure.putInt(this.mContext.getContentResolver(), "privacy_status_" + packageName, isOpen ? 1 : 0);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public boolean isAppPrivacyEnabled(String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            throw new RuntimeException("packageName can not be null or empty");
        }
        long identity = Binder.clearCallingIdentity();
        try {
            boolean z = true;
            if (Settings.Secure.getInt(this.mContext.getContentResolver(), "privacy_status_" + packageName, 1) == 0) {
                z = false;
            }
            return z;
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public boolean isAllowStartService(Intent service, int userId) {
        checkPermission();
        return AutoStartManagerService.getInstance().isAllowStartService(this.mContext, service, userId);
    }

    public IBinder getTopActivity() {
        checkPermission();
        HashMap<String, Object> topActivity = WindowProcessUtils.getTopRunningActivityInfo();
        if (topActivity != null) {
            Intent intent = (Intent) topActivity.get("intent");
            ComponentName componentName = intent.getComponent();
            if (componentName != null) {
                String clsName = componentName.getClassName();
                String pkgName = componentName.getPackageName();
                if ("com.google.android.packageinstaller".equals(pkgName)) {
                    if ("com.android.packageinstaller.InstallAppProgress".equals(clsName) || "com.android.packageinstaller.InstallSuccess".equals(clsName) || !"com.android.packageinstaller.PackageInstallerActivity".equals(clsName)) {
                        IBinder token = (IBinder) topActivity.get("token");
                        return token;
                    }
                    return null;
                }
                return null;
            }
            return null;
        }
        return null;
    }

    public IBinder getAppRunningControlIBinder() {
        return this.mAppRunningControlBinder;
    }

    public static AppRunningControlService getAppRunningControlService() {
        return mAppRunningControlService;
    }

    public void watchGreenGuardProcess() {
        GreenGuardManagerService.startWatchGreenguardProcess(this.mContext);
    }

    public int getSecondSpaceId() {
        long callingId = Binder.clearCallingIdentity();
        try {
            return Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "second_user_id", ScreenRotationAnimationImpl.BLACK_SURFACE_INVALID_POSITION, 0);
        } finally {
            Binder.restoreCallingIdentity(callingId);
        }
    }

    public int moveTaskToStack(int taskId, int stackId, boolean toTop) {
        if (toTop) {
            long callingId = Binder.clearCallingIdentity();
            try {
                Settings.Secure.putIntForUser(this.mContext.getContentResolver(), "quick_reply", 0, -2);
                return -1;
            } finally {
                Binder.restoreCallingIdentity(callingId);
            }
        }
        return -1;
    }

    public void setStickWindowName(String component) {
    }

    public boolean getStickWindowName(String component) {
        return false;
    }

    public int resizeTask(int taskId, Rect bounds, int resizeMode) {
        return -1;
    }

    public void pushUpdatePkgsData(List<String> updatePkgsList, boolean enable) {
        checkWakePathPermission();
        WakePathChecker.getInstance().pushUpdatePkgsData(updatePkgsList, enable);
    }

    public void setPrivacyApp(String packageName, int userId, boolean isPrivacy) {
        checkPermission();
        synchronized (this.mUserStateLock) {
            UserState userStateLocked = getUserStateLocked(userId);
            PackageSetting ps = getPackageSetting(userStateLocked.mPackages, packageName);
            ps.isPrivacyApp = isPrivacy;
            scheduleWriteSettings();
        }
    }

    public boolean isPrivacyApp(String packageName, int userId) {
        boolean z;
        checkPermission();
        synchronized (this.mUserStateLock) {
            UserState userState = getUserStateLocked(userId);
            try {
                PackageSetting ps = getPackageSetting(userState.mPackages, packageName);
                z = ps.isPrivacyApp;
            } catch (Exception e) {
                Log.e(TAG, "isPrivacyApp error", e);
                return false;
            }
        }
        return z;
    }

    public List<String> getAllPrivacyApps(int userId) {
        List<String> privacyAppsList;
        checkPermission();
        synchronized (this.mUserStateLock) {
            privacyAppsList = new ArrayList<>();
            UserState userState = getUserStateLocked(userId);
            HashMap<String, PackageSetting> packages = userState.mPackages;
            Set<String> pkgNames = packages.keySet();
            for (String pkgName : pkgNames) {
                try {
                    PackageSetting ps = getPackageSetting(userState.mPackages, pkgName);
                    if (ps.isPrivacyApp) {
                        privacyAppsList.add(pkgName);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "getAllPrivacyApps error", e);
                }
            }
        }
        return privacyAppsList;
    }

    public void updateLauncherPackageNames() {
        WakePathChecker.getInstance().init(this.mContext);
    }

    private void checkGrantPermissionPkg() {
        String callingPackageName = ProcessUtils.getPackageNameByPid(Binder.getCallingPid());
        if (!"com.lbe.security.miui".equals(callingPackageName)) {
            throw new SecurityException("Permission Denial: attempt to grant/revoke permission from pid=" + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid() + ", pkg=" + callingPackageName);
        }
    }

    public void grantRuntimePermissionAsUser(String packageName, String permName, int userId) {
        checkGrantPermissionPkg();
        if (PermissionManager.checkPackageNamePermission(permName, packageName, userId) == 0) {
            return;
        }
        long identity = Binder.clearCallingIdentity();
        try {
            this.mPermissionManagerService.grantRuntimePermission(packageName, permName, userId);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void revokeRuntimePermissionAsUser(String packageName, String permName, int userId) {
        checkGrantPermissionPkg();
        if (PermissionManager.checkPackageNamePermission(permName, packageName, userId) != 0) {
            return;
        }
        long identity = Binder.clearCallingIdentity();
        try {
            this.mPermissionManagerService.revokeRuntimePermission(packageName, permName, userId, "");
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void revokeRuntimePermissionAsUserNotKill(String packageName, String permName, int userId) {
        checkGrantPermissionPkg();
        if (PermissionManager.checkPackageNamePermission(permName, packageName, userId) != 0) {
            return;
        }
        long identity = Binder.clearCallingIdentity();
        try {
            this.mPermissionManagerService.revokeRuntimePermissionNotKill(packageName, permName, userId);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public int getPermissionFlagsAsUser(String permName, String packageName, int userId) {
        checkGrantPermissionPkg();
        long identity = Binder.clearCallingIdentity();
        try {
            return this.mPermissionManagerService.getPermissionFlags(packageName, permName, userId);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void updatePermissionFlagsAsUser(String permissionName, String packageName, int flagMask, int flagValues, int userId) {
        checkGrantPermissionPkg();
        long identity = Binder.clearCallingIdentity();
        try {
            try {
                this.mPermissionManagerService.updatePermissionFlags(packageName, permissionName, flagMask, flagValues, true, userId);
            } catch (Exception e) {
                Log.e(TAG, "updatePermissionFlagsAsUser failed: perm=" + permissionName + ", pkg=" + packageName + ", mask=" + flagMask + ", value=" + flagValues, e);
            }
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    public void updateLedStatus(boolean on) {
        checkPermission();
        LogicalLight logicalLight = this.mLedLight;
        if (logicalLight == null) {
            Log.i(TAG, "updateLightsLocked mLedLight cannot assess");
            return;
        }
        if (on) {
            logicalLight.setColor(this.mLightOn);
        } else {
            logicalLight.turnOff();
        }
        Log.i(TAG, "updateLightsLocked " + on + " , calling pid= " + Binder.getCallingPid() + ", uid=" + Binder.getCallingUid());
    }

    public void exemptTemporarily(String packageName) {
        checkPermission();
        PendingIntentRecordImpl.exemptTemporarily(packageName, false);
    }

    public void setGameStorageApp(String packageName, int userId, boolean isStorage) {
        checkPermission();
        synchronized (this) {
            UserState userStateLocked = getUserStateLocked(userId);
            PackageSetting ps = getPackageSetting(userStateLocked.mPackages, packageName);
            ps.isGameStorageApp = isStorage;
            scheduleWriteSettings();
        }
    }

    public boolean isGameStorageApp(String packageName, int userId) {
        boolean z;
        checkPermission();
        synchronized (this) {
            UserState userState = getUserStateLocked(userId);
            try {
                PackageSetting ps = getPackageSetting(userState.mPackages, packageName);
                z = ps.isGameStorageApp;
            } catch (Exception e) {
                Log.e(TAG, "get app is game stroage failed", e);
                return false;
            }
        }
        return z;
    }

    public List<String> getAllGameStorageApps(int userId) {
        List<String> storageAppsList;
        checkPermission();
        synchronized (this) {
            storageAppsList = new ArrayList<>();
            UserState userState = getUserStateLocked(userId);
            HashMap<String, PackageSetting> packages = userState.mPackages;
            Set<String> pkgNames = packages.keySet();
            for (String pkgName : pkgNames) {
                try {
                    PackageSetting ps = getPackageSetting(userState.mPackages, pkgName);
                    if (ps.isGameStorageApp) {
                        storageAppsList.add(pkgName);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "get game storage all apps failed", e);
                }
            }
        }
        return storageAppsList;
    }

    public void registerLocationBlurryManager(ILocationBlurry manager) {
        checkWakePathPermission();
        MiuiBlurLocationManagerStub.get().registerLocationBlurryManager(manager);
    }

    public CellIdentity getBlurryCellLocation(CellIdentity location) {
        checkBlurLocationPermission();
        return MiuiBlurLocationManagerStub.get().getBlurryCellLocation(location);
    }

    public List<CellInfo> getBlurryCellInfos(List<CellInfo> location) {
        checkBlurLocationPermission();
        return MiuiBlurLocationManagerStub.get().getBlurryCellInfos(location);
    }

    private void checkBlurLocationPermission() {
        int callingUid = Binder.getCallingUid();
        if (UserHandle.getAppId(callingUid) >= 10000) {
            throw new SecurityException("Uid " + callingUid + " can't get blur location info");
        }
    }

    private void scheduleForMaskObserver(String pkg, int userHandle) {
        String token = pkg + "_" + userHandle;
        this.mHandler.removeCallbacksAndEqualMessages(token);
        this.mHandler.postDelayed(new Runnable() { // from class: com.miui.server.SecurityManagerService$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                SecurityManagerService.this.updateMaskObserverValues();
            }
        }, token, LOCK_TIME_OUT);
    }

    public void updateMaskObserverValues() {
        long origId = Binder.clearCallingIdentity();
        try {
            int oldValue = Settings.Secure.getInt(this.mContext.getContentResolver(), APPLOCK_MASK_NOTIFY, 0);
            Settings.Secure.putInt(this.mContext.getContentResolver(), APPLOCK_MASK_NOTIFY, oldValue ^ 1);
        } catch (Exception e) {
            Log.e(TAG, "write setting secure failed.", e);
        }
        Binder.restoreCallingIdentity(origId);
    }

    public String getShouldMaskApps() {
        String jSONArray;
        checkPermissionByUid(1000);
        synchronized (this.mUserStateLock) {
            try {
                try {
                    JSONArray maskArray = new JSONArray();
                    for (int i = 0; i < this.mUserStates.size(); i++) {
                        UserState userState = this.mUserStates.valueAt(i);
                        JSONObject userStateObj = new JSONObject();
                        userStateObj.put("userId", userState.userHandle);
                        JSONArray itemArray = new JSONArray();
                        boolean enabled = userState.mAccessControlEnabled;
                        if (userState.userHandle == 999) {
                            enabled = getUserStateLocked(0).mAccessControlEnabled;
                        }
                        if (enabled) {
                            for (PackageSetting ps : userState.mPackages.values()) {
                                if (!TextUtils.isEmpty(ps.name) && ps.accessControl && ps.maskNotification && !checkAccessControlPassLockedCore(userState, ps.name, null)) {
                                    itemArray.put(ps.name);
                                }
                            }
                        }
                        userStateObj.put("shouldMaskApps", itemArray);
                        maskArray.put(userStateObj);
                    }
                    jSONArray = maskArray.toString();
                } catch (Exception e) {
                    Log.e(TAG, "getShouldMaskApps failed. ", e);
                    return null;
                }
            } catch (Throwable th) {
                throw th;
            }
        }
        return jSONArray;
    }

    public String getPlatformVAID() {
        if (UserHandle.getAppId(Binder.getCallingUid()) > 10000) {
            this.mContext.enforceCallingOrSelfPermission(PLATFORM_VAID_PERMISSION, "Not allowed get platform vaid from other!");
        }
        long identity = Binder.clearCallingIdentity();
        try {
            if (TextUtils.isEmpty(this.mPlatformVAID)) {
                this.mPlatformVAID = readSystemDataStringFile(VAID_PLATFORM_CACHE_PATH);
            }
            Binder.restoreCallingIdentity(identity);
            return this.mPlatformVAID;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(identity);
            throw th;
        }
    }

    public void pushPrivacyVirtualDisplayList(List<String> privacyList) {
        checkPermission();
        this.mPrivacyVirtualDisplay.clear();
        this.mPrivacyVirtualDisplay.addAll(privacyList);
        Log.i(TAG, "privacy virtual display updated! size=" + this.mPrivacyVirtualDisplay.size());
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class LocalService extends SecurityManagerInternal {
        public static final int DISPLAY_DEVICE_EVENT_ADDED = 1;
        public static final int DISPLAY_DEVICE_EVENT_CHANGED = 2;
        public static final int DISPLAY_DEVICE_EVENT_REMOVED = 3;

        private LocalService() {
            SecurityManagerService.this = r1;
        }

        public void onDisplayDeviceEvent(String packageName, String name, IBinder token, int event) {
            if (SecurityManagerService.this.mPrivacyVirtualDisplay.contains(packageName) && 1 == event) {
                SecurityManagerService.this.mPrivacyDisplayNameList.add(name);
                SurfaceControl.setMiSecurityDisplay(token, true);
            }
            if (3 == event && SecurityManagerService.this.mPrivacyDisplayNameList.remove(name)) {
                SurfaceControl.setMiSecurityDisplay(token, false);
            }
        }
    }
}
