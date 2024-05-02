package com.android.server.input.shoulderkey;

import android.app.ActivityTaskManager;
import android.app.IActivityTaskManager;
import android.app.KeyguardManager;
import android.app.TaskStackListener;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManagerInternal;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.server.am.SplitScreenReporter;
import android.util.ArrayMap;
import android.util.Slog;
import android.util.SparseArray;
import android.view.DisplayInfo;
import android.view.KeyEvent;
import android.view.MotionEvent;
import com.android.internal.util.DumpUtils;
import com.android.server.LocalServices;
import com.android.server.MiuiBatteryStatsService;
import com.android.server.MiuiBgThread;
import com.android.server.am.ProcessUtils;
import com.android.server.input.MiInputManager;
import com.android.server.input.config.InputCommonConfig;
import com.android.server.input.shoulderkey.ShoulderKeyManagerService;
import com.android.server.wm.MiuiFreeformPinManagerService;
import com.miui.server.input.stylus.MiuiStylusPageKeyListener;
import com.miui.server.input.util.MiuiCustomizeShortCutUtils;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import miui.hardware.shoulderkey.IShoulderKeyManager;
import miui.hardware.shoulderkey.ITouchMotionEventListener;
import miui.hardware.shoulderkey.ShoulderKey;
import miui.hardware.shoulderkey.ShoulderKeyManager;
import miui.hardware.shoulderkey.ShoulderKeyMap;
import miui.os.Build;
/* loaded from: classes.dex */
public class ShoulderKeyManagerService extends IShoulderKeyManager.Stub {
    private static final long GAMEBOOSTER_DEBOUNCE_DELAY_MILLS = 150;
    private static final String GAME_BOOSTER_SWITCH = "shoulder_quick_star_gameturbo";
    private static final String KEY_GAME_BOOSTER = "gb_boosting";
    private static final int NONUI_SENSOR_ID = 33171027;
    public static final String SHOULDEKEY_SOUND_TYPE = "shoulderkey_sound_type";
    private static final int SHOULDERKEY_POSITION_LEFT = 0;
    private static final int SHOULDERKEY_POSITION_RIGHT = 1;
    public static final String SHOULDERKEY_SOUND_SWITCH = "shoulderkey_sound_switch";
    private static final String TAG = "ShoulderKeyManager";
    private IActivityTaskManager mActivityTaskManager;
    private boolean mBoosterSwitch;
    private Context mContext;
    private String mCurrentForegroundAppLabel;
    private String mCurrentForegroundPkg;
    private DisplayInfo mDisplayInfo;
    private DisplayManager mDisplayManager;
    private DisplayManagerInternal mDisplayManagerInternal;
    private long mDownTime;
    private H mHandler;
    private HandlerThread mHandlerThread;
    private boolean mIsGameMode;
    private boolean mIsPocketMode;
    private boolean mIsScreenOn;
    private KeyguardManager mKeyguardManager;
    private boolean mLeftShoulderKeySwitchStatus;
    private boolean mLeftShoulderKeySwitchTriggered;
    private LocalService mLocalService;
    private MiuiShoulderKeyShortcutListener mMiuiShoulderKeyShortcutListener;
    private Sensor mNonUISensor;
    private PackageManager mPackageManager;
    private boolean mRecordEventStatus;
    private boolean mRegisteredNonUI;
    private boolean mRightShoulderKeySwitchStatus;
    private boolean mRightShoulderKeySwitchTriggered;
    private MiuiSettingsObserver mSettingsObserver;
    private boolean mShoulderKeySoundSwitch;
    private String mShoulderKeySoundType;
    private SensorManager mSm;
    private boolean DEBUG = false;
    private HashSet<Integer> mInjectEventPids = new HashSet<>();
    private final HashSet<String> mInjcetEeventPackages = new HashSet<>();
    private final DisplayManager.DisplayListener mDisplayListener = new DisplayManager.DisplayListener() { // from class: com.android.server.input.shoulderkey.ShoulderKeyManagerService.1
        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayChanged(int displayId) {
            if (displayId != 0) {
                return;
            }
            ShoulderKeyManagerService shoulderKeyManagerService = ShoulderKeyManagerService.this;
            shoulderKeyManagerService.mDisplayInfo = shoulderKeyManagerService.mDisplayManagerInternal.getDisplayInfo(0);
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayRemoved(int displayId) {
        }

        @Override // android.hardware.display.DisplayManager.DisplayListener
        public void onDisplayAdded(int displayId) {
        }
    };
    private SensorEventListener mNonUIListener = new SensorEventListener() { // from class: com.android.server.input.shoulderkey.ShoulderKeyManagerService.2
        @Override // android.hardware.SensorEventListener
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == ShoulderKeyManagerService.NONUI_SENSOR_ID) {
                if (event.values[0] != MiuiFreeformPinManagerService.EDGE_AREA) {
                    ShoulderKeyManagerService.this.mIsPocketMode = true;
                } else if (event.values[0] == MiuiFreeformPinManagerService.EDGE_AREA) {
                    ShoulderKeyManagerService.this.mIsPocketMode = false;
                }
                Slog.d(ShoulderKeyManagerService.TAG, "NonUIEventListener onSensorChanged,mIsPocketMode = " + ShoulderKeyManagerService.this.mIsPocketMode);
            }
        }

        @Override // android.hardware.SensorEventListener
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
    private final Object mTouchMotionEventLock = new Object();
    private final SparseArray<TouchMotionEventListenerRecord> mTouchMotionEventListeners = new SparseArray<>();
    private final List<TouchMotionEventListenerRecord> mTempTouchMotionEventListenersToNotify = new ArrayList();
    private TaskStackListenerImpl mTaskStackListener = new TaskStackListenerImpl();
    private ArrayMap<ShoulderKey, ShoulderKeyMap> mLifeKeyMapper = new ArrayMap<>();
    private boolean mSupportShoulderKey = ShoulderKeyManager.SUPPORT_SHOULDERKEY;

    public ShoulderKeyManagerService(Context context) {
        this.mContext = context;
        HandlerThread handlerThread = new HandlerThread("shoulderKey");
        this.mHandlerThread = handlerThread;
        handlerThread.start();
        this.mHandler = new H(this.mHandlerThread.getLooper());
        LocalService localService = new LocalService();
        this.mLocalService = localService;
        LocalServices.addService(ShoulderKeyManagerInternal.class, localService);
        MiuiSettingsObserver miuiSettingsObserver = new MiuiSettingsObserver(this.mHandler);
        this.mSettingsObserver = miuiSettingsObserver;
        miuiSettingsObserver.observe();
        this.mMiuiShoulderKeyShortcutListener = new MiuiShoulderKeyShortcutListener(this.mContext);
        init();
    }

    private void init() {
        if (this.mSupportShoulderKey) {
            this.mLeftShoulderKeySwitchStatus = ShoulderKeyUtil.getShoulderKeySwitchStatus(0);
            this.mRightShoulderKeySwitchStatus = ShoulderKeyUtil.getShoulderKeySwitchStatus(1);
        }
        this.mInjectEventPids.clear();
        this.mRecordEventStatus = false;
        this.mInjcetEeventPackages.add("com.xiaomi.macro");
        this.mInjcetEeventPackages.add("com.xiaomi.migameservice");
        this.mInjcetEeventPackages.add("com.xiaomi.joyose");
    }

    public void systemReadyInternal() {
        this.mActivityTaskManager = ActivityTaskManager.getService();
        this.mPackageManager = this.mContext.getPackageManager();
        this.mKeyguardManager = (KeyguardManager) this.mContext.getSystemService(MiuiStylusPageKeyListener.SCENE_KEYGUARD);
        registerForegroundAppUpdater();
        if (this.mSupportShoulderKey) {
            SensorManager sensorManager = (SensorManager) this.mContext.getSystemService("sensor");
            this.mSm = sensorManager;
            this.mNonUISensor = sensorManager.getDefaultSensor(NONUI_SENSOR_ID, true);
        }
        DisplayManager displayManager = (DisplayManager) this.mContext.getSystemService(DisplayManager.class);
        this.mDisplayManager = displayManager;
        displayManager.registerDisplayListener(this.mDisplayListener, null);
        DisplayManagerInternal displayManagerInternal = (DisplayManagerInternal) LocalServices.getService(DisplayManagerInternal.class);
        this.mDisplayManagerInternal = displayManagerInternal;
        this.mDisplayInfo = displayManagerInternal.getDisplayInfo(0);
    }

    private void registerForegroundAppUpdater() {
        try {
            this.mActivityTaskManager.registerTaskStackListener(this.mTaskStackListener);
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to register foreground app updater: " + e);
        }
    }

    public void loadLiftKeyMap(Map mapper) {
        if (mapper == null) {
            throw new IllegalArgumentException("lift key mapper is null");
        }
        if (mapper.size() == 0) {
            throw new IllegalArgumentException("lift key mapper is empty");
        }
        Slog.d(TAG, "loadLiftKeyMap, mapper.size() = " + mapper.size());
        Message msg = this.mHandler.obtainMessage(0);
        msg.obj = mapper;
        this.mHandler.sendMessage(msg);
    }

    public void unloadLiftKeyMap() {
        Slog.d(TAG, "unloadLiftKeyMap");
        this.mHandler.sendEmptyMessage(1);
    }

    public boolean getShoulderKeySwitchStatus(int position) {
        if (position == 0) {
            return this.mLeftShoulderKeySwitchStatus;
        }
        if (position == 1) {
            return this.mRightShoulderKeySwitchStatus;
        }
        return false;
    }

    public void setInjectMotionEventStatus(boolean enable) {
        if (!checkInjectEventsPermission()) {
            Slog.d(TAG, Binder.getCallingPid() + "process Not have INJECT_EVENTS permission!");
            return;
        }
        Slog.d(TAG, ProcessUtils.getProcessNameByPid(Binder.getCallingPid()) + " setInjectMotionEventStatus " + enable);
        int pid = Binder.getCallingPid();
        if (enable) {
            this.mInjectEventPids.add(Integer.valueOf(pid));
        } else {
            this.mInjectEventPids.remove(Integer.valueOf(pid));
        }
        this.mHandler.sendEmptyMessage(5);
    }

    public void injectTouchMotionEvent(MotionEvent event) {
        if (!checkInjectEventsPermission()) {
            Slog.d(TAG, Binder.getCallingPid() + " Not have INJECT_EVENTS permission!");
            return;
        }
        Message msg = this.mHandler.obtainMessage(2);
        msg.obj = event;
        msg.arg1 = 2;
        msg.arg2 = Binder.getCallingPid();
        this.mHandler.sendMessage(msg);
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (!DumpUtils.checkDumpPermission(this.mContext, TAG, pw)) {
            return;
        }
        if (args.length == 2) {
            boolean z = false;
            if ("debuglog".equals(args[0])) {
                if (Integer.parseInt(args[1]) == 1) {
                    z = true;
                }
                this.DEBUG = z;
            }
        }
        dumpInternal(pw);
    }

    private boolean checkInjectEventsPermission() {
        int pid = Binder.getCallingPid();
        return this.mInjcetEeventPackages.contains(ProcessUtils.getPackageNameByPid(pid));
    }

    public void dumpInternal(PrintWriter pw) {
        pw.println("SHOULDERKEY MANAGER (dumpsys shoulderkey)\n");
        pw.println("    DEBUG=" + this.DEBUG);
        pw.println("    mIsGameMode=" + this.mIsGameMode);
        pw.println("    mSupportShoulderKey=" + this.mSupportShoulderKey);
        pw.println("    mRecordEventStatus=" + this.mRecordEventStatus);
        pw.println("    mInjectEventPids=" + this.mInjectEventPids);
        if (this.mSupportShoulderKey) {
            pw.println("    mBoosterSwitch=" + this.mBoosterSwitch);
            pw.println("    mLeftShoulderKeySwitchStatus=" + this.mLeftShoulderKeySwitchStatus);
            pw.println("    mRightShoulderKeySwitchStatus=" + this.mRightShoulderKeySwitchStatus);
            pw.println("    mShoulderKeySoundSwitch=" + this.mShoulderKeySoundSwitch);
            pw.println("    mShoulderKeySoundType=" + this.mShoulderKeySoundType);
            pw.println("    DisplayInfo { rotation=" + this.mDisplayInfo.rotation + " width=" + this.mDisplayInfo.logicalWidth + " height=" + this.mDisplayInfo.logicalHeight + " }");
        }
    }

    private void handleShoulderKeyToMotionEvent(KeyEvent event, int position) {
        ShoulderKeyMap keymap;
        float centerY;
        float centerX;
        int action = event.getAction();
        if (action != 0 && action != 1) {
            return;
        }
        int productId = event.getDevice().getProductId();
        int keycode = event.getKeyCode();
        int i = 0;
        while (true) {
            if (i >= this.mLifeKeyMapper.size()) {
                keymap = null;
                break;
            } else if (!this.mLifeKeyMapper.keyAt(i).equals(productId, keycode)) {
                i++;
            } else {
                ShoulderKeyMap keymap2 = this.mLifeKeyMapper.valueAt(i);
                keymap = keymap2;
                break;
            }
        }
        if (keymap == null) {
            return;
        }
        if (action == 0) {
            this.mDownTime = SystemClock.uptimeMillis();
        }
        if (keymap.isIsSeparateMapping()) {
            if (action == 0) {
                centerX = keymap.getDownCenterX();
                centerY = keymap.getDownCenterY();
            } else {
                centerX = keymap.getUpCenterX();
                centerY = keymap.getUpCenterY();
            }
            float f = centerX;
            float f2 = centerY;
            MotionEvent downEvent = MotionEvent.obtain(this.mDownTime, SystemClock.uptimeMillis(), 0, f, f2, 0);
            this.mHandler.obtainMessage(2, position, Process.myPid(), downEvent).sendToTarget();
            MotionEvent upEvent = MotionEvent.obtain(this.mDownTime, SystemClock.uptimeMillis(), 1, f, f2, 0);
            this.mHandler.obtainMessage(2, position, Process.myPid(), upEvent).sendToTarget();
            return;
        }
        MotionEvent motionEvent = MotionEvent.obtain(this.mDownTime, SystemClock.uptimeMillis(), action, keymap.getCenterX(), keymap.getCenterY(), 0);
        this.mHandler.obtainMessage(2, position, Process.myPid(), motionEvent).sendToTarget();
    }

    public void transformMotionEventForInjection(MotionEvent motionEvent) {
        if (this.DEBUG) {
            Slog.d(TAG, "before transform for injection: " + motionEvent.toString());
        }
        int rotation = this.mDisplayInfo.rotation;
        int width = this.mDisplayInfo.logicalWidth;
        int height = this.mDisplayInfo.logicalHeight;
        switch (rotation) {
            case 1:
                motionEvent.applyTransform(MotionEvent.createRotateMatrix(3, height, width));
                break;
            case 2:
                motionEvent.applyTransform(MotionEvent.createRotateMatrix(2, width, height));
                break;
            case 3:
                motionEvent.applyTransform(MotionEvent.createRotateMatrix(1, height, width));
                break;
        }
        if (this.DEBUG) {
            Slog.d(TAG, "after transform for injection: " + motionEvent.toString());
        }
    }

    public void transformMotionEventForDeliver(MotionEvent motionEvent) {
        if (this.DEBUG) {
            Slog.d(TAG, "before transform for deliver: " + motionEvent.toString());
        }
        int rotation = this.mDisplayInfo.rotation;
        int width = this.mDisplayInfo.logicalWidth;
        int height = this.mDisplayInfo.logicalHeight;
        switch (rotation) {
            case 1:
                motionEvent.applyTransform(MotionEvent.createRotateMatrix(1, width, height));
                break;
            case 2:
                motionEvent.applyTransform(MotionEvent.createRotateMatrix(2, width, height));
                break;
            case 3:
                motionEvent.applyTransform(MotionEvent.createRotateMatrix(3, width, height));
                break;
        }
        if (this.DEBUG) {
            Slog.d(TAG, "after transform for deliver: " + motionEvent.toString());
        }
    }

    public void handleShoulderKeyEventInternal(KeyEvent event) {
        if (isShoulderKeyCanShortCut(event.getKeyCode())) {
            this.mMiuiShoulderKeyShortcutListener.handleShoulderKeyShortcut(event);
        }
        int position = -1;
        if (event.getKeyCode() == 131) {
            position = 0;
        } else if (event.getKeyCode() == 132) {
            position = 1;
        }
        if (position != -1) {
            sendShoulderKeyEventBroadcast(1, position, event.getAction());
            if (this.mIsGameMode && !this.mLifeKeyMapper.isEmpty()) {
                handleShoulderKeyToMotionEvent(event, position);
            }
        }
        if (event.getAction() == 0) {
            if (event.getKeyCode() == 133) {
                setShoulderKeySwitchStatusInternal(0, true);
            } else if (event.getKeyCode() == 134) {
                setShoulderKeySwitchStatusInternal(0, false);
            } else if (event.getKeyCode() == 135) {
                setShoulderKeySwitchStatusInternal(1, true);
            } else if (event.getKeyCode() == 136) {
                setShoulderKeySwitchStatusInternal(1, false);
            }
        }
    }

    public boolean isShoulderKeyCanShortCut(int keyCode) {
        if ((keyCode != 131 && keyCode != 132) || !ShoulderKeyManager.SUPPORT_SHOULDERKEY_MORE || this.mIsPocketMode || this.mIsGameMode || !isUserSetupComplete()) {
            return false;
        }
        return true;
    }

    /* JADX WARN: Multi-variable type inference failed */
    public void setShoulderKeySwitchStatusInternal(int position, boolean isPopup) {
        if (position == 0) {
            this.mLeftShoulderKeySwitchStatus = isPopup;
            this.mLeftShoulderKeySwitchTriggered = isPopup;
        } else if (position == 1) {
            this.mRightShoulderKeySwitchStatus = isPopup;
            this.mRightShoulderKeySwitchTriggered = isPopup;
        } else {
            return;
        }
        if (isPopup != 0) {
            this.mHandler.sendEmptyMessageDelayed(4, GAMEBOOSTER_DEBOUNCE_DELAY_MILLS);
        }
        sendShoulderKeyEventBroadcast(0, position, isPopup ? 1 : 0);
        interceptGameBooster();
        playSoundIfNeeded(this.mShoulderKeySoundType + "-" + position + "-" + ((int) isPopup));
        if ((this.mLeftShoulderKeySwitchStatus || this.mRightShoulderKeySwitchStatus) && !this.mIsScreenOn) {
            registerNonUIListener();
        } else {
            unregisterNonUIListener();
        }
    }

    private void playSoundIfNeeded(String soundId) {
        if (this.mShoulderKeySoundSwitch && this.mIsScreenOn) {
            ShoulderKeyUtil.playSound(soundId, false);
        }
    }

    private void interceptGameBooster() {
        boolean isKeyguardShown = this.mKeyguardManager.isKeyguardLocked();
        if (!this.mIsGameMode && this.mIsScreenOn && this.mBoosterSwitch && !isKeyguardShown && this.mLeftShoulderKeySwitchTriggered && this.mRightShoulderKeySwitchTriggered && isUserSetupComplete()) {
            launchGameBooster();
        }
    }

    private void registerNonUIListener() {
        Sensor sensor;
        if (this.mRegisteredNonUI) {
            return;
        }
        SensorManager sensorManager = this.mSm;
        if (sensorManager != null && (sensor = this.mNonUISensor) != null) {
            sensorManager.registerListener(this.mNonUIListener, sensor, 3, this.mHandler);
            this.mRegisteredNonUI = true;
            Slog.w(TAG, " register NonUISensorListener");
            return;
        }
        Slog.w(TAG, " mNonUISensor is null");
    }

    private void unregisterNonUIListener() {
        SensorManager sensorManager;
        if (this.mRegisteredNonUI && (sensorManager = this.mSm) != null) {
            sensorManager.unregisterListener(this.mNonUIListener);
            this.mRegisteredNonUI = false;
            this.mIsPocketMode = false;
            Slog.w(TAG, " unregister NonUISensorListener,mIsPocketMode = false");
        }
    }

    private void launchGameBooster() {
        Intent intent = new Intent("com.miui.gamebooster.action.ACCESS_MAINACTIVITY");
        intent.putExtra("jump_target", "gamebox");
        intent.addFlags(268435456);
        this.mContext.startActivityAsUser(intent, UserHandle.CURRENT);
    }

    public boolean isUserSetupComplete() {
        return Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "user_setup_complete", 0, -2) != 0;
    }

    private void sendShoulderKeyEventBroadcast(int type, int position, int action) {
        ShoulderKeyOneTrack.reportShoulderKeyActionOneTrack(this.mContext, type, position, action, this.mIsGameMode, this.mCurrentForegroundAppLabel);
        Intent intent = new Intent("com.miui.shoulderkey");
        intent.putExtra(MiuiCustomizeShortCutUtils.ATTRIBUTE_TYPE, type);
        intent.putExtra("position", position);
        intent.putExtra(SplitScreenReporter.STR_ACTION, action);
        this.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
    }

    public void loadSoundResourceIfNeeded() {
        if (this.mShoulderKeySoundSwitch) {
            ShoulderKeyUtil.loadSoundResource(this.mContext);
        } else {
            ShoulderKeyUtil.releaseSoundResource();
        }
    }

    public void updateScreenStateInternal(boolean isScreenOn) {
        this.mIsScreenOn = isScreenOn;
        if ((this.mLeftShoulderKeySwitchStatus || this.mRightShoulderKeySwitchStatus) && !isScreenOn) {
            registerNonUIListener();
        } else {
            unregisterNonUIListener();
        }
    }

    public void notifyForegroundAppChanged() {
        this.mHandler.post(new Runnable() { // from class: com.android.server.input.shoulderkey.ShoulderKeyManagerService.3
            @Override // java.lang.Runnable
            public void run() {
                try {
                    ActivityTaskManager.RootTaskInfo info = ShoulderKeyManagerService.this.mActivityTaskManager.getFocusedRootTaskInfo();
                    if (info != null && info.topActivity != null) {
                        String packageName = info.topActivity.getPackageName();
                        if (ShoulderKeyManagerService.this.mCurrentForegroundPkg != null && ShoulderKeyManagerService.this.mCurrentForegroundPkg.equals(packageName)) {
                            return;
                        }
                        ShoulderKeyManagerService.this.mCurrentForegroundPkg = packageName;
                        ShoulderKeyManagerService shoulderKeyManagerService = ShoulderKeyManagerService.this;
                        shoulderKeyManagerService.mCurrentForegroundAppLabel = shoulderKeyManagerService.getAppLabelByPkgName(packageName);
                    }
                } catch (RemoteException e) {
                }
            }
        });
    }

    public String getAppLabelByPkgName(String packageName) {
        ApplicationInfo ai = null;
        try {
            ai = this.mPackageManager.getApplicationInfo(packageName, 64);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (ai == null) {
            return "";
        }
        String label = ai.loadLabel(this.mPackageManager).toString();
        return label;
    }

    /* loaded from: classes.dex */
    public final class TouchMotionEventListenerRecord implements IBinder.DeathRecipient {
        private final ITouchMotionEventListener mListener;
        private final int mPid;

        public TouchMotionEventListenerRecord(int pid, ITouchMotionEventListener listener) {
            ShoulderKeyManagerService.this = r1;
            this.mPid = pid;
            this.mListener = listener;
        }

        @Override // android.os.IBinder.DeathRecipient
        public void binderDied() {
            if (ShoulderKeyManagerService.this.DEBUG) {
                Slog.d(ShoulderKeyManagerService.TAG, "Touch MoitonEvent listener for pid " + this.mPid + " died.");
            }
            ShoulderKeyManagerService.this.onTouchMotionEventListenerDied(this.mPid);
        }

        public void notifyTouchMotionEvent(MotionEvent event) {
            try {
                this.mListener.onTouchMotionEvent(event);
            } catch (RemoteException ex) {
                Slog.w(ShoulderKeyManagerService.TAG, "Failed to notify process " + this.mPid + " that Touch MotionEvent, assuming it died.", ex);
                binderDied();
            }
        }
    }

    public void registerTouchMotionEventListener(ITouchMotionEventListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        synchronized (this.mTouchMotionEventLock) {
            int callingPid = Binder.getCallingPid();
            if (this.mTouchMotionEventListeners.get(callingPid) != null) {
                throw new IllegalStateException("The calling process has already registered a TabletModeChangedListener.");
            }
            TouchMotionEventListenerRecord record = new TouchMotionEventListenerRecord(callingPid, listener);
            try {
                IBinder binder = listener.asBinder();
                binder.linkToDeath(record, 0);
                this.mTouchMotionEventListeners.put(callingPid, record);
                this.mHandler.obtainMessage(6, true).sendToTarget();
            } catch (RemoteException ex) {
                throw new IllegalStateException(ex);
            }
        }
    }

    public void unregisterTouchMotionEventListener() {
        synchronized (this.mTouchMotionEventLock) {
            int callingPid = Binder.getCallingPid();
            onTouchMotionEventListenerDied(callingPid);
        }
    }

    public void onTouchMotionEventListenerDied(int pid) {
        synchronized (this.mTouchMotionEventLock) {
            this.mTouchMotionEventListeners.remove(pid);
            this.mHandler.obtainMessage(6, Boolean.valueOf(this.mTouchMotionEventListeners.size() != 0)).sendToTarget();
        }
    }

    public void deliverTouchMotionEvent(MotionEvent event) {
        int numListeners;
        if (this.DEBUG) {
            Slog.d(TAG, "deliverTouchMotionEvent " + event.toString());
        }
        this.mTempTouchMotionEventListenersToNotify.clear();
        synchronized (this.mTouchMotionEventLock) {
            numListeners = this.mTouchMotionEventListeners.size();
            for (int i = 0; i < numListeners; i++) {
                this.mTempTouchMotionEventListenersToNotify.add(this.mTouchMotionEventListeners.valueAt(i));
            }
        }
        for (int i2 = 0; i2 < numListeners; i2++) {
            this.mTempTouchMotionEventListenersToNotify.get(i2).notifyTouchMotionEvent(event);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class LocalService implements ShoulderKeyManagerInternal {
        LocalService() {
            ShoulderKeyManagerService.this = this$0;
        }

        @Override // com.android.server.input.shoulderkey.ShoulderKeyManagerInternal
        public void systemReady() {
            ShoulderKeyManagerService.this.systemReadyInternal();
        }

        @Override // com.android.server.input.shoulderkey.ShoulderKeyManagerInternal
        public void handleShoulderKeyEvent(KeyEvent event) {
            ShoulderKeyManagerService.this.handleShoulderKeyEventInternal(event);
        }

        @Override // com.android.server.input.shoulderkey.ShoulderKeyManagerInternal
        public void updateScreenState(boolean isScreenOn) {
            ShoulderKeyManagerService.this.updateScreenStateInternal(isScreenOn);
        }

        @Override // com.android.server.input.shoulderkey.ShoulderKeyManagerInternal
        public void setShoulderKeySwitchStatus(int position, boolean isPopup) {
            ShoulderKeyManagerService.this.setShoulderKeySwitchStatusInternal(position, isPopup);
        }

        @Override // com.android.server.input.shoulderkey.ShoulderKeyManagerInternal
        public void notifyTouchMotionEvent(MotionEvent event) {
            Message msg = ShoulderKeyManagerService.this.mHandler.obtainMessage(3, event);
            ShoulderKeyManagerService.this.mHandler.sendMessage(msg);
        }

        @Override // com.android.server.input.shoulderkey.ShoulderKeyManagerInternal
        public void onUserSwitch() {
            ShoulderKeyManagerService.this.mSettingsObserver.update();
            ShoulderKeyManagerService.this.mMiuiShoulderKeyShortcutListener.updateSettings();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class MiuiSettingsObserver extends ContentObserver {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        MiuiSettingsObserver(Handler handler) {
            super(handler);
            ShoulderKeyManagerService.this = this$0;
        }

        void observe() {
            ContentResolver resolver = ShoulderKeyManagerService.this.mContext.getContentResolver();
            resolver.registerContentObserver(Settings.Secure.getUriFor(ShoulderKeyManagerService.KEY_GAME_BOOSTER), false, this, -1);
            resolver.registerContentObserver(Settings.Secure.getUriFor(ShoulderKeyManagerService.GAME_BOOSTER_SWITCH), false, this, -1);
            resolver.registerContentObserver(Settings.System.getUriFor(ShoulderKeyManagerService.SHOULDERKEY_SOUND_SWITCH), false, this, -1);
            resolver.registerContentObserver(Settings.System.getUriFor(ShoulderKeyManagerService.SHOULDEKEY_SOUND_TYPE), false, this, -1);
            update();
        }

        void update() {
            onChange(false, Settings.Secure.getUriFor(ShoulderKeyManagerService.KEY_GAME_BOOSTER));
            onChange(false, Settings.Secure.getUriFor(ShoulderKeyManagerService.GAME_BOOSTER_SWITCH));
            onChange(false, Settings.System.getUriFor(ShoulderKeyManagerService.SHOULDERKEY_SOUND_SWITCH));
            onChange(false, Settings.System.getUriFor(ShoulderKeyManagerService.SHOULDEKEY_SOUND_TYPE));
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChanged, Uri uri) {
            ContentResolver resolver = ShoulderKeyManagerService.this.mContext.getContentResolver();
            boolean z = false;
            if (Settings.Secure.getUriFor(ShoulderKeyManagerService.KEY_GAME_BOOSTER).equals(uri)) {
                ShoulderKeyManagerService shoulderKeyManagerService = ShoulderKeyManagerService.this;
                if (Settings.Secure.getIntForUser(resolver, ShoulderKeyManagerService.KEY_GAME_BOOSTER, 0, -2) == 1) {
                    z = true;
                }
                shoulderKeyManagerService.mIsGameMode = z;
                if (!ShoulderKeyManagerService.this.mIsGameMode) {
                    ShoulderKeyManagerService.this.mHandler.obtainMessage(7).sendToTarget();
                }
            } else if (Settings.Secure.getUriFor(ShoulderKeyManagerService.GAME_BOOSTER_SWITCH).equals(uri)) {
                ShoulderKeyManagerService shoulderKeyManagerService2 = ShoulderKeyManagerService.this;
                if (Settings.Secure.getIntForUser(resolver, ShoulderKeyManagerService.GAME_BOOSTER_SWITCH, 1, -2) == 1) {
                    z = true;
                }
                shoulderKeyManagerService2.mBoosterSwitch = z;
            } else if (Settings.System.getUriFor(ShoulderKeyManagerService.SHOULDERKEY_SOUND_SWITCH).equals(uri)) {
                ShoulderKeyManagerService shoulderKeyManagerService3 = ShoulderKeyManagerService.this;
                if (Settings.System.getIntForUser(resolver, ShoulderKeyManagerService.SHOULDERKEY_SOUND_SWITCH, 0, -2) == 1) {
                    z = true;
                }
                shoulderKeyManagerService3.mShoulderKeySoundSwitch = z;
                ShoulderKeyManagerService.this.loadSoundResourceIfNeeded();
            } else if (Settings.System.getUriFor(ShoulderKeyManagerService.SHOULDEKEY_SOUND_TYPE).equals(uri)) {
                ShoulderKeyManagerService.this.mShoulderKeySoundType = Settings.System.getStringForUser(resolver, ShoulderKeyManagerService.SHOULDEKEY_SOUND_TYPE, -2);
            }
        }
    }

    /* loaded from: classes.dex */
    public class TaskStackListenerImpl extends TaskStackListener {
        TaskStackListenerImpl() {
            ShoulderKeyManagerService.this = this$0;
        }

        public void onTaskStackChanged() {
            ShoulderKeyManagerService.this.notifyForegroundAppChanged();
        }
    }

    /* loaded from: classes.dex */
    public static class ShoulderKeyOneTrack {
        private static final String EXTRA_APP_ID = "31000000481";
        private static final String EXTRA_EVENT_NAME = "shoulderkey";
        private static final String EXTRA_PACKAGE_NAME = "com.xiaomi.shoulderkey";
        private static final int FLAG_NON_ANONYMOUS = 2;
        private static final String INTENT_ACTION_ONETRACK = "onetrack.action.TRACK_EVENT";
        private static final String INTENT_PACKAGE_ONETRACK = "com.miui.analytics";
        private static final String KEY_ACTION = "action";
        private static final String KEY_EVENT_TYPE = "event_type";
        private static final String KEY_GAME_NAME = "game_name";
        private static final String KEY_IS_GAMEBOOSTER = "is_gamebooster";
        private static final String KEY_POSITION = "position";
        private static final String TAG = "ShoulderKeyOneTrack";

        private ShoulderKeyOneTrack() {
        }

        public static void reportShoulderKeyActionOneTrack(final Context context, final int eventType, final int position, final int action, final boolean isGameMode, final String pkgLabel) {
            if (context == null || Build.IS_INTERNATIONAL_BUILD) {
                return;
            }
            MiuiBgThread.getHandler().post(new Runnable() { // from class: com.android.server.input.shoulderkey.ShoulderKeyManagerService$ShoulderKeyOneTrack$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    ShoulderKeyManagerService.ShoulderKeyOneTrack.lambda$reportShoulderKeyActionOneTrack$0(eventType, position, action, isGameMode, pkgLabel, context);
                }
            });
        }

        public static /* synthetic */ void lambda$reportShoulderKeyActionOneTrack$0(int eventType, int position, int action, boolean isGameMode, String pkgLabel, Context context) {
            Intent intent = new Intent("onetrack.action.TRACK_EVENT");
            intent.setPackage("com.miui.analytics");
            intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_APP_ID, EXTRA_APP_ID);
            intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_EVENT_NAME, EXTRA_EVENT_NAME);
            intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_PACKAGE, EXTRA_PACKAGE_NAME);
            intent.putExtra(KEY_EVENT_TYPE, eventType);
            intent.putExtra(KEY_POSITION, position);
            intent.putExtra("action", action);
            intent.putExtra(KEY_IS_GAMEBOOSTER, isGameMode);
            if (isGameMode) {
                intent.putExtra(KEY_GAME_NAME, pkgLabel);
            }
            intent.setFlags(2);
            try {
                context.startServiceAsUser(intent, UserHandle.CURRENT);
            } catch (IllegalStateException e) {
                Slog.w(TAG, "Failed to upload ShoulderKey event.");
            } catch (SecurityException e2) {
                Slog.w(TAG, "Unable to start service.");
            }
        }
    }

    /* loaded from: classes.dex */
    public class H extends Handler {
        private static final int MSG_DELIVER_TOUCH_MOTIONEVENT = 3;
        private static final int MSG_EXIT_GAMEMODE = 7;
        private static final int MSG_INJECT_EVENT_STATUS = 5;
        private static final int MSG_INJECT_MOTIONEVENT = 2;
        private static final int MSG_LOAD_LIFTKEYMAP = 0;
        private static final int MSG_RECORD_EVENT_STATUS = 6;
        private static final int MSG_RESET_SHOULDERKEY_TRIGGER_STATE = 4;
        private static final int MSG_UNLOAD_LIFTKEYMAP = 1;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public H(Looper looper) {
            super(looper);
            ShoulderKeyManagerService.this = this$0;
        }

        private void updateInjectEventStatus() {
            InputCommonConfig.getInstance().setInjectEventStatus(ShoulderKeyManagerService.this.mInjectEventPids.size() != 0);
            InputCommonConfig.getInstance().flushToNative();
        }

        private void updateRecordEventStatus(Boolean enable) {
            ShoulderKeyManagerService.this.mRecordEventStatus = enable.booleanValue();
            InputCommonConfig.getInstance().setRecordEventStatus(ShoulderKeyManagerService.this.mRecordEventStatus);
            InputCommonConfig.getInstance().flushToNative();
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    Map mapper = (Map) msg.obj;
                    ShoulderKeyManagerService.this.mInjectEventPids.add(Integer.valueOf(Process.myPid()));
                    updateInjectEventStatus();
                    if (mapper != null) {
                        ShoulderKeyManagerService.this.mLifeKeyMapper.clear();
                        ShoulderKeyManagerService.this.mLifeKeyMapper.putAll(mapper);
                        Slog.d(ShoulderKeyManagerService.TAG, "loadLiftKeyMap " + ShoulderKeyManagerService.this.mLifeKeyMapper);
                        return;
                    }
                    Slog.d(ShoulderKeyManagerService.TAG, "loadLiftKeyMap : null");
                    return;
                case 1:
                    ShoulderKeyManagerService.this.mInjectEventPids.remove(Integer.valueOf(Process.myPid()));
                    updateInjectEventStatus();
                    ShoulderKeyManagerService.this.mLifeKeyMapper.clear();
                    return;
                case 2:
                    MotionEvent event = (MotionEvent) msg.obj;
                    ShoulderKeyManagerService.this.transformMotionEventForInjection(event);
                    MiInputManager.getInstance().injectMotionEvent(event, msg.arg1);
                    if (ShoulderKeyManagerService.this.DEBUG) {
                        Slog.d(ShoulderKeyManagerService.TAG, ProcessUtils.getProcessNameByPid(msg.arg2) + " inject motionEvent " + msg.arg1 + " " + event.toString());
                        return;
                    } else if (event.getActionMasked() != 2) {
                        Slog.d(ShoulderKeyManagerService.TAG, ProcessUtils.getProcessNameByPid(msg.arg2) + " inject motionEvent " + msg.arg1 + " " + MotionEvent.actionToString(event.getAction()));
                        return;
                    } else {
                        return;
                    }
                case 3:
                    MotionEvent event2 = (MotionEvent) msg.obj;
                    ShoulderKeyManagerService.this.transformMotionEventForDeliver(event2);
                    ShoulderKeyManagerService.this.deliverTouchMotionEvent(event2);
                    return;
                case 4:
                    ShoulderKeyManagerService.this.mLeftShoulderKeySwitchTriggered = false;
                    ShoulderKeyManagerService.this.mRightShoulderKeySwitchTriggered = false;
                    return;
                case 5:
                    updateInjectEventStatus();
                    return;
                case 6:
                    updateRecordEventStatus((Boolean) msg.obj);
                    return;
                case 7:
                    if (ShoulderKeyManagerService.this.DEBUG) {
                        Slog.d(ShoulderKeyManagerService.TAG, "EXIT GAMEMODE");
                    }
                    ShoulderKeyManagerService.this.mInjectEventPids.clear();
                    updateInjectEventStatus();
                    updateRecordEventStatus(false);
                    return;
                default:
                    return;
            }
        }
    }
}
