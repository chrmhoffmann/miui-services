package com.miui.server.smartpower;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.graphics.Point;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Slog;
import android.view.RemoteAnimationTarget;
import android.view.SurfaceControl;
import android.view.WindowInsets;
import com.android.server.am.SmartPowerService;
import com.android.server.display.DisplayManagerServiceImpl;
import com.android.server.display.DisplayModeDirector;
import com.android.server.display.DisplayModeDirectorImpl;
import com.android.server.inputmethod.InputMethodManagerServiceImpl;
import com.android.server.wm.ActivityRecord;
import com.android.server.wm.RealTimeModeControllerImpl;
import com.android.server.wm.SchedBoostGesturesEvent;
import com.android.server.wm.WindowManagerService;
import com.android.server.wm.WindowManagerServiceImpl;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
/* loaded from: classes.dex */
public class SmartDisplayPolicyManager {
    private static final int INSET_IME_HIDE = 0;
    private static final int INSET_IME_SHOW = 1;
    private static final int INTERACT_ALL_TYPE = 1;
    private static final int INTERACT_APP_TRANSITION = 2;
    private static final int INTERACT_FOCUS_WINDOW_CHANGE = 7;
    private static final int INTERACT_INPUT_DOWN = 8;
    private static final int INTERACT_INPUT_FLING = 10;
    private static final int INTERACT_INPUT_MOVE = 9;
    private static final int INTERACT_INSET_ANIMATION = 4;
    private static final int INTERACT_NONE_TYPE = 0;
    private static final int INTERACT_RECENT_ANIMATION = 5;
    private static final int INTERACT_REMOTE_ANIMATION = 6;
    private static final int INTERACT_WINDOW_ANIMATION = 3;
    private static final int INVALID_ANIMATION_DUR = 200;
    private static final int INVALID_SF_MODE_ID = -1;
    private static final int MODULE_SF_INTERACTION = 268;
    private static final int MSG_DOWN_REFRESHRATE = 2;
    private static final int MSG_UP_REFRESHRATE = 1;
    private static final int SF_INTERACTION_END = 0;
    private static final int SF_INTERACTION_START = 1;
    private static final String SF_PERF_INTERACTION = "perf_interaction";
    public static final String TAG = "SmartPower.DisplayPolicy";
    private static final int TRANSACTION_SF_DISPLAY_FEATURE_IDLE = 31107;
    private final Context mContext;
    private int mDisplayModeHeight;
    private int mDisplayModeWidth;
    private final H mHandler;
    private final HandlerThread mHandlerTh;
    private RefreshRatePolicyController mRefreshRatePolicyController;
    private SchedBoostGesturesEvent mSchedBoostGesturesEvent;
    private SurfaceControl.DisplayMode[] mSupportedDisplayModes;
    private final WindowManagerService mWMS;
    public static final boolean DEBUG = SmartPowerService.DEBUG;
    public static boolean sEnable = SmartPowerSettings.DISPLAY_POLICY_ENABLE;
    private final Object mDisplayLock = new Object();
    private int mLastInputMethodStatus = 0;
    private String mLastFocusPackage = "";
    private final IBinder mSurfaceFlinger = ServiceManager.getService("SurfaceFlinger");

    public static String interactionTypeToString(int type) {
        switch (type) {
            case 0:
                return "none";
            case 1:
                return "all";
            case 2:
                return "app transition";
            case 3:
                return "window animation";
            case 4:
            default:
                return String.valueOf(type);
            case 5:
                return "recent animation";
            case 6:
                return "remote animation";
            case 7:
                return "focus window changed";
            case 8:
                return "input: down";
            case 9:
                return "input: move";
            case 10:
                return "input: fling";
        }
    }

    public SmartDisplayPolicyManager(Context context, WindowManagerService wms) {
        HandlerThread handlerThread = new HandlerThread("SmartPowerDisplayTh", -2);
        this.mHandlerTh = handlerThread;
        this.mRefreshRatePolicyController = null;
        this.mSchedBoostGesturesEvent = null;
        this.mContext = context;
        this.mWMS = wms;
        handlerThread.start();
        this.mHandler = new H(handlerThread.getLooper());
        Process.setThreadGroupAndCpuset(handlerThread.getThreadId(), 1);
        this.mRefreshRatePolicyController = new RefreshRatePolicyController();
        SchedBoostGesturesEvent schedBoostGesturesEvent = new SchedBoostGesturesEvent(handlerThread.getLooper());
        this.mSchedBoostGesturesEvent = schedBoostGesturesEvent;
        schedBoostGesturesEvent.init(context);
        this.mSchedBoostGesturesEvent.setGesturesEventListener(new SchedBoostGesturesEvent.GesturesEventListener() { // from class: com.miui.server.smartpower.SmartDisplayPolicyManager.1
            @Override // com.android.server.wm.SchedBoostGesturesEvent.GesturesEventListener
            public void onFling(int durationMs) {
                SmartDisplayPolicyManager.this.notifyInputEventReceived(10, durationMs);
            }

            @Override // com.android.server.wm.SchedBoostGesturesEvent.GesturesEventListener
            public void onScroll(boolean started) {
            }

            @Override // com.android.server.wm.SchedBoostGesturesEvent.GesturesEventListener
            public void onDown() {
                SmartDisplayPolicyManager.this.notifyInputEventReceived(8, 0);
            }

            @Override // com.android.server.wm.SchedBoostGesturesEvent.GesturesEventListener
            public void onMove() {
                SmartDisplayPolicyManager.this.notifyInputEventReceived(9, 0);
            }
        });
    }

    public void init() {
    }

    private void setDisplaySize(int width, int height) {
        synchronized (this.mDisplayLock) {
            this.mDisplayModeWidth = width;
            this.mDisplayModeHeight = height;
            if (DEBUG) {
                Slog.d(TAG, "update display size: " + this.mDisplayModeWidth + " * " + this.mDisplayModeHeight);
            }
        }
    }

    private void resetDisplaySize() {
        synchronized (this.mDisplayLock) {
            this.mDisplayModeWidth = 0;
            this.mDisplayModeHeight = 0;
        }
    }

    private void updateSupportDisplayModes() {
        SurfaceControl.DisplayMode[] displayModes = getSupportDisplayModes();
        synchronized (this.mDisplayLock) {
            this.mSupportedDisplayModes = displayModes;
            updatePolicyControllerLocked();
        }
    }

    private SurfaceControl.DisplayMode[] getSupportDisplayModes() {
        SurfaceControl.DynamicDisplayInfo dynamicDisplayInfo = DisplayManagerServiceImpl.getInstance().updateDefaultDisplaySupportMode();
        if (dynamicDisplayInfo == null) {
            Slog.e(TAG, "get dynamic display info error");
            return null;
        }
        SurfaceControl.DisplayMode[] supportedDisplayModes = dynamicDisplayInfo.supportedDisplayModes;
        if (supportedDisplayModes != null && DEBUG) {
            Slog.d(TAG, "update display modes: " + Arrays.toString(supportedDisplayModes));
        }
        return supportedDisplayModes;
    }

    private void updatePolicyControllerLocked() {
        this.mRefreshRatePolicyController.updateControllerLocked();
    }

    public void notifyAppTransitionStartLocked(long animDuration) {
        if (!isSupportSmartDisplayPolicy() || animDuration <= 200) {
            return;
        }
        if (DEBUG) {
            Slog.d(TAG, "notifyAppTransitionStart animDuration :" + animDuration);
        }
        RefreshRatePolicyController refreshRatePolicyController = this.mRefreshRatePolicyController;
        refreshRatePolicyController.notifyInteractionStart(new ComingInteraction(refreshRatePolicyController.mMinRefreshRateInPolicy, this.mRefreshRatePolicyController.mMaxRefreshRateInPolicy, 2, animDuration));
    }

    public void notifyWindowAnimationStartLocked(long animDuration, SurfaceControl animationLeash) {
        if (!isSupportSmartDisplayPolicy() || animDuration <= 200) {
            return;
        }
        if (DEBUG) {
            Slog.d(TAG, "notifyWindowAnimationStart animDuration :" + animDuration);
        }
        RefreshRatePolicyController refreshRatePolicyController = this.mRefreshRatePolicyController;
        refreshRatePolicyController.notifyInteractionStart(new ComingInteraction(refreshRatePolicyController.mMinRefreshRateInPolicy, this.mRefreshRatePolicyController.mMaxRefreshRateInPolicy, 3, animDuration));
    }

    public void notifyInsetAnimationShow(int types) {
        if (!isSupportSmartDisplayPolicy()) {
            return;
        }
        if (DEBUG) {
            Slog.d(TAG, "notifyInsetAnimationShow");
        }
        if ((WindowInsets.Type.ime() & types) != 0) {
            notifyInputMethodAnimationStart(1);
        }
    }

    public void notifyInsetAnimationHide(int types) {
        if (!isSupportSmartDisplayPolicy()) {
            return;
        }
        if (DEBUG) {
            Slog.d(TAG, "notifyInsetAnimationHide");
        }
        if ((WindowInsets.Type.ime() & types) != 0) {
            notifyInputMethodAnimationStart(0);
        }
    }

    private void notifyInputMethodAnimationStart(int status) {
        if (status == 1 && this.mLastInputMethodStatus == 1) {
            return;
        }
        if (status == 1 || (status == 0 && this.mLastFocusPackage.equals(getCurrentFocusPackageName()))) {
            RefreshRatePolicyController refreshRatePolicyController = this.mRefreshRatePolicyController;
            refreshRatePolicyController.notifyInteractionStart(new ComingInteraction(refreshRatePolicyController.mMinRefreshRateInPolicy, this.mRefreshRatePolicyController.mMaxRefreshRateInPolicy, 4, 0L));
        }
        this.mLastInputMethodStatus = status;
        this.mLastFocusPackage = getCurrentFocusPackageName();
    }

    public void notifyRecentsAnimationStart(ActivityRecord targetActivity) {
        if (!isSupportSmartDisplayPolicy()) {
            return;
        }
        if (DEBUG) {
            Slog.d(TAG, "notifyRecentsAnimationStart");
        }
        RefreshRatePolicyController refreshRatePolicyController = this.mRefreshRatePolicyController;
        refreshRatePolicyController.notifyInteractionStart(new ComingInteraction(refreshRatePolicyController.mMinRefreshRateInPolicy, this.mRefreshRatePolicyController.mMaxRefreshRateInPolicy, 5, 0L));
    }

    public void notifyRecentsAnimationEnd() {
        if (!isSupportSmartDisplayPolicy()) {
            return;
        }
        if (DEBUG) {
            Slog.d(TAG, "notifyRecentsAnimationEnd");
        }
        RefreshRatePolicyController refreshRatePolicyController = this.mRefreshRatePolicyController;
        refreshRatePolicyController.notifyInteractionEnd(new ComingInteraction(refreshRatePolicyController.mMinRefreshRateInPolicy, this.mRefreshRatePolicyController.mMinRefreshRateInPolicy, 5, 0L));
    }

    public void notifyRemoteAnimationStart(RemoteAnimationTarget[] appTargets) {
        if (!isSupportSmartDisplayPolicy()) {
            return;
        }
        if (DEBUG) {
            Slog.d(TAG, "notifyRemoteAnimationStart");
        }
        RefreshRatePolicyController refreshRatePolicyController = this.mRefreshRatePolicyController;
        refreshRatePolicyController.notifyInteractionStart(new ComingInteraction(refreshRatePolicyController.mMinRefreshRateInPolicy, this.mRefreshRatePolicyController.mMaxRefreshRateInPolicy, 6, 0L));
    }

    public void notifyRemoteAnimationEnd() {
        if (!isSupportSmartDisplayPolicy()) {
            return;
        }
        if (DEBUG) {
            Slog.d(TAG, "notifyRemoteAnimationEnd");
        }
        RefreshRatePolicyController refreshRatePolicyController = this.mRefreshRatePolicyController;
        refreshRatePolicyController.notifyInteractionEnd(new ComingInteraction(refreshRatePolicyController.mMinRefreshRateInPolicy, this.mRefreshRatePolicyController.mMinRefreshRateInPolicy, 6, 0L));
    }

    public void notifyFocusedWindowChangeLocked(String oldFocusPackage, String newFocusPackage) {
        if (!isSupportSmartDisplayPolicy() || oldFocusPackage == null || newFocusPackage == null || oldFocusPackage.equals(newFocusPackage)) {
            return;
        }
        if (this.mRefreshRatePolicyController.isFocusedPackageWhiteList(newFocusPackage)) {
            if (DEBUG) {
                Slog.d(TAG, "notifyFocusedWindowChanged, new: " + newFocusPackage);
            }
            RefreshRatePolicyController refreshRatePolicyController = this.mRefreshRatePolicyController;
            refreshRatePolicyController.notifyInteractionStart(new ComingInteraction(refreshRatePolicyController.mMinRefreshRateInPolicy, this.mRefreshRatePolicyController.mMaxRefreshRateInPolicy, 7, 0L));
        } else if (this.mRefreshRatePolicyController.isFocusedPackageWhiteList(oldFocusPackage)) {
            if (DEBUG) {
                Slog.d(TAG, "notifyFocusedWindowChanged, old: " + oldFocusPackage);
            }
            RefreshRatePolicyController refreshRatePolicyController2 = this.mRefreshRatePolicyController;
            refreshRatePolicyController2.notifyInteractionEnd(new ComingInteraction(refreshRatePolicyController2.mMinRefreshRateInPolicy, this.mRefreshRatePolicyController.mMinRefreshRateInPolicy, 7, 0L));
        }
    }

    public void notifyInputEventReceived(int inputType, int durationMs) {
        if (!isSupportSmartDisplayPolicy()) {
            return;
        }
        String focusedPackage = getCurrentFocusPackageName();
        if (this.mRefreshRatePolicyController.isInputPackageWhiteList(focusedPackage)) {
            if (DEBUG) {
                Slog.d(TAG, "notifyInputEventReceived, " + interactionTypeToString(inputType) + ", durationMs: " + durationMs + ", focus: " + focusedPackage);
            }
            RefreshRatePolicyController refreshRatePolicyController = this.mRefreshRatePolicyController;
            refreshRatePolicyController.notifyInteractionStart(new ComingInteraction(refreshRatePolicyController.mMinRefreshRateInPolicy, this.mRefreshRatePolicyController.mMaxRefreshRateInPolicy, inputType, durationMs));
        }
    }

    public void notifyDisplayDeviceStateChangeLocked(int deviceState) {
        if (sEnable && deviceState != -1) {
            updateSupportDisplayModes();
            resetDisplaySize();
        }
    }

    public void notifyDisplaySwitchResolutionLocked(int width, int height, int density) {
        if (sEnable) {
            resetDisplaySize();
        }
    }

    public boolean shouldInterceptUpdateDisplayModeSpecs(int displayId, DisplayModeDirector.DesiredDisplayModeSpecs modeSpecs) {
        if (isSupportSmartDisplayPolicy() && displayId == 0) {
            return this.mRefreshRatePolicyController.shouldInterceptUpdateDisplayModeSpecs(modeSpecs);
        }
        return false;
    }

    public void shouldUpRefreshRate(ComingInteraction comingInteraction) {
        float maxUpRefreshRate = comingInteraction.mMaxRefreshRate;
        float minUpRefreshRate = comingInteraction.mMinRefreshRate;
        long interactDuration = comingInteraction.mDuration;
        int baseSfModeId = findBaseSfModeId(maxUpRefreshRate);
        if (baseSfModeId == -1 || isSwitchingResolution()) {
            this.mRefreshRatePolicyController.reset();
            if (DEBUG) {
                Slog.d(TAG, "invalid baseSfModeId or switching resolution now");
                return;
            }
            return;
        }
        realControlRefreshRate(baseSfModeId, false, minUpRefreshRate, maxUpRefreshRate, minUpRefreshRate, maxUpRefreshRate);
        Message msg = this.mHandler.obtainMessage(2);
        this.mHandler.sendMessageDelayed(msg, interactDuration);
        if (DEBUG) {
            Slog.d(TAG, "up refreshrate: " + maxUpRefreshRate);
        }
    }

    public void shouldDownRefreshRate() {
        this.mHandler.removeMessages(2);
        this.mRefreshRatePolicyController.reset();
        notifyDisplayModeSpecsChanged();
        if (DEBUG) {
            Slog.d(TAG, "down refreshrate");
        }
    }

    private void realControlRefreshRate(int baseSfModeId, boolean allowGroupSwitching, float primaryMinRefreshRate, float primaryMaxRefreshRate, float appMinRefreshRate, float appMaxRefreshRate) {
        notifySurfaceFlingerInteractionChanged(1);
        SurfaceControl.DesiredDisplayModeSpecs desired = new SurfaceControl.DesiredDisplayModeSpecs(baseSfModeId, allowGroupSwitching, primaryMinRefreshRate, primaryMaxRefreshRate, appMinRefreshRate, appMaxRefreshRate);
        DisplayManagerServiceImpl.getInstance().shouldUpdateDisplayModeSpecs(desired);
    }

    private void notifyDisplayModeSpecsChanged() {
        notifySurfaceFlingerInteractionChanged(0);
        DisplayModeDirectorImpl.getInstance().notifyDisplayModeSpecsChanged();
    }

    private void notifySurfaceFlingerInteractionChanged(int actionType) {
        if (this.mSurfaceFlinger == null) {
            return;
        }
        Parcel data = Parcel.obtain();
        data.writeInterfaceToken("android.ui.ISurfaceComposer");
        data.writeInt(MODULE_SF_INTERACTION);
        data.writeInt(actionType);
        data.writeString(SF_PERF_INTERACTION);
        try {
            try {
                this.mSurfaceFlinger.transact(TRANSACTION_SF_DISPLAY_FEATURE_IDLE, data, null, 1);
            } catch (RemoteException | SecurityException ex) {
                Slog.e(TAG, "Failed to notify idle to SurfaceFlinger", ex);
            }
        } finally {
            data.recycle();
        }
    }

    public boolean isSupportSmartDisplayPolicy() {
        if (!sEnable) {
            return false;
        }
        if (this.mSupportedDisplayModes == null) {
            updateSupportDisplayModes();
        }
        return this.mSupportedDisplayModes != null;
    }

    private String getCurrentFocusPackageName() {
        String focusedPackage = RealTimeModeControllerImpl.get().getAppPackageName();
        return focusedPackage != null ? focusedPackage : "";
    }

    private boolean isSwitchingResolution() {
        return WindowManagerServiceImpl.getInstance().isPendingSwitchResolution();
    }

    private int findBaseSfModeId(float upRefreshRate) {
        synchronized (this.mDisplayLock) {
            if (isSwitchingResolution()) {
                return -1;
            }
            if (this.mDisplayModeWidth == 0 || this.mDisplayModeHeight == 0) {
                Point displaySize = new Point();
                this.mWMS.getInitialDisplaySize(0, displaySize);
                setDisplaySize(displaySize.x, displaySize.y);
            }
            return findBaseSfModeIdLocked(upRefreshRate);
        }
    }

    private int findBaseSfModeIdLocked(float upRefreshRate) {
        SurfaceControl.DisplayMode[] displayModeArr;
        for (SurfaceControl.DisplayMode mode : this.mSupportedDisplayModes) {
            if (upRefreshRate == mode.refreshRate && this.mDisplayModeWidth == mode.width && this.mDisplayModeHeight == mode.height) {
                if (DEBUG) {
                    Slog.d(TAG, "select mode: " + mode);
                }
                return mode.id;
            }
        }
        return -1;
    }

    public void dump(PrintWriter pw, String[] args, int opti) {
        pw.println("SmartDisplayPolicyManager");
        pw.println("enable: " + isSupportSmartDisplayPolicy());
        pw.println("supportedDisplayModes: " + Arrays.toString(this.mSupportedDisplayModes));
        RefreshRatePolicyController refreshRatePolicyController = this.mRefreshRatePolicyController;
        if (refreshRatePolicyController != null) {
            refreshRatePolicyController.dump(pw, args, opti);
        }
    }

    /* loaded from: classes.dex */
    public class H extends Handler {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        H(Looper looper) {
            super(looper);
            SmartDisplayPolicyManager.this = r1;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    ComingInteraction obj = (ComingInteraction) msg.obj;
                    SmartDisplayPolicyManager.this.shouldUpRefreshRate(obj);
                    return;
                case 2:
                    SmartDisplayPolicyManager.this.shouldDownRefreshRate();
                    return;
                default:
                    return;
            }
        }
    }

    /* loaded from: classes.dex */
    public class RefreshRatePolicyController {
        private static final String DB_MIUI_REFRESH_RATE = "miui_refresh_rate";
        private static final int DEFAULT_MIN_REFRESH_RATE = 60;
        private static final long DELAY_DOWN_FOCUS_WINDOW_CHANGED_TIME = 500;
        private static final long DELAY_DOWN_RECENT_ANIMATION_TIME = 500;
        private static final long DELAY_DOWN_REMOTE_ANIMATION_TIME = 500;
        private static final long DELAY_DOWN_TIME = 500;
        private static final long INTERACT_APP_TRANSITION_DUR = 20;
        private static final long INTERACT_DEFAULT_DUR = 1000;
        private static final long INTERACT_INSET_ANIMATION_DUR = 300;
        private static final long INTERACT_RECENT_ANIMATION_DUR = 30000;
        private static final long INTERACT_REMOTE_ANIMATION_DUR = 30000;
        private static final long INTERACT_WINDOW_ANIMATION_DUR = 20;
        private static final int SOFTWARE_REFRESH_RATE = 90;
        private long mCurrentInteractDuration;
        private long mCurrentInteractEndTime;
        private int mCurrentInteractRefreshRate;
        private int mCurrentInteractType;
        private int mCurrentUserRefreshRate;
        private boolean mDefaultRefreshRateEnable;
        private final Set<String> mFocusedPackageWhiteList;
        private final Set<String> mInputPackageWhiteList;
        private final Map<Integer, Long> mInteractDelayDownTimeMap;
        private final Map<Integer, Long> mInteractDurationMap;
        private final Object mLock;
        private int mMaxRefreshRateInPolicy;
        private int mMaxRefreshRateSupport;
        private int mMinRefreshRateInPolicy;
        private int mMinRefreshRateSupport;
        private RefreshRateSavingStrategyControl mRefreshRateSavingStrategyControl;
        private final Set<Integer> mSupportRefreshRates;

        private RefreshRatePolicyController() {
            SmartDisplayPolicyManager.this = r7;
            this.mLock = new Object();
            HashSet hashSet = new HashSet();
            this.mFocusedPackageWhiteList = hashSet;
            HashSet hashSet2 = new HashSet();
            this.mInputPackageWhiteList = hashSet2;
            HashSet hashSet3 = new HashSet();
            this.mSupportRefreshRates = hashSet3;
            HashMap hashMap = new HashMap();
            this.mInteractDurationMap = hashMap;
            HashMap hashMap2 = new HashMap();
            this.mInteractDelayDownTimeMap = hashMap2;
            this.mMaxRefreshRateSupport = 60;
            this.mMinRefreshRateSupport = 60;
            this.mMaxRefreshRateInPolicy = 60;
            this.mMinRefreshRateInPolicy = 0;
            this.mDefaultRefreshRateEnable = true;
            this.mCurrentUserRefreshRate = 60;
            this.mRefreshRateSavingStrategyControl = null;
            hashSet3.add(60);
            this.mRefreshRateSavingStrategyControl = new RefreshRateSavingStrategyControl();
            hashSet.add("com.android.systemui");
            hashSet2.add("com.android.systemui");
            hashSet2.add(InputMethodManagerServiceImpl.MIUI_HOME);
            hashSet2.add("com.mi.android.globallauncher");
            hashMap.put(2, 20L);
            hashMap.put(3, 20L);
            hashMap.put(4, 300L);
            hashMap.put(5, 30000L);
            hashMap.put(6, 30000L);
            hashMap2.put(5, 500L);
            hashMap2.put(6, 500L);
            hashMap2.put(7, 500L);
        }

        public void updateControllerLocked() {
            synchronized (this.mLock) {
                resetControllerLocked();
                updateSupportRefreshRatesLocked();
                this.mRefreshRateSavingStrategyControl.updateAppropriateRefreshRate();
            }
        }

        private void resetControllerLocked() {
            this.mSupportRefreshRates.clear();
            this.mSupportRefreshRates.add(60);
            this.mMaxRefreshRateSupport = 60;
            this.mMinRefreshRateSupport = 60;
            this.mMaxRefreshRateInPolicy = 60;
            this.mMinRefreshRateInPolicy = 0;
            this.mDefaultRefreshRateEnable = true;
            this.mCurrentUserRefreshRate = 60;
        }

        private void updateSupportRefreshRatesLocked() {
            SurfaceControl.DisplayMode[] displayModeArr;
            if (SmartDisplayPolicyManager.this.mSupportedDisplayModes != null) {
                for (SurfaceControl.DisplayMode mode : SmartDisplayPolicyManager.this.mSupportedDisplayModes) {
                    int realRefreshRate = Math.round(mode.refreshRate * 10.0f) / 10;
                    mode.refreshRate = realRefreshRate;
                    this.mSupportRefreshRates.add(Integer.valueOf(realRefreshRate));
                    this.mMaxRefreshRateSupport = Math.max(this.mMaxRefreshRateSupport, realRefreshRate);
                }
            }
        }

        public boolean isInputPackageWhiteList(String packageName) {
            return this.mInputPackageWhiteList.contains(packageName);
        }

        public boolean isFocusedPackageWhiteList(String packageName) {
            return this.mFocusedPackageWhiteList.contains(packageName);
        }

        public void notifyInteractionStart(ComingInteraction comingInteraction) {
            synchronized (this.mLock) {
                if (isEnable() && !shouldInterceptAdjustRefreshRate(comingInteraction)) {
                    comingInteraction.mDuration += getDurationForInteraction(comingInteraction.mInteractType);
                    long comingInteractEndTime = System.currentTimeMillis() + comingInteraction.mDuration;
                    if (shouldInterceptComingInteractLocked(comingInteraction, comingInteractEndTime)) {
                        return;
                    }
                    updateCurrentInteractionLocked(comingInteraction, comingInteractEndTime);
                    dropCurrentInteraction();
                    Message msg = SmartDisplayPolicyManager.this.mHandler.obtainMessage(1, comingInteraction);
                    SmartDisplayPolicyManager.this.mHandler.sendMessage(msg);
                }
            }
        }

        public void notifyInteractionEnd(ComingInteraction comingInteraction) {
            synchronized (this.mLock) {
                if (!isEnable()) {
                    return;
                }
                long delayTime = getDelayDownTimeForInteraction(comingInteraction.mInteractType);
                comingInteraction.mDuration = delayTime;
                updateCurrentInteractionLocked(comingInteraction, System.currentTimeMillis() + delayTime);
                Message msg = SmartDisplayPolicyManager.this.mHandler.obtainMessage(2);
                SmartDisplayPolicyManager.this.mHandler.sendMessageDelayed(msg, delayTime);
            }
        }

        private boolean shouldInterceptComingInteractLocked(ComingInteraction comingInteraction, long comingInteractEndTime) {
            if (!isAniminting()) {
                return false;
            }
            if (comingInteractEndTime <= this.mCurrentInteractEndTime) {
                return true;
            }
            updateCurrentInteractionLocked(comingInteraction, comingInteractEndTime);
            SmartDisplayPolicyManager.this.mHandler.removeMessages(2);
            SmartDisplayPolicyManager.this.mHandler.sendEmptyMessageDelayed(2, comingInteraction.mDuration);
            return true;
        }

        private boolean shouldInterceptAdjustRefreshRate(ComingInteraction comingInteraction) {
            return this.mMaxRefreshRateSupport > SOFTWARE_REFRESH_RATE && getCurrentMiuiRefreshRate() == SOFTWARE_REFRESH_RATE && comingInteraction.mInteractType != 5 && comingInteraction.mInteractType != 6;
        }

        private int getCurrentMiuiRefreshRate() {
            return Settings.Secure.getInt(SmartDisplayPolicyManager.this.mContext.getContentResolver(), DB_MIUI_REFRESH_RATE, 0);
        }

        public boolean shouldInterceptUpdateDisplayModeSpecs(DisplayModeDirector.DesiredDisplayModeSpecs modeSpecs) {
            boolean interceptOrNot = false;
            if (!isAniminting()) {
                return false;
            }
            float maxRefreshRate = Math.max(modeSpecs.primaryRefreshRateRange.max, modeSpecs.appRequestRefreshRateRange.max);
            if (maxRefreshRate < this.mMaxRefreshRateInPolicy) {
                interceptOrNot = true;
            }
            if (SmartDisplayPolicyManager.DEBUG) {
                Slog.d(SmartDisplayPolicyManager.TAG, "intercept update mode specs: " + interceptOrNot);
            }
            return interceptOrNot;
        }

        private void updateCurrentInteractionLocked(ComingInteraction comingInteraction, long comingInteractEndTime) {
            this.mCurrentInteractRefreshRate = comingInteraction.mMaxRefreshRate;
            this.mCurrentInteractType = comingInteraction.mInteractType;
            this.mCurrentInteractDuration = comingInteraction.mDuration;
            this.mCurrentInteractEndTime = comingInteractEndTime;
        }

        private boolean isAniminting() {
            return this.mCurrentInteractEndTime != 0;
        }

        private long getDurationForInteraction(int interactType) {
            Long interactDuration = this.mInteractDurationMap.get(Integer.valueOf(interactType));
            return interactDuration != null ? interactDuration.longValue() : INTERACT_DEFAULT_DUR;
        }

        private long getDelayDownTimeForInteraction(int interactType) {
            Long interactDelayDownTime = this.mInteractDelayDownTimeMap.get(Integer.valueOf(interactType));
            if (interactDelayDownTime != null) {
                return interactDelayDownTime.longValue();
            }
            return 0L;
        }

        private void dropCurrentInteraction() {
            SmartDisplayPolicyManager.this.mHandler.removeMessages(1);
            SmartDisplayPolicyManager.this.mHandler.removeMessages(2);
        }

        public void notifyThermalRefreshRateChanged(int thermalRefreshRate) {
            synchronized (this.mLock) {
                if (thermalRefreshRate >= 60) {
                    this.mMaxRefreshRateInPolicy = thermalRefreshRate;
                    reset();
                    dropCurrentInteraction();
                } else {
                    this.mRefreshRateSavingStrategyControl.updateAppropriateRefreshRate();
                }
                if (SmartDisplayPolicyManager.DEBUG) {
                    Slog.d(SmartDisplayPolicyManager.TAG, "thermalRefreshRate changed: " + thermalRefreshRate);
                }
            }
        }

        public void reset() {
            synchronized (this.mLock) {
                this.mCurrentInteractRefreshRate = 0;
                this.mCurrentInteractType = 0;
                this.mCurrentInteractDuration = 0L;
                this.mCurrentInteractEndTime = 0L;
            }
        }

        private boolean isEnable() {
            return SmartDisplayPolicyManager.sEnable && this.mMaxRefreshRateInPolicy != this.mMinRefreshRateSupport;
        }

        public void dump(PrintWriter pw, String[] args, int opti) {
            pw.println("RefreshRatePolicyController");
            pw.println("status: " + isEnable());
            pw.println("refresh rate support: " + this.mSupportRefreshRates.toString());
            pw.println("default refresh rate: " + this.mDefaultRefreshRateEnable);
            pw.println("user refresh rate: " + this.mCurrentUserRefreshRate);
            pw.println("max refresh rate in policy: " + this.mMaxRefreshRateInPolicy);
            pw.println("min refresh rate in policy: " + this.mMinRefreshRateInPolicy);
            pw.println("current type: " + SmartDisplayPolicyManager.interactionTypeToString(this.mCurrentInteractType));
            pw.println("current interact refresh rate: " + this.mCurrentInteractRefreshRate);
            pw.println("current interact duration: " + this.mCurrentInteractDuration);
            pw.println("mCurrentInteractEndTime: " + this.mCurrentInteractEndTime);
        }

        /* loaded from: classes.dex */
        public class RefreshRateSavingStrategyControl {
            private static final String DB_DEFAULT_REFRESH_RATE = "is_smart_fps";
            private static final String DB_THERMAL_REFRESH_RATE = "thermal_limit_refresh_rate";
            private static final String DB_USER_REFRESH_RATE = "user_refresh_rate";
            private final Uri DEFAULT_URI;
            private final Uri THERMAL_URI;
            private final Uri USER_URI;

            RefreshRateSavingStrategyControl() {
                RefreshRatePolicyController.this = r7;
                Uri uriFor = Settings.System.getUriFor(DB_DEFAULT_REFRESH_RATE);
                this.DEFAULT_URI = uriFor;
                Uri uriFor2 = Settings.Secure.getUriFor("user_refresh_rate");
                this.USER_URI = uriFor2;
                Uri uriFor3 = Settings.System.getUriFor(DB_THERMAL_REFRESH_RATE);
                this.THERMAL_URI = uriFor3;
                ContentResolver cr = SmartDisplayPolicyManager.this.mContext.getContentResolverForUser(UserHandle.SYSTEM);
                cr.registerContentObserver(uriFor, true, new PolicyChangeObserver(SmartDisplayPolicyManager.this.mHandler));
                cr.registerContentObserver(uriFor2, true, new PolicyChangeObserver(SmartDisplayPolicyManager.this.mHandler));
                cr.registerContentObserver(uriFor3, true, new PolicyChangeObserver(SmartDisplayPolicyManager.this.mHandler));
            }

            /* JADX INFO: Access modifiers changed from: private */
            /* loaded from: classes.dex */
            public class PolicyChangeObserver extends ContentObserver {
                /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
                public PolicyChangeObserver(Handler handler) {
                    super(handler);
                    RefreshRateSavingStrategyControl.this = r1;
                }

                @Override // android.database.ContentObserver
                public void onChange(boolean selfChange, Uri uri) {
                    super.onChange(selfChange, uri);
                    if (!SmartDisplayPolicyManager.this.isSupportSmartDisplayPolicy()) {
                        return;
                    }
                    if (uri.equals(RefreshRateSavingStrategyControl.this.THERMAL_URI)) {
                        int thermalRefreshRate = Settings.System.getInt(SmartDisplayPolicyManager.this.mContext.getContentResolver(), RefreshRateSavingStrategyControl.DB_THERMAL_REFRESH_RATE, 0);
                        RefreshRatePolicyController.this.notifyThermalRefreshRateChanged(thermalRefreshRate);
                        return;
                    }
                    RefreshRateSavingStrategyControl.this.updateAppropriateRefreshRate();
                }
            }

            public void updateAppropriateRefreshRate() {
                synchronized (RefreshRatePolicyController.this.mLock) {
                    RefreshRatePolicyController refreshRatePolicyController = RefreshRatePolicyController.this;
                    boolean z = true;
                    if (Settings.System.getInt(SmartDisplayPolicyManager.this.mContext.getContentResolver(), DB_DEFAULT_REFRESH_RATE, 0) != 1) {
                        z = false;
                    }
                    refreshRatePolicyController.mDefaultRefreshRateEnable = z;
                    RefreshRatePolicyController refreshRatePolicyController2 = RefreshRatePolicyController.this;
                    refreshRatePolicyController2.mCurrentUserRefreshRate = Settings.Secure.getInt(SmartDisplayPolicyManager.this.mContext.getContentResolver(), "user_refresh_rate", 0);
                    if (RefreshRatePolicyController.this.mDefaultRefreshRateEnable) {
                        RefreshRatePolicyController refreshRatePolicyController3 = RefreshRatePolicyController.this;
                        refreshRatePolicyController3.mMaxRefreshRateInPolicy = ((Integer) refreshRatePolicyController3.mSupportRefreshRates.stream().max(new Comparator() { // from class: com.miui.server.smartpower.SmartDisplayPolicyManager$RefreshRatePolicyController$RefreshRateSavingStrategyControl$$ExternalSyntheticLambda0
                            @Override // java.util.Comparator
                            public final int compare(Object obj, Object obj2) {
                                return ((Integer) obj).compareTo((Integer) obj2);
                            }
                        }).get()).intValue();
                    } else if (RefreshRatePolicyController.this.mSupportRefreshRates.contains(Integer.valueOf(RefreshRatePolicyController.this.mCurrentUserRefreshRate))) {
                        RefreshRatePolicyController refreshRatePolicyController4 = RefreshRatePolicyController.this;
                        refreshRatePolicyController4.mMaxRefreshRateInPolicy = refreshRatePolicyController4.mCurrentUserRefreshRate;
                    } else {
                        Slog.d(SmartDisplayPolicyManager.TAG, "user choice unkown refresh rate");
                    }
                    if (SmartDisplayPolicyManager.DEBUG) {
                        Slog.d(SmartDisplayPolicyManager.TAG, "default enable: " + RefreshRatePolicyController.this.mDefaultRefreshRateEnable + ", refresh rate: " + RefreshRatePolicyController.this.mMaxRefreshRateInPolicy);
                    }
                }
            }
        }
    }

    /* loaded from: classes.dex */
    public static class ComingInteraction {
        private long mDuration;
        private int mInteractType;
        private int mMaxRefreshRate;
        private int mMinRefreshRate;

        private ComingInteraction(int mMinRefreshRate, int mMaxRefreshRate, int mInteractType, long mDuration) {
            this.mMinRefreshRate = mMinRefreshRate;
            this.mMaxRefreshRate = mMaxRefreshRate;
            this.mInteractType = mInteractType;
            this.mDuration = mDuration;
        }
    }
}
