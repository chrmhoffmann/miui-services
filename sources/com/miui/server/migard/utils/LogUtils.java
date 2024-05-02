package com.miui.server.migard.utils;

import android.os.Build;
import android.util.Slog;
/* loaded from: classes.dex */
public final class LogUtils {
    private static boolean DEBUG_VERSION = Build.IS_DEBUGGABLE;
    private static boolean DEBUG = false;

    private LogUtils() {
    }

    public static void i(String tag, String msg) {
        Slog.i(tag, msg);
    }

    public static void d(String tag, String msg) {
        if (DEBUG_VERSION || DEBUG) {
            Slog.d(tag, msg);
        }
    }

    public static void e(String tag, String msg) {
        Slog.e(tag, msg);
    }

    public static void e(String tag, String msg, Throwable tr) {
        Slog.e(tag, msg, tr);
    }
}
