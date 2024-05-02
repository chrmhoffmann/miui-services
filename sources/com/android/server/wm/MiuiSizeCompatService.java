package com.android.server.wm;

import android.app.ActivityOptions;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Binder;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.sizecompat.AspectRatioInfo;
import android.sizecompat.IMiuiSizeCompat;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import android.util.Xml;
import android.view.IDisplayFoldListener;
import android.view.WindowManager;
import android.window.WindowContainerToken;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.function.pooled.PooledLambda;
import com.android.server.LocalServices;
import com.android.server.MiuiBgThread;
import com.android.server.MiuiFgThread;
import com.android.server.UiThread;
import com.android.server.am.ActivityManagerService;
import com.android.server.am.BroadcastQueueImpl;
import com.android.server.am.PendingIntentRecord;
import com.android.server.content.MiSyncConstants;
import com.android.server.inputmethod.InputMethodManagerServiceImpl;
import com.android.server.statusbar.StatusBarManagerInternal;
import com.android.server.wm.MiuiFreeformTrackManager;
import com.android.server.wm.MiuiSizeCompatService;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import libcore.io.IoUtils;
import miui.process.ForegroundInfo;
import miui.process.IForegroundInfoListener;
import miui.process.ProcessManager;
import miui.security.SecurityManager;
import miuix.appcompat.app.AlertDialog;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
/* loaded from: classes.dex */
public class MiuiSizeCompatService extends IMiuiSizeCompat.Stub {
    public static final String APP_CONTINUITY_FIX_RATIO_KEY = "app_continuity_o_fixratiolist";
    public static boolean DEBUG = false;
    public static final String FAST_XML = "http://xmlpull.org/v1/doc/features.html#indent-output";
    private static final String FORCE_RESIZABLE_ACTIVITIES = "force_resizable_activities";
    private static final int NOTIFICATION_ID = 2000;
    private static final String PACKAGE_NAME = "packageName";
    public static final String SERVICE_NAME = "MiuiSizeCompat";
    private static final String SETTING_CONFIG_BACKUP_FILE_NAME = "size_compat_setting_config-backup.xml";
    private static final String SETTING_CONFIG_FILE_NAME = "size_compat_setting_config.xml";
    private static final String SET_FIXED_ASPECT_RATIO_COMMAND = "-setFixedAspectRatio";
    private static final String SET_SIZE_COMPAT_DEBUG = "-setSizeCompatDebug";
    public static final String SYSTEM_USERS = "system/users/";
    private static final String TAG = "MiuiSizeCompatService";
    private static final String XML_ATTRIBUTE_ASPECT_RATIO = "aspectRatio";
    private static final String XML_ATTRIBUTE_DISPLAY_NAME = "displayName";
    private static final String XML_ATTRIBUTE_GRAVITY = "gravity";
    private static final String XML_ATTRIBUTE_NAME = "name";
    private static final String XML_ATTRIBUTE_SETTING_CONDIG = "setting_config";
    private static final String XML_ELEMENT_SETTING = "setting";
    private static final List<String> mCallerWhiteList;
    private static final List<String> mRestartTaskActs;
    private static final List<String> mRestartTaskApps;
    private ActivityManagerService mAms;
    private ActivityTaskManagerService mAtms;
    private File mBackupSettingFilename;
    private final String mCompatModeModuleName;
    private final Context mContext;
    private ActivityRecord mCurFullAct;
    private NotificationManager mNotificationManager;
    private boolean mNotificationShowing;
    private AlertDialog mRestartAppDialog;
    private SecurityManager mSecurityManager;
    private boolean mServiceReady;
    private File mSettingFilename;
    private StatusBarManagerInternal mStatusBarService;
    private Context mUiContext;
    private final String SIZE_COMPAT_CHANNEL_NAME = SERVICE_NAME;
    private final String SIZE_COMPAT_CHANNEL_ID = "WaringNotification";
    private final String NOTIFICATION_PACKAGE_ICON = "com.android.settings";
    private final String NOTIFICATION_ICON_KEY = "miui.appIcon";
    private final String NOTIFICATION_SERVICE_CLASS_NAME = "android.sizecompat.SizeCompatChangeService";
    private final String NOTIFICATION_SERVICE_PACKAGE = "android";
    private final Map<String, AspectRatioInfo> mSettingConfigs = new ConcurrentHashMap();
    private final Map<String, AspectRatioInfo> mStaticSettingConfigs = new ConcurrentHashMap();
    private final Map<String, AspectRatioInfo> mGameSettingConfig = new ConcurrentHashMap();
    private final String GAME_JSON_KEY_NAME = "pkgName";
    private final String GAME_JSON_KEY_RATIO = XML_ATTRIBUTE_ASPECT_RATIO;
    private final String GAME_JSON_KEY_GRAVITY = XML_ATTRIBUTE_GRAVITY;
    private final List<String> APPLIACTION_APP_BLACK_LIST = new ArrayList();
    private final Object mLock = new Object();
    private String PERMISSION_ACTIVITY = "com.lbe.security.miui/com.android.packageinstaller.permission.ui.GrantPermissionsActivity";
    private IForegroundInfoListener.Stub mAppObserver = new IForegroundInfoListener.Stub() { // from class: com.android.server.wm.MiuiSizeCompatService.2
        {
            MiuiSizeCompatService.this = this;
        }

        public void onForegroundInfoChanged(ForegroundInfo foregroundInfo) throws RemoteException {
            if (MiuiSizeCompatService.this.mCurFullAct == null || MiuiSizeCompatService.this.mCurFullAct.packageName == null || !MiuiSizeCompatService.this.mNotificationShowing) {
                MiuiSizeCompatService.this.cancelSizeCompatNotification("exit foregroud");
            }
            if (foregroundInfo != null && !MiuiSizeCompatService.this.mCurFullAct.packageName.equals(foregroundInfo.mForegroundPackageName)) {
                if (MiuiSizeCompatService.DEBUG) {
                    Slog.d(MiuiSizeCompatService.TAG, "Last is " + MiuiSizeCompatService.this.mCurFullAct.packageName + " and current is " + foregroundInfo.mForegroundPackageName);
                }
                ActivityRecord top = MiuiSizeCompatService.this.mAtms.mRootWindowContainer.getTopResumedActivity();
                if (top != null && !MiuiSizeCompatService.this.PERMISSION_ACTIVITY.equals(top.shortComponentName)) {
                    MiuiSizeCompatService.this.cancelSizeCompatNotification("exit foregroud");
                }
            }
        }
    };
    private IDisplayFoldListener.Stub mFoldObserver = new IDisplayFoldListener.Stub() { // from class: com.android.server.wm.MiuiSizeCompatService.3
        {
            MiuiSizeCompatService.this = this;
        }

        public void onDisplayFoldChanged(int displayId, boolean folded) throws RemoteException {
            if (MiuiSizeCompatService.this.mCurFullAct == null || MiuiSizeCompatService.this.mCurFullAct.packageName == null || !MiuiSizeCompatService.this.mNotificationShowing || folded) {
                if (MiuiSizeCompatService.DEBUG) {
                    Slog.d(MiuiSizeCompatService.TAG, MiuiSizeCompatService.this.mCurFullAct.shortComponentName + " ,mNotificationShowing= " + MiuiSizeCompatService.this.mNotificationShowing + " ,folded= " + folded);
                }
                MiuiSizeCompatService.this.cancelSizeCompatNotification("device fold");
            }
        }
    };
    private final H mBgHandler = new H(MiuiBgThread.getHandler().getLooper());
    private final UiHandler mUiHandler = new UiHandler(UiThread.getHandler().getLooper());
    private final Handler mFgHandler = MiuiFgThread.getHandler();

    static {
        ArrayList arrayList = new ArrayList();
        mRestartTaskApps = arrayList;
        ArrayList arrayList2 = new ArrayList();
        mRestartTaskActs = arrayList2;
        ArrayList arrayList3 = new ArrayList();
        mCallerWhiteList = arrayList3;
        arrayList.add("com.cntaiping.tpapp");
        arrayList.add("com.oda_cad");
        arrayList.add("com.jime.encyclopediascanning");
        arrayList.add("com.tmri.app.main");
        arrayList.add("com.mahjong.nb");
        arrayList.add("com.yusi.chongchong");
        arrayList2.add("com.lianjia.common.vr.webview.VrWebviewActivity");
        arrayList2.add("com.alipay.mobile.quinox.LauncherActivity");
        arrayList3.add("com.android.settings");
        arrayList3.add("com.android.systemui");
        arrayList3.add("android");
        arrayList3.add(ActivityTaskSupervisorImpl.MIUI_APP_LOCK_PACKAGE_NAME);
    }

    public MiuiSizeCompatService(Context context) {
        this.mContext = context;
        LocalServices.addService(MiuiSizeCompatInternal.class, new LocalService());
        this.mCompatModeModuleName = context.getResources().getString(286196523);
        initBlackAppList();
    }

    public void onSystemReady(ActivityTaskManagerService atms) {
        this.mAtms = atms;
        this.mAms = atms.mWindowManager.mActivityManager;
        this.mSecurityManager = (SecurityManager) this.mContext.getSystemService("security");
        this.mNotificationManager = (NotificationManager) this.mContext.getSystemService("notification");
        this.mStatusBarService = (StatusBarManagerInternal) LocalServices.getService(StatusBarManagerInternal.class);
        this.mUiContext = atms.mUiContext;
        initStaticSettings();
        registerUserSwitchReceiver();
    }

    public void initBlackAppList() {
        this.APPLIACTION_APP_BLACK_LIST.add("com.android.email");
        this.APPLIACTION_APP_BLACK_LIST.add("com.android.soundrecorder");
        this.APPLIACTION_APP_BLACK_LIST.add("com.duokan.phone.remotecontroller");
        this.APPLIACTION_APP_BLACK_LIST.add("com.mi.health");
        this.APPLIACTION_APP_BLACK_LIST.add("com.duokan.reader");
    }

    public void registerDataObserver() {
        updateSettingConfigsFromCloud();
        this.mContext.getContentResolver().registerContentObserver(MiuiSettings.SettingsCloudData.getCloudDataNotifyUri(), true, new ContentObserver(this.mBgHandler) { // from class: com.android.server.wm.MiuiSizeCompatService.1
            {
                MiuiSizeCompatService.this = this;
            }

            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                Slog.w(MiuiSizeCompatService.TAG, "MiuiSizeCompat policySettingConfigsFromCloud onChange--");
                MiuiSizeCompatService.this.updateSettingConfigsFromCloud();
            }
        });
    }

    public void updateSettingConfigsFromCloud() {
        String data;
        long start = SystemClock.uptimeMillis();
        try {
            data = MiuiSettings.SettingsCloudData.getCloudDataString(this.mContext.getContentResolver(), this.mCompatModeModuleName, APP_CONTINUITY_FIX_RATIO_KEY, (String) null);
            Slog.d(TAG, "getListFromCloud: data: " + data);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (TextUtils.isEmpty(data)) {
            return;
        }
        JSONArray apps = new JSONArray(data);
        if (apps.length() <= 0) {
            return;
        }
        this.mStaticSettingConfigs.clear();
        for (int i = 0; i < apps.length(); i++) {
            String app = apps.getString(i);
            if (!TextUtils.isEmpty(app)) {
                String[] split = app.split(",");
                float ratio = parseRatioFromStr(split.length == 2 ? split[1] : "");
                this.mStaticSettingConfigs.put(split[0], new AspectRatioInfo(split[0], ratio));
            }
        }
        checkSlow(start, 100L, "updateSettingConfigsFromCloud");
    }

    private void initStaticSettings() {
        try {
            long start = SystemClock.uptimeMillis();
            String[] ratioPackages = this.mContext.getResources().getStringArray(285409344);
            if (ratioPackages != null && ratioPackages.length != 0) {
                for (String ratioPackage : ratioPackages) {
                    String[] split = ratioPackage.split(",");
                    float ratio = parseRatioFromStr(split.length == 2 ? split[1] : "");
                    this.mStaticSettingConfigs.put(split[0], new AspectRatioInfo(split[0], ratio));
                }
                Slog.d(TAG, "Static setting configs: " + this.mStaticSettingConfigs.size());
                checkSlow(start, 100L, "initStaticSettings");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    float parseRatioFromStr(String value) {
        if ("".equals(value)) {
            return 1.7777778f;
        }
        if ("-1".equals(value)) {
            return -1.0f;
        }
        if (MiuiFreeformTrackManager.SmallWindowTrackConstants.STACK_RATIO3.equals(value)) {
            return 1.3333333f;
        }
        String[] values = value.split(":");
        if (values.length != 2) {
            return 1.7777778f;
        }
        float width = Float.parseFloat(values[0]);
        float height = Float.parseFloat(values[1]);
        float ratio = width / height;
        return ratio;
    }

    /* loaded from: classes.dex */
    public final class H extends Handler {
        public static final int MSG_USER_SWITCH = 1;
        public static final int MSG_USER_UNLOCKED = 2;
        public static final int MSG_WRITE_CONFIRM = 3;
        public static final int MSG_WRITE_SETTINGS = 4;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public H(Looper looper) {
            super(looper);
            MiuiSizeCompatService.this = this$0;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    MiuiSizeCompatService.this.onUserSwitch();
                    return;
                case 2:
                    MiuiSizeCompatService.this.mBgHandler.postDelayed(new Runnable() { // from class: com.android.server.wm.MiuiSizeCompatService$H$$ExternalSyntheticLambda0
                        @Override // java.lang.Runnable
                        public final void run() {
                            MiuiSizeCompatService.H.this.m1859xb9ef4939();
                        }
                    }, 10000L);
                    return;
                case 3:
                    if (MiuiSizeCompatService.this.mSecurityManager != null && (msg.obj instanceof String)) {
                        boolean z = true;
                        if (msg.arg1 != 1) {
                            z = false;
                        }
                        boolean confirm = z;
                        MiuiSizeCompatService.this.mSecurityManager.setScRelaunchNeedConfirmForUser((String) msg.obj, confirm, msg.arg2);
                        return;
                    }
                    return;
                case 4:
                    MiuiSizeCompatService.this.writeSetting();
                    return;
                default:
                    throw new IllegalStateException("Unexpected value: " + msg.what);
            }
        }

        /* renamed from: lambda$handleMessage$0$com-android-server-wm-MiuiSizeCompatService$H */
        public /* synthetic */ void m1859xb9ef4939() {
            MiuiSizeCompatJob.sheduleJob(MiuiSizeCompatService.this.mContext);
            MiuiSizeCompatService.this.registerDataObserver();
        }
    }

    public void restartProcessForRatioLocked(ActivityRecord ar, float aspectRatio) {
        float aspectRatio2;
        ActivityRecord restart;
        long start = SystemClock.uptimeMillis();
        if (ar == null || ar.packageName == null || !this.mServiceReady) {
            return;
        }
        String packageName = ar.packageName;
        AspectRatioInfo info = this.mSettingConfigs.get(packageName);
        if (info != null) {
            aspectRatio2 = aspectRatio;
        } else {
            aspectRatio2 = 0.0f;
        }
        Slog.d(TAG, "Original ar is " + ar.shortComponentName);
        setMiuiSizeCompatRatio(packageName, aspectRatio2, true, false);
        synchronized (this.mAtms.mGlobalLock) {
            restart = ar.isTopRunningActivity() ? ar : getRestartAct(packageName);
            if (restart != null) {
                if (!mRestartTaskApps.contains(packageName) && !isInRestartTaskActs(ar)) {
                    restart.restartProcessIfVisible();
                }
                restartTask(restart.getRootTaskId());
                Slog.d(TAG, packageName + " is in restart task list ,so restart task instead.");
            }
        }
        if (restart == null) {
            removeRunningApp(packageName);
        }
        checkSlow(start, 200L, "restartProcessForRatioLocked");
    }

    private ActivityRecord getRestartAct(String packageName) {
        ActivityRecord below;
        ActivityRecord top = this.mAtms.mRootWindowContainer.getTopResumedActivity();
        if (top == null) {
            return null;
        }
        if (TextUtils.equals(top.packageName, packageName)) {
            Slog.d(TAG, "Real restart for " + top.shortComponentName);
            return top;
        } else if (!this.PERMISSION_ACTIVITY.equals(top.shortComponentName) || top.getTask() == null || (below = top.getTask().getActivityBelow(top)) == null || !packageName.equals(below.packageName)) {
            return null;
        } else {
            Slog.d(TAG, "Real restart for " + below.shortComponentName);
            return below;
        }
    }

    private void restartTask(int taskId) {
        WindowManagerService.boostPriorityForLockedSection();
        Task task = this.mAtms.mRootWindowContainer.anyTaskForId(taskId);
        if (task == null) {
            Slog.d(TAG, "Restart task fail, task is null.");
            return;
        }
        Intent intent = task.intent;
        int callingUid = task.mCallingUid;
        int realCallingUid = Binder.getCallingUid();
        int realCallingPid = Binder.getCallingPid();
        String callingPackage = task.mCallingPackage;
        String callingFeatureId = task.mCallingFeatureId;
        int userId = task.mUserId;
        task.removeImmediately("sizecompat");
        ActivityOptions option = ActivityOptions.makeBasic();
        option.setLaunchWindowingMode(1);
        SafeActivityOptions options = new SafeActivityOptions(option);
        this.mAtms.getActivityStartController().startActivityInPackage(callingUid, realCallingPid, realCallingUid, callingPackage, callingFeatureId, intent, (String) null, (IBinder) null, (String) null, 0, 0, options, userId, (Task) null, "sizecompat", false, (PendingIntentRecord) null, true);
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    private boolean isInRestartTaskActs(ActivityRecord ar) {
        if (ar == null || ar.mActivityComponent == null) {
            return false;
        }
        return mRestartTaskActs.contains(ar.mActivityComponent.getClassName());
    }

    /* loaded from: classes.dex */
    public class UiHandler extends Handler {
        public static final int MSG_SHOW_APP_RESTART_DIALOG = 1;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public UiHandler(Looper looper) {
            super(looper);
            MiuiSizeCompatService.this = r1;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    if (msg.obj instanceof ActivityRecord) {
                        try {
                            MiuiSizeCompatService.this.showConfirmDialog((ActivityRecord) msg.obj);
                            return;
                        } catch (Exception e) {
                            e.printStackTrace();
                            return;
                        }
                    }
                    return;
                default:
                    throw new IllegalStateException("Unexpected value: " + msg.what);
            }
        }
    }

    void registerUserSwitchReceiver() {
        IntentFilter userSwitchFilter = new IntentFilter();
        userSwitchFilter.addAction("android.intent.action.USER_SWITCHED");
        userSwitchFilter.addAction("android.intent.action.USER_UNLOCKED");
        this.mContext.registerReceiver(new BroadcastReceiver() { // from class: com.android.server.wm.MiuiSizeCompatService.4
            {
                MiuiSizeCompatService.this = this;
            }

            /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                char c;
                String action = intent.getAction();
                switch (action.hashCode()) {
                    case 833559602:
                        if (action.equals("android.intent.action.USER_UNLOCKED")) {
                            c = 1;
                            break;
                        }
                        c = 65535;
                        break;
                    case 959232034:
                        if (action.equals("android.intent.action.USER_SWITCHED")) {
                            c = 0;
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
                        MiuiSizeCompatService.this.mBgHandler.sendMessage(MiuiSizeCompatService.this.mBgHandler.obtainMessage(1));
                        return;
                    case 1:
                        MiuiSizeCompatService.this.mBgHandler.sendMessage(MiuiSizeCompatService.this.mBgHandler.obtainMessage(2));
                        return;
                    default:
                        Slog.d(MiuiSizeCompatService.TAG, "Other action.");
                        return;
                }
            }
        }, userSwitchFilter);
    }

    public void onUserSwitch() {
        initSettingsDirForUser(this.mAtms.getCurrentUserId());
        this.mSettingConfigs.clear();
        readSetting();
        synchronized (this.mLock) {
            this.mServiceReady = true;
            this.mLock.notifyAll();
        }
    }

    public Map<String, AspectRatioInfo> getMiuiSizeCompatEnabledApps() {
        long start = SystemClock.uptimeMillis();
        final Map<String, AspectRatioInfo> map = new HashMap<>();
        if (!this.mServiceReady) {
            Slog.d(TAG, "Get miui size compat enableds apps fail : service is not ready.");
            return map;
        }
        this.mStaticSettingConfigs.forEach(new BiConsumer<String, AspectRatioInfo>() { // from class: com.android.server.wm.MiuiSizeCompatService.5
            {
                MiuiSizeCompatService.this = this;
            }

            public void accept(String pkg, AspectRatioInfo aspectRatioInfo) {
                map.put(pkg, aspectRatioInfo);
            }
        });
        this.mSettingConfigs.forEach(new BiConsumer<String, AspectRatioInfo>() { // from class: com.android.server.wm.MiuiSizeCompatService.6
            {
                MiuiSizeCompatService.this = this;
            }

            public void accept(String pkg, AspectRatioInfo aspectRatioInfo) {
                map.put(pkg, aspectRatioInfo);
            }
        });
        checkSlow(start, 200L, "getMiuiSizeCompatEnabledApps");
        return map;
    }

    public Map<String, AspectRatioInfo> getMiuiSizeCompatInstalledApps() {
        AspectRatioInfo info;
        long start = SystemClock.uptimeMillis();
        Map<String, AspectRatioInfo> map = new HashMap<>();
        if (!this.mServiceReady) {
            Slog.d(TAG, "Get miui size compat installed apps fail : service is not ready.");
            return map;
        }
        PackageManager packageManager = this.mContext.getPackageManager();
        List<PackageInfo> list = packageManager.getInstalledPackages(64);
        for (PackageInfo object : list) {
            try {
                if (!isSystemApp(object.applicationInfo) && pkgHasIcon(object.packageName) && !this.APPLIACTION_APP_BLACK_LIST.contains(object.packageName)) {
                    String packageName = object.applicationInfo.packageName;
                    String label = (String) object.applicationInfo.loadLabel(packageManager);
                    if (this.mSettingConfigs.containsKey(packageName)) {
                        info = this.mSettingConfigs.get(packageName);
                    } else if (this.mStaticSettingConfigs.containsKey(packageName)) {
                        info = this.mStaticSettingConfigs.get(packageName);
                    } else {
                        info = new AspectRatioInfo(packageName, -1.0f);
                    }
                    if (TextUtils.isEmpty(info.mApplicationName)) {
                        info.mApplicationName = label;
                    }
                    map.put(packageName, info);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        checkSlow(start, 400L, "getMiuiSizeCompatInstalledApps");
        return map;
    }

    public boolean isSystemApp(ApplicationInfo info) {
        return (info.flags & 1) > 0 || info.uid < 10000 || info.packageName.contains("com.miui") || info.packageName.contains(MiSyncConstants.Config.XIAOMI_ACCOUNT_TYPE);
    }

    public boolean pkgHasIcon(String pkgName) {
        LauncherApps launcherApps = (LauncherApps) this.mContext.getSystemService("launcherapps");
        List<LauncherActivityInfo> infos = launcherApps.getActivityList(pkgName, UserHandle.SYSTEM);
        return !infos.isEmpty();
    }

    public boolean setMiuiSizeCompatEnabled(String pkgName, AspectRatioInfo info) {
        if (!this.mServiceReady) {
            Slog.d(TAG, "Set miui size compat enabled fail: service is not ready.");
            return false;
        }
        long start = SystemClock.uptimeMillis();
        if (info == null || TextUtils.isEmpty(pkgName)) {
            Slog.d(TAG, "Set miui size compat enabled fail: App pkgName or aspect info is null.");
            return false;
        }
        this.mSettingConfigs.put(pkgName, info);
        writeSetting();
        checkSlow(start, 300L, "setMiuiSizeCompatEnabled");
        return true;
    }

    public boolean setMiuiSizeCompatRatio(String pkgName, float ratio, boolean write, boolean kill) {
        AspectRatioInfo info;
        if (!this.mServiceReady) {
            Slog.d(TAG, "Set miui size compat ratio fail : service is not ready.");
            return false;
        }
        long start = SystemClock.uptimeMillis();
        int callingPid = Binder.getCallingPid();
        int callingUid = Binder.getCallingUid();
        if (TextUtils.isEmpty(pkgName)) {
            Slog.d(TAG, "Set miui size compat ratio fail : pkgName is null.");
            return false;
        } else if (!checkInterfaceAccess(callingPid, callingUid)) {
            Slog.e(TAG, "Do not allow pid= " + callingPid + " uid= " + callingUid + " to change miui size compat ratio.");
            return false;
        } else {
            if (DEBUG || callingPid != Process.myPid()) {
                Slog.d(TAG, "setMiuiSizeCompatRatio " + pkgName + " ratio = " + ratio + " from pid " + callingPid);
            }
            AspectRatioInfo info2 = this.mSettingConfigs.get(pkgName);
            if (info2 != null) {
                info = info2;
            } else {
                AspectRatioInfo staticInfo = this.mStaticSettingConfigs.get(pkgName);
                AspectRatioInfo info3 = staticInfo != null ? new AspectRatioInfo(pkgName, staticInfo.mAspectRatio) : new AspectRatioInfo(pkgName, -1.0f);
                this.mSettingConfigs.put(pkgName, info3);
                info = info3;
            }
            info.setAspectRatio(ratio);
            if (write) {
                H h = this.mBgHandler;
                h.sendMessage(h.obtainMessage(4));
            }
            if (kill) {
                removeRunningApp(pkgName);
            }
            checkSlow(start, 100L, "setMiuiSizeCompatRatio, write: " + write + " , kill= " + kill);
            return true;
        }
    }

    private boolean checkInterfaceAccess(int callingPid, int callingUid) {
        if (callingPid == Process.myPid() || callingUid == 0 || callingUid == NOTIFICATION_ID) {
            return true;
        }
        synchronized (this.mAtms.mGlobalLock) {
            WindowProcessController wpc = this.mAtms.mProcessMap.getProcess(callingPid);
            if (wpc != null && wpc.mInfo != null && !TextUtils.isEmpty(wpc.mInfo.packageName)) {
                return mCallerWhiteList.contains(wpc.mInfo.packageName);
            }
            return false;
        }
    }

    public void removeRunningApp(String pkgName) {
        this.mAms.forceStopPackage(pkgName, this.mAtms.getCurrentUserId());
        Slog.i(TAG, " Force stop " + pkgName + " due to sizecompatRatio change");
    }

    public boolean switchToFullscreen(WindowContainerToken token) {
        ActivityTaskManagerService.enforceTaskPermission("restartTopActivityProcessIfVisible()");
        try {
            long start = SystemClock.uptimeMillis();
            ActivityRecord activity = getActivityFromTokenLocked(token);
            if (activity != null && activity.packageName != null) {
                if (this.mGameSettingConfig.get(activity.packageName) != null) {
                    Slog.e(TAG, "Game app can not switch to fullscreen by systemui button.");
                    return false;
                }
                boolean confirm = this.mSecurityManager.isScRelaunchNeedConfirm(activity.packageName);
                if (confirm) {
                    UiHandler uiHandler = this.mUiHandler;
                    uiHandler.sendMessage(uiHandler.obtainMessage(1, activity));
                } else {
                    this.mCurFullAct = activity;
                    restartProcessForRatioLocked(activity, MiuiFreeformPinManagerService.EDGE_AREA);
                }
                checkSlow(start, 200L, "switchToFullscreen");
                return true;
            }
            Slog.e(TAG, "Could not resolve activity from token");
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void restorePrevRatio() {
        if (this.mCurFullAct != null && this.mNotificationShowing) {
            MiuiSizeCompatService$$ExternalSyntheticLambda0 miuiSizeCompatService$$ExternalSyntheticLambda0 = new MiuiSizeCompatService$$ExternalSyntheticLambda0();
            ActivityRecord activityRecord = this.mCurFullAct;
            Message message = PooledLambda.obtainMessage(miuiSizeCompatService$$ExternalSyntheticLambda0, this, activityRecord, Float.valueOf(getLastAspectRatioValue(activityRecord.packageName)));
            this.mFgHandler.sendMessage(message);
        }
    }

    public void cancelSizeCompatNotification(String reason) {
        try {
            long start = SystemClock.uptimeMillis();
            StatusBarManagerInternal statusBarManagerInternal = this.mStatusBarService;
            if (statusBarManagerInternal != null) {
                statusBarManagerInternal.collapsePanels();
            }
            this.mNotificationManager.cancel(NOTIFICATION_ID);
            ProcessManager.unregisterForegroundInfoListener(this.mAppObserver);
            this.mAtms.mWindowManager.unregisterDisplayFoldListener(this.mFoldObserver);
            this.mNotificationShowing = false;
            this.mCurFullAct = null;
            checkSlow(start, 100L, "cancelSizeCompatNotification");
            Slog.d(TAG, "Cancel warning notification for " + reason);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean setMiuiGameSizeCompatList(String json, boolean override) {
        if (override) {
            try {
                this.mGameSettingConfig.clear();
            } catch (JSONException e) {
                e.printStackTrace();
                Slog.e(TAG, "The game list json is error format.\n" + json);
                return false;
            } catch (Exception e2) {
                e2.printStackTrace();
                return false;
            }
        }
        if (TextUtils.isEmpty(json)) {
            Slog.d(TAG, "The game json list is empty, override is " + override);
            return true;
        }
        JSONArray jsonArray = new JSONArray(json);
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject object = (JSONObject) jsonArray.get(i);
            String pkgName = (String) object.get("pkgName");
            int gravity = ((Integer) object.get(XML_ATTRIBUTE_GRAVITY)).intValue();
            if (!AspectRatioInfo.isGravityEffect(gravity)) {
                Slog.e(TAG, "The gravity is not effect, use default center gravity instead.");
                gravity = 17;
            }
            float ratio = parseAspectRatio(object.get(XML_ATTRIBUTE_ASPECT_RATIO));
            this.mGameSettingConfig.put(pkgName, new AspectRatioInfo(pkgName, ratio, gravity));
        }
        if (DEBUG) {
            Log.d(TAG, "setMiuiGameSizeCompatList：" + json);
        }
        return true;
    }

    public float getMiuiSizeCompatAppRatio(String pkgName) {
        if (TextUtils.isEmpty(pkgName)) {
            Slog.e(TAG, "pkgName can not be empty");
            return -1.0f;
        }
        AspectRatioInfo info = this.mSettingConfigs.get(pkgName);
        if (info == null) {
            info = this.mStaticSettingConfigs.get(pkgName);
        }
        if (info == null) {
            return -1.0f;
        }
        return info.mAspectRatio;
    }

    private float parseAspectRatio(Object obj) {
        if (obj instanceof Float) {
            return ((Float) obj).floatValue();
        }
        if (obj instanceof Double) {
            return ((Double) obj).floatValue();
        }
        if (obj instanceof Integer) {
            return ((Integer) obj).floatValue();
        }
        if (obj instanceof String) {
            return Float.parseFloat((String) obj);
        }
        Slog.e(TAG, "The game aspect ratio is error format");
        return MiuiFreeformPinManagerService.EDGE_AREA;
    }

    public Map<String, AspectRatioInfo> getMiuiGameSizeCompatEnabledApps() {
        long start = SystemClock.uptimeMillis();
        final Map<String, AspectRatioInfo> map = new HashMap<>();
        if (!this.mServiceReady) {
            Slog.d(TAG, "Get miui game size compat enableds apps fail : service is not ready.");
            return map;
        }
        this.mGameSettingConfig.forEach(new BiConsumer<String, AspectRatioInfo>() { // from class: com.android.server.wm.MiuiSizeCompatService.7
            {
                MiuiSizeCompatService.this = this;
            }

            public void accept(String pkg, AspectRatioInfo aspectRatioInfo) {
                map.put(pkg, aspectRatioInfo);
            }
        });
        checkSlow(start, 200L, "getMiuiGameSizeCompatList");
        return map;
    }

    private ActivityRecord getActivityFromTokenLocked(WindowContainerToken token) {
        synchronized (this.mAtms.mGlobalLock) {
            WindowContainer wc = WindowContainer.fromBinder(token.asBinder());
            if (wc == null) {
                Slog.w(TAG, "Could not resolve window from token");
                return null;
            }
            Task task = wc.asTask();
            if (task == null) {
                Slog.w(TAG, "Could not resolve task from token");
                return null;
            }
            return task.getTopNonFinishingActivity();
        }
    }

    public void showConfirmDialog(final ActivityRecord activity) {
        int themeId;
        long start = SystemClock.uptimeMillis();
        if (activity != null && activity.packageName != null) {
            this.mCurFullAct = null;
            AlertDialog alertDialog = this.mRestartAppDialog;
            if (alertDialog != null && alertDialog.isShowing()) {
                this.mRestartAppDialog.dismiss();
            }
            int themeId2 = this.mUiContext.getResources().getIdentifier("AlertDialog.Theme.DayNight", "style", InputMethodManagerServiceImpl.MIUIXPACKAGE);
            if (themeId2 != 0) {
                themeId = themeId2;
            } else {
                themeId = 16974545;
            }
            String title = this.mUiContext.getResources().getString(286196451, getAppNameByPkg(activity.packageName));
            DialogInterface.OnClickListener positiveListener = new DialogInterface.OnClickListener() { // from class: com.android.server.wm.MiuiSizeCompatService$$ExternalSyntheticLambda1
                @Override // android.content.DialogInterface.OnClickListener
                public final void onClick(DialogInterface dialogInterface, int i) {
                    MiuiSizeCompatService.this.m1857x87f26ac1(activity, dialogInterface, i);
                }
            };
            DialogInterface.OnClickListener negativeListener = new DialogInterface.OnClickListener() { // from class: com.android.server.wm.MiuiSizeCompatService$$ExternalSyntheticLambda2
                @Override // android.content.DialogInterface.OnClickListener
                public final void onClick(DialogInterface dialogInterface, int i) {
                    MiuiSizeCompatService.lambda$showConfirmDialog$1(dialogInterface, i);
                }
            };
            DialogInterface.OnCancelListener cancelListener = new DialogInterface.OnCancelListener() { // from class: com.android.server.wm.MiuiSizeCompatService$$ExternalSyntheticLambda3
                @Override // android.content.DialogInterface.OnCancelListener
                public final void onCancel(DialogInterface dialogInterface) {
                    MiuiSizeCompatService.lambda$showConfirmDialog$2(dialogInterface);
                }
            };
            DialogInterface.OnDismissListener dismissListener = new DialogInterface.OnDismissListener() { // from class: com.android.server.wm.MiuiSizeCompatService$$ExternalSyntheticLambda4
                @Override // android.content.DialogInterface.OnDismissListener
                public final void onDismiss(DialogInterface dialogInterface) {
                    MiuiSizeCompatService.this.m1858x57d4b244(activity, dialogInterface);
                }
            };
            AlertDialog create = new AlertDialog.Builder(this.mUiContext, themeId).setCancelable(true).setTitle(title).setMessage(this.mUiContext.getResources().getString(286196480)).setCheckBox(true, this.mUiContext.getResources().getString(286196447)).setPositiveButton(this.mUiContext.getResources().getString(286196449), positiveListener).setNegativeButton(this.mUiContext.getResources().getString(286196448), negativeListener).setOnCancelListener(cancelListener).setOnDismissListener(dismissListener).create();
            this.mRestartAppDialog = create;
            WindowManager.LayoutParams attrs = create.getWindow().getAttributes();
            attrs.setTitle(title);
            attrs.type = 2003;
            attrs.flags |= 131072;
            attrs.gravity = 81;
            attrs.privateFlags |= 272;
            this.mRestartAppDialog.getWindow().setAttributes(attrs);
            this.mRestartAppDialog.show();
            checkSlow(start, 200L, "showConfirmDialog");
            Slog.d(TAG, "Show confirm dialog in size compat mode.");
            return;
        }
        Slog.d(TAG, "Show confirm dialog fail, activity is null.");
    }

    /* renamed from: lambda$showConfirmDialog$0$com-android-server-wm-MiuiSizeCompatService */
    public /* synthetic */ void m1857x87f26ac1(ActivityRecord activity, DialogInterface dialog, int which) {
        AlertDialog alertDialog = this.mRestartAppDialog;
        if (alertDialog != null && alertDialog.isChecked()) {
            H h = this.mBgHandler;
            h.sendMessage(h.obtainMessage(3, 0, this.mAtms.getCurrentUserId(), activity.packageName));
        }
        this.mCurFullAct = activity;
    }

    public static /* synthetic */ void lambda$showConfirmDialog$1(DialogInterface dialog, int which) {
    }

    public static /* synthetic */ void lambda$showConfirmDialog$2(DialogInterface dialog) {
    }

    /* renamed from: lambda$showConfirmDialog$3$com-android-server-wm-MiuiSizeCompatService */
    public /* synthetic */ void m1858x57d4b244(ActivityRecord activity, DialogInterface dialog) {
        ActivityRecord activityRecord = this.mCurFullAct;
        if (activityRecord != null && TextUtils.equals(activityRecord.packageName, activity.packageName)) {
            Message message = PooledLambda.obtainMessage(new MiuiSizeCompatService$$ExternalSyntheticLambda0(), this, activity, Float.valueOf((float) MiuiFreeformPinManagerService.EDGE_AREA));
            this.mFgHandler.sendMessage(message);
        }
    }

    public String getAppNameByPkg(String pkgName) {
        PackageManager pm = this.mContext.getPackageManager();
        try {
            ApplicationInfo info = pm.getApplicationInfo(pkgName, 1);
            return info.loadLabel(pm).toString();
        } catch (PackageManager.NameNotFoundException e) {
            Slog.d(TAG, pkgName + " may not be installed.");
            return pkgName;
        }
    }

    boolean isSizeCompatDisabled() {
        return Settings.Global.getInt(this.mContext.getContentResolver(), FORCE_RESIZABLE_ACTIVITIES, 0) != 0;
    }

    public float getLastAspectRatioValue(String pkgName) {
        AspectRatioInfo info = this.mSettingConfigs.get(pkgName);
        if (info != null) {
            return info.mLastAspectRatio;
        }
        return -1.0f;
    }

    public void initSettingsDirForUser(int userId) {
        String userSettingsDir = SYSTEM_USERS + userId;
        File systemDir = new File(Environment.getDataDirectory(), userSettingsDir);
        try {
            if (!systemDir.mkdirs()) {
                Slog.e(TAG, "Making dir failed");
            }
        } catch (SecurityException e) {
            Slog.e(TAG, "Exception throw while Making dir");
        }
        FileUtils.setPermissions(systemDir.toString(), 509, -1, -1);
        this.mSettingFilename = new File(systemDir, SETTING_CONFIG_FILE_NAME);
        this.mBackupSettingFilename = new File(systemDir, SETTING_CONFIG_BACKUP_FILE_NAME);
    }

    /* loaded from: classes.dex */
    public final class LocalService extends MiuiSizeCompatInternal {
        private LocalService() {
            MiuiSizeCompatService.this = r1;
        }

        @Override // com.android.server.wm.MiuiSizeCompatInternal
        public void onSystemReady(ActivityTaskManagerService atms) {
            try {
                MiuiSizeCompatService.this.onSystemReady(atms);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override // com.android.server.wm.MiuiSizeCompatInternal
        public Map<String, AspectRatioInfo> getMiuiSizeCompatEnabledApps() {
            return MiuiSizeCompatService.this.getMiuiSizeCompatEnabledApps();
        }

        @Override // com.android.server.wm.MiuiSizeCompatInternal
        public void showWarningNotification(ActivityRecord ar) {
            boolean z = false;
            boolean skip = ar == null || MiuiSizeCompatService.this.mCurFullAct == null || MiuiSizeCompatService.this.mNotificationShowing || !MiuiSizeCompatService.this.mServiceReady;
            if (!skip) {
                if (!TextUtils.equals(ar.packageName, MiuiSizeCompatService.this.mCurFullAct.packageName) && !MiuiSizeCompatService.this.PERMISSION_ACTIVITY.equals(ar.shortComponentName)) {
                    z = true;
                }
                skip = z;
            }
            if (skip) {
                if (MiuiSizeCompatService.this.mCurFullAct != null && ar != null && MiuiSizeCompatService.DEBUG) {
                    Slog.d(MiuiSizeCompatService.TAG, "Can not show notification for " + ar.shortComponentName + " ,mNotificationShowing= " + MiuiSizeCompatService.this.mNotificationShowing + " ,mCurFullAct= " + MiuiSizeCompatService.this.mCurFullAct.shortComponentName);
                    return;
                }
                return;
            }
            if (ar != MiuiSizeCompatService.this.mCurFullAct) {
                Slog.d(MiuiSizeCompatService.TAG, "ar=" + ar.shortComponentName + "\nmCurFullAct=" + MiuiSizeCompatService.this.mCurFullAct.shortComponentName);
            }
            AspectRatioInfo info = (AspectRatioInfo) MiuiSizeCompatService.this.mSettingConfigs.get(ar.packageName);
            if (info != null && info.mAspectRatio == MiuiFreeformPinManagerService.EDGE_AREA && info.mLastAspectRatio > MiuiFreeformPinManagerService.EDGE_AREA) {
                Message message = PooledLambda.obtainMessage(new BiConsumer() { // from class: com.android.server.wm.MiuiSizeCompatService$LocalService$$ExternalSyntheticLambda0
                    @Override // java.util.function.BiConsumer
                    public final void accept(Object obj, Object obj2) {
                        ((MiuiSizeCompatService) obj).showWarningNotification((ActivityRecord) obj2);
                    }
                }, MiuiSizeCompatService.this, ar);
                MiuiSizeCompatService.this.mFgHandler.sendMessage(message);
            } else if (info != null) {
                Slog.d(MiuiSizeCompatService.TAG, "Can not show notification, mAspectRatio=" + info.mAspectRatio + " mLastAspectRatio=" + info.mLastAspectRatio);
            }
        }

        @Override // com.android.server.wm.MiuiSizeCompatInternal
        public boolean isAppSizeCompatRestarting(String pkgName) {
            return !TextUtils.isEmpty(pkgName) && MiuiSizeCompatService.this.mCurFullAct != null && TextUtils.equals(MiuiSizeCompatService.this.mCurFullAct.packageName, pkgName);
        }

        @Override // com.android.server.wm.MiuiSizeCompatInternal
        public float getAspectRatioByPackage(String pkgName) {
            AspectRatioInfo info;
            if (MiuiSizeCompatService.this.mGameSettingConfig.containsKey(pkgName)) {
                info = (AspectRatioInfo) MiuiSizeCompatService.this.mGameSettingConfig.get(pkgName);
            } else if (MiuiSizeCompatService.this.mSettingConfigs.containsKey(pkgName)) {
                info = (AspectRatioInfo) MiuiSizeCompatService.this.mSettingConfigs.get(pkgName);
            } else {
                info = (AspectRatioInfo) MiuiSizeCompatService.this.mStaticSettingConfigs.get(pkgName);
            }
            if (info != null) {
                return info.mAspectRatio;
            }
            return -1.0f;
        }

        @Override // com.android.server.wm.MiuiSizeCompatInternal
        public Map<String, AspectRatioInfo> getMiuiGameSizeCompatEnabledApps() {
            return MiuiSizeCompatService.this.getMiuiGameSizeCompatEnabledApps();
        }

        @Override // com.android.server.wm.MiuiSizeCompatInternal
        public int getAspectGravityByPackage(String pkgName) {
            AspectRatioInfo info;
            if (MiuiSizeCompatService.this.mGameSettingConfig.containsKey(pkgName)) {
                info = (AspectRatioInfo) MiuiSizeCompatService.this.mGameSettingConfig.get(pkgName);
            } else if (MiuiSizeCompatService.this.mSettingConfigs.containsKey(pkgName)) {
                info = (AspectRatioInfo) MiuiSizeCompatService.this.mSettingConfigs.get(pkgName);
            } else {
                info = (AspectRatioInfo) MiuiSizeCompatService.this.mStaticSettingConfigs.get(pkgName);
            }
            if (info != null) {
                return info.mGravity;
            }
            return 17;
        }

        @Override // com.android.server.wm.MiuiSizeCompatInternal
        public boolean inMiuiGameSizeCompat(String pkgName) {
            return !TextUtils.isEmpty(pkgName) && MiuiSizeCompatService.this.mGameSettingConfig.get(pkgName) != null;
        }

        @Override // com.android.server.wm.MiuiSizeCompatInternal
        public boolean executeShellCommand(String command, String[] args, PrintWriter pw) {
            boolean z = false;
            if (MiuiSizeCompatService.SET_FIXED_ASPECT_RATIO_COMMAND.equals(command)) {
                if ((args.length != 1 && args.length != 2) || args[0] == null) {
                    pw.println(command + " options requires:");
                    printCommandHelp(pw, command, new String[]{"packageName"});
                    printCommandHelp(pw, command, new String[]{"packageName", "--remove"});
                    printCommandHelp(pw, command, new String[]{"packageName:packageName:...", "longSize:shortSize"});
                } else if (MiuiSizeCompatService.this.isSizeCompatDisabled()) {
                    Slog.w(MiuiSizeCompatService.TAG, "force resizable activities");
                    return true;
                } else {
                    String value = (args.length != 2 || args[1] == null) ? "" : args[1];
                    String[] pkgs = args[0].split(":");
                    boolean remove = value.equals("--remove") || value.equals("0");
                    int i = 0;
                    while (i < pkgs.length) {
                        boolean write = i == pkgs.length - 1;
                        if (remove) {
                            MiuiSizeCompatService.this.setMiuiSizeCompatRatio(pkgs[i], -1.0f, write, true);
                        } else {
                            MiuiSizeCompatService miuiSizeCompatService = MiuiSizeCompatService.this;
                            miuiSizeCompatService.setMiuiSizeCompatRatio(pkgs[i], miuiSizeCompatService.parseRatioFromStr(value), write, true);
                        }
                        i++;
                    }
                }
                return true;
            } else if (!MiuiSizeCompatService.SET_SIZE_COMPAT_DEBUG.equals(command)) {
                return false;
            } else {
                if (args != null && args.length == 1 && "true".equals(args[0])) {
                    z = true;
                }
                MiuiSizeCompatService.DEBUG = z;
                pw.println("Set size compat debug mode.");
                return true;
            }
        }

        private void printCommandHelp(PrintWriter pw, String cmd, String[] options) {
            StringBuilder stringBuilder = new StringBuilder();
            for (String option : options) {
                stringBuilder.append(" [");
                stringBuilder.append(option);
                stringBuilder.append("]");
            }
            pw.println(cmd + stringBuilder.toString());
        }

        @Override // com.android.server.wm.MiuiSizeCompatInternal
        public void dump(PrintWriter pw, String prefix) {
            String innerPrefix = prefix + "  ";
            if (!MiuiSizeCompatService.this.mStaticSettingConfigs.isEmpty()) {
                pw.println(prefix + "FixedAspectRatioPackages(System)");
                for (Map.Entry<String, AspectRatioInfo> entry : MiuiSizeCompatService.this.mStaticSettingConfigs.entrySet()) {
                    pw.println(innerPrefix + "[" + entry.getKey() + "] " + entry.getValue().mAspectRatio);
                }
            }
            pw.println(prefix + "FixedAspectRatioPackages(UserSetting)");
            if (!MiuiSizeCompatService.this.mSettingConfigs.isEmpty()) {
                for (Map.Entry<String, AspectRatioInfo> entry2 : MiuiSizeCompatService.this.mSettingConfigs.entrySet()) {
                    pw.println(innerPrefix + "[" + entry2.getKey() + "] " + entry2.getValue().mAspectRatio);
                }
            }
            pw.println(prefix + "GameAspectRatioPackages");
            if (!MiuiSizeCompatService.this.mGameSettingConfig.isEmpty()) {
                for (Map.Entry<String, AspectRatioInfo> entry3 : MiuiSizeCompatService.this.mGameSettingConfig.entrySet()) {
                    pw.println(innerPrefix + "[" + entry3.getKey() + "]" + entry3.getValue().mAspectRatio + "," + entry3.getValue().mGravity);
                }
            }
        }
    }

    public void showWarningNotification(ActivityRecord ar) {
        try {
            long start = SystemClock.uptimeMillis();
            this.mNotificationManager.createNotificationChannel(new NotificationChannel("WaringNotification", SERVICE_NAME, 4));
            Bitmap bitmap = getBitmap("com.android.settings");
            String content = this.mUiContext.getResources().getString(286196476);
            String title = this.mUiContext.getResources().getString(286196479);
            Notification notification = new Notification.Builder(this.mContext, "WaringNotification").setSmallIcon(17302859).setContentText(content).setContentTitle(title).addAction(makeSizeCompatAction(1)).addAction(makeSizeCompatAction(2)).setOngoing(false).setAutoCancel(true).build();
            if (bitmap != null) {
                notification.extras.putParcelable("miui.appIcon", Icon.createWithBitmap(bitmap));
            }
            this.mNotificationManager.notify(NOTIFICATION_ID, notification);
            this.mNotificationShowing = true;
            ProcessManager.registerForegroundInfoListener(this.mAppObserver);
            this.mAtms.mWindowManager.registerDisplayFoldListener(this.mFoldObserver);
            Slog.d(TAG, "Show warning notification in size compat mode.");
            checkSlow(start, 200L, "showWarningNotification");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Notification.Action makeSizeCompatAction(int type) {
        long start = SystemClock.uptimeMillis();
        Intent intent = new Intent();
        intent.setClassName("android", "android.sizecompat.SizeCompatChangeService");
        intent.putExtra("extra_type", type);
        PendingIntent pendingIntent = PendingIntent.getService(this.mContext, type, intent, BroadcastQueueImpl.FLAG_IMMUTABLE);
        int titleResId = type == 1 ? 286196477 : 286196478;
        Notification.Action action = new Notification.Action.Builder(Icon.createWithResource(this.mUiContext, 17302859), this.mUiContext.getResources().getString(titleResId), pendingIntent).build();
        checkSlow(start, 50L, "makeSizeCompatAction type: " + type);
        return action;
    }

    private void checkSlow(long startTime, long threshold, String where) {
        long took = SystemClock.uptimeMillis() - startTime;
        if (took > threshold) {
            Slog.w(TAG, "Slow operation: " + where + " took " + took + "ms.");
        }
    }

    private Bitmap getBitmap(String packageName) {
        try {
            PackageManager packageManager = this.mContext.getApplicationContext().getPackageManager();
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, 0);
            if (applicationInfo == null) {
                return null;
            }
            Drawable d = packageManager.getApplicationIcon(applicationInfo);
            BitmapDrawable bd = (BitmapDrawable) d;
            Bitmap bm = bd.getBitmap();
            return bm;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Package " + packageName + " not found, Ignoring.");
            return null;
        }
    }

    public void readSetting() {
        FileInputStream settingsFileStream = null;
        if (this.mBackupSettingFilename.exists()) {
            try {
                settingsFileStream = new FileInputStream(this.mBackupSettingFilename);
                if (this.mSettingFilename.exists()) {
                    Slog.v(TAG, "Cleaning up size_compat_setting_config.xml");
                    this.mSettingFilename.delete();
                }
            } catch (IOException e) {
                Slog.e(TAG, "size_compat_setting_config-backup.xml load config: ", e);
            }
        }
        if (settingsFileStream == null) {
            if (!this.mSettingFilename.exists()) {
                Slog.v(TAG, "size_compat_setting_config.xml not found");
                return;
            }
            try {
                settingsFileStream = new FileInputStream(this.mSettingFilename);
            } catch (IOException e2) {
                Slog.e(TAG, "size_compat_setting_config.xml load config: ", e2);
            }
        }
        if (settingsFileStream != null) {
            try {
                XmlPullParser xmlParser = Xml.newPullParser();
                xmlParser.setInput(settingsFileStream, null);
                for (int xmlEventType = xmlParser.next(); xmlEventType != 1; xmlEventType = xmlParser.next()) {
                    if (xmlEventType == 2 && "setting".equals(xmlParser.getName())) {
                        String packageName = xmlParser.getAttributeValue(null, "name");
                        String applicationName = xmlParser.getAttributeValue(null, XML_ATTRIBUTE_DISPLAY_NAME);
                        String aspectRatio = xmlParser.getAttributeValue(null, XML_ATTRIBUTE_ASPECT_RATIO);
                        String gravity = xmlParser.getAttributeValue(null, XML_ATTRIBUTE_GRAVITY);
                        this.mSettingConfigs.put(packageName, new AspectRatioInfo(packageName, applicationName, Float.parseFloat(aspectRatio), Integer.parseInt(gravity)));
                    }
                }
            } catch (FileNotFoundException e3) {
                Slog.e(TAG, "size_compat_setting_config.xmlload config: ", e3);
            } catch (IOException e4) {
                Slog.e(TAG, "size_compat_setting_config.xmlload config: ", e4);
            } catch (XmlPullParserException e5) {
                Slog.e(TAG, "size_compat_setting_config.xmlload config: ", e5);
            } catch (Throwable e6) {
                Slog.e(TAG, "size_compat_setting_config.xmlload config: ", e6);
            }
        }
        Slog.d(TAG, "Setting configs: " + this.mSettingConfigs.size());
        if (settingsFileStream != null) {
            try {
                settingsFileStream.close();
            } catch (IOException e7) {
                Slog.e(TAG, "size_compat_setting_config.xmlload config:IO Exception while closing stream", e7);
            }
        }
    }

    public void writeSetting() {
        BufferedOutputStream settingsBufferedOutputStream = null;
        FileOutputStream settingsFileStream = null;
        long startTime = SystemClock.uptimeMillis();
        if (this.mSettingFilename.exists()) {
            if (this.mBackupSettingFilename.exists()) {
                this.mSettingFilename.delete();
                Slog.v(TAG, "size_compat_setting_config.xml delete old file");
            } else if (!this.mSettingFilename.renameTo(this.mBackupSettingFilename)) {
                Slog.e(TAG, "Unable to backup size_compat_setting_config, current changes will be lost at reboot");
                return;
            }
        }
        try {
            try {
                settingsFileStream = new FileOutputStream(this.mSettingFilename);
                settingsBufferedOutputStream = new BufferedOutputStream(settingsFileStream);
                FastXmlSerializer serializer = new FastXmlSerializer();
                serializer.setOutput(settingsBufferedOutputStream, StandardCharsets.UTF_8.name());
                serializer.startDocument((String) null, true);
                serializer.setFeature(FAST_XML, true);
                serializer.startTag((String) null, XML_ATTRIBUTE_SETTING_CONDIG);
                for (String pkg : this.mSettingConfigs.keySet()) {
                    AspectRatioInfo info = this.mSettingConfigs.get(pkg);
                    if (info == null) {
                        Slog.e(TAG, "Info is null when write settings.");
                    } else {
                        serializer.startTag((String) null, "setting");
                        serializer.attribute((String) null, "name", pkg);
                        String applicationName = info.mApplicationName == null ? "" : info.mApplicationName;
                        serializer.attribute((String) null, XML_ATTRIBUTE_DISPLAY_NAME, applicationName);
                        serializer.attribute((String) null, XML_ATTRIBUTE_ASPECT_RATIO, String.valueOf(info.mAspectRatio));
                        serializer.attribute((String) null, XML_ATTRIBUTE_GRAVITY, String.valueOf(info.mGravity));
                        serializer.endTag((String) null, "setting");
                    }
                }
                serializer.endTag((String) null, XML_ATTRIBUTE_SETTING_CONDIG);
                serializer.endDocument();
                settingsBufferedOutputStream.flush();
                FileUtils.sync(settingsFileStream);
                this.mBackupSettingFilename.delete();
                FileUtils.setPermissions(this.mSettingFilename.toString(), 432, -1, -1);
                checkSlow(startTime, 200L, "write size_compat_setting_config.xml");
            } catch (IOException e) {
                if (this.mSettingFilename.exists() && !this.mSettingFilename.delete()) {
                    Slog.w(TAG, "Failed to clean up mangled file: " + this.mSettingFilename);
                }
                Slog.e(TAG, "Unable to write host recognize settings,current changes will be lost at reboot", e);
            }
        } finally {
            IoUtils.closeQuietly(settingsFileStream);
            IoUtils.closeQuietly(settingsBufferedOutputStream);
        }
    }
}
