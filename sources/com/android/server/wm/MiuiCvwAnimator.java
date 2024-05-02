package com.android.server.wm;

import android.graphics.HardwareRenderer;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.view.SurfaceControl;
import android.view.ViewRootImpl;
import com.android.server.wm.MiuiCvwAnimator;
import com.android.server.wm.MiuiCvwGestureController;
import com.android.server.wm.MiuiFreeFormDynamicAnimation;
import com.miui.internal.dynamicanimation.animation.FloatPropertyCompat;
import com.miui.internal.dynamicanimation.animation.FloatValueHolder;
/* loaded from: classes.dex */
public class MiuiCvwAnimator {
    static final int ANIMATION_BACK_TO_FULLSCREEN = 5;
    static final int ANIMATION_BACK_TO_MINI = 18;
    static final int ANIMATION_DEFAULT_SET_PARAMS = 6;
    static final int ANIMATION_FREEFORM_TO_FULLSCREEN = 12;
    static final int ANIMATION_FREEFORM_TO_MINI_LFET = 7;
    static final int ANIMATION_FREEFORM_TO_MINI_RIGHT = 8;
    static final int ANIMATION_FULLSCREEN_TO_FREEFORM = 2;
    static final int ANIMATION_FULLSCREEN_TO_FREEFORM_LEFT = 3;
    static final int ANIMATION_FULLSCREEN_TO_FREEFORM_RIGHT = 4;
    static final int ANIMATION_FULLSCREEN_TO_MINI_LEFT = 9;
    static final int ANIMATION_FULLSCREEN_TO_MINI_RIGHT = 10;
    static final int ANIMATION_MINI_TO_FREEFORM_LFET = 15;
    static final int ANIMATION_MINI_TO_FREEFORM_RIGHT = 16;
    static final int ANIMATION_MINI_TO_FULLSCREEN = 17;
    static final int ANIMATION_RESIZE_BACK_LEFT = 13;
    static final int ANIMATION_RESIZE_BACK_RIGHT = 14;
    static final int ANIMATION_RESIZE_LEFT = 0;
    static final int ANIMATION_RESIZE_RIGHT = 1;
    static final int ANIMATION_SCALE_HOME = 11;
    static final int ANIMATION_UNDEFINED = -1;
    public static final int FLAG_ALL = -1;
    public static final int FLAG_ALPHA = 1;
    public static final int FLAG_CORNER_RADIUS = 16;
    public static final int FLAG_MATRIX = 2;
    public static final int FLAG_VISIBILITY = 64;
    public static final int FLAG_WINDOW_CROP = 4;
    public static final int FLAG_WINDOW_POSITION = 8;
    static final String FLOAT_PROPERTY_ALPHA = "ALPHA";
    static final String FLOAT_PROPERTY_SCALE = "SCALE";
    static final String FLOAT_PROPERTY_SCALE_X = "SCALE_X";
    static final String FLOAT_PROPERTY_SCALE_Y = "SCALE_Y";
    static final String FLOAT_PROPERTY_TRANSLATE_BOTTOM = "TRANSLATE_BOTTOM";
    static final String FLOAT_PROPERTY_TRANSLATE_RIGHT = "TRANSLATE_RIGHT";
    static final String FLOAT_PROPERTY_TRANSLATE_X = "TRANSLATE_X";
    static final String FLOAT_PROPERTY_TRANSLATE_Y = "TRANSLATE_Y";
    static final float SET_PARAMS_BACK_TO_FULLSCREEN_DAMPING = 1.0f;
    static final float SET_PARAMS_BACK_TO_FULLSCREEN_STIFFNESS = 500.0f;
    static final float SET_PARAMS_CANNOT_RESIZEABLE_MOVE_DAMPING = 0.65f;
    static final float SET_PARAMS_CANNOT_RESIZEABLE_MOVE_STIFFNESS = 157.913f;
    static final float SET_PARAMS_DOWN_FREEFORM_SCALE_STIFFNESS = 400.0f;
    static final float SET_PARAMS_ENTER_FREEFORM_DAMPING = 1.0f;
    static final float SET_PARAMS_ENTER_FREEFORM_STIFFNESS = 300.0f;
    static final float SET_PARAMS_ENTER_MINI_DAMPING = 0.75f;
    static final float SET_PARAMS_ENTER_MINI_MOVE_DAMPING = 1.0f;
    static final float SET_PARAMS_ENTER_MINI_MOVE_STIFFNESS = 700.0f;
    static final float SET_PARAMS_ENTER_MINI_STIFFNESS = 400.0f;
    static final float SET_PARAMS_MOVE_DAMPING = 1.0f;
    static final float SET_PARAMS_MOVE_STIFFNESS = 1000.0f;
    static final int SPRINGANIMATION_ALPHA = 1;
    static final int SPRINGANIMATION_SCALE = 4;
    static final int SPRINGANIMATION_SCALE_X = 5;
    static final int SPRINGANIMATION_SCALE_Y = 6;
    static final int SPRINGANIMATION_TRANSLATE_BOTTOM = 8;
    static final int SPRINGANIMATION_TRANSLATE_RIGHT = 7;
    static final int SPRINGANIMATION_TRANSLATE_X = 2;
    static final int SPRINGANIMATION_TRANSLATE_Y = 3;
    private static final String TAG = MiuiCvwAnimator.class.getSimpleName();
    private final MiuiCvwGestureController mController;
    private final MiuiCvwGestureHandlerImpl mGestureHandlerImpl;
    private ViewRootImpl mTargetViewRootImpl;
    private final SurfaceControl.Transaction mGestureTransaction = new SurfaceControl.Transaction();
    private final SurfaceControl.Transaction mShowSyncTransaction = new SurfaceControl.Transaction();
    private final SurfaceControl.Transaction mHideSyncTransaction = new SurfaceControl.Transaction();

    public MiuiCvwAnimator(MiuiCvwGestureController controller, MiuiCvwGestureHandlerImpl handler) {
        this.mController = controller;
        this.mGestureHandlerImpl = handler;
    }

    void setCornerRadiusInTransaction(MiuiFreeFormActivityStack stack, float radius) {
        synchronized (this) {
            if (stack != null) {
                if (stack.mTaskAnimationAdapter != null) {
                    synchronized (stack.mTaskAnimationAdapter.mLock) {
                        SurfaceControl leash = stack.getFreeFormConrolSurface();
                        if (leash != null && leash.isValid()) {
                            this.mGestureTransaction.setCornerRadius(leash, radius);
                        }
                    }
                }
            }
        }
    }

    public void setMatrixInTransaction(MiuiFreeFormActivityStack stack, float dsdx, float dtdx, float dtdy, float dsdy) {
        synchronized (this) {
            if (stack != null) {
                if (stack.mTaskAnimationAdapter != null) {
                    synchronized (stack.mTaskAnimationAdapter.mLock) {
                        SurfaceControl leash = stack.getFreeFormConrolSurface();
                        if (leash != null && leash.isValid()) {
                            this.mGestureTransaction.setMatrix(leash, dsdx, dtdx, dtdy, dsdy);
                        }
                    }
                }
            }
        }
    }

    void setMatrixInTransaction(MiuiFreeFormActivityStack stack, Matrix matrix, float[] float9) {
        synchronized (this) {
            if (stack != null) {
                if (stack.mTaskAnimationAdapter != null) {
                    synchronized (stack.mTaskAnimationAdapter.mLock) {
                        SurfaceControl leash = stack.getFreeFormConrolSurface();
                        if (leash != null && leash.isValid()) {
                            this.mGestureTransaction.setMatrix(leash, matrix, float9);
                        }
                    }
                }
            }
        }
    }

    public void setPositionInTransaction(MiuiFreeFormActivityStack stack, float x, float y) {
        synchronized (this) {
            if (stack != null) {
                if (stack.mTaskAnimationAdapter != null) {
                    synchronized (stack.mTaskAnimationAdapter.mLock) {
                        SurfaceControl leash = stack.getFreeFormConrolSurface();
                        if (leash != null && leash.isValid()) {
                            this.mGestureTransaction.setPosition(leash, x, y);
                        }
                    }
                }
            }
        }
    }

    void setWindowCropInTransaction(MiuiFreeFormActivityStack stack, Rect clipRect) {
        synchronized (this) {
            if (stack != null) {
                if (stack.mTaskAnimationAdapter != null) {
                    synchronized (stack.mTaskAnimationAdapter.mLock) {
                        SurfaceControl leash = stack.getFreeFormConrolSurface();
                        if (leash != null && leash.isValid()) {
                            this.mGestureTransaction.setWindowCrop(leash, clipRect);
                        }
                    }
                }
            }
        }
    }

    public void setTaskAlphaInTransaction(MiuiFreeFormActivityStack stack, float alpha) {
        synchronized (this) {
            if (stack == null) {
                return;
            }
            SurfaceControl surface = stack.mTask.getSurfaceControl();
            if (surface != null && surface.isValid()) {
                if (alpha == MiuiFreeformPinManagerService.EDGE_AREA) {
                    this.mHideSyncTransaction.hide(surface);
                    this.mHideSyncTransaction.setAlpha(surface, alpha);
                } else {
                    this.mShowSyncTransaction.show(surface);
                    this.mShowSyncTransaction.setAlpha(surface, alpha);
                }
            }
        }
    }

    void setAlphaInTransaction(MiuiFreeFormActivityStack stack, float alpha) {
        synchronized (this) {
            if (stack != null) {
                if (stack.mTaskAnimationAdapter != null) {
                    synchronized (stack.mTaskAnimationAdapter.mLock) {
                        SurfaceControl leash = stack.getFreeFormConrolSurface();
                        if (leash != null && leash.isValid()) {
                            this.mGestureTransaction.setAlpha(leash, alpha);
                        }
                    }
                }
            }
        }
    }

    public void removeAnimationControlLeash(MiuiFreeFormActivityStack stack) {
        synchronized (this.mController.mWmService.mAtmService.mGlobalLock) {
            synchronized (this) {
                if (stack != null) {
                    stack.removeAnimationControlLeash();
                }
            }
        }
    }

    void setAlphaInTransaction(SurfaceControl surface, float alpha) {
        synchronized (this) {
            if (surface != null) {
                if (surface.isValid()) {
                    this.mGestureTransaction.setAlpha(surface, alpha);
                }
            }
        }
    }

    void setWindowCropInTransaction(SurfaceControl surface, Rect crop) {
        synchronized (this) {
            if (surface != null) {
                if (surface.isValid()) {
                    this.mGestureTransaction.setWindowCrop(surface, crop);
                }
            }
        }
    }

    void setCornerRadiusInTransaction(SurfaceControl surface, float radius) {
        synchronized (this) {
            if (surface != null) {
                if (surface.isValid()) {
                    this.mGestureTransaction.setCornerRadius(surface, radius);
                }
            }
        }
    }

    void setMatrixInTransaction(SurfaceControl surface, float dsdx, float dtdx, float dtdy, float dsdy) {
        synchronized (this) {
            if (surface != null) {
                if (surface.isValid()) {
                    this.mGestureTransaction.setMatrix(surface, dsdx, dtdx, dtdy, dsdy);
                }
            }
        }
    }

    void setPositionInTransaction(SurfaceControl surface, float x, float y) {
        synchronized (this) {
            if (surface != null) {
                if (surface.isValid()) {
                    this.mGestureTransaction.setPosition(surface, x, y);
                }
            }
        }
    }

    SurfaceControl.Transaction getPenddingTransaction() {
        return this.mGestureTransaction;
    }

    public SurfaceControl.Transaction getShowSyncTransaction() {
        return this.mShowSyncTransaction;
    }

    public SurfaceControl.Transaction getHideSyncTransaction() {
        return this.mHideSyncTransaction;
    }

    public void applyTransaction() {
        synchronized (this) {
            this.mGestureTransaction.apply();
        }
    }

    public void setViewRootImpl(ViewRootImpl viewRoot) {
        this.mTargetViewRootImpl = viewRoot;
    }

    public boolean mergeTransactionToView(final SurfaceParams surfaceParams) {
        ViewRootImpl viewRootImpl = this.mTargetViewRootImpl;
        if (viewRootImpl == null) {
            synchronized (this) {
                SurfaceControl.Transaction t = new SurfaceControl.Transaction();
                surfaceParams.applyTo(t);
                t.apply();
            }
            return true;
        }
        viewRootImpl.registerRtFrameCallback(new HardwareRenderer.FrameDrawingCallback() { // from class: com.android.server.wm.MiuiCvwAnimator.1
            public void onFrameDraw(long frame) {
                synchronized (MiuiCvwAnimator.this) {
                    SurfaceControl.Transaction t2 = new SurfaceControl.Transaction();
                    surfaceParams.applyTo(t2);
                    if (MiuiCvwAnimator.this.mTargetViewRootImpl != null) {
                        MiuiCvwAnimator.this.mTargetViewRootImpl.mergeWithNextTransaction(t2, frame);
                    } else {
                        t2.apply();
                    }
                }
            }
        });
        return true;
    }

    public boolean mergeVisibilityTransaction(SurfaceControl surface, ViewRootImpl viewRoot, SurfaceControl.Transaction t, long frame) {
        synchronized (this) {
            if (surface != null) {
                if (surface.isValid()) {
                    SurfaceControl parent = viewRoot.getSurfaceControl();
                    if (parent != null && parent.isValid()) {
                        viewRoot.mergeWithNextTransaction(t, frame);
                        return true;
                    }
                    t.apply();
                    return false;
                }
            }
            return false;
        }
    }

    /* loaded from: classes.dex */
    public static class SurfaceParams {
        public final float alpha;
        public final float cornerRadius;
        private final int flags;
        private final float[] mTmpValues;
        public final Matrix matrix;
        public final MiuiFreeFormActivityStack stack;
        public final boolean visible;
        public final Rect windowCrop;
        int x;
        int y;

        /* loaded from: classes.dex */
        public static class Builder {
            float alpha;
            float cornerRadius;
            int flags;
            Matrix matrix;
            final MiuiFreeFormActivityStack stack;
            boolean visible;
            Rect windowCrop;
            int x;
            int y;

            public Builder(MiuiFreeFormActivityStack stack) {
                this.stack = stack;
            }

            public Builder withAlpha(float alpha) {
                this.alpha = alpha;
                this.flags |= 1;
                return this;
            }

            public Builder withMatrix(float dsdx, float dsdy) {
                Matrix matrix = new Matrix();
                this.matrix = matrix;
                matrix.postScale(dsdx, dsdy);
                this.flags |= 2;
                return this;
            }

            public Builder withPosition(int x, int y) {
                this.x = x;
                this.y = y;
                this.flags |= 8;
                return this;
            }

            public Builder withWindowCrop(Rect windowCrop) {
                this.windowCrop = new Rect(windowCrop);
                this.flags |= 4;
                return this;
            }

            public Builder withCornerRadius(float radius) {
                this.cornerRadius = radius;
                this.flags |= 16;
                return this;
            }

            public Builder withVisibility(boolean visible) {
                this.visible = visible;
                this.flags |= 64;
                return this;
            }

            public SurfaceParams build() {
                return new SurfaceParams(this.stack, this.flags, this.alpha, this.x, this.y, this.matrix, this.windowCrop, this.cornerRadius, this.visible);
            }
        }

        private SurfaceParams(MiuiFreeFormActivityStack stack, int flags, float alpha, int x, int y, Matrix matrix, Rect windowCrop, float cornerRadius, boolean visible) {
            this.mTmpValues = new float[9];
            this.flags = flags;
            this.stack = stack;
            this.alpha = alpha;
            this.x = x;
            this.y = y;
            this.matrix = matrix;
            this.windowCrop = windowCrop;
            this.cornerRadius = cornerRadius;
            this.visible = visible;
        }

        public void applyTo(SurfaceControl.Transaction t) {
            MiuiFreeFormActivityStack miuiFreeFormActivityStack = this.stack;
            if (miuiFreeFormActivityStack == null || miuiFreeFormActivityStack.mTask == null || this.stack.mTaskAnimationAdapter == null) {
                return;
            }
            SurfaceControl taskSurface = this.stack.mTask.getSurfaceControl();
            if (taskSurface == null || !taskSurface.isValid()) {
                MiuiCvwGestureController.Slog.d(MiuiCvwAnimator.TAG, "taskSurface IS invaild");
                return;
            }
            synchronized (this.stack.mTaskAnimationAdapter.mLock) {
                SurfaceControl leash = this.stack.getFreeFormConrolSurface();
                if (leash != null && leash.isValid()) {
                    if ((this.flags & 2) != 0) {
                        t.setMatrix(leash, this.matrix, this.mTmpValues);
                    }
                    if ((this.flags & 8) != 0) {
                        t.setPosition(leash, this.x, this.y);
                    }
                    if ((this.flags & 4) != 0) {
                        t.setWindowCrop(leash, this.windowCrop);
                    }
                    if ((this.flags & 1) != 0) {
                        t.setAlpha(leash, this.alpha);
                    }
                    if ((this.flags & 16) != 0) {
                        t.setCornerRadius(leash, this.cornerRadius);
                    }
                    if ((this.flags & 64) != 0) {
                        if (this.visible) {
                            t.show(leash);
                        } else {
                            t.hide(leash);
                        }
                    }
                }
            }
        }
    }

    public void createSpringAnimation(MiuiFreeFormActivityStack stack, int springAnimalType, float startValue, float endValue, AnimalLock animalLock, SpringAnimatorListener<MiuiFreeFormActivityStack> listener) {
        switch (springAnimalType) {
            case 1:
                if (animalLock.mAlphaAnimation == null) {
                    MiuiFreeFormSpringAnimation alphaSpringAnimation = createSpringAnimation(stack, startValue, endValue, animalLock.mStiffness, animalLock.mDamping, MiuiFreeformPinManagerService.EDGE_AREA, springAnimalType, listener);
                    animalLock.mAlphaAnimation = alphaSpringAnimation;
                }
                MiuiFreeFormSpringAnimation alphaSpringAnimation2 = animalLock.mAlphaAnimation;
                alphaSpringAnimation2.getSpring().setDampingRatio(animalLock.mDamping);
                animalLock.mAlphaAnimation.getSpring().setStiffness(animalLock.mStiffness);
                animalLock.mAlphaAnimation.setStartValue(animalLock.mAlphaAnimation.getCurrentValue());
                animalLock.mAlphaAnimation.animateToFinalPosition(endValue);
                return;
            case 2:
                if (animalLock.mTranslateXAnimation == null) {
                    MiuiFreeFormSpringAnimation tXSpringAnimation = createSpringAnimation(stack, startValue, endValue, animalLock.mStiffness, animalLock.mDamping, MiuiFreeformPinManagerService.EDGE_AREA, springAnimalType, listener);
                    animalLock.mTranslateXAnimation = tXSpringAnimation;
                }
                MiuiFreeFormSpringAnimation tXSpringAnimation2 = animalLock.mTranslateXAnimation;
                tXSpringAnimation2.getSpring().setDampingRatio(animalLock.mDamping);
                animalLock.mTranslateXAnimation.getSpring().setStiffness(animalLock.mStiffness);
                animalLock.mTranslateXAnimation.setStartValue(animalLock.mTranslateXAnimation.getCurrentValue());
                animalLock.mTranslateXAnimation.animateToFinalPosition(endValue);
                return;
            case 3:
                if (animalLock.mTranslateYAnimation == null) {
                    MiuiFreeFormSpringAnimation tYSpringAnimation = createSpringAnimation(stack, startValue, endValue, animalLock.mStiffness, animalLock.mDamping, MiuiFreeformPinManagerService.EDGE_AREA, springAnimalType, listener);
                    animalLock.mTranslateYAnimation = tYSpringAnimation;
                }
                MiuiFreeFormSpringAnimation tYSpringAnimation2 = animalLock.mTranslateYAnimation;
                tYSpringAnimation2.getSpring().setDampingRatio(animalLock.mDamping);
                animalLock.mTranslateYAnimation.getSpring().setStiffness(animalLock.mStiffness);
                animalLock.mTranslateYAnimation.setStartValue(animalLock.mTranslateYAnimation.getCurrentValue());
                animalLock.mTranslateYAnimation.animateToFinalPosition(endValue);
                return;
            case 4:
                if (animalLock.mScaleAnimation == null) {
                    MiuiFreeFormSpringAnimation scaleSpringAnimation = createSpringAnimation(stack, startValue, endValue, animalLock.mStiffness, animalLock.mDamping, MiuiFreeformPinManagerService.EDGE_AREA, springAnimalType, listener);
                    animalLock.mScaleAnimation = scaleSpringAnimation;
                }
                MiuiFreeFormSpringAnimation scaleSpringAnimation2 = animalLock.mScaleAnimation;
                scaleSpringAnimation2.getSpring().setDampingRatio(animalLock.mDamping);
                animalLock.mScaleAnimation.getSpring().setStiffness(animalLock.mStiffness);
                animalLock.mScaleAnimation.setStartValue(animalLock.mScaleAnimation.getCurrentValue());
                animalLock.mScaleAnimation.animateToFinalPosition(endValue);
                return;
            case 5:
                if (animalLock.mScaleXAnimation == null) {
                    MiuiFreeFormSpringAnimation scaleXSpringAnimation = createSpringAnimation(stack, startValue, endValue, animalLock.mStiffness, animalLock.mDamping, MiuiFreeformPinManagerService.EDGE_AREA, springAnimalType, listener);
                    animalLock.mScaleXAnimation = scaleXSpringAnimation;
                }
                MiuiFreeFormSpringAnimation scaleXSpringAnimation2 = animalLock.mScaleXAnimation;
                scaleXSpringAnimation2.getSpring().setDampingRatio(animalLock.mDamping);
                animalLock.mScaleXAnimation.getSpring().setStiffness(animalLock.mStiffness);
                animalLock.mScaleXAnimation.setStartValue(animalLock.mScaleXAnimation.getCurrentValue());
                animalLock.mScaleXAnimation.animateToFinalPosition(endValue);
                return;
            case 6:
                if (animalLock.mScaleYAnimation == null) {
                    MiuiFreeFormSpringAnimation scaleYSpringAnimation = createSpringAnimation(stack, startValue, endValue, animalLock.mStiffness, animalLock.mDamping, MiuiFreeformPinManagerService.EDGE_AREA, springAnimalType, listener);
                    animalLock.mScaleYAnimation = scaleYSpringAnimation;
                }
                MiuiFreeFormSpringAnimation scaleYSpringAnimation2 = animalLock.mScaleYAnimation;
                scaleYSpringAnimation2.getSpring().setDampingRatio(animalLock.mDamping);
                animalLock.mScaleYAnimation.getSpring().setStiffness(animalLock.mStiffness);
                animalLock.mScaleYAnimation.setStartValue(animalLock.mScaleYAnimation.getCurrentValue());
                animalLock.mScaleYAnimation.animateToFinalPosition(endValue);
                return;
            case 7:
                if (animalLock.mTranslateRightAnimation == null) {
                    MiuiFreeFormSpringAnimation tRSpringAnimation = createSpringAnimation(stack, startValue, endValue, animalLock.mStiffness, animalLock.mDamping, MiuiFreeformPinManagerService.EDGE_AREA, springAnimalType, listener);
                    animalLock.mTranslateRightAnimation = tRSpringAnimation;
                }
                MiuiFreeFormSpringAnimation tRSpringAnimation2 = animalLock.mTranslateRightAnimation;
                tRSpringAnimation2.getSpring().setDampingRatio(animalLock.mDamping);
                animalLock.mTranslateRightAnimation.getSpring().setStiffness(animalLock.mStiffness);
                animalLock.mTranslateRightAnimation.setStartValue(animalLock.mTranslateRightAnimation.getCurrentValue());
                animalLock.mTranslateRightAnimation.animateToFinalPosition(endValue);
                return;
            case 8:
                if (animalLock.mTranslateBottomAnimation == null) {
                    MiuiFreeFormSpringAnimation tBSpringAnimation = createSpringAnimation(stack, startValue, endValue, animalLock.mStiffness, animalLock.mDamping, MiuiFreeformPinManagerService.EDGE_AREA, springAnimalType, listener);
                    animalLock.mTranslateBottomAnimation = tBSpringAnimation;
                }
                MiuiFreeFormSpringAnimation tBSpringAnimation2 = animalLock.mTranslateBottomAnimation;
                tBSpringAnimation2.getSpring().setDampingRatio(animalLock.mDamping);
                animalLock.mTranslateBottomAnimation.getSpring().setStiffness(animalLock.mStiffness);
                animalLock.mTranslateBottomAnimation.setStartValue(animalLock.mTranslateBottomAnimation.getCurrentValue());
                animalLock.mTranslateBottomAnimation.animateToFinalPosition(endValue);
                return;
            default:
                return;
        }
    }

    public void spring(AnimalLock animalLock, float stiffness, float damping) {
        if (animalLock != null) {
            animalLock.mStiffness = stiffness;
            animalLock.mDamping = damping;
        }
    }

    public void addTaskEndListener(int animationType, AnimalLock animalLock, SpringAnimatorListener<MiuiFreeFormActivityStack> springAnimatorListener) {
        if (animalLock.mScaleAnimation != null) {
            animalLock.mScaleAnimation.addEndListener(createAnimationEndListener("SCALE", springAnimatorListener));
        }
        if (animalLock.mScaleXAnimation != null) {
            animalLock.mScaleXAnimation.addEndListener(createAnimationEndListener("SCALE_X", springAnimatorListener));
        }
        if (animalLock.mScaleYAnimation != null) {
            animalLock.mScaleYAnimation.addEndListener(createAnimationEndListener("SCALE_Y", springAnimatorListener));
        }
        if (animalLock.mTranslateXAnimation != null) {
            animalLock.mTranslateXAnimation.addEndListener(createAnimationEndListener("TRANSLATE_X", springAnimatorListener));
        }
        if (animalLock.mTranslateYAnimation != null) {
            animalLock.mTranslateYAnimation.addEndListener(createAnimationEndListener("TRANSLATE_Y", springAnimatorListener));
        }
        if (animalLock.mTranslateRightAnimation != null) {
            animalLock.mTranslateRightAnimation.addEndListener(createAnimationEndListener(FLOAT_PROPERTY_TRANSLATE_RIGHT, springAnimatorListener));
        }
        if (animalLock.mTranslateBottomAnimation != null) {
            animalLock.mTranslateBottomAnimation.addEndListener(createAnimationEndListener(FLOAT_PROPERTY_TRANSLATE_BOTTOM, springAnimatorListener));
        }
        if (animalLock.mAlphaAnimation != null) {
            animalLock.mAlphaAnimation.addEndListener(createAnimationEndListener("ALPHA", springAnimatorListener));
        }
        animalLock.start(animationType);
    }

    private MiuiFreeFormSpringAnimation createSpringAnimation(MiuiFreeFormActivityStack stack, float startValue, float finalPosition, float stiffness, float damping, float startVelocity, int animalType, SpringAnimatorListener<MiuiFreeFormActivityStack> springAnimatorListener) {
        MiuiFreeFormSpringAnimation springAnimation = null;
        switch (animalType) {
            case 1:
                springAnimation = new MiuiFreeFormSpringAnimation(stack, createTaskProperty(animalType, "ALPHA", springAnimatorListener), finalPosition);
                break;
            case 2:
                springAnimation = new MiuiFreeFormSpringAnimation(stack, createTaskProperty(animalType, "TRANSLATE_X", springAnimatorListener), finalPosition);
                break;
            case 3:
                springAnimation = new MiuiFreeFormSpringAnimation(stack, createTaskProperty(animalType, "TRANSLATE_Y", springAnimatorListener), finalPosition);
                break;
            case 4:
                springAnimation = new MiuiFreeFormSpringAnimation(stack, createTaskProperty(animalType, "SCALE", springAnimatorListener), finalPosition);
                break;
            case 5:
                springAnimation = new MiuiFreeFormSpringAnimation(stack, createTaskProperty(animalType, "SCALE_X", springAnimatorListener), finalPosition);
                break;
            case 6:
                springAnimation = new MiuiFreeFormSpringAnimation(stack, createTaskProperty(animalType, "SCALE_Y", springAnimatorListener), finalPosition);
                break;
            case 7:
                springAnimation = new MiuiFreeFormSpringAnimation(stack, createTaskProperty(animalType, FLOAT_PROPERTY_TRANSLATE_RIGHT, springAnimatorListener), finalPosition);
                break;
            case 8:
                springAnimation = new MiuiFreeFormSpringAnimation(stack, createTaskProperty(animalType, FLOAT_PROPERTY_TRANSLATE_BOTTOM, springAnimatorListener), finalPosition);
                break;
        }
        springAnimation.getSpring().setStiffness(stiffness);
        springAnimation.getSpring().setDampingRatio(damping);
        if (animalType == 4 || animalType == 5 || animalType == 6) {
            springAnimation.setMinimumVisibleChange(0.002f);
        } else {
            springAnimation.setMinimumVisibleChange(1.0f);
        }
        springAnimation.setStartValue(startValue);
        springAnimation.setStartVelocity(startVelocity);
        return springAnimation;
    }

    private FloatPropertyCompat createTaskProperty(final int springAnimalType, String propertyType, final SpringAnimatorListener<MiuiFreeFormActivityStack> springAnimatorListener) {
        MiuiFreeFormDynamicAnimation.ActivityStackProperty floatPropertyCompat = new MiuiFreeFormDynamicAnimation.ActivityStackProperty(propertyType) { // from class: com.android.server.wm.MiuiCvwAnimator.2
            public float getValue(MiuiFreeFormActivityStack stack) {
                return springAnimatorListener.getStartValue(springAnimalType);
            }

            public void setValue(MiuiFreeFormActivityStack stack, float value) {
                springAnimatorListener.updateValue(stack, value, springAnimalType);
            }
        };
        return floatPropertyCompat;
    }

    MiuiFreeFormSpringAnimation createCoverSpringAnimation(float startValue, float finalPosition, float stiffness, float damping, float startVelocity) {
        MiuiFreeFormSpringAnimation springAnimation = new MiuiFreeFormSpringAnimation(new FloatValueHolder());
        MiuiFreeFormSpringForce springForce = new MiuiFreeFormSpringForce();
        springForce.setStiffness(stiffness);
        springForce.setDampingRatio(damping);
        springForce.setFinalPosition(finalPosition);
        springAnimation.setSpring(springForce);
        springAnimation.setMinimumVisibleChange(0.002f);
        springAnimation.setStartValue(startValue);
        springAnimation.setStartVelocity(startVelocity);
        return springAnimation;
    }

    public void startHomeLeashAnimation(MiuiFreeFormActivityStack stack, AnimalLock animalLock, SpringAnimatorListener<MiuiFreeFormActivityStack> listener) {
        MiuiFreeFormSpringAnimation scaleSpringAnimation = createSpringAnimation(stack, 0.9f, 1.0f, 438.6491f, 0.9f, MiuiFreeformPinManagerService.EDGE_AREA, 4, listener);
        scaleSpringAnimation.addEndListener(createAnimationEndListener("SCALE", listener));
        animalLock.mScaleAnimation = scaleSpringAnimation;
        animalLock.start(11);
    }

    private <Target> MiuiFreeFormDynamicAnimation.OnAnimationEndListener createAnimationEndListener(final String propertyType, final SpringAnimatorListener<Target> springAnimatorListener) {
        return new MiuiFreeFormDynamicAnimation.OnAnimationEndListener() { // from class: com.android.server.wm.MiuiCvwAnimator$$ExternalSyntheticLambda0
            @Override // com.android.server.wm.MiuiFreeFormDynamicAnimation.OnAnimationEndListener
            public final void onAnimationEnd(MiuiFreeFormDynamicAnimation miuiFreeFormDynamicAnimation, boolean z, float f, float f2) {
                MiuiCvwAnimator.SpringAnimatorListener.this.onAnimationEnd(miuiFreeFormDynamicAnimation, propertyType);
            }
        };
    }

    /* loaded from: classes.dex */
    public static final class AnimalLock {
        MiuiFreeFormSpringAnimation mAlphaAnimation;
        MiuiFreeFormSpringAnimation mScaleAnimation;
        MiuiFreeFormSpringAnimation mScaleXAnimation;
        MiuiFreeFormSpringAnimation mScaleYAnimation;
        MiuiFreeFormActivityStack mStack;
        MiuiFreeFormSpringAnimation mTranslateBottomAnimation;
        MiuiFreeFormSpringAnimation mTranslateRightAnimation;
        MiuiFreeFormSpringAnimation mTranslateXAnimation;
        MiuiFreeFormSpringAnimation mTranslateYAnimation;
        int mCurrentAnimation = -1;
        float mDamping = 1.0f;
        float mStiffness = MiuiCvwAnimator.SET_PARAMS_MOVE_STIFFNESS;
        boolean mScaleEnd = true;
        boolean mScaleXEnd = true;
        boolean mScaleYEnd = true;
        boolean mTranslateYEnd = true;
        boolean mTranslateXEnd = true;
        boolean mTranslateRightEnd = true;
        boolean mTranslateBottomEnd = true;
        boolean mAlphaEnd = true;

        public AnimalLock(MiuiFreeFormActivityStack stack) {
            this.mStack = stack;
        }

        public void resetAnimalState() {
            this.mScaleEnd = true;
            this.mScaleXEnd = true;
            this.mScaleYEnd = true;
            this.mTranslateYEnd = true;
            this.mTranslateXEnd = true;
            this.mTranslateRightEnd = true;
            this.mTranslateBottomEnd = true;
            this.mAlphaEnd = true;
            this.mScaleAnimation = null;
            this.mScaleXAnimation = null;
            this.mScaleYAnimation = null;
            this.mTranslateYAnimation = null;
            this.mTranslateXAnimation = null;
            this.mAlphaAnimation = null;
            this.mStack = null;
            this.mDamping = 1.0f;
            this.mStiffness = MiuiCvwAnimator.SET_PARAMS_MOVE_STIFFNESS;
        }

        public void animationEnd(MiuiFreeFormDynamicAnimation animation) {
            if (animation == this.mScaleAnimation) {
                this.mScaleEnd = true;
            } else if (animation == this.mScaleXAnimation) {
                this.mScaleXEnd = true;
            } else if (animation == this.mScaleYAnimation) {
                this.mScaleYEnd = true;
            } else if (animation == this.mTranslateYAnimation) {
                this.mTranslateYEnd = true;
            } else if (animation == this.mTranslateXAnimation) {
                this.mTranslateXEnd = true;
            } else if (animation == this.mTranslateRightAnimation) {
                this.mTranslateRightEnd = true;
            } else if (animation == this.mTranslateBottomAnimation) {
                this.mTranslateBottomEnd = true;
            } else if (animation == this.mAlphaAnimation) {
                this.mAlphaEnd = true;
            }
        }

        private void skipToEnd() {
            MiuiFreeFormSpringAnimation miuiFreeFormSpringAnimation = this.mScaleAnimation;
            if (miuiFreeFormSpringAnimation != null) {
                miuiFreeFormSpringAnimation.skipToEnd();
            }
            MiuiFreeFormSpringAnimation miuiFreeFormSpringAnimation2 = this.mScaleXAnimation;
            if (miuiFreeFormSpringAnimation2 != null) {
                miuiFreeFormSpringAnimation2.skipToEnd();
            }
            MiuiFreeFormSpringAnimation miuiFreeFormSpringAnimation3 = this.mScaleYAnimation;
            if (miuiFreeFormSpringAnimation3 != null) {
                miuiFreeFormSpringAnimation3.skipToEnd();
            }
            MiuiFreeFormSpringAnimation miuiFreeFormSpringAnimation4 = this.mTranslateYAnimation;
            if (miuiFreeFormSpringAnimation4 != null) {
                miuiFreeFormSpringAnimation4.skipToEnd();
            }
            MiuiFreeFormSpringAnimation miuiFreeFormSpringAnimation5 = this.mTranslateXAnimation;
            if (miuiFreeFormSpringAnimation5 != null) {
                miuiFreeFormSpringAnimation5.skipToEnd();
            }
            MiuiFreeFormSpringAnimation miuiFreeFormSpringAnimation6 = this.mTranslateRightAnimation;
            if (miuiFreeFormSpringAnimation6 != null) {
                miuiFreeFormSpringAnimation6.skipToEnd();
            }
            MiuiFreeFormSpringAnimation miuiFreeFormSpringAnimation7 = this.mTranslateBottomAnimation;
            if (miuiFreeFormSpringAnimation7 != null) {
                miuiFreeFormSpringAnimation7.skipToEnd();
            }
            MiuiFreeFormSpringAnimation miuiFreeFormSpringAnimation8 = this.mAlphaAnimation;
            if (miuiFreeFormSpringAnimation8 != null) {
                miuiFreeFormSpringAnimation8.skipToEnd();
            }
            resetAnimalState();
        }

        private void cancel() {
            MiuiFreeFormSpringAnimation miuiFreeFormSpringAnimation = this.mScaleAnimation;
            if (miuiFreeFormSpringAnimation != null) {
                miuiFreeFormSpringAnimation.cancel();
            }
            MiuiFreeFormSpringAnimation miuiFreeFormSpringAnimation2 = this.mScaleXAnimation;
            if (miuiFreeFormSpringAnimation2 != null) {
                miuiFreeFormSpringAnimation2.cancel();
            }
            MiuiFreeFormSpringAnimation miuiFreeFormSpringAnimation3 = this.mScaleYAnimation;
            if (miuiFreeFormSpringAnimation3 != null) {
                miuiFreeFormSpringAnimation3.cancel();
            }
            MiuiFreeFormSpringAnimation miuiFreeFormSpringAnimation4 = this.mTranslateYAnimation;
            if (miuiFreeFormSpringAnimation4 != null) {
                miuiFreeFormSpringAnimation4.cancel();
            }
            MiuiFreeFormSpringAnimation miuiFreeFormSpringAnimation5 = this.mTranslateXAnimation;
            if (miuiFreeFormSpringAnimation5 != null) {
                miuiFreeFormSpringAnimation5.cancel();
            }
            MiuiFreeFormSpringAnimation miuiFreeFormSpringAnimation6 = this.mTranslateBottomAnimation;
            if (miuiFreeFormSpringAnimation6 != null) {
                miuiFreeFormSpringAnimation6.cancel();
            }
            MiuiFreeFormSpringAnimation miuiFreeFormSpringAnimation7 = this.mTranslateRightAnimation;
            if (miuiFreeFormSpringAnimation7 != null) {
                miuiFreeFormSpringAnimation7.cancel();
            }
            MiuiFreeFormSpringAnimation miuiFreeFormSpringAnimation8 = this.mAlphaAnimation;
            if (miuiFreeFormSpringAnimation8 != null) {
                miuiFreeFormSpringAnimation8.cancel();
            }
            resetAnimalState();
        }

        public void start(int animationType) {
            this.mCurrentAnimation = animationType;
            MiuiFreeFormSpringAnimation miuiFreeFormSpringAnimation = this.mScaleAnimation;
            if (miuiFreeFormSpringAnimation != null) {
                this.mScaleEnd = false;
                miuiFreeFormSpringAnimation.start();
            }
            MiuiFreeFormSpringAnimation miuiFreeFormSpringAnimation2 = this.mScaleXAnimation;
            if (miuiFreeFormSpringAnimation2 != null) {
                this.mScaleXEnd = false;
                miuiFreeFormSpringAnimation2.start();
            }
            MiuiFreeFormSpringAnimation miuiFreeFormSpringAnimation3 = this.mScaleYAnimation;
            if (miuiFreeFormSpringAnimation3 != null) {
                this.mScaleYEnd = false;
                miuiFreeFormSpringAnimation3.start();
            }
            MiuiFreeFormSpringAnimation miuiFreeFormSpringAnimation4 = this.mTranslateYAnimation;
            if (miuiFreeFormSpringAnimation4 != null) {
                this.mTranslateYEnd = false;
                miuiFreeFormSpringAnimation4.start();
            }
            MiuiFreeFormSpringAnimation miuiFreeFormSpringAnimation5 = this.mTranslateXAnimation;
            if (miuiFreeFormSpringAnimation5 != null) {
                this.mTranslateXEnd = false;
                miuiFreeFormSpringAnimation5.start();
            }
            MiuiFreeFormSpringAnimation miuiFreeFormSpringAnimation6 = this.mTranslateRightAnimation;
            if (miuiFreeFormSpringAnimation6 != null) {
                this.mTranslateRightEnd = false;
                miuiFreeFormSpringAnimation6.start();
            }
            MiuiFreeFormSpringAnimation miuiFreeFormSpringAnimation7 = this.mTranslateBottomAnimation;
            if (miuiFreeFormSpringAnimation7 != null) {
                this.mTranslateBottomEnd = false;
                miuiFreeFormSpringAnimation7.start();
            }
            MiuiFreeFormSpringAnimation miuiFreeFormSpringAnimation8 = this.mAlphaAnimation;
            if (miuiFreeFormSpringAnimation8 != null) {
                this.mAlphaEnd = false;
                miuiFreeFormSpringAnimation8.start();
            }
        }

        public boolean isAnimalFinished() {
            return this.mScaleEnd && this.mScaleXEnd && this.mScaleYEnd && this.mTranslateYEnd && this.mTranslateXEnd && this.mTranslateRightEnd && this.mTranslateBottomEnd && this.mAlphaEnd;
        }
    }

    /* loaded from: classes.dex */
    public interface SpringAnimatorListener<Target> {
        void onAnimationEnd(MiuiFreeFormDynamicAnimation<?> miuiFreeFormDynamicAnimation, String str);

        void updateValue(Target target, float f, int i);

        default float getStartValue(int springAnimalType) {
            return MiuiFreeformPinManagerService.EDGE_AREA;
        }
    }
}
