package com.android.server.wm;

import android.animation.ValueAnimator;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.util.MiuiMultiWindowUtils;
import android.view.SurfaceControl;
import android.view.animation.Interpolator;
import android.view.animation.Transformation;
import com.android.server.wm.MiuiFreeFormDynamicAnimation;
import com.miui.internal.dynamicanimation.animation.FloatPropertyCompat;
import java.util.HashMap;
/* loaded from: classes.dex */
public class MiuiFreeFormGestureAnimator {
    public static final String ALPHA = "ALPHA";
    public static final String ALPHA_END_LISTENER = "ALPHA_END_LISTENER";
    public static final int ANIMATION_ALPHA = 13;
    public static final int ANIMATION_BOTTOM_BAR_FREEFROM_TO_LEFT_SMALL_FREEFORM = 18;
    public static final int ANIMATION_BOTTOM_BAR_FREEFROM_TO_RIGHT_SMALL_FREEFORM = 19;
    public static final int ANIMATION_BOTTOM_BAR_SET_PARAMS = 20;
    public static final int ANIMATION_CLOSE = 0;
    public static final int ANIMATION_DIRECT_OPEN = 15;
    public static final int ANIMATION_FLASHBACK_START_FROM_BACKGROUND = 22;
    public static final int ANIMATION_FOCUS_SHADOW = 16;
    public static final int ANIMATION_FREEFORM_PIN = 25;
    public static final int ANIMATION_FREEFORM_UNPIN = 28;
    public static final int ANIMATION_FREEFORM_WINDOW_TRANSLATE = 8;
    public static final int ANIMATION_FROM_SMALL_TO_FREEFORM_WINDOW = 5;
    public static final int ANIMATION_FROM_SMALL_TO_FULL_WINDOW = 6;
    public static final int ANIMATION_LAUNCH_SMALL_FREEFORM_WINDOW = 12;
    public static final int ANIMATION_LOSE_FOCUS_SHADOW = 17;
    public static final int ANIMATION_OPEN = 1;
    public static final int ANIMATION_REQUEST_ORIENTATION_RESIZE_TASK = 21;
    public static final int ANIMATION_RESET = 2;
    public static final int ANIMATION_RESIZE_BACK_LEFT_BOTTOM = 10;
    public static final int ANIMATION_RESIZE_BACK_RIGHT_BOTTOM = 11;
    public static final int ANIMATION_SCALE_HOME = 14;
    public static final int ANIMATION_SHOW_FREEFORM_WINDOW = 4;
    public static final int ANIMATION_SHOW_SMALL_WINDOW = 3;
    public static final int ANIMATION_SLIDE_FREEFORM_TO_PIN = 26;
    public static final int ANIMATION_SLIDE_SMALL_FREEFORM_TO_PIN = 27;
    public static final int ANIMATION_SMALL_FREEFORM_CLOSE = 24;
    public static final int ANIMATION_SMALL_FREEFORM_EXIT_FROM_HOT_AREA = 23;
    public static final int ANIMATION_SMALL_FREEFORM_PIN = 30;
    public static final int ANIMATION_SMALL_FREEFORM_UNPIN = 29;
    public static final int ANIMATION_SMALL_TO_CORNER = 9;
    public static final int ANIMATION_SMALL_WINDOW_TRANSLATE = 7;
    public static final int ANIMATION_UNDEFINED = -1;
    public static final float BOTTOM_BAR_SET_PARAMS_MOVE_DAMPINT = 0.99f;
    public static final float BOTTOM_BAR_SET_PARAMS_MOVE_STIFFNESS = 3947.8f;
    public static final float BOTTOM_BAR_SET_PARAMS_UP_DAMPINT = 0.75f;
    public static final float BOTTOM_BAR_SET_PARAMS_UP_STIFFNESS = 130.5f;
    public static final String CLIP_HEIGHT_Y_END_LISTENER = "CLIP_HEIGHT_Y_END_LISTENER";
    public static final String CLIP_WIDTH_X_END_LISTENER = "CLIP_WIDTH_X_END_LISTENER";
    public static final float CLOSE_ANIM_DAMPINT = 0.95f;
    public static final float CLOSE_ANIM_STIFFNESS = 322.2f;
    public static final float DAMPINT = 0.99f;
    public static final String FLOAT_PROPERTY_ALPHA = "ALPHA";
    public static final String FLOAT_PROPERTY_CLIP_HEIGHT_Y = "CLIP_HEIGHT_Y";
    public static final String FLOAT_PROPERTY_CLIP_WIDTH_X = "CLIP_WIDTH_X";
    public static final String FLOAT_PROPERTY_ROUNDCORNER = "ROUNDCORNER";
    public static final String FLOAT_PROPERTY_SCALE = "SCALE";
    public static final String FLOAT_PROPERTY_SCALE_X = "SCALE_X";
    public static final String FLOAT_PROPERTY_SCALE_Y = "SCALE_Y";
    public static final String FLOAT_PROPERTY_SHADOW_ALPHA = "SHADOW_ALPHA";
    public static final String FLOAT_PROPERTY_TRANSLATE_X = "TRANSLATE_X";
    public static final String FLOAT_PROPERTY_TRANSLATE_Y = "TRANSLATE_Y";
    public static final float FOCUS_SHADOW_ALPHA = 0.09f;
    public static final float LOSE_FOCUS_SHADOW_ALPHA = 0.03f;
    public static final float MINI_WINDOW_SHADOW_ALPHA = 0.03f;
    public static final float PICKUP_FOCUS_SHADOW_ALPHA = 0.06f;
    public static final float PICKUP_WINDOW_SCALE = 1.02f;
    public static final float PIN_DAMPINT = 0.9f;
    public static final float PIN_STIFFNESS = 322.27f;
    public static final float PIN_TRAN_DAMPINT = 0.78f;
    public static final float PIN_TRAN_STIFFNESS = 109.7f;
    static final Interpolator QUINT_EASE_OUT_INTERPOLATOR = new QuintEaseOutInterpolator();
    public static final String ROUNDCORNER_END_LISTENER = "ROUNDCORNER_END_LISTENER";
    public static final String SCALE_END_LISTENER = "SCALE_END_LISTENER";
    public static final String SCALE_X_END_LISTENER = "SCALE_X_END_LISTENER";
    public static final String SCALE_Y_END_LISTENER = "SCALE_Y_END_LISTENER";
    public static final String SHADOW_ALPHA_END_LISTENER = "SHADOW_ALPHA_END_LISTENER";
    public static final float SMALL_WINDOW_DAMPINT = 0.7f;
    public static final float SMALL_WINDOW_MOVE_DAMPINT = 0.99f;
    public static final float SMALL_WINDOW_MOVE_STIFFNESS = 3947.8f;
    public static final float SMALL_WINDOW_PREPARE_TO_EXIT_SCALE = 0.85f;
    public static final float SMALL_WINDOW_SCALE_DAMPING = 0.9f;
    public static final float SMALL_WINDOW_SCALE_STIFFNESS = 438.65f;
    public static final float SMALL_WINDOW_STIFFNESS = 631.7f;
    public static final float SMALL_WINDOW_UP_DAMPINT = 0.75f;
    public static final float SMALL_WINDOW_UP_STIFFNESS = 130.5f;
    public static final int SPRINGANIMATION_ALPHA = 1;
    public static final int SPRINGANIMATION_CLIP_HEIGHT_Y = 9;
    public static final int SPRINGANIMATION_CLIP_WIDTH_X = 8;
    public static final int SPRINGANIMATION_ROUNDCORNER = 7;
    public static final int SPRINGANIMATION_SCALE = 4;
    public static final int SPRINGANIMATION_SCALE_X = 5;
    public static final int SPRINGANIMATION_SCALE_Y = 6;
    public static final int SPRINGANIMATION_SHADOW_ALPHA = 10;
    public static final int SPRINGANIMATION_TRANSLATE_X = 2;
    public static final int SPRINGANIMATION_TRANSLATE_Y = 3;
    public static final float STIFFNESS = 2000.0f;
    public static final String TAG = "MiuiFreeFormGestureAnimator";
    public static final String TRANSLATE_X_END_LISTENER = "TRANSLATE_X_END_LISTENER";
    public static final String TRANSLATE_Y_END_LISTENER = "TRANSLATE_Y_END_LISTENER";
    public static final float UNPIN_TRAN_DAMPINT = 0.99f;
    final SurfaceControl.Transaction mGestureTransaction = new SurfaceControl.Transaction();
    private final HashMap<WindowToken, SurfaceControl> mLeashMap = new HashMap<>();
    private final MiuiFreeFormGesturePointerEventListener mListener;
    private final WindowManagerService mService;

    /* loaded from: classes.dex */
    public static class TmpValues {
        final Transformation transformation = new Transformation();
        final float[] floats = new float[9];
    }

    public MiuiFreeFormGestureAnimator(WindowManagerService service, MiuiFreeFormGesturePointerEventListener listener) {
        this.mService = service;
        this.mListener = listener;
        AppTransitionInjector.initDisplayRoundCorner(service.mContext);
    }

    public void createLeash(MiuiFreeFormActivityStack stack) {
        synchronized (this) {
            stack.createLeashIfNeeded(MiuiMultiWindowUtils.getFreeformRoundCorner(this.mService.mContext));
        }
    }

    public void createFlashBackLeash(WindowToken token, MiuiFreeFormActivityStack stack) {
        synchronized (this) {
            if (token == null || stack == null) {
                return;
            }
            if (this.mLeashMap.containsKey(token)) {
                return;
            }
            ActivityRecord activityRecord = stack.mTask.getTopActivity(false, true);
            DisplayContent displayContent = stack.mTask.mDisplayContent;
            if (activityRecord != null && displayContent != null) {
                SurfaceControl curSurface = token.getSurfaceControl();
                if (token.hasCommittedReparentToAnimationLeash()) {
                    token.cancelAnimation();
                }
                int width = token.getSurfaceWidth();
                int height = token.getSurfaceHeight();
                SurfaceControl leash = displayContent.makeOverlay().setName(curSurface + "-miui-freeform-flashback-leash").setBufferSize(width, height).build();
                this.mGestureTransaction.show(leash);
                this.mGestureTransaction.reparent(curSurface, leash);
                if (leash != null && leash.isValid()) {
                    this.mGestureTransaction.setRelativeLayer(leash, activityRecord.getSurfaceControl(), activityRecord.getPrefixOrderIndex() + 10);
                    this.mLeashMap.put(token, leash);
                }
            }
        }
    }

    public void resetFlashBackLeash(WindowToken token) {
        SurfaceControl leash;
        synchronized (this) {
            if (this.mLeashMap.containsKey(token) && (leash = this.mLeashMap.remove(token)) != null && leash.isValid()) {
                this.mGestureTransaction.remove(leash);
                applyTransaction();
            }
        }
    }

    public void reparentFlashBackLeash(WindowToken token) {
        SurfaceControl leash;
        synchronized (this) {
            if (this.mLeashMap.containsKey(token) && (leash = this.mLeashMap.remove(token)) != null && leash.isValid()) {
                SurfaceControl surface = token.getSurfaceControl();
                SurfaceControl parent = token.getParentSurfaceControl();
                if (parent != null && surface != null) {
                    this.mGestureTransaction.reparent(surface, parent);
                }
                this.mGestureTransaction.remove(leash);
                applyTransaction();
            }
        }
    }

    public void setPositionInTransaction(WindowToken token, float x, float y) {
        synchronized (this) {
            if (token != null) {
                if (!this.mLeashMap.containsKey(token)) {
                    return;
                }
            }
            SurfaceControl leash = this.mLeashMap.get(token);
            if (leash != null && leash.isValid()) {
                this.mGestureTransaction.setPosition(leash, x, y);
            }
        }
    }

    public void setAlphaInTransaction(MiuiFreeFormActivityStack stack, float alpha) {
        synchronized (this) {
            if (stack == null) {
                return;
            }
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

    public void setCornerRadiusInTransaction(MiuiFreeFormActivityStack stack, float radius) {
        synchronized (this) {
            if (stack == null) {
                return;
            }
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

    public void setCornerRadiusInTransactionForSurfaceControl(SurfaceControl surface, float radius) {
        synchronized (this) {
            if (surface != null) {
                if (surface.isValid()) {
                    this.mGestureTransaction.setCornerRadius(surface, radius);
                }
            }
        }
    }

    public void setAlphaInTransaction(SurfaceControl surface, float alpha) {
        synchronized (this) {
            if (surface != null) {
                if (surface.isValid()) {
                    this.mGestureTransaction.setAlpha(surface, alpha);
                }
            }
        }
    }

    void hideTaskDimmerLayer(MiuiFreeFormActivityStack stack) {
        Dimmer dimmer;
        if (stack != null && (dimmer = stack.mTask.getDimmer()) != null && dimmer.mDimState != null && dimmer.mDimState.isVisible && dimmer.mDimState.mDimLayer != null) {
            this.mGestureTransaction.hide(dimmer.mDimState.mDimLayer);
        }
    }

    public void hideStack(MiuiFreeFormActivityStack stack) {
        synchronized (this.mService.mGlobalLock) {
            synchronized (this) {
                if (stack != null && stack.mTask != null) {
                    hide(stack.mTask);
                }
            }
        }
    }

    public void hide(WindowContainer wc) {
        SurfaceControl sc;
        if (wc != null && (sc = wc.mSurfaceControl) != null && sc.isValid()) {
            this.mGestureTransaction.hide(sc);
        }
    }

    public void showStack(MiuiFreeFormActivityStack stack) {
        showStack(stack, this.mGestureTransaction);
    }

    void showStack(MiuiFreeFormActivityStack stack, SurfaceControl.Transaction t) {
        synchronized (this.mService.mGlobalLock) {
            synchronized (this) {
                if (stack == null || t == null) {
                    return;
                }
                SurfaceControl sc = stack.mTask.getSurfaceControl();
                if (sc != null && sc.isValid()) {
                    t.show(sc);
                }
            }
        }
    }

    void hideAppWindowToken(ActivityRecord activityRecord) {
        synchronized (this.mService.mGlobalLock) {
            if (activityRecord == null) {
                return;
            }
            SurfaceControl sc = activityRecord.mSurfaceControl;
            if (sc != null && sc.isValid()) {
                this.mGestureTransaction.hide(sc);
            }
        }
    }

    public void setMatrixInTransaction(MiuiFreeFormActivityStack stack, float dsdx, float dtdx, float dtdy, float dsdy) {
        synchronized (this) {
            if (stack == null) {
                return;
            }
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

    public void setMatrixInTransaction(MiuiFreeFormActivityStack stack, Matrix matrix, float[] float9) {
        synchronized (this) {
            if (stack == null) {
                return;
            }
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

    public void setPositionInTransaction(MiuiFreeFormActivityStack stack, float x, float y) {
        synchronized (this) {
            if (stack == null) {
                return;
            }
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

    public void setWindowCropInTransaction(MiuiFreeFormActivityStack stack, Rect clipRect) {
        synchronized (this) {
            if (stack == null) {
                return;
            }
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

    public void removeAnimationControlLeash(MiuiFreeFormActivityStack stack) {
        synchronized (this.mService.mGlobalLock) {
            synchronized (this) {
                if (stack != null) {
                    stack.removeAnimationControlLeash();
                }
            }
        }
    }

    public void applyTransaction() {
        synchronized (this) {
            this.mGestureTransaction.apply();
        }
    }

    /* loaded from: classes.dex */
    public static class SfValueAnimator extends ValueAnimator {
        public SfValueAnimator() {
            setFloatValues(MiuiFreeformPinManagerService.EDGE_AREA, 1.0f);
        }
    }

    /* loaded from: classes.dex */
    private static class QuintEaseOutInterpolator implements Interpolator {
        private QuintEaseOutInterpolator() {
        }

        @Override // android.animation.TimeInterpolator
        public float getInterpolation(float t) {
            float t2 = t - 1.0f;
            return (t2 * t2 * t2 * t2 * t2) + 1.0f;
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private FloatPropertyCompat createWindowStateProperty(String type) {
        char c;
        switch (type.hashCode()) {
            case -2137640112:
                if (type.equals(FLOAT_PROPERTY_CLIP_HEIGHT_Y)) {
                    c = '\b';
                    break;
                }
                c = 65535;
                break;
            case -1666090365:
                if (type.equals(FLOAT_PROPERTY_SCALE_X)) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case -1666090364:
                if (type.equals(FLOAT_PROPERTY_SCALE_Y)) {
                    c = 3;
                    break;
                }
                c = 65535;
                break;
            case 62372158:
                if (type.equals("ALPHA")) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            case 78713130:
                if (type.equals(FLOAT_PROPERTY_SCALE)) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case 312310151:
                if (type.equals(FLOAT_PROPERTY_TRANSLATE_X)) {
                    c = 4;
                    break;
                }
                c = 65535;
                break;
            case 312310152:
                if (type.equals(FLOAT_PROPERTY_TRANSLATE_Y)) {
                    c = 5;
                    break;
                }
                c = 65535;
                break;
            case 1023173136:
                if (type.equals(FLOAT_PROPERTY_CLIP_WIDTH_X)) {
                    c = 7;
                    break;
                }
                c = 65535;
                break;
            case 1984770399:
                if (type.equals(FLOAT_PROPERTY_SHADOW_ALPHA)) {
                    c = '\t';
                    break;
                }
                c = 65535;
                break;
            case 2033964355:
                if (type.equals(FLOAT_PROPERTY_ROUNDCORNER)) {
                    c = 6;
                    break;
                }
                c = 65535;
                break;
            default:
                c = 65535;
                break;
        }
        switch (c) {
            case 0:
                FloatPropertyCompat floatPropertyCompat = new MiuiFreeFormDynamicAnimation.ActivityStackProperty("ALPHA") { // from class: com.android.server.wm.MiuiFreeFormGestureAnimator.1
                    public float getValue(MiuiFreeFormActivityStack stack) {
                        return MiuiFreeformPinManagerService.EDGE_AREA;
                    }

                    public void setValue(MiuiFreeFormActivityStack stack, float value) {
                        MiuiFreeFormGestureAnimator.this.mListener.mFreeFormWindowMotionHelper.setAlpha(stack, value);
                    }
                };
                return floatPropertyCompat;
            case 1:
                FloatPropertyCompat floatPropertyCompat2 = new MiuiFreeFormDynamicAnimation.ActivityStackProperty(FLOAT_PROPERTY_SCALE) { // from class: com.android.server.wm.MiuiFreeFormGestureAnimator.2
                    public float getValue(MiuiFreeFormActivityStack stack) {
                        return MiuiFreeformPinManagerService.EDGE_AREA;
                    }

                    public void setValue(MiuiFreeFormActivityStack stack, float value) {
                        MiuiFreeFormGestureAnimator.this.mListener.mFreeFormWindowMotionHelper.setScale(stack, value);
                    }
                };
                return floatPropertyCompat2;
            case 2:
                FloatPropertyCompat floatPropertyCompat3 = new MiuiFreeFormDynamicAnimation.ActivityStackProperty(FLOAT_PROPERTY_SCALE_X) { // from class: com.android.server.wm.MiuiFreeFormGestureAnimator.3
                    public float getValue(MiuiFreeFormActivityStack stack) {
                        return MiuiFreeformPinManagerService.EDGE_AREA;
                    }

                    public void setValue(MiuiFreeFormActivityStack stack, float value) {
                        MiuiFreeFormGestureAnimator.this.mListener.mFreeFormWindowMotionHelper.setXScale(stack, value);
                    }
                };
                return floatPropertyCompat3;
            case 3:
                FloatPropertyCompat floatPropertyCompat4 = new MiuiFreeFormDynamicAnimation.ActivityStackProperty(FLOAT_PROPERTY_SCALE_Y) { // from class: com.android.server.wm.MiuiFreeFormGestureAnimator.4
                    public float getValue(MiuiFreeFormActivityStack stack) {
                        return MiuiFreeformPinManagerService.EDGE_AREA;
                    }

                    public void setValue(MiuiFreeFormActivityStack stack, float value) {
                        MiuiFreeFormGestureAnimator.this.mListener.mFreeFormWindowMotionHelper.setYScale(stack, value);
                    }
                };
                return floatPropertyCompat4;
            case 4:
                FloatPropertyCompat floatPropertyCompat5 = new MiuiFreeFormDynamicAnimation.ActivityStackProperty(FLOAT_PROPERTY_TRANSLATE_X) { // from class: com.android.server.wm.MiuiFreeFormGestureAnimator.5
                    public float getValue(MiuiFreeFormActivityStack stack) {
                        return MiuiFreeformPinManagerService.EDGE_AREA;
                    }

                    public void setValue(MiuiFreeFormActivityStack stack, float value) {
                        MiuiFreeFormGestureAnimator.this.mListener.mFreeFormWindowMotionHelper.setPositionX(stack, value);
                    }
                };
                return floatPropertyCompat5;
            case 5:
                FloatPropertyCompat floatPropertyCompat6 = new MiuiFreeFormDynamicAnimation.ActivityStackProperty(FLOAT_PROPERTY_TRANSLATE_Y) { // from class: com.android.server.wm.MiuiFreeFormGestureAnimator.6
                    public float getValue(MiuiFreeFormActivityStack stack) {
                        return MiuiFreeformPinManagerService.EDGE_AREA;
                    }

                    public void setValue(MiuiFreeFormActivityStack stack, float value) {
                        MiuiFreeFormGestureAnimator.this.mListener.mFreeFormWindowMotionHelper.setPositionY(stack, value);
                    }
                };
                return floatPropertyCompat6;
            case 6:
                FloatPropertyCompat floatPropertyCompat7 = new MiuiFreeFormDynamicAnimation.ActivityStackProperty(FLOAT_PROPERTY_ROUNDCORNER) { // from class: com.android.server.wm.MiuiFreeFormGestureAnimator.7
                    public float getValue(MiuiFreeFormActivityStack stack) {
                        return MiuiFreeformPinManagerService.EDGE_AREA;
                    }

                    public void setValue(MiuiFreeFormActivityStack stack, float value) {
                        MiuiFreeFormGestureAnimator.this.mListener.mFreeFormWindowMotionHelper.setRoundCorner(stack, value);
                    }
                };
                return floatPropertyCompat7;
            case 7:
                FloatPropertyCompat floatPropertyCompat8 = new MiuiFreeFormDynamicAnimation.ActivityStackProperty(FLOAT_PROPERTY_CLIP_WIDTH_X) { // from class: com.android.server.wm.MiuiFreeFormGestureAnimator.8
                    public float getValue(MiuiFreeFormActivityStack stack) {
                        return MiuiFreeformPinManagerService.EDGE_AREA;
                    }

                    public void setValue(MiuiFreeFormActivityStack stack, float value) {
                        MiuiFreeFormGestureAnimator.this.mListener.mFreeFormWindowMotionHelper.setClipWidthX(stack, value);
                    }
                };
                return floatPropertyCompat8;
            case '\b':
                FloatPropertyCompat floatPropertyCompat9 = new MiuiFreeFormDynamicAnimation.ActivityStackProperty(FLOAT_PROPERTY_CLIP_HEIGHT_Y) { // from class: com.android.server.wm.MiuiFreeFormGestureAnimator.9
                    public float getValue(MiuiFreeFormActivityStack stack) {
                        return MiuiFreeformPinManagerService.EDGE_AREA;
                    }

                    public void setValue(MiuiFreeFormActivityStack stack, float value) {
                        MiuiFreeFormGestureAnimator.this.mListener.mFreeFormWindowMotionHelper.setClipHeightY(stack, value);
                    }
                };
                return floatPropertyCompat9;
            case '\t':
                FloatPropertyCompat floatPropertyCompat10 = new MiuiFreeFormDynamicAnimation.ActivityStackProperty(FLOAT_PROPERTY_SHADOW_ALPHA) { // from class: com.android.server.wm.MiuiFreeFormGestureAnimator.10
                    public float getValue(MiuiFreeFormActivityStack stack) {
                        return MiuiFreeformPinManagerService.EDGE_AREA;
                    }

                    public void setValue(MiuiFreeFormActivityStack stack, float value) {
                        MiuiFreeFormGestureAnimator.this.mListener.mFreeFormWindowMotionHelper.setShadowAlpha(stack, value);
                    }
                };
                return floatPropertyCompat10;
            default:
                return null;
        }
    }

    public MiuiFreeFormSpringAnimation createSpringAnimation(MiuiFreeFormActivityStack stack, float startValue, float finalPosition, float stiffness, float damping, float startVelocity, int animalType) {
        return createSpringAnimation(stack, startValue, finalPosition, stiffness, damping, startVelocity, animalType, false);
    }

    public MiuiFreeFormSpringAnimation createSpringAnimation(MiuiFreeFormActivityStack stack, float startValue, float finalPosition, float stiffness, float damping, float startVelocity, int animalType, boolean needExecuteWhenAnimationEnd) {
        MiuiFreeFormSpringAnimation springAnimation = null;
        switch (animalType) {
            case 1:
                springAnimation = new MiuiFreeFormSpringAnimation(stack, createWindowStateProperty("ALPHA"), finalPosition);
                springAnimation.setMinimumVisibleChange(0.00390625f);
                break;
            case 2:
                springAnimation = new MiuiFreeFormSpringAnimation(stack, createWindowStateProperty(FLOAT_PROPERTY_TRANSLATE_X), finalPosition);
                springAnimation.setMinimumVisibleChange(1.0f);
                break;
            case 3:
                springAnimation = new MiuiFreeFormSpringAnimation(stack, createWindowStateProperty(FLOAT_PROPERTY_TRANSLATE_Y), finalPosition);
                springAnimation.setMinimumVisibleChange(1.0f);
                break;
            case 4:
                springAnimation = new MiuiFreeFormSpringAnimation(stack, createWindowStateProperty(FLOAT_PROPERTY_SCALE), finalPosition);
                springAnimation.setMinimumVisibleChange(0.002f);
                break;
            case 5:
                springAnimation = new MiuiFreeFormSpringAnimation(stack, createWindowStateProperty(FLOAT_PROPERTY_SCALE_X), finalPosition);
                springAnimation.setMinimumVisibleChange(0.002f);
                break;
            case 6:
                springAnimation = new MiuiFreeFormSpringAnimation(stack, createWindowStateProperty(FLOAT_PROPERTY_SCALE_Y), finalPosition);
                springAnimation.setMinimumVisibleChange(0.002f);
                break;
            case 7:
                springAnimation = new MiuiFreeFormSpringAnimation(stack, createWindowStateProperty(FLOAT_PROPERTY_ROUNDCORNER), finalPosition);
                springAnimation.setMinimumVisibleChange(1.0f);
                break;
            case 8:
                springAnimation = new MiuiFreeFormSpringAnimation(stack, createWindowStateProperty(FLOAT_PROPERTY_CLIP_WIDTH_X), finalPosition);
                springAnimation.setMinimumVisibleChange(1.0f);
                break;
            case 9:
                springAnimation = new MiuiFreeFormSpringAnimation(stack, createWindowStateProperty(FLOAT_PROPERTY_CLIP_HEIGHT_Y), finalPosition);
                springAnimation.setMinimumVisibleChange(1.0f);
                break;
            case 10:
                springAnimation = new MiuiFreeFormSpringAnimation(stack, createWindowStateProperty(FLOAT_PROPERTY_SHADOW_ALPHA), finalPosition);
                springAnimation.setMinimumVisibleChange(0.00390625f);
                break;
        }
        if (springAnimation == null) {
            return null;
        }
        springAnimation.getSpring().setStiffness(stiffness);
        springAnimation.getSpring().setDampingRatio(damping);
        springAnimation.setStartValue(startValue);
        springAnimation.setStartVelocity(startVelocity);
        if (needExecuteWhenAnimationEnd) {
            springAnimation.setNeedExecuteCallbackWhenAnimationEnd(true);
        }
        return springAnimation;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    public MiuiFreeFormDynamicAnimation.OnAnimationEndListener createAnimationEndListener(String type) {
        char c;
        switch (type.hashCode()) {
            case -1900960104:
                if (type.equals(SHADOW_ALPHA_END_LISTENER)) {
                    c = '\t';
                    break;
                }
                c = 65535;
                break;
            case -1620347596:
                if (type.equals(ROUNDCORNER_END_LISTENER)) {
                    c = 6;
                    break;
                }
                c = 65535;
                break;
            case -1029893049:
                if (type.equals(CLIP_HEIGHT_Y_END_LISTENER)) {
                    c = '\b';
                    break;
                }
                c = 65535;
                break;
            case -832953965:
                if (type.equals(SCALE_Y_END_LISTENER)) {
                    c = 3;
                    break;
                }
                c = 65535;
                break;
            case -327395340:
                if (type.equals(SCALE_X_END_LISTENER)) {
                    c = 2;
                    break;
                }
                c = 65535;
                break;
            case 26978575:
                if (type.equals(TRANSLATE_Y_END_LISTENER)) {
                    c = 5;
                    break;
                }
                c = 65535;
                break;
            case 112134317:
                if (type.equals(SCALE_END_LISTENER)) {
                    c = 1;
                    break;
                }
                c = 65535;
                break;
            case 478731655:
                if (type.equals(CLIP_WIDTH_X_END_LISTENER)) {
                    c = 7;
                    break;
                }
                c = 65535;
                break;
            case 532537200:
                if (type.equals(TRANSLATE_X_END_LISTENER)) {
                    c = 4;
                    break;
                }
                c = 65535;
                break;
            case 1393369369:
                if (type.equals(ALPHA_END_LISTENER)) {
                    c = 0;
                    break;
                }
                c = 65535;
                break;
            default:
                c = 65535;
                break;
        }
        switch (c) {
            case 0:
                MiuiFreeFormDynamicAnimation.OnAnimationEndListener onAnimationEndListener = new MiuiFreeFormDynamicAnimation.OnAnimationEndListener() { // from class: com.android.server.wm.MiuiFreeFormGestureAnimator.11
                    @Override // com.android.server.wm.MiuiFreeFormDynamicAnimation.OnAnimationEndListener
                    public void onAnimationEnd(MiuiFreeFormDynamicAnimation dynamicAnimation, boolean b, float v, float v1) {
                        MiuiFreeFormGestureAnimator.this.mListener.mFreeFormWindowMotionHelper.onAlphaAnimationEnd((MiuiFreeFormSpringAnimation) dynamicAnimation);
                    }
                };
                return onAnimationEndListener;
            case 1:
                MiuiFreeFormDynamicAnimation.OnAnimationEndListener onAnimationEndListener2 = new MiuiFreeFormDynamicAnimation.OnAnimationEndListener() { // from class: com.android.server.wm.MiuiFreeFormGestureAnimator.12
                    @Override // com.android.server.wm.MiuiFreeFormDynamicAnimation.OnAnimationEndListener
                    public void onAnimationEnd(MiuiFreeFormDynamicAnimation dynamicAnimation, boolean b, float v, float v1) {
                        MiuiFreeFormGestureAnimator.this.mListener.mFreeFormWindowMotionHelper.onScaleAnimationEnd((MiuiFreeFormSpringAnimation) dynamicAnimation);
                    }
                };
                return onAnimationEndListener2;
            case 2:
                MiuiFreeFormDynamicAnimation.OnAnimationEndListener onAnimationEndListener3 = new MiuiFreeFormDynamicAnimation.OnAnimationEndListener() { // from class: com.android.server.wm.MiuiFreeFormGestureAnimator.13
                    @Override // com.android.server.wm.MiuiFreeFormDynamicAnimation.OnAnimationEndListener
                    public void onAnimationEnd(MiuiFreeFormDynamicAnimation dynamicAnimation, boolean b, float v, float v1) {
                        MiuiFreeFormGestureAnimator.this.mListener.mFreeFormWindowMotionHelper.onScaleXAnimationEnd((MiuiFreeFormSpringAnimation) dynamicAnimation);
                    }
                };
                return onAnimationEndListener3;
            case 3:
                MiuiFreeFormDynamicAnimation.OnAnimationEndListener onAnimationEndListener4 = new MiuiFreeFormDynamicAnimation.OnAnimationEndListener() { // from class: com.android.server.wm.MiuiFreeFormGestureAnimator.14
                    @Override // com.android.server.wm.MiuiFreeFormDynamicAnimation.OnAnimationEndListener
                    public void onAnimationEnd(MiuiFreeFormDynamicAnimation dynamicAnimation, boolean b, float v, float v1) {
                        MiuiFreeFormGestureAnimator.this.mListener.mFreeFormWindowMotionHelper.onScaleYAnimationEnd((MiuiFreeFormSpringAnimation) dynamicAnimation);
                    }
                };
                return onAnimationEndListener4;
            case 4:
                MiuiFreeFormDynamicAnimation.OnAnimationEndListener onAnimationEndListener5 = new MiuiFreeFormDynamicAnimation.OnAnimationEndListener() { // from class: com.android.server.wm.MiuiFreeFormGestureAnimator.15
                    @Override // com.android.server.wm.MiuiFreeFormDynamicAnimation.OnAnimationEndListener
                    public void onAnimationEnd(MiuiFreeFormDynamicAnimation dynamicAnimation, boolean b, float v, float v1) {
                        MiuiFreeFormGestureAnimator.this.mListener.mFreeFormWindowMotionHelper.onTranslateXAnimationEnd((MiuiFreeFormSpringAnimation) dynamicAnimation);
                    }
                };
                return onAnimationEndListener5;
            case 5:
                MiuiFreeFormDynamicAnimation.OnAnimationEndListener onAnimationEndListener6 = new MiuiFreeFormDynamicAnimation.OnAnimationEndListener() { // from class: com.android.server.wm.MiuiFreeFormGestureAnimator.16
                    @Override // com.android.server.wm.MiuiFreeFormDynamicAnimation.OnAnimationEndListener
                    public void onAnimationEnd(MiuiFreeFormDynamicAnimation dynamicAnimation, boolean b, float v, float v1) {
                        MiuiFreeFormGestureAnimator.this.mListener.mFreeFormWindowMotionHelper.onTranslateYAnimationEnd((MiuiFreeFormSpringAnimation) dynamicAnimation);
                    }
                };
                return onAnimationEndListener6;
            case 6:
                MiuiFreeFormDynamicAnimation.OnAnimationEndListener onAnimationEndListener7 = new MiuiFreeFormDynamicAnimation.OnAnimationEndListener() { // from class: com.android.server.wm.MiuiFreeFormGestureAnimator.17
                    @Override // com.android.server.wm.MiuiFreeFormDynamicAnimation.OnAnimationEndListener
                    public void onAnimationEnd(MiuiFreeFormDynamicAnimation dynamicAnimation, boolean b, float v, float v1) {
                        MiuiFreeFormGestureAnimator.this.mListener.mFreeFormWindowMotionHelper.onRoundCornorAnimationEnd((MiuiFreeFormSpringAnimation) dynamicAnimation);
                    }
                };
                return onAnimationEndListener7;
            case 7:
                MiuiFreeFormDynamicAnimation.OnAnimationEndListener onAnimationEndListener8 = new MiuiFreeFormDynamicAnimation.OnAnimationEndListener() { // from class: com.android.server.wm.MiuiFreeFormGestureAnimator.18
                    @Override // com.android.server.wm.MiuiFreeFormDynamicAnimation.OnAnimationEndListener
                    public void onAnimationEnd(MiuiFreeFormDynamicAnimation dynamicAnimation, boolean b, float v, float v1) {
                        MiuiFreeFormGestureAnimator.this.mListener.mFreeFormWindowMotionHelper.onClipWidthXAnimationEnd((MiuiFreeFormSpringAnimation) dynamicAnimation);
                    }
                };
                return onAnimationEndListener8;
            case '\b':
                MiuiFreeFormDynamicAnimation.OnAnimationEndListener onAnimationEndListener9 = new MiuiFreeFormDynamicAnimation.OnAnimationEndListener() { // from class: com.android.server.wm.MiuiFreeFormGestureAnimator.19
                    @Override // com.android.server.wm.MiuiFreeFormDynamicAnimation.OnAnimationEndListener
                    public void onAnimationEnd(MiuiFreeFormDynamicAnimation dynamicAnimation, boolean b, float v, float v1) {
                        MiuiFreeFormGestureAnimator.this.mListener.mFreeFormWindowMotionHelper.onClipHeightYAnimationEnd((MiuiFreeFormSpringAnimation) dynamicAnimation);
                    }
                };
                return onAnimationEndListener9;
            case '\t':
                MiuiFreeFormDynamicAnimation.OnAnimationEndListener onAnimationEndListener10 = new MiuiFreeFormDynamicAnimation.OnAnimationEndListener() { // from class: com.android.server.wm.MiuiFreeFormGestureAnimator.20
                    @Override // com.android.server.wm.MiuiFreeFormDynamicAnimation.OnAnimationEndListener
                    public void onAnimationEnd(MiuiFreeFormDynamicAnimation dynamicAnimation, boolean b, float v, float v1) {
                        MiuiFreeFormGestureAnimator.this.mListener.mFreeFormWindowMotionHelper.onShadowAlphaAnimationEnd((MiuiFreeFormSpringAnimation) dynamicAnimation);
                    }
                };
                return onAnimationEndListener10;
            default:
                return null;
        }
    }

    /* loaded from: classes.dex */
    public static final class AnimalLock {
        MiuiFreeFormSpringAnimation mAlphaAnimation;
        MiuiFreeFormSpringAnimation mClipHeightYAnimation;
        MiuiFreeFormSpringAnimation mClipWidthXAnimation;
        MiuiFreeFormSpringAnimation mRoundCornorAnimation;
        MiuiFreeFormSpringAnimation mScaleAnimation;
        MiuiFreeFormSpringAnimation mScaleXAnimation;
        MiuiFreeFormSpringAnimation mScaleYAnimation;
        MiuiFreeFormSpringAnimation mShadowAlphaAnimation;
        MiuiFreeFormActivityStack mStack;
        MiuiFreeFormSpringAnimation mTranslateXAnimation;
        MiuiFreeFormSpringAnimation mTranslateYAnimation;
        int mCurrentAnimation = -1;
        boolean mScaleEnd = true;
        boolean mScaleXEnd = true;
        boolean mScaleYEnd = true;
        boolean mTranslateYEnd = true;
        boolean mTranslateXEnd = true;
        boolean mAlphaEnd = true;
        boolean mRoundCornerEnd = true;
        boolean mClipWidthXEnd = true;
        boolean mClipHeightYEnd = true;
        boolean mShadowAlphaEnd = true;

        public AnimalLock(MiuiFreeFormActivityStack stack) {
            this.mStack = stack;
        }

        public String toString() {
            return "AnimalLock{mStack=" + this.mStack + ", mCurrentAnimation=" + this.mCurrentAnimation + ", mScaleAnimation=" + this.mScaleAnimation + ", mScaleXAnimation=" + this.mScaleXAnimation + ", mScaleYAnimation=" + this.mScaleYAnimation + ", mTranslateYAnimation=" + this.mTranslateYAnimation + ", mTranslateXAnimation=" + this.mTranslateXAnimation + ", mAlphaAnimation=" + this.mAlphaAnimation + ", mRoundCornorAnimation=" + this.mRoundCornorAnimation + ", mShadowAlphaAnimation=" + this.mShadowAlphaAnimation + ", mScaleEnd=" + this.mScaleEnd + ", mScaleXEnd=" + this.mScaleXEnd + ", mScaleYEnd=" + this.mScaleYEnd + ", mTranslateYEnd=" + this.mTranslateYEnd + ", mTranslateXEnd=" + this.mTranslateXEnd + ", mAlphaEnd=" + this.mAlphaEnd + ", mRoundCoundEnd=" + this.mRoundCornerEnd + ", mShadowAlphaEnd=" + this.mShadowAlphaEnd + '}';
        }

        public void animationEnd(MiuiFreeFormSpringAnimation animation) {
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
            } else if (animation == this.mAlphaAnimation) {
                this.mAlphaEnd = true;
            } else if (animation == this.mRoundCornorAnimation) {
                this.mRoundCornerEnd = true;
            } else if (animation == this.mClipWidthXAnimation) {
                this.mClipWidthXEnd = true;
            } else if (animation == this.mClipHeightYAnimation) {
                this.mClipHeightYEnd = true;
            } else if (animation == this.mShadowAlphaAnimation) {
                this.mShadowAlphaEnd = true;
            }
        }

        public void resetAnimalState() {
            this.mScaleEnd = true;
            this.mScaleXEnd = true;
            this.mScaleYEnd = true;
            this.mTranslateYEnd = true;
            this.mTranslateXEnd = true;
            this.mAlphaEnd = true;
            this.mRoundCornerEnd = true;
            this.mShadowAlphaEnd = true;
            this.mClipWidthXEnd = true;
            this.mClipHeightYEnd = true;
            this.mScaleAnimation = null;
            this.mScaleXAnimation = null;
            this.mScaleYAnimation = null;
            this.mTranslateYAnimation = null;
            this.mTranslateXAnimation = null;
            this.mAlphaAnimation = null;
            this.mRoundCornorAnimation = null;
            this.mClipWidthXAnimation = null;
            this.mClipHeightYAnimation = null;
            this.mShadowAlphaAnimation = null;
            this.mStack = null;
        }

        public void skipToEnd() {
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
            MiuiFreeFormSpringAnimation miuiFreeFormSpringAnimation6 = this.mAlphaAnimation;
            if (miuiFreeFormSpringAnimation6 != null) {
                miuiFreeFormSpringAnimation6.skipToEnd();
            }
            MiuiFreeFormSpringAnimation miuiFreeFormSpringAnimation7 = this.mRoundCornorAnimation;
            if (miuiFreeFormSpringAnimation7 != null) {
                miuiFreeFormSpringAnimation7.skipToEnd();
            }
            MiuiFreeFormSpringAnimation miuiFreeFormSpringAnimation8 = this.mClipWidthXAnimation;
            if (miuiFreeFormSpringAnimation8 != null) {
                miuiFreeFormSpringAnimation8.skipToEnd();
            }
            MiuiFreeFormSpringAnimation miuiFreeFormSpringAnimation9 = this.mClipHeightYAnimation;
            if (miuiFreeFormSpringAnimation9 != null) {
                miuiFreeFormSpringAnimation9.skipToEnd();
            }
            MiuiFreeFormSpringAnimation miuiFreeFormSpringAnimation10 = this.mShadowAlphaAnimation;
            if (miuiFreeFormSpringAnimation10 != null) {
                miuiFreeFormSpringAnimation10.skipToEnd();
            }
            resetAnimalState();
        }

        public void cancel() {
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
            MiuiFreeFormSpringAnimation miuiFreeFormSpringAnimation6 = this.mAlphaAnimation;
            if (miuiFreeFormSpringAnimation6 != null) {
                miuiFreeFormSpringAnimation6.cancel();
            }
            MiuiFreeFormSpringAnimation miuiFreeFormSpringAnimation7 = this.mRoundCornorAnimation;
            if (miuiFreeFormSpringAnimation7 != null) {
                miuiFreeFormSpringAnimation7.cancel();
            }
            MiuiFreeFormSpringAnimation miuiFreeFormSpringAnimation8 = this.mClipWidthXAnimation;
            if (miuiFreeFormSpringAnimation8 != null) {
                miuiFreeFormSpringAnimation8.cancel();
            }
            MiuiFreeFormSpringAnimation miuiFreeFormSpringAnimation9 = this.mClipHeightYAnimation;
            if (miuiFreeFormSpringAnimation9 != null) {
                miuiFreeFormSpringAnimation9.cancel();
            }
            MiuiFreeFormSpringAnimation miuiFreeFormSpringAnimation10 = this.mShadowAlphaAnimation;
            if (miuiFreeFormSpringAnimation10 != null) {
                miuiFreeFormSpringAnimation10.cancel();
            }
            resetAnimalState();
        }

        public void start(int animationType, MiuiFreeFormGestureAnimator miuiFreeFormGestureAnimator) {
            MiuiFreeFormAnimationHandler.getInstance().setMiuiFreeFormGestureAnimator(miuiFreeFormGestureAnimator);
            start(animationType);
        }

        public void start(int animationType) {
            MiuiFreeFormActivityStack miuiFreeFormActivityStack = this.mStack;
            if (miuiFreeFormActivityStack != null && !miuiFreeFormActivityStack.checkReadyForFreeFormControl()) {
                return;
            }
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
            MiuiFreeFormSpringAnimation miuiFreeFormSpringAnimation6 = this.mAlphaAnimation;
            if (miuiFreeFormSpringAnimation6 != null) {
                this.mAlphaEnd = false;
                miuiFreeFormSpringAnimation6.start();
            }
            MiuiFreeFormSpringAnimation miuiFreeFormSpringAnimation7 = this.mRoundCornorAnimation;
            if (miuiFreeFormSpringAnimation7 != null) {
                this.mRoundCornerEnd = false;
                miuiFreeFormSpringAnimation7.start();
            }
            MiuiFreeFormSpringAnimation miuiFreeFormSpringAnimation8 = this.mClipWidthXAnimation;
            if (miuiFreeFormSpringAnimation8 != null) {
                this.mClipWidthXEnd = false;
                miuiFreeFormSpringAnimation8.start();
            }
            MiuiFreeFormSpringAnimation miuiFreeFormSpringAnimation9 = this.mClipHeightYAnimation;
            if (miuiFreeFormSpringAnimation9 != null) {
                this.mClipHeightYEnd = false;
                miuiFreeFormSpringAnimation9.start();
            }
            MiuiFreeFormSpringAnimation miuiFreeFormSpringAnimation10 = this.mShadowAlphaAnimation;
            if (miuiFreeFormSpringAnimation10 != null) {
                this.mShadowAlphaEnd = false;
                miuiFreeFormSpringAnimation10.start();
            }
        }

        public boolean isAnimalFinished() {
            return this.mScaleEnd && this.mScaleXEnd && this.mScaleYEnd && this.mTranslateYEnd && this.mTranslateXEnd && this.mAlphaEnd && this.mRoundCornerEnd && this.mClipWidthXEnd && this.mClipHeightYEnd && this.mShadowAlphaEnd;
        }
    }
}
