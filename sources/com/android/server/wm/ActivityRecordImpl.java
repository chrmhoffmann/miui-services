package com.android.server.wm;

import android.app.AppOpsManager;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.os.SystemProperties;
import android.server.am.SplitScreenReporter;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.MiuiSplitUtils;
import android.util.Slog;
import android.window.TaskSnapshot;
import com.android.server.padkeyboard.MiuiPadKeyboardManager;
import com.miui.base.MiuiStubRegistry;
import com.miui.server.smartpower.SmartPowerPolicyManager;
import miui.os.DeviceFeature;
import miui.os.MiuiInit;
/* loaded from: classes.dex */
public class ActivityRecordImpl implements ActivityRecordStub {
    private static final ArraySet<String> BLACK_LIST_NOT_ALLOWED_SPLASHSCREEN;
    public static final String PERMISSION_ACTIVITY = "com.android.packageinstaller.permission.ui.GrantPermissionsActivity";
    private static final String TAG = "ActivityRecordImpl";

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<ActivityRecordImpl> {

        /* compiled from: ActivityRecordImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final ActivityRecordImpl INSTANCE = new ActivityRecordImpl();
        }

        public ActivityRecordImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public ActivityRecordImpl provideNewInstance() {
            return new ActivityRecordImpl();
        }
    }

    static {
        ArraySet<String> arraySet = new ArraySet<>();
        BLACK_LIST_NOT_ALLOWED_SPLASHSCREEN = arraySet;
        arraySet.add("com.iflytek.inputmethod.miui/com.iflytek.inputmethod.LauncherSettingsActivity");
        arraySet.add("com.android.camera/.AssistantCamera");
        arraySet.add("com.tencent.mm/.plugin.voip.ui.VideoActivity");
        arraySet.add("com.miui.securitycenter/com.miui.applicationlock.ConfirmAccessControl");
        arraySet.add("com.android.deskclock/.alarm.alert.AlarmAlertFullScreenActivity");
        arraySet.add("tv.danmaku.bili/com.bilibili.video.story.StoryVideoActivity");
        arraySet.add("tv.danmaku.bili/com.bilibili.video.videodetail.VideoDetailsActivity");
    }

    static ActivityRecordImpl getInstance() {
        return (ActivityRecordImpl) ActivityRecordStub.get();
    }

    public boolean canShowWhenLocked(AppOpsManager appOpsManager, int uid, String packageName) {
        int mode = appOpsManager.noteOpNoThrow(10020, uid, packageName, (String) null, "ActivityRecordImpl#canShowWhenLocked");
        if (mode != 0) {
            Slog.i(TAG, "MIUILOG- Show when locked PermissionDenied pkg : " + packageName + " uid : " + uid);
            return false;
        }
        return true;
    }

    public void notifyAppResumedFinished(ActivityRecord r) {
        if (r.mChildren.isEmpty()) {
            r.getDisplayContent().mUnknownAppVisibilityController.appRemovedOrHidden(r);
        } else {
            r.getDisplayContent().mUnknownAppVisibilityController.notifyAppResumedFinished(r);
        }
    }

    public boolean shouldNotBeResume(ActivityRecord r) {
        return r != null && r.getDisplayContent() != null && DeviceFeature.IS_SUBSCREEN_DEVICE && r.getDisplayId() == 2 && r.getDisplayContent().isSleeping() && TextUtils.equals(r.packageName, "com.xiaomi.misubscreenui");
    }

    public boolean allowTaskSnapshot(String pkg, boolean allowTaskSnapshot, TaskSnapshot snapshot, ActivityRecord mActivity) {
        if (!AppTransitionInjector.disableSnapshot(pkg) && !AppTransitionInjector.disableSnapshotForApplock(pkg, mActivity.getUid()) && !AppTransitionInjector.disableSnapshotByComponent(mActivity)) {
            if (snapshot == null) {
                return allowTaskSnapshot;
            }
            if (snapshot.getWindowingMode() == 5 && !mActivity.inFreeformWindowingMode()) {
                return false;
            }
            WindowState mainWin = mActivity.findMainWindow();
            if ((mainWin != null && mainWin.isSecureLocked()) || mActivity.isLetterboxedForFixedOrientationAndAspectRatio()) {
                return false;
            }
            if (mActivity.getTask() == null) {
                return allowTaskSnapshot;
            }
            int width = mActivity.getTask().getBounds().width();
            int height = mActivity.getTask().getBounds().height();
            if (mActivity.getDisplayContent().getRotation() == snapshot.getRotation() && ((width != snapshot.getSnapshot().getWidth() || height != snapshot.getSnapshot().getHeight()) && !mActivity.inFreeformWindowingMode())) {
                Slog.d(TAG, "Task bounds and snapshot don't match. task width=" + width + ", height=" + height + ": snapshot w=" + snapshot.getSnapshot().getWidth() + ", h=" + snapshot.getSnapshot().getHeight() + ", name = " + mActivity);
                return false;
            }
            return allowTaskSnapshot;
        }
        return false;
    }

    public boolean disableSplashScreen(String componentName) {
        if (BLACK_LIST_NOT_ALLOWED_SPLASHSCREEN.contains(componentName)) {
            return true;
        }
        return false;
    }

    public boolean allowShowSlpha(String pkg) {
        if (AppTransitionInjector.ignoreLaunchedFromSystemSurface(pkg)) {
            return false;
        }
        return true;
    }

    public boolean miuiFullscreen(String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return false;
        }
        return !MiuiInit.isRestrictAspect(packageName);
    }

    public boolean isCTS() {
        return !SystemProperties.getBoolean("persist.sys.miui_optimization", !SplitScreenReporter.ACTION_ENTER_SPLIT.equals(SystemProperties.get("ro.miui.cts")));
    }

    public boolean isMiuiMwAnimationBelowStack(ActivityRecord activity) {
        return false;
    }

    public String getClassName(ActivityRecord r) {
        if (r != null && r.intent != null && r.intent.getComponent() != null) {
            return r.intent.getComponent().getClassName();
        }
        return null;
    }

    public boolean initSplitMode(ActivityRecord ar, Intent intent) {
        if (MiuiSplitUtils.isInUnsplittableList(ar.packageName, ar.mActivityComponent.getClassName())) {
            intent.addMiuiFlags(8);
        }
        boolean isSplitMode = (intent.getMiuiFlags() & 4) != 0 && (intent.getMiuiFlags() & 8) == 0;
        if (isSplitMode) {
            if (ar.occludesParent(true)) {
                ar.setOccludesParent(ar.mAtmService.mWindowManager.mPolicy.isDisplayFolded());
                return isSplitMode;
            } else if ((intent.getMiuiFlags() & 16) == 0) {
                intent.addMiuiFlags(8);
                return false;
            } else {
                return isSplitMode;
            }
        }
        return isSplitMode;
    }

    public boolean isSecSplit(boolean isSplitMode, Intent intent) {
        return isSplitMode && (intent.getMiuiFlags() & 64) != 0;
    }

    public boolean isWorldCirculate(Task task) {
        if (task == null) {
            return false;
        }
        int displayId = task.getDisplayId();
        return displayId != 0 && "com.xiaomi.mirror".equals(getOwnerPackageName(task));
    }

    public String getOwnerPackageName(Task task) {
        DisplayContent display;
        ActivityRecord resumedActivity = task.getResumedActivity();
        if (resumedActivity != null && (display = resumedActivity.mDisplayContent) != null) {
            String ownerPackageName = display.mDisplayInfo.ownerPackageName;
            Slog.d(TAG, "displayId = " + task.getDisplayId() + " ownerPackageName = " + ownerPackageName);
            return ownerPackageName;
        }
        return null;
    }

    public void updateSpaceToFill(boolean isEmbedded, Rect spaceToFill, Rect windowFrame) {
        if (isEmbedded) {
            spaceToFill.set(spaceToFill.left, windowFrame.top, spaceToFill.right, windowFrame.bottom);
        }
    }

    public void onWindowsVisible(ActivityRecord ar) {
        ActivityTaskManagerServiceImpl atmsImpl = ActivityTaskManagerServiceImpl.getInstance();
        if (atmsImpl != null && atmsImpl.getMiuiSizeCompatIn() != null) {
            atmsImpl.getMiuiSizeCompatIn().showWarningNotification(ar);
        }
    }

    public boolean shouldUseCompatMode() {
        ActivityTaskManagerServiceImpl atmsImpl;
        return !Build.IS_MIUI || (atmsImpl = ActivityTaskManagerServiceImpl.getInstance()) == null || !atmsImpl.mAtmService.isInSplitScreenWindowingMode() || miui.os.Build.IS_TABLET;
    }

    public void updateLatterboxParam(boolean isEmbedded, Point surfaceOrigin) {
        if (isEmbedded) {
            surfaceOrigin.set(0, 0);
        }
    }

    public int shouldClearActivityInfoFlags() {
        return MiuiPadKeyboardManager.shouldClearActivityInfoFlags();
    }

    public int addThemeFlag(String packageName) {
        if ("com.android.settings".equals(packageName)) {
            return SmartPowerPolicyManager.WHITE_LIST_TYPE_PROVIDER_CLOUDCONTROL;
        }
        return 0;
    }
}
