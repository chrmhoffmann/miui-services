package com.android.server.wm;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.graphics.Region;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemProperties;
import android.server.am.SplitScreenReporter;
import android.util.MiuiMultiWindowAdapter;
import android.util.MiuiMultiWindowUtils;
import android.view.WindowManager;
import android.view.animation.Animation;
import com.android.server.wm.ActivityRecord;
import com.miui.base.MiuiStubRegistry;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
/* loaded from: classes.dex */
public class MiuiFreeformServiceImpl implements MiuiFreeformServiceStub {
    public static final String UNKNOWN = "unknown1";

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MiuiFreeformServiceImpl> {

        /* compiled from: MiuiFreeformServiceImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MiuiFreeformServiceImpl INSTANCE = new MiuiFreeformServiceImpl();
        }

        public MiuiFreeformServiceImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public MiuiFreeformServiceImpl provideNewInstance() {
            return new MiuiFreeformServiceImpl();
        }
    }

    public void updateApplicationConfiguration(ActivityTaskSupervisor stackSupervisor, Configuration globalConfiguration, String packageName) {
        ActivityTaskSupervisorImpl.updateApplicationConfiguration(stackSupervisor, globalConfiguration, packageName);
    }

    public void setFreeformBlackList(List<String> blackList) {
        MiuiMultiWindowAdapter.setFreeformBlackList(blackList);
    }

    public List<String> getFreeformBlackList() {
        return MiuiMultiWindowAdapter.getFreeformBlackList();
    }

    public List<String> getFreeformBlackList(Context context, HashMap<String, LinkedList<Long>> allTimestamps, boolean isNeedUpdateList) {
        return MiuiMultiWindowUtils.getFreeformBlackList(context, allTimestamps, isNeedUpdateList);
    }

    public void setFreeformVideoWhiteList(List<String> whiteList) {
        MiuiMultiWindowAdapter.setFreeformVideoWhiteList(whiteList);
    }

    public List<String> getFreeformVideoWhiteList() {
        return MiuiMultiWindowAdapter.getFreeformVideoWhiteList();
    }

    public void setFreeformDisableOverlayList(List<String> disableOverlayList) {
        MiuiMultiWindowAdapter.setFreeformDisableOverlayList(disableOverlayList);
    }

    public List<String> getFreeformDisableOverlayList() {
        return MiuiMultiWindowAdapter.getFreeformDisableOverlayList();
    }

    public void setFreeformIgnoreRequestOrientationList(List<String> ignoreRequestOrientationList) {
        MiuiMultiWindowAdapter.setFreeformIgnoreRequestOrientationList(ignoreRequestOrientationList);
    }

    public List<String> getFreeformIgnoreRequestOrientationList() {
        return MiuiMultiWindowAdapter.getFreeformIgnoreRequestOrientationList();
    }

    public void setFreeformNeedRelunchList(List<String> needRelunchList) {
        MiuiMultiWindowAdapter.setFreeformNeedRelunchList(needRelunchList);
    }

    public List<String> getFreeformNeedRelunchList() {
        return MiuiMultiWindowAdapter.getFreeformNeedRelunchList();
    }

    public void setStartFromFreeformBlackList(List<String> startFromFreeformBlackList) {
        MiuiMultiWindowAdapter.setStartFromFreeformBlackList(startFromFreeformBlackList);
    }

    public List<String> getStartFromFreeformBlackList() {
        return MiuiMultiWindowAdapter.getStartFromFreeformBlackList();
    }

    public void setHideSelfIfNewFreeformTaskWhiteList(List<String> hideSelfIfNewFreeformTaskWhiteList) {
        MiuiMultiWindowAdapter.setHideSelfIfNewFreeformTaskWhiteList(hideSelfIfNewFreeformTaskWhiteList);
    }

    public List<String> getHideSelfIfNewFreeformTaskWhiteList() {
        return MiuiMultiWindowAdapter.getHideSelfIfNewFreeformTaskWhiteList();
    }

    public void setShowHiddenTaskIfFinishedWhiteList(List<String> showHiddenTaskIfFinishedWhiteList) {
        MiuiMultiWindowAdapter.setShowHiddenTaskIfFinishedWhiteList(showHiddenTaskIfFinishedWhiteList);
    }

    public List<String> getShowHiddenTaskIfFinishedWhiteList() {
        return MiuiMultiWindowAdapter.getShowHiddenTaskIfFinishedWhiteList();
    }

    public void setFreeformResizeableWhiteList(List<String> resizeableWhiteList) {
        MiuiMultiWindowAdapter.setFreeformResizeableWhiteList(resizeableWhiteList);
    }

    public List<String> getFreeformResizeableWhiteList() {
        return MiuiMultiWindowAdapter.getFreeformResizeableWhiteList();
    }

    public void setApplicationLockActivityList(List<String> applicationLockActivityList) {
        MiuiMultiWindowAdapter.setApplicationLockActivityList(applicationLockActivityList);
    }

    public List<String> getApplicationLockActivityList() {
        return MiuiMultiWindowAdapter.getApplicationLockActivityList();
    }

    public void setFreeformCaptionInsetsHeightToZeroList(List<String> freeformCaptionInsetsHeightToZeroList) {
        MiuiMultiWindowAdapter.setFreeformCaptionInsetsHeightToZeroList(freeformCaptionInsetsHeightToZeroList);
    }

    public List<String> getFreeformCaptionInsetsHeightToZeroList() {
        return MiuiMultiWindowAdapter.getFreeformCaptionInsetsHeightToZeroList();
    }

    public void setForceLandscapeApplication(List<String> forceLandscapeApplication) {
        MiuiMultiWindowAdapter.setForceLandscapeApplication(forceLandscapeApplication);
    }

    public List<String> getForceLandscapeApplication() {
        return MiuiMultiWindowAdapter.getForceLandscapeApplication();
    }

    public void setTopGameList(List<String> topGameList) {
        MiuiMultiWindowAdapter.setTopGameList(topGameList);
    }

    public List<String> getTopGameList() {
        return MiuiMultiWindowAdapter.getTopGameList();
    }

    public void setTopVideoList(List<String> topVideoList) {
        MiuiMultiWindowAdapter.setTopVideoList(topVideoList);
    }

    public List<String> getTopVideoList() {
        return MiuiMultiWindowAdapter.getTopVideoList();
    }

    public void setFixedRotationAppList(List<String> fixedRotationAppList) {
        MiuiMultiWindowAdapter.setFixedRotationAppList(fixedRotationAppList);
    }

    public List<String> getFixedRotationAppList() {
        return MiuiMultiWindowAdapter.getFixedRotationAppList();
    }

    public void setRotationFromDisplayApp(List<String> rotationFromDisplayApp) {
        MiuiMultiWindowAdapter.setRotationFromDisplayApp(rotationFromDisplayApp);
    }

    public List<String> getRotationFromDisplayApp() {
        return MiuiMultiWindowAdapter.getRotationFromDisplayApp();
    }

    public void setUseDefaultCameraPipelineApp(List<String> useDefaultCameraPipelineApp) {
        MiuiMultiWindowAdapter.setUseDefaultCameraPipelineApp(useDefaultCameraPipelineApp);
    }

    public List<String> getUseDefaultCameraPipelineApp() {
        return MiuiMultiWindowAdapter.getUseDefaultCameraPipelineApp();
    }

    public void setSensorDisableWhiteList(List<String> sensorDisableWhiteList) {
        MiuiMultiWindowAdapter.setSensorDisableWhiteList(sensorDisableWhiteList);
    }

    public List<String> getSensorDisableWhiteList() {
        return MiuiMultiWindowAdapter.getSensorDisableWhiteList();
    }

    public void setLaunchInTaskList(List<String> launchInTaskList) {
        MiuiMultiWindowAdapter.setLaunchInTaskList(launchInTaskList);
    }

    public List<String> getLaunchInTaskList() {
        return MiuiMultiWindowAdapter.getLaunchInTaskList();
    }

    public void setSensorDisableList(List<String> sensorDisableList) {
        MiuiMultiWindowAdapter.setSensorDisableList(sensorDisableList);
    }

    public List<String> getSensorDisableList() {
        return MiuiMultiWindowAdapter.getSensorDisableList();
    }

    public boolean supportsFreeform() {
        return ActivityTaskSupervisorImpl.supportsFreeform();
    }

    public boolean notNeedRelunchFreeform(String packageName, Configuration tempConfig, Configuration fullConfig) {
        return MiuiMultiWindowAdapter.notNeedRelunchFreeform(packageName, tempConfig, fullConfig);
    }

    public Rect getFreeformRect(Context context, boolean needDisplayContentRotation, boolean isVertical, boolean isMiniFreeformMode, boolean isFreeformLandscape, Rect outBounds) {
        return MiuiMultiWindowUtils.getFreeformRect(context, needDisplayContentRotation, isVertical, isMiniFreeformMode, isFreeformLandscape, outBounds);
    }

    public ActivityOptions modifyLaunchActivityOptionIfNeed(ActivityTaskManagerService service, RootWindowContainer root, String callingPackgae, ActivityOptions options, WindowProcessController callerApp, Intent intent, int userId, ActivityInfo aInfo, ActivityRecord sourceRecord) {
        return ActivityStarterInjector.modifyLaunchActivityOptionIfNeed(service, root, callingPackgae, options, callerApp, intent, userId, aInfo, sourceRecord);
    }

    public void checkFreeformSupport(ActivityTaskManagerService service, ActivityOptions options) {
        ActivityStarterInjector.checkFreeformSupport(service, options);
    }

    public boolean isForceResizeable() {
        return MiuiMultiWindowUtils.isForceResizeable();
    }

    public int handleFreeformModeRequst(IBinder token, int cmd, Context mContext) {
        return ActivityTaskManagerServiceImpl.handleFreeformModeRequst(token, cmd, mContext);
    }

    public Animation loadFreeFormAnimation(WindowManagerService service, int transit, boolean enter, Rect frame, WindowContainer container) {
        return AppTransitionInjector.loadFreeFormAnimation(service, transit, enter, frame, container);
    }

    public float getMiuiMultiWindowUtilsScale() {
        return MiuiMultiWindowUtils.sScale;
    }

    public void setMiuiMultiWindowUtilsScale(float scale) {
        MiuiMultiWindowUtils.sScale = scale;
    }

    public boolean isOrientationLandscape(int orientation) {
        return MiuiMultiWindowUtils.isOrientationLandscape(orientation);
    }

    public int getTopDecorCaptionViewHeight() {
        return MiuiMultiWindowUtils.TOP_DECOR_CAPTIONVIEW_HEIGHT;
    }

    public float getFreeformRoundCorner() {
        return MiuiMultiWindowUtils.FREEFORM_ROUND_CORNER;
    }

    public void adjuestScaleAndFrame(WindowState win, Task task) {
        WindowStateStubImpl.adjuestScaleAndFrame(win, task);
    }

    public void adjuestFrameForChild(WindowState win) {
        WindowStateStubImpl.adjuestFrameForChild(win);
    }

    public void adjuestFreeFormTouchRegion(WindowState win, Region outRegion) {
        WindowStateStubImpl.adjuestFreeFormTouchRegion(win, outRegion);
    }

    public void onForegroundWindowChanged(WindowProcessController app, ActivityInfo info, ActivityRecord record, ActivityRecord.State state) {
        ActivityTaskManagerServiceImpl.onForegroundWindowChanged(app, info, record, state);
    }

    public void modifySupportFreeform(ActivityTaskManagerService service) {
        service.mSupportsFreeformWindowManagement = SystemProperties.getBoolean("persist.sys.miui_optimization", !SplitScreenReporter.ACTION_ENTER_SPLIT.equals(SystemProperties.get("ro.miui.cts")));
    }

    public boolean isMiuiBuild() {
        return SystemProperties.getBoolean("persist.sys.miui_optimization", !SplitScreenReporter.ACTION_ENTER_SPLIT.equals(SystemProperties.get("ro.miui.cts")));
    }

    public boolean isUseFreeFormAnimation(int transit) {
        return AppTransitionInjector.isUseFreeFormAnimation(transit);
    }

    public boolean isMiuiCvwFeatureEnable() {
        return MiuiCvwGestureController.isMiuiCvwFeatureEnable();
    }

    public float getShadowRadius() {
        return 400.0f;
    }

    public int getHomeUid() {
        return MiuiFreeFormManagerService.mHomeUid;
    }

    public boolean getEnableKeepFreeformState() {
        return false;
    }

    public boolean isPadScreen(Context context) {
        return MiuiMultiWindowUtils.isPadScreen(context);
    }

    public boolean multiFreeFormSupported(Context context) {
        return MiuiMultiWindowUtils.multiFreeFormSupported(context);
    }

    public float getHotSpaceOffsite() {
        return MiuiMultiWindowUtils.HOT_SPACE_OFFSITE;
    }

    public boolean needRelunchFreeform(String packageName, Configuration tempConfig, Configuration fullConfig) {
        return MiuiMultiWindowAdapter.needRelunchFreeform(packageName, tempConfig, fullConfig);
    }

    public int getHotSpaceResizeOffsetPad() {
        return 33;
    }

    public int getHotSpaceTopCaptionUpwardsOffset() {
        return 22;
    }

    public float reviewFreeFormBounds(Rect currentBounds, Rect newBounds, float currentScale, Rect accessibleArea) {
        return MiuiMultiWindowUtils.reviewFreeFormBounds(currentBounds, newBounds, currentScale, accessibleArea);
    }

    public Rect getFreeFormAccessibleArea(Context context, DisplayContent displayContent) {
        if (displayContent == null) {
            return new Rect();
        }
        InsetsStateController insetsStateController = displayContent.getInsetsStateController();
        return MiuiMultiWindowUtils.getFreeFormAccessibleArea(context, displayContent.getRotation(), MiuiFreeFormGestureDetector.getStatusBarHeight(insetsStateController), MiuiFreeFormGestureDetector.getNavBarHeight(insetsStateController), MiuiFreeFormGestureDetector.getDisplayCutoutHeight(displayContent.mDisplayFrames));
    }

    public float getFreeformScale(ActivityOptions options) {
        Method method = MiuiMultiWindowUtils.isMethodExist(options, "getActivityOptionsInjector", new Object[0]);
        if (method != null) {
            try {
                return ((Float) MiuiMultiWindowUtils.invoke(method.invoke(options, new Object[0]), "getFreeformScale", new Object[0])).floatValue();
            } catch (Exception e) {
                e.printStackTrace();
                return 1.0f;
            }
        }
        return 1.0f;
    }

    public boolean setFreeformScale(float scale, ActivityOptions options) {
        Method method = MiuiMultiWindowUtils.isMethodExist(options, "getActivityOptionsInjector", new Object[0]);
        if (method != null) {
            try {
                return MiuiMultiWindowUtils.invoke(method.invoke(options, new Object[0]), "setFreeformScale", new Object[]{Float.valueOf(scale)}) != null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public boolean isMIUIProduct() {
        return Build.IS_MIUI;
    }

    public boolean isSupportMiuiShadow() {
        return MiuiMultiWindowUtils.isSupportMiuiShadow();
    }

    public boolean isFreeFormOverlayWindow(WindowManager.LayoutParams lp) {
        return (lp == null || (lp.extraFlags & 536870912) == 0) ? false : true;
    }
}
