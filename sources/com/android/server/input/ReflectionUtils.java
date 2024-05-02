package com.android.server.input;

import android.util.Slog;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
/* loaded from: classes.dex */
public class ReflectionUtils {
    private static final String TAG = "InputFeatureReflection";
    private static MethodEntry sMethodEntry = new MethodEntry();
    private static volatile Map<MethodEntry, Method> sMethodEntryMap = new HashMap();

    static {
        initMethod(InputManagerService.class, "nativeSwitchTouchWorkMode", new Class[]{Long.TYPE, Integer.TYPE});
        initMethod(InputManagerService.class, "nativeSetDebugInput", new Class[]{Long.TYPE, Integer.TYPE});
    }

    private static void initMethod(Class whichClass, String methodName, Class[] paramsClass) {
        try {
            Method method = whichClass.getDeclaredMethod(methodName, paramsClass);
            if (method != null) {
                synchronized (sMethodEntryMap) {
                    sMethodEntryMap.put(new MethodEntry(whichClass, methodName, paramsClass, method), method);
                }
            }
        } catch (Exception e) {
            Slog.i(TAG, "Init method Fail!");
        }
    }

    public static Object callPrivateMethod(Object obj, String methodName, Object... params) {
        Class[] paramsClass = getParameterTypes(params);
        sMethodEntry.setClassName(obj.getClass()).setMethodName(methodName).setParmasClass(paramsClass);
        try {
            if (sMethodEntryMap.containsKey(sMethodEntry)) {
                sMethodEntryMap.get(sMethodEntry).setAccessible(true);
                return sMethodEntryMap.get(sMethodEntry).invoke(obj, params);
            }
            Method method = obj.getClass().getDeclaredMethod(methodName, paramsClass);
            if (method == null) {
                return null;
            }
            synchronized (sMethodEntryMap) {
                sMethodEntryMap.put(new MethodEntry(obj.getClass(), methodName, paramsClass, method), method);
            }
            method.setAccessible(true);
            return method.invoke(obj, params);
        } catch (Exception e) {
            Slog.i(TAG, "callPrivateMethod Fail!");
            return null;
        }
    }

    public static Object callPrivateMethod(Class whichClass, Object obj, String methodName, Object... params) {
        Class[] paramsClass = getParameterTypes(params);
        sMethodEntry.setClassName(obj.getClass()).setMethodName(methodName).setParmasClass(paramsClass);
        try {
            if (sMethodEntryMap.containsKey(sMethodEntry)) {
                sMethodEntryMap.get(sMethodEntry).setAccessible(true);
                return sMethodEntryMap.get(sMethodEntry).invoke(obj, params);
            }
            Method method = whichClass.getDeclaredMethod(methodName, paramsClass);
            if (method == null) {
                return null;
            }
            synchronized (sMethodEntryMap) {
                sMethodEntryMap.put(new MethodEntry(obj.getClass(), methodName, paramsClass, method), method);
            }
            method.setAccessible(true);
            return method.invoke(obj, params);
        } catch (Exception e) {
            Slog.i(TAG, "callPrivateMethod Fail!");
            return null;
        }
    }

    public static Object callStaticMethod(Class whichClass, String methodName, Object... params) {
        Class[] paramsClass = getParameterTypes(params);
        return callStaticMethod(whichClass, methodName, paramsClass, params);
    }

    public static Object callStaticMethod(Class whichClass, String methodName, Class[] paramsClass, Object... params) {
        sMethodEntry.setClassName(whichClass).setMethodName(methodName).setParmasClass(paramsClass);
        try {
            if (sMethodEntryMap.containsKey(sMethodEntry)) {
                sMethodEntryMap.get(sMethodEntry).setAccessible(true);
                return sMethodEntryMap.get(sMethodEntry).invoke(null, params);
            }
            Method method = whichClass.getDeclaredMethod(methodName, paramsClass);
            if (method == null) {
                return null;
            }
            synchronized (sMethodEntryMap) {
                sMethodEntryMap.put(new MethodEntry(whichClass, methodName, paramsClass, method), method);
            }
            method.setAccessible(true);
            return method.invoke(null, params);
        } catch (Exception e) {
            Slog.i(TAG, "callStaticMethod Fail!");
            return null;
        }
    }

    private static Class[] getParameterTypes(Object... args) {
        if (args == null) {
            return null;
        }
        Class[] parameterTypes = new Class[args.length];
        int j = args.length;
        for (int i = 0; i < j; i++) {
            if (args[i] instanceof Integer) {
                parameterTypes[i] = Integer.TYPE;
            } else if (args[i] instanceof Byte) {
                parameterTypes[i] = Byte.TYPE;
            } else if (args[i] instanceof Short) {
                parameterTypes[i] = Short.TYPE;
            } else if (args[i] instanceof Float) {
                parameterTypes[i] = Float.TYPE;
            } else if (args[i] instanceof Double) {
                parameterTypes[i] = Double.TYPE;
            } else if (args[i] instanceof Character) {
                parameterTypes[i] = Character.TYPE;
            } else if (args[i] instanceof Long) {
                parameterTypes[i] = Long.TYPE;
            } else if (args[i] instanceof Boolean) {
                parameterTypes[i] = Boolean.TYPE;
            } else {
                parameterTypes[i] = args[i].getClass();
            }
        }
        return parameterTypes;
    }

    /* loaded from: classes.dex */
    public static class MethodEntry {
        Class mClassName;
        Method mMethod;
        String mMethodName;
        Class[] mParmasClass;

        MethodEntry() {
        }

        MethodEntry(Class className, String methodName, Class[] parmasClass, Method method) {
            this.mClassName = className;
            this.mMethodName = methodName;
            this.mParmasClass = parmasClass;
            this.mMethod = method;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            MethodEntry that = (MethodEntry) o;
            if (Arrays.equals(this.mParmasClass, that.mParmasClass) && Objects.equals(this.mMethodName, that.mMethodName) && Objects.equals(this.mClassName, that.mClassName)) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            int result = Objects.hash(this.mMethodName, this.mClassName);
            return (result * 31) + Arrays.hashCode(this.mParmasClass);
        }

        public MethodEntry setParmasClass(Class[] parmasClass) {
            this.mParmasClass = parmasClass;
            return this;
        }

        public MethodEntry setMethodName(String methodName) {
            this.mMethodName = methodName;
            return this;
        }

        public MethodEntry setClassName(Class className) {
            this.mClassName = className;
            return this;
        }

        public void setmMethod(Method mMethod) {
            this.mMethod = mMethod;
        }

        public Method getmMethod() {
            return this.mMethod;
        }

        public String getMethodName() {
            return this.mMethodName;
        }

        public Class getClassName() {
            return this.mClassName;
        }

        public Class[] getParmasClass() {
            return this.mParmasClass;
        }
    }
}
