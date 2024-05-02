package com.android.server;

import android.app.ActivityManagerInternal;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManagerInternal;
import android.content.pm.PackageManagerStub;
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
import android.util.ArrayMap;
import android.util.Slog;
import android.util.SparseArray;
import com.android.server.appop.AppOpsServiceStub;
import com.android.server.pm.parsing.pkg.AndroidPackage;
import com.android.server.wifi.ArpPacket;
import com.miui.base.MiuiStubRegistry;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import miui.os.Build;
import miui.security.SecurityManager;
/* loaded from: classes.dex */
public class AppOpsServiceStubImpl extends AppOpsServiceStub {
    public static final boolean DEBUG = false;
    private static final String KEY_APP_BEHAVIOR_RECORD_ENABLE = "key_app_behavior_record_enable";
    private static final Set<String> MESSAGE_NOT_RECORD;
    private static final String MESSAGE_NO_ASK = "noteNoAsk";
    private static final String MESSAGE_NO_ASK_NO_RECORD = "NoAskNoRecord";
    private static final ArrayMap<String, String> PLATFORM_PERMISSIONS;
    private static final String POWER_SAVE_MODE_OPEN = "POWER_SAVE_MODE_OPEN";
    private static final String TAG = "AppOpsServiceStubImpl";
    private static final String WIFI_MESSAGE_STARTWITH = "WifiPermissionsUtil#noteAppOpAllowed";
    private static final Set<String> sCtsIgnore;
    private static final Map<Integer, Integer> supportVirtualGrant;
    private ActivityManagerInternal mActivityManagerInternal;
    private Context mContext;
    private PackageManagerInternal mPackageManagerInternal;
    private boolean mPowerSaving;
    private SecurityManager mSecurityManager;
    final SparseArray<UserState> mUidStates = new SparseArray<>();
    private boolean mRecordEnable = true;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<AppOpsServiceStubImpl> {

        /* compiled from: AppOpsServiceStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final AppOpsServiceStubImpl INSTANCE = new AppOpsServiceStubImpl();
        }

        public AppOpsServiceStubImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public AppOpsServiceStubImpl provideNewInstance() {
            return new AppOpsServiceStubImpl();
        }
    }

    static {
        HashSet hashSet = new HashSet();
        sCtsIgnore = hashSet;
        HashSet hashSet2 = new HashSet();
        MESSAGE_NOT_RECORD = hashSet2;
        ArrayMap<String, String> arrayMap = new ArrayMap<>();
        PLATFORM_PERMISSIONS = arrayMap;
        arrayMap.put("android.permission.SEND_SMS", "android.permission-group.SMS");
        arrayMap.put("android.permission.RECEIVE_SMS", "android.permission-group.SMS");
        arrayMap.put("android.permission.READ_SMS", "android.permission-group.SMS");
        arrayMap.put("android.permission.RECEIVE_MMS", "android.permission-group.SMS");
        arrayMap.put("android.permission.RECEIVE_WAP_PUSH", "android.permission-group.SMS");
        arrayMap.put("android.permission.READ_CELL_BROADCASTS", "android.permission-group.SMS");
        hashSet.add("android.app.usage.cts");
        hashSet.add("com.android.cts.usepermission");
        hashSet.add("com.android.cts.permission");
        hashSet.add("com.android.cts.netlegacy22.permission");
        hashSet.add("android.netlegacy22.permission.cts");
        hashSet.add("android.provider.cts");
        hashSet.add("android.telephony2.cts");
        hashSet.add("android.permission.cts");
        hashSet.add("com.android.cts.writeexternalstorageapp");
        hashSet.add("com.android.cts.readexternalstorageapp");
        hashSet.add("com.android.cts.externalstorageapp");
        hashSet.add("android.server.alertwindowapp");
        hashSet.add("android.server.alertwindowappsdk25");
        hashSet.add("com.android.app2");
        hashSet.add("com.android.cts.appbinding.app");
        hashSet.add("com.android.cts.launcherapps.simplepremapp");
        hashSet2.add("AutoStartManagerService#signalStopProcessesLocked");
        hashSet2.add("ContentProvider#enforceReadPermission");
        hashSet2.add("ContentProvider#enforceWritePermission");
        hashSet2.add("AppOpsHelper#checkLocationAccess");
        hashSet2.add(WIFI_MESSAGE_STARTWITH);
        HashMap hashMap = new HashMap();
        supportVirtualGrant = hashMap;
        hashMap.put(14, 10028);
        hashMap.put(4, 10029);
        hashMap.put(8, 10030);
        hashMap.put(6, 10031);
        hashMap.put(51, 10032);
        hashMap.put(65, 10032);
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
        this.mContext.getContentResolver().registerContentObserver(uri, true, new ContentObserver(null) { // from class: com.android.server.AppOpsServiceStubImpl.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri2) {
                AppOpsServiceStubImpl.this.updatePowerState();
            }
        });
        updatePowerState();
        Uri enableUri = Settings.Global.getUriFor(KEY_APP_BEHAVIOR_RECORD_ENABLE);
        ContentObserver observer = new ContentObserver(null) { // from class: com.android.server.AppOpsServiceStubImpl.2
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange, Uri uri2) {
                AppOpsServiceStubImpl.this.updateRecordState();
            }
        };
        this.mContext.getContentResolver().registerContentObserver(enableUri, false, observer);
        updateRecordState();
    }

    public void updateRecordState() {
        boolean z = true;
        if (Settings.Global.getInt(this.mContext.getContentResolver(), KEY_APP_BEHAVIOR_RECORD_ENABLE, 1) != 1) {
            z = false;
        }
        this.mRecordEnable = z;
        Slog.d(TAG, "updateRecordState: " + this.mRecordEnable);
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
        if (AppOpsService.sOpInControl.get(Integer.valueOf(op)) == null || op == 10008 || (securityManager = this.mSecurityManager) == null) {
            return true;
        }
        boolean result = securityManager.getAppPermissionControlOpen(UserHandle.getUserId(uid));
        return result;
    }

    public int askOperationLocked(int code, int uid, String packageName, String message) {
        if (MESSAGE_NO_ASK.equals(message) || MESSAGE_NO_ASK_NO_RECORD.equals(message)) {
            return 1;
        }
        int userId = UserHandle.getUserId(uid);
        if (userId == 999) {
            uid = UserHandle.getUid(0, UserHandle.getAppId(uid));
        }
        UserState uidState = getUidState(UserHandle.getUserId(uid), false);
        if (uidState == null || uidState.mCallbackBinder == null) {
            return 1;
        }
        int result = uidState.mCallbackBinder.callNonOneWayTransact(1, new Object[]{Integer.valueOf(uid), packageName, Integer.valueOf(code)});
        return result;
    }

    private ActivityManagerInternal getActivityManagerInternal() {
        if (this.mActivityManagerInternal == null) {
            this.mActivityManagerInternal = (ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class);
        }
        return this.mActivityManagerInternal;
    }

    private PackageManagerInternal getPackageManagerInternal() {
        if (this.mPackageManagerInternal == null) {
            this.mPackageManagerInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        }
        return this.mPackageManagerInternal;
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
        if (UserHandle.getAppId(uid) <= 2000 || !this.mRecordEnable) {
            return;
        }
        if ((mode == 4 || mode == 0) && processState > 200) {
            if (appWidgetVisible || (getActivityManagerInternal() != null && getActivityManagerInternal().isPendingTopUid(uid))) {
                processState = ScoutHelper.BINDER_FULL_KILL_SCORE_ADJ_MIN;
                mode = 0;
            } else if (processState <= AppOpsManager.resolveFirstUnrestrictedUidState(op)) {
                switch (op) {
                    case 0:
                    case 1:
                    case 41:
                    case ArpPacket.ARP_ETHER_IPV4_LEN /* 42 */:
                        if ((capability & 1) != 0) {
                            processState = ScoutHelper.BINDER_FULL_KILL_SCORE_ADJ_MIN;
                            mode = 0;
                            break;
                        }
                        break;
                    case 26:
                        if ((capability & 2) != 0) {
                            processState = ScoutHelper.BINDER_FULL_KILL_SCORE_ADJ_MIN;
                            mode = 0;
                            break;
                        }
                        break;
                    case 27:
                        if ((capability & 4) != 0) {
                            processState = ScoutHelper.BINDER_FULL_KILL_SCORE_ADJ_MIN;
                            mode = 0;
                            break;
                        }
                        break;
                    default:
                        processState = ScoutHelper.BINDER_FULL_KILL_SCORE_ADJ_MIN;
                        mode = 0;
                        break;
                }
                if (mode != 0) {
                    mode = 1;
                }
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

    public void startService(final int userId) {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() { // from class: com.android.server.AppOpsServiceStubImpl$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                AppOpsServiceStubImpl.this.m11lambda$startService$0$comandroidserverAppOpsServiceStubImpl(userId);
            }
        }, 1300L);
    }

    /* renamed from: lambda$startService$0$com-android-server-AppOpsServiceStubImpl */
    public /* synthetic */ void m11lambda$startService$0$comandroidserverAppOpsServiceStubImpl(int userId) {
        try {
            Intent intent = new Intent("com.miui.permission.Action.SecurityService");
            intent.setPackage("com.lbe.security.miui");
            this.mContext.startServiceAsUser(intent, new UserHandle(userId));
        } catch (Exception e) {
            Slog.e(TAG, "Start Error", e);
        }
    }

    /* loaded from: classes.dex */
    public final class Callback implements IBinder.DeathRecipient {
        final IBinder mCallback;
        volatile boolean mUnLink;
        final int mUserId;

        public Callback(IBinder callback, int userId) {
            AppOpsServiceStubImpl.this = this$0;
            this.mCallback = callback;
            this.mUserId = userId;
            try {
                callback.linkToDeath(this, 0);
                Slog.d(AppOpsServiceStubImpl.TAG, "linkToDeath");
            } catch (RemoteException e) {
                Slog.e(AppOpsServiceStubImpl.TAG, "linkToDeath failed!", e);
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
                Slog.e(AppOpsServiceStubImpl.TAG, "unlinkToDeath failed!", e);
            }
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            unlinkToDeath();
            AppOpsServiceStubImpl.this.startService(this.mUserId);
            Slog.d(AppOpsServiceStubImpl.TAG, "binderDied mUserId : " + this.mUserId);
        }
    }

    public boolean assessVirtualEnable(int code, int uid, int pid) {
        if (code > 10000 || code == 29 || code == 30 || UserHandle.getAppId(uid) < 10000) {
            return true;
        }
        AndroidPackage pkg = getPackageManagerInternal().getPackage(uid);
        return pkg != null && pkg.isSystem();
    }

    public boolean isSupportVirtualGrant(int code) {
        return !Build.IS_INTERNATIONAL_BUILD && supportVirtualGrant.containsKey(Integer.valueOf(code));
    }

    public int convertVirtualOp(int code) {
        return supportVirtualGrant.getOrDefault(Integer.valueOf(code), Integer.valueOf(code)).intValue();
    }

    public boolean supportForegroundByMiui(String permission) {
        return "android.permission.READ_EXTERNAL_STORAGE".equals(permission) || "android.permission.WRITE_EXTERNAL_STORAGE".equals(permission) || "android.permission.READ_CALL_LOG".equals(permission) || "android.permission.WRITE_CALL_LOG".equals(permission) || "android.permission.READ_CALENDAR".equals(permission) || "android.permission.WRITE_CALENDAR".equals(permission) || "android.permission.READ_CONTACTS".equals(permission) || "android.permission.WRITE_CONTACTS".equals(permission) || "android.permission.GET_ACCOUNTS".equals(permission);
    }

    public boolean skipSyncAppOpsWithRuntime(int code, int mode, int oldMode) {
        if (PackageManagerStub.get().isOptimizationMode()) {
            String permission = AppOpsManager.opToPermission(code);
            if (oldMode == 4 && AppOpsServiceState.supportForegroundByMiui(permission)) {
                return true;
            }
            return TextUtils.equals(PLATFORM_PERMISSIONS.get(permission), "android.permission-group.SMS");
        }
        return false;
    }

    public boolean skipNotificationRequest(ActivityInfo activityInfo) {
        return (activityInfo == null || activityInfo.name == null || !activityInfo.name.contains("SplashActivity")) ? false : true;
    }

    public int getNotificationRequestDelay(ActivityInfo activityInfo) {
        if (activityInfo == null) {
            return 0;
        }
        if (this.mContext.getResources().getConfiguration().orientation == 1) {
            switch (activityInfo.screenOrientation) {
                case 0:
                case 6:
                case 8:
                case 11:
                    return 1000;
                default:
                    return 0;
            }
        }
        switch (activityInfo.screenOrientation) {
            case 1:
            case 7:
            case 9:
            case 12:
                return 1000;
            default:
                return 0;
        }
    }
}
