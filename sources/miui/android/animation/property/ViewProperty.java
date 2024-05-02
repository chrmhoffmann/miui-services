package miui.android.animation.property;

import android.os.Build;
import android.view.View;
import com.android.server.wm.MiuiFreeformPinManagerService;
import com.miui.server.input.edgesuppression.EdgeSuppressionManager;
import miui.android.animation.utils.KeyUtils;
/* loaded from: classes.dex */
public abstract class ViewProperty extends FloatProperty<View> {
    public static final ViewProperty TRANSLATION_X = new ViewProperty("translationX") { // from class: miui.android.animation.property.ViewProperty.1
        public void setValue(View view, float value) {
            view.setTranslationX(value);
        }

        public float getValue(View view) {
            return view.getTranslationX();
        }
    };
    public static final ViewProperty TRANSLATION_Y = new ViewProperty("translationY") { // from class: miui.android.animation.property.ViewProperty.2
        public void setValue(View view, float value) {
            view.setTranslationY(value);
        }

        public float getValue(View view) {
            return view.getTranslationY();
        }
    };
    public static final ViewProperty TRANSLATION_Z = new ViewProperty("translationZ") { // from class: miui.android.animation.property.ViewProperty.3
        public void setValue(View view, float value) {
            if (Build.VERSION.SDK_INT >= 21) {
                view.setTranslationZ(value);
            }
        }

        public float getValue(View view) {
            if (Build.VERSION.SDK_INT >= 21) {
                return view.getTranslationZ();
            }
            return MiuiFreeformPinManagerService.EDGE_AREA;
        }
    };
    public static final ViewProperty SCALE_X = new ViewProperty("scaleX") { // from class: miui.android.animation.property.ViewProperty.4
        public void setValue(View view, float value) {
            view.setScaleX(value);
        }

        public float getValue(View view) {
            return view.getScaleX();
        }
    };
    public static final ViewProperty SCALE_Y = new ViewProperty("scaleY") { // from class: miui.android.animation.property.ViewProperty.5
        public void setValue(View view, float value) {
            view.setScaleY(value);
        }

        public float getValue(View view) {
            return view.getScaleY();
        }
    };
    public static final ViewProperty ROTATION = new ViewProperty(EdgeSuppressionManager.REASON_OF_ROTATION) { // from class: miui.android.animation.property.ViewProperty.6
        public void setValue(View view, float value) {
            view.setRotation(value);
        }

        public float getValue(View view) {
            return view.getRotation();
        }
    };
    public static final ViewProperty ROTATION_X = new ViewProperty("rotationX") { // from class: miui.android.animation.property.ViewProperty.7
        public void setValue(View view, float value) {
            view.setRotationX(value);
        }

        public float getValue(View view) {
            return view.getRotationX();
        }
    };
    public static final ViewProperty ROTATION_Y = new ViewProperty("rotationY") { // from class: miui.android.animation.property.ViewProperty.8
        public void setValue(View view, float value) {
            view.setRotationY(value);
        }

        public float getValue(View view) {
            return view.getRotationY();
        }
    };
    public static final ViewProperty X = new ViewProperty("x") { // from class: miui.android.animation.property.ViewProperty.9
        public void setValue(View view, float value) {
            view.setX(value);
        }

        public float getValue(View view) {
            return view.getX();
        }
    };
    public static final ViewProperty Y = new ViewProperty("y") { // from class: miui.android.animation.property.ViewProperty.10
        public void setValue(View view, float value) {
            view.setY(value);
        }

        public float getValue(View view) {
            return view.getY();
        }
    };
    public static final ViewProperty Z = new ViewProperty("z") { // from class: miui.android.animation.property.ViewProperty.11
        public void setValue(View view, float value) {
            if (Build.VERSION.SDK_INT >= 21) {
                view.setZ(value);
            }
        }

        public float getValue(View view) {
            if (Build.VERSION.SDK_INT >= 21) {
                return view.getZ();
            }
            return MiuiFreeformPinManagerService.EDGE_AREA;
        }
    };
    public static final ViewProperty HEIGHT = new ViewProperty("height") { // from class: miui.android.animation.property.ViewProperty.12
        public void setValue(View view, float value) {
            view.getLayoutParams().height = (int) value;
            view.setTag(KeyUtils.KEY_FOLME_SET_HEIGHT, Float.valueOf(value));
            view.requestLayout();
        }

        public float getValue(View view) {
            int height = view.getHeight();
            Float value = (Float) view.getTag(KeyUtils.KEY_FOLME_SET_HEIGHT);
            if (value != null) {
                return value.floatValue();
            }
            if (height == 0 && ViewProperty.isInInitLayout(view)) {
                height = view.getMeasuredHeight();
            }
            return height;
        }
    };
    public static final ViewProperty WIDTH = new ViewProperty("width") { // from class: miui.android.animation.property.ViewProperty.13
        public void setValue(View view, float value) {
            view.getLayoutParams().width = (int) value;
            view.setTag(KeyUtils.KEY_FOLME_SET_WIDTH, Float.valueOf(value));
            view.requestLayout();
        }

        public float getValue(View view) {
            int width = view.getWidth();
            Float value = (Float) view.getTag(KeyUtils.KEY_FOLME_SET_WIDTH);
            if (value != null) {
                return value.floatValue();
            }
            if (width == 0 && ViewProperty.isInInitLayout(view)) {
                width = view.getMeasuredWidth();
            }
            return width;
        }
    };
    public static final ViewProperty ALPHA = new ViewProperty("alpha") { // from class: miui.android.animation.property.ViewProperty.14
        public void setValue(View view, float value) {
            view.setAlpha(value);
        }

        public float getValue(View view) {
            return view.getAlpha();
        }
    };
    public static final ViewProperty AUTO_ALPHA = new ViewProperty("autoAlpha") { // from class: miui.android.animation.property.ViewProperty.15
        public void setValue(View view, float value) {
            view.setAlpha(value);
            boolean isTransparent = Math.abs(value) <= 0.00390625f;
            if (view.getVisibility() != 0 && value > MiuiFreeformPinManagerService.EDGE_AREA && !isTransparent) {
                view.setVisibility(0);
            } else if (isTransparent) {
                view.setVisibility(8);
            }
        }

        public float getValue(View view) {
            return view.getAlpha();
        }
    };
    public static final ViewProperty SCROLL_X = new ViewProperty("scrollX") { // from class: miui.android.animation.property.ViewProperty.16
        public void setValue(View view, float value) {
            view.setScrollX((int) value);
        }

        public float getValue(View view) {
            return view.getScrollX();
        }
    };
    public static final ViewProperty SCROLL_Y = new ViewProperty("scrollY") { // from class: miui.android.animation.property.ViewProperty.17
        public void setValue(View view, float value) {
            view.setScrollY((int) value);
        }

        public float getValue(View view) {
            return view.getScrollY();
        }
    };
    public static final ViewProperty FOREGROUND = new ViewProperty("deprecated_foreground") { // from class: miui.android.animation.property.ViewProperty.18
        public void setValue(View view, float value) {
        }

        public float getValue(View view) {
            return MiuiFreeformPinManagerService.EDGE_AREA;
        }
    };
    public static final ViewProperty BACKGROUND = new ViewProperty("deprecated_background") { // from class: miui.android.animation.property.ViewProperty.19
        public void setValue(View view, float value) {
        }

        public float getValue(View view) {
            return MiuiFreeformPinManagerService.EDGE_AREA;
        }
    };

    public ViewProperty(String name) {
        super(name);
    }

    @Override // miui.android.animation.property.FloatProperty
    public String toString() {
        return "ViewProperty{mPropertyName='" + this.mPropertyName + "'}";
    }

    public static boolean isInInitLayout(View view) {
        return view.getTag(KeyUtils.KEY_FOLME_INIT_LAYOUT) != null;
    }
}
