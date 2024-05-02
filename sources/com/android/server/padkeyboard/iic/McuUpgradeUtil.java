package com.android.server.padkeyboard.iic;

import android.content.Context;
import android.util.Slog;
import com.android.server.padkeyboard.MiuiKeyboardUtil;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
/* loaded from: classes.dex */
public class McuUpgradeUtil {
    private static final String TAG = "IIC_McuUpgradeUtil";
    public int mBinCheckSum;
    public int mBinLength;
    public int mBinPid;
    public int mBinRunStartAddress;
    public String mBinVersion;
    public int mBinVid;
    private Context mContext;
    private byte[] mFileBuf;
    public boolean mIsFileValid;
    public String mOtaFilepath;
    private final byte[] mBinRoDataByte = new byte[4];
    public byte[] mBinRunStartAddressByte = new byte[4];
    public byte[] mBinVidByte = new byte[2];
    public byte[] mBinPidByte = new byte[2];
    public byte[] mBinLengthByte = new byte[4];
    public byte[] mBinStartAddressByte = new byte[4];
    public byte[] mBinCheckSumByte = new byte[4];
    public byte[] mBinFlashAddressByte = new byte[4];
    public byte[] mBinDeviceTypeByte = new byte[1];

    public McuUpgradeUtil(Context context, String otaFilepath) {
        if (otaFilepath == null) {
            Slog.i(TAG, "UpgradeOta file Path is null");
            return;
        }
        this.mContext = context;
        this.mOtaFilepath = otaFilepath;
        Slog.i(TAG, "UpgradeOta file Path:" + this.mOtaFilepath);
        byte[] readUpgradeFile = readUpgradeFile(this.mOtaFilepath);
        this.mFileBuf = readUpgradeFile;
        if (readUpgradeFile == null) {
            Slog.i(TAG, "UpgradeOta file buff is null or length low than 6000");
        } else {
            this.mIsFileValid = parseOtaFile();
        }
    }

    private byte[] readUpgradeFile(String filePath) {
        File mFile = new File(filePath);
        if (mFile.exists()) {
            byte[] fileBuf = readFileToBuf(filePath);
            return fileBuf;
        }
        Slog.i(TAG, "The Upgrade bin file does not exist.");
        return null;
    }

    public boolean isValidFile() {
        return this.mIsFileValid;
    }

    private byte[] readFileToBuf(String fileName) {
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
        String str = this.mBinVersion;
        return str != null && !version.startsWith(str) && this.mBinVersion.compareTo(version) > 0;
    }

    private boolean parseOtaFile() {
        byte[] bArr = this.mFileBuf;
        if (bArr != null && bArr.length > 32768) {
            byte[] binStartFlagByte = new byte[7];
            System.arraycopy(bArr, 32704, binStartFlagByte, 0, binStartFlagByte.length);
            String binStartFlag = MiuiKeyboardUtil.Bytes2String(binStartFlagByte);
            Slog.i(TAG, "Bin Start Flag: " + binStartFlag);
            int fileHeadBaseAddress = 32704 + 7;
            byte[] chipIDByte = new byte[2];
            System.arraycopy(this.mFileBuf, fileHeadBaseAddress, chipIDByte, 0, chipIDByte.length);
            String binChipId = MiuiKeyboardUtil.Bytes2Hex(chipIDByte, 2);
            Slog.i(TAG, "Bin Chip ID: " + binChipId);
            int fileHeadBaseAddress2 = fileHeadBaseAddress + 2;
            byte[] bArr2 = this.mFileBuf;
            byte[] bArr3 = this.mBinRoDataByte;
            System.arraycopy(bArr2, fileHeadBaseAddress2, bArr3, 0, bArr3.length);
            Slog.i(TAG, "Bin RoData : " + MiuiKeyboardUtil.Bytes2Hex(this.mBinRoDataByte, 4));
            int fileHeadBaseAddress3 = fileHeadBaseAddress2 + 4;
            byte[] rameCodeByte = new byte[4];
            System.arraycopy(this.mFileBuf, fileHeadBaseAddress3, rameCodeByte, 0, rameCodeByte.length);
            Slog.i(TAG, "Bin RameCode : " + MiuiKeyboardUtil.Bytes2Hex(rameCodeByte, 4));
            int fileHeadBaseAddress4 = fileHeadBaseAddress3 + 4;
            byte[] rameCodeLenByte = new byte[4];
            System.arraycopy(this.mFileBuf, fileHeadBaseAddress4, rameCodeLenByte, 0, rameCodeLenByte.length);
            Slog.i(TAG, "Bin RameCodeLength : " + MiuiKeyboardUtil.Bytes2Hex(rameCodeLenByte, 4));
            int fileHeadBaseAddress5 = fileHeadBaseAddress4 + 4;
            byte[] bArr4 = this.mFileBuf;
            byte[] bArr5 = this.mBinRunStartAddressByte;
            System.arraycopy(bArr4, fileHeadBaseAddress5, bArr5, 0, bArr5.length);
            byte[] bArr6 = this.mBinRunStartAddressByte;
            this.mBinRunStartAddress = ((bArr6[3] << CommunicationUtil.MCU_ADDRESS) & (-16777216)) + ((bArr6[2] << 16) & 16711680) + ((bArr6[1] << 8) & 65280) + (bArr6[0] & 255);
            Slog.i(TAG, "Bin Run Start Address: " + this.mBinRunStartAddress);
            int fileHeadBaseAddress6 = fileHeadBaseAddress5 + 4;
            byte[] bArr7 = this.mFileBuf;
            byte[] bArr8 = this.mBinPidByte;
            System.arraycopy(bArr7, fileHeadBaseAddress6, bArr8, 0, bArr8.length);
            byte[] bArr9 = this.mBinPidByte;
            this.mBinPid = ((bArr9[1] << 8) & 65280) + (bArr9[0] & 255);
            Slog.i(TAG, "Bin Pid: " + this.mBinPid);
            int fileHeadBaseAddress7 = fileHeadBaseAddress6 + 2;
            byte[] bArr10 = this.mFileBuf;
            byte[] bArr11 = this.mBinVidByte;
            System.arraycopy(bArr10, fileHeadBaseAddress7, bArr11, 0, bArr11.length);
            byte[] bArr12 = this.mBinVidByte;
            this.mBinVid = ((bArr12[1] << 8) & 65280) + (bArr12[0] & 255);
            Slog.i(TAG, "Bin Vid: " + this.mBinVid);
            int fileHeadBaseAddress8 = fileHeadBaseAddress7 + 2 + 2;
            byte[] binVersionByte = new byte[16];
            System.arraycopy(this.mFileBuf, fileHeadBaseAddress8, binVersionByte, 0, binVersionByte.length);
            this.mBinVersion = MiuiKeyboardUtil.Bytes2String(binVersionByte);
            Slog.i(TAG, "Bin Version : " + this.mBinVersion);
            int fileHeadBaseAddress9 = fileHeadBaseAddress8 + 17;
            byte[] bArr13 = this.mFileBuf;
            byte[] bArr14 = this.mBinDeviceTypeByte;
            System.arraycopy(bArr13, 32748, bArr14, 0, bArr14.length);
            byte[] bArr15 = this.mFileBuf;
            byte[] bArr16 = this.mBinLengthByte;
            System.arraycopy(bArr15, fileHeadBaseAddress9, bArr16, 0, bArr16.length);
            byte[] bArr17 = this.mBinLengthByte;
            this.mBinLength = ((bArr17[3] << CommunicationUtil.MCU_ADDRESS) & (-16777216)) + ((bArr17[2] << 16) & 16711680) + ((bArr17[1] << 8) & 65280) + (bArr17[0] & 255);
            Slog.i(TAG, "Bin Length: " + this.mBinLength);
            byte[] bArr18 = this.mFileBuf;
            byte[] bArr19 = this.mBinCheckSumByte;
            System.arraycopy(bArr18, fileHeadBaseAddress9 + 4, bArr19, 0, bArr19.length);
            byte[] bArr20 = this.mBinCheckSumByte;
            this.mBinCheckSum = ((bArr20[3] << CommunicationUtil.MCU_ADDRESS) & (-16777216)) + ((bArr20[2] << 16) & 16711680) + ((bArr20[1] << 8) & 65280) + (bArr20[0] & 255);
            Slog.i(TAG, "Bin CheckSum: " + this.mBinCheckSum);
            byte[] bArr21 = this.mFileBuf;
            int sum1 = CommunicationUtil.getSumInt(bArr21, 32768, bArr21.length - 32768);
            if (sum1 != this.mBinCheckSum) {
                Slog.i(TAG, "Bin Check Check Sum Error:" + sum1 + "/" + this.mBinCheckSum);
                return false;
            }
            byte sum2 = CommunicationUtil.getSum(this.mFileBuf, 32704, 63);
            if (sum2 != this.mFileBuf[32767]) {
                Slog.i(TAG, "Bin Check Head Sum Error:" + ((int) sum2) + "/" + ((int) this.mFileBuf[32767]));
                return false;
            }
            byte[] bArr22 = this.mBinFlashAddressByte;
            bArr22[0] = 0;
            bArr22[1] = CommunicationUtil.PAD_ADDRESS;
            bArr22[2] = 0;
            bArr22[3] = 0;
            byte[] bArr23 = this.mBinStartAddressByte;
            bArr23[0] = 0;
            bArr23[1] = 0;
            bArr23[2] = 0;
            bArr23[3] = 0;
            this.mBinDeviceTypeByte[0] = CommunicationUtil.MCU_ADDRESS;
            return true;
        } else if (bArr != null) {
            Slog.e(TAG, "length = " + this.mFileBuf.length);
            return false;
        } else {
            return false;
        }
    }

    public byte[] getUpgradeCommand() {
        byte[] bytes = new byte[68];
        bytes[0] = -86;
        bytes[1] = CommunicationUtil.KEYBOARD_COLOR_WHITE;
        bytes[2] = 50;
        bytes[3] = 0;
        bytes[4] = CommunicationUtil.SEND_REPORT_ID_LONG_DATA;
        bytes[5] = 48;
        bytes[6] = CommunicationUtil.PAD_ADDRESS;
        bytes[7] = CommunicationUtil.MCU_ADDRESS;
        bytes[8] = 2;
        bytes[9] = 25;
        System.arraycopy(this.mBinLengthByte, 0, bytes, 10, 4);
        System.arraycopy(this.mBinStartAddressByte, 0, bytes, 14, 4);
        System.arraycopy(this.mBinCheckSumByte, 0, bytes, 18, 4);
        System.arraycopy(this.mBinFlashAddressByte, 0, bytes, 22, 4);
        System.arraycopy(this.mBinVidByte, 0, bytes, 26, 2);
        System.arraycopy(this.mBinPidByte, 0, bytes, 28, 2);
        bytes[30] = 1;
        bytes[31] = this.mBinDeviceTypeByte[0];
        int[] tiems = MiuiKeyboardUtil.getYearMonthDayByTimestamp(System.currentTimeMillis());
        bytes[32] = (byte) tiems[0];
        bytes[33] = (byte) tiems[1];
        bytes[34] = (byte) tiems[2];
        bytes[35] = CommunicationUtil.getSum(bytes, 4, 31);
        Slog.i(TAG, "send UpgradeCommand : " + MiuiKeyboardUtil.Bytes2Hex(bytes, bytes.length));
        return bytes;
    }

    public byte[] getBinPackInfo(int offset) {
        byte[] bytes = new byte[68];
        int binlength = 52;
        bytes[0] = -86;
        bytes[1] = CommunicationUtil.KEYBOARD_COLOR_WHITE;
        bytes[2] = 50;
        bytes[3] = 0;
        bytes[4] = CommunicationUtil.SEND_REPORT_ID_LONG_DATA;
        bytes[5] = 48;
        bytes[6] = CommunicationUtil.PAD_ADDRESS;
        bytes[7] = CommunicationUtil.MCU_ADDRESS;
        bytes[8] = CommunicationUtil.SEND_UPGRADE_PACKAGE_COMMAND;
        bytes[9] = CommunicationUtil.KEYBOARD_ADDRESS;
        byte[] offsetB = MiuiKeyboardUtil.int2Bytes(offset);
        bytes[10] = offsetB[3];
        bytes[11] = offsetB[2];
        bytes[12] = offsetB[1];
        byte[] bArr = this.mFileBuf;
        if (bArr.length < offset + 32704 + 52) {
            binlength = (bArr.length - 32704) - offset;
        }
        bytes[13] = CommunicationUtil.COMMAND_AUTH_52;
        System.arraycopy(bArr, offset + 32704, bytes, 14, binlength);
        bytes[66] = CommunicationUtil.getSum(bytes, 4, 62);
        return bytes;
    }

    public byte[] getUpEndInfo() {
        byte[] bytes = new byte[68];
        bytes[0] = -86;
        bytes[1] = CommunicationUtil.KEYBOARD_COLOR_WHITE;
        bytes[2] = 50;
        bytes[3] = 0;
        bytes[4] = CommunicationUtil.SEND_REPORT_ID_LONG_DATA;
        bytes[5] = 48;
        bytes[6] = CommunicationUtil.PAD_ADDRESS;
        bytes[7] = CommunicationUtil.MCU_ADDRESS;
        bytes[8] = 4;
        bytes[9] = 21;
        System.arraycopy(this.mBinLengthByte, 0, bytes, 10, 4);
        System.arraycopy(this.mBinStartAddressByte, 0, bytes, 14, 4);
        System.arraycopy(this.mBinCheckSumByte, 0, bytes, 18, 4);
        System.arraycopy(this.mBinFlashAddressByte, 0, bytes, 22, 4);
        System.arraycopy(this.mBinVidByte, 0, bytes, 26, 2);
        System.arraycopy(this.mBinPidByte, 0, bytes, 28, 2);
        bytes[30] = this.mBinDeviceTypeByte[0];
        bytes[31] = CommunicationUtil.getSum(bytes, 4, 27);
        Slog.i(TAG, "send UpEndInfo : " + MiuiKeyboardUtil.Bytes2Hex(bytes, bytes.length));
        return bytes;
    }

    public byte[] getUpFlashInfo() {
        byte[] bytes = new byte[68];
        bytes[0] = -86;
        bytes[1] = CommunicationUtil.KEYBOARD_COLOR_WHITE;
        bytes[2] = 50;
        bytes[3] = 0;
        bytes[4] = CommunicationUtil.SEND_REPORT_ID_LONG_DATA;
        bytes[5] = 48;
        bytes[6] = CommunicationUtil.PAD_ADDRESS;
        bytes[7] = CommunicationUtil.MCU_ADDRESS;
        bytes[8] = 6;
        bytes[9] = 4;
        System.arraycopy(this.mBinLengthByte, 0, bytes, 10, 4);
        bytes[14] = CommunicationUtil.getSum(bytes, 4, 10);
        return bytes;
    }

    public byte[] getResetInfo() {
        byte[] bytes = new byte[68];
        bytes[0] = -86;
        bytes[1] = CommunicationUtil.KEYBOARD_COLOR_WHITE;
        bytes[2] = 50;
        bytes[3] = 0;
        bytes[4] = CommunicationUtil.SEND_REPORT_ID_LONG_DATA;
        bytes[5] = 48;
        bytes[6] = CommunicationUtil.PAD_ADDRESS;
        bytes[7] = CommunicationUtil.MCU_ADDRESS;
        bytes[8] = 3;
        bytes[9] = 4;
        bytes[10] = CommunicationUtil.RESPONSE_TYPE;
        bytes[11] = CommunicationUtil.SEND_REPORT_ID_SHORT_DATA;
        bytes[12] = CommunicationUtil.KEYBOARD_ADDRESS;
        bytes[13] = 48;
        bytes[14] = CommunicationUtil.getSum(bytes, 4, 10);
        return bytes;
    }

    public int getBinPacketTotal(int length) {
        switch (length) {
            case 32:
                int i = this.mBinLength;
                int totle = (i + 64) / 20;
                if ((i + 64) % 20 != 0) {
                    return totle + 1;
                }
                return totle;
            case 64:
                int i2 = this.mBinLength;
                int totle2 = (i2 + 64) / 52;
                if ((i2 + 64) % 52 != 0) {
                    return totle2 + 1;
                }
                return totle2;
            default:
                return 0;
        }
    }
}
