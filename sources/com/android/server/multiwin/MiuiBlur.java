package com.android.server.multiwin;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.Slog;
import com.android.server.wm.MiuiFreeformPinManagerService;
import com.miui.iimagekit.blur.BlurAlgorithm;
/* loaded from: classes.dex */
public final class MiuiBlur {
    private static final String TAG = "MiuiBlur";
    private static BlurAlgorithm mStaticAlgorithm = new BlurAlgorithm((Context) null, 2, true);

    public static Bitmap blur(Bitmap input, int radius, int downscale, boolean isNeedSkipNavBar) {
        if (input == null || input.getWidth() == 0 || input.getHeight() == 0) {
            Slog.e(TAG, "blur: input bitmap is null or bitmap size is 0");
            return input;
        } else if (radius <= 0 || downscale <= 0) {
            Slog.e(TAG, "blur: blur radius and downscale must > 0");
            return input;
        } else {
            int blurRadius = radius / downscale;
            if (blurRadius == 0) {
                Slog.e(TAG, "blur: blur radius downscale must < radius");
                return input;
            }
            return getBlurredBitmap(getBitmapForBlur(input, downscale, isNeedSkipNavBar), blurRadius);
        }
    }

    private static Bitmap getBitmapForBlur(Bitmap input, int downscale, boolean isNeedSkipNavBar) {
        int scaledW = Math.max(1, input.getWidth() / downscale);
        int scaledH = Math.max(1, input.getHeight() / downscale);
        Bitmap bitmapForBlur = Bitmap.createBitmap(scaledW, scaledH, Bitmap.Config.ARGB_4444);
        Canvas blurCanvas = new Canvas(bitmapForBlur);
        RectF rectF = new RectF(MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA, scaledW, scaledH);
        Bitmap temp = input.copy(Bitmap.Config.ARGB_8888, true);
        Paint paint = new Paint();
        paint.setShader(getBitmapScaleShader(temp, bitmapForBlur, isNeedSkipNavBar));
        blurCanvas.drawRect(rectF, paint);
        return bitmapForBlur;
    }

    private static BitmapShader getBitmapScaleShader(Bitmap in, Bitmap out, boolean isNeedSkipNavBar) {
        float scaleRateY;
        float scaleRateX;
        if (isNeedSkipNavBar) {
            scaleRateX = out.getWidth() / in.getWidth();
            scaleRateY = out.getHeight() / in.getHeight();
        } else {
            scaleRateX = out.getWidth() / in.getWidth();
            scaleRateY = out.getHeight() / in.getHeight();
        }
        Matrix matrix = new Matrix();
        matrix.postScale(scaleRateX, scaleRateY);
        BitmapShader shader = new BitmapShader(in, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        shader.setLocalMatrix(matrix);
        return shader;
    }

    private static Bitmap getBlurredBitmap(Bitmap bitmapForBlur, int radius) {
        Bitmap blurredBitmap = Bitmap.createBitmap(bitmapForBlur.getWidth(), bitmapForBlur.getHeight(), Bitmap.Config.ARGB_4444);
        BlurAlgorithm blurAlgorithm = mStaticAlgorithm;
        if (blurAlgorithm != null) {
            blurAlgorithm.blur(bitmapForBlur, blurredBitmap, radius);
        }
        return blurredBitmap;
    }
}
