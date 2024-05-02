package com.android.server;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.server.am.BroadcastQueueImpl;
import com.miui.base.MiuiStubRegistry;
/* loaded from: classes.dex */
public class WiredAccessoryManagerStubImpl implements WiredAccessoryManagerStub {
    private static final int NOTE_USB_UNSUPPORT_HEADSET_PLUG = 1397262472;
    private static final int SW_JACK_UNSUPPORTED_INSERT_BIT = 524288;
    private static final int SW_JACK_UNSUPPORTED_INSERT_BIT_21 = 1048576;
    private static final int SW_JACK_VIDEOOUT_INSERT = 256;
    private static final String TAG = "WiredAccessoryManagerStubImpl";
    private NotificationManager mNm;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<WiredAccessoryManagerStubImpl> {

        /* compiled from: WiredAccessoryManagerStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final WiredAccessoryManagerStubImpl INSTANCE = new WiredAccessoryManagerStubImpl();
        }

        public WiredAccessoryManagerStubImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public WiredAccessoryManagerStubImpl provideNewInstance() {
            return new WiredAccessoryManagerStubImpl();
        }
    }

    public boolean isDeviceUnsupported(Context context, int switchValues, int switchMask) {
        if (isUnsupportedBit(switchMask)) {
            Log.d(TAG, "Device unsupported");
            showUnsupportDeviceNotification(context, switchValues);
            return true;
        }
        return false;
    }

    private boolean isUnsupportedBit(int switchMask) {
        return "lmi".equals(Build.DEVICE) ? (SW_JACK_UNSUPPORTED_INSERT_BIT_21 & switchMask) != 0 : (524288 & switchMask) != 0 || switchMask == 276;
    }

    private void showUnsupportDeviceNotification(Context context, int switchValues) {
        if (context != null) {
            if (this.mNm == null) {
                this.mNm = (NotificationManager) context.getSystemService("notification");
            }
            if (switchValues == 524308 || switchValues == 276 || switchValues == 1048596) {
                Log.d(TAG, "Unsupported headset inserted");
                createNotification(context);
            } else if (switchValues == 0) {
                Log.d(TAG, "Unsupported headset removed");
                this.mNm.cancel(NOTE_USB_UNSUPPORT_HEADSET_PLUG);
            }
        }
    }

    private void createNotification(Context context) {
        Intent intent = Intent.makeRestartActivityTask(new ComponentName("com.android.settings", "com.android.settings.Settings$UsbHeadsetUnSupportActivity"));
        PendingIntent pit = PendingIntent.getActivity(context, 0, intent, BroadcastQueueImpl.FLAG_IMMUTABLE);
        String channel = SystemNotificationChannels.USB_HEADSET;
        Notification.Builder builder = new Notification.Builder(context, channel).setSmallIcon(17303662).setWhen(0L).setOngoing(true).setDefaults(0).setColor(context.getColor(17170460)).setCategory("sys").setVisibility(1).setContentIntent(pit).setContentTitle(context.getString(286196245)).setContentText(context.getString(286196243));
        Notification notify = builder.build();
        this.mNm.notify(NOTE_USB_UNSUPPORT_HEADSET_PLUG, notify);
    }
}
