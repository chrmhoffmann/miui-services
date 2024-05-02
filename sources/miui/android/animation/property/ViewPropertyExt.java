package miui.android.animation.property;

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;
import com.android.server.wm.MiuiFreeformPinManagerService;
import miui.android.animation.utils.KeyUtils;
/* loaded from: classes.dex */
public class ViewPropertyExt {
    public static final ForegroundProperty FOREGROUND = new ForegroundProperty();
    public static final BackgroundProperty BACKGROUND = new BackgroundProperty();

    private ViewPropertyExt() {
    }

    /* loaded from: classes.dex */
    public static class ForegroundProperty extends ViewProperty implements IIntValueProperty<View> {
        private ForegroundProperty() {
            super("foreground");
        }

        public float getValue(View object) {
            return MiuiFreeformPinManagerService.EDGE_AREA;
        }

        public void setValue(View object, float value) {
        }

        public int getIntValue(View view) {
            Object tag = view.getTag(KeyUtils.KEY_FOLME_FORGROUND_COLOR);
            if (tag instanceof Integer) {
                return ((Integer) tag).intValue();
            }
            return 0;
        }

        public void setIntValue(View view, int value) {
            Drawable fg;
            view.setTag(KeyUtils.KEY_FOLME_FORGROUND_COLOR, Integer.valueOf(value));
            if (Build.VERSION.SDK_INT >= 23 && (fg = view.getForeground()) != null) {
                fg.invalidateSelf();
            }
        }
    }

    /* loaded from: classes.dex */
    public static class BackgroundProperty extends ViewProperty implements IIntValueProperty<View> {
        private BackgroundProperty() {
            super("background");
        }

        public float getValue(View object) {
            return MiuiFreeformPinManagerService.EDGE_AREA;
        }

        public void setValue(View object, float value) {
        }

        public void setIntValue(View target, int value) {
            target.setBackgroundColor(value);
        }

        public int getIntValue(View target) {
            Drawable bg = target.getBackground();
            if (bg instanceof ColorDrawable) {
                return ((ColorDrawable) bg).getColor();
            }
            return 0;
        }
    }
}
