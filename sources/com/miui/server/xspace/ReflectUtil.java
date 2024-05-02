package com.miui.server.xspace;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
/* loaded from: classes.dex */
public class ReflectUtil {
    public static <T> T callObjectMethod(Object target, Class<T> returnType, String method, Class<?>[] parameterTypes, Object... values) throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Method declaredMethod = target.getClass().getDeclaredMethod(method, parameterTypes);
        declaredMethod.setAccessible(true);
        return (T) declaredMethod.invoke(target, values);
    }

    public static <T> T callObjectMethod2(Object target, Class<T> returnType, String method, Class<?>[] parameterTypes, Object... values) throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Method declaredMethod = target.getClass().getMethod(method, parameterTypes);
        declaredMethod.setAccessible(true);
        return (T) declaredMethod.invoke(target, values);
    }

    public static Object callObjectMethod(Object target, String method, Class<?>[] parameterTypes, Object... values) throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Method declaredMethod = target.getClass().getDeclaredMethod(method, parameterTypes);
        return declaredMethod.invoke(target, values);
    }

    public static Object callObjectMethod2(Object target, String method, Class<?>[] parameterTypes, Object... values) throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Method getMethod = target.getClass().getMethod(method, parameterTypes);
        return getMethod.invoke(target, values);
    }

    public static Object callObjectMethod(Object target, String method, Class<?> clazz, Class<?>[] parameterTypes, Object... values) throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Method declaredMethod = clazz.getDeclaredMethod(method, parameterTypes);
        declaredMethod.setAccessible(true);
        return declaredMethod.invoke(target, values);
    }

    public static <T> T callStaticObjectMethod(Class<?> clazz, Class<T> returnType, String method, Class<?>[] parameterTypes, Object... values) throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Method declaredMethod = clazz.getDeclaredMethod(method, parameterTypes);
        declaredMethod.setAccessible(true);
        return (T) declaredMethod.invoke(null, values);
    }

    public static Object callStaticObjectMethod(Class<?> clazz, String method, Class<?>[] parameterTypes, Object... values) throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Method declaredMethod = clazz.getDeclaredMethod(method, parameterTypes);
        declaredMethod.setAccessible(true);
        return declaredMethod.invoke(null, values);
    }

    public static void setObjectField(Object target, String field, Object value) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        Field declaredField = target.getClass().getDeclaredField(field);
        declaredField.setAccessible(true);
        declaredField.set(target, value);
    }

    public static void setObjectField(Object target, Class<?> clazz, String field, Object value) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        Field declaredField = clazz.getDeclaredField(field);
        declaredField.setAccessible(true);
        declaredField.set(target, value);
    }

    public static Object getObjectField(Object target, String field) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        Field declaredField = target.getClass().getDeclaredField(field);
        declaredField.setAccessible(true);
        return declaredField.get(target);
    }

    public static Object getObjectField2(Object target, String field) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        Field declaredField = target.getClass().getField(field);
        declaredField.setAccessible(true);
        return declaredField.get(target);
    }

    public static <T> T getObjectField(Object target, String field, Class<T> returnType) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        Field declaredField = target.getClass().getDeclaredField(field);
        declaredField.setAccessible(true);
        return (T) declaredField.get(target);
    }

    public static Object getObjectField(Object target, Class<?> clazz, String field) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        Field declaredField = clazz.getDeclaredField(field);
        declaredField.setAccessible(true);
        return declaredField.get(target);
    }

    public static void setStaticObjectField(Class<?> clazz, String field, Object value) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        Field declaredField = clazz.getDeclaredField(field);
        declaredField.setAccessible(true);
        declaredField.set(null, value);
    }

    public static Object getStaticObjectField(Class<?> clazz, String field) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        Field declaredField = clazz.getDeclaredField(field);
        declaredField.setAccessible(true);
        return declaredField.get(null);
    }

    public static <T> T getStaticObjectField(Class<?> clazz, String field, Class<T> returnType) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        Field declaredField = clazz.getDeclaredField(field);
        declaredField.setAccessible(true);
        return (T) declaredField.get(null);
    }

    public static Object callAnyObjectMethod(Class<? extends Object> clazz, Object target, String method, Class<?>[] parameterTypes, Object... values) throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Method declaredMethod = clazz.getDeclaredMethod(method, parameterTypes);
        declaredMethod.setAccessible(true);
        return declaredMethod.invoke(target, values);
    }
}
