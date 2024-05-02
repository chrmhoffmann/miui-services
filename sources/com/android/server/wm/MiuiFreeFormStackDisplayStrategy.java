package com.android.server.wm;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.ServiceManager;
import android.util.ArrayMap;
import android.util.MiuiMultiWindowAdapter;
import android.util.MiuiMultiWindowUtils;
import android.view.WindowManager;
import android.widget.Toast;
import com.android.server.UiThread;
import com.android.server.am.ProcessManagerService;
import com.android.server.am.ProcessRecord;
import com.android.server.am.SystemPressureController;
import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import miui.os.Build;
/* loaded from: classes.dex */
public class MiuiFreeFormStackDisplayStrategy {
    public static final int APP_TYPE = 2;
    private static final int CHECK_MEM_EXCEPTION_DELAY_TIME = 600000;
    public static final int GAME_TYPE = 1;
    private static final long MEM_EXCEPTION_THRESHOLDKB = 2621440;
    private static ArrayMap<Integer, Float> mMemoryMapOfAppType;
    private H mCheckExceptionHandler;
    private AlertDialog mDialog;
    private MiuiFreeFormManagerService mFreeFormManagerService;
    private final String TAG = "MiuiFreeFormStackDisplayStrategy";
    private int mDefaultMaxFreeformCount = 2;
    private final Handler mHandler = UiThread.getHandler();

    static {
        ArrayMap<Integer, Float> arrayMap = new ArrayMap<>();
        mMemoryMapOfAppType = arrayMap;
        arrayMap.put(1, Float.valueOf(1.0f));
        mMemoryMapOfAppType.put(2, Float.valueOf(0.7f));
    }

    public MiuiFreeFormStackDisplayStrategy(MiuiFreeFormManagerService service) {
        this.mFreeFormManagerService = service;
        HandlerThread handlerThread = new HandlerThread("checkMemExceptionFreeformApp");
        handlerThread.start();
        this.mCheckExceptionHandler = new H(handlerThread.getLooper());
    }

    private long getTotalMemory(Context context) {
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        ActivityManager am = (ActivityManager) context.getSystemService("activity");
        am.getMemoryInfo(memoryInfo);
        MiuiFreeFormManagerService.logd(true, "MiuiFreeFormStackDisplayStrategy", "totalMemory " + memoryInfo.totalMem);
        return memoryInfo.totalMem;
    }

    private long getAvailMemory(Context context) {
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        ActivityManager am = (ActivityManager) context.getSystemService("activity");
        am.getMemoryInfo(memoryInfo);
        MiuiFreeFormManagerService.logd(true, "MiuiFreeFormStackDisplayStrategy", "availMemory " + memoryInfo.availMem);
        return memoryInfo.availMem;
    }

    private boolean isSplitScreenMode() {
        return this.mFreeFormManagerService.mActivityTaskManagerService.isInSplitScreenWindowingMode();
    }

    private boolean isInEmbeddedWindowingMode(MiuiFreeFormActivityStack stack) {
        Object activityEmbedded;
        ActivityRecord topActivity = stack.mTask.getDisplayContent().getDefaultTaskDisplayArea().getTopActivity(false, true);
        if (topActivity == null || topActivity.getRootTaskId() == stack.mTask.getRootTaskId() || (activityEmbedded = MiuiMultiWindowUtils.invoke(topActivity, "isActivityEmbedded", new Object[]{false})) == null) {
            return false;
        }
        return ((Boolean) activityEmbedded).booleanValue();
    }

    public void onMiuiFreeFormStasckAdded(ConcurrentHashMap<Integer, MiuiFreeFormActivityStack> freeFormActivityStacks, MiuiFreeFormGestureController miuiFreeFormGestureController, MiuiFreeFormActivityStack addingStack) {
        int i;
        if (addingStack.getStackPackageName() == null || addingStack.mHasHadStackAdded) {
            return;
        }
        if (miuiFreeFormGestureController.isSwitchingApp()) {
            return;
        }
        addingStack.mHasHadStackAdded = true;
        boolean isAddingTopGame = MiuiMultiWindowAdapter.isInTopGameList(addingStack.getStackPackageName());
        StringBuilder log = new StringBuilder();
        log.append("onMiuiFreeFormStasckAdded  isAddingTopGame= ");
        log.append(isAddingTopGame);
        log.append(" addingStack= ");
        log.append(addingStack.getStackPackageName());
        MiuiFreeFormManagerService.logd(true, "MiuiFreeFormStackDisplayStrategy", log.toString());
        if (isAddingTopGame) {
            for (Integer taskId : freeFormActivityStacks.keySet()) {
                MiuiFreeFormActivityStack mffas = freeFormActivityStacks.get(taskId);
                if (taskId.intValue() != addingStack.mStackID && MiuiMultiWindowAdapter.isInTopGameList(mffas.getStackPackageName())) {
                    exitFreeForm(mffas, miuiFreeFormGestureController);
                    MiuiFreeFormManagerService.logd(true, "MiuiFreeFormStackDisplayStrategy", "onMiuiFreeFormStasckAdded Max TOP GAME FreeForm Window Num reached!");
                    return;
                }
            }
        }
        int maxStackCount = getMaxMiuiFreeFormStackCount(addingStack.getStackPackageName(), addingStack);
        int size = freeFormActivityStacks.size();
        log.delete(0, log.length());
        log.append("onMiuiFreeFormStasckAdded size = ");
        log.append(size);
        log.append(" getMaxMiuiFreeFormStackCount() = ");
        log.append(maxStackCount);
        MiuiFreeFormManagerService.logd(true, "MiuiFreeFormStackDisplayStrategy", log.toString());
        if (size <= maxStackCount) {
            i = 1;
        } else {
            MiuiFreeFormActivityStack mffas2 = this.mFreeFormManagerService.getBottomFreeFormActivityStack(size, addingStack);
            if (mffas2 == null) {
                i = 1;
            } else if (mffas2.mTask != null && mffas2.mTask.getResumedActivity() != null && mffas2.mTask.getResumedActivity().intent != null && mffas2.mTask.getResumedActivity().intent.getComponent() != null && addingStack.mTask != null && addingStack.mTask.intent != null && addingStack.mTask.intent.getComponent() != null && MiuiMultiWindowAdapter.HIDE_SELF_IF_NEW_FREEFORM_TASK_WHITE_LIST_ACTIVITY.contains(mffas2.mTask.getResumedActivity().intent.getComponent().getClassName()) && MiuiMultiWindowAdapter.SHOW_HIDDEN_TASK_IF_FINISHED_WHITE_LIST_ACTIVITY.contains(addingStack.mTask.intent.getComponent().getClassName())) {
                return;
            } else {
                if (miuiFreeFormGestureController.mTrackManager != null) {
                    if (mffas2.isInFreeFormMode()) {
                        miuiFreeFormGestureController.mTrackManager.trackSmallWindowPinedQuitEvent(mffas2.getStackPackageName(), mffas2.getApplicationName(), mffas2.mPinedStartTime != 0 ? ((float) (System.currentTimeMillis() - mffas2.mPinedStartTime)) / 1000.0f : MiuiFreeformPinManagerService.EDGE_AREA);
                    } else if (mffas2.isInMiniFreeFormMode()) {
                        miuiFreeFormGestureController.mTrackManager.trackMiniWindowPinedQuitEvent(mffas2.getStackPackageName(), mffas2.getApplicationName(), mffas2.mPinedStartTime != 0 ? ((float) (System.currentTimeMillis() - mffas2.mPinedStartTime)) / 1000.0f : MiuiFreeformPinManagerService.EDGE_AREA);
                    }
                }
                exitFreeForm(mffas2, miuiFreeFormGestureController);
                i = 1;
                MiuiFreeFormManagerService.logd(true, "MiuiFreeFormStackDisplayStrategy", "onMiuiFreeFormStasckAdded Max FreeForm Window Num reached!");
            }
        }
        if (size > i && maxStackCount > i && miuiFreeFormGestureController.mTrackManager != null) {
            miuiFreeFormGestureController.mTrackManager.trackMultiFreeformEnterEvent(size < maxStackCount ? size : maxStackCount);
        }
    }

    public void exitFreeForm(MiuiFreeFormActivityStack mffas, MiuiFreeFormGestureController miuiFreeFormGestureController) {
        if (mffas.isInMiniFreeFormMode()) {
            miuiFreeFormGestureController.mGestureListener.startExitSmallFreeformApplication(mffas);
        } else if (mffas.isInFreeFormMode()) {
            miuiFreeFormGestureController.mGestureListener.startExitApplication(mffas);
        }
    }

    public int getMaxMiuiFreeFormStackCount(String packageName, MiuiFreeFormActivityStack stack) {
        if (!MiuiMultiWindowUtils.multiFreeFormSupported(this.mFreeFormManagerService.mActivityTaskManagerService.mContext)) {
            return 1;
        }
        if (stack == null) {
            return 0;
        }
        int totalMemory = Build.TOTAL_RAM;
        if (totalMemory >= 7) {
            if (isInEmbeddedWindowingMode(stack) || isSplitScreenMode()) {
                return 2;
            }
            return this.mDefaultMaxFreeformCount;
        } else if (totalMemory < 5) {
            return totalMemory >= 3 ? 1 : 0;
        } else if (isInEmbeddedWindowingMode(stack) || isSplitScreenMode()) {
            return 1;
        } else {
            return this.mDefaultMaxFreeformCount;
        }
    }

    public boolean shouldStopStartFreeform(String packageName) {
        float availCapacity = byteToGB(getAvailMemory(this.mFreeFormManagerService.mActivityTaskManagerService.mContext));
        boolean isGame = MiuiMultiWindowAdapter.isInTopGameList(packageName);
        float reclaimThreshold = isGame ? 1.3f : 1.0f;
        if (availCapacity < reclaimThreshold) {
            SystemPressureController.getInstance().KillProcessForPadSmallWindowMode(packageName);
        }
        MiuiFreeFormManagerService.logd(true, "MiuiFreeFormStackDisplayStrategy", "getMaxMiuiFreeFormStackCount availCapacity = " + availCapacity + " isGame= " + isGame);
        if (this.mFreeFormManagerService.getCurrentUnReplaceFreeform(packageName).size() >= 2) {
            this.mHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreeFormStackDisplayStrategy.1
                @Override // java.lang.Runnable
                public void run() {
                    Toast toast = Toast.makeText(MiuiFreeFormStackDisplayStrategy.this.mFreeFormManagerService.mActivityTaskManagerService.mContext, MiuiFreeFormStackDisplayStrategy.this.mFreeFormManagerService.mActivityTaskManagerService.mContext.getString(286196645), 0);
                    toast.show();
                }
            });
            return true;
        }
        return false;
    }

    public int getMaxMiuiFreeFormStackCountForFlashBack(String packageName, boolean isLaunchFlashBack) {
        if (!isLaunchFlashBack) {
            return 0;
        }
        if (!MiuiMultiWindowUtils.multiFreeFormSupported(this.mFreeFormManagerService.mActivityTaskManagerService.mContext)) {
            return 1;
        }
        int totalMemory = Build.TOTAL_RAM;
        if (totalMemory >= 5) {
            return this.mDefaultMaxFreeformCount;
        }
        return totalMemory >= 3 ? 1 : 0;
    }

    public String getStackPackageName(Task targetTask) {
        if (targetTask == null) {
            return null;
        }
        if (targetTask.origActivity != null) {
            return targetTask.origActivity.getPackageName();
        }
        if (targetTask.realActivity != null) {
            return targetTask.realActivity.getPackageName();
        }
        if (targetTask.getTopActivity(false, true) == null) {
            return null;
        }
        return targetTask.getTopActivity(false, true).packageName;
    }

    private float byteToGB(long size) {
        long mb = 1024 * 1024;
        long gb = 1024 * mb;
        return (((float) size) * 1.0f) / ((float) gb);
    }

    public void startCheckMemExceptionFreeformApp() {
        this.mCheckExceptionHandler.sendEmptyMessageDelayed(1, 600000L);
    }

    public void stopCheckMemExceptionFreeformApp() {
        if (this.mCheckExceptionHandler.hasMessages(1)) {
            this.mCheckExceptionHandler.removeMessages(1);
        }
        dismiss();
    }

    /* loaded from: classes.dex */
    public class H extends Handler {
        public static final int MSG_CHECK_EXCEPTION_FREEFORM_APP = 1;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public H(Looper looper) {
            super(looper);
            MiuiFreeFormStackDisplayStrategy.this = r1;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    try {
                        List<MiuiFreeFormActivityStack> formActivityStacks = MiuiFreeFormStackDisplayStrategy.this.mFreeFormManagerService.getAllMiuiFreeFormActivityStack();
                        for (MiuiFreeFormActivityStack formActivityStack : formActivityStacks) {
                            MiuiFreeFormStackDisplayStrategy.this.checkExceptionFreeformApp(formActivityStack);
                        }
                        if (!formActivityStacks.isEmpty()) {
                            MiuiFreeFormStackDisplayStrategy.this.startCheckMemExceptionFreeformApp();
                            return;
                        }
                        return;
                    } catch (Exception e) {
                        MiuiFreeFormManagerService unused = MiuiFreeFormStackDisplayStrategy.this.mFreeFormManagerService;
                        MiuiFreeFormManagerService.logd(true, "MiuiFreeFormStackDisplayStrategy", "checkExceptionFreeformApp failed: " + e.toString());
                        return;
                    }
                default:
                    return;
            }
        }
    }

    public void checkExceptionFreeformApp(MiuiFreeFormActivityStack freeFormActivityStack) {
        Object profile;
        Object lastPss;
        ProcessManagerService processManagerService = (ProcessManagerService) ServiceManager.getService("ProcessManager");
        List<ProcessRecord> apps = processManagerService.getProcessRecordList(freeFormActivityStack.getStackPackageName(), freeFormActivityStack.mUserId);
        if (apps == null || apps.isEmpty()) {
            return;
        }
        long totalPss = 0;
        for (int i = 0; i < apps.size(); i++) {
            try {
                ProcessRecord app = apps.get(i);
                if (app != null && (profile = getFieldValue("mProfile", app)) != null && (lastPss = getFieldValue("mLastPss", profile)) != null) {
                    totalPss += ((Long) lastPss).longValue();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        MiuiFreeFormManagerService.logd(true, "MiuiFreeFormStackDisplayStrategy", "checkExceptionFreeformApp totalPss: " + totalPss + " packageName:" + freeFormActivityStack.getStackPackageName());
        if (totalPss > MEM_EXCEPTION_THRESHOLDKB && !isShowing() && !freeFormActivityStack.mMemoryExceptionHadBeenNegatived) {
            createCheckExceptionDialog(freeFormActivityStack);
            show();
        }
    }

    public static Object getFieldValue(String fieldName, Object instance) {
        try {
            Field field = instance.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(instance);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void createCheckExceptionDialog(final MiuiFreeFormActivityStack freeFormActivityStack) {
        if (freeFormActivityStack == null) {
            return;
        }
        AlertDialog.Builder b = new AlertDialog.Builder(this.mFreeFormManagerService.mActivityTaskManagerService.mContext);
        b.setCancelable(false);
        String message = this.mFreeFormManagerService.mActivityTaskManagerService.mContext.getResources().getString(286196536, freeFormActivityStack.getApplicationName());
        b.setMessage(message);
        b.setPositiveButton(286196538, new DialogInterface.OnClickListener() { // from class: com.android.server.wm.MiuiFreeFormStackDisplayStrategy.2
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialogInterface, int i) {
                MiuiFreeFormActivityStack miuiFreeFormActivityStack = freeFormActivityStack;
                if (miuiFreeFormActivityStack != null) {
                    MiuiFreeFormStackDisplayStrategy miuiFreeFormStackDisplayStrategy = MiuiFreeFormStackDisplayStrategy.this;
                    miuiFreeFormStackDisplayStrategy.exitFreeForm(miuiFreeFormActivityStack, (MiuiFreeFormGestureController) miuiFreeFormStackDisplayStrategy.mFreeFormManagerService.mActivityTaskManagerService.mWindowManager.mMiuiFreeFormGestureController);
                }
                MiuiFreeFormStackDisplayStrategy.this.dismiss();
            }
        });
        b.setNegativeButton(286196537, new DialogInterface.OnClickListener() { // from class: com.android.server.wm.MiuiFreeFormStackDisplayStrategy.3
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialogInterface, int i) {
                freeFormActivityStack.mMemoryExceptionHadBeenNegatived = true;
                MiuiFreeFormStackDisplayStrategy.this.dismiss();
            }
        });
        AlertDialog create = b.create();
        this.mDialog = create;
        if (create != null) {
            WindowManager.LayoutParams attrs = create.getWindow().getAttributes();
            attrs.type = 2003;
            this.mDialog.getWindow().setAttributes(attrs);
        }
    }

    private void show() {
        AlertDialog alertDialog = this.mDialog;
        if (alertDialog != null) {
            alertDialog.show();
            WindowManager.LayoutParams attrs = this.mDialog.getWindow().getAttributes();
            attrs.gravity = 17;
            attrs.width = 846;
            attrs.height = 480;
            this.mDialog.getWindow().setAttributes(attrs);
        }
    }

    private boolean isShowing() {
        AlertDialog alertDialog = this.mDialog;
        return alertDialog != null && alertDialog.isShowing();
    }

    public void dismiss() {
        AlertDialog alertDialog = this.mDialog;
        if (alertDialog != null) {
            alertDialog.dismiss();
            this.mDialog = null;
        }
    }

    public void showOpenMiuiOptimizationToast() {
        this.mHandler.post(new Runnable() { // from class: com.android.server.wm.MiuiFreeFormStackDisplayStrategy.4
            @Override // java.lang.Runnable
            public void run() {
                Toast toast = Toast.makeText(MiuiFreeFormStackDisplayStrategy.this.mFreeFormManagerService.mActivityTaskManagerService.mContext, MiuiFreeFormStackDisplayStrategy.this.mFreeFormManagerService.mActivityTaskManagerService.mContext.getString(286196386), 0);
                toast.show();
            }
        });
    }
}
