package miui.android.animation;

import java.util.concurrent.atomic.AtomicInteger;
import miui.android.animation.property.ColorProperty;
import miui.android.animation.property.FloatProperty;
import miui.android.animation.property.IIntValueProperty;
import miui.android.animation.property.IntValueProperty;
import miui.android.animation.property.ValueProperty;
import miui.android.animation.property.ValueTargetObject;
import miui.android.animation.property.ViewProperty;
/* loaded from: classes.dex */
public class ValueTarget extends IAnimTarget {
    private static final float DEFAULT_MIN_VALUE = 0.002f;
    static ITargetCreator sCreator = new ITargetCreator() { // from class: miui.android.animation.ValueTarget.1
        @Override // miui.android.animation.ITargetCreator
        public IAnimTarget createTarget(Object targetObject) {
            return new ValueTarget(targetObject);
        }
    };
    private AtomicInteger mMaxType;
    private ValueTargetObject mTargetObj;

    public ValueTarget() {
        this(null);
    }

    private ValueTarget(Object targetObj) {
        this.mMaxType = new AtomicInteger(1000);
        this.mTargetObj = new ValueTargetObject(targetObj == null ? Integer.valueOf(getId()) : targetObj);
    }

    @Override // miui.android.animation.IAnimTarget
    public void setValue(FloatProperty property, float value) {
        if (isPredefinedProperty(property)) {
            this.mTargetObj.setPropertyValue(property.getName(), Float.TYPE, Float.valueOf(value));
        } else {
            property.setValue(this.mTargetObj.getRealObject(), value);
        }
    }

    public float getValue(String propertyName) {
        return getValue(getFloatProperty(propertyName));
    }

    public void setValue(String propertyName, float value) {
        setValue(getFloatProperty(propertyName), value);
    }

    public int getIntValue(String propertyName) {
        return getIntValue(getIntValueProperty(propertyName));
    }

    public void setIntValue(String propertyName, int value) {
        setIntValue(getIntValueProperty(propertyName), value);
    }

    @Override // miui.android.animation.IAnimTarget
    public float getValue(FloatProperty property) {
        if (isPredefinedProperty(property)) {
            Float value = (Float) this.mTargetObj.getPropertyValue(property.getName(), Float.TYPE);
            if (value != null) {
                return value.floatValue();
            }
            return Float.MAX_VALUE;
        }
        return property.getValue(this.mTargetObj.getRealObject());
    }

    @Override // miui.android.animation.IAnimTarget
    public void setIntValue(IIntValueProperty property, int value) {
        if (isPredefinedProperty(property)) {
            this.mTargetObj.setPropertyValue(property.getName(), Integer.TYPE, Integer.valueOf(value));
        } else {
            property.setIntValue(this.mTargetObj.getRealObject(), value);
        }
    }

    @Override // miui.android.animation.IAnimTarget
    public int getIntValue(IIntValueProperty property) {
        if (isPredefinedProperty(property)) {
            Integer value = (Integer) this.mTargetObj.getPropertyValue(property.getName(), Integer.TYPE);
            if (value != null) {
                return value.intValue();
            }
            return Integer.MAX_VALUE;
        }
        return property.getIntValue(this.mTargetObj.getRealObject());
    }

    public double getVelocity(String propertyName) {
        return getVelocity(getFloatProperty(propertyName));
    }

    public void setVelocity(String propertyName, double velocity) {
        setVelocity(getFloatProperty(propertyName), velocity);
    }

    @Override // miui.android.animation.IAnimTarget
    public float getMinVisibleChange(Object key) {
        if ((key instanceof IIntValueProperty) && !(key instanceof ColorProperty)) {
            return 1.0f;
        }
        return super.getMinVisibleChange(key);
    }

    @Override // miui.android.animation.IAnimTarget
    public boolean isValid() {
        return this.mTargetObj.isValid();
    }

    @Override // miui.android.animation.IAnimTarget
    public Object getTargetObject() {
        return this.mTargetObj;
    }

    @Override // miui.android.animation.IAnimTarget
    public void clean() {
    }

    public FloatProperty createProperty(String name, Class<?> valueClass) {
        return (valueClass == Integer.TYPE || valueClass == Integer.class) ? new IntValueProperty(name) : new ValueProperty(name);
    }

    @Override // miui.android.animation.IAnimTarget
    public float getDefaultMinVisible() {
        return 0.002f;
    }

    private boolean isPredefinedProperty(Object property) {
        return (property instanceof ValueProperty) || (property instanceof ViewProperty) || (property instanceof ColorProperty);
    }

    public FloatProperty getFloatProperty(String propertyName) {
        return createProperty(propertyName, Float.TYPE);
    }

    public IIntValueProperty getIntValueProperty(String propertyName) {
        return (IIntValueProperty) createProperty(propertyName, Integer.TYPE);
    }
}
