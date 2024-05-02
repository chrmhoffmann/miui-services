package com.android.server.am;

import android.app.ActivityManager;
import android.app.ActivityTaskManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ServiceInfo;
import android.database.ContentObserver;
import android.location.ILocationListener;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.ShellCommand;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Slog;
import android.view.RemoteAnimationTarget;
import android.view.SurfaceControl;
import android.view.WindowManager;
import com.android.internal.util.DumpUtils;
import com.android.server.LocalServices;
import com.android.server.am.AppStateManager;
import com.android.server.am.PendingIntentRecord;
import com.android.server.am.SystemPressureController;
import com.android.server.display.DisplayModeDirector;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.wm.ActivityRecord;
import com.android.server.wm.ActivityTaskManagerService;
import com.android.server.wm.WindowManagerService;
import com.miui.base.MiuiStubRegistry;
import com.miui.base.MiuiStubUtil;
import com.miui.server.smartpower.AppPowerResourceManager;
import com.miui.server.smartpower.ComponentsManager;
import com.miui.server.smartpower.PowerFrozenManager;
import com.miui.server.smartpower.SmartCpuPolicyManager;
import com.miui.server.smartpower.SmartDisplayPolicyManager;
import com.miui.server.smartpower.SmartPowerPolicyManager;
import com.miui.server.smartpower.SmartPowerSettings;
import com.miui.server.smartpower.SmartScenarioManager;
import com.miui.server.smartpower.SmartThermalPolicyManager;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import miui.security.CallerInfo;
import miui.smartpower.IScenarioCallback;
import miui.smartpower.ISmartPowerManager;
/* loaded from: classes.dex */
public class SmartPowerService extends ISmartPowerManager.Stub implements SmartPowerServiceStub {
    private static final String CLOUD_ALARM_WHITE_LIST = "power_alarm_white_list";
    private static final String CLOUD_BROADCAST_WHITE_LIST = "power_broadcast_white_list";
    private static final String CLOUD_CONTROL_URI = "perf_shielder_smartpower";
    private static final String CLOUD_DISPLAY_ENABLE = "perf_power_display_enable";
    private static final String CLOUD_ENABLE = "perf_power_appState_enable";
    private static final String CLOUD_FROZEN_ENABLE = "perf_power_freeze_enable";
    private static final String CLOUD_INTERCEPT_ENABLE = "perf_power_intercept_enable";
    private static final String CLOUD_PKG_WHITE_LIST = "power_pkg_white_list";
    private static final String CLOUD_PROC_WHITE_LIST = "power_proc_white_list";
    private static final String CLOUD_PROVIDER_WHITE_LIST = "power_provider_white_list";
    private static final String CLOUD_SCREENON_WHITE_LIST = "power_screenon_white_list";
    private static final String CLOUD_SERVICE_WHITE_LIST = "power_service_white_list";
    public static final boolean DEBUG = SmartPowerSettings.DEBUG_ALL;
    public static final String SERVICE_NAME = "smartpower";
    public static final String TAG = "SmartPower";
    public static final int THREAD_GROUP_FOREGROUND = 1;
    private ActivityManagerService mAMS;
    private ActivityTaskManagerService mATMS;
    private Context mContext;
    private MyHandler mHandler;
    private PackageManagerInternal mPackageManager;
    private SmartCpuPolicyManager mSmartCpuPolicyManager;
    private WindowManagerService mWMS;
    private AppStateManager mAppStateManager = null;
    private SmartPowerPolicyManager mSmartPowerPolicyManager = null;
    private AppPowerResourceManager mAppPowerResourceManager = null;
    private ComponentsManager mComponentsManager = null;
    private SmartScenarioManager mSmartScenarioManager = null;
    private SmartThermalPolicyManager mSmartThermalPolicyManager = null;
    private SmartDisplayPolicyManager mSmartDisplayPolicyManager = null;
    private HandlerThread mHandlerTh = new HandlerThread(SmartPowerPolicyManager.REASON, -2);
    private boolean mSystemReady = false;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<SmartPowerService> {

        /* compiled from: SmartPowerService$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final SmartPowerService INSTANCE = new SmartPowerService();
        }

        public SmartPowerService provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public SmartPowerService provideNewInstance() {
            return new SmartPowerService();
        }
    }

    /* loaded from: classes.dex */
    public class MyHandler extends Handler {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public MyHandler(Looper looper) {
            super(looper);
            SmartPowerService.this = r1;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int i = msg.what;
        }
    }

    void systemReady(Context context) {
        this.mContext = context;
        this.mHandlerTh.start();
        this.mAMS = ActivityManager.getService();
        this.mATMS = ActivityTaskManager.getService();
        this.mWMS = ServiceManager.getService("window");
        this.mPackageManager = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        this.mHandler = new MyHandler(this.mHandlerTh.getLooper());
        Process.setThreadGroupAndCpuset(this.mHandlerTh.getThreadId(), 1);
        this.mSmartPowerPolicyManager = new SmartPowerPolicyManager(context, this.mHandlerTh.getLooper(), this.mAMS);
        this.mAppPowerResourceManager = new AppPowerResourceManager(context, this.mHandlerTh.getLooper());
        this.mAppStateManager = new AppStateManager(context, this.mHandlerTh.getLooper(), this.mAMS);
        this.mComponentsManager = new ComponentsManager(context, this.mHandlerTh.getLooper());
        this.mSmartCpuPolicyManager = new SmartCpuPolicyManager(context, this.mAMS);
        SmartScenarioManager smartScenarioManager = new SmartScenarioManager(context, this.mHandlerTh.getLooper());
        this.mSmartScenarioManager = smartScenarioManager;
        this.mSmartThermalPolicyManager = new SmartThermalPolicyManager(smartScenarioManager);
        this.mSmartDisplayPolicyManager = new SmartDisplayPolicyManager(context, this.mWMS);
        this.mAppStateManager.init(this.mAppPowerResourceManager, this.mSmartPowerPolicyManager, this.mSmartScenarioManager);
        this.mSmartPowerPolicyManager.init(this.mAppStateManager, this.mAppPowerResourceManager);
        this.mComponentsManager.init(this.mAppStateManager, this.mSmartPowerPolicyManager);
        this.mSmartCpuPolicyManager.init(this.mAppStateManager, this.mSmartPowerPolicyManager);
        this.mSmartDisplayPolicyManager.init();
        registerCloudObserver(this.mContext);
        updateCloudControlParas();
        this.mSystemReady = true;
    }

    public static SmartPowerService getInstance() {
        return (SmartPowerService) MiuiStubUtil.getImpl(SmartPowerServiceStub.class);
    }

    /* JADX WARN: Type inference failed for: r0v0, types: [com.android.server.am.SmartPowerService, android.os.IBinder] */
    public static void startService(Context context) {
        ?? smartPowerService = getInstance();
        smartPowerService.systemReady(context);
        ServiceManager.addService(SERVICE_NAME, (IBinder) smartPowerService);
    }

    private void checkPermission() {
        int uid = Binder.getCallingUid();
        if (1000 == uid) {
            return;
        }
        throw new SecurityException("Uid " + uid + " does not have permission to smartpower");
    }

    public void registThermalScenarioCallback(IScenarioCallback callback) {
        if (this.mSystemReady) {
            this.mSmartThermalPolicyManager.registThermalScenarioCallback(callback);
        }
    }

    public boolean isUidIdle(int uid) {
        if (this.mSystemReady) {
            return this.mAppStateManager.isUidIdle(uid);
        }
        return false;
    }

    public void playbackStateChanged(int uid, int pid, int oldState, int newState) {
        if (this.mSystemReady) {
            this.mAppPowerResourceManager.playbackStateChanged(uid, pid, oldState, newState);
        }
    }

    public void recordAudioFocus(int uid, int pid, String clientId, boolean request) {
        if (this.mSystemReady) {
            this.mAppPowerResourceManager.recordAudioFocus(uid, pid, clientId, request);
        }
    }

    public void recordAudioFocusLoss(int uid, String clientId, int focusLoss) {
        if (this.mSystemReady) {
            this.mAppPowerResourceManager.recordAudioFocusLoss(uid, clientId, focusLoss);
        }
    }

    public void onPlayerTrack(int uid, int pid, int piid, int sessionId) {
        if (this.mSystemReady) {
            this.mAppPowerResourceManager.onPlayerTrack(uid, pid, piid, sessionId);
        }
    }

    public void onPlayerRlease(int uid, int pid, int piid) {
        if (this.mSystemReady) {
            this.mAppPowerResourceManager.onPlayerRlease(uid, pid, piid);
        }
    }

    public void onPlayerEvent(int uid, int pid, int piid, int event) {
        if (this.mSystemReady) {
            this.mAppPowerResourceManager.onPlayerEvent(uid, pid, piid, event);
        }
    }

    public void onRecorderTrack(int uid, int pid, int riid) {
        if (this.mSystemReady) {
            this.mAppPowerResourceManager.onRecorderTrack(uid, pid, riid);
        }
    }

    public void onRecorderRlease(int uid, int riid) {
        if (this.mSystemReady) {
            this.mAppPowerResourceManager.onRecorderRlease(uid, riid);
        }
    }

    public void onRecorderEvent(int uid, int riid, int event) {
        if (this.mSystemReady) {
            this.mAppPowerResourceManager.onRecorderEvent(uid, riid, event);
        }
    }

    public void onExternalAudioRegister(int uid, int pid) {
        if (this.mSystemReady) {
            this.mSmartPowerPolicyManager.onExternalAudioRegister(uid, pid);
        }
    }

    public void uidAudioStatusChanged(int uid, boolean active) {
        if (this.mSystemReady) {
            this.mAppPowerResourceManager.uidAudioStatusChanged(uid, active);
        }
    }

    public void uidVideoStatusChanged(int uid, boolean active) {
        if (this.mSystemReady) {
            this.mAppPowerResourceManager.uidVideoStatusChanged(uid, active);
        }
    }

    public void onProcessStart(ProcessRecord r) {
        if (this.mSystemReady && r != null) {
            this.mAppStateManager.processStartedLocked(r, r.info.packageName);
        }
    }

    public void onProcessKill(ProcessRecord r) {
        if (this.mSystemReady && r != null) {
            this.mAppStateManager.processKilledLocked(r);
        }
    }

    public void updateProcess(ProcessRecord r) {
        if (this.mSystemReady && r != null) {
            this.mAppStateManager.updateProcessLocked(r);
        }
    }

    public void onActivityStartUnchecked(String name, int uid, int pid, String packageName, int launchedFromUid, int launchedFromPid, String launchedFromPackage, boolean isColdStart) {
        if (this.mSystemReady) {
            this.mComponentsManager.activityStartBeforeLocked(name, launchedFromPid, uid, packageName, isColdStart);
        }
    }

    public void onForegroundActivityChangedLocked(String name, int uid, int pid, String packageName, int launchedFromUid, int launchedFromPid, String launchedFromPackage, boolean isColdStart) {
        if (this.mSystemReady) {
            this.mComponentsManager.onForegroundActivityChangedLocked(launchedFromPid, uid, name);
            SystemPressureController.getInstance().foregroundActivityChanged(new SystemPressureController.ControllerActivityInfo(uid, pid, packageName, launchedFromUid, launchedFromPid, launchedFromPackage, isColdStart));
        }
    }

    public void onBroadcastStatusChanged(ProcessRecord processRecord, boolean active, Intent intent) {
        if (this.mSystemReady && processRecord != null) {
            this.mComponentsManager.broadcastStatusChangedLocked(processRecord, active, intent);
        }
    }

    public void onServiceStatusChanged(ProcessRecord processRecord, boolean active, ServiceRecord record, int execution) {
        if (this.mSystemReady && processRecord != null) {
            if (active && execution == 2) {
                for (ArrayList<ConnectionRecord> connList : record.getConnections().values()) {
                    Iterator<ConnectionRecord> it = connList.iterator();
                    while (it.hasNext()) {
                        ConnectionRecord conn = it.next();
                        onServiceConnectionChanged(conn.binding, true);
                    }
                }
            }
            this.mComponentsManager.serviceStatusChangedLocked(processRecord, active, record.shortInstanceName, execution);
        }
    }

    public boolean shouldInterceptProvider(int callingUid, ProcessRecord callingProc, ProcessRecord hostingProc, boolean isRunning) {
        if (this.mSystemReady && hostingProc != null) {
            return this.mSmartPowerPolicyManager.shouldInterceptProviderLocked(callingUid, callingProc, hostingProc, isRunning);
        }
        return false;
    }

    public void onContentProviderStatusChanged(int callingUid, ProcessRecord callingProc, ProcessRecord hostingProc, ContentProviderRecord record, boolean isRunning) {
        if (this.mSystemReady && hostingProc != null) {
            this.mComponentsManager.contentProviderStatusChangedLocked(hostingProc, record.toString());
        }
    }

    public void onServiceConnectionChanged(AppBindRecord bindRecord, boolean isConnect) {
        if (this.mSystemReady && bindRecord != null && bindRecord.client != null && bindRecord.service != null && bindRecord.intent != null) {
            if (!isConnect && bindRecord.intent.apps.containsKey(bindRecord.client)) {
                return;
            }
            ProcessRecord serviceApp = bindRecord.service.app;
            if (serviceApp == null) {
                int serviceFlag = bindRecord.service.serviceInfo.flags;
                if ((serviceFlag & 2) != 0) {
                    serviceApp = bindRecord.service.isolationHostProc;
                } else if (bindRecord.service.appInfo != null) {
                    serviceApp = this.mAMS.getProcessRecordLocked(bindRecord.service.processName, bindRecord.service.appInfo.uid);
                }
            }
            if (serviceApp != null && serviceApp.mPid > 0) {
                this.mComponentsManager.serviceConnectionChangedLocked(bindRecord.client, serviceApp, isConnect, bindRecord.intent.collectFlags());
            }
        }
    }

    public void onProviderConnectionChanged(ContentProviderConnection conn, boolean isConnect) {
        if (this.mSystemReady && conn != null && conn.client != null && conn.provider != null && conn.provider.proc != null) {
            this.mComponentsManager.providerConnectionChangedLocked(conn.client, conn.provider.proc, isConnect);
        }
    }

    public void onAlarmStatusChanged(int uid, boolean active) {
        if (this.mSystemReady) {
            this.mComponentsManager.alarmStatusChangedLocked(uid, active);
        }
    }

    public boolean shouldInterceptAlarm(int uid, int type) {
        if (this.mSystemReady) {
            return this.mSmartPowerPolicyManager.shouldInterceptAlarmLocked(uid, type);
        }
        return false;
    }

    public boolean shouldInterceptBroadcast(ProcessRecord recProc, BroadcastRecord record) {
        if (this.mSystemReady && recProc != null) {
            return this.mSmartPowerPolicyManager.shouldInterceptBroadcastLocked(record.callingUid, record.callingPid, record.callerPackage, recProc, record.intent, record.ordered, record.sticky);
        }
        return false;
    }

    public boolean skipFrozenAppAnr(ApplicationInfo info, int uid, String report) {
        if (this.mSystemReady && info != null) {
            return this.mSmartPowerPolicyManager.skipFrozenAppAnr(info, uid, report);
        }
        return false;
    }

    public boolean shouldInterceptService(Intent service, CallerInfo callingInfo, ServiceInfo serviceInfo) {
        if (this.mSystemReady && callingInfo != null && serviceInfo != null && serviceInfo.applicationInfo != null) {
            return this.mSmartPowerPolicyManager.shouldInterceptService(service, callingInfo, serviceInfo);
        }
        return false;
    }

    public void updateActivitiesVisibility() {
        if (this.mSystemReady) {
            this.mComponentsManager.updateActivitiesVisibilityLocked();
        }
    }

    public void onActivityVisibilityChanged(int uid, String processName, ActivityRecord record, boolean visible) {
        if (this.mSystemReady) {
            this.mComponentsManager.activityVisibilityChangedLocked(uid, processName, record, visible);
        }
    }

    public void onWindowVisibilityChanged(int uid, int pid, WindowManagerPolicy.WindowState win, WindowManager.LayoutParams attrs, boolean visible) {
        if (this.mSystemReady) {
            this.mComponentsManager.windowVisibilityChangedLocked(uid, pid, win, attrs, visible);
        }
    }

    public void applyOomAdjLocked(ProcessRecord app) {
        if (this.mSystemReady && app != null) {
            this.mComponentsManager.onApplyOomAdjLocked(app);
        }
    }

    public void onSendPendingIntent(PendingIntentRecord.Key key, String callingPkg, String targetPkg, Intent intent) {
        if (this.mSystemReady) {
            int uid = this.mPackageManager.getPackageUid(targetPkg, 0L, key.userId);
            this.mComponentsManager.onSendPendingIntent(uid, key.typeName());
        }
    }

    public void onMediaKey(int uid, int pid) {
        if (this.mSystemReady) {
            this.mAppStateManager.onMediaKey(uid, pid);
        }
    }

    public void onMediaKey(int uid) {
        if (this.mSystemReady) {
            this.mAppStateManager.onMediaKey(uid);
        }
    }

    public void onWallpaperChanged(boolean active, int uid, String packageName) {
        if (this.mSystemReady) {
            this.mSmartPowerPolicyManager.onWallpaperChangedLocked(active, uid, packageName);
        }
    }

    public void onVpnChanged(boolean active, int uid, String packageName) {
        if (this.mSystemReady) {
            this.mSmartPowerPolicyManager.onVpnChanged(active, uid, packageName);
        }
    }

    public void onBackupChanged(boolean active, ProcessRecord proc) {
        if (this.mSystemReady) {
            this.mSmartPowerPolicyManager.onBackupChanged(active, proc.uid, proc.info.packageName);
            this.mAppStateManager.onBackupChanged(active, proc);
        }
    }

    public void onUsbStateChanged(boolean isConnected, long functions, Intent intent) {
        if (this.mSystemReady) {
            boolean isUsbDataTrans = isUsbDataTransferActive(functions);
            this.mSmartPowerPolicyManager.onUsbStateChanged(isConnected, isUsbDataTrans);
        }
    }

    protected static boolean isUsbDataTransferActive(long functions) {
        return ((4 & functions) == 0 && (16 & functions) == 0) ? false : true;
    }

    public void onInputMethodShow(int uid) {
        if (this.mSystemReady) {
            this.mAppStateManager.onInputMethodShow(uid);
        }
    }

    public void onAcquireLocation(int uid, int pid, ILocationListener listener) {
        if (this.mSystemReady) {
            this.mAppPowerResourceManager.onAquireLocation(uid, pid, listener);
        }
    }

    public void onReleaseLocation(int uid, int pid, ILocationListener listener) {
        if (this.mSystemReady) {
            this.mAppPowerResourceManager.onReleaseLocation(uid, pid, listener);
        }
    }

    public void onAcquireWakelock(IBinder lock, int flags, String tag, int ownerUid, int ownerPid) {
        if (this.mSystemReady) {
            this.mAppPowerResourceManager.onAcquireWakelock(lock, flags, tag, ownerUid, ownerPid);
        }
    }

    public void onReleaseWakelock(IBinder lock, int flags) {
        if (this.mSystemReady) {
            this.mAppPowerResourceManager.onReleaseWakelock(lock, flags);
        }
    }

    public void onBluetoothEvent(boolean isConnect, int bleType, int uid, int pid, String pkg, int data) {
        int uid2 = uid;
        if (this.mSystemReady) {
            if (uid2 <= 0 && TextUtils.isEmpty(pkg)) {
                return;
            }
            ArrayList<Integer> pids = new ArrayList<>();
            AppStateManager.AppState appState = null;
            if (uid2 <= 0) {
                appState = this.mAppStateManager.getAppState(pkg);
            } else if (pid > 0) {
                pids.add(Integer.valueOf(pid));
            } else {
                appState = this.mAppStateManager.getAppState(uid2);
            }
            if (appState != null) {
                uid2 = appState.getUid();
                ArrayList<AppStateManager.AppState.RunningProcess> runningProcs = appState.getRunningProcessList();
                for (int i = 0; i < runningProcs.size(); i++) {
                    pids.add(Integer.valueOf(runningProcs.get(i).getPid()));
                }
            }
            Iterator<Integer> it = pids.iterator();
            while (it.hasNext()) {
                Integer proc = it.next();
                this.mAppPowerResourceManager.onBluetoothEvent(isConnect, bleType, uid2, proc.intValue(), data);
            }
        }
    }

    public void reportTrackStatus(int uid, int pid, int sessionId, boolean isMuted) {
        if (this.mSystemReady) {
            this.mAppPowerResourceManager.reportTrackStatus(uid, pid, sessionId, isMuted);
        }
    }

    public void notifyCameraForegroundState(String cameraId, boolean isForeground, String caller, int callerUid, int callerPid) {
        if (this.mSystemReady) {
            this.mAppPowerResourceManager.notifyCameraForegroundState(cameraId, isForeground, caller, callerUid, callerPid);
        }
    }

    private void registerCloudObserver(Context context) {
        ContentObserver observer = new ContentObserver(this.mHandler) { // from class: com.android.server.am.SmartPowerService.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                if (uri.equals(Settings.System.getUriFor(SmartPowerService.CLOUD_CONTROL_URI))) {
                    SmartPowerService.this.updateCloudControlParas();
                }
            }
        };
        context.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOUD_CONTROL_URI), false, observer, -2);
    }

    public void updateCloudControlParas() {
        String frozenEnableStr = getCloudControlData(CLOUD_FROZEN_ENABLE);
        if (!TextUtils.isEmpty(frozenEnableStr)) {
            boolean frozenEnable = Boolean.parseBoolean(frozenEnableStr);
            PowerFrozenManager.getInstance().syncCloudControlSettings(frozenEnable);
            SystemProperties.set(SmartPowerSettings.FROZEN_PROP, frozenEnableStr);
            logCloudControlParas(CLOUD_FROZEN_ENABLE, frozenEnableStr);
        }
        String appStateEnable = getCloudControlData(CLOUD_ENABLE);
        if (!TextUtils.isEmpty(appStateEnable)) {
            SystemProperties.set(SmartPowerSettings.APP_STATE_PROP, appStateEnable);
            logCloudControlParas(CLOUD_ENABLE, appStateEnable);
        }
        String interceptEnable = getCloudControlData(CLOUD_INTERCEPT_ENABLE);
        if (!TextUtils.isEmpty(interceptEnable)) {
            SystemProperties.set(SmartPowerSettings.INTERCEPT_PROP, interceptEnable);
            SmartPowerSettings.PROP_INTERCEPT_ENABLE = Boolean.parseBoolean(interceptEnable);
            logCloudControlParas(CLOUD_INTERCEPT_ENABLE, interceptEnable);
        }
        String dipslayEnable = getCloudControlData(CLOUD_DISPLAY_ENABLE);
        if (!TextUtils.isEmpty(dipslayEnable)) {
            SystemProperties.set(SmartPowerSettings.DISPLAY_POLICY_PROP, dipslayEnable);
            logCloudControlParas(CLOUD_DISPLAY_ENABLE, dipslayEnable);
        }
        String pkgWhiteListString = getCloudControlData(CLOUD_PKG_WHITE_LIST);
        this.mSmartPowerPolicyManager.updateCloudPackageWhiteList(pkgWhiteListString);
        logCloudControlParas(CLOUD_PKG_WHITE_LIST, pkgWhiteListString);
        String procWhiteListString = getCloudControlData(CLOUD_PROC_WHITE_LIST);
        this.mSmartPowerPolicyManager.updateCloudProcessWhiteList(procWhiteListString);
        logCloudControlParas(CLOUD_PROC_WHITE_LIST, procWhiteListString);
        String screenonWhiteListString = getCloudControlData(CLOUD_SCREENON_WHITE_LIST);
        this.mSmartPowerPolicyManager.updateCloudSreenonWhiteList(screenonWhiteListString);
        logCloudControlParas(CLOUD_SCREENON_WHITE_LIST, screenonWhiteListString);
        String broadcastWhiteListString = getCloudControlData(CLOUD_BROADCAST_WHITE_LIST);
        this.mSmartPowerPolicyManager.updateCloudBroadcastWhiteLit(broadcastWhiteListString);
        logCloudControlParas(CLOUD_BROADCAST_WHITE_LIST, broadcastWhiteListString);
        String alarmWhiteListString = getCloudControlData(CLOUD_ALARM_WHITE_LIST);
        this.mSmartPowerPolicyManager.updateCloudAlarmWhiteLit(alarmWhiteListString);
        logCloudControlParas(CLOUD_ALARM_WHITE_LIST, alarmWhiteListString);
        String providerWhiteListString = getCloudControlData(CLOUD_PROVIDER_WHITE_LIST);
        this.mSmartPowerPolicyManager.updateCloudProviderWhiteLit(providerWhiteListString);
        logCloudControlParas(CLOUD_PROVIDER_WHITE_LIST, providerWhiteListString);
        String serviceWhiteListString = getCloudControlData(CLOUD_SERVICE_WHITE_LIST);
        this.mSmartPowerPolicyManager.updateCloudServiceWhiteLit(serviceWhiteListString);
        logCloudControlParas(CLOUD_SERVICE_WHITE_LIST, serviceWhiteListString);
    }

    private String getCloudControlData(String key) {
        return Settings.System.getStringForUser(this.mContext.getContentResolver(), key, -2);
    }

    private void logCloudControlParas(String key, String data) {
        Slog.d("SmartPower", "sync cloud control " + key + " " + data);
    }

    public ArrayList<AppStateManager.AppState> getAllAppState() {
        if (this.mSystemReady) {
            return this.mAppStateManager.getAllAppState();
        }
        return null;
    }

    public ArrayList<AppStateManager.AppState.RunningProcess> getLruProcesses() {
        if (this.mSystemReady) {
            return this.mAppStateManager.getLruProcesses();
        }
        return null;
    }

    public ArrayList<AppStateManager.AppState.RunningProcess> getLruProcesses(int uid, String packageName) {
        if (this.mSystemReady) {
            return this.mAppStateManager.getLruProcesses(uid, packageName);
        }
        return null;
    }

    public AppStateManager.AppState.RunningProcess getRunningProcess(int uid, String processName) {
        if (this.mSystemReady) {
            return this.mAppStateManager.getRunningProcess(uid, processName);
        }
        return null;
    }

    public void hibernateAllIfNeeded(String reason) {
        if (this.mSystemReady) {
            this.mAppStateManager.hibernateAllIfNeeded(reason);
        }
    }

    public boolean isUidVisible(int uid) {
        if (this.mSystemReady) {
            return this.mAppStateManager.isUidVisible(uid);
        }
        return false;
    }

    public boolean isProcessPerceptible(int uid, String procName) {
        if (this.mSystemReady) {
            return this.mAppStateManager.isProcessPerceptible(uid, procName);
        }
        return false;
    }

    public boolean isProcessWhiteList(int flag, String packageName, String procName) {
        if (this.mSystemReady) {
            return this.mSmartPowerPolicyManager.isProcessWhiteList(flag, packageName, procName);
        }
        return false;
    }

    public boolean isAppAudioActive(int uid) {
        if (this.mSystemReady) {
            return this.mAppPowerResourceManager.isAppResActive(uid, 1);
        }
        return true;
    }

    public boolean isAppGpsActive(int uid) {
        if (this.mSystemReady) {
            return this.mAppPowerResourceManager.isAppResActive(uid, 3);
        }
        return true;
    }

    public boolean isAppAudioActive(int uid, int pid) {
        if (this.mSystemReady) {
            return this.mAppPowerResourceManager.isAppResActive(uid, pid, 1);
        }
        return true;
    }

    public long getLastMusicPlayTimeStamp(int pid) {
        if (this.mSystemReady) {
            return this.mAppPowerResourceManager.getLastMusicPlayTimeStamp(pid);
        }
        return 0L;
    }

    public boolean isAppGpsActive(int uid, int pid) {
        if (this.mSystemReady) {
            return this.mAppPowerResourceManager.isAppResActive(uid, pid, 3);
        }
        return true;
    }

    public long updateCpuStatsNow() {
        if (this.mSystemReady) {
            return this.mSmartCpuPolicyManager.updateCpuStatsNow();
        }
        return 0L;
    }

    public int getBackgroundCpuCoreNum() {
        if (this.mSystemReady) {
            return this.mSmartCpuPolicyManager.getBackgroundCpuCoreNum();
        }
        return 4;
    }

    protected void registerAppStateListener(AppStateManager.IProcessStateCallback callback) {
        if (this.mSystemReady) {
            this.mAppStateManager.registerAppStateListener(callback);
        }
    }

    protected void unRegisterAppStateListener(AppStateManager.IProcessStateCallback callback) {
        if (this.mSystemReady) {
            this.mAppStateManager.unRegisterAppStateListener(callback);
        }
    }

    public void onCpuPressureEvents(int level) {
        if (this.mSystemReady) {
            this.mSmartCpuPolicyManager.cpuPressureEvents(level);
        }
    }

    public void onCpuExceptionEvents(int type) {
        if (this.mSystemReady) {
            this.mSmartCpuPolicyManager.cpuExceptionEvents(type);
        }
    }

    public void onAppTransitionStartLocked(long animDuration) {
        if (this.mSystemReady) {
            this.mSmartDisplayPolicyManager.notifyAppTransitionStartLocked(animDuration);
        }
    }

    public void onWindowAnimationStartLocked(long animDuration, SurfaceControl animationLeash) {
        if (this.mSystemReady) {
            this.mSmartDisplayPolicyManager.notifyWindowAnimationStartLocked(animDuration, animationLeash);
        }
    }

    public void onInsetAnimationShow(int type) {
        if (this.mSystemReady) {
            this.mSmartDisplayPolicyManager.notifyInsetAnimationShow(type);
        }
    }

    public void onInsetAnimationHide(int type) {
        if (this.mSystemReady) {
            this.mSmartDisplayPolicyManager.notifyInsetAnimationHide(type);
        }
    }

    public void onRecentsAnimationStart(ActivityRecord targetActivity) {
        if (this.mSystemReady) {
            this.mSmartDisplayPolicyManager.notifyRecentsAnimationStart(targetActivity);
        }
    }

    public void onRecentsAnimationEnd() {
        if (this.mSystemReady) {
            this.mSmartDisplayPolicyManager.notifyRecentsAnimationEnd();
        }
    }

    public void onRemoteAnimationStart(RemoteAnimationTarget[] appTargets) {
        if (this.mSystemReady) {
            this.mSmartDisplayPolicyManager.notifyRemoteAnimationStart(appTargets);
        }
    }

    public void onRemoteAnimationEnd() {
        if (this.mSystemReady) {
            this.mSmartDisplayPolicyManager.notifyRemoteAnimationEnd();
        }
    }

    public void onFocusedWindowChangeLocked(String oldPackage, String newPackage) {
        if (this.mSystemReady) {
            this.mSmartDisplayPolicyManager.notifyFocusedWindowChangeLocked(oldPackage, newPackage);
        }
    }

    public void onDisplayDeviceStateChangeLocked(int deviceState) {
        if (this.mSystemReady) {
            this.mSmartDisplayPolicyManager.notifyDisplayDeviceStateChangeLocked(deviceState);
        }
    }

    public void onDisplaySwitchResolutionLocked(int width, int height, int density) {
        if (this.mSystemReady) {
            this.mSmartDisplayPolicyManager.notifyDisplaySwitchResolutionLocked(width, height, density);
        }
    }

    public boolean shouldInterceptUpdateDisplayModeSpecs(int displayId, DisplayModeDirector.DesiredDisplayModeSpecs modeSpecs) {
        if (this.mSystemReady) {
            return this.mSmartDisplayPolicyManager.shouldInterceptUpdateDisplayModeSpecs(displayId, modeSpecs);
        }
        return false;
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (!DumpUtils.checkDumpPermission(this.mContext, "SmartPower", pw)) {
            return;
        }
        pw.println("smart power (smartpower):");
        try {
            if (0 < args.length) {
                String parm = args[0];
                int opti = 0 + 1;
                if (parm.contains("policy")) {
                    this.mSmartPowerPolicyManager.dump(pw, args, opti);
                } else if (parm.contains("appstate")) {
                    this.mAppStateManager.dump(pw, args, opti);
                } else if (parm.contains("fz")) {
                    PowerFrozenManager.getInstance().dump(pw, args, opti);
                } else if (parm.contains("scene")) {
                    this.mSmartScenarioManager.dump(pw, args, opti);
                } else if (parm.contains("display")) {
                    this.mSmartDisplayPolicyManager.dump(pw, args, opti);
                } else if (parm.equals("set")) {
                    dumpSettings(pw, args, opti);
                    dumpConfig(pw);
                } else if (parm.equals("-a")) {
                    dumpConfig(pw);
                    this.mSmartPowerPolicyManager.dump(pw, args, opti);
                    pw.println("");
                    this.mAppStateManager.dump(pw, args, opti);
                    pw.println("");
                    this.mSmartScenarioManager.dump(pw, args, opti);
                    pw.println("");
                    PowerFrozenManager.getInstance().dump(pw, args, opti);
                    pw.println("");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void dumpSettings(PrintWriter pw, String[] args, int opti) {
        if (opti + 1 < args.length) {
            int opti2 = opti + 1;
            String key = args[opti];
            int i = opti2 + 1;
            String value = args[opti2];
            if (key.contains("fz")) {
                if (value.equals("true") || value.equals("false")) {
                    SystemProperties.set(SmartPowerSettings.FROZEN_PROP, value);
                }
            } else if (key.contains("intercept") && (value.equals("true") || value.equals("false"))) {
                SystemProperties.set(SmartPowerSettings.INTERCEPT_PROP, value);
                SmartPowerSettings.PROP_INTERCEPT_ENABLE = Boolean.parseBoolean(value);
            }
        }
    }

    private void dumpConfig(PrintWriter pw) {
        pw.println("smartpower config:");
        pw.println("  appstate(" + SmartPowerSettings.APP_STATE_ENABLE + ") fz(" + SmartPowerSettings.PROP_FROZEN_ENABLE + ") intercept(" + SmartPowerSettings.PROP_INTERCEPT_ENABLE + ")");
        pw.println("");
    }

    /* JADX WARN: Multi-variable type inference failed */
    public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
        new SmartPowerShellCommand(this).exec(this, in, out, err, args, callback, resultReceiver);
    }

    /* loaded from: classes.dex */
    static class SmartPowerShellCommand extends ShellCommand {
        SmartPowerService mService;

        SmartPowerShellCommand(SmartPowerService service) {
            this.mService = service;
        }

        public int onCommand(String cmd) {
            FileDescriptor fd = getOutFileDescriptor();
            PrintWriter pw = getOutPrintWriter();
            String[] args = getAllArgs();
            if (cmd == null) {
                return handleDefaultCommands(cmd);
            }
            try {
                this.mService.dump(fd, pw, args);
                return -1;
            } catch (Exception e) {
                pw.println(e);
                return -1;
            }
        }

        public void onHelp() {
            PrintWriter pw = getOutPrintWriter();
            pw.println("smart power (smartpower) commands:");
            pw.println();
            pw.println("  policy");
            pw.println("  appstate");
            pw.println("  fz");
            pw.println();
        }
    }
}
