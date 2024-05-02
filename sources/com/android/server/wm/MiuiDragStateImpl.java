package com.android.server.wm;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.Slog;
import android.view.SurfaceControl;
import com.miui.base.MiuiStubRegistry;
import com.xiaomi.mirror.service.MirrorService;
/* loaded from: classes.dex */
public class MiuiDragStateImpl implements DragStateStub {
    private Rect mFreeformDragBound;
    private boolean mIsDropSuccessAnimEnabled = false;
    private int mPendingDragEndedHeight;
    private int mPendingDragEndedLocX;
    private int mPendingDragEndedLocY;
    private int mPendingDragEndedWidth;

    /* loaded from: classes.dex */
    public final class Provider implements MiuiStubRegistry.ImplProvider<MiuiDragStateImpl> {

        /* compiled from: MiuiDragStateImpl$Provider.java */
        /* loaded from: classes.dex */
        public static final class SINGLETON {
            public static final MiuiDragStateImpl INSTANCE = new MiuiDragStateImpl();
        }

        public MiuiDragStateImpl provideSingleton() {
            return SINGLETON.INSTANCE;
        }

        public MiuiDragStateImpl provideNewInstance() {
            return new MiuiDragStateImpl();
        }
    }

    public boolean isDropSuccessAnimEnabled() {
        return this.mIsDropSuccessAnimEnabled;
    }

    public void setIsDropSuccessAnimEnabled(boolean isDropSuccess) {
        Slog.i("WindowManager", "setIsDropSuccessAnimEnabled isDropSuccess=" + isDropSuccess);
        this.mIsDropSuccessAnimEnabled = isDropSuccess;
    }

    public void setPendingDragEndedLoc(int x, int y, int width, int height) {
        this.mPendingDragEndedLocX = x;
        this.mPendingDragEndedLocY = y;
        this.mPendingDragEndedWidth = width;
        this.mPendingDragEndedHeight = height;
    }

    public void setFreeformDragBound(Rect dropBound) {
        this.mFreeformDragBound = new Rect(dropBound);
    }

    /* loaded from: classes.dex */
    private class MiuiMultiWindowAnimationListener implements ValueAnimator.AnimatorUpdateListener, Animator.AnimatorListener {
        private DragState mDragState;

        private MiuiMultiWindowAnimationListener(DragState dragState) {
            MiuiDragStateImpl.this = r1;
            this.mDragState = dragState;
        }

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationCancel(Animator param1Animator) {
        }

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationEnd(Animator param1Animator) {
            DragState dragState = this.mDragState;
            dragState.mAnimationCompleted = true;
            dragState.mDragDropController.sendHandlerMessage(2, (Object) null);
        }

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationRepeat(Animator animator) {
        }

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationStart(Animator animator) {
        }

        @Override // android.animation.ValueAnimator.AnimatorUpdateListener
        public void onAnimationUpdate(ValueAnimator animator) {
            SurfaceControl.Transaction transaction = new SurfaceControl.Transaction();
            try {
                transaction.setPosition(this.mDragState.mSurfaceControl, ((Float) animator.getAnimatedValue("x")).floatValue(), ((Float) animator.getAnimatedValue("y")).floatValue());
                transaction.setAlpha(this.mDragState.mSurfaceControl, ((Float) animator.getAnimatedValue("alpha")).floatValue());
                transaction.setMatrix(this.mDragState.mSurfaceControl, ((Float) animator.getAnimatedValue("scale")).floatValue(), MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA, ((Float) animator.getAnimatedValue("scale")).floatValue());
                transaction.apply();
            } catch (Exception e) {
            }
        }
    }

    public Animator createDragEndedAnimationLocked(DragState dragState, Point displaySize) {
        Slog.d("WindowManager", "createDragEndedAnimationLocked, mPendingDragEndedLocX = " + this.mPendingDragEndedLocX + ", mPendingDragEndedLocY = " + this.mPendingDragEndedLocY);
        int surfaceWidth = 0;
        if (dragState.mSurfaceControl == null) {
            Slog.w("WindowManager", "createDragEndedAnimationLocked surface is null!");
        } else {
            Slog.d("WindowManager", "createDragEndedAnimationLocked surface = " + dragState.mSurfaceControl + ", width = " + dragState.mSurfaceControl.getWidth() + ", height = " + dragState.mSurfaceControl.getHeight());
            surfaceWidth = dragState.mSurfaceControl.getWidth();
        }
        float fromTranslationX = dragState.mCurrentX - dragState.mThumbOffsetX;
        float toTranslationX = (this.mPendingDragEndedLocX + (this.mPendingDragEndedWidth / 2.0f)) - (surfaceWidth / 2.0f);
        float fromTranslationY = dragState.mCurrentY - dragState.mThumbOffsetY;
        float toTranslationY = this.mPendingDragEndedLocY;
        if (displaySize.x == this.mPendingDragEndedWidth && displaySize.y == this.mPendingDragEndedHeight) {
            toTranslationY = this.mFreeformDragBound.top;
        }
        ValueAnimator translateAnimator = ValueAnimator.ofPropertyValuesHolder(PropertyValuesHolder.ofFloat("x", fromTranslationX, toTranslationX), PropertyValuesHolder.ofFloat("y", fromTranslationY, toTranslationY), PropertyValuesHolder.ofFloat("scale", 1.0f, 1.0f), PropertyValuesHolder.ofFloat("alpha", 1.0f, 1.0f));
        MiuiMultiWindowAnimationListener listener = new MiuiMultiWindowAnimationListener(dragState);
        translateAnimator.setDuration(350L);
        translateAnimator.setInterpolator(new PhysicBasedInterpolator(0.9f, 0.857f));
        translateAnimator.addUpdateListener(listener);
        ValueAnimator disappearAnimator = ValueAnimator.ofPropertyValuesHolder(PropertyValuesHolder.ofFloat("x", toTranslationX, toTranslationX), PropertyValuesHolder.ofFloat("y", toTranslationY, toTranslationY), PropertyValuesHolder.ofFloat("scale", 1.0f, 1.0f), PropertyValuesHolder.ofFloat("alpha", 1.0f, 1.0f));
        disappearAnimator.setDuration(250L);
        disappearAnimator.setInterpolator(new PhysicBasedInterpolator(0.9f, 0.857f));
        disappearAnimator.addUpdateListener(listener);
        disappearAnimator.addListener(listener);
        final AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playSequentially(translateAnimator, disappearAnimator);
        MiuiMultiWindowSwitchManager.getInstance(dragState.mService.mAtmService).getDragEndAnimHanlder().post(new Runnable() { // from class: com.android.server.wm.MiuiDragStateImpl.1
            @Override // java.lang.Runnable
            public void run() {
                animatorSet.start();
            }
        });
        Slog.d("WindowManager", "createDragEndedAnimationLocked: animation start!");
        return animatorSet;
    }

    public boolean needFinishAnimator() {
        return MirrorService.get().isNeedFinishAnimator();
    }
}
