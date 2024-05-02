package com.android.server.vibrator;

import android.content.Context;
import android.database.ContentObserver;
import android.hardware.camera2.CameraManager;
import android.hardware.vibrator.IVibrator;
import android.net.Uri;
import android.os.CombinedVibration;
import android.os.DeadObjectException;
import android.os.DynamicEffect;
import android.os.ExternalVibration;
import android.os.Handler;
import android.os.Looper;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.Vibrator;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Slog;
import com.android.server.MQSThread;
import com.android.server.wm.MiuiFreeformPinManagerService;
import com.miui.base.MiuiStubRegistry;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import miui.os.Build;
import miui.util.AudioManagerHelper;
import vendor.hardware.vibratorfeature.IVibratorExt;
/* loaded from: classes.dex */
public class VibratorManagerServiceImpl implements VibratorManagerServiceStub {
    private static final long DYNAMIC_EFFECT_IGNORE_TIMEOUT = 3000;
    private static final String HAPTIC_EFFECT_CONFIG_STRENGTH = "haptic_feedback_config_strength";
    private static final String HAPTIC_FEEDBACK_DISABLE = "haptic_feedback_disable";
    private static final String HAPTIC_FEEDBACK_INFINITE_INTENSITY = "haptic_feedback_infinite_intensity";
    private static final long MAX_VIBRATOR_TIMEOUT = 10000;
    private static final long PERFECT_VIBRATOR_TIMEOUT = 1000;
    private static final String TAG = "VibratorManagerServiceImpl";
    private static final int USAGE_DYNAMICEFFECT = -2;
    private CameraManager mCameraManager;
    private Context mContext;
    private Vibration mCurrentDynamicEffect;
    private Uri mHapticFeedbackDisableUri;
    private Uri mHapticFeedbackInfiniteIntensityUri;
    private boolean mIncall;
    private PhoneStateListener mListener;
    private volatile MQSThread mMQSThread;
    private SettingsObserver mSettingObserver;
    private Vibrator mVibrator;
    private static long VIBRATION_THRESHOLD_IN_CALL = 30;
    private static final String DEVICE = Build.DEVICE.toLowerCase();
    private static final boolean mIgnoreVibrationWhenCamera = SystemProperties.getBoolean("sys.haptic.ignoreWhenCamera", false);
    private static int hapticFeedbackDisable = 0;
    private IVibrator mHal = null;
    private IVibratorExt mHalExt = null;
    private Set<String> mCameraUnAvailableSets = new HashSet();
    private final CameraManager.AvailabilityCallback mAvailabilityCallback = new CameraManager.AvailabilityCallback() { // from class: com.android.server.vibrator.VibratorManagerServiceImpl.1
        @Override // android.hardware.camera2.CameraManager.AvailabilityCallback
        public void onCameraAvailable(String cameraId) {
            Slog.w(VibratorManagerServiceImpl.TAG, "onCameraAvailable with id" + cameraId);
            if (VibratorManagerServiceImpl.this.mCameraUnAvailableSets.remove(cameraId)) {
                Slog.w(VibratorManagerServiceImpl.TAG, "remove from mCameraUnAvailableSet");
            }
        }

        @Override // android.hardware.camera2.CameraManager.AvailabilityCallback
        public void onCameraUnavailable(String cameraId) {
            Slog.w(VibratorManagerServiceImpl.TAG, "onCameraUnavailable with id" + cameraId);
            VibratorManagerServiceImpl.this.mCameraUnAvailableSets.add(cameraId);
        }
    };

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<VibratorManagerServiceImpl> {

        /* compiled from: VibratorManagerServiceImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final VibratorManagerServiceImpl INSTANCE = new VibratorManagerServiceImpl();
        }

        public VibratorManagerServiceImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public VibratorManagerServiceImpl provideNewInstance() {
            return new VibratorManagerServiceImpl();
        }
    }

    public void init(Context context) {
        this.mContext = context;
        listenForCallState();
        if (mIgnoreVibrationWhenCamera) {
            CameraManager cameraManager = (CameraManager) context.getSystemService("camera");
            this.mCameraManager = cameraManager;
            cameraManager.registerAvailabilityCallback(this.mAvailabilityCallback, (Handler) null);
        }
        this.mSettingObserver = new SettingsObserver(new Handler(Looper.myLooper()));
        Uri uriFor = Settings.System.getUriFor(HAPTIC_FEEDBACK_INFINITE_INTENSITY);
        this.mHapticFeedbackInfiniteIntensityUri = uriFor;
        registerSettingsObserver(uriFor);
        getHalExt();
        try {
            this.mHalExt.setAmplitudeExt(Settings.System.getFloatForUser(this.mContext.getContentResolver(), HAPTIC_FEEDBACK_INFINITE_INTENSITY, 1.0f, -2), 0);
        } catch (DeadObjectException e) {
            Slog.e(TAG, "fail to init amplitude ext, reset mHalExt " + e);
            getHalExt();
        } catch (Exception e2) {
            Slog.e(TAG, "skip init amplitude ext " + e2);
        }
        Settings.System.putInt(this.mContext.getContentResolver(), HAPTIC_FEEDBACK_DISABLE, 0);
        Uri uriFor2 = Settings.System.getUriFor(HAPTIC_FEEDBACK_DISABLE);
        this.mHapticFeedbackDisableUri = uriFor2;
        registerSettingsObserver(uriFor2);
    }

    private void registerSettingsObserver(Uri settingUri) {
        this.mContext.getContentResolver().registerContentObserver(settingUri, true, this.mSettingObserver, -1);
    }

    private void listenForCallState() {
        this.mListener = new PhoneStateListener() { // from class: com.android.server.vibrator.VibratorManagerServiceImpl.2
            @Override // android.telephony.PhoneStateListener
            public void onCallStateChanged(int state, String incomingNumber) {
                VibratorManagerServiceImpl.this.mIncall = state != 0;
            }
        };
        TelephonyManager.from(this.mContext).listen(this.mListener, 32);
    }

    public CombinedVibration calculateVibrateForMiui(CombinedVibration effect, int usageHint) {
        if (shouldIgnoredVibration(usageHint)) {
            return null;
        }
        return effect;
    }

    public boolean shouldIgnoredVibration(int usageHint) {
        if ((usageHint == 33 || usageHint == 49) && !AudioManagerHelper.isVibrateEnabled(this.mContext)) {
            return true;
        }
        if (!mIgnoreVibrationWhenCamera || this.mCameraUnAvailableSets.isEmpty()) {
            return false;
        }
        if (usageHint == 17 || usageHint == 33) {
            Slog.w(TAG, "ignore incoming vibration in favor of camera in device  " + Build.DEVICE);
            return true;
        }
        return false;
    }

    public long weakenVibrationIfNecessary(long time, int uid) {
        if (this.mIncall && UserHandle.isApp(uid) && time > VIBRATION_THRESHOLD_IN_CALL) {
            return VIBRATION_THRESHOLD_IN_CALL;
        }
        if (time > 10000) {
            return PERFECT_VIBRATOR_TIMEOUT;
        }
        return time;
    }

    public String startVibrationLockedInjector(Vibration vib) {
        if (hapticFeedbackDisable == 1) {
            return "hapticFeedbackDisable";
        }
        if (playDynamicEffectIfItIs(vib)) {
            return "playDynamicEffect";
        }
        if (processTaggedReasonIfItHas(vib)) {
            return "processTaggedReason";
        }
        return null;
    }

    public String startVibrationLockedInjector(ExternalVibration vib) {
        if (hapticFeedbackDisable == 1) {
            return "hapticFeedbackDisable";
        }
        return null;
    }

    public boolean processTaggedReasonIfItHas(Vibration vib) {
        if (vib.reason == null) {
            return false;
        }
        String[] settingsInfo = vib.reason.split(";");
        if (!settingsInfo[0].split("=")[0].equals("TAG") || settingsInfo[0].split("=").length != 2) {
            return false;
        }
        HashMap<String, String> settingsMap = new HashMap<>();
        for (String settingInfo : settingsInfo) {
            String[] setting = settingInfo.split("=");
            if (setting.length == 2) {
                settingsMap.put(setting[0], setting[1]);
            }
        }
        String str = settingsMap.get("TAG");
        char c = 65535;
        switch (str.hashCode()) {
            case -1616845542:
                if (str.equals(HAPTIC_FEEDBACK_INFINITE_INTENSITY)) {
                    c = 1;
                    break;
                }
                break;
            case -146773876:
                if (str.equals(HAPTIC_EFFECT_CONFIG_STRENGTH)) {
                    c = 0;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
                try {
                    int effectId = Integer.parseInt(settingsMap.get("effectId"));
                    float intensity = Float.parseFloat(settingsMap.get("intensity"));
                    this.mHalExt.configStrengthForEffect(effectId, intensity);
                } catch (DeadObjectException e) {
                    Slog.e(TAG, "fail to configStrength, reset mHalExt " + e);
                    getHalExt();
                } catch (Exception e2) {
                    Slog.e(TAG, "skip configStrength " + e2);
                }
                return true;
            case 1:
                try {
                    float intensity2 = Float.parseFloat(settingsMap.get("intensity"));
                    this.mHalExt.setAmplitudeExt(intensity2, 1);
                } catch (DeadObjectException e3) {
                    Slog.e(TAG, "fail to setAmplitudeExt, reset mHalExt " + e3);
                    getHalExt();
                } catch (Exception e4) {
                    Slog.e(TAG, "skip setAmplitudeExt " + e4);
                }
                return true;
            default:
                return false;
        }
    }

    public boolean playDynamicEffectIfItIs(Vibration vib) {
        if (vib.getEffect() instanceof DynamicEffect) {
            this.mCurrentDynamicEffect = vib;
            DynamicEffect d = vib.getEffect();
            try {
                this.mHalExt.play(d.encapsulate(), d.mLoop, d.mInterval, -1);
                this.mMQSThread = new MQSThread(vib.opPkg);
                this.mMQSThread.start();
                return true;
            } catch (DeadObjectException e) {
                Slog.e(TAG, "fail to play dynamicEffect, reset mHalExt " + e);
                getHalExt();
                return true;
            } catch (Exception e2) {
                Slog.e(TAG, "skip play dynamicEffect " + e2);
                return true;
            }
        }
        return false;
    }

    public boolean cancelDynamicEffectIfItIs(int usageFilter) {
        if (usageFilter != -2 || this.mCurrentDynamicEffect == null) {
            return false;
        }
        try {
            this.mHal.off();
            this.mCurrentDynamicEffect = null;
            return true;
        } catch (DeadObjectException e) {
            Slog.e(TAG, "fail to cancel dynamicEffect, reset mHalExt " + e);
            getHalExt();
            return false;
        } catch (Exception e2) {
            Slog.e(TAG, "skip cancel dynamicEffect " + e2);
            return false;
        }
    }

    public boolean shouldIgnoreForDynamicEffect(Vibration incomingVibration) {
        if (this.mCurrentDynamicEffect != null) {
            CombinedVibration incoming = incomingVibration.getEffect();
            if (incomingVibration.attrs.getUsage() == 33 || incomingVibration.attrs.getUsage() == 17) {
                Slog.d(TAG, "stop DynamicEffect in favor of current Effect's attribute" + incomingVibration.attrs.getUsage());
                cancelDynamicEffectIfItIs(-2);
                return false;
            } else if (!(incoming instanceof DynamicEffect)) {
                if (SystemClock.uptimeMillis() - this.mCurrentDynamicEffect.startUptimeMillis < 3000) {
                    Slog.d(TAG, "Ignoring incoming vibration in favor of current Dynamic Effect");
                    return true;
                }
                Slog.d(TAG, "cancel current dynamiceffect");
                cancelDynamicEffectIfItIs(-2);
            }
        }
        return false;
    }

    private IVibrator getHal() {
        try {
        } catch (Exception e) {
            Slog.d(TAG, "fail to get vibrator hal server " + e);
            this.mHal = null;
        }
        if (!SystemProperties.get("sys.haptic.motor", "").equals("linear") && !SystemProperties.get("sys.haptic.motor", "").equals("zlinear")) {
            Slog.d(TAG, "skip vibrator hal");
            return null;
        }
        Slog.d(TAG, "waiting for vibratorfeature hal begin");
        this.mHal = IVibrator.Stub.asInterface(ServiceManager.waitForService("android.hardware.vibrator.IVibrator/vibratorfeature"));
        Slog.d(TAG, "waiting for vibratorfeature hal end");
        return this.mHal;
    }

    public IVibratorExt getHalExt() {
        getHal();
        try {
        } catch (Exception e) {
            Slog.d(TAG, "fail to get VibratorExt server " + e);
            this.mHalExt = null;
        }
        if (!SystemProperties.get("sys.haptic.motor", "").equals("linear") && !SystemProperties.get("sys.haptic.motor", "").equals("zlinear")) {
            Slog.d(TAG, "skip vibrator halExt");
            return null;
        }
        this.mHalExt = IVibratorExt.Stub.asInterface(getHal().asBinder().getExtension());
        return this.mHalExt;
    }

    /* loaded from: classes.dex */
    public final class SettingsObserver extends ContentObserver {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        SettingsObserver(Handler handler) {
            super(handler);
            VibratorManagerServiceImpl.this = r1;
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            if (VibratorManagerServiceImpl.this.mVibrator == null) {
                VibratorManagerServiceImpl vibratorManagerServiceImpl = VibratorManagerServiceImpl.this;
                vibratorManagerServiceImpl.mVibrator = (Vibrator) vibratorManagerServiceImpl.mContext.getSystemService("vibrator");
            }
            try {
                if (uri.equals(VibratorManagerServiceImpl.this.mHapticFeedbackInfiniteIntensityUri)) {
                    VibratorManagerServiceImpl.this.mHalExt.setAmplitudeExt(Settings.System.getFloatForUser(VibratorManagerServiceImpl.this.mContext.getContentResolver(), VibratorManagerServiceImpl.HAPTIC_FEEDBACK_INFINITE_INTENSITY, 1.0f, -2), 0);
                } else if (uri.equals(VibratorManagerServiceImpl.this.mHapticFeedbackDisableUri)) {
                    VibratorManagerServiceImpl.hapticFeedbackDisable = Settings.System.getIntForUser(VibratorManagerServiceImpl.this.mContext.getContentResolver(), VibratorManagerServiceImpl.HAPTIC_FEEDBACK_DISABLE, -2);
                    if (VibratorManagerServiceImpl.hapticFeedbackDisable == 1) {
                        VibratorManagerServiceImpl.this.mHal.off();
                        VibratorManagerServiceImpl.this.mHalExt.setAmplitudeExt(MiuiFreeformPinManagerService.EDGE_AREA, 2);
                        VibratorManagerServiceImpl.this.mVibrator.cancel(33);
                        Slog.d(VibratorManagerServiceImpl.TAG, "start disable haptic feedback");
                    }
                }
            } catch (DeadObjectException e) {
                Slog.e(VibratorManagerServiceImpl.TAG, "fail to setAmplitudeExt, reset mHalExt " + e);
                VibratorManagerServiceImpl.this.getHalExt();
            } catch (Exception e2) {
                Slog.e(VibratorManagerServiceImpl.TAG, "skip setAmplitudeExt with flag " + e2);
            }
        }
    }
}
