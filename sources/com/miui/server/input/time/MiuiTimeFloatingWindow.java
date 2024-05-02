package com.miui.server.input.time;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Slog;
import android.view.Choreographer;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import com.android.internal.content.PackageMonitor;
import com.android.server.wm.MiuiFreeformPinManagerService;
import java.io.PrintWriter;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;
/* loaded from: classes.dex */
public class MiuiTimeFloatingWindow {
    private static final long LOG_PRINT_TIME_DIFF = 300000;
    private static final int REASON_SCREEN_STATE_CHANGE = 0;
    private static final int REASON_SETTINGS_CHANGE = 1;
    private static final String SETTINGS_PACKAGE_NAME = "com.android.settings";
    private static final String TAG = "MiuiTimeFloatingWindow";
    private Choreographer mChoreographer;
    private final Context mContext;
    private DateTimeFormatter mDateTimeFormatter;
    private Handler mHandler;
    private boolean mIsFirstScreenOn;
    private boolean mIsInit;
    private boolean mIsScreenOn;
    private boolean mIsTimeFloatingWindowOn;
    private WindowManager.LayoutParams mLayoutParams;
    private PackageMonitor mPackageMonitor;
    private View mRootView;
    private volatile boolean mShowTime;
    private TextView mTimeTextView;
    private IntentFilter mTimezoneChangedIntentFilter;
    private BroadcastReceiver mTimezoneChangedReceiver;
    private WindowManager mWindowManager;
    private long mTimerForLog = 0;
    private final Runnable mDrawCallBack = new Runnable() { // from class: com.miui.server.input.time.MiuiTimeFloatingWindow.1
        @Override // java.lang.Runnable
        public void run() {
            MiuiTimeFloatingWindow.this.mChoreographer.removeCallbacks(3, MiuiTimeFloatingWindow.this.mDrawCallBack, null);
            if (!MiuiTimeFloatingWindow.this.mShowTime) {
                return;
            }
            MiuiTimeFloatingWindow.this.updateTime();
            MiuiTimeFloatingWindow.this.mChoreographer.postCallback(3, MiuiTimeFloatingWindow.this.mDrawCallBack, null);
        }
    };
    private String mLastTimeText = "";

    public MiuiTimeFloatingWindow(Context context) {
        this.mContext = context;
        MiuiSettingsObserver miuiSettingsObserver = new MiuiSettingsObserver(this.mHandler);
        miuiSettingsObserver.observe();
    }

    public void initLayoutParams() {
        this.mLayoutParams.flags = 296;
        this.mLayoutParams.layoutInDisplayCutoutMode = 1;
        this.mLayoutParams.type = 2018;
        this.mLayoutParams.setTrustedOverlay();
        this.mLayoutParams.format = -3;
        this.mLayoutParams.gravity = 8388659;
        this.mLayoutParams.x = 0;
        this.mLayoutParams.y = 0;
        try {
            this.mLayoutParams.y = this.mWindowManager.getMaximumWindowMetrics().getWindowInsets().getDisplayCutout().getSafeInsetTop();
        } catch (Exception e) {
            Slog.d(TAG, e.getMessage());
        }
        this.mLayoutParams.width = -2;
        this.mLayoutParams.height = -2;
        this.mLayoutParams.setTitle(TAG);
        if (ActivityManager.isHighEndGfx()) {
            this.mLayoutParams.flags |= 16777216;
            this.mLayoutParams.privateFlags |= 2;
        }
    }

    public void initView() {
        LayoutInflater layoutInflater = (LayoutInflater) this.mContext.getSystemService(LayoutInflater.class);
        View inflate = layoutInflater.inflate(285999164, (ViewGroup) null);
        this.mRootView = inflate;
        this.mTimeTextView = (TextView) inflate.findViewById(285868102);
        this.mRootView.setOnTouchListener(new FloatingWindowOnTouchListener());
        this.mRootView.setForceDarkAllowed(false);
    }

    /* renamed from: addRootViewToWindow */
    public void m2276x6242cbc5(int reason) {
        if (reason == 1) {
            Slog.i(TAG, "Because settings state change, add window");
            this.mWindowManager.addView(this.mRootView, this.mLayoutParams);
        }
        if (this.mShowTime || !this.mIsScreenOn) {
            return;
        }
        this.mRootView.setVisibility(0);
        this.mShowTime = true;
        this.mChoreographer.postCallback(3, this.mDrawCallBack, null);
        Slog.i(TAG, "Time floating window show");
    }

    /* renamed from: removeRootViewFromWindow */
    public void m2274xbd90ad21(int reason) {
        if (reason == 1) {
            Slog.i(TAG, "Because settings state change, remove window");
            this.mWindowManager.removeViewImmediate(this.mRootView);
        }
        if (!this.mShowTime) {
            return;
        }
        this.mRootView.setVisibility(8);
        this.mShowTime = false;
        this.mTimerForLog = 0L;
        Slog.i(TAG, "Time floating window hide");
    }

    private void init() {
        this.mWindowManager = (WindowManager) this.mContext.getSystemService(WindowManager.class);
        this.mLayoutParams = new WindowManager.LayoutParams();
        HandlerThread handlerThread = new HandlerThread("TimeFloatingWindow");
        handlerThread.start();
        Handler handler = new Handler(handlerThread.getLooper());
        this.mHandler = handler;
        handler.post(new Runnable() { // from class: com.miui.server.input.time.MiuiTimeFloatingWindow$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                MiuiTimeFloatingWindow.this.initLayoutParams();
            }
        });
        this.mHandler.post(new Runnable() { // from class: com.miui.server.input.time.MiuiTimeFloatingWindow$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                MiuiTimeFloatingWindow.this.initView();
            }
        });
        updateDateTimeFormatter();
        this.mHandler.post(new Runnable() { // from class: com.miui.server.input.time.MiuiTimeFloatingWindow$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() {
                MiuiTimeFloatingWindow.this.m2275lambda$init$0$commiuiserverinputtimeMiuiTimeFloatingWindow();
            }
        });
        this.mIsInit = true;
        this.mPackageMonitor = new PackageMonitor() { // from class: com.miui.server.input.time.MiuiTimeFloatingWindow.2
            public void onPackageDataCleared(String packageName, int uid) {
                if (!MiuiTimeFloatingWindow.SETTINGS_PACKAGE_NAME.equals(packageName)) {
                    return;
                }
                Slog.i(MiuiTimeFloatingWindow.TAG, "settings data was cleared, write current value.");
                Settings.System.putIntForUser(MiuiTimeFloatingWindow.this.mContext.getContentResolver(), "miui_time_floating_window", MiuiTimeFloatingWindow.this.mIsTimeFloatingWindowOn ? 1 : 0, -2);
            }
        };
        this.mTimezoneChangedIntentFilter = new IntentFilter("android.intent.action.TIMEZONE_CHANGED");
        this.mTimezoneChangedReceiver = new BroadcastReceiver() { // from class: com.miui.server.input.time.MiuiTimeFloatingWindow.3
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context, Intent intent) {
                MiuiTimeFloatingWindow.this.updateDateTimeFormatter();
            }
        };
    }

    /* renamed from: lambda$init$0$com-miui-server-input-time-MiuiTimeFloatingWindow */
    public /* synthetic */ void m2275lambda$init$0$commiuiserverinputtimeMiuiTimeFloatingWindow() {
        this.mChoreographer = Choreographer.getInstance();
    }

    public void showTimeFloatWindow(final int reason) {
        if (!this.mIsInit) {
            init();
        }
        Slog.i(TAG, "Request show time floating window because " + reasonToString(reason) + " mShowTime = " + this.mShowTime + " mIsScreenOn = " + this.mIsScreenOn);
        this.mHandler.post(new Runnable() { // from class: com.miui.server.input.time.MiuiTimeFloatingWindow$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                MiuiTimeFloatingWindow.this.m2276x6242cbc5(reason);
            }
        });
    }

    public void hideTimeFloatWindow(final int reason) {
        Slog.i(TAG, "Request hide time floating window because " + reasonToString(reason) + " mShowTime = " + this.mShowTime + " mIsScreenOn = " + this.mIsScreenOn);
        this.mHandler.post(new Runnable() { // from class: com.miui.server.input.time.MiuiTimeFloatingWindow$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                MiuiTimeFloatingWindow.this.m2274xbd90ad21(reason);
            }
        });
    }

    public void updateTime() {
        String time = this.mDateTimeFormatter.format(Instant.now());
        if (!this.mLastTimeText.equals(time)) {
            this.mTimeTextView.setText(time);
            this.mLastTimeText = time;
        }
        logForIsRunning();
    }

    private void logForIsRunning() {
        long now = System.currentTimeMillis();
        if (now > this.mTimerForLog) {
            Slog.i(TAG, "Time floating window is running.");
            this.mTimerForLog = LOG_PRINT_TIME_DIFF + now;
        }
    }

    public void updateTimeFloatWindowState() {
        boolean newState = false;
        int newValue = Settings.System.getIntForUser(this.mContext.getContentResolver(), "miui_time_floating_window", 0, -2);
        if (newValue != 0) {
            newState = true;
        }
        if (this.mIsTimeFloatingWindowOn == newState) {
            Slog.w(TAG, "The setting value was not change, but receive the notify, new state is " + newState);
            return;
        }
        this.mIsTimeFloatingWindowOn = newState;
        if (newState) {
            showTimeFloatWindow(1);
            registerReceivers();
            return;
        }
        hideTimeFloatWindow(1);
        unregisterReceivers();
    }

    public void updateDateTimeFormatter() {
        this.mDateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(TimeZone.getDefault().toZoneId());
    }

    private void registerReceivers() {
        try {
            this.mContext.registerReceiver(this.mTimezoneChangedReceiver, this.mTimezoneChangedIntentFilter);
            this.mPackageMonitor.register(this.mContext, this.mHandler.getLooper(), UserHandle.CURRENT, true);
            Slog.i(TAG, "Register time zone and package monitor.");
        } catch (Exception e) {
            Slog.w(TAG, e.getMessage());
        }
    }

    private void unregisterReceivers() {
        try {
            this.mContext.unregisterReceiver(this.mTimezoneChangedReceiver);
            this.mPackageMonitor.unregister();
            Slog.i(TAG, "Unregister time zone and package monitor.");
        } catch (Exception e) {
            Slog.w(TAG, e.getMessage());
        }
    }

    public void updateScreenState(boolean isScreenOn) {
        if (this.mIsScreenOn == isScreenOn) {
            Slog.w(TAG, "The screen state not change, but receive the notify, now isScreenOn = " + isScreenOn);
            return;
        }
        this.mIsScreenOn = isScreenOn;
        if (isScreenOn && !this.mIsFirstScreenOn) {
            Slog.i(TAG, "First screen on let's read setting value");
            updateTimeFloatWindowState();
            this.mIsFirstScreenOn = true;
        } else if (!this.mIsTimeFloatingWindowOn) {
        } else {
            if (isScreenOn) {
                showTimeFloatWindow(0);
            } else {
                hideTimeFloatWindow(0);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class MiuiSettingsObserver extends ContentObserver {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public MiuiSettingsObserver(Handler handler) {
            super(handler);
            MiuiTimeFloatingWindow.this = r1;
        }

        void observe() {
            ContentResolver resolver = MiuiTimeFloatingWindow.this.mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor("miui_time_floating_window"), false, this, -2);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            MiuiTimeFloatingWindow.this.updateTimeFloatWindowState();
        }
    }

    /* loaded from: classes.dex */
    public class FloatingWindowOnTouchListener implements View.OnTouchListener {
        private static final int MOVE_THRESHOLD = 3;
        private final int[] mLocationTemp;
        protected float mStartRawX;
        protected float mStartRawY;
        protected float mStartX;
        protected float mStartY;

        private FloatingWindowOnTouchListener() {
            MiuiTimeFloatingWindow.this = r1;
            this.mStartRawX = MiuiFreeformPinManagerService.EDGE_AREA;
            this.mStartRawY = MiuiFreeformPinManagerService.EDGE_AREA;
            this.mStartX = MiuiFreeformPinManagerService.EDGE_AREA;
            this.mStartY = MiuiFreeformPinManagerService.EDGE_AREA;
            this.mLocationTemp = new int[2];
        }

        @Override // android.view.View.OnTouchListener
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case 0:
                    this.mStartRawX = event.getRawX();
                    this.mStartRawY = event.getRawY();
                    v.getLocationOnScreen(this.mLocationTemp);
                    int[] iArr = this.mLocationTemp;
                    this.mStartX = iArr[0];
                    this.mStartY = iArr[1];
                    break;
                case 1:
                    if (isClick(event)) {
                        v.performClick();
                        break;
                    }
                    break;
                case 2:
                    if (!isClick(event)) {
                        actionMoveEvent(event);
                        break;
                    }
                    break;
            }
            return true;
        }

        protected void actionMoveEvent(MotionEvent event) {
            int newX = (int) (this.mStartX + (event.getRawX() - this.mStartRawX));
            int newY = (int) (this.mStartY + (event.getRawY() - this.mStartRawY));
            MiuiTimeFloatingWindow.this.updateLocation(newX, newY);
        }

        private boolean isClick(MotionEvent event) {
            return Math.abs(this.mStartRawX - event.getRawX()) <= 3.0f && Math.abs(this.mStartRawY - event.getRawY()) <= 3.0f;
        }
    }

    public void updateLocation(int x, int y) {
        if ((this.mLayoutParams.x == x && this.mLayoutParams.y == y) || !this.mShowTime) {
            return;
        }
        this.mLayoutParams.x = x;
        this.mLayoutParams.y = y;
        this.mWindowManager.updateViewLayout(this.mRootView, this.mLayoutParams);
    }

    private String reasonToString(int reason) {
        switch (reason) {
            case 0:
                return "SCREEN_STATE_CHANGE";
            case 1:
                return "SETTINGS_CHANGE";
            default:
                return "UNKNOWN";
        }
    }

    public void dump(String prefix, PrintWriter pw) {
        pw.print("    ");
        pw.println(TAG);
        pw.println(prefix + "mShowTime = " + this.mShowTime);
        pw.println(prefix + "mIsTimeFloatingWindowOn = " + this.mIsTimeFloatingWindowOn);
        pw.println(prefix + "mIsScreenOn = " + this.mIsScreenOn);
    }
}
