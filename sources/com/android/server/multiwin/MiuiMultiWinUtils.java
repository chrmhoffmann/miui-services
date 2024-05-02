package com.android.server.multiwin;

import android.app.WindowConfiguration;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.display.DisplayManagerGlobal;
import android.os.Build;
import android.os.IBinder;
import android.os.UserHandle;
import android.provider.Settings;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Slog;
import android.view.Display;
import android.view.SurfaceControl;
import android.widget.ImageView;
import com.android.server.multiwin.listener.BlurListener;
import com.android.server.wm.ActivityTaskManagerService;
import com.android.server.wm.MiuiFreeformPinManagerService;
import com.android.server.wm.MiuiMultiWindowManager;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import miui.os.UserHandleEx;
/* loaded from: classes.dex */
public final class MiuiMultiWinUtils {
    private static final String BLUR_THREAD_NAME = "MiuiMultiWinUtils - BlurScreenShotThread";
    private static final int CORE_THREADS_NUM = 1;
    private static final int DEFAULT = -1;
    public static final int DEFAULT_ANIMATION_DURATION = 350;
    private static final int DEFAULT_BLUR_RADIUS = 15;
    public static final int DEFAULT_RADIATED_STROKE = 20;
    public static final int DEFAULT_RADIATED_WIDTH = 60;
    private static final double EQ_DELTA_TH = 1.0E-6d;
    private static final String FLOAT_TASK_STATE_KEY = "float_task_state";
    private static final long KEEP_ALIVE_DURATION = 60;
    public static final long MAX_ANIMATION_ALPHA = 1;
    public static final int MAX_BLUR_RADIUS = 25;
    private static final int MAX_THREADS_NUM = 5;
    public static final long MIN_ANIMATION_ALPHA = 0;
    private static final int SAMPLE_SIZE = 12;
    private static final String TAG = "MiuiMultiWinUtils";
    private static ThreadPoolExecutor sPoolExecutor;

    public static Drawable bitmap2Drawable(Bitmap bitmap) {
        return new BitmapDrawable(bitmap);
    }

    public static Bitmap drawable2Bitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bd = (BitmapDrawable) drawable;
            return bd.getBitmap();
        }
        int w = drawable.getIntrinsicWidth();
        int h = drawable.getIntrinsicHeight();
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, w, h);
        drawable.draw(canvas);
        return bitmap;
    }

    public static Bitmap getRadiatedBitmap(Bitmap input, int w, int stroke) {
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
        float cx = (tempInput.getWidth() * 1.0f) / 2.0f;
        canvas.drawCircle(cx, cx, radius, paint);
        return tempInput;
    }

    public static Bitmap changeBitmapAlpha(Bitmap sourceBitmap, int dstAlpha) {
        if (sourceBitmap == null) {
            return null;
        }
        if (dstAlpha < 0 || dstAlpha > 255) {
            return sourceBitmap;
        }
        Bitmap dstBitmap = Bitmap.createBitmap(sourceBitmap.getWidth(), sourceBitmap.getHeight(), sourceBitmap.getConfig());
        Canvas canvas = new Canvas(dstBitmap);
        Paint paint = new Paint();
        paint.setAlpha(dstAlpha);
        canvas.drawBitmap(sourceBitmap, MiuiFreeformPinManagerService.EDGE_AREA, MiuiFreeformPinManagerService.EDGE_AREA, paint);
        return dstBitmap;
    }

    public static Bitmap rsBlurNoScale(Context context, Bitmap bmp, int radius) {
        Bitmap result = bmp.copy(bmp.getConfig(), true);
        RenderScript renderScript = RenderScript.create(context);
        if (renderScript == null) {
            Log.w(TAG, "rsBlur failed, cause renderScript is null!");
            return bmp;
        }
        Allocation input = Allocation.createFromBitmap(renderScript, result);
        if (input == null) {
            Log.w(TAG, "rsBlur failed, cause input is null!");
            return bmp;
        }
        Allocation output = Allocation.createTyped(renderScript, input.getType());
        if (output == null) {
            Log.w(TAG, "rsBlur failed, cause output is null!");
            return bmp;
        }
        ScriptIntrinsicBlur scriptIntrinsicBlur = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript));
        if (scriptIntrinsicBlur == null) {
            Log.w(TAG, "rsBlur failed, cause scriptIntrinsicBlur is null!");
            return bmp;
        }
        scriptIntrinsicBlur.setInput(input);
        scriptIntrinsicBlur.setRadius(radius);
        scriptIntrinsicBlur.forEach(output);
        output.copyTo(result);
        renderScript.destroy();
        return result;
    }

    public static void blurForScreenShot(Bitmap inputBitmap, ImageView dstImageView, BlurListener blurListener, ImageView.ScaleType scaleType) {
        blurForScreenShot(inputBitmap, dstImageView, blurListener, scaleType, 15);
    }

    public static void blurForScreenShot(final Bitmap inputBitmap, final ImageView dstImageView, final BlurListener blurListener, final ImageView.ScaleType scaleType, final int blurRadius) {
        if (inputBitmap == null || dstImageView == null) {
            Log.w(TAG, "blurForScreenShot failed, cause inputBitmap or dstImageView is null");
            return;
        }
        if (sPoolExecutor == null) {
            sPoolExecutor = new ThreadPoolExecutor(1, 5, KEEP_ALIVE_DURATION, TimeUnit.SECONDS, new LinkedBlockingQueue(), new ThreadFactory() { // from class: com.android.server.multiwin.MiuiMultiWinUtils.1
                @Override // java.util.concurrent.ThreadFactory
                public final Thread newThread(Runnable r) {
                    return new Thread(r, MiuiMultiWinUtils.BLUR_THREAD_NAME);
                }
            }, new ThreadPoolExecutor.DiscardOldestPolicy());
        }
        sPoolExecutor.execute(new Runnable() { // from class: com.android.server.multiwin.MiuiMultiWinUtils.2
            @Override // java.lang.Runnable
            public void run() {
                Bitmap mBitmap = MiuiMultiWinUtils.rsBlur(dstImageView.getContext(), inputBitmap, blurRadius);
                if (ImageView.ScaleType.FIT_XY != scaleType) {
                    mBitmap = Bitmap.createScaledBitmap(mBitmap, inputBitmap.getWidth(), inputBitmap.getHeight(), true);
                }
                final Bitmap mDrawBitmap = mBitmap;
                dstImageView.post(new Runnable() { // from class: com.android.server.multiwin.MiuiMultiWinUtils.2.1
                    @Override // java.lang.Runnable
                    public void run() {
                        dstImageView.setScaleType(scaleType);
                        Drawable mDrawable = MiuiMultiWinUtils.bitmap2Drawable(mDrawBitmap);
                        dstImageView.setImageDrawable(mDrawable);
                        if (blurListener != null) {
                            blurListener.onBlurDone();
                        }
                    }
                });
            }
        });
    }

    public static void blurForScreenShot(ImageView sourceImageView, ImageView dstImageView, BlurListener blurListener, ImageView.ScaleType scaleType) {
        blurForScreenShot(drawable2Bitmap(sourceImageView.getDrawable()), dstImageView, blurListener, scaleType, 15);
    }

    public static int convertWindowMode2SplitMode(int windowMode, boolean isLandScape) {
        if (WindowConfiguration.isSplitScreenPrimaryWindowingMode(windowMode)) {
            return isLandScape ? 1 : 3;
        } else if (WindowConfiguration.isSplitScreenSecondaryWindowingMode(windowMode)) {
            return isLandScape ? 2 : 4;
        } else if (WindowConfiguration.isFreeFormWindowingMode(windowMode)) {
            return 5;
        } else {
            return 0;
        }
    }

    public static int dip2px(Context context, float dipValue) {
        return (int) ((context.getResources().getDisplayMetrics().density * dipValue) + 0.5f);
    }

    public static boolean floatEquals(float f1, float f2) {
        float result = f1 > f2 ? f1 - f2 : f2 - f1;
        return ((double) result) <= EQ_DELTA_TH;
    }

    public static Drawable getAppIcon(Context context, String pkgName, int userId) {
        PackageManager pm;
        ApplicationInfo mAppInfo;
        ApplicationInfo mAppInfo2 = null;
        Slog.d(TAG, "get app icon: pkgName = " + pkgName);
        Drawable mDrawable = null;
        if (pkgName != null && (pm = context.getPackageManager()) != null) {
            try {
                mAppInfo2 = pm.getApplicationInfoAsUser(pkgName, 0, userId);
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "icon not load!");
            }
            Drawable mDrawable2 = mAppInfo2 == null ? null : mAppInfo2.loadIcon(pm);
            if (mDrawable2 == null) {
                Log.w(TAG, "getAppIcon failed, cause iconDrawable is null!");
                return null;
            }
            UserHandle userHandle = UserHandleEx.getUserHandle(userId);
            if (userHandle != null) {
                mDrawable = pm.getUserBadgedIcon(mDrawable2, userHandle);
            }
            Drawable result = mDrawable == null ? mDrawable2 : mDrawable;
            DisplayMetrics dm = context.getResources().getDisplayMetrics();
            if (dm == null) {
                Log.w(TAG, "getAppIcon failed, cause DisplayMetrics is null!");
                return result;
            }
            float density = dm.density;
            if (result instanceof BitmapDrawable) {
                BitmapDrawable icon = new BitmapDrawable(drawable2Bitmap(result));
                icon.setBounds(0, 0, (int) (density * 68.0f), (int) (68.0f * density));
                return icon;
            }
            if (Build.VERSION.SDK_INT < 26) {
                mAppInfo = mAppInfo2;
            } else if (result instanceof AdaptiveIconDrawable) {
                AdaptiveIconDrawable drawable = (AdaptiveIconDrawable) result;
                int w = drawable.getIntrinsicWidth();
                int h = drawable.getIntrinsicHeight();
                Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                drawable.draw(canvas);
                BitmapDrawable icon2 = new BitmapDrawable(bitmap);
                icon2.setBounds(0, 0, (int) (density * 68.0f), (int) (68.0f * density));
                return icon2;
            } else {
                mAppInfo = mAppInfo2;
            }
            return null;
        }
        return null;
    }

    public static Rect getBoundWithoutNavBar(int navBarPos, Rect navBarBound, Rect bound) {
        Rect outBound = new Rect(bound);
        if (navBarPos == 1) {
            outBound.left += navBarBound.width();
        }
        if (navBarPos == 2) {
            outBound.right -= navBarBound.width();
        }
        if (navBarPos == 4) {
            outBound.bottom -= navBarBound.height();
        }
        return outBound;
    }

    public static Point getDisplaySize() {
        Display display = DisplayManagerGlobal.getInstance().getRealDisplay(0);
        if (display == null) {
            Log.w(TAG, "getDisplaySize failed, cause display is null!");
            return null;
        }
        Point displaySize = new Point();
        display.getRealSize(displaySize);
        return displaySize;
    }

    public static int getFloatTaskState(Context context) {
        if (context == null) {
            return 0;
        }
        int state = Settings.Secure.getIntForUser(context.getContentResolver(), FLOAT_TASK_STATE_KEY, -1, -2);
        return state;
    }

    public static Bitmap getScreenShotBmpWithoutNavBar(Bitmap src, int navBarPos, Rect navBarBound, float srcScale) {
        if (src == null) {
            Slog.w(TAG, "getScreenShotBmpWithoutNavBar failed, cause src is null!");
            return src;
        } else if (navBarPos == -1) {
            return src;
        } else {
            Bitmap temp = src;
            int navBarWidth = (int) (navBarBound.width() * srcScale);
            int navBarHeight = (int) (navBarBound.height() * srcScale);
            int sourceHeight = src.getHeight();
            int sourceWidth = src.getWidth();
            if (navBarPos == 1) {
                temp = Bitmap.createBitmap(src, navBarWidth, 0, sourceWidth - navBarWidth, sourceHeight);
            }
            if (navBarPos == 2) {
                temp = Bitmap.createBitmap(src, 0, 0, sourceWidth - navBarWidth, sourceHeight);
            }
            if (navBarPos == 4) {
                Bitmap temp2 = Bitmap.createBitmap(src, 0, 0, sourceWidth, sourceHeight - navBarHeight);
                return temp2;
            }
            return temp;
        }
    }

    public static Bitmap getStatusBarScreenShot(int statusBarWidth, int statusBarHeight) {
        Rect sourceCrop = new Rect(0, 0, statusBarWidth, statusBarHeight);
        Display display = DisplayManagerGlobal.getInstance().getRealDisplay(0);
        if (display == null) {
            Log.w(TAG, "getStatusBarScreenShot failed, cause display is null!");
            return null;
        }
        return getScreenShot(display.getDisplayId(), statusBarWidth, statusBarHeight, sourceCrop);
    }

    public static Bitmap getScreenShot(int displayId, int width, int height, Rect bounds) {
        IBinder displayToken = SurfaceControl.getInternalDisplayToken();
        SurfaceControl.DisplayCaptureArgs displayCaptureArgs = new SurfaceControl.DisplayCaptureArgs.Builder(displayToken).setSize(width, height).setSourceCrop(bounds).build();
        SurfaceControl.ScreenshotHardwareBuffer screenshotHardwareBuffer = SurfaceControl.captureDisplay(displayCaptureArgs);
        if (screenshotHardwareBuffer != null) {
            return screenshotHardwareBuffer.asBitmap();
        }
        return null;
    }

    public static Bitmap getWallpaperScreenShot(ActivityTaskManagerService atms) {
        if (atms == null) {
            Slog.w(TAG, "getWallpaperScreenShot failed, cause atms is null!");
            return null;
        }
        MiuiMultiWindowManager manager = MiuiMultiWindowManager.getInstance(atms);
        if (manager != null) {
            return null;
        }
        Slog.w(TAG, "getWallpaperScreenShot failed, cause manager is null!");
        return null;
    }

    public static boolean isInNightMode(Context context) {
        if (context == null) {
            Log.w(TAG, "check if isInNightMode failed, cause context is null");
            return false;
        } else if ((context.getResources().getConfiguration().uiMode & 48) != 32) {
            return false;
        } else {
            return true;
        }
    }

    public static boolean isNeedToResizeWithoutNavBar(int splitMode, int navBarPos) {
        if (splitMode == 1 && (navBarPos == 1 || navBarPos == 4)) {
            return true;
        }
        if (splitMode == 2 && (navBarPos == 2 || navBarPos == 4)) {
            return true;
        }
        if (splitMode == 3 && (navBarPos == 1 || navBarPos == 2)) {
            return true;
        }
        if (splitMode != 4) {
            return false;
        }
        return navBarPos == 1 || navBarPos == 2 || navBarPos == 4;
    }

    public static void putFloatTaskStateToSettings(boolean isEnable, Context context) {
        if (context == null) {
            return;
        }
        if (isEnable) {
            Settings.Secure.putIntForUser(context.getContentResolver(), FLOAT_TASK_STATE_KEY, 1, -2);
        } else {
            Settings.Secure.putIntForUser(context.getContentResolver(), FLOAT_TASK_STATE_KEY, 0, -2);
        }
    }

    public static Bitmap rsBlur(Context context, Bitmap bmp, int radius) {
        if (context == null) {
            Log.w(TAG, "context is null, rsBlur failed!");
            return bmp;
        } else if (bmp == null) {
            Log.w(TAG, "bmp is null, ruBlur failed!");
            return bmp;
        } else {
            Bitmap blurBmp = Bitmap.createScaledBitmap(bmp, Math.round(bmp.getWidth() / 12.0f), Math.round(bmp.getHeight() / 12.0f), false);
            if (blurBmp == null) {
                Log.w(TAG, "rsBlur failed, cause blurBmp is null!");
                return bmp;
            }
            RenderScript renderScript = RenderScript.create(context);
            if (renderScript == null) {
                Log.w(TAG, "rsBlur failed, cause renderScript is null!");
                return bmp;
            }
            Allocation input = Allocation.createFromBitmap(renderScript, blurBmp);
            if (input == null) {
                Log.w(TAG, "rsBlur failed, cause input is null!");
                return bmp;
            }
            Allocation output = Allocation.createTyped(renderScript, input.getType());
            if (output == null) {
                Log.w(TAG, "rsBlur failed, cause output is null!");
                return bmp;
            }
            ScriptIntrinsicBlur scriptIntrinsicBlur = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript));
            if (scriptIntrinsicBlur == null) {
                Log.w(TAG, "rsBlur failed, cause scriptIntrinsicBlur is null!");
                return bmp;
            }
            scriptIntrinsicBlur.setInput(input);
            scriptIntrinsicBlur.setRadius(radius);
            scriptIntrinsicBlur.forEach(output);
            output.copyTo(blurBmp);
            renderScript.destroy();
            return blurBmp;
        }
    }

    public static Bitmap takeScreenshot(int topHeightSubtraction) {
        Display display = DisplayManagerGlobal.getInstance().getRealDisplay(0);
        if (display == null) {
            Log.w(TAG, "takeScreenshot failed, cause display is null!");
            return null;
        }
        Point displaySize = getDisplaySize();
        if (displaySize == null) {
            return null;
        }
        int screenShotWidth = displaySize.x;
        int screenShotHeight = displaySize.y - topHeightSubtraction;
        Rect crop = new Rect(0, topHeightSubtraction, displaySize.x, displaySize.y);
        Log.i(TAG, "Taking screenshot of dimensions " + screenShotWidth + " x " + screenShotHeight + ", crop = " + crop);
        Bitmap screenShot = getScreenShot(display.getDisplayId(), (int) (screenShotWidth / 2.0f), (int) (screenShotHeight / 2.0f), crop);
        if (screenShot == null) {
            Log.w(TAG, "Failed to take screenshot of dimensions " + screenShotWidth + " x " + screenShotHeight);
            return null;
        }
        Bitmap softBmp = screenShot.copy(Bitmap.Config.ARGB_8888, true);
        if (softBmp == null) {
            Log.w(TAG, "Failed to copy soft bitmap!");
            return null;
        }
        softBmp.setHasAlpha(false);
        return softBmp;
    }
}
