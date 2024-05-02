package com.android.server.display;

import android.app.ActivityThread;
import android.content.res.Resources;
import android.util.MathUtils;
import com.android.server.wm.MiuiFreeformPinManagerService;
import com.miui.base.MiuiStubRegistry;
/* loaded from: classes.dex */
public class MiuiBrightnessUtilsImpl extends MiuiBrightnessUtilsStub {
    private static final float A;
    private static final float B;
    private static final float C;
    private static final float R;
    private static final Resources resources;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MiuiBrightnessUtilsImpl> {

        /* compiled from: MiuiBrightnessUtilsImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MiuiBrightnessUtilsImpl INSTANCE = new MiuiBrightnessUtilsImpl();
        }

        public MiuiBrightnessUtilsImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public MiuiBrightnessUtilsImpl provideNewInstance() {
            return new MiuiBrightnessUtilsImpl();
        }
    }

    static {
        Resources resources2 = ActivityThread.currentApplication().getResources();
        resources = resources2;
        R = resources2.getFloat(285671454);
        A = resources2.getFloat(285671451);
        B = resources2.getFloat(285671452);
        C = resources2.getFloat(285671453);
    }

    public float convertGammaToLinear(float val) {
        float ret;
        float f = R;
        if (val <= f) {
            ret = MathUtils.sq(val / f);
        } else {
            float ret2 = C;
            ret = MathUtils.exp((val - ret2) / A) + B;
        }
        float normalizedRet = MathUtils.constrain(ret, (float) MiuiFreeformPinManagerService.EDGE_AREA, 12.0f);
        return normalizedRet / 12.0f;
    }

    public float convertLinearToGamma(float val) {
        float normalizedVal = 12.0f * val;
        if (normalizedVal <= 1.0f) {
            float ret = MathUtils.sqrt(normalizedVal) * R;
            return ret;
        }
        float ret2 = A;
        return (ret2 * MathUtils.log(normalizedVal - B)) + C;
    }
}
