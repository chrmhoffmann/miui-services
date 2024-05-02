package miui.android.animation;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;
import com.android.server.wm.MiuiFreeformPinManagerService;
import java.lang.ref.WeakReference;
import miui.android.animation.property.FloatProperty;
import miui.android.animation.property.ViewProperty;
import miui.android.animation.utils.CommonUtils;
import miui.android.animation.utils.KeyUtils;
/* loaded from: classes.dex */
public class ViewTarget extends IAnimTarget<View> {
    public static final ITargetCreator<View> sCreator = new ITargetCreator<View>() { // from class: miui.android.animation.ViewTarget.1
        public IAnimTarget createTarget(View targetObject) {
            return new ViewTarget(targetObject);
        }
    };
    private WeakReference<Context> mContextRef;
    private LifecycleCallbacks mLifecycleCallbacks;
    private ViewLifecyclerObserver mViewLifecyclerObserver;
    private WeakReference<View> mViewRef;

    private ViewTarget(View view) {
        this.mViewRef = new WeakReference<>(view);
        registerLifecycle(view.getContext());
    }

    private boolean registerLifecycle(Context context) {
        while (context != null) {
            if (context instanceof LifecycleOwner) {
                this.mContextRef = new WeakReference<>(context);
                if (this.mViewLifecyclerObserver == null) {
                    this.mViewLifecyclerObserver = new ViewLifecyclerObserver();
                }
                ((LifecycleOwner) context).getLifecycle().addObserver(this.mViewLifecyclerObserver);
                return true;
            } else if (context instanceof Activity) {
                if (Build.VERSION.SDK_INT >= 29) {
                    this.mContextRef = new WeakReference<>(context);
                    if (this.mLifecycleCallbacks == null) {
                        this.mLifecycleCallbacks = new LifecycleCallbacks();
                    }
                    ((Activity) context).registerActivityLifecycleCallbacks(this.mLifecycleCallbacks);
                    return true;
                }
                return false;
            } else {
                context = context instanceof ContextWrapper ? ((ContextWrapper) context).getBaseContext() : null;
            }
        }
        return false;
    }

    private boolean unRegisterLifecycle(Context context) {
        LifecycleCallbacks lifecycleCallbacks;
        if (context == null) {
            return false;
        }
        if (context instanceof LifecycleOwner) {
            if (this.mViewLifecyclerObserver != null) {
                ((LifecycleOwner) context).getLifecycle().removeObserver(this.mViewLifecyclerObserver);
            }
            this.mViewLifecyclerObserver = null;
            return true;
        } else if (Build.VERSION.SDK_INT < 29 || !(context instanceof Activity) || (lifecycleCallbacks = this.mLifecycleCallbacks) == null) {
            return false;
        } else {
            ((Activity) context).unregisterActivityLifecycleCallbacks(lifecycleCallbacks);
            this.mLifecycleCallbacks = null;
            return true;
        }
    }

    @Override // miui.android.animation.IAnimTarget
    public View getTargetObject() {
        return this.mViewRef.get();
    }

    @Override // miui.android.animation.IAnimTarget
    public void clean() {
    }

    @Override // miui.android.animation.IAnimTarget
    public boolean isValid() {
        View view = this.mViewRef.get();
        return view != null;
    }

    @Override // miui.android.animation.IAnimTarget
    public void getLocationOnScreen(int[] location) {
        View view = this.mViewRef.get();
        if (view == null) {
            location[1] = Integer.MAX_VALUE;
            location[0] = Integer.MAX_VALUE;
            return;
        }
        view.getLocationOnScreen(location);
    }

    @Override // miui.android.animation.IAnimTarget
    public void onFrameEnd(boolean isFinished) {
        View view = this.mViewRef.get();
        if (isFinished && view != null) {
            view.setTag(KeyUtils.KEY_FOLME_SET_HEIGHT, null);
            view.setTag(KeyUtils.KEY_FOLME_SET_WIDTH, null);
            view.setTag(KeyUtils.KEY_FOLME_SET_HOVER, Float.valueOf((float) MiuiFreeformPinManagerService.EDGE_AREA));
        }
    }

    @Override // miui.android.animation.IAnimTarget
    public void executeOnInitialized(final Runnable task) {
        final View view = this.mViewRef.get();
        if (view != null) {
            if (view.getVisibility() == 8 && !view.isLaidOut() && (view.getWidth() == 0 || view.getHeight() == 0)) {
                post(new Runnable() { // from class: miui.android.animation.ViewTarget.2
                    @Override // java.lang.Runnable
                    public void run() {
                        ViewTarget.this.initLayout(view, task);
                    }
                });
            } else {
                post(task);
            }
        }
    }

    public void initLayout(View view, Runnable task) {
        ViewParent parent = view.getParent();
        if (parent instanceof ViewGroup) {
            view.setTag(KeyUtils.KEY_FOLME_INIT_LAYOUT, true);
            ViewGroup vp = (ViewGroup) parent;
            int left = vp.getLeft();
            int top = vp.getTop();
            int visibility = view.getVisibility();
            if (visibility == 8) {
                view.setVisibility(4);
            }
            vp.measure(vp.getWidth(), vp.getHeight());
            vp.layout(left, top, vp.getWidth() + left, vp.getHeight() + top);
            view.setVisibility(visibility);
            task.run();
            view.setTag(KeyUtils.KEY_FOLME_INIT_LAYOUT, null);
        }
    }

    @Override // miui.android.animation.IAnimTarget
    public boolean shouldUseIntValue(FloatProperty property) {
        if (property == ViewProperty.WIDTH || property == ViewProperty.HEIGHT || property == ViewProperty.SCROLL_X || property == ViewProperty.SCROLL_Y) {
            return true;
        }
        return super.shouldUseIntValue(property);
    }

    @Override // miui.android.animation.IAnimTarget
    public boolean allowAnimRun() {
        View view = getTargetObject();
        return view != null && !Folme.isInDraggingState(view);
    }

    @Override // miui.android.animation.IAnimTarget
    public void post(Runnable task) {
        View view = getTargetObject();
        if (view == null) {
            return;
        }
        if (!this.handler.isInTargetThread() && view.isAttachedToWindow()) {
            view.post(task);
        } else {
            executeTask(task);
        }
    }

    private void executeTask(Runnable task) {
        try {
            task.run();
        } catch (Exception e) {
            Log.w(CommonUtils.TAG, "ViewTarget.executeTask failed, " + getTargetObject(), e);
        }
    }

    /* loaded from: classes.dex */
    public class ViewLifecyclerObserver implements LifecycleObserver {
        protected ViewLifecyclerObserver() {
            ViewTarget.this = this$0;
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        void onDestroy() {
            ViewTarget.this.cleanViewTarget();
        }
    }

    /* loaded from: classes.dex */
    public class LifecycleCallbacks implements Application.ActivityLifecycleCallbacks {
        protected LifecycleCallbacks() {
            ViewTarget.this = this$0;
        }

        @Override // android.app.Application.ActivityLifecycleCallbacks
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        }

        @Override // android.app.Application.ActivityLifecycleCallbacks
        public void onActivityStarted(Activity activity) {
        }

        @Override // android.app.Application.ActivityLifecycleCallbacks
        public void onActivityResumed(Activity activity) {
        }

        @Override // android.app.Application.ActivityLifecycleCallbacks
        public void onActivityPaused(Activity activity) {
        }

        @Override // android.app.Application.ActivityLifecycleCallbacks
        public void onActivityStopped(Activity activity) {
        }

        @Override // android.app.Application.ActivityLifecycleCallbacks
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        }

        @Override // android.app.Application.ActivityLifecycleCallbacks
        public void onActivityDestroyed(Activity activity) {
            ViewTarget.this.cleanViewTarget();
        }
    }

    public void cleanViewTarget() {
        WeakReference<Context> weakReference = this.mContextRef;
        if (weakReference != null) {
            unRegisterLifecycle(weakReference.get());
        }
        setCorner(MiuiFreeformPinManagerService.EDGE_AREA);
        Folme.clean(this);
    }

    private void setCorner(float radius) {
        View view = this.mViewRef.get();
        if (view != null) {
            view.setTag(KeyUtils.KEY_FOLME_SET_HOVER, Float.valueOf(radius));
        }
    }
}
