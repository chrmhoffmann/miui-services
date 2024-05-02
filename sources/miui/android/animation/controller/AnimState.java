package miui.android.animation.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import miui.android.animation.IAnimTarget;
import miui.android.animation.base.AnimConfig;
import miui.android.animation.base.AnimSpecialConfig;
import miui.android.animation.internal.AnimValueUtils;
import miui.android.animation.listener.UpdateInfo;
import miui.android.animation.property.FloatProperty;
import miui.android.animation.property.IIntValueProperty;
import miui.android.animation.property.ISpecificProperty;
import miui.android.animation.property.IntValueProperty;
import miui.android.animation.property.ValueProperty;
import miui.android.animation.property.ViewProperty;
import miui.android.animation.utils.CommonUtils;
import miui.android.animation.utils.ObjectPool;
/* loaded from: classes.dex */
public class AnimState {
    public static final long FLAG_IN_TOUCH = 4;
    public static final long FLAG_PARALLEL = 2;
    public static final long FLAG_QUEUE = 1;
    private static final int STEP = 100;
    private static final String TAG = "TAG_";
    public static final int VIEW_POS = 1000100;
    public static final int VIEW_SIZE = 1000000;
    private static final AtomicInteger sId = new AtomicInteger();
    public long flags;
    public final boolean isTemporary;
    private final AnimConfig mConfig;
    private final Map<Object, Double> mMap;
    private volatile Object mTag;
    IntValueProperty tempIntValueProperty;
    ValueProperty tempValueProperty;

    public static void alignState(AnimState state, Collection<UpdateInfo> updateList) {
        UpdateInfo update;
        for (UpdateInfo update2 : updateList) {
            if (!state.contains(update2.property)) {
                if (update2.useInt) {
                    state.add(update2.property, (int) update2.animInfo.startValue);
                } else {
                    state.add(update2.property, (float) update2.animInfo.startValue);
                }
            }
        }
        List<Object> delList = (List) ObjectPool.acquire(ArrayList.class, new Object[0]);
        for (Object key : state.keySet()) {
            if (key instanceof FloatProperty) {
                update = UpdateInfo.findBy(updateList, (FloatProperty) key);
            } else {
                update = UpdateInfo.findByName(updateList, (String) key);
            }
            if (update == null) {
                delList.add(key);
            }
        }
        for (Object key2 : delList) {
            state.remove(key2);
        }
        ObjectPool.release(delList);
    }

    public AnimState() {
        this(null, false);
    }

    public AnimState(Object t) {
        this(t, false);
    }

    public AnimState(Object t, boolean temporary) {
        this.tempValueProperty = new ValueProperty("");
        this.tempIntValueProperty = new IntValueProperty("");
        this.mConfig = new AnimConfig();
        this.mMap = new ConcurrentHashMap();
        setTag(t);
        this.isTemporary = temporary;
    }

    public final void setTag(Object tag) {
        this.mTag = tag != null ? tag : TAG + sId.incrementAndGet();
    }

    public void clear() {
        this.mConfig.clear();
        this.mMap.clear();
    }

    public void set(AnimState state) {
        if (state == null) {
            return;
        }
        setTag(state.mTag);
        append(state);
    }

    private void append(AnimState state) {
        this.mConfig.copy(state.mConfig);
        this.mMap.clear();
        this.mMap.putAll(state.mMap);
    }

    public Object getTag() {
        return this.mTag;
    }

    public AnimState add(String name, float value, long... flags) {
        if (flags.length > 0) {
            setConfigFlag(name, flags[0]);
        }
        return add(name, value);
    }

    public AnimState add(String name, int value, long... flags) {
        if (flags.length > 0) {
            setConfigFlag(name, flags[0] | 4);
        } else {
            setConfigFlag(name, getConfigFlags(name) | 4);
        }
        return add(name, value);
    }

    public AnimState add(ViewProperty property, float value, long... flags) {
        return add((FloatProperty) property, value, flags);
    }

    public AnimState add(ViewProperty property, int value, long... flags) {
        return add((FloatProperty) property, value, flags);
    }

    public AnimState add(FloatProperty property, float value, long... flags) {
        if (flags.length > 0) {
            setConfigFlag(property, flags[0]);
        }
        return add(property, value);
    }

    public AnimState add(FloatProperty property, int value, long... flags) {
        if (flags.length > 0) {
            setConfigFlag(property, flags[0] | 4);
        } else {
            setConfigFlag(property, getConfigFlags(property) | 4);
        }
        return add(property, value);
    }

    public AnimState add(Object key, double value) {
        setMapValue(key, value);
        return this;
    }

    public void setConfigFlag(Object key, long flag) {
        String name = key instanceof FloatProperty ? ((FloatProperty) key).getName() : (String) key;
        this.mConfig.queryAndCreateSpecial(name).flags = flag;
    }

    public boolean contains(Object property) {
        if (property == null) {
            return false;
        }
        if (this.mMap.containsKey(property)) {
            return true;
        }
        if (!(property instanceof FloatProperty)) {
            return false;
        }
        return this.mMap.containsKey(((FloatProperty) property).getName());
    }

    public boolean isEmpty() {
        return this.mMap.isEmpty();
    }

    public Set<Object> keySet() {
        return this.mMap.keySet();
    }

    public int getInt(IIntValueProperty property) {
        Double value = getMapValue(property);
        if (value != null) {
            return value.intValue();
        }
        return Integer.MAX_VALUE;
    }

    public int getInt(String name) {
        return getInt(new IntValueProperty(name));
    }

    public float getFloat(FloatProperty property) {
        Double value = getMapValue(property);
        if (value != null) {
            return value.floatValue();
        }
        return Float.MAX_VALUE;
    }

    public float getFloat(String name) {
        Double value = getMapValue(name);
        if (value != null) {
            return value.floatValue();
        }
        return Float.MAX_VALUE;
    }

    public double get(IAnimTarget target, FloatProperty property) {
        Double value = getMapValue(property);
        if (value != null) {
            return getProperValue(target, property, value.doubleValue());
        }
        return Double.MAX_VALUE;
    }

    private Double getMapValue(Object key) {
        Double value = this.mMap.get(key);
        if (value == null && (key instanceof FloatProperty)) {
            return this.mMap.get(((FloatProperty) key).getName());
        }
        return value;
    }

    private void setMapValue(Object key, double value) {
        if ((key instanceof FloatProperty) && this.mMap.containsKey(((FloatProperty) key).getName())) {
            this.mMap.put(((FloatProperty) key).getName(), Double.valueOf(value));
        } else {
            this.mMap.put(key, Double.valueOf(value));
        }
    }

    private double getProperValue(IAnimTarget target, FloatProperty property, double value) {
        long flag = getConfigFlags(property);
        boolean isDelta = CommonUtils.hasFlags(flag, 1L);
        if (isDelta || value == 1000000.0d || value == 1000100.0d || (property instanceof ISpecificProperty)) {
            double curValue = AnimValueUtils.getValue(target, property, value);
            if (isDelta && !AnimValueUtils.isInvalid(value)) {
                setConfigFlag(property, (-2) & flag);
                double curValue2 = curValue + value;
                setMapValue(property, curValue2);
                return curValue2;
            }
            return curValue;
        }
        return value;
    }

    public long getConfigFlags(Object key) {
        String name = key instanceof FloatProperty ? ((FloatProperty) key).getName() : (String) key;
        AnimSpecialConfig config = this.mConfig.getSpecialConfig(name);
        if (config != null) {
            return config.flags;
        }
        return 0L;
    }

    public AnimConfig getConfig() {
        return this.mConfig;
    }

    public AnimState remove(Object key) {
        this.mMap.remove(key);
        if (key instanceof FloatProperty) {
            this.mMap.remove(((FloatProperty) key).getName());
        }
        return this;
    }

    public FloatProperty getProperty(Object key) {
        if (key instanceof FloatProperty) {
            return (FloatProperty) key;
        }
        String name = (String) key;
        if (CommonUtils.hasFlags(getConfigFlags(name), 4L)) {
            FloatProperty property = new IntValueProperty(name);
            return property;
        }
        FloatProperty property2 = new ValueProperty(name);
        return property2;
    }

    public FloatProperty getTempProperty(Object key) {
        if (key instanceof FloatProperty) {
            return (FloatProperty) key;
        }
        String name = (String) key;
        ValueProperty property = CommonUtils.hasFlags(getConfigFlags(name), 4L) ? this.tempIntValueProperty : this.tempValueProperty;
        property.setName((String) key);
        return property;
    }

    public String toString() {
        return "\nAnimState{mTag='" + this.mTag + "', flags:" + this.flags + ", mMaps=" + ((Object) CommonUtils.mapToString(this.mMap, "    ")) + '}';
    }
}
