package com.miui.server.input;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Point;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Slog;
import android.view.IRotationWatcher;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.view.WindowManagerPolicyConstants;
import com.android.server.wm.MiuiFreeformPinManagerService;
import com.android.server.wm.WindowManagerService;
import com.miui.server.input.util.ShortCutActionsUtils;
import java.io.PrintWriter;
import java.util.Objects;
import miui.android.animation.controller.AnimState;
import miui.util.BackTapSensorWrapper;
/* loaded from: classes.dex */
public class MiuiBackTapGestureService {
    private static final String KEY_GAME_BOOSTER = "gb_boosting";
    private static final String TAG = "MiuiBackTapGestureService";
    private String mBackDoubleTapFunction;
    private BackTapTouchHelper mBackTapTouchHelper;
    private String mBackTripleTapFunction;
    private Context mContext;
    private Handler mHandler;
    private MiuiSettingsObserver mMiuiSettingsObserver;
    private BackTapSensorWrapper mBackTapSensorWrapper = null;
    private boolean mIsSupportBackTapSensor = false;
    private boolean mIsBackTapSensorListenerRegistered = false;
    private boolean mScreenOn = false;
    private int mCurrentUserId = -2;
    private volatile boolean mIsGameMode = false;
    private boolean mIsUnFolded = false;
    private final BackTapSensorWrapper.BackTapSensorChangeListener mBackTapSensorChangeListener = new BackTapSensorWrapper.BackTapSensorChangeListener() { // from class: com.miui.server.input.MiuiBackTapGestureService.1
        public void onBackDoubleTap() {
            if (!"none".equals(MiuiBackTapGestureService.this.mBackDoubleTapFunction) && !MiuiBackTapGestureService.this.mBackTapTouchHelper.checkTouchStatus() && !MiuiBackTapGestureService.this.mIsGameMode) {
                MiuiBackTapGestureService miuiBackTapGestureService = MiuiBackTapGestureService.this;
                miuiBackTapGestureService.postShortcutFunction(miuiBackTapGestureService.mBackDoubleTapFunction, 0, "back_double_tap");
            }
        }

        public void onBackTripleTap() {
            if (!"none".equals(MiuiBackTapGestureService.this.mBackTripleTapFunction) && !MiuiBackTapGestureService.this.mBackTapTouchHelper.checkTouchStatus() && !MiuiBackTapGestureService.this.mIsGameMode) {
                MiuiBackTapGestureService miuiBackTapGestureService = MiuiBackTapGestureService.this;
                miuiBackTapGestureService.postShortcutFunction(miuiBackTapGestureService.mBackTripleTapFunction, 0, "back_triple_tap");
            }
        }
    };

    public MiuiBackTapGestureService(Context context) {
        init(context);
    }

    private void init(Context context) {
        this.mContext = context;
        boolean z = ((SensorManager) context.getSystemService("sensor")).getDefaultSensor(33171045) != null;
        this.mIsSupportBackTapSensor = z;
        if (z) {
            this.mHandler = new H();
            this.mBackTapSensorWrapper = new BackTapSensorWrapper(this.mContext);
            backTapSettingsInit();
        }
    }

    private void backTapSettingsInit() {
        this.mBackDoubleTapFunction = MiuiSettings.Key.getKeyAndGestureShortcutFunction(this.mContext, "back_double_tap");
        this.mBackTripleTapFunction = MiuiSettings.Key.getKeyAndGestureShortcutFunction(this.mContext, "back_triple_tap");
        updateBackTapFeatureState();
        MiuiSettingsObserver miuiSettingsObserver = new MiuiSettingsObserver(this.mHandler);
        this.mMiuiSettingsObserver = miuiSettingsObserver;
        miuiSettingsObserver.observe();
        this.mBackTapTouchHelper = new BackTapTouchHelper();
        if ("turn_on_torch".equals(this.mBackDoubleTapFunction)) {
            Settings.System.putStringForUser(this.mContext.getContentResolver(), "back_double_tap", "none", -2);
        }
        if ("turn_on_torch".equals(this.mBackTripleTapFunction)) {
            Settings.System.putStringForUser(this.mContext.getContentResolver(), "back_triple_tap", "none", -2);
        }
    }

    public void notifyScreenOn() {
        this.mScreenOn = true;
        updateBackTapFeatureState();
    }

    public void notifyScreenOff() {
        this.mScreenOn = false;
        updateBackTapFeatureState();
    }

    public void notifyFoldStatus(boolean folded) {
        this.mIsUnFolded = !folded;
    }

    public void onUserSwitch(int newUserId) {
        if (this.mCurrentUserId != newUserId) {
            this.mCurrentUserId = newUserId;
            updateSettings();
        }
    }

    private void updateSettings() {
        if (!this.mIsSupportBackTapSensor) {
            return;
        }
        this.mMiuiSettingsObserver.onChange(false, Settings.System.getUriFor("back_double_tap"));
        this.mMiuiSettingsObserver.onChange(false, Settings.System.getUriFor("back_triple_tap"));
    }

    public void updateBackTapFeatureState() {
        if (!this.mIsSupportBackTapSensor) {
            return;
        }
        this.mHandler.post(new Runnable() { // from class: com.miui.server.input.MiuiBackTapGestureService.2
            /* JADX WARN: Code restructure failed: missing block: B:11:0x002f, code lost:
                if (r0.checkEmpty(r0.mBackTripleTapFunction) == false) goto L12;
             */
            @Override // java.lang.Runnable
            /*
                Code decompiled incorrectly, please refer to instructions dump.
                To view partially-correct add '--show-bad-code' argument
            */
            public void run() {
                /*
                    r3 = this;
                    com.miui.server.input.MiuiBackTapGestureService r0 = com.miui.server.input.MiuiBackTapGestureService.this
                    miui.util.BackTapSensorWrapper r0 = com.miui.server.input.MiuiBackTapGestureService.m2130$$Nest$fgetmBackTapSensorWrapper(r0)
                    if (r0 == 0) goto Lad
                    com.miui.server.input.MiuiBackTapGestureService r0 = com.miui.server.input.MiuiBackTapGestureService.this
                    boolean r0 = com.miui.server.input.MiuiBackTapGestureService.m2137$$Nest$fgetmScreenOn(r0)
                    r1 = 0
                    if (r0 == 0) goto L8e
                    com.miui.server.input.MiuiBackTapGestureService r0 = com.miui.server.input.MiuiBackTapGestureService.this
                    boolean r0 = com.miui.server.input.MiuiBackTapGestureService.m2134$$Nest$fgetmIsBackTapSensorListenerRegistered(r0)
                    if (r0 != 0) goto L50
                    com.miui.server.input.MiuiBackTapGestureService r0 = com.miui.server.input.MiuiBackTapGestureService.this
                    java.lang.String r2 = com.miui.server.input.MiuiBackTapGestureService.m2128$$Nest$fgetmBackDoubleTapFunction(r0)
                    boolean r0 = com.miui.server.input.MiuiBackTapGestureService.m2142$$Nest$mcheckEmpty(r0, r2)
                    if (r0 == 0) goto L31
                    com.miui.server.input.MiuiBackTapGestureService r0 = com.miui.server.input.MiuiBackTapGestureService.this
                    java.lang.String r2 = com.miui.server.input.MiuiBackTapGestureService.m2132$$Nest$fgetmBackTripleTapFunction(r0)
                    boolean r0 = com.miui.server.input.MiuiBackTapGestureService.m2142$$Nest$mcheckEmpty(r0, r2)
                    if (r0 != 0) goto L50
                L31:
                    com.miui.server.input.MiuiBackTapGestureService r0 = com.miui.server.input.MiuiBackTapGestureService.this
                    miui.util.BackTapSensorWrapper r0 = com.miui.server.input.MiuiBackTapGestureService.m2130$$Nest$fgetmBackTapSensorWrapper(r0)
                    com.miui.server.input.MiuiBackTapGestureService r1 = com.miui.server.input.MiuiBackTapGestureService.this
                    miui.util.BackTapSensorWrapper$BackTapSensorChangeListener r1 = com.miui.server.input.MiuiBackTapGestureService.m2129$$Nest$fgetmBackTapSensorChangeListener(r1)
                    r0.registerListener(r1)
                    com.miui.server.input.MiuiBackTapGestureService r0 = com.miui.server.input.MiuiBackTapGestureService.this
                    r1 = 1
                    com.miui.server.input.MiuiBackTapGestureService.m2140$$Nest$fputmIsBackTapSensorListenerRegistered(r0, r1)
                    com.miui.server.input.MiuiBackTapGestureService r0 = com.miui.server.input.MiuiBackTapGestureService.this
                    com.miui.server.input.MiuiBackTapGestureService$BackTapTouchHelper r0 = com.miui.server.input.MiuiBackTapGestureService.m2131$$Nest$fgetmBackTapTouchHelper(r0)
                    com.miui.server.input.MiuiBackTapGestureService.BackTapTouchHelper.m2158$$Nest$msetTouchTrackingEnabled(r0, r1)
                    goto Lad
                L50:
                    com.miui.server.input.MiuiBackTapGestureService r0 = com.miui.server.input.MiuiBackTapGestureService.this
                    boolean r0 = com.miui.server.input.MiuiBackTapGestureService.m2134$$Nest$fgetmIsBackTapSensorListenerRegistered(r0)
                    if (r0 == 0) goto Lad
                    com.miui.server.input.MiuiBackTapGestureService r0 = com.miui.server.input.MiuiBackTapGestureService.this
                    java.lang.String r2 = com.miui.server.input.MiuiBackTapGestureService.m2128$$Nest$fgetmBackDoubleTapFunction(r0)
                    boolean r0 = com.miui.server.input.MiuiBackTapGestureService.m2142$$Nest$mcheckEmpty(r0, r2)
                    if (r0 == 0) goto Lad
                    com.miui.server.input.MiuiBackTapGestureService r0 = com.miui.server.input.MiuiBackTapGestureService.this
                    java.lang.String r2 = com.miui.server.input.MiuiBackTapGestureService.m2132$$Nest$fgetmBackTripleTapFunction(r0)
                    boolean r0 = com.miui.server.input.MiuiBackTapGestureService.m2142$$Nest$mcheckEmpty(r0, r2)
                    if (r0 == 0) goto Lad
                    com.miui.server.input.MiuiBackTapGestureService r0 = com.miui.server.input.MiuiBackTapGestureService.this
                    miui.util.BackTapSensorWrapper r0 = com.miui.server.input.MiuiBackTapGestureService.m2130$$Nest$fgetmBackTapSensorWrapper(r0)
                    com.miui.server.input.MiuiBackTapGestureService r2 = com.miui.server.input.MiuiBackTapGestureService.this
                    miui.util.BackTapSensorWrapper$BackTapSensorChangeListener r2 = com.miui.server.input.MiuiBackTapGestureService.m2129$$Nest$fgetmBackTapSensorChangeListener(r2)
                    r0.unregisterListener(r2)
                    com.miui.server.input.MiuiBackTapGestureService r0 = com.miui.server.input.MiuiBackTapGestureService.this
                    com.miui.server.input.MiuiBackTapGestureService.m2140$$Nest$fputmIsBackTapSensorListenerRegistered(r0, r1)
                    com.miui.server.input.MiuiBackTapGestureService r0 = com.miui.server.input.MiuiBackTapGestureService.this
                    com.miui.server.input.MiuiBackTapGestureService$BackTapTouchHelper r0 = com.miui.server.input.MiuiBackTapGestureService.m2131$$Nest$fgetmBackTapTouchHelper(r0)
                    com.miui.server.input.MiuiBackTapGestureService.BackTapTouchHelper.m2158$$Nest$msetTouchTrackingEnabled(r0, r1)
                    goto Lad
                L8e:
                    com.miui.server.input.MiuiBackTapGestureService r0 = com.miui.server.input.MiuiBackTapGestureService.this
                    boolean r0 = com.miui.server.input.MiuiBackTapGestureService.m2134$$Nest$fgetmIsBackTapSensorListenerRegistered(r0)
                    if (r0 == 0) goto Lad
                    com.miui.server.input.MiuiBackTapGestureService r0 = com.miui.server.input.MiuiBackTapGestureService.this
                    miui.util.BackTapSensorWrapper r0 = com.miui.server.input.MiuiBackTapGestureService.m2130$$Nest$fgetmBackTapSensorWrapper(r0)
                    r0.unregisterAllListeners()
                    com.miui.server.input.MiuiBackTapGestureService r0 = com.miui.server.input.MiuiBackTapGestureService.this
                    com.miui.server.input.MiuiBackTapGestureService.m2140$$Nest$fputmIsBackTapSensorListenerRegistered(r0, r1)
                    com.miui.server.input.MiuiBackTapGestureService r0 = com.miui.server.input.MiuiBackTapGestureService.this
                    com.miui.server.input.MiuiBackTapGestureService$BackTapTouchHelper r0 = com.miui.server.input.MiuiBackTapGestureService.m2131$$Nest$fgetmBackTapTouchHelper(r0)
                    com.miui.server.input.MiuiBackTapGestureService.BackTapTouchHelper.m2158$$Nest$msetTouchTrackingEnabled(r0, r1)
                Lad:
                    return
                */
                throw new UnsupportedOperationException("Method not decompiled: com.miui.server.input.MiuiBackTapGestureService.AnonymousClass2.run():void");
            }
        });
    }

    public boolean checkEmpty(String feature) {
        if (TextUtils.isEmpty(feature) || feature.equals("none")) {
            return true;
        }
        return false;
    }

    public boolean postShortcutFunction(String action, int delay, String shortcut) {
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

    /* loaded from: classes.dex */
    public class MiuiSettingsObserver extends ContentObserver {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public MiuiSettingsObserver(Handler handler) {
            super(handler);
            MiuiBackTapGestureService.this = this$0;
        }

        void observe() {
            ContentResolver resolver = MiuiBackTapGestureService.this.mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor("back_double_tap"), false, this, -1);
            resolver.registerContentObserver(Settings.System.getUriFor("back_triple_tap"), false, this, -1);
            resolver.registerContentObserver(Settings.Secure.getUriFor(MiuiBackTapGestureService.KEY_GAME_BOOSTER), false, this, -1);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            if (Settings.System.getUriFor("back_double_tap").equals(uri)) {
                MiuiBackTapGestureService miuiBackTapGestureService = MiuiBackTapGestureService.this;
                miuiBackTapGestureService.mBackDoubleTapFunction = MiuiSettings.Key.getKeyAndGestureShortcutFunction(miuiBackTapGestureService.mContext, "back_double_tap");
            } else if (Settings.System.getUriFor("back_triple_tap").equals(uri)) {
                MiuiBackTapGestureService miuiBackTapGestureService2 = MiuiBackTapGestureService.this;
                miuiBackTapGestureService2.mBackTripleTapFunction = MiuiSettings.Key.getKeyAndGestureShortcutFunction(miuiBackTapGestureService2.mContext, "back_triple_tap");
            } else if (Settings.Secure.getUriFor(MiuiBackTapGestureService.KEY_GAME_BOOSTER).equals(uri)) {
                MiuiBackTapGestureService miuiBackTapGestureService3 = MiuiBackTapGestureService.this;
                boolean z = false;
                if (Settings.Secure.getInt(miuiBackTapGestureService3.mContext.getContentResolver(), MiuiBackTapGestureService.KEY_GAME_BOOSTER, 0) == 1) {
                    z = true;
                }
                miuiBackTapGestureService3.mIsGameMode = z;
            }
            MiuiBackTapGestureService.this.updateBackTapFeatureState();
        }
    }

    /* loaded from: classes.dex */
    public class H extends Handler {
        public static final int MSG_BACKTAP_FUNCTION = 1;

        H() {
            MiuiBackTapGestureService.this = this$0;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                String shortcut = msg.getData().getString("shortcut");
                String function = (String) msg.obj;
                if (function == null) {
                    return;
                }
                ShortCutActionsUtils.getInstance(MiuiBackTapGestureService.this.mContext).triggerFunction(function, shortcut, null, false);
            }
        }
    }

    public void dump(String prefix, PrintWriter pw) {
        pw.print("    ");
        pw.println(TAG);
        pw.print(prefix);
        pw.print("mIsSupportBackTapSensor=");
        pw.println(this.mIsSupportBackTapSensor);
        pw.print(prefix);
        pw.print("mIsBackTapSensorListenerRegistered=");
        pw.println(this.mIsBackTapSensorListenerRegistered);
        pw.print(prefix);
        pw.print("mBackDoubleTapFunction=");
        pw.println(this.mBackDoubleTapFunction);
        pw.print(prefix);
        pw.print("mBackTripleTapFunction=");
        pw.println(this.mBackTripleTapFunction);
        pw.print(prefix);
        pw.print("mIsGameMode=");
        pw.println(this.mIsGameMode);
        BackTapTouchHelper backTapTouchHelper = this.mBackTapTouchHelper;
        if (backTapTouchHelper != null) {
            backTapTouchHelper.dump(prefix, pw, backTapTouchHelper.isDebuggable());
        }
    }

    /* loaded from: classes.dex */
    public class BackTapTouchHelper {
        private RotationWatcher mRotationWatcher;
        private final String TAG = "BackTapTouchHelper";
        private final String BACK_TAP_TOUCH_DEBUG = "sys.sensor.backtap.dbg";
        private boolean DEBUG = false;
        private boolean mTouchTrackingEnabled = false;
        private float TOUCH_LEFT = 100.0f;
        private int SCREEN_WIDTH;
        private float TOUCH_RIGHT = this.SCREEN_WIDTH - 100.0f;
        private float TOUCH_TOP = 100.0f;
        private int SCREEN_HEIGHT;
        private float TOUCH_BOTTOM = this.SCREEN_HEIGHT - 100.0f;
        private int mRotation = -1;
        private final int TOUCH_EVENT_DEBOUNCE = Resources.getSystem().getInteger(285933585);
        private WindowManagerService mWms = ServiceManager.getService("window");
        private TouchPositionTracker mTouchPositionTracker = new TouchPositionTracker();

        public BackTapTouchHelper() {
            MiuiBackTapGestureService.this = r2;
            initialize();
        }

        private void initialize() {
            WindowManager wm = (WindowManager) MiuiBackTapGestureService.this.mContext.getSystemService("window");
            Point outSize = new Point();
            wm.getDefaultDisplay().getRealSize(outSize);
            this.SCREEN_WIDTH = outSize.x;
            int i = outSize.y;
            this.SCREEN_HEIGHT = i;
            this.TOUCH_LEFT = 50.0f;
            float f = this.SCREEN_WIDTH - 50.0f;
            this.TOUCH_RIGHT = f;
            this.TOUCH_TOP = 100.0f;
            float f2 = i;
            this.TOUCH_BOTTOM = f2;
            this.mTouchPositionTracker.updateTouchBorder(50.0f, f, 100.0f, f2);
            RotationWatcher rotationWatcher = new RotationWatcher();
            this.mRotationWatcher = rotationWatcher;
            this.mWms.watchRotation(rotationWatcher, MiuiBackTapGestureService.this.mContext.getDisplay().getDisplayId());
            Slog.d("BackTapTouchHelper", "mBackTapTouchHelper initialize!! SCREEN_WIDTH: = " + this.SCREEN_WIDTH + ", SCREEN_HEIGHT = " + this.SCREEN_HEIGHT);
        }

        public boolean checkTouchStatus() {
            int touchStatus = this.mTouchPositionTracker.getTouchStatus();
            Objects.requireNonNull(this.mTouchPositionTracker);
            if (touchStatus == 1) {
                Slog.d("BackTapTouchHelper", "getTouchStatus: TOUCH_POSITIVE, sensor event will be ignored");
                return true;
            } else if (MiuiBackTapGestureService.this.mIsUnFolded) {
                Slog.d("BackTapTouchHelper", "mIsUnFolded is " + MiuiBackTapGestureService.this.mIsUnFolded);
                return true;
            } else {
                long mNowTime = SystemClock.elapsedRealtimeNanos();
                int touchStatus2 = this.mTouchPositionTracker.getTouchStatus();
                Objects.requireNonNull(this.mTouchPositionTracker);
                if (touchStatus2 == 0 && mNowTime - this.mTouchPositionTracker.mLastObservedTouchTime < this.TOUCH_EVENT_DEBOUNCE * AnimState.VIEW_SIZE) {
                    Slog.d("BackTapTouchHelper", "checkTouchStatus: { mow time: " + mNowTime + " last positive time: " + this.mTouchPositionTracker.mLastObservedTouchTime + "}");
                    Slog.w("BackTapTouchHelper", "sensor event will be ignored due to touch event timeout not reached!");
                    return true;
                }
                return false;
            }
        }

        public void dump(String prefix, PrintWriter pw, boolean enableDebug) {
            pw.print("    ");
            pw.println("BackTapTouchHelper");
            pw.print(prefix);
            pw.print("SCREEN_WIDTH=");
            pw.println(this.SCREEN_WIDTH);
            pw.print(prefix);
            pw.print("SCREEN_HEIGHT=");
            pw.println(this.SCREEN_HEIGHT);
            pw.print(prefix);
            pw.print("TOUCH_REGION=[");
            pw.println(this.mTouchPositionTracker.mTouchLeft + ", " + this.mTouchPositionTracker.mTouchTop + ", " + this.mTouchPositionTracker.mTouchRight + ", " + this.mTouchPositionTracker.mTouchBottom + "]");
            pw.print(prefix);
            pw.print("TOUCH_EVENT_DEBOUNCE=");
            pw.println(this.TOUCH_EVENT_DEBOUNCE);
            this.DEBUG = enableDebug;
        }

        public void setTouchTrackingEnabled(boolean enable) {
            if (enable) {
                if (!this.mTouchTrackingEnabled) {
                    this.mWms.registerPointerEventListener(this.mTouchPositionTracker, 0);
                    this.mTouchTrackingEnabled = true;
                    Slog.d("BackTapTouchHelper", "touch pointer listener has registered!");
                    return;
                }
                return;
            }
            TouchPositionTracker touchPositionTracker = this.mTouchPositionTracker;
            if (touchPositionTracker != null && this.mTouchTrackingEnabled) {
                this.mWms.unregisterPointerEventListener(touchPositionTracker, 0);
                this.mTouchTrackingEnabled = false;
                Slog.d("BackTapTouchHelper", "touch pointer listener has unregistered!");
            }
        }

        public boolean isDebuggable() {
            return SystemProperties.getBoolean("sys.sensor.backtap.dbg", false);
        }

        public void notifyRotationChanged(int rotation) {
            TouchPositionTracker touchPositionTracker = this.mTouchPositionTracker;
            if (touchPositionTracker != null) {
                touchPositionTracker.updateTouchBorder(rotation);
            }
        }

        /* loaded from: classes.dex */
        public class RotationWatcher extends IRotationWatcher.Stub {
            private RotationWatcher() {
                BackTapTouchHelper.this = r1;
            }

            public void onRotationChanged(int i) throws RemoteException {
                Slog.d("BackTapTouchHelper", "rotation changed = " + i);
                if (BackTapTouchHelper.this.mRotation != i) {
                    BackTapTouchHelper.this.mRotation = i;
                    BackTapTouchHelper backTapTouchHelper = BackTapTouchHelper.this;
                    backTapTouchHelper.notifyRotationChanged(backTapTouchHelper.mRotation);
                }
            }
        }

        /* loaded from: classes.dex */
        public class TouchPositionTracker implements WindowManagerPolicyConstants.PointerEventListener {
            private final int TOUCH_UNKNOWN = -1;
            private final int TOUCH_NEGATIVE = 0;
            private final int TOUCH_POSITIVE = 1;
            int mTouchStatus = -1;
            public volatile float mTouchLeft = MiuiFreeformPinManagerService.EDGE_AREA;
            public volatile float mTouchRight = 500.0f;
            public volatile float mTouchTop = MiuiFreeformPinManagerService.EDGE_AREA;
            public volatile float mTouchBottom = 500.0f;
            private long mLastObservedTouchTime = 0;
            private short mPointerIndexTriggerBitMask = 0;

            public TouchPositionTracker() {
                BackTapTouchHelper.this = r4;
                Slog.d("BackTapTouchHelper", "TouchPositionTracker new obj!");
            }

            void updateTouchBorder(int rotation) {
                switch (rotation) {
                    case 1:
                        updateTouchBorder(BackTapTouchHelper.this.TOUCH_TOP, BackTapTouchHelper.this.TOUCH_BOTTOM, BackTapTouchHelper.this.SCREEN_WIDTH - BackTapTouchHelper.this.TOUCH_RIGHT, BackTapTouchHelper.this.SCREEN_WIDTH - BackTapTouchHelper.this.TOUCH_LEFT);
                        return;
                    case 2:
                    default:
                        updateTouchBorder(BackTapTouchHelper.this.TOUCH_LEFT, BackTapTouchHelper.this.TOUCH_RIGHT, BackTapTouchHelper.this.TOUCH_TOP, BackTapTouchHelper.this.TOUCH_BOTTOM);
                        return;
                    case 3:
                        updateTouchBorder(BackTapTouchHelper.this.SCREEN_HEIGHT - BackTapTouchHelper.this.TOUCH_BOTTOM, BackTapTouchHelper.this.SCREEN_HEIGHT - BackTapTouchHelper.this.TOUCH_TOP, BackTapTouchHelper.this.TOUCH_LEFT, BackTapTouchHelper.this.TOUCH_RIGHT);
                        return;
                }
            }

            void updateTouchBorder(float left, float right, float top, float bottom) {
                this.mTouchLeft = left;
                this.mTouchRight = right;
                this.mTouchTop = top;
                this.mTouchBottom = bottom;
                if (BackTapTouchHelper.this.DEBUG) {
                    Slog.d("BackTapTouchHelper", "updateTouchBorder!!");
                }
            }

            void verifyMotionEvent(MotionEvent.PointerCoords mPointerCoords, int pointerId) {
                if (BackTapTouchHelper.this.DEBUG) {
                    Slog.d("BackTapTouchHelper", "verifyMotionEvent [ID: " + pointerId + ", Ori: " + mPointerCoords.getAxisValue(8) + "],  { " + mPointerCoords.getAxisValue(0) + ", " + mPointerCoords.getAxisValue(1) + "}");
                }
                if (mPointerCoords.getAxisValue(0) > this.mTouchLeft && mPointerCoords.getAxisValue(0) < this.mTouchRight && mPointerCoords.getAxisValue(1) > this.mTouchTop && mPointerCoords.getAxisValue(1) < this.mTouchBottom) {
                    this.mPointerIndexTriggerBitMask = (short) (this.mPointerIndexTriggerBitMask | (1 << pointerId));
                    if (BackTapTouchHelper.this.DEBUG) {
                        Slog.w("BackTapTouchHelper", "touch: { time: " + this.mLastObservedTouchTime + "}");
                    }
                } else {
                    this.mPointerIndexTriggerBitMask = (short) (this.mPointerIndexTriggerBitMask & (~(1 << pointerId)));
                }
                if (this.mPointerIndexTriggerBitMask != 0) {
                    this.mTouchStatus = 1;
                } else {
                    this.mTouchStatus = 0;
                }
            }

            int getTouchStatus() {
                if (BackTapTouchHelper.this.DEBUG) {
                    Slog.d("BackTapTouchHelper", "touch status=" + this.mTouchStatus);
                }
                return this.mTouchStatus;
            }

            public void onPointerEvent(MotionEvent motionEvent) {
                if (motionEvent.isTouchEvent()) {
                    int action = motionEvent.getAction();
                    int mPointerCount = motionEvent.getPointerCount();
                    MotionEvent.PointerCoords mPointerCoords = new MotionEvent.PointerCoords();
                    if (BackTapTouchHelper.this.DEBUG) {
                        for (int i = 0; i < mPointerCount; i++) {
                            int mPointerId = motionEvent.getPointerId(i);
                            Slog.d("BackTapTouchHelper", "touch onPointerEvent: index: " + i + ", id:  " + mPointerId);
                        }
                    }
                    int i2 = action & 255;
                    switch (i2) {
                        case 0:
                        case 2:
                            for (int i3 = 0; i3 < mPointerCount; i3++) {
                                motionEvent.getPointerCoords(i3, mPointerCoords);
                                if (BackTapTouchHelper.this.DEBUG) {
                                    Slog.d("BackTapTouchHelper", "touch getPointerCoords: ORIENTATION = " + mPointerCoords.getAxisValue(8) + ", x = " + mPointerCoords.getAxisValue(0) + ", y = " + mPointerCoords.getAxisValue(1));
                                }
                                verifyMotionEvent(mPointerCoords, motionEvent.getPointerId(i3));
                            }
                            return;
                        case 1:
                        case 3:
                        case 6:
                            if (action == 1 || action == 3) {
                                if (BackTapTouchHelper.this.DEBUG) {
                                    Slog.d("BackTapTouchHelper", "touch onPointerEvent mTouchStatus = " + this.mTouchStatus);
                                    Slog.d("BackTapTouchHelper", "touch onPointerEvent action: " + action);
                                }
                                if (this.mTouchStatus == 1) {
                                    this.mLastObservedTouchTime = SystemClock.elapsedRealtimeNanos();
                                }
                                this.mTouchStatus = 0;
                            }
                            int index = (65280 & action) >> 8;
                            int pointerId = motionEvent.getPointerId(index);
                            this.mPointerIndexTriggerBitMask = (short) (this.mPointerIndexTriggerBitMask & (~(1 << pointerId)));
                            if (BackTapTouchHelper.this.DEBUG) {
                                Slog.d("BackTapTouchHelper", "touch onPointerEvent ACTION_POINTER_UP: index:" + index + ", pointerId:" + pointerId);
                                return;
                            }
                            return;
                        case 4:
                        case 5:
                        default:
                            if (BackTapTouchHelper.this.DEBUG) {
                                Slog.d("BackTapTouchHelper", "touch onPointerEvent action: " + action);
                                return;
                            }
                            return;
                    }
                }
            }
        }
    }
}
