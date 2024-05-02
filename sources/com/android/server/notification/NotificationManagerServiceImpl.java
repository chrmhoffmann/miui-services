package com.android.server.notification;

import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.MiuiNotification;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManagerInternal;
import android.content.pm.ParceledListSlice;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Process;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.VibrationEffect;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Pair;
import android.util.Slog;
import android.util.TypedXmlPullParser;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.ArrayUtils;
import com.android.server.LocalServices;
import com.android.server.lights.MiuiLightsService;
import com.android.server.notification.NotificationManagerService;
import com.android.server.notification.NotificationRecord;
import com.android.server.notification.NotificationUsageStats;
import com.android.server.pm.MiuiDefaultPermissionGrantPolicy;
import com.android.server.wm.ActivityTaskSupervisorImpl;
import com.miui.base.MiuiStubRegistry;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import miui.content.pm.PreloadedAppPolicy;
import miui.os.Build;
import miui.security.ISecurityManager;
import miui.util.QuietUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
/* loaded from: classes.dex */
public class NotificationManagerServiceImpl implements NotificationManagerServiceStub {
    private static final String BREATHING_LIGHT = "breathing_light";
    protected static final String ENABLED_SERVICES_SEPARATOR = ":";
    private static final int INVALID_UID = -1;
    private static final List<String> MIUI_SYSTEM_APPS_LIST = Arrays.asList(MiuiDefaultPermissionGrantPolicy.MIUI_SYSTEM_APPS);
    private static final List<String> OTHER_APPS_LIST;
    private static final String SPLIT_CHAR = "\\|";
    private static final int SYSTEM_APP_MASK = 129;
    public static final String TAG = "NotificationManagerServiceImpl";
    private static final String XMSF_CHANNEL_ID_PREFIX = "mipush|";
    private static final String XMSF_FAKE_CONDITION_PROVIDER_PATH = "xmsf_fake_condition_provider_path";
    protected static final String XMSF_PACKAGE_NAME = "com.xiaomi.xmsf";
    private static final Set<String> sAllowToastSet;
    private String[] allowNotificationAccessList;
    private String[] interceptChannelId;
    private String[] interceptListener;
    private Context mContext;
    private final String PRIVACY_INPUT_MODE_PKG_NAME = "miui_privacy_input_pkg_name";
    private String mPrivacyInputModePkgName = null;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<NotificationManagerServiceImpl> {

        /* compiled from: NotificationManagerServiceImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final NotificationManagerServiceImpl INSTANCE = new NotificationManagerServiceImpl();
        }

        public NotificationManagerServiceImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public NotificationManagerServiceImpl provideNewInstance() {
            return new NotificationManagerServiceImpl();
        }
    }

    public void init(Context context) {
        this.mContext = context;
        this.interceptListener = context.getResources().getStringArray(285409320);
        this.interceptChannelId = this.mContext.getResources().getStringArray(285409319);
        this.allowNotificationAccessList = this.mContext.getResources().getStringArray(285409321);
        Slog.i(TAG, " allowNotificationListener :" + Arrays.toString(this.allowNotificationAccessList));
        registerPrivacyInputMode(BackgroundThread.getHandler());
    }

    public boolean isDeniedPlaySound(AudioManager audioManager, NotificationRecord record) {
        return !isAudioCanPlay(audioManager) || isSilenceMode(record) || !record.isStatusBarAllowSound();
    }

    public boolean isSupportSilenceMode() {
        return MiuiSettings.SilenceMode.isSupported;
    }

    private boolean isAudioCanPlay(AudioManager audioManager) {
        return (audioManager.getRingerModeInternal() == 1 || audioManager.getRingerModeInternal() == 0) ? false : true;
    }

    private boolean isSilenceMode(NotificationRecord record) {
        MiuiNotification extraNotification = record.getSbn().getNotification().extraNotification;
        return QuietUtils.checkQuiet(5, 0, record.getSbn().getPackageName(), extraNotification == null ? null : extraNotification.getTargetPkg());
    }

    public boolean isDeniedPlayVibration(AudioManager audioManager, NotificationRecord record, NotificationUsageStats.AggregatedStats mStats) {
        boolean exRingerModeSilent = audioManager.getRingerMode() == 0;
        return exRingerModeSilent || !record.isStatusBarAllowVibration() || shouldSkipFrequentlyVib(record, mStats);
    }

    public VibrationEffect adjustVibration(VibrationEffect vibration) {
        if (vibration != null && (vibration instanceof VibrationEffect.Composed)) {
            VibrationEffect.Composed currentVibration = (VibrationEffect.Composed) vibration;
            if (currentVibration.getSegments() != null && currentVibration.getSegments().size() > 4) {
                return new VibrationEffect.Composed(currentVibration.getSegments().subList(0, 4), currentVibration.getRepeatIndex());
            }
            return vibration;
        }
        return vibration;
    }

    public boolean isDeniedLed(NotificationRecord record) {
        return !record.isStatusBarAllowLed();
    }

    public void calculateAudiblyAlerted(AudioManager audioManager, NotificationRecord record) {
        boolean buzz = false;
        boolean beep = false;
        boolean z = true;
        boolean aboveThreshold = record.getImportance() >= 3;
        Uri soundUri = record.getSound();
        VibrationEffect vibration = record.getVibration();
        boolean hasValidVibrate = vibration != null;
        if (aboveThreshold && audioManager != null) {
            boolean hasValidSound = soundUri != null && !Uri.EMPTY.equals(soundUri);
            if (hasValidSound) {
                beep = true;
            }
            boolean ringerModeSilent = audioManager.getRingerModeInternal() == 0;
            if (hasValidVibrate && !ringerModeSilent) {
                buzz = true;
            }
        }
        if (!buzz && !beep) {
            z = false;
        }
        record.setAudiblyAlerted(z);
    }

    public void checkFullScreenIntent(Notification notification, AppOpsManager appOpsManager, int uid, String packageName) {
        if (!Build.IS_INTERNATIONAL_BUILD && notification.fullScreenIntent != null) {
            int mode = appOpsManager.noteOpNoThrow(10021, uid, packageName, (String) null, "NotificationManagementImpl#checkFullScreenIntent");
            if (mode != 0) {
                Slog.i(TAG, "MIUILOG- Permission Denied Activity : " + notification.fullScreenIntent + " pkg : " + packageName + " uid : " + uid);
                notification.fullScreenIntent = null;
            }
        }
    }

    static {
        HashSet hashSet = new HashSet();
        sAllowToastSet = hashSet;
        hashSet.add("com.lbe.security.miui");
        ArrayList arrayList = new ArrayList();
        OTHER_APPS_LIST = arrayList;
        arrayList.add("com.xiaomi.wearable");
        arrayList.add("com.xiaomi.hm.health");
        arrayList.add("com.mi.health");
    }

    public boolean isAllowAppRenderedToast(String pkg, int callingUid, int userId) {
        if (sAllowToastSet.contains(pkg)) {
            PackageManagerInternal pm = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
            try {
                PackageInfo info = pm.getPackageInfo(pkg, 0L, Process.myUid(), userId);
                if (!info.applicationInfo.isSystemApp()) {
                    return false;
                }
                return callingUid == info.applicationInfo.uid;
            } catch (Exception e) {
                Slog.e(TAG, "can't find package!", e);
            }
        }
        return false;
    }

    public boolean isPackageDistractionRestrictionLocked(NotificationRecord r) {
        String pkg = r.getSbn().getPackageName();
        int callingUid = r.getSbn().getUid();
        return isPackageDistractionRestrictionForUser(pkg, callingUid);
    }

    private boolean isPackageDistractionRestrictionForUser(String pkg, int uid) {
        int userId = UserHandle.getUserId(uid);
        try {
            PackageManagerInternal pmi = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
            return (pmi.getDistractingPackageRestrictions(pkg, userId) & 2) != 0;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public void checkAllowToCreateChannels(String pkg, ParceledListSlice channelsList) {
        if (!isCallerXmsfOrSystem(pkg)) {
            List<NotificationChannel> channels = channelsList.getList();
            for (int i = 0; i < channels.size(); i++) {
                NotificationChannel channel = channels.get(i);
                if (channel != null && isXmsfChannelId(channel.getId())) {
                    throw new SecurityException("Pkg " + pkg + " cannot create channels, because " + channel.getId() + " starts with 'mipush|'");
                }
            }
        }
    }

    private boolean isCallerXmsfOrSystem(String callingPkg) {
        return isXmsf(callingPkg) || isCallerSystem();
    }

    private boolean isXmsf(String pkg) {
        return XMSF_PACKAGE_NAME.equals(pkg);
    }

    private boolean isCallerSystem() {
        int appId = UserHandle.getAppId(Binder.getCallingUid());
        return appId == 1000;
    }

    private boolean isUidSystemOrPhone(int uid) {
        int appid = UserHandle.getAppId(uid);
        return appid == 1000 || appid == 1001 || uid == 0;
    }

    private boolean isXmsfChannelId(String channelId) {
        return !TextUtils.isEmpty(channelId) && channelId.startsWith(XMSF_CHANNEL_ID_PREFIX);
    }

    public boolean checkIsXmsfFakeConditionProviderEnabled(String path) {
        return XMSF_FAKE_CONDITION_PROVIDER_PATH.equals(path) && checkCallerIsXmsf();
    }

    private boolean checkCallerIsXmsf(String callingPkg, String targetPkg, int callingUid, int userId) {
        if (!isXmsf(callingPkg) || isXmsf(targetPkg)) {
            return false;
        }
        return isCallerSystem() || checkCallerIsXmsfInternal(callingUid, userId);
    }

    private boolean checkCallerIsXmsfInternal(int callingUid, int userId) {
        try {
            ApplicationInfo ai = ((PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class)).getApplicationInfo(XMSF_PACKAGE_NAME, 0L, callingUid, userId);
            if (ai == null || !UserHandle.isSameApp(ai.uid, callingUid)) {
                return false;
            }
            if ((ai.flags & 129) != 0) {
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isDelegateAllowed(String callingPkg, int callingUid, String targetPkg, int userId) {
        return isCallerAndroid(callingPkg, callingUid) || isCallerSecurityCenter(callingPkg, callingUid) || checkCallerIsXmsf(callingPkg, targetPkg, callingUid, userId);
    }

    private boolean isCallerAndroid(String callingPkg, int callingUid) {
        return isUidSystemOrPhone(callingUid) && callingPkg != null && "android".equals(callingPkg);
    }

    private boolean isCallerSecurityCenter(String callingPkg, int uid) {
        int appid = UserHandle.getAppId(uid);
        return appid == 1000 && ActivityTaskSupervisorImpl.MIUI_APP_LOCK_PACKAGE_NAME.equals(callingPkg);
    }

    public boolean isDeniedLocalNotification(AppOpsManager appops, Notification notification, int callingUid, String callingPkg) {
        if (XMSF_PACKAGE_NAME.equals(callingPkg) || (notification.flags & 64) != 0) {
            return false;
        }
        int mode = appops.noteOpNoThrow(10033, callingUid, callingPkg);
        if (mode == 0) {
            return false;
        }
        Slog.i(TAG, "MIUILOG- Permission Denied to post local notification for " + callingPkg);
        return true;
    }

    public String getPlayVibrationPkg(String pkg, String opPkg, String defaultPkg) {
        if (isXmsf(opPkg) && !isXmsf(pkg)) {
            return pkg;
        }
        return defaultPkg;
    }

    public boolean checkCallerIsXmsf() {
        return checkCallerIsXmsfInternal(Binder.getCallingUid(), UserHandle.getCallingUserId());
    }

    public boolean shouldInjectDeleteChannel(String pkg, String channelId, String groupId) {
        if (isXmsf(pkg) && (isXmsfChannelId(channelId) || isXmsfChannelId(groupId))) {
            return true;
        }
        if (!TextUtils.isEmpty(channelId)) {
            checkAllowToDeleteChannel(pkg, channelId);
            return false;
        }
        return false;
    }

    public String getAppPkgByChannel(String channelIdOrGroupId, String defaultPkg) {
        if (!TextUtils.isEmpty(channelIdOrGroupId)) {
            String[] array = channelIdOrGroupId.split(SPLIT_CHAR);
            if (array.length >= 2) {
                return array[1];
            }
        }
        return defaultPkg;
    }

    public int getAppUidByPkg(String pkg, int defaultUid) {
        int appUid = getUidByPkg(pkg, UserHandle.getCallingUserId());
        if (appUid != -1) {
            return appUid;
        }
        return defaultUid;
    }

    private void checkAllowToDeleteChannel(String pkg, String channelId) {
        if (!isCallerXmsfOrSystem(pkg) && isXmsfChannelId(channelId)) {
            throw new SecurityException("Pkg " + pkg + " cannot delete channel " + channelId + " that starts with 'mipush|'");
        }
    }

    private int getUidByPkg(String pkg, int userId) {
        try {
            if (TextUtils.isEmpty(pkg)) {
                return -1;
            }
            int uid = ((PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class)).getPackageUid(pkg, 0L, userId);
            return uid;
        } catch (Exception e) {
            return -1;
        }
    }

    public boolean isDeniedDemoPostNotification(int userId, int uid, Notification notification, String pkg) {
        if (notification == null || !Build.IS_DEMO_BUILD || notification.isForegroundService() || hasProgress(notification) || notification.hasMediaSession() || isCallingSystem(userId, uid, pkg)) {
            return false;
        }
        Slog.i(TAG, "Denied demo product third-party app :" + pkg + "to post notification :" + notification.toString());
        return true;
    }

    private boolean hasProgress(Notification n) {
        return n.extras.containsKey("android.progress") && n.extras.containsKey("android.progressMax") && n.extras.getInt("android.progressMax") != 0 && n.extras.getInt("android.progress") != n.extras.getInt("android.progressMax");
    }

    private boolean isCallingSystem(int userId, int uid, String callingPkg) {
        int appid = UserHandle.getAppId(uid);
        if (appid == 1000 || appid == 1001 || uid == 0 || uid == 2000) {
            return true;
        }
        try {
            ApplicationInfo ai = AppGlobals.getPackageManager().getApplicationInfo(callingPkg, 0L, userId);
            if (ai == null) {
                Slog.d(TAG, "Unknown package " + callingPkg);
                return false;
            }
            return ai.isSystemApp();
        } catch (Exception e) {
            Slog.e(TAG, "isCallingSystem error: " + e);
            return false;
        }
    }

    public Object getVibRateLimiter() {
        return new VibRateLimiter();
    }

    private boolean shouldSkipFrequentlyVib(NotificationRecord record, NotificationUsageStats.AggregatedStats mStats) {
        if (((VibRateLimiter) mStats.vibRate).shouldRateLimitVib(SystemClock.elapsedRealtime())) {
            mStats.numVibViolations++;
            Slog.e(TAG, "Cancel the recent frequently vibration in 15s " + record.getKey());
            return true;
        }
        return false;
    }

    public boolean checkInterceptListener(StatusBarNotification sbn, ComponentName component) {
        return checkInterceptListenerForXiaomiInternal(sbn, component) || checkInterceptListenerForMIUIInternal(sbn, component.getPackageName());
    }

    private boolean checkInterceptListenerForXiaomiInternal(StatusBarNotification sbn, ComponentName component) {
        if (!ArrayUtils.contains(this.interceptListener, component.flattenToString()) || !ArrayUtils.contains(this.interceptChannelId, sbn.getPackageName() + ENABLED_SERVICES_SEPARATOR + sbn.getNotification().getChannelId())) {
            return false;
        }
        Slog.w(TAG, "checkInterceptListenerForXiaomiInternal pkg:" + sbn.getPackageName() + "listener:" + component.flattenToString());
        return true;
    }

    private boolean checkInterceptListenerForMIUIInternal(StatusBarNotification sbn, String listener) {
        boolean z = false;
        if (!checkAppSystemOrNot(listener) && !MIUI_SYSTEM_APPS_LIST.contains(listener) && !OTHER_APPS_LIST.contains(listener) && !PreloadedAppPolicy.isProtectedDataApp((Context) null, listener, 0) && checkNotificationMasked(sbn.getPackageName(), sbn.getUid())) {
            z = true;
        }
        boolean intercept = z;
        if (intercept) {
            Slog.w(TAG, "checkInterceptListenerForMIUIInternal pkg:" + sbn.getPackageName());
        }
        return intercept;
    }

    private boolean checkNotificationMasked(String pkg, int uid) {
        boolean z;
        IBinder b = ServiceManager.getService("security");
        if (b == null) {
            return false;
        }
        try {
            int userId = UserHandle.getUserId(uid);
            ISecurityManager service = ISecurityManager.Stub.asInterface(b);
            if (service.haveAccessControlPassword(userId) && service.getApplicationAccessControlEnabledAsUser(pkg, userId)) {
                if (service.getApplicationMaskNotificationEnabledAsUser(pkg, userId)) {
                    z = true;
                    boolean result = z;
                    return result;
                }
            }
            z = false;
            boolean result2 = z;
            return result2;
        } catch (Exception e) {
            Slog.e(TAG, "check notification masked error: ", e);
            return false;
        }
    }

    private boolean checkAppSystemOrNot(String monitorPkg) {
        try {
            ApplicationInfo ai = ((PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class)).getApplicationInfo(monitorPkg, 0L, 1000, 0);
            if (ai == null) {
                return false;
            }
            boolean isSystem = ai.isSystemApp();
            return isSystem;
        } catch (Exception e) {
            Slog.e(TAG, "checkAppSystemOrNot : ", e);
            return false;
        }
    }

    public IBinder getColorLightManager() {
        if (MiuiLightsService.getInstance() != null) {
            return MiuiLightsService.getInstance().getBinderService();
        }
        return null;
    }

    public void dumpLight(PrintWriter pw, NotificationManagerService.DumpFilter filter) {
        if (MiuiLightsService.getInstance() != null) {
            MiuiLightsService.getInstance().dumpLight(pw, filter);
        }
    }

    public boolean enableBlockedToasts(String pkg) {
        if (this.mPrivacyInputModePkgName == null) {
            String packageName = Settings.Secure.getString(this.mContext.getContentResolver(), "miui_privacy_input_pkg_name");
            this.mPrivacyInputModePkgName = packageName == null ? "" : packageName;
        }
        return !pkg.equals(this.mPrivacyInputModePkgName);
    }

    private void registerPrivacyInputMode(Handler handler) {
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("miui_privacy_input_pkg_name"), false, new ContentObserver(handler) { // from class: com.android.server.notification.NotificationManagerServiceImpl.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri) {
                super.onChange(selfChange, uri);
                String pkg = Settings.Secure.getString(NotificationManagerServiceImpl.this.mContext.getContentResolver(), "miui_privacy_input_pkg_name");
                NotificationManagerServiceImpl.this.mPrivacyInputModePkgName = pkg == null ? "" : pkg;
                Slog.d(NotificationManagerServiceImpl.TAG, "onChange:" + pkg);
            }
        });
    }

    public Pair<Integer, String> getMiuiDefaultListerWithVersion() {
        String defaultListenerAccess = this.mContext.getResources().getString(286195872);
        if (defaultListenerAccess != null) {
            String[] listenersWithVersion = defaultListenerAccess.split(",");
            Slog.d(TAG, "listenersWithVersion " + Arrays.toString(listenersWithVersion));
            if (listenersWithVersion != null) {
                int version = -1;
                String listeners = null;
                if (listenersWithVersion.length == 2) {
                    listeners = listenersWithVersion[0];
                    version = Integer.valueOf(listenersWithVersion[1]).intValue();
                } else if (listenersWithVersion.length == 1) {
                    listeners = listenersWithVersion[0];
                    version = 0;
                }
                return new Pair<>(Integer.valueOf(version), listeners);
            }
        }
        return new Pair<>(-1, null);
    }

    public void readDefaultsAndFixMiuiVersion(ManagedServices services, String defaultComponents, TypedXmlPullParser parser) {
        ComponentName cn;
        boolean isEnableListener = parser.getName().equals("enabled_listeners");
        if (!TextUtils.isEmpty(defaultComponents)) {
            boolean hasBarrage = false;
            String[] components = defaultComponents.split(ENABLED_SERVICES_SEPARATOR);
            for (int i = 0; i < components.length; i++) {
                if (!TextUtils.isEmpty(components[i]) && (cn = ComponentName.unflattenFromString(components[i])) != null && cn.getPackageName().equals("com.xiaomi.barrage") && isEnableListener) {
                    hasBarrage = true;
                }
            }
            if (!hasBarrage && services.getMiuiVersion() != -1) {
                services.setMiuiVersion(-1);
            }
        } else if (isEnableListener && services.getMiuiVersion() != -1) {
            services.setMiuiVersion(-1);
        }
    }

    public NotificationRecord.Light customizeNotificationLight(int color, int onMs, int offMs, Context context) {
        String jsonText = Settings.Secure.getStringForUser(context.getContentResolver(), BREATHING_LIGHT, -2);
        if (TextUtils.isEmpty(jsonText)) {
            return new NotificationRecord.Light(color, onMs, offMs);
        }
        int customizeColor = color;
        int customizeOnMs = onMs;
        try {
            JSONArray jsonArray = new JSONArray(jsonText);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject logObject = jsonArray.getJSONObject(i);
                if (logObject.getInt("light") == 4) {
                    customizeColor = logObject.getInt("color");
                    customizeOnMs = logObject.getInt("onMS");
                }
            }
        } catch (JSONException e) {
            Slog.i(TAG, "Light jsonArray error", e);
        }
        return new NotificationRecord.Light(customizeColor, customizeOnMs, offMs);
    }

    public void fixCheckInterceptListenerAutoGroup(Notification notification, String pkg) {
        try {
            if (notification.getChannelId() != null && ArrayUtils.contains(this.interceptChannelId, pkg + ENABLED_SERVICES_SEPARATOR + notification.getChannelId())) {
                Class notificationClazz = notification.getClass();
                Field mGroupKeyField = notificationClazz.getDeclaredField("mGroupKey");
                mGroupKeyField.setAccessible(true);
                mGroupKeyField.set(notification, notification.getChannelId() + "_autogroup");
            }
        } catch (Exception e) {
            Slog.i(TAG, " fixCheckInterceptListenerGroup exception:", e);
        }
    }

    public void isAllowAppNotificationListener(boolean isPackageAdded, NotificationManagerService.NotificationListeners mListeners, int userId, String pkg) {
        String[] strArr;
        if (isPackageAdded && (strArr = this.allowNotificationAccessList) != null && ArrayUtils.contains(strArr, pkg)) {
            NotificationManager nm = (NotificationManager) this.mContext.getSystemService(NotificationManager.class);
            ArraySet<ComponentName> listeners = mListeners.queryPackageForServices(pkg, 786432, 0);
            for (int k = 0; k < listeners.size(); k++) {
                ComponentName cn = listeners.valueAt(k);
                try {
                    nm.setNotificationListenerAccessGrantedForUser(cn, userId, true);
                } catch (Exception e) {
                    Slog.i(TAG, " isAllowAppNotificationListener exception:", e);
                }
            }
        }
    }
}
