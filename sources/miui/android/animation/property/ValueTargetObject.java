package miui.android.animation.property;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import miui.android.animation.utils.CommonUtils;
import miui.android.animation.utils.FieldManager;
/* loaded from: classes.dex */
public class ValueTargetObject {
    private WeakReference<Object> mRef;
    private Object mTempObj;
    private FieldManager mFieldManager = new FieldManager();
    private Map<String, Object> mValueMap = new ConcurrentHashMap();

    public ValueTargetObject(Object o) {
        if (CommonUtils.isBuiltInClass(o.getClass())) {
            this.mTempObj = o;
        } else {
            this.mRef = new WeakReference<>(o);
        }
    }

    public boolean isValid() {
        return getRealObject() != null;
    }

    public Object getRealObject() {
        WeakReference<Object> weakReference = this.mRef;
        return weakReference != null ? weakReference.get() : this.mTempObj;
    }

    public <T> T getPropertyValue(String propertyName, Class<T> clz) {
        Object target = getRealObject();
        if (target == null || this.mTempObj == target) {
            return (T) this.mValueMap.get(propertyName);
        }
        T value = (T) this.mValueMap.get(propertyName);
        return value != null ? value : (T) this.mFieldManager.getField(target, propertyName, clz);
    }

    public <T> void setPropertyValue(String propertyName, Class<T> clz, T value) {
        Object target = getRealObject();
        if (target == null || this.mTempObj == target) {
            this.mValueMap.put(propertyName, value);
        } else if (this.mValueMap.containsKey(propertyName) || !this.mFieldManager.setField(target, propertyName, clz, value)) {
            this.mValueMap.put(propertyName, value);
        }
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (o.getClass() != getClass()) {
            Object obj = this.mTempObj;
            if (obj != null) {
                return Objects.equals(obj, o);
            }
            Object refObj = getRealObject();
            if (refObj == null) {
                return false;
            }
            return Objects.equals(refObj, o);
        }
        ValueTargetObject that = (ValueTargetObject) o;
        Object obj2 = this.mTempObj;
        if (obj2 != null) {
            return Objects.equals(obj2, that.mTempObj);
        }
        return Objects.equals(getRealObject(), that.getRealObject());
    }

    public int hashCode() {
        Object obj = this.mTempObj;
        if (obj != null) {
            return obj.hashCode();
        }
        Object obj2 = getRealObject();
        if (obj2 != null) {
            return obj2.hashCode();
        }
        return 0;
    }

    public String toString() {
        return "ValueTargetObject{" + getRealObject() + "}";
    }
}
