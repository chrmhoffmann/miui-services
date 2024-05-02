package com.android.server.input.config;

import android.os.Parcel;
import com.android.server.input.MiInputManager;
import java.lang.reflect.Field;
/* loaded from: classes.dex */
public abstract class BaseInputConfig {
    public static final long PARCEL_NATIVE_PTR_INVALID = -1;

    public abstract int getConfigType();

    protected abstract void writeToParcel(Parcel parcel);

    public long getConfigNativePtr() {
        Parcel dest = Parcel.obtain();
        writeToParcel(dest);
        dest.marshall();
        dest.setDataPosition(0);
        try {
            Field privateStringFiled = Parcel.class.getDeclaredField("mNativePtr");
            privateStringFiled.setAccessible(true);
            return ((Long) privateStringFiled.get(dest)).longValue();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return -1L;
        } catch (NoSuchFieldException e2) {
            e2.printStackTrace();
            return -1L;
        }
    }

    public void flushToNative() {
        MiInputManager.getInstance().setInputConfig(getConfigType(), getConfigNativePtr());
    }
}
