package com.android.server.wm;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.MiuiMultiWindowUtils;
import android.util.Slog;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
/* loaded from: classes.dex */
public class MiuiFreeFormHotSpotView extends View {
    public static final int FREEFORM_ANIMATION_IN = 0;
    public static final int FREEFORM_ANIMATION_OUT = 1;
    private static final float FREEFORM_HOTSPOT_FREEFROM_COLOR = 206.0f;
    private static final float FREEFORM_HOTSPOT_NIGHT_FREEFROM_COLOR = 171.0f;
    private static final float FREEFORM_HOTSPOT_NIGHT_SMALL_FREEFROM_COLOR = 79.0f;
    private static final float FREEFORM_HOTSPOT_SMALL_FREEFROM_COLOR = 92.0f;
    public static final int SMALL_FREEFORM_ANIMATION_IN = 2;
    public static final int SMALL_FREEFORM_ANIMATION_OUT = 3;
    private static final String TAG = "HotSpotView";
    private Context mContext;
    float mCurrentAlpha;
    float mCurrentColor;
    int mCurrentHotSpot;
    float mCurrentRaduis;
    private ValueAnimator mInAnimator;
    float mMaxColor;
    float mMinColor;
    private ValueAnimator mOutAnimator;
    private Paint mPaint;
    private ValueAnimator mSmallInAnimator;
    private ValueAnimator mSmallOutAnimator;
    private static boolean DEBUG = MiuiFreeFormGestureController.DEBUG;
    private static final int FREEFORM_HOTSPOT_MIN_RADIUS = (int) TypedValue.applyDimension(1, 72.73f, Resources.getSystem().getDisplayMetrics());
    private static final int FREEFORM_HOTSPOT_MAX_RADIUS = (int) TypedValue.applyDimension(1, 94.55f, Resources.getSystem().getDisplayMetrics());

    public MiuiFreeFormHotSpotView(Context context) {
        this(context, null);
    }

    public MiuiFreeFormHotSpotView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        setBackgroundColor(0);
        this.mPaint = new Paint();
        this.mCurrentHotSpot = -1;
        this.mCurrentAlpha = MiuiFreeformPinManagerService.EDGE_AREA;
        this.mCurrentRaduis = FREEFORM_HOTSPOT_MIN_RADIUS;
        if (MiuiMultiWindowUtils.isNightMode(this.mContext)) {
            this.mMaxColor = FREEFORM_HOTSPOT_NIGHT_FREEFROM_COLOR;
            this.mMinColor = FREEFORM_HOTSPOT_NIGHT_SMALL_FREEFROM_COLOR;
        } else {
            this.mMaxColor = FREEFORM_HOTSPOT_FREEFROM_COLOR;
            this.mMinColor = FREEFORM_HOTSPOT_SMALL_FREEFROM_COLOR;
        }
        this.mCurrentColor = this.mMaxColor;
        Log.i(TAG, "MiuiFreeFormHotSpotView() Context:" + context);
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Paint paint = this.mPaint;
        float f = this.mCurrentColor;
        paint.setColor(Color.valueOf(f / 255.0f, f / 255.0f, f / 255.0f, this.mCurrentAlpha).toArgb());
        switch (this.mCurrentHotSpot) {
            case 1:
                canvas.drawCircle(MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA, this.mCurrentRaduis, this.mPaint);
                break;
            case 2:
                canvas.drawCircle(getDisplayWidth(), MiuiFreeformPinManagerService.EDGE_AREA, this.mCurrentRaduis, this.mPaint);
                break;
            case 3:
                canvas.drawCircle(MiuiFreeformPinManagerService.EDGE_AREA, getDisplayHeight(), this.mCurrentRaduis, this.mPaint);
                break;
            case 4:
                canvas.drawCircle(getDisplayWidth(), getDisplayHeight(), this.mCurrentRaduis, this.mPaint);
                break;
        }
        Slog.d(TAG, " mCurrentHotSpot: " + this.mCurrentHotSpot + "mCurrentRaduis" + this.mCurrentRaduis + " mCurrentColor:" + this.mCurrentColor + " mCurrentAlpha:" + this.mCurrentAlpha + "getDisplayWidth(): " + getDisplayWidth() + " getDisplayHeight():" + getDisplayHeight());
    }

    public void startAnimating(final int type) {
        switch (type) {
            case 0:
                ValueAnimator ofFloat = ValueAnimator.ofFloat(this.mCurrentAlpha, 0.4f);
                this.mInAnimator = ofFloat;
                ofFloat.setDuration(300L);
                this.mInAnimator.setInterpolator(new MiuiMultiWindowUtils.QuadraticEaseOutInterpolator());
                this.mInAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.server.wm.MiuiFreeFormHotSpotView.1
                    @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        try {
                            MiuiFreeFormHotSpotView miuiFreeFormHotSpotView = MiuiFreeFormHotSpotView.this;
                            miuiFreeFormHotSpotView.mCurrentAlpha = ((Float) miuiFreeFormHotSpotView.mInAnimator.getAnimatedValue()).floatValue();
                            MiuiFreeFormHotSpotView.this.invalidate();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                this.mInAnimator.addListener(new Animator.AnimatorListener() { // from class: com.android.server.wm.MiuiFreeFormHotSpotView.2
                    @Override // android.animation.Animator.AnimatorListener
                    public void onAnimationStart(Animator animation) {
                        if (type == 0) {
                            Log.d(MiuiFreeFormHotSpotView.TAG, "setVisibility(VISIBLE)");
                            MiuiFreeFormHotSpotView.this.setVisibility(0);
                        }
                    }

                    @Override // android.animation.Animator.AnimatorListener
                    public void onAnimationEnd(Animator animation) {
                    }

                    @Override // android.animation.Animator.AnimatorListener
                    public void onAnimationCancel(Animator animation) {
                    }

                    @Override // android.animation.Animator.AnimatorListener
                    public void onAnimationRepeat(Animator animation) {
                    }
                });
                ValueAnimator valueAnimator = this.mOutAnimator;
                if (valueAnimator != null && valueAnimator.isStarted()) {
                    this.mOutAnimator.cancel();
                }
                this.mInAnimator.start();
                return;
            case 1:
                ValueAnimator ofFloat2 = ValueAnimator.ofFloat(this.mCurrentAlpha, MiuiFreeformPinManagerService.EDGE_AREA);
                this.mOutAnimator = ofFloat2;
                ofFloat2.setDuration(300L);
                this.mOutAnimator.setInterpolator(new MiuiMultiWindowUtils.QuadraticEaseOutInterpolator());
                this.mOutAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.server.wm.MiuiFreeFormHotSpotView.3
                    @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                    public void onAnimationUpdate(ValueAnimator valueAnimator2) {
                        try {
                            MiuiFreeFormHotSpotView miuiFreeFormHotSpotView = MiuiFreeFormHotSpotView.this;
                            miuiFreeFormHotSpotView.mCurrentAlpha = ((Float) miuiFreeFormHotSpotView.mOutAnimator.getAnimatedValue()).floatValue();
                            MiuiFreeFormHotSpotView.this.invalidate();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                this.mOutAnimator.addListener(new Animator.AnimatorListener() { // from class: com.android.server.wm.MiuiFreeFormHotSpotView.4
                    @Override // android.animation.Animator.AnimatorListener
                    public void onAnimationStart(Animator animation) {
                    }

                    @Override // android.animation.Animator.AnimatorListener
                    public void onAnimationEnd(Animator animation) {
                        if (type == 1) {
                            Log.d(MiuiFreeFormHotSpotView.TAG, "setVisibility(GONE)");
                            MiuiFreeFormHotSpotView.this.setVisibility(8);
                        }
                    }

                    @Override // android.animation.Animator.AnimatorListener
                    public void onAnimationCancel(Animator animation) {
                    }

                    @Override // android.animation.Animator.AnimatorListener
                    public void onAnimationRepeat(Animator animation) {
                    }
                });
                ValueAnimator valueAnimator2 = this.mInAnimator;
                if (valueAnimator2 != null && valueAnimator2.isStarted()) {
                    this.mInAnimator.cancel();
                }
                this.mOutAnimator.start();
                return;
            case 2:
                ValueAnimator ofFloat3 = ValueAnimator.ofFloat(this.mCurrentColor, this.mMinColor);
                this.mSmallInAnimator = ofFloat3;
                ofFloat3.setDuration(300L);
                this.mSmallInAnimator.setInterpolator(new MiuiMultiWindowUtils.QuadraticEaseOutInterpolator());
                this.mSmallInAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.server.wm.MiuiFreeFormHotSpotView.5
                    @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                    public void onAnimationUpdate(ValueAnimator valueAnimator3) {
                        try {
                            MiuiFreeFormHotSpotView miuiFreeFormHotSpotView = MiuiFreeFormHotSpotView.this;
                            miuiFreeFormHotSpotView.mCurrentColor = ((Float) miuiFreeFormHotSpotView.mSmallInAnimator.getAnimatedValue()).floatValue();
                            Log.d(MiuiFreeFormHotSpotView.TAG, "SMALL_FREEFORM_ANIMATION_IN mCurrentColor: " + MiuiFreeFormHotSpotView.this.mCurrentColor);
                            MiuiFreeFormHotSpotView.this.invalidate();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                ValueAnimator valueAnimator3 = this.mSmallOutAnimator;
                if (valueAnimator3 != null && valueAnimator3.isStarted()) {
                    this.mSmallOutAnimator.cancel();
                }
                this.mSmallInAnimator.start();
                return;
            case 3:
                ValueAnimator ofFloat4 = ValueAnimator.ofFloat(this.mCurrentColor, this.mMaxColor);
                this.mSmallOutAnimator = ofFloat4;
                ofFloat4.setDuration(300L);
                this.mSmallOutAnimator.setInterpolator(new MiuiMultiWindowUtils.QuadraticEaseOutInterpolator());
                this.mSmallOutAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() { // from class: com.android.server.wm.MiuiFreeFormHotSpotView.6
                    @Override // android.animation.ValueAnimator.AnimatorUpdateListener
                    public void onAnimationUpdate(ValueAnimator valueAnimator4) {
                        try {
                            MiuiFreeFormHotSpotView miuiFreeFormHotSpotView = MiuiFreeFormHotSpotView.this;
                            miuiFreeFormHotSpotView.mCurrentColor = ((Float) miuiFreeFormHotSpotView.mSmallOutAnimator.getAnimatedValue()).floatValue();
                            Log.d(MiuiFreeFormHotSpotView.TAG, "SMALL_FREEFORM_ANIMATION_OUT mCurrentColor: " + MiuiFreeFormHotSpotView.this.mCurrentColor);
                            MiuiFreeFormHotSpotView.this.invalidate();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                ValueAnimator valueAnimator4 = this.mSmallInAnimator;
                if (valueAnimator4 != null && valueAnimator4.isStarted()) {
                    this.mSmallInAnimator.cancel();
                }
                this.mSmallOutAnimator.start();
                return;
            default:
                return;
        }
    }

    public void inHotSpotArea(int hotSpotNum, float x, float y) {
        this.mCurrentHotSpot = hotSpotNum;
        culcalateRadiusAndColor(hotSpotNum, x, y);
    }

    private void culcalateRadiusAndColor(int hotSpotNum, float x, float y) {
        double currentDistance;
        float maxDistance;
        float subMaxMinRadius;
        double d;
        float f;
        float f2;
        double d2;
        float f3;
        float f4;
        double d3;
        float f5;
        float f6;
        double d4;
        float f7;
        float f8;
        if (MiuiMultiWindowUtils.IS_FOLD_SCREEN_DEVICE) {
            maxDistance = MiuiMultiWindowUtils.FREEFORM_HOTSPOT_REMINDER_PORTRAIT_RADIUS_FOLD + MiuiMultiWindowUtils.FREEFORM_HOTSPOT_REMINDER_PORTRAIT_VERTICAL_TOP_MARGIN_FLOD;
            currentDistance = MiuiMultiWindowUtils.FREEFORM_HOTSPOT_REMINDER_PORTRAIT_RADIUS_FOLD + MiuiMultiWindowUtils.FREEFORM_HOTSPOT_REMINDER_PORTRAIT_VERTICAL_TOP_MARGIN_FLOD;
        } else {
            float maxDistance2 = MiuiMultiWindowUtils.FREEFORM_HOTSPOT_REMINDER_PORTRAIT_RADIUS;
            maxDistance = maxDistance2 + MiuiMultiWindowUtils.FREEFORM_HOTSPOT_REMINDER_PORTRAIT_VERTICAL_TOP_MARGIN;
            currentDistance = MiuiMultiWindowUtils.FREEFORM_HOTSPOT_REMINDER_PORTRAIT_RADIUS + MiuiMultiWindowUtils.FREEFORM_HOTSPOT_REMINDER_PORTRAIT_VERTICAL_TOP_MARGIN;
        }
        switch (this.mCurrentHotSpot) {
            case 1:
                boolean isPortrait = isPortrait();
                if (MiuiMultiWindowUtils.IS_FOLD_SCREEN_DEVICE) {
                    if (isPortrait) {
                        f2 = (MiuiMultiWindowUtils.FREEFORM_HOTSPOT_REMINDER_PORTRAIT_RADIUS_FOLD + MiuiMultiWindowUtils.FREEFORM_HOTSPOT_REMINDER_PORTRAIT_VERTICAL_TOP_MARGIN_FLOD) - MiuiMultiWindowUtils.FREEFORM_HOTSPOT_TRIGGER_PORTRAIT_RADIUS;
                    } else {
                        f2 = (MiuiMultiWindowUtils.FREEFORM_HOTSPOT_REMINDER_LANDCAPE_RADIUS_FOLD + MiuiMultiWindowUtils.FREEFORM_HOTSPOT_REMINDER_LANDCAPE_HORIZONTAL_TOP_MARGIN) - MiuiMultiWindowUtils.FREEFORM_HOTSPOT_TRIGGER_LANDCAPE_RADIUS;
                    }
                    maxDistance = f2;
                } else {
                    if (isPortrait) {
                        f = (MiuiMultiWindowUtils.FREEFORM_HOTSPOT_REMINDER_PORTRAIT_RADIUS + MiuiMultiWindowUtils.FREEFORM_HOTSPOT_REMINDER_PORTRAIT_VERTICAL_TOP_MARGIN) - MiuiMultiWindowUtils.FREEFORM_HOTSPOT_TRIGGER_PORTRAIT_RADIUS;
                    } else {
                        f = (MiuiMultiWindowUtils.FREEFORM_HOTSPOT_REMINDER_LANDCAPE_RADIUS + MiuiMultiWindowUtils.FREEFORM_HOTSPOT_REMINDER_LANDCAPE_HORIZONTAL_TOP_MARGIN) - MiuiMultiWindowUtils.FREEFORM_HOTSPOT_TRIGGER_LANDCAPE_RADIUS;
                    }
                    maxDistance = f;
                }
                if (isPortrait) {
                    d = MiuiMultiWindowUtils.getDistance(x, y, (float) MiuiFreeformPinManagerService.EDGE_AREA, (float) MiuiFreeformPinManagerService.EDGE_AREA) - MiuiMultiWindowUtils.FREEFORM_HOTSPOT_TRIGGER_PORTRAIT_RADIUS;
                } else {
                    d = MiuiMultiWindowUtils.getDistance(x, y, (float) MiuiFreeformPinManagerService.EDGE_AREA, (float) MiuiFreeformPinManagerService.EDGE_AREA) - MiuiMultiWindowUtils.FREEFORM_HOTSPOT_TRIGGER_LANDCAPE_RADIUS;
                }
                currentDistance = d;
                break;
            case 2:
                boolean isPortrait2 = isPortrait();
                if (MiuiMultiWindowUtils.IS_FOLD_SCREEN_DEVICE) {
                    if (isPortrait2) {
                        f4 = (MiuiMultiWindowUtils.FREEFORM_HOTSPOT_REMINDER_PORTRAIT_RADIUS_FOLD + MiuiMultiWindowUtils.FREEFORM_HOTSPOT_REMINDER_PORTRAIT_VERTICAL_TOP_MARGIN_FLOD) - MiuiMultiWindowUtils.FREEFORM_HOTSPOT_TRIGGER_PORTRAIT_RADIUS;
                    } else {
                        f4 = (MiuiMultiWindowUtils.FREEFORM_HOTSPOT_REMINDER_LANDCAPE_RADIUS_FOLD + MiuiMultiWindowUtils.FREEFORM_HOTSPOT_REMINDER_LANDCAPE_HORIZONTAL_TOP_MARGIN) - MiuiMultiWindowUtils.FREEFORM_HOTSPOT_TRIGGER_LANDCAPE_RADIUS;
                    }
                    maxDistance = f4;
                } else {
                    if (isPortrait2) {
                        f3 = (MiuiMultiWindowUtils.FREEFORM_HOTSPOT_REMINDER_PORTRAIT_RADIUS + MiuiMultiWindowUtils.FREEFORM_HOTSPOT_REMINDER_PORTRAIT_VERTICAL_TOP_MARGIN) - MiuiMultiWindowUtils.FREEFORM_HOTSPOT_TRIGGER_PORTRAIT_RADIUS;
                    } else {
                        f3 = (MiuiMultiWindowUtils.FREEFORM_HOTSPOT_REMINDER_LANDCAPE_RADIUS + MiuiMultiWindowUtils.FREEFORM_HOTSPOT_REMINDER_LANDCAPE_HORIZONTAL_TOP_MARGIN) - MiuiMultiWindowUtils.FREEFORM_HOTSPOT_TRIGGER_LANDCAPE_RADIUS;
                    }
                    maxDistance = f3;
                }
                if (isPortrait2) {
                    d2 = MiuiMultiWindowUtils.getDistance(x, y, getDisplayWidth(), (float) MiuiFreeformPinManagerService.EDGE_AREA) - MiuiMultiWindowUtils.FREEFORM_HOTSPOT_TRIGGER_PORTRAIT_RADIUS;
                } else {
                    d2 = MiuiMultiWindowUtils.getDistance(x, y, getDisplayWidth(), (float) MiuiFreeformPinManagerService.EDGE_AREA) - MiuiMultiWindowUtils.FREEFORM_HOTSPOT_TRIGGER_LANDCAPE_RADIUS;
                }
                currentDistance = d2;
                break;
            case 3:
                boolean isPortrait3 = isPortrait();
                if (MiuiMultiWindowUtils.IS_FOLD_SCREEN_DEVICE) {
                    if (isPortrait3) {
                        f6 = (MiuiMultiWindowUtils.FREEFORM_HOTSPOT_REMINDER_PORTRAIT_RADIUS_FOLD + MiuiMultiWindowUtils.FREEFORM_HOTSPOT_REMINDER_PORTRAIT_VERTICAL_BOTTOM_MARGIN_FOLD) - MiuiMultiWindowUtils.FREEFORM_HOTSPOT_TRIGGER_PORTRAIT_RADIUS;
                    } else {
                        f6 = (MiuiMultiWindowUtils.FREEFORM_HOTSPOT_REMINDER_LANDCAPE_RADIUS_FOLD + MiuiMultiWindowUtils.FREEFORM_HOTSPOT_REMINDER_LANDCAPE_HORIZONTAL_BOTTOM_MARGIN) - MiuiMultiWindowUtils.FREEFORM_HOTSPOT_TRIGGER_LANDCAPE_RADIUS;
                    }
                    maxDistance = f6;
                } else {
                    if (isPortrait3) {
                        f5 = (MiuiMultiWindowUtils.FREEFORM_HOTSPOT_REMINDER_PORTRAIT_RADIUS + MiuiMultiWindowUtils.FREEFORM_HOTSPOT_REMINDER_PORTRAIT_VERTICAL_BOTTOM_MARGIN) - MiuiMultiWindowUtils.FREEFORM_HOTSPOT_TRIGGER_PORTRAIT_RADIUS;
                    } else {
                        f5 = (MiuiMultiWindowUtils.FREEFORM_HOTSPOT_REMINDER_LANDCAPE_RADIUS + MiuiMultiWindowUtils.FREEFORM_HOTSPOT_REMINDER_LANDCAPE_HORIZONTAL_BOTTOM_MARGIN) - MiuiMultiWindowUtils.FREEFORM_HOTSPOT_TRIGGER_LANDCAPE_RADIUS;
                    }
                    maxDistance = f5;
                }
                if (isPortrait3) {
                    d3 = MiuiMultiWindowUtils.getDistance(x, y, (float) MiuiFreeformPinManagerService.EDGE_AREA, getDisplayHeight()) - MiuiMultiWindowUtils.FREEFORM_HOTSPOT_TRIGGER_PORTRAIT_RADIUS;
                } else {
                    d3 = MiuiMultiWindowUtils.getDistance(x, y, (float) MiuiFreeformPinManagerService.EDGE_AREA, getDisplayHeight()) - MiuiMultiWindowUtils.FREEFORM_HOTSPOT_TRIGGER_LANDCAPE_RADIUS;
                }
                currentDistance = d3;
                break;
            case 4:
                boolean isPortrait4 = isPortrait();
                if (MiuiMultiWindowUtils.IS_FOLD_SCREEN_DEVICE) {
                    if (isPortrait4) {
                        f8 = (MiuiMultiWindowUtils.FREEFORM_HOTSPOT_REMINDER_PORTRAIT_RADIUS_FOLD + MiuiMultiWindowUtils.FREEFORM_HOTSPOT_REMINDER_PORTRAIT_VERTICAL_BOTTOM_MARGIN_FOLD) - MiuiMultiWindowUtils.FREEFORM_HOTSPOT_TRIGGER_PORTRAIT_RADIUS;
                    } else {
                        f8 = (MiuiMultiWindowUtils.FREEFORM_HOTSPOT_REMINDER_LANDCAPE_RADIUS_FOLD + MiuiMultiWindowUtils.FREEFORM_HOTSPOT_REMINDER_LANDCAPE_HORIZONTAL_BOTTOM_MARGIN) - MiuiMultiWindowUtils.FREEFORM_HOTSPOT_TRIGGER_LANDCAPE_RADIUS;
                    }
                    maxDistance = f8;
                } else {
                    if (isPortrait4) {
                        f7 = (MiuiMultiWindowUtils.FREEFORM_HOTSPOT_REMINDER_PORTRAIT_RADIUS + MiuiMultiWindowUtils.FREEFORM_HOTSPOT_REMINDER_PORTRAIT_VERTICAL_BOTTOM_MARGIN) - MiuiMultiWindowUtils.FREEFORM_HOTSPOT_TRIGGER_PORTRAIT_RADIUS;
                    } else {
                        f7 = (MiuiMultiWindowUtils.FREEFORM_HOTSPOT_REMINDER_LANDCAPE_RADIUS + MiuiMultiWindowUtils.FREEFORM_HOTSPOT_REMINDER_LANDCAPE_HORIZONTAL_BOTTOM_MARGIN) - MiuiMultiWindowUtils.FREEFORM_HOTSPOT_TRIGGER_LANDCAPE_RADIUS;
                    }
                    maxDistance = f7;
                }
                if (isPortrait4) {
                    d4 = MiuiMultiWindowUtils.getDistance(x, y, getDisplayWidth(), getDisplayHeight()) - MiuiMultiWindowUtils.FREEFORM_HOTSPOT_TRIGGER_PORTRAIT_RADIUS;
                } else {
                    d4 = MiuiMultiWindowUtils.getDistance(x, y, getDisplayWidth(), getDisplayHeight()) - MiuiMultiWindowUtils.FREEFORM_HOTSPOT_TRIGGER_LANDCAPE_RADIUS;
                }
                currentDistance = d4;
                break;
        }
        if (MiuiMultiWindowUtils.IS_FOLD_SCREEN_DEVICE) {
            subMaxMinRadius = MiuiMultiWindowUtils.FREEFORM_HOTSPOT_REMINDER_PORTRAIT_RADIUS_FOLD - MiuiMultiWindowUtils.FREEFORM_HOTSPOT_REMINDER_LANDCAPE_RADIUS_FOLD;
        } else {
            float subMaxMinRadius2 = MiuiMultiWindowUtils.FREEFORM_HOTSPOT_REMINDER_PORTRAIT_RADIUS;
            subMaxMinRadius = subMaxMinRadius2 - MiuiMultiWindowUtils.FREEFORM_HOTSPOT_REMINDER_LANDCAPE_RADIUS;
        }
        this.mCurrentRaduis = FREEFORM_HOTSPOT_MAX_RADIUS - ((((float) currentDistance) / maxDistance) * subMaxMinRadius);
        invalidate();
        Log.d(TAG, " hotSpotNum: " + hotSpotNum + " x: " + x + " y: " + y + "mCurrentRaduis" + this.mCurrentRaduis + " mCurrentColor:" + this.mCurrentColor);
    }

    public void enterSmallWindow() {
        Log.d(TAG, "enterSmallWindow");
        startAnimating(2);
    }

    public void outSmallWindow() {
        Log.d(TAG, "outSmallWindow");
        startAnimating(3);
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
        Log.d(TAG, "show");
        if (MiuiMultiWindowUtils.isNightMode(this.mContext)) {
            this.mMaxColor = FREEFORM_HOTSPOT_NIGHT_FREEFROM_COLOR;
            this.mMinColor = FREEFORM_HOTSPOT_NIGHT_SMALL_FREEFROM_COLOR;
        } else {
            this.mMaxColor = FREEFORM_HOTSPOT_FREEFROM_COLOR;
            this.mMinColor = FREEFORM_HOTSPOT_SMALL_FREEFROM_COLOR;
        }
        this.mCurrentColor = this.mMaxColor;
        startAnimating(0);
    }

    public void hide() {
        Log.d(TAG, "hide");
        startAnimating(1);
    }
}
