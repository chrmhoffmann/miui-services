package com.android.server.audio.dolbyeffect.deviceinfo;
/* loaded from: classes.dex */
public class UsbDeviceInfo extends DeviceInfoBase {
    int mProductID;
    int mVendorID;

    public int getVendorID() {
        return this.mVendorID;
    }

    public void setVendorID(int VendorID) {
        this.mVendorID = VendorID;
    }

    public int getProductID() {
        return this.mProductID;
    }

    public void setProductID(int ProductID) {
        this.mProductID = ProductID;
    }

    public UsbDeviceInfo(int mDeviceType, int mVendorID, int mProductID) {
        this.mVendorID = mVendorID;
        this.mProductID = mProductID;
        this.mDeviceType = mDeviceType;
        this.mDevice = "USB headset";
    }
}
