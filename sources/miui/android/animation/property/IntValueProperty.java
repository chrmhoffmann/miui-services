package miui.android.animation.property;
/* loaded from: classes.dex */
public class IntValueProperty extends ValueProperty implements IIntValueProperty {
    public IntValueProperty(String name) {
        super(name);
    }

    @Override // miui.android.animation.property.IIntValueProperty
    public void setIntValue(Object o, int i) {
        if (o instanceof ValueTargetObject) {
            ((ValueTargetObject) o).setPropertyValue(getName(), Integer.TYPE, Integer.valueOf(i));
        }
    }

    @Override // miui.android.animation.property.IIntValueProperty
    public int getIntValue(Object o) {
        Integer value;
        if ((o instanceof ValueTargetObject) && (value = (Integer) ((ValueTargetObject) o).getPropertyValue(getName(), Integer.TYPE)) != null) {
            return value.intValue();
        }
        return 0;
    }

    @Override // miui.android.animation.property.ValueProperty, miui.android.animation.property.FloatProperty
    public String toString() {
        return "IntValueProperty{name=" + getName() + '}';
    }
}
