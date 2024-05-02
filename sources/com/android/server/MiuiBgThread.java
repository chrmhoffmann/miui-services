package com.android.server;

import android.os.Handler;
/* loaded from: classes.dex */
public final class MiuiBgThread extends ServiceThread {
    private static Handler sHandler;
    private static MiuiBgThread sInstance;

    private MiuiBgThread() {
        super("miui.bg", 10, true);
    }

    private static void ensureThreadLocked() {
        if (sInstance == null) {
            MiuiBgThread miuiBgThread = new MiuiBgThread();
            sInstance = miuiBgThread;
            miuiBgThread.start();
            sHandler = new Handler(sInstance.getLooper());
        }
    }

    public static MiuiBgThread get() {
        MiuiBgThread miuiBgThread;
        synchronized (MiuiBgThread.class) {
            ensureThreadLocked();
            miuiBgThread = sInstance;
        }
        return miuiBgThread;
    }

    public static Handler getHandler() {
        Handler handler;
        synchronized (MiuiBgThread.class) {
            ensureThreadLocked();
            handler = sHandler;
        }
        return handler;
    }
}
