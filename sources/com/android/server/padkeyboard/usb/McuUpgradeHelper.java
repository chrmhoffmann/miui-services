package com.android.server.padkeyboard.usb;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.util.Slog;
import com.android.server.ScoutHelper;
import com.android.server.padkeyboard.MiuiKeyboardUtil;
import com.android.server.padkeyboard.MiuiPadKeyboardManager;
import com.android.server.padkeyboard.iic.CommunicationUtil;
import java.util.Arrays;
/* loaded from: classes.dex */
public class McuUpgradeHelper extends UpgradeHelper {
    protected static final int BIN_HEAD_LENGTH = 63;
    protected static final int HEAD_BASE_ADDRESS = 32704;
    protected static final String KB_MCU_BIN_PATH = "/vendor/etc/XM_KB_MCU.bin";
    protected static final int TARGET = 24;
    public static final String VERSION_HEAD = "XM2021";
    protected String mBinChipId;
    protected int mBinPid;
    protected String mBinStartFlag;
    protected int mBinVid;
    protected int mRunStartAddress;
    protected final byte[] mBinRoDataByte = new byte[4];
    protected final byte[] mRameCodeByte = new byte[4];
    protected final byte[] mRameCodeLenByte = new byte[4];
    protected final byte[] mRunStartByte = new byte[4];
    protected final byte[] mPidByte = new byte[2];
    protected final byte[] mVidByte = new byte[2];
    protected final byte[] mVersionByte = new byte[16];
    protected final byte[] mLengthByte = new byte[4];
    protected final byte[] mDeviceTypeByte = new byte[1];
    protected final byte[] mCheckSumByte = new byte[4];

    public McuUpgradeHelper(Context context) {
        super(context, KB_MCU_BIN_PATH);
        parseUpgradeFileHead();
    }

    @Override // com.android.server.padkeyboard.usb.UpgradeHelper
    protected void parseUpgradeFileHead() {
        if (this.mFileBuf != null && this.mFileBuf.length > 32768) {
            byte[] startFlagByte = new byte[7];
            System.arraycopy(this.mFileBuf, HEAD_BASE_ADDRESS, startFlagByte, 0, startFlagByte.length);
            this.mBinStartFlag = MiuiKeyboardUtil.Bytes2String(startFlagByte);
            int headAddress = HEAD_BASE_ADDRESS + startFlagByte.length;
            byte[] chipIDByte = new byte[2];
            System.arraycopy(this.mFileBuf, headAddress, chipIDByte, 0, chipIDByte.length);
            this.mBinChipId = MiuiKeyboardUtil.Bytes2Hex(chipIDByte, chipIDByte.length);
            int headAddress2 = headAddress + chipIDByte.length;
            byte[] bArr = this.mFileBuf;
            byte[] bArr2 = this.mBinRoDataByte;
            System.arraycopy(bArr, headAddress2, bArr2, 0, bArr2.length);
            int headAddress3 = headAddress2 + this.mBinRoDataByte.length;
            byte[] bArr3 = this.mFileBuf;
            byte[] bArr4 = this.mRameCodeByte;
            System.arraycopy(bArr3, headAddress3, bArr4, 0, bArr4.length);
            int headAddress4 = headAddress3 + this.mRameCodeByte.length;
            byte[] bArr5 = this.mFileBuf;
            byte[] bArr6 = this.mRameCodeLenByte;
            System.arraycopy(bArr5, headAddress4, bArr6, 0, bArr6.length);
            int headAddress5 = headAddress4 + this.mRameCodeLenByte.length;
            byte[] bArr7 = this.mFileBuf;
            byte[] bArr8 = this.mRunStartByte;
            System.arraycopy(bArr7, headAddress5, bArr8, 0, bArr8.length);
            byte[] bArr9 = this.mRunStartByte;
            this.mRunStartAddress = ((bArr9[3] << CommunicationUtil.MCU_ADDRESS) & (-16777216)) + ((bArr9[2] << 16) & 16711680) + ((bArr9[1] << 8) & 65280) + (bArr9[0] & 255);
            int headAddress6 = headAddress5 + bArr9.length;
            byte[] bArr10 = this.mFileBuf;
            byte[] bArr11 = this.mPidByte;
            System.arraycopy(bArr10, headAddress6, bArr11, 0, bArr11.length);
            byte[] bArr12 = this.mPidByte;
            this.mBinPid = ((bArr12[1] << 8) & 65280) + (bArr12[0] & 255);
            int headAddress7 = headAddress6 + bArr12.length;
            byte[] bArr13 = this.mFileBuf;
            byte[] bArr14 = this.mVidByte;
            System.arraycopy(bArr13, headAddress7, bArr14, 0, bArr14.length);
            byte[] bArr15 = this.mVidByte;
            this.mBinVid = ((bArr15[1] << 8) & 65280) + (bArr15[0] & 255);
            int headAddress8 = headAddress7 + bArr15.length + 2;
            byte[] bArr16 = this.mFileBuf;
            byte[] bArr17 = this.mVersionByte;
            System.arraycopy(bArr16, headAddress8, bArr17, 0, bArr17.length);
            this.mVersion = MiuiKeyboardUtil.Bytes2String(this.mVersionByte);
            Slog.i(MiuiPadKeyboardManager.TAG, "Bin Version : " + this.mVersion);
            int headAddress9 = headAddress8 + this.mVersionByte.length;
            byte[] bArr18 = this.mFileBuf;
            byte[] bArr19 = this.mDeviceTypeByte;
            System.arraycopy(bArr18, headAddress9, bArr19, 0, bArr19.length);
            int headAddress10 = headAddress9 + this.mDeviceTypeByte.length;
            byte[] bArr20 = this.mFileBuf;
            byte[] bArr21 = this.mLengthByte;
            System.arraycopy(bArr20, headAddress10, bArr21, 0, bArr21.length);
            byte[] bArr22 = this.mLengthByte;
            this.mBinLength = ((bArr22[3] << CommunicationUtil.MCU_ADDRESS) & (-16777216)) + ((bArr22[2] << 16) & 16711680) + ((bArr22[1] << 8) & 65280) + (bArr22[0] & 255);
            int headAddress11 = headAddress10 + this.mLengthByte.length;
            byte[] bArr23 = this.mFileBuf;
            byte[] bArr24 = this.mCheckSumByte;
            System.arraycopy(bArr23, headAddress11, bArr24, 0, bArr24.length);
            byte[] bArr25 = this.mCheckSumByte;
            this.mCheckSum = ((bArr25[3] << CommunicationUtil.MCU_ADDRESS) & (-16777216)) + ((bArr25[2] << 16) & 16711680) + ((bArr25[1] << 8) & 65280) + (bArr25[0] & 255);
            byte sum = MiuiKeyboardUtil.getSum(this.mFileBuf, HEAD_BASE_ADDRESS, BIN_HEAD_LENGTH);
            byte binSum = this.mFileBuf[32767];
            if (sum != binSum) {
                Slog.i(MiuiPadKeyboardManager.TAG, "bin check head sum error:" + ((int) sum) + "/" + ((int) binSum));
            }
        }
    }

    @Override // com.android.server.padkeyboard.usb.UpgradeHelper
    public byte[] getUpgradeCommand() {
        byte[] bytes = new byte[32];
        bytes[0] = CommunicationUtil.SEND_REPORT_ID_LONG_DATA;
        bytes[1] = 48;
        bytes[2] = CommunicationUtil.PAD_ADDRESS;
        bytes[3] = CommunicationUtil.MCU_ADDRESS;
        bytes[4] = 2;
        bytes[5] = 25;
        System.arraycopy(this.mLengthByte, 0, bytes, 6, 4);
        System.arraycopy(new byte[4], 0, bytes, 10, 4);
        System.arraycopy(this.mCheckSumByte, 0, bytes, 14, 4);
        byte[] binFlashAddressByte = {0, CommunicationUtil.PAD_ADDRESS, 0, 0};
        System.arraycopy(binFlashAddressByte, 0, bytes, 18, 4);
        System.arraycopy(this.mVidByte, 0, bytes, 22, 2);
        System.arraycopy(this.mPidByte, 0, bytes, 24, 2);
        bytes[26] = 1;
        bytes[27] = CommunicationUtil.MCU_ADDRESS;
        int[] times = MiuiKeyboardUtil.getYearMonthDayByTimestamp(System.currentTimeMillis());
        bytes[28] = (byte) times[0];
        bytes[29] = (byte) times[1];
        bytes[30] = (byte) times[2];
        bytes[31] = MiuiKeyboardUtil.getSum(bytes, 0, 31);
        return bytes;
    }

    @Override // com.android.server.padkeyboard.usb.UpgradeHelper
    public byte[] getBinPackInfo(int offset, int packetLength) {
        int binlength;
        int binlength2;
        byte[] bytes = new byte[32];
        if (packetLength == 32) {
            bytes = new byte[32];
            bytes[0] = CommunicationUtil.SEND_REPORT_ID_LONG_DATA;
            bytes[1] = 48;
            bytes[2] = CommunicationUtil.PAD_ADDRESS;
            bytes[3] = CommunicationUtil.MCU_ADDRESS;
            bytes[4] = CommunicationUtil.SEND_UPGRADE_PACKAGE_COMMAND;
            bytes[5] = CommunicationUtil.SEND_RESTORE_COMMAND;
            byte[] offsetB = MiuiKeyboardUtil.int2Bytes(offset);
            bytes[6] = offsetB[3];
            bytes[7] = offsetB[2];
            bytes[8] = offsetB[1];
            if (this.mFileBuf.length < offset + HEAD_BASE_ADDRESS + 20) {
                int binlength3 = (this.mFileBuf.length - 32704) - offset;
                bytes[9] = MiuiKeyboardUtil.int2Bytes(binlength3)[3];
                binlength2 = binlength3;
            } else {
                binlength2 = 20;
                bytes[9] = 20;
            }
            System.arraycopy(this.mFileBuf, offset + HEAD_BASE_ADDRESS, bytes, 10, binlength2);
            bytes[31] = MiuiKeyboardUtil.getSum(bytes, 0, 31);
        } else if (packetLength == 64) {
            bytes = new byte[64];
            bytes[0] = CommunicationUtil.SEND_REPORT_ID_LONG_DATA;
            bytes[1] = 48;
            bytes[2] = CommunicationUtil.PAD_ADDRESS;
            bytes[3] = CommunicationUtil.MCU_ADDRESS;
            bytes[4] = CommunicationUtil.SEND_UPGRADE_PACKAGE_COMMAND;
            bytes[5] = CommunicationUtil.KEYBOARD_ADDRESS;
            byte[] offsetB2 = MiuiKeyboardUtil.int2Bytes(offset);
            bytes[6] = offsetB2[3];
            bytes[7] = offsetB2[2];
            bytes[8] = offsetB2[1];
            if (this.mFileBuf.length < offset + HEAD_BASE_ADDRESS + 52) {
                int binlength4 = (this.mFileBuf.length - 32704) - offset;
                bytes[9] = CommunicationUtil.COMMAND_AUTH_52;
                binlength = binlength4;
            } else {
                binlength = 52;
                bytes[9] = CommunicationUtil.COMMAND_AUTH_52;
            }
            System.arraycopy(this.mFileBuf, offset + HEAD_BASE_ADDRESS, bytes, 10, binlength);
            bytes[62] = MiuiKeyboardUtil.getSum(bytes, 0, 62);
        }
        return bytes;
    }

    @Override // com.android.server.padkeyboard.usb.UpgradeHelper
    public byte[] getUpEndCommand() {
        byte[] bytes = new byte[32];
        bytes[0] = CommunicationUtil.SEND_REPORT_ID_LONG_DATA;
        bytes[1] = 48;
        bytes[2] = CommunicationUtil.PAD_ADDRESS;
        bytes[3] = CommunicationUtil.MCU_ADDRESS;
        bytes[4] = 4;
        bytes[5] = 21;
        System.arraycopy(this.mLengthByte, 0, bytes, 6, 4);
        System.arraycopy(this.mCheckSumByte, 0, bytes, 14, 4);
        bytes[19] = CommunicationUtil.PAD_ADDRESS;
        System.arraycopy(this.mVidByte, 0, bytes, 22, 2);
        System.arraycopy(this.mPidByte, 0, bytes, 24, 2);
        bytes[26] = CommunicationUtil.MCU_ADDRESS;
        bytes[27] = MiuiKeyboardUtil.getSum(bytes, 0, 27);
        return bytes;
    }

    @Override // com.android.server.padkeyboard.usb.UpgradeHelper
    public byte[] getResetCommand() {
        byte[] bytes = new byte[32];
        bytes[0] = CommunicationUtil.SEND_REPORT_ID_SHORT_DATA;
        bytes[1] = 48;
        bytes[2] = CommunicationUtil.PAD_ADDRESS;
        bytes[3] = CommunicationUtil.MCU_ADDRESS;
        bytes[4] = 3;
        bytes[5] = 4;
        bytes[6] = CommunicationUtil.RESPONSE_TYPE;
        bytes[7] = CommunicationUtil.SEND_REPORT_ID_SHORT_DATA;
        bytes[8] = CommunicationUtil.KEYBOARD_ADDRESS;
        bytes[9] = 48;
        bytes[10] = MiuiKeyboardUtil.getSum(bytes, 0, 10);
        return bytes;
    }

    public byte[] getUpFlashCommand() {
        byte[] bytes = new byte[32];
        bytes[0] = CommunicationUtil.SEND_REPORT_ID_LONG_DATA;
        bytes[1] = 48;
        bytes[2] = CommunicationUtil.PAD_ADDRESS;
        bytes[3] = CommunicationUtil.MCU_ADDRESS;
        bytes[4] = 6;
        bytes[5] = 4;
        System.arraycopy(this.mLengthByte, 0, bytes, 6, 4);
        bytes[10] = MiuiKeyboardUtil.getSum(bytes, 0, 10);
        return bytes;
    }

    @Override // com.android.server.padkeyboard.usb.UpgradeHelper
    public int getBinPacketNum(int length) {
        if (length == 32) {
            int totle = (int) Math.ceil((this.mBinLength + 64) / 20.0d);
            return totle;
        } else if (length != 64) {
            return 0;
        } else {
            int totle2 = (int) Math.ceil((this.mBinLength + 64) / 52.0d);
            return totle2;
        }
    }

    @Override // com.android.server.padkeyboard.usb.UpgradeHelper
    public boolean startUpgrade(UsbDevice usbDevice, UsbDeviceConnection usbConnection, UsbEndpoint outUsbEndpoint, UsbEndpoint inUsbEndpoint) {
        Slog.i(MiuiPadKeyboardManager.TAG, "start mcu upgrade");
        if (usbDevice == null || usbConnection == null || outUsbEndpoint == null || inUsbEndpoint == null) {
            Slog.i(MiuiPadKeyboardManager.TAG, "start upgrade failed : device not ready");
            return false;
        }
        this.mUsbDevice = usbDevice;
        this.mUsbConnection = usbConnection;
        this.mOutUsbEndpoint = outUsbEndpoint;
        this.mInUsbEndpoint = inUsbEndpoint;
        if (this.mFileBuf == null || this.mFileBuf.length < 32768) {
            Slog.i(MiuiPadKeyboardManager.TAG, "start upgrade failed : invalid bin file");
            return false;
        }
        return sendUpgradeCommand();
    }

    private boolean sendUpgradeCommand() {
        Slog.i(MiuiPadKeyboardManager.TAG, "send upgrade command");
        Arrays.fill(this.mSendBuf, (byte) 0);
        byte[] command = getUpgradeCommand();
        System.arraycopy(command, 0, this.mSendBuf, 0, command.length);
        for (int i = 0; i < 3; i++) {
            if (!sendUsbData(this.mUsbConnection, this.mOutUsbEndpoint, this.mSendBuf)) {
                Slog.i(MiuiPadKeyboardManager.TAG, "send upgrade command failed");
            } else {
                Arrays.fill(this.mRecBuf, (byte) 0);
                while (sendUsbData(this.mUsbConnection, this.mInUsbEndpoint, this.mRecBuf)) {
                    if (parseUpgradeCommand()) {
                        return true;
                    }
                }
            }
            MiuiKeyboardUtil.operationWait(500);
            if (this.mUsbDevice == null) {
                return false;
            }
        }
        return false;
    }

    private boolean parseUpgradeCommand() {
        if (this.mRecBuf[6] != 0) {
            Slog.i(MiuiPadKeyboardManager.TAG, "receive upgrade command state error:" + ((int) this.mRecBuf[6]));
            return false;
        } else if (this.mRecBuf[7] != 0) {
            Slog.i(MiuiPadKeyboardManager.TAG, "receive upgrade command mode error:" + ((int) this.mRecBuf[6]));
            return false;
        } else if (!MiuiKeyboardUtil.checkSum(this.mRecBuf, 0, 8, this.mRecBuf[8])) {
            Slog.i(MiuiPadKeyboardManager.TAG, "receive upgrade command checkSum error:" + ((int) this.mRecBuf[8]));
            return false;
        } else {
            return sendBinData();
        }
    }

    private boolean sendBinData() {
        int packetNum = getBinPacketNum(64);
        Slog.i(MiuiPadKeyboardManager.TAG, "send bin data, packet num: " + packetNum);
        for (int num = 0; num < packetNum; num++) {
            if (!sendOnePacket(num)) {
                Slog.i(MiuiPadKeyboardManager.TAG, "send bin data " + num + " failed");
                return false;
            }
        }
        return sendUpEndCommand();
    }

    private boolean sendOnePacket(int num) {
        Arrays.fill(this.mSendBuf, (byte) 0);
        System.arraycopy(getBinPackInfo(num * 52, 64), 0, this.mSendBuf, 0, 64);
        for (int i = 0; i < 3; i++) {
            if (sendUsbData(this.mUsbConnection, this.mOutUsbEndpoint, this.mSendBuf)) {
                Arrays.fill(this.mRecBuf, (byte) 0);
                while (sendUsbData(this.mUsbConnection, this.mInUsbEndpoint, this.mRecBuf)) {
                    if (this.mRecBuf[4] == -111 && this.mRecBuf[6] == 0) {
                        return true;
                    }
                }
                continue;
            }
            MiuiKeyboardUtil.operationWait(ScoutHelper.BINDER_FULL_KILL_SCORE_ADJ_MIN);
        }
        return false;
    }

    private boolean sendUpEndCommand() {
        Arrays.fill(this.mSendBuf, (byte) 0);
        byte[] command = getUpEndCommand();
        System.arraycopy(command, 0, this.mSendBuf, 0, command.length);
        for (int i = 0; i < 3; i++) {
            if (sendUsbData(this.mUsbConnection, this.mOutUsbEndpoint, this.mSendBuf)) {
                Arrays.fill(this.mRecBuf, (byte) 0);
                while (sendUsbData(this.mUsbConnection, this.mInUsbEndpoint, this.mRecBuf)) {
                    if (parseUpEndCommand()) {
                        return true;
                    }
                }
            } else {
                Slog.i(MiuiPadKeyboardManager.TAG, "send up end command failed");
            }
            MiuiKeyboardUtil.operationWait(500);
            if (this.mUsbDevice == null) {
                return false;
            }
        }
        return false;
    }

    private boolean parseUpEndCommand() {
        if (this.mRecBuf[6] == 1) {
            Slog.i(MiuiPadKeyboardManager.TAG, "receive up end command state need wait");
            return false;
        } else if (this.mRecBuf[6] != 0) {
            Slog.i(MiuiPadKeyboardManager.TAG, "receive up end command state error:" + ((int) this.mRecBuf[6]));
            return false;
        } else {
            return sendUpFlashCommand();
        }
    }

    private boolean sendUpFlashCommand() {
        Arrays.fill(this.mSendBuf, (byte) 0);
        byte[] command = getUpFlashCommand();
        System.arraycopy(command, 0, this.mSendBuf, 0, command.length);
        for (int i = 0; i < 6; i++) {
            if (sendUsbData(this.mUsbConnection, this.mOutUsbEndpoint, this.mSendBuf)) {
                Arrays.fill(this.mRecBuf, (byte) 0);
                while (sendUsbData(this.mUsbConnection, this.mInUsbEndpoint, this.mRecBuf)) {
                    if (parseUpFlashCommand()) {
                        return true;
                    }
                }
            } else {
                Slog.i(MiuiPadKeyboardManager.TAG, "send up flash command failed");
            }
            MiuiKeyboardUtil.operationWait(1000);
            if (this.mUsbDevice == null) {
                return false;
            }
        }
        return false;
    }

    private boolean parseUpFlashCommand() {
        if (this.mRecBuf[6] != 0) {
            Slog.i(MiuiPadKeyboardManager.TAG, "receive up flash command state error:" + ((int) this.mRecBuf[6]));
            return false;
        } else if (!MiuiKeyboardUtil.checkSum(this.mRecBuf, 0, 7, this.mRecBuf[7])) {
            Slog.i(MiuiPadKeyboardManager.TAG, "receive up flash command checkSum error:" + ((int) this.mRecBuf[7]));
            return false;
        } else {
            return sendResetCommand();
        }
    }

    private boolean sendResetCommand() {
        Slog.i(MiuiPadKeyboardManager.TAG, "send reset command");
        Arrays.fill(this.mSendBuf, (byte) 0);
        byte[] command = getResetCommand();
        System.arraycopy(command, 0, this.mSendBuf, 0, command.length);
        boolean resetSent = false;
        for (int i = 0; i < 3; i++) {
            if (!sendUsbData(this.mUsbConnection, this.mOutUsbEndpoint, this.mSendBuf)) {
                Slog.i(MiuiPadKeyboardManager.TAG, "send reset command failed");
            } else {
                resetSent = true;
                Arrays.fill(this.mRecBuf, (byte) 0);
                while (sendUsbData(this.mUsbConnection, this.mInUsbEndpoint, this.mRecBuf)) {
                    if (this.mRecBuf[6] != 0) {
                        Slog.i(MiuiPadKeyboardManager.TAG, "receive reset command state error:" + ((int) this.mRecBuf[6]));
                    } else if (!MiuiKeyboardUtil.checkSum(this.mRecBuf, 0, 7, this.mRecBuf[7])) {
                        Slog.i(MiuiPadKeyboardManager.TAG, "receive reset command checkSum error:" + ((int) this.mRecBuf[7]));
                    } else {
                        Slog.i(MiuiPadKeyboardManager.TAG, "receive reset command");
                        return true;
                    }
                }
            }
            MiuiKeyboardUtil.operationWait(1000);
            if (this.mUsbDevice == null) {
                return true;
            }
        }
        return resetSent;
    }
}
