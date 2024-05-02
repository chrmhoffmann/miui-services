package miui.android.animation.controller;

import android.util.ArrayMap;
import java.util.Map;
import miui.android.animation.IAnimTarget;
import miui.android.animation.base.AnimConfig;
import miui.android.animation.base.AnimConfigLink;
import miui.android.animation.base.AnimSpecialConfig;
import miui.android.animation.listener.TransitionListener;
import miui.android.animation.property.FloatProperty;
import miui.android.animation.utils.EaseManager;
/* loaded from: classes.dex */
public class StateManager {
    static final String TAG_AUTO_SET_TO = "autoSetTo";
    static final String TAG_SET_TO = "defaultSetTo";
    static final String TAG_TO = "defaultTo";
    Object mCurTag;
    final Map<Object, AnimState> mStateMap = new ArrayMap();
    final AnimState mToState = new AnimState(TAG_TO, true);
    final AnimState mSetToState = new AnimState(TAG_SET_TO, true);
    final AnimState mAutoSetToState = new AnimState(TAG_AUTO_SET_TO, true);
    StateHelper mStateHelper = new StateHelper();

    public boolean hasState(Object tag) {
        return this.mStateMap.containsKey(tag);
    }

    public void addState(AnimState state) {
        this.mStateMap.put(state.getTag(), state);
    }

    public AnimState getState(Object tag) {
        return getState(tag, true);
    }

    public AnimState getSetToState(IAnimTarget target, AnimConfigLink link, Object... propertyAndValues) {
        AnimState tempState = getStateByArgs(this.mSetToState, propertyAndValues);
        setAnimState(target, tempState, link, propertyAndValues);
        return tempState;
    }

    public AnimState getToState(IAnimTarget target, AnimConfigLink link, Object... propertyAndValues) {
        AnimState state = getStateByArgs(getCurrentState(), propertyAndValues);
        setAnimState(target, state, link, propertyAndValues);
        return state;
    }

    private AnimState getState(Object tag, boolean create) {
        if (tag == null) {
            return null;
        }
        if (tag instanceof AnimState) {
            return (AnimState) tag;
        }
        AnimState state = this.mStateMap.get(tag);
        if (state == null && create) {
            AnimState state2 = new AnimState(tag);
            addState(state2);
            return state2;
        }
        return state;
    }

    public void clear() {
        this.mStateMap.clear();
    }

    public AnimState setup(Object tag) {
        AnimState state;
        if (tag instanceof AnimState) {
            state = (AnimState) tag;
        } else {
            state = this.mStateMap.get(tag);
            if (state == null) {
                state = new AnimState(tag);
                addState(state);
            }
        }
        this.mCurTag = state;
        return state;
    }

    public void addListener(TransitionListener listener) {
        getCurrentState().getConfig().addListeners(listener);
    }

    public void removeListener(TransitionListener listener) {
        getCurrentState().getConfig().removeListeners(listener);
    }

    public void setEase(EaseManager.EaseStyle ease, FloatProperty... properties) {
        AnimConfig config = getCurrentState().getConfig();
        if (properties.length == 0) {
            config.setEase(ease);
            return;
        }
        for (FloatProperty property : properties) {
            config.setSpecial(property, ease, new float[0]);
        }
    }

    public void setEase(int style, float... factors) {
        getCurrentState().getConfig().setEase(style, factors);
    }

    public void setEase(FloatProperty property, int style, float... factors) {
        getCurrentState().getConfig().setSpecial(property, style, factors);
    }

    public void setStateFlags(Object tag, long flags) {
        AnimState state = getState(tag);
        state.flags = flags;
    }

    public void setTransitionFlags(Object tag, long flags, FloatProperty... properties) {
        AnimState state = getState(tag);
        AnimConfig config = state.getConfig();
        if (properties.length == 0) {
            config.flags = flags;
            return;
        }
        for (FloatProperty property : properties) {
            AnimSpecialConfig sc = config.getSpecialConfig(property);
            if (sc == null) {
                sc = new AnimSpecialConfig();
                config.setSpecial(property, sc);
            }
            sc.flags = flags;
        }
    }

    public void addInitProperty(FloatProperty property, int value) {
        add(property, value, 2L);
    }

    public void addInitProperty(FloatProperty property, float value) {
        add(property, value, 2L);
    }

    public void addInitProperty(String propertyName, int value) {
        add(propertyName, value, 2L);
    }

    public void addInitProperty(String propertyName, float value) {
        add(propertyName, value, 2L);
    }

    public void add(String propertyName, float value) {
        getCurrentState().add(propertyName, value);
    }

    public void add(String propertyName, int value) {
        getCurrentState().add(propertyName, value);
    }

    public void add(String propertyName, float value, long flag) {
        AnimState state = getCurrentState();
        state.setConfigFlag(propertyName, flag);
        state.add(propertyName, value);
    }

    public void add(String propertyName, int value, long flag) {
        AnimState state = getCurrentState();
        state.setConfigFlag(propertyName, flag);
        state.add(propertyName, value);
    }

    public void add(FloatProperty property, int value) {
        getCurrentState().add(property, value);
    }

    public void add(FloatProperty property, float value) {
        getCurrentState().add(property, value);
    }

    public void add(FloatProperty property, int value, long flag) {
        AnimState state = getCurrentState();
        state.setConfigFlag(property, flag);
        state.add(property, value);
    }

    public void add(FloatProperty property, float value, long flag) {
        AnimState state = getCurrentState();
        state.setConfigFlag(property, flag);
        state.add(property, value);
    }

    public AnimState getCurrentState() {
        if (this.mCurTag == null) {
            this.mCurTag = this.mToState;
        }
        return getState(this.mCurTag);
    }

    public void addTempConfig(AnimState toState, AnimConfigLink configLink) {
        AnimState animState = this.mToState;
        if (toState != animState) {
            configLink.add(animState.getConfig(), new boolean[0]);
        }
    }

    public void clearTempState(AnimState state) {
        if (state == this.mToState || state == this.mSetToState) {
            state.clear();
        }
    }

    private AnimState getStateByArgs(Object defaultTag, Object... propertyAndValues) {
        AnimState state = null;
        if (propertyAndValues.length > 0 && (state = getState(propertyAndValues[0], false)) == null) {
            state = getStateByName(propertyAndValues);
        }
        if (state == null) {
            AnimState state2 = getState(defaultTag);
            return state2;
        }
        return state;
    }

    private AnimState getStateByName(Object... propertyAndValues) {
        Object first = propertyAndValues[0];
        Object second = propertyAndValues.length > 1 ? propertyAndValues[1] : null;
        if (!(first instanceof String) || !(second instanceof String)) {
            return null;
        }
        return getState(first, true);
    }

    private void setAnimState(IAnimTarget target, AnimState state, AnimConfigLink link, Object... propertyAndValues) {
        this.mStateHelper.parse(target, state, link, propertyAndValues);
    }
}
