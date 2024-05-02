package miui.android.animation.controller;

import java.lang.reflect.Array;
import miui.android.animation.IAnimTarget;
import miui.android.animation.IStateStyle;
import miui.android.animation.ValueTarget;
import miui.android.animation.base.AnimConfig;
import miui.android.animation.base.AnimConfigLink;
import miui.android.animation.internal.AnimRunner;
import miui.android.animation.internal.PredictTask;
import miui.android.animation.listener.TransitionListener;
import miui.android.animation.property.FloatProperty;
import miui.android.animation.utils.EaseManager;
import miui.android.animation.utils.LogUtils;
/* loaded from: classes.dex */
public class FolmeState implements IFolmeStateStyle {
    IAnimTarget mTarget;
    StateManager mStateMgr = new StateManager();
    AnimConfigLink mConfigLink = new AnimConfigLink();
    private boolean mEnableAnim = true;

    public FolmeState(IAnimTarget target) {
        this.mTarget = target;
    }

    @Override // miui.android.animation.controller.IFolmeStateStyle
    public IAnimTarget getTarget() {
        return this.mTarget;
    }

    @Override // miui.android.animation.IStateStyle
    public IStateStyle setTo(Object tag) {
        return setTo(tag, new AnimConfig[0]);
    }

    @Override // miui.android.animation.IStateStyle
    public IStateStyle setTo(Object tag, AnimConfig... oneTimeConfig) {
        return setTo(tag, AnimConfigLink.linkConfig(oneTimeConfig));
    }

    private IStateStyle setTo(final Object tag, final AnimConfigLink oneTimeConfig) {
        IAnimTarget iAnimTarget = this.mTarget;
        if (iAnimTarget == null) {
            return this;
        }
        if ((tag instanceof Integer) || (tag instanceof Float)) {
            return setTo(tag, oneTimeConfig);
        }
        iAnimTarget.executeOnInitialized(new Runnable() { // from class: miui.android.animation.controller.FolmeState.1
            @Override // java.lang.Runnable
            public void run() {
                AnimState toState = FolmeState.this.getState(tag);
                IAnimTarget target = FolmeState.this.getTarget();
                if (LogUtils.isLogEnabled()) {
                    LogUtils.debug("FolmeState.setTo, state = " + toState, new Object[0]);
                }
                target.animManager.setTo(toState, oneTimeConfig);
                FolmeState.this.mStateMgr.clearTempState(toState);
            }
        });
        return this;
    }

    @Override // miui.android.animation.IStateStyle
    public IStateStyle to(AnimConfig... oneTimeConfig) {
        return to(getCurrentState(), oneTimeConfig);
    }

    @Override // miui.android.animation.IStateStyle
    public IStateStyle to(Object tag, AnimConfig... oneTimeConfig) {
        if ((tag instanceof AnimState) || this.mStateMgr.hasState(tag)) {
            return fromTo((Object) null, getState(tag), oneTimeConfig);
        }
        if (!tag.getClass().isArray()) {
            return to(tag, oneTimeConfig);
        }
        int length = Array.getLength(tag);
        Object[] argArray = new Object[oneTimeConfig.length + length];
        System.arraycopy(tag, 0, argArray, 0, length);
        System.arraycopy(oneTimeConfig, 0, argArray, length, oneTimeConfig.length);
        return to(argArray);
    }

    @Override // miui.android.animation.IStateStyle
    public IStateStyle then(Object tag, AnimConfig... oneTimeConfig) {
        this.mStateMgr.setStateFlags(tag, 1L);
        return to(tag, oneTimeConfig);
    }

    @Override // miui.android.animation.IStateStyle
    public IStateStyle fromTo(Object fromTag, Object toTag, AnimConfig... oneTimeConfig) {
        AnimConfigLink configLink = getConfigLink();
        for (AnimConfig config : oneTimeConfig) {
            configLink.add(config, new boolean[0]);
        }
        return fromTo(fromTag, toTag, configLink);
    }

    private IStateStyle fromTo(Object fromTag, Object toTag, AnimConfigLink oneTimeConfig) {
        if (this.mEnableAnim) {
            this.mStateMgr.setup(toTag);
            if (fromTag != null) {
                setTo(fromTag);
            }
            AnimState toState = getState(toTag);
            this.mStateMgr.addTempConfig(toState, oneTimeConfig);
            AnimRunner.getInst().run(this.mTarget, getState(fromTag), getState(toTag), oneTimeConfig);
            this.mStateMgr.clearTempState(toState);
            oneTimeConfig.clear();
        }
        return this;
    }

    @Override // miui.android.animation.IStateContainer
    public void enableDefaultAnim(boolean enable) {
        this.mEnableAnim = enable;
    }

    @Override // miui.android.animation.IStateContainer
    public void clean() {
        cancel();
    }

    @Override // miui.android.animation.ICancelableStyle
    public void cancel() {
        AnimRunner.getInst().cancel(this.mTarget, (FloatProperty[]) null);
    }

    @Override // miui.android.animation.ICancelableStyle
    public void cancel(FloatProperty... properties) {
        AnimRunner.getInst().cancel(this.mTarget, properties);
    }

    @Override // miui.android.animation.ICancelableStyle
    public void cancel(String... propertyNames) {
        IAnimTarget target = getTarget();
        if (propertyNames.length == 0 || !(target instanceof ValueTarget)) {
            return;
        }
        AnimRunner.getInst().cancel(this.mTarget, propertyNames);
    }

    @Override // miui.android.animation.ICancelableStyle
    public void end(Object... propertyList) {
        if (propertyList.length > 0) {
            if (propertyList[0] instanceof FloatProperty) {
                FloatProperty[] propList = new FloatProperty[propertyList.length];
                System.arraycopy(propertyList, 0, propList, 0, propertyList.length);
                AnimRunner.getInst().end(this.mTarget, propList);
                return;
            }
            String[] propList2 = new String[propertyList.length];
            System.arraycopy(propertyList, 0, propList2, 0, propertyList.length);
            AnimRunner.getInst().end(this.mTarget, propList2);
        }
    }

    @Override // miui.android.animation.IStateStyle
    public IStateStyle setTo(Object... propertyAndValues) {
        AnimConfigLink configLink = getConfigLink();
        AnimState state = this.mStateMgr.getSetToState(getTarget(), configLink, propertyAndValues);
        setTo(state, configLink);
        return this;
    }

    @Override // miui.android.animation.IStateStyle
    public IStateStyle to(Object... propertyAndValues) {
        return fromTo((Object) null, this.mStateMgr.getToState(getTarget(), getConfigLink(), propertyAndValues), new AnimConfig[0]);
    }

    @Override // miui.android.animation.IStateStyle
    public long predictDuration(Object... propertyAndValues) {
        IAnimTarget target = getTarget();
        AnimConfigLink configLink = getConfigLink();
        AnimState state = this.mStateMgr.getToState(target, configLink, propertyAndValues);
        long duration = PredictTask.predictDuration(target, null, state, configLink);
        this.mStateMgr.clearTempState(state);
        configLink.clear();
        return duration;
    }

    private AnimConfigLink getConfigLink() {
        return this.mConfigLink;
    }

    @Override // miui.android.animation.IStateStyle
    public IStateStyle then(Object... propertyAndValues) {
        AnimConfig config = new AnimConfig();
        AnimState animState = getState(propertyAndValues);
        animState.flags = 1L;
        return to(animState, config);
    }

    @Override // miui.android.animation.IStateStyle
    public IStateStyle autoSetTo(Object... propertyAndValues) {
        return this;
    }

    @Override // miui.android.animation.IStateStyle
    public IStateStyle setFlags(long flag) {
        IAnimTarget target = getTarget();
        target.setFlags(flag);
        return this;
    }

    @Override // miui.android.animation.controller.IFolmeStateStyle
    public AnimState getState(Object tag) {
        return this.mStateMgr.getState(tag);
    }

    @Override // miui.android.animation.controller.IFolmeStateStyle
    public void addState(AnimState state) {
        this.mStateMgr.addState(state);
    }

    @Override // miui.android.animation.IStateStyle
    public IStateStyle setup(Object tag) {
        this.mStateMgr.setup(tag);
        return this;
    }

    @Override // miui.android.animation.IStateStyle
    public IStateStyle set(Object tag) {
        this.mStateMgr.setup(tag);
        return this;
    }

    @Override // miui.android.animation.IStateStyle
    public IStateStyle addListener(TransitionListener listener) {
        this.mStateMgr.addListener(listener);
        return this;
    }

    @Override // miui.android.animation.IStateStyle
    public IStateStyle removeListener(TransitionListener listener) {
        this.mStateMgr.removeListener(listener);
        return this;
    }

    @Override // miui.android.animation.IStateStyle
    public IStateStyle addInitProperty(FloatProperty property, int value) {
        this.mStateMgr.addInitProperty(property, value);
        return this;
    }

    @Override // miui.android.animation.IStateStyle
    public IStateStyle addInitProperty(FloatProperty property, float value) {
        this.mStateMgr.addInitProperty(property, value);
        return this;
    }

    @Override // miui.android.animation.IStateStyle
    public IStateStyle addInitProperty(String propertyName, int value) {
        this.mStateMgr.addInitProperty(propertyName, value);
        return this;
    }

    @Override // miui.android.animation.IStateStyle
    public IStateStyle addInitProperty(String propertyName, float value) {
        this.mStateMgr.addInitProperty(propertyName, value);
        return this;
    }

    @Override // miui.android.animation.IStateStyle
    public IStateStyle add(FloatProperty property, int value, long flag) {
        this.mStateMgr.add(property, value, flag);
        return this;
    }

    @Override // miui.android.animation.IStateStyle
    public IStateStyle add(String propertyName, int value, long flag) {
        this.mStateMgr.add(propertyName, value, flag);
        return this;
    }

    @Override // miui.android.animation.IStateStyle
    public IStateStyle add(FloatProperty property, int value) {
        this.mStateMgr.add(property, value);
        return this;
    }

    @Override // miui.android.animation.IStateStyle
    public IStateStyle add(FloatProperty property, float value) {
        this.mStateMgr.add(property, value);
        return this;
    }

    @Override // miui.android.animation.IStateStyle
    public IStateStyle add(FloatProperty property, float value, long flag) {
        this.mStateMgr.add(property, value, flag);
        return this;
    }

    @Override // miui.android.animation.IStateStyle
    public IStateStyle add(String propertyName, int value) {
        this.mStateMgr.add(propertyName, value);
        return this;
    }

    @Override // miui.android.animation.IStateStyle
    public IStateStyle add(String propertyName, float value) {
        this.mStateMgr.add(propertyName, value);
        return this;
    }

    @Override // miui.android.animation.IStateStyle
    public IStateStyle add(String propertyName, float value, long flag) {
        this.mStateMgr.add(propertyName, value, flag);
        return this;
    }

    @Override // miui.android.animation.IStateStyle
    @Deprecated
    public IStateStyle setConfig(AnimConfig config, FloatProperty... properties) {
        return this;
    }

    @Override // miui.android.animation.IStateStyle
    public IStateStyle setEase(EaseManager.EaseStyle ease, FloatProperty... properties) {
        this.mStateMgr.setEase(ease, properties);
        return this;
    }

    @Override // miui.android.animation.IStateStyle
    public IStateStyle setEase(int style, float... factors) {
        this.mStateMgr.setEase(style, factors);
        return this;
    }

    @Override // miui.android.animation.IStateStyle
    public IStateStyle setEase(FloatProperty property, int style, float... factors) {
        this.mStateMgr.setEase(property, style, factors);
        return this;
    }

    @Override // miui.android.animation.IStateStyle
    public AnimState getCurrentState() {
        return this.mStateMgr.getCurrentState();
    }

    @Override // miui.android.animation.IStateStyle
    public IStateStyle setTransitionFlags(long flags, FloatProperty... properties) {
        StateManager stateManager = this.mStateMgr;
        stateManager.setTransitionFlags(stateManager.getCurrentState(), flags, properties);
        return this;
    }

    @Override // miui.android.animation.IStateContainer
    @Deprecated
    public void addConfig(Object tag, AnimConfig... configs) {
    }
}
