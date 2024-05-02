package com.android.server.display;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.SystemSensorManager;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.util.Slog;
import android.util.TimeUtils;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.server.content.SyncManagerStubImpl;
import java.io.File;
import java.io.PrintWriter;
import miui.os.DeviceFeature;
/* loaded from: classes.dex */
public class SunlightController {
    private static boolean DEBUG = false;
    private static final String ENABLE_SENSOR_REASON_DEFAULT = "sunlight_mode";
    private static final String ENABLE_SENSOR_REASON_NOTIFICATION = "prepare_for_notifaction";
    private static final int MSG_INITIALIZE = 4;
    private static final int MSG_SCREEN_HANG_UP_RECEIVE = 3;
    private static final int MSG_SCREEN_ON_OFF_RECEIVE = 2;
    private static final int MSG_UPDATE_SUNLIGHT_MODE = 1;
    private static final int RESET_USER_DISABLE_DURATION = 300000;
    private static final int SUNLIGHT_AMBIENT_LIGHT_HORIZON = 10000;
    private static final int SUNLIGHT_LIGHT_SENSOR_RATE = 250;
    private static final String TAG = "SunlightController";
    private static final int THRESHOLD_ENTER_SUNLIGHT_DURATION = 5000;
    private static final int THRESHOLD_EXIT_SUNLIGHT_DURATION = 2000;
    private static final int THRESHOLD_SUNLIGHT_BRIGHTNESS;
    private static final int THRESHOLD_SUNLIGHT_LUX = 12000;
    private AmbientLightRingBuffer mAmbientLightRingBuffer;
    private boolean mAutoBrightnessSettingsEnable;
    private boolean mBelowThresholdBrightness;
    private Callback mCallback;
    private Context mContext;
    private float mCurrentAmbientLux;
    private int mCurrentScreenBrightnessSettings;
    private int mDisplayId;
    private SunlightModeHandler mHandler;
    private float mLastObservedLux;
    private long mLastObservedLuxTime;
    private long mLastScreenOffTime;
    private boolean mLastSunlightSettingsEnable;
    private Sensor mLightSensor;
    private boolean mLowPowerState;
    private NotificationHelper mNotificationHelper;
    private PowerManager mPowerManager;
    private boolean mPreparedForNotification;
    private int mScreenBrightnessDefaultSettings;
    private boolean mScreenIsHangUp;
    private SensorManager mSensorManager;
    private SettingsObserver mSettingsObserver;
    private boolean mSunlightModeActive;
    private boolean mSunlightModeDisabledByUser;
    private boolean mSunlightModeEnable;
    private long mSunlightSensorEnableTime;
    private boolean mSunlightSensorEnabled;
    private String mSunlightSensorEnabledReason;
    private boolean mSunlightSettingsEnable;
    private boolean mScreenOn = true;
    private final SensorEventListener mLightSensorListener = new SensorEventListener() { // from class: com.android.server.display.SunlightController.1
        @Override // android.hardware.SensorEventListener
        public void onSensorChanged(SensorEvent event) {
            if (SunlightController.this.mSunlightSensorEnabled) {
                long time = SystemClock.uptimeMillis();
                float lux = event.values[0];
                SunlightController.this.handleLightSensorEvent(time, lux);
            }
        }

        @Override // android.hardware.SensorEventListener
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    /* loaded from: classes.dex */
    public interface Callback {
        void notifySunlightStateChange(boolean z);
    }

    static {
        THRESHOLD_SUNLIGHT_BRIGHTNESS = DeviceFeature.BACKLIGHT_BIT == 11 ? 500 : 1000;
    }

    public SunlightController(Context context, Callback callback, Looper looper, int displayId) {
        if (displayId != 0) {
            throw new IllegalArgumentException("Sunlight mode can only be used on the default display.");
        }
        this.mContext = context;
        this.mCallback = callback;
        this.mDisplayId = displayId;
        this.mHandler = new SunlightModeHandler(looper);
        this.mNotificationHelper = new NotificationHelper();
        SystemSensorManager systemSensorManager = new SystemSensorManager(this.mContext, this.mHandler.getLooper());
        this.mSensorManager = systemSensorManager;
        this.mLightSensor = systemSensorManager.getDefaultSensor(5);
        this.mAmbientLightRingBuffer = new AmbientLightRingBuffer(250L, 10000);
        this.mSettingsObserver = new SettingsObserver(this.mHandler);
        PowerManager powerManager = (PowerManager) this.mContext.getSystemService("power");
        this.mPowerManager = powerManager;
        this.mScreenBrightnessDefaultSettings = powerManager.getDefaultScreenBrightnessSetting();
        this.mHandler.sendEmptyMessage(4);
    }

    public void register() {
        registerSettingsObserver();
        registerScreenOnReceiver();
        registerHangUpReceiver();
        updateSunlightModeSettings();
    }

    private void registerSettingsObserver() {
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(ENABLE_SENSOR_REASON_DEFAULT), false, this.mSettingsObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("screen_brightness_mode"), false, this.mSettingsObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("screen_brightness"), false, this.mSettingsObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("low_power_level_state"), false, this.mSettingsObserver, -1);
    }

    private void registerScreenOnReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.SCREEN_ON");
        filter.addAction("android.intent.action.SCREEN_OFF");
        filter.setPriority(1000);
        this.mContext.registerReceiver(new ScreenOnReceiver(), filter);
    }

    private void registerHangUpReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("miui.intent.action.HANG_UP_CHANGED");
        this.mContext.registerReceiver(new ScreenHangUpReceiver(), filter);
    }

    public void updateSunlightModeSettings() {
        boolean z = false;
        boolean sunlightSettingsChanged = Settings.System.getIntForUser(this.mContext.getContentResolver(), ENABLE_SENSOR_REASON_DEFAULT, 0, -2) != 0;
        if (Settings.System.getIntForUser(this.mContext.getContentResolver(), "screen_brightness_mode", 0, -2) != 0) {
            z = true;
        }
        this.mAutoBrightnessSettingsEnable = z;
        this.mCurrentScreenBrightnessSettings = Settings.System.getIntForUser(this.mContext.getContentResolver(), "screen_brightness", this.mScreenBrightnessDefaultSettings, -2);
        if (!sunlightSettingsChanged && this.mSunlightSettingsEnable) {
            resetUserDisableTemporaryData();
        }
        this.mSunlightSettingsEnable = sunlightSettingsChanged;
        if (DEBUG) {
            Slog.d(TAG, "updateSunlightModeSettings: mSunlightSettingsEnable=" + this.mSunlightSettingsEnable + ", mAutoBrightnessSettingsEnable=" + this.mAutoBrightnessSettingsEnable + ", mCurrentScreenBrightnessSettings=" + this.mCurrentScreenBrightnessSettings);
        }
        if (!updateSunlightModeCondition()) {
            shouldPrepareToNotify();
        }
    }

    private void shouldPrepareToNotify() {
        boolean enable = !this.mSunlightSettingsEnable && !this.mAutoBrightnessSettingsEnable && this.mBelowThresholdBrightness && this.mScreenOn && !this.mScreenIsHangUp && !this.mNotificationHelper.mHasReachedLimitTimes;
        setLightSensorEnabledForNotification(enable);
    }

    private void setLightSensorEnabledForNotification(boolean enable) {
        if (enable != this.mPreparedForNotification) {
            this.mPreparedForNotification = enable;
            setLightSensorEnabled(enable, ENABLE_SENSOR_REASON_NOTIFICATION);
        }
    }

    private boolean updateSunlightModeCondition() {
        boolean z = true;
        this.mBelowThresholdBrightness = this.mCurrentScreenBrightnessSettings < THRESHOLD_SUNLIGHT_BRIGHTNESS;
        if (this.mAutoBrightnessSettingsEnable || !this.mSunlightSettingsEnable || !this.mScreenOn || this.mSunlightModeDisabledByUser || this.mScreenIsHangUp) {
            z = false;
        }
        boolean enable = z;
        if (DEBUG) {
            Slog.d(TAG, "updateSunlightModeCondition: mSunlightModeEnable: " + this.mSunlightModeEnable + ", enable: " + enable + ", mScreenOn: " + this.mScreenOn + ", mSunlightModeDisabledByUser: " + this.mSunlightModeDisabledByUser + ", mBelowThresholdBrightness: " + this.mBelowThresholdBrightness + ", mScreenIsHangUp" + this.mScreenIsHangUp);
        }
        if (enable != this.mSunlightModeEnable) {
            this.mSunlightModeEnable = enable;
            this.mPreparedForNotification = false;
            setLightSensorEnabled(enable);
        }
        return enable;
    }

    private void updateNotificationState() {
        if (this.mCurrentAmbientLux >= 12000.0f && !this.mNotificationHelper.showNotificationIfNecessary()) {
            setLightSensorEnabledForNotification(false);
        }
    }

    public void updateAmbientLightSensor(Sensor sensor) {
        if (this.mLightSensor != sensor) {
            this.mLightSensor = sensor;
            if (this.mSunlightSensorEnabled) {
                this.mSensorManager.unregisterListener(this.mLightSensorListener);
                this.mSensorManager.registerListener(this.mLightSensorListener, this.mLightSensor, 0, this.mHandler);
            }
        }
    }

    private boolean setLightSensorEnabled(boolean enabled) {
        return setLightSensorEnabled(enabled, ENABLE_SENSOR_REASON_DEFAULT);
    }

    private boolean setLightSensorEnabled(boolean enabled, String reason) {
        if (DEBUG) {
            Slog.d(TAG, "setLightSensorEnabled: " + enabled + ", enabled reason: " + reason);
        }
        if (enabled) {
            if (reason.equals(this.mSunlightSensorEnabledReason)) {
                this.mSunlightSensorEnabledReason = reason;
            }
            if (this.mSunlightSensorEnabled) {
                return false;
            }
            this.mSunlightSensorEnabled = true;
            this.mSensorManager.registerListener(this.mLightSensorListener, this.mLightSensor, 0, this.mHandler);
            this.mSunlightSensorEnableTime = SystemClock.uptimeMillis();
            return true;
        } else if (!this.mSunlightSensorEnabled) {
            return false;
        } else {
            this.mSunlightSensorEnabled = false;
            this.mPreparedForNotification = false;
            this.mSunlightSensorEnableTime = SystemClock.uptimeMillis();
            this.mHandler.removeMessages(1);
            this.mSensorManager.unregisterListener(this.mLightSensorListener);
            setSunLightModeActive(false);
            return true;
        }
    }

    public void handleLightSensorEvent(long time, float lux) {
        if (DEBUG) {
            Slog.d(TAG, "handleLightSensorEvent: lux = " + lux);
        }
        this.mHandler.removeMessages(1);
        applyLightSensorMeasurement(time, lux);
        updateAmbientLux(time);
    }

    public void updateAmbientLux() {
        long time = SystemClock.uptimeMillis();
        this.mAmbientLightRingBuffer.prune(time - 10000);
        updateAmbientLux(time);
    }

    private void updateAmbientLux(long time) {
        this.mCurrentAmbientLux = this.mAmbientLightRingBuffer.calculateAmbientLux(time, 10000L);
        long nextEnterSunlightModeTime = nextEnterSunlightModeTransition(time);
        long nextExitSunlightModeTime = nextExitSunlightModeTransition(time);
        if (this.mPreparedForNotification && !this.mSunlightModeEnable) {
            if (!this.mLowPowerState) {
                updateNotificationState();
                return;
            }
            return;
        }
        float f = this.mCurrentAmbientLux;
        if (f >= 12000.0f && nextEnterSunlightModeTime <= time && this.mBelowThresholdBrightness && this.mSunlightModeEnable) {
            setSunLightModeActive(true);
        } else if (f < 12000.0f && nextExitSunlightModeTime <= time) {
            setSunLightModeActive(false);
        }
        long nextTransitionTime = Math.min(nextEnterSunlightModeTime, nextExitSunlightModeTime);
        long nextTransitionTime2 = nextTransitionTime > time ? nextTransitionTime : 1000 + time;
        if (DEBUG) {
            Slog.d(TAG, "updateAmbientLux: Scheduling lux update for " + TimeUtils.formatUptime(nextTransitionTime2));
        }
        this.mHandler.sendEmptyMessageAtTime(1, nextTransitionTime2);
    }

    private void setSunLightModeActive(boolean active) {
        if (active != this.mSunlightModeActive) {
            Slog.d(TAG, "setSunLightModeActive: active: " + active);
            this.mSunlightModeActive = active;
            this.mCallback.notifySunlightStateChange(active);
        }
    }

    private void applyLightSensorMeasurement(long time, float lux) {
        this.mAmbientLightRingBuffer.prune(time - 10000);
        this.mAmbientLightRingBuffer.push(time, lux);
        this.mLastObservedLux = lux;
        this.mLastObservedLuxTime = time;
    }

    private long nextEnterSunlightModeTransition(long time) {
        int N = this.mAmbientLightRingBuffer.size();
        long earliestValidTime = time;
        for (int i = N - 1; i >= 0 && this.mAmbientLightRingBuffer.getLux(i) > 12000.0f; i--) {
            earliestValidTime = this.mAmbientLightRingBuffer.getTime(i);
        }
        return 5000 + earliestValidTime;
    }

    private long nextExitSunlightModeTransition(long time) {
        int N = this.mAmbientLightRingBuffer.size();
        long earliestValidTime = time;
        for (int i = N - 1; i >= 0 && this.mAmbientLightRingBuffer.getLux(i) < 12000.0f; i--) {
            earliestValidTime = this.mAmbientLightRingBuffer.getTime(i);
        }
        return 2000 + earliestValidTime;
    }

    public void setSunlightModeDisabledByUserTemporary() {
        if (!this.mSunlightModeDisabledByUser) {
            Slog.d(TAG, "Disable sunlight mode temporarily due to user slide bar.");
            this.mSunlightModeDisabledByUser = true;
            updateSunlightModeCondition();
        }
    }

    public boolean isSunlightModeDisabledByUser() {
        return this.mSunlightModeDisabledByUser;
    }

    /* loaded from: classes.dex */
    public class SunlightModeHandler extends Handler {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public SunlightModeHandler(Looper looper) {
            super(looper);
            SunlightController.this = r1;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    SunlightController.this.updateAmbientLux();
                    return;
                case 2:
                    SunlightController.this.updateScreenState(((Boolean) msg.obj).booleanValue());
                    return;
                case 3:
                    SunlightController.this.updateHangUpState(((Boolean) msg.obj).booleanValue());
                    return;
                case 4:
                    SunlightController.this.register();
                    return;
                default:
                    return;
            }
        }
    }

    public void updateHangUpState(boolean screenIsHangUp) {
        if (screenIsHangUp != this.mScreenIsHangUp) {
            this.mScreenIsHangUp = screenIsHangUp;
            updateSunlightModeCondition();
        }
    }

    public void updateScreenState(boolean screenOn) {
        if (screenOn != this.mScreenOn) {
            this.mScreenOn = screenOn;
            long currentTime = SystemClock.elapsedRealtime();
            if (this.mScreenOn) {
                if (currentTime - this.mLastScreenOffTime >= 300000) {
                    resetUserDisableTemporaryData();
                }
            } else {
                clearAmbientLightRingBuffer();
                this.mLastScreenOffTime = currentTime;
            }
            updateSunlightModeCondition();
            shouldPrepareToNotify();
        }
    }

    public void updateLowPowerState() {
        int i = 0;
        boolean z = Settings.System.getInt(this.mContext.getContentResolver(), "low_power_level_state", 0) != 0;
        this.mLowPowerState = z;
        if (z) {
            this.mLastSunlightSettingsEnable = this.mSunlightSettingsEnable;
            Settings.System.putInt(this.mContext.getContentResolver(), ENABLE_SENSOR_REASON_DEFAULT, 0);
            return;
        }
        ContentResolver contentResolver = this.mContext.getContentResolver();
        if (this.mLastSunlightSettingsEnable) {
            i = 1;
        }
        Settings.System.putInt(contentResolver, ENABLE_SENSOR_REASON_DEFAULT, i);
    }

    private void resetUserDisableTemporaryData() {
        if (this.mSunlightModeDisabledByUser) {
            Slog.d(TAG, "Reset user slide operation.");
            this.mSunlightModeDisabledByUser = false;
            updateSunlightModeCondition();
        }
    }

    private void clearAmbientLightRingBuffer() {
        this.mAmbientLightRingBuffer.clear();
    }

    /* loaded from: classes.dex */
    public class ScreenOnReceiver extends BroadcastReceiver {
        private ScreenOnReceiver() {
            SunlightController.this = r1;
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            boolean screenOn = false;
            if (intent.getAction().equals("android.intent.action.SCREEN_ON")) {
                screenOn = true;
            }
            if (SunlightController.DEBUG) {
                Slog.d(SunlightController.TAG, "Receive screen on/off broadcast: " + (screenOn ? "screen on." : "screen off."));
            }
            Message msg = SunlightController.this.mHandler.obtainMessage(2, Boolean.valueOf(screenOn));
            msg.sendToTarget();
        }
    }

    /* loaded from: classes.dex */
    public class ScreenHangUpReceiver extends BroadcastReceiver {
        private ScreenHangUpReceiver() {
            SunlightController.this = r1;
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            boolean screenIsHangUp = intent.getBooleanExtra("hang_up_enable", false);
            if (SunlightController.DEBUG) {
                Slog.d(SunlightController.TAG, "Receive screen hang on broadcast.");
            }
            Message msg = SunlightController.this.mHandler.obtainMessage(3, Boolean.valueOf(screenIsHangUp));
            msg.sendToTarget();
        }
    }

    /* loaded from: classes.dex */
    public class SettingsObserver extends ContentObserver {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public SettingsObserver(Handler handler) {
            super(handler);
            SunlightController.this = r1;
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            char c;
            String lastPathSegment = uri.getLastPathSegment();
            switch (lastPathSegment.hashCode()) {
                case -1763718536:
                    if (lastPathSegment.equals(SunlightController.ENABLE_SENSOR_REASON_DEFAULT)) {
                        c = 0;
                        break;
                    }
                    c = 65535;
                    break;
                case -704809039:
                    if (lastPathSegment.equals("low_power_level_state")) {
                        c = 3;
                        break;
                    }
                    c = 65535;
                    break;
                case -693072130:
                    if (lastPathSegment.equals("screen_brightness_mode")) {
                        c = 1;
                        break;
                    }
                    c = 65535;
                    break;
                case 1735689732:
                    if (lastPathSegment.equals("screen_brightness")) {
                        c = 2;
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
                    SunlightController.this.updateSunlightModeSettings();
                    return;
                case 3:
                    SunlightController.this.updateLowPowerState();
                    return;
                default:
                    return;
            }
        }
    }

    /* loaded from: classes.dex */
    public final class NotificationHelper {
        private static final String AUTO_BRIGHTNESS_ACTION = "com.android.settings/com.android.settings.display.BrightnessActivity";
        private static final int DEFAULT_NOTIFICATION_LIMIT = 2;
        private static final String KEY_NOTIFICATION_LAST_SHOW_TIME = "last_show_time";
        private static final String KEY_NOTIFICATION_LIMIT = "shown_times";
        private static final int MINI_INTERVAL_NOTIFICATION = 3600000;
        private static final int NOTIFICATION_ID = 1000;
        private static final String NOTIFICATION_TAG = "SUNLIGHT_NOTIFY";
        private static final String PREFS_SUNLIGHT_FILE = "sunlight_notification.xml";
        private boolean mHasReachedLimitTimes;
        private long mLastShowNotificationTime;
        private Notification mNotification;
        private SharedPreferences mNotificationLimitTimesPrefs;
        private NotificationManager mNotificationManager;

        public NotificationHelper() {
            SunlightController.this = r4;
            this.mNotificationManager = (NotificationManager) r4.mContext.getSystemService("notification");
            SharedPreferences sharedPreferences = r4.mContext.createDeviceProtectedStorageContext().getSharedPreferences(new File(getSystemDir(), PREFS_SUNLIGHT_FILE), 0);
            this.mNotificationLimitTimesPrefs = sharedPreferences;
            this.mLastShowNotificationTime = sharedPreferences.getLong(KEY_NOTIFICATION_LAST_SHOW_TIME, 0L);
        }

        public boolean showNotificationIfNecessary() {
            if (isNotificationActive() || constrainedByInterval() || hasReachedLimitTimes()) {
                return false;
            }
            if (this.mNotification == null) {
                buildNotification();
            }
            updateLastShowTimePrefs();
            this.mNotificationManager.notify(NOTIFICATION_TAG, 1000, this.mNotification);
            return true;
        }

        private boolean isNotificationActive() {
            StatusBarNotification[] activeNotifications = this.mNotificationManager.getActiveNotifications();
            for (StatusBarNotification sbn : activeNotifications) {
                if (1000 == sbn.getId() && NOTIFICATION_TAG.equals(sbn.getTag())) {
                    return true;
                }
            }
            return false;
        }

        private boolean constrainedByInterval() {
            long now = SystemClock.elapsedRealtime();
            long j = this.mLastShowNotificationTime;
            if (j == 0) {
                return false;
            }
            long interval = now - j;
            if (SunlightController.DEBUG) {
                Slog.d(SunlightController.TAG, "constrainedByInterval, interval=" + interval);
            }
            return interval <= SyncManagerStubImpl.SYNC_DELAY_ON_DISALLOW_METERED;
        }

        private boolean hasReachedLimitTimes() {
            int times = this.mNotificationLimitTimesPrefs.getInt(KEY_NOTIFICATION_LIMIT, 2);
            if (times > 0) {
                this.mNotificationLimitTimesPrefs.edit().putInt(KEY_NOTIFICATION_LIMIT, times - 1).commit();
                this.mHasReachedLimitTimes = false;
                return false;
            }
            this.mHasReachedLimitTimes = true;
            return true;
        }

        private void updateLastShowTimePrefs() {
            this.mLastShowNotificationTime = SystemClock.elapsedRealtime();
            this.mNotificationLimitTimesPrefs.edit().putLong(KEY_NOTIFICATION_LAST_SHOW_TIME, this.mLastShowNotificationTime).commit();
        }

        private void buildNotification() {
            PendingIntent pendingIntent = PendingIntent.getActivity(SunlightController.this.mContext, 0, getBrightnessActivityIntent(), 335544320);
            this.mNotification = new Notification.Builder(SunlightController.this.mContext, SystemNotificationChannels.ALERTS).setContentIntent(pendingIntent).setContentTitle(SunlightController.this.mContext.getString(286196604)).setContentText(SunlightController.this.mContext.getString(286196603)).setSmallIcon(17303631).setAutoCancel(true).build();
        }

        private Intent getBrightnessActivityIntent() {
            ComponentName component = ComponentName.unflattenFromString(AUTO_BRIGHTNESS_ACTION);
            if (component == null) {
                return null;
            }
            Intent intent = new Intent("android.intent.action.MAIN");
            intent.setComponent(component);
            intent.setFlags(335544320);
            return intent;
        }

        private File getSystemDir() {
            return new File(Environment.getDataDirectory(), "system");
        }

        public void dump(PrintWriter pw) {
            pw.println("  Sunlight Controller Noticationcation Helper:");
            pw.println("    mHasReachedLimitTimes=" + this.mHasReachedLimitTimes);
            pw.println("    mLastShowNotificationTime=" + this.mLastShowNotificationTime);
        }
    }

    public void dump(PrintWriter pw) {
        pw.println();
        pw.println("Sunlight Controller Configuration:");
        pw.println("  mSunlightSettingsEnable=" + this.mSunlightSettingsEnable);
        pw.println("  mSunlightSensorEnableTime=" + this.mSunlightSensorEnableTime);
        pw.println("  mLastObservedLux=" + this.mLastObservedLux);
        pw.println("  mLastObservedLuxTime=" + this.mLastObservedLuxTime);
        pw.println("  mCurrentAmbientLux=" + this.mCurrentAmbientLux);
        pw.println("  mSunlightSensorEnabled=" + this.mSunlightSensorEnabled);
        if (this.mSunlightSensorEnabled) {
            pw.println("  mSunlightSensorEnabledReason=" + this.mSunlightSensorEnabledReason);
        }
        pw.println("  mBelowThresholdBrightness=" + this.mBelowThresholdBrightness);
        pw.println("  mSunlightModeActive=" + this.mSunlightModeActive);
        pw.println("  mSunlightModeDisabledByUser=" + this.mSunlightModeDisabledByUser);
        pw.println("  mAmbientLightRingBuffer=" + this.mAmbientLightRingBuffer);
        pw.println("  mScreenIsHangUp=" + this.mScreenIsHangUp);
        this.mNotificationHelper.dump(pw);
        DEBUG = DisplayDebugConfig.DEBUG_SC;
    }
}
