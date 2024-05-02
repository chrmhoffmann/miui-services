package com.android.server.display;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.MathUtils;
import android.util.Slog;
import android.util.SparseArray;
import com.android.internal.display.BrightnessSynchronizer;
import com.android.server.display.AutomaticBrightnessControllerStub;
import com.android.server.display.DisplayDeviceConfig;
import com.android.server.wm.MiuiFreeformPinManagerService;
import com.miui.base.MiuiStubRegistry;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import miui.os.DeviceFeature;
import miui.util.FeatureParser;
/* loaded from: classes.dex */
public class AutomaticBrightnessControllerImpl extends AutomaticBrightnessControllerStub {
    private static final float ALLOW_FAST_RATE_RATIO;
    private static final int ALLOW_FAST_RATE_TIME;
    private static final float ALLOW_FAST_RATE_VALUE;
    private static final long ALLOW_NONUI_FAST_UPDATE_BRIGHTNESS_TIME = 2000;
    private static final float ASSISTSENSOR_BRIGHTENINGRATIO = 2.0f;
    private static final float ASSISTSENSOR_BRIGHTENING_MINTHRES = 5.0f;
    private static final float ASSISTSENSOR_DARKENINGRATIO = 0.2f;
    private static final float ASSISTSENSOR_DATA_THRESHOLD;
    private static final long ASSISTSENSOR_DEBOUNCETIME = 10000;
    private static final float ASSISTSENSOR_MAXTHRES;
    private static final int ASSIST_SENSOR_TYPE = 33171055;
    private static final int BRIGHTNESS_12BIT = 4095;
    private static final int BRIGHTNESS_IN_BRIGHTENING = 2;
    private static final int BRIGHTNESS_IN_DARKENING = 1;
    private static final int BRIGHTNESS_IN_STABLE = 0;
    public static final String CAMERA_ROLE_IDS = "com.xiaomi.cameraid.role.cameraIds";
    private static boolean DEBUG = false;
    private static final int DO_NOT_REDUCE_BRIGHTNESS_INTERVAL = 300000;
    private static final int DYNAMIC_DARKENING_DEBOUNCE1;
    private static final int DYNAMIC_DARKENING_DEBOUNCE2;
    private static final int DYNAMIC_DARKENING_DEFAULTDEBOUNCE;
    private static final float DYNAMIC_DARKENING_LUXTHRESHOLD;
    private static final boolean IS_UMI_0B_DISPLAY_PANEL;
    private static final int MANUAL_DIMMING_DEBOUNCE_TIME = 150;
    private static final int MANUAL_DIMMING_DURATION_MILLIS = 150;
    private static final float MIN_ADJUST_RATE;
    private static final float MOTIONSENSOR_STATIC_LUXTHRESHOLD;
    private static final int MOTIONSENSOR_TYPE = 33171039;
    private static final int MOTION_MOVE = 1;
    private static final int MOTION_STATIC = 2;
    private static final int MOTION_STATIC_ROTATE = 3;
    private static final int MSG_RESET_FAST_RATE = 2;
    private static final int MSG_UPDATE_OUT_PACKET_TIME = 1;
    private static final float NIT_LEVEL = 40.0f;
    private static final float NIT_LEVEL1 = 35.0f;
    private static final float NIT_LEVEL2 = 87.450005f;
    private static final float NIT_LEVEL3 = 265.0f;
    private static final int NONUI_SENSOR_TYPE = 33171027;
    private static final int NORMAL_BRIGHTNESS_ON;
    private static final float NOT_IN_POCKET = 0.0f;
    private static final String OLED_PANEL_ID;
    public static final int PARALLEL_VIRTUAL_ROLE_ID = 102;
    private static final int PROXIMITY_NEGATIVE = 0;
    private static final int PROXIMITY_POSITIVE = 1;
    private static final int PROXIMITY_UNKNOWN = -1;
    private static final int RATE_LEVEL = 40;
    private static final int RESET_FAST_RATE_TIME;
    private static final int SENSOR_TYPE_LIGHT_SECONDARY = 33171081;
    private static final boolean SMD_EFFECT_STATUS;
    private static final boolean SUPPORT_MANUAL_DIMMING;
    private static final String TAG = "AutomaticBrightnessControllerImpl";
    private static final float TIME_1 = 0.0f;
    private static final float TIME_2 = 0.8f;
    private static final float TIME_3 = 1.8f;
    private static final float TIME_4 = 4.0f;
    private static final float TIME_5 = 24.0f;
    private static final int TORCH_CLOSE_DELAY = 1800;
    private static final float TYPICAL_PROXIMITY_THRESHOLD = 5.0f;
    private static final boolean USE_ASSISTSENSOR_ENABLED;
    private static final boolean USE_DAEMON_SENSOR_POLICY;
    private static final boolean USE_DYNAMIC_DEBOUNCE;
    private static final boolean USE_FOLD_ENABLED;
    private static final boolean USE_MOTIONSENSOR_ENABLED;
    private static final boolean USE_NONUI_ENABLED;
    private static final boolean USE_PROXIMITY_ENABLED;
    public static final int VIRTUAL_BACK_ROLE_ID = 100;
    public static final int VIRTUAL_FRONT_ROLE_ID = 101;
    private static final float[] mALevels;
    private static final float[] mBLevels;
    private static final float[] mNitsLevels;
    private float mAmbientLux;
    private boolean mAppliedDimming;
    private boolean mApplyingFastRate;
    private Sensor mAssistSensor;
    private Handler mBrightnessControllerImplHandler;
    private BrightnessMappingStrategy mBrightnessMapper;
    private CameraManager mCameraManager;
    private CloudControllerListener mCloudControllerListener;
    private Context mContext;
    private DaemonSensorPolicy mDaemonSensorPolicy;
    private DisplayDeviceConfig mDisplayDeviceConfig;
    private DisplayPowerControllerImpl mDisplayPowerControllerImpl;
    private DualSensorPolicy mDualSensorPolicy;
    private long mEnterGameTime;
    private boolean mFirstUseFastRateDueToOutPocket;
    private Handler mHandler;
    private DisplayDeviceConfig.HighBrightnessModeData mHighBrightnessModeData;
    public boolean mIsAutoBrightnessByApplicationAnimationEnable;
    private boolean mIsAutoBrightnessByApplicationRateEnable;
    private boolean mIsGameSceneEnable;
    private float mLastBrightness;
    private long mLastTimeToSetManualDimmingEnabledMillis;
    private boolean mManualDimmingEnable;
    private Sensor mMotionSensor;
    private float mNeedUseFastRateBrightness;
    private Sensor mNonUiSensor;
    private boolean mPendingUseFastRateDueToFirstAutoBrightness;
    private boolean mPendingUseFastRateDueToOutPocket;
    private boolean mProximityPositive;
    private Sensor mProximitySensor;
    private float mProximityThreshold;
    private float mRealLux;
    private SceneDetector mSceneDetector;
    private SensorManager mSensorManager;
    private boolean mSuppressDarkeningEnable;
    private TouchCoverProtectionHelper mTouchAreaHelper;
    private int mProximity = -1;
    private boolean mProximitySensorEnabled = false;
    private boolean mSlowChange = false;
    private float mStartBrightness = -1.0f;
    private float mStartBrightnessForManualDimming = MiuiFreeformPinManagerService.EDGE_AREA;
    private boolean mAutomaticBrightnessEnable = false;
    private int mBrightnessStatus = 0;
    private boolean mMotionSensorEnabled = false;
    private int mMotionStatus = 1;
    private boolean mAssistSensorEnabled = false;
    private float mAssistSensorData = -1.0f;
    private float mAssistDarkeningThres = -1.0f;
    private float mAssistBrighteningThres = -1.0f;
    private long mAssistSensorTime = -1;
    private float mCurrentLux = -1.0f;
    private long mDynamicEnvStartTime = 0;
    private boolean mIsTorchOpen = false;
    private long mTorchCloseTime = 0;
    private float mBrighteningRatio = Resources.getSystem().getFloat(285671463);
    private Sensor mSecLightSensor = null;
    private final float SLOW_RATE_TIME = 10.0f;
    private final float FAST_RATE_TIME = 5.0f;
    private final float AUTOBRIGHTNESS_BY_APPLICATION_MIN_RATE = 0.01f;
    private float mAutoBrightnessByApplicationRate = MiuiFreeformPinManagerService.EDGE_AREA;
    private float mHbmTransitionPointNit = Float.NaN;
    private float mBrightnessMaxNit = Float.NaN;
    private boolean mNonUiSensorEnabled = false;
    private float mNonUiSensorData = MiuiFreeformPinManagerService.EDGE_AREA;
    private long mNotInPocketTime = -1;
    private final boolean SUPPORT_TEMEPERATURE_CONTROL = FeatureParser.getBoolean("support_temperature_control", false);
    private final CameraManager.TorchCallback mTorchCallback = new CameraManager.TorchCallback() { // from class: com.android.server.display.AutomaticBrightnessControllerImpl.1
        @Override // android.hardware.camera2.CameraManager.TorchCallback
        public void onTorchModeUnavailable(String cameraId) {
            AutomaticBrightnessControllerImpl.this.mIsTorchOpen = true;
        }

        @Override // android.hardware.camera2.CameraManager.TorchCallback
        public void onTorchModeChanged(String cameraId, boolean enabled) {
            try {
                CameraCharacteristics cameraCharacteristics = AutomaticBrightnessControllerImpl.this.mCameraManager.getCameraCharacteristics(cameraId);
                Integer[] roleIds = (Integer[]) cameraCharacteristics.get(new CameraCharacteristics.Key(AutomaticBrightnessControllerImpl.CAMERA_ROLE_IDS, Integer[].class));
                if (roleIds != null) {
                    List<Integer> list = Arrays.asList(roleIds);
                    if (!list.contains(100) && !list.contains(101)) {
                        if (list.contains(102)) {
                            return;
                        }
                    } else {
                        return;
                    }
                }
            } catch (CameraAccessException e) {
                Slog.e(AutomaticBrightnessControllerImpl.TAG, "onTorchModeChanged: can't get characteristics for camera " + cameraId, e);
            } catch (IllegalArgumentException e2) {
                Slog.e(AutomaticBrightnessControllerImpl.TAG, "onTorchModeChanged: invalid tag com.xiaomi.cameraid.role.cameraIds");
            }
            if (enabled) {
                AutomaticBrightnessControllerImpl.this.mIsTorchOpen = true;
            } else {
                if (AutomaticBrightnessControllerImpl.this.mIsTorchOpen) {
                    AutomaticBrightnessControllerImpl.this.mTorchCloseTime = System.currentTimeMillis();
                    AutomaticBrightnessControllerImpl.this.mAssistSensorData = -1.0f;
                }
                AutomaticBrightnessControllerImpl.this.mIsTorchOpen = false;
            }
            Slog.i(AutomaticBrightnessControllerImpl.TAG, "onTorchModeChanged, mIsTorchOpen=" + AutomaticBrightnessControllerImpl.this.mIsTorchOpen);
        }
    };
    private SensorEventListener mSensorListener = new SensorEventListener() { // from class: com.android.server.display.AutomaticBrightnessControllerImpl.2
        @Override // android.hardware.SensorEventListener
        public void onSensorChanged(SensorEvent event) {
            switch (event.sensor.getType()) {
                case 8:
                    AutomaticBrightnessControllerImpl.this.onProximitySensorChanged(event);
                    return;
                case AutomaticBrightnessControllerImpl.NONUI_SENSOR_TYPE /* 33171027 */:
                    AutomaticBrightnessControllerImpl.this.onNonUiSensorChanged(event);
                    return;
                case AutomaticBrightnessControllerImpl.MOTIONSENSOR_TYPE /* 33171039 */:
                    AutomaticBrightnessControllerImpl.this.onMotionSensorChanged(event);
                    return;
                default:
                    return;
            }
        }

        @Override // android.hardware.SensorEventListener
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    /* loaded from: classes.dex */
    public interface CloudControllerListener {
        boolean isAutoBrightnessStatisticsEventEnable();

        boolean isPointLightSourceDetectorEnable();
    }

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<AutomaticBrightnessControllerImpl> {

        /* compiled from: AutomaticBrightnessControllerImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final AutomaticBrightnessControllerImpl INSTANCE = new AutomaticBrightnessControllerImpl();
        }

        public AutomaticBrightnessControllerImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public AutomaticBrightnessControllerImpl provideNewInstance() {
            return new AutomaticBrightnessControllerImpl();
        }
    }

    static {
        boolean z = false;
        USE_PROXIMITY_ENABLED = Resources.getSystem().getBoolean(285540373) && !DeviceFeature.hasSupportAudioPromity();
        ALLOW_FAST_RATE_TIME = Resources.getSystem().getInteger(285933576);
        RESET_FAST_RATE_TIME = Resources.getSystem().getInteger(285933620);
        ALLOW_FAST_RATE_RATIO = Resources.getSystem().getFloat(285671455);
        ALLOW_FAST_RATE_VALUE = Resources.getSystem().getFloat(285671456);
        SUPPORT_MANUAL_DIMMING = FeatureParser.getBoolean("support_manual_dimming", false);
        USE_NONUI_ENABLED = Resources.getSystem().getBoolean(285540372);
        String str = SystemProperties.get("ro.boot.oled_panel_id", "");
        OLED_PANEL_ID = str;
        if ("0B".equals(str) && ("umi".equals(Build.DEVICE) || "umiin".equals(Build.DEVICE))) {
            z = true;
        }
        IS_UMI_0B_DISPLAY_PANEL = z;
        ASSISTSENSOR_MAXTHRES = Resources.getSystem().getInteger(285933577);
        USE_DYNAMIC_DEBOUNCE = Resources.getSystem().getBoolean(285540369);
        USE_MOTIONSENSOR_ENABLED = Resources.getSystem().getBoolean(285540371);
        USE_ASSISTSENSOR_ENABLED = Resources.getSystem().getBoolean(285540368);
        DYNAMIC_DARKENING_DEBOUNCE1 = Resources.getSystem().getInteger(285933598);
        DYNAMIC_DARKENING_DEBOUNCE2 = Resources.getSystem().getInteger(285933599);
        SMD_EFFECT_STATUS = Resources.getSystem().getBoolean(285540441);
        DYNAMIC_DARKENING_DEFAULTDEBOUNCE = Resources.getSystem().getInteger(285933596);
        ASSISTSENSOR_DATA_THRESHOLD = Resources.getSystem().getInteger(285933578);
        MOTIONSENSOR_STATIC_LUXTHRESHOLD = Resources.getSystem().getInteger(285933611);
        DYNAMIC_DARKENING_LUXTHRESHOLD = Resources.getSystem().getInteger(285933597);
        mNitsLevels = new float[]{800.0f, 251.0f, 150.0f, 100.0f, 70.0f, 50.0f, NIT_LEVEL, 30.0f, 28.5f};
        mALevels = new float[]{800.0f, 569.48f, 344.89f, 237.75f, 179.71f, 135.19f, 113.59f, 62.84f, 676.87f};
        mBLevels = new float[]{0.9887f, 0.992f, 0.995f, 0.9965f, 0.9973f, 0.9979f, 0.9982f, 0.999f, 0.996f};
        int i = (1 << DeviceFeature.BACKLIGHT_BIT) - 1;
        NORMAL_BRIGHTNESS_ON = i;
        MIN_ADJUST_RATE = 1.0f / i;
        USE_FOLD_ENABLED = Resources.getSystem().getBoolean(285540370);
        USE_DAEMON_SENSOR_POLICY = FeatureParser.getBoolean("use_daemon_sensor_policy", true);
    }

    /* JADX WARN: Removed duplicated region for block: B:29:0x007a  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private boolean needToUpdateAssistSensorData() {
        /*
            r12 = this;
            boolean r0 = com.android.server.display.AutomaticBrightnessControllerImpl.DEBUG
            java.lang.String r1 = ", mAssistSensorTime="
            java.lang.String r2 = "USE_ASSISTSENSOR_ENABLED="
            java.lang.String r3 = "AutomaticBrightnessControllerImpl"
            if (r0 == 0) goto L2a
            java.lang.StringBuilder r0 = new java.lang.StringBuilder
            r0.<init>()
            java.lang.StringBuilder r0 = r0.append(r2)
            boolean r4 = com.android.server.display.AutomaticBrightnessControllerImpl.USE_ASSISTSENSOR_ENABLED
            java.lang.StringBuilder r0 = r0.append(r4)
            java.lang.StringBuilder r0 = r0.append(r1)
            long r4 = r12.mAssistSensorTime
            java.lang.StringBuilder r0 = r0.append(r4)
            java.lang.String r0 = r0.toString()
            android.util.Slog.i(r3, r0)
        L2a:
            boolean r0 = com.android.server.display.AutomaticBrightnessControllerImpl.USE_ASSISTSENSOR_ENABLED
            if (r0 == 0) goto Ldb
            long r4 = r12.mAssistSensorTime
            r6 = -1
            int r4 = (r4 > r6 ? 1 : (r4 == r6 ? 0 : -1))
            if (r4 != 0) goto L38
            goto Ldb
        L38:
            long r4 = android.os.SystemClock.uptimeMillis()
            long r6 = r12.mAssistSensorTime
            long r6 = r4 - r6
            r8 = 0
            float r9 = r12.mAssistBrighteningThres
            r10 = -1082130432(0xffffffffbf800000, float:-1.0)
            int r11 = (r9 > r10 ? 1 : (r9 == r10 ? 0 : -1))
            if (r11 == 0) goto L75
            float r11 = r12.mAssistDarkeningThres
            int r10 = (r11 > r10 ? 1 : (r11 == r10 ? 0 : -1))
            if (r10 == 0) goto L75
            float r10 = r12.mAssistSensorData
            int r9 = (r10 > r9 ? 1 : (r10 == r9 ? 0 : -1))
            if (r9 > 0) goto L59
            int r9 = (r10 > r11 ? 1 : (r10 == r11 ? 0 : -1))
            if (r9 >= 0) goto L75
        L59:
            boolean r9 = r12.checkAssistSensorValid()
            if (r9 == 0) goto L75
            float r9 = r12.mAssistSensorData
            float r10 = com.android.server.display.AutomaticBrightnessControllerImpl.ASSISTSENSOR_MAXTHRES
            int r9 = (r9 > r10 ? 1 : (r9 == r10 ? 0 : -1))
            if (r9 >= 0) goto L75
            r9 = 10000(0x2710, double:4.9407E-320)
            int r9 = (r6 > r9 ? 1 : (r6 == r9 ? 0 : -1))
            if (r9 <= 0) goto L75
            boolean r9 = r12.isMotionStaticRotate()
            if (r9 != 0) goto L75
            r8 = 1
            goto L76
        L75:
            r8 = 0
        L76:
            boolean r9 = com.android.server.display.AutomaticBrightnessControllerImpl.DEBUG
            if (r9 == 0) goto Lda
            java.lang.StringBuilder r9 = new java.lang.StringBuilder
            r9.<init>()
            java.lang.StringBuilder r2 = r9.append(r2)
            java.lang.StringBuilder r0 = r2.append(r0)
            java.lang.StringBuilder r0 = r0.append(r1)
            long r1 = r12.mAssistSensorTime
            java.lang.StringBuilder r0 = r0.append(r1)
            java.lang.String r1 = ", currTime="
            java.lang.StringBuilder r0 = r0.append(r1)
            java.lang.StringBuilder r0 = r0.append(r4)
            java.lang.String r1 = ", deltaTime="
            java.lang.StringBuilder r0 = r0.append(r1)
            java.lang.StringBuilder r0 = r0.append(r6)
            java.lang.String r1 = ", mAssistSensorData="
            java.lang.StringBuilder r0 = r0.append(r1)
            float r1 = r12.mAssistSensorData
            java.lang.StringBuilder r0 = r0.append(r1)
            java.lang.String r1 = ", mAssistBrighteningThres="
            java.lang.StringBuilder r0 = r0.append(r1)
            float r1 = r12.mAssistBrighteningThres
            java.lang.StringBuilder r0 = r0.append(r1)
            java.lang.String r1 = ", mAssistDarkeningThres="
            java.lang.StringBuilder r0 = r0.append(r1)
            float r1 = r12.mAssistDarkeningThres
            java.lang.StringBuilder r0 = r0.append(r1)
            java.lang.String r1 = ", ret="
            java.lang.StringBuilder r0 = r0.append(r1)
            java.lang.StringBuilder r0 = r0.append(r8)
            java.lang.String r0 = r0.toString()
            android.util.Slog.i(r3, r0)
        Lda:
            return r8
        Ldb:
            r0 = 0
            return r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.display.AutomaticBrightnessControllerImpl.needToUpdateAssistSensorData():boolean");
    }

    public Sensor getSecLightSensor() {
        return this.mSecLightSensor;
    }

    public void initialize(SensorManager sensorManager, Context context, Handler handler, Sensor lightSensor, int lightSensorWarmUpTime, int lightSensorRate, long brighteningLightDebounceConfig, long darkeningLightDebounceConfig, int ambientLightHorizonLong, int ambientLightHorizonShort, HysteresisLevels ambientBrightnessThresholds, AutomaticBrightnessControllerStub.DualSensorPolicyListener listener) {
        this.mContext = context;
        this.mHandler = handler;
        this.mBrightnessControllerImplHandler = new AutomaticBrightnessControllerImplHandler(this.mHandler.getLooper());
        this.mSensorManager = sensorManager;
        this.mDaemonSensorPolicy = new DaemonSensorPolicy(this.mContext, sensorManager, handler.getLooper(), this, lightSensor);
        this.mTouchAreaHelper = new TouchCoverProtectionHelper(this.mContext, handler.getLooper());
        this.mSceneDetector = new SceneDetector(handler.getLooper(), sensorManager, this, this.mContext);
        this.mDualSensorPolicy = new DualSensorPolicy(handler.getLooper(), sensorManager, lightSensorWarmUpTime, lightSensorRate, brighteningLightDebounceConfig, darkeningLightDebounceConfig, ambientLightHorizonLong, ambientLightHorizonShort, ambientBrightnessThresholds, listener, this);
        if (USE_FOLD_ENABLED) {
            this.mSecLightSensor = this.mSensorManager.getDefaultSensor(SENSOR_TYPE_LIGHT_SECONDARY);
        }
        if (USE_PROXIMITY_ENABLED) {
            Sensor defaultSensor = this.mSensorManager.getDefaultSensor(8);
            this.mProximitySensor = defaultSensor;
            if (defaultSensor != null) {
                this.mProximityThreshold = Math.min(defaultSensor.getMaximumRange(), 5.0f);
            }
        }
        if (USE_MOTIONSENSOR_ENABLED) {
            this.mMotionSensor = this.mSensorManager.getDefaultSensor(MOTIONSENSOR_TYPE);
        }
        if (USE_NONUI_ENABLED) {
            Sensor defaultSensor2 = this.mSensorManager.getDefaultSensor(NONUI_SENSOR_TYPE);
            this.mNonUiSensor = defaultSensor2;
            if (defaultSensor2 == null) {
                this.mNonUiSensor = this.mSensorManager.getDefaultSensor(NONUI_SENSOR_TYPE, true);
            }
        }
        CameraManager cameraManager = (CameraManager) this.mContext.getSystemService("camera");
        this.mCameraManager = cameraManager;
        Handler handler2 = this.mHandler;
        if (handler2 != null) {
            cameraManager.registerTorchCallback(this.mTorchCallback, handler2);
        }
    }

    public void setDisplayPowerControllerImpl(DisplayPowerControllerImpl impl) {
        this.mDisplayPowerControllerImpl = impl;
    }

    public float configure(int state, float screenAutoBrightness, int displayPolicy) {
        boolean enable = state == 1 && displayPolicy != 1;
        setSensorEnabled(enable);
        if (USE_DAEMON_SENSOR_POLICY) {
            this.mDaemonSensorPolicy.notifyRegisterDaemonLightSensor(state, displayPolicy);
        }
        this.mTouchAreaHelper.configure(enable);
        if (!enable && this.mAutomaticBrightnessEnable) {
            this.mAutomaticBrightnessEnable = false;
            this.mCurrentLux = -1.0f;
            this.mBrightnessStatus = 0;
            this.mDynamicEnvStartTime = 0L;
            this.mApplyingFastRate = false;
            this.mBrightnessControllerImplHandler.removeMessages(2);
        } else if (enable && !this.mAutomaticBrightnessEnable) {
            this.mAutomaticBrightnessEnable = true;
            this.mPendingUseFastRateDueToFirstAutoBrightness = true;
        }
        return screenAutoBrightness;
    }

    private void setSensorEnabled(boolean enable) {
        if (USE_PROXIMITY_ENABLED) {
            setProximitySensorEnabled(enable);
        }
        if (USE_MOTIONSENSOR_ENABLED) {
            setMotionSensorEnabled(enable);
        }
        if (USE_ASSISTSENSOR_ENABLED) {
            this.mDualSensorPolicy.setSensorEnabled(enable);
        }
        if (USE_NONUI_ENABLED) {
            setNonUiSensorEnabled(enable);
        }
        if (isPointLightSourceDetectorEnable() && supportDualSensorPolicy()) {
            this.mSceneDetector.setSensorEnable(enable);
        }
    }

    private void setProximitySensorEnabled(boolean enable) {
        if (enable && !this.mProximitySensorEnabled) {
            Slog.i(TAG, "setProximitySensorEnabled enable");
            this.mProximitySensorEnabled = true;
            this.mSensorManager.registerListener(this.mSensorListener, this.mProximitySensor, 3);
        } else if (!enable && this.mProximitySensorEnabled) {
            Slog.i(TAG, "setProximitySensorEnabled disable");
            this.mProximitySensorEnabled = false;
            this.mProximity = -1;
            this.mSensorManager.unregisterListener(this.mSensorListener, this.mProximitySensor);
        }
    }

    private void setMotionSensorEnabled(boolean enable) {
        if (enable && !this.mMotionSensorEnabled) {
            Slog.i(TAG, "setMotionSensorEnabled enable");
            this.mMotionSensorEnabled = true;
            this.mSensorManager.registerListener(this.mSensorListener, this.mMotionSensor, 3);
        } else if (!enable && this.mMotionSensorEnabled) {
            Slog.i(TAG, "setMotionSensorEnabled disable");
            this.mMotionSensorEnabled = false;
            this.mMotionStatus = 1;
            this.mSensorManager.unregisterListener(this.mSensorListener, this.mMotionSensor);
        }
    }

    private void setNonUiSensorEnabled(boolean enable) {
        Sensor sensor;
        if (enable && !this.mNonUiSensorEnabled && (sensor = this.mNonUiSensor) != null) {
            this.mNonUiSensorEnabled = true;
            this.mSensorManager.registerListener(this.mSensorListener, sensor, 3);
            Slog.i(TAG, "setNonuiSensorEnabled enable");
        } else if (!enable && this.mNonUiSensorEnabled) {
            this.mNonUiSensorEnabled = false;
            this.mNotInPocketTime = -1L;
            this.mNonUiSensorData = MiuiFreeformPinManagerService.EDGE_AREA;
            this.mBrightnessControllerImplHandler.removeMessages(1);
            this.mSensorManager.unregisterListener(this.mSensorListener, this.mNonUiSensor);
            Slog.i(TAG, "setNonuiSensorEnabled disable");
        }
    }

    public void onProximitySensorChanged(SensorEvent event) {
        if (this.mProximitySensorEnabled) {
            boolean z = false;
            float distance = event.values[0];
            if (distance >= MiuiFreeformPinManagerService.EDGE_AREA && distance < this.mProximityThreshold) {
                z = true;
            }
            this.mProximityPositive = z;
        }
    }

    public void onMotionSensorChanged(SensorEvent event) {
        if (this.mMotionSensorEnabled) {
            this.mMotionStatus = (int) event.values[0];
            if (DEBUG) {
                Slog.i(TAG, "Auto-brightness motion status: " + this.mMotionStatus);
            }
        }
    }

    public void onNonUiSensorChanged(SensorEvent event) {
        if (this.mNonUiSensorEnabled && event.values[0] != this.mNonUiSensorData) {
            this.mNonUiSensorData = event.values[0];
            if (event.values[0] == MiuiFreeformPinManagerService.EDGE_AREA) {
                this.mBrightnessControllerImplHandler.sendEmptyMessage(1);
            }
        }
    }

    public boolean dropAmbientLuxIfNeeded() {
        if (this.mTouchAreaHelper.isTouchCoverProtectionActive()) {
            Slog.d(TAG, "drop the ambient lux due to touch events.");
            return true;
        } else if (USE_PROXIMITY_ENABLED && this.mProximityPositive) {
            Slog.d(TAG, "drop the ambient lux due to proximity events.");
            return true;
        } else {
            return false;
        }
    }

    public boolean dropDecreaseLuxIfNeeded() {
        long now = SystemClock.uptimeMillis();
        if (this.mIsGameSceneEnable) {
            if (now - this.mEnterGameTime <= 300000 || this.mTouchAreaHelper.isGameSceneWithinTouchTime()) {
                Slog.d(TAG, "drop the ambient lux due to game scene enable.");
                return true;
            }
            return false;
        }
        return false;
    }

    public void updateGameSceneEnable(boolean enable) {
        this.mIsGameSceneEnable = enable;
        this.mEnterGameTime = enable ? SystemClock.uptimeMillis() : this.mEnterGameTime;
    }

    float getCurrentRealLux() {
        return this.mRealLux;
    }

    float getCurrentLux(float ambientlux) {
        if (DEBUG) {
            Slog.i(TAG, "mRealLux = " + this.mRealLux + ", ambientlux = " + ambientlux);
        }
        float f = this.mRealLux;
        if (f >= TIME_2 * ambientlux && f <= 1.2f * ambientlux) {
            return f;
        }
        return ambientlux;
    }

    public boolean checkAssistSensorValid() {
        if (USE_ASSISTSENSOR_ENABLED && !this.mIsTorchOpen && System.currentTimeMillis() - this.mTorchCloseTime > 1800) {
            return true;
        }
        if (DEBUG) {
            Slog.d(TAG, "drop assist light data due to within one second of turning off the torch.");
            return false;
        }
        return false;
    }

    /* JADX WARN: Code restructure failed: missing block: B:5:0x000b, code lost:
        if (r1 > r6) goto L7;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public float getCurrentLux(float r6, float r7, float r8) {
        /*
            r5 = this;
            r0 = r6
            boolean r1 = r5.checkAssistSensorValid()
            if (r1 == 0) goto Le
            float r1 = r5.mAssistSensorData
            int r2 = (r1 > r6 ? 1 : (r1 == r6 ? 0 : -1))
            if (r2 <= 0) goto Le
            goto Lf
        Le:
            r1 = r6
        Lf:
            r0 = r1
            float r1 = r5.mAssistSensorData
            int r2 = (r0 > r1 ? 1 : (r0 == r1 ? 0 : -1))
            if (r2 != 0) goto L33
            r2 = 1045220557(0x3e4ccccd, float:0.2)
            float r2 = r2 * r1
            r5.mAssistDarkeningThres = r2
            r2 = 1073741824(0x40000000, float:2.0)
            float r2 = r2 * r1
            r5.mAssistBrighteningThres = r2
            r3 = 1084227584(0x40a00000, float:5.0)
            float r4 = r1 + r3
            int r2 = (r2 > r4 ? 1 : (r2 == r4 ? 0 : -1))
            if (r2 >= 0) goto L2c
            float r1 = r1 + r3
            r5.mAssistBrighteningThres = r1
        L2c:
            long r1 = android.os.SystemClock.uptimeMillis()
            r5.mAssistSensorTime = r1
            goto L37
        L33:
            r1 = -1
            r5.mAssistSensorTime = r1
        L37:
            r5.mCurrentLux = r0
            int r1 = (r6 > r0 ? 1 : (r6 == r0 ? 0 : -1))
            if (r1 != 0) goto L41
            boolean r1 = com.android.server.display.AutomaticBrightnessControllerImpl.DEBUG
            if (r1 == 0) goto Lb3
        L41:
            java.lang.StringBuilder r1 = new java.lang.StringBuilder
            r1.<init>()
            java.lang.String r2 = "maxLux="
            java.lang.StringBuilder r1 = r1.append(r2)
            java.lang.StringBuilder r1 = r1.append(r7)
            java.lang.String r2 = ", minLux="
            java.lang.StringBuilder r1 = r1.append(r2)
            java.lang.StringBuilder r1 = r1.append(r8)
            java.lang.String r2 = ", lux="
            java.lang.StringBuilder r1 = r1.append(r2)
            java.lang.StringBuilder r1 = r1.append(r6)
            java.lang.String r2 = ", mAssistSensorData="
            java.lang.StringBuilder r1 = r1.append(r2)
            float r2 = r5.mAssistSensorData
            java.lang.StringBuilder r1 = r1.append(r2)
            java.lang.String r2 = ", mAssistDarkeningThres="
            java.lang.StringBuilder r1 = r1.append(r2)
            float r2 = r5.mAssistDarkeningThres
            java.lang.StringBuilder r1 = r1.append(r2)
            java.lang.String r2 = ", mAssistBrighteningThres="
            java.lang.StringBuilder r1 = r1.append(r2)
            float r2 = r5.mAssistBrighteningThres
            java.lang.StringBuilder r1 = r1.append(r2)
            java.lang.String r2 = ", mAssistSensorTime="
            java.lang.StringBuilder r1 = r1.append(r2)
            long r2 = r5.mAssistSensorTime
            java.lang.StringBuilder r1 = r1.append(r2)
            java.lang.String r2 = ", currLux="
            java.lang.StringBuilder r1 = r1.append(r2)
            java.lang.StringBuilder r1 = r1.append(r0)
            java.lang.String r2 = ", mBrightnessStatus="
            java.lang.StringBuilder r1 = r1.append(r2)
            int r2 = r5.mBrightnessStatus
            java.lang.StringBuilder r1 = r1.append(r2)
            java.lang.String r1 = r1.toString()
            java.lang.String r2 = "AutomaticBrightnessControllerImpl"
            android.util.Slog.i(r2, r1)
        Lb3:
            return r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.display.AutomaticBrightnessControllerImpl.getCurrentLux(float, float, float):float");
    }

    public void checkBrightening(float lux, float brightenThreshold, float darkThreshold) {
        if (lux > brightenThreshold) {
            this.mBrightnessStatus = 2;
            this.mDynamicEnvStartTime = SystemClock.uptimeMillis();
        } else if (lux < darkThreshold) {
            this.mBrightnessStatus = 1;
            this.mDynamicEnvStartTime = SystemClock.uptimeMillis();
        } else {
            this.mBrightnessStatus = 0;
        }
    }

    boolean checkDynamicDebounce() {
        long currTime = SystemClock.uptimeMillis();
        long deltaTime = currTime - this.mDynamicEnvStartTime;
        if (DEBUG) {
            Slog.i(TAG, "mDynamicEnvStartTime=" + this.mDynamicEnvStartTime + ", deltaTime=" + deltaTime + ", currTime=" + currTime);
        }
        if (this.mDynamicEnvStartTime == 0 || deltaTime > ALLOW_NONUI_FAST_UPDATE_BRIGHTNESS_TIME) {
            return true;
        }
        return false;
    }

    int checkMotionStatus() {
        if (USE_MOTIONSENSOR_ENABLED) {
            int i = this.mMotionStatus;
            if (i == 2 || i == 3) {
                return i;
            }
            return 0;
        }
        return 0;
    }

    public boolean isMotionStaticRotate() {
        if (SMD_EFFECT_STATUS && checkMotionStatus() == 3) {
            return true;
        }
        return false;
    }

    /* JADX WARN: Removed duplicated region for block: B:32:0x005d  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public long updateDarkeningDebounce(float r5, float r6, float r7) {
        /*
            r4 = this;
            int r0 = r4.checkMotionStatus()
            r1 = -1082130432(0xffffffffbf800000, float:-1.0)
            r2 = 3
            if (r0 != r2) goto L1f
            float r0 = r4.mCurrentLux
            float r2 = com.android.server.display.AutomaticBrightnessControllerImpl.MOTIONSENSOR_STATIC_LUXTHRESHOLD
            int r2 = (r0 > r2 ? 1 : (r0 == r2 ? 0 : -1))
            if (r2 >= 0) goto L1f
            int r0 = (r0 > r1 ? 1 : (r0 == r1 ? 0 : -1))
            if (r0 == 0) goto L1f
            boolean r0 = r4.checkDynamicDebounce()
            if (r0 == 0) goto L1f
            int r0 = com.android.server.display.AutomaticBrightnessControllerImpl.DYNAMIC_DARKENING_DEBOUNCE1
            long r0 = (long) r0
            goto L59
        L1f:
            int r0 = r4.checkMotionStatus()
            r2 = 2
            if (r0 != r2) goto L3c
            float r0 = r4.mCurrentLux
            float r2 = com.android.server.display.AutomaticBrightnessControllerImpl.MOTIONSENSOR_STATIC_LUXTHRESHOLD
            int r2 = (r0 > r2 ? 1 : (r0 == r2 ? 0 : -1))
            if (r2 >= 0) goto L3c
            int r0 = (r0 > r1 ? 1 : (r0 == r1 ? 0 : -1))
            if (r0 == 0) goto L3c
            boolean r0 = r4.checkDynamicDebounce()
            if (r0 == 0) goto L3c
            int r0 = com.android.server.display.AutomaticBrightnessControllerImpl.DYNAMIC_DARKENING_DEBOUNCE2
            long r0 = (long) r0
            goto L59
        L3c:
            boolean r0 = com.android.server.display.AutomaticBrightnessControllerImpl.USE_DYNAMIC_DEBOUNCE
            if (r0 == 0) goto L56
            float r0 = r4.mCurrentLux
            float r2 = com.android.server.display.AutomaticBrightnessControllerImpl.DYNAMIC_DARKENING_LUXTHRESHOLD
            int r2 = (r0 > r2 ? 1 : (r0 == r2 ? 0 : -1))
            if (r2 >= 0) goto L56
            int r0 = (r0 > r1 ? 1 : (r0 == r1 ? 0 : -1))
            if (r0 == 0) goto L56
            boolean r0 = r4.checkDynamicDebounce()
            if (r0 == 0) goto L56
            int r0 = com.android.server.display.AutomaticBrightnessControllerImpl.DYNAMIC_DARKENING_DEBOUNCE2
            long r0 = (long) r0
            goto L59
        L56:
            int r0 = com.android.server.display.AutomaticBrightnessControllerImpl.DYNAMIC_DARKENING_DEFAULTDEBOUNCE
            long r0 = (long) r0
        L59:
            boolean r2 = com.android.server.display.AutomaticBrightnessControllerImpl.DEBUG
            if (r2 == 0) goto L8d
            java.lang.StringBuilder r2 = new java.lang.StringBuilder
            r2.<init>()
            java.lang.String r3 = "DarkeningLightDebounce="
            java.lang.StringBuilder r2 = r2.append(r3)
            java.lang.StringBuilder r2 = r2.append(r0)
            java.lang.String r3 = ", mMotionStatus = "
            java.lang.StringBuilder r2 = r2.append(r3)
            int r3 = r4.mMotionStatus
            java.lang.StringBuilder r2 = r2.append(r3)
            java.lang.String r3 = ", mCurrentLux = "
            java.lang.StringBuilder r2 = r2.append(r3)
            float r3 = r4.mCurrentLux
            java.lang.StringBuilder r2 = r2.append(r3)
            java.lang.String r2 = r2.toString()
            java.lang.String r3 = "AutomaticBrightnessControllerImpl"
            android.util.Slog.i(r3, r2)
        L8d:
            return r0
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.display.AutomaticBrightnessControllerImpl.updateDarkeningDebounce(float, float, float):long");
    }

    public boolean shouldUseFastRate(boolean screenOnBecauseOfProximity, float brightness) {
        if (this.mPendingUseFastRateDueToFirstAutoBrightness) {
            this.mPendingUseFastRateDueToFirstAutoBrightness = false;
            this.mApplyingFastRate = true;
            this.mNeedUseFastRateBrightness = brightness;
            this.mLastBrightness = brightness;
            this.mBrightnessControllerImplHandler.removeMessages(2);
            this.mBrightnessControllerImplHandler.sendEmptyMessageDelayed(2, RESET_FAST_RATE_TIME);
            Slog.d(TAG, "Use fast rate due to first auto-brightness");
            return true;
        } else if (!this.mPendingUseFastRateDueToOutPocket || screenOnBecauseOfProximity) {
            return false;
        } else {
            this.mPendingUseFastRateDueToOutPocket = false;
            this.mApplyingFastRate = true;
            this.mBrightnessControllerImplHandler.removeMessages(2);
            this.mBrightnessControllerImplHandler.sendEmptyMessageDelayed(2, RESET_FAST_RATE_TIME);
            Slog.d(TAG, "Use fast rate due to out of pocket");
            return true;
        }
    }

    protected void notifyAutoBrightnessChanged(float screenBrightness, long sensorEnableTime) {
        this.mApplyingFastRate = false;
        float nit = convertToNit(screenBrightness);
        float lastNit = convertToNit(this.mLastBrightness);
        if (!SUPPORT_MANUAL_DIMMING && SystemClock.uptimeMillis() <= ALLOW_FAST_RATE_TIME + sensorEnableTime && nit >= (ALLOW_FAST_RATE_RATIO * lastNit) + lastNit && nit >= ALLOW_FAST_RATE_VALUE + lastNit) {
            this.mPendingUseFastRateDueToFirstAutoBrightness = true;
        } else if (USE_NONUI_ENABLED && this.mFirstUseFastRateDueToOutPocket && SystemClock.uptimeMillis() - this.mNotInPocketTime <= ALLOW_NONUI_FAST_UPDATE_BRIGHTNESS_TIME) {
            this.mPendingUseFastRateDueToOutPocket = true;
            this.mFirstUseFastRateDueToOutPocket = false;
            this.mNeedUseFastRateBrightness = screenBrightness;
        }
        this.mLastBrightness = screenBrightness;
    }

    protected void resetApplyingFastRate(float tgtBrightness) {
        if (this.mApplyingFastRate) {
            float f = this.mNeedUseFastRateBrightness;
            if (f != MiuiFreeformPinManagerService.EDGE_AREA && f != tgtBrightness) {
                this.mApplyingFastRate = false;
                this.mNeedUseFastRateBrightness = MiuiFreeformPinManagerService.EDGE_AREA;
                Slog.i(TAG, "Reset apply fast rate.");
            }
        }
    }

    public void updateSlowChangeStatus(boolean slowChange, boolean appliedDimming, boolean appliedLowPower, float startBrightness) {
        this.mSlowChange = (!appliedDimming) & slowChange & (!appliedLowPower);
        this.mAppliedDimming = appliedDimming;
        this.mStartBrightness = startBrightness;
        if (DEBUG) {
            Slog.d(TAG, "updateSlowChangeStatus: " + this.mSlowChange + ", appliedDimming: " + this.mAppliedDimming + ", appliedLowPower: " + appliedLowPower + ", startBrightness: " + this.mStartBrightness);
        }
    }

    float convertToNit(float brightness) {
        BrightnessMappingStrategy brightnessMappingStrategy = this.mBrightnessMapper;
        if (brightnessMappingStrategy != null) {
            return brightnessMappingStrategy.convertToNits(brightness);
        }
        return Float.NaN;
    }

    float convertToBrightness(float nit) {
        BrightnessMappingStrategy brightnessMappingStrategy = this.mBrightnessMapper;
        if (brightnessMappingStrategy != null) {
            return brightnessMappingStrategy.convertToBrightness(nit);
        }
        return Float.NaN;
    }

    int getIndex(float nit) {
        int index = 1;
        while (true) {
            float[] fArr = mNitsLevels;
            if (fArr.length <= index || nit >= fArr[index]) {
                break;
            }
            index++;
        }
        if (DEBUG) {
            Slog.d(TAG, "nit: " + nit + ", index: " + index);
        }
        return index - 1;
    }

    float getTime(float nit, int index) {
        float a = mALevels[index];
        float b = mBLevels[index];
        float time = (MathUtils.log(nit / a) / MathUtils.log(b)) / 24.0f;
        if (DEBUG) {
            Slog.d(TAG, "time: " + time + ", a: " + a + ", b: " + b);
        }
        return MathUtils.abs(time);
    }

    float getDarkeningRate(float brightness) {
        float rate;
        DisplayDeviceConfig.HighBrightnessModeData highBrightnessModeData = this.mHighBrightnessModeData;
        float normalMaxBrightnessFloat = highBrightnessModeData == null ? 1.0f : highBrightnessModeData.transitionPoint;
        float normalNit = this.mDisplayDeviceConfig.getNitsFromBacklight(normalMaxBrightnessFloat);
        if (normalNit == -1.0f) {
            normalNit = 500.0f;
        }
        int normalMaxBrightnessInt = BrightnessSynchronizer.brightnessFloatToInt(normalMaxBrightnessFloat);
        float nit = convertToNit(brightness);
        int index = getIndex(nit);
        float[] fArr = mBLevels;
        float rate2 = MathUtils.abs(mALevels[index] * 24.0f * MathUtils.pow(fArr[index], getTime(nit, index) * 24.0f) * MathUtils.log(fArr[index]));
        float f = this.mHbmTransitionPointNit;
        if (nit > f) {
            rate = rate2 / (this.mBrightnessMaxNit - f);
        } else {
            rate = (rate2 * normalMaxBrightnessFloat) / normalNit;
        }
        if (normalMaxBrightnessInt < 4095 && nit <= NIT_LEVEL) {
            rate = NIT_LEVEL / NORMAL_BRIGHTNESS_ON;
        } else if (IS_UMI_0B_DISPLAY_PANEL && nit <= NIT_LEVEL) {
            rate = 80.0f / NORMAL_BRIGHTNESS_ON;
        }
        if (DEBUG) {
            Slog.d(TAG, "rate: " + rate);
        }
        return MathUtils.max(rate, MIN_ADJUST_RATE);
    }

    float getExpRate(float begin, float end, float nit, float time1, float time2) {
        float beginDbv = convertToBrightness(begin);
        float endDbv = convertToBrightness(end);
        float curDbv = convertToBrightness(nit);
        float a = MathUtils.log(endDbv / beginDbv) / (time2 - time1);
        return a * curDbv;
    }

    float getBrighteningRate(float brightness, float startBrightness, float tgtBrightness) {
        float rate;
        float nit = convertToNit(brightness);
        float startNit = convertToNit(startBrightness);
        float targetNit = convertToNit(tgtBrightness);
        if (startNit < 35.0f) {
            if (targetNit < 35.0f) {
                rate = convertToBrightness(targetNit - startNit) / TIME_4;
            } else if (targetNit < NIT_LEVEL3) {
                if (nit < 35.0f) {
                    rate = (convertToBrightness(35.0f) - startBrightness) / TIME_3;
                } else {
                    rate = getExpRate(35.0f, targetNit, nit, TIME_3, TIME_4);
                }
            } else if (nit < 35.0f) {
                rate = (convertToBrightness(35.0f) - startBrightness) / TIME_2;
            } else if (nit < NIT_LEVEL2) {
                rate = getExpRate(35.0f, NIT_LEVEL2, nit, TIME_2, TIME_3);
            } else {
                rate = getExpRate(NIT_LEVEL2, targetNit, nit, TIME_3, TIME_4);
            }
        } else if (startNit < NIT_LEVEL3) {
            if (targetNit < NIT_LEVEL3) {
                rate = getExpRate(startNit, targetNit, nit, MiuiFreeformPinManagerService.EDGE_AREA, TIME_4);
            } else if (nit < NIT_LEVEL2) {
                rate = getExpRate(startNit, NIT_LEVEL2, nit, MiuiFreeformPinManagerService.EDGE_AREA, TIME_3);
            } else {
                rate = getExpRate(NIT_LEVEL2, targetNit, nit, TIME_3, TIME_4);
            }
        } else {
            rate = getExpRate(startNit, targetNit, nit, MiuiFreeformPinManagerService.EDGE_AREA, TIME_4);
        }
        return MathUtils.max(rate, MIN_ADJUST_RATE);
    }

    public float computeFloatRate(float rate, float currBrightness, float tgtBrightness) {
        if (this.mManualDimmingEnable) {
            return getManualDimmingRate(this.mStartBrightnessForManualDimming, tgtBrightness);
        }
        if (this.SUPPORT_TEMEPERATURE_CONTROL && rate == 0.007354082f) {
            return rate;
        }
        resetApplyingFastRate(tgtBrightness);
        float newRate = rate;
        String reason = "no_changed";
        if ((this.mAutomaticBrightnessEnable && this.mSlowChange) || this.mDisplayPowerControllerImpl.isEnterGalleryHdr() || this.mDisplayPowerControllerImpl.isExitGalleryHdr()) {
            if (rate == MiuiFreeformPinManagerService.EDGE_AREA) {
                newRate = MiuiFreeformPinManagerService.EDGE_AREA;
            } else if (this.mApplyingFastRate) {
                newRate = rate;
                reason = "apply_fast_rate";
            } else if (isAutoBrightnessByApplicationRateEnable()) {
                newRate = getAutobrightnessByApplicationRate(currBrightness, tgtBrightness, newRate);
                reason = "auto_brt_by_app";
            } else {
                float f = this.mStartBrightness;
                if (f < tgtBrightness) {
                    newRate = getBrighteningRate(currBrightness, f, tgtBrightness);
                    reason = "auto_brt_increase";
                } else {
                    newRate = getDarkeningRate(currBrightness);
                    reason = "auto_brt_decrease";
                }
            }
        }
        if (DEBUG) {
            Slog.i(TAG, "computeFloatRate: in rate: " + rate + ", out rate: " + newRate + ", reason: " + reason);
        }
        return newRate;
    }

    public void dump(PrintWriter pw) {
        pw.println("  USE_PROXIMITY_ENABLED=" + USE_PROXIMITY_ENABLED);
        pw.println("  BACKLIGHT_BIT=" + DeviceFeature.BACKLIGHT_BIT);
        pw.println("  HBM_BITS=" + DeviceFeature.HBMBACKLIGHT_BIT);
        pw.println("  mBrighteningRatio=" + this.mBrighteningRatio);
        this.mTouchAreaHelper.dump(pw);
        this.mDaemonSensorPolicy.dump(pw);
        if (supportDualSensorPolicy()) {
            this.mDualSensorPolicy.dump(pw);
        }
        if (isPointLightSourceDetectorEnable()) {
            this.mSceneDetector.dump(pw);
        }
        DEBUG = DisplayDebugConfig.DEBUG_ABC;
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

    public void setAutoBrightnessByApplicationRateEnable(boolean enable) {
        this.mIsAutoBrightnessByApplicationRateEnable = BrightnessMappingStrategyStub.getInstance().isSupportAutobrightnessByApplicationCategory() && enable;
    }

    public void setAutoBrightnessByApplicationAnimationEnable(boolean enable) {
        this.mIsAutoBrightnessByApplicationAnimationEnable = enable;
    }

    public boolean isAutoBrightnessByApplicationAnimationEnable() {
        return this.mIsAutoBrightnessByApplicationAnimationEnable;
    }

    public boolean isAutoBrightnessByApplicationRateEnable() {
        return this.mIsAutoBrightnessByApplicationRateEnable;
    }

    public void addUserDataPointByApplication(float lux, float brightness, String packageName, BrightnessMappingStrategy mapper) {
        if (mapper instanceof MiuiPhysicalBrightnessMappingStrategy) {
            MiuiPhysicalBrightnessMappingStrategy brightnessMapper = (MiuiPhysicalBrightnessMappingStrategy) mapper;
            brightnessMapper.addUserDataPoint(lux, brightness, packageName);
            return;
        }
        mapper.addUserDataPoint(lux, brightness);
    }

    public float getBrightnessByApplication(float lux, String packageName, int category, BrightnessMappingStrategy mapper, float value) {
        if (mapper instanceof MiuiPhysicalBrightnessMappingStrategy) {
            MiuiPhysicalBrightnessMappingStrategy brightnessMapper = (MiuiPhysicalBrightnessMappingStrategy) mapper;
            return brightnessMapper.getBrightness(lux, packageName);
        }
        return mapper.getBrightness(lux, packageName, category);
    }

    public float getAutobrightnessByApplicationRate(float currentBrightness, float targetBrightness, float rate) {
        if (isAutoBrightnessByApplicationAnimationEnable()) {
            if (targetBrightness > currentBrightness) {
                float diffBrightness = targetBrightness - currentBrightness;
                if (diffBrightness > currentBrightness) {
                    this.mAutoBrightnessByApplicationRate = diffBrightness / 10.0f;
                } else {
                    this.mAutoBrightnessByApplicationRate = diffBrightness / 5.0f;
                }
            } else {
                float diffBrightness2 = currentBrightness - targetBrightness;
                if (diffBrightness2 > targetBrightness) {
                    this.mAutoBrightnessByApplicationRate = diffBrightness2 / 10.0f;
                } else {
                    this.mAutoBrightnessByApplicationRate = diffBrightness2 / 5.0f;
                }
            }
            setAutoBrightnessByApplicationAnimationEnable(false);
        }
        return MathUtils.max(this.mAutoBrightnessByApplicationRate, 0.01f);
    }

    public SparseArray<Float> fillInLuxFromDaemonSensor() {
        SparseArray<Float> daemonSensorArray = new SparseArray<Float>() { // from class: com.android.server.display.AutomaticBrightnessControllerImpl.3
            {
                AutomaticBrightnessControllerImpl.this = this;
                int i = AutomaticBrightnessControllerStub.HANDLE_MAIN_LUX_EVENT;
                Float valueOf = Float.valueOf(Float.NaN);
                put(i, valueOf);
                put(AutomaticBrightnessControllerStub.HANDLE_ASSIST_LUX_EVENT, valueOf);
            }
        };
        if (!USE_DAEMON_SENSOR_POLICY) {
            return daemonSensorArray;
        }
        float mainLux = this.mDaemonSensorPolicy.getMainLightSensorLux();
        float assistLux = this.mDaemonSensorPolicy.getDaemonSensorValue(ASSIST_SENSOR_TYPE);
        if (USE_ASSISTSENSOR_ENABLED && checkAssistSensorValid() && assistLux > mainLux) {
            daemonSensorArray.put(HANDLE_ASSIST_LUX_EVENT, Float.valueOf(assistLux));
        } else {
            daemonSensorArray.put(HANDLE_MAIN_LUX_EVENT, Float.valueOf(mainLux));
        }
        if (DEBUG) {
            Slog.d(TAG, "fillInLuxFromDaemonSensor: mainLux: " + mainLux + ", assistLux: " + assistLux);
        }
        return daemonSensorArray;
    }

    public void stop() {
        setSensorEnabled(false);
        this.mDaemonSensorPolicy.stop();
        CameraManager cameraManager = this.mCameraManager;
        if (cameraManager != null) {
            cameraManager.unregisterTorchCallback(this.mTorchCallback);
        }
        this.mHandler.post(new Runnable() { // from class: com.android.server.display.AutomaticBrightnessControllerImpl$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                AutomaticBrightnessControllerImpl.this.m541x40168046();
            }
        });
    }

    /* renamed from: lambda$stop$0$com-android-server-display-AutomaticBrightnessControllerImpl */
    public /* synthetic */ void m541x40168046() {
        this.mTouchAreaHelper.stop();
    }

    public void setUpBrightnessMapper(BrightnessMappingStrategy mapper) {
        this.mBrightnessMapper = mapper;
    }

    public void setUpDisplayDeviceConfig(DisplayDeviceConfig deviceConfig) {
        this.mDisplayDeviceConfig = deviceConfig;
        DisplayDeviceConfig.HighBrightnessModeData highBrightnessModeData = deviceConfig.getHighBrightnessModeData();
        this.mHighBrightnessModeData = highBrightnessModeData;
        if (highBrightnessModeData != null) {
            this.mHbmTransitionPointNit = convertToNit(highBrightnessModeData.transitionPoint);
            this.mBrightnessMaxNit = convertToNit(1.0f);
            return;
        }
        float[] nits = this.mDisplayDeviceConfig.getNits();
        if (nits != null && nits.length != 0) {
            this.mBrightnessMaxNit = nits[nits.length - 1];
            return;
        }
        this.mBrightnessMaxNit = 500.0f;
        Slog.e(TAG, "The max nit of the device is not adapted.");
    }

    public void setUpLogicalDisplay(LogicalDisplay logicalDisplay) {
        TouchCoverProtectionHelper touchCoverProtectionHelper = this.mTouchAreaHelper;
        if (touchCoverProtectionHelper != null) {
            touchCoverProtectionHelper.setUpLogicalDisplay(logicalDisplay);
        }
    }

    public boolean getIsTorchOpen() {
        return this.mIsTorchOpen;
    }

    protected long getTorchCloseTime() {
        return this.mTorchCloseTime;
    }

    public void showTouchCoverProtectionRect(boolean isShow) {
        TouchCoverProtectionHelper touchCoverProtectionHelper = this.mTouchAreaHelper;
        if (touchCoverProtectionHelper != null) {
            touchCoverProtectionHelper.showTouchCoverProtectionRect(isShow);
        }
    }

    public boolean supportDualSensorPolicy() {
        return USE_ASSISTSENSOR_ENABLED && this.mDualSensorPolicy.getAssistLightSensor() != null;
    }

    public float getAmbientLux(int event, float preLux, float updateLux, boolean needUpdateLux) {
        if (needUpdateLux) {
            this.mAmbientLux = updateLux;
        }
        return this.mDualSensorPolicy.getAmbientLux(preLux, updateLux, needUpdateLux);
    }

    public boolean updateMainLightSensorAmbientThreshold(int event) {
        return this.mDualSensorPolicy.updateMainLightSensorAmbientThreshold(event);
    }

    public boolean updateBrightnessUsingMainLightSensor() {
        return this.mDualSensorPolicy.updateBrightnessUsingMainLightSensor();
    }

    public boolean updateDualSensorPolicy(long time, int event) {
        return this.mDualSensorPolicy.updateDualSensorPolicy(time, event);
    }

    public void setManualDimmingEnabled(boolean manualDimmingEnable, float startBrightness) {
        long currTime = SystemClock.uptimeMillis();
        this.mStartBrightnessForManualDimming = startBrightness;
        if (this.mManualDimmingEnable && !manualDimmingEnable && currTime - this.mLastTimeToSetManualDimmingEnabledMillis < 150) {
            Slog.d(TAG, "ignore the mManualDimmingEnable = false!");
        } else {
            this.mManualDimmingEnable = manualDimmingEnable;
        }
        this.mLastTimeToSetManualDimmingEnabledMillis = currTime;
    }

    private float getManualDimmingRate(float startBrightness, float tgtBrightness) {
        return (MathUtils.abs(tgtBrightness - startBrightness) * 1000.0f) / 150.0f;
    }

    public float getCurrentAmbientLux() {
        return this.mAmbientLux;
    }

    /* loaded from: classes.dex */
    private final class AutomaticBrightnessControllerImplHandler extends Handler {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public AutomaticBrightnessControllerImplHandler(Looper looper) {
            super(looper, null, true);
            AutomaticBrightnessControllerImpl.this = r2;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    AutomaticBrightnessControllerImpl.this.mNotInPocketTime = SystemClock.uptimeMillis();
                    AutomaticBrightnessControllerImpl.this.mFirstUseFastRateDueToOutPocket = true;
                    Slog.i(AutomaticBrightnessControllerImpl.TAG, "take phone out of pocket at the current time!");
                    return;
                case 2:
                    AutomaticBrightnessControllerImpl.this.mApplyingFastRate = false;
                    AutomaticBrightnessControllerImpl.this.mNeedUseFastRateBrightness = MiuiFreeformPinManagerService.EDGE_AREA;
                    Slog.i(AutomaticBrightnessControllerImpl.TAG, "Reset apply fast rate.");
                    return;
                default:
                    return;
            }
        }
    }

    public void notifyUnregisterDaemonSensor() {
        this.mDaemonSensorPolicy.setDaemonLightSensorsEnabled(false);
    }

    public void setAmbientLuxWhenInvalid(int event, float lux) {
        this.mDualSensorPolicy.setAmbientLuxWhenInvalid(event, lux);
    }

    public void notifyAdjustDarkeningDebounce(long time) {
        this.mDualSensorPolicy.adjustDarkeningDebounce(time);
    }

    public boolean needSuppressDarkening(float preLux, float updateLux, boolean needUpdateLux) {
        return this.mSuppressDarkeningEnable && needUpdateLux && preLux >= updateLux;
    }

    public void setSuppressDarkeningEnable(boolean enable) {
        this.mSuppressDarkeningEnable = enable;
    }

    public boolean getSuppressDarkeningEnable() {
        return this.mSuppressDarkeningEnable;
    }

    public long getAssistDarkeningDelayTime() {
        return this.mDualSensorPolicy.getAssistDarkeningDelayTime();
    }

    public long getMainDarkeningDelayTime() {
        return this.mDualSensorPolicy.getMainDarkeningDelayTime();
    }

    public void setUpCloudControllerListener(CloudControllerListener listener) {
        this.mCloudControllerListener = listener;
    }

    private boolean isPointLightSourceDetectorEnable() {
        CloudControllerListener cloudControllerListener = this.mCloudControllerListener;
        if (cloudControllerListener != null) {
            return cloudControllerListener.isPointLightSourceDetectorEnable();
        }
        return false;
    }
}
