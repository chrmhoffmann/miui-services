package com.android.server.am;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.Process;
import android.util.Slog;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public final class MiuiMemServiceHelper extends BroadcastReceiver {
    private static final long CHARGING_RECLAIM_DELAYED = 60000;
    private static final boolean DEBUG = MiuiMemoryService.DEBUG;
    private static final long SCREENOFF_RECLAIM_DELAYED = 1200000;
    private static final String TAG = "MiuiMemoryService";
    private IntentFilter mFilter;
    private boolean mIsCharging;
    private boolean mIsScreenOff;
    private MiuiMemoryService mMemService;
    private PowerManager mPowerManager;
    private boolean mSetIsSatisfied;
    private WorkHandler mWorkHandler;
    final HandlerThread mWorkThread = new HandlerThread("MiuiMemoryService_Helper");

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public class WorkHandler extends Handler {
        public static final int CHARGING_RECLAIM = 1;
        public static final int SCREENOFF_RECLAIM = 2;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public WorkHandler(Looper looper) {
            super(looper);
            MiuiMemServiceHelper.this = r1;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                case 2:
                    Slog.v("MiuiMemoryService", "start screen off reclaim... " + msg.what);
                    MiuiMemServiceHelper.this.mMemService.runProcsCompaction(2);
                    return;
                default:
                    return;
            }
        }
    }

    public MiuiMemServiceHelper(Context context, MiuiMemoryService service) {
        this.mPowerManager = (PowerManager) context.getSystemService(PowerManager.class);
        this.mMemService = service;
        if (this.mPowerManager != null) {
            registerBroadcast(context);
        } else {
            Slog.e("MiuiMemoryService", "Register broadcast failed!");
        }
    }

    public void startWork() {
        this.mWorkThread.start();
        this.mWorkHandler = new WorkHandler(this.mWorkThread.getLooper());
        Process.setThreadGroupAndCpuset(this.mWorkThread.getThreadId(), 2);
    }

    private void registerBroadcast(Context context) {
        IntentFilter intentFilter = new IntentFilter();
        this.mFilter = intentFilter;
        intentFilter.addAction("android.intent.action.SCREEN_ON");
        this.mFilter.addAction("android.intent.action.SCREEN_OFF");
        this.mFilter.addAction("android.intent.action.DREAMING_STARTED");
        this.mFilter.addAction("android.intent.action.DREAMING_STOPPED");
        this.mFilter.addAction("android.intent.action.ACTION_POWER_CONNECTED");
        this.mFilter.addAction("android.intent.action.ACTION_POWER_DISCONNECTED");
        context.registerReceiver(this, this.mFilter);
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        char c;
        String action = intent.getAction();
        boolean z = false;
        switch (action.hashCode()) {
            case -2128145023:
                if (action.equals("android.intent.action.SCREEN_OFF")) {
                    c = 3;
                    break;
                }
                c = 65535;
                break;
            case -1886648615:
                if (action.equals("android.intent.action.ACTION_POWER_DISCONNECTED")) {
                    c = 5;
                    break;
                }
                c = 65535;
                break;
            case -1454123155:
                if (action.equals("android.intent.action.SCREEN_ON")) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case 244891622:
                if (action.equals("android.intent.action.DREAMING_STARTED")) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case 257757490:
                if (action.equals("android.intent.action.DREAMING_STOPPED")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case 1019184907:
                if (action.equals("android.intent.action.ACTION_POWER_CONNECTED")) {
                    c = 4;
                    break;
                }
                c = 65535;
                break;
            default:
                c = 65535;
                break;
        }
        switch (c) {
            case 0:
                if (!this.mPowerManager.isInteractive()) {
                    return;
                }
            case 1:
                this.mIsScreenOff = false;
                break;
            case 2:
            case 3:
                this.mIsScreenOff = true;
                break;
            case 4:
                this.mIsCharging = true;
                break;
            case 5:
                this.mIsCharging = false;
                break;
            default:
                return;
        }
        if (DEBUG) {
            Slog.v("MiuiMemoryService", String.format("Received:%s, cur:%s,%s", action, Boolean.valueOf(this.mIsCharging), Boolean.valueOf(this.mIsScreenOff)));
        }
        if (this.mIsScreenOff && !this.mWorkHandler.hasMessages(2)) {
            this.mWorkHandler.sendEmptyMessageDelayed(2, SCREENOFF_RECLAIM_DELAYED);
        } else {
            this.mWorkHandler.removeMessages(2);
            this.mMemService.interruptProcsCompaction();
        }
        if (this.mIsCharging && this.mIsScreenOff) {
            z = true;
        }
        boolean curIsSatisfied = z;
        if (curIsSatisfied != this.mSetIsSatisfied) {
            this.mWorkHandler.removeMessages(1);
            if (curIsSatisfied) {
                this.mWorkHandler.sendEmptyMessageDelayed(1, 60000L);
            } else {
                this.mMemService.interruptProcsCompaction();
            }
            this.mSetIsSatisfied = curIsSatisfied;
        }
    }
}
