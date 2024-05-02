package com.android.server.display;

import android.hardware.display.BrightnessConfiguration;
import android.hardware.display.BrightnessCorrection;
import android.util.MathUtils;
import android.util.Pair;
import android.util.Slog;
import android.util.Spline;
import com.android.internal.util.Preconditions;
import com.android.server.wm.MiuiFreeformPinManagerService;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
/* loaded from: classes.dex */
public class MiuiPhysicalBrightnessMappingStrategy extends BrightnessMappingStrategy {
    public static final int CATEGORY_GAME = 1;
    public static final int CATEGORY_IMAGE = 4;
    public static final int CATEGORY_MAPS = 3;
    public static final int CATEGORY_READERS = 5;
    public static final int CATEGORY_SUM = 6;
    public static final int CATEGORY_UNDEFINED = 0;
    public static final int CATEGORY_VIDEO = 2;
    private static final String TAG = "MiuiBrightnessMappingStrategy";
    private int mApplicationCategory;
    private float mAutoBrightnessAdjustment;
    private final float[] mBrightness;
    private BrightnessMappingStrategyStub mBrightnessMappingStrategyImpl;
    private boolean mBrightnessRangeAdjustmentApplied;
    private Spline mBrightnessSpline;
    private Spline mBrightnessToNitsSpline;
    private BrightnessConfiguration mConfig;
    private final BrightnessConfiguration mDefaultConfig;
    private boolean mIsGlobalAdjustment;
    private final float mMaxGamma;
    private MiuiDisplayCloudController mMiuiDisplayCloudController;
    private final float[] mNits;
    private Spline mNitsToBrightnessSpline;
    private float mShortTermModelUserBrightness;
    private float mShortTermModelUserLux;
    private HashMap<Integer, Spline> mSplineGroup = new HashMap<>();
    private HashMap<Integer, Boolean> mAdjustedSplineMapper = new HashMap<>();

    public MiuiPhysicalBrightnessMappingStrategy(BrightnessConfiguration config, float[] nits, float[] brightness, float maxGamma) {
        boolean z = true;
        Preconditions.checkArgument((nits.length == 0 || brightness.length == 0) ? false : true, "Nits and brightness arrays must not be empty!");
        Preconditions.checkArgument(nits.length != brightness.length ? false : z, "Nits and brightness arrays must be the same length!");
        Objects.requireNonNull(config);
        Preconditions.checkArrayElementsInRange(nits, (float) MiuiFreeformPinManagerService.EDGE_AREA, Float.MAX_VALUE, "nits");
        Preconditions.checkArrayElementsInRange(brightness, (float) MiuiFreeformPinManagerService.EDGE_AREA, 1.0f, "brightness");
        this.mBrightnessMappingStrategyImpl = BrightnessMappingStrategyStub.getInstance();
        this.mMaxGamma = maxGamma;
        this.mAutoBrightnessAdjustment = MiuiFreeformPinManagerService.EDGE_AREA;
        this.mShortTermModelUserLux = -1.0f;
        this.mShortTermModelUserBrightness = -1.0f;
        this.mApplicationCategory = 0;
        this.mIsGlobalAdjustment = false;
        this.mNits = nits;
        this.mBrightness = brightness;
        computeNitsBrightnessSplines(nits);
        this.mDefaultConfig = config;
        this.mConfig = config;
        computeSpline();
    }

    private void initializeSplineGroup(Spline spline) {
        for (int i = 0; i < 6; i++) {
            this.mSplineGroup.put(Integer.valueOf(i), spline);
            this.mAdjustedSplineMapper.put(Integer.valueOf(i), false);
        }
    }

    private void adjustSplineGroup(int category, Spline spline) {
        if (this.mIsGlobalAdjustment) {
            for (int i = 0; i < 6; i++) {
                this.mSplineGroup.put(Integer.valueOf(i), spline);
            }
        } else if (category == 0) {
            for (Map.Entry<Integer, Boolean> entry : this.mAdjustedSplineMapper.entrySet()) {
                if (!entry.getValue().booleanValue()) {
                    this.mSplineGroup.put(entry.getKey(), spline);
                }
            }
        } else {
            this.mSplineGroup.put(Integer.valueOf(this.mApplicationCategory), spline);
            this.mAdjustedSplineMapper.put(Integer.valueOf(this.mApplicationCategory), true);
        }
    }

    public boolean setBrightnessConfiguration(BrightnessConfiguration config) {
        if (config == null) {
            config = this.mDefaultConfig;
        }
        if (config.equals(this.mConfig)) {
            return false;
        }
        this.mConfig = config;
        computeSpline();
        return true;
    }

    public BrightnessConfiguration getBrightnessConfiguration() {
        return this.mConfig;
    }

    public float getBrightness(float lux, String packageName, int category) {
        float nits = this.mBrightnessSpline.interpolate(lux);
        float brightness = this.mNitsToBrightnessSpline.interpolate(nits);
        if (this.mShortTermModelUserLux == -1.0f) {
            return correctBrightness(brightness, packageName, category);
        }
        if (this.mLoggingEnabled) {
            Slog.d(TAG, "user point set, correction not applied");
            return brightness;
        }
        return brightness;
    }

    public float getBrightness(float lux, String packageName) {
        int category = applicationCategoryInfo(packageName);
        float nits = ((Spline) Objects.requireNonNull(this.mSplineGroup.get(Integer.valueOf(category)))).interpolate(lux);
        float backlight = this.mNitsToBrightnessSpline.interpolate(nits);
        Slog.d(TAG, "autobrightness adjustment by applications is applied, lux = " + lux + ", brightness = " + backlight + ", appPackageName = " + packageName);
        return backlight;
    }

    public float getAutoBrightnessAdjustment() {
        return this.mAutoBrightnessAdjustment;
    }

    public boolean setAutoBrightnessAdjustment(float adjustment) {
        float adjustment2 = MathUtils.constrain(adjustment, -1.0f, 1.0f);
        if (adjustment2 == this.mAutoBrightnessAdjustment) {
            return false;
        }
        this.mAutoBrightnessAdjustment = adjustment2;
        computeSpline();
        return true;
    }

    public float convertToNits(float brightness) {
        return this.mBrightnessToNitsSpline.interpolate(brightness);
    }

    public float convertToBrightness(float nit) {
        return this.mNitsToBrightnessSpline.interpolate(nit);
    }

    public void addUserDataPoint(float lux, float brightness) {
        addUserDataPoint(lux, brightness, null);
    }

    public void addUserDataPoint(float lux, float brightness, String packageName) {
        isGlobalAdjustment(packageName);
        this.mApplicationCategory = applicationCategoryInfo(packageName);
        Slog.d(TAG, "User add data point: lux = " + lux + ", brightness = " + brightness + ", packageName = " + packageName + ", category =" + this.mApplicationCategory);
        float unadjustedBrightness = getUnadjustedBrightness(lux);
        this.mBrightnessMappingStrategyImpl.updateUnadjustedBrightness(lux, brightness, unadjustedBrightness);
        float adjustment = inferAutoBrightnessAdjustment(this.mMaxGamma, brightness, unadjustedBrightness);
        this.mAutoBrightnessAdjustment = adjustment;
        this.mShortTermModelUserLux = lux;
        this.mShortTermModelUserBrightness = brightness;
        computeSpline();
    }

    public void clearUserDataPoints() {
        if (this.mShortTermModelUserLux != -1.0f) {
            Slog.d(TAG, "Clear user data points.");
            this.mAutoBrightnessAdjustment = MiuiFreeformPinManagerService.EDGE_AREA;
            this.mShortTermModelUserLux = -1.0f;
            this.mShortTermModelUserBrightness = -1.0f;
            computeSpline();
        }
    }

    public boolean hasUserDataPoints() {
        return this.mShortTermModelUserLux != -1.0f;
    }

    public boolean isDefaultConfig() {
        return this.mDefaultConfig.equals(this.mConfig);
    }

    public BrightnessConfiguration getDefaultConfig() {
        return this.mDefaultConfig;
    }

    public void recalculateSplines(boolean applyAdjustment, float[] adjustedNits) {
        this.mBrightnessRangeAdjustmentApplied = applyAdjustment;
        computeNitsBrightnessSplines(applyAdjustment ? adjustedNits : this.mNits);
    }

    public float getUserDataPoint() {
        return this.mShortTermModelUserLux;
    }

    public Spline getBrightnessSpline() {
        return this.mBrightnessSpline;
    }

    public boolean isForIdleMode() {
        return false;
    }

    public long getShortTermModelTimeout() {
        if (this.mConfig.getShortTermModelTimeoutMillis() >= 0) {
            return this.mConfig.getShortTermModelTimeoutMillis();
        }
        return this.mDefaultConfig.getShortTermModelTimeoutMillis();
    }

    private void computeSpline() {
        Pair<float[], float[]> defaultCurve = this.mConfig.getCurve();
        float[] defaultLux = (float[]) defaultCurve.first;
        float[] defaultNits = (float[]) defaultCurve.second;
        float[] defaultBrightness = new float[defaultNits.length];
        for (int i = 0; i < defaultBrightness.length; i++) {
            defaultBrightness[i] = this.mNitsToBrightnessSpline.interpolate(defaultNits[i]);
        }
        Pair<float[], float[]> curve = getAdjustedCurve(defaultLux, defaultBrightness, this.mShortTermModelUserLux, this.mShortTermModelUserBrightness, this.mAutoBrightnessAdjustment, this.mMaxGamma);
        float[] lux = (float[]) curve.first;
        float[] brightness = (float[]) curve.second;
        float[] nits = new float[brightness.length];
        for (int i2 = 0; i2 < nits.length; i2++) {
            nits[i2] = this.mBrightnessToNitsSpline.interpolate(brightness[i2]);
        }
        Spline createSpline = Spline.createSpline(lux, nits);
        this.mBrightnessSpline = createSpline;
        if (this.mShortTermModelUserLux != -1.0f) {
            adjustSplineGroup(this.mApplicationCategory, createSpline);
        } else {
            initializeSplineGroup(createSpline);
        }
    }

    private float getUnadjustedBrightness(float lux) {
        Pair<float[], float[]> curve = this.mConfig.getCurve();
        Spline spline = Spline.createSpline((float[]) curve.first, (float[]) curve.second);
        return this.mNitsToBrightnessSpline.interpolate(spline.interpolate(lux));
    }

    private float correctBrightness(float brightness, String packageName, int category) {
        BrightnessCorrection correction;
        BrightnessCorrection correction2;
        if (packageName != null && (correction2 = this.mConfig.getCorrectionByPackageName(packageName)) != null) {
            return correction2.apply(brightness);
        }
        if (category != -1 && (correction = this.mConfig.getCorrectionByCategory(category)) != null) {
            return correction.apply(brightness);
        }
        return brightness;
    }

    private int applicationCategoryInfo(String packageName) {
        Map<Integer, List<String>> appMapper;
        MiuiDisplayCloudController miuiDisplayCloudController = this.mMiuiDisplayCloudController;
        if (miuiDisplayCloudController != null && (appMapper = miuiDisplayCloudController.getShortTermModelAppMapper()) != null && this.mMiuiDisplayCloudController.isShortTermModelEnable()) {
            if (((List) Objects.requireNonNull(appMapper.get(1))).contains(packageName)) {
                return 1;
            }
            if (((List) Objects.requireNonNull(appMapper.get(2))).contains(packageName)) {
                return 2;
            }
            if (((List) Objects.requireNonNull(appMapper.get(3))).contains(packageName)) {
                return 3;
            }
            if (((List) Objects.requireNonNull(appMapper.get(4))).contains(packageName)) {
                return 4;
            }
            if (((List) Objects.requireNonNull(appMapper.get(5))).contains(packageName)) {
                return 5;
            }
            return 0;
        }
        return 0;
    }

    private void computeNitsBrightnessSplines(float[] nits) {
        this.mNitsToBrightnessSpline = Spline.createLinearSpline(nits, this.mBrightness);
        this.mBrightnessToNitsSpline = Spline.createLinearSpline(this.mBrightness, nits);
    }

    public void setMiuiDisplayCloudController(MiuiDisplayCloudController cloudController) {
        this.mMiuiDisplayCloudController = cloudController;
    }

    private void isGlobalAdjustment(String packageName) {
        MiuiDisplayCloudController miuiDisplayCloudController = this.mMiuiDisplayCloudController;
        boolean z = false;
        if (miuiDisplayCloudController != null && miuiDisplayCloudController.getShortTermModelAppMapper() != null && ((List) Objects.requireNonNull(this.mMiuiDisplayCloudController.getShortTermModelAppMapper().get(0))).contains(packageName)) {
            z = true;
        }
        this.mIsGlobalAdjustment = z;
    }

    public void dump(PrintWriter pw, float hbmTransition) {
        pw.println("MiuiPhysicalMappingStrategy Configuration:");
        pw.println("  mConfig=" + this.mConfig);
        pw.println("  mBrightnessSpline=" + this.mBrightnessSpline);
        pw.println("  mNitsToBacklightSpline=" + this.mNitsToBrightnessSpline);
        pw.println("  mMaxGamma=" + this.mMaxGamma);
        pw.println("  mAutoBrightnessAdjustment=" + this.mAutoBrightnessAdjustment);
        pw.println("  mShortTermModelUserLux=" + this.mShortTermModelUserLux);
        pw.println("  mShortTermModelUserBrightness=" + this.mShortTermModelUserBrightness);
        pw.println("  mDefaultConfig=" + this.mDefaultConfig);
        pw.println("  UndefinedBrightnessSpline=" + this.mSplineGroup.get(0));
        pw.println("  GameBrightnessSpline=" + this.mSplineGroup.get(1));
        pw.println("  VideoBrightnessSpline=" + this.mSplineGroup.get(2));
        pw.println("  MapsBrightnessSpline=" + this.mSplineGroup.get(3));
        pw.println("  ImageBrightnessSpline=" + this.mSplineGroup.get(4));
        pw.println("  ReadersBrightnessSpline=" + this.mSplineGroup.get(5));
    }
}
