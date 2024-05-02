package com.android.server.padkeyboard.usb;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.util.Slog;
import com.android.server.padkeyboard.MiuiPadKeyboardManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
/* loaded from: classes.dex */
public abstract class UpgradeHelper {
    protected static final String BIN_PATH = "/vendor/etc/";
    protected static final int MAX_RETRY_TIMES = 3;
    protected static final int MIN_FILE_LENGTH = 64;
    protected int mBinLength;
    protected int mCheckSum;
    protected Context mContext;
    protected byte[] mFileBuf;
    protected UsbEndpoint mInUsbEndpoint;
    protected UsbEndpoint mOutUsbEndpoint;
    protected UsbDeviceConnection mUsbConnection;
    protected UsbDevice mUsbDevice;
    protected UsbInterface mUsbInterface;
    protected String mVersion = "0000";
    protected final byte[] mSendBuf = new byte[64];
    protected final byte[] mRecBuf = new byte[64];

    public abstract byte[] getBinPackInfo(int i, int i2);

    public abstract int getBinPacketNum(int i);

    public abstract byte[] getResetCommand();

    public abstract byte[] getUpEndCommand();

    public abstract byte[] getUpgradeCommand();

    protected abstract void parseUpgradeFileHead();

    public abstract boolean startUpgrade(UsbDevice usbDevice, UsbDeviceConnection usbDeviceConnection, UsbEndpoint usbEndpoint, UsbEndpoint usbEndpoint2);

    public UpgradeHelper(Context context, String binPath) {
        this.mContext = context;
        readUpgradeFile(binPath);
    }

    protected void readUpgradeFile(String binPath) {
        File binFile = new File(binPath);
        if (binFile.exists()) {
            this.mFileBuf = readFileToBuf(binFile);
        } else {
            Slog.i(MiuiPadKeyboardManager.TAG, "upgrade file not exist: " + binFile.getPath());
        }
    }

    protected byte[] readFileToBuf(File file) {
        byte[] fileBuf = null;
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            try {
                int fileLen = fileInputStream.available();
                fileBuf = new byte[fileLen];
                fileInputStream.read(fileBuf);
                fileInputStream.close();
            } catch (Throwable th) {
                try {
                    fileInputStream.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
                throw th;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();
        }
        return fileBuf;
    }

    public boolean isLowerVersionThan(String version) {
        String str;
        return this.mFileBuf == null || (str = this.mVersion) == null || (version != null && version.compareTo(str) >= 0);
    }

    public String getVersion() {
        return this.mVersion;
    }

    public boolean sendUsbData(UsbDeviceConnection connection, UsbEndpoint endpoint, byte[] data) {
        return (connection == null || endpoint == null || data == null || connection.bulkTransfer(endpoint, data, data.length, 500) == -1) ? false : true;
    }
}
