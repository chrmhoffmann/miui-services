package com.android.server.wm;

import android.graphics.Point;
import android.graphics.Rect;
import android.os.HandlerThread;
import android.os.Message;
import android.util.MiuiMultiWindowUtils;
import android.view.Choreographer;
import android.view.SurfaceControl;
import android.window.WindowContainerTransaction;
import com.android.server.wm.MiuiCvwAnimator;
import com.android.server.wm.MiuiCvwGestureController;
import com.android.server.wm.MiuiCvwPolicy;
import com.android.server.wm.MiuiFreeformTrackManager;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import miui.os.Build;
/* loaded from: classes.dex */
public class MiuiCvwLayerSupervisor {
    public static final int CVW_OVERLAYS_COMING = 4;
    public static final int CVW_OVERLAYS_CREATED = 2;
    public static final int CVW_OVERLAYS_CREATEING = 1;
    public static final int CVW_OVERLAYS_HIDE_FINISH = 64;
    public static final int CVW_OVERLAYS_HIDING = 32;
    public static final int CVW_OVERLAYS_REMOVE_FINISH = 2048;
    public static final int CVW_OVERLAYS_REMOVING = 1024;
    public static final int CVW_OVERLAYS_SHOWING = 8;
    public static final int CVW_OVERLAYS_SHOW_FINISH = 16;
    public static final int CVW_STATUS_NULL = 0;
    public static final int CVW_TASK_ANIMATING = 512;
    public static final int CVW_TASK_LEASH_CREATED = 4096;
    public static final int CVW_TASK_LEASH_REMOVED = 8192;
    public static final int CVW_TASK_RESIZED = 256;
    public static final int CVW_TASK_RESIZE_WAITING = 128;
    private static final String CVW_VIEW_RENDER_THREAD = "cvw_view_render";
    private static final int EXECUTE_TASK_COMMAND = 1;
    private static final int EXECUTE_TASK_RESIZE = 2;
    private static final String TAG = MiuiCvwLayerSupervisor.class.getSimpleName();
    private MiuiFreeFormActivityStack mAnimatingStack;
    protected final MiuiCvwAnimator mAnimator;
    private final MiuiCvwBackgroundLayer mBackgroundLayer;
    private Choreographer mChoreographer;
    protected final MiuiCvwGestureController mController;
    protected final MiuiCvwCoverLayer mCoverLayer;
    private volatile float mCurrentAlpha;
    private volatile float mCurrentHeight;
    private volatile float mCurrentScale;
    private volatile float mCurrentWidth;
    private volatile float mCurrentXScale;
    private volatile float mCurrentYScale;
    MiuiCvwGestureController.H mExecuteHandler;
    protected final MiuiCvwGestureHandlerImpl mGestureHandlerImpl;
    private volatile float mPosBottom;
    private volatile float mPosLeft;
    private volatile float mPosRight;
    private volatile float mPosTop;
    private volatile float mPrePosBottom;
    private volatile float mPrePosLeft;
    private volatile float mPrePosRight;
    private volatile float mPrePosTop;
    private volatile float mRadius;
    private volatile float mScale;
    private volatile float mScaleX;
    private volatile float mScaleY;
    MiuiCvwGestureController.H mViewRenderHandler;
    private int mCVWStatus = 0;
    private final ArrayList<IWindowFrameReceiver> mWinFrameReceivers = new ArrayList<>();
    private final ConcurrentHashMap<MiuiFreeFormActivityStack, MiuiCvwAnimator.AnimalLock> mWindowLocks = new ConcurrentHashMap<>();
    private Rect mLastTaskBounds = new Rect();
    private final Choreographer.FrameCallback mChoreographerCallback = new Choreographer.FrameCallback() { // from class: com.android.server.wm.MiuiCvwLayerSupervisor.1
        @Override // android.view.Choreographer.FrameCallback
        public void doFrame(long frameTimeNanos) {
            if (MiuiCvwLayerSupervisor.this.mPrePosTop == MiuiCvwLayerSupervisor.this.mPosTop && MiuiCvwLayerSupervisor.this.mPrePosRight == MiuiCvwLayerSupervisor.this.mPosRight && MiuiCvwLayerSupervisor.this.mPrePosLeft == MiuiCvwLayerSupervisor.this.mPosLeft && MiuiCvwLayerSupervisor.this.mPrePosBottom == MiuiCvwLayerSupervisor.this.mPosBottom && !MiuiCvwLayerSupervisor.this.mCoverLayer.requestPushing()) {
                if (MiuiCvwLayerSupervisor.this.isCVWTaskAnimating()) {
                    long rateDelay = (((float) MiuiCvwLayerSupervisor.this.mChoreographer.getFrameIntervalNanos()) * 1.0E-6f) / 4.0f;
                    MiuiCvwLayerSupervisor.this.mChoreographer.postFrameCallbackDelayed(MiuiCvwLayerSupervisor.this.mChoreographerCallback, rateDelay);
                    return;
                }
                return;
            }
            for (int i = 0; i < MiuiCvwLayerSupervisor.this.mWinFrameReceivers.size(); i++) {
                IWindowFrameReceiver receiver = (IWindowFrameReceiver) MiuiCvwLayerSupervisor.this.mWinFrameReceivers.get(i);
                receiver.doFrame((int) MiuiCvwLayerSupervisor.this.mPosLeft, (int) MiuiCvwLayerSupervisor.this.mPosTop, (int) MiuiCvwLayerSupervisor.this.mPosRight, (int) MiuiCvwLayerSupervisor.this.mPosBottom);
            }
            if (MiuiCvwLayerSupervisor.this.isCVWTaskAnimating()) {
                long rateDelay2 = (((float) MiuiCvwLayerSupervisor.this.mChoreographer.getFrameIntervalNanos()) * 1.0E-6f) / 4.0f;
                MiuiCvwLayerSupervisor.this.mChoreographer.postFrameCallbackDelayed(MiuiCvwLayerSupervisor.this.mChoreographerCallback, rateDelay2);
            } else {
                MiuiCvwGestureController.Slog.d(MiuiCvwLayerSupervisor.TAG, "doFrame end");
            }
            MiuiCvwLayerSupervisor miuiCvwLayerSupervisor = MiuiCvwLayerSupervisor.this;
            miuiCvwLayerSupervisor.mPrePosTop = miuiCvwLayerSupervisor.mPosTop;
            MiuiCvwLayerSupervisor miuiCvwLayerSupervisor2 = MiuiCvwLayerSupervisor.this;
            miuiCvwLayerSupervisor2.mPrePosRight = miuiCvwLayerSupervisor2.mPosRight;
            MiuiCvwLayerSupervisor miuiCvwLayerSupervisor3 = MiuiCvwLayerSupervisor.this;
            miuiCvwLayerSupervisor3.mPrePosBottom = miuiCvwLayerSupervisor3.mPosBottom;
            MiuiCvwLayerSupervisor miuiCvwLayerSupervisor4 = MiuiCvwLayerSupervisor.this;
            miuiCvwLayerSupervisor4.mPrePosLeft = miuiCvwLayerSupervisor4.mPosLeft;
        }
    };
    private final MiuiCvwAnimator.SpringAnimatorListener<MiuiFreeFormActivityStack> mTaskSpringAnimatorListener = new MiuiCvwAnimator.SpringAnimatorListener<MiuiFreeFormActivityStack>() { // from class: com.android.server.wm.MiuiCvwLayerSupervisor.3
        public void updateValue(MiuiFreeFormActivityStack stack, float value, int springAnimalType) {
            switch (springAnimalType) {
                case 1:
                    setAlpha(stack, value);
                    return;
                case 2:
                    setPositionX(stack, value);
                    return;
                case 3:
                    setPositionY(stack, value);
                    return;
                case 4:
                    setScale(stack, value);
                    return;
                case 5:
                    setXScale(stack, value);
                    return;
                case 6:
                    setYScale(stack, value);
                    return;
                case 7:
                    setPositionRight(stack, value);
                    return;
                case 8:
                    setPositionBottom(stack, value);
                    return;
                default:
                    return;
            }
        }

        @Override // com.android.server.wm.MiuiCvwAnimator.SpringAnimatorListener
        public void onAnimationEnd(MiuiFreeFormDynamicAnimation<?> animation, String propertyType) {
            MiuiCvwAnimator.AnimalLock animalLock = (MiuiCvwAnimator.AnimalLock) MiuiCvwLayerSupervisor.this.mWindowLocks.get(animation.mTarget);
            if (animalLock == null) {
                return;
            }
            char c = 65535;
            switch (propertyType.hashCode()) {
                case -1666090365:
                    if (propertyType.equals(MiuiFreeFormGestureAnimator.FLOAT_PROPERTY_SCALE_X)) {
                        c = 2;
                        break;
                    }
                    break;
                case -1666090364:
                    if (propertyType.equals(MiuiFreeFormGestureAnimator.FLOAT_PROPERTY_SCALE_Y)) {
                        c = 3;
                        break;
                    }
                    break;
                case 62372158:
                    if (propertyType.equals("ALPHA")) {
                        c = 0;
                        break;
                    }
                    break;
                case 78713130:
                    if (propertyType.equals(MiuiFreeFormGestureAnimator.FLOAT_PROPERTY_SCALE)) {
                        c = 1;
                        break;
                    }
                    break;
                case 312310151:
                    if (propertyType.equals(MiuiFreeFormGestureAnimator.FLOAT_PROPERTY_TRANSLATE_X)) {
                        c = 4;
                        break;
                    }
                    break;
                case 312310152:
                    if (propertyType.equals(MiuiFreeFormGestureAnimator.FLOAT_PROPERTY_TRANSLATE_Y)) {
                        c = 5;
                        break;
                    }
                    break;
                case 745870251:
                    if (propertyType.equals("TRANSLATE_RIGHT")) {
                        c = 6;
                        break;
                    }
                    break;
                case 1195014748:
                    if (propertyType.equals("TRANSLATE_BOTTOM")) {
                        c = 7;
                        break;
                    }
                    break;
            }
            switch (c) {
                case 0:
                    MiuiCvwGestureController.Slog.d(MiuiCvwLayerSupervisor.TAG, "onAlphaAnimationEnd\u3000animalLock:" + animalLock);
                    break;
                case 1:
                    MiuiCvwGestureController.Slog.d(MiuiCvwLayerSupervisor.TAG, "onScaleAnimationEnd\u3000animalLock:" + animalLock);
                    break;
                case 2:
                    MiuiCvwGestureController.Slog.d(MiuiCvwLayerSupervisor.TAG, "onScaleXAnimationEnd\u3000animalLock:" + animalLock);
                    break;
                case 3:
                    MiuiCvwGestureController.Slog.d(MiuiCvwLayerSupervisor.TAG, "onScaleYAnimationEnd\u3000animalLock:" + animalLock);
                    break;
                case 4:
                    MiuiCvwGestureController.Slog.d(MiuiCvwLayerSupervisor.TAG, "onTranslateXAnimationEnd\u3000animalLock:" + animalLock);
                    break;
                case 5:
                    MiuiCvwGestureController.Slog.d(MiuiCvwLayerSupervisor.TAG, "onTranslateYAnimationEnd\u3000animalLock:" + animalLock);
                    break;
                case 6:
                    MiuiCvwGestureController.Slog.d(MiuiCvwLayerSupervisor.TAG, "onTranslateRIGHTAnimationEnd\u3000animalLock:" + animalLock);
                    break;
                case 7:
                    MiuiCvwGestureController.Slog.d(MiuiCvwLayerSupervisor.TAG, "onTranslateBOTTOMAnimationEnd\u3000animalLock:" + animalLock);
                    break;
            }
            MiuiCvwLayerSupervisor.this.onAnimationEnd(animalLock, animation);
        }

        void setScale(MiuiFreeFormActivityStack stack, float scale) {
            if (stack == null) {
                MiuiCvwGestureController.Slog.d(MiuiCvwLayerSupervisor.TAG, "setScale stack is null,return;");
            } else if (stack.mTask == null) {
                MiuiCvwGestureController.Slog.d(MiuiCvwLayerSupervisor.TAG, "setScale task is null,return;");
            } else {
                MiuiCvwAnimator.AnimalLock animalLock = (MiuiCvwAnimator.AnimalLock) MiuiCvwLayerSupervisor.this.mWindowLocks.get(stack);
                if (animalLock == null) {
                    return;
                }
                MiuiCvwLayerSupervisor.this.mCurrentScale = scale;
            }
        }

        void setPositionX(MiuiFreeFormActivityStack stack, float x) {
            if (stack == null) {
                MiuiCvwGestureController.Slog.d(MiuiCvwLayerSupervisor.TAG, "setPositionX stack is null,return;");
            } else if (stack.mTask == null) {
                MiuiCvwGestureController.Slog.d(MiuiCvwLayerSupervisor.TAG, "setPositionX task is null,return;");
            } else {
                MiuiCvwAnimator.AnimalLock animalLock = (MiuiCvwAnimator.AnimalLock) MiuiCvwLayerSupervisor.this.mWindowLocks.get(stack);
                if (animalLock != null && animalLock.mCurrentAnimation != 11) {
                    MiuiCvwLayerSupervisor.this.mPosLeft = (int) (x + 0.5d);
                }
            }
        }

        void setPositionY(MiuiFreeFormActivityStack stack, float y) {
            if (stack == null) {
                MiuiCvwGestureController.Slog.d(MiuiCvwLayerSupervisor.TAG, "setPositionY stack is null,return;");
            } else if (stack.mTask == null) {
                MiuiCvwGestureController.Slog.d(MiuiCvwLayerSupervisor.TAG, "setPositionY task is null,return;");
            } else {
                MiuiCvwAnimator.AnimalLock animalLock = (MiuiCvwAnimator.AnimalLock) MiuiCvwLayerSupervisor.this.mWindowLocks.get(stack);
                if (animalLock != null && animalLock.mCurrentAnimation != 11) {
                    MiuiCvwLayerSupervisor.this.mPosTop = (int) (y + 0.5d);
                }
            }
        }

        void setPositionRight(MiuiFreeFormActivityStack stack, float right) {
            if (stack == null) {
                MiuiCvwGestureController.Slog.d(MiuiCvwLayerSupervisor.TAG, "setPositionY stack is null,return;");
            } else if (stack.mTask == null) {
                MiuiCvwGestureController.Slog.d(MiuiCvwLayerSupervisor.TAG, "setPositionY task is null,return;");
            } else {
                MiuiCvwAnimator.AnimalLock animalLock = (MiuiCvwAnimator.AnimalLock) MiuiCvwLayerSupervisor.this.mWindowLocks.get(stack);
                if (animalLock != null && animalLock.mCurrentAnimation != 11) {
                    MiuiCvwLayerSupervisor.this.mPosRight = (int) (right + 0.5d);
                    MiuiCvwGestureController.Slog.d(MiuiCvwLayerSupervisor.TAG, "setPositionRight :" + MiuiCvwLayerSupervisor.this.mPosRight);
                }
            }
        }

        void setPositionBottom(MiuiFreeFormActivityStack stack, float bottom) {
            if (stack == null) {
                MiuiCvwGestureController.Slog.d(MiuiCvwLayerSupervisor.TAG, "setPositionY stack is null,return;");
            } else if (stack.mTask == null) {
                MiuiCvwGestureController.Slog.d(MiuiCvwLayerSupervisor.TAG, "setPositionY task is null,return;");
            } else {
                MiuiCvwAnimator.AnimalLock animalLock = (MiuiCvwAnimator.AnimalLock) MiuiCvwLayerSupervisor.this.mWindowLocks.get(stack);
                if (animalLock != null && animalLock.mCurrentAnimation != 11) {
                    MiuiCvwLayerSupervisor.this.mPosBottom = (int) (bottom + 0.5d);
                }
            }
        }

        void setAlpha(MiuiFreeFormActivityStack stack, float alpha) {
            MiuiCvwLayerSupervisor.this.mCurrentAlpha = alpha;
        }

        void setXScale(MiuiFreeFormActivityStack stack, float scale) {
            MiuiCvwLayerSupervisor.this.mCurrentXScale = scale;
        }

        void setYScale(MiuiFreeFormActivityStack stack, float scale) {
            MiuiCvwLayerSupervisor.this.mCurrentYScale = scale;
        }
    };
    private final MiuiCvwAnimator.SpringAnimatorListener<MiuiFreeFormActivityStack> mHomeTaskSpringAnimatorListener = new MiuiCvwAnimator.SpringAnimatorListener<MiuiFreeFormActivityStack>() { // from class: com.android.server.wm.MiuiCvwLayerSupervisor.4
        public void updateValue(MiuiFreeFormActivityStack stack, float value, int springAnimalType) {
            if (stack == null) {
                MiuiCvwGestureController.Slog.d(MiuiCvwLayerSupervisor.TAG, "updateHomeTaskValue stack is null,return;");
            } else if (stack.mTask == null) {
                MiuiCvwGestureController.Slog.d(MiuiCvwLayerSupervisor.TAG, "updateHomeTaskValue task is null,return;");
            } else {
                MiuiCvwAnimator.AnimalLock animalLock = (MiuiCvwAnimator.AnimalLock) MiuiCvwLayerSupervisor.this.mWindowLocks.get(stack);
                if (animalLock == null) {
                    return;
                }
                float x = (MiuiCvwPolicy.mCurrScreenWidth / 2.0f) - ((MiuiCvwPolicy.mCurrScreenWidth * value) / 2.0f);
                float y = (MiuiCvwPolicy.mCurrScreenHeight / 2.0f) - ((MiuiCvwPolicy.mCurrScreenHeight * value) / 2.0f);
                MiuiCvwLayerSupervisor.this.mAnimator.setPositionInTransaction(stack, x, y);
                MiuiCvwLayerSupervisor.this.mAnimator.setMatrixInTransaction(stack, value, MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA, value);
                MiuiCvwLayerSupervisor.this.mAnimator.applyTransaction();
            }
        }

        @Override // com.android.server.wm.MiuiCvwAnimator.SpringAnimatorListener
        public void onAnimationEnd(MiuiFreeFormDynamicAnimation<?> animation, String propertyType) {
            MiuiCvwAnimator.AnimalLock animalLock = (MiuiCvwAnimator.AnimalLock) MiuiCvwLayerSupervisor.this.mWindowLocks.get(animation.mTarget);
            if (animalLock == null) {
                return;
            }
            MiuiCvwGestureController.Slog.d(MiuiCvwLayerSupervisor.TAG, "onHomeTaskScaleAnimationEnd\u3000animalLock:" + animalLock);
            MiuiCvwLayerSupervisor.this.onAnimationEnd(animalLock, animation);
        }
    };

    /* loaded from: classes.dex */
    public interface IWindowFrameReceiver {
        void doFrame(int i, int i2, int i3, int i4);
    }

    public void requestTraversal(long delay) {
        long rateDelay = 0;
        if (delay > 0) {
            rateDelay = delay;
        }
        this.mChoreographer.postFrameCallbackDelayed(this.mChoreographerCallback, rateDelay);
    }

    public MiuiCvwLayerSupervisor(MiuiCvwGestureController controller, MiuiCvwGestureHandlerImpl handler) {
        this.mController = controller;
        this.mGestureHandlerImpl = handler;
        initHandler();
        register(new IWindowFrameReceiver() { // from class: com.android.server.wm.MiuiCvwLayerSupervisor$$ExternalSyntheticLambda0
            @Override // com.android.server.wm.MiuiCvwLayerSupervisor.IWindowFrameReceiver
            public final void doFrame(int i, int i2, int i3, int i4) {
                MiuiCvwLayerSupervisor.this.applyTaskLeashFrame(i, i2, i3, i4);
            }
        });
        MiuiCvwAnimator miuiCvwAnimator = new MiuiCvwAnimator(controller, handler);
        this.mAnimator = miuiCvwAnimator;
        this.mCoverLayer = new MiuiCvwCoverLayer(controller, this, miuiCvwAnimator);
        this.mBackgroundLayer = new MiuiCvwBackgroundLayer(controller);
    }

    private void initHandler() {
        HandlerThread viewRenderThread = new HandlerThread(CVW_VIEW_RENDER_THREAD, -4);
        viewRenderThread.start();
        MiuiCvwGestureController.H h = new MiuiCvwGestureController.H(viewRenderThread.getLooper());
        this.mViewRenderHandler = h;
        h.post(new Runnable() { // from class: com.android.server.wm.MiuiCvwLayerSupervisor$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                MiuiCvwLayerSupervisor.this.m1588xca97d7f7();
            }
        });
        this.mExecuteHandler = new MiuiCvwGestureController.H(this.mController.mExecuteThread.getLooper()) { // from class: com.android.server.wm.MiuiCvwLayerSupervisor.2
            @Override // com.android.server.wm.MiuiCvwGestureController.H, android.os.Handler
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                int what = msg.what;
                switch (what) {
                    case 1:
                        MiuiCvwLayerSupervisor.this.preExecuteInner((MiuiFreeFormActivityStack) msg.obj, msg.arg1);
                        return;
                    case 2:
                        MiuiCvwLayerSupervisor.this.preResizeTaskInner((MiuiFreeFormActivityStack) msg.obj);
                        return;
                    default:
                        return;
                }
            }
        };
    }

    /* renamed from: lambda$initHandler$0$com-android-server-wm-MiuiCvwLayerSupervisor */
    public /* synthetic */ void m1588xca97d7f7() {
        this.mChoreographer = Choreographer.getInstance();
    }

    public boolean checkAnimatingStack() {
        if (this.mAnimatingStack == null) {
            resetAllState();
            return false;
        }
        return true;
    }

    public void register(IWindowFrameReceiver receiver) {
        if (!this.mWinFrameReceivers.contains(receiver)) {
            this.mWinFrameReceivers.add(receiver);
        }
    }

    public void unregister(IWindowFrameReceiver receiver) {
        this.mWinFrameReceivers.remove(receiver);
    }

    public void createTaskLeash(final MiuiCvwPolicy.TaskWrapperInfo info) {
        ActivityRecord activityRecord;
        if (info.stack == null || info.stack.mTask == null || (activityRecord = info.stack.mTask.getTopVisibleActivity()) == null) {
            return;
        }
        WindowState w = activityRecord.findMainWindow();
        if (w != null && info.stack != null) {
            addCVWStatus(4096);
            info.stack.createLeashIfNeeded(getFreeformRadius(false));
            this.mExecuteHandler.postAtFrontOfQueue(new Runnable() { // from class: com.android.server.wm.MiuiCvwLayerSupervisor$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    MiuiCvwLayerSupervisor.this.m1587xc22468b4(info);
                }
            });
        }
    }

    /* renamed from: lambda$createTaskLeash$1$com-android-server-wm-MiuiCvwLayerSupervisor */
    public /* synthetic */ void m1587xc22468b4(MiuiCvwPolicy.TaskWrapperInfo info) {
        if (info.supportCvw && info.stack != null) {
            this.mCoverLayer.createCoverLayer(info);
        }
    }

    public void showCoverLayer() {
        if (isCovering()) {
            MiuiCvwGestureController.Slog.d(TAG, "Overlays is showing,no required.");
            return;
        }
        addCVWStatus(4);
        this.mCoverLayer.requestShowOverlays();
    }

    public void createBackgroundLayer(Task task) {
        this.mBackgroundLayer.createLayer(task);
    }

    public void updateBackgroundLayerStatus(MiuiFreeFormActivityStack task, Rect scale, Rect src) {
        if (this.mGestureHandlerImpl.getLaunchWinMode() == 0 && task.getFreeFormConrolSurface() != null) {
            this.mBackgroundLayer.updateLayerStatus(task.getFreeFormConrolSurface(), scale, src);
        }
    }

    public void resetAllState() {
        this.mController.cancelCVWGesture();
        this.mAnimatingStack = null;
        this.mWindowLocks.clear();
        setTaskAnimationFinished();
        removeBackgroundLayer(null);
        this.mCoverLayer.remove();
        removeTaskLeashUi();
    }

    public void removeBackgroundLayer(MiuiFreeFormActivityStack stack) {
        this.mBackgroundLayer.removeLayer(stack);
    }

    public void removeCoverLayer(long delay) {
        this.mCoverLayer.remove(delay);
    }

    public void notifyFullscreenEntering() {
        this.mCoverLayer.notifyFullscreenEntering();
    }

    public void notifyMiniEntering() {
        this.mCoverLayer.notifyMiniEntering();
    }

    public void notifyFreeformEntering() {
        this.mCoverLayer.notifyFreeformEntering();
    }

    void notifyEnterMini() {
        this.mCoverLayer.notifyEnterMini();
    }

    public void showDim(MiuiFreeFormActivityStack stack) {
        this.mBackgroundLayer.showDim(stack);
    }

    public void hideDim(MiuiFreeFormActivityStack stack) {
        this.mBackgroundLayer.hideDim(stack);
    }

    public boolean isGestureWinFreeform() {
        return this.mGestureHandlerImpl.isGestureWinFreeform();
    }

    public boolean isGestureWinFullscreen() {
        return this.mGestureHandlerImpl.isGestureWinFullscreen();
    }

    public boolean isGestureWinMini() {
        return this.mGestureHandlerImpl.isGestureWinMini();
    }

    public float getFreeformRadius(boolean isSmallFreeform) {
        return isSmallFreeform ? MiuiMultiWindowUtils.getSmallFreeformRoundCorner(this.mController.mWmService.mContext) : MiuiMultiWindowUtils.getFreeformRoundCorner(this.mController.mWmService.mContext);
    }

    public float computeFreeformRadius(boolean isSmallFreeform, float scale) {
        float radius = MiuiMultiWindowUtils.getFreeformRoundCorner(this.mController.mWmService.mContext);
        if ((this.mGestureHandlerImpl.getLaunchWinMode() == 2 && !this.mGestureHandlerImpl.isResizeFinished()) || (this.mGestureHandlerImpl.isResizeFinished() && isSmallFreeform)) {
            radius = MiuiMultiWindowUtils.getSmallFreeformRoundCorner(this.mController.mWmService.mContext);
        }
        return radius / scale;
    }

    public boolean isAnimating() {
        return isCVWTaskAnimating() || isCovering();
    }

    private void scaleHomeTask() {
        MiuiFreeFormActivityStack stack = this.mController.mMiuiFreeFormManagerService.getHomeActivityStack();
        if (stack != null) {
            stack.createLeashIfNeeded(MiuiFreeformPinManagerService.EDGE_AREA);
            MiuiCvwAnimator.AnimalLock animalLock = this.mWindowLocks.get(stack);
            if (animalLock == null) {
                animalLock = new MiuiCvwAnimator.AnimalLock(stack);
                this.mWindowLocks.put(stack, animalLock);
            }
            this.mAnimator.startHomeLeashAnimation(stack, animalLock, this.mHomeTaskSpringAnimatorListener);
        }
    }

    private void removeHomeLeash() {
        MiuiFreeFormActivityStack stack = this.mController.mMiuiFreeFormManagerService.getHomeActivityStack();
        if (stack != null) {
            this.mAnimator.removeAnimationControlLeash(stack);
        }
    }

    public MiuiCvwLayerSupervisor useAt(MiuiFreeFormActivityStack stack) {
        this.mAnimatingStack = stack;
        return this;
    }

    public void prepareSetTo(int... propertyAndValues) {
        if (propertyAndValues.length == 0 || (propertyAndValues.length & 1) == 1) {
            return;
        }
        for (int i = 0; i < propertyAndValues.length; i += 2) {
            prepareSetTo(propertyAndValues[i], propertyAndValues[i + 1]);
        }
    }

    void prepareSetTo(int property, float value) {
        switch (property) {
            case 1:
                this.mCurrentAlpha = value;
                return;
            case 2:
                this.mPosLeft = (int) (value + 0.5d);
                return;
            case 3:
                this.mPosTop = (int) (value + 0.5d);
                return;
            case 4:
                this.mCurrentScale = value;
                return;
            case 5:
                this.mCurrentXScale = value;
                return;
            case 6:
                this.mCurrentYScale = value;
                return;
            case 7:
                this.mPosRight = (int) (value + 0.5d);
                return;
            case 8:
                this.mPosBottom = (int) (value + 0.5d);
                return;
            default:
                return;
        }
    }

    public void scrollTo(Rect to) {
        MiuiFreeFormActivityStack miuiFreeFormActivityStack = this.mAnimatingStack;
        if (miuiFreeFormActivityStack == null) {
            MiuiCvwGestureController.Slog.d(TAG, "setYScale stack is null,return;");
        } else if (miuiFreeFormActivityStack.mTask == null) {
            MiuiCvwGestureController.Slog.d(TAG, "setYScale task is null,return;");
        } else {
            this.mPosLeft = to.left;
            this.mPosTop = to.top;
            this.mPosRight = to.right;
            this.mPosBottom = to.bottom;
            requestTraversal(getFirstPhase());
        }
    }

    private long getFirstPhase() {
        return (((float) this.mChoreographer.getFrameIntervalNanos()) * 1.0E-6f) / 4.0f;
    }

    float getRefreshRateMillis() {
        return ((float) this.mChoreographer.getFrameIntervalNanos()) * 1.0E-6f;
    }

    public MiuiCvwLayerSupervisor to(int... propertyAndValues) {
        if (propertyAndValues.length == 0 || (propertyAndValues.length & 1) == 1) {
            MiuiCvwGestureController.Slog.i(TAG, "to params is invalid");
            return this;
        }
        for (int i = 0; i < propertyAndValues.length; i += 2) {
            to(propertyAndValues[i], propertyAndValues[i + 1]);
        }
        return this;
    }

    MiuiCvwLayerSupervisor to(int springAnimalType, float endValue) {
        MiuiCvwAnimator.AnimalLock animalLock = this.mWindowLocks.get(this.mAnimatingStack);
        if (animalLock == null) {
            animalLock = new MiuiCvwAnimator.AnimalLock(this.mAnimatingStack);
            this.mWindowLocks.put(this.mAnimatingStack, animalLock);
        }
        this.mAnimator.createSpringAnimation(this.mAnimatingStack, springAnimalType, getValue(springAnimalType), endValue, animalLock, this.mTaskSpringAnimatorListener);
        return this;
    }

    private float getValue(int springAnimalType) {
        switch (springAnimalType) {
            case 1:
                return this.mCurrentAlpha;
            case 2:
                return this.mPosLeft;
            case 3:
                return this.mPosTop;
            case 4:
                return this.mCurrentScale;
            case 5:
                return this.mCurrentXScale;
            case 6:
                return this.mCurrentYScale;
            case 7:
                return this.mPosRight;
            case 8:
                return this.mPosBottom;
            default:
                return MiuiFreeformPinManagerService.EDGE_AREA;
        }
    }

    public MiuiCvwLayerSupervisor spring(float stiffness, float damping) {
        MiuiCvwAnimator.AnimalLock animalLock = this.mWindowLocks.get(this.mAnimatingStack);
        if (animalLock == null) {
            animalLock = new MiuiCvwAnimator.AnimalLock(this.mAnimatingStack);
            this.mWindowLocks.put(this.mAnimatingStack, animalLock);
        }
        animalLock.mStiffness = stiffness;
        animalLock.mDamping = damping;
        return this;
    }

    public void start(int animationType, boolean isUp) {
        MiuiCvwAnimator.AnimalLock animalLock = this.mWindowLocks.get(this.mAnimatingStack);
        if (animalLock == null) {
            MiuiCvwGestureController.Slog.i(TAG, "animalLock is null");
            return;
        }
        if (isUp) {
            this.mAnimator.addTaskEndListener(animationType, animalLock, this.mTaskSpringAnimatorListener);
        }
        animalLock.start(animationType);
        if (!isCVWTaskAnimating()) {
            addCVWStatus(512);
            requestTraversal(getFirstPhase());
            this.mController.setMagicSplitBarAndWallpaperVisibility(this.mAnimatingStack.mTask, false);
        }
    }

    public void onAnimationEnd(MiuiCvwAnimator.AnimalLock animalLock, MiuiFreeFormDynamicAnimation dynamicAnimation) {
        animalLock.animationEnd(dynamicAnimation);
        MiuiCvwGestureController.Slog.d(TAG, "onAnimationEnd currentAnimation:" + animalLock.mCurrentAnimation + " isAnimalFinished:" + animalLock.isAnimalFinished() + ", " + Thread.currentThread().getName());
        if (animalLock.isAnimalFinished()) {
            if (this.mAnimatingStack == animalLock.mStack) {
                this.mAnimatingStack = null;
            }
            this.mWindowLocks.remove(animalLock.mStack);
            setTaskAnimationFinished();
            allAnimalFinishedActions(animalLock);
            animalLock.resetAnimalState();
        }
    }

    void setTaskAnimationFinished() {
        removeCVWStatus(512);
        this.mCoverLayer.tryRemoveOverlays();
    }

    public boolean isAllAnimalFinished() {
        return !isCVWTaskAnimating();
    }

    private void allAnimalFinishedActions(MiuiCvwAnimator.AnimalLock animalLock) {
        int i;
        MiuiCvwPolicy.TaskWrapperInfo info = this.mController.mCvwPolicy.getResultTaskWrapperInfo();
        MiuiFreeFormGestureController miuiFreeFormGestureController = (MiuiFreeFormGestureController) this.mController.mWmService.mMiuiFreeFormGestureController;
        switch (animalLock.mCurrentAnimation) {
            case 0:
            case 1:
                if (info != null) {
                    MiuiFreeFormActivityStack stack = this.mController.mStack;
                    if (this.mController.mMiuiFreeFormManagerService != null && stack != null) {
                        this.mController.isFreeformResizingByCvw = true;
                        this.mController.mMiuiFreeFormManagerService.updateMiuiFreeFormStackBounds(stack.mStackID, info.scale, info.actualBounds, info.miniBounds, this.mController.mTrackEvent);
                    }
                    this.mController.notifyFreeFormApplicationResizeEnd();
                    break;
                } else {
                    MiuiCvwGestureController.Slog.d(TAG, "last TaskWrapperInfo is null,return");
                    return;
                }
            case 5:
                if (this.mController.mStack != null) {
                    MiuiCvwGestureController miuiCvwGestureController = this.mController;
                    miuiCvwGestureController.setMagicSplitBarAndWallpaperVisibility(miuiCvwGestureController.mStack.mTask, true);
                }
                this.mBackgroundLayer.adjustHideWallpaperTime(true);
                break;
            case 7:
            case 8:
            case 18:
                if (info != null) {
                    if (this.mController.mStack != null) {
                        this.mController.isFreeformResizingByCvw = true;
                        this.mController.mMiuiFreeFormManagerService.launchSmallFreeformByFreeform(this.mController.mStack.mTask, this.mAnimator, this.mGestureHandlerImpl, info.actualBounds, info.miniBounds, info.scale, this.mController.mTrackEvent);
                        break;
                    }
                } else {
                    MiuiCvwGestureController.Slog.d(TAG, "last TaskWrapperInfo is null,return");
                    return;
                }
                break;
            case 12:
                if (info == null) {
                    MiuiCvwGestureController.Slog.d(TAG, "last TaskWrapperInfo is null,return");
                    return;
                } else if (this.mController.mMiuiFreeFormManagerService != null && this.mController.mStack != null && this.mController.mStack != null) {
                    i = 3;
                    try {
                        try {
                            this.mController.mStack.mFreeFormScale = 1.0f;
                            this.mController.mWmService.mAtmService.resizeTask(this.mController.mStack.mStackID, new Rect(0, 0, this.mGestureHandlerImpl.mScreenWidth, this.mGestureHandlerImpl.mScreenHeight), 0);
                            miuiFreeFormGestureController.mTrackManager.trackSmallWindowQuitEvent(MiuiFreeformTrackManager.SmallWindowTrackConstants.QUIT_WAY_NAME5, this.mController.mTrackEvent.targetStackRatio, this.mController.mStack.getStackPackageName(), this.mController.mStack.getApplicationName(), -1L);
                        } catch (Exception e) {
                            e.printStackTrace();
                            miuiFreeFormGestureController.mTrackManager.trackSmallWindowQuitEvent(MiuiFreeformTrackManager.SmallWindowTrackConstants.QUIT_WAY_NAME5, this.mController.mTrackEvent.targetStackRatio, this.mController.mStack.getStackPackageName(), this.mController.mStack.getApplicationName(), -1L);
                        }
                        miuiFreeFormGestureController.mGestureListener.startShowFullScreenWindow(i, this.mController.mStack);
                        break;
                    } catch (Throwable th) {
                        miuiFreeFormGestureController.mTrackManager.trackSmallWindowQuitEvent(MiuiFreeformTrackManager.SmallWindowTrackConstants.QUIT_WAY_NAME5, this.mController.mTrackEvent.targetStackRatio, this.mController.mStack.getStackPackageName(), this.mController.mStack.getApplicationName(), -1L);
                        throw th;
                    }
                }
                break;
            case 15:
            case 16:
                if (info != null) {
                    if (this.mController.mStack != null) {
                        this.mController.isFreeformResizingByCvw = true;
                        this.mController.mMiuiFreeFormManagerService.launchFreeformBySmallFreeform(this.mController.mStack, info.actualBounds, info.scale, this.mController.mTrackEvent);
                        miuiFreeFormGestureController.mTrackManager.trackSmallWindowEnterWayEvent(MiuiFreeformTrackManager.SmallWindowTrackConstants.ENTER_WAY_NAME5, this.mController.mTrackEvent.targetStackRatio, this.mController.mStack.getStackPackageName(), this.mController.mStack.getApplicationName(), this.mController.mMiuiFreeFormManagerService.mFreeFormActivityStacks.size());
                        miuiFreeFormGestureController.mTrackManager.trackMiniWindowQuitEvent(MiuiFreeformTrackManager.MiniWindowTrackConstants.QUIT_WAY_NAME4, new Point(this.mController.mStack.mStackControlInfo.mSmallWindowBounds.left, this.mController.mStack.mStackControlInfo.mSmallWindowBounds.top), this.mController.mStack.getStackPackageName(), this.mController.mStack.getApplicationName());
                        break;
                    }
                } else {
                    MiuiCvwGestureController.Slog.d(TAG, "last TaskWrapperInfo is null,return");
                    return;
                }
                break;
            case 17:
                if (info != null) {
                    if (this.mController.mMiuiFreeFormManagerService != null && this.mController.mStack != null && this.mController.mStack != null) {
                        i = 5;
                        try {
                            try {
                                this.mController.mStack.mFreeFormScale = 1.0f;
                                this.mController.mWmService.mAtmService.resizeTask(this.mController.mStack.mStackID, new Rect(0, 0, this.mGestureHandlerImpl.mScreenWidth, this.mGestureHandlerImpl.mScreenHeight), 0);
                            } finally {
                                miuiFreeFormGestureController.mGestureListener.startShowFullScreenWindow(i, this.mController.mStack);
                            }
                        } catch (Exception e2) {
                            e2.printStackTrace();
                        }
                        break;
                    }
                } else {
                    MiuiCvwGestureController.Slog.d(TAG, "last TaskWrapperInfo is null,return");
                    return;
                }
                break;
        }
        if (this.mController.mStack != null) {
            this.mController.mStack.endControl();
        }
        resetStateAfterAllAnimationEnd(animalLock.mCurrentAnimation);
    }

    /* JADX WARN: Code restructure failed: missing block: B:28:0x00be, code lost:
        if (r5.mGestureHandlerImpl.mTaskWrapperInfo == null) goto L23;
     */
    /* JADX WARN: Code restructure failed: missing block: B:29:0x00c1, code lost:
        return;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public void removeTaskLeashUi() {
        /*
            r5 = this;
            java.lang.String r0 = "removeTaskLeash finally:"
            boolean r1 = r5.isAnimating()
            if (r1 == 0) goto L9
            return
        L9:
            com.android.server.wm.MiuiCvwGestureHandlerImpl r1 = r5.mGestureHandlerImpl
            boolean r1 = r1.isResizeFinished()
            if (r1 != 0) goto L12
            return
        L12:
            com.android.server.wm.MiuiCvwGestureController r1 = r5.mController     // Catch: java.lang.Throwable -> L83 java.lang.Exception -> L85
            com.android.server.wm.MiuiFreeFormActivityStack r1 = r1.mStack     // Catch: java.lang.Throwable -> L83 java.lang.Exception -> L85
            if (r1 == 0) goto L52
            com.android.server.wm.MiuiCvwGestureController r1 = r5.mController     // Catch: java.lang.Throwable -> L83 java.lang.Exception -> L85
            com.android.server.wm.MiuiFreeFormActivityStack r1 = r1.mStack     // Catch: java.lang.Throwable -> L83 java.lang.Exception -> L85
            int r1 = r1.mMiuiFreeFromWindowMode     // Catch: java.lang.Throwable -> L83 java.lang.Exception -> L85
            r2 = -1
            if (r1 != r2) goto L32
            com.android.server.wm.MiuiCvwGestureController r1 = r5.mController     // Catch: java.lang.Throwable -> L83 java.lang.Exception -> L85
            com.android.server.wm.MiuiFreeFormManagerService r1 = r1.mMiuiFreeFormManagerService     // Catch: java.lang.Throwable -> L83 java.lang.Exception -> L85
            com.android.server.wm.MiuiCvwGestureController r2 = r5.mController     // Catch: java.lang.Throwable -> L83 java.lang.Exception -> L85
            com.android.server.wm.MiuiFreeFormActivityStack r2 = r2.mStack     // Catch: java.lang.Throwable -> L83 java.lang.Exception -> L85
            int r2 = r2.mStackID     // Catch: java.lang.Throwable -> L83 java.lang.Exception -> L85
            com.android.server.wm.MiuiFreeFormActivityStackStub r1 = r1.getMiuiFreeFormActivityStack(r2)     // Catch: java.lang.Throwable -> L83 java.lang.Exception -> L85
            com.android.server.wm.MiuiFreeFormActivityStack r1 = (com.android.server.wm.MiuiFreeFormActivityStack) r1     // Catch: java.lang.Throwable -> L83 java.lang.Exception -> L85
            goto L36
        L32:
            com.android.server.wm.MiuiCvwGestureController r1 = r5.mController     // Catch: java.lang.Throwable -> L83 java.lang.Exception -> L85
            com.android.server.wm.MiuiFreeFormActivityStack r1 = r1.mStack     // Catch: java.lang.Throwable -> L83 java.lang.Exception -> L85
        L36:
            if (r1 != 0) goto L3e
            com.android.server.wm.MiuiCvwGestureController r2 = r5.mController     // Catch: java.lang.Throwable -> L83 java.lang.Exception -> L85
            com.android.server.wm.MiuiFreeFormActivityStack r2 = r2.mStack     // Catch: java.lang.Throwable -> L83 java.lang.Exception -> L85
            r1 = r2
        L3e:
            boolean r2 = r1.isInMiniFreeFormMode()     // Catch: java.lang.Throwable -> L83 java.lang.Exception -> L85
            if (r2 != 0) goto L4d
            com.android.server.wm.MiuiCvwAnimator r2 = r5.mAnimator     // Catch: java.lang.Throwable -> L83 java.lang.Exception -> L85
            com.android.server.wm.MiuiCvwGestureController r3 = r5.mController     // Catch: java.lang.Throwable -> L83 java.lang.Exception -> L85
            com.android.server.wm.MiuiFreeFormActivityStack r3 = r3.mStack     // Catch: java.lang.Throwable -> L83 java.lang.Exception -> L85
            r2.removeAnimationControlLeash(r3)     // Catch: java.lang.Throwable -> L83 java.lang.Exception -> L85
        L4d:
            r2 = 8192(0x2000, float:1.148E-41)
            r5.addCVWStatus(r2)     // Catch: java.lang.Throwable -> L83 java.lang.Exception -> L85
        L52:
            java.lang.String r1 = com.android.server.wm.MiuiCvwLayerSupervisor.TAG
            java.lang.StringBuilder r2 = new java.lang.StringBuilder
            r2.<init>()
            java.lang.StringBuilder r0 = r2.append(r0)
            java.lang.Thread r2 = java.lang.Thread.currentThread()
            java.lang.String r2 = r2.getName()
            java.lang.StringBuilder r0 = r0.append(r2)
            java.lang.String r0 = r0.toString()
            com.android.server.wm.MiuiCvwGestureController.Slog.d(r1, r0)
            com.android.server.wm.MiuiCvwGestureHandlerImpl r0 = r5.mGestureHandlerImpl
            com.android.server.wm.MiuiCvwPolicy$TaskWrapperInfo r0 = r0.mTaskWrapperInfo
            if (r0 == 0) goto L7d
        L76:
            com.android.server.wm.MiuiCvwGestureHandlerImpl r0 = r5.mGestureHandlerImpl
            com.android.server.wm.MiuiCvwPolicy$TaskWrapperInfo r0 = r0.mTaskWrapperInfo
            r0.reset()
        L7d:
            com.android.server.wm.MiuiCvwGestureController r0 = r5.mController
            r0.resetStateUi()
            goto Lc1
        L83:
            r1 = move-exception
            goto Lc2
        L85:
            r1 = move-exception
            java.lang.String r2 = com.android.server.wm.MiuiCvwLayerSupervisor.TAG     // Catch: java.lang.Throwable -> L83
            java.lang.StringBuilder r3 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L83
            r3.<init>()     // Catch: java.lang.Throwable -> L83
            java.lang.String r4 = "resetState error:"
            java.lang.StringBuilder r3 = r3.append(r4)     // Catch: java.lang.Throwable -> L83
            java.lang.StringBuilder r3 = r3.append(r1)     // Catch: java.lang.Throwable -> L83
            java.lang.String r3 = r3.toString()     // Catch: java.lang.Throwable -> L83
            com.android.server.wm.MiuiCvwGestureController.Slog.d(r2, r3)     // Catch: java.lang.Throwable -> L83
            java.lang.StringBuilder r1 = new java.lang.StringBuilder
            r1.<init>()
            java.lang.StringBuilder r0 = r1.append(r0)
            java.lang.Thread r1 = java.lang.Thread.currentThread()
            java.lang.String r1 = r1.getName()
            java.lang.StringBuilder r0 = r0.append(r1)
            java.lang.String r0 = r0.toString()
            com.android.server.wm.MiuiCvwGestureController.Slog.d(r2, r0)
            com.android.server.wm.MiuiCvwGestureHandlerImpl r0 = r5.mGestureHandlerImpl
            com.android.server.wm.MiuiCvwPolicy$TaskWrapperInfo r0 = r0.mTaskWrapperInfo
            if (r0 == 0) goto L7d
            goto L76
        Lc1:
            return
        Lc2:
            java.lang.String r2 = com.android.server.wm.MiuiCvwLayerSupervisor.TAG
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            r3.<init>()
            java.lang.StringBuilder r0 = r3.append(r0)
            java.lang.Thread r3 = java.lang.Thread.currentThread()
            java.lang.String r3 = r3.getName()
            java.lang.StringBuilder r0 = r0.append(r3)
            java.lang.String r0 = r0.toString()
            com.android.server.wm.MiuiCvwGestureController.Slog.d(r2, r0)
            com.android.server.wm.MiuiCvwGestureHandlerImpl r0 = r5.mGestureHandlerImpl
            com.android.server.wm.MiuiCvwPolicy$TaskWrapperInfo r0 = r0.mTaskWrapperInfo
            if (r0 == 0) goto Led
            com.android.server.wm.MiuiCvwGestureHandlerImpl r0 = r5.mGestureHandlerImpl
            com.android.server.wm.MiuiCvwPolicy$TaskWrapperInfo r0 = r0.mTaskWrapperInfo
            r0.reset()
        Led:
            com.android.server.wm.MiuiCvwGestureController r0 = r5.mController
            r0.resetStateUi()
            throw r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wm.MiuiCvwLayerSupervisor.removeTaskLeashUi():void");
    }

    public SurfaceControl hideTask() {
        MiuiFreeFormActivityStack stack = this.mGestureHandlerImpl.mTaskWrapperInfo.stack;
        if (stack != null && stack.mTask != null) {
            this.mAnimator.setTaskAlphaInTransaction(stack, MiuiFreeformPinManagerService.EDGE_AREA);
            return stack.mTask.getSurfaceControl();
        }
        return null;
    }

    public SurfaceControl showTask() {
        MiuiFreeFormActivityStack stack = this.mGestureHandlerImpl.mTaskWrapperInfo.stack;
        if (stack != null && stack.mTask != null) {
            this.mAnimator.setTaskAlphaInTransaction(stack, 1.0f);
            return stack.mTask.getSurfaceControl();
        }
        return null;
    }

    public void updateRefVisualBounds() {
        MiuiFreeFormActivityStack stack;
        MiuiCvwPolicy.TaskWrapperInfo info = this.mGestureHandlerImpl.mCvwPolicy.getTmpFreeformTaskWrapper();
        if (info != null && info.isValid()) {
            float visualScale = 1.0f;
            Rect visualBounds = info.visualBounds;
            MiuiCvwPolicy.TaskWrapperInfo wrapperInfo = this.mGestureHandlerImpl.mTaskWrapperInfo;
            if (wrapperInfo != null && (stack = this.mGestureHandlerImpl.mTaskWrapperInfo.stack) != null && stack.isInMiniFreeFormMode() && isGestureWinMini()) {
                visualScale = (stack.mStackControlInfo.mSmallWindowBounds.height() * 1.0f) / (info.scale * info.actualBounds.height());
                visualBounds = wrapperInfo.visualBounds;
            }
            this.mGestureHandlerImpl.updateTaskWrapperVisualBounds(visualBounds, visualScale);
        }
    }

    public boolean applyTaskLeashFrame(int l, int t, int r, int b) {
        MiuiCvwPolicy.TaskWrapperInfo wrapperInfo = this.mGestureHandlerImpl.mTaskWrapperInfo;
        if (wrapperInfo == null) {
            MiuiCvwGestureController.Slog.d(TAG, "WrapperInfo is null,return;");
            return false;
        }
        MiuiFreeFormActivityStack stack = wrapperInfo.stack;
        if (stack == null) {
            MiuiCvwGestureController.Slog.d(TAG, "stack is null,return;");
            return false;
        } else if (stack.mTask == null) {
            MiuiCvwGestureController.Slog.d(TAG, "task is null,return;");
            return false;
        } else if (isAllAnimalFinished() && wrapperInfo.supportCvw) {
            MiuiCvwGestureController.Slog.d(TAG, "isAllAnimalFinished,return;");
            return false;
        } else if (wrapperInfo.visualBounds == null || wrapperInfo.visualBounds.isEmpty()) {
            MiuiCvwGestureController.Slog.d(TAG, "visualBounds is empty");
            return false;
        } else {
            geometrySet(l, t, r, b, wrapperInfo, stack);
            return true;
        }
    }

    void geometrySet(int l, int t, int r, int b, MiuiCvwPolicy.TaskWrapperInfo wrapperInfo, MiuiFreeFormActivityStack stack) {
        this.mCurrentWidth = r - l;
        this.mCurrentHeight = b - t;
        this.mScaleX = ((this.mCurrentWidth * 1.0f) / wrapperInfo.visualBounds.width()) * wrapperInfo.visualScale;
        this.mScaleY = ((this.mCurrentHeight * 1.0f) / wrapperInfo.visualBounds.height()) * wrapperInfo.visualScale;
        this.mScale = Math.max(this.mScaleX, this.mScaleY);
        this.mRadius = computeFreeformRadius(this.mCoverLayer.isMiniMode(), this.mScale);
        MiuiCvwAnimator.SurfaceParams params = new MiuiCvwAnimator.SurfaceParams.Builder(stack).withMatrix(this.mScaleX, this.mScaleY).withPosition(l, t).withCornerRadius(this.mRadius).build();
        this.mAnimator.mergeTransactionToView(params);
        this.mCoverLayer.applyWindowFrame(l, t, r, b);
    }

    public void preResizeTask(MiuiFreeFormActivityStack stack) {
        this.mController.windowDrawBegin();
        Message message = Message.obtain();
        message.what = 2;
        message.obj = stack;
        this.mExecuteHandler.sendMessageAtFrontOfQueue(message);
    }

    public void preResizeTask() {
        preResizeTask(this.mAnimatingStack);
    }

    public void preExecute(MiuiFreeFormActivityStack stack, int animtionType) {
        Message message = Message.obtain();
        message.what = 1;
        message.obj = stack;
        message.arg1 = animtionType;
        this.mExecuteHandler.sendMessageAtFrontOfQueue(message);
    }

    public void preResizeTaskInner(MiuiFreeFormActivityStack stack) {
        if (isOverlaysComing()) {
            setTaskResizeWaiting();
            MiuiCvwGestureController.Slog.d(TAG, "addOverlayStatus OVERLAYS_TASK_RESIZE_WAITING");
            return;
        }
        MiuiCvwPolicy.TaskWrapperInfo info = this.mGestureHandlerImpl.mCvwPolicy.getTmpFreeformTaskWrapper();
        if (stack != null && info != null && info.isValid() && this.mController.inFreeform(stack.mTask)) {
            try {
                stack.mFreeFormScale = info.scale;
                this.mController.mWmService.mAtmService.resizeTask(stack.mStackID, info.actualBounds, 1);
            } catch (Exception e) {
                MiuiCvwGestureController.Slog.e(TAG, "preResizeTaskInner error", e);
            }
        }
    }

    public void preStartFreeform(MiuiFreeFormActivityStack stack) {
        if (!isCovering()) {
            return;
        }
        MiuiCvwPolicy.TaskWrapperInfo info = this.mGestureHandlerImpl.mCvwPolicy.getTmpFreeformTaskWrapper();
        if (stack != null && info != null) {
            stack.mFreeFormScale = info.scale;
            synchronized (this.mController.mWmService.mAtmService.getGlobalLock()) {
                WindowContainerTransaction wct = new WindowContainerTransaction();
                try {
                    wct.setBounds(stack.mTask.mRemoteToken.toWindowContainerToken(), info.actualBounds);
                    this.mController.mWmService.mAtmService.mWindowOrganizerController.applyTransaction(wct);
                } catch (Exception e) {
                    MiuiCvwGestureController.Slog.e(TAG, "launchFreeformByCVW error", e);
                }
            }
        }
    }

    public void preExecuteInner(MiuiFreeFormActivityStack stack, int animtionType) {
        switch (animtionType) {
            case 3:
            case 4:
                if (this.mController.mMiuiFreeFormManagerService != null && this.mController.mStack != null) {
                    MiuiCvwPolicy.TaskWrapperInfo info = this.mGestureHandlerImpl.mCvwPolicy.getResultTaskWrapperInfo();
                    stack.setStackFreeFormMode(0);
                    this.mController.freezeHomeAppTransitionOnce();
                    this.mController.isFreeformResizingByCvw = true;
                    try {
                        this.mController.mMiuiFreeFormManagerService.launchFreeformByCVW(stack.mTask, this.mGestureHandlerImpl, this.mAnimator, info.actualBounds, info.miniBounds, info.scale, this.mController.mTrackEvent);
                        this.mGestureHandlerImpl.updateTaskWrapperVisualBounds(info.visualBounds);
                    } catch (Exception e) {
                    }
                }
                scaleHomeTask();
                break;
            case 9:
            case 10:
                MiuiCvwPolicy.TaskWrapperInfo info2 = this.mGestureHandlerImpl.mCvwPolicy.getResultTaskWrapperInfo();
                if (stack != null) {
                    stack.setStackFreeFormMode(1);
                    this.mController.freezeHomeAppTransitionOnce();
                    this.mController.isFreeformResizingByCvw = true;
                    try {
                        this.mController.mMiuiFreeFormManagerService.launchSmallFreeformByCVW(stack, this.mGestureHandlerImpl, this.mAnimator, info2.actualBounds, info2.miniBounds, info2.scale, this.mController.mTrackEvent);
                        this.mGestureHandlerImpl.updateTaskWrapperVisualBounds(info2.visualBounds);
                    } catch (Exception e2) {
                    }
                }
                scaleHomeTask();
                break;
        }
        resetStateAfterPreExecute(animtionType);
    }

    private void resetStateAfterAllAnimationEnd(int animtionType) {
        switch (animtionType) {
            case 0:
            case 1:
            case 3:
            case 4:
            case 15:
            case 16:
                if (!this.mCoverLayer.isCovering()) {
                    removeTaskLeashUi();
                }
                removeBackgroundLayer(null);
                return;
            case 2:
            case 6:
            case 13:
            case 14:
            case 17:
            default:
                return;
            case 5:
            case 9:
            case 10:
                removeBackgroundLayer(null);
                if (!this.mCoverLayer.isCovering()) {
                    removeTaskLeashUi();
                    return;
                } else {
                    this.mCoverLayer.remove();
                    return;
                }
            case 7:
            case 8:
            case 18:
                this.mCoverLayer.remove();
                removeBackgroundLayer(null);
                return;
            case 11:
                removeHomeLeash();
                return;
            case 12:
                this.mCoverLayer.remove(500L);
                removeTaskLeashUi();
                removeBackgroundLayer(null);
                return;
        }
    }

    private void resetStateAfterPreExecute(int animtionType) {
        switch (animtionType) {
            case 3:
            case 4:
            case 9:
            case 10:
            default:
                return;
        }
    }

    public void addCVWStatus(int status) {
        switch (status) {
            case 1:
                int i = this.mCVWStatus | 1;
                this.mCVWStatus = i;
                int i2 = i & (-3);
                this.mCVWStatus = i2;
                int i3 = i2 & (-5);
                this.mCVWStatus = i3;
                int i4 = i3 & (-9);
                this.mCVWStatus = i4;
                int i5 = i4 & (-17);
                this.mCVWStatus = i5;
                int i6 = i5 & (-33);
                this.mCVWStatus = i6;
                int i7 = i6 & (-65);
                this.mCVWStatus = i7;
                int i8 = i7 & (-1025);
                this.mCVWStatus = i8;
                this.mCVWStatus = i8 & (-2049);
                break;
            case 2:
                int i9 = this.mCVWStatus | 2;
                this.mCVWStatus = i9;
                int i10 = i9 & (-2);
                this.mCVWStatus = i10;
                int i11 = i10 & (-5);
                this.mCVWStatus = i11;
                int i12 = i11 & (-9);
                this.mCVWStatus = i12;
                int i13 = i12 & (-17);
                this.mCVWStatus = i13;
                int i14 = i13 & (-33);
                this.mCVWStatus = i14;
                int i15 = i14 & (-65);
                this.mCVWStatus = i15;
                int i16 = i15 & (-1025);
                this.mCVWStatus = i16;
                this.mCVWStatus = i16 & (-2049);
                break;
            case 4:
                int i17 = this.mCVWStatus | 4;
                this.mCVWStatus = i17;
                int i18 = i17 & (-9);
                this.mCVWStatus = i18;
                int i19 = i18 & (-17);
                this.mCVWStatus = i19;
                int i20 = i19 & (-33);
                this.mCVWStatus = i20;
                int i21 = i20 & (-65);
                this.mCVWStatus = i21;
                int i22 = i21 & (-1025);
                this.mCVWStatus = i22;
                this.mCVWStatus = i22 & (-2049);
                break;
            case 8:
                int i23 = this.mCVWStatus | 8;
                this.mCVWStatus = i23;
                int i24 = i23 & (-17);
                this.mCVWStatus = i24;
                int i25 = i24 & (-33);
                this.mCVWStatus = i25;
                int i26 = i25 & (-65);
                this.mCVWStatus = i26;
                int i27 = i26 & (-1025);
                this.mCVWStatus = i27;
                this.mCVWStatus = i27 & (-2049);
                break;
            case 16:
                int i28 = this.mCVWStatus | 16;
                this.mCVWStatus = i28;
                int i29 = i28 & (-5);
                this.mCVWStatus = i29;
                int i30 = i29 & (-33);
                this.mCVWStatus = i30;
                int i31 = i30 & (-65);
                this.mCVWStatus = i31;
                int i32 = i31 & (-1025);
                this.mCVWStatus = i32;
                this.mCVWStatus = i32 & (-2049);
                break;
            case 32:
                int i33 = this.mCVWStatus | 32;
                this.mCVWStatus = i33;
                int i34 = i33 & (-65);
                this.mCVWStatus = i34;
                int i35 = i34 & (-1025);
                this.mCVWStatus = i35;
                this.mCVWStatus = i35 & (-2049);
                break;
            case 64:
                int i36 = this.mCVWStatus | 64;
                this.mCVWStatus = i36;
                int i37 = i36 & (-1025);
                this.mCVWStatus = i37;
                this.mCVWStatus = i37 & (-2049);
                break;
            case 128:
                int i38 = this.mCVWStatus | 128;
                this.mCVWStatus = i38;
                this.mCVWStatus = i38 & (-257);
                break;
            case 256:
                int i39 = this.mCVWStatus | 256;
                this.mCVWStatus = i39;
                this.mCVWStatus = i39 & (-129);
                break;
            case 1024:
                int i40 = this.mCVWStatus | 1024;
                this.mCVWStatus = i40;
                this.mCVWStatus = i40 & (-2049);
                break;
            case 2048:
                this.mCVWStatus = 0;
                this.mCVWStatus = 0 | 2048;
                break;
            case 4096:
                int i41 = this.mCVWStatus | 4096;
                this.mCVWStatus = i41;
                this.mCVWStatus = i41 & (-8193);
                break;
            case 8192:
                int i42 = this.mCVWStatus | 8192;
                this.mCVWStatus = i42;
                this.mCVWStatus = i42 & (-4097);
                break;
            default:
                this.mCVWStatus |= status;
                break;
        }
        MiuiCvwGestureController.Slog.d(TAG, "addCVWStatus status:" + statusToString(status) + ",thread :" + Thread.currentThread().getName(), Build.IS_DEVELOPMENT_VERSION);
    }

    void removeCVWStatus(int status) {
        this.mCVWStatus &= ~status;
    }

    public int getCVWStatus() {
        return this.mCVWStatus;
    }

    void clearCVWStatus() {
        this.mCVWStatus = 0;
    }

    public void overlaysShowStart() {
        addCVWStatus(8);
    }

    public void overlaysShowFinished() {
        addCVWStatus(16);
    }

    public void overlaysHideStart() {
        addCVWStatus(32);
    }

    public void overlaysHideFinished() {
        addCVWStatus(64);
    }

    public boolean isCovering() {
        return (this.mCVWStatus & 8) != 0;
    }

    public boolean isCVWTaskAnimating() {
        return (this.mCVWStatus & 512) != 0;
    }

    public boolean isOverlaysComing() {
        return (this.mCVWStatus & 4) != 0;
    }

    public boolean isOverlaysHideFinished() {
        return (this.mCVWStatus & 64) != 0;
    }

    public boolean isOverlaysHiding() {
        return (this.mCVWStatus & 32) != 0;
    }

    public boolean isOverlaysRemoving() {
        return (this.mCVWStatus & 1024) != 0;
    }

    public boolean isOverlaysRemoved() {
        return (this.mCVWStatus & 2048) != 0;
    }

    public boolean taskResizeWaiting() {
        return (this.mCVWStatus & 128) != 0;
    }

    public boolean isTaskResized() {
        return (this.mCVWStatus & 256) != 0;
    }

    public boolean isOverlaysShowFinished() {
        return (this.mCVWStatus & 16) != 0;
    }

    public boolean isOverlayRemoved() {
        return (this.mCVWStatus & 2048) != 0;
    }

    public boolean isTaskLeashRemoved() {
        return (this.mCVWStatus & 8192) != 0;
    }

    public void taskResized() {
        addCVWStatus(256);
    }

    public void setTaskResizeWaiting() {
        addCVWStatus(128);
    }

    public void checkOverlaysStatus() {
        String str = TAG;
        MiuiCvwGestureController.Slog.d(str, "check overlays status :" + this.mCVWStatus);
        if (isCovering()) {
            if (!isOverlaysRemoving() || !isOverlaysRemoved()) {
                MiuiCvwGestureController.Slog.e(str, "Overlays is showing abnormally and needs to be removed！");
                this.mCoverLayer.requestRemoveOverlays();
                return;
            }
            MiuiCvwGestureController.Slog.e(str, "Overlays is showing abnormally.");
        }
    }

    public static String statusToString(int status) {
        switch (status) {
            case 0:
                return "CVW_STATUS_NULL";
            case 1:
                return "CVW_OVERLAYS_CREATEING";
            case 2:
                return "CVW_OVERLAYS_CREATED";
            case 4:
                return "CVW_OVERLAYS_COMING";
            case 8:
                return "CVW_OVERLAYS_SHOWING";
            case 16:
                return "CVW_OVERLAYS_SHOW_FINISH";
            case 32:
                return "CVW_OVERLAYS_HIDING";
            case 64:
                return "CVW_OVERLAYS_HIDE_FINISH";
            case 128:
                return "CVW_TASK_RESIZE_WAITING";
            case 256:
                return "CVW_TASK_RESIZED";
            case 512:
                return "CVW_TASK_ANIMATING";
            case 1024:
                return "CVW_OVERLAYS_REMOVING";
            case 2048:
                return "CVW_OVERLAYS_REMOVE_FINISH";
            case 4096:
                return "CVW_TASK_LEASH_CREATED";
            case 8192:
                return "CVW_TASK_LEASH_REMOVED";
            default:
                return "unknown status (" + status + ")";
        }
    }

    public void dump(PrintWriter pw, String prefix) {
        String innerPrefix = prefix + "  ";
        pw.println("MiuiCvwLayerSupervisor:");
        pw.print(innerPrefix);
        pw.println("isTaskAnimating :" + isCVWTaskAnimating());
        pw.print(innerPrefix);
        pw.println("isCovering:" + isCovering());
        pw.print(innerPrefix);
        pw.println("isRemoving:" + isOverlaysRemoving());
        if (!this.mWindowLocks.isEmpty()) {
            pw.print(innerPrefix);
            pw.println("mWindowLocks size :" + this.mWindowLocks.size());
        }
        if (this.mAnimatingStack != null) {
            pw.print(innerPrefix);
            pw.println("mAnimatingStack:" + this.mAnimatingStack);
        }
    }
}
