package com.android.server.wm;

import android.graphics.Rect;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.view.InsetsSource;
import android.view.InsetsState;
import android.view.WindowInsets;
import com.android.server.padkeyboard.MiuiPadKeyboardManager;
import com.android.server.policy.BaseMiuiPhoneWindowManager;
import com.android.server.policy.WindowManagerPolicy;
import com.miui.base.MiuiStubRegistry;
import com.miui.daemon.performance.PerfShielderManager;
import com.miui.server.input.AutoDisableScreenButtonsManager;
import com.miui.server.input.knock.MiuiKnockGestureService;
import java.util.Iterator;
import miui.hardware.display.DisplayFeatureManager;
import miui.os.DeviceFeature;
import miui.util.ReflectionUtils;
/* loaded from: classes.dex */
public class DisplayContentStubImpl implements DisplayContentStub {
    private static final String DESCRIPTOR = "miui.systemui.keyguard.Wallpaper";
    private static final int FPS_COMMON = 60;
    private static final int SCREEN_DPI_MODE = 24;
    private boolean mStatusBarVisible = true;
    public static int sLastUserRefreshRate = -1;
    public static int sCurrentRefreshRate = -1;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<DisplayContentStubImpl> {

        /* compiled from: DisplayContentStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final DisplayContentStubImpl INSTANCE = new DisplayContentStubImpl();
        }

        public DisplayContentStubImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public DisplayContentStubImpl provideNewInstance() {
            return new DisplayContentStubImpl();
        }
    }

    static int getFullScreenIndex(Task stack, WindowList<Task> children, int targetPosition) {
        if (stack.getWindowingMode() == 1) {
            Iterator it = children.iterator();
            while (it.hasNext()) {
                Task tStack = (Task) it.next();
                if (tStack.getWindowingMode() == 5 && children.indexOf(tStack) > 0) {
                    return children.indexOf(tStack) - 1;
                }
            }
            return targetPosition;
        }
        return targetPosition;
    }

    static int getFullScreenIndex(boolean toTop, Task stack, WindowList<Task> children, int targetPosition, boolean adding) {
        if (toTop && stack.getWindowingMode() == 1) {
            Iterator it = children.iterator();
            while (it.hasNext()) {
                Task tStack = (Task) it.next();
                if (tStack.getWindowingMode() == 5) {
                    int topChildPosition = children.indexOf(tStack);
                    return adding ? topChildPosition : topChildPosition > 0 ? topChildPosition - 1 : 0;
                }
            }
            return targetPosition;
        }
        return targetPosition;
    }

    static void updateRefreshRateIfNeed(boolean isInMultiWindow) {
        int currentFps = getCurrentRefreshRate();
        if (sCurrentRefreshRate != currentFps) {
            sCurrentRefreshRate = currentFps;
        }
        if (isInMultiWindow) {
            int i = sCurrentRefreshRate;
            if (i > 60) {
                sLastUserRefreshRate = i;
                setCurrentRefreshRate(60);
                return;
            }
            return;
        }
        int i2 = sLastUserRefreshRate;
        if (i2 >= 60) {
            setCurrentRefreshRate(i2);
            sLastUserRefreshRate = -1;
        }
    }

    private static int getCurrentRefreshRate() {
        int fps = SystemProperties.getInt("persist.vendor.dfps.level", 60);
        int powerFps = SystemProperties.getInt("persist.vendor.power.dfps.level", 0);
        if (powerFps != 0) {
            return powerFps;
        }
        return fps;
    }

    private static void setCurrentRefreshRate(int fps) {
        sCurrentRefreshRate = fps;
        DisplayFeatureManager.getInstance().setScreenEffect(24, fps);
    }

    public int compare(WindowToken token1, WindowToken token2) {
        try {
            if (token1.windowType == token2.windowType && token1.windowType == 2013) {
                if (token1.token != null && DESCRIPTOR.equals(token1.token.getInterfaceDescriptor())) {
                    return 1;
                }
                if (token2.token != null) {
                    if (DESCRIPTOR.equals(token2.token.getInterfaceDescriptor())) {
                        return -1;
                    }
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        WindowManagerService wmService = WindowManagerServiceImpl.getInstance().mWmService;
        return wmService.mPolicy.getWindowLayerFromTypeLw(token1.windowType, token1.mOwnerCanManageAppTokens) < wmService.mPolicy.getWindowLayerFromTypeLw(token2.windowType, token2.mOwnerCanManageAppTokens) ? -1 : 1;
    }

    public void finishLayoutLw(WindowManagerPolicy policy, DisplayContent displayContent, DisplayFrames displayFrames) {
        if (policy instanceof BaseMiuiPhoneWindowManager) {
            Rect inputMethodWindowRegion = getInputMethodWindowVisibleRegion(displayContent);
            ((BaseMiuiPhoneWindowManager) policy).finishLayoutLw(displayFrames, inputMethodWindowRegion);
            WindowState statusBar = displayContent.getDisplayPolicy().getStatusBar();
            if (statusBar != null && this.mStatusBarVisible != statusBar.isVisible()) {
                boolean isVisible = statusBar.isVisible();
                this.mStatusBarVisible = isVisible;
                AutoDisableScreenButtonsManager.onStatusBarVisibilityChangeStatic(isVisible);
                MiuiKnockGestureService.finishPostLayoutPolicyLw(this.mStatusBarVisible);
            }
        }
    }

    private Rect getInputMethodWindowVisibleRegion(DisplayContent displayContent) {
        InsetsState state = displayContent.getInsetsStateController().getRawInsetsState();
        InsetsSource imeSource = state.peekSource(19);
        if (imeSource == null || !imeSource.isVisible()) {
            return new Rect(0, 0, 0, 0);
        }
        Rect imeFrame = imeSource.getVisibleFrame() != null ? imeSource.getVisibleFrame() : imeSource.getFrame();
        Rect dockFrame = (Rect) ReflectionUtils.tryGetObjectField(displayContent, "mTmpRect", Rect.class).get();
        dockFrame.set(state.getDisplayFrame());
        dockFrame.inset(state.calculateInsets(dockFrame, WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout(), false));
        return new Rect(dockFrame.left, imeFrame.top, dockFrame.right, dockFrame.bottom);
    }

    public boolean isSubDisplayOff(DisplayContent display) {
        return display != null && DeviceFeature.IS_SUBSCREEN_DEVICE && display.getDisplayId() == 2 && display.getDisplay().getState() == 1;
    }

    public boolean isSubDisplay(int displayId) {
        return DeviceFeature.IS_SUBSCREEN_DEVICE && displayId == 2;
    }

    public void reportFocusChanged(WindowState currentWindow, WindowState newWindow) {
        String str = null;
        String str2 = currentWindow != null ? currentWindow.mAttrs.packageName : null;
        String str3 = newWindow != null ? newWindow.mAttrs.packageName : null;
        String charSequence = currentWindow != null ? currentWindow.mAttrs.getTitle().toString() : null;
        if (newWindow != null) {
            str = newWindow.mAttrs.getTitle().toString();
        }
        PerfShielderManager.reportFocusChanged(str2, str3, charSequence, str);
    }

    public boolean attachToDisplayCompatMode(WindowState mImeLayeringTarget) {
        Task task;
        if (mImeLayeringTarget == null || mImeLayeringTarget.mActivityRecord == null || (task = mImeLayeringTarget.mActivityRecord.getTask()) == null || !task.mDisplayCompatAvailable) {
            return false;
        }
        return true;
    }

    public boolean needEnsureVisible(DisplayContent dc, Task task) {
        return dc == null || task == null || task.affinity == null || !isSubDisplayOff(dc) || !task.affinity.contains("com.xiaomi.misubscreenui");
    }

    public boolean shouldInterceptRelaunch(int vendorId, int productId) {
        return MiuiPadKeyboardManager.isXiaomiKeyboard(vendorId, productId);
    }

    public boolean isSupportedImeSnapShot() {
        return DeviceFeature.SUPPORT_IME_SNAPSHOT;
    }

    public boolean skipImeWindowsForMiui(DisplayContent display) {
        if (!ActivityTaskManagerServiceImpl.getInstance().isVerticalSplit()) {
            return true;
        }
        return !display.mAtmService.isInSplitScreenWindowingMode();
    }
}
