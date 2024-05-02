package com.android.server.policy;

import android.util.Slog;
import com.android.server.LocalServices;
import com.android.server.input.ReflectionUtils;
/* loaded from: classes.dex */
public class MiuiInputLog {
    private static final int DETAIL = 1;
    private static final int MAJOR = 0;
    private static final String TAG = "MiuiInputKeyEventLog";
    private static volatile MiuiInputLog sInstance;
    private int mInputDebugLevel = 0;
    private WindowManagerPolicy mWindowManagerPolicy = (WindowManagerPolicy) LocalServices.getService(WindowManagerPolicy.class);

    private MiuiInputLog() {
    }

    public static MiuiInputLog getInstance() {
        if (sInstance == null) {
            synchronized (MiuiInputLog.class) {
                if (sInstance == null) {
                    sInstance = new MiuiInputLog();
                }
            }
        }
        return sInstance;
    }

    public static void error(String msg) {
        Slog.e(TAG, msg);
    }

    public static void error(String msg, Throwable throwable) {
        Slog.e(TAG, msg, throwable);
    }

    public static void defaults(String msg) {
        Slog.w(TAG, msg);
    }

    public static void defaults(String msg, Throwable throwable) {
        Slog.w(TAG, msg, throwable);
    }

    public static void major(String msg) {
        Slog.i(TAG, msg);
    }

    public static void major(String msg, Throwable throwable) {
        Slog.i(TAG, msg, throwable);
    }

    public static void detail(String msg) {
        if (getLogLevel() >= 1) {
            Slog.d(TAG, msg);
        }
    }

    public static void detail(String msg, Throwable throwable) {
        if (getLogLevel() >= 1) {
            Slog.d(TAG, msg, throwable);
        }
    }

    private static int getLogLevel() {
        return getInstance().mInputDebugLevel;
    }

    public void setLogLevel(int logLevel) {
        defaults("setLogLevel:" + logLevel);
        this.mInputDebugLevel = logLevel;
        boolean flag = logLevel >= 1;
        PhoneWindowManager phoneWindowManager = this.mWindowManagerPolicy;
        if (phoneWindowManager instanceof PhoneWindowManager) {
            ReflectionUtils.callPrivateMethod(PhoneWindowManager.class, phoneWindowManager, "setDebugSwitch", Boolean.valueOf(flag));
        }
    }
}
