package com.android.server.net;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.INetworkManagementService;
import java.io.FileDescriptor;
import java.io.PrintWriter;
/* loaded from: classes.dex */
public class NetworkStatsServiceInjector {
    private static BroadcastReceiver sBroadcastReceiver;
    private static INetworkManagementService sNetworkManager;
    private static boolean sScreenState;

    private NetworkStatsServiceInjector() {
    }

    public static void registerReceiver(Context context, Handler handler, INetworkManagementService networkManager) {
        if (sBroadcastReceiver != null) {
            return;
        }
        sScreenState = true;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.SCREEN_ON");
        intentFilter.addAction("android.intent.action.SCREEN_OFF");
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { // from class: com.android.server.net.NetworkStatsServiceInjector.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                boolean screenOn;
                if (intent == null) {
                    return;
                }
                String action = intent.getAction();
                if ("android.intent.action.SCREEN_ON".equals(action)) {
                    screenOn = true;
                } else {
                    screenOn = false;
                }
                if (NetworkStatsServiceInjector.sScreenState != screenOn) {
                    NetworkStatsServiceInjector.updateForScreenChanged(screenOn);
                    NetworkStatsServiceInjector.sScreenState = screenOn;
                }
            }
        };
        sBroadcastReceiver = broadcastReceiver;
        sNetworkManager = networkManager;
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

    public static void updateForScreenChanged(boolean screenOn) {
        NetworkStatsActualCollection.updateNetworkStats(sNetworkManager, screenOn);
    }

    public static void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        writer.println("dump Uid SourceCollection:");
        NetworkStatsActualCollection.dump(fd, writer, args);
    }
}
