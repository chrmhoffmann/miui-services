package com.miui.server.input.knock;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Slog;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.view.WindowManagerPolicyConstants;
import com.android.server.input.InputManagerService;
import com.android.server.input.config.InputKnockConfig;
import com.android.server.wm.WindowManagerService;
import com.miui.server.input.knock.MiuiKnockGestureService;
import com.miui.server.input.knock.checker.KnockGestureDouble;
import com.miui.server.input.knock.checker.KnockGestureHorizontalBrightness;
import com.miui.server.input.knock.checker.KnockGesturePartialShot;
import com.miui.server.input.knock.checker.KnockGestureV;
import com.miui.server.input.knock.config.KnockConfig;
import com.miui.server.input.knock.config.KnockConfigHelper;
import com.miui.server.input.knock.config.filter.KnockConfigFilter;
import com.miui.server.input.knock.config.filter.KnockConfigFilterDeviceName;
import com.miui.server.input.knock.config.filter.KnockConfigFilterDisplayVersion;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import miui.os.Build;
import miui.util.FeatureParser;
/* loaded from: classes.dex */
public class MiuiKnockGestureService implements WindowManagerPolicyConstants.PointerEventListener {
    private static final int DELAY_CHECK_TIME = 1000;
    private static final String KEY_GAME_BOOSTER = "gb_boosting";
    private static final String KEY_OPEN_GAME_BOOSTER = "pref_open_game_booster";
    private static final String LOG_NATIVE_FEATURE_ERROR = "native feature not merge";
    private static final int MOTION_EVENT_TOOL_TYPE_KNOCK = 32;
    private static final int PROPERTY_COLLECT_DATA = 2;
    private static final String PROPERTY_DISPLAY_VERSION = "vendor.panel.vendor";
    private static final int PROPERTY_OPEN_KNOCK_FEATURE = 1;
    private static final String TAG = "MiuiKnockGestureService";
    private static WeakReference<MiuiKnockGestureService> sMiuiKnockGestureService;
    private KnockGestureDouble mCheckerDoubleKnock;
    private KnockGesturePartialShot mCheckerGesturePartialShot;
    private KnockGestureHorizontalBrightness mCheckerHorizontalBrightness;
    private KnockGestureV mCheckerKnockGestureV;
    private boolean mCloseKnockAllFeature;
    private Context mContext;
    private int mDelayCheckTimes;
    private String mDisplayVersion;
    private String mFunctionDoubleKnock;
    private String mFunctionGesturePartial;
    private String mFunctionGestureV;
    private String mFunctionHorizontalSlid;
    private Handler mHandler;
    private InputManagerService mInputManagerService;
    private boolean mIsGameMode;
    private boolean mIsScreenOn;
    private boolean mIsSetupComplete;
    private boolean mIsStatusBarVisible;
    private KnockCheckDelegate mKnockCheckDelegate;
    private KnockConfig mKnockConfig;
    private KnockConfigHelper mKnockConfigHelper;
    private MiuiSettingsObserver mMiuiSettingsObserver;
    private int mNativeFeatureState;
    private boolean mOpenGameBooster;
    private WindowManager mWindowManager;
    private WindowManagerService mWindowManagerService;
    private int mCurrentUserId = -2;
    private boolean mHasRegisterListener = false;
    private Point mSizePoint = new Point(0, 0);
    private Point mTempPoint = new Point(0, 0);
    private Runnable mDelayRunnable = new AnonymousClass1();
    private BroadcastReceiver mConfigurationReceiver = new BroadcastReceiver() { // from class: com.miui.server.input.knock.MiuiKnockGestureService.2
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            MiuiKnockGestureService.this.mWindowManager.getDefaultDisplay().getRealSize(MiuiKnockGestureService.this.mTempPoint);
            if (MiuiKnockGestureService.this.mSizePoint.x != MiuiKnockGestureService.this.mTempPoint.x && MiuiKnockGestureService.this.mSizePoint.x != MiuiKnockGestureService.this.mTempPoint.y) {
                Slog.i(MiuiKnockGestureService.TAG, "Screen resolution conversion so reset Knock Valid Area");
                MiuiKnockGestureService.this.setKnockValidRect();
                MiuiKnockGestureService.this.mKnockCheckDelegate.onScreenSizeChanged();
            }
        }
    };

    /* renamed from: com.miui.server.input.knock.MiuiKnockGestureService$1 */
    /* loaded from: classes.dex */
    public class AnonymousClass1 implements Runnable {
        AnonymousClass1() {
            MiuiKnockGestureService.this = this$0;
        }

        @Override // java.lang.Runnable
        public void run() {
            MiuiKnockGestureService.this.mDisplayVersion = SystemProperties.get(MiuiKnockGestureService.PROPERTY_DISPLAY_VERSION);
            if (TextUtils.isEmpty(MiuiKnockGestureService.this.mDisplayVersion) || !MiuiKnockGestureService.isInteger(MiuiKnockGestureService.this.mDisplayVersion)) {
                if (MiuiKnockGestureService.this.mDelayCheckTimes < 3) {
                    MiuiKnockGestureService.this.mDelayCheckTimes++;
                    MiuiKnockGestureService.this.mHandler.postDelayed(new Runnable() { // from class: com.miui.server.input.knock.MiuiKnockGestureService$1$$ExternalSyntheticLambda0
                        @Override // java.lang.Runnable
                        public final void run() {
                            MiuiKnockGestureService.AnonymousClass1.this.run();
                        }
                    }, 1000L);
                    return;
                }
                Slog.e(MiuiKnockGestureService.TAG, "display_version get fail");
            } else if (MiuiKnockGestureService.this.displayVersionSupportCheck()) {
                MiuiKnockGestureService.this.featureSupportInit();
            }
        }
    }

    public void setInputMethodRect(Rect inputMethodRegion) {
        if (!isFeatureInit()) {
            return;
        }
        if (inputMethodRegion == null) {
            setKnockInvalidRect(0, 0, 0, 0);
        } else {
            setKnockInvalidRect(inputMethodRegion.left, inputMethodRegion.top, inputMethodRegion.right, inputMethodRegion.bottom);
        }
    }

    public void updateScreenState(boolean screenOn) {
        if (!isFeatureInit()) {
            return;
        }
        Message message = this.mHandler.obtainMessage(3, Boolean.valueOf(screenOn));
        this.mHandler.sendMessage(message);
    }

    private boolean isFeatureInit() {
        return sMiuiKnockGestureService != null;
    }

    public static void finishPostLayoutPolicyLw(boolean statusBarVisible) {
        MiuiKnockGestureService miuiKnockGestureService;
        WeakReference<MiuiKnockGestureService> weakReference = sMiuiKnockGestureService;
        if (weakReference != null && (miuiKnockGestureService = weakReference.get()) != null) {
            miuiKnockGestureService.onStatusBarVisibilityChangeStatic(statusBarVisible);
        }
    }

    public MiuiKnockGestureService(Context context) {
        initialize(context);
    }

    private void initialize(Context context) {
        this.mContext = context;
        this.mWindowManagerService = ServiceManager.getService("window");
        this.mInputManagerService = ServiceManager.getService("input");
        this.mWindowManager = (WindowManager) this.mContext.getSystemService("window");
        this.mHandler = new H();
        KnockConfigHelper knockConfigHelper = KnockConfigHelper.getInstance();
        this.mKnockConfigHelper = knockConfigHelper;
        knockConfigHelper.initConfig();
        initDeviceKnockProperty();
    }

    private void initDeviceKnockProperty() {
        boolean deviceSupport = this.mKnockConfigHelper.isDeviceSupport();
        if (!deviceSupport) {
            return;
        }
        String str = SystemProperties.get(PROPERTY_DISPLAY_VERSION);
        this.mDisplayVersion = str;
        if (TextUtils.isEmpty(str) || !isInteger(this.mDisplayVersion)) {
            this.mHandler.postDelayed(this.mDelayRunnable, 1000L);
        } else if (displayVersionSupportCheck()) {
            featureSupportInit();
        } else {
            MiuiSettings.Global.putBoolean(this.mContext.getContentResolver(), "support_knock", false);
        }
    }

    public boolean displayVersionSupportCheck() {
        List<KnockConfigFilter> knockConfigFilterList = new ArrayList<>();
        KnockConfigFilterDeviceName filterDeviceName = new KnockConfigFilterDeviceName(Build.DEVICE);
        KnockConfigFilterDisplayVersion filterDisplayVersion = new KnockConfigFilterDisplayVersion(this.mDisplayVersion);
        knockConfigFilterList.add(filterDeviceName);
        knockConfigFilterList.add(filterDisplayVersion);
        KnockConfig knockConfig = this.mKnockConfigHelper.getKnockConfig(knockConfigFilterList);
        this.mKnockConfig = knockConfig;
        if (knockConfig == null) {
            return false;
        }
        Slog.w(TAG, knockConfig.toString());
        return ((this.mKnockConfig.deviceProperty & 1) == 0 && (this.mKnockConfig.deviceProperty & 2) == 0) ? false : true;
    }

    public static boolean isInteger(String displayVersion) {
        Pattern pattern = Pattern.compile("^[-\\+]?[\\d]*$");
        return pattern.matcher(displayVersion).matches();
    }

    public void featureSupportInit() {
        setKnockValidRect();
        if (FeatureParser.getIntArray("screen_resolution_supported") != null) {
            this.mContext.registerReceiver(this.mConfigurationReceiver, new IntentFilter("android.intent.action.CONFIGURATION_CHANGED"));
        }
        setKnockDeviceProperty();
        setKnockDlcPath(this.mKnockConfig.localAlgorithmPath);
        setKnockScoreThreshold(this.mKnockConfig.knockScoreThreshold);
        setKnockUseFrame(this.mKnockConfig.useFrame);
        setKnockQuickMoveSpeed(this.mKnockConfig.quickMoveSpeed);
        setKnockSensorThreshold(this.mKnockConfig.sensorThreshold);
        boolean z = true;
        MiuiSettings.Global.putBoolean(this.mContext.getContentResolver(), "support_knock", true);
        this.mFunctionDoubleKnock = MiuiSettings.Key.getKeyAndGestureShortcutFunction(this.mContext, "double_knock");
        this.mFunctionGestureV = MiuiSettings.Key.getKeyAndGestureShortcutFunction(this.mContext, "knock_gesture_v");
        this.mFunctionGesturePartial = MiuiSettings.Key.getKeyAndGestureShortcutFunction(this.mContext, "knock_slide_shape");
        this.mFunctionHorizontalSlid = MiuiSettings.Key.getKeyAndGestureShortcutFunction(this.mContext, "knock_long_press_horizontal_slid");
        this.mOpenGameBooster = Settings.Secure.getInt(this.mContext.getContentResolver(), KEY_OPEN_GAME_BOOSTER, 1) == 1;
        this.mIsGameMode = Settings.Secure.getInt(this.mContext.getContentResolver(), KEY_GAME_BOOSTER, 0) == 1;
        if (Settings.System.getInt(this.mContext.getContentResolver(), "close_knock_all_feature", 0) != 1) {
            z = false;
        }
        this.mCloseKnockAllFeature = z;
        this.mKnockCheckDelegate = new KnockCheckDelegate(this.mContext);
        this.mCheckerHorizontalBrightness = new KnockGestureHorizontalBrightness(this.mContext);
        this.mCheckerKnockGestureV = new KnockGestureV(this.mContext);
        this.mCheckerGesturePartialShot = new KnockGesturePartialShot(this.mContext);
        this.mCheckerDoubleKnock = new KnockGestureDouble(this.mContext);
        this.mCheckerKnockGestureV.setFunction(this.mFunctionGestureV);
        this.mCheckerDoubleKnock.setFunction(this.mFunctionDoubleKnock);
        this.mCheckerGesturePartialShot.setFunction(this.mFunctionGesturePartial);
        this.mCheckerHorizontalBrightness.setFunction(this.mFunctionHorizontalSlid);
        this.mKnockCheckDelegate.registerKnockChecker(this.mCheckerDoubleKnock);
        this.mKnockCheckDelegate.registerKnockChecker(this.mCheckerKnockGestureV);
        this.mKnockCheckDelegate.registerKnockChecker(this.mCheckerHorizontalBrightness);
        this.mKnockCheckDelegate.registerKnockChecker(this.mCheckerGesturePartialShot);
        this.mIsSetupComplete = isUserSetUp();
        updateKnockFeatureState();
        MiuiSettingsObserver miuiSettingsObserver = new MiuiSettingsObserver(this.mHandler);
        this.mMiuiSettingsObserver = miuiSettingsObserver;
        miuiSettingsObserver.observe();
        sMiuiKnockGestureService = new WeakReference<>(this);
    }

    /* loaded from: classes.dex */
    public class H extends Handler {
        static final int MSG_KEY_SCREEN_STATE_CHANGED = 3;
        static final int MSG_KEY_STATUSBAR_CHANGED = 1;
        static final int MSG_KEY_UPDATE_FEATURE_STATE = 2;

        private H() {
            MiuiKnockGestureService.this = r1;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    MiuiKnockGestureService.this.mIsStatusBarVisible = ((Boolean) msg.obj).booleanValue();
                    MiuiKnockGestureService.this.updateKnockFeatureState();
                    return;
                case 2:
                    MiuiKnockGestureService.this.updateKnockFeatureState();
                    return;
                case 3:
                    MiuiKnockGestureService.this.mIsScreenOn = ((Boolean) msg.obj).booleanValue();
                    MiuiKnockGestureService.this.updateKnockFeatureState();
                    MiuiKnockGestureService.this.mKnockCheckDelegate.updateScreenState(MiuiKnockGestureService.this.mIsScreenOn);
                    return;
                default:
                    return;
            }
        }
    }

    private void onStatusBarVisibilityChangeStatic(boolean statusBarVisible) {
        Message message = this.mHandler.obtainMessage(1, Boolean.valueOf(statusBarVisible));
        this.mHandler.sendMessage(message);
    }

    public void updateKnockFeatureState() {
        if (this.mCloseKnockAllFeature) {
            Slog.i(TAG, "mCloseKnockAllFeature true, close knock");
            setNativeKnockFeatureState(0);
        } else if (!this.mIsScreenOn) {
            Slog.i(TAG, "mIsScreenOn false, close knock");
            setNativeKnockFeatureState(0);
        } else if (this.mIsGameMode) {
            Slog.i(TAG, "mIsGameMode true, close knock");
            setNativeKnockFeatureState(0);
        } else if (!this.mOpenGameBooster && !this.mIsStatusBarVisible) {
            Slog.i(TAG, "mOpenGameBooster false, mIsStatusBarVisible false, close knock");
            setNativeKnockFeatureState(0);
        } else {
            int i = 0;
            if (!checkEmpty(this.mFunctionDoubleKnock) || !checkEmpty(this.mFunctionGestureV) || !checkEmpty(this.mFunctionGesturePartial) || !checkEmpty(this.mFunctionHorizontalSlid)) {
                i = 1;
            }
            if (i != 0 && !this.mHasRegisterListener) {
                this.mWindowManagerService.registerPointerEventListener(this, 0);
                this.mHasRegisterListener = true;
            } else if (i == 0 && this.mHasRegisterListener) {
                this.mWindowManagerService.unregisterPointerEventListener(this, 0);
                this.mHasRegisterListener = false;
            }
            setNativeKnockFeatureState(i);
        }
    }

    private void setNativeKnockFeatureState(int state) {
        if (state != this.mNativeFeatureState) {
            this.mNativeFeatureState = state;
            InputKnockConfig config = InputKnockConfig.getInstance();
            config.setKnockFeatureState(state);
            config.flushToNative();
        }
    }

    private void setKnockDeviceProperty() {
        InputKnockConfig config = InputKnockConfig.getInstance();
        config.setKnockDeviceProperty(this.mKnockConfig.deviceProperty);
        config.flushToNative();
    }

    public void setKnockValidRect() {
        this.mWindowManager.getDefaultDisplay().getRealSize(this.mSizePoint);
        int width = Math.min(this.mSizePoint.x, this.mSizePoint.y);
        int height = Math.max(this.mSizePoint.x, this.mSizePoint.y);
        int left = (this.mKnockConfig.knockRegion.left * width) / this.mKnockConfig.deviceX[0];
        int top = (this.mKnockConfig.knockRegion.top * height) / this.mKnockConfig.deviceX[1];
        int right = ((this.mKnockConfig.deviceX[0] - this.mKnockConfig.knockRegion.right) * width) / this.mKnockConfig.deviceX[0];
        int bottom = ((this.mKnockConfig.deviceX[1] - this.mKnockConfig.knockRegion.bottom) * height) / this.mKnockConfig.deviceX[1];
        InputKnockConfig config = InputKnockConfig.getInstance();
        config.setKnockValidRect(left, top, right, bottom);
        config.flushToNative();
    }

    private void setKnockInvalidRect(int left, int top, int right, int bottom) {
        InputKnockConfig config = InputKnockConfig.getInstance();
        config.setKnockInValidRect(left, top, right, bottom);
        config.flushToNative();
    }

    private void setKnockDlcPath(String dlcPath) {
        InputKnockConfig config = InputKnockConfig.getInstance();
        config.updateAlgorithmPath(dlcPath);
        config.flushToNative();
    }

    private void setKnockScoreThreshold(float score) {
        InputKnockConfig config = InputKnockConfig.getInstance();
        config.setKnockScoreThreshold(score);
        config.flushToNative();
    }

    private void setKnockUseFrame(int useFrame) {
        InputKnockConfig config = InputKnockConfig.getInstance();
        config.setKnockUseFrame(useFrame);
        config.flushToNative();
    }

    private void setKnockQuickMoveSpeed(float quickMoveSpeed) {
        InputKnockConfig config = InputKnockConfig.getInstance();
        config.setKnockQuickMoveSpeed(quickMoveSpeed);
        config.flushToNative();
    }

    private void setKnockSensorThreshold(int[] sensorThreshold) {
        InputKnockConfig config = InputKnockConfig.getInstance();
        config.setKnockSensorThreshold(sensorThreshold);
        config.flushToNative();
    }

    private boolean checkEmpty(String feature) {
        if (TextUtils.isEmpty(feature) || feature.equals("none")) {
            return true;
        }
        return false;
    }

    public void onPointerEvent(MotionEvent event) {
        if (event.getToolType(0) == 32) {
            if (isMotionUserSetUp(event)) {
                this.mKnockCheckDelegate.onTouchEvent(event);
            } else if (event.getAction() == 0) {
                Slog.i(TAG, "user not setup complete, action down");
            }
        }
    }

    public void onUserSwitch(int newUserId) {
        if (this.mCurrentUserId != newUserId) {
            this.mCurrentUserId = newUserId;
            updateSettings();
        }
    }

    private void updateSettings() {
        MiuiSettingsObserver miuiSettingsObserver = this.mMiuiSettingsObserver;
        if (miuiSettingsObserver != null) {
            miuiSettingsObserver.onChange(false, Settings.System.getUriFor("double_knock"));
            this.mMiuiSettingsObserver.onChange(false, Settings.System.getUriFor("knock_gesture_v"));
            this.mMiuiSettingsObserver.onChange(false, Settings.System.getUriFor("knock_slide_shape"));
            this.mMiuiSettingsObserver.onChange(false, Settings.System.getUriFor("knock_long_press_horizontal_slid"));
            this.mMiuiSettingsObserver.onChange(false, Settings.System.getUriFor(KEY_GAME_BOOSTER));
            this.mMiuiSettingsObserver.onChange(false, Settings.System.getUriFor(KEY_OPEN_GAME_BOOSTER));
            this.mMiuiSettingsObserver.onChange(false, Settings.System.getUriFor("close_knock_all_feature"));
        }
    }

    /* loaded from: classes.dex */
    public class MiuiSettingsObserver extends ContentObserver {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        MiuiSettingsObserver(Handler handler) {
            super(handler);
            MiuiKnockGestureService.this = this$0;
        }

        void observe() {
            ContentResolver resolver = MiuiKnockGestureService.this.mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor("double_knock"), false, this, -1);
            resolver.registerContentObserver(Settings.System.getUriFor("knock_gesture_v"), false, this, -1);
            resolver.registerContentObserver(Settings.System.getUriFor("knock_slide_shape"), false, this, -1);
            resolver.registerContentObserver(Settings.System.getUriFor("knock_long_press_horizontal_slid"), false, this, -1);
            resolver.registerContentObserver(Settings.Secure.getUriFor(MiuiKnockGestureService.KEY_GAME_BOOSTER), false, this, -1);
            resolver.registerContentObserver(Settings.Secure.getUriFor(MiuiKnockGestureService.KEY_OPEN_GAME_BOOSTER), false, this, -1);
            resolver.registerContentObserver(Settings.System.getUriFor("close_knock_all_feature"), false, this, -1);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            if (Settings.System.getUriFor("double_knock").equals(uri)) {
                MiuiKnockGestureService miuiKnockGestureService = MiuiKnockGestureService.this;
                miuiKnockGestureService.mFunctionDoubleKnock = MiuiSettings.Key.getKeyAndGestureShortcutFunction(miuiKnockGestureService.mContext, "double_knock");
                MiuiKnockGestureService.this.mCheckerDoubleKnock.setFunction(MiuiKnockGestureService.this.mFunctionDoubleKnock);
                Slog.i(MiuiKnockGestureService.TAG, "knock feature change , double_knock_function:" + MiuiKnockGestureService.this.mFunctionDoubleKnock);
            } else if (Settings.System.getUriFor("knock_gesture_v").equals(uri)) {
                MiuiKnockGestureService miuiKnockGestureService2 = MiuiKnockGestureService.this;
                miuiKnockGestureService2.mFunctionGestureV = MiuiSettings.Key.getKeyAndGestureShortcutFunction(miuiKnockGestureService2.mContext, "knock_gesture_v");
                MiuiKnockGestureService.this.mCheckerKnockGestureV.setFunction(MiuiKnockGestureService.this.mFunctionGestureV);
                Slog.i(MiuiKnockGestureService.TAG, "knock feature change , knock_gesture_v:" + MiuiKnockGestureService.this.mFunctionGestureV);
            } else if (Settings.System.getUriFor("knock_slide_shape").equals(uri)) {
                MiuiKnockGestureService miuiKnockGestureService3 = MiuiKnockGestureService.this;
                miuiKnockGestureService3.mFunctionGesturePartial = MiuiSettings.Key.getKeyAndGestureShortcutFunction(miuiKnockGestureService3.mContext, "knock_slide_shape");
                MiuiKnockGestureService.this.mCheckerGesturePartialShot.setFunction(MiuiKnockGestureService.this.mFunctionGesturePartial);
                Slog.i(MiuiKnockGestureService.TAG, "knock feature change , knock_gesture_partial_screenshot:" + MiuiKnockGestureService.this.mFunctionGesturePartial);
            } else if (Settings.System.getUriFor("knock_long_press_horizontal_slid").equals(uri)) {
                MiuiKnockGestureService miuiKnockGestureService4 = MiuiKnockGestureService.this;
                miuiKnockGestureService4.mFunctionHorizontalSlid = MiuiSettings.Key.getKeyAndGestureShortcutFunction(miuiKnockGestureService4.mContext, "knock_long_press_horizontal_slid");
                MiuiKnockGestureService.this.mCheckerHorizontalBrightness.setFunction(MiuiKnockGestureService.this.mFunctionHorizontalSlid);
                Slog.i(MiuiKnockGestureService.TAG, "knock feature change , knock_long_press_horizontal_slid:" + MiuiKnockGestureService.this.mFunctionHorizontalSlid);
            } else {
                boolean z = false;
                if (Settings.Secure.getUriFor(MiuiKnockGestureService.KEY_GAME_BOOSTER).equals(uri)) {
                    MiuiKnockGestureService miuiKnockGestureService5 = MiuiKnockGestureService.this;
                    if (Settings.Secure.getInt(miuiKnockGestureService5.mContext.getContentResolver(), MiuiKnockGestureService.KEY_GAME_BOOSTER, 0) == 1) {
                        z = true;
                    }
                    miuiKnockGestureService5.mIsGameMode = z;
                    Slog.d(MiuiKnockGestureService.TAG, "game_booster changed, isGameMode = " + MiuiKnockGestureService.this.mIsGameMode);
                } else if (Settings.Secure.getUriFor(MiuiKnockGestureService.KEY_OPEN_GAME_BOOSTER).equals(uri)) {
                    MiuiKnockGestureService miuiKnockGestureService6 = MiuiKnockGestureService.this;
                    if (Settings.Secure.getInt(miuiKnockGestureService6.mContext.getContentResolver(), MiuiKnockGestureService.KEY_OPEN_GAME_BOOSTER, 1) == 1) {
                        z = true;
                    }
                    miuiKnockGestureService6.mOpenGameBooster = z;
                    Slog.d(MiuiKnockGestureService.TAG, "game_booster_feature changed, mOpenGameBooster = " + MiuiKnockGestureService.this.mOpenGameBooster);
                } else if (Settings.System.getUriFor("close_knock_all_feature").equals(uri)) {
                    MiuiKnockGestureService miuiKnockGestureService7 = MiuiKnockGestureService.this;
                    if (Settings.System.getInt(miuiKnockGestureService7.mContext.getContentResolver(), "close_knock_all_feature", 0) == 1) {
                        z = true;
                    }
                    miuiKnockGestureService7.mCloseKnockAllFeature = z;
                    Slog.d(MiuiKnockGestureService.TAG, "close all knock feature changed, mOpenGameBooster = " + MiuiKnockGestureService.this.mOpenGameBooster);
                }
            }
            MiuiKnockGestureService.this.updateKnockFeatureState();
        }
    }

    private boolean isMotionUserSetUp(MotionEvent event) {
        if (!this.mIsSetupComplete && event.getAction() == 0) {
            this.mIsSetupComplete = isUserSetUp();
        }
        return this.mIsSetupComplete;
    }

    private boolean isUserSetUp() {
        return Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "user_setup_complete", 0, -2) != 0;
    }

    public void dump(String prefix, PrintWriter pw) {
        pw.print("    ");
        pw.println(TAG);
        pw.print(prefix);
        pw.print("mIsSetupComplete=");
        pw.println(this.mIsSetupComplete);
        pw.print(prefix);
        pw.print("mCloseKnockAllFeature=");
        pw.println(this.mCloseKnockAllFeature);
        pw.print(prefix);
        pw.print("mKnockConfig=");
        pw.println(this.mKnockConfig);
        pw.print(prefix);
        pw.print("mDisplayVersion=");
        pw.println(this.mDisplayVersion);
        pw.print(prefix);
        pw.print("mFeatureInit=");
        pw.println(isFeatureInit());
        pw.print(prefix);
        pw.print("mDoubleKnockFunction=");
        pw.println(this.mFunctionDoubleKnock);
        pw.print(prefix);
        pw.print("mKnockGestureV=");
        pw.println(this.mFunctionGestureV);
        pw.print(prefix);
        pw.print("mGesturePartialFunction=");
        pw.println(this.mFunctionGesturePartial);
        pw.print(prefix);
        pw.print("mHorizontalSlidFunction=");
        pw.println(this.mFunctionHorizontalSlid);
    }
}
