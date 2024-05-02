package miui.android.animation.internal;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import miui.android.animation.IAnimTarget;
import miui.android.animation.ViewTarget;
import miui.android.animation.controller.AnimState;
import miui.android.animation.listener.UpdateInfo;
import miui.android.animation.property.FloatProperty;
import miui.android.animation.property.IIntValueProperty;
import miui.android.animation.utils.LinkNode;
import miui.android.animation.utils.LogUtils;
/* loaded from: classes.dex */
public class RunnerHandler extends Handler {
    public static final int ANIM_MSG_RUNNER_RUN = 3;
    public static final int ANIM_MSG_SETUP = 1;
    public static final int ANIM_MSG_SET_TO = 4;
    public static final int ANIM_MSG_UPDATE = 2;
    private boolean mIsTaskRunning;
    private boolean mRunnerStart;
    private boolean mStart;
    private final Set<IAnimTarget> runningTarget = new HashSet();
    private final Map<IAnimTarget, AnimOperationInfo> mOpMap = new ConcurrentHashMap();
    private final Map<IAnimTarget, TransitionInfo> mTransMap = new HashMap();
    private final List<AnimTask> mTaskList = new ArrayList();
    private final List<IAnimTarget> mDelList = new ArrayList();
    private final List<TransitionInfo> mTransList = new ArrayList();
    private long mLastRun = 0;
    private long mTotalT = 0;
    private int mFrameCount = 0;
    private final int[] mSplitInfo = new int[2];

    /* loaded from: classes.dex */
    public static class SetToInfo {
        AnimState state;
        IAnimTarget target;

        private SetToInfo() {
        }
    }

    public RunnerHandler(Looper looper) {
        super(looper);
    }

    public void setOperation(AnimOperationInfo opInfo) {
        if (opInfo.target.isAnimRunning(new FloatProperty[0])) {
            opInfo.sendTime = System.nanoTime();
            this.mOpMap.put(opInfo.target, opInfo);
        }
    }

    public void addSetToState(IAnimTarget target, AnimState to) {
        SetToInfo info = new SetToInfo();
        info.target = target;
        if (to.isTemporary) {
            info.state = new AnimState();
            info.state.set(to);
        } else {
            info.state = to;
        }
        obtainMessage(4, info).sendToTarget();
    }

    @Override // android.os.Handler
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case 1:
                TransitionInfo info = TransitionInfo.sMap.remove(Integer.valueOf(msg.arg1));
                if (info != null) {
                    addToMap(info.target, info, this.mTransMap);
                    if (!this.mIsTaskRunning) {
                        doSetup();
                        break;
                    }
                }
                break;
            case 2:
                updateAnim();
                break;
            case 3:
                if (this.mRunnerStart) {
                    long now = System.currentTimeMillis();
                    long averageDelta = AnimRunner.getInst().getAverageDelta();
                    boolean toPage = ((Boolean) msg.obj).booleanValue();
                    if (!this.mStart) {
                        this.mStart = true;
                        this.mTotalT = 0L;
                        this.mFrameCount = 0;
                        runAnim(now, averageDelta, toPage);
                        break;
                    } else if (!this.mIsTaskRunning) {
                        runAnim(now, now - this.mLastRun, toPage);
                        break;
                    }
                }
                break;
            case 4:
                onSetTo((SetToInfo) msg.obj);
                break;
        }
        msg.obj = null;
    }

    private void onSetTo(SetToInfo setInfo) {
        boolean isViewTarget = setInfo.target instanceof ViewTarget;
        for (Object key : setInfo.state.keySet()) {
            FloatProperty property = setInfo.state.getProperty(key);
            UpdateInfo update = setInfo.target.animManager.mUpdateMap.get(property);
            double value = setInfo.state.get(setInfo.target, property);
            if (update != null) {
                update.animInfo.setToValue = value;
                if (!isViewTarget) {
                    update.setTargetValue(setInfo.target);
                }
            } else if (property instanceof IIntValueProperty) {
                setInfo.target.setIntValue((IIntValueProperty) property, (int) value);
            } else {
                setInfo.target.setValue(property, (float) value);
            }
        }
        if (!setInfo.target.isAnimRunning(new FloatProperty[0])) {
            setInfo.target.animManager.mUpdateMap.clear();
        }
    }

    private <T extends LinkNode> void addToMap(IAnimTarget target, T node, Map<IAnimTarget, T> map) {
        T head = map.get(target);
        if (head == null) {
            map.put(target, node);
        } else {
            head.addToTail(node);
        }
    }

    private void stopAnimRunner() {
        if (this.mStart) {
            if (LogUtils.isLogEnabled()) {
                LogUtils.debug("RunnerHandler.stopAnimRunner", "total time = " + this.mTotalT, "frame count = " + this.mFrameCount);
            }
            this.mStart = false;
            this.mRunnerStart = false;
            this.mTotalT = 0L;
            this.mFrameCount = 0;
            AnimRunner.getInst().end();
        }
    }

    private void updateAnim() {
        this.mIsTaskRunning = false;
        boolean isRunning = false;
        for (IAnimTarget target : this.runningTarget) {
            if (updateTarget(target, this.mTransList) || setupWaitTrans(target)) {
                isRunning = true;
            } else {
                this.mDelList.add(target);
            }
            this.mTransList.clear();
        }
        this.runningTarget.removeAll(this.mDelList);
        this.mDelList.clear();
        if (!this.mTransMap.isEmpty()) {
            doSetup();
            isRunning = true;
        }
        if (!isRunning) {
            stopAnimRunner();
        }
    }

    private boolean setupWaitTrans(IAnimTarget target) {
        TransitionInfo info = target.animManager.mWaitState.poll();
        if (info != null) {
            addToMap(info.target, info, this.mTransMap);
            return true;
        }
        return false;
    }

    private boolean isInfoInTransMap(TransitionInfo info) {
        for (TransitionInfo node = this.mTransMap.get(info.target); node != null; node = (TransitionInfo) node.next) {
            if (node == info) {
                return true;
            }
        }
        return false;
    }

    private boolean updateTarget(IAnimTarget target, List<TransitionInfo> transList) {
        int i;
        target.animManager.getTransitionInfos(transList);
        int runCount = 0;
        int animStartAfterCancel = 0;
        AnimOperationInfo opInfo = this.mOpMap.get(target);
        for (TransitionInfo info : transList) {
            if (isInfoInTransMap(info)) {
                runCount++;
            } else {
                AnimOperationInfo useOp = opInfo;
                if (useOp != null && info.startTime > useOp.sendTime) {
                    useOp = null;
                    animStartAfterCancel++;
                }
                AnimStats stats = info.getAnimStats();
                if (stats.isStarted()) {
                    handleUpdate(info, useOp, stats);
                }
                if (!LogUtils.isLogEnabled()) {
                    i = 4;
                } else {
                    String str = "---- updateAnim, target = " + target;
                    Object[] objArr = new Object[6];
                    objArr[0] = "key = " + info.key;
                    objArr[1] = "useOp = " + useOp;
                    objArr[2] = "info.startTime = " + info.startTime;
                    objArr[3] = "opInfo.time = " + (opInfo != null ? Long.valueOf(opInfo.sendTime) : null);
                    i = 4;
                    objArr[4] = "stats.isRunning = " + stats.isRunning();
                    objArr[5] = "stats = " + stats;
                    LogUtils.debug(str, objArr);
                }
                if (!stats.isRunning()) {
                    target.animManager.notifyTransitionEnd(info, 2, stats.cancelCount > stats.endCount ? i : 3);
                } else {
                    runCount++;
                }
            }
        }
        if (opInfo != null && (animStartAfterCancel == transList.size() || opInfo.isUsed())) {
            this.mOpMap.remove(target);
        }
        transList.clear();
        return runCount > 0;
    }

    private static void handleUpdate(TransitionInfo info, AnimOperationInfo opInfo, AnimStats stats) {
        boolean isRunningAnim = info.target.animManager.mStartAnim.contains(info.key);
        for (AnimTask task : info.animTasks) {
            List<UpdateInfo> updateList = info.updateList;
            int i = task.startPos;
            int n = task.getAnimCount() + i;
            while (i < n) {
                UpdateInfo update = updateList.get(i);
                if (!handleSetTo(task, stats, update) && isRunningAnim && opInfo != null) {
                    doSetOperation(task, stats, update, opInfo);
                }
                i++;
            }
        }
        if (info.target.animManager.mStartAnim.add(info.key) && stats.isRunning() && stats.updateCount > 0) {
            TransitionInfo.sMap.put(Integer.valueOf(info.id), info);
            info.target.handler.obtainMessage(0, info.id, 0).sendToTarget();
        }
    }

    private static boolean handleSetTo(AnimTask task, AnimStats stats, UpdateInfo update) {
        if (!AnimValueUtils.handleSetToValue(update)) {
            return false;
        }
        if (AnimTask.isRunning(update.animInfo.op)) {
            task.animStats.cancelCount++;
            stats.cancelCount++;
            update.setOp((byte) 4);
            TransitionInfo.decreaseStartCountForDelayAnim(task, stats, update, update.animInfo.op);
        }
        return true;
    }

    private static void doSetOperation(AnimTask task, AnimStats stats, UpdateInfo update, AnimOperationInfo opInfo) {
        byte op = update.animInfo.op;
        if (AnimTask.isRunning(op) && opInfo.op != 0) {
            if ((opInfo.propList == null || opInfo.propList.contains(update.property)) && AnimTask.isRunning(update.animInfo.op)) {
                opInfo.usedCount++;
                if (opInfo.op == 3) {
                    if (update.animInfo.targetValue != Double.MAX_VALUE) {
                        update.animInfo.value = update.animInfo.targetValue;
                    }
                    task.animStats.endCount++;
                    stats.endCount++;
                } else if (opInfo.op == 4) {
                    task.animStats.cancelCount++;
                    stats.cancelCount++;
                }
                update.setOp(opInfo.op);
                TransitionInfo.decreaseStartCountForDelayAnim(task, stats, update, op);
            }
        }
    }

    private void runAnim(long now, long deltaT, boolean toPage) {
        long deltaT2;
        if (!this.runningTarget.isEmpty()) {
            this.mLastRun = now;
            long averageDelta = AnimRunner.getInst().getAverageDelta();
            int i = this.mFrameCount;
            if (i == 1 && deltaT > 2 * averageDelta) {
                deltaT2 = averageDelta;
            } else {
                deltaT2 = deltaT;
            }
            this.mTotalT += deltaT2;
            this.mFrameCount = i + 1;
            int animCount = getTotalAnimCount();
            ThreadPoolUtil.getSplitCount(animCount, this.mSplitInfo);
            int[] iArr = this.mSplitInfo;
            int splitCount = iArr[0];
            int singleCount = iArr[1];
            for (IAnimTarget target : this.runningTarget) {
                target.animManager.getTransitionInfos(this.mTransList);
            }
            addAnimTask(this.mTransList, singleCount, splitCount);
            this.mIsTaskRunning = true ^ this.mTaskList.isEmpty();
            AnimTask.sTaskCount.set(this.mTaskList.size());
            for (AnimTask task : this.mTaskList) {
                task.start(this.mTotalT, deltaT2, toPage);
                singleCount = singleCount;
            }
            this.mTransList.clear();
            this.mTaskList.clear();
            return;
        }
        stopAnimRunner();
    }

    private void addAnimTask(List<TransitionInfo> transList, int singleCount, int splitCount) {
        for (TransitionInfo info : transList) {
            for (AnimTask task : info.animTasks) {
                AnimTask curTask = getTaskOfMinCount();
                if (curTask == null || (this.mTaskList.size() < splitCount && curTask.getTotalAnimCount() + task.getAnimCount() > singleCount)) {
                    this.mTaskList.add(task);
                } else {
                    curTask.addToTail(task);
                }
            }
        }
    }

    private AnimTask getTaskOfMinCount() {
        AnimTask state = null;
        int min = Integer.MAX_VALUE;
        for (AnimTask s : this.mTaskList) {
            int totalCount = s.getTotalAnimCount();
            if (totalCount < min) {
                min = totalCount;
                state = s;
            }
        }
        return state;
    }

    private int getTotalAnimCount() {
        int count = 0;
        for (IAnimTarget target : this.runningTarget) {
            count += target.animManager.getTotalAnimCount();
        }
        return count;
    }

    private void doSetup() {
        for (TransitionInfo info : this.mTransMap.values()) {
            this.runningTarget.add(info.target);
            TransitionInfo head = info;
            do {
                head.target.animManager.setupTransition(head);
                head = head.remove();
            } while (head != null);
        }
        this.mTransMap.clear();
        if (!this.mRunnerStart) {
            this.mRunnerStart = true;
            AnimRunner.getInst().start();
        }
    }
}
