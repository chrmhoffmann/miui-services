package com.android.server;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.ActivityTaskManager;
import android.app.IMiuiActivityObserver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.hardware.display.BrightnessInfo;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.FloatProperty;
import android.util.MathUtils;
import android.util.Slog;
import android.view.IWindowAnimationFinishedCallback;
import android.view.IWindowManager;
import com.android.server.wm.MiuiColorFade;
import com.android.server.wm.MiuiFreeformPinManagerService;
import com.miui.base.MiuiStubRegistry;
import java.io.PrintWriter;
/* loaded from: classes.dex */
public class MiuiUiModeManagerServiceStubImpl implements MiuiUiModeManagerServiceStub {
    private static float A = 0.0f;
    private static final int ANIMATION_DURATION_MILLIS = 800;
    private static float B = 0.0f;
    private static float C = 0.0f;
    private static final int GAMMA_SPACE_MIN = 0;
    private static final int NIGHT_CHANGE_ANIM_START = 1;
    private static float R = 0.0f;
    private static final String TAG = "MiuiUiModeManagerServiceStubImpl";
    private Context mContext;
    private int mDelayedSetNightModeUser;
    private DisplayManager mDisplayManager;
    private ForceDarkUiModeModeManager mForceDarkUiModeModeManager;
    private IWindowManager mIWindowManager;
    private ComponentName mLastResumedActivity;
    private MiuiColorFade mMiuiColorFade;
    private ObjectAnimator mNightColorFadeAnimator;
    private UiModeManagerService mUiModeManagerService;
    private static final boolean IS_MEXICO_TELCEL = "mx_telcel".equals(SystemProperties.get("ro.miui.customized.region"));
    private static final boolean IS_JP_KDDI = "jp_kd".equals(SystemProperties.get("ro.miui.customized.region"));
    private static final int GAMMA_SPACE_MAX = Resources.getSystem().getInteger(17694941);
    private static final FloatProperty<MiuiColorFade> COLOR_FADE_LEVEL = new FloatProperty<MiuiColorFade>("alphaLevel") { // from class: com.android.server.MiuiUiModeManagerServiceStubImpl.2
        public void setValue(MiuiColorFade object, float value) {
            object.setColorFadeAlphaLevel(value);
        }

        public Float get(MiuiColorFade object) {
            return Float.valueOf(object.getColorFadeAlphaLevel());
        }
    };
    private volatile int mDelayedNightMode = -1;
    private final Handler mHandler = new Handler() { // from class: com.android.server.MiuiUiModeManagerServiceStubImpl.1
        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    MiuiUiModeManagerServiceStubImpl.this.doNightChangeAnimation();
                    return;
                default:
                    return;
            }
        }
    };
    private boolean mIsActivityResumed = false;
    private boolean mIsActivityPaused = false;
    private boolean mShouldPendingSwitch = false;
    private boolean mIsDelayedNightMode = false;
    private IWindowAnimationFinishedCallback mAnimFinishedCallback = new IWindowAnimationFinishedCallback.Stub() { // from class: com.android.server.MiuiUiModeManagerServiceStubImpl.3
        public void onWindowAnimFinished() {
            if (MiuiUiModeManagerServiceStubImpl.this.mShouldPendingSwitch) {
                if ((MiuiUiModeManagerServiceStubImpl.this.mIsActivityResumed || MiuiUiModeManagerServiceStubImpl.this.mIsActivityPaused) && MiuiUiModeManagerServiceStubImpl.this.mDelayedNightMode > 0) {
                    MiuiUiModeManagerServiceStubImpl.this.mIsDelayedNightMode = true;
                    try {
                        Slog.i(MiuiUiModeManagerServiceStubImpl.TAG, "apply pending night mode switch " + MiuiUiModeManagerServiceStubImpl.this.mDelayedNightMode);
                        MiuiUiModeManagerServiceStubImpl.this.mUiModeManagerService.getService().setNightMode(MiuiUiModeManagerServiceStubImpl.this.mDelayedNightMode);
                    } catch (RemoteException e) {
                        Slog.w(MiuiUiModeManagerServiceStubImpl.TAG, "Failure communicating with uimode manager", e);
                    }
                    MiuiUiModeManagerServiceStubImpl.this.mIsActivityResumed = false;
                    MiuiUiModeManagerServiceStubImpl.this.mIsActivityPaused = false;
                    MiuiUiModeManagerServiceStubImpl.this.mShouldPendingSwitch = false;
                }
            }
        }
    };
    private final DisplayManager.DisplayListener mDisplayListener = new DisplayManager.DisplayListener() { // from class: com.android.server.MiuiUiModeManagerServiceStubImpl.4
        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayAdded(int displayId) {
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayRemoved(int displayId) {
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayChanged(int displayId) {
            BrightnessInfo info = MiuiUiModeManagerServiceStubImpl.this.mContext.getDisplay().getBrightnessInfo();
            if (info == null) {
                return;
            }
            float mMaximumBrightness = info.brightnessMaximum;
            float mMinimumBrightness = info.brightnessMinimum;
            float mBrightnessValue = info.brightness;
            MiuiUiModeManagerServiceStubImpl miuiUiModeManagerServiceStubImpl = MiuiUiModeManagerServiceStubImpl.this;
            miuiUiModeManagerServiceStubImpl.updateAlpha(miuiUiModeManagerServiceStubImpl.mContext, mBrightnessValue, mMaximumBrightness, mMinimumBrightness);
        }
    };
    private final Animator.AnimatorListener mAnimatorListener = new Animator.AnimatorListener() { // from class: com.android.server.MiuiUiModeManagerServiceStubImpl.5
        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationStart(Animator animation) {
        }

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationEnd(Animator animation) {
            MiuiUiModeManagerServiceStubImpl.this.mHandler.removeMessages(1);
            MiuiUiModeManagerServiceStubImpl.this.mMiuiColorFade.dismiss();
        }

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationRepeat(Animator animation) {
        }

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationCancel(Animator animation) {
            MiuiUiModeManagerServiceStubImpl.this.mHandler.removeMessages(1);
            MiuiUiModeManagerServiceStubImpl.this.mMiuiColorFade.dismiss();
        }
    };
    private final IMiuiActivityObserver mMiuiActivityObserver = new IMiuiActivityObserver.Stub() { // from class: com.android.server.MiuiUiModeManagerServiceStubImpl.7
        public void activityIdle(Intent intent) throws RemoteException {
        }

        public void activityResumed(Intent intent) throws RemoteException {
            ComponentName cn = intent.getComponent();
            if (cn != null && !cn.equals(MiuiUiModeManagerServiceStubImpl.this.mLastResumedActivity)) {
                MiuiUiModeManagerServiceStubImpl.this.mIsActivityResumed = true;
            }
        }

        public void activityPaused(Intent intent) throws RemoteException {
            ComponentName cn = intent.getComponent();
            if (cn != null && cn.equals(MiuiUiModeManagerServiceStubImpl.this.mLastResumedActivity)) {
                MiuiUiModeManagerServiceStubImpl.this.mIsActivityPaused = true;
            }
        }

        public void activityStopped(Intent intent) throws RemoteException {
        }

        public void activityDestroyed(Intent intent) throws RemoteException {
        }
    };

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MiuiUiModeManagerServiceStubImpl> {

        /* compiled from: MiuiUiModeManagerServiceStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MiuiUiModeManagerServiceStubImpl INSTANCE = new MiuiUiModeManagerServiceStubImpl();
        }

        public MiuiUiModeManagerServiceStubImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public MiuiUiModeManagerServiceStubImpl provideNewInstance() {
            return new MiuiUiModeManagerServiceStubImpl();
        }
    }

    public void init(UiModeManagerService uiModeManagerService) {
        this.mUiModeManagerService = uiModeManagerService;
        Context context = uiModeManagerService.getContext();
        this.mContext = context;
        this.mDisplayManager = (DisplayManager) context.getSystemService(DisplayManager.class);
        PowerManager powerManager = (PowerManager) this.mContext.getSystemService("power");
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.USER_SWITCHED");
        registUIModeScaleChangeObserver(uiModeManagerService, this.mContext);
        this.mDisplayManager.registerDisplayListener(this.mDisplayListener, this.mHandler, 8L);
        try {
            Slog.i(TAG, "mNightMode: " + uiModeManagerService.getService().getNightMode());
        } catch (RemoteException e) {
            Slog.w(TAG, "Failure communicating with uimode manager", e);
        }
        this.mIWindowManager = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
        MiuiColorFade miuiColorFade = new MiuiColorFade(0);
        this.mMiuiColorFade = miuiColorFade;
        ObjectAnimator ofFloat = ObjectAnimator.ofFloat(miuiColorFade, COLOR_FADE_LEVEL, 1.0f, MiuiFreeformPinManagerService.EDGE_AREA);
        this.mNightColorFadeAnimator = ofFloat;
        ofFloat.setDuration(800L);
        this.mNightColorFadeAnimator.addListener(this.mAnimatorListener);
        registerAnimFinishedCallback();
        registerActivityObserver();
        this.mForceDarkUiModeModeManager = new ForceDarkUiModeModeManager(uiModeManagerService);
        R = this.mContext.getResources().getFloat(285671454);
        A = this.mContext.getResources().getFloat(285671451);
        B = this.mContext.getResources().getFloat(285671452);
        C = this.mContext.getResources().getFloat(285671453);
    }

    public void updateAlpha(Context context, float mBrightness, float mMaxBrightness, float mMinBrightness) {
        float ratio = convertLinearToGammaFloat(mBrightness, mMinBrightness, mMaxBrightness) / GAMMA_SPACE_MAX;
        double alpha = 0.0d;
        if (ratio < 0.4d && ratio > 0.1d) {
            alpha = Math.sqrt((0.4d - ratio) / 2.5d);
        }
        if (ratio <= 0.1d) {
            alpha = (ratio + 0.1366d) / 0.683d;
        }
        float setalpha = (float) alpha;
        Settings.System.putFloat(context.getContentResolver(), "contrast_alpha", setalpha);
    }

    private int convertLinearToGammaFloat(float val, float min, float max) {
        float ret;
        float normalizedVal = MathUtils.norm(min, max, val) * 12.0f;
        if (normalizedVal <= 1.0f) {
            ret = MathUtils.sqrt(normalizedVal) * R;
        } else {
            float ret2 = A;
            ret = (ret2 * MathUtils.log(normalizedVal - B)) + C;
        }
        return Math.round(MathUtils.lerp(0, GAMMA_SPACE_MAX, ret));
    }

    public void onBootPhase(int phase) {
        this.mForceDarkUiModeModeManager.onBootPhase(phase);
    }

    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        return this.mForceDarkUiModeModeManager.onTransact(code, data, reply, flags);
    }

    public void setNightMode(int mode) {
        Slog.i(TAG, "mNightMode: " + mode);
        this.mDelayedNightMode = -1;
        this.mIsDelayedNightMode = false;
    }

    public void doNightChangeAnimation() {
        MiuiColorFade miuiColorFade = this.mMiuiColorFade;
        if (miuiColorFade != null && miuiColorFade.prepare(0)) {
            this.mNightColorFadeAnimator.start();
        }
    }

    private void registUIModeScaleChangeObserver(final UiModeManagerService service, final Context context) {
        ContentObserver uiModeScaleChangedObserver = new ContentObserver(new Handler()) { // from class: com.android.server.MiuiUiModeManagerServiceStubImpl.6
            @Override // android.database.ContentObserver
            public void onChange(boolean selfChange) {
                service.setDefaultUiModeType(Settings.System.getInt(context.getContentResolver(), "ui_mode_scale", 1));
                synchronized (MiuiUiModeManagerServiceStubImpl.this.mUiModeManagerService.getInnerLock()) {
                    if (service.mSystemReady) {
                        service.updateLocked(0, 0);
                    }
                }
            }
        };
        context.getContentResolver().registerContentObserver(Settings.System.getUriFor("ui_mode_scale"), false, uiModeScaleChangedObserver);
        uiModeScaleChangedObserver.onChange(false);
    }

    public boolean shouldDelayNightModeChange(int mode, int user) {
        if (this.mIsDelayedNightMode) {
            return false;
        }
        boolean onlyTimingMode = Settings.Global.getInt(this.mContext.getContentResolver(), "uimode_timing", 0) != 0;
        if (onlyTimingMode && mode != 0) {
            Slog.i(TAG, "pending switch night mode to " + mode);
            Settings.Global.putInt(this.mContext.getContentResolver(), "uimode_timing", 0);
            Intent activity = null;
            try {
                activity = ActivityTaskManager.getService().getTopVisibleActivity();
            } catch (RemoteException e) {
                Slog.w(TAG, "Failure communicating with activity manager", e);
            }
            ComponentName component = activity != null ? activity.getComponent() : null;
            this.mLastResumedActivity = component;
            if (component != null) {
                this.mDelayedNightMode = mode;
                this.mDelayedSetNightModeUser = user;
                this.mShouldPendingSwitch = true;
                return true;
            }
        }
        this.mShouldPendingSwitch = false;
        return false;
    }

    private void registerAnimFinishedCallback() {
        try {
            this.mIWindowManager.registerUiModeAnimFinishedCallback(this.mAnimFinishedCallback);
        } catch (RemoteException e) {
            Slog.d(TAG, "registerAnimFinishedCallback error" + e);
        }
    }

    private void registerActivityObserver() {
        try {
            ActivityTaskManager.getService().registerActivityObserver(this.mMiuiActivityObserver, new Intent());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void dump(PrintWriter pw) {
        this.mForceDarkUiModeModeManager.dump(pw);
    }
}
