package com.android.server.wm;

import android.util.Slog;
import android.view.DisplayInfo;
import android.view.WindowManager;
import com.miui.base.MiuiStubRegistry;
/* loaded from: classes.dex */
public class AppRTWmsImpl implements AppRTWmsStub {

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<AppRTWmsImpl> {

        /* compiled from: AppRTWmsImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final AppRTWmsImpl INSTANCE = new AppRTWmsImpl();
        }

        public AppRTWmsImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public AppRTWmsImpl provideNewInstance() {
            return new AppRTWmsImpl();
        }
    }

    public void setAppResolutionTunerSupport(boolean support) {
        getTunerList().setAppRTEnable(support);
    }

    public boolean isAppResolutionTunerSupport() {
        return getTunerList().isAppRTEnable();
    }

    public void setWindowScaleByWL(WindowState win, DisplayInfo displayInfo, WindowManager.LayoutParams attrs, int requestedWidth, int requestedHeight) {
        int width = displayInfo.logicalWidth;
        int height = displayInfo.logicalHeight;
        String windowName = null;
        String packageName = attrs != null ? attrs.packageName : null;
        if (attrs != null && attrs.getTitle() != null) {
            windowName = attrs.getTitle().toString();
        }
        if (attrs != null && packageName != null && windowName != null && !windowName.contains("FastStarting") && !windowName.contains("Splash Screen") && !windowName.contains("PopupWindow")) {
            if (((height == requestedHeight && width == requestedWidth) || (attrs.width == -1 && attrs.height == -1 && attrs.x == 0 && attrs.y == 0)) && getTunerList().isScaledByWMS(packageName, windowName)) {
                float scale = getTunerList().getScaleValue(packageName);
                if (scale != 1.0f) {
                    win.mHWScale = scale;
                    win.mNeedHWResizer = true;
                    if (AppResolutionTuner.DEBUG) {
                        Slog.d(AppResolutionTuner.TAG, "setWindowScaleByWL, scale: " + scale + ", win: " + win + ",attrs: " + attrs.getTitle().toString());
                    }
                }
            }
        }
    }

    public boolean updateResolutionTunerConfig(String config) {
        return getTunerList().updateResolutionTunerConfig(config);
    }

    public String getTAG() {
        return AppResolutionTuner.TAG;
    }

    public boolean isDebug() {
        return AppResolutionTuner.DEBUG;
    }

    private AppResolutionTuner getTunerList() {
        return AppResolutionTuner.getInstance();
    }
}
