package com.android.server.wm;

import android.app.servertransaction.BoundsCompat;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.Rect;
import android.view.DisplayCutout;
/* loaded from: classes.dex */
public class BoundsCompatUtils {
    private static volatile BoundsCompatUtils sSingleInstance = null;
    private static final Object M_LOCK = new Object();

    public static BoundsCompatUtils getInstance() {
        if (sSingleInstance == null) {
            synchronized (M_LOCK) {
                if (sSingleInstance == null) {
                    sSingleInstance = new BoundsCompatUtils();
                }
            }
        }
        return sSingleInstance;
    }

    public Configuration getCompatConfiguration(Configuration globalConfig, float aspectRatio, DisplayContent dp) {
        if (aspectRatio < 1.0f) {
            aspectRatio = 1.0f / aspectRatio;
        }
        Configuration compatConfig = new Configuration(globalConfig);
        Rect globalBounds = globalConfig.windowConfiguration.getBounds();
        Rect globalAppBounds = globalConfig.windowConfiguration.getAppBounds();
        Point displaySize = new Point(globalBounds.width(), globalBounds.height());
        Rect compatBounds = BoundsCompat.getInstance().computeCompatBounds(aspectRatio, displaySize, 17, globalConfig.orientation, 0);
        Rect compatAppBounds = new Rect(compatBounds);
        if (compatBounds.top < globalAppBounds.top) {
            compatAppBounds.top = globalAppBounds.top;
        }
        if (compatBounds.bottom > globalAppBounds.bottom) {
            compatAppBounds.bottom = globalAppBounds.bottom;
        }
        compatConfig.windowConfiguration.setBounds(compatBounds);
        compatConfig.windowConfiguration.setAppBounds(compatAppBounds);
        compatConfig.windowConfiguration.setMaxBounds(compatAppBounds);
        adaptCompatBounds(compatConfig, dp);
        return compatConfig;
    }

    public void adaptCompatBounds(Configuration config, DisplayContent dc) {
        Rect bounds = config.windowConfiguration.getBounds();
        if (bounds.isEmpty() || config.densityDpi == 0) {
            return;
        }
        float density = config.densityDpi / 160.0f;
        int width = bounds.width();
        int height = bounds.height();
        int i = (int) (width / density);
        config.compatScreenWidthDp = i;
        config.screenWidthDp = i;
        int i2 = 1;
        if (config.orientation == 1 && dc != null) {
            config.screenHeightDp = (int) (dc.getDisplayPolicy().getConfigDisplayHeight(width, height, 0, config.uiMode, (DisplayCutout) null) / density);
        } else {
            int i3 = (int) (height / density);
            config.compatScreenHeightDp = i3;
            config.screenHeightDp = i3;
        }
        if (config.screenWidthDp > config.screenHeightDp) {
            i2 = 2;
        }
        config.orientation = i2;
        int shortSizeDp = width <= height ? config.screenWidthDp : config.screenHeightDp;
        int longSizeDp = width >= height ? config.screenWidthDp : config.screenHeightDp;
        config.compatSmallestScreenWidthDp = shortSizeDp;
        config.smallestScreenWidthDp = shortSizeDp;
        int sl = Configuration.resetScreenLayout(config.screenLayout);
        config.screenLayout = Configuration.reduceScreenLayout(sl, longSizeDp, shortSizeDp);
    }
}
