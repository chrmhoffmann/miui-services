package com.android.server.wm;

import android.graphics.Rect;
import android.graphics.Region;
import android.os.IBinder;
import android.view.InsetsState;
import android.view.WindowManager;
import com.android.server.inputmethod.InputMethodManagerServiceImpl;
import com.miui.base.MiuiStubRegistry;
/* loaded from: classes.dex */
public class WindowStateStubImpl extends WindowStateStub {

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<WindowStateStubImpl> {

        /* compiled from: WindowStateStubImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final WindowStateStubImpl INSTANCE = new WindowStateStubImpl();
        }

        public WindowStateStubImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public WindowStateStubImpl provideNewInstance() {
            return new WindowStateStubImpl();
        }
    }

    public boolean isMiuiLayoutInCutoutAlways(WindowManager.LayoutParams attrs) {
        return attrs != null && attrs.miuiAlwaysDisplayInCutout;
    }

    public boolean notifyNonAppSurfaceVisibilityChanged(String pkgName, int type) {
        return InputMethodManagerServiceImpl.MIUI_HOME.equals(pkgName) && type == 4;
    }

    public boolean isStatusBarForceBlackWindow(WindowManager.LayoutParams attrs) {
        return "ForceBlack".equals(attrs.getTitle()) && "com.android.systemui".equals(attrs.packageName);
    }

    public static void adjuestScaleAndFrame(WindowState win, Task task) {
    }

    public static void adjuestFrameForChild(WindowState win) {
        Task task;
        if (!win.isChildWindow() && (task = win.getTask()) != null) {
            Rect rect = task.getBounds();
            if (rect.left < win.mWindowFrames.mFrame.left) {
                int relativeLeft = win.mWindowFrames.mFrame.left - rect.left;
                int weightOffset = relativeLeft - ((int) (relativeLeft * win.mGlobalScale));
                win.mWindowFrames.mFrame.left -= weightOffset;
                win.mWindowFrames.mFrame.right -= weightOffset;
            }
            if (rect.top < win.mWindowFrames.mFrame.top) {
                int relativeTop = win.mWindowFrames.mFrame.top - rect.top;
                int heightOffset = relativeTop - ((int) (relativeTop * win.mGlobalScale));
                win.mWindowFrames.mFrame.top -= heightOffset;
                win.mWindowFrames.mFrame.bottom -= heightOffset;
            }
        }
    }

    public static void adjuestFreeFormTouchRegion(WindowState win, Region outRegion) {
    }

    public static WindowState getWinStateFromInputWinMap(WindowManagerService windowManagerService, IBinder token) {
        WindowState windowState;
        synchronized (windowManagerService.mGlobalLock) {
            windowState = (WindowState) windowManagerService.mInputToWindowMap.get(token);
        }
        return windowState;
    }

    public IMiuiWindowStateEx getMiuiWindowStateEx(WindowManagerService service, Object w) {
        return new MiuiWindowStateEx(service, w);
    }

    public InsetsState adjustInsetsForSplit(WindowState ws, InsetsState insets) {
        if (ws == null || insets == null || !ActivityTaskManagerServiceImpl.getInstance().isVerticalSplit()) {
            return insets;
        }
        if (ws.inSplitScreenWindowingMode() && ws.mActivityRecord != null && !ws.mActivityRecord.mImeInsetsFrozenUntilStartInput) {
            boolean removeIme = false;
            InsetsControlTarget imeTarget = ws.getDisplayContent().getImeTarget(0);
            if (imeTarget != null && imeTarget.getWindow() != null && ws.getTask() != imeTarget.getWindow().getTask()) {
                removeIme = true;
            }
            if (removeIme) {
                insets.removeSource(19);
            }
        }
        return insets;
    }
}
