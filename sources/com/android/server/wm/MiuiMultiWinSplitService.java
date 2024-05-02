package com.android.server.wm;

import android.graphics.Point;
import android.os.Bundle;
import android.os.IBinder;
import miui.app.IMiuiMultiWinSplitManager;
/* loaded from: classes.dex */
public class MiuiMultiWinSplitService extends IMiuiMultiWinSplitManager.Stub {
    ActivityTaskManagerService mActivityTaskManagerService;

    public MiuiMultiWinSplitService(ActivityTaskManagerService mActivityTaskManagerService) {
        this.mActivityTaskManagerService = mActivityTaskManagerService;
    }

    public void handleMultiWindowSwitch(final IBinder iBinder, final Bundle bundle) {
        final MiuiMultiWindowSwitchManager miuiMultiWindowSwitchManager = MiuiMultiWindowSwitchManager.getInstance(this.mActivityTaskManagerService);
        this.mActivityTaskManagerService.mUiHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiMultiWinSplitService.1
            @Override // java.lang.Runnable
            public void run() {
                miuiMultiWindowSwitchManager.addHotArea(iBinder, bundle);
            }
        });
    }

    public Bundle getSplitRootTasksPos(int displayId) {
        MiuiMultiWindowSwitchManager miuiMultiWindowSwitchManager = MiuiMultiWindowSwitchManager.getInstance(this.mActivityTaskManagerService);
        return miuiMultiWindowSwitchManager.getSplitRootTasksPos(displayId);
    }

    public boolean isSupportDragForMultiWin(IBinder iBinder) {
        MiuiMultiWindowSwitchManager miuiMultiWindowSwitchManager = MiuiMultiWindowSwitchManager.getInstance(this.mActivityTaskManagerService);
        return miuiMultiWindowSwitchManager.checkIfSplitAvailable(iBinder);
    }

    public int adjustToSplitWindingMode(IBinder iBinder) {
        MiuiMultiWindowSwitchManager miuiMultiWindowSwitchManager = MiuiMultiWindowSwitchManager.getInstance(this.mActivityTaskManagerService);
        return miuiMultiWindowSwitchManager.adjustToSplitWindingMode(iBinder);
    }

    public boolean getTouchOffsetInTask(IBinder iBinder, Point point) {
        MiuiMultiWindowSwitchManager miuiMultiWindowSwitchManager = MiuiMultiWindowSwitchManager.getInstance(this.mActivityTaskManagerService);
        return miuiMultiWindowSwitchManager.getTouchOffsetInTask(iBinder, point);
    }
}
