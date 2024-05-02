package com.android.server.wm;

import android.animation.Animator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.graphics.Rect;
import android.view.SurfaceControl;
import android.view.SurfaceSession;
import com.android.server.wm.MiuiCvwGestureController;
/* loaded from: classes.dex */
public class MiuiCvwBackgroundLayer {
    private static final long DEFAULT_ANIM_DURATION = 150;
    private static final String DIM_ALPHA = "alpha";
    private static final String DIM_BLUR = "blur";
    private static final int DIM_HIDE = 1;
    private static final int DIM_REMOVE = 2;
    private static final int DIM_SHOW = 0;
    private static final int MAX_BLUR_RADIUS = 80;
    private static final String TAG = MiuiCvwBackgroundLayer.class.getSimpleName();
    private static final int TIME_HIDE_WALLPAPER_DEFAULT = 800;
    private final MiuiCvwGestureController mController;
    private float mDimAlpha;
    private ValueAnimator mDimAnim;
    private int mDimBlur;
    private DimLayer mDimLayer;
    private ValueAnimator mRemoveDimAnim;
    private int mTimeHideWallpaper = 800;
    private final SurfaceControl.Transaction mTransaction = new SurfaceControl.Transaction();
    private WindowState mWindowState;

    public MiuiCvwBackgroundLayer(MiuiCvwGestureController controller) {
        this.mController = controller;
    }

    public void createLayer(Task task) {
        TaskDisplayArea displayArea;
        SurfaceControl parent;
        ActivityRecord activityRecord;
        WindowState windowState;
        if (task == null || (displayArea = task.getDisplayArea()) == null || (parent = displayArea.getSurfaceControl()) == null || (activityRecord = task.getTopVisibleActivity()) == null || (windowState = activityRecord.findMainWindow()) == null) {
            return;
        }
        this.mTimeHideWallpaper = 800;
        if ((windowState.getAttrs().flags & 1048576) == 0) {
            this.mWindowState = windowState;
        }
        if (isSupportDim()) {
            if (this.mDimLayer == null) {
                this.mDimLayer = new DimLayer();
            }
            this.mDimLayer.setDimCrop(this.mController.mDefaultGestureHandler.mScreenWidth, this.mController.mDefaultGestureHandler.mScreenHeight);
            this.mDimLayer.createDimLayer(parent);
        }
    }

    public void updateLayerStatus(SurfaceControl parent, Rect scale, Rect src) {
        WindowState windowState = this.mWindowState;
        if (windowState != null && !windowState.mForceShowWallpaper) {
            this.mWindowState.mForceShowWallpaper = true;
            synchronized (this.mController.mWmService.mAtmService.mGlobalLock) {
                this.mWindowState.getDisplayContent().mWallpaperController.adjustWallpaperWindows();
            }
        }
        if (isSupportDim() && this.mDimLayer != null) {
            float width = (((scale.width() * scale.height()) * 1.0f) / (src.width() * src.height())) * 0.5f;
            this.mDimAlpha = width;
            this.mDimBlur = 80;
            dimBelow(width, 80, parent);
        }
    }

    void dimBelow(float alpha, int blur, SurfaceControl relate) {
        this.mDimLayer.dim(alpha, blur, relate, false);
    }

    public void removeLayer(MiuiFreeFormActivityStack stack) {
        ValueAnimator valueAnimator = this.mRemoveDimAnim;
        if (valueAnimator != null && valueAnimator.isRunning()) {
            MiuiCvwGestureController.Slog.d(TAG, "removing dim layer");
            return;
        }
        if (isSupportDim() && this.mDimLayer != null) {
            WindowState windowState = this.mWindowState;
            if (windowState != null && windowState.mForceShowWallpaper) {
                final WindowState windowState2 = this.mWindowState;
                this.mWindowState = null;
                int timeHideWallpaper = this.mTimeHideWallpaper;
                this.mController.mHandler.postDelayed(new Runnable() { // from class: com.android.server.wm.MiuiCvwBackgroundLayer$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        MiuiCvwBackgroundLayer.this.m1519x98125dae(windowState2);
                    }
                }, timeHideWallpaper);
            }
            this.mDimLayer.removeDimLayer(stack);
        }
        this.mWindowState = null;
    }

    /* renamed from: lambda$removeLayer$0$com-android-server-wm-MiuiCvwBackgroundLayer */
    public /* synthetic */ void m1519x98125dae(WindowState windowState) {
        if (windowState == this.mWindowState) {
            return;
        }
        windowState.mForceShowWallpaper = false;
        synchronized (this.mController.mWmService.mAtmService.mGlobalLock) {
            windowState.getDisplayContent().mWallpaperController.adjustWallpaperWindows();
        }
    }

    public void adjustHideWallpaperTime(boolean actionCancel) {
        if (actionCancel && this.mWindowState != null) {
            this.mTimeHideWallpaper = 0;
        }
    }

    private boolean isSupportDim() {
        return true;
    }

    private boolean isSupportBlur() {
        return true;
    }

    /* loaded from: classes.dex */
    public class DimLayer {
        private static final String TAG = "DimLayer";
        private int mDimCropHeight;
        private int mDimCropWidth;
        private SurfaceControl mDimLayerLeash;
        private final Object mLock;

        private DimLayer() {
            MiuiCvwBackgroundLayer.this = r1;
            this.mLock = new Object();
        }

        public SurfaceControl getDimLayerLeash() {
            return this.mDimLayerLeash;
        }

        public void setDimCrop(int displayWidth, int displayHeight) {
            this.mDimCropWidth = displayWidth;
            this.mDimCropHeight = displayHeight;
        }

        public void createDimLayer(SurfaceControl surfaceControl) {
            synchronized (this.mLock) {
                removeDimImmediately(this.mDimLayerLeash);
                this.mDimLayerLeash = makeDimLayer(surfaceControl);
                MiuiCvwGestureController.Slog.d(TAG, "createDimLayer");
            }
        }

        public void dim(float alpha, int blurRadius, SurfaceControl relate, boolean isDimSelf) {
            synchronized (this.mLock) {
                try {
                    if (surfaceValid(this.mDimLayerLeash) && surfaceValid(relate)) {
                        float currentAlpha = Math.min(1.0f, Math.max((float) MiuiFreeformPinManagerService.EDGE_AREA, alpha));
                        if (currentAlpha > MiuiFreeformPinManagerService.EDGE_AREA) {
                            MiuiCvwBackgroundLayer.this.mTransaction.show(this.mDimLayerLeash);
                        } else {
                            MiuiCvwBackgroundLayer.this.mTransaction.hide(this.mDimLayerLeash);
                        }
                        MiuiCvwBackgroundLayer.this.mTransaction.setRelativeLayer(this.mDimLayerLeash, relate, isDimSelf ? 1 : -1);
                        MiuiCvwBackgroundLayer.this.mTransaction.setWindowCrop(this.mDimLayerLeash, this.mDimCropWidth, this.mDimCropHeight);
                        MiuiCvwBackgroundLayer.this.mTransaction.setAlpha(this.mDimLayerLeash, currentAlpha);
                        MiuiCvwBackgroundLayer.this.mTransaction.setBackgroundBlurRadius(this.mDimLayerLeash, blurRadius);
                        MiuiCvwBackgroundLayer.this.mTransaction.apply();
                        MiuiCvwGestureController.Slog.d(TAG, "dim mCurrentAlpha=" + currentAlpha + "   isDimSelf=" + isDimSelf);
                    }
                } catch (Exception e) {
                    MiuiCvwGestureController.Slog.e(TAG, "dim error", e);
                }
            }
        }

        void removeDimLayer(MiuiFreeFormActivityStack stack) {
            SurfaceControl surfaceControl = this.mDimLayerLeash;
            if (surfaceControl == null || !surfaceControl.isValid()) {
                return;
            }
            if (stack != null) {
                MiuiCvwBackgroundLayer.this.removeDim(stack);
            } else {
                removeDimImmediately(this.mDimLayerLeash);
            }
        }

        void removeDimImmediately(SurfaceControl dimLayerLeash) {
            synchronized (this.mLock) {
                if (dimLayerLeash != null) {
                    try {
                        if (dimLayerLeash.isValid()) {
                            MiuiCvwBackgroundLayer.this.mTransaction.remove(dimLayerLeash);
                            MiuiCvwBackgroundLayer.this.mTransaction.apply();
                            MiuiCvwGestureController.Slog.d(TAG, "removeDimLayer");
                        }
                    } catch (Exception e) {
                        MiuiCvwGestureController.Slog.e(TAG, "removeDimLayer error", e);
                    }
                }
            }
        }

        private SurfaceControl makeDimLayer(SurfaceControl parent) {
            if (parent != null) {
                SurfaceControl surfaceControl = new SurfaceControl.Builder(new SurfaceSession()).setParent(parent).setColorLayer().setName("CVW Dim Layer").build();
                return surfaceControl;
            }
            return null;
        }

        boolean surfaceValid(SurfaceControl surfaceControl) {
            return surfaceControl != null && surfaceControl.isValid();
        }
    }

    public void showDim(MiuiFreeFormActivityStack stack) {
        if (stack == null) {
            return;
        }
        ValueAnimator valueAnimator = this.mDimAnim;
        if (valueAnimator != null && valueAnimator.isRunning()) {
            this.mDimAnim.cancel();
        }
        ValueAnimator ofPropertyValuesHolder = ValueAnimator.ofPropertyValuesHolder(PropertyValuesHolder.ofFloat(DIM_ALPHA, this.mDimAlpha, 0.5f), PropertyValuesHolder.ofInt(DIM_BLUR, this.mDimBlur, 80));
        this.mDimAnim = ofPropertyValuesHolder;
        ofPropertyValuesHolder.setDuration(DEFAULT_ANIM_DURATION);
        this.mDimAnim.removeAllUpdateListeners();
        this.mDimAnim.addUpdateListener(new CVWAnimatorUpdateListener(stack.getFreeFormConrolSurface()));
        this.mDimAnim.addListener(new CvwAnimatorListener(0));
        this.mDimAnim.start();
    }

    public void hideDim(MiuiFreeFormActivityStack stack) {
        if (stack == null) {
            return;
        }
        ValueAnimator valueAnimator = this.mDimAnim;
        if (valueAnimator != null && valueAnimator.isRunning()) {
            this.mDimAnim.cancel();
        }
        ValueAnimator ofPropertyValuesHolder = ValueAnimator.ofPropertyValuesHolder(PropertyValuesHolder.ofFloat(DIM_ALPHA, this.mDimAlpha, MiuiFreeformPinManagerService.EDGE_AREA), PropertyValuesHolder.ofInt(DIM_BLUR, this.mDimBlur, 0));
        this.mDimAnim = ofPropertyValuesHolder;
        ofPropertyValuesHolder.setDuration(DEFAULT_ANIM_DURATION);
        this.mDimAnim.removeAllUpdateListeners();
        this.mDimAnim.addUpdateListener(new CVWAnimatorUpdateListener(stack.getFreeFormConrolSurface()));
        this.mDimAnim.addListener(new CvwAnimatorListener(1));
        this.mDimAnim.start();
    }

    void removeDim(MiuiFreeFormActivityStack stack) {
        ValueAnimator valueAnimator = this.mRemoveDimAnim;
        if (valueAnimator != null && valueAnimator.isRunning()) {
            this.mRemoveDimAnim.cancel();
        }
        ValueAnimator ofPropertyValuesHolder = ValueAnimator.ofPropertyValuesHolder(PropertyValuesHolder.ofFloat(DIM_ALPHA, this.mDimAlpha, MiuiFreeformPinManagerService.EDGE_AREA), PropertyValuesHolder.ofInt(DIM_BLUR, this.mDimBlur, 0));
        this.mRemoveDimAnim = ofPropertyValuesHolder;
        ofPropertyValuesHolder.setDuration(300L);
        this.mRemoveDimAnim.removeAllUpdateListeners();
        this.mRemoveDimAnim.addUpdateListener(new CVWAnimatorUpdateListener(stack.getFreeFormConrolSurface()));
        this.mRemoveDimAnim.addListener(new CvwAnimatorListener(2));
        this.mRemoveDimAnim.start();
    }

    /* loaded from: classes.dex */
    public class CVWAnimatorUpdateListener implements ValueAnimator.AnimatorUpdateListener {
        SurfaceControl relate;

        CVWAnimatorUpdateListener(SurfaceControl relate) {
            MiuiCvwBackgroundLayer.this = r1;
            this.relate = relate;
        }

        @Override // android.animation.ValueAnimator.AnimatorUpdateListener
        public void onAnimationUpdate(ValueAnimator animation) {
            MiuiCvwBackgroundLayer.this.mDimAlpha = ((Float) animation.getAnimatedValue(MiuiCvwBackgroundLayer.DIM_ALPHA)).floatValue();
            MiuiCvwBackgroundLayer.this.mDimBlur = ((Integer) animation.getAnimatedValue(MiuiCvwBackgroundLayer.DIM_BLUR)).intValue();
            MiuiCvwBackgroundLayer miuiCvwBackgroundLayer = MiuiCvwBackgroundLayer.this;
            miuiCvwBackgroundLayer.dimBelow(miuiCvwBackgroundLayer.mDimAlpha, MiuiCvwBackgroundLayer.this.mDimBlur, this.relate);
        }
    }

    /* loaded from: classes.dex */
    public class CvwAnimatorListener implements Animator.AnimatorListener {
        private final int mAnimationFlag;

        CvwAnimatorListener(int animationType) {
            MiuiCvwBackgroundLayer.this = r1;
            this.mAnimationFlag = animationType;
        }

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationStart(Animator animation) {
        }

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationEnd(Animator animation) {
            if (this.mAnimationFlag == 2) {
                MiuiCvwBackgroundLayer.this.mDimLayer.removeDimImmediately(MiuiCvwBackgroundLayer.this.mDimLayer.getDimLayerLeash());
            }
        }

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationCancel(Animator animation) {
            if (this.mAnimationFlag == 2) {
                MiuiCvwBackgroundLayer.this.mDimLayer.removeDimImmediately(MiuiCvwBackgroundLayer.this.mDimLayer.getDimLayerLeash());
            }
        }

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationRepeat(Animator animation) {
        }
    }
}
