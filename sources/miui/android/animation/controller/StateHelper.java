package miui.android.animation.controller;

import java.lang.reflect.Array;
import miui.android.animation.IAnimTarget;
import miui.android.animation.ValueTarget;
import miui.android.animation.base.AnimConfig;
import miui.android.animation.base.AnimConfigLink;
import miui.android.animation.listener.TransitionListener;
import miui.android.animation.property.FloatProperty;
import miui.android.animation.property.IIntValueProperty;
import miui.android.animation.property.IntValueProperty;
import miui.android.animation.property.ValueProperty;
import miui.android.animation.utils.EaseManager;
/* loaded from: classes.dex */
class StateHelper {
    static final ValueProperty DEFAULT_PROPERTY = new ValueProperty("defaultProperty");
    static final IntValueProperty DEFAULT_INT_PROPERTY = new IntValueProperty("defaultIntProperty");

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r0v3, types: [boolean] */
    /* JADX WARN: Type inference failed for: r0v4, types: [int] */
    /* JADX WARN: Type inference failed for: r0v5 */
    /* JADX WARN: Type inference failed for: r0v6, types: [int] */
    /* JADX WARN: Type inference failed for: r0v7, types: [int] */
    public void parse(IAnimTarget target, AnimState state, AnimConfigLink link, Object... propertyAndValues) {
        if (propertyAndValues.length == 0) {
            return;
        }
        ?? equals = propertyAndValues[0].equals(state.getTag());
        while (equals < propertyAndValues.length) {
            int i = equals == true ? 1 : 0;
            int i2 = equals == true ? 1 : 0;
            Object key = propertyAndValues[i];
            int i3 = equals == true ? 1 : 0;
            int i4 = equals == true ? 1 : 0;
            Object value = i3 + 1 < propertyAndValues.length ? propertyAndValues[equals + 1] : null;
            if ((key instanceof String) && (value instanceof String)) {
                equals++;
            } else {
                int i5 = equals == true ? 1 : 0;
                int i6 = equals == true ? 1 : 0;
                equals = setPropertyAndValue(target, state, link, key, value, i5, propertyAndValues);
            }
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:14:0x0034  */
    /* JADX WARN: Removed duplicated region for block: B:15:0x0037  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private int setPropertyAndValue(miui.android.animation.IAnimTarget r14, miui.android.animation.controller.AnimState r15, miui.android.animation.base.AnimConfigLink r16, java.lang.Object r17, java.lang.Object r18, int r19, java.lang.Object... r20) {
        /*
            r13 = this;
            r6 = r13
            r7 = r17
            r8 = 0
            r9 = r16
            boolean r0 = r13.checkAndSetAnimConfig(r9, r7)
            if (r0 != 0) goto L2d
            r10 = r14
            r11 = r18
            miui.android.animation.property.FloatProperty r0 = r13.getProperty(r14, r7, r11)
            r12 = r0
            if (r0 == 0) goto L30
            boolean r0 = r13.isDefaultProperty(r12)
            if (r0 == 0) goto L1f
            r4 = r19
            goto L22
        L1f:
            int r0 = r19 + 1
            r4 = r0
        L22:
            r0 = r13
            r1 = r14
            r2 = r15
            r3 = r12
            r5 = r20
            int r8 = r0.addProperty(r1, r2, r3, r4, r5)
            goto L32
        L2d:
            r10 = r14
            r11 = r18
        L30:
            r4 = r19
        L32:
            if (r8 <= 0) goto L37
            int r0 = r4 + r8
            goto L39
        L37:
            int r0 = r4 + 1
        L39:
            return r0
        */
        throw new UnsupportedOperationException("Method not decompiled: miui.android.animation.controller.StateHelper.setPropertyAndValue(miui.android.animation.IAnimTarget, miui.android.animation.controller.AnimState, miui.android.animation.base.AnimConfigLink, java.lang.Object, java.lang.Object, int, java.lang.Object[]):int");
    }

    private boolean isDefaultProperty(FloatProperty property) {
        return property == DEFAULT_PROPERTY || property == DEFAULT_INT_PROPERTY;
    }

    private boolean checkAndSetAnimConfig(AnimConfigLink link, Object obj) {
        if ((obj instanceof TransitionListener) || (obj instanceof EaseManager.EaseStyle)) {
            setTempConfig(link.getHead(), obj);
            return true;
        } else if (obj.getClass().isArray()) {
            boolean ret = false;
            int n = Array.getLength(obj);
            for (int i = 0; i < n; i++) {
                Object element = Array.get(obj, i);
                ret = addConfigToLink(link, element) || ret;
            }
            return ret;
        } else {
            boolean ret2 = addConfigToLink(link, obj);
            return ret2;
        }
    }

    private void setTempConfig(AnimConfig config, Object setObj) {
        if (!(setObj instanceof TransitionListener)) {
            if (setObj instanceof EaseManager.EaseStyle) {
                config.setEase((EaseManager.EaseStyle) setObj);
                return;
            }
            return;
        }
        config.addListeners((TransitionListener) setObj);
    }

    private boolean addConfigToLink(AnimConfigLink link, Object obj) {
        if (obj instanceof AnimConfig) {
            link.add((AnimConfig) obj, new boolean[0]);
            return true;
        }
        if (obj instanceof AnimConfigLink) {
            link.add((AnimConfigLink) obj, new boolean[0]);
        }
        return false;
    }

    private FloatProperty getProperty(IAnimTarget target, Object key, Object value) {
        if (key instanceof FloatProperty) {
            FloatProperty property = (FloatProperty) key;
            return property;
        } else if ((key instanceof String) && (target instanceof ValueTarget)) {
            Class<?> valueClass = value != null ? value.getClass() : null;
            FloatProperty property2 = ((ValueTarget) target).createProperty((String) key, valueClass);
            return property2;
        } else if (!(key instanceof Float)) {
            return null;
        } else {
            FloatProperty property3 = DEFAULT_PROPERTY;
            return property3;
        }
    }

    private int addProperty(IAnimTarget target, AnimState state, FloatProperty property, int index, Object... propertyAndValues) {
        Object value;
        if (property == null || (value = getPropertyValue(index, propertyAndValues)) == null || !addPropertyValue(state, property, value)) {
            return 0;
        }
        int delta = 0 + 1;
        if (setInitVelocity(target, property, index + 1, propertyAndValues)) {
            return delta + 1;
        }
        return delta;
    }

    private Object getPropertyValue(int index, Object... propertyAndValues) {
        if (index < propertyAndValues.length) {
            return propertyAndValues[index];
        }
        return null;
    }

    private boolean setInitVelocity(IAnimTarget target, FloatProperty property, int index, Object... propertyAndValues) {
        if (index >= propertyAndValues.length) {
            return false;
        }
        Object secondArg = propertyAndValues[index];
        if (!(secondArg instanceof Float)) {
            return false;
        }
        target.setVelocity(property, ((Float) secondArg).floatValue());
        return true;
    }

    private boolean addPropertyValue(AnimState state, FloatProperty property, Object value) {
        boolean isInt = value instanceof Integer;
        if (isInt || (value instanceof Float) || (value instanceof Double)) {
            if (property instanceof IIntValueProperty) {
                state.add(property, toInt(value, isInt));
                return true;
            }
            state.add(property, toFloat(value, isInt));
            return true;
        }
        return false;
    }

    private int toInt(Object value, boolean isInt) {
        return isInt ? ((Integer) value).intValue() : (int) ((Float) value).floatValue();
    }

    private float toFloat(Object value, boolean isInt) {
        return isInt ? ((Integer) value).intValue() : ((Float) value).floatValue();
    }
}
