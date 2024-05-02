package com.android.server.padkeyboard.iic;
/* loaded from: classes.dex */
public interface NanoSocketCallback {
    public static final int CALLBACK_TYPE_KB = 20;
    public static final int CALLBACK_TYPE_KB_DEV = 3;
    public static final int CALLBACK_TYPE_PAD_MCU = 10;
    public static final String OTA_ERROR_REASON_NO_SOCKET = "Socket linked exception";
    public static final String OTA_ERROR_REASON_NO_VALID = "Invalid upgrade file";
    public static final String OTA_ERROR_REASON_NO_VERSION = "The device version is not read or version doesn't need upgrading";
    public static final String OTA_ERROR_REASON_PACKAGE_NUM = "Accumulation and verification failed";
    public static final String OTA_ERROR_REASON_WRITE_SOCKET_EXCEPTION = "Write data Socket exception";
    public static final int OTA_STATE_TYPE_BEGIN = 0;
    public static final int OTA_STATE_TYPE_FAIL = 1;
    public static final int OTA_STATE_TYPE_SUCCESS = 2;

    void onAuthResult(byte[] bArr);

    void onHallStatusChanged(byte b);

    void onKeyState(int i, int i2);

    void onKeyStateData(int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8, int i9, int i10, int i11);

    void onKeyboardGSensorChanged(float f, float f2, float f3);

    void onKeyboardSleepStatusChanged(boolean z);

    void onKeyboardStateChanged(boolean z);

    void onOtaErrorInfo(int i, String str);

    void onOtaProgress(int i, float f);

    void onOtaStateChange(int i, int i2);

    void onReadSocketNumError(String str);

    void onStateData(int i, byte b, String str, int i2);

    void onUpdateVersion(int i, String str);

    void onWriteSocketErrorInfo(String str);

    void readyToCheckAuth();
}
