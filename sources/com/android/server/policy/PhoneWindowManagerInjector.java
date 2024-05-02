package com.android.server.policy;

import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.os.SystemProperties;
import android.util.Slog;
import android.view.KeyEvent;
import com.android.internal.policy.PhoneWindow;
/* loaded from: classes.dex */
class PhoneWindowManagerInjector {
    static final String TAG = "starting_window";
    private static boolean sEnableSW = SystemProperties.getBoolean("persist.startingwindow.enable", false);

    PhoneWindowManagerInjector() {
    }

    static void setDefaultBackgroundDrawable(PhoneWindow win) {
        if (!sEnableSW) {
            return;
        }
        TypedArray a = win.getWindowStyle();
        int windowBackgroundId = a.getResourceId(1, 0);
        boolean windowIsTranslucent = a.getBoolean(5, false);
        boolean windowDisableStarting = a.getBoolean(12, false);
        if (windowBackgroundId == 0 || windowIsTranslucent || windowDisableStarting) {
            win.setBackgroundDrawable(new ColorDrawable(-1));
            Slog.d(TAG, "add default startingwindow");
        }
    }

    static void performReleaseHapticFeedback(PhoneWindowManager manager, KeyEvent event, int policyFlags) {
        if (event.getAction() == 0) {
        }
    }
}
