package com.android.server.input;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Slog;
/* loaded from: classes.dex */
public final class MiuiInputThread extends HandlerThread {
    private static final String TAG = "MiuiInputThread";
    private static volatile Handler sHandler;
    private static volatile MiuiInputThread sInstance;

    private MiuiInputThread() {
        super("miui_input_thread", -4);
        Slog.d(TAG, "Shared singleton Thread for input services start");
    }

    private static void ensureThreadLocked() {
        if (sInstance == null) {
            sInstance = new MiuiInputThread();
            sInstance.start();
            sHandler = new Handler(sInstance.getLooper());
        }
    }

    public static MiuiInputThread getThread() {
        MiuiInputThread miuiInputThread;
        synchronized (MiuiInputThread.class) {
            ensureThreadLocked();
            miuiInputThread = sInstance;
        }
        return miuiInputThread;
    }

    public static Handler getHandler() {
        Handler handler;
        synchronized (MiuiInputThread.class) {
            ensureThreadLocked();
            handler = sHandler;
        }
        return handler;
    }
}
