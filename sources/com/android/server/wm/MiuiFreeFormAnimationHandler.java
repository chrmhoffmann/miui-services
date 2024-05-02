package com.android.server.wm;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.ArrayMap;
import android.view.Choreographer;
import java.util.ArrayList;
import java.util.List;
/* loaded from: classes.dex */
public class MiuiFreeFormAnimationHandler {
    private static final long FRAME_DELAY_MS = 10;
    private static final String TAG = "MiuiFreeFormAnimationHandler";
    public static final ThreadLocal<MiuiFreeFormAnimationHandler> sAnimatorHandler = new ThreadLocal<>();
    private MiuiFreeFormGestureAnimator mMiuiFreeFormGestureAnimator;
    private AnimationFrameCallbackProvider mProvider;
    private final ArrayMap<AnimationFrameCallback, Long> mDelayedCallbackStartTime = new ArrayMap<>();
    final ArrayList<AnimationFrameCallback> mAnimationCallbacks = new ArrayList<>();
    private final AnimationCallbackDispatcher mCallbackDispatcher = new AnimationCallbackDispatcher();
    long mCurrentFrameTime = 0;
    private boolean mListDirty = false;
    private List<AnimationFrameCallback> mEndAnimationCallbacks = new ArrayList();

    MiuiFreeFormAnimationHandler() {
    }

    /* loaded from: classes.dex */
    public interface AnimationFrameCallback {
        boolean doAnimationFrame(long j);

        default boolean doAnimationFrame() {
            return false;
        }
    }

    /* loaded from: classes.dex */
    public class AnimationCallbackDispatcher {
        AnimationCallbackDispatcher() {
            MiuiFreeFormAnimationHandler.this = this$0;
        }

        void dispatchAnimationFrame() {
            MiuiFreeFormAnimationHandler.this.mCurrentFrameTime = SystemClock.uptimeMillis();
            MiuiFreeFormAnimationHandler miuiFreeFormAnimationHandler = MiuiFreeFormAnimationHandler.this;
            miuiFreeFormAnimationHandler.doAnimationFrame(miuiFreeFormAnimationHandler.mCurrentFrameTime);
            if (MiuiFreeFormAnimationHandler.this.mAnimationCallbacks.size() > 0) {
                MiuiFreeFormAnimationHandler.this.getProvider().postFrameCallback();
            }
        }
    }

    public static MiuiFreeFormAnimationHandler getInstance() {
        ThreadLocal<MiuiFreeFormAnimationHandler> threadLocal = sAnimatorHandler;
        if (threadLocal.get() == null) {
            threadLocal.set(new MiuiFreeFormAnimationHandler());
        }
        return threadLocal.get();
    }

    public void setMiuiFreeFormGestureAnimator(MiuiFreeFormGestureAnimator miuiFreeFormGestureAnimator) {
        this.mMiuiFreeFormGestureAnimator = miuiFreeFormGestureAnimator;
    }

    public MiuiFreeFormGestureAnimator getMiuiFreeFormGestureAnimator() {
        return this.mMiuiFreeFormGestureAnimator;
    }

    public void clearEndAnimationCallbacks() {
        this.mEndAnimationCallbacks.clear();
    }

    public static long getFrameTime() {
        ThreadLocal<MiuiFreeFormAnimationHandler> threadLocal = sAnimatorHandler;
        if (threadLocal.get() == null) {
            return 0L;
        }
        return threadLocal.get().mCurrentFrameTime;
    }

    public void setProvider(AnimationFrameCallbackProvider provider) {
        this.mProvider = provider;
    }

    AnimationFrameCallbackProvider getProvider() {
        if (this.mProvider == null) {
            if (Build.VERSION.SDK_INT >= 16) {
                this.mProvider = new FrameCallbackProvider16(this.mCallbackDispatcher);
            } else {
                this.mProvider = new FrameCallbackProvider14(this.mCallbackDispatcher);
            }
        }
        return this.mProvider;
    }

    public void addAnimationFrameCallback(AnimationFrameCallback callback, long delay) {
        if (this.mAnimationCallbacks.size() == 0) {
            getProvider().postFrameCallback();
        }
        if (!this.mAnimationCallbacks.contains(callback)) {
            this.mAnimationCallbacks.add(callback);
        }
        if (delay > 0) {
            this.mDelayedCallbackStartTime.put(callback, Long.valueOf(SystemClock.uptimeMillis() + delay));
        }
    }

    public void removeCallback(AnimationFrameCallback callback) {
        this.mDelayedCallbackStartTime.remove(callback);
        int id = this.mAnimationCallbacks.indexOf(callback);
        if (id >= 0) {
            if (callback instanceof MiuiFreeFormDynamicAnimation) {
                MiuiFreeFormDynamicAnimation<?> animation = (MiuiFreeFormDynamicAnimation) callback;
                if (animation.needExecuteCallbackWhenAnimationEnd()) {
                    this.mEndAnimationCallbacks.add(callback);
                }
            }
            this.mAnimationCallbacks.set(id, null);
            this.mListDirty = true;
        }
    }

    void doAnimationFrame(long frameTime) {
        long currentTime = SystemClock.uptimeMillis();
        for (int i = 0; i < this.mAnimationCallbacks.size(); i++) {
            AnimationFrameCallback callback = this.mAnimationCallbacks.get(i);
            if (callback != null && isCallbackDue(callback, currentTime)) {
                callback.doAnimationFrame(frameTime);
            }
        }
        cleanUpList();
        for (AnimationFrameCallback endCallback : this.mEndAnimationCallbacks) {
            if (endCallback instanceof MiuiFreeFormDynamicAnimation) {
                MiuiFreeFormDynamicAnimation<?> animation = (MiuiFreeFormDynamicAnimation) endCallback;
                if (animation.needExecuteCallbackWhenAnimationEnd()) {
                    endCallback.doAnimationFrame();
                }
            }
        }
        MiuiFreeFormGestureAnimator animator = getInstance().getMiuiFreeFormGestureAnimator();
        if (animator != null) {
            animator.applyTransaction();
        }
    }

    private boolean isCallbackDue(AnimationFrameCallback callback, long currentTime) {
        Long startTime = this.mDelayedCallbackStartTime.get(callback);
        if (startTime == null) {
            return true;
        }
        if (startTime.longValue() < currentTime) {
            this.mDelayedCallbackStartTime.remove(callback);
            return true;
        }
        return false;
    }

    private void cleanUpList() {
        if (this.mListDirty) {
            for (int i = this.mAnimationCallbacks.size() - 1; i >= 0; i--) {
                if (this.mAnimationCallbacks.get(i) == null) {
                    this.mAnimationCallbacks.remove(i);
                }
            }
            this.mListDirty = false;
            if (this.mAnimationCallbacks.isEmpty()) {
                clearEndAnimationCallbacks();
            }
        }
    }

    /* loaded from: classes.dex */
    public static class FrameCallbackProvider16 extends AnimationFrameCallbackProvider {
        private final Choreographer mChoreographer = Choreographer.getInstance();
        private final Choreographer.FrameCallback mChoreographerCallback = new Choreographer.FrameCallback() { // from class: com.android.server.wm.MiuiFreeFormAnimationHandler.FrameCallbackProvider16.1
            @Override // android.view.Choreographer.FrameCallback
            public void doFrame(long frameTimeNanos) {
                FrameCallbackProvider16.this.mDispatcher.dispatchAnimationFrame();
            }
        };

        FrameCallbackProvider16(AnimationCallbackDispatcher dispatcher) {
            super(dispatcher);
        }

        @Override // com.android.server.wm.MiuiFreeFormAnimationHandler.AnimationFrameCallbackProvider
        void postFrameCallback() {
            this.mChoreographer.postFrameCallback(this.mChoreographerCallback);
        }
    }

    /* loaded from: classes.dex */
    public static class FrameCallbackProvider14 extends AnimationFrameCallbackProvider {
        long mLastFrameTime = -1;
        private final Runnable mRunnable = new Runnable() { // from class: com.android.server.wm.MiuiFreeFormAnimationHandler.FrameCallbackProvider14.1
            @Override // java.lang.Runnable
            public void run() {
                FrameCallbackProvider14.this.mLastFrameTime = SystemClock.uptimeMillis();
                FrameCallbackProvider14.this.mDispatcher.dispatchAnimationFrame();
            }
        };
        private final Handler mHandler = new Handler(Looper.myLooper());

        FrameCallbackProvider14(AnimationCallbackDispatcher dispatcher) {
            super(dispatcher);
        }

        @Override // com.android.server.wm.MiuiFreeFormAnimationHandler.AnimationFrameCallbackProvider
        void postFrameCallback() {
            long delay = MiuiFreeFormAnimationHandler.FRAME_DELAY_MS - (SystemClock.uptimeMillis() - this.mLastFrameTime);
            this.mHandler.postDelayed(this.mRunnable, Math.max(delay, 0L));
        }
    }

    /* loaded from: classes.dex */
    public static abstract class AnimationFrameCallbackProvider {
        final AnimationCallbackDispatcher mDispatcher;

        abstract void postFrameCallback();

        AnimationFrameCallbackProvider(AnimationCallbackDispatcher dispatcher) {
            this.mDispatcher = dispatcher;
        }
    }
}
