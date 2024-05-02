package com.android.server.policy;

import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
/* loaded from: classes.dex */
public class FindDevicePowerOffLocateManager {
    public static final String GLOBAL_ACTIONS = "global_actions";
    public static final String IMPERCEPTIBLE_POWER_PRESS = "imperceptible_power_press";

    public static void sendFindDeviceLocateBroadcast(Context context, String cause) {
        Intent intent = new Intent("miui.intent.action.FIND_DEVICE_LOCATE");
        intent.setPackage("com.xiaomi.finddevice");
        intent.putExtra("cause", cause);
        context.sendBroadcastAsUser(intent, UserHandle.OWNER);
    }
}
