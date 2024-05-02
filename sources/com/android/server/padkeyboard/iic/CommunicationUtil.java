package com.android.server.padkeyboard.iic;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Slog;
import com.android.server.padkeyboard.MiuiKeyboardUtil;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
/* loaded from: classes.dex */
public class CommunicationUtil {
    public static final int BUFFER_SIZE = 1024;
    public static final byte COMMAND_AUTH_3 = 50;
    public static final byte COMMAND_AUTH_51 = 51;
    public static final byte COMMAND_AUTH_52 = 52;
    public static final byte COMMAND_AUTH_7 = 53;
    public static final byte COMMAND_AUTH_START = 49;
    public static final byte COMMAND_CHECK_MCU_STATUS = -95;
    public static final byte COMMAND_CUSTOM_DATA_KB = 105;
    public static final byte COMMAND_DATA_PKG = -111;
    public static final byte COMMAND_GET_VERSION = 1;
    public static final byte COMMAND_G_SENSOR = 100;
    public static final byte COMMAND_KB_FEATURE_CAPS_LOCK = 38;
    public static final byte COMMAND_KB_FEATURE_POWER = 37;
    public static final byte COMMAND_KB_FEATURE_RECOVER = 32;
    public static final byte COMMAND_KB_FEATURE_SLEEP = 40;
    public static final byte COMMAND_KEYBOARD_RESPONSE_STATUS = 48;
    public static final byte COMMAND_KEYBOARD_STATUS = -16;
    public static final byte COMMAND_KEYBOARD_UPGRADE_STATUS = 117;
    public static final byte COMMAND_MCU_BOOT = 18;
    public static final byte COMMAND_MCU_RESET = 3;
    public static final byte COMMAND_READ_KB_STATUS = 82;
    public static final byte COMMAND_RESPONSE_MCU_STATUS = -94;
    public static final byte COMMAND_SUCCESS_RUN = 0;
    public static final byte COMMAND_UPGRADE = 2;
    public static final byte COMMAND_UPGRADE_FINISHED = 4;
    public static final byte COMMAND_UPGRADE_FLASH = 6;
    public static final byte FEATURE_DISABLE = 0;
    public static final byte FEATURE_ENABLE = 1;
    public static final byte KEYBOARD_ADDRESS = 56;
    public static final byte KEYBOARD_COLOR_BLACK = 65;
    public static final byte KEYBOARD_COLOR_WHITE = 66;
    public static final byte KEYBOARD_FLASH_ADDRESS = 57;
    public static final byte MCU_ADDRESS = 24;
    public static final byte PAD_ADDRESS = Byte.MIN_VALUE;
    public static final byte REPLENISH_PROTOCOL_COMMAND = 49;
    public static final byte RESPONSE_TYPE = 87;
    public static final byte RESPONSE_VENDOR_ONE_LONG_REPORT_ID = 36;
    public static final byte RESPONSE_VENDOR_ONE_SHORT_REPORT_ID = 35;
    public static final byte RESPONSE_VENDOR_TWO_REPORT_ID = 38;
    public static final byte RESPONSE_VENDOR_TWO_SHORT_REPORT_ID = 34;
    public static final int SEND_COMMAND_BYTE_LONG = 68;
    public static final int SEND_COMMAND_BYTE_SHORT = 34;
    public static final byte SEND_EMPTY_DATA = 0;
    public static final byte SEND_REPORT_ID_LONG_DATA = 79;
    public static final byte SEND_REPORT_ID_SHORT_DATA = 78;
    public static final byte SEND_RESTORE_COMMAND = 29;
    public static final byte SEND_SERIAL_NUMBER = 0;
    public static final byte SEND_TYPE = 50;
    public static final byte SEND_UPGRADE_PACKAGE_COMMAND = 17;
    public static final String SOCKET_ADDRESS = "/data/nanosic/localsocket/.ctrl";
    public static final String TAG = "IIC_CommunicationUtil";
    public static final int UPGRADE_KEYBOARD_FLASH_MODE = 0;
    public static final int UPGRADE_KEYBOARD_PASS_MODE = 1;
    public static final int UPGRADE_MCU_MODE = 2;
    public static final byte UPGRADE_PROTOCOL_COMMAND = 48;
    private static volatile CommunicationUtil sCommunicationUtil;
    private LocalSocket mLocalSocket;
    private NanoSocketCallback mNanoSocketCallback;
    private ReadSocketHandler mReadSocketHandler;
    private SocketCallBack mSocketCallBack;
    private int mWriteSocketErrorCode = 0;
    private final Object mLock = new Object();

    /* loaded from: classes.dex */
    public interface SocketCallBack {
        void responseFromKeyboard(byte[] bArr);

        void responseFromMCU(byte[] bArr);
    }

    /* loaded from: classes.dex */
    public enum KB_FEATURE {
        KB_ENABLE(CommunicationUtil.RESPONSE_VENDOR_TWO_SHORT_REPORT_ID, 1),
        KB_POWER(CommunicationUtil.COMMAND_KB_FEATURE_POWER, 2),
        KB_CAPS_KEY((byte) 38, 3),
        KB_WRITE_CMD_ACK((byte) 48, 4),
        KB_FIRMWARE_RECOVER(CommunicationUtil.COMMAND_KB_FEATURE_RECOVER, 5),
        KB_SLEEP_STATUS(CommunicationUtil.COMMAND_KB_FEATURE_SLEEP, 6),
        KB_G_SENSOR((byte) 39, 7);
        
        private final byte mCommand;
        private final int mIndex;

        KB_FEATURE(byte commandId, int index) {
            this.mCommand = commandId;
            this.mIndex = index;
        }

        public byte getCommand() {
            return this.mCommand;
        }

        public int getIndex() {
            return this.mIndex;
        }
    }

    /* loaded from: classes.dex */
    public enum AUTH_COMMAND {
        AUTH_START((byte) 49, (byte) 6),
        AUTH_STEP3((byte) 50, CommunicationUtil.RESPONSE_VENDOR_ONE_LONG_REPORT_ID),
        AUTH_STEP5_1(CommunicationUtil.COMMAND_AUTH_51, (byte) 16),
        AUTH_STEP5_2(CommunicationUtil.COMMAND_AUTH_52, CommunicationUtil.COMMAND_AUTH_52),
        AUTH_STEP7(CommunicationUtil.COMMAND_AUTH_7, (byte) 2);
        
        private final byte mCommand;
        private final byte mSize;

        AUTH_COMMAND(byte command, byte size) {
            this.mCommand = command;
            this.mSize = size;
        }

        public byte getCommand() {
            return this.mCommand;
        }

        public byte getSize() {
            return this.mSize;
        }
    }

    public static CommunicationUtil getInstance() {
        if (sCommunicationUtil == null) {
            synchronized (CommunicationUtil.class) {
                if (sCommunicationUtil == null) {
                    sCommunicationUtil = new CommunicationUtil();
                }
            }
        }
        return sCommunicationUtil;
    }

    private CommunicationUtil() {
        initSocketClient();
    }

    private void resetSocketClient() {
        LocalSocket localSocket = this.mLocalSocket;
        if (localSocket != null) {
            try {
                localSocket.close();
            } catch (Exception e) {
                Slog.e(TAG, "close history Socket exception");
            }
        }
        ReadSocketHandler readSocketHandler = this.mReadSocketHandler;
        if (readSocketHandler != null) {
            readSocketHandler.removeMessages(1);
        }
        synchronized (this.mLock) {
            this.mWriteSocketErrorCode = 0;
        }
    }

    private void initSocketClient() {
        LocalSocketAddress localSocketAddress = new LocalSocketAddress(SOCKET_ADDRESS, LocalSocketAddress.Namespace.ABSTRACT);
        try {
            LocalSocket localSocket = new LocalSocket();
            this.mLocalSocket = localSocket;
            localSocket.connect(localSocketAddress);
            this.mLocalSocket.setReceiveBufferSize(1024);
            this.mLocalSocket.setSendBufferSize(1024);
            if (this.mReadSocketHandler == null) {
                HandlerThread readThread = new HandlerThread("IIC_Read_Socket");
                readThread.start();
                this.mReadSocketHandler = new ReadSocketHandler(readThread.getLooper());
            }
            ReadSocketHandler readSocketHandler = this.mReadSocketHandler;
            readSocketHandler.sendMessage(readSocketHandler.obtainMessage(1));
            Slog.i(TAG, "LocalSocket connect:" + this.mLocalSocket.isConnected());
        } catch (IOException e) {
            e.printStackTrace();
            Slog.i(TAG, "LocalSocket connect Exception:" + e.toString());
        }
    }

    public void registerSocketCallback(SocketCallBack socketCallBack) {
        this.mSocketCallBack = socketCallBack;
    }

    public void setOtaCallBack(NanoSocketCallback callBack) {
        this.mNanoSocketCallback = callBack;
    }

    public LocalSocket getLocalSocket() {
        return this.mLocalSocket;
    }

    public boolean getSocketStatus() {
        LocalSocket localSocket = this.mLocalSocket;
        return localSocket != null && localSocket.isConnected();
    }

    public int getSocketErrorCode() {
        int i;
        synchronized (this.mLock) {
            i = this.mWriteSocketErrorCode;
        }
        return i;
    }

    public byte[] getVersionCommand(byte targetAddress) {
        byte[] temp = new byte[68];
        temp[0] = -86;
        temp[1] = KEYBOARD_COLOR_WHITE;
        temp[2] = 50;
        temp[3] = 0;
        temp[4] = SEND_REPORT_ID_LONG_DATA;
        temp[5] = 48;
        temp[6] = PAD_ADDRESS;
        temp[7] = targetAddress;
        temp[8] = 1;
        temp[9] = 0;
        temp[10] = getSum(temp, 4, 6);
        return temp;
    }

    public void sendRestoreMcuCommand() {
        byte[] temp = new byte[68];
        temp[0] = 50;
        temp[1] = 0;
        temp[2] = SEND_REPORT_ID_LONG_DATA;
        temp[3] = 48;
        temp[4] = PAD_ADDRESS;
        temp[5] = MCU_ADDRESS;
        temp[6] = SEND_RESTORE_COMMAND;
        temp[7] = 13;
        temp[8] = -116;
        temp[9] = 63;
        temp[10] = 101;
        temp[11] = -127;
        temp[12] = -110;
        temp[13] = -50;
        temp[14] = 0;
        temp[15] = -108;
        temp[16] = -88;
        temp[17] = -126;
        temp[18] = 45;
        temp[19] = 91;
        temp[20] = 126;
        temp[21] = getSum(temp, 2, 19);
        writeSocketCmd(temp);
    }

    public boolean writeSocketCmd(byte[] buf) {
        LocalSocket localSocket = this.mLocalSocket;
        if (localSocket != null && localSocket.isConnected()) {
            try {
                Slog.i(TAG, "write socket : " + MiuiKeyboardUtil.Bytes2Hex(buf, buf.length));
                OutputStream outputStream = this.mLocalSocket.getOutputStream();
                outputStream.write(buf);
                outputStream.flush();
                return true;
            } catch (Exception e) {
                Slog.e(TAG, "LocalSocket write Exception:" + e.getMessage());
                resetSocketClient();
                initSocketClient();
                return false;
            }
        }
        resetSocketClient();
        initSocketClient();
        return false;
    }

    public byte[] getLongRawCommand() {
        return new byte[68];
    }

    public void setCommandHead(byte[] bytes) {
        bytes[0] = -86;
        bytes[1] = KEYBOARD_COLOR_WHITE;
        bytes[2] = 50;
        bytes[3] = 0;
    }

    public void setSetKeyboardStatusCommand(byte[] bytes, byte commandId, byte data) {
        bytes[4] = SEND_REPORT_ID_SHORT_DATA;
        bytes[5] = 49;
        bytes[6] = PAD_ADDRESS;
        bytes[7] = KEYBOARD_ADDRESS;
        bytes[8] = commandId;
        bytes[9] = 1;
        bytes[10] = data;
        bytes[11] = getSum(bytes, 4, 7);
    }

    public void setReadKeyboardCommand(byte[] bytes, byte reportId, byte version, byte sourceAdd, byte targetAdd, byte feature) {
        bytes[4] = reportId;
        bytes[5] = version;
        bytes[6] = sourceAdd;
        bytes[7] = targetAdd;
        bytes[8] = feature;
        bytes[9] = 0;
        bytes[10] = getSum(bytes, 4, 7);
    }

    public void setCheckKeyboardResponseCommand(byte[] bytes, byte reportId, byte version, byte sourceAdd, byte targetAdd, byte lastCommandId) {
        bytes[4] = reportId;
        bytes[5] = version;
        bytes[6] = sourceAdd;
        bytes[7] = targetAdd;
        bytes[8] = 48;
        bytes[9] = 1;
        bytes[10] = lastCommandId;
        bytes[11] = getSum(bytes, 4, 7);
    }

    public void setCheckMCUStatusCommand(byte[] data, byte command) {
        setCommandHead(data);
        data[4] = SEND_REPORT_ID_SHORT_DATA;
        data[5] = 49;
        data[6] = PAD_ADDRESS;
        data[7] = KEYBOARD_ADDRESS;
        data[8] = command;
        data[9] = 1;
        data[10] = 1;
        data[11] = getSum(data, 4, 7);
    }

    public void setGetHallStatusCommand(byte[] data) {
        setCommandHead(data);
        data[4] = SEND_REPORT_ID_LONG_DATA;
        data[5] = COMMAND_KB_FEATURE_RECOVER;
        data[6] = PAD_ADDRESS;
        data[7] = PAD_ADDRESS;
        data[8] = -31;
        data[9] = 1;
        data[10] = 0;
        data[11] = getSum(data, 4, 7);
    }

    public static synchronized byte getSum(byte[] command, int start, int length) {
        byte sum;
        synchronized (CommunicationUtil.class) {
            sum = 0;
            for (int i = start; i < start + length; i++) {
                sum = (byte) ((command[i] & 255) + sum);
            }
        }
        return sum;
    }

    public static synchronized int getSumInt(byte[] data, int start, int length) {
        int sum;
        synchronized (CommunicationUtil.class) {
            sum = 0;
            for (int i = start; i < start + length; i++) {
                sum += data[i] & 255;
            }
        }
        return sum;
    }

    public boolean checkSum(byte[] data, int start, int length, byte sum) {
        byte dataSum = getSum(data, start, length);
        byte[] bytes = {dataSum};
        if (bytes[0] == sum) {
            return true;
        }
        return false;
    }

    /* loaded from: classes.dex */
    public class ReadSocketHandler extends Handler {
        public static final int MSG_READ = 1;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        ReadSocketHandler(Looper looper) {
            super(looper);
            CommunicationUtil.this = this$0;
        }

        /* JADX WARN: Multi-variable type inference failed */
        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                try {
                    InputStream inputStream = CommunicationUtil.this.mLocalSocket.getInputStream();
                    while (CommunicationUtil.this.mLocalSocket.isConnected()) {
                        byte[] data = new byte[1024];
                        int readLength = inputStream.read(data);
                        int position = 0;
                        while (true) {
                            if (position < readLength) {
                                if (data[position] != -86) {
                                    Slog.e(CommunicationUtil.TAG, "Receiver Data is too old!");
                                    break;
                                }
                                byte[] temp = new byte[data[position + 1]];
                                if (position + 2 + temp.length > data.length) {
                                    Slog.e(CommunicationUtil.TAG, "Drop not complete Data!");
                                    break;
                                }
                                System.arraycopy(data, position + 2, temp, 0, temp.length);
                                dealReadSocketPackage(temp);
                                position += temp.length + 2;
                            } else {
                                break;
                            }
                        }
                    }
                    if (inputStream != null) {
                        inputStream.close();
                    }
                } catch (IOException e) {
                    Slog.e(CommunicationUtil.TAG, "LocalSocket read Exception:" + e.getMessage());
                }
            }
        }

        private void dealReadSocketPackage(byte[] data) {
            if (CommunicationUtil.this.mNanoSocketCallback == null || CommunicationUtil.this.mSocketCallBack == null) {
                Slog.e(CommunicationUtil.TAG, "Miui Keyboard Manager is not ready,Abandon this socket package.");
                return;
            }
            if (data[0] == Byte.MIN_VALUE) {
                CommunicationUtil.this.mNanoSocketCallback.onWriteSocketErrorInfo(NanoSocketCallback.OTA_ERROR_REASON_WRITE_SOCKET_EXCEPTION);
                Slog.d(CommunicationUtil.TAG, "Exception socket command is:" + MiuiKeyboardUtil.Bytes2Hex(data, data.length));
                synchronized (CommunicationUtil.this.mLock) {
                    CommunicationUtil.this.mWriteSocketErrorCode = data[8];
                }
            }
            if ((data[0] == 36 || data[0] == 35 || data[0] == 38 || data[0] == 34) && data[3] == Byte.MIN_VALUE) {
                if (data[1] == 48) {
                    if (data[2] == 24) {
                        CommunicationUtil.this.mSocketCallBack.responseFromMCU(data);
                    } else if (data[2] == 56 || data[2] == 57) {
                        CommunicationUtil.this.mSocketCallBack.responseFromKeyboard(data);
                    }
                } else if (data[1] == 49 && data[2] == 56) {
                    CommunicationUtil.this.mSocketCallBack.responseFromKeyboard(data);
                } else if (data[1] == 32 && data[2] == Byte.MIN_VALUE && data[4] == -31 && data[5] == 1) {
                    CommunicationUtil.this.mNanoSocketCallback.onHallStatusChanged(data[6]);
                }
            }
        }
    }
}
