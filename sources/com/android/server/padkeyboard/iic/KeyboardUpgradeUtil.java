package com.android.server.padkeyboard.iic;

import android.content.Context;
import android.util.Slog;
import com.android.server.padkeyboard.MiuiKeyboardUtil;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
/* loaded from: classes.dex */
public class KeyboardUpgradeUtil {
    private static final String TAG = "KeyboardUpgradeUtil";
    public int mBinCheckSum;
    public int mBinLength;
    public String mBinVersionStr;
    private Context mContext;
    private byte[] mFileBuf;
    public String mUpgradeFilePath;
    public boolean mValid;
    public byte[] mBinLengthByte = new byte[4];
    public byte[] mBinCheckSumByte = new byte[4];
    public byte[] mBinVersion = new byte[2];
    public byte[] mBinStartAddressByte = new byte[4];

    public KeyboardUpgradeUtil(Context context, String path) {
        if (path == null) {
            Slog.i(TAG, "UpgradeOta file Path is null");
            return;
        }
        this.mContext = context;
        this.mUpgradeFilePath = path;
        Slog.i(TAG, "UpgradeOta file Path:" + this.mUpgradeFilePath);
        byte[] ReadUpgradeFile = ReadUpgradeFile(this.mUpgradeFilePath);
        this.mFileBuf = ReadUpgradeFile;
        if (ReadUpgradeFile == null) {
            Slog.i(TAG, "UpgradeOta file buff is null or length low than 6000");
        } else {
            this.mValid = parseOtaFile();
        }
    }

    private byte[] ReadUpgradeFile(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            byte[] fileBuf = ReadFileToBuf(filePath);
            return fileBuf;
        }
        Slog.i(TAG, "=== The Upgrade bin file does not exist.");
        return null;
    }

    public boolean isValidFile() {
        return this.mValid;
    }

    private byte[] ReadFileToBuf(String fileName) {
        byte[] fileBuf = null;
        try {
            InputStream inputStream = new FileInputStream(fileName);
            int FileLen = inputStream.available();
            fileBuf = new byte[FileLen];
            inputStream.read(fileBuf);
            inputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fileBuf;
    }

    public boolean checkVersion(String version) {
        String str = this.mBinVersionStr;
        return str != null && !version.startsWith(str) && this.mBinVersionStr.compareTo(version) > 0;
    }

    private boolean parseOtaFile() {
        byte[] bArr = this.mFileBuf;
        if (bArr == null || bArr.length <= 135168) {
            return false;
        }
        byte[] binStartFlagByte = new byte[8];
        System.arraycopy(bArr, 135104, binStartFlagByte, 0, binStartFlagByte.length);
        String binStartFlag = MiuiKeyboardUtil.Bytes2String(binStartFlagByte);
        Slog.i(TAG, "Bin Start Flag: " + binStartFlag);
        int fileHeadBaseAddr = 135104 + 8;
        byte[] bArr2 = this.mFileBuf;
        byte[] bArr3 = this.mBinLengthByte;
        System.arraycopy(bArr2, fileHeadBaseAddr, bArr3, 0, bArr3.length);
        byte[] bArr4 = this.mBinLengthByte;
        this.mBinLength = ((bArr4[3] << CommunicationUtil.MCU_ADDRESS) & (-16777216)) + ((bArr4[2] << 16) & 16711680) + ((bArr4[1] << 8) & 65280) + (bArr4[0] & 255);
        Slog.i(TAG, "Bin Length: " + this.mBinLength);
        int fileHeadBaseAddr2 = fileHeadBaseAddr + 4;
        byte[] bArr5 = this.mFileBuf;
        byte[] bArr6 = this.mBinCheckSumByte;
        System.arraycopy(bArr5, fileHeadBaseAddr2, bArr6, 0, bArr6.length);
        byte[] bArr7 = this.mBinCheckSumByte;
        this.mBinCheckSum = ((bArr7[3] << CommunicationUtil.MCU_ADDRESS) & (-16777216)) + ((bArr7[2] << 16) & 16711680) + ((bArr7[1] << 8) & 65280) + (bArr7[0] & 255);
        Slog.i(TAG, "Bin CheckSum: " + this.mBinCheckSum);
        byte[] bArr8 = new byte[2];
        this.mBinVersion = bArr8;
        System.arraycopy(this.mFileBuf, fileHeadBaseAddr2 + 4, bArr8, 0, bArr8.length);
        this.mBinVersionStr = String.format("%02x", Byte.valueOf(this.mBinVersion[1])) + String.format("%02x", Byte.valueOf(this.mBinVersion[0]));
        Slog.i(TAG, "Bin Version : " + this.mBinVersionStr);
        byte[] bArr9 = this.mFileBuf;
        int sum1 = CommunicationUtil.getSumInt(bArr9, 135168, bArr9.length - 135168);
        if (sum1 != this.mBinCheckSum) {
            Slog.i(TAG, "Bin Check Check Sum Error:" + sum1 + "/" + this.mBinCheckSum);
            return false;
        }
        byte sum2 = CommunicationUtil.getSum(this.mFileBuf, 135104, 63);
        if (sum2 != this.mFileBuf[135167]) {
            Slog.i(TAG, "Bin Check Head Sum Error:" + ((int) sum2) + "/" + ((int) this.mFileBuf[135167]));
            return false;
        }
        byte[] bArr10 = this.mBinStartAddressByte;
        bArr10[0] = 0;
        bArr10[1] = CommunicationUtil.PAD_ADDRESS;
        bArr10[2] = 5;
        bArr10[3] = 0;
        return true;
    }

    public byte[] getUpgradeInfo(byte target) {
        byte[] bytes = new byte[68];
        bytes[0] = -86;
        bytes[1] = CommunicationUtil.KEYBOARD_COLOR_WHITE;
        bytes[2] = 50;
        bytes[3] = 0;
        bytes[4] = CommunicationUtil.SEND_REPORT_ID_LONG_DATA;
        bytes[5] = 48;
        bytes[6] = CommunicationUtil.PAD_ADDRESS;
        bytes[7] = target;
        bytes[8] = 2;
        bytes[9] = 14;
        System.arraycopy(this.mBinLengthByte, 0, bytes, 10, 4);
        System.arraycopy(this.mBinStartAddressByte, 0, bytes, 14, 4);
        System.arraycopy(this.mBinCheckSumByte, 0, bytes, 18, 4);
        System.arraycopy(this.mBinVersion, 0, bytes, 22, 2);
        bytes[24] = CommunicationUtil.getSum(bytes, 4, 21);
        return bytes;
    }

    public byte[] getBinPackInfo(byte target, int offset) {
        byte[] bytes = new byte[68];
        int binlength = 52;
        bytes[0] = -86;
        bytes[1] = CommunicationUtil.KEYBOARD_COLOR_WHITE;
        bytes[2] = 50;
        bytes[3] = 0;
        bytes[4] = CommunicationUtil.SEND_REPORT_ID_LONG_DATA;
        bytes[5] = 48;
        bytes[6] = CommunicationUtil.PAD_ADDRESS;
        bytes[7] = target;
        bytes[8] = CommunicationUtil.SEND_UPGRADE_PACKAGE_COMMAND;
        bytes[9] = CommunicationUtil.KEYBOARD_ADDRESS;
        byte[] offsetB = MiuiKeyboardUtil.int2Bytes(offset);
        bytes[10] = offsetB[3];
        bytes[11] = offsetB[2];
        bytes[12] = offsetB[1];
        byte[] bArr = this.mFileBuf;
        if (bArr.length < offset + 135168 + 52) {
            binlength = (bArr.length - 135168) - offset;
        }
        bytes[13] = CommunicationUtil.COMMAND_AUTH_52;
        System.arraycopy(bArr, 135168 + offset, bytes, 14, binlength);
        bytes[66] = CommunicationUtil.getSum(bytes, 4, 62);
        return bytes;
    }

    public byte[] getUpEndInfo(byte target) {
        byte[] bytes = new byte[68];
        bytes[0] = -86;
        bytes[1] = CommunicationUtil.KEYBOARD_COLOR_WHITE;
        bytes[2] = 50;
        bytes[3] = 0;
        bytes[4] = CommunicationUtil.SEND_REPORT_ID_LONG_DATA;
        bytes[5] = 48;
        bytes[6] = CommunicationUtil.PAD_ADDRESS;
        bytes[7] = target;
        bytes[8] = 4;
        bytes[9] = 14;
        System.arraycopy(this.mBinLengthByte, 0, bytes, 10, 4);
        System.arraycopy(this.mBinStartAddressByte, 0, bytes, 14, 4);
        System.arraycopy(this.mBinCheckSumByte, 0, bytes, 18, 4);
        System.arraycopy(this.mBinVersion, 0, bytes, 22, 2);
        bytes[24] = CommunicationUtil.getSum(bytes, 4, 21);
        return bytes;
    }

    public byte[] getUpFlashInfo(byte target) {
        byte[] bytes = new byte[68];
        bytes[0] = -86;
        bytes[1] = CommunicationUtil.KEYBOARD_COLOR_WHITE;
        bytes[2] = 50;
        bytes[3] = 0;
        bytes[4] = CommunicationUtil.SEND_REPORT_ID_LONG_DATA;
        bytes[5] = 48;
        bytes[6] = CommunicationUtil.PAD_ADDRESS;
        bytes[7] = target;
        bytes[8] = 6;
        bytes[9] = 4;
        System.arraycopy(this.mBinStartAddressByte, 0, bytes, 10, 4);
        bytes[14] = CommunicationUtil.getSum(bytes, 4, 10);
        return bytes;
    }

    public int getBinPacketTotal(int length) {
        switch (length) {
            case 32:
                int i = this.mBinLength;
                int totle = i / 20;
                if (i % 20 != 0) {
                    return totle + 1;
                }
                return totle;
            case 64:
                int i2 = this.mBinLength;
                int totle2 = i2 / 52;
                if (i2 % 52 != 0) {
                    return totle2 + 1;
                }
                return totle2;
            default:
                return 0;
        }
    }
}
