package com.android.server.wm;

import android.app.ActivityManager;
import android.view.SurfaceControl;
import android.window.WindowContainerToken;
import android.window.WindowContainerTransaction;
import com.android.server.wm.MiuiCvwGestureController;
/* loaded from: classes.dex */
class MiuiCvwTaskOrganizer {
    private static final String TAG = MiuiCvwTaskOrganizer.class.getSimpleName();
    MiuiCvwGestureController mController;
    private SurfaceControl mLeash;
    private WindowContainerToken mToken;

    public MiuiCvwTaskOrganizer(MiuiCvwGestureController controller) {
        this.mController = controller;
    }

    public void onTaskAppeared(ActivityManager.RunningTaskInfo taskInfo) {
    }

    public void onTaskVanished(ActivityManager.RunningTaskInfo taskInfo) {
        MiuiCvwGestureController.Slog.d(TAG, "onTaskVanished: " + taskInfo);
    }

    public void onTaskInfoChanged(ActivityManager.RunningTaskInfo taskInfo) {
        MiuiCvwGestureController.Slog.d(TAG, "onTaskInfoChanged: " + taskInfo);
    }

    public void applyTransaction(WindowContainerTransaction tct) {
    }
}
