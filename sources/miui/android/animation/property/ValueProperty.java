package miui.android.animation.property;

import com.android.server.wm.MiuiFreeformPinManagerService;
import java.util.Objects;
/* loaded from: classes.dex */
public class ValueProperty extends FloatProperty {
    private volatile String mName;

    public ValueProperty(String name) {
        super(name);
    }

    @Override // android.util.Property
    public String getName() {
        return this.mName != null ? this.mName : super.getName();
    }

    public void setName(String name) {
        this.mName = name;
    }

    @Override // miui.android.animation.property.FloatProperty
    public float getValue(Object o) {
        Float value;
        if ((o instanceof ValueTargetObject) && (value = (Float) ((ValueTargetObject) o).getPropertyValue(getName(), Float.TYPE)) != null) {
            return value.floatValue();
        }
        return MiuiFreeformPinManagerService.EDGE_AREA;
    }

    @Override // miui.android.animation.property.FloatProperty
    public void setValue(Object o, float v) {
        if (o instanceof ValueTargetObject) {
            ((ValueTargetObject) o).setPropertyValue(getName(), Float.TYPE, Float.valueOf(v));
        }
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !ValueProperty.class.isAssignableFrom(o.getClass())) {
            return false;
        }
        ValueProperty that = (ValueProperty) o;
        return Objects.equals(getName(), that.getName());
    }

    public int hashCode() {
        return Objects.hash(getName());
    }

    @Override // miui.android.animation.property.FloatProperty
    public String toString() {
        return "ValueProperty{name=" + getName() + '}';
    }
}
