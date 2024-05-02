package com.android.server.am;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import com.android.internal.os.BatteryStatsImpl;
import java.io.FileDescriptor;
import java.io.PrintWriter;
/* loaded from: classes.dex */
public class BatteryStatsServiceInjector {
    private static BroadcastReceiver sBroadcastReceiver;
    private static boolean sFirstRecord;
    private static boolean sScreenState;

    private BatteryStatsServiceInjector() {
    }

    public static void registerReceiver(Context context, Handler handler, final BatteryStatsImpl batteryStatsImpl) {
        if (sBroadcastReceiver != null) {
            return;
        }
        sFirstRecord = true;
        sScreenState = true;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.SCREEN_ON");
        intentFilter.addAction("android.intent.action.SCREEN_OFF");
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { // from class: com.android.server.am.BatteryStatsServiceInjector.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                if (intent == null) {
                    return;
                }
                String action = intent.getAction();
                boolean screenOn = "android.intent.action.SCREEN_ON".equals(action);
                if (BatteryStatsServiceInjector.sScreenState != screenOn) {
                    try {
                        CpuTimeCollection.updateUidCpuTime(batteryStatsImpl, screenOn, BatteryStatsServiceInjector.sFirstRecord);
                        BatteryStatsServiceInjector.sFirstRecord = false;
                    } catch (Exception e) {
                        CpuTimeCollection.resetCpuTimeModule();
                        BatteryStatsServiceInjector.sFirstRecord = true;
                    }
                    BatteryStatsServiceInjector.sScreenState = screenOn;
                }
            }
        };
        sBroadcastReceiver = broadcastReceiver;
        context.registerReceiver(broadcastReceiver, intentFilter, null, handler);
    }

    public static void unRegisterReceiver(Context context) {
        BroadcastReceiver broadcastReceiver = sBroadcastReceiver;
        if (broadcastReceiver == null) {
            return;
        }
        context.unregisterReceiver(broadcastReceiver);
        sBroadcastReceiver = null;
    }

    public static void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        writer.println("dump Uid SourceCollection:");
        CpuTimeCollection.dump(fd, writer, args);
    }
}
