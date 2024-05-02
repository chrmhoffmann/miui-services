package com.android.server.padkeyboard.iic;

import android.content.Context;
import android.net.LocalSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.UserHandle;
import android.provider.MiuiSettings;
import android.util.Slog;
import android.widget.Toast;
import com.android.server.padkeyboard.MiuiKeyboardUtil;
import com.android.server.padkeyboard.iic.CommunicationUtil;
import com.android.server.padkeyboard.iic.IICNodeHelper;
import com.android.server.padkeyboard.usb.UsbKeyboardUtil;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
/* loaded from: classes.dex */
public class IICNodeHelper {
    public static final int BUFFER_SIZE = 100;
    public static final int KB_FLASH_UPGRADE = 2;
    public static final int KB_UPGRADE = 3;
    private static final String KEYBOARD_COLOR_BLACK = "black";
    private static final String KEYBOARD_COLOR_WHITE = "white";
    public static final int MCU_UPGRADE = 1;
    private static final String PROVIDER_KEYBOARD_COLOR = "miui_keyboard_color";
    private static final String TAG = "IIC_IICNodeHelper";
    private static volatile IICNodeHelper sInstance;
    private byte[] mAuthCommandResponse;
    private final CommunicationUtil mCommunicationUtil;
    private final Context mContext;
    private CountDownLatch mCountDownLatch;
    private volatile boolean mIsBinDataOK;
    private String mKeyBoardUpgradeFilePath;
    private KeyboardUpgradeUtil mKeyBoardUpgradeUtil;
    private CommandWorker mKeyUpgradeCommandWorker;
    private volatile String mKeyboardFlashVersion;
    private volatile String mKeyboardVersion;
    private CommandWorker mKeyboardVersionWorker;
    private final LocalSocket mLocalSocket;
    private CommandWorker mMCUUpgradeCommandWorker;
    private String mMCUUpgradeFilePath;
    private volatile String mMCUVersion;
    private CommandWorker mMCUVersionWorker;
    private McuUpgradeUtil mMcuUpgradeUtil;
    public NanoSocketCallback mNanoSocketCallback;
    private final RepeatSendCommandHandler mRepeatSendCommandHandler;
    private volatile int mReceiverBinIndex = -1;
    private boolean mShouldUpgradeMCU = true;
    private boolean mShouldUpgradeKeyboard = true;

    /* loaded from: classes.dex */
    public interface ResponseListener {
        void onCommandResponse();
    }

    public static IICNodeHelper getInstance(Context context) {
        if (sInstance == null) {
            synchronized (IICNodeHelper.class) {
                if (sInstance == null) {
                    sInstance = new IICNodeHelper(context);
                }
            }
        }
        return sInstance;
    }

    private IICNodeHelper(Context context) {
        this.mContext = context;
        CommunicationUtil communicationUtil = CommunicationUtil.getInstance();
        this.mCommunicationUtil = communicationUtil;
        communicationUtil.registerSocketCallback(new SocketCallbackListener());
        this.mMcuUpgradeUtil = new McuUpgradeUtil(context, null);
        this.mKeyBoardUpgradeUtil = new KeyboardUpgradeUtil(context, null);
        this.mLocalSocket = communicationUtil.getLocalSocket();
        HandlerThread thread = new HandlerThread("repeat_send_command");
        thread.start();
        this.mRepeatSendCommandHandler = new RepeatSendCommandHandler(thread.getLooper());
        initCommandWorker();
    }

    public void setOtaCallBack(NanoSocketCallback callBack) {
        this.mNanoSocketCallback = callBack;
        this.mCommunicationUtil.setOtaCallBack(callBack);
    }

    public void sendGetVersionCommand() {
        this.mMCUVersionWorker.insertCommandToQueue(this.mCommunicationUtil.getVersionCommand(CommunicationUtil.MCU_ADDRESS));
        this.mMCUVersionWorker.sendCommand(4);
    }

    public void sendRestoreMcuCommand() {
        this.mCommunicationUtil.sendRestoreMcuCommand();
    }

    public void startUpgrade() {
        upgradeKeyboard(CommunicationUtil.KEYBOARD_ADDRESS, this.mKeyBoardUpgradeFilePath);
    }

    public void setUpgradeFile(int type, String binPath) {
        if (binPath == null) {
            Slog.e(TAG, "Start UpgradeThread binPath is null");
        } else if (!this.mCommunicationUtil.getSocketStatus()) {
            Slog.e(TAG, "LocalSocket is stop when upgrade");
        } else if (type == 1) {
            this.mMCUUpgradeFilePath = binPath;
        } else if (type == 3) {
            this.mKeyBoardUpgradeFilePath = binPath;
        }
    }

    public void setKeyboardFeature(boolean enable, int featureType) {
        byte feature;
        if (!this.mCommunicationUtil.getSocketStatus()) {
            Slog.e(TAG, "LocalSocket is stop when setKeyboardFeature");
            return;
        }
        if (featureType == CommunicationUtil.KB_FEATURE.KB_ENABLE.getIndex()) {
            feature = CommunicationUtil.KB_FEATURE.KB_ENABLE.getCommand();
        } else if (featureType == CommunicationUtil.KB_FEATURE.KB_POWER.getIndex()) {
            feature = CommunicationUtil.KB_FEATURE.KB_POWER.getCommand();
        } else if (featureType == CommunicationUtil.KB_FEATURE.KB_CAPS_KEY.getIndex()) {
            feature = CommunicationUtil.KB_FEATURE.KB_CAPS_KEY.getCommand();
        } else if (featureType == CommunicationUtil.KB_FEATURE.KB_G_SENSOR.getIndex()) {
            feature = CommunicationUtil.KB_FEATURE.KB_G_SENSOR.getCommand();
        } else {
            return;
        }
        byte[] command = this.mCommunicationUtil.getLongRawCommand();
        this.mCommunicationUtil.setCommandHead(command);
        this.mCommunicationUtil.setSetKeyboardStatusCommand(command, feature, enable ? (byte) 1 : (byte) 0);
        this.mCommunicationUtil.writeSocketCmd(command);
        Slog.i(TAG, "set keyboard status command :: " + MiuiKeyboardUtil.Bytes2Hex(command, command.length));
    }

    public void readKeyboardStatus() {
        if (!this.mCommunicationUtil.getSocketStatus()) {
            Slog.e(TAG, "LocalSocket is stop when read KeyboardStatus");
            return;
        }
        byte[] command = this.mCommunicationUtil.getLongRawCommand();
        this.mCommunicationUtil.setCommandHead(command);
        this.mCommunicationUtil.setReadKeyboardCommand(command, CommunicationUtil.SEND_REPORT_ID_SHORT_DATA, (byte) 49, CommunicationUtil.PAD_ADDRESS, CommunicationUtil.KEYBOARD_ADDRESS, CommunicationUtil.COMMAND_READ_KB_STATUS);
        this.mCommunicationUtil.writeSocketCmd(command);
    }

    public byte[] checkAuth(byte[] command) {
        CountDownLatch countDownLatch = new CountDownLatch(2);
        this.mCountDownLatch = countDownLatch;
        countDownLatch.countDown();
        this.mCommunicationUtil.writeSocketCmd(command);
        try {
            this.mCountDownLatch.await(400L, TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Slog.i(TAG, exception.toString());
            Thread.currentThread().interrupt();
        }
        return this.mAuthCommandResponse;
    }

    public void checkMCUStatus() {
        byte[] command = this.mCommunicationUtil.getLongRawCommand();
        this.mCommunicationUtil.setCheckMCUStatusCommand(command, CommunicationUtil.COMMAND_CHECK_MCU_STATUS);
        Slog.d(TAG, "write checkMCUStatus");
        this.mCommunicationUtil.writeSocketCmd(command);
    }

    public void checkHallStatus() {
        byte[] command = this.mCommunicationUtil.getLongRawCommand();
        this.mCommunicationUtil.setGetHallStatusCommand(command);
        this.mCommunicationUtil.writeSocketCmd(command);
    }

    private void upgradeMCU(String binPath) {
        if (binPath == null) {
            this.mKeyUpgradeCommandWorker.emptyCommandResponse();
        } else if (this.mShouldUpgradeMCU) {
            McuUpgradeUtil upgradeFile = new McuUpgradeUtil(this.mContext, binPath);
            if (!upgradeFile.isValidFile()) {
                this.mNanoSocketCallback.onOtaErrorInfo(10, NanoSocketCallback.OTA_ERROR_REASON_NO_VALID);
            } else if (this.mLocalSocket == null) {
                this.mNanoSocketCallback.onOtaErrorInfo(10, NanoSocketCallback.OTA_ERROR_REASON_NO_SOCKET);
            } else {
                this.mMcuUpgradeUtil = upgradeFile;
                Slog.i(TAG, "Start upgrade MCU");
                this.mMCUUpgradeCommandWorker.insertCommandToQueue(this.mCommunicationUtil.getVersionCommand(CommunicationUtil.MCU_ADDRESS));
                this.mMCUUpgradeCommandWorker.insertCommandToQueue(this.mMcuUpgradeUtil.getUpgradeCommand());
                this.mMCUUpgradeCommandWorker.insertCommandToQueue(this.mMcuUpgradeUtil.getBinPackInfo(0));
                this.mMCUUpgradeCommandWorker.sendCommand(4);
            }
        } else {
            this.mMCUUpgradeCommandWorker.emptyCommandResponse();
        }
    }

    private void upgradeKeyboard(byte target, String binPath) {
        if (binPath == null) {
            this.mKeyUpgradeCommandWorker.emptyCommandResponse();
        } else if (this.mShouldUpgradeKeyboard) {
            KeyboardUpgradeUtil upgradeFile = new KeyboardUpgradeUtil(this.mContext, binPath);
            if (!upgradeFile.isValidFile()) {
                this.mNanoSocketCallback.onOtaErrorInfo(20, NanoSocketCallback.OTA_ERROR_REASON_NO_VALID);
            } else if (this.mLocalSocket == null) {
                this.mNanoSocketCallback.onOtaErrorInfo(20, NanoSocketCallback.OTA_ERROR_REASON_NO_SOCKET);
            } else {
                this.mKeyBoardUpgradeUtil = upgradeFile;
                Slog.i(TAG, "Start upgrade Keyboard");
                this.mKeyUpgradeCommandWorker.insertCommandToQueue(this.mCommunicationUtil.getVersionCommand(CommunicationUtil.KEYBOARD_ADDRESS));
                this.mKeyUpgradeCommandWorker.insertCommandToQueue(this.mKeyBoardUpgradeUtil.getUpgradeInfo(target));
                this.mKeyUpgradeCommandWorker.insertCommandToQueue(this.mKeyBoardUpgradeUtil.getBinPackInfo(target, 0));
                this.mKeyUpgradeCommandWorker.sendCommand(4);
            }
        } else {
            this.mKeyUpgradeCommandWorker.emptyCommandResponse();
        }
    }

    public void checkDataPkgSum(byte[] data, String reason) {
        if (!this.mCommunicationUtil.checkSum(data, 0, data[5] + 6, data[data[5] + 6])) {
            this.mNanoSocketCallback.onReadSocketNumError(NanoSocketCallback.OTA_ERROR_REASON_PACKAGE_NUM + reason);
        }
    }

    public void parseKeyboardStatus(byte[] data) {
        if (data[0] == 38 || data[0] == 36 || data[0] == 35) {
            if (data[18] == 0) {
                if ((data[9] & 99) != 35) {
                    this.mNanoSocketCallback.onKeyboardStateChanged(false);
                    if ((data[9] & 99) == 67) {
                        Slog.i(TAG, "Keyboard is connect,but Pogo pin is exception");
                        Context context = this.mContext;
                        Toast.makeText(context, context.getResources().getString(286196270), 0).show();
                        return;
                    } else if ((data[9] & 3) == 1) {
                        Slog.i(TAG, "Keyboard is connect,but TRX is exception or leather case coming");
                        return;
                    } else if ((data[9] & 3) == 0) {
                        Slog.i(TAG, "Keyboard is disConnect");
                        return;
                    } else {
                        Slog.e(TAG, "Other exception,Keyboard is disConnection");
                        return;
                    }
                }
                this.mNanoSocketCallback.onKeyboardStateChanged(true);
            } else if (data[18] == 1) {
                this.mNanoSocketCallback.onKeyboardStateChanged(false);
                Slog.i(TAG, "The keyboard power supply current exceeds the limit！");
            }
        }
    }

    private void initCommandWorker() {
        this.mKeyUpgradeCommandWorker = new CommandWorker(new ResponseListener() { // from class: com.android.server.padkeyboard.iic.IICNodeHelper$$ExternalSyntheticLambda0
            @Override // com.android.server.padkeyboard.iic.IICNodeHelper.ResponseListener
            public final void onCommandResponse() {
                IICNodeHelper.this.m1274x9cc45152();
            }
        });
        this.mMCUUpgradeCommandWorker = new CommandWorker(new ResponseListener() { // from class: com.android.server.padkeyboard.iic.IICNodeHelper$$ExternalSyntheticLambda1
            @Override // com.android.server.padkeyboard.iic.IICNodeHelper.ResponseListener
            public final void onCommandResponse() {
                IICNodeHelper.this.m1275x8e6df771();
            }
        });
        this.mMCUVersionWorker = new CommandWorker(new ResponseListener() { // from class: com.android.server.padkeyboard.iic.IICNodeHelper$$ExternalSyntheticLambda2
            @Override // com.android.server.padkeyboard.iic.IICNodeHelper.ResponseListener
            public final void onCommandResponse() {
                IICNodeHelper.this.m1276x80179d90();
            }
        });
        this.mKeyboardVersionWorker = new CommandWorker(new ResponseListener() { // from class: com.android.server.padkeyboard.iic.IICNodeHelper$$ExternalSyntheticLambda3
            @Override // com.android.server.padkeyboard.iic.IICNodeHelper.ResponseListener
            public final void onCommandResponse() {
                IICNodeHelper.this.m1277x71c143af();
            }
        });
    }

    /* renamed from: lambda$initCommandWorker$0$com-android-server-padkeyboard-iic-IICNodeHelper */
    public /* synthetic */ void m1274x9cc45152() {
        byte[] responseData = this.mKeyUpgradeCommandWorker.getCommandResponse();
        if (responseData[4] == -111) {
            if (!this.mIsBinDataOK) {
                int index = this.mKeyUpgradeCommandWorker.getReceiverPackageIndex() + 1;
                this.mKeyUpgradeCommandWorker.insertCommandToQueue(this.mKeyBoardUpgradeUtil.getBinPackInfo(CommunicationUtil.KEYBOARD_ADDRESS, index * 52));
            } else {
                this.mKeyUpgradeCommandWorker.insertCommandToQueue(this.mKeyBoardUpgradeUtil.getUpEndInfo(CommunicationUtil.KEYBOARD_ADDRESS));
                this.mKeyUpgradeCommandWorker.insertCommandToQueue(this.mKeyBoardUpgradeUtil.getUpFlashInfo(CommunicationUtil.KEYBOARD_ADDRESS));
                this.mIsBinDataOK = false;
            }
            this.mKeyUpgradeCommandWorker.sendCommand(4);
        } else if (this.mKeyUpgradeCommandWorker.getCommandSize() != 0) {
            this.mKeyUpgradeCommandWorker.sendCommand(4);
        } else {
            Slog.i(TAG, "Stop upgrade keyboard because no Command");
            this.mKeyUpgradeCommandWorker.resetWorker();
            upgradeMCU(this.mMCUUpgradeFilePath);
        }
    }

    /* renamed from: lambda$initCommandWorker$1$com-android-server-padkeyboard-iic-IICNodeHelper */
    public /* synthetic */ void m1275x8e6df771() {
        byte[] responseData = this.mMCUUpgradeCommandWorker.getCommandResponse();
        if (responseData[4] == -111) {
            if (!this.mIsBinDataOK) {
                int index = this.mMCUUpgradeCommandWorker.getReceiverPackageIndex() + 1;
                this.mMCUUpgradeCommandWorker.insertCommandToQueue(this.mMcuUpgradeUtil.getBinPackInfo(index * 52));
            } else {
                this.mMCUUpgradeCommandWorker.insertCommandToQueue(this.mMcuUpgradeUtil.getUpEndInfo());
                this.mMCUUpgradeCommandWorker.insertCommandToQueue(this.mMcuUpgradeUtil.getUpFlashInfo());
                this.mMCUUpgradeCommandWorker.insertCommandToQueue(this.mMcuUpgradeUtil.getResetInfo());
                this.mIsBinDataOK = false;
            }
            this.mMCUUpgradeCommandWorker.sendCommand(4);
        } else if (this.mMCUUpgradeCommandWorker.getCommandSize() != 0) {
            this.mMCUUpgradeCommandWorker.sendCommand(4);
        } else {
            Slog.i(TAG, "Stop upgrade MCU because no Command");
            this.mMCUUpgradeCommandWorker.resetWorker();
            this.mNanoSocketCallback.readyToCheckAuth();
        }
    }

    /* renamed from: lambda$initCommandWorker$2$com-android-server-padkeyboard-iic-IICNodeHelper */
    public /* synthetic */ void m1276x80179d90() {
        if (this.mMCUVersionWorker.getCommandSize() != 0) {
            this.mMCUVersionWorker.sendCommand(4);
            return;
        }
        this.mMCUVersionWorker.resetWorker();
        this.mKeyboardVersionWorker.insertCommandToQueue(this.mCommunicationUtil.getVersionCommand(CommunicationUtil.KEYBOARD_ADDRESS));
        this.mKeyboardVersionWorker.sendCommand(4);
    }

    /* renamed from: lambda$initCommandWorker$3$com-android-server-padkeyboard-iic-IICNodeHelper */
    public /* synthetic */ void m1277x71c143af() {
        if (this.mKeyboardVersionWorker.getCommandSize() != 0) {
            this.mKeyboardVersionWorker.sendCommand(4);
        } else {
            this.mKeyboardVersionWorker.resetWorker();
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class SocketCallbackListener implements CommunicationUtil.SocketCallBack {
        SocketCallbackListener() {
            IICNodeHelper.this = this$0;
        }

        @Override // com.android.server.padkeyboard.iic.CommunicationUtil.SocketCallBack
        public void responseFromKeyboard(byte[] data) {
            switch (data[4]) {
                case -111:
                    IICNodeHelper.this.checkDataPkgSum(data, "Receiver Keyboard Bin Package");
                    if (data[6] == 0) {
                        IICNodeHelper.this.mReceiverBinIndex = ((((data[10] << 16) & 16711680) + ((data[9] << 8) & 65280)) + (data[8] & 255)) / 52;
                        if (IICNodeHelper.this.mKeyUpgradeCommandWorker.isWorkRunning()) {
                            if (IICNodeHelper.this.mReceiverBinIndex == IICNodeHelper.this.mKeyBoardUpgradeUtil.getBinPacketTotal(64) - 1) {
                                IICNodeHelper.this.mIsBinDataOK = true;
                            }
                            IICNodeHelper.this.mKeyUpgradeCommandWorker.triggerResponseForPackage(data, IICNodeHelper.this.mReceiverBinIndex);
                        }
                        float progress = Math.round(((IICNodeHelper.this.mReceiverBinIndex / IICNodeHelper.this.mKeyBoardUpgradeUtil.getBinPacketTotal(64)) * 10000.0f) / 100.0f);
                        if (IICNodeHelper.this.mNanoSocketCallback != null) {
                            IICNodeHelper.this.mNanoSocketCallback.onOtaProgress(20, progress);
                            return;
                        }
                        return;
                    }
                    IICNodeHelper.this.mNanoSocketCallback.onOtaErrorInfo(20, "Keyboard Bin Package Info Response error " + ((int) data[6]));
                    IICNodeHelper.this.mKeyUpgradeCommandWorker.onWorkException(data);
                    return;
                case -94:
                    IICNodeHelper.this.checkDataPkgSum(data, "Receiver Keyboard Search Status");
                    if (data[7] == 0) {
                        int battry = ((data[11] << 8) & 65280) + (data[10] & 255);
                        int serial = data[17] & 7;
                        int times = (data[17] >> 4) & 15;
                        if (IICNodeHelper.this.mNanoSocketCallback != null) {
                            IICNodeHelper.this.mNanoSocketCallback.onStateData(1, data[9], battry + "mV", times);
                        }
                        IICNodeHelper.this.parseKeyboardStatus(data);
                        Slog.i(IICNodeHelper.TAG, "Receiver mcu KEY_S:" + String.format("0x%02x", Byte.valueOf(data[9])) + " KEY_R:" + battry + "mv E_UART:" + String.format("0x%02x%02x", Byte.valueOf(data[13]), Byte.valueOf(data[12])) + " E_PPM:" + String.format("0x%02x%02x%02x%02x", Byte.valueOf(data[17]), Byte.valueOf(data[16]), Byte.valueOf(data[15]), Byte.valueOf(data[14])) + " Serial:" + serial + " PowerUp:" + times + " OCP_F:" + ((int) data[18]));
                        return;
                    } else if (IICNodeHelper.this.mNanoSocketCallback != null) {
                        IICNodeHelper.this.mNanoSocketCallback.onStateData(6, (byte) 0, "0mV", 0);
                        return;
                    } else {
                        return;
                    }
                case -16:
                    IICNodeHelper.this.checkDataPkgSum(data, "Receiver 803 report read Keyboard status");
                    if (data[7] == 0) {
                        IICNodeHelper.this.mNanoSocketCallback.onKeyState(1, 0);
                        return;
                    } else if (data[7] == 1) {
                        Slog.e(IICNodeHelper.TAG, "Command word not supported/malformed");
                        return;
                    } else if (data[7] == 2) {
                        Slog.e(IICNodeHelper.TAG, "Keyboard is disconnection, Command write fail");
                        return;
                    } else if (data[7] == 3) {
                        Slog.e(IICNodeHelper.TAG, "MCU is busy,The Command lose possibility");
                        return;
                    } else {
                        Slog.e(IICNodeHelper.TAG, "Other Error");
                        return;
                    }
                case 1:
                    IICNodeHelper.this.checkDataPkgSum(data, "Receiver keyBoard/Flash Version");
                    if (data[2] == 56) {
                        IICNodeHelper.this.mKeyboardVersion = String.format("%02x", Byte.valueOf(data[7])) + String.format("%02x", Byte.valueOf(data[6]));
                        NanoSocketCallback nanoSocketCallback = IICNodeHelper.this.mNanoSocketCallback;
                        NanoSocketCallback nanoSocketCallback2 = IICNodeHelper.this.mNanoSocketCallback;
                        nanoSocketCallback.onUpdateVersion(20, IICNodeHelper.this.mKeyboardVersion);
                        if (data[5] == 5) {
                            IICNodeHelper.this.updateKeyboardColor(data[10]);
                        }
                        if (IICNodeHelper.this.mKeyUpgradeCommandWorker.isWorkRunning()) {
                            if (IICNodeHelper.this.mKeyBoardUpgradeUtil.checkVersion(IICNodeHelper.this.mKeyboardVersion)) {
                                Toast.makeText(IICNodeHelper.this.mContext, IICNodeHelper.this.mContext.getResources().getString(286196275), 0).show();
                                IICNodeHelper.this.mKeyUpgradeCommandWorker.triggerResponse(data);
                                return;
                            }
                            IICNodeHelper.this.mShouldUpgradeKeyboard = false;
                            IICNodeHelper.this.mNanoSocketCallback.onOtaErrorInfo(20, NanoSocketCallback.OTA_ERROR_REASON_NO_VERSION);
                            IICNodeHelper.this.mKeyUpgradeCommandWorker.onWorkException(data);
                            return;
                        } else if (IICNodeHelper.this.mKeyboardVersionWorker.isWorkRunning()) {
                            IICNodeHelper.this.mKeyboardVersionWorker.triggerResponse(data);
                            return;
                        } else {
                            return;
                        }
                    } else if (data[2] == 57) {
                        IICNodeHelper.this.mKeyboardFlashVersion = String.format("%02x", Byte.valueOf(data[9])) + String.format("%02x", Byte.valueOf(data[8]));
                        NanoSocketCallback nanoSocketCallback3 = IICNodeHelper.this.mNanoSocketCallback;
                        NanoSocketCallback nanoSocketCallback4 = IICNodeHelper.this.mNanoSocketCallback;
                        nanoSocketCallback3.onUpdateVersion(3, IICNodeHelper.this.mKeyboardFlashVersion);
                        return;
                    } else {
                        return;
                    }
                case 2:
                    IICNodeHelper.this.checkDataPkgSum(data, "Receiver Keyboard Upgrade");
                    if (data[6] == 0) {
                        Slog.i(IICNodeHelper.TAG, "Receiver Keyboard Upgrade Mode");
                        if (IICNodeHelper.this.mKeyUpgradeCommandWorker.isWorkRunning()) {
                            IICNodeHelper.this.mNanoSocketCallback.onOtaStateChange(3, 0);
                            IICNodeHelper.this.mKeyUpgradeCommandWorker.triggerResponse(data);
                            return;
                        }
                        return;
                    }
                    IICNodeHelper.this.mNanoSocketCallback.onOtaErrorInfo(20, "Upgrade Info Response error" + ((int) data[6]));
                    IICNodeHelper.this.mKeyUpgradeCommandWorker.onWorkException(data);
                    return;
                case 4:
                    IICNodeHelper.this.checkDataPkgSum(data, "Receiver Keyboard Upgrade End");
                    if (data[6] == 0) {
                        IICNodeHelper.this.mKeyUpgradeCommandWorker.triggerResponse(data);
                        return;
                    } else if (IICNodeHelper.this.mNanoSocketCallback != null) {
                        IICNodeHelper.this.mNanoSocketCallback.onOtaErrorInfo(20, "Upgrade finished Info Response error " + ((int) data[6]));
                        IICNodeHelper.this.mKeyUpgradeCommandWorker.onWorkException(data);
                        return;
                    } else {
                        return;
                    }
                case 6:
                    IICNodeHelper.this.checkDataPkgSum(data, "Receiver Keyboard Flash");
                    if (data[6] == 0) {
                        IICNodeHelper.this.mKeyUpgradeCommandWorker.triggerResponse(data);
                        IICNodeHelper.this.mNanoSocketCallback.onOtaStateChange(3, 2);
                        return;
                    }
                    IICNodeHelper.this.mNanoSocketCallback.onOtaErrorInfo(20, "Keyboard Flash Info Response error " + ((int) data[6]));
                    IICNodeHelper.this.mKeyUpgradeCommandWorker.onWorkException(data);
                    return;
                case 32:
                    IICNodeHelper.this.checkDataPkgSum(data, "Receiver Keyboard recover firmware status");
                    if (data[5] != 1) {
                        Slog.e(IICNodeHelper.TAG, "parse keyboard recover firmware status fail!");
                        return;
                    } else if (data[6] == 54) {
                        Toast.makeText(IICNodeHelper.this.mContext, IICNodeHelper.this.mContext.getResources().getString(286196262), 0).show();
                        return;
                    } else {
                        return;
                    }
                case UsbKeyboardUtil.COMMAND_POWER_STATE /* 37 */:
                    if (data[6] == 1) {
                        Slog.i(IICNodeHelper.TAG, "Set keyboard power on");
                        return;
                    } else {
                        Slog.i(IICNodeHelper.TAG, "Set keyboard power off");
                        return;
                    }
                case 38:
                    if (data[6] == 1) {
                        Slog.i(IICNodeHelper.TAG, "Set keyboard capslock on");
                        return;
                    } else {
                        Slog.i(IICNodeHelper.TAG, "Set keyboard capslock off");
                        return;
                    }
                case 40:
                    IICNodeHelper.this.checkDataPkgSum(data, "Receiver Keyboard sleep status");
                    if (data[5] != 1) {
                        Slog.e(IICNodeHelper.TAG, "parse KeyboardSleepStatus fail!");
                        return;
                    } else if (data[6] == 0) {
                        IICNodeHelper.this.mNanoSocketCallback.onKeyboardSleepStatusChanged(true);
                        return;
                    } else if (data[6] == 1) {
                        IICNodeHelper.this.mNanoSocketCallback.onKeyboardSleepStatusChanged(false);
                        return;
                    } else {
                        Slog.e(IICNodeHelper.TAG, "parse KeyboardSleepStatus get unknown status!");
                        return;
                    }
                case 48:
                    Slog.i(IICNodeHelper.TAG, "Command :" + ((int) data[6]) + " result is :" + ((int) data[7]));
                    return;
                case 49:
                case 50:
                case 51:
                case 52:
                case 53:
                    IICNodeHelper.this.mNanoSocketCallback.onAuthResult(data);
                    IICNodeHelper.this.mAuthCommandResponse = data;
                    IICNodeHelper.this.mCountDownLatch.countDown();
                    return;
                case UsbKeyboardUtil.COMMAND_READ_KEYBOARD /* 82 */:
                    IICNodeHelper.this.checkDataPkgSum(data, "Receiver Keyboard Status");
                    int x2 = ((data[11] << 8) & 65280) + (data[10] & 255);
                    int y2 = ((data[13] << 8) & 65280) + (data[12] & 255);
                    int z2 = ((data[15] << 8) & 65280) + (data[14] & 255);
                    if (data[11] < 0) {
                        x2 = -(65536 - x2);
                    }
                    if (data[13] < 0) {
                        y2 = -(65536 - y2);
                    }
                    if (data[15] < 0) {
                        z2 = -(65536 - z2);
                    }
                    if (IICNodeHelper.this.mNanoSocketCallback != null) {
                        IICNodeHelper.this.mNanoSocketCallback.onKeyStateData(1, data[6], data[7], data[8], data[9], x2, y2, z2, data[16], data[17], data[18]);
                        return;
                    }
                    return;
                case 100:
                    IICNodeHelper.this.checkDataPkgSum(data, "Receiver GSensor status");
                    if (data[5] != 6) {
                        Slog.e(IICNodeHelper.TAG, "parseGSensorStatus fail!");
                        return;
                    }
                    int x = ((data[7] << 4) & 4080) | ((data[6] >> 4) & 15);
                    int y = ((data[9] << 4) & 4080) | ((data[8] >> 4) & 15);
                    int z = ((data[10] >> 4) & 15) | ((data[11] << 4) & 4080);
                    if ((x & 2048) == 2048) {
                        x = -(4096 - x);
                    }
                    if ((y & 2048) == 2048) {
                        y = -(4096 - y);
                    }
                    if ((z & 2048) == 2048) {
                        z = -(4096 - z);
                    }
                    float x_normal = (x * 9.8f) / 256.0f;
                    float y_normal = ((-y) * 9.8f) / 256.0f;
                    float z_normal = ((-z) * 9.8f) / 256.0f;
                    IICNodeHelper.this.mNanoSocketCallback.onKeyboardGSensorChanged(x_normal, y_normal, z_normal);
                    return;
                default:
                    Slog.e(IICNodeHelper.TAG, "UnDefine Command:" + MiuiKeyboardUtil.Bytes2Hex(data, data.length));
                    return;
            }
        }

        @Override // com.android.server.padkeyboard.iic.CommunicationUtil.SocketCallBack
        public void responseFromMCU(byte[] data) {
            switch (data[4]) {
                case -111:
                    IICNodeHelper.this.checkDataPkgSum(data, "Receiver MCU Bin Package");
                    if (data[6] == 0) {
                        int nextIndex = ((((data[10] << 16) & 16711680) + ((data[9] << 8) & 65280)) + (data[8] & 255)) / 52;
                        IICNodeHelper.this.mReceiverBinIndex = nextIndex;
                        if (IICNodeHelper.this.mReceiverBinIndex == IICNodeHelper.this.mMcuUpgradeUtil.getBinPacketTotal(64) - 1) {
                            IICNodeHelper.this.mIsBinDataOK = true;
                        }
                        IICNodeHelper.this.mMCUUpgradeCommandWorker.triggerResponseForPackage(data, IICNodeHelper.this.mReceiverBinIndex);
                        float progress = Math.round(((IICNodeHelper.this.mReceiverBinIndex / IICNodeHelper.this.mMcuUpgradeUtil.getBinPacketTotal(64)) * 10000.0f) / 100.0f);
                        if (IICNodeHelper.this.mNanoSocketCallback != null) {
                            IICNodeHelper.this.mNanoSocketCallback.onOtaProgress(10, progress);
                            return;
                        }
                        return;
                    }
                    IICNodeHelper.this.mNanoSocketCallback.onOtaErrorInfo(10, "MCU Package info error:" + ((int) data[6]));
                    IICNodeHelper.this.mMCUUpgradeCommandWorker.onWorkException(data);
                    return;
                case 1:
                    IICNodeHelper.this.checkDataPkgSum(data, "Receiver get MCU version");
                    if (data[6] == 2) {
                        byte[] deviceVersion = new byte[16];
                        System.arraycopy(data, 7, deviceVersion, 0, 16);
                        IICNodeHelper.this.mMCUVersion = MiuiKeyboardUtil.Bytes2String(deviceVersion);
                        if (IICNodeHelper.this.mMcuUpgradeUtil.checkVersion(IICNodeHelper.this.mMCUVersion)) {
                            IICNodeHelper.this.mMCUUpgradeCommandWorker.triggerResponse(data);
                        } else {
                            IICNodeHelper.this.mShouldUpgradeMCU = false;
                            IICNodeHelper.this.mNanoSocketCallback.onOtaErrorInfo(10, NanoSocketCallback.OTA_ERROR_REASON_NO_VERSION);
                            IICNodeHelper.this.mMCUUpgradeCommandWorker.onWorkException(data);
                        }
                        IICNodeHelper.this.mMCUVersionWorker.triggerResponse(data);
                        if (IICNodeHelper.this.mNanoSocketCallback != null) {
                            NanoSocketCallback nanoSocketCallback = IICNodeHelper.this.mNanoSocketCallback;
                            NanoSocketCallback nanoSocketCallback2 = IICNodeHelper.this.mNanoSocketCallback;
                            nanoSocketCallback.onUpdateVersion(10, IICNodeHelper.this.mMCUVersion);
                            return;
                        }
                        return;
                    }
                    return;
                case 2:
                    IICNodeHelper.this.checkDataPkgSum(data, "Receiver MCU Upgrade");
                    if (data[6] == 0) {
                        Slog.i(IICNodeHelper.TAG, "receiver MCU Upgrade start");
                        IICNodeHelper.this.mNanoSocketCallback.onOtaStateChange(1, 0);
                        IICNodeHelper.this.mMCUUpgradeCommandWorker.triggerResponse(data);
                        return;
                    }
                    IICNodeHelper.this.mNanoSocketCallback.onOtaErrorInfo(10, "MCU Upgrade info error" + ((int) data[6]));
                    IICNodeHelper.this.mMCUUpgradeCommandWorker.onWorkException(data);
                    return;
                case 3:
                    IICNodeHelper.this.checkDataPkgSum(data, "Receiver MCU Reset");
                    if (data[6] == 0) {
                        IICNodeHelper.this.mMCUVersion = null;
                        IICNodeHelper.this.mMCUUpgradeCommandWorker.triggerResponse(data);
                        return;
                    }
                    IICNodeHelper.this.mNanoSocketCallback.onOtaErrorInfo(10, "MCU Reset error:" + ((int) data[6]));
                    IICNodeHelper.this.mMCUUpgradeCommandWorker.onWorkException(data);
                    return;
                case 4:
                    IICNodeHelper.this.checkDataPkgSum(data, "Receiver MCU UpEnd");
                    if (data[6] == 0) {
                        IICNodeHelper.this.mMCUUpgradeCommandWorker.triggerResponse(data);
                        return;
                    }
                    IICNodeHelper.this.mNanoSocketCallback.onOtaErrorInfo(10, "MCU UpEnd error:" + ((int) data[6]));
                    IICNodeHelper.this.mMCUUpgradeCommandWorker.onWorkException(data);
                    return;
                case 6:
                    IICNodeHelper.this.checkDataPkgSum(data, "Receiver MCU Flash");
                    if (data[6] == 0) {
                        Slog.i(IICNodeHelper.TAG, "Receiver Flash");
                        IICNodeHelper.this.mMCUUpgradeCommandWorker.triggerResponse(data);
                        IICNodeHelper.this.mNanoSocketCallback.onOtaStateChange(1, 2);
                        return;
                    } else if (data[6] != 5) {
                        IICNodeHelper.this.mMCUUpgradeCommandWorker.onWorkException(data);
                        IICNodeHelper.this.mNanoSocketCallback.onOtaErrorInfo(10, "MCU Flash error:" + ((int) data[6]));
                        return;
                    } else {
                        return;
                    }
                default:
                    return;
            }
        }
    }

    public void updateKeyboardColor(byte value) {
        if (value == 65) {
            MiuiSettings.System.putStringForUser(this.mContext.getContentResolver(), PROVIDER_KEYBOARD_COLOR, KEYBOARD_COLOR_BLACK, UserHandle.myUserId());
        } else if (value == 66) {
            MiuiSettings.System.putStringForUser(this.mContext.getContentResolver(), PROVIDER_KEYBOARD_COLOR, KEYBOARD_COLOR_WHITE, UserHandle.myUserId());
        }
    }

    public void setShouldUpgradeStatus() {
        this.mShouldUpgradeMCU = true;
        this.mShouldUpgradeKeyboard = true;
    }

    /* loaded from: classes.dex */
    public class RepeatSendCommandHandler extends Handler {
        public static final String DATA_SEND_COMMAND = "command";
        public static final String DATA_WORKER_OBJECT = "worker";
        public static final int MSG_SEND_COMMAND_QUEUE = 97;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        RepeatSendCommandHandler(Looper looper) {
            super(looper);
            IICNodeHelper.this = this$0;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            if (msg.what == 97) {
                int repeatTimes = msg.arg1;
                CommandWorker worker = (CommandWorker) msg.getData().getParcelable(DATA_WORKER_OBJECT);
                if (repeatTimes != 0) {
                    byte[] command = msg.getData().getByteArray(DATA_SEND_COMMAND);
                    Slog.i(IICNodeHelper.TAG, "write socket command repeat :" + repeatTimes);
                    IICNodeHelper.this.mCommunicationUtil.writeSocketCmd(command);
                    Bundle data = new Bundle();
                    data.putByteArray(DATA_SEND_COMMAND, command);
                    data.putParcelable(DATA_WORKER_OBJECT, worker);
                    Message msg2 = Message.obtain(IICNodeHelper.this.mRepeatSendCommandHandler, 97);
                    msg2.arg1 = repeatTimes - 1;
                    msg2.setData(data);
                    IICNodeHelper.this.mRepeatSendCommandHandler.sendMessageDelayed(msg2, 1500L);
                    return;
                }
                worker.notResponseException();
            }
        }
    }

    public void shutdownAllCommandQueue() {
        this.mKeyUpgradeCommandWorker.resetWorker();
        this.mMCUUpgradeCommandWorker.resetWorker();
        this.mMCUVersionWorker.resetWorker();
        this.mKeyboardVersionWorker.resetWorker();
    }

    /* loaded from: classes.dex */
    public class CommandWorker implements Parcelable {
        private final Queue<byte[]> mCommandQueue = new LinkedList();
        private byte[] mDoingCommand;
        private boolean mIsRunning;
        private int mReceiverPackageIndex;
        private byte[] mResponse;
        private ResponseListener mResponseListener;

        public void insertCommandToQueue(byte[] command) {
            this.mCommandQueue.offer(command);
        }

        public CommandWorker(ResponseListener listener) {
            IICNodeHelper.this = this$0;
            this.mResponseListener = listener;
        }

        public boolean isWorkRunning() {
            return this.mIsRunning;
        }

        public int getCommandSize() {
            return this.mCommandQueue.size();
        }

        public void sendCommand(int repeatTimes) {
            this.mIsRunning = true;
            this.mDoingCommand = this.mCommandQueue.poll();
            IICNodeHelper.this.mRepeatSendCommandHandler.removeMessages(97);
            IICNodeHelper.this.mRepeatSendCommandHandler.post(new Runnable() { // from class: com.android.server.padkeyboard.iic.IICNodeHelper$CommandWorker$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    IICNodeHelper.CommandWorker.this.m1279x5cbab68d();
                }
            });
            Bundle data = new Bundle();
            data.putByteArray(RepeatSendCommandHandler.DATA_SEND_COMMAND, this.mDoingCommand);
            data.putParcelable(RepeatSendCommandHandler.DATA_WORKER_OBJECT, this);
            Message msg2 = Message.obtain(IICNodeHelper.this.mRepeatSendCommandHandler, 97);
            msg2.arg1 = repeatTimes;
            msg2.setData(data);
            IICNodeHelper.this.mRepeatSendCommandHandler.sendMessageDelayed(msg2, 180L);
        }

        /* renamed from: lambda$sendCommand$0$com-android-server-padkeyboard-iic-IICNodeHelper$CommandWorker */
        public /* synthetic */ void m1279x5cbab68d() {
            IICNodeHelper.this.mCommunicationUtil.writeSocketCmd(this.mDoingCommand);
        }

        public void notResponseException() {
            if (!this.mIsRunning) {
                return;
            }
            IICNodeHelper.this.mNanoSocketCallback.onWriteSocketErrorInfo("Time Out!");
            resetWorker();
            this.mResponseListener.onCommandResponse();
        }

        public void triggerResponse(byte[] response) {
            if (this.mIsRunning && response[4] == this.mDoingCommand[8]) {
                this.mResponse = response;
                this.mResponseListener.onCommandResponse();
            }
        }

        public void triggerResponseForPackage(byte[] response, int index) {
            if (!this.mIsRunning) {
                return;
            }
            this.mResponse = response;
            this.mReceiverPackageIndex = index;
            this.mResponseListener.onCommandResponse();
        }

        public int getReceiverPackageIndex() {
            return this.mReceiverPackageIndex;
        }

        public byte[] getCommandResponse() {
            return this.mResponse;
        }

        public void onWorkException(byte[] response) {
            if (!this.mIsRunning) {
                return;
            }
            byte b = response[4];
            byte b2 = this.mDoingCommand[8];
            if (b == b2 || (response[4] == -111 && b2 == 17)) {
                resetWorker();
                this.mResponseListener.onCommandResponse();
            }
        }

        public void emptyCommandResponse() {
            this.mResponseListener.onCommandResponse();
        }

        public void resetWorker() {
            this.mReceiverPackageIndex = 0;
            this.mDoingCommand = new byte[66];
            this.mIsRunning = false;
            this.mCommandQueue.clear();
            this.mResponse = new byte[66];
            IICNodeHelper.this.mRepeatSendCommandHandler.removeMessages(97);
        }

        @Override // android.os.Parcelable
        public int describeContents() {
            return 0;
        }

        @Override // android.os.Parcelable
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.mReceiverPackageIndex);
        }
    }
}
