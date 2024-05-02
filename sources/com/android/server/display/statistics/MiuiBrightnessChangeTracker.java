package com.android.server.display.statistics;

import android.app.ActivityTaskManager;
import android.app.AlarmManager;
import android.app.IActivityTaskManager;
import android.app.TaskStackListener;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Parcelable;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.MathUtils;
import android.util.Slog;
import android.util.SparseArray;
import android.util.Spline;
import android.util.TimeUtils;
import android.view.IRotationWatcher;
import android.view.MotionEvent;
import android.view.WindowManagerPolicyConstants;
import com.android.internal.display.BrightnessSynchronizer;
import com.android.internal.os.BackgroundThread;
import com.android.internal.os.SomeArgs;
import com.android.server.LocalServices;
import com.android.server.am.ProcessStarter;
import com.android.server.display.AutomaticBrightnessControllerImpl;
import com.android.server.display.statistics.BrightnessEvent;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.wm.MiuiFreeformPinManagerService;
import com.android.server.wm.ScreenRotationAnimationImpl;
import com.android.server.wm.WindowManagerService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import miui.os.DeviceFeature;
/* loaded from: classes.dex */
public class MiuiBrightnessChangeTracker {
    private static final int BRIGHTNESS_LOW;
    private static final int BRIGHTNESS_MEDIUM;
    private static final int DEBOUNCE_REPORT_BRIGHTNESS = 5000;
    private static final boolean DEBUG;
    private static final int FACTOR_DARK_MODE = 4;
    private static final int FACTOR_DC_MODE = 2;
    private static final int FACTOR_READING_MODE = 1;
    private static final int HBM_MAX_BRIGHTNESS;
    private static final int HBM_MAX_NIT;
    private static final float HBM_MIN_SCREEN_NIT;
    private static final boolean IS_12_BIT_BRIGHTNESS;
    private static final int LUX_SPLIT_HIGH = 3500;
    private static final int LUX_SPLIT_LOW = 30;
    private static final int LUX_SPLIT_MEDIUM = 900;
    private static final int LUX_SPLIT_SUPREME_FIVE = 100000;
    private static final int LUX_SPLIT_SUPREME_FOUR = 65000;
    private static final int LUX_SPLIT_SUPREME_ONE = 10000;
    private static final int LUX_SPLIT_SUPREME_THREE = 35000;
    private static final int LUX_SPLIT_SUPREME_TWO = 15000;
    private static final float MAX_SCREEN_NIT;
    private static final int MAX_SPAN_INDEX = 44;
    private static final float MIN_SCREEN_NIT;
    private static final int MSG_BRIGHTNESS_REPORT_DEBOUNCE = 3;
    private static final int MSG_HANDLE_EVENT_CHANGED = 1;
    private static final int MSG_RESET_BRIGHTNESS_ANIMATION_INFO = 12;
    private static final int MSG_ROTATION_CHANGED = 6;
    private static final int MSG_UPDATE_BRIGHTNESS_ANIMATION_DURATION = 10;
    private static final int MSG_UPDATE_BRIGHTNESS_ANIMATION_INFO = 8;
    private static final int MSG_UPDATE_BRIGHTNESS_ANIMATION_TARGET = 9;
    private static final int MSG_UPDATE_BRIGHTNESS_STATISTICS_DATA = 7;
    private static final int MSG_UPDATE_FOREGROUND_APP = 5;
    private static final int MSG_UPDATE_MOTION_EVENT = 2;
    private static final int MSG_UPDATE_SCREEN_STATE = 4;
    private static final int MSG_UPDATE_TEMPORARY_BRIGHTNESS_TIME_STAMP = 11;
    private static final int NORMAL_SCREEN_BRIGHTNESS;
    private static final String REASON_UPDATE_BRIGHTNESS_USAGE_REPORT = "report";
    private static final String REASON_UPDATE_BRIGHTNESS_USAGE_SCREEN_OFF = "screen off";
    private static final String REASON_UPDATE_BRIGHTNESS_USAGE_SPAN_CHANGE = "brightness span change";
    private static final float SCREEN_NIT_SPAN_FIVE = 500.0f;
    private static final float SCREEN_NIT_SPAN_FOUR = 80.0f;
    private static final float SCREEN_NIT_SPAN_ONE = 3.5f;
    private static final float SCREEN_NIT_SPAN_SIX = 1000.0f;
    private static final float SCREEN_NIT_SPAN_THREE = 50.0f;
    private static final float SCREEN_NIT_SPAN_TWO = 8.0f;
    private static final int SPAN_BRIGHTNESS_HIGH_STEP;
    private static final int SPAN_BRIGHTNESS_LOW_STEP;
    private static final int SPAN_LUX_STEP_HIGH = 200;
    private static final int SPAN_LUX_STEP_LOW = 5;
    private static final int SPAN_LUX_STEP_MEDIUM = 100;
    private static final int SPAN_LUX_STEP_SUPREME = 500;
    private static final float SPAN_SCREEN_NIT_STEP_FOUR = 50.0f;
    private static final float SPAN_SCREEN_NIT_STEP_ONE = 7.0f;
    private static final float SPAN_SCREEN_NIT_STEP_THREE = 20.0f;
    private static final float SPAN_SCREEN_NIT_STEP_TWO = 10.0f;
    protected static final String TAG = "MiuiBrightnessChangeTracker";
    private AlarmManager mAlarmManager;
    private float mAutoBrightnessDuration;
    private boolean mAutoBrightnessEnable;
    private float mAutoBrightnessIntegral;
    private boolean mAutoBrightnessModeChanged;
    private boolean mBrightnessAnimStart;
    private long mBrightnessAnimStartTime;
    private AutomaticBrightnessControllerImpl.CloudControllerListener mCloudControllerListener;
    private ContentResolver mContentResolver;
    private Context mContext;
    private float mCurrentBrightnessAnimValue;
    private String mForegroundAppPackageName;
    private float mGrayScale;
    private boolean mHaveValidMotionForWindowBrightness;
    private boolean mHaveValidWindowBrightness;
    private int mInterruptBrightnessAnimationTimes;
    private boolean mIsTemporaryBrightnessAdjustment;
    private boolean mIsValidResetAutoBrightnessMode;
    private boolean mLastAutoBrightnessEnable;
    private BrightnessChangeItem mLastBrightnessChangeItem;
    private int mLastBrightnessOverrideFromWindow;
    private long mLastResetBrightnessModeTime;
    private int mLastScreenBrightness;
    private long mLastScreenOnTimeStamp;
    private long mLatestDraggingChangedTime;
    private float mManualBrightnessDuration;
    private float mManualBrightnessIntegral;
    private int mOrientation;
    private boolean mPendingAnimationStart;
    private BrightnessChangeItem mPendingBrightnessChangeItem;
    private float mPendingTargetBrightnessAnimValue;
    private WindowOverlayEventListener mPointerEventListener;
    private boolean mPointerEventListenerEnabled;
    private PowerManager mPowerManager;
    private boolean mRotationListenerEnabled;
    private RotationWatcher mRotationWatcher;
    private int mScreenMaximumBrightness;
    private boolean mScreenOn;
    private SettingsObserver mSettingsObserver;
    private long mStartTimeStamp;
    private SwitchStatsHelper mSwitchStatsHelper;
    private float mTargetBrightnessAnimValue;
    private TaskStackListenerImpl mTaskStackListener;
    private long mTemporaryBrightnessTimeStamp;
    private boolean mUserDragging;
    private int mUserResetBrightnessModeTimes;
    private boolean mWindowOverrideBrightnessChanging;
    private int mCurrentUserId = ScreenRotationAnimationImpl.BLACK_SURFACE_INVALID_POSITION;
    private Map<Integer, Long> mBrightnessCumulativeUsages = new HashMap();
    private float mLastStoreBrightness = Float.NaN;
    private float mLastAutoBrightness = Float.NaN;
    private float mLastManualBrightness = Float.NaN;
    private SparseArray<Float> mAverageBrightnessArray = new SparseArray<>();
    private final long DEBUG_REPORT_TIME_DURATION = ProcessStarter.REDUCE_DIED_PROC_COUNT_DELAY;
    private int mBrightnessChangedState = -1;
    private int mPendingBrightnessChangedState = -1;
    private float mLongTermModelSplineError = -1.0f;
    private float mDefaultSplineError = -1.0f;
    private final AlarmManager.OnAlarmListener mOnAlarmListener = new AlarmManager.OnAlarmListener() { // from class: com.android.server.display.statistics.MiuiBrightnessChangeTracker.1
        @Override // android.app.AlarmManager.OnAlarmListener
        public void onAlarm() {
            MiuiBrightnessChangeTracker.this.reportScheduleEvent();
            MiuiBrightnessChangeTracker.this.setReportScheduleEventAlarm(false);
        }
    };
    private final WindowManagerPolicy mPolicy = (WindowManagerPolicy) LocalServices.getService(WindowManagerPolicy.class);
    private Handler mBackgroundHandler = new BrightnessChangeHandler(BackgroundThread.getHandler().getLooper());
    private IActivityTaskManager mActivityTaskManager = ActivityTaskManager.getService();
    private WindowManagerService mWms = ServiceManager.getService("window");

    static {
        boolean z = false;
        DEBUG = SystemProperties.getInt("debug.miui.display.dgb", 0) != 0;
        int integer = Resources.getSystem().getInteger(17694941);
        NORMAL_SCREEN_BRIGHTNESS = integer;
        float parseFloat = Float.parseFloat(SystemProperties.get("persist.vendor.max.brightness", "0"));
        MAX_SCREEN_NIT = parseFloat;
        MIN_SCREEN_NIT = parseFloat / integer;
        int i = 1 << DeviceFeature.HBMBACKLIGHT_BIT;
        HBM_MAX_BRIGHTNESS = i;
        int integer2 = Resources.getSystem().getInteger(285933609);
        HBM_MAX_NIT = integer2;
        HBM_MIN_SCREEN_NIT = integer2 / i;
        if (DeviceFeature.BACKLIGHT_BIT > 11) {
            z = true;
        }
        IS_12_BIT_BRIGHTNESS = z;
        BRIGHTNESS_LOW = z ? 40 : 20;
        BRIGHTNESS_MEDIUM = z ? 640 : 320;
        SPAN_BRIGHTNESS_LOW_STEP = z ? 60 : 30;
        SPAN_BRIGHTNESS_HIGH_STEP = z ? 175 : 90;
    }

    public MiuiBrightnessChangeTracker(Context context) {
        this.mContext = context;
        this.mPowerManager = (PowerManager) context.getSystemService("power");
        this.mAlarmManager = (AlarmManager) context.getSystemService("alarm");
        this.mBackgroundHandler.post(new Runnable() { // from class: com.android.server.display.statistics.MiuiBrightnessChangeTracker$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                MiuiBrightnessChangeTracker.this.m746x6a28a33();
            }
        });
    }

    /* renamed from: start */
    public void m746x6a28a33() {
        this.mPointerEventListener = new WindowOverlayEventListener();
        this.mSwitchStatsHelper = SwitchStatsHelper.getInstance(this.mContext);
        this.mTaskStackListener = new TaskStackListenerImpl();
        this.mRotationWatcher = new RotationWatcher();
        this.mScreenMaximumBrightness = this.mPowerManager.getMaximumScreenBrightnessSetting();
        this.mStartTimeStamp = SystemClock.elapsedRealtime();
        setRotationListener(true);
        setPointerEventListener(true);
        registerForegroundAppUpdater();
        registerScreenStateReceiver();
        setReportScheduleEventAlarm(true);
        this.mSettingsObserver = new SettingsObserver(this.mBackgroundHandler);
        this.mContentResolver = this.mContext.getContentResolver();
        registerSettingsObserver();
    }

    public void setReportScheduleEventAlarm(boolean init) {
        long duration;
        long now = SystemClock.elapsedRealtime();
        boolean z = DEBUG;
        if (z) {
            duration = ProcessStarter.REDUCE_DIED_PROC_COUNT_DELAY;
        } else {
            duration = init ? 43200000L : 86400000L;
        }
        long nextTime = now + duration;
        if (z) {
            Slog.d(TAG, "setReportSwitchStatAlarm: next time: " + TimeUtils.formatDuration(duration));
        }
        this.mAlarmManager.setExact(2, nextTime, "report_switch_stats", this.mOnAlarmListener, this.mBackgroundHandler);
    }

    public void reportScheduleEvent() {
        reportSwitchStatsEvent();
        reportBrightnessUsageDuration();
        reportExtraData();
        reportAdvancedEvent();
    }

    public void notifyBrightnessEventIfNeeded(boolean screenOn, float brightness, boolean userInitiatedChange, boolean useAutoBrightness, float brightnessOverrideFromWindow, boolean lowPowerMode, float ambientLux, float userDataPoint, boolean defaultConfig, Spline brightnessSpline, boolean sunlightActive) {
        int type = getBrightnessType(userInitiatedChange, useAutoBrightness, brightnessOverrideFromWindow, sunlightActive);
        int brightnessInt = BrightnessSynchronizer.brightnessFloatToInt(brightness);
        scheduleUpdateBrightnessStatisticsData(screenOn, brightnessInt);
        BrightnessChangeItem brightnessChangeItem = this.mLastBrightnessChangeItem;
        if (brightnessChangeItem == null || brightnessChangeItem.brightness != brightnessInt) {
            if (this.mPolicy.isKeyguardShowingAndNotOccluded() || this.mPolicy.isKeyguardShowing()) {
                return;
            }
            if (screenOn) {
                updateInterruptBrightnessAnimDurationIfNeeded(type, brightness);
                BrightnessChangeItem item = new BrightnessChangeItem(brightnessInt, userInitiatedChange, useAutoBrightness, brightnessOverrideFromWindow, lowPowerMode, sunlightActive, ambientLux, userDataPoint, defaultConfig, brightnessSpline, type);
                this.mLastBrightnessChangeItem = item;
                Message msg = Message.obtain(this.mBackgroundHandler, 1, item);
                msg.sendToTarget();
            }
        }
    }

    public void handleBrightnessChangeEvent(BrightnessChangeItem item) {
        boolean windowOverrideApplying = !Float.isNaN(item.brightnessOverrideFromWindow);
        boolean windowOverrideChanging = windowOverrideApplying && item.brightnessOverrideFromWindow != ((float) this.mLastBrightnessOverrideFromWindow);
        if (windowOverrideChanging != this.mWindowOverrideBrightnessChanging) {
            this.mWindowOverrideBrightnessChanging = windowOverrideChanging;
        }
        if (windowOverrideChanging) {
            long now = SystemClock.elapsedRealtime();
            if (this.mUserDragging || (this.mHaveValidMotionForWindowBrightness && now - this.mLatestDraggingChangedTime < 50)) {
                this.mLastBrightnessOverrideFromWindow = item.brightness;
                this.mHaveValidWindowBrightness = true;
                if (DEBUG) {
                    Slog.d(TAG, "Brightness from window is changing: " + item.brightness);
                }
            }
        }
        if (windowOverrideApplying && !this.mHaveValidWindowBrightness) {
            return;
        }
        this.mPendingBrightnessChangeItem = item;
        debounceBrightnessEvent(5000L);
    }

    private void debounceBrightnessEvent(long debounceTime) {
        this.mBackgroundHandler.removeMessages(3);
        Message msg = this.mBackgroundHandler.obtainMessage(3);
        this.mBackgroundHandler.sendMessageDelayed(msg, debounceTime);
    }

    private void setPointerEventListener(boolean enable) {
        WindowManagerService windowManagerService = this.mWms;
        if (windowManagerService == null) {
            return;
        }
        if (enable) {
            if (!this.mPointerEventListenerEnabled) {
                windowManagerService.registerPointerEventListener(this.mPointerEventListener, 0);
                this.mPointerEventListenerEnabled = true;
                if (DEBUG) {
                    Slog.d(TAG, "register pointer event listener.");
                }
            }
        } else if (this.mPointerEventListenerEnabled) {
            windowManagerService.unregisterPointerEventListener(this.mPointerEventListener, 0);
            this.mPointerEventListenerEnabled = false;
            if (DEBUG) {
                Slog.d(TAG, "unregister pointer event listener.");
            }
        }
    }

    private void setRotationListener(boolean enable) {
        if (enable) {
            if (!this.mRotationListenerEnabled) {
                this.mRotationListenerEnabled = true;
                this.mWms.watchRotation(this.mRotationWatcher, 0);
                if (DEBUG) {
                    Slog.d(TAG, "register rotation listener.");
                }
            }
        } else if (this.mRotationListenerEnabled) {
            this.mRotationListenerEnabled = false;
            this.mWms.removeRotationWatcher(this.mRotationWatcher);
            if (DEBUG) {
                Slog.d(TAG, "unregister rotation listener.");
            }
        }
    }

    public void readyToReportEvent() {
        BrightnessChangeItem brightnessChangeItem = this.mPendingBrightnessChangeItem;
        if (brightnessChangeItem != null) {
            if (this.mHaveValidWindowBrightness && this.mUserDragging) {
                return;
            }
            reportBrightnessEvent(brightnessChangeItem);
            resetPendingParams();
        }
    }

    private void reportBrightnessEvent(BrightnessChangeItem item) {
        if (DEBUG) {
            Slog.d(TAG, "brightness changed, let's make a recode: " + item.toString() + ", foregroundApps: " + this.mForegroundAppPackageName);
        }
        long now = System.currentTimeMillis();
        int curSpanIndex = getBrightnessSpanByNit(item.brightness);
        int preSpanIndex = getBrightnessSpanByNit(this.mLastScreenBrightness);
        BrightnessEvent event = new BrightnessEvent();
        event.setEventType(item.type).setCurBrightnessSpanIndex(curSpanIndex).setPreBrightnessSpanIndex(preSpanIndex);
        if (item.type != 1 || this.mCloudControllerListener.isAutoBrightnessStatisticsEventEnable()) {
            event.setScreenBrightness(item.brightness).setPreviousBrightness(this.mLastScreenBrightness).setAmbientLux(item.ambientLux).setUserDataPoint(item.userDataPoint).setForegroundPackage(this.mForegroundAppPackageName).setLowPowerModeFlag(item.lowPowerMode).setIsDefaultConfig(item.defaultConfig).setSpline(splitSpline(item.brightnessSpline)).setAffectFactorFlag(convergeAffectFactors()).setLuxSpanIndex(getAmbientLuxSpanIndex(item.ambientLux)).setTimeStamp(now).setOrientation(this.mOrientation).setDisplayGrayScale(this.mGrayScale).setUserId(this.mCurrentUserId);
        }
        reportEventToMQS(event, "brightness");
        updateBrightnessUsageIfNeeded(curSpanIndex, preSpanIndex);
    }

    public void updateGrayScale(float grayScale) {
        this.mGrayScale = grayScale;
    }

    private void reportSwitchStatsEvent() {
        List<BrightnessEvent.SwitchStatEntry> allSwitchStats = this.mSwitchStatsHelper.getAllSwitchStats();
        if (DEBUG) {
            Slog.d(TAG, "reportSwitchStatsEvent: allSwitchStats:" + allSwitchStats);
        }
        BrightnessEvent event = new BrightnessEvent();
        event.setEventType(5).setSwitchStats(allSwitchStats).setTimeStamp(System.currentTimeMillis());
        reportEventToMQS(event, "brightness");
    }

    private void reportBrightnessUsageDuration() {
        updateBrightnessUsage(getBrightnessSpanByNit(this.mLastScreenBrightness), false, REASON_UPDATE_BRIGHTNESS_USAGE_REPORT);
        if (DEBUG) {
            Slog.d(TAG, "reportBrightnessUsageDuration: mBrightnessUsageMap: " + this.mBrightnessCumulativeUsages);
        }
        if (!this.mBrightnessCumulativeUsages.isEmpty()) {
            BrightnessEvent event = new BrightnessEvent();
            event.setEventType(6).setBrightnessUsageMap(this.mBrightnessCumulativeUsages).setTimeStamp(System.currentTimeMillis());
            reportEventToMQS(event, "brightness");
            this.mBrightnessCumulativeUsages.clear();
        }
    }

    private void reportEventToMQS(Parcelable parcelable, String eventName) {
        OneTrackUploaderHelper.reportToOneTrack(this.mContext, parcelable, eventName);
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            if ("brightness".equals(eventName)) {
                BrightnessEvent event = (BrightnessEvent) parcelable;
                sb.append(event.toString());
            } else if (OneTrackUploaderHelper.ADVANCED_EVENT_NAME.equals(eventName)) {
                AdvancedEvent event2 = (AdvancedEvent) parcelable;
                sb.append(event2.convertToString());
            }
            Slog.d(TAG, "reportEventToMQS: eventName:" + eventName + ", event:" + ((Object) sb));
        }
    }

    private void resetPendingParams() {
        this.mLastScreenBrightness = this.mPendingBrightnessChangeItem.brightness;
        this.mPendingBrightnessChangeItem = null;
        resetWindowOverrideParams();
    }

    private void resetWindowOverrideParams() {
        this.mHaveValidWindowBrightness = false;
        this.mLastBrightnessOverrideFromWindow = -1;
        this.mWindowOverrideBrightnessChanging = false;
    }

    private void registerForegroundAppUpdater() {
        try {
            this.mActivityTaskManager.registerTaskStackListener(this.mTaskStackListener);
            updateForegroundApps();
        } catch (RemoteException e) {
            if (DEBUG) {
                Slog.e(TAG, "Failed to register foreground app updater: " + e);
            }
        }
    }

    public void updateForegroundApps() {
        try {
            ActivityTaskManager.RootTaskInfo info = this.mActivityTaskManager.getFocusedRootTaskInfo();
            if (info != null && info.topActivity != null && info.getWindowingMode() != 5 && info.getWindowingMode() != 3 && info.getWindowingMode() != 4) {
                String packageName = info.topActivity.getPackageName();
                String str = this.mForegroundAppPackageName;
                if (str != null && str.equals(packageName)) {
                    return;
                }
                this.mCurrentUserId = info.userId;
                this.mForegroundAppPackageName = packageName;
            }
        } catch (RemoteException e) {
        }
    }

    private void registerScreenStateReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.SCREEN_ON");
        filter.addAction("android.intent.action.SCREEN_OFF");
        filter.setPriority(1000);
        this.mContext.registerReceiver(new ScreenStateReceiver(), filter);
    }

    public void updatePointerEventMotionState(boolean dragging, int distanceX, int distanceY) {
        if (this.mUserDragging != dragging) {
            this.mUserDragging = dragging;
            if (!dragging && checkIsValidMotionForWindowBrightness(distanceX, distanceY)) {
                this.mLatestDraggingChangedTime = SystemClock.elapsedRealtime();
                if (this.mHaveValidWindowBrightness) {
                    debounceBrightnessEvent(5000L);
                }
            }
        }
    }

    private boolean checkIsValidMotionForWindowBrightness(int distanceX, int distanceY) {
        boolean isValid = true;
        if (distanceY <= 10 || (distanceX != 0 && distanceY / distanceX < 2)) {
            isValid = false;
            if (DEBUG) {
                Slog.d(TAG, "checkIsValidMotionForWindowBrightness: invalid and return.");
            }
        }
        this.mHaveValidMotionForWindowBrightness = isValid;
        return isValid;
    }

    private void updateBrightnessUsageIfNeeded(int curSpanIndex, int preSpanIndex) {
        if (curSpanIndex != preSpanIndex && isValidStartTimeStamp()) {
            updateBrightnessUsage(preSpanIndex, false, REASON_UPDATE_BRIGHTNESS_USAGE_SPAN_CHANGE);
        }
    }

    private void updateBrightnessUsage(int spanIndex, boolean dueToScreenOff, String reason) {
        if (isValidStartTimeStamp()) {
            long now = SystemClock.elapsedRealtime();
            long duration = now - this.mStartTimeStamp;
            long cumulativeDuration = duration;
            if (this.mBrightnessCumulativeUsages.containsKey(Integer.valueOf(spanIndex))) {
                cumulativeDuration += this.mBrightnessCumulativeUsages.get(Integer.valueOf(spanIndex)).longValue();
            }
            this.mBrightnessCumulativeUsages.put(Integer.valueOf(spanIndex), Long.valueOf(cumulativeDuration));
            if (dueToScreenOff) {
                this.mStartTimeStamp = 0L;
            } else {
                this.mStartTimeStamp = now;
            }
            if (DEBUG) {
                Slog.d(TAG, "updateBrightnessUsage: reason: " + reason + ", span: " + spanIndex + ", append duration: " + TimeUtils.formatDuration(duration) + ", all durations: " + TimeUtils.formatDuration(cumulativeDuration));
            }
        }
    }

    public void updateScreenStateChanged(boolean screenOn) {
        updateStartTimeStamp(screenOn);
        setPointerEventListener(screenOn);
        setRotationListener(screenOn);
    }

    private void updateStartTimeStamp(boolean screenOn) {
        if (DEBUG) {
            Slog.d(TAG, "updateStartTimeStamp: screenOn: " + screenOn);
        }
        if (screenOn && !isValidStartTimeStamp()) {
            this.mStartTimeStamp = SystemClock.elapsedRealtime();
        } else if (!screenOn) {
            updateBrightnessUsage(getBrightnessSpanByNit(this.mLastScreenBrightness), true, REASON_UPDATE_BRIGHTNESS_USAGE_SCREEN_OFF);
        }
    }

    private int getBrightnessType(boolean userInitiatedChange, boolean useAutoBrightness, float brightnessOverrideFromWindow, boolean sunlightActive) {
        if (!Float.isNaN(brightnessOverrideFromWindow)) {
            return 3;
        }
        if (useAutoBrightness && userInitiatedChange) {
            return 2;
        }
        if (useAutoBrightness) {
            return 1;
        }
        if (!sunlightActive) {
            return 0;
        }
        return 4;
    }

    private boolean isValidStartTimeStamp() {
        return this.mStartTimeStamp > 0;
    }

    private int getAmbientLuxSpanIndex(float lux) {
        int index;
        if (lux < 30.0f) {
            index = (int) (lux / 5.0f);
        } else if (lux < 900.0f) {
            index = (int) ((lux / 100.0f) + 6.0f);
        } else if (lux < 3500.0f) {
            index = (int) (((lux - 900.0f) / 200.0f) + 15.0f);
        } else if (lux < 10000.0f) {
            index = (int) (((lux - 3500.0f) / SCREEN_NIT_SPAN_FIVE) + 28.0f);
        } else if (lux < 15000.0f) {
            index = 41;
        } else if (lux < 35000.0f) {
            index = 42;
        } else if (lux < 65000.0f) {
            index = 43;
        } else if (lux < 100000.0f) {
            index = MAX_SPAN_INDEX;
        } else {
            index = MAX_SPAN_INDEX;
        }
        if (DEBUG) {
            Slog.d(TAG, "lux = " + lux + ", index = " + index);
        }
        return index;
    }

    private int getBrightnessSpanByNit(int brightness) {
        float screenNit = getScreenNit(brightness);
        int index = 0;
        if (screenNit >= SCREEN_NIT_SPAN_ONE && screenNit < SCREEN_NIT_SPAN_TWO) {
            index = 1;
        } else if (screenNit >= SCREEN_NIT_SPAN_TWO && screenNit < 50.0f) {
            index = ((int) ((screenNit - SCREEN_NIT_SPAN_TWO) / SPAN_SCREEN_NIT_STEP_ONE)) + 2;
        } else if (screenNit >= 50.0f && screenNit < SCREEN_NIT_SPAN_FOUR) {
            index = ((int) ((screenNit - 50.0f) / SPAN_SCREEN_NIT_STEP_TWO)) + 2 + 6;
        } else if (screenNit >= SCREEN_NIT_SPAN_FOUR && screenNit < SCREEN_NIT_SPAN_FIVE) {
            index = ((int) ((screenNit - SCREEN_NIT_SPAN_FOUR) / SPAN_SCREEN_NIT_STEP_THREE)) + 2 + 9;
        } else if (screenNit >= SCREEN_NIT_SPAN_FIVE && screenNit < SCREEN_NIT_SPAN_SIX) {
            index = ((int) ((screenNit - SCREEN_NIT_SPAN_FIVE) / 50.0f)) + 2 + 30;
        }
        if (DEBUG) {
            Slog.d(TAG, "brightness = " + brightness + ", screenNit = " + screenNit + ", index = " + index);
        }
        return index;
    }

    private float getScreenNit(int brightness) {
        int i = NORMAL_SCREEN_BRIGHTNESS;
        if (brightness > i) {
            float screenNit = MAX_SCREEN_NIT + ((brightness - i) * HBM_MIN_SCREEN_NIT);
            return screenNit;
        }
        float screenNit2 = MIN_SCREEN_NIT * brightness;
        return screenNit2;
    }

    private int convergeAffectFactors() {
        int factor = 0;
        if (this.mSwitchStatsHelper.isReadModeSettingsEnable()) {
            factor = 0 | 1;
        }
        if (this.mSwitchStatsHelper.isDcBacklightSettingsEnable()) {
            factor |= 2;
        }
        if (this.mSwitchStatsHelper.isDarkModeSettingsEnable()) {
            return factor | 4;
        }
        return factor;
    }

    private String splitSpline(Spline spline) {
        if (spline != null) {
            String sp = spline.toString();
            return spline.toString().substring(sp.indexOf(123) + 1, sp.lastIndexOf(125));
        }
        return "";
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class BrightnessChangeHandler extends Handler {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public BrightnessChangeHandler(Looper looper) {
            super(looper);
            MiuiBrightnessChangeTracker.this = r1;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    MiuiBrightnessChangeTracker.this.handleBrightnessChangeEvent((BrightnessChangeItem) msg.obj);
                    return;
                case 2:
                    MiuiBrightnessChangeTracker.this.updatePointerEventMotionState(((Boolean) msg.obj).booleanValue(), msg.arg1, msg.arg2);
                    return;
                case 3:
                    MiuiBrightnessChangeTracker.this.readyToReportEvent();
                    return;
                case 4:
                    MiuiBrightnessChangeTracker.this.updateScreenStateChanged(((Boolean) msg.obj).booleanValue());
                    return;
                case 5:
                    MiuiBrightnessChangeTracker.this.updateForegroundApps();
                    return;
                case 6:
                    MiuiBrightnessChangeTracker.this.mOrientation = ((Integer) msg.obj).intValue();
                    return;
                case 7:
                    SomeArgs args = (SomeArgs) msg.obj;
                    MiuiBrightnessChangeTracker.this.updateBrightnessStatisticsData(((Boolean) args.arg1).booleanValue(), ((Float) args.arg2).floatValue());
                    return;
                case 8:
                    SomeArgs someArgs = (SomeArgs) msg.obj;
                    MiuiBrightnessChangeTracker.this.updateBrightnessAnimInfo(((Float) someArgs.arg1).floatValue(), ((Float) someArgs.arg2).floatValue(), ((Boolean) someArgs.arg3).booleanValue());
                    return;
                case 9:
                    MiuiBrightnessChangeTracker.this.mTargetBrightnessAnimValue = ((Float) msg.obj).floatValue();
                    return;
                case 10:
                    MiuiBrightnessChangeTracker.this.updateInterruptBrightnessAnimDuration(msg.arg1, ((Float) msg.obj).floatValue());
                    return;
                case 11:
                    MiuiBrightnessChangeTracker.this.mTemporaryBrightnessTimeStamp = SystemClock.elapsedRealtime();
                    return;
                case 12:
                    MiuiBrightnessChangeTracker.this.resetBrightnessAnimInfo();
                    return;
                default:
                    return;
            }
        }
    }

    /* loaded from: classes.dex */
    public class BrightnessChangeItem {
        float ambientLux;
        int brightness;
        float brightnessOverrideFromWindow;
        Spline brightnessSpline;
        boolean defaultConfig;
        boolean lowPowerMode;
        boolean sunlightActive;
        int type;
        boolean useAutoBrightness;
        float userDataPoint;
        boolean userInitiatedChange;

        public BrightnessChangeItem(int brightness, boolean userInitiatedChange, boolean useAutoBrightness, float brightnessOverrideFromWindow, boolean lowPowerMode, boolean sunlightActive, float ambientLux, float userDataPoint, boolean defaultConfig, Spline spline, int type) {
            MiuiBrightnessChangeTracker.this = r1;
            this.brightness = brightness;
            this.brightnessOverrideFromWindow = brightnessOverrideFromWindow;
            this.userInitiatedChange = userInitiatedChange;
            this.useAutoBrightness = useAutoBrightness;
            this.lowPowerMode = lowPowerMode;
            this.sunlightActive = sunlightActive;
            this.ambientLux = ambientLux;
            this.userDataPoint = userDataPoint;
            this.defaultConfig = defaultConfig;
            this.brightnessSpline = spline;
            this.type = type;
        }

        public boolean equals(Object o) {
            if (o == null || !(o instanceof BrightnessChangeItem)) {
                return false;
            }
            BrightnessChangeItem value = (BrightnessChangeItem) o;
            return this.brightness == value.brightness && this.type == value.type && this.userInitiatedChange == value.userInitiatedChange && this.useAutoBrightness == value.useAutoBrightness && this.brightnessOverrideFromWindow == value.brightnessOverrideFromWindow && this.lowPowerMode == value.lowPowerMode && this.ambientLux == value.ambientLux && this.userDataPoint == value.userDataPoint;
        }

        public String toString() {
            return "BrightnessChangeItem{type=" + typeToString(this.type) + ", brightness=" + this.brightness + ", brightnessOverrideFromWindow=" + this.brightnessOverrideFromWindow + ", userInitiatedChange=" + this.userInitiatedChange + ", useAutoBrightness=" + this.useAutoBrightness + ", lowPowerMode=" + this.lowPowerMode + ", sunlightActive=" + this.sunlightActive + ", ambientLux=" + this.ambientLux + ", userDataPoint=" + this.userDataPoint + ", defaultConfig=" + this.defaultConfig + ", brightnessSpline=" + this.brightnessSpline + '}';
        }

        private String typeToString(int type) {
            switch (type) {
                case 0:
                    return "manual_brightness";
                case 1:
                    return "auto_brightness";
                case 2:
                    return "auto_manual_brightness";
                case 3:
                    return "window_override_brightness";
                case 4:
                    return "sunlight_brightness";
                default:
                    return null;
            }
        }
    }

    /* loaded from: classes.dex */
    public class ScreenStateReceiver extends BroadcastReceiver {
        ScreenStateReceiver() {
            MiuiBrightnessChangeTracker.this = this$0;
        }

        /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            char c;
            boolean screenOn;
            String action = intent.getAction();
            switch (action.hashCode()) {
                case -2128145023:
                    if (action.equals("android.intent.action.SCREEN_OFF")) {
                        c = 1;
                        break;
                    }
                    c = 65535;
                    break;
                case -1454123155:
                    if (action.equals("android.intent.action.SCREEN_ON")) {
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
                    screenOn = true;
                    break;
                default:
                    screenOn = false;
                    break;
            }
            Message msg = Message.obtain(MiuiBrightnessChangeTracker.this.mBackgroundHandler, 4, Boolean.valueOf(screenOn));
            msg.sendToTarget();
        }
    }

    /* loaded from: classes.dex */
    public class TaskStackListenerImpl extends TaskStackListener {
        TaskStackListenerImpl() {
            MiuiBrightnessChangeTracker.this = this$0;
        }

        public void onTaskStackChanged() {
            MiuiBrightnessChangeTracker.this.mBackgroundHandler.sendEmptyMessage(5);
        }
    }

    /* loaded from: classes.dex */
    public class RotationWatcher extends IRotationWatcher.Stub {
        RotationWatcher() {
            MiuiBrightnessChangeTracker.this = this$0;
        }

        public void onRotationChanged(int rotation) throws RemoteException {
            Message msg = Message.obtain(MiuiBrightnessChangeTracker.this.mBackgroundHandler, 6, Integer.valueOf(rotation));
            msg.sendToTarget();
        }
    }

    /* loaded from: classes.dex */
    public class WindowOverlayEventListener implements WindowManagerPolicyConstants.PointerEventListener {
        int distance_x;
        int distance_y;
        boolean userDragging;
        float eventXDown = -1.0f;
        float eventYDown = -1.0f;
        float eventXUp = -1.0f;
        float eventYUp = -1.0f;

        WindowOverlayEventListener() {
            MiuiBrightnessChangeTracker.this = this$0;
        }

        public void onPointerEvent(MotionEvent event) {
            switch (event.getAction()) {
                case 0:
                    this.eventXDown = event.getX();
                    this.eventYDown = event.getY();
                    break;
                case 1:
                    this.eventXUp = event.getX();
                    this.eventYUp = event.getY();
                    this.userDragging = false;
                    break;
                case 2:
                    this.userDragging = true;
                    break;
            }
            if (this.userDragging != MiuiBrightnessChangeTracker.this.mUserDragging) {
                MiuiBrightnessChangeTracker.this.mBackgroundHandler.removeMessages(2);
                if (!this.userDragging) {
                    this.distance_x = (int) MathUtils.abs(this.eventXUp - this.eventXDown);
                    this.distance_y = (int) MathUtils.abs(this.eventYUp - this.eventYDown);
                    if (MiuiBrightnessChangeTracker.DEBUG) {
                        Slog.d(MiuiBrightnessChangeTracker.TAG, "onPointerEvent: x_down: " + this.eventXDown + ", y_down: " + this.eventYDown + ", x_up: " + this.eventXUp + ", y_up: " + this.eventYUp + ", distance_x: " + this.distance_x + ", distance_y: " + this.distance_y);
                    }
                }
                Message message = Message.obtain(MiuiBrightnessChangeTracker.this.mBackgroundHandler, 2, this.distance_x, this.distance_y, Boolean.valueOf(this.userDragging));
                message.sendToTarget();
            }
        }
    }

    private void scheduleUpdateBrightnessStatisticsData(boolean screenOn, float brightness) {
        SomeArgs args = SomeArgs.obtain();
        args.arg1 = Boolean.valueOf(screenOn);
        args.arg2 = Float.valueOf(brightness);
        this.mBackgroundHandler.obtainMessage(7, args).sendToTarget();
    }

    public void updateBrightnessStatisticsData(boolean screenOn, float brightness) {
        long now = SystemClock.elapsedRealtime();
        if (this.mScreenOn != screenOn) {
            this.mScreenOn = screenOn;
            if (screenOn) {
                this.mLastScreenOnTimeStamp = 0L;
            } else {
                this.mLastStoreBrightness = Float.NaN;
                computeLastAverageBrightness(now);
            }
        }
        if (screenOn) {
            computeAverageBrightnessIfNeeded(brightness);
        }
    }

    private void computeAverageBrightnessIfNeeded(float brightness) {
        if (this.mLastStoreBrightness == brightness && !this.mAutoBrightnessModeChanged) {
            return;
        }
        this.mLastStoreBrightness = brightness;
        long now = SystemClock.elapsedRealtime();
        if (this.mLastScreenOnTimeStamp != 0) {
            computeLastAverageBrightness(now);
        }
        this.mLastAutoBrightness = Float.NaN;
        this.mLastManualBrightness = Float.NaN;
        if (this.mAutoBrightnessEnable) {
            this.mLastAutoBrightness = brightness;
        } else {
            this.mLastManualBrightness = brightness;
        }
        this.mLastScreenOnTimeStamp = now;
        if (this.mAutoBrightnessModeChanged) {
            this.mAutoBrightnessModeChanged = false;
        }
        if (DEBUG) {
            Slog.d(TAG, "computeAverageBrightnessIfNeeded: current brightness: " + brightness + ", mAutoBrightnessEnable: " + this.mAutoBrightnessEnable + ", time: " + this.mLastScreenOnTimeStamp);
        }
    }

    private void computeLastAverageBrightness(long now) {
        float timeDuration = ((float) (now - this.mLastScreenOnTimeStamp)) * 0.001f;
        if (timeDuration != MiuiFreeformPinManagerService.EDGE_AREA) {
            if (!Float.isNaN(this.mLastAutoBrightness)) {
                float f = this.mAutoBrightnessDuration + timeDuration;
                this.mAutoBrightnessDuration = f;
                float f2 = this.mAutoBrightnessIntegral + (this.mLastAutoBrightness * timeDuration);
                this.mAutoBrightnessIntegral = f2;
                this.mAverageBrightnessArray.put(1, Float.valueOf(f2 / f));
                if (DEBUG) {
                    Slog.d(TAG, "computeLastAverageBrightness: compute last auto average brightness, timeDuration: " + timeDuration + ", mAutoBrightnessDuration: " + this.mAutoBrightnessDuration + ", mAutoBrightnessIntegral: " + this.mAutoBrightnessIntegral + ", mAverageBrightnessArray: " + this.mAverageBrightnessArray.toString());
                    return;
                }
                return;
            }
            float f3 = this.mManualBrightnessDuration + timeDuration;
            this.mManualBrightnessDuration = f3;
            float f4 = this.mManualBrightnessIntegral + (this.mLastManualBrightness * timeDuration);
            this.mManualBrightnessIntegral = f4;
            this.mAverageBrightnessArray.put(0, Float.valueOf(f4 / f3));
            if (DEBUG) {
                Slog.d(TAG, "computeLastAverageBrightness: compute last manual average brightness, timeDuration: " + timeDuration + ", mManualBrightnessDuration: " + this.mManualBrightnessDuration + ", mManualBrightnessIntegral: " + this.mManualBrightnessIntegral + ", mAverageBrightnessArray: " + this.mAverageBrightnessArray.toString());
            }
        }
    }

    /* loaded from: classes.dex */
    public class SettingsObserver extends ContentObserver {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public SettingsObserver(Handler handler) {
            super(handler);
            MiuiBrightnessChangeTracker.this = r1;
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            if (selfChange) {
                return;
            }
            String lastPathSegment = uri.getLastPathSegment();
            char c = 65535;
            boolean z = false;
            switch (lastPathSegment.hashCode()) {
                case -693072130:
                    if (lastPathSegment.equals("screen_brightness_mode")) {
                        c = 0;
                        break;
                    }
                    break;
            }
            switch (c) {
                case 0:
                    MiuiBrightnessChangeTracker miuiBrightnessChangeTracker = MiuiBrightnessChangeTracker.this;
                    if (Settings.System.getIntForUser(miuiBrightnessChangeTracker.mContentResolver, "screen_brightness_mode", 0, -2) == 1) {
                        z = true;
                    }
                    miuiBrightnessChangeTracker.mAutoBrightnessEnable = z;
                    MiuiBrightnessChangeTracker.this.mAutoBrightnessModeChanged = true;
                    MiuiBrightnessChangeTracker.this.updateUserResetAutoBrightnessModeTimes();
                    return;
                default:
                    return;
            }
        }
    }

    public void updateUserResetAutoBrightnessModeTimes() {
        if (this.mAutoBrightnessEnable == this.mLastAutoBrightnessEnable) {
            return;
        }
        long now = SystemClock.elapsedRealtime();
        boolean z = true;
        if (this.mAutoBrightnessEnable && this.mIsValidResetAutoBrightnessMode) {
            long j = this.mLastResetBrightnessModeTime;
            if (j != 0 && now - j <= 5000) {
                this.mUserResetBrightnessModeTimes++;
                if (DEBUG) {
                    Slog.d(TAG, "onChange: mUserResetBrightnessModeTimes:" + this.mUserResetBrightnessModeTimes);
                }
            }
        }
        if (!this.mLastAutoBrightnessEnable || this.mAutoBrightnessEnable) {
            z = false;
        }
        this.mIsValidResetAutoBrightnessMode = z;
        this.mLastAutoBrightnessEnable = this.mAutoBrightnessEnable;
        this.mLastResetBrightnessModeTime = now;
    }

    private void registerSettingsObserver() {
        boolean z = false;
        this.mContentResolver.registerContentObserver(Settings.System.getUriFor("screen_brightness_mode"), false, this.mSettingsObserver, -1);
        if (Settings.System.getIntForUser(this.mContentResolver, "screen_brightness_mode", 0, -2) == 1) {
            z = true;
        }
        this.mAutoBrightnessEnable = z;
        this.mLastAutoBrightnessEnable = z;
    }

    private void reportExtraData() {
        if (this.mAverageBrightnessArray.size() != 0) {
            BrightnessEvent event = new BrightnessEvent();
            event.setEventType(7).setExtra(this.mAverageBrightnessArray.toString());
            reportEventToMQS(event, "brightness");
            resetExtraData();
        }
    }

    private void resetExtraData() {
        this.mAutoBrightnessDuration = MiuiFreeformPinManagerService.EDGE_AREA;
        this.mAutoBrightnessIntegral = MiuiFreeformPinManagerService.EDGE_AREA;
        this.mManualBrightnessDuration = MiuiFreeformPinManagerService.EDGE_AREA;
        this.mManualBrightnessIntegral = MiuiFreeformPinManagerService.EDGE_AREA;
        this.mAverageBrightnessArray.clear();
    }

    public void notifyUpdateTempBrightnessTimeStampIfNeeded(boolean enable) {
        if (enable != this.mIsTemporaryBrightnessAdjustment) {
            this.mIsTemporaryBrightnessAdjustment = enable;
            if (enable) {
                this.mPendingAnimationStart = false;
                this.mBackgroundHandler.obtainMessage(11).sendToTarget();
            }
        }
    }

    public void notifyUpdateBrightnessAnimInfo(float currentBrightnessAnim, float brightnessAnim, float targetBrightnessAnim) {
        int i;
        int i2;
        boolean begin = brightnessAnim != targetBrightnessAnim;
        int state = getBrightnessChangedState(currentBrightnessAnim, targetBrightnessAnim);
        if (begin != this.mPendingAnimationStart || (begin && (i2 = this.mPendingBrightnessChangedState) != 2 && i2 != 3 && state != 2 && state != 3 && state != i2)) {
            this.mPendingAnimationStart = begin;
            this.mPendingBrightnessChangedState = state;
            this.mPendingTargetBrightnessAnimValue = targetBrightnessAnim;
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = Float.valueOf(currentBrightnessAnim);
            args.arg2 = Float.valueOf(targetBrightnessAnim);
            args.arg3 = Boolean.valueOf(begin);
            this.mBackgroundHandler.obtainMessage(8, args).sendToTarget();
        } else if (begin && (i = this.mPendingBrightnessChangedState) != 2 && i != 3 && state != 2 && state != 3 && state == i && targetBrightnessAnim != this.mPendingTargetBrightnessAnimValue) {
            this.mPendingTargetBrightnessAnimValue = targetBrightnessAnim;
            Message msg = this.mBackgroundHandler.obtainMessage(9);
            msg.obj = Float.valueOf(targetBrightnessAnim);
            msg.sendToTarget();
        }
    }

    public void updateBrightnessAnimInfo(float currentBrightnessAnim, float targetBrightnessAnim, boolean begin) {
        if (this.mAutoBrightnessEnable) {
            this.mCurrentBrightnessAnimValue = currentBrightnessAnim;
            this.mTargetBrightnessAnimValue = targetBrightnessAnim;
            this.mBrightnessAnimStart = begin;
            this.mBrightnessAnimStartTime = SystemClock.elapsedRealtime();
            this.mBrightnessChangedState = getBrightnessChangedState(currentBrightnessAnim, targetBrightnessAnim);
            if (DEBUG) {
                Slog.d(TAG, "updateBrightnessAnimInfo: mCurrentAnimateValue:" + this.mCurrentBrightnessAnimValue + ", mTargetAnimateValue:" + this.mTargetBrightnessAnimValue + ", mAnimationStart:" + this.mBrightnessAnimStart + ", mAnimationStartTime:" + this.mBrightnessAnimStartTime + ", mBrightnessChangedState = " + this.mBrightnessChangedState);
            }
        }
    }

    private void updateInterruptBrightnessAnimDurationIfNeeded(int type, float brightness) {
        Message msg = this.mBackgroundHandler.obtainMessage(10);
        msg.arg1 = type;
        msg.obj = Float.valueOf(brightness);
        msg.sendToTarget();
    }

    public void updateInterruptBrightnessAnimDuration(int type, float brightness) {
        boolean isSameAdjustment = (this.mBrightnessChangedState == 0 && getBrightnessChangedState(this.mCurrentBrightnessAnimValue, brightness) == 0) || (this.mBrightnessChangedState == 1 && getBrightnessChangedState(this.mCurrentBrightnessAnimValue, brightness) == 1);
        if (type == 2) {
            if (this.mBrightnessAnimStart && isSameAdjustment) {
                long j = this.mTemporaryBrightnessTimeStamp;
                if (j != 0) {
                    long duration = j - this.mBrightnessAnimStartTime;
                    this.mInterruptBrightnessAnimationTimes++;
                    AdvancedEvent event = new AdvancedEvent();
                    event.setEventType(1).setInterruptBrightnessAnimationTimes(this.mInterruptBrightnessAnimationTimes).setAutoBrightnessAnimationDuration(((float) duration) * 0.001f).setCurrentAnimateValue(BrightnessSynchronizer.brightnessFloatToInt(this.mCurrentBrightnessAnimValue)).setTargetAnimateValue(BrightnessSynchronizer.brightnessFloatToInt(this.mTargetBrightnessAnimValue)).setUserBrightness(BrightnessSynchronizer.brightnessFloatToInt(brightness)).setBrightnessChangedState(this.mBrightnessChangedState);
                    reportEventToMQS(event, OneTrackUploaderHelper.ADVANCED_EVENT_NAME);
                    if (DEBUG) {
                        Slog.d(TAG, "updateInterruptBrightnessAnimDuration: duration:" + (((float) duration) * 0.001f) + ", mInterruptBrightnessAnimationTimes:" + this.mInterruptBrightnessAnimationTimes + ", currentAnimateValue:" + BrightnessSynchronizer.brightnessFloatToInt(this.mCurrentBrightnessAnimValue) + ", targetAnimateValue:" + BrightnessSynchronizer.brightnessFloatToInt(this.mTargetBrightnessAnimValue) + ", userBrightness:" + BrightnessSynchronizer.brightnessFloatToInt(brightness) + ", mBrightnessChangedState:" + this.mBrightnessChangedState);
                    }
                }
            }
            this.mBrightnessAnimStart = false;
            this.mTemporaryBrightnessTimeStamp = 0L;
        }
    }

    private void reportAdvancedEvent() {
        AdvancedEvent event = new AdvancedEvent();
        event.setEventType(2).setInterruptBrightnessAnimationTimes(this.mInterruptBrightnessAnimationTimes).setUserResetBrightnessModeTimes(this.mUserResetBrightnessModeTimes).setLongTermModelSplineError(this.mLongTermModelSplineError).setDefaultSplineError(this.mDefaultSplineError);
        reportEventToMQS(event, OneTrackUploaderHelper.ADVANCED_EVENT_NAME);
        resetAdvancedEventData();
    }

    private void resetAdvancedEventData() {
        this.mInterruptBrightnessAnimationTimes = 0;
        this.mUserResetBrightnessModeTimes = 0;
        this.mLongTermModelSplineError = -1.0f;
        this.mDefaultSplineError = -1.0f;
    }

    private int getBrightnessChangedState(float currentValue, float targetValue) {
        if (currentValue == MiuiFreeformPinManagerService.EDGE_AREA || targetValue == MiuiFreeformPinManagerService.EDGE_AREA) {
            return 3;
        }
        if (targetValue > currentValue) {
            return 0;
        }
        if (targetValue < currentValue) {
            return 1;
        }
        return 2;
    }

    public void resetBrightnessAnimInfo() {
        this.mCurrentBrightnessAnimValue = MiuiFreeformPinManagerService.EDGE_AREA;
        this.mTargetBrightnessAnimValue = MiuiFreeformPinManagerService.EDGE_AREA;
        this.mBrightnessAnimStart = false;
        this.mBrightnessAnimStartTime = 0L;
        this.mBrightnessChangedState = -1;
        this.mTemporaryBrightnessTimeStamp = 0L;
    }

    public void notifyResetBrightnessAnimInfo() {
        this.mPendingAnimationStart = false;
        this.mBackgroundHandler.obtainMessage(12).sendToTarget();
    }

    public void setUpCloudControllerListener(AutomaticBrightnessControllerImpl.CloudControllerListener listener) {
        this.mCloudControllerListener = listener;
    }
}
