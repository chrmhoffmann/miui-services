package com.android.server.display;

import android.hardware.display.BrightnessConfiguration;
import android.os.SystemProperties;
import android.util.MathUtils;
import android.util.Slog;
import com.miui.base.MiuiStubRegistry;
import miui.os.Build;
import miui.util.FeatureParser;
/* loaded from: classes.dex */
public class BrightnessMappingStrategyImpl implements BrightnessMappingStrategyStub {
    private static boolean DEBUG = false;
    private static final String TAG = "BrightnessMappingStrategyImpl";
    private float sUnadjustedBrightness = -1.0f;
    private final boolean IS_SUPPORT_AUTOBRIGHTNESS_BY_APPLICATION_CATEGORY = FeatureParser.getBoolean("support_autobrightness_by_application_category", false);
    private final boolean IS_INTERNAL_BUILD = Build.IS_INTERNATIONAL_BUILD;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<BrightnessMappingStrategyImpl> {

        /* compiled from: BrightnessMappingStrategyImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final BrightnessMappingStrategyImpl INSTANCE = new BrightnessMappingStrategyImpl();
        }

        public BrightnessMappingStrategyImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public BrightnessMappingStrategyImpl provideNewInstance() {
            return new BrightnessMappingStrategyImpl();
        }
    }

    public float getMaxScreenNit() {
        return Float.parseFloat(SystemProperties.get("persist.vendor.max.brightness", "0"));
    }

    public void updateUnadjustedBrightness(float lux, float brightness, float unadjustedbrightness) {
        if (DEBUG) {
            Slog.i(TAG, "userLux=" + lux + ", userBrightness=" + brightness + ", unadjustedbrightness=" + unadjustedbrightness);
        }
        this.sUnadjustedBrightness = unadjustedbrightness;
    }

    public boolean smoothNewCurve(float[] lux, float[] brightness, int idx) {
        if (DEBUG) {
            Slog.d(TAG, "userLux=" + lux[idx] + ", userBrightness=" + brightness[idx] + ", unadjustedbrightness=" + this.sUnadjustedBrightness);
        }
        for (int i = idx + 1; i < lux.length; i++) {
            brightness[i] = brightness[i] - (this.sUnadjustedBrightness - brightness[idx]);
            brightness[i] = MathUtils.max(brightness[i], brightness[i - 1]);
        }
        for (int i2 = idx - 1; i2 >= 0; i2--) {
            float f = brightness[i2];
            float f2 = this.sUnadjustedBrightness;
            brightness[i2] = f - (((f2 - brightness[idx]) * brightness[i2]) / f2);
            brightness[i2] = MathUtils.min(brightness[i2], brightness[i2 + 1]);
        }
        return true;
    }

    public boolean isSupportAutobrightnessByApplicationCategory() {
        return this.IS_SUPPORT_AUTOBRIGHTNESS_BY_APPLICATION_CATEGORY && !this.IS_INTERNAL_BUILD;
    }

    public BrightnessMappingStrategy getMiuiMapperInstance(BrightnessConfiguration build, float[] nitsRange, float[] brightnessRange, float autoBrightnessAdjustmentMaxGamma) {
        return new MiuiPhysicalBrightnessMappingStrategy(build, nitsRange, brightnessRange, autoBrightnessAdjustmentMaxGamma);
    }
}
