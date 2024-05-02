package com.android.server.wm;

import android.graphics.Rect;
import android.util.MiuiMultiWindowUtils;
import android.util.Slog;
import com.android.server.wm.MiuiFreeFormGestureAnimator;
/* loaded from: classes.dex */
public class MiuiFreeFormTaskPositioner {
    private static final String TAG = "MiuiFreeFormTaskPositioner";
    private MiuiFreeFormSmallWindowMotionHelper mFreeFormSmallWindowMotionHelper;
    private MiuiFreeFormWindowMotionHelper mFreeFormWindowMotionHelper;
    private MiuiFreeFormGesturePointerEventListener mListener;

    public MiuiFreeFormTaskPositioner(MiuiFreeFormGesturePointerEventListener listener) {
        this.mListener = listener;
        this.mFreeFormWindowMotionHelper = listener.mFreeFormWindowMotionHelper;
        this.mFreeFormSmallWindowMotionHelper = this.mListener.mSmallFreeFormWindowMotionHelper;
    }

    public void updateWindowDownBounds(float x, float y, float startDragX, float startDragY, float xVelocity, float yVelocity, boolean isVertical, Rect windowOriginalBounds, MiuiFreeFormActivityStack stack) {
        if (this.mFreeFormWindowMotionHelper.mShadowAnimator != null && this.mFreeFormWindowMotionHelper.mShadowAnimator.isRunning()) {
            this.mFreeFormWindowMotionHelper.mShadowAnimator.removeAllUpdateListeners();
            this.mFreeFormWindowMotionHelper.mShadowAnimator.cancel();
        }
        stack.mStackControlInfo.mWindowBounds.set(windowOriginalBounds);
        updateWindowMoveBounds(x, y, startDragX, startDragY, xVelocity, yVelocity, isVertical, windowOriginalBounds, stack);
    }

    public void updateWindowMoveBounds(float x, float y, float startDragX, float startDragY, float xVelocity, float yVelocity, boolean isVertical, Rect windowOriginalBounds, MiuiFreeFormActivityStack stack) {
        Exception e;
        int offsetX = Math.round(x - startDragX);
        int offsetY = Math.round(y - startDragY);
        if (stack.mStackControlInfo.mLastWindowDragBounds.isEmpty()) {
            stack.mStackControlInfo.mLastWindowDragBounds.set(windowOriginalBounds);
        } else {
            stack.mStackControlInfo.mLastWindowDragBounds.set(new Rect(stack.mStackControlInfo.mWindowBounds));
        }
        stack.mStackControlInfo.mWindowBounds = new Rect(windowOriginalBounds);
        stack.mStackControlInfo.mWindowBounds.offsetTo(windowOriginalBounds.left + offsetX, windowOriginalBounds.top + offsetY);
        if (stack.mStackControlInfo.mWindowBounds.top < MiuiFreeFormGestureDetector.getStatusBarHeight(this.mListener.mGestureController.mInsetsStateController, false)) {
            stack.mStackControlInfo.mWindowBounds.offsetTo(stack.mStackControlInfo.mWindowBounds.left, MiuiFreeFormGestureDetector.getStatusBarHeight(this.mListener.mGestureController.mInsetsStateController, false));
        }
        if (MiuiFreeFormGestureController.DEBUG) {
            Slog.d(TAG, "updateWindowMoveBounds startDragX" + startDragX + " startDragY:" + startDragY + " x:" + x + " y:" + y + "offsetX:" + offsetX + " offsetY:" + offsetY + " windowDragBounds:" + stack.mStackControlInfo.mWindowBounds + " windowOriginalBounds:" + windowOriginalBounds + " LastWindowDragBounds:" + stack.mStackControlInfo.mLastWindowDragBounds + " stack:" + stack);
        }
        try {
            try {
                translateScaleAnimal(x, y, xVelocity, yVelocity, stack.mStackControlInfo.mLastWindowDragBounds, stack.mStackControlInfo.mWindowBounds, 8, false, stack);
            } catch (Exception e2) {
                e = e2;
                e.printStackTrace();
            }
        } catch (Exception e3) {
            e = e3;
        }
    }

    /* JADX WARN: Can't wrap try/catch for region: R(26:2|(1:4)(4:5|(1:7)(1:8)|9|(1:11)(2:12|(1:14)(1:15)))|16|(1:18)(1:19)|20|(1:22)|23|(1:25)|26|(1:28)|29|(1:31)|32|(5:34|(1:39)(1:38)|40|220|44)(1:48)|49|(2:51|(9:57|60|74|61|62|70|63|68|69))(1:58)|59|60|74|61|62|70|63|68|69|(1:(0))) */
    /* JADX WARN: Code restructure failed: missing block: B:64:0x038f, code lost:
        r0 = e;
     */
    /* JADX WARN: Code restructure failed: missing block: B:65:0x0391, code lost:
        r0 = e;
     */
    /* JADX WARN: Code restructure failed: missing block: B:66:0x0392, code lost:
        r11 = r36;
     */
    /* JADX WARN: Code restructure failed: missing block: B:67:0x0399, code lost:
        r0.printStackTrace();
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public void updateWindowUpBounds(float r28, float r29, float r30, float r31, float r32, float r33, boolean r34, android.graphics.Rect r35, com.android.server.wm.MiuiFreeFormActivityStack r36) {
        /*
            Method dump skipped, instructions count: 932
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.wm.MiuiFreeFormTaskPositioner.updateWindowUpBounds(float, float, float, float, float, float, boolean, android.graphics.Rect, com.android.server.wm.MiuiFreeFormActivityStack):void");
    }

    private boolean isEnterPinMode(MiuiFreeFormActivityStack stack, float x, float y, float xVelocity, float yVelocity, float predictX, float predictY) {
        if (!this.mListener.mGestureController.mMiuiFreeFormManagerService.isSupportPin()) {
            return false;
        }
        if (stack.isInFreeFormMode()) {
            boolean isRight = predictX > ((float) (this.mListener.mScreenWidth / 2));
            boolean velocityDirectionToPin = !isRight ? xVelocity < MiuiFreeformPinManagerService.EDGE_AREA : xVelocity > MiuiFreeformPinManagerService.EDGE_AREA;
            boolean velocityDegreeToPin = ((double) Math.abs(xVelocity)) > Math.tan(0.2617993877991494d) * ((double) Math.abs(yVelocity));
            boolean isOut = MiuiFreeformPinManagerService.isOutsideScreen(x, y, predictX, predictY, this.mListener.mScreenWidth, this.mListener.mScreenHeight);
            boolean isStop = MiuiMultiWindowUtils.mergeXY(xVelocity, yVelocity) < ((double) MiuiMultiWindowUtils.applyDip2Px(363.64f));
            return isOut || (!isStop && velocityDirectionToPin && velocityDegreeToPin && MiuiMultiWindowUtils.outerScreen(predictX, MiuiMultiWindowUtils.applyDip2Px(145.45f), this.mListener.mScreenWidth));
        }
        boolean isOut2 = stack.isInMiniFreeFormMode();
        if (!isOut2) {
            return false;
        }
        boolean velocityDegreeToPin2 = Math.abs(xVelocity) > Math.abs(yVelocity);
        boolean isOut3 = MiuiFreeformPinManagerService.isOutsideScreen(x, y, predictX, predictY, this.mListener.mScreenWidth, this.mListener.mScreenHeight);
        boolean isStop2 = MiuiMultiWindowUtils.mergeXY(xVelocity, yVelocity) < ((double) MiuiMultiWindowUtils.applyDip2Px(363.64f));
        if ((!isOut3 || !velocityDegreeToPin2) && (!isStop2 || !MiuiMultiWindowUtils.outerScreen(x, MiuiMultiWindowUtils.applyDip2Px((float) MiuiFreeformPinManagerService.EDGE_AREA), this.mListener.mScreenWidth))) {
            return false;
        }
        return true;
    }

    public void updateSmallWindowDownBounds(MiuiFreeFormActivityStack stack) {
        MiuiFreeFormGestureAnimator.AnimalLock animalLock = this.mFreeFormWindowMotionHelper.mStackLocks.get(stack);
        if (animalLock != null && animalLock.mCurrentAnimation == 8) {
            Slog.d(TAG, "ANIMATION_FREEFORM_WINDOW_TRANSLATE cancel animalLock: " + animalLock);
            animalLock.cancel();
        }
    }

    public void updateSmallWindowMoveBounds(float x, float y, float xVelocity, float yVelocity, float startDragX, float startDragY, boolean isVertical, Rect windowOriginalBounds, MiuiFreeFormActivityStack stack) {
        Exception e;
        int offsetX = Math.round(x - startDragX);
        int offsetY = Math.round(y - startDragY);
        if (stack.mStackControlInfo.mLastWindowDragBounds.isEmpty()) {
            stack.mStackControlInfo.mLastWindowDragBounds.set(windowOriginalBounds);
        } else {
            stack.mStackControlInfo.mLastWindowDragBounds.set(new Rect(stack.mStackControlInfo.mSmallWindowBounds));
        }
        stack.mStackControlInfo.mSmallWindowBounds = new Rect(windowOriginalBounds);
        stack.mStackControlInfo.mSmallWindowBounds.offsetTo(windowOriginalBounds.left + offsetX, windowOriginalBounds.top + offsetY);
        if (MiuiFreeFormGestureController.DEBUG) {
            Slog.d(TAG, "updateSmallWindowMoveBounds startDragX" + startDragX + " startDragY:" + startDragY + " x:" + x + " y:" + y + "offsetX:" + offsetX + " offsetY:" + offsetY + " windowDragBounds:" + stack.mStackControlInfo.mSmallWindowBounds + " windowOriginalBounds:" + windowOriginalBounds);
        }
        if (!windowOriginalBounds.equals(stack.mStackControlInfo.mSmallWindowBounds)) {
            try {
            } catch (Exception e2) {
                e = e2;
            }
            try {
                smallFreeFormTranslateScaleAnimal(x, y, xVelocity, yVelocity, stack.mStackControlInfo.mLastWindowDragBounds, stack.mStackControlInfo.mSmallWindowBounds, 7, false, stack);
            } catch (Exception e3) {
                e = e3;
                e.printStackTrace();
            }
        }
    }

    public void updateSmallWindowUpBounds(float x, float y, float xVelocity, float yVelocity, float startDragX, float startDragY, boolean isVertical, Rect windowOriginalBounds, Rect windowTargetBounds, MiuiFreeFormActivityStack stack) {
        Slog.d(TAG, "updateSmallWindowUpBounds startDragX" + startDragX + " startDragY:" + startDragY + " x:" + x + " y:" + y + " windowOriginalBounds:" + windowOriginalBounds + " windowTargetBounds:" + windowTargetBounds);
        stack.mStackControlInfo.mSmallWindowBounds = new Rect(windowTargetBounds);
        try {
            float[] predictXY = MiuiMultiWindowUtils.getPredictXY(x, y, xVelocity, yVelocity, 1.2f);
            if (isEnterPinMode(stack, x, y, xVelocity, yVelocity, predictXY[0], predictXY[1])) {
                Slog.d(TAG, "move small freeofrm to pin tigger");
                stack.setInPinMode(true);
                smallFreeFormTranslateScaleAnimal(x, y, xVelocity, yVelocity, windowOriginalBounds, stack.mStackControlInfo.mSmallWindowBounds, 27, true, stack);
            } else if (this.mFreeFormSmallWindowMotionHelper.isUpToExit()) {
                smallFreeFormTranslateScaleAnimal(x, y, xVelocity, yVelocity, windowOriginalBounds, stack.mStackControlInfo.mSmallWindowBounds, 23, true, stack);
                this.mFreeFormSmallWindowMotionHelper.setUpToExit(false);
            } else {
                smallFreeFormTranslateScaleAnimal(x, y, xVelocity, yVelocity, windowOriginalBounds, stack.mStackControlInfo.mSmallWindowBounds, 9, true, stack);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        stack.mStackControlInfo.mLastWindowDragBounds.setEmpty();
    }

    public void translateScaleAnimal(float x, float y, float xVelocity, float yVelocity, Rect startWindowDragBounds, Rect finalWindowDragBounds, int animationType, boolean isUp, MiuiFreeFormActivityStack stack) {
        int animationType2;
        float targetWidthScale;
        float targetHeightScale;
        float targetWidthScale2;
        float targetHeightScale2;
        float targetTY;
        float targetTX;
        int i;
        float targetWidthScale3;
        float targetHeightScale3;
        if (!Float.isNaN(stack.mStackControlInfo.mSmallWindowTargetHScale) && !Float.isNaN(stack.mStackControlInfo.mSmallWindowTargetWScale)) {
            MiuiFreeFormGestureAnimator.AnimalLock animalLock = this.mFreeFormWindowMotionHelper.mStackLocks.get(stack);
            if (animalLock == null) {
                Slog.d(TAG, "translateScaleAnimal create animation isUp " + isUp);
                stack.mStackControlInfo.mOriPosX = startWindowDragBounds.left;
                stack.mStackControlInfo.mOriPosY = startWindowDragBounds.top;
                MiuiFreeFormGestureAnimator.AnimalLock animalLock2 = new MiuiFreeFormGestureAnimator.AnimalLock(stack);
                this.mFreeFormWindowMotionHelper.mStackLocks.put(stack, animalLock2);
                float startTX = startWindowDragBounds.left;
                float startTY = startWindowDragBounds.top;
                float targetTX2 = finalWindowDragBounds.left;
                float targetTY2 = finalWindowDragBounds.top;
                if (this.mFreeFormWindowMotionHelper.mEnteredHotArea) {
                    targetHeightScale3 = stack.mStackControlInfo.mSmallWindowTargetHScale;
                    targetWidthScale3 = stack.mStackControlInfo.mSmallWindowTargetWScale;
                } else if (!isUp) {
                    targetHeightScale3 = 1.02f;
                    targetWidthScale3 = 1.02f;
                } else {
                    targetHeightScale3 = 1.0f;
                    targetWidthScale3 = 1.0f;
                }
                MiuiFreeFormSpringAnimation tXSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, startTX, targetTX2, 2000.0f, 0.99f, MiuiFreeformPinManagerService.EDGE_AREA, 2);
                MiuiFreeFormSpringAnimation tYSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, startTY, targetTY2, 2000.0f, 0.99f, MiuiFreeformPinManagerService.EDGE_AREA, 3);
                MiuiFreeFormSpringAnimation scaleXSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, 1.0f, targetWidthScale3, 631.7f, 0.7f, MiuiFreeformPinManagerService.EDGE_AREA, 5);
                MiuiFreeFormSpringAnimation scaleYSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, 1.0f, targetHeightScale3, 631.7f, 0.7f, MiuiFreeformPinManagerService.EDGE_AREA, 6);
                animalLock2.mTranslateXAnimation = tXSpringAnimation;
                animalLock2.mTranslateYAnimation = tYSpringAnimation;
                animalLock2.mScaleXAnimation = scaleXSpringAnimation;
                animalLock2.mScaleYAnimation = scaleYSpringAnimation;
                animationType2 = animationType;
                animalLock = animalLock2;
            } else {
                if (MiuiFreeFormGestureController.DEBUG) {
                    Slog.d(TAG, "translateScaleAnimal update isUp:" + isUp);
                }
                float targetTX3 = finalWindowDragBounds.left;
                float targetTY3 = finalWindowDragBounds.top;
                if (this.mFreeFormWindowMotionHelper.mEnteredHotArea) {
                    targetHeightScale = stack.mStackControlInfo.mSmallWindowTargetHScale;
                    targetWidthScale = stack.mStackControlInfo.mSmallWindowTargetWScale;
                } else {
                    targetHeightScale = 1.02f;
                    targetWidthScale = 1.02f;
                    if (isUp) {
                        targetHeightScale = 1.0f;
                        targetWidthScale = 1.0f;
                    }
                }
                animationType2 = animationType;
                if (animationType2 == 26 && stack.inPinMode() && isUp) {
                    float[] predictXY = MiuiMultiWindowUtils.getPredictXY(x, y, xVelocity, yVelocity, 3.0f);
                    float targetX = predictXY[0];
                    float targetHeightScale4 = targetHeightScale;
                    float targetWidthScale4 = targetWidthScale;
                    float targetY = Math.max(MiuiMultiWindowUtils.applyDip2Px(35.0f), Math.min(MiuiMultiWindowUtils.getPredictY(x, y, predictXY[0], predictXY[1], this.mListener.mScreenWidth), (this.mListener.mScreenHeight - MiuiMultiWindowUtils.applyDip2Px(35.0f)) - ((int) MiuiMultiWindowUtils.applyDip2Px(64.0f))));
                    if (targetX > this.mListener.mScreenWidth / 2) {
                        i = (int) ((this.mListener.mScreenWidth - ((int) MiuiMultiWindowUtils.applyDip2Px(24.0f))) + 0.5f);
                    } else {
                        i = (int) (-((((int) MiuiMultiWindowUtils.applyDip2Px(64.0f)) - ((int) MiuiMultiWindowUtils.applyDip2Px(24.0f))) + 0.5f));
                    }
                    int left = i;
                    int top = (int) (0.5f + targetY);
                    float targetY2 = MiuiMultiWindowUtils.applyDip2Px(64.0f);
                    Rect finalBounds = new Rect(left, top, ((int) targetY2) + left, ((int) MiuiMultiWindowUtils.applyDip2Px(64.0f)) + top);
                    this.mListener.mGestureController.mMiuiFreeformPinManagerService.adjustFloatingWindowPosIfNeed(this.mListener.mGestureController.mMiuiFreeFormManagerService.getCurrPinFloatingWindowPos(true, true), finalBounds);
                    this.mListener.mGestureController.mMiuiFreeformPinManagerService.updatePinFloatingWindowPos(stack, finalBounds);
                    this.mListener.mGestureController.mMiuiFreeformPinManagerService.setPinFloatingWindowAnimationInfo(stack, false);
                    stack.mIsPinFloatingWindowPosInit = true;
                    ActivityRecord activityRecord = stack.mLastIconLayerWindowToken;
                    if (activityRecord != null && activityRecord.mFloatWindwoIconSurfaceControl != null) {
                        float targetTX4 = finalBounds.centerX() - (((stack.mTask.getBounds().width() * stack.mFreeFormScale) * 0.7f) / 2.0f);
                        float targetTY4 = finalBounds.centerY() - (((stack.mTask.getBounds().height() * stack.mFreeFormScale) * 0.7f) / 2.0f);
                        targetHeightScale2 = 0.7f;
                        targetWidthScale2 = 0.7f;
                        slideFreeformToPin(stack, animalLock, 0.7f, 0.7f, activityRecord);
                        this.mListener.hideInputMethodWindowIfNeeded();
                        targetTX = targetTX4;
                        targetTY = targetTY4;
                    } else {
                        stack.setInPinMode(false);
                        Slog.d(TAG, "skip slide mini freeform to pin animation activityRecord: " + activityRecord);
                        animationType2 = 8;
                        targetTX = targetTX3;
                        targetTY = targetTY3;
                        targetHeightScale2 = targetHeightScale4;
                        targetWidthScale2 = targetWidthScale4;
                    }
                } else {
                    targetTX = targetTX3;
                    targetTY = targetTY3;
                    targetHeightScale2 = targetHeightScale;
                    targetWidthScale2 = targetWidthScale;
                }
                if (isUp) {
                    if (animationType2 == 26 && stack.inPinMode() && isUp) {
                        animalLock.mTranslateXAnimation.getSpring().setStiffness(109.7f);
                        animalLock.mTranslateXAnimation.getSpring().setDampingRatio(0.78f);
                        animalLock.mTranslateYAnimation.getSpring().setStiffness(109.7f);
                        animalLock.mTranslateYAnimation.getSpring().setDampingRatio(0.78f);
                        animalLock.mScaleXAnimation.getSpring().setStiffness(322.27f);
                        animalLock.mScaleXAnimation.getSpring().setDampingRatio(0.9f);
                        animalLock.mScaleYAnimation.getSpring().setStiffness(322.27f);
                        animalLock.mScaleYAnimation.getSpring().setDampingRatio(0.9f);
                    } else {
                        animalLock.mTranslateXAnimation.getSpring().setStiffness(2000.0f);
                        animalLock.mTranslateXAnimation.getSpring().setDampingRatio(0.99f);
                        animalLock.mTranslateYAnimation.getSpring().setStiffness(2000.0f);
                        animalLock.mTranslateYAnimation.getSpring().setDampingRatio(0.99f);
                        animalLock.mScaleXAnimation.getSpring().setStiffness(631.7f);
                        animalLock.mScaleXAnimation.getSpring().setDampingRatio(0.7f);
                        animalLock.mScaleYAnimation.getSpring().setStiffness(631.7f);
                        animalLock.mScaleYAnimation.getSpring().setDampingRatio(0.7f);
                    }
                    animalLock.mTranslateXAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.TRANSLATE_X_END_LISTENER));
                    animalLock.mTranslateYAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.TRANSLATE_Y_END_LISTENER));
                    animalLock.mScaleXAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.SCALE_X_END_LISTENER));
                    animalLock.mScaleYAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.SCALE_Y_END_LISTENER));
                }
                animalLock.mScaleXAnimation.setStartValue(animalLock.mScaleXAnimation.getCurrentValue());
                animalLock.mScaleXAnimation.animateToFinalPosition(targetWidthScale2);
                animalLock.mScaleYAnimation.setStartValue(animalLock.mScaleYAnimation.getCurrentValue());
                animalLock.mScaleYAnimation.animateToFinalPosition(targetHeightScale2);
                animalLock.mTranslateXAnimation.setStartValue(animalLock.mTranslateXAnimation.getCurrentValue());
                animalLock.mTranslateXAnimation.animateToFinalPosition(targetTX);
                animalLock.mTranslateYAnimation.setStartValue(animalLock.mTranslateYAnimation.getCurrentValue());
                animalLock.mTranslateYAnimation.animateToFinalPosition(targetTY);
            }
            animalLock.start(animationType2);
            return;
        }
        Slog.d(TAG, "translateScaleAnimal invalid windowBounds");
    }

    public void smallFreeFormTranslateScaleAnimal(Rect startWindowDragBounds, Rect finalWindowDragBounds, int animationType, boolean isUp, MiuiFreeFormActivityStack stack) {
        smallFreeFormTranslateScaleAnimal(MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA, startWindowDragBounds, finalWindowDragBounds, animationType, isUp, stack);
    }

    public void smallFreeFormTranslateScaleAnimal(float x, float y, float xVelocity, float yVelocity, Rect startWindowDragBounds, Rect finalWindowDragBounds, int animationType, boolean isUp, MiuiFreeFormActivityStack stack) {
        int animationType2;
        MiuiFreeFormGestureAnimator.AnimalLock animalLock;
        MiuiFreeFormGestureAnimator.AnimalLock animalLock2;
        float targetTY;
        float targetTX;
        float targetY;
        int i;
        int animationType3;
        MiuiFreeFormGestureAnimator.AnimalLock animalLock3 = this.mFreeFormWindowMotionHelper.mStackLocks.get(stack);
        if (animalLock3 == null) {
            Slog.d(TAG, "translateAnimal create Animation");
            float startTX = startWindowDragBounds.left;
            float startTY = startWindowDragBounds.top;
            float startScaleX = stack.mStackControlInfo.mSmallWindowTargetWScale;
            float startScaleY = stack.mStackControlInfo.mSmallWindowTargetHScale;
            float f = finalWindowDragBounds.left;
            float targetTY2 = finalWindowDragBounds.top;
            MiuiFreeFormSpringAnimation tXSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, startTX, f, 3947.8f, 0.99f, MiuiFreeformPinManagerService.EDGE_AREA, 2);
            MiuiFreeFormSpringAnimation tYSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, startTY, targetTY2, 3947.8f, 0.99f, MiuiFreeformPinManagerService.EDGE_AREA, 3);
            MiuiFreeFormSpringAnimation scaleXSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, startScaleX, startScaleX, 438.65f, 0.9f, MiuiFreeformPinManagerService.EDGE_AREA, 5);
            MiuiFreeFormSpringAnimation scaleYSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, startScaleY, startScaleY, 438.65f, 0.9f, MiuiFreeformPinManagerService.EDGE_AREA, 6);
            MiuiFreeFormSpringAnimation alphaSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, 1.0f, 1.0f, 631.7f, 0.7f, MiuiFreeformPinManagerService.EDGE_AREA, 1).setMaxValue(1.0f).setMinValue(MiuiFreeformPinManagerService.EDGE_AREA);
            animalLock = new MiuiFreeFormGestureAnimator.AnimalLock(stack);
            this.mFreeFormWindowMotionHelper.mStackLocks.put(stack, animalLock);
            animalLock.mTranslateXAnimation = tXSpringAnimation;
            animalLock.mTranslateYAnimation = tYSpringAnimation;
            animalLock.mScaleXAnimation = scaleXSpringAnimation;
            animalLock.mScaleYAnimation = scaleYSpringAnimation;
            animalLock.mAlphaAnimation = alphaSpringAnimation;
            animationType2 = animationType;
        } else {
            if (MiuiFreeFormGestureController.DEBUG) {
                Slog.d(TAG, "translateAnimal update");
            }
            float targetTX2 = finalWindowDragBounds.left;
            float targetTY3 = finalWindowDragBounds.top;
            float targetWidthScale = stack.mStackControlInfo.mSmallWindowTargetWScale;
            float targetHeightScale = stack.mStackControlInfo.mSmallWindowTargetHScale;
            float targetAlpha = 1.0f;
            if (this.mFreeFormSmallWindowMotionHelper.isUpToExit()) {
                targetWidthScale *= 0.85f;
                targetHeightScale *= 0.85f;
                targetAlpha = 0.7f;
                if (isUp) {
                    targetWidthScale = MiuiFreeformPinManagerService.EDGE_AREA;
                    targetHeightScale = MiuiFreeformPinManagerService.EDGE_AREA;
                    targetAlpha = MiuiFreeformPinManagerService.EDGE_AREA;
                }
            }
            animationType2 = animationType;
            if (animationType2 == 27 && stack.inPinMode() && isUp) {
                float[] predictXY = MiuiMultiWindowUtils.getPredictXY(x, y, xVelocity, yVelocity, 1.0f);
                float targetX = predictXY[0];
                float targetY2 = Math.max(MiuiMultiWindowUtils.applyDip2Px(35.0f), Math.min(MiuiMultiWindowUtils.getPredictY(x, y, predictXY[0], predictXY[1], this.mListener.mScreenWidth), (this.mListener.mScreenHeight - MiuiMultiWindowUtils.applyDip2Px(35.0f)) - ((int) MiuiMultiWindowUtils.applyDip2Px(64.0f))));
                double speed = MiuiMultiWindowUtils.mergeXY(xVelocity, yVelocity);
                if (speed < MiuiMultiWindowUtils.applyDip2Px(363.64f)) {
                    float targetY3 = Math.max(MiuiMultiWindowUtils.applyDip2Px(35.0f), Math.min(y, (this.mListener.mScreenHeight - MiuiMultiWindowUtils.applyDip2Px(35.0f)) - ((int) MiuiMultiWindowUtils.applyDip2Px(64.0f))));
                    targetY = targetY3;
                } else {
                    targetY = targetY2;
                }
                if (targetX > this.mListener.mScreenWidth / 2) {
                    i = (int) ((this.mListener.mScreenWidth - ((int) MiuiMultiWindowUtils.applyDip2Px(24.0f))) + 0.5f);
                } else {
                    i = (int) (-((((int) MiuiMultiWindowUtils.applyDip2Px(64.0f)) - ((int) MiuiMultiWindowUtils.applyDip2Px(24.0f))) + 0.5f));
                }
                int left = i;
                int top = (int) ((targetY - (((int) MiuiMultiWindowUtils.applyDip2Px(64.0f)) / 2)) + 0.5f);
                float targetY4 = MiuiMultiWindowUtils.applyDip2Px(64.0f);
                Rect finalBounds = new Rect(left, top, ((int) targetY4) + left, ((int) MiuiMultiWindowUtils.applyDip2Px(64.0f)) + top);
                this.mListener.mGestureController.mMiuiFreeformPinManagerService.adjustFloatingWindowPosIfNeed(this.mListener.mGestureController.mMiuiFreeFormManagerService.getCurrPinFloatingWindowPos(true, true), finalBounds);
                this.mListener.mGestureController.mMiuiFreeformPinManagerService.updatePinFloatingWindowPos(stack, finalBounds);
                this.mListener.mGestureController.mMiuiFreeformPinManagerService.setPinFloatingWindowAnimationInfo(stack, false);
                stack.mIsPinFloatingWindowPosInit = true;
                ActivityRecord activityRecord = stack.mLastIconLayerWindowToken;
                if (activityRecord == null || activityRecord.mFloatWindwoIconSurfaceControl == null) {
                    animalLock2 = animalLock3;
                    stack.setInPinMode(false);
                    if (this.mFreeFormSmallWindowMotionHelper.isUpToExit()) {
                        animationType3 = 23;
                    } else {
                        animationType3 = 9;
                    }
                    Slog.d(TAG, "skip slide mini freeform to pin animation activityRecord: " + activityRecord);
                    animationType2 = animationType3;
                    targetTX = targetTX2;
                    targetTY = targetTY3;
                } else {
                    targetAlpha = MiuiFreeformPinManagerService.EDGE_AREA;
                    targetWidthScale = stack.mStackControlInfo.mSmallWindowTargetWScale * 0.7f;
                    targetHeightScale = stack.mStackControlInfo.mSmallWindowTargetHScale * 0.7f;
                    float targetTX3 = stack.mPinFloatingWindowPos.centerX() - (((stack.mTask.getBounds().width() * stack.mFreeFormScale) * targetWidthScale) / 2.0f);
                    animalLock2 = animalLock3;
                    slideMiniFreeformToPin(stack, animalLock3, targetWidthScale, targetHeightScale, activityRecord);
                    targetTX = targetTX3;
                    targetTY = stack.mPinFloatingWindowPos.centerY() - (((stack.mTask.getBounds().height() * stack.mFreeFormScale) * targetHeightScale) / 2.0f);
                }
            } else {
                animalLock2 = animalLock3;
                targetTX = targetTX2;
                targetTY = targetTY3;
            }
            animalLock2.mTranslateXAnimation.setStartValue(animalLock2.mTranslateXAnimation.getCurrentValue());
            animalLock2.mTranslateXAnimation.animateToFinalPosition(targetTX);
            animalLock2.mTranslateYAnimation.setStartValue(animalLock2.mTranslateYAnimation.getCurrentValue());
            animalLock2.mTranslateYAnimation.animateToFinalPosition(targetTY);
            animalLock2.mScaleXAnimation.setStartValue(animalLock2.mScaleXAnimation.getCurrentValue());
            animalLock2.mScaleXAnimation.animateToFinalPosition(targetWidthScale);
            animalLock2.mScaleYAnimation.setStartValue(animalLock2.mScaleYAnimation.getCurrentValue());
            animalLock2.mScaleYAnimation.animateToFinalPosition(targetHeightScale);
            animalLock2.mAlphaAnimation.setStartValue(animalLock2.mAlphaAnimation.getCurrentValue());
            animalLock2.mAlphaAnimation.animateToFinalPosition(targetAlpha);
            if (isUp) {
                if (animationType2 == 27 && stack.inPinMode()) {
                    animalLock2.mTranslateXAnimation.getSpring().setStiffness(109.7f);
                    animalLock2.mTranslateXAnimation.getSpring().setDampingRatio(0.78f);
                    animalLock2.mTranslateYAnimation.getSpring().setStiffness(109.7f);
                    animalLock2.mTranslateYAnimation.getSpring().setDampingRatio(0.78f);
                    animalLock2.mScaleXAnimation.getSpring().setStiffness(322.27f);
                    animalLock2.mScaleXAnimation.getSpring().setDampingRatio(0.9f);
                    animalLock2.mScaleYAnimation.getSpring().setStiffness(322.27f);
                    animalLock2.mScaleYAnimation.getSpring().setDampingRatio(0.9f);
                    animalLock2.mAlphaAnimation.getSpring().setStiffness(322.27f);
                    animalLock2.mAlphaAnimation.getSpring().setDampingRatio(0.9f);
                } else {
                    animalLock2.mTranslateXAnimation.getSpring().setStiffness(130.5f);
                    animalLock2.mTranslateXAnimation.getSpring().setDampingRatio(0.75f);
                    animalLock2.mTranslateYAnimation.getSpring().setStiffness(130.5f);
                    animalLock2.mTranslateYAnimation.getSpring().setDampingRatio(0.75f);
                    animalLock2.mScaleXAnimation.getSpring().setStiffness(130.5f);
                    animalLock2.mScaleXAnimation.getSpring().setDampingRatio(0.75f);
                    animalLock2.mScaleYAnimation.getSpring().setStiffness(130.5f);
                    animalLock2.mScaleYAnimation.getSpring().setDampingRatio(0.75f);
                    animalLock2.mAlphaAnimation.getSpring().setStiffness(130.5f);
                    animalLock2.mAlphaAnimation.getSpring().setDampingRatio(0.75f);
                }
            } else {
                animalLock2.mTranslateXAnimation.getSpring().setStiffness(3947.8f);
                animalLock2.mTranslateXAnimation.getSpring().setDampingRatio(0.99f);
                animalLock2.mTranslateYAnimation.getSpring().setStiffness(3947.8f);
                animalLock2.mTranslateYAnimation.getSpring().setDampingRatio(0.99f);
                animalLock2.mScaleXAnimation.getSpring().setStiffness(438.65f);
                animalLock2.mScaleXAnimation.getSpring().setDampingRatio(0.9f);
                animalLock2.mScaleYAnimation.getSpring().setStiffness(438.65f);
                animalLock2.mScaleYAnimation.getSpring().setDampingRatio(0.9f);
                animalLock2.mAlphaAnimation.getSpring().setStiffness(631.7f);
                animalLock2.mAlphaAnimation.getSpring().setDampingRatio(0.7f);
            }
            animalLock = animalLock2;
        }
        if (isUp) {
            animalLock.mTranslateXAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.TRANSLATE_X_END_LISTENER));
            animalLock.mTranslateYAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.TRANSLATE_Y_END_LISTENER));
            animalLock.mScaleXAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.SCALE_X_END_LISTENER));
            animalLock.mScaleYAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.SCALE_Y_END_LISTENER));
            animalLock.mAlphaAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.ALPHA_END_LISTENER));
        }
        animalLock.start(animationType2);
    }

    private void slideFreeformToPin(MiuiFreeFormActivityStack stack, MiuiFreeFormGestureAnimator.AnimalLock animalLock, float targetHeightScale, float targetWidthScale, ActivityRecord activityRecord) {
        float startRoundCorner = MiuiMultiWindowUtils.getFreeformRoundCorner(this.mListener.mService.mContext);
        float startClipX = stack.mTask.getBounds().width() * stack.mFreeFormScale;
        float startClipY = stack.mTask.getBounds().height() * stack.mFreeFormScale;
        float startShadowAlpha = MiuiMultiWindowUtils.MIUI_FREEFORM_AMBIENT_COLOR[3];
        float targetRoundCornor = ((int) MiuiMultiWindowUtils.applyDip2Px(18.18f)) / targetWidthScale;
        float targetClipWidth = ((int) MiuiMultiWindowUtils.applyDip2Px(64.0f)) / targetWidthScale;
        float targetClipHeight = ((int) MiuiMultiWindowUtils.applyDip2Px(64.0f)) / targetHeightScale;
        stack.mIsRunningPinAnim = true;
        this.mFreeFormWindowMotionHelper.drawIcon(activityRecord.mFloatWindwoIconSurfaceControl, stack, targetWidthScale, targetHeightScale);
        this.mListener.mGestureAnimator.setWindowCropInTransaction(stack, new Rect((int) ((((stack.mTask.getBounds().width() * stack.mFreeFormScale) / 2.0f) - (startClipX / 2.0f)) + 0.5f), (int) ((((stack.mTask.getBounds().height() * stack.mFreeFormScale) / 2.0f) - (startClipY / 2.0f)) + 0.5f), (int) (((stack.mTask.getBounds().width() * stack.mFreeFormScale) / 2.0f) + (startClipX / 2.0f) + 0.5f), (int) (((stack.mTask.getBounds().height() * stack.mFreeFormScale) / 2.0f) + (startClipY / 2.0f) + 0.5f)));
        this.mListener.mGestureAnimator.setCornerRadiusInTransaction(stack, startRoundCorner);
        this.mListener.mGestureController.mMiuiFreeFormShadowHelper.setShadowSettingsInTransaction(stack, 1, 400.0f, MiuiMultiWindowUtils.MIUI_FREEFORM_AMBIENT_COLOR, MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA, 1);
        synchronized (this.mListener.mService.mGlobalLock) {
            try {
                this.mListener.mGestureAnimator.setCornerRadiusInTransactionForSurfaceControl(stack.mTask.mSurfaceControl, MiuiFreeformPinManagerService.EDGE_AREA);
                this.mListener.mGestureController.mMiuiFreeFormShadowHelper.setShadowSettingsInTransactionForSurfaceControl(stack.mTask.mSurfaceControl, 0, MiuiFreeformPinManagerService.EDGE_AREA, MiuiMultiWindowUtils.MIUI_FREEFORM_RESET_COLOR, MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA, 1);
            } catch (Throwable th) {
                th = th;
                while (true) {
                    try {
                        break;
                    } catch (Throwable th2) {
                        th = th2;
                    }
                }
                throw th;
            }
        }
        this.mListener.mGestureAnimator.applyTransaction();
        MiuiFreeFormSpringAnimation alphaSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, 1.0f, MiuiFreeformPinManagerService.EDGE_AREA, 631.7f, 0.7f, MiuiFreeformPinManagerService.EDGE_AREA, 1).setMaxValue(1.0f).setMinValue(MiuiFreeformPinManagerService.EDGE_AREA);
        MiuiFreeFormSpringAnimation roundCornerSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, startRoundCorner, targetRoundCornor, 322.27f, 0.9f, MiuiFreeformPinManagerService.EDGE_AREA, 7);
        MiuiFreeFormSpringAnimation clipWidthXSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, startClipX, targetClipWidth, 322.27f, 0.9f, MiuiFreeformPinManagerService.EDGE_AREA, 8).setMaxValue(startClipX);
        MiuiFreeFormSpringAnimation clipHeightYSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, startClipY, targetClipHeight, 322.27f, 0.9f, MiuiFreeformPinManagerService.EDGE_AREA, 9).setMaxValue(startClipY);
        MiuiFreeFormSpringAnimation shadowAlphaSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, startShadowAlpha, MiuiFreeformPinManagerService.EDGE_AREA, 322.27f, 0.9f, MiuiFreeformPinManagerService.EDGE_AREA, 10).setMaxValue(1.0f).setMinValue(MiuiFreeformPinManagerService.EDGE_AREA);
        animalLock.mAlphaAnimation = alphaSpringAnimation;
        animalLock.mRoundCornorAnimation = roundCornerSpringAnimation;
        animalLock.mClipWidthXAnimation = clipWidthXSpringAnimation;
        animalLock.mClipHeightYAnimation = clipHeightYSpringAnimation;
        animalLock.mShadowAlphaAnimation = shadowAlphaSpringAnimation;
        animalLock.mAlphaAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.ALPHA_END_LISTENER));
        animalLock.mRoundCornorAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.ROUNDCORNER_END_LISTENER));
        animalLock.mClipWidthXAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.CLIP_WIDTH_X_END_LISTENER));
        animalLock.mClipHeightYAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.CLIP_HEIGHT_Y_END_LISTENER));
        animalLock.mShadowAlphaAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.SHADOW_ALPHA_END_LISTENER));
        stack.mWindowRoundCorner = startRoundCorner;
        stack.mWindowScaleX = 1.0f;
        stack.mWindowScaleY = 1.0f;
    }

    private void slideMiniFreeformToPin(MiuiFreeFormActivityStack stack, MiuiFreeFormGestureAnimator.AnimalLock animalLock, float targetWidthScale, float targetHeightScale, ActivityRecord activityRecord) {
        this.mListener.mGestureController.mMiuiFreeFormFlashBackHelper.stopFlashBackService(this.mListener.mService.mContext, this.mListener.mHandler);
        float maxScale = Math.max(stack.mStackControlInfo.mSmallWindowTargetHScale, stack.mStackControlInfo.mSmallWindowTargetWScale);
        float miniCornerRaduis = MiuiMultiWindowUtils.getSmallFreeformRoundCorner(this.mListener.mService.mContext) / maxScale;
        float startClipWidth = stack.mTask.getBounds().width() * stack.mFreeFormScale;
        float startClipHeight = stack.mTask.getBounds().height() * stack.mFreeFormScale;
        float startShadowAlpha = MiuiMultiWindowUtils.MIUI_FREEFORM_AMBIENT_COLOR[3];
        float targetRoundCornor = ((int) MiuiMultiWindowUtils.applyDip2Px(18.18f)) / targetWidthScale;
        float targetClipWidth = ((int) MiuiMultiWindowUtils.applyDip2Px(64.0f)) / targetWidthScale;
        float targetClipHeight = ((int) MiuiMultiWindowUtils.applyDip2Px(64.0f)) / targetHeightScale;
        stack.mIsRunningPinAnim = true;
        this.mFreeFormWindowMotionHelper.drawIcon(activityRecord.mFloatWindwoIconSurfaceControl, stack, targetWidthScale, targetHeightScale);
        this.mListener.mGestureAnimator.setWindowCropInTransaction(stack, new Rect((int) ((((stack.mTask.getBounds().width() * stack.mFreeFormScale) / 2.0f) - (startClipWidth / 2.0f)) + 0.5f), (int) ((((stack.mTask.getBounds().height() * stack.mFreeFormScale) / 2.0f) - (startClipHeight / 2.0f)) + 0.5f), (int) (((stack.mTask.getBounds().width() * stack.mFreeFormScale) / 2.0f) + (startClipWidth / 2.0f) + 0.5f), (int) (((stack.mTask.getBounds().height() * stack.mFreeFormScale) / 2.0f) + (startClipHeight / 2.0f) + 0.5f)));
        this.mListener.mGestureAnimator.setCornerRadiusInTransaction(stack, miniCornerRaduis);
        this.mListener.mGestureController.mMiuiFreeFormShadowHelper.setShadowSettingsInTransaction(stack, 1, 400.0f, MiuiMultiWindowUtils.MIUI_FREEFORM_AMBIENT_COLOR, MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA, 1);
        synchronized (this.mListener.mService.mGlobalLock) {
            try {
                this.mListener.mGestureAnimator.setCornerRadiusInTransactionForSurfaceControl(stack.mTask.mSurfaceControl, MiuiFreeformPinManagerService.EDGE_AREA);
                this.mListener.mGestureController.mMiuiFreeFormShadowHelper.setShadowSettingsInTransactionForSurfaceControl(stack.mTask.mSurfaceControl, 0, MiuiFreeformPinManagerService.EDGE_AREA, MiuiMultiWindowUtils.MIUI_FREEFORM_RESET_COLOR, MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA, 1);
            } catch (Throwable th) {
                th = th;
                while (true) {
                    try {
                        break;
                    } catch (Throwable th2) {
                        th = th2;
                    }
                }
                throw th;
            }
        }
        this.mListener.mGestureAnimator.applyTransaction();
        MiuiFreeFormSpringAnimation clipWidthXSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, startClipWidth, targetClipWidth, 322.27f, 0.9f, MiuiFreeformPinManagerService.EDGE_AREA, 8);
        MiuiFreeFormSpringAnimation clipHeightYSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, startClipHeight, targetClipHeight, 322.27f, 0.9f, MiuiFreeformPinManagerService.EDGE_AREA, 9);
        MiuiFreeFormSpringAnimation roundCornerSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, miniCornerRaduis, targetRoundCornor, 322.27f, 0.9f, MiuiFreeformPinManagerService.EDGE_AREA, 7);
        MiuiFreeFormSpringAnimation shadowAlphaSpringAnimation = this.mListener.mGestureAnimator.createSpringAnimation(stack, startShadowAlpha, MiuiFreeformPinManagerService.EDGE_AREA, 322.27f, 0.9f, MiuiFreeformPinManagerService.EDGE_AREA, 10).setMaxValue(1.0f).setMinValue(MiuiFreeformPinManagerService.EDGE_AREA);
        animalLock.mClipWidthXAnimation = clipWidthXSpringAnimation;
        animalLock.mClipHeightYAnimation = clipHeightYSpringAnimation;
        animalLock.mRoundCornorAnimation = roundCornerSpringAnimation;
        animalLock.mShadowAlphaAnimation = shadowAlphaSpringAnimation;
        animalLock.mClipWidthXAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.CLIP_WIDTH_X_END_LISTENER));
        animalLock.mClipHeightYAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.CLIP_HEIGHT_Y_END_LISTENER));
        animalLock.mRoundCornorAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.ROUNDCORNER_END_LISTENER));
        animalLock.mShadowAlphaAnimation.addEndListener(this.mListener.mGestureAnimator.createAnimationEndListener(MiuiFreeFormGestureAnimator.SHADOW_ALPHA_END_LISTENER));
        stack.mWindowRoundCorner = miniCornerRaduis;
        stack.mWindowScaleX = stack.mStackControlInfo.mSmallWindowTargetWScale;
        stack.mWindowScaleY = stack.mStackControlInfo.mSmallWindowTargetHScale;
    }
}
