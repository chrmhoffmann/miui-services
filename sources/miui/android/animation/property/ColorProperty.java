package miui.android.animation.property;

import com.android.server.wm.MiuiFreeformPinManagerService;
import java.util.Objects;
/* loaded from: classes.dex */
public class ColorProperty<T> extends FloatProperty<T> implements IIntValueProperty<T> {
    private int mColorValue;

    public ColorProperty(String name) {
        super(name);
    }

    @Override // miui.android.animation.property.FloatProperty
    public void setValue(T object, float value) {
    }

    @Override // miui.android.animation.property.FloatProperty
    public float getValue(T o) {
        return MiuiFreeformPinManagerService.EDGE_AREA;
    }

    @Override // miui.android.animation.property.IIntValueProperty
    public void setIntValue(T o, int i) {
        this.mColorValue = i;
        if (o instanceof ValueTargetObject) {
            ValueTargetObject obj = (ValueTargetObject) o;
            obj.setPropertyValue(getName(), Integer.TYPE, Integer.valueOf(i));
        }
    }

    @Override // miui.android.animation.property.IIntValueProperty
    public int getIntValue(T o) {
        if (o instanceof ValueTargetObject) {
            ValueTargetObject obj = (ValueTargetObject) o;
            this.mColorValue = ((Integer) obj.getPropertyValue(getName(), Integer.TYPE)).intValue();
        }
        return this.mColorValue;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ColorProperty that = (ColorProperty) o;
        return this.mPropertyName.equals(that.mPropertyName);
    }

    public int hashCode() {
        return Objects.hash(this.mPropertyName);
    }
}
