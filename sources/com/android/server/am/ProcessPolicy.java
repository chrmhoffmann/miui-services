package com.android.server.am;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.UserHandle;
import android.provider.MiuiSettings;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.view.accessibility.AccessibilityManager;
import com.android.internal.os.BackgroundThread;
import com.android.internal.os.BatteryStatsManagerStub;
import com.android.server.ScoutHelper;
import com.android.server.ServiceThread;
import com.miui.enterprise.settings.EnterpriseSettings;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import miui.os.Build;
import miui.process.ProcessCloudData;
import miui.process.ProcessManager;
import miui.util.DeviceLevel;
import org.json.JSONArray;
import org.json.JSONObject;
/* loaded from: classes.dex */
public class ProcessPolicy implements BatteryStatsManagerStub.ActiveCallback {
    private static final double CAMERA_BOOST_THRESHOLD_PERCENT;
    public static final boolean DEBUG = true;
    public static final boolean DEBUG_ACTIVE = true;
    private static final long DEFAULT_FASTBOOT_THRESHOLDKB = 524288000;
    private static final boolean DYNAMIC_LIST_CHECK_ADJ = true;
    public static final int FLAG_CLOUD_WHITE_LIST = 4;
    public static final int FLAG_DISABLE_FORCE_STOP = 32;
    public static final int FLAG_DISABLE_TRIM_MEMORY = 16;
    public static final int FLAG_DYNAMIC_WHITE_LIST = 2;
    public static final int FLAG_ENABLE_CALL_PROTECT = 64;
    public static final int FLAG_ENTERPRISE_APP_LIST = 4096;
    public static final int FLAG_FAST_BOOT_APP_LIST = 2048;
    public static final int FLAG_NEED_TRACE_LIST = 128;
    public static final int FLAG_SECRETLY_PROTECT_APP_LIST = 1024;
    public static final int FLAG_STATIC_WHILTE_LIST = 1;
    public static final int FLAG_USER_DEFINED_LIST = 8;
    private static final String JSON_KEY_PACKAGE_NAMES = "pkgs";
    private static final String JSON_KEY_USER_ID = "u";
    private static final int MSG_UPDATE_AUDIO_OFF = 1;
    private static final int MSG_UPDATE_STOP_GPS = 2;
    private static final int PERCEPTIBLE_APP_ADJ;
    private static final String PREFS_LOCKED_APPS = "Locked_apps";
    private static final int PRIORITY_LEVEL_HEAVY_WEIGHT = 3;
    private static final int PRIORITY_LEVEL_PERCEPTIBLE = 2;
    private static final int PRIORITY_LEVEL_UNKNOWN = -1;
    private static final int PRIORITY_LEVEL_VISIBLE = 1;
    private static final long RAM_SIZE_1GB = 1073741824;
    public static final String REASON_ANR = "anr";
    public static final String REASON_AUTO_IDLE_KILL = "AutoIdleKill";
    public static final String REASON_AUTO_LOCK_OFF_CLEAN = "AutoLockOffClean";
    public static final String REASON_AUTO_LOCK_OFF_CLEAN_BY_PRIORITY = "AutoLockOffCleanByPriority";
    public static final String REASON_AUTO_POWER_KILL = "AutoPowerKill";
    public static final String REASON_AUTO_SLEEP_CLEAN = "AutoSleepClean";
    public static final String REASON_AUTO_SYSTEM_ABNORMAL_CLEAN = "AutoSystemAbnormalClean";
    public static final String REASON_AUTO_THERMAL_KILL = "AutoThermalKill";
    public static final String REASON_AUTO_THERMAL_KILL_ALL_LEVEL_1 = "AutoThermalKillAll1";
    public static final String REASON_AUTO_THERMAL_KILL_ALL_LEVEL_2 = "AutoThermalKillAll2";
    public static final String REASON_CRASH = "crash";
    public static final String REASON_DISPLAY_SIZE_CHANGED = "DisplaySizeChanged";
    public static final String REASON_FORCE_CLEAN = "ForceClean";
    public static final String REASON_GAME_CLEAN = "GameClean";
    public static final String REASON_GARBAGE_CLEAN = "GarbageClean";
    public static final String REASON_LOCK_SCREEN_CLEAN = "LockScreenClean";
    public static final String REASON_LOW_MEMO = "lowMemory";
    public static final String REASON_MIUI_MEMO_SERVICE = "MiuiMemoryService";
    public static final String REASON_ONE_KEY_CLEAN = "OneKeyClean";
    public static final String REASON_OPTIMIZATION_CLEAN = "OptimizationClean";
    public static final String REASON_SCREEN_OFF_CPU_CHECK_KILL = "ScreenOffCPUCheckKill";
    public static final String REASON_SWIPE_UP_CLEAN = "SwipeUpClean";
    public static final String REASON_UNKNOWN = "Unknown";
    public static final String REASON_USER_DEFINED = "UserDefined";
    public static final String TAG = "ProcessManager";
    public static final String TAG_PM = "ProcessManager";
    public static final boolean TAG_WITH_CLASS_NAME = false;
    private static final long UPDATE_AUDIO_OFF_DELAY = 600;
    private static final long UPDATE_STOP_GPS_DELAY = 1000;
    public static final int USER_ALL = -100;
    public static final SparseArray<Pair<Integer, Integer>> sProcessPriorityMap;
    private AccessibilityManager mAccessibilityManager;
    private ActiveUpdateHandler mActiveUpdateHandler;
    private ActivityManagerService mActivityManagerService;
    private ProcessManagerService mProcessManagerService;
    private static List<String> sStaticWhiteList = new ArrayList();
    private static List<String> sProcessStaticWhiteList = new ArrayList();
    private static HashMap<String, Boolean> sDynamicWhiteList = new HashMap<>();
    private static List<String> sCloudWhiteList = new ArrayList();
    private static List<String> sUserDefinedWhiteList = new ArrayList();
    private static HashMap<Integer, Set<String>> sLockedApplicationList = new HashMap<>();
    private static List<String> sDisableTrimList = new ArrayList();
    private static List<String> sDisableForceStopList = new ArrayList();
    private static List<String> sEnableCallProtectList = new ArrayList();
    private static List<String> sNeedTraceList = new ArrayList();
    private static List<String> sSecretlyProtectAppList = new ArrayList();
    static List<String> sUserKillProcReasons = new ArrayList();
    static List<String> sLowMemKillProcReasons = new ArrayList();
    private static List<String> sEnterpriseAppList = new ArrayList();
    private static List<String> sFgServiceCheckList = new ArrayList();
    private static Map<String, String> sBoundFgServiceProtectMap = new HashMap();
    private static List<String> sDisplaySizeProtectList = new ArrayList();
    private static List<String> sDisplaySizeBlackList = new ArrayList();
    private static SparseArray<ActiveUidRecord> sActiveUidList = new SparseArray<>();
    private static SparseArray<ActiveUidRecord> sTempInactiveAudioList = new SparseArray<>();
    private static SparseArray<ActiveUidRecord> sTempInactiveGPSList = new SparseArray<>();
    private static Map<String, Long> sFastBootAppMap = new HashMap();
    private static Map<String, Integer> sAppProtectMap = new HashMap();
    private static Map<String, Integer> sFgServiceProtectMap = new HashMap();
    private static Map<String, Integer> sCameraMemThresholdMap = new HashMap();
    private static final Object sLock = new Object();
    private static final String DEVICE = Build.DEVICE.toLowerCase();

    static {
        SparseArray<Pair<Integer, Integer>> sparseArray = new SparseArray<>();
        sProcessPriorityMap = sparseArray;
        PERCEPTIBLE_APP_ADJ = Build.VERSION.SDK_INT > 23 ? ScoutHelper.BINDER_FULL_KILL_SCORE_ADJ_MIN : 2;
        CAMERA_BOOST_THRESHOLD_PERCENT = Process.getTotalMemory() / RAM_SIZE_1GB < 3 ? 0.375d : 0.25d;
        sUserKillProcReasons.add(REASON_ONE_KEY_CLEAN);
        sUserKillProcReasons.add(REASON_FORCE_CLEAN);
        sUserKillProcReasons.add(REASON_GARBAGE_CLEAN);
        sUserKillProcReasons.add(REASON_GAME_CLEAN);
        sUserKillProcReasons.add(REASON_OPTIMIZATION_CLEAN);
        sUserKillProcReasons.add(REASON_SWIPE_UP_CLEAN);
        sUserKillProcReasons.add(REASON_USER_DEFINED);
        sLowMemKillProcReasons.add(REASON_ANR);
        sLowMemKillProcReasons.add(REASON_CRASH);
        sLowMemKillProcReasons.add(REASON_MIUI_MEMO_SERVICE);
        sLowMemKillProcReasons.add(REASON_LOW_MEMO);
        long memoryThreshold = getMemoryThresholdForFastBooApp().longValue();
        sFastBootAppMap.put("com.tencent.mm", Long.valueOf(memoryThreshold));
        sFastBootAppMap.put("com.tencent.mobileqq", Long.valueOf(memoryThreshold));
        sparseArray.put(-1, ProcessUtils.PRIORITY_UNKNOW);
        sparseArray.put(1, ProcessUtils.PRIORITY_VISIBLE);
        sparseArray.put(2, ProcessUtils.PRIORITY_PERCEPTIBLE);
        sparseArray.put(3, ProcessUtils.PRIORITY_HEAVY);
        sAppProtectMap.put("com.miui.bugreport", 3);
        sAppProtectMap.put("com.miui.virtualsim", 3);
        sAppProtectMap.put("com.miui.touchassistant", 3);
        sAppProtectMap.put("com.xiaomi.joyose", 3);
        sAppProtectMap.put("com.miui.tsmclient", 3);
        sAppProtectMap.put("com.miui.powerkeeper", 3);
        sBoundFgServiceProtectMap.put("com.milink.service", "com.xiaomi.miplay_client");
        sCameraMemThresholdMap.put("polaris", 1572864);
        sCameraMemThresholdMap.put("sirius", 1048576);
        sCameraMemThresholdMap.put("dipper", 1572864);
        sCameraMemThresholdMap.put("ursa", 1572864);
        sCameraMemThresholdMap.put("perseus", 1887232);
        sCameraMemThresholdMap.put("equuleus", 1572864);
        sCameraMemThresholdMap.put("cactus", 768000);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class ActiveUpdateHandler extends Handler {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public ActiveUpdateHandler(Looper looper) {
            super(looper, null, true);
            ProcessPolicy.this = this$0;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            ActiveUidRecord uidRecord = (ActiveUidRecord) msg.obj;
            switch (msg.what) {
                case 1:
                    checkRemoveActiveUid(uidRecord, 1);
                    return;
                case 2:
                    checkRemoveActiveUid(uidRecord, 2);
                    return;
                default:
                    return;
            }
        }

        private void checkRemoveActiveUid(ActiveUidRecord uidRecord, int flag) {
            if (uidRecord != null) {
                synchronized (ProcessPolicy.sLock) {
                    try {
                        switch (flag) {
                            case 1:
                                ProcessPolicy.sTempInactiveAudioList.remove(uidRecord.uid);
                                break;
                            case 2:
                                ProcessPolicy.sTempInactiveGPSList.remove(uidRecord.uid);
                                break;
                            default:
                                return;
                        }
                        uidRecord.flag &= ~flag;
                        if (uidRecord.flag == 0) {
                            ProcessPolicy.sActiveUidList.remove(uidRecord.uid);
                            Slog.d("ProcessManager", "real remove inactive uid : " + uidRecord.uid + " flag : " + flag);
                        }
                    } finally {
                    }
                }
            }
        }
    }

    /* loaded from: classes.dex */
    public static final class ActiveUidRecord {
        static final int ACTIVE_AUDIO = 1;
        static final int ACTIVE_GPS = 2;
        static final int NO_ACTIVE = 0;
        public int flag;
        public int uid;

        public ActiveUidRecord(int _uid) {
            this.uid = _uid;
        }

        private void makeActiveString(StringBuilder sb) {
            sb.append("flag :");
            sb.append(this.flag);
            sb.append(' ');
            boolean printed = false;
            int i = this.flag;
            if (i == 0) {
                sb.append("NONE");
                return;
            }
            if ((i & 1) != 0) {
                printed = true;
                sb.append("A");
            }
            if ((this.flag & 2) != 0) {
                if (printed) {
                    sb.append("|");
                }
                sb.append("G");
            }
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(128);
            sb.append("ActiveUidRecord{");
            sb.append(Integer.toHexString(System.identityHashCode(this)));
            sb.append(' ');
            UserHandle.formatUid(sb, this.uid);
            sb.append(' ');
            makeActiveString(sb);
            sb.append("}");
            return sb.toString();
        }
    }

    public ProcessPolicy(ProcessManagerService processManagerService, ActivityManagerService ams, AccessibilityManager accessibilityManager, ServiceThread thread) {
        this.mProcessManagerService = processManagerService;
        this.mActivityManagerService = ams;
        this.mAccessibilityManager = accessibilityManager;
        this.mActiveUpdateHandler = new ActiveUpdateHandler(thread.getLooper());
    }

    public void systemReady(Context context) {
        String[] prcWhiteList;
        String[] pckWhiteList;
        synchronized (sLock) {
            sStaticWhiteList = new ArrayList(Arrays.asList(context.getResources().getStringArray(285409383)));
            sProcessStaticWhiteList = new ArrayList(Arrays.asList(context.getResources().getStringArray(285409392)));
            if (DeviceLevel.IS_MIUI_LITE_VERSION) {
                pckWhiteList = context.getResources().getStringArray(285409384);
                prcWhiteList = context.getResources().getStringArray(285409393);
            } else {
                pckWhiteList = context.getResources().getStringArray(285409382);
                prcWhiteList = context.getResources().getStringArray(285409391);
            }
            if (pckWhiteList != null) {
                sStaticWhiteList.addAll(new ArrayList(Arrays.asList(pckWhiteList)));
            }
            if (prcWhiteList != null) {
                sProcessStaticWhiteList.addAll(new ArrayList(Arrays.asList(prcWhiteList)));
            }
            sDisableTrimList = Arrays.asList(context.getResources().getStringArray(285409386));
            sDisableForceStopList = Arrays.asList(context.getResources().getStringArray(285409385));
            sNeedTraceList = Arrays.asList(context.getResources().getStringArray(285409378));
            sSecretlyProtectAppList = new ArrayList(Arrays.asList(context.getResources().getStringArray(285409390)));
            sFgServiceCheckList = Arrays.asList(context.getResources().getStringArray(285409389));
            sDisplaySizeProtectList = Arrays.asList(context.getResources().getStringArray(285409388));
            sDisplaySizeBlackList = Arrays.asList(context.getResources().getStringArray(285409387));
        }
        loadLockedAppFromSettings(context);
        updateApplicationLockedState("com.jeejen.family.miui", -100, true);
    }

    public List<String> getWhiteList(int flags) {
        List<String> whiteList = new ArrayList<>();
        synchronized (sLock) {
            if ((flags & 1) != 0) {
                try {
                    whiteList.addAll(sStaticWhiteList);
                } catch (Throwable th) {
                    throw th;
                }
            }
            if ((flags & 2) != 0) {
                whiteList.addAll(sDynamicWhiteList.keySet());
            }
            if ((flags & 4) != 0) {
                whiteList.addAll(sCloudWhiteList);
            }
            if ((flags & 8) != 0) {
                whiteList.addAll(sUserDefinedWhiteList);
            }
            if ((flags & 16) != 0) {
                whiteList.addAll(sDisableTrimList);
            }
            if ((flags & 32) != 0) {
                whiteList.addAll(sDisableForceStopList);
            }
            if ((flags & 64) != 0) {
                whiteList.addAll(sEnableCallProtectList);
            }
            if ((flags & 128) != 0) {
                whiteList.addAll(sNeedTraceList);
            }
            if ((flags & 1024) != 0) {
                whiteList.addAll(sSecretlyProtectAppList);
            }
            if ((flags & 2048) != 0) {
                whiteList.addAll(sFastBootAppMap.keySet());
            }
            if (EnterpriseSettings.ENTERPRISE_ACTIVATED && (flags & 4096) != 0) {
                whiteList.addAll(sEnterpriseAppList);
            }
        }
        return whiteList;
    }

    public void addWhiteList(int flag, List<String> whiteList, boolean append) {
        List<String> targetWhiteList;
        synchronized (sLock) {
            try {
                if ((flag & 1) != 0) {
                    targetWhiteList = sStaticWhiteList;
                } else if ((flag & 4) != 0) {
                    targetWhiteList = sCloudWhiteList;
                } else if ((flag & 8) != 0) {
                    targetWhiteList = sUserDefinedWhiteList;
                } else if ((flag & 16) != 0) {
                    targetWhiteList = sDisableTrimList;
                } else if ((flag & 32) != 0) {
                    targetWhiteList = sDisableForceStopList;
                } else if (EnterpriseSettings.ENTERPRISE_ACTIVATED && (flag & 4096) != 0) {
                    targetWhiteList = sEnterpriseAppList;
                } else {
                    targetWhiteList = new ArrayList<>();
                    Slog.e("ProcessManager", "addWhiteList with unknown flag=" + flag);
                }
                if (!append) {
                    targetWhiteList.clear();
                }
                targetWhiteList.addAll(whiteList);
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    public HashMap<String, Boolean> updateDynamicWhiteList(Context context, int userId) {
        Throwable th;
        HashMap<String, Boolean> activeWhiteList = new HashMap<>();
        String wallpaperPkg = ProcessUtils.getActiveWallpaperPackage(context);
        boolean z = true;
        if (wallpaperPkg != null) {
            activeWhiteList.put(wallpaperPkg, true);
        }
        String inputMethodPkg = ProcessUtils.getDefaultInputMethod(context);
        if (inputMethodPkg != null) {
            activeWhiteList.put(inputMethodPkg, true);
        }
        String ttsEngine = ProcessUtils.getActiveTtsEngine(context);
        if (ttsEngine != null) {
            activeWhiteList.put(ttsEngine, true);
        }
        if (ProcessUtils.isPhoneWorking()) {
            activeWhiteList.put("com.android.incallui", true);
        }
        activeWhiteList.put("com.miui.voip", true);
        synchronized (this.mActivityManagerService) {
            try {
                for (String packageName : sFgServiceCheckList) {
                    try {
                        List<ProcessRecord> appList = this.mProcessManagerService.getProcessRecordList(packageName, userId);
                        Iterator<ProcessRecord> it = appList.iterator();
                        while (true) {
                            if (!it.hasNext()) {
                                break;
                            }
                            ProcessRecord app = it.next();
                            if (app != null && app.mServices.hasForegroundServices()) {
                                activeWhiteList.put(packageName, true);
                                if (sBoundFgServiceProtectMap.containsKey(packageName)) {
                                    activeWhiteList.put(sBoundFgServiceProtectMap.get(packageName), false);
                                }
                            }
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        throw th;
                    }
                }
                List<AccessibilityServiceInfo> accessibilityList = this.mAccessibilityManager.getEnabledAccessibilityServiceList(-1);
                if (accessibilityList != null && !accessibilityList.isEmpty()) {
                    for (AccessibilityServiceInfo info : accessibilityList) {
                        if (info != null && !TextUtils.isEmpty(info.getId())) {
                            ComponentName componentName = ComponentName.unflattenFromString(info.getId());
                            String pkg = componentName != null ? componentName.getPackageName() : null;
                            if (pkg != null && !activeWhiteList.containsKey(pkg)) {
                                ApplicationInfo app2 = info.getResolveInfo().serviceInfo.applicationInfo;
                                boolean isSystemApp = (app2.isSystemApp() || app2.isUpdatedSystemApp()) ? z : false;
                                if (isSystemApp) {
                                    activeWhiteList.put(pkg, false);
                                } else if ((info.feedbackType & 7) != 0) {
                                    activeWhiteList.put(componentName.getPackageName(), true);
                                }
                            }
                        }
                        z = true;
                    }
                }
                Log.d("ProcessManager", "update DY:" + Arrays.toString(activeWhiteList.keySet().toArray()));
                synchronized (sLock) {
                    sDynamicWhiteList = activeWhiteList;
                }
                return activeWhiteList;
            } catch (Throwable th3) {
                th = th3;
            }
        }
    }

    public void resetWhiteList(Context context, int userId) {
        updateDynamicWhiteList(context, userId);
        synchronized (sLock) {
            sUserDefinedWhiteList.clear();
        }
    }

    public void updateApplicationLockedState(final Context context, int userId, String packageName, boolean isLocked) {
        updateApplicationLockedState(packageName, userId, isLocked);
        BackgroundThread.getHandler().post(new Runnable() { // from class: com.android.server.am.ProcessPolicy.1
            @Override // java.lang.Runnable
            public void run() {
                ProcessPolicy.this.saveLockedAppIntoSettings(context);
            }
        });
        ProcessRecord targetApp = this.mProcessManagerService.getProcessRecord(packageName, userId);
        if (targetApp != null) {
            promoteLockedApp(targetApp);
        }
    }

    public static Long getMemoryThresholdForFastBooApp() {
        long memoryThreshold;
        long deviceTotalMem = Process.getTotalMemory() / RAM_SIZE_1GB;
        if (deviceTotalMem < 4) {
            memoryThreshold = 819200;
        } else if (deviceTotalMem < 8) {
            memoryThreshold = 1536000;
        } else {
            memoryThreshold = 2048000;
        }
        return Long.valueOf(memoryThreshold);
    }

    private void updateApplicationLockedState(String packageName, int userId, boolean isLocked) {
        synchronized (sLock) {
            Set<String> lockedApplication = sLockedApplicationList.get(Integer.valueOf(userId));
            if (lockedApplication == null) {
                lockedApplication = new HashSet();
                sLockedApplicationList.put(Integer.valueOf(userId), lockedApplication);
            }
            if (isLocked) {
                lockedApplication.add(packageName);
            } else {
                lockedApplication.remove(packageName);
                removeDefaultLockedAppIfExists(packageName);
            }
        }
    }

    private void removeDefaultLockedAppIfExists(String packageName) {
        Set<String> defaultLockedApps = sLockedApplicationList.get(-100);
        if (defaultLockedApps != null && defaultLockedApps.contains(packageName)) {
            defaultLockedApps.remove(packageName);
        }
    }

    protected void promoteLockedApp(ProcessRecord app) {
        if (app.isPersistent() || isInSecretlyProtectList(app.processName)) {
            Log.d("ProcessManager", "do not promote " + app.processName);
            return;
        }
        boolean isLocked = isLockedApplication(app.processName, app.userId);
        int targetMaxAdj = isLocked ? ProcessManager.LOCKED_MAX_ADJ : 1001;
        int targetMaxProcState = isLocked ? ProcessManager.LOCKED_MAX_PROCESS_STATE : 20;
        updateMaxAdjLocked(app, targetMaxAdj, isLocked);
        updateMaxProcStateLocked(app, targetMaxProcState, isLocked);
        Slog.d("ProcessManager", "promoteLockedApp:" + isLocked + ", set " + app.processName + " maxAdj to " + ProcessList.makeOomAdjString(app.mState.getMaxAdj(), false) + ", maxProcState to + " + ProcessList.makeProcStateString(getAppMaxProcState(app)));
    }

    public void saveLockedAppIntoSettings(Context context) {
        synchronized (sLock) {
            JSONArray userSpaceArray = new JSONArray();
            try {
                for (Integer userId : sLockedApplicationList.keySet()) {
                    JSONObject userSpaceObject = new JSONObject();
                    userSpaceObject.put(JSON_KEY_USER_ID, userId);
                    userSpaceObject.put(JSON_KEY_PACKAGE_NAMES, new JSONArray((Collection) sLockedApplicationList.get(userId)));
                    userSpaceArray.put(userSpaceObject);
                }
                Log.d("ProcessManager", "saveLockedAppIntoSettings:" + userSpaceArray.toString());
            } catch (Exception e) {
                Log.d("ProcessManager", "saveLockedAppIntoSettings failed: " + e.toString());
                e.printStackTrace();
            }
            MiuiSettings.System.putString(context.getContentResolver(), "locked_apps", userSpaceArray.toString());
        }
    }

    private void loadLockedAppFromSettings(Context context) {
        synchronized (sLock) {
            String jsonFormatText = MiuiSettings.System.getString(context.getContentResolver(), "locked_apps");
            if (TextUtils.isEmpty(jsonFormatText)) {
                return;
            }
            try {
                JSONArray userSpaceArray = new JSONArray(jsonFormatText);
                for (int spaceIndex = 0; spaceIndex < userSpaceArray.length(); spaceIndex++) {
                    JSONObject userSpaceObject = (JSONObject) userSpaceArray.get(spaceIndex);
                    int userId = userSpaceObject.getInt(JSON_KEY_USER_ID);
                    JSONArray packageNameArray = userSpaceObject.getJSONArray(JSON_KEY_PACKAGE_NAMES);
                    Set<String> packageNameSet = new HashSet<>();
                    for (int pkgIndex = 0; pkgIndex < packageNameArray.length(); pkgIndex++) {
                        packageNameSet.add(packageNameArray.getString(pkgIndex));
                    }
                    sLockedApplicationList.put(Integer.valueOf(userId), packageNameSet);
                    Log.d("ProcessManager", "loadLockedAppFromSettings userId:" + userId + "-pkgNames:" + Arrays.toString(packageNameSet.toArray()));
                }
            } catch (Exception e) {
                Log.d("ProcessManager", "loadLockedApp failed: " + e.toString());
                e.printStackTrace();
            }
        }
    }

    public boolean isLockedApplication(String packageName, int userId) {
        synchronized (sLock) {
            if (!isLockedApplicationForUserId(packageName, userId) && !isLockedApplicationForUserId(packageName, -100)) {
                return false;
            }
            return true;
        }
    }

    private boolean isLockedApplicationForUserId(String packageName, int userId) {
        Set<String> lockedApplication;
        if (packageName != null && (lockedApplication = sLockedApplicationList.get(Integer.valueOf(userId))) != null) {
            for (String item : lockedApplication) {
                if (item.equals(packageName)) {
                    return true;
                }
            }
        }
        return false;
    }

    public List<String> getLockedApplication(int userId) {
        List<String> lockedApps = new ArrayList<>();
        Set<String> userApps = sLockedApplicationList.get(Integer.valueOf(userId));
        if (userApps != null && userApps.size() > 0) {
            lockedApps.addAll(userApps);
        }
        lockedApps.addAll(sLockedApplicationList.get(-100));
        return lockedApps;
    }

    public void updateCloudData(ProcessCloudData cloudData) {
        updateCloudWhiteList(cloudData);
        updateAppProtectMap(cloudData);
        updateFgProtectMap(cloudData);
        updateFastBootList(cloudData);
        updateCameraMemThresholdMap(cloudData);
        updateSecretlyProtectAppList(cloudData);
    }

    private void updateCloudWhiteList(ProcessCloudData cloudData) {
        List<String> cloudWhiteList = cloudData.getCloudWhiteList();
        synchronized (sLock) {
            if (cloudWhiteList != null) {
                try {
                    if (!cloudWhiteList.isEmpty() && !cloudWhiteList.equals(sCloudWhiteList)) {
                        sCloudWhiteList.clear();
                        sCloudWhiteList.addAll(cloudWhiteList);
                        Log.d("ProcessManager", "update CL:" + Arrays.toString(sCloudWhiteList.toArray()));
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
            if ((cloudWhiteList == null || cloudWhiteList.isEmpty()) && !sCloudWhiteList.isEmpty()) {
                sCloudWhiteList.clear();
                Log.d("ProcessManager", "update CL:" + Arrays.toString(sCloudWhiteList.toArray()));
            }
        }
    }

    private void updateAppProtectMap(ProcessCloudData cloudData) {
        Map<String, Integer> appProtectMap = cloudData.getAppProtectMap();
        synchronized (sLock) {
            if (appProtectMap != null) {
                try {
                    if (!appProtectMap.isEmpty() && !appProtectMap.equals(sAppProtectMap)) {
                        sAppProtectMap.clear();
                        sAppProtectMap.putAll(appProtectMap);
                        Log.d("ProcessManager", "update AP:" + Arrays.toString(sAppProtectMap.keySet().toArray()));
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
            if ((appProtectMap == null || appProtectMap.isEmpty()) && !sAppProtectMap.isEmpty()) {
                sAppProtectMap.clear();
                Log.d("ProcessManager", "update AP:" + Arrays.toString(sAppProtectMap.keySet().toArray()));
            }
        }
    }

    private void updateFgProtectMap(ProcessCloudData cloudData) {
        Map<String, Integer> fgProtectMap = cloudData.getFgProtectMap();
        synchronized (sLock) {
            if (fgProtectMap != null) {
                try {
                    if (!fgProtectMap.isEmpty() && !fgProtectMap.equals(sFgServiceProtectMap)) {
                        sFgServiceProtectMap.clear();
                        sFgServiceProtectMap.putAll(fgProtectMap);
                        Log.d("ProcessManager", "update FG:" + Arrays.toString(sFgServiceProtectMap.keySet().toArray()));
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
            if ((fgProtectMap == null || fgProtectMap.isEmpty()) && !sFgServiceProtectMap.isEmpty()) {
                sFgServiceProtectMap.clear();
                Log.d("ProcessManager", "update FG:" + Arrays.toString(sFgServiceProtectMap.keySet().toArray()));
            }
        }
    }

    private void updateFastBootList(ProcessCloudData cloudData) {
        Set<String> oldFastBootSet;
        List<String> fastBootList = cloudData.getFastBootList();
        Object obj = sLock;
        synchronized (obj) {
            oldFastBootSet = sFastBootAppMap.keySet();
        }
        if (fastBootList != null && !fastBootList.isEmpty() && !oldFastBootSet.equals(new HashSet(fastBootList))) {
            synchronized (obj) {
                Map<String, Long> temp = new HashMap<>();
                for (String packageName : fastBootList) {
                    long thresholdKb = sFastBootAppMap.get(packageName).longValue();
                    temp.put(packageName, Long.valueOf(thresholdKb > 0 ? thresholdKb : DEFAULT_FASTBOOT_THRESHOLDKB));
                }
                sFastBootAppMap.clear();
                sFastBootAppMap.putAll(temp);
            }
            Log.d("ProcessManager", "update FA:" + Arrays.toString(sFastBootAppMap.keySet().toArray()));
        } else if ((fastBootList == null || fastBootList.isEmpty()) && !sFastBootAppMap.isEmpty()) {
            synchronized (obj) {
                sFastBootAppMap.clear();
            }
            Log.d("ProcessManager", "update FA:" + Arrays.toString(sFastBootAppMap.keySet().toArray()));
        }
    }

    private void updateCameraMemThresholdMap(ProcessCloudData cloudData) {
        Map<String, Integer> thresholdMap = cloudData.getCameraMemThresholdMap();
        synchronized (sLock) {
            if (thresholdMap != null) {
                try {
                    if (!thresholdMap.isEmpty() && !thresholdMap.equals(sCameraMemThresholdMap)) {
                        sCameraMemThresholdMap.clear();
                        sCameraMemThresholdMap.putAll(thresholdMap);
                        Log.d("ProcessManager", "update CM:" + Arrays.toString(sCameraMemThresholdMap.keySet().toArray()));
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
            if ((thresholdMap == null || thresholdMap.isEmpty()) && !sCameraMemThresholdMap.isEmpty()) {
                sCameraMemThresholdMap.clear();
                Log.d("ProcessManager", "update CM:" + Arrays.toString(sCameraMemThresholdMap.keySet().toArray()));
            }
        }
    }

    private void updateSecretlyProtectAppList(ProcessCloudData cloudData) {
        List<String> secretlyProtectAppList = cloudData.getSecretlyProtectAppList();
        synchronized (sLock) {
            if (secretlyProtectAppList != null) {
                try {
                    if (!secretlyProtectAppList.isEmpty() && !secretlyProtectAppList.equals(sSecretlyProtectAppList)) {
                        sSecretlyProtectAppList.clear();
                        sSecretlyProtectAppList.addAll(secretlyProtectAppList);
                        Log.d("ProcessManager", "update SPAL:" + Arrays.toString(sSecretlyProtectAppList.toArray()));
                    }
                } catch (Throwable th) {
                    throw th;
                }
            }
            if ((secretlyProtectAppList == null || secretlyProtectAppList.isEmpty()) && !sSecretlyProtectAppList.isEmpty()) {
                sSecretlyProtectAppList.clear();
                Log.d("ProcessManager", "update SPAL:" + Arrays.toString(sSecretlyProtectAppList.toArray()));
            }
        }
    }

    public boolean isProcessImportant(ProcessRecord app) {
        Pair<Boolean, Boolean> isInDynamicPair = isInDynamicList(app);
        return ((Boolean) isInDynamicPair.first).booleanValue() && (!((Boolean) isInDynamicPair.second).booleanValue() || app.mServices.hasForegroundServices() || app.mState.getCurAdj() <= PERCEPTIBLE_APP_ADJ);
    }

    public Pair<Boolean, Boolean> isInDynamicList(ProcessRecord app) {
        if (app != null) {
            synchronized (sLock) {
                String packageName = app.info.packageName;
                if (sDynamicWhiteList.keySet().contains(packageName)) {
                    return new Pair<>(Boolean.TRUE, sDynamicWhiteList.get(packageName));
                }
            }
        }
        return new Pair<>(Boolean.FALSE, Boolean.FALSE);
    }

    public boolean isFastBootEnable(String packageName, int uid, boolean checkPss) {
        return uid > 0 && isInFastBootList(packageName, uid, checkPss) && this.mProcessManagerService.isAllowAutoStart(packageName, uid);
    }

    public boolean isInFastBootList(String packageName, int uid, boolean checkPss) {
        boolean res;
        long pss = checkPss ? ProcessUtils.getPackageLastPss(packageName, UserHandle.getUserId(uid)) : 0L;
        synchronized (sLock) {
            res = sFastBootAppMap.keySet().contains(packageName);
            if (res && checkPss && pss > sFastBootAppMap.get(packageName).longValue()) {
                Log.w("ProcessManager", "ignore fast boot, caused pkg:" + packageName + " is too large, with pss:" + pss);
                res = false;
            }
        }
        return res;
    }

    public boolean isInAppProtectList(String packageName) {
        boolean contains;
        synchronized (sLock) {
            contains = sAppProtectMap.keySet().contains(packageName);
        }
        return contains;
    }

    public boolean isInProcessStaticWhiteList(String processName) {
        boolean contains;
        synchronized (sLock) {
            contains = sProcessStaticWhiteList.contains(processName);
        }
        return contains;
    }

    public boolean isInDisplaySizeWhiteList(String processName) {
        boolean contains;
        synchronized (sLock) {
            contains = sDisplaySizeProtectList.contains(processName);
        }
        return contains;
    }

    public boolean isInDisplaySizeBlackList(String processName) {
        boolean contains;
        synchronized (sLock) {
            contains = sDisplaySizeBlackList.contains(processName);
        }
        return contains;
    }

    public boolean isInSecretlyProtectList(String processName) {
        boolean contains;
        synchronized (sLock) {
            contains = sSecretlyProtectAppList.contains(processName);
        }
        return contains;
    }

    public long getCameraMemThreshold() {
        if (Build.VERSION.SDK_INT >= 28) {
            return (long) ((Process.getTotalMemory() / 1024) * CAMERA_BOOST_THRESHOLD_PERCENT);
        }
        Map<String, Integer> map = sCameraMemThresholdMap;
        String str = DEVICE;
        if (map.containsKey(str)) {
            return sCameraMemThresholdMap.get(str).intValue();
        }
        return -1L;
    }

    public boolean isCameraBoostEnable() {
        return Build.VERSION.SDK_INT >= 28 || sCameraMemThresholdMap.keySet().contains(DEVICE);
    }

    public boolean protectCurrentProcess(ProcessRecord app, boolean isProtected) {
        Integer priorityLevel;
        if (app == null || app.info == null) {
            return false;
        }
        synchronized (sLock) {
            priorityLevel = sAppProtectMap.get(app.info.packageName);
        }
        if (priorityLevel == null) {
            return false;
        }
        updateProcessPriority(app, priorityLevel.intValue(), isProtected);
        Slog.d("ProcessManager", "protectCurrentProcess:" + isProtected + ", set " + app.processName + " maxAdj to " + ProcessList.makeOomAdjString(app.mState.getMaxAdj(), false) + ", maxProcState to + " + ProcessList.makeProcStateString(getAppMaxProcState(app)));
        return true;
    }

    public void updateProcessForegroundLocked(ProcessRecord app) {
        Integer priorityLevel;
        if (app == null || app.info == null) {
            return;
        }
        synchronized (sLock) {
            priorityLevel = sFgServiceProtectMap.get(app.info.packageName);
        }
        if (priorityLevel == null) {
            return;
        }
        updateProcessPriority(app, priorityLevel.intValue(), app.mServices.hasForegroundServices());
        Slog.d("ProcessManager", "updateProcessForegroundLocked:" + app.mServices.hasForegroundServices() + ", set " + app.processName + " maxAdj to " + ProcessList.makeOomAdjString(app.mState.getMaxAdj(), false) + ", maxProcState to + " + ProcessList.makeProcStateString(getAppMaxProcState(app)));
    }

    private void updateProcessPriority(ProcessRecord app, int priorityLevel, boolean protect) {
        Pair<Integer, Integer> priorityPair = sProcessPriorityMap.get(priorityLevel);
        if (priorityPair != null) {
            int targetMaxAdj = protect ? ((Integer) priorityPair.first).intValue() : 1001;
            updateMaxAdjLocked(app, targetMaxAdj, protect);
            int targetMaxProcState = protect ? ((Integer) priorityPair.second).intValue() : 20;
            updateMaxProcStateLocked(app, targetMaxProcState, protect);
        }
    }

    private void updateMaxAdjLocked(ProcessRecord app, int targetMaxAdj, boolean protect) {
        if (app.isPersistent()) {
            return;
        }
        if (protect && app.mState.getMaxAdj() > targetMaxAdj) {
            app.mState.setMaxAdj(targetMaxAdj);
        } else if (!protect && app.mState.getMaxAdj() < targetMaxAdj) {
            app.mState.setMaxAdj(targetMaxAdj);
        }
    }

    private void updateMaxProcStateLocked(ProcessRecord app, int targetMaxProcState, boolean protect) {
        if (protect && getAppMaxProcState(app) > targetMaxProcState) {
            setAppMaxProcState(app, targetMaxProcState);
        } else if (!protect && getAppMaxProcState(app) < targetMaxProcState) {
            setAppMaxProcState(app, targetMaxProcState);
        }
    }

    public List<ActiveUidRecord> getActiveUidRecordList(int flag) {
        List<ActiveUidRecord> records;
        synchronized (sLock) {
            records = new ArrayList<>();
            for (int i = sActiveUidList.size() - 1; i >= 0; i--) {
                ActiveUidRecord r = sActiveUidList.valueAt(i);
                if ((r.flag & flag) != 0) {
                    records.add(r);
                }
            }
        }
        return records;
    }

    public List<Integer> getActiveUidList(int flag) {
        List<Integer> records;
        synchronized (sLock) {
            records = new ArrayList<>();
            for (int i = sActiveUidList.size() - 1; i >= 0; i--) {
                ActiveUidRecord r = sActiveUidList.valueAt(i);
                if ((r.flag & flag) != 0) {
                    records.add(Integer.valueOf(r.uid));
                }
            }
        }
        return records;
    }

    public void noteAudioOnLocked(int uid) {
        if (UserHandle.isApp(uid)) {
            synchronized (sLock) {
                ActiveUidRecord temp = sTempInactiveAudioList.get(uid);
                if (temp != null) {
                    this.mActiveUpdateHandler.removeMessages(1, temp);
                    sTempInactiveAudioList.remove(uid);
                    Slog.d("ProcessManager", "remove temp audio active uid : " + uid);
                } else {
                    ActiveUidRecord r = sActiveUidList.get(uid);
                    if (r == null) {
                        r = new ActiveUidRecord(uid);
                    }
                    r.flag = 1 | r.flag;
                    sActiveUidList.put(uid, r);
                    Slog.d("ProcessManager", "add audio active uid : " + uid);
                }
            }
        }
    }

    public void noteAudioOffLocked(int uid) {
        if (UserHandle.isApp(uid)) {
            synchronized (sLock) {
                ActiveUidRecord r = sActiveUidList.get(uid);
                if (r != null) {
                    sTempInactiveAudioList.put(uid, r);
                    Message msg = this.mActiveUpdateHandler.obtainMessage(1, r);
                    this.mActiveUpdateHandler.sendMessageDelayed(msg, UPDATE_AUDIO_OFF_DELAY);
                    Slog.d("ProcessManager", "add temp remove audio inactive uid : " + uid);
                }
            }
        }
    }

    public void noteResetAudioLocked() {
        synchronized (sLock) {
            List<ActiveUidRecord> removed = new ArrayList<>();
            int N = sActiveUidList.size();
            for (int i = 0; i < N; i++) {
                ActiveUidRecord r = sActiveUidList.valueAt(i);
                r.flag &= -2;
                if (r.flag == 0) {
                    removed.add(r);
                }
            }
            for (ActiveUidRecord r2 : removed) {
                sActiveUidList.remove(r2.uid);
            }
            Slog.d("ProcessManager", " noteResetAudioLocked removed ActiveUids : " + Arrays.toString(removed.toArray()));
        }
    }

    public void noteStartGpsLocked(int uid) {
        if (UserHandle.isApp(uid)) {
            synchronized (sLock) {
                ActiveUidRecord temp = sTempInactiveGPSList.get(uid);
                if (temp != null) {
                    this.mActiveUpdateHandler.removeMessages(2, temp);
                    sTempInactiveGPSList.remove(uid);
                    Slog.d("ProcessManager", "remove temp gps active uid : " + uid);
                } else {
                    ActiveUidRecord r = sActiveUidList.get(uid);
                    if (r == null) {
                        r = new ActiveUidRecord(uid);
                    }
                    r.flag = 2 | r.flag;
                    sActiveUidList.put(uid, r);
                    Slog.d("ProcessManager", "add gps active uid : " + uid);
                }
            }
        }
    }

    public void noteStopGpsLocked(int uid) {
        if (UserHandle.isApp(uid)) {
            synchronized (sLock) {
                ActiveUidRecord r = sActiveUidList.get(uid);
                if (r != null) {
                    sTempInactiveGPSList.put(uid, r);
                    Message msg = this.mActiveUpdateHandler.obtainMessage(2, r);
                    this.mActiveUpdateHandler.sendMessageDelayed(msg, UPDATE_STOP_GPS_DELAY);
                    Slog.d("ProcessManager", "add temp remove gps inactive uid : " + uid);
                }
            }
        }
    }

    public void dump(PrintWriter pw, String prefix) {
        pw.println("Process Policy:");
        if (sDynamicWhiteList.size() > 0) {
            pw.println("DY:");
            Set<String> dynamic = sDynamicWhiteList.keySet();
            for (String pkg : dynamic) {
                pw.print(prefix);
                pw.println(pkg + " : " + sDynamicWhiteList.get(pkg));
            }
        }
        if (sCloudWhiteList.size() > 0) {
            pw.println("CL:");
            for (int i = 0; i < sCloudWhiteList.size(); i++) {
                pw.print(prefix);
                pw.println(sCloudWhiteList.get(i));
            }
        }
        if (sUserDefinedWhiteList.size() > 0) {
            pw.println("US:");
            for (int i2 = 0; i2 < sUserDefinedWhiteList.size(); i2++) {
                pw.print(prefix);
                pw.println(sUserDefinedWhiteList.get(i2));
            }
        }
        if (sLockedApplicationList.size() > 0) {
            pw.println("LO:");
            Set<Integer> userIds = sLockedApplicationList.keySet();
            for (Integer userId : userIds) {
                Set<String> lockedApplication = sLockedApplicationList.get(userId);
                pw.print(prefix);
                pw.println("userId=" + userId);
                for (String app : lockedApplication) {
                    pw.print(prefix);
                    pw.println(app);
                }
            }
        }
        if (sFastBootAppMap.size() > 0) {
            pw.println("FA:");
            for (String protectedPackage : sFastBootAppMap.keySet()) {
                pw.print(prefix);
                pw.println(protectedPackage);
            }
        }
        if (EnterpriseSettings.ENTERPRISE_ACTIVATED) {
            pw.println("EP Activated: true");
            if (sEnterpriseAppList.size() > 0) {
                for (int i3 = 0; i3 < sEnterpriseAppList.size(); i3++) {
                    pw.print(prefix);
                    pw.println(sEnterpriseAppList.get(i3));
                }
            }
        }
        if (sSecretlyProtectAppList.size() > 0) {
            pw.println("SPAL:");
            for (int i4 = 0; i4 < sSecretlyProtectAppList.size(); i4++) {
                pw.print(prefix);
                pw.println(sSecretlyProtectAppList.get(i4));
            }
        }
        if (sActiveUidList.size() > 0) {
            pw.println("ACU:");
            for (int i5 = 0; i5 < sActiveUidList.size(); i5++) {
                pw.print(prefix);
                pw.println(sActiveUidList.valueAt(i5));
            }
        }
    }

    public static void setAppMaxProcState(ProcessRecord app, int targetMaxProcState) {
        app.mState.mMaxProcState = targetMaxProcState;
    }

    public static int getAppMaxProcState(ProcessRecord app) {
        return app.mState.mMaxProcState;
    }
}
