package com.android.server.wm;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.res.Resources;
import android.graphics.BLASTBufferQueue;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.MiuiMultiWindowUtils;
import android.util.Slog;
import android.util.TypedValue;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.WindowManager;
import miui.android.animation.controller.AnimState;
/* loaded from: classes.dex */
public class MiuiFreeFormExchangeOverlay {
    private static final int FREEFORM_EXCHANGE_TIP_TEXT_SIZE_IN_DP = 16;
    public static final int MSG_DESTORY = 3;
    public static final int MSG_HIDE = 2;
    public static final int MSG_SHOW = 1;
    private static final String TAG = "MiuiFreeFormExchangeOverlay";
    private static final int TEXT_SIZE_DISPLAY_WIDTH_BENCHMARK_IN_PX = 1220;
    private BLASTBufferQueue mBlastBufferQueue;
    private String mExchagneTip;
    private Bitmap mExchangeImage;
    private Handler mHandler;
    private int mImageLeft;
    private int mImageTop;
    private float mRelativeDensity;
    private int mScreenHeight;
    private int mScreenWidth;
    private SurfaceControl mSurfaceControl;
    private int mText2Left;
    private int mText2Top;
    private int mTextColor;
    private int mTextLeft;
    private int mTextTop;
    private ValueAnimator mValueAnimator;
    private WindowManagerService mWms;
    private static final int V_TEXT_TO_IMAGE_MARGIN = (int) TypedValue.applyDimension(1, 30.1f, Resources.getSystem().getDisplayMetrics());
    private static final int H_TEXT_TO_IMAGE_MARGIN = (int) TypedValue.applyDimension(1, 30.1f, Resources.getSystem().getDisplayMetrics());
    private static final int V_TEXT2_TO_TEXT_MARGIN = (int) TypedValue.applyDimension(1, 23.3f, Resources.getSystem().getDisplayMetrics());
    private static final int H_TEXT2_TO_TEXT_MARGIN = (int) TypedValue.applyDimension(1, 23.3f, Resources.getSystem().getDisplayMetrics());
    private static final int V_IMAGE_MARGIN_TOP = (int) TypedValue.applyDimension(1, 145.45f, Resources.getSystem().getDisplayMetrics());
    private static final int H_IMAGE_MARGIN_TOP = (int) TypedValue.applyDimension(1, 108.0f, Resources.getSystem().getDisplayMetrics());
    private Surface mSurface = new Surface();
    private Object mSurfaceControlLock = new Object();
    private float mAlpha = 1.0f;
    final ColorMatrix mColorMatrix = new ColorMatrix();
    private final Paint mPaint = new Paint(1);

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public final class ExchangeHandler extends Handler {
        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public ExchangeHandler(Looper looper) {
            super(looper);
            MiuiFreeFormExchangeOverlay.this = r1;
        }

        @Override // android.os.Handler
        public void handleMessage(Message msg) {
            boolean z = false;
            switch (msg.what) {
                case 1:
                    MiuiFreeFormExchangeOverlay miuiFreeFormExchangeOverlay = MiuiFreeFormExchangeOverlay.this;
                    if (msg.arg1 == 1) {
                        z = true;
                    }
                    miuiFreeFormExchangeOverlay.showOverlay(z);
                    return;
                case 2:
                    MiuiFreeFormExchangeOverlay miuiFreeFormExchangeOverlay2 = MiuiFreeFormExchangeOverlay.this;
                    if (msg.arg1 == 1) {
                        z = true;
                    }
                    miuiFreeFormExchangeOverlay2.hideOverlay(z);
                    return;
                case 3:
                    MiuiFreeFormExchangeOverlay miuiFreeFormExchangeOverlay3 = MiuiFreeFormExchangeOverlay.this;
                    if (msg.arg1 == 1) {
                        z = true;
                    }
                    miuiFreeFormExchangeOverlay3.destroyOverlay(z);
                    return;
                default:
                    return;
            }
        }
    }

    public MiuiFreeFormExchangeOverlay(MiuiFreeFormGestureController miuiFreeFormGestureController) {
        Task topRootTask;
        this.mSurfaceControl = null;
        Slog.d(TAG, TAG);
        this.mWms = miuiFreeFormGestureController.mService;
        this.mHandler = new ExchangeHandler(miuiFreeFormGestureController.mHandler.getLooper());
        this.mExchagneTip = this.mWms.mContext.getResources().getString(286196211);
        this.mExchangeImage = BitmapFactory.decodeResource(this.mWms.mContext.getResources(), 285737090);
        this.mTextColor = this.mWms.mContext.getResources().getColor(285605932);
        WindowManager wm = (WindowManager) this.mWms.mContext.getSystemService("window");
        DisplayMetrics outMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(outMetrics);
        this.mScreenWidth = outMetrics.widthPixels;
        this.mScreenHeight = outMetrics.heightPixels;
        this.mRelativeDensity = outMetrics.density;
        SurfaceControl parent = null;
        synchronized (this.mWms.mGlobalLock) {
            TaskDisplayArea tc = this.mWms.mRoot.getDefaultTaskDisplayArea();
            if (tc != null && (topRootTask = tc.getTopRootTaskInWindowingMode(1)) != null) {
                parent = topRootTask.getSurfaceControl();
            }
        }
        if (parent == null) {
            Slog.e(TAG, "Did not get TaskContainer");
            return;
        }
        SurfaceControl ctrl = null;
        try {
            ctrl = this.mWms.getDefaultDisplayContentLocked().makeOverlay().setName(TAG).setBufferSize(this.mScreenWidth, this.mScreenHeight).setParent(parent).setFormat(-3).build();
            SurfaceControl.Transaction t = (SurfaceControl.Transaction) this.mWms.mTransactionFactory.get();
            t.setLayer(ctrl, AnimState.VIEW_SIZE);
            t.setPosition(ctrl, MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA);
            t.hide(ctrl);
            t.apply();
        } catch (Surface.OutOfResourcesException e) {
            Slog.e(TAG, "createrSurface e" + e);
        }
        synchronized (this.mSurfaceControlLock) {
            if (ctrl != null) {
                this.mSurfaceControl = ctrl;
                Surface blastSurface = getOrCreateBLASTSurface(this.mScreenWidth, this.mScreenHeight);
                if (blastSurface != null) {
                    this.mSurface.transferFrom(blastSurface);
                }
            }
        }
    }

    private Surface getOrCreateBLASTSurface(int width, int height) {
        if (!this.mSurfaceControl.isValid()) {
            return null;
        }
        BLASTBufferQueue bLASTBufferQueue = this.mBlastBufferQueue;
        if (bLASTBufferQueue == null) {
            BLASTBufferQueue bLASTBufferQueue2 = new BLASTBufferQueue(TAG, this.mSurfaceControl, width, height, -3);
            this.mBlastBufferQueue = bLASTBufferQueue2;
            Surface ret = bLASTBufferQueue2.createSurface();
            return ret;
        }
        bLASTBufferQueue.update(this.mSurfaceControl, width, height, -3);
        return null;
    }

    public void drawIfNeeded() {
        Slog.d(TAG, "drawIfNeeded");
        Rect dirty = new Rect(0, 0, this.mScreenWidth, this.mScreenHeight);
        Canvas c = null;
        try {
            Surface surface = this.mSurface;
            if (surface != null) {
                c = surface.lockCanvas(dirty);
            }
        } catch (Exception e) {
        }
        if (c == null) {
            try {
                Surface surface2 = this.mSurface;
                if (surface2 != null) {
                    surface2.unlockCanvasAndPost(c);
                    return;
                }
                return;
            } catch (Exception e2) {
                return;
            }
        }
        c.drawColor(0, PorterDuff.Mode.CLEAR);
        int exchange_blur_black_color = this.mWms.mContext.getResources().getColor(285605931);
        this.mPaint.setColor(exchange_blur_black_color);
        this.mPaint.setStyle(Paint.Style.FILL);
        c.drawPaint(this.mPaint);
        this.mPaint.reset();
        this.mPaint.setAntiAlias(true);
        this.mPaint.setTextSize(this.mRelativeDensity * 16.0f);
        this.mPaint.setTypeface(Typeface.create("MI Lan Pro", 0));
        this.mPaint.setColor(this.mTextColor);
        this.mPaint.setAlpha((int) (this.mAlpha * Color.alpha(this.mTextColor)));
        Slog.d(TAG, "mTextColor alpha" + Color.alpha(this.mTextColor));
        float length = this.mPaint.measureText(this.mExchagneTip);
        int i = this.mScreenWidth;
        if (length > i) {
            this.mPaint.setTextSize(((i * 16) * this.mRelativeDensity) / 1220.0f);
            length = this.mPaint.measureText(this.mExchagneTip);
        }
        int i2 = this.mScreenHeight;
        int i3 = this.mScreenWidth;
        if (i2 < i3) {
            this.mImageLeft = (i3 - this.mExchangeImage.getWidth()) / 2;
            int i4 = V_IMAGE_MARGIN_TOP;
            this.mImageTop = i4;
            this.mTextLeft = ((int) (this.mScreenWidth - length)) / 2;
            this.mTextTop = i4 + this.mExchangeImage.getHeight() + V_TEXT_TO_IMAGE_MARGIN;
        } else {
            this.mImageLeft = (i3 - this.mExchangeImage.getWidth()) / 2;
            int i5 = H_IMAGE_MARGIN_TOP;
            this.mImageTop = i5;
            this.mTextLeft = ((int) (this.mScreenWidth - length)) / 2;
            this.mTextTop = i5 + this.mExchangeImage.getHeight() + H_TEXT_TO_IMAGE_MARGIN;
        }
        c.drawText(this.mExchagneTip, this.mTextLeft, this.mTextTop, this.mPaint);
        this.mPaint.reset();
        this.mPaint.setAntiAlias(true);
        this.mColorMatrix.set(new float[]{1.0f, MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA, 1.0f, MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA, 1.0f, MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA, this.mAlpha, MiuiFreeformPinManagerService.EDGE_AREA});
        this.mPaint.setColorFilter(new ColorMatrixColorFilter(this.mColorMatrix));
        c.drawBitmap(this.mExchangeImage, this.mImageLeft, this.mImageTop, this.mPaint);
        Surface surface3 = this.mSurface;
        if (surface3 != null) {
            surface3.unlockCanvasAndPost(c);
        }
        this.mPaint.reset();
    }

    public void show(boolean animating) {
        Handler handler = this.mHandler;
        if (handler != null) {
            handler.removeMessages(1);
            Message msg = this.mHandler.obtainMessage();
            msg.what = 1;
            msg.arg1 = animating ? 1 : 0;
            this.mHandler.sendMessage(msg);
        }
    }

    public void hide(boolean animating) {
        Handler handler = this.mHandler;
        if (handler != null) {
            handler.removeMessages(2);
            Message msg = this.mHandler.obtainMessage();
            msg.what = 2;
            msg.arg1 = animating ? 1 : 0;
            this.mHandler.sendMessage(msg);
        }
    }

    public void destroy(boolean animating) {
        Handler handler = this.mHandler;
        if (handler != null) {
            handler.removeMessages(3);
            Message msg = this.mHandler.obtainMessage();
            msg.what = 3;
            msg.arg1 = animating ? 1 : 0;
            this.mHandler.sendMessage(msg);
        }
    }

    public void showOverlay(boolean animate) {
        synchronized (this.mSurfaceControlLock) {
            Slog.d(TAG, "showOverlay surfaceControl=" + this.mSurfaceControl + " animate=" + animate);
            SurfaceControl surfaceControl = this.mSurfaceControl;
            if (surfaceControl != null && surfaceControl.isValid()) {
                if (animate) {
                    startAnimation(MiuiFreeformPinManagerService.EDGE_AREA, 1.0f, true, false);
                } else {
                    drawIfNeeded();
                    SurfaceControl.Transaction t = (SurfaceControl.Transaction) this.mWms.mTransactionFactory.get();
                    t.setBackgroundBlurRadius(this.mSurfaceControl, 100);
                    t.show(this.mSurfaceControl);
                    t.apply();
                }
            }
        }
    }

    public void hideOverlay(boolean animate) {
        synchronized (this.mSurfaceControlLock) {
            Slog.d(TAG, "hideOverlay surfaceControl=" + this.mSurfaceControl + " animate=" + animate);
            SurfaceControl surfaceControl = this.mSurfaceControl;
            if (surfaceControl != null && surfaceControl.isValid()) {
                if (animate) {
                    startAnimation(1.0f, MiuiFreeformPinManagerService.EDGE_AREA, false, false);
                } else {
                    SurfaceControl.Transaction t = (SurfaceControl.Transaction) this.mWms.mTransactionFactory.get();
                    t.hide(this.mSurfaceControl);
                    t.apply();
                }
            }
        }
    }

    public void destroyOverlay(boolean animate) {
        synchronized (this.mSurfaceControlLock) {
            Slog.d(TAG, "destroyOverlay surfaceControl=" + this.mSurfaceControl + " animate=" + animate);
            if (animate) {
                startAnimation(1.0f, MiuiFreeformPinManagerService.EDGE_AREA, false, true);
            } else {
                Surface surface = this.mSurface;
                if (surface != null) {
                    surface.destroy();
                    this.mSurface = null;
                }
                SurfaceControl surfaceControl = this.mSurfaceControl;
                if (surfaceControl != null && surfaceControl.isValid()) {
                    SurfaceControl.Transaction t = (SurfaceControl.Transaction) this.mWms.mTransactionFactory.get();
                    t.remove(this.mSurfaceControl);
                    t.apply();
                    this.mSurfaceControl = null;
                }
                this.mHandler = null;
            }
        }
    }

    void startAnimation(float start, float end, final boolean show, final boolean destory) {
        Slog.d(TAG, "startShowWithAnimation start=" + start + " end=" + end + " show=" + show + " destory=" + destory);
        ValueAnimator valueAnimator = this.mValueAnimator;
        if (valueAnimator != null) {
            start = ((Float) valueAnimator.getAnimatedValue()).floatValue();
            this.mValueAnimator.cancel();
        }
        ValueAnimator ofFloat = ValueAnimator.ofFloat(start, end);
        this.mValueAnimator = ofFloat;
        ofFloat.setDuration(200L);
        this.mValueAnimator.setInterpolator(new MiuiMultiWindowUtils.QuadraticEaseOutInterpolator());
        this.mValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.server.wm.MiuiFreeFormExchangeOverlay.1
            @Override // android.animation.ValueAnimator.AnimatorUpdateListener
            public void onAnimationUpdate(ValueAnimator animation) {
                float alpha = ((Float) animation.getAnimatedValue()).floatValue();
                Slog.d(MiuiFreeFormExchangeOverlay.TAG, "mSurfaceControl=" + MiuiFreeFormExchangeOverlay.this.mSurfaceControl + " alpha=" + alpha);
                if (MiuiFreeFormExchangeOverlay.this.mSurfaceControl != null && MiuiFreeFormExchangeOverlay.this.mSurfaceControl.isValid()) {
                    SurfaceControl.Transaction t = (SurfaceControl.Transaction) MiuiFreeFormExchangeOverlay.this.mWms.mTransactionFactory.get();
                    t.show(MiuiFreeFormExchangeOverlay.this.mSurfaceControl);
                    t.setBackgroundBlurRadius(MiuiFreeFormExchangeOverlay.this.mSurfaceControl, (int) (100.0f * alpha));
                    MiuiFreeFormExchangeOverlay.this.mAlpha = alpha;
                    MiuiFreeFormExchangeOverlay.this.drawIfNeeded();
                    t.apply();
                }
            }
        });
        this.mValueAnimator.addListener(new Animator.AnimatorListener() { // from class: com.android.server.wm.MiuiFreeFormExchangeOverlay.2
            @Override // android.animation.Animator.AnimatorListener
            public void onAnimationStart(Animator animator) {
                if (show && MiuiFreeFormExchangeOverlay.this.mSurfaceControl != null && MiuiFreeFormExchangeOverlay.this.mSurfaceControl.isValid()) {
                    SurfaceControl.Transaction t = (SurfaceControl.Transaction) MiuiFreeFormExchangeOverlay.this.mWms.mTransactionFactory.get();
                    t.setBackgroundBlurRadius(MiuiFreeFormExchangeOverlay.this.mSurfaceControl, 0);
                    MiuiFreeFormExchangeOverlay.this.mAlpha = MiuiFreeformPinManagerService.EDGE_AREA;
                    t.show(MiuiFreeFormExchangeOverlay.this.mSurfaceControl);
                    MiuiFreeFormExchangeOverlay.this.drawIfNeeded();
                    t.apply();
                }
            }

            @Override // android.animation.Animator.AnimatorListener
            public void onAnimationEnd(Animator animator) {
                if (!show) {
                    if (destory) {
                        Slog.d(MiuiFreeFormExchangeOverlay.TAG, "destroyOverlay destory=" + destory + " mSurface=" + MiuiFreeFormExchangeOverlay.this.mSurface);
                        if (MiuiFreeFormExchangeOverlay.this.mSurface != null) {
                            MiuiFreeFormExchangeOverlay.this.mSurface.destroy();
                            MiuiFreeFormExchangeOverlay.this.mSurface = null;
                        }
                        if (MiuiFreeFormExchangeOverlay.this.mSurfaceControl != null && MiuiFreeFormExchangeOverlay.this.mSurfaceControl.isValid()) {
                            SurfaceControl.Transaction t = (SurfaceControl.Transaction) MiuiFreeFormExchangeOverlay.this.mWms.mTransactionFactory.get();
                            t.remove(MiuiFreeFormExchangeOverlay.this.mSurfaceControl);
                            t.apply();
                            MiuiFreeFormExchangeOverlay.this.mSurfaceControl = null;
                        }
                        MiuiFreeFormExchangeOverlay.this.mHandler = null;
                    } else if (MiuiFreeFormExchangeOverlay.this.mSurfaceControl != null && MiuiFreeFormExchangeOverlay.this.mSurfaceControl.isValid()) {
                        SurfaceControl.Transaction t2 = (SurfaceControl.Transaction) MiuiFreeFormExchangeOverlay.this.mWms.mTransactionFactory.get();
                        t2.hide(MiuiFreeFormExchangeOverlay.this.mSurfaceControl);
                        t2.apply();
                    }
                }
            }

            @Override // android.animation.Animator.AnimatorListener
            public void onAnimationCancel(Animator animator) {
            }

            @Override // android.animation.Animator.AnimatorListener
            public void onAnimationRepeat(Animator animator) {
            }
        });
        this.mValueAnimator.start();
    }
}
