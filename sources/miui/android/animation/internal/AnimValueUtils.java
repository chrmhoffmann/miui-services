package miui.android.animation.internal;

import miui.android.animation.IAnimTarget;
import miui.android.animation.listener.UpdateInfo;
import miui.android.animation.property.FloatProperty;
import miui.android.animation.property.IIntValueProperty;
import miui.android.animation.property.ISpecificProperty;
import miui.android.animation.utils.CommonUtils;
/* loaded from: classes.dex */
public class AnimValueUtils {
    private AnimValueUtils() {
    }

    public static double getValueOfTarget(IAnimTarget target, FloatProperty property, double value) {
        if (value == 2.147483647E9d) {
            return target.getIntValue((IIntValueProperty) property);
        }
        if (value == 3.4028234663852886E38d) {
            return target.getValue(property);
        }
        return getValue(target, property, value);
    }

    public static double getValue(IAnimTarget target, FloatProperty property, double value) {
        if (property instanceof ISpecificProperty) {
            return ((ISpecificProperty) property).getSpecificValue((float) value);
        }
        return getCurTargetValue(target, property, value);
    }

    private static double getCurTargetValue(IAnimTarget target, FloatProperty property, double value) {
        double sig = Math.signum(value);
        double absValue = Math.abs(value);
        if (absValue == 1000000.0d) {
            return CommonUtils.getSize(target, property) * sig;
        }
        double curValue = property instanceof IIntValueProperty ? target.getIntValue((IIntValueProperty) property) : target.getValue(property);
        if (absValue == 1000100.0d) {
            return curValue * sig;
        }
        return curValue;
    }

    public static boolean isInvalid(double value) {
        return value == Double.MAX_VALUE || value == 3.4028234663852886E38d || value == 2.147483647E9d;
    }

    public static boolean handleSetToValue(UpdateInfo update) {
        if (!isInvalid(update.animInfo.setToValue)) {
            update.animInfo.value = update.animInfo.setToValue;
            update.animInfo.setToValue = Double.MAX_VALUE;
            return true;
        }
        return false;
    }
}
