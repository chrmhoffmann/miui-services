package com.android.server.padkeyboard;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Slog;
import com.android.server.padkeyboard.MiuiPadKeyboardManager;
import com.xiaomi.devauth.IMiDevAuthInterface;
/* loaded from: classes.dex */
public class KeyboardAuthHelper {
    private static final String CLASS_NAME = "com.xiaomi.devauth.MiDevAuthService";
    public static final int INTERNAL_ERROR_LIMIT = 3;
    public static final int KEYBOARD_AUTH_OK = 0;
    public static final int KEYBOARD_IDENTITY_RETRY_TIME = 5000;
    public static final int KEYBOARD_INTERNAL_ERROR = 3;
    public static final int KEYBOARD_NEED_CHECK_AGAIN = 2;
    public static final int KEYBOARD_REJECT = 1;
    public static final int KEYBOARD_TRANSFER_ERROR = 4;
    private static final String MIDEVAUTH_TAG = "MiuiKeyboardManager_MiDevAuthService";
    private static final String PACKAGE_NAME = "com.xiaomi.devauth";
    public static final int TRANSFER_ERROR_LIMIT = 2;
    private MiuiPadKeyboardManager.CommandCallback mChallengeCallback;
    private ServiceConnection mConn;
    private Context mContext;
    private IBinder.DeathRecipient mDeathRecipient;
    private MiuiPadKeyboardManager.CommandCallback mInitCallback;
    private IMiDevAuthInterface mService;
    private static int sTransferErrorCount = 0;
    private static int sInternalErrorCount = 0;

    private static void increaseTransferErrorCounter() {
        sTransferErrorCount++;
    }

    private static void increaseInternalErrorCounter() {
        sInternalErrorCount++;
    }

    private static void resetTransferErrorCounter() {
        sTransferErrorCount = 0;
    }

    private static void resetInternalErrorCounter() {
        sInternalErrorCount = 0;
    }

    /* loaded from: classes.dex */
    public static class KeyboardAuthInstance {
        private static final KeyboardAuthHelper INSTANCE = new KeyboardAuthHelper();

        private KeyboardAuthInstance() {
        }
    }

    private KeyboardAuthHelper() {
        this.mContext = null;
        this.mService = null;
        this.mInitCallback = new MiuiPadKeyboardManager.CommandCallback() { // from class: com.android.server.padkeyboard.KeyboardAuthHelper$$ExternalSyntheticLambda0
            @Override // com.android.server.padkeyboard.MiuiPadKeyboardManager.CommandCallback
            public final boolean isCorrectPackage(byte[] bArr) {
                return KeyboardAuthHelper.lambda$new$0(bArr);
            }
        };
        this.mChallengeCallback = new MiuiPadKeyboardManager.CommandCallback() { // from class: com.android.server.padkeyboard.KeyboardAuthHelper$$ExternalSyntheticLambda1
            @Override // com.android.server.padkeyboard.MiuiPadKeyboardManager.CommandCallback
            public final boolean isCorrectPackage(byte[] bArr) {
                return KeyboardAuthHelper.lambda$new$1(bArr);
            }
        };
        this.mDeathRecipient = new IBinder.DeathRecipient() { // from class: com.android.server.padkeyboard.KeyboardAuthHelper.1
            @Override // android.os.IBinder.DeathRecipient
            public void binderDied() {
                if (KeyboardAuthHelper.this.mService == null) {
                    return;
                }
                Slog.d(KeyboardAuthHelper.MIDEVAUTH_TAG, "binderDied, unlink service");
                KeyboardAuthHelper.this.mService.asBinder().unlinkToDeath(KeyboardAuthHelper.this.mDeathRecipient, 0);
            }
        };
        this.mConn = new ServiceConnection() { // from class: com.android.server.padkeyboard.KeyboardAuthHelper.2
            @Override // android.content.ServiceConnection
            public void onServiceConnected(ComponentName name, IBinder service) {
                try {
                    service.linkToDeath(KeyboardAuthHelper.this.mDeathRecipient, 0);
                } catch (RemoteException e) {
                    Slog.e(KeyboardAuthHelper.MIDEVAUTH_TAG, "linkToDeath fail: " + e);
                }
                KeyboardAuthHelper.this.mService = IMiDevAuthInterface.Stub.asInterface(service);
                if (KeyboardAuthHelper.this.mService == null) {
                    Slog.e(KeyboardAuthHelper.MIDEVAUTH_TAG, "Try connect midevauth service fail");
                }
            }

            @Override // android.content.ServiceConnection
            public void onServiceDisconnected(ComponentName name) {
                if (KeyboardAuthHelper.this.mContext != null) {
                    Slog.i(KeyboardAuthHelper.MIDEVAUTH_TAG, "re-bind to MiDevAuth service");
                    KeyboardAuthHelper.this.mContext.unbindService(KeyboardAuthHelper.this.mConn);
                    KeyboardAuthHelper.this.initService();
                }
            }
        };
    }

    private void setContext(Context context) {
        this.mContext = context;
    }

    public static KeyboardAuthHelper getInstance(Context context) {
        Slog.i(MIDEVAUTH_TAG, "Init bind to MiDevAuth service");
        KeyboardAuthInstance.INSTANCE.setContext(context);
        KeyboardAuthInstance.INSTANCE.initService();
        return KeyboardAuthInstance.INSTANCE;
    }

    public static /* synthetic */ boolean lambda$new$0(byte[] recBuf) {
        Slog.i(MIDEVAUTH_TAG, "Init, get rsp:" + MiuiKeyboardUtil.Bytes2Hex(recBuf, recBuf.length));
        if (recBuf[5] != 26) {
            Slog.i(MIDEVAUTH_TAG, "Init, Wrong length:" + String.format("%02x", Byte.valueOf(recBuf[5])));
            return false;
        } else if (MiuiKeyboardUtil.checkSum(recBuf, 0, 32, recBuf[32])) {
            return true;
        } else {
            Slog.i(MIDEVAUTH_TAG, "MiDevAuth Init, Receive wrong checksum");
            return false;
        }
    }

    public static /* synthetic */ boolean lambda$new$1(byte[] recBuf) {
        Slog.i(MIDEVAUTH_TAG, "GetToken, rsp:" + MiuiKeyboardUtil.Bytes2Hex(recBuf, recBuf.length));
        if (recBuf[5] != 16) {
            Slog.i(MIDEVAUTH_TAG, "GetToken, Wrong length:" + String.format("%02x", Byte.valueOf(recBuf[5])));
            return false;
        } else if (MiuiKeyboardUtil.checkSum(recBuf, 0, 22, recBuf[22])) {
            return true;
        } else {
            Slog.i(MIDEVAUTH_TAG, "GetToken, Receive wrong checksum");
            return false;
        }
    }

    public void initService() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(PACKAGE_NAME, CLASS_NAME));
        if (!this.mContext.bindServiceAsUser(intent, this.mConn, 1, UserHandle.CURRENT)) {
            Slog.e(MIDEVAUTH_TAG, "cannot bind service: com.xiaomi.devauth.MiDevAuthService");
        }
    }

    public int checkKeyboardIdentity(MiuiPadKeyboardManager miuiPadKeyboardManager, boolean isFirst) {
        RemoteException e;
        Slog.i(MIDEVAUTH_TAG, "Begin check keyboardIdentity");
        if (isFirst) {
            resetTransferErrorCounter();
            resetInternalErrorCounter();
        }
        if (sTransferErrorCount > 2) {
            Slog.e(MIDEVAUTH_TAG, "Meet transfer error counter:" + sTransferErrorCount);
            return 4;
        } else if (sInternalErrorCount > 3) {
            Slog.e(MIDEVAUTH_TAG, "Meet internal error counter:" + sInternalErrorCount);
            return 3;
        } else {
            byte[] uid = new byte[16];
            byte[] keyMetaData1 = new byte[4];
            byte[] keyMetaData2 = new byte[4];
            byte[] initCommand = miuiPadKeyboardManager.commandMiDevAuthInit();
            byte[] r1 = miuiPadKeyboardManager.sendCommandForRespond(initCommand, this.mInitCallback);
            if (r1.length != 0) {
                System.arraycopy(r1, 8, uid, 0, 16);
                System.arraycopy(r1, 24, keyMetaData1, 0, 4);
                System.arraycopy(r1, 28, keyMetaData2, 0, 4);
                if (this.mService == null) {
                    initService();
                }
                IMiDevAuthInterface iMiDevAuthInterface = this.mService;
                if (iMiDevAuthInterface == null) {
                    Slog.e(MIDEVAUTH_TAG, "MiDevAuth Service is unavaiable!");
                    return 2;
                }
                try {
                    byte[] chooseKeyMeta = iMiDevAuthInterface.chooseKey(uid, keyMetaData1, keyMetaData2);
                    if (chooseKeyMeta.length == 4) {
                        try {
                            byte[] challenge = this.mService.getChallenge(16);
                            if (challenge.length != 16) {
                                increaseInternalErrorCounter();
                                Slog.e(MIDEVAUTH_TAG, "Get challenge from midevauth service fail!");
                                return 2;
                            }
                            byte[] challengeCommand = miuiPadKeyboardManager.commandMiAuthStep3Type1(chooseKeyMeta, challenge);
                            byte[] r2 = miuiPadKeyboardManager.sendCommandForRespond(challengeCommand, this.mChallengeCallback);
                            if (r2.length == 0) {
                                increaseTransferErrorCounter();
                                Slog.e(MIDEVAUTH_TAG, "MiDevAuth GetToken fail! Error counter:" + sTransferErrorCount);
                                return 2;
                            }
                            byte[] token = new byte[16];
                            System.arraycopy(r2, 6, token, 0, 16);
                            try {
                            } catch (RemoteException e2) {
                                e = e2;
                            }
                            try {
                                int result = this.mService.tokenVerify(1, uid, chooseKeyMeta, challenge, token);
                                if (result == 1) {
                                    Slog.i(MIDEVAUTH_TAG, "Check keyboard PASS with online key");
                                    resetTransferErrorCounter();
                                    resetInternalErrorCounter();
                                    return 0;
                                } else if (result == 2) {
                                    Slog.i(MIDEVAUTH_TAG, "Check keyboard PASS with offline key, need check again later");
                                    resetTransferErrorCounter();
                                    resetInternalErrorCounter();
                                    return 2;
                                } else if (result == 3) {
                                    resetTransferErrorCounter();
                                    increaseInternalErrorCounter();
                                    Slog.e(MIDEVAUTH_TAG, "Meet internal error when try check keyboard!");
                                    return 2;
                                } else {
                                    Slog.i(MIDEVAUTH_TAG, "Check keyboard Fail!");
                                    return 1;
                                }
                            } catch (RemoteException e3) {
                                e = e3;
                                increaseInternalErrorCounter();
                                Slog.e(MIDEVAUTH_TAG, "call token_verify fail: " + e);
                                return 2;
                            }
                        } catch (RemoteException e4) {
                            increaseInternalErrorCounter();
                            Slog.e(MIDEVAUTH_TAG, "call getChallenge fail: " + e4);
                            return 2;
                        }
                    }
                    Slog.e(MIDEVAUTH_TAG, "Choose KeyMeta from midevauth service fail!");
                    if (chooseKeyMeta.length == 0) {
                        return 1;
                    }
                    increaseInternalErrorCounter();
                    return 2;
                } catch (RemoteException e5) {
                    increaseInternalErrorCounter();
                    Slog.e(MIDEVAUTH_TAG, "call chooseKeyMeta fail: " + e5);
                    return 2;
                }
            }
            increaseTransferErrorCounter();
            Slog.e(MIDEVAUTH_TAG, "MiDevAuth Init fail! Error counter:" + sTransferErrorCount);
            return 2;
        }
    }
}
