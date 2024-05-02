package miui.android.animation.utils;

import android.util.Log;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
/* loaded from: classes.dex */
public class StyleComposer {
    private static final String TAG = "StyleComposer";

    /* loaded from: classes.dex */
    public interface IInterceptor<T> {
        Object onMethod(Method method, Object[] objArr, T... tArr);

        boolean shouldIntercept(Method method, Object[] objArr);
    }

    public static <T> T compose(Class<T> interfaceClz, T... styles) {
        return (T) compose(interfaceClz, null, styles);
    }

    public static <T> T compose(final Class<T> interfaceClz, final IInterceptor interceptor, final T... styles) {
        InvocationHandler handler = new InvocationHandler() { // from class: miui.android.animation.utils.StyleComposer.1
            @Override // java.lang.reflect.InvocationHandler
            public Object invoke(Object proxy, Method method, Object[] args) {
                Object[] objArr;
                Object[] objArr2;
                Object retValue = null;
                IInterceptor iInterceptor = interceptor;
                if (iInterceptor != null && iInterceptor.shouldIntercept(method, args)) {
                    retValue = interceptor.onMethod(method, args, styles);
                } else {
                    for (Object obj : styles) {
                        try {
                            retValue = method.invoke(obj, args);
                        } catch (Exception e) {
                            Log.w(StyleComposer.TAG, "failed to invoke " + method + " for " + obj, e.getCause());
                        }
                    }
                }
                if (retValue != null) {
                    if (retValue == styles[objArr.length - 1]) {
                        return interfaceClz.cast(proxy);
                    }
                }
                return retValue;
            }
        };
        Object proxy = Proxy.newProxyInstance(interfaceClz.getClassLoader(), new Class[]{interfaceClz}, handler);
        if (interfaceClz.isInstance(proxy)) {
            return interfaceClz.cast(proxy);
        }
        return null;
    }
}
