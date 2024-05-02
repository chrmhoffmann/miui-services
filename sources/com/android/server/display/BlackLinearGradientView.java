package com.android.server.display;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;
import com.android.server.wm.MiuiFreeformPinManagerService;
/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: classes.dex */
public class BlackLinearGradientView extends View {
    private static final boolean DEBUG = false;
    public static final String TAG = "BlackLinearGradientView";
    BlackLinearGradient mBlackLinearGradient;
    private Paint mPaint;
    private RectF mRectF;
    private final int DEFAULT_COLOR = -16777216;
    private int mStartColor = -16777216;
    private int mEndColor = -16777216;

    public BlackLinearGradientView(Context context) {
        super(context);
        initPaint();
    }

    private void initPaint() {
        Paint paint = new Paint();
        this.mPaint = paint;
        paint.setAntiAlias(true);
        this.mPaint.setStyle(Paint.Style.FILL);
        this.mRectF = new RectF(MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA, getWidth(), getHeight());
    }

    @Override // android.view.View
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        this.mRectF = new RectF(MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA, width, height);
        this.mBlackLinearGradient = new BlackLinearGradient((float) MiuiFreeformPinManagerService.EDGE_AREA, (float) MiuiFreeformPinManagerService.EDGE_AREA, (float) MiuiFreeformPinManagerService.EDGE_AREA, height, new int[]{-16777216, -16777216}, new float[]{MiuiFreeformPinManagerService.EDGE_AREA, 1.0f}, Shader.TileMode.CLAMP);
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        this.mPaint.setShader(this.mBlackLinearGradient);
        canvas.drawRect(this.mRectF, this.mPaint);
    }

    public void setLinearGradientColor(int[] colors) {
        setLinearGradientColor(colors, new float[]{MiuiFreeformPinManagerService.EDGE_AREA, 1.0f});
    }

    public void setLinearGradientColor(int[] colors, float[] positions) {
        if (colors.length < 2 || positions.length < 2) {
            return;
        }
        this.mStartColor = colors[0];
        this.mEndColor = colors[1];
        this.mBlackLinearGradient = new BlackLinearGradient((float) MiuiFreeformPinManagerService.EDGE_AREA, (float) MiuiFreeformPinManagerService.EDGE_AREA, (float) MiuiFreeformPinManagerService.EDGE_AREA, this.mRectF.bottom, colors, positions, Shader.TileMode.CLAMP);
        postInvalidate();
    }

    @Override // android.view.View
    public String toString() {
        return "LinearGradientView{ mStartColor=0x" + String.format("%08x", Integer.valueOf(this.mStartColor)) + ", mEndColor=0x" + String.format("%08x", Integer.valueOf(this.mEndColor)) + ", mRectF=" + this.mRectF + '}';
    }

    /* loaded from: classes.dex */
    public class BlackLinearGradient extends LinearGradient {
        float endX;
        float endY;
        float startX;
        float startY;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public BlackLinearGradient(float x0, float y0, float x1, float y1, int[] colors, float[] positions, Shader.TileMode tile) {
            super(x0, y0, x1, y1, colors, positions, tile);
            BlackLinearGradientView.this = r10;
        }

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        public BlackLinearGradient(float x0, float y0, float x1, float y1, int color0, int color1, Shader.TileMode tile) {
            super(x0, y0, x1, y1, color0, color1, tile);
            BlackLinearGradientView.this = r10;
        }

        public float getStartX() {
            return this.startX;
        }

        public void setStartX(float startX) {
            this.startX = startX;
        }

        public float getStartY() {
            return this.startY;
        }

        public void setStartY(float startY) {
            this.startY = startY;
        }

        public float getEndX() {
            return this.endX;
        }

        public void setEndX(float endX) {
            this.endX = endX;
        }

        public float getEndY() {
            return this.endY;
        }

        public void setEndY(float endY) {
            this.endY = endY;
        }
    }
}
