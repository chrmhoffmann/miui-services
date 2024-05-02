package com.android.server.notification;

import android.app.Notification;
import android.content.Context;
import android.os.Binder;
import android.provider.Settings;
import com.android.server.notification.NotificationRecord;
import java.util.Iterator;
/* loaded from: classes.dex */
public class NotificationLightController {
    public static NotificationRecord.Light customizeNotificationLight(NotificationManagerService service, NotificationRecord ledNotification) {
        int defaultColor = service.getContext().getResources().getColor(17170813);
        customizeNotificationLight(service, ledNotification.getNotification(), defaultColor);
        int ledARGB = ledNotification.getNotification().ledARGB;
        if (ledARGB == 0) {
            return ledNotification.getLight();
        }
        int onMs = ledNotification.getNotification().ledOnMS;
        int offMs = ledNotification.getNotification().ledOffMS;
        return new NotificationRecord.Light(ledARGB, onMs, offMs);
    }

    public static void customizeNotificationLight(NotificationManagerService service, Notification notification, int defaultNotificationColor) {
        long identify = Binder.clearCallingIdentity();
        boolean customized = false;
        Iterator it = service.mLights.iterator();
        while (it.hasNext()) {
            String light = (String) it.next();
            if (light.contains("com.android.phone") || light.contains("com.android.server.telecom")) {
                customizeNotificationLight(service.getContext(), notification, "call_breathing_light_color", "call_breathing_light_freq", defaultNotificationColor);
                return;
            } else if (light.contains("com.android.mms")) {
                customizeNotificationLight(service.getContext(), notification, "mms_breathing_light_color", "mms_breathing_light_freq", defaultNotificationColor);
                customized = true;
            }
        }
        if (customized) {
            return;
        }
        if ((notification.defaults & 4) != 0) {
            customizeNotificationLight(service.getContext(), notification, "breathing_light_color", "breathing_light_freq", defaultNotificationColor);
        }
        Binder.restoreCallingIdentity(identify);
    }

    private static void customizeNotificationLight(Context context, Notification notification, String colorKey, String freqKey, int defaultNotificationColor) {
        int defaultFreq = context.getResources().getInteger(285933595);
        notification.ledARGB = Settings.System.getIntForUser(context.getContentResolver(), colorKey, defaultNotificationColor, -2);
        int freq = Settings.System.getIntForUser(context.getContentResolver(), freqKey, defaultFreq, -2);
        int[] offOn = getLedPwmOffOn(freq < 0 ? defaultFreq : freq);
        notification.ledOnMS = offOn[1];
        notification.ledOffMS = offOn[0];
    }

    public static int[] getLedPwmOffOn(int totalLength) {
        int[] values = {(totalLength / 4) * 3, totalLength - values[0]};
        return values;
    }
}
