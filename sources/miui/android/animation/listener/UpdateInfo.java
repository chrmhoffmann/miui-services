package miui.android.animation.listener;

import java.util.Collection;
import miui.android.animation.IAnimTarget;
import miui.android.animation.internal.AnimInfo;
import miui.android.animation.internal.AnimTask;
import miui.android.animation.property.FloatProperty;
import miui.android.animation.property.IIntValueProperty;
/* loaded from: classes.dex */
public class UpdateInfo {
    public final AnimInfo animInfo = new AnimInfo();
    public volatile int frameCount;
    public volatile boolean isCompleted;
    public final FloatProperty property;
    public final boolean useInt;
    public volatile double velocity;

    public static UpdateInfo findByName(Collection<UpdateInfo> list, String name) {
        for (UpdateInfo update : list) {
            if (update.property.getName().equals(name)) {
                return update;
            }
        }
        return null;
    }

    public static UpdateInfo findBy(Collection<UpdateInfo> list, FloatProperty property) {
        for (UpdateInfo update : list) {
            if (update.property.equals(property)) {
                return update;
            }
        }
        return null;
    }

    public UpdateInfo(FloatProperty property) {
        this.property = property;
        this.useInt = property instanceof IIntValueProperty;
    }

    public Class<?> getType() {
        return this.property instanceof IIntValueProperty ? Integer.TYPE : Float.TYPE;
    }

    public <T> T getValue(Class<T> clz) {
        if (clz == Float.class || clz == Float.TYPE) {
            return (T) Float.valueOf(getFloatValue());
        }
        if (clz == Double.class || clz == Double.TYPE) {
            return (T) Double.valueOf(this.animInfo.value);
        }
        return (T) Integer.valueOf(getIntValue());
    }

    public float getFloatValue() {
        double setToValue = this.animInfo.setToValue;
        if (setToValue == Double.MAX_VALUE) {
            if (this.animInfo.value != Double.MAX_VALUE) {
                return (float) this.animInfo.value;
            }
            return Float.MAX_VALUE;
        }
        return (float) setToValue;
    }

    public int getIntValue() {
        double setToValue = this.animInfo.setToValue;
        if (setToValue == Double.MAX_VALUE) {
            if (this.animInfo.value != Double.MAX_VALUE) {
                return (int) this.animInfo.value;
            }
            return Integer.MAX_VALUE;
        }
        return (int) setToValue;
    }

    public String toString() {
        return "UpdateInfo{, property=" + this.property + ", velocity=" + this.velocity + ", value = " + this.animInfo.value + ", useInt=" + this.useInt + ", frameCount=" + this.frameCount + ", isCompleted=" + this.isCompleted + '}';
    }

    public void reset() {
        this.isCompleted = false;
        this.frameCount = 0;
    }

    public void setOp(byte op) {
        this.isCompleted = op == 0 || op > 2;
        if (this.isCompleted && AnimTask.isRunning(this.animInfo.op)) {
            this.animInfo.justEnd = true;
        }
        this.animInfo.op = op;
    }

    public boolean isValid() {
        return this.property != null;
    }

    public void setTargetValue(IAnimTarget target) {
        if (this.useInt) {
            target.setIntValue((IIntValueProperty) this.property, getIntValue());
        } else {
            target.setValue(this.property, getFloatValue());
        }
    }
}
