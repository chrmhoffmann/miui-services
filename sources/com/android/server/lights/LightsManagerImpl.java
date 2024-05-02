package com.android.server.lights;

import android.content.res.Resources;
import android.util.Slog;
import com.miui.base.MiuiStubRegistry;
import miui.os.DeviceFeature;
/* loaded from: classes.dex */
public class LightsManagerImpl implements LightsManagerStub {
    private static final boolean SUPPORT_HBM = Resources.getSystem().getBoolean(285540443);
    private static final String TAG = "LightsManagerImpl";

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<LightsManagerImpl> {

        /* compiled from: LightsManagerImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final LightsManagerImpl INSTANCE = new LightsManagerImpl();
        }

        public LightsManagerImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public LightsManagerImpl provideNewInstance() {
            return new LightsManagerImpl();
        }
    }

    public int brightnessToColor(int id, int brightness, int lastColor) {
        if (id == 0 && DeviceFeature.BACKLIGHT_BIT > 8 && DeviceFeature.BACKLIGHT_BIT <= 14) {
            if (brightness < 0) {
                Slog.e(TAG, "invalid backlight " + brightness + " !!!");
                return lastColor;
            } else if (SUPPORT_HBM) {
                return brightness & 16383;
            } else {
                return brightness & 8191;
            }
        }
        int color = brightness & 255;
        return color | (-16777216) | (color << 16) | (color << 8);
    }
}
