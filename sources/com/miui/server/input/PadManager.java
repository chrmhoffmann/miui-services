package com.miui.server.input;

import android.app.ActivityThread;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.hardware.display.DisplayManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.MathUtils;
import android.util.Slog;
import android.view.InputDevice;
import android.view.KeyEvent;
import com.android.server.ScoutHelper;
import com.android.server.input.InputOneTrackUtil;
import com.android.server.input.MiuiInputThread;
import com.android.server.padkeyboard.MiuiIICKeyboardManager;
import com.android.server.padkeyboard.MiuiPadKeyboardManager;
import com.android.server.wm.MiuiFreeformPinManagerService;
import java.util.ArrayList;
import miui.os.Build;
/* loaded from: classes.dex */
public class PadManager {
    private static final String IIC_MI_MEDIA_KEYBOARD_NAME = "Xiaomi Consumer";
    private static final String TAG = "PadManager";
    static ArrayList<Integer> sFNKeyCode;
    private static volatile PadManager sIntance;
    private volatile boolean mIsCapsLock;
    private boolean mIsUserSetup;
    private MiuiPadSettingsObserver mMiuiPadSettingsObserver;
    private boolean mRunning;
    private volatile boolean mIsLidOpen = true;
    private volatile boolean mIsTabletOpen = true;
    private final Object mLock = new Object();
    private final Context mContext = ActivityThread.currentActivityThread().getSystemContext();
    private final Handler mHandler = new H(MiuiInputThread.getHandler().getLooper());

    static {
        ArrayList<Integer> arrayList = new ArrayList<>();
        sFNKeyCode = arrayList;
        arrayList.add(87);
        sFNKeyCode.add(85);
        sFNKeyCode.add(88);
        sFNKeyCode.add(25);
        sFNKeyCode.add(164);
        sFNKeyCode.add(24);
        sFNKeyCode.add(220);
        sFNKeyCode.add(221);
    }

    private PadManager() {
    }

    public static PadManager getInstance() {
        if (sIntance == null) {
            synchronized (PadManager.class) {
                if (sIntance == null) {
                    sIntance = new PadManager();
                }
            }
        }
        return sIntance;
    }

    public boolean isPad() {
        return Build.IS_TABLET;
    }

    public void registerPadSettingsObserver() {
        MiuiPadSettingsObserver miuiPadSettingsObserver = new MiuiPadSettingsObserver(MiuiInputThread.getHandler());
        this.mMiuiPadSettingsObserver = miuiPadSettingsObserver;
        miuiPadSettingsObserver.observer();
    }

    public void setIsLidOpen(boolean isLidOpen) {
        this.mIsLidOpen = isLidOpen;
    }

    public void setIsTableOpen(boolean isTabletOpen) {
        this.mIsTabletOpen = isTabletOpen;
    }

    public boolean padLidInterceptWakeKey(KeyEvent event) {
        return isPad() && (!this.mIsLidOpen || !this.mIsTabletOpen) && isWakeKeyFromKeyboard(event);
    }

    private boolean isWakeKeyFromKeyboard(KeyEvent event) {
        InputDevice device = event.getDevice();
        return device != null && device.getProductId() == 148 && device.getVendorId() == 2087;
    }

    public boolean adjustBrightnessFromKeycode(KeyEvent event) {
        if (event.getKeyCode() == 220 || event.getKeyCode() == 221) {
            if (event.getAction() == 0) {
                this.mRunning = true;
                Message msg = this.mHandler.obtainMessage(0);
                Bundle bundle = new Bundle();
                bundle.putParcelable("keyEvent", event);
                bundle.putInt("delay", ScoutHelper.BINDER_FULL_KILL_SCORE_ADJ_MIN);
                msg.setData(bundle);
                this.mHandler.sendMessage(msg);
                if (this.mIsUserSetup) {
                    this.mContext.startActivityAsUser(new Intent("com.android.intent.action.SHOW_BRIGHTNESS_DIALOG"), null, UserHandle.CURRENT_OR_SELF);
                }
            } else {
                this.mRunning = false;
                this.mHandler.removeCallbacksAndMessages(null);
            }
            Slog.i(TAG, "handle brightness key for miui");
            return true;
        }
        this.mRunning = false;
        this.mHandler.removeCallbacksAndMessages(null);
        return false;
    }

    public void notifySystemBooted() {
        if (MiuiIICKeyboardManager.supportPadKeyboard()) {
            registerPadSettingsObserver();
            MiuiPadKeyboardManager.getKeyboardManager(this.mContext).readKeyboardStatus();
            MiuiPadKeyboardManager.getKeyboardManager(this.mContext).readHallStatus();
        }
    }

    public void statusMediaFunction(KeyEvent event) {
        if (event.getDevice() != null && IIC_MI_MEDIA_KEYBOARD_NAME.equals(event.getDevice().getName()) && sFNKeyCode.contains(Integer.valueOf(event.getKeyCode())) && event.getAction() == 0) {
            InputOneTrackUtil.getInstance(this.mContext).trackKeyboardEvent(String.valueOf(event.getKeyCode()));
        }
    }

    public boolean getCapsLockStatus() {
        return this.mIsCapsLock;
    }

    public void setCapsLockStatus(boolean isCapsLock) {
        this.mIsCapsLock = isCapsLock;
    }

    /* loaded from: classes.dex */
    public class MiuiPadSettingsObserver extends ContentObserver {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        MiuiPadSettingsObserver(Handler handler) {
            super(handler);
            PadManager.this = this$0;
        }

        void observer() {
            ContentResolver resolver = PadManager.this.mContext.getContentResolver();
            resolver.registerContentObserver(Settings.Secure.getUriFor("user_setup_complete"), false, this, -1);
            onChange(false, Settings.Secure.getUriFor("user_setup_complete"));
        }

        @Override // android.database.ContentObserver
        public void onChange(boolean selfChange, Uri uri) {
            ContentResolver resolver = PadManager.this.mContext.getContentResolver();
            if (Settings.Secure.getUriFor("user_setup_complete").equals(uri)) {
                PadManager padManager = PadManager.this;
                boolean z = false;
                if (Settings.Secure.getIntForUser(resolver, "user_setup_complete", 0, UserHandle.myUserId()) != 0) {
                    z = true;
                }
                padManager.mIsUserSetup = z;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class H extends Handler {
        private static final float BRIGHTNESS_STEP = 0.059f;
        private static final String DATA_DELAY_TIME = "delay";
        private static final String DATA_KEYEVENT = "keyEvent";

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        H(Looper looper) {
            super(looper);
            PadManager.this = this$0;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                int auto = Settings.System.getIntForUser(PadManager.this.mContext.getContentResolver(), "screen_brightness_mode", 0, -3);
                if (auto != 0) {
                    Settings.System.putIntForUser(PadManager.this.mContext.getContentResolver(), "screen_brightness_mode", 0, -3);
                }
                KeyEvent event = (KeyEvent) msg.getData().getParcelable(DATA_KEYEVENT);
                int delayTime = msg.getData().getInt(DATA_DELAY_TIME);
                DisplayManager displayManager = (DisplayManager) PadManager.this.mContext.getSystemService(DisplayManager.class);
                float nowBrightness = displayManager.getBrightness(0);
                float gammaValue = BrightnessUtils.convertLinearToGamma(nowBrightness);
                float linearValue = BrightnessUtils.convertGammaToLinear(event.getKeyCode() == 220 ? gammaValue - BRIGHTNESS_STEP : gammaValue + BRIGHTNESS_STEP);
                displayManager.setBrightness(0, linearValue);
                if (PadManager.this.mRunning) {
                    Message delayMsg = PadManager.this.mHandler.obtainMessage(0);
                    Bundle bundle = new Bundle();
                    bundle.putParcelable(DATA_KEYEVENT, event);
                    bundle.putInt(DATA_DELAY_TIME, 50);
                    delayMsg.setData(bundle);
                    PadManager.this.mHandler.sendMessageDelayed(delayMsg, delayTime);
                }
            }
        }
    }

    /* loaded from: classes.dex */
    private static class BrightnessUtils {
        private static final float A = 0.17883277f;
        private static final float B = 0.28466892f;
        private static final float C = 0.5599107f;
        private static final float R = 0.5f;

        private BrightnessUtils() {
        }

        public static final float convertGammaToLinear(float val) {
            float ret;
            if (val <= 0.5f) {
                ret = MathUtils.sq(val / 0.5f);
            } else {
                ret = MathUtils.exp((val - C) / A) + B;
            }
            float normalizedRet = MathUtils.constrain(ret, (float) MiuiFreeformPinManagerService.EDGE_AREA, 12.0f);
            return normalizedRet / 12.0f;
        }

        public static final float convertLinearToGamma(float val) {
            float normalizedVal = 12.0f * val;
            if (normalizedVal <= 1.0f) {
                float ret = MathUtils.sqrt(normalizedVal) * 0.5f;
                return ret;
            }
            float ret2 = C + (MathUtils.log(normalizedVal - B) * A);
            return ret2;
        }
    }
}
