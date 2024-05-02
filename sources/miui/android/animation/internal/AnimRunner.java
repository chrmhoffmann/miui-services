package miui.android.animation.internal;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import java.util.Collection;
import java.util.Iterator;
import miui.android.animation.Folme;
import miui.android.animation.IAnimTarget;
import miui.android.animation.base.AnimConfigLink;
import miui.android.animation.controller.AnimState;
import miui.android.animation.physics.AnimationHandler;
import miui.android.animation.property.FloatProperty;
import miui.android.animation.utils.CommonUtils;
import miui.android.animation.utils.LogUtils;
/* loaded from: classes.dex */
public class AnimRunner implements AnimationHandler.AnimationFrameCallback {
    public static final long MAX_DELTA = 16;
    private static final int MAX_RECORD = 5;
    private static final int MSG_END = 1;
    private static final int MSG_START = 0;
    private static final Handler sMainHandler = new Handler(Looper.getMainLooper()) { // from class: miui.android.animation.internal.AnimRunner.1
        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    AnimRunner.startAnimRunner();
                    return;
                case 1:
                    AnimRunner.endAnimation();
                    return;
                default:
                    return;
            }
        }
    };
    public static final RunnerHandler sRunnerHandler;
    private static final HandlerThread sRunnerThread;
    private volatile long mAverageDelta;
    private long[] mDeltaRecord;
    private volatile boolean mIsRunning;
    private long mLastFrameTime;
    private float mRatio;
    private int mRecordCount;

    /* loaded from: classes.dex */
    public static class Holder {
        static final AnimRunner inst = new AnimRunner();

        private Holder() {
        }
    }

    public static AnimRunner getInst() {
        return Holder.inst;
    }

    static {
        HandlerThread handlerThread = new HandlerThread("AnimRunnerThread", 5);
        sRunnerThread = handlerThread;
        handlerThread.start();
        sRunnerHandler = new RunnerHandler(handlerThread.getLooper());
    }

    private static void updateAnimRunner(Collection<IAnimTarget> targets, boolean toPage) {
        for (IAnimTarget target : targets) {
            boolean isAnimRunning = target.animManager.isAnimRunning(new FloatProperty[0]);
            if (isAnimRunning) {
                if (toPage) {
                    target.animManager.runUpdate();
                } else {
                    target.animManager.update(false);
                }
            } else if (!isAnimRunning && target.hasFlags(1L)) {
                Folme.clean(target);
            }
        }
    }

    public static void startAnimRunner() {
        AnimRunner runner = getInst();
        if (runner.mIsRunning) {
            return;
        }
        if (LogUtils.isLogEnabled()) {
            LogUtils.debug("AnimRunner.start", new Object[0]);
        }
        runner.mRatio = Folme.getTimeRatio();
        runner.mIsRunning = true;
        AnimationHandler.getInstance().addAnimationFrameCallback(runner, 0L);
    }

    public static void endAnimation() {
        AnimRunner runner = getInst();
        if (!runner.mIsRunning) {
            return;
        }
        if (LogUtils.isLogEnabled()) {
            LogUtils.debug("AnimRunner.endAnimation", new Object[0]);
        }
        runner.mIsRunning = false;
        AnimationHandler.getInstance().removeCallback(runner);
    }

    private AnimRunner() {
        this.mAverageDelta = 16L;
        this.mDeltaRecord = new long[]{0, 0, 0, 0, 0};
        this.mRecordCount = 0;
    }

    @Override // miui.android.animation.physics.AnimationHandler.AnimationFrameCallback
    public boolean doAnimationFrame(long frameTime) {
        boolean z;
        updateRunningTime(frameTime);
        if (this.mIsRunning) {
            Collection<IAnimTarget> targets = Folme.getTargets();
            int animCounnt = 0;
            Iterator<IAnimTarget> it = targets.iterator();
            while (true) {
                z = false;
                if (!it.hasNext()) {
                    break;
                }
                IAnimTarget target = it.next();
                boolean isAnimRunning = target.animManager.isAnimRunning(new FloatProperty[0]);
                if (isAnimRunning) {
                    animCounnt += target.animManager.getTotalAnimCount();
                }
            }
            if (animCounnt > 500) {
                z = true;
            }
            boolean toPage = z;
            if (!toPage && targets.size() > 0) {
                updateAnimRunner(targets, toPage);
            }
            RunnerHandler runnerHandler = sRunnerHandler;
            Message msg = runnerHandler.obtainMessage();
            msg.what = 3;
            msg.obj = Boolean.valueOf(toPage);
            runnerHandler.sendMessage(msg);
            if (toPage) {
                updateAnimRunner(targets, toPage);
            }
        }
        return this.mIsRunning;
    }

    public void cancel(IAnimTarget target, String... propertyNames) {
        sRunnerHandler.setOperation(new AnimOperationInfo(target, (byte) 4, propertyNames, null));
    }

    public void cancel(IAnimTarget target, FloatProperty... properties) {
        sRunnerHandler.setOperation(new AnimOperationInfo(target, (byte) 4, null, properties));
    }

    public void end(IAnimTarget target, String... propertyNames) {
        if (CommonUtils.isArrayEmpty(propertyNames)) {
            target.handler.sendEmptyMessage(3);
        }
        sRunnerHandler.setOperation(new AnimOperationInfo(target, (byte) 3, propertyNames, null));
    }

    public void end(IAnimTarget target, FloatProperty... properties) {
        if (CommonUtils.isArrayEmpty(properties)) {
            target.handler.sendEmptyMessage(3);
        }
        sRunnerHandler.setOperation(new AnimOperationInfo(target, (byte) 3, null, properties));
    }

    public void run(IAnimTarget target, AnimState from, AnimState to, AnimConfigLink config) {
        TransitionInfo info = new TransitionInfo(target, from, to, config);
        run(info);
    }

    public void run(final TransitionInfo info) {
        info.target.executeOnInitialized(new Runnable() { // from class: miui.android.animation.internal.AnimRunner.2
            @Override // java.lang.Runnable
            public void run() {
                info.target.animManager.startAnim(info);
            }
        });
    }

    public void start() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            startAnimRunner();
        } else {
            sMainHandler.sendEmptyMessage(0);
        }
    }

    public void end() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            endAnimation();
        } else {
            sMainHandler.sendEmptyMessage(1);
        }
    }

    public long getAverageDelta() {
        return this.mAverageDelta;
    }

    private long calculateAverageDelta(long deltaT) {
        long average = average(this.mDeltaRecord);
        long deltaT2 = average > 0 ? average : deltaT;
        if (deltaT2 == 0 || deltaT2 > 16) {
            deltaT2 = 16;
        }
        return (long) Math.ceil(((float) deltaT2) / this.mRatio);
    }

    private void updateRunningTime(long frameTime) {
        long deltaT;
        long deltaT2 = this.mLastFrameTime;
        if (deltaT2 == 0) {
            this.mLastFrameTime = frameTime;
            deltaT = 0;
        } else {
            deltaT = frameTime - deltaT2;
            this.mLastFrameTime = frameTime;
        }
        int i = this.mRecordCount;
        int idx = i % 5;
        this.mDeltaRecord[idx] = deltaT;
        this.mRecordCount = i + 1;
        this.mAverageDelta = calculateAverageDelta(deltaT);
    }

    private long average(long[] array) {
        long total = 0;
        int count = 0;
        int length = array.length;
        for (int i = 0; i < length; i++) {
            long a = array[i];
            total += a;
            count = a > 0 ? count + 1 : count;
        }
        if (count > 0) {
            return total / count;
        }
        return 0L;
    }
}
