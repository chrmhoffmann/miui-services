package com.android.server.wm;

import android.os.SystemClock;
import android.util.Slog;
import android.view.WindowManager;
import com.miui.base.MiuiStubRegistry;
import com.miui.server.greeze.GreezeManagerService;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
/* loaded from: classes.dex */
public class TaskStubImpl implements TaskStub {
    private static final String TAG = "TaskStubImpl";
    private static final List<String> mNeedFinishAct;
    private boolean isHierarchyBottom = false;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<TaskStubImpl> {

        /* compiled from: TaskStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final TaskStubImpl INSTANCE = new TaskStubImpl();
        }

        public TaskStubImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public TaskStubImpl provideNewInstance() {
            return new TaskStubImpl();
        }
    }

    static {
        ArrayList arrayList = new ArrayList();
        mNeedFinishAct = arrayList;
        arrayList.add("com.miui.securitycenter/com.miui.permcenter.permissions.SystemAppPermissionDialogActivity");
        arrayList.add("com.xiaomi.account/.ui.SystemAccountAuthDialogActivity");
    }

    public static /* synthetic */ boolean lambda$isSplitScreenModeDismissed$0(Task root, Task task) {
        return task != root && task.hasChild();
    }

    public boolean isSplitScreenModeDismissed(final Task root) {
        return root != null && root.getTask(new Predicate() { // from class: com.android.server.wm.TaskStubImpl$$ExternalSyntheticLambda0
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return TaskStubImpl.lambda$isSplitScreenModeDismissed$0(root, (Task) obj);
            }
        }) == null;
    }

    public void onSplitScreenParentChanged(WindowContainer<?> oldParent, WindowContainer<?> newParent) {
        ActivityRecord topActivity;
        long start = SystemClock.uptimeMillis();
        boolean newInFullScreenMode = false;
        boolean oldInSpliScreenMode = (oldParent == null || oldParent.asTask() == null || !oldParent.inSplitScreenWindowingMode()) ? false : true;
        if (newParent != null && newParent.asTaskDisplayArea() != null && newParent.getWindowingMode() == 1) {
            newInFullScreenMode = true;
        }
        if (oldInSpliScreenMode && newInFullScreenMode && isSplitScreenModeDismissed(oldParent.asTask().getRootTask())) {
            Task task = newParent.asTaskDisplayArea().getTopRootTaskInWindowingMode(1);
            if (task == null || (topActivity = task.topRunningActivityLocked()) == null) {
                return;
            }
            ActivityTaskManagerServiceImpl.getInstance().onForegroundActivityChangedLocked(topActivity);
        }
        long took = SystemClock.uptimeMillis() - start;
        if (took > 50) {
            Slog.d(TAG, "onSplitScreenParentChanged took " + took + "ms");
        }
    }

    public boolean isHierarchyBottom() {
        return this.isHierarchyBottom;
    }

    public void setHierarchy(boolean bottom) {
        this.isHierarchyBottom = bottom;
    }

    public void updateForegroundActivityInAppPair(Task task, boolean isInAppPair) {
        Task topStack;
        ActivityRecord topActivity;
        if (!isInAppPair) {
            TaskDisplayArea displayArea = task.getDisplayArea();
            if (displayArea == null || (topStack = displayArea.getTopRootTaskInWindowingMode(1)) == null || (topActivity = topStack.topRunningActivityLocked()) == null) {
                return;
            }
            ActivityTaskManagerServiceImpl.getInstance().onForegroundActivityChangedLocked(topActivity);
            return;
        }
        ActivityRecord topActivity2 = task.topRunningActivityLocked();
        if (topActivity2 == null) {
            return;
        }
        ActivityTaskManagerServiceImpl.getInstance().onForegroundActivityChangedLocked(topActivity2);
    }

    public void notifyMovetoFront(int uid) {
        GreezeManagerService.getService().notifyMovetoFront(uid);
    }

    public int getLayoutInDisplayCutoutMode(WindowManager.LayoutParams attrs) {
        if (attrs == null) {
            return 0;
        }
        if (attrs.miuiAlwaysDisplayInCutout) {
            return 3;
        }
        return attrs.layoutInDisplayCutoutMode;
    }

    public void clearRootProcess(WindowProcessController app, Task task) {
        ActivityRecord top;
        if (app == null || task == null || !app.isRemoved() || task.getNonFinishingActivityCount() != 1) {
            return;
        }
        if ((task.affinity == null || task.affinity.contains("com.xiaomi.vipaccount")) && (top = task.getTopNonFinishingActivity()) != null && top.app != app && mNeedFinishAct.contains(top.shortComponentName)) {
            Slog.d(TAG, "Only left " + top.shortComponentName + " in task " + task.mTaskId + " and finish it.");
            top.finishIfPossible("clearNonAppAct", false);
        }
    }
}
