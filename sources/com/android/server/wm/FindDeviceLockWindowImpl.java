package com.android.server.wm;

import android.view.WindowManager;
import com.android.internal.util.ToBooleanFunction;
import com.miui.base.MiuiStubRegistry;
/* loaded from: classes.dex */
public class FindDeviceLockWindowImpl implements FindDeviceLockWindowStub {
    private static final String TAG = "FindDeviceLockWindowImpl";
    private static WindowState sTmpFirstAppWindow;
    private static WindowState sTmpLockWindow;

    /* loaded from: classes.dex */
    private enum LockDeviceWindowPolicy {
        HIDE,
        SHOW
    }

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<FindDeviceLockWindowImpl> {

        /* compiled from: FindDeviceLockWindowImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final FindDeviceLockWindowImpl INSTANCE = new FindDeviceLockWindowImpl();
        }

        public FindDeviceLockWindowImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public FindDeviceLockWindowImpl provideNewInstance() {
            return new FindDeviceLockWindowImpl();
        }
    }

    public void updateLockDeviceWindowLocked(WindowManagerService wms, DisplayContent dc) {
        if (wms == null || dc == null) {
            return;
        }
        try {
            dc.forAllWindows(new ToBooleanFunction<WindowState>() { // from class: com.android.server.wm.FindDeviceLockWindowImpl.1
                public boolean apply(WindowState win) {
                    if (win == null || win.mAttrs == null) {
                        return false;
                    }
                    int type = win.mAttrs.type;
                    int extraFlags = win.mAttrs.extraFlags;
                    if ((extraFlags & 2048) != 0) {
                        FindDeviceLockWindowImpl.sTmpLockWindow = win;
                    } else if (type >= 1 && type < 2000 && win.getParentWindow() == null) {
                        if (FindDeviceLockWindowImpl.sTmpFirstAppWindow == null || (FindDeviceLockWindowImpl.sTmpFirstAppWindow.mActivityRecord != null && FindDeviceLockWindowImpl.sTmpFirstAppWindow.mActivityRecord.equals(win.mActivityRecord))) {
                            FindDeviceLockWindowImpl.sTmpFirstAppWindow = win;
                        } else if (FindDeviceLockWindowImpl.sTmpLockWindow != null) {
                            return true;
                        }
                    }
                    return false;
                }
            }, true);
            if (sTmpLockWindow == null) {
                return;
            }
            boolean hideLockWindow = false;
            WindowState windowState = sTmpFirstAppWindow;
            if (windowState != null && (windowState.mAttrs.extraFlags & 4096) != 0 && sTmpFirstAppWindow.isVisible() && sTmpFirstAppWindow.isDrawn()) {
                WindowState windowState2 = sTmpFirstAppWindow;
                if (isObscuringFullScreen(windowState2, windowState2.mAttrs)) {
                    hideLockWindow = true;
                }
            }
            boolean change = hideLockWindow ? sTmpLockWindow.hide(false, false) : sTmpLockWindow.show(false, false);
            if (change) {
                wms.mFocusMayChange = true;
            }
        } finally {
            sTmpLockWindow = null;
            sTmpFirstAppWindow = null;
        }
    }

    private static boolean isObscuringFullScreen(WindowState win, WindowManager.LayoutParams params) {
        return win != null && params != null && win.isObscuringDisplay() && params.x == 0 && params.y == 0 && params.width == -1 && params.height == -1;
    }
}
