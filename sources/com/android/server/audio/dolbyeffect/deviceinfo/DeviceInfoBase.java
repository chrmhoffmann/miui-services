package com.android.server.audio.dolbyeffect.deviceinfo;
/* loaded from: classes.dex */
public class DeviceInfoBase {
    String mDevice;
    int mDeviceType;
    public static int TYPE_USB = 0;
    public static int TYPE_WIRED = 1;
    public static int TYPE_BT = 2;

    public int getDeviceType() {
        return this.mDeviceType;
    }

    public void setDeviceType(int DeviceType) {
        this.mDeviceType = DeviceType;
    }

    public String getDevice() {
        return this.mDevice;
    }

    public void setDevice(String Device) {
        this.mDevice = Device;
    }

    public String toString() {
        return "BaseDeviceProduct{mDeviceType=" + this.mDeviceType + ", mDevice='" + this.mDevice + "'}";
    }
}
