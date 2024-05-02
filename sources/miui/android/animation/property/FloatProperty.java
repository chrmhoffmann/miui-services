package miui.android.animation.property;

import android.util.Property;
import com.android.server.wm.MiuiFreeformPinManagerService;
/* loaded from: classes.dex */
public abstract class FloatProperty<T> extends Property<T, Float> {
    final String mPropertyName;

    public abstract float getValue(T t);

    public abstract void setValue(T t, float f);

    /* JADX WARN: Multi-variable type inference failed */
    @Override // android.util.Property
    public /* bridge */ /* synthetic */ void set(Object obj, Float f) {
        set2((FloatProperty<T>) obj, f);
    }

    public FloatProperty(String name) {
        super(Float.class, name);
        this.mPropertyName = name;
    }

    @Override // android.util.Property
    public Float get(T object) {
        if (object == null) {
            return Float.valueOf((float) MiuiFreeformPinManagerService.EDGE_AREA);
        }
        return Float.valueOf(getValue(object));
    }

    /* renamed from: set */
    public final void set2(T object, Float value) {
        if (object != null) {
            setValue(object, value.floatValue());
        }
    }

    public String toString() {
        return getClass().getSimpleName() + "{mPropertyName='" + this.mPropertyName + "'}";
    }
}
