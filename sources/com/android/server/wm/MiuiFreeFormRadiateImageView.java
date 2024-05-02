package com.android.server.wm;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.BlendMode;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
/* loaded from: classes.dex */
public class MiuiFreeFormRadiateImageView extends View {
    private static final int ICON_W_H_THRESHOLD = (int) TypedValue.applyDimension(1, 69.0f, Resources.getSystem().getDisplayMetrics());
    private int centerX;
    private int centerY;
    private Matrix mBackGroundMatrix;
    private int mBlurIconLeft;
    private int mBlurIconTop;
    private int mBlurRatio;
    private int mHeight;
    private Drawable mIcon;
    private Bitmap mIconBit;
    private int mIconHeight;
    private int mIconLeft;
    private Matrix mIconMatrix;
    private int mIconTop;
    private int mIconWidth;
    private Paint mLightPaint;
    private Paint mPaint;
    private Bitmap mRadiateBit;
    private float mShadowAlpha;
    private int mShadowDiameter;
    private int mWidth;

    public MiuiFreeFormRadiateImageView(Context context) {
        super(context);
        init();
    }

    public MiuiFreeFormRadiateImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        this.mLightPaint = new Paint();
        this.mPaint = new Paint(3);
        this.mIconMatrix = new Matrix();
        this.mBackGroundMatrix = new Matrix();
    }

    @Override // android.view.View
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int iconHeight = this.mIconWidth;
        int iconWidth = iconHeight != 0 ? iconHeight : ICON_W_H_THRESHOLD;
        if (this.mIconHeight == 0) {
            iconHeight = ICON_W_H_THRESHOLD;
        }
        int defaultDiameter = Math.max(getMeasuredWidth(), getMeasuredHeight());
        int shadowDiameter = this.mShadowDiameter;
        if (shadowDiameter == 0) {
            shadowDiameter = defaultDiameter;
        }
        this.centerX = getLeft() + (getMeasuredWidth() / 2);
        int top = getTop() + (getMeasuredHeight() / 2);
        this.centerY = top;
        int i = this.centerX;
        this.mBlurIconLeft = (int) ((i - ((shadowDiameter * 1.0f) / 2.0f)) + 0.5f);
        this.mBlurIconTop = (int) ((top - ((shadowDiameter * 1.0f) / 2.0f)) + 0.5f);
        this.mIconLeft = (int) ((i - ((iconWidth * 1.0f) / 2.0f)) + 0.5f);
        this.mIconTop = (int) ((top - ((iconHeight * 1.0f) / 2.0f)) + 0.5f);
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
    }

    public void setIcon(Drawable icon) {
        this.mIcon = icon;
    }

    public void showRadiate(Bitmap icon, int shadowDiameter, int blurRatio) {
        if (icon == null || icon.isRecycled()) {
            return;
        }
        int i = this.mWidth;
        this.centerX = i / 2;
        int i2 = this.mHeight;
        this.centerY = i2 / 2;
        this.mShadowDiameter = shadowDiameter > 0 ? shadowDiameter : Math.max(i, i2);
        this.mBlurRatio = blurRatio > 0 ? blurRatio : 25;
        this.mBlurIconLeft = (int) ((this.centerX - ((shadowDiameter * 1.0f) / 2.0f)) + 0.5f);
        this.mBlurIconTop = (int) ((this.centerY - ((shadowDiameter * 1.0f) / 2.0f)) + 0.5f);
        this.mIconBit = icon;
        this.mRadiateBit = radiate(getContext(), this.mIconBit);
        invalidate();
    }

    public void showIcon(Bitmap icon, int w, int h, int shadowDiameter, int blurRatio) {
        if (icon == null || icon.isRecycled()) {
            return;
        }
        int i = this.mWidth;
        this.centerX = i / 2;
        int i2 = this.mHeight;
        this.centerY = i2 / 2;
        this.mShadowDiameter = shadowDiameter > 0 ? shadowDiameter : Math.max(i, i2);
        this.mBlurRatio = blurRatio > 0 ? blurRatio : 25;
        this.mBlurIconLeft = (int) ((this.centerX - ((shadowDiameter * 1.0f) / 2.0f)) + 0.5f);
        this.mBlurIconTop = (int) ((this.centerY - ((shadowDiameter * 1.0f) / 2.0f)) + 0.5f);
        this.mIconBit = icon;
        this.mRadiateBit = radiate(getContext(), this.mIconBit);
        int i3 = w > 0 ? w : ICON_W_H_THRESHOLD;
        this.mIconWidth = i3;
        int i4 = h > 0 ? h : ICON_W_H_THRESHOLD;
        this.mIconHeight = i4;
        this.mIconLeft = (int) ((this.centerX - ((i3 * 1.0f) / 2.0f)) + 0.5f);
        this.mIconTop = (int) ((this.centerY - ((i4 * 1.0f) / 2.0f)) + 0.5f);
        invalidate();
    }

    public void hideIcon() {
        this.mIconWidth = 0;
        this.mIconHeight = 0;
        invalidate();
    }

    public void setIconWidth(int w) {
        this.mIconWidth = w;
        invalidate();
    }

    public void setIconHeight(int h) {
        this.mIconHeight = h;
        invalidate();
    }

    public void setShadowAlpha(float alpha) {
        this.mShadowAlpha = alpha;
        invalidate();
    }

    @Override // android.view.View
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        this.mHeight = getHeight();
        int width = getWidth();
        this.mWidth = width;
        int i = width / 2;
        this.centerX = i;
        int i2 = this.mHeight / 2;
        this.centerY = i2;
        int i3 = this.mShadowDiameter;
        this.mBlurIconLeft = (int) ((i - ((i3 * 1.0f) / 2.0f)) + 0.5f);
        this.mBlurIconTop = (int) ((i2 - ((i3 * 1.0f) / 2.0f)) + 0.5f);
        this.mIconLeft = (int) ((i - ((this.mIconWidth * 1.0f) / 2.0f)) + 0.5f);
        this.mIconTop = (int) ((i2 - ((this.mIconHeight * 1.0f) / 2.0f)) + 0.5f);
        invalidate();
    }

    private Bitmap getRadiateMaterial(Bitmap input, int w, int stroke) {
        float strokeWidth = stroke;
        int realDiameter = (int) (w + (strokeWidth * 2.0f));
        float radius = (w * 1.0f) / 2.0f;
        Bitmap tempInput = Bitmap.createBitmap(realDiameter, realDiameter, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(tempInput);
        Paint paint = new Paint();
        BitmapShader bitmapShader = new BitmapShader(input, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        float scale = (w * 1.0f) / input.getWidth();
        Matrix matrix = new Matrix();
        matrix.setScale(scale, scale);
        matrix.postTranslate((realDiameter - w) / 2, (realDiameter - w) / 2);
        bitmapShader.setLocalMatrix(matrix);
        paint.setShader(bitmapShader);
        canvas.drawColor(0, BlendMode.CLEAR);
        float cx = (tempInput.getWidth() * 1.0f) / 2.0f;
        canvas.drawCircle(cx, cx, radius, paint);
        return tempInput;
    }

    public Bitmap radiate(Context context, Bitmap input) {
        int outDiameter = this.mShadowDiameter;
        if (outDiameter <= 0) {
            outDiameter = this.mHeight;
        }
        Bitmap tempInput = getRadiateMaterial(input, 40, 30);
        Bitmap result = tempInput.copy(tempInput.getConfig(), true);
        RenderScript rsScript = RenderScript.create(context);
        if (rsScript == null) {
            return null;
        }
        float outScale = outDiameter / result.getHeight();
        Allocation alloc = Allocation.createFromBitmap(rsScript, tempInput, Allocation.MipmapControl.MIPMAP_NONE, 1);
        Allocation outAlloc = Allocation.createTyped(rsScript, alloc.getType());
        ScriptIntrinsicBlur blur = ScriptIntrinsicBlur.create(rsScript, Element.U8_4(rsScript));
        blur.setRadius(this.mBlurRatio);
        blur.setInput(alloc);
        blur.forEach(outAlloc);
        outAlloc.copyTo(result);
        rsScript.destroy();
        Bitmap output = Bitmap.createBitmap(outDiameter, outDiameter, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint();
        paint.setFilterBitmap(true);
        paint.setXfermode(null);
        paint.setAntiAlias(true);
        canvas.save();
        canvas.scale(outScale, outScale);
        canvas.drawBitmap(result, MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA, paint);
        canvas.restore();
        if (result != null && !result.isRecycled()) {
            result.recycle();
        }
        if (tempInput != null && !tempInput.isRecycled()) {
            tempInput.recycle();
        }
        return output;
    }

    public void setBlurType(int blurType) {
        switch (blurType) {
            case 0:
                this.mLightPaint.setMaskFilter(new BlurMaskFilter(this.mBlurRatio, BlurMaskFilter.Blur.INNER));
                break;
            case 1:
                this.mLightPaint.setMaskFilter(new BlurMaskFilter(this.mBlurRatio, BlurMaskFilter.Blur.NORMAL));
                break;
            case 2:
                this.mLightPaint.setMaskFilter(new BlurMaskFilter(this.mBlurRatio, BlurMaskFilter.Blur.SOLID));
                break;
            case 3:
                this.mLightPaint.setMaskFilter(new BlurMaskFilter(this.mBlurRatio, BlurMaskFilter.Blur.OUTER));
                break;
        }
        invalidate();
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Bitmap bitmap = this.mIconBit;
        if (bitmap != null && !bitmap.isRecycled()) {
            float shadowAlpha = this.mShadowAlpha;
            if (shadowAlpha <= MiuiFreeformPinManagerService.EDGE_AREA) {
                shadowAlpha = 0.5f;
            }
            this.mPaint.setAlpha((int) (255.0f * shadowAlpha));
            this.mBackGroundMatrix.postTranslate(this.mBlurIconLeft, this.mBlurIconTop);
            canvas.drawBitmap(this.mRadiateBit, this.mBackGroundMatrix, this.mPaint);
            int i = this.mIconHeight;
            if (i != 0 && this.mIconWidth != 0) {
                float iconYScale = (i * 1.0f) / this.mIconBit.getHeight();
                float iconXScale = (this.mIconWidth * 1.0f) / this.mIconBit.getWidth();
                this.mIconMatrix.postScale(iconXScale, iconYScale);
                this.mIconMatrix.postTranslate(this.mIconLeft, this.mIconTop);
                canvas.drawBitmap(this.mIconBit, this.mIconMatrix, null);
                this.mIconMatrix.reset();
            }
            this.mBackGroundMatrix.reset();
        }
    }

    @Override // android.view.View
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override // android.view.View
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Bitmap bitmap = this.mRadiateBit;
        if (bitmap != null && !bitmap.isRecycled()) {
            this.mRadiateBit.recycle();
            this.mRadiateBit = null;
        }
        Bitmap bitmap2 = this.mIconBit;
        if (bitmap2 != null && !bitmap2.isRecycled()) {
            this.mIconBit.recycle();
            this.mIconBit = null;
        }
    }
}
