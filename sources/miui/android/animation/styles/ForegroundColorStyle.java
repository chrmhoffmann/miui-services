package miui.android.animation.styles;

import android.graphics.Color;
import android.os.Build;
import android.view.View;
import miui.android.animation.IAnimTarget;
import miui.android.animation.ViewTarget;
import miui.android.animation.listener.UpdateInfo;
import miui.android.animation.utils.KeyUtils;
/* loaded from: classes.dex */
public class ForegroundColorStyle extends PropertyStyle {
    public static void start(IAnimTarget target, UpdateInfo update) {
        View targetView = getView(target);
        if (isInvalid(targetView)) {
            return;
        }
        int tintMode = update.animInfo.tintMode;
        TintDrawable drawable = TintDrawable.setAndGet(targetView);
        Object object = targetView.getTag(KeyUtils.KEY_FOLME_SET_HOVER);
        if (object != null && ((object instanceof Float) || (object instanceof Integer))) {
            float rodia = ((Float) object).floatValue();
            drawable.setCorner(rodia);
        }
        drawable.initTintBuffer(tintMode & 1);
    }

    public static void end(IAnimTarget target, UpdateInfo update) {
        View targetView = getView(target);
        if (isInvalid(targetView)) {
            return;
        }
        TintDrawable drawable = TintDrawable.get(targetView);
        int value = (int) update.animInfo.value;
        if (drawable != null && Color.alpha(value) == 0) {
            drawable.restoreOriginalDrawable();
        }
    }

    private static View getView(IAnimTarget target) {
        if (target instanceof ViewTarget) {
            return ((ViewTarget) target).getTargetObject();
        }
        return null;
    }

    private static boolean isInvalid(View target) {
        return target == null || Build.VERSION.SDK_INT < 23;
    }
}
