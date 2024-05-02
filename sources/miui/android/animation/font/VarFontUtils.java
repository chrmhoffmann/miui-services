package miui.android.animation.font;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.widget.TextView;
import com.android.server.ScoutHelper;
import com.android.server.lights.view.MessageView;
import com.android.server.wm.MiuiFreeformPinManagerService;
import java.io.File;
import miui.android.animation.internal.AnimTask;
import miui.android.animation.utils.CommonUtils;
import miuix.animation.utils.DeviceUtils;
/* loaded from: classes.dex */
public class VarFontUtils {
    private static final int DEFAULT_WGHT = 50;
    private static final String FORMAT_WGHT = "'wght' ";
    private static final boolean IS_USING_VAR_FONT;
    private static final String KEY_FONT_WEIGHT = "key_miui_font_weight_scale";
    public static final int MIN_WGHT;
    private static final int[] MITYPE_WGHT;
    private static final int[] MIUI_WGHT;
    private static final int[][][] RULES;
    private static final String THEME_FONT_DIR = "/data/system/theme/fonts/";

    private static boolean isUsingThemeFont() {
        String[] files;
        File dir = new File(THEME_FONT_DIR);
        try {
            if (!dir.exists() || (files = dir.list()) == null) {
                return false;
            }
            return files.length > 0;
        } catch (Exception e) {
            Log.w(CommonUtils.TAG, "isUsingThemeFont, failed to check theme font directory", e);
        }
        return false;
    }

    private static boolean isFontAnimDisabled() {
        try {
            String value = CommonUtils.readProp("ro.miui.ui.font.animation");
            return value.equals("disable");
        } catch (Exception e) {
            Log.w(CommonUtils.TAG, "isFontAnimationEnabled failed", e);
            return false;
        }
    }

    static {
        boolean z = !isUsingThemeFont() && Build.VERSION.SDK_INT >= 26 && DeviceUtils.getTotalRam() > 6 && !isFontAnimDisabled() && DeviceUtils.getDeviceLevel() > 0;
        IS_USING_VAR_FONT = z;
        if (z) {
            MIUI_WGHT = new int[]{AnimTask.MAX_PAGE_SIZE, ScoutHelper.BINDER_FULL_KILL_SCORE_ADJ_MIN, 250, 305, 340, 400, 480, 540, 630, MessageView.WaveConfig.EDGE_X};
            MITYPE_WGHT = new int[]{10, 20, 30, 40, 50, 60, 70, 80, 90, 100};
            MIN_WGHT = 10;
            RULES = r5;
            int[][][] iArr = {new int[][]{new int[]{0, 5}, new int[]{0, 5}, new int[]{1, 6}, new int[]{2, 6}, new int[]{2, 7}, new int[]{3, 8}, new int[]{4, 8}, new int[]{5, 9}, new int[]{6, 9}, new int[]{7, 9}}, new int[][]{new int[]{0, 2}, new int[]{0, 3}, new int[]{1, 4}, new int[]{1, 5}, new int[]{2, 6}, new int[]{2, 7}, new int[]{3, 8}, new int[]{4, 9}, new int[]{5, 9}, new int[]{6, 9}}, new int[][]{new int[]{0, 5}, new int[]{1, 5}, new int[]{2, 5}, new int[]{3, 6}, new int[]{3, 6}, new int[]{4, 7}, new int[]{5, 8}, new int[]{6, 8}, new int[]{7, 8}, new int[]{8, 9}}};
            return;
        }
        MIN_WGHT = 0;
        int[] iArr2 = new int[0];
        MITYPE_WGHT = iArr2;
        MIUI_WGHT = iArr2;
        RULES = new int[0][];
    }

    private VarFontUtils() {
    }

    public static int getScaleWght(int fontWeight, float scaleTextSize, int fontType, int scale) {
        if (!IS_USING_VAR_FONT) {
            return fontWeight;
        }
        int[] range = getWghtRange(fontWeight, scaleTextSize);
        int startWght = getWghtByType(range[0], fontType);
        int midWght = getWghtByType(fontWeight, fontType);
        int endWght = getWghtByType(range[1], fontType);
        if (scale < 50) {
            float t = scale / 50.0f;
            int wght = (int) (((1.0f - t) * startWght) + (midWght * t));
            return wght;
        } else if (scale <= 50) {
            return midWght;
        } else {
            float t2 = (scale - 50) / 50.0f;
            int wght2 = (int) (((1.0f - t2) * midWght) + (endWght * t2));
            return wght2;
        }
    }

    public static void setVariationFont(TextView textView, int wght) {
        if (IS_USING_VAR_FONT) {
            textView.setFontVariationSettings(FORMAT_WGHT + wght);
        }
    }

    private static int[] getWghtArray(int fontType) {
        return (fontType == 1 || fontType == 2) ? MITYPE_WGHT : MIUI_WGHT;
    }

    public static int getSysFontScale(Context context) {
        ContentResolver resolver = context.getContentResolver();
        return Settings.System.getInt(resolver, KEY_FONT_WEIGHT, 50);
    }

    private static int[] getWghtRange(int fontWeight, float textSize) {
        int idx = 0;
        if (textSize > 20.0f) {
            idx = 1;
        } else if (textSize > MiuiFreeformPinManagerService.EDGE_AREA && textSize < 12.0f) {
            idx = 2;
        }
        return RULES[idx][fontWeight];
    }

    private static int getWghtByType(int fontWeight, int fontType) {
        return getWghtArray(fontType)[fontWeight];
    }
}
