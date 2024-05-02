package com.android.server.policy;

import android.app.ActivityManagerNative;
import android.app.ActivityTaskManager;
import android.app.AlertDialog;
import android.app.IActivityTaskManager;
import android.app.MiuiStatusBarManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.biometrics.BiometricManager;
import android.hardware.fingerprint.IFingerprintService;
import android.hardware.input.InputManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Slog;
import android.view.IWindowManager;
import android.view.KeyEvent;
import android.view.WindowManager;
import com.android.internal.statusbar.IStatusBarService;
import com.android.server.LocalServices;
import com.android.server.cameracovered.MiuiCameraCoveredManagerService;
import com.android.server.input.ReflectionUtils;
import com.android.server.input.overscroller.ScrollerOptimizationConfigProvider;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.statusbar.StatusBarManagerInternal;
import com.android.server.wm.AccountHelper;
import com.miui.server.input.PadManager;
import com.miui.server.input.util.MiuiCustomizeShortCutUtils;
import com.miui.server.process.ProcessManagerInternal;
import java.lang.reflect.Method;
import miui.os.Build;
import miui.os.DeviceFeature;
import miui.view.MiuiSecurityPermissionHandler;
/* loaded from: classes.dex */
public class MiuiPhoneWindowManager extends BaseMiuiPhoneWindowManager {
    private static final int ACTION_NOT_PASS_TO_USER = 0;
    private static final int ACTION_PASS_TO_USER = 1;
    private static final String CAMERA_COVERED_SERVICE = "camera_covered_service";
    private static final int FINGERPRINT_NAV_ACTION_DEFAULT = -1;
    private static final int FINGERPRINT_NAV_ACTION_HOME = 1;
    private static final int FINGERPRINT_NAV_ACTION_NONE = 0;
    protected static final int NAV_BAR_BOTTOM = 0;
    protected static final int NAV_BAR_LEFT = 2;
    protected static final int NAV_BAR_RIGHT = 1;
    private static final boolean SUPPORT_POWERFP = SystemProperties.getBoolean("ro.hardware.fp.sideCap", false);
    private AccountHelper mAccountHelper;
    private BiometricManager mBiometricManager;
    private int mDisplayHeight;
    private int mDisplayRotation;
    private int mDisplayWidth;
    private IFingerprintService mFingerprintService;
    private MiuiSecurityPermissionHandler mMiuiSecurityPermissionHandler;
    private MIUIWatermarkCallback mPhoneWindowCallback;
    private long interceptPowerKeyTimeByDpadCenter = -1;
    private MiuiCameraCoveredManagerService mCameraCoveredService = null;
    private final Object mPowerLock = new Object();
    private Method mGetFpLockoutModeMethod = null;
    private AlertDialog mFpNavCenterActionChooseDialog = null;

    /* loaded from: classes.dex */
    public interface MIUIWatermarkCallback {
        void onHideWatermark();

        void onShowWatermark();
    }

    public void init(Context context, IWindowManager windowManager, WindowManagerPolicy.WindowManagerFuncs windowManagerFuncs) {
        super.init(context, windowManager, windowManagerFuncs);
        initInternal(context, windowManager, windowManagerFuncs);
    }

    public void systemReady() {
        super.systemReady();
        this.mMiuiKeyguardDelegate = new MiuiKeyguardServiceDelegate(this, this.mKeyguardDelegate, this.mPowerManager);
        this.mBiometricManager = (BiometricManager) this.mContext.getSystemService(BiometricManager.class);
        IBinder binder = ServiceManager.getService("fingerprint");
        this.mFingerprintService = IFingerprintService.Stub.asInterface(binder);
        systemReadyInternal();
        if (Build.IS_PRIVATE_BUILD || Build.IS_PRIVATE_WATER_MARKER) {
            AccountHelper accountHelper = AccountHelper.getInstance();
            this.mAccountHelper = accountHelper;
            accountHelper.registerAccountListener(this.mContext, new AccountHelper.AccountCallback() { // from class: com.android.server.policy.MiuiPhoneWindowManager.1
                @Override // com.android.server.wm.AccountHelper.AccountCallback
                public void onXiaomiAccountLogin() {
                    if (MiuiPhoneWindowManager.this.mMiuiSecurityPermissionHandler != null) {
                        MiuiPhoneWindowManager.this.mMiuiSecurityPermissionHandler.handleAccountLogin();
                    }
                }

                @Override // com.android.server.wm.AccountHelper.AccountCallback
                public void onXiaomiAccountLogout() {
                    if (MiuiPhoneWindowManager.this.mMiuiSecurityPermissionHandler != null) {
                        MiuiPhoneWindowManager.this.mMiuiSecurityPermissionHandler.handleAccountLogout();
                    }
                }

                @Override // com.android.server.wm.AccountHelper.AccountCallback
                public void onWifiSettingFinish() {
                    if (MiuiPhoneWindowManager.this.mMiuiSecurityPermissionHandler != null) {
                        MiuiPhoneWindowManager.this.mMiuiSecurityPermissionHandler.handleWifiSettingFinish();
                    }
                }
            });
            this.mMiuiSecurityPermissionHandler = new MiuiSecurityPermissionHandler(this.mContext, new MiuiSecurityPermissionHandler.PermissionViewCallback() { // from class: com.android.server.policy.MiuiPhoneWindowManager.2
                public void onShowWaterMarker() {
                    if (MiuiPhoneWindowManager.this.mPhoneWindowCallback != null) {
                        MiuiPhoneWindowManager.this.mPhoneWindowCallback.onShowWatermark();
                    }
                }

                public void onAddAccount() {
                    MiuiPhoneWindowManager.this.mAccountHelper.addAccount(MiuiPhoneWindowManager.this.mContext);
                }

                public void onListenAccount(int mode) {
                    MiuiPhoneWindowManager.this.mAccountHelper.ListenAccount(mode);
                }

                public void onUnListenAccount(int mode) {
                    MiuiPhoneWindowManager.this.mAccountHelper.UnListenAccount(mode);
                }

                public void onHideWaterMarker() {
                    if (MiuiPhoneWindowManager.this.mPhoneWindowCallback != null) {
                        MiuiPhoneWindowManager.this.mPhoneWindowCallback.onHideWatermark();
                    }
                }

                public void onListenPermission() {
                }
            });
        }
        if (Build.IS_TABLET) {
            MiuiCustomizeShortCutUtils.getInstance(this.mContext).loadShortCuts();
            MiuiCustomizeShortCutUtils.getInstance(this.mContext).enableAutoRemoveShortCutWhenAppRemove();
        }
    }

    public void systemBooted() {
        super.systemBooted();
        PadManager.getInstance().notifySystemBooted();
        if (DeviceFeature.SUPPORT_FRONTCAMERA_CIRCLE_BLACK && this.mCameraCoveredService == null) {
            MiuiCameraCoveredManagerService miuiCameraCoveredManagerService = (MiuiCameraCoveredManagerService) ServiceManager.getService("camera_covered_service");
            this.mCameraCoveredService = miuiCameraCoveredManagerService;
            if (miuiCameraCoveredManagerService == null) {
                Slog.e("WindowManager", "camera_covered_service not start!");
                return;
            }
            try {
                miuiCameraCoveredManagerService.systemBooted();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Slog.e("WindowManager", "camera_covered_service start again!");
        }
        ScrollerOptimizationConfigProvider.getInstance().systemBooted(this.mContext);
    }

    @Override // com.android.server.policy.BaseMiuiPhoneWindowManager
    protected void launchRecentPanelInternal() {
        StatusBarManagerInternal statusbar = getStatusBarManagerInternal();
        if (statusbar != null) {
            MiuiInputLog.defaults("execute launchRecentPanelInternal");
            statusbar.toggleRecentApps();
        }
    }

    @Override // com.android.server.policy.BaseMiuiPhoneWindowManager
    protected void preloadRecentAppsInternal() {
        StatusBarManagerInternal statusbar = getStatusBarManagerInternal();
        if (statusbar != null) {
            MiuiInputLog.defaults("execute preloadRecentAppsInternal");
            statusbar.preloadRecentApps();
        }
    }

    @Override // com.android.server.policy.BaseMiuiPhoneWindowManager
    protected void cancelPreloadRecentAppsInternal() {
        StatusBarManagerInternal statusbar = getStatusBarManagerInternal();
        if (statusbar != null) {
            MiuiInputLog.defaults("execute cancelPreloadRecentAppsInternal");
            statusbar.cancelPreloadRecentApps();
        }
    }

    @Override // com.android.server.policy.BaseMiuiPhoneWindowManager
    protected void toggleSplitScreenInternal() {
        StatusBarManagerInternal statusbar = getStatusBarManagerInternal();
        if (statusbar != null) {
            MiuiInputLog.defaults("execute toggleSplitScreenInternal");
            statusbar.toggleSplitScreen();
        }
    }

    void showGlobalActionsInternal() {
        MiuiInputLog.defaults("execute showGlobalActionsInternal");
        if (stopGoogleAssistantVoiceMonitoring()) {
            this.mContext.sendBroadcast(new Intent("close_asssistant_ui"));
            MiuiInputLog.defaults("close asssistant ui");
        }
        super.showGlobalActionsInternal();
    }

    private boolean stopGoogleAssistantVoiceMonitoring() {
        if (Build.IS_INTERNATIONAL_BUILD && this.mMiuiKeyShortcutManager != null && "launch_google_search".equals(this.mMiuiKeyShortcutManager.getFunction("long_press_power_key"))) {
            return true;
        }
        return false;
    }

    @Override // com.android.server.policy.BaseMiuiPhoneWindowManager
    protected void launchAssistActionInternal(String hint, Bundle args) {
        if (hint != null) {
            args.putBoolean(hint, true);
        }
        StatusBarManagerInternal statusbar = getStatusBarManagerInternal();
        if (statusbar != null) {
            Slog.i("WindowManager", "launch Google Assist");
            statusbar.startAssist(args);
        }
    }

    @Override // com.android.server.policy.BaseMiuiPhoneWindowManager
    protected boolean isScreenOnInternal() {
        return isScreenOn();
    }

    @Override // com.android.server.policy.BaseMiuiPhoneWindowManager
    protected void finishActivityInternal(IBinder token, int code, Intent data) throws RemoteException {
        ActivityManagerNative.getDefault().finishActivity(token, code, data, 0);
    }

    @Override // com.android.server.policy.BaseMiuiPhoneWindowManager
    protected void forceStopPackage(String packageName, int OwningUserId, String reason) {
        ((ProcessManagerInternal) LocalServices.getService(ProcessManagerInternal.class)).forceStopPackage(packageName, OwningUserId, reason);
    }

    public int interceptKeyBeforeQueueing(KeyEvent event, int policyFlags) {
        return interceptKeyBeforeQueueingInternal(event, policyFlags, (536870912 & policyFlags) != 0);
    }

    @Override // com.android.server.policy.BaseMiuiPhoneWindowManager
    protected int callSuperInterceptKeyBeforeQueueing(KeyEvent event, int policyFlags, boolean isScreenOn) {
        int interceptKeyBeforeQueueing;
        synchronized (this.mPowerLock) {
            interceptKeyBeforeQueueing = super.interceptKeyBeforeQueueing(event, policyFlags);
        }
        return interceptKeyBeforeQueueing;
    }

    @Override // com.android.server.policy.BaseMiuiPhoneWindowManager
    protected WindowManagerPolicy.WindowState getKeyguardWindowState() {
        return null;
    }

    @Override // com.android.server.policy.BaseMiuiPhoneWindowManager
    public int intercept(KeyEvent event, int policyFlags, boolean isScreenOn, int expectedResult) {
        super.intercept(event, policyFlags, isScreenOn, expectedResult);
        PowerManager pm = (PowerManager) this.mContext.getSystemService("power");
        if (expectedResult == -1) {
            pm.goToSleep(SystemClock.uptimeMillis());
            return 0;
        } else if (expectedResult == 1) {
            pm.wakeUp(SystemClock.uptimeMillis());
            return 0;
        } else {
            return 0;
        }
    }

    @Override // com.android.server.policy.BaseMiuiPhoneWindowManager
    protected int getWakePolicyFlag() {
        return 1;
    }

    @Override // com.android.server.policy.BaseMiuiPhoneWindowManager
    protected boolean screenOffBecauseOfProxSensor() {
        return false;
    }

    @Override // com.android.server.policy.BaseMiuiPhoneWindowManager
    protected void onStatusBarPanelRevealed(IStatusBarService statusBarService) {
        try {
            statusBarService.onPanelRevealed(true, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override // com.android.server.policy.BaseMiuiPhoneWindowManager
    protected boolean stopLockTaskMode() {
        try {
            IActivityTaskManager activityTaskManager = ActivityTaskManager.getService();
            if (activityTaskManager != null && activityTaskManager.isInLockTaskMode()) {
                activityTaskManager.stopSystemLockTaskMode();
                return true;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override // com.android.server.policy.BaseMiuiPhoneWindowManager
    protected boolean isInLockTaskMode() {
        try {
            IActivityTaskManager activityTaskManager = ActivityTaskManager.getService();
            if (activityTaskManager != null) {
                return activityTaskManager.isInLockTaskMode();
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override // com.android.server.policy.BaseMiuiPhoneWindowManager
    protected boolean isFingerPrintKey(KeyEvent event) {
        if (event.getDevice() == null || this.mFpNavEventNameList == null || !this.mFpNavEventNameList.contains(event.getDevice().getName())) {
            return false;
        }
        int keyCode = event.getKeyCode();
        switch (keyCode) {
            case 22:
            case 23:
                return true;
            default:
                return false;
        }
    }

    private void processFrontFingerprintDpcenterEvent(KeyEvent event) {
        if (event.getAction() == 1) {
            if (this.mDpadCenterDown) {
                this.mDpadCenterDown = false;
                if (this.mHomeDownAfterDpCenter) {
                    this.mHomeDownAfterDpCenter = false;
                    Slog.w("BaseMiuiPhoneWindowManager", "After dpcenter & home down, ignore tap fingerprint");
                    return;
                }
            }
            if (isDeviceProvisioned() && !this.mMiuiKeyguardDelegate.isShowingAndNotHidden() && event.getEventTime() - event.getDownTime() < 300) {
                if (this.mMiuiKeyShortcutManager.mSingleKeyUse) {
                    injectEvent(event, 4, -1);
                } else if (-1 == this.mMiuiKeyShortcutManager.mFingerPrintNavCenterAction) {
                    this.mHandler.post(new Runnable() { // from class: com.android.server.policy.MiuiPhoneWindowManager.3
                        @Override // java.lang.Runnable
                        public void run() {
                            MiuiPhoneWindowManager.this.bringUpActionChooseDlg();
                        }
                    });
                } else if (1 == this.mMiuiKeyShortcutManager.mFingerPrintNavCenterAction) {
                    injectEvent(event, 3, -1);
                } else {
                    int i = this.mMiuiKeyShortcutManager.mFingerPrintNavCenterAction;
                }
            }
        } else if (event.getAction() == 0) {
            this.mDpadCenterDown = true;
        }
    }

    private void processFrontFingerprintDprightEvent(KeyEvent event) {
        Slog.d("BaseMiuiPhoneWindowManager", "processFrontFingerprintDprightEvent");
    }

    private void processBackFingerprintDpcenterEvent(KeyEvent event, boolean isScreenOn) {
        if (event.getAction() == 0 && isDeviceProvisioned()) {
            boolean z = false;
            if (isScreenOn) {
                this.mPowerManager.userActivity(SystemClock.uptimeMillis(), false);
            } else if (SUPPORT_POWERFP) {
                boolean lockout = false;
                IFingerprintService iFingerprintService = this.mFingerprintService;
                if (iFingerprintService != null) {
                    try {
                        if (iFingerprintService.getLockoutModeForUser(0, 0) != 0) {
                            z = true;
                        }
                        lockout = z;
                    } catch (RemoteException e) {
                        throw e.rethrowFromSystemServer();
                    }
                }
                if (hasEnrolledFingerpirntForAuthentication() != 11 && lockout) {
                    Slog.d("BaseMiuiPhoneWindowManager", "fingerprint lockoutmode: " + lockout);
                    this.interceptPowerKeyTimeByDpadCenter = SystemClock.uptimeMillis() + 300;
                    this.mPowerManager.wakeUp(SystemClock.uptimeMillis(), "miui.policy:FINGERPRINT_DPAD_CENTER");
                }
            } else {
                this.mPowerManager.wakeUp(SystemClock.uptimeMillis(), "miui.policy:FINGERPRINT_DPAD_CENTER");
            }
        }
    }

    @Override // com.android.server.policy.BaseMiuiPhoneWindowManager
    protected int processFingerprintNavigationEvent(KeyEvent event, boolean isScreenOn) {
        int keyCode = event.getKeyCode();
        switch (keyCode) {
            case 22:
                processFrontFingerprintDprightEvent(event);
                return 0;
            case 23:
                if (this.mFrontFingerprintSensor) {
                    if (!this.mSupportTapFingerprintSensorToHome) {
                        return 0;
                    }
                    processFrontFingerprintDpcenterEvent(event);
                    return 0;
                }
                processBackFingerprintDpcenterEvent(event, isScreenOn);
                return 0;
            default:
                return 0;
        }
    }

    @Override // com.android.server.policy.BaseMiuiPhoneWindowManager
    protected boolean interceptPowerKeyByFingerPrintKey() {
        return this.interceptPowerKeyTimeByDpadCenter > SystemClock.uptimeMillis();
    }

    protected int hasEnrolledFingerpirntForAuthentication() {
        return this.mBiometricManager.canAuthenticate();
    }

    protected int getFingerprintLockoutMode(Object bm) {
        try {
            if (this.mGetFpLockoutModeMethod == null) {
                this.mGetFpLockoutModeMethod = bm.getClass().getDeclaredMethod("getLockoutMode", new Class[0]);
            }
            int res = ((Integer) this.mGetFpLockoutModeMethod.invoke(bm, new Object[0])).intValue();
            return res;
        } catch (Exception e) {
            Slog.e("BaseMiuiPhoneWindowManager", "getFingerprintLockoutMode function exception");
            e.printStackTrace();
            return 0;
        }
    }

    public void bringUpActionChooseDlg() {
        if (this.mFpNavCenterActionChooseDialog != null) {
            return;
        }
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() { // from class: com.android.server.policy.MiuiPhoneWindowManager.4
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialog, int which) {
                int value;
                if (which == -1) {
                    value = 1;
                } else {
                    value = 0;
                }
                Settings.System.putIntForUser(MiuiPhoneWindowManager.this.mContext.getContentResolver(), "fingerprint_nav_center_action", value, -2);
                if (MiuiPhoneWindowManager.this.mFpNavCenterActionChooseDialog != null) {
                    MiuiPhoneWindowManager.this.mFpNavCenterActionChooseDialog.dismiss();
                    MiuiPhoneWindowManager.this.mFpNavCenterActionChooseDialog = null;
                }
            }
        };
        AlertDialog create = new AlertDialog.Builder(this.mContext).setTitle(286196208).setMessage(286196205).setPositiveButton(286196207, listener).setNegativeButton(286196206, listener).setCancelable(false).create();
        this.mFpNavCenterActionChooseDialog = create;
        WindowManager.LayoutParams lp = create.getWindow().getAttributes();
        lp.type = 2008;
        this.mFpNavCenterActionChooseDialog.getWindow().setAttributes(lp);
        this.mFpNavCenterActionChooseDialog.show();
    }

    private void injectEvent(KeyEvent event, int injectKeyCode, int deviceId) {
        long now = SystemClock.uptimeMillis();
        KeyEvent homeDown = new KeyEvent(now, now, 0, injectKeyCode, 0, 0, deviceId, 0, event.getFlags(), event.getSource());
        KeyEvent homeUp = new KeyEvent(now, now, 1, injectKeyCode, 0, 0, deviceId, 0, event.getFlags(), event.getSource());
        InputManager.getInstance().injectInputEvent(homeDown, 0);
        InputManager.getInstance().injectInputEvent(homeUp, 0);
    }

    private boolean hideStatusBar(int flag, int sys) {
        if ((flag & 1024) != 0 || (sys & 4) != 0) {
            return true;
        }
        return false;
    }

    private boolean hideNavBar(int flag, int sys) {
        if ((sys & 2) != 0 || (sys & 6144) != 0) {
            return true;
        }
        return false;
    }

    private int getExtraWindowSystemUiVis(WindowManagerPolicy.WindowState transWin) {
        WindowManager.LayoutParams attrs;
        int vis = 0;
        if (transWin != null && (attrs = (WindowManager.LayoutParams) ReflectionUtils.callPrivateMethod(transWin, "getAttrs", new Object[0])) != null) {
            vis = 0 | attrs.extraFlags;
            if (attrs.type == 3) {
                vis |= 1;
            }
        }
        return MiuiStatusBarManager.getSystemUIVisibilityFlags(vis);
    }

    private boolean drawsSystemBarBackground(WindowManagerPolicy.WindowState win) {
        WindowManager.LayoutParams attrs;
        if (win == null || (attrs = (WindowManager.LayoutParams) ReflectionUtils.callPrivateMethod(win, "getAttrs", new Object[0])) == null || (attrs.flags & Integer.MIN_VALUE) != 0) {
            return true;
        }
        return false;
    }

    private boolean forcesDrawStatusBarBackground(WindowManagerPolicy.WindowState win) {
        WindowManager.LayoutParams attrs;
        if (win == null || (attrs = (WindowManager.LayoutParams) ReflectionUtils.callPrivateMethod(win, "getAttrs", new Object[0])) == null || (attrs.privateFlags & 131072) != 0) {
            return true;
        }
        return false;
    }

    public void registerMIUIWatermarkCallback(MIUIWatermarkCallback callback) {
        this.mPhoneWindowCallback = callback;
    }
}
