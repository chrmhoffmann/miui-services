package com.android.server.wm;

import android.sizecompat.AspectRatioInfo;
import java.io.PrintWriter;
import java.util.Map;
/* loaded from: classes.dex */
public abstract class MiuiSizeCompatInternal {
    public abstract void dump(PrintWriter printWriter, String str);

    public abstract boolean executeShellCommand(String str, String[] strArr, PrintWriter printWriter);

    public abstract int getAspectGravityByPackage(String str);

    public abstract float getAspectRatioByPackage(String str);

    public abstract Map<String, AspectRatioInfo> getMiuiGameSizeCompatEnabledApps();

    public abstract Map<String, AspectRatioInfo> getMiuiSizeCompatEnabledApps();

    public abstract boolean inMiuiGameSizeCompat(String str);

    public abstract boolean isAppSizeCompatRestarting(String str);

    public abstract void onSystemReady(ActivityTaskManagerService activityTaskManagerService);

    public abstract void showWarningNotification(ActivityRecord activityRecord);
}
