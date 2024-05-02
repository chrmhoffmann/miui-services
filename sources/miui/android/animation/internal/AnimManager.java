package miui.android.animation.internal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import miui.android.animation.IAnimTarget;
import miui.android.animation.base.AnimConfigLink;
import miui.android.animation.controller.AnimState;
import miui.android.animation.internal.TransitionInfo;
import miui.android.animation.listener.UpdateInfo;
import miui.android.animation.property.FloatProperty;
import miui.android.animation.property.IIntValueProperty;
import miui.android.animation.utils.CommonUtils;
import miui.android.animation.utils.LogUtils;
/* loaded from: classes.dex */
public class AnimManager implements TransitionInfo.IUpdateInfoCreator {
    IAnimTarget mTarget;
    private List<UpdateInfo> mUpdateList;
    final Set<Object> mStartAnim = new HashSet();
    final ConcurrentHashMap<FloatProperty, UpdateInfo> mUpdateMap = new ConcurrentHashMap<>();
    final ConcurrentHashMap<Object, TransitionInfo> mRunningInfo = new ConcurrentHashMap<>();
    final ConcurrentLinkedQueue<TransitionInfo> mWaitState = new ConcurrentLinkedQueue<>();
    private final Runnable mUpdateTask = new Runnable() { // from class: miui.android.animation.internal.AnimManager.1
        @Override // java.lang.Runnable
        public void run() {
            AnimManager.this.update(true);
        }
    };

    public void update(boolean toPage) {
        this.mTarget.handler.update(toPage);
    }

    public void runUpdate() {
        this.mTarget.post(this.mUpdateTask);
    }

    public void setTarget(IAnimTarget target) {
        this.mTarget = target;
    }

    public void getTransitionInfos(List<TransitionInfo> list) {
        for (TransitionInfo info : this.mRunningInfo.values()) {
            if (info.updateList != null && !info.updateList.isEmpty()) {
                list.add(info);
            }
        }
    }

    public void clear() {
        this.mStartAnim.clear();
        this.mUpdateMap.clear();
        this.mRunningInfo.clear();
        this.mWaitState.clear();
    }

    public int getTotalAnimCount() {
        int count = 0;
        for (TransitionInfo info : this.mRunningInfo.values()) {
            count += info.getAnimCount();
        }
        return count;
    }

    public boolean isAnimRunning(FloatProperty... properties) {
        if (!CommonUtils.isArrayEmpty(properties) || (this.mRunningInfo.isEmpty() && this.mWaitState.isEmpty())) {
            for (TransitionInfo info : this.mRunningInfo.values()) {
                if (containProperties(info, properties)) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    private boolean containProperties(TransitionInfo info, FloatProperty... properties) {
        for (FloatProperty property : properties) {
            if (info.containsProperty(property)) {
                return true;
            }
        }
        return false;
    }

    public void startAnim(TransitionInfo info) {
        if (pendState(info)) {
            LogUtils.debug(this + ".startAnim, pendState", new Object[0]);
            return;
        }
        TransitionInfo.sMap.put(Integer.valueOf(info.id), info);
        AnimRunner.sRunnerHandler.obtainMessage(1, info.id, 0).sendToTarget();
    }

    public void setupTransition(TransitionInfo info) {
        this.mRunningInfo.put(info.key, info);
        info.initUpdateList(this);
        info.setupTasks(true);
        removeSameAnim(info);
    }

    private void removeSameAnim(TransitionInfo info) {
        for (TransitionInfo runInfo : this.mRunningInfo.values()) {
            if (runInfo != info) {
                List<UpdateInfo> updateList = runInfo.updateList;
                if (this.mUpdateList == null) {
                    this.mUpdateList = new ArrayList();
                }
                for (UpdateInfo update : updateList) {
                    if (!info.to.contains(update.property)) {
                        this.mUpdateList.add(update);
                    }
                }
                if (this.mUpdateList.isEmpty()) {
                    notifyTransitionEnd(runInfo, 5, 4);
                } else if (this.mUpdateList.size() != runInfo.updateList.size()) {
                    runInfo.updateList = this.mUpdateList;
                    this.mUpdateList = null;
                    runInfo.setupTasks(false);
                } else {
                    this.mUpdateList.clear();
                }
            }
        }
    }

    public void notifyTransitionEnd(TransitionInfo info, int msg, int reason) {
        this.mRunningInfo.remove(info.key);
        if (this.mStartAnim.remove(info.key)) {
            TransitionInfo.sMap.put(Integer.valueOf(info.id), info);
            this.mTarget.handler.obtainMessage(msg, info.id, reason).sendToTarget();
        }
        if (!isAnimRunning(new FloatProperty[0])) {
            this.mUpdateMap.clear();
        }
    }

    private boolean pendState(TransitionInfo info) {
        if (CommonUtils.hasFlags(info.to.flags, 1L)) {
            this.mWaitState.add(info);
            return true;
        }
        return false;
    }

    public void setTo(AnimState to, AnimConfigLink config) {
        if (LogUtils.isLogEnabled()) {
            LogUtils.debug("setTo, target = " + this.mTarget, "to = " + to);
        }
        if (to.keySet().size() > 150) {
            AnimRunner.sRunnerHandler.addSetToState(this.mTarget, to);
        } else {
            setTargetValue(to, config);
        }
    }

    private void setTargetValue(AnimState to, AnimConfigLink config) {
        for (Object key : to.keySet()) {
            FloatProperty property = to.getTempProperty(key);
            double value = to.get(this.mTarget, property);
            UpdateInfo update = this.mTarget.animManager.mUpdateMap.get(property);
            if (update != null) {
                update.animInfo.setToValue = value;
            }
            if (property instanceof IIntValueProperty) {
                this.mTarget.setIntValue((IIntValueProperty) property, (int) value);
            } else {
                this.mTarget.setValue(property, (float) value);
            }
            this.mTarget.trackVelocity(property, value);
        }
        this.mTarget.setToNotify(to, config);
    }

    public double getVelocity(FloatProperty property) {
        return getUpdateInfo(property).velocity;
    }

    public void setVelocity(FloatProperty property, float velocity) {
        getUpdateInfo(property).velocity = velocity;
    }

    @Override // miui.android.animation.internal.TransitionInfo.IUpdateInfoCreator
    public UpdateInfo getUpdateInfo(FloatProperty property) {
        UpdateInfo update = this.mUpdateMap.get(property);
        if (update == null) {
            UpdateInfo update2 = new UpdateInfo(property);
            UpdateInfo prev = this.mUpdateMap.putIfAbsent(property, update2);
            return prev != null ? prev : update2;
        }
        return update;
    }
}
