package com.miui.server.smartpower;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManagerInternal;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.SystemProperties;
import android.os.Trace;
import android.os.UserHandle;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.server.am.SplitScreenReporter;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.EventLog;
import android.util.Slog;
import com.android.server.DeviceIdleInternal;
import com.android.server.LocalServices;
import com.android.server.am.ActivityManagerService;
import com.android.server.am.ActivityManagerServiceImpl;
import com.android.server.am.AppStateManager;
import com.android.server.am.ProcessRecord;
import com.android.server.am.SmartPowerService;
import com.android.server.am.SystemPressureController;
import com.android.server.audio.AudioServiceStubImpl;
import com.android.server.content.SyncManagerStubImpl;
import com.miui.server.sptm.SpeedTestModeServiceImpl;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import miui.security.CallerInfo;
/* loaded from: classes.dex */
public class SmartPowerPolicyManager implements AppStateManager.IProcessStateCallback {
    public static final int INVALID_PID = -1;
    public static final int MSG_UPDATE_USAGESTATS = 1;
    public static final long NEVER_PERIODIC_ACTIVE = 0;
    public static final int PERIODIC_ACTIVE_THRESHOLD_HIGH = 5;
    private static final String PROP_MIUI_OPTIMIZATION = "persist.sys.miui_optimization";
    public static final String REASON = "SmartPowerService";
    public static final String TAG = "SmartPower.PowerPolicy";
    public static final long UPDATE_USAGESTATS_DURATION = 1800000;
    public static final int WHITE_LIST_ACTION_HIBERNATION = 2;
    public static final int WHITE_LIST_ACTION_INTERCEPT_ALARM = 8;
    public static final int WHITE_LIST_ACTION_INTERCEPT_PROVIDER = 32;
    public static final int WHITE_LIST_ACTION_INTERCEPT_SERVICE = 16;
    public static final int WHITE_LIST_ACTION_SCREENON_HIBERNATION = 4;
    public static final int WHITE_LIST_TYPE_ALARM_CLOUDCONTROL = 65536;
    public static final int WHITE_LIST_TYPE_ALARM_DEFAULT = 32768;
    private static final int WHITE_LIST_TYPE_ALARM_MASK = 114688;
    private static final int WHITE_LIST_TYPE_ALARM_MAX = 131072;
    private static final int WHITE_LIST_TYPE_ALARM_MIN = 16384;
    public static final int WHITE_LIST_TYPE_BACKUP = 64;
    public static final int WHITE_LIST_TYPE_CLOUDCONTROL = 4;
    public static final int WHITE_LIST_TYPE_CTS = 1024;
    public static final int WHITE_LIST_TYPE_DEFAULT = 2;
    public static final int WHITE_LIST_TYPE_DEPEND = 256;
    public static final int WHITE_LIST_TYPE_EXTAUDIO = 512;
    public static final int WHITE_LIST_TYPE_FREQUENTTHAW = 32;
    private static final int WHITE_LIST_TYPE_HIBERNATION_MASK = 2046;
    private static final int WHITE_LIST_TYPE_MAX = 2048;
    private static final int WHITE_LIST_TYPE_MIN = 1;
    public static final int WHITE_LIST_TYPE_PROVIDER_CLOUDCONTROL = 4194304;
    public static final int WHITE_LIST_TYPE_PROVIDER_DEFAULT = 2097152;
    private static final int WHITE_LIST_TYPE_PROVIDER_MASK = 7340032;
    private static final int WHITE_LIST_TYPE_PROVIDER_MAX = 8388608;
    private static final int WHITE_LIST_TYPE_PROVIDER_MIN = 1048576;
    public static final int WHITE_LIST_TYPE_SCREENON_CLOUDCONTROL = 8192;
    public static final int WHITE_LIST_TYPE_SCREENON_DEFAULT = 4096;
    private static final int WHITE_LIST_TYPE_SCREENON_MASK = 14336;
    private static final int WHITE_LIST_TYPE_SCREENON_MAX = 16384;
    private static final int WHITE_LIST_TYPE_SCREENON_MIN = 2048;
    public static final int WHITE_LIST_TYPE_SERVICE_CLOUDCONTROL = 524288;
    public static final int WHITE_LIST_TYPE_SERVICE_DEFAULT = 262144;
    private static final int WHITE_LIST_TYPE_SERVICE_MASK = 917504;
    private static final int WHITE_LIST_TYPE_SERVICE_MAX = 1048576;
    private static final int WHITE_LIST_TYPE_SERVICE_MIN = 131072;
    public static final int WHITE_LIST_TYPE_USB = 128;
    public static final int WHITE_LIST_TYPE_VPN = 8;
    public static final int WHITE_LIST_TYPE_WALLPAPER = 16;
    private ActivityManagerService mAMS;
    private Context mContext;
    private Handler mHandler;
    private PowerSavingStrategyControl mPowerSavingStrategyControl;
    public static final boolean DEBUG = SmartPowerService.DEBUG;
    public static boolean sCtsModeEnable = false;
    private final ArrayMap<String, WhiteListItem> mPackageWhiteList = new ArrayMap<>();
    private final ArrayMap<String, WhiteListItem> mProcessWhiteList = new ArrayMap<>();
    private final ArrayMap<Integer, WhiteListItem> mUidWhiteList = new ArrayMap<>();
    private final ArrayMap<Integer, WhiteListItem> mPidWhiteList = new ArrayMap<>();
    private final ArrayMap<Integer, ArraySet<String>> mDependencyMap = new ArrayMap<>();
    private AppStateManager mAppStateManager = null;
    private AppPowerResourceManager mAppPowerResourceManager = null;
    private final ArraySet<String> mBroadcastBlackList = new ArraySet<>();
    private final ArraySet<String> mBroadcastWhiteList = new ArraySet<>();
    private final ArraySet<String> mBroadcastProcessActionWhiteList = new ArraySet<>();
    private final HashMap<String, UsageStatsInfo> mUsageStatsMap = new HashMap<>();
    private boolean mScreenOff = false;
    private final BroadcastReceiver mSysStatusReceiver = new BroadcastReceiver() { // from class: com.miui.server.smartpower.SmartPowerPolicyManager.1
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.SCREEN_OFF".equals(intent.getAction())) {
                SmartPowerPolicyManager.this.mScreenOff = true;
            } else if ("android.intent.action.SCREEN_ON".equals(intent.getAction())) {
                SmartPowerPolicyManager.this.mScreenOff = false;
            } else if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction()) && !SmartPowerPolicyManager.this.mPowerSavingStrategyControl.mIsInit) {
                SmartPowerPolicyManager.this.mPowerSavingStrategyControl.updateAllNoRestrictApps();
            }
        }
    };
    private UsageStatsManagerInternal mUsageStats = (UsageStatsManagerInternal) LocalServices.getService(UsageStatsManagerInternal.class);
    private DeviceIdleInternal mLocalDeviceIdleController = (DeviceIdleInternal) LocalServices.getService(DeviceIdleInternal.class);

    public SmartPowerPolicyManager(Context context, Looper looper, ActivityManagerService ams) {
        this.mHandler = new MyHandler(looper);
        this.mAMS = ams;
        this.mContext = context;
        for (String str : context.getResources().getStringArray(285409404)) {
            addPackageWhiteList(str, 2);
        }
        String[] processes = context.getResources().getStringArray(285409405);
        for (String str2 : processes) {
            addProcessWhiteList(str2, 2);
        }
        for (String str3 : context.getResources().getStringArray(285409400)) {
            addAlarmPackageWhiteList(str3);
        }
        for (String str4 : context.getResources().getStringArray(285409407)) {
            addServicePackageWhiteList(str4);
        }
        for (String str5 : context.getResources().getStringArray(285409406)) {
            addProviderPackageWhiteList(str5);
        }
        String[] packges = context.getResources().getStringArray(285409402);
        for (String str6 : packges) {
            addBroadcastProcessActionWhiteList(str6);
        }
        this.mBroadcastBlackList.addAll(Arrays.asList(context.getResources().getStringArray(285409401)));
        this.mBroadcastWhiteList.addAll(Arrays.asList(context.getResources().getStringArray(285409403)));
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.SCREEN_OFF");
        filter.addAction("android.intent.action.SCREEN_ON");
        filter.addAction("android.intent.action.BOOT_COMPLETED");
        context.registerReceiver(this.mSysStatusReceiver, filter);
        registerCTSModeObserver();
    }

    public void init(AppStateManager appStateManager, AppPowerResourceManager appPowerResourceManager) {
        this.mAppStateManager = appStateManager;
        appStateManager.registerAppStateListener(this);
        this.mAppPowerResourceManager = appPowerResourceManager;
        this.mPowerSavingStrategyControl = new PowerSavingStrategyControl();
    }

    public void onExternalAudioRegister(int uid, int pid) {
        AppStateManager.AppState.RunningProcess proc = this.mAppStateManager.getRunningProcess(uid, pid);
        if (proc != null) {
            addPidWhiteList(uid, pid, proc.getProcessName(), 512);
        }
    }

    public void onWallpaperChangedLocked(boolean active, int uid, String packageName) {
        if (active) {
            addUidWhiteList(uid, packageName, 16);
        } else {
            removeUidWhiteList(uid, packageName, 16);
        }
    }

    public void onVpnChanged(boolean active, int uid, String packageName) {
        if (active) {
            addUidWhiteList(uid, packageName, 8);
        } else {
            removeUidWhiteList(uid, packageName, 8);
        }
    }

    public void onBackupChanged(boolean active, int uid, String packageName) {
        if (active) {
            addUidWhiteList(uid, packageName, 64);
        } else {
            removeUidWhiteList(uid, packageName, 64);
        }
    }

    public void onUsbStateChanged(boolean isConnected, boolean isDataTransfer) {
    }

    public void onDependChanged(boolean isConnected, String processName, String dependProcName, int uid, int pid) {
        synchronized (this.mDependencyMap) {
            ArraySet<String> dependList = this.mDependencyMap.get(Integer.valueOf(pid));
            if (isConnected) {
                if (dependList == null) {
                    dependList = new ArraySet<>();
                    this.mDependencyMap.put(Integer.valueOf(pid), dependList);
                    addPidWhiteList(uid, pid, dependProcName, 256);
                }
                dependList.add(processName);
            } else if (dependList != null) {
                dependList.remove(processName);
                if (dependList.size() == 0) {
                    this.mDependencyMap.remove(Integer.valueOf(pid));
                    removePidWhiteList(uid, pid, dependProcName, 256);
                }
            }
        }
    }

    public boolean isDependedProcess(int pid) {
        boolean containsKey;
        synchronized (this.mDependencyMap) {
            containsKey = this.mDependencyMap.containsKey(Integer.valueOf(pid));
        }
        return containsKey;
    }

    public void updateCloudPackageWhiteList(String pkgList) {
        if (!TextUtils.isEmpty(pkgList)) {
            String[] pkgArray = pkgList.split(",");
            for (String packageName : pkgArray) {
                addPackageWhiteList(packageName, 4);
            }
        }
    }

    public void updateCloudProcessWhiteList(String procList) {
        if (!TextUtils.isEmpty(procList)) {
            String[] procArray = procList.split(",");
            for (String procName : procArray) {
                addProcessWhiteList(procName, 4);
            }
        }
    }

    public void updateCloudSreenonWhiteList(String sreenonList) {
        if (!TextUtils.isEmpty(sreenonList)) {
            String[] screenAppArray = sreenonList.split(",");
            for (String packageName : screenAppArray) {
                addSreenonWhiteList(packageName, 8192);
            }
        }
    }

    public void updateCloudBroadcastWhiteLit(String procList) {
        if (!TextUtils.isEmpty(procList)) {
            String[] procArray = procList.split(",");
            for (String procAction : procArray) {
                addBroadcastProcessActionWhiteList(procAction);
            }
        }
    }

    public void updateCloudAlarmWhiteLit(String procList) {
        if (!TextUtils.isEmpty(procList)) {
            String[] procArray = procList.split(",");
            for (String proc : procArray) {
                addAlarmPackageWhiteList(proc);
            }
        }
    }

    public void updateCloudProviderWhiteLit(String procList) {
        if (!TextUtils.isEmpty(procList)) {
            String[] procArray = procList.split(",");
            for (String proc : procArray) {
                addProviderPackageWhiteList(proc);
            }
        }
    }

    public void updateCloudServiceWhiteLit(String procList) {
        if (!TextUtils.isEmpty(procList)) {
            String[] procArray = procList.split(",");
            for (String proc : procArray) {
                addServicePackageWhiteList(proc);
            }
        }
    }

    public boolean shouldInterceptProviderLocked(int callingUid, ProcessRecord callingProc, ProcessRecord hostingProc, boolean isRunning) {
        AppStateManager.AppState hostingState;
        if (!isRunning || !isEnableIntercept() || sCtsModeEnable || (hostingState = this.mAppStateManager.getAppState(hostingProc.uid)) == null || isProcessHibernationWhiteList(hostingProc.uid, hostingProc.mPid, hostingState.getPackageName(), hostingProc.processName) || hostingState.isProcessPerceptible(hostingProc.mPid) || hostingState.isSystemApp() || hostingState.isSystemSignature() || isProviderPackageWhiteList(hostingState.getPackageName())) {
            return false;
        }
        if (callingProc != null) {
            AppStateManager.AppState callingState = this.mAppStateManager.getAppState(callingProc.uid);
            if (callingState == null || AppStateManager.isSystemApp(callingProc) || callingProc.mPid == hostingProc.mPid || isProcessHibernationWhiteList(callingProc.uid, callingProc.mPid, callingState.getPackageName(), callingProc.processName) || callingState.isProcessPerceptible(callingProc.mPid)) {
                return false;
            }
        } else {
            AppStateManager.AppState callingState2 = this.mAppStateManager.getAppState(callingUid);
            if (callingState2 == null || callingState2.isVsible()) {
                return false;
            }
        }
        EventLog.writeEvent((int) SmartPowerSettings.EVENT_TAGS, "power prv h:{ u:" + hostingProc.uid + " p:" + hostingProc.processName + " s:" + hostingState.getProcessState(hostingProc.processName) + "} c:" + callingUid);
        return true;
    }

    public boolean shouldInterceptBroadcastLocked(int callingUid, int callingPid, String callingPkg, ProcessRecord recProc, Intent intent, boolean ordered, boolean sticky) {
        if (recProc == null || !isEnableIntercept() || sCtsModeEnable) {
            return false;
        }
        AppStateManager.AppState recAppState = this.mAppStateManager.getAppState(recProc.uid);
        if (recAppState != null && !recAppState.isSystemApp()) {
            if (callingPid != recProc.mPid || !recAppState.isSystemSignature()) {
                if (isSpeedTestMode()) {
                    return true;
                }
                if (!isBroadcastProcessWhiteList(recProc.processName, intent.getAction()) && !isBroadcastWhiteList(intent.getAction()) && !recAppState.isProcessPerceptible(recProc.mPid) && !isProcessHibernationWhiteList(recAppState.getUid(), recProc.mPid, recAppState.getPackageName(), recProc.processName)) {
                    if (isBroadcastBlackList(intent.getAction())) {
                        return true;
                    }
                    int recState = recAppState.getProcessState(recProc.processName);
                    if (recState < 6 && recState > 0 && !sticky) {
                        return false;
                    }
                    String cmpPackage = intent.getComponent() != null ? intent.getComponent().getPackageName() : "";
                    if (!TextUtils.isEmpty(cmpPackage) && cmpPackage.equals(recAppState.getPackageName())) {
                        return false;
                    }
                    AppStateManager.AppState callAppState = this.mAppStateManager.getAppState(callingUid);
                    if (callAppState == null) {
                        return sticky;
                    }
                    if ((!recAppState.isAutoStartApp() && recState <= 0) || (!callAppState.isSystemApp() && this.mAppStateManager.isProcessKilled(callingUid, callingPid))) {
                        if (DEBUG) {
                            Slog.d(TAG, "Intercept: broadcast from{ " + callingUid + "/" + callingPkg + " (" + callingPid + ") killed=" + this.mAppStateManager.isProcessKilled(callingUid, callingPid) + "} to{" + recProc.uid + "/" + recProc.processName + " (" + recProc.mPid + ") killed=" + this.mAppStateManager.isProcessKilled(callingUid, callingPid) + " " + intent.toString() + " sticky=" + sticky + " ordered=" + ordered);
                            return true;
                        }
                        return true;
                    }
                    if (!Process.isCoreUid(callingUid) || sticky) {
                        if (callingUid == recProc.uid && recProc.processName.endsWith(":widgetProvider")) {
                            return false;
                        }
                        AppStateManager.AppState.RunningProcess callingProc = this.mAppStateManager.getRunningProcess(callingUid, callingPid);
                        if (callingProc != null && (callingProc.isProcessPerceptible() || isProcessHibernationWhiteList(callingProc))) {
                            return false;
                        }
                        if (DEBUG) {
                            Slog.d(TAG, "Intercept: broadcast from{ " + callingUid + "/" + callingPkg + " (" + callingPid + ")} to{" + recProc.uid + "/" + recProc.processName + " (" + recProc.mPid + ") " + intent.toString() + " sticky=" + sticky + " ordered=" + ordered);
                            return true;
                        }
                        return true;
                    }
                    return false;
                }
                return false;
            }
        }
        return false;
    }

    public boolean shouldInterceptAlarmLocked(int uid, int type) {
        AppStateManager.AppState state;
        if (!isEnableIntercept() || sCtsModeEnable || (state = this.mAppStateManager.getAppState(uid)) == null || state.isProcessPerceptible() || isAppHibernationWhiteList(uid, state.getPackageName()) || state.isSystemApp()) {
            return false;
        }
        if (isSpeedTestMode()) {
            return true;
        }
        if (type == 0 || type == 2 || isAlarmPackageWhiteList(state.getPackageName())) {
            return false;
        }
        EventLog.writeEvent((int) SmartPowerSettings.EVENT_TAGS, "power alarm h:{ u:" + uid + " t:" + type + "}" + state.getPackageName());
        return true;
    }

    public boolean skipFrozenAppAnr(ApplicationInfo info, int uid, String report) {
        int timeout;
        if (!isEnableFrozen()) {
            return false;
        }
        if (PowerFrozenManager.getInstance().isFrozenForUid(uid)) {
            return true;
        }
        if (TextUtils.isEmpty(report)) {
            return false;
        }
        if (report.startsWith("Broadcast of")) {
            timeout = 20000;
        } else if (report.startsWith("executing service")) {
            timeout = 20000;
        } else if (report.startsWith("ContentProvider not")) {
            timeout = AudioServiceStubImpl.READ_MUSIC_PLAY_BACK_DELAY;
        } else if (!report.startsWith("Input dispatching")) {
            return false;
        } else {
            timeout = 5000;
        }
        return this.mAppStateManager.isRecentThawed(uid, System.currentTimeMillis() - timeout);
    }

    public boolean shouldInterceptService(Intent service, CallerInfo callingInfo, ServiceInfo serviceInfo) {
        if (isEnableIntercept() && !sCtsModeEnable) {
            AppStateManager.AppState callingApp = this.mAppStateManager.getAppState(callingInfo.callerUid);
            int serviceUid = serviceInfo.applicationInfo.uid;
            String servicePckName = serviceInfo.applicationInfo.packageName;
            String serviceProcName = serviceInfo.processName;
            boolean isSelfProc = serviceProcName.equals(callingInfo.callerProcessName);
            boolean isWidgetSelf = isSelfProc && serviceProcName.endsWith(":widgetProvider");
            if (callingApp != null) {
                boolean isSystemSignSelf = isSelfProc && callingApp.isSystemSignature();
                if (callingInfo.callerUid == serviceUid && !callingApp.isSystemApp()) {
                    if (!callingApp.isAutoStartApp() && !isWidgetSelf && !isSystemSignSelf) {
                        if (!callingApp.isProcessPerceptible(callingInfo.callerPid) && !isProcessHibernationWhiteList(callingInfo.callerUid, callingInfo.callerPid, callingInfo.callerPkg, callingInfo.callerProcessName) && !isServicePackageWhiteList(servicePckName)) {
                            AppStateManager.AppState.RunningProcess serverProc = this.mAppStateManager.getRunningProcess(serviceUid, serviceProcName);
                            if (serverProc != null && !serverProc.isProcessPerceptible()) {
                                if (!isProcessHibernationWhiteList(serviceUid, serverProc.getPid(), servicePckName, serviceProcName) && (serverProc.getPid() != callingInfo.callerPid || !hasPidWhiteList(callingInfo.callerUid))) {
                                    EventLog.writeEvent((int) SmartPowerSettings.EVENT_TAGS, "power ser h:{ u:" + serviceUid + " p:" + serviceProcName + "/" + serverProc.getPid() + " s:" + serverProc.getCurrentState() + "} c:" + callingInfo.callerUid + ":" + callingInfo.callerPid);
                                    if (DEBUG) {
                                        Slog.d(TAG, "Intercept: service calling uid " + callingInfo.callerUid + "/" + callingInfo.callerProcessName + " serviceState:" + callingApp.getProcessState(serviceProcName) + " serviceInfo( processName=" + serviceProcName + " getComponentName=" + serviceInfo.getComponentName() + " uid=" + serviceUid + ") Intent=" + service);
                                    }
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
            return false;
        }
        return false;
    }

    public static boolean isSpeedTestMode() {
        return SpeedTestModeServiceImpl.getInstance().isSpeedTestMode();
    }

    public boolean isScreenOff() {
        return this.mScreenOff;
    }

    @Override // com.android.server.am.AppStateManager.IProcessStateCallback
    public void onProcessStateChanged(AppStateManager.AppState.RunningProcess proc, int oldState, int newState, String reason) {
        WhiteListItem item;
        Trace.traceBegin(131072L, "onProcessStateChanged");
        if (newState >= 6 && oldState < 6) {
            idleProcess(proc);
        }
        if (newState >= 7 && oldState < 7) {
            hibernationProcess(proc, reason);
        }
        if (newState < 7 && oldState == 7) {
            activeProcess(proc, reason);
        }
        if (newState == 0) {
            synchronized (this.mPidWhiteList) {
                item = this.mPidWhiteList.remove(Integer.valueOf(proc.getPid()));
            }
            if (item != null) {
                synchronized (this.mDependencyMap) {
                    this.mDependencyMap.remove(Integer.valueOf(item.mPid));
                }
            }
        }
        Trace.traceEnd(131072L);
    }

    @Override // com.android.server.am.AppStateManager.IProcessStateCallback
    public void onAppStateChanged(AppStateManager.AppState app, int oldState, int newState, boolean oldProtectState, boolean newProtectState, String reason) {
        Trace.traceBegin(131072L, "onAppStateChanged");
        if (!newProtectState && newState >= 5 && oldState < 5) {
            idleApp(app);
        }
        if (!newProtectState && newState >= 6 && oldState < 6) {
            hibernationApp(app, reason);
        }
        if ((newState < 6 || newProtectState) && oldState == 6 && !oldProtectState) {
            activeApp(app, reason);
        }
        if (newState == 0) {
            synchronized (this.mUidWhiteList) {
                this.mUidWhiteList.remove(Integer.valueOf(app.getUid()));
            }
        }
        Trace.traceEnd(131072L);
    }

    public long getIdleDur(String packageName) {
        return SmartPowerSettings.IDLE_DURATION;
    }

    public long getHibernateDuration(AppStateManager.AppState app) {
        int usageLevel = app.getUsageLevel();
        if (usageLevel == 5 && app.isAutoStartApp() && !app.hasProtectProcess()) {
            return SmartPowerSettings.HIBERNATE_DURATION;
        }
        return 0L;
    }

    public long getMaintenanceDur(String packageName) {
        return SmartPowerSettings.MAINTENANCE_DURATION;
    }

    public long getInactiveDuration(String packageName, int uid) {
        if (isSpeedTestMode()) {
            return SmartPowerSettings.INACTIVE_SPTM_DURATION;
        }
        return SmartPowerSettings.INACTIVE_DURATION;
    }

    public boolean needCheckNet(String packageName, int uid) {
        UsageStatsInfo info = getUsageStatsInfo(packageName);
        return info != null && info.mAppLaunchCount > 0;
    }

    private void idleProcess(AppStateManager.AppState.RunningProcess proc) {
    }

    private void idleApp(final AppStateManager.AppState app) {
        if (isEnableIntercept() && !sCtsModeEnable && !checkCtsProcess(app.getPackageName())) {
            Runnable idleRunnable = new Runnable() { // from class: com.miui.server.smartpower.SmartPowerPolicyManager$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    SmartPowerPolicyManager.this.m2400xc02f742f(app);
                }
            };
            if (app.getMinAmsProcState() > 6) {
                this.mHandler.post(idleRunnable);
            } else {
                this.mHandler.postDelayed(idleRunnable, ActivityManagerServiceImpl.KEEP_FOREGROUND_DURATION);
            }
        }
    }

    /* renamed from: lambda$idleApp$0$com-miui-server-smartpower-SmartPowerPolicyManager */
    public /* synthetic */ void m2400xc02f742f(AppStateManager.AppState app) {
        String event = "power idle " + app.getPackageName() + "/" + app.getUid();
        Trace.traceBegin(131072L, event);
        if (app.isAlive() && app.isIdle() && !isDeviceIdleWhiteList(app.getUid())) {
            try {
                this.mAMS.makePackageIdle(app.getPackageName(), -2);
                EventLog.writeEvent((int) SmartPowerSettings.EVENT_TAGS, event);
            } catch (Exception e) {
            }
        }
        Trace.traceEnd(131072L);
    }

    private boolean isDeviceIdleWhiteList(int uid) {
        DeviceIdleInternal deviceIdleInternal = this.mLocalDeviceIdleController;
        return deviceIdleInternal != null && deviceIdleInternal.isAppOnWhitelist(UserHandle.getAppId(uid));
    }

    private void hibernationApp(AppStateManager.AppState app, String reason) {
        if (isEnableIntercept()) {
            Trace.traceBegin(131072L, "hibernationApp");
            EventLog.writeEvent((int) SmartPowerSettings.EVENT_TAGS, "power hibernation " + app.getPackageName() + "/" + app.getUid());
            this.mAppPowerResourceManager.releaseAppAllPowerResources(app.getUid());
            Trace.traceEnd(131072L);
        }
    }

    private void hibernationProcess(AppStateManager.AppState.RunningProcess proc, String reason) {
        if (proc.getProcessRecord() == null) {
            return;
        }
        Trace.traceBegin(131072L, "hibernationProcess");
        if (forzenProcess(proc.getUid(), proc.getPid(), proc.getProcessName(), reason)) {
            EventLog.writeEvent((int) SmartPowerSettings.EVENT_TAGS, "power hibernation (" + proc.getPid() + ")" + proc.getProcessName() + "/" + proc.getUid() + " adj " + proc.getAdj());
        }
        compactProcess(proc.getPackageName(), proc);
        Trace.traceEnd(131072L);
    }

    private void activeApp(AppStateManager.AppState app, String reason) {
        if (isEnableFrozen()) {
            EventLog.writeEvent((int) SmartPowerSettings.EVENT_TAGS, "power active " + app.getPackageName() + "/" + app.getUid());
            this.mAppPowerResourceManager.resumeAppAllPowerResources(app.getUid());
        }
    }

    private void activeProcess(AppStateManager.AppState.RunningProcess proc, String reason) {
        Trace.traceBegin(131072L, "activeProcess");
        if (thawProcess(proc.getUid(), proc.getPid(), reason)) {
            EventLog.writeEvent((int) SmartPowerSettings.EVENT_TAGS, "power active  (" + proc.getPid() + ")" + proc.getProcessName() + "/" + proc.getUid());
        }
        Trace.traceEnd(131072L);
    }

    private void compactProcess(String packageName, AppStateManager.AppState.RunningProcess proc) {
        if (AppStateManager.isSystemApp(proc.getProcessRecord())) {
            return;
        }
        SystemPressureController.getInstance().compactBackgroundProcess(proc, false);
    }

    private boolean thawProcess(int uid, int pid, String reason) {
        if (pid <= 0 || !PowerFrozenManager.getInstance().isAllFrozenPid(pid)) {
            return false;
        }
        boolean done = PowerFrozenManager.getInstance().thawProcess(uid, pid, reason);
        return done;
    }

    private boolean forzenProcess(int uid, int pid, String processName, String reason) {
        if (pid <= 0 || PowerFrozenManager.getInstance().isAllFrozenPid(pid)) {
            return false;
        }
        boolean done = PowerFrozenManager.getInstance().frozenProcess(uid, pid, processName, reason);
        return done;
    }

    public boolean isProcessHibernationWhiteList(AppStateManager.AppState.RunningProcess proc) {
        return isProcessHibernationWhiteList(proc.getUid(), proc.getPid(), proc.getPackageName(), proc.getProcessName());
    }

    public boolean isAppHibernationWhiteList(AppStateManager.AppState appState) {
        return isAppHibernationWhiteList(appState.getUid(), appState.getPackageName());
    }

    public boolean isProcessHibernationWhiteList(int uid, int pid, String packageName, String processName) {
        if (isAppHibernationWhiteList(uid, packageName) || isProcessWhiteList(processName) || checkCtsProcess(processName) || isPidWhiteList(pid)) {
            return true;
        }
        return false;
    }

    public boolean isAppHibernationWhiteList(int uid, String packageName) {
        if (Process.isCoreUid(uid) || isPackageWhiteList(packageName) || isUidWhiteList(uid) || this.mPowerSavingStrategyControl.isNoRestrictApp(packageName)) {
            return true;
        }
        return false;
    }

    private boolean isEnableFrozen() {
        return PowerFrozenManager.isEnable();
    }

    private boolean isEnableIntercept() {
        return SmartPowerSettings.PROP_INTERCEPT_ENABLE || isSpeedTestMode() || isEnableFrozen();
    }

    private boolean checkCtsProcess(String processName) {
        return processName.startsWith("com.android.cts.") || processName.startsWith("android.app.cts.") || processName.startsWith("com.android.server.cts.") || processName.startsWith("com.android.RemoteDPC") || processName.startsWith("com.google.android.gts") || processName.startsWith("android.jobscheduler.cts");
    }

    private boolean isPackageWhiteList(String packageName) {
        synchronized (this.mPackageWhiteList) {
            WhiteListItem item = this.mPackageWhiteList.get(packageName);
            if (item != null && (item.hasAction(2) || (!this.mScreenOff && item.hasAction(4)))) {
                return true;
            }
            return false;
        }
    }

    private boolean isUidWhiteList(int uid) {
        synchronized (this.mUidWhiteList) {
            WhiteListItem item = this.mUidWhiteList.get(Integer.valueOf(uid));
            if (item != null && item.hasAction(2)) {
                return true;
            }
            return false;
        }
    }

    public boolean isPackageWhiteList(int flag, String packageName) {
        synchronized (this.mPackageWhiteList) {
            WhiteListItem item = this.mPackageWhiteList.get(packageName);
            if (item != null && (item.mTypes & flag) != 0) {
                return true;
            }
            synchronized (this.mUidWhiteList) {
                Iterator<WhiteListItem> it = this.mUidWhiteList.values().iterator();
                while (true) {
                    if (!it.hasNext()) {
                        break;
                    }
                    WhiteListItem item2 = it.next();
                    if (item2.mName.equals(packageName)) {
                        if (item2.hasWhiteListType(flag)) {
                            return true;
                        }
                    }
                }
                return false;
            }
        }
    }

    private boolean isProcessWhiteList(String procName) {
        synchronized (this.mProcessWhiteList) {
            WhiteListItem item = this.mProcessWhiteList.get(procName);
            if (item != null && item.hasAction(2)) {
                return true;
            }
            return false;
        }
    }

    private boolean isPidWhiteList(int pid) {
        synchronized (this.mPidWhiteList) {
            WhiteListItem item = this.mPidWhiteList.get(Integer.valueOf(pid));
            if (item != null && item.hasAction(2)) {
                return true;
            }
            return false;
        }
    }

    private boolean hasPidWhiteList(int uid) {
        synchronized (this.mPidWhiteList) {
            for (WhiteListItem item : this.mPidWhiteList.values()) {
                if (item.mUid == uid && item.hasAction(2)) {
                    return true;
                }
            }
            return false;
        }
    }

    public boolean isProcessWhiteList(int flag, String procName) {
        synchronized (this.mProcessWhiteList) {
            WhiteListItem item = this.mProcessWhiteList.get(procName);
            if (item != null && (item.mTypes & flag) != 0) {
                return true;
            }
            synchronized (this.mPidWhiteList) {
                Iterator<WhiteListItem> it = this.mPidWhiteList.values().iterator();
                while (true) {
                    if (!it.hasNext()) {
                        break;
                    }
                    WhiteListItem item2 = it.next();
                    if (item2.mName.equals(procName)) {
                        if (item2.hasWhiteListType(flag)) {
                            return true;
                        }
                    }
                }
                return false;
            }
        }
    }

    public boolean isProcessWhiteList(int flag, String packageName, String procName) {
        if (isPackageWhiteList(flag, packageName)) {
            return true;
        }
        return isProcessWhiteList(flag, procName);
    }

    private boolean isBroadcastProcessWhiteList(String processName, String action) {
        boolean contains;
        synchronized (this.mBroadcastProcessActionWhiteList) {
            contains = this.mBroadcastProcessActionWhiteList.contains(processName + action);
        }
        return contains;
    }

    public UsageStatsInfo getUsageStatsInfo(String packageName) {
        UsageStatsInfo info;
        synchronized (this.mUsageStatsMap) {
            info = this.mUsageStatsMap.get(packageName);
        }
        if (info == null) {
            if (this.mUsageStatsMap.isEmpty()) {
                this.mHandler.removeMessages(1);
                this.mHandler.sendEmptyMessage(1);
            }
            return new UsageStatsInfo(packageName);
        }
        return info;
    }

    public void updateUsageStats() {
        Trace.traceBegin(131072L, "updateUsageStats");
        List<UsageStats> usageStats = queryUsageStats();
        if (usageStats == null || usageStats.isEmpty()) {
            Trace.traceEnd(131072L);
            return;
        }
        HashMap<String, UsageStatsInfo> usageStatsMap = new HashMap<>();
        for (UsageStats st : usageStats) {
            UsageStatsInfo info = usageStatsMap.get(st.mPackageName);
            if (info == null) {
                usageStatsMap.put(st.mPackageName, new UsageStatsInfo(st));
            } else {
                info.updateStatsInfo(st);
            }
        }
        synchronized (this.mUsageStatsMap) {
            this.mUsageStatsMap.clear();
            this.mUsageStatsMap.putAll(usageStatsMap);
        }
        this.mHandler.removeMessages(1);
        this.mHandler.sendEmptyMessageDelayed(1, UPDATE_USAGESTATS_DURATION);
        Trace.traceEnd(131072L);
    }

    private List<UsageStats> queryUsageStats() {
        long endTime = System.currentTimeMillis();
        long beginTime = endTime - 604800000;
        return this.mUsageStats.queryUsageStatsForUser(UserHandle.getCallingUserId(), 1, beginTime, endTime, false);
    }

    private void addUidWhiteList(final int uid, String pkgName, int type) {
        WhiteListItem item;
        synchronized (this.mUidWhiteList) {
            item = addDynamicWhiteListLocked(this.mUidWhiteList, Integer.valueOf(uid), pkgName, type, 2);
        }
        if (item != null) {
            item.mUid = uid;
            this.mHandler.post(new Runnable() { // from class: com.miui.server.smartpower.SmartPowerPolicyManager.2
                @Override // java.lang.Runnable
                public void run() {
                    SmartPowerPolicyManager.this.mAppStateManager.onAddToWhiteList(uid);
                }
            });
        }
    }

    private void removeUidWhiteList(final int uid, String pkgName, int type) {
        WhiteListItem item;
        synchronized (this.mUidWhiteList) {
            item = removeDynamicWhiteListLocked(this.mUidWhiteList, Integer.valueOf(uid), pkgName, type, 2);
        }
        if (item != null) {
            this.mHandler.post(new Runnable() { // from class: com.miui.server.smartpower.SmartPowerPolicyManager.3
                @Override // java.lang.Runnable
                public void run() {
                    SmartPowerPolicyManager.this.mAppStateManager.onRemoveFromWhiteList(uid);
                }
            });
        }
    }

    private void addPackageWhiteList(String pkgName, int type) {
        synchronized (this.mPackageWhiteList) {
            addStaticWhiteListLocked(this.mPackageWhiteList, pkgName, type, 2);
        }
    }

    private void addPidWhiteList(final int uid, final int pid, String procName, int type) {
        WhiteListItem item;
        synchronized (this.mPidWhiteList) {
            item = addDynamicWhiteListLocked(this.mPidWhiteList, Integer.valueOf(pid), procName, type, 2);
        }
        if (item != null) {
            item.mUid = uid;
            item.mPid = pid;
            this.mHandler.post(new Runnable() { // from class: com.miui.server.smartpower.SmartPowerPolicyManager.4
                @Override // java.lang.Runnable
                public void run() {
                    SmartPowerPolicyManager.this.mAppStateManager.onAddToWhiteList(uid, pid);
                }
            });
        }
    }

    private void removePidWhiteList(final int uid, final int pid, String procName, int type) {
        WhiteListItem item;
        synchronized (this.mPidWhiteList) {
            item = removeDynamicWhiteListLocked(this.mPidWhiteList, Integer.valueOf(pid), procName, type, 2);
        }
        if (item != null) {
            this.mHandler.post(new Runnable() { // from class: com.miui.server.smartpower.SmartPowerPolicyManager.5
                @Override // java.lang.Runnable
                public void run() {
                    SmartPowerPolicyManager.this.mAppStateManager.onRemoveFromWhiteList(uid, pid);
                }
            });
        }
    }

    private void addProcessWhiteList(String procName, int type) {
        synchronized (this.mProcessWhiteList) {
            addStaticWhiteListLocked(this.mProcessWhiteList, procName, type, 2);
        }
    }

    private void addSreenonWhiteList(String pkgName, int type) {
        synchronized (this.mPackageWhiteList) {
            addStaticWhiteListLocked(this.mPackageWhiteList, pkgName, type, 4);
        }
    }

    private void addServicePackageWhiteList(String pkgName) {
        synchronized (this.mPackageWhiteList) {
            addStaticWhiteListLocked(this.mPackageWhiteList, pkgName, WHITE_LIST_TYPE_SERVICE_DEFAULT, 16);
        }
    }

    public boolean isServicePackageWhiteList(String pkgName) {
        synchronized (this.mPackageWhiteList) {
            WhiteListItem item = this.mPackageWhiteList.get(pkgName);
            if (item != null && item.hasAction(16)) {
                return true;
            }
            return false;
        }
    }

    private void addProviderPackageWhiteList(String pkgName) {
        synchronized (this.mPackageWhiteList) {
            addStaticWhiteListLocked(this.mPackageWhiteList, pkgName, WHITE_LIST_TYPE_PROVIDER_DEFAULT, 32);
        }
    }

    public boolean isProviderPackageWhiteList(String pkgName) {
        synchronized (this.mPackageWhiteList) {
            WhiteListItem item = this.mPackageWhiteList.get(pkgName);
            if (item != null && item.hasAction(32)) {
                return true;
            }
            return false;
        }
    }

    private void addAlarmPackageWhiteList(String pkgname) {
        synchronized (this.mPackageWhiteList) {
            addStaticWhiteListLocked(this.mPackageWhiteList, pkgname, 32768, 8);
        }
    }

    public boolean isAlarmPackageWhiteList(String pkgname) {
        synchronized (this.mPackageWhiteList) {
            WhiteListItem item = this.mPackageWhiteList.get(pkgname);
            if (item != null && item.hasAction(8)) {
                return true;
            }
            return false;
        }
    }

    private void addStaticWhiteListLocked(ArrayMap<String, WhiteListItem> whiteList, String name, int type, int action) {
        WhiteListItem item = whiteList.get(name);
        if (item == null) {
            whiteList.put(name, new WhiteListItem(name, type, action));
            return;
        }
        item.mTypes |= type;
        item.mActions |= action;
    }

    private WhiteListItem addDynamicWhiteListLocked(ArrayMap<Integer, WhiteListItem> whiteList, Integer key, String name, int type, int action) {
        WhiteListItem item = whiteList.get(key);
        if (item == null) {
            WhiteListItem item2 = new WhiteListItem(name, type, action);
            whiteList.put(key, item2);
            return item2;
        }
        item.mTypes |= type;
        item.mActions |= action;
        return null;
    }

    private WhiteListItem removeDynamicWhiteListLocked(ArrayMap<Integer, WhiteListItem> whiteList, Integer key, String name, int type, int action) {
        WhiteListItem item = whiteList.get(key);
        if (item != null) {
            item.mTypes &= ~type;
            if ((item.mTypes & getWhiteListTypeMask(action)) == 0) {
                item.mActions &= ~action;
            }
            if (item.mTypes == 0 && item.mActions == 0) {
                whiteList.remove(key);
                return item;
            }
            return null;
        }
        return null;
    }

    private void addBroadcastProcessActionWhiteList(String processName, String action) {
        addBroadcastProcessActionWhiteList(processName + action);
    }

    private void addBroadcastProcessActionWhiteList(String processAction) {
        synchronized (this.mBroadcastProcessActionWhiteList) {
            this.mBroadcastProcessActionWhiteList.add(processAction);
        }
    }

    private void addBroadcastBlackList(String intent) {
        synchronized (this.mBroadcastBlackList) {
            this.mBroadcastBlackList.add(intent);
        }
    }

    public boolean isBroadcastBlackList(String intent) {
        boolean contains;
        synchronized (this.mBroadcastBlackList) {
            contains = this.mBroadcastBlackList.contains(intent);
        }
        return contains;
    }

    private void addBroadcastWhiteList(String intent) {
        synchronized (this.mBroadcastWhiteList) {
            this.mBroadcastWhiteList.add(intent);
        }
    }

    public boolean isBroadcastWhiteList(String intent) {
        boolean contains;
        synchronized (this.mBroadcastWhiteList) {
            contains = this.mBroadcastWhiteList.contains(intent);
        }
        return contains;
    }

    public static String whiteListTypeToString(int type) {
        String typeStr = "";
        if ((type & 2) != 0 || (type & 4096) != 0 || (32768 & type) != 0 || (262144 & type) != 0 || (2097152 & type) != 0) {
            typeStr = typeStr + "default, ";
        }
        if ((type & 4) != 0 || (type & 8192) != 0 || (65536 & type) != 0 || (524288 & type) != 0 || (4194304 & type) != 0) {
            typeStr = typeStr + "cloud control, ";
        }
        if ((type & 8) != 0) {
            typeStr = typeStr + "vpn, ";
        }
        if ((type & 16) != 0) {
            typeStr = typeStr + "wallpaper, ";
        }
        if ((type & 32) != 0) {
            typeStr = typeStr + "frequent thaw, ";
        }
        if ((type & 64) != 0) {
            typeStr = typeStr + "backup, ";
        }
        if ((type & 128) != 0) {
            typeStr = typeStr + "usb, ";
        }
        if ((type & 256) != 0) {
            typeStr = typeStr + "depend, ";
        }
        if ((type & 512) != 0) {
            typeStr = typeStr + "external audio focus policy, ";
        }
        if ((type & 1024) != 0) {
            return typeStr + "cts, ";
        }
        return typeStr;
    }

    private static int getWhiteListTypeMask(int action) {
        switch (action) {
            case 2:
                return WHITE_LIST_TYPE_HIBERNATION_MASK;
            case 4:
                return WHITE_LIST_TYPE_SCREENON_MASK;
            case 8:
                return WHITE_LIST_TYPE_ALARM_MASK;
            case 16:
                return WHITE_LIST_TYPE_SERVICE_MASK;
            case 32:
                return WHITE_LIST_TYPE_PROVIDER_MASK;
            default:
                return 0;
        }
    }

    public static String whiteListActionToString(int action) {
        String actionStr = "";
        if ((action & 2) != 0) {
            actionStr = actionStr + "hibernation, ";
        }
        if ((action & 4) != 0) {
            actionStr = actionStr + "screenon, ";
        }
        if ((action & 8) != 0) {
            actionStr = actionStr + "alarm, ";
        }
        if ((action & 16) != 0) {
            actionStr = actionStr + "service, ";
        }
        if ((action & 32) != 0) {
            return actionStr + "provider, ";
        }
        return actionStr;
    }

    public void dump(PrintWriter pw, String[] args, int opti) {
        if (opti + 1 < args.length) {
            pw.println("add white list:");
            int opti2 = opti + 1;
            String parm = args[opti];
            int opti3 = opti2 + 1;
            String value = args[opti2];
            if (parm.contains("addProviderPkg")) {
                addProviderPackageWhiteList(value);
            } else if (parm.contains("addBroadcastProc")) {
                addBroadcastProcessActionWhiteList(value);
            } else if (parm.contains("addFrozenPkg")) {
                addPackageWhiteList(value, 4);
            } else {
                pw.println("The parameter is invalid");
                return;
            }
            opti = opti3;
        }
        pw.println("PackageWhiteList:");
        synchronized (this.mPackageWhiteList) {
            Iterator<Map.Entry<String, WhiteListItem>> it = this.mPackageWhiteList.entrySet().iterator();
            while (it.hasNext()) {
                pw.println("    " + it.next().getValue());
            }
        }
        pw.println("ProcessWhiteList:");
        synchronized (this.mProcessWhiteList) {
            Iterator<Map.Entry<String, WhiteListItem>> it2 = this.mProcessWhiteList.entrySet().iterator();
            while (it2.hasNext()) {
                pw.println("    " + it2.next().getValue());
            }
        }
        pw.println("UidWhiteList:");
        synchronized (this.mUidWhiteList) {
            Iterator<Map.Entry<Integer, WhiteListItem>> it3 = this.mUidWhiteList.entrySet().iterator();
            while (it3.hasNext()) {
                pw.println("    " + it3.next().getValue());
            }
        }
        pw.println("PidWhiteList:");
        synchronized (this.mPidWhiteList) {
            Iterator<Map.Entry<Integer, WhiteListItem>> it4 = this.mPidWhiteList.entrySet().iterator();
            while (it4.hasNext()) {
                pw.println("    " + it4.next().getValue());
            }
        }
        pw.println("BroadcastWhiteList:");
        synchronized (this.mBroadcastWhiteList) {
            for (int i = 0; i < this.mBroadcastWhiteList.size(); i++) {
                pw.println("    " + this.mBroadcastWhiteList.valueAt(i));
            }
        }
        pw.println("BroadcastProcessWhiteList:");
        synchronized (this.mBroadcastProcessActionWhiteList) {
            for (int i2 = 0; i2 < this.mBroadcastProcessActionWhiteList.size(); i2++) {
                pw.println("    " + this.mBroadcastProcessActionWhiteList.valueAt(i2));
            }
        }
        pw.println("Dependency:");
        synchronized (this.mDependencyMap) {
            for (Map.Entry<Integer, ArraySet<String>> entry : this.mDependencyMap.entrySet()) {
                pw.println("    " + entry.getKey() + " " + entry.getValue());
            }
        }
        pw.println("UsageStats:");
        synchronized (this.mUsageStatsMap) {
            for (UsageStatsInfo info : this.mUsageStatsMap.values()) {
                info.dump(pw, args, opti);
            }
        }
    }

    public boolean isNoRestrictApp(String packageName) {
        return this.mPowerSavingStrategyControl.isNoRestrictApp(packageName);
    }

    private void registerCTSModeObserver() {
        ContentObserver observer = new ContentObserver(null) { // from class: com.miui.server.smartpower.SmartPowerPolicyManager.6
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                SmartPowerPolicyManager.sCtsModeEnable = !SystemProperties.getBoolean(SmartPowerPolicyManager.PROP_MIUI_OPTIMIZATION, !SplitScreenReporter.ACTION_ENTER_SPLIT.equals(SystemProperties.get("ro.miui.cts")));
            }
        };
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(MiuiSettings.Secure.MIUI_OPTIMIZATION), false, observer, -2);
        observer.onChange(false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class MyHandler extends Handler {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        MyHandler(Looper looper) {
            super(looper);
            SmartPowerPolicyManager.this = r1;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    SmartPowerPolicyManager.this.updateUsageStats();
                    return;
                default:
                    return;
            }
        }
    }

    /* loaded from: classes.dex */
    public class WhiteListItem {
        private int mActions;
        private String mName;
        private int mTypes;
        private int mUid = -1;
        private int mPid = -1;

        WhiteListItem(String name, int type, int action) {
            SmartPowerPolicyManager.this = this$0;
            this.mName = name;
            this.mTypes = type;
            this.mActions = action;
        }

        public boolean hasWhiteListType(int type) {
            return (this.mTypes & type) != 0;
        }

        public boolean hasAction(int action) {
            return (this.mActions & action) != 0;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(128);
            sb.append(this.mName);
            if (this.mUid != -1) {
                sb.append("(u:");
                sb.append(this.mUid);
                if (this.mPid != -1) {
                    sb.append(", p:");
                    sb.append(this.mPid);
                }
                sb.append(")");
            }
            sb.append(" {");
            sb.append(SmartPowerPolicyManager.whiteListTypeToString(this.mTypes));
            sb.append("} {");
            sb.append(SmartPowerPolicyManager.whiteListActionToString(this.mActions));
            sb.append("}");
            return sb.toString();
        }
    }

    /* loaded from: classes.dex */
    public class UsageStatsInfo {
        private String mPackageName;
        private long mTotalTimeInForeground = 0;
        private long mTotalTimeVisible = 0;
        private long mLastTimeVisible = 0;
        private int mAppLaunchCount = 0;
        private long mBeginTimeStamp = 0;
        private long mEndTimeStamp = 0;
        private long mDurationHours = 0;

        UsageStatsInfo(String pakageName) {
            SmartPowerPolicyManager.this = this$0;
            this.mPackageName = pakageName;
        }

        UsageStatsInfo(UsageStats stats) {
            SmartPowerPolicyManager.this = this$0;
            updateStatsInfo(stats);
        }

        void updateStatsInfo(UsageStats stats) {
            long firstInstallTime = 0;
            this.mPackageName = stats.mPackageName;
            try {
                firstInstallTime = SmartPowerPolicyManager.this.mContext.getPackageManager().getPackageInfo(this.mPackageName, 0).firstInstallTime;
            } catch (PackageManager.NameNotFoundException e) {
            }
            this.mTotalTimeInForeground += stats.mTotalTimeInForeground;
            this.mTotalTimeVisible += stats.mTotalTimeVisible;
            this.mAppLaunchCount += stats.mAppLaunchCount;
            if (this.mBeginTimeStamp == 0) {
                this.mBeginTimeStamp = Math.max(stats.mBeginTimeStamp, firstInstallTime);
            } else {
                long beginTimeStamp = Math.max(stats.mBeginTimeStamp, firstInstallTime);
                this.mBeginTimeStamp = Math.min(beginTimeStamp, this.mBeginTimeStamp);
            }
            long beginTimeStamp2 = stats.mEndTimeStamp;
            long max = Math.max(beginTimeStamp2, this.mEndTimeStamp);
            this.mEndTimeStamp = max;
            this.mDurationHours = (max - this.mBeginTimeStamp) / SyncManagerStubImpl.SYNC_DELAY_ON_DISALLOW_METERED;
            this.mLastTimeVisible = stats.mLastTimeVisible;
        }

        public int getAppLaunchCount() {
            return this.mAppLaunchCount;
        }

        public long getLastTimeVisible() {
            return this.mLastTimeVisible;
        }

        public void dump(PrintWriter pw, String[] args, int opti) {
            if (this.mAppLaunchCount > 0) {
                pw.println("#" + this.mPackageName + " dur:" + this.mDurationHours + "H vis=" + this.mTotalTimeVisible + "ms top=" + this.mTotalTimeInForeground + "ms launch=" + this.mAppLaunchCount);
            }
        }

        public String toString() {
            return this.mPackageName + " dur:" + this.mDurationHours + "H vis=" + this.mTotalTimeVisible + "ms top=" + this.mTotalTimeInForeground + "ms launch=" + this.mAppLaunchCount;
        }

        public void reset() {
            this.mTotalTimeInForeground = 0L;
            this.mTotalTimeVisible = 0L;
            this.mAppLaunchCount = 0;
            this.mBeginTimeStamp = 0L;
            this.mEndTimeStamp = 0L;
            this.mDurationHours = 0L;
        }
    }

    /* loaded from: classes.dex */
    public class PowerSavingStrategyControl {
        private static final String DB_AUTHORITY = "com.miui.powerkeeper.configure";
        private static final String DB_COL_BG_CONTROL = "bgControl";
        private static final String DB_COL_PACKAGE_NAME = "pkgName";
        private static final String DB_COL_USER_ID = "userId";
        public static final String DB_TABLE = "userTable";
        private static final String POLICY_NO_RESTRICT = "noRestrict";
        private final Uri CONTENT_URI;
        private ArraySet<String> mNoRestrictApps = new ArraySet<>();
        private boolean mIsInit = false;

        PowerSavingStrategyControl() {
            SmartPowerPolicyManager.this = r4;
            Uri withAppendedPath = Uri.withAppendedPath(Uri.parse("content://com.miui.powerkeeper.configure"), DB_TABLE);
            this.CONTENT_URI = withAppendedPath;
            ContentResolver cr = r4.mContext.getContentResolverForUser(UserHandle.SYSTEM);
            cr.registerContentObserver(withAppendedPath, true, new PolicyChangeObserver(r4.mHandler));
        }

        /* JADX INFO: Access modifiers changed from: private */
        /* loaded from: classes.dex */
        public class PolicyChangeObserver extends ContentObserver {
            /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
            public PolicyChangeObserver(Handler handler) {
                super(handler);
                PowerSavingStrategyControl.this = r1;
            }

            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                super.onChange(selfChange, uri);
                ContentResolver cr = SmartPowerPolicyManager.this.mContext.getContentResolverForUser(UserHandle.SYSTEM);
                Cursor cursor = cr.query(uri, null, null, null, null);
                if (cursor == null) {
                    return;
                }
                try {
                    if (cursor.getCount() > 0) {
                        int pkgIndex = cursor.getColumnIndex(PowerSavingStrategyControl.DB_COL_PACKAGE_NAME);
                        int bgControlIndex = cursor.getColumnIndex(PowerSavingStrategyControl.DB_COL_BG_CONTROL);
                        cursor.moveToFirst();
                        if (!cursor.isNull(bgControlIndex)) {
                            String pkg = cursor.getString(pkgIndex);
                            String bgControl = cursor.getString(bgControlIndex);
                            if (PowerSavingStrategyControl.POLICY_NO_RESTRICT.equals(bgControl)) {
                                PowerSavingStrategyControl.this.mNoRestrictApps.add(pkg);
                            } else {
                                PowerSavingStrategyControl.this.mNoRestrictApps.remove(pkg);
                            }
                        }
                    }
                } catch (Exception e) {
                } catch (Throwable th) {
                    cursor.close();
                    throw th;
                }
                cursor.close();
            }
        }

        public boolean isNoRestrictApp(String packageName) {
            return this.mNoRestrictApps.contains(packageName);
        }

        public void updateAllNoRestrictApps() {
            String[] projection = {DB_COL_PACKAGE_NAME};
            String[] selectionArgs = {Integer.toString(0), POLICY_NO_RESTRICT};
            ContentResolver cr = SmartPowerPolicyManager.this.mContext.getContentResolverForUser(UserHandle.SYSTEM);
            Cursor cursor = cr.query(this.CONTENT_URI, projection, "userId = ? AND bgControl = ?", selectionArgs, null);
            if (cursor == null) {
                return;
            }
            this.mIsInit = true;
            this.mNoRestrictApps.clear();
            try {
                try {
                    int bgPkgIndex = cursor.getColumnIndex(DB_COL_PACKAGE_NAME);
                    while (cursor.moveToNext()) {
                        String app = cursor.getString(bgPkgIndex);
                        this.mNoRestrictApps.add(app);
                    }
                } catch (Exception e) {
                    this.mIsInit = false;
                }
            } finally {
                cursor.close();
            }
        }
    }
}
