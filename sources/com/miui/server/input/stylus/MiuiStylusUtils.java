package com.miui.server.input.stylus;

import android.app.ActivityManager;
import android.view.WindowManager;
import miui.util.FeatureParser;
/* loaded from: classes.dex */
public class MiuiStylusUtils {
    private static final int DURATION_TIMEOUT = 6000;
    private static final int FLAG_SHOW_FOR_ALL_USERS = 16;

    private MiuiStylusUtils() {
    }

    public static boolean supportStylusGesture() {
        return FeatureParser.getBoolean("support_stylus_gesture", false);
    }

    public static void initLayoutParam(WindowManager.LayoutParams lp) {
        lp.width = -1;
        lp.height = -1;
        lp.type = 2024;
        lp.flags = 263460;
        lp.layoutInDisplayCutoutMode = 1;
        lp.format = -3;
        lp.windowAnimations = -1;
        lp.setFitInsetsTypes(0);
        if (ActivityManager.isHighEndGfx()) {
            lp.flags |= 16777216;
            lp.privateFlags |= 2;
        }
        lp.privateFlags |= 16;
        lp.hideTimeoutMilliseconds = 6000L;
        lp.setTitle("StylusMask");
    }
}
