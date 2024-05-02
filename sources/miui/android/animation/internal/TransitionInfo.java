package miui.android.animation.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import miui.android.animation.IAnimTarget;
import miui.android.animation.base.AnimConfig;
import miui.android.animation.base.AnimConfigLink;
import miui.android.animation.controller.AnimState;
import miui.android.animation.listener.UpdateInfo;
import miui.android.animation.property.ColorProperty;
import miui.android.animation.property.FloatProperty;
import miui.android.animation.utils.LinkNode;
import miui.android.animation.utils.LogUtils;
/* loaded from: classes.dex */
public class TransitionInfo extends LinkNode<TransitionInfo> {
    public volatile AnimState from;
    public final int id;
    public volatile Object key;
    public volatile long startTime;
    public final Object tag;
    public final IAnimTarget target;
    public volatile AnimState to;
    public volatile List<UpdateInfo> updateList;
    public static final Map<Integer, TransitionInfo> sMap = new ConcurrentHashMap();
    private static final AtomicInteger sIdGenerator = new AtomicInteger();
    public volatile AnimConfig config = new AnimConfig();
    public List<AnimTask> animTasks = new ArrayList();
    private final AnimStats mAnimStats = new AnimStats();

    /* loaded from: classes.dex */
    public interface IUpdateInfoCreator {
        UpdateInfo getUpdateInfo(FloatProperty floatProperty);
    }

    public static void decreaseStartCountForDelayAnim(AnimTask task, AnimStats stats, UpdateInfo update, byte op) {
        if (task != null && op == 1 && update.animInfo.delay > 0 && task.animStats.startCount > 0) {
            task.animStats.startCount--;
            stats.startCount--;
        }
    }

    public TransitionInfo(IAnimTarget target, AnimState f, AnimState t, AnimConfigLink c) {
        int incrementAndGet = sIdGenerator.incrementAndGet();
        this.id = incrementAndGet;
        this.target = target;
        this.from = getState(f);
        this.to = getState(t);
        Object tag = this.to.getTag();
        this.tag = tag;
        if (t.isTemporary) {
            this.key = tag + String.valueOf(incrementAndGet);
        } else {
            this.key = tag;
        }
        this.updateList = null;
        initValueForColorProperty();
        this.config.copy(t.getConfig());
        if (c != null) {
            c.addTo(this.config);
        }
    }

    public void setupTasks(boolean isInit) {
        int animCount = this.updateList.size();
        int splitCount = Math.max(1, animCount / AnimTask.MAX_SINGLE_TASK_SIZE);
        int singleCount = (int) Math.ceil(animCount / splitCount);
        if (this.animTasks.size() > splitCount) {
            List<AnimTask> list = this.animTasks;
            list.subList(splitCount, list.size()).clear();
        } else {
            for (int i = this.animTasks.size(); i < splitCount; i++) {
                this.animTasks.add(new AnimTask());
            }
        }
        int startPos = 0;
        for (AnimTask task : this.animTasks) {
            task.info = this;
            int amount = startPos + singleCount > animCount ? animCount - startPos : singleCount;
            task.setup(startPos, amount);
            if (isInit) {
                task.animStats.startCount = amount;
            } else {
                task.updateAnimStats();
            }
            startPos += amount;
        }
    }

    private AnimState getState(AnimState state) {
        if (state != null && state.isTemporary) {
            AnimState s = new AnimState();
            s.set(state);
            return s;
        }
        return state;
    }

    public int getAnimCount() {
        return this.to.keySet().size();
    }

    public boolean containsProperty(FloatProperty property) {
        return this.to.contains(property);
    }

    private void initValueForColorProperty() {
        if (this.from == null) {
            return;
        }
        for (Object key : this.to.keySet()) {
            FloatProperty property = this.to.getTempProperty(key);
            if (property instanceof ColorProperty) {
                double curValue = AnimValueUtils.getValueOfTarget(this.target, property, Double.MAX_VALUE);
                if (AnimValueUtils.isInvalid(curValue)) {
                    double fv = this.from.get(this.target, property);
                    if (!AnimValueUtils.isInvalid(fv)) {
                        this.target.setIntValue((ColorProperty) property, (int) fv);
                    }
                }
            }
        }
    }

    public void initUpdateList(IUpdateInfoCreator creator) {
        boolean z;
        this.startTime = System.nanoTime();
        AnimState f = this.from;
        AnimState t = this.to;
        boolean logEnabled = LogUtils.isLogEnabled();
        if (logEnabled) {
            LogUtils.debug("-- doSetup, target = " + this.target + ", key = " + this.key + ", f = " + f + ", t = " + t + "\nconfig = " + this.config, new Object[0]);
        }
        List<UpdateInfo> list = new ArrayList<>();
        for (Object key : t.keySet()) {
            FloatProperty property = t.getProperty(key);
            UpdateInfo update = creator.getUpdateInfo(property);
            list.add(update);
            update.animInfo.targetValue = t.get(this.target, property);
            if (f != null) {
                update.animInfo.startValue = f.get(this.target, property);
            } else {
                double startValue = update.animInfo.startValue;
                double curValue = AnimValueUtils.getValueOfTarget(this.target, property, startValue);
                if (!AnimValueUtils.isInvalid(curValue)) {
                    update.animInfo.startValue = curValue;
                }
            }
            AnimValueUtils.handleSetToValue(update);
            if (!logEnabled) {
                z = false;
            } else {
                z = false;
                LogUtils.debug("-- doSetup, target = " + this.target + ", property = " + property.getName() + ", startValue = " + update.animInfo.startValue + ", targetValue = " + update.animInfo.targetValue + ", value = " + update.animInfo.value, new Object[0]);
            }
        }
        this.updateList = list;
    }

    public AnimStats getAnimStats() {
        this.mAnimStats.clear();
        for (AnimTask task : this.animTasks) {
            this.mAnimStats.add(task.animStats);
        }
        return this.mAnimStats;
    }

    public String toString() {
        StringBuilder append = new StringBuilder().append("TransitionInfo{target = ");
        IAnimTarget iAnimTarget = this.target;
        return append.append(iAnimTarget != null ? iAnimTarget.getTargetObject() : null).append(", key = ").append(this.key).append(", propSize = ").append(this.to.keySet().size()).append(", next = ").append(this.next).append('}').toString();
    }
}
