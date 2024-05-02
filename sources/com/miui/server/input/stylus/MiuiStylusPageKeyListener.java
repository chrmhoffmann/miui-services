package com.miui.server.input.stylus;

import android.app.KeyguardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Slog;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.WindowManager;
import com.android.server.input.MiuiInputThread;
import com.android.server.input.ReflectionUtils;
import com.android.server.policy.WindowManagerPolicy;
import com.miui.server.input.MiuiInputSettingsConnection;
import com.miui.server.input.util.ShortCutActionsUtils;
import java.io.PrintWriter;
import java.util.Set;
import java.util.function.Consumer;
import miui.util.FeatureParser;
/* loaded from: classes.dex */
public class MiuiStylusPageKeyListener {
    private static final String HOME_PACKAGE_NAME = "com.miui.home";
    private static final String KEY_GAME_BOOSTER = "gb_boosting";
    private static final int LONG_PRESS_TIME_OUT = 380;
    private static final Set<String> NOTE_PACKAGE_NAME = Set.of("com.miui.notes", "com.miui.pen.demo");
    public static final String SCENE_APP = "app";
    public static final String SCENE_HOME = "home";
    public static final String SCENE_KEYGUARD = "keyguard";
    public static final String SCENE_OFF_SCREEN = "off_screen";
    private static final String SYSTEM_UI_PACKAGE_NAME = "com.android.systemui";
    private static final String TAG = "MiuiStylusPageKeyListener";
    private final Context mContext;
    private WindowManagerPolicy.WindowState mFocusedWindow;
    private final Handler mHandler;
    private final MiuiInputSettingsConnection mInputSettingsConnection;
    private boolean mIsGameMode;
    private boolean mIsRequestMaskShow;
    private boolean mIsScreenOn;
    private final MiuiSettingsObserver mMiuiSettingsObserver;
    private final MiuiStylusDeviceListener mMiuiStylusDeviceListener;
    private volatile boolean mQuickNoteKeyFunctionTriggered;
    private String mScene;
    private volatile boolean mScreenShotKeyFunctionTriggered;
    private boolean mIsUserSetupComplete = isUserSetUp();
    private final boolean mIsSupportQuickNoteWhenScreenOff = FeatureParser.getBoolean("stylus_quick_note", false);

    public MiuiStylusPageKeyListener(Context context) {
        this.mContext = context;
        H h = new H(MiuiInputThread.getHandler().getLooper());
        this.mHandler = h;
        this.mMiuiStylusDeviceListener = new MiuiStylusDeviceListener(context);
        MiuiInputSettingsConnection miuiInputSettingsConnection = MiuiInputSettingsConnection.getInstance();
        this.mInputSettingsConnection = miuiInputSettingsConnection;
        miuiInputSettingsConnection.registerCallbackListener(1, new Consumer() { // from class: com.miui.server.input.stylus.MiuiStylusPageKeyListener$$ExternalSyntheticLambda0
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                MiuiStylusPageKeyListener.this.m2265x4c7014d8((Message) obj);
            }
        });
        MiuiSettingsObserver miuiSettingsObserver = new MiuiSettingsObserver(h);
        this.mMiuiSettingsObserver = miuiSettingsObserver;
        miuiSettingsObserver.observe();
    }

    /* renamed from: lambda$new$0$com-miui-server-input-stylus-MiuiStylusPageKeyListener */
    public /* synthetic */ void m2265x4c7014d8(Message value) {
        triggerQuickNoteKeyFunction();
    }

    public long getDelayTime(KeyEvent keyEvent) {
        if (isScreenShotKey(keyEvent) || isQuickNoteKey(keyEvent)) {
            boolean isTriggered = isQuickNoteKey(keyEvent) ? this.mQuickNoteKeyFunctionTriggered : this.mScreenShotKeyFunctionTriggered;
            if (isTriggered) {
                int repeatCount = keyEvent.getRepeatCount();
                if (repeatCount == 0) {
                    Slog.i(TAG, "Shortcut triggered, so intercept " + KeyEvent.keyCodeToString(keyEvent.getKeyCode()));
                    return -1L;
                }
                return -1L;
            } else if (!this.mHandler.hasMessages(1) && !this.mHandler.hasMessages(2)) {
                return 0L;
            } else {
                long now = SystemClock.uptimeMillis();
                long timeoutTime = keyEvent.getDownTime() + 380 + 20;
                if (now >= timeoutTime) {
                    return 0L;
                }
                return timeoutTime - now;
            }
        }
        return 0L;
    }

    public boolean shouldInterceptKey(KeyEvent event) {
        if (isUserSetupComplete() && !this.mIsGameMode) {
            if (isScreenShotKey(event)) {
                return interceptScreenShotKey(event);
            }
            if (!isQuickNoteKey(event)) {
                return false;
            }
            return interceptQuickNoteKey(event);
        }
        return false;
    }

    private boolean interceptQuickNoteKey(KeyEvent event) {
        boolean isUp = event.getAction() == 1;
        if (isUp) {
            this.mHandler.sendEmptyMessage(4);
            removeMessageIfHas(1);
            return this.mQuickNoteKeyFunctionTriggered;
        }
        this.mQuickNoteKeyFunctionTriggered = false;
        if (this.mIsScreenOn) {
            return interceptQuickNoteKeyDownWhenScreenOn();
        }
        return interceptQuickNoteKeyDownWhenScreenOff();
    }

    private boolean interceptQuickNoteKeyDownWhenScreenOff() {
        if (this.mIsSupportQuickNoteWhenScreenOff) {
            this.mHandler.sendEmptyMessage(5);
            return false;
        }
        return false;
    }

    private boolean interceptQuickNoteKeyDownWhenScreenOn() {
        WindowManagerPolicy.WindowState windowState = this.mFocusedWindow;
        if (windowState == null) {
            Slog.i(TAG, "focus window is null");
            return false;
        } else if (NOTE_PACKAGE_NAME.contains(windowState.getOwningPackage())) {
            Slog.i(TAG, "focus window is notes, so long press page down disable");
            return false;
        } else {
            this.mHandler.sendEmptyMessageDelayed(1, 380L);
            return false;
        }
    }

    private boolean interceptScreenShotKey(KeyEvent event) {
        WindowManager.LayoutParams attrs;
        boolean z = true;
        if (event.getAction() != 1) {
            z = false;
        }
        boolean isUp = z;
        if (isUp) {
            this.mHandler.removeMessages(2);
            return this.mScreenShotKeyFunctionTriggered;
        }
        this.mScreenShotKeyFunctionTriggered = false;
        WindowManagerPolicy.WindowState windowState = this.mFocusedWindow;
        if (windowState == null || (attrs = (WindowManager.LayoutParams) ReflectionUtils.callPrivateMethod(windowState, "getAttrs", new Object[0])) == null || (attrs.flags & 8192) == 0) {
            this.mHandler.sendEmptyMessageDelayed(2, 380L);
            return false;
        }
        Slog.d(TAG, "focus window is secure, so long press page up disable");
        return false;
    }

    public void addStylusMaskWindow() {
        if (!this.mIsScreenOn) {
            Slog.w(TAG, "Can't add stylus mask window because screen is not on");
            return;
        }
        this.mScene = SCENE_APP;
        String owningPackage = null;
        WindowManagerPolicy.WindowState windowState = this.mFocusedWindow;
        if (windowState != null) {
            owningPackage = windowState.getOwningPackage();
        }
        if (((KeyguardManager) this.mContext.getSystemService(KeyguardManager.class)).isKeyguardLocked() && "com.android.systemui".equals(owningPackage)) {
            this.mScene = SCENE_KEYGUARD;
        } else if ("com.miui.home".equals(owningPackage)) {
            this.mScene = SCENE_HOME;
        }
        Slog.i(TAG, "request add stylus mask");
        this.mInputSettingsConnection.sendMessageToInputSettings(1);
        this.mIsRequestMaskShow = true;
    }

    public void removeStylusMaskWindow(boolean animation) {
        Slog.i(TAG, "request remove stylus mask, mIsRequestMaskShow = " + this.mIsRequestMaskShow);
        if (!this.mIsRequestMaskShow) {
            return;
        }
        this.mInputSettingsConnection.sendMessageToInputSettings(2, animation ? 1 : 0);
        this.mIsRequestMaskShow = false;
    }

    private void removeMessageIfHas(int what) {
        if (this.mHandler.hasMessages(what)) {
            this.mHandler.removeMessages(what);
        }
    }

    private boolean isUserSetupComplete() {
        if (!this.mIsUserSetupComplete) {
            boolean isUserSetUp = isUserSetUp();
            this.mIsUserSetupComplete = isUserSetUp;
            return isUserSetUp;
        }
        return true;
    }

    public boolean needInterceptBeforeDispatching(KeyEvent event) {
        return isUserSetupComplete() && !this.mIsGameMode && (isScreenShotKey(event) || isQuickNoteKey(event));
    }

    public void updateScreenState(boolean screenOn) {
        this.mIsScreenOn = screenOn;
        if (screenOn) {
            return;
        }
        this.mHandler.sendEmptyMessage(3);
    }

    public void onDefaultDisplayFocusChangedLw(WindowManagerPolicy.WindowState newFocus) {
        this.mFocusedWindow = newFocus;
    }

    private boolean isUserSetUp() {
        return Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "user_setup_complete", 0, -2) != 0;
    }

    private void takePartialScreenshot() {
        if (!this.mIsScreenOn) {
            Slog.w(TAG, "Can't take screenshot because screen is not on");
        } else {
            ShortCutActionsUtils.getInstance(this.mContext).triggerFunction("stylus_partial_screenshot", "long_press_page_up_key", null, false);
        }
    }

    private void launchNote() {
        Bundle bundle = new Bundle();
        bundle.putString("scene", this.mScene);
        ShortCutActionsUtils.getInstance(this.mContext).triggerFunction("note", "stylus", bundle, true);
    }

    private void triggerQuickNoteKeyFunction() {
        launchNote();
    }

    public void triggerScreenShotKeyFunction() {
        takePartialScreenshot();
    }

    public void triggerNoteWhenScreenOff() {
        this.mScene = SCENE_OFF_SCREEN;
        launchNote();
    }

    private static boolean isQuickNoteKey(KeyEvent event) {
        int keyCode = event.getKeyCode();
        InputDevice device = event.getDevice();
        return keyCode == 93 && device != null && device.isXiaomiStylus() > 0;
    }

    private static boolean isScreenShotKey(KeyEvent event) {
        int keyCode = event.getKeyCode();
        InputDevice device = event.getDevice();
        return keyCode == 92 && device != null && device.isXiaomiStylus() > 0;
    }

    public void onUserSwitch(int newUserId) {
        updateGameModeSettings();
    }

    public void updateGameModeSettings() {
        boolean z = false;
        if (Settings.Secure.getInt(this.mContext.getContentResolver(), KEY_GAME_BOOSTER, 0) == 1) {
            z = true;
        }
        this.mIsGameMode = z;
    }

    public void dump(String prefix, PrintWriter pw) {
        pw.print("    ");
        pw.println(TAG);
        pw.print(prefix);
        pw.print("mScene=");
        pw.println(this.mScene);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class H extends Handler {
        private static final int MSG_KEY_DISMISS_WITHOUT_ANIMATION = 3;
        private static final int MSG_KEY_DISMISS_WITH_ANIMATION = 4;
        private static final int MSG_OFF_SCREEN_QUICK_NOTE_KEY_PRESS_ACTION = 5;
        private static final int MSG_QUICK_NOTE_KEY_LONG_PRESS_ACTION = 1;
        private static final int MSG_SCREEN_SHOT_KEY_LONG_PRESS_ACTION = 2;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public H(Looper looper) {
            super(looper);
            MiuiStylusPageKeyListener.this = r1;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    MiuiStylusPageKeyListener.this.mQuickNoteKeyFunctionTriggered = true;
                    MiuiStylusPageKeyListener.this.addStylusMaskWindow();
                    return;
                case 2:
                    MiuiStylusPageKeyListener.this.mScreenShotKeyFunctionTriggered = true;
                    MiuiStylusPageKeyListener.this.triggerScreenShotKeyFunction();
                    return;
                case 3:
                    MiuiStylusPageKeyListener.this.removeStylusMaskWindow(false);
                    return;
                case 4:
                    MiuiStylusPageKeyListener.this.removeStylusMaskWindow(true);
                    return;
                case 5:
                    MiuiStylusPageKeyListener.this.mQuickNoteKeyFunctionTriggered = true;
                    MiuiStylusPageKeyListener.this.triggerNoteWhenScreenOff();
                    return;
                default:
                    return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class MiuiSettingsObserver extends ContentObserver {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        MiuiSettingsObserver(Handler handler) {
            super(handler);
            MiuiStylusPageKeyListener.this = this$0;
        }

        void observe() {
            MiuiStylusPageKeyListener.this.updateGameModeSettings();
            ContentResolver resolver = MiuiStylusPageKeyListener.this.mContext.getContentResolver();
            resolver.registerContentObserver(Settings.Secure.getUriFor(MiuiStylusPageKeyListener.KEY_GAME_BOOSTER), false, this, -1);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            if (Settings.Secure.getUriFor(MiuiStylusPageKeyListener.KEY_GAME_BOOSTER).equals(uri)) {
                MiuiStylusPageKeyListener.this.updateGameModeSettings();
                Slog.d(MiuiStylusPageKeyListener.TAG, "game_booster changed, isGameMode = " + MiuiStylusPageKeyListener.this.mIsGameMode);
            }
        }
    }
}
