package com.android.server.wm;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.provider.Settings;
import android.util.MiuiMultiWindowAdapter;
import android.util.MiuiMultiWindowUtils;
import android.util.Slog;
import android.window.WindowContainerTransaction;
import com.android.server.wm.MiuiCvwGestureController;
import com.android.server.wm.MiuiFreeFormActivityStack;
import com.android.server.wm.MiuiFreeformTrackManager;
import com.miui.base.MiuiStubRegistry;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import miui.app.IFreeformCallback;
import miui.app.IMiuiFreeFormManager;
import miui.app.MiuiFreeFormManager;
/* loaded from: classes.dex */
public class MiuiFreeFormManagerService extends IMiuiFreeFormManager.Stub implements MiuiFreeFormManagerServiceStub {
    private static final int DEFAULT_MAX_FREEFORM_COUNT = 2;
    private static final String TAG = "MiuiFreeFormManagerService";
    public static int mHomeUid = -1;
    ActivityTaskManagerService mActivityTaskManagerService;
    MiuiFreeFormCameraStrategy mFreeFormCameraStrategy;
    MiuiFreeFormStackDisplayStrategy mFreeFormStackDisplayStrategy;
    Handler mHandler;
    private SensorManager mSensorManager;
    final ConcurrentHashMap<Integer, MiuiFreeFormActivityStack> mFreeFormActivityStacks = new ConcurrentHashMap<>();
    private final RemoteCallbackList<IFreeformCallback> mCallbacks = new RemoteCallbackList<>();
    private final ConcurrentHashMap<Integer, MiuiFreeFormActivityStack> mFreeFormSurpportActivityStacks = new ConcurrentHashMap<>();

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MiuiFreeFormManagerService> {

        /* compiled from: MiuiFreeFormManagerService$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MiuiFreeFormManagerService INSTANCE = new MiuiFreeFormManagerService();
        }

        public MiuiFreeFormManagerService provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public MiuiFreeFormManagerService provideNewInstance() {
            return new MiuiFreeFormManagerService();
        }
    }

    public void init(ActivityTaskManagerService ams) {
        this.mActivityTaskManagerService = ams;
        this.mFreeFormStackDisplayStrategy = new MiuiFreeFormStackDisplayStrategy(this);
        this.mFreeFormCameraStrategy = new MiuiFreeFormCameraStrategy(this);
    }

    public void init(Handler handler) {
        this.mHandler = handler;
        this.mSensorManager = (SensorManager) this.mActivityTaskManagerService.mContext.getSystemService("sensor");
    }

    public void notifyHasDrawn(WindowState windowState) {
        MiuiFreeFormActivityStack mffas;
        if (windowState == null || windowState.getRootTask() == null || (mffas = getMiuiFreeFormActivityStackForMiuiFB(windowState.getRootTask().mTaskId)) == null || !mffas.inPinMode()) {
            return;
        }
        MiuiFreeFormGestureController miuiFreeFormGestureController = (MiuiFreeFormGestureController) this.mActivityTaskManagerService.mWindowManager.mMiuiFreeFormGestureController;
        miuiFreeFormGestureController.mMiuiFreeformPinManagerService.notifyHasDrawn(mffas, windowState);
    }

    public void updateMiuiFreeformShadow(Task task) {
        MiuiFreeFormGestureController miuiFreeFormGestureController = (MiuiFreeFormGestureController) this.mActivityTaskManagerService.mWindowManager.mMiuiFreeFormGestureController;
        miuiFreeFormGestureController.mMiuiFreeFormShadowHelper.updateMiuiFreeformShadow(task);
    }

    public boolean shouldAddingToTask(String shortComponentName) {
        return MiuiMultiWindowAdapter.getLaunchInTaskList().contains(shortComponentName);
    }

    public boolean isSupportPin() {
        return true;
    }

    public int getCurrentMiuiFreeFormNum() {
        return this.mFreeFormActivityStacks.size();
    }

    public void onActivityStackFirstActivityRecordAdded(int rootId) {
        MiuiFreeFormActivityStack mffas = getMiuiFreeFormActivityStackForMiuiFB(rootId);
        if (mffas == null) {
            return;
        }
        if (mffas.shouldDelayDispatchFreeFormStackModeChanged) {
            int i = 0;
            mffas.shouldDelayDispatchFreeFormStackModeChanged = false;
            if (!mffas.isInFreeFormMode()) {
                i = 1;
            }
            dispatchFreeFormStackModeChanged(i, mffas);
        }
        this.mFreeFormStackDisplayStrategy.onMiuiFreeFormStasckAdded(this.mFreeFormActivityStacks, (MiuiFreeFormGestureController) this.mActivityTaskManagerService.mWindowManager.mMiuiFreeFormGestureController, mffas);
    }

    public void addFreeFormActivityStack(Task rootTask) {
        addFreeFormActivityStack(rootTask, -1);
    }

    public void addFreeFormActivityStack(Task rootTask, int miuiFreeFromWindowMode) {
        if (rootTask.isActivityTypeHome() || this.mFreeFormActivityStacks.containsKey(Integer.valueOf(rootTask.mTaskId))) {
            return;
        }
        int i = 0;
        if (this.mFreeFormActivityStacks.isEmpty()) {
            MiuiFreeFormGestureController miuiFreeFormGestureController = (MiuiFreeFormGestureController) this.mActivityTaskManagerService.mWindowManager.mMiuiFreeFormGestureController;
            miuiFreeFormGestureController.startFirstFreeformTask();
            this.mFreeFormStackDisplayStrategy.startCheckMemExceptionFreeformApp();
            Settings.Secure.putIntForUser(this.mActivityTaskManagerService.mContext.getContentResolver(), "freeform_window_state", 0, -2);
        }
        logd(TAG, "addFreeFormActivityStack as = " + rootTask);
        MiuiFreeFormActivityStack mffas = new MiuiFreeFormActivityStack(rootTask, miuiFreeFromWindowMode);
        this.mFreeFormActivityStacks.put(Integer.valueOf(rootTask.mTaskId), mffas);
        onMiuiFreeFormStasckAdded(mffas);
        if (mffas.getStackPackageName() != null) {
            if (!mffas.isInFreeFormMode()) {
                i = 1;
            }
            dispatchFreeFormStackModeChanged(i, mffas);
            return;
        }
        mffas.shouldDelayDispatchFreeFormStackModeChanged = true;
    }

    public void updateCornerRadius(Task rootTask) {
        if (rootTask == null || !this.mFreeFormActivityStacks.containsKey(Integer.valueOf(rootTask.getRootTaskId()))) {
            return;
        }
        logd(TAG, "updateCornerRadius rootTask = " + rootTask);
        MiuiFreeFormActivityStack stack = getMiuiFreeFormActivityStackForMiuiFB(rootTask.getRootTaskId());
        stack.updateCornerRadius();
    }

    public void onStartActivity(Task as, ActivityOptions options) {
        Method method;
        if (as == null || !as.isActivityTypeHome()) {
            logd(TAG, "onStartActivityInner as = " + as + " op = " + options);
            if (options != null && options.getLaunchWindowingMode() == 5 && as != null && as.mAtmService.mSupportsFreeformWindowManagement && (method = MiuiMultiWindowUtils.isMethodExist(options, "getActivityOptionsInjector", new Object[0])) != null) {
                try {
                    boolean needAnimation = ((Boolean) MiuiMultiWindowUtils.invoke(method.invoke(options, new Object[0]), "getFreeformAnimation", new Object[0])).booleanValue();
                    boolean useCustomLaunchBounds = ((Boolean) MiuiMultiWindowUtils.invoke(method.invoke(options, new Object[0]), "getUseCustomLaunchBounds", new Object[0])).booleanValue();
                    if (useCustomLaunchBounds && as.mFreeFormShouldBeAvoided) {
                        as.mFreeFormShouldBeAvoided = false;
                    }
                    as.mFreeFormLaunchBoundsFromOptions = options.getLaunchBounds();
                    addFreeFormActivityStack(as.inMultiWindowMode() ? as : as.getRootTask());
                    MiuiFreeFormActivityStack mffas = getMiuiFreeFormActivityStackForMiuiFB(as.inMultiWindowMode() ? as.mTaskId : as.getRootTaskId());
                    mffas.mIsLandcapeFreeform = options.getLaunchBounds().width() > options.getLaunchBounds().height();
                    mffas.mNeedAnimation = needAnimation;
                    float scaleInOptions = ((Float) MiuiMultiWindowUtils.invoke(method.invoke(options, new Object[0]), "getFreeformScale", new Object[0])).floatValue();
                    mffas.mFreeFormScale = scaleInOptions != -1.0f ? scaleInOptions : MiuiMultiWindowUtils.getOriFreeformScale(this.mActivityTaskManagerService.mContext, mffas.mIsLandcapeFreeform);
                    mffas.mStackRatio = mffas.mIsLandcapeFreeform ? MiuiFreeformTrackManager.SmallWindowTrackConstants.STACK_RATIO2 : MiuiFreeformTrackManager.SmallWindowTrackConstants.STACK_RATIO1;
                    logd(TAG, "onStartActivityInner as = " + as + " op = " + options);
                } catch (Exception e) {
                }
            }
        }
    }

    private void onMiuiFreeFormStasckAdded(MiuiFreeFormActivityStack stack) {
        this.mFreeFormStackDisplayStrategy.onMiuiFreeFormStasckAdded(this.mFreeFormActivityStacks, (MiuiFreeFormGestureController) this.mActivityTaskManagerService.mWindowManager.mMiuiFreeFormGestureController, stack);
        this.mActivityTaskManagerService.getTaskChangeNotificationController().notifyTaskStackChanged();
        stack.onMiuiFreeFormStasckAdded();
        setCameraRotationIfNeeded();
    }

    public void setCameraRotationIfNeeded() {
        this.mFreeFormCameraStrategy.rotateCameraIfNeeded();
    }

    public void removeFreeFormActivityStack(Task task, boolean stackRemoved) {
        if (task != null && this.mFreeFormActivityStacks.containsKey(Integer.valueOf(task.getRootTaskId()))) {
            MiuiFreeFormActivityStack stack = getMiuiFreeFormActivityStackForMiuiFB(task.getRootTaskId());
            logd(TAG, "removeFreeFormActivityStack as = " + stack);
            if (stack == null) {
                return;
            }
            if (stack.inPinMode()) {
                MiuiFreeFormGestureController miuiFreeFormGestureController = (MiuiFreeFormGestureController) this.mActivityTaskManagerService.mWindowManager.mMiuiFreeFormGestureController;
                if (miuiFreeFormGestureController.mTrackManager != null) {
                    boolean isInFreeFormMode = stack.isInFreeFormMode();
                    float f = MiuiFreeformPinManagerService.EDGE_AREA;
                    if (isInFreeFormMode) {
                        MiuiFreeformTrackManager miuiFreeformTrackManager = miuiFreeFormGestureController.mTrackManager;
                        String stackPackageName = stack.getStackPackageName();
                        String applicationName = stack.getApplicationName();
                        if (stack.mPinedStartTime != 0) {
                            f = ((float) (System.currentTimeMillis() - stack.mPinedStartTime)) / 1000.0f;
                        }
                        miuiFreeformTrackManager.trackSmallWindowPinedQuitEvent(stackPackageName, applicationName, f);
                    } else if (stack.isInMiniFreeFormMode()) {
                        MiuiFreeformTrackManager miuiFreeformTrackManager2 = miuiFreeFormGestureController.mTrackManager;
                        String stackPackageName2 = stack.getStackPackageName();
                        String applicationName2 = stack.getApplicationName();
                        if (stack.mPinedStartTime != 0) {
                            f = ((float) (System.currentTimeMillis() - stack.mPinedStartTime)) / 1000.0f;
                        }
                        miuiFreeformTrackManager2.trackMiniWindowPinedQuitEvent(stackPackageName2, applicationName2, f);
                    }
                }
                stack.setInPinMode(false);
                stack.mPinedStartTime = 0L;
            }
            stack.mTask.mFreeFormLaunchBoundsFromOptions = null;
            stack.mTask.mFreeFormShouldBeAvoided = true;
            if (stackRemoved) {
                MiuiFreeFormGestureController miuiFreeFormGestureController2 = (MiuiFreeFormGestureController) this.mActivityTaskManagerService.mWindowManager.mMiuiFreeFormGestureController;
                miuiFreeFormGestureController2.mGestureListener.mGestureAnimator.hideStack(stack);
                miuiFreeFormGestureController2.mGestureListener.mGestureAnimator.applyTransaction();
                miuiFreeFormGestureController2.mGestureListener.mGestureAnimator.removeAnimationControlLeash(stack);
            }
            this.mFreeFormActivityStacks.remove(Integer.valueOf(task.getRootTaskId()));
            if (stack.isInFreeFormMode()) {
                stack.setStackFreeFormMode(-1);
                dispatchFreeFormStackModeChanged(3, stack);
            } else if (stack.isInMiniFreeFormMode()) {
                stack.setStackFreeFormMode(-1);
                dispatchFreeFormStackModeChanged(5, stack);
            }
            task.setAlwaysOnTop(false);
            onMiuiFreeFormStasckremove(stack);
            if (this.mFreeFormActivityStacks.isEmpty()) {
                ((MiuiFreeFormGestureController) this.mActivityTaskManagerService.mWindowManager.mMiuiFreeFormGestureController).closeLastFreeformTask();
                this.mFreeFormStackDisplayStrategy.stopCheckMemExceptionFreeformApp();
                Settings.Secure.putIntForUser(this.mActivityTaskManagerService.mContext.getContentResolver(), "freeform_window_state", -1, -2);
            }
            setCameraRotationIfNeeded();
            this.mFreeFormStackDisplayStrategy.dismiss();
        }
    }

    private void onMiuiFreeFormStasckremove(MiuiFreeFormActivityStack stack) {
        stack.onMiuiFreeFormStasckremove();
    }

    public void removeSensorDisableApp(String packageName) {
        if (!MiuiMultiWindowAdapter.SENSOR_DISABLE_WHITE_LIST.contains(packageName)) {
            MiuiMultiWindowUtils.invoke(this.mSensorManager, "removeSensorDisableApp", new Object[]{packageName});
            Slog.d(TAG, "removeSensorDisableApp: " + packageName);
        }
    }

    public void setSensorDisableApp(String packageName) {
        if (!MiuiMultiWindowAdapter.SENSOR_DISABLE_WHITE_LIST.contains(packageName)) {
            MiuiMultiWindowUtils.invoke(this.mSensorManager, "setSensorDisableApp", new Object[]{packageName});
            Slog.d(TAG, "setSensorDisableApp: " + packageName);
        }
    }

    public void onActivityStackWindowModeSet(Task rootTask, int mode) {
        if (rootTask.isActivityTypeHome()) {
            if (!this.mFreeFormSurpportActivityStacks.containsKey(Integer.valueOf(rootTask.getRootTaskId()))) {
                this.mFreeFormSurpportActivityStacks.forEach(new BiConsumer() { // from class: com.android.server.wm.MiuiFreeFormManagerService$$ExternalSyntheticLambda7
                    @Override // java.util.function.BiConsumer
                    public final void accept(Object obj, Object obj2) {
                        MiuiFreeFormManagerService.this.m1681xbf928845((Integer) obj, (MiuiFreeFormActivityStack) obj2);
                    }
                });
            }
            this.mFreeFormSurpportActivityStacks.put(Integer.valueOf(rootTask.getRootTaskId()), new MiuiFreeFormActivityStack(rootTask));
        } else if (mode == 5) {
            addFreeFormActivityStack(rootTask);
        } else {
            removeFreeFormActivityStack(rootTask, false);
        }
    }

    /* renamed from: lambda$onActivityStackWindowModeSet$0$com-android-server-wm-MiuiFreeFormManagerService */
    public /* synthetic */ void m1681xbf928845(Integer taskId, MiuiFreeFormActivityStack mffas) {
        if (mffas.mTask.isActivityTypeHome()) {
            this.mFreeFormSurpportActivityStacks.remove(taskId);
        }
    }

    public String getRootTaskPackageName(Task task) {
        ActivityRecord homeActivity = task.getActivity(new Predicate() { // from class: com.android.server.wm.MiuiFreeFormManagerService$$ExternalSyntheticLambda3
            @Override // java.util.function.Predicate
            public final boolean test(Object obj) {
                return MiuiFreeFormManagerService.lambda$getRootTaskPackageName$1((ActivityRecord) obj);
            }
        });
        return homeActivity.packageName;
    }

    public static /* synthetic */ boolean lambda$getRootTaskPackageName$1(ActivityRecord r) {
        return true;
    }

    public void onActivityStackConfigurationChanged(Task task, int prevWindowingMode, int overrideWindowingMode) {
        if (task.isActivityTypeHome()) {
            if (!this.mFreeFormSurpportActivityStacks.containsKey(Integer.valueOf(task.getRootTaskId()))) {
                this.mFreeFormSurpportActivityStacks.forEach(new BiConsumer() { // from class: com.android.server.wm.MiuiFreeFormManagerService$$ExternalSyntheticLambda6
                    @Override // java.util.function.BiConsumer
                    public final void accept(Object obj, Object obj2) {
                        MiuiFreeFormManagerService.this.m1680x436d119a((Integer) obj, (MiuiFreeFormActivityStack) obj2);
                    }
                });
                this.mFreeFormSurpportActivityStacks.put(Integer.valueOf(task.getRootTaskId()), new MiuiFreeFormActivityStack(task));
            }
            try {
                if (-1 == mHomeUid) {
                    mHomeUid = this.mActivityTaskManagerService.mContext.getPackageManager().getPackageUid(getStackPackageName(task), 0);
                }
            } catch (Exception e) {
            }
        } else if (task.inFreeformSmallWinMode()) {
            addFreeFormActivityStack(task);
            MiuiFreeFormActivityStack mffas = getMiuiFreeFormActivityStackForMiuiFB(task.getRootTaskId());
            if (mffas != null) {
                mffas.onConfigurationChanged();
            }
        } else if ((prevWindowingMode == 3 || prevWindowingMode == 4 || prevWindowingMode == 6) && (overrideWindowingMode == 0 || overrideWindowingMode == 1 || overrideWindowingMode == 6)) {
        } else {
            removeFreeFormActivityStack(task, false);
        }
    }

    /* renamed from: lambda$onActivityStackConfigurationChanged$2$com-android-server-wm-MiuiFreeFormManagerService */
    public /* synthetic */ void m1680x436d119a(Integer taskId, MiuiFreeFormActivityStack mffas) {
        if (mffas.mTask.isActivityTypeHome()) {
            this.mFreeFormSurpportActivityStacks.remove(taskId);
        }
    }

    public void addFreeFormActivityStackFromStartSmallFreeform(Task task) {
        task.mLaunchMiniFreeformFromFull = true;
        task.mFreeFormShouldBeAvoided = false;
        addFreeFormActivityStack(task, 1);
        MiuiFreeFormActivityStack mffas = getMiuiFreeFormActivityStackForMiuiFB(task.getRootTaskId());
        if (mffas != null) {
            MiuiFreeFormGestureController miuiFreeFormGestureController = (MiuiFreeFormGestureController) this.mActivityTaskManagerService.mWindowManager.mMiuiFreeFormGestureController;
            miuiFreeFormGestureController.showScreenSurface(mffas);
        }
    }

    public void launchFreeformByCVW(Task task, MiuiCvwGestureHandlerImpl miuiCvwGestureHandler, MiuiCvwAnimator animator, Rect taskBounds, Rect miniFreeformBounds, float freeFormScale, MiuiCvwGestureController.CvwTrackEvent trackEvent) {
        String str;
        String str2;
        addFreeFormActivityStack(task);
        MiuiFreeFormActivityStack mffas = getMiuiFreeFormActivityStackForMiuiFB(task.getRootTaskId());
        if (mffas == null) {
            return;
        }
        if (mffas.mCVWControlInfo == null) {
            mffas.mCVWControlInfo = new MiuiFreeFormActivityStack.CVWControlInfo();
        }
        if (!mffas.isInMiniFreeFormMode()) {
            mffas.setStackFreeFormMode(0);
            MiuiFreeFormGestureController miuiFreeFormGestureController = (MiuiFreeFormGestureController) this.mActivityTaskManagerService.mWindowManager.mMiuiFreeFormGestureController;
            miuiFreeFormGestureController.mTrackManager.trackSmallWindowEnterWayEvent(trackEvent.enterWay, trackEvent.targetStackRatio, mffas.getStackPackageName(), mffas.getApplicationName(), this.mFreeFormActivityStacks.size());
        }
        mffas.mStackRatio = trackEvent.targetStackRatio;
        mffas.mFreeFormScale = freeFormScale;
        mffas.mCVWControlInfo.freeFormScale = freeFormScale;
        mffas.mCVWControlInfo.needRemoveCvwCoverLayer = true;
        mffas.mCVWControlInfo.taskBounds = taskBounds;
        mffas.mCVWControlInfo.isSwitchedToFreeform = false;
        mffas.mCVWControlInfo.miniFreeformBounds.set(miniFreeformBounds);
        mffas.mCVWControlInfo.fullConfiguration = new Configuration(task.getConfiguration());
        MiuiFreeFormGestureController miuiFreeFormGestureController2 = (MiuiFreeFormGestureController) this.mActivityTaskManagerService.mWindowManager.mMiuiFreeFormGestureController;
        miuiFreeFormGestureController2.mGestureListener.showScreenSurface(mffas);
        logd(TAG, "launchFreeformByCVW taskBounds=" + taskBounds + " freeFormScale=" + freeFormScale);
        synchronized (this.mActivityTaskManagerService.getGlobalLock()) {
            this.mActivityTaskManagerService.mTaskSupervisor.beginActivityVisibilityUpdate();
            this.mActivityTaskManagerService.mTaskSupervisor.beginDeferResume();
            WindowContainerTransaction wct = new WindowContainerTransaction();
            try {
                wct.setWindowingMode(task.mRemoteToken.toWindowContainerToken(), 5);
                this.mActivityTaskManagerService.mWindowOrganizerController.applyTransaction(wct);
                wct.setBounds(task.mRemoteToken.toWindowContainerToken(), taskBounds);
                this.mActivityTaskManagerService.mWindowOrganizerController.applyTransaction(wct);
                this.mActivityTaskManagerService.mTaskSupervisor.endActivityVisibilityUpdate();
                this.mActivityTaskManagerService.mTaskSupervisor.endDeferResume();
                task.getDisplayContent().ensureActivitiesVisible((ActivityRecord) null, 0, true, true);
                launchFreeformByCVWCompleted(task, animator, miuiCvwGestureHandler);
                str2 = TAG;
                str = "launchFreeformByCVW end ";
            } catch (Exception e) {
                Slog.e(TAG, "launchFreeformByCVW error", e);
                this.mActivityTaskManagerService.mTaskSupervisor.endActivityVisibilityUpdate();
                this.mActivityTaskManagerService.mTaskSupervisor.endDeferResume();
                task.getDisplayContent().ensureActivitiesVisible((ActivityRecord) null, 0, true, true);
                launchFreeformByCVWCompleted(task, animator, miuiCvwGestureHandler);
                str2 = TAG;
                str = "launchFreeformByCVW end ";
            }
            logd(str2, str);
        }
    }

    public void launchSmallFreeformByCVW(MiuiFreeFormActivityStack stack, MiuiCvwGestureHandlerImpl miuiCvwGestureHandler, MiuiCvwAnimator animator, Rect taskBounds, Rect miniFreeformBounds, float freeFormScale, MiuiCvwGestureController.CvwTrackEvent trackEvent) {
        addFreeFormActivityStack(stack.mTask);
        MiuiFreeFormActivityStack mffas = getMiuiFreeFormActivityStackForMiuiFB(stack.mTask.getRootTaskId());
        if (mffas == null) {
            return;
        }
        mffas.mTaskAnimationAdapter = stack.mTaskAnimationAdapter;
        mffas.setStackFreeFormMode(1);
        mffas.mTask.mFreeFormShouldBeAvoided = false;
        mffas.mStackRatio = trackEvent.targetStackRatio;
        if (mffas.mCVWControlInfo == null) {
            mffas.mCVWControlInfo = new MiuiFreeFormActivityStack.CVWControlInfo();
        }
        launchFreeformByCVW(stack.mTask, miuiCvwGestureHandler, animator, taskBounds, miniFreeformBounds, freeFormScale, trackEvent);
        MiuiFreeFormGestureController miuiFreeFormGestureController = (MiuiFreeFormGestureController) this.mActivityTaskManagerService.mWindowManager.mMiuiFreeFormGestureController;
        miuiFreeFormGestureController.mTrackManager.trackMiniWindowEnterWayEvent(trackEvent.enterWay, new Point(miniFreeformBounds.left, miniFreeformBounds.top), mffas.getStackPackageName(), mffas.getApplicationName(), this.mFreeFormActivityStacks.size());
    }

    private void launchFreeformByCVWCompleted(Task task, MiuiCvwAnimator animator, MiuiCvwGestureHandlerImpl miuiCvwGestureHandler) {
        MiuiFreeFormActivityStack mffas;
        if (task.inFreeformSmallWinMode() && (mffas = getMiuiFreeFormActivityStackForMiuiFB(task.getRootTaskId())) != null && mffas.isLaunchedByCVW() && !mffas.mCVWControlInfo.isSwitchedToFreeform && !mffas.mCVWControlInfo.fullConfiguration.windowConfiguration.getBounds().equals(task.getConfiguration().windowConfiguration.getBounds())) {
            if (mffas.isInFreeFormMode()) {
                if (mffas.mCVWControlInfo.needRemoveCvwCoverLayer) {
                    MiuiCvwGestureController miuiCvwGestureController = (MiuiCvwGestureController) this.mActivityTaskManagerService.mWindowManager.mMiuiCvwGestureController;
                    miuiCvwGestureController.resetGestureAnimatorState();
                }
                logd(TAG, "launchFreeformByCVWCompleted mffas=" + task);
            } else {
                if (mffas.mCVWControlInfo.needRemoveCvwCoverLayer) {
                    MiuiCvwGestureController miuiCvwGestureController2 = (MiuiCvwGestureController) this.mActivityTaskManagerService.mWindowManager.mMiuiCvwGestureController;
                    miuiCvwGestureController2.resetGestureAnimatorState();
                }
                MiuiFreeFormGestureController miuiFreeFormGestureController = (MiuiFreeFormGestureController) this.mActivityTaskManagerService.mWindowManager.mMiuiFreeFormGestureController;
                miuiFreeFormGestureController.launchSmallFreeformByCVW(mffas, animator, miuiCvwGestureHandler, mffas.mCVWControlInfo.taskBounds, mffas.mCVWControlInfo.miniFreeformBounds, mffas.mCVWControlInfo.freeFormScale, false);
                logd(TAG, "launchSmallFreeformByCVWCompleted mffas=" + mffas);
            }
            mffas.mCVWControlInfo.fullConfiguration = task.getConfiguration();
            mffas.mCVWControlInfo.needRemoveCvwCoverLayer = false;
            mffas.mCVWControlInfo.isSwitchedToFreeform = true;
            mffas.updateCornerRadius();
            MiuiFreeFormGestureController miuiFreeFormGestureController2 = (MiuiFreeFormGestureController) this.mActivityTaskManagerService.mWindowManager.mMiuiFreeFormGestureController;
            miuiFreeFormGestureController2.mGestureListener.hideScreenSurface(mffas);
        }
    }

    public void launchSmallFreeformByFreeform(Task task, MiuiCvwAnimator animator, MiuiCvwGestureHandlerImpl miuiCvwGestureHandler, Rect taskBounds, Rect miniFreeformBounds, float freeFormScale, MiuiCvwGestureController.CvwTrackEvent trackEvent) {
        MiuiFreeFormActivityStack mffas = getMiuiFreeFormActivityStackForMiuiFB(task.getRootTaskId());
        if (mffas == null) {
            MiuiCvwGestureController miuiCvwGestureController = (MiuiCvwGestureController) this.mActivityTaskManagerService.mWindowManager.mMiuiCvwGestureController;
            miuiCvwGestureController.resetGestureAnimatorState();
            return;
        }
        if (mffas.mCVWControlInfo == null) {
            mffas.mCVWControlInfo = new MiuiFreeFormActivityStack.CVWControlInfo();
        }
        mffas.setStackFreeFormMode(1);
        mffas.mFreeFormScale = freeFormScale;
        mffas.mStackRatio = trackEvent.targetStackRatio;
        mffas.mCVWControlInfo.freeFormScale = freeFormScale;
        mffas.mCVWControlInfo.taskBounds = taskBounds;
        mffas.mCVWControlInfo.miniFreeformBounds.set(miniFreeformBounds);
        mffas.mCVWControlInfo.fullConfiguration = new Configuration(task.getConfiguration());
        MiuiFreeFormGestureController miuiFreeFormGestureController = (MiuiFreeFormGestureController) this.mActivityTaskManagerService.mWindowManager.mMiuiFreeFormGestureController;
        miuiFreeFormGestureController.mGestureListener.showScreenSurface(mffas);
        MiuiCvwGestureController miuiCvwGestureController2 = (MiuiCvwGestureController) this.mActivityTaskManagerService.mWindowManager.mMiuiCvwGestureController;
        miuiCvwGestureController2.resetGestureAnimatorState();
        miuiFreeFormGestureController.launchSmallFreeformByCVW(mffas, animator, miuiCvwGestureHandler, mffas.mCVWControlInfo.taskBounds, mffas.mCVWControlInfo.miniFreeformBounds, mffas.mCVWControlInfo.freeFormScale, true);
        mffas.updateCornerRadius();
        logd(TAG, "launchSmallFreeformByFreeformCompleted mffas=" + mffas);
        miuiFreeFormGestureController.mGestureListener.hideScreenSurface(mffas);
        miuiFreeFormGestureController.mTrackManager.trackMiniWindowEnterWayEvent(trackEvent.enterWay, new Point(miniFreeformBounds.left, miniFreeformBounds.top), mffas.getStackPackageName(), mffas.getApplicationName(), this.mFreeFormActivityStacks.size());
    }

    /* JADX WARN: Removed duplicated region for block: B:39:0x008a A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public void launchFreeformBySmallFreeform(com.android.server.wm.MiuiFreeFormActivityStack r18, android.graphics.Rect r19, float r20, com.android.server.wm.MiuiCvwGestureController.CvwTrackEvent r21) {
        /*
            r17 = this;
            r1 = r17
            r2 = r18
            r3 = r21
            java.lang.String r0 = "MiuiFreeFormManagerService"
            java.lang.String r4 = "launchFreeformBySmallFreeform"
            android.util.Slog.d(r0, r4)
            r4 = 0
            r2.setIsFlashBackMode(r4)
            r2.setStackFreeFormMode(r4)
            com.android.server.wm.ActivityTaskManagerService r0 = r1.mActivityTaskManagerService
            com.android.server.wm.WindowManagerService r0 = r0.mWindowManager
            com.android.server.wm.MiuiFreeformGestureControllerStub r0 = r0.mMiuiFreeFormGestureController
            r5 = r0
            com.android.server.wm.MiuiFreeFormGestureController r5 = (com.android.server.wm.MiuiFreeFormGestureController) r5
            r0 = 4
            r5.notifyStartSmallFreeFormToFreeformWindow(r0, r2)
            android.view.SurfaceControl$Transaction r0 = new android.view.SurfaceControl$Transaction
            r0.<init>()
            r6 = r0
            com.android.server.wm.ActivityTaskManagerService r0 = r1.mActivityTaskManagerService
            com.android.server.wm.WindowManagerService r0 = r0.mWindowManager
            com.android.server.wm.WindowManagerGlobalLock r7 = r0.mGlobalLock
            monitor-enter(r7)
            com.android.server.wm.DisplayContent r0 = r5.mDisplayContent     // Catch: java.lang.Throwable -> Lb9
            com.android.server.wm.InputMonitor r0 = r0.getInputMonitor()     // Catch: java.lang.Throwable -> Lb9
            r0.updateInputWindowsImmediately(r6)     // Catch: java.lang.Throwable -> Lb9
            monitor-exit(r7)     // Catch: java.lang.Throwable -> Lb9
            r6.apply()
            com.android.server.wm.ActivityTaskManagerService r0 = r1.mActivityTaskManagerService
            com.android.server.wm.WindowManagerService r0 = r0.mWindowManager
            r0.openSurfaceTransaction()
            r8 = r20
            r2.mFreeFormScale = r8     // Catch: java.lang.Throwable -> L62 java.lang.Exception -> L66
            com.android.server.wm.MiuiFreeFormGesturePointerEventListener r0 = r5.mGestureListener     // Catch: java.lang.Throwable -> L62 java.lang.Exception -> L66
            com.android.server.wm.MiuiFreeFormGestureAnimator r0 = r0.mGestureAnimator     // Catch: java.lang.Throwable -> L62 java.lang.Exception -> L66
            r0.removeAnimationControlLeash(r2)     // Catch: java.lang.Throwable -> L62 java.lang.Exception -> L66
            com.android.server.wm.MiuiFreeFormFlashBackHelper r0 = r5.mMiuiFreeFormFlashBackHelper     // Catch: java.lang.Throwable -> L62 java.lang.Exception -> L66
            r0.resetFlashBackWindowIfNeeded()     // Catch: java.lang.Throwable -> L62 java.lang.Exception -> L66
            com.android.server.wm.ActivityTaskManagerService r0 = r1.mActivityTaskManagerService     // Catch: java.lang.Throwable -> L62 java.lang.Exception -> L66
            int r7 = r2.mStackID     // Catch: java.lang.Throwable -> L62 java.lang.Exception -> L66
            r9 = 2
            r10 = r19
            r0.resizeTask(r7, r10, r9)     // Catch: java.lang.Exception -> L60 java.lang.Throwable -> Lae
            r18.updateCornerRadius()     // Catch: java.lang.Exception -> L60 java.lang.Throwable -> Lae
            goto L6c
        L60:
            r0 = move-exception
            goto L69
        L62:
            r0 = move-exception
            r10 = r19
            goto Laf
        L66:
            r0 = move-exception
            r10 = r19
        L69:
            r0.printStackTrace()     // Catch: java.lang.Throwable -> Lae
        L6c:
            com.android.server.wm.ActivityTaskManagerService r0 = r1.mActivityTaskManagerService
            com.android.server.wm.WindowManagerService r0 = r0.mWindowManager
            java.lang.String r7 = "updateMiuiFreeFormStackBounds"
            r0.closeSurfaceTransaction(r7)
            com.android.server.wm.MiuiFreeFormFlashBackHelper r0 = r5.mMiuiFreeFormFlashBackHelper
            com.android.server.wm.ActivityTaskManagerService r7 = r1.mActivityTaskManagerService
            com.android.server.wm.WindowManagerService r7 = r7.mWindowManager
            android.content.Context r7 = r7.mContext
            android.os.Handler r9 = r1.mHandler
            r0.stopFlashBackService(r7, r9)
            com.android.server.wm.ActivityTaskManagerService r0 = r1.mActivityTaskManagerService
            com.android.server.wm.WindowManagerService r0 = r0.mWindowManager
            com.android.server.wm.WindowManagerGlobalLock r7 = r0.mGlobalLock
            monitor-enter(r7)
            com.android.server.wm.ActivityTaskManagerService r0 = r1.mActivityTaskManagerService     // Catch: java.lang.Throwable -> Lab
            com.android.server.wm.WindowManagerService r0 = r0.mWindowManager     // Catch: java.lang.Throwable -> Lab
            r9 = 1
            r0.updateFocusedWindowLocked(r4, r9)     // Catch: java.lang.Throwable -> Lab
            monitor-exit(r7)     // Catch: java.lang.Throwable -> Lab
            com.android.server.wm.MiuiFreeformTrackManager r11 = r5.mTrackManager
            java.lang.String r12 = r3.enterWay
            java.lang.String r13 = r3.targetStackRatio
            java.lang.String r14 = r18.getStackPackageName()
            java.lang.String r15 = r18.getApplicationName()
            java.util.concurrent.ConcurrentHashMap<java.lang.Integer, com.android.server.wm.MiuiFreeFormActivityStack> r0 = r1.mFreeFormActivityStacks
            int r16 = r0.size()
            r11.trackSmallWindowEnterWayEvent(r12, r13, r14, r15, r16)
            return
        Lab:
            r0 = move-exception
            monitor-exit(r7)     // Catch: java.lang.Throwable -> Lab
            throw r0
        Lae:
            r0 = move-exception
        Laf:
            com.android.server.wm.ActivityTaskManagerService r4 = r1.mActivityTaskManagerService
            com.android.server.wm.WindowManagerService r4 = r4.mWindowManager
            java.lang.String r7 = "updateMiuiFreeFormStackBounds"
            r4.closeSurfaceTransaction(r7)
            throw r0
        Lb9:
            r0 = move-exception
            r10 = r19
            r8 = r20
        Lbe:
            monitor-exit(r7)     // Catch: java.lang.Throwable -> Lc0
            throw r0
        Lc0:
            r0 = move-exception
            goto Lbe
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wm.MiuiFreeFormManagerService.launchFreeformBySmallFreeform(com.android.server.wm.MiuiFreeFormActivityStack, android.graphics.Rect, float, com.android.server.wm.MiuiCvwGestureController$CvwTrackEvent):void");
    }

    public boolean updateMiuiFreeFormStackBounds(int taskId, float freeFormScale, Rect taskBounds, Rect miniFreeformBounds, MiuiCvwGestureController.CvwTrackEvent trackEvent) {
        MiuiFreeFormActivityStack mffas = getMiuiFreeFormActivityStackForMiuiFB(taskId);
        if (mffas == null) {
            return false;
        }
        if (mffas.mCVWControlInfo == null) {
            mffas.mCVWControlInfo = new MiuiFreeFormActivityStack.CVWControlInfo();
        }
        mffas.mStackRatio = trackEvent.targetStackRatio;
        mffas.mCVWControlInfo.freeFormScale = freeFormScale;
        mffas.mCVWControlInfo.miniFreeformBounds.set(miniFreeformBounds);
        MiuiFreeFormGestureController miuiFreeFormGestureController = (MiuiFreeFormGestureController) this.mActivityTaskManagerService.mWindowManager.mMiuiFreeFormGestureController;
        miuiFreeFormGestureController.mTrackManager.trackSmallWindowResizeEvent(trackEvent.originalStackRatio, trackEvent.targetStackRatio, mffas.getStackPackageName(), mffas.getApplicationName(), -1L);
        logd(TAG, "updateMiuiFreeFormStackBounds as = " + mffas + "  taskBounds = " + taskBounds);
        this.mActivityTaskManagerService.mWindowManager.openSurfaceTransaction();
        try {
            try {
                mffas.mFreeFormScale = freeFormScale;
                mffas.removeAnimationControlLeash();
                this.mActivityTaskManagerService.resizeTask(taskId, taskBounds, 2);
                mffas.updateCornerRadius();
            } catch (Exception e) {
                e.printStackTrace();
            }
            this.mActivityTaskManagerService.mWindowManager.closeSurfaceTransaction("updateMiuiFreeFormStackBounds");
            this.mActivityTaskManagerService.mWindowManager.scheduleAnimationLocked();
            this.mActivityTaskManagerService.mWindowManager.mMiuiCvwGestureController.resetGestureAnimatorState();
            return true;
        } catch (Throwable th) {
            this.mActivityTaskManagerService.mWindowManager.closeSurfaceTransaction("updateMiuiFreeFormStackBounds");
            throw th;
        }
    }

    public boolean updateMiuiFreeFormStackScale(int taskId, float freeFormScale) {
        MiuiFreeFormActivityStack mffas = getMiuiFreeFormActivityStackForMiuiFB(taskId);
        if (mffas == null) {
            return false;
        }
        if (mffas.mCVWControlInfo == null) {
            mffas.mCVWControlInfo = new MiuiFreeFormActivityStack.CVWControlInfo();
        }
        mffas.mFreeFormScale = freeFormScale;
        mffas.mCVWControlInfo.freeFormScale = freeFormScale;
        logd(TAG, "updateFreeFormStackScale as = " + mffas + "   freeFormScale = " + freeFormScale);
        mffas.updateCornerRadius();
        dispatchFreeFormStackModeChanged(7, mffas);
        return true;
    }

    public Rect getMiuiFreeFormStackBounds(int taskId) {
        MiuiFreeFormActivityStack mffas = getMiuiFreeFormActivityStackForMiuiFB(taskId);
        if (mffas != null) {
            return mffas.mTask.getBounds();
        }
        return null;
    }

    public float getMiuiFreeFormActivityStackScale(int taskId) {
        MiuiFreeFormActivityStack mffas = getMiuiFreeFormActivityStackForMiuiFB(taskId);
        if (mffas != null) {
            return mffas.mFreeFormScale;
        }
        return 1.0f;
    }

    public MiuiFreeFormActivityStack getTargetResizeMiuiFreeFormStack(final float touchX, final float touchY) {
        MiuiFreeFormActivityStack mffas;
        synchronized (this.mActivityTaskManagerService.mWindowManager.mGlobalLock) {
            TaskDisplayArea defaultTaskDisplayArea = this.mActivityTaskManagerService.mRootWindowContainer.getDefaultTaskDisplayArea();
            final Rect visualRect = new Rect();
            Task targetTask = defaultTaskDisplayArea.getTask(new Predicate() { // from class: com.android.server.wm.MiuiFreeFormManagerService$$ExternalSyntheticLambda5
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return MiuiFreeFormManagerService.this.m1679x796b43ea(visualRect, touchX, touchY, (Task) obj);
                }
            }, true);
            MiuiFreeFormGestureController miuiFreeFormGestureController = (MiuiFreeFormGestureController) this.mActivityTaskManagerService.mWindowManager.mMiuiFreeFormGestureController;
            boolean isInExcludeRegion = miuiFreeFormGestureController.mGestureListener.isInExcludeRegion(touchX, touchY);
            mffas = targetTask == null ? null : getMiuiFreeFormActivityStackForMiuiFB(targetTask.getRootTaskId());
            if (isInExcludeRegion || !MiuiMultiWindowUtils.inCvwResizeRegion(visualRect, touchX, touchY)) {
                mffas = null;
            }
            logd(TAG, "getTargetResizeMiuiFreeFormStack mffas = " + mffas + " isInExcludeRegion=" + isInExcludeRegion);
        }
        return mffas;
    }

    /* renamed from: lambda$getTargetResizeMiuiFreeFormStack$3$com-android-server-wm-MiuiFreeFormManagerService */
    public /* synthetic */ boolean m1679x796b43ea(Rect visualRect, float touchX, float touchY, Task t) {
        MiuiFreeFormActivityStack mffs;
        if (!t.inFreeformSmallWinMode() || (mffs = getMiuiFreeFormActivityStackForMiuiFB(t.getRootTaskId())) == null || mffs.inPinMode()) {
            return false;
        }
        if (mffs.isInMiniFreeFormMode()) {
            visualRect.set(mffs.mStackControlInfo.mSmallWindowBounds);
        } else if (mffs.isInFreeFormMode()) {
            visualRect.set(MiuiMultiWindowUtils.getVisualBounds(mffs.mTask.getBounds(), mffs.mFreeFormScale));
        }
        return visualRect.contains((int) touchX, (int) touchY) || MiuiMultiWindowUtils.inCvwResizeRegion(visualRect, touchX, touchY);
    }

    public MiuiFreeFormActivityStackStub getMiuiFreeFormActivityStack(int taskId) {
        return this.mFreeFormActivityStacks.get(Integer.valueOf(taskId));
    }

    public MiuiFreeFormActivityStack getMiuiFreeFormActivityStackForMiuiFB(int taskId) {
        return this.mFreeFormActivityStacks.get(Integer.valueOf(taskId));
    }

    public MiuiFreeFormActivityStackStub getMiuiFreeFormActivityStack(String packageName, int userId) {
        for (Integer taskId : this.mFreeFormActivityStacks.keySet()) {
            MiuiFreeFormActivityStack mffas = this.mFreeFormActivityStacks.get(taskId);
            if (mffas != null && mffas.getStackPackageName() != null && mffas.getStackPackageName().equals(packageName) && mffas.mUserId == userId) {
                return mffas;
            }
        }
        return null;
    }

    public List<MiuiFreeFormActivityStack> getAllMiuiFreeFormActivityStack() {
        List<MiuiFreeFormActivityStack> stackList = new ArrayList<>();
        for (MiuiFreeFormActivityStack stack : this.mFreeFormActivityStacks.values()) {
            stackList.add(stack);
        }
        return stackList;
    }

    public boolean isInFreeForm(String packageName) {
        for (Integer taskId : this.mFreeFormActivityStacks.keySet()) {
            MiuiFreeFormActivityStack mffas = this.mFreeFormActivityStacks.get(taskId);
            if (mffas != null && mffas.getStackPackageName() != null && mffas.getStackPackageName().equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    public int getFreeFormWindowMode(int rootStackId) {
        MiuiFreeFormActivityStack mffas = getMiuiFreeFormActivityStackForMiuiFB(rootStackId);
        if (mffas == null) {
            return -1;
        }
        return mffas.mMiuiFreeFromWindowMode;
    }

    public Rect getSmallFreeFormWindowBounds(int rootStackId) {
        MiuiFreeFormActivityStack mffas = getMiuiFreeFormActivityStackForMiuiFB(rootStackId);
        if (mffas == null) {
            return null;
        }
        return new Rect(mffas.mStackControlInfo.mSmallWindowBounds);
    }

    public MiuiFreeFormActivityStack getTopFreeFormActivityStack() {
        synchronized (this.mActivityTaskManagerService.mWindowManager.mGlobalLock) {
            Task topRootTask = this.mActivityTaskManagerService.mRootWindowContainer.getDefaultTaskDisplayArea().getTopRootTaskInWindowingMode(5);
            if (topRootTask == null) {
                return null;
            }
            return getMiuiFreeFormActivityStackForMiuiFB(topRootTask.getRootTaskId());
        }
    }

    public MiuiFreeFormActivityStack getBottomFreeFormActivityStack(final int freeformStackSize, final MiuiFreeFormActivityStack addingStack) {
        synchronized (this.mActivityTaskManagerService.mWindowManager.mGlobalLock) {
            TaskDisplayArea defaultTaskDisplayArea = this.mActivityTaskManagerService.mRootWindowContainer.getDefaultTaskDisplayArea();
            final MiuiFreeFormGestureController miuiFreeFormGestureController = (MiuiFreeFormGestureController) this.mActivityTaskManagerService.mWindowManager.mMiuiFreeFormGestureController;
            final List<MiuiFreeFormManager.MiuiFreeFormStackInfo> pinedList = getAllPinedFreeFormStackInfosOnDisplay(defaultTaskDisplayArea.mDisplayContent.getDisplayId());
            final boolean hasMiniPined = miuiFreeFormGestureController.mMiuiFreeformPinManagerService.hasMiniPined(pinedList);
            miuiFreeFormGestureController.mMiuiFreeformPinManagerService.resetPinedCount();
            Task bottomTask = defaultTaskDisplayArea.getTask(new Predicate() { // from class: com.android.server.wm.MiuiFreeFormManagerService$$ExternalSyntheticLambda1
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return MiuiFreeFormManagerService.this.m1677x1b4af12e(freeformStackSize, addingStack, miuiFreeFormGestureController, pinedList, hasMiniPined, (Task) obj);
                }
            }, false);
            if (bottomTask == null) {
                return null;
            }
            return getMiuiFreeFormActivityStackForMiuiFB(bottomTask.getRootTaskId());
        }
    }

    /* renamed from: lambda$getBottomFreeFormActivityStack$4$com-android-server-wm-MiuiFreeFormManagerService */
    public /* synthetic */ boolean m1677x1b4af12e(int freeformStackSize, MiuiFreeFormActivityStack addingStack, MiuiFreeFormGestureController miuiFreeFormGestureController, List pinedList, boolean hasMiniPined, Task t) {
        if (t.inFreeformSmallWinMode()) {
            if (freeformStackSize > 2) {
                MiuiFreeFormActivityStack formActivityStack = this.mFreeFormActivityStacks.get(Integer.valueOf(t.getRootTaskId()));
                if (formActivityStack != null && formActivityStack != addingStack) {
                    miuiFreeFormGestureController.mMiuiFreeformPinManagerService.calculatePinedCount(formActivityStack);
                    if ((formActivityStack.isInFreeFormMode() && !formActivityStack.inPinMode()) || ((formActivityStack.isInMiniFreeFormMode() && !formActivityStack.inPinMode() && pinedList.size() >= 1) || ((formActivityStack.isInFreeFormMode() && formActivityStack.inPinMode() && (miuiFreeFormGestureController.mMiuiFreeformPinManagerService.getmPinedFreeformCount() >= 2 || hasMiniPined)) || (formActivityStack.isInMiniFreeFormMode() && formActivityStack.inPinMode() && miuiFreeFormGestureController.mMiuiFreeformPinManagerService.getmPinedMiniFreeformCount() >= 2)))) {
                        return true;
                    }
                }
                return false;
            }
            MiuiFreeFormActivityStack stack = this.mFreeFormActivityStacks.get(Integer.valueOf(t.getRootTaskId()));
            logd(true, TAG, "getBottomFreeFormActivityStack stack.getStackPackageName()= " + (stack == null ? null : stack.getStackPackageName()) + " addingStack.getStackPackageName()= " + addingStack.getStackPackageName());
            return stack != null && !stack.equals(addingStack);
        }
        return false;
    }

    public MiuiFreeFormActivityStack getHomeActivityStack() {
        Task homeTask = this.mActivityTaskManagerService.mRootWindowContainer.getDefaultTaskDisplayArea().getRootHomeTask();
        if (homeTask == null) {
            return null;
        }
        return this.mFreeFormSurpportActivityStacks.get(Integer.valueOf(homeTask.getRootTaskId()));
    }

    public List<MiuiFreeFormManager.MiuiFreeFormStackInfo> getAllFreeFormStackInfosOnDisplay(int displayId) {
        long ident = Binder.clearCallingIdentity();
        try {
            List<MiuiFreeFormManager.MiuiFreeFormStackInfo> list = new ArrayList<>();
            if (-1 == displayId) {
                for (Integer taskId : this.mFreeFormActivityStacks.keySet()) {
                    MiuiFreeFormActivityStack mffas = this.mFreeFormActivityStacks.get(taskId);
                    if (mffas != null) {
                        list.add(mffas.getMiuiFreeFormStackInfo());
                    }
                }
                return list;
            }
            for (Integer taskId2 : this.mFreeFormActivityStacks.keySet()) {
                MiuiFreeFormActivityStack mffas2 = this.mFreeFormActivityStacks.get(taskId2);
                if (mffas2 != null && mffas2.mDisplayId == displayId) {
                    list.add(mffas2.getMiuiFreeFormStackInfo());
                }
            }
            return list;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public boolean getAllFreeFormStackOnDisplayEndPinByHandyMode(int displayId) {
        long ident = Binder.clearCallingIdentity();
        try {
            new ArrayList();
            if (-1 == displayId) {
                for (Integer taskId : this.mFreeFormActivityStacks.keySet()) {
                    MiuiFreeFormActivityStack mffas = this.mFreeFormActivityStacks.get(taskId);
                    if (mffas != null && mffas.mIsRunningPinAnim) {
                        return false;
                    }
                }
                return true;
            }
            for (Integer taskId2 : this.mFreeFormActivityStacks.keySet()) {
                MiuiFreeFormActivityStack mffas2 = this.mFreeFormActivityStacks.get(taskId2);
                if (mffas2 != null && mffas2.mDisplayId == displayId && mffas2.mIsRunningPinAnim) {
                    return false;
                }
            }
            return true;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public List<MiuiFreeFormManager.MiuiFreeFormStackInfo> getAllPinedFreeFormStackInfosOnDisplay(int displayId) {
        long ident = Binder.clearCallingIdentity();
        try {
            List<MiuiFreeFormManager.MiuiFreeFormStackInfo> list = new ArrayList<>();
            if (-1 == displayId) {
                for (Map.Entry<Integer, MiuiFreeFormActivityStack> entry : this.mFreeFormActivityStacks.entrySet()) {
                    MiuiFreeFormActivityStack mffas = entry.getValue();
                    if (mffas != null && mffas.inPinMode()) {
                        list.add(mffas.getMiuiFreeFormStackInfo());
                    }
                }
                return list;
            }
            for (Map.Entry<Integer, MiuiFreeFormActivityStack> entry2 : this.mFreeFormActivityStacks.entrySet()) {
                MiuiFreeFormActivityStack mffas2 = entry2.getValue();
                if (mffas2 != null && mffas2.mDisplayId == displayId && mffas2.inPinMode()) {
                    list.add(mffas2.getMiuiFreeFormStackInfo());
                }
            }
            return list;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public List<Rect> getCurrPinFloatingWindowPos(boolean isSortByTop) {
        return getCurrPinFloatingWindowPos(isSortByTop, false);
    }

    public List<Rect> getCurrPinFloatingWindowPos(boolean isSortByTop, boolean addSideBarRects) {
        List<Rect> pinPosList = new ArrayList<>();
        for (Map.Entry<Integer, MiuiFreeFormActivityStack> entry : this.mFreeFormActivityStacks.entrySet()) {
            MiuiFreeFormActivityStack mffas = entry.getValue();
            if (mffas != null && mffas.inPinMode() && mffas.mIsPinFloatingWindowPosInit) {
                pinPosList.add(new Rect(mffas.mPinFloatingWindowPos));
            }
        }
        if (addSideBarRects) {
            MiuiFreeFormGestureController miuiFreeFormGestureController = (MiuiFreeFormGestureController) this.mActivityTaskManagerService.mWindowManager.mMiuiFreeFormGestureController;
            ArrayList<Rect> sideBarRects = miuiFreeFormGestureController.mMiuiFreeformPinManagerService.getSidebarLineRects();
            Iterator<Rect> it = sideBarRects.iterator();
            while (it.hasNext()) {
                Rect rect = it.next();
                if (!rect.isEmpty()) {
                    pinPosList.add(new Rect(rect));
                }
            }
        }
        if (isSortByTop) {
            pinPosList.sort(new Comparator() { // from class: com.android.server.wm.MiuiFreeFormManagerService$$ExternalSyntheticLambda4
                @Override // java.util.Comparator
                public final int compare(Object obj, Object obj2) {
                    return MiuiFreeFormManagerService.lambda$getCurrPinFloatingWindowPos$5((Rect) obj, (Rect) obj2);
                }
            });
        }
        Slog.d(TAG, "getOtherPinFloatingWindowPos: pinPosList: " + pinPosList);
        return pinPosList;
    }

    public static /* synthetic */ int lambda$getCurrPinFloatingWindowPos$5(Rect rect1, Rect rect2) {
        return rect1.top - rect2.top;
    }

    /* JADX WARN: Removed duplicated region for block: B:6:0x001d A[Catch: all -> 0x0062, TryCatch #2 {all -> 0x0062, blocks: (B:3:0x0004, B:4:0x0016, B:6:0x001d, B:8:0x0029, B:12:0x0036, B:13:0x003c, B:14:0x003d, B:16:0x0049, B:19:0x004e, B:20:0x005a), top: B:26:0x0004 }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public miui.app.MiuiFreeFormManager.MiuiFreeFormStackInfo getFreeFormStackToAvoid(final int r8, java.lang.String r9) {
        /*
            r7 = this;
            long r0 = android.os.Binder.clearCallingIdentity()
            com.android.server.wm.ActivityTaskManagerService r2 = r7.mActivityTaskManagerService     // Catch: java.lang.Throwable -> L62
            com.android.server.wm.RootWindowContainer r2 = r2.mRootWindowContainer     // Catch: java.lang.Throwable -> L62
            com.android.server.wm.TaskDisplayArea r2 = r2.getDefaultTaskDisplayArea()     // Catch: java.lang.Throwable -> L62
            java.util.concurrent.ConcurrentHashMap<java.lang.Integer, com.android.server.wm.MiuiFreeFormActivityStack> r3 = r7.mFreeFormActivityStacks     // Catch: java.lang.Throwable -> L62
            java.util.Collection r3 = r3.values()     // Catch: java.lang.Throwable -> L62
            java.util.Iterator r3 = r3.iterator()     // Catch: java.lang.Throwable -> L62
        L16:
            boolean r4 = r3.hasNext()     // Catch: java.lang.Throwable -> L62
            r5 = 0
            if (r4 == 0) goto L36
            java.lang.Object r4 = r3.next()     // Catch: java.lang.Throwable -> L62
            com.android.server.wm.MiuiFreeFormActivityStack r4 = (com.android.server.wm.MiuiFreeFormActivityStack) r4     // Catch: java.lang.Throwable -> L62
            boolean r6 = r4.isInMiniFreeFormMode()     // Catch: java.lang.Throwable -> L62
            if (r6 != 0) goto L31
            boolean r6 = r4.inPinMode()     // Catch: java.lang.Throwable -> L62
            if (r6 == 0) goto L30
            goto L31
        L30:
            goto L16
        L31:
            android.os.Binder.restoreCallingIdentity(r0)
            return r5
        L36:
            com.android.server.wm.ActivityTaskManagerService r3 = r7.mActivityTaskManagerService     // Catch: java.lang.Throwable -> L62
            com.android.server.wm.WindowManagerService r3 = r3.mWindowManager     // Catch: java.lang.Throwable -> L62
            com.android.server.wm.WindowManagerGlobalLock r3 = r3.mGlobalLock     // Catch: java.lang.Throwable -> L62
            monitor-enter(r3)     // Catch: java.lang.Throwable -> L62
            com.android.server.wm.MiuiFreeFormManagerService$$ExternalSyntheticLambda0 r4 = new com.android.server.wm.MiuiFreeFormManagerService$$ExternalSyntheticLambda0     // Catch: java.lang.Throwable -> L5f
            r4.<init>()     // Catch: java.lang.Throwable -> L5f
            r6 = 1
            com.android.server.wm.Task r4 = r2.getTask(r4, r6)     // Catch: java.lang.Throwable -> L5f
            if (r4 != 0) goto L4e
            monitor-exit(r3)     // Catch: java.lang.Throwable -> L5f
            android.os.Binder.restoreCallingIdentity(r0)
            return r5
        L4e:
            int r5 = r4.getRootTaskId()     // Catch: java.lang.Throwable -> L5f
            com.android.server.wm.MiuiFreeFormActivityStack r5 = r7.getMiuiFreeFormActivityStackForMiuiFB(r5)     // Catch: java.lang.Throwable -> L5f
            miui.app.MiuiFreeFormManager$MiuiFreeFormStackInfo r5 = r5.getMiuiFreeFormStackInfo()     // Catch: java.lang.Throwable -> L5f
            monitor-exit(r3)     // Catch: java.lang.Throwable -> L5f
            android.os.Binder.restoreCallingIdentity(r0)
            return r5
        L5f:
            r4 = move-exception
            monitor-exit(r3)     // Catch: java.lang.Throwable -> L5f
            throw r4     // Catch: java.lang.Throwable -> L62
        L62:
            r2 = move-exception
            android.os.Binder.restoreCallingIdentity(r0)
            throw r2
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wm.MiuiFreeFormManagerService.getFreeFormStackToAvoid(int, java.lang.String):miui.app.MiuiFreeFormManager$MiuiFreeFormStackInfo");
    }

    /* renamed from: lambda$getFreeFormStackToAvoid$6$com-android-server-wm-MiuiFreeFormManagerService */
    public /* synthetic */ boolean m1678x6544d8b6(int displayId, Task t) {
        MiuiFreeFormActivityStack mffas;
        if (t.inFreeformSmallWinMode() && (mffas = getMiuiFreeFormActivityStackForMiuiFB(t.getRootTaskId())) != null) {
            if ((-1 == displayId || mffas.mDisplayId == displayId) && mffas.mStackControlInfo.mCurrentAnimation != 0 && !mffas.inPinMode()) {
                return true;
            }
            return false;
        }
        return false;
    }

    public MiuiFreeFormManager.MiuiFreeFormStackInfo getFreeFormStackInfoByActivity(IBinder token) {
        MiuiFreeFormActivityStack mffas;
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mActivityTaskManagerService.mWindowManager.mGlobalLock) {
                ActivityRecord r = ActivityRecord.isInRootTaskLocked(token);
                if (r != null && (mffas = getMiuiFreeFormActivityStackForMiuiFB(r.getRootTaskId())) != null) {
                    return mffas.getMiuiFreeFormStackInfo();
                }
                return null;
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public MiuiFreeFormManager.MiuiFreeFormStackInfo getFreeFormStackInfoByWindow(IBinder wtoken) {
        MiuiFreeFormActivityStack mffas;
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mActivityTaskManagerService.mWindowManager.mGlobalLock) {
                WindowState win = this.mActivityTaskManagerService.mWindowManager.windowForClientLocked((Session) null, wtoken, false);
                if (win != null && win.mActivityRecord != null && (mffas = getMiuiFreeFormActivityStackForMiuiFB(win.mActivityRecord.getRootTaskId())) != null) {
                    return mffas.getMiuiFreeFormStackInfo();
                }
                return null;
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public MiuiFreeFormManager.MiuiFreeFormStackInfo getFreeFormStackInfoByStackId(int stackId) {
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mActivityTaskManagerService.mWindowManager.mGlobalLock) {
                MiuiFreeFormActivityStack mffas = getMiuiFreeFormActivityStackForMiuiFB(stackId);
                if (mffas != null) {
                    return mffas.getMiuiFreeFormStackInfo();
                }
                return null;
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void updatePinFloatingWindowPos(Rect rect, int taskId) {
        updatePinFloatingWindowPos(rect, taskId, true);
    }

    public void updatePinFloatingWindowPos(Rect rect, int taskId, boolean isMovePinPos) {
        long ident = Binder.clearCallingIdentity();
        try {
            try {
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (!isSupportPin()) {
                return;
            }
            synchronized (this.mActivityTaskManagerService.mWindowManager.mGlobalLock) {
                Slog.d(TAG, "updatePinFloatingWindowPos: rect" + rect + " taskId: " + taskId);
                MiuiFreeFormActivityStack mffas = getMiuiFreeFormActivityStackForMiuiFB(taskId);
                if (mffas != null) {
                    MiuiFreeFormGestureController miuiFreeFormGestureController = (MiuiFreeFormGestureController) this.mActivityTaskManagerService.mWindowManager.mMiuiFreeFormGestureController;
                    miuiFreeFormGestureController.mMiuiFreeformPinManagerService.updatePinFloatingWindowPos(mffas, rect, isMovePinPos);
                }
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public boolean unPinFloatingWindow(int taskId) {
        if (!isSupportPin()) {
            return false;
        }
        MiuiFreeFormActivityStack mffas = getMiuiFreeFormActivityStackForMiuiFB(taskId);
        if (mffas == null || mffas.mPinFloatingWindowPos == null || mffas.mPinFloatingWindowPos.isEmpty()) {
            Slog.d(TAG, "skip unPinFloatingWindow: taskId" + taskId + " mffas: " + mffas);
            return false;
        }
        return unPinFloatingWindow(mffas.mPinFloatingWindowPos, taskId, MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA, true);
    }

    public boolean unPinFloatingWindow(Rect rect, int taskId, float upVelocityX, float upVelocityY, boolean isClick) {
        boolean unPinFloatingWindow;
        long ident = Binder.clearCallingIdentity();
        try {
            try {
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (!isSupportPin()) {
                return false;
            }
            MiuiFreeFormActivityStack mffas = getMiuiFreeFormActivityStackForMiuiFB(taskId);
            if (mffas != null && mffas.inPinMode()) {
                updatePinFloatingWindowPos(rect, taskId, false);
                synchronized (this.mActivityTaskManagerService.mWindowManager.mGlobalLock) {
                    MiuiFreeFormGestureController miuiFreeFormGestureController = (MiuiFreeFormGestureController) this.mActivityTaskManagerService.mWindowManager.mMiuiFreeFormGestureController;
                    unPinFloatingWindow = miuiFreeFormGestureController.mMiuiFreeformPinManagerService.unPinFloatingWindow(mffas, upVelocityX, upVelocityY, isClick);
                }
                return unPinFloatingWindow;
            }
            return false;
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void hidePinFloatingWindow(int taskId) {
        long ident = Binder.clearCallingIdentity();
        try {
            try {
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (!isSupportPin()) {
                return;
            }
            synchronized (this.mActivityTaskManagerService.mWindowManager.mGlobalLock) {
                MiuiFreeFormActivityStack mffas = getMiuiFreeFormActivityStackForMiuiFB(taskId);
                if (mffas != null) {
                    MiuiFreeFormGestureController miuiFreeFormGestureController = (MiuiFreeFormGestureController) this.mActivityTaskManagerService.mWindowManager.mMiuiFreeFormGestureController;
                    miuiFreeFormGestureController.mMiuiFreeformPinManagerService.hidePinFloatingWindow(mffas);
                }
            }
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void dispatchFreeFormStackModeChanged(final int action, MiuiFreeFormActivityStack mffas) {
        if (mffas == null) {
            return;
        }
        final MiuiFreeFormManager.MiuiFreeFormStackInfo freeFormASInfo = mffas.getMiuiFreeFormStackInfo();
        logd(true, TAG, "action: " + action + "dispatchFreeFormStackModeChanged freeFormASInfo = " + freeFormASInfo);
        synchronized (this.mCallbacks) {
            int callbacksCount = this.mCallbacks.beginBroadcast();
            for (int i = 0; i < callbacksCount; i++) {
                IFreeformCallback freeformCallback = this.mCallbacks.getBroadcastItem(i);
                try {
                    freeformCallback.dispatchFreeFormStackModeChanged(action, freeFormASInfo);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            this.mCallbacks.finishBroadcast();
        }
        if (mffas.mTask != null) {
            synchronized (mffas.mTask.mWmService.mGlobalLock) {
                mffas.mTask.forAllWindows(new Consumer() { // from class: com.android.server.wm.MiuiFreeFormManagerService$$ExternalSyntheticLambda2
                    @Override // java.util.function.Consumer
                    public final void accept(Object obj) {
                        ((WindowState) obj).dispatchFreeFormStackModeChanged(action, freeFormASInfo);
                    }
                }, true);
            }
        }
    }

    public void registerFreeformCallback(IFreeformCallback freeformCallback) {
        synchronized (this.mCallbacks) {
            this.mCallbacks.register(freeformCallback);
        }
    }

    public void unregisterFreeformCallback(IFreeformCallback freeformCallback) {
        synchronized (this.mCallbacks) {
            this.mCallbacks.unregister(freeformCallback);
        }
    }

    public int getFirstUseMiuiFreeForm() {
        return getFirstUseMiuiFreeForm(this.mActivityTaskManagerService.mWindowManager.mContext);
    }

    public int getFirstUseMiuiFreeForm(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(), "first_use_freeform", 0);
    }

    public void setFirstUseMiuiFreeForm(int first) {
        long origId = Binder.clearCallingIdentity();
        try {
            setFirstUseMiuiFreeForm(this.mActivityTaskManagerService.mWindowManager.mContext, first);
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public void setFirstUseMiuiFreeForm(Context context, int first) {
        Settings.Secure.putIntForUser(context.getContentResolver(), "first_use_freeform", first, -2);
    }

    public int getFirstUseTipConfirmTimes() {
        return getFirstUseTipConfirmTimes(this.mActivityTaskManagerService.mWindowManager.mContext);
    }

    public int getFirstUseTipConfirmTimes(Context context) {
        return Settings.Secure.getIntForUser(context.getContentResolver(), "first_use_tip_confirm_times", 0, -2);
    }

    public void setFirstUseTipConfirmTimes(int time) {
        long origId = Binder.clearCallingIdentity();
        try {
            setFirstUseTipConfirmTimes(time, this.mActivityTaskManagerService.mWindowManager.mContext);
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
    }

    public void setFirstUseTipConfirmTimes(int time, Context context) {
        Settings.Secure.putIntForUser(context.getContentResolver(), "first_use_tip_confirm_times", time, -2);
    }

    public void trackClickSmallWindowEvent() {
        MiuiFreeFormGestureController miuiFreeFormGestureController = (MiuiFreeFormGestureController) this.mActivityTaskManagerService.mWindowManager.mMiuiFreeFormGestureController;
        MiuiFreeFormActivityStack topFreeFormActivityStack = getTopFreeFormActivityStack();
        if (topFreeFormActivityStack != null) {
            miuiFreeFormGestureController.mTrackManager.trackClickSmallWindowEvent(topFreeFormActivityStack.getStackPackageName(), topFreeFormActivityStack.getApplicationName());
        }
    }

    public List<MiuiFreeFormActivityStack> getNotPinedMiniFreeFormActivityStack() {
        List<MiuiFreeFormActivityStack> miniFfas = new ArrayList<>();
        for (MiuiFreeFormActivityStack ffas : this.mFreeFormActivityStacks.values()) {
            if (ffas.isInMiniFreeFormMode() && !ffas.inPinMode()) {
                miniFfas.add(ffas);
            }
        }
        return miniFfas;
    }

    public List<MiuiFreeFormActivityStack> getMiniFreeFormActivityStack() {
        List<MiuiFreeFormActivityStack> miniFfas = new ArrayList<>();
        for (MiuiFreeFormActivityStack ffas : this.mFreeFormActivityStacks.values()) {
            if (ffas.isInMiniFreeFormMode()) {
                miniFfas.add(ffas);
            }
        }
        return miniFfas;
    }

    public MiuiFreeFormActivityStack getTopInMiniWindowActivityStack(float x, float y) {
        for (Integer taskId : this.mFreeFormActivityStacks.keySet()) {
            MiuiFreeFormActivityStack mffas = this.mFreeFormActivityStacks.get(taskId);
            if (mffas != null && mffas.mMiuiFreeFromWindowMode == 1 && mffas.mStackControlInfo.mSmallWindowBounds.contains((int) x, (int) y)) {
                return mffas;
            }
        }
        return null;
    }

    public void notifyAppStopped(Task rootTask) {
        MiuiFreeFormActivityStack mffas;
        if (rootTask != null && rootTask.inFreeformSmallWinMode() && (mffas = getMiuiFreeFormActivityStackForMiuiFB(rootTask.getRootTaskId())) != null) {
            MiuiFreeFormGestureController miuiFreeFormGestureController = (MiuiFreeFormGestureController) this.mActivityTaskManagerService.mWindowManager.mMiuiFreeFormGestureController;
            miuiFreeFormGestureController.notifyAppStopped(mffas);
        }
    }

    public void moveTaskToFront(Task task) {
        MiuiFreeFormActivityStack mffas;
        if (task != null && task.inFreeformSmallWinMode() && (mffas = getMiuiFreeFormActivityStackForMiuiFB(task.getRootTaskId())) != null) {
            MiuiFreeFormGestureController miuiFreeFormGestureController = (MiuiFreeFormGestureController) this.mActivityTaskManagerService.mWindowManager.mMiuiFreeFormGestureController;
            miuiFreeFormGestureController.moveTaskToFront(mffas);
            if (mffas.inPinMode()) {
                if (mffas.isInMiniFreeFormMode()) {
                    mffas.setStackFreeFormMode(0);
                    mffas.removeAnimationControlLeash();
                }
                unPinFloatingWindow(task.mTaskId);
            }
        }
    }

    public void moveToFront(Task task) {
        if (task == null || !task.inFreeformSmallWinMode()) {
            return;
        }
        avoidedByOthersIfNeeded(task);
        MiuiFreeFormActivityStack mffas = getMiuiFreeFormActivityStackForMiuiFB(task.getRootTaskId());
        if (mffas != null) {
            MiuiFreeFormGestureController miuiFreeFormGestureController = (MiuiFreeFormGestureController) this.mActivityTaskManagerService.mWindowManager.mMiuiFreeFormGestureController;
            miuiFreeFormGestureController.moveToFront(mffas);
        }
    }

    public boolean shouldStopStartFreeform(String packageName) {
        return this.mFreeFormStackDisplayStrategy.shouldStopStartFreeform(packageName);
    }

    public List<MiuiFreeFormManager.MiuiFreeFormStackInfo> getCurrentUnReplaceFreeform(String packageName) {
        List<MiuiFreeFormManager.MiuiFreeFormStackInfo> list = new ArrayList<>();
        for (Map.Entry<Integer, MiuiFreeFormActivityStack> entry : this.mFreeFormActivityStacks.entrySet()) {
            MiuiFreeFormActivityStack mffas = entry.getValue();
            if (mffas != null && packageName != null && !packageName.equals(mffas.getStackPackageName()) && (mffas.isInMiniFreeFormMode() || mffas.inPinMode())) {
                list.add(mffas.getMiuiFreeFormStackInfo());
            }
        }
        return list;
    }

    public int getMaxMiuiFreeFormStackCountForFlashBack(String packageName, boolean isLaunchFlashBack) {
        return this.mFreeFormStackDisplayStrategy.getMaxMiuiFreeFormStackCountForFlashBack(packageName, isLaunchFlashBack);
    }

    public String getStackPackageName(Task targetTask) {
        return this.mFreeFormStackDisplayStrategy.getStackPackageName(targetTask);
    }

    public void avoidedByOthersIfNeeded(Task task) {
        MiuiFreeFormActivityStack newlyAddedFreeFormStack;
        if (task != null && task.inFreeformSmallWinMode() && MiuiMultiWindowUtils.isPadScreen(this.mActivityTaskManagerService.mContext) && (newlyAddedFreeFormStack = getMiuiFreeFormActivityStackForMiuiFB(task.getRootTaskId())) != null && newlyAddedFreeFormStack.mTask.mFreeFormShouldBeAvoided) {
            if (this.mFreeFormActivityStacks.values().size() == 2) {
                for (MiuiFreeFormActivityStack stack : this.mFreeFormActivityStacks.values()) {
                    if (stack != null && stack != newlyAddedFreeFormStack && stack.mMiuiFreeFromWindowMode == 0 && stack.mStackControlInfo.mCurrentAnimation != 0 && !stack.inPinMode()) {
                        stack.avoidNewlyAddedStackIfNeeded(newlyAddedFreeFormStack);
                    }
                }
            }
            newlyAddedFreeFormStack.mTask.mFreeFormShouldBeAvoided = false;
        }
    }

    public void notifyCameraStateChanged(String packageName, int cameraState) {
        MiuiFreeFormCameraStrategy miuiFreeFormCameraStrategy = this.mFreeFormCameraStrategy;
        int i = 1;
        if (cameraState != 1) {
            i = 0;
        }
        miuiFreeFormCameraStrategy.onCameraStateChanged(packageName, i);
    }

    public boolean openCameraInFreeForm(String packageName) {
        return this.mFreeFormCameraStrategy.openCameraInFreeForm(packageName);
    }

    public boolean startActivityUncheckedBefore(Task task) {
        MiuiFreeFormActivityStack mffas;
        if (task == null || !task.inFreeformSmallWinMode() || (mffas = getMiuiFreeFormActivityStackForMiuiFB(task.getRootTaskId())) == null) {
            return false;
        }
        MiuiFreeFormGestureController miuiFreeFormGestureController = (MiuiFreeFormGestureController) this.mActivityTaskManagerService.mWindowManager.mMiuiFreeFormGestureController;
        return miuiFreeFormGestureController.startActivityUncheckedBefore(mffas);
    }

    public boolean handleAppDied(WindowProcessController app, Task task) {
        if (task == null || !task.inFreeformSmallWinMode()) {
            return false;
        }
        MiuiFreeFormActivityStack mffas = getMiuiFreeFormActivityStackForMiuiFB(task.getRootTaskId());
        MiuiFreeFormGestureController miuiFreeFormGestureController = (MiuiFreeFormGestureController) this.mActivityTaskManagerService.mWindowManager.mMiuiFreeFormGestureController;
        if (mffas != null) {
            if (mffas.isInMiniFreeFormMode()) {
                if (app != null && app.mInfo != null && app.mInfo.packageName.equals(mffas.getStackPackageName())) {
                    miuiFreeFormGestureController.mTrackManager.trackMiniWindowQuitEvent("其他", new Point(mffas.mStackControlInfo.mSmallWindowBounds.left, mffas.mStackControlInfo.mSmallWindowBounds.top), mffas.getStackPackageName(), mffas.getApplicationName());
                    return true;
                }
                return true;
            } else if (mffas.isInFreeFormMode() && app != null && app.mInfo != null && app.mInfo.packageName.equals(mffas.getStackPackageName())) {
                miuiFreeFormGestureController.mTrackManager.trackSmallWindowQuitEvent("其他", mffas.mStackRatio, mffas.getStackPackageName(), mffas.getApplicationName(), -1L);
                return true;
            } else {
                return true;
            }
        }
        return true;
    }

    public static void logd(String tag, String string) {
        logd(false, tag, string);
    }

    public static void logd(boolean enable, String tag, String string) {
        if (MiuiFreeFormGestureController.DEBUG || enable) {
            Slog.d(tag, string);
        }
    }

    public String getDumpInfo() {
        String infos = "FreeForm ActivityStacks:\n";
        int num = 1;
        for (MiuiFreeFormActivityStack value : this.mFreeFormActivityStacks.values()) {
            infos = ((infos + "Stack " + num + " :\n") + value) + "\n";
            num++;
        }
        return infos + "Total " + (num - 1) + " freeform stacks\n";
    }

    public void postAtFrontOfQueue(Runnable r) {
        this.mHandler.postAtFrontOfQueue(r);
    }

    public void setCameraOrientation(int newRotation) {
        this.mFreeFormCameraStrategy.setCameraOrientation(newRotation);
    }

    public void updateTaskUserId(int taskId, int userId) {
        MiuiFreeFormActivityStack mffas = getMiuiFreeFormActivityStackForMiuiFB(taskId);
        if (mffas == null) {
            return;
        }
        mffas.mUserId = userId;
    }

    public void hideCallingTaskIfAddSpecificChild(WindowContainer child) {
        if (child != null && child.asActivityRecord() != null && child.asActivityRecord().getTask() != null && child.asActivityRecord().intent != null && child.asActivityRecord().intent.getComponent() != null && MiuiMultiWindowAdapter.SHOW_HIDDEN_TASK_IF_FINISHED_WHITE_LIST_ACTIVITY.contains(child.asActivityRecord().intent.getComponent().getClassName())) {
            for (MiuiFreeFormActivityStack ffas : this.mFreeFormActivityStacks.values()) {
                if (ffas != null && ffas.mTask != null && ffas.mTask.intent != null && ffas.mTask.intent.getComponent() != null && ffas.mTask.intent.getComponent().getPackageName() != null && ffas.mTask.intent.getComponent().getPackageName().equals(child.asActivityRecord().getTask().mCallingPackage) && ffas.mTask.getResumedActivity() != null && ffas.mTask.getResumedActivity().intent != null && ffas.mTask.getResumedActivity().intent.getComponent() != null && MiuiMultiWindowAdapter.HIDE_SELF_IF_NEW_FREEFORM_TASK_WHITE_LIST_ACTIVITY.contains(ffas.mTask.getResumedActivity().intent.getComponent().getClassName())) {
                    ffas.mTask.getSyncTransaction().hide(ffas.mTask.getSurfaceControl());
                }
            }
        }
    }

    public void showHiddenTaskIfRemoveSpecificChild(WindowContainer child) {
        if (child != null && child.asActivityRecord() != null && child.asActivityRecord().getTask() != null && child.asActivityRecord().intent != null && child.asActivityRecord().intent.getComponent() != null && MiuiMultiWindowAdapter.SHOW_HIDDEN_TASK_IF_FINISHED_WHITE_LIST_ACTIVITY.contains(child.asActivityRecord().intent.getComponent().getClassName())) {
            for (MiuiFreeFormActivityStack ffas : this.mFreeFormActivityStacks.values()) {
                if (ffas != null && ffas.mTask != null && ffas.mTask.intent != null && ffas.mTask.intent.getComponent() != null && ffas.mTask.intent.getComponent().getPackageName() != null && ffas.mTask.intent.getComponent().getPackageName().equals(child.asActivityRecord().getTask().mCallingPackage) && ffas.mTask.getTopNonFinishingActivity() != null && ffas.mTask.getTopNonFinishingActivity().intent != null && ffas.mTask.getTopNonFinishingActivity().intent.getComponent() != null && MiuiMultiWindowAdapter.HIDE_SELF_IF_NEW_FREEFORM_TASK_WHITE_LIST_ACTIVITY.contains(ffas.mTask.getTopNonFinishingActivity().intent.getComponent().getClassName())) {
                    ffas.mTask.getSyncTransaction().show(ffas.mTask.getSurfaceControl());
                }
            }
        }
    }

    public void showOpenMiuiOptimizationToast() {
        this.mFreeFormStackDisplayStrategy.showOpenMiuiOptimizationToast();
    }

    public void startFullscreenFromFreeform(boolean isBroadCast, MiuiFreeFormActivityStackStub mffas) {
        MiuiFreeFormGestureController miuiFreeFormGestureController = (MiuiFreeFormGestureController) this.mActivityTaskManagerService.mWindowManager.mMiuiFreeFormGestureController;
        miuiFreeFormGestureController.startFullscreenFromFreeform(isBroadCast, mffas, null);
    }
}
