package miui.android.animation.styles;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;
import android.view.View;
import com.android.server.wm.MiuiFreeformPinManagerService;
import miui.android.animation.Folme;
import miui.android.animation.property.ViewPropertyExt;
import miui.android.animation.utils.CommonUtils;
/* loaded from: classes.dex */
public class TintDrawable extends Drawable {
    private static final View.OnAttachStateChangeListener sListener = new View.OnAttachStateChangeListener() { // from class: miui.android.animation.styles.TintDrawable.1
        @Override // android.view.View.OnAttachStateChangeListener
        public void onViewAttachedToWindow(View v) {
        }

        @Override // android.view.View.OnAttachStateChangeListener
        public void onViewDetachedFromWindow(View v) {
            TintDrawable sd = TintDrawable.get(v);
            if (sd != null && Build.VERSION.SDK_INT >= 23) {
                Drawable oriDrawable = sd.mOriDrawable;
                if (oriDrawable != null) {
                    v.setForeground(oriDrawable);
                }
                sd.clear();
                v.removeOnAttachStateChangeListener(this);
            }
        }
    };
    private Bitmap mBitmap;
    private Drawable mOriDrawable;
    private View mView;
    private Paint mPaint = new Paint();
    private RectF mBounds = new RectF();
    private Rect mSrcRect = new Rect();
    private RectF mCornerBounds = new RectF();
    private boolean isSetCorner = false;
    private float mEadius = MiuiFreeformPinManagerService.EDGE_AREA;

    @Override // android.graphics.drawable.Drawable
    public void setAlpha(int alpha) {
    }

    @Override // android.graphics.drawable.Drawable
    public void setColorFilter(ColorFilter colorFilter) {
    }

    @Override // android.graphics.drawable.Drawable
    public int getOpacity() {
        return -2;
    }

    public static TintDrawable get(View view) {
        if (Build.VERSION.SDK_INT >= 23) {
            Drawable fg = view.getForeground();
            if (fg instanceof TintDrawable) {
                return (TintDrawable) fg;
            }
            return null;
        }
        return null;
    }

    public static TintDrawable setAndGet(final View view) {
        TintDrawable sd = get(view);
        if (sd == null && Build.VERSION.SDK_INT >= 23) {
            final TintDrawable sd2 = new TintDrawable();
            sd2.mView = view;
            sd2.setOriDrawable(view.getForeground());
            view.addOnAttachStateChangeListener(sListener);
            Folme.post(view, new Runnable() { // from class: miui.android.animation.styles.TintDrawable.2
                @Override // java.lang.Runnable
                public void run() {
                    view.setForeground(sd2);
                }
            });
            return sd2;
        }
        return sd;
    }

    private void setOriDrawable(Drawable d) {
        this.mOriDrawable = d;
    }

    public void setCorner(float radius) {
        this.isSetCorner = radius != MiuiFreeformPinManagerService.EDGE_AREA;
        this.mEadius = radius;
    }

    public void initTintBuffer(int tintMode) {
        View view = this.mView;
        if (view == null) {
            return;
        }
        int width = view.getWidth();
        int height = this.mView.getHeight();
        if (width == 0 || height == 0) {
            recycleBitmap();
            return;
        }
        createBitmap(width, height);
        initBitmap(tintMode);
    }

    private void createBitmap(int width, int height) {
        if (Build.VERSION.SDK_INT >= 23) {
            Bitmap bitmap = this.mBitmap;
            if (bitmap != null && bitmap.getWidth() == width && this.mBitmap.getHeight() == this.mView.getHeight()) {
                return;
            }
            recycleBitmap();
            this.mPaint.setAntiAlias(true);
            try {
                this.mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            } catch (OutOfMemoryError e) {
                Log.w(CommonUtils.TAG, "TintDrawable.createBitmap failed, out of memory");
            }
        }
    }

    public void clear() {
        recycleBitmap();
    }

    private void recycleBitmap() {
        Bitmap bitmap = this.mBitmap;
        if (bitmap != null) {
            bitmap.recycle();
            this.mBitmap = null;
        }
    }

    private void initBitmap(int tintMode) {
        if (Build.VERSION.SDK_INT < 23) {
            return;
        }
        Bitmap bitmap = this.mBitmap;
        if (bitmap == null || bitmap.isRecycled()) {
            this.mView.setForeground(this.mOriDrawable);
            return;
        }
        try {
            this.mBitmap.eraseColor(0);
            Canvas canvas = new Canvas(this.mBitmap);
            int left = this.mView.getScrollX();
            int top = this.mView.getScrollY();
            canvas.translate(-left, -top);
            this.mView.setForeground(this.mOriDrawable);
            this.mView.draw(canvas);
            this.mView.setForeground(this);
            if (tintMode == 0) {
                ColorMatrix mColorMatrix = new ColorMatrix(new float[]{1.0f, MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA, 1.0f, MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA, 1.0f, MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA, Float.MAX_VALUE, MiuiFreeformPinManagerService.EDGE_AREA});
                this.mPaint.setColorFilter(new ColorMatrixColorFilter(mColorMatrix));
                canvas.drawBitmap(this.mBitmap, MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA, this.mPaint);
            }
        } catch (Exception e) {
            Log.w(CommonUtils.TAG, "TintDrawable.initBitmap failed, " + e);
        }
    }

    @Override // android.graphics.drawable.Drawable
    public void draw(Canvas canvas) {
        int left = this.mView.getScrollX();
        int top = this.mView.getScrollY();
        int width = this.mView.getWidth();
        int height = this.mView.getHeight();
        this.mBounds.set(left, top, left + width, top + height);
        this.mSrcRect.set(0, 0, width, height);
        canvas.save();
        int color = ViewPropertyExt.FOREGROUND.getIntValue(this.mView);
        try {
            canvas.clipRect(this.mBounds);
            canvas.drawColor(0);
            Drawable drawable = this.mOriDrawable;
            if (drawable != null) {
                drawable.draw(canvas);
            }
            Bitmap bitmap = this.mBitmap;
            if (bitmap != null && !bitmap.isRecycled()) {
                if (this.isSetCorner) {
                    this.mCornerBounds.set(this.mSrcRect);
                    this.mPaint.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
                    RectF rectF = this.mCornerBounds;
                    float f = this.mEadius;
                    canvas.drawRoundRect(rectF, f, f, this.mPaint);
                } else {
                    this.mPaint.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
                    canvas.drawBitmap(this.mBitmap, this.mSrcRect, this.mBounds, this.mPaint);
                }
                return;
            }
            if (Build.VERSION.SDK_INT >= 23) {
                this.mView.setForeground(this.mOriDrawable);
            }
        } finally {
            canvas.restore();
        }
    }

    public void restoreOriginalDrawable() {
        clear();
        invalidateSelf();
    }
}
