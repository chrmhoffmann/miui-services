package com.miui.server.input;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Slog;
/* loaded from: classes.dex */
public class MiuiInputMonitor {
    private static final String TAG = "MiuiInputMonitor";
    private static volatile MiuiInputMonitor sInstance;
    private final Handler mHandler;

    private MiuiInputMonitor() {
        HandlerThread handlerThread = new HandlerThread("input_monitor_monitor", -4);
        handlerThread.start();
        this.mHandler = new Handler(handlerThread.getLooper());
        Slog.d(TAG, "Input monitor's monitor thread start");
    }

    public static MiuiInputMonitor getInstance() {
        if (sInstance == null) {
            synchronized (MiuiInputMonitor.class) {
                if (sInstance == null) {
                    sInstance = new MiuiInputMonitor();
                }
            }
        }
        return sInstance;
    }

    public void onEvent(Runnable runnable) {
        this.mHandler.postDelayed(runnable, 2500L);
    }

    public void onFinishEvent(Runnable runnable) {
        this.mHandler.removeCallbacks(runnable);
    }
}
