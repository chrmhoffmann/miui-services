package com.android.server.display;

import android.app.ActivityTaskManager;
import android.app.IActivityTaskManager;
import android.app.TaskStackListener;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.hardware.Sensor;
import android.hardware.devicestate.DeviceStateManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerExecutor;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.MathUtils;
import android.util.Slog;
import android.util.Spline;
import com.android.internal.display.BrightnessSynchronizer;
import com.android.internal.os.BackgroundThread;
import com.android.server.LocalServices;
import com.android.server.display.AutomaticBrightnessControllerImpl;
import com.android.server.display.DisplayDeviceConfig;
import com.android.server.display.DisplayPowerControllerStub;
import com.android.server.display.HighBrightnessModeController;
import com.android.server.display.SunlightController;
import com.android.server.display.statistics.MiuiBrightnessChangeTracker;
import com.android.server.display.statistics.OneTrackFoldStateHelper;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.wm.MiuiFreeformPinManagerService;
import com.android.server.wm.WindowManagerServiceStub;
import com.miui.base.MiuiStubRegistry;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.function.Consumer;
import miui.util.FeatureParser;
/* loaded from: classes.dex */
public class DisplayPowerControllerImpl implements DisplayPowerControllerStub, SunlightController.Callback {
    private static final float COEFFICIENT = 0.5f;
    private static final float[] DATA_D;
    private static final int DISPLAY_DIM_STATE = 262;
    private static final int DOZE_LIGHT_LOW = 1;
    public static final int EPSILON = 3;
    private static final float GRAY_BRIGHTNESS_RATE;
    private static final boolean IS_FOLDABLE_DEVICE;
    private static final String KEY_CURTAIN_ANIM_ENABLED = "curtain_anim_enabled";
    private static final String KEY_IS_DYNAMIC_LOCK_SCREEN_SHOW = "is_dynamic_lockscreen_shown";
    private static final String KEY_SUNLIGHT_MODE_AVAILABLE = "config_sunlight_mode_available";
    private static final float MAX_A;
    private static final float MAX_DIFF;
    public static final float MAX_GALLERY_HDR_FACTOR = 2.25f;
    public static final float MIN_GALLERY_HDR_FACTOR = 1.0f;
    private static final int MSG_UPDATE_DOLBY_STATE = 2;
    private static final int MSG_UPDATE_FOREGROUND_APP = 4;
    private static final int MSG_UPDATE_FOREGROUND_APP_SYNC = 3;
    private static final int MSG_UPDATE_GRAY_SCALE = 1;
    private static final String PACKAGE_DIM_SYSTEM = "system";
    private static final boolean SUPPORT_IDLE_DIM;
    private static final Resources SYSTEM_RESOURCES;
    private static final String TAG = "DisplayPowerControllerImpl";
    private static final int TRANSACTION_NOTIFY_BRIGHTNESS = 31104;
    private static final int TRANSACTION_NOTIFY_DIM = 31107;
    private static final float V1;
    private static final float V2;
    private static final boolean mSupportGalleryHdr;
    private IActivityTaskManager mActivityTaskManager;
    private float mActualScreenOnBrightness;
    private boolean mAppliedDimming;
    private boolean mAppliedLowPower;
    private boolean mAppliedScreenBrightnessOverride;
    private boolean mAppliedSunlightMode;
    private boolean mAutoBrightnessEnable;
    private AutomaticBrightnessControllerImpl mAutomaticBrightnessControllerImpl;
    private int mBCBCState;
    private BrightnessMappingStrategy mBrightnessMapper;
    private boolean mColorInversionEnabled;
    private Context mContext;
    private float mCurrentBrightness;
    private float mCurrentSdrBrightness;
    private boolean mCurtainAnimationAvailable;
    private boolean mCurtainAnimationEnabled;
    private float mDesiredBrightness;
    private int mDesiredBrightnessInt;
    private DisplayDeviceConfig mDisplayDeviceConfig;
    private int mDisplayId;
    private DisplayManagerServiceImpl mDisplayMangerServiceImpl;
    private DisplayPowerController mDisplayPowerController;
    private boolean mDolbyStateEnable;
    private boolean mDozeInLowBrightness;
    private Boolean mFolded;
    private String mForegroundAppPackageName;
    private boolean mGalleryHdrThrottled;
    private DisplayPowerControllerImplHandler mHandler;
    private HighBrightnessModeController mHbmController;
    private HighBrightnessModeController.HdrStateListener mHdrStateListener;
    private IBinder mISurfaceFlinger;
    private boolean mInitialBCBCParameters;
    private boolean mIsDynamicLockScreenShowing;
    private boolean mIsGalleryHdrEnable;
    private float mK1;
    private float mK2;
    private float mK3;
    private float mK4;
    private boolean mLastAnimating;
    private float mLastSettingsBrightnessBeforeApplySunlight;
    private boolean mLastSlowChange;
    private LogicalDisplay mLogicalDisplay;
    private MiuiBrightnessChangeTracker mMiuiBrightnessChangeTracker;
    protected MiuiDisplayCloudController mMiuiDisplayCloudController;
    private boolean mPendingApplyGrayBrightness;
    private String mPendingForegroundAppPackageName;
    private boolean mPendingShowCurtainAnimation;
    private WindowManagerPolicy mPolicy;
    private PowerManager mPowerManager;
    private float[] mRealtimeArrayD;
    private float mScreenBrightnessRangeMaximum;
    private float mScreenBrightnessRangeMinimum;
    private SettingsObserver mSettingsObserver;
    private boolean mShouldDimming;
    private SunlightController mSunlightController;
    private boolean mSunlightModeActive;
    private boolean mSunlightModeAvailable;
    private DisplayPowerControllerStub.SunlightStateChangedListener mSunlightStateListener;
    private float mTargetBrightness;
    private float mTargetSdrBrightness;
    private TaskStackListenerImpl mTaskStackListener;
    private TemperatureController mTemperatureController;
    private ThermalObserver mThermalObserver;
    private boolean mUpdateBrightnessAnimInfoEnable;
    private static boolean DEBUG = false;
    private static final boolean SUPPORT_BCBC_BY_AMBIENT_LUX = FeatureParser.getBoolean("support_bcbc_by_ambient_lux", false);
    private static final float[] mBCBCLuxThreshold = {10.0f, 100.0f};
    private static final float[] mBCBCNitDecreaseThreshold = {5.0f, 12.0f};
    private static boolean BCBC_ENABLE = SystemProperties.getBoolean("ro.vendor.bcbc.enable", false);
    private float mDozeScreenBrightness = -1.0f;
    private float mRealtimeMaxA = MAX_A;
    private float mRealtimeMaxDiff = MAX_DIFF;
    private float mBackLightRate = -1.0f;
    private float mGrayBrightnessFactor = 1.0f;
    private float mLastBrightness = -1.0f;
    private float mLastGrayScale = -1.0f;
    private float mGrayScale = Float.NaN;
    private final boolean SUPPORT_DOLBY_VERSION_BRIGHTEN = FeatureParser.getBoolean("support_dolby_version_brighten", false);
    private final boolean SUPPORT_MANUAL_DIMMING = FeatureParser.getBoolean("support_manual_dimming", false);
    private final boolean SUPPORT_TEMEPERATURE_CONTROL = FeatureParser.getBoolean("support_temperature_control", false);
    private int mLastDisplayState = 2;
    private float mCurrentGalleryHdrBoostFactor = 1.0f;
    private AutomaticBrightnessControllerImpl.CloudControllerListener mCloudListener = new AutomaticBrightnessControllerImpl.CloudControllerListener() { // from class: com.android.server.display.DisplayPowerControllerImpl.3
        @Override // com.android.server.display.AutomaticBrightnessControllerImpl.CloudControllerListener
        public boolean isPointLightSourceDetectorEnable() {
            if (DisplayPowerControllerImpl.this.mMiuiDisplayCloudController != null) {
                return DisplayPowerControllerImpl.this.mMiuiDisplayCloudController.isPointLightSourceDetectorEnable();
            }
            return false;
        }

        @Override // com.android.server.display.AutomaticBrightnessControllerImpl.CloudControllerListener
        public boolean isAutoBrightnessStatisticsEventEnable() {
            if (DisplayPowerControllerImpl.this.mMiuiDisplayCloudController != null) {
                return DisplayPowerControllerImpl.this.mMiuiDisplayCloudController.isAutoBrightnessStatisticsEventEnable();
            }
            return false;
        }
    };

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<DisplayPowerControllerImpl> {

        /* compiled from: DisplayPowerControllerImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final DisplayPowerControllerImpl INSTANCE = new DisplayPowerControllerImpl();
        }

        public DisplayPowerControllerImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public DisplayPowerControllerImpl provideNewInstance() {
            return new DisplayPowerControllerImpl();
        }
    }

    public DisplayPowerControllerImpl() {
        float[] fArr = DATA_D;
        this.mRealtimeArrayD = Arrays.copyOf(fArr, fArr.length);
    }

    static {
        Resources system = Resources.getSystem();
        SYSTEM_RESOURCES = system;
        DATA_D = getFloatArray(system.obtainTypedArray(285409303));
        V1 = system.getFloat(285671462);
        V2 = system.getFloat(285671461);
        MAX_DIFF = system.getFloat(285671457);
        MAX_A = system.getFloat(285671459);
        GRAY_BRIGHTNESS_RATE = system.getFloat(285671464);
        IS_FOLDABLE_DEVICE = SystemProperties.getInt("persist.sys.muiltdisplay_type", 0) == 2;
        SUPPORT_IDLE_DIM = FeatureParser.getBoolean("support_idle_dim", false);
        mSupportGalleryHdr = FeatureParser.getBoolean("support_gallery_hdr", false);
    }

    public void init(DisplayPowerController displayPowerController, Context context, Looper looper, int displayId, float brightnessMin, float brightnessMax) {
        this.mContext = context;
        this.mDisplayPowerController = displayPowerController;
        this.mDisplayId = displayId;
        if (DisplayManagerServiceStub.getInstance() instanceof DisplayManagerServiceImpl) {
            this.mDisplayMangerServiceImpl = (DisplayManagerServiceImpl) DisplayManagerServiceStub.getInstance();
        }
        DisplayManagerServiceImpl displayManagerServiceImpl = this.mDisplayMangerServiceImpl;
        if (displayManagerServiceImpl != null && this.mDisplayId == 0) {
            displayManagerServiceImpl.setUpDisplayPowerControllerImpl(this);
        }
        boolean z = true;
        if (this.mDisplayId != 0 || !FeatureParser.getBoolean(KEY_SUNLIGHT_MODE_AVAILABLE, true)) {
            z = false;
        }
        this.mSunlightModeAvailable = z;
        this.mCurtainAnimationAvailable = this.mContext.getResources().getBoolean(285540375);
        this.mHandler = new DisplayPowerControllerImplHandler(looper);
        if (this.mSunlightModeAvailable) {
            this.mSunlightController = new SunlightController(context, this, looper, displayId);
        }
        this.mISurfaceFlinger = ServiceManager.getService("SurfaceFlinger");
        this.mScreenBrightnessRangeMinimum = brightnessMin;
        this.mScreenBrightnessRangeMaximum = brightnessMax;
        this.mSettingsObserver = new SettingsObserver(this.mHandler);
        registerSettingsObserver();
        this.mActivityTaskManager = ActivityTaskManager.getService();
        this.mTaskStackListener = new TaskStackListenerImpl();
        this.mPolicy = (WindowManagerPolicy) LocalServices.getService(WindowManagerPolicy.class);
        this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
        if (displayId == 0) {
            this.mMiuiBrightnessChangeTracker = new MiuiBrightnessChangeTracker(context);
            this.mMiuiDisplayCloudController = new MiuiDisplayCloudController(looper, context);
            this.mThermalObserver = new ThermalObserver(BackgroundThread.get().getLooper(), this);
            if (this.SUPPORT_TEMEPERATURE_CONTROL) {
                this.mTemperatureController = new TemperatureController(this.mContext, BackgroundThread.get().getLooper(), this.mDisplayPowerController, this.mMiuiDisplayCloudController);
            }
            this.mHandler.post(new Runnable() { // from class: com.android.server.display.DisplayPowerControllerImpl$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    DisplayPowerControllerImpl.this.m570x83a39a7f();
                }
            });
            if (IS_FOLDABLE_DEVICE) {
                DeviceStateManager deviceStateManager = (DeviceStateManager) context.getSystemService(DeviceStateManager.class);
                deviceStateManager.registerCallback(new HandlerExecutor(this.mHandler), new DeviceStateManager.FoldStateListener(context, new Consumer() { // from class: com.android.server.display.DisplayPowerControllerImpl$$ExternalSyntheticLambda1
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        DisplayPowerControllerImpl.this.m571xacf7efc0((Boolean) obj);
                    }
                }));
            }
        }
        computeBCBCAdjustmentParams();
        this.mHdrStateListener = new HighBrightnessModeController.HdrStateListener() { // from class: com.android.server.display.DisplayPowerControllerImpl.1
            public void onHdrStateChanged() {
                if (DisplayPowerControllerImpl.this.mAutoBrightnessEnable) {
                    DisplayPowerControllerImpl.this.resetBCBCState();
                }
            }
        };
    }

    /* renamed from: lambda$init$1$com-android-server-display-DisplayPowerControllerImpl */
    public /* synthetic */ void m571xacf7efc0(Boolean folded) {
        setDeviceFolded(folded.booleanValue());
    }

    private void setDeviceFolded(boolean folded) {
        Boolean bool = this.mFolded;
        if (bool != null && bool.booleanValue() == folded) {
            return;
        }
        this.mFolded = Boolean.valueOf(folded);
        OneTrackFoldStateHelper.getInstance().oneTrackFoldState(folded);
        if (DEBUG) {
            Slog.d(TAG, "mFolded: " + this.mFolded);
        }
        boolean isInteractive = this.mPowerManager.isInteractive();
        if (this.mCurtainAnimationAvailable && this.mCurtainAnimationEnabled && !this.mIsDynamicLockScreenShowing && !isInteractive && !isFolded() && !this.mPendingShowCurtainAnimation) {
            this.mPendingShowCurtainAnimation = true;
        }
    }

    public boolean isCurtainAnimationNeeded() {
        return this.mPendingShowCurtainAnimation;
    }

    public void onCurtainAnimationFinished() {
        if (this.mPendingShowCurtainAnimation) {
            this.mPendingShowCurtainAnimation = false;
        }
    }

    public boolean isFolded() {
        Boolean bool = this.mFolded;
        if (bool != null) {
            return bool.booleanValue();
        }
        return false;
    }

    private void registerSettingsObserver() {
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor("accessibility_display_inversion_enabled"), false, this.mSettingsObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("screen_brightness_mode"), false, this.mSettingsObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(KEY_CURTAIN_ANIM_ENABLED), false, this.mSettingsObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(KEY_IS_DYNAMIC_LOCK_SCREEN_SHOW), false, this.mSettingsObserver, -1);
        loadSettings();
    }

    public void mayBeReportUserDisableSunlightTemporary(float tempBrightness) {
        if (this.mSunlightModeAvailable && this.mAppliedSunlightMode && !Float.isNaN(tempBrightness)) {
            this.mSunlightController.setSunlightModeDisabledByUserTemporary();
        }
    }

    public void updateAmbientLightSensor(Sensor lightSensor) {
        SunlightController sunlightController = this.mSunlightController;
        if (sunlightController != null) {
            sunlightController.updateAmbientLightSensor(lightSensor);
        }
    }

    public void setSunlightListener(DisplayPowerControllerStub.SunlightStateChangedListener listener) {
        this.mSunlightStateListener = listener;
    }

    private boolean isAllowedUseSunlightMode() {
        return this.mSunlightModeActive && !isSunlightModeDisabledByUser();
    }

    public float canApplyingSunlightBrightness(float currentScreenBrightness, float brightness) {
        DisplayPowerControllerStub.SunlightStateChangedListener sunlightStateChangedListener;
        float tempBrightness = brightness;
        if (this.mSunlightModeAvailable && isAllowedUseSunlightMode()) {
            if (this.mDisplayDeviceConfig.getHighBrightnessModeData() != null) {
                tempBrightness = this.mDisplayDeviceConfig.getHighBrightnessModeData().transitionPoint;
            } else {
                tempBrightness = 1.0f;
            }
            if (!this.mAppliedSunlightMode) {
                this.mLastSettingsBrightnessBeforeApplySunlight = currentScreenBrightness;
            }
            this.mAppliedSunlightMode = true;
            DisplayPowerControllerStub.SunlightStateChangedListener sunlightStateChangedListener2 = this.mSunlightStateListener;
            if (sunlightStateChangedListener2 != null) {
                sunlightStateChangedListener2.updateScreenBrightnessSettingDueToSunlight(tempBrightness);
            }
            if (DEBUG) {
                Slog.d(TAG, "updatePowerState: appling sunlight mode brightness.last brightness:" + this.mLastSettingsBrightnessBeforeApplySunlight);
            }
        } else if (this.mAppliedSunlightMode) {
            this.mAppliedSunlightMode = false;
            if (!isSunlightModeDisabledByUser() && (sunlightStateChangedListener = this.mSunlightStateListener) != null) {
                sunlightStateChangedListener.updateScreenBrightnessSettingDueToSunlight(this.mLastSettingsBrightnessBeforeApplySunlight);
            }
            if (DEBUG) {
                Slog.d(TAG, "updatePowerState: exit sunlight mode brightness. reset brightness: " + this.mLastSettingsBrightnessBeforeApplySunlight);
            }
        }
        return tempBrightness;
    }

    private boolean isSunlightModeDisabledByUser() {
        return this.mSunlightController.isSunlightModeDisabledByUser();
    }

    @Override // com.android.server.display.SunlightController.Callback
    public void notifySunlightStateChange(boolean active) {
        if (DEBUG) {
            Slog.d(TAG, "notifySunlightStateChange: " + active);
        }
        this.mSunlightModeActive = active;
        DisplayPowerControllerStub.SunlightStateChangedListener sunlightStateChangedListener = this.mSunlightStateListener;
        if (sunlightStateChangedListener != null) {
            sunlightStateChangedListener.onSunlightStateChange();
        }
    }

    private void sendSurfaceFlingerActualBrightness(int brightness) {
        if (DEBUG) {
            Slog.d(TAG, "sendSurfaceFlingerActualBrightness, brightness = " + brightness);
        }
        if (this.mISurfaceFlinger != null) {
            Parcel data = Parcel.obtain();
            data.writeInterfaceToken("android.ui.ISurfaceComposer");
            data.writeInt(brightness);
            try {
                try {
                    this.mISurfaceFlinger.transact(TRANSACTION_NOTIFY_BRIGHTNESS, data, null, 1);
                } catch (RemoteException | SecurityException ex) {
                    Slog.e(TAG, "Failed to send brightness to SurfaceFlinger", ex);
                }
            } finally {
                data.recycle();
            }
        }
    }

    public void sendBrightnessToSurfaceFlingerIfNeeded(float target, float dozeBrightness, boolean isDimming) {
        float f;
        switch (this.mDisplayPowerController.getDisplayPowerState().getScreenState()) {
            case 1:
            case 3:
                if (!BrightnessSynchronizer.floatEquals(dozeBrightness, this.mDozeScreenBrightness)) {
                    this.mDozeScreenBrightness = target;
                    boolean z = true;
                    if (BrightnessSynchronizer.brightnessFloatToInt(dozeBrightness) != 1) {
                        z = false;
                    }
                    this.mDozeInLowBrightness = z;
                }
                float pendingBrightness = this.mDozeInLowBrightness ? this.mDozeScreenBrightness : this.mActualScreenOnBrightness;
                if (pendingBrightness != this.mDesiredBrightness) {
                    this.mDesiredBrightness = pendingBrightness;
                    int brightnessFloatToInt = BrightnessSynchronizer.brightnessFloatToInt(pendingBrightness);
                    this.mDesiredBrightnessInt = brightnessFloatToInt;
                    sendSurfaceFlingerActualBrightness(brightnessFloatToInt);
                    return;
                }
                return;
            case 2:
                if (target != this.mActualScreenOnBrightness && !isDimming) {
                    if (this.mSunlightModeActive) {
                        f = BrightnessSynchronizer.brightnessFloatToInt(this.mLastSettingsBrightnessBeforeApplySunlight);
                    } else {
                        f = target;
                    }
                    this.mActualScreenOnBrightness = f;
                    return;
                }
                return;
            default:
                return;
        }
    }

    public void notifyBrightnessChangeIfNeeded(boolean screenOn, float brightness, boolean userInitiatedChange, boolean useAutoBrightness, float brightnessOverrideFromWindow, boolean lowPowerMode, float ambientLux, float userDataPoint, boolean defaultConfig, Spline brightnessSpline) {
        MiuiBrightnessChangeTracker miuiBrightnessChangeTracker = this.mMiuiBrightnessChangeTracker;
        if (miuiBrightnessChangeTracker != null) {
            miuiBrightnessChangeTracker.notifyBrightnessEventIfNeeded(screenOn, brightness, userInitiatedChange, useAutoBrightness, brightnessOverrideFromWindow, lowPowerMode, ambientLux, userDataPoint, defaultConfig, brightnessSpline, this.mSunlightModeActive);
        }
    }

    public float adjustBrightness(float brightness, boolean autoBrightnessEnabled) {
        if (this.mDisplayPowerController.getDisplayPowerState().getScreenState() == 2) {
            if (autoBrightnessEnabled) {
                if (!isKeyguardOn()) {
                    brightness = adjustBrightnessBCBC(brightness);
                }
            } else {
                brightness = canApplyingSunlightBrightness(this.mDisplayPowerController.getScreenBrightnessSetting(), brightness);
            }
            return adjustBrightnessByTemperature(brightness);
        }
        return brightness;
    }

    /* JADX WARN: Removed duplicated region for block: B:29:0x0062  */
    /* JADX WARN: Removed duplicated region for block: B:30:0x0067  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private float adjustBrightnessBCBC(float r7) {
        /*
            r6 = this;
            r0 = r7
            boolean r1 = r6.isGrayScaleLegal()
            if (r1 == 0) goto L6f
            float r1 = r6.mGrayScale
            float r7 = r6.calculateBrightnessBCBC(r7, r1)
            int r1 = (r7 > r0 ? 1 : (r7 == r0 ? 0 : -1))
            r2 = 1
            java.lang.String r3 = "DisplayPowerControllerImpl"
            r4 = -1082130432(0xffffffffbf800000, float:-1.0)
            if (r1 == 0) goto L34
            float r1 = r6.mLastBrightness
            int r5 = (r1 > r0 ? 1 : (r1 == r0 ? 0 : -1))
            if (r5 == 0) goto L20
            int r1 = (r1 > r4 ? 1 : (r1 == r4 ? 0 : -1))
            if (r1 != 0) goto L34
        L20:
            float r1 = r6.mLastGrayScale
            float r5 = r6.mGrayScale
            int r1 = (r1 > r5 ? 1 : (r1 == r5 ? 0 : -1))
            if (r1 == 0) goto L34
            r6.mPendingApplyGrayBrightness = r2
            boolean r1 = com.android.server.display.DisplayPowerControllerImpl.DEBUG
            if (r1 == 0) goto L5e
            java.lang.String r1 = "apply gray brightness"
            android.util.Slog.d(r3, r1)
            goto L5e
        L34:
            float r1 = r6.mLastGrayScale
            float r5 = r6.mGrayScale
            int r1 = (r1 > r5 ? 1 : (r1 == r5 ? 0 : -1))
            if (r1 == 0) goto L52
            int r1 = (r0 > r7 ? 1 : (r0 == r7 ? 0 : -1))
            if (r1 != 0) goto L52
            float r1 = r6.mLastBrightness
            int r1 = (r1 > r4 ? 1 : (r1 == r4 ? 0 : -1))
            if (r1 == 0) goto L52
            r6.mPendingApplyGrayBrightness = r2
            boolean r1 = com.android.server.display.DisplayPowerControllerImpl.DEBUG
            if (r1 == 0) goto L5e
            java.lang.String r1 = "exit gray brightness"
            android.util.Slog.d(r3, r1)
            goto L5e
        L52:
            r1 = 0
            r6.mPendingApplyGrayBrightness = r1
            boolean r1 = com.android.server.display.DisplayPowerControllerImpl.DEBUG
            if (r1 == 0) goto L5e
            java.lang.String r1 = "skip gray brightness"
            android.util.Slog.d(r3, r1)
        L5e:
            boolean r1 = r6.mPendingApplyGrayBrightness
            if (r1 == 0) goto L67
            float r1 = com.android.server.display.DisplayPowerControllerImpl.GRAY_BRIGHTNESS_RATE
            r6.mBackLightRate = r1
            goto L69
        L67:
            r6.mBackLightRate = r4
        L69:
            float r1 = r6.mGrayScale
            r6.mLastGrayScale = r1
            r6.mLastBrightness = r0
        L6f:
            return r7
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.display.DisplayPowerControllerImpl.adjustBrightnessBCBC(float):float");
    }

    public void resetBCBCState() {
        this.mGrayScale = Float.NaN;
        this.mLastGrayScale = -1.0f;
        this.mLastBrightness = -1.0f;
        this.mBackLightRate = -1.0f;
        this.mPendingApplyGrayBrightness = false;
        updateBCBCStateIfNeeded();
    }

    private boolean isGrayScaleLegal() {
        return !Float.isNaN(this.mGrayScale) && this.mGrayScale > MiuiFreeformPinManagerService.EDGE_AREA;
    }

    public void updateBrightnessChangeStatus(boolean animating, int displayState, boolean slowChange, boolean appliedDimming, boolean appliedLowPower, float currentBrightness, float currentSdrBrightness, float targetBrightness, float targetSdrBrightness) {
        this.mLastAnimating = animating;
        this.mLastDisplayState = displayState;
        this.mCurrentBrightness = currentBrightness;
        this.mCurrentSdrBrightness = currentSdrBrightness;
        this.mLastSlowChange = slowChange;
        this.mAppliedDimming = appliedDimming;
        this.mAppliedLowPower = appliedLowPower;
        this.mTargetBrightness = targetBrightness;
        this.mTargetSdrBrightness = targetSdrBrightness;
        Slog.d(TAG, "updateBrightnessChangeStatus: animating: " + animating + ", displayState: " + displayState + ", slowChange: " + slowChange + ", appliedDimming: " + appliedDimming + ", appliedLowPower: " + appliedLowPower + ", currentBrightness: " + currentBrightness + ", currentSdrBrightness: " + currentSdrBrightness + ", targetBrightness: " + targetBrightness + ", targetSdrBrightness: " + targetSdrBrightness);
        AutomaticBrightnessControllerImpl automaticBrightnessControllerImpl = this.mAutomaticBrightnessControllerImpl;
        if (automaticBrightnessControllerImpl != null) {
            automaticBrightnessControllerImpl.updateSlowChangeStatus(slowChange, appliedDimming, appliedLowPower, currentBrightness);
        }
    }

    public float adjustBackLightRate(float inRate) {
        TemperatureController temperatureController;
        float outRate = inRate;
        String reason = "no_changed";
        if (inRate > MiuiFreeformPinManagerService.EDGE_AREA && this.mPendingApplyGrayBrightness) {
            this.mPendingApplyGrayBrightness = false;
            outRate = this.mBackLightRate;
            reason = "bcbc";
        }
        if (this.SUPPORT_TEMEPERATURE_CONTROL && (temperatureController = this.mTemperatureController) != null && temperatureController.getAnimating()) {
            outRate = 0.007354082f;
            this.mTemperatureController.resetAnimation();
            reason = "temperature control brightness";
        }
        Slog.i(TAG, "Animating brightness rate: " + inRate + ", outRate: " + outRate + ", reason: " + reason);
        return outRate;
    }

    private float calculateBrightnessBCBC(float brightness, float grayScale) {
        float ratio = brightness / this.mScreenBrightnessRangeMaximum;
        float f = V2;
        if (grayScale > f) {
            float[] fArr = this.mRealtimeArrayD;
            float f2 = fArr[4];
            if (ratio > f2 && ratio <= fArr[5]) {
                this.mGrayBrightnessFactor = (this.mK3 * (ratio - f2) * (grayScale - f)) + 1.0f;
            } else if (ratio > fArr[5] && ratio <= fArr[6]) {
                this.mGrayBrightnessFactor = 1.0f - ((this.mRealtimeMaxDiff * (grayScale - f)) / ((1.0f - f) * ratio));
            } else {
                if (ratio > fArr[6]) {
                    float f3 = fArr[7];
                    if (ratio < f3) {
                        this.mGrayBrightnessFactor = (this.mK4 * (ratio - f3) * (grayScale - f)) + 1.0f;
                    }
                }
                this.mGrayBrightnessFactor = 1.0f;
            }
        } else {
            if (grayScale > MiuiFreeformPinManagerService.EDGE_AREA) {
                float f4 = V1;
                if (grayScale < f4) {
                    float[] fArr2 = this.mRealtimeArrayD;
                    float f5 = fArr2[0];
                    if (ratio > f5 && ratio <= fArr2[1]) {
                        this.mGrayBrightnessFactor = (this.mK1 * (ratio - f5) * (grayScale - f4)) + 1.0f;
                    } else if (ratio > fArr2[1] && ratio <= fArr2[2]) {
                        float f6 = this.mRealtimeMaxA;
                        this.mGrayBrightnessFactor = (f6 + 1.0f) - ((f6 / f4) * grayScale);
                    } else {
                        if (ratio > fArr2[2]) {
                            float f7 = fArr2[3];
                            if (ratio < f7) {
                                this.mGrayBrightnessFactor = (this.mK2 * (ratio - f7) * (grayScale - f4)) + 1.0f;
                            }
                        }
                        this.mGrayBrightnessFactor = 1.0f;
                    }
                }
            }
            this.mGrayBrightnessFactor = 1.0f;
        }
        float f8 = this.mGrayBrightnessFactor;
        float outBrightness = brightness * f8;
        if (SUPPORT_BCBC_BY_AMBIENT_LUX && f8 < 1.0f) {
            outBrightness = adjustBrightnessByLux(brightness, outBrightness);
        }
        if (DEBUG) {
            Slog.d(TAG, " grayScale = " + grayScale + " factor = " + this.mGrayBrightnessFactor + " inBrightness = " + brightness + " outBrightness = " + outBrightness);
        }
        return outBrightness;
    }

    /* JADX WARN: Removed duplicated region for block: B:18:0x0054  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private float adjustBrightnessByLux(float r9, float r10) {
        /*
            r8 = this;
            r0 = r10
            com.android.server.display.BrightnessMappingStrategy r1 = r8.mBrightnessMapper
            float r1 = r1.convertToNits(r9)
            com.android.server.display.BrightnessMappingStrategy r2 = r8.mBrightnessMapper
            float r2 = r2.convertToNits(r10)
            com.android.server.display.AutomaticBrightnessControllerImpl r3 = r8.mAutomaticBrightnessControllerImpl
            float r3 = r3.getCurrentAmbientLux()
            float[] r4 = com.android.server.display.DisplayPowerControllerImpl.mBCBCLuxThreshold
            r5 = 0
            r6 = r4[r5]
            int r6 = (r3 > r6 ? 1 : (r3 == r6 ? 0 : -1))
            if (r6 >= 0) goto L33
            int r6 = (r1 > r2 ? 1 : (r1 == r2 ? 0 : -1))
            if (r6 <= 0) goto L33
            float r6 = r1 - r2
            float[] r7 = com.android.server.display.DisplayPowerControllerImpl.mBCBCNitDecreaseThreshold
            r5 = r7[r5]
            int r6 = (r6 > r5 ? 1 : (r6 == r5 ? 0 : -1))
            if (r6 <= 0) goto L33
            com.android.server.display.BrightnessMappingStrategy r4 = r8.mBrightnessMapper
            float r5 = r1 - r5
            float r0 = r4.convertToBrightness(r5)
            goto L50
        L33:
            r5 = 1
            r4 = r4[r5]
            int r4 = (r3 > r4 ? 1 : (r3 == r4 ? 0 : -1))
            if (r4 >= 0) goto L50
            int r4 = (r1 > r2 ? 1 : (r1 == r2 ? 0 : -1))
            if (r4 <= 0) goto L50
            float r4 = r1 - r2
            float[] r6 = com.android.server.display.DisplayPowerControllerImpl.mBCBCNitDecreaseThreshold
            r5 = r6[r5]
            int r4 = (r4 > r5 ? 1 : (r4 == r5 ? 0 : -1))
            if (r4 <= 0) goto L50
            com.android.server.display.BrightnessMappingStrategy r4 = r8.mBrightnessMapper
            float r5 = r1 - r5
            float r0 = r4.convertToBrightness(r5)
        L50:
            boolean r4 = com.android.server.display.DisplayPowerControllerImpl.DEBUG
            if (r4 == 0) goto L8a
            java.lang.StringBuilder r4 = new java.lang.StringBuilder
            r4.<init>()
            java.lang.String r5 = "adjustBrightnessByLux: currentLux: "
            java.lang.StringBuilder r4 = r4.append(r5)
            java.lang.StringBuilder r4 = r4.append(r3)
            java.lang.String r5 = ", preNit: "
            java.lang.StringBuilder r4 = r4.append(r5)
            java.lang.StringBuilder r4 = r4.append(r1)
            java.lang.String r5 = ", currentNit: "
            java.lang.StringBuilder r4 = r4.append(r5)
            java.lang.StringBuilder r4 = r4.append(r2)
            java.lang.String r5 = ", currentBrightness: "
            java.lang.StringBuilder r4 = r4.append(r5)
            java.lang.StringBuilder r4 = r4.append(r10)
            java.lang.String r4 = r4.toString()
            java.lang.String r5 = "DisplayPowerControllerImpl"
            android.util.Slog.d(r5, r4)
        L8a:
            return r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.display.DisplayPowerControllerImpl.adjustBrightnessByLux(float, float):float");
    }

    float getGrayBrightnessFactor() {
        return this.mGrayBrightnessFactor;
    }

    private static float[] getFloatArray(TypedArray array) {
        int length = array.length();
        float[] floatArray = new float[length];
        for (int i = 0; i < length; i++) {
            floatArray[i] = array.getFloat(i, Float.NaN);
        }
        array.recycle();
        return floatArray;
    }

    private void computeBCBCAdjustmentParams() {
        float f = this.mRealtimeMaxA;
        float f2 = V1;
        float[] fArr = this.mRealtimeArrayD;
        this.mK1 = (-f) / ((fArr[1] - fArr[0]) * f2);
        this.mK2 = f / (f2 * (fArr[3] - fArr[2]));
        float f3 = this.mRealtimeMaxDiff;
        float f4 = fArr[5];
        float f5 = V2;
        this.mK3 = (-f3) / (((1.0f - f5) * f4) * (f4 - fArr[4]));
        float f6 = fArr[6];
        this.mK4 = f3 / (((1.0f - f5) * f6) * (fArr[7] - f6));
    }

    private void initDisplayCloudControllerIfNeeded() {
        MiuiDisplayCloudController miuiDisplayCloudController;
        BrightnessMappingStrategy brightnessMappingStrategy = this.mBrightnessMapper;
        if ((brightnessMappingStrategy instanceof MiuiPhysicalBrightnessMappingStrategy) && (miuiDisplayCloudController = this.mMiuiDisplayCloudController) != null) {
            ((MiuiPhysicalBrightnessMappingStrategy) brightnessMappingStrategy).setMiuiDisplayCloudController(miuiDisplayCloudController);
        }
    }

    public boolean isColorInversionEnabled() {
        return this.mColorInversionEnabled;
    }

    public void updateColorInversionEnabled() {
        boolean z = false;
        if (Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "accessibility_display_inversion_enabled", 0, -2) != 0) {
            z = true;
        }
        this.mColorInversionEnabled = z;
    }

    public void updateGalleryHdrState(boolean enable) {
        if (this.mIsGalleryHdrEnable != enable) {
            this.mIsGalleryHdrEnable = enable;
            this.mDisplayPowerController.updateBrightness();
        }
    }

    public void updateGalleryHdrThermalThrottler(boolean throttled) {
        if (this.mGalleryHdrThrottled != throttled) {
            this.mGalleryHdrThrottled = throttled;
            this.mDisplayPowerController.updateBrightness();
        }
    }

    public boolean isGalleryHdrEnable() {
        return mSupportGalleryHdr && this.mIsGalleryHdrEnable && !this.mGalleryHdrThrottled;
    }

    public boolean isEnterGalleryHdr() {
        if (isGalleryHdrEnable() && !this.mLastSlowChange) {
            float f = this.mTargetBrightness;
            if (f > this.mTargetSdrBrightness && !this.mAppliedDimming && f > this.mCurrentBrightness) {
                return true;
            }
        }
        return false;
    }

    public boolean isExitGalleryHdr() {
        return this.mCurrentBrightness != MiuiFreeformPinManagerService.EDGE_AREA && !isGalleryHdrEnable() && this.mCurrentGalleryHdrBoostFactor > 1.0f;
    }

    public float getGalleryHdrBoostFactor(float sdrBacklight, float hdrBacklight) {
        if (isGalleryHdrEnable() || this.mCurrentGalleryHdrBoostFactor != 1.0f) {
            String tempReason = "null";
            float hdrNit = this.mDisplayDeviceConfig.getNitsFromBacklight(hdrBacklight);
            float sdrNit = this.mDisplayDeviceConfig.getNitsFromBacklight(sdrBacklight);
            float factor = calculateGalleryHdrBoostFactor(hdrNit, sdrNit);
            if (isEnterGalleryHdr()) {
                tempReason = "enter_gallery_hdr_boost";
            } else if (isExitGalleryHdr()) {
                tempReason = "exit_gallery_hdr_boost";
            } else if (this.mAppliedDimming && isGalleryHdrEnable()) {
                tempReason = "enter_dim_state";
            }
            this.mCurrentGalleryHdrBoostFactor = factor;
            if (DEBUG) {
                Slog.d(TAG, "getGalleryHdrBoostFactor: reason:" + tempReason + ", hdrBrightness: " + hdrBacklight + ", sdrBrightness: " + sdrBacklight + ", mCurrentBrightness: " + this.mCurrentBrightness + ", mCurrentSdrBrightness: " + this.mCurrentSdrBrightness + ", hdrNit: " + hdrNit + ", sdrNit: " + sdrNit + ", factor: " + factor);
            }
            return factor;
        }
        return 1.0f;
    }

    private float calculateGalleryHdrBoostFactor(float hdrNit, float sdrNit) {
        if (hdrNit == MiuiFreeformPinManagerService.EDGE_AREA || sdrNit == MiuiFreeformPinManagerService.EDGE_AREA || Float.isNaN(sdrNit) || Float.isNaN(hdrNit)) {
            return 1.0f;
        }
        float factor = BigDecimal.valueOf(hdrNit / sdrNit).setScale(3, RoundingMode.HALF_UP).floatValue();
        return MathUtils.constrain(factor, 1.0f, 2.25f);
    }

    public void notifySystemBrightnessChange() {
        if (isOverrideBrightnessPolicyEnable() && this.mAppliedScreenBrightnessOverride) {
            WindowManagerServiceStub.get().notifySystemBrightnessChange();
            this.mAppliedScreenBrightnessOverride = false;
        }
    }

    public void setAppliedScreenBrightnessOverride(boolean isApplied) {
        if (this.mAppliedScreenBrightnessOverride != isApplied) {
            if (this.mAutoBrightnessEnable && !isApplied) {
                resetBCBCState();
            }
            this.mAppliedScreenBrightnessOverride = isApplied;
        }
    }

    private boolean isOverrideBrightnessPolicyEnable() {
        MiuiDisplayCloudController miuiDisplayCloudController = this.mMiuiDisplayCloudController;
        if (miuiDisplayCloudController != null) {
            return miuiDisplayCloudController.isOverrideBrightnessPolicyEnable();
        }
        return false;
    }

    /* loaded from: classes.dex */
    public final class SettingsObserver extends ContentObserver {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public SettingsObserver(Handler handler) {
            super(handler);
            DisplayPowerControllerImpl.this = r1;
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            char c;
            String lastPathSegment = uri.getLastPathSegment();
            switch (lastPathSegment.hashCode()) {
                case -693072130:
                    if (lastPathSegment.equals("screen_brightness_mode")) {
                        c = 1;
                        break;
                    }
                    c = 65535;
                    break;
                case -551230169:
                    if (lastPathSegment.equals("accessibility_display_inversion_enabled")) {
                        c = 0;
                        break;
                    }
                    c = 65535;
                    break;
                case -267787360:
                    if (lastPathSegment.equals(DisplayPowerControllerImpl.KEY_CURTAIN_ANIM_ENABLED)) {
                        c = 2;
                        break;
                    }
                    c = 65535;
                    break;
                case 1108440478:
                    if (lastPathSegment.equals(DisplayPowerControllerImpl.KEY_IS_DYNAMIC_LOCK_SCREEN_SHOW)) {
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
                    DisplayPowerControllerImpl.this.updateColorInversionEnabled();
                    return;
                case 1:
                    DisplayPowerControllerImpl.this.updateAutoBrightnessMode();
                    return;
                case 2:
                case 3:
                    DisplayPowerControllerImpl.this.updateCurtainAnimationEnabled();
                    return;
                default:
                    return;
            }
        }
    }

    public void receiveNoticeFromDisplayPowerController(int code, Bundle bundle) {
        switch (code) {
            case 1:
                updateGrayScale(bundle);
                return;
            case 2:
            default:
                return;
            case 3:
                updateDolbyState(bundle);
                return;
        }
    }

    public void updateGrayScale(Bundle bundle) {
        Message msg = this.mHandler.obtainMessage(1);
        msg.setData(bundle);
        this.mHandler.sendMessage(msg);
    }

    public void setGrayScale(float grayScale) {
        this.mGrayScale = grayScale;
        this.mMiuiBrightnessChangeTracker.updateGrayScale(grayScale);
        this.mDisplayPowerController.updateBrightness();
    }

    private void updateDolbyState(Bundle bundle) {
        Message msg = this.mHandler.obtainMessage(2);
        msg.setData(bundle);
        msg.sendToTarget();
    }

    /* loaded from: classes.dex */
    public final class DisplayPowerControllerImplHandler extends Handler {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public DisplayPowerControllerImplHandler(Looper looper) {
            super(looper, null, true);
            DisplayPowerControllerImpl.this = r2;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    DisplayPowerControllerImpl.this.setGrayScale(msg.getData().getFloat("gray_scale"));
                    return;
                case 2:
                    DisplayPowerControllerImpl.this.updateDolbyBrightnessIfNeeded(msg.getData().getBoolean("dolby_version_state", false));
                    return;
                case 3:
                    DisplayPowerControllerImpl.this.updateForegroundAppSync();
                    return;
                case 4:
                    DisplayPowerControllerImpl.this.updateForegroundApp();
                    return;
                default:
                    return;
            }
        }
    }

    public void updateAutoBrightnessComponent(BrightnessMappingStrategy brightnessMapper, AutomaticBrightnessControllerStub stub, DisplayDeviceConfig displayDeviceConfig, HighBrightnessModeController hbmController, LogicalDisplay logicalDisplay) {
        this.mBrightnessMapper = brightnessMapper;
        this.mDisplayDeviceConfig = displayDeviceConfig;
        AutomaticBrightnessControllerImpl automaticBrightnessControllerImpl = (AutomaticBrightnessControllerImpl) stub;
        this.mAutomaticBrightnessControllerImpl = automaticBrightnessControllerImpl;
        this.mHbmController = hbmController;
        this.mLogicalDisplay = logicalDisplay;
        if (automaticBrightnessControllerImpl != null) {
            automaticBrightnessControllerImpl.setUpBrightnessMapper(brightnessMapper);
            this.mAutomaticBrightnessControllerImpl.setUpDisplayDeviceConfig(this.mDisplayDeviceConfig);
            this.mAutomaticBrightnessControllerImpl.setUpLogicalDisplay(this.mLogicalDisplay);
            this.mAutomaticBrightnessControllerImpl.setUpCloudControllerListener(this.mCloudListener);
            MiuiBrightnessChangeTracker miuiBrightnessChangeTracker = this.mMiuiBrightnessChangeTracker;
            if (miuiBrightnessChangeTracker != null) {
                miuiBrightnessChangeTracker.setUpCloudControllerListener(this.mCloudListener);
            }
            this.mAutomaticBrightnessControllerImpl.setDisplayPowerControllerImpl(this);
            DisplayDeviceConfig.HighBrightnessModeData highBrightnessModeData = this.mDisplayDeviceConfig.getHighBrightnessModeData();
            if (highBrightnessModeData != null) {
                recalculationForBCBC(0.5f);
                this.mHbmController.registerListener(this.mHdrStateListener);
            }
        }
        initDisplayCloudControllerIfNeeded();
    }

    private void recalculationForBCBC(float coefficient) {
        if (this.mInitialBCBCParameters) {
            return;
        }
        this.mInitialBCBCParameters = true;
        int i = 0;
        while (true) {
            float[] fArr = this.mRealtimeArrayD;
            if (i < fArr.length) {
                fArr[i] = BigDecimal.valueOf(DATA_D[i] * coefficient).setScale(6, 4).floatValue();
                i++;
            } else {
                this.mRealtimeMaxDiff = BigDecimal.valueOf(MAX_DIFF * coefficient).setScale(6, 4).floatValue();
                computeBCBCAdjustmentParams();
                return;
            }
        }
    }

    public void updateAutoBrightnessMode() {
        boolean z = true;
        if (Settings.System.getIntForUser(this.mContext.getContentResolver(), "screen_brightness_mode", 0, -2) != 1) {
            z = false;
        }
        this.mAutoBrightnessEnable = z;
        updateBCBCStateIfNeeded();
        if (!this.mAutoBrightnessEnable) {
            updateBrightnessAnimInfoIfNeeded(false);
        }
    }

    private void loadSettings() {
        updateColorInversionEnabled();
        updateAutoBrightnessMode();
        updateCurtainAnimationEnabled();
    }

    /* renamed from: registerForegroundAppUpdater */
    public void m570x83a39a7f() {
        try {
            this.mActivityTaskManager.registerTaskStackListener(this.mTaskStackListener);
            updateForegroundApp();
        } catch (RemoteException e) {
        }
    }

    /* loaded from: classes.dex */
    public class TaskStackListenerImpl extends TaskStackListener {
        TaskStackListenerImpl() {
            DisplayPowerControllerImpl.this = this$0;
        }

        public void onTaskStackChanged() {
            DisplayPowerControllerImpl.this.mHandler.sendEmptyMessage(4);
        }
    }

    public void updateForegroundApp() {
        BackgroundThread.getHandler().post(new Runnable() { // from class: com.android.server.display.DisplayPowerControllerImpl.2
            @Override // java.lang.Runnable
            public void run() {
                try {
                    ActivityTaskManager.RootTaskInfo info = DisplayPowerControllerImpl.this.mActivityTaskManager.getFocusedRootTaskInfo();
                    if (info != null && info.topActivity != null && info.getWindowingMode() != 5 && info.getWindowingMode() != 3 && info.getWindowingMode() != 4) {
                        String packageName = info.topActivity.getPackageName();
                        if (packageName != null && packageName.equals(DisplayPowerControllerImpl.this.mForegroundAppPackageName)) {
                            return;
                        }
                        DisplayPowerControllerImpl.this.mPendingForegroundAppPackageName = packageName;
                        DisplayPowerControllerImpl.this.mHandler.sendEmptyMessage(3);
                    }
                } catch (RemoteException e) {
                }
            }
        });
    }

    public void updateForegroundAppSync() {
        this.mForegroundAppPackageName = this.mPendingForegroundAppPackageName;
        this.mPendingForegroundAppPackageName = null;
        if (this.mAutoBrightnessEnable) {
            updateBCBCStateIfNeeded();
            updateGameSceneEnable();
        }
    }

    public void updateBCBCStateIfNeeded() {
        MiuiDisplayCloudController miuiDisplayCloudController;
        if (BCBC_ENABLE && (miuiDisplayCloudController = this.mMiuiDisplayCloudController) != null) {
            int state = (!miuiDisplayCloudController.getBCBCAppList().contains(this.mForegroundAppPackageName) || !this.mAutoBrightnessEnable || this.mDolbyStateEnable || this.mHbmController.getHighBrightnessMode() == 2) ? 0 : 1;
            if (state != this.mBCBCState) {
                this.mBCBCState = state;
                ScreenEffectServiceStub.getInstance().updateBCBCState(state);
                if (DEBUG) {
                    Slog.d(TAG, (state == 1 ? "Enter " : "Exit ") + "BCBC State, mForegroundAppPackageName = " + this.mForegroundAppPackageName + ", mAutoBrightnessEnable = " + this.mAutoBrightnessEnable);
                }
            } else if (DEBUG) {
                Slog.d(TAG, "Skip BCBC State, mBCBCState = " + this.mBCBCState);
            }
        }
    }

    private boolean isKeyguardOn() {
        return this.mPolicy.isKeyguardShowing() || this.mPolicy.isKeyguardShowingAndNotOccluded();
    }

    public boolean shouldUseFastRate(boolean screenOnBecauseOfProximity, float brightness) {
        AutomaticBrightnessControllerImpl automaticBrightnessControllerImpl = this.mAutomaticBrightnessControllerImpl;
        if (automaticBrightnessControllerImpl != null) {
            return automaticBrightnessControllerImpl.shouldUseFastRate(screenOnBecauseOfProximity, brightness);
        }
        return false;
    }

    public float computeFloatRate(float rate, float currentValue, float targetValue) {
        AutomaticBrightnessControllerImpl automaticBrightnessControllerImpl = this.mAutomaticBrightnessControllerImpl;
        if (automaticBrightnessControllerImpl != null) {
            return automaticBrightnessControllerImpl.computeFloatRate(rate, currentValue, targetValue);
        }
        return Float.NaN;
    }

    public void updateCurtainAnimationEnabled() {
        boolean z = true;
        this.mCurtainAnimationEnabled = Settings.System.getIntForUser(this.mContext.getContentResolver(), KEY_CURTAIN_ANIM_ENABLED, 1, -2) == 1;
        if (Settings.Secure.getIntForUser(this.mContext.getContentResolver(), KEY_IS_DYNAMIC_LOCK_SCREEN_SHOW, 0, -2) != 1) {
            z = false;
        }
        this.mIsDynamicLockScreenShowing = z;
    }

    public void dump(PrintWriter pw) {
        if (this.mSunlightModeAvailable) {
            this.mSunlightController.dump(pw);
            pw.println("  mAppliedSunlightMode=" + this.mAppliedSunlightMode);
            pw.println("  mLastSettingsBrightnessBeforeApplySunlight=" + this.mLastSettingsBrightnessBeforeApplySunlight);
            pw.println("  mGrayScale=" + this.mGrayScale);
            pw.println("  mColorInversionEnabled=" + this.mColorInversionEnabled);
            pw.println("  BCBC_ENABLE=" + BCBC_ENABLE);
            pw.println("  mCurtainAnimationAvailable=" + this.mCurtainAnimationAvailable);
            pw.println("  mCurtainAnimationEnabled=" + this.mCurtainAnimationEnabled);
            pw.println("  mIsDynamicLockScreenShowing=" + this.mIsDynamicLockScreenShowing);
            pw.println("  mPendingShowCurtainAnimation=" + this.mPendingShowCurtainAnimation);
        }
        pw.println("  Gallery Hdr Boost:");
        pw.println("      mSupportGalleryHdr=" + mSupportGalleryHdr);
        pw.println("      mIsGalleryHdrEnable=" + this.mIsGalleryHdrEnable);
        pw.println("      mGalleryHdrThrottled=" + this.mGalleryHdrThrottled);
        pw.println("      mCurrentGalleryHdrBoostFactor=" + this.mCurrentGalleryHdrBoostFactor);
        ThermalObserver thermalObserver = this.mThermalObserver;
        if (thermalObserver != null) {
            thermalObserver.dump(pw);
        }
        DEBUG = DisplayDebugConfig.DEBUG_DPC;
        MiuiDisplayCloudController miuiDisplayCloudController = this.mMiuiDisplayCloudController;
        if (miuiDisplayCloudController != null) {
            miuiDisplayCloudController.dump(pw);
        }
    }

    public void updateDolbyBrightnessIfNeeded(boolean enable) {
        if (this.SUPPORT_DOLBY_VERSION_BRIGHTEN && this.mHbmController != null && this.mDolbyStateEnable != enable) {
            this.mDolbyStateEnable = enable;
            if (this.mAutoBrightnessEnable) {
                resetBCBCState();
            }
            this.mHbmController.setDolbyEnable(this.mDolbyStateEnable);
            this.mDisplayPowerController.updateBrightness();
        }
    }

    public void stop() {
        this.mContext.getContentResolver().unregisterContentObserver(this.mSettingsObserver);
    }

    public void showTouchCoverProtectionRect(boolean isShow) {
        AutomaticBrightnessControllerImpl automaticBrightnessControllerImpl = this.mAutomaticBrightnessControllerImpl;
        if (automaticBrightnessControllerImpl != null) {
            automaticBrightnessControllerImpl.showTouchCoverProtectionRect(isShow);
        }
    }

    public void setManualDimmingEnabled(boolean manualDimmingEnable, float startBrightness) {
        AutomaticBrightnessControllerImpl automaticBrightnessControllerImpl = this.mAutomaticBrightnessControllerImpl;
        if (automaticBrightnessControllerImpl != null) {
            automaticBrightnessControllerImpl.setManualDimmingEnabled(manualDimmingEnable, startBrightness);
        }
    }

    public boolean shouldDimmingWhenSlideBrightnessBar(boolean brightnessIsTemporary) {
        boolean z = this.SUPPORT_MANUAL_DIMMING && brightnessIsTemporary;
        this.mShouldDimming = z;
        return z;
    }

    public void notifyUpdateBrightnessAnimInfo(float currentBrightnessAnim, float brightnessAnim, float targetBrightnessAnim) {
        MiuiBrightnessChangeTracker miuiBrightnessChangeTracker = this.mMiuiBrightnessChangeTracker;
        if (miuiBrightnessChangeTracker != null && this.mUpdateBrightnessAnimInfoEnable && !this.mShouldDimming) {
            miuiBrightnessChangeTracker.notifyUpdateBrightnessAnimInfo(currentBrightnessAnim, brightnessAnim, targetBrightnessAnim);
        }
    }

    public void notifyUpdateTempBrightnessTimeStamp(boolean enable) {
        MiuiBrightnessChangeTracker miuiBrightnessChangeTracker = this.mMiuiBrightnessChangeTracker;
        if (miuiBrightnessChangeTracker != null) {
            miuiBrightnessChangeTracker.notifyUpdateTempBrightnessTimeStampIfNeeded(enable);
        }
    }

    public void updateBrightnessAnimInfoIfNeeded(boolean enable) {
        MiuiBrightnessChangeTracker miuiBrightnessChangeTracker;
        if (enable != this.mUpdateBrightnessAnimInfoEnable && (miuiBrightnessChangeTracker = this.mMiuiBrightnessChangeTracker) != null) {
            this.mUpdateBrightnessAnimInfoEnable = enable;
            if (!enable) {
                miuiBrightnessChangeTracker.notifyResetBrightnessAnimInfo();
            }
        }
    }

    private void updateGameSceneEnable() {
        this.mAutomaticBrightnessControllerImpl.updateGameSceneEnable(this.mMiuiDisplayCloudController.getTouchCoverProtectionGameList().contains(this.mForegroundAppPackageName));
    }

    public void sendDimStateToSurfaceFlinger(boolean isDim) {
        if (!SUPPORT_IDLE_DIM) {
            return;
        }
        if (DEBUG) {
            Slog.d(TAG, "sendDimStateToSurfaceFlinger is dim " + isDim);
        }
        if (this.mISurfaceFlinger != null) {
            Parcel data = Parcel.obtain();
            data.writeInterfaceToken("android.ui.ISurfaceComposer");
            data.writeInt(DISPLAY_DIM_STATE);
            data.writeInt(isDim ? 1 : 0);
            data.writeString(PACKAGE_DIM_SYSTEM);
            try {
                try {
                    this.mISurfaceFlinger.transact(TRANSACTION_NOTIFY_DIM, data, null, 1);
                } catch (RemoteException | SecurityException ex) {
                    Slog.e(TAG, "Failed to send brightness to SurfaceFlinger", ex);
                }
            } finally {
                data.recycle();
            }
        }
    }

    public float adjustBrightnessByTemperature(float brightness) {
        TemperatureController temperatureController;
        if (this.SUPPORT_TEMEPERATURE_CONTROL && (temperatureController = this.mTemperatureController) != null) {
            float temperatureMaxBrightness = temperatureController.getTemperatureControlBrightness();
            if (brightness > temperatureMaxBrightness) {
                return temperatureMaxBrightness;
            }
            return brightness;
        }
        return brightness;
    }
}
