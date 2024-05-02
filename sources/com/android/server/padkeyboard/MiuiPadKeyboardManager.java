package com.android.server.padkeyboard;

import android.content.Context;
import android.hardware.SensorEventListener;
import android.util.Slog;
import android.view.InputDevice;
import java.io.PrintWriter;
import miui.hardware.input.MiuiKeyboardStatus;
/* loaded from: classes.dex */
public interface MiuiPadKeyboardManager extends SensorEventListener {
    public static final String TAG = "MiuiPadKeyboardManager";

    /* loaded from: classes.dex */
    public interface CommandCallback {
        boolean isCorrectPackage(byte[] bArr);
    }

    /* loaded from: classes.dex */
    public interface KeyboardAuthCallback {
        public static final int CHALLENGE_TYPE = 2;
        public static final int INIT_TYPE = 1;

        void respondKeyboard(byte[] bArr, int i);
    }

    byte[] commandMiAuthStep3Type1(byte[] bArr, byte[] bArr2);

    byte[] commandMiDevAuthInit();

    void dump(String str, PrintWriter printWriter);

    void enableOrDisableInputDevice();

    int getKeyboardDeviceId();

    void getKeyboardReportData();

    MiuiKeyboardStatus getKeyboardStatus();

    boolean isKeyboardReady();

    void notifyLidSwitchChanged(boolean z);

    void notifyScreenState(boolean z);

    void notifyTabletSwitchChanged(boolean z);

    void readKeyboardStatus();

    InputDevice[] removeKeyboardDevicesIfNeeded(InputDevice[] inputDeviceArr);

    static boolean isXiaomiKeyboard(int vendorId, int productId) {
        if (MiuiIICKeyboardManager.supportPadKeyboard()) {
            return MiuiIICKeyboardManager.isXiaomiKeyboard(vendorId, productId);
        }
        if (MiuiUsbKeyboardManager.supportPadKeyboard()) {
            return MiuiUsbKeyboardManager.isXiaomiKeyboard(vendorId, productId);
        }
        return false;
    }

    static int shouldClearActivityInfoFlags() {
        if (MiuiIICKeyboardManager.supportPadKeyboard()) {
            return MiuiIICKeyboardManager.shouldClearActivityInfoFlags();
        }
        if (MiuiUsbKeyboardManager.supportPadKeyboard()) {
            return MiuiUsbKeyboardManager.shouldClearActivityInfoFlags();
        }
        return 0;
    }

    default byte[] sendCommandForRespond(byte[] command, CommandCallback callback) {
        return new byte[0];
    }

    default void setCapsLockLight(boolean enable) {
    }

    default void readHallStatus() {
    }

    static MiuiPadKeyboardManager getKeyboardManager(Context context) {
        if (MiuiUsbKeyboardManager.supportPadKeyboard()) {
            return MiuiUsbKeyboardManager.getInstance(context);
        }
        if (MiuiIICKeyboardManager.supportPadKeyboard()) {
            return MiuiIICKeyboardManager.getInstance(context);
        }
        Slog.e(TAG, "notSupport any keyboard!");
        return null;
    }
}
