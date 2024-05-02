package com.android.server.usb;

import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.os.UserHandle;
import android.util.Slog;
import com.android.server.MiuiBatteryStatsService;
import com.android.server.usb.descriptors.UsbDescriptorParser;
import com.miui.base.MiuiStubRegistry;
import miui.os.Build;
import miui.util.IMiCharge;
/* loaded from: classes.dex */
public class MiuiUsbServiceImpl implements MiuiUsbServiceStub {
    private static final int FLAG_NON_ANONYMOUS = 2;
    public static final int SPECIAL_PRODUCT_ID = 16380;
    public static final int SPECIAL_VENDOR_ID = 12806;
    private final String TAG = "MiuiUsbServiceImpl";

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MiuiUsbServiceImpl> {

        /* compiled from: MiuiUsbServiceImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MiuiUsbServiceImpl INSTANCE = new MiuiUsbServiceImpl();
        }

        public MiuiUsbServiceImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public MiuiUsbServiceImpl provideNewInstance() {
            return new MiuiUsbServiceImpl();
        }
    }

    public void collectUsbHostConnectedInfo(Context context, UsbDescriptorParser parser) {
        IMiCharge miCharge = IMiCharge.getInstance();
        if (!Build.IS_INTERNATIONAL_BUILD) {
            boolean hasAudio = parser.hasAudioInterface();
            boolean hasHid = parser.hasHIDInterface();
            boolean hasStorage = parser.hasStorageInterface();
            String value = "";
            if (miCharge.isDPConnected()) {
                value = value + "DP";
            } else {
                if (hasAudio) {
                    value = value + "Audio";
                }
                if (hasHid) {
                    value = value + "HID";
                }
                if (hasStorage) {
                    value = value + "Storage";
                }
                if (value == null || value.length() == 0) {
                    value = value + "Other";
                }
            }
            Intent intent = new Intent(MiuiBatteryStatsService.TrackBatteryUsbInfo.ACTION_TRACK_EVENT);
            intent.setPackage(MiuiBatteryStatsService.TrackBatteryUsbInfo.ANALYTICS_PACKAGE);
            intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_APP_ID, MiuiBatteryStatsService.TrackBatteryUsbInfo.USB_APP_ID);
            intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_EVENT_NAME, "usb_host");
            intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.PARAM_PACKAGE, "Android");
            intent.putExtra("device_connected", value);
            intent.putExtra(MiuiBatteryStatsService.TrackBatteryUsbInfo.USB32, miCharge.isUSB32() ? 1 : 0);
            if (!Build.IS_INTERNATIONAL_BUILD) {
                intent.setFlags(2);
            }
            try {
                context.startServiceAsUser(intent, UserHandle.CURRENT);
            } catch (IllegalStateException e) {
                Slog.e("MiuiUsbServiceImpl", "Start one-Track service failed", e);
            }
            Slog.d("MiuiUsbServiceImpl", "connected device : " + value);
        }
    }

    public boolean isSpecialKeyBoard(UsbDevice device) {
        if (12806 == device.getVendorId() && 16380 == device.getProductId()) {
            return true;
        }
        return false;
    }
}
