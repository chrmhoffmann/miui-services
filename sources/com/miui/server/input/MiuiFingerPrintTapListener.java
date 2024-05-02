package com.miui.server.input;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.MiuiSettings;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Slog;
import android.view.KeyEvent;
import android.view.MotionEvent;
import com.miui.server.input.gesture.MiuiGestureListener;
import com.miui.server.input.gesture.MiuiGestureMonitor;
import com.miui.server.input.util.ShortCutActionsUtils;
import java.io.PrintWriter;
import miui.util.FeatureParser;
/* loaded from: classes.dex */
public class MiuiFingerPrintTapListener {
    private static final int FINGERPRINT_PRODUCT = 2184;
    private static final int FINGERPRINT_VENDOR = 1638;
    private static final String TAG = "MiuiFingerPrintTapListener";
    private Context mContext;
    private int mCurrentUserId = -2;
    private String mDoubleTapSideFp;
    private H mHandler;
    private boolean mIsFeatureSupport;
    private boolean mIsRegisterListener;
    private boolean mIsSetupComplete;
    private MiuiFingerPrintTapGestureListener mMiuiFingerPrintTapGestureListener;
    private MiuiGestureMonitor mMiuiGestureMonitor;
    private MiuiSettingsObserver mMiuiSettingsObserver;

    public MiuiFingerPrintTapListener(Context context) {
        boolean z = FeatureParser.getBoolean("is_support_fingerprint_tap", false);
        this.mIsFeatureSupport = z;
        this.mContext = context;
        if (z) {
            initialize(context);
        }
    }

    private void initialize(Context context) {
        this.mHandler = new H();
        this.mDoubleTapSideFp = MiuiSettings.Key.getKeyAndGestureShortcutFunction(context, "fingerprint_double_tap");
        this.mIsSetupComplete = isUserSetUp();
        this.mMiuiFingerPrintTapGestureListener = new MiuiFingerPrintTapGestureListener();
        this.mMiuiGestureMonitor = MiuiGestureMonitor.getInstance(this.mContext);
        MiuiSettingsObserver miuiSettingsObserver = new MiuiSettingsObserver(this.mHandler);
        this.mMiuiSettingsObserver = miuiSettingsObserver;
        miuiSettingsObserver.observe();
        updatePrintTapState();
    }

    public boolean shouldInterceptSlideFpTapKey(KeyEvent event, boolean isScreenOn, boolean keyguard) {
        if (event.getKeyCode() == 98 && event.getDevice().getVendorId() == FINGERPRINT_VENDOR && event.getDevice().getProductId() == FINGERPRINT_PRODUCT) {
            if (event.getAction() == 1 && isScreenOn) {
                if (!this.mIsSetupComplete) {
                    boolean isUserSetUp = isUserSetUp();
                    this.mIsSetupComplete = isUserSetUp;
                    if (!isUserSetUp) {
                        return true;
                    }
                }
                if (keyguard) {
                    Slog.d(TAG, "Keyguard active, so fingerprint double tap can't trigger function");
                    return true;
                } else if (this.mMiuiFingerPrintTapGestureListener.isFingerPressing()) {
                    Slog.d(TAG, "finger pressing, so fingerprint double tap can't trigger function");
                    return true;
                } else if (!checkEmpty(this.mDoubleTapSideFp)) {
                    H h = this.mHandler;
                    h.sendMessage(h.obtainMessage(1, "fingerprint_double_tap"));
                }
            }
            return true;
        }
        return false;
    }

    public void toggleFpDoubleTap() {
        ShortCutActionsUtils.getInstance(this.mContext).triggerFunction(this.mDoubleTapSideFp, "fingerprint_double_tap", null, false);
    }

    public void onUserSwitch(int newUserId) {
        if (this.mCurrentUserId != newUserId) {
            this.mCurrentUserId = newUserId;
            updateSettings();
        }
    }

    private void updateSettings() {
        MiuiSettingsObserver miuiSettingsObserver = this.mMiuiSettingsObserver;
        if (miuiSettingsObserver != null) {
            miuiSettingsObserver.onChange(false, Settings.System.getUriFor("fingerprint_double_tap"));
        }
    }

    public void updatePrintTapState() {
        if (!checkEmpty(this.mDoubleTapSideFp) && !this.mIsRegisterListener) {
            this.mMiuiGestureMonitor.registerPointerEventListener(this.mMiuiFingerPrintTapGestureListener);
            this.mIsRegisterListener = true;
        } else if (checkEmpty(this.mDoubleTapSideFp) && this.mIsRegisterListener) {
            this.mMiuiGestureMonitor.unregisterPointerEventListener(this.mMiuiFingerPrintTapGestureListener);
            this.mIsRegisterListener = false;
        }
    }

    private boolean checkEmpty(String feature) {
        return TextUtils.isEmpty(feature) || "none".equals(feature);
    }

    private boolean isUserSetUp() {
        return Settings.Secure.getIntForUser(this.mContext.getContentResolver(), "user_setup_complete", 0, -2) != 0;
    }

    public void dump(String prefix, PrintWriter pw) {
        pw.print("    ");
        pw.println(TAG);
        pw.print(prefix);
        pw.print("mIsSetupComplete=");
        pw.println(this.mIsSetupComplete);
        pw.print(prefix);
        pw.print("mDoubleTapSideFp=");
        pw.println(this.mDoubleTapSideFp);
    }

    /* loaded from: classes.dex */
    public static class MiuiFingerPrintTapGestureListener implements MiuiGestureListener {
        private int mPointerCount;

        MiuiFingerPrintTapGestureListener() {
        }

        @Override // com.miui.server.input.gesture.MiuiGestureListener
        public void onPointerEvent(MotionEvent motionEvent) {
            switch (motionEvent.getActionMasked()) {
                case 1:
                case 3:
                    this.mPointerCount = 0;
                    return;
                case 2:
                default:
                    this.mPointerCount = motionEvent.getPointerCount();
                    return;
            }
        }

        public boolean isFingerPressing() {
            return this.mPointerCount > 0;
        }
    }

    /* loaded from: classes.dex */
    public class H extends Handler {
        static final int DOUBLE_TAP_SIDE_FP = 1;

        private H() {
            MiuiFingerPrintTapListener.this = r1;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                MiuiFingerPrintTapListener.this.toggleFpDoubleTap();
            }
        }
    }

    /* loaded from: classes.dex */
    public class MiuiSettingsObserver extends ContentObserver {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        MiuiSettingsObserver(Handler handler) {
            super(handler);
            MiuiFingerPrintTapListener.this = this$0;
        }

        void observe() {
            ContentResolver resolver = MiuiFingerPrintTapListener.this.mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor("fingerprint_double_tap"), false, this, -1);
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            if (Settings.System.getUriFor("fingerprint_double_tap").equals(uri)) {
                MiuiFingerPrintTapListener miuiFingerPrintTapListener = MiuiFingerPrintTapListener.this;
                miuiFingerPrintTapListener.mDoubleTapSideFp = Settings.System.getStringForUser(miuiFingerPrintTapListener.mContext.getContentResolver(), "fingerprint_double_tap", -2);
                Slog.d(MiuiFingerPrintTapListener.TAG, "fingerprint_drop_down changed, mDoubleTapSideFp = " + MiuiFingerPrintTapListener.this.mDoubleTapSideFp);
            }
            MiuiFingerPrintTapListener.this.updatePrintTapState();
        }
    }
}
