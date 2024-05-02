package com.android.server.input.shoulderkey;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Slog;
import android.view.KeyEvent;
import android.view.ViewConfiguration;
import com.android.server.MiuiBatteryStatsService;
import com.android.server.MiuiBgThread;
import com.android.server.input.shoulderkey.MiuiShoulderKeyShortcutListener;
import com.miui.server.input.util.ShortCutActionsUtils;
import miui.os.Build;
/* loaded from: classes.dex */
public class MiuiShoulderKeyShortcutListener {
    private static final int ENABLE_SHOULDER_KEY_PRESS_INTERVAL = 300;
    private static final String TAG = "MiuiShoulderKeyShortcutListener";
    private Context mContext;
    private boolean mDoNotShowDialogAgain;
    private H mHandler;
    private boolean mLeftShoulderKeyDoubleClickTriggered;
    private long mLeftShoulderKeyLastDownTime;
    private boolean mLeftShoulderKeyLongPressTriggered;
    private int mLeftShoulderKeyPressedCount;
    private MiuiSettingsObserver mMiuiSettingsObserver;
    private PowerManager mPowerManager;
    private boolean mRightShoulderKeyDoubleClickTriggered;
    private long mRightShoulderKeyLastDownTime;
    private boolean mRightShoulderKeyLongPressTriggered;
    private int mRightShoulderKeyPressedCount;
    private PowerManager.WakeLock mShoulderKeyWakeLock;
    private final int mKeyLongPressTimeout = ViewConfiguration.getLongPressTimeout();
    private String mLeftShoulderKeySingleClickFunction = getFunction("left_shoulder_key_single_click");
    private String mLeftShoulderKeyDoubleClickFunction = getFunction("left_shoulder_key_double_click");
    private String mLeftShoulderKeyLongPressFunction = getFunction("left_shoulder_key_long_press");
    private String mRightShoulderKeySingleClickFunction = getFunction("right_shoulder_key_single_click");
    private String mRightShoulderKeyDoubleClickFunction = getFunction("right_shoulder_key_double_click");
    private String mRightShoulderKeyLongPressFunction = getFunction("right_shoulder_key_long_press");

    public MiuiShoulderKeyShortcutListener(Context context) {
        this.mContext = context;
        this.mHandler = new H(context.getMainLooper());
        MiuiSettingsObserver miuiSettingsObserver = new MiuiSettingsObserver(this.mHandler);
        this.mMiuiSettingsObserver = miuiSettingsObserver;
        miuiSettingsObserver.observe();
        this.mDoNotShowDialogAgain = Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "do_not_show_shoulder_key_shortcut_prompt", 0, -2) == 1;
        PowerManager powerManager = (PowerManager) this.mContext.getSystemService("power");
        this.mPowerManager = powerManager;
        this.mShoulderKeyWakeLock = powerManager.newWakeLock(1, "PhoneWindowManager.mShoulderKeyWakeLock");
    }

    public String getFunction(String action) {
        String function = MiuiSettings.Key.getKeyAndGestureShortcutFunction(this.mContext, action);
        if (TextUtils.isEmpty(function)) {
            return "none";
        }
        return function;
    }

    public void handleShoulderKeyShortcut(KeyEvent event) {
        if (promptUser(event)) {
            this.mHandler.sendEmptyMessage(7);
        }
        this.mShoulderKeyWakeLock.acquire(1000L);
        if (event.getKeyCode() == 131) {
            if (event.getAction() == 0) {
                this.mHandler.sendEmptyMessageDelayed(3, this.mKeyLongPressTimeout);
                handleLeftShoulderKeyDoubleClickAction();
            } else if (event.getAction() == 1 && this.mHandler.hasMessages(3)) {
                this.mHandler.removeMessages(3);
            }
        } else if (event.getKeyCode() == 132) {
            if (event.getAction() == 0) {
                this.mHandler.sendEmptyMessageDelayed(6, this.mKeyLongPressTimeout);
                handleRightShoulderKeyDoubleClickAction();
            } else if (event.getAction() == 1 && this.mHandler.hasMessages(6)) {
                this.mHandler.removeMessages(6);
            }
        }
    }

    private boolean promptUser(KeyEvent event) {
        if (!this.mPowerManager.isScreenOn() || this.mDoNotShowDialogAgain) {
            return false;
        }
        return (event.getKeyCode() == 131 && event.getAction() == 0) ? isEmpty(this.mLeftShoulderKeyDoubleClickFunction) && isEmpty(this.mLeftShoulderKeyLongPressFunction) : event.getKeyCode() == 132 && event.getAction() == 0 && isEmpty(this.mRightShoulderKeyDoubleClickFunction) && isEmpty(this.mRightShoulderKeyLongPressFunction);
    }

    public boolean isEmpty(String function) {
        return TextUtils.isEmpty(function) || "none".equals(function);
    }

    private void handleLeftShoulderKeyDoubleClickAction() {
        long now = SystemClock.elapsedRealtime();
        if (now - this.mLeftShoulderKeyLastDownTime < 300) {
            this.mLeftShoulderKeyPressedCount++;
        } else {
            this.mLeftShoulderKeyPressedCount = 1;
            this.mLeftShoulderKeyLastDownTime = now;
        }
        if (this.mLeftShoulderKeyPressedCount == 2) {
            if (this.mHandler.hasMessages(1)) {
                this.mHandler.removeMessages(1);
            }
            this.mHandler.sendEmptyMessage(2);
        }
    }

    private void handleRightShoulderKeyDoubleClickAction() {
        long now = SystemClock.elapsedRealtime();
        if (now - this.mRightShoulderKeyLastDownTime < 300) {
            this.mRightShoulderKeyPressedCount++;
        } else {
            this.mRightShoulderKeyPressedCount = 1;
            this.mRightShoulderKeyLastDownTime = now;
        }
        if (this.mRightShoulderKeyPressedCount == 2) {
            if (this.mHandler.hasMessages(4)) {
                this.mHandler.removeMessages(4);
            }
            this.mHandler.sendEmptyMessage(5);
        }
    }

    public void updateSettings() {
        this.mMiuiSettingsObserver.update();
    }

    /* loaded from: classes.dex */
    public class H extends Handler {
        static final int MSG_LEFT_SHOULDER_KEY_DOUBLE_CLICK = 2;
        static final int MSG_LEFT_SHOULDER_KEY_LONG_PRESS = 3;
        static final int MSG_LEFT_SHOULDER_KEY_SINGLE_CLICK = 1;
        static final int MSG_RIGHT_SHOULDER_KEY_DOUBLE_CLICK = 5;
        static final int MSG_RIGHT_SHOULDER_KEY_LONG_PRESS = 6;
        static final int MSG_RIGHT_SHOULDER_KEY_SINGLE_CLICK = 4;
        static final int MSG_SHOW_DIALOG = 7;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public H(Looper looper) {
            super(looper);
            MiuiShoulderKeyShortcutListener.this = r1;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    if (!MiuiShoulderKeyShortcutListener.this.mLeftShoulderKeyLongPressTriggered && !MiuiShoulderKeyShortcutListener.this.mLeftShoulderKeyDoubleClickTriggered) {
                        triggerFunction(MiuiShoulderKeyShortcutListener.this.mLeftShoulderKeySingleClickFunction, "left_shoulder_key_single_click");
                        return;
                    }
                    MiuiShoulderKeyShortcutListener.this.mLeftShoulderKeyLongPressTriggered = false;
                    MiuiShoulderKeyShortcutListener.this.mLeftShoulderKeyDoubleClickTriggered = false;
                    return;
                case 2:
                    MiuiShoulderKeyShortcutListener.this.mLeftShoulderKeyDoubleClickTriggered = true;
                    triggerFunction(MiuiShoulderKeyShortcutListener.this.mLeftShoulderKeyDoubleClickFunction, "left_shoulder_key_double_click");
                    return;
                case 3:
                    MiuiShoulderKeyShortcutListener.this.mLeftShoulderKeyLongPressTriggered = true;
                    triggerFunction(MiuiShoulderKeyShortcutListener.this.mLeftShoulderKeyLongPressFunction, "left_shoulder_key_long_press");
                    return;
                case 4:
                    if (!MiuiShoulderKeyShortcutListener.this.mRightShoulderKeyLongPressTriggered && !MiuiShoulderKeyShortcutListener.this.mRightShoulderKeyDoubleClickTriggered) {
                        triggerFunction(MiuiShoulderKeyShortcutListener.this.mRightShoulderKeySingleClickFunction, "right_shoulder_key_single_click");
                        return;
                    }
                    MiuiShoulderKeyShortcutListener.this.mRightShoulderKeyLongPressTriggered = false;
                    MiuiShoulderKeyShortcutListener.this.mRightShoulderKeyDoubleClickTriggered = false;
                    return;
                case 5:
                    MiuiShoulderKeyShortcutListener.this.mRightShoulderKeyDoubleClickTriggered = true;
                    triggerFunction(MiuiShoulderKeyShortcutListener.this.mRightShoulderKeyDoubleClickFunction, "right_shoulder_key_double_click");
                    return;
                case 6:
                    MiuiShoulderKeyShortcutListener.this.mRightShoulderKeyLongPressTriggered = true;
                    triggerFunction(MiuiShoulderKeyShortcutListener.this.mRightShoulderKeyLongPressFunction, "right_shoulder_key_long_press");
                    return;
                case 7:
                    sendShowPromptDialogBroadcast();
                    return;
                default:
                    return;
            }
        }

        private void triggerFunction(String function, String action) {
            if (MiuiShoulderKeyShortcutListener.this.isEmpty(function)) {
                return;
            }
            if (!MiuiShoulderKeyShortcutListener.this.mPowerManager.isScreenOn()) {
                PowerManager.WakeLock wakeLock = MiuiShoulderKeyShortcutListener.this.mPowerManager.newWakeLock(268435466, "bright");
                wakeLock.acquire();
                wakeLock.release();
            }
            ShortCutActionsUtils.getInstance(MiuiShoulderKeyShortcutListener.this.mContext).triggerFunction(function, action, null, false);
            ShoulderKeyShortcutOneTrack.reportShoulderKeyShortcutOneTrack(MiuiShoulderKeyShortcutListener.this.mContext, action, function);
        }

        private void sendShowPromptDialogBroadcast() {
            Intent intent = new Intent();
            intent.setAction("com.miui.shoulderkey.shortcut");
            intent.setPackage("com.android.settings");
            MiuiShoulderKeyShortcutListener.this.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class MiuiSettingsObserver extends ContentObserver {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        MiuiSettingsObserver(Handler handler) {
            super(handler);
            MiuiShoulderKeyShortcutListener.this = this$0;
        }

        void observe() {
            ContentResolver resolver = MiuiShoulderKeyShortcutListener.this.mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor("left_shoulder_key_single_click"), false, this, -1);
            resolver.registerContentObserver(Settings.System.getUriFor("left_shoulder_key_double_click"), false, this, -1);
            resolver.registerContentObserver(Settings.System.getUriFor("left_shoulder_key_long_press"), false, this, -1);
            resolver.registerContentObserver(Settings.System.getUriFor("right_shoulder_key_single_click"), false, this, -1);
            resolver.registerContentObserver(Settings.System.getUriFor("right_shoulder_key_double_click"), false, this, -1);
            resolver.registerContentObserver(Settings.System.getUriFor("right_shoulder_key_long_press"), false, this, -1);
            resolver.registerContentObserver(Settings.Secure.getUriFor("do_not_show_shoulder_key_shortcut_prompt"), false, this, -1);
        }

        void update() {
            onChange(false, Settings.System.getUriFor("left_shoulder_key_single_click"));
            onChange(false, Settings.System.getUriFor("left_shoulder_key_double_click"));
            onChange(false, Settings.System.getUriFor("left_shoulder_key_long_press"));
            onChange(false, Settings.System.getUriFor("right_shoulder_key_single_click"));
            onChange(false, Settings.System.getUriFor("right_shoulder_key_double_click"));
            onChange(false, Settings.System.getUriFor("right_shoulder_key_long_press"));
            onChange(false, Settings.Secure.getUriFor("do_not_show_shoulder_key_shortcut_prompt"));
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            if (Settings.System.getUriFor("left_shoulder_key_single_click").equals(uri)) {
                MiuiShoulderKeyShortcutListener miuiShoulderKeyShortcutListener = MiuiShoulderKeyShortcutListener.this;
                miuiShoulderKeyShortcutListener.mLeftShoulderKeySingleClickFunction = miuiShoulderKeyShortcutListener.getFunction("left_shoulder_key_single_click");
                Slog.d(MiuiShoulderKeyShortcutListener.TAG, "mLeftShoulderKeySingleClickFunction = " + MiuiShoulderKeyShortcutListener.this.mLeftShoulderKeySingleClickFunction);
            } else if (Settings.System.getUriFor("left_shoulder_key_double_click").equals(uri)) {
                MiuiShoulderKeyShortcutListener miuiShoulderKeyShortcutListener2 = MiuiShoulderKeyShortcutListener.this;
                miuiShoulderKeyShortcutListener2.mLeftShoulderKeyDoubleClickFunction = miuiShoulderKeyShortcutListener2.getFunction("left_shoulder_key_double_click");
                Slog.d(MiuiShoulderKeyShortcutListener.TAG, "mLeftShoulderKeyDoubleClickFunction = " + MiuiShoulderKeyShortcutListener.this.mLeftShoulderKeyDoubleClickFunction);
            } else if (Settings.System.getUriFor("left_shoulder_key_long_press").equals(uri)) {
                MiuiShoulderKeyShortcutListener miuiShoulderKeyShortcutListener3 = MiuiShoulderKeyShortcutListener.this;
                miuiShoulderKeyShortcutListener3.mLeftShoulderKeyLongPressFunction = miuiShoulderKeyShortcutListener3.getFunction("left_shoulder_key_long_press");
                Slog.d(MiuiShoulderKeyShortcutListener.TAG, "mLeftShoulderKeyLongPressFunction = " + MiuiShoulderKeyShortcutListener.this.mLeftShoulderKeyLongPressFunction);
            } else if (Settings.System.getUriFor("right_shoulder_key_single_click").equals(uri)) {
                MiuiShoulderKeyShortcutListener miuiShoulderKeyShortcutListener4 = MiuiShoulderKeyShortcutListener.this;
                miuiShoulderKeyShortcutListener4.mRightShoulderKeySingleClickFunction = miuiShoulderKeyShortcutListener4.getFunction("right_shoulder_key_single_click");
                Slog.d(MiuiShoulderKeyShortcutListener.TAG, "mRightShoulderKeySingleClickFunction = " + MiuiShoulderKeyShortcutListener.this.mRightShoulderKeySingleClickFunction);
            } else if (Settings.System.getUriFor("right_shoulder_key_double_click").equals(uri)) {
                MiuiShoulderKeyShortcutListener miuiShoulderKeyShortcutListener5 = MiuiShoulderKeyShortcutListener.this;
                miuiShoulderKeyShortcutListener5.mRightShoulderKeyDoubleClickFunction = miuiShoulderKeyShortcutListener5.getFunction("right_shoulder_key_double_click");
                Slog.d(MiuiShoulderKeyShortcutListener.TAG, "mRightShoulderKeyDoubleClickFunction = " + MiuiShoulderKeyShortcutListener.this.mRightShoulderKeyDoubleClickFunction);
            } else if (Settings.System.getUriFor("right_shoulder_key_long_press").equals(uri)) {
                MiuiShoulderKeyShortcutListener miuiShoulderKeyShortcutListener6 = MiuiShoulderKeyShortcutListener.this;
                miuiShoulderKeyShortcutListener6.mRightShoulderKeyLongPressFunction = miuiShoulderKeyShortcutListener6.getFunction("right_shoulder_key_long_press");
                Slog.d(MiuiShoulderKeyShortcutListener.TAG, "mRightShoulderKeyLongPressFunction = " + MiuiShoulderKeyShortcutListener.this.mRightShoulderKeyLongPressFunction);
            } else if (Settings.Secure.getUriFor("do_not_show_shoulder_key_shortcut_prompt").equals(uri)) {
                MiuiShoulderKeyShortcutListener miuiShoulderKeyShortcutListener7 = MiuiShoulderKeyShortcutListener.this;
                boolean z = false;
                if (Settings.Secure.getIntForUser(miuiShoulderKeyShortcutListener7.mContext.getContentResolver(), "do_not_show_shoulder_key_shortcut_prompt", 0, -2) == 1) {
                    z = true;
                }
                miuiShoulderKeyShortcutListener7.mDoNotShowDialogAgain = z;
                Slog.d(MiuiShoulderKeyShortcutListener.TAG, "mDoNotShowDialogAgain = " + MiuiShoulderKeyShortcutListener.this.mDoNotShowDialogAgain);
            }
        }
    }

    /* loaded from: classes.dex */
    public static class ShoulderKeyShortcutOneTrack {
        private static final String EXTRA_APP_ID = "31000000481";
        private static final String EXTRA_EVENT_NAME = "shortcut";
        private static final String EXTRA_PACKAGE_NAME = "com.xiaomi.shoulderkey";
        private static final int FLAG_NON_ANONYMOUS = 2;
        private static final String INTENT_ACTION_ONETRACK = "onetrack.action.TRACK_EVENT";
        private static final String INTENT_PACKAGE_ONETRACK = "com.miui.analytics";
        private static final String KEY_SHORTCUT_ACTION = "shortcut_action";
        private static final String KEY_SHORTCUT_FUNCTION = "shortcut_function";
        private static final String TAG = "ShoulderKeyShortcutOneTrack";

        private ShoulderKeyShortcutOneTrack() {
        }

        public static void reportShoulderKeyShortcutOneTrack(final Context context, final String action, final String function) {
            if (context == null || Build.IS_INTERNATIONAL_BUILD) {
                return;
            }
            MiuiBgThread.getHandler().post(new Runnable() { // from class: com.android.server.input.shoulderkey.MiuiShoulderKeyShortcutListener$ShoulderKeyShortcutOneTrack$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    MiuiShoulderKeyShortcutListener.ShoulderKeyShortcutOneTrack.lambda$reportShoulderKeyShortcutOneTrack$0(action, function, context);
                }
            });
        }

        public static /* synthetic */ void lambda$reportShoulderKeyShortcutOneTrack$0(String action, String function, Context context) {
            Intent intent = new Intent("onetrack.action.TRACK_EVENT");
            intent.setPackage("com.miui.analytics");
            intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_APP_ID, EXTRA_APP_ID);
            intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_EVENT_NAME, EXTRA_EVENT_NAME);
            intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_PACKAGE, EXTRA_PACKAGE_NAME);
            intent.putExtra(KEY_SHORTCUT_ACTION, action);
            intent.putExtra(KEY_SHORTCUT_FUNCTION, function);
            intent.setFlags(2);
            try {
                context.startServiceAsUser(intent, UserHandle.CURRENT);
            } catch (IllegalStateException e) {
                Slog.w(TAG, "Failed to upload ShoulderKey shortcut event.");
            } catch (SecurityException e2) {
                Slog.w(TAG, "Unable to start service.");
            }
        }
    }
}
