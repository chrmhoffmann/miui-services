package miui.android.animation.listener;

import java.util.Collection;
import miui.android.animation.property.FloatProperty;
import miui.android.animation.property.IIntValueProperty;
/* loaded from: classes.dex */
public class TransitionListener {
    public TransitionListener() {
    }

    @Deprecated
    public TransitionListener(long fs) {
    }

    @Deprecated
    public void onUpdate(Object toTag, FloatProperty property, float value, boolean isCompleted) {
    }

    @Deprecated
    public void onUpdate(Object toTag, FloatProperty property, float value, float velocity, boolean isCompleted) {
    }

    @Deprecated
    public void onUpdate(Object toTag, IIntValueProperty property, int value, float velocity, boolean isCompleted) {
    }

    public void onUpdate(Object toTag, Collection<UpdateInfo> updateList) {
    }

    @Deprecated
    public void onBegin(Object toTag, UpdateInfo update) {
    }

    public void onBegin(Object toTag, Collection<UpdateInfo> updateList) {
    }

    @Deprecated
    public void onComplete(Object toTag, UpdateInfo update) {
    }

    @Deprecated
    public void onCancel(Object toTag, UpdateInfo update) {
    }

    public void onBegin(Object toTag) {
    }

    public void onComplete(Object toTag) {
    }

    public void onCancel(Object toTag) {
    }
}
