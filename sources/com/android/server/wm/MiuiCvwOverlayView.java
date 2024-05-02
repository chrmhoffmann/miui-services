package com.android.server.wm;

import android.animation.Animator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.BLASTBufferQueue;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.HardwareRenderer;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewRootImpl;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.server.wm.MiuiCvwGestureController;
import com.android.server.wm.MiuiCvwOverlayView;
import java.lang.reflect.Field;
/* loaded from: classes.dex */
public class MiuiCvwOverlayView extends FrameLayout implements ViewRootImpl.SurfaceChangedCallback {
    public static final int ANIMATION_CORNER_TO_MINI = 128;
    public static final int ANIMATION_HIDE = 4;
    public static final int ANIMATION_SHOW = 2;
    public static final int ANIMATION_SWITCH_FREEOFRM = 16;
    public static final int ANIMATION_SWITCH_FULLSCREEN = 64;
    public static final int ANIMATION_SWITCH_MINI = 32;
    private static final String CONTENT_ALPHA = "alpha";
    private static final float CONTENT_ALPHA_IN_VISIBLE = 0.01f;
    private static final float CONTENT_ALPHA_SLIGHTLY_VISIBLE = 0.01f;
    private static final float CONTENT_ALPHA_VISIBLE = 1.0f;
    private static final String CONTENT_BLUR = "blur";
    protected static final float CONTENT_BLUR_EFFECT_VALUE_END = 120.0f;
    private static final float CONTENT_BLUR_EFFECT_VALUE_START = 1.0f;
    private static final String CONTENT_CORNER = "corner";
    private static final float FREEFORM_ICON_SCALE = 1.0f;
    private static final int FREEFORM_ICON_TRANSACTION_Y = 0;
    private static final float FREEFORM_SHAPE_ALPHA = 1.0f;
    private static final float FREEFORM_TEXT_ALPHA = 0.0f;
    private static final float FREEFORM_TEXT_SCALE = 0.5f;
    private static final String ICON_SCALE = "iconScale";
    private static final String ICON_TRANSACTION_Y = "iconTransY";
    private static final float MINI_ICON_SCALE = 0.58f;
    private static final int MINI_ICON_TRANSACTION_Y = -32;
    private static final float MINI_SHAPE_ALPHA = 0.0f;
    private static final float MINI_TEXT_ALPHA = 1.0f;
    private static final int MINI_TEXT_TRANSACTION_Y = 50;
    public static final int OVERLAY_SHADOW_ALPHA = 60;
    public static final int OVERLAY_SHADOW_RADIUS = 65;
    public static final int[] OVERLAY_SHADOW_RGB = {0, 0, 0};
    private static final float RADIATE_IMAGE_SCALE_DEFAULT = 0.5f;
    private static final float RADIATE_IMAGE_SCALE_MINI = 0.29f;
    private static final String SHAPE_ALPHA = "shapeAlpha";
    private static final String TAG = "MiuiCvwOverlayView";
    private static final String TEXT_ALPHA = "textAlpha";
    private static final String TEXT_SCALE = "textScale";
    public static final int WIN_TYPE_FREEFORM = 1;
    public static final int WIN_TYPE_FULLSCREEN = 0;
    public static final int WIN_TYPE_MINI = 2;
    public static final int WIN_TYPE_NULL = -1;
    private float mAlpha;
    private boolean mAlphaHideBlastSync;
    private boolean mAlphaShowBlastSync;
    private View mContent;
    private float mContentBlur;
    private final Context mContext;
    private float mCorner;
    private View mCoverContent;
    private int mDisplayHeight;
    private int mDisplayWidth;
    public final ViewTreeObserver.OnPreDrawListener mDrawListener;
    private ValueAnimator mEnterFreeformAnimator;
    private ValueAnimator mEnterFullscreenAnimator;
    private ValueAnimator mEnterMiniAnimator;
    private ValueAnimator mHideAnimator;
    private ImageView mIcon;
    private float mIconScale;
    private int mIconTransY;
    private long mLastAttemptedDrawFrameNum;
    private ViewGroup mMarkContent;
    private MiuiCvwCoverLayer mMiuiCvwCoverLayer;
    private final CornerOutlineProvider mOutLineProvider;
    private BLASTBufferQueue mOverlaysBBQ;
    private int mPreWinMode;
    private boolean mPrepareShow;
    private MiuiCvwRadiateImageView mRadiateImage;
    private float mRadiateScale;
    private boolean mRefreshRateSet;
    private boolean mResetFinished;
    Paint mShadowPaint;
    RectF mShadowRectF;
    private boolean mShadowShow;
    private float mShapeAlpha;
    private MiuiCvwFreeformShapeView mShapeView;
    private boolean mShowAnimFinished;
    private ValueAnimator mShowAnimator;
    private TextView mText;
    private float mTextAlpha;
    private float mTextScale;
    private int mTextTransY;

    public MiuiCvwOverlayView(Context context) {
        this(context, null);
    }

    public MiuiCvwOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mAlpha = 0.01f;
        this.mShapeAlpha = MiuiFreeformPinManagerService.EDGE_AREA;
        this.mTextAlpha = MiuiFreeformPinManagerService.EDGE_AREA;
        this.mTextScale = 1.0f;
        this.mTextTransY = 0;
        this.mIconScale = 1.0f;
        this.mRadiateScale = RADIATE_IMAGE_SCALE_MINI;
        this.mIconTransY = 0;
        this.mContentBlur = 1.0f;
        this.mPreWinMode = -1;
        this.mPrepareShow = false;
        this.mShadowShow = false;
        this.mShowAnimFinished = false;
        this.mResetFinished = false;
        this.mAlphaShowBlastSync = false;
        this.mAlphaHideBlastSync = false;
        this.mRefreshRateSet = false;
        this.mDrawListener = new ViewTreeObserver.OnPreDrawListener() { // from class: com.android.server.wm.MiuiCvwOverlayView$$ExternalSyntheticLambda3
            @Override // android.view.ViewTreeObserver.OnPreDrawListener
            public final boolean onPreDraw() {
                return MiuiCvwOverlayView.this.m1622lambda$new$0$comandroidserverwmMiuiCvwOverlayView();
            }
        };
        this.mShadowRectF = new RectF();
        setWillNotDraw(false);
        Paint paint = new Paint();
        this.mShadowPaint = paint;
        paint.setStyle(Paint.Style.FILL);
        this.mShadowPaint.setAntiAlias(true);
        this.mContext = context;
        this.mOutLineProvider = new CornerOutlineProvider();
        setBackgroundColor(0);
        setLayoutDirection(0);
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (this.mShadowShow) {
            this.mShadowRectF.left = this.mContent.getX();
            this.mShadowRectF.right = this.mContent.getX() + this.mContent.getLayoutParams().width;
            this.mShadowRectF.top = this.mContent.getY();
            this.mShadowRectF.bottom = this.mContent.getY() + this.mContent.getLayoutParams().height;
            this.mShadowPaint.setStyle(Paint.Style.FILL);
            Paint paint = this.mShadowPaint;
            int[] iArr = OVERLAY_SHADOW_RGB;
            paint.setShadowLayer(65.0f, MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA, Color.argb(60, iArr[0], iArr[1], iArr[2]));
            this.mShadowPaint.setColor(Color.argb(0, iArr[0], iArr[1], iArr[2]));
            RectF rectF = this.mShadowRectF;
            float f = this.mCorner;
            canvas.drawRoundRect(rectF, f, f, this.mShadowPaint);
            this.mShadowPaint.setShadowLayer(MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA, 0);
        }
    }

    public int getDisplayHeight() {
        WindowManager wm = (WindowManager) this.mContext.getSystemService("window");
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(outMetrics);
        return outMetrics.heightPixels;
    }

    public int getDisplayWidth() {
        WindowManager wm = (WindowManager) this.mContext.getSystemService("window");
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(outMetrics);
        return outMetrics.widthPixels;
    }

    public boolean isPortrait() {
        return getResources().getConfiguration().orientation == 1;
    }

    public void show() {
        startAnimating(2);
    }

    public void hide() {
        startAnimating(4);
    }

    public void setFrame(float x, float y, float r, float b) {
        contentLayout((int) x, (int) y, (int) r, (int) b);
        markContentLayout((int) x, (int) y, (int) r, (int) b);
        coverContentLayout((int) x, (int) y, (int) r, (int) b);
        requestLayout();
    }

    private void contentLayout(int l, int t, int r, int b) {
        this.mContent.setTranslationX(l);
        this.mContent.setTranslationY(t);
        this.mContent.getLayoutParams().width = r - l;
        this.mContent.getLayoutParams().height = b - t;
    }

    private void coverContentLayout(int l, int t, int r, int b) {
        this.mCoverContent.setTranslationX(l);
        this.mCoverContent.setTranslationY(t);
        this.mCoverContent.getLayoutParams().width = r - l;
        this.mCoverContent.getLayoutParams().height = b - t;
    }

    private void markContentLayout(int l, int t, int r, int b) {
        this.mMarkContent.setTranslationX(l);
        this.mMarkContent.setTranslationY(t);
        this.mMarkContent.getLayoutParams().width = r - l;
        this.mMarkContent.getLayoutParams().height = b - t;
    }

    @Override // android.view.View
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mDisplayHeight = getDisplayHeight();
        this.mDisplayWidth = getDisplayWidth();
        this.mRadiateImage = (MiuiCvwRadiateImageView) findViewById(285868110);
        this.mShapeView = (MiuiCvwFreeformShapeView) findViewById(285868193);
        this.mText = (TextView) findViewById(285868200);
        this.mIcon = (ImageView) findViewById(285868109);
        this.mCoverContent = findViewById(285868093);
        this.mContent = findViewById(285868090);
        this.mMarkContent = (ViewGroup) findViewById(285868130);
        this.mShapeView.setShapeAlpha(MiuiFreeformPinManagerService.EDGE_AREA);
        this.mText.setAlpha(MiuiFreeformPinManagerService.EDGE_AREA);
        this.mText.setTranslationY(50.0f);
        this.mText.invalidate();
        this.mRadiateImage.setScaleX(0.5f);
        this.mRadiateImage.setScaleY(0.5f);
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ViewTreeObserver observer = getViewTreeObserver();
        observer.addOnPreDrawListener(this.mDrawListener);
        getViewRootImpl().addSurfaceChangedCallback(this);
        this.mMiuiCvwCoverLayer.overlaysAttached();
        this.mContent.setAlpha(0.01f);
        this.mMarkContent.setAlpha(0.01f);
        setAlpha(0.01f);
    }

    public void setContent(Bitmap icon, Bitmap content) {
        this.mIcon.setImageBitmap(icon);
        this.mRadiateImage.showRadiate(icon, 1600, 25);
        this.mRadiateImage.setShadowAlpha(0.6f);
        this.mContent.setBackground(new BitmapDrawable(content));
        setCorner(this.mMiuiCvwCoverLayer.getRadius());
        if (!this.mMiuiCvwCoverLayer.windowIsFullScreen()) {
            enableSolidColor();
        }
        this.mPrepareShow = true;
    }

    void enableSolidColor() {
        this.mShapeView.enableSolidColor();
    }

    public void setBlurContent(Bitmap content) {
        if (!this.mResetFinished) {
            this.mCoverContent.setBackground(new BitmapDrawable(content));
        }
    }

    public void reset() {
        this.mResetFinished = true;
        this.mCoverContent.setBackground(null);
        this.mContent.setBackground(null);
        setVisibility(8);
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        ViewTreeObserver observer = getViewTreeObserver();
        observer.removeOnPreDrawListener(this.mDrawListener);
        this.mMiuiCvwCoverLayer.overlaysDetached();
    }

    public void init(MiuiCvwCoverLayer layer) {
        this.mMiuiCvwCoverLayer = layer;
    }

    public boolean prepareToShow() {
        return this.mPrepareShow;
    }

    public void setCorner(float corner) {
        this.mCorner = corner;
        this.mShapeView.setBackgroundCorner(corner);
        this.mContent.setClipToOutline(true);
        this.mContent.setOutlineProvider(this.mOutLineProvider);
        this.mCoverContent.setClipToOutline(true);
        this.mCoverContent.setOutlineProvider(this.mOutLineProvider);
    }

    public void switchFullScreen() {
        startAnimating(64);
        if (this.mPreWinMode != 0) {
            if (!this.mMiuiCvwCoverLayer.isOverlaysShowFinished()) {
                startAnimating(2);
            }
            this.mPreWinMode = 0;
        }
    }

    public void switchMini() {
        if (this.mPreWinMode != 2) {
            startAnimating(32);
            if (!this.mMiuiCvwCoverLayer.isOverlaysShowFinished()) {
                startAnimating(2);
            }
            this.mPreWinMode = 2;
        }
    }

    public void enterMini() {
        if (this.mPreWinMode == 2) {
            startAnimating(128);
            if (!this.mMiuiCvwCoverLayer.isOverlaysShowFinished()) {
                startAnimating(2);
            }
        }
    }

    public void switchFreeform() {
        if (this.mPreWinMode != 1) {
            startAnimating(16);
            if (!this.mMiuiCvwCoverLayer.isOverlaysShowFinished()) {
                startAnimating(2);
            }
            this.mPreWinMode = 1;
        }
    }

    /* loaded from: classes.dex */
    public class CornerOutlineProvider extends ViewOutlineProvider {
        private CornerOutlineProvider() {
            MiuiCvwOverlayView.this = r1;
        }

        @Override // android.view.ViewOutlineProvider
        public void getOutline(View view, Outline outline) {
            int width = view.getWidth();
            int height = view.getHeight();
            outline.setRoundRect(0, 0, width, height, MiuiCvwOverlayView.this.mCorner);
        }
    }

    public void startAnimating(int animFlag) {
        ValueAnimator valueAnimator;
        ValueAnimator valueAnimator2;
        if (animFlag == 16) {
            ValueAnimator valueAnimator3 = this.mEnterFullscreenAnimator;
            if (valueAnimator3 != null && valueAnimator3.isRunning()) {
                this.mEnterFullscreenAnimator.cancel();
                this.mEnterFullscreenAnimator = null;
            }
            ValueAnimator valueAnimator4 = this.mEnterMiniAnimator;
            if (valueAnimator4 != null && valueAnimator4.isRunning()) {
                this.mEnterMiniAnimator.cancel();
                this.mEnterMiniAnimator = null;
            }
            ValueAnimator ofPropertyValuesHolder = ValueAnimator.ofPropertyValuesHolder(PropertyValuesHolder.ofFloat(SHAPE_ALPHA, this.mShapeAlpha, 1.0f), PropertyValuesHolder.ofFloat(ICON_SCALE, this.mIconScale, 1.0f), PropertyValuesHolder.ofInt(ICON_TRANSACTION_Y, this.mIconTransY, 0), PropertyValuesHolder.ofFloat(TEXT_ALPHA, this.mTextAlpha, MiuiFreeformPinManagerService.EDGE_AREA), PropertyValuesHolder.ofFloat(TEXT_SCALE, this.mTextScale, 0.5f));
            this.mEnterFreeformAnimator = ofPropertyValuesHolder;
            ofPropertyValuesHolder.setDuration(250L);
            this.mEnterFreeformAnimator.setInterpolator(new DecelerateInterpolator(1.5f));
            this.mEnterFreeformAnimator.addUpdateListener(new CvwAnimatorUpdateListener(animFlag));
            this.mEnterFreeformAnimator.addListener(new CvwAnimatorListener(animFlag));
            this.mEnterFreeformAnimator.start();
        } else if (animFlag == 64) {
            ValueAnimator valueAnimator5 = this.mEnterFreeformAnimator;
            if (valueAnimator5 != null && valueAnimator5.isRunning()) {
                this.mEnterFreeformAnimator.cancel();
                this.mEnterFreeformAnimator = null;
            }
            ValueAnimator ofPropertyValuesHolder2 = ValueAnimator.ofPropertyValuesHolder(PropertyValuesHolder.ofFloat(SHAPE_ALPHA, this.mShapeAlpha, MiuiFreeformPinManagerService.EDGE_AREA), PropertyValuesHolder.ofFloat(ICON_SCALE, this.mIconScale, 1.0f), PropertyValuesHolder.ofInt(ICON_TRANSACTION_Y, this.mIconTransY, 0), PropertyValuesHolder.ofFloat(TEXT_ALPHA, this.mTextAlpha, MiuiFreeformPinManagerService.EDGE_AREA), PropertyValuesHolder.ofFloat(TEXT_SCALE, this.mTextScale, 0.5f));
            this.mEnterFullscreenAnimator = ofPropertyValuesHolder2;
            ofPropertyValuesHolder2.setDuration(300L);
            this.mEnterFullscreenAnimator.addUpdateListener(new CvwAnimatorUpdateListener(animFlag));
            this.mEnterFullscreenAnimator.addListener(new CvwAnimatorListener(animFlag));
            this.mEnterFullscreenAnimator.start();
        } else if (animFlag == 32) {
            ValueAnimator valueAnimator6 = this.mEnterFreeformAnimator;
            if (valueAnimator6 != null && valueAnimator6.isRunning()) {
                this.mEnterFreeformAnimator.cancel();
                this.mEnterFreeformAnimator = null;
            }
            ValueAnimator ofPropertyValuesHolder3 = ValueAnimator.ofPropertyValuesHolder(PropertyValuesHolder.ofFloat(SHAPE_ALPHA, this.mShapeAlpha, MiuiFreeformPinManagerService.EDGE_AREA), PropertyValuesHolder.ofFloat(ICON_SCALE, this.mIconScale, MINI_ICON_SCALE), PropertyValuesHolder.ofInt(ICON_TRANSACTION_Y, this.mIconTransY, MINI_ICON_TRANSACTION_Y), PropertyValuesHolder.ofFloat(TEXT_ALPHA, this.mTextAlpha, 1.0f));
            this.mEnterMiniAnimator = ofPropertyValuesHolder3;
            ofPropertyValuesHolder3.setDuration(300L);
            this.mEnterMiniAnimator.setInterpolator(new DecelerateInterpolator(1.5f));
            this.mEnterMiniAnimator.addUpdateListener(new CvwAnimatorUpdateListener(animFlag));
            this.mEnterMiniAnimator.addListener(new CvwAnimatorListener(animFlag));
            this.mEnterMiniAnimator.start();
        } else if (animFlag == 128) {
            ValueAnimator animator = ValueAnimator.ofPropertyValuesHolder(PropertyValuesHolder.ofFloat(CONTENT_CORNER, this.mCorner, this.mMiuiCvwCoverLayer.getMiniFreeformRadius()));
            animator.setDuration(300L);
            animator.addUpdateListener(new CvwAnimatorUpdateListener(animFlag));
            animator.start();
        } else if (animFlag == 2) {
            ValueAnimator valueAnimator7 = this.mHideAnimator;
            if (valueAnimator7 != null && valueAnimator7.isStarted()) {
                return;
            }
            ValueAnimator valueAnimator8 = this.mShowAnimator;
            if (valueAnimator8 != null && valueAnimator8.isStarted()) {
                return;
            }
            ValueAnimator valueAnimator9 = this.mEnterMiniAnimator;
            boolean modeSwitchAnimStarted = (valueAnimator9 != null && valueAnimator9.isStarted()) || ((valueAnimator = this.mEnterFullscreenAnimator) != null && valueAnimator.isStarted()) || ((valueAnimator2 = this.mEnterFreeformAnimator) != null && valueAnimator2.isStarted());
            if (modeSwitchAnimStarted) {
                this.mShowAnimator = ValueAnimator.ofPropertyValuesHolder(PropertyValuesHolder.ofFloat(CONTENT_ALPHA, this.mAlpha, 1.0f), PropertyValuesHolder.ofFloat(CONTENT_CORNER, this.mCorner, this.mMiuiCvwCoverLayer.getRadius()), PropertyValuesHolder.ofFloat(CONTENT_BLUR, this.mContentBlur, CONTENT_BLUR_EFFECT_VALUE_END));
            } else {
                float f = this.mCorner;
                this.mShowAnimator = ValueAnimator.ofPropertyValuesHolder(PropertyValuesHolder.ofFloat(ICON_SCALE, MINI_ICON_SCALE, 1.0f), PropertyValuesHolder.ofFloat(CONTENT_CORNER, f, f), PropertyValuesHolder.ofFloat(CONTENT_ALPHA, this.mAlpha, 1.0f), PropertyValuesHolder.ofFloat(CONTENT_BLUR, this.mContentBlur, CONTENT_BLUR_EFFECT_VALUE_END));
            }
            this.mShowAnimator.setDuration(300L);
            this.mShowAnimator.addUpdateListener(new CvwAnimatorUpdateListener(animFlag));
            this.mShowAnimator.addListener(new CvwAnimatorListener(animFlag));
            this.mShowAnimator.start();
        } else if (animFlag != 4 || this.mMiuiCvwCoverLayer.isOverlaysHiding()) {
        } else {
            ValueAnimator valueAnimator10 = this.mShowAnimator;
            if (valueAnimator10 != null && valueAnimator10.isStarted()) {
                this.mShowAnimator.cancel();
            }
            if (this.mMiuiCvwCoverLayer.isMiniMode()) {
                this.mHideAnimator = ValueAnimator.ofPropertyValuesHolder(PropertyValuesHolder.ofFloat(CONTENT_ALPHA, this.mAlpha, 0.01f), PropertyValuesHolder.ofFloat(CONTENT_CORNER, this.mCorner, this.mMiuiCvwCoverLayer.getMiniFreeformRadius()), PropertyValuesHolder.ofFloat(ICON_SCALE, this.mIconScale, MINI_ICON_SCALE));
            } else {
                this.mHideAnimator = ValueAnimator.ofPropertyValuesHolder(PropertyValuesHolder.ofFloat(CONTENT_ALPHA, this.mAlpha, 0.01f), PropertyValuesHolder.ofFloat(CONTENT_CORNER, this.mCorner, this.mMiuiCvwCoverLayer.getFreeformRadius()), PropertyValuesHolder.ofFloat(ICON_SCALE, this.mIconScale, MINI_ICON_SCALE));
            }
            this.mHideAnimator.setDuration(200L);
            this.mHideAnimator.addUpdateListener(new CvwAnimatorUpdateListener(animFlag));
            this.mHideAnimator.addListener(new CvwAnimatorListener(animFlag));
            this.mHideAnimator.start();
        }
    }

    /* loaded from: classes.dex */
    public class CvwAnimatorUpdateListener implements ValueAnimator.AnimatorUpdateListener {
        public int mAnimationFlag;

        CvwAnimatorUpdateListener(int animationType) {
            MiuiCvwOverlayView.this = r1;
            this.mAnimationFlag = animationType;
        }

        @Override // android.animation.ValueAnimator.AnimatorUpdateListener
        public void onAnimationUpdate(ValueAnimator animation) {
            int i = this.mAnimationFlag;
            if (i == 16 || i == 64 || i == 32) {
                MiuiCvwOverlayView.this.mShapeAlpha = ((Float) animation.getAnimatedValue(MiuiCvwOverlayView.SHAPE_ALPHA)).floatValue();
                MiuiCvwOverlayView.this.mIconScale = ((Float) animation.getAnimatedValue(MiuiCvwOverlayView.ICON_SCALE)).floatValue();
                MiuiCvwOverlayView.this.mIconTransY = ((Integer) animation.getAnimatedValue(MiuiCvwOverlayView.ICON_TRANSACTION_Y)).intValue();
                MiuiCvwOverlayView.this.mTextAlpha = ((Float) animation.getAnimatedValue(MiuiCvwOverlayView.TEXT_ALPHA)).floatValue();
                MiuiCvwOverlayView.this.mShapeView.setShapeAlpha(MiuiCvwOverlayView.this.mShapeAlpha);
                MiuiCvwOverlayView.this.mIcon.setScaleX(MiuiCvwOverlayView.this.mIconScale);
                MiuiCvwOverlayView.this.mIcon.setScaleY(MiuiCvwOverlayView.this.mIconScale);
                MiuiCvwOverlayView.this.mIcon.setTranslationY(MiuiCvwOverlayView.this.mIconTransY);
                MiuiCvwOverlayView.this.mText.setAlpha(MiuiCvwOverlayView.this.mTextAlpha);
                MiuiCvwOverlayView miuiCvwOverlayView = MiuiCvwOverlayView.this;
                miuiCvwOverlayView.mRadiateScale = miuiCvwOverlayView.mIconScale * 0.5f;
                MiuiCvwOverlayView.this.mRadiateImage.setScaleX(MiuiCvwOverlayView.this.mRadiateScale);
                MiuiCvwOverlayView.this.mRadiateImage.setScaleY(MiuiCvwOverlayView.this.mRadiateScale);
            } else if (i == 128) {
                MiuiCvwOverlayView.this.mCorner = ((Float) animation.getAnimatedValue(MiuiCvwOverlayView.CONTENT_CORNER)).floatValue();
                MiuiCvwOverlayView miuiCvwOverlayView2 = MiuiCvwOverlayView.this;
                miuiCvwOverlayView2.setCorner(miuiCvwOverlayView2.mCorner);
            } else if (i == 2) {
                if (MiuiCvwOverlayView.this.mHideAnimator != null && MiuiCvwOverlayView.this.mHideAnimator.isRunning()) {
                    MiuiCvwOverlayView.this.mShowAnimator.cancel();
                    return;
                }
                MiuiCvwOverlayView.this.mAlpha = ((Float) animation.getAnimatedValue(MiuiCvwOverlayView.CONTENT_ALPHA)).floatValue();
                MiuiCvwOverlayView.this.mCorner = ((Float) animation.getAnimatedValue(MiuiCvwOverlayView.CONTENT_CORNER)).floatValue();
                MiuiCvwOverlayView.this.mContentBlur = ((Float) animation.getAnimatedValue(MiuiCvwOverlayView.CONTENT_BLUR)).floatValue();
                Object scaleValue = animation.getAnimatedValue(MiuiCvwOverlayView.ICON_SCALE);
                if (scaleValue != null) {
                    MiuiCvwOverlayView.this.mIconScale = ((Float) scaleValue).floatValue();
                    MiuiCvwOverlayView.this.mIcon.setScaleX(MiuiCvwOverlayView.this.mIconScale);
                    MiuiCvwOverlayView.this.mIcon.setScaleY(MiuiCvwOverlayView.this.mIconScale);
                }
                MiuiCvwOverlayView.this.mMarkContent.setAlpha(MiuiCvwOverlayView.this.mAlpha);
                MiuiCvwOverlayView.this.mShapeView.setBackgroundAlpha(MiuiCvwOverlayView.this.mAlpha);
                if (!MiuiCvwOverlayView.this.mShowAnimFinished) {
                    MiuiCvwOverlayView.this.mContent.setRenderEffect(RenderEffect.createBlurEffect(MiuiCvwOverlayView.this.mContentBlur, MiuiCvwOverlayView.this.mContentBlur, Shader.TileMode.CLAMP));
                }
                MiuiCvwOverlayView miuiCvwOverlayView3 = MiuiCvwOverlayView.this;
                miuiCvwOverlayView3.setCorner(miuiCvwOverlayView3.mCorner);
            } else if (i == 4) {
                MiuiCvwOverlayView.this.mAlpha = ((Float) animation.getAnimatedValue(MiuiCvwOverlayView.CONTENT_ALPHA)).floatValue();
                MiuiCvwOverlayView.this.mIconScale = ((Float) animation.getAnimatedValue(MiuiCvwOverlayView.ICON_SCALE)).floatValue();
                MiuiCvwOverlayView.this.mIcon.setScaleX(MiuiCvwOverlayView.this.mIconScale);
                MiuiCvwOverlayView.this.mIcon.setScaleY(MiuiCvwOverlayView.this.mIconScale);
                MiuiCvwOverlayView miuiCvwOverlayView4 = MiuiCvwOverlayView.this;
                miuiCvwOverlayView4.setAlpha(miuiCvwOverlayView4.mAlpha);
                MiuiCvwOverlayView.this.mCorner = ((Float) animation.getAnimatedValue(MiuiCvwOverlayView.CONTENT_CORNER)).floatValue();
                MiuiCvwOverlayView miuiCvwOverlayView5 = MiuiCvwOverlayView.this;
                miuiCvwOverlayView5.setCorner(miuiCvwOverlayView5.mCorner);
            }
        }
    }

    /* loaded from: classes.dex */
    public class CvwAnimatorListener implements Animator.AnimatorListener {
        private final int mAnimationFlag;

        CvwAnimatorListener(int animationType) {
            MiuiCvwOverlayView.this = r1;
            this.mAnimationFlag = animationType;
        }

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationStart(Animator animation) {
            int i = this.mAnimationFlag;
            if (i == 2 || i == 32) {
                if (MiuiCvwOverlayView.this.mMiuiCvwCoverLayer.isOverlaysShowFinished()) {
                    return;
                }
                MiuiCvwOverlayView.this.mShadowShow = true;
                MiuiCvwOverlayView.this.mAlphaShowBlastSync = true;
                MiuiCvwOverlayView.this.mMiuiCvwCoverLayer.overlaysShowStart();
                MiuiCvwOverlayView.this.setAlpha(1.0f);
                MiuiCvwOverlayView.this.mContent.setAlpha(1.0f);
                MiuiCvwOverlayView.this.mMarkContent.setAlpha(1.0f);
            } else if (i == 4) {
                MiuiCvwOverlayView.this.mAlphaHideBlastSync = true;
                MiuiCvwOverlayView.this.mShadowShow = false;
                MiuiCvwOverlayView.this.mMiuiCvwCoverLayer.overlaysHideStart();
            }
        }

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationEnd(Animator animation) {
            int i = this.mAnimationFlag;
            if (i == 4) {
                MiuiCvwOverlayView.this.mMiuiCvwCoverLayer.overlaysHideFinished();
            } else if (i == 2) {
                if (!MiuiCvwOverlayView.this.mShowAnimFinished) {
                    MiuiCvwOverlayView.this.mShowAnimFinished = true;
                    MiuiCvwOverlayView.this.hideContent();
                }
                MiuiCvwOverlayView.this.mMiuiCvwCoverLayer.overlaysShowFinished();
            }
        }

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationCancel(Animator animation) {
            MiuiCvwOverlayView.this.mAlpha = ((Float) ((ValueAnimator) animation).getAnimatedValue()).floatValue();
            int i = this.mAnimationFlag;
            if (i == 4) {
                MiuiCvwOverlayView.this.mMiuiCvwCoverLayer.overlaysHideFinished();
            } else if (i == 2) {
                MiuiCvwOverlayView.this.mMiuiCvwCoverLayer.overlaysShowFinished();
            }
        }

        @Override // android.animation.Animator.AnimatorListener
        public void onAnimationRepeat(Animator animation) {
        }
    }

    public void hideContent() {
        ValueAnimator valueAnimator = this.mHideAnimator;
        if (valueAnimator == null || !valueAnimator.isStarted()) {
            ValueAnimator hideContentAnim = ValueAnimator.ofPropertyValuesHolder(PropertyValuesHolder.ofFloat(CONTENT_ALPHA, this.mContent.getAlpha(), 0.01f));
            hideContentAnim.setDuration(200L);
            hideContentAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.server.wm.MiuiCvwOverlayView.1
                @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                public void onAnimationUpdate(ValueAnimator animation) {
                    if (MiuiCvwOverlayView.this.mHideAnimator != null && MiuiCvwOverlayView.this.mHideAnimator.isStarted()) {
                        return;
                    }
                    float alpha = ((Float) animation.getAnimatedValue(MiuiCvwOverlayView.CONTENT_ALPHA)).floatValue();
                    MiuiCvwOverlayView.this.mContent.setAlpha(alpha);
                }
            });
            hideContentAnim.addListener(new Animator.AnimatorListener() { // from class: com.android.server.wm.MiuiCvwOverlayView.2
                @Override // android.animation.Animator.AnimatorListener
                public void onAnimationStart(Animator animation) {
                }

                @Override // android.animation.Animator.AnimatorListener
                public void onAnimationEnd(Animator animation) {
                    MiuiCvwOverlayView.this.mContent.setBackground(null);
                    MiuiCvwOverlayView.this.mContent.setRenderEffect(null);
                }

                @Override // android.animation.Animator.AnimatorListener
                public void onAnimationCancel(Animator animation) {
                    MiuiCvwOverlayView.this.mContent.setBackground(null);
                    MiuiCvwOverlayView.this.mContent.setRenderEffect(null);
                }

                @Override // android.animation.Animator.AnimatorListener
                public void onAnimationRepeat(Animator animation) {
                }
            });
            hideContentAnim.start();
        }
    }

    /* renamed from: lambda$new$0$com-android-server-wm-MiuiCvwOverlayView */
    public /* synthetic */ boolean m1622lambda$new$0$comandroidserverwmMiuiCvwOverlayView() {
        Surface sc;
        if (!this.mRefreshRateSet && (sc = getViewRootImpl().mSurface) != null && sc.isValid()) {
            this.mRefreshRateSet = true;
            sc.setFrameRate(this.mMiuiCvwCoverLayer.getMaxRefrashRate(), 1, 1);
        }
        float contentAlpha = getAlpha();
        if (this.mAlphaShowBlastSync) {
            hideTaskWithBlastSync(contentAlpha);
        }
        if (this.mAlphaHideBlastSync) {
            showTaskWithBlastSync(contentAlpha);
        }
        if (this.mMiuiCvwCoverLayer.isTaskResizeWaiting()) {
            resizeTaskWithBlastSync(contentAlpha);
        }
        return true;
    }

    private void showTaskWithBlastSync(float contentAlpha) {
        if (contentAlpha < 1.0f) {
            this.mAlphaHideBlastSync = false;
            getViewRootImpl().registerRtFrameCallback(new HardwareRenderer.FrameDrawingCallback() { // from class: com.android.server.wm.MiuiCvwOverlayView$$ExternalSyntheticLambda1
                public final void onFrameDraw(long j) {
                    MiuiCvwOverlayView.this.m1623xdd12fb41(j);
                }
            });
        }
    }

    /* renamed from: lambda$showTaskWithBlastSync$1$com-android-server-wm-MiuiCvwOverlayView */
    public /* synthetic */ void m1623xdd12fb41(long frame) {
        MiuiCvwGestureController.Slog.d("registerRtFrameCallback", "showTaskWithBlastSync :" + frame);
        this.mMiuiCvwCoverLayer.mCvwAnimator.mergeVisibilityTransaction(this.mMiuiCvwCoverLayer.showTask(), getViewRootImpl(), this.mMiuiCvwCoverLayer.mCvwAnimator.getShowSyncTransaction(), frame);
    }

    /* renamed from: com.android.server.wm.MiuiCvwOverlayView$3 */
    /* loaded from: classes.dex */
    public class AnonymousClass3 implements HardwareRenderer.FrameDrawingCallback {
        AnonymousClass3() {
            MiuiCvwOverlayView.this = this$0;
        }

        public void onFrameDraw(long frame) {
        }

        public HardwareRenderer.FrameCommitCallback onFrameDraw(int syncResult, final long frame) {
            if ((syncResult & 6) != 0) {
                MiuiCvwGestureController.Slog.d(MiuiCvwOverlayView.TAG, "syncResult=" + syncResult);
                MiuiCvwOverlayView.this.mMiuiCvwCoverLayer.setTaskResizeWaiting();
                return null;
            }
            return new HardwareRenderer.FrameCommitCallback() { // from class: com.android.server.wm.MiuiCvwOverlayView$3$$ExternalSyntheticLambda0
                public final void onFrameCommit(boolean z) {
                    MiuiCvwOverlayView.AnonymousClass3.this.m1624lambda$onFrameDraw$1$comandroidserverwmMiuiCvwOverlayView$3(frame, z);
                }
            };
        }

        /* renamed from: lambda$onFrameDraw$1$com-android-server-wm-MiuiCvwOverlayView$3 */
        public /* synthetic */ void m1624lambda$onFrameDraw$1$comandroidserverwmMiuiCvwOverlayView$3(long frame, boolean didProduceBuffer) {
            MiuiCvwGestureController.Slog.d(MiuiCvwOverlayView.TAG, "Received frameCommittedCallback lastAttemptedDrawFrameNum=" + frame + " didProduceBuffer=" + didProduceBuffer);
            if (!didProduceBuffer) {
                MiuiCvwOverlayView.this.mMiuiCvwCoverLayer.setTaskResizeWaiting();
                return;
            }
            View view = MiuiCvwOverlayView.this.mContent;
            final MiuiCvwOverlayView miuiCvwOverlayView = MiuiCvwOverlayView.this;
            view.removeCallbacks(new Runnable() { // from class: com.android.server.wm.MiuiCvwOverlayView$3$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    MiuiCvwOverlayView.this.resizeTaskRemedy();
                }
            });
            MiuiCvwOverlayView.this.mMiuiCvwCoverLayer.overlaysShowFinished();
            MiuiCvwOverlayView.this.mMiuiCvwCoverLayer.resizeTask();
            MiuiCvwGestureController.Slog.d(MiuiCvwOverlayView.TAG, "resizeTaskWithBlastSync end,frameNumber :" + frame);
        }
    }

    private void resizeTaskWithBlastSync(float contentAlpha) {
        if (contentAlpha == 1.0f) {
            getViewRootImpl().registerRtFrameCallback(new AnonymousClass3());
        }
        this.mContent.postDelayed(new Runnable() { // from class: com.android.server.wm.MiuiCvwOverlayView$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                MiuiCvwOverlayView.this.resizeTaskRemedy();
            }
        }, 30L);
        this.mMiuiCvwCoverLayer.taskResized();
    }

    private void hideTaskWithBlastSync(float contentAlpha) {
        if (contentAlpha == 1.0f) {
            this.mAlphaShowBlastSync = false;
            getViewRootImpl().registerRtFrameCallback(new HardwareRenderer.FrameDrawingCallback() { // from class: com.android.server.wm.MiuiCvwOverlayView$$ExternalSyntheticLambda2
                public final void onFrameDraw(long j) {
                    MiuiCvwOverlayView.this.m1621xeee87525(j);
                }
            });
        }
    }

    /* renamed from: lambda$hideTaskWithBlastSync$2$com-android-server-wm-MiuiCvwOverlayView */
    public /* synthetic */ void m1621xeee87525(long frame) {
        this.mMiuiCvwCoverLayer.mCvwAnimator.mergeVisibilityTransaction(this.mMiuiCvwCoverLayer.hideTask(), getViewRootImpl(), this.mMiuiCvwCoverLayer.mCvwAnimator.getHideSyncTransaction(), frame);
    }

    public void resizeTaskRemedy() {
        this.mMiuiCvwCoverLayer.resizeTask();
    }

    public long getLastAttemptedDrawFrameNum() {
        return this.mLastAttemptedDrawFrameNum;
    }

    public void setLastAttemptedDrawFrameNum(long frameNum) {
        this.mLastAttemptedDrawFrameNum = frameNum;
    }

    public BLASTBufferQueue getOverlaysBBQ() {
        ViewRootImpl viewRoot = getViewRootImpl();
        if (viewRoot == null) {
            return null;
        }
        Class<?> clazz = viewRoot.getClass();
        try {
            Field bbq = clazz.getDeclaredField("mBlastBufferQueue");
            bbq.setAccessible(true);
            return (BLASTBufferQueue) bbq.get(viewRoot);
        } catch (Exception e) {
            MiuiCvwGestureController.Slog.e(TAG, "getOverlaysBBQ error :" + e.toString());
            return null;
        }
    }

    public void surfaceCreated(SurfaceControl.Transaction t) {
        this.mOverlaysBBQ = getOverlaysBBQ();
    }

    public void surfaceReplaced(SurfaceControl.Transaction t) {
        this.mOverlaysBBQ = getOverlaysBBQ();
    }

    public void surfaceDestroyed() {
        this.mOverlaysBBQ = null;
    }
}
