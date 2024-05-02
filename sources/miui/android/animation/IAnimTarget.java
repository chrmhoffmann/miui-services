package miui.android.animation;

import android.util.ArrayMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import miui.android.animation.base.AnimConfigLink;
import miui.android.animation.controller.AnimState;
import miui.android.animation.internal.AnimManager;
import miui.android.animation.internal.NotifyManager;
import miui.android.animation.internal.TargetHandler;
import miui.android.animation.internal.TargetVelocityTracker;
import miui.android.animation.listener.ListenerNotifier;
import miui.android.animation.property.FloatProperty;
import miui.android.animation.property.IIntValueProperty;
import miui.android.animation.property.ValueProperty;
import miui.android.animation.property.ViewProperty;
import miui.android.animation.property.ViewPropertyExt;
import miui.android.animation.utils.CommonUtils;
/* loaded from: classes.dex */
public abstract class IAnimTarget<T> {
    public static final long FLAT_ONESHOT = 1;
    static final AtomicInteger sTargetIds = new AtomicInteger(Integer.MAX_VALUE);
    public final AnimManager animManager;
    long mFlags;
    public final TargetHandler handler = new TargetHandler(this);
    NotifyManager notifyManager = new NotifyManager(this);
    float mDefaultMinVisible = Float.MAX_VALUE;
    Map<Object, Float> mMinVisibleChanges = new ArrayMap();
    public final int id = sTargetIds.decrementAndGet();
    final TargetVelocityTracker mTracker = new TargetVelocityTracker();

    public abstract void clean();

    public abstract T getTargetObject();

    public IAnimTarget() {
        AnimManager animManager = new AnimManager();
        this.animManager = animManager;
        animManager.setTarget(this);
        setMinVisibleChange(0.1f, ViewProperty.ROTATION, ViewProperty.ROTATION_X, ViewProperty.ROTATION_Y);
        setMinVisibleChange(0.00390625f, ViewProperty.ALPHA, ViewProperty.AUTO_ALPHA, ViewPropertyExt.FOREGROUND, ViewPropertyExt.BACKGROUND);
        setMinVisibleChange(0.002f, ViewProperty.SCALE_X, ViewProperty.SCALE_Y);
    }

    public ListenerNotifier getNotifier() {
        return this.notifyManager.getNotifier();
    }

    public void setToNotify(AnimState state, AnimConfigLink configLink) {
        this.notifyManager.setToNotify(state, configLink);
    }

    public boolean isAnimRunning(FloatProperty... properties) {
        return this.animManager.isAnimRunning(properties);
    }

    public boolean allowAnimRun() {
        return true;
    }

    public int getId() {
        return this.id;
    }

    public void setFlags(long flags) {
        this.mFlags = flags;
    }

    public boolean hasFlags(long flags) {
        return CommonUtils.hasFlags(this.mFlags, flags);
    }

    public float getMinVisibleChange(Object key) {
        Float threshold = this.mMinVisibleChanges.get(key);
        if (threshold != null) {
            return threshold.floatValue();
        }
        float f = this.mDefaultMinVisible;
        if (f != Float.MAX_VALUE) {
            return f;
        }
        return getDefaultMinVisible();
    }

    public IAnimTarget setDefaultMinVisibleChange(float defaultMinVisible) {
        this.mDefaultMinVisible = defaultMinVisible;
        return this;
    }

    public IAnimTarget setMinVisibleChange(float minVisible, FloatProperty... properties) {
        for (FloatProperty prop : properties) {
            this.mMinVisibleChanges.put(prop, Float.valueOf(minVisible));
        }
        return this;
    }

    public IAnimTarget setMinVisibleChange(Object key, float minVisible) {
        this.mMinVisibleChanges.put(key, Float.valueOf(minVisible));
        return this;
    }

    public IAnimTarget setMinVisibleChange(float minVisible, String... names) {
        for (String name : names) {
            setMinVisibleChange(new ValueProperty(name), minVisible);
        }
        return this;
    }

    public void executeOnInitialized(Runnable task) {
        post(task);
    }

    public boolean isValid() {
        return true;
    }

    public void onFrameEnd(boolean isFinished) {
    }

    public void getLocationOnScreen(int[] location) {
        location[1] = 0;
        location[0] = 0;
    }

    public float getValue(FloatProperty property) {
        T targetObj = getTargetObject();
        if (targetObj != null) {
            return property.getValue(targetObj);
        }
        return Float.MAX_VALUE;
    }

    public void setValue(FloatProperty property, float value) {
        T targetObj = getTargetObject();
        if (targetObj != null && Math.abs(value) != Float.MAX_VALUE) {
            property.setValue(targetObj, value);
        }
    }

    public int getIntValue(IIntValueProperty property) {
        T targetObj = getTargetObject();
        if (targetObj != null) {
            return property.getIntValue(targetObj);
        }
        return Integer.MAX_VALUE;
    }

    public void setIntValue(IIntValueProperty property, int value) {
        T targetObj = getTargetObject();
        if (targetObj != null && Math.abs(value) != Integer.MAX_VALUE) {
            property.setIntValue(targetObj, value);
        }
    }

    public double getVelocity(FloatProperty property) {
        return this.animManager.getVelocity(property);
    }

    public void setVelocity(FloatProperty property, double v) {
        if (v != 3.4028234663852886E38d) {
            this.animManager.setVelocity(property, (float) v);
        }
    }

    public void post(Runnable task) {
        if (this.handler.threadId == Thread.currentThread().getId()) {
            task.run();
        } else {
            this.handler.post(task);
        }
    }

    public float getDefaultMinVisible() {
        return 1.0f;
    }

    public boolean shouldUseIntValue(FloatProperty property) {
        return property instanceof IIntValueProperty;
    }

    public void trackVelocity(FloatProperty property, double value) {
        this.mTracker.trackVelocity(this, property, value);
    }

    public String toString() {
        return "IAnimTarget{" + getTargetObject() + "}";
    }
}
