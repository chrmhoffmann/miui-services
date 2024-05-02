package com.android.server.am;

import android.app.ActivityManager;
import android.app.AppGlobals;
import android.app.IApplicationThread;
import android.content.ComponentName;
import android.content.Context;
import android.content.IIntentSender;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.FileUtils;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.app.IPerfShielder;
import com.android.internal.os.ProcessCpuTracker;
import com.android.internal.os.TransferPipe;
import com.android.server.ScoutHelper;
import com.android.server.padkeyboard.usb.UsbKeyboardUtil;
import com.android.server.wm.ActivityTaskManagerDebugConfig;
import com.android.server.wm.ActivityTaskManagerServiceImpl;
import com.android.server.wm.ActivityTaskManagerServiceStub;
import com.android.server.wm.WindowProcessUtils;
import com.miui.base.MiuiStubRegistry;
import com.miui.base.MiuiStubUtil;
import com.miui.server.PerfShielderService;
import com.miui.server.SecurityManagerService;
import com.miui.server.greeze.FreezeUtils;
import com.miui.server.greeze.GreezeManagerService;
import com.miui.server.sptm.SpeedTestModeServiceImpl;
import com.miui.server.xspace.XSpaceManagerServiceStub;
import com.miui.whetstone.PowerKeeperPolicy;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import miui.app.backup.BackupManager;
import miui.drm.DrmBroadcast;
import miui.mqsas.scout.ScoutUtils;
import miui.mqsas.sdk.BootEventManager;
import miui.os.Build;
import miui.os.DeviceFeature;
import miui.process.ProcessConfig;
import miui.process.ProcessManager;
import miui.security.CallerInfo;
import miui.security.WakePathChecker;
import miui.util.font.SymlinkUtils;
import vendor.xiaomi.hardware.misys.V1_0.IResultValue;
/* loaded from: classes.dex */
public class ActivityManagerServiceImpl extends ActivityManagerServiceStub {
    public static final long BOOST_DURATION = 3000;
    private static final String BOOST_TAG = "Boost";
    private static final String CARLINK = "com.miui.carlink";
    public static final long KEEP_FOREGROUND_DURATION = 20000;
    public static final String MIUI_APP_TAG = "MIUIScout App";
    private static final String MIUI_NOTIFICATION = "com.miui.notification";
    private static final String MIUI_VOICE = "com.miui.voiceassist";
    private static final String MI_PUSH = "com.xiaomi.mipush.sdk.PushMessageHandler";
    private static final String MI_VOICE = "com.miui.voiceassist/com.xiaomi.voiceassistant.VoiceService";
    private static final String PROP_DISABLE_AUTORESTART_APP_PREFIX = "sys.rescuepartyplus.disable_autorestart.";
    static final int PUSH_SERVICE_WHITELIST_TIMEOUT = 60000;
    public static final int SIGNAL_QUIT = 3;
    private static final String TAG = "ActivityManagerServiceImpl";
    private static final String WEIXIN = "com.tencent.mm";
    public static final List<String> WIDGET_PROVIDER_WHITE_LIST;
    private static final String XIAOMI_BLUETOOTH = "com.xiaomi.bluetooth";
    private static final String XMSF = "com.xiaomi.xmsf";
    private static final HashSet<String> mIgnoreAuthorityList;
    private static volatile ProcessManagerService sPms;
    ActivityManagerService mAmService;
    Context mContext;
    private Intent mLastSplitIntent;
    private IPerfShielder mPerfService;
    private Map<Integer, Stack<IBinder>> mSplitActivityEntryStack;
    private Bundle mSplitExtras;
    boolean mSystemReady;
    private static ArrayList<String> dumpTraceRequestList = new ArrayList<>();
    private static AtomicInteger requestDumpTraceCount = new AtomicInteger(0);
    private static AtomicBoolean dumpFlag = new AtomicBoolean(false);
    private Map<Integer, Intent> mCurrentSplitIntent = new HashMap();
    private GreezeManagerService greezer = null;
    private ArrayList<Integer> mBackupingList = new ArrayList<>();
    private int mInstrUid = -1;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<ActivityManagerServiceImpl> {

        /* compiled from: ActivityManagerServiceImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final ActivityManagerServiceImpl INSTANCE = new ActivityManagerServiceImpl();
        }

        public ActivityManagerServiceImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public ActivityManagerServiceImpl provideNewInstance() {
            return new ActivityManagerServiceImpl();
        }
    }

    static {
        ArrayList arrayList = new ArrayList();
        WIDGET_PROVIDER_WHITE_LIST = arrayList;
        arrayList.add("com.android.calendar");
        HashSet<String> hashSet = new HashSet<>();
        mIgnoreAuthorityList = hashSet;
        hashSet.add("com.miui.securitycenter.zman.fileProvider");
        hashSet.add("com.xiaomi.misettings.FileProvider");
        hashSet.add("com.xiaomi.mirror.remoteprovider");
        hashSet.add("com.xiaomi.aiasst.service.fileProvider");
        hashSet.add("com.miui.bugreport.fileprovider");
        hashSet.add("com.miui.cleanmaster.fileProvider");
    }

    public static ActivityManagerServiceImpl getInstance() {
        return (ActivityManagerServiceImpl) MiuiStubUtil.getImpl(ActivityManagerServiceStub.class);
    }

    private static ProcessManagerService getProcessManagerService() {
        if (sPms == null) {
            sPms = (ProcessManagerService) ServiceManager.getService("ProcessManager");
        }
        return sPms;
    }

    void init(ActivityManagerService ams, Context context) {
        this.mAmService = ams;
        this.mContext = context;
        MiuiWarnings.getInstance().init(context);
        BroadcastQueueImpl.getInstance().init(this.mAmService, this.mContext);
        DumpScoutTraceThread dumpScoutTraceThread = new DumpScoutTraceThread(DumpScoutTraceThread.TAG, this);
        dumpScoutTraceThread.start();
        Slog.i(TAG, "DumpScoutTraceThread begin running.");
        if (Build.IS_DEBUGGABLE && SystemProperties.getBoolean("debug.block_system", false)) {
            Slog.w(TAG, "boot monitor system_watchdog...");
            SystemClock.sleep(Long.MAX_VALUE);
        }
    }

    void onSystemReady() {
        this.mSystemReady = true;
        PerfShielderService asInterface = IPerfShielder.Stub.asInterface(ServiceManager.getService(PerfShielderService.SERVICE_NAME));
        this.mPerfService = asInterface;
        if (asInterface != null) {
            asInterface.systemReady();
        }
        try {
            ensureDeviceProvisioned(this.mContext);
        } catch (Exception e) {
            Log.e(TAG, "ensureDeviceProvisioned occurs Exception.", e);
        }
    }

    private static boolean isDeviceProvisioned(Context context) {
        return Settings.Global.getInt(context.getContentResolver(), "device_provisioned", 0) != 0;
    }

    private static void ensureDeviceProvisioned(Context context) {
        ComponentName checkEnableName;
        if (!isDeviceProvisioned(context)) {
            PackageManager pm = context.getPackageManager();
            if (!Build.IS_INTERNATIONAL_BUILD) {
                checkEnableName = new ComponentName("com.android.provision", "com.android.provision.activities.DefaultActivity");
            } else {
                checkEnableName = new ComponentName("com.google.android.setupwizard", "com.google.android.setupwizard.SetupWizardActivity");
            }
            if (pm != null && pm.getComponentEnabledSetting(checkEnableName) == 2) {
                Log.e(TAG, "The device provisioned state is inconsistent,try to restore.");
                Settings.Secure.putInt(context.getContentResolver(), "device_provisioned", 1);
                if (!Build.IS_INTERNATIONAL_BUILD) {
                    ComponentName name = new ComponentName("com.android.provision", "com.android.provision.activities.DefaultActivity");
                    pm.setComponentEnabledSetting(name, 1, 1);
                    Intent intent = new Intent("android.intent.action.MAIN");
                    intent.setComponent(name);
                    intent.addFlags(268435456);
                    intent.addCategory("android.intent.category.HOME");
                    context.startActivity(intent);
                    return;
                }
                Settings.Secure.putInt(context.getContentResolver(), "user_setup_complete", 1);
            }
        }
    }

    public boolean isRestrictBackgroundAction(String localhost, int callerUid, String callerPkgName, int calleeUid, String calleePkgName) {
        if (getGreezeService() != null) {
            return getGreezeService().isRestrictBackgroundAction(localhost, callerUid, callerPkgName, calleeUid, calleePkgName);
        }
        return true;
    }

    void finishBooting() {
        XSpaceManagerServiceStub.getInstance().init(this.mContext);
        DrmBroadcast.getInstance(this.mContext).broadcast();
        Intent intent = new Intent("miui.intent.action.FINISH_BOOTING");
        intent.setFlags(268435456);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.SYSTEM);
        ActivityTaskManagerServiceImpl.getInstance().updateResizeBlackList(this.mContext);
        if (!ScoutUtils.REBOOT_COREDUMP) {
            ScoutUtils.isLibraryTest();
        }
    }

    void markAmsReady() {
        BootEventManager.getInstance().setAmsReady(SystemClock.uptimeMillis());
    }

    void markUIReady() {
        long bootCompleteTime = SystemClock.uptimeMillis();
        BootEventManager.getInstance().setUIReady(bootCompleteTime);
        BootEventManager.getInstance().setBootComplete(bootCompleteTime);
    }

    void reportBootEvent() {
        BootEventManager.getInstance();
        BootEventManager.reportBootEvent();
    }

    public int getAppStartMode(int uid, int defMode, int callingPid, String callingPackage) {
        ProcessRecord proc;
        if (XMSF.equalsIgnoreCase(callingPackage) || MIUI_VOICE.equalsIgnoreCase(callingPackage) || CARLINK.equalsIgnoreCase(callingPackage) || MIUI_NOTIFICATION.equalsIgnoreCase(callingPackage) || XIAOMI_BLUETOOTH.equalsIgnoreCase(callingPackage)) {
            UidRecord record = this.mAmService.mProcessList.getUidRecordLOSP(uid);
            if (record != null && record.isIdle() && !record.isCurAllowListed()) {
                synchronized (this.mAmService.mPidsSelfLocked) {
                    proc = this.mAmService.mPidsSelfLocked.get(callingPid);
                }
                int callingUid = proc != null ? proc.uid : Binder.getCallingUid();
                this.mAmService.tempAllowlistUidLocked(record.getUid(), (long) SecurityManagerService.LOCK_TIME_OUT, 101, "push-service-launch", 0, callingUid);
                return 0;
            }
            return 0;
        }
        return defMode;
    }

    public void startProcessLocked(ProcessRecord app, String hostingType, String hostingNameStr) {
        UidRecord record;
        String callerPackage = app.callerPackage;
        if ((XMSF.equalsIgnoreCase(callerPackage) || MIUI_NOTIFICATION.equalsIgnoreCase(callerPackage)) && (record = this.mAmService.mProcessList.getUidRecordLOSP(app.uid)) != null && record.isIdle() && !record.isCurAllowListed()) {
            int callingUid = Binder.getCallingUid();
            this.mAmService.tempAllowlistUidLocked(record.getUid(), (long) SecurityManagerService.LOCK_TIME_OUT, 101, "push-service-launch", 0, callingUid);
        }
    }

    public String getProcessNameByPid(int pid) {
        return ProcessUtils.getProcessNameByPid(pid);
    }

    public String getPackageNameByPid(int pid) {
        return ProcessUtils.getPackageNameByPid(pid);
    }

    public boolean checkRunningCompatibility(IApplicationThread caller, Intent service, String resolvedType, int userId) {
        if (!this.mSystemReady) {
            return true;
        }
        CallerInfo callerInfo = WindowProcessUtils.getCallerInfo(this.mAmService.mActivityTaskManager, caller);
        return checkServiceWakePath(service, resolvedType, callerInfo, userId);
    }

    public boolean checkRunningCompatibility(Intent service, String resolvedType, int callingUid, int callingPid, int userId) {
        ProcessRecord record;
        if (!this.mSystemReady) {
            return true;
        }
        CallerInfo callerInfo = WindowProcessUtils.getCallerInfo(this.mAmService.mActivityTaskManager, callingPid, callingUid);
        if (callerInfo == null && (record = ProcessUtils.getProcessRecordByPid(callingPid)) != null) {
            callerInfo = new CallerInfo();
            callerInfo.callerUid = callingUid;
            callerInfo.callerPkg = record.info.packageName;
            callerInfo.callerPid = callingPid;
            callerInfo.callerProcessName = record.processName;
        }
        return checkServiceWakePath(service, resolvedType, callerInfo, userId);
    }

    private boolean checkServiceWakePath(Intent service, String resolvedType, CallerInfo callerInfo, int userId) {
        try {
            ResolveInfo rInfo = AppGlobals.getPackageManager().resolveService(service, resolvedType, 1024L, userId);
            ServiceInfo sInfo = rInfo != null ? rInfo.serviceInfo : null;
            if (!SmartPowerService.getInstance().shouldInterceptService(service, callerInfo, sInfo)) {
                if (!checkWakePath(this.mAmService, callerInfo, null, service, sInfo, 8, userId)) {
                    return false;
                }
                return true;
            }
            return false;
        } catch (RemoteException e) {
            return true;
        }
    }

    public boolean checkRunningCompatibility(IApplicationThread caller, ActivityInfo info, Intent intent, int userId, String callingPackage) {
        if (info == null) {
            return true;
        }
        CallerInfo callerInfo = WindowProcessUtils.getCallerInfo(this.mAmService.mActivityTaskManager, caller);
        return checkWakePath(this.mAmService, callerInfo, callingPackage, intent, info, 1, userId);
    }

    public boolean checkRunningCompatibility(IApplicationThread caller, int callingUid, ContentProviderRecord record, int userId) {
        if (!this.mSystemReady || record == null || record.name == null) {
            return true;
        }
        Intent intent = new Intent();
        intent.setClassName(record.name.getPackageName(), record.name.getClassName());
        intent.putExtra("android.intent.extra.UID", callingUid);
        CallerInfo callerInfo = WindowProcessUtils.getCallerInfo(this.mAmService.mActivityTaskManager, caller);
        return checkWakePath(this.mAmService, callerInfo, null, intent, record.info, 4, userId);
    }

    /* JADX WARN: Code restructure failed: missing block: B:34:0x00c9, code lost:
        if (com.android.server.wm.WindowProcessUtils.isPackageRunning(r15.mAmService.mActivityTaskManager, r2, r3.processName, r3.uid) != false) goto L37;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public boolean checkRunningCompatibility(android.content.ComponentName r16, int r17, int r18, int r19) {
        /*
            Method dump skipped, instructions count: 283
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.am.ActivityManagerServiceImpl.checkRunningCompatibility(android.content.ComponentName, int, int, int):boolean");
    }

    static boolean checkWakePath(ActivityManagerService ams, CallerInfo callerInfo, String callingPackage, Intent intent, ComponentInfo info, int wakeType, int userId) {
        long startTime;
        String callerPkg;
        int calleeUid;
        String calleePkg;
        String className;
        int calleeUid2;
        boolean abort;
        if (ams == null || intent == null || info == null) {
            return true;
        }
        WakePathChecker checker = WakePathChecker.getInstance();
        checker.updatePath(intent, info, wakeType, userId);
        long startTime2 = SystemClock.elapsedRealtime();
        int callerUid = -1;
        String callerPkg2 = "";
        if (callerInfo != null) {
            String widgetProcessName = callerInfo.callerPkg + ":widgetProvider";
            if (WIDGET_PROVIDER_WHITE_LIST.contains(callerInfo.callerPkg)) {
                startTime = startTime2;
            } else if (widgetProcessName.equals(callerInfo.callerProcessName)) {
                switch (wakeType) {
                    case 1:
                        boolean abort2 = !PendingIntentRecordImpl.containsPendingIntent(callerInfo.callerPkg);
                        abort = abort2;
                        break;
                    default:
                        boolean abort3 = !WindowProcessUtils.isProcessRunning(ams.mActivityTaskManager, info.processName, info.applicationInfo.uid);
                        abort = abort3;
                        break;
                }
                if (abort) {
                    Slog.i(TAG, "MIUILOG- Reject widget call from " + callerInfo.callerPkg);
                    WakePathChecker.getInstance().recordWakePathCall(callerInfo.callerPkg, info.packageName, wakeType, UserHandle.getUserId(callerInfo.callerUid), UserHandle.getUserId(info.applicationInfo.uid), false);
                    return false;
                }
                startTime = startTime2;
            } else {
                startTime = startTime2;
            }
            String callerPkg3 = callerInfo.callerPkg;
            callerUid = callerInfo.callerUid;
            callerPkg = callerPkg3;
        } else {
            startTime = startTime2;
            if (TextUtils.isEmpty(callingPackage) && wakeType == 4) {
                int callerUid2 = intent.getIntExtra("android.intent.extra.UID", -1);
                if (callerUid2 != -1) {
                    try {
                        String[] pkgs = ams.getPackageManager().getPackagesForUid(callerUid2);
                        if (pkgs != null && pkgs.length != 0) {
                            callerPkg2 = pkgs[0];
                        }
                        callerUid = callerUid2;
                        callerPkg = callerPkg2;
                    } catch (Exception e) {
                        Log.e(TAG, "getPackagesFor uid exception!", e);
                        callerPkg = "android";
                        callerUid = callerUid2;
                    }
                } else {
                    callerPkg = "android";
                    callerUid = callerUid2;
                }
            } else {
                callerPkg = callingPackage;
            }
        }
        String calleePkg2 = info.packageName;
        String className2 = info.name;
        String action = intent.getAction();
        if (info.applicationInfo == null) {
            calleeUid = -1;
        } else {
            calleeUid = info.applicationInfo.uid;
        }
        if (TextUtils.isEmpty(calleePkg2) || TextUtils.equals(callerPkg, calleePkg2)) {
            return true;
        }
        if (calleeUid < 0) {
            calleeUid2 = calleeUid;
            className = className2;
            calleePkg = calleePkg2;
        } else if (!WindowProcessUtils.isPackageRunning(ams.mActivityTaskManager, calleePkg2, info.processName, calleeUid)) {
            calleeUid2 = calleeUid;
            className = className2;
            calleePkg = calleePkg2;
        } else {
            boolean isAllow = !checker.calleeAliveMatchBlackRule(action, className2, callerPkg, calleePkg2, userId, wakeType << 12, true);
            return isAllow;
        }
        boolean ret = !checker.matchWakePathRule(action, className, callerPkg, calleePkg, callerUid, calleeUid2, wakeType, userId);
        checkTime(startTime, "checkWakePath");
        return ret;
    }

    private static void checkTime(long startTime, String where) {
        long now = SystemClock.elapsedRealtime();
        if (now - startTime > 1000) {
            Slog.w(TAG, "MIUILOG-checkTime:Slow operation: " + (now - startTime) + "ms so far, now at " + where);
        }
    }

    public boolean ignoreSpecifiedAuthority(String authority) {
        return mIgnoreAuthorityList.contains(authority);
    }

    public boolean shouldCrossXSpace(String packageName, int userId) {
        return false;
    }

    boolean isStartWithBackupRestriction(Context context, String backupPkgName, ProcessRecord app) {
        ApplicationInfo appInfo = app.getActiveInstrumentation() != null ? app.getActiveInstrumentation().mTargetInfo : app.info;
        return backupPkgName.equals(appInfo.packageName) && !BackupManager.isSysAppForBackup(context, appInfo.packageName);
    }

    public void killProcessDueToResolutionChanged() {
        ProcessConfig config = new ProcessConfig(18);
        config.setPriority(6);
        ProcessManager.kill(config);
    }

    public void finishBootingAsUser(int userId) {
        ActivityTaskManagerServiceImpl.getInstance().restartSubScreenUiIfNeeded(userId, "finishBooting");
    }

    public void moveUserToForeground(int oldUserId, int newUserId) {
        ActivityTaskManagerServiceImpl.getInstance().restartSubScreenUiIfNeeded(newUserId, "moveUserToForeground");
    }

    public boolean isAllowedOperatorGetPhoneNumber(ActivityManagerService ams, String permission) {
        String[] content = permission.split(";");
        if (content.length == 4) {
            int pid = Integer.parseInt(content[3]);
            String packageName = ProcessUtils.getPackageNameByPid(pid);
            if (TextUtils.isEmpty(packageName)) {
                return true;
            }
            int op = Integer.parseInt(content[1]);
            int uid = Integer.parseInt(content[2]);
            return ams.mAppOpsService.noteOperation(op, uid, packageName, (String) null, false, "ActivityManagerServiceImpl#isAllowedOperatorGetPhoneNumber", false).getOpMode() == 0;
        }
        return true;
    }

    public boolean onTransact(ActivityManagerService service, int code, Parcel data, Parcel reply, int flags) {
        if (code == 16777214) {
            return setPackageHoldOn(service, data, reply);
        }
        if (code == 16777213) {
            return getPackageHoldOn(data, reply);
        }
        if (code == 16777212) {
            data.enforceInterface("android.app.IActivityManager");
            ActivityInfo info = ActivityTaskManagerServiceImpl.getInstance().getLastResumedActivityInfo();
            reply.writeNoException();
            reply.writeParcelable(info, 0);
            return true;
        } else if (code == 16776609) {
            data.enforceInterface("android.app.IActivityManager");
            boolean isTopSplitActivity = isTopSplitActivity(data.readInt(), data.readStrongBinder());
            reply.writeNoException();
            int isTop = isTopSplitActivity ? 1 : 0;
            reply.writeInt(isTop);
            return true;
        } else if (code == 16776608) {
            data.enforceInterface("android.app.IActivityManager");
            IBinder token = getTopSplitActivity(data.readInt());
            reply.writeNoException();
            reply.writeStrongBinder(token);
            return true;
        } else if (code == 16776610) {
            data.enforceInterface("android.app.IActivityManager");
            removeFromEntryStack(data.readInt(), data.readStrongBinder());
            return true;
        } else if (code == 16776611) {
            data.enforceInterface("android.app.IActivityManager");
            clearEntryStack(data.readInt(), data.readStrongBinder());
            return true;
        } else if (code == 16776612) {
            data.enforceInterface("android.app.IActivityManager");
            addToEntryStack(data.readInt(), data.readStrongBinder(), data.readInt(), (Intent) Intent.CREATOR.createFromParcel(data));
            reply.writeNoException();
            return true;
        } else if (code == 16776613) {
            data.enforceInterface("android.app.IActivityManager");
            int pid = data.readInt();
            boolean isForLast = data.readInt() > 0;
            Parcelable[] parcelables = getIntentInfo(pid, isForLast);
            reply.writeNoException();
            reply.writeParcelableArray(parcelables, 0);
            return true;
        } else if (code == 16776614) {
            boolean isForLast2 = false;
            data.enforceInterface("android.app.IActivityManager");
            Intent intent = (Intent) data.readParcelable(Intent.class.getClassLoader());
            int pid2 = data.readInt();
            Bundle bundle = data.readBundle();
            if (data.readInt() > 0) {
                isForLast2 = true;
            }
            setIntentInfo(intent, pid2, bundle, isForLast2);
            reply.writeNoException();
            return true;
        } else if (code != 16776607) {
            return false;
        } else {
            data.enforceInterface("android.app.IActivityManager");
            IBinder token2 = data.readStrongBinder();
            ActivityManager.RunningTaskInfo taskInfo = ActivityTaskManagerServiceStub.get().getSplitTaskInfo(token2);
            reply.writeNoException();
            reply.writeParcelable(taskInfo, 0);
            return true;
        }
    }

    public boolean getPackageHoldOn(Parcel data, Parcel reply) {
        data.enforceInterface("android.app.IActivityManager");
        int callingUid = Binder.getCallingUid();
        long ident = Binder.clearCallingIdentity();
        reply.writeNoException();
        try {
            if (UserHandle.getAppId(callingUid) != 1000) {
                reply.writeString("");
                Slog.e(TAG, "Permission Denial: getPackageHoldOn() not from system " + callingUid);
            } else {
                reply.writeString(ActivityTaskManagerServiceStub.get().getPackageHoldOn());
            }
            Binder.restoreCallingIdentity(ident);
            return true;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(ident);
            throw th;
        }
    }

    public boolean setPackageHoldOn(ActivityManagerService service, Parcel data, Parcel reply) {
        data.enforceInterface("android.app.IActivityManager");
        int callingUid = Binder.getCallingUid();
        long ident = Binder.clearCallingIdentity();
        try {
            if (UserHandle.getAppId(callingUid) == 1000) {
                ActivityTaskManagerServiceStub.get().setPackageHoldOn(service.mActivityTaskManager, data.readString());
            } else {
                Slog.e(TAG, "Permission Denial: setPackageHoldOn() not from system uid " + callingUid);
            }
            Binder.restoreCallingIdentity(ident);
            reply.writeNoException();
            return true;
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(ident);
            throw th;
        }
    }

    public static boolean isSystemPackage(String packageName, int userId) {
        try {
            ApplicationInfo applicationInfo = AppGlobals.getPackageManager().getApplicationInfo(packageName, 0L, userId);
            if (applicationInfo == null) {
                return true;
            }
            int flags = applicationInfo.flags;
            return ((flags & 1) == 0 && (flags & 128) == 0) ? false : true;
        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
    }

    public boolean isBoostNeeded(ProcessRecord app, String hostingType, String hostingName) {
        String callerPackage = app.callerPackage;
        boolean isNeeded = false;
        boolean isSystem = isSystemPackage(callerPackage, 0);
        boolean isNeeded2 = "service".equals(hostingType) && hostingName.endsWith(MI_PUSH) && XMSF.equals(callerPackage) && isSystem;
        boolean isNeeded3 = MI_VOICE.equals(hostingName) || isNeeded2;
        if (MIUI_NOTIFICATION.equals(callerPackage) || isNeeded3) {
            isNeeded = true;
        }
        if (WEIXIN.equals(app.processName)) {
            isNeeded = true;
        }
        Slog.d(BOOST_TAG, "hostingType=" + hostingType + ", hostingName=" + hostingName + ", callerPackage=" + callerPackage + ", isSystem=" + isSystem + ", isBoostNeeded=" + isNeeded + ".");
        return isNeeded;
    }

    public boolean doBoostEx(ProcessRecord app, long beginTime) {
        boolean boostNeededNext = false | doTopAppBoost(app, beginTime);
        if (WEIXIN.equals(app.processName)) {
            return boostNeededNext | doForegroundBoost(app, beginTime);
        }
        return boostNeededNext;
    }

    public String dumpMiuiStackTraces(int[] pids) {
        int callingUid = Binder.getCallingUid();
        if (UserHandle.getAppId(callingUid) != 1000) {
            throw new SecurityException("Only the system process can call dumpMiuiStackTraces, received request from uid: " + callingUid);
        }
        if (pids.length < 1) {
            Slog.w(TAG, "dumpMiuiStackTraces: pids is null");
            return null;
        }
        ArrayList<Integer> javapids = new ArrayList<>(3);
        ArrayList<Integer> nativePids = new ArrayList<>(3);
        for (int i = 0; i < pids.length; i++) {
            int adj = ScoutHelper.getOomAdjOfPid(TAG, pids[i]);
            int isJavaOrNativeProcess = ScoutHelper.checkIsJavaOrNativeProcess(adj);
            if (isJavaOrNativeProcess != 0) {
                if (isJavaOrNativeProcess == 1) {
                    javapids.add(Integer.valueOf(pids[i]));
                } else if (isJavaOrNativeProcess == 2) {
                    nativePids.add(Integer.valueOf(pids[i]));
                }
            }
        }
        File mTraceFile = ActivityManagerService.dumpStackTraces(javapids, (ProcessCpuTracker) null, (SparseArray) null, nativePids, (StringWriter) null, "App Scout Exception", (String) null);
        if (mTraceFile == null) {
            return null;
        }
        return mTraceFile.getAbsolutePath();
    }

    public File dumpAppStackTraces(ArrayList<Integer> firstPids, SparseArray<Boolean> lastPids, ArrayList<Integer> nativePids, String subject, String path) {
        Slog.i(MIUI_APP_TAG, "dumpStackTraces pids=" + lastPids + " nativepids=" + nativePids);
        File tracesFile = new File(path);
        try {
            if (tracesFile.createNewFile()) {
                FileUtils.setPermissions(tracesFile.getAbsolutePath(), 384, -1, -1);
            }
            if (subject != null) {
                try {
                    FileOutputStream fos = new FileOutputStream(tracesFile, true);
                    String header = "Subject: " + subject + "\n";
                    fos.write(header.getBytes(StandardCharsets.UTF_8));
                    fos.close();
                } catch (IOException e) {
                    Slog.w(MIUI_APP_TAG, "Exception writing subject to scout dump file:", e);
                }
            }
            ActivityManagerService.dumpStackTraces(tracesFile.getAbsolutePath(), firstPids, nativePids, (ArrayList) null);
            return tracesFile;
        } catch (IOException e2) {
            Slog.w(MIUI_APP_TAG, "Exception creating scout dump file:", e2);
            return null;
        }
    }

    public File dumpOneProcessTraces(int pid, String path, String subject) {
        ArrayList<Integer> firstPids = new ArrayList<>();
        ArrayList<Integer> nativePids = new ArrayList<>();
        int adj = ScoutHelper.getOomAdjOfPid(TAG, pid);
        int isJavaOrNativeProcess = ScoutHelper.checkIsJavaOrNativeProcess(adj);
        if (isJavaOrNativeProcess == 1) {
            firstPids.add(Integer.valueOf(pid));
        } else if (isJavaOrNativeProcess == 2) {
            nativePids.add(Integer.valueOf(pid));
        } else {
            Slog.w(MIUI_APP_TAG, "can not distinguish for this process's adj" + adj);
            return null;
        }
        File traceFile = new File(path);
        try {
            if (traceFile.createNewFile()) {
                FileUtils.setPermissions(traceFile.getAbsolutePath(), 384, -1, -1);
            }
            if (subject != null) {
                try {
                    FileOutputStream fos = new FileOutputStream(traceFile, true);
                    String header = "Subject: " + subject + "\n";
                    fos.write(header.getBytes(StandardCharsets.UTF_8));
                    fos.close();
                } catch (IOException e) {
                    Slog.w(MIUI_APP_TAG, "Exception writing subject to scout dump file:", e);
                }
            }
            ActivityManagerService.dumpStackTraces(traceFile.getAbsolutePath(), firstPids, nativePids, (ArrayList) null);
            return traceFile;
        } catch (IOException e2) {
            Slog.w(MIUI_APP_TAG, "Exception creating scout dump file:", e2);
            return null;
        }
    }

    public void dumpSystemTraces(final String path) {
        requestDumpTraceCount.getAndIncrement();
        if (requestDumpTraceCount.get() > 0 && !dumpFlag.get()) {
            requestDumpTraceCount.getAndDecrement();
            dumpFlag.set(true);
            ExecutorService executor = Executors.newSingleThreadExecutor();
            try {
                try {
                    executor.execute(new Runnable() { // from class: com.android.server.am.ActivityManagerServiceImpl$$ExternalSyntheticLambda0
                        @Override // java.lang.Runnable
                        public final void run() {
                            ActivityManagerServiceImpl.this.m207x3cc32ce2(path);
                        }
                    });
                    Slog.w(MIUI_APP_TAG, "dumpSystemTraces finally shutdown.");
                    if (executor == null) {
                        return;
                    }
                } catch (Exception e) {
                    Slog.w(MIUI_APP_TAG, "Exception occurs while dumping system scout trace file:", e);
                    Slog.w(MIUI_APP_TAG, "dumpSystemTraces finally shutdown.");
                    if (executor == null) {
                        return;
                    }
                }
                executor.shutdown();
                return;
            } catch (Throwable th) {
                Slog.w(MIUI_APP_TAG, "dumpSystemTraces finally shutdown.");
                if (executor != null) {
                    executor.shutdown();
                }
                throw th;
            }
        }
        synchronized (dumpTraceRequestList) {
            dumpTraceRequestList.add(path);
        }
    }

    /* JADX WARN: Type inference failed for: r1v9, types: [com.android.server.am.ActivityManagerServiceImpl$1] */
    /* renamed from: lambda$dumpSystemTraces$0$com-android-server-am-ActivityManagerServiceImpl */
    public /* synthetic */ void m207x3cc32ce2(String path) {
        ScoutHelper.CheckDState(MIUI_APP_TAG, ActivityManagerService.MY_PID);
        Slog.i(MIUI_APP_TAG, "Start dumping system_server trace ...");
        final File systemTraceFile = dumpOneProcessTraces(ActivityManagerService.MY_PID, path, "App Scout Exception");
        if (systemTraceFile != null) {
            Slog.d(MIUI_APP_TAG, "Dump scout system trace file successfully!");
            final ArrayList<String> tempArray = new ArrayList<>();
            synchronized (dumpTraceRequestList) {
                Iterator<String> it = dumpTraceRequestList.iterator();
                while (it.hasNext()) {
                    String filePath = it.next();
                    tempArray.add(filePath);
                }
                dumpTraceRequestList.clear();
            }
            dumpFlag.set(false);
            Slog.d(MIUI_APP_TAG, "starting copying file");
            if (requestDumpTraceCount.get() > 0 && tempArray.size() > 0) {
                new Thread() { // from class: com.android.server.am.ActivityManagerServiceImpl.1
                    @Override // java.lang.Thread, java.lang.Runnable
                    public void run() {
                        Iterator it2 = tempArray.iterator();
                        while (it2.hasNext()) {
                            String dumpPath = (String) it2.next();
                            File dumpFile = new File(dumpPath);
                            ActivityManagerServiceImpl.requestDumpTraceCount.getAndDecrement();
                            Slog.d(ActivityManagerServiceImpl.MIUI_APP_TAG, "requestDumpTraceCount delete one, now is " + ActivityManagerServiceImpl.requestDumpTraceCount.toString());
                            try {
                                if (dumpFile.createNewFile()) {
                                    FileUtils.setPermissions(dumpFile.getAbsolutePath(), 384, -1, -1);
                                    if (FileUtils.copyFile(systemTraceFile, dumpFile)) {
                                        Slog.i(ActivityManagerServiceImpl.MIUI_APP_TAG, "Success copying system_server trace to path" + dumpPath);
                                    } else {
                                        Slog.w(ActivityManagerServiceImpl.MIUI_APP_TAG, "Fail to copy system_server trace to path" + dumpPath);
                                    }
                                }
                            } catch (IOException e) {
                                Slog.w(ActivityManagerServiceImpl.MIUI_APP_TAG, "Exception occurs while copying system scout trace file:", e);
                            }
                        }
                    }
                }.start();
                return;
            }
            return;
        }
        Slog.w(MIUI_APP_TAG, "Dump scout system trace file fail!");
        dumpFlag.set(false);
    }

    public void dumpMiuiJavaTrace(int pid) {
        if (Process.getThreadGroupLeader(pid) == pid && ScoutHelper.getOomAdjOfPid("MIUI ANR", pid) > -1000) {
            Process.sendSignal(pid, 3);
            Slog.w("MIUI ANR", "[Scout] Send SIGNAL_QUIT to generate java stack dump. Pid:" + pid);
        }
    }

    public int getOomAdjOfPid(int pid) {
        int callingUid = Binder.getCallingUid();
        if (UserHandle.getAppId(callingUid) != 1000) {
            throw new SecurityException("Only the system process can call getOomAdjOfPid, received request from uid: " + callingUid);
        }
        return ScoutHelper.getOomAdjOfPid(TAG, pid);
    }

    private static boolean doTopAppBoost(ProcessRecord app, long beginTime) {
        if (SystemClock.uptimeMillis() - beginTime > BOOST_DURATION || app.mState.getCurrentSchedulingGroup() == 3) {
            return false;
        }
        if (app.mState.getCurrentSchedulingGroup() < 3) {
            app.mState.setCurrentSchedulingGroup(3);
            Slog.d(BOOST_TAG, "Process is boosted to top app, processName=" + app.processName + ".");
            return true;
        }
        return true;
    }

    private static boolean doForegroundBoost(ProcessRecord app, long beginTime) {
        if (SystemClock.uptimeMillis() - beginTime > KEEP_FOREGROUND_DURATION) {
            return false;
        }
        if (app.mState.getCurrentSchedulingGroup() < 2) {
            app.mState.setCurrentSchedulingGroup(2);
            return true;
        }
        return true;
    }

    private static boolean checkThawTime(int uid, String report, GreezeManagerService greezer) {
        int timeout;
        Slog.d(TAG, "checkThawTime uid=" + uid + " report=" + report);
        if (TextUtils.isEmpty(report)) {
            return false;
        }
        if (report.startsWith("Broadcast of") || report.startsWith("executing service") || report.startsWith("ContentProvider not")) {
            timeout = 20000;
        } else if (!report.startsWith("Input dispatching")) {
            return false;
        } else {
            timeout = SpeedTestModeServiceImpl.ENABLE_SPTM_MIN_MEMORY;
        }
        Slog.d(TAG, "checkThawTime thawTime=" + greezer.getLastThawedTime(uid, 1) + " now=" + SystemClock.uptimeMillis());
        long thawTime = greezer.getLastThawedTime(uid, 1);
        if (uid < 10000 || uid > 19999 || thawTime <= 0 || SystemClock.uptimeMillis() - thawTime >= timeout) {
            return false;
        }
        Slog.d(TAG, "matched " + report + " app time uid=" + uid);
        return true;
    }

    private GreezeManagerService getGreezeService() {
        if (this.greezer == null) {
            this.greezer = GreezeManagerService.getService();
        }
        return this.greezer;
    }

    public boolean skipFrozenAppAnr(ApplicationInfo info, int uid, String report) {
        GreezeManagerService greezer = getGreezeService();
        if (greezer == null) {
            return false;
        }
        int appid = UserHandle.getAppId(info.uid);
        if (uid != info.uid) {
            appid = UserHandle.getAppId(uid);
        }
        if (appid <= 1000) {
            return false;
        }
        if (SmartPowerService.getInstance().skipFrozenAppAnr(info, uid, report)) {
            return true;
        }
        int[] frozenUids = greezer.getFrozenUids(1);
        if (frozenUids.length > 0) {
            for (int i = 0; i < frozenUids.length; i++) {
                if (frozenUids[i] == appid) {
                    Slog.d(TAG, " matched app is " + frozenUids[i] + " appid is " + appid);
                    return true;
                }
            }
        }
        if (checkThawTime(uid, report, greezer)) {
            return true;
        }
        if (frozenUids.length > 0) {
            Slog.d(TAG, "procs: " + FreezeUtils.getFrozenPids().toString());
        }
        return false;
    }

    public void cleanUpApplicationRecordLocked(ProcessRecord app) {
        super.cleanUpApplicationRecordLocked(app);
        if (DeviceFeature.SUPPORT_SPLIT_ACTIVITY) {
            Map<Integer, Stack<IBinder>> map = this.mSplitActivityEntryStack;
            if (map != null && map.containsKey(Integer.valueOf(app.mPid))) {
                Slog.w(TAG, "Split main entrance killed, clear sub activities for " + app.info.packageName + ", mPid " + app.mPid);
                clearEntryStack(app.mPid, null);
                this.mSplitActivityEntryStack.remove(Integer.valueOf(app.mPid));
            }
            Map<Integer, Intent> map2 = this.mCurrentSplitIntent;
            if (map2 != null) {
                map2.remove(Integer.valueOf(app.mPid));
            }
            Slog.d(TAG, "Cleaning tablet split stack.");
        }
    }

    public boolean isTopSplitActivity(int pid, IBinder token) {
        Stack<IBinder> stack;
        Map<Integer, Stack<IBinder>> map = this.mSplitActivityEntryStack;
        return map != null && !map.isEmpty() && token != null && (stack = this.mSplitActivityEntryStack.get(Integer.valueOf(pid))) != null && !stack.empty() && token.equals(stack.peek());
    }

    public IBinder getTopSplitActivity(int pid) {
        Stack<IBinder> stack;
        Map<Integer, Stack<IBinder>> map = this.mSplitActivityEntryStack;
        if (map == null || map.isEmpty() || (stack = this.mSplitActivityEntryStack.get(Integer.valueOf(pid))) == null || stack.empty()) {
            return null;
        }
        return stack.peek();
    }

    public void removeFromEntryStack(int pid, IBinder token) {
        Map<Integer, Stack<IBinder>> map;
        Stack<IBinder> stack;
        if (token != null && (map = this.mSplitActivityEntryStack) != null && (stack = map.get(Integer.valueOf(pid))) != null && stack.empty()) {
            stack.remove(token);
        }
    }

    public void clearEntryStack(int pid, IBinder selfToken) {
        Stack<IBinder> stack;
        Map<Integer, Stack<IBinder>> map = this.mSplitActivityEntryStack;
        if (map == null || map.isEmpty() || (stack = this.mSplitActivityEntryStack.get(Integer.valueOf(pid))) == null || stack.empty()) {
            return;
        }
        if (selfToken != null && !selfToken.equals(stack.peek())) {
            return;
        }
        long ident = Binder.clearCallingIdentity();
        while (!stack.empty()) {
            IBinder token = stack.pop();
            if (token != null && !token.equals(selfToken)) {
                this.mAmService.finishActivity(token, 0, (Intent) null, 0);
            }
        }
        Binder.restoreCallingIdentity(ident);
        if (selfToken != null) {
            stack.push(selfToken);
        }
    }

    public void addToEntryStack(int pid, IBinder token, int resultCode, Intent resultData) {
        if (this.mSplitActivityEntryStack == null) {
            this.mSplitActivityEntryStack = new HashMap();
        }
        Stack<IBinder> pkgStack = this.mSplitActivityEntryStack.get(Integer.valueOf(pid));
        if (pkgStack == null) {
            pkgStack = new Stack<>();
        }
        pkgStack.push(token);
        this.mSplitActivityEntryStack.put(Integer.valueOf(pid), pkgStack);
    }

    private Parcelable[] getIntentInfo(int pid, boolean isForLast) {
        return isForLast ? new Parcelable[]{this.mLastSplitIntent, this.mSplitExtras} : new Parcelable[]{this.mCurrentSplitIntent.get(Integer.valueOf(pid)), null};
    }

    private void setIntentInfo(Intent intent, int pid, Bundle bundle, boolean isForLast) {
        if (isForLast) {
            this.mLastSplitIntent = intent;
            this.mSplitExtras = bundle;
            return;
        }
        if (!this.mCurrentSplitIntent.containsKey(Integer.valueOf(pid))) {
            Log.e(TAG, "CRITICAL_LOG add intent info.");
        }
        this.mCurrentSplitIntent.put(Integer.valueOf(pid), intent);
    }

    public boolean interceptAppRestartIfNeeded(String processName, String type) {
        ProcessManagerService pmi = getProcessManagerService();
        if (pmi != null) {
            return pmi.interceptAppRestartIfNeeded(processName, type);
        }
        return false;
    }

    public void boostCameraByThreshold(Intent in) {
        ProcessManagerService pmi = getProcessManagerService();
        if (pmi != null) {
            long startTime = SystemClock.elapsedRealtime();
            if (isNeedBoostCamera(in)) {
                pmi.boostCameraByThreshold(0L);
            }
            long diff = SystemClock.elapsedRealtime() - startTime;
            if (diff > 100) {
                Slog.w(TAG, "boostCameraByThreshold: Slow operation: " + diff + "ms so far");
            }
        }
    }

    private boolean isNeedBoostCamera(Intent in) {
        if (in == null) {
            return false;
        }
        ComponentName comp = in.getComponent();
        String action = in.getAction();
        if (comp == null || action == null) {
            return false;
        }
        if (!TextUtils.equals(comp.flattenToShortString(), "com.android.camera/.Camera") && !TextUtils.equals(comp.flattenToShortString(), "com.android.camera/.VoiceCamera")) {
            return false;
        }
        return true;
    }

    public boolean isKillProvider(ContentProviderRecord cpr, ProcessRecord proc, ProcessRecord capp) {
        if (capp.mState.getCurAdj() > 200 && !ProcessUtils.isHomeProcess(capp)) {
            return true;
        }
        Object obj = "??";
        StringBuilder append = new StringBuilder().append("visible app ").append(capp.processName).append(" depends on provider ").append(cpr.name.flattenToShortString()).append(" in dying proc ").append(proc != null ? proc.processName : obj).append(" (adj ");
        if (proc != null) {
            obj = Integer.valueOf(proc.mState.getSetAdj());
        }
        Slog.w(TAG, append.append(obj).append(")").toString());
        return false;
    }

    public boolean killPackageProcesses(String packageName, int appId, int userId, String reason) {
        boolean result = false;
        synchronized (this.mAmService) {
            ActivityManagerService.boostPriorityForLockedSection();
            try {
                result = this.mAmService.mProcessList.killPackageProcessesLSP(packageName, appId, userId, 0, false, true, true, false, true, false, 13, 0, reason);
            } catch (Exception e) {
                Slog.e(TAG, "invoke killPackageProcessesLocked error:", e);
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
        }
        return result;
    }

    public void reportActivityLaunchTime(String packageName, String launchedFromPackage, long totalTime, int launchState) {
        IPerfShielder iPerfShielder = this.mPerfService;
        if (iPerfShielder != null && totalTime > 0 && launchState == 1 && launchedFromPackage != packageName) {
            try {
                iPerfShielder.reportActivityLaunchTime(packageName, totalTime);
            } catch (RemoteException e) {
            }
        }
    }

    public void syncFontForWebView() {
        if (Build.IS_INTERNATIONAL_BUILD) {
            return;
        }
        SymlinkUtils.onAttachApplication();
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public void enableAmsDebugConfig(String config, boolean enable) {
        char c;
        Slog.d(TAG, "enableAMSDebugConfig, config=:" + config + ", enable=:" + enable);
        switch (config.hashCode()) {
            case -2091566946:
                if (config.equals("DEBUG_VISIBILITY")) {
                    c = ' ';
                    break;
                }
                c = 65535;
                break;
            case -1705346631:
                if (config.equals("DEBUG_ANR")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case -1705346571:
                if (config.equals("DEBUG_APP")) {
                    c = '!';
                    break;
                }
                c = 65535;
                break;
            case -1705335933:
                if (config.equals("DEBUG_LRU")) {
                    c = '\t';
                    break;
                }
                c = 65535;
                break;
            case -1705332060:
                if (config.equals("DEBUG_PSS")) {
                    c = 19;
                    break;
                }
                c = 65535;
                break;
            case -1621031281:
                if (config.equals("DEBUG_FREEZER")) {
                    c = '\b';
                    break;
                }
                c = 65535;
                break;
            case -1462091039:
                if (config.equals("DEBUG_TRANSITION")) {
                    c = 31;
                    break;
                }
                c = 65535;
                break;
            case -1325909408:
                if (config.equals("DEBUG_IDLE")) {
                    c = '\"';
                    break;
                }
                c = 65535;
                break;
            case -1067959432:
                if (config.equals("DEBUG_ALLOWLISTS")) {
                    c = 26;
                    break;
                }
                c = 65535;
                break;
            case -866453022:
                if (config.equals("DEBUG_UID_OBSERVERS")) {
                    c = 23;
                    break;
                }
                c = 65535;
                break;
            case -735231590:
                if (config.equals("DEBUG_OOM_ADJ_REASON")) {
                    c = '\r';
                    break;
                }
                c = 65535;
                break;
            case -595903599:
                if (config.equals("DEBUG_COMPACTION")) {
                    c = 7;
                    break;
                }
                c = 65535;
                break;
            case -322538852:
                if (config.equals("DEBUG_SERVICE_EXECUTING")) {
                    c = 22;
                    break;
                }
                c = 65535;
                break;
            case -274692833:
                if (config.equals("DEBUG_PERMISSIONS_REVIEW")) {
                    c = 25;
                    break;
                }
                c = 65535;
                break;
            case -160480136:
                if (config.equals("DEBUG_CLEANUP")) {
                    c = '(';
                    break;
                }
                c = 65535;
                break;
            case -61428073:
                if (config.equals("DEBUG_METRICS")) {
                    c = ')';
                    break;
                }
                c = 65535;
                break;
            case -61191196:
                if (config.equals("DEBUG_RECENTS_TRIM_TASKS")) {
                    c = 28;
                    break;
                }
                c = 65535;
                break;
            case -44209236:
                if (config.equals("DEBUG_USER_LEAVING")) {
                    c = '$';
                    break;
                }
                c = 65535;
                break;
            case 52145557:
                if (config.equals("DEBUG_BROADCAST")) {
                    c = 3;
                    break;
                }
                c = 65535;
                break;
            case 53108793:
                if (config.equals("DEBUG_PERMISSIONS_REVIEW_ATMS")) {
                    c = '%';
                    break;
                }
                c = 65535;
                break;
            case 65041228:
                if (config.equals("DEBUG_RECENTS")) {
                    c = 27;
                    break;
                }
                c = 65535;
                break;
            case 73340379:
                if (config.equals("DEBUG_RELEASE")) {
                    c = '#';
                    break;
                }
                c = 65535;
                break;
            case 80292298:
                if (config.equals("DEBUG_RESULTS")) {
                    c = '&';
                    break;
                }
                c = 65535;
                break;
            case 156502476:
                if (config.equals("DEBUG_BROADCAST_LIGHT")) {
                    c = 5;
                    break;
                }
                c = 65535;
                break;
            case 576262193:
                if (config.equals("DEBUG_PROCESSES")) {
                    c = 17;
                    break;
                }
                c = 65535;
                break;
            case 596380184:
                if (config.equals("DEBUG_BROADCAST_BACKGROUND")) {
                    c = 4;
                    break;
                }
                c = 65535;
                break;
            case 764822901:
                if (config.equals("DEBUG_ACTIVITY_STARTS")) {
                    c = '\'';
                    break;
                }
                c = 65535;
                break;
            case 826230786:
                if (config.equals("DEBUG_NETWORK")) {
                    c = 11;
                    break;
                }
                c = 65535;
                break;
            case 833576886:
                if (config.equals("DEBUG_ROOT_TASK")) {
                    c = 29;
                    break;
                }
                c = 65535;
                break;
            case 966898825:
                if (config.equals("DEBUG_SERVICE")) {
                    c = 20;
                    break;
                }
                c = 65535;
                break;
            case 1202911566:
                if (config.equals("DEBUG_BACKUP")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case 1222829889:
                if (config.equals("DEBUG_PROCESS_OBSERVERS")) {
                    c = 16;
                    break;
                }
                c = 65535;
                break;
            case 1232431591:
                if (config.equals("DEBUG_POWER_QUICK")) {
                    c = 15;
                    break;
                }
                c = 65535;
                break;
            case 1330462516:
                if (config.equals("DEBUG_MU")) {
                    c = '\n';
                    break;
                }
                c = 65535;
                break;
            case 1388181797:
                if (config.equals("DEBUG_FOREGROUND_SERVICE")) {
                    c = 21;
                    break;
                }
                c = 65535;
                break;
            case 1441072931:
                if (config.equals("DEBUG_BACKGROUND_CHECK")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case 1700665397:
                if (config.equals("DEBUG_USAGE_STATS")) {
                    c = 24;
                    break;
                }
                c = 65535;
                break;
            case 1710111424:
                if (config.equals("DEBUG_SWITCH")) {
                    c = 30;
                    break;
                }
                c = 65535;
                break;
            case 1837355645:
                if (config.equals("DEBUG_PROVIDER")) {
                    c = 18;
                    break;
                }
                c = 65535;
                break;
            case 1853284313:
                if (config.equals("DEBUG_POWER")) {
                    c = 14;
                    break;
                }
                c = 65535;
                break;
            case 1981739733:
                if (config.equals("DEBUG_BROADCAST_DEFERRAL")) {
                    c = 6;
                    break;
                }
                c = 65535;
                break;
            case 1993785769:
                if (config.equals("DEBUG_OOM_ADJ")) {
                    c = '\f';
                    break;
                }
                c = 65535;
                break;
            default:
                c = 65535;
                break;
        }
        switch (c) {
            case 0:
                ActivityManagerDebugConfig.DEBUG_ANR = enable;
                return;
            case 1:
                ActivityManagerDebugConfig.DEBUG_BACKGROUND_CHECK = enable;
                return;
            case 2:
                ActivityManagerDebugConfig.DEBUG_BACKUP = enable;
                return;
            case 3:
                ActivityManagerDebugConfig.DEBUG_BROADCAST = enable;
                return;
            case 4:
                ActivityManagerDebugConfig.DEBUG_BROADCAST_BACKGROUND = enable;
                return;
            case 5:
                ActivityManagerDebugConfig.DEBUG_BROADCAST_LIGHT = enable;
                return;
            case 6:
                ActivityManagerDebugConfig.DEBUG_BROADCAST_DEFERRAL = enable;
                return;
            case 7:
                ActivityManagerDebugConfig.DEBUG_COMPACTION = enable;
                return;
            case '\b':
                ActivityManagerDebugConfig.DEBUG_FREEZER = enable;
                return;
            case '\t':
                ActivityManagerDebugConfig.DEBUG_LRU = enable;
                return;
            case '\n':
                ActivityManagerDebugConfig.DEBUG_MU = enable;
                return;
            case 11:
                ActivityManagerDebugConfig.DEBUG_NETWORK = enable;
                return;
            case '\f':
                ActivityManagerDebugConfig.DEBUG_OOM_ADJ = enable;
                return;
            case '\r':
                ActivityManagerDebugConfig.DEBUG_OOM_ADJ_REASON = enable;
                return;
            case 14:
                ActivityManagerDebugConfig.DEBUG_POWER = enable;
                return;
            case 15:
                ActivityManagerDebugConfig.DEBUG_POWER_QUICK = enable;
                return;
            case 16:
                ActivityManagerDebugConfig.DEBUG_PROCESS_OBSERVERS = enable;
                return;
            case 17:
                ActivityManagerDebugConfig.DEBUG_PROCESSES = enable;
                return;
            case 18:
                ActivityManagerDebugConfig.DEBUG_PROVIDER = enable;
                return;
            case 19:
                ActivityManagerDebugConfig.DEBUG_PSS = enable;
                return;
            case 20:
                ActivityManagerDebugConfig.DEBUG_SERVICE = enable;
                return;
            case 21:
                ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE = enable;
                return;
            case 22:
                ActivityManagerDebugConfig.DEBUG_SERVICE_EXECUTING = enable;
                return;
            case 23:
                ActivityManagerDebugConfig.DEBUG_UID_OBSERVERS = enable;
                return;
            case 24:
                ActivityManagerDebugConfig.DEBUG_USAGE_STATS = enable;
                return;
            case 25:
                ActivityManagerDebugConfig.DEBUG_PERMISSIONS_REVIEW = enable;
                return;
            case 26:
                ActivityManagerDebugConfig.DEBUG_ALLOWLISTS = enable;
                return;
            case 27:
                ActivityTaskManagerDebugConfig.DEBUG_RECENTS = enable;
                return;
            case 28:
                ActivityTaskManagerDebugConfig.DEBUG_RECENTS_TRIM_TASKS = enable;
                return;
            case 29:
                ActivityTaskManagerDebugConfig.DEBUG_ROOT_TASK = enable;
                return;
            case 30:
                ActivityTaskManagerDebugConfig.DEBUG_SWITCH = enable;
                return;
            case IResultValue.MISYS_EMLINK /* 31 */:
                ActivityTaskManagerDebugConfig.DEBUG_TRANSITION = enable;
                return;
            case ' ':
                ActivityTaskManagerDebugConfig.DEBUG_VISIBILITY = enable;
                return;
            case '!':
                ActivityTaskManagerDebugConfig.DEBUG_APP = enable;
                return;
            case '\"':
                ActivityTaskManagerDebugConfig.DEBUG_IDLE = enable;
                return;
            case UsbKeyboardUtil.COMMAND_BACK_LIGHT_ENABLE /* 35 */:
                ActivityTaskManagerDebugConfig.DEBUG_RELEASE = enable;
                return;
            case UsbKeyboardUtil.COMMAND_TOUCH_PAD_SENSITIVITY /* 36 */:
                ActivityTaskManagerDebugConfig.DEBUG_USER_LEAVING = enable;
                return;
            case UsbKeyboardUtil.COMMAND_POWER_STATE /* 37 */:
                ActivityTaskManagerDebugConfig.DEBUG_PERMISSIONS_REVIEW = enable;
                return;
            case '&':
                ActivityTaskManagerDebugConfig.DEBUG_RESULTS = enable;
                return;
            case '\'':
                ActivityTaskManagerDebugConfig.DEBUG_ACTIVITY_STARTS = enable;
                return;
            case '(':
                ActivityTaskManagerDebugConfig.DEBUG_CLEANUP = enable;
                return;
            case ')':
                ActivityTaskManagerDebugConfig.DEBUG_METRICS = enable;
                return;
            default:
                return;
        }
    }

    public boolean skipPruneOldTraces() {
        if (ScoutUtils.isLibraryTest()) {
            return true;
        }
        return false;
    }

    boolean isHomeOrRecentsToKeepAlive(String packageName) {
        return PeriodicCleanerService.isHomeOrRecentsToKeepAlive(packageName);
    }

    public boolean checkStartInputMethodSettingsActivity(IIntentSender target) {
        if (target instanceof PendingIntentRecord) {
            PendingIntentRecord pendingIntentRecord = (PendingIntentRecord) target;
            if (pendingIntentRecord.key != null && pendingIntentRecord.key.requestIntent != null && "android.settings.INPUT_METHOD_SETTINGS".equals(pendingIntentRecord.key.requestIntent.getAction())) {
                return true;
            }
            return false;
        }
        return false;
    }

    public boolean dump(String cmd, FileDescriptor fd, PrintWriter pw, String[] args, int opti, boolean dumpAll, boolean dumpClient, String dumpPackage) {
        if ("logging".equals(cmd)) {
            dumpLogText(pw);
            return true;
        } else if ("app-logging".equals(cmd)) {
            return dumpAppLogText(fd, pw, args, opti);
        } else {
            return false;
        }
    }

    private boolean dumpAppLogText(FileDescriptor fd, PrintWriter pw, String[] args, int opti) {
        if (opti < args.length) {
            String dumpPackage = args[opti];
            int opti2 = opti + 1;
            if (opti2 < args.length) {
                try {
                    int uid = Integer.parseInt(args[opti2]);
                    synchronized (this.mAmService) {
                        ProcessRecord app = this.mAmService.getProcessRecordLocked(dumpPackage, uid);
                        if (app != null) {
                            pw.println("\n** APP LOGGIN in pid " + app.mPid + "[" + dumpPackage + "] **");
                            IApplicationThread thread = app.getThread();
                            if (thread != null) {
                                try {
                                    try {
                                        TransferPipe tp = new TransferPipe();
                                        try {
                                            thread.dumpLogText(tp.getWriteFd());
                                            tp.go(fd);
                                            return true;
                                        } finally {
                                            tp.kill();
                                        }
                                    } catch (IOException e) {
                                        pw.println("Got IoException! " + e);
                                        pw.flush();
                                    }
                                } catch (RemoteException e2) {
                                    pw.println("Got RemoteException! " + e2);
                                    pw.flush();
                                }
                            }
                        } else {
                            pw.println("app-logging: " + dumpPackage + "(" + uid + ") not running.");
                            return false;
                        }
                    }
                } catch (NumberFormatException e3) {
                    pw.println("app-logging: uid format is error, please input integer.");
                    return false;
                }
            } else {
                pw.println("app-logging: no uid specified.");
            }
            return false;
        }
        pw.println("app-logging: no process name specified");
        return false;
    }

    public void dumpLogText(PrintWriter pw) {
        pw.println("ACTIVITY MANAGER LOGGING (dumpsys activity logging)");
        Pair<String, String> pair = new Pair<>("", "");
        Pair<String, String> pair2 = generateGroups((String) pair.first, (String) pair.second, ActivityManagerDebugConfig.DEBUG_BACKGROUND_CHECK, "DEBUG_BACKGROUND_CHECK");
        Pair<String, String> pair3 = generateGroups((String) pair2.first, (String) pair2.second, ActivityManagerDebugConfig.DEBUG_BROADCAST, "DEBUG_BROADCAST");
        Pair<String, String> pair4 = generateGroups((String) pair3.first, (String) pair3.second, ActivityManagerDebugConfig.DEBUG_BROADCAST_BACKGROUND, "DEBUG_BROADCAST_BACKGROUND");
        Pair<String, String> pair5 = generateGroups((String) pair4.first, (String) pair4.second, ActivityManagerDebugConfig.DEBUG_BROADCAST_LIGHT, "DEBUG_BROADCAST_LIGHT");
        Pair<String, String> pair6 = generateGroups((String) pair5.first, (String) pair5.second, ActivityManagerDebugConfig.DEBUG_BROADCAST_DEFERRAL, "DEBUG_BROADCAST_DEFERRAL");
        Pair<String, String> pair7 = generateGroups((String) pair6.first, (String) pair6.second, ActivityManagerDebugConfig.DEBUG_PROVIDER, "DEBUG_PROVIDER");
        Pair<String, String> pair8 = generateGroups((String) pair7.first, (String) pair7.second, ActivityManagerDebugConfig.DEBUG_SERVICE, "DEBUG_SERVICE");
        Pair<String, String> pair9 = generateGroups((String) pair8.first, (String) pair8.second, ActivityManagerDebugConfig.DEBUG_FOREGROUND_SERVICE, "DEBUG_FOREGROUND_SERVICE");
        Pair<String, String> pair10 = generateGroups((String) pair9.first, (String) pair9.second, ActivityManagerDebugConfig.DEBUG_SERVICE_EXECUTING, "DEBUG_SERVICE_EXECUTING");
        Pair<String, String> pair11 = generateGroups((String) pair10.first, (String) pair10.second, ActivityManagerDebugConfig.DEBUG_ALLOWLISTS, "DEBUG_ALLOWLISTS");
        Pair<String, String> pair12 = generateGroups((String) pair11.first, (String) pair11.second, ActivityTaskManagerDebugConfig.DEBUG_RECENTS, "DEBUG_RECENTS");
        Pair<String, String> pair13 = generateGroups((String) pair12.first, (String) pair12.second, ActivityTaskManagerDebugConfig.DEBUG_RECENTS_TRIM_TASKS, "DEBUG_RECENTS_TRIM_TASKS");
        Pair<String, String> pair14 = generateGroups((String) pair13.first, (String) pair13.second, ActivityTaskManagerDebugConfig.DEBUG_ROOT_TASK, "DEBUG_ROOT_TASK");
        Pair<String, String> pair15 = generateGroups((String) pair14.first, (String) pair14.second, ActivityTaskManagerDebugConfig.DEBUG_SWITCH, "DEBUG_SWITCH");
        Pair<String, String> pair16 = generateGroups((String) pair15.first, (String) pair15.second, ActivityTaskManagerDebugConfig.DEBUG_TRANSITION, "DEBUG_TRANSITION");
        Pair<String, String> pair17 = generateGroups((String) pair16.first, (String) pair16.second, ActivityTaskManagerDebugConfig.DEBUG_VISIBILITY, "DEBUG_VISIBILITY");
        Pair<String, String> pair18 = generateGroups((String) pair17.first, (String) pair17.second, ActivityTaskManagerDebugConfig.DEBUG_APP, "DEBUG_APP");
        Pair<String, String> pair19 = generateGroups((String) pair18.first, (String) pair18.second, ActivityTaskManagerDebugConfig.DEBUG_IDLE, "DEBUG_IDLE");
        Pair<String, String> pair20 = generateGroups((String) pair19.first, (String) pair19.second, ActivityTaskManagerDebugConfig.DEBUG_RELEASE, "DEBUG_RELEASE");
        Pair<String, String> pair21 = generateGroups((String) pair20.first, (String) pair20.second, ActivityTaskManagerDebugConfig.DEBUG_USER_LEAVING, "DEBUG_USER_LEAVING");
        Pair<String, String> pair22 = generateGroups((String) pair21.first, (String) pair21.second, ActivityManagerDebugConfig.DEBUG_PERMISSIONS_REVIEW, "DEBUG_PERMISSIONS_REVIEW");
        Pair<String, String> pair23 = generateGroups((String) pair22.first, (String) pair22.second, ActivityTaskManagerDebugConfig.DEBUG_RESULTS, "DEBUG_RESULTS");
        Pair<String, String> pair24 = generateGroups((String) pair23.first, (String) pair23.second, ActivityTaskManagerDebugConfig.DEBUG_ACTIVITY_STARTS, "DEBUG_ACTIVITY_STARTS");
        Pair<String, String> pair25 = generateGroups((String) pair24.first, (String) pair24.second, ActivityTaskManagerDebugConfig.DEBUG_CLEANUP, "DEBUG_CLEANUP");
        Pair<String, String> pair26 = generateGroups((String) pair25.first, (String) pair25.second, ActivityTaskManagerDebugConfig.DEBUG_METRICS, "DEBUG_METRICS");
        pw.println("Enabled log groups:");
        pw.println((String) pair26.first);
        pw.println();
        pw.println("Disabled log groups:");
        pw.println((String) pair26.second);
    }

    private Pair<String, String> generateGroups(String enabled, String disabled, boolean config, String group) {
        if (config) {
            enabled = enabled + (TextUtils.isEmpty(enabled) ? group : " " + group);
        } else {
            disabled = disabled + (TextUtils.isEmpty(disabled) ? group : " " + group);
        }
        return new Pair<>(enabled, disabled);
    }

    public boolean checkAppDisableStatus(String packageName) {
        if (SystemProperties.getBoolean(PROP_DISABLE_AUTORESTART_APP_PREFIX + packageName, false)) {
            Slog.w(TAG, "Disable App [" + packageName + "] auto start!");
            return true;
        }
        return false;
    }

    public boolean isSystemApp(int pid) {
        boolean z;
        synchronized (this.mAmService.mPidsSelfLocked) {
            ProcessRecord app = this.mAmService.mPidsSelfLocked.get(pid);
            z = (app == null || app.info == null || !app.info.isSystemApp()) ? false : true;
        }
        return z;
    }

    public String getPackageNameForPid(int pid) {
        String str;
        synchronized (this.mAmService.mPidsSelfLocked) {
            ProcessRecord app = this.mAmService.mPidsSelfLocked.get(pid);
            str = (app == null || app.info == null) ? null : app.info.packageName;
        }
        return str;
    }

    public void notifyExcuteServices(ProcessRecord app) {
        GreezeManagerService greezeManagerService = this.greezer;
        if (greezeManagerService != null) {
            greezeManagerService.notifyExcuteServices(app.uid);
        }
    }

    public void backupBind(int uid, boolean start) {
        if (this.greezer == null || uid < 10000 || uid > 19999) {
            return;
        }
        if (!this.mBackupingList.contains(Integer.valueOf(uid)) && start) {
            this.mBackupingList.add(Integer.valueOf(uid));
            this.greezer.notifyBackup(uid, true);
        } else if (this.mBackupingList.contains(Integer.valueOf(uid)) && !start) {
            this.mBackupingList.remove(Integer.valueOf(uid));
            Bundle b = new Bundle();
            b.putInt("uid", uid);
            b.putBoolean("start", false);
            PowerKeeperPolicy.getInstance().notifyEvent(18, b);
        }
    }

    public boolean isBackuping(int uid) {
        return this.mBackupingList.contains(Integer.valueOf(uid));
    }

    public void setActiveInstrumentation(ComponentName instr) {
        int i = -1;
        if (instr != null) {
            String mInstrPkg = instr.getPackageName();
            if (mInstrPkg == null) {
                return;
            }
            try {
                PackageManager mPm = this.mContext.getPackageManager();
                ApplicationInfo info = mPm.getApplicationInfo(mInstrPkg, 0);
                if (info != null) {
                    i = info.uid;
                }
                this.mInstrUid = i;
                return;
            } catch (Exception e) {
                return;
            }
        }
        this.mInstrUid = -1;
    }

    public boolean isActiveInstruUid(int uid) {
        return uid == this.mInstrUid;
    }
}
