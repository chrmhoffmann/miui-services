package com.android.server.wm;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.ServiceManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.MiuiMultiWindowUtils;
import android.util.Slog;
import android.util.TypedValue;
import android.view.SurfaceControl;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.Transformation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import java.util.Collection;
import java.util.HashSet;
import miuix.animation.Folme;
import miuix.animation.IStateStyle;
import miuix.animation.base.AnimConfig;
import miuix.animation.controller.AnimState;
import miuix.animation.listener.TransitionListener;
import miuix.animation.listener.UpdateInfo;
/* loaded from: classes.dex */
public class MiuiFreeFormOverlayView extends FrameLayout {
    private static final String ALPHA = "ALPHA";
    public static final int ANIMATION_ALPHA = 0;
    public static final int ANIMATION_BORDER_HIDE = 3;
    public static final int ANIMATION_BORDER_OPEN = 2;
    public static final int ANIMATION_CONTENT_OPEN = 1;
    public static final int ANIMATION_SWITCH_OVERLAY_ALPHA = 5;
    public static final int ANIMATION_SWITCH_OVERLAY_OPEN = 4;
    private static final int HIDDEN_STATE = 0;
    private static final int ICON_W_H_THRESHOLD = (int) TypedValue.applyDimension(1, 69.0f, Resources.getSystem().getDisplayMetrics());
    private static final String SCALE_HEIGTH = "HEIGHT_SCALE";
    private static final String SCALE_WIDTH = "WIDTH_SCALE";
    private static final int SHOWN_STATE = 1;
    private static final String TAG = "OverlayView";
    private static final String TRANSLATE_X = "TRANSLATE_X";
    private static final String TRANSLATE_Y = "TRANSLATE_Y";
    private MiuiFreeFormRoundRectView mBorderView;
    private View mContentView;
    private MiuiFreeFormWindowController mController;
    private AnimConfig mFolmeAnimConfig;
    private IStateStyle mFolmeStyle;
    private MiuiFreeFormActivityStack mMffas;
    private String mPackageName;
    private MiuiFreeFormRadiateImageView mRadiateIconView;
    private float mRadius;
    public volatile boolean mRemoveAnimationToDo;
    private MiuiFreeFormActivityStack mTopffas;
    private Bitmap mIconBmp = null;
    private Rect mStartBounds = new Rect();
    private int ACTION_UNDEFINED = -1;
    public int ACTION_ALPHA_SHOW = 0;
    private int ACTION_ALPHA_HIDE = 1;

    public MiuiFreeFormOverlayView(Context context) {
        super(context);
        init(context);
    }

    public MiuiFreeFormOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MiuiFreeFormOverlayView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        float screenRoundCornerRadiusTop = MiuiFreeFormGestureDetector.getScreenRoundCornerRadiusTop(getContext());
        this.mRadius = screenRoundCornerRadiusTop;
        if (screenRoundCornerRadiusTop == -1.0f) {
            screenRoundCornerRadiusTop = 66.0f;
        }
        this.mRadius = screenRoundCornerRadiusTop;
    }

    public void setController(MiuiFreeFormWindowController controller) {
        this.mController = controller;
        initFolmeConfig();
    }

    public void initFolmeConfig() {
        this.mFolmeAnimConfig = new AnimConfig().setEase(-2, new float[]{0.9f, 0.3f}).addListeners(new TransitionListener[]{new TransitionListener() { // from class: com.android.server.wm.MiuiFreeFormOverlayView.1
            public void onComplete(Object toTag) {
                super.onComplete(toTag);
                if (((Integer) toTag).intValue() == 0) {
                    MiuiFreeFormOverlayView.this.mBorderView.setVisibility(4);
                    MiuiFreeFormOverlayView.this.setVisibility(4);
                }
            }

            public void onUpdate(Object toTag, Collection<UpdateInfo> updateList) {
                int top;
                int right;
                int bottom;
                int left;
                super.onUpdate(toTag, updateList);
                float animationTransX = MiuiFreeformPinManagerService.EDGE_AREA;
                float animationTransY = MiuiFreeformPinManagerService.EDGE_AREA;
                float scaleX = MiuiFreeformPinManagerService.EDGE_AREA;
                float scaleY = MiuiFreeformPinManagerService.EDGE_AREA;
                float alpha = MiuiFreeformPinManagerService.EDGE_AREA;
                UpdateInfo translationXInfo = UpdateInfo.findByName(updateList, "translationX");
                UpdateInfo translationYInfo = UpdateInfo.findByName(updateList, "translationY");
                UpdateInfo scaleXInfo = UpdateInfo.findByName(updateList, "scaleX");
                UpdateInfo scaleYInfo = UpdateInfo.findByName(updateList, "scaleY");
                UpdateInfo alphaInfo = UpdateInfo.findByName(updateList, "alpha");
                if (translationXInfo != null) {
                    animationTransX = ((Float) translationXInfo.getValue(Float.class)).floatValue();
                }
                if (translationYInfo != null) {
                    animationTransY = ((Float) translationYInfo.getValue(Float.class)).floatValue();
                }
                if (scaleXInfo != null) {
                    scaleX = ((Float) scaleXInfo.getValue(Float.class)).floatValue();
                }
                if (scaleYInfo != null) {
                    scaleY = ((Float) scaleYInfo.getValue(Float.class)).floatValue();
                }
                if (alphaInfo != null) {
                    alpha = ((Float) alphaInfo.getValue(Float.class)).floatValue();
                }
                if (!MiuiFreeFormOverlayView.this.mController.mGestureController.mGestureListener.mIsPortrait) {
                    int left2 = (int) (animationTransX - ((MiuiFreeFormOverlayView.this.mController.mScreenLongSide * scaleX) / 2.0f));
                    int top2 = (int) (animationTransY - ((MiuiFreeFormOverlayView.this.mController.mScreenShortSide * scaleY) / 2.0f));
                    int right2 = (int) (((MiuiFreeFormOverlayView.this.mController.mScreenLongSide * scaleX) / 2.0f) + animationTransX);
                    int bottom2 = (int) (((MiuiFreeFormOverlayView.this.mController.mScreenShortSide * scaleY) / 2.0f) + animationTransY);
                    if (right2 <= MiuiFreeFormOverlayView.this.mController.mScreenLongSide - 4) {
                        right = right2;
                    } else {
                        right = MiuiFreeFormOverlayView.this.mController.mScreenLongSide - 4;
                    }
                    if (left2 < 4) {
                        left2 = 4;
                    }
                    if (top2 < 4) {
                        top2 = 4;
                    }
                    if (bottom2 <= MiuiFreeFormOverlayView.this.mController.mScreenShortSide - 4) {
                        bottom = bottom2;
                        top = top2;
                        left = left2;
                    } else {
                        bottom = MiuiFreeFormOverlayView.this.mController.mScreenShortSide - 4;
                        top = top2;
                        left = left2;
                    }
                } else {
                    left = (int) (animationTransX - ((MiuiFreeFormOverlayView.this.mController.mScreenShortSide * scaleX) / 2.0f));
                    top = (int) (animationTransY - ((MiuiFreeFormOverlayView.this.mController.mScreenLongSide * scaleY) / 2.0f));
                    right = (int) (((MiuiFreeFormOverlayView.this.mController.mScreenShortSide * scaleX) / 2.0f) + animationTransX);
                    bottom = (int) (((MiuiFreeFormOverlayView.this.mController.mScreenLongSide * scaleY) / 2.0f) + animationTransY);
                    if (right > MiuiFreeFormOverlayView.this.mController.mScreenShortSide - 4) {
                        right = MiuiFreeFormOverlayView.this.mController.mScreenShortSide - 4;
                    }
                    if (left < 4) {
                        left = 4;
                    }
                    if (top < 4) {
                        top = 4;
                    }
                    if (bottom > MiuiFreeFormOverlayView.this.mController.mScreenLongSide - 4) {
                        bottom = MiuiFreeFormOverlayView.this.mController.mScreenLongSide - 4;
                    }
                }
                Rect bounds = new Rect(left, top, right, bottom);
                Slog.d(MiuiFreeFormOverlayView.TAG, "Folme onAnimationUpdate: bounds:" + bounds + "animationTransX:" + animationTransX + " animationTransY:" + animationTransY + " scaleX:" + scaleX + " scaleY:" + scaleY + " alpha:" + alpha);
                MiuiFreeFormOverlayView.this.mBorderView.setRectBounds(bounds);
                if (((Integer) toTag).intValue() == 0) {
                    MiuiFreeFormOverlayView.this.mBorderView.setAlpha(alpha);
                }
            }
        }});
    }

    @Override // android.view.View
    protected void onConfigurationChanged(Configuration newConfig) {
        int orientation;
        Slog.d(TAG, "onConfigurationChanged ori=" + newConfig.orientation);
        MiuiFreeFormActivityStack miuiFreeFormActivityStack = this.mMffas;
        if (miuiFreeFormActivityStack == null || miuiFreeFormActivityStack.mTask == null) {
            return;
        }
        boolean z = false;
        boolean z2 = newConfig.orientation == 1;
        if (getWidth() < getHeight()) {
            z = true;
        }
        if (z2 == z) {
            return;
        }
        ActivityRecord ar = this.mMffas.mTask.getTopVisibleActivity();
        synchronized (this.mController.mGestureController.mService.mGlobalLock) {
            orientation = this.mMffas.mTask.getOrientation();
            if (ar != null) {
                orientation = ar.getOrientation();
            }
        }
        if (newConfig.orientation == 1) {
            if (TAG.equals(getTag().toString()) || "SwitchOverlayView".equals(getTag().toString())) {
                this.mController.updateOvleryView(1);
                this.mContentView.setX(MiuiFreeformPinManagerService.EDGE_AREA);
                this.mContentView.setY(MiuiFreeformPinManagerService.EDGE_AREA);
                this.mContentView.getLayoutParams().width = -1;
                this.mContentView.getLayoutParams().height = -1;
                Bitmap cover = ((BitmapDrawable) this.mContentView.getBackground()).getBitmap();
                this.mContentView.setBackground(new BitmapDrawable(rotateBitmap(cover, 90.0f)));
                this.mContentView.requestLayout();
                if (MiuiMultiWindowUtils.isOrientationLandscape(orientation)) {
                    this.mRadiateIconView.setRotation(90.0f);
                } else {
                    this.mRadiateIconView.setRotation(MiuiFreeformPinManagerService.EDGE_AREA);
                }
                invalidate();
                if (TAG.equals(getTag().toString())) {
                    MiuiFreeFormWindowController.DropWindowType = -1;
                }
            }
        } else if (newConfig.orientation == 2) {
            if ((TAG.equals(getTag().toString()) && MiuiFreeFormWindowController.DropWindowType == 0) || "SwitchOverlayView".equals(getTag().toString())) {
                this.mController.updateOvleryView(2);
                this.mContentView.setX(MiuiFreeformPinManagerService.EDGE_AREA);
                this.mContentView.setY(MiuiFreeformPinManagerService.EDGE_AREA);
                this.mContentView.getLayoutParams().width = -1;
                this.mContentView.getLayoutParams().height = -1;
                Bitmap cover2 = ((BitmapDrawable) this.mContentView.getBackground()).getBitmap();
                this.mContentView.setBackground(new BitmapDrawable(rotateBitmap(cover2, -90.0f)));
                this.mContentView.requestLayout();
                if (MiuiMultiWindowUtils.isOrientationPortrait(orientation) && !isSupportEmbeddedWindow(this.mMffas.getStackPackageName())) {
                    this.mRadiateIconView.setRotation(-90.0f);
                } else {
                    this.mRadiateIconView.setRotation(MiuiFreeformPinManagerService.EDGE_AREA);
                }
                invalidate();
                if (TAG.equals(getTag().toString()) && MiuiFreeFormWindowController.DropWindowType == 0) {
                    MiuiFreeFormWindowController.DropWindowType = -1;
                }
            }
        }
    }

    private boolean isSupportEmbeddedWindow(String packageName) {
        Object isSupportEmbedded;
        return (ServiceManager.getService("miui_embedding_window") == null || (isSupportEmbedded = MiuiMultiWindowUtils.invoke(ServiceManager.getService("miui_embedding_window"), "isEmbeddingEnabledForPackage", new Object[]{packageName})) == null || !((Boolean) isSupportEmbedded).booleanValue()) ? false : true;
    }

    public void startRemoveOverLayViewAnimation() {
        post(new Runnable() { // from class: com.android.server.wm.MiuiFreeFormOverlayView.2
            @Override // java.lang.Runnable
            public void run() {
                PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat("ALPHA", 1.0f, MiuiFreeformPinManagerService.EDGE_AREA);
                ValueAnimator animator = ValueAnimator.ofPropertyValuesHolder(alpha);
                animator.setInterpolator(new LinearInterpolator());
                animator.setDuration(200L);
                MiuiFreeFormOverlayView miuiFreeFormOverlayView = MiuiFreeFormOverlayView.this;
                animator.addUpdateListener(new AnimatorUpdateListener(animator, 0, miuiFreeFormOverlayView));
                MiuiFreeFormOverlayView miuiFreeFormOverlayView2 = MiuiFreeFormOverlayView.this;
                animator.addListener(new AnimatorListener(0, miuiFreeFormOverlayView2, miuiFreeFormOverlayView2.ACTION_ALPHA_HIDE));
                animator.start();
            }
        });
    }

    public void startRemoveSwitchOverLayViewAnimation() {
        postDelayed(new Runnable() { // from class: com.android.server.wm.MiuiFreeFormOverlayView.3
            @Override // java.lang.Runnable
            public void run() {
                PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat("ALPHA", 1.0f, MiuiFreeformPinManagerService.EDGE_AREA);
                ValueAnimator animator = ValueAnimator.ofPropertyValuesHolder(alpha);
                animator.setInterpolator(new LinearInterpolator());
                animator.setDuration(100L);
                MiuiFreeFormOverlayView miuiFreeFormOverlayView = MiuiFreeFormOverlayView.this;
                animator.addUpdateListener(new AnimatorUpdateListener(animator, 0, miuiFreeFormOverlayView));
                MiuiFreeFormOverlayView miuiFreeFormOverlayView2 = MiuiFreeFormOverlayView.this;
                animator.addListener(new AnimatorListener(0, miuiFreeFormOverlayView2, miuiFreeFormOverlayView2.ACTION_ALPHA_HIDE));
                animator.start();
            }
        }, 200L);
    }

    @Override // android.view.View
    protected void onFinishInflate() {
        super.onFinishInflate();
        Slog.d(TAG, "onFinishInflate");
        this.mRadiateIconView = (MiuiFreeFormRadiateImageView) getChildAt(2);
        this.mContentView = getChildAt(1);
        this.mBorderView = (MiuiFreeFormRoundRectView) getChildAt(0);
        this.mContentView.setVisibility(4);
        this.mBorderView.setVisibility(4);
        this.mRadiateIconView.setVisibility(4);
        setBgRadius(this.mContentView, this.mRadius);
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void onAttachedToWindow() {
        Bitmap taskBlurCover;
        int orientation;
        super.onAttachedToWindow();
        Slog.d(TAG, "onAttachedToWindow");
        try {
            synchronized (this.mController.mGestureController.mGestureListener.mLock) {
                MiuiFreeFormActivityStack clonedCurrentControlActivityStack = null;
                MiuiFreeFormActivityStack currentControlActivityStack = this.mController.mGestureController.mMiuiFreeFormManagerService.getTopFreeFormActivityStack();
                if (currentControlActivityStack != null) {
                    clonedCurrentControlActivityStack = new MiuiFreeFormActivityStack(currentControlActivityStack);
                }
                this.mTopffas = clonedCurrentControlActivityStack;
            }
            if (getContext().getResources().getConfiguration().orientation == 1) {
                taskBlurCover = loadTaskBlurCover(this.mTopffas, this.mController.mScreenShortSide, this.mController.mScreenLongSide);
            } else {
                taskBlurCover = loadTaskBlurCover(this.mTopffas, this.mController.mScreenLongSide, this.mController.mScreenShortSide);
            }
            this.mContentView.setBackground(new BitmapDrawable(taskBlurCover));
            this.mContentView.setVisibility(4);
            MiuiFreeFormActivityStack miuiFreeFormActivityStack = this.mTopffas;
            if (miuiFreeFormActivityStack != null && miuiFreeFormActivityStack.mTask != null) {
                ActivityRecord ar = this.mTopffas.mTask.getTopVisibleActivity();
                synchronized (this.mController.mGestureController.mService.mGlobalLock) {
                    orientation = this.mTopffas.mTask.getOrientation();
                    if (ar != null) {
                        orientation = ar.getOrientation();
                    }
                }
                if (getContext().getResources().getConfiguration().orientation == 1) {
                    if ((TAG.equals(getTag().toString()) || "SwitchOverlayView".equals(getTag().toString())) && MiuiMultiWindowUtils.isOrientationLandscape(orientation)) {
                        this.mRadiateIconView.setRotation(90.0f);
                    }
                } else if ((TAG.equals(getTag().toString()) || "SwitchOverlayView".equals(getTag().toString())) && MiuiMultiWindowUtils.isOrientationPortrait(orientation) && !isSupportEmbeddedWindow(this.mTopffas.getStackPackageName())) {
                    this.mRadiateIconView.setRotation(-90.0f);
                }
            }
            hide();
        } catch (Exception e) {
            Slog.d(TAG, "onAttachedToWindow failed:" + e);
        }
    }

    private void setBackgroundImage(MiuiFreeFormActivityStack task) {
        Bitmap taskBlurCover;
        if (getContext().getResources().getConfiguration().orientation == 1) {
            taskBlurCover = loadTaskBlurCover(task, this.mController.mScreenShortSide, this.mController.mScreenLongSide);
        } else {
            taskBlurCover = loadTaskBlurCover(task, this.mController.mScreenLongSide, this.mController.mScreenShortSide);
        }
        this.mContentView.setBackground(new BitmapDrawable(taskBlurCover));
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void onDetachedFromWindow() {
        Slog.d(TAG, "onDetachedFromWindow");
        this.mTopffas = null;
        this.mMffas = null;
    }

    public void setStartBounds(Rect contentBounds) {
        if (getVisibility() == 0 && this.mContentView.getVisibility() == 0) {
            return;
        }
        Slog.d(TAG, "setStartBounds:" + contentBounds);
        contentBounds.set((int) (contentBounds.left - this.mRadius), (int) (contentBounds.top - this.mRadius), (int) (contentBounds.right + this.mRadius), (int) (contentBounds.bottom + this.mRadius));
        this.mContentView.setX(contentBounds.left);
        this.mContentView.setY(contentBounds.top);
        this.mContentView.getLayoutParams().width = contentBounds.width();
        this.mContentView.getLayoutParams().height = contentBounds.height();
        this.mContentView.requestLayout();
        this.mStartBounds.set(contentBounds);
    }

    public void startContentAnimation(int animationType, String packageName, int action, MiuiFreeFormActivityStack mffas) {
        if (getVisibility() == 0 && this.mContentView.getVisibility() == 0) {
            return;
        }
        if (mffas != null) {
            setBackgroundImage(mffas);
        }
        startOpenAnimation(animationType, packageName, action, mffas);
    }

    public void startBorderAlphaHideAnimation() {
        PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat("ALPHA", 1.0f, MiuiFreeformPinManagerService.EDGE_AREA);
        ValueAnimator animator = ValueAnimator.ofPropertyValuesHolder(alpha);
        animator.setInterpolator(new LinearInterpolator());
        animator.setDuration(200L);
        animator.addUpdateListener(new AnimatorUpdateListener(animator, 0, this.mBorderView));
        animator.addListener(new AnimatorListener(0, this.mBorderView, this.ACTION_ALPHA_HIDE));
        animator.start();
    }

    public void startBorderAnimation(boolean appear) {
        float centerY;
        float centerX;
        float nowHeightScale;
        float nowWidthScale;
        setVisibility(0);
        this.mRadiateIconView.setVisibility(4);
        this.mContentView.setVisibility(4);
        this.mBorderView.setVisibility(0);
        Rect contentBounds = this.mStartBounds;
        if (this.mController.mGestureController.mGestureListener.mIsPortrait) {
            nowWidthScale = (contentBounds.width() * 1.0f) / this.mController.mScreenShortSide;
            nowHeightScale = (contentBounds.height() * 1.0f) / this.mController.mScreenLongSide;
            centerX = this.mController.mScreenShortSide / 2;
            centerY = this.mController.mScreenLongSide / 2;
        } else {
            nowWidthScale = (contentBounds.width() * 1.0f) / this.mController.mScreenLongSide;
            nowHeightScale = (contentBounds.height() * 1.0f) / this.mController.mScreenShortSide;
            centerX = this.mController.mScreenLongSide / 2;
            centerY = this.mController.mScreenShortSide / 2;
        }
        AnimState hiddenState = new AnimState(0).add("translationX", contentBounds.centerX()).add("translationY", contentBounds.centerY()).add("scaleX", nowWidthScale).add("scaleY", nowHeightScale).add("alpha", 0.0d);
        AnimState shownState = new AnimState(1).add("translationX", centerX).add("translationY", centerY).add("scaleX", 1.0d).add("scaleY", 1.0d).add("alpha", 1.0d);
        IStateStyle iStateStyle = this.mFolmeStyle;
        if (iStateStyle != null) {
            iStateStyle.cancel();
        }
        IStateStyle useValue = Folme.useValue(new Object[0]);
        this.mFolmeStyle = useValue;
        if (appear) {
            this.mBorderView.setAlpha(1.0f);
            this.mFolmeStyle.fromTo(hiddenState, shownState, new AnimConfig[]{this.mFolmeAnimConfig});
            return;
        }
        useValue.fromTo(shownState, hiddenState, new AnimConfig[]{this.mFolmeAnimConfig});
    }

    private void startOpenAnimation(int actionType, String packageName, int action, MiuiFreeFormActivityStack mffas) {
        Bitmap taskBlurCover;
        Slog.d(TAG, "startOpenAnimation:actionType=" + actionType);
        this.mPackageName = packageName;
        this.mMffas = mffas;
        int i = 5;
        if (actionType == 1) {
            synchronized (this.mController.mGestureController.mGestureListener.mLock) {
                MiuiFreeFormActivityStack clonedCurrentControlActivityStack = null;
                MiuiFreeFormActivityStack currentControlActivityStack = this.mController.mGestureController.mMiuiFreeFormManagerService.getTopFreeFormActivityStack();
                if (currentControlActivityStack != null) {
                    clonedCurrentControlActivityStack = new MiuiFreeFormActivityStack(currentControlActivityStack);
                }
                this.mTopffas = clonedCurrentControlActivityStack;
            }
            if (getContext().getResources().getConfiguration().orientation == 1) {
                Bitmap taskBlurCover2 = loadTaskBlurCover(this.mTopffas, this.mController.mScreenShortSide, this.mController.mScreenLongSide);
                taskBlurCover = taskBlurCover2;
            } else {
                Bitmap taskBlurCover3 = loadTaskBlurCover(this.mTopffas, this.mController.mScreenLongSide, this.mController.mScreenShortSide);
                taskBlurCover = taskBlurCover3;
            }
            this.mContentView.setBackground(new BitmapDrawable(taskBlurCover));
            setVisibility(0);
            this.mContentView.setVisibility(0);
            this.mRadiateIconView.setVisibility(4);
            PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat("ALPHA", MiuiFreeformPinManagerService.EDGE_AREA, 1.0f);
            ValueAnimator animator = ValueAnimator.ofPropertyValuesHolder(alpha);
            animator.setInterpolator(MiuiFreeFormGestureAnimator.QUINT_EASE_OUT_INTERPOLATOR);
            animator.setDuration(200L);
            animator.addUpdateListener(new AnimatorUpdateListener(animator, 0, this));
            int i2 = this.ACTION_ALPHA_SHOW;
            if (!this.mMffas.isInMiniFreeFormMode()) {
                i = 3;
            }
            animator.addListener(new AnimatorListener(0, this, i2, i));
            animator.start();
        } else if (actionType == 4) {
            setVisibility(0);
            this.mContentView.setVisibility(0);
            this.mRadiateIconView.setVisibility(4);
            PropertyValuesHolder alpha2 = PropertyValuesHolder.ofFloat("ALPHA", MiuiFreeformPinManagerService.EDGE_AREA, 1.0f);
            ValueAnimator animator2 = ValueAnimator.ofPropertyValuesHolder(alpha2);
            animator2.setInterpolator(MiuiFreeFormGestureAnimator.QUINT_EASE_OUT_INTERPOLATOR);
            animator2.setDuration(100L);
            animator2.addUpdateListener(new AnimatorUpdateListener(animator2, 5, this));
            animator2.addListener(new AnimatorListener(5, this, this.ACTION_ALPHA_SHOW));
            animator2.start();
        }
    }

    public void startOpenAnimation(final int freeformAction, final int animationType) {
        float centerY;
        float centerX;
        float nowHeightScale;
        float nowWidthScale;
        Rect contentBounds = this.mStartBounds;
        if (this.mController.mGestureController.mGestureListener.mIsPortrait) {
            float nowWidthScale2 = (contentBounds.width() * 1.0f) / this.mController.mScreenShortSide;
            float nowHeightScale2 = (contentBounds.height() * 1.0f) / this.mController.mScreenLongSide;
            float centerX2 = this.mController.mScreenShortSide / 2;
            nowWidthScale = nowWidthScale2;
            nowHeightScale = nowHeightScale2;
            centerX = centerX2;
            centerY = this.mController.mScreenLongSide / 2;
        } else {
            float nowWidthScale3 = (contentBounds.width() * 1.0f) / this.mController.mScreenLongSide;
            float nowHeightScale3 = (contentBounds.height() * 1.0f) / this.mController.mScreenShortSide;
            float centerX3 = this.mController.mScreenLongSide / 2;
            nowWidthScale = nowWidthScale3;
            nowHeightScale = nowHeightScale3;
            centerX = centerX3;
            centerY = this.mController.mScreenShortSide / 2;
        }
        Collection<Animator> animatorItems = new HashSet<>();
        AnimatorSet animatorSet = new AnimatorSet();
        PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat(SCALE_WIDTH, nowWidthScale, 1.0f);
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat(SCALE_HEIGTH, nowHeightScale, 1.0f);
        PropertyValuesHolder translateX = PropertyValuesHolder.ofFloat("TRANSLATE_X", contentBounds.centerX(), centerX);
        PropertyValuesHolder translateY = PropertyValuesHolder.ofFloat("TRANSLATE_Y", contentBounds.centerY(), centerY);
        ValueAnimator animator = ValueAnimator.ofPropertyValuesHolder(translateX, translateY, scaleX, scaleY);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.setDuration(350L);
        animator.addUpdateListener(new AnimatorUpdateListener(animator, animationType, this.mContentView));
        animator.setInterpolator(MiuiFreeFormGestureAnimator.QUINT_EASE_OUT_INTERPOLATOR);
        animatorItems.add(animator);
        animatorSet.playTogether(animatorItems);
        animatorSet.addListener(new AnimatorListener(animationType, this.mContentView, this.ACTION_UNDEFINED, freeformAction));
        animatorSet.start();
        postDelayed(new Runnable() { // from class: com.android.server.wm.MiuiFreeFormOverlayView.4
            @Override // java.lang.Runnable
            public void run() {
                PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat("ALPHA", MiuiFreeformPinManagerService.EDGE_AREA, 1.0f);
                ValueAnimator animator2 = ValueAnimator.ofPropertyValuesHolder(alpha);
                animator2.setInterpolator(MiuiFreeFormGestureAnimator.QUINT_EASE_OUT_INTERPOLATOR);
                animator2.setDuration(175L);
                int i = animationType;
                if (4 == i) {
                    MiuiFreeFormOverlayView miuiFreeFormOverlayView = MiuiFreeFormOverlayView.this;
                    animator2.addUpdateListener(new AnimatorUpdateListener(animator2, 5, miuiFreeFormOverlayView.mRadiateIconView));
                    MiuiFreeFormOverlayView miuiFreeFormOverlayView2 = MiuiFreeFormOverlayView.this;
                    animator2.addListener(new AnimatorListener(5, miuiFreeFormOverlayView2.mRadiateIconView, MiuiFreeFormOverlayView.this.ACTION_UNDEFINED));
                } else if (1 == i) {
                    MiuiFreeFormOverlayView miuiFreeFormOverlayView3 = MiuiFreeFormOverlayView.this;
                    animator2.addUpdateListener(new AnimatorUpdateListener(animator2, 0, miuiFreeFormOverlayView3.mRadiateIconView));
                    MiuiFreeFormOverlayView miuiFreeFormOverlayView4 = MiuiFreeFormOverlayView.this;
                    animator2.addListener(new AnimatorListener(0, miuiFreeFormOverlayView4.mRadiateIconView, MiuiFreeFormOverlayView.this.ACTION_UNDEFINED, freeformAction));
                }
                animator2.start();
            }
        }, 175L);
    }

    public void hide() {
        setVisibility(8);
    }

    public void show() {
        setVisibility(0);
    }

    /* loaded from: classes.dex */
    public class AnimatorUpdateListener implements ValueAnimator.AnimatorUpdateListener {
        private int mAnimationType;
        private ValueAnimator mAnimator;
        private View mView;

        AnimatorUpdateListener(ValueAnimator animator, int animationType, View view) {
            MiuiFreeFormOverlayView.this = r1;
            this.mAnimator = animator;
            this.mAnimationType = animationType;
            this.mView = view;
        }

        @Override // android.animation.ValueAnimator.AnimatorUpdateListener
        public void onAnimationUpdate(ValueAnimator animation) {
            int right;
            int top;
            int bottom;
            int left;
            int i = this.mAnimationType;
            if (i == 5) {
                float value = ((Float) animation.getAnimatedValue("ALPHA")).floatValue();
                if (this.mView instanceof MiuiFreeFormRadiateImageView) {
                    MiuiFreeFormOverlayView.this.mRadiateIconView.setAlpha(value);
                }
            } else if (i == 0) {
                float value2 = ((Float) animation.getAnimatedValue("ALPHA")).floatValue();
                View view = this.mView;
                if (view instanceof MiuiFreeFormRadiateImageView) {
                    MiuiFreeFormOverlayView.this.mRadiateIconView.setAlpha(value2);
                } else if (view instanceof MiuiFreeFormRoundRectView) {
                    MiuiFreeFormOverlayView.this.mBorderView.setAlpha(value2);
                } else {
                    MiuiFreeFormOverlayView.this.setAlpha(value2);
                }
            } else if (i == 1 || i == 4) {
                float animationTransX = ((Float) animation.getAnimatedValue("TRANSLATE_X")).floatValue();
                float animationTransY = ((Float) animation.getAnimatedValue("TRANSLATE_Y")).floatValue();
                float scaleX = ((Float) animation.getAnimatedValue(MiuiFreeFormOverlayView.SCALE_WIDTH)).floatValue();
                float scaleY = ((Float) animation.getAnimatedValue(MiuiFreeFormOverlayView.SCALE_HEIGTH)).floatValue();
                if (MiuiFreeFormOverlayView.this.mController.mGestureController.mGestureListener.mIsPortrait) {
                    left = (int) (animationTransX - ((MiuiFreeFormOverlayView.this.mController.mScreenShortSide * scaleX) / 2.0f));
                    top = (int) (animationTransY - ((MiuiFreeFormOverlayView.this.mController.mScreenLongSide * scaleY) / 2.0f));
                    right = (int) (((MiuiFreeFormOverlayView.this.mController.mScreenShortSide * scaleX) / 2.0f) + animationTransX);
                    bottom = (int) (((MiuiFreeFormOverlayView.this.mController.mScreenLongSide * scaleY) / 2.0f) + animationTransY);
                } else {
                    left = (int) (animationTransX - ((MiuiFreeFormOverlayView.this.mController.mScreenLongSide * scaleX) / 2.0f));
                    top = (int) (animationTransY - ((MiuiFreeFormOverlayView.this.mController.mScreenShortSide * scaleY) / 2.0f));
                    right = (int) (((MiuiFreeFormOverlayView.this.mController.mScreenLongSide * scaleX) / 2.0f) + animationTransX);
                    bottom = (int) (((MiuiFreeFormOverlayView.this.mController.mScreenShortSide * scaleY) / 2.0f) + animationTransY);
                }
                Rect bounds = new Rect(left, top, right, bottom);
                Slog.d(MiuiFreeFormOverlayView.TAG, "onAnimationUpdate  mView:" + this.mView + " bounds:" + bounds + "animationTransX:" + animationTransX + " animationTransY:" + animationTransY + " scaleX:" + scaleX + " scaleY:" + scaleY + " mIsPortrait:" + MiuiFreeFormOverlayView.this.mController.mGestureController.mGestureListener.mIsPortrait);
                MiuiFreeFormOverlayView.this.mContentView.setX(bounds.left);
                MiuiFreeFormOverlayView.this.mContentView.setY(bounds.top);
                MiuiFreeFormOverlayView.this.mContentView.getLayoutParams().width = bounds.width();
                MiuiFreeFormOverlayView.this.mContentView.getLayoutParams().height = bounds.height();
                MiuiFreeFormOverlayView.this.mContentView.requestLayout();
            }
        }
    }

    /* loaded from: classes.dex */
    public class AnimatorListener implements Animator.AnimatorListener {
        private int mAction;
        private int mAnimationType;
        private int mFreeformAction;
        private View mView;

        AnimatorListener(int animationType, View view, int action) {
            MiuiFreeFormOverlayView.this = r1;
            this.mAnimationType = animationType;
            this.mView = view;
            this.mAction = action;
        }

        AnimatorListener(int animationType, View view, int action, int freeformAction) {
            MiuiFreeFormOverlayView.this = r1;
            this.mAnimationType = animationType;
            this.mView = view;
            this.mAction = action;
            this.mFreeformAction = freeformAction;
        }

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationStart(Animator animator) {
            Slog.d(MiuiFreeFormOverlayView.TAG, "onAnimationStart  mView:" + this.mView + " mAnimationType:" + this.mAnimationType + " mAction:" + this.mAction);
            int i = this.mAnimationType;
            if (i == 0) {
                MiuiFreeFormOverlayView miuiFreeFormOverlayView = MiuiFreeFormOverlayView.this;
                miuiFreeFormOverlayView.mIconBmp = miuiFreeFormOverlayView.loadTaskIcon(miuiFreeFormOverlayView.getContext(), MiuiFreeFormOverlayView.this.mPackageName);
                if (this.mView instanceof MiuiFreeFormRadiateImageView) {
                    MiuiFreeFormOverlayView.this.mRadiateIconView.showIcon(MiuiFreeFormOverlayView.this.mIconBmp, MiuiFreeFormOverlayView.ICON_W_H_THRESHOLD, MiuiFreeFormOverlayView.ICON_W_H_THRESHOLD, ScreenRotationAnimationImpl.COVER_EGE, 25);
                    MiuiFreeFormOverlayView.this.mRadiateIconView.setVisibility(0);
                }
            } else if (i == 5) {
                MiuiFreeFormOverlayView miuiFreeFormOverlayView2 = MiuiFreeFormOverlayView.this;
                miuiFreeFormOverlayView2.mIconBmp = miuiFreeFormOverlayView2.loadTaskIcon(miuiFreeFormOverlayView2.getContext(), MiuiFreeFormOverlayView.this.mPackageName);
                if (this.mView instanceof MiuiFreeFormRadiateImageView) {
                    MiuiFreeFormOverlayView.this.mRadiateIconView.showIcon(MiuiFreeFormOverlayView.this.mIconBmp, MiuiFreeFormOverlayView.ICON_W_H_THRESHOLD, MiuiFreeFormOverlayView.ICON_W_H_THRESHOLD, ScreenRotationAnimationImpl.COVER_EGE, 25);
                    MiuiFreeFormOverlayView.this.mRadiateIconView.setVisibility(0);
                }
            }
        }

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationEnd(Animator animator) {
            Bitmap taskBlurCover;
            MiuiFreeFormOverlayView miuiFreeFormOverlayView;
            Slog.d(MiuiFreeFormOverlayView.TAG, "onAnimationEnd  mView:" + this.mView + " mAnimationType:" + this.mAnimationType + " mAction:" + this.mAction);
            int i = this.mAnimationType;
            if (i == 0) {
                View view = this.mView;
                if (!(view instanceof ImageView) && view == (miuiFreeFormOverlayView = MiuiFreeFormOverlayView.this)) {
                    if (this.mAction == miuiFreeFormOverlayView.ACTION_ALPHA_SHOW) {
                        MiuiFreeFormOverlayView.this.startOpenAnimation(this.mFreeformAction, 1);
                    } else if (this.mAction == MiuiFreeFormOverlayView.this.ACTION_ALPHA_HIDE) {
                        if (MiuiFreeFormOverlayView.TAG.equals(MiuiFreeFormOverlayView.this.getTag().toString())) {
                            MiuiFreeFormOverlayView.this.mController.hideOverlayView();
                            MiuiFreeFormOverlayView.this.mRemoveAnimationToDo = false;
                            if (MiuiFreeFormOverlayView.this.mController.mShouldRemoveOverlayView) {
                                MiuiFreeFormOverlayView.this.mController.removeOverlayView();
                                MiuiFreeFormOverlayView.this.mController.mShouldRemoveOverlayView = false;
                            }
                        } else {
                            MiuiFreeFormOverlayView.this.mController.hideSwitchOverlayView();
                        }
                        MiuiFreeFormOverlayView.this.mController.setDisableScreenRotation(false);
                    }
                }
            } else if (i == 1) {
                MiuiFreeFormOverlayView.this.postDelayed(new Runnable() { // from class: com.android.server.wm.MiuiFreeFormOverlayView.AnimatorListener.1
                    @Override // java.lang.Runnable
                    public void run() {
                        try {
                            MiuiFreeFormOverlayView.this.mController.mGestureController.mService.mActivityManager.resizeTask(MiuiFreeFormOverlayView.this.mMffas.mStackID, (Rect) null, 0);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        MiuiFreeFormOverlayView.this.mController.setDisableScreenRotation(true);
                        MiuiFreeFormOverlayView.this.mController.startShowFullScreenWindow(AnimatorListener.this.mFreeformAction, MiuiFreeFormOverlayView.this.mMffas);
                    }
                }, 200L);
            } else if (i == 4) {
                View view2 = new View(MiuiFreeFormOverlayView.this.getContext());
                if (MiuiFreeFormOverlayView.this.getContext().getResources().getConfiguration().orientation == 1) {
                    MiuiFreeFormOverlayView miuiFreeFormOverlayView2 = MiuiFreeFormOverlayView.this;
                    taskBlurCover = miuiFreeFormOverlayView2.loadTaskBlurCover(miuiFreeFormOverlayView2.mTopffas, MiuiFreeFormOverlayView.this.mController.mScreenShortSide, MiuiFreeFormOverlayView.this.mController.mScreenLongSide);
                } else {
                    MiuiFreeFormOverlayView miuiFreeFormOverlayView3 = MiuiFreeFormOverlayView.this;
                    taskBlurCover = miuiFreeFormOverlayView3.loadTaskBlurCover(miuiFreeFormOverlayView3.mTopffas, MiuiFreeFormOverlayView.this.mController.mScreenLongSide, MiuiFreeFormOverlayView.this.mController.mScreenShortSide);
                }
                view2.setBackground(new BitmapDrawable(taskBlurCover));
                view2.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));
                MiuiFreeFormOverlayView.this.mContentView = view2;
                MiuiFreeFormOverlayView miuiFreeFormOverlayView4 = MiuiFreeFormOverlayView.this;
                miuiFreeFormOverlayView4.setBgRadius(miuiFreeFormOverlayView4.mContentView, MiuiFreeFormOverlayView.this.mRadius);
                MiuiFreeFormOverlayView.this.removeViewAt(1);
                MiuiFreeFormOverlayView.this.addView(view2, 1);
                MiuiFreeFormOverlayView.this.mController.setDisableScreenRotation(true);
                MiuiFreeFormOverlayView.this.postDelayed(new Runnable() { // from class: com.android.server.wm.MiuiFreeFormOverlayView.AnimatorListener.2
                    @Override // java.lang.Runnable
                    public void run() {
                        MiuiFreeFormOverlayView.this.mController.mGestureController.mMiuiFreeFormSwitchAppHelper.startSwitchAppTwoStep();
                    }
                }, 50L);
            } else if (i == 5 && this.mAction == MiuiFreeFormOverlayView.this.ACTION_ALPHA_SHOW) {
                MiuiFreeFormOverlayView.this.startOpenAnimation(-1, 4);
            }
        }

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationCancel(Animator animator) {
        }

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationRepeat(Animator animator) {
        }
    }

    /* loaded from: classes.dex */
    private static class ScaleXAnimation extends Animation {
        private float mFromX;
        private float mPivotX;
        private float mToX;

        public ScaleXAnimation(float fromX, float toX) {
            this.mFromX = fromX;
            this.mToX = toX;
            this.mPivotX = MiuiFreeformPinManagerService.EDGE_AREA;
        }

        public ScaleXAnimation(float fromX, float toX, float pivotX) {
            this.mFromX = fromX;
            this.mToX = toX;
            this.mPivotX = pivotX;
        }

        @Override // android.view.animation.Animation
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            float sx = 1.0f;
            float scale = getScaleFactor();
            float f = this.mFromX;
            if (f != 1.0f || this.mToX != 1.0f) {
                sx = f + ((this.mToX - f) * interpolatedTime);
            }
            if (this.mPivotX == MiuiFreeformPinManagerService.EDGE_AREA) {
                t.getMatrix().setScale(sx, 1.0f);
            } else {
                t.getMatrix().setScale(sx, 1.0f, this.mPivotX * scale, MiuiFreeformPinManagerService.EDGE_AREA);
            }
        }
    }

    /* loaded from: classes.dex */
    private static class ScaleYAnimation extends Animation {
        private float mFromY;
        private float mPivotY;
        private float mToY;

        public ScaleYAnimation(float fromY, float toY) {
            this.mFromY = fromY;
            this.mToY = toY;
            this.mPivotY = MiuiFreeformPinManagerService.EDGE_AREA;
        }

        public ScaleYAnimation(float fromY, float toY, float pivotY) {
            this.mFromY = fromY;
            this.mToY = toY;
            this.mPivotY = pivotY;
        }

        @Override // android.view.animation.Animation
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            float sy = 1.0f;
            float scale = getScaleFactor();
            float f = this.mFromY;
            if (f != 1.0f || this.mToY != 1.0f) {
                sy = f + ((this.mToY - f) * interpolatedTime);
            }
            if (this.mPivotY == MiuiFreeformPinManagerService.EDGE_AREA) {
                t.getMatrix().setScale(1.0f, sy);
            } else {
                t.getMatrix().setScale(1.0f, sy, MiuiFreeformPinManagerService.EDGE_AREA, this.mPivotY * scale);
            }
        }
    }

    public void setBgRadius(View content, final float radius) {
        content.setOutlineProvider(new ViewOutlineProvider() { // from class: com.android.server.wm.MiuiFreeFormOverlayView.5
            @Override // android.view.ViewOutlineProvider
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
            }
        });
        content.setClipToOutline(true);
    }

    public Bitmap loadTaskBlurCover(MiuiFreeFormActivityStack mffas, int width, int height) {
        WindowState w;
        if (mffas == null) {
            return null;
        }
        try {
            w = mffas.mTask.getTopVisibleActivity().findMainWindow();
        } catch (Exception e) {
        }
        try {
            if (w != null && !w.isSecureLocked()) {
                Bitmap blurCover = null;
                Rect sourceCrop = new Rect();
                Rect taskBounds = mffas.mTask.getBounds();
                sourceCrop.set(taskBounds.left, taskBounds.top, taskBounds.left + ((int) (taskBounds.width() * mffas.mFreeFormScale)), taskBounds.top + ((int) (taskBounds.height() * mffas.mFreeFormScale)));
                sourceCrop.offsetTo(0, 0);
                SurfaceControl.ScreenshotHardwareBuffer screenshotBuffer = SurfaceControl.captureLayers(mffas.mTask.getSurfaceControl(), sourceCrop, 0.1f);
                if (screenshotBuffer != null) {
                    screenshotBuffer.asBitmap();
                }
                if (0 != 0) {
                    blurCover = Bitmap.createScaledBitmap(null, width, height, true);
                }
                if (blurCover == null) {
                    return MiuiMultiWindowUtils.drawableToBitmap(getContext().getResources().getDrawable(285737091));
                }
                return blurCover;
            }
            return MiuiMultiWindowUtils.drawableToBitmap(getContext().getResources().getDrawable(285737091));
        } catch (Exception e2) {
            return MiuiMultiWindowUtils.drawableToBitmap(getContext().getResources().getDrawable(285737091));
        }
    }

    public Bitmap loadTaskIcon(Context context, String pkg) {
        if (context == null || TextUtils.isEmpty(pkg)) {
            return null;
        }
        try {
            PackageManager packageManager = context.getPackageManager();
            Bitmap activityIcon = zoomDrawableIfNeed(packageManager.getApplicationIcon(pkg));
            return activityIcon;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private Bitmap zoomDrawableIfNeed(Drawable drawable) {
        if (drawable == null) {
            return null;
        }
        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();
        if (width <= 0 || height <= 0) {
            return null;
        }
        int i = ICON_W_H_THRESHOLD;
        float scale = i / width;
        if (width == i && height == i) {
            return MiuiMultiWindowUtils.drawableToBitmap(drawable);
        }
        Bitmap oldbmp = MiuiMultiWindowUtils.drawableToBitmap(drawable);
        Bitmap output = Bitmap.createBitmap(oldbmp.getWidth(), oldbmp.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint();
        Rect rect = new Rect(0, 0, oldbmp.getWidth(), oldbmp.getHeight());
        RectF rectF = new RectF(rect);
        float roundPx = (36 / 2.0f) / scale;
        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(-12434878);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(oldbmp, rect, rect, paint);
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);
        Bitmap newbmp = Bitmap.createBitmap(output, 0, 0, width, height, matrix, true);
        return newbmp;
    }

    private Bitmap rotateBitmap(Bitmap bitmap, float degree) {
        if (bitmap == null) {
            return null;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        try {
            return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean removeAnimationToDo() {
        return this.mRemoveAnimationToDo;
    }
}
