package com.android.server.audio.dolbyeffect.deviceinfo;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
/* loaded from: classes.dex */
public class BtDeviceInfo extends DeviceInfoBase {
    private static final int ATSETTING_VERSION = 0;
    private static final String AT_SETTING_PRE_SUFFIX = "FF";
    private static final String HEADSET_AT_COMMAND_TAG = "01020101";
    int mMajorID;
    int mMinorID;
    boolean mState;

    public int getMajorID() {
        return this.mMajorID;
    }

    public void setMajorID(int MajorID) {
        this.mMajorID = MajorID;
    }

    public int getMinorID() {
        return this.mMinorID;
    }

    public void setMinorID(int MinorID) {
        this.mMinorID = MinorID;
    }

    public boolean getState() {
        return this.mState;
    }

    public void setState(boolean State) {
        this.mState = State;
    }

    public BtDeviceInfo(int deviceType, int majorID, int minorID, String device, boolean state) {
        this.mMajorID = majorID;
        this.mMinorID = minorID;
        this.mDevice = device;
        this.mDeviceType = deviceType;
        this.mState = state;
    }

    /* JADX WARN: Multi-variable type inference failed */
    public static boolean tryGetIdsFromIntent(Intent intent, int[] ids, String[] deviceName) {
        char c;
        int i = 2;
        boolean z = false;
        if (ids.length == 2) {
            if (deviceName.length != 1) {
                return false;
            }
            BluetoothDevice extraDevice = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
            deviceName[0] = extraDevice != null ? extraDevice.getAddress() : "NULL";
            Object[] args = (Object[]) intent.getExtra("android.bluetooth.headset.extra.VENDOR_SPECIFIC_HEADSET_EVENT_ARGS");
            if (args != null && args.length == 1) {
                int majorId = 0;
                int minorId = 0;
                String local = ((String) args[0]).toUpperCase();
                if (!local.startsWith(AT_SETTING_PRE_SUFFIX) || !local.endsWith(AT_SETTING_PRE_SUFFIX)) {
                    return false;
                }
                if (local.length() < 4) {
                    return false;
                }
                String local2 = local.substring(2, local.length() - 2);
                if (!local2.startsWith(HEADSET_AT_COMMAND_TAG)) {
                    return false;
                }
                String local3 = local2.substring(8);
                byte[] argbyte = hexToByteArray(local3);
                if (argbyte[1] == 0 && (argbyte[0] + 1) * 2 == local3.length()) {
                    byte[] argbyte2 = hexToByteArray(local3.substring(4));
                    int position = 2;
                    for (int i2 = 1; position <= argbyte2.length - i2; i2 = 1) {
                        int lengthFlag = argbyte2[position - 2];
                        if ((position + lengthFlag) - i > argbyte2.length - i2) {
                            return z;
                        }
                        int tag = argbyte2[position - 1];
                        byte[] arr = new byte[lengthFlag];
                        int i3 = z ? 1 : 0;
                        int i4 = z ? 1 : 0;
                        System.arraycopy(argbyte2, position, arr, i3, lengthFlag - 1);
                        if (tag != 32) {
                            c = 4;
                        } else {
                            int majorId2 = arr[3] + (arr[i] * 256) + (arr[1] * 256 * 256);
                            c = 4;
                            minorId = (arr[5] * 256) + arr[4];
                            majorId = majorId2;
                        }
                        position += lengthFlag + 1;
                        i = 2;
                        z = false;
                    }
                    if (majorId == 0 && minorId == 0) {
                        return false;
                    }
                    ids[0] = majorId;
                    ids[1] = minorId;
                    return true;
                }
                return false;
            }
            return false;
        }
        return false;
    }

    public static byte[] hexToByteArray(String digits) {
        int bytes = digits.length() / 2;
        byte[] result = new byte[bytes];
        for (int i = 0; i < digits.length(); i += 2) {
            result[i / 2] = (byte) Integer.parseInt(digits.substring(i, i + 2), 16);
        }
        return result;
    }
}
