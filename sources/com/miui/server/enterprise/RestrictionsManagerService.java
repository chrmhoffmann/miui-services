package com.miui.server.enterprise;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AppOpsManager;
import android.app.StatusBarManager;
import android.app.admin.IDevicePolicyManager;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.hardware.usb.UsbManager;
import android.net.IVpnManager;
import android.net.Uri;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.provider.Settings;
import android.util.Log;
import android.util.Slog;
import com.android.internal.app.LocalePicker;
import com.android.internal.net.LegacyVpnInfo;
import com.android.internal.net.VpnConfig;
import com.android.server.content.MiSyncConstants;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.UserManagerService;
import com.android.server.wm.WindowManagerService;
import com.miui.enterprise.IRestrictionsManager;
import com.miui.enterprise.RestrictionsHelper;
import com.miui.enterprise.settings.EnterpriseSettings;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import miui.securityspace.CrossUserUtils;
/* loaded from: classes.dex */
public class RestrictionsManagerService extends IRestrictionsManager.Stub {
    private static final String TAG = "Enterprise-restric";
    private AppOpsManager mAppOpsManager;
    private Context mContext;
    private ComponentName mDeviceOwner;
    private IDevicePolicyManager mDevicePolicyManager;
    private Handler mHandler;
    private WindowManagerService mWindowManagerService = ServiceManager.getService("window");
    private PackageManagerService.IPackageManagerImpl mPMS = ServiceManager.getService("package");
    private UserManagerService mUserManager = ServiceManager.getService("user");

    /* JADX WARN: Multi-variable type inference failed */
    public void bootComplete() {
        Slog.d(TAG, "Restriction init");
        UserManager userManager = (UserManager) this.mContext.getSystemService("user");
        List<UserInfo> users = userManager.getUsers();
        for (UserInfo user : users) {
            if (RestrictionsHelper.hasRestriction(this.mContext, "disallow_screencapture", user.id)) {
                RestrictionManagerServiceProxy.setScreenCaptureDisabled(this.mWindowManagerService, this.mContext, user.id, true);
            }
            if (RestrictionsHelper.hasRestriction(this.mContext, "disallow_vpn", user.id)) {
                this.mAppOpsManager.setUserRestrictionForUser(47, true, this, null, user.id);
            }
            if (RestrictionsHelper.hasRestriction(this.mContext, "disallow_fingerprint", user.id)) {
                this.mAppOpsManager.setUserRestrictionForUser(55, true, this, null, user.id);
            }
            if (RestrictionsHelper.hasRestriction(this.mContext, "disallow_imeiread", user.id)) {
                this.mAppOpsManager.setUserRestrictionForUser(51, true, this, null, user.id);
            }
        }
        startWatchLocationRestriction();
    }

    public RestrictionsManagerService(Context context) {
        this.mContext = context;
        IDevicePolicyManager asInterface = IDevicePolicyManager.Stub.asInterface(ServiceManager.getService("device_policy"));
        this.mDevicePolicyManager = asInterface;
        try {
            this.mDeviceOwner = asInterface.getDeviceOwnerComponent(true);
        } catch (RemoteException e) {
        }
        this.mAppOpsManager = (AppOpsManager) this.mContext.getSystemService("appops");
        this.mHandler = new Handler();
    }

    private void startWatchLocationRestriction() {
        Uri uri = Settings.Secure.getUriFor("location_providers_allowed");
        this.mContext.getContentResolver().registerContentObserver(uri, true, new ContentObserver(this.mHandler) { // from class: com.miui.server.enterprise.RestrictionsManagerService.1
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                int currentUserId = CrossUserUtils.getCurrentUserId();
                int mode = EnterpriseSettings.getInt(RestrictionsManagerService.this.mContext, "gps_state", 1, currentUserId);
                if (mode == 4) {
                    Slog.d(RestrictionsManagerService.TAG, "FORCE_OPEN GPS");
                    Settings.Secure.putIntForUser(RestrictionsManagerService.this.mContext.getContentResolver(), "location_mode", 3, currentUserId);
                } else if (mode == 0) {
                    Slog.d(RestrictionsManagerService.TAG, "Close GPS");
                    Settings.Secure.putIntForUser(RestrictionsManagerService.this.mContext.getContentResolver(), "location_mode", 0, currentUserId);
                }
            }
        }, -1);
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code restructure failed: missing block: B:13:0x0035, code lost:
        if (r9.equals("airplane_state") != false) goto L27;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public void setControlStatus(java.lang.String r9, int r10, int r11) {
        /*
            Method dump skipped, instructions count: 310
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.miui.server.enterprise.RestrictionsManagerService.setControlStatus(java.lang.String, int, int):void");
    }

    private boolean shouldOpen(int state) {
        return state == 2 || state == 4;
    }

    private boolean shouldClose(int state) {
        return state == 0 || state == 3;
    }

    /* JADX WARN: Multi-variable type inference failed */
    public void setRestriction(String key, boolean value, int userId) {
        ServiceUtils.checkPermission(this.mContext);
        EnterpriseSettings.putInt(this.mContext, key, value ? 1 : 0, userId);
        char c = 65535;
        int i = 0;
        switch (key.hashCode()) {
            case -1915200762:
                if (key.equals("disallow_backup")) {
                    c = 15;
                    break;
                }
                break;
            case -1886279575:
                if (key.equals("disallow_camera")) {
                    c = 2;
                    break;
                }
                break;
            case -1859967211:
                if (key.equals("disallow_system_update")) {
                    c = 19;
                    break;
                }
                break;
            case -1552410727:
                if (key.equals("disallow_landscape_statusbar")) {
                    c = 25;
                    break;
                }
                break;
            case -1425744347:
                if (key.equals("disallow_sdcard")) {
                    c = 5;
                    break;
                }
                break;
            case -1395678890:
                if (key.equals("disallow_tether")) {
                    c = 1;
                    break;
                }
                break;
            case -1094316185:
                if (key.equals("disallow_auto_sync")) {
                    c = 16;
                    break;
                }
                break;
            case -831735134:
                if (key.equals("disallow_imeiread")) {
                    c = '\r';
                    break;
                }
                break;
            case -453687405:
                if (key.equals("disallow_usbdebug")) {
                    c = '\t';
                    break;
                }
                break;
            case -429823771:
                if (key.equals("disallow_mtp")) {
                    c = 6;
                    break;
                }
                break;
            case -429821858:
                if (key.equals("disallow_otg")) {
                    c = 7;
                    break;
                }
                break;
            case -429815248:
                if (key.equals("disallow_vpn")) {
                    c = 0;
                    break;
                }
                break;
            case -208396879:
                if (key.equals("disallow_timeset")) {
                    c = '\f';
                    break;
                }
                break;
            case -170808536:
                if (key.equals("disable_usb_device")) {
                    c = '\b';
                    break;
                }
                break;
            case 82961609:
                if (key.equals("disallow_factoryreset")) {
                    c = 11;
                    break;
                }
                break;
            case 408143697:
                if (key.equals("disallow_safe_mode")) {
                    c = 17;
                    break;
                }
                break;
            case 411690763:
                if (key.equals("disallow_key_back")) {
                    c = 22;
                    break;
                }
                break;
            case 411883267:
                if (key.equals("disallow_key_home")) {
                    c = 23;
                    break;
                }
                break;
            case 412022659:
                if (key.equals("disallow_key_menu")) {
                    c = 24;
                    break;
                }
                break;
            case 470637462:
                if (key.equals("disallow_screencapture")) {
                    c = 4;
                    break;
                }
                break;
            case 480222787:
                if (key.equals("disallow_change_language")) {
                    c = 18;
                    break;
                }
                break;
            case 487960096:
                if (key.equals("disallow_fingerprint")) {
                    c = '\n';
                    break;
                }
                break;
            case 724096394:
                if (key.equals("disallow_status_bar")) {
                    c = 20;
                    break;
                }
                break;
            case 907380174:
                if (key.equals("disallow_mi_account")) {
                    c = 21;
                    break;
                }
                break;
            case 1157197624:
                if (key.equals("disable_accelerometer")) {
                    c = 14;
                    break;
                }
                break;
            case 1846688878:
                if (key.equals("disallow_microphone")) {
                    c = 3;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
                boolean hasUserRestriction = this.mUserManager.hasUserRestriction("no_config_vpn", userId);
                if (value != 0 && CrossUserUtils.getCurrentUserId() == userId && !hasUserRestriction) {
                    try {
                        IVpnManager vpnmanager = IVpnManager.Stub.asInterface(ServiceManager.getService("vpn_management"));
                        VpnConfig mConfig = vpnmanager.getVpnConfig(UserHandle.myUserId());
                        if (mConfig != null) {
                            mConfig.configureIntent.send();
                        }
                        LegacyVpnInfo info = vpnmanager.getLegacyVpnInfo(UserHandle.myUserId());
                        if (info != null) {
                            info.intent.send();
                        }
                    } catch (Exception e) {
                        Slog.d(TAG, "Something wrong while close vpn: " + e);
                    }
                }
                this.mUserManager.setUserRestriction("no_config_vpn", value, userId);
                this.mAppOpsManager.setUserRestrictionForUser(47, value, this, null, userId);
                return;
            case 1:
                boolean hasUserRestriction2 = this.mUserManager.hasUserRestriction("no_config_tethering", userId);
                if (value != 0 && userId == CrossUserUtils.getCurrentUserId() && !hasUserRestriction2) {
                    RestrictionManagerServiceProxy.setWifiApEnabled(this.mContext, false);
                }
                this.mUserManager.setUserRestriction("no_config_tethering", value, userId);
                return;
            case 2:
                this.mUserManager.setUserRestriction("no_camera", value, userId);
                return;
            case 3:
                this.mUserManager.setUserRestriction("no_record_audio", value, userId);
                return;
            case 4:
                RestrictionManagerServiceProxy.setScreenCaptureDisabled(this.mWindowManagerService, this.mContext, userId, value);
                return;
            case 5:
                if (value != 0 && CrossUserUtils.getCurrentUserId() == userId) {
                    unmountPublicVolume(4);
                    return;
                }
                return;
            case 6:
                UsbManager usbManager = (UsbManager) this.mContext.getSystemService("usb");
                this.mUserManager.setUserRestriction("no_usb_file_transfer", value, userId);
                setUsbFunction(usbManager, "none");
                return;
            case 7:
            case 14:
            case 19:
            case 21:
            case 22:
            case 23:
            case 24:
            case 25:
                return;
            case '\b':
                if (value != 0 && CrossUserUtils.getCurrentUserId() == userId) {
                    unmountPublicVolume(8);
                    return;
                }
                return;
            case '\t':
                if (CrossUserUtils.getCurrentUserId() == userId) {
                    Settings.Global.putInt(this.mContext.getContentResolver(), "adb_enabled", 0);
                }
                this.mUserManager.setUserRestriction("no_debugging_features", value, userId);
                return;
            case '\n':
                this.mAppOpsManager.setUserRestrictionForUser(55, value, this, null, userId);
                return;
            case 11:
                this.mUserManager.setUserRestriction("no_factory_reset", value, userId);
                return;
            case '\f':
                if (CrossUserUtils.getCurrentUserId() == userId) {
                    Settings.Global.putInt(this.mContext.getContentResolver(), "auto_time", value);
                }
                Settings.Secure.putIntForUser(this.mContext.getContentResolver(), "time_change_disallow", value, userId);
                return;
            case '\r':
                this.mAppOpsManager.setUserRestrictionForUser(51, value, this, null, userId);
                return;
            case 15:
                if (value != 0) {
                    this.mPMS.setApplicationEnabledSetting("com.miui.backup", 2, 0, userId, this.mContext.getPackageName());
                    return;
                } else {
                    this.mPMS.setApplicationEnabledSetting("com.miui.backup", 1, 0, userId, this.mContext.getPackageName());
                    return;
                }
            case 16:
                if (value != 0) {
                    ContentResolver.setMasterSyncAutomaticallyAsUser(false, userId);
                    closeCloudBackup(userId);
                    return;
                }
                return;
            case 17:
                this.mUserManager.setUserRestriction("no_safe_boot", value, userId);
                return;
            case 18:
                LocalePicker.updateLocale(Locale.CHINA);
                return;
            case 20:
                StatusBarManager statusBarManager = (StatusBarManager) this.mContext.getSystemService("statusbar");
                if (statusBarManager == null) {
                    Log.e(TAG, "statusBarManager is null!");
                    return;
                }
                if (value != 0) {
                    i = 65536;
                }
                statusBarManager.disable(i);
                return;
            default:
                throw new IllegalArgumentException("Unknown restriction item: " + key);
        }
    }

    public int getControlStatus(String key, int userId) {
        ServiceUtils.checkPermission(this.mContext);
        return EnterpriseSettings.getInt(this.mContext, key, 1, userId);
    }

    public boolean hasRestriction(String key, int userId) {
        ServiceUtils.checkPermission(this.mContext);
        return EnterpriseSettings.getInt(this.mContext, key, 0, userId) == 1;
    }

    private void unmountPublicVolume(int volFlag) {
        final StorageManager storageManager = (StorageManager) this.mContext.getSystemService(StorageManager.class);
        VolumeInfo usbVol = null;
        Iterator it = storageManager.getVolumes().iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            VolumeInfo vol = (VolumeInfo) it.next();
            if (vol.getType() == 0 && (vol.getDisk().flags & volFlag) == volFlag) {
                usbVol = vol;
                break;
            }
        }
        if (usbVol != null && usbVol.getState() == 2) {
            final String volId = usbVol.getId();
            new Thread(new Runnable() { // from class: com.miui.server.enterprise.RestrictionsManagerService.2
                @Override // java.lang.Runnable
                public void run() {
                    storageManager.unmount(volId);
                }
            }).start();
        }
    }

    private void closeCloudBackup(int userId) {
        Account account = null;
        AccountManager am = AccountManager.get(this.mContext);
        Account[] accounts = am.getAccountsByTypeAsUser(MiSyncConstants.Config.XIAOMI_ACCOUNT_TYPE, new UserHandle(userId));
        if (accounts.length > 0) {
            account = accounts[0];
        }
        if (account != null) {
            ContentValues values = new ContentValues();
            values.put("account_name", account.name);
            values.put("is_open", (Boolean) false);
            Uri cloudBackupInfoUri = Uri.parse("content://com.miui.micloud").buildUpon().appendPath("cloud_backup_info").build();
            this.mContext.getContentResolver().update(ContentProvider.maybeAddUserId(cloudBackupInfoUri, userId), values, null, null);
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.miui.cloudbackup", "com.miui.cloudbackup.service.CloudBackupService"));
            intent.setAction("close_cloud_back_up");
            this.mContext.startServiceAsUser(intent, new UserHandle(userId));
        }
    }

    private void setUsbFunction(UsbManager usbManager, String function) {
        try {
            Method method = UsbManager.class.getDeclaredMethod("setCurrentFunction", String.class);
            method.setAccessible(true);
            method.invoke(usbManager, function);
        } catch (Exception e) {
            try {
                Method method2 = UsbManager.class.getDeclaredMethod("setCurrentFunction", String.class, Boolean.TYPE);
                method2.setAccessible(true);
                method2.invoke(usbManager, function, false);
            } catch (Exception e1) {
                Slog.d(TAG, "Failed to set usb function", e1);
            }
        }
    }
}
