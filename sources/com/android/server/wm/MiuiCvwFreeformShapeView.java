package com.android.server.wm;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.BlendMode;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.util.MiuiMultiWindowUtils;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
/* loaded from: classes.dex */
public class MiuiCvwFreeformShapeView extends View {
    private static final int BOTTOM_POLE_COLOR_ALPHA = 51;
    private static final float BOTTOM_POLE_RADIUS;
    private static final int FREEFORM_BOTTOM_POLE_H;
    private static final int FREEFORM_TOP_POLE_H;
    private static final int SMALL_DOTS_COLOR_ALPHA = 51;
    private static final String TAG = "MiuiCvwFreeformShapeView";
    private static final int TOP_POLE_COLOR_ALPHA = 51;
    private static final float TOP_POLE_RADIUS;
    private boolean enableSolidColor;
    private float mBackgroundAlpha;
    private float mBackgroundCorner;
    private final RectF mBottomPoleRect;
    private Context mContext;
    private Paint mFilterPaint;
    private boolean mIsNight;
    private Paint mPaint;
    private final RectF mParentRect;
    private float mRequestAlpha;
    private final RectF mTopPoleRect;
    private static final int FREEFORM_TOP_POLE_W = (int) TypedValue.applyDimension(1, 53.0f, Resources.getSystem().getDisplayMetrics());
    private static final int FREEFORM_BOTTOM_POLE_W = (int) TypedValue.applyDimension(1, 98.0f, Resources.getSystem().getDisplayMetrics());
    private static final int FREEFORM_BOTTOM_SMALL_DOTS_RADIUS = (int) TypedValue.applyDimension(1, 3.0f, Resources.getSystem().getDisplayMetrics());
    private static final int FREEFORM_TOP_POLE_PADDING_TOP = (int) TypedValue.applyDimension(1, 10.0f, Resources.getSystem().getDisplayMetrics());
    private static final int FREEFORM_BOTTOM_POLE_PADDING_BOTTOM = (int) TypedValue.applyDimension(1, 10.0f, Resources.getSystem().getDisplayMetrics());
    private static final int FREEFORM_BOTTOM_SMALL_DOTS_PADDING = (int) TypedValue.applyDimension(1, 14.0f, Resources.getSystem().getDisplayMetrics());
    private static final int TOP_POLE_COLOR = Color.rgb(61, 61, 61);
    private static final int BOTTOM_POLE_COLOR = Color.rgb(61, 61, 61);
    private static final int SMALL_DOTS_COLOR = Color.rgb(61, 61, 61);
    private static final int COVER_MODE_NIGHT_YES_BG_COLOR = Color.parseColor("#000000");
    private static final int COVER_MODE_NIGHT_NO_BG_COLOR = Color.parseColor("#FFFFFF");
    private static final int COVER_NO_BLUR_COLOR = Color.parseColor("#4DFFFFFF");

    static {
        int applyDimension = (int) TypedValue.applyDimension(1, 3.0f, Resources.getSystem().getDisplayMetrics());
        FREEFORM_TOP_POLE_H = applyDimension;
        int applyDimension2 = (int) TypedValue.applyDimension(1, 3.0f, Resources.getSystem().getDisplayMetrics());
        FREEFORM_BOTTOM_POLE_H = applyDimension2;
        TOP_POLE_RADIUS = (applyDimension * 1.0f) / 2.0f;
        BOTTOM_POLE_RADIUS = (applyDimension2 * 1.0f) / 2.0f;
    }

    public MiuiCvwFreeformShapeView(Context context) {
        this(context, null);
    }

    public MiuiCvwFreeformShapeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mTopPoleRect = new RectF();
        this.mBottomPoleRect = new RectF();
        this.mParentRect = new RectF();
        this.mIsNight = false;
        this.mRequestAlpha = 1.0f;
        this.mBackgroundAlpha = 1.0f;
        this.enableSolidColor = false;
        this.mContext = context;
        this.mPaint = new Paint();
        this.mFilterPaint = new Paint(3);
        setBackgroundColor(0);
        this.mIsNight = MiuiMultiWindowUtils.isNightMode(context);
    }

    private RectF computeTopPoleRect() {
        float parentWidth = ((View) getParent()).getWidth();
        int i = FREEFORM_TOP_POLE_W;
        int topPoleX = (int) (((parentWidth - i) / 2.0f) + 0.5f);
        int topPoleY = FREEFORM_TOP_POLE_PADDING_TOP;
        int topPoleRight = i + topPoleX;
        int topPoleBottom = FREEFORM_TOP_POLE_H + topPoleY;
        this.mTopPoleRect.set(topPoleX, topPoleY, topPoleRight, topPoleBottom);
        return this.mTopPoleRect;
    }

    private RectF computeBottomPoleRect(int leftDotsPadding, int rightDotsPadding) {
        float parentWidth = ((View) getParent()).getWidth();
        float parentHeight = ((View) getParent()).getHeight();
        int i = FREEFORM_BOTTOM_POLE_W;
        int topPoleX = (int) (((parentWidth - i) / 2.0f) + 0.5f);
        int topPoleY = (int) ((parentHeight - FREEFORM_BOTTOM_POLE_PADDING_BOTTOM) + 0.5f);
        int topPoleRight = i + topPoleX;
        int topPoleBottom = FREEFORM_BOTTOM_POLE_H + topPoleY;
        this.mBottomPoleRect.set(Math.max(topPoleX, leftDotsPadding + 50), topPoleY, Math.min(topPoleRight, rightDotsPadding - 50), topPoleBottom);
        return this.mBottomPoleRect;
    }

    private int computeLeftDotsCX() {
        return FREEFORM_BOTTOM_SMALL_DOTS_PADDING;
    }

    private int computeRightDotsCX() {
        View parent = (View) getParent();
        return parent.getWidth() - FREEFORM_BOTTOM_SMALL_DOTS_PADDING;
    }

    private int computeDotsCY() {
        View parent = (View) getParent();
        return parent.getHeight() - FREEFORM_BOTTOM_POLE_PADDING_BOTTOM;
    }

    @Override // android.view.View
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        View parent = (ViewGroup) getParent();
        this.mParentRect.set(parent.getLeft(), parent.getTop(), parent.getRight(), parent.getBottom());
    }

    public void enableSolidColor() {
        this.enableSolidColor = true;
    }

    public void setShapeAlpha(float alpha) {
        this.mRequestAlpha = alpha;
    }

    public void setBackgroundAlpha(float alpha) {
        this.mBackgroundAlpha = alpha;
    }

    public void setBackgroundCorner(float corner) {
        this.mBackgroundCorner = corner;
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (this.enableSolidColor) {
            this.mPaint.setAlpha((int) (this.mBackgroundAlpha * 255.0f));
            if (this.mIsNight) {
                this.mPaint.setColor(COVER_MODE_NIGHT_YES_BG_COLOR);
                float f = this.mBackgroundCorner;
                canvas.drawRoundRect(0.0f, 0.0f, ((View) getParent()).getWidth(), ((View) getParent()).getHeight(), f, f, this.mPaint);
                this.mPaint.setColor(COVER_NO_BLUR_COLOR);
                float f2 = this.mBackgroundCorner;
                canvas.drawRoundRect(0.0f, 0.0f, ((View) getParent()).getWidth(), ((View) getParent()).getHeight(), f2, f2, this.mPaint);
            } else {
                this.mPaint.setColor(COVER_MODE_NIGHT_NO_BG_COLOR);
                float f3 = this.mBackgroundCorner;
                canvas.drawRoundRect(0.0f, 0.0f, ((View) getParent()).getWidth(), ((View) getParent()).getHeight(), f3, f3, this.mPaint);
                this.mPaint.setColor(COVER_NO_BLUR_COLOR);
                float f4 = this.mBackgroundCorner;
                canvas.drawRoundRect(0.0f, 0.0f, ((View) getParent()).getWidth(), ((View) getParent()).getHeight(), f4, f4, this.mPaint);
            }
        } else if (this.mIsNight) {
            canvas.setDrawFilter(new PaintFlagsDrawFilter(0, 3));
            setupBlendMode(1636469386, this.mFilterPaint, BlendMode.COLOR_BURN);
            setupBlendMode(1296187970, this.mFilterPaint, null);
            float f5 = this.mBackgroundCorner;
            canvas.drawRoundRect(0.0f, 0.0f, ((View) getParent()).getWidth(), ((View) getParent()).getHeight(), f5, f5, this.mFilterPaint);
        }
        if (this.mRequestAlpha > 0.0f) {
            int leftDotsCX = computeLeftDotsCX();
            int rightDotsCX = computeRightDotsCX();
            int dotsCY = computeDotsCY();
            int i = FREEFORM_BOTTOM_SMALL_DOTS_RADIUS;
            int leftDotsPadding = leftDotsCX + i;
            int rightDotsPadding = rightDotsCX - i;
            RectF topPoleRect = computeTopPoleRect();
            RectF bottomPoleRect = computeBottomPoleRect(leftDotsPadding, rightDotsPadding);
            this.mPaint.setColor(TOP_POLE_COLOR);
            this.mPaint.setStyle(Paint.Style.FILL);
            this.mPaint.setAlpha((int) (this.mRequestAlpha * 51.0f));
            float f6 = TOP_POLE_RADIUS;
            canvas.drawRoundRect(topPoleRect, f6, f6, this.mPaint);
            this.mPaint.setColor(BOTTOM_POLE_COLOR);
            this.mPaint.setAlpha((int) (this.mRequestAlpha * 51.0f));
            float f7 = BOTTOM_POLE_RADIUS;
            canvas.drawRoundRect(bottomPoleRect, f7, f7, this.mPaint);
            this.mPaint.setColor(SMALL_DOTS_COLOR);
            this.mPaint.setAlpha((int) (this.mRequestAlpha * 51.0f));
            canvas.drawCircle(leftDotsCX, dotsCY, i, this.mPaint);
            canvas.drawCircle(rightDotsCX, dotsCY, i, this.mPaint);
            this.mPaint.reset();
        }
    }

    private void setupBlendMode(int color, Paint blendPaint, BlendMode blendMode) {
        blendPaint.setColor(color);
        if (Build.VERSION.SDK_INT >= 29) {
            blendPaint.setBlendMode(blendMode);
        }
    }

    public boolean isPortrait() {
        return getResources().getConfiguration().orientation == 1;
    }

    public void updateShapeBounds(float left, float top, float right, float bottom) {
        this.mParentRect.set(left, top, right, bottom);
        invalidate();
    }
}
