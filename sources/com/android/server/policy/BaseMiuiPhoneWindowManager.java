package com.android.server.policy;

import android.app.ActivityManager;
import android.app.Dialog;
import android.app.SearchManager;
import android.app.StatusBarManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Rect;
import android.hardware.input.InputManager;
import android.media.AudioManager;
import android.media.AudioSystem;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.telecom.TelecomManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Slog;
import android.view.IDisplayFoldListener;
import android.view.IRotationWatcher;
import android.view.IWindowManager;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.android.internal.statusbar.IStatusBarService;
import com.android.server.LocalServices;
import com.android.server.display.DisplayManagerServiceStub;
import com.android.server.input.InputOneTrackUtil;
import com.android.server.input.ReflectionUtils;
import com.android.server.input.overscroller.ScrollerOptimizationConfigProvider;
import com.android.server.input.pocketmode.MiuiPocketModeManager;
import com.android.server.input.pocketmode.MiuiPocketModeSensorWrapper;
import com.android.server.input.shoulderkey.ShoulderKeyManagerInternal;
import com.android.server.padkeyboard.MiuiPadKeyboardManager;
import com.android.server.padkeyboard.usb.UsbKeyboardUtil;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.statusbar.StatusBarManagerInternal;
import com.android.server.wm.ActivityTaskManagerServiceStub;
import com.android.server.wm.ActivityTaskSupervisorImpl;
import com.android.server.wm.DisplayFrames;
import com.android.server.wm.MiuiFreeformPinManagerService;
import com.android.server.wm.WindowState;
import com.android.server.wm.WindowStateStubImpl;
import com.miui.server.AccessController;
import com.miui.server.input.AutoDisableScreenButtonsManager;
import com.miui.server.input.MiuiBackTapGestureService;
import com.miui.server.input.MiuiFingerPrintTapListener;
import com.miui.server.input.PadManager;
import com.miui.server.input.edgesuppression.EdgeSuppressionManager;
import com.miui.server.input.gesture.multifingergesture.MiuiMultiFingerGestureManager;
import com.miui.server.input.knock.MiuiKnockGestureService;
import com.miui.server.input.stylus.MiuiStylusPageKeyListener;
import com.miui.server.input.stylus.MiuiStylusUtils;
import com.miui.server.input.time.MiuiTimeFloatingWindow;
import com.miui.server.input.util.MiuiCustomizeShortCutUtils;
import com.miui.server.input.util.ShortCutActionsUtils;
import com.miui.whetstone.PowerKeeperPolicy;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.BooleanSupplier;
import miui.app.MiuiFreeFormManager;
import miui.os.Build;
import miui.os.DeviceFeature;
import miui.provider.SettingsStringUtil;
import miui.util.FeatureParser;
import miui.util.HapticFeedbackUtil;
import miui.util.ITouchFeature;
import miui.util.SmartCoverManager;
import org.json.JSONException;
import org.json.JSONObject;
/* loaded from: classes.dex */
public abstract class BaseMiuiPhoneWindowManager extends PhoneWindowManager {
    private static final int ACCESSIBLE_MODE_LARGE_DENSITY = 461;
    private static final int ACCESSIBLE_MODE_SMALL_DENSITY = 352;
    private static final int ACTION_DOUBLE_CLICK = 1;
    private static final int ACTION_LONGPRESS = 2;
    private static final String ACTION_PARTIAL_SCREENSHOT = "android.intent.action.CAPTURE_PARTIAL_SCREENSHOT";
    private static final String ACTION_POWER_UP = "power_up";
    private static final int ACTION_SINGLE_CLICK = 0;
    private static final int BTN_MOUSE = 272;
    private static final int COMBINE_VOLUME_KEY_DELAY_TIME = 150;
    private static final boolean DEBUG = false;
    private static final int DOUBLE_CLICK_AI_KEY_TIME = 300;
    private static final int ENABLE_HOME_KEY_DOUBLE_TAP_INTERVAL = 300;
    private static final int ENABLE_HOME_KEY_PRESS_INTERVAL = 300;
    private static final int ENABLE_VOLUME_KEY_PRESS_COUNTS = 2;
    private static final int ENABLE_VOLUME_KEY_PRESS_INTERVAL = 300;
    private static final String HOME_PACKAGE_NAME = "com.miui.home";
    protected static final int INTERCEPT_EXPECTED_RESULT_GO_TO_SLEEP = -1;
    protected static final int INTERCEPT_EXPECTED_RESULT_NONE = 0;
    protected static final int INTERCEPT_EXPECTED_RESULT_WAKE_UP = 1;
    private static final int KEYCODE_AI = 689;
    private static final String KEY_GAME_BOOSTER = "gb_boosting";
    private static final int KEY_LONG_PRESS_TIMEOUT_DELAY_FOR_SHOW_MENU = 50;
    private static final String LAST_POWER_UP_EVENT = "power_up_event";
    private static final String LAST_POWER_UP_POLICY = "power_up_policy";
    private static final String LAST_POWER_UP_SCREEN_STATE = "power_up_screen_state";
    private static final int LAUNCH_AU_PAY_WHEN_SOS_ENABLE_DELAY_TIME = 500;
    private static final int LAUNCH_SOS_BY_PRESS_POWER_KEY_CONTINUOUSLY = 5;
    private static final int LONG_PRESS_AI_KEY_TIME = 500;
    private static final long LONG_PRESS_POWER_KEY_TIMEOUT = 3000;
    private static final int LONG_PRESS_VOLUME_DOWN_ACTION_NONE = 0;
    private static final int LONG_PRESS_VOLUME_DOWN_ACTION_PAY = 2;
    private static final int LONG_PRESS_VOLUME_DOWN_ACTION_STREET_SNAP = 1;
    private static final int LONG_PRESS_VOLUME_TIME = 1000;
    private static final int META_KEY_PRESS_INTERVAL = 300;
    private static final int MSG_COMBINE_VOLUME_KEY_DELAY_TIME = 3000;
    private static final String NAVIGATION_BAR_WINDOW_LOADED = "navigation_bar_window_loaded";
    private static final String PERMISSION_INTERNAL_GENERAL_API = "miui.permission.USE_INTERNAL_GENERAL_API";
    private static final long POWER_DOUBLE_TAP_MAX_TIME_MS = 300;
    private static final long POWER_DOUBLE_TAP_MIN_TIME_MS = 0;
    protected static final String REASON_FP_DPAD_CENTER_WAKEUP = "miui.policy:FINGERPRINT_DPAD_CENTER";
    private static final String SCREEN_KEY_LONG_PRESS_VOLUME_DOWN = "screen_key_long_press_volume_down";
    private static final String SYSTEM_SETTINGS_VR_MODE = "vr_mode";
    private static final int SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR = 16;
    private static final int XIAO_POWER_GUIDE_VERSIONCODE = 3;
    static final ArrayList<Integer> sScreenRecorderKeyEventList;
    static final ArrayList<Integer> sVoiceAssistKeyEventList;
    boolean mAccessibilityShortcutOnLockScreen;
    private SettingsStringUtil.SettingStringHelper mAccessibilityShortcutSetting;
    private PowerManager.WakeLock mAiKeyWakeLock;
    private Intent mAssistIntent;
    private AudioManager mAudioManager;
    private AutoDisableScreenButtonsManager mAutoDisableScreenButtonsManager;
    private ProgressBar mBootProgress;
    private String[] mBootText;
    private TextView mBootTextView;
    private ComponentName mCameraComponentName;
    private Intent mCameraIntent;
    boolean mCameraKeyWakeScreen;
    private int mCurrentUserId;
    private boolean mDoubleClickAiKeyIsConsumed;
    boolean mDpadCenterDown;
    private Intent mDumpLogIntent;
    private EdgeSuppressionManager mEdgeSuppressionManager;
    protected WindowManagerPolicy.WindowState mFocusedWindow;
    private boolean mFolded;
    private boolean mForbidFullScreen;
    protected boolean mFrontFingerprintSensor;
    protected Handler mHandler;
    private HapticFeedbackUtil mHapticFeedbackUtil;
    private boolean mHasWatchedRotation;
    boolean mHaveBankCard;
    boolean mHaveTranksCard;
    private PowerManager.WakeLock mHelpKeyWakeLock;
    boolean mHomeConsumed;
    boolean mHomeDoubleClickPending;
    boolean mHomeDoubleTapPending;
    boolean mHomeDownAfterDpCenter;
    String mImperceptiblePowerKey;
    private int mInputMethodWindowVisibleHeight;
    private boolean mIsFoldChanged;
    protected boolean mIsStatusBarVisibleInFullscreen;
    private boolean mIsSupportGloablTounchDirection;
    private boolean mIsVRMode;
    private volatile boolean mIsXiaoaiServiceTriggeredByLongPressPower;
    protected int mKeyLongPressTimeout;
    private int mKeyPressed;
    private int mKeyPressing;
    private long mLastPowerDown;
    private boolean mLongPressAiKeyIsConsumed;
    boolean mMikeymodeEnabled;
    private MiuiBackTapGestureService mMiuiBackTapGestureService;
    private Dialog mMiuiBootMsgDialog;
    private MiuiFingerPrintTapListener mMiuiFingerPrintTapListener;
    private MiuiKeyInterceptExtend mMiuiKeyInterceptExtend;
    protected MiuiKeyShortcutManager mMiuiKeyShortcutManager;
    protected MiuiKeyguardServiceDelegate mMiuiKeyguardDelegate;
    private MiuiKnockGestureService mMiuiKnockGestureService;
    private MiuiPadKeyboardManager mMiuiPadKeyboardManager;
    private MiuiPocketModeManager mMiuiPocketModeManager;
    private MiuiStylusPageKeyListener mMiuiStylusPageKeyListener;
    protected MiuiMultiFingerGestureManager mMiuiThreeGestureListener;
    private MiuiTimeFloatingWindow mMiuiTimeFloatingWindow;
    int mNavBarHeight;
    int mNavBarHeightLand;
    int mNavBarWidth;
    private Intent mNfcIntent;
    private Message mPowerMessage;
    private MiuiScreenOnProximityLock mProximitySensor;
    private boolean mRequestShowMenu;
    private RotationWatcher mRotationWatcher;
    protected int mScreenOffReason;
    private MiuiSettingsObserver mSettingsObserver;
    private int mShortcutPressing;
    boolean mShortcutServiceIsTalkBack;
    private boolean mShortcutTriggered;
    private boolean mShouldSendPowerUpToSmartHome;
    private ShoulderKeyManagerInternal mShoulderKeyManagerInternal;
    private Intent mSmartHomeIntent;
    protected boolean mSupportTapFingerprintSensorToHome;
    private HashSet<String> mSystemKeyPackages;
    boolean mTalkBackIsOpened;
    boolean mTestModeEnabled;
    private boolean mTorchEnabled;
    boolean mTrackballWakeScreen;
    private volatile long mUpdateWakeUpDetailTime;
    private long mVolumeButtonPrePressedTime;
    private long mVolumeButtonPressedCount;
    private boolean mVolumeDownKeyConsumed;
    private boolean mVolumeDownKeyPressed;
    private long mVolumeDownKeyTime;
    private PowerManager.WakeLock mVolumeKeyWakeLock;
    private boolean mVolumeUpKeyConsumed;
    private boolean mVolumeUpKeyPressed;
    private long mVolumeUpKeyTime;
    private volatile String mWakeUpDetail;
    private boolean mWifiOnly;
    private WindowManagerPolicy.WindowState mWin;
    private static PhoneWindowManagerFeatureImpl phoneWindowManagerFeature = new PhoneWindowManagerFeatureImpl();
    private static final int SHORTCUT_HOME_POWER = getKeyBitmask(3) | getKeyBitmask(26);
    private static final int SHORTCUT_BACK_POWER = getKeyBitmask(4) | getKeyBitmask(26);
    private static final int SHORTCUT_MENU_POWER = getKeyBitmask(187) | getKeyBitmask(26);
    private static final int SHORTCUT_SCREENSHOT_ANDROID = getKeyBitmask(26) | getKeyBitmask(25);
    private static final int SHORTCUT_SCREENSHOT_MIUI = getKeyBitmask(187) | getKeyBitmask(25);
    private static final int SHORTCUT_SCREENSHOT_SINGLE_KEY = getKeyBitmask(3) | getKeyBitmask(25);
    private static final int SHORTCUT_UNLOCK = getKeyBitmask(4) | getKeyBitmask(24);
    protected static final boolean SUPPORT_EDGE_TOUCH_VOLUME = FeatureParser.getBoolean("support_edge_touch_volume", false);
    protected static final boolean SUPPORT_POWERFP = SystemProperties.getBoolean("ro.hardware.fp.sideCap", false);
    private static final boolean IS_CETUS = "cetus".equals(Build.DEVICE);
    private static final ComponentName talkBackServiceName = new ComponentName("com.google.android.marvin.talkback", "com.google.android.marvin.talkback.TalkBackService");
    private static final int[] WINDOW_TYPES_WHERE_HOME_DOESNT_WORK = {2003, 2010};
    private int mPressPowerKeyCount = 1;
    private long mPressPowerKeyTime = 0;
    private int mDoubleClickAiKeyCount = 0;
    private long mLastClickAiKeyTime = 0;
    private int mLongPressVolumeDownBehavior = 0;
    Runnable mPowerLongPressOriginal = phoneWindowManagerFeature.getPowerLongPress(this);
    private int mXiaoaiPowerGuideCount = 0;
    private final IDisplayFoldListener mIDisplayFoldListener = new IDisplayFoldListener.Stub() { // from class: com.android.server.policy.BaseMiuiPhoneWindowManager.2
        public void onDisplayFoldChanged(int displayId, boolean folded) {
            if (BaseMiuiPhoneWindowManager.this.mFolded != folded) {
                BaseMiuiPhoneWindowManager.this.mFolded = folded;
                BaseMiuiPhoneWindowManager.this.mIsFoldChanged = true;
                BaseMiuiPhoneWindowManager.this.setTouchFeatureRotation();
            }
            if (BaseMiuiPhoneWindowManager.this.mMiuiBackTapGestureService != null) {
                BaseMiuiPhoneWindowManager.this.mMiuiBackTapGestureService.notifyFoldStatus(folded);
            }
            if (BaseMiuiPhoneWindowManager.this.mProximitySensor != null) {
                BaseMiuiPhoneWindowManager.this.mProximitySensor.release(true);
            }
        }
    };
    private SmartCoverManager mSmartCoverManager = new SmartCoverManager();
    private final Runnable mTurnOffTorch = new Runnable() { // from class: com.android.server.policy.BaseMiuiPhoneWindowManager.3
        @Override // java.lang.Runnable
        public void run() {
            if (BaseMiuiPhoneWindowManager.this.mTorchEnabled) {
                Bundle torchBundle = new Bundle();
                torchBundle.putBoolean(ShortCutActionsUtils.EXTRA_TORCH_ENABLED, false);
                ShortCutActionsUtils.getInstance(BaseMiuiPhoneWindowManager.this.mContext).triggerFunction("turn_on_torch", "launch_by_runnable", torchBundle, true);
            }
        }
    };
    private Runnable mSingleClickAiKeyRunnable = new Runnable() { // from class: com.android.server.policy.BaseMiuiPhoneWindowManager.4
        @Override // java.lang.Runnable
        public void run() {
            BaseMiuiPhoneWindowManager.this.mDoubleClickAiKeyCount = 0;
            MiuiInputLog.detail("single click ai key");
            BaseMiuiPhoneWindowManager.this.startAiKeyService("key_single_click_ai_button_settings");
        }
    };
    private Runnable mDoubleClickAiKeyRunnable = new Runnable() { // from class: com.android.server.policy.BaseMiuiPhoneWindowManager.5
        @Override // java.lang.Runnable
        public void run() {
            MiuiInputLog.detail("double click ai key");
            BaseMiuiPhoneWindowManager.this.startAiKeyService("key_double_click_ai_button_settings");
        }
    };
    private Runnable mLongPressDownAiKeyRunnable = new Runnable() { // from class: com.android.server.policy.BaseMiuiPhoneWindowManager.6
        @Override // java.lang.Runnable
        public void run() {
            BaseMiuiPhoneWindowManager.this.mDoubleClickAiKeyCount = 0;
            BaseMiuiPhoneWindowManager.this.mLongPressAiKeyIsConsumed = true;
            MiuiInputLog.detail("long press down ai key");
            BaseMiuiPhoneWindowManager.this.startAiKeyService("key_long_press_down_ai_button_settings");
        }
    };
    private Runnable mLongPressUpAiKeyRunnable = new Runnable() { // from class: com.android.server.policy.BaseMiuiPhoneWindowManager.7
        @Override // java.lang.Runnable
        public void run() {
            BaseMiuiPhoneWindowManager.this.mDoubleClickAiKeyCount = 0;
            MiuiInputLog.detail("long press up ai key");
            BaseMiuiPhoneWindowManager.this.startAiKeyService("key_long_press_up_ai_button_settings");
        }
    };
    private final MiuiPocketModeSensorWrapper.ProximitySensorChangeListener mWakeUpKeySensorListener = new MiuiPocketModeSensorWrapper.ProximitySensorChangeListener() { // from class: com.android.server.policy.BaseMiuiPhoneWindowManager.8
        @Override // com.android.server.input.pocketmode.MiuiPocketModeSensorWrapper.ProximitySensorChangeListener
        public void onSensorChanged(boolean tooClose) {
            BaseMiuiPhoneWindowManager.this.mMiuiPocketModeManager.unregisterListener();
            if (tooClose) {
                Slog.w("BaseMiuiPhoneWindowManager", "Going to sleep due to KEYCODE_WAKEUP/KEYCODE_DPAD_CENTER: proximity sensor too close");
                ShortCutActionsUtils.getInstance(BaseMiuiPhoneWindowManager.this.mContext).triggerFunction("go_to_sleep", ShortCutActionsUtils.REASON_OF_TRIGGERED_BY_PROXIMITY_SENSOR, null, false);
            }
        }
    };
    private int mMetaKeyAction = 0;
    private boolean mMetaKeyConsume = true;
    private boolean mSystemShortcutsMenuShown = false;
    private int mKeyBoardDeviceId = -1;
    private final Runnable mMetaKeyRunnable = new Runnable() { // from class: com.android.server.policy.BaseMiuiPhoneWindowManager.10
        @Override // java.lang.Runnable
        public void run() {
            if (!BaseMiuiPhoneWindowManager.this.mMetaKeyConsume) {
                BaseMiuiPhoneWindowManager.this.mMetaKeyConsume = true;
                MiuiInputLog.major(" mMetaKeyAction : " + BaseMiuiPhoneWindowManager.this.mMetaKeyAction);
                Intent intent = null;
                if (BaseMiuiPhoneWindowManager.this.mMetaKeyAction == 0) {
                    intent = new Intent("com.miui.home.action.SINGLE_CLICK");
                    intent.setPackage("com.miui.home");
                    InputOneTrackUtil.getInstance(BaseMiuiPhoneWindowManager.this.mContext).trackKeyboardEvent(MiuiCustomizeShortCutUtils.SHOW_DOCK);
                } else if (BaseMiuiPhoneWindowManager.this.mMetaKeyAction == 1) {
                    intent = new Intent("com.miui.home.action.DOUBLE_CLICK");
                    intent.setPackage("com.miui.home");
                    InputOneTrackUtil.getInstance(BaseMiuiPhoneWindowManager.this.mContext).trackKeyboardEvent(MiuiCustomizeShortCutUtils.TOGGLE_RECENT_APP);
                } else if (BaseMiuiPhoneWindowManager.this.mMetaKeyAction == 2) {
                    BaseMiuiPhoneWindowManager baseMiuiPhoneWindowManager = BaseMiuiPhoneWindowManager.this;
                    baseMiuiPhoneWindowManager.showKeyboardShortcutsMenu(baseMiuiPhoneWindowManager.mKeyBoardDeviceId, true, true);
                    InputOneTrackUtil.getInstance(BaseMiuiPhoneWindowManager.this.mContext).trackKeyboardEvent(MiuiCustomizeShortCutUtils.SHOW_SHORTCUT_LIST);
                }
                if (intent != null) {
                    BaseMiuiPhoneWindowManager.this.sendAsyncBroadcast(intent, BaseMiuiPhoneWindowManager.PERMISSION_INTERNAL_GENERAL_API);
                }
            }
        }
    };
    protected List<String> mFpNavEventNameList = null;
    private boolean mKeyguardOnWhenHomeDown = false;
    private Binder mWindowFlagBinder = new Binder();
    private Binder mBinder = new Binder();
    BroadcastReceiver mStatusBarExitFullscreenReceiver = new BroadcastReceiver() { // from class: com.android.server.policy.BaseMiuiPhoneWindowManager.11
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            BaseMiuiPhoneWindowManager.this.setStatusBarInFullscreen(false);
        }
    };
    BroadcastReceiver mScreenshotReceiver = new BroadcastReceiver() { // from class: com.android.server.policy.BaseMiuiPhoneWindowManager.12
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            BaseMiuiPhoneWindowManager.this.takeScreenshot(intent, "screen_shot");
        }
    };
    BroadcastReceiver mPartialScreenshotReceiver = new BroadcastReceiver() { // from class: com.android.server.policy.BaseMiuiPhoneWindowManager.13
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            BaseMiuiPhoneWindowManager.this.takeScreenshot(intent, "partial_screen_shot");
        }
    };
    private final Runnable mHomeDoubleTapTimeoutRunnable = new Runnable() { // from class: com.android.server.policy.BaseMiuiPhoneWindowManager.19
        @Override // java.lang.Runnable
        public void run() {
            if (BaseMiuiPhoneWindowManager.this.mHomeDoubleTapPending) {
                BaseMiuiPhoneWindowManager.this.mHomeDoubleTapPending = false;
                BaseMiuiPhoneWindowManager.this.launchHomeFromHotKey(0);
            }
        }
    };
    private final Runnable mHomeDoubleClickTimeoutRunnable = new Runnable() { // from class: com.android.server.policy.BaseMiuiPhoneWindowManager.20
        @Override // java.lang.Runnable
        public void run() {
            if (BaseMiuiPhoneWindowManager.this.mHomeDoubleClickPending) {
                BaseMiuiPhoneWindowManager.this.mHomeDoubleClickPending = false;
            }
        }
    };
    private final Runnable mDoubleClickPowerRunnable = new Runnable() { // from class: com.android.server.policy.BaseMiuiPhoneWindowManager$$ExternalSyntheticLambda3
        @Override // java.lang.Runnable
        public final void run() {
            BaseMiuiPhoneWindowManager.this.m1387x2b1f794f();
        }
    };
    private boolean mScreenRecorderEnabled = false;
    BroadcastReceiver mScreenRecordeEnablekKeyEventReceiver = new BroadcastReceiver() { // from class: com.android.server.policy.BaseMiuiPhoneWindowManager.21
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            boolean enable = intent.getBooleanExtra(MiuiCustomizeShortCutUtils.ATTRIBUTE_ENABLE, false);
            Slog.i("WindowManager", "mScreenRecordeEnablekKeyEventReceiver enable=" + enable);
            BaseMiuiPhoneWindowManager.this.setScreenRecorderEnabled(enable);
        }
    };
    private boolean mTrackDumpLogKeyCodePengding = false;
    private long mTrackDumpLogKeyCodeStartTime = 0;
    private int mTrackDumpLogKeyCodeLastKeyCode = 25;
    private int mTrackDumpLogKeyCodeTimeOut = 2000;
    private int mTrackDumpLogKeyCodeVolumeDownTimes = 0;
    private boolean mVoiceAssistEnabled = false;
    private BroadcastReceiver mInternalBroadcastReceiver = new BroadcastReceiver() { // from class: com.android.server.policy.BaseMiuiPhoneWindowManager.22
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("com.miui.app.ExtraStatusBarManager.action_enter_drive_mode".equals(action)) {
                BaseMiuiPhoneWindowManager.this.mForbidFullScreen = true;
            } else if ("com.miui.app.ExtraStatusBarManager.action_leave_drive_mode".equals(action)) {
                BaseMiuiPhoneWindowManager.this.mForbidFullScreen = false;
            }
        }
    };

    protected abstract int callSuperInterceptKeyBeforeQueueing(KeyEvent keyEvent, int i, boolean z);

    protected abstract void cancelPreloadRecentAppsInternal();

    protected abstract void finishActivityInternal(IBinder iBinder, int i, Intent intent) throws RemoteException;

    protected abstract void forceStopPackage(String str, int i, String str2);

    protected abstract WindowManagerPolicy.WindowState getKeyguardWindowState();

    protected abstract int getWakePolicyFlag();

    protected abstract boolean interceptPowerKeyByFingerPrintKey();

    protected abstract boolean isFingerPrintKey(KeyEvent keyEvent);

    protected abstract boolean isScreenOnInternal();

    protected abstract void launchAssistActionInternal(String str, Bundle bundle);

    protected abstract void launchRecentPanelInternal();

    protected abstract void onStatusBarPanelRevealed(IStatusBarService iStatusBarService);

    protected abstract void preloadRecentAppsInternal();

    protected abstract int processFingerprintNavigationEvent(KeyEvent keyEvent, boolean z);

    protected abstract boolean screenOffBecauseOfProxSensor();

    protected abstract void toggleSplitScreenInternal();

    public BaseMiuiPhoneWindowManager() {
        HashSet<String> hashSet = new HashSet<>();
        this.mSystemKeyPackages = hashSet;
        hashSet.add("android");
        this.mSystemKeyPackages.add("com.android.systemui");
        this.mSystemKeyPackages.add("com.android.phone");
        this.mSystemKeyPackages.add("com.android.mms");
        this.mSystemKeyPackages.add("com.android.contacts");
        this.mSystemKeyPackages.add("com.miui.home");
        this.mSystemKeyPackages.add("com.jeejen.family.miui");
        this.mSystemKeyPackages.add("com.android.incallui");
        this.mSystemKeyPackages.add("com.miui.backup");
        this.mSystemKeyPackages.add(ActivityTaskSupervisorImpl.MIUI_APP_LOCK_PACKAGE_NAME);
        this.mSystemKeyPackages.add("com.xiaomi.mihomemanager");
        this.mSystemKeyPackages.add("com.miui.securityadd");
    }

    private static int getKeyBitmask(int keycode) {
        switch (keycode) {
            case 3:
                return 8;
            case 4:
                return 16;
            case 24:
                return 128;
            case 25:
                return 64;
            case 26:
                return 32;
            case UsbKeyboardUtil.COMMAND_READ_KEYBOARD /* 82 */:
                return 2;
            case 187:
                return 4;
            default:
                return 1;
        }
    }

    static {
        ArrayList<Integer> arrayList = new ArrayList<>();
        sScreenRecorderKeyEventList = arrayList;
        arrayList.add(3);
        arrayList.add(4);
        arrayList.add(82);
        arrayList.add(187);
        arrayList.add(24);
        arrayList.add(25);
        arrayList.add(26);
        ArrayList<Integer> arrayList2 = new ArrayList<>();
        sVoiceAssistKeyEventList = arrayList2;
        arrayList2.add(4);
    }

    private int getCodeByKeyPressed(int keyPressed) {
        switch (keyPressed) {
            case 2:
                return 82;
            case 4:
                return 187;
            case 8:
                return 3;
            case 16:
                return 4;
            case 32:
                return 26;
            case 64:
                return 25;
            case 128:
                return 24;
            default:
                return 0;
        }
    }

    public void initInternal(Context context, IWindowManager windowManager, WindowManagerPolicy.WindowManagerFuncs windowManagerFuncs) {
        Resources res = context.getResources();
        this.mNavBarWidth = res.getDimensionPixelSize(17105382);
        this.mNavBarHeight = res.getDimensionPixelSize(17105377);
        this.mNavBarHeightLand = res.getDimensionPixelSize(17105379);
        boolean hasSupportGlobalTouchDirection = ITouchFeature.getInstance().hasSupportGlobalTouchDirection();
        this.mIsSupportGloablTounchDirection = hasSupportGlobalTouchDirection;
        if (hasSupportGlobalTouchDirection || EdgeSuppressionManager.IS_SUPPORT_EDGE_MODE) {
            handleTouchFeatureRotationWatcher();
        }
        this.mHandler = new H();
        this.mSettingsObserver = new MiuiSettingsObserver(this.mHandler);
        this.mEdgeSuppressionManager = EdgeSuppressionManager.getInstance(context);
        this.mMiuiKeyShortcutManager = MiuiKeyShortcutManager.getInstance(context, this.mHandler, this.mKeyCombinationManager, this.mSingleKeyGestureDetector);
        if (MiuiStylusUtils.supportStylusGesture()) {
            this.mMiuiStylusPageKeyListener = new MiuiStylusPageKeyListener(context);
        }
        this.mMiuiKeyInterceptExtend = MiuiKeyInterceptExtend.getInstance(this.mHandler, this.mContext, this.mPowerManager, this.mCurrentUserId, this.mMiuiKeyShortcutManager.mSingleKeyUse);
        this.mSettingsObserver.observe();
        phoneWindowManagerFeature.setPowerLongPress(this, new Runnable() { // from class: com.android.server.policy.BaseMiuiPhoneWindowManager.1
            @Override // java.lang.Runnable
            public void run() {
                BaseMiuiPhoneWindowManager.this.mPowerLongPressOriginal.run();
            }
        });
        this.mVolumeKeyWakeLock = this.mPowerManager.newWakeLock(1, "PhoneWindowManager.mVolumeKeyWakeLock");
        this.mAiKeyWakeLock = this.mPowerManager.newWakeLock(1, "PhoneWindowManager.mAiKeyWakeLock");
        this.mHelpKeyWakeLock = this.mPowerManager.newWakeLock(1, "PhoneWindowManager.mHelpKeyWakeLock");
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.CAPTURE_SCREENSHOT");
        context.registerReceiverAsUser(this.mScreenshotReceiver, UserHandle.ALL, filter, PERMISSION_INTERNAL_GENERAL_API, null);
        IntentFilter filter2 = new IntentFilter();
        filter2.addAction(ACTION_PARTIAL_SCREENSHOT);
        context.registerReceiverAsUser(this.mPartialScreenshotReceiver, UserHandle.ALL, filter2, PERMISSION_INTERNAL_GENERAL_API, null);
        IntentFilter filter3 = new IntentFilter();
        filter3.addAction("com.miui.app.ExtraStatusBarManager.EXIT_FULLSCREEN");
        context.registerReceiver(this.mStatusBarExitFullscreenReceiver, filter3);
        IntentFilter filter4 = new IntentFilter();
        filter4.addAction("miui.intent.SCREEN_RECORDER_ENABLE_KEYEVENT");
        context.registerReceiver(this.mScreenRecordeEnablekKeyEventReceiver, filter4);
        IntentFilter filter5 = new IntentFilter();
        filter5.addAction("com.miui.app.ExtraStatusBarManager.action_enter_drive_mode");
        filter5.addAction("com.miui.app.ExtraStatusBarManager.action_leave_drive_mode");
        context.registerReceiverAsUser(this.mInternalBroadcastReceiver, UserHandle.ALL, filter5, PERMISSION_INTERNAL_GENERAL_API, this.mHandler);
        this.mHapticFeedbackUtil = new HapticFeedbackUtil(context, false);
        this.mAutoDisableScreenButtonsManager = new AutoDisableScreenButtonsManager(context, this.mHandler);
        this.mSmartCoverManager.init(context, this.mPowerManager, this.mHandler);
        saveWindowTypeLayer(context);
        this.mMiuiTimeFloatingWindow = new MiuiTimeFloatingWindow(this.mContext);
    }

    private boolean canReceiveInput(WindowManagerPolicy.WindowState win) {
        boolean z;
        boolean z2;
        WindowManager.LayoutParams attrs = (WindowManager.LayoutParams) ReflectionUtils.callPrivateMethod(win, "getAttrs", new Object[0]);
        boolean notFocusable = false;
        boolean altFocusableIm = false;
        if (attrs != null) {
            if ((attrs.flags & 8) == 0) {
                z = false;
            } else {
                z = true;
            }
            notFocusable = z;
            if ((attrs.flags & 131072) == 0) {
                z2 = false;
            } else {
                z2 = true;
            }
            altFocusableIm = z2;
        }
        boolean notFocusableForIm = notFocusable ^ altFocusableIm;
        if (notFocusableForIm) {
            return false;
        }
        return true;
    }

    private void saveWindowTypeLayer(Context context) {
        JSONObject typeLayers = new JSONObject();
        int[] types = {2000, 2001, 2013};
        for (int type : types) {
            int layer = getWindowLayerFromTypeLw(type);
            if (layer != 2) {
                try {
                    typeLayers.put(Integer.toString(type), layer);
                } catch (JSONException ex) {
                    Slog.e("WindowManager", "JSONException", ex);
                }
            }
        }
        MiuiSettings.System.putString(context.getContentResolver(), "window_type_layer", typeLayers.toString());
    }

    public void systemReadyInternal() {
        this.mMiuiFingerPrintTapListener = new MiuiFingerPrintTapListener(this.mContext);
        MiuiMultiFingerGestureManager miuiMultiFingerGestureManager = new MiuiMultiFingerGestureManager(this.mContext, this.mHandler);
        this.mMiuiThreeGestureListener = miuiMultiFingerGestureManager;
        miuiMultiFingerGestureManager.initKeyguardActiveFunction(new BooleanSupplier() { // from class: com.android.server.policy.BaseMiuiPhoneWindowManager$$ExternalSyntheticLambda1
            @Override // java.util.function.BooleanSupplier
            public final boolean getAsBoolean() {
                return BaseMiuiPhoneWindowManager.this.getKeyguardActive();
            }
        });
        this.mMiuiKnockGestureService = new MiuiKnockGestureService(this.mContext);
        this.mMiuiBackTapGestureService = new MiuiBackTapGestureService(this.mContext);
        ShoulderKeyManagerInternal shoulderKeyManagerInternal = (ShoulderKeyManagerInternal) LocalServices.getService(ShoulderKeyManagerInternal.class);
        this.mShoulderKeyManagerInternal = shoulderKeyManagerInternal;
        if (shoulderKeyManagerInternal != null) {
            shoulderKeyManagerInternal.systemReady();
        }
        PackageManager pm = this.mContext.getPackageManager();
        if (MiuiPocketModeManager.isSupportInertialAndLightSensor(this.mContext) || (pm != null && pm.hasSystemFeature("android.hardware.sensor.proximity") && !DeviceFeature.hasSupportAudioPromity())) {
            this.mProximitySensor = new MiuiScreenOnProximityLock(this.mContext, this.mMiuiKeyguardDelegate, this.mHandler.getLooper());
        }
        Settings.Global.putInt(this.mContext.getContentResolver(), "torch_state", 0);
        Settings.Global.putInt(this.mContext.getContentResolver(), "auto_test_mode_on", 0);
        this.mIsVRMode = false;
        Settings.System.putInt(this.mContext.getContentResolver(), SYSTEM_SETTINGS_VR_MODE, 0);
        this.mFrontFingerprintSensor = FeatureParser.getBoolean("front_fingerprint_sensor", false);
        this.mSupportTapFingerprintSensorToHome = FeatureParser.getBoolean("support_tap_fingerprint_sensor_to_home", false);
        this.mFpNavEventNameList = new ArrayList();
        String[] strArray = FeatureParser.getStringArray("fp_nav_event_name_list");
        if (strArray != null) {
            for (String str : strArray) {
                this.mFpNavEventNameList.add(str);
            }
        }
        Settings.Global.putStringForUser(this.mContext.getContentResolver(), "policy_control", "immersive.preconfirms=*", -2);
        if (Settings.System.getInt(this.mContext.getContentResolver(), "persist.camera.snap.enable", 0) == 1) {
            Settings.System.putInt(this.mContext.getContentResolver(), "persist.camera.snap.enable", 0);
            if (!this.mHaveTranksCard) {
                Settings.Secure.putStringForUser(this.mContext.getContentResolver(), "key_long_press_volume_down", "Street-snap", this.mCurrentUserId);
            } else {
                Settings.Secure.putStringForUser(this.mContext.getContentResolver(), "key_long_press_volume_down", "none", this.mCurrentUserId);
            }
        }
        this.mSettingsObserver.onChange(false);
        this.mEdgeSuppressionManager.handleEdgeModeChange(EdgeSuppressionManager.REASON_OF_CONFIGURATION);
        MiuiPadKeyboardManager keyboardManager = MiuiPadKeyboardManager.getKeyboardManager(this.mContext);
        this.mMiuiPadKeyboardManager = keyboardManager;
        if (keyboardManager != null) {
            LocalServices.addService(MiuiPadKeyboardManager.class, keyboardManager);
        }
        if (MiuiSettings.System.IS_FOLD_DEVICE) {
            registerDisplayFoldListener(this.mIDisplayFoldListener);
        }
    }

    public void startedWakingUp(int why) {
        super.startedWakingUp(why);
        if (this.mMiuiPocketModeManager == null) {
            this.mMiuiPocketModeManager = new MiuiPocketModeManager(this.mContext);
        }
        if (this.mProximitySensor != null && isDeviceProvisioned() && !MiuiSettings.System.isInSmallWindowMode(this.mContext) && this.mMiuiPocketModeManager.getPocketModeEnableSettings() && !this.mIsVRMode && !this.mIsFoldChanged && !skipPocketModeAquireByWakeUpDetail(this.mWakeUpDetail)) {
            this.mProximitySensor.aquire();
        }
        this.mMiuiKnockGestureService.updateScreenState(true);
        this.mMiuiThreeGestureListener.updateScreenState(true);
        this.mMiuiTimeFloatingWindow.updateScreenState(true);
        this.mMiuiBackTapGestureService.notifyScreenOn();
        MiuiStylusPageKeyListener miuiStylusPageKeyListener = this.mMiuiStylusPageKeyListener;
        if (miuiStylusPageKeyListener != null) {
            miuiStylusPageKeyListener.updateScreenState(true);
        }
        ShoulderKeyManagerInternal shoulderKeyManagerInternal = this.mShoulderKeyManagerInternal;
        if (shoulderKeyManagerInternal != null) {
            shoulderKeyManagerInternal.updateScreenState(true);
        }
        MiuiPadKeyboardManager miuiPadKeyboardManager = this.mMiuiPadKeyboardManager;
        if (miuiPadKeyboardManager != null) {
            miuiPadKeyboardManager.notifyScreenState(true);
        }
    }

    public void setWakeUpDetail(String detail) {
        this.mWakeUpDetail = detail;
        this.mUpdateWakeUpDetailTime = System.currentTimeMillis();
    }

    private boolean skipPocketModeAquireByWakeUpDetail(String detail) {
        long currentTime = System.currentTimeMillis();
        return MiuiScreenOnProximityLock.SKIP_AQUIRE_WAKE_UP_DETAIL_LIST.contains(detail) && currentTime > this.mUpdateWakeUpDetailTime && currentTime - this.mUpdateWakeUpDetailTime < 100;
    }

    public void screenTurningOn(int displayId, WindowManagerPolicy.ScreenOnListener screenOnListener) {
        MiuiKeyguardServiceDelegate miuiKeyguardServiceDelegate;
        super.screenTurningOn(displayId, screenOnListener);
        if (screenOnListener == null && (miuiKeyguardServiceDelegate = this.mMiuiKeyguardDelegate) != null) {
            miuiKeyguardServiceDelegate.onScreenTurnedOnWithoutListener();
        }
        if (MiuiSettings.System.IS_FOLD_DEVICE) {
            DisplayManagerServiceStub.getInstance().screenTurningOn();
        }
    }

    public void screenTurningOff(int displayId, WindowManagerPolicy.ScreenOffListener screenOffListener) {
        super.screenTurningOff(displayId, screenOffListener);
        if (MiuiSettings.System.IS_FOLD_DEVICE) {
            DisplayManagerServiceStub.getInstance().screenTurningOff();
        }
    }

    public void screenTurnedOff(int displayId) {
        super.screenTurnedOff(displayId);
        startCameraProcess();
    }

    private void startCameraProcess() {
        try {
            Intent cameraIntent = new Intent("miui.action.CAMERA_EMPTY_SERVICE");
            cameraIntent.setPackage(AccessController.PACKAGE_CAMERA);
            this.mContext.startServiceAsUser(cameraIntent, UserHandle.CURRENT);
        } catch (Exception e) {
            Slog.e("WindowManager", "IllegalAccessException", e);
        }
    }

    public void startedGoingToSleep(int why) {
        this.mMiuiKnockGestureService.updateScreenState(false);
        this.mMiuiThreeGestureListener.updateScreenState(false);
        ShoulderKeyManagerInternal shoulderKeyManagerInternal = this.mShoulderKeyManagerInternal;
        if (shoulderKeyManagerInternal != null) {
            shoulderKeyManagerInternal.updateScreenState(false);
        }
        MiuiStylusPageKeyListener miuiStylusPageKeyListener = this.mMiuiStylusPageKeyListener;
        if (miuiStylusPageKeyListener != null) {
            miuiStylusPageKeyListener.updateScreenState(false);
        }
        this.mMiuiTimeFloatingWindow.updateScreenState(false);
        MiuiPadKeyboardManager miuiPadKeyboardManager = this.mMiuiPadKeyboardManager;
        if (miuiPadKeyboardManager != null) {
            miuiPadKeyboardManager.notifyScreenState(false);
        }
        super.startedGoingToSleep(why);
    }

    public void finishedGoingToSleep(int why) {
        screenTurnedOffInternal(why);
        this.mMiuiBackTapGestureService.notifyScreenOff();
        this.mEdgeSuppressionManager.finishedGoingToSleep();
        releaseScreenOnProximitySensor(true);
        super.finishedGoingToSleep(why);
    }

    public void finishedWakingUp(int why) {
        super.finishedWakingUp(why);
        this.mEdgeSuppressionManager.finishedWakingUp();
    }

    protected void screenTurnedOffInternal(int why) {
        resetKeyStatus();
        this.mScreenOffReason = why;
    }

    public void notifyLidSwitchChanged(long whenNanos, boolean lidOpen) {
        this.mSmartCoverManager.notifyLidSwitchChanged(lidOpen, this.mFolded);
        MiuiPadKeyboardManager miuiPadKeyboardManager = this.mMiuiPadKeyboardManager;
        if (miuiPadKeyboardManager != null) {
            miuiPadKeyboardManager.notifyLidSwitchChanged(lidOpen);
        }
        PadManager.getInstance().setIsLidOpen(lidOpen);
    }

    private void releaseScreenOnProximitySensor(boolean isNowRelease) {
        MiuiScreenOnProximityLock miuiScreenOnProximityLock = this.mProximitySensor;
        if (miuiScreenOnProximityLock != null) {
            miuiScreenOnProximityLock.release(isNowRelease);
        }
    }

    public long interceptKeyBeforeDispatching(IBinder focusedToken, KeyEvent event, int policyFlags) {
        int repeatCount = event.getRepeatCount();
        boolean down = event.getAction() == 0;
        boolean canceled = event.isCanceled();
        MiuiKeyguardServiceDelegate miuiKeyguardServiceDelegate = this.mMiuiKeyguardDelegate;
        boolean keyguardActive = miuiKeyguardServiceDelegate != null && miuiKeyguardServiceDelegate.isShowingAndNotHidden();
        if (this.mMiuiKeyInterceptExtend.skipInterceptKeyLocked(event, this.mFocusedWindow)) {
            MiuiInputLog.defaults("Skip intercept this key for miui+ at before dispatching");
            return 0L;
        }
        WindowState winStateFromInputWinMap = WindowStateStubImpl.getWinStateFromInputWinMap(this.mWindowManager, focusedToken);
        if (down && repeatCount == 0) {
            this.mWin = winStateFromInputWinMap;
        }
        int keyCode = event.getKeyCode();
        if (keyCode == 82) {
            if (this.mTestModeEnabled) {
                MiuiInputLog.major("Ignoring MENU because mTestModeEnabled = " + this.mTestModeEnabled);
                return 0L;
            } else if (isLockDeviceWindow(winStateFromInputWinMap)) {
                MiuiInputLog.major("device locked, pass MENU to lock window");
                return 0L;
            } else if (!this.mMiuiKeyShortcutManager.mPressToAppSwitch) {
                MiuiInputLog.major("is show menu");
                return 0L;
            } else if (!this.mRequestShowMenu) {
                if (!keyguardOn()) {
                    if (down) {
                        sendFullScreenStateToTaskSnapshot();
                        preloadRecentApps();
                    } else if (canceled) {
                        cancelPreloadRecentAppsInternal();
                    } else {
                        m1384x3ade717a();
                    }
                }
                return -1L;
            } else if (repeatCount != 0) {
                return -1L;
            } else {
                if (!down) {
                    this.mRequestShowMenu = false;
                }
                return 0L;
            }
        } else if (keyCode == 3) {
            if (this.mTestModeEnabled) {
                MiuiInputLog.major("Ignoring HOME because mTestModeEnabled = " + this.mTestModeEnabled);
                return 0L;
            } else if (isLockDeviceWindow(winStateFromInputWinMap)) {
                MiuiInputLog.major("device locked, pass HOME to lock window");
                return 0L;
            } else {
                if (!down) {
                    if (this.mHomeConsumed) {
                        this.mHomeConsumed = false;
                        return -1L;
                    } else if (!canceled && phoneWindowManagerFeature.isScreenOnFully(this)) {
                        TelecomManager telecomManager = getTelecommService();
                        if (!this.mWifiOnly && telecomManager != null && telecomManager.isRinging() && isInCallScreenShowing()) {
                            MiuiInputLog.major("Ignoring HOME; there's a ringing incoming call.");
                        } else {
                            if (keyguardActive) {
                                StatusBarManager sbm = (StatusBarManager) this.mContext.getSystemService("statusbar");
                                sbm.collapsePanels();
                            }
                            if (!this.mKeyguardOnWhenHomeDown) {
                                if (this.mMiuiKeyShortcutManager.mDoubleTapOnHomeBehavior != 0 && !keyguardActive) {
                                    this.mHandler.removeCallbacks(this.mHomeDoubleTapTimeoutRunnable);
                                    this.mHomeDoubleTapPending = true;
                                    this.mHandler.postDelayed(this.mHomeDoubleTapTimeoutRunnable, 300L);
                                    return -1L;
                                }
                                MiuiInputLog.error("before go to home");
                                Intent intent = new Intent("com.miui.launch_home_from_hotkey");
                                sendAsyncBroadcast(intent, PERMISSION_INTERNAL_GENERAL_API);
                                launchHomeFromHotKey(0);
                            } else {
                                MiuiInputLog.major("Ignoring HOME; keyguard is on when first Home down");
                            }
                        }
                    } else {
                        MiuiInputLog.error("Ignoring HOME; event canceled.");
                    }
                } else {
                    sendFullScreenStateToHome();
                    WindowManager.LayoutParams attrs = winStateFromInputWinMap != null ? (WindowManager.LayoutParams) ReflectionUtils.callPrivateMethod(winStateFromInputWinMap, "getAttrs", new Object[0]) : null;
                    if (attrs != null) {
                        int type = attrs.type;
                        if (type == 2004 || type == 2009) {
                            return 0L;
                        }
                        int typeCount = WINDOW_TYPES_WHERE_HOME_DOESNT_WORK.length;
                        for (int i = 0; i < typeCount; i++) {
                            if (type == WINDOW_TYPES_WHERE_HOME_DOESNT_WORK[i]) {
                                removeKeyLongPress(keyCode);
                            }
                        }
                    }
                }
                if (repeatCount == 0) {
                    if (this.mHomeDoubleTapPending) {
                        this.mHomeDoubleTapPending = false;
                        this.mHandler.removeCallbacks(this.mHomeDoubleTapTimeoutRunnable);
                        handleDoubleTapOnHome();
                        return -1L;
                    } else if (this.mMiuiKeyShortcutManager.mDoubleTapOnHomeBehavior == 1) {
                        preloadRecentApps();
                        return -1L;
                    } else {
                        return -1L;
                    }
                }
                return -1L;
            }
        } else if (keyCode == 25 && this.mVolumeDownKeyConsumed) {
            if (!down) {
                this.mVolumeDownKeyConsumed = false;
                return -1L;
            }
            return -1L;
        } else if (keyCode == 24 && this.mVolumeUpKeyConsumed) {
            if (!down) {
                this.mVolumeUpKeyConsumed = false;
                return -1L;
            }
            return -1L;
        } else {
            MiuiStylusPageKeyListener miuiStylusPageKeyListener = this.mMiuiStylusPageKeyListener;
            if (miuiStylusPageKeyListener != null && miuiStylusPageKeyListener.needInterceptBeforeDispatching(event)) {
                return this.mMiuiStylusPageKeyListener.getDelayTime(event);
            }
            if (down && repeatCount == 0 && !keyguardOn() && isMiPad() && handleCustomizeShortcut(event)) {
                return -1L;
            }
            if (event.isMetaPressed() && keyCode != 117 && keyCode != 118 && isMiPad()) {
                return 0L;
            }
            if (this.mMiuiKeyInterceptExtend.interceptKeyBeforeDispatching(event)) {
                return -1L;
            }
            return super.interceptKeyBeforeDispatching(focusedToken, event, policyFlags);
        }
    }

    private boolean isInCallScreenShowing() {
        ActivityManager activityManager = (ActivityManager) this.mContext.getSystemService("activity");
        String runningActivity = activityManager.getRunningTasks(1).get(0).topActivity.getClassName();
        return "com.android.phone.MiuiInCallScreen".equals(runningActivity) || "com.android.incallui.InCallActivity".equals(runningActivity);
    }

    public void markShortcutTriggered() {
        this.mShortcutTriggered = true;
        this.mShortcutPressing |= this.mKeyPressing;
        phoneWindowManagerFeature.callInterceptPowerKeyUp(this, false);
    }

    private boolean handleKeyCombination() {
        int i = this.mKeyPressed;
        if (i != this.mKeyPressing || this.mShortcutTriggered) {
            return false;
        }
        if (i == SHORTCUT_HOME_POWER) {
            boolean result = stopLockTaskMode();
            return postKeyFunction(this.mMiuiKeyShortcutManager.getFunction("key_combination_power_home"), 0, "key_combination_power_home") | result;
        } else if (i == SHORTCUT_BACK_POWER && !this.mMiuiKeyShortcutManager.mSingleKeyUse) {
            boolean result2 = postKeyFunction(this.mMiuiKeyShortcutManager.getFunction("key_combination_power_back"), 0, "key_combination_power_back");
            return result2;
        } else if (this.mKeyPressed == SHORTCUT_MENU_POWER && !this.mMiuiKeyShortcutManager.mSingleKeyUse) {
            boolean result3 = postKeyFunction(this.mMiuiKeyShortcutManager.getFunction("key_combination_power_menu"), 0, "key_combination_power_menu");
            return result3;
        } else if (this.mKeyPressed == SHORTCUT_SCREENSHOT_MIUI && !this.mMiuiKeyShortcutManager.mSingleKeyUse) {
            boolean result4 = postKeyFunction("screen_shot", 0, null);
            return result4;
        } else if (this.mKeyPressed == SHORTCUT_SCREENSHOT_SINGLE_KEY && this.mMiuiKeyShortcutManager.mSingleKeyUse) {
            boolean result5 = postKeyFunction("screen_shot", 0, null);
            return result5;
        } else {
            MiuiScreenOnProximityLock miuiScreenOnProximityLock = this.mProximitySensor;
            if (miuiScreenOnProximityLock == null || !miuiScreenOnProximityLock.isHeld()) {
                return false;
            }
            if ((this.mKeyPressed != SHORTCUT_UNLOCK && (!hasNavigationBar() || (this.mKeyPressed & getKeyBitmask(24)) == 0)) || this.mMiuiKeyShortcutManager.mSingleKeyUse) {
                return false;
            }
            releaseScreenOnProximitySensor(false);
            return true;
        }
    }

    public static void sendRecordCountEvent(Context context, String category, String event) {
        Intent intent = new Intent("com.miui.gallery.intent.action.SEND_STAT");
        intent.setPackage(AccessController.PACKAGE_GALLERY);
        intent.putExtra("stat_type", "count_event");
        intent.putExtra("category", category);
        intent.putExtra("event", event);
        context.sendBroadcast(intent);
    }

    private Toast makeAllUserToastAndShow(String text, int duration) {
        Toast toast = Toast.makeText(this.mContext, text, duration);
        toast.show();
        return toast;
    }

    protected boolean stopLockTaskMode() {
        return false;
    }

    protected boolean isInLockTaskMode() {
        return false;
    }

    private void resetKeyStatus() {
        this.mKeyPressed = 0;
        this.mKeyPressing = 0;
        this.mShortcutPressing = 0;
        this.mShortcutTriggered = false;
    }

    public void startAiKeyService(String pressType) {
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.android.settings", "com.android.settings.ai.AidaemonService"));
            intent.putExtra("key_ai_button_settings", pressType);
            this.mContext.startServiceAsUser(intent, UserHandle.CURRENT);
        } catch (Exception e) {
            Slog.e("WindowManager", e.toString());
        }
    }

    private void handleAiKeyEvent(KeyEvent event, boolean down) {
        if (down) {
            long keyDownTime = SystemClock.uptimeMillis();
            if (event.getRepeatCount() == 0) {
                this.mDoubleClickAiKeyIsConsumed = false;
                this.mLongPressAiKeyIsConsumed = false;
                this.mDoubleClickAiKeyCount++;
                this.mHandler.postDelayed(this.mLongPressDownAiKeyRunnable, 500L);
            }
            if (keyDownTime - this.mLastClickAiKeyTime < 300 && this.mDoubleClickAiKeyCount == 2) {
                this.mHandler.post(this.mDoubleClickAiKeyRunnable);
                this.mDoubleClickAiKeyIsConsumed = true;
                this.mDoubleClickAiKeyCount = 0;
                this.mHandler.removeCallbacks(this.mSingleClickAiKeyRunnable);
                this.mHandler.removeCallbacks(this.mLongPressDownAiKeyRunnable);
            }
            this.mLastClickAiKeyTime = keyDownTime;
        } else if (this.mLongPressAiKeyIsConsumed) {
            this.mHandler.post(this.mLongPressUpAiKeyRunnable);
        } else if (!this.mDoubleClickAiKeyIsConsumed) {
            this.mHandler.postDelayed(this.mSingleClickAiKeyRunnable, 300L);
            this.mHandler.removeCallbacks(this.mLongPressDownAiKeyRunnable);
        }
    }

    public int intercept(KeyEvent event, int policyFlags, boolean isScreenOn, int expectedResult) {
        boolean down = event.getAction() == 0;
        if (!down) {
            cancelEventAndCallSuperQueueing(event, policyFlags, isScreenOn);
        }
        return expectedResult;
    }

    protected void registerProximitySensor() {
        this.mHandler.post(new Runnable() { // from class: com.android.server.policy.BaseMiuiPhoneWindowManager.9
            @Override // java.lang.Runnable
            public void run() {
                if (BaseMiuiPhoneWindowManager.this.mMiuiPocketModeManager == null) {
                    BaseMiuiPhoneWindowManager baseMiuiPhoneWindowManager = BaseMiuiPhoneWindowManager.this;
                    baseMiuiPhoneWindowManager.mMiuiPocketModeManager = new MiuiPocketModeManager(baseMiuiPhoneWindowManager.mContext);
                }
                BaseMiuiPhoneWindowManager.this.mMiuiPocketModeManager.registerListener(BaseMiuiPhoneWindowManager.this.mWakeUpKeySensorListener);
            }
        });
    }

    private boolean shouldInterceptKey(KeyEvent event, int policyFlags, boolean isScreenOn) {
        int keyCode = event.getKeyCode();
        if (this.mIsVRMode) {
            MiuiInputLog.major("VR mode drop all keys.");
            return true;
        } else if (SystemProperties.getInt("sys.in_shutdown_progress", 0) == 1) {
            MiuiInputLog.major("this device is being shut down, ignore key event.");
            return true;
        } else if (!isScreenOn && (4 == keyCode || 82 == keyCode)) {
            MiuiInputLog.major("Cancel back or menu key when screen is off");
            cancelEventAndCallSuperQueueing(event, policyFlags, false);
            return true;
        } else {
            InputDevice inputDevice = event.getDevice();
            if (inputDevice == null || inputDevice.getProductId() != 1576) {
                return false;
            }
            ShoulderKeyManagerInternal shoulderKeyManagerInternal = this.mShoulderKeyManagerInternal;
            if (shoulderKeyManagerInternal != null) {
                shoulderKeyManagerInternal.handleShoulderKeyEvent(event);
            }
            return true;
        }
    }

    private void cancelEventAndCallSuperQueueing(KeyEvent event, int policyFlags, boolean isScreenOn) {
        callSuperInterceptKeyBeforeQueueing(KeyEvent.changeFlags(event, event.getFlags() | 32), policyFlags, isScreenOn);
    }

    private boolean markKeyPressAndHandleKeyCombin(int keyCode, boolean down) {
        int firstKeyCode;
        if (this.mKeyPressing == 0) {
            resetKeyStatus();
        }
        int keyBitMask = getKeyBitmask(keyCode);
        int i = this.mKeyPressed;
        if (i != 0 && (firstKeyCode = getCodeByKeyPressed(i)) != 0) {
            removeKeyLongPress(firstKeyCode);
        }
        if (down) {
            this.mKeyPressed |= keyBitMask;
            this.mKeyPressing |= keyBitMask;
        } else {
            this.mKeyPressing &= ~keyBitMask;
        }
        if (handleKeyCombination()) {
            return true;
        }
        if (this.mShortcutTriggered && (this.mShortcutPressing & keyBitMask) != 0 && !down) {
            removeKeyLongPress(keyCode);
            this.mShortcutPressing &= ~keyBitMask;
            MiuiInputLog.detail("remove keyCode:" + keyCode + ",shortcut is trriggered");
            return true;
        }
        return false;
    }

    private void notifyPowerKeeperKeyEvent(KeyEvent event) {
        Bundle b = new Bundle();
        b.putInt("eventcode", event.getKeyCode());
        b.putBoolean("down", event.getAction() == 0);
        PowerKeeperPolicy.getInstance().notifyKeyEvent(b);
    }

    private void handleMetaKey(KeyEvent event) {
        boolean down = event.getAction() == 0;
        int keyCode = event.getKeyCode();
        if (KeyEvent.isMetaKey(keyCode)) {
            if (!this.mMiuiKeyShortcutManager.getEnableKsFeature() || !isUserSetupComplete()) {
                MiuiInputLog.major("Not handle meta key");
            } else if (down) {
                this.mHandler.removeCallbacks(this.mMetaKeyRunnable);
                this.mKeyBoardDeviceId = event.getDeviceId();
                if (this.mMetaKeyConsume) {
                    this.mMetaKeyConsume = false;
                    this.mMetaKeyAction = 2;
                    this.mHandler.postDelayed(this.mMetaKeyRunnable, 300L);
                    return;
                }
                this.mMetaKeyAction = 1;
                this.mHandler.post(this.mMetaKeyRunnable);
            } else if (!this.mMetaKeyConsume && this.mMetaKeyAction != 1) {
                this.mMetaKeyAction = 0;
            } else if (this.mSystemShortcutsMenuShown) {
                showKeyboardShortcutsMenu(this.mKeyBoardDeviceId, true, false);
            }
        } else if (!this.mMetaKeyConsume) {
            this.mMetaKeyConsume = true;
        } else if (this.mSystemShortcutsMenuShown) {
            showKeyboardShortcutsMenu(this.mKeyBoardDeviceId, true, false);
        }
    }

    private boolean handleCustomizeShortcut(KeyEvent event) {
        if (!this.mMiuiKeyShortcutManager.getEnableKsFeature()) {
            return false;
        }
        long shortcutCode = event.getKeyCode();
        if (event.isCtrlPressed()) {
            shortcutCode |= 17592186044416L;
        }
        if (event.isAltPressed()) {
            shortcutCode |= 8589934592L;
        }
        if (event.isShiftPressed()) {
            shortcutCode |= 4294967296L;
        }
        if (event.isMetaPressed()) {
            shortcutCode |= 281474976710656L;
        }
        final MiuiCustomizeShortCutUtils.MiuiKeyboardShortcutInfo info = MiuiCustomizeShortCutUtils.getInstance(this.mContext).getShortcut(shortcutCode);
        if (info == null || !info.isEnable()) {
            return false;
        }
        if (!isUserSetupComplete()) {
            MiuiInputLog.major("Not trigger shortcut, user setup not complete");
            return false;
        }
        InputOneTrackUtil.getInstance(this.mContext).trackKeyboardEvent(MiuiCustomizeShortCutUtils.getInstance(this.mContext).getShortCutNameByType(info));
        if (info.getType() == 1) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.policy.BaseMiuiPhoneWindowManager$$ExternalSyntheticLambda4
                @Override // java.lang.Runnable
                public final void run() {
                    BaseMiuiPhoneWindowManager.this.m1379xb854f85f();
                }
            });
        } else if (info.getType() == 2) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.policy.BaseMiuiPhoneWindowManager$$ExternalSyntheticLambda5
                @Override // java.lang.Runnable
                public final void run() {
                    BaseMiuiPhoneWindowManager.this.m1380xd27076fe();
                }
            });
        } else if (info.getType() == 5) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.policy.BaseMiuiPhoneWindowManager$$ExternalSyntheticLambda6
                @Override // java.lang.Runnable
                public final void run() {
                    BaseMiuiPhoneWindowManager.this.m1381xec8bf59d(info);
                }
            });
        } else if (info.getType() == 7) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.policy.BaseMiuiPhoneWindowManager$$ExternalSyntheticLambda7
                @Override // java.lang.Runnable
                public final void run() {
                    BaseMiuiPhoneWindowManager.this.m1382x6a7743c(info);
                }
            });
        } else if (info.getType() == 6) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.policy.BaseMiuiPhoneWindowManager$$ExternalSyntheticLambda8
                @Override // java.lang.Runnable
                public final void run() {
                    BaseMiuiPhoneWindowManager.this.m1383x20c2f2db(info);
                }
            });
        } else if (info.getType() == 3) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.policy.BaseMiuiPhoneWindowManager$$ExternalSyntheticLambda9
                @Override // java.lang.Runnable
                public final void run() {
                    BaseMiuiPhoneWindowManager.this.m1384x3ade717a();
                }
            });
        } else if (info.getType() == 4) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.policy.BaseMiuiPhoneWindowManager$$ExternalSyntheticLambda10
                @Override // java.lang.Runnable
                public final void run() {
                    BaseMiuiPhoneWindowManager.this.m1385x54f9f019();
                }
            });
        } else if (info.getType() == 0) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.policy.BaseMiuiPhoneWindowManager$$ExternalSyntheticLambda11
                @Override // java.lang.Runnable
                public final void run() {
                    BaseMiuiPhoneWindowManager.this.m1386x6f156eb8(info);
                }
            });
        } else if (info.getType() != 8) {
            return false;
        } else {
            this.mHandler.post(new Runnable() { // from class: com.android.server.policy.BaseMiuiPhoneWindowManager$$ExternalSyntheticLambda12
                @Override // java.lang.Runnable
                public final void run() {
                    ActivityTaskManagerServiceStub.get().onMetaKeyCombination();
                }
            });
        }
        return true;
    }

    /* renamed from: lambda$handleCustomizeShortcut$0$com-android-server-policy-BaseMiuiPhoneWindowManager */
    public /* synthetic */ void m1379xb854f85f() {
        ShortCutActionsUtils.getInstance(this.mContext).launchControlCenter();
    }

    /* renamed from: lambda$handleCustomizeShortcut$1$com-android-server-policy-BaseMiuiPhoneWindowManager */
    public /* synthetic */ void m1380xd27076fe() {
        ShortCutActionsUtils.getInstance(this.mContext).launchNotificationCenter();
    }

    /* renamed from: lambda$handleCustomizeShortcut$2$com-android-server-policy-BaseMiuiPhoneWindowManager */
    public /* synthetic */ void m1381xec8bf59d(MiuiCustomizeShortCutUtils.MiuiKeyboardShortcutInfo info) {
        ShortCutActionsUtils.getInstance(this.mContext).triggerFunction("go_to_sleep", MiuiKeyShortcutManager.ACTION_KEYBOARD + info.getShortcutKeyCode(), null, false);
    }

    /* renamed from: lambda$handleCustomizeShortcut$3$com-android-server-policy-BaseMiuiPhoneWindowManager */
    public /* synthetic */ void m1382x6a7743c(MiuiCustomizeShortCutUtils.MiuiKeyboardShortcutInfo info) {
        ShortCutActionsUtils.getInstance(this.mContext).triggerFunction("partial_screen_shot", MiuiKeyShortcutManager.ACTION_KEYBOARD + info.getShortcutKeyCode(), null, false);
    }

    /* renamed from: lambda$handleCustomizeShortcut$4$com-android-server-policy-BaseMiuiPhoneWindowManager */
    public /* synthetic */ void m1383x20c2f2db(MiuiCustomizeShortCutUtils.MiuiKeyboardShortcutInfo info) {
        ShortCutActionsUtils.getInstance(this.mContext).triggerFunction("screen_shot", MiuiKeyShortcutManager.ACTION_KEYBOARD + info.getShortcutKeyCode(), null, false);
    }

    /* renamed from: lambda$handleCustomizeShortcut$6$com-android-server-policy-BaseMiuiPhoneWindowManager */
    public /* synthetic */ void m1385x54f9f019() {
        launchHomeFromHotKey(0);
    }

    /* renamed from: lambda$handleCustomizeShortcut$7$com-android-server-policy-BaseMiuiPhoneWindowManager */
    public /* synthetic */ void m1386x6f156eb8(MiuiCustomizeShortCutUtils.MiuiKeyboardShortcutInfo info) {
        this.mContext.startActivityAsUser(info.getIntent(), UserHandle.CURRENT);
    }

    private boolean isMiPad() {
        return PadManager.getInstance().isPad();
    }

    public void showRecentApps(boolean triggeredFromAltTab) {
        this.mPreloadedRecentApps = false;
        StatusBarManagerInternal statusbar = getStatusBarManagerInternal();
        if (statusbar != null) {
            statusbar.showRecentApps(triggeredFromAltTab);
        }
    }

    public void showKeyboardShortcutsMenu(int deviceId, boolean system, boolean show) {
        MiuiInputLog.major("toggleKeyboardShortcutsMenu ,deviceId: " + deviceId + "  system:" + system + " show:" + show);
        if (system) {
            this.mSystemShortcutsMenuShown = show;
        }
        Intent intent = new Intent("com.miui.systemui.action.KEYBOARD_SHORTCUTS");
        intent.setPackage("com.android.systemui");
        intent.putExtra("system", system);
        intent.putExtra("show", show);
        intent.putExtra("deviceId", deviceId);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
    }

    /* JADX WARN: Removed duplicated region for block: B:240:0x03ff A[RETURN] */
    /* JADX WARN: Removed duplicated region for block: B:241:0x0401  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public int interceptKeyBeforeQueueingInternal(android.view.KeyEvent r17, int r18, boolean r19) {
        /*
            Method dump skipped, instructions count: 1070
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.policy.BaseMiuiPhoneWindowManager.interceptKeyBeforeQueueingInternal(android.view.KeyEvent, int, boolean):int");
    }

    private void interceptAccessibilityShortcutChord(boolean keyguardActive) {
        if (this.mVolumeDownKeyPressed && this.mVolumeUpKeyPressed && this.mTalkBackIsOpened) {
            long now = SystemClock.uptimeMillis();
            if (now <= this.mVolumeDownKeyTime + 150 && now <= this.mVolumeUpKeyTime + 150) {
                this.mVolumeDownKeyConsumed = true;
                this.mVolumeUpKeyConsumed = true;
                if (!this.mShortcutServiceIsTalkBack || (keyguardActive && !this.mAccessibilityShortcutOnLockScreen)) {
                    Handler handler = this.mHandler;
                    handler.sendMessageDelayed(handler.obtainMessage(1, "close_talkback"), 3000L);
                }
            }
        }
    }

    private void cancelPendingAccessibilityShortcutAction() {
        this.mHandler.removeMessages(1, "close_talkback");
    }

    private boolean closeTorchWhenScreenOff(boolean isScreenOn) {
        if (!this.mTorchEnabled || isScreenOn) {
            return false;
        }
        Bundle torchBundle = new Bundle();
        torchBundle.putBoolean(ShortCutActionsUtils.EXTRA_TORCH_ENABLED, false);
        ShortCutActionsUtils.getInstance(this.mContext).triggerFunction("turn_on_torch", ShortCutActionsUtils.REASON_OF_TRIGGERED_TORCH_BY_POWER, torchBundle, true);
        return true;
    }

    private void streetSnap(boolean isScreenOn, int keyCode, boolean down, KeyEvent event) {
        if (!isScreenOn && this.mLongPressVolumeDownBehavior == 1) {
            Intent keyIntent = null;
            if (keyCode == 24 || keyCode == 25) {
                keyIntent = new Intent("miui.intent.action.CAMERA_KEY_BUTTON");
            } else if (down && keyCode == 26) {
                keyIntent = new Intent("android.intent.action.KEYCODE_POWER_UP");
            }
            if (keyIntent != null) {
                keyIntent.setClassName(AccessController.PACKAGE_CAMERA, "com.android.camera.snap.SnapKeyReceiver");
                keyIntent.putExtra("key_code", keyCode);
                keyIntent.putExtra("key_action", event.getAction());
                keyIntent.putExtra("key_event_time", event.getEventTime());
                this.mContext.sendBroadcastAsUser(keyIntent, UserHandle.CURRENT);
            }
        }
    }

    private boolean shouldInterceptHeadSetHookKey(int keyCode, KeyEvent event) {
        if (this.mMikeymodeEnabled && keyCode == 79) {
            Intent mikeyIntent = new Intent("miui.intent.action.MIKEY_BUTTON");
            mikeyIntent.setPackage("com.xiaomi.miclick");
            mikeyIntent.putExtra("key_action", event.getAction());
            mikeyIntent.putExtra("key_event_time", event.getEventTime());
            this.mContext.sendBroadcast(mikeyIntent);
            return true;
        }
        return false;
    }

    private boolean sendOthersBroadcast(boolean down, boolean isScreenOn, boolean keyguardActive, int keyCode, KeyEvent event) {
        IStatusBarService statusBarService;
        if (down) {
            if (isScreenOn && !keyguardActive && (keyCode == 26 || keyCode == 25 || keyCode == 24 || keyCode == 164 || keyCode == 85 || keyCode == 79)) {
                Intent i = new Intent("miui.intent.action.KEYCODE_EXTERNAL");
                i.putExtra("android.intent.extra.KEY_EVENT", event);
                i.addFlags(1073741824);
                sendAsyncBroadcast(i);
            }
            boolean stopNotification = keyCode == 26;
            if (!stopNotification && keyguardActive && (keyCode == 25 || keyCode == 24 || keyCode == 164)) {
                stopNotification = true;
            }
            if (stopNotification && this.mSystemReady && (statusBarService = getStatusBarService()) != null) {
                onStatusBarPanelRevealed(statusBarService);
            }
            if (keyCode == 25 || keyCode == 24) {
                ContentResolver cr = this.mContext.getContentResolver();
                String proc = Settings.System.getString(cr, "remote_control_proc_name");
                String pkg = Settings.System.getString(cr, "remote_control_pkg_name");
                if (proc != null && pkg != null) {
                    SystemClock.uptimeMillis();
                    boolean running = checkProcessRunning(proc);
                    if (!running) {
                        Settings.System.putString(cr, "remote_control_proc_name", null);
                        Settings.System.putString(cr, "remote_control_pkg_name", null);
                    } else {
                        Intent i2 = new Intent("miui.intent.action.REMOTE_CONTROL");
                        i2.setPackage(pkg);
                        i2.addFlags(1073741824);
                        i2.putExtra("android.intent.extra.KEY_EVENT", event);
                        sendAsyncBroadcast(i2);
                        return true;
                    }
                }
            }
        } else if (keyCode == 26) {
            sendAsyncBroadcast(new Intent("android.intent.action.KEYCODE_POWER_UP"));
        }
        return false;
    }

    private boolean inFingerprintEnrolling() {
        String topClassName;
        ActivityManager am = (ActivityManager) this.mContext.getSystemService("activity");
        try {
            topClassName = am.getRunningTasks(1).get(0).topActivity.getClassName();
        } catch (Exception e) {
            MiuiInputLog.error("Exception", e);
        }
        return "com.android.settings.NewFingerprintInternalActivity".equals(topClassName);
    }

    public void setStatusBarInFullscreen(boolean show) {
        this.mIsStatusBarVisibleInFullscreen = show;
        try {
            IStatusBarService statusbar = getStatusBarService();
            if (statusbar != null) {
                statusbar.disable(show ? Integer.MIN_VALUE : 0, this.mBinder, "android");
            }
        } catch (RemoteException e) {
            Slog.e("WindowManager", "RemoteException", e);
            this.mStatusBarService = null;
        }
        try {
            this.mWindowManager.updateRotation(false, true);
        } catch (RemoteException e2) {
            Slog.e("WindowManager", "RemoteException", e2);
        }
    }

    protected void registerStatusBarInputEventReceiver() {
    }

    protected void unregisterStatusBarInputEventReceiver() {
    }

    /* loaded from: classes.dex */
    protected class StatusBarPointEventTracker {
        private float mDownX = -1.0f;
        private float mDownY = -1.0f;

        public StatusBarPointEventTracker() {
            BaseMiuiPhoneWindowManager.this = this$0;
        }

        protected void onTrack(MotionEvent motionEvent) {
            switch (motionEvent.getActionMasked()) {
                case 0:
                    float statusBarExpandHeight = motionEvent.getRawX();
                    this.mDownX = statusBarExpandHeight;
                    this.mDownY = motionEvent.getRawY();
                    return;
                case 1:
                case 2:
                case 3:
                    float statusBarExpandHeight2 = BaseMiuiPhoneWindowManager.this.mContext.getResources().getFraction(285802496, 0, 0);
                    float f = this.mDownY;
                    if (statusBarExpandHeight2 >= f && f != -1.0f) {
                        float distanceX = Math.abs(this.mDownX - motionEvent.getRawX());
                        float distanceY = Math.abs(this.mDownY - motionEvent.getRawY());
                        if (2.0f * distanceX <= distanceY && MiuiFreeformPinManagerService.EDGE_AREA <= distanceY) {
                            BaseMiuiPhoneWindowManager.this.setStatusBarInFullscreen(true);
                            this.mDownY = MiuiFreeformPinManagerService.EDGE_AREA;
                            return;
                        }
                        return;
                    }
                    return;
                default:
                    return;
            }
        }
    }

    public void takeScreenshot(final Intent intent, final String function) {
        this.mHandler.postDelayed(new Runnable() { // from class: com.android.server.policy.BaseMiuiPhoneWindowManager$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                BaseMiuiPhoneWindowManager.this.m1388xe44c6bf0(function, intent);
            }
        }, intent.getLongExtra("capture_delay", 0L));
    }

    /* renamed from: lambda$takeScreenshot$9$com-android-server-policy-BaseMiuiPhoneWindowManager */
    public /* synthetic */ void m1388xe44c6bf0(String function, Intent intent) {
        ShortCutActionsUtils.getInstance(this.mContext).triggerFunction(function, intent.getStringExtra("shortcut"), null, false);
    }

    private void postKeyLongPress(int keyCode, boolean underKeyguard) {
        switch (keyCode) {
            case 3:
                if (this.mFrontFingerprintSensor && ((this.mSupportTapFingerprintSensorToHome || "capricorn".equals(Build.DEVICE)) && underKeyguard)) {
                    return;
                }
                if (!this.mMiuiKeyShortcutManager.supportAOSPTriggerFunction(3)) {
                    MiuiInputLog.major("Aosp not support long press home");
                    return;
                } else {
                    postKeyFunction(this.mMiuiKeyShortcutManager.getFunction("long_press_home_key"), this.mKeyLongPressTimeout, "long_press_home_key");
                    return;
                }
            case 4:
                postKeyFunction(this.mMiuiKeyShortcutManager.getFunction("long_press_back_key"), this.mKeyLongPressTimeout, "long_press_back_key");
                return;
            case 26:
                postKeyFunction(this.mMiuiKeyShortcutManager.getFunction("long_press_power_key"), this.mMiuiKeyShortcutManager.mLongPressPowerTime, "long_press_power_key");
                postKeyFunction(this.mImperceptiblePowerKey, (int) getImperceptiblePowerKeyTimeOut(), "imperceptible_press_power_key");
                return;
            case UsbKeyboardUtil.COMMAND_READ_KEYBOARD /* 82 */:
            case 187:
                if (underKeyguard && !TextUtils.isEmpty(this.mMiuiKeyShortcutManager.mLongPressMenuKeyWhenLock) && !"none".equals(this.mMiuiKeyShortcutManager.mLongPressMenuKeyWhenLock)) {
                    postKeyFunction(this.mMiuiKeyShortcutManager.mLongPressMenuKeyWhenLock, this.mKeyLongPressTimeout, "long_press_menu_key_when_lock");
                    return;
                } else if (!this.mMiuiKeyShortcutManager.mPressToAppSwitch && "show_menu".equals(this.mMiuiKeyShortcutManager.getFunction("long_press_menu_key"))) {
                    return;
                } else {
                    String action = this.mMiuiKeyShortcutManager.getFunction("long_press_menu_key");
                    int delay = this.mKeyLongPressTimeout;
                    if ("show_menu".equals(action)) {
                        delay += 50;
                    }
                    postKeyFunction(action, delay, "long_press_menu_key");
                    return;
                }
            default:
                return;
        }
    }

    private void removeKeyLongPress(int keyCode) {
        switch (keyCode) {
            case 3:
                removeKeyFunction(this.mMiuiKeyShortcutManager.getFunction("long_press_home_key"));
                return;
            case 4:
                removeKeyFunction(this.mMiuiKeyShortcutManager.getFunction("long_press_back_key"));
                return;
            case 26:
                removeKeyFunction(this.mMiuiKeyShortcutManager.getFunction("long_press_power_key"));
                removeKeyFunction(this.mImperceptiblePowerKey);
                return;
            case UsbKeyboardUtil.COMMAND_READ_KEYBOARD /* 82 */:
            case 187:
                removeKeyFunction(this.mMiuiKeyShortcutManager.getFunction("long_press_menu_key"));
                removeKeyFunction(this.mMiuiKeyShortcutManager.mLongPressMenuKeyWhenLock);
                return;
            default:
                return;
        }
    }

    private boolean postKeyFunction(String action, int delay, String shortcut) {
        if (TextUtils.isEmpty(action)) {
            return false;
        }
        Message message = this.mHandler.obtainMessage(1, action);
        Bundle bundle = new Bundle();
        bundle.putString("shortcut", shortcut);
        message.setData(bundle);
        this.mHandler.sendMessageDelayed(message, delay);
        return true;
    }

    private void removeKeyFunction(String action) {
        this.mHandler.removeMessages(1, action);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class H extends Handler {
        static final int MSG_DISPATCH_SHOW_RECENTS = 3;
        static final int MSG_KEY_DELAY_POWER = 2;
        static final int MSG_KEY_FUNCTION = 1;

        private H() {
            BaseMiuiPhoneWindowManager.this = r1;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            if (BaseMiuiPhoneWindowManager.this.mShortcutTriggered) {
                return;
            }
            if (msg.what == 2) {
                BaseMiuiPhoneWindowManager.this.callSuperInterceptKeyBeforeQueueing((KeyEvent) msg.getData().getParcelable(BaseMiuiPhoneWindowManager.LAST_POWER_UP_EVENT), msg.getData().getInt(BaseMiuiPhoneWindowManager.LAST_POWER_UP_POLICY), msg.getData().getBoolean(BaseMiuiPhoneWindowManager.LAST_POWER_UP_SCREEN_STATE));
            } else if (msg.what == 1) {
                if (!BaseMiuiPhoneWindowManager.this.mPowerManager.isScreenOn()) {
                    return;
                }
                String shortcut = msg.getData().getString("shortcut");
                boolean triggered = false;
                String action = (String) msg.obj;
                if (action == null) {
                    return;
                }
                if ("launch_camera".equals(action)) {
                    triggered = ShortCutActionsUtils.getInstance(BaseMiuiPhoneWindowManager.this.mContext).triggerFunction("launch_camera", shortcut, null, true);
                } else {
                    boolean phoneIdle = false;
                    if ("screen_shot".equals(action)) {
                        triggered = ShortCutActionsUtils.getInstance(BaseMiuiPhoneWindowManager.this.mContext).triggerFunction("screen_shot", shortcut, null, false);
                        BaseMiuiPhoneWindowManager.sendRecordCountEvent(BaseMiuiPhoneWindowManager.this.mContext, MiuiCustomizeShortCutUtils.SCREENSHOT, "key_shortcut");
                    } else if ("partial_screen_shot".equals(action)) {
                        triggered = ShortCutActionsUtils.getInstance(BaseMiuiPhoneWindowManager.this.mContext).triggerFunction("partial_screen_shot", shortcut, null, false);
                    } else if ("launch_voice_assistant".equals(action)) {
                        if (!"long_press_power_key".equals(shortcut)) {
                            triggered = ShortCutActionsUtils.getInstance(BaseMiuiPhoneWindowManager.this.mContext).triggerFunction("launch_voice_assistant", shortcut, null, true);
                        } else {
                            Bundle voiceBundle = new Bundle();
                            voiceBundle.putString(ShortCutActionsUtils.EXTRA_LONG_PRESS_POWER_FUNCTION, "long_press_power_key");
                            voiceBundle.putBoolean(ShortCutActionsUtils.EXTRA_POWER_GUIDE, false);
                            triggered = ShortCutActionsUtils.getInstance(BaseMiuiPhoneWindowManager.this.mContext).triggerFunction("launch_voice_assistant", shortcut, voiceBundle, true);
                        }
                        if (triggered) {
                            BaseMiuiPhoneWindowManager.this.changeXiaoaiPowerGuideStatus(shortcut);
                        }
                    } else if ("launch_google_search".equals(action)) {
                        if (BaseMiuiPhoneWindowManager.this.mMiuiKeyShortcutManager.isCtsMode() || !MiuiKeyShortcutManager.SUPPORT_GOOGLE_RSA_PROTOCOL || !"long_press_power_key".equals(shortcut) || !BaseMiuiPhoneWindowManager.this.mMiuiKeyShortcutManager.supportRSARegion() || BaseMiuiPhoneWindowManager.this.mMiuiKeyShortcutManager.getRSAGuideStatus() != 1) {
                            triggered = ShortCutActionsUtils.getInstance(BaseMiuiPhoneWindowManager.this.mContext).triggerFunction("launch_google_search", shortcut, null, true);
                        } else {
                            triggered = ShortCutActionsUtils.getInstance(BaseMiuiPhoneWindowManager.this.mContext).triggerFunction("launch_global_power_guide", shortcut, null, true);
                            Settings.System.putIntForUser(BaseMiuiPhoneWindowManager.this.mContext.getContentResolver(), "global_power_guide", 0, BaseMiuiPhoneWindowManager.this.mCurrentUserId);
                            Settings.System.putIntForUser(BaseMiuiPhoneWindowManager.this.mContext.getContentResolver(), "google_assistant_guide", 0, BaseMiuiPhoneWindowManager.this.mCurrentUserId);
                        }
                    } else if ("launch_smarthome".equals(action)) {
                        if ("long_press_power_key".equals(shortcut)) {
                            BaseMiuiPhoneWindowManager.this.mShouldSendPowerUpToSmartHome = true;
                        }
                        Bundle smartHomeBundle = new Bundle();
                        smartHomeBundle.putString(ShortCutActionsUtils.EXTRA_LONG_PRESS_POWER_FUNCTION, "long_press_power_key");
                        triggered = ShortCutActionsUtils.getInstance(BaseMiuiPhoneWindowManager.this.mContext).triggerFunction("launch_smarthome", shortcut, smartHomeBundle, true);
                    } else if ("go_to_sleep".equals(action)) {
                        triggered = ShortCutActionsUtils.getInstance(BaseMiuiPhoneWindowManager.this.mContext).triggerFunction("go_to_sleep", shortcut, null, true);
                    } else if ("turn_on_torch".equals(action)) {
                        TelecomManager telecomManager = BaseMiuiPhoneWindowManager.this.getTelecommService();
                        if (BaseMiuiPhoneWindowManager.this.mWifiOnly || (telecomManager != null && telecomManager.getCallState() == 0)) {
                            phoneIdle = true;
                        }
                        if (phoneIdle) {
                            triggered = ShortCutActionsUtils.getInstance(BaseMiuiPhoneWindowManager.this.mContext).triggerFunction("turn_on_torch", shortcut, null, true);
                        }
                    } else if ("close_app".equals(action)) {
                        boolean isTriggeredByBack = "close_app".equals(BaseMiuiPhoneWindowManager.this.mMiuiKeyShortcutManager.getFunction("long_press_back_key"));
                        triggered = BaseMiuiPhoneWindowManager.this.closeApp(isTriggeredByBack);
                    } else if ("show_menu".equals(action)) {
                        triggered = BaseMiuiPhoneWindowManager.this.showMenu();
                    } else if ("mi_pay".equals(action)) {
                        triggered = ShortCutActionsUtils.getInstance(BaseMiuiPhoneWindowManager.this.mContext).triggerFunction("mi_pay", shortcut, null, true);
                    } else if ("dump_log".equals(action)) {
                        triggered = ShortCutActionsUtils.getInstance(BaseMiuiPhoneWindowManager.this.mContext).triggerFunction("dump_log", shortcut, null, true);
                    } else if ("launch_recents".equals(action)) {
                        BaseMiuiPhoneWindowManager.this.preloadRecentApps();
                        triggered = BaseMiuiPhoneWindowManager.this.m1384x3ade717a();
                        BaseMiuiPhoneWindowManager.this.mHapticFeedbackUtil.performHapticFeedback("virtual_key_longpress", false);
                    } else if ("split_screen".equals(action)) {
                        if (BaseMiuiPhoneWindowManager.this.isSupportSpiltScreen()) {
                            BaseMiuiPhoneWindowManager.this.toggleSplitScreenInternal();
                            triggered = true;
                            BaseMiuiPhoneWindowManager.this.mHapticFeedbackUtil.performHapticFeedback("virtual_key_longpress", false);
                        }
                    } else if ("close_talkback".equals(action)) {
                        if (BaseMiuiPhoneWindowManager.this.mVolumeDownKeyConsumed && BaseMiuiPhoneWindowManager.this.mVolumeUpKeyConsumed) {
                            BaseMiuiPhoneWindowManager.this.closeTalkBack();
                            MiuiInputLog.defaults("combine volume key,talkback is closed");
                        }
                    } else if ("find_device_locate".equals(action)) {
                        triggered = ShortCutActionsUtils.getInstance(BaseMiuiPhoneWindowManager.this.mContext).triggerFunction("find_device_locate", shortcut, null, true);
                    } else if ("none".equals(action)) {
                        if (BaseMiuiPhoneWindowManager.this.mMiuiKeyShortcutManager.mXiaoaiPowerGuideFlag == 1 && "long_press_power_key".equals(shortcut)) {
                            Bundle voiceBundle2 = new Bundle();
                            voiceBundle2.putBoolean(ShortCutActionsUtils.EXTRA_POWER_GUIDE, true);
                            voiceBundle2.putString(ShortCutActionsUtils.EXTRA_LONG_PRESS_POWER_FUNCTION, "long_press_power_key");
                            triggered = ShortCutActionsUtils.getInstance(BaseMiuiPhoneWindowManager.this.mContext).triggerFunction("launch_voice_assistant", "long_press_power_key", voiceBundle2, true);
                            if (triggered) {
                                BaseMiuiPhoneWindowManager.this.changeXiaoaiPowerGuideStatus(shortcut);
                            }
                        }
                        if (ShortCutActionsUtils.getInstance(BaseMiuiPhoneWindowManager.this.mContext).isNeedShowGoogleGuideForOperator() && !BaseMiuiPhoneWindowManager.this.mMiuiKeyShortcutManager.isDisabledGoogleAssistantGuide() && "long_press_power_key".equals(shortcut)) {
                            if (MiuiKeyShortcutManager.SUPPORT_GOOGLE_RSA_PROTOCOL) {
                                if (!BaseMiuiPhoneWindowManager.this.mMiuiKeyShortcutManager.supportRSARegion()) {
                                    triggered = BaseMiuiPhoneWindowManager.this.handleGoogleAssistantGuide();
                                }
                            } else {
                                triggered = BaseMiuiPhoneWindowManager.this.handleGoogleAssistantGuide();
                            }
                        }
                    }
                }
                if (triggered) {
                    BaseMiuiPhoneWindowManager.this.markShortcutTriggered();
                }
            } else if (msg.what == 3) {
                boolean triggeredFromAltTab = ((Boolean) msg.obj).booleanValue();
                BaseMiuiPhoneWindowManager.this.showRecentApps(triggeredFromAltTab);
            }
            super.handleMessage(msg);
        }
    }

    public boolean handleGoogleAssistantGuide() {
        Settings.System.putIntForUser(this.mContext.getContentResolver(), "google_assistant_guide", 0, this.mCurrentUserId);
        return ShortCutActionsUtils.getInstance(this.mContext).triggerFunction("launch_google_assistant_guide", "long_press_power_key", null, true);
    }

    public boolean isSupportSpiltScreen() {
        if (IS_CETUS && !isLargeScreen(this.mContext)) {
            MiuiInputLog.major("Ignore because the current window is the small screen");
            makeAllUserToastAndShow(this.mContext.getString(286196200), 0);
            return false;
        }
        List<MiuiFreeFormManager.MiuiFreeFormStackInfo> freeFormStackInfoList = MiuiFreeFormManager.getAllFreeFormStackInfosOnDisplay(-1);
        if (freeFormStackInfoList == null || freeFormStackInfoList.size() == 0) {
            return true;
        }
        MiuiInputLog.major("Ignore because The Application at FreeForm Mode");
        return false;
    }

    public void closeTalkBack() {
        SettingsStringUtil.SettingStringHelper settingStringHelper = this.mAccessibilityShortcutSetting;
        settingStringHelper.write(SettingsStringUtil.ComponentNameSet.remove(settingStringHelper.read(), talkBackServiceName));
    }

    private Intent getPowerGuideIntent() {
        Intent powerGuideIntent = new Intent();
        powerGuideIntent.setClassName("com.miui.voiceassist", "com.xiaomi.voiceassistant.guidePage.PowerGuideDialogActivityV2");
        powerGuideIntent.putExtra("showSwitchNotice", true);
        powerGuideIntent.addFlags(805306368);
        return powerGuideIntent;
    }

    private boolean launchApp(Intent intent) {
        if (!isUserSetupComplete()) {
            return false;
        }
        try {
            intent.addFlags(268435456);
            this.mContext.startActivityAsUser(intent, UserHandle.CURRENT);
            return true;
        } catch (ActivityNotFoundException e) {
            MiuiInputLog.error("ActivityNotFoundException", e);
            return false;
        } catch (IllegalStateException e2) {
            MiuiInputLog.error("IllegalStateException", e2);
            return false;
        }
    }

    public void changeXiaoaiPowerGuideStatus(String action) {
        if ("long_press_power_key".equals(action)) {
            if (this.mMiuiKeyShortcutManager.mXiaoaiPowerGuideFlag == 1) {
                int i = this.mXiaoaiPowerGuideCount + 1;
                this.mXiaoaiPowerGuideCount = i;
                if (i >= 2) {
                    Settings.System.putIntForUser(this.mContext.getContentResolver(), "xiaoai_power_guide", 0, this.mCurrentUserId);
                }
            }
            this.mIsXiaoaiServiceTriggeredByLongPressPower = true;
        } else if ("power_up".equals(action)) {
            this.mIsXiaoaiServiceTriggeredByLongPressPower = false;
        }
    }

    private long getImperceptiblePowerKeyTimeOut() {
        return SystemProperties.getLong("ro.miui.imp_power_time", this.mMiuiKeyShortcutManager.getPowerLongPressTimeOut());
    }

    public void onDefaultDisplayFocusChangedLw(WindowManagerPolicy.WindowState newFocus) {
        super.onDefaultDisplayFocusChangedLw(newFocus);
        this.mFocusedWindow = newFocus;
        MiuiStylusPageKeyListener miuiStylusPageKeyListener = this.mMiuiStylusPageKeyListener;
        if (miuiStylusPageKeyListener != null) {
            miuiStylusPageKeyListener.onDefaultDisplayFocusChangedLw(newFocus);
        }
    }

    public boolean closeApp(boolean isTriggeredByBack) {
        WindowManagerPolicy.WindowState _win;
        WindowManager.LayoutParams attrs;
        if (isTriggeredByBack) {
            _win = this.mWin;
        } else {
            _win = this.mFocusedWindow;
        }
        if (_win == null || (attrs = (WindowManager.LayoutParams) ReflectionUtils.callPrivateMethod(_win, "getAttrs", new Object[0])) == null) {
            return false;
        }
        int type = attrs.type;
        if ((type < 1 || type > 99) && (type < 1000 || type > 1999)) {
            return false;
        }
        String title = null;
        String packageName = attrs.packageName;
        PackageManager pm = this.mContext.getPackageManager();
        try {
            String className = attrs.getTitle().toString();
            int index = className.lastIndexOf(47);
            if (index >= 0) {
                ComponentName componentName = new ComponentName(packageName, (String) className.subSequence(index + 1, className.length()));
                ActivityInfo activityInfo = pm.getActivityInfo(componentName, 0);
                title = activityInfo.loadLabel(pm).toString();
            }
        } catch (PackageManager.NameNotFoundException e) {
            MiuiInputLog.error("NameNotFoundException", e);
        }
        try {
            if (TextUtils.isEmpty(title)) {
                ApplicationInfo applicationInfo = pm.getApplicationInfo(packageName, 0);
                title = applicationInfo.loadLabel(pm).toString();
            }
        } catch (PackageManager.NameNotFoundException e2) {
            MiuiInputLog.error("NameNotFoundException", e2);
        }
        if (TextUtils.isEmpty(title)) {
            title = packageName;
        }
        if (packageName.equals("com.miui.home")) {
            MiuiInputLog.major("The current window is the home,not need to close app");
            return true;
        } else if (!this.mSystemKeyPackages.contains(packageName)) {
            try {
                finishActivityInternal(attrs.token, 0, null);
            } catch (RemoteException e3) {
                MiuiInputLog.error("RemoteException", e3);
            }
            forceStopPackage(packageName, -2, "key shortcut");
            MiuiInputLog.major("The 'close app' interface was called successfully");
            makeAllUserToastAndShow(this.mContext.getString(286196202, title), 0);
            return true;
        } else {
            makeAllUserToastAndShow(this.mContext.getString(286196203, title), 0);
            MiuiInputLog.major("The current window is the system window,not need to close app");
            return true;
        }
    }

    public boolean launchAssistAction(String hint, Bundle args) {
        sendCloseSystemWindows("assist");
        if (!isUserSetupComplete()) {
            return false;
        }
        if ((this.mContext.getResources().getConfiguration().uiMode & 15) == 4) {
            ((SearchManager) this.mContext.getSystemService("search")).launchAssist(args);
            return true;
        }
        launchAssistActionInternal(hint, args);
        return true;
    }

    /* renamed from: launchRecentPanel */
    public boolean m1384x3ade717a() {
        sendCloseSystemWindows("recentapps");
        if (keyguardOn()) {
            return false;
        }
        ActivityManager activityManager = (ActivityManager) this.mContext.getSystemService("activity");
        TelecomManager telecomManager = getTelecommService();
        if (!this.mWifiOnly && telecomManager != null && telecomManager.isRinging() && isInCallScreenShowing()) {
            MiuiInputLog.major("Ignoring recent apps button; there's a ringing incoming call.");
            return true;
        }
        launchRecentPanelInternal();
        return true;
    }

    public void preloadRecentApps() {
        preloadRecentAppsInternal();
    }

    public boolean showMenu() {
        this.mRequestShowMenu = true;
        this.mHapticFeedbackUtil.performHapticFeedback("virtual_key_longpress", false);
        markShortcutTriggered();
        injectEvent(82);
        MiuiInputLog.major("show menu,the inject event is KEYCODE_MENU ");
        return false;
    }

    private void injectEvent(int injectKeyCode) {
        long now = SystemClock.uptimeMillis();
        KeyEvent homeDown = new KeyEvent(now, now, 0, injectKeyCode, 0, 0, -1, 0);
        KeyEvent homeUp = new KeyEvent(now, now, 1, injectKeyCode, 0, 0, -1, 0);
        InputManager.getInstance().injectInputEvent(homeDown, 0);
        InputManager.getInstance().injectInputEvent(homeUp, 0);
    }

    private AudioManager getAudioManager() {
        if (this.mAudioManager == null) {
            this.mAudioManager = (AudioManager) this.mContext.getSystemService("audio");
        }
        return this.mAudioManager;
    }

    private void playSoundEffect(int policyFlags, int keyCode, boolean down, int repeatCount) {
        if (down && (policyFlags & 2) != 0 && repeatCount == 0 && !this.mVibrator.hasVibrator() && !hasNavigationBar()) {
            switch (keyCode) {
                case 3:
                case 4:
                case UsbKeyboardUtil.COMMAND_READ_KEYBOARD /* 82 */:
                case 84:
                case 187:
                    playSoundEffect();
                    return;
                default:
                    return;
            }
        }
    }

    private boolean playSoundEffect() {
        AudioManager audioManager = getAudioManager();
        if (audioManager == null) {
            return false;
        }
        audioManager.playSoundEffect(0);
        return true;
    }

    public boolean performHapticFeedback(int uid, String packageName, int effectId, boolean always, String reason) {
        if (this.mHapticFeedbackUtil.isSupportedEffect(effectId)) {
            return this.mHapticFeedbackUtil.performHapticFeedback(effectId, always);
        }
        return super.performHapticFeedback(uid, packageName, effectId, always, reason);
    }

    public boolean isGameMode() {
        return Settings.Secure.getInt(this.mContext.getContentResolver(), KEY_GAME_BOOSTER, 0) == 1;
    }

    public void setTouchFeatureRotation() {
        int rotation = this.mContext.getDisplay().getRotation();
        Slog.d("WindowManager", "set rotation = " + rotation);
        int targetId = 0;
        if (MiuiSettings.System.IS_FOLD_DEVICE && this.mFolded) {
            targetId = 1;
        }
        ITouchFeature.getInstance().setTouchMode(targetId, 8, rotation);
    }

    public void handleTouchFeatureRotationWatcher() {
        if (this.mRotationWatcher == null) {
            this.mRotationWatcher = new RotationWatcher();
        }
        if (this.mIsSupportGloablTounchDirection || isGameMode() || EdgeSuppressionManager.IS_SUPPORT_EDGE_MODE) {
            setTouchFeatureRotation();
            if (!this.mHasWatchedRotation) {
                try {
                    this.mWindowManager.watchRotation(this.mRotationWatcher, this.mContext.getDisplay().getDisplayId());
                    this.mHasWatchedRotation = true;
                    return;
                } catch (RemoteException e) {
                    e.printStackTrace();
                    return;
                }
            }
            return;
        }
        try {
            this.mWindowManager.removeRotationWatcher(this.mRotationWatcher);
            this.mHasWatchedRotation = false;
        } catch (RemoteException e2) {
            e2.printStackTrace();
        }
    }

    /* loaded from: classes.dex */
    public class RotationWatcher extends IRotationWatcher.Stub {
        RotationWatcher() {
            BaseMiuiPhoneWindowManager.this = this$0;
        }

        public void onRotationChanged(int i) throws RemoteException {
            Slog.d("WindowManager", "rotation changed = " + i);
            int targetId = 0;
            if (MiuiSettings.System.IS_FOLD_DEVICE && BaseMiuiPhoneWindowManager.this.mFolded) {
                targetId = 1;
            }
            ITouchFeature.getInstance().setTouchMode(targetId, 8, i);
            if (!BaseMiuiPhoneWindowManager.this.isGameMode() && EdgeSuppressionManager.IS_SUPPORT_EDGE_MODE) {
                BaseMiuiPhoneWindowManager.this.mEdgeSuppressionManager.handleEdgeModeChange(EdgeSuppressionManager.REASON_OF_ROTATION);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class MiuiSettingsObserver extends ContentObserver {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        MiuiSettingsObserver(Handler handler) {
            super(handler);
            BaseMiuiPhoneWindowManager.this = this$0;
        }

        void observe() {
            ContentResolver resolver = BaseMiuiPhoneWindowManager.this.mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor("trackball_wake_screen"), false, this, -1);
            resolver.registerContentObserver(Settings.System.getUriFor("camera_key_preferred_action_type"), false, this, -1);
            resolver.registerContentObserver(Settings.System.getUriFor("camera_key_preferred_action_shortcut_id"), false, this, -1);
            resolver.registerContentObserver(Settings.System.getUriFor("volumekey_wake_screen"), false, this, -1);
            resolver.registerContentObserver(Settings.Secure.getUriFor("key_long_press_volume_down"), false, this, -1);
            resolver.registerContentObserver(Settings.Global.getUriFor("auto_test_mode_on"), false, this, -1);
            resolver.registerContentObserver(Settings.Secure.getUriFor("key_bank_card_in_ese"), false, this, -1);
            resolver.registerContentObserver(Settings.Secure.getUriFor("key_trans_card_in_ese"), false, this, -1);
            resolver.registerContentObserver(Settings.Global.getUriFor("torch_state"), false, this);
            resolver.registerContentObserver(Settings.System.getUriFor(BaseMiuiPhoneWindowManager.SYSTEM_SETTINGS_VR_MODE), false, this, -1);
            resolver.registerContentObserver(Settings.Secure.getUriFor("enable_mikey_mode"), false, this, -1);
            resolver.registerContentObserver(Settings.Secure.getUriFor("enabled_accessibility_services"), false, this, -1);
            resolver.registerContentObserver(Settings.Secure.getUriFor("accessibility_shortcut_target_service"), false, this, -1);
            resolver.registerContentObserver(Settings.Secure.getUriFor("accessibility_shortcut_on_lock_screen"), false, this, -1);
            resolver.registerContentObserver(Settings.System.getUriFor("three_gesture_down"), false, this, -1);
            resolver.registerContentObserver(Settings.System.getUriFor("three_gesture_long_press"), false, this, -1);
            resolver.registerContentObserver(Settings.System.getUriFor("send_back_when_xiaoai_appear"), false, this, -1);
            resolver.registerContentObserver(Settings.Global.getUriFor("imperceptible_press_power_key"), false, this, -1);
            resolver.registerContentObserver(Settings.Secure.getUriFor("long_press_timeout"), false, this, -1);
            if ((!BaseMiuiPhoneWindowManager.this.mIsSupportGloablTounchDirection || EdgeSuppressionManager.IS_SUPPORT_EDGE_MODE) && DeviceFeature.SUPPORT_GAME_MODE) {
                resolver.registerContentObserver(Settings.Secure.getUriFor(BaseMiuiPhoneWindowManager.KEY_GAME_BOOSTER), false, this, -1);
            }
            onChange(false, Settings.Global.getUriFor("imperceptible_press_power_key"));
            onChange(false, Settings.Secure.getUriFor("long_press_timeout"));
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            ContentResolver resolver = BaseMiuiPhoneWindowManager.this.mContext.getContentResolver();
            if (Settings.Secure.getUriFor(BaseMiuiPhoneWindowManager.KEY_GAME_BOOSTER).equals(uri)) {
                if (EdgeSuppressionManager.IS_SUPPORT_EDGE_MODE && !BaseMiuiPhoneWindowManager.this.isGameMode()) {
                    BaseMiuiPhoneWindowManager.this.mEdgeSuppressionManager.handleEdgeModeChange(EdgeSuppressionManager.REASON_OF_GAMEBOOSTER);
                } else {
                    BaseMiuiPhoneWindowManager.this.handleTouchFeatureRotationWatcher();
                }
            } else if (Settings.Global.getUriFor("imperceptible_press_power_key").equals(uri)) {
                BaseMiuiPhoneWindowManager baseMiuiPhoneWindowManager = BaseMiuiPhoneWindowManager.this;
                baseMiuiPhoneWindowManager.mImperceptiblePowerKey = MiuiSettings.Key.getKeyAndGestureShortcutFunction(baseMiuiPhoneWindowManager.mContext, "imperceptible_press_power_key");
            } else if (Settings.Secure.getUriFor("long_press_timeout").equals(uri)) {
                BaseMiuiPhoneWindowManager baseMiuiPhoneWindowManager2 = BaseMiuiPhoneWindowManager.this;
                baseMiuiPhoneWindowManager2.mKeyLongPressTimeout = Settings.Secure.getIntForUser(resolver, "long_press_timeout", 0, baseMiuiPhoneWindowManager2.mCurrentUserId);
            } else {
                super.onChange(selfChange, uri);
            }
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange) {
            ContentResolver resolver = BaseMiuiPhoneWindowManager.this.mContext.getContentResolver();
            Object lock = BaseMiuiPhoneWindowManager.phoneWindowManagerFeature.getLock(BaseMiuiPhoneWindowManager.this);
            synchronized (lock) {
                MiuiSettings.Key.updateOldKeyFunctionToNew(BaseMiuiPhoneWindowManager.this.mContext);
                if (!BaseMiuiPhoneWindowManager.this.hasNavigationBar()) {
                    BaseMiuiPhoneWindowManager.this.mMiuiKeyShortcutManager.mLongPressMenuKeyWhenLock = MiuiSettings.Key.getKeyAndGestureShortcutFunction(BaseMiuiPhoneWindowManager.this.mContext, "long_press_menu_key_when_lock");
                } else {
                    BaseMiuiPhoneWindowManager.this.mMiuiKeyShortcutManager.mLongPressMenuKeyWhenLock = null;
                }
                boolean z = true;
                BaseMiuiPhoneWindowManager.this.mMiuiKeyShortcutManager.mPressToAppSwitch = Settings.System.getIntForUser(resolver, "screen_key_press_app_switch", 1, BaseMiuiPhoneWindowManager.this.mCurrentUserId) != 0;
                BaseMiuiPhoneWindowManager baseMiuiPhoneWindowManager = BaseMiuiPhoneWindowManager.this;
                baseMiuiPhoneWindowManager.mTrackballWakeScreen = Settings.System.getIntForUser(resolver, "trackball_wake_screen", 0, baseMiuiPhoneWindowManager.mCurrentUserId) == 1;
                BaseMiuiPhoneWindowManager baseMiuiPhoneWindowManager2 = BaseMiuiPhoneWindowManager.this;
                baseMiuiPhoneWindowManager2.mMikeymodeEnabled = Settings.Secure.getIntForUser(resolver, "enable_mikey_mode", 0, baseMiuiPhoneWindowManager2.mCurrentUserId) != 0;
                BaseMiuiPhoneWindowManager.this.mTorchEnabled = Settings.Global.getInt(resolver, "torch_state", 0) != 0;
                if (!BaseMiuiPhoneWindowManager.this.mTorchEnabled) {
                    BaseMiuiPhoneWindowManager.this.mHandler.removeCallbacks(BaseMiuiPhoneWindowManager.this.mTurnOffTorch);
                }
                int cameraKeyActionType = Settings.System.getIntForUser(resolver, "camera_key_preferred_action_type", 0, BaseMiuiPhoneWindowManager.this.mCurrentUserId);
                BaseMiuiPhoneWindowManager baseMiuiPhoneWindowManager3 = BaseMiuiPhoneWindowManager.this;
                baseMiuiPhoneWindowManager3.mCameraKeyWakeScreen = 1 == cameraKeyActionType && 4 == Settings.System.getIntForUser(resolver, "camera_key_preferred_action_shortcut_id", -1, baseMiuiPhoneWindowManager3.mCurrentUserId);
                BaseMiuiPhoneWindowManager.this.mTestModeEnabled = Settings.Global.getInt(resolver, "auto_test_mode_on", 0) != 0;
                BaseMiuiPhoneWindowManager baseMiuiPhoneWindowManager4 = BaseMiuiPhoneWindowManager.this;
                baseMiuiPhoneWindowManager4.mVoiceAssistEnabled = Settings.System.getIntForUser(resolver, "send_back_when_xiaoai_appear", 0, baseMiuiPhoneWindowManager4.mCurrentUserId) != 0;
                BaseMiuiPhoneWindowManager baseMiuiPhoneWindowManager5 = BaseMiuiPhoneWindowManager.this;
                baseMiuiPhoneWindowManager5.mHaveBankCard = Settings.Secure.getIntForUser(resolver, "key_bank_card_in_ese", 0, baseMiuiPhoneWindowManager5.mCurrentUserId) > 0;
                BaseMiuiPhoneWindowManager baseMiuiPhoneWindowManager6 = BaseMiuiPhoneWindowManager.this;
                baseMiuiPhoneWindowManager6.mHaveTranksCard = Settings.Secure.getIntForUser(resolver, "key_trans_card_in_ese", 0, baseMiuiPhoneWindowManager6.mCurrentUserId) > 0;
                String action = Settings.Secure.getStringForUser(resolver, "key_long_press_volume_down", BaseMiuiPhoneWindowManager.this.mCurrentUserId);
                if (action != null) {
                    if (!"Street-snap".equals(action) && !"Street-snap-picture".equals(action) && !"Street-snap-movie".equals(action)) {
                        if ("public_transportation_shortcuts".equals(action)) {
                            BaseMiuiPhoneWindowManager.this.mLongPressVolumeDownBehavior = 2;
                        } else {
                            BaseMiuiPhoneWindowManager.this.mLongPressVolumeDownBehavior = 0;
                        }
                    }
                    BaseMiuiPhoneWindowManager.this.mLongPressVolumeDownBehavior = 1;
                } else {
                    Settings.Secure.putStringForUser(resolver, "key_long_press_volume_down", "none", BaseMiuiPhoneWindowManager.this.mCurrentUserId);
                }
                BaseMiuiPhoneWindowManager baseMiuiPhoneWindowManager7 = BaseMiuiPhoneWindowManager.this;
                baseMiuiPhoneWindowManager7.mIsVRMode = Settings.System.getIntForUser(resolver, BaseMiuiPhoneWindowManager.SYSTEM_SETTINGS_VR_MODE, 0, baseMiuiPhoneWindowManager7.mCurrentUserId) == 1;
                if (BaseMiuiPhoneWindowManager.this.mAccessibilityShortcutSetting == null) {
                    BaseMiuiPhoneWindowManager.this.mAccessibilityShortcutSetting = new SettingsStringUtil.SettingStringHelper(BaseMiuiPhoneWindowManager.this.mContext.getContentResolver(), "enabled_accessibility_services", BaseMiuiPhoneWindowManager.this.mCurrentUserId);
                }
                String shortcutService = BaseMiuiPhoneWindowManager.this.mAccessibilityShortcutSetting.read();
                BaseMiuiPhoneWindowManager.this.mTalkBackIsOpened = !TextUtils.isEmpty(shortcutService) && SettingsStringUtil.ComponentNameSet.contains(shortcutService, BaseMiuiPhoneWindowManager.talkBackServiceName);
                String accessibilityShortcut = Settings.Secure.getStringForUser(resolver, "accessibility_shortcut_target_service", BaseMiuiPhoneWindowManager.this.mCurrentUserId);
                if (accessibilityShortcut == null) {
                    accessibilityShortcut = BaseMiuiPhoneWindowManager.this.mContext.getString(17039939);
                }
                BaseMiuiPhoneWindowManager.this.mShortcutServiceIsTalkBack = 0 != 0 && !TextUtils.isEmpty(accessibilityShortcut) && ComponentName.unflattenFromString(accessibilityShortcut).equals(BaseMiuiPhoneWindowManager.talkBackServiceName);
                BaseMiuiPhoneWindowManager baseMiuiPhoneWindowManager8 = BaseMiuiPhoneWindowManager.this;
                if (Settings.Secure.getIntForUser(resolver, "accessibility_shortcut_on_lock_screen", 0, baseMiuiPhoneWindowManager8.mCurrentUserId) != 1) {
                    z = false;
                }
                baseMiuiPhoneWindowManager8.mAccessibilityShortcutOnLockScreen = z;
            }
        }
    }

    public void setCurrentUserLw(int newUserId) {
        super.setCurrentUserLw(newUserId);
        this.mCurrentUserId = newUserId;
        this.mAutoDisableScreenButtonsManager.onUserSwitch(newUserId);
        this.mSmartCoverManager.onUserSwitch(newUserId);
        this.mAccessibilityShortcutSetting.setUserId(newUserId);
        this.mSettingsObserver.onChange(false);
        this.mMiuiBackTapGestureService.onUserSwitch(newUserId);
        this.mMiuiKnockGestureService.onUserSwitch(newUserId);
        this.mMiuiThreeGestureListener.onUserSwitch(newUserId);
        this.mMiuiKeyShortcutManager.onUserSwitch(newUserId);
        this.mMiuiFingerPrintTapListener.onUserSwitch(newUserId);
        ShoulderKeyManagerInternal shoulderKeyManagerInternal = this.mShoulderKeyManagerInternal;
        if (shoulderKeyManagerInternal != null) {
            shoulderKeyManagerInternal.onUserSwitch();
        }
        this.mMiuiKeyInterceptExtend.setCurrentUserId(this.mCurrentUserId);
        ScrollerOptimizationConfigProvider.getInstance().onUserSwitch(newUserId);
        MiuiStylusPageKeyListener miuiStylusPageKeyListener = this.mMiuiStylusPageKeyListener;
        if (miuiStylusPageKeyListener != null) {
            miuiStylusPageKeyListener.onUserSwitch(newUserId);
        }
    }

    public void showBootMessage(final CharSequence msg, boolean always) {
        this.mHandler.post(new Runnable() { // from class: com.android.server.policy.BaseMiuiPhoneWindowManager.14
            @Override // java.lang.Runnable
            public void run() {
                String[] split;
                if (BaseMiuiPhoneWindowManager.this.mMiuiBootMsgDialog == null) {
                    BaseMiuiPhoneWindowManager.this.mMiuiBootMsgDialog = new Dialog(BaseMiuiPhoneWindowManager.this.mContext, 286261258) { // from class: com.android.server.policy.BaseMiuiPhoneWindowManager.14.1
                        @Override // android.app.Dialog, android.view.Window.Callback
                        public boolean dispatchKeyEvent(KeyEvent event) {
                            return true;
                        }

                        @Override // android.app.Dialog, android.view.Window.Callback
                        public boolean dispatchKeyShortcutEvent(KeyEvent event) {
                            return true;
                        }

                        @Override // android.app.Dialog, android.view.Window.Callback
                        public boolean dispatchTouchEvent(MotionEvent ev) {
                            return true;
                        }

                        @Override // android.app.Dialog, android.view.Window.Callback
                        public boolean dispatchTrackballEvent(MotionEvent ev) {
                            return true;
                        }

                        @Override // android.app.Dialog, android.view.Window.Callback
                        public boolean dispatchGenericMotionEvent(MotionEvent ev) {
                            return true;
                        }

                        @Override // android.app.Dialog, android.view.Window.Callback
                        public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
                            return true;
                        }
                    };
                    View view = LayoutInflater.from(BaseMiuiPhoneWindowManager.this.mMiuiBootMsgDialog.getContext()).inflate(285999115, (ViewGroup) null);
                    BaseMiuiPhoneWindowManager.this.mMiuiBootMsgDialog.setContentView(view);
                    BaseMiuiPhoneWindowManager.this.mMiuiBootMsgDialog.getWindow().setType(2021);
                    BaseMiuiPhoneWindowManager.this.mMiuiBootMsgDialog.getWindow().addFlags(1282);
                    BaseMiuiPhoneWindowManager.this.mMiuiBootMsgDialog.getWindow().setDimAmount(1.0f);
                    WindowManager.LayoutParams lp = BaseMiuiPhoneWindowManager.this.mMiuiBootMsgDialog.getWindow().getAttributes();
                    lp.screenOrientation = 5;
                    BaseMiuiPhoneWindowManager.this.mMiuiBootMsgDialog.getWindow().setAttributes(lp);
                    BaseMiuiPhoneWindowManager.this.mMiuiBootMsgDialog.setCancelable(false);
                    BaseMiuiPhoneWindowManager.this.mMiuiBootMsgDialog.show();
                    ImageView bootLogo = (ImageView) view.findViewById(285868062);
                    bootLogo.setVisibility(0);
                    if ("beryllium".equals(Build.DEVICE)) {
                        String hwc = SystemProperties.get("ro.boot.hwc", "");
                        if (hwc.contains("INDIA")) {
                            bootLogo.setImageResource(285737029);
                        } else if (hwc.contains("GLOBAL")) {
                            bootLogo.setImageResource(285737028);
                        }
                    }
                    BaseMiuiPhoneWindowManager.this.mBootProgress = (ProgressBar) view.findViewById(285868063);
                    BaseMiuiPhoneWindowManager.this.mBootProgress.setVisibility(4);
                    BaseMiuiPhoneWindowManager baseMiuiPhoneWindowManager = BaseMiuiPhoneWindowManager.this;
                    baseMiuiPhoneWindowManager.mBootText = baseMiuiPhoneWindowManager.mContext.getResources().getStringArray(285409296);
                    if (BaseMiuiPhoneWindowManager.this.mBootText != null && BaseMiuiPhoneWindowManager.this.mBootText.length > 0) {
                        BaseMiuiPhoneWindowManager.this.mBootTextView = (TextView) view.findViewById(285868064);
                        BaseMiuiPhoneWindowManager.this.mBootTextView.setVisibility(4);
                    }
                }
                List<String> parseList = new ArrayList<>();
                CharSequence charSequence = msg;
                if (charSequence != null) {
                    for (String sp : String.valueOf(charSequence).replaceAll("[^0-9]", ",").split(",")) {
                        if (sp.length() > 0) {
                            parseList.add(sp);
                        }
                    }
                }
                if (parseList.size() == 2) {
                    int progress = Integer.parseInt(parseList.get(0));
                    int total = Integer.parseInt(parseList.get(1));
                    if (progress > total) {
                        progress = total;
                        total = progress;
                    }
                    if (total > 3) {
                        BaseMiuiPhoneWindowManager.this.mBootProgress.setVisibility(0);
                        BaseMiuiPhoneWindowManager.this.mBootProgress.setMax(total);
                        BaseMiuiPhoneWindowManager.this.mBootProgress.setProgress(progress);
                        if (BaseMiuiPhoneWindowManager.this.mBootTextView != null && BaseMiuiPhoneWindowManager.this.mBootText != null) {
                            BaseMiuiPhoneWindowManager.this.mBootTextView.setVisibility(0);
                            int pos = (BaseMiuiPhoneWindowManager.this.mBootText.length * progress) / total;
                            if (pos >= BaseMiuiPhoneWindowManager.this.mBootText.length) {
                                pos = BaseMiuiPhoneWindowManager.this.mBootText.length - 1;
                            }
                            BaseMiuiPhoneWindowManager.this.mBootTextView.setText(BaseMiuiPhoneWindowManager.this.mBootText[pos]);
                        }
                    }
                }
            }
        });
    }

    public void hideBootMessages() {
        this.mHandler.post(new Runnable() { // from class: com.android.server.policy.BaseMiuiPhoneWindowManager.15
            @Override // java.lang.Runnable
            public void run() {
                if (BaseMiuiPhoneWindowManager.this.mMiuiBootMsgDialog != null) {
                    BaseMiuiPhoneWindowManager.this.mMiuiBootMsgDialog.dismiss();
                    BaseMiuiPhoneWindowManager.this.mMiuiBootMsgDialog = null;
                    BaseMiuiPhoneWindowManager.this.mBootProgress = null;
                    BaseMiuiPhoneWindowManager.this.mBootTextView = null;
                    BaseMiuiPhoneWindowManager.this.mBootText = null;
                }
            }
        });
    }

    static IWindowManager getWindownManagerService() {
        IWindowManager service = IWindowManager.Stub.asInterface(ServiceManager.checkService("window"));
        if (service == null) {
            Slog.w("WindowManager", "Unable to find IWindowManager interface.");
        }
        return service;
    }

    boolean checkProcessRunning(String processName) {
        List<ActivityManager.RunningAppProcessInfo> procs;
        ActivityManager am = (ActivityManager) this.mContext.getSystemService("activity");
        if (am == null || (procs = am.getRunningAppProcesses()) == null) {
            return false;
        }
        for (ActivityManager.RunningAppProcessInfo info : procs) {
            if (processName.equalsIgnoreCase(info.processName)) {
                return true;
            }
        }
        return false;
    }

    boolean isPhoneOffhook() {
        TelephonyManager telephonyManager = (TelephonyManager) this.mContext.getSystemService(TelephonyManager.class);
        if (telephonyManager == null) {
            return false;
        }
        boolean isOffhook = telephonyManager.isOffhook();
        return isOffhook;
    }

    void sendAsyncBroadcast(final Intent intent) {
        if (this.mSystemReady) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.policy.BaseMiuiPhoneWindowManager.16
                @Override // java.lang.Runnable
                public void run() {
                    BaseMiuiPhoneWindowManager.this.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
                }
            });
        }
    }

    void sendAsyncBroadcast(final Intent intent, final String receiverPermission) {
        if (this.mSystemReady) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.policy.BaseMiuiPhoneWindowManager.17
                @Override // java.lang.Runnable
                public void run() {
                    BaseMiuiPhoneWindowManager.this.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT, receiverPermission);
                }
            });
        }
    }

    void sendAsyncBroadcastForAllUser(final Intent intent) {
        if (this.mSystemReady) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.policy.BaseMiuiPhoneWindowManager.18
                @Override // java.lang.Runnable
                public void run() {
                    BaseMiuiPhoneWindowManager.this.mContext.sendBroadcastAsUser(intent, new UserHandle(-1));
                }
            });
        }
    }

    public void enableScreenAfterBoot() {
        super.enableScreenAfterBoot();
        this.mWifiOnly = SystemProperties.getBoolean("ro.radio.noril", false);
        this.mSmartCoverManager.enableLidAfterBoot();
    }

    public void finishLayoutLw(DisplayFrames displayFrames, Rect inputMethodRegion) {
        int inputMethodHeight = inputMethodRegion.bottom - inputMethodRegion.top;
        if (this.mInputMethodWindowVisibleHeight != inputMethodHeight) {
            this.mInputMethodWindowVisibleHeight = inputMethodHeight;
            Slog.i("WindowManager", "input method visible height changed " + inputMethodHeight);
            this.mMiuiKnockGestureService.setInputMethodRect(inputMethodRegion);
            Intent intent = new Intent("miui.intent.action.INPUT_METHOD_VISIBLE_HEIGHT_CHANGED");
            intent.putExtra("miui.intent.extra.input_method_visible_height", this.mInputMethodWindowVisibleHeight);
            sendAsyncBroadcast(intent, PERMISSION_INTERNAL_GENERAL_API);
        }
        int displayWidth = Math.min(displayFrames.mDisplayWidth, displayFrames.mDisplayHeight) - 1;
        if (this.mIsFoldChanged && displayFrames.mDisplayId == 0 && displayWidth != this.mEdgeSuppressionManager.getScreenWidth()) {
            this.mIsFoldChanged = false;
            this.mEdgeSuppressionManager.handleEdgeModeChange(EdgeSuppressionManager.REASON_OF_CONFIGURATION, this.mFolded, displayFrames);
        }
    }

    protected boolean getForbidFullScreenFlag() {
        return this.mForbidFullScreen;
    }

    private void handleDoubleTapOnHome() {
        if (this.mMiuiKeyShortcutManager.mDoubleTapOnHomeBehavior == 1) {
            this.mHomeConsumed = true;
            m1384x3ade717a();
        }
    }

    private boolean isNfcEnable(boolean ishomeclick) {
        if (!ishomeclick) {
            return this.mLongPressVolumeDownBehavior == 2 && this.mHaveTranksCard;
        } else if ("sagit".equals(Build.DEVICE) || "jason".equals(Build.DEVICE)) {
            return false;
        } else {
            return this.mHaveBankCard || this.mHaveTranksCard;
        }
    }

    protected boolean interceptHomeKeyStartNfc(KeyEvent event, int policyFlags, boolean isScreenOn) {
        MiuiKeyguardServiceDelegate miuiKeyguardServiceDelegate;
        boolean intercept = false;
        if (!isNfcEnable(true)) {
            return false;
        }
        boolean down = event.getAction() == 0;
        int keyCode = event.getKeyCode();
        boolean isInjected = (16777216 & policyFlags) != 0;
        boolean KeyguardNotActive = !this.mHomeDoubleClickPending && (miuiKeyguardServiceDelegate = this.mMiuiKeyguardDelegate) != null && !miuiKeyguardServiceDelegate.isShowingAndNotHidden();
        if (isInjected || ((isScreenOn && KeyguardNotActive) || keyCode != 3 || !down || event.getRepeatCount() != 0)) {
            return false;
        }
        if (this.mHomeDoubleClickPending) {
            try {
                this.mHandler.removeCallbacks(this.mHomeDoubleClickTimeoutRunnable);
                intercept = true;
                this.mHomeDoubleClickPending = false;
                this.mHomeConsumed = true;
                this.mMiuiKeyguardDelegate.OnDoubleClickHome();
                ShortCutActionsUtils.getInstance(this.mContext).triggerFunction("mi_pay", ShortCutActionsUtils.REASON_OF_DOUBLE_CLICK_HOME_KEY, null, true);
                return true;
            } catch (ActivityNotFoundException e) {
                MiuiInputLog.error("mNfcIntent problem", e);
                return intercept;
            }
        }
        this.mHomeDoubleClickPending = true;
        this.mHandler.postDelayed(this.mHomeDoubleClickTimeoutRunnable, 300L);
        return false;
    }

    /* renamed from: lambda$new$10$com-android-server-policy-BaseMiuiPhoneWindowManager */
    public /* synthetic */ void m1387x2b1f794f() {
        ShortCutActionsUtils.getInstance(this.mContext).triggerFunction(this.mMiuiKeyShortcutManager.getFunction("double_click_power_key"), "double_click_power", null, false);
    }

    private boolean interceptPowerKey(KeyEvent event, boolean isScreenOn) {
        if (event.getAction() == 0 && event.getKeyCode() == 26) {
            boolean interceptWhenScreenOn = false;
            this.mPressPowerKeyTime = event.getDownTime();
            long now = SystemClock.elapsedRealtime();
            boolean interceptWhenScreenOff = closeTorchWhenScreenOff(isScreenOn);
            long doubleTapInterval = now - this.mLastPowerDown;
            if (doubleTapInterval < 300 && doubleTapInterval > 0) {
                int i = this.mPressPowerKeyCount + 1;
                this.mPressPowerKeyCount = i;
                if (i <= 2 && this.mMiuiKeyShortcutManager.getFunction("double_click_power_key") != null && !"none".equals(this.mMiuiKeyShortcutManager.getFunction("double_click_power_key"))) {
                    interceptWhenScreenOn = isScreenOn;
                }
                if (this.mPressPowerKeyCount > 2 && this.mHandler.hasCallbacks(this.mDoubleClickPowerRunnable)) {
                    this.mHandler.removeCallbacks(this.mDoubleClickPowerRunnable);
                }
            } else {
                this.mPressPowerKeyCount = 1;
                if (this.mHandler.hasMessages(2)) {
                    this.mHandler.removeMessages(2);
                    callSuperInterceptKeyBeforeQueueing((KeyEvent) this.mPowerMessage.getData().getParcelable(LAST_POWER_UP_EVENT), this.mPowerMessage.getData().getInt(LAST_POWER_UP_POLICY), this.mPowerMessage.getData().getBoolean(LAST_POWER_UP_SCREEN_STATE));
                }
            }
            this.mLastPowerDown = now;
            if (this.mPressPowerKeyCount == 5 && this.mMiuiKeyShortcutManager.isSosCanBeTrigger()) {
                Intent intent = new Intent("miui.intent.action.LAUNCH_SOS");
                intent.setPackage("com.android.settings");
                launchApp(intent);
            }
            if (this.mPressPowerKeyCount == 2) {
                if (SUPPORT_POWERFP && inFingerprintEnrolling()) {
                    MiuiInputLog.defaults("Power button double tap gesture detected, but in FingerprintEnrolling, return.");
                    return false;
                }
                MiuiInputLog.defaults("Power button double tap gesture detected, " + this.mMiuiKeyShortcutManager.getFunction("double_click_power_key") + ". Interval=" + doubleTapInterval + "ms");
                if ("launch_camera".equals(this.mMiuiKeyShortcutManager.getFunction("double_click_power_key"))) {
                    ShortCutActionsUtils.getInstance(this.mContext).triggerFunction("launch_camera", ShortCutActionsUtils.REASON_OF_DOUBLE_CLICK_POWER_KEY, null, true);
                } else if (!"turn_on_torch".equals(this.mMiuiKeyShortcutManager.getFunction("double_click_power_key"))) {
                    if (this.mMiuiKeyShortcutManager.needDelayPowerKey()) {
                        if (isScreenOn) {
                            if (!"au_pay".equals(this.mMiuiKeyShortcutManager.getFunction("double_click_power_key")) || (!this.mMiuiKeyShortcutManager.isSosCanBeTrigger() && !this.mMiuiKeyShortcutManager.isGoogleSosEnable())) {
                                this.mHandler.post(this.mDoubleClickPowerRunnable);
                            } else {
                                this.mHandler.postDelayed(this.mDoubleClickPowerRunnable, 500L);
                            }
                        } else {
                            interceptWhenScreenOff = true;
                        }
                        this.mHandler.removeMessages(2);
                    } else if ("launch_smarthome".equals(this.mMiuiKeyShortcutManager.getFunction("double_click_power_key"))) {
                        Bundle smartHomeBundle = new Bundle();
                        smartHomeBundle.putString(ShortCutActionsUtils.EXTRA_ACTION_SOURCE, "double_click_power_key");
                        ShortCutActionsUtils.getInstance(this.mContext).triggerFunction("launch_smarthome", "double_click_power_key", smartHomeBundle, true);
                    }
                } else {
                    TelecomManager telecomManager = getTelecommService();
                    boolean phoneIdle = this.mWifiOnly || (telecomManager != null && telecomManager.getCallState() == 0);
                    if (phoneIdle) {
                        ShortCutActionsUtils.getInstance(this.mContext).triggerFunction("turn_on_torch", "double_click_power_key", null, true);
                    }
                }
            }
            return interceptWhenScreenOn || interceptWhenScreenOff;
        }
        return false;
    }

    private boolean interceptPowerKeyUp(KeyEvent event, int policyFlag, boolean isScreenOn) {
        if (event.getAction() != 0 && event.getKeyCode() == 26 && this.mMiuiKeyShortcutManager.needDelayPowerKey() && this.mKeyPressed == getKeyBitmask(26) && this.mPressPowerKeyTime == event.getDownTime()) {
            int i = this.mPressPowerKeyCount;
            if (i == 1) {
                long realDelayTime = 300 - (SystemClock.elapsedRealtime() - this.mLastPowerDown);
                if (realDelayTime <= 0) {
                    return false;
                }
                Message msg = this.mHandler.obtainMessage(2);
                Bundle bundle = new Bundle();
                bundle.putBoolean(LAST_POWER_UP_SCREEN_STATE, isScreenOn);
                bundle.putInt(LAST_POWER_UP_POLICY, policyFlag);
                bundle.putParcelable(LAST_POWER_UP_EVENT, event.copy());
                msg.setData(bundle);
                if (this.mPowerManager.isScreenOn()) {
                    this.mPowerMessage = Message.obtain(msg);
                    this.mHandler.sendMessageDelayed(msg, realDelayTime);
                    return true;
                }
            } else if (i == 2) {
                callSuperInterceptKeyBeforeQueueing(KeyEvent.changeFlags(event, event.getFlags() | 32), policyFlag, isScreenOn);
                return true;
            }
        }
        return false;
    }

    protected boolean interceptVoluemeKeyStartCamera(KeyEvent event, int policyFlags, boolean isScreenOn) {
        boolean KeyguardNotActive = false;
        boolean down = event.getAction() == 0;
        int keyCode = event.getKeyCode();
        boolean isInjected = (policyFlags & 16777216) != 0;
        MiuiKeyguardServiceDelegate miuiKeyguardServiceDelegate = this.mMiuiKeyguardDelegate;
        if (miuiKeyguardServiceDelegate != null && !miuiKeyguardServiceDelegate.isShowingAndNotHidden()) {
            KeyguardNotActive = true;
        }
        if (isInjected || ((isScreenOn && KeyguardNotActive) || keyCode != 25 || !down || event.getRepeatCount() != 0)) {
            return false;
        }
        long now = SystemClock.elapsedRealtime();
        if (now - this.mVolumeButtonPrePressedTime < 300) {
            this.mVolumeButtonPressedCount++;
        } else {
            this.mVolumeButtonPressedCount = 1L;
            this.mVolumeButtonPrePressedTime = now;
        }
        if (this.mVolumeButtonPressedCount < 2 || isAudioActive()) {
            return false;
        }
        try {
            this.mVolumeKeyWakeLock.acquire(5000L);
            ShortCutActionsUtils.getInstance(this.mContext).triggerFunction("launch_camera", ShortCutActionsUtils.REASON_OF_DOUBLE_CLICK_VOLUME_DOWN, null, true);
            return true;
        } catch (ActivityNotFoundException e) {
            MiuiInputLog.error("mCameraIntent problem", e);
            return false;
        }
    }

    public boolean getKeyguardActive() {
        return this.mMiuiKeyguardDelegate != null && (!this.mPowerManager.isScreenOn() ? this.mMiuiKeyguardDelegate.isShowing() : this.mMiuiKeyguardDelegate.isShowingAndNotHidden());
    }

    private Intent getSmartHomeIntent() {
        if (this.mSmartHomeIntent == null) {
            Intent intent = new Intent();
            this.mSmartHomeIntent = intent;
            intent.setComponent(new ComponentName("com.miui.smarthomeplus", "com.miui.smarthomeplus.UWBEntryService"));
        }
        return this.mSmartHomeIntent;
    }

    private Intent getVoiceAssistIntent() {
        if (this.mAssistIntent == null) {
            this.mAssistIntent = new Intent("android.intent.action.ASSIST");
        }
        this.mAssistIntent.setComponent(null);
        return this.mAssistIntent;
    }

    private boolean isAudioActive() {
        boolean active = false;
        int mode = getAudioManager().getMode();
        if (mode > 0 && mode < 7) {
            MiuiInputLog.major("isAudioActive():true");
            return true;
        }
        int size = AudioSystem.getNumStreamTypes();
        for (int i = 0; i < size && (1 == i || !(active = AudioSystem.isStreamActive(i, 0))); i++) {
        }
        if (active) {
            MiuiInputLog.major("isAudioActive():" + active);
        }
        return active;
    }

    public void setScreenRecorderEnabled(boolean enable) {
        this.mScreenRecorderEnabled = enable;
    }

    private boolean isTrackInputEvenForScreenRecorder(KeyEvent event) {
        if (this.mScreenRecorderEnabled && sScreenRecorderKeyEventList.contains(Integer.valueOf(event.getKeyCode()))) {
            return true;
        }
        return false;
    }

    private void sendKeyEventBroadcast(KeyEvent event) {
        Intent intent = new Intent("miui.intent.SCREEN_RECORDER_TRACK_KEYEVENT");
        intent.setPackage("com.miui.screenrecorder");
        intent.putExtra(MiuiCustomizeShortCutUtils.ATTRIBUTE_KEYCODE, event.getKeyCode());
        intent.putExtra("isdown", event.getAction() == 0);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
    }

    private void trackDumpLogKeyCode(KeyEvent event) {
        int code = event.getKeyCode();
        if (code != 25 && code != 24) {
            this.mTrackDumpLogKeyCodePengding = false;
            return;
        }
        InputDevice inputDevice = event.getDevice();
        if (inputDevice != null && inputDevice.isExternal()) {
            this.mTrackDumpLogKeyCodePengding = false;
            return;
        }
        boolean z = this.mTrackDumpLogKeyCodePengding;
        if (!z && code == 24) {
            this.mTrackDumpLogKeyCodePengding = true;
            this.mTrackDumpLogKeyCodeStartTime = event.getEventTime();
            this.mTrackDumpLogKeyCodeLastKeyCode = 24;
            this.mTrackDumpLogKeyCodeVolumeDownTimes = 0;
        } else if (z) {
            long timeDelta = event.getEventTime() - this.mTrackDumpLogKeyCodeStartTime;
            if (timeDelta >= this.mTrackDumpLogKeyCodeTimeOut || code == this.mTrackDumpLogKeyCodeLastKeyCode) {
                this.mTrackDumpLogKeyCodePengding = false;
                if (code == 24) {
                    this.mTrackDumpLogKeyCodePengding = true;
                    this.mTrackDumpLogKeyCodeStartTime = event.getEventTime();
                    this.mTrackDumpLogKeyCodeLastKeyCode = 24;
                    this.mTrackDumpLogKeyCodeVolumeDownTimes = 0;
                    return;
                }
                return;
            }
            this.mTrackDumpLogKeyCodeLastKeyCode = code;
            if (code == 25) {
                this.mTrackDumpLogKeyCodeVolumeDownTimes++;
            }
            if (this.mTrackDumpLogKeyCodeVolumeDownTimes == 3) {
                this.mTrackDumpLogKeyCodePengding = false;
                MiuiInputLog.defaults("DumpLog triggered");
                this.mHandler.post(new Runnable() { // from class: com.android.server.policy.BaseMiuiPhoneWindowManager$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        BaseMiuiPhoneWindowManager.this.m1389x8611d395();
                    }
                });
            }
        }
    }

    /* renamed from: lambda$trackDumpLogKeyCode$11$com-android-server-policy-BaseMiuiPhoneWindowManager */
    public /* synthetic */ void m1389x8611d395() {
        ShortCutActionsUtils.getInstance(this.mContext).triggerFunction("dump_log_or_secret_code", "volume_down_up_three_time", null, false);
    }

    private boolean isTrackInputEventForVoiceAssist(KeyEvent event) {
        if (this.mVoiceAssistEnabled && sVoiceAssistKeyEventList.contains(Integer.valueOf(event.getKeyCode()))) {
            return true;
        }
        return false;
    }

    private void sendVoiceAssistKeyEventBroadcast(KeyEvent event) {
        Intent intent = new Intent("miui.intent.VOICE_ASSIST_TRACK_KEYEVENT");
        intent.setPackage("com.miui.voiceassist");
        intent.putExtra(MiuiCustomizeShortCutUtils.ATTRIBUTE_KEYCODE, event.getKeyCode());
        intent.putExtra("isdown", event.getAction() == 0);
        sendAsyncBroadcast(intent);
    }

    private void sendFullScreenStateToTaskSnapshot() {
        Intent intent = new Intent("com.miui.fullscreen_state_change");
        intent.putExtra("state", "taskSnapshot");
        sendAsyncBroadcast(intent);
    }

    private void sendFullScreenStateToHome() {
        Intent intent = new Intent("com.miui.fullscreen_state_change");
        intent.putExtra("state", "toHome");
        sendAsyncBroadcastForAllUser(intent);
    }

    private void sendBackKeyEventBroadcast(KeyEvent event) {
        Intent intent = new Intent("miui.intent.KEYCODE_BACK");
        intent.putExtra("android.intent.extra.KEY_EVENT", event);
        sendAsyncBroadcast(intent);
    }

    private boolean isLockDeviceWindow(WindowManagerPolicy.WindowState win) {
        WindowManager.LayoutParams lp;
        return (win == null || (lp = (WindowManager.LayoutParams) ReflectionUtils.callPrivateMethod(win, "getAttrs", new Object[0])) == null || (lp.extraFlags & 2048) == 0) ? false : true;
    }

    public void addPowerVolumeDownRule() {
        this.mKeyCombinationManager.addRule(this.mPowerVolumeDownRule);
    }

    public void removePowerVolumeDownRule() {
        this.mKeyCombinationManager.removeRule(this.mPowerVolumeDownRule);
    }

    public static boolean isLargeScreen(Context context) {
        Configuration configuration = context.getResources().getConfiguration();
        int smallestScreenWidthDp = configuration.smallestScreenWidthDp;
        return configuration.densityDpi == ACCESSIBLE_MODE_SMALL_DENSITY ? smallestScreenWidthDp > 400 : configuration.densityDpi == ACCESSIBLE_MODE_LARGE_DENSITY ? smallestScreenWidthDp > 305 : smallestScreenWidthDp > 320;
    }

    public void dump(String prefix, PrintWriter pw, String[] args) {
        super.dump(prefix, pw, args);
        pw.print(prefix);
        pw.println("BaseMiuiPhoneWindowManager");
        String prefix2 = prefix + "  ";
        pw.print(prefix2);
        pw.print("mInputMethodWindowVisibleHeight=");
        pw.println(this.mInputMethodWindowVisibleHeight);
        pw.print(prefix2);
        pw.print("mFrontFingerprintSensor=");
        pw.println(this.mFrontFingerprintSensor);
        pw.print(prefix2);
        pw.print("mSupportTapFingerprintSensorToHome=");
        pw.println(this.mSupportTapFingerprintSensorToHome);
        pw.print(prefix2);
        pw.print("mScreenOffReason=");
        pw.println(this.mScreenOffReason);
        pw.print(prefix2);
        pw.print("mIsStatusBarVisibleInFullscreen=");
        pw.println(this.mIsStatusBarVisibleInFullscreen);
        pw.print(prefix2);
        pw.print("mTorchEnabled=");
        pw.println(this.mTorchEnabled);
        pw.print(prefix2);
        pw.print("mScreenRecorderEnabled=");
        pw.println(this.mScreenRecorderEnabled);
        pw.print(prefix2);
        pw.print("mVoiceAssistEnabled=");
        pw.println(this.mVoiceAssistEnabled);
        pw.print(prefix2);
        pw.print("mWifiOnly=");
        pw.println(this.mWifiOnly);
        pw.print("    ");
        pw.println("KeyPress");
        pw.print(prefix2);
        pw.print("mKeyPressed=");
        pw.print(Integer.toBinaryString(this.mKeyPressed));
        pw.print(" mKeyPressing=");
        pw.print(Integer.toBinaryString(this.mKeyPressing));
        pw.print(" mShortcutPressing=");
        pw.println(Integer.toBinaryString(this.mShortcutPressing));
        pw.print(prefix2);
        pw.print("KEYCODE_MENU KeyBitmask=");
        pw.println(Integer.toBinaryString(getKeyBitmask(82)));
        pw.print(prefix2);
        pw.print("KEYCODE_APP_SWITCH KeyBitmask=");
        pw.println(Integer.toBinaryString(getKeyBitmask(187)));
        pw.print(prefix2);
        pw.print("KEYCODE_HOME KeyBitmask=");
        pw.println(Integer.toBinaryString(getKeyBitmask(3)));
        pw.print(prefix2);
        pw.print("KEYCODE_BACK KeyBitmask=");
        pw.println(Integer.toBinaryString(getKeyBitmask(4)));
        pw.print(prefix2);
        pw.print("KEYCODE_POWER KeyBitmask=");
        pw.println(Integer.toBinaryString(getKeyBitmask(26)));
        pw.print(prefix2);
        pw.print("KEYCODE_VOLUME_DOWN KeyBitmask=");
        pw.println(Integer.toBinaryString(getKeyBitmask(25)));
        pw.print(prefix2);
        pw.print("KEYCODE_VOLUME_UP KeyBitmask=");
        pw.println(Integer.toBinaryString(getKeyBitmask(24)));
        pw.print(prefix2);
        pw.print("ElSE KEYCODE KeyBitmask=");
        pw.println(Integer.toBinaryString(1));
        pw.print(prefix2);
        pw.print("SHORTCUT_HOME_POWER=");
        pw.println(Integer.toBinaryString(SHORTCUT_HOME_POWER));
        pw.print(prefix2);
        pw.print("SHORTCUT_BACK_POWER=");
        pw.println(Integer.toBinaryString(SHORTCUT_BACK_POWER));
        pw.print(prefix2);
        pw.print("SHORTCUT_MENU_POWER=");
        pw.println(Integer.toBinaryString(SHORTCUT_MENU_POWER));
        pw.print(prefix2);
        pw.print("SHORTCUT_SCREENSHOT_ANDROID=");
        pw.println(Integer.toBinaryString(SHORTCUT_SCREENSHOT_ANDROID));
        pw.print(prefix2);
        pw.print("SHORTCUT_SCREENSHOT_MIUI=");
        pw.println(Integer.toBinaryString(SHORTCUT_SCREENSHOT_MIUI));
        pw.print(prefix2);
        pw.print("SHORTCUT_UNLOCK=");
        pw.println(Integer.toBinaryString(SHORTCUT_UNLOCK));
        pw.print(prefix2);
        pw.print("mShortcutTriggered=");
        pw.println(this.mShortcutTriggered);
        pw.print(prefix2);
        pw.print("mDpadCenterDown=");
        pw.println(this.mDpadCenterDown);
        pw.print(prefix2);
        pw.print("mHomeDownAfterDpCenter=");
        pw.println(this.mHomeDownAfterDpCenter);
        pw.print("    ");
        pw.println("KeyResponseSetting");
        pw.print(prefix2);
        pw.print("mCurrentUserId=");
        pw.println(this.mCurrentUserId);
        pw.print(prefix2);
        pw.print("mMikeymodeEnabled=");
        pw.println(this.mMikeymodeEnabled);
        pw.print(prefix2);
        pw.print("mCameraKeyWakeScreen=");
        pw.println(this.mCameraKeyWakeScreen);
        pw.print(prefix2);
        pw.print("mTrackballWakeScreen=");
        pw.println(this.mTrackballWakeScreen);
        pw.print(prefix2);
        pw.print("mTestModeEnabled=");
        pw.println(this.mTestModeEnabled);
        pw.print(prefix2);
        pw.print("mScreenButtonsDisabled=");
        pw.println(this.mAutoDisableScreenButtonsManager.isScreenButtonsDisabled());
        pw.print(prefix2);
        pw.print("mVolumeButtonPrePressedTime=");
        pw.println(this.mVolumeButtonPrePressedTime);
        pw.print(prefix2);
        pw.print("mVolumeButtonPressedCount=");
        pw.println(this.mVolumeButtonPressedCount);
        pw.print(prefix2);
        pw.print("mHaveBankCard=");
        pw.println(this.mHaveBankCard);
        pw.print(prefix2);
        pw.print("mHaveTranksCard=");
        pw.println(this.mHaveTranksCard);
        pw.print(prefix2);
        pw.print("mLongPressVolumeDownBehavior=");
        pw.println(this.mLongPressVolumeDownBehavior);
        pw.print(prefix2);
        pw.print("mIsVRMode=");
        pw.println(this.mIsVRMode);
        pw.print(prefix2);
        pw.print("mTalkBackIsOpened=");
        pw.println(this.mTalkBackIsOpened);
        pw.print(prefix2);
        pw.print("mShortcutServiceIsTalkBack=");
        pw.println(this.mShortcutServiceIsTalkBack);
        this.mSmartCoverManager.dump(prefix2, pw);
        this.mMiuiThreeGestureListener.dump(prefix2, pw);
        this.mMiuiKnockGestureService.dump(prefix2, pw);
        this.mMiuiBackTapGestureService.dump(prefix2, pw);
        this.mMiuiKeyShortcutManager.dump(prefix2, pw);
        this.mMiuiFingerPrintTapListener.dump(prefix2, pw);
        this.mMiuiTimeFloatingWindow.dump(prefix2, pw);
        if (isMiPad()) {
            MiuiCustomizeShortCutUtils.getInstance(this.mContext).dump(prefix2, pw);
        }
        MiuiStylusPageKeyListener miuiStylusPageKeyListener = this.mMiuiStylusPageKeyListener;
        if (miuiStylusPageKeyListener != null) {
            miuiStylusPageKeyListener.dump(prefix2, pw);
        }
        MiuiPadKeyboardManager miuiPadKeyboardManager = this.mMiuiPadKeyboardManager;
        if (miuiPadKeyboardManager != null) {
            miuiPadKeyboardManager.dump(prefix2, pw);
        }
    }
}
