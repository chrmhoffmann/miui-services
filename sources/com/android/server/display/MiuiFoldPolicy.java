package com.android.server.display;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings;
import android.server.am.SplitScreenReporter;
import android.telecom.TelecomManager;
import android.util.Slog;
import com.android.server.LocalServices;
import com.android.server.ServiceThread;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.wm.MiuiFreeformPinManagerService;
import java.util.HashSet;
import java.util.Set;
/* loaded from: classes.dex */
public class MiuiFoldPolicy {
    private static final String CLOSE_LID_DISPLAY_SETTING = "close_lid_display_setting";
    private static final Boolean DEBUG = false;
    private static final int FOLD_GESTURE_ANGLE_THRESHOLD = 82;
    private static final String MIUI_OPTIMIZATION = "miui_optimization";
    private static final int MSG_RELEASE_SWIPE_UP_WINDOW = 2;
    private static final int MSG_SET_WAKE_STATE = 4;
    private static final int MSG_SHOW_SWIPE_UP_WINDOW = 1;
    private static final int MSG_UPDATE_DEVICE_STATE = 3;
    private static final int MSG_USER_SWITCH = 5;
    private static final int SETTING_EVENT_INVALID = -1;
    private static final int SETTING_EVENT_KEEP_ON = 2;
    private static final int SETTING_EVENT_SCREEN_OFF = 0;
    private static final int SETTING_EVENT_SMART = 3;
    private static final int SETTING_EVENT_SWIPE_UP = 1;
    public static final String TAG = "MiuiFoldPolicy";
    public static final int TYPE_HINGE_STATE = 33171087;
    private static final int VIRTUAL_CAMERA_BOUNDARY = 100;
    private AudioManager mAudioManager;
    private CameraManager mCameraManager;
    private Context mContext;
    private final int[] mFoldedDeviceStates;
    private Handler mHandler;
    private boolean mIsCtsMode;
    private boolean mIsDeviceProvisioned;
    private boolean mLockScreenAfterFolded;
    private Sensor mMiHingeAngleSensor;
    private boolean mMiHingeAngleSensorEnabled;
    private boolean mNeedOffDueToFoldGesture;
    private boolean mNeedReleaseSwipeUpWindow;
    private boolean mNeedShowSwipeUpWindow;
    private int mScreenStateAfterFold;
    private SensorManager mSensorManager;
    private SettingsObserver mSettingsObserver;
    private SwipeUpWindow mSwipeUpWindow;
    private TelecomManager mTelecomManager;
    private WindowManagerPolicy mWindowManagerPolicy;
    private int mState = -1;
    private final Set<Integer> mOpeningCameraID = new HashSet();
    private int mFoldGestureAngleThreshold = 82;
    private final CameraManager.AvailabilityCallback mAvailabilityCallback = new CameraManager.AvailabilityCallback() { // from class: com.android.server.display.MiuiFoldPolicy.1
        @Override // android.hardware.camera2.CameraManager.AvailabilityCallback
        public void onCameraAvailable(String cameraId) {
            super.onCameraAvailable(cameraId);
            int id = Integer.parseInt(cameraId);
            if (id >= 100) {
                return;
            }
            MiuiFoldPolicy.this.mOpeningCameraID.remove(Integer.valueOf(id));
        }

        @Override // android.hardware.camera2.CameraManager.AvailabilityCallback
        public void onCameraUnavailable(String cameraId) {
            super.onCameraUnavailable(cameraId);
            int id = Integer.parseInt(cameraId);
            if (id >= 100) {
                return;
            }
            MiuiFoldPolicy.this.mOpeningCameraID.add(Integer.valueOf(id));
        }
    };
    private final SensorEventListener mMiHingeAngleSensorListener = new SensorEventListener() { // from class: com.android.server.display.MiuiFoldPolicy.2
        @Override // android.hardware.SensorEventListener
        public void onSensorChanged(SensorEvent event) {
            MiuiFoldPolicy.this.mNeedOffDueToFoldGesture = false;
            if (event.values[4] == MiuiFreeformPinManagerService.EDGE_AREA && event.values[11] < MiuiFoldPolicy.this.mFoldGestureAngleThreshold) {
                MiuiFoldPolicy.this.mNeedOffDueToFoldGesture = true;
            }
        }

        @Override // android.hardware.SensorEventListener
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    public MiuiFoldPolicy(Context context) {
        this.mContext = context;
        this.mFoldedDeviceStates = context.getResources().getIntArray(17236070);
    }

    public void initMiuiFoldPolicy() {
        ServiceThread handlerThread = new ServiceThread(TAG, -4, false);
        handlerThread.start();
        this.mHandler = new MiuiFoldPolicyHandler(handlerThread.getLooper());
        this.mTelecomManager = (TelecomManager) this.mContext.getSystemService("telecom");
        this.mAudioManager = (AudioManager) this.mContext.getSystemService("audio");
        this.mCameraManager = (CameraManager) this.mContext.getSystemService("camera");
        this.mSensorManager = (SensorManager) this.mContext.getSystemService("sensor");
        this.mWindowManagerPolicy = (WindowManagerPolicy) LocalServices.getService(WindowManagerPolicy.class);
        this.mCameraManager.registerAvailabilityCallback(this.mAvailabilityCallback, this.mHandler);
        this.mMiHingeAngleSensor = this.mSensorManager.getDefaultSensor(TYPE_HINGE_STATE);
        this.mSettingsObserver = new SettingsObserver(this.mHandler);
        this.mSwipeUpWindow = new SwipeUpWindow(this.mContext, handlerThread.getLooper());
        registerContentObserver();
        this.mContext.registerReceiver(new UserSwitchReceiver(), new IntentFilter("android.intent.action.USER_SWITCHED"));
    }

    public void dealDisplayTransition() {
        Handler handler = this.mHandler;
        if (handler == null) {
            return;
        }
        handler.removeMessages(1);
        this.mHandler.sendEmptyMessage(1);
    }

    public void setDeviceStateLocked(int state) {
        if (this.mHandler == null) {
            return;
        }
        if (DEBUG.booleanValue()) {
            Slog.i(TAG, "setDeviceStateLocked: " + state);
        }
        Message msg = this.mHandler.obtainMessage(3);
        msg.arg1 = state;
        this.mHandler.sendMessage(msg);
    }

    public void handleDeviceStateChanged(int state) {
        if (state == this.mState) {
            Slog.i(TAG, "startMiuiFoldPolicy return because of posture(" + this.mState + ")");
            return;
        }
        this.mState = state;
        this.mSwipeUpWindow.cancelScreenOffDelay();
        this.mNeedShowSwipeUpWindow = false;
        this.mNeedReleaseSwipeUpWindow = true;
        if (!isFolded() || isKeepScreenOnAfterFolded() || isCtsScene() || !this.mIsDeviceProvisioned) {
            return;
        }
        int i = this.mScreenStateAfterFold;
        if (i == 0) {
            screenTurnOff();
        } else if (i == 3) {
            if (this.mNeedOffDueToFoldGesture) {
                screenTurnOff();
            }
        } else if (i == 1) {
            this.mNeedShowSwipeUpWindow = true;
            this.mNeedReleaseSwipeUpWindow = false;
        }
    }

    private boolean isKeepScreenOnAfterFolded() {
        switch (this.mScreenStateAfterFold) {
            case 0:
                boolean isKeepScreenOn = isHoldScreenOn();
                return isKeepScreenOn;
            case 1:
                boolean isKeepScreenOn2 = isHoldScreenOn() || this.mWindowManagerPolicy.isKeyguardShowing();
                return isKeepScreenOn2;
            case 2:
                return true;
            default:
                return false;
        }
    }

    private boolean isFolded() {
        int[] iArr = this.mFoldedDeviceStates;
        return iArr != null && iArr.length > 0 && iArr[0] == this.mState;
    }

    public void showSwipeUpWindow() {
        if (this.mNeedReleaseSwipeUpWindow || !isFolded()) {
            this.mNeedReleaseSwipeUpWindow = false;
            releaseSwipUpWindow();
        } else if (this.mNeedShowSwipeUpWindow) {
            this.mNeedShowSwipeUpWindow = false;
            this.mSwipeUpWindow.showSwipeUpWindow();
        }
    }

    public void releaseSwipUpWindow() {
        this.mSwipeUpWindow.releaseSwipeWindow();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class MiuiFoldPolicyHandler extends Handler {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public MiuiFoldPolicyHandler(Looper looper) {
            super(looper);
            MiuiFoldPolicy.this = r1;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    MiuiFoldPolicy.this.showSwipeUpWindow();
                    return;
                case 2:
                    MiuiFoldPolicy.this.releaseSwipUpWindow();
                    return;
                case 3:
                    MiuiFoldPolicy.this.handleDeviceStateChanged(msg.arg1);
                    return;
                case 4:
                default:
                    return;
                case 5:
                    MiuiFoldPolicy.this.updateSettings();
                    return;
            }
        }
    }

    public void notifyReleaseWindow() {
        Handler handler = this.mHandler;
        if (handler == null) {
            return;
        }
        handler.removeCallbacksAndMessages(null);
        if (this.mSwipeUpWindow != null) {
            this.mHandler.sendEmptyMessage(2);
        }
    }

    private void screenTurnOff() {
        PowerManager powerManager = (PowerManager) this.mContext.getSystemService("power");
        powerManager.goToSleep(SystemClock.uptimeMillis(), 3, 0);
    }

    private boolean isHoldScreenOn() {
        return this.mTelecomManager.isInCall() || !this.mOpeningCameraID.isEmpty() || this.mAudioManager.getMode() == 2 || this.mAudioManager.getMode() == 3;
    }

    /* loaded from: classes.dex */
    public final class SettingsObserver extends ContentObserver {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public SettingsObserver(Handler handler) {
            super(handler);
            MiuiFoldPolicy.this = r1;
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            char c;
            String lastPathSegment = uri.getLastPathSegment();
            switch (lastPathSegment.hashCode()) {
                case -1346041141:
                    if (lastPathSegment.equals("fold_gesture_angle_threshold")) {
                        c = 0;
                        break;
                    }
                    c = 65535;
                    break;
                case -746666980:
                    if (lastPathSegment.equals(MiuiFoldPolicy.MIUI_OPTIMIZATION)) {
                        c = 2;
                        break;
                    }
                    c = 65535;
                    break;
                case 1230812500:
                    if (lastPathSegment.equals(MiuiFoldPolicy.CLOSE_LID_DISPLAY_SETTING)) {
                        c = 1;
                        break;
                    }
                    c = 65535;
                    break;
                case 1384083403:
                    if (lastPathSegment.equals("device_provisioned")) {
                        c = 3;
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
                    MiuiFoldPolicy.this.updateFoldGestureAngleThreshold();
                    return;
                case 1:
                    MiuiFoldPolicy.this.updateScreenStateAfterFold();
                    return;
                case 2:
                    MiuiFoldPolicy.this.updateCtsMode();
                    return;
                case 3:
                    MiuiFoldPolicy.this.updateDeviceProVisioned();
                    return;
                default:
                    return;
            }
        }
    }

    public void updateSettings() {
        updateScreenStateAfterFold();
        updateFoldGestureAngleThreshold();
        updateCtsMode();
        updateDeviceProVisioned();
    }

    private void registerContentObserver() {
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(CLOSE_LID_DISPLAY_SETTING), false, this.mSettingsObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("fold_gesture_angle_threshold"), false, this.mSettingsObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(MIUI_OPTIMIZATION), false, this.mSettingsObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor("device_provisioned"), false, this.mSettingsObserver, -1);
        updateSettings();
    }

    public void updateFoldGestureAngleThreshold() {
        this.mFoldGestureAngleThreshold = Settings.System.getIntForUser(this.mContext.getContentResolver(), "fold_gesture_angle_threshold", 82, -2);
    }

    public void updateScreenStateAfterFold() {
        int intForUser = Settings.System.getIntForUser(this.mContext.getContentResolver(), CLOSE_LID_DISPLAY_SETTING, -1, -2);
        this.mScreenStateAfterFold = intForUser;
        boolean z = true;
        if (intForUser == -1) {
            if ("cetus".equals(Build.DEVICE)) {
                this.mScreenStateAfterFold = 2;
            } else {
                this.mScreenStateAfterFold = 1;
            }
            Settings.System.putIntForUser(this.mContext.getContentResolver(), CLOSE_LID_DISPLAY_SETTING, this.mScreenStateAfterFold, -2);
        }
        if (this.mScreenStateAfterFold != 3) {
            z = false;
        }
        registerMiHingeAngleSensorListener(z);
    }

    public void updateCtsMode() {
        this.mIsCtsMode = !SystemProperties.getBoolean("persist.sys.miui_optimization", !SplitScreenReporter.ACTION_ENTER_SPLIT.equals(SystemProperties.get("ro.miui.cts")));
    }

    private boolean isCtsScene() {
        int i;
        boolean z = true;
        if (!this.mIsCtsMode || ((i = this.mScreenStateAfterFold) != 1 && i != 3)) {
            z = false;
        }
        boolean isCtsScene = z;
        if (isCtsScene) {
            Slog.i(TAG, "running cts, skip fold policy.");
        }
        return isCtsScene;
    }

    public void updateDeviceProVisioned() {
        boolean z = true;
        if (Settings.Global.getInt(this.mContext.getContentResolver(), "device_provisioned", 1) == 0) {
            z = false;
        }
        this.mIsDeviceProvisioned = z;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class UserSwitchReceiver extends BroadcastReceiver {
        private UserSwitchReceiver() {
            MiuiFoldPolicy.this = r1;
        }

        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            MiuiFoldPolicy.this.mHandler.sendEmptyMessage(5);
        }
    }

    private void registerMiHingeAngleSensorListener(boolean enable) {
        if (enable) {
            if (!this.mMiHingeAngleSensorEnabled) {
                this.mMiHingeAngleSensorEnabled = true;
                this.mSensorManager.registerListener(this.mMiHingeAngleSensorListener, this.mMiHingeAngleSensor, 0);
            }
        } else if (this.mMiHingeAngleSensorEnabled) {
            this.mMiHingeAngleSensorEnabled = false;
            this.mSensorManager.unregisterListener(this.mMiHingeAngleSensorListener);
        }
    }
}
