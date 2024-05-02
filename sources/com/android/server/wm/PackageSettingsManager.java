package com.android.server.wm;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.MiuiAppSizeCompatModeStub;
import android.util.Slog;
import com.android.server.am.PendingIntentRecord;
import com.android.server.wm.PackageSettingsManager;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
/* loaded from: classes.dex */
public class PackageSettingsManager {
    private static final String TAG = "PackageSettingsManager";
    private final ActivityTaskManagerService mAtmService;
    private final ActivityTaskManagerServiceImpl mAtmServiceImpl;
    public final DisplayCompatPackageManager mDisplayCompatPackages = new DisplayCompatPackageManager();
    private SafeActivityOptions mFullScreenLaunchOption;
    boolean mHasDisplayCutout;

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface DisplayCompatPolicy {
        public static final int CONTINUITY_RELAUNCH_BY_BLOCK_LIST = 6;
        public static final int CONTINUITY_RESTART_BY_BLOCK_LIST = 7;
        public static final int EXCLUDE_BY_META_DATA = 5;
        public static final int FORCED_RESIZEABLE_BY_ALLOW_LIST = 3;
        public static final int FORCED_RESIZEABLE_BY_USER_SETTING = 1;
        public static final int FORCED_UNRESIZEABLE_BY_BLOCK_LIST = 4;
        public static final int FORCED_UNRESIZEABLE_BY_USER_SETTING = 2;
        public static final int NONE = 0;
    }

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface RequestPackageSettings {
        public static final int APP_CONTINUITY_PACKAGES = 1;
        public static final int CUSTOM_ASPECT_RATIO_PACKAGES = 2;
    }

    public PackageSettingsManager(ActivityTaskManagerServiceImpl atmServiceImpl) {
        this.mAtmServiceImpl = atmServiceImpl;
        this.mAtmService = atmServiceImpl.mAtmService;
    }

    static String displayCompatPolicyToString(int policy) {
        if (policy == 0) {
            return "NONE";
        }
        if (policy == 1) {
            return "FORCED_RESIZEABLE_BY_USER_SETTING";
        }
        if (policy == 2) {
            return "FORCED_UNRESIZEABLE_BY_USER_SETTING";
        }
        if (policy == 3) {
            return "FORCED_RESIZEABLE_BY_ALLOW_LIST";
        }
        if (policy == 4) {
            return "FORCED_UNRESIZEABLE_BY_BLOCK_LIST";
        }
        if (policy == 7) {
            return "CONTINUITY_RESTART_BY_BLOCK_LIST";
        }
        if (policy == 6) {
            return "CROSS_PLATFORM_BY_BLOCK_LIST";
        }
        if (policy == 5) {
            return "EXCLUDE_BY_META_DATA";
        }
        return Integer.toString(policy);
    }

    public static boolean isForcedResizeableDisplayCompatPolicy(int policy) {
        if (policy == 3 || policy == 1 || policy == 5) {
            return true;
        }
        return false;
    }

    public static boolean isForcedUnresizeableDisplayCompatPolicy(int policy) {
        return policy == 4 || policy == 2 || policy == 7 || policy == 6;
    }

    public void dump(PrintWriter pw, String prefix) {
        if (!MiuiAppSizeCompatModeStub.get().isEnabled()) {
            pw.println("MiuiAppSizeCompatMode not enabled");
            return;
        }
        String innerPrefix = prefix + "  ";
        pw.println(prefix + "PACKAGE SETTINGS MANAGER");
        if (this.mHasDisplayCutout) {
            pw.println(innerPrefix + "mHasDisplayCutout=true");
        }
        pw.println();
        this.mDisplayCompatPackages.dump(pw, innerPrefix);
        pw.println();
    }

    void killAndRestartTask(int restartTaskId, final String packageName, ActivityOptions activityOptions, final int userId, String reason) {
        Task task;
        final TaskStarter taskStarter;
        synchronized (this.mAtmService.mGlobalLock) {
            WindowManagerService.boostPriorityForLockedSection();
            if (restartTaskId != -1) {
                Task targetTask = this.mAtmService.mRootWindowContainer.anyTaskForId(restartTaskId, 1);
                taskStarter = new TaskStarter(targetTask, activityOptions, reason);
                task = targetTask;
            } else {
                RootWindowContainer rootWindowContainer = this.mAtmService.mRootWindowContainer;
                task = rootWindowContainer.getTask(new Predicate() { // from class: com.android.server.wm.PackageSettingsManager$$ExternalSyntheticLambda0
                    @Override // java.util.function.Predicate
                    public final boolean test(Object obj) {
                        return PackageSettingsManager.lambda$killAndRestartTask$0(userId, packageName, (Task) obj);
                    }
                });
                taskStarter = null;
            }
            if (task != null) {
                this.mAtmService.mTaskSupervisor.removeTask(task, true, false, reason);
                if (taskStarter != null) {
                    this.mAtmService.mH.post(new Runnable() { // from class: com.android.server.wm.PackageSettingsManager$$ExternalSyntheticLambda1
                        @Override // java.lang.Runnable
                        public final void run() {
                            PackageSettingsManager.TaskStarter.this.restartTask();
                        }
                    });
                }
            }
            WindowManagerService.resetPriorityAfterLockedSection();
        }
    }

    public static /* synthetic */ boolean lambda$killAndRestartTask$0(int userId, String packageName, Task t) {
        if (t.mUserId == userId && t.realActivity != null) {
            String pkgName = t.realActivity.getPackageName();
            return packageName.equals(pkgName);
        }
        return false;
    }

    /* loaded from: classes.dex */
    public class DisplayCompatPackageManager {
        private Map<String, Integer> mPackagesMapByUserSettings;
        private final Map<String, Integer> mPackagesMapBySystem = new ConcurrentHashMap();
        final Consumer<ConcurrentHashMap<String, String>> mCallback = new Consumer() { // from class: com.android.server.wm.PackageSettingsManager$DisplayCompatPackageManager$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                PackageSettingsManager.DisplayCompatPackageManager.this.m1869xedb1ef77((ConcurrentHashMap) obj);
            }
        };

        DisplayCompatPackageManager() {
            PackageSettingsManager.this = this$0;
        }

        /* renamed from: lambda$new$0$com-android-server-wm-PackageSettingsManager$DisplayCompatPackageManager */
        public /* synthetic */ void m1869xedb1ef77(ConcurrentHashMap map) {
            this.mPackagesMapBySystem.clear();
            map.forEach(new BiConsumer<String, String>() { // from class: com.android.server.wm.PackageSettingsManager.DisplayCompatPackageManager.1
                public void accept(String key, String value) {
                    Slog.d("BoundsCompat", "DisplayCompatPackageManager key = " + key + " value = " + value);
                    if (FoldablePackagePolicy.POLICY_VALUE_ALLOW_LIST.equals(value)) {
                        DisplayCompatPackageManager.this.mPackagesMapBySystem.put(key, 3);
                    }
                    if (FoldablePackagePolicy.POLICY_VALUE_BLOCK_LIST.equals(value)) {
                        DisplayCompatPackageManager.this.mPackagesMapBySystem.put(key, 4);
                    }
                    if (FoldablePackagePolicy.POLICY_VALUE_RESTART_LIST.equals(value)) {
                        DisplayCompatPackageManager.this.mPackagesMapBySystem.put(key, 7);
                    }
                    if (FoldablePackagePolicy.POLICY_VALUE_RELAUNCH_LIST.equals(value)) {
                        DisplayCompatPackageManager.this.mPackagesMapBySystem.put(key, 6);
                    }
                }
            });
        }

        public void dump(PrintWriter pw, String prefix) {
            String innerPrefix = prefix + "  ";
            if (!this.mPackagesMapBySystem.isEmpty()) {
                pw.println(prefix + "DisplayCompatPackages(System)");
                for (Map.Entry<String, Integer> entry : this.mPackagesMapBySystem.entrySet()) {
                    pw.println(innerPrefix + "[" + entry.getKey() + "] " + PackageSettingsManager.displayCompatPolicyToString(entry.getValue().intValue()));
                }
            }
            Map<String, Integer> map = this.mPackagesMapByUserSettings;
            Map<String, Integer> map2 = this.mPackagesMapByUserSettings;
            if (map2 != null && !map2.isEmpty()) {
                pw.println(prefix + "DisplayCompatPackages(UserSetting)");
                for (Map.Entry<String, Integer> entry2 : this.mPackagesMapByUserSettings.entrySet()) {
                    pw.println(innerPrefix + "[" + entry2.getKey() + "] " + PackageSettingsManager.displayCompatPolicyToString(entry2.getValue().intValue()));
                }
            }
        }

        public void clearUserSettings() {
            Map<String, Integer> map = this.mPackagesMapByUserSettings;
            if (map != null) {
                map.clear();
            }
        }

        public int getPolicy(String packageName) {
            int policy;
            Slog.w(PackageSettingsManager.TAG, "DisplayCompatPackageManager getPolicy packageName = " + packageName);
            if (PackageSettingsManager.this.mAtmService.mForceResizableActivities) {
                return 3;
            }
            Map<String, Integer> map = this.mPackagesMapByUserSettings;
            if (map != null) {
                Object value = map.get(packageName);
                if ((value instanceof Integer) && ((policy = ((Integer) value).intValue()) == 1 || policy == 2)) {
                    return policy;
                }
            }
            Object value2 = this.mPackagesMapBySystem.get(packageName);
            if (value2 instanceof Integer) {
                int policy2 = ((Integer) value2).intValue();
                if (policy2 == 3 || policy2 == 4 || policy2 == 7 || policy2 == 6) {
                    return policy2;
                }
            } else if (PackageSettingsManager.this.mAtmServiceImpl.getMetaDataBoolean(packageName, "miui.supportAppContinuity") || PackageSettingsManager.this.mAtmServiceImpl.getMetaDataBoolean(packageName, "android.supports_size_changes")) {
                return 3;
            }
            if (!PackageSettingsManager.this.mAtmServiceImpl.getMetaDataBoolean(packageName, "miui.supportAppContinuity") && !PackageSettingsManager.this.mAtmServiceImpl.getMetaDataBoolean(packageName, "android.supports_size_changes")) {
                return 0;
            }
            return 5;
        }

        public void setPolicy(Map<? extends String, ? extends Integer> requestedPackages, boolean replaceAll) {
            Map<String, Integer> map = this.mPackagesMapByUserSettings;
            if (map == null) {
                this.mPackagesMapByUserSettings = new ConcurrentHashMap();
            } else if (replaceAll) {
                map.clear();
            }
            this.mPackagesMapByUserSettings.putAll(requestedPackages);
        }
    }

    /* loaded from: classes.dex */
    public class TaskStarter {
        final String callingFeatureId;
        final String callingPackage;
        final int callingUid;
        final Intent intent;
        final SafeActivityOptions mOptions;
        final String mReason;
        final int realCallingPid;
        final int realCallingUid;
        final int userId;

        private TaskStarter(Task task, ActivityOptions activityOptions, String reason) {
            PackageSettingsManager.this = r3;
            this.callingUid = task.mCallingUid;
            this.realCallingPid = Binder.getCallingPid();
            this.realCallingUid = Binder.getCallingUid();
            this.callingPackage = task.mCallingPackage;
            this.callingFeatureId = task.mCallingFeatureId;
            this.intent = task.intent;
            if (activityOptions != null) {
                if (activityOptions.getLaunchWindowingMode() != 1) {
                    activityOptions.setLaunchWindowingMode(1);
                }
                this.mOptions = new SafeActivityOptions(activityOptions);
            } else {
                if (r3.mFullScreenLaunchOption == null) {
                    ActivityOptions option = ActivityOptions.makeBasic();
                    option.setLaunchWindowingMode(1);
                    r3.mFullScreenLaunchOption = new SafeActivityOptions(option);
                }
                this.mOptions = r3.mFullScreenLaunchOption;
            }
            this.userId = task.mUserId;
            this.mReason = reason;
        }

        public void restartTask() {
            synchronized (PackageSettingsManager.this.mAtmService.mGlobalLock) {
                WindowManagerService.boostPriorityForLockedSection();
                PackageSettingsManager.this.mAtmService.getActivityStartController().startActivityInPackage(this.callingUid, this.realCallingPid, this.realCallingUid, this.callingPackage, this.callingFeatureId, this.intent, (String) null, (IBinder) null, (String) null, 0, 0, this.mOptions, this.userId, (Task) null, this.mReason, false, (PendingIntentRecord) null, true);
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }
}
