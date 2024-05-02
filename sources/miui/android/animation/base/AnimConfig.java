package miui.android.animation.base;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import miui.android.animation.listener.TransitionListener;
import miui.android.animation.property.FloatProperty;
import miui.android.animation.utils.CommonUtils;
import miui.android.animation.utils.EaseManager;
/* loaded from: classes.dex */
public class AnimConfig {
    public static final long FLAG_DELTA = 1;
    public static final long FLAG_INIT = 2;
    public static final long FLAG_INT = 4;
    public static final int TINT_ALPHA = 0;
    public static final int TINT_OPAQUE = 1;
    public static final EaseManager.EaseStyle sDefEase = EaseManager.getStyle(-2, 0.85f, 0.3f);
    public long delay;
    public EaseManager.EaseStyle ease;
    public long flags;
    public float fromSpeed;
    public final HashSet<TransitionListener> listeners;
    private final Map<String, AnimSpecialConfig> mSpecialNameMap;
    @Deprecated
    public long minDuration;
    public Object tag;
    public int tintMode;

    public AnimConfig() {
        this(false);
    }

    public AnimConfig(boolean isSpecial) {
        this.fromSpeed = Float.MAX_VALUE;
        if (!isSpecial) {
            this.mSpecialNameMap = new HashMap();
            this.listeners = new HashSet<>();
            return;
        }
        this.mSpecialNameMap = null;
        this.listeners = null;
    }

    public AnimConfig(AnimConfig src) {
        this(false);
        copy(src);
    }

    public void copy(AnimConfig src) {
        if (src != null && src != this) {
            this.delay = src.delay;
            this.ease = src.ease;
            this.listeners.addAll(src.listeners);
            this.tag = src.tag;
            this.flags = src.flags;
            this.fromSpeed = src.fromSpeed;
            this.minDuration = src.minDuration;
            this.tintMode = src.tintMode;
            Map<String, AnimSpecialConfig> map = this.mSpecialNameMap;
            if (map != null) {
                map.clear();
                this.mSpecialNameMap.putAll(src.mSpecialNameMap);
            }
        }
    }

    public void clear() {
        this.delay = 0L;
        this.ease = null;
        this.listeners.clear();
        this.tag = null;
        this.flags = 0L;
        this.fromSpeed = Float.MAX_VALUE;
        this.minDuration = 0L;
        this.tintMode = 0;
        Map<String, AnimSpecialConfig> map = this.mSpecialNameMap;
        if (map != null) {
            map.clear();
        }
    }

    public AnimConfig setEase(EaseManager.EaseStyle e) {
        this.ease = e;
        return this;
    }

    public AnimConfig setEase(int style, float... factors) {
        this.ease = EaseManager.getStyle(style, factors);
        return this;
    }

    public AnimConfig setDelay(long d) {
        this.delay = d;
        return this;
    }

    public AnimConfig setFromSpeed(float speed) {
        this.fromSpeed = speed;
        return this;
    }

    public AnimConfig addListeners(TransitionListener... l) {
        Collections.addAll(this.listeners, l);
        return this;
    }

    public AnimConfig setTintMode(int mode) {
        this.tintMode = mode;
        return this;
    }

    public void addSpecialConfigs(AnimConfig src) {
        this.mSpecialNameMap.putAll(src.mSpecialNameMap);
    }

    public AnimConfig removeListeners(TransitionListener... l) {
        if (l.length == 0) {
            this.listeners.clear();
        } else {
            this.listeners.removeAll(Arrays.asList(l));
        }
        return this;
    }

    public AnimConfig setTag(Object t) {
        this.tag = t;
        return this;
    }

    public AnimConfig setMinDuration(long md) {
        this.minDuration = md;
        return this;
    }

    public AnimConfig setSpecial(String name, long delay, float... velocity) {
        return setSpecial(name, (EaseManager.EaseStyle) null, delay, velocity);
    }

    public AnimConfig setSpecial(String name, EaseManager.EaseStyle ease, float... velocity) {
        return setSpecial(name, ease, 0L, velocity);
    }

    public AnimConfig setSpecial(String name, EaseManager.EaseStyle ease, long delay, float... velocity) {
        AnimSpecialConfig sc = queryAndCreateSpecial(name, true);
        setSpecial(sc, ease, delay, velocity);
        return this;
    }

    public AnimConfig setSpecial(FloatProperty property, long delay, float... velocity) {
        return setSpecial(property, (EaseManager.EaseStyle) null, delay, velocity);
    }

    public AnimConfig setSpecial(FloatProperty property, EaseManager.EaseStyle ease, float... velocity) {
        setSpecial(property, ease, -1L, velocity);
        return this;
    }

    public AnimConfig setSpecial(FloatProperty property, EaseManager.EaseStyle ease, long delay, float... velocity) {
        AnimSpecialConfig sc = queryAndCreateSpecial(property, true);
        setSpecial(sc, ease, delay, velocity);
        return this;
    }

    public void setSpecial(AnimSpecialConfig sc, EaseManager.EaseStyle ease, long delay, float... velocity) {
        if (ease != null) {
            sc.setEase(ease);
        }
        if (delay > 0) {
            sc.setDelay(delay);
        }
        if (velocity.length > 0) {
            sc.setFromSpeed(velocity[0]);
        }
    }

    public AnimSpecialConfig getSpecialConfig(FloatProperty property) {
        return queryAndCreateSpecial(property, false);
    }

    public AnimSpecialConfig queryAndCreateSpecial(FloatProperty property) {
        return queryAndCreateSpecial(property, true);
    }

    public AnimSpecialConfig queryAndCreateSpecial(String name) {
        return queryAndCreateSpecial(name, true);
    }

    private AnimSpecialConfig queryAndCreateSpecial(FloatProperty property, boolean create) {
        if (property == null) {
            return null;
        }
        return queryAndCreateSpecial(property.getName(), create);
    }

    public AnimSpecialConfig getSpecialConfig(String name) {
        return queryAndCreateSpecial(name, false);
    }

    public AnimConfig setSpecial(FloatProperty property, AnimSpecialConfig sc) {
        if (sc != null) {
            this.mSpecialNameMap.put(property.getName(), sc);
        } else {
            this.mSpecialNameMap.remove(property.getName());
        }
        return this;
    }

    public AnimConfig setSpecial(String name, AnimSpecialConfig sc) {
        if (sc != null) {
            this.mSpecialNameMap.put(name, sc);
        } else {
            this.mSpecialNameMap.remove(name);
        }
        return this;
    }

    private AnimSpecialConfig queryAndCreateSpecial(String name, boolean create) {
        AnimSpecialConfig sc = this.mSpecialNameMap.get(name);
        if (sc == null && create) {
            AnimSpecialConfig sc2 = new AnimSpecialConfig();
            this.mSpecialNameMap.put(name, sc2);
            return sc2;
        }
        return sc;
    }

    public String toString() {
        return "AnimConfig{delay=" + this.delay + ", minDuration=" + this.minDuration + ", ease=" + this.ease + ", fromSpeed=" + this.fromSpeed + ", tintMode=" + this.tintMode + ", tag=" + this.tag + ", flags=" + this.flags + ", listeners=" + this.listeners + ", specialNameMap = " + ((Object) CommonUtils.mapToString(this.mSpecialNameMap, "    ")) + '}';
    }
}
