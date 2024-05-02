package com.android.server.wm;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.IBinder;
import android.util.MiuiMultiWindowUtils;
import com.miui.base.MiuiStubRegistry;
import java.io.PrintWriter;
/* loaded from: classes.dex */
public class MiuiMultiWindowServiceImpl implements MiuiMultiWindowServiceStub {
    public static final String TAG = "MiuiMultiWindowATMImpl";
    ActivityTaskManagerService mAtms;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MiuiMultiWindowServiceImpl> {

        /* compiled from: MiuiMultiWindowServiceImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MiuiMultiWindowServiceImpl INSTANCE = new MiuiMultiWindowServiceImpl();
        }

        public MiuiMultiWindowServiceImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public MiuiMultiWindowServiceImpl provideNewInstance() {
            return new MiuiMultiWindowServiceImpl();
        }
    }

    public void init(ActivityTaskManagerService service) {
        this.mAtms = service;
    }

    public void moveActivityTaskToBackEx(IBinder token) {
        ActivityRecord record;
        if (token != null && (record = ActivityRecord.forTokenLocked(token)) != null) {
            if (record.inSplitScreenWindowingMode() && record.isVisible()) {
                MiuiMultiWindowManager.getInstance(this.mAtms).takeTaskSnapshot(record.token, false);
            } else {
                record.inFreeformWindowingMode();
            }
        }
    }

    public void dump(PrintWriter pw, String[] args, int opti) {
        MiuiMultiWindowSwitchManager miuiMultiWindowSwitchManager = MiuiMultiWindowSwitchManager.getInstance(this.mAtms);
        miuiMultiWindowSwitchManager.dump(pw, args, opti);
    }

    public boolean isCtsModeEnabled() {
        MiuiMultiWindowSwitchManager miuiMultiWindowSwitchManager = MiuiMultiWindowSwitchManager.getInstance(this.mAtms);
        return miuiMultiWindowSwitchManager.isCtsModeEnabled();
    }

    public boolean isProcessingDrag() {
        MiuiMultiWindowSwitchManager miuiMultiWindowSwitchManager = MiuiMultiWindowSwitchManager.getInstance(this.mAtms);
        return miuiMultiWindowSwitchManager.isProcessingDrag();
    }

    public boolean isMiuiMultiWinChangeSupport() {
        return MiuiMultiWindowUtils.MULTIWIN_SWITCH_ENABLED;
    }

    public float getSplitScreenDragBarWidth(Resources resources) {
        return resources != null ? resources.getDimension(285671520) : MiuiFreeformPinManagerService.EDGE_AREA;
    }

    public boolean isEventXInSplitDragBarRegion(DisplayContent displayContent, Context context, float eventX, float splitScreenDragBarWidth) {
        Task topRootTaskInWindowingMode;
        if (displayContent != null && context != null) {
            TaskDisplayArea taskDisplayArea = displayContent.getDefaultTaskDisplayArea();
            if (displayContent.mAtmService.isInSplitScreenWindowingMode()) {
                synchronized (displayContent.mAtmService.getGlobalLock()) {
                    topRootTaskInWindowingMode = displayContent.mAtmService.mRootWindowContainer.getDefaultTaskDisplayArea().getTopRootTaskInWindowingMode(1);
                }
                if (topRootTaskInWindowingMode == null || !(topRootTaskInWindowingMode instanceof Task)) {
                    return false;
                }
                Task splitScreenRootTask = topRootTaskInWindowingMode.asTask();
                Rect displayBounds = taskDisplayArea.getDisplayContent().getBounds();
                Rect splitScreenBound0 = splitScreenRootTask.getChildAt(0).getBounds();
                if (splitScreenBound0.width() == displayBounds.width()) {
                    float splitScreenMiddle0 = (splitScreenBound0.left + splitScreenBound0.right) / 2.0f;
                    if (eventX >= splitScreenMiddle0 - (splitScreenDragBarWidth / 2.0f) && eventX <= (splitScreenDragBarWidth / 2.0f) + splitScreenMiddle0) {
                        return true;
                    }
                } else {
                    float splitScreenMiddle02 = (splitScreenBound0.left + splitScreenBound0.right) / 2.0f;
                    if (eventX >= splitScreenMiddle02 - (splitScreenDragBarWidth / 2.0f) && eventX <= (splitScreenDragBarWidth / 2.0f) + splitScreenMiddle02) {
                        return true;
                    }
                    Rect splitScreenBound1 = splitScreenRootTask.getChildAt(1).getBounds();
                    float splitScreenMiddle1 = (splitScreenBound1.left + splitScreenBound1.right) / 2.0f;
                    if (eventX >= splitScreenMiddle1 - (splitScreenDragBarWidth / 2.0f) && eventX <= (splitScreenDragBarWidth / 2.0f) + splitScreenMiddle1) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
