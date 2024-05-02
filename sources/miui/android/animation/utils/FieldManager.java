package miui.android.animation.utils;

import android.util.ArrayMap;
import android.util.Log;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
/* loaded from: classes.dex */
public class FieldManager {
    static final String GET = "get";
    static final String SET = "set";
    Map<String, MethodInfo> mMethodMap = new ArrayMap();
    Map<String, FieldInfo> mFieldMap = new ArrayMap();

    /* loaded from: classes.dex */
    public static class MethodInfo {
        Method method;

        MethodInfo() {
        }
    }

    /* loaded from: classes.dex */
    public static class FieldInfo {
        Field field;

        FieldInfo() {
        }
    }

    public synchronized <T> T getField(Object obj, String name, Class<T> clz) {
        if (obj == null) {
            return null;
        }
        MethodInfo getter = this.mMethodMap.get(name);
        if (getter == null) {
            getter = getMethod(obj, getMethodName(name, GET), this.mMethodMap, new Class[0]);
        }
        if (getter.method != null) {
            Object retValue = invokeMethod(obj, getter.method, new Object[0]);
            return (T) retToClz(retValue, clz);
        }
        FieldInfo fieldInfo = this.mFieldMap.get(name);
        if (fieldInfo == null) {
            fieldInfo = getField(obj, name, clz, this.mFieldMap);
        }
        if (fieldInfo.field == null) {
            return null;
        }
        return (T) getValueByField(obj, fieldInfo.field);
    }

    public synchronized <T> boolean setField(Object obj, String name, Class<T> clz, T value) {
        if (obj == null) {
            return false;
        }
        MethodInfo setter = this.mMethodMap.get(name);
        if (setter == null) {
            setter = getMethod(obj, getMethodName(name, SET), this.mMethodMap, clz);
        }
        if (setter.method != null) {
            invokeMethod(obj, setter.method, value);
            return true;
        }
        FieldInfo fieldInfo = this.mFieldMap.get(name);
        if (fieldInfo == null) {
            fieldInfo = getField(obj, name, clz, this.mFieldMap);
        }
        if (fieldInfo.field == null) {
            return false;
        }
        setValueByField(obj, fieldInfo.field, value);
        return true;
    }

    static <T> T retToClz(Object retValue, Class<T> clz) {
        if (!(retValue instanceof Number)) {
            return null;
        }
        Number number = (Number) retValue;
        if (clz == Float.class || clz == Float.TYPE) {
            return (T) Float.valueOf(number.floatValue());
        }
        if (clz == Integer.class || clz == Integer.TYPE) {
            return (T) Integer.valueOf(number.intValue());
        }
        throw new IllegalArgumentException("getPropertyValue, clz must be float or int instead of " + clz);
    }

    static String getMethodName(String propertyName, String prefix) {
        return prefix + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
    }

    static <T> T getValueByField(Object o, Field field) {
        try {
            return (T) field.get(o);
        } catch (Exception e) {
            return null;
        }
    }

    static <T> void setValueByField(Object o, Field field, T value) {
        try {
            field.set(o, value);
        } catch (Exception e) {
        }
    }

    static MethodInfo getMethod(Object obj, String name, Map<String, MethodInfo> map, Class<?>... parameterClass) {
        MethodInfo info = map.get(name);
        if (info == null) {
            MethodInfo info2 = new MethodInfo();
            info2.method = getMethod(obj, name, parameterClass);
            map.put(name, info2);
            return info2;
        }
        return info;
    }

    static Method getMethod(Object o, String name, Class<?>... parameterClass) {
        Method method = null;
        try {
            method = o.getClass().getDeclaredMethod(name, parameterClass);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e) {
            try {
                Method method2 = o.getClass().getMethod(name, parameterClass);
                return method2;
            } catch (NoSuchMethodException e2) {
                return method;
            }
        }
    }

    static FieldInfo getField(Object obj, String name, Class<?> clz, Map<String, FieldInfo> fieldMap) {
        FieldInfo info = fieldMap.get(name);
        if (info == null) {
            FieldInfo info2 = new FieldInfo();
            info2.field = getFieldByType(obj, name, clz);
            fieldMap.put(name, info2);
            return info2;
        }
        return info;
    }

    static Field getFieldByType(Object o, String name, Class<?> type) {
        Field field = null;
        try {
            field = o.getClass().getDeclaredField(name);
            field.setAccessible(true);
        } catch (NoSuchFieldException e) {
            try {
                field = o.getClass().getField(name);
            } catch (NoSuchFieldException e2) {
            }
        }
        if (field != null && field.getType() != type) {
            return null;
        }
        return field;
    }

    static <T> T invokeMethod(Object o, Method method, Object... parameter) {
        if (method != null) {
            try {
                return (T) method.invoke(o, parameter);
            } catch (Exception e) {
                Log.d(CommonUtils.TAG, "ValueProperty.invokeMethod failed, " + method.getName(), e);
                return null;
            }
        }
        return null;
    }
}
