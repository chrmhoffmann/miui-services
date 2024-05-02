package com.android.server.power;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.display.DisplayManagerInternal;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.PowerManager;
import android.os.PowerManagerInternal;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.WorkSource;
import android.provider.Settings;
import android.util.Slog;
import android.util.SparseArray;
import android.util.TimeUtils;
import android.widget.Toast;
import com.android.internal.app.IUidStateChangeCallback;
import com.android.server.LocalServices;
import com.android.server.UiThread;
import com.android.server.policy.BaseMiuiPhoneWindowManager;
import com.android.server.policy.DisplayTurnoverManager;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.power.PowerManagerService;
import com.miui.base.MiuiStubRegistry;
import com.miui.server.greeze.FreezeUtils;
import com.miui.server.greeze.GreezeManagerService;
import com.miui.whetstone.PowerKeeperPolicy;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import miui.greeze.IGreezeManager;
import miui.os.DeviceFeature;
import miui.util.FeatureParser;
/* loaded from: classes.dex */
public class PowerManagerServiceImpl extends PowerManagerServiceStub {
    private static final long DECREASE_BRIGHTNESS_WAIT_TIME = 180000;
    private static final int DEFAULT_SCREEN_OFF_TIMEOUT_SECONDARY_DISPLAY = 15000;
    private static final int DEFAULT_SUBSCREEN_SUPER_POWER_SAVE_MODE = 0;
    private static final int DIRTY_SCREEN_BRIGHTNESS_BOOST = 2048;
    private static final int DIRTY_SETTINGS = 32;
    private static final int FLAG_BOOST_BRIGHTNESS = 2;
    private static final int FLAG_KEEP_SECONDARY_ALWAYS_ON = 1;
    private static final int FLAG_SCREEN_PROJECTION = 0;
    private static final String GAME_MODE_URI = "gb_boosting";
    private static final String GTB_BRIGHTNESS_ADJUST = "gb_brightness";
    private static final String KEY_DEVICE_LOCK = "com.xiaomi.system.devicelock.locked";
    private static final String KEY_SECONDARY_SCREEN_ENABLE = "subscreen_switch";
    private static final int PICK_UP_SENSOR_TYPE = 33171036;
    private static final String POWER_SAVE_MODE_OPEN = "POWER_SAVE_MODE_OPEN";
    private static final String REASON_CHANGE_SECONDARY_STATE_CAMERA_CALL = "CAMERA_CALL";
    private static final String SCREEN_OFF_TIMEOUT_SECONDARY_DISPLAY = "subscreen_display_time";
    private static final String SUBSCREEN_SUPER_POWER_SAVE_MODE = "subscreen_super_power_save_mode";
    private static final int SUB_DISPLAY_GROUP_ID = 1;
    private static final String TAG = "PowerManagerServiceImpl";
    private static final long VIDEO_DECREASE_BRIGHTNESS_WAIT_TIME = 180000;
    private static final String VIDEO_IDLE_STATUS = "video_idle_status";
    private static final String VIDEO_IDLE_TIMEOUT = "video_idle_timeout";
    private static final int WAKE_LOCK_SCREEN_BRIGHT = 2;
    private static final int WAKE_LOCK_SCREEN_DIM = 4;
    private static final int WAKE_LOCK_STAY_AWAKE = 32;
    private boolean isDeviceLock;
    private boolean mAdaptiveSleepEnabled;
    private boolean mAlwaysWakeUp;
    private boolean mBootCompleted;
    private boolean mBrightnessBoostForSoftLightInProgress;
    private Context mContext;
    private int mDriveMode;
    private Handler mHandler;
    private boolean mHangUpEnabled;
    private boolean mIsPowerSaveModeEnabled;
    private boolean mIsUserSetupComplete;
    private long mLastRequestDimmingTime;
    private String mLastSecondaryDisplayGoToSleepReason;
    private long mLastSecondaryDisplayGoToSleepTime;
    private long mLastSecondaryDisplayUserActivityTime;
    private String mLastSecondaryDisplayWakeUpReason;
    private long mLastSecondaryDisplayWakeUpTime;
    private Object mLock;
    private NotifierInjector mNotifierInjector;
    private boolean mPendingSetDirtyDueToRequestDimming;
    private boolean mPendingStopBrightnessBoost;
    private boolean mPickUpGestureWakeUpEnabled;
    private Sensor mPickUpSensor;
    private boolean mPickUpSensorEnabled;
    private WindowManagerPolicy mPolicy;
    private SparseArray<PowerGroup> mPowerGroups;
    private PowerKeeperPolicy mPowerKeeperPolicy;
    private PowerManager mPowerManager;
    private PowerManagerService mPowerManagerService;
    private int mPreWakefulness;
    private ContentResolver mResolver;
    private boolean mScreenProjectionEnabled;
    private boolean mSecondaryDisplayEnabled;
    private long mSecondaryDisplayScreenOffTimeout;
    private SensorManager mSensorManager;
    private SettingsObserver mSettingsObserver;
    private boolean mSituatedDimmingDueToSynergy;
    private boolean mSupportAdaptiveSleep;
    private boolean mSynergyModeEnable;
    private boolean mSystemReady;
    private UidStateHelper mUidStateHelper;
    private int mUserActivitySecondaryDisplaySummary;
    private boolean mVideoIdleStatus;
    private ArrayList<PowerManagerService.WakeLock> mWakeLocks;
    private int mWakefulness;
    private static final boolean DEBUG = SystemProperties.getBoolean("miui.power.dbg", false);
    private static final boolean mBrightnessDecreaseEnable = FeatureParser.getBoolean("support_low_power_mode_decrease_brightness", false);
    private static final float mBrightnessDecreaseRatio = FeatureParser.getFloat("brightness_decrease_ratio", 0.9f).floatValue();
    private static final boolean SUPPORT_GAME_DIM = FeatureParser.getBoolean("support_game_dim", false);
    private static final boolean SUPPORT_VIDEO_IDLE_DIM = FeatureParser.getBoolean("support_video_idle_dim", false);
    private long mUserActivityTimeoutOverrideFromMirrorManager = -1;
    private HashMap<IBinder, ClientDeathCallback> mClientDeathCallbacks = new HashMap<>();
    private boolean mGameModeEnable = false;
    private boolean mGameBrightnessEnable = false;
    private long mVideoIdleBrightWaitTime = 180000;
    private int mSubScreenSuperPowerSaveMode = 0;
    private IGreezeManager mIGreezeManager = null;
    private IUidStateChangeCallback sUidStateChangeCallback = new IUidStateChangeCallback.Stub() { // from class: com.android.server.power.PowerManagerServiceImpl.1
        public void onUidStateChange(int uid, int state) {
            if (state > 0 && PowerManagerServiceImpl.this.getScreenWakeLockHoldByUid(uid) > 0) {
                PowerManagerServiceImpl.this.restoreScreenWakeLockDisabledState(uid);
            }
        }
    };
    private final SensorEventListener mPickUpSensorListener = new SensorEventListener() { // from class: com.android.server.power.PowerManagerServiceImpl.3
        @Override // android.hardware.SensorEventListener
        public void onSensorChanged(SensorEvent sensorEvent) {
            if (sensorEvent.values[0] == 1.0f) {
                PowerManagerServiceImpl.this.updateUserActivityLocked();
            }
        }

        @Override // android.hardware.SensorEventListener
        public void onAccuracyChanged(Sensor sensor, int i) {
        }
    };

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<PowerManagerServiceImpl> {

        /* compiled from: PowerManagerServiceImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final PowerManagerServiceImpl INSTANCE = new PowerManagerServiceImpl();
        }

        public PowerManagerServiceImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public PowerManagerServiceImpl provideNewInstance() {
            return new PowerManagerServiceImpl();
        }
    }

    public void init(PowerManagerService powerManagerService, ArrayList<PowerManagerService.WakeLock> allWakeLocks, Object lock, Context context, Looper looper, SparseArray<PowerGroup> powerGroup) {
        this.mContext = context;
        this.mPowerManagerService = powerManagerService;
        this.mWakeLocks = allWakeLocks;
        this.mLock = lock;
        this.mHandler = new PowerManagerStubHandler(looper);
        this.mUidStateHelper = UidStateHelper.get();
        this.mPowerKeeperPolicy = PowerKeeperPolicy.getInstance();
        this.mResolver = this.mContext.getContentResolver();
        this.mSettingsObserver = new SettingsObserver(this.mHandler);
        this.mSystemReady = true;
        this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
        this.mNotifierInjector = new NotifierInjector(this.mContext);
        this.mPowerGroups = powerGroup;
        this.mPolicy = (WindowManagerPolicy) LocalServices.getService(WindowManagerPolicy.class);
        SensorManager sensorManager = (SensorManager) this.mContext.getSystemService("sensor");
        this.mSensorManager = sensorManager;
        this.mPickUpSensor = sensorManager.getDefaultSensor(PICK_UP_SENSOR_TYPE);
        systemReady();
    }

    private void systemReady() {
        this.mSupportAdaptiveSleep = this.mContext.getResources().getBoolean(17891343);
        resetScreenProjectionSettingsLocked();
        loadSettingsLocked();
        this.mResolver.registerContentObserver(Settings.Secure.getUriFor("screen_project_in_screening"), false, this.mSettingsObserver, -1);
        this.mResolver.registerContentObserver(Settings.Secure.getUriFor("screen_project_hang_up_on"), false, this.mSettingsObserver, -1);
        this.mResolver.registerContentObserver(Settings.Secure.getUriFor("synergy_mode"), false, this.mSettingsObserver, -1);
        this.mResolver.registerContentObserver(Settings.System.getUriFor("accelerometer_rotation"), false, this.mSettingsObserver, -1);
        this.mResolver.registerContentObserver(Settings.Global.getUriFor(KEY_DEVICE_LOCK), false, this.mSettingsObserver, -1);
        this.mResolver.registerContentObserver(Settings.System.getUriFor(SCREEN_OFF_TIMEOUT_SECONDARY_DISPLAY), false, this.mSettingsObserver, -1);
        this.mResolver.registerContentObserver(Settings.System.getUriFor(SUBSCREEN_SUPER_POWER_SAVE_MODE), false, this.mSettingsObserver, -1);
        this.mResolver.registerContentObserver(Settings.Secure.getUriFor(GAME_MODE_URI), false, this.mSettingsObserver, -1);
        this.mResolver.registerContentObserver(Settings.Secure.getUriFor(GTB_BRIGHTNESS_ADJUST), false, this.mSettingsObserver, -1);
        this.mResolver.registerContentObserver(Settings.System.getUriFor("drive_mode_drive_mode"), false, this.mSettingsObserver, -1);
        this.mResolver.registerContentObserver(Settings.System.getUriFor("pick_up_gesture_wakeup_mode"), false, this.mSettingsObserver, -1);
        this.mResolver.registerContentObserver(Settings.Secure.getUriFor("adaptive_sleep"), false, this.mSettingsObserver, -1);
        this.mResolver.registerContentObserver(Settings.System.getUriFor(POWER_SAVE_MODE_OPEN), false, this.mSettingsObserver, -1);
        if (SUPPORT_VIDEO_IDLE_DIM) {
            this.mResolver.registerContentObserver(Settings.Secure.getUriFor(VIDEO_IDLE_STATUS), false, this.mSettingsObserver, -1);
            this.mResolver.registerContentObserver(Settings.Secure.getUriFor(VIDEO_IDLE_TIMEOUT), false, this.mSettingsObserver, -1);
        }
    }

    private void loadSettingsLocked() {
        updateDriveModeLocked();
        updateSecondaryDisplayScreenOffTimeoutLocked();
        updateUserSetupCompleteLocked();
        updatePickUpGestureWakeUpModeLocked();
        updateAdaptiveSleepLocked();
        updatePowerSaveMode();
    }

    private void updateDriveModeLocked() {
        this.mDriveMode = Settings.System.getIntForUser(this.mResolver, "drive_mode_drive_mode", 0, -2);
    }

    private void updatePickUpGestureWakeUpModeLocked() {
        boolean z = false;
        if (Settings.System.getIntForUser(this.mResolver, "pick_up_gesture_wakeup_mode", 0, -2) == 1) {
            z = true;
        }
        this.mPickUpGestureWakeUpEnabled = z;
    }

    private void updateAdaptiveSleepLocked() {
        boolean z = false;
        if (Settings.Secure.getIntForUser(this.mResolver, "adaptive_sleep", 0, -2) == 1) {
            z = true;
        }
        this.mAdaptiveSleepEnabled = z;
    }

    public void setPickUpSensorEnable(boolean isDimming) {
        if (this.mSupportAdaptiveSleep && this.mAdaptiveSleepEnabled && this.mPickUpGestureWakeUpEnabled && !this.mPolicy.isKeyguardShowingAndNotOccluded() && isDimming && !this.mPickUpSensorEnabled) {
            this.mPickUpSensorEnabled = true;
            this.mSensorManager.registerListener(this.mPickUpSensorListener, this.mPickUpSensor, 3);
        } else if (this.mPickUpSensorEnabled) {
            this.mPickUpSensorEnabled = false;
            this.mSensorManager.unregisterListener(this.mPickUpSensorListener);
        }
    }

    private int[] getRealOwners(PowerManagerService.WakeLock wakeLock) {
        int[] iArr = new int[0];
        if (wakeLock.mWorkSource == null) {
            return new int[]{wakeLock.mOwnerUid};
        }
        int N = wakeLock.mWorkSource.size();
        int[] realOwners = new int[N];
        for (int i = 0; i < N; i++) {
            realOwners[i] = wakeLock.mWorkSource.get(i);
        }
        return realOwners;
    }

    public int getPartialWakeLockHoldByUid(int uid) {
        int wakeLockNum = 0;
        synchronized (this.mLock) {
            Iterator<PowerManagerService.WakeLock> it = this.mWakeLocks.iterator();
            while (it.hasNext()) {
                PowerManagerService.WakeLock wakeLock = it.next();
                WorkSource ws = wakeLock.mWorkSource;
                if (ws != null || wakeLock.mOwnerUid == uid) {
                    if (ws == null || ws.get(0) == uid) {
                        int wakeLockType = wakeLock.mFlags & 65535;
                        if (wakeLockType == 1) {
                            wakeLockNum++;
                        }
                    }
                }
            }
        }
        return wakeLockNum;
    }

    public int getScreenWakeLockHoldByUid(int uid) {
        int wakeLockNum = 0;
        synchronized (this.mLock) {
            Iterator<PowerManagerService.WakeLock> it = this.mWakeLocks.iterator();
            while (it.hasNext()) {
                PowerManagerService.WakeLock wakeLock = it.next();
                WorkSource ws = wakeLock.mWorkSource;
                if (ws != null || wakeLock.mOwnerUid == uid) {
                    if (ws == null || ws.get(0) == uid) {
                        int wakeLockType = wakeLock.mFlags & 65535;
                        switch (wakeLockType) {
                            case 6:
                            case 10:
                            case 26:
                                wakeLockNum++;
                                break;
                        }
                    }
                }
            }
        }
        return wakeLockNum;
    }

    public boolean isBackgroundScreenWakelock(PowerManagerService.WakeLock wakeLock) {
        boolean foreground = false;
        int[] realOwners = getRealOwners(wakeLock);
        for (int realOwner : realOwners) {
            UidStateHelper uidStateHelper = this.mUidStateHelper;
            if (uidStateHelper != null) {
                foreground |= uidStateHelper.isUidForeground(realOwner, true);
            }
        }
        return !foreground;
    }

    private IGreezeManager getGreeze() {
        if (this.mIGreezeManager == null) {
            this.mIGreezeManager = IGreezeManager.Stub.asInterface(ServiceManager.getService(GreezeManagerService.SERVICE_NAME));
        }
        return this.mIGreezeManager;
    }

    public boolean isWakelockDisabledByPolicy(PowerManagerService.WakeLock wakeLock) {
        boolean disabled = false;
        int[] realOwners = getRealOwners(wakeLock);
        for (int realOwner : realOwners) {
            PowerKeeperPolicy powerKeeperPolicy = this.mPowerKeeperPolicy;
            if (powerKeeperPolicy != null && powerKeeperPolicy.isWakelockDisabledByPolicy(wakeLock.mTag, realOwner) && (wakeLock.mOwnerUid != 1000 || wakeLock.mTag == null || !wakeLock.mTag.contains("vibrator"))) {
                disabled = true;
                Slog.d(TAG, "wakeLock:[" + wakeLock.toString() + "] is disabled by policy");
                break;
            }
        }
        if (!disabled) {
            boolean disabled2 = checkUidFrozen(wakeLock);
            return disabled2;
        }
        return disabled;
    }

    private boolean setWakeLockDisabledStateLocked(PowerManagerService.WakeLock wakeLock, boolean disabled) {
        if (wakeLock.mDisabled == disabled) {
            return false;
        }
        wakeLock.mDisabled = disabled;
        return true;
    }

    private boolean checkUidFrozen(PowerManagerService.WakeLock wakeLock) {
        boolean disabled = false;
        int[] realOwners = getRealOwners(wakeLock);
        for (int realOwner : realOwners) {
            try {
                if (UserHandle.isApp(realOwner) && getGreeze() != null && getGreeze().isUidFrozen(realOwner)) {
                    disabled = true;
                    if (!DEBUG) {
                        break;
                    }
                    Slog.d(TAG, "wakeLock:[" + wakeLock.toString() + "] is disabled uid =" + realOwner + " pid=" + wakeLock.mOwnerPid + " pids:" + FreezeUtils.getFrozenPids().toString());
                    break;
                }
            } catch (Exception e) {
                Slog.e(TAG, "checkUidFrozen err:", e);
            }
        }
        return disabled;
    }

    void updateWakeLockDisabledStateLocked(PowerManagerService.WakeLock wakeLock, boolean notify) {
        boolean changed = false;
        switch (wakeLock.mFlags & 65535) {
            case 1:
                boolean disabled = isWakelockDisabledByPolicy(wakeLock);
                changed = setWakeLockDisabledStateLocked(wakeLock, disabled);
                if (changed) {
                    this.mPowerManagerService.setWakeLockDirtyLocked();
                    break;
                }
                break;
        }
        if (notify && changed && wakeLock.mDisabled) {
            this.mPowerManagerService.notifyWakeLockReleasedLocked(wakeLock);
            this.mPowerManagerService.updatePowerStateLocked();
        }
    }

    public void updateAllPartialWakeLockDisableState() {
        synchronized (this.mLock) {
            boolean changed = false;
            Iterator<PowerManagerService.WakeLock> it = this.mWakeLocks.iterator();
            while (it.hasNext()) {
                PowerManagerService.WakeLock wakeLock = it.next();
                switch (wakeLock.mFlags & 65535) {
                    case 1:
                        boolean disabled = isWakelockDisabledByPolicy(wakeLock);
                        changed |= setWakeLockDisabledStateLocked(wakeLock, disabled);
                        break;
                }
            }
            if (changed) {
                this.mPowerManagerService.setWakeLockDirtyLocked();
                this.mPowerManagerService.updatePowerStateLocked();
            }
        }
    }

    void updateAllScreenWakeLockDisabledStateLocked() {
        Iterator<PowerManagerService.WakeLock> it = this.mWakeLocks.iterator();
        while (it.hasNext()) {
            PowerManagerService.WakeLock wakeLock = it.next();
            switch (wakeLock.mFlags & 65535) {
                case 6:
                case 10:
                case 26:
                    if (wakeLock.mOwnerUid != 1000) {
                        Slog.w(TAG, "screen wakeLock:[" + wakeLock.toString() + "] not by window manager");
                        break;
                    } else {
                        break;
                    }
            }
        }
        if (0 != 0) {
            this.mPowerManagerService.setWakeLockDirtyLocked();
        }
    }

    void restoreScreenWakeLockDisabledState(int uid) {
        synchronized (this.mLock) {
            boolean changed = false;
            Iterator<PowerManagerService.WakeLock> it = this.mWakeLocks.iterator();
            while (it.hasNext()) {
                PowerManagerService.WakeLock wakeLock = it.next();
                switch (wakeLock.mFlags & 65535) {
                    case 6:
                    case 10:
                    case 26:
                        int[] realOwners = getRealOwners(wakeLock);
                        int length = realOwners.length;
                        int i = 0;
                        while (true) {
                            if (i < length) {
                                int realOwner = realOwners[i];
                                if (realOwner != uid || !wakeLock.mDisabled) {
                                    i++;
                                } else {
                                    changed |= setWakeLockDisabledStateLocked(wakeLock, false);
                                    this.mPowerManagerService.notifyWakeLockAcquiredLocked(wakeLock);
                                    Slog.d(TAG, "screen wakeLock:[" + wakeLock.toString() + "] enabled");
                                    break;
                                }
                            }
                        }
                        break;
                }
            }
            if (changed) {
                this.mPowerManagerService.setWakeLockDirtyLocked();
                this.mPowerManagerService.updatePowerStateLocked();
            }
        }
    }

    public void setUidPartialWakeLockDisabledState(int uid, String tag, boolean disabled) {
        if (tag == null && !UserHandle.isApp(uid)) {
            throw new IllegalArgumentException("can not disable all wakelock for uid " + uid);
        }
        synchronized (this.mLock) {
            Iterator<PowerManagerService.WakeLock> it = this.mWakeLocks.iterator();
            while (it.hasNext()) {
                PowerManagerService.WakeLock wakeLock = it.next();
                boolean changed = false;
                switch (wakeLock.mFlags & 65535) {
                    case 1:
                        int[] realOwners = getRealOwners(wakeLock);
                        for (int realOwner : realOwners) {
                            if (realOwner == uid && (tag == null || tag.equals(wakeLock.mTag))) {
                                changed = setWakeLockDisabledStateLocked(wakeLock, disabled);
                                break;
                            }
                        }
                        break;
                }
                if (changed) {
                    if (wakeLock.mDisabled) {
                        Slog.d(TAG, "set partial wakelock disabled:[" + wakeLock.toString() + "]");
                        this.mPowerManagerService.notifyWakeLockReleasedLocked(wakeLock);
                    } else {
                        Slog.d(TAG, "set partial wakelock enabled:[" + wakeLock.toString() + "]");
                        this.mPowerManagerService.notifyWakeLockAcquiredLocked(wakeLock);
                    }
                    this.mPowerManagerService.setWakeLockDirtyLocked();
                    this.mPowerManagerService.updatePowerStateLocked();
                }
            }
        }
    }

    public boolean isShutdownOrRebootPermitted(boolean shutdown, boolean confirm, String reason, boolean wait) {
        if (shutdown && this.isDeviceLock) {
            Handler h = UiThread.getHandler();
            if (h != null) {
                h.post(new Runnable() { // from class: com.android.server.power.PowerManagerServiceImpl.2
                    @Override // java.lang.Runnable
                    public void run() {
                        Toast.makeText(PowerManagerServiceImpl.this.mContext, 286196346, 1).show();
                    }
                });
                return false;
            }
            return false;
        }
        return true;
    }

    private void resetScreenProjectionSettingsLocked() {
        Settings.Secure.putInt(this.mContext.getContentResolver(), "screen_project_in_screening", 0);
        setHangUpModeLocked(false);
        Settings.Secure.putInt(this.mContext.getContentResolver(), "synergy_mode", 0);
    }

    private void setHangUpModeLocked(boolean enable) {
        Settings.Secure.putInt(this.mContext.getContentResolver(), "screen_project_hang_up_on", enable ? 1 : 0);
    }

    public boolean hangUpNoUpdateLocked(boolean hangUp) {
        if (!this.mBootCompleted || !this.mSystemReady) {
            return false;
        }
        if (hangUp && this.mWakefulness == 4) {
            return false;
        }
        if (!hangUp && this.mWakefulness != 4) {
            return false;
        }
        Slog.d(TAG, "hangUpNoUpdateLocked: " + (hangUp ? "enter" : "exit") + " hang up mode...");
        long eventTime = SystemClock.uptimeMillis();
        if (!hangUp) {
            updateUserActivityLocked();
        }
        this.mPowerManagerService.setWakefulnessLocked(0, hangUp ? 4 : 1, eventTime, 1000, 0, 0, (String) null, "hangup");
        this.mNotifierInjector.onWakefulnessInHangUp(hangUp, this.mWakefulness);
        return true;
    }

    public void setBootPhase(int phase) {
        if (phase == 1000) {
            this.mBootCompleted = true;
        }
    }

    public boolean shouldRequestDisplayHangupLocked() {
        return (this.mScreenProjectionEnabled || this.mSynergyModeEnable) && this.mWakefulness == 4;
    }

    public boolean isInHangUpState() {
        return (this.mScreenProjectionEnabled || this.mSynergyModeEnable) && this.mWakefulness == 4;
    }

    public boolean exitHangupWakefulnessLocked() {
        return this.mPreWakefulness == 4 && this.mWakefulness != 4;
    }

    protected void finishWakefulnessChangeIfNeededLocked() {
        if ((!this.mScreenProjectionEnabled && !this.mHangUpEnabled && !this.mSynergyModeEnable) || this.mWakefulness != 4) {
            this.mNotifierInjector.onWakefulnessInHangUp(false, this.mWakefulness);
        }
    }

    public boolean shouldEnterHangupLocked() {
        return this.mScreenProjectionEnabled || this.mSynergyModeEnable;
    }

    protected long shouldOverrideUserActivityTimeoutLocked(long timeout) {
        if (this.mSynergyModeEnable) {
            long j = this.mUserActivityTimeoutOverrideFromMirrorManager;
            if (j >= 0) {
                return Math.min(timeout, j);
            }
            return timeout;
        }
        return timeout;
    }

    private boolean updateScreenProjectionLocked() {
        boolean screenProjectingEnabled = Settings.Secure.getInt(this.mResolver, "screen_project_in_screening", 0) == 1;
        boolean hangUpEnabled = Settings.Secure.getInt(this.mResolver, "screen_project_hang_up_on", 0) == 1;
        boolean synergyModeEnable = Settings.Secure.getInt(this.mResolver, "synergy_mode", 0) == 1;
        if (screenProjectingEnabled == this.mScreenProjectionEnabled && hangUpEnabled == this.mHangUpEnabled && synergyModeEnable == this.mSynergyModeEnable) {
            return false;
        }
        if (!hangUpEnabled && this.mHangUpEnabled) {
            this.mHangUpEnabled = false;
            return true;
        }
        this.mScreenProjectionEnabled = screenProjectingEnabled;
        this.mHangUpEnabled = hangUpEnabled;
        this.mSynergyModeEnable = synergyModeEnable;
        if (screenProjectingEnabled && hangUpEnabled) {
            hangUpNoUpdateLocked(true);
        } else if (!screenProjectingEnabled && !synergyModeEnable) {
            hangUpNoUpdateLocked(false);
        }
        if (this.mHangUpEnabled) {
            setHangUpModeLocked(false);
        }
        return true;
    }

    protected int adjustWakeLockDueToHangUpLocked(int wakeLockSummary) {
        if ((wakeLockSummary & 38) != 0 && (this.mScreenProjectionEnabled || this.mSynergyModeEnable)) {
            return wakeLockSummary & (-39);
        }
        if (isGameDimEnable() || isVideoDimEnable()) {
            return (wakeLockSummary & (-3)) | 4;
        }
        return wakeLockSummary;
    }

    public void updateWakefulnessLocked(int wakefulness) {
        this.mPreWakefulness = this.mWakefulness;
        this.mWakefulness = wakefulness;
    }

    protected void requestDimmingRightNowInternal(long timeMillis) {
        synchronized (this.mLock) {
            if (!this.mSituatedDimmingDueToSynergy) {
                if (DEBUG) {
                    Slog.d(TAG, "requestDimmingRightNowInternal: timeout: " + timeMillis);
                }
                updateUserActivityLocked();
                this.mLastRequestDimmingTime = SystemClock.uptimeMillis();
                this.mSituatedDimmingDueToSynergy = true;
                this.mPendingSetDirtyDueToRequestDimming = true;
                this.mUserActivityTimeoutOverrideFromMirrorManager = timeMillis;
                this.mPowerManagerService.updatePowerStateLocked();
            }
        }
    }

    public void clearRequestDimmingParamsLocked() {
        if (this.mSituatedDimmingDueToSynergy) {
            this.mSituatedDimmingDueToSynergy = false;
            this.mUserActivityTimeoutOverrideFromMirrorManager = -1L;
            this.mLastRequestDimmingTime = -1L;
            this.mPendingSetDirtyDueToRequestDimming = false;
        }
    }

    protected long updateNextDimTimeoutIfNeededLocked(long nextTimeout, long lastUserActivityTime) {
        if (this.mSynergyModeEnable && this.mSituatedDimmingDueToSynergy && this.mUserActivityTimeoutOverrideFromMirrorManager >= 0 && this.mLastRequestDimmingTime > 0) {
            long nextTimeout2 = this.mLastRequestDimmingTime;
            return nextTimeout2;
        } else if (isGameDimEnable()) {
            long nextTimeout3 = lastUserActivityTime + 180000;
            return nextTimeout3;
        } else if (isVideoDimEnable()) {
            long nextTimeout4 = lastUserActivityTime + this.mVideoIdleBrightWaitTime;
            return nextTimeout4;
        } else {
            return nextTimeout;
        }
    }

    protected void updateUserActivityLocked() {
        this.mPowerManager.userActivity(SystemClock.uptimeMillis(), 0, 0);
    }

    protected int adjustDirtyIfNeededLocked(int mDirty) {
        if (this.mSynergyModeEnable && this.mSituatedDimmingDueToSynergy && this.mPendingSetDirtyDueToRequestDimming) {
            mDirty |= 32;
            this.mPendingSetDirtyDueToRequestDimming = false;
        }
        if (this.mPendingStopBrightnessBoost) {
            return mDirty | 2048;
        }
        return mDirty;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public void handleSettingsChangedLocked(Uri uri) {
        char c;
        String lastPathSegment = uri.getLastPathSegment();
        switch (lastPathSegment.hashCode()) {
            case -1848054051:
                if (lastPathSegment.equals(SCREEN_OFF_TIMEOUT_SECONDARY_DISPLAY)) {
                    c = 6;
                    break;
                }
                c = 65535;
                break;
            case -1568446215:
                if (lastPathSegment.equals(VIDEO_IDLE_STATUS)) {
                    c = 11;
                    break;
                }
                c = 65535;
                break;
            case -1475410818:
                if (lastPathSegment.equals(KEY_DEVICE_LOCK)) {
                    c = 5;
                    break;
                }
                c = 65535;
                break;
            case -1438362181:
                if (lastPathSegment.equals("synergy_mode")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case -1187891250:
                if (lastPathSegment.equals("adaptive_sleep")) {
                    c = 14;
                    break;
                }
                c = 65535;
                break;
            case -974080081:
                if (lastPathSegment.equals("user_setup_complete")) {
                    c = 7;
                    break;
                }
                c = 65535;
                break;
            case -901388401:
                if (lastPathSegment.equals("screen_project_hang_up_on")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case -889914649:
                if (lastPathSegment.equals("pick_up_gesture_wakeup_mode")) {
                    c = '\r';
                    break;
                }
                c = 65535;
                break;
            case -793979590:
                if (lastPathSegment.equals(VIDEO_IDLE_TIMEOUT)) {
                    c = '\f';
                    break;
                }
                c = 65535;
                break;
            case -445165996:
                if (lastPathSegment.equals(SUBSCREEN_SUPER_POWER_SAVE_MODE)) {
                    c = '\b';
                    break;
                }
                c = 65535;
                break;
            case -377350077:
                if (lastPathSegment.equals(GAME_MODE_URI)) {
                    c = '\t';
                    break;
                }
                c = 65535;
                break;
            case 81099118:
                if (lastPathSegment.equals("accelerometer_rotation")) {
                    c = 3;
                    break;
                }
                c = 65535;
                break;
            case 1345941045:
                if (lastPathSegment.equals(GTB_BRIGHTNESS_ADJUST)) {
                    c = '\n';
                    break;
                }
                c = 65535;
                break;
            case 1455506878:
                if (lastPathSegment.equals(POWER_SAVE_MODE_OPEN)) {
                    c = 15;
                    break;
                }
                c = 65535;
                break;
            case 1497178783:
                if (lastPathSegment.equals("drive_mode_drive_mode")) {
                    c = 4;
                    break;
                }
                c = 65535;
                break;
            case 2030129845:
                if (lastPathSegment.equals("screen_project_in_screening")) {
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
            case 1:
            case 2:
                if (updateScreenProjectionLocked()) {
                    this.mPowerManagerService.updatePowerStateLocked();
                    return;
                }
                return;
            case 3:
                this.mNotifierInjector.updateAccelerometerRotationLocked();
                return;
            case 4:
                updateDriveModeLocked();
                return;
            case 5:
                updateDeviceLockState();
                return;
            case 6:
                updateSecondaryDisplayScreenOffTimeoutLocked();
                return;
            case 7:
                updateUserSetupCompleteLocked();
                return;
            case '\b':
                updateSubScreenSuperPowerSaveMode();
                return;
            case '\t':
            case '\n':
                updateGameDimState();
                this.mPowerManagerService.updatePowerStateLocked();
                return;
            case 11:
            case '\f':
                updateVideoDimState();
                this.mPowerManagerService.updatePowerStateLocked();
                return;
            case '\r':
                updatePickUpGestureWakeUpModeLocked();
                return;
            case 14:
                updateAdaptiveSleepLocked();
                return;
            case 15:
                updatePowerSaveMode();
                this.mPowerManagerService.updatePowerStateLocked();
                return;
            default:
                return;
        }
    }

    private void updateGameDimState() {
        boolean z = true;
        this.mGameBrightnessEnable = Settings.Secure.getIntForUser(this.mResolver, GTB_BRIGHTNESS_ADJUST, 1, -2) == 1;
        if (Settings.Secure.getIntForUser(this.mResolver, GAME_MODE_URI, 0, -2) != 1) {
            z = false;
        }
        this.mGameModeEnable = z;
    }

    private boolean isGameDimEnable() {
        return this.mGameBrightnessEnable && this.mGameModeEnable && SUPPORT_GAME_DIM;
    }

    protected void sendBroadcastRestoreBrightnessIfNeededLocked() {
        if (isGameDimEnable() || isVideoDimEnable()) {
            this.mNotifierInjector.sendBroadcastRestoreBrightness();
        }
    }

    private boolean isVideoDimEnable() {
        return SUPPORT_VIDEO_IDLE_DIM && this.mVideoIdleStatus;
    }

    private void updateVideoDimState() {
        boolean z = false;
        if (Settings.Secure.getIntForUser(this.mResolver, VIDEO_IDLE_STATUS, 0, -2) == 1) {
            z = true;
        }
        this.mVideoIdleStatus = z;
        this.mVideoIdleBrightWaitTime = Settings.Secure.getLongForUser(this.mResolver, VIDEO_IDLE_TIMEOUT, 180000L, -2);
    }

    private void updateDeviceLockState() {
        boolean z = false;
        if (Settings.Global.getInt(this.mResolver, KEY_DEVICE_LOCK, 0) != 0) {
            z = true;
        }
        this.isDeviceLock = z;
    }

    private void requestHangUpWhenScreenProjectInternal(IBinder token, boolean hangup) {
        synchronized (this.mLock) {
            setDeathCallbackLocked(token, 0, hangup);
            setHangUpModeLocked(hangup);
        }
    }

    private void setDeathCallbackLocked(IBinder token, int flag, boolean register) {
        synchronized (this.mLock) {
            if (register) {
                registerDeathCallbackLocked(token, flag);
            } else {
                unregisterDeathCallbackLocked(token);
            }
        }
    }

    protected void registerDeathCallbackLocked(IBinder token, int flag) {
        if (this.mClientDeathCallbacks.containsKey(token)) {
            Slog.d(TAG, "Client token " + token + " has already registered.");
        } else {
            this.mClientDeathCallbacks.put(token, new ClientDeathCallback(token, flag));
        }
    }

    protected void registerDeathCallbackLocked(IBinder token) {
        if (this.mClientDeathCallbacks.containsKey(token)) {
            Slog.d(TAG, "Client token " + token + " has already registered.");
        } else {
            this.mClientDeathCallbacks.put(token, new ClientDeathCallback(this, token));
        }
    }

    protected void unregisterDeathCallbackLocked(IBinder token) {
        ClientDeathCallback deathCallback;
        if (token != null && (deathCallback = this.mClientDeathCallbacks.remove(token)) != null) {
            token.unlinkToDeath(deathCallback, 0);
        }
    }

    private void doDieLocked() {
        clearRequestDimmingParamsLocked();
        resetScreenProjectionSettingsLocked();
    }

    public void doDieLocked(int flag) {
        if ((flag & 1) != 0 && this.mAlwaysWakeUp) {
            this.mAlwaysWakeUp = false;
            this.mPowerManagerService.updatePowerStateLocked();
        } else if (flag == 0) {
            clearRequestDimmingParamsLocked();
            resetScreenProjectionSettingsLocked();
        } else if (flag == 2) {
            stopBoostingBrightnessLocked(1000);
        }
    }

    protected void requestDimmingRightNow(long timeMillis) {
        long ident = Binder.clearCallingIdentity();
        if (timeMillis > 0) {
            try {
                requestDimmingRightNowInternal(timeMillis);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    private void requestHangUpWhenScreenProject(IBinder token, boolean hangup) {
        long ident = Binder.clearCallingIdentity();
        if (token != null) {
            try {
                requestHangUpWhenScreenProjectInternal(token, hangup);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    private void boostScreenBrightnessInternal(IBinder token, boolean enabled, int uid) {
        synchronized (this.mLock) {
            if (this.mBrightnessBoostForSoftLightInProgress != enabled) {
                setDeathCallbackLocked(token, 2, enabled);
                this.mBrightnessBoostForSoftLightInProgress = enabled;
                if (enabled) {
                    Slog.d(TAG, "Start boosting screen brightness: uid = " + uid);
                    this.mPowerManager.boostScreenBrightness(SystemClock.uptimeMillis());
                } else {
                    stopBoostingBrightnessLocked(uid);
                }
            }
        }
    }

    private void stopBoostingBrightnessLocked(int uid) {
        Slog.d(TAG, "stop boosting screen brightness: uid = " + uid);
        if (this.mBrightnessBoostForSoftLightInProgress) {
            this.mBrightnessBoostForSoftLightInProgress = false;
        }
        this.mPendingStopBrightnessBoost = true;
        this.mPowerManagerService.updatePowerStateLocked();
    }

    /* loaded from: classes.dex */
    private class PowerManagerStubHandler extends Handler {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public PowerManagerStubHandler(Looper looper) {
            super(looper);
            PowerManagerServiceImpl.this = r1;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
        }
    }

    /* loaded from: classes.dex */
    public class ClientDeathCallback implements IBinder.DeathRecipient {
        private int mFlag;
        private IBinder mToken;

        public ClientDeathCallback(PowerManagerServiceImpl this$0, IBinder token) {
            this(token, 0);
        }

        public ClientDeathCallback(IBinder token, int flag) {
            PowerManagerServiceImpl.this = this$0;
            this.mToken = token;
            this.mFlag = flag;
            try {
                token.linkToDeath(this, 0);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            Slog.d(PowerManagerServiceImpl.TAG, "binderDied: flag: " + this.mFlag);
            synchronized (PowerManagerServiceImpl.this.mLock) {
                PowerManagerServiceImpl.this.doDieLocked(this.mFlag);
            }
        }
    }

    /* loaded from: classes.dex */
    public final class SettingsObserver extends ContentObserver {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public SettingsObserver(Handler handler) {
            super(handler);
            PowerManagerServiceImpl.this = r1;
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            synchronized (PowerManagerServiceImpl.this.mLock) {
                PowerManagerServiceImpl.this.handleSettingsChangedLocked(uri);
            }
        }
    }

    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) {
        try {
            switch (code) {
                case 16777207:
                    data.enforceInterface("android.os.IPowerManager");
                    IBinder token3 = data.readStrongBinder();
                    boolean enabled = data.readBoolean();
                    boostScreenBrightness(token3, enabled);
                    return true;
                case 16777208:
                    data.enforceInterface("android.os.IPowerManager");
                    IBinder token2 = data.readStrongBinder();
                    int flag2 = data.readInt();
                    clearWakeUpFlags(token2, flag2);
                    return true;
                case 16777209:
                    data.enforceInterface("android.os.IPowerManager");
                    reply.writeInt(getSecondaryDisplayWakefulnessLocked());
                    return true;
                case 16777210:
                    data.enforceInterface("android.os.IPowerManager");
                    IBinder token1 = data.readStrongBinder();
                    long wakeUpTime1 = data.readLong();
                    int flag = data.readInt();
                    String wakeUpReason1 = data.readString();
                    wakeUpSecondaryDisplay(token1, wakeUpTime1, flag, wakeUpReason1);
                    return true;
                case DisplayTurnoverManager.CODE_TURN_OFF_SUB_DISPLAY /* 16777211 */:
                    data.enforceInterface("android.os.IPowerManager");
                    long goToSleepTime = data.readLong();
                    String goToSleepReason = data.readString();
                    goToSleepSecondaryDisplay(goToSleepTime, goToSleepReason);
                    return true;
                case DisplayTurnoverManager.CODE_TURN_ON_SUB_DISPLAY /* 16777212 */:
                    data.enforceInterface("android.os.IPowerManager");
                    long wakeUpTime = data.readLong();
                    String wakeUpReason = data.readString();
                    wakeUpSecondaryDisplay(wakeUpTime, wakeUpReason);
                    return true;
                case 16777213:
                    data.enforceInterface("android.os.IPowerManager");
                    IBinder token = data.readStrongBinder();
                    boolean hangup = data.readBoolean();
                    requestHangUpWhenScreenProject(token, hangup);
                    return true;
                case 16777214:
                    data.enforceInterface("android.os.IPowerManager");
                    long duration = data.readLong();
                    requestDimmingRightNow(duration);
                    return true;
                default:
                    return false;
            }
        } catch (RuntimeException e) {
            throw e;
        }
    }

    private void boostScreenBrightness(IBinder token, boolean enabled) {
        if (token == null) {
            throw new IllegalArgumentException("token must not be null");
        }
        int uid = Binder.getCallingUid();
        long ident = Binder.clearCallingIdentity();
        try {
            boostScreenBrightnessInternal(token, enabled, uid);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private void wakeUpSecondaryDisplay(long eventTime, String reason) {
        wakeUpSecondaryDisplay(null, eventTime, 0, reason);
    }

    private void wakeUpSecondaryDisplay(IBinder token, long eventTime, int flag, String reason) {
        if (!DeviceFeature.IS_SUBSCREEN_DEVICE) {
            return;
        }
        if (eventTime > SystemClock.uptimeMillis()) {
            throw new IllegalArgumentException("event time must not be in the future");
        }
        int uid = Binder.getCallingUid();
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mLock) {
                if (eventTime >= this.mLastSecondaryDisplayWakeUpTime) {
                    if ((getSecondaryDisplayWakefulnessLocked() != 1 || REASON_CHANGE_SECONDARY_STATE_CAMERA_CALL.equals(reason)) && this.mSystemReady) {
                        this.mLastSecondaryDisplayWakeUpTime = eventTime;
                        this.mLastSecondaryDisplayUserActivityTime = eventTime;
                        this.mLastSecondaryDisplayWakeUpReason = reason;
                        updateAlwaysWakeUpIfNeededLocked(token, flag);
                        Slog.d(TAG, "Waking up secondary display from: " + uid + ", reason: " + reason);
                        this.mPowerManagerService.wakeUpSecondaryDisplay(this.mPowerGroups.get(1), eventTime, 0, "sub-display", uid);
                        return;
                    }
                }
                Slog.d(TAG, "wakeUpSecondaryDisplay: return");
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private void goToSleepSecondaryDisplay(long eventTime, String reason) {
        if (!DeviceFeature.IS_SUBSCREEN_DEVICE) {
            return;
        }
        if (eventTime > SystemClock.uptimeMillis()) {
            throw new IllegalArgumentException("event time must not be in the future");
        }
        int uid = Binder.getCallingUid();
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mLock) {
                if (eventTime >= this.mLastSecondaryDisplayWakeUpTime && PowerManagerInternal.isInteractive(getSecondaryDisplayWakefulnessLocked()) && this.mSystemReady && this.mBootCompleted) {
                    if (shouldGoToSleepWhileAlwaysOn(reason)) {
                        Slog.d(TAG, "Ignore " + reason + " to apply secondary display sleep while mAlwaysWakeUp");
                        return;
                    }
                    this.mLastSecondaryDisplayGoToSleepTime = eventTime;
                    this.mLastSecondaryDisplayGoToSleepReason = reason;
                    this.mAlwaysWakeUp = false;
                    Slog.d(TAG, "Going to sleep secondary display from: " + uid + ", reason: " + reason);
                    this.mPowerManagerService.goToSleepSecondaryDisplay(this.mPowerGroups.get(1), eventTime, 0, uid);
                    return;
                }
                Slog.d(TAG, "goToSleepSecondaryDisplay: return");
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private void clearWakeUpFlags(IBinder token, int flag) {
        long ident = Binder.clearCallingIdentity();
        try {
            clearWakeUpFlagsInternal(token, flag);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private void updateAlwaysWakeUpIfNeededLocked(IBinder token, int flag) {
        if (token != null && (flag & 1) != 0) {
            this.mAlwaysWakeUp = true;
            setDeathCallbackLocked(token, flag, true);
        }
    }

    public boolean isSubScreenSuperPowerSaveModeOpen() {
        if (DeviceFeature.IS_SUBSCREEN_DEVICE && this.mSubScreenSuperPowerSaveMode != 0) {
            Slog.i(TAG, "wilderness subscreen_super_power_save_mode open");
            return true;
        }
        return false;
    }

    private void updateSubScreenSuperPowerSaveMode() {
        this.mSubScreenSuperPowerSaveMode = Settings.System.getIntForUser(this.mResolver, SUBSCREEN_SUPER_POWER_SAVE_MODE, 0, -2);
    }

    private void updateSecondaryDisplayScreenOffTimeoutLocked() {
        this.mSecondaryDisplayScreenOffTimeout = Settings.System.getInt(this.mResolver, SCREEN_OFF_TIMEOUT_SECONDARY_DISPLAY, DEFAULT_SCREEN_OFF_TIMEOUT_SECONDARY_DISPLAY);
    }

    private void updateUserSetupCompleteLocked() {
        boolean z = false;
        if (Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "user_setup_complete", 0, -2) != 0) {
            z = true;
        }
        this.mIsUserSetupComplete = z;
    }

    protected int getSecondaryDisplayWakefulnessLocked() {
        return this.mPowerGroups.get(1).getWakefulnessLocked();
    }

    private boolean shouldGoToSleepWhileAlwaysOn(String reason) {
        return this.mAlwaysWakeUp && !REASON_CHANGE_SECONDARY_STATE_CAMERA_CALL.equals(reason);
    }

    private void clearWakeUpFlagsInternal(IBinder token, int flag) {
        synchronized (this.mLock) {
            if (DeviceFeature.IS_SUBSCREEN_DEVICE && (flag & 1) != 0) {
                this.mAlwaysWakeUp = false;
                unregisterDeathCallbackLocked(token);
                long now = SystemClock.uptimeMillis();
                this.mPowerManagerService.userActivitySecondaryDisplay(2, now, 0, 0, 1000);
            }
        }
    }

    private void updatePowerSaveMode() {
        boolean z = false;
        boolean isPowerSaveModeEnabled = Settings.System.getIntForUser(this.mResolver, POWER_SAVE_MODE_OPEN, 0, -2) != 0;
        if (mBrightnessDecreaseEnable && isPowerSaveModeEnabled) {
            z = true;
        }
        this.mIsPowerSaveModeEnabled = z;
        Slog.i(TAG, "updatePowerSaveMode: mIsPowerSaveModeEnabled: " + this.mIsPowerSaveModeEnabled);
    }

    public void updateLowPowerModeIfNeeded(DisplayManagerInternal.DisplayPowerRequest displayPowerRequest) {
        if (!this.mIsPowerSaveModeEnabled) {
            return;
        }
        displayPowerRequest.lowPowerMode = true;
        displayPowerRequest.screenLowPowerBrightnessFactor = mBrightnessDecreaseRatio;
    }

    public long adjustScreenOffTimeoutIfNeededLocked(PowerGroup powerGroup, long timeout) {
        if (DeviceFeature.IS_SUBSCREEN_DEVICE && powerGroup.getGroupId() == 1) {
            return this.mSecondaryDisplayScreenOffTimeout;
        }
        return timeout;
    }

    public boolean shouldAlwaysWakeUpSecondaryDisplay() {
        return this.mAlwaysWakeUp;
    }

    public long getDimDurationExtraTime(long extraTimeMillis) {
        if (this.mDriveMode != 1 || extraTimeMillis <= 0) {
            return 0L;
        }
        return extraTimeMillis;
    }

    public boolean isBrightnessBoostForSoftLightInProgress() {
        return this.mBrightnessBoostForSoftLightInProgress;
    }

    public boolean isPendingStopBrightnessBoost() {
        return this.mPendingStopBrightnessBoost;
    }

    public void resetBoostBrightnessIfNeededLocked() {
        if (this.mPendingStopBrightnessBoost) {
            this.mPendingStopBrightnessBoost = false;
        }
    }

    public void setWakeUpDetail(String detail) {
        BaseMiuiPhoneWindowManager baseMiuiPhoneWindowManager = this.mPolicy;
        if (baseMiuiPhoneWindowManager instanceof BaseMiuiPhoneWindowManager) {
            baseMiuiPhoneWindowManager.setWakeUpDetail(detail);
        }
    }

    public void dumpLocal(PrintWriter pw) {
        pw.println("mScreenProjectionEnabled: " + this.mScreenProjectionEnabled);
        pw.println("mHangUpEnabled: " + this.mHangUpEnabled);
        pw.println("mSynergyModeEnable: " + this.mSynergyModeEnable);
        pw.println("mSituatedDimmingDueToSynergy: " + this.mSituatedDimmingDueToSynergy);
        pw.println("mLastRequestDimmingTime: " + this.mLastRequestDimmingTime);
        pw.println("mPendingSetDirtyDueToRequestDimming: " + this.mPendingSetDirtyDueToRequestDimming);
        pw.println("mUserActivityTimeoutOverrideFromMirrorManager: " + this.mUserActivityTimeoutOverrideFromMirrorManager);
        pw.println("mIsPowerSaveModeEnabled: " + this.mIsPowerSaveModeEnabled);
        this.mNotifierInjector.dump(pw);
        if (DeviceFeature.IS_SUBSCREEN_DEVICE) {
            pw.println();
            pw.println("Secondary display power state: ");
            long now = SystemClock.uptimeMillis();
            long lastUserActivityDuration = now - this.mLastSecondaryDisplayUserActivityTime;
            long lastGoToSleepDuration = now - this.mLastSecondaryDisplayGoToSleepTime;
            long lastWakeUpDuration = now - this.mLastSecondaryDisplayWakeUpTime;
            pw.println("    mLastSecondaryDisplayUserActivityTime: " + TimeUtils.formatDuration(lastUserActivityDuration) + " ago.");
            pw.println("    mLastSecondaryDisplayWakeUpTime: " + TimeUtils.formatDuration(lastWakeUpDuration) + " ago.");
            pw.println("    mLastSecondaryDisplayGoToSleepTime: " + TimeUtils.formatDuration(lastGoToSleepDuration) + " ago.");
            pw.println("    mUserActivitySecondaryDisplaySummary: 0x" + Integer.toHexString(this.mUserActivitySecondaryDisplaySummary));
            pw.println("    mLastSecondaryDisplayWakeUpReason: " + this.mLastSecondaryDisplayWakeUpReason);
            pw.println("    mLastSecondaryDisplayGoToSleepReason: " + this.mLastSecondaryDisplayGoToSleepReason);
            pw.println("    mSecondaryDisplayScreenOffTimeout: " + this.mSecondaryDisplayScreenOffTimeout + " ms.");
            pw.println("    mAlwaysWakeUp: " + this.mAlwaysWakeUp);
        }
    }
}
