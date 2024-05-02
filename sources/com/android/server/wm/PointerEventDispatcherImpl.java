package com.android.server.wm;

import android.view.MotionEvent;
import com.miui.base.MiuiStubRegistry;
import com.miui.server.input.MiuiEventRunnable;
import com.miui.server.input.MiuiInputMonitor;
/* loaded from: classes.dex */
public class PointerEventDispatcherImpl implements PointerEventDispatcherStub {
    private final MiuiEventRunnable mEventRunnable = new MiuiEventRunnable();

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<PointerEventDispatcherImpl> {

        /* compiled from: PointerEventDispatcherImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final PointerEventDispatcherImpl INSTANCE = new PointerEventDispatcherImpl();
        }

        public PointerEventDispatcherImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public PointerEventDispatcherImpl provideNewInstance() {
            return new PointerEventDispatcherImpl();
        }
    }

    public void onEvent(MotionEvent event, String monitor) {
        this.mEventRunnable.setData(event, monitor);
        MiuiInputMonitor.getInstance().onEvent(this.mEventRunnable);
    }

    public void onFinishEvent() {
        MiuiInputMonitor.getInstance().onFinishEvent(this.mEventRunnable);
    }
}
