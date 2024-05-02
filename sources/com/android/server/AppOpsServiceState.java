package com.android.server;

import android.app.ActivityManagerInternal;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManagerInternal;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.MiuiBinderProxy;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Slog;
import android.util.SparseArray;
import com.android.server.am.ProcessUtils;
import com.android.server.biometrics.sensors.face.MiuiFaceHidl;
import com.android.server.wifi.ArpPacket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import miui.os.Build;
import miui.security.SecurityManager;
import miui.util.FeatureParser;
/* loaded from: classes.dex */
public class AppOpsServiceState {
    public static final boolean DEBUG = false;
    public static final int FLAG_NOT_RECORD = 32;
    private static final String MESSAGE_NO_ASK = "noteNoAsk";
    private static final String MESSAGE_NO_ASK_NO_RECORD = "NoAskNoRecord";
    public static final int OPERATION_TYPE_AI_ALLOW = 5;
    public static final int OPERATION_TYPE_FINISH = 3;
    public static final int OPERATION_TYPE_NOTE = 1;
    public static final int OPERATION_TYPE_START = 2;
    public static final int OPERATION_TYPE_USER_ALLOW = 6;
    public static final int OPERATION_TYPE_VIRTUAL = 4;
    private static final String POWER_SAVE_MODE_OPEN = "POWER_SAVE_MODE_OPEN";
    private static final String TAG = "AppOpsServiceState";
    private static final String WIFI_MESSAGE_STARTWITH = "WifiPermissionsUtil#noteAppOpAllowed";
    static final HashMap<Integer, String> sOpInControl;
    private static Map<Integer, Integer> supportVirtualGrant;
    private ActivityManagerInternal mActivityManagerInternal;
    private Context mContext;
    private boolean mPowerSaving;
    private SecurityManager mSecurityManager;
    private static Set<String> sCtsIgnore = new HashSet();
    private static Set<String> MESSAGE_NOT_RECORD = new HashSet();
    private int mDefaultMode = 1;
    final SparseArray<UserState> mUidStates = new SparseArray<>();

    static {
        HashMap<Integer, String> hashMap = new HashMap<>();
        sOpInControl = hashMap;
        hashMap.put(20, "android.permission.SEND_SMS");
        hashMap.put(15, "android.permission.WRITE_SMS");
        hashMap.put(22, "android.permission.WRITE_SMS");
        hashMap.put(10010, "android.permission.WRITE_SMS");
        hashMap.put(14, "android.permission.READ_SMS");
        hashMap.put(16, "android.permission.RECEIVE_SMS");
        hashMap.put(17, "android.permission.RECEIVE_SMS");
        hashMap.put(21, "android.permission.READ_SMS");
        hashMap.put(10004, "android.permission.INTERNET");
        hashMap.put(10011, "android.permission.WRITE_SMS");
        hashMap.put(10006, "android.permission.WRITE_SMS");
        hashMap.put(18, "android.permission.RECEIVE_MMS");
        hashMap.put(19, "android.permission.RECEIVE_MMS");
        hashMap.put(13, "android.permission.CALL_PHONE");
        hashMap.put(54, "android.permission.PROCESS_OUTGOING_CALLS");
        hashMap.put(5, "android.permission.WRITE_CONTACTS");
        hashMap.put(4, "android.permission.READ_CONTACTS");
        hashMap.put(7, "android.permission.WRITE_CALL_LOG");
        hashMap.put(10013, "android.permission.WRITE_CALL_LOG");
        hashMap.put(6, "android.permission.READ_CALL_LOG");
        hashMap.put(0, "android.permission.ACCESS_COARSE_LOCATION");
        hashMap.put(1, "android.permission.ACCESS_FINE_LOCATION");
        hashMap.put(2, "android.permission.ACCESS_FINE_LOCATION");
        hashMap.put(10, "android.permission.ACCESS_COARSE_LOCATION");
        hashMap.put(12, "android.permission.ACCESS_COARSE_LOCATION");
        hashMap.put(41, "android.permission.ACCESS_FINE_LOCATION");
        hashMap.put(42, "android.permission.ACCESS_FINE_LOCATION");
        hashMap.put(51, "android.permission.READ_PHONE_STATE");
        hashMap.put(8, "android.permission.READ_CALENDAR");
        hashMap.put(9, "android.permission.WRITE_CALENDAR");
        hashMap.put(10015, "ACCESS_XIAOMI_ACCOUNT");
        hashMap.put(62, "android.permission.GET_ACCOUNTS");
        hashMap.put(52, "com.android.voicemail.permission.ADD_VOICEMAIL");
        hashMap.put(53, "android.permission.USE_SIP");
        hashMap.put(26, "android.permission.CAMERA");
        hashMap.put(27, "android.permission.RECORD_AUDIO");
        hashMap.put(59, "android.permission.READ_EXTERNAL_STORAGE");
        hashMap.put(60, "android.permission.WRITE_EXTERNAL_STORAGE");
        hashMap.put(23, "android.permission.WRITE_SETTINGS");
        hashMap.put(10003, "android.permission.CHANGE_NETWORK_STATE");
        hashMap.put(Integer.valueOf((int) MiuiFaceHidl.MSG_GET_IMG_FRAME), "android.permission.CHANGE_WIFI_STATE");
        hashMap.put(10002, "android.permission.BLUETOOTH_ADMIN");
        hashMap.put(10016, "android.permission.NFC");
        hashMap.put(10017, "com.android.launcher.permission.INSTALL_SHORTCUT");
        hashMap.put(24, "android.permission.SYSTEM_ALERT_WINDOW");
        hashMap.put(56, "android.permission.BODY_SENSORS");
        hashMap.put(10008, "AUTO_START");
        sCtsIgnore.add("android.app.usage.cts");
        sCtsIgnore.add("com.android.cts.usepermission");
        sCtsIgnore.add("com.android.cts.permission");
        sCtsIgnore.add("com.android.cts.netlegacy22.permission");
        sCtsIgnore.add("android.netlegacy22.permission.cts");
        sCtsIgnore.add("android.provider.cts");
        sCtsIgnore.add("android.telephony2.cts");
        sCtsIgnore.add("android.permission.cts");
        sCtsIgnore.add("com.android.cts.writeexternalstorageapp");
        sCtsIgnore.add("com.android.cts.readexternalstorageapp");
        sCtsIgnore.add("com.android.cts.externalstorageapp");
        sCtsIgnore.add("android.server.alertwindowapp");
        sCtsIgnore.add("android.server.alertwindowappsdk25");
        sCtsIgnore.add("com.android.app2");
        sCtsIgnore.add("com.android.cts.appbinding.app");
        sCtsIgnore.add("com.android.cts.launcherapps.simplepremapp");
        MESSAGE_NOT_RECORD.add("AutoStartManagerService#signalStopProcessesLocked");
        MESSAGE_NOT_RECORD.add("ContentProvider#enforceReadPermission");
        MESSAGE_NOT_RECORD.add("ContentProvider#enforceWritePermission");
        MESSAGE_NOT_RECORD.add("AppOpsHelper#checkLocationAccess");
        MESSAGE_NOT_RECORD.add(WIFI_MESSAGE_STARTWITH);
        HashMap hashMap2 = new HashMap();
        supportVirtualGrant = hashMap2;
        hashMap2.put(14, 10028);
        supportVirtualGrant.put(4, 10029);
        supportVirtualGrant.put(8, 10030);
        supportVirtualGrant.put(6, 10031);
        supportVirtualGrant.put(51, 10032);
    }

    /* loaded from: classes.dex */
    public static final class UserState {
        Callback mCallback;
        MiuiBinderProxy mCallbackBinder;

        private UserState() {
        }
    }

    public void init(Context context) {
        this.mContext = context;
        this.mActivityManagerInternal = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
        if (FeatureParser.getBoolean("is_pad", false)) {
            this.mDefaultMode = 0;
        }
    }

    private synchronized UserState getUidState(int userHandle, boolean edit) {
        UserState userState = this.mUidStates.get(userHandle);
        if (userState == null) {
            if (!edit) {
                return null;
            }
            userState = new UserState();
            this.mUidStates.put(userHandle, userState);
        }
        return userState;
    }

    public synchronized void removeUser(int userHandle) {
        this.mUidStates.remove(userHandle);
    }

    public void systemReady() {
        this.mSecurityManager = (SecurityManager) this.mContext.getSystemService("security");
        Uri uri = Settings.System.getUriFor(POWER_SAVE_MODE_OPEN);
        this.mContext.getContentResolver().registerContentObserver(uri, true, new ContentObserver(null) { // from class: com.android.server.AppOpsServiceState.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri2) {
                AppOpsServiceState.this.updatePowerState();
            }
        });
        updatePowerState();
    }

    public void updatePowerState() {
        boolean z = false;
        if (Settings.System.getInt(this.mContext.getContentResolver(), POWER_SAVE_MODE_OPEN, 0) == 1) {
            z = true;
        }
        this.mPowerSaving = z;
    }

    public boolean isAppPermissionControlOpen(int op, int uid) {
        SecurityManager securityManager;
        if (sOpInControl.get(Integer.valueOf(op)) == null || op == 10008 || (securityManager = this.mSecurityManager) == null) {
            return true;
        }
        boolean result = securityManager.getAppPermissionControlOpen(UserHandle.getUserId(uid));
        return result;
    }

    public int askOperationLocked(int code, int uid, String packageName, String message) {
        if (MESSAGE_NO_ASK.equals(message) || MESSAGE_NO_ASK_NO_RECORD.equals(message)) {
            return this.mDefaultMode;
        }
        int result = this.mDefaultMode;
        int userId = UserHandle.getUserId(uid);
        if (userId == 999) {
            uid = UserHandle.getUid(0, UserHandle.getAppId(uid));
        }
        UserState uidState = getUidState(UserHandle.getUserId(uid), false);
        if (uidState != null && uidState.mCallbackBinder != null) {
            return uidState.mCallbackBinder.callNonOneWayTransact(1, new Object[]{Integer.valueOf(uid), packageName, Integer.valueOf(code)});
        }
        return result;
    }

    public void onAppApplyOperation(int uid, String packageName, int op, int mode, int operationType, int processState, int capability, boolean appWidgetVisible, int flags, String message) {
        if ((flags & 32) == 0 && !MESSAGE_NOT_RECORD.contains(message)) {
            if ((!TextUtils.isEmpty(message) && message.startsWith(WIFI_MESSAGE_STARTWITH)) || MESSAGE_NO_ASK_NO_RECORD.equals(message)) {
                return;
            }
            onAppApplyOperation(uid, packageName, op, mode, operationType, processState, capability, appWidgetVisible);
        }
    }

    public void onAppApplyOperation(int uid, String packageName, int op, int mode, int operationType, int processState, int capability, boolean appWidgetVisible) {
        ActivityManagerInternal activityManagerInternal;
        if (UserHandle.getAppId(uid) < 10000) {
            return;
        }
        if (mode == 4 && processState > 200 && processState <= AppOpsManager.resolveFirstUnrestrictedUidState(op)) {
            if (appWidgetVisible || ((activityManagerInternal = this.mActivityManagerInternal) != null && activityManagerInternal.isPendingTopUid(uid))) {
                processState = ScoutHelper.BINDER_FULL_KILL_SCORE_ADJ_MIN;
            }
            switch (op) {
                case 0:
                case 1:
                case 41:
                case ArpPacket.ARP_ETHER_IPV4_LEN /* 42 */:
                    if ((capability & 1) != 0) {
                        processState = ScoutHelper.BINDER_FULL_KILL_SCORE_ADJ_MIN;
                        break;
                    }
                    break;
                case 26:
                    if ((capability & 2) != 0) {
                        processState = ScoutHelper.BINDER_FULL_KILL_SCORE_ADJ_MIN;
                        break;
                    }
                    break;
                case 27:
                    if ((capability & 4) != 0) {
                        processState = ScoutHelper.BINDER_FULL_KILL_SCORE_ADJ_MIN;
                        break;
                    }
                    break;
            }
        }
        UserState uidState = getUidState(UserHandle.getUserId(uid), false);
        if (uidState != null && uidState.mCallbackBinder != null) {
            uidState.mCallbackBinder.callOneWayTransact(4, new Object[]{Integer.valueOf(uid), packageName, Integer.valueOf(op), Integer.valueOf(mode), Integer.valueOf(operationType), Integer.valueOf(processState)});
        }
    }

    public void updateProcessState(int uid, int procState) {
        UserState uidState;
        if (UserHandle.getAppId(uid) >= 10000 && (uidState = getUidState(UserHandle.getUserId(uid), false)) != null && uidState.mCallbackBinder != null) {
            uidState.mCallbackBinder.callOneWayTransact(5, new Object[]{Integer.valueOf(uid), Integer.valueOf(procState)});
        }
    }

    public int registerCallback(IBinder callback) {
        this.mContext.enforcePermission("android.permission.UPDATE_APP_OPS_STATS", Binder.getCallingPid(), Binder.getCallingUid(), null);
        if (callback == null) {
            return -1;
        }
        int callingUserId = UserHandle.getCallingUserId();
        if (callingUserId == 0) {
            registerCallback(callback, 999);
        }
        return registerCallback(callback, callingUserId);
    }

    public int registerCallback(IBinder callback, int userId) {
        UserState uidState;
        this.mContext.enforcePermission("android.permission.UPDATE_APP_OPS_STATS", Binder.getCallingPid(), Binder.getCallingUid(), null);
        if (callback == null || (uidState = getUidState(userId, true)) == null) {
            return -1;
        }
        uidState.mCallbackBinder = new MiuiBinderProxy(callback, "com.android.internal.app.IOpsCallback");
        if (uidState.mCallback != null) {
            uidState.mCallback.unlinkToDeath();
        }
        uidState.mCallback = new Callback(callback, userId);
        return 0;
    }

    public static boolean isCtsIgnore(String packageName) {
        return sCtsIgnore.contains(packageName);
    }

    public void startService(final int userId) {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() { // from class: com.android.server.AppOpsServiceState.2
            @Override // java.lang.Runnable
            public void run() {
                try {
                    Intent intent = new Intent("com.miui.permission.Action.SecurityService");
                    intent.setPackage("com.lbe.security.miui");
                    AppOpsServiceState.this.mContext.startServiceAsUser(intent, new UserHandle(userId));
                } catch (Exception e) {
                    Slog.e(AppOpsServiceState.TAG, "Start Error", e);
                }
            }
        }, 1300L);
    }

    /* loaded from: classes.dex */
    public final class Callback implements IBinder.DeathRecipient {
        final IBinder mCallback;
        volatile boolean mUnLink;
        final int mUserId;

        public Callback(IBinder callback, int userId) {
            AppOpsServiceState.this = this$0;
            this.mCallback = callback;
            this.mUserId = userId;
            try {
                callback.linkToDeath(this, 0);
                Slog.d(AppOpsServiceState.TAG, "linkToDeath");
            } catch (RemoteException e) {
            }
        }

        public void unlinkToDeath() {
            if (this.mUnLink) {
                return;
            }
            try {
                this.mUnLink = true;
                this.mCallback.unlinkToDeath(this, 0);
            } catch (Exception e) {
            }
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            unlinkToDeath();
            AppOpsServiceState.this.startService(this.mUserId);
            Slog.d(AppOpsServiceState.TAG, "binderDied mUserId : " + this.mUserId);
        }
    }

    public static boolean assessVirtualEnable(int code, int uid, int pid) {
        if (code > 10000 || code == 29 || code == 30 || UserHandle.getAppId(uid) < 10000) {
            return true;
        }
        String pkgName = ProcessUtils.getPackageNameByPid(pid);
        PackageManagerInternal pmi = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        ApplicationInfo appInfo = pmi.getApplicationInfo(pkgName, 0L, 1000, UserHandle.getUserId(uid));
        return (appInfo == null || (appInfo.flags & 1) == 0) ? false : true;
    }

    public static boolean isSupportVirtualGrant(int code) {
        return !Build.IS_INTERNATIONAL_BUILD && supportVirtualGrant.containsKey(Integer.valueOf(code));
    }

    public static int convertVirtualOp(int code) {
        return supportVirtualGrant.getOrDefault(Integer.valueOf(code), Integer.valueOf(code)).intValue();
    }

    public static boolean supportForegroundByMiui(String permission) {
        return "android.permission.READ_EXTERNAL_STORAGE".equals(permission) || "android.permission.WRITE_EXTERNAL_STORAGE".equals(permission) || "android.permission.READ_CALL_LOG".equals(permission) || "android.permission.WRITE_CALL_LOG".equals(permission) || "android.permission.READ_CALENDAR".equals(permission) || "android.permission.WRITE_CALENDAR".equals(permission) || "android.permission.READ_CONTACTS".equals(permission) || "android.permission.WRITE_CONTACTS".equals(permission);
    }
}
