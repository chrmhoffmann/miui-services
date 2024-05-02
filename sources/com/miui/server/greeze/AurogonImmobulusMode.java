package com.miui.server.greeze;

import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.util.Slog;
import com.android.server.LocalServices;
import com.android.server.MiuiBatteryStatsService;
import com.android.server.am.ActivityManagerServiceStub;
import com.android.server.am.ProcessManagerService;
import com.android.server.inputmethod.InputMethodManagerServiceImpl;
import com.miui.server.AccessController;
import com.miui.server.SplashScreenServiceDelegate;
import com.miui.server.input.util.MiuiCustomizeShortCutUtils;
import com.miui.server.process.ProcessManagerInternal;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/* loaded from: classes.dex */
public class AurogonImmobulusMode {
    private static final String BLUETOOTH_GLOBAL_SETTING = "RECORD_BLE_APPNAME";
    private static final String IMMOBULUS_GAME_CONTROLLER = "com.xiaomi.joyose.action.GAME_STATUS_UPDATE";
    private static final String IMMOBULUS_GAME_KEY_ALLOW_LIST = "com.xiaomi.joyose.key.BACKGROUND_FREEZE_WHITELIST";
    private static final String IMMOBULUS_GAME_KEY_STATUS = "com.xiaomi.joyose.key.GAME_STATUS";
    private static final int IMMOBULUS_GAME_VALUE_QUIT = 2;
    private static final int IMMOBULUS_GAME_VALUE_TRIGGER = 1;
    private static final int IMMOBULUS_LEVEL_FORCESTOP = 3;
    private static final int IMMOBULUS_REPEAT_TIME = 2000;
    private static final String IMMOBULUS_SWITCH_SECURE_SETTING = "immobulus_mode_switch_restrict";
    public static final int MSG_IMMOBULUS_MODE_QUIT_ACTION = 102;
    public static final int MSG_IMMOBULUS_MODE_TRIGGER_ACTION = 101;
    public static final int MSG_LAUNCH_MODE_QUIT_ACTION = 106;
    public static final int MSG_LAUNCH_MODE_TRIGGER_ACTION = 105;
    public static final int MSG_REMOVE_ALL_MESSAGE = 104;
    public static final int MSG_REPEAT_FREEZE_APP = 103;
    public static final String TAG = "AurogonImmobulusMode";
    private static final long TIME_FREEZE_DURATION_UNUSEFUL = 20000;
    private static final long TIME_FREEZE_DURATION_USEFUL = 60000;
    private static final long TIME_LAUNCH_MODE_TIMEOUT = 3000;
    public static final int TYPE_MODE_IMMOBULUS = 8;
    public static final int TYPE_MODE_LAUNCH = 16;
    public Context mContext;
    public GreezeManagerService mGreezeService;
    public ImmobulusHandler mHandler;
    public boolean mImmobulusModeEnabled;
    public boolean mLaunchModeEnabled;
    private PackageManager mPm;
    public ProcessManagerInternal mProcessManagerInternal;
    public ProcessManagerService mProcessManagerService;
    private SettingsObserver mSettingsObserver;
    private static final String AUROGON_IMMOBULUS_SWITCH_PROPERTY = "persist.sys.aurogon.immobulus";
    public static final boolean IMMOBULUS_ENABLED = SystemProperties.getBoolean(AUROGON_IMMOBULUS_SWITCH_PROPERTY, true);
    public static final boolean CN_MODEL = "CN".equals(SystemProperties.get("ro.miui.region", "unknown"));
    private static List<String> REASON_FREEZE = new ArrayList(Arrays.asList("IMMOBULUS", "LAUNCH_MODE"));
    private static List<String> REASON_UNFREEZE = new ArrayList(Arrays.asList("SYNC_BINDER", "ASYNC_BINDER", "PACKET", "BINDER_SERVICE", "SIGNAL", "BROADCAST"));
    public static List<String> mMessageApp = new ArrayList(Arrays.asList("com.tencent.mobileqq", "com.tencent.mm", "com.alibaba.android.rimet", "com.ss.android.lark", "com.ss.android.lark.kami", "com.tencent.wework"));
    public List<String> mImmobulusAllowList = null;
    public List<AurogonAppInfo> mImmobulusTargetList = null;
    public Map<Integer, List<Integer>> mFgServiceAppList = null;
    public List<Integer> mVPNAppList = null;
    public List<AurogonAppInfo> mQuitImmobulusList = null;
    public List<AurogonAppInfo> mQuitLaunchModeList = null;
    public List<Integer> mDownloadAppList = null;
    public List<String> mBluetoothUsingList = null;
    public boolean mEnterImmobulusMode = false;
    public boolean mLastBarExpandIMStatus = false;
    public boolean mVpnConnect = false;
    public boolean mEnabledLMCamera = true;
    public boolean mEnterIMCamera = false;
    public int mCameraUid = -1;
    public boolean mCtsModeOn = false;
    public ConnectivityManager mConnMgr = null;
    public String mCurrentIMEPacageName = "";
    public String mLastPackageName = "";
    public String mWallPaperPackageName = "";
    private List<String> mAllowList = new ArrayList(Arrays.asList("android.uid.shared", SplashScreenServiceDelegate.SPLASHSCREEN_PACKAGE, "com.android.providers.media.module", "com.google.android.webview", "com.miui.voicetrigger", "com.miui.voiceassist", "com.dewmobile.kuaiya", "com.android.permissioncontroller", "com.android.htmlviewer", "com.google.android.providers.media.module", "com.android.incallui", "org.codeaurora.ims", "com.android.providers.contacts", "com.xiaomi.xaee", "com.android.calllogbackup", "com.android.providers.blockednumber", "com.android.providers.userdictionary", "com.xiaomi.aireco", "com.miui.securityinputmethod", InputMethodManagerServiceImpl.MIUI_HOME, "com.miui.newhome", "com.miui.screenshot", "com.lbe.security.miui", "org.ifaa.aidl.manager", "com.xiaomi.macro", "com.miui.rom", "com.miui.personalassistant", MiuiBatteryStatsService.TrackBatteryUsbInfo.ANALYTICS_PACKAGE, "com.miui.mediaviewer", "com.xiaomi.gamecenter.sdk.service"));
    private List<String> mCloudAllowList = null;
    private List<String> mImportantAppList = new ArrayList(Arrays.asList(InputMethodManagerServiceImpl.MIUI_HOME, AccessController.PACKAGE_CAMERA, "com.goodix.fingerprint.setting", AccessController.PACKAGE_GALLERY));
    private List<String> CTS_NAME = new ArrayList(Arrays.asList("android.net.cts", "com.android.cts.verifier"));
    public List<Integer> mOnWindowsAppList = new ArrayList();

    public AurogonImmobulusMode(Context context, HandlerThread ht, GreezeManagerService service) {
        this.mContext = null;
        this.mImmobulusModeEnabled = true;
        this.mLaunchModeEnabled = true;
        this.mHandler = null;
        this.mGreezeService = null;
        this.mSettingsObserver = null;
        this.mPm = null;
        this.mProcessManagerService = null;
        this.mProcessManagerInternal = null;
        this.mContext = context;
        if (!IMMOBULUS_ENABLED) {
            this.mImmobulusModeEnabled = false;
            this.mLaunchModeEnabled = false;
            return;
        }
        init();
        this.mHandler = new ImmobulusHandler(ht.getLooper());
        this.mPm = this.mContext.getPackageManager();
        this.mGreezeService = service;
        this.mSettingsObserver = new SettingsObserver(this.mHandler);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor(BLUETOOTH_GLOBAL_SETTING), false, this.mSettingsObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(IMMOBULUS_SWITCH_SECURE_SETTING), false, this.mSettingsObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("default_input_method"), false, this.mSettingsObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(MiuiSettings.Secure.MIUI_OPTIMIZATION), false, this.mSettingsObserver, -2);
        this.mProcessManagerService = (ProcessManagerService) ServiceManager.getService("ProcessManager");
        this.mProcessManagerInternal = (ProcessManagerInternal) LocalServices.getService(ProcessManagerInternal.class);
        new ImmobulusBroadcastReceiver();
        updateCloudAllowList();
        updateIMEAppStatus();
        getConnectivityManager();
    }

    private void init() {
        this.mImmobulusAllowList = new ArrayList();
        this.mImmobulusTargetList = new ArrayList();
        this.mBluetoothUsingList = new ArrayList();
        this.mQuitImmobulusList = new ArrayList();
        this.mDownloadAppList = new ArrayList();
        this.mQuitLaunchModeList = new ArrayList();
        this.mCloudAllowList = new ArrayList();
        this.mFgServiceAppList = new HashMap();
        this.mVPNAppList = new ArrayList();
        if (this.mLaunchModeEnabled) {
            boolean z = !SystemProperties.getBoolean("persist.sys.miui_optimization", true);
            this.mCtsModeOn = z;
            if (z) {
                this.mLaunchModeEnabled = false;
            }
        }
    }

    public void TriggerImmobulusModeAction() {
        StringBuilder log = new StringBuilder();
        log.append("IM FZ uid = [");
        updateAppsOnWindowsStatus();
        synchronized (this.mImmobulusTargetList) {
            for (AurogonAppInfo app : this.mImmobulusTargetList) {
                if (!this.mImmobulusAllowList.contains(app.mPackageName) && this.mGreezeService.isAppRunning(app.mUid)) {
                    if (this.mGreezeService.isAppRunningInFg(app.mUid)) {
                        Slog.d(TAG, " uid = " + app.mUid + "is running in FG, skip check!");
                    } else if (this.mGreezeService.isUidFrozen(app.mUid)) {
                        this.mGreezeService.updateFrozenInfoForImmobulus(app.mUid, 8);
                    } else {
                        List<Integer> list = this.mOnWindowsAppList;
                        if (list != null && list.contains(Integer.valueOf(app.mUid))) {
                            if (GreezeManagerService.DEBUG) {
                                Slog.d(TAG, " uid = " + app.mUid + " is show on screen, skip it!");
                            }
                        } else if (app.hasIcon && !this.mAllowList.contains(app.mPackageName)) {
                            if (!checkAppStatusForFreeze(app)) {
                                repeatCheckAppForImmobulusMode(app);
                            } else {
                                boolean success = freezeActionForImmobulus(app, "IMMOBULUS");
                                if (success) {
                                    log.append(" " + app.mUid);
                                }
                            }
                        }
                    }
                }
            }
        }
        log.append("]");
        this.mGreezeService.addToDumpHistory(log.toString());
    }

    public void QuitImmobulusModeAction() {
        removeAllMsg();
        StringBuilder log = new StringBuilder();
        log.append("IM finish THAW uid = [");
        for (AurogonAppInfo app : this.mQuitImmobulusList) {
            boolean success = unFreezeActionForImmobulus(app, "IMMOBULUS");
            if (success) {
                log.append(" " + app.mUid);
            }
        }
        log.append("]");
        this.mGreezeService.addToDumpHistory(log.toString());
        this.mGreezeService.resetStatusForImmobulus(8);
        this.mEnterImmobulusMode = false;
    }

    public void checkAppForImmobulusMode(AurogonAppInfo app) {
        if (!this.mEnterImmobulusMode || this.mGreezeService.isAppRunningInFg(app.mUid) || this.mGreezeService.isUidFrozen(app.mUid) || this.mImmobulusAllowList.contains(app.mPackageName) || !this.mGreezeService.isAppRunning(app.mUid)) {
            return;
        }
        if (!checkAppStatusForFreeze(app)) {
            repeatCheckAppForImmobulusMode(app);
        } else {
            freezeActionForImmobulus(app, "repeat");
        }
    }

    public void repeatCheckAppForImmobulusMode(int uid) {
        AurogonAppInfo app = getAurogonAppInfo(uid);
        if (app != null) {
            repeatCheckAppForImmobulusMode(app);
        }
    }

    public void repeatCheckAppForImmobulusMode(AurogonAppInfo app) {
        if (app == null || this.mHandler.hasMessages(MSG_REPEAT_FREEZE_APP, app) || !app.hasIcon) {
            return;
        }
        sendMeesage(MSG_REPEAT_FREEZE_APP, -1, -1, app, 2000L);
    }

    public boolean checkAppStatusForFreeze(AurogonAppInfo app) {
        if (this.mGreezeService.isAppRunningInFg(app.mUid)) {
            if (GreezeManagerService.DEBUG) {
                Slog.d(TAG, " uid = " + app.mUid + "is running in FG, skip check!");
            }
            return false;
        } else if (this.mGreezeService.isUidActive(app.mUid)) {
            if (GreezeManagerService.DEBUG) {
                Slog.d(TAG, " uid = " + app.mUid + " is using GPS/Audio/Vibrator!");
            }
            return false;
        } else if (this.mBluetoothUsingList.contains(app.mPackageName)) {
            if (GreezeManagerService.DEBUG) {
                Slog.d(TAG, " uid = " + app.mUid + " is using BT!");
            }
            return false;
        } else if (ActivityManagerServiceStub.get().isBackuping(app.mUid) || ActivityManagerServiceStub.get().isActiveInstruUid(app.mUid)) {
            if (GreezeManagerService.DEBUG) {
                Slog.d(TAG, " uid = " + app.mUid + " is using backingg or activeIns");
            }
            return false;
        } else if (this.mDownloadAppList.contains(Integer.valueOf(app.mUid))) {
            if (GreezeManagerService.DEBUG) {
                Slog.d(TAG, "uid = " + app.mUid + " is downloading!");
            }
            return false;
        } else if (this.mCurrentIMEPacageName.equals(app.mPackageName)) {
            return false;
        } else {
            if (this.mVpnConnect && isVpnApp(app.mUid)) {
                if (GreezeManagerService.DEBUG) {
                    Slog.d(TAG, "uid = " + app.mUid + " is vpn app!");
                }
                return false;
            } else if (this.mGreezeService.checkOrderBCRecivingApp(app.mUid)) {
                if (GreezeManagerService.DEBUG) {
                    Slog.d("Aurogon", "freeze app was reciving broadcast! mUid = " + app.mUid);
                }
                return false;
            } else {
                return true;
            }
        }
    }

    public boolean freezeActionForImmobulus(AurogonAppInfo app, String reason) {
        if (isSystemOrMiuiImportantApp(app) || checkAppFgService(app.mUid)) {
            this.mQuitImmobulusList.add(app);
        }
        boolean success = this.mGreezeService.freezeAction(app.mUid, 8, reason, false);
        if (success) {
            app.freezeTime = SystemClock.uptimeMillis();
        } else {
            Slog.d("Aurogon", "freeze app failed uid = " + app.mUid);
            repeatCheckAppForImmobulusMode(app);
        }
        return success;
    }

    public boolean unFreezeActionForImmobulus(AurogonAppInfo app, String reason) {
        boolean success = this.mGreezeService.thawUid(app.mUid, 8, reason);
        if (success) {
            if (this.mEnterImmobulusMode) {
                long currentTime = SystemClock.uptimeMillis();
                long durration = currentTime - app.freezeTime;
                if (durration < 20000) {
                    app.level++;
                } else if (durration > 60000) {
                    app.level--;
                }
                app.unFreezeTime = currentTime;
                if (app.level >= 3) {
                    forceStopAppForImmobulus(app.mPackageName, "frequent wakeup");
                    app.level = 0;
                    return success;
                }
                repeatCheckAppForImmobulusMode(app);
            }
        } else {
            Slog.d(TAG, " unFreeze uid = " + app.mUid + " reason : " + reason + " failed!");
        }
        return success;
    }

    public void forceStopAppForImmobulus(String packageName, String reason) {
        this.mGreezeService.forceStopPackage(packageName, this.mContext.getUserId(), reason);
    }

    public boolean isSystemOrMiuiImportantApp(AurogonAppInfo app) {
        return app.mPackageName.contains(".miui") || app.mPackageName.contains(".xiaomi") || app.mPackageName.contains(".google") || app.mPackageName.contains("com.android") || this.mImportantAppList.contains(app.mPackageName) || isSystemApp(app.mPackageName);
    }

    public boolean isRunningLaunchMode() {
        return this.mHandler.hasMessages(106);
    }

    public void sendMeesage(int what, int args1, int args2, Object obj, long delayTime) {
        Message msg = this.mHandler.obtainMessage(what);
        if (args1 != -1) {
            msg.arg1 = args1;
        }
        if (args2 != -1) {
            msg.arg2 = args2;
        }
        if (obj != null) {
            msg.obj = obj;
        }
        if (delayTime == -1) {
            this.mHandler.sendMessage(msg);
        } else {
            this.mHandler.sendMessageDelayed(msg, delayTime);
        }
    }

    public boolean checkAppFgService(int uid) {
        synchronized (this.mFgServiceAppList) {
            List<Integer> list = this.mFgServiceAppList.get(Integer.valueOf(uid));
            if (list != null && list.size() > 0) {
                return true;
            }
            return false;
        }
    }

    public AurogonAppInfo getAurogonAppInfo(int uid) {
        synchronized (this.mImmobulusTargetList) {
            for (AurogonAppInfo app : this.mImmobulusTargetList) {
                if (app.mUid == uid) {
                    return app;
                }
            }
            return null;
        }
    }

    public AurogonAppInfo getAurogonAppInfo(String packageName) {
        synchronized (this.mImmobulusTargetList) {
            for (AurogonAppInfo app : this.mImmobulusTargetList) {
                if (app.mPackageName == packageName) {
                    return app;
                }
            }
            return null;
        }
    }

    public void notifyAppSwitchToBg(int uid) {
        if (!IMMOBULUS_ENABLED) {
            return;
        }
        String packageName = getPackageNameFromUid(uid);
        if (AccessController.PACKAGE_CAMERA.equals(packageName) && this.mEnterImmobulusMode) {
            triggerImmobulusMode(false);
        }
        AurogonAppInfo app = getAurogonAppInfo(uid);
        if (app != null && app.mPackageName.equals(this.mLastPackageName)) {
            finishLaunchMode();
        }
        updateTargetList(uid);
    }

    public void notifyAppActive(int uid) {
        if (!IMMOBULUS_ENABLED) {
            return;
        }
        updateTargetList(uid);
        AurogonAppInfo app = getAurogonAppInfo(uid);
        if (app != null && !this.mAllowList.contains(app.mPackageName) && this.mEnterImmobulusMode) {
            repeatCheckAppForImmobulusMode(app);
        }
    }

    public void notifyFgServicesChanged(int pid, int uid) {
        if (this.mProcessManagerInternal == null) {
            Slog.d(TAG, " mProcessManagerInternal = null");
            this.mProcessManagerInternal = getProcessManagerInternal();
        }
        checkFgServicesList();
        if (this.mProcessManagerInternal.checkAppFgServices(pid)) {
            updateFgServicesList(uid, pid, true);
        } else {
            updateFgServicesList(uid, pid, false);
        }
    }

    private void checkFgServicesList() {
        if (this.mProcessManagerInternal == null) {
            return;
        }
        synchronized (this.mFgServiceAppList) {
            List<Integer> tempUidList = new ArrayList<>();
            for (Integer num : this.mFgServiceAppList.keySet()) {
                int uid = num.intValue();
                List<Integer> list = this.mFgServiceAppList.get(Integer.valueOf(uid));
                if (list != null) {
                    List<Integer> tempList = new ArrayList<>();
                    for (Integer num2 : list) {
                        int pid = num2.intValue();
                        if (!this.mProcessManagerInternal.checkAppFgServices(pid)) {
                            tempList.add(Integer.valueOf(pid));
                        }
                    }
                    for (Integer num3 : tempList) {
                        int tempPid = num3.intValue();
                        list.remove(Integer.valueOf(tempPid));
                    }
                    if (list.size() == 0) {
                        tempUidList.add(Integer.valueOf(uid));
                    }
                }
            }
            for (Integer num4 : tempUidList) {
                int tempUid = num4.intValue();
                Slog.d(TAG, " remove fg services app uid = " + tempUid);
                this.mFgServiceAppList.remove(Integer.valueOf(tempUid));
            }
        }
    }

    public ProcessManagerInternal getProcessManagerInternal() {
        ProcessManagerInternal pmi = (ProcessManagerInternal) LocalServices.getService(ProcessManagerInternal.class);
        return pmi;
    }

    public void updateAppsOnWindowsStatus() {
        if (this.mGreezeService.mWindowManager != null) {
            try {
                List<String> list = this.mGreezeService.mWindowManager.getAppsOnWindowsStatus();
                if (list != null) {
                    this.mOnWindowsAppList.clear();
                    for (String uid : list) {
                        this.mOnWindowsAppList.add(Integer.valueOf(Integer.parseInt(uid)));
                    }
                }
            } catch (Exception e) {
            }
        }
    }

    public void updateTargetList(int uid) {
        synchronized (this.mImmobulusTargetList) {
            if (uid < 10000 || uid > 19999) {
                return;
            }
            for (AurogonAppInfo app : this.mImmobulusTargetList) {
                if (app.mUid == uid) {
                    return;
                }
            }
            String packageName = getPackageNameFromUid(uid);
            if (packageName == null) {
                return;
            }
            if (AccessController.PACKAGE_CAMERA.equals(packageName)) {
                this.mCameraUid = uid;
            }
            String packageName2 = packageName.split(":")[0];
            if (!packageName2.contains(".qualcomm") && !packageName2.contains("com.qti")) {
                if (isCtsApp(packageName2) && !packageName2.contains("ctsshim")) {
                    QuitLaunchModeAction(false);
                    this.mLaunchModeEnabled = false;
                }
                AurogonAppInfo app2 = new AurogonAppInfo(uid, packageName2);
                if (isAppHasIcon(packageName2)) {
                    app2.hasIcon = true;
                }
                this.mImmobulusTargetList.add(app2);
            }
        }
    }

    public void addLaunchModeQiutList(final int uid) {
        this.mHandler.post(new Runnable() { // from class: com.miui.server.greeze.AurogonImmobulusMode.1
            @Override // java.lang.Runnable
            public void run() {
                AurogonAppInfo app = AurogonImmobulusMode.this.getAurogonAppInfo(uid);
                synchronized (AurogonImmobulusMode.this.mQuitLaunchModeList) {
                    if (app != null) {
                        AurogonImmobulusMode.this.mQuitLaunchModeList.add(app);
                    }
                }
            }
        });
    }

    public boolean isCtsApp(String packageName) {
        String[] str = packageName.split(".");
        if (str != null) {
            for (String str2 : str) {
                if ("cts".equals(str)) {
                    return true;
                }
            }
        }
        return this.CTS_NAME.contains(packageName);
    }

    private boolean isSystemApp(String packageName) {
        try {
            ApplicationInfo info = this.mPm.getApplicationInfo(packageName, 0);
            if (info == null) {
                return false;
            }
            if ((info.flags & 1) == 0) {
                return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isAppHasIcon(String packageName) {
        Intent intent = new Intent();
        intent.addCategory("android.intent.category.LAUNCHER");
        intent.setAction("android.intent.action.MAIN");
        if (packageName != null) {
            intent.setPackage(packageName);
        }
        List<ResolveInfo> resolveInfolist = this.mPm.queryIntentActivities(intent, 131072);
        if (resolveInfolist == null || resolveInfolist.size() <= 0) {
            return false;
        }
        return true;
    }

    public void updateFgServicesList(int uid, int pid, boolean allow) {
        synchronized (this.mFgServiceAppList) {
            List<Integer> list = this.mFgServiceAppList.get(Integer.valueOf(uid));
            if (allow) {
                if (list == null) {
                    list = new ArrayList();
                    this.mFgServiceAppList.put(Integer.valueOf(uid), list);
                }
                if (!list.contains(Integer.valueOf(pid))) {
                    list.add(Integer.valueOf(pid));
                }
            } else if (list != null && list.contains(Integer.valueOf(pid))) {
                list.remove(Integer.valueOf(pid));
                if (list.size() == 0) {
                    this.mFgServiceAppList.remove(Integer.valueOf(uid));
                }
            }
        }
    }

    private String getPackageNameFromUid(int uid) {
        String packageName = null;
        PackageManager packageManager = this.mPm;
        if (packageManager != null) {
            packageName = packageManager.getNameForUid(uid);
        }
        if (packageName == null) {
            Slog.d(TAG, "get caller pkgname failed uid = " + uid);
        }
        return packageName;
    }

    public boolean isDownloadApp(int uid) {
        boolean z;
        synchronized (this.mDownloadAppList) {
            z = this.mDownloadAppList.get(uid) != null;
        }
        return z;
    }

    public void updateDownloadAppStatus() {
    }

    public void getConnectivityManager() {
        if (this.mConnMgr == null) {
            this.mConnMgr = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        }
    }

    public boolean isNeedRestictNetworkPolicy(int uid) {
        AurogonAppInfo app = getAurogonAppInfo(uid);
        if (app != null && mMessageApp.contains(app.mPackageName)) {
            return true;
        }
        return false;
    }

    public void triggerLaunchModeAction(int uid) {
        if (this.mHandler.hasMessages(106)) {
            this.mHandler.removeMessages(106);
            sendMeesage(106, -1, -1, null, 5000L);
        }
        StringBuilder log = new StringBuilder();
        log.append("LM FZ [" + uid + "] uid = [");
        List<Integer> list = new ArrayList<>();
        updateAppsOnWindowsStatus();
        this.mGreezeService.updatexcuteServiceStatus();
        synchronized (this.mImmobulusTargetList) {
            for (AurogonAppInfo app : this.mImmobulusTargetList) {
                if (uid != app.mUid) {
                    if (!this.mGreezeService.isAppRunning(app.mUid)) {
                        if (GreezeManagerService.DEBUG) {
                            Slog.d(TAG, " uid = " + app.mUid + " is not running app, skip check!");
                        }
                    } else {
                        List<Integer> list2 = this.mOnWindowsAppList;
                        if (list2 != null && list2.contains(Integer.valueOf(app.mUid))) {
                            if (GreezeManagerService.DEBUG) {
                                Slog.d(TAG, " uid = " + app.mUid + " is show on screen, skip it!");
                            }
                        } else if (isWallPaperApp(app.mPackageName)) {
                            if (GreezeManagerService.DEBUG) {
                                Slog.d(TAG, " uid = " + app.mUid + " is WallPaperApp app, skip check!");
                            }
                        } else if (this.mGreezeService.isUidFrozen(app.mUid)) {
                            if (GreezeManagerService.DEBUG) {
                                Slog.d(TAG, " uid = " + app.mUid + " is frozen app, skip check!");
                            }
                            this.mGreezeService.updateFrozenInfoForImmobulus(app.mUid, 16);
                        } else if (this.mAllowList.contains(app.mPackageName)) {
                            if (GreezeManagerService.DEBUG) {
                                Slog.d(TAG, " uid = " + app.mUid + " is allow list app, skip check!");
                            }
                        } else if (checkAppStatusForFreeze(app)) {
                            if (GreezeManagerService.mCgroupV1Flag) {
                                list.add(Integer.valueOf(app.mUid));
                            } else {
                                boolean success = freezeActionForLaunchMode(app);
                                if (success) {
                                    log.append(" " + app.mUid);
                                }
                            }
                        }
                    }
                }
            }
        }
        if (GreezeManagerService.mCgroupV1Flag && list.size() > 0) {
            int[] uids = new int[list.size()];
            for (int i = 0; i < list.size(); i++) {
                uids[i] = list.get(i).intValue();
            }
            List<Integer> result = this.mGreezeService.freezeUids(uids, 0L, 16, "LAUNCH_MODE", false);
            for (Integer num : result) {
                int temp = num.intValue();
                log.append(" " + temp);
                AurogonAppInfo app2 = getAurogonAppInfo(temp);
                if (app2 != null) {
                    if (isSystemOrMiuiImportantApp(app2) || checkAppFgService(app2.mUid) || !app2.hasIcon) {
                        synchronized (this.mQuitLaunchModeList) {
                            this.mQuitLaunchModeList.add(app2);
                        }
                    }
                }
            }
        }
        log.append("]");
        this.mGreezeService.addToDumpHistory(log.toString());
        if (this.mHandler.hasMessages(106)) {
            this.mHandler.removeMessages(106);
        }
        sendMeesage(106, -1, -1, null, 3000L);
    }

    public void QuitLaunchModeAction(boolean timeout) {
        String reason;
        if (timeout) {
            reason = "LM timeout";
        } else if (this.mHandler.hasMessages(106)) {
            this.mHandler.removeMessages(106);
            reason = "LM finish";
        } else {
            return;
        }
        StringBuilder log = new StringBuilder();
        log.append(reason + " THAW uid = [");
        synchronized (this.mQuitLaunchModeList) {
            for (AurogonAppInfo app : this.mQuitLaunchModeList) {
                boolean success = this.mGreezeService.thawUid(app.mUid, 16, "LAUNCH_MODE");
                if (success) {
                    log.append(" " + app.mUid);
                }
            }
            this.mQuitLaunchModeList.clear();
        }
        log.append("]");
        this.mGreezeService.addToDumpHistory(log.toString());
        this.mGreezeService.resetStatusForImmobulus(16);
        this.mLastPackageName = "";
    }

    public boolean freezeActionForLaunchMode(AurogonAppInfo app) {
        boolean isNeedCompact = false;
        if (isSystemOrMiuiImportantApp(app) || checkAppFgService(app.mUid) || !app.hasIcon) {
            synchronized (this.mQuitLaunchModeList) {
                this.mQuitLaunchModeList.add(app);
            }
        } else {
            isNeedCompact = true;
        }
        boolean success = this.mGreezeService.freezeAction(app.mUid, 16, "LAUNCH_MODE", isNeedCompact);
        return success;
    }

    public void triggerLaunchMode(String processName, int uid) {
        if (!this.mLaunchModeEnabled) {
            return;
        }
        Slog.d(TAG, "launch app processName = " + processName + " uid = " + uid);
        this.mLastPackageName = processName;
        if (AccessController.PACKAGE_CAMERA.equals(processName)) {
            this.mEnterIMCamera = true;
        }
        sendMeesage(MSG_LAUNCH_MODE_TRIGGER_ACTION, uid, -1, null, -1L);
    }

    public void finishLaunchMode(String processName, int uid) {
        if (processName != this.mLastPackageName) {
            return;
        }
        sendMeesage(106, 1, -1, null, -1L);
    }

    public void finishLaunchMode() {
        if (!this.mHandler.hasMessages(106)) {
            return;
        }
        sendMeesage(106, 1, -1, null, -1L);
    }

    public void triggerImmobulusMode(boolean allow) {
        if (!this.mImmobulusModeEnabled) {
            return;
        }
        if (allow) {
            if (!this.mEnterImmobulusMode) {
                this.mEnterImmobulusMode = true;
                sendMeesage(101, -1, -1, null, 3500L);
            }
        } else if (this.mEnterImmobulusMode) {
            this.mHandler.sendEmptyMessage(102);
        }
    }

    public boolean isModeReason(String reason) {
        if (REASON_FREEZE.contains(reason)) {
            return true;
        }
        return false;
    }

    public boolean isWallPaperApp(String packageName) {
        if (this.mWallPaperPackageName.equals("")) {
            getWallpaperPackageName();
        }
        return packageName.equals(this.mWallPaperPackageName);
    }

    public boolean isVpnApp(int uid) {
        boolean z;
        synchronized (this.mVPNAppList) {
            z = this.mVPNAppList.contains(Integer.valueOf(uid));
        }
        return z;
    }

    public void getWallpaperPackageName() {
        try {
            WallpaperManager wm = (WallpaperManager) this.mContext.getSystemService("wallpaper");
            WallpaperInfo info = wm.getWallpaperInfo();
            if (info != null) {
                this.mWallPaperPackageName = info.getPackageName();
            } else {
                ComponentName componentName = WallpaperManager.getDefaultWallpaperComponent(this.mContext);
                if (componentName != null) {
                    this.mWallPaperPackageName = componentName.getPackageName();
                }
            }
        } catch (Exception e) {
        }
    }

    /* loaded from: classes.dex */
    public final class ImmobulusHandler extends Handler {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        private ImmobulusHandler(Looper looper) {
            super(looper);
            AurogonImmobulusMode.this = r1;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            int what = msg.what;
            switch (what) {
                case 101:
                    AurogonImmobulusMode.this.TriggerImmobulusModeAction();
                    return;
                case 102:
                    AurogonImmobulusMode.this.QuitImmobulusModeAction();
                    return;
                case AurogonImmobulusMode.MSG_REPEAT_FREEZE_APP /* 103 */:
                    AurogonAppInfo app = (AurogonAppInfo) msg.obj;
                    AurogonImmobulusMode.this.checkAppForImmobulusMode(app);
                    return;
                case AurogonImmobulusMode.MSG_REMOVE_ALL_MESSAGE /* 104 */:
                    AurogonImmobulusMode.this.removeAllMsg();
                    return;
                case AurogonImmobulusMode.MSG_LAUNCH_MODE_TRIGGER_ACTION /* 105 */:
                    int flag = msg.arg1;
                    AurogonImmobulusMode.this.triggerLaunchModeAction(flag);
                    return;
                case 106:
                    int flag2 = msg.arg1;
                    if (flag2 != 1) {
                        AurogonImmobulusMode.this.QuitLaunchModeAction(true);
                        return;
                    } else {
                        AurogonImmobulusMode.this.QuitLaunchModeAction(false);
                        return;
                    }
                default:
                    return;
            }
        }
    }

    public void removeAllMsg() {
        this.mHandler.removeMessages(101);
        this.mHandler.removeMessages(102);
        this.mHandler.removeMessages(MSG_REPEAT_FREEZE_APP);
        this.mHandler.removeMessages(MSG_REMOVE_ALL_MESSAGE);
    }

    /* loaded from: classes.dex */
    public class ImmobulusBroadcastReceiver extends BroadcastReceiver {
        public ImmobulusBroadcastReceiver() {
            AurogonImmobulusMode.this = this$0;
            IntentFilter intent = new IntentFilter();
            intent.addAction(AurogonImmobulusMode.IMMOBULUS_GAME_CONTROLLER);
            intent.addAction("android.intent.action.WALLPAPER_CHANGED");
            intent.addAction("android.net.conn.CONNECTIVITY_CHANGE");
            this$0.mContext.registerReceiver(this, intent);
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if (!AurogonImmobulusMode.this.mImmobulusModeEnabled) {
                return;
            }
            String action = intent.getAction();
            boolean z = false;
            if (!AurogonImmobulusMode.IMMOBULUS_GAME_CONTROLLER.equals(action)) {
                if ("android.intent.action.WALLPAPER_CHANGED".equals(action)) {
                    AurogonImmobulusMode.this.getWallpaperPackageName();
                    return;
                } else if ("android.net.conn.CONNECTIVITY_CHANGE".equals(action) && AurogonImmobulusMode.this.mConnMgr != null) {
                    NetworkInfo networkInfo = AurogonImmobulusMode.this.mConnMgr.getNetworkInfo(17);
                    AurogonImmobulusMode aurogonImmobulusMode = AurogonImmobulusMode.this;
                    if (networkInfo != null) {
                        z = networkInfo.isConnected();
                    }
                    aurogonImmobulusMode.mVpnConnect = z;
                    if (AurogonImmobulusMode.this.mVpnConnect) {
                        AurogonImmobulusMode.this.updateVpnStatus(networkInfo);
                        return;
                    }
                    return;
                } else {
                    return;
                }
            }
            int type = intent.getIntExtra(AurogonImmobulusMode.IMMOBULUS_GAME_KEY_STATUS, 0);
            String[] allowList = intent.getStringArrayExtra(AurogonImmobulusMode.IMMOBULUS_GAME_KEY_ALLOW_LIST);
            for (String str : allowList) {
                if (!AurogonImmobulusMode.this.mImmobulusAllowList.contains(str)) {
                    AurogonImmobulusMode.this.mImmobulusAllowList.add(str);
                }
            }
            if (type == 1) {
                if (!AurogonImmobulusMode.this.mEnterImmobulusMode) {
                    AurogonImmobulusMode.this.mEnterImmobulusMode = true;
                    AurogonImmobulusMode.this.sendMeesage(101, -1, -1, null, 3500L);
                    return;
                }
                Slog.d(AurogonImmobulusMode.TAG, "immobulus mode has enter!");
            } else if (type == 2 && AurogonImmobulusMode.this.mEnterImmobulusMode) {
                AurogonImmobulusMode.this.mHandler.sendEmptyMessage(102);
            }
        }
    }

    public void updateVpnStatus(NetworkInfo networkInfo) {
        ConnectivityManager connectivityManager = this.mConnMgr;
        NetworkCapabilities nc = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
        if (nc != null) {
            int[] uids = nc.getAdministratorUids();
            synchronized (this.mVPNAppList) {
                this.mVPNAppList.clear();
                for (int uid : uids) {
                    this.mVPNAppList.add(Integer.valueOf(uid));
                }
            }
        }
    }

    public void updateCloudAllowList() {
        if (!this.mGreezeService.mPowerMilletEnable || this.mCtsModeOn || !CN_MODEL) {
            this.mLaunchModeEnabled = false;
            this.mImmobulusModeEnabled = false;
            return;
        }
        String str = Settings.Secure.getString(this.mContext.getContentResolver(), IMMOBULUS_SWITCH_SECURE_SETTING);
        if (str != null) {
            Slog.d(TAG, "clound setting str = " + str);
            String[] temp = str.split("_");
            if (temp.length < 3) {
                return;
            }
            if (MiuiCustomizeShortCutUtils.ATTRIBUTE_ENABLE.equals(temp[0])) {
                int enable = Integer.parseInt(temp[1]);
                if ((enable & 8) != 0) {
                    this.mImmobulusModeEnabled = true;
                } else {
                    this.mImmobulusModeEnabled = false;
                }
                if ((enable & 16) != 0) {
                    this.mLaunchModeEnabled = true;
                } else {
                    this.mLaunchModeEnabled = false;
                }
            }
            if ("allowlist".equals(temp[2])) {
                synchronized (this.mAllowList) {
                    this.mAllowList.removeAll(this.mCloudAllowList);
                    this.mCloudAllowList.clear();
                    for (int i = 3; i < temp.length; i++) {
                        this.mCloudAllowList.add(temp[i]);
                    }
                    this.mAllowList.addAll(this.mCloudAllowList);
                }
            }
        }
    }

    public void updateIMEAppStatus() {
        String curImeId = Settings.Secure.getString(this.mContext.getContentResolver(), "default_input_method");
        if (curImeId != null) {
            String[] str = curImeId.split("/");
            if (str.length > 1) {
                this.mCurrentIMEPacageName = str[0];
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class SettingsObserver extends ContentObserver {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public SettingsObserver(Handler handler) {
            super(handler);
            AurogonImmobulusMode.this = r1;
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            if (uri == null) {
                return;
            }
            if (uri.equals(Settings.Global.getUriFor(AurogonImmobulusMode.BLUETOOTH_GLOBAL_SETTING))) {
                String str = Settings.Global.getString(AurogonImmobulusMode.this.mContext.getContentResolver(), AurogonImmobulusMode.BLUETOOTH_GLOBAL_SETTING);
                if (str != null) {
                    AurogonImmobulusMode.this.mBluetoothUsingList.clear();
                    String[] names = str.split("#");
                    for (String name : names) {
                        AurogonImmobulusMode.this.mBluetoothUsingList.add(name);
                        AurogonAppInfo app = AurogonImmobulusMode.this.getAurogonAppInfo(name);
                        if (app != null && AurogonImmobulusMode.this.mGreezeService.isUidFrozen(app.mUid)) {
                            AurogonImmobulusMode.this.unFreezeActionForImmobulus(app, "BT connect");
                        }
                    }
                }
            } else if (uri.equals(Settings.Secure.getUriFor(AurogonImmobulusMode.IMMOBULUS_SWITCH_SECURE_SETTING))) {
                AurogonImmobulusMode.this.updateCloudAllowList();
            } else if (uri.equals(Settings.Secure.getUriFor("default_input_method"))) {
                AurogonImmobulusMode.this.updateIMEAppStatus();
            } else if (uri.equals(Settings.Secure.getUriFor(MiuiSettings.Secure.MIUI_OPTIMIZATION))) {
                AurogonImmobulusMode.this.mCtsModeOn = !SystemProperties.getBoolean("persist.sys.miui_optimization", false);
                if (AurogonImmobulusMode.this.mCtsModeOn) {
                    AurogonImmobulusMode.this.QuitLaunchModeAction(false);
                    AurogonImmobulusMode.this.mLaunchModeEnabled = false;
                }
            }
        }
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("AurogonImmobulusMode : ");
        pw.println("mImmobulusModeEnabled : " + this.mImmobulusModeEnabled);
        pw.println("AurogonImmobulusMode LM : ");
        pw.println("LM enabled : " + this.mLaunchModeEnabled);
        if (GreezeManagerService.DEBUG) {
            pw.println("mImmobulusAllowList : ");
            for (String str : this.mImmobulusAllowList) {
                pw.println(" " + str);
            }
            pw.println("mImmobulusTargetList : ");
            synchronized (this.mImmobulusTargetList) {
                for (AurogonAppInfo app : this.mImmobulusTargetList) {
                    pw.println(" " + app.toString());
                }
            }
            synchronized (this.mFgServiceAppList) {
                pw.println("mFgServiceAppList : ");
                for (Integer num : this.mFgServiceAppList.keySet()) {
                    int uid = num.intValue();
                    pw.println(" uid = " + uid);
                }
            }
            pw.println("mAllowList : ");
            for (String str1 : this.mAllowList) {
                pw.println(" " + str1);
            }
            pw.println("mWallPaperPackageName");
            pw.println(" " + this.mWallPaperPackageName);
        }
        if (args.length == 0) {
            return;
        }
        if ("Immobulus".equals(args[0])) {
            if (MiuiCustomizeShortCutUtils.ATTRIBUTE_ENABLE.equals(args[1])) {
                this.mImmobulusModeEnabled = true;
                this.mHandler.sendEmptyMessage(101);
            } else if ("disabled".equals(args[1])) {
                this.mImmobulusModeEnabled = false;
                this.mHandler.sendEmptyMessage(102);
            }
        }
        if (args.length == 3 && "LM".equals(args[0])) {
            if ("add".equals(args[1])) {
                this.mAllowList.add(args[2]);
            } else if ("remove".equals(args[1])) {
                if (this.mAllowList.contains(args[2])) {
                    this.mAllowList.remove(args[2]);
                }
            } else if ("set".equals(args[1])) {
                if ("true".equals(args[2])) {
                    this.mLaunchModeEnabled = true;
                } else if ("false".equals(args[2])) {
                    this.mLaunchModeEnabled = false;
                }
            } else if ("camera".equals(args[1])) {
                if ("true".equals(args[2])) {
                    this.mEnabledLMCamera = true;
                } else if ("false".equals(args[2])) {
                    this.mEnabledLMCamera = false;
                }
            }
        }
    }
}
